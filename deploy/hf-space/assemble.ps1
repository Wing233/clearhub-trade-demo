param(
    [string]$Out = (Join-Path $env:TEMP "clearhub-hf-space")
)

$ErrorActionPreference = "Stop"
$root = Resolve-Path (Join-Path $PSScriptRoot "..\..")

if (Test-Path $Out) {
    Remove-Item $Out -Recurse -Force
}
New-Item -ItemType Directory -Path $Out | Out-Null
New-Item -ItemType Directory -Path (Join-Path $Out "app") | Out-Null
New-Item -ItemType Directory -Path (Join-Path $Out "web") | Out-Null
New-Item -ItemType Directory -Path (Join-Path $Out "db") | Out-Null

Copy-Item (Join-Path $PSScriptRoot "Dockerfile") $Out
Copy-Item (Join-Path $PSScriptRoot "README.md") $Out
Copy-Item (Join-Path $PSScriptRoot "entrypoint.sh") $Out
Copy-Item (Join-Path $PSScriptRoot "db-init.sh") $Out
Copy-Item (Join-Path $PSScriptRoot "supervisord.conf") $Out
Copy-Item (Join-Path $PSScriptRoot "nginx.conf") $Out
Copy-Item (Join-Path $PSScriptRoot "mysql-hf.cnf") $Out

Copy-Item (Join-Path $root "target\trade-core.jar") (Join-Path $Out "app\trade-core.jar")
Copy-Item (Join-Path $root "services\mock-bank\target\mock-bank.jar") (Join-Path $Out "app\mock-bank.jar")
Copy-Item (Join-Path $root "services\clearing-adapter\target\clearing-adapter.jar") (Join-Path $Out "app\clearing-adapter.jar")
Copy-Item (Join-Path $root "db\init\01-schema.sql") (Join-Path $Out "db\01-schema.sql")
Copy-Item (Join-Path $root "web\dist\*") (Join-Path $Out "web") -Recurse

Get-ChildItem $Out -Recurse -File | Measure-Object -Property Length -Sum |
    Select-Object Count, @{Name = "SizeMB"; Expression = { [math]::Round($_.Sum / 1MB, 1) } }
Write-Host "HF Space staging ready: $Out"
