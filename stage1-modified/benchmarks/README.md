# Reproducible Stage 1 benchmark

## Common workload

`corpus/manifest.csv` is the declared corpus. Every language reads the same
Header/Body files, IDs, query terms, and UTF-8 bytes. The tokenizer contract is:
`lowercase(Locale.ROOT)`/`lowercase invariant` -> replace every character
outside `[a-z0-9 whitespace]` with a space -> split whitespace -> unique terms.
The expected `adventure` answer is books `1,2`; `science` is book `3`.

## Measured operations

| Table file | Area | Structure/layout | Timed region |
|---|---|---|---|
| `results-*.csv` | datalake | `by_book`, `by_time`, `by_batch` | create directories and write all Header/Body files; download excluded |
| `results-*.csv` | index | monolithic JSON, hierarchy JSON | build the in-memory postings or serialize the already-built postings |
| `results-*.csv` | index | in-memory | add one new book to an existing index |

Each row records books, bytes, files, directories, terms, postings, query,
answer count, and status. No row reports a disk throughput rate without byte and
file counts. MongoDB is a separate Stage 1 storage option; it is not silently
substituted by a local JSON benchmark and must be run with `MONGO_URI` when
available.

## Commands and traceability

```powershell
cd stage1-modified
powershell -File benchmarks/run-benchmarks.ps1
powershell -File benchmarks/scale-benchmarks.ps1
```

The script records the current source revision with Git, rebuilds Java/C#/Python,
cleans each output root, and writes raw CSV results. Java uses JDK 21 in this
environment, C# uses .NET 8, and Python uses the installed CPython runtime.
Timing uses `System.nanoTime`, `Stopwatch`, and `time.perf_counter`; reported
values are milliseconds. The timed regions consume their outputs by checking
file counts, serialized bytes, postings, and query answer counts.
`RESULTS.md` numbers the presentation tables and is generated from the raw CSVs.

`scale-benchmarks.ps1` generates deterministic 500- and 1000-book corpora and
stores their separate raw results under `scale-results-500/` and
`scale-results-1000/`. These directories are ignored as generated evidence; copy
the CSVs into the report artifacts when submitting a scale experiment.

The serialized metadata schema is `book_id,title,author,language,year`; the
monolithic index is a deterministic JSON object `term -> sorted book IDs`, and
the hierarchy index stores the same object partitioned by initial-character
bucket. Stage 1 paths are `data/datalake/<layout>`, `data/control/*.txt`,
`metadata.db`, and `data/index/{monolithic,hierarchy}` in the production
pipeline; benchmark paths are isolated below `benchmarks/`.

## Interpretation limits

The corpus is intentionally small and local; it is a correctness/measurement
calibration run, not evidence for 500/1000-book scalability. Warm-up, OS cache,
filesystem, CPU frequency, and background processes affect wall-clock results.
Repeat the script on the declared machine and report median/uncertainty before
making performance claims. A zero-allocation claim is not used: the report
should use the recorded serialized footprint and `InvertedIndex.MemoryFootprint`
scope (map entries and postings estimate only).
