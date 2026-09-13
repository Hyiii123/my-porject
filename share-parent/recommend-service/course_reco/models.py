from __future__ import annotations

import os
from dataclasses import dataclass

import torch
from torch import nn


class MostPop:
    def fit(
        self,
        pairs: list[tuple[list[int], int]],
        num_items: int,
        *,
        train_sequences: list[list[int]] | None = None,
    ) -> "MostPop":
        """Fit popularity on training interactions.

        ``pairs`` is retained for backward compatibility.  When the runner
        supplies ``train_sequences``, every observed training interaction is
        counted, including the first item of each user sequence.  Counting
        only next-item targets would systematically under-count sequence
        starts and is not the usual MostPop baseline.
        """
        from collections import Counter

        counts = Counter()
        if train_sequences is not None:
            counts.update(item for sequence in train_sequences for item in sequence)
        else:
            counts.update(target for _, target in pairs)
        self.order = sorted(range(num_items), key=lambda x: (-counts[x], x))
        return self

    def rank(self, history: list[int]) -> list[int]:
        return self.order


class MarkovChain:
    def fit(
        self,
        pairs: list[tuple[list[int], int]],
        num_items: int,
        *,
        train_sequences: list[list[int]] | None = None,
    ) -> "MarkovChain":
        from collections import Counter, defaultdict

        self.num_items = num_items
        self.trans: dict[int, Counter[int]] = defaultdict(Counter)
        self.pop = Counter()
        for hist, target in pairs:
            if hist:
                self.trans[hist[-1]][target] += 1
        if train_sequences is not None:
            self.pop.update(item for sequence in train_sequences for item in sequence)
        else:
            self.pop.update(target for _, target in pairs)
        return self

    def rank(self, history: list[int]) -> list[int]:
        local = self.trans.get(history[-1], {}) if history else {}
        return sorted(range(self.num_items), key=lambda x: (-local.get(x, 0), -self.pop[x], x))


class ItemKNN:
    def fit(self, sequences: list[list[int]], num_items: int) -> "ItemKNN":
        """Fit item co-occurrence only on the training prefix sequences."""
        import numpy as np

        self.num_items = num_items
        self.co = np.zeros((num_items, num_items), dtype=np.float32)
        freq = np.zeros(num_items, dtype=np.float32)
        for seq in sequences:
            unique = sorted(set(seq))
            for i in unique:
                freq[i] += 1
                for j in unique:
                    if i != j:
                        self.co[i, j] += 1
        denom = np.sqrt(freq[:, None] * freq[None, :]) + 1e-8
        self.sim = self.co / denom
        return self

    def rank(self, history: list[int]) -> list[int]:
        import numpy as np

        score = self.sim[history[-10:]].sum(axis=0) if history else np.zeros(self.num_items)
        return np.argsort(-score, kind="stable").tolist()


class BPRMF(nn.Module):
    """Bayesian Personalized Ranking matrix factorization over train prefixes."""

    def __init__(self, num_users: int, num_items: int, hidden: int = 64):
        super().__init__()
        self.user = nn.Embedding(num_users, hidden)
        self.item = nn.Embedding(num_items, hidden)
        nn.init.normal_(self.user.weight, std=0.01)
        nn.init.normal_(self.item.weight, std=0.01)

    def score_all(self, user_ids: torch.Tensor) -> torch.Tensor:
        return self.user(user_ids) @ self.item.weight.t()

    def bpr_loss(
        self, users: torch.Tensor, positives: torch.Tensor, negatives: torch.Tensor
    ) -> torch.Tensor:
        u = self.user(users)
        pos = (u * self.item(positives)).sum(-1)
        neg = (u * self.item(negatives)).sum(-1)
        return -torch.log(torch.sigmoid(pos - neg) + 1e-8).mean()


class SequenceRanker(nn.Module):
    """GRU4Rec, SASRec and BERT4Rec-compatible full-catalog rankers.

    BERT4Rec uses a bidirectional Transformer and masked-item training. At
    inference the next item is scored at an appended mask token, rather than
    using the last unmasked hidden state.
    """

    def __init__(self, num_items: int, hidden: int = 64, kind: str = "gru", max_len: int = 50):
        super().__init__()
        if kind not in {"gru", "sasrec", "bert"}:
            raise ValueError(f"unknown sequence ranker kind: {kind}")
        self.num_items = num_items
        self.pad_id = num_items
        self.mask_id = num_items + 1 if kind == "bert" else self.pad_id
        self.kind = kind
        self.max_len = max(2, int(max_len))
        embedding_size = num_items + 2 if kind == "bert" else num_items + 1
        self.embedding = nn.Embedding(embedding_size, hidden, padding_idx=self.pad_id)
        self.position = nn.Embedding(self.max_len, hidden)
        if kind == "gru":
            self.encoder = nn.GRU(hidden, hidden, batch_first=True)
        else:
            nhead = max(
                (candidate for candidate in (8, 4, 2, 1) if hidden % candidate == 0),
                default=1,
            )
            layer = nn.TransformerEncoderLayer(
                hidden,
                nhead,
                hidden * 4,
                dropout=0.1,
                batch_first=True,
                norm_first=False,
            )
            self.encoder = nn.TransformerEncoder(layer, num_layers=2)
        self.output = nn.Linear(hidden, num_items)

    def _truncate(self, histories: torch.Tensor, lengths: torch.Tensor):
        if histories.size(1) <= self.max_len:
            return histories, lengths.clamp(min=1, max=self.max_len)
        histories = histories[:, -self.max_len :]
        lengths = lengths.clamp(min=1, max=self.max_len)
        return histories, lengths

    def forward(self, histories: torch.Tensor, lengths: torch.Tensor) -> torch.Tensor:
        histories, lengths = self._truncate(histories, lengths)
        bsz, seqlen = histories.shape
        x = self.embedding(histories)
        pos = torch.arange(seqlen, device=histories.device).unsqueeze(0)
        x = x + self.position(pos)
        pad_mask = histories.eq(self.pad_id)
        if self.kind == "gru":
            encoded, _ = self.encoder(x)
            idx = (lengths - 1).clamp(min=0, max=seqlen - 1)
            last = encoded[torch.arange(bsz, device=x.device), idx]
            return self.output(last)

        causal = None
        if self.kind == "sasrec":
            causal = torch.triu(torch.ones(seqlen, seqlen, device=x.device, dtype=torch.bool), 1)
        encoded = self.encoder(x, mask=causal, src_key_padding_mask=pad_mask)
        if self.kind == "bert":
            return self.output(encoded)
        idx = (lengths - 1).clamp(min=0, max=seqlen - 1)
        last = encoded[torch.arange(bsz, device=x.device), idx]
        return self.output(last)


class DRAGLiteRanker(nn.Module):
    """Small retrieval-fusion ranker for fast ablations and smoke tests."""

    def __init__(self, num_items: int, hidden: int = 64):
        super().__init__()
        self.num_items = num_items
        self.pad_id = num_items
        self.item = nn.Embedding(num_items + 1, hidden, padding_idx=self.pad_id)
        self.gate = nn.Sequential(nn.Linear(hidden * 2, hidden), nn.Sigmoid())
        self.out = nn.Linear(hidden, num_items)

    def forward(
        self, histories: torch.Tensor, lengths: torch.Tensor, evidence: torch.Tensor
    ) -> torch.Tensor:
        h = self.item(histories)
        mask = histories.ne(self.pad_id).unsqueeze(-1)
        h = (h * mask).sum(1) / mask.sum(1).clamp(min=1)
        e = self.item(evidence)
        emask = evidence.ne(self.pad_id).unsqueeze(-1)
        e = (e * emask).sum(1) / emask.sum(1).clamp(min=1)
        gate = self.gate(torch.cat([h, e], dim=-1))
        fused = gate * h + (1 - gate) * e
        return self.out(fused)


@dataclass
class T5Batch:
    input_ids: torch.Tensor
    attention_mask: torch.Tensor
    targets: torch.Tensor


class _PairwiseMaskT5Attention(nn.Module):
    """Add a per-example encoder mask to a Transformers T5 self-attention.

    Transformers 4.46 expands the public 2-D T5 mask inside ``T5Stack`` and
    does not accept a 3-D pairwise mask there.  Wrapping the self-attention
    module lets us combine the normal padding mask with the pairwise evidence
    mask at the point where T5 expects a broadcastable additive bias.  The
    wrapper is installed only for one forward call and is removed
    immediately afterwards.
    """

    def __init__(self, attention: nn.Module, pairwise_mask: torch.Tensor):
        super().__init__()
        self.attention = attention
        self.pairwise_mask = pairwise_mask

    def forward(
        self,
        hidden_states: torch.Tensor,
        mask: torch.Tensor | None = None,
        *args,
        **kwargs,
    ):
        if mask is not None:
            pairwise = self.pairwise_mask.to(
                device=hidden_states.device,
                dtype=hidden_states.dtype,
            )
            blocked = (1.0 - pairwise).unsqueeze(1)
            pairwise_bias = blocked * torch.finfo(hidden_states.dtype).min
            mask = mask + pairwise_bias
        return self.attention(hidden_states, mask, *args, **kwargs)


class T5CourseRanker(nn.Module):
    """T5-small next-course ranker with constrained catalog output tokens."""

    def __init__(self, model_name: str, course_ids: list[str]):
        super().__init__()
        try:
            # This project uses PyTorch T5 only.  A broken globally installed
            # TensorFlow package must not make the lazy Transformers import
            # fail before the model can be constructed.
            os.environ["USE_TF"] = "0"
            from transformers import AutoTokenizer, T5ForConditionalGeneration
        except Exception as exc:
            raise RuntimeError("T5 后端需要安装 requirements-full.txt") from exc
        self.tokenizer = AutoTokenizer.from_pretrained(model_name)
        self.course_tokens = [f"<course_{i}>" for i in range(len(course_ids))]
        self.evidence_tokens = [f"<EVID_{i}>" for i in range(32)]
        special = self.course_tokens + [
            "<HIST>",
            "<STATE>",
            "<FRONTIER>",
            "<TASK>",
            "<MASK>",
            *self.evidence_tokens,
        ]
        self.tokenizer.add_special_tokens({"additional_special_tokens": special})
        self.model = T5ForConditionalGeneration.from_pretrained(model_name)
        self.model.resize_token_embeddings(len(self.tokenizer))
        # This ranker always supplies one decoder start token and consumes
        # logits immediately.  Decoder KV caching is useful for autoregressive
        # generation, but retains needless state for every training/inference
        # call here and increases allocator pressure over long runs.
        self.model.config.use_cache = False
        generation_config = getattr(self.model, "generation_config", None)
        if generation_config is not None:
            generation_config.use_cache = False
        token_ids = self.tokenizer.convert_tokens_to_ids(self.course_tokens)
        self.register_buffer("course_token_ids", torch.tensor(token_ids, dtype=torch.long))
        evidence_ids = self.tokenizer.convert_tokens_to_ids(self.evidence_tokens)
        self.register_buffer("evidence_token_ids", torch.tensor(evidence_ids, dtype=torch.long))

    def tree_attention_mask(self, input_ids: torch.Tensor, attention_mask: torch.Tensor) -> torch.Tensor:
        """Return a 3-D 0/1 encoder mask for isolated evidence branches.

        History/state tokens are global. Tokens between two evidence markers
        can attend to their own branch and the history, but not to unrelated
        branches. T5 accepts a batch x sequence x sequence 0/1 mask and turns
        it into its additive attention bias internally.
        """
        batch, length = input_ids.shape
        # A tree mask is binary.  Keeping it as int64 multiplies the temporary
        # pairwise-mask footprint by eight without changing the result.
        masks = torch.zeros(
            (batch, length, length), dtype=torch.bool, device=input_ids.device
        )
        marker_set = set(self.evidence_token_ids.detach().cpu().tolist())
        for row in range(batch):
            valid = int(attention_mask[row].sum().item())
            if valid <= 0:
                continue
            markers = [
                index
                for index, token in enumerate(input_ids[row, :valid].tolist())
                if token in marker_set
            ]
            if not markers:
                masks[row, :valid, :valid] = True
                continue
            history_end = markers[0]
            masks[row, :history_end, :valid] = True
            for index, start in enumerate(markers):
                end = markers[index + 1] if index + 1 < len(markers) else valid
                masks[row, start:end, :history_end] = True
                masks[row, start:end, start:end] = True
        return masks

    def forward(self, input_ids: torch.Tensor, attention_mask: torch.Tensor) -> torch.Tensor:
        start = self.model.config.decoder_start_token_id
        if start is None:
            start = self.tokenizer.pad_token_id
        if start is None:
            raise RuntimeError("T5 模型没有 decoder_start_token_id 或 pad_token_id")
        decoder_ids = torch.full(
            (input_ids.size(0), 1),
            start,
            device=input_ids.device,
            dtype=torch.long,
        )
        if attention_mask.dim() == 3:
            # ``T5Stack`` 4.46 accepts only a 2-D padding mask at its public
            # boundary.  The pairwise mask is applied inside each encoder
            # self-attention; the decoder cross-attention receives only the
            # derived valid-token mask.
            pairwise_mask = attention_mask
            padding_mask = pairwise_mask.any(dim=1).to(dtype=torch.long)
            wrappers: list[tuple[nn.Module, nn.Module]] = []
            for block in self.model.encoder.block:
                self_attention = block.layer[0].SelfAttention
                wrapped = _PairwiseMaskT5Attention(self_attention, pairwise_mask)
                block.layer[0].SelfAttention = wrapped
                wrappers.append((block.layer[0], self_attention))
            try:
                out = self.model(
                    input_ids=input_ids,
                    attention_mask=padding_mask,
                    decoder_input_ids=decoder_ids,
                    use_cache=False,
                )
            finally:
                for layer, original in wrappers:
                    layer.SelfAttention = original
        else:
            out = self.model(
                input_ids=input_ids,
                attention_mask=attention_mask,
                decoder_input_ids=decoder_ids,
                use_cache=False,
            )
        return out.logits[:, 0, self.course_token_ids]
