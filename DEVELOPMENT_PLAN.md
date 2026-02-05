# ChatV2 - План разработки

## Обзор документа

Этот документ содержит детальный план разработки профессионального локального чат-приложения на Java 21.

---

## Фаза 1: Настройка проекта и инфраструктуры (Неделя 1)

### Задачи

#### 1.1 Инициализация Maven проекта
- [x] Создать root POM с dependencyManagement
- [ ] Создать структуру модулей (common, server, client, plugins, apps)
- [ ] Настроить Java 21 компилятор
- [ ] Настроить Git репозиторий и .gitignore
- [ ] Добавить README.md с инструкциями

**Критерии приемки:**
- `mvn clean install` выполняется без ошибок
- Все модули корректно собираются
- Зависимости с фиксированными версиями

#### 1.2 Настройка структуры пакетов
- [ ] Создать структуру пакетов для `chat-common`
- [ ] Создать структуру пакетов для `chat-server`
- [ ] Создать структуру пакетов для `chat-client`
- [ ] Создать структуру пакетов для плагинов шифрования

#### 1.3 Конфигурация логирования
- [ ] Добавить Log4j2 во все модули
- [ ] Создать log4j2.xml конфигурацию для сервера
- [ ] Создать log4j2.xml конфигурацию для клиента
- [ ] Настроить уровни логирования (DEBUG в dev, INFO в prod)

#### 1.4 Настройка JUnit 5
- [ ] Добавить JUnit 5 зависимости
- [ ] Создать базовый класс для тестов
- [ ] Настроить surefire plugin
- [ ] Создать пример теста для каждого модуля

---

## Фаза 2: Общий модуль (chat-common) (Неделя 2)

### Задачи

#### 2.1 Модели данных
**Файл:** `UserProfile.java`
```java
public record UserProfile(
    UUID userId,
    String username,
    String passwordHash,
    String salt,
    String fullName,
    byte[] avatarData,
    String bio,
    UserStatus status,
    Instant createdAt,
    Instant updatedAt
) {}
```
- [ ] Создать record UserProfile
- [ ] Добавить методы валидации
- [ ] Реализовать JSON сериализацию/десериализацию
- [ ] Создать unit тесты

**Файл:** `Chat.java`
- [ ] Создать record Chat
- [ ] Поля: chatId, chatType (enum), name, description, ownerId, participants
- [ ] Создать enum ChatType (PRIVATE, GROUP)
- [ ] Создать unit тесты

**Файл:** `Message.java`
- [ ] Создать record Message
- [ ] Поля: messageId, chatId, senderId, content, messageType, replyToId, timestamp
- [ ] Создать enum MessageType (TEXT, IMAGE, FILE, SYSTEM)
- [ ] Создать unit тесты

#### 2.2 Протокол сообщений
**Файл:** `MessageType.java` (enum)
- [ ] Определить все типы сообщений (AUTH, CHAT, USER, SESSION)
- [ ] Добавить методы для определения направления (REQ/RES)
- [ ] Создать unit тесты

**Файл:** `ChatMessage.java`
```java
public class ChatMessage {
    private final MessageHeader header;
    private final MessageBody body;
    private final byte[] payload;
    private final int checksum;
    // методы: encode, decode, validate
}
```
- [ ] Реализовать структуру бинарного сообщения
- [ ] Header: magic number (4) + type (2) + version (1) + flags (1)
- [ ] Length: 4 bytes (body length)
- [ ] Body: переменная длина
- [ ] Checksum: CRC32 (4 bytes)
- [ ] Создать unit тесты

**Файл:** `MessageCodec.java`
- [ ] Реализовать Encoder (ByteBuf -> ChatMessage)
- [ ] Реализовать Decoder (ChatMessage -> ByteBuf)
- [ ] Обработка флага шифрования
- [ ] Создать unit тесты с Netty EmbeddedChannel

#### 2.3 Криптография
**Файл:** `CryptoUtils.java`
- [ ] Реализовать метод для генерации UUID
- [ ] Реализовать хеширование паролей (Argon2)
- [ ] Реализовать генерацию соли
- [ ] Создать unit тесты

**Файл:** `KeyManager.java`
- [ ] Генерация AES-256 ключей
- [ ] Генерация RSA ключей (4096 бит)
- [ ] Сериализация ключей в PEM формат
- [ ] Создать unit тесты

#### 2.4 Исключения
- [ ] Создать базовый класс `ChatException`
- [ ] Создать `AuthenticationException`
- [ ] Создать `AuthorizationException`
- [ ] Создать `NetworkException`
- [ ] Создать `EncryptionException`
- [ ] Создать `ValidationException`

---

## Фаза 3: Архитектура плагинов шифрования (Неделя 3)

### Задачи

#### 3.1 API плагинов
**Модуль:** `chat-encryption-api`

**Файл:** `EncryptionPlugin.java`
```java
public interface EncryptionPlugin {
    String getName();
    String getVersion();
    EncryptionAlgorithm getAlgorithm();
    CompletableFuture<byte[]> encrypt(byte[] plaintext, KeySpec keySpec);
    CompletableFuture<byte[]> decrypt(byte[] ciphertext, KeySpec keySpec);
    CompletableFuture<KeySpec> generateKey();
    boolean isKeyValid(KeySpec keySpec);
}
```
- [ ] Определить интерфейс EncryptionPlugin
- [ ] Создать record EncryptionAlgorithm
- [ ] Создать record KeySpec
- [ ] Создать unit тесты интерфейса

#### 3.2 AES-256 плагин
**Модуль:** `chat-encryption-aes`

**Файл:** `AesEncryptionPlugin.java`
- [ ] Реализовать EncryptionPlugin
- [ ] Использовать AES/GCM/NoPadding
- [ ] Генерация случайного IV для каждого сообщения
- [ ] Аутентификационный тег 128 бит
- [ ] Создать unit тесты (векторные тесты)

**Файл:** `AesKeyGenerator.java`
- [ ] Генерация 256-битных ключей
- [ ] Использовать SecureRandom
- [ ] Создать unit тесты

#### 3.3 RSA плагин
**Модуль:** `chat-encryption-rsa`

**Файл:** `RsaEncryptionPlugin.java`
- [ ] Реализовать EncryptionPlugin
- [ ] Использовать RSA/ECB/PKCS1Padding
- [ ] Размер ключа 4096 бит
- [ ] Использование Bouncy Castle
- [ ] Создать unit тесты

#### 3.4 Загрузчик плагинов
**Файл:** `EncryptionPluginLoader.java`
- [ ] Использовать ServiceLoader (SPI)
- [ ] META-INF/services/com.chatv2.encryption.api.EncryptionPlugin
- [ ] Поддержка динамической загрузки из JAR файлов
- [ ] Создать unit тесты

---

## Фаза 4: База данных сервера (Неделя 4)

### Задачи

#### 4.1 Инициализация базы данных
**Файл:** `DatabaseManager.java`
- [ ] Создать connection pool (HikariCP)
- [ ] Настроить embedded H2 базу данных
- [ ] Режим: файловая база данных (для персистентности)
- [ ] Connection URL: jdbc:h2:./data/chat.db
- [ ] Создать unit тесты

#### 4.2 Миграции схемы
**Файл:** `SchemaInitializer.java`
- [ ] Создать таблицу users
- [ ] Создать таблицу sessions
- [ ] Создать таблицу chats
- [ ] Создать таблицу chat_participants
- [ ] Создать таблицу messages
- [ ] Создать таблицу message_read_receipts
- [ ] Создать индексы
- [ ] Создать unit тесты

#### 4.3 Repository Pattern
**Файл:** `UserRepository.java`
```java
public interface UserRepository {
    CompletableFuture<UserProfile> save(UserProfile user);
    CompletableFuture<Optional<UserProfile>> findById(UUID userId);
    CompletableFuture<Optional<UserProfile>> findByUsername(String username);
    CompletableFuture<List<UserProfile>> findAll();
    CompletableFuture<Void> delete(UUID userId);
}
```
- [ ] Создать интерфейс UserRepository
- [ ] Реализовать UserRepositoryImpl
- [ ] Создать unit тесты с in-memory H2

**Файл:** `SessionRepository.java`
- [ ] Создать интерфейс SessionRepository
- [ ] Реализовать SessionRepositoryImpl
- [ ] Методы: save, findByToken, deleteByUserId, deleteExpired
- [ ] Создать unit тесты

**Файл:** `ChatRepository.java`
- [ ] Создать интерфейс ChatRepository
- [ ] Реализовать ChatRepositoryImpl
- [ ] Методы: save, findById, findByUserId, delete
- [ ] Создать unit тесты

**Файл:** `MessageRepository.java`
- [ ] Создать интерфейс MessageRepository
- [ ] Реализовать MessageRepositoryImpl
- [ ] Методы: save, findById, findByChatId, findHistory
- [ ] Пагинация истории сообщений
- [ ] Создать unit тесты

---

## Фаза 5: Серверные менеджеры бизнес-логики (Неделя 5)

### Задачи

#### 5.1 UserManager
**Файл:** `UserManager.java`
```java
public interface UserManager {
    CompletableFuture<UserProfile> register(String username, String password);
    CompletableFuture<UserProfile> login(String username, String password);
    CompletableFuture<Void> logout(UUID userId);
    CompletableFuture<UserProfile> getProfile(UUID userId);
    CompletableFuture<Void> updateProfile(UUID userId, UserProfile updates);
    CompletableFuture<List<UserProfile>> searchUsers(String query);
    CompletableFuture<Void> updateStatus(UUID userId, UserStatus status);
}
```
- [ ] Создать интерфейс UserManager
- [ ] Реализовать UserManagerImpl
- [ ] Регистрация: валидация имени, хеширование пароля
- [ ] Логин: проверка пароля через Argon2
- [ ] Поиск пользователей по никнейму (LIKE %query%)
- [ ] Создать unit тесты
- [ ] Создать интеграционные тесты

#### 5.2 SessionManager
**Файл:** `SessionManager.java`
```java
public interface SessionManager {
    CompletableFuture<Session> createSession(UUID userId);
    CompletableFuture<Optional<Session>> validateSession(String token);
    CompletableFuture<Session> refreshSession(String token);
    CompletableFuture<Void> terminateSession(String token);
    CompletableFuture<Void> terminateAllUserSessions(UUID userId);
}
```
- [ ] Создать интерфейс SessionManager
- [ ] Реализовать SessionManagerImpl
- [ ] Генерация JWT токенов
- [ ] Хранение токенов в базе данных
- [ ] Проверка срока действия
- [ ] Создать unit тесты

#### 5.3 ChatManager
**Файл:** `ChatManager.java`
```java
public interface ChatManager {
    CompletableFuture<Chat> createPrivateChat(UUID user1Id, UUID user2Id);
    CompletableFuture<Chat> createGroupChat(UUID ownerId, String name, Set<UUID> memberIds);
    CompletableFuture<Void> addParticipant(UUID chatId, UUID userId);
    CompletableFuture<Void> removeParticipant(UUID chatId, UUID userId);
    CompletableFuture<List<Chat>> getUserChats(UUID userId);
    CompletableFuture<Set<UUID>> getParticipants(UUID chatId);
    CompletableFuture<Void> updateChatInfo(UUID chatId, String name, String description);
}
```
- [ ] Создать интерфейс ChatManager
- [ ] Реализовать ChatManagerImpl
- [ ] Логика создания личного чата (проверка на дубликат)
- [ ] Логика группового чата (валидация участников)
- [ ] Права доступа (owner, admin, member)
- [ ] Создать unit тесты
- [ ] Создать интеграционные тесты

#### 5.4 MessageManager
**Файл:** `MessageManager.java`
```java
public interface MessageManager {
    CompletableFuture<Message> sendMessage(UUID chatId, UUID senderId, String content);
    CompletableFuture<List<Message>> getHistory(UUID chatId, int limit, int offset);
    CompletableFuture<Void> markAsRead(UUID messageId, UUID userId);
    CompletableFuture<List<Message>> getUnreadMessages(UUID userId, UUID chatId);
}
```
- [ ] Создать интерфейс MessageManager
- [ ] Реализовать MessageManagerImpl
- [ ] Отправка сообщения с валидацией
- [ ] Получение истории с пагинацией
- [ ] Отметки о прочтении
- [ ] Создать unit тесты

---

## Фаза 6: Netty сервер и обработчики (Неделя 6-7)

### Задачи

#### 6.1 ChatServer - основной класс
**Файл:** `ChatServer.java`
```java
public class ChatServer {
    private final ServerConfig config;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;

    public ChatServer(ServerConfig config) { }
    public CompletableFuture<Void> start() { }
    public CompletableFuture<Void> stop() { }
}
```
- [ ] Создать класс ChatServer
- [ ] Настроить NioEventLoopGroup с виртуальными потоками для worker-ов
- [ ] Настроить ServerBootstrap
- [ ] Использовать Netty 4.1.109.Final
- [ ] Graceful shutdown с таймаутом
- [ ] Создать unit тесты с EmbeddedChannel

#### 6.2 Bootstrap Factory
**Файл:** `BootstrapFactory.java`
- [ ] Создать factory для конфигурации bootstrap
- [ ] Настроить SO_BACKLOG
- [ ] Настроить TCP_NODELAY
- [ ] Настроить KEEP_ALIVE
- [ ] Создать unit тесты

#### 6.3 Channel Initializer
**Файл:** `ServerInitializer.java`
- [ ] Создать ChannelInitializer
- [ ] Добавить MessageCodec
- [ ] Добавить EncryptionHandler
- [ ] Добавить SessionHandler
- [ ] Добавить MessageHandler
- [ ] Добавить ExceptionHandler
- [ ] Создать unit тесты

#### 6.4 Обработчики сообщений
**Файл:** `AuthHandler.java`
- [ ] Обработка AUTH_REGISTER_REQ
- [ ] Обработка AUTH_LOGIN_REQ
- [ ] Обработка AUTH_LOGOUT_REQ
- [ ] Отправка соответствующих ответов
- [ ] Создать unit тесты

**Файл:** `ChatHandler.java`
- [ ] Обработка CHAT_CREATE_REQ
- [ ] Обработка CHAT_JOIN_REQ
- [ ] Обработка CHAT_LEAVE_REQ
- [ ] Обработка MESSAGE_SEND_REQ
- [ ] Отправка событий другим участникам
- [ ] Создать unit тесты

**Файл:** `EncryptionHandler.java`
- [ ] Перехват входящих сообщений
- [ ] Дешифрование payload
- [ ] Шифрование исходящих сообщений
- [ ] Использование активного плагина шифрования
- [ ] Создать unit тесты

**Файл:** `SessionHandler.java`
- [ ] Валидация токена сессии
- [ ] Проверка срока действия
- [ ] Получение userId из токена
- [ ] Добавление userId в ChannelAttributes
- [ ] Создать unit тесты

**Файл:** `ExceptionHandler.java`
- [ ] Обработка всех исключений в pipeline
- [ ] Логирование ошибок
- [ ] Отправка ERROR сообщений клиенту
- [ ] Создать unit тесты

---

## Фаза 7: UDP Broadcast и обнаружение серверов (Неделя 8)

### Задачи

#### 7.1 Серверный UDP broadcast
**Файл:** `ServerDiscoveryBroadcaster.java`
- [ ] Создать UDP broadcaster
- [ ] Мультикаст адрес: 239.255.255.250:9999
- [ ] Период отправки: 5 секунд
- [ ] Payload: JSON с информацией о сервере
- [ ] Создать unit тесты

**Файл:** `DiscoveryPacket.java`
```json
{
  "serverName": "ChatV2 Server",
  "address": "192.168.1.100",
  "port": 8080,
  "version": "1.0.0",
  "encryptionRequired": true,
  "currentUsers": 42,
  "maxUsers": 1000
}
```
- [ ] Создать record DiscoveryPacket
- [ ] JSON сериализация/десериализация
- [ ] Создать unit тесты

#### 7.2 Клиентский UDP listener
**Файл:** `ServerDiscovery.java`
- [ ] Создать UDP listener
- [ ] Подписка на мультикаст группу
- [ ] Прием broadcast сообщений
- [ ] Парсинг и кеширование серверов
- [ ] Создать unit тесты

---

## Фаза 8: Клиентская часть - Сеть (Неделя 9)

### Задачи

#### 8.1 NetworkClient
**Файл:** `NetworkClient.java`
```java
public class NetworkClient {
    private final EncryptionPlugin encryptionPlugin;
    private EventLoopGroup eventLoopGroup;
    private Channel channel;

    public NetworkClient(EncryptionPlugin plugin) { }
    public CompletableFuture<Void> connect(String host, int port) { }
    public CompletableFuture<Void> disconnect() { }
    public CompletableFuture<ChatMessage> sendRequest(ChatMessage request) { }
}
```
- [ ] Создать класс NetworkClient
- [ ] Реализовать соединение с сервером
- [ ] Настроить виртуальные потоки
- [ ] Обработка разрыва соединения
- [ ] Автоматическое переподключение
- [ ] Создать unit тесты

#### 8.2 Client Initializer
**Файл:** `ClientInitializer.java`
- [ ] Создать ChannelInitializer для клиента
- [ ] Добавить MessageCodec
- [ ] Добавить EncryptionHandler
- [ ] Добавить ClientHandler
- [ ] Создать unit тесты

#### 8.3 Client Handler
**Файл:** `ClientHandler.java`
- [ ] Обработка входящих сообщений от сервера
- [ ] Диспетчеризация сообщений по типу
- [ ] Callback для уведомлений UI
- [ ] Обработка heartbeat/ping
- [ ] Создать unit тесты

#### 8.4 Key Exchange
**Файл:** `KeyExchangeProtocol.java`
- [ ] Получение RSA публичного ключа от сервера
- [ ] Генерация AES сессионного ключа
- [ ] Шифрование AES ключа RSA публичным ключом
- [ ] Отправка зашифрованного ключа на сервер
- [ ] Создать unit тесты

---

## Фаза 9: Клиентская часть - Обнаружение серверов (Неделя 10)

### Задачи

#### 9.1 ServerDiscovery (client side)
**Файл:** `ServerDiscovery.java`
- [ ] Реализовать UDP broadcast listener
- [ ] Метод: discoverServers(Duration timeout)
- [ ] Кеширование списка серверов
- [ ] Периодическое обновление списка
- [ ] Создать unit тесты

#### 9.2 ServerInfo
**Файл:** `ServerInfo.java`
```java
public record ServerInfo(
    String serverName,
    String address,
    int port,
    String version,
    boolean encryptionRequired,
    int currentUsers,
    int maxUsers,
    Instant lastSeen
) {}
```
- [ ] Создать record ServerInfo
- [ ] Методы: isAvailable, getLoadPercentage
- [ ] Создать unit тесты

---

## Фаза 10: JavaFX GUI - Сервер (Неделя 11-12)

### Задачи

#### 10.1 Server Admin Application
**Файл:** `ServerAdminApp.java`
- [ ] Создать класс JavaFX Application
- [ ] Настроить primaryStage
- [ ] Создать контейнер для сцен
- [ ] Загрузка стилей CSS
- [ ] Создать unit тесты

#### 10.2 Dashboard Scene
**Файл:** `DashboardController.java`
- [ ] Статистика: количество пользователей, чатов, сообщений
- [ ] Графики: активность по времени
- [ ] Список активных подключений
- [ ] Кнопки управления (старт/стоп сервера)
- [ ] FXML файл: DashboardView.fxml
- [ ] Создать unit тесты (TestFX)

#### 10.3 User Management Scene
**Файл:** `UserController.java`
- [ ] TableView всех пользователей
- [ ] Поиск и фильтрация
- [ ] CRUD операции (создание, редактирование, удаление)
- [ ] Просмотр деталей пользователя
- [ ] FXML файл: UserManagementView.fxml
- [ ] Создать unit тесты

#### 10.4 Chat Management Scene
**Файл:** `ChatController.java`
- [ ] TableView всех чатов
- [ ] Просмотр участников чата
- [ ] Управление правами
- [ ] Просмотр истории сообщений
- [ ] FXML файл: ChatManagementView.fxml
- [ ] Создать unit тесты

#### 10.5 Log Viewer Scene
**Файл:** `LogViewerController.java`
- [ ] Просмотр логов в реальном времени
- [ ] Фильтрация по уровню (ERROR, WARN, INFO)
- [ ] Поиск по тексту
- [ ] Автопрокрутка
- [ ] FXML файл: LogViewerView.fxml
- [ ] Создать unit тесты

---

## Фаза 11: JavaFX GUI - Клиент (Неделя 13-15)

### Задачи

#### 11.1 ChatClient Application
**Файл:** `ChatClientApp.java`
- [ ] Создать класс JavaFX Application
- [ ] Настроить primaryStage
- [ ] Управление сценами
- [ ] Загрузка конфигурации
- [ ] Инициализация компонентов

#### 11.2 Server Selection Scene
**Файл:** `ServerSelectionController.java`
- [ ] ListView обнаруженных серверов
- [ ] Ручной ввод адреса/порта
- [ ] Кнопка "Обновить список"
- [ ] Индикатор доступности сервера
- [ ] FXML файл: ServerSelectionView.fxml
- [ ] Создать unit тесты

#### 11.3 Login Scene
**Файл:** `LoginController.java`
- [ ] Поля: username, password
- [ ] Кнопка "Войти"
- [ ] Кнопка "Регистрация"
- [ ] Запоминание данных
- [ ] Валидация ввода
- [ ] FXML файл: LoginView.fxml
- [ ] Создать unit тесты

#### 11.4 Registration Scene
**Файл:** `RegistrationController.java`
- [ ] Поля: username, password, confirm password, email
- [ ] Валидация полей
- [ ] Проверка сложности пароля
- [ ] Аватар (загрузка изображения)
- [ ] FXML файл: RegistrationView.fxml
- [ ] Создать unit тесты

#### 11.5 Chat Scene (главная)
**Файл:** `ChatController.java`
- [ ] Слева: список чатов
- [ ] Центр: история сообщений
- [ ] Справа: список участников
- [ ] Снизу: поле ввода сообщения
- [ ] FXML файл: ChatView.fxml
- [ ] Создать unit тесты

#### 11.6 Custom Components
**Файл:** `MessageBubble.java`
- [ ] Custom control для отображения сообщения
- [ ] В зависимости от отправителя (свое/чужое)
- [ ] Время отправки
- [ ] Статус прочтения
- [ ] Вложения (изображения)
- [ ] Создать unit тесты

**Файл:** `UserListCell.java`
- [ ] Custom ListCell для списка пользователей
- [ ] Аватар + никнейм + статус
- [ ] Цветовой индикатор статуса
- [ ] Создать unit тесты

**Файл:** `AvatarImageView.java`
- [ ] Circle clipped ImageView для аватара
- [ ] Загрузка из byte[] или URL
- [ ] Placeholder при отсутствии
- [ ] Создать unit тесты

#### 11.7 Profile Scene
**Файл:** `ProfileController.java`
- [ ] Отображение профиля пользователя
- [ ] Редактирование (собственный профиль)
- [ ] Загрузка аватара
- [ ] Изменение статуса
- [ ] FXML файл: ProfileView.fxml
- [ ] Создать unit тесты

---

## Фаза 12: Тестирование и оптимизация (Неделя 16-18)

### Задачи

#### 12.1 Unit Testing
- [ ] Достижение покрытия кода > 80%
- [ ] Тесты для всех менеджеров
- [ ] Тесты для всех репозиториев
- [ ] Тесты для Netty handlers
- [ ] Тесты для компонентов шифрования

#### 12.2 Integration Testing
- [ ] Интеграционные тесты сервера с embedded Netty
- [ ] Интеграционные тесты клиента с mock сервером
- [ ] Тесты полной аутентификации
- [ ] Тесты создания чатов
- [ ] Тесты отправки сообщений

#### 12.3 Performance Testing
- [ ] Load testing с JMeter или Gatling
- [ ] Тест на 1000+ одновременных подключений
- [ ] Тест пропускной способности сообщений
- [ ] Профилирование памяти (VisualVM, JProfiler)
- [ ] Оптимизация горячих точек

#### 12.4 Security Testing
- [ ] Проверка на SQL Injection
- [ ] Проверка на XSS (если есть web часть)
- [ ] Тест на brute-force атаки
- [ ] Анализ утечек данных
- [ ] Проверка strength шифрования

#### 12.5 Virtual Threads Validation
- [ ] Проверка корректности использования virtual threads
- [ ] Тест на deadlock-free
- [ ] Тест на race conditions
- [ ] Профилирование потребления памяти virtual threads

---

## Фаза 13: Документация и деплой (Неделя 19-20)

### Задачи

#### 13.1 Документация
- [ ] User Guide (руководство пользователя)
- [ ] Admin Guide (руководство администратора)
- [ ] Developer Guide (руководство разработчика)
- [ ] API Documentation (Javadoc)
- [ ] Protocol Specification
- [ ] Installation Guide

#### 13.2 Сборка и упаковка
- [ ] Настроить maven-assembly-plugin для fat JAR
- [ ] Создать инсталлятор для Windows (.msi)
- [ ] Создать инсталлятор для Linux (.deb, .rpm)
- [ ] Создать DMG для macOS
- [ ] Тестировать установку на всех ОС

#### 13.3 Деплой
- [ ] Настроить CI/CD (GitHub Actions)
- [ ] Автоматический запуск тестов
- [ ] Автоматический релиз при теге
- [ ] Публикация артефактов

---

## Фаза 14: Дополнительные функции (Неделя 21+)

### Дополнительные возможности (по желанию)

- [ ] Файловые вложения
- [ ] Голосовые сообщения
- [ ] Видеозвонки (WebRTC)
- [ ] Эмодзи и стикеры
- [ ] Темы оформления
- [ ] Многоязычность (i18n)
- [ ] Уведомления в системе
- [ ] Экспорт истории чата
- [ ] Поиск по сообщениям
- [ ] Блокировка пользователей
- [ ] Администрирование сервера через web

---

## Checklist для каждого компонента

### При реализации каждого компонента:

1. **Код:**
   - [ ] Следовать Java 21 best practices
   - [ ] Использовать virtual threads где применимо
   - [ ] Соблюдать SOLID принципы
   - [ ] Добавлять JavaDoc комментарии
   - [ ] Обрабатывать все исключения

2. **Тесты:**
   - [ ] Unit тесты для всех public методов
   - [ ] Интеграционные тесты для важных сценариев
   - [ ] Edge cases testing
   - [ ] Тесты на валидацию ввода

3. **Документация:**
   - [ ] JavaDoc для всех public API
   - [ ] Примеры использования в комментариях
   - [ ] README для модуля (если нужно)

4. **Code Quality:**
   - [ ] Checkstyle/PMD passes
   - [ ] SpotBugs анализ
   - [ ] Code review
   - [ ] SonarQube анализ

---

## Определения "Готово" (Definition of Done)

Компонент считается готовым, когда:

1. ✅ Код компилируется без ошибок
2. ✅ Все unit тесты проходят (зеленые)
3. ✅ Интеграционные тесты проходят
4. ✅ Code coverage >= 80%
5. ✅ JavaDoc документация полная
6. ✅ Code review пройден
7. ✅ Нет критических warning-ов от компилятора
8. ✅ Логирование настроено корректно
9. ✅ Обработка ошибок реализована
10. ✅ SOLID принципы соблюдены

---

## Метрики успеха

| Метрика | Цель | Текущее |
|---------|------|---------|
| Test Coverage | 80%+ | - |
| Build Time | < 2 min | - |
| Startup Time (Server) | < 5 sec | - |
| Concurrent Connections | 10,000+ | - |
| Message Throughput | 100,000 msg/s | - |
| Memory per Connection | < 10KB | - |
| Average Latency | < 50ms | - |

---

**Версия документа:** 1.0.0
**Дата обновления:** Февраль 2026
