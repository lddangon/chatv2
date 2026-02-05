# Итоги преобразования Java records в классы

## Что было сделано:

1. **ServerProperties.java**: Преобразованы 5 record в классы:
   - ServerConfig -> class с полями host, port, name
   - UdpConfig -> class с полями enabled, multicastAddress, port, broadcastInterval
   - DatabaseConfig -> class с полями path, connectionPoolSize
   - EncryptionConfig -> class с полями required, defaultPlugin, rsaKeySize, aesKeySize
   - SessionConfig -> class с полями tokenExpirationSeconds, refreshTokenExpirationDays

2. **ServerConfig.java** (в пакете core): Преобразован record в class с полями:
   - host, port, name, databasePath, connectionPoolSize
   - encryptionRequired, rsaKeySize, aesKeySize
   - udpMulticastAddress, udpMulticastPort, udpEnabled
   - sessionExpirationSeconds, tokenExpirationSeconds

3. **Исправлены все вызовы методов**:
   - Заменены вызовы record методов (например, `config.host()`) на вызовы геттеров (например, `config.getHost()`)
   - Обновлены методы в классах-использователях:
     - ServerDiscoveryBroadcaster.java
     - SessionManager.java
     - ChatServer.java
     - BootstrapFactory.java
     - DashboardController.java
     - ServerAdminApp.java
     - DashboardControllerTest.java
     - ServerConfigTest.java

4. **Сохранена логика валидации**: Все проверки из компактных конструкторов record перенесены в конструкторы классов.

## Результат:

- Код теперь компилируется с Java 17 (не требует preview features)
- Все классы конфигурации работают как раньше, но используют обычные классы вместо records
- Сохранена вся функциональность, включая валидацию и значения по умолчанию
- Все тесты и остальной код проекта обновлены для использования новых геттеров

Преобразование успешно завершено!