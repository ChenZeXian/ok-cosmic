#!/usr/bin/env python3
# SPDX-License-Identifier: NOASSERTION
"""
cosmic-form-metadata.py — Cosmic form metadata query and cache tool.

Usage:
    python3 cosmic-form-metadata.py --config ok-cosmic.json get --form-id <formId>
    python3 cosmic-form-metadata.py --config ok-cosmic.json get --bill-name <billName>
    python3 cosmic-form-metadata.py --config ok-cosmic.json get --form-id <formId> --fuzzy qty price amount
    python3 cosmic-form-metadata.py --config ok-cosmic.json get --form-id <formId> --fuzzy status --show-detail
    python3 cosmic-form-metadata.py --config ok-cosmic.json get --form-id <formId> --tree
    python3 cosmic-form-metadata.py --config ok-cosmic.json get --form-id <formId> --refresh

What it provides:
1. Form metadata lookup by formId or billName
2. Local SQLite cache for metadata payloads
3. Fuzzy field filtering for keys / names
4. Optional detail mode for enum mappings and reference types

What it does NOT do:
- It does not query the form_metadata_cache table directly for user-facing output semantics
- It does not infer field meaning beyond metadata returned by the configured API
- It does not rebuild the API knowledge graph database

Prerequisites:
- A valid ok-cosmic.json or equivalent meta config
- A reachable COSMIC_META_API / meta.apiUrl endpoint for cache misses
"""

import json
import os
import sys
import argparse
import time
import sqlite3
import re
import ssl
import urllib.error
import urllib.request
from typing import Any, Dict, List, Optional

from config_loader import load_project_config


class MetadataDbCache:
    def __init__(self, db_path: str, ttl: int = 600):
        self.db_path = db_path
        self.ttl = ttl
        self._init_db()

    def _init_db(self):
        try:
            with sqlite3.connect(self.db_path) as conn:
                conn.execute("""
                    CREATE TABLE IF NOT EXISTS form_metadata_cache (
                        form_id TEXT PRIMARY KEY,
                        payload TEXT,
                        updated_at INTEGER
                    )
                """)
        except Exception as e:
            print(f" (DEBUG) 初始化数据库失败: {e}", file=sys.stderr)

    def get(self, form_id: str) -> Optional[Dict[str, Any]]:
        try:
            with sqlite3.connect(self.db_path) as conn:
                conn.row_factory = sqlite3.Row
                row = conn.execute(
                    "SELECT payload, updated_at FROM form_metadata_cache WHERE form_id = ?",
                    (form_id,)
                ).fetchone()
                if not row or (time.time() - row['updated_at'] > self.ttl):
                    return None
                return json.loads(row['payload'])
        except Exception:
            return None

    def set(self, form_id: str, payload: Dict[str, Any]):
        try:
            with sqlite3.connect(self.db_path) as conn:
                conn.execute(
                    "INSERT OR REPLACE INTO form_metadata_cache (form_id, payload, updated_at) VALUES (?, ?, ?)",
                    (form_id, json.dumps(payload, ensure_ascii=False), int(time.time()))
                )
        except Exception as e:
            print(f" (DEBUG) 写入数据库失败: {e}", file=sys.stderr)

    def remove(self, form_id: str):
        try:
            with sqlite3.connect(self.db_path) as conn:
                conn.execute("DELETE FROM form_metadata_cache WHERE form_id = ?", (form_id,))
        except Exception:
            pass


class FormMetadata:
    def __init__(self, config: Dict[str, Any], debug: bool = False):
        self.debug = debug
        self.config_dir = str(config.get("__config_dir__", "")).strip()
        graph_config = config.get("graph", {})
        meta_config = config.get("meta", config)
        db_path = os.getenv("COSMIC_KNOWLEDGE_DB") or str(graph_config.get("dbPath", "references/ok-cosmic-knowledge.db"))
        raw_db_path = os.path.expanduser(db_path)
        if os.path.isabs(raw_db_path):
            self.db_path = os.path.normpath(raw_db_path)
        else:
            base_dir = self.config_dir or os.getcwd()
            self.db_path = os.path.normpath(os.path.abspath(os.path.join(base_dir, raw_db_path)))
        self.cache = MetadataDbCache(self.db_path)
        self.api_url = (os.getenv("COSMIC_META_API", "").strip() or str(meta_config.get("apiUrl", "")))
        self.timeout = float(meta_config.get("timeoutSeconds", 15))
        self.skip_ssl_verify = meta_config.get("skipSslVerify", True)

    def _log_debug(self, msg: str):
        if self.debug:
            print(f" (DEBUG) {msg}", file=sys.stderr)

    def _post(self, payload: Dict[str, Any]) -> Dict[str, Any]:
        if not self.api_url:
            raise RuntimeError("COSMIC_META_API is not configured.")
        req_body = json.dumps(payload, ensure_ascii=False).encode("utf-8")
        req = urllib.request.Request(self.api_url, data=req_body, headers={"Content-Type": "application/json"}, method="POST")
        try:
            ssl_context = ssl.create_default_context()
            if self.skip_ssl_verify:
                ssl_context.check_hostname = False
                ssl_context.verify_mode = ssl.CERT_NONE
            with urllib.request.urlopen(req, timeout=self.timeout, context=ssl_context) as resp:
                return json.loads(resp.read().decode("utf-8", errors="replace"))
        except Exception as e:
            raise RuntimeError(f"Network error: {e}")

    @staticmethod
    def _normalize_operates(raw_operates: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
        """
        兼容旧版 `buttons` 与新版 `operateMetas` 结构:
        - buttons: {name, key, ...}
        - operateMetas: {opName, opKey, opType}
        统一输出为: {name, key, type}
        """
        normalized: List[Dict[str, Any]] = []
        for op in raw_operates:
            if not isinstance(op, dict):
                continue
            key = op.get("key") or op.get("opKey")
            name = op.get("name") or op.get("opName")
            op_type = op.get("type") or op.get("opType")
            if not key and not name:
                continue
            normalized.append({
                "name": name or "-",
                "key": key or "-",
                "type": op_type or "-"
            })
        return normalized

    @staticmethod
    def _field_sort_key(field: Dict[str, Any], sort_by: str = "key") -> tuple:
        value = str(field.get(sort_by, "") or "").lower()
        key = str(field.get("key", "") or "").lower()
        name = str(field.get("name", "") or "").lower()
        node_type = str(field.get("type", "") or "").lower()
        db_key = str(field.get("dbKey", "") or "").lower()
        return (value, key, name, node_type, db_key)

    @classmethod
    def _sort_fields(cls, fields: List[Dict[str, Any]], sort_by: str = "key") -> List[Dict[str, Any]]:
        return sorted(fields, key=lambda f: cls._field_sort_key(f, sort_by=sort_by))

    @classmethod
    def _build_tree_lines(
        cls,
        fields: List[Dict[str, Any]],
        filter_patterns: Optional[List[str]] = None,
        sort_by: str = "key"
    ) -> List[str]:
        def is_hit(f: Dict[str, Any]) -> bool:
            if not filter_patterns:
                return True
            target_text = (
                str(f.get("key", ""))
                + "|"
                + str(f.get("name", ""))
                + "|"
                + str(f.get("type", ""))
                + "|"
                + str(f.get("dbKey", ""))
                + "|"
                + str(f.get("refType", ""))
            ).lower()
            for p in filter_patterns:
                try:
                    if re.search(p, target_text, re.IGNORECASE):
                        return True
                except Exception:
                    if p.lower() in target_text:
                        return True
            return False

        by_key: Dict[str, Dict[str, Any]] = {}
        for f in fields:
            key = str(f.get("key", "")).strip()
            if key and key not in by_key:
                by_key[key] = f

        if not by_key:
            return []

        children: Dict[Optional[str], List[str]] = {}
        for k, f in by_key.items():
            parent = f.get("parentKey")
            parent_key = str(parent).strip() if parent is not None else None
            if parent_key == "":
                parent_key = None
            children.setdefault(parent_key, []).append(k)

        for parent_key, ks in children.items():
            children[parent_key] = sorted(
                ks,
                key=lambda k: cls._field_sort_key(by_key.get(k, {}), sort_by=sort_by)
            )

        include_keys = set(by_key.keys())
        if filter_patterns:
            include_keys = {k for k, f in by_key.items() if is_hit(f)}
            # 补齐祖先节点，便于阅读路径
            queue = list(include_keys)
            while queue:
                cur = queue.pop()
                parent = by_key.get(cur, {}).get("parentKey")
                parent_key = str(parent).strip() if parent is not None else None
                if parent_key and parent_key in by_key and parent_key not in include_keys:
                    include_keys.add(parent_key)
                    queue.append(parent_key)

        def fmt_node(key: str) -> str:
            f = by_key[key]
            name = f.get("name", "-")
            node_type = f.get("type", "-")
            db_key = f.get("dbKey", "-")
            return f"{name} (`{key}`) [{node_type}] dbKey=`{db_key}`"

        lines: List[str] = []
        visited: set = set()

        def walk(node_key: str, prefix: str, is_last: bool):
            if node_key in visited or node_key not in include_keys:
                return
            visited.add(node_key)
            branch = "`- " if is_last else "|- "
            lines.append(f"{prefix}{branch}{fmt_node(node_key)}")
            child_keys = [ck for ck in children.get(node_key, []) if ck in include_keys]
            for i, ck in enumerate(child_keys):
                next_prefix = prefix + ("   " if is_last else "|  ")
                walk(ck, next_prefix, i == len(child_keys) - 1)

        roots = []
        for k in sorted(include_keys, key=lambda x: cls._field_sort_key(by_key.get(x, {}), sort_by=sort_by)):
            parent = by_key.get(k, {}).get("parentKey")
            parent_key = str(parent).strip() if parent is not None else None
            if not parent_key or parent_key not in include_keys:
                roots.append(k)

        roots = sorted(set(roots), key=lambda x: cls._field_sort_key(by_key.get(x, {}), sort_by=sort_by))
        for i, rk in enumerate(roots):
            walk(rk, "", i == len(roots) - 1)

        return lines

    def get_meta_fields(
        self,
        formId: Optional[str] = None,
        billName: Optional[str] = None,
        filter_patterns: Optional[List[str]] = None,
        show_detail: bool = False,
        tree_view: bool = False,
        sort_by: str = "key",
        view: str = "all"
    ) -> str:
        if not formId and not billName:
            return "❌ 错误: 必须提供 formId 或 billName"

        target_payload = None
        if formId:
            target_payload = self.cache.get(formId)
            if target_payload: self._log_debug(f"命中缓存 (FormId: {formId})")

        if not target_payload:
            self._log_debug("数据库无有效缓存，发起远程全量拉取...")
            # 始终拉取全量用于本地缓存
            payload = {"data": {"formId": formId or "", "billName": billName or "", "fuzzyFields": []}}
            resp = self._post(payload)
            if not resp.get("status"):
                return f"❌ 接口请求失败: {resp.get('message', '未知错误')}"

            data = resp.get("data", {})
            if data.get("code") in ("MULTI_MATCH", "BILL_NOT_FOUND"):
                msg = data.get("message", "单据未找到")
                cand_str = "\n".join([f"- {c.get('formName')} (`{c.get('formId')}`)" for c in data.get("candidates", [])])
                return f"❌ {msg}\n{cand_str}"

            target_payload = data
            real_form_id = data.get("form", {}).get("formId")
            if real_form_id:
                self.cache.set(real_form_id, data)

        # 数据提取
        form = target_payload.get("form", {})
        form_fields = target_payload.get("formFields") or []
        entity_fields = target_payload.get("entityFields") or []
        raw_operates = (
            target_payload.get("operateMetas")
            or target_payload.get("buttons")
            or []
        )
        buttons = self._normalize_operates(raw_operates)

        # 过滤与搜索逻辑
        AUDIT_FIELDS = {'creator', 'createtime', 'modifier', 'modifytime', 'org', 'createorg', 'group', 'status', 'enable'}

        def is_biz_field(f):
            k = str(f.get('key', '')).lower()
            return not any(a in k for a in AUDIT_FIELDS)

        def is_hit(f):
            if not filter_patterns: return True
            target_text = (
                str(f.get('key', ''))
                + "|"
                + str(f.get('name', ''))
                + "|"
                + str(f.get('type', ''))
            ).lower()
            for p in filter_patterns:
                try:
                    if re.search(p, target_text, re.IGNORECASE): return True
                except Exception:
                    if p.lower() in target_text: return True
            return False

        # 应用过滤
        biz_form_fields = [f for f in form_fields if is_biz_field(f)]
        biz_entity_fields = [f for f in entity_fields if is_biz_field(f)]
        biz_form_fields = self._sort_fields(biz_form_fields, sort_by=sort_by)
        biz_entity_fields = self._sort_fields(biz_entity_fields, sort_by=sort_by)

        display_form_fields = [f for f in biz_form_fields if is_hit(f)]
        display_entity_fields = [f for f in biz_entity_fields if is_hit(f)]
        display_buttons = [b for b in buttons if is_hit(b)]

        view = (view or "all").strip()
        show_form = view in ("form", "all")
        show_entity = view in ("entity", "all")
        show_operate = view in ("operate", "all")

        selected_biz_form_fields = biz_form_fields if show_form else []
        selected_biz_entity_fields = biz_entity_fields if show_entity else []
        selected_display_form_fields = display_form_fields if show_form else []
        selected_display_entity_fields = display_entity_fields if show_entity else []
        selected_display_buttons = display_buttons if show_operate else []
        selected_buttons = buttons if show_operate else []

        md = [f"## 📋 单据: {form.get('formName')} ({form.get('formId')})"]
        md.append(
            "**模型信息**: "
            f"dbTableKey=`{form.get('dbTableKey', '-')}`, "
            f"dbRoute=`{form.get('dbRoute', '-')}`, "
            f"modelType=`{form.get('modelType', '-')}`"
        )
        md.append(f"**视图**: `{view}`")
        md.append(f"**元数据状态**: 已缓存本地 (DB驱动)\n")

        # 根据是否有过滤词切换视图
        if tree_view:
            all_biz = []
            seen = set()
            for f in selected_biz_form_fields + selected_biz_entity_fields:
                k = f.get("key")
                if k and k not in seen:
                    all_biz.append(f)
                    seen.add(k)

            tree_lines = self._build_tree_lines(all_biz, filter_patterns=filter_patterns, sort_by=sort_by) if all_biz else []
            if filter_patterns:
                md.append(f"### 🌳 字段树 (按条件筛选: {', '.join(filter_patterns)}，排序: {sort_by})")
            else:
                md.append(f"### 🌳 字段树 (按 parentKey，排序: {sort_by})")

            if tree_lines:
                md.extend(tree_lines)
            else:
                md.append("> 未找到匹配字段（当前 view 可能不包含字段类型）")

            if selected_display_buttons:
                md.append("\n### 🔘 操作按钮 (匹配)")
                md.append(", ".join([f"{b.get('name')}(`{b.get('key')}`/{b.get('type')})" for b in selected_display_buttons]))
        elif filter_patterns:
            md.append(f"### 🔍 字段详情 (按条件筛选: {', '.join(filter_patterns)}，排序: {sort_by})")
            if selected_display_form_fields or selected_display_entity_fields:
                if show_detail:
                    md.append("| 名称 | 标识 (Key) | 类型 | 数据库字段 (dbKey) | 详情 (枚举/基础资料引用) |")
                    md.append("| :--- | :--- | :--- | :--- | :--- |")
                else:
                    md.append("| 名称 | 标识 (Key) | 类型 | 数据库字段 (dbKey) | 绑定路径 |")
                    md.append("| :--- | :--- | :--- | :--- | :--- |")

                # 优先显示表单字段，再显示实体字段（去重）
                seen_keys = set()
                for f in selected_display_form_fields + selected_display_entity_fields:
                    if f.get('key') not in seen_keys:
                        if show_detail:
                            detail_parts = []
                            ext_map = f.get('extMap')
                            ref_type = f.get('refType')
                            db_key = f.get('dbKey', '-')
                            if ext_map:
                                detail_parts.append("枚举: " + ", ".join([f"{k}:{v}" for k, v in ext_map.items()]))
                            if ref_type:
                                detail_parts.append(f"基础资料引用: `{ref_type}`")
                            detail_str = "；".join(detail_parts) if detail_parts else "-"
                            md.append(f"| {f.get('name')} | `{f.get('key')}` | {f.get('type')} | `{db_key}` | {detail_str} |")
                        else:
                            db_key = f.get('dbKey', '-')
                            binding = f.get('entityPath') or f.get('dbKey') or '-'
                            md.append(f"| {f.get('name')} | `{f.get('key')}` | {f.get('type')} | `{db_key}` | `{binding}` |")
                        seen_keys.add(f.get('key'))
            else:
                md.append("> 当前 view 不包含字段类型或无匹配字段")

            if selected_display_buttons:
                md.append("\n### 🔘 操作按钮 (匹配)")
                md.append(", ".join([f"{b.get('name')}(`{b.get('key')}`/{b.get('type')})" for b in selected_display_buttons]))
        else:
            # 概览模式：只输出紧凑的 Key-Name 映射
            md.append(f"### 📑 字段概览 (共 {len(selected_biz_form_fields) + len(selected_biz_entity_fields)} 个非审计字段，排序: {sort_by})")
            # 合并展示
            all_biz = []
            seen = set()
            for f in selected_biz_form_fields + selected_biz_entity_fields:
                if f.get('key') not in seen:
                    all_biz.append(f)
                    seen.add(f.get('key'))

            chunk_size = 3
            for i in range(0, min(len(all_biz), 120), chunk_size):
                chunk = all_biz[i:i+chunk_size]
                line = "  ".join([f"• {f.get('name')}: `{f.get('key')}`" for f in chunk])
                md.append(line)

            if len(all_biz) > 120:
                md.append(f"\n> *提示: 字段较多已截断。本地已缓存全量，请传入关键词（支持正则）获取特定字段详情。*")

            if selected_buttons:
                md.append("\n### 🔘 全部操作按钮")
                md.append(", ".join([f"{b.get('name')}(`{b.get('key')}`/{b.get('type')})" for b in selected_buttons]))

        return "\n".join(md)


def main():
    parser = argparse.ArgumentParser(description="Cosmic Form Metadata CLI (View Filter Mode)")
    parser.add_argument("--config", help="Path to ok-cosmic.json")
    parser.add_argument("--debug", action="store_true")

    sub_parser = parser.add_subparsers(dest="command")
    get_parser = sub_parser.add_parser("get")
    get_parser.add_argument("--form-id")
    get_parser.add_argument("--bill-name")
    get_parser.add_argument("--fuzzy", nargs="*", help="筛选关键词或正则模式，触发详情视图")
    get_parser.add_argument("--show-detail", action="store_true", help="显示枚举值映射(extMap)或基础资料引用类型(refType)")
    get_parser.add_argument("--tree", action="store_true", help="按 parentKey 输出字段树（可与 --fuzzy 联用）")
    get_parser.add_argument("--sort", choices=["key", "name", "type", "dbKey"], default="key", help="字段排序方式")
    get_parser.add_argument("--view", choices=["form", "entity", "operate", "all"], default="all", help="元数据视图范围")
    get_parser.add_argument("--debug", action="store_true")
    get_parser.add_argument("--refresh", action="store_true")

    args = parser.parse_args()
    config = load_project_config(args.config)
    fm = FormMetadata(config, debug=(args.debug or getattr(args, 'debug', False)))

    if args.command == "get":
        if args.refresh and args.form_id:
            fm.cache.remove(args.form_id)
        print(
            fm.get_meta_fields(
                formId=args.form_id,
                billName=args.bill_name,
                filter_patterns=args.fuzzy,
                show_detail=args.show_detail,
                tree_view=args.tree,
                sort_by=args.sort,
                view=args.view
            )
        )
    else:
        parser.print_help()

if __name__ == "__main__":
    main()