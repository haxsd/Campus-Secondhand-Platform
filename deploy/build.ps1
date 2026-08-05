$ErrorActionPreference = 'Stop'
$ProjectRoot = Split-Path -Parent $PSScriptRoot

Push-Location $ProjectRoot
try {
    & "$ProjectRoot\backend\mvnw.cmd" -B clean package -DskipTests
    if ($LASTEXITCODE -ne 0) { throw "Backend package failed with exit code $LASTEXITCODE" }

    Push-Location "$ProjectRoot\frontend"
    try {
        npm ci
        if ($LASTEXITCODE -ne 0) { throw "npm ci failed with exit code $LASTEXITCODE" }
        npm run build
        if ($LASTEXITCODE -ne 0) { throw "Frontend build failed with exit code $LASTEXITCODE" }
    }
    finally {
        Pop-Location
    }
}
finally {
    Pop-Location
}
