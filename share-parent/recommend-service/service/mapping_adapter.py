from __future__ import annotations

import json
import logging
from pathlib import Path
import re
from typing import Any, Dict, List, Optional, Sequence, Set

logger = logging.getLogger("mapping_adapter")

# Key domain anchoring table: maps technology keywords to MOOCCubeX course IDs
DOMAIN_ANCHORS = {
    # Java & Backend
    "java": "C_677010",
    "springboot": "C_677010",
    "springcloud": "C_677010",
    "mybatis": "C_677010",
    "jvm": "C_677010",
    "微服务": "C_677010",
    # Database
    "mysql": "C_680910",
    "sql": "C_680910",
    "redis": "C_680910",
    "innodb": "C_680910",
    "数据库": "C_680910",
    # Linux & DevOps & Cloud Native
    "linux": "C_680747",
    "docker": "C_680747",
    "k8s": "C_680747",
    "kubernetes": "C_680747",
    "devops": "C_680747",
    # Frontend & Mobile
    "vue": "C_680745",
    "vue3": "C_680745",
    "react": "C_680745",
    "javascript": "C_680745",
    "typescript": "C_680745",
    "前端": "C_680745",
    "小程序": "C_680745",
    "flutter": "C_681336",
    "android": "C_681336",
    "移动端": "C_681336",
    # Python & AI & ML & Big Data
    "python": "C_697616",
    "机器学习": "C_696922",
    "深度学习": "C_696922",
    "ai": "C_682737",
    "人工智能": "C_682737",
    "大模型": "C_696922",
    "数据挖掘": "C_597208",
    "大数据": "C_734011",
    "flink": "C_734011",
    "spark": "C_734011",
    # Network & Security
    "网络": "C_677218",
    "计算机网络": "C_677218",
    "网络安全": "C_682400",
    "密码学": "C_682400",
    "安全": "C_682400",
    "高并发": "C_677218",
    # Systems & Algorithms
    "算法": "C_682759",
    "数据结构": "C_696773",
    "操作系统": "C_697721",
    "c++": "C_676937",
    "go": "C_677218",
    "rust": "C_697721",
    "软件工程": "C_682633",
}

class CourseMappingAdapter:
    """Bidirectional adapter between Business Course IDs and MOOCCubeX Dataset IDs."""

    def __init__(self, dataset_courses: Sequence[str], alignment_path: Optional[Path] = None):
        self.dataset_courses = list(dataset_courses)
        self.num_courses = max(1, len(self.dataset_courses))
        self.course_to_idx = {cid: idx for idx, cid in enumerate(self.dataset_courses)}

        if alignment_path is None:
            base_dir = Path(__file__).resolve().parent.parent
            alignment_path = base_dir / "data" / "semantic_alignment.json"

        self.forward_map: Dict[str, Any] = {}
        self.backward_clusters: Dict[str, List[Dict[str, Any]]] = {}

        if alignment_path.exists():
            try:
                with open(alignment_path, "r", encoding="utf-8") as f:
                    align_data = json.load(f)
                self.forward_map = align_data.get("forward_map", {})
                self.backward_clusters = align_data.get("backward_clusters", {})
                logger.info(
                    "成功加载双塔混合语义对齐资产: %d 门正向对齐映射, %d 个反向语义簇",
                    len(self.forward_map),
                    len(self.backward_clusters),
                )
            except Exception as ex:
                logger.warning("加载语义对齐文件失败 (%s)，回退至关键词与哈希映射: %s", alignment_path, ex)
        else:
            logger.warning("未找到语义对齐文件: %s，回退至基础映射", alignment_path)

    def to_dataset_id(self, raw_id: Any, keywords: str = "") -> str:
        """基于双塔混合语义对齐表，将业务课程 ID 映射为 MOOCCubeX 数据集 ID"""
        # 1. 优先查真实语义对齐索引
        if raw_id is not None:
            s = str(raw_id).strip()
            if s in self.forward_map:
                mapped_id = self.forward_map[s].get("dataset_id")
                if mapped_id and mapped_id in self.course_to_idx:
                    return mapped_id

            # 已是数据集原生 C_ 编号
            if s.startswith("C_") and s in self.course_to_idx:
                return s

        # 2. 关键词领域锚定匹配
        if keywords:
            for kw in re.split(r"[,，\s]+", keywords.lower()):
                if (did := DOMAIN_ANCHORS.get(kw)) and did in self.course_to_idx:
                    return did

        # 3. 兜底保护
        if raw_id is None:
            return self.dataset_courses[0]

        try:
            val = int(str(raw_id).strip())
            return self.dataset_courses[abs(val - 1) % self.num_courses]
        except (ValueError, TypeError):
            return self.dataset_courses[abs(sum(ord(c) for c in str(raw_id))) % self.num_courses]

    def to_business_id(
        self,
        dataset_id: str,
        candidate_offset: int = 0,
        exclude_ids: Optional[Set[int]] = None,
    ) -> int:
        """从语义对齐反向簇中，挑选该学科方向下最契合且未被排除的业务课程 ID"""
        exclude_ids = exclude_ids or set()
        cluster = self.backward_clusters.get(dataset_id, [])
        if cluster:
            pool = [c for c in cluster if int(c["business_id"]) not in exclude_ids] or cluster
            return int(pool[candidate_offset % len(pool)]["business_id"])

        return ((self.course_to_idx.get(dataset_id, 0) * 6 + candidate_offset) % 270) + 1

    def get_alignment_info(self, business_id: Any) -> Optional[Dict[str, Any]]:
        """获取指定业务课程的语义对齐元数据"""
        return self.forward_map.get(str(business_id))

    @staticmethod
    def clean_concept_name(raw_concept: str) -> str:
        """Clean raw concept string into readable Chinese/English concept."""
        if not raw_concept:
            return ""
        s = str(raw_concept).removeprefix("K_")
        s = re.sub(r"_计算机科学与技术$", "", s)
        s = re.sub(r"_.*$", "", s)
        return s.replace("_", " ").strip()
