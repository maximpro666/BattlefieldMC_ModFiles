param(
    [string]$ConfigPath = (Join-Path $PSScriptRoot "match-config.json"),
    [string]$LobbyRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\run")).Path
)

# Check config exists
if (-not (Test-Path $ConfigPath)) {
    Write-Error "[launcher] match-config.json not found at $ConfigPath"
    exit 1
}

$config = Get-Content $ConfigPath | ConvertFrom-Json
$template = $config.templateDir
$temp = $config.tempDir
$port = $config.matchPort
$timeout = [int]$config.startTimeoutSeconds

# Validate template exists
if (-not (Test-Path $template)) {
    Write-Error "[launcher] Template directory not found at $template"
    exit 1
}

Write-Host "[launcher] Syncing template to temp (robocopy)..."
robocopy "$template" "$temp" /MIR /NJH /NJS /NDL /NP /R:1 /W:1
if ($LASTEXITCODE -ge 8) {
    Write-Warning "[launcher] Robocopy reported errors, continuing anyway..."
}

# Copy map sources and pool config from lobby server if available
$lobbySources = Join-Path $LobbyRoot "pwp_sources"
$lobbyMapsJson = Join-Path $LobbyRoot "pwp_maps.json"
$matchSources = Join-Path $temp "pwp_sources"
$matchMapsJson = Join-Path $temp "pwp_maps.json"

if (Test-Path $lobbySources) {
    Write-Host "[launcher] Copying map sources from lobby..."
    robocopy "$lobbySources" "$matchSources" /MIR /NJH /NJS /NDL /NP /R:1 /W:1
}
if (Test-Path $lobbyMapsJson) {
    Copy-Item -Force $lobbyMapsJson $matchMapsJson
    Write-Host "[launcher] Copied pwp_maps.json from lobby"
}

$serverProps = Join-Path $temp "server.properties"
if (Test-Path $serverProps) {
    (Get-Content $serverProps) -replace 'server-port=\d+', "server-port=$port" | Set-Content $serverProps
    Write-Host "[launcher] Port set to $port"
}

Remove-Item -Recurse -Force (Join-Path $temp "world") -ErrorAction SilentlyContinue
Remove-Item -Recurse -Force (Join-Path $temp "logs") -ErrorAction SilentlyContinue
Remove-Item -Recurse -Force (Join-Path $temp "crash-reports") -ErrorAction SilentlyContinue
Remove-Item -Force (Join-Path $temp ".ready") -ErrorAction SilentlyContinue

# Wait for port to be free (old process might still be shutting down)
Write-Host "[launcher] Waiting for port $port to be free..."
$portFree = $false
for ($i = 0; $i -lt 30; $i++) {
    $conn = Test-NetConnection -ComputerName 127.0.0.1 -Port $port -WarningAction SilentlyContinue -InformationLevel Quiet 2>$null
    if (-not $conn) { $portFree = $true; break }
    Start-Sleep -Seconds 1
}
if (-not $portFree) {
    Write-Warning "[launcher] Port $port still in use after 30s, attempting to start anyway..."
}

$argsFile = "libraries/net/minecraftforge/forge/1.20.1-47.3.0/win_args.txt"
$fullArgsFile = Join-Path $temp $argsFile.Replace("/", "\")

if (-not (Test-Path $fullArgsFile)) {
    Write-Error "[launcher] win_args.txt not found at $fullArgsFile"
    exit 1
}

Write-Host "[launcher] Starting Forge server..."

# Disable NightConfig file watcher to prevent ConcurrentModificationException
# from ModernFix's NightConfigWatchThrottler during class loading
$jvmOpts = @(
    "@user_jvm_args.txt"
    "-Dpwp.mode=match"
    "-Dforge.configWatcher=false"
    "-Dcom.electronwill.nightconfig.core.file.FileWatcher.disabled=true"
    "@$argsFile"
    "nogui"
)

$proc = Start-Process -FilePath "java" -ArgumentList $jvmOpts -WorkingDirectory $temp -NoNewWindow -PassThru -WindowStyle Hidden

$logFile = Join-Path $temp "logs\latest.log"
$elapsed = 0
$doneRegex = 'Done\s*\('
$countdownRegex = 'Game starts in \d+s'
$doneFound = $false

Write-Host "[launcher] Waiting for server to start..."
while ($elapsed -lt $timeout) {
    if (Test-Path $logFile) {
        $lines = Get-Content $logFile -Tail 20
        foreach ($line in $lines) {
            if (-not $doneFound -and $line -match $doneRegex) {
                $doneFound = $true
                Write-Host "[launcher] Server loaded, waiting for game countdown..."
            }
            if ($doneFound -and $line -match $countdownRegex) {
                $procId = $proc.Id
                Start-Sleep -Seconds 2
                Set-Content -Path (Join-Path $temp ".ready") -Value $procId
                Write-Host $line
                Write-Host "[launcher] Server ready! PID: $procId"
                exit 0
            }
        }
    } else {
        Write-Host "[launcher] Waiting for log file..."
    }
    Start-Sleep -Seconds 2
    $elapsed += 2
}

Write-Error "[launcher] Timeout: server failed to start in $timeout seconds"
Stop-Process -Id $proc.Id -Force -ErrorAction SilentlyContinue
exit 1
