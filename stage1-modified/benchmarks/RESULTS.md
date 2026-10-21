# Validated Stage 1 results

All tables are generated from the raw CSV files in this directory. Times are wall-clock milliseconds; bytes/files/directories are recorded for the same workload.

## Table 1. Datalake writes by layout and language

| Language | Layout | Books | Bytes | Files | Directories | Time (ms) |
|---|---|---:|---:|---:|---:|---:|
| csharp | by_book | 3 | 444 | 6 | 3 | 8.3259 |
| csharp | by_time | 3 | 444 | 6 | 1 | 6.2341 |
| csharp | by_batch | 3 | 444 | 6 | 1 | 5.2538 |
| java | by_book | 3 | 444 | 6 | 3 | 22.4393 |
| java | by_time | 3 | 444 | 6 | 1 | 9.2394 |
| java | by_batch | 3 | 444 | 6 | 1 | 8.5294 |
| python | by_book | 3 | 444 | 6 | 3 | 5.1669 |
| python | by_time | 3 | 444 | 6 | 1 | 5.0598 |
| python | by_batch | 3 | 444 | 6 | 1 | 4.9025 |

Workload: three declared Header/Body pairs, directory creation and local writes included; Gutenberg download excluded.

## Table 2. Index construction, persistence, and query

| Language | Structure | Operation | Books | Bytes | Files | Terms | Postings | Query | Answers | Time (ms) |
|---|---|---|---:|---:|---:|---:|---:|---|---:|---:|
| csharp | monolithic-json | build | 3 | 288 | 1 | 21 | 28 | adventure | 2 | 26.6585 |
| csharp | monolithic-json | persist | 3 | 288 | 1 | 21 | 28 |  |  | 25.4894 |
| csharp | monolithic-json | query | 3 | 288 | 1 | 21 | 28 | adventure | 2 | 14.2272 |
| csharp | hierarchy-json | build | 3 | 301 | 14 | 21 | 28 | adventure | 2 | 26.6585 |
| csharp | hierarchy-json | persist | 3 | 301 | 14 | 21 | 28 |  |  | 36.4051 |
| csharp | hierarchy-json | query | 3 | 301 | 14 | 21 | 28 | adventure | 2 | 4.4842 |
| csharp | in-memory | incremental_update | 1 | 0 | 0 | 21 | 36 |  |  | 0.1333 |
| java | JsonFileIndexStorage | build | 3 | 381 | 1 | 21 | 28 | adventure | 2 | 3.0105 |
| java | JsonFileIndexStorage | persist | 3 | 381 | 1 | 21 | 28 |  |  | 13.9349 |
| java | JsonFileIndexStorage | query | 3 | 381 | 1 | 21 | 28 | adventure | 2 | 0.0011 |
| java | FolderHierarchyIndexStorage | build | 3 | 420 | 14 | 21 | 28 | adventure | 2 | 3.0105 |
| java | FolderHierarchyIndexStorage | persist | 3 | 420 | 14 | 21 | 28 |  |  | 14.6827 |
| java | FolderHierarchyIndexStorage | query | 3 | 420 | 14 | 21 | 28 | adventure | 2 | 0.0010 |
| java | in-memory | incremental_update | 1 | 0 | 0 | 21 | 36 |  |  | 0.4289 |
| python | monolithic-json | build | 3 | 336 | 1 | 21 | 28 | adventure | 2 | 0.5218 |
| python | monolithic-json | persist | 3 | 336 | 1 | 21 | 28 |  |  | 0.6347 |
| python | monolithic-json | query | 3 | 336 | 1 | 21 | 28 | adventure | 2 | 0.8683 |
| python | hierarchy-json | build | 3 | 336 | 14 | 21 | 28 | adventure | 2 | 0.5218 |
| python | hierarchy-json | persist | 3 | 336 | 14 | 21 | 28 |  |  | 9.5622 |
| python | hierarchy-json | query | 3 | 336 | 14 | 21 | 28 | adventure | 2 | 14.3214 |
| python | in-memory | incremental_update | 1 | 0 | 0 | 21 | 36 |  |  | 0.1649 |

Workload: same in-memory postings and the same `adventure` query; query rows read the serialized representation for Python/C# and the equivalent in-memory representation for Java.

These three-book results validate correctness and measurement boundaries only. They do not support 500/1000-book scalability claims.
