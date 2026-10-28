$ErrorActionPreference = 'Continue'
Write-Host '=== CampusLab environment check ===' -ForegroundColor Cyan
foreach ($command in @('java', 'javac', 'mvn', 'git')) {
    $found = Get-Command $command -ErrorAction SilentlyContinue
    if ($found) { Write-Host "[OK] $command -> $($found.Source)" -ForegroundColor Green }
    else { Write-Host "[MISSING] $command is not on PATH" -ForegroundColor Red }
}
Write-Host ''
java -version 2>&1
mvn -version 2>&1
 git --version 2>&1
if (Test-Path '.env') { Write-Host '[OK] .env exists (verify DB settings locally; do not share its contents).' -ForegroundColor Green }
else { Write-Host '[NEXT] Run .\scripts\setup.ps1 to create the local .env file.' -ForegroundColor Yellow }
