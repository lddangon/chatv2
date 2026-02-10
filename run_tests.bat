@echo off
echo ====================================
echo Running ChatV2 All Tests
echo ====================================
echo.

cd /d %~dp0

echo Running full test suite...
call mvn clean test -DskipTests=false

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ====================================
    echo All tests PASSED!
    echo ====================================
    exit /b 0
) else (
    echo.
    echo ====================================
    echo Some tests FAILED!
    echo ====================================
    exit /b 1
)
