@echo off
chcp 65001 >nul
echo Starting ChatV2 Server...
java -jar chat-apps/chat-server-launcher/target/chat-server-launcher-1.0.0-jar-with-dependencies.jar %*
pause
