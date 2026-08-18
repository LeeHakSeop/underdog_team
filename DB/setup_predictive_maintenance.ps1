[CmdletBinding()]
param(
    [string]$DatabaseHost = 'localhost',
    [int]$DatabasePort = 5432,
    [string]$DatabaseName = 'port_db',
    [string]$DatabaseUser = 'port_user',
    [string]$ApiBaseUrl = 'http://localhost',
    [switch]$SkipSchema,
    [switch]$SkipImport
)

$ErrorActionPreference = 'Stop'

$schemaPath = Join-Path $PSScriptRoot 'predictive_maintenance.sql'
$csvPath = Join-Path $PSScriptRoot 'data\01_dashboard_timeseries.csv'
$importUrl = "$($ApiBaseUrl.TrimEnd('/'))/api/predictive-maintenance/sensor-data/import"
$summaryUrl = "$($ApiBaseUrl.TrimEnd('/'))/api/predictive-maintenance/sensor-data/summary"

if (-not $SkipSchema) {
    if (-not (Test-Path -LiteralPath $schemaPath)) {
        throw "Schema file not found: $schemaPath"
    }

    if (-not (Get-Command psql -ErrorAction SilentlyContinue)) {
        throw 'psql 명령을 찾을 수 없습니다. PostgreSQL bin 폴더를 PATH에 추가한 뒤 다시 실행하세요.'
    }

    Write-Host '[1/2] 예지보전 테이블 3개를 생성합니다.'
    Write-Host 'PostgreSQL 비밀번호를 요청하면 현재 컴퓨터의 DB 비밀번호를 입력하세요.'

    & psql `
        --host=$DatabaseHost `
        --port=$DatabasePort `
        --username=$DatabaseUser `
        --dbname=$DatabaseName `
        --set=ON_ERROR_STOP=1 `
        --file=$schemaPath

    if ($LASTEXITCODE -ne 0) {
        throw "예지보전 스키마 적용에 실패했습니다. psql 종료 코드: $LASTEXITCODE"
    }
}

if (-not $SkipImport) {
    if (-not (Test-Path -LiteralPath $csvPath)) {
        throw "CSV file not found: $csvPath"
    }

    if (-not (Get-Command curl.exe -ErrorAction SilentlyContinue)) {
        throw 'curl.exe 명령을 찾을 수 없습니다.'
    }

    Write-Host '[2/2] CSV를 실행 중인 Spring 백엔드 API로 적재합니다.'
    Write-Host "백엔드 주소: $ApiBaseUrl"

    & curl.exe `
        --fail-with-body `
        --silent `
        --show-error `
        --request POST `
        --form "file=@$csvPath;type=text/csv" `
        $importUrl

    if ($LASTEXITCODE -ne 0) {
        throw 'CSV 적재에 실패했습니다. Spring 백엔드가 실행 중인지 확인하세요.'
    }

    Write-Host "`n적재 결과를 확인합니다."
    & curl.exe --fail-with-body --silent --show-error $summaryUrl

    if ($LASTEXITCODE -ne 0) {
        throw '적재 결과 조회에 실패했습니다.'
    }
}

Write-Host "`n예지보전 DB 준비가 완료되었습니다."
