# Script to open the frontend pages in browser
# Run this in a SECOND terminal after starting the server

Clear-Host

Write-Host "═══════════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host "          🌐 OPENING QUIZ SYSTEM IN BROWSER" -ForegroundColor Cyan
Write-Host "═══════════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host ""

# Check if server is running
try {
    $response = Invoke-WebRequest -Uri "http://localhost:8080/api/status" -TimeoutSec 2 -ErrorAction Stop
    Write-Host "✅ Server is running!" -ForegroundColor Green
} catch {
    Write-Host "⚠️  Server is not running yet!" -ForegroundColor Yellow
    Write-Host "   Please start the server first with: .\START_SERVER.ps1" -ForegroundColor Yellow
    Write-Host ""
    $confirm = Read-Host "Open browser anyway? (y/n)"
    if ($confirm -ne "y") {
        exit 0
    }
}

Write-Host ""
Write-Host "📖 Opening pages..." -ForegroundColor Cyan
Write-Host ""

# Open different pages based on user choice
Write-Host "Select which page to open:" -ForegroundColor Yellow
Write-Host "  1) Student Portal (Take Quiz)" -ForegroundColor White
Write-Host "  2) Admin Dashboard (Manage Quiz)" -ForegroundColor White
Write-Host "  3) Leaderboard (View Results)" -ForegroundColor White
Write-Host "  4) All Pages" -ForegroundColor White
Write-Host ""

$choice = Read-Host "Enter choice (1-4)"

switch ($choice) {
    "1" {
        Write-Host "🎓 Opening Student Portal..." -ForegroundColor Green
        Start-Process "http://localhost:8080/pages/student.html"
    }
    "2" {
        Write-Host "👨‍💼 Opening Admin Dashboard..." -ForegroundColor Green
        Start-Process "http://localhost:8080/pages/admin.html"
    }
    "3" {
        Write-Host "🏆 Opening Leaderboard..." -ForegroundColor Green
        Start-Process "http://localhost:8080/pages/leaderboard.html"
    }
    "4" {
        Write-Host "🌐 Opening all pages..." -ForegroundColor Green
        Start-Sleep -Milliseconds 500
        Start-Process "http://localhost:8080/pages/student.html"
        Start-Sleep -Milliseconds 500
        Start-Process "http://localhost:8080/pages/admin.html"
        Start-Sleep -Milliseconds 500
        Start-Process "http://localhost:8080/pages/leaderboard.html"
    }
    default {
        Write-Host "❌ Invalid choice. Opening Student Portal by default..." -ForegroundColor Yellow
        Start-Process "http://localhost:8080/pages/student.html"
    }
}

Write-Host ""
Write-Host "✅ Browser opened!" -ForegroundColor Green
Write-Host ""
Write-Host "📝 Quick Guide:" -ForegroundColor Cyan
Write-Host "   1. Students connect at: http://localhost:8080/pages/student.html" -ForegroundColor Gray
Write-Host "   2. Admin starts quiz at: http://localhost:8080/pages/admin.html" -ForegroundColor Gray
Write-Host "   3. View results at: http://localhost:8080/pages/leaderboard.html" -ForegroundColor Gray
Write-Host ""
