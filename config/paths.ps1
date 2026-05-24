# ============================================
# PWP — Project Warfare Pigeo
# Central path configuration (.ps1)
# Dot-source with: . .\config\paths.ps1
# ============================================

# Project root
$script:PWP_ROOT = "C:\Users\maska\OneDrive\Desktop\BattlefieldMC_ModFiles"

# Java
$script:JAVA_HOME = "C:\Users\maska\AppData\Roaming\PrismLauncher\java\java-runtime-gamma"
$script:JAVA_CMD = "$JAVA_HOME\bin\java.exe"

# Deploy targets
$script:DEPLOY_SERVER = "C:\Users\maska\OneDrive\Desktop\servar"
$script:DEPLOY_LOBBY = "C:\Users\maska\OneDrive\Desktop\servar"
$script:DEPLOY_TEMPLATE = "C:\Users\maska\OneDrive\Desktop\match-template"
$script:DEPLOY_CLIENT = "C:\Users\maska\AppData\Roaming\PrismLauncher\instances\1.20.1\minecraft"

# Alternate client (TLauncher)
$script:DEPLOY_CLIENT_TLAUNCHER = "$env:APPDATA\.tlauncher\legacy\Minecraft\game\home\Forge-1.20\mods"

# PrismLauncher executable
$script:PRISM_LAUNCHER = "C:\Users\maska\AppData\Local\Programs\PrismLauncher\prismlauncher.exe"
