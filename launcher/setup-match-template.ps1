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

Write-Host "Match template created at $TemplateDir"
Write-Host "You can now deploy new builds with: gradlew deployToTemplate"
Write-Host "Make sure launcher/match-config.json has the correct paths."
