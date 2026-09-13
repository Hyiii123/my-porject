# DRAG-KP4SR 知识拓扑增强个性化推荐微服务

## 1. 服务简介
本服务基于清华大学 MOOCCubeX 真实知识拓扑图谱与 DRAG-KP4SR 论文核心推荐算法架构构建，作为天机系统多智能体个性化推荐体系的核心算法引擎，通过 Docker 容器化运行于云端（容器名：`tianji-recommend`，端口：`5000`）。

## 2. 核心技术能力
- **先修知识图谱推导**：基于 265,305 条实体先修有向拓扑边（Prerequisite Graph），构建 BFS 知识前沿（Frontier Expansion）探测算法。
- **混合特征评分**：融合 BM25 文本语义相关性、目标岗位关键词加权对齐、先修拓扑可解释性距离。
- **语义桥接适配器**：实现天机业务课程库（IDs 1~320）与 MOOCCubeX 计算机科学核心体系（51门）的精准双向映射。
- **可解释性证据溯源**：在召回候选课程的同时，提取从学员已掌握知识点到推荐课程所需先修知识点的显式推导链路（`evidencePaths`），供 Spring AI 多智能体中解释生成智能体（ExplanationGenerationAgent）合成高说服力的推荐理由。

## 3. 接口规范

### 3.1 健康检查
- **路径**：`GET /api/recommend/health`
- **响应**：
```json
{
  "status": "UP",
  "engine": "DRAG-KP4SR",
  "dataSource": "MOOCCubeX-Official",
  "catalogCourses": 51,
  "prerequisiteEdges": 265305,
  "totalSequences": 42354
}
```

### 3.2 预测推荐
- **路径**：`POST /api/recommend/predict`
- **请求体**：
```json
{
  "userId": 201,
  "historyCourseIds": [1, 2],
  "intendedRole": "AI开发工程师",
  "preferredDifficulty": 2,
  "topSkills": ["Python", "机器学习"],
  "topK": 6,
  "useFrontier": true
}
```
- **核心响应属性**：
  - `data[].courseId`: 映射回天机数据库系统的课程 ID (1~320)
  - `data[].score`: 算法融合匹配分 (0~100)
  - `data[].matchTag`: 智能体标签 (`知识前沿突破` / `先修核心进阶` / `图谱综合推荐`)
  - `data[].features.evidencePaths`: 知识图谱先修推导链，如 `["K_导出_计算机科学与技术 --PREREQUISITE--> K_E1线路_计算机科学与技术"]`
