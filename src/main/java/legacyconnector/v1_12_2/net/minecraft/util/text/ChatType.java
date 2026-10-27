package legacyconnector.v1_12_2.net.minecraft.util.text;

/** Where a chat message shows: the chat, the chat as a system message, or the action bar. */
public enum ChatType {
    CHAT((byte) 0),
    SYSTEM((byte) 1),
    GAME_INFO((byte) 2);

    private final byte id;

    ChatType(byte id) {
        this.id = id;
    }

    public byte getId() {
        return id;
    }

    public static ChatType byId(byte idIn) {
        for (ChatType t : values()) if (idIn == t.id) return t;
        return CHAT;
    }
}
