$ErrorActionPreference = 'Stop'

$requiredVariables = @(
    'KAKAO_CLIENT_ID',
    'KAKAO_CLIENT_SECRET',
    'KAKAO_REDIRECT_URI',
    'KAKAO_TOKEN_ENCRYPTION_KEY'
)

$missingVariables = $requiredVariables | Where-Object {
    [string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($_, 'Process'))
}

if ($missingVariables.Count -gt 0) {
    Write-Host 'Kakao OAuth setup is incomplete.' -ForegroundColor Yellow
    Write-Host 'Run setup-kakao-oauth.ps1 once, then open a new terminal.'
    Write-Host ('Missing: ' + ($missingVariables -join ', '))
    exit 1
}

$env:KAKAO_MESSAGE_ENABLED = 'true'
$env:KAKAO_MESSAGE_DRY_RUN = 'false'
$env:KAKAO_DASHBOARD_URL = 'http://localhost:5173/admin/predictive-maintenance'

Push-Location $PSScriptRoot
try {
    Write-Host 'Starting backend with persistent Kakao OAuth.' -ForegroundColor Green
    Write-Host 'Use the Kakao account connection button in the predictive maintenance screen.'
    & .\gradlew.bat bootRun
}
finally {
    Pop-Location
}
