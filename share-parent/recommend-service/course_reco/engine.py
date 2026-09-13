from __future__ import annotations

import csv
import copy
import gc
import json
import os
import random
import shutil
import struct
import tempfile
import time
from collections import Counter, OrderedDict
from pathlib import Path
from typing import Any, Callable, Iterable, Sequence

import numpy as np
import torch
from torch import nn

from .data import (
    CourseDataset,
    SequenceExample,
    SplitBundle,
    build_course_prerequisites,
    build_item_mapping,
    load_moocubex,
    make_synthetic_dataset,
    make_split_bundle,
)
from .metrics import (
    per_example_metrics,
    prerequisite_consistency_with_coverage,
    ranking_metrics,
)
from .models import (
    BPRMF,
    DRAGLiteRanker,
    ItemKNN,
    MarkovChain,
    MostPop,
    SequenceRanker,
    T5CourseRanker,
)
from .retrieval import Evidence, HybridRetriever, KnowledgeState, random_evidence


METHOD_ALIASES = {
    "kp4sr_random": "kp4sr",
    "kp4sr_bm25": "drag_kp4sr",
    "kp4sr_dense": "drag_kp4sr",
    "kp4sr_hybrid": "drag_kp4sr",
    "drag_kp4sr_nomask": "drag_kp4sr",
    "drag_kp4sr_nofrontier": "drag_kp4sr",
}

DEFAULT_METHODS = (
    "mostpop",
    "itemknn",
    "markov",
    "bprmf",
    "gru4rec",
    "sasrec",
    "bert4rec",
    "p5",
    "kp4sr",
    "drag_kp4sr",
)

ABLATION_METHODS = (
    "kp4sr_random",
    "kp4sr_bm25",
    "kp4sr_dense",
    "kp4sr_hybrid",
    "drag_kp4sr_nomask",
    "drag_kp4sr_nofrontier",
)

SUPPORTED_METHODS = set(DEFAULT_METHODS) | set(ABLATION_METHODS)


def seed_everything(seed: int) -> None:
    random.seed(seed)
    np.random.seed(seed)
    torch.manual_seed(seed)
    if torch.cuda.is_available():
        torch.cuda.manual_seed_all(seed)
    torch.use_deterministic_algorithms(False)


def _sync_device(device: torch.device) -> None:
    if device.type == "cuda":
        torch.cuda.synchronize(device)


def _resolve_device(requested: Any) -> torch.device:
    """Resolve a requested accelerator without breaking CPU-only machines."""
    value = str(requested or "auto").strip().lower()
    if value in {"auto", "default"}:
        return torch.device("cuda" if torch.cuda.is_available() else "cpu")
    try:
        device = torch.device(value)
    except (TypeError, RuntimeError) as exc:
        raise ValueError(f"无法识别 device={requested!r}；请使用 cpu、cuda[:index] 或 auto") from exc
    if device.type == "cuda":
        if not torch.cuda.is_available():
            return torch.device("cpu")
        if device.index is not None and device.index >= torch.cuda.device_count():
            raise ValueError(
                f"请求的 CUDA 设备 {device.index} 不存在；可用设备数为 {torch.cuda.device_count()}"
            )
    mps_backend = getattr(torch.backends, "mps", None)
    if device.type == "mps" and (mps_backend is None or not mps_backend.is_available()):
        return torch.device("cpu")
    return device


def _example_values(example: SequenceExample | tuple[list[int], int]):
    if isinstance(example, SequenceExample):
        return example.history, example.target
    return example


def _batchify(
    examples: Sequence[tuple[list[int], int]],
    pad_id: int,
    batch_size: int,
    shuffle: bool,
    seed: int,
    max_len: int | None = None,
):
    order = list(range(len(examples)))
    if shuffle:
        random.Random(seed).shuffle(order)
    for start in range(0, len(order), batch_size):
        batch = [examples[i] for i in order[start : start + batch_size]]
        histories = []
        targets = []
        for history, target in batch:
            history = list(history)
            if max_len is not None and len(history) > max_len:
                history = history[-max_len:]
            histories.append(history or [pad_id])
            targets.append(target)
        longest = max(len(history) for history in histories)
        x = torch.full((len(batch), longest), pad_id, dtype=torch.long)
        lengths = torch.tensor(
            [0 if history == [pad_id] else len(history) for history in histories],
            dtype=torch.long,
        )
        y = torch.tensor(targets, dtype=torch.long)
        for row, history in enumerate(histories):
            if lengths[row] > 0:
                x[row, : lengths[row]] = torch.tensor(history, dtype=torch.long)
        yield x, lengths.clamp(min=1), y


def _train_neural(
    model: nn.Module,
    train: Sequence[tuple[list[int], int]],
    device: torch.device,
    epochs: int,
    batch_size: int,
    lr: float,
    *,
    evidence_fn: Callable[[torch.Tensor], torch.Tensor] | None = None,
    max_len: int | None = None,
    seed: int = 2026,
) -> float | None:
    if not train:
        return None
    model.to(device)
    opt = torch.optim.AdamW(model.parameters(), lr=lr)
    loss_fn = nn.CrossEntropyLoss()
    model.train()
    last_loss = 0.0
    for epoch in range(epochs):
        for x, lengths, y in _batchify(
            train, model.pad_id, batch_size, True, seed + epoch, max_len=max_len
        ):
            x, lengths, y = x.to(device), lengths.to(device), y.to(device)
            opt.zero_grad(set_to_none=True)
            if evidence_fn:
                evidence = evidence_fn(x).to(device)
                logits = model(x, lengths, evidence)
            else:
                logits = model(x, lengths)
            loss = loss_fn(logits, y)
            loss.backward()
            nn.utils.clip_grad_norm_(model.parameters(), 5.0)
            opt.step()
            last_loss = float(loss.detach())
    return last_loss


def _make_masked_examples(
    train_sequences: Sequence[Sequence[int]], max_len: int, mask_id: int
) -> list[tuple[list[int], int, int]]:
    """Create deterministic BERT4Rec masked-item examples from train only."""
    masked: list[tuple[list[int], int, int]] = []
    for sequence in train_sequences:
        sequence = list(sequence)[-max_len:]
        for position, target in enumerate(sequence):
            inputs = list(sequence)
            inputs[position] = mask_id
            masked.append((inputs, position, target))
    return masked


def _train_bert(
    model: SequenceRanker,
    train_sequences: Sequence[Sequence[int]],
    device: torch.device,
    epochs: int,
    batch_size: int,
    lr: float,
    seed: int,
) -> float | None:
    masked = _make_masked_examples(train_sequences, model.max_len, model.mask_id)
    if not masked:
        return None
    model.to(device)
    opt = torch.optim.AdamW(model.parameters(), lr=lr)
    loss_fn = nn.CrossEntropyLoss()
    last_loss = 0.0
    for epoch in range(epochs):
        order = list(range(len(masked)))
        random.Random(seed + epoch).shuffle(order)
        model.train()
        for start in range(0, len(order), batch_size):
            batch = [masked[index] for index in order[start : start + batch_size]]
            max_batch_len = max(len(row[0]) for row in batch)
            x = torch.full((len(batch), max_batch_len), model.pad_id, dtype=torch.long)
            positions = torch.tensor([row[1] for row in batch], dtype=torch.long)
            targets = torch.tensor([row[2] for row in batch], dtype=torch.long)
            lengths = torch.tensor([len(row[0]) for row in batch], dtype=torch.long)
            for row, (inputs, _, _) in enumerate(batch):
                x[row, : len(inputs)] = torch.tensor(inputs, dtype=torch.long)
            x, positions, targets, lengths = (
                x.to(device),
                positions.to(device),
                targets.to(device),
                lengths.to(device),
            )
            opt.zero_grad(set_to_none=True)
            all_logits = model(x, lengths)
            logits = all_logits[torch.arange(len(batch), device=device), positions]
            loss = loss_fn(logits, targets)
            loss.backward()
            nn.utils.clip_grad_norm_(model.parameters(), 5.0)
            opt.step()
            last_loss = float(loss.detach())
    return last_loss


def _rank_bert(
    model: SequenceRanker,
    examples: Sequence[tuple[list[int], int]],
    device: torch.device,
    batch_size: int,
) -> list[list[int]]:
    rankings: list[list[int]] = []
    model.eval()
    with torch.no_grad():
        for start in range(0, len(examples), batch_size):
            batch = examples[start : start + batch_size]
            prepared: list[list[int]] = []
            positions: list[int] = []
            for history, _ in batch:
                history = list(history)[-(model.max_len - 1) :]
                positions.append(len(history))
                prepared.append(history + [model.mask_id])
            max_batch_len = max((len(row) for row in prepared), default=1)
            x = torch.full((len(prepared), max_batch_len), model.pad_id, dtype=torch.long)
            lengths = torch.tensor([len(row) for row in prepared], dtype=torch.long)
            for row, values in enumerate(prepared):
                x[row, : len(values)] = torch.tensor(values, dtype=torch.long)
            x, lengths = x.to(device), lengths.to(device)
            logits = model(x, lengths)
            selected = logits[torch.arange(len(batch), device=device), torch.tensor(positions, device=device)]
            rankings.extend(torch.argsort(selected, dim=-1, descending=True).cpu().tolist())
    return rankings


def _rank_neural(
    model: SequenceRanker | DRAGLiteRanker,
    examples: Sequence[tuple[list[int], int]],
    device: torch.device,
    batch_size: int,
    *,
    evidence_fn: Callable[[torch.Tensor], torch.Tensor] | None = None,
    max_len: int | None = None,
) -> list[list[int]]:
    if isinstance(model, SequenceRanker) and model.kind == "bert":
        return _rank_bert(model, examples, device, batch_size)
    model.eval()
    rankings: list[list[int]] = []
    with torch.no_grad():
        for x, lengths, _ in _batchify(
            examples, model.pad_id, batch_size, False, 0, max_len=max_len
        ):
            x, lengths = x.to(device), lengths.to(device)
            if evidence_fn:
                logits = model(x, lengths, evidence_fn(x).to(device))
            else:
                logits = model(x, lengths)
            rankings.extend(torch.argsort(logits, dim=-1, descending=True).cpu().tolist())
    return rankings


def _canonical_method(method: str) -> str:
    return METHOD_ALIASES.get(method.lower(), method.lower())


def _effective_config(config: dict[str, Any]) -> dict[str, Any]:
    """Apply method-specific ablation switches before any model is built."""
    effective = copy.deepcopy(config)
    original = str(effective.get("method", "mostpop")).lower()
    retrieval = effective.get("retrieval")
    if not isinstance(retrieval, dict):
        retrieval = {}
        effective["retrieval"] = retrieval
    model = effective.get("model")
    if not isinstance(model, dict):
        model = {}
        effective["model"] = model
    if original == "p5":
        retrieval["evidence_mode"] = "none"
        retrieval["use_frontier"] = False
    elif original == "kp4sr":
        # KP4SR is the fixed/static knowledge-prompt control.  BM25 is an
        # explicit retrieval ablation and must not be conflated with A0.
        retrieval["evidence_mode"] = "static"
        retrieval["use_frontier"] = False
    elif original == "kp4sr_random":
        retrieval["evidence_mode"] = "random"
        retrieval["use_frontier"] = False
    elif original == "kp4sr_bm25":
        retrieval["evidence_mode"] = "bm25"
        retrieval["use_frontier"] = False
    elif original == "kp4sr_dense":
        retrieval["evidence_mode"] = "dense"
        retrieval["use_frontier"] = False
    elif original == "kp4sr_hybrid":
        retrieval["evidence_mode"] = "hybrid"
        retrieval["use_frontier"] = False
    elif original == "drag_kp4sr_nomask":
        model["tree_mask"] = False
        retrieval["evidence_mode"] = "hybrid"
    elif original == "drag_kp4sr":
        retrieval["evidence_mode"] = "hybrid"
    elif original == "drag_kp4sr_nofrontier":
        retrieval["use_frontier"] = False
        retrieval["evidence_mode"] = "hybrid"
    return effective


def _retrieval_config(config: dict[str, Any]) -> dict[str, Any]:
    result = dict(config.get("retrieval", {}) or {})
    method = str(config.get("method", "")).lower()
    if method in {"p5", "kp4sr"}:
        result.setdefault("evidence_mode", "none" if method == "p5" else "static")
    if method == "kp4sr_random":
        result["evidence_mode"] = "random"
    if method == "kp4sr_bm25":
        result["evidence_mode"] = "bm25"
    if method == "kp4sr_dense":
        result["evidence_mode"] = "dense"
    if method == "kp4sr_hybrid":
        result["evidence_mode"] = "hybrid"
    if method == "drag_kp4sr_nofrontier":
        result["use_frontier"] = False
    return result


def _make_retriever(
    dataset: CourseDataset,
    config: dict[str, Any],
    device: torch.device,
    *,
    need_dense: bool,
) -> HybridRetriever:
    retrieval_cfg = _retrieval_config(config)
    dense_model = retrieval_cfg.get("dense_model") if need_dense else None
    model_cfg = config.get("model", {}) or {}
    dense_fallback = bool(
        retrieval_cfg.get(
            "dense_fallback",
            not dense_model and str(model_cfg.get("backend", "lite")).lower() == "lite",
        )
    )
    if need_dense and not dense_model and dense_fallback:
        dense_model = "__hash__"
    if need_dense and not dense_model:
        raise ValueError(
            "当前实验要求 dense/hybrid 检索，但 retrieval.dense_model 未配置；"
            "请指定真实向量模型，或显式设置 retrieval.dense_fallback: true。"
        )
    weights = {
        key: retrieval_cfg[key]
        for key in ("bm25", "dense", "graph", "field")
        if key in retrieval_cfg
    }
    return HybridRetriever(
        dataset.course_text,
        dataset.course_meta,
        dataset.prerequisite_edges,
        dense_model=dense_model,
        course_concepts=dataset.course_concepts,
        dense_device=str(device) if dense_model else None,
        weights=weights,
        decay=float(retrieval_cfg.get("decay", 0.3)),
        max_path_length=int(retrieval_cfg.get("max_path_length", 2)),
        dense_fallback=dense_fallback,
        cache_size=int(retrieval_cfg.get("cache_size", 512)),
    )


def _retrieval_snapshot(stats: Any) -> dict[str, Any]:
    """Copy retrieval counters so training/warm-up/test phases can be split."""
    return {
        "search_seconds": float(getattr(stats, "search_seconds", 0.0)),
        "path_seconds": float(getattr(stats, "path_seconds", 0.0)),
        "calls": int(getattr(stats, "calls", 0)),
        "fallback_calls": int(getattr(stats, "fallback_calls", 0)),
        "empty_calls": int(getattr(stats, "empty_calls", 0)),
        "cache_hits": int(getattr(stats, "cache_hits", 0)),
    }


def _retrieval_delta(after: dict[str, Any], before: dict[str, Any]) -> dict[str, Any]:
    delta: dict[str, Any] = {
        "index_build_seconds": 0.0,
        "dense_backend": "none",
        "dense_fallback": False,
    }
    for field_name in ("search_seconds", "path_seconds"):
        delta[field_name] = max(
            0.0,
            float(after.get(field_name, 0.0)) - float(before.get(field_name, 0.0)),
        )
    for field_name in ("calls", "fallback_calls", "empty_calls", "cache_hits"):
        delta[field_name] = max(
            0,
            int(after.get(field_name, 0)) - int(before.get(field_name, 0)),
        )
    return delta


def _make_evidence_fn(
    retriever: HybridRetriever,
    id_to_item: list[str],
    item_to_id: dict[str, int],
    pad_id: int,
    top_k: int,
    mode: str,
    seed: int,
    *,
    use_frontier: bool = True,
    evidence_mode: str | None = None,
) -> Callable[[torch.Tensor], torch.Tensor]:
    selected_mode = (evidence_mode or mode or "hybrid").lower()
    # The lite DRAG path calls this function once for every training prefix.
    # Keeping every prefix's evidence alive duplicates the retriever cache and
    # grows without a bound on MOOCCubeX.  Reuse the same bounded budget as the
    # retriever, which is enough for repeated prefixes within nearby batches.
    cache_limit = max(0, int(getattr(retriever, "cache_size", 0)))
    cache: OrderedDict[tuple[str, ...], list[Evidence]] = OrderedDict()

    def fn(histories: torch.Tensor) -> torch.Tensor:
        rows: list[list[int]] = []
        for row in histories.detach().cpu().tolist():
            history = [id_to_item[i] for i in row if i != pad_id and i < len(id_to_item)]
            key = tuple(history)
            evidence = cache.get(key)
            if evidence is None:
                if selected_mode == "none":
                    evidence: list[Evidence] = []
                else:
                    evidence = retriever.search(
                        history,
                        top_k,
                        mode=selected_mode,
                        use_frontier=use_frontier,
                        random_seed=seed,
                    )
                if not evidence and selected_mode != "none":
                    evidence = retriever.static_evidence(history, top_k)
                    retriever.stats.fallback_calls += 1
                if cache_limit:
                    cache[key] = evidence
                    cache.move_to_end(key)
                    while len(cache) > cache_limit:
                        cache.popitem(last=False)
            else:
                cache.move_to_end(key)
            ids = [item_to_id[e.doc_id] for e in evidence if e.doc_id in item_to_id]
            rows.append((ids + [pad_id] * top_k)[:top_k])
        return torch.tensor(rows, dtype=torch.long)

    def cache_clear() -> None:
        cache.clear()

    fn.cache_clear = cache_clear  # type: ignore[attr-defined]
    return fn


def _state_label(concept: str) -> str:
    return str(concept).removeprefix("K_").replace("_", " ")


_PROMPT_RECORD_HEADER = struct.Struct("<Q")


class _DiskPromptStore:
    """Random-access prompt storage that keeps the full corpus off the heap."""

    def __init__(self, path: Path, offsets: list[int]):
        self.path = path
        self.offsets = offsets
        self._handle = path.open("rb")

    @classmethod
    def build(
        cls,
        path: Path,
        prompts: Iterable[str],
        *,
        label: str,
        total: int,
        progress_every: int = 5_000,
    ) -> "_DiskPromptStore":
        path.parent.mkdir(parents=True, exist_ok=True)
        offsets: list[int] = []
        try:
            with path.open("wb") as handle:
                for index, prompt in enumerate(prompts, start=1):
                    payload = str(prompt).encode("utf-8")
                    offsets.append(handle.tell())
                    handle.write(_PROMPT_RECORD_HEADER.pack(len(payload)))
                    handle.write(payload)
                    if index == 1 or index % progress_every == 0 or index == total:
                        print(
                            f"[t5] {label} prompts: {index}/{total}",
                            flush=True,
                        )
        except Exception:
            try:
                path.unlink()
            except OSError:
                pass
            raise
        return cls(path, offsets)

    @classmethod
    def open(
        cls,
        path: Path,
        *,
        expected_total: int | None = None,
    ) -> "_DiskPromptStore":
        """Open a completed prompt store and rebuild its offset index.

        The index is intentionally kept in memory, while prompt payloads stay
        on disk.  Rebuilding it lets a later retry reuse a cache left behind
        by a forcibly terminated process without retaining the prompts on the
        Python heap.
        """
        file_size = path.stat().st_size
        offsets: list[int] = []
        with path.open("rb") as handle:
            while handle.tell() < file_size:
                offset = handle.tell()
                header = handle.read(_PROMPT_RECORD_HEADER.size)
                if len(header) != _PROMPT_RECORD_HEADER.size:
                    raise RuntimeError(f"prompt cache header is truncated at offset {offset}")
                (payload_size,) = _PROMPT_RECORD_HEADER.unpack(header)
                payload_start = handle.tell()
                if payload_size > file_size - payload_start:
                    raise RuntimeError(f"prompt cache record is truncated at offset {offset}")
                offsets.append(offset)
                handle.seek(payload_size, os.SEEK_CUR)
        if expected_total is not None and len(offsets) != expected_total:
            raise RuntimeError(
                f"prompt cache contains {len(offsets)} records; expected {expected_total}"
            )
        return cls(path, offsets)

    def __len__(self) -> int:
        return len(self.offsets)

    def _read_at(self, index: int) -> str:
        if index < 0 or index >= len(self.offsets):
            raise IndexError(f"prompt index out of range: {index}")
        self._handle.seek(self.offsets[index])
        header = self._handle.read(_PROMPT_RECORD_HEADER.size)
        if len(header) != _PROMPT_RECORD_HEADER.size:
            raise RuntimeError(f"prompt cache header is truncated at index {index}")
        (size,) = _PROMPT_RECORD_HEADER.unpack(header)
        payload = self._handle.read(size)
        if len(payload) != size:
            raise RuntimeError(f"prompt cache record is truncated at index {index}")
        return payload.decode("utf-8")

    def get_many(self, indices: Iterable[int]) -> list[str]:
        return [self._read_at(int(index)) for index in indices]

    def close(self, *, remove: bool = True) -> None:
        handle = getattr(self, "_handle", None)
        if handle is not None and not handle.closed:
            handle.close()
        if remove:
            try:
                self.path.unlink()
            except OSError:
                pass


def _prompt_cache_dir(seed: int) -> Path:
    configured = os.environ.get("T5_PROMPT_CACHE_ROOT")
    parent = (
        Path(configured)
        if configured
        else Path(tempfile.gettempdir()) / "course_reco_t5_prompt_cache"
    )
    parent.mkdir(parents=True, exist_ok=True)
    return Path(tempfile.mkdtemp(prefix=f"seed{seed}_", dir=str(parent)))


def _load_reusable_prompt_cache(
    seed: int,
    *,
    train_total: int,
    test_total: int,
) -> tuple[Path, _DiskPromptStore, _DiskPromptStore] | None:
    """Find and open a complete cache left by an earlier attempt.

    Cache directories are scoped to one method output directory by the
    caller.  The record counts provide an additional integrity check, so a
    partially written or mismatched cache is never used as training input.
    """
    configured = os.environ.get("T5_PROMPT_CACHE_ROOT")
    if not configured:
        return None
    parent = Path(configured)
    if not parent.is_dir():
        return None
    candidates = sorted(
        (path for path in parent.iterdir() if path.is_dir() and path.name.startswith(f"seed{seed}_")),
        key=lambda path: path.stat().st_mtime,
        reverse=True,
    )
    for candidate in candidates:
        train_path = candidate / "train.prompts.bin"
        test_path = candidate / "test.prompts.bin"
        if not train_path.is_file() or not test_path.is_file():
            continue
        train_store: _DiskPromptStore | None = None
        test_store: _DiskPromptStore | None = None
        try:
            train_store = _DiskPromptStore.open(train_path, expected_total=train_total)
            test_store = _DiskPromptStore.open(test_path, expected_total=test_total)
        except (OSError, RuntimeError, ValueError):
            if train_store is not None:
                train_store.close(remove=False)
            if test_store is not None:
                test_store.close(remove=False)
            # This is an old, non-reusable attempt. It is safe to remove only
            # after both stores failed validation and neither is open.
            shutil.rmtree(candidate, ignore_errors=True)
            continue
        print(f"[t5] reusing prompt cache: {candidate}", flush=True)
        return candidate, train_store, test_store
    return None


def _prompt_evidence(
    retriever: HybridRetriever,
    history: Sequence[str],
    retrieval_cfg: dict[str, Any],
    method: str,
    seed: int,
) -> list[Evidence]:
    top_k = int(retrieval_cfg.get("top_k", 5))
    mode = str(retrieval_cfg.get("evidence_mode", retrieval_cfg.get("mode", "hybrid"))).lower()
    use_frontier = bool(retrieval_cfg.get("use_frontier", True))
    if method == "p5" or mode == "none":
        return []
    evidence = retriever.search(
        history,
        top_k,
        mode=mode,
        use_frontier=use_frontier,
        random_seed=seed,
    )
    if not evidence:
        evidence = retriever.static_evidence(history, top_k)
        retriever.stats.fallback_calls += 1
    if method == "drag_kp4sr" and use_frontier and mode in {"hybrid", "dense", "bm25"}:
        paths = retriever.path_evidence(history, min(2, top_k), use_frontier=True)
        evidence = (paths + evidence)[:top_k]
    return evidence


def _iter_prompt_texts(
    examples: Sequence[SequenceExample],
    dataset: CourseDataset,
    retriever: HybridRetriever,
    method: str,
    retrieval_cfg: dict[str, Any],
    seed: int,
) -> Iterable[str]:
    use_frontier = bool(retrieval_cfg.get("use_frontier", True))
    for example in examples:
        history = example.history[-int(retrieval_cfg.get("history_limit", 10)) :]
        recent = " ; ".join(dataset.course_text.get(course, course) for course in history)
        if method == "p5":
            yield f"<HIST> 已完成课程：{recent}。 <TASK> 下一门课程是 <MASK>。"
            continue
        state: KnowledgeState = retriever.build_state(history)
        mastered = " ; ".join(_state_label(x) for x in sorted(
            state.mastered_weights,
            key=lambda x: (-state.mastered_weights[x], x),
        )[:32])
        frontier = ""
        if use_frontier:
            frontier = " ; ".join(_state_label(x) for x in sorted(
                state.frontier_weights,
                key=lambda x: (-state.frontier_weights[x], x),
            )[:32])
        evidence = _prompt_evidence(retriever, history, retrieval_cfg, method, seed)
        chunks = [
            f"<HIST> 已完成课程：{recent}。",
            f"<STATE> 已接触概念：{mastered}。",
        ]
        if use_frontier:
            chunks.append(f"<FRONTIER> 待衔接概念：{frontier}。")
        chunks.append("<TASK> 下一门课程是 <MASK>。")
        for index, item in enumerate(evidence[:32]):
            chunks.append(f"<EVID_{index}> [{item.kind}] {item.text}")
        yield " ".join(chunks)


def _build_prompt_texts(
    examples: Sequence[SequenceExample],
    dataset: CourseDataset,
    retriever: HybridRetriever,
    method: str,
    retrieval_cfg: dict[str, Any],
    seed: int,
) -> list[str]:
    """Backward-compatible in-memory prompt builder for small callers/tests."""
    return list(_iter_prompt_texts(examples, dataset, retriever, method, retrieval_cfg, seed))


def _run_t5(
    config: dict[str, Any],
    dataset: CourseDataset,
    bundle: SplitBundle,
    train_target_ids: Sequence[int],
    item_to_id: dict[str, int],
    id_to_item: list[str],
    device: torch.device,
) -> tuple[list[list[int]], float | None, dict[str, Any]]:
    method = _canonical_method(str(config.get("method", "drag_kp4sr")))
    model_cfg = config.get("model", {})
    train_cfg = config.get("training", {})
    retrieval_cfg = _retrieval_config(config)
    need_dense = str(
        retrieval_cfg.get("evidence_mode", retrieval_cfg.get("mode", "hybrid"))
    ).lower() in {"dense", "hybrid"}
    retriever: HybridRetriever | None = _make_retriever(
        dataset, config, device, need_dense=need_dense
    )
    model: T5CourseRanker | None = None
    raw_train = bundle.examples["train"]
    raw_test = bundle.examples["test"]
    seed = int(config.get("seed", 2026))
    reusable_cache = _load_reusable_prompt_cache(
        seed,
        train_total=len(raw_train),
        test_total=len(raw_test),
    )
    if reusable_cache is None:
        prompt_dir = _prompt_cache_dir(seed)
    else:
        prompt_dir = reusable_cache[0]
    train_prompts: _DiskPromptStore | None = None
    test_prompts: _DiskPromptStore | None = None
    try:
        # Prompt construction is the largest host-RAM phase.  Keep only an
        # offset index in Python and stream the actual strings through an F:
        # drive cache file instead of retaining tens of thousands of prompts.
        if reusable_cache is None:
            train_prompts = _DiskPromptStore.build(
                prompt_dir / "train.prompts.bin",
                _iter_prompt_texts(raw_train, dataset, retriever, method, retrieval_cfg, seed),
                label=f"{method} seed={seed} train",
                total=len(raw_train),
            )
        else:
            train_prompts, test_prompts = reusable_cache[1], reusable_cache[2]
        train_retrieval_snapshot = _retrieval_snapshot(retriever.stats)
        retriever.clear_runtime_caches()
        gc.collect()

        if reusable_cache is None:
            test_prompts = _DiskPromptStore.build(
                prompt_dir / "test.prompts.bin",
                _iter_prompt_texts(raw_test, dataset, retriever, method, retrieval_cfg, seed),
                label=f"{method} seed={seed} test",
                total=len(raw_test),
            )
        test_prompt_retrieval_snapshot = _retrieval_snapshot(retriever.stats)
        test_retrieval = _retrieval_delta(
            test_prompt_retrieval_snapshot,
            train_retrieval_snapshot,
        )
        retriever.clear_runtime_caches()
        retrieval_details = dict(vars(retriever.stats))
        # BGE is only needed while prompts are being built.  Keeping it alive
        # beside T5 wastes GPU/host memory for the entire training run and was
        # a major contributor to the previous multi-process OOMs.
        del retriever
        retriever = None
        gc.collect()
        if device.type == "cuda":
            torch.cuda.empty_cache()

        model = T5CourseRanker(
            model_cfg.get("pretrained", "google-t5/t5-small"), id_to_item
        ).to(device)
        train_targets = torch.tensor(train_target_ids, dtype=torch.long)
        opt = torch.optim.AdamW(model.parameters(), lr=float(train_cfg.get("lr", 5e-5)))
        loss_fn = nn.CrossEntropyLoss()
        batch_size = int(train_cfg.get("batch_size", 8))
        epochs = int(train_cfg.get("epochs", 3))
        max_length = int(model_cfg.get("max_length", 384))
        last_loss: float | None = None
        _sync_device(device)
        train_started = time.perf_counter()
        for epoch in range(epochs):
            order = list(range(len(train_prompts)))
            random.Random(seed + epoch).shuffle(order)
            model.train()
            total_train_batches = (len(order) + batch_size - 1) // batch_size
            epoch_started = time.perf_counter()
            print(
                f"[t5] {method} seed={seed} train epoch {epoch + 1}/{epochs} "
                f"start; batches={total_train_batches}",
                flush=True,
            )
            for start in range(0, len(order), batch_size):
                batch_index = start // batch_size + 1
                indices = order[start : start + batch_size]
                batch = model.tokenizer(
                    train_prompts.get_many(indices),
                    padding=True,
                    truncation=True,
                    max_length=max_length,
                    return_tensors="pt",
                ).to(device)
                targets = train_targets[indices].to(device)
                opt.zero_grad(set_to_none=True)
                if model_cfg.get("tree_mask", True) and method in {"kp4sr", "drag_kp4sr"}:
                    attention = model.tree_attention_mask(batch.input_ids, batch.attention_mask)
                else:
                    attention = batch.attention_mask
                logits = model(batch.input_ids, attention)
                loss = loss_fn(logits, targets)
                loss.backward()
                nn.utils.clip_grad_norm_(model.parameters(), 1.0)
                opt.step()
                last_loss = float(loss.detach())
                # Do not keep the last autograd graph/batch alive while the
                # next batch is prepared.  The allocator may retain reusable
                # blocks, but the Python references themselves must disappear.
                del batch, targets, attention, logits, loss
                if (
                    batch_index == 1
                    or batch_index % 100 == 0
                    or batch_index == total_train_batches
                ):
                    elapsed = time.perf_counter() - epoch_started
                    print(
                        f"[t5] {method} seed={seed} train epoch {epoch + 1}/{epochs} "
                        f"batch {batch_index}/{total_train_batches} "
                        f"loss={last_loss:.6f} elapsed={elapsed / 60.0:.1f}min",
                        flush=True,
                    )
            print(
                f"[t5] {method} seed={seed} train epoch {epoch + 1}/{epochs} "
                f"done elapsed={(time.perf_counter() - epoch_started) / 60.0:.1f}min",
                flush=True,
            )

        # AdamW keeps two state tensors per trainable parameter.  They are
        # useful during optimization only; retaining them through evaluation
        # needlessly raises the process and GPU working set.
        del opt, loss_fn, train_targets
        gc.collect()
        if device.type == "cuda":
            torch.cuda.empty_cache()

        rankings: list[list[int]] = []
        token_lengths: list[int] = []
        truncated = 0
        _sync_device(device)
        train_seconds = time.perf_counter() - train_started
        efficiency_cfg = config.get("efficiency", {}) or {}
        warmup_enabled = bool(efficiency_cfg.get("warmup", True))
        warmup_cases = min(
            len(test_prompts),
            max(1, int(efficiency_cfg.get("warmup_cases", batch_size))),
        ) if len(test_prompts) else 0
        if warmup_enabled and warmup_cases:
            model.eval()
            warmup_batch = model.tokenizer(
                test_prompts.get_many(range(warmup_cases)),
                padding=True,
                truncation=True,
                max_length=max_length,
                return_tensors="pt",
            ).to(device)
            if model_cfg.get("tree_mask", True) and method in {"kp4sr", "drag_kp4sr"}:
                warmup_attention = model.tree_attention_mask(
                    warmup_batch.input_ids, warmup_batch.attention_mask
                )
            else:
                warmup_attention = warmup_batch.attention_mask
            with torch.inference_mode():
                model(warmup_batch.input_ids, warmup_attention)
            _sync_device(device)
            del warmup_batch, warmup_attention
            if device.type == "cuda":
                torch.cuda.empty_cache()
        inference_started = time.perf_counter()
        model.eval()
        total_test_batches = (len(test_prompts) + batch_size - 1) // batch_size
        with torch.inference_mode():
            for start in range(0, len(test_prompts), batch_size):
                batch_index = start // batch_size + 1
                texts = test_prompts.get_many(
                    range(start, min(start + batch_size, len(test_prompts)))
                )
                batch = model.tokenizer(
                    texts,
                    padding=True,
                    truncation=True,
                    max_length=max_length,
                    return_tensors="pt",
                ).to(device)
                lengths = batch.attention_mask.sum(dim=1).detach().cpu().tolist()
                token_lengths.extend(int(value) for value in lengths)
                truncated += sum(int(value >= max_length) for value in lengths)
                if model_cfg.get("tree_mask", True) and method in {"kp4sr", "drag_kp4sr"}:
                    attention = model.tree_attention_mask(batch.input_ids, batch.attention_mask)
                else:
                    attention = batch.attention_mask
                logits = model(batch.input_ids, attention)
                rankings.extend(torch.argsort(logits, dim=-1, descending=True).cpu().tolist())
                del texts, batch, attention, logits, lengths
                if (
                    batch_index == 1
                    or batch_index % 100 == 0
                    or batch_index == total_test_batches
                ):
                    print(
                        f"[t5] {method} seed={seed} test batch "
                        f"{batch_index}/{total_test_batches} "
                        f"elapsed={(time.perf_counter() - inference_started) / 60.0:.1f}min",
                        flush=True,
                    )
        _sync_device(device)
        inference_seconds = time.perf_counter() - inference_started
        details = {
            "retrieval": retrieval_details,
            "retrieval_test": test_retrieval,
            "train_seconds": train_seconds,
            "inference_seconds": inference_seconds,
            "average_input_tokens": float(np.mean(token_lengths)) if token_lengths else 0.0,
            "input_tokens_test": int(sum(token_lengths)),
            "truncated_test_prompts": int(truncated),
            "test_prompts": len(test_prompts),
            "warmup_enabled": warmup_enabled,
            "warmup_cases": warmup_cases,
        }
        return rankings, last_loss, details
    finally:
        if train_prompts is not None:
            train_prompts.close()
        if test_prompts is not None:
            test_prompts.close()
        if retriever is not None:
            retriever.clear_runtime_caches()
        gc.collect()
        # The prompt stores contain large binary files, so rmdir() is not
        # sufficient after close(). Remove the per-run directory on every
        # normal return or Python exception.
        shutil.rmtree(prompt_dir, ignore_errors=True)


def _mask_seen_logits(logits: torch.Tensor, histories: Sequence[Sequence[int]]) -> torch.Tensor:
    logits = logits.clone()
    for row, history in enumerate(histories):
        for item in set(history):
            if 0 <= item < logits.size(-1):
                logits[row, item] = -torch.inf
    return logits


def _postprocess_rankings(
    rankings: Sequence[Sequence[int]],
    test_examples: Sequence[SequenceExample],
    item_to_id: dict[str, int],
    num_items: int,
    *,
    exclude_seen: bool,
) -> list[list[int]]:
    output: list[list[int]] = []
    for ranking, example in zip(rankings, test_examples):
        seen = {item_to_id[x] for x in example.history if x in item_to_id} if exclude_seen else set()
        row = [int(item) for item in ranking if 0 <= int(item) < num_items and int(item) not in seen]
        missing = [item for item in range(num_items) if item not in seen and item not in row]
        output.append(row + missing)
    return output


def _slice_report(
    ranked_items: list[list[str]],
    test_examples: Sequence[SequenceExample],
    targets: list[str],
    train_counts: Counter[str],
    course_count: int,
    *,
    course_prerequisites: dict[str, set[str]] | None = None,
    course_concepts: dict[str, list[str]] | None = None,
) -> dict[str, dict[str, Any]]:
    history_lengths = sorted(len(example.history) for example in test_examples)
    if history_lengths:
        short_threshold = int(np.floor(np.quantile(history_lengths, 0.25)))
        long_threshold = int(np.ceil(np.quantile(history_lengths, 0.75)))
        short_threshold = max(1, short_threshold)
        long_threshold = max(short_threshold, long_threshold)
    else:
        short_threshold = 0
        long_threshold = 0
    nonzero = sorted(value for value in train_counts.values() if value > 0)
    if nonzero:
        tail_index = max(0, min(len(nonzero) - 1, int(np.floor(0.2 * (len(nonzero) - 1)))))
        tail_threshold = nonzero[tail_index]
    else:
        tail_threshold = 0
    long_tail = {course for course, count in train_counts.items() if 0 < count <= tail_threshold}
    masks = {
        "short_history": [
            bool(history_lengths) and len(example.history) <= short_threshold
            for example in test_examples
        ],
        "long_history": [
            bool(history_lengths) and len(example.history) >= long_threshold
            for example in test_examples
        ],
        "long_tail_target": [target in long_tail for target in targets],
        "cold_start_target": [train_counts.get(target, 0) == 0 for target in targets],
        "cold_user": [len(example.history) <= 2 for example in test_examples],
    }
    result: dict[str, dict[str, Any]] = {}
    for name, mask in masks.items():
        selected = [index for index, value in enumerate(mask) if value]
        subset_rankings = [ranked_items[index] for index in selected]
        subset_targets = [targets[index] for index in selected]
        metrics = ranking_metrics(subset_rankings, subset_targets) if selected else {}
        if selected:
            metrics.update(
                prerequisite_consistency_with_coverage(
                    subset_rankings,
                    [test_examples[index].history for index in selected],
                    course_prerequisites=course_prerequisites,
                    course_concepts=course_concepts,
                    k=10,
                )
            )
        else:
            metrics.update({"PCR@10": 0.0, "PCR_coverage@10": 0.0})
        result[name] = {
            "examples": len(selected),
            "target_courses": len(set(subset_targets)),
            "course_catalog": course_count,
            "train_tail_threshold": tail_threshold,
            "history_threshold": short_threshold if name == "short_history" else long_threshold if name == "long_history" else None,
            **metrics,
        }
    return result


def _bpr_rank(
    model: BPRMF,
    examples: Sequence[SequenceExample],
    item_to_id: dict[str, int],
    device: torch.device,
    *,
    exclude_seen: bool,
) -> list[list[int]]:
    model.eval()
    user_ids = torch.tensor([example.user_index for example in examples], dtype=torch.long, device=device)
    with torch.no_grad():
        scores = model.score_all(user_ids)
        if exclude_seen:
            for row, example in enumerate(examples):
                for course in set(example.history):
                    item = item_to_id.get(course)
                    if item is not None:
                        scores[row, item] = -torch.inf
        return torch.argsort(scores, dim=-1, descending=True).cpu().tolist()


def run(config: dict[str, Any]) -> dict[str, Any]:
    method_original = str(config.get("method", "mostpop")).lower()
    if method_original not in SUPPORTED_METHODS:
        supported = ", ".join(sorted(SUPPORTED_METHODS))
        raise ValueError(f"未知 method={method_original}；可选方法: {supported}")
    config = _effective_config(config)
    seed = int(config.get("seed", 2026))
    seed_everything(seed)
    if torch.cuda.is_available():
        torch.cuda.reset_peak_memory_stats()
    data_cfg = config.get("data", {}) or {}
    if not isinstance(data_cfg, dict):
        raise ValueError("data 配置必须是 YAML 对象")
    data_name = str(data_cfg.get("name", "synthetic")).lower()
    if data_name == "moocubex":
        data_root = data_cfg.get("root")
        if not data_root:
            raise ValueError("moocubex 数据必须配置 data.root")
        target_fields = data_cfg.get("target_fields", data_cfg.get("target_field", ["计算机科学与技术"]))
        if isinstance(target_fields, str):
            target_fields = [target_fields]
        dataset = load_moocubex(
            data_root,
            int(data_cfg.get("min_sequence_length", 3)),
            int(data_cfg.get("course_min_interactions", 5)),
            target_fields,
            prepare=bool(data_cfg.get("prepare", True)),
        )
    elif data_name == "synthetic":
        users = int(data_cfg.get("users", 72))
        courses = int(data_cfg.get("courses", 24))
        if users < 1 or courses < 2:
            raise ValueError("synthetic 数据至少需要 users>=1 且 courses>=2")
        dataset = make_synthetic_dataset(
            seed,
            users,
            courses,
        )
    else:
        raise ValueError(f"未知 data.name={data_name!r}；可选值为 synthetic 或 moocubex")

    if not dataset.sequences:
        raise ValueError(
            f"{data_name} 数据经过过滤后没有可用用户序列；请降低过滤阈值或检查数据字段"
        )

    item_to_id, id_to_item = build_item_mapping(dataset)
    if len(id_to_item) < 2:
        raise ValueError("可用课程数少于 2，无法进行下一课程排序实验")
    split_strategy = str(
        config.get("split_strategy", data_cfg.get("split_strategy", "leave_two_out"))
    )
    split_cfg = config.get("split", {}) or {}
    bundle = make_split_bundle(
        dataset.sequences,
        dataset.timestamps,
        strategy=split_strategy,
        train_ratio=float(split_cfg.get("train_ratio", 0.8)),
        val_ratio=float(split_cfg.get("val_ratio", 0.1)),
    )
    method = _canonical_method(method_original)
    model_cfg = config.get("model", {}) or {}
    train_cfg = config.get("training", {}) or {}
    if not isinstance(model_cfg, dict) or not isinstance(train_cfg, dict):
        raise ValueError("model 和 training 配置必须是 YAML 对象")
    is_t5 = str(model_cfg.get("backend", "lite")).lower() == "t5" and method in {
        "p5",
        "kp4sr",
        "drag_kp4sr",
    }
    if is_t5:
        # The T5 path only needs target IDs.  Keeping a second integer copy of
        # every history would duplicate the string examples while prompts are
        # being built.
        encoded_split = None
        t5_train_target_ids: list[int] | None = [
            item_to_id[example.target] for example in bundle.examples["train"]
        ]
        raw_train = bundle.examples["train"]
        raw_test = bundle.examples["test"]
    else:
        encode = lambda example: (
            [item_to_id[x] for x in example.history],
            item_to_id[example.target],
        )
        encoded_split = {
            name: [encode(example) for example in examples]
            for name, examples in bundle.examples.items()
        }
        t5_train_target_ids = None
        raw_train = encoded_split["train"]
        raw_test = encoded_split["test"]
    batch_size = int(train_cfg.get("batch_size", 64))
    epochs = int(train_cfg.get("epochs", 3))
    learning_rate = float(train_cfg.get("lr", 1e-3))
    if batch_size <= 0:
        raise ValueError("training.batch_size 必须大于 0")
    if epochs < 0:
        raise ValueError("training.epochs 不能为负数")
    if learning_rate <= 0:
        raise ValueError("training.lr 必须大于 0")
    device = _resolve_device(
        config.get("device", "cuda" if torch.cuda.is_available() else "cpu")
    )
    started = time.perf_counter()
    train_loss: float | None = None
    details: dict[str, Any] = {}
    train_seconds = 0.0
    inference_seconds = 0.0

    efficiency_cfg = config.get("efficiency", {}) or {}
    warmup_enabled = bool(efficiency_cfg.get("warmup", True))
    default_batch_size = int(train_cfg.get("batch_size", 64))
    warmup_cases = min(
        len(raw_test),
        max(1, int(efficiency_cfg.get("warmup_cases", default_batch_size))),
    ) if raw_test else 0

    def timed_inference(
        function: Callable[[], list[list[int]]],
        warmup: Callable[[], Any] | None = None,
    ) -> tuple[list[list[int]], float]:
        if warmup_enabled and warmup is not None and warmup_cases:
            warmup()
        _sync_device(device)
        inference_started = time.perf_counter()
        value = function()
        _sync_device(device)
        return value, time.perf_counter() - inference_started

    if method == "mostpop":
        fit_started = time.perf_counter()
        train_sequences = [[item_to_id[x] for x in sequence] for sequence in bundle.train_sequences]
        model = MostPop().fit(
            raw_train,
            len(id_to_item),
            train_sequences=train_sequences,
        )
        train_seconds = time.perf_counter() - fit_started
        ranked, inference_seconds = timed_inference(
            lambda: [model.rank(history) for history, _ in raw_test],
            lambda: [model.rank(history) for history, _ in raw_test[:warmup_cases]],
        )
    elif method == "markov":
        fit_started = time.perf_counter()
        train_sequences = [[item_to_id[x] for x in sequence] for sequence in bundle.train_sequences]
        model = MarkovChain().fit(
            raw_train,
            len(id_to_item),
            train_sequences=train_sequences,
        )
        train_seconds = time.perf_counter() - fit_started
        ranked, inference_seconds = timed_inference(
            lambda: [model.rank(history) for history, _ in raw_test],
            lambda: [model.rank(history) for history, _ in raw_test[:warmup_cases]],
        )
    elif method == "itemknn":
        fit_started = time.perf_counter()
        train_sequences = [[item_to_id[x] for x in sequence] for sequence in bundle.train_sequences]
        model = ItemKNN().fit(train_sequences, len(id_to_item))
        train_seconds = time.perf_counter() - fit_started
        ranked, inference_seconds = timed_inference(
            lambda: [model.rank(history) for history, _ in raw_test],
            lambda: [model.rank(history) for history, _ in raw_test[:warmup_cases]],
        )
    elif method == "bprmf":
        fit_started = time.perf_counter()
        user_positive: list[tuple[int, int]] = []
        seen_by_user: list[set[int]] = []
        for user_index, sequence in enumerate(bundle.train_sequences):
            train_items = [item_to_id[x] for x in sequence if x in item_to_id]
            seen_by_user.append(set(train_items))
            user_positive.extend((user_index, item) for item in train_items)
        model = BPRMF(len(dataset.sequences), len(id_to_item), int(model_cfg.get("hidden", 64))).to(device)
        opt = torch.optim.AdamW(model.parameters(), lr=float(train_cfg.get("lr", 1e-3)))
        generator = torch.Generator().manual_seed(seed)
        if user_positive:
            for _ in range(int(train_cfg.get("epochs", 10))):
                order = torch.randperm(len(user_positive), generator=generator).tolist()
                for start_index in range(0, len(order), int(train_cfg.get("batch_size", 256))):
                    records = [
                        user_positive[i]
                        for i in order[start_index : start_index + int(train_cfg.get("batch_size", 256))]
                    ]
                    negatives: list[int] = []
                    sampled_records: list[tuple[int, int]] = []
                    for user_index, positive in records:
                        available = [item for item in range(len(id_to_item)) if item not in seen_by_user[user_index]]
                        if not available:
                            available = [item for item in range(len(id_to_item)) if item != positive]
                        if not available:
                            continue
                        sampled_records.append((user_index, positive))
                        negatives.append(available[int(torch.randint(len(available), (1,), generator=generator))])
                    if not sampled_records:
                        continue
                    users = torch.tensor([x[0] for x in sampled_records], dtype=torch.long)
                    positives = torch.tensor([x[1] for x in sampled_records], dtype=torch.long)
                    neg = torch.tensor(negatives, dtype=torch.long)
                    model.train()
                    opt.zero_grad(set_to_none=True)
                    loss = model.bpr_loss(users.to(device), positives.to(device), neg.to(device))
                    loss.backward()
                    opt.step()
                    train_loss = float(loss.detach())
        _sync_device(device)
        train_seconds = time.perf_counter() - fit_started
        ranked, inference_seconds = timed_inference(
            lambda: _bpr_rank(
                model,
                bundle.examples["test"],
                item_to_id,
                device,
                exclude_seen=bool(config.get("exclude_seen", True)),
            ),
            lambda: _bpr_rank(
                model,
                bundle.examples["test"][:warmup_cases],
                item_to_id,
                device,
                exclude_seen=bool(config.get("exclude_seen", True)),
            ),
        )
    elif method in {"gru4rec", "sasrec", "bert4rec"}:
        fit_started = time.perf_counter()
        kind = {"gru4rec": "gru", "sasrec": "sasrec", "bert4rec": "bert"}[method]
        model = SequenceRanker(
            len(id_to_item),
            int(model_cfg.get("hidden", 64)),
            kind,
            int(model_cfg.get("max_len", 50)),
        )
        if kind == "bert":
            train_sequences = [[item_to_id[x] for x in sequence] for sequence in bundle.train_sequences]
            train_loss = _train_bert(
                model,
                train_sequences,
                device,
                int(train_cfg.get("epochs", 3)),
                int(train_cfg.get("batch_size", 64)),
                float(train_cfg.get("lr", 1e-3)),
                seed,
            )
            _sync_device(device)
            train_seconds = time.perf_counter() - fit_started
            ranked, inference_seconds = timed_inference(
                lambda: _rank_neural(model, raw_test, device, default_batch_size),
                lambda: _rank_neural(
                    model, raw_test[:warmup_cases], device, default_batch_size
                ),
            )
        else:
            train_loss = _train_neural(
                model,
                raw_train,
                device,
                int(train_cfg.get("epochs", 3)),
                int(train_cfg.get("batch_size", 64)),
                float(train_cfg.get("lr", 1e-3)),
                max_len=int(model_cfg.get("max_len", 50)),
                seed=seed,
            )
            _sync_device(device)
            train_seconds = time.perf_counter() - fit_started
            ranked, inference_seconds = timed_inference(
                lambda: _rank_neural(
                    model,
                    raw_test,
                    device,
                    default_batch_size,
                    max_len=int(model_cfg.get("max_len", 50)),
                ),
                lambda: _rank_neural(
                    model,
                    raw_test[:warmup_cases],
                    device,
                    default_batch_size,
                    max_len=int(model_cfg.get("max_len", 50)),
                ),
            )
    elif method in {"p5", "kp4sr", "drag_kp4sr"}:
        backend = str(model_cfg.get("backend", "lite")).lower()
        if backend == "t5":
            if t5_train_target_ids is None:
                raise RuntimeError("T5 后端未准备训练目标 ID")
            ranked, train_loss, details = _run_t5(
                {**config, "method": method},
                dataset,
                bundle,
                t5_train_target_ids,
                item_to_id,
                id_to_item,
                device,
            )
            train_seconds = float(details.get("train_seconds", 0.0))
            inference_seconds = float(details.get("inference_seconds", 0.0))
        else:
            fit_started = time.perf_counter()
            retrieval_cfg = _retrieval_config({**config, "method": method_original})
            need_dense = str(retrieval_cfg.get("evidence_mode", retrieval_cfg.get("mode", "hybrid"))).lower() in {
                "dense",
                "hybrid",
            }
            retriever = _make_retriever(dataset, config, device, need_dense=need_dense)
            model = DRAGLiteRanker(len(id_to_item), int(model_cfg.get("hidden", 64)))
            evidence_mode = str(retrieval_cfg.get("evidence_mode", "none" if method == "p5" else retrieval_cfg.get("mode", "hybrid")))
            evidence_fn = _make_evidence_fn(
                retriever,
                id_to_item,
                item_to_id,
                model.pad_id,
                int(retrieval_cfg.get("top_k", 5)),
                str(retrieval_cfg.get("mode", "hybrid")),
                seed,
                use_frontier=bool(retrieval_cfg.get("use_frontier", True)),
                evidence_mode=evidence_mode,
            )
            train_loss = _train_neural(
                model,
                raw_train,
                device,
                int(train_cfg.get("epochs", 3)),
                int(train_cfg.get("batch_size", 64)),
                float(train_cfg.get("lr", 1e-3)),
                evidence_fn=evidence_fn,
                max_len=int(model_cfg.get("max_len", 50)),
                seed=seed,
            )
            _sync_device(device)
            train_seconds = time.perf_counter() - fit_started
            after_train_retrieval_snapshot = _retrieval_snapshot(retriever.stats)
            warmup_retrieval_snapshot: dict[str, Any] | None = None

            def warmup_drag() -> None:
                nonlocal warmup_retrieval_snapshot
                _rank_neural(
                    model,
                    raw_test[:warmup_cases],
                    device,
                    default_batch_size,
                    evidence_fn=evidence_fn,
                    max_len=int(model_cfg.get("max_len", 50)),
                )
                warmup_retrieval_snapshot = _retrieval_snapshot(retriever.stats)
                cache_clear = getattr(evidence_fn, "cache_clear", None)
                if cache_clear:
                    cache_clear()
                retriever._search_cache.clear()

            ranked, inference_seconds = timed_inference(
                lambda: _rank_neural(
                    model,
                    raw_test,
                    device,
                    default_batch_size,
                    evidence_fn=evidence_fn,
                    max_len=int(model_cfg.get("max_len", 50)),
                ),
                warmup_drag,
            )
            details["retrieval"] = dict(vars(retriever.stats))
            test_retrieval_before = warmup_retrieval_snapshot or after_train_retrieval_snapshot
            test_retrieval_after = _retrieval_snapshot(retriever.stats)
            details["retrieval_test"] = _retrieval_delta(
                test_retrieval_after,
                test_retrieval_before,
            )
    else:
        raise ValueError(f"未知 method: {method_original}")

    exclude_seen = bool(config.get("exclude_seen", True))
    ranked = _postprocess_rankings(
        ranked,
        bundle.examples["test"],
        item_to_id,
        len(id_to_item),
        exclude_seen=exclude_seen,
    )
    elapsed = time.perf_counter() - started
    ranked_items = [[id_to_item[i] for i in row] for row in ranked]
    targets = [example.target for example in bundle.examples["test"]]
    histories = [example.history for example in bundle.examples["test"]]
    metrics = ranking_metrics(ranked_items, targets)
    course_prerequisites = build_course_prerequisites(dataset.course_concepts, dataset.prerequisite_edges)
    metrics.update(
        prerequisite_consistency_with_coverage(
            ranked_items,
            histories,
            course_prerequisites=course_prerequisites,
            course_concepts=dataset.course_concepts,
            k=10,
        )
    )
    train_counts = Counter(course for sequence in bundle.train_sequences for course in sequence)
    slices = _slice_report(
        ranked_items,
        bundle.examples["test"],
        targets,
        train_counts,
        len(id_to_item),
        course_prerequisites=course_prerequisites,
        course_concepts=dataset.course_concepts,
    )
    example_records = per_example_metrics(
        ranked_items,
        targets,
        histories,
        course_prerequisites=course_prerequisites,
        course_concepts=dataset.course_concepts,
    )
    for record, example in zip(example_records, bundle.examples["test"]):
        record["user_index"] = example.user_index
    for case_index, record in enumerate(example_records):
        record["case_index"] = case_index
    effective_retrieval = _retrieval_config(config)
    default_retrieval = {
        "index_build_seconds": 0.0,
        "search_seconds": 0.0,
        "path_seconds": 0.0,
        "calls": 0,
        "fallback_calls": 0,
        "empty_calls": 0,
        "cache_hits": 0,
        "dense_backend": "none",
        "dense_fallback": False,
    }
    retrieval_details = {**default_retrieval, **dict(details.get("retrieval", {}))}
    retrieval_test_details = {
        **default_retrieval,
        **dict(details.get("retrieval_test", {})),
    }
    retrieval_total_seconds = float(retrieval_details.get("search_seconds", 0.0)) + float(
        retrieval_details.get("path_seconds", 0.0)
    )
    retrieval_test_total_seconds = float(retrieval_test_details.get("search_seconds", 0.0)) + float(
        retrieval_test_details.get("path_seconds", 0.0)
    )
    result: dict[str, Any] = {
        "method": method_original,
        "canonical_method": method,
        "backend": model_cfg.get("backend", "n/a"),
        "seed": seed,
        "device": str(device),
        "data_name": data_name,
        "data_stats": dataset.stats,
        "split_strategy": bundle.strategy,
        "split_cutoffs": bundle.cutoffs,
        "exclude_seen": exclude_seen,
        "config": config,
        "ablation": {
            "evidence_mode": effective_retrieval.get("evidence_mode", "none"),
            "use_frontier": bool(effective_retrieval.get("use_frontier", True)),
            "tree_mask": model_cfg.get("tree_mask", None),
            "tree_mask_applicable": bool(
                str(model_cfg.get("backend", "lite")).lower() == "t5"
                and method in {"kp4sr", "drag_kp4sr"}
            ),
        },
        "num_users": len(dataset.sequences),
        "num_items": len(id_to_item),
        "train_pairs": len(bundle.examples["train"]),
        "validation_cases": len(bundle.examples["val"]),
        "test_cases": len(bundle.examples["test"]),
        "train_interactions": sum(map(len, bundle.train_sequences)),
        "train_loss_last": train_loss,
        "train_seconds": train_seconds,
        "inference_seconds": inference_seconds,
        "elapsed_seconds": elapsed,
        "peak_cuda_mb": torch.cuda.max_memory_allocated() / 1024**2 if device.type == "cuda" else 0.0,
        **metrics,
        "slices": slices,
        "retrieval": retrieval_details,
        "retrieval_test": retrieval_test_details,
        "efficiency": {
            "training_seconds": train_seconds,
            "inference_seconds": inference_seconds,
            "inference_ms_per_case": 1000.0 * inference_seconds / max(1, len(raw_test)),
            "warmup_enabled": warmup_enabled,
            "warmup_cases": warmup_cases,
            "index_build_seconds": float(details.get("retrieval", {}).get("index_build_seconds", 0.0)),
            "retrieval_seconds": float(retrieval_details.get("search_seconds", 0.0)),
            "retrieval_path_seconds": float(retrieval_details.get("path_seconds", 0.0)),
            "retrieval_total_seconds": retrieval_total_seconds,
            "retrieval_test_seconds": float(retrieval_test_details.get("search_seconds", 0.0)),
            "retrieval_test_path_seconds": float(retrieval_test_details.get("path_seconds", 0.0)),
            "retrieval_test_total_seconds": retrieval_test_total_seconds,
            "retrieval_ms_per_test_case": 1000.0 * retrieval_test_total_seconds / max(1, len(raw_test)),
            "retrieval_seconds_per_test_call": retrieval_test_total_seconds / max(
                1, int(retrieval_test_details.get("calls", 0))
            ),
            "retrieval_calls": int(retrieval_details.get("calls", 0)),
            "retrieval_fallbacks": int(retrieval_details.get("fallback_calls", 0)),
            "retrieval_cache_hits": int(retrieval_details.get("cache_hits", 0)),
            "retrieval_test_calls": int(retrieval_test_details.get("calls", 0)),
            "retrieval_test_fallbacks": int(retrieval_test_details.get("fallback_calls", 0)),
            "dense_backend": retrieval_details.get("dense_backend", "none"),
            "dense_fallback": bool(retrieval_details.get("dense_fallback", False)),
            "average_input_tokens": float(details.get("average_input_tokens", 0.0)),
            "input_tokens_test": int(details.get("input_tokens_test", 0)),
            "truncated_test_prompts": int(details.get("truncated_test_prompts", 0)),
        },
        "per_example": example_records,
    }
    return result


def save_result(result: dict[str, Any], output_dir: str | Path) -> Path:
    output_dir = Path(output_dir)
    output_dir.mkdir(parents=True, exist_ok=True)
    path = output_dir / f"{result['method']}_seed{result['seed']}.json"
    path.write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
    return path


def aggregate_results(
    output_dir: str | Path,
    *,
    run_keys: set[tuple[str, str]] | None = None,
) -> Path:
    output_dir = Path(output_dir)
    rows: list[dict[str, Any]] = []
    files = sorted(set(output_dir.glob("*_seed*.json")).union(output_dir.glob("*/*_seed*.json")))
    for path in files:
        try:
            value = json.loads(path.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError):
            continue
        if isinstance(value, dict) and value.get("method"):
            key = (str(value.get("method")), str(value.get("seed", "")))
            if run_keys is not None and key not in run_keys:
                continue
            rows.append(value)
    path = output_dir / "results_summary.csv"
    if rows:
        keys = sorted({key for row in rows for key, value in row.items() if not isinstance(value, (dict, list))})
        with path.open("w", newline="", encoding="utf-8-sig") as f:
            writer = csv.DictWriter(f, fieldnames=keys, extrasaction="ignore")
            writer.writeheader()
            writer.writerows(rows)
    else:
        path.write_text("", encoding="utf-8-sig")
    return path


def aggregate_seed_results(
    output_dir: str | Path, reference_method: str = "drag_kp4sr"
) -> dict[str, str]:
    from .statistics import write_aggregates

    return write_aggregates(output_dir, reference_method=reference_method)
