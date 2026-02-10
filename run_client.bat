@echo off
chcp 65001 >nul
echo Starting ChatV2 Client...
java -jar chat-apps/chat-client-launcher/target/chat-client-launcher-1.0.0-jar-with-dependencies.jar %*
pause
