@echo off
REM AQS Java Compilation Script
REM Compiles Java files and outputs class files to the build folder

echo === AQS Java Compilation Script ===

REM Create build directory if it doesn't exist
if not exist "build" (
    echo Creating build directory...
    mkdir build
)

REM Compile Java files
echo Compiling Java files...
javac -d build AQS_Demo.java

if %errorlevel% equ 0 (
    echo Compilation successful!
    echo Class files created in build/ directory:
    
    REM List compiled class files
    dir /b build\*.class
    
    echo.
    echo To run the demo:
    echo   java -cp build AQS_Demo
) else (
    echo Compilation failed! Please ensure Java is installed and in PATH.
    exit /b 1
)

echo.
echo === Compilation Complete === 