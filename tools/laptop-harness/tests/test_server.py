import http.client
import json
import subprocess
import threading
import time
import unittest
from unittest.mock import patch

from mobileaihand.actions import compile_task
from mobileaihand.server import TaskHTTPServer, TaskManager, mobilerun_runner


class ServerTest(unittest.TestCase):
    def setUp(self):
        self.calls = []

        def runner(prompt):
            self.calls.append(prompt)
            return subprocess.CompletedProcess([], 0, stdout="Phone reported Android 15", stderr="")

        self.server = TaskHTTPServer(("127.0.0.1", 0), TaskManager(runner), "test-token")
        self.thread = threading.Thread(target=self.server.serve_forever, daemon=True)
        self.thread.start()

    def tearDown(self):
        self.server.shutdown()
        self.server.server_close()
        self.thread.join(timeout=2)

    def request(self, method, path, body=None, token="test-token"):
        conn = http.client.HTTPConnection("127.0.0.1", self.server.server_port, timeout=2)
        headers = {"Authorization": "Bearer " + token}
        if body is not None:
            body = json.dumps(body)
            headers["Content-Type"] = "application/json"
        conn.request(method, path, body, headers)
        response = conn.getresponse()
        result = response.status, json.loads(response.read())
        conn.close()
        return result

    def test_approval_required_and_one_task_at_a_time(self):
        status, task = self.request("POST", "/api/tasks", {"prompt": "Open Settings"})
        self.assertEqual(status, 201)
        self.assertEqual(task["state"], "awaiting_approval")
        self.assertEqual(self.calls, [])
        self.assertEqual(self.request("POST", "/api/tasks", {"prompt": "Another"})[0], 409)
        self.assertEqual(self.request("POST", f"/api/tasks/{task['id']}/approve", {})[0], 200)
        for _ in range(50):
            status, result = self.request("GET", f"/api/tasks/{task['id']}")
            if result["state"] != "running":
                break
            time.sleep(.01)
        self.assertEqual(result["state"], "finished")
        self.assertEqual(self.calls, ["Open Settings"])
        self.assertIn("Android 15", result["output"])
        self.assertEqual(self.request("POST", f"/api/tasks/{task['id']}/approve", {})[0], 409)

    def test_authentication_and_input_validation(self):
        conn = http.client.HTTPConnection("127.0.0.1", self.server.server_port, timeout=2)
        conn.request("GET", "/")
        response = conn.getresponse()
        self.assertEqual(response.status, 200)
        self.assertIn(b"mobileAiHand", response.read())
        conn.close()
        self.assertEqual(self.request("POST", "/api/tasks", {"prompt": "Hi"}, token="bad")[0], 401)
        self.assertEqual(self.request("POST", "/api/tasks", {"prompt": "   "})[0], 400)
        self.assertEqual(self.request("POST", "/api/tasks", {"prompt": "x" * 2001})[0], 400)
        self.assertEqual(self.calls, [])

    def test_device_selection_required_before_dispatch(self):
        with self.assertRaisesRegex(RuntimeError, "--device"):
            mobilerun_runner(None)("Open Settings")

    def test_selected_device_is_passed_to_agent(self):
        fake_result = subprocess.CompletedProcess([], 0, stdout="done", stderr="")
        with patch("mobileaihand.server.shutil.which", return_value="/usr/bin/mobilerun"), \
             patch("mobileaihand.server.subprocess.run", return_value=fake_result) as run:
            self.assertIs(mobilerun_runner("pixel-8")("Open Settings"), fake_result)
        command = run.call_args.args[0]
        self.assertEqual(command[command.index("--device") + 1], "pixel-8")
        self.assertEqual(command[-1], "Open Settings")

    def test_guided_tasks_keep_recipient_and_account_details(self):
        summary, instruction = compile_task("reddit", "", {
            "community": "r/android", "title": "New app", "body": "A draft post"
        })
        self.assertIn("r/android", summary)
        self.assertIn("A draft post", instruction)
        self.assertIn("check for a visible post", instruction)
        summary, instruction = compile_task("whatsapp_photos", "", {"contact": "Alex"})
        self.assertIn("Alex", summary)
        self.assertIn("phone's local calendar date", instruction)
        self.assertIn("recipient cannot be verified", instruction)
        with self.assertRaises(ValueError):
            compile_task("reddit", "", {"community": "bad/community", "title": "T", "body": "B"})

    def test_model_settings_are_passed_to_agent(self):
        fake_result = subprocess.CompletedProcess([], 0, stdout="done", stderr="")
        with patch("mobileaihand.server.shutil.which", return_value="/usr/bin/mobilerun"), \
             patch("mobileaihand.server.subprocess.run", return_value=fake_result) as run:
            mobilerun_runner("pixel-8", "Ollama", "gemma3:4b", "http://localhost:11434", True)("Test")
        command = run.call_args.args[0]
        self.assertIn("Ollama", command)
        self.assertIn("gemma3:4b", command)
        self.assertIn("http://localhost:11434", command)
        self.assertIn("--vision", command)


if __name__ == "__main__":
    unittest.main()
