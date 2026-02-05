# ChatV2 - Детальный План Разработки для Senior Developer

## Обзор документа

Этот документ содержит детальный технический план для реализации всех невыполненных задач проекта ChatV2. План организован по приоритетам и включает технические спецификации для каждого компонента.

---

## Стек технологий

| Категория | Технология | Версия | Назначение |
|-----------|------------|--------|------------|
| Язык | Java | 21 LTS | Core language с virtual threads |
| Build Tool | Maven | 3.9.6 | Dependency management |
| Network | Netty | 4.1.109.Final | Async network framework |
| GUI | JavaFX | 21.0.1 | Desktop UI |
| Database | H2 | 2.2.224 | Embedded SQL database |
| Crypto | Bouncy Castle | 1.77 | Cryptography library |
| JSON | Jackson | 2.16.1 | JSON serialization |
| YAML | SnakeYAML | 2.2 | YAML configuration parsing |
| JWT | java-jwt | 0.12.3 | JWT token generation/validation |
| Logging | Log4j2 | 2.23.1 | Logging framework |
| Testing | JUnit 5 | 5.10.2 | Unit testing |
| GUI Testing | TestFX | 4.0.18 | JavaFX UI testing |

---

## Структура проекта

```
D:\code\Chatv2\
├── pom.xml                                        # Root POM
├── chat-common/                                   # ✅ Реализовано
├── chat-server/                                   # ✅ Частично реализовано
│   ├── pom.xml
│   └── src/main/java/com/chatv2/server/
│       ├── core/                                  # ✅ ChatServer, ServerConfig
│       ├── manager/                               # ✅ All Managers
│       ├── handler/                               # ⚠️ Base handlers implemented
│       ├── storage/                               # ✅ All Repositories
│       ├── gui/                                   # ❌ TODO: JavaFX Admin GUI
│       ├── config/                                # ❌ TODO: ServerProperties
│       ├── broadcaster/                           # ❌ TODO: ServerDiscoveryBroadcaster
│       └── pipeline/                              # ❌ TODO: EncryptionHandler
├── chat-client/                                   # ✅ Частично реализовано
│   ├── pom.xml
│   └── src/main/java/com/chatv2/client/
│       ├── core/                                  # ✅ ChatClient, ClientConfig
│       ├── discovery/                             # ✅ ServerDiscovery, ServerInfo
│       ├── network/                               # ✅ NetworkClient, ClientHandler
│       ├── gui/                                   # ❌ TODO: JavaFX Client GUI
│       ├── config/                                # ❌ TODO: ClientProperties
│       └── controller/                           # ❌ TODO: UI Controllers
├── chat-encryption-plugins/                       # ✅ Реализовано
│   ├── chat-encryption-api/
│   ├── chat-encryption-aes/
│   └── chat-encryption-rsa/
└── chat-apps/                                     # ✅ Частично реализовано
    ├── chat-server-launcher/
    └── chat-client-launcher/
```

---

## ПРИОРИТЕТ 1: Core Functionality (для базовой работоспособности)

### Задача 1.1: Configuration Management (ServerProperties)

**Класс:** `ServerProperties.java`
**Путь:** `chat-server/src/main/java/com/chatv2/server/config/ServerProperties.java`

**Описание:**
Класс для загрузки конфигурации сервера из YAML файла. Поддерживает дефолтные значения, валидацию и горячую перезагрузку.

**Основные методы:**
```java
public class ServerProperties {
    // Загрузка конфигурации из файла
    public static ServerProperties load(String configPath) throws IOException;

    // Getters для всех настроек
    public String getHost();
    public int getPort();
    public String getServerName();
    public UdpConfig getUdpConfig();
    public DatabaseConfig getDatabaseConfig();
    public EncryptionConfig getEncryptionConfig();
    public SessionConfig getSessionConfig();

    // Валидация конфигурации
    public void validate() throws ValidationException;

    // Records для nested configurations
    public record UdpConfig(boolean enabled, String multicastAddress, int port, int broadcastInterval);
    public record DatabaseConfig(String path, int connectionPoolSize);
    public record EncryptionConfig(boolean required, String defaultPlugin, int rsaKeySize, int aesKeySize);
    public record SessionConfig(int tokenExpirationSeconds, int refreshTokenExpirationDays);
}
```

**Технические спецификации:**
- Использовать SnakeYAML 2.2 для парсинга YAML
- Формат конфигурации соответствует README.md
- Путь по умолчанию: `config/server-config.yaml`
- При отсутствии файла создавать с дефолтными значениями
- Поддержка переменных окружения (${ENV_VAR})
- Логирование изменений конфигурации

**Зависимости от других задач:**
- Зависит от chat-common (ValidationException)

**Сложность:** Medium

---

### Задача 1.2: Configuration Management (ClientProperties)

**Класс:** `ClientProperties.java`
**Путь:** `chat-client/src/main/java/com/chatv2/client/config/ClientProperties.java`

**Описание:**
Класс для загрузки конфигурации клиента из YAML файла.

**Основные методы:**
```java
public class ClientProperties {
    public static ClientProperties load(String configPath) throws IOException;

    // Getters
    public String getClientName();
    public String getVersion();
    public DiscoveryConfig getDiscoveryConfig();
    public ConnectionConfig getConnectionConfig();
    public EncryptionConfig getEncryptionConfig();
    public UiConfig getUiConfig();

    // Records
    public record DiscoveryConfig(boolean enabled, String multicastAddress, int port, int timeoutSeconds);
    public record ConnectionConfig(int reconnectAttempts, int reconnectDelaySeconds, int heartbeatIntervalSeconds);
    public record EncryptionConfig(boolean enabled);
    public record UiConfig(String theme, String language, int avatarSize);
}
```

**Технические спецификации:**
- Использовать SnakeYAML 2.2
- Формат конфигурации соответствует README.md
- Путь по умолчанию: `config/client-config.yaml`
- Валидация всех параметров

**Зависимости от других задач:**
- Нет зависимостей

**Сложность:** Low

---

### Задача 1.3: Session Management (JWT)

**Класс:** `SessionManagerImpl.java`
**Путь:** `chat-server/src/main/java/com/chatv2/server/manager/SessionManagerImpl.java`

**Описание:**
Реализация интерфейса SessionManager с генерацией и валидацией JWT токенов. Использование java-jwt библиотеки.

**Основные методы:**
```java
public class SessionManagerImpl implements SessionManager {
    // Генерация JWT токена
    @Override
    public CompletableFuture<Session> createSession(UUID userId) {
        // 1. Генерация уникального session ID
        // 2. Генерация JWT токена с payload: userId, iat, exp
        // 3. Сохранение сессии в БД
        // 4. Возврат Session записи
    }

    // Валидация JWT токена
    @Override
    public CompletableFuture<Optional<Session>> validateSession(String token) {
        // 1. Парсинг JWT токена
        // 2. Проверка подписи (HMAC256)
        // 3. Проверка срока действия
        // 4. Поиск сессии в БД
        // 5. Обновление lastAccessedAt
    }

    // Обновление токена
    @Override
    public CompletableFuture<Session> refreshSession(String token) {
        // 1. Валидация старого токена
        // 2. Генерация нового токена
        // 3. Обновление в БД
    }

    // Завершение сессии
    @Override
    public CompletableFuture<Void> terminateSession(String token);
    public CompletableFuture<Void> terminateAllUserSessions(UUID userId);
}
```

**Технические спецификации:**
- Использовать java-jwt 0.12.3 (com.auth0:java-jwt)
- Алгоритм подписи: HMAC256
- Секрет ключ из конфигурации или генерация при старте
- Payload токена: {userId, iat, exp}
- Хранение токенов в таблице sessions БД
- Автоматическая очистка expired сессий (cron job)
- Логирование всех операций сессий

**Зависимости от других задач:**
- Зависит от SessionRepository
- Зависит от ServerProperties

**Сложность:** High

---

### Задача 1.4: EncryptionHandler (Netty Pipeline)

**Класс:** `EncryptionHandler.java`
**Путь:** `chat-server/src/main/java/com/chatv2/server/pipeline/EncryptionHandler.java`

**Описание:**
Netty ChannelHandler для автоматического шифрования/дешифрования сообщений в pipeline. Интеграция с EncryptionPluginManager.

**Основные методы:**
```java
@ChannelHandler.Sharable
public class EncryptionHandler extends ChannelDuplexHandler {
    private final EncryptionPluginManager pluginManager;
    private final Map<Channel, byte[]> sessionKeys = new ConcurrentHashMap<>();

    // Дешифрование входящих сообщений (read)
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof ChatMessage chatMsg) {
            if (chatMsg.getHeader().isEncrypted()) {
                // 1. Извлечь IV и tag из payload
                // 2. Получить session key для канала
                // 3. Дешифровать payload
                // 4. Обновить флаг isEncrypted = false
            }
            ctx.fireChannelRead(chatMsg);
        }
    }

    // Шифрование исходящих сообщений (write)
    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof ChatMessage chatMsg) {
            if (shouldEncrypt(chatMsg)) {
                // 1. Получить session key для канала
                // 2. Сгенерировать случайный IV
                // 3. Зашифровать payload (AES-256-GCM)
                // 4. Добавить IV и tag в payload
                // 5. Установить флаг isEncrypted = true
            }
            ctx.write(chatMsg, promise);
        }
    }

    // Установка session key для канала
    public void setSessionKey(Channel channel, byte[] sessionKey);
}
```

**Технические спецификации:**
- Наследовать от ChannelDuplexHandler
- Аннотация @Sharable для reuse между каналами
- Использовать активный EncryptionPlugin из pluginManager
- Формат зашифрованного payload: [IV 16 bytes] [Tag 16 bytes] [Encrypted Data]
- Обработка исключений шифрования/дешифрования
- Логирование операций шифрования (без чувствительных данных)

**Зависимости от других задач:**
- Зависит от EncryptionPluginManager
- Зависит от ChatMessage из chat-common

**Сложность:** High

---

### Задача 1.5: Binary Protocol Encoder/Decoder (Full Implementation)

**Класс:** `MessageCodec.java`
**Путь:** `chat-common/src/main/java/com/chatv2/common/protocol/MessageCodec.java`

**Описание:**
Полная реализация энкодера/декодера бинарного протокола для Netty. Поддержка всех типов сообщений и флагов.

**Основные методы:**
```java
public class MessageCodec extends MessageToMessageCodec<ByteBuf, ChatMessage> {
    // Декодирование ByteBuf -> ChatMessage
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf buf, List<Object> out) throws Exception {
        // 1. Проверка минимальной длины (32 bytes)
        // 2. Чтение Magic Number (4 bytes) - валидация
        // 3. Чтение Message Type (2 bytes)
        // 4. Чтение Version (1 byte) - валидация
        // 5. Чтение Flags (1 byte)
        // 6. Чтение Message ID (UUID, 16 bytes)
        // 7. Чтение Payload Length (4 bytes) - валидация max size
        // 8. Чтение Timestamp (8 bytes)
        // 9. Чтение Checksum (4 bytes) - CRC32 валидация
        // 10. Чтение Payload (variable length)
        // 11. Создание ChatMessage
    }

    // Кодирование ChatMessage -> ByteBuf
    @Override
    protected void encode(ChannelHandlerContext ctx, ChatMessage msg, List<Object> out) throws Exception {
        // 1. Создать ByteBuf с вычисленным размером
        // 2. Записать Magic Number
        // 3. Записать Message Type
        // 4. Записать Version
        // 5. Записать Flags
        // 6. Записать Message ID
        // 7. Записать Payload Length
        // 8. Записать Timestamp
        // 9. Вычислить и записать Checksum
        // 10. Записать Payload
    }

    // Вспомогательные методы
    private int calculateChecksum(ByteBuf payload);
    private void validateMagicNumber(int magic) throws ProtocolException;
    private void validatePayloadLength(int length) throws ProtocolException;
}
```

**Технические спецификации:**
- Наследовать от MessageToMessageCodec<ByteBuf, ChatMessage>
- Magic Number: 0x43484154 ("CHAT")
- Max Payload Size: 1MB (настраиваемо)
- Checksum: CRC32
- Endianness: Big Endian (Netty default)
- Обработка всех флагов (ENCRYPTED, COMPRESSED)
- Оптимизация с использованием ByteBufAllocator
- Логирование ошибок протокола

**Зависимости от других задач:**
- Зависит от ChatMessage
- Зависит от ProtocolMessageType

**Сложность:** Medium

---

## ПРИОРИТЕТ 2: Network Features

### Задача 2.1: UDP Broadcast Server side

**Класс:** `ServerDiscoveryBroadcaster.java`
**Путь:** `chat-server/src/main/java/com/chatv2/server/broadcaster/ServerDiscoveryBroadcaster.java`

**Описание:**
Компонент для периодической рассылки информации о сервере через UDP multicast для auto-discovery.

**Основные методы:**
```java
public class ServerDiscoveryBroadcaster implements Runnable {
    private final DatagramSocket socket;
    private final InetAddress multicastAddress;
    private final int port;
    private final int broadcastInterval;
    private final ServerInfo serverInfo;
    private volatile boolean running = true;
    private ScheduledExecutorService scheduler;

    // Запуск broadcaster
    public void start() {
        // 1. Создать DatagramSocket
        // 2. Подписаться на multicast группу
        // 3. Запланировать периодическую отправку
    }

    // Остановка broadcaster
    public void stop() {
        // 1. Установить running = false
        // 2. Shutdown scheduler
        // 3. Закрыть socket
    }

    // Периодическая отправка discovery packet
    @Override
    public void run() {
        if (running) {
            // 1. Сериализовать ServerInfo в JSON
            // 2. Создать DatagramPacket
            // 3. Отправить на multicast address:port
            // 4. Логировать отправку
        }
    }

    // Обновление информации о сервере
    public void updateServerInfo(int currentUsers, ServerState state);
}
```

**Технические спецификации:**
- Multicast Address: 239.255.255.250
- Port: 9999
- Broadcast Interval: 5 секунд (настраиваемо)
- Payload: JSON формат (DiscoveryPacket)
- Использовать Jackson для JSON сериализации
- Поддержка IPv4 multicast
- Graceful shutdown
- Логирование ошибок отправки

**Зависимости от других задач:**
- Зависит от ServerInfo из chat-client (для record определения)
- Зависит от Jackson

**Сложность:** Medium

---

### Задача 2.2: ConnectionManager (Automatic Reconnection)

**Класс:** `ConnectionManager.java`
**Путь:** `chat-client/src/main/java/com/chatv2/client/network/ConnectionManager.java`

**Описание:**
Компонент для управления жизненным циклом соединения клиента с автоматическим переподключением.

**Основные методы:**
```java
public class ConnectionManager {
    private final NetworkClient networkClient;
    private final ConnectionConfig config;
    private volatile ConnectionState state = ConnectionState.DISCONNECTED;
    private ScheduledExecutorService reconnectScheduler;
    private int reconnectAttempts = 0;

    // Подключение к серверу
    public CompletableFuture<Void> connect(String host, int port) {
        // 1. Проверить состояние (не должен быть уже подключен)
        // 2. Установить state = CONNECTING
        // 3. Вызвать networkClient.connect()
        // 4. При успехе: state = CONNECTED, reconnectAttempts = 0
        // 5. При ошибке: запустить процедуру переподключения
    }

    // Отключение от сервера
    public CompletableFuture<Void> disconnect() {
        // 1. Отменить scheduled reconnect
        // 2. Вызвать networkClient.disconnect()
        // 3. Установить state = DISCONNECTED
    }

    // Автоматическое переподключение
    private void scheduleReconnect(String host, int port) {
        // 1. Проверить reconnectAttempts < maxAttempts
        // 2. Задержка: delay * reconnectAttempts (exponential backoff)
        // 3. Запланировать reconnect()
        // 4. Увеличить reconnectAttempts
    }

    // Проверка соединения (heartbeat)
    public CompletableFuture<Boolean> checkConnection() {
        // 1. Отправить PING сообщение
        // 2. Ожидать PONG с таймаутом
        // 3. Вернуть результат
    }

    // Getters
    public ConnectionState getState();
    public boolean isConnected();
}
```

**Технические спецификации:**
- Exponential backoff: 5s, 10s, 20s, 40s, 80s
- Max reconnect attempts: 5 (настраиваемо)
- Heartbeat interval: 30 секунд (настраиваемо)
- Использовать ScheduledExecutorService с virtual threads
- Callback для уведомления UI об изменениях состояния
- Логирование всех попыток подключения

**Зависимости от других задач:**
- Зависит от NetworkClient
- Зависит от ConnectionConfig (из ClientProperties)

**Сложность:** Medium

---

### Задача 2.3: Key Exchange Protocol

**Класс:** `KeyExchangeProtocol.java`
**Путь:** `chat-client/src/main/java/com/chatv2/client/network/KeyExchangeProtocol.java`

**Описание:**
Протокол обмена ключами между клиентом и сервером для установки зашифрованного соединения.

**Основные методы:**
```java
public class KeyExchangeProtocol {
    private final NetworkClient networkClient;
    private final RsaEncryptionPlugin rsaPlugin;
    private final AesEncryptionPlugin aesPlugin;
    private byte[] sessionKey;
    private PublicKey serverPublicKey;

    // Полный цикл обмена ключами
    public CompletableFuture<byte[]> performKeyExchange() {
        // Шаг 1: Запрос RSA публичного ключа сервера
        return requestServerPublicKey()
            // Шаг 2: Генерация AES сессионного ключа
            .thenCompose(publicKey -> generateSessionKey(publicKey))
            // Шаг 3: Шифрование AES ключа RSA публичным ключом
            .thenCompose(this::encryptSessionKey)
            // Шаг 4: Отправка зашифрованного ключа серверу
            .thenCompose(this::sendEncryptedKey)
            // Шаг 5: Получение подтверждения от сервера
            .thenCompose(this::receiveConfirmation);
    }

    // Запрос RSA публичного ключа
    private CompletableFuture<PublicKey> requestServerPublicKey() {
        // 1. Создать AUTH_HANDSHAKE_REQ сообщение
        // 2. Отправить через networkClient
        // 3. Парсить AUTH_HANDSHAKE_RES
        // 4. Извлечь RSA public key (PEM формат)
        // 5. Десериализовать KeyFactory
    }

    // Генерация AES сессионного ключа
    private CompletableFuture<byte[]> generateSessionKey(PublicKey publicKey) {
        // 1. Сохранить serverPublicKey
        // 2. Вызвать aesPlugin.generateKey()
        // 3. Вернуть сырые байты ключа
    }

    // Шифрование AES ключа
    private CompletableFuture<byte[]> encryptSessionKey(byte[] sessionKey) {
        // 1. Создать KeySpec для RSA
        // 2. Вызвать rsaPlugin.encrypt()
        // 3. Вернуть зашифрованные байты
    }

    // Отправка зашифрованного ключа
    private CompletableFuture<Void> sendEncryptedKey(byte[] encryptedKey) {
        // 1. Создать AUTH_KEY_EXCHANGE_REQ
        // 2. Вложить зашифрованный ключ
        // 3. Отправить через networkClient
    }

    // Получение подтверждения
    private CompletableFuture<byte[]> receiveConfirmation(Void unused) {
        // 1. Ожидать AUTH_KEY_EXCHANGE_RES
        // 2. Проверить success flag
        // 3. Вернуть sessionKey
    }

    // Getter
    public byte[] getSessionKey();
}
```

**Технические спецификации:**
- Использовать RSA-4096 с OAEP padding
- Использовать AES-256-GCM для сессионного ключа
- Ключ должен быть уникальным для каждой сессии
- Логировать все этапы (без ключей)
- Таймауты для каждого этапа (30 секунд)
- Обработка ошибок и повторные попытки

**Зависимости от других задач:**
- Зависит от NetworkClient
- Зависит от RsaEncryptionPlugin
- Зависит от AesEncryptionPlugin

**Сложность:** High

---

## ПРИОРИТЕТ 3: GUI Server

### Задача 3.1: ServerAdminApp (JavaFX Main)

**Класс:** `ServerAdminApp.java`
**Путь:** `chat-server/src/main/java/com/chatv2/server/gui/ServerAdminApp.java`

**Описание:**
JavaFX Application класс для запуска админской панели сервера.

**Основные методы:**
```java
public class ServerAdminApp extends Application {
    private static Logger log = LoggerFactory.getLogger(ServerAdminApp.class);
    private ChatServer chatServer;

    @Override
    public void init() throws Exception {
        // 1. Загрузить конфигурацию
        // 2. Инициализировать logging
        // 3. Создать ChatServer instance
        // 4. Запустить сервер (виртуальный thread)
    }

    @Override
    public void start(Stage primaryStage) {
        // 1. Создать main BorderPane
        // 2. Создать sidebar navigation
        // 3. Загрузить FXML для Dashboard (default)
        // 4. Настроить primaryStage
        // 5. Показать окно
    }

    @Override
    public void stop() throws Exception {
        // 1. Остановить ChatServer (graceful shutdown)
        // 2. Закрыть все ресурсы
    }

    // Переключение сцен
    private void switchScene(String sceneName, FXMLLoader loader);
}
```

**Технические спецификации:**
- Наследовать от javafx.application.Application
- Использовать FXMLLoader для загрузки FXML
- CSS стилизация (dark theme)
- Размер окна: 1200x800 минимальный
- Обработка закрытия окна (confirmation dialog)
- Интеграция с ChatServer для реального времени
- Логирование всех событий UI

**Зависимости от других задач:**
- Зависит от ChatServer
- Зависит от всех Controller классов

**Сложность:** Low

---

### Задача 3.2: DashboardController + FXML

**Класс:** `DashboardController.java`
**Путь:** `chat-server/src/main/java/com/chatv2/server/gui/controller/DashboardController.java`
**FXML:** `chat-server/src/main/resources/fxml/DashboardView.fxml`

**Описание:**
Контроллер для главной страницы админ-панели с статистикой сервера.

**Основные методы:**
```java
public class DashboardController implements Initializable {
    @FXML private Label serverNameLabel;
    @FXML private Label serverStatusLabel;
    @FXML private Label uptimeLabel;
    @FXML private Label connectedClientsLabel;
    @FXML private Label totalUsersLabel;
    @FXML private Label totalChatsLabel;
    @FXML private Label totalMessagesLabel;
    @FXML private Label connectionsPerMinuteLabel;
    @FXML private PieChart activityChart;
    @FXML private LineChart<String, Number> trafficChart;
    @FXML private Button startButton;
    @FXML private Button stopButton;
    @FXML private Button restartButton;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 1. Инициализировать UI компоненты
        // 2. Запустить periodic updates (ScheduledExecutorService)
        // 3. Настроить charts
    }

    // Обновление статистики
    private void updateStatistics() {
        // 1. Получить данные от ChatServer
        // 2. Обновить labels
        // 3. Обновить charts
    }

    // Обработчики кнопок
    @FXML private void handleStartButton();
    @FXML private void handleStopButton();
    @FXML private void handleRestartButton();
}
```

**Технические спецификации FXML:**
- BorderPane layout
- Top: Server info (name, status, uptime)
- Center: Two column layout
  - Left: Statistics cards (Users, Chats, Messages, Connections)
  - Right: Charts (PieChart, LineChart)
- Bottom: Control buttons (Start, Stop, Restart)
- Использовать JavaFX Charts API
- Обновление каждую секунду

**Зависимости от других задач:**
- Зависит от ChatServer для данных
- Зависит от ServerAdminApp

**Сложность:** Medium

---

### Задача 3.3: UserController + FXML

**Класс:** `UserController.java`
**Путь:** `chat-server/src/main/java/com/chatv2/server/gui/controller/UserController.java`
**FXML:** `chat-server/src/main/resources/fxml/UserManagementView.fxml`

**Описание:**
Контроллер для управления пользователями (CRUD операции).

**Основные методы:**
```java
public class UserController implements Initializable {
    @FXML private TableView<UserProfile> userTable;
    @FXML private TableColumn<UserProfile, String> usernameColumn;
    @FXML private TableColumn<UserProfile, String> fullNameColumn;
    @FXML private TableColumn<UserProfile, UserStatus> statusColumn;
    @FXML private TableColumn<UserProfile, Instant> createdAtColumn;
    @FXML private TextField searchField;
    @FXML private Button addButton;
    @FXML private Button editButton;
    @FXML private Button deleteButton;
    @FXML private Button refreshButton;

    private UserManager userManager;
    private ObservableList<UserProfile> users = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 1. Настроить TableColumn cell value factories
        // 2. Загрузить пользователей из userManager
        // 3. Настроить фильтрацию по поиску
    }

    // Загрузка пользователей
    private void loadUsers() {
        // 1. Вызвать userManager.getAllUsers()
        // 2. Обновить ObservableList
    }

    // Поиск пользователей
    @FXML private void handleSearch() {
        // 1. Получить текст из searchField
        // 2. Вызвать userManager.searchUsers()
        // 3. Обновить TableView
    }

    // Добавление пользователя
    @FXML private void handleAddUser() {
        // 1. Показать диалог создания пользователя
        // 2. Собрать данные формы
        // 3. Вызвать userManager.register()
        // 4. Обновить список
    }

    // Редактирование пользователя
    @FXML private void handleEditUser() {
        // 1. Получить выбранного пользователя
        // 2. Показать диалог редактирования
        // 3. Вызвать userManager.updateProfile()
        // 4. Обновить список
    }

    // Удаление пользователя
    @FXML private void handleDeleteUser() {
        // 1. Получить выбранного пользователя
        // 2. Показать confirmation dialog
        // 3. Вызвать userManager.deleteUser()
        // 4. Обновить список
    }

    // Обновление списка
    @FXML private void handleRefresh();
}
```

**Технические спецификации FXML:**
- BorderPane layout
- Top: Search field + Refresh button
- Center: TableView (User info)
- Bottom: CRUD buttons (Add, Edit, Delete)
- Использовать Custom TableCell для отображения статусов
- Dialog для добавления/редактирования

**Зависимости от других задач:**
- Зависит от UserManager
- Зависит от UserProfile из chat-common

**Сложность:** Medium

---

### Задача 3.4: ChatController + FXML

**Класс:** `ChatController.java`
**Путь:** `chat-server/src/main/java/com/chatv2/server/gui/controller/ChatController.java`
**FXML:** `chat-server/src/main/resources/fxml/ChatManagementView.fxml`

**Описание:**
Контроллер для управления чатами и их участниками.

**Основные методы:**
```java
public class ChatController implements Initializable {
    @FXML private TableView<Chat> chatTable;
    @FXML private TableColumn<Chat, String> chatNameColumn;
    @FXML private TableColumn<Chat, ChatType> chatTypeColumn;
    @FXML private TableColumn<Chat, Integer> participantsCountColumn;
    @FXML private ListView<UserProfile> participantsListView;
    @FXML private Button viewParticipantsButton;
    @FXML private Button addParticipantButton;
    @FXML private Button removeParticipantButton;
    @FXML private Button viewHistoryButton;

    private ChatManager chatManager;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 1. Настроить TableView
        // 2. Загрузить чаты
    }

    // Загрузка чатов
    private void loadChats();

    // Просмотр участников
    @FXML private void handleViewParticipants() {
        // 1. Получить выбранный чат
        // 2. Вызвать chatManager.getParticipants()
        // 3. Обновить ListView
    }

    // Добавление участника
    @FXML private void handleAddParticipant();

    // Удаление участника
    @FXML private void handleRemoveParticipant();

    // Просмотр истории сообщений
    @FXML private void handleViewHistory();
}
```

**Технические спецификации FXML:**
- SplitPane layout (left: чаты, right: участники)
- TableView для списка чатов
- ListView для списка участников
- Кнопки управления участниками
- Dialog для просмотра истории сообщений

**Зависимости от других задач:**
- Зависит от ChatManager
- Зависит на Chat из chat-common

**Сложность:** Medium

---

### Задача 3.5: LogViewerController + FXML

**Класс:** `LogViewerController.java`
**Путь:** `chat-server/src/main/java/com/chatv2/server/gui/controller/LogViewerController.java`
**FXML:** `chat-server/src/main/resources/fxml/LogViewerView.fxml`

**Описание:**
Контроллер для просмотра логов сервера в реальном времени.

**Основные методы:**
```java
public class LogViewerController implements Initializable {
    @FXML private TextArea logTextArea;
    @FXML private ComboBox<LogLevel> levelFilterComboBox;
    @FXML private TextField searchField;
    @FXML private Button clearButton;
    @FXML private Button autoScrollCheckBox;

    private LogAppender customAppender;
    private boolean autoScroll = true;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 1. Настроить Log4j2 custom appender
        // 2. Настроить фильтрацию по уровню
        // 3. Настроить поиск
    }

    // Добавление лог записи
    private void appendLog(LogEvent event) {
        // 1. Проверить фильтр уровня
        // 2. Проверить фильтр поиска
        // 3. Добавить в TextArea
        // 4. Auto scroll если включен
    }

    // Фильтрация по уровню
    @FXML private void handleLevelFilter() {
        // 1. Получить выбранный уровень
        // 2. Обновить фильтр appender
        // 3. Перезагрузить логи
    }

    // Поиск по тексту
    @FXML private void handleSearch();

    // Очистка логов
    @FXML private void handleClear();

    // Включение/отключение автопрокрутки
    @FXML private void handleAutoScroll();
}
```

**Технические спецификации FXML:**
- VBox layout
- Top: Фильтры (level, search)
- Center: TextArea (editable=false)
- Bottom: Buttons (Clear, AutoScroll checkbox)
- Custom Log4j2 Appender для перехвата логов
- Поддержка цветов для разных уровней (ERROR-red, WARN-yellow, etc.)

**Зависимости от других задач:**
- Зависит от Log4j2

**Сложность:** Medium

---

## ПРИОРИТЕТ 4: GUI Client

### Задача 4.1: ChatClientApp (JavaFX Main)

**Класс:** `ChatClientApp.java`
**Путь:** `chat-client/src/main/java/com/chatv2/client/gui/ChatClientApp.java`

**Описание:**
JavaFX Application класс для запуска клиентского приложения.

**Основные методы:**
```java
public class ChatClientApp extends Application {
    private ChatClient chatClient;
    private ConnectionManager connectionManager;
    private Scene serverSelectionScene;
    private Scene loginScene;
    private Scene chatScene;
    private Scene profileScene;

    @Override
    public void init() throws Exception {
        // 1. Загрузить конфигурацию
        // 2. Инициализировать ChatClient
        // 3. Инициализировать ConnectionManager
    }

    @Override
    public void start(Stage primaryStage) {
        // 1. Загрузить все сцены
        // 2. Показать ServerSelectionScene (default)
    }

    // Переключение сцен
    public void showServerSelectionScene();
    public void showLoginScene();
    public void showChatScene();
    public void showProfileScene();
}
```

**Технические спецификации:**
- Наследовать от javafx.application.Application
- Управление всеми сценами приложения
- Singleton instance для доступа из контроллеров
- Обработка закрытия приложения

**Зависимости от других задач:**
- Зависит от ChatClient
- Зависит от всех Client Controller классов

**Сложность:** Low

---

### Задача 4.2: ServerSelectionController + FXML

**Класс:** `ServerSelectionController.java`
**Путь:** `chat-client/src/main/java/com/chatv2/client/gui/controller/ServerSelectionController.java`
**FXML:** `chat-client/src/main/resources/fxml/ServerSelectionView.fxml`

**Описание:**
Контроллер для выбора сервера из списка обнаруженных или ручного ввода.

**Основные методы:**
```java
public class ServerSelectionController implements Initializable {
    @FXML private ListView<ServerInfo> serverListView;
    @FXML private Button refreshButton;
    @FXML private Button connectButton;
    @FXML private TextField manualHostField;
    @FXML private TextField manualPortField;
    @FXML private Button manualConnectButton;

    private ServerDiscovery serverDiscovery;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 1. Настроить ListView с custom ListCell
        // 2. Запустить ServerDiscovery
        // 3. Обновить список каждые 30 секунд
    }

    // Обновление списка серверов
    private void refreshServerList() {
        // 1. Вызвать serverDiscovery.discoverServers()
        // 2. Обновить ObservableList
    }

    // Подключение к выбранному серверу
    @FXML private void handleConnect() {
        // 1. Получить выбранный сервер
        // 2. Вызвать connectionManager.connect()
        // 3. При успехе: показать LoginScene
    }

    // Ручное подключение
    @FXML private void handleManualConnect() {
        // 1. Валидировать поля
        // 2. Вызвать connectionManager.connect()
        // 3. При успехе: показать LoginScene
    }

    // Обновление списка
    @FXML private void handleRefresh();
}
```

**Технические спецификации FXML:**
- BorderPane layout
- Top: Manual connection fields + button
- Center: ListView (ServerInfo)
- Bottom: Refresh + Connect buttons
- Использовать ServerListCell для отображения информации о сервере

**Зависимости от других задач:**
- Зависит от ServerDiscovery
- Зависит от ConnectionManager
- Зависит от ServerListCell (Custom Component)

**Сложность:** Medium

---

### Задача 4.3: LoginController + FXML

**Класс:** `LoginController.java`
**Путь:** `chat-client/src/main/java/com/chatv2/client/gui/controller/LoginController.java`
**FXML:** `chat-client/src/main/resources/fxml/LoginView.fxml`

**Описание:**
Контроллер для авторизации пользователя.

**Основные методы:**
```java
public class LoginController implements Initializable {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private CheckBox rememberMeCheckBox;
    @FXML private Button loginButton;
    @FXML private Button registerButton;
    @FXML private Button backButton;
    @FXML private Label errorLabel;

    private NetworkClient networkClient;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 1. Загрузить сохраненные данные если rememberMe
    }

    // Обработка логина
    @FXML private void handleLogin() {
        // 1. Валидировать поля
        // 2. Создать AUTH_LOGIN_REQ сообщение
        // 3. Отправить через networkClient
        // 4. Парсить AUTH_LOGIN_RES
        // 5. При успехе: сохранить токен, показать ChatScene
        // 6. При ошибке: показать error message
    }

    // Переход к регистрации
    @FXML private void handleRegister();

    // Возврат к выбору сервера
    @FXML private void handleBack();
}
```

**Технические спецификации FXML:**
- VBox layout centered
- TextField для username
- PasswordField для password
- CheckBox для "запомнить меня"
- Кнопки: Login, Register, Back
- Error label для сообщений об ошибках
- Валидация полей в реальном времени

**Зависимости от других задач:**
- Зависит от NetworkClient

**Сложность:** Low

---

### Задача 4.4: RegistrationController + FXML

**Класс:** `RegistrationController.java`
**Путь:** `chat-client/src/main/java/com/chatv2/client/gui/controller/RegistrationController.java`
**FXML:** `chat-client/src/main/resources/fxml/RegistrationView.fxml`

**Описание:**
Контроллер для регистрации нового пользователя.

**Основные методы:**
```java
public class RegistrationController implements Initializable {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private TextField fullNameField;
    @FXML private TextField emailField;
    @FXML private Button chooseAvatarButton;
    @FXML private ImageView avatarImageView;
    @FXML private Button registerButton;
    @FXML private Button backButton;

    private byte[] avatarData;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 1. Настроить валидацию полей
    }

    // Валидация формы
    private boolean validateForm() {
        // 1. Проверить username (3-20 chars, alphanumeric)
        // 2. Проверить password (минимум 8 chars, сложность)
        // 3. Проверить password == confirmPassword
        // 4. Проверить fullName (не пустой)
    }

    // Выбор аватара
    @FXML private void handleChooseAvatar() {
        // 1. Показать FileChooser
        // 2. Загрузить изображение
        // 3. Изменить размер до 128x128
        // 4. Показать в avatarImageView
    }

    // Регистрация
    @FXML private void handleRegister() {
        // 1. Валидировать форму
        // 2. Хешировать пароль (Argon2)
        // 3. Создать AUTH_REGISTER_REQ сообщение
        // 4. Отправить через networkClient
        // 5. Парсить AUTH_REGISTER_RES
        // 6. При успехе: показать LoginScene
    }

    // Возврат к логину
    @FXML private void handleBack();
}
```

**Технические спецификации FXML:**
- GridPane layout (2 колонки)
- Labels и TextField/PasswordField для каждого поля
- AvatarImageView для предпросмотра аватара
- Button для загрузки аватара
- Validation messages для каждого поля

**Зависимости от других задач:**
- Зависит от NetworkClient
- Зависит от AvatarImageView (Custom Component)

**Сложность:** Medium

---

### Задача 4.5: ChatController + FXML (Client side)

**Класс:** `ChatController.java`
**Путь:** `chat-client/src/main/java/com/chatv2/client/gui/controller/ChatController.java`
**FXML:** `chat-client/src/main/resources/fxml/ChatView.fxml`

**Описание:**
Главный контроллер чата с отображением сообщений, списка чатов и участников.

**Основные методы:**
```java
public class ChatController implements Initializable {
    @FXML private ListView<Chat> chatListView;
    @FXML private ScrollPane messageScrollPane;
    @FXML private VBox messageVBox;
    @FXML private ListView<UserProfile> participantsListView;
    @FXML private TextArea messageTextArea;
    @FXML private Button sendButton;
    @FXML private Label currentChatLabel;

    private NetworkClient networkClient;
    private UUID currentChatId;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 1. Настроить ListView для чатов
        // 2. Настроить MessageVBox с MessageBubble
        // 3. Настроить ListView для участников
        // 4. Подписаться на входящие сообщения
    }

    // Загрузка списка чатов
    private void loadChats() {
        // 1. Создать CHAT_LIST_REQ сообщение
        // 2. Отправить через networkClient
        // 3. Парсить CHAT_LIST_RES
        // 4. Обновить ObservableList
    }

    // Выбор чата
    @FXML private void handleChatSelection() {
        // 1. Получить выбранный чат
        // 2. Установить currentChatId
        // 3. Загрузить историю сообщений
        // 4. Загрузить список участников
    }

    // Загрузка истории сообщений
    private void loadMessageHistory(UUID chatId) {
        // 1. Создать MESSAGE_HISTORY_REQ сообщение
        // 2. Отправить через networkClient
        // 3. Парсить MESSAGE_HISTORY_RES
        // 4. Создать MessageBubble для каждого сообщения
        // 5. Добавить в messageVBox
    }

    // Отправка сообщения
    @FXML private void handleSendMessage() {
        // 1. Получить текст из messageTextArea
        // 2. Создать MESSAGE_SEND_REQ сообщение
        // 3. Отправить через networkClient
        // 4. Очистить messageTextArea
        // 5. Добавить сообщение в messageVBox (оптимистично)
    }

    // Получение входящего сообщения
    public void receiveMessage(Message message) {
        // 1. Создать MessageBubble
        // 2. Добавить в messageVBox
        // 3. Auto scroll вниз
    }
}
```

**Техничесские спецификации FXML:**
- BorderPane layout
- Left: ListView (Chats) - ширина 250px
- Center: SplitPane (вертикальный)
  - Top: ScrollPane + VBox (Messages)
  - Bottom: TextArea + Button (Send message)
- Right: ListView (Participants) - ширина 200px
- Использовать MessageBubble для отображения сообщений
- Использовать UserListCell для отображения участников

**Зависимости от других задач:**
- Зависит от NetworkClient
- Зависит от MessageBubble (Custom Component)
- Зависит от UserListCell (Custom Component)

**Сложность:** High

---

### Задача 4.6: ProfileController + FXML

**Класс:** `ProfileController.java`
**Путь:** `chat-client/src/main/java/com/chatv2/client/gui/controller/ProfileController.java`
**FXML:** `chat-client/src/main/resources/fxml/ProfileView.fxml`

**Описание:**
Контроллер для просмотра и редактирования профиля пользователя.

**Основные методы:**
```java
public class ProfileController implements Initializable {
    @FXML private AvatarImageView avatarImageView;
    @FXML private Label usernameLabel;
    @FXML private TextField fullNameField;
    @FXML private TextArea bioTextArea;
    @FXML private ComboBox<UserStatus> statusComboBox;
    @FXML private Button saveButton;
    @FXML private Button backButton;
    @FXML private Button changeAvatarButton;

    private NetworkClient networkClient;
    private UserProfile currentUserProfile;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 1. Загрузить профиль пользователя
    }

    // Загрузка профиля
    private void loadProfile() {
        // 1. Создать USER_GET_PROFILE_REQ сообщение
        // 2. Отправить через networkClient
        // 3. Парсить USER_GET_PROFILE_RES
        // 4. Отобразить в UI
    }

    // Смена аватара
    @FXML private void handleChangeAvatar() {
        // 1. Показать FileChooser
        // 2. Загрузить и изменить размер изображения
        // 3. Обновить avatarImageView
    }

    // Сохранение изменений
    @FXML private void handleSave() {
        // 1. Собрать данные из полей
        // 2. Создать USER_UPDATE_PROFILE_REQ сообщение
        // 3. Отправить через networkClient
        // 4. Парсить USER_UPDATE_PROFILE_RES
    }

    // Возврат к чату
    @FXML private void handleBack();
}
```

**Технические спецификации FXML:**
- VBox layout centered
- AvatarImageView (крупный, 128px)
- Labels и Fields для редактирования
- ComboBox для выбора статуса
- Кнопки: Save, Back, Change Avatar
- Readonly поля для username

**Зависимости от других задач:**
- Зависит от NetworkClient
- Зависит от AvatarImageView (Custom Component)

**Сложность:** Medium

---

## ПРИОРИТЕТ 5: Custom Components

### Задача 5.1: MessageBubble

**Класс:** `MessageBubble.java`
**Путь:** `chat-client/src/main/java/com/chatv2/client/gui/component/MessageBubble.java`

**Описание:**
Custom JavaFX control для отображения сообщения в чате с разными стилями для входящих и исходящих сообщений.

**Основные методы:**
```java
public class MessageBubble extends VBox {
    private Label senderLabel;
    private Label contentLabel;
    private Label timestampLabel;
    private HBox statusIndicator;

    public MessageBubble(Message message, boolean isOwnMessage) {
        // 1. Применить стиль в зависимости от isOwnMessage
        // 2. Создать и настроить компоненты
        // 3. Добавить в VBox
    }

    // Установка сообщения
    public void setMessage(Message message, boolean isOwnMessage) {
        // 1. Обновить senderLabel (если не own message)
        // 2. Обновить contentLabel
        // 3. Обновить timestampLabel
        // 4. Обновить statusIndicator (read receipts)
        // 5. Применить стиль
    }

    // Вспомогательные методы
    private void applyStyle(boolean isOwnMessage);
    private String formatTimestamp(Instant timestamp);
}
```

**Технические спецификации:**
- Наследовать от VBox
- Два стиля: .message-bubble-outgoing (справа, синий), .message-bubble-incoming (слева, серый)
- Скругленные углы (radius 10px)
- Отображение времени в формате HH:MM
- Индикатор статуса прочтения (✓✓)
- Поддержка встречных сообщений (replyToMessage)
- CSS стилизация

**Зависимости от других задач:**
- Зависит от Message из chat-common

**Сложность:** Medium

---

### Задача 5.2: UserListCell

**Класс:** `UserListCell.java`
**Путь:** `chat-client/src/main/java/com/chatv2/client/gui/component/UserListCell.java`

**Описание:**
Custom ListCell для отображения пользователя с аватаром, именем и статусом.

**Основные методы:**
```java
public class UserListCell extends ListCell<UserProfile> {
    private HBox container;
    private AvatarImageView avatarImageView;
    private VBox labelsBox;
    private Label usernameLabel;
    private Label statusLabel;

    public UserListCell() {
        // 1. Инициализировать компоненты
        // 2. Настроить layout
    }

    @Override
    protected void updateItem(UserProfile user, boolean empty) {
        super.updateItem(user, empty);

        if (empty || user == null) {
            setGraphic(null);
            setText(null);
        } else {
            // 1. Обновить avatarImageView с user.getAvatarData()
            // 2. Обновить usernameLabel
            // 3. Обновить statusLabel с цветом
            // 4. Установить graphic
        }
    }

    // Вспомогательные методы
    private String getStatusColor(UserStatus status);
}
```

**Технические спецификации:**
- Наследовать от ListCell<UserProfile>
- HBox layout: Avatar (слева) + VBox (справа)
- AvatarImageView размер 40px
- UsernameLabel: жирный шрифт
- StatusLabel: цветной индикатор (Online-green, Offline-gray, Away-yellow)
- Hover effect (изменение цвета фона)
- CSS стилизация

**Зависимости от других задач:**
- Зависит на UserProfile из chat-common
- Зависит от AvatarImageView

**Сложность:** Medium

---

### Задача 5.3: AvatarImageView

**Класс:** `AvatarImageView.java`
**Путь:** `chat-client/src/main/java/com/chatv2/client/gui/component/AvatarImageView.java`

**Описание:**
Custom ImageView для отображения аватара пользователя с круговым обрезанием и placeholder.

**Основные методы:**
```java
public class AvatarImageView extends StackPane {
    private ImageView imageView;
    private Circle clip;
    private Label placeholderLabel;

    public AvatarImageView(int size) {
        // 1. Создать ImageView
        // 2. Создать Circle clip
        // 3. Создать placeholderLabel (первая буква username)
        // 4. Настроить StackPane
        // 5. Применить стиль
    }

    // Установка изображения из byte[]
    public void setAvatarData(byte[] avatarData) {
        if (avatarData == null || avatarData.length == 0) {
            // Показать placeholder
        } else {
            // 1. Создать Image из byte[]
            // 2. Установить в imageView
            // 3. Скрыть placeholder
        }
    }

    // Установка изображения из URL
    public void setAvatarUrl(String url) {
        // Асинхронная загрузка из URL
    }

    // Установка placeholder
    public void setPlaceholder(String username) {
        // 1. Получить первую букву username
        // 2. Установить в placeholderLabel
        // 3. Скрыть imageView
    }

    // Изменение размера
    public void setSize(int size);
}
```

**Технические спецификации:**
- Наследовать от StackPane
- Круглое обрезание (Circle clip)
- Placeholder с первой буквой username
- Размер: настраиваемый (по умолчанию 40px)
- Поддержка загрузки из byte[] и URL
- CSS стилизация (.avatar-image-view)
- Цвет placeholder: случайный или на основе username

**Зависимости от других задач:**
- Нет зависимостей

**Сложность:** Low

---

### Задача 5.4: StatusIndicator

**Класс:** `StatusIndicator.java`
**Путь:** `chat-client/src/main/java/com/chatv2/client/gui/component/StatusIndicator.java`

**Описание:**
Custom компонент для визуального отображения статуса пользователя (онлайн/оффлайн/отошел).

**Основные методы:**
```java
public class StatusIndicator extends Circle {
    public StatusIndicator(UserStatus status, double radius) {
        super(radius);
        setStroke(Color.WHITE);
        setStrokeWidth(2);
        setStatus(status);
    }

    // Установка статуса
    public void setStatus(UserStatus status) {
        switch (status) {
            case ONLINE -> setFill(Color.LIMEGREEN);
            case OFFLINE -> setFill(Color.GRAY);
            case AWAY -> setFill(Color.ORANGE);
        }
    }

    // Анимация пульсации для ONLINE статуса
    public void setPulsing(boolean pulsing) {
        if (pulsing) {
            // Создать ScaleTransition
        } else {
            // Удалить анимацию
        }
    }
}
```

**Технические спецификации:**
- Наследовать от Circle
- Размер: настраиваемый (по умолчанию 8px)
- Цвета: ONLINE-green, OFFLINE-gray, AWAY-orange
- Белая обводка
- Опциональная анимация пульсации для ONLINE
- Tooltip с текстом статуса

**Зависимости от других задач:**
- Зависит от UserStatus из chat-common

**Сложность:** Low

---

## ПРИОРИТЕТ 6: Testing & Packaging

### Задача 6.1: TestFX Tests for GUI

**Классы тестов:**
- `DashboardControllerTest.java`
- `UserControllerTest.java`
- `ChatControllerTest.java`
- `ServerSelectionControllerTest.java`
- `LoginControllerTest.java`
- `ChatControllerTest.java`

**Путь:** `chat-client/src/test/java/com/chatv2/client/gui/controller/` и `chat-server/src/test/java/com/chatv2/server/gui/controller/`

**Описание:**
Unit и интеграционные тесты для GUI компонентов с использованием TestFX.

**Пример теста для LoginController:**
```java
@ExtendWith(ApplicationExtension.class)
class LoginControllerTest {
    @Start
    private void start(Stage stage) throws Exception {
        // 1. Загрузить FXML
        // 2. Создать controller
        // 3. Показать сцену
    }

    @Test
    void testLoginWithValidCredentials(FxRobot robot) {
        // 1. Ввести username
        robot.clickOn("#usernameField").write("testuser");
        // 2. Ввести password
        robot.clickOn("#passwordField").write("password123");
        // 3. Нажать кнопку Login
        robot.clickOn("#loginButton");
        // 4. Проверить, что сцена изменилась
        verifyThat("#chatScene", Node::isVisible);
    }

    @Test
    void testLoginWithInvalidCredentials(FxRobot robot) {
        // 1. Ввести неверные данные
        robot.clickOn("#usernameField").write("invalid");
        robot.clickOn("#passwordField").write("wrong");
        // 2. Нажать кнопку Login
        robot.clickOn("#loginButton");
        // 3. Проверить, что error label виден
        verifyThat("#errorLabel", Node::isVisible);
    }
}
```

**Технические спецификации:**
- Использовать TestFX 4.0.18
- Использовать @ExtendWith(ApplicationExtension.class)
- Использовать FxRobot для взаимодействия с UI
- Тестировать все контроллеры
- Mock NetworkClient и другие зависимости
- Code coverage для GUI: минимум 70%

**Зависимости от других задач:**
- Зависит от всех GUI компонентов

**Сложность:** High

---

### Задача 6.2: Maven Assembly Configuration

**Файл:** `assembly.xml` для Server
**Путь:** `chat-apps/chat-server-launcher/src/assembly/assembly.xml`

**Файл:** `assembly.xml` для Client
**Путь:** `chat-apps/chat-client-launcher/src/assembly/assembly.xml`

**Описание:**
Конфигурация maven-assembly-plugin для создания fat JAR файлов.

**Пример assembly.xml для Server:**
```xml
<assembly xmlns="http://maven.apache.org/ASSEMBLY/2.1.1"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/ASSEMBLY/2.1.1 https://maven.apache.org/xsd/assembly-2.1.1.xsd">
    <id>jar-with-dependencies</id>
    <formats>
        <format>jar</format>
    </formats>
    <includeBaseDirectory>false</includeBaseDirectory>

    <dependencySets>
        <dependencySet>
            <outputDirectory>/</outputDirectory>
            <useProjectArtifact>true</useProjectArtifact>
            <unpack>true</unpack>
            <scope>runtime</scope>
        </dependencySet>
    </dependencySets>

    <fileSets>
        <fileSet>
            <directory>src/main/resources</directory>
            <outputDirectory>/</outputDirectory>
            <includes>
                <include>**/*.fxml</include>
                <include>**/*.css</include>
                <include>**/*.yaml</include>
                <include>**/*.properties</include>
            </includes>
        </fileSet>
        <fileSet>
            <directory>src/main/resources</directory>
            <outputDirectory>/config</outputDirectory>
            <includes>
                <include>server-config.yaml</include>
            </includes>
        </fileSet>
    </fileSets>

    <archive>
        <manifest>
            <mainClass>com.chatv2.launcher.server.ServerLauncher</mainClass>
            <addDefaultImplementationEntries>true</addDefaultImplementationEntries>
            <addDefaultSpecificationEntries>true</addDefaultSpecificationEntries>
        </manifest>
        <manifestEntries>
            <JavaFX-Version>${javafx.version}</JavaFX-Version>
        </manifestEntries>
    </archive>
</assembly>
```

**Конфигурация POM для server-launcher:**
```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-assembly-plugin</artifactId>
            <version>3.6.0</version>
            <configuration>
                <descriptors>
                    <descriptor>src/assembly/assembly.xml</descriptor>
                </descriptors>
                <finalName>chat-server-${project.version}</finalName>
                <appendAssemblyId>true</appendAssemblyId>
            </configuration>
            <executions>
                <execution>
                    <id>make-assembly</id>
                    <phase>package</phase>
                    <goals>
                        <goal>single</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

**Технические спецификации:**
- Использовать maven-assembly-plugin 3.6.0
- Создавать fat JAR с name: `chat-server-1.0.0-jar-with-dependencies.jar`
- Включать все runtime зависимости
- Включать ресурсы (FXML, CSS, YAML)
- Main-Class: `com.chatv2.launcher.server.ServerLauncher` / `com.chatv2.launcher.client.ClientLauncher`
- Упаковывать конфигурационные файлы в `/config` директорию внутри JAR
- Убедиться, что JavaFX модули включены
- Проверка: запуск `java -jar target/chat-server-1.0.0-jar-with-dependencies.jar`

**Зависимости от других задач:**
- Зависит от всех модулей проекта

**Сложность:** Medium

---

## Общие требования

### Java 21 Features
- Использовать **virtual threads** (Thread.ofVirtual()) для асинхронных операций
- Использовать **records** для неизменяемых данных (DTOs, configs)
- Использовать **pattern matching** для instanceof
- Использовать **switch expressions**
- Использовать **text blocks** для многострочных строк (SQL queries, JSON templates)

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
- **GUI Tests**: TestFX для всех контроллеров
- **Coverage Target**: > 80%
- **Naming**: `[methodName]_[expectedBehavior]_[inputState]`

---

## Порядок реализации

1. **ПРИОРИТЕТ 1** (Core Functionality) - 1-2 недели
   - 1.1 ServerProperties
   - 1.2 ClientProperties
   - 1.3 SessionManager (JWT)
   - 1.4 EncryptionHandler
   - 1.5 Binary Protocol Encoder/Decoder

2. **ПРИОРИТЕТ 2** (Network Features) - 1-2 недели
   - 2.1 UDP Broadcast Server side
   - 2.2 ConnectionManager
   - 2.3 Key Exchange Protocol

3. **ПРИОРИТЕТ 3** (GUI Server) - 2-3 недели
   - 3.1 ServerAdminApp
   - 3.2 DashboardController + FXML
   - 3.3 UserController + FXML
   - 3.4 ChatController + FXML
   - 3.5 LogViewerController + FXML

4. **ПРИОРИТЕТ 4** (GUI Client) - 3-4 недели
   - 4.1 ChatClientApp
   - 4.2 ServerSelectionController + FXML
   - 4.3 LoginController + FXML
   - 4.4 RegistrationController + FXML
   - 4.5 ChatController + FXML
   - 4.6 ProfileController + FXML

5. **ПРИОРИТЕТ 5** (Custom Components) - 1-2 недели
   - 5.1 MessageBubble
   - 5.2 UserListCell
   - 5.3 AvatarImageView
   - 5.4 StatusIndicator

6. **ПРИОРИТЕТ 6** (Testing & Packaging) - 1-2 недели
   - 6.1 TestFX tests for GUI
   - 6.2 Maven Assembly configuration

**Общее время estimation:** 9-13 недель

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

**Версия документа:** 1.0.0
**Дата:** Февраль 2026
**Автор:** System Architect
