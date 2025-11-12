# PowerShell script to run the Quiz Server
# Usage: .\run.ps1

# Check if compiled
if (-not (Test-Path "out/server/QuizServer.class")) {
    Write-Host "⚠️  Server not compiled yet. Running compile.ps1 first..." -ForegroundColor Yellow
    Write-Host ""
    & .\compile.ps1
    if ($LASTEXITCODE -ne 0) {
        Write-Host "❌ Compilation failed. Cannot start server." -ForegroundColor Red
        exit 1
    }
    Write-Host ""
}

Write-Host "═══════════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host "           🎯 ONLINE QUIZ SYSTEM - SERVER" -ForegroundColor Cyan
Write-Host "═══════════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host ""

# Check Java version
$javaVersion = java -version 2>&1 | Select-String "version" | ForEach-Object { $_.ToString() }
Write-Host "☕ Java Version: $javaVersion" -ForegroundColor Gray
Write-Host ""

Write-Host "🚀 Starting Quiz Server..." -ForegroundColor Green
Write-Host ""

# Run the server
java -cp "out;lib/gson-2.10.1.jar" server.QuizServer
