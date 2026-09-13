from __future__ import annotations

import csv
import json
import logging
import math
import os
import re
import sys
import time
import urllib.request
from pathlib import Path
from typing import Any, Dict, List, Set, Tuple

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")
logger = logging.getLogger("alignment_builder")

CATEGORY_MAP = {
    1: "前端开发",
    2: "后端开发",
    3: "移动开发",
    4: "数据库",
    5: "云计算与DevOps",
    6: "人工智能",
    7: "数据科学",
    8: "网络安全",
    9: "游戏开发",
    10: "区块链",
}

# 领域学科与 MOOCCubeX 课程的先验关联加权
DOMAIN_CATEGORY_AFFINITY: Dict[int, Set[str]] = {
    1: {"C_680745", "C_680892", "C_680972", "C_697616"},                          # 前端
    2: {"C_677010", "C_682759", "C_676937", "C_697791", "C_682189", "C_784172"},  # 后端
    3: {"C_681336", "C_680745", "C_682400"},                                      # 移动端
    4: {"C_680910", "C_682737"},                                                  # 数据库
    5: {"C_680747", "C_681678", "C_770751", "C_697721", "C_697821"},              # DevOps/Linux
    6: {"C_696922", "C_681444", "C_697616", "C_597208"},                          # AI
    7: {"C_597208", "C_734011", "C_677157", "C_680762", "C_680931"},              # 数据科学与大数据
    8: {"C_677218", "C_682360", "C_680992"},                                      # 网络与安全
    9: {"C_681345", "C_680972", "C_676937", "C_707379", "C_746968"},              # 游戏/图形/多媒体
    10: {"C_677218", "C_682737", "C_682637"},                                     # 区块链/分布式网络
}


def clean_tokens(text: str) -> List[str]:
    """提取标准化英文术语与中文名词词素"""
    tokens = re.findall(r"[A-Za-z0-9_+#.]+|[\u4e00-\u9fa5]{2,6}", str(text).lower())
    return [t for t in tokens if len(t) > 1]


def get_embedding(text: str, embed_url: str = "http://127.0.0.1:18000/embed") -> List[float]:
    """通过 tianji-embedding 服务获取 512 维 BGE-small-zh 语义向量"""
    req_body = json.dumps({"text": text[:384]}).encode("utf-8")
    req = urllib.request.Request(
        embed_url,
        data=req_body,
        headers={"Content-Type": "application/json"},
    )
    with urllib.request.urlopen(req, timeout=10.0) as resp:
        data = json.loads(resp.read().decode("utf-8"))
        return data["vector"]


def cosine_sim(vec_a: List[float], vec_b: List[float]) -> float:
    dot = sum(a * b for a, b in zip(vec_a, vec_b))
    norm_a = math.sqrt(sum(a * a for a in vec_a))
    norm_b = math.sqrt(sum(b * b for b in vec_b))
    if norm_a <= 0 or norm_b <= 0:
        return 0.0
    return max(0.0, min(1.0, dot / (norm_a * norm_b)))


def jaccard_sim(set_a: Set[str], set_b: Set[str]) -> float:
    if not set_a or not set_b:
        return 0.0
    inter = len(set_a & set_b)
    union = len(set_a | set_b)
    return inter / union if union > 0 else 0.0


def build_semantic_alignment(
    tsv_path: Path,
    meta_path: Path,
    concepts_path: Path,
    output_path: Path,
    embed_url: str = "http://127.0.0.1:18000/embed",
) -> Dict[str, Any]:
    logger.info("开始加载业务课程数据: %s", tsv_path)
    business_courses: List[Dict[str, Any]] = []
    with open(tsv_path, "r", encoding="utf-8") as f:
        reader = csv.DictReader(f, delimiter="\t")
        for r in reader:
            cid = int(r["id"])
            cat_id = int(r["category_id"]) if r.get("category_id") else 1
            skills = r.get("skills", "") or ""
            target_role = r.get("target_role", "") or ""
            short_desc = r.get("short_description", "") or ""
            name = r.get("course_name", "") or ""
            learners = int(r.get("learner_count", 0) or 0)
            diff = int(r.get("difficulty_level", 2) or 2)

            cat_name = CATEGORY_MAP.get(cat_id, "综合技术")
            # 强化富文本表征权重
            rich_text = f"{name} {name} {skills} {skills} {target_role} {cat_name} {short_desc}"
            tokens = set(clean_tokens(rich_text))

            business_courses.append({
                "id": cid,
                "name": name,
                "category_id": cat_id,
                "category_name": cat_name,
                "skills": skills,
                "target_role": target_role,
                "learner_count": learners,
                "difficulty_level": diff,
                "rich_text": rich_text,
                "tokens": tokens,
            })
    logger.info("成功加载 %d 门业务课程", len(business_courses))

    logger.info("开始加载 MOOCCubeX 数据集元数据: %s", meta_path)
    with open(meta_path, "r", encoding="utf-8") as f:
        mooc_courses_raw: Dict[str, Any] = json.load(f)

    mooc_concepts_map: Dict[str, List[str]] = {}
    if concepts_path.exists():
        with open(concepts_path, "r", encoding="utf-8") as f:
            mooc_concepts_map = json.load(f)

    mooc_courses: List[Dict[str, Any]] = []
    for mid, info in mooc_courses_raw.items():
        name = info.get("name", "")
        fields = " ".join(info.get("field", []))
        about = info.get("about", "")
        concepts = mooc_concepts_map.get(mid, [])
        concept_names = [c.removeprefix("K_").split("_")[0] for c in concepts[:20]]
        concepts_str = " ".join(concept_names)

        rich_text = f"{name} {name} {fields} {about[:150]} {concepts_str}"
        tokens = set(clean_tokens(rich_text))

        mooc_courses.append({
            "id": mid,
            "name": name,
            "field": info.get("field", []),
            "rich_text": rich_text,
            "tokens": tokens,
            "concepts": concept_names[:10],
        })
    logger.info("成功加载 %d 门 MOOCCubeX 课程", len(mooc_courses))

    # 1. 批量向量化 (Dense Vector Generation)
    logger.info("通过向量服务生成 Dense 向量: %s", embed_url)
    biz_vectors: List[List[float]] = []
    for i, b in enumerate(business_courses):
        if (i + 1) % 50 == 0 or i == len(business_courses) - 1:
            logger.info("正在生成业务课程向量 [%d/%d]...", i + 1, len(business_courses))
        v = get_embedding(b["rich_text"], embed_url=embed_url)
        biz_vectors.append(v)

    mooc_vectors: List[List[float]] = []
    for j, m in enumerate(mooc_courses):
        v = get_embedding(m["rich_text"], embed_url=embed_url)
        mooc_vectors.append(v)
    logger.info("全部课程 Dense 向量生成完成 (512-dim)")

    # 2. 计算双塔混合相似度矩阵
    forward_map: Dict[str, Any] = {}
    backward_clusters: Dict[str, List[Dict[str, Any]]] = {m["id"]: [] for m in mooc_courses}

    for i, b in enumerate(business_courses):
        b_id = b["id"]
        cat_id = b["category_id"]
        affinities = DOMAIN_CATEGORY_AFFINITY.get(cat_id, set())

        candidate_scores: List[Tuple[str, float, str]] = []
        for j, m in enumerate(mooc_courses):
            m_id = m["id"]
            d_sim = cosine_sim(biz_vectors[i], mooc_vectors[j])
            l_sim = jaccard_sim(b["tokens"], m["tokens"])
            aff_bonus = 0.12 if m_id in affinities else 0.0

            # 综合加权公式
            final_score = (0.60 * d_sim) + (0.28 * l_sim) + aff_bonus
            candidate_scores.append((m_id, round(final_score, 4), m["name"]))

        candidate_scores.sort(key=lambda x: x[1], reverse=True)
        best_m_id, best_score, best_name = candidate_scores[0]

        top_candidates = [
            {"dataset_id": c[0], "score": c[1], "dataset_name": c[2]}
            for c in candidate_scores[:3]
        ]

        forward_map[str(b_id)] = {
            "dataset_id": best_m_id,
            "score": best_score,
            "dataset_name": best_name,
            "business_name": b["name"],
            "category_name": b["category_name"],
            "top_candidates": top_candidates,
        }

        backward_clusters[best_m_id].append({
            "business_id": b_id,
            "course_name": b["name"],
            "score": best_score,
            "learner_count": b["learner_count"],
            "difficulty_level": b["difficulty_level"],
            "category_name": b["category_name"],
        })

    # 对反向簇内部根据 (匹配得分 * 0.7 + 热度归一 * 0.3) 排序
    for m_id, b_list in backward_clusters.items():
        max_learners = max((x["learner_count"] for x in b_list), default=1)
        b_list.sort(
            key=lambda x: (x["score"] * 0.75 + (x["learner_count"] / max(max_learners, 1)) * 0.25),
            reverse=True,
        )

    # 统计孤立簇与平均分
    non_empty_clusters = sum(1 for v in backward_clusters.values() if v)
    avg_score = sum(v["score"] for v in forward_map.values()) / len(forward_map)

    alignment_result = {
        "metadata": {
            "version": "1.0.0-HybridSemantic",
            "businessCourseCount": len(business_courses),
            "moocCourseCount": len(mooc_courses),
            "activeClusters": non_empty_clusters,
            "averageAlignmentScore": round(avg_score, 4),
            "generatedAt": time.strftime("%Y-%m-%d %H:%M:%S"),
        },
        "forward_map": forward_map,
        "backward_clusters": backward_clusters,
    }

    output_path.parent.mkdir(parents=True, exist_ok=True)
    with open(output_path, "w", encoding="utf-8") as f:
        json.dump(alignment_result, f, ensure_ascii=False, indent=2)

    logger.info(
        "实体对齐预计算完成！保存至 %s (有效簇: %d/%d, 平均得分: %.4f)",
        output_path,
        non_empty_clusters,
        len(mooc_courses),
        avg_score,
    )
    return alignment_result


if __name__ == "__main__":
    base_dir = Path(__file__).resolve().parent.parent
    tsv = base_dir / "data" / "business_courses.tsv"
    meta = base_dir / "data" / "MOOCCubeX" / "processed" / "course_meta.json"
    concepts = base_dir / "data" / "MOOCCubeX" / "processed" / "course_concepts.json"
    out = base_dir / "data" / "semantic_alignment.json"

    embed_url = os.getenv("EMBED_SERVICE_URL", "http://tianji-embedding:8000/embed")
    build_semantic_alignment(tsv, meta, concepts, out, embed_url=embed_url)

