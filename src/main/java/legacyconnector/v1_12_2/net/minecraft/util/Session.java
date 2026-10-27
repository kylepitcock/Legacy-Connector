package legacyconnector.v1_12_2.net.minecraft.util;

/**
 * The signed-in account: name, UUID (without dashes) and access token. Made by {@code Minecraft.getSession()} from the modern user.
 * (Created for area R2b's {@code getSession}; area F owns the class.)
 */
public class Session {
    private final String username;
    private final String playerID;
    private final String token;
    private final String sessionType;

    public Session(String usernameIn, String playerIDIn, String tokenIn, String sessionTypeIn) {
        this.username = usernameIn;
        this.playerID = playerIDIn;
        this.token = tokenIn;
        this.sessionType = sessionTypeIn;
    }

    public String getSessionID() {
        return "token:" + token + ":" + playerID;
    }

    public String getPlayerID() {
        return playerID;
    }

    public String getUsername() {
        return username;
    }

    public String getToken() {
        return token;
    }

    public com.mojang.authlib.GameProfile getProfile() {
        java.util.UUID id;
        try {
            id = com.mojang.util.UndashedUuid.fromStringLenient(playerID);
        } catch (IllegalArgumentException e) {
            id = null;
        }
        return new com.mojang.authlib.GameProfile(id == null ? java.util.UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes(
                java.nio.charset.StandardCharsets.UTF_8)) : id, username);
    }
}
