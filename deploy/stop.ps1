$ErrorActionPreference = 'Continue'
$ProjectRoot = Split-Path -Parent $PSScriptRoot
$EnvFile = Join-Path $PSScriptRoot '.env'

Push-Location $ProjectRoot
try {
    $pidFile = Join-Path $env:TEMP 'campus-trade-backend.pid'
    if (Test-Path $pidFile) {
        $pid = Get-Content $pidFile
        Stop-Process -Id $pid -Force -ErrorAction SilentlyContinue
        Remove-Item $pidFile -Force -ErrorAction SilentlyContinue
    }
    docker compose --env-file $EnvFile --profile rocketmq --profile production -f deploy/docker-compose.yml down
}
finally {
    Pop-Location
}
