"""
智问学伴 - 自研推荐算法独立 Python 接口服务模板 (FastAPI / Flask 参考实现)
运行命令:
    pip install fastapi uvicorn
    uvicorn recommend_model_service:app --host 0.0.0.0 --port 5000
"""

from fastapi import FastAPI
from pydantic import BaseModel
from typing import List, Dict, Optional, Any

app = FastAPI(title="Tianji Custom Recommendation Algorithm Service")

class RecommendRequest(BaseModel):
    userId: Optional[int] = None
    intendedRole: Optional[str] = ""
    preferredDifficulty: Optional[int] = 2
    topSkills: Optional[List[str]] = []
    skillWeights: Optional[Dict[str, int]] = {}
    skillGaps: Optional[List[str]] = []
    enrolledCourseIds: Optional[List[int]] = []
    topK: Optional[int] = 12

class CourseCandidate(BaseModel):
    courseId: int
    score: float
    matchTag: str
    features: Optional[Dict[str, Any]] = {}

class RecommendResponse(BaseModel):
    code: int = 200
    msg: str = "success"
    data: List[CourseCandidate]

@app.post("/api/recommend/predict", response_model=RecommendResponse)
def predict_recommendations(req: RecommendRequest):
    """
    接收 Java 多智能体系统的学员画像特征，执行您训练好的推荐模型推理。
    后续将您的模型推理逻辑 (如 PyTorch / TensorFlow / LightGBM / NGCF 等) 替换此处即可。
    """
    user_id = req.userId
    role = req.intendedRole or "Java 架构师"
    top_k = req.topK or 12
    enrolled = set(req.enrolledCourseIds or [])
    
    # === 此处嵌入您的真实训练模型 ===
    # 示例返回逻辑 (模拟模型输出)：
    # predictions = my_trained_model.predict(user_features)
    sample_candidates = []
    # 示例候选课程 ID 集合 (避开已选课)
    sample_ids = [21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 61, 62, 101, 102]
    for idx, cid in enumerate(sample_ids):
        if cid in enrolled:
            continue
        sample_candidates.append(CourseCandidate(
            courseId=cid,
            score=round(99.0 - idx * 1.5, 2),
            matchTag="自研深度模型精排",
            features={"model_version": "v1.0-exp", "latent_sim": 0.95 - idx * 0.02}
        ))
        if len(sample_candidates) >= top_k:
            break
            
    return RecommendResponse(code=200, msg="success", data=sample_candidates)

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=5000)
