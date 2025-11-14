# PowerShell script to compile Java backend
# Usage: .\compile.ps1

Write-Host "🧹 Cleaning old build..." -ForegroundColor Cyan
if (Test-Path "out") {
    Remove-Item -Recurse -Force "out"
}
New-Item -ItemType Directory -Force -Path "out" | Out-Null

Write-Host "📦 Compiling Java sources..." -ForegroundColor Cyan

# Find all .java files recursively
$sourceFiles = Get-ChildItem -Path "src" -Filter "*.java" -Recurse | Select-Object -ExpandProperty FullName

if ($sourceFiles.Count -eq 0) {
    Write-Host "❌ No Java files found!" -ForegroundColor Red
    exit 1
}

Write-Host "   Found $($sourceFiles.Count) Java files" -ForegroundColor Gray

# Compile all files
$classpath = ".;lib/gson-2.10.1.jar"
javac -d out -cp $classpath @sourceFiles

if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ Compilation successful!" -ForegroundColor Green
    Write-Host ""
    Write-Host "📂 Compiled classes are in: out/" -ForegroundColor Cyan
    Write-Host "🚀 Run the server with: .\run.ps1" -ForegroundColor Cyan
} else {
    Write-Host "❌ Compilation failed!" -ForegroundColor Red
    exit 1
}
