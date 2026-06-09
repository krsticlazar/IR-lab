@echo off
setlocal EnableExtensions

set "SCRIPT_DIR=%~dp0"
for %%I in ("%SCRIPT_DIR%..") do set "PROJECT_ROOT=%%~fI"

echo Starting IR Lab 4 backend and frontend...
echo Backend:  http://localhost:5044
echo Frontend: http://localhost:5173
echo Elasticsearch must already be running on http://localhost:9200
echo.

if not exist "%PROJECT_ROOT%\backend\Lab4.Api.csproj" (
    echo Backend project was not found.
    echo Expected: %PROJECT_ROOT%\backend\Lab4.Api.csproj
    pause
    exit /b 1
)

if not exist "%PROJECT_ROOT%\frontend\package.json" (
    echo Frontend package.json was not found.
    echo Expected: %PROJECT_ROOT%\frontend\package.json
    pause
    exit /b 1
)

where dotnet >nul 2>nul
if errorlevel 1 (
    echo dotnet was not found. Run scripts\init_env.cmd first.
    pause
    exit /b 1
)

where npm.cmd >nul 2>nul
if errorlevel 1 (
    echo npm was not found. Run scripts\init_env.cmd first.
    pause
    exit /b 1
)

start "IR Lab 4 Backend" /D "%PROJECT_ROOT%\backend" cmd /k "dotnet run"
start "IR Lab 4 Frontend" /D "%PROJECT_ROOT%\frontend" cmd /k "npm.cmd run dev"

echo Two CMD windows were opened.
echo Keep both windows open while using the application.
timeout /t 3 /nobreak >nul
exit /b 0
