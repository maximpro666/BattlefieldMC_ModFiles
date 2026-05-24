@echo off
:: ============================================
:: PWP ? Project Warfare Pigeo
:: Central path configuration (.bat)
:: Source with: call "config\paths.bat"
:: ============================================

:: Project root
set "PWP_ROOT=C:\Users\maska\OneDrive\Desktop\BattlefieldMC_ModFiles"

:: Java
set "JAVA_HOME=C:\Users\maska\AppData\Roaming\PrismLauncher\java\java-runtime-gamma"
set "JAVA_CMD=%JAVA_HOME%\bin\java.exe"

:: Deploy targets
set "DEPLOY_SERVER=C:\Users\maska\OneDrive\Desktop\servar"
set "DEPLOY_LOBBY=C:\Users\maska\OneDrive\Desktop\servar"
set "DEPLOY_TEMPLATE=C:\Users\maska\OneDrive\Desktop\match-template"
set "DEPLOY_CLIENT=C:\Users\maska\AppData\Roaming\PrismLauncher\instances\1.20.1\minecraft"

:: Alternate client (TLauncher)
set "DEPLOY_CLIENT_TLAUNCHER=%APPDATA%\.tlauncher\legacy\Minecraft\game\home\Forge-1.20\mods"

:: PrismLauncher executable
set "PRISM_LAUNCHER=C:\Users\maska\AppData\Local\Programs\PrismLauncher\prismlauncher.exe"
