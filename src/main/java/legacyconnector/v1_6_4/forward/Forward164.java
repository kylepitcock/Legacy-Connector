package legacyconnector.v1_6_4.forward;

import legacyconnector.v1_7_10.net.minecraft.nbt.NBTBase;
import legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagList;

/**
 * Static helpers the 1.6.4 loader (loader/.../ForwardCalls, rules in forward/1.6.4/member-manual.tsv) calls for 1.6.4 API that has no
 * one-to-one 1.7.10 member: the receiver becomes the first argument. They live in the 1.6.4 shim namespace because the class does not
 * exist in 1.7.10.
 */
public final class Forward164 {
    private Forward164() {}

    /**
     * 1.6.4 {@code NBTTagCompound.getTagList(name)}: the list stored under the key whatever its element type (1.7.10 asks for the
     * type and returns an empty list on a mismatch). A missing key gives an empty list, as in 1.6.4.
     */
    public static NBTTagList getTagList(NBTTagCompound tag, String key) {
        NBTBase value = tag.getTag(key);
        return value instanceof NBTTagList list ? list : new NBTTagList();
    }

    /** 1.6.4 {@code NBTTagList.tagAt(i)}: the element of any type (1.7.10 only has typed getters). */
    public static NBTBase tagAt(NBTTagList list, int index) {
        if (index < 0 || index >= list.tagCount()) return null;
        return NBTBase.mcc$wrap(list.mcc$modern().get(index), list);
    }

    /** 1.6.4 {@code FMLPreInitializationEvent.getModLog()}: a {@code java.util.logging.Logger} (1.7.10 returns a log4j one). */
    public static java.util.logging.Logger getModLog(legacyconnector.v1_7_10.cpw.mods.fml.common.event.FMLPreInitializationEvent event) {
        String id = event.getModMetadata() != null ? event.getModMetadata().modId : null;
        return java.util.logging.Logger.getLogger(id != null ? id : "ForgeModLoader");
    }

    // ------------------------------------------------------------------ sound: Minecraft.sndManager and its 1.6.4 methods

    /** 1.6.4 {@code Minecraft.sndManager}: the sound manager (1.7.10 reaches it through the sound handler). */
    public static legacyconnector.v1_7_10.net.minecraft.client.audio.SoundManager sndManager(legacyconnector.v1_7_10.net.minecraft.client.Minecraft mc) {
        return legacyconnector.v1_7_10.net.minecraft.client.audio.SoundManager.INSTANCE;
    }

    private static legacyconnector.v1_7_10.net.minecraft.util.ResourceLocation soundLocation(String name) {
        return new legacyconnector.v1_7_10.net.minecraft.util.ResourceLocation(name == null ? "" : name);
    }

    /** 1.6.4 {@code playSoundFX(name, volume, pitch)}: a sound at the listener (UI sounds, button clicks). */
    public static void playSoundFX(legacyconnector.v1_7_10.net.minecraft.client.audio.SoundManager m, String name, float volume, float pitch) {
        m.playSound(legacyconnector.v1_7_10.net.minecraft.client.audio.PositionedSoundRecord.createPositionedSoundRecord(soundLocation(name), pitch));
    }

    /** 1.6.4 {@code playSound(name, x, y, z, volume, pitch)}. */
    public static void playSound(legacyconnector.v1_7_10.net.minecraft.client.audio.SoundManager m, String name, float x, float y, float z, float volume, float pitch) {
        m.playSound(legacyconnector.v1_7_10.net.minecraft.client.audio.PositionedSoundRecord.createPositionedSoundRecord(soundLocation(name), volume, pitch, x, y, z));
    }

    /** 1.6.4 {@code playEntitySound}: a one-shot sound at the entity (the sound does not follow it). */
    public static void playEntitySound(legacyconnector.v1_7_10.net.minecraft.client.audio.SoundManager m, String name,
            legacyconnector.v1_7_10.net.minecraft.entity.Entity entity, float volume, float pitch, boolean loop) {
        if (entity == null) return;
        playSound(m, name, (float) legacyconnector.v1_7_10.net.minecraft.entity.Entity.mcc$get$posX(entity),
                (float) legacyconnector.v1_7_10.net.minecraft.entity.Entity.mcc$get$posY(entity),
                (float) legacyconnector.v1_7_10.net.minecraft.entity.Entity.mcc$get$posZ(entity), volume, pitch);
    }

    /** 1.6.4 {@code playStreaming(name, x, y, z)} (records and music): a record sound at the position; a null name stops the music. */
    public static void playStreaming(legacyconnector.v1_7_10.net.minecraft.client.audio.SoundManager m, String name, float x, float y, float z) {
        if (name == null) {
            m.stopAllSounds();
            return;
        }
        m.playSound(legacyconnector.v1_7_10.net.minecraft.client.audio.PositionedSoundRecord.createRecordSoundAtPosition(soundLocation(name), x, y, z));
    }

    /**
     * 1.6.4 {@code addSound(name)} / {@code addStreaming(name)}: registered a sound file by name. Sounds come from the mod's {@code sounds.json}
     * and the resource packs here, so there is nothing to register.
     */
    public static void addSound(legacyconnector.v1_7_10.net.minecraft.client.audio.SoundManager m, String name) {
    }

    /** 1.6.4 {@code setEntitySoundPitch}: entity sounds are one-shots here and cannot be changed after the start. */
    public static void setEntitySoundPitch(legacyconnector.v1_7_10.net.minecraft.client.audio.SoundManager m, legacyconnector.v1_7_10.net.minecraft.entity.Entity entity, float pitch) {
    }

    public static void setEntitySoundVolume(legacyconnector.v1_7_10.net.minecraft.client.audio.SoundManager m, legacyconnector.v1_7_10.net.minecraft.entity.Entity entity, float volume) {
    }

    /** 1.6.4 {@code GameSettings.soundVolume}: the master volume slider. */
    public static float soundVolume(legacyconnector.v1_7_10.net.minecraft.client.settings.GameSettings settings) {
        return settings.getSoundLevel(legacyconnector.v1_7_10.net.minecraft.client.audio.SoundCategory.MASTER);
    }

    /** 1.6.4 {@code GameSettings.renderDistance}: 0 far (16 chunks), 1 normal (8), 2 short (4), 3 tiny (2). */
    public static int renderDistance(legacyconnector.v1_7_10.net.minecraft.client.settings.GameSettings settings) {
        int chunks = settings.renderDistanceChunks;
        return chunks >= 16 ? 0 : chunks >= 8 ? 1 : chunks >= 4 ? 2 : 3;
    }

    // ------------------------------------------------------------------ NBT names (1.6.4 tags carried their name)

    /** 1.6.4 {@code NBTTagCompound.getTags()}: the member tags; each remembers its key for {@link #getName}. */
    public static java.util.Collection<NBTBase> getTags(NBTTagCompound tag) {
        java.util.List<NBTBase> out = new java.util.ArrayList<>();
        for (String key : new java.util.ArrayList<>(tag.getKeySet())) {
            NBTBase value = tag.getTag(key);
            if (value == null) continue;
            value.mcc$name = key;
            out.add(value);
        }
        return out;
    }

    /** 1.6.4 {@code NBTBase.getName()}: the key the tag was read under through {@link #getTags}, else "". */
    public static String getName(NBTBase tag) {
        return tag.mcc$name == null ? "" : tag.mcc$name;
    }

    public static NBTBase setName(NBTBase tag, String name) {
        tag.mcc$name = name == null ? "" : name;
        return tag;
    }

    // ------------------------------------------------------------------ server lists by player name

    private static String[] opNames(legacyconnector.v1_7_10.net.minecraft.server.management.ServerConfigurationManager m) {
        String[] names = m.getOppedPlayerNames();
        return names == null ? new String[0] : names;
    }

    /** 1.6.4 {@code isPlayerOpped(name)}: the ops list is by name (case-insensitive). */
    public static boolean isPlayerOpped(legacyconnector.v1_7_10.net.minecraft.server.management.ServerConfigurationManager m, String name) {
        if (name == null) return false;
        for (String op : opNames(m)) if (op.equalsIgnoreCase(name)) return true;
        return false;
    }

    /** 1.6.4 {@code getOps()}: the set of operator names. */
    public static java.util.Set getOps(legacyconnector.v1_7_10.net.minecraft.server.management.ServerConfigurationManager m) {
        return new java.util.HashSet<>(java.util.Arrays.asList(opNames(m)));
    }

    /** 1.6.4 {@code addOp(name)}: an online player's profile, else the offline profile of the name. */
    public static void addOp(legacyconnector.v1_7_10.net.minecraft.server.management.ServerConfigurationManager m, String name) {
        if (name == null) return;
        var online = m.getPlayerByUsername(name);
        com.mojang.authlib.GameProfile profile = online != null ? online.mcc$serverPlayer().getGameProfile()
                : new com.mojang.authlib.GameProfile(net.minecraft.core.UUIDUtil.createOfflinePlayerUUID(name), name);
        m.addOp(profile);
    }
}
