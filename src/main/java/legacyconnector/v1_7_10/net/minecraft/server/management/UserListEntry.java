package legacyconnector.v1_7_10.net.minecraft.server.management;

import com.google.gson.JsonObject;

/** One entry of a server user list (ops, bans, whitelist): the user it is about. */
public class UserListEntry<T> {
    final T value;

    public UserListEntry(T value) {
        this.value = value;
    }

    protected UserListEntry(T value, JsonObject json) {
        this.value = value;
    }

    T getValue() {
        return value;
    }

    /** The user of the entry (the package-private {@code getValue} is for the lists). */
    public T mcc$value() {
        return value;
    }

    boolean hasBanExpired() {
        return false;
    }

    protected void onSerialization(JsonObject json) {
    }

    /** {@link #onSerialization} for the lists in other packages. */
    public JsonObject mcc$serialize() {
        JsonObject json = new JsonObject();
        onSerialization(json);
        return json;
    }

    /** {@link #hasBanExpired} for code outside the package. */
    public boolean mcc$expired() {
        return hasBanExpired();
    }

    /** The profile's name, id and offline fallback in the 1.7.10 JSON layout ({@code uuid}, {@code name}). */
    static void mcc$writeProfile(JsonObject json, com.mojang.authlib.GameProfile profile) {
        if (profile == null || profile.getId() == null) return;
        json.addProperty("uuid", profile.getId().toString());
        json.addProperty("name", profile.getName());
    }

    static com.mojang.authlib.GameProfile mcc$readProfile(JsonObject json) {
        if (!json.has("uuid") || !json.has("name")) return null;
        try {
            return new com.mojang.authlib.GameProfile(java.util.UUID.fromString(json.get("uuid").getAsString()), json.get("name").getAsString());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
