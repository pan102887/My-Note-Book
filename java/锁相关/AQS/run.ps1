#!/usr/bin/env pwsh

# AQS Demo Run Script
# Runs the compiled AQS demo, auto-compiles if needed
# Usage: .\run.ps1 [clean]

Write-Host "=== AQS Demo Runner ===" -ForegroundColor Green

# Check for clean parameter
if ($args[0] -eq "clean") {
    Write-Host "Cleaning build directory..." -ForegroundColor Yellow
    if (Test-Path "build") {
        Remove-Item -Recurse -Force "build"
        Write-Host "Build directory cleaned successfully." -ForegroundColor Green
    } else {
        Write-Host "Build directory does not exist. Nothing to clean." -ForegroundColor Cyan
    }
    exit 0
}

# Check if build directory exists
if (!(Test-Path "build")) {
    Write-Host "Build directory not found. Auto-compiling..." -ForegroundColor Yellow
    try {
        & .\compile.ps1
        if ($LASTEXITCODE -ne 0) {
            Write-Host "Compilation failed! Cannot run demo." -ForegroundColor Red
            exit 1
        }
    } catch {
        Write-Host "Error during auto-compilation: $($_.Exception.Message)" -ForegroundColor Red
        exit 1
    }
}

# Check if class files exist
if (!(Test-Path "build\AQS_Demo.class")) {
    Write-Host "AQS_Demo.class not found. Auto-compiling..." -ForegroundColor Yellow
    try {
        & .\compile.ps1
        if ($LASTEXITCODE -ne 0) {
            Write-Host "Compilation failed! Cannot run demo." -ForegroundColor Red
            exit 1
        }
    } catch {
        Write-Host "Error during auto-compilation: $($_.Exception.Message)" -ForegroundColor Red
        exit 1
    }
}

Write-Host "Starting AQS Demo..." -ForegroundColor Cyan
Write-Host ""

try {
    # Run the demo
    java -cp build AQS_Demo
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host ""
        Write-Host "=== Demo completed successfully ===" -ForegroundColor Green
    } else {
        Write-Host ""
        Write-Host "=== Demo failed to run ===" -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "Error running demo: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "Please ensure Java is installed and in PATH." -ForegroundColor Yellow
    exit 1
} 