param(
    [string]$SearchUrl = "http://localhost:7003/search?q=adventure&limit=20",
    [int]$Requests = 20
)

$times = 1..$Requests | ForEach-Object {
    $watch = [Diagnostics.Stopwatch]::StartNew()
    Invoke-RestMethod $SearchUrl | Out-Null
    $watch.Stop()
    $watch.Elapsed.TotalMilliseconds
}
$average = ($times | Measure-Object -Average).Average
[pscustomobject]@{
    requests = $Requests
    average_ms = [math]::Round($average, 2)
    min_ms = [math]::Round(($times | Measure-Object -Minimum).Minimum, 2)
    max_ms = [math]::Round(($times | Measure-Object -Maximum).Maximum, 2)
}
