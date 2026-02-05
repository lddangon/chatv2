# ChatV2 - Professional Local Chat Application

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![Maven](https://img.shields.io/badge/Maven-3.9+-red.svg)](https://maven.apache.org/)
[![Netty](https://img.shields.io/badge/Netty-4.1.109-blue.svg)](https://netty.io/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

Professional local chat application with client-server architecture, built on Java 21 with modern features including virtual threads, AES-256 encryption, and JavaFX GUI.

## Features

- **Modern Java 21**: Leverages virtual threads for massive concurrency
- **Secure Communication**: AES-256-GCM encryption with RSA-4096 key exchange
- **Auto-Discovery**: Automatic server discovery via UDP broadcast
- **Rich GUI**: JavaFX-based user interface for both client and server admin
- **Plugin Architecture**: Extensible encryption plugins via SPI
- **Full Featured**: User profiles, avatars, private/group chats, message history
- **Embedded Database**: H2 database for data persistence
- **Real-time**: Instant messaging with typing indicators and read receipts

## Architecture

```
┌──────────────────┐         UDP/TCP         ┌──────────────────┐
│   ChatClient     │ ◄──── Discovery ────── │   ChatServer     │
│  (JavaFX GUI)    │ ◄──── Encrypted ────── │  (Netty Server)  │
└──────────────────┘         AES-256          └──────────────────┘
                                                   │
                                            ┌──────┴──────┐
                                            │   H2 DB     │
                                            └─────────────┘
```

## Technology Stack

| Technology | Version | Purpose |
|------------|---------|---------|
| Java | 21 (LTS) | Core language with virtual threads |
| Netty | 4.1.109.Final | Async network framework |
| JavaFX | 21.0.1 | Modern desktop GUI |
| H2 Database | 2.2.224 | Embedded SQL database |
| Bouncy Castle | 1.77 | Strong cryptography |
| JUnit | 5.10.2 | Unit and integration testing |
| Log4j2 | 2.23.1 | Logging framework |
| Maven | 3.9.6 | Build and dependency management |

## Quick Start

### Prerequisites

- Java 21 or later
- Maven 3.9 or later
- (Optional) JavaFX SDK (usually included with JDK 21)

### Building the Project

```bash
# Clone the repository
git clone <repository-url>
cd Chatv2

# Build all modules
mvn clean install

# Skip tests
mvn clean install -DskipTests

# Build specific module
mvn clean install -pl chat-server

# Package applications
mvn clean package
```

### Running the Server

```bash
# Navigate to server launcher
cd chat-apps/chat-server-launcher

# Run the server
java -jar target/chat-server-launcher-1.0.0-jar-with-dependencies.jar

# Or using Maven
mvn exec:java -Dexec.mainClass="com.chatv2.launcher.server.ServerLauncher"
```

The server will:
- Start on port `8080` (configurable)
- Listen for UDP broadcasts on port `9999`
- Initialize embedded H2 database in `data/chat.db`
- Launch admin GUI (JavaFX)

### Running the Client

```bash
# Navigate to client launcher
cd chat-apps/chat-client-launcher

# Run the client
java -jar target/chat-client-launcher-1.0.0-jar-with-dependencies.jar

# Or using Maven
mvn exec:java -Dexec.mainClass="com.chatv2.launcher.client.ClientLauncher"
```

The client will:
- Show server selection screen (auto-discovered servers)
- Prompt for login or registration
- Display main chat interface

## Configuration

### Server Configuration

Create/edit `config/server-config.yaml`:

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

session:
  token_expiration_seconds: 3600
  refresh_token_expiration_days: 7

logging:
  level: "INFO"
  file: "logs/server.log"
```

### Client Configuration

Create/edit `config/client-config.yaml`:

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
```

## Project Structure

```
chatv2/
├── pom.xml                                    # Root Maven POM
├── ARCHITECTURE.md                            # Detailed architecture
├── PROTOCOL_SPEC.md                           # Protocol specification
├── DEVELOPMENT_PLAN.md                        # Development roadmap
├── README.md                                  # This file
│
├── chat-common/                               # Shared module
│   ├── pom.xml
│   └── src/main/java/com/chatv2/common/
│       ├── model/                             # Data models
│       ├── protocol/                          # Binary protocol
│       ├── crypto/                            # Crypto utilities
│       └── exception/                         # Custom exceptions
│
├── chat-server/                               # Server module
│   ├── pom.xml
│   └── src/main/java/com/chatv2/server/
│       ├── core/                              # Server core
│       ├── manager/                           # Business logic
│       ├── handler/                           # Netty handlers
│       ├── storage/                           # Database access
│       └── gui/                               # Admin interface
│
├── chat-client/                               # Client module
│   ├── pom.xml
│   └── src/main/java/com/chatv2/client/
│       ├── core/                              # Client core
│       ├── discovery/                         # UDP discovery
│       ├── gui/                               # JavaFX UI
│       └── network/                           # Network client
│
├── chat-encryption-plugins/                   # Encryption plugins
│   ├── chat-encryption-api/                   # Plugin API
│   ├── chat-encryption-aes/                   # AES-256 plugin
│   └── chat-encryption-rsa/                   # RSA plugin
│
└── chat-apps/                                 # Application launchers
    ├── chat-server-launcher/
    └── chat-client-launcher/
```

## Documentation

- [Architecture](ARCHITECTURE.md) - Complete system architecture and design
- [Protocol Specification](PROTOCOL_SPEC.md) - Binary protocol details
- [Development Plan](DEVELOPMENT_PLAN.md) - Step-by-step implementation plan

## Protocol

The application uses a binary protocol over TCP with the following characteristics:

- **Header:** 28 bytes (magic number, type, version, flags, UUID, length, checksum)
- **Payload:** JSON-encoded data
- **Encryption:** AES-256-GCM for all authenticated messages
- **Key Exchange:** RSA-4096 for initial handshake
- **Discovery:** UDP broadcast on 239.255.255.250:9999

See [PROTOCOL_SPEC.md](PROTOCOL_SPEC.md) for complete details.

## Encryption

### Default Encryption Configuration

- **Algorithm:** AES-256-GCM
- **Key Size:** 256 bits
- **IV Size:** 128 bits (random per message)
- **Tag Size:** 128 bits (authentication)
- **Key Exchange:** RSA-4096 with OAEP padding

### Key Exchange Flow

1. Client connects to server
2. Server sends RSA public key (4096-bit)
3. Client generates AES-256 session key
4. Client encrypts session key with RSA public key
5. Server decrypts with RSA private key
6. All subsequent messages encrypted with AES-256

## Development

### Setting Up IDE

#### IntelliJ IDEA

1. Open the project as Maven project
2. Ensure Java 21 SDK is selected
3. Enable annotation processing if needed
4. Run tests: Right-click project -> Run Tests

#### Eclipse

1. Import as Maven project
2. Configure Java 21 JRE
3. Update Maven project
4. Run as JUnit test

### Running Tests

```bash
# Run all tests
mvn test

# Run tests for specific module
mvn test -pl chat-server

# Run specific test class
mvn test -Dtest=UserManagerTest

# Generate coverage report
mvn test jacoco:report
```

### Code Quality

```bash
# Check code style
mvn checkstyle:check

# Find bugs
mvn spotbugs:check

# Run PMD analysis
mvn pmd:check

# Full quality check
mvn verify
```

## Security

### Security Best Practices

1. **Passwords**: Stored as Argon2id hashes with per-user salt
2. **Session Tokens**: JWT with 256-bit secret, 1-hour expiration
3. **Encryption**: AES-256-GCM with authenticated encryption
4. **Key Management**: RSA keys stored in Java KeyStore
5. **Input Validation**: All user inputs sanitized and validated
6. **Rate Limiting**: Brute-force protection on authentication

### Security Audits

- SQL Injection prevention
- XSS protection (if web features added)
- CSRF protection
- Timing attack mitigation
- Secure random number generation

## Performance

### Benchmarks

| Metric | Target | Actual |
|--------|--------|--------|
| Concurrent Connections | 10,000+ | TBD |
| Message Throughput | 100,000 msg/s | TBD |
| Memory per Connection | < 10KB | TBD |
| Startup Time (Server) | < 5 sec | TBD |
| Latency (avg) | < 50ms | TBD |

### Optimization

- Virtual threads for massive concurrency
- Connection pooling for database
- Asynchronous I/O with Netty
- Message compression for large payloads
- Efficient binary protocol

## Troubleshooting

### Server Won't Start

- Check if port 8080 is available
- Verify Java 21 is installed: `java -version`
- Check firewall settings for UDP/TCP ports
- Review logs in `logs/server.log`

### Client Can't Find Server

- Ensure UDP broadcast is enabled in server config
- Check firewall allows UDP on port 9999
- Verify both client and server on same network
- Try manual server address entry

### Connection Issues

- Verify encryption settings match
- Check network connectivity
- Review server logs for errors
- Ensure server is not at capacity

## Contributing

Contributions are welcome! Please follow these guidelines:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Code Style

- Follow Java Code Conventions
- Use 4-space indentation
- Add JavaDoc for all public APIs
- Write unit tests for new features
- Ensure all tests pass before PR

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- Netty team for the excellent network framework
- JavaFX community for modern GUI toolkit
- Bouncy Castle for cryptography library
- All contributors to this project

## Contact

- Project Lead: [Your Name]
- Email: your.email@example.com
- Issues: [GitHub Issues](https://github.com/yourusername/chatv2/issues)

## Roadmap

- [x] Core architecture
- [x] Binary protocol
- [x] Encryption plugins
- [ ] Complete GUI implementation
- [ ] File transfer support
- [ ] Voice messages
- [ ] Video calls
- [ ] Mobile client (Android/iOS)
- [ ] Web client (WebSocket)
- [ ] Multi-language support
- [ ] Plugin marketplace

---

**Version:** 1.0.0
**Last Updated:** February 2026
**Status:** In Development
