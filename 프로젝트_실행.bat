@echo off
setlocal

set "PROJECT_ROOT=%~dp0"
set "AI_DIR=%PROJECT_ROOT%AI"
set "BACKEND_DIR=%PROJECT_ROOT%backend\portprj"
set "FRONTEND_DIR=%PROJECT_ROOT%frontend"
set "PYTHON_EXE=%USERPROFILE%\miniconda3\envs\paddle310\python.exe"

if not exist "%PYTHON_EXE%" goto :missing_python
if not exist "%FRONTEND_DIR%\node_modules" goto :missing_frontend

echo Starting Port Gate services...
echo AI: http://127.0.0.1:8000/api/health
echo Backend: http://localhost

start "Port Gate AI - 8000" /D "%AI_DIR%" "%ComSpec%" /k %PYTHON_EXE% -m uvicorn app.main:app --host 127.0.0.1 --port 8000
start "Port Gate Backend - 80" /D "%BACKEND_DIR%" "%ComSpec%" /k gradlew.bat bootRun
start "Port Gate Frontend - Vite" /D "%FRONTEND_DIR%" "%ComSpec%" /k npm.cmd run dev

echo AI, backend, and frontend windows have been opened.
pause
exit /b 0

:missing_python
echo ERROR: AI Python environment was not found.
echo Expected: %PYTHON_EXE%
pause
exit /b 1

:missing_frontend
echo ERROR: Frontend node_modules was not found.
echo Run npm.cmd install in the frontend folder first.
pause
exit /b 1
