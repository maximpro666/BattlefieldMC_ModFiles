@echo off
cd /d "C:\Users\maska\OneDrive\Desktop\BattlefieldMC_ModFiles"
echo Building TeamSystem...
call .\gradlew build 2>&1
if %errorlevel% neq 0 (
    echo.
    echo BUILD FAILED
    pause
    exit /b 1
)

REM Copy mod JAR to TLauncher (client)
copy /Y build\libs\pwp-1.0.0.jar "%APPDATA%\.tlauncher\legacy\Minecraft\game\home\Forge-1.20\mods\" >nul
echo [mod deployed to TLauncher (client)]

REM Copy mod JAR to lobby server
copy /Y build\libs\pwp-1.0.0.jar "C:\Users\maska\OneDrive\Desktop\servar\mods\" >nul
echo [mod deployed to lobby server]

echo Starting Forge lobby server (this takes ~60s)...
start "Forge Server" cmd /c "cd /d C:\Users\maska\OneDrive\Desktop\servar && run.bat"

echo Waiting for server on port 25565...
:waitloop
>nul 2>&1 timeout /t 5
>nul 2>&1 powershell -Command "try{$s=New-Object System.Net.Sockets.TcpClient;$s.Connect('127.0.0.1',25565);$s.Close();exit 0}catch{exit 1}"
if %errorlevel% neq 0 (
    echo Still waiting...
    goto waitloop
)

echo Server is ready!
echo Starting Minecraft client...
start "" "C:\Users\maska\AppData\Local\Programs\PrismLauncher\prismlauncher.exe" -l "1.20.1" -s localhost:25565
echo.
echo Done! Lobby server + client launched (no proxy).
echo /startmatch to start a game.
pause
