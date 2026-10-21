# Validated Stage 1 results

All tables are generated from the raw CSV files in this directory. Times are wall-clock milliseconds; bytes/files/directories are recorded for the same workload.

## Table 1. Datalake writes by layout and language

| Language | Layout | Books | Bytes | Files | Directories | Time (ms) |
|---|---|---:|---:|---:|---:|---:|
| csharp | by_book | 3 | 444 | 6 | 3 | 7.0109 |
| csharp | by_time | 3 | 444 | 6 | 1 | 4.8706 |
| csharp | by_batch | 3 | 444 | 6 | 1 | 4.5917 |
| java | by_book | 3 | 444 | 6 | 3 | 30.2461 |
| java | by_time | 3 | 444 | 6 | 1 | 13.2173 |
| java | by_batch | 3 | 444 | 6 | 1 | 11.3890 |
| python | by_book | 3 | 444 | 6 | 3 | 5.9087 |
| python | by_time | 3 | 444 | 6 | 1 | 5.8520 |
| python | by_batch | 3 | 444 | 6 | 1 | 6.1587 |

Workload: three declared Header/Body pairs, directory creation and local writes included; Gutenberg download excluded.

## Table 2. Index construction, persistence, and query

| Language | Structure | Operation | Books | Bytes | Files | Terms | Postings | Query | Answers | Time (ms) |
|---|---|---|---:|---:|---:|---:|---:|---|---:|---:|
| csharp | monolithic-json | build | 3 | 288 | 1 | 21 | 28 | adventure | 2 | 18.3216 |
| csharp | monolithic-json | persist | 3 | 288 | 1 | 21 | 28 |  |  | 17.6621 |
| csharp | monolithic-json | query | 3 | 288 | 1 | 21 | 28 | adventure | 2 | 3.4353 |
| csharp | hierarchy-json | build | 3 | 301 | 14 | 21 | 28 | adventure | 2 | 18.3216 |
| csharp | hierarchy-json | persist | 3 | 301 | 14 | 21 | 28 |  |  | 14.1129 |
| csharp | hierarchy-json | query | 3 | 301 | 14 | 21 | 28 | adventure | 2 | 3.7468 |
| csharp | in-memory | incremental_update | 1 | 0 | 0 | 21 | 36 |  |  | 0.1135 |
| java | JsonFileIndexStorage | build | 3 | 381 | 1 | 21 | 28 | adventure | 2 | 3.3825 |
| java | JsonFileIndexStorage | persist | 3 | 381 | 1 | 21 | 28 |  |  | 16.4956 |
| java | JsonFileIndexStorage | query | 3 | 381 | 1 | 21 | 28 | adventure | 2 | 0.0016 |
| java | FolderHierarchyIndexStorage | build | 3 | 420 | 14 | 21 | 28 | adventure | 2 | 3.3825 |
| java | FolderHierarchyIndexStorage | persist | 3 | 420 | 14 | 21 | 28 |  |  | 16.8492 |
| java | FolderHierarchyIndexStorage | query | 3 | 420 | 14 | 21 | 28 | adventure | 2 | 0.0151 |
| java | in-memory | incremental_update | 1 | 0 | 0 | 21 | 36 |  |  | 0.6842 |
| python | monolithic-json | build | 3 | 336 | 1 | 21 | 28 | adventure | 2 | 0.5929 |
| python | monolithic-json | persist | 3 | 336 | 1 | 21 | 28 |  |  | 0.6682 |
| python | monolithic-json | query | 3 | 336 | 1 | 21 | 28 | adventure | 2 | 0.8018 |
| python | hierarchy-json | build | 3 | 336 | 14 | 21 | 28 | adventure | 2 | 0.5929 |
| python | hierarchy-json | persist | 3 | 336 | 14 | 21 | 28 |  |  | 11.0021 |
| python | hierarchy-json | query | 3 | 336 | 14 | 21 | 28 | adventure | 2 | 30.6770 |
| python | in-memory | incremental_update | 1 | 0 | 0 | 21 | 36 |  |  | 0.2065 |

Workload: same in-memory postings and the same `adventure` query; query rows read the serialized representation for Python/C# and the equivalent in-memory representation for Java.

These three-book results validate correctness and measurement boundaries only. They do not support 500/1000-book scalability claims.
