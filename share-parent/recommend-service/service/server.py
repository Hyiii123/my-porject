from __future__ import annotations

import argparse
import json
import logging
import sys
from http import HTTPStatus
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path

# Ensure package root is in sys.path
BASE_DIR = Path(__file__).resolve().parent.parent
if str(BASE_DIR) not in sys.path:
    sys.path.insert(0, str(BASE_DIR))

from service.model_holder import DragModelHolder

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] [DRAG-Server] %(message)s",
)
logger = logging.getLogger("drag_server")


class DragRecommendHandler(BaseHTTPRequestHandler):
    """HTTP Request Handler for DRAG-KP4SR Inference Service."""

    def _send_json(self, status: int, data: dict):
        payload = json.dumps(data, ensure_ascii=False).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(payload)))
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Headers", "Content-Type, Authorization")
        self.send_header("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
        self.end_headers()
        self.wfile.write(payload)

    def do_OPTIONS(self):
        self.send_response(HTTPStatus.NO_CONTENT)
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Headers", "Content-Type, Authorization")
        self.send_header("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
        self.end_headers()

    def do_GET(self):
        path = self.path.split("?")[0].rstrip("/")
        if path in {"", "/api/recommend/health", "/health"}:
            holder = DragModelHolder.get_instance()
            self._send_json(
                HTTPStatus.OK,
                {
                    "status": "UP",
                    "engine": "DRAG-KP4SR",
                    "dataSource": holder.data_source,
                    "catalogCourses": len(holder.dataset.course_meta),
                    "prerequisiteEdges": len(holder.dataset.prerequisite_edges),
                    "totalSequences": len(holder.dataset.sequences),
                },
            )
        elif path == "/api/recommend/concept-graph":
            holder = DragModelHolder.get_instance()
            # Return sample graph nodes and edges for visualization
            edges = holder.dataset.prerequisite_edges[:50]
            clean_edges = [
                [holder.adapter.clean_concept_name(u), holder.adapter.clean_concept_name(v)]
                for u, v in edges
            ]
            self._send_json(
                HTTPStatus.OK,
                {
                    "code": 200,
                    "edges": clean_edges,
                    "totalEdges": len(holder.dataset.prerequisite_edges),
                },
            )
        else:
            self._send_json(HTTPStatus.NOT_FOUND, {"code": 404, "msg": "Endpoint not found"})

    def do_POST(self):
        path = self.path.split("?")[0].rstrip("/")
        if path in {
            "/api/recommend/predict",
            "/api/v1/recommend/rank",
            "/recommend/predict",
        }:
            content_length = int(self.headers.get("Content-Length", 0))
            if content_length <= 0:
                self._send_json(
                    HTTPStatus.BAD_REQUEST,
                    {"code": 400, "msg": "Empty request body"},
                )
                return

            body = self.rfile.read(content_length)
            try:
                payload = json.loads(body.decode("utf-8"))
            except Exception as ex:
                self._send_json(
                    HTTPStatus.BAD_REQUEST,
                    {"code": 400, "msg": f"Invalid JSON payload: {ex}"},
                )
                return

            try:
                holder = DragModelHolder.get_instance()
                result = holder.predict(payload)
                self._send_json(HTTPStatus.OK, result)
            except Exception as ex:
                logger.error("Error executing recommendation prediction: %s", ex, exc_info=True)
                self._send_json(
                    HTTPStatus.INTERNAL_SERVER_ERROR,
                    {"code": 500, "msg": f"Model inference error: {ex}"},
                )
        else:
            self._send_json(HTTPStatus.NOT_FOUND, {"code": 404, "msg": "Endpoint not found"})


def run_server(host: str = "0.0.0.0", port: int = 5000):
    holder = DragModelHolder.get_instance()
    server_address = (host, port)
    httpd = ThreadingHTTPServer(server_address, DragRecommendHandler)
    logger.info("DRAG-KP4SR Inference Server listening on http://%s:%d", host, port)
    logger.info("Healthcheck endpoint: http://%s:%d/api/recommend/health", host, port)
    logger.info("Prediction endpoint:  http://%s:%d/api/recommend/predict", host, port)
    try:
        httpd.serve_forever()
    except KeyboardInterrupt:
        logger.info("Shutting down DRAG-KP4SR server...")
        httpd.server_close()


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="DRAG-KP4SR Inference Server")
    parser.add_argument("--host", default="0.0.0.0", help="Binding host (default: 0.0.0.0)")
    parser.add_argument("--port", type=int, default=5000, help="Binding port (default: 5000)")
    args = parser.parse_args()
    run_server(host=args.host, port=args.port)
