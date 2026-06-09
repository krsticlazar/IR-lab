@echo off
setlocal EnableExtensions

set "SCRIPT_DIR=%~dp0"
for %%I in ("%SCRIPT_DIR%..") do set "PROJECT_ROOT=%%~fI"

echo ================================================
echo IR Lab 4 - environment setup
echo ================================================
echo Project root: %PROJECT_ROOT%
echo.

where winget >nul 2>nul
if errorlevel 1 (
    set "HAS_WINGET=0"
) else (
    set "HAS_WINGET=1"
)

where dotnet >nul 2>nul
if errorlevel 1 (
    if "%HAS_WINGET%"=="1" (
        echo Installing .NET SDK 8...
        winget install --id Microsoft.DotNet.SDK.8 -e --accept-package-agreements --accept-source-agreements
    ) else (
        echo .NET SDK 8 was not found and winget is not available.
        echo Install .NET SDK 8 manually, then run this script again.
        goto fail
    )
) else (
    echo .NET SDK found:
    dotnet --version
)

where node >nul 2>nul
if errorlevel 1 (
    if "%HAS_WINGET%"=="1" (
        echo Installing Node.js LTS...
        winget install --id OpenJS.NodeJS.LTS -e --accept-package-agreements --accept-source-agreements
    ) else (
        echo Node.js was not found and winget is not available.
        echo Install Node.js LTS manually, then run this script again.
        goto fail
    )
) else (
    echo Node.js found:
    node --version
)

where npm.cmd >nul 2>nul
if errorlevel 1 (
    echo npm was not found. If Node.js was just installed, close this window and run init_env.cmd again.
    goto fail
) else (
    echo npm found:
    call npm.cmd --version
)

where docker >nul 2>nul
if errorlevel 1 (
    if "%HAS_WINGET%"=="1" (
        echo Docker was not found. Installing Docker Desktop...
        winget install --id Docker.DockerDesktop -e --accept-package-agreements --accept-source-agreements
        echo If Docker Desktop was just installed, restart Windows before starting Elasticsearch.
    ) else (
        echo Docker was not found. Install Docker Desktop manually if it is not already installed.
        echo This script will continue because backend and frontend can still be prepared.
    )
) else (
    echo Docker found:
    docker --version
)

echo.
echo Restoring and building backend...
cd /d "%PROJECT_ROOT%\backend"
dotnet restore
if errorlevel 1 goto fail
dotnet build
if errorlevel 1 goto fail

echo.
echo Installing frontend packages...
cd /d "%PROJECT_ROOT%\frontend"
call npm.cmd install
if errorlevel 1 goto fail

echo.
echo Environment setup finished.
echo Next steps:
echo 1. Start Elasticsearch using scripts\docker_notes.txt.
echo 2. Run scripts\start_app.cmd.
pause
exit /b 0

:fail
echo.
echo Setup failed. Read the error above and run init_env.cmd again after fixing it.
pause
exit /b 1
