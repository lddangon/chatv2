@echo off
echo Running all tests for ChatV2 project...
echo ====================================
cd /d %~dp0

echo.
echo [1/5] Running chat-common tests...
call mvn clean test -pl chat-common -q
if %ERRORLEVEL% NEQ 0 (
    echo FAILED: chat-common tests
    exit /b 1
)
echo PASSED: chat-common tests

echo.
echo [2/5] Running chat-server tests...
call mvn test -pl chat-server -q
if %ERRORLEVEL% NEQ 0 (
    echo FAILED: chat-server tests
    exit /b 1
)
echo PASSED: chat-server tests

echo.
echo [3/5] Running chat-client tests...
call mvn test -pl chat-client -q
if %ERRORLEVEL% NEQ 0 (
    echo FAILED: chat-client tests
    exit /b 1
)
echo PASSED: chat-client tests

echo.
echo [4/5] Running encryption plugins tests...
call mvn test -pl chat-encryption-plugins -q
if %ERRORLEVEL% NEQ 0 (
    echo FAILED: encryption plugins tests
    exit /b 1
)
echo PASSED: encryption plugins tests

echo.
echo [5/5] Running all tests combined...
call mvn test -q
if %ERRORLEVEL% NEQ 0 (
    echo FAILED: Combined tests
    exit /b 1
)

echo.
echo ====================================
echo All tests PASSED!
echo ====================================
exit /b 0
