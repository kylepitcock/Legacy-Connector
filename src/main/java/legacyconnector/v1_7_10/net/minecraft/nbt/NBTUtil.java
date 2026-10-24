package legacyconnector.v1_7_10.net.minecraft.nbt;

import com.mojang.authlib.GameProfile;
import java.util.UUID;

/** Game profile (de)serialisation in the 1.7.10 layout: {@code Name} and {@code Id} strings. */
public class NBTUtil {
    public static GameProfile readGameProfileFromNBT(NBTTagCompound tag) {
        String name = tag.hasKey("Name", 8) ? tag.getString("Name") : null;
        String id = tag.hasKey("Id", 8) ? tag.getString("Id") : null;
        if ((name == null || name.isEmpty()) && (id == null || id.isEmpty())) return null;
        try {
            UUID uuid;
            try {
                uuid = id == null ? null : UUID.fromString(id);
            } catch (IllegalArgumentException e) {
                uuid = null;
            }
            return new GameProfile(uuid, name);
        } catch (RuntimeException e) {
            return null;
        }
    }

    public static void writeGameProfileToNBT(NBTTagCompound tag, GameProfile profile) {
        if (profile == null) return;
        if (profile.getName() != null && !profile.getName().isEmpty()) tag.setString("Name", profile.getName());
        if (profile.getId() != null) tag.setString("Id", profile.getId().toString());
    }
}
