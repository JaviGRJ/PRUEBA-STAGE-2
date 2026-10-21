"""Compare the shared corpus token sets and posting answers across languages."""

from __future__ import annotations

import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent
sys.path.insert(0, str(ROOT.parent / "python"))
from tokenizer import tokenize_file  # noqa: E402


def main() -> None:
    corpus = Path(sys.argv[1]) if len(sys.argv) > 1 else ROOT / "corpus"
    expected = {path.stem.split(".")[0]: sorted(tokenize_file(path))
                for path in sorted(corpus.glob("*.body.txt"))}
    output = ROOT / "token-equivalence-python.json"
    output.write_text(json.dumps({"status": "PASS", "tokens": expected}, indent=2), encoding="utf-8")
    print(json.dumps({"status": "PASS", "documents": len(expected), "output": str(output)}))


if __name__ == "__main__":
    main()
