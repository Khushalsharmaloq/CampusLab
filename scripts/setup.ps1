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

$url = 'jdbc:mysql://localhost:3306/campuslab?createDatabaseIfNotExist=true&serverTimezone=UTC'
# The .env parser reads everything after the first '=' as the password value.
$escapedPassword = $dbPassword
Write-Host ''
Write-Host 'Optional first administrator bootstrap' -ForegroundColor Cyan
Write-Host 'Leave the email blank to configure the administrator later.'
$adminEmail = Read-Host 'Initial administrator email'
$adminName = 'CampusLab Administrator'
$adminPassword = ''
if (-not [string]::IsNullOrWhiteSpace($adminEmail)) {
    $adminNameInput = Read-Host 'Initial administrator name [CampusLab Administrator]'
    if (-not [string]::IsNullOrWhiteSpace($adminNameInput)) { $adminName = $adminNameInput.Trim() }
    $secureAdminPassword = Read-Host 'Initial administrator password (at least 12 characters)' -AsSecureString
    $adminPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secureAdminPassword)
    try { $adminPassword = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($adminPointer) }
    finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($adminPointer) }
    if ($adminPassword.Length -lt 12) { throw 'Initial administrator password must be at least 12 characters.' }
}
$lines = @(
    '# CampusLab local configuration. This file is excluded from Git.',
    "DB_URL=$url",
    "DB_USER=$dbUser",
    ('DB_PASSWORD=' + $escapedPassword)
)
if (-not [string]::IsNullOrWhiteSpace($adminEmail)) {
    $lines += 'BOOTSTRAP_ADMIN_NAME=' + $adminName
    $lines += 'BOOTSTRAP_ADMIN_EMAIL=' + $adminEmail.Trim().ToLowerInvariant()
    $lines += 'BOOTSTRAP_ADMIN_PASSWORD=' + $adminPassword
}
[System.IO.File]::WriteAllLines($envPath, $lines, [System.Text.UTF8Encoding]::new($false))
Write-Host "Created $envPath" -ForegroundColor Green
Write-Host 'Next: run .\scripts\build.ps1, then .\scripts\run-desktop.ps1.' -ForegroundColor Cyan
if (-not [string]::IsNullOrWhiteSpace($adminEmail)) { Write-Host 'After the first successful startup, remove BOOTSTRAP_ADMIN_PASSWORD from .env.' -ForegroundColor Yellow }
