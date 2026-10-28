$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$envPath = Join-Path $root '.env'

if (Test-Path $envPath) {
    $answer = Read-Host '.env already exists. Reconfigure it? (y/N)'
    if ($answer -notin @('y', 'Y', 'yes', 'YES')) {
        Write-Host 'Kept the existing .env file.' -ForegroundColor Green
        exit 0
    }
}

Write-Host ''
Write-Host 'CampusLab local database configuration' -ForegroundColor Cyan
Write-Host 'Enter the MySQL account that can create/use the campuslab database.'
$dbUser = Read-Host 'MySQL username [root]'
if ([string]::IsNullOrWhiteSpace($dbUser)) { $dbUser = 'root' }
$securePassword = Read-Host 'MySQL password (leave blank if your account has no password)' -AsSecureString
$pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)
try { $dbPassword = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer) }
finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer) }

$url = 'jdbc:mysql://localhost:3306/campuslab?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC'
# The .env parser reads everything after the first '=' as the password value.
$escapedPassword = $dbPassword
$lines = @(
    '# CampusLab local configuration. This file is excluded from Git.',
    "DB_URL=$url",
    "DB_USER=$dbUser",
    ('DB_PASSWORD=' + $escapedPassword)
)
[System.IO.File]::WriteAllLines($envPath, $lines, [System.Text.UTF8Encoding]::new($false))
Write-Host "Created $envPath" -ForegroundColor Green
Write-Host 'Next: run .\scripts\build.ps1, then .\scripts\run-desktop.ps1.' -ForegroundColor Cyan
