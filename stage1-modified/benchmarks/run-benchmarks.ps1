param(
    [string]$Corpus = "",
    [string]$OutputDir = "benchmarks"
)
$ErrorActionPreference = "Stop"
$stage = Split-Path -Parent $PSScriptRoot
$corpus = if ($Corpus) { (Resolve-Path $Corpus).Path } else { (Join-Path $PSScriptRoot "corpus") }
$mvn = "C:\Program Files\JetBrains\IntelliJ IDEA 2025.3\plugins\maven\lib\maven3\bin\mvn.cmd"

Push-Location $stage
try {
    New-Item -ItemType Directory -Force $OutputDir | Out-Null
    $javaVersion = (cmd /c "java -version 2>&1" | Out-String).Trim()
    @(
        "revision=$(git rev-parse HEAD)"
        "os=$([System.Environment]::OSVersion.VersionString)"
        "java=$javaVersion"
        "dotnet=$(dotnet --version)"
        "python=$(python --version 2>&1)"
        "timestamp=$([DateTime]::UtcNow.ToString('o'))"
        "corpus=$corpus\manifest.csv"
        "network_download_in_timed_region=false"
    ) | Set-Content (Join-Path $OutputDir "environment.txt")
    & $mvn -q clean test
    & $mvn -q dependency:build-classpath "-Dmdep.outputFile=target\benchmark-cp.txt"
    $cp = (Get-Content "target\benchmark-cp.txt" -Raw).Trim() + ";target\classes"
    java -cp $cp org.ulpgc.bigdata.benchmarks.Stage1Benchmark $corpus (Join-Path $OutputDir "results-java.csv")
    python "benchmarks\python_benchmark.py" $corpus (Join-Path $OutputDir "results-python.csv")
    dotnet run --project "csharp\Stage1Modified.csproj" -- $corpus (Join-Path $OutputDir "results-csharp.csv")
    python "benchmarks\compare_tokenizers.py" $corpus
    python "benchmarks\validate_results.py" (Join-Path $OutputDir "results-java.csv") (Join-Path $OutputDir "results-python.csv") (Join-Path $OutputDir "results-csharp.csv")
    python "benchmarks\render_results.py" $OutputDir
    Write-Output "Stage 1 benchmark completed. Results are in stage1-modified/benchmarks."
}
finally { Pop-Location }
