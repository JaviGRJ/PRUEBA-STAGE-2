"""Render numbered Markdown result tables from the raw CSV files."""

from __future__ import annotations

import csv
import sys
from pathlib import Path


def read(path: Path) -> list[dict[str, str]]:
    with path.open(encoding="utf-8") as stream:
        return list(csv.DictReader(stream))


def main() -> None:
    root = Path(sys.argv[1]) if len(sys.argv) > 1 else Path(__file__).parent
    all_rows = [row for path in sorted(root.glob("results-*.csv")) for row in read(path)]
    lines = [
        "# Validated Stage 1 results",
        "",
        "All tables are generated from the raw CSV files in this directory. Times are wall-clock milliseconds; bytes/files/directories are recorded for the same workload.",
        "",
        "## Table 1. Datalake writes by layout and language",
        "",
        "| Language | Layout | Books | Bytes | Files | Directories | Time (ms) |",
        "|---|---|---:|---:|---:|---:|---:|",
    ]
    for row in all_rows:
        if row["area"] == "datalake":
            lines.append(f'| {row["language"]} | {row["structure"]} | {row["books"]} | {row["bytes"]} | {row["files"]} | {row["directories"]} | {row["time_ms"]} |')
    lines += [
        "",
        "Workload: three declared Header/Body pairs, directory creation and local writes included; Gutenberg download excluded.",
        "",
        "## Table 2. Index construction, persistence, and query",
        "",
        "| Language | Structure | Operation | Books | Bytes | Files | Terms | Postings | Query | Answers | Time (ms) |",
        "|---|---|---|---:|---:|---:|---:|---:|---|---:|---:|",
    ]
    for row in all_rows:
        if row["area"] == "index":
            lines.append(f'| {row["language"]} | {row["structure"]} | {row["operation"]} | {row["books"]} | {row["bytes"]} | {row["files"]} | {row["terms"]} | {row["postings"]} | {row["query"]} | {row["answer_count"]} | {row["time_ms"]} |')
    lines += [
        "",
        "Workload: same in-memory postings and the same `adventure` query; query rows read the serialized representation for Python/C# and the equivalent in-memory representation for Java.",
        "",
        "These three-book results validate correctness and measurement boundaries only. They do not support 500/1000-book scalability claims.",
    ]
    (root / "RESULTS.md").write_text("\n".join(lines) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()
