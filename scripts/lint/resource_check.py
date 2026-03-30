# -*- coding: utf-8 -*-
"""资源管理检查 (RESOURCE-*) — 来源: anti-patterns.md"""

import re
from typing import List

from .base import LintIssue, Severity


# 资源持有规则列表
RESOURCE_RULES = [
    {
        "pattern": r"(private|protected|public)\s+(DataSet|InputStream|OutputStream|Connection|ResultSet)\s+\w+\s*;",
        "rule_id": "RESOURCE-001",
        "severity": Severity.WARNING,
        "message": "插件成员变量不应持有 DataSet/InputStream 等资源对象，有序列化问题",
        "fix_hint": "在方法内使用后立即关闭，不持有引用",
    },
    {
        "pattern": r"static\s+(?!final\b)\w+\s+\w+\s*=",
        "rule_id": "RESOURCE-002",
        "severity": Severity.WARNING,
        "message": "不应使用非 final 的 static 变量存储状态，多实例会冲突",
        "fix_hint": "使用 PageCache 或实例变量",
        "exclude_pattern": r"(static\s+final|static\s+\w+\s+[A-Z_]+\s*=|LogFactory|getLogger|Log\s+log)",
    },
]


def check(filepath: str, lines: List[str]) -> List[LintIssue]:
    """执行资源管理检查，返回问题列表。"""
    issues: List[LintIssue] = []

    for i, line in enumerate(lines):
        lineno = i + 1
        for rule in RESOURCE_RULES:
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
