package legacyconnector.v1_7_10.net.minecraft.util;

import com.mojang.authlib.GameProfile;
import java.util.UUID;

/** The player account of this client. */
public class Session {
    private final String username;
    private final String playerId;
    private final String token;

    public Session(String username, String playerId, String token) {
        this.username = username;
        this.playerId = playerId;
        this.token = token;
    }

    public static Session mcc$current() {
        try {
            var user = net.minecraft.client.Minecraft.getInstance().getUser();
            return new Session(user.getName(), user.getProfileId().toString().replace("-", ""), user.getAccessToken());
        } catch (RuntimeException | LinkageError e) {
            return new Session("Player", "00000000000000000000000000000000", "0");
        }
    }

    /** The account's game profile (FTBLib reads the id of it while loading). */
    public GameProfile getProfile() {
        try {
            String id = playerId;
            return new GameProfile(java.util.UUID.fromString(id.substring(0, 8) + "-" + id.substring(8, 12) + "-" + id.substring(12, 16) + "-" + id.substring(16, 20) + "-" + id.substring(20)), username);
        } catch (RuntimeException e) {
            return new GameProfile(java.util.UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes()), username);
        }
    }

    public String getUsername() {
        return username;
    }

    public String getPlayerID() {
        return playerId;
    }

    public String getToken() {
        return token;
    }

    public String getSessionID() {
        return "token:" + token + ":" + playerId;
    }

    public GameProfile func_148256_e() {
        try {
            String id = playerId;
            return new GameProfile(java.util.UUID.fromString(id.substring(0, 8) + "-" + id.substring(8, 12) + "-" + id.substring(12, 16) + "-" + id.substring(16, 20) + "-" + id.substring(20)), username);
        } catch (IllegalArgumentException e) {
            return new GameProfile(UUID.nameUUIDFromBytes(username.getBytes()), username);
        }
    }
}
