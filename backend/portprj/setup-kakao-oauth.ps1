[CmdletBinding()]
param(
    [switch]$UseExistingTokenDatabase
)

$ErrorActionPreference = 'Stop'

Write-Host 'One-time Kakao OAuth setup' -ForegroundColor Cyan
Write-Host 'Values are saved in the current Windows user environment, not in the repository.'
Write-Host ''

$clientId = Read-Host 'Kakao REST API key'
$secureClientSecret = Read-Host 'Kakao client secret' -AsSecureString
$clientSecret = [System.Net.NetworkCredential]::new('', $secureClientSecret).Password
$redirectUri = Read-Host 'Redirect URI [http://localhost/api/predictive-maintenance/demo/notifications/kakao/oauth/callback]'
$frontendReturnUrl = Read-Host 'Frontend return URL [http://localhost:5173/admin/predictive-maintenance]'

if ([string]::IsNullOrWhiteSpace($clientId)) {
    throw 'The Kakao REST API key is required.'
}
if ([string]::IsNullOrWhiteSpace($clientSecret)) {
    throw 'The Kakao client secret is required.'
}
if ([string]::IsNullOrWhiteSpace($redirectUri)) {
    $redirectUri = 'http://localhost/api/predictive-maintenance/demo/notifications/kakao/oauth/callback'
}
if ([string]::IsNullOrWhiteSpace($frontendReturnUrl)) {
    $frontendReturnUrl = 'http://localhost:5173/admin/predictive-maintenance'
}

$encryptionKey = [Environment]::GetEnvironmentVariable('KAKAO_TOKEN_ENCRYPTION_KEY', 'User')
if ($UseExistingTokenDatabase) {
    Write-Host ''
    Write-Host 'This computer will use a database that already contains encrypted Kakao tokens.' -ForegroundColor Yellow
    $secureEncryptionKey = Read-Host 'Existing KAKAO_TOKEN_ENCRYPTION_KEY' -AsSecureString
    $encryptionKey = [System.Net.NetworkCredential]::new('', $secureEncryptionKey).Password
    $secureEncryptionKey = $null

    if ([string]::IsNullOrWhiteSpace($encryptionKey)) {
        throw 'The existing token encryption key is required when -UseExistingTokenDatabase is specified.'
    }
}
elseif ([string]::IsNullOrWhiteSpace($encryptionKey)) {
    $keyBytes = New-Object byte[] 48
    $randomGenerator = [Security.Cryptography.RandomNumberGenerator]::Create()
    try {
        $randomGenerator.GetBytes($keyBytes)
    }
    finally {
        $randomGenerator.Dispose()
    }
    $encryptionKey = [Convert]::ToBase64String($keyBytes)
}

[Environment]::SetEnvironmentVariable('KAKAO_CLIENT_ID', $clientId, 'User')
[Environment]::SetEnvironmentVariable('KAKAO_CLIENT_SECRET', $clientSecret, 'User')
[Environment]::SetEnvironmentVariable('KAKAO_REDIRECT_URI', $redirectUri, 'User')
[Environment]::SetEnvironmentVariable('KAKAO_FRONTEND_RETURN_URL', $frontendReturnUrl, 'User')
[Environment]::SetEnvironmentVariable('KAKAO_TOKEN_ENCRYPTION_KEY', $encryptionKey, 'User')
[Environment]::SetEnvironmentVariable('KAKAO_MESSAGE_ENABLED', 'true', 'User')
[Environment]::SetEnvironmentVariable('KAKAO_MESSAGE_DRY_RUN', 'false', 'User')
[Environment]::SetEnvironmentVariable('KAKAO_DASHBOARD_URL', $frontendReturnUrl, 'User')

$clientSecret = $null
$secureClientSecret = $null
$encryptionKey = $null

Write-Host ''
Write-Host 'Setup complete.' -ForegroundColor Green
Write-Host 'Close every IntelliJ window and start IntelliJ again before running PortprjApplication.' -ForegroundColor Yellow
Write-Host 'Register this exact redirect URI in Kakao Developers:'
Write-Host $redirectUri -ForegroundColor Yellow
