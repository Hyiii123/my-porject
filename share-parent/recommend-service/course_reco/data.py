from __future__ import annotations

import json
import math
import random
from collections import Counter, defaultdict
from dataclasses import dataclass, field
from datetime import datetime
from json import JSONDecodeError, JSONDecoder
from pathlib import Path
from typing import Any, Iterable, Iterator, Sequence


PROCESSED_FILES = (
    "sequences.jsonl",
    "course_meta.json",
    "course_concepts.json",
    "prerequisite_edges.json",
    "stats.json",
)


@dataclass
class CourseDataset:
    sequences: list[list[str]]
    course_text: dict[str, str]
    course_meta: dict[str, dict[str, Any]]
    prerequisite_edges: list[tuple[str, str]]
    user_ids: list[str] = field(default_factory=list)
    timestamps: list[list[str]] = field(default_factory=list)
    course_concepts: dict[str, list[str]] = field(default_factory=dict)
    stats: dict[str, Any] = field(default_factory=dict)


@dataclass
class SequenceExample:
    user_index: int
    history: list[str]
    target: str
    history_times: list[str] = field(default_factory=list)
    target_time: str | None = None


@dataclass
class SplitBundle:
    examples: dict[str, list[SequenceExample]]
    train_sequences: list[list[str]]
    train_timestamps: list[list[str]]
    strategy: str
    cutoffs: dict[str, str | None] = field(default_factory=dict)


def _read_json_items(path: Path) -> Iterator[Any]:
    """Stream a JSON array or JSONL file.

    MOOCCubeX releases have appeared as both JSONL and JSON arrays.  The user
    entity file is large, so loading an array with ``json.load`` would waste
    several gigabytes of memory.  This small decoder keeps the array format
    streaming as well.
    """
    with path.open("r", encoding="utf-8") as f:
        first = ""
        while True:
            first = f.read(1)
            if not first or not first.isspace():
                break
        f.seek(0)
        if first == "\ufeff":
            first = f.read(1)
            while first and first.isspace():
                first = f.read(1)
            f.seek(0)
        if first == "[":
            decoder = JSONDecoder()
            buffer = f.read(1024 * 1024)
            if buffer.startswith("\ufeff"):
                buffer = buffer[1:]
            if buffer.lstrip().startswith("["):
                prefix = buffer.index("[")
                buffer = buffer[prefix + 1 :]
            eof = False
            while True:
                buffer = buffer.lstrip()
                if buffer.startswith("]"):
                    return
                if buffer.startswith(","):
                    buffer = buffer[1:]
                    continue
                if not buffer:
                    if eof:
                        raise ValueError(f"JSON 数组未闭合: {path}")
                    chunk = f.read(1024 * 1024)
                    if not chunk:
                        eof = True
                        continue
                    buffer += chunk
                    continue
                try:
                    value, end = decoder.raw_decode(buffer)
                except JSONDecodeError:
                    if eof:
                        raise ValueError(f"JSON 数组记录格式错误: {path}")
                    chunk = f.read(1024 * 1024)
                    if not chunk:
                        eof = True
                    else:
                        buffer += chunk
                    continue
                yield value
                buffer = buffer[end:]
            return
        first_line = f.readline()
        stripped_first_line = first_line.lstrip("\ufeff").strip()
        if stripped_first_line.startswith("{") and not stripped_first_line.endswith("}"):
            f.seek(0)
            value = json.load(f)
            yield value
            return
        if stripped_first_line:
            yield json.loads(stripped_first_line)
        for line in f:
            line = line.lstrip("\ufeff").strip()
            if line:
                yield json.loads(line)


def _read_json_records(path: Path) -> Iterator[dict[str, Any]]:
    """Read dictionary records from a JSON array, JSONL, or single object."""
    for value in _read_json_items(path):
        if isinstance(value, dict):
            yield value


def _write_json(path: Path, value: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2), encoding="utf-8")


def _median_from_histogram(histogram: Counter[int]) -> float:
    """Return an exact median without retaining every sequence in memory."""
    total = sum(histogram.values())
    if total <= 0:
        return 0.0
    positions = ((total + 1) // 2, (total + 2) // 2)
    values: list[int] = []
    cumulative = 0
    position_index = 0
    for length in sorted(histogram):
        cumulative += histogram[length]
        while position_index < len(positions) and cumulative >= positions[position_index]:
            values.append(length)
            position_index += 1
    return float(sum(values) / len(values))


def _time_key(value: Any) -> tuple[int, Any]:
    """Return a stable order key for ISO strings, numbers and unknown values."""
    if isinstance(value, (int, float)):
        return (0, float(value))
    text = str(value)
    try:
        # JSON releases sometimes encode epoch times as strings.  Comparing
        # them lexicographically would put "10" before "2".
        return (0, float(text))
    except (TypeError, ValueError):
        pass
    try:
        return (0, datetime.fromisoformat(text.replace("Z", "+00:00")).timestamp())
    except (TypeError, ValueError, OverflowError):
        return (1, text)


def _normalise_concept_id(value: Any) -> str:
    text = str(value).strip()
    if not text:
        return ""
    return text if text.startswith("K_") else f"K_{text}"


def _normalise_course_id(value: Any) -> str:
    """Normalise official numeric course IDs to the ``C_<id>`` form.

    ``entities/course.json`` and ``user.json`` use IDs such as ``C_681932``
    while ``relations/course-field.json`` stores the same ID as the integer
    ``681932``.  Keeping one representation is required for field filtering
    and for joining all relation files.
    """
    text = str(value).strip()
    if text.isdigit():
        return f"C_{text}"
    return text


def _clean_values(value: Any) -> list[str]:
    """Convert relation fields that may be scalar, list, or pipe-separated."""
    if value is None:
        return []
    values = value if isinstance(value, (list, tuple, set)) else [value]
    output: list[str] = []
    for item in values:
        if isinstance(item, (list, tuple, set)):
            output.extend(_clean_values(item))
            continue
        if isinstance(item, dict):
            output.extend(_clean_values(list(item.values())))
            continue
        for part in str(item).replace("|", ";").split(";"):
            part = part.strip()
            if part:
                output.append(part)
    return output


def _normalise_sequence(
    user: dict[str, Any], allowed_courses: set[str] | None = None
) -> tuple[list[str], list[str]]:
    raw_courses = user.get("course_order", user.get("courses", user.get("course_ids", [])))
    courses = [
        _normalise_course_id(x)
        for x in _clean_values(raw_courses)
        if str(x).strip()
    ]
    raw_times = user.get("enroll_time", user.get("enroll_times", user.get("timestamps", [])))
    if isinstance(raw_times, (list, tuple)):
        time_values = list(raw_times)
    elif raw_times is None:
        time_values = []
    else:
        time_values = [raw_times]
    if len(time_values) == len(courses) and courses:
        pairs = sorted(zip(time_values, courses), key=lambda x: (_time_key(x[0]), x[1]))
    else:
        pairs = [("", course) for course in courses]
    if allowed_courses is not None:
        pairs = [(time, course) for time, course in pairs if course in allowed_courses]

    deduped_courses: list[str] = []
    deduped_times: list[str] = []
    for time, course in pairs:
        if deduped_courses and course == deduped_courses[-1]:
            continue
        deduped_courses.append(course)
        deduped_times.append(str(time).strip())
    return deduped_courses, deduped_times


def _load_course_fields(path: Path) -> dict[str, list[str]]:
    fields: dict[str, list[str]] = defaultdict(list)
    if not path.exists():
        return fields
    for row in _read_json_items(path):
        if not isinstance(row, dict):
            continue
        cid = _normalise_course_id(row.get("course_id", row.get("course", row.get("id", ""))))
        values = row.get("field", row.get("fields", row.get("course_field", [])))
        if cid and values is not None:
            fields[cid].extend(_clean_values(values))
            continue
        # Also accept a compact mapping: {"C_001": ["field-a", "field-b"]}.
        for key, value in row.items():
            key = _normalise_course_id(key)
            if key and key not in {"field", "fields"}:
                fields[key].extend(_clean_values(value))
    return {cid: sorted(set(values)) for cid, values in fields.items()}


def _load_course_concepts(path: Path) -> dict[str, list[str]]:
    mapping: dict[str, set[str]] = defaultdict(set)
    if not path.exists():
        return {}
    with path.open("r", encoding="utf-8") as f:
        for line in f:
            parts = line.replace(",", " ").replace("\t", " ").split()
            if len(parts) < 2:
                continue
            first, second = parts[0], parts[1]
            if first.startswith("C_") and second.startswith("K_"):
                course_id, concept_id = first, second
            elif first.startswith("K_") and second.startswith("C_"):
                concept_id, course_id = first, second
            elif first.startswith("K_"):
                concept_id, course_id = first, second
            elif second.startswith("K_"):
                course_id, concept_id = first, second
            else:
                continue
            mapping[_normalise_course_id(course_id)].add(_normalise_concept_id(concept_id))
    return {cid: sorted(values) for cid, values in mapping.items() if values}


def _truthy_ground_truth(value: Any) -> bool:
    if value is None:
        return True
    if isinstance(value, bool):
        return value
    if isinstance(value, (int, float)):
        return value != 0
    return str(value).strip().lower() in {"1", "true", "yes", "y"}


def _load_prerequisite_edges(path: Path) -> list[tuple[str, str]]:
    edges: set[tuple[str, str]] = set()
    if not path.exists():
        return []
    def add_edge(source: Any, target: Any) -> None:
        c1 = _normalise_concept_id(source)
        c2 = _normalise_concept_id(target)
        if c1 and c2 and c1 != c2:
            edges.add((c1, c2))

    for row in _read_json_items(path):
        if isinstance(row, (list, tuple)) and len(row) >= 2:
            add_edge(row[0], row[1])
            continue
        if not isinstance(row, dict):
            continue
        if not _truthy_ground_truth(row.get("ground_truth")):
            continue
        source = row.get("c1", row.get("source", row.get("prerequisite")))
        target = row.get("c2", row.get("target", row.get("concept")))
        if source is not None and target is not None:
            for value in _clean_values(source):
                for target_value in _clean_values(target):
                    add_edge(value, target_value)
            continue
        edge = row.get("edge")
        if isinstance(edge, (list, tuple)) and len(edge) >= 2:
            add_edge(edge[0], edge[1])
            continue
        # A few exports use {target_concept: [prerequisite_concepts]}.
        for target_key, prerequisites in row.items():
            if target_key in {"ground_truth", "label", "score"}:
                continue
            for prerequisite in _clean_values(prerequisites):
                add_edge(prerequisite, target_key)
    return sorted(edges)


def _resolve_prerequisite_edges(
    edges: Iterable[tuple[str, str]], course_concepts: dict[str, list[str]]
) -> list[tuple[str, str]]:
    """Resolve prerequisite names to the field-qualified course concept IDs.

    ``cs.json`` stores names such as ``操作命令`` while
    ``concept-course.txt`` generally stores IDs such as
    ``K_操作命令_计算机科学与技术``.  Matching both the full ID and every
    underscore-delimited prefix supports both official forms without relying on
    a hand-written concept dictionary.
    """
    known = sorted({str(concept) for values in course_concepts.values() for concept in values})
    aliases: dict[str, set[str]] = defaultdict(set)
    for concept in known:
        normalised = _normalise_concept_id(concept)
        bare = normalised.removeprefix("K_")
        parts = bare.split("_")
        for end in range(1, len(parts) + 1):
            aliases["_".join(parts[:end])].add(normalised)
        aliases[bare].add(normalised)
        aliases[normalised].add(normalised)

    resolved: set[tuple[str, str]] = set()
    for source, target in edges:
        source_id = _normalise_concept_id(source)
        target_id = _normalise_concept_id(target)
        source_candidates = aliases.get(source_id) or aliases.get(source_id.removeprefix("K_"), set())
        target_candidates = aliases.get(target_id) or aliases.get(target_id.removeprefix("K_"), set())
        if not source_candidates and source_id in known:
            source_candidates = {source_id}
        if not target_candidates and target_id in known:
            target_candidates = {target_id}
        for source_value in source_candidates:
            for target_value in target_candidates:
                if source_value != target_value:
                    resolved.add((source_value, target_value))
    return sorted(resolved)


def _course_text(row: dict[str, Any], concepts: Sequence[str] = ()) -> str:
    fields = row.get("field") or []
    prereq = row.get("prerequisites") or []
    if not isinstance(fields, list):
        fields = [fields]
    if not isinstance(prereq, list):
        prereq = [prereq]
    concept_names = [str(x).removeprefix("K_").replace("_", " ") for x in concepts]
    parts = [row.get("name", ""), row.get("about", ""), *fields, *prereq, *concept_names]
    return " ".join(str(x).strip() for x in parts if str(x).strip())


def _processed_ready(path: Path) -> bool:
    return all((path / name).exists() for name in PROCESSED_FILES)


def _processed_matches(
    path: Path,
    *,
    min_sequence_length: int,
    course_min_interactions: int,
    target_fields: Sequence[str],
) -> bool:
    if not _processed_ready(path):
        return False
    try:
        stats = json.loads((path / "stats.json").read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError):
        return False
    try:
        return (
            int(stats.get("min_sequence_length", -1)) == int(min_sequence_length)
            and int(stats.get("course_min_interactions", -1)) == int(course_min_interactions)
            and sorted(str(x) for x in stats.get("target_fields", []))
            == sorted(str(x) for x in target_fields)
        )
    except (TypeError, ValueError):
        return False


def _load_processed(path: Path) -> CourseDataset:
    meta = json.loads((path / "course_meta.json").read_text(encoding="utf-8"))
    concepts = json.loads((path / "course_concepts.json").read_text(encoding="utf-8"))
    edges = [tuple(x) for x in json.loads((path / "prerequisite_edges.json").read_text(encoding="utf-8"))]
    stats = json.loads((path / "stats.json").read_text(encoding="utf-8"))
    sequences: list[list[str]] = []
    timestamps: list[list[str]] = []
    user_ids: list[str] = []
    with (path / "sequences.jsonl").open("r", encoding="utf-8") as f:
        for line in f:
            row = json.loads(line)
            sequences.append([str(x) for x in row.get("courses", [])])
            timestamps.append([str(x) for x in row.get("times", [])])
            user_ids.append(f"user_{int(row.get('user_index', len(user_ids))):08d}")
    course_meta = {str(k): v for k, v in meta.items()}
    course_concepts = {str(k): [str(x) for x in v] for k, v in concepts.items()}
    course_text = {cid: _course_text(row, course_concepts.get(cid, [])) for cid, row in course_meta.items()}
    return CourseDataset(
        sequences=sequences,
        course_text=course_text,
        course_meta=course_meta,
        prerequisite_edges=edges,
        user_ids=user_ids,
        timestamps=timestamps,
        course_concepts=course_concepts,
        stats=stats,
    )


def prepare_mooccubex(
    root: str | Path,
    output_dir: str | Path | None = None,
    *,
    min_sequence_length: int = 3,
    course_min_interactions: int = 5,
    target_fields: Sequence[str] = ("计算机科学与技术",),
    progress_every: int = 250_000,
) -> dict[str, Any]:
    """Build a compact, anonymized CS subset from the official raw files.

    The raw user file is streamed twice: once for course frequencies and once
    for filtered sequences. This keeps memory bounded while computing the
    course-frequency threshold before user filtering.
    """
    root = Path(root)
    if int(min_sequence_length) < 1:
        raise ValueError("min_sequence_length 必须大于等于 1")
    if int(course_min_interactions) < 0:
        raise ValueError("course_min_interactions 不能为负数")
    if isinstance(target_fields, str):
        target_fields = [target_fields]
    out = Path(output_dir) if output_dir is not None else root / "processed"
    out.mkdir(parents=True, exist_ok=True)
    course_path = root / "entities" / "course.json"
    user_path = root / "entities" / "user.json"
    field_path = root / "relations" / "course-field.json"
    concept_path = root / "relations" / "concept-course.txt"
    prereq_path = root / "prerequisites" / "cs.json"
    if not course_path.exists() or not user_path.exists():
        raise FileNotFoundError(
            "MOOCCubeX 缺少 entities/course.json 或 entities/user.json；请先下载官方数据。"
        )

    course_meta_all: dict[str, dict[str, Any]] = {}
    for row in _read_json_records(course_path):
        cid = _normalise_course_id(row.get("id", ""))
        if cid:
            course_meta_all[cid] = row

    field_map = _load_course_fields(field_path)
    target_field_set = {str(x).strip() for x in target_fields if str(x).strip()}
    target_courses = {
        cid for cid, fields in field_map.items() if target_field_set.intersection(fields)
    }
    if not target_courses:
        for cid, row in course_meta_all.items():
            values = row.get("field") or []
            if not isinstance(values, list):
                values = [values]
            if target_field_set.intersection(str(x).strip() for x in values):
                target_courses.add(cid)
    if not target_courses:
        available_fields = sorted(
            {
                field
                for values in field_map.values()
                for field in values
            }
            | {
                field
                for row in course_meta_all.values()
                for field in _clean_values(row.get("field", []))
            }
        )
        preview = ", ".join(available_fields[:12]) or "（未找到 field 关系）"
        raise ValueError(
            f"没有课程匹配 target_fields={sorted(target_field_set)}；可用 field 示例: {preview}。"
            "请用 --target-field 指定官方字段，或检查 course-field.json。"
        )
    for cid, fields in field_map.items():
        if cid in course_meta_all and fields:
            course_meta_all[cid] = {**course_meta_all[cid], "field": fields}

    course_concepts_all = _load_course_concepts(concept_path)
    prerequisite_edges_raw = _load_prerequisite_edges(prereq_path)

    course_counts: Counter[str] = Counter()
    raw_users = target_users = 0
    for row in _read_json_records(user_path):
        raw_users += 1
        courses, _ = _normalise_sequence(row, target_courses)
        if courses:
            target_users += 1
            course_counts.update(courses)
        if progress_every and raw_users % progress_every == 0:
            print(f"frequency pass: {raw_users:,} users", flush=True)

    kept_courses = {
        cid for cid in target_courses if course_counts.get(cid, 0) >= course_min_interactions
    }
    kept_courses.intersection_update(course_meta_all)
    kept_meta = {cid: course_meta_all[cid] for cid in sorted(kept_courses) if cid in course_meta_all}
    kept_concepts = {cid: sorted(set(course_concepts_all.get(cid, []))) for cid in kept_meta}
    prerequisite_edges = _resolve_prerequisite_edges(prerequisite_edges_raw, kept_concepts)

    # The compact file is the source of truth.  Do not retain every filtered
    # sequence here as well: the official user file is large and the next
    # stage will load the compact file when an experiment starts.
    sequence_count = 0
    interaction_count = 0
    length_histogram: Counter[int] = Counter()
    with (out / "sequences.jsonl").open("w", encoding="utf-8") as out_f:
        user_index = 0
        for raw_users_seen, row in enumerate(_read_json_records(user_path), start=1):
            courses, times = _normalise_sequence(row, kept_courses)
            if len(courses) < min_sequence_length:
                continue
            out_f.write(
                json.dumps(
                    {"user_index": user_index, "courses": courses, "times": times},
                    ensure_ascii=False,
                )
                + "\n"
            )
            sequence_count += 1
            interaction_count += len(courses)
            length_histogram[len(courses)] += 1
            user_index += 1
            if progress_every and raw_users_seen % progress_every == 0:
                print(f"sequence pass: {raw_users_seen:,} users", flush=True)

    train_interactions = sum(
        count * max(0, length - 2)
        for length, count in length_histogram.items()
    )
    concept_count = len({concept for values in kept_concepts.values() for concept in values})
    stats: dict[str, Any] = {
        "source": "THU-KEG/MOOCCubeX official download",
        "raw_users": raw_users,
        "target_field_users": target_users,
        "target_fields": sorted(target_field_set),
        "target_courses_before_frequency": len(target_courses),
        "course_min_interactions": course_min_interactions,
        "min_sequence_length": min_sequence_length,
        "users": sequence_count,
        "courses": len(kept_meta),
        "users_after_sequence_filter": sequence_count,
        "users_removed_short_sequence": max(0, target_users - sequence_count),
        "interactions": interaction_count,
        "train_interactions_leave_two_out": train_interactions,
        "mean_sequence_length": (
            float(interaction_count / sequence_count) if sequence_count else 0.0
        ),
        "median_sequence_length": _median_from_histogram(length_histogram),
        "max_sequence_length": max(length_histogram, default=0),
        "concepts": concept_count,
        "prerequisite_edges": len(prerequisite_edges),
        "prerequisite_edges_raw_ground_truth": len(prerequisite_edges_raw),
        "prerequisite_edges_resolved": len(prerequisite_edges),
        "course_frequency": {cid: course_counts[cid] for cid in sorted(kept_courses)},
    }
    _write_json(out / "course_meta.json", kept_meta)
    _write_json(out / "course_concepts.json", kept_concepts)
    _write_json(out / "prerequisite_edges.json", prerequisite_edges)
    _write_json(out / "stats.json", stats)
    return stats


def load_moocubex(
    root: str | Path,
    min_sequence_length: int = 3,
    course_min_interactions: int = 5,
    target_fields: Sequence[str] = ("计算机科学与技术",),
    *,
    prepare: bool = True,
) -> CourseDataset:
    """Load the compact processed MOOCCubeX subset, preparing it if needed."""
    root = Path(root)
    if isinstance(target_fields, str):
        target_fields = [target_fields]
    processed = root / "processed"
    if not _processed_matches(
        processed,
        min_sequence_length=min_sequence_length,
        course_min_interactions=course_min_interactions,
        target_fields=target_fields,
    ):
        if not prepare:
            if not _processed_ready(processed):
                raise FileNotFoundError(f"未找到 {processed}；请先运行数据预处理。")
            raise ValueError(
                f"{processed} 已存在，但其预处理参数与当前配置不一致；"
                "请将 prepare 设为 true 重新构建，或调整 min_sequence_length、"
                "course_min_interactions、target_fields。"
            )
        else:
            prepare_mooccubex(
                root,
                processed,
                min_sequence_length=min_sequence_length,
                course_min_interactions=course_min_interactions,
                target_fields=target_fields,
            )
    return _load_processed(processed)


def load_moocubex_raw(root: str | Path, min_sequence_length: int = 3) -> CourseDataset:
    """Compatibility loader for small raw fixtures and legacy callers."""
    root = Path(root)
    course_path = root / "entities" / "course.json"
    user_path = root / "entities" / "user.json"
    if not course_path.exists() or not user_path.exists():
        raise FileNotFoundError(
            "MOOCCubeX 缺少 entities/course.json 或 entities/user.json；请先下载官方数据。"
        )
    course_meta: dict[str, dict[str, Any]] = {}
    for row in _read_json_records(course_path):
        cid = _normalise_course_id(row.get("id", row.get("course_id", "")))
        if cid:
            course_meta[cid] = row
    sequences: list[list[str]] = []
    timestamps: list[list[str]] = []
    user_ids: list[str] = []
    for row in _read_json_records(user_path):
        courses, times = _normalise_sequence(row, set(course_meta))
        if len(courses) >= min_sequence_length:
            sequences.append(courses)
            timestamps.append(times)
            user_ids.append(f"user_{len(user_ids):08d}")
    course_concepts = _load_course_concepts(root / "relations" / "concept-course.txt")
    edges = _resolve_prerequisite_edges(
        _load_prerequisite_edges(root / "prerequisites" / "cs.json"), course_concepts
    )
    course_text = {cid: _course_text(row, course_concepts.get(cid, [])) for cid, row in course_meta.items()}
    return CourseDataset(
        sequences,
        course_text,
        course_meta,
        edges,
        user_ids=user_ids,
        timestamps=timestamps,
        course_concepts=course_concepts,
        stats={
            "source": "MOOCCubeX raw compatibility loader",
            "users": len(sequences),
            "courses": len(course_meta),
            "interactions": sum(map(len, sequences)),
            "min_sequence_length": min_sequence_length,
        },
    )


def make_synthetic_dataset(seed: int = 2026, users: int = 72, courses: int = 24) -> CourseDataset:
    """Generate deterministic toy data for installation and pipeline tests."""
    rng = random.Random(seed)
    ids = [f"C_{i:03d}" for i in range(courses)]
    fields = ["programming", "mathematics", "data", "systems"]
    meta: dict[str, dict[str, Any]] = {}
    text: dict[str, str] = {}
    concepts: dict[str, list[str]] = {}
    for i, cid in enumerate(ids):
        field_name = fields[i % len(fields)]
        meta[cid] = {
            "id": cid,
            "name": f"Course {i}",
            "about": f"level {i // len(fields)} {field_name}",
            "field": [field_name],
            "prerequisites": [ids[i - len(fields)]] if i >= len(fields) else [],
        }
        concepts[cid] = [f"K_{field_name}", f"K_level_{i // len(fields)}"]
        text[cid] = f"Course {i} {field_name} level {i // len(fields)}"
    seqs: list[list[str]] = []
    times: list[list[str]] = []
    for _ in range(users):
        field_offset = rng.randrange(len(fields))
        length = rng.randint(6, 11)
        level = rng.randrange(2)
        seq = []
        for step in range(length):
            idx = min(courses - 1, (level + step // 2) * len(fields) + field_offset)
            if rng.random() < 0.18:
                idx = min(courses - 1, idx + rng.randrange(len(fields)))
            seq.append(ids[idx])
        seqs.append(seq)
        times.append([f"2026-01-{step + 1:02d}T00:00:00" for step in range(len(seq))])
    edges = [
        (f"K_level_{i // len(fields) - 1}", f"K_level_{i // len(fields)}")
        for i in range(len(fields), courses)
    ]
    return CourseDataset(
        seqs,
        text,
        meta,
        edges,
        user_ids=[f"user_{i:08d}" for i in range(users)],
        timestamps=times,
        course_concepts=concepts,
        stats={"users": users, "courses": courses, "interactions": sum(map(len, seqs))},
    )


def _leave_two_out_bundle(
    sequences: list[list[str]], timestamps: list[list[str]] | None
) -> SplitBundle:
    examples: dict[str, list[SequenceExample]] = {"train": [], "val": [], "test": []}
    train_sequences: list[list[str]] = []
    train_timestamps: list[list[str]] = []
    timestamps = timestamps or [[] for _ in sequences]
    for user_index, seq in enumerate(sequences):
        if len(seq) < 3:
            train_sequences.append([])
            train_timestamps.append([])
            continue
        ts = list(timestamps[user_index]) if user_index < len(timestamps) else []
        if len(ts) != len(seq):
            ts = ["" for _ in seq]
        train_seq = seq[:-2]
        train_ts = ts[:-2]
        train_sequences.append(list(train_seq))
        train_timestamps.append(list(train_ts))
        for pos in range(1, len(train_seq)):
            examples["train"].append(
                SequenceExample(user_index, train_seq[:pos], train_seq[pos], train_ts[:pos], train_ts[pos])
            )
        examples["val"].append(SequenceExample(user_index, seq[:-2], seq[-2], ts[:-2], ts[-2]))
        examples["test"].append(SequenceExample(user_index, seq[:-1], seq[-1], ts[:-1], ts[-1]))
    return SplitBundle(examples, train_sequences, train_timestamps, "leave_two_out")


def _global_time_bundle(
    sequences: list[list[str]],
    timestamps: list[list[str]],
    train_ratio: float,
    val_ratio: float,
) -> SplitBundle:
    events: list[tuple[tuple[int, Any], int, int, str, str]] = []
    for user_index, seq in enumerate(sequences):
        ts = timestamps[user_index] if user_index < len(timestamps) else []
        if len(ts) != len(seq):
            raise ValueError("strict_global_time 要求每个序列都有与课程一一对应的时间戳")
        if any(not str(value).strip() for value in ts):
            raise ValueError("strict_global_time 要求时间戳不能为空")
        for pos, (course, timestamp) in enumerate(zip(seq, ts)):
            events.append((_time_key(timestamp), user_index, pos, course, str(timestamp)))
    if not events:
        return SplitBundle(
            {"train": [], "val": [], "test": []},
            [[] for _ in sequences],
            [[] for _ in sequences],
            "strict_global_time",
        )
    events.sort(key=lambda row: (row[0], row[1], row[2]))
    train_ratio = min(max(float(train_ratio), 0.5), 0.95)
    val_ratio = min(max(float(val_ratio), 0.01), 0.4)
    train_index = min(len(events) - 1, max(0, int(math.floor(len(events) * train_ratio)) - 1))
    val_index = min(
        len(events) - 1,
        max(train_index, int(math.floor(len(events) * (train_ratio + val_ratio))) - 1),
    )
    train_cut = events[train_index][0]
    val_cut = events[val_index][0]

    examples: dict[str, list[SequenceExample]] = {"train": [], "val": [], "test": []}
    train_sequences: list[list[str]] = []
    train_timestamps: list[list[str]] = []
    for user_index, seq in enumerate(sequences):
        ts = timestamps[user_index]
        user_events = [(course, str(time)) for course, time in zip(seq, ts)]
        train_items = [(course, time) for course, time in user_events if _time_key(time) <= train_cut]
        val_items = [(course, time) for course, time in user_events if train_cut < _time_key(time) <= val_cut]
        test_items = [(course, time) for course, time in user_events if _time_key(time) > val_cut]
        train_sequences.append([course for course, _ in train_items])
        train_timestamps.append([time for _, time in train_items])
        for pos in range(1, len(train_items)):
            history = train_items[:pos]
            examples["train"].append(
                SequenceExample(
                    user_index,
                    [course for course, _ in history],
                    train_items[pos][0],
                    [time for _, time in history],
                    train_items[pos][1],
                )
            )
        prior_items = list(train_items)
        for target, target_time in val_items:
            if prior_items:
                examples["val"].append(
                    SequenceExample(
                        user_index,
                        [course for course, _ in prior_items],
                        target,
                        [time for _, time in prior_items],
                        target_time,
                    )
                )
            prior_items.append((target, target_time))
        for target, target_time in test_items:
            if prior_items:
                examples["test"].append(
                    SequenceExample(
                        user_index,
                        [course for course, _ in prior_items],
                        target,
                        [time for _, time in prior_items],
                        target_time,
                    )
                )
            prior_items.append((target, target_time))
    return SplitBundle(
        examples,
        train_sequences,
        train_timestamps,
        "strict_global_time",
        {"train_end": events[train_index][4], "validation_end": events[val_index][4]},
    )


def make_split_bundle(
    sequences: list[list[str]],
    timestamps: list[list[str]] | None = None,
    *,
    strategy: str = "leave_two_out",
    train_ratio: float = 0.8,
    val_ratio: float = 0.1,
) -> SplitBundle:
    strategy = strategy.lower()
    if strategy in {"leave_two_out", "leave-two-out", "loo"}:
        return _leave_two_out_bundle(sequences, timestamps)
    if strategy in {"strict_global_time", "global_time", "global"}:
        if timestamps is None:
            raise ValueError("strict_global_time 需要 MOOCCubeX enroll_time")
        return _global_time_bundle(sequences, timestamps, train_ratio, val_ratio)
    raise ValueError(f"未知切分策略: {strategy}")


def split_leave_two_out(
    sequences: list[list[str]], timestamps: list[list[str]] | None = None
) -> dict[str, list[tuple[list[str], str]]]:
    """Backward-compatible tuple split used by older callers and tests."""
    bundle = make_split_bundle(sequences, timestamps, strategy="leave_two_out")
    return {
        name: [(example.history, example.target) for example in examples]
        for name, examples in bundle.examples.items()
    }


def build_item_mapping(dataset: CourseDataset) -> tuple[dict[str, int], list[str]]:
    observed = {x for seq in dataset.sequences for x in seq}
    items = sorted(observed)
    return {item: i for i, item in enumerate(items)}, items


def build_course_prerequisites(
    course_concepts: dict[str, list[str]], edges: Iterable[tuple[str, str]]
) -> dict[str, set[str]]:
    """Map concept prerequisite edges to transitive prerequisites per course."""
    predecessors: dict[str, set[str]] = defaultdict(set)
    for source, target in _resolve_prerequisite_edges(edges, course_concepts):
        source = _normalise_concept_id(source)
        target = _normalise_concept_id(target)
        if source and target and source != target:
            predecessors[target].add(source)
    output: dict[str, set[str]] = {}
    for course_id, concepts in course_concepts.items():
        own = set(concepts)
        required: set[str] = set()
        queue = list(own)
        while queue:
            concept = queue.pop()
            for source in predecessors.get(concept, set()):
                if source in own or source in required:
                    continue
                required.add(source)
                queue.append(source)
        output[str(course_id)] = required
    return output
