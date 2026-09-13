from __future__ import annotations

import math
from collections import defaultdict
from typing import Iterable, Sequence


def _rank_of(ranking: Sequence[str], target: str) -> int:
    try:
        return list(ranking).index(target) + 1
    except ValueError:
        return 0


def per_example_metrics(
    ranked_items: list[list[str]],
    targets: list[str],
    histories: list[list[str]],
    *,
    course_prerequisites: dict[str, set[str]] | None = None,
    course_concepts: dict[str, list[str]] | None = None,
    k: int = 10,
) -> list[dict[str, float | int | str | None]]:
    """Return user-level records used by slices and paired statistics."""
    records: list[dict[str, float | int | str | None]] = []
    prereq = course_prerequisites or {}
    concepts = course_concepts or {}
    pcr_key = f"pcr@{k}"
    coverage_key = f"pcr_coverage@{k}"
    for ranking, target, history in zip(ranked_items, targets, histories):
        rank = _rank_of(ranking, target)
        row: dict[str, float | int | str | None] = {
            "target": target,
            "history_length": len(history),
            "rank": rank,
            "hit@5": int(0 < rank <= 5),
            "hit@10": int(0 < rank <= 10),
            "ndcg@5": 1.0 / math.log2(rank + 1) if 0 < rank <= 5 else 0.0,
            "ndcg@10": 1.0 / math.log2(rank + 1) if 0 < rank <= 10 else 0.0,
            "mrr@10": 1.0 / rank if 0 < rank <= 10 else 0.0,
        }
        target_prereq = prereq.get(target, set())
        mastered = {
            concept
            for course in history
            for concept in concepts.get(course, [])
        }
        row["target_prereq_annotated"] = int(bool(target_prereq))
        row["target_prereq_satisfied"] = int(bool(target_prereq) and target_prereq.issubset(mastered))
        annotated_recommendations = [
            set(prereq.get(item, set()))
            for item in ranking[:k]
            if prereq.get(item, set())
        ]
        if annotated_recommendations:
            row[pcr_key] = sum(
                int(required.issubset(mastered)) for required in annotated_recommendations
            ) / len(annotated_recommendations)
            row[coverage_key] = len(annotated_recommendations) / max(1, min(k, len(ranking)))
        else:
            # PCR is undefined when none of the recommended courses has an
            # observable prerequisite annotation.  Keep the field explicit
            # so every per-example record has the same schema; aggregation
            # utilities intentionally ignore this None value and report the
            # coverage alongside the conditional PCR.
            row[pcr_key] = None
            row[coverage_key] = 0.0
        records.append(row)
    return records


def ranking_metrics(
    ranked_items: list[list[str]], targets: list[str], ks: tuple[int, ...] = (5, 10)
) -> dict[str, float]:
    if len(ranked_items) != len(targets):
        raise ValueError("ranked_items and targets length mismatch")
    n = max(1, len(targets))
    out: dict[str, float] = {}
    for k in ks:
        hits = 0.0
        ndcg = 0.0
        for ranking, target in zip(ranked_items, targets):
            rank = _rank_of(ranking[:k], target)
            if rank:
                hits += 1.0
                ndcg += 1.0 / math.log2(rank + 1)
        out[f"HR@{k}"] = hits / n
        out[f"NDCG@{k}"] = ndcg / n
    rr = 0.0
    for ranking, target in zip(ranked_items, targets):
        rank = _rank_of(ranking[:10], target)
        if rank:
            rr += 1.0 / rank
    out["MRR@10"] = rr / n
    return out


def prerequisite_consistency_with_coverage(
    ranked_items: list[list[str]],
    histories: list[list[str]],
    *,
    course_prerequisites: dict[str, set[str]] | None = None,
    course_concepts: dict[str, list[str]] | None = None,
    edges: Iterable[tuple[str, str]] = (),
    k: int = 10,
) -> dict[str, float]:
    """Compute PCR@K and the fraction of recommendations with annotations.

    The prior implementation treated concept IDs as if they were course IDs.
    This function first maps concept prerequisite edges to course concepts and
    only evaluates recommendations whose prerequisite annotation is available.
    """
    if course_prerequisites is None:
        prereq_by_concept: dict[str, set[str]] = defaultdict(set)
        for source, target in edges:
            prereq_by_concept[str(target)].add(str(source))
        course_prerequisites = {
            course: {
                source
                for concept in concepts
                for source in prereq_by_concept.get(str(concept), set())
                if source not in set(concepts)
            }
            for course, concepts in (course_concepts or {}).items()
        }
    concepts = course_concepts or {}
    good = 0
    annotated = 0
    total_recommendations = 0
    for ranking, history in zip(ranked_items, histories):
        mastered = {
            concept
            for course in history
            for concept in concepts.get(course, [])
        }
        for item in ranking[:k]:
            total_recommendations += 1
            required = set(course_prerequisites.get(item, set()))
            if not required:
                continue
            annotated += 1
            good += int(required.issubset(mastered))
    return {
        f"PCR@{k}": good / annotated if annotated else 0.0,
        f"PCR_coverage@{k}": annotated / max(1, total_recommendations),
    }


def prerequisite_consistency(
    ranked_items: list[list[str]],
    histories: list[list[str]],
    edges: Iterable[tuple[str, str]],
    k: int = 10,
    *,
    course_concepts: dict[str, list[str]] | None = None,
) -> float:
    """Backward-compatible PCR@K scalar."""
    return prerequisite_consistency_with_coverage(
        ranked_items,
        histories,
        edges=edges,
        course_concepts=course_concepts,
        k=k,
    )[f"PCR@{k}"]
