#!/usr/bin/env python3
"""
Converte um documento OpenAPI 3.x JSON em uma collection Postman v2.1.

Uso:
  python3 scripts/export_openapi_to_postman.py http://localhost:8080/v3/api-docs
  python3 scripts/export_openapi_to_postman.py openapi.json doc/postman/api.postman_collection.json
"""

from __future__ import annotations

import json
import sys
import urllib.parse
import urllib.request
from collections import OrderedDict
from pathlib import Path


HTTP_METHODS = ["get", "post", "put", "patch", "delete", "head", "options"]


def load_openapi(source: str) -> tuple[dict, str]:
    parsed = urllib.parse.urlparse(source)
    if parsed.scheme in {"http", "https"}:
        with urllib.request.urlopen(source) as response:
            raw = response.read().decode("utf-8")
        base_url = f"{parsed.scheme}://{parsed.netloc}"
        return json.loads(raw), base_url

    path = Path(source)
    raw = path.read_text(encoding="utf-8")
    return json.loads(raw), "{{baseUrl}}"


def server_url(spec: dict, fallback_base_url: str) -> str:
    servers = spec.get("servers") or []
    if servers:
        url = servers[0].get("url", "").strip()
        if url == "/":
            return fallback_base_url
        if url:
            return url.rstrip("/")
    return fallback_base_url


def resolve_example(content: dict) -> str | None:
    app_json = (content or {}).get("application/json") or {}

    example = app_json.get("example")
    if example is not None:
        return json.dumps(example, ensure_ascii=False, indent=2) if not isinstance(example, str) else example

    examples = app_json.get("examples") or {}
    for value in examples.values():
        if "value" in value:
            sample = value["value"]
            return json.dumps(sample, ensure_ascii=False, indent=2) if not isinstance(sample, str) else sample

    schema = app_json.get("schema") or {}
    sample = schema_to_sample(schema, {})
    if sample is None:
        return None
    return json.dumps(sample, ensure_ascii=False, indent=2)


def schema_to_sample(schema: dict, components: dict) -> object | None:
    if not schema:
        return None

    ref = schema.get("$ref")
    if ref:
        name = ref.split("/")[-1]
        resolved = components.get("schemas", {}).get(name, {})
        return schema_to_sample(resolved, components)

    if "example" in schema:
        return schema["example"]

    if "enum" in schema and schema["enum"]:
        return schema["enum"][0]

    schema_type = schema.get("type")

    if schema_type == "object" or "properties" in schema:
        result = {}
        for prop_name, prop_schema in (schema.get("properties") or {}).items():
            result[prop_name] = schema_to_sample(prop_schema, components)
        return result

    if schema_type == "array":
        item = schema_to_sample(schema.get("items") or {}, components)
        return [] if item is None else [item]

    if schema_type == "integer":
        return 0
    if schema_type == "number":
        return 0
    if schema_type == "boolean":
        return False

    return ""


def to_postman_url(full_url: str) -> dict:
    parsed = urllib.parse.urlparse(full_url)
    path_segments = [segment for segment in parsed.path.split("/") if segment]
    host_segments = parsed.netloc.split(".") if parsed.netloc else ["{{baseUrl}}"]
    raw = full_url

    if full_url.startswith("{{baseUrl}}"):
        raw = full_url
        stripped = full_url.replace("{{baseUrl}}", "", 1)
        path_segments = [segment for segment in stripped.split("/") if segment]
        host_segments = ["{{baseUrl}}"]

    return {
        "raw": raw,
        "host": host_segments,
        "path": path_segments,
    }


def build_request(path: str, method: str, operation: dict, base_url: str, components: dict) -> dict:
    request = {
        "method": method.upper(),
        "header": [],
        "url": to_postman_url(f"{base_url.rstrip('/')}{path}"),
        "description": operation.get("description") or operation.get("summary") or "",
    }

    body = resolve_example(operation.get("requestBody", {}).get("content", {}))
    if body is not None:
        request["header"].append({"key": "Content-Type", "value": "application/json"})
        request["body"] = {
            "mode": "raw",
            "raw": body,
            "options": {"raw": {"language": "json"}},
        }

    return {
        "name": operation.get("summary") or f"{method.upper()} {path}",
        "request": request,
        "response": [],
    }


def build_collection(spec: dict, fallback_base_url: str) -> dict:
    info = spec.get("info") or {}
    components = spec.get("components") or {}
    base_url = server_url(spec, fallback_base_url)
    grouped = OrderedDict()

    for path, path_item in (spec.get("paths") or {}).items():
        for method in HTTP_METHODS:
            operation = path_item.get(method)
            if not operation:
                continue
            tags = operation.get("tags") or ["Default"]
            folder = tags[0]
            grouped.setdefault(folder, [])
            grouped[folder].append(build_request(path, method, operation, base_url, components))

    items = [{"name": folder, "item": requests} for folder, requests in grouped.items()]

    return {
        "info": {
            "name": info.get("title", "API Collection"),
            "_postman_id": "f0d1fb10-9d8f-4b34-bf44-4aa4f7f48f1e",
            "description": info.get("description", ""),
            "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json",
        },
        "item": items,
        "variable": [
            {"key": "baseUrl", "value": base_url},
            {"key": "imei", "value": "123458800000015"},
            {"key": "deviceId", "value": "1"},
        ],
    }


def main() -> int:
    if len(sys.argv) not in {2, 3}:
        print(__doc__.strip())
        return 1

    source = sys.argv[1]
    if len(sys.argv) == 3:
        output = Path(sys.argv[2])
    else:
        output = Path("doc/postman/openapi.postman_collection.json")

    spec, fallback_base_url = load_openapi(source)
    collection = build_collection(spec, fallback_base_url)
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(collection, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"Collection gerada em: {output}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
