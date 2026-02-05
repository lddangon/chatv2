@echo off
echo Running ChatManager tests...
echo.

REM Set classpath
set CLASSPATH=.
REM Add all JAR files from lib directory to classpath
for /r "target\classes" %%a in (*.class) do call :addtocp "%%a"
for /r "target\test-classes" %%a in (*.class) do call :addtocp "%%a"

REM Add dependencies
for /r "target" %%a in (*.jar) do call :addtocp "%%a"

echo Classpath: %CLASSPATH%
echo.

REM Run the test
java -cp %CLASSPATH% org.junit.platform.console.ConsoleLauncher --select-class=com.chatv2.server.manager.ChatManagerTest

goto :eof

:addtocp
set CLASSPATH=%CLASSPATH%;%~1
goto :eof