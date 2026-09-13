from __future__ import annotations

import logging
import time
from pathlib import Path
from typing import Any, Dict, List

from course_reco.data import CourseDataset, _load_processed, make_synthetic_dataset
from course_reco.retrieval import HybridRetriever, KnowledgeState
from service.mapping_adapter import CourseMappingAdapter

logger = logging.getLogger("drag_service")

class DragModelHolder:
    """Singleton holder for DRAG-KP4SR dataset, retriever, and semantic adapter."""

    _instance: DragModelHolder | None = None

    def __init__(self, data_root: Path | None = None):
        if data_root is None:
            # Default to data/MOOCCubeX/processed relative to project root
            base_dir = Path(__file__).resolve().parent.parent
            data_root = base_dir / "data" / "MOOCCubeX" / "processed"

        started = time.perf_counter()
        logger.info("Initializing DRAG-KP4SR Knowledge Graph & Hybrid Retriever from %s...", data_root)

        if data_root.exists() and (data_root / "course_meta.json").exists():
            self.dataset = _load_processed(data_root)
            self.data_source = "MOOCCubeX-Official"
        else:
            logger.warning("Processed MOOCCubeX directory not found at %s. Falling back to synthetic dataset.", data_root)
            self.dataset = make_synthetic_dataset(users=100, courses=50, concepts=500, edges=1200)
            self.data_source = "Synthetic-Fallback"

        self.adapter = CourseMappingAdapter(sorted(self.dataset.course_meta.keys()))

        # HybridRetriever with Lexical BM25 + Graph Prerequisite + Field scoring
        self.retriever = HybridRetriever(
            self.dataset.course_text,
            self.dataset.course_meta,
            self.dataset.prerequisite_edges,
            dense_model=None,  # CPU friendly
            course_concepts=self.dataset.course_concepts,
            decay=0.3,
            max_path_length=2,
            cache_size=1024,
        )

        cost = time.perf_counter() - started
        logger.info(
            "DRAG-KP4SR engine initialized in %.2fs: %d courses, %d prereq edges, %d sequences.",
            cost,
            len(self.dataset.course_meta),
            len(self.dataset.prerequisite_edges),
            len(self.dataset.sequences),
        )

    @classmethod
    def get_instance(cls) -> DragModelHolder:
        if cls._instance is None:
            cls._instance = DragModelHolder()
        return cls._instance

    def predict(self, payload: Dict[str, Any]) -> Dict[str, Any]:
        """Execute DRAG-KP4SR sequential ranking and knowledge path inference."""
        started = time.perf_counter()

        user_id = payload.get("userId")
        raw_history = payload.get("historyCourseIds") or []
        intended_role = str(payload.get("intendedRole") or "")
        top_skills = payload.get("topSkills") or []
        top_k = max(1, min(int(payload.get("topK") or 12), 30))

        # 1. Map business history into dataset IDs
        dataset_history: List[str] = []
        for item in raw_history:
            did = self.adapter.to_dataset_id(item)
            if did and (not dataset_history or did != dataset_history[-1]):
                dataset_history.append(did)

        # Cold-start fallback if history is empty
        if not dataset_history:
            seed_kw = " ".join(top_skills + [intended_role])
            seed_did = self.adapter.to_dataset_id(None, keywords=seed_kw)
            dataset_history = [seed_did]

        # 2. Build dynamic KnowledgeState (Mastered with decay, Frontier BFS)
        state: KnowledgeState = self.retriever.build_state(dataset_history)

        top_mastered = [
            self.adapter.clean_concept_name(c)
            for c in sorted(state.mastered_weights, key=lambda x: (-state.mastered_weights[x], x))[:10]
        ]
        top_frontier = [
            self.adapter.clean_concept_name(c)
            for c in sorted(state.frontier_weights, key=lambda x: (-state.frontier_weights[x], x))[:10]
        ]

        # 3. Hybrid search (BM25 + Graph + Field)
        raw_candidates = self.retriever.search(
            dataset_history,
            top_k=top_k * 2,
            mode="hybrid",
            use_frontier=True,
            exclude=dataset_history,
        )

        # 4. Prerequisite path extraction
        path_evidences = self.retriever.path_evidence(dataset_history, top_k=5, use_frontier=True)
        evidence_paths_text = [p.text for p in path_evidences]

        # 5. Format results compatible with Java AlgorithmCandidateDTO
        results: List[Dict[str, Any]] = []
        seen_bus_ids = set()

        for offset, evidence in enumerate(raw_candidates):
            bus_id = self.adapter.to_business_id(evidence.doc_id, candidate_offset=offset)
            if bus_id in seen_bus_ids:
                bus_id = (bus_id % 320) + 1
            seen_bus_ids.add(bus_id)

            # Score normalized to 82.0 ~ 98.5
            norm_score = round(82.0 + float(evidence.score) * 16.5, 1)
            norm_score = max(80.0, min(99.0, norm_score))

            tag = "知识前沿突破" if offset < 3 else ("先修核心进阶" if offset < 7 else "图谱综合推荐")

            features = {
                "rawCourseCode": evidence.doc_id,
                "algorithmScore": norm_score,
                "evidenceKind": evidence.kind,
                "evidenceText": evidence.text[:120] if evidence.text else "",
                "masteredConcepts": top_mastered[:5],
                "frontierConcepts": top_frontier[:5],
                "evidencePaths": evidence_paths_text[:3],
            }

            results.append({
                "courseId": bus_id,
                "score": norm_score,
                "matchTag": tag,
                "features": features,
            })

            if len(results) >= top_k:
                break

        latency_ms = round((time.perf_counter() - started) * 1000.0, 2)

        return {
            "code": 200,
            "msg": "success",
            "engine": "DRAG-KP4SR-Lite",
            "latencyMs": latency_ms,
            "knowledgeState": {
                "masteredCount": len(state.mastered),
                "frontierCount": len(state.frontier),
                "topMastered": top_mastered,
                "topFrontier": top_frontier,
                "evidencePaths": evidence_paths_text,
            },
            "data": results,
        }
