# ChatV2 Protocol Specification v1.0

## Table of Contents
1. [Overview](#overview)
2. [Binary Message Format](#binary-message-format)
3. [Message Types](#message-types)
4. [Message Payloads](#message-payloads)
5. [Encryption Protocol](#encryption-protocol)
6. [Server Discovery Protocol](#server-discovery-protocol)
7. [Error Handling](#error-handling)
8. [Examples](#examples)

---

## 1. Overview

### 1.1 Protocol Characteristics

- **Transport Protocol:** TCP (for client-server) + UDP (for discovery)
- **Message Format:** Binary with JSON payloads
- **Encryption:** AES-256-GCM for messages, RSA-4096 for key exchange
- **Endianness:** Big-endian (network byte order)
- **Character Encoding:** UTF-8
- **Version:** 1.0

### 1.2 Connection Flow

```
Client                                    Server
  │                                          │
  ├───── TCP Connect ───────────────────────>│
  │                                          │
  ├───── AUTH_HANDSHAKE_REQ ────────────────>│
  │     (RSA public key request)             │
  │                                          │
  ├───── AUTH_HANDSHAKE_RES ────────────────│
  │     { RSA Public Key (PEM) }             │
  │                                          │
  ├───── AUTH_KEY_EXCHANGE_REQ ────────────>│
  │     { AES Key (encrypted with RSA) }     │
  │                                          │
  ├───── AUTH_KEY_EXCHANGE_RES ─────────────│
  │     { success: true }                   │
  │                                          │
  ├───── AUTH_LOGIN_REQ (AES encrypted) ────>│
  │     { username, password }              │
  │                                          │
  ├───── AUTH_LOGIN_RES (AES encrypted) ─────│
  │     { token, userProfile }              │
  │                                          │
  ├───── [Message Exchange] ────────────────>│
  │     (All encrypted with AES)            │
  │                                          │
```

---

## 2. Binary Message Format

### 2.1 Complete Message Structure

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           MESSAGE STRUCTURE                                  │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  Byte Offset    Size (bytes)    Field Name              Description         │
│  ─────────────────────────────────────────────────────────────────────────  │
│                                                                              │
│  0x00           4               MAGIC_NUMBER           Protocol identifier   │
│  0x04           2               MESSAGE_TYPE           Message type enum     │
│  0x06           1               VERSION                Protocol version      │
│  0x07           1               FLAGS                  Bitfield flags         │
│  0x08           16              MESSAGE_ID             Full UUID (msg ID)    │
│  0x18           4               PAYLOAD_LENGTH         Body length in bytes │
│  0x1C           8               TIMESTAMP              Unix epoch (ms)       │
│  0x24           4               CHECKSUM               CRC32 of body        │
│  0x28           PAYLOAD_LENGTH  PAYLOAD                JSON body (optional) │
│                                                                              │
│  HEADER_SIZE    40 bytes                                                         │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 2.2 Header Fields

#### 2.2.1 MAGIC_NUMBER
- **Size:** 4 bytes
- **Value:** `0x43 0x48 0x41 0x54` ("CHAT" in ASCII)
- **Purpose:** Protocol identification and synchronization

#### 2.2.2 MESSAGE_TYPE
- **Size:** 2 bytes
- **Type:** Unsigned short (uint16)
- **Values:** See [Message Types](#message-types)

#### 2.2.3 VERSION
- **Size:** 1 byte
- **Type:** Unsigned byte (uint8)
- **Current Value:** `0x01` (version 1.0)

#### 2.2.4 FLAGS
- **Size:** 1 byte
- **Type:** Unsigned byte (bitfield)

```
Bit 7 (0x80): ENCRYPTED    - Payload is encrypted
Bit 6 (0x40): COMPRESSED   - Payload is compressed (GZIP)
Bit 5 (0x20): URGENT        - High priority message
Bit 4 (0x10): ACK_REQUIRED  - Acknowledgment required
Bit 3 (0x08): REPLY         - This is a reply to previous message
Bit 2-0: RESERVED          - Must be zero
```

#### 2.2.5 MESSAGE_ID
- **Size:** 16 bytes
- **Type:** Full UUID (mostSigBits + leastSigBits)
- **Purpose:** Unique message identifier for correlation and tracking

#### 2.2.6 PAYLOAD_LENGTH
- **Size:** 4 bytes
- **Type:** Unsigned int (uint32)
- **Max Value:** 10,485,760 bytes (~10MB)
- **Purpose:** Length of payload field

#### 2.2.7 TIMESTAMP
- **Size:** 8 bytes
- **Type:** Unsigned long (uint64)
- **Format:** Unix epoch in milliseconds
- **Purpose:** Message creation timestamp

#### 2.2.8 CHECKSUM
- **Size:** 4 bytes
- **Type:** Unsigned int (uint32)
- **Algorithm:** CRC32
- **Purpose:** Integrity verification of payload

### 2.3 Encryption Payload Structure (when FLAGS.ENCRYPTED = 1)

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        ENCRYPTED PAYLOAD STRUCTURE                           │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  Byte Offset    Size (bytes)    Field Name              Description         │
│  ─────────────────────────────────────────────────────────────────────────  │
│                                                                              │
│  0x00           16              IV                     AES Initialization   │
│                                                Vector (random)             │
│  0x10           16              TAG                    GCM authentication   │
│                                                tag                          │
│  0x20           Variable         ENCRYPTED_DATA        AES-256-GCM          │
│                                                encrypted payload          │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Message Types

### 3.1 Type Ranges

| Range | Category |
|-------|----------|
| 0x0000 - 0x00FF | Discovery (UDP) |
| 0x0100 - 0x01FF | Authentication |
| 0x0200 - 0x02FF | Session Management |
| 0x0300 - 0x03FF | User Management |
| 0x0400 - 0x04FF | Chat Management |
| 0x0500 - 0x05FF | Messaging |
| 0x0600 - 0x06FF | File Transfer |
| 0x0700 - 0x07FF | Administration |
| 0x0800 - 0x0FFF | Reserved |
| 0xF000 - 0xFFFF | System/Meta |

### 3.2 Message Type Enum

```java
public enum MessageType {
    // Discovery Messages (0x0000 - 0x00FF)
    SERVICE_DISCOVERY_REQ(0x0001),
    SERVICE_DISCOVERY_RES(0x0002),

    // Authentication Handshake (0x0100 - 0x011F)
    AUTH_HANDSHAKE_REQ(0x0100),
    AUTH_HANDSHAKE_RES(0x0101),
    AUTH_KEY_EXCHANGE_REQ(0x0102),
    AUTH_KEY_EXCHANGE_RES(0x0103),

    // Authentication Messages (0x0120 - 0x01FF)
    AUTH_REGISTER_REQ(0x0120),
    AUTH_REGISTER_RES(0x0121),
    AUTH_LOGIN_REQ(0x0122),
    AUTH_LOGIN_RES(0x0123),
    AUTH_LOGOUT_REQ(0x0124),
    AUTH_LOGOUT_RES(0x0125),
    AUTH_TOKEN_REFRESH(0x0126),
    AUTH_PASSWORD_RESET_REQ(0x0127),
    AUTH_PASSWORD_RESET_RES(0x0128),

    // Session Management (0x0200 - 0x02FF)
    SESSION_VALIDATE_REQ(0x0200),
    SESSION_VALIDATE_RES(0x0201),
    SESSION_INFO_REQ(0x0202),
    SESSION_INFO_RES(0x0203),
    SESSION_TERMINATE(0x0204),

    // User Management (0x0300 - 0x03FF)
    USER_GET_PROFILE_REQ(0x0300),
    USER_GET_PROFILE_RES(0x0301),
    USER_UPDATE_PROFILE_REQ(0x0302),
    USER_UPDATE_PROFILE_RES(0x0303),
    USER_SEARCH_REQ(0x0304),
    USER_SEARCH_RES(0x0305),
    USER_STATUS_UPDATE(0x0306),
    USER_ONLINE_LIST(0x0307),
    USER_GET_AVATAR_REQ(0x0308),
    USER_GET_AVATAR_RES(0x0309),
    USER_SET_AVATAR_REQ(0x030A),
    USER_SET_AVATAR_RES(0x030B),

    // Chat Management (0x0400 - 0x04FF)
    CHAT_CREATE_REQ(0x0400),
    CHAT_CREATE_RES(0x0401),
    CHAT_LIST_REQ(0x0402),
    CHAT_LIST_RES(0x0403),
    CHAT_JOIN_REQ(0x0404),
    CHAT_JOIN_RES(0x0405),
    CHAT_LEAVE_REQ(0x0406),
    CHAT_LEAVE_RES(0x0407),
    CHAT_INFO_REQ(0x0408),
    CHAT_INFO_RES(0x0409),
    CHAT_UPDATE_REQ(0x040A),
    CHAT_UPDATE_RES(0x040B),
    CHAT_DELETE_REQ(0x040C),
    CHAT_DELETE_RES(0x040D),
    CHAT_ADD_PARTICIPANT_REQ(0x040E),
    CHAT_ADD_PARTICIPANT_RES(0x040F),
    CHAT_REMOVE_PARTICIPANT_REQ(0x0410),
    CHAT_REMOVE_PARTICIPANT_RES(0x0411),

    // Messaging (0x0500 - 0x05FF)
    MESSAGE_SEND_REQ(0x0500),
    MESSAGE_SEND_RES(0x0501),
    MESSAGE_RECEIVE(0x0502),
    MESSAGE_HISTORY_REQ(0x0503),
    MESSAGE_HISTORY_RES(0x0504),
    MESSAGE_READ_RECEIPT(0x0505),
    MESSAGE_EDIT_REQ(0x0506),
    MESSAGE_EDIT_RES(0x0507),
    MESSAGE_DELETE_REQ(0x0508),
    MESSAGE_DELETE_RES(0x0509),
    MESSAGE_TYPING_INDICATOR(0x050A),

    // System Messages (0xF000 - 0xFFFF)
    PING(0xF000),
    PONG(0xF001),
    ERROR(0xF002),
    SERVER_SHUTDOWN(0xF003),
    BROADCAST(0xF004);
}
```

---

## 4. Message Payloads

All payloads are JSON strings (UTF-8 encoded), encrypted when FLAGS.ENCRYPTED = 1.

### 4.1 Discovery Messages (UDP)

#### 4.1.1 SERVICE_DISCOVERY_REQ

**Direction:** Client -> Server (UDP Broadcast)
**Flags:** Not encrypted
**Payload:**
```json
{
  "clientId": "550e8400-e29b-41d4-a716-446655440000",
  "clientVersion": "1.0.0",
  "platform": "Windows 11",
  "requestedEncryption": true
}
```

#### 4.1.2 SERVICE_DISCOVERY_RES

**Direction:** Server -> Client (UDP Broadcast)
**Flags:** Not encrypted
**Payload:**
```json
{
  "serverId": "server-uuid-123",
  "serverName": "ChatV2 Server",
  "address": "192.168.1.100",
  "port": 8080,
  "version": "1.0.0",
  "encryptionRequired": true,
  "encryptionType": "AES-256-GCM",
  "currentUsers": 42,
  "maxUsers": 1000,
  "features": [
    "FILE_TRANSFER",
    "VOICE_MESSAGES",
    "ENCRYPTION"
  ]
}
```

### 4.2 Authentication Handshake

#### 4.2.1 AUTH_HANDSHAKE_REQ

**Direction:** Client -> Server
**Flags:** Not encrypted
**Payload:**
```json
{
  "supportedEncryption": ["AES-256-GCM"],
  "rsaKeySize": 4096
}
```

#### 4.2.2 AUTH_HANDSHAKE_RES

**Direction:** Server -> Client
**Flags:** Not encrypted
**Payload:**
```json
{
  "success": true,
  "rsaPublicKeyPEM": "-----BEGIN PUBLIC KEY-----\nMIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKCAgEA...\n-----END PUBLIC KEY-----",
  "sessionId": "session-uuid-123",
  "serverTime": 1735670400000
}
```

#### 4.2.3 AUTH_KEY_EXCHANGE_REQ

**Direction:** Client -> Server
**Flags:** Encrypted with RSA public key
**Payload:**
```json
{
  "encryptedAesKey": "base64_encoded_rsa_encrypted_aes_key",
  "aesKeyDerivation": "random",
  "keyVersion": 1
}
```

#### 4.2.4 AUTH_KEY_EXCHANGE_RES

**Direction:** Server -> Client
**Flags:** Not encrypted
**Payload:**
```json
{
  "success": true,
  "keyEstablished": true,
  "encryptionReady": true
}
```

### 4.3 Authentication Messages (Encrypted)

#### 4.3.1 AUTH_REGISTER_REQ

**Payload:**
```json
{
  "username": "john_doe",
  "password": "encrypted_password_base64",
  "email": "john@example.com",
  "fullName": "John Doe",
  "avatarData": "base64_encoded_image_or_null"
}
```

#### 4.3.2 AUTH_REGISTER_RES

**Payload:**
```json
{
  "success": true,
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "message": "Registration successful"
}
```

#### 4.3.3 AUTH_LOGIN_REQ

**Payload:**
```json
{
  "username": "john_doe",
  "password": "encrypted_password_base64",
  "deviceId": "device-uuid-123",
  "clientVersion": "1.0.0",
  "rememberMe": true
}
```

#### 4.3.4 AUTH_LOGIN_RES

**Payload:**
```json
{
  "success": true,
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "userProfile": {
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "username": "john_doe",
    "fullName": "John Doe",
    "email": "john@example.com",
    "avatarData": "base64_encoded_image_or_null",
    "bio": "Software developer",
    "status": "ONLINE",
    "createdAt": 1735670400000
  },
  "expiresIn": 3600,
  "serverTime": 1735670400000
}
```

### 4.4 Session Management (Encrypted)

#### 4.4.1 SESSION_VALIDATE_REQ

**Payload:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

#### 4.4.2 SESSION_VALIDATE_RES

**Payload:**
```json
{
  "valid": true,
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "expiresAt": 1735674000000
}
```

### 4.5 User Management (Encrypted)

#### 4.5.1 USER_GET_PROFILE_REQ

**Payload:**
```json
{
  "userId": "550e8400-e29b-41d4-a716-446655440000"
}
```

#### 4.5.2 USER_GET_PROFILE_RES

**Payload:**
```json
{
  "success": true,
  "userProfile": {
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "username": "john_doe",
    "fullName": "John Doe",
    "email": "john@example.com",
    "avatarData": "base64_encoded_image_or_null",
    "bio": "Software developer",
    "status": "ONLINE",
    "createdAt": 1735670400000
  }
}
```

#### 4.5.3 USER_UPDATE_PROFILE_REQ

**Payload:**
```json
{
  "updates": {
    "fullName": "John Updated",
    "bio": "Updated bio",
    "avatarData": "base64_encoded_new_avatar"
  }
}
```

#### 4.5.4 USER_UPDATE_PROFILE_RES

**Payload:**
```json
{
  "success": true,
  "userProfile": {
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "username": "john_doe",
    "fullName": "John Updated",
    "email": "john@example.com",
    "avatarData": "base64_encoded_new_avatar",
    "bio": "Updated bio",
    "status": "ONLINE"
  }
}
```

#### 4.5.5 USER_SEARCH_REQ

**Payload:**
```json
{
  "query": "john",
  "limit": 20,
  "offset": 0
}
```

#### 4.5.6 USER_SEARCH_RES

**Payload:**
```json
{
  "success": true,
  "results": [
    {
      "userId": "550e8400-e29b-41d4-a716-446655440000",
      "username": "john_doe",
      "fullName": "John Doe",
      "avatarData": "base64_encoded_image_or_null",
      "status": "ONLINE"
    }
  ],
  "total": 1,
  "hasMore": false
}
```

#### 4.5.7 USER_STATUS_UPDATE

**Direction:** Bidirectional
**Payload:**
```json
{
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "ONLINE",
  "statusMessage": "Available for chat"
}
```

#### 4.5.8 USER_ONLINE_LIST

**Direction:** Server -> Client
**Payload:**
```json
{
  "onlineUsers": [
    {
      "userId": "550e8400-e29b-41d4-a716-446655440000",
      "username": "john_doe",
      "status": "ONLINE"
    }
  ],
  "totalCount": 42
}
```

### 4.6 Chat Management (Encrypted)

#### 4.6.1 CHAT_CREATE_REQ

**Payload:**
```json
{
  "chatType": "GROUP",
  "name": "Project Team",
  "description": "Discussion about the project",
  "participantIds": [
    "550e8400-e29b-41d4-a716-446655440001",
    "550e8400-e29b-41d4-a716-446655440002"
  ]
}
```

#### 4.6.2 CHAT_CREATE_RES

**Payload:**
```json
{
  "success": true,
  "chat": {
    "chatId": "chat-uuid-123",
    "chatType": "GROUP",
    "name": "Project Team",
    "description": "Discussion about the project",
    "ownerId": "550e8400-e29b-41d4-a716-446655440000",
    "avatarData": null,
    "createdAt": 1735670400000,
    "participantCount": 3
  }
}
```

#### 4.6.3 CHAT_LIST_REQ

**Payload:**
```json
{
  "limit": 50,
  "offset": 0,
  "chatType": null
}
```

#### 4.6.4 CHAT_LIST_RES

**Payload:**
```json
{
  "success": true,
  "chats": [
    {
      "chatId": "chat-uuid-123",
      "chatType": "GROUP",
      "name": "Project Team",
      "description": "Discussion about the project",
      "avatarData": "base64_encoded_image_or_null",
      "participantCount": 3,
      "unreadCount": 5,
      "lastMessage": {
        "messageId": "msg-uuid-456",
        "content": "See you tomorrow!",
        "senderId": "550e8400-e29b-41d4-a716-446655440001",
        "timestamp": 1735670400000
      },
      "lastActivity": 1735670400000
    }
  ],
  "total": 10,
  "hasMore": true
}
```

#### 4.6.5 CHAT_JOIN_REQ

**Payload:**
```json
{
  "chatId": "chat-uuid-123"
}
```

#### 4.6.6 CHAT_JOIN_RES

**Payload:**
```json
{
  "success": true,
  "chatId": "chat-uuid-123",
  "role": "MEMBER"
}
```

#### 4.6.7 CHAT_LEAVE_REQ

**Payload:**
```json
{
  "chatId": "chat-uuid-123"
}
```

#### 4.6.8 CHAT_LEAVE_RES

**Payload:**
```json
{
  "success": true
}
```

### 4.7 Messaging (Encrypted)

#### 4.7.1 MESSAGE_SEND_REQ

**Payload:**
```json
{
  "chatId": "chat-uuid-123",
  "content": "Hello, everyone!",
  "messageType": "TEXT",
  "replyToMessageId": null,
  "attachments": []
}
```

#### 4.7.2 MESSAGE_SEND_RES

**Payload:**
```json
{
  "success": true,
  "message": {
    "messageId": "msg-uuid-789",
    "chatId": "chat-uuid-123",
    "senderId": "550e8400-e29b-41d4-a716-446655440000",
    "content": "Hello, everyone!",
    "messageType": "TEXT",
    "replyToMessageId": null,
    "createdAt": 1735670400000,
    "editedAt": null
  }
}
```

#### 4.7.3 MESSAGE_RECEIVE

**Direction:** Server -> Client (Push)
**Payload:**
```json
{
  "message": {
    "messageId": "msg-uuid-789",
    "chatId": "chat-uuid-123",
    "senderId": "550e8400-e29b-41d4-a716-446655440000",
    "senderUsername": "john_doe",
    "senderAvatar": "base64_encoded_avatar_or_null",
    "content": "Hello, everyone!",
    "messageType": "TEXT",
    "replyToMessageId": null,
    "attachments": [],
    "createdAt": 1735670400000,
    "editedAt": null
  }
}
```

#### 4.7.4 MESSAGE_HISTORY_REQ

**Payload:**
```json
{
  "chatId": "chat-uuid-123",
  "limit": 50,
  "offset": 0,
  "beforeMessageId": null
}
```

#### 4.7.5 MESSAGE_HISTORY_RES

**Payload:**
```json
{
  "success": true,
  "messages": [
    {
      "messageId": "msg-uuid-789",
      "chatId": "chat-uuid-123",
      "senderId": "550e8400-e29b-41d4-a716-446655440000",
      "senderUsername": "john_doe",
      "content": "Hello, everyone!",
      "messageType": "TEXT",
      "replyToMessageId": null,
      "createdAt": 1735670400000,
      "editedAt": null,
      "readBy": ["550e8400-e29b-41d4-a716-446655440001"]
    }
  ],
  "total": 100,
  "hasMore": true
}
```

#### 4.7.6 MESSAGE_READ_RECEIPT

**Direction:** Bidirectional
**Payload:**
```json
{
  "messageId": "msg-uuid-789",
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "readAt": 1735670400000
}
```

#### 4.7.7 MESSAGE_TYPING_INDICATOR

**Direction:** Bidirectional
**Payload:**
```json
{
  "chatId": "chat-uuid-123",
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "isTyping": true
}
```

### 4.8 System Messages (Not Encrypted)

#### 4.8.1 PING

**Payload:**
```json
{
  "timestamp": 1735670400000
}
```

#### 4.8.2 PONG

**Payload:**
```json
{
  "timestamp": 1735670400000,
  "serverTime": 1735670400001
}
```

#### 4.8.3 ERROR

**Payload:**
```json
{
  "code": 1005,
  "name": "INVALID_CREDENTIALS",
  "message": "Username or password is incorrect",
  "details": {
    "attempt": 3,
    "remainingAttempts": 2
  }
}
```

#### 4.8.4 SERVER_SHUTDOWN

**Direction:** Server -> Client
**Payload:**
```json
{
  "reason": "Scheduled maintenance",
  "shutdownAt": 1735671000000,
  "message": "Server will be shut down for maintenance in 10 minutes"
}
```

---

## 5. Encryption Protocol

### 5.1 Key Exchange

1. **Client connects** to server via TCP
2. **Client sends** `AUTH_HANDSHAKE_REQ`
3. **Server responds** with `AUTH_HANDSHAKE_RES` containing RSA-4096 public key in PEM format
4. **Client generates** random AES-256 session key
5. **Client encrypts** AES key with RSA public key (OAEP padding)
6. **Client sends** `AUTH_KEY_EXCHANGE_REQ` with encrypted AES key
7. **Server decrypts** AES key with RSA private key
8. **Server responds** with `AUTH_KEY_EXCHANGE_RES`
9. **All subsequent messages** encrypted with AES-256-GCM

### 5.2 AES-256-GCM Configuration

```
Algorithm:          AES/GCM/NoPadding
Key Size:           256 bits (32 bytes)
IV Size:            96 bits (12 bytes) - random per message
Tag Size:           128 bits (16 bytes) - authentication tag
Key Derivation:     SecureRandom (direct)
Nonce Management:   Random IV per message (no counter)
```

### 5.3 Encryption Process

1. Generate random IV (12 bytes)
2. Encrypt payload with AES-256-GCM using IV and session key
3. Obtain authentication tag (16 bytes)
4. Construct encrypted payload: [IV (12) | Tag (16) | Encrypted Data (N)]
5. Set FLAGS.ENCRYPTED = 1 in message header
6. Set FLAGS.COMRESSED = 1 if payload compressed

### 5.4 Decryption Process

1. Check FLAGS.ENCRYPTED = 1
2. Extract IV from first 12 bytes
3. Extract authentication tag from bytes 12-27
4. Extract encrypted data from byte 28 onwards
5. Decrypt with AES-256-GCM using IV, tag, and session key
6. Verify authentication tag (automatic in GCM mode)

### 5.5 Key Rotation

**Server-side:**
- Generate new session key every 24 hours
- Send `AUTH_KEY_ROTATION` notification to clients
- Continue accepting old key for 10 minutes (grace period)

**Client-side:**
- Accept key rotation notification
- Perform key exchange with new key
- Switch to new key immediately

---

## 6. Server Discovery Protocol (UDP)

### 6.1 Configuration

```
Multicast Address: 239.255.255.250
Port:              9999
Protocol:          UDP
TTL:               4 (same subnet)
Interval:          5 seconds (server broadcast)
Timeout:           30 seconds (client discovery)
```

### 6.2 Server Broadcast

Every 5 seconds, server broadcasts `SERVICE_DISCOVERY_RES` to multicast group.

### 6.3 Client Discovery

1. Client sends `SERVICE_DISCOVERY_REQ` to multicast group
2. Client listens for `SERVICE_DISCOVERY_RES` for 30 seconds
3. Client maintains list of discovered servers with timestamps
4. Servers older than 60 seconds are removed from list
5. Client can refresh list at any time

---

## 7. Error Handling

### 7.1 Error Codes

| Code  | Name                    | Category       |
|-------|-------------------------|----------------|
| 1000  | SUCCESS                 | Success        |
| 1001  | INVALID_REQUEST         | Client Error   |
| 1002  | UNAUTHORIZED            | Auth Error     |
| 1003  | FORBIDDEN               | Auth Error     |
| 1004  | USER_NOT_FOUND          | Client Error   |
| 1005  | INVALID_CREDENTIALS     | Auth Error     |
| 1006  | USER_EXISTS             | Client Error   |
| 1007  | SESSION_EXPIRED         | Auth Error     |
| 1008  | CHAT_NOT_FOUND          | Client Error   |
| 1009  | MESSAGE_NOT_FOUND       | Client Error   |
| 1010  | ENCRYPTION_ERROR        | Server Error   |
| 1011  | NETWORK_ERROR           | Server Error   |
| 1012  | INTERNAL_ERROR          | Server Error   |
| 1013  | RATE_LIMIT_EXCEEDED     | Client Error   |
| 1014  | INVALID_TOKEN           | Auth Error     |
| 1015  | INSUFFICIENT_PERMISSION | Auth Error     |
| 1016  | QUOTA_EXCEEDED          | Client Error   |
| 1017  | MALFORMED_PAYLOAD       | Client Error   |
| 1018  | CHECKSUM_MISMATCH       | Protocol Error |
| 1019  | UNSUPPORTED_VERSION     | Protocol Error |
| 1020  | INVALID_ENCRYPTION      | Protocol Error |

### 7.2 Error Response Format

```json
{
  "code": 1005,
  "name": "INVALID_CREDENTIALS",
  "message": "Username or password is incorrect",
  "details": {
    "attempt": 3,
    "remainingAttempts": 2,
    "lockoutTime": null
  },
  "requestId": "msg-uuid-123"
}
```

### 7.3 Error Handling Flow

1. **Error detected** by server
2. **Generate ERROR message** with appropriate code
3. **Send ERROR response** to client
4. **Client receives ERROR**
5. **Client logs error** and displays to user
6. **Client may retry** (if appropriate) or show error message

---

## 8. Examples

### 8.1 Complete Message Example (Hex)

**Raw Message (Unencrypted):**
```
Header:
  43 48 41 54                       # MAGIC_NUMBER "CHAT"
  01 20                            # MESSAGE_TYPE 0x0120 (AUTH_REGISTER_REQ)
  01                               # VERSION 1
  00                               # FLAGS (not encrypted)
  55 0E 84 00 E2 9B 41 D4 71 64 44 66 55 44 00 00  # MESSAGE_ID (UUID mostSigBits)
  00 00 00 00 00 00 00 00          # MESSAGE_ID (UUID leastSigBits)
  00 00 01 56                      # PAYLOAD_LENGTH (342 bytes)
  00 00 00 67 42 0F 00             # TIMESTAMP (1735670400000 ms)
  A1 B2 C3 D4                      # CHECKSUM (CRC32)

Payload (JSON):
  {
    "username": "john_doe",
    "password": "encrypted_password_base64",
    "email": "john@example.com",
    "fullName": "John Doe",
    "avatarData": null
  }
```

### 8.2 Complete Encrypted Message Example

**Encrypted Message Structure:**
```
Header:
  43 48 41 54                       # MAGIC_NUMBER "CHAT"
  01 22                            # MESSAGE_TYPE 0x0122 (AUTH_LOGIN_REQ)
  01                               # VERSION 1
  80                               # FLAGS (encrypted)
  55 0E 84 00 E2 9B 41 D4 71 64 44 66 55 44 00 01  # MESSAGE_ID (UUID mostSigBits)
  00 00 00 00 00 00 00 00          # MESSAGE_ID (UUID leastSigBits)
  00 00 00 88                      # PAYLOAD_LENGTH (136 bytes = 16 IV + 16 Tag + 104 Data)
  00 00 00 67 42 0F 00             # TIMESTAMP
  1A 2B 3C 4D                      # CHECKSUM

Encrypted Payload:
  [IV (16 bytes)]                  # Random initialization vector
  [Tag (16 bytes)]                 # GCM authentication tag
  [Encrypted Data (104 bytes)]     # AES-256-GCM encrypted JSON payload
```

### 8.3 Message Flow Example

```
Client                              Server
  │                                   │
  │──── TCP Connect ──────────────────>│
  │                                   │
  │──── AUTH_HANDSHAKE_REQ ───────────>│
  │     { "rsaKeySize": 4096 }        │
  │                                   │
  │<──── AUTH_HANDSHAKE_RES ───────────│
  │     { "rsaPublicKeyPEM": "..." }  │
  │                                   │
  │──── AUTH_KEY_EXCHANGE_REQ ───────>│
  │     { "encryptedAesKey": "..." }  │
  │                                   │
  │<──── AUTH_KEY_EXCHANGE_RES ───────│
  │     { "success": true }           │
  │                                   │
  │──── AUTH_LOGIN_REQ (encrypted) ──>│
  │     { "username": "john_doe",     │
  │       "password": "..." }         │
  │                                   │
  │<──── AUTH_LOGIN_RES (encrypted) ──│
  │     { "success": true,           │
  │       "token": "...",             │
  │       "userProfile": {...} }      │
  │                                   │
  │──── CHAT_LIST_REQ (encrypted) ───>│
  │     { "limit": 50 }               │
  │                                   │
  │<──── CHAT_LIST_RES (encrypted) ───│
  │     { "success": true,            │
  │       "chats": [...] }            │
  │                                   │
  │──── MESSAGE_SEND_REQ (encrypted)─>│
  │     { "chatId": "...",             │
  │       "content": "Hello!" }        │
  │                                   │
  │<──── MESSAGE_SEND_RES (encrypted)─│
  │     { "success": true,            │
  │       "message": {...} }          │
  │                                   │
  │<──── MESSAGE_RECEIVE (encrypted)─>│
  │     { "message": {...} }          │
  │                                   │
```

---

## Appendix A: CRC32 Implementation

```java
import java.util.zip.CRC32;

public class ChecksumUtils {
    public static int calculateChecksum(byte[] data) {
        CRC32 crc32 = new CRC32();
        crc32.update(data);
        return (int) crc32.getValue();
    }

    public static boolean validateChecksum(byte[] data, int expectedChecksum) {
        return calculateChecksum(data) == expectedChecksum;
    }
}
```

## Appendix B: Byte Order Conversion

```java
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class ByteOrderUtils {
    public static int readInt(byte[] data, int offset) {
        return ByteBuffer.wrap(data, offset, 4)
            .order(ByteOrder.BIG_ENDIAN)
            .getInt();
    }

    public static void writeInt(byte[] data, int offset, int value) {
        ByteBuffer.wrap(data, offset, 4)
            .order(ByteOrder.BIG_ENDIAN)
            .putInt(value);
    }

    public static long readLong(byte[] data, int offset) {
        return ByteBuffer.wrap(data, offset, 8)
            .order(ByteOrder.BIG_ENDIAN)
            .getLong();
    }

    public static void writeLong(byte[] data, int offset, long value) {
        ByteBuffer.wrap(data, offset, 8)
            .order(ByteOrder.BIG_ENDIAN)
            .putLong(value);
    }
}
```

---

**Document Version:** 1.0.0
**Last Updated:** February 2026
**Protocol Version:** 1.0
