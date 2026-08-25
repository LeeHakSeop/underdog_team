$ErrorActionPreference = 'Stop'

Write-Host 'Update Kakao client secret' -ForegroundColor Cyan
$secureClientSecret = Read-Host 'New Kakao Login client secret' -AsSecureString
$clientSecret = [System.Net.NetworkCredential]::new('', $secureClientSecret).Password

if ([string]::IsNullOrWhiteSpace($clientSecret)) {
    throw 'The Kakao client secret is required.'
}

[Environment]::SetEnvironmentVariable('KAKAO_CLIENT_SECRET', $clientSecret, 'User')

$clientSecret = $null
$secureClientSecret = $null

Write-Host 'Client secret updated. Restart IntelliJ before running the backend again.' -ForegroundColor Green
