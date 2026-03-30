#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
cosmic-post-lint.py — 苍穹代码生成后自动校验脚本

依次调用 lint/ 子模块执行各类检查，汇总输出报告。

用法:
    python3 cosmic-post-lint.py <file_or_directory> [--fix-hint] [--json] [--strict]

示例:
    python3 cosmic-post-lint.py ./src/main/java/
    python3 cosmic-post-lint.py MyPlugin.java --fix-hint
    python3 cosmic-post-lint.py ./src/ --json --strict
"""

import argparse
import json
import os
import sys
from pathlib import Path
from typing import List

from lint.base import Severity, LintIssue, LintReport
from lint import hal_check, scene_check, style_check, resource_check, verify_check


# ──────────────────────────────────────────────
# 检查器注册表
# ──────────────────────────────────────────────

# 每个检查器必须暴露 check(filepath, lines) -> List[LintIssue]
# 新增检查器只需:  1) 在 lint/ 下新建 xxx_check.py  2) 在此列表注册
CHECKERS = [
    {"name": "幻觉检查",   "module": hal_check,      "always": True},
    {"name": "场景错配",   "module": scene_check,    "always": True},
    {"name": "编码偏好",   "module": style_check,    "always": True},
    {"name": "资源管理",   "module": resource_check,  "always": True},
    {"name": "验证注释",   "module": verify_check,    "always": False},  # 仅 --strict 模式
]


# ──────────────────────────────────────────────
# 文件扫描
# ──────────────────────────────────────────────

def collect_java_files(path: str) -> List[str]:
    """递归收集所有 .java 文件"""
    p = Path(path)
    if p.is_file() and p.suffix == ".java":
        return [str(p)]
    elif p.is_dir():
        return sorted(str(f) for f in p.rglob("*.java"))
    else:
        return []


def read_file_lines(filepath: str) -> List[str]:
    """读取文件内容，返回行列表"""
    try:
        with open(filepath, "r", encoding="utf-8") as f:
            return [l.rstrip("\n") for l in f.readlines()]
    except (UnicodeDecodeError, IOError):
        return []


# ──────────────────────────────────────────────
# 核心编排
# ──────────────────────────────────────────────

def check_file(filepath: str, strict: bool = False) -> List[LintIssue]:
    """对单个 Java 文件依次调用所有检查器"""
    lines = read_file_lines(filepath)
    if not lines:
        return []

    issues: List[LintIssue] = []
    for checker in CHECKERS:
        if not checker["always"] and not strict:
            continue
        issues.extend(checker["module"].check(filepath, lines))

    return issues


# ──────────────────────────────────────────────
# 报告输出
# ──────────────────────────────────────────────

RED = "\033[91m"
YELLOW = "\033[93m"
CYAN = "\033[96m"
GREEN = "\033[92m"
GRAY = "\033[90m"
BOLD = "\033[1m"
RESET = "\033[0m"

SEVERITY_COLORS = {
    Severity.ERROR: RED,
    Severity.WARNING: YELLOW,
    Severity.INFO: CYAN,
}

SEVERITY_ICONS = {
    Severity.ERROR: "❌",
    Severity.WARNING: "⚠️ ",
    Severity.INFO: "💡",
}


def print_report(report: LintReport, show_fix_hint: bool = False):
    """终端友好格式输出"""
    if not report.issues:
        print(f"\n{GREEN}{BOLD}✅ 检查通过！共扫描 {report.total_files} 个文件，未发现问题。{RESET}\n")
        return

    by_file: dict[str, List[LintIssue]] = {}
    for issue in report.issues:
        by_file.setdefault(issue.file, []).append(issue)

    print(f"\n{BOLD}{'═' * 60}{RESET}")
    print(f"{BOLD}  苍穹代码校验报告{RESET}")
    print(f"{BOLD}{'═' * 60}{RESET}\n")

    for filepath, file_issues in by_file.items():
        rel = os.path.relpath(filepath)
        print(f"  {BOLD}📄 {rel}{RESET}")
        for issue in sorted(file_issues, key=lambda x: x.line):
            color = SEVERITY_COLORS[issue.severity]
            icon = SEVERITY_ICONS[issue.severity]
            print(f"    {color}{icon} L{issue.line:>4d} [{issue.rule_id}]{RESET} {issue.message}")
            if issue.source_line:
                print(f"         {GRAY}> {issue.source_line}{RESET}")
            if show_fix_hint and issue.fix_hint:
                print(f"         {GREEN}💊 修复: {issue.fix_hint}{RESET}")
        print()

    print(f"  {BOLD}{'─' * 50}{RESET}")
    print(f"  📊 扫描文件: {report.total_files}   "
          f"总问题: {report.total_issues}   "
          f"{RED}ERROR: {report.errors}{RESET}   "
          f"{YELLOW}WARNING: {report.warnings}{RESET}   "
          f"{CYAN}INFO: {report.infos}{RESET}")

    if report.errors > 0:
        print(f"\n  {RED}{BOLD}🚫 发现 {report.errors} 个错误，请修复后再提交。{RESET}\n")
    elif report.warnings > 0:
        print(f"\n  {YELLOW}{BOLD}⚠️  发现 {report.warnings} 个警告，建议修复。{RESET}\n")
    else:
        print(f"\n  {GREEN}{BOLD}✅ 仅有建议项，整体良好。{RESET}\n")


def print_json_report(report: LintReport):
    """JSON 格式输出（适合 CI/CD 集成）"""
    output = {
        "summary": {
            "total_files": report.total_files,
            "total_issues": report.total_issues,
            "errors": report.errors,
            "warnings": report.warnings,
            "infos": report.infos,
            "passed": report.errors == 0,
        },
        "issues": [
            {
                "file": os.path.relpath(i.file),
                "line": i.line,
                "severity": i.severity.value,
                "rule_id": i.rule_id,
                "message": i.message,
                "fix_hint": i.fix_hint,
                "source_line": i.source_line,
            }
            for i in report.issues
        ],
    }
    print(json.dumps(output, ensure_ascii=False, indent=2))


# ──────────────────────────────────────────────
# 入口
# ──────────────────────────────────────────────

def main():
    parser = argparse.ArgumentParser(
        description="苍穹代码生成后自动校验脚本",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
检查模块:
  lint/hal_check.py       幻觉检查 (HAL-*)
  lint/scene_check.py     场景错配 (SCENE-*)
  lint/style_check.py     编码偏好 (STYLE-*)
  lint/resource_check.py  资源管理 (RESOURCE-*)
  lint/verify_check.py    验证注释 (VERIFY-*, 仅 --strict)

扩展方式:
  1. 在 lint/ 下新建 xxx_check.py，暴露 check(filepath, lines) 函数
  2. 在 cosmic-post-lint.py 的 CHECKERS 列表中注册
        """,
    )
    parser.add_argument("path", help="要检查的 Java 文件或目录路径")
    parser.add_argument("--fix-hint", action="store_true", help="显示修复建议")
    parser.add_argument("--json", action="store_true", help="输出 JSON 格式（适合 CI）")
    parser.add_argument("--strict", action="store_true",
                        help="严格模式：额外检查 @Override 验证来源注释")
    parser.add_argument("--min-severity", choices=["error", "warning", "info"],
                        default="info", help="最低报告级别 (默认: info)")

    args = parser.parse_args()

    severity_filter = {
        "error": {Severity.ERROR},
        "warning": {Severity.ERROR, Severity.WARNING},
        "info": {Severity.ERROR, Severity.WARNING, Severity.INFO},
    }[args.min_severity]

    files = collect_java_files(args.path)
    if not files:
        print(f"❌ 未找到 Java 文件: {args.path}", file=sys.stderr)
        sys.exit(1)

    report = LintReport(total_files=len(files))

    for filepath in files:
        issues = check_file(filepath, strict=args.strict)
        for issue in issues:
            if issue.severity in severity_filter:
                report.add(issue)

    if args.json:
        print_json_report(report)
    else:
        print_report(report, show_fix_hint=args.fix_hint)

    sys.exit(1 if report.errors > 0 else 0)


if __name__ == "__main__":
    main()
