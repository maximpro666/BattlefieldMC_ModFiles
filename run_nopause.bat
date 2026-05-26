@echo off
title PWP - Project Warfare Pigeo

:: ============================================
:: PWP - Project Warfare Pigeo
:: Studio: Pigeo Studios
:: ============================================
::
:: Commands:
::   run_nopause              - build + deploy + start server
::   run_nopause full         - build + deploy + server + client launch
::   run_nopause deploy       - build + deploy only
::   run_nopause server       - start server only (no build)
::   run_nopause client       - deploy to PrismLauncher only
::   run_nopause tlauncher    - deploy to TLauncher only
::

:: Load central config
call "%~dp0config\paths.bat"

:: ----- Mode selection -----
if /I "%1"=="deploy" goto deploy_only
if /I "%1"=="server" goto run_server
if /I "%1"=="client" goto deploy_client
if /I "%1"=="tlauncher" goto deploy_tlauncher

set "FULL_MODE=0"
if /I "%1"=="full" set "FULL_MODE=1"

echo.
echo === PWP - Project Warfare Pigeo ===
echo Studio: Pigeo Studios
echo Minecraft 1.20.1 Forge
echo.
echo Server: %DEPLOY_SERVER%
echo Client: %DEPLOY_CLIENT%
echo.

:: ----- Build mod -----
echo [1/3] Building mod...
cd /d "%PWP_ROOT%"
call gradlew build
if %errorlevel% neq 0 (
    echo [ERROR] Mod build failed
    pause
    exit /b 1
)

:: ----- Deploy -----
:deploy_only
echo [2/3] Deploying...
cd /d "%PWP_ROOT%"
call gradlew deployAll ^
    -PdeployServerDir="%DEPLOY_SERVER%" ^
    -PdeployLobbyDir="%DEPLOY_LOBBY%" ^
    -PdeployMatchTemplateDir="%DEPLOY_TEMPLATE%" ^
    -PdeployClientDir="%DEPLOY_CLIENT%"
if %errorlevel% neq 0 (
    echo [ERROR] Deploy failed
    pause
    exit /b 1
)

:: Deploy to TLauncher
if exist "%DEPLOY_CLIENT_TLAUNCHER%" (
    copy /Y "%PWP_ROOT%\build\libs\pwp-1.0.0.jar" "%DEPLOY_CLIENT_TLAUNCHER%\" >nul
    echo [tlauncher] deployed
)

if /I "%1"=="deploy" goto done

:: ----- Start server -----
:run_server
echo [3/3] Starting server...
start "Forge Server" /D "%DEPLOY_SERVER%" "%JAVA_CMD%" @user_jvm_args.txt @libraries/net/minecraftforge/forge/1.20.1-47.3.0/win_args.txt nogui

if "%FULL_MODE%"=="1" goto wait_for_server
goto done

:wait_for_server
echo Waiting for server on port 25565...
>nul 2>&1 timeout /t 5
>nul 2>&1 powershell -Command "try{$s=New-Object System.Net.Sockets.TcpClient;$s.Connect('127.0.0.1',25565);$s.Close();exit 0}catch{exit 1}"
if %errorlevel% neq 0 (
    echo Still waiting...
    goto wait_for_server
)

:: Wait for mod initialization (server_ready.flag written by PWP onServerStarted)
echo Waiting for mod initialisation...
:wait_for_ready
>nul 2>&1 timeout /t 2
if not exist "%DEPLOY_SERVER%\server_ready.flag" (
    goto wait_for_ready
)
echo Server is ready!
echo Starting Minecraft client...
start "" "%PRISM_LAUNCHER%" -l "1.20.1" -s 127.0.0.1:25565
goto done

:: ----- Deploy to PrismLauncher only -----
:deploy_client
echo Deploying to PrismLauncher...
cd /d "%PWP_ROOT%"
call gradlew deployToClient -PdeployClientDir="%DEPLOY_CLIENT%"
if %errorlevel% neq 0 (
    echo [ERROR] Client deploy failed
    pause
    exit /b 1
)
goto done

:: ----- Deploy to TLauncher only -----
:deploy_tlauncher
echo Deploying to TLauncher...
if not exist "%DEPLOY_CLIENT_TLAUNCHER%" (
    echo [ERROR] TLauncher mods folder not found
    pause
    exit /b 1
)
copy /Y "%PWP_ROOT%\build\libs\pwp-1.0.0.jar" "%DEPLOY_CLIENT_TLAUNCHER%\" >nul
echo [tlauncher] deployed
goto done

:done
echo.
echo === Done ===
if "%FULL_MODE%"=="1" (
    echo Lobby server + client launched.
    echo Use /startmatch to start a game.
)
pause
