param([int[]]$Sizes = @(500, 1000))
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
foreach ($size in $Sizes) {
    $corpus = Join-Path $PSScriptRoot "generated-$size"
    $output = Join-Path $PSScriptRoot "scale-results-$size"
    python (Join-Path $PSScriptRoot "generate_corpus.py") $size $corpus
    powershell -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot "run-benchmarks.ps1") `
        -Corpus $corpus -OutputDir $output
}
