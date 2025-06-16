#!/usr/bin/env pwsh

# AQS Java Compilation Script
# Compiles Java files and outputs class files to the build folder

Write-Host "=== AQS Java Compilation Script ===" -ForegroundColor Green

# Create build directory if it doesn't exist
if (!(Test-Path "build")) {
    Write-Host "Creating build directory..." -ForegroundColor Yellow
    New-Item -ItemType Directory -Path "build" -Force | Out-Null
}

# Compile Java files
Write-Host "Compiling Java files..." -ForegroundColor Yellow

try {
    javac -d build AQS_Demo.java
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "Compilation successful!" -ForegroundColor Green
        Write-Host "Class files created in build/ directory:" -ForegroundColor Cyan
        
        # List compiled class files
        $classFiles = Get-ChildItem -Path "build" -Filter "*.class" -Recurse
        foreach ($file in $classFiles) {
            Write-Host "  - $($file.Name)" -ForegroundColor White
        }
        
        Write-Host "`nTo run the demo:" -ForegroundColor Yellow
        Write-Host "  java -cp build AQS_Demo" -ForegroundColor Cyan
    } else {
        Write-Host "Compilation failed! Please ensure Java is installed and in PATH." -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "Error during compilation: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "Please ensure Java is installed and in PATH." -ForegroundColor Red
    exit 1
}

Write-Host "`n=== Compilation Complete ===" -ForegroundColor Green 