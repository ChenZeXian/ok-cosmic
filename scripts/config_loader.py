#!/usr/bin/env python3
# SPDX-License-Identifier: NOASSERTION
"""
config_loader.py — Shared configuration loader for ok-cosmic scripts.

Provides a unified `load_project_config()` that both cosmic-api-knowledge.py
and cosmic-form-metadata.py use to locate and parse ok-cosmic.json.

Search priority:
1. Explicit config_path argument (typically from --config CLI flag)
2. Environment variables: COSMIC_GRAPH_CONFIG / COSMIC_META_CONFIG
3. Walk up from cwd checking common relative paths at each level
"""

import json
import os
import sys
from pathlib import Path
from typing import Any, Dict, List, Optional

# Windows console encoding fix (shared by all scripts)
if os.name == "nt":
    try:
        sys.stdout.reconfigure(encoding="utf-8")
        sys.stderr.reconfigure(encoding="utf-8")
    except Exception:
        pass


def load_project_config(config_path: Optional[str] = None) -> Dict[str, Any]:
    """
    Load project-level config from provided path, environment, or common project paths.

    Search priority:
    1. Explicit ``config_path`` (from --config flag)
    2. Environment variables ``COSMIC_GRAPH_CONFIG`` / ``COSMIC_META_CONFIG``
    3. Walk up from cwd: ok-cosmic.json / .ok-cosmic/ok-cosmic.json / references/ok-cosmic.json

    Returns a dict with two injected keys for downstream consumers:
    - ``__config_path__``: absolute path to the resolved config file
    - ``__config_dir__``:  absolute path to the parent directory of the config file
    """
    candidates: List[Path] = []

    # 1. Explicit path
    if config_path:
        candidates.append(Path(config_path).expanduser())

    # 2. Environment variables (both names for backward compat)
    for env_key in ("COSMIC_GRAPH_CONFIG", "COSMIC_META_CONFIG"):
        env_val = os.getenv(env_key, "").strip()
        if env_val:
            p = Path(env_val).expanduser()
            if p not in candidates:
                candidates.append(p)

    # 3. Walk up from cwd
    start_dir = Path.cwd().resolve()
    rel_paths = (
        "ok-cosmic.json",
        ".ok-cosmic/ok-cosmic.json",
        "references/ok-cosmic.json",
    )
    for directory in (start_dir, *start_dir.parents):
        for rel in rel_paths:
            p = directory / rel
            if p not in candidates:
                candidates.append(p)

    # Try each candidate in order
    for path in candidates:
        if path.is_file():
            try:
                with path.open("r", encoding="utf-8") as f:
                    data = json.load(f)
                if not isinstance(data, dict):
                    continue
                resolved = path.resolve()
                data.setdefault("__config_path__", str(resolved))
                data.setdefault("__config_dir__", str(resolved.parent))
                return data
            except Exception:
                continue

    return {}
