"""Local prompt server for a paired phone running Mobilerun Portal."""

from __future__ import annotations

import argparse
import json
import os
import secrets
import shutil
import subprocess
import threading
import uuid
from dataclasses import dataclass, field
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import urlsplit

from .actions import compile_task


PAGE = (Path(__file__).parent / "web.html").read_bytes()
MAX_PROMPT = 2000
MAX_BODY = 16_384
MAX_OUTPUT = 12_000


@dataclass
class Task:
    id: str
    prompt: str
    agent_prompt: str
    action: str = "general"
    state: str = "awaiting_approval"
    output: str = ""
    exit_code: int | None = None

    def public(self) -> dict:
        return {"id": self.id, "prompt": self.prompt, "action": self.action, "state": self.state,
                "output": self.output, "exit_code": self.exit_code}


@dataclass
class TaskManager:
    runner: callable
    tasks: dict[str, Task] = field(default_factory=dict)
    lock: threading.Lock = field(default_factory=threading.Lock)

    def submit(self, prompt: str, action: str = "general", details: dict | None = None) -> Task:
        summary, agent_prompt = compile_task(action, prompt, details)
        with self.lock:
            if any(t.state in ("awaiting_approval", "running") for t in self.tasks.values()):
                raise RuntimeError("Finish the current task before starting another")
            task = Task(uuid.uuid4().hex, summary, agent_prompt, action)
            self.tasks[task.id] = task
            return task

    def get(self, task_id: str) -> Task | None:
        with self.lock:
            return self.tasks.get(task_id)

    def approve(self, task_id: str) -> Task:
        with self.lock:
            task = self.tasks.get(task_id)
            if task is None:
                raise KeyError(task_id)
            if task.state != "awaiting_approval":
                raise RuntimeError("Task has already started")
            task.state = "running"
        threading.Thread(target=self._execute, args=(task,), daemon=True).start()
        return task

    def _execute(self, task: Task) -> None:
        try:
            result = self.runner(task.agent_prompt)
            output = result.stdout + ("\n" + result.stderr if result.stderr else "")
            code = result.returncode
        except subprocess.TimeoutExpired:
            output, code = "Phone task timed out after 5 minutes.", 124
        except (OSError, RuntimeError) as exc:
            output, code = f"Could not start phone task: {exc}", 1
        with self.lock:
            task.output = output[-MAX_OUTPUT:]
            task.exit_code = code
            task.state = "finished" if code == 0 else "failed"


def mobilerun_runner(device: str | None, provider: str | None = None,
                     model: str | None = None, base_url: str | None = None,
                     vision: bool = False):
    def run(prompt: str) -> subprocess.CompletedProcess:
        if not device:
            raise RuntimeError("Select the intended phone with --device (see `uv run mobilerun devices`)")
        executable = shutil.which("mobilerun")
        if not executable:
            raise RuntimeError("Mobilerun is not installed; run uv sync --frozen")
        command = [executable, "run", "--steps", "20", "--no-stream",
                   "--save-trajectory", "none"]
        if device:
            command += ["--device", device]
        if provider:
            command += ["--provider", provider]
        if model:
            command += ["--model", model]
        if base_url:
            command += ["--base_url", base_url]
        if vision:
            command += ["--vision"]
        command.append(prompt)
        return subprocess.run(command, capture_output=True, text=True,
                              errors="replace", timeout=300, check=False)
    return run


class TaskHTTPServer(ThreadingHTTPServer):
    def __init__(self, address, manager: TaskManager, token: str):
        super().__init__(address, TaskHandler)
        self.manager = manager
        self.token = token


class TaskHandler(BaseHTTPRequestHandler):
    server: TaskHTTPServer

    def log_message(self, format, *args):
        # Keep prompts and access tokens out of request logs.
        pass

    def _send(self, status: int, payload: dict) -> None:
        data = json.dumps(payload).encode()
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(data)))
        self.send_header("Cache-Control", "no-store")
        self.send_header("X-Content-Type-Options", "nosniff")
        self.end_headers()
        self.wfile.write(data)

    def _authorized(self) -> bool:
        supplied = self.headers.get("Authorization", "")
        if not secrets.compare_digest(supplied, "Bearer " + self.server.token):
            self._send(401, {"error": "Access token required"})
            return False
        return True

    def do_GET(self):
        path = urlsplit(self.path).path
        if path == "/":
            self.send_response(200)
            self.send_header("Content-Type", "text/html; charset=utf-8")
            self.send_header("Content-Length", str(len(PAGE)))
            self.send_header("Cache-Control", "no-store")
            self.send_header("X-Content-Type-Options", "nosniff")
            self.end_headers()
            self.wfile.write(PAGE)
            return
        if not self._authorized():
            return
        if path.startswith("/api/tasks/"):
            task = self.server.manager.get(path.removeprefix("/api/tasks/"))
            self._send(200, task.public()) if task else self._send(404, {"error": "Task not found"})
            return
        self._send(404, {"error": "Not found"})

    def do_POST(self):
        if not self._authorized():
            return
        length = self.headers.get("Content-Length", "")
        if not length.isdigit() or int(length) > MAX_BODY:
            self._send(413, {"error": "Invalid request size"})
            return
        try:
            body = json.loads(self.rfile.read(int(length)))
        except (ValueError, UnicodeDecodeError):
            self._send(400, {"error": "Invalid JSON"})
            return
        path = urlsplit(self.path).path
        try:
            if path == "/api/tasks":
                if not isinstance(body, dict) or not isinstance(body.get("prompt"), str):
                    raise ValueError("A text prompt is required")
                self._send(201, self.server.manager.submit(
                    body["prompt"], body.get("action", "general"), body.get("details")
                ).public())
            elif path.startswith("/api/tasks/") and path.endswith("/approve"):
                task_id = path[len("/api/tasks/"):-len("/approve")]
                self._send(200, self.server.manager.approve(task_id).public())
            else:
                self._send(404, {"error": "Not found"})
        except ValueError as exc:
            self._send(400, {"error": str(exc)})
        except KeyError:
            self._send(404, {"error": "Task not found"})
        except RuntimeError as exc:
            self._send(409, {"error": str(exc)})


def main() -> None:
    parser = argparse.ArgumentParser(description="Prompt a paired Android phone")
    parser.add_argument("prompt", nargs="?", help="Run one task from the command line")
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--port", type=int, default=8765)
    parser.add_argument("--device", help="Mobilerun device serial or Portal URL")
    parser.add_argument("--model-provider", help="Mobilerun provider, for example Ollama")
    parser.add_argument("--model", help="Model name selected at the provider")
    parser.add_argument("--model-base-url", help="Local model server URL")
    parser.add_argument("--vision", action="store_true", help="Also show screenshots to a vision-capable model")
    parser.add_argument("--allow-http-lan", action="store_true",
                        help="Allow binding beyond loopback on a trusted local network")
    args = parser.parse_args()
    if args.model_provider == "Ollama" and (not args.model or not args.model_base_url):
        parser.error("Ollama requires --model and --model-base-url")
    runner = mobilerun_runner(args.device, args.model_provider, args.model,
                              args.model_base_url, args.vision)
    if args.prompt is not None:
        if not args.prompt.strip() or len(args.prompt) > MAX_PROMPT:
            parser.error(f"Prompt must contain 1–{MAX_PROMPT} characters")
        print(f"Ready to run on your phone: {args.prompt}")
        if input("Run this task? [y/N] ").strip().lower() not in ("y", "yes"):
            print("Task cancelled")
            return
        try:
            result = runner(args.prompt)
        except (subprocess.TimeoutExpired, OSError, RuntimeError) as exc:
            parser.exit(1, f"Phone task failed: {exc}\n")
        if result.stdout:
            print(result.stdout)
        if result.stderr:
            print(result.stderr)
        raise SystemExit(result.returncode)
    if args.host not in ("127.0.0.1", "localhost", "::1") and not args.allow_http_lan:
        parser.error("LAN access requires --allow-http-lan; use HTTPS for access over the Internet")
    token = os.environ.get("MOBILEAIHAND_ACCESS_TOKEN") or secrets.token_urlsafe(24)
    server = TaskHTTPServer((args.host, args.port), TaskManager(runner), token)
    print(f"Open http://{args.host}:{server.server_port} in a browser", flush=True)
    if "MOBILEAIHAND_ACCESS_TOKEN" not in os.environ:
        print(f"Access token: {token}", flush=True)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass
    finally:
        server.server_close()


if __name__ == "__main__":
    main()
