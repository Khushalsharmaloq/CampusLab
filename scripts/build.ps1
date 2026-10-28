$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
Push-Location $root
try {
    mvn clean test package
    if ($LASTEXITCODE -ne 0) { throw "Maven build failed with exit code $LASTEXITCODE. Review the first [ERROR] above." }
    Write-Host "Build successful: $root\target\campuslab.war" -ForegroundColor Green
} finally { Pop-Location }
