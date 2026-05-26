param(
    [string]$SourceServer = "",
    [string]$TemplateDir = ""
)

# Load central paths
. (Join-Path $PSScriptRoot "..\config\paths.ps1")

# Use central config paths by default
if (-not $SourceServer) {
    $SourceServer = $script:DEPLOY_SERVER
}
if (-not $TemplateDir) {
    $TemplateDir = $script:DEPLOY_TEMPLATE
}

if (-not (Test-Path $SourceServer)) {
    Write-Error "Source server not found at $SourceServer"
    exit 1
}

Write-Host "Creating match template from $SourceServer"
Write-Host "Target: $TemplateDir"

if (Test-Path $TemplateDir) {
    Write-Host "Removing existing template..."
    Remove-Item -Recurse -Force $TemplateDir
}

# Copy everything except world, logs, crash-reports
$exclude = @("world", "logs", "crash-reports", "server_test.log", "server_test_err.log")
Copy-Item -Recurse "$SourceServer\*" $TemplateDir -Exclude $exclude

# Set port to 25566 in template
$props = Join-Path $TemplateDir "server.properties"
if (Test-Path $props) {
    $content = Get-Content $props
    $content = $content -replace 'server-port=\d+', 'server-port=25566'
    $content = $content -replace 'server-ip=', 'server-ip=127.0.0.1'
    $content | Set-Content $props
    Write-Host "Port set to 25566"
}

# Ensure online-mode=false in template (matches main server)
if (Test-Path $props) {
    $content = Get-Content $props
    if ($content -match 'online-mode=') {
        $content = $content -replace 'online-mode=true', 'online-mode=false'
    } else {
        $content += "`nonline-mode=false"
    }
    # Disable structure generation in template (lobby uses AutumnLobby world data)
    if ($content -match 'generate-structures=') {
        $content = $content -replace 'generate-structures=true', 'generate-structures=false'
    } else {
        $content += "`ngenerate-structures=false"
    }
    # Reset level-type to normal (overworld generator)
    if ($content -match 'level-type=') {
        $content = $content -replace 'level-type=[^\r\n]*', 'level-type=minecraft\:normal'
    } else {
        $content += "`nlevel-type=minecraft\:normal"
    }
    $content | Set-Content $props
}

# Ensure eula=true
$eula = Join-Path $TemplateDir "eula.txt"
Set-Content -Path $eula -Value "eula=true" -Force

# Add a user_jvm_args.txt if missing (basic settings)
$jvmArgsFile = Join-Path $TemplateDir "user_jvm_args.txt"
if (-not (Test-Path $jvmArgsFile)) {
    @(
        "-Xmx3G"
        "-XX:+UseG1GC"
        "-XX:+ParallelRefProcEnabled"
        "-XX:MaxGCPauseMillis=200"
        "-XX:+UnlockExperimentalVMOptions"
        "-XX:+DisableExplicitGC"
        "-XX:+AlwaysPreTouch"
        "--add-opens java.base/java.util=ALL-UNNAMED"
    ) | Out-File -FilePath $jvmArgsFile -Encoding ASCII
    Write-Host "Created user_jvm_args.txt with default settings"
}

# Copy launcher scripts to be adjacent to template (on Desktop)
$launcherTarget = Join-Path (Split-Path $TemplateDir -Parent) "launcher"
if (-not (Test-Path $launcherTarget)) {
    $projectLauncher = Join-Path $PSScriptRoot "..\launcher"
    if (Test-Path $projectLauncher) {
        New-Item -ItemType Directory -Path $launcherTarget -Force | Out-Null
        Copy-Item "$projectLauncher\*" $launcherTarget -Recurse -Force
        Write-Host "Copied launcher scripts to $launcherTarget"
    }
}

# ============================================
# Integrate AutumnLobby as the lobby world
# Instead of pwp:lobby dimension (flat world), the match server
# now uses the overworld as its lobby, populated with AutumnLobby data.
# ============================================
$autumnLobbyZip = "C:\Users\maska\OneDrive\Desktop\всячина\КАРТЫ\AutumnLobby.zip"
$templateWorldDir = Join-Path $TemplateDir "world"
$templateRegionDir = Join-Path $templateWorldDir "region"

if (Test-Path $autumnLobbyZip) {
    Write-Host "Integrating AutumnLobby lobby world..."

    # Extract AutumnLobby to a temp folder
    $tmpExtract = Join-Path $env:TEMP "autumn_lobby_extract_$(Get-Random)"
    New-Item -ItemType Directory -Path $tmpExtract -Force | Out-Null

    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $zip = [System.IO.Compression.ZipFile]::OpenRead($autumnLobbyZip)
    foreach ($entry in $zip.Entries) {
        # Entries are like "AutumnLobby/region/r.0.0.mca" → strip "AutumnLobby/" prefix
        $relativePath = $entry.FullName -replace '^AutumnLobby/', ''
        if ([string]::IsNullOrEmpty($relativePath)) { continue }
        $targetPath = Join-Path $tmpExtract $relativePath
        $targetDir = Split-Path $targetPath -Parent
        if (-not (Test-Path $targetDir)) {
            New-Item -ItemType Directory -Path $targetDir -Force | Out-Null
        }
        [System.IO.Compression.ZipFileExtensions]::ExtractToFile($entry, $targetPath, $true)
    }
    $zip.Dispose()

    # Ensure world directory exists
    if (-not (Test-Path $templateWorldDir)) {
        New-Item -ItemType Directory -Path $templateWorldDir -Force | Out-Null
    }
    if (-not (Test-Path $templateRegionDir)) {
        New-Item -ItemType Directory -Path $templateRegionDir -Force | Out-Null
    }

    # Remove old overworld region files (void/flat filler data)
    Remove-Item -Path "$templateRegionDir\*.mca" -Force -ErrorAction SilentlyContinue

    # Copy AutumnLobby region files → template world/region/
    $srcRegion = Join-Path $tmpExtract "region"
    if (Test-Path $srcRegion) {
        Copy-Item -Path "$srcRegion\*.mca" -Destination $templateRegionDir -Force
        Write-Host "  Copied $((Get-ChildItem $srcRegion -Filter '*.mca').Count) region files"
    } else {
        Write-Warning "  No region files found in AutumnLobby!"
    }

    # Copy AutumnLobby level.dat → template world/ (for correct spawn 136,68,116)
    $srcLevel = Join-Path $tmpExtract "level.dat"
    if (Test-Path $srcLevel) {
        Copy-Item -Path $srcLevel -Destination (Join-Path $templateWorldDir "level.dat") -Force
        Write-Host "  Copied level.dat (spawn: 136, 68, 116)"
    }

    # Copy other AutumnLobby data (structures, POI, entities, etc.)
    foreach ($sub in @('data', 'entities', 'poi', 'advancements', 'stats', 'playerdata', 'serverconfig')) {
        $srcSub = Join-Path $tmpExtract $sub
        if (Test-Path $srcSub) {
            $dstSub = Join-Path $templateWorldDir $sub
            Copy-Item -Path "$srcSub\*" -Destination $dstSub -Recurse -Force -ErrorAction SilentlyContinue
        }
    }

    # Clean up lobby dimension folder (pwp:lobby is no longer used)
    $lobbyDimDir = Join-Path $templateWorldDir "dimensions" "pwp" "lobby"
    if (Test-Path $lobbyDimDir) {
        Remove-Item -Recurse -Force $lobbyDimDir -ErrorAction SilentlyContinue
        Write-Host "  Removed old pwp:lobby dimension data"
    }

    # Clean up extracted temp
    Remove-Item -Recurse -Force $tmpExtract -ErrorAction SilentlyContinue

    Write-Host "AutumnLobby integrated successfully"
} else {
    Write-Warning "AutumnLobby.zip not found at $autumnLobbyZip — lobby will use default overworld data!"
    Write-Warning "Download or place AutumnLobby.zip at: $autumnLobbyZip"
}

Write-Host "Match template created at $TemplateDir"
Write-Host "You can now deploy new builds with: gradlew deployToTemplate"
Write-Host "Make sure launcher/match-config.json has the correct paths."
