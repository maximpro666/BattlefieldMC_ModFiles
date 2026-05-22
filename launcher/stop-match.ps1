param(
    [string]$ConfigPath = (Join-Path $PSScriptRoot "match-config.json")
)

if (-not (Test-Path $ConfigPath)) {
    Write-Warning "[launcher] match-config.json not found at $ConfigPath, skipping stop"
    exit 0
}

$config = Get-Content $ConfigPath | ConvertFrom-Json
$temp = $config.tempDir

Write-Host "[launcher] Stopping match server..."

# Kill Java processes on the match port
$port = $config.matchPort
$javaProcs = Get-Process -Name "java" -ErrorAction SilentlyContinue | Where-Object {
    $_.CommandLine -match "server-port=$port" -or $_.CommandLine -match "$port"
}
foreach ($p in $javaProcs) {
    try {
        Stop-Process -Id $p.Id -Force -ErrorAction Stop
        Write-Host "[launcher] Killed java process $($p.Id) on port $port"
    } catch {
        Write-Host "[launcher] Process $($p.Id) already exited"
    }
}

# Also try the .ready file
$readyFile = Join-Path $temp ".ready"
if (Test-Path $readyFile) {
    $procId = Get-Content $readyFile
    try {
        Stop-Process -Id $procId -Force -ErrorAction Stop
        Write-Host "[launcher] Process $procId stopped"
    } catch {
        Write-Host "[launcher] Process $procId already exited"
    }
}

# Clean up match_ready.flag files in all match directories
Get-ChildItem -Path (Split-Path $temp -Parent) -Directory -Filter "match-*" | ForEach-Object {
    $flag = Join-Path $_.FullName "match_ready.flag"
    Remove-Item -Path $flag -Force -ErrorAction SilentlyContinue
}

# Clean up launcher-level flags
Remove-Item -Path "$PSScriptRoot\match_cycle_done.flag" -Force -ErrorAction SilentlyContinue
Remove-Item -Path "$PSScriptRoot\match_active.flag" -Force -ErrorAction SilentlyContinue

Start-Sleep -Seconds 2
if (Test-Path $temp) {
    Remove-Item -Recurse -Force $temp -ErrorAction SilentlyContinue
    Write-Host "[launcher] Removed $temp"
}

exit 0
