# Stage 1 requirement coverage

| Requirement area | Java | Python | C# | Evidence |
|---|---|---|---|---|
| Common corpus and tokenizer | yes | yes | yes | `corpus/`, tokenizer implementations |
| `by_book` write | yes | yes | yes | `results-*.csv` |
| `by_time` write | yes | yes | yes | `results-*.csv` |
| `by_batch` write | yes | yes | yes | `results-*.csv` |
| Monolithic index | yes | yes | yes | `results-*.csv` |
| Hierarchy index | yes | yes | yes | `results-*.csv` |
| Incremental update | yes | yes | yes | `results-*.csv` |
| Query answer validation | yes | yes | yes | `answer_count`, expected `adventure=1,2` |
| MongoDB storage | implementation | not claimed | not claimed | `MongoIndexStorage`; requires external MongoDB |
| Metadata/IDs/postings equivalence | parser and index | tokenizer/index | tokenizer/index | common manifest and checks |

The benchmark deliberately does not claim that a small three-book run proves
scalability. 500/1000-book runs remain an explicit workload extension, not an
unverified label.
