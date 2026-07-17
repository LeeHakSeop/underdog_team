@echo off
chcp 65001 > nul
set /p DBNAME=Database name: 
set /p DBUSER=PostgreSQL user [postgres]: 
if "%DBUSER%"=="" set DBUSER=postgres
psql -v ON_ERROR_STOP=1 -U %DBUSER% -d %DBNAME% -f RUN_EXISTING_DB_FULL_PATCH.psql
if errorlevel 1 (
  echo.
  echo [FAILED] Check the error message above.
  pause
  exit /b 1
)
echo.
echo [SUCCESS] Full DB patch completed.
pause
