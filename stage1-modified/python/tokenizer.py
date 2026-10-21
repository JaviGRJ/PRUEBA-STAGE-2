"""Tokenizer and metrics with the same rules as the Java Stage 1 tokenizer."""

from __future__ import annotations

import re
import time
from pathlib import Path

TOKEN_PATTERN = re.compile(r"[^a-z0-9\s]+")


def tokenize(text: str) -> set[str]:
    normalized = TOKEN_PATTERN.sub(" ", text.lower())
    return {token for token in re.split(r"\s+", normalized) if token}


def tokenize_file(path: str | Path) -> set[str]:
    return tokenize(Path(path).read_text(encoding="utf-8"))


def measure_storage(path: str | Path, operation) -> dict[str, float | int]:
    """Measure a storage operation with a system clock and report bytes/ms."""
    started = time.perf_counter()
    operation()
    elapsed_ms = (time.perf_counter() - started) * 1000.0
    file_path = Path(path)
    size_bytes = file_path.stat().st_size if file_path.is_file() else sum(
        item.stat().st_size for item in file_path.rglob("*") if item.is_file()
    )
    return {"size_bytes": size_bytes, "size_kb": size_bytes / 1024.0, "time_ms": elapsed_ms}
