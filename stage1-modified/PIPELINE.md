# Stage 1 pipeline

```mermaid
flowchart LR
  G[Gutenberg downloader] --> H[Header files]
  G --> B[Body files]
  H --> L1[(by_book)]
  H --> L2[(by_time)]
  H --> L3[(by_batch)]
  B --> L1
  B --> L2
  B --> L3
  H --> M[MetadataParser]
  M --> S[(metadata.db / SQLite)]
  B --> T[Shared tokenizer]
  T --> I[InvertedIndex]
  I --> J[(Monolithic JSON)]
  I --> Y[(Hierarchy JSON)]
  I --> N[(MongoDB)]
  L1 --> C[downloaded_books.txt]
  I --> X[indexed_books.txt]
  C --> P[PipelineController]
  P --> X
```

`downloaded_books.txt` is updated after Header/Body creation. The indexing
control file is updated only after every configured storage returns successfully.
The benchmark write region excludes Gutenberg network download, so disk results
are not mislabeled network or generic-operation throughput.
