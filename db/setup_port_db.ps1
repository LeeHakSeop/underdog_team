param(
    [string] $AdminUser = "postgres",
    [string] $AdminPassword = "",
    [string] $HostName = "localhost",
    [int] $Port = 5432
)

$ErrorActionPreference = "Stop"

$psql = "C:\Program Files\PostgreSQL\18\bin\psql.exe"
if (-not (Test-Path $psql)) {
    $psql = "psql"
}

if ($AdminPassword) {
    $env:PGPASSWORD = $AdminPassword
}

& $psql -h $HostName -p $Port -U $AdminUser -d postgres -v ON_ERROR_STOP=1 -c "DO `$`$ BEGIN IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'port_user') THEN CREATE ROLE port_user LOGIN PASSWORD '123456'; ELSE ALTER ROLE port_user WITH LOGIN PASSWORD '123456'; END IF; END `$`$;"

$dbExists = & $psql -h $HostName -p $Port -U $AdminUser -d postgres -tAc "SELECT 1 FROM pg_database WHERE datname = 'port_db';"
if ($dbExists.Trim() -ne "1") {
    & $psql -h $HostName -p $Port -U $AdminUser -d postgres -v ON_ERROR_STOP=1 -c "CREATE DATABASE port_db OWNER port_user;"
}

& $psql -h $HostName -p $Port -U $AdminUser -d port_db -v ON_ERROR_STOP=1 -f "$PSScriptRoot\schema.sql"
& $psql -h $HostName -p $Port -U $AdminUser -d port_db -v ON_ERROR_STOP=1 -f "$PSScriptRoot\data.sql"
& $psql -h $HostName -p $Port -U $AdminUser -d port_db -v ON_ERROR_STOP=1 -c "GRANT ALL PRIVILEGES ON SCHEMA port_gate TO port_user; GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA port_gate TO port_user; GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA port_gate TO port_user;"

Write-Output "port_db setup complete. App user: port_user / 123456"
