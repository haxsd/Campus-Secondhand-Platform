$ErrorActionPreference = 'Stop'
$ProjectRoot = Split-Path -Parent $PSScriptRoot
$EnvFile = Join-Path $PSScriptRoot '.env'

function Import-DotEnv([string]$Path) {
    if (-not (Test-Path $Path)) { return }
    foreach ($line in Get-Content $Path) {
        if ($line -match '^\s*#' -or $line -notmatch '=') { continue }
        $parts = $line -split '=', 2
        [Environment]::SetEnvironmentVariable($parts[0].Trim(), $parts[1].Trim())
    }
}

Import-DotEnv $EnvFile
$env:CT_PROFILE = if ($env:CT_PROFILE) { $env:CT_PROFILE } else { 'prod' }

Push-Location $ProjectRoot
try {
    docker compose --env-file $EnvFile --profile rocketmq -f deploy/docker-compose.yml up -d
    if ($LASTEXITCODE -ne 0) { throw "Dependency startup failed with exit code $LASTEXITCODE" }

    $jar = Get-ChildItem "$ProjectRoot\backend\target\*.jar" |
        Where-Object { $_.Name -notlike '*-sources.jar' -and $_.Name -notlike '*-plain.jar' } |
        Select-Object -First 1
    if (-not $jar) { throw 'Backend JAR not found. Run deploy\build.ps1 first.' }

    $backendStdout = Join-Path $env:TEMP 'campus-trade-backend.out.log'
    $backendStderr = Join-Path $env:TEMP 'campus-trade-backend.err.log'
    $backend = Start-Process java -ArgumentList "-Duser.timezone=Asia/Shanghai", "-jar", "`"$($jar.FullName)`"" `
        -WorkingDirectory $ProjectRoot -RedirectStandardOutput $backendStdout `
        -RedirectStandardError $backendStderr -PassThru
    $backend.Id | Set-Content (Join-Path $env:TEMP 'campus-trade-backend.pid')

    if (Get-Command nginx.exe -ErrorAction SilentlyContinue) {
        nginx.exe -p "$ProjectRoot\" -c deploy/nginx/campus-trade.conf
    }
    else {
        Write-Warning 'nginx.exe was not found on PATH; start nginx separately with deploy\nginx\campus-trade.conf.'
    }
}
finally {
    Pop-Location
}
