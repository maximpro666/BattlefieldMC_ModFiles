param(
    [string]$SourceServer = "C:\Users\maska\OneDrive\Desktop\servar",
    [string]$TemplateDir = "C:\Users\maska\OneDrive\Desktop\match-template"
)

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
Copy-Item -Recurse $SourceServer $TemplateDir -Exclude $exclude

# Set port to 25566 in template
$props = Join-Path $TemplateDir "server.properties"
if (Test-Path $props) {
    $content = Get-Content $props
    $content = $content -replace 'server-port=\d+', 'server-port=25566'
    $content = $content -replace 'server-ip=', 'server-ip=127.0.0.1'
    $content | Set-Content $props
    Write-Host "Port set to 25566"
}

# Ensure eula=true
$eula = Join-Path $TemplateDir "eula.txt"
Set-Content -Path $eula -Value "eula=true" -Force

# Clean mods (keep only teamsystem, TACZ, SuperbWarfare, and essentials)
# Actually keep all mods - they should be same as main server

Write-Host "Match template created at $TemplateDir"
Write-Host "You can now deploy new builds with: gradlew deployToTemplate"
