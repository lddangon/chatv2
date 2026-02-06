@echo off
chcp 65001 >nul

echo ========================================
echo Build and Run Script
echo ========================================
echo.

echo [1] Setting project directory...
cd /d D:\code\Chatv2
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Failed to change directory to D:\code\Chatv2
    exit /b 1
)
echo [OK] Current directory: %CD%
echo.

echo [2] Starting Maven build...
echo Command: mvn clean package -DskipTests
echo.
mvn clean package -DskipTests

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ========================================
    echo [ERROR] Build failed!
    echo Please check the error messages above.
    echo ========================================
    pause
    exit /b 1
)

echo.
echo ========================================
echo [OK] Build completed successfully!
echo ========================================
echo.

echo [3] Starting server...
echo.

REM Поиск JAR файла в целевой директории
for /f "delims=" %%f in ('dir /b /s target\*.jar 2^>nul ^| findstr /i /v "original"') do (
    set JAR_FILE=%%f
)

if not defined JAR_FILE (
    echo [ERROR] No JAR file found in target directory!
    pause
    exit /b 1
)

echo Running: %JAR_FILE%
echo Press Ctrl+C to stop the server
echo ========================================
echo.

java -jar "%JAR_FILE%"

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ========================================
    echo [ERROR] Server terminated with error!
    echo Error code: %ERRORLEVEL%
    echo ========================================
    pause
    exit /b %ERRORLEVEL%
)
