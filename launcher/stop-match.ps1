param(
    [string]$ConfigPath = (Join-Path $PSScriptRoot "match-config.json")
)

$config = Get-Content $ConfigPath | ConvertFrom-Json
$temp = $config.tempDir

Write-Host "[launcher] Stopping match server..."
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

Start-Sleep -Seconds 2
if (Test-Path $temp) {
    Remove-Item -Recurse -Force $temp
    Write-Host "[launcher] Removed $temp"
}

exit 0
