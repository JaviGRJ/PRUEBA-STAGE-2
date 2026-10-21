"""Validate that all reported languages completed the same useful workload."""

from __future__ import annotations

import csv
import sys
from pathlib import Path


def main() -> None:
    paths = [Path(value) for value in sys.argv[1:]]
    if len(paths) != 3:
        raise SystemExit("expected Java, Python and C# CSV paths")
    rows = [list(csv.DictReader(path.open(encoding="utf-8"))) for path in paths]
    languages = {row[0]["language"] for row in rows}
    if languages != {"java", "python", "csharp"}:
        raise AssertionError(f"unexpected language rows: {languages}")
    for language_rows in rows:
        for row in language_rows:
            if row["status"] != "PASS":
                raise AssertionError(f"failed row: {row}")
            if float(row["time_ms"]) <= 0:
                raise AssertionError(f"non-positive timing: {row}")
    datalake = {
        (row["structure"], row["operation"], row["books"], row["bytes"], row["files"], row["directories"])
        for row in rows[0] if row["area"] == "datalake"
    }
    for language_rows in rows[1:]:
        actual = {
            (row["structure"], row["operation"], row["books"], row["bytes"], row["files"], row["directories"])
            for row in language_rows if row["area"] == "datalake"
        }
        if actual != datalake:
            raise AssertionError("datalake layout outputs differ across languages")
    answers = {
        (row["operation"], row["query"], row["answer_count"], row["terms"], row["postings"])
        for row in rows[0] if row["area"] == "index" and row["query"]
    }
    for language_rows in rows[1:]:
        actual = {
            (row["operation"], row["query"], row["answer_count"], row["terms"], row["postings"])
            for row in language_rows if row["area"] == "index" and row["query"]
        }
        if actual != answers:
            raise AssertionError("index terms/postings/query answers differ across languages")
    print("Cross-language structure and answer validation passed")


if __name__ == "__main__":
    main()
