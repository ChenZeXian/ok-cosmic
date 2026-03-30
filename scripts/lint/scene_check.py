# -*- coding: utf-8 -*-
"""场景错配检查 (SCENE-*) — 来源: anti-patterns.md 场景错配表"""

import re
from typing import List

from .base import LintIssue, Severity, is_comment_or_string, detect_plugin_type, detect_listener_interfaces


def check(filepath: str, lines: List[str]) -> List[LintIssue]:
    """执行场景错配检查，返回问题列表。"""
    issues: List[LintIssue] = []
    is_op, is_ui = detect_plugin_type(lines)
    listeners = detect_listener_interfaces(lines)

    # ── 逐行检查 ──
    for i, line in enumerate(lines):
        lineno = i + 1

        # SCENE-001: 操作插件不应调 getView()
        if is_op and not is_comment_or_string(line):
            if re.search(r"this\s*\.\s*getView\s*\(\s*\)", line) or re.search(r"getView\(\)", line):
                if not re.search(r"(log\.|//|\".*getView)", line):
                    issues.append(LintIssue(
                        file=filepath, line=lineno,
                        severity=Severity.WARNING,
                        rule_id="SCENE-001",
                        message="操作插件中调用 getView()：操作插件无 UI 上下文",
                        fix_hint="使用 log 记录日志或 OpUtils.addErrorMessage() 报告错误",
                        source_line=line.strip(),
                    ))

        # SCENE-002: 操作插件不应通过 model 操作
        if is_op and not is_comment_or_string(line):
            if re.search(r"this\s*\.\s*getModel\s*\(\s*\)\s*\.\s*setValue", line):
                issues.append(LintIssue(
                    file=filepath, line=lineno,
                    severity=Severity.WARNING,
                    rule_id="SCENE-002",
                    message="操作插件中调用 getModel().setValue()：操作插件不通过 model 操作",
                    fix_hint="直接操作 DynamicObject 数据包: dataEntity.set(\"field\", value)",
                    source_line=line.strip(),
                ))

        # SCENE-003: registerListener 中读数据
        if re.search(r"(getModel|getValue)\s*\(", line) and not is_comment_or_string(line):
            for j in range(max(0, i - 20), i):
                if re.search(r"void\s+registerListener\s*\(", lines[j]):
                    issues.append(LintIssue(
                        file=filepath, line=lineno,
                        severity=Severity.WARNING,
                        rule_id="SCENE-003",
                        message="在 registerListener 中调用 getValue()：此时数据尚未绑定",
                        fix_hint="推迟到 afterBindData() 中处理",
                        source_line=line.strip(),
                    ))
                    break

    # ── 文件级检查: Listener 注册 ──
    if listeners:
        has_register = any(
            re.search(r"void\s+registerListener\s*\(", l) for l in lines
        )
        if not has_register:
            # SCENE-004: 实现了 Listener 但没有 registerListener 方法
            issues.append(LintIssue(
                file=filepath, line=1,
                severity=Severity.WARNING,
                rule_id="SCENE-004",
                message=f"实现了 Listener 接口 ({', '.join(listeners)}) 但未找到 registerListener 方法",
                fix_hint="在 registerListener() 中调用 addXxxListener() 注册监听",
            ))
        else:
            # SCENE-005: 有 registerListener 但没有 addXxxListener 调用
            in_register = False
            has_add_listener = False
            for line in lines:
                if re.search(r"void\s+registerListener\s*\(", line):
                    in_register = True
                if in_register and re.search(r"add\w*Listener\s*\(", line):
                    has_add_listener = True
                if in_register and line.strip() == "}":
                    in_register = False
            if not has_add_listener:
                issues.append(LintIssue(
                    file=filepath, line=1,
                    severity=Severity.WARNING,
                    rule_id="SCENE-005",
                    message=f"实现了 Listener 接口 ({', '.join(listeners)}) 但 registerListener 中未调用 addXxxListener()",
                    fix_hint="在 registerListener() 中调用对应的 addXxxListener(this) 注册",
                ))

    return issues
