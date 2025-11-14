# Main script to start the Quiz Server
# Run this from the root directory: .\START_SERVER.ps1

Clear-Host

Write-Host "═══════════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host "       🎓 ONLINE QUIZ SYSTEM - STARTUP SCRIPT" -ForegroundColor Cyan
Write-Host "═══════════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host ""

# Navigate to backend
Set-Location "backend"

# Compile and run
& .\compile.ps1

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    & .\run.ps1
}
