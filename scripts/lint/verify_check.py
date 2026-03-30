# -*- coding: utf-8 -*-
"""验证来源注释检查 (VERIFY-*) — 来源: constraints.md（仅严格模式生效）"""

import re
from typing import List

from .base import LintIssue, Severity


OVERRIDE_PATTERN = re.compile(r"@Override")
VERIFY_PATTERN = re.compile(r"//\s*验证来源:")


def check(filepath: str, lines: List[str]) -> List[LintIssue]:
    """执行 @Override 验证来源注释检查，返回问题列表。"""
    issues: List[LintIssue] = []

    for i, line in enumerate(lines):
        if OVERRIDE_PATTERN.search(line):
            # 检查上方 3 行内是否有验证来源注释
            has_verify = False
            for j in range(max(0, i - 3), i):
                if VERIFY_PATTERN.search(lines[j]):
                    has_verify = True
                    break
            if not has_verify:
                issues.append(LintIssue(
                    file=filepath, line=i + 1,
                    severity=Severity.INFO,
                    rule_id="VERIFY-001",
                    message="@Override 方法缺少验证来源注释",
                    fix_hint="在 @Override 上方添加: // 验证来源: scripts/cosmic-api-knowledge.py detail [ClassName]",
                    source_line=line.strip(),
                ))

    return issues
