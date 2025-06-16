@echo off
REM AQS Demo Run Script
REM Runs the compiled AQS demo, auto-compiles if needed
REM Usage: run.bat [clean]

echo === AQS Demo Runner ===

REM Check for clean parameter
if "%1"=="clean" (
    echo Cleaning build directory...
    if exist "build" (
        rmdir /s /q build
        echo Build directory cleaned successfully.
    ) else (
        echo Build directory does not exist. Nothing to clean.
    )
    exit /b 0
)

REM Check if build directory exists
if not exist "build" (
    echo Build directory not found. Auto-compiling...
    call compile.bat
    if %errorlevel% neq 0 (
        echo Compilation failed! Cannot run demo.
        exit /b 1
    )
)

REM Check if class files exist
if not exist "build\AQS_Demo.class" (
    echo AQS_Demo.class not found. Auto-compiling...
    call compile.bat
    if %errorlevel% neq 0 (
        echo Compilation failed! Cannot run demo.
        exit /b 1
    )
)

echo Starting AQS Demo...
echo.

REM Run the demo
java -cp build AQS_Demo

if %errorlevel% equ 0 (
    echo.
    echo === Demo completed successfully ===
) else (
    echo.
    echo === Demo failed to run ===
    exit /b 1
) 