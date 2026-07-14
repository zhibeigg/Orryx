"""Loopback/private HTTP wrapper for the hardened Creation Suite service runner."""
from __future__ import annotations

from dataclasses import dataclass
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import hmac
import ipaddress
import json
import os
from pathlib import Path
from typing import Any, Mapping

from .service_runner import run_service_request


@dataclass(frozen=True)
class ServiceHttpConfig:
    bind: str
    port: int
    shared_secret: str
    workspace_root: Path
    workspace_mode: str = "project"
    max_request_bytes: int = 1_048_576

    @classmethod
    def from_environment(cls, environment: Mapping[str, str] | None = None) -> "ServiceHttpConfig":
        env = dict(os.environ if environment is None else environment)
        bind = env.get("RUNNER_BIND", "127.0.0.1").strip()
        port = int(env.get("RUNNER_PORT", "9781"))
        secret = env.get("RUNNER_SHARED_SECRET", "")
        root = Path(env.get("RUNNER_WORKSPACE_ROOT", "")).expanduser()
        mode = env.get("RUNNER_WORKSPACE_MODE", "project").strip().casefold()
        maximum = int(env.get("RUNNER_MAX_REQUEST_BYTES", "1048576"))
        allow_private = env.get("RUNNER_ALLOW_PRIVATE_BIND", "false").strip().casefold() == "true"
        address = ipaddress.ip_address(bind)
        if not address.is_loopback and not (allow_private and address.is_private):
            raise ValueError("RUNNER_BIND 必须是 loopback；私网监听需要显式 RUNNER_ALLOW_PRIVATE_BIND=true")
        if port not in range(1, 65536):
            raise ValueError("RUNNER_PORT 无效")
        if len(secret) < 32 or any(char.isspace() for char in secret):
            raise ValueError("RUNNER_SHARED_SECRET 至少 32 字符且不能包含空白")
        if not str(root) or str(root) == ".":
            raise ValueError("RUNNER_WORKSPACE_ROOT 不能为空")
        root = root.resolve()
        if mode not in {"project", "standalone"}:
            raise ValueError("RUNNER_WORKSPACE_MODE 无效")
        if maximum not in range(1024, 16 * 1024 * 1024 + 1):
            raise ValueError("RUNNER_MAX_REQUEST_BYTES 无效")
        return cls(bind, port, secret, root, mode, maximum)


def authorize(header: str | None, shared_secret: str) -> bool:
    prefix = "Bearer "
    if header is None or not header.startswith(prefix):
        return False
    return hmac.compare_digest(header[len(prefix):].encode(), shared_secret.encode())


def execute_http_request(body: bytes, authorization: str | None, config: ServiceHttpConfig) -> tuple[int, dict[str, Any]]:
    if not authorize(authorization, config.shared_secret):
        return 401, {"status": "rejected", "errors": [{"code": "UNAUTHORIZED", "pointer": "", "message": "认证失败"}]}
    if len(body) > config.max_request_bytes:
        return 413, {"status": "rejected", "errors": [{"code": "REQUEST_TOO_LARGE", "pointer": "", "message": "请求体过大"}]}
    try:
        value = json.loads(body.decode("utf-8"))
    except (UnicodeDecodeError, json.JSONDecodeError):
        return 400, {"status": "rejected", "errors": [{"code": "INVALID_JSON", "pointer": "", "message": "JSON 无效"}]}
    result = run_service_request(value, workspace_root=config.workspace_root, workspace_mode=config.workspace_mode)
    return (200 if result.get("status") == "completed" else 422), result


def serve(config: ServiceHttpConfig) -> None:
    class Handler(BaseHTTPRequestHandler):
        server_version = "OrryxRunner/1"

        def log_message(self, format: str, *args: object) -> None:
            return

        def do_GET(self) -> None:  # noqa: N802
            if self.path != "/health/live":
                self.send_error(404)
                return
            self._json(200, {"status": "UP"})

        def do_POST(self) -> None:  # noqa: N802
            if self.path != "/v1/run":
                self.send_error(404)
                return
            try:
                length = int(self.headers.get("Content-Length", "0"))
            except ValueError:
                self._json(400, {"status": "rejected", "errors": [{"code": "INVALID_LENGTH", "pointer": "", "message": "Content-Length 无效"}]})
                return
            if length < 0 or length > config.max_request_bytes:
                self._json(413, {"status": "rejected", "errors": [{"code": "REQUEST_TOO_LARGE", "pointer": "", "message": "请求体过大"}]})
                return
            status, payload = execute_http_request(self.rfile.read(length), self.headers.get("Authorization"), config)
            self._json(status, payload)

        def _json(self, status: int, payload: Mapping[str, Any]) -> None:
            encoded = json.dumps(payload, ensure_ascii=False, separators=(",", ":")).encode("utf-8")
            self.send_response(status)
            self.send_header("Content-Type", "application/json; charset=utf-8")
            self.send_header("Cache-Control", "no-store")
            self.send_header("Content-Length", str(len(encoded)))
            self.end_headers()
            self.wfile.write(encoded)

    ThreadingHTTPServer((config.bind, config.port), Handler).serve_forever()
