@echo off
chcp 65001 >nul
title PWP - Project Warfare Pigeo

:: ============================================
:: PWP — Project Warfare Pigeo
:: Studio: Pigeo Studios
:: ============================================
:: 
:: Команды:
::   run_nopause         — сборка + деплой + запуск сервера
::   run_nopause deploy  — только сборка + деплой
::   run_nopause server  — только запуск сервера (без сборки)
::   run_nopause client  — только деплой в клиент
:: 

set JAVA_HOME=C:\Users\maska\AppData\Roaming\PrismLauncher\java\java-runtime-gamma
set JAVA_CMD="%JAVA_HOME%\bin\java.exe"

:: ----- Пути развёртывания -----
set DEPLOY_SERVER=C:\Users\maska\OneDrive\Desktop\servar
set DEPLOY_LOBBY=C:\Users\maska\OneDrive\Desktop\servar
set DEPLOY_TEMPLATE=C:\Users\maska\OneDrive\Desktop\match-template
set DEPLOY_CLIENT=C:\Users\maska\AppData\Roaming\PrismLauncher\instances\1.20.1\minecraft

:: ----- Режим -----
if /I "%1"=="deploy" goto deploy_only
if /I "%1"=="server" goto run_server
if /I "%1"=="client" goto deploy_client

echo.
echo === PWP — Project Warfare Pigeo ===
echo Studio: Pigeo Studios
echo Minecraft 1.20.1 Forge
echo.
echo Server: %DEPLOY_SERVER%
echo Client: %DEPLOY_CLIENT%
echo.

:: ----- Сборка -----
echo [1/3] Сборка...
call gradlew build
if %errorlevel% neq 0 (
    echo [ERROR] Сборка не удалась
    pause
    exit /b 1
)

:: ----- Деплой -----
:deploy_only
echo [2/3] Деплой...
call gradlew deployAll ^
    -PdeployServerDir="%DEPLOY_SERVER%" ^
    -PdeployLobbyDir="%DEPLOY_LOBBY%" ^
    -PdeployMatchTemplateDir="%DEPLOY_TEMPLATE%" ^
    -PdeployClientDir="%DEPLOY_CLIENT%"
if %errorlevel% neq 0 (
    echo [ERROR] Деплой не удался
    pause
    exit /b 1
)
if /I "%1"=="deploy" goto done

:: ----- Запуск сервера -----
:run_server
echo [3/3] Запуск сервера...
cd /d "%DEPLOY_SERVER%"
%JAVA_CMD% @user_jvm_args.txt @libraries/net/minecraftforge/forge/1.20.1-47.3.0/win_args.txt nogui
cd /d "%~dp0"
goto done

:: ----- Только деплой в клиент -----
:deploy_client
echo Деплой в клиент...
call gradlew deployToClient -PdeployClientDir="%DEPLOY_CLIENT%"
if %errorlevel% neq 0 (
    echo [ERROR] Деплой не удался
    pause
    exit /b 1
)
goto done

:done
echo.
echo === Готово ===
pause
