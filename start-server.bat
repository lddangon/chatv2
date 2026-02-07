@echo off
echo Starting ChatV2 Server with encryption plugin diagnostics...
echo.

java -jar "chat-apps/chat-server-launcher/target/chat-server-launcher-1.0.0-jar-with-dependencies.jar" --no-gui --port 8083 --database ./test-diagnostics-server.db

echo.
echo Server process terminated.
pause