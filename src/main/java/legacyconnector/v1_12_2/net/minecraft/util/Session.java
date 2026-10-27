package legacyconnector.v1_12_2.net.minecraft.util;

import com.mojang.authlib.GameProfile;

/** The logged-in user of the client (name, id, access token), as 1.12.2's. */
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

    /** The legacy view of the modern client's user (the id without dashes, as 1.12.2 kept it). */
    public static Session mcc$of(net.minecraft.client.User user) {
        return user == null ? null : new Session(user.getName(), user.getProfileId().toString().replace("-", ""), user.getAccessToken(),
                user.getType().getName());
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

    public GameProfile getProfile() {
        try {
            java.util.UUID uuid = java.util.UUID.fromString(playerID.replaceFirst(
                    "(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5"));
            return new GameProfile(uuid, username);
        } catch (IllegalArgumentException e) {
            return new GameProfile(net.minecraft.core.UUIDUtil.createOfflinePlayerUUID(username), username);
        }
    }

    public String getSessionType() {
        return sessionType;
    }
}
