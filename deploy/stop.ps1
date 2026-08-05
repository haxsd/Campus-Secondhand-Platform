$ErrorActionPreference = 'Continue'
$ProjectRoot = Split-Path -Parent $PSScriptRoot
$EnvFile = Join-Path $PSScriptRoot '.env'

Push-Location $ProjectRoot
try {
    docker compose --env-file $EnvFile --profile rocketmq -f deploy/docker-compose.yml down
    if (Get-Command nginx.exe -ErrorAction SilentlyContinue) {
        nginx.exe -p "$ProjectRoot\" -c deploy/nginx/campus-trade.conf -s stop
    }
}
finally {
    Pop-Location
}

$pidFile = Join-Path $env:TEMP 'campus-trade-backend.pid'
if (Test-Path $pidFile) {
    $pid = Get-Content $pidFile
    Stop-Process -Id $pid -Force -ErrorAction SilentlyContinue
    Remove-Item $pidFile -Force -ErrorAction SilentlyContinue
}
