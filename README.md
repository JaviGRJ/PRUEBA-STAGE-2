# Big Data inverted index

The repository is intentionally split into three areas:

- `RESOURCE. Docker project example/` is the professor's original example and is
  not modified.
- `stage1-modified/` contains the refactored Java data layer and equivalent
  Python/C# tokenizer and measurement implementations.
- `stage2/` contains the independent Java 17/Javalin service-oriented
  implementation, its OpenAPI contract, and run instructions.

Build Stage 1 with `mvn -f stage1-modified/pom.xml test` and Stage 2 with
`mvn -f stage2/pom.xml clean package`. The Stage 2 workflow is exposed through
the Control service at port 7000; see `stage2/README.md` and
`stage2/openapi.yaml`.
