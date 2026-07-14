import json
from pathlib import Path

import pytest

from orryx_toolkit.service_http import ServiceHttpConfig, authorize, execute_http_request


def config(tmp_path: Path) -> ServiceHttpConfig:
    return ServiceHttpConfig("127.0.0.1", 9781, "s" * 32, tmp_path)


def test_config_requires_loopback_or_explicit_private_and_strong_secret(tmp_path: Path):
    base = {"RUNNER_SHARED_SECRET": "s" * 32, "RUNNER_WORKSPACE_ROOT": str(tmp_path)}
    assert ServiceHttpConfig.from_environment(base).bind == "127.0.0.1"
    with pytest.raises(ValueError):
        ServiceHttpConfig.from_environment(base | {"RUNNER_BIND": "0.0.0.0"})
    with pytest.raises(ValueError):
        ServiceHttpConfig.from_environment(base | {"RUNNER_SHARED_SECRET": "short"})


def test_authorization_is_bearer_and_request_is_bounded(tmp_path: Path):
    current = config(tmp_path)
    assert authorize("Bearer " + "s" * 32, current.shared_secret)
    assert not authorize("Bearer wrong", current.shared_secret)
    status, _ = execute_http_request(b"{}", None, current)
    assert status == 401
    status, _ = execute_http_request(b"x" * (current.max_request_bytes + 1), "Bearer " + "s" * 32, current)
    assert status == 413


def test_http_wrapper_preserves_runner_rejection(tmp_path: Path):
    envelope = {"envelopeVersion": "1.0", "contract": {"contractVersion": "1.0", "component": "ability", "operation": "materialize", "request": {}, "policy": {}}}
    status, payload = execute_http_request(json.dumps(envelope).encode(), "Bearer " + "s" * 32, config(tmp_path))
    assert status == 422
    assert any(error["code"] == "SERVICE_OPERATION_FORBIDDEN" for error in payload["errors"])
