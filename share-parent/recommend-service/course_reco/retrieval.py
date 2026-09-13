from __future__ import annotations

import math
import os
import re
import time
from collections import Counter, OrderedDict, defaultdict, deque
from dataclasses import dataclass, field
import hashlib
from typing import Any, Iterable, Sequence

import numpy as np


TOKEN_RE = re.compile(r"[\w\u4e00-\u9fff]+", re.UNICODE)


def tokenize(text: str) -> list[str]:
    return [x.lower() for x in TOKEN_RE.findall(str(text))]


@dataclass
class Evidence:
    doc_id: str
    text: str
    score: float
    source: str
    kind: str = "DESCRIPTION"
    nodes: tuple[str, ...] = ()
    relation: str = "DESCRIPTION"


@dataclass
class KnowledgeState:
    mastered_weights: dict[str, float] = field(default_factory=dict)
    frontier_weights: dict[str, float] = field(default_factory=dict)
    recent_courses: list[str] = field(default_factory=list)
    fields: list[str] = field(default_factory=list)
    paths: dict[str, list[tuple[str, ...]]] = field(default_factory=dict)

    @property
    def mastered(self) -> set[str]:
        return set(self.mastered_weights)

    @property
    def frontier(self) -> set[str]:
        return set(self.frontier_weights)


@dataclass
class RetrievalStats:
    index_build_seconds: float = 0.0
    search_seconds: float = 0.0
    path_seconds: float = 0.0
    calls: int = 0
    fallback_calls: int = 0
    empty_calls: int = 0
    cache_hits: int = 0
    dense_backend: str = "none"
    dense_fallback: bool = False


class LexicalRetriever:
    """Dependency-free BM25-compatible lexical retriever."""

    def __init__(self, documents: dict[str, str]):
        self.documents = documents
        self.tokens = {k: tokenize(v) for k, v in documents.items()}
        # Term frequencies do not depend on the query.  Rebuilding a Counter
        # for every document on every search made large real-data runs spend
        # most of their time in Python bookkeeping.
        self.term_freqs = {key: Counter(tokens) for key, tokens in self.tokens.items()}
        self.df: Counter[str] = Counter()
        for toks in self.tokens.values():
            self.df.update(set(toks))
        n = max(1, len(self.tokens))
        self.idf = {
            term: math.log(1 + (n - frequency + 0.5) / (frequency + 0.5))
            for term, frequency in self.df.items()
        }
        self.avgdl = sum(map(len, self.tokens.values())) / max(1, len(self.tokens))
        self.doc_norm = {
            key: 1.5 * (1 - 0.75 + 0.75 * len(tokens) / max(self.avgdl, 1e-9))
            for key, tokens in self.tokens.items()
        }
        # Inverted index: query terms only visit documents in which they
        # occur.  This is algebraically identical to the previous full
        # document scan, but is much faster for long course/concept prompts.
        self.postings: dict[str, list[tuple[str, int]]] = defaultdict(list)
        for doc_id, frequencies in self.term_freqs.items():
            for term, frequency in frequencies.items():
                self.postings[term].append((doc_id, frequency))

    def search(self, query: str, top_k: int = 5) -> list[Evidence]:
        # The previous implementation added the same term's contribution
        # once per occurrence.  Aggregate query terms and multiply by their
        # count; this is the same BM25 calculation with far fewer Python
        # loops for long prompts containing repeated concept names.
        q = Counter(tokenize(query))
        scores: Counter[str] = Counter()
        for term, query_frequency in q.items():
            idf = self.idf.get(term, 0.0)
            if not idf:
                continue
            for doc_id, frequency in self.postings.get(term, ()):
                denom = frequency + self.doc_norm[doc_id]
                scores[doc_id] += query_frequency * idf * (
                    frequency * 2.5 / denom if denom else 0.0
                )
        scored = [
            Evidence(doc_id, self.documents[doc_id], scores.get(doc_id, 0.0), "bm25")
            for doc_id in self.documents
        ]
        return sorted(scored, key=lambda x: (-x.score, x.doc_id))[:top_k]


class DenseRetriever:
    """Optional Sentence-Transformers cosine retriever with query caching."""

    def __init__(
        self,
        documents: dict[str, str],
        model_name: str,
        device: str | None = None,
        *,
        cache_size: int = 512,
    ):
        started = time.perf_counter()
        self.documents = documents
        self.ids = sorted(documents)
        self.cache_size = max(0, int(cache_size))
        self.query_cache: OrderedDict[str, np.ndarray] = OrderedDict()
        if str(model_name).lower() in {"hash", "__hash__", "fallback"}:
            self.backend = "hash"
            self.dimension = 256
            self.embeddings = np.vstack(
                [self._hash_vector(documents[doc_id]) for doc_id in self.ids]
            ) if self.ids else np.zeros((0, self.dimension), dtype=np.float32)
            self.build_seconds = time.perf_counter() - started
            return
        try:
            os.environ["USE_TF"] = "0"
            from sentence_transformers import SentenceTransformer
        except ImportError as exc:
            raise RuntimeError("稠密检索需要安装 requirements-full.txt") from exc
        self.backend = "sentence_transformers"
        self.model = SentenceTransformer(model_name, device=device)
        self.embeddings = self.model.encode(
            [documents[i] for i in self.ids],
            normalize_embeddings=True,
            show_progress_bar=False,
        )
        self.embeddings = np.asarray(self.embeddings, dtype=np.float32)
        self.build_seconds = time.perf_counter() - started

    @staticmethod
    def _hash_vector(text: str, dimension: int = 256) -> np.ndarray:
        vector = np.zeros(dimension, dtype=np.float32)
        for token in tokenize(text):
            digest = hashlib.blake2b(token.encode("utf-8"), digest_size=16).digest()
            index = int.from_bytes(digest[:8], "little") % dimension
            sign = 1.0 if digest[8] & 1 else -1.0
            vector[index] += sign
            second = int.from_bytes(digest[9:], "little") % dimension
            vector[second] += 0.5 * sign
        norm = float(np.linalg.norm(vector))
        return vector / norm if norm > 1e-12 else vector

    def search(self, query: str, top_k: int = 5) -> list[Evidence]:
        cached = self.query_cache.get(query)
        if cached is not None:
            self.query_cache.move_to_end(query)
        else:
            if self.backend == "hash":
                cached = self._hash_vector(query, self.embeddings.shape[1])
            else:
                cached = np.asarray(
                    self.model.encode([query], normalize_embeddings=True)[0], dtype=np.float32
                )
            if self.cache_size:
                self.query_cache[query] = cached
                self.query_cache.move_to_end(query)
                while len(self.query_cache) > self.cache_size:
                    self.query_cache.popitem(last=False)
        query_vec = cached
        scores = self.embeddings @ query_vec
        order = np.argsort(-scores, kind="stable")[:top_k]
        return [
            Evidence(self.ids[i], self.documents[self.ids[i]], float(scores[i]), "dense")
            for i in order
        ]


class HybridRetriever:
    """State-aware retriever over course descriptions and concept/prerequisite graph."""

    def __init__(
        self,
        documents: dict[str, str],
        metadata: dict[str, dict[str, Any]],
        prerequisite_edges: Iterable[tuple[str, str]],
        dense_model: str | None = None,
        course_concepts: dict[str, list[str]] | None = None,
        *,
        dense_device: str | None = None,
        weights: dict[str, float] | None = None,
        decay: float = 0.3,
        max_path_length: int = 2,
        dense_fallback: bool = False,
        cache_size: int = 512,
    ):
        started = time.perf_counter()
        self.documents = {str(k): str(v) for k, v in documents.items()}
        self.metadata = {str(k): v for k, v in metadata.items()}
        self.course_concepts = {
            str(k): [str(x) for x in values]
            for k, values in (course_concepts or {}).items()
        }
        self.concept_to_courses: dict[str, set[str]] = defaultdict(set)
        for course, concepts in self.course_concepts.items():
            for concept in concepts:
                self.concept_to_courses[concept].add(course)
        self.edges = sorted({(str(a), str(b)) for a, b in prerequisite_edges if a and b and a != b})
        self.successors: dict[str, set[str]] = defaultdict(set)
        self.predecessors: dict[str, set[str]] = defaultdict(set)
        for source, target in self.edges:
            self.successors[source].add(target)
            self.predecessors[target].add(source)
        self._successor_lists = {
            node: sorted(targets) for node, targets in self.successors.items()
        }
        # In _graph_scores the original implementation scanned every
        # course/concept pair for every history.  These reverse indexes make
        # the same max-score calculation proportional to the concepts active
        # in the current learner state instead of the whole prerequisite graph.
        self.prerequisite_concept_to_courses: dict[str, set[str]] = defaultdict(set)
        for course, concepts in self.course_concepts.items():
            for concept in concepts:
                for predecessor in self.predecessors.get(concept, set()):
                    self.prerequisite_concept_to_courses[predecessor].add(course)
        # Course metadata is fixed for the whole run.  Expand each course's
        # concept set through the prerequisite graph once, then combine those
        # cached contributions for each learner history.  Without this cache,
        # every distinct training prefix repeated the same depth-limited BFS
        # over the 200k+ edge graph.
        self._course_expansion_cache: dict[
            str, tuple[dict[str, float], dict[str, list[tuple[str, ...]]]]
        ] = {}
        # A run can contain tens of thousands of distinct prefixes.  Keeping
        # every expanded state alive is both unnecessary (the evidence
        # callback has its own per-run cache) and particularly expensive for
        # MOOCCubeX.  A small cache still serves the repeated build_state call
        # made inside one search/query operation.
        self._state_cache_limit = 256
        self.cache_size = max(0, int(cache_size))
        self.lexical = LexicalRetriever(self.documents)
        self.dense_fallback = bool(
            dense_model and str(dense_model).lower() in {"hash", "__hash__", "fallback"}
        )
        if dense_model:
            try:
                self.dense = DenseRetriever(
                    self.documents,
                    dense_model,
                    dense_device,
                    cache_size=self.cache_size,
                )
            except Exception as exc:
                if not dense_fallback:
                    raise RuntimeError(
                        f"稠密检索初始化失败（{dense_model}）。请安装 requirements-full.txt，"
                        "或设置 retrieval.dense_fallback: true 使用可复现的本地哈希向量。"
                    ) from exc
                self.dense = DenseRetriever(
                    self.documents,
                    "__hash__",
                    dense_device,
                    cache_size=self.cache_size,
                )
                self.dense_fallback = True
        else:
            self.dense = None
        cfg = weights or {}
        self.weights = {
            "bm25": float(cfg.get("bm25", 0.55 if not self.dense else 0.4)),
            "dense": float(cfg.get("dense", 0.0 if not self.dense else 0.3)),
            "graph": float(cfg.get("graph", 0.2 if not self.dense else 0.3)),
            "field": float(cfg.get("field", 0.05)),
        }
        total = sum(max(0.0, x) for x in self.weights.values()) or 1.0
        self.weights = {key: max(0.0, value) / total for key, value in self.weights.items()}
        self.decay = max(0.0, float(decay))
        self.max_path_length = max(1, int(max_path_length))
        self.stats = RetrievalStats(
            index_build_seconds=time.perf_counter() - started,
            dense_backend=self.dense.backend if self.dense else "none",
            dense_fallback=self.dense_fallback,
        )
        self._state_cache: dict[tuple[str, ...], KnowledgeState] = {}
        self._search_cache: OrderedDict[
            tuple[tuple[str, ...], int, bool, str], list[Evidence]
        ] = OrderedDict()

    def _fields_for_course(self, course_id: str) -> set[str]:
        values = (self.metadata.get(course_id) or {}).get("field") or []
        if not isinstance(values, list):
            values = [values]
        return {str(value).strip() for value in values if str(value).strip()}

    def build_state(self, history: Sequence[str]) -> KnowledgeState:
        key = tuple(str(x) for x in history)
        if key in self._state_cache:
            return self._state_cache[key]
        mastered: dict[str, float] = defaultdict(float)
        recent = list(key[-10:])
        for position, course in enumerate(recent):
            weight = math.exp(-self.decay * (len(recent) - position - 1))
            for concept in self.course_concepts.get(course, []):
                mastered[concept] += weight
        mastered_set = set(mastered)
        frontier: dict[str, float] = defaultdict(float)
        paths: dict[str, list[tuple[str, ...]]] = defaultdict(list)
        for position, course in enumerate(recent):
            course_weight = math.exp(-self.decay * (len(recent) - position - 1))
            contribution, course_paths = self._expand_course(course)
            for target, value in contribution.items():
                if target not in mastered_set:
                    frontier[target] += course_weight * value
            for target, target_paths in course_paths.items():
                if target in mastered_set:
                    continue
                # path_evidence currently requests at most two paths in total;
                # retaining two per target preserves the useful prefix while
                # avoiding a large repeated path list on the real graph.
                for path in target_paths:
                    if path not in paths[target] and len(paths[target]) < 2:
                        paths[target].append(path)
        fields = sorted({field for course in recent for field in self._fields_for_course(course)})
        state = KnowledgeState(
            mastered_weights=dict(mastered),
            frontier_weights=dict(frontier),
            recent_courses=recent,
            fields=fields,
            paths={key: value for key, value in paths.items()},
        )
        self._state_cache[key] = state
        while len(self._state_cache) > self._state_cache_limit:
            self._state_cache.pop(next(iter(self._state_cache)))
        return state

    def _expand_course(
        self, course: str
    ) -> tuple[dict[str, float], dict[str, list[tuple[str, ...]]]]:
        """Precompute depth-limited prerequisite propagation for one course."""
        course = str(course)
        cached = self._course_expansion_cache.get(course)
        if cached is not None:
            return cached
        contribution: dict[str, float] = defaultdict(float)
        paths: dict[str, list[tuple[str, ...]]] = defaultdict(list)
        if self.max_path_length <= 2:
            # The configured/default path depth is two.  Expressing this
            # directly avoids allocating a deque and a tuple for every graph
            # walk while preserving the original 1.0 / 0.5 propagation
            # weights and cycle checks.
            for source in self.course_concepts.get(course, []):
                direct = self._successor_lists.get(source, ())
                for target in direct:
                    if target == source:
                        continue
                    contribution[target] += 1.0
                    if len(paths[target]) < 2:
                        paths[target].append((source, target))
                if self.max_path_length < 2:
                    continue
                for middle in direct:
                    if middle == source:
                        continue
                    for target in self._successor_lists.get(middle, ()):
                        if target == source or target == middle:
                            continue
                        contribution[target] += 0.5
                        if len(paths[target]) < 2:
                            paths[target].append((source, middle, target))
            result = (dict(contribution), dict(paths))
            self._course_expansion_cache[course] = result
            return result
        for source in self.course_concepts.get(course, []):
            queue: deque[tuple[str, tuple[str, ...], int, float]] = deque(
                [(source, (source,), 0, 1.0)]
            )
            while queue:
                node, path, depth, path_weight = queue.popleft()
                if depth >= self.max_path_length:
                    continue
                for target in self._successor_lists.get(node, ()):
                    if target in path:
                        continue
                    next_path = (*path, target)
                    next_weight = path_weight * (0.5 ** depth)
                    contribution[target] += next_weight
                    if len(paths[target]) < 2 and next_path not in paths[target]:
                        paths[target].append(next_path)
                    queue.append((target, next_path, depth + 1, next_weight))
        result = (dict(contribution), dict(paths))
        self._course_expansion_cache[course] = result
        return result

    def query(self, history: Sequence[str], use_frontier: bool = True) -> str:
        state = self.build_state(history)
        recent_text = " ".join(self.documents.get(course, course) for course in state.recent_courses)
        mastered = " ".join(sorted(state.mastered_weights, key=lambda x: (-state.mastered_weights[x], x))[:32])
        frontier = ""
        if use_frontier:
            frontier = " ".join(
                sorted(state.frontier_weights, key=lambda x: (-state.frontier_weights[x], x))[:32]
            )
        fields = " ".join(state.fields)
        return f"{recent_text} mastered {mastered} frontier {frontier} fields {fields}".strip()

    def static_evidence(
        self, history: Sequence[str], top_k: int = 5, *, exclude: Iterable[str] = ()
    ) -> list[Evidence]:
        """Build the fixed KP4SR evidence control from observed history only."""
        excluded = {str(x) for x in exclude}
        output: list[Evidence] = []
        seen: set[str] = set()
        for course in reversed([str(x) for x in history]):
            if course in excluded or course in seen or course not in self.documents:
                continue
            seen.add(course)
            concepts = self.course_concepts.get(course, [])
            text = self.documents.get(course, course)
            if concepts:
                text = f"{text} --CONCEPT--> {' '.join(concepts)}"
                output.append(
                    Evidence(
                        course,
                        text,
                        0.0,
                        "static",
                        kind="CONCEPT",
                        nodes=(course, *concepts),
                        relation="CONCEPT",
                    )
                )
            else:
                output.append(
                    Evidence(
                        course,
                        text,
                        0.0,
                        "static",
                        kind="DESCRIPTION",
                        nodes=(course,),
                        relation="DESCRIPTION",
                    )
                )
            if len(output) >= top_k:
                break
        return output

    def _graph_scores(self, state: KnowledgeState) -> dict[str, float]:
        scores: dict[str, float] = {}
        for concept, weight in state.frontier_weights.items():
            for course in self.concept_to_courses.get(concept, set()):
                scores[course] = max(scores.get(course, 0.0), weight)
        for predecessor, weight in state.mastered_weights.items():
            for course in self.prerequisite_concept_to_courses.get(predecessor, set()):
                scores[course] = max(scores.get(course, 0.0), weight)
        return scores

    @staticmethod
    def _normalise(values: dict[str, float]) -> dict[str, float]:
        if not values:
            return {}
        lo = min(values.values())
        hi = max(values.values())
        if hi - lo < 1e-12:
            return {key: 1.0 for key in values}
        return {key: (value - lo) / (hi - lo) for key, value in values.items()}

    def search(
        self,
        history: Sequence[str],
        top_k: int = 5,
        *,
        mode: str = "hybrid",
        use_frontier: bool = True,
        exclude: Iterable[str] = (),
        random_seed: int = 2026,
    ) -> list[Evidence]:
        started = time.perf_counter()
        mode = mode.lower()
        if mode not in {"bm25", "dense", "hybrid", "static", "random", "none"}:
            raise ValueError(f"未知检索模式: {mode}")
        key = (tuple(str(x) for x in history), int(top_k), bool(use_frontier), mode)
        excluded = {str(x) for x in exclude}
        cached = self._search_cache.get(key)
        if cached is not None and not excluded and mode != "random":
            self._search_cache.move_to_end(key)
            self.stats.cache_hits += 1
            self.stats.calls += 1
            self.stats.search_seconds += time.perf_counter() - started
            return cached[:top_k]
        if mode == "none":
            result: list[Evidence] = []
        elif mode == "static":
            result = self.static_evidence(history, top_k, exclude=excluded)
        elif mode == "random":
            result = random_evidence(
                self.documents, history, top_k, random_seed, exclude=excluded
            )
        else:
            state = self.build_state(history)
            query = self.query(history, use_frontier=use_frontier)
            candidate_k = max(top_k * 5, 20)
            lexical = {e.doc_id: e for e in self.lexical.search(query, candidate_k)}
            dense: dict[str, Evidence] = {}
            if self.dense and mode in {"dense", "hybrid"}:
                dense = {e.doc_id: e for e in self.dense.search(query, candidate_k)}
            graph_raw = self._graph_scores(state) if use_frontier else {}
            if mode == "bm25":
                dense = {}
                graph_raw = {}
            elif mode == "dense":
                lexical = {}
                graph_raw = {}
            lex_norm = self._normalise({key: value.score for key, value in lexical.items()})
            dense_norm = self._normalise({key: value.score for key, value in dense.items()})
            graph_norm = self._normalise(graph_raw)
            fields = set(state.fields)
            candidates = set(lexical) | set(dense) | set(graph_raw) | set(self.documents)
            result = []
            for course in sorted(candidates):
                if course in excluded:
                    continue
                course_fields = self._fields_for_course(course)
                field_score = float(bool(fields & course_fields))
                score = (
                    self.weights["bm25"] * lex_norm.get(course, 0.0)
                    + self.weights["dense"] * dense_norm.get(course, 0.0)
                    + self.weights["graph"] * graph_norm.get(course, 0.0)
                    + self.weights["field"] * field_score
                )
                source = mode
                if mode == "static":
                    source = "static"
                result.append(
                    Evidence(
                        course,
                        self.documents.get(course, course),
                        float(score),
                        source,
                        kind="DESCRIPTION",
                        nodes=(course,),
                        relation="DESCRIPTION",
                    )
                )
            result.sort(key=lambda evidence: (-evidence.score, evidence.doc_id))
            result = result[:top_k]
        if not result and mode not in {"none", "static"}:
            self.stats.empty_calls += 1
            self.stats.fallback_calls += 1
        self.stats.calls += 1
        self.stats.search_seconds += time.perf_counter() - started
        if not excluded and mode != "random":
            if self.cache_size:
                self._search_cache[key] = list(result)
                self._search_cache.move_to_end(key)
                while len(self._search_cache) > self.cache_size:
                    self._search_cache.popitem(last=False)
        return result

    def clear_runtime_caches(self) -> None:
        """Release per-query state that is only useful within one phase.

        The course expansion cache is retained because it is small and shared
        by all histories.  Query/state/search caches are bounded, but clearing
        them between prompt-building phases keeps the process RSS from
        carrying the entire training phase into evaluation.
        """
        self._state_cache.clear()
        self._search_cache.clear()
        if self.dense is not None:
            self.dense.query_cache.clear()

    def path_evidence(
        self, history: Sequence[str], top_k: int = 5, *, use_frontier: bool = True
    ) -> list[Evidence]:
        """Return explicit course-concept-prerequisite path evidence for prompts."""
        started = time.perf_counter()
        if not use_frontier:
            return []
        state = self.build_state(history)
        output: list[Evidence] = []
        for frontier_concept in sorted(
            state.frontier_weights,
            key=lambda concept: (-state.frontier_weights[concept], concept),
        ):
            for path in state.paths.get(frontier_concept, []):
                if len(path) < 2:
                    continue
                text = " --PREREQUISITE--> ".join(path)
                output.append(
                    Evidence(
                        doc_id=f"path:{'->'.join(path)}",
                        text=text,
                        score=float(state.frontier_weights[frontier_concept]),
                        source="graph",
                        kind="PREREQUISITE",
                        nodes=tuple(path),
                        relation="PREREQUISITE",
                    )
                )
                if len(output) >= top_k:
                    self.stats.path_seconds += time.perf_counter() - started
                    return output
        self.stats.path_seconds += time.perf_counter() - started
        return output


def random_evidence(
    documents: dict[str, str],
    history: Sequence[str],
    top_k: int,
    seed: int,
    *,
    exclude: Iterable[str] = (),
) -> list[Evidence]:
    import random

    # Do not use Python's process-randomised hash: reproducibility must hold
    # across machines and worker processes.
    history_hash = sum((index + 1) * sum(ord(ch) for ch in str(value)) for index, value in enumerate(history))
    rng = random.Random(int(seed) + history_hash)
    excluded = {str(x) for x in exclude}
    keys = [key for key in sorted(documents) if key not in excluded]
    chosen = rng.sample(keys, k=min(max(0, int(top_k)), len(keys)))
    return [
        Evidence(k, documents[k], 0.0, "random", kind="DESCRIPTION", nodes=(k,), relation="DESCRIPTION")
        for k in chosen
    ]
