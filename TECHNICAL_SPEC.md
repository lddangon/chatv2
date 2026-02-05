# Техническое задание для разработчика (ТЗ)

## Проект: ChatV2 - Профессиональное локальное чат-приложение

### Обзор
Разработать клиент-серверное чат-приложение на Java 21 с использованием Netty, JavaFX, H2 Database и AES-256 шифрования. Приложение должно поддерживать обнаружение серверов в локальной сети через UDP broadcast, профили пользователей с аватарами, личные и групповые чаты, а также иметь графический интерфейс для клиента и администрирования сервера.

---

## Требования к реализации по модулям

### Модуль 1: chat-common (Общий модуль)

#### Задачи:
1. **Модели данных (model/)**
   - `UserProfile` - профиль пользователя (UUID, username, passwordHash, salt, fullName, avatar, bio, status)
   - `Chat` - метаданные чата (UUID, chatType, name, description, ownerId)
   - `Message` - сообщение (UUID, chatId, senderId, content, messageType, timestamp)
   - `Session` - сессия (UUID, userId, token, expiresAt)
   - Enum: `ChatType` (PRIVATE, GROUP), `UserStatus` (ONLINE, OFFLINE, AWAY), `MessageType` (TEXT, IMAGE, FILE)

2. **Протокол (protocol/)**
   - `MessageType` - enum всех типов сообщений (AUTH, CHAT, USER, SESSION и т.д.)
   - `ChatMessage` - бинарное сообщение (Header, Body, Payload, Checksum)
   - `MessageCodec` - энкодер/декодер для Netty
   - `ProtocolConstants` - константы протокола (MAGIC_NUMBER, VERSION, MAX_PAYLOAD_SIZE)

3. **Криптография (crypto/)**
   - `KeyManager` - генерация AES-256 и RSA-4096 ключей
   - `CryptoUtils` - утилиты хеширования (Argon2), генерации соли, UUID
   - `EncryptionResult` - результат шифрования (ciphertext, IV, tag)
   - Исключения: `EncryptionException`, `DecryptionException`

4. **Исключения (exception/)**
   - `ChatException` - базовый класс
   - `AuthenticationException` - ошибки аутентификации
   - `AuthorizationException` - ошибки авторизации
   - `NetworkException` - сетевые ошибки
   - `ValidationException` - ошибки валидации

5. **Утилиты (util/)**
   - `IdGenerator` - генерация UUID
   - `DateUtils` - работа с датами/временем
   - `ByteUtils` - операции с байтовыми массивами
   - `Constants` - константы приложения

**Требования:**
- Все модели использовать Java 21 records
- Реализовать Jackson JSON сериализацию/десериализацию
- Unit тесты на все классы (минимум 80% покрытие)

---

### Модуль 2: chat-encryption-plugins (Плагины шифрования)

#### Подмодуль 2.1: chat-encryption-api

**Задачи:**
1. `EncryptionPlugin` - интерфейс плагина (SPI)
   ```java
   String getName();
   String getVersion();
   EncryptionAlgorithm getAlgorithm();
   CompletableFuture<byte[]> encrypt(byte[] plaintext, KeySpec keySpec);
   CompletableFuture<byte[]> decrypt(byte[] ciphertext, KeySpec keySpec);
   CompletableFuture<KeySpec> generateKey();
   ```
2. `EncryptionAlgorithm` - спецификация алгоритма
3. `KeySpec` - спецификация ключа
4. `EncryptionContext` - контекст шифрования

**Требования:**
- Использовать ServiceLoader для SPI
- Поддерживать асинхронные операции через CompletableFuture
- JavaDoc документация для всех методов

#### Подмодуль 2.2: chat-encryption-aes

**Задачи:**
1. `AesEncryptionPlugin` - реализация плагина AES-256
   - Использовать AES/GCM/NoPadding
   - 256-битные ключи
   - 128-битный IV (случайный для каждого сообщения)
   - 128-битный authentication tag

2. `AesKeyGenerator` - генерация ключей
3. `AesEncryptor` - шифрование
4. `AesDecryptor` - расшифрование

**Требования:**
- Vector testing с известными векторами шифрования
- SecureRandom для генерации IV
- Обработка всех исключений безопасности

#### Подмодуль 2.3: chat-encryption-rsa

**Задачи:**
1. `RsaEncryptionPlugin` - реализация плагина RSA
   - RSA/ECB/OAEPWithSHA-256AndMGF1Padding
   - 4096-битные ключи
   - Использовать Bouncy Castle

2. `RsaKeyGenerator` - генерация ключей
3. `RsaEncryptor` - шифрование
4. `RsaDecryptor` - расшифрование

**Требования:**
- Экспорт ключей в PEM формат
- Импорт ключей из PEM формата
- Хранение приватных ключей в Java KeyStore

#### Подмодуль 2.4: EncryptionPluginManager

**Задачи:**
1. `EncryptionPluginLoader` - загрузка плагинов через SPI
2. `EncryptionPluginManager` - управление плагинами
   - Загрузка из classpath
   - Динамическая загрузка из JAR файлов
   - Установка активного плагина

**Требования:**
- META-INF/services/com.chatv2.encryption.api.EncryptionPlugin
- Поддержка горячей замены плагинов

---

### Модуль 3: chat-server (Сервер)

#### Задачи:

1. **Ядро сервера (core/)**
   - `ChatServer` - основной класс сервера
     - Инициализация Netty ServerBootstrap
     - Настройка EventLoopGroup с виртуальными потоками
     - Методы: start(), stop(), getState(), getConnectedClients()
     - Graceful shutdown с таймаутом

   - `ServerConfig` - конфигурация сервера (record)
     - host, port, name
     - database settings
     - encryption settings
     - UDP broadcast settings

   - `BootstrapFactory` - factory для конфигурации bootstrap

2. **Менеджеры бизнес-логики (manager/)**

   **UserManager:**
   - Регистрация пользователей
   - Аутентификация (username/password)
   - Получение/обновление профиля
   - Поиск пользователей
   - Обновление статуса

   **SessionManager:**
   - Создание сессий
   - Генерация JWT токенов
   - Валидация токенов
   - Обновление токенов
   - Завершение сессий

   **ChatManager:**
   - Создание личных чатов
   - Создание групповых чатов
   - Добавление/удаление участников
   - Получение списка чатов пользователя
   - Обновление информации о чате

   **MessageManager:**
   - Отправка сообщений
   - Получение истории сообщений
   - Отметки о прочтении
   - Получение непрочитанных сообщений

3. **Netty Handlers (handler/)**

   - `ServerInitializer` - инициализация pipeline
   - `AuthHandler` - обработка AUTH_* сообщений
   - `ChatHandler` - обработка CHAT_* и MESSAGE_* сообщений
   - `EncryptionHandler` - шифрование/дешифрование
   - `SessionHandler` - валидация сессий
   - `ExceptionHandler` - глобальная обработка ошибок

4. **База данных (storage/)**

   - `DatabaseManager` - управление соединениями
   - `SchemaInitializer` - инициализация схемы БД
   - `UserRepository` - CRUD для пользователей
   - `SessionRepository` - CRUD для сессий
   - `ChatRepository` - CRUD для чатов
   - `MessageRepository` - CRUD для сообщений

   **Схема БД:**
   - users (userId, username, passwordHash, salt, fullName, avatar, bio, status, createdAt, updatedAt)
   - sessions (sessionId, userId, token, expiresAt, createdAt, lastAccessedAt)
   - chats (chatId, chatType, name, description, ownerId, avatar, createdAt, updatedAt)
   - chat_participants (id, chatId, userId, joinedAt, role)
   - messages (messageId, chatId, senderId, content, messageType, replyToId, createdAt, editedAt, deletedAt)
   - message_read_receipts (receiptId, messageId, userId, readAt)

5. **GUI для администрирования (gui/)**

   **JavaFX Application:**
   - `ServerAdminApp` - главное приложение
   - `DashboardController` - статистика сервера
   - `UserController` - управление пользователями
   - `ChatController` - управление чатами
   - `LogViewerController` - просмотр логов

   **FXML Views:**
   - DashboardView.fxml - дашборд с графиками
   - UserManagementView.fxml - CRUD пользователей
   - ChatManagementView.fxml - CRUD чатов
   - LogViewerView.fxml - просмотр логов в реальном времени

6. **UDP Broadcast (core/)**
   - `ServerDiscoveryBroadcaster` - рассылка информации о сервере
   - `DiscoveryPacket` - пакет обнаружения (JSON)

**Требования:**
- Использовать виртуальные потоки Java 21 (Thread.ofVirtual())
- Вся бизнес-логика асинхронная (CompletableFuture)
- SOLID принципы
- Unit и интеграционные тесты
- Логирование всех операций

---

### Модуль 4: chat-client (Клиент)

#### Задачи:

1. **Ядро клиента (core/)**
   - `ChatClient` - основной класс клиента
   - `ClientConfig` - конфигурация клиента
   - `ConnectionState` - состояние соединения (enum)

2. **Обнаружение серверов (discovery/)**
   - `ServerDiscovery` - UDP discovery listener
   - `DiscoveryRequest` - запрос обнаружения
   - `DiscoveryResponse` - ответ сервера
   - `ServerInfo` - информация о сервере (record)

3. **Сетевой клиент (network/)**
   - `NetworkClient` - Netty TCP клиент
   - `ClientInitializer` - инициализация pipeline
   - `ClientHandler` - обработчик входящих сообщений
   - `ConnectionManager` - управление соединением
   - `ResponseFuture` - асинхронные ответы

4. **JavaFX GUI (gui/)**

   **Application:**
   - `ChatClientApp` - главное приложение

   **Сцены:**
   - `ServerSelectionScene` - выбор сервера
   - `LoginScene` - вход в систему
   - `ChatScene` - главный интерфейс чата
   - `ProfileScene` - профиль пользователя
   - `RegistrationScene` - регистрация

   **Контроллеры:**
   - `ServerSelectionController` - выбор сервера из списка
   - `LoginController` - аутентификация
   - `ChatController` - отправка/получение сообщений
   - `ProfileController` - редактирование профиля
   - `RegistrationController` - регистрация пользователя

   **FXML Views:**
   - ServerSelectionView.fxml
   - LoginView.fxml
   - ChatView.fxml
   - ProfileView.fxml
   - RegistrationView.fxml

   **Custom Components:**
   - `MessageBubble` - отображение сообщения
   - `UserListCell` - ячейка списка пользователей
   - `AvatarImageView` - аватар пользователя
   - `StatusIndicator` - индикатор статуса

5. **Key Exchange (network/)**
   - `KeyExchangeProtocol` - обмен ключами с сервером
     - Получение RSA публичного ключа
     - Генерация AES сессионного ключа
     - Шифрование AES ключа RSA
     - Отправка зашифрованного ключа

**Требования:**
- Реактивный UI с JavaFX bindings
- Автоматическое переподключение
- Оффлайн режим с кешированием
- Unit тесты (TestFX для UI тестов)

---

### Модуль 5: chat-apps (Лаунчеры)

#### Задачи:

1. **chat-server-launcher**
   - `ServerLauncher` - точка входа сервера
   - Загрузка конфигурации
   - Инициализация logging
   - Запуск ChatServer
   - Запуск ServerAdminApp

2. **chat-client-launcher**
   - `ClientLauncher` - точка входа клиента
   - Загрузка конфигурации
   - Инициализация logging
   - Запуск ChatClientApp

**Требования:**
- Maven assembly для создания fat JAR
- Манифест с Main-Class
- Поддержка командной строки (--config, --help)

---

## Протокол взаимодействия

### Бинарный формат сообщения

```
Header (28 bytes):
  [0-3]   MAGIC_NUMBER (0x43 0x48 41 54)
  [4-5]   MESSAGE_TYPE (uint16)
  [6]     VERSION (uint8)
  [7]     FLAGS (uint8): ENCRYPTED=0x80, COMPRESSED=0x40
  [8-15]  MESSAGE_ID (UUID)
  [16-19] PAYLOAD_LENGTH (uint32)
  [20-27] TIMESTAMP (uint64, epoch ms)
  [28-31] CHECKSUM (CRC32)

Payload (encrypted if FLAGS.ENCRYPTED):
  [0-15]  IV (AES initialization vector)
  [16-31] TAG (GCM authentication tag)
  [32-...] ENCRYPTED_DATA (AES-256-GCM encrypted JSON)
```

### Типы сообщений

**Обнаружение (UDP):**
- SERVICE_DISCOVERY_REQ (0x0001)
- SERVICE_DISCOVERY_RES (0x0002)

**Аутентификация:**
- AUTH_HANDSHAKE_REQ (0x0100)
- AUTH_HANDSHAKE_RES (0x0101)
- AUTH_KEY_EXCHANGE_REQ (0x0102)
- AUTH_KEY_EXCHANGE_RES (0x0103)
- AUTH_LOGIN_REQ (0x0122)
- AUTH_LOGIN_RES (0x0123)
- AUTH_REGISTER_REQ (0x0120)
- AUTH_REGISTER_RES (0x0121)
- AUTH_LOGOUT_REQ (0x0124)
- AUTH_LOGOUT_RES (0x0125)

**Управление пользователями:**
- USER_GET_PROFILE_REQ (0x0300)
- USER_GET_PROFILE_RES (0x0301)
- USER_UPDATE_PROFILE_REQ (0x0302)
- USER_UPDATE_PROFILE_RES (0x0303)
- USER_SEARCH_REQ (0x0304)
- USER_SEARCH_RES (0x0305)
- USER_STATUS_UPDATE (0x0306)

**Управление чатами:**
- CHAT_CREATE_REQ (0x0400)
- CHAT_CREATE_RES (0x0401)
- CHAT_LIST_REQ (0x0402)
- CHAT_LIST_RES (0x0403)
- CHAT_JOIN_REQ (0x0404)
- CHAT_JOIN_RES (0x0405)
- CHAT_LEAVE_REQ (0x0406)
- CHAT_LEAVE_RES (0x0407)

**Сообщения:**
- MESSAGE_SEND_REQ (0x0500)
- MESSAGE_SEND_RES (0x0501)
- MESSAGE_RECEIVE (0x0502)
- MESSAGE_HISTORY_REQ (0x0503)
- MESSAGE_HISTORY_RES (0x0504)
- MESSAGE_READ_RECEIPT (0x0505)
- MESSAGE_TYPING_INDICATOR (0x050A)

**Системные:**
- PING (0xF000)
- PONG (0xF001)
- ERROR (0xF002)

**Подробнее:** Смотреть [PROTOCOL_SPEC.md](PROTOCOL_SPEC.md)

---

## Требования к коду

### Java 21 Features
- Использовать **virtual threads** (Thread.ofVirtual()) для асинхронных операций
- Использовать **records** для неизменяемых данных
- Использовать **pattern matching** для instanceof
- Использовать **switch expressions**
- Использовать **text blocks** для многострочных строк

### SOLID Principles
- **S**: Каждый класс имеет одну ответственность
- **O**: Классы открыты для расширения, закрыты для модификации
- **L**: Подклассы должны быть взаимозаменяемыми
- **I**: Интерфейсы должны быть специализированными
- **D**: Модули верхнего уровня не зависят от модулей нижнего уровня

### Code Style
- 4 пробела для отступа
- JavaDoc для всех public API
- Осмысленные имена переменных/методов
- Максимальная длина метода: 50 строк
- Максимальная длина класса: 500 строк
- Cyclomatic complexity: < 10

### Error Handling
- Никаких empty catch blocks
- Логировать все исключения с контекстом
- Использовать кастомные исключения
- Возвращать осмысленные сообщения об ошибках

### Logging
- Использовать SLF4J + Log4j2
- Уровни: TRACE, DEBUG, INFO, WARN, ERROR
- Включать контекст (userId, chatId, messageId)
- Избегать строковой конкатенации в logging (use parameterized logging)

### Testing
- **Unit Tests**: Все бизнес-методы
- **Integration Tests**: Взаимодействие компонентов
- **Coverage Target**: > 80%
- **Naming**: `[methodName]_[expectedBehavior]_[inputState]`

---

## Технические метрики

| Метрика | Цель | Измерение |
|---------|------|-----------|
| Test Coverage | > 80% | JaCoCo |
| Build Time | < 2 мин | Maven build |
| Startup Time (Server) | < 5 сек | System time |
| Startup Time (Client) | < 3 сек | System time |
| Concurrent Connections | 10,000+ | Load test |
| Message Throughput | 100,000 msg/s | Performance test |
| Memory per Connection | < 10KB | Profiler |
| Avg Latency | < 50ms | Network measurement |

---

## Критерии приемки (Definition of Done)

Компонент считается завершенным, когда:

1. ✅ Код компилируется без ошибок и warning-ов
2. ✅ Все unit тесты проходят (зеленые)
3. ✅ Интеграционные тесты проходят
4. ✅ Code coverage >= 80%
5. ✅ JavaDoc документация полная
6. ✅ Code review пройден
7. ✅ Checkstyle/PMD passes
8. ✅ SpotBugs не находит критических багов
9. ✅ Логирование настроено корректно
10. ✅ Обработка ошибок реализована

---

## Сроки разработки

- **Фаза 1**: Настройка проекта и инфраструктуры - 1 неделя
- **Фаза 2**: Общий модуль (chat-common) - 1 неделя
- **Фаза 3**: Плагины шифрования - 1 неделя
- **Фаза 4**: База данных сервера - 1 неделя
- **Фаза 5**: Серверные менеджеры - 1 неделя
- **Фаза 6**: Netty сервер и обработчики - 2 недели
- **Фаза 7**: UDP Broadcast - 1 неделя
- **Фаза 8**: Клиентская сеть - 1 неделя
- **Фаза 9**: Клиентское обнаружение - 1 неделя
- **Фаза 10**: JavaFX GUI Server - 2 недели
- **Фаза 11**: JavaFX GUI Client - 3 недели
- **Фаза 12**: Тестирование и оптимизация - 3 недели
- **Фаза 13**: Документация и деплой - 2 недели

**Итого:** ~21 неделя (5 месяцев)

---

## Документация

Для подробной информации смотрите:
- [ARCHITECTURE.md](ARCHITECTURE.md) - Полная архитектура системы
- [PROTOCOL_SPEC.md](PROTOCOL_SPEC.md) - Спецификация протокола
- [DEVELOPMENT_PLAN.md](DEVELOPMENT_PLAN.md) - Детальный план разработки
- [README.md](README.md) - Краткое описание и quick start

---

## Контакт

Вопросы и предложения направлять на:
- Email: dev@chatv2.local
- GitHub Issues: https://github.com/chatv2/chatv2/issues

---

**Версия ТЗ:** 1.0.0
**Дата:** Февраль 2026
