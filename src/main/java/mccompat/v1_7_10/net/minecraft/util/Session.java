package mccompat.v1_7_10.net.minecraft.util;

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
            return new GameProfile(UUID.fromString(playerId.replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5")), username);
        } catch (IllegalArgumentException e) {
            return new GameProfile(UUID.nameUUIDFromBytes(username.getBytes()), username);
        }
    }
}
