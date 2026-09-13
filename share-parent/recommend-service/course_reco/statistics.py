from __future__ import annotations

"""Dependency-light aggregation and paired significance utilities.

The experiment runner stores one JSON record per method and seed.  This module
keeps aggregation outside the model code so that a failed optional model never
prevents already completed results from being summarized.
"""

import csv
import json
import math
from collections import defaultdict
from pathlib import Path
from typing import Any, Iterable, Sequence

import numpy as np


DEFAULT_METRICS = ("HR@5", "HR@10", "NDCG@5", "NDCG@10", "MRR@10", "PCR@10")

PER_CASE_METRIC_NAMES = {
    "HR@5": "hit@5",
    "HR@10": "hit@10",
    "NDCG@5": "ndcg@5",
    "NDCG@10": "ndcg@10",
    "MRR@10": "mrr@10",
    "PCR@10": "pcr@10",
}


def load_result_files(
    output_dir: str | Path,
    *,
    run_keys: set[tuple[str, str]] | None = None,
) -> list[dict[str, Any]]:
    output_dir = Path(output_dir)
    rows: list[dict[str, Any]] = []
    files = sorted(set(output_dir.glob("*_seed*.json")).union(output_dir.glob("*/*_seed*.json")))
    for path in files:
        try:
            value = json.loads(path.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError):
            continue
        if isinstance(value, dict) and value.get("method") and "per_example" in value:
            key = (str(value.get("method")), str(value.get("seed", "")))
            if run_keys is not None and key not in run_keys:
                continue
            value["_path"] = str(path)
            rows.append(value)
    return rows


def _write_csv(path: Path, rows: Sequence[dict[str, Any]]) -> Path:
    path.parent.mkdir(parents=True, exist_ok=True)
    if not rows:
        path.write_text("", encoding="utf-8")
        return path
    fields = sorted({key for row in rows for key in row})
    with path.open("w", newline="", encoding="utf-8-sig") as handle:
        writer = csv.DictWriter(handle, fieldnames=fields, extrasaction="ignore")
        writer.writeheader()
        writer.writerows(rows)
    return path


def seed_summary(
    results: Sequence[dict[str, Any]], metrics: Iterable[str] = DEFAULT_METRICS
) -> list[dict[str, Any]]:
    metrics = tuple(metrics)
    grouped: dict[tuple[str, str], list[float]] = defaultdict(list)
    for result in results:
        method = str(result.get("method", ""))
        for metric in metrics:
            value = result.get(metric)
            if isinstance(value, (int, float)) and math.isfinite(float(value)):
                grouped[(method, metric)].append(float(value))
    rows: list[dict[str, Any]] = []
    for (method, metric), values in sorted(grouped.items()):
        array = np.asarray(values, dtype=np.float64)
        rows.append(
            {
                "method": method,
                "metric": metric,
                "n_seeds": int(array.size),
                "mean": float(array.mean()) if array.size else 0.0,
                "std": float(array.std(ddof=1)) if array.size > 1 else 0.0,
                "min": float(array.min()) if array.size else 0.0,
                "max": float(array.max()) if array.size else 0.0,
                "seeds": ",".join(str(result.get("seed")) for result in results if result.get("method") == method),
            }
        )
    return rows


def _exact_sign_flip_pvalue(differences: np.ndarray) -> float:
    n = int(differences.size)
    observed = abs(float(differences.mean()))
    if observed <= 1e-15:
        return 1.0
    total = 1 << n
    exceed = 0
    for mask in range(total):
        signs = np.where(
            ((mask >> np.arange(n, dtype=np.int64)) & 1) == 1,
            1.0,
            -1.0,
        )
        if abs(float(np.mean(signs * differences))) >= observed - 1e-15:
            exceed += 1
    return exceed / total


def paired_test(
    left: Sequence[float],
    right: Sequence[float],
    *,
    exact_max_n: int = 16,
) -> dict[str, float | int | str]:
    """Run a paired test without requiring SciPy.

    Small samples use an exact sign-flip permutation test.  Large test sets
    use the paired-t statistic with a normal-tail approximation; this avoids
    allocating an ``n_permutations x n_cases`` matrix for tens of thousands of
    MOOCCubeX cases while retaining a paired analysis.
    """
    if len(left) != len(right):
        raise ValueError("paired_test 输入长度不一致")
    differences = np.asarray(left, dtype=np.float64) - np.asarray(right, dtype=np.float64)
    differences = differences[np.isfinite(differences)]
    n = int(differences.size)
    if n == 0:
        return {
            "n": 0,
            "mean_diff": 0.0,
            "std_diff": 0.0,
            "effect_size_dz": 0.0,
            "p_value": 1.0,
            "test": "not_available",
        }
    mean_diff = float(differences.mean())
    std_diff = float(differences.std(ddof=1)) if n > 1 else 0.0
    effect = mean_diff / std_diff if std_diff > 1e-12 else (0.0 if abs(mean_diff) < 1e-12 else math.copysign(math.inf, mean_diff))
    if n <= exact_max_n:
        p_value = _exact_sign_flip_pvalue(differences)
        test_name = "exact_sign_flip"
    elif std_diff <= 1e-12:
        p_value = 1.0 if abs(mean_diff) < 1e-12 else 0.0
        test_name = "paired_t_degenerate"
    else:
        t_value = mean_diff / (std_diff / math.sqrt(n))
        p_value = math.erfc(abs(t_value) / math.sqrt(2.0))
        test_name = "paired_t_normal_approx"
    return {
        "n": n,
        "mean_diff": mean_diff,
        "std_diff": std_diff,
        "effect_size_dz": float(effect),
        "p_value": float(min(1.0, max(0.0, p_value))),
        "test": test_name,
    }


def holm_bonferroni(p_values: Sequence[float]) -> list[float]:
    """Return Holm step-down adjusted p-values in the original order."""
    if not p_values:
        return []
    order = sorted(range(len(p_values)), key=lambda index: float(p_values[index]))
    adjusted = [1.0] * len(p_values)
    running = 0.0
    count = len(p_values)
    for rank, index in enumerate(order):
        candidate = min(1.0, (count - rank) * float(p_values[index]))
        running = max(running, candidate)
        adjusted[index] = running
    return adjusted


def _per_example_values(
    result: dict[str, Any], metric: str
) -> dict[tuple[int, str], float]:
    metric = PER_CASE_METRIC_NAMES.get(metric, metric)
    values: dict[tuple[int, str], float] = {}
    for fallback_index, row in enumerate(result.get("per_example", [])):
        if not isinstance(row, dict):
            continue
        case_index = int(row.get("case_index", fallback_index))
        target = str(row.get("target", ""))
        value = row.get(metric)
        if isinstance(value, (int, float)) and math.isfinite(float(value)):
            values[(case_index, target)] = float(value)
    return values


def significance_rows(
    results: Sequence[dict[str, Any]],
    *,
    reference_method: str = "drag_kp4sr",
    metrics: Iterable[str] = DEFAULT_METRICS,
    alpha: float = 0.05,
) -> list[dict[str, Any]]:
    """Compare every method with a reference on matched per-case records."""
    metrics = tuple(metrics)
    by_method_seed: dict[tuple[str, str], dict[str, Any]] = {}
    methods: set[str] = set()
    seeds: set[str] = set()
    for result in results:
        method = str(result.get("method", ""))
        seed = str(result.get("seed", ""))
        by_method_seed[(method, seed)] = result
        methods.add(method)
        seeds.add(seed)
    if reference_method not in methods:
        return []

    rows: list[dict[str, Any]] = []
    for method in sorted(methods):
        if method == reference_method:
            continue
        for metric in metrics:
            left: list[float] = []
            right: list[float] = []
            used_seeds: set[str] = set()
            for seed in sorted(seeds):
                candidate = by_method_seed.get((method, seed))
                reference = by_method_seed.get((reference_method, seed))
                if candidate is None or reference is None:
                    continue
                candidate_values = _per_example_values(candidate, metric)
                reference_values = _per_example_values(reference, metric)
                keys = sorted(set(candidate_values) & set(reference_values))
                left.extend(candidate_values[key] for key in keys)
                right.extend(reference_values[key] for key in keys)
                if keys:
                    used_seeds.add(seed)
            result = paired_test(left, right)
            result.update(
                {
                    "method": method,
                    "reference": reference_method,
                    "metric": metric,
                    "n_seeds": len(used_seeds),
                    "seeds": ",".join(sorted(used_seeds)),
                    "alpha": alpha,
                }
            )
            rows.append(result)

    for metric in metrics:
        indices = [index for index, row in enumerate(rows) if row["metric"] == metric]
        adjusted = holm_bonferroni([float(rows[index]["p_value"]) for index in indices])
        for index, value in zip(indices, adjusted):
            rows[index]["p_holm"] = value
            rows[index]["significant_holm"] = bool(value < alpha)
    return rows


def slice_summary(
    results: Sequence[dict[str, Any]], metrics: Iterable[str] = DEFAULT_METRICS
) -> list[dict[str, Any]]:
    """Flatten per-run slice metrics for short/long-history and cold-start tables."""
    metric_names = tuple(metrics)
    grouped: dict[tuple[str, str, str], list[float]] = defaultdict(list)
    examples: dict[tuple[str, str], list[int]] = defaultdict(list)
    for result in results:
        method = str(result.get("method", ""))
        for slice_name, values in (result.get("slices", {}) or {}).items():
            if not isinstance(values, dict):
                continue
            key = (method, str(slice_name))
            examples[key].append(int(values.get("examples", 0)))
            for metric in metric_names:
                value = values.get(metric)
                if isinstance(value, (int, float)) and math.isfinite(float(value)):
                    grouped[(method, str(slice_name), metric)].append(float(value))
    rows: list[dict[str, Any]] = []
    for (method, slice_name, metric), values in sorted(grouped.items()):
        array = np.asarray(values, dtype=np.float64)
        count = examples[(method, slice_name)]
        rows.append(
            {
                "method": method,
                "slice": slice_name,
                "metric": metric,
                "n_runs": int(array.size),
                "mean": float(array.mean()),
                "std": float(array.std(ddof=1)) if array.size > 1 else 0.0,
                "mean_examples": float(np.mean(count)) if count else 0.0,
            }
        )
    return rows


def efficiency_rows(results: Sequence[dict[str, Any]]) -> list[dict[str, Any]]:
    rows: list[dict[str, Any]] = []
    fields = (
        "training_seconds",
        "inference_seconds",
        "inference_ms_per_case",
        "warmup_enabled",
        "warmup_cases",
        "index_build_seconds",
        "retrieval_seconds",
        "retrieval_path_seconds",
        "retrieval_total_seconds",
        "retrieval_test_seconds",
        "retrieval_test_path_seconds",
        "retrieval_test_total_seconds",
        "retrieval_ms_per_test_case",
        "retrieval_seconds_per_test_call",
        "retrieval_calls",
        "retrieval_fallbacks",
        "retrieval_cache_hits",
        "retrieval_test_calls",
        "retrieval_test_fallbacks",
        "average_input_tokens",
        "input_tokens_test",
        "truncated_test_prompts",
        "dense_backend",
        "dense_fallback",
    )
    for result in results:
        row: dict[str, Any] = {
            "method": result.get("method", ""),
            "seed": result.get("seed", ""),
        }
        values = result.get("efficiency", {}) or {}
        for field_name in fields:
            row[field_name] = values.get(field_name, "")
        rows.append(row)
    return rows


def write_aggregates(
    output_dir: str | Path,
    *,
    reference_method: str = "drag_kp4sr",
    metrics: Iterable[str] = DEFAULT_METRICS,
    run_keys: set[tuple[str, str]] | None = None,
) -> dict[str, str]:
    output_dir = Path(output_dir)
    results = load_result_files(output_dir, run_keys=run_keys)
    summary_rows = seed_summary(results, metrics)
    significance = significance_rows(
        results, reference_method=reference_method, metrics=metrics
    )
    slices = slice_summary(results, metrics)
    efficiency = efficiency_rows(results)
    summary_path = _write_csv(output_dir / "seed_summary.csv", summary_rows)
    significance_path = _write_csv(output_dir / "significance.csv", significance)
    slice_path = _write_csv(output_dir / "slice_summary.csv", slices)
    efficiency_path = _write_csv(output_dir / "efficiency_summary.csv", efficiency)
    (output_dir / "seed_summary.json").write_text(
        json.dumps(summary_rows, ensure_ascii=False, indent=2), encoding="utf-8"
    )
    (output_dir / "significance.json").write_text(
        json.dumps(significance, ensure_ascii=False, indent=2), encoding="utf-8"
    )
    return {
        "seed_summary": str(summary_path),
        "significance": str(significance_path),
        "slice_summary": str(slice_path),
        "efficiency_summary": str(efficiency_path),
    }
