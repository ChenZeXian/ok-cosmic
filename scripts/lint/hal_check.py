# -*- coding: utf-8 -*-
"""幻觉方法名/类名检查 (HAL-*) — 来源: anti-patterns.md"""

import re
from typing import List

from .base import LintIssue, Severity, is_comment_or_string


# ── 幻觉方法名黑名单 ──
HAL_METHOD_RULES = [
    {
        "pattern": r"\.setReadOnly\s*\(",
        "rule_id": "HAL-METHOD-001",
        "message": "苍穹不存在 setReadOnly() 方法",
        "fix_hint": "使用 getView().setEnable(false, \"key\")",
    },
    {
        "pattern": r"\bafterCreateControl\s*\(",
        "rule_id": "HAL-METHOD-002",
        "message": "苍穹不存在 afterCreateControl() 事件方法",
        "fix_hint": "使用 afterBindData() 或 registerListener()",
    },
    {
        "pattern": r"getView\(\)\s*\.\s*refresh\s*\(\s*\)",
        "rule_id": "HAL-METHOD-003",
        "message": "苍穹不存在 getView().refresh() 方法",
        "fix_hint": "使用 getView().updateView(\"key\") 或 getView().updateView()",
    },
    {
        "pattern": r"model\s*\.\s*getEntryCount\s*\(",
        "rule_id": "HAL-METHOD-004",
        "message": "苍穹不存在 getEntryCount()，方法名和参数都不同",
        "fix_hint": "使用 model.getEntryRowCount(\"entryKey\")",
    },
    {
        "pattern": r"model\s*\.\s*deleteRow\s*\(",
        "rule_id": "HAL-METHOD-005",
        "message": "苍穹不存在 deleteRow()，方法名不同",
        "fix_hint": "使用 model.deleteEntryRow(\"entryKey\", rowIndex)",
    },
    {
        "pattern": r"model\s*\.\s*addRow\s*\(",
        "rule_id": "HAL-METHOD-006",
        "message": "苍穹不存在 addRow()，方法名不同",
        "fix_hint": "使用 model.createNewEntryRow(\"entryKey\", rowIndex)",
    },
    {
        "pattern": r"\bdestroy\s*\(\s*\)",
        "rule_id": "HAL-METHOD-007",
        "message": "苍穹方法名是 destory()（少一个 r），不是 destroy()",
        "fix_hint": "使用 destory()（苍穹拼写如此）",
    },
    {
        "pattern": r"IDataModel\s*\.\s*setReadOnly\s*\(",
        "rule_id": "HAL-METHOD-008",
        "message": "IDataModel 不存在 setReadOnly()，模型层不负责 UI 状态",
        "fix_hint": "使用 getView().setEnable(false, \"key\")",
    },
]

# ── 幻觉类名黑名单 ──
HAL_CLASS_PATTERNS = [
    {
        "pattern": r"\b(Cosmic|Cloud)[A-Z]\w*(?:Utils|Helper|Service)\b",
        "rule_id": "HAL-CLASS-001",
        "message": "不存在以 Cosmic/Cloud 开头的工具类，除非脚本明确查到",
        "fix_hint": "使用 cosmic-api-knowledge.py search 确认实际类名",
    },
    {
        "pattern": r"\bBillHelper\b",
        "rule_id": "HAL-CLASS-002",
        "message": "不存在 BillHelper 类",
        "fix_hint": "使用 BusinessDataServiceHelper",
    },
    {
        "pattern": r"\bFormHelper\b",
        "rule_id": "HAL-CLASS-003",
        "message": "不存在 FormHelper 类",
        "fix_hint": "使用 FormUtils（位于 kd.cd.common.form）",
    },
    {
        "pattern": r"\bListHelper\b",
        "rule_id": "HAL-CLASS-004",
        "message": "不存在 ListHelper 类",
        "fix_hint": "使用 BaseDataServiceHelper 或 QueryServiceHelper",
    },
    {
        "pattern": r"\bPluginHelper\b",
        "rule_id": "HAL-CLASS-005",
        "message": "不存在 PluginHelper 类",
        "fix_hint": "按具体场景使用对应的 ServiceHelper",
    },
]


def check(filepath: str, lines: List[str]) -> List[LintIssue]:
    """执行幻觉方法名/类名检查，返回问题列表。"""
    issues: List[LintIssue] = []

    for i, line in enumerate(lines):
        if is_comment_or_string(line):
            continue

        lineno = i + 1

        # 幻觉方法名
        for rule in HAL_METHOD_RULES:
            if re.search(rule["pattern"], line):
                issues.append(LintIssue(
                    file=filepath, line=lineno,
                    severity=Severity.ERROR,
                    rule_id=rule["rule_id"],
                    message=rule["message"],
                    fix_hint=rule["fix_hint"],
                    source_line=line.strip(),
                ))

        # 幻觉类名
        for rule in HAL_CLASS_PATTERNS:
            if re.search(rule["pattern"], line):
                issues.append(LintIssue(
                    file=filepath, line=lineno,
                    severity=Severity.ERROR,
                    rule_id=rule["rule_id"],
                    message=rule["message"],
                    fix_hint=rule["fix_hint"],
                    source_line=line.strip(),
                ))

    return issues
