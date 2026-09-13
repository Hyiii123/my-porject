import os
from fastapi import FastAPI, Query
from pydantic import BaseModel
from fastembed import TextEmbedding
import requests

app = FastAPI(title="Zhiwen Knowledge Embedding & Semantic Search Service")

QDRANT_URL = os.getenv("QDRANT_URL", "http://qdrant:6333")
COLLECTION_NAME = os.getenv("COLLECTION_NAME", "zhiwen_knowledge")
CACHE_DIR = os.getenv("FASTEMBED_CACHE_PATH", "/root/.cache/fastembed")

model = None

@app.on_event("startup")
def load_model():
    global model
    print(f"Loading BAAI/bge-small-zh-v1.5 model from {CACHE_DIR}...")
    model = TextEmbedding(model_name="BAAI/bge-small-zh-v1.5", cache_dir=CACHE_DIR)
    print("Model loaded successfully!")

@app.get("/health")
def health():
    return {"status": "ok", "collection": COLLECTION_NAME}

class EmbedRequest(BaseModel):
    text: str

@app.post("/embed")
def embed(req: EmbedRequest):
    vec = list(model.embed([req.text]))[0]
    return {"vector": [round(float(v), 6) for v in vec]}

@app.get("/search")
def search(q: str = Query(..., description="Query text"), limit: int = 3):
    from urllib.parse import unquote
    clean_q = unquote(q).strip()
    while "%" in clean_q:
        new_q = unquote(clean_q)
        if new_q == clean_q:
            break
        clean_q = new_q
    if not clean_q:
        return {"hits": []}
    vec = list(model.embed([clean_q]))[0]
    vec_list = [round(float(v), 6) for v in vec]
    
    search_payload = {
        "vector": vec_list,
        "limit": limit,
        "with_payload": True
    }
    try:
        resp = requests.post(
            f"{QDRANT_URL}/collections/{COLLECTION_NAME}/points/search",
            json=search_payload,
            timeout=3.0
        )
        if resp.status_code == 200:
            data = resp.json()
            hits = []
            for item in data.get("result", []):
                payload = item.get("payload", {})
                hits.append({
                    "id": item.get("id"),
                    "score": round(item.get("score", 0.0), 4),
                    "question": payload.get("question", ""),
                    "answer": payload.get("answer", ""),
                    "category": payload.get("category", ""),
                    "keywords": payload.get("keywords", "")
                })
            return {"hits": hits}
    except Exception as e:
        print(f"Error querying Qdrant: {e}")
    return {"hits": []}
