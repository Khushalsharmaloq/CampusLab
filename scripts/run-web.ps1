param(
    [Parameter(Mandatory = $true, HelpMessage = 'Full path to the extracted Apache Tomcat 11 folder')]
    [string]$TomcatHome
)
$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$tomcat = (Resolve-Path $TomcatHome).Path
if (-not (Test-Path (Join-Path $root '.env'))) { throw 'Missing .env. Run .\scripts\setup.ps1 first.' }
if (-not (Test-Path (Join-Path $tomcat 'bin\startup.bat'))) { throw 'TomcatHome must be the extracted Tomcat folder containing bin\startup.bat.' }
if (-not (Test-Path (Join-Path $tomcat 'webapps'))) { throw 'Tomcat webapps folder was not found.' }

Push-Location $root
try {
    mvn clean test package
    if ($LASTEXITCODE -ne 0) { throw "Maven build failed with exit code $LASTEXITCODE." }
    $env:CAMPUSLAB_ENV_FILE = Join-Path $root '.env'
    Copy-Item (Join-Path $root 'target\campuslab.war') (Join-Path $tomcat 'webapps\campuslab.war') -Force
    Write-Host 'WAR deployed to Tomcat webapps folder.' -ForegroundColor Green
    Write-Host 'Launching Tomcat. If it is already running, shut it down first and rerun this script.' -ForegroundColor Yellow
    & (Join-Path $tomcat 'bin\startup.bat')
    Start-Sleep -Seconds 3
    Start-Process 'http://localhost:8080/campuslab/login'
} finally { Pop-Location }
