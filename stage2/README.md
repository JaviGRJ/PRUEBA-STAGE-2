# Stage 2 service-oriented search engine

Stage 2 uses classical HTTP orchestration on one host:

| Service | Default port | Consumer | Responsibility |
|---|---:|---|---|
| Control | 7000 | operator | calls ingestion, then indexing |
| Ingestion | 7001 | Control | downloads Gutenberg text into `DATA_ROOT/datalake/by_book` |
| Indexing | 7002 | Control | builds and atomically publishes `datamart/published.json` and metadata |
| Search | 7003 | front-end | `/search` and `/books/{bookId}` |

Build with `cd stage2; mvn clean package`. Run each main class from the generated
classpath (or from an IDE): `IngestionService`, `IndexingService`,
`SearchService`, and `ControlService`. `PORT`, `DATA_ROOT`, `INGESTION_URL`, and
`INDEXING_URL` are configurable environment variables.

Each service also has an independent Maven project under `services/`; build all
four executable jars with `cd services; mvn clean package`.

Example workflow:

```text
POST http://localhost:7000/run/1342
GET  http://localhost:7003/search?q=adventure&language=en&offset=0&limit=20
GET  http://localhost:7003/books/1342
```

The control files are `downloaded_books.txt` and `indexed_books.txt`. Ingestion
is idempotent, indexing requires an ingested body, and Search reads one atomically
replaced `datamart/published.json` snapshot containing both the postings and
metadata. SQLite `metadata.db` is updated in the same indexing critical section.
The complete versioned contract is `openapi.yaml`. Run
`powershell -File scripts/validate-api.ps1` for interface checks and
`powershell -File scripts/benchmark-search.ps1` for repeatable latency samples.

The single-host deployment uses one process per service and Javalin request
threads. Indexing serializes publication with a process-local critical section;
the control files are idempotent. For multiple replicas in a future deployment,
replace the local lock with a distributed claim/lease and move the datalake and
published snapshot to shared durable storage.
