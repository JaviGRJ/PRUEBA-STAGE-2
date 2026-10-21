"""Generate a deterministic N-book corpus from the shared benchmark vocabulary."""

from __future__ import annotations

import csv
import sys
from pathlib import Path


def main() -> None:
    count = int(sys.argv[1])
    destination = Path(sys.argv[2])
    destination.mkdir(parents=True, exist_ok=True)
    rows = []
    for book_id in range(1, count + 1):
        query = "adventure" if book_id % 2 else "science"
        (destination / f"{book_id}.header.txt").write_text(
            f"Title: Synthetic Book {book_id}\nAuthor: Synthetic Author\nLanguage: en\nRelease Date: 2000\n",
            encoding="utf-8")
        (destination / f"{book_id}.body.txt").write_text(
            f"{query} common corpus document number {book_id} with stable tokens.\n",
            encoding="utf-8")
        rows.append([book_id, f"{book_id}.header.txt", f"{book_id}.body.txt", query])
    with (destination / "manifest.csv").open("w", newline="", encoding="utf-8") as stream:
        writer = csv.writer(stream)
        writer.writerow(["book_id", "header_file", "body_file", "expected_query"])
        writer.writerows(rows)


if __name__ == "__main__":
    main()
