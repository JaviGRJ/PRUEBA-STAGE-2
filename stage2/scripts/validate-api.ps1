param(
    [string]$SearchUrl = "http://localhost:7003",
    [string]$IndexingUrl = "http://localhost:7002"
)

$indexed = Invoke-RestMethod -Method Post "$IndexingUrl/index/update/1342"
if ($indexed.status -ne "indexed") { throw "Indexing API did not publish indexed status" }

$result = Invoke-RestMethod "$SearchUrl/search?q=adventure&author=Jane%20Austen&language=en&year=1813&offset=0&limit=20"
if ($result.total -lt 1) { throw "Search API returned no matching result" }
if ($result.results[0].year -ne 1813) { throw "Year filter/result contract failed" }

try {
    Invoke-RestMethod "$SearchUrl/search" -ErrorAction Stop
    throw "Missing q was accepted"
} catch {
    if ($_.Exception.Response.StatusCode.value__ -ne 400) { throw }
}

Write-Output "API validation passed"
