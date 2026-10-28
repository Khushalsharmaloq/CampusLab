$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
if (-not (Test-Path (Join-Path $root '.env'))) { throw 'Missing .env. Run .\scripts\setup.ps1 first.' }
$env:CAMPUSLAB_ENV_FILE = Join-Path $root '.env'
Push-Location $root
try {
    mvn exec:java "-Dexec.mainClass=com.campuslab.desktop.CampusLabDesktop"
    if ($LASTEXITCODE -ne 0) { throw "CampusLab desktop stopped with exit code $LASTEXITCODE." }
} finally { Pop-Location }
