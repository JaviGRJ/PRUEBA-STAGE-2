"""Structure-specific Stage 1 benchmark using the shared corpus and tokenizer."""

from __future__ import annotations

import csv
import json
import shutil
import sys
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parent
sys.path.insert(0, str(ROOT.parent / "python"))
from tokenizer import tokenize_file  # noqa: E402


def metric(root: Path, elapsed_ms: float) -> tuple[int, int, int, float]:
    files = [p for p in root.rglob("*") if p.is_file()] if root.exists() else []
    directories = [p for p in root.rglob("*") if p.is_dir()] if root.exists() else []
    size = sum(p.stat().st_size for p in files)
    return size, len(files), len(directories), elapsed_ms


def read_books(corpus: Path) -> list[dict[str, str]]:
    with (corpus / "manifest.csv").open(newline="", encoding="utf-8") as stream:
        return list(csv.DictReader(stream))


def main() -> None:
    corpus = Path(sys.argv[1]) if len(sys.argv) > 1 else ROOT / "corpus"
    output = Path(sys.argv[2]) if len(sys.argv) > 2 else ROOT / "results-python.csv"
    output.parent.mkdir(parents=True, exist_ok=True)
    books = read_books(corpus)
    rows = [["language", "area", "structure", "operation", "books", "bytes", "files",
             "directories", "time_ms", "terms", "postings", "query", "answer_count", "status"]]

    for layout in ("by_book", "by_time", "by_batch"):
        root = output.parent / f"python-run-{layout}"
        shutil.rmtree(root, ignore_errors=True)
        started = time.perf_counter()
        for book in books:
            folder = {"by_book": root / book["book_id"],
                      "by_time": root / "1970-01-01",
                      "by_batch": root / "batch-1"}[layout]
            folder.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(corpus / book["header_file"], folder / f'{book["book_id"]}.header.txt')
            shutil.copyfile(corpus / book["body_file"], folder / f'{book["book_id"]}.body.txt')
        size, files, directories, elapsed = metric(root, (time.perf_counter() - started) * 1000)
        rows.append(["python", "datalake", layout, "write", len(books), size, files, directories,
                     f"{elapsed:.4f}", "", "", "", "", "PASS"])

    started = time.perf_counter()
    index: dict[str, list[int]] = {}
    for book in books:
        for token in tokenize_file(corpus / book["body_file"]):
            index.setdefault(token, [])
            if int(book["book_id"]) not in index[token]:
                index[token].append(int(book["book_id"]))
    build_ms = (time.perf_counter() - started) * 1000
    postings = sum(len(value) for value in index.values())
    query_count = len(index.get("adventure", []))
    for structure in ("monolithic-json", "hierarchy-json"):
        root = output.parent / f"python-index-{structure}"
        shutil.rmtree(root, ignore_errors=True)
        started = time.perf_counter()
        if structure == "monolithic-json":
            root.mkdir(parents=True)
            (root / "index.json").write_text(json.dumps(index, sort_keys=True), encoding="utf-8")
        else:
            buckets: dict[str, dict[str, list[int]]] = {}
            for term, ids in index.items():
                bucket = term[0] if term[0].isalnum() else "misc"
                buckets.setdefault(bucket, {})[term] = ids
            for bucket, values in buckets.items():
                folder = root / bucket
                folder.mkdir(parents=True, exist_ok=True)
                (folder / "index.json").write_text(json.dumps(values, sort_keys=True), encoding="utf-8")
        persist_ms = (time.perf_counter() - started) * 1000
        size, files, directories, _ = metric(root, 0)
        rows.append(["python", "index", structure, "build", len(books), size, files, directories,
                     f"{build_ms:.4f}", len(index), postings, "adventure", query_count, "PASS"])
        rows.append(["python", "index", structure, "persist", len(books), size, files, directories,
                     f"{persist_ms:.4f}", len(index), postings, "", "", "PASS"])
        started = time.perf_counter()
        if structure == "monolithic-json":
            loaded = json.loads((root / "index.json").read_text(encoding="utf-8"))
        else:
            loaded = {}
            for file in root.rglob("index.json"):
                loaded.update(json.loads(file.read_text(encoding="utf-8")))
        query_count = len(loaded.get("adventure", []))
        query_ms = (time.perf_counter() - started) * 1000
        rows.append(["python", "index", structure, "query", len(books), size, files, directories,
                     f"{query_ms:.4f}", len(loaded), sum(map(len, loaded.values())), "adventure", query_count, "PASS"])
    started = time.perf_counter()
    for token in tokenize_file(corpus / books[0]["body_file"]):
        index.setdefault(token, [])
        if 99 not in index[token]:
            index[token].append(99)
    update_ms = (time.perf_counter() - started) * 1000
    rows.append(["python", "index", "in-memory", "incremental_update", 1, 0, 0, 0,
                 f"{update_ms:.4f}", len(index), sum(map(len, index.values())), "", "", "PASS"])
    output.write_text("\n".join(",".join(map(str, row)) for row in rows) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()
