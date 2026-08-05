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

function Get-JavaMajorVersion([string]$JavaPath) {
    $versionOutput = (& $JavaPath -version 2>&1 | Out-String)
    if ($versionOutput -match 'version "([^"]+)"') {
        $version = $matches[1]
        if ($version -like '1.*') {
            return [int]($version.Split('.')[1])
        }
        return [int]($version.Split('.')[0])
    }
    return $null
}

Import-DotEnv $EnvFile
$env:CT_PROFILE = if ($env:CT_PROFILE) { $env:CT_PROFILE } else { 'prod' }
$uploadDir = if ($env:CT_UPLOAD_DIR) { $env:CT_UPLOAD_DIR } else { 'C:\campus-trade\uploads' }
New-Item -ItemType Directory -Path $uploadDir -Force | Out-Null

Push-Location $ProjectRoot
try {
    docker compose --env-file $EnvFile --profile production -f deploy/docker-compose.yml up -d
    if ($LASTEXITCODE -ne 0) { throw "MySQL, Redis or nginx startup failed with exit code $LASTEXITCODE" }

    docker compose --env-file $EnvFile -f deploy/rocketmq/docker-compose.yml up -d
    if ($LASTEXITCODE -ne 0) { throw "RocketMQ startup failed with exit code $LASTEXITCODE" }

    for ($attempt = 0; $attempt -lt 60; $attempt++) {
        $topicStatus = docker inspect campus-trade-topic-init --format '{{.State.Status}} {{.State.ExitCode}}' 2>$null
        if ($topicStatus -eq 'exited 0') { break }
        if ($topicStatus -like 'exited *') { throw "RocketMQ topic initialization failed: $topicStatus" }
        Start-Sleep -Seconds 2
    }
    if ($topicStatus -ne 'exited 0') { throw 'Timed out waiting for RocketMQ topic initialization.' }

    $jar = Get-ChildItem "$ProjectRoot\backend\target\*.jar" |
        Where-Object { $_.Name -notlike '*-sources.jar' -and $_.Name -notlike '*-plain.jar' } |
        Select-Object -First 1
    if (-not $jar) { throw 'Backend JAR not found. Run deploy\build.ps1 first.' }

    $backendStdout = Join-Path $env:TEMP 'campus-trade-backend.out.log'
    $backendStderr = Join-Path $env:TEMP 'campus-trade-backend.err.log'
    $javaPath = $null
    $javaMajorVersion = $null
    $pathJava = Get-Command java -ErrorAction SilentlyContinue
    if ($pathJava) {
        $pathJavaMajorVersion = Get-JavaMajorVersion $pathJava.Source
        if ($pathJavaMajorVersion -ge 17) {
            $javaPath = $pathJava.Source
            $javaMajorVersion = $pathJavaMajorVersion
        }
    }

    if (-not $javaPath -and $env:JAVA_HOME) {
        $javaHomeJava = Join-Path $env:JAVA_HOME 'bin\java.exe'
        if (Test-Path $javaHomeJava) {
            $javaHomeMajorVersion = Get-JavaMajorVersion $javaHomeJava
            if ($javaHomeMajorVersion -ge 17) {
                $javaPath = $javaHomeJava
                $javaMajorVersion = $javaHomeMajorVersion
            }
        }
    }

    if (-not $javaPath) {
        throw 'Java 17 or newer was not found. Add a Java 17+ bin directory to PATH or set JAVA_HOME to a Java 17+ installation.'
    }

    $backend = Start-Process $javaPath -ArgumentList "-Duser.timezone=Asia/Shanghai", "-jar", "`"$($jar.FullName)`"" `
        -WorkingDirectory $ProjectRoot -RedirectStandardOutput $backendStdout `
        -RedirectStandardError $backendStderr -PassThru
    $backend.Id | Set-Content (Join-Path $env:TEMP 'campus-trade-backend.pid')

}
finally {
    Pop-Location
}
