from __future__ import annotations

import re
from typing import Any, Sequence

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

    def __init__(self, dataset_courses: Sequence[str]):
        self.dataset_courses = list(dataset_courses)
        self.num_courses = max(1, len(self.dataset_courses))
        self.course_to_idx = {cid: idx for idx, cid in enumerate(self.dataset_courses)}

    def to_dataset_id(self, raw_id: Any, keywords: str = "") -> str:
        """Convert a database course ID or keywords to a MOOCCubeX dataset ID."""
        if keywords:
            kw_clean = keywords.lower().replace(",", " ").replace("，", " ").split()
            for kw in kw_clean:
                if kw in DOMAIN_ANCHORS and DOMAIN_ANCHORS[kw] in self.course_to_idx:
                    return DOMAIN_ANCHORS[kw]

        if raw_id is None:
            return self.dataset_courses[0]

        s = str(raw_id).strip()
        if s.startswith("C_") and s in self.course_to_idx:
            return s

        try:
            val = int(s)
            idx = abs(val - 1) % self.num_courses
            return self.dataset_courses[idx]
        except (ValueError, TypeError):
            h = abs(sum(ord(c) for c in s)) % self.num_courses
            return self.dataset_courses[h]

    def to_business_id(self, dataset_id: str, candidate_offset: int = 0) -> int:
        """Convert a MOOCCubeX dataset ID to a valid database course ID (1..320)."""
        idx = self.course_to_idx.get(dataset_id, 0)
        # Spread across 1..320 range
        bus_id = ((idx * 6 + candidate_offset) % 320) + 1
        return bus_id

    @staticmethod
    def clean_concept_name(raw_concept: str) -> str:
        """Clean raw concept string into readable Chinese/English concept."""
        if not raw_concept:
            return ""
        s = str(raw_concept).removeprefix("K_")
        s = re.sub(r"_计算机科学与技术$", "", s)
        s = re.sub(r"_.*$", "", s)
        return s.replace("_", " ").strip()
