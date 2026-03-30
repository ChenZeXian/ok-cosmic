import os
from typing import Any, Dict, List, Optional, Tuple

import requests
from cachetools import TTLCache
from mcp.server.fastmcp import FastMCP

mcp = FastMCP("cosmic-bill-metadata")

API_URL = os.getenv("COSMIC_META_API")
if not API_URL:
    raise RuntimeError("COSMIC_META_API is required")
API_TOKEN = os.getenv("COSMIC_META_TOKEN", "")
TIMEOUT_SECONDS = float(os.getenv("COSMIC_META_TIMEOUT", "8"))
cache = TTLCache(maxsize=512, ttl=900)


def _post(payload: Dict[str, Any]) -> Dict[str, Any]:
    headers = {"Content-Type": "application/json"}
    if API_TOKEN:
        headers["Authorization"] = f"Bearer {API_TOKEN}"
    resp = requests.post(API_URL, json=payload, headers=headers, timeout=TIMEOUT_SECONDS)
    resp.raise_for_status()
    raw = resp.json()

    # kapi 常见响应包裹：{"status": true/false, "errorCode": "...", "data": {...}}
    if isinstance(raw, dict) and "data" in raw:
        status = raw.get("status")
        if status is False:
            message = raw.get("message") or "remote api status=false"
            error_code = raw.get("errorCode")
            raise RuntimeError(f"{message} (errorCode={error_code})")
        data = raw.get("data")
        return data if isinstance(data, dict) else {}

    return raw if isinstance(raw, dict) else {}


def _normalize_new_fields(data: Dict[str, Any]) -> Tuple[List[Dict[str, Any]], List[Dict[str, Any]]]:
    form_fields = data.get("formFields")
    entity_fields = data.get("entityFields")

    if not isinstance(form_fields, list):
        form_fields = []
    if not isinstance(entity_fields, list):
        entity_fields = []
    return form_fields, entity_fields


def _build_field_index(
    form_fields: List[Dict[str, Any]],
    entity_fields: List[Dict[str, Any]],
) -> Dict[str, Dict[str, Any]]:
    idx: Dict[str, Dict[str, Any]] = {}

    for f in form_fields:
        key = f.get("key")
        if key:
            idx[f"form:key:{key}"] = f

    for f in entity_fields:
        key = f.get("key")
        if key:
            idx[f"entity:key:{key}"] = f

    return idx


@mcp.tool()
def get_meta_fields(
    formId: Optional[str] = None,
    billName: Optional[str] = None,
    fuzzyFields: Optional[List[str]] = None,
) -> Dict[str, Any]:
    """根据 formId（表单英文标识） / billName（表单中文名称） 获取单据元数据字段，优先通过 fuzzyFields（字段的英文标识/中文名称/正则表达式语句进行混合均可）进行字段模糊筛选，不传fuzzyFields参数默认查询所有字段信息（兜底策略，不要轻易尝试）"""
    if not formId and not billName:
        raise ValueError("formId 和 billName 至少传一个")

    cache_suffix = ""
    if fuzzyFields is not None:
        cache_suffix = f"|{repr(fuzzyFields)}"
    cache_key = f"{formId or ''}|{billName or ''}{cache_suffix}"
    if cache_key in cache:
        return {"status": "ok", "source": "cache", **cache[cache_key]}

    data_payload: Dict[str, Any] = {"formId": formId, "billName": billName}
    if fuzzyFields is not None:
        data_payload["fuzzyFields"] = fuzzyFields
    payload = {"data": data_payload}
    data = _post(payload)

    if data.get("code") == "MULTI_MATCH":
        return {
            "status": "need_confirm",
            "message": data.get("message", "匹配到多个单据"),
            "candidates": data.get("candidates", []),
        }

    if data.get("code") == "BILL_NOT_FOUND":
        return {
            "status": "not_found",
            "message": data.get("message", "未找到单据"),
        }

    form_fields, entity_fields = _normalize_new_fields(data)
    result = {
        "form": data.get("form", {}),
        "formFields": form_fields,
        "entityFields": entity_fields,
        "fieldIndex": _build_field_index(form_fields, entity_fields),
    }
    cache[cache_key] = result
    return {"status": "ok", "source": "api", **result}


if __name__ == "__main__":
    mcp.run(transport=os.getenv("MCP_TRANSPORT", "stdio"))
