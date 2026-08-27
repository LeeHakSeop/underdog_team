[CmdletBinding()]
param(
    [string]$DatabaseHost = 'localhost',
    [int]$DatabasePort = 5432,
    [string]$DatabaseName = 'port_db',
    [string]$DatabaseUser = 'port_user'
)

$ErrorActionPreference = 'Stop'
$schemaPath = Join-Path $PSScriptRoot 'kakao_oauth.sql'

if (-not (Test-Path -LiteralPath $schemaPath)) {
    throw "Schema file not found: $schemaPath"
}
if (-not (Get-Command psql -ErrorAction SilentlyContinue)) {
    throw 'psql 명령을 찾을 수 없습니다. PostgreSQL bin 폴더를 PATH에 추가하세요.'
}

Write-Host '카카오 OAuth 토큰 테이블을 준비합니다.' -ForegroundColor Cyan
Write-Host '비밀번호를 요청하면 지정한 PostgreSQL 사용자의 비밀번호를 입력하세요.'

& psql `
    --host=$DatabaseHost `
    --port=$DatabasePort `
    --username=$DatabaseUser `
    --dbname=$DatabaseName `
    --set=ON_ERROR_STOP=1 `
    --file=$schemaPath

if ($LASTEXITCODE -ne 0) {
    throw "카카오 OAuth 테이블 생성에 실패했습니다. psql 종료 코드: $LASTEXITCODE"
}

Write-Host 'kakao_oauth_connection 테이블 준비가 완료되었습니다.' -ForegroundColor Green
