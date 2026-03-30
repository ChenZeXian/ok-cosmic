# -*- coding: utf-8 -*-
"""公共数据结构与工具函数，所有检查模块共享。"""

import re
from dataclasses import dataclass, field
from enum import Enum
from typing import List, Tuple


# ──────────────────────────────────────────────
# 数据结构
# ──────────────────────────────────────────────

class Severity(str, Enum):
    ERROR = "ERROR"      # 必定错误
    WARNING = "WARNING"  # 场景错配 / 强烈不推荐
    INFO = "INFO"        # 风格偏好 / 建议


@dataclass
class LintIssue:
    file: str
    line: int
    severity: Severity
    rule_id: str
    message: str
    fix_hint: str = ""
    source_line: str = ""


@dataclass
class LintReport:
    total_files: int = 0
    total_issues: int = 0
    errors: int = 0
    warnings: int = 0
    infos: int = 0
    issues: List[LintIssue] = field(default_factory=list)

    def add(self, issue: LintIssue):
        self.issues.append(issue)
        self.total_issues += 1
        if issue.severity == Severity.ERROR:
            self.errors += 1
        elif issue.severity == Severity.WARNING:
            self.warnings += 1
        else:
            self.infos += 1


# ──────────────────────────────────────────────
# 公共工具函数
# ──────────────────────────────────────────────

def is_comment_or_string(line: str) -> bool:
    """粗略判断当前行是否为注释或字符串（排除误报）"""
    stripped = line.strip()
    return stripped.startswith("//") or stripped.startswith("*") or stripped.startswith("/*")


# 操作插件的类标识模式
OP_PLUGIN_MARKERS = [
    r"extends\s+AbstractOperationServicePlugIn\b",
    r"extends\s+AbstractOperationServicePlugInExt\b",
    r"extends\s+AbstractValidatorExt\b",
    r"implements\s+.*OperationServicePlugIn\b",
]

# UI 插件标识模式
UI_PLUGIN_MARKERS = [
    r"extends\s+AbstractFormPluginExt\b",
    r"extends\s+AbstractBillPlugInExt\b",
    r"extends\s+AbstractListPluginExt\b",
    r"extends\s+AbstractFormPlugin\b",
    r"extends\s+AbstractBillPlugIn\b",
    r"extends\s+AbstractListPlugin\b",
    r"extends\s+AbstractTreeListPlugin\b",
]


def detect_plugin_type(lines: List[str]) -> Tuple[bool, bool]:
    """检测文件是操作插件还是 UI 插件，返回 (is_op, is_ui)"""
    full_text = "\n".join(lines)
    is_op = any(re.search(p, full_text) for p in OP_PLUGIN_MARKERS)
    is_ui = any(re.search(p, full_text) for p in UI_PLUGIN_MARKERS)
    return is_op, is_ui


def detect_listener_interfaces(lines: List[str]) -> List[str]:
    """检测文件实现了哪些 Listener 接口"""
    listeners = []
    for line in lines:
        m = re.search(r"implements\s+(.+?)(?:\s*\{|$)", line)
        if m:
            ifaces = [s.strip() for s in m.group(1).split(",")]
            listeners.extend(i for i in ifaces if "Listener" in i)
    return listeners
