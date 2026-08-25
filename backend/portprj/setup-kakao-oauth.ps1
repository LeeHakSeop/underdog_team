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
if ([string]::IsNullOrWhiteSpace($encryptionKey)) {
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

$clientSecret = $null
$secureClientSecret = $null
$encryptionKey = $null

Write-Host ''
Write-Host 'Setup complete. Close this terminal and open a new terminal before starting the backend.' -ForegroundColor Green
Write-Host 'Register this exact redirect URI in Kakao Developers:'
Write-Host $redirectUri -ForegroundColor Yellow
