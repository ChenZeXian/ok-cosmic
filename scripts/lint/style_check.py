# -*- coding: utf-8 -*-
"""编码偏好检查 (STYLE-*) — 来源: coding-preferences.md"""

import re
from typing import List

from .base import LintIssue, Severity, is_comment_or_string


# 编码偏好规则列表
STYLE_RULES = [
    {
        "pattern": r"\bStringUtils\s*\.\s*(isBlank|isNotBlank|isEmpty|isNotEmpty|equals)\b",
        "rule_id": "STYLE-001",
        "severity": Severity.INFO,
        "message": "字符串判空应使用 CharSequenceUtils 而非 StringUtils",
        "fix_hint": "使用 CharSequenceUtils.isBlank() / isNotBlank()",
    },
    {
        "pattern": r"!=\s*null\s*&&\s*!\w+\.isEmpty\(\)",
        "rule_id": "STYLE-002",
        "severity": Severity.INFO,
        "message": "集合判空应使用 CollectionUtils 封装",
        "fix_hint": "使用 CollectionUtils.isNotEmpty(collection)",
    },
    {
        "pattern": r"\bOperationServiceHelper\s*\.\s*(save|submit|audit)\b",
        "rule_id": "STYLE-003",
        "severity": Severity.WARNING,
        "message": "不应散落调用 OperationServiceHelper，缺少错误聚合",
        "fix_hint": "使用 OpUtils.executeOperateOrThrow() 或 OperateChain",
    },
    {
        "pattern": r"new\s+PushArgs\s*\(",
        "rule_id": "STYLE-004",
        "severity": Severity.WARNING,
        "message": "不应手拼 PushArgs，有封装可用",
        "fix_hint": "使用 BotpUtils 封装下推逻辑",
    },
    {
        "pattern": r"new\s+DrawArgs\s*\(",
        "rule_id": "STYLE-005",
        "severity": Severity.WARNING,
        "message": "不应手拼 DrawArgs，有封装可用",
        "fix_hint": "使用 BotpUtils 封装选单逻辑",
    },
    {
        "pattern": r"dynamicObject\s*\.\s*get\(\s*\"[\w.]+\.[\w.]+",
        "rule_id": "STYLE-006",
        "severity": Severity.WARNING,
        "message": "不应直接深链式 dynamicObject.get(\"a.b.c\")，不安全",
        "fix_hint": "使用 DynamicObjectUtils.safeGet(dynamicObject, \"field\")",
    },
    {
        "pattern": r"\bAttachmentServiceHelper\s*\.",
        "rule_id": "STYLE-007",
        "severity": Severity.INFO,
        "message": "附件处理应优先使用 AttachmentUtils 封装",
        "fix_hint": "使用 AttachmentUtils 和 uploader",
    },
]


def check(filepath: str, lines: List[str]) -> List[LintIssue]:
    """执行编码偏好检查，返回问题列表。"""
    issues: List[LintIssue] = []

    for i, line in enumerate(lines):
        if is_comment_or_string(line):
            continue

        lineno = i + 1
        for rule in STYLE_RULES:
            exclude = rule.get("exclude_pattern")
            if exclude and re.search(exclude, line):
                continue
            if re.search(rule["pattern"], line):
                issues.append(LintIssue(
                    file=filepath, line=lineno,
                    severity=rule["severity"],
                    rule_id=rule["rule_id"],
                    message=rule["message"],
                    fix_hint=rule["fix_hint"],
                    source_line=line.strip(),
                ))

    return issues
