# ChatV2 - Professional Local Chat Application Architecture Specification

## Table of Contents
1. [Overview](#overview)
2. [Package Structure](#package-structure)
3. [Protocol Specification](#protocol-specification)
4. [Component Specifications](#component-specifications)
5. [SOLID Principles](#solid-principles)
6. [Virtual Threads Usage](#virtual-threads-usage)
7. [Plugin Architecture](#plugin-architecture)
8. [Security Architecture](#security-architecture)
9. [Database Schema](#database-schema)
10. [Development Roadmap](#development-roadmap)

---

## 1. Overview

### 1.1 System Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              CLIENT LAYER                                     │
│  ┌─────────────────────────────────────────────────────────────────────────┐│
│  │  ChatClient (JavaFX)                                                      ││
│  │  ├─ Scenes: ServerSelection | Login | Chat | Profile                      ││
│  │  ├─ Controllers: Scene-specific MVC controllers                           ││
│  │  └─ ViewModel: Observable data models                                     ││
│  └─────────────────────────────────────────────────────────────────────────┘│
│  ┌─────────────────────────────────────────────────────────────────────────┐│
│  │  ServerDiscovery (UDP Broadcast)                                         ││
│  │  ├─ UDP Multicast: 239.255.255.250:9999                                  ││
│  │  └─ Discovery protocol: SERVICE_DISCOVERY_REQ/RES                        ││
│  └─────────────────────────────────────────────────────────────────────────┘│
│  ┌─────────────────────────────────────────────────────────────────────────┐│
│  │  NetworkClient (Netty)                                                   ││
│  │  ├─ TCP Client Bootstrapper                                              ││
│  │  ├─ Encryption Plugin Interface                                          ││
│  │  └─ Message Handler Pipeline                                              ││
│  └─────────────────────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────────────────────┘
                                    │ AES-256 encrypted
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                           NETWORK LAYER (TCP)                                │
│                              Port: 8080                                      │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                              SERVER LAYER                                    │
│  ┌─────────────────────────────────────────────────────────────────────────┐│
│  │  ChatServer (Netty)                                                      ││
│  │  ├─ TCP Server Bootstrapper                                              ││
│  │  ├─ Boss EventLoopGroup (1 thread)                                      ││
│  │  ├─ Worker EventLoopGroup (Virtual Threads)                              ││
│  │  └─ UDP Broadcast Listener (Service Discovery)                            ││
│  └─────────────────────────────────────────────────────────────────────────┘│
│  ┌─────────────────────────────────────────────────────────────────────────┐│
│  │  Managers (Business Logic)                                               ││
│  │  ├─ UserManager: User registration, authentication, profiles             ││
│  │  ├─ ChatManager: Private/group chats, message routing                   ││
│  │  ├─ SessionManager: Session lifecycle, token management                 ││
│  │  └─ MessageManager: Message persistence, history                        ││
│  └─────────────────────────────────────────────────────────────────────────┘│
│  ┌─────────────────────────────────────────────────────────────────────────┐│
│  │  Server Admin GUI (JavaFX)                                               ││
│  │  ├─ Dashboard: Server statistics, online users                          ││
│  │  ├─ User Management: CRUD operations                                     ││
│  │  ├─ Chat Management: View/manage chats                                   ││
│  │  └─ Logs: Real-time log viewer                                           ││
│  └─────────────────────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                           DATA LAYER                                         │
│  ┌─────────────────────────────────────────────────────────────────────────┐│
│  │  H2 Database (Embedded)                                                 ││
│  │  ├─ users: User profiles, credentials                                    ││
│  │  ├─ sessions: Active sessions, tokens                                    ││
│  │  ├─ chats: Chat metadata                                                 ││
│  │  ├─ chat_participants: Chat membership                                  ││
│  │  └─ messages: Message history                                           ││
│  └─────────────────────────────────────────────────────────────────────────┘│
│  ┌─────────────────────────────────────────────────────────────────────────┐│
│  │  Encryption Plugins                                                      ││
│  │  ├─ EncryptionPlugin (SPI interface)                                     ││
│  │  ├─ AesEncryptionPlugin: AES-256 implementation                         ││
│  │  └─ RsaEncryptionPlugin: RSA for key exchange                           ││
│  └─────────────────────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────────────────────┘
```

### 1.2 Key Technologies

| Category | Technology | Purpose |
|----------|-----------|---------|
| Language | Java 21 | Virtual threads, pattern matching, records |
| Network | Netty 4.1.109 | Asynchronous I/O, TCP/UDP |
| GUI | JavaFX 21.0.1 | Modern desktop UI |
| Database | H2 2.2.224 | Embedded SQL database |
| Crypto | Bouncy Castle 1.77 | Strong cryptography |
| Build | Maven 3.9.6 | Dependency management |
| Testing | JUnit 5.10.2 | Unit and integration tests |

---

## 2. Package Structure

### 2.1 Overall Package Hierarchy

```
com.chatv2
├── common/
│   ├── model/                     # Shared data models
│   ├── protocol/                  # Protocol definitions
│   ├── crypto/                    # Shared cryptography utilities
│   ├── exception/                 # Custom exceptions
│   └── util/                      # Shared utilities
├── server/
│   ├── core/                      # Server core components
│   ├── manager/                   # Business logic managers
│   ├── handler/                   # Netty channel handlers
│   ├── storage/                   # Database access layer
│   ├── gui/                       # Server admin GUI
│   ├── plugin/                    # Plugin management
│   └── config/                    # Server configuration
├── client/
│   ├── core/                      # Client core components
│   ├── discovery/                 # Server discovery (UDP)
│   ├── gui/                       # JavaFX UI
│   ├── network/                   # Network client
│   ├── controller/                # UI controllers
│   └── config/                    # Client configuration
└── encryption/
    ├── api/                       # Plugin API and SPI
    ├── aes/                       # AES-256 implementation
    └── rsa/                       # RSA implementation
```

### 2.2 Detailed Package Structure

#### 2.2.1 Common Module (`chat-common`)

```
com.chatv2.common/
├── model/
│   ├── UserProfile.java           # User profile record
│   ├── Chat.java                  # Chat metadata record
│   ├── Message.java               # Message record
│   ├── Session.java               # Session record
│   ├── ChatType.java              # Enum: PRIVATE, GROUP
│   └── UserStatus.java            # Enum: ONLINE, OFFLINE, AWAY
│
├── protocol/
│   ├── MessageType.java           # Enum of all message types
│   ├── ChatMessage.java           # Binary message protocol
│   ├── MessageCodec.java          # Message encoder/decoder
│   ├── ProtocolConstants.java    # Protocol constants
│   └── packet/
│       ├── Packet.java           # Base packet class
│       ├── Header.java           # Packet header
│       ├── Body.java             # Packet body
│       └── Checksum.java         # Packet checksum
│
├── crypto/
│   ├── EncryptionResult.java     # Encryption result record
│   ├── KeyManager.java           # Key generation/management
│   ├── CryptoUtils.java          # Cryptographic utilities
│   └── exception/
│       ├── EncryptionException.java
│       └── DecryptionException.java
│
├── exception/
│   ├── ChatException.java        # Base chat exception
│   ├── AuthenticationException.java
│   ├── AuthorizationException.java
│   ├── NetworkException.java
│   └── ValidationException.java
│
└── util/
    ├── IdGenerator.java          # UUID generation utilities
    ├── DateUtils.java            # Date/time utilities
    ├── ByteUtils.java            # Byte array utilities
    └── Constants.java            # Application constants
```

#### 2.2.2 Server Module (`chat-server`)

```
com.chatv2.server/
├── core/
│   ├── ChatServer.java           # Main server entry point
│   ├── ServerConfig.java         # Server configuration record
│   └── BootstrapFactory.java    # Netty bootstrap factory
│
├── manager/
│   ├── UserManager.java          # User management interface
│   ├── UserManagerImpl.java      # User management implementation
│   ├── ChatManager.java          # Chat management interface
│   ├── ChatManagerImpl.java      # Chat management implementation
│   ├── SessionManager.java       # Session management interface
│   ├── SessionManagerImpl.java   # Session management implementation
│   └── MessageManager.java       # Message management interface
│
├── handler/
│   ├── ServerInitializer.java   # Channel initializer
│   ├── AuthHandler.java          # Authentication handler
│   ├── MessageHandler.java       # Message processing handler
│   ├── EncryptionHandler.java   # Encryption/decryption handler
│   ├── SessionHandler.java       # Session validation handler
│   └── ExceptionHandler.java    # Global exception handler
│
├── storage/
│   ├── DatabaseManager.java     # Database connection management
│   ├── UserRepository.java      # User data access
│   ├── ChatRepository.java      # Chat data access
│   ├── SessionRepository.java   # Session data access
│   ├── MessageRepository.java   # Message data access
│   └── migration/
│       ├── SchemaInitializer.java  # Database schema initialization
│       └── DataMigrator.java       # Data migration utilities
│
├── gui/
│   ├── ServerAdminApp.java      # JavaFX Application entry
│   ├── controller/
│   │   ├── DashboardController.java
│   │   ├── UserController.java
│   │   ├── ChatController.java
│   │   └── LogViewerController.java
│   ├── view/
│   │   ├── DashboardView.fxml
│   │   ├── UserManagementView.fxml
│   │   ├── ChatManagementView.fxml
│   │   └── LogViewerView.fxml
│   └── model/
│       ├── ServerStatistics.java
│       ├── UserTableModel.java
│       └── ChatTableModel.java
│
├── plugin/
│   ├── EncryptionPluginManager.java   # Plugin loading/management
│   ├── EncryptionPluginLoader.java    # SPI plugin loader
│   └── EncryptionContext.java         # Encryption context
│
└── config/
    ├── ServerProperties.java    # Server property loading
    └── DefaultConfig.java        # Default configuration values
```

#### 2.2.3 Client Module (`chat-client`)

```
com.chatv2.client/
├── core/
│   ├── ChatClient.java          # Main client entry point
│   ├── ClientConfig.java        # Client configuration record
│   └── ConnectionState.java     # Connection state enum
│
├── discovery/
│   ├── ServerDiscovery.java     # UDP broadcast discovery
│   ├── DiscoveryRequest.java    # Discovery request packet
│   ├── DiscoveryResponse.java   # Discovery response packet
│   ├── ServerInfo.java          # Discovered server info record
│   └── DiscoveryListener.java   # Async discovery listener
│
├── gui/
│   ├── ChatClientApp.java       # JavaFX Application entry
│   ├── scene/
│   │   ├── ServerSelectionScene.java
│   │   ├── LoginScene.java
│   │   ├── ChatScene.java
│   │   └── ProfileScene.java
│   ├── controller/
│   │   ├── ServerSelectionController.java
│   │   ├── LoginController.java
│   │   ├── ChatController.java
│   │   └── ProfileController.java
│   ├── view/
│   │   ├── ServerSelectionView.fxml
│   │   ├── LoginView.fxml
│   │   ├── ChatView.fxml
│   │   └── ProfileView.fxml
│   └── component/
│       ├── MessageBubble.java      # Custom message component
│       ├── UserListCell.java       # Custom list cell for users
│       ├── AvatarImageView.java    # Avatar image component
│       └── StatusIndicator.java    # User status indicator
│
├── network/
│   ├── NetworkClient.java        # Netty TCP client
│   ├── ClientInitializer.java    # Channel initializer
│   ├── ClientHandler.java        # Client message handler
│   ├── ConnectionManager.java    # Connection lifecycle
│   └── ResponseFuture.java       # Async response handling
│
└── config/
    ├── ClientProperties.java    # Client property loading
    └── DefaultConfig.java        # Default configuration values
```

#### 2.2.4 Encryption Module (`chat-encryption-plugins`)

```
com.chatv2.encryption/
├── api/
│   ├── EncryptionPlugin.java        # Plugin interface (SPI)
│   ├── EncryptionType.java         # Encryption type enum
│   ├── EncryptionAlgorithm.java    # Algorithm specification record
│   ├── KeySpec.java                # Key specification record
│   └── EncryptionContext.java      # Encryption context interface
│
├── aes/
│   ├── AesEncryptionPlugin.java    # AES-256 plugin implementation
│   ├── AesKeyGenerator.java        # AES key generation
│   ├── AesEncryptor.java           # AES encryption
│   └── AesDecryptor.java           # AES decryption
│
└── rsa/
    ├── RsaEncryptionPlugin.java    # RSA plugin implementation
    ├── RsaKeyGenerator.java        # RSA key generation
    ├── RsaEncryptor.java           # RSA encryption
    └── RsaDecryptor.java           # RSA decryption
```

---

## 3. Protocol Specification

### 3.1 Binary Message Format

All messages between client and server use a binary protocol with the following structure:

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         MESSAGE STRUCTURE                                    │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌──────────┬──────────┬──────────┬──────────┬────────────────────────────┐  │
│  │  Header  │  Length  │  Body    │  Checksum│    Encrypted Payload       │  │
│  │  4 bytes │  4 bytes │  2 bytes │  4 bytes │      Variable length        │  │
│  └──────────┴──────────┴──────────┴──────────┴────────────────────────────┘  │
│                                                                              │
│  Header: Magic number + Message Type + Version + Flags                      │
│  Length: Total message body length in bytes                                  │
│  Body: Message type-specific data (JSON or binary)                           │
│  Checksum: CRC32 of body for integrity verification                          │
│  Encrypted Payload: AES-256 encrypted body (if encryption enabled)          │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 3.2 Header Format (4 bytes)

```
Bit Layout:
[0-7]   Magic Number (0xCA)
[8-15]  Message Type (enum value)
[16-23] Version (protocol version)
[24-31] Flags (bitfield: ENCRYPTED=0x01, COMPRESSED=0x02)
```

### 3.3 Message Types

```java
public enum MessageType {
    // Discovery Messages (UDP)
    SERVICE_DISCOVERY_REQ(0x01),
    SERVICE_DISCOVERY_RES(0x02),

    // Authentication Messages
    AUTH_REGISTER_REQ(0x10),
    AUTH_REGISTER_RES(0x11),
    AUTH_LOGIN_REQ(0x12),
    AUTH_LOGIN_RES(0x13),
    AUTH_LOGOUT_REQ(0x14),
    AUTH_LOGOUT_RES(0x15),
    AUTH_TOKEN_REFRESH(0x16),

    // Session Messages
    SESSION_CREATE_REQ(0x20),
    SESSION_CREATE_RES(0x21),
    SESSION_VALIDATE_REQ(0x22),
    SESSION_VALIDATE_RES(0x23),
    SESSION_TERMINATE_REQ(0x24),

    // User Messages
    USER_GET_PROFILE_REQ(0x30),
    USER_GET_PROFILE_RES(0x31),
    USER_UPDATE_PROFILE_REQ(0x32),
    USER_UPDATE_PROFILE_RES(0x33),
    USER_SEARCH_REQ(0x34),
    USER_SEARCH_RES(0x35),
    USER_STATUS_UPDATE(0x36),
    USER_ONLINE_LIST(0x37),

    // Chat Messages
    CHAT_CREATE_REQ(0x40),
    CHAT_CREATE_RES(0x41),
    CHAT_LIST_REQ(0x42),
    CHAT_LIST_RES(0x43),
    CHAT_JOIN_REQ(0x44),
    CHAT_JOIN_RES(0x45),
    CHAT_LEAVE_REQ(0x46),
    CHAT_LEAVE_RES(0x47),
    CHAT_INFO_REQ(0x48),
    CHAT_INFO_RES(0x49),

    // Message Messages
    MESSAGE_SEND_REQ(0x50),
    MESSAGE_SEND_RES(0x51),
    MESSAGE_RECEIVE(0x52),
    MESSAGE_HISTORY_REQ(0x53),
    MESSAGE_HISTORY_RES(0x54),
    MESSAGE_READ_RECEIPT(0x55),

    // Group Chat Messages
    GROUP_ADD_MEMBER_REQ(0x60),
    GROUP_ADD_MEMBER_RES(0x61),
    GROUP_REMOVE_MEMBER_REQ(0x62),
    GROUP_REMOVE_MEMBER_RES(0x63),
    GROUP_UPDATE_INFO_REQ(0x64),
    GROUP_UPDATE_INFO_RES(0x65),

    // Server Messages
    SERVER_SHUTDOWN(0xF0),
    SERVER_ERROR(0xF1),
    SERVER_PING(0xF2),
    SERVER_PONG(0xF3);
}
```

### 3.4 Message Body Examples

#### 3.4.1 Authentication Login Request

```json
{
  "username": "john_doe",
  "password": "encrypted_password_base64",
  "clientVersion": "1.0.0",
  "deviceId": "550e8400-e29b-41d4-a716-446655440000"
}
```

#### 3.4.2 Authentication Login Response

```json
{
  "success": true,
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "userProfile": {
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "username": "john_doe",
    "fullName": "John Doe",
    "avatar": "data:image/png;base64,...",
    "bio": "Software developer"
  },
  "expiresIn": 3600
}
```

#### 3.4.3 Message Send Request

```json
{
  "chatId": "chat-uuid-123",
  "content": "Hello, world!",
  "messageType": "TEXT",
  "replyToMessageId": null,
  "attachments": []
}
```

#### 3.4.4 Service Discovery Response (UDP)

```json
{
  "serverName": "Chat Server",
  "serverAddress": "192.168.1.100",
  "serverPort": 8080,
  "version": "1.0.0",
  "maxUsers": 1000,
  "currentUsers": 42,
  "encryptionRequired": true,
  "encryptionType": "AES-256"
}
```

### 3.5 Encryption Details

#### 3.5.1 Key Exchange Protocol

```
1. Client connects to server (TCP)
2. Server sends RSA public key (4096-bit)
3. Client generates AES-256 session key
4. Client encrypts session key with RSA public key
5. Client sends encrypted session key to server
6. Server decrypts session key with RSA private key
7. All subsequent messages encrypted with AES-256
```

#### 3.5.2 AES-256 Configuration

```
Algorithm: AES/GCM/NoPadding
Key Size: 256 bits
IV Size: 128 bits (random per message)
Tag Size: 128 bits (authentication tag)
```

---

## 4. Component Specifications

### 4.1 Server Components

#### 4.1.1 ChatServer

**Responsibilities:**
- Initialize Netty server bootstrap
- Configure channel pipeline
- Start/stop server lifecycle
- Manage event loop groups (using virtual threads)
- Broadcast server availability (UDP)

**Public Interface:**
```java
public class ChatServer {
    public ChatServer(ServerConfig config);
    public CompletableFuture<Void> start();
    public CompletableFuture<Void> stop();
    public ServerState getState();
    public int getConnectedClients();
}
```

**Implementation Requirements:**
- Use `Thread.ofVirtual().factory()` for worker event loops
- Implement graceful shutdown with timeout
- Support hot-reload of configuration
- Expose metrics (connections, messages, errors)

#### 4.1.2 UserManager

**Responsibilities:**
- User registration and validation
- Authentication (username/password)
- Profile management (CRUD)
- User search functionality
- Avatar handling

**Public Interface:**
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

**SOLID Principles:**
- **S**: Only handles user-related operations
- **O**: Extensible for new authentication providers
- **L**: Subtypes can strengthen invariants
- **I**: Split into UserRegistration, UserProfile, UserAuth interfaces
- **D**: Depends on UserRepository abstraction

#### 4.1.3 ChatManager

**Responsibilities:**
- Create private and group chats
- Manage chat participants
- Route messages to appropriate chats
- Handle chat metadata

**Public Interface:**
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

#### 4.1.4 SessionManager

**Responsibilities:**
- Session creation and validation
- Token generation and verification
- Session expiration handling
- Session persistence

**Public Interface:**
```java
public interface SessionManager {
    CompletableFuture<Session> createSession(UUID userId);
    CompletableFuture<Session> validateSession(String token);
    CompletableFuture<Void> refreshSession(String token);
    CompletableFuture<Void> terminateSession(String token);
    CompletableFuture<Void> terminateAllUserSessions(UUID userId);
}
```

### 4.2 Client Components

#### 4.2.1 ServerDiscovery

**Responsibilities:**
- Broadcast discovery request on UDP
- Listen for server responses
- Maintain list of discovered servers
- Periodically refresh server list

**Public Interface:**
```java
public class ServerDiscovery {
    public ServerDiscovery();
    public CompletableFuture<List<ServerInfo>> discoverServers(Duration timeout);
    public void startDiscoveryListener(Consumer<ServerInfo> callback);
    public void stopDiscoveryListener();
}
```

**Configuration:**
- UDP Multicast Address: 239.255.255.250
- UDP Port: 9999
- Broadcast Interval: 5 seconds
- Discovery Timeout: 30 seconds

#### 4.2.2 ChatClient

**Responsibilities:**
- Manage client lifecycle
- Handle GUI scene transitions
- Coordinate network and UI
- Manage offline/online state

**Public Interface:**
```java
public class ChatClient {
    public ChatClient(ClientConfig config);
    public CompletableFuture<Void> connect(String serverHost, int serverPort);
    public CompletableFuture<Void> disconnect();
    public ConnectionState getState();
    public CompletableFuture<Void> sendMessage(Message message);
    public void setMessageConsumer(Consumer<Message> consumer);
}
```

#### 4.2.3 NetworkClient

**Responsibilities:**
- Establish TCP connection to server
- Handle message encoding/decoding
- Manage encryption
- Handle connection failures and reconnection

**Public Interface:**
```java
public class NetworkClient {
    public NetworkClient(EncryptionPlugin encryptionPlugin);
    public CompletableFuture<Void> connect(String host, int port);
    public CompletableFuture<Void> disconnect();
    public CompletableFuture<ChatMessage> sendRequest(ChatMessage request);
    public void setResponseHandler(BiFunction<UUID, ChatMessage, Void> handler);
    public boolean isConnected();
}
```

### 4.3 Encryption Components

#### 4.3.1 EncryptionPlugin (SPI)

**Public Interface:**
```java
public interface EncryptionPlugin {
    String getName();
    String getVersion();
    EncryptionAlgorithm getAlgorithm();
    CompletableFuture<byte[]> encrypt(byte[] plaintext, KeySpec keySpec);
    CompletableFuture<byte[]> decrypt(byte[] ciphertext, KeySpec keySpec);
    CompletableFuture<KeySpec> generateKey();
}
```

#### 4.3.2 EncryptionPluginManager

**Responsibilities:**
- Load plugins from classpath using SPI
- Manage active encryption plugin
- Provide plugin metadata

**Public Interface:**
```java
public class EncryptionPluginManager {
    public EncryptionPluginManager();
    public Map<String, EncryptionPlugin> loadPlugins();
    public void setActivePlugin(String pluginName);
    public EncryptionPlugin getActivePlugin();
}
```

---

## 5. SOLID Principles

### 5.1 Single Responsibility Principle (SRP)

Each class has one reason to change:

| Class | Responsibility | Changes When |
|-------|----------------|---------------|
| `ChatServer` | Server lifecycle and networking | Protocol changes |
| `UserManager` | User business logic | User domain changes |
| `UserRepository` | User data access | Storage technology changes |
| `MessageCodec` | Message encoding/decoding | Protocol format changes |
| `ServerDiscovery` | Server discovery (UDP) | Discovery protocol changes |

### 5.2 Open/Closed Principle (OCP)

**Interfaces for extension:**
```java
// Abstract base class for handlers
public abstract class MessageHandler extends SimpleChannelInboundHandler<ChatMessage> {
    protected abstract boolean canHandle(MessageType type);
    protected abstract void handle0(ChannelHandlerContext ctx, ChatMessage msg);
}

// New handler types can be added without modifying existing code
public class CustomMessageHandler extends MessageHandler {
    @Override
    protected boolean canHandle(MessageType type) {
        return type == MessageType.CUSTOM_MESSAGE;
    }

    @Override
    protected void handle0(ChannelHandlerContext ctx, ChatMessage msg) {
        // Custom handling
    }
}
```

### 5.3 Liskov Substitution Principle (LSP)

**Subtype behavior:**
```java
// All encryption plugins must be interchangeable
public class EncryptionService {
    private final EncryptionPlugin plugin;

    public EncryptionService(EncryptionPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin);
    }

    public CompletableFuture<byte[]> encrypt(byte[] data) {
        return plugin.encrypt(data, plugin.generateKey().join());
    }
}

// Can swap AES plugin for RSA without breaking code
```

### 5.4 Interface Segregation Principle (ISP)

**Split interfaces:**
```java
// Instead of one large interface
public interface UserManager {
    // Auth methods
    CompletableFuture<UserProfile> login(String username, String password);
    CompletableFuture<Void> logout(UUID userId);

    // Profile methods
    CompletableFuture<UserProfile> getProfile(UUID userId);
    CompletableFuture<Void> updateProfile(UUID userId, UserProfile updates);

    // Search methods
    CompletableFuture<List<UserProfile>> searchUsers(String query);
}

// Split into focused interfaces
public interface UserAuthService {
    CompletableFuture<UserProfile> login(String username, String password);
    CompletableFuture<Void> logout(UUID userId);
}

public interface UserProfileService {
    CompletableFuture<UserProfile> getProfile(UUID userId);
    CompletableFuture<Void> updateProfile(UUID userId, UserProfile updates);
}

public interface UserSearchService {
    CompletableFuture<List<UserProfile>> searchUsers(String query);
}
```

### 5.5 Dependency Inversion Principle (DIP)

**High-level modules depend on abstractions:**
```java
// High-level ChatManager depends on repository interface
public class ChatManagerImpl implements ChatManager {
    private final ChatRepository chatRepository;
    private final NotificationService notificationService;

    public ChatManagerImpl(ChatRepository repo, NotificationService notifier) {
        this.chatRepository = repo; // Interface, not concrete implementation
        this.notificationService = notifier;
    }

    @Override
    public CompletableFuture<Chat> createGroupChat(UUID ownerId, String name, Set<UUID> members) {
        // Uses interface methods
        return chatRepository.save(chat)
            .thenCompose(chat -> notificationService.notifyMembers(chat, members));
    }
}
```

---

## 6. Virtual Threads Usage

### 6.1 Virtual Threads in Server

**Boss Event Loop (Platform Thread):**
```java
EventLoopGroup bossGroup = new NioEventLoopGroup(1);
```

**Worker Event Loop (Virtual Threads):**
```java
EventLoopGroup workerGroup = new NioEventLoopGroup(0,
    Thread.ofVirtual().factory());
```

### 6.2 Virtual Threads for Business Logic

**Asynchronous processing with virtual threads:**
```java
public class MessageHandler extends SimpleChannelInboundHandler<ChatMessage> {
    private final ExecutorService executor =
        Executors.newVirtualThreadPerTaskExecutor();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ChatMessage msg) {
        // Offload blocking operations to virtual thread
        executor.submit(() -> {
            try {
                processMessageAsync(ctx, msg).join();
            } catch (Exception e) {
                log.error("Error processing message", e);
            }
        });
    }

    private CompletableFuture<Void> processMessageAsync(ChannelHandlerContext ctx, ChatMessage msg) {
        return CompletableFuture.supplyAsync(() -> {
            // Business logic here
            return messageManager.saveMessage(msg);
        }, executor).thenCompose(savedMsg -> {
            // Chain additional async operations
            return notificationService.notifyRecipients(savedMsg);
        });
    }
}
```

### 6.3 Virtual Threads in Client

**Concurrent operations:**
```java
public class ChatClient {
    private final ExecutorService executor =
        Executors.newVirtualThreadPerTaskExecutor();

    public CompletableFuture<Void> connectAsync(String host, int port) {
        return CompletableFuture.supplyAsync(() -> {
            // Blocking connection operation
            return networkClient.connect(host, port).join();
        }, executor);
    }

    public void sendMultipleMessages(List<Message> messages) {
        messages.forEach(msg ->
            executor.submit(() -> sendMessageAsync(msg))
        );
    }
}
```

### 6.4 Benefits of Virtual Threads

1. **Massive Concurrency:** Handle thousands of concurrent connections
2. **Simplified Code:** Write blocking code without callbacks
3. **Resource Efficiency:** Minimal memory overhead per thread
4. **Better Debugging:** Natural stack traces

---

## 7. Plugin Architecture

### 7.1 SPI (Service Provider Interface)

**Plugin Discovery:**
```java
// META-INF/services/com.chatv2.encryption.api.EncryptionPlugin
com.chatv2.encryption.aes.AesEncryptionPlugin
com.chatv2.encryption.rsa.RsaEncryptionPlugin
```

### 7.2 Plugin Loading

**Plugin Manager Implementation:**
```java
public class EncryptionPluginLoader {
    private static final Logger log = LoggerFactory.getLogger(EncryptionPluginLoader.class);

    public Map<String, EncryptionPlugin> loadPlugins() {
        ServiceLoader<EncryptionPlugin> loader =
            ServiceLoader.load(EncryptionPlugin.class);

        Map<String, EncryptionPlugin> plugins = new ConcurrentHashMap<>();

        loader.forEach(plugin -> {
            String name = plugin.getName();
            log.info("Loading encryption plugin: {} v{}", name, plugin.getVersion());
            plugins.put(name, plugin);
        });

        return plugins;
    }
}
```

### 7.3 Plugin Interface

```java
public interface EncryptionPlugin {
    /**
     * Returns the plugin name
     */
    String getName();

    /**
     * Returns the plugin version
     */
    String getVersion();

    /**
     * Returns the encryption algorithm specification
     */
    EncryptionAlgorithm getAlgorithm();

    /**
     * Encrypts data asynchronously
     */
    CompletableFuture<byte[]> encrypt(byte[] plaintext, KeySpec keySpec);

    /**
     * Decrypts data asynchronously
     */
    CompletableFuture<byte[]> decrypt(byte[] ciphertext, KeySpec keySpec);

    /**
     * Generates a new encryption key
     */
    CompletableFuture<KeySpec> generateKey();

    /**
     * Validates if key spec is compatible with this plugin
     */
    boolean isKeyValid(KeySpec keySpec);
}
```

### 7.4 Plugin Registration

**Dynamic Plugin Loading:**
```java
public class EncryptionPluginManager {
    private final Map<String, EncryptionPlugin> plugins = new ConcurrentHashMap<>();
    private EncryptionPlugin activePlugin;

    public void loadPluginsFromDirectory(Path pluginDir) {
        try (URLClassLoader classLoader = new URLClassLoader(
                new URL[] { pluginDir.toUri().toURL() })) {

            ServiceLoader<EncryptionPlugin> loader =
                ServiceLoader.load(EncryptionPlugin.class, classLoader);

            loader.forEach(plugin -> {
                plugins.put(plugin.getName(), plugin);
                log.info("Loaded plugin: {}", plugin.getName());
            });

        } catch (MalformedURLException e) {
            log.error("Failed to load plugins from directory", e);
        }
    }
}
```

---

## 8. Security Architecture

### 8.1 Authentication Flow

```
Client                          Server
  │                               │
  │───── AUTH_LOGIN_REQ ─────────>│
  │     {username, encryptedPwd}  │
  │                               │
  │                               ├─ Verify credentials
  │                               ├─ Generate session token
  │                               ├─ Store session in DB
  │                               │
  │<──── AUTH_LOGIN_RES ──────────│
  │     {success, token, profile} │
  │                               │
```

### 8.2 Encryption Flow

```
Client                          Server
  │                               │
  │───── TCP Connect ────────────>│
  │                               │
  │<───── RSA Public Key ─────────│
  │                               │
  │  Generate AES Session Key     │
  │  Encrypt with RSA Public Key  │
  │                               │
  │───── Encrypted AES Key ──────>│
  │                               │
  │                               ├─ Decrypt with RSA Private Key
  │                               ├─ Store session key
  │                               │
  │<───── ACK (Ready) ────────────│
  │                               │
  │───── AES-256 Encrypted ──────>│
  │       Message                │
  │                               │
```

### 8.3 Security Best Practices

1. **Password Storage:**
   - Use Argon2id hashing
   - Salt per user
   - Minimum 64-bit salt, 32-byte hash

2. **Session Tokens:**
   - JWT with 256-bit secret
   - Expiration: 1 hour
   - Refresh token: 7 days

3. **Key Management:**
   - Never store private keys in code
   - Use Java KeyStore for RSA keys
   - Generate new AES keys per session

4. **Input Validation:**
   - Sanitize all user inputs
   - Limit message sizes (max 10KB)
   - Rate limit authentication attempts

---

## 9. Database Schema

### 9.1 Users Table

```sql
CREATE TABLE users (
    user_id UUID PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    salt VARCHAR(64) NOT NULL,
    full_name VARCHAR(100),
    avatar_data BLOB,
    bio VARCHAR(500),
    status VARCHAR(20) DEFAULT 'OFFLINE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_username (username)
);
```

### 9.2 Sessions Table

```sql
CREATE TABLE sessions (
    session_id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(user_id),
    token VARCHAR(255) UNIQUE NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_accessed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_token (token),
    INDEX idx_user_id (user_id)
);
```

### 9.3 Chats Table

```sql
CREATE TABLE chats (
    chat_id UUID PRIMARY KEY,
    chat_type VARCHAR(20) NOT NULL, -- 'PRIVATE' or 'GROUP'
    name VARCHAR(100),
    description VARCHAR(500),
    owner_id UUID REFERENCES users(user_id),
    avatar_data BLOB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_type (chat_type),
    INDEX idx_owner (owner_id)
);
```

### 9.4 Chat Participants Table

```sql
CREATE TABLE chat_participants (
    id UUID PRIMARY KEY,
    chat_id UUID NOT NULL REFERENCES chats(chat_id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    role VARCHAR(20) DEFAULT 'MEMBER', -- 'OWNER', 'ADMIN', 'MEMBER'
    UNIQUE (chat_id, user_id),
    INDEX idx_chat (chat_id),
    INDEX idx_user (user_id)
);
```

### 9.5 Messages Table

```sql
CREATE TABLE messages (
    message_id UUID PRIMARY KEY,
    chat_id UUID NOT NULL REFERENCES chats(chat_id) ON DELETE CASCADE,
    sender_id UUID NOT NULL REFERENCES users(user_id),
    content TEXT NOT NULL,
    message_type VARCHAR(20) DEFAULT 'TEXT',
    reply_to_message_id UUID REFERENCES messages(message_id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    edited_at TIMESTAMP,
    deleted_at TIMESTAMP,
    INDEX idx_chat (chat_id, created_at),
    INDEX idx_sender (sender_id),
    INDEX idx_created (created_at)
);
```

### 9.6 Message Read Receipts Table

```sql
CREATE TABLE message_read_receipts (
    receipt_id UUID PRIMARY KEY,
    message_id UUID NOT NULL REFERENCES messages(message_id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(user_id),
    read_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (message_id, user_id),
    INDEX idx_message (message_id),
    INDEX idx_user (user_id)
);
```

---

## 10. Development Roadmap

### Phase 1: Core Infrastructure (Weeks 1-3)

- [ ] Set up Maven multi-module project
- [ ] Create package structure
- [ ] Implement binary protocol (MessageCodec)
- [ ] Create Netty bootstrap factories
- [ ] Implement virtual thread configuration
- [ ] Set up logging framework

### Phase 2: Database Layer (Weeks 4-5)

- [ ] Create database schema
- [ ] Implement Repository pattern
- [ ] Create DatabaseManager
- [ ] Implement data migration scripts
- [ ] Write unit tests for repositories

### Phase 3: Server Core (Weeks 6-8)

- [ ] Implement ChatServer with Netty
- [ ] Create Netty channel handlers
- [ ] Implement UserManager
- [ ] Implement SessionManager
- [ ] Implement ChatManager
- [ ] Add UDP broadcast for discovery

### Phase 4: Encryption & Plugins (Weeks 9-10)

- [ ] Create EncryptionPlugin API
- [ ] Implement AesEncryptionPlugin
- [ ] Implement RsaEncryptionPlugin
- [ ] Create EncryptionPluginManager
- [ ] Implement key exchange protocol

### Phase 5: Server GUI (Weeks 11-12)

- [ ] Create JavaFX admin application
- [ ] Implement dashboard with statistics
- [ ] Create user management UI
- [ ] Create chat management UI
- [ ] Add real-time log viewer

### Phase 6: Client Core (Weeks 13-14)

- [ ] Implement NetworkClient
- [ ] Create ServerDiscovery (UDP)
- [ ] Implement ChatClient
- [ ] Add connection management
- [ ] Implement message handling

### Phase 7: Client GUI (Weeks 15-17)

- [ ] Create JavaFX client application
- [ ] Implement server selection scene
- [ ] Implement login scene
- [ ] Implement chat scene with message bubbles
- [ ] Implement profile scene
- [ ] Add avatar handling

### Phase 8: Testing & Optimization (Weeks 18-20)

- [ ] Write comprehensive unit tests
- [ ] Write integration tests
- [ ] Performance testing with virtual threads
- [ ] Load testing with multiple clients
- [ ] Security audit
- [ ] Code review and refactoring

### Phase 9: Deployment & Documentation (Weeks 21-22)

- [ ] Create deployment scripts
- [ ] Package applications with dependencies
- [ ] Write user documentation
- [ ] Write developer documentation
- [ ] Create demo videos

---

## Appendix A: Configuration Examples

### Server Configuration

```yaml
server:
  host: "0.0.0.0"
  port: 8080
  name: "ChatV2 Server"

udp:
  enabled: true
  multicast_address: "239.255.255.250"
  port: 9999
  broadcast_interval_seconds: 5

database:
  path: "data/chat.db"
  connection_pool_size: 10

encryption:
  required: true
  default_plugin: "AES-256"
  rsa_key_size: 4096
  aes_key_size: 256

session:
  token_expiration_seconds: 3600
  refresh_token_expiration_days: 7

logging:
  level: "INFO"
  file: "logs/server.log"
  max_file_size: "100MB"
  max_files: 10
```

### Client Configuration

```yaml
client:
  name: "ChatV2 Client"
  version: "1.0.0"

discovery:
  enabled: true
  multicast_address: "239.255.255.250"
  port: 9999
  timeout_seconds: 30

connection:
  reconnect_attempts: 5
  reconnect_delay_seconds: 5
  heartbeat_interval_seconds: 30

encryption:
  enabled: true

ui:
  theme: "dark"
  language: "en"
  avatar_size: 64

logging:
  level: "INFO"
  file: "logs/client.log"
```

---

## Appendix B: Error Handling

### Error Codes

| Code | Name | Description |
|------|------|-------------|
| 1000 | SUCCESS | Operation successful |
| 1001 | INVALID_REQUEST | Request format is invalid |
| 1002 | UNAUTHORIZED | Authentication required |
| 1003 | FORBIDDEN | Insufficient permissions |
| 1004 | USER_NOT_FOUND | User does not exist |
| 1005 | INVALID_CREDENTIALS | Username/password incorrect |
| 1006 | USER_EXISTS | Username already taken |
| 1007 | SESSION_EXPIRED | Session token expired |
| 1008 | CHAT_NOT_FOUND | Chat does not exist |
| 1009 | MESSAGE_NOT_FOUND | Message does not exist |
| 1010 | ENCRYPTION_ERROR | Encryption/decryption failed |
| 1011 | NETWORK_ERROR | Network communication error |
| 1012 | INTERNAL_ERROR | Unexpected server error |

---

## Appendix C: Testing Strategy

### Unit Testing

- **Coverage Target:** 80%+
- **Framework:** JUnit 5 + Mockito
- **Test Naming:** `[methodName]_[expectedBehavior]_[inputState]`

### Integration Testing

- Test component interactions
- Use H2 in-memory database
- Mock network components
- Test encryption pipeline

### Performance Testing

- Target: 10,000 concurrent connections
- Message throughput: 100,000 msg/sec
- Virtual thread scalability testing
- Memory profiling

### Security Testing

- Penetration testing
- Encryption strength validation
- Input fuzzing
- Authentication bypass attempts

---

**Document Version:** 1.0.0
**Last Updated:** February 2026
**Author:** ChatV2 Architecture Team
