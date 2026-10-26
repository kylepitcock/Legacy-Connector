package legacyconnector.v1_7_10.net.minecraft.nbt;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import java.util.UUID;

/**
 * Game profile (de)serialisation in the 1.7.10 layout: {@code Name} and {@code Id} strings and the {@code Properties}
 * (skin textures). 1.7.10 profiles could miss either the id or the name; authlib 6 rejects null for both, so a missing id
 * becomes {@link net.minecraft.Util#NIL_UUID} and a missing name the empty string, as modern resolvable profiles do, and
 * neither is written back.
 */
public class NBTUtil {
    public static GameProfile readGameProfileFromNBT(NBTTagCompound tag) {
        String name = tag.hasKey("Name", 8) ? tag.getString("Name") : null;
        String id = tag.hasKey("Id", 8) ? tag.getString("Id") : null;
        if ((name == null || name.isEmpty()) && (id == null || id.isEmpty())) return null;
        UUID uuid;
        try {
            uuid = id == null || id.isEmpty() ? null : UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            uuid = null;
        }
        GameProfile profile;
        try {
            profile = new GameProfile(uuid != null ? uuid : net.minecraft.Util.NIL_UUID, name != null ? name : "");
        } catch (RuntimeException e) {
            return null;
        }
        if (tag.hasKey("Properties", 10)) {
            NBTTagCompound properties = tag.getCompoundTag("Properties");
            for (String key : properties.getKeySet()) {
                NBTTagList list = properties.getTagList(key, 10);
                for (int i = 0; i < list.tagCount(); i++) {
                    NBTTagCompound entry = list.getCompoundTagAt(i);
                    String value = entry.getString("Value");
                    profile.getProperties().put(key, entry.hasKey("Signature", 8)
                            ? new Property(key, value, entry.getString("Signature")) : new Property(key, value));
                }
            }
        }
        return profile;
    }

    public static void writeGameProfileToNBT(NBTTagCompound tag, GameProfile profile) {
        if (profile == null) return;
        if (profile.getName() != null && !profile.getName().isEmpty()) tag.setString("Name", profile.getName());
        if (profile.getId() != null && !net.minecraft.Util.NIL_UUID.equals(profile.getId())) tag.setString("Id", profile.getId().toString());
        if (!profile.getProperties().isEmpty()) {
            NBTTagCompound properties = new NBTTagCompound();
            for (String key : profile.getProperties().keySet()) {
                NBTTagList list = new NBTTagList();
                for (Property property : profile.getProperties().get(key)) {
                    NBTTagCompound entry = new NBTTagCompound();
                    entry.setString("Value", property.value());
                    if (property.hasSignature()) entry.setString("Signature", property.signature());
                    list.appendTag(entry);
                }
                properties.setTag(key, list);
            }
            tag.setTag("Properties", properties);
        }
    }
}
