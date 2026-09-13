import json
import sys
from pathlib import Path
import threading
import time
import urllib.request
import unittest

BASE_DIR = Path(__file__).resolve().parent.parent
if str(BASE_DIR) not in sys.path:
    sys.path.insert(0, str(BASE_DIR))

from service.server import run_server
from service.model_holder import DragModelHolder

class TestDragServer(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.port = 5099
        cls.thread = threading.Thread(
            target=run_server,
            kwargs={"host": "127.0.0.1", "port": cls.port},
            daemon=True,
        )
        cls.thread.start()
        time.sleep(1.5)  # Wait for startup

    def test_health(self):
        url = f"http://127.0.0.1:{self.port}/api/recommend/health"
        req = urllib.request.Request(url)
        with urllib.request.urlopen(req) as resp:
            data = json.loads(resp.read().decode("utf-8"))
            self.assertEqual(data.get("status"), "UP")
            self.assertEqual(data.get("engine"), "DRAG-KP4SR")

    def test_predict(self):
        url = f"http://127.0.0.1:{self.port}/api/recommend/predict"
        payload = json.dumps({
            "userId": 1001,
            "historyCourseIds": [1, 4, 8],
            "intendedRole": "Java 架构师",
            "topSkills": ["Java", "SpringBoot", "MySQL"],
            "topK": 5
        }).encode("utf-8")
        req = urllib.request.Request(
            url,
            data=payload,
            headers={"Content-Type": "application/json"}
        )
        with urllib.request.urlopen(req) as resp:
            data = json.loads(resp.read().decode("utf-8"))
            self.assertEqual(data.get("code"), 200)
            self.assertIn("data", data)
            self.assertTrue(len(data["data"]) > 0)
            candidate = data["data"][0]
            self.assertIn("courseId", candidate)
            self.assertIn("score", candidate)
            self.assertIn("features", candidate)
            self.assertIn("evidencePaths", candidate["features"])
            print("Successfully verified prediction response:", candidate)

if __name__ == "__main__":
    unittest.main()
