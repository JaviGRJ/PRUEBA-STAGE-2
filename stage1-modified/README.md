# Stage 1 modified

`src/` contains the original Java data layer, with the tokenizer using the shared
`lowercase -> replace non-[a-z0-9] -> split whitespace` contract. The equivalent
implementations are in `python/tokenizer.py` and `csharp/Tokenizer.cs`.

All measurements use `System.nanoTime()`/`Stopwatch`/`time.perf_counter()` and
report real elapsed milliseconds plus bytes and KB. `DataLakeMetrics` can measure
the `by_book`, `by_time`, and `by_batch` roots without making claims about
sub-picosecond work.

`DataLakeLayouts.write(...)` creates Header/Body files under any of the three
layouts and returns `sizeBytes`, `sizeKb`, and `timeMs`, so the report can
compare the same input and operation across strategies.
