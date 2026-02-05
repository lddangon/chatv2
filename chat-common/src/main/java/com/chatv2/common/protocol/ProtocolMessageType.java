package com.chatv2.common.protocol;

/**
 * Enum of all message types in the ChatV2 protocol.
 * Message types are organized by functional category.
 */
public enum ProtocolMessageType {
    // Discovery Messages (0x0000 - 0x00FF) - UDP
    SERVICE_DISCOVERY_REQ(0x0001, false),
    SERVICE_DISCOVERY_RES(0x0002, false),

    // Authentication Handshake (0x0100 - 0x011F)
    AUTH_HANDSHAKE_REQ(0x0100, false),
    AUTH_HANDSHAKE_RES(0x0101, false),
    AUTH_KEY_EXCHANGE_REQ(0x0102, true),
    AUTH_KEY_EXCHANGE_RES(0x0103, false),

    // Authentication Messages (0x0120 - 0x01FF)
    AUTH_REGISTER_REQ(0x0120, false),
    AUTH_REGISTER_RES(0x0121, false),
    AUTH_LOGIN_REQ(0x0122, false),
    AUTH_LOGIN_RES(0x0123, false),
    AUTH_LOGOUT_REQ(0x0124, false),
    AUTH_LOGOUT_RES(0x0125, false),
    AUTH_TOKEN_REFRESH(0x0126, false),
    AUTH_PASSWORD_RESET_REQ(0x0127, false),
    AUTH_PASSWORD_RESET_RES(0x0128, false),

    // Session Management (0x0200 - 0x02FF)
    SESSION_VALIDATE_REQ(0x0200, false),
    SESSION_VALIDATE_RES(0x0201, false),
    SESSION_INFO_REQ(0x0202, false),
    SESSION_INFO_RES(0x0203, false),
    SESSION_TERMINATE(0x0204, false),

    // User Management (0x0300 - 0x03FF)
    USER_GET_PROFILE_REQ(0x0300, false),
    USER_GET_PROFILE_RES(0x0301, false),
    USER_UPDATE_PROFILE_REQ(0x0302, false),
    USER_UPDATE_PROFILE_RES(0x0303, false),
    USER_SEARCH_REQ(0x0304, false),
    USER_SEARCH_RES(0x0305, false),
    USER_STATUS_UPDATE(0x0306, false),
    USER_ONLINE_LIST(0x0307, false),
    USER_GET_AVATAR_REQ(0x0308, false),
    USER_GET_AVATAR_RES(0x0309, false),
    USER_SET_AVATAR_REQ(0x030A, false),
    USER_SET_AVATAR_RES(0x030B, false),

    // Chat Management (0x0400 - 0x04FF)
    CHAT_CREATE_REQ(0x0400, false),
    CHAT_CREATE_RES(0x0401, false),
    CHAT_LIST_REQ(0x0402, false),
    CHAT_LIST_RES(0x0403, false),
    CHAT_JOIN_REQ(0x0404, false),
    CHAT_JOIN_RES(0x0405, false),
    CHAT_LEAVE_REQ(0x0406, false),
    CHAT_LEAVE_RES(0x0407, false),
    CHAT_INFO_REQ(0x0408, false),
    CHAT_INFO_RES(0x0409, false),
    CHAT_UPDATE_REQ(0x040A, false),
    CHAT_UPDATE_RES(0x040B, false),
    CHAT_DELETE_REQ(0x040C, false),
    CHAT_DELETE_RES(0x040D, false),
    CHAT_ADD_PARTICIPANT_REQ(0x040E, false),
    CHAT_ADD_PARTICIPANT_RES(0x040F, false),
    CHAT_REMOVE_PARTICIPANT_REQ(0x0410, false),
    CHAT_REMOVE_PARTICIPANT_RES(0x0411, false),

    // Messaging (0x0500 - 0x05FF)
    MESSAGE_SEND_REQ(0x0500, false),
    MESSAGE_SEND_RES(0x0501, false),
    MESSAGE_RECEIVE(0x0502, false),
    MESSAGE_HISTORY_REQ(0x0503, false),
    MESSAGE_HISTORY_RES(0x0504, false),
    MESSAGE_READ_RECEIPT(0x0505, false),
    MESSAGE_EDIT_REQ(0x0506, false),
    MESSAGE_EDIT_RES(0x0507, false),
    MESSAGE_DELETE_REQ(0x0508, false),
    MESSAGE_DELETE_RES(0x0509, false),
    MESSAGE_TYPING_INDICATOR(0x050A, false),

    // System Messages (0xF000 - 0xFFFF)
    PING(0xF000, false),
    PONG(0xF001, false),
    ERROR(0xF002, false),
    SERVER_SHUTDOWN(0xF003, false),
    BROADCAST(0xF004, false);

    private final short code;
    private final boolean rsaEncrypted;

    ProtocolMessageType(int code, boolean rsaEncrypted) {
        this.code = (short) code;
        this.rsaEncrypted = rsaEncrypted;
    }

    public short getCode() {
        return code;
    }

    public boolean isRsaEncrypted() {
        return rsaEncrypted;
    }

    public boolean isRequest() {
        return (code & 0x01) == 0x00;
    }

    public boolean isResponse() {
        return (code & 0x01) == 0x01;
    }

    public static ProtocolMessageType fromCode(short code) {
        for (ProtocolMessageType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown message type code: " + code);
    }
}
