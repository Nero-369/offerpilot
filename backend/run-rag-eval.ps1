param(
    [string]$BaseUrl = "http://localhost",
    [string]$Dataset = "$PSScriptRoot\rag-eval.example.json"
)

$resolvedDataset = (Resolve-Path -LiteralPath $Dataset).Path
$body = Get-Content -LiteralPath $resolvedDataset -Raw -Encoding UTF8
$result = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/v1/knowledge/evaluate" -ContentType "application/json; charset=utf-8" -Body $body
$result | ConvertTo-Json -Depth 8
