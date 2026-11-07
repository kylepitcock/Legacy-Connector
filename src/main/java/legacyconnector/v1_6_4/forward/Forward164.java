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
     * and the resource packs here, so the name is only remembered by the sound pool.
     */
    public static void addSound(legacyconnector.v1_7_10.net.minecraft.client.audio.SoundManager m, String name) {
        legacyconnector.v1_6_4.net.minecraft.client.audio.SoundPool.SOUNDS.addSound(name);
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

    // ------------------------------------------------------------------ legacy wire formats (Packet.readItemStack, NBTBase.readNamedTag)

    /** 1.6.4 {@code Packet.readItemStack(DataInput)}: short id (-1 for none), byte count, short damage, gzipped NBT behind a short length (-1 for none). */
    public static legacyconnector.v1_7_10.net.minecraft.item.ItemStack readItemStack(java.io.DataInput in) throws java.io.IOException {
        short id = in.readShort();
        if (id < 0) return null;
        int count = in.readByte();
        int damage = in.readShort();
        legacyconnector.v1_7_10.net.minecraft.item.Item item = legacyconnector.v1_6_4.net.minecraft.util.Ids.item(id);
        legacyconnector.v1_7_10.net.minecraft.item.ItemStack stack = item == null ? null
                : new legacyconnector.v1_7_10.net.minecraft.item.ItemStack(item, count, damage);
        short length = in.readShort();
        if (length >= 0) {
            byte[] bytes = new byte[length];
            in.readFully(bytes);
            if (stack != null) {
                net.minecraft.nbt.CompoundTag tag = net.minecraft.nbt.NbtIo.readCompressed(new java.io.ByteArrayInputStream(bytes), net.minecraft.nbt.NbtAccounter.unlimitedHeap());
                stack.setTagCompound(new NBTTagCompound(tag));
            }
        }
        return stack;
    }

    public static void writeItemStack(legacyconnector.v1_7_10.net.minecraft.item.ItemStack stack, java.io.DataOutput out) throws java.io.IOException {
        legacyconnector.v1_7_10.net.minecraft.item.Item item = stack == null ? null : stack.getItem();
        if (item == null) {
            out.writeShort(-1);
            return;
        }
        out.writeShort(legacyconnector.v1_6_4.net.minecraft.util.Ids.itemId(item));
        out.writeByte(legacyconnector.v1_7_10.net.minecraft.item.ItemStack.mcc$get$stackSize(stack));
        out.writeShort(stack.getItemDamage());
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            out.writeShort(-1);
            return;
        }
        java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
        net.minecraft.nbt.NbtIo.writeCompressed(tag.mcc$modern(), bytes);
        out.writeShort((short) bytes.size());
        out.write(bytes.toByteArray());
    }

    /** 1.6.4 {@code NBTBase.readNamedTag(DataInput)}: type byte (0 ends), UTF name, payload. The name stays on the tag (see {@link #getName}). */
    public static NBTBase readNamedTag(java.io.DataInput in) throws java.io.IOException {
        byte type = in.readByte();
        if (type == 0) return null;
        String name = in.readUTF();
        net.minecraft.nbt.Tag tag = net.minecraft.nbt.TagTypes.getType(type).load(in, net.minecraft.nbt.NbtAccounter.unlimitedHeap());
        NBTBase wrapped = NBTBase.mcc$wrap(tag, null);
        wrapped.mcc$name = name;
        return wrapped;
    }

    public static void writeNamedTag(NBTBase tag, java.io.DataOutput out) throws java.io.IOException {
        byte type = tag.getId();
        out.writeByte(type);
        if (type == 0) return;
        out.writeUTF(getName(tag));
        tag.mcc$modern().write(out);
    }

    // ------------------------------------------------------------------ constructor argument converters (rules "C ... args")

    /** 1.6.4 {@code new ScaledResolution(gameSettings, w, h)}: 1.7.10 takes the client. */
    public static legacyconnector.v1_7_10.net.minecraft.client.Minecraft minecraft(legacyconnector.v1_7_10.net.minecraft.client.settings.GameSettings settings) {
        return legacyconnector.v1_7_10.net.minecraft.client.Minecraft.getMinecraft();
    }

    /** 1.6.4 players were named; 1.7.10 ones have a profile (the offline profile of the name, as an offline server makes it). */
    public static com.mojang.authlib.GameProfile profile(String name) {
        String n = name == null ? "[Unknown]" : name;
        return new com.mojang.authlib.GameProfile(net.minecraft.core.UUIDUtil.createOfflinePlayerUUID(n), n);
    }

    public static legacyconnector.v1_7_10.net.minecraft.world.WorldServer worldServer(legacyconnector.v1_7_10.net.minecraft.world.World world) {
        return (legacyconnector.v1_7_10.net.minecraft.world.WorldServer) world;
    }

    /** 1.6.4 {@code new Achievement(id, name, ...)}: the stat id was derived from the name. */
    public static String achievementId(String name) {
        return "achievement." + name;
    }

    /** 1.6.4 {@code FakePlayerFactory.get(world, name)}. */
    public static legacyconnector.v1_7_10.net.minecraftforge.common.util.FakePlayer fakePlayer(legacyconnector.v1_7_10.net.minecraft.world.World world, String name) {
        return legacyconnector.v1_7_10.net.minecraftforge.common.util.FakePlayerFactory.get(worldServer(world), profile(name));
    }

    /** 1.6.4 {@code MinecraftForge.setToolClass(item, toolClass, level)}: 1.7.10 sets it on the item. */
    public static void setToolClass(legacyconnector.v1_7_10.net.minecraft.item.Item item, String toolClass, int level) {
        item.setHarvestLevel(toolClass, level);
    }

    /**
     * 1.6.4 {@code getOwnerName()} of tameable animals: the owner's name (1.7.10 stores the UUID; the name of an online player, else the UUID text).
     */
    public static String getOwnerName(legacyconnector.v1_7_10.net.minecraft.entity.IEntityOwnable entity) {
        String id = entity.func_152113_b();
        if (id == null || id.isEmpty()) return "";
        try {
            java.util.UUID uuid = java.util.UUID.fromString(id);
            var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                var player = server.getPlayerList().getPlayer(uuid);
                if (player != null) return player.getGameProfile().getName();
                var cached = server.getProfileCache() == null ? java.util.Optional.<com.mojang.authlib.GameProfile>empty() : server.getProfileCache().get(uuid);
                if (cached.isPresent()) return cached.get().getName();
            }
        } catch (IllegalArgumentException e) {
            return id;   // already a name
        }
        return id;
    }

    // ------------------------------------------------------------------ furnace recipes by id and damage

    /**
     * 1.6.4 {@code FurnaceRecipes.getMetaSmeltingList()}: a map keyed by {@code List(itemId, damage)}, a live view of the recipes: a put registers a
     * smelting recipe for that item and damage, a get finds one.
     */
    public static java.util.Map getMetaSmeltingList(legacyconnector.v1_7_10.net.minecraft.item.crafting.FurnaceRecipes recipes) {
        return new java.util.AbstractMap<Object, Object>() {
            private legacyconnector.v1_7_10.net.minecraft.item.ItemStack stackOf(Object key) {
                if (!(key instanceof java.util.List<?> l) || l.size() != 2 || !(l.get(0) instanceof Integer id) || !(l.get(1) instanceof Integer dmg)) return null;
                legacyconnector.v1_7_10.net.minecraft.item.Item item = legacyconnector.v1_6_4.net.minecraft.util.Ids.item(id);
                return item == null ? null : new legacyconnector.v1_7_10.net.minecraft.item.ItemStack(item, 1, dmg);
            }

            @Override
            public Object put(Object key, Object value) {
                legacyconnector.v1_7_10.net.minecraft.item.ItemStack in = stackOf(key);
                if (in != null && value instanceof legacyconnector.v1_7_10.net.minecraft.item.ItemStack out) recipes.addSmeltingRecipe(in, out, 0.0F);
                return null;
            }

            @Override
            public Object get(Object key) {
                legacyconnector.v1_7_10.net.minecraft.item.ItemStack in = stackOf(key);
                return in == null ? null : recipes.getSmeltingResult(in);
            }

            @Override
            public boolean containsKey(Object key) {
                return get(key) != null;
            }

            @Override
            public java.util.Set<Entry<Object, Object>> entrySet() {
                java.util.Set<Entry<Object, Object>> out = new java.util.LinkedHashSet<>();
                for (Object o : recipes.getSmeltingList().entrySet()) {
                    Entry<?, ?> e = (Entry<?, ?>) o;
                    if (e.getKey() instanceof legacyconnector.v1_7_10.net.minecraft.item.ItemStack in && in.getItem() != null) {
                        out.add(new java.util.AbstractMap.SimpleEntry<>(java.util.Arrays.asList(legacyconnector.v1_6_4.net.minecraft.util.Ids.itemId(in.getItem()), in.getItemDamage()), e.getValue()));
                    }
                }
                return out;
            }
        };
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

<<<<<<< HEAD
    /** A profile for a player object built without one ({@code new EntityPlayer(world)} of 1.5.2 and 1.4.7 subclasses): a fresh UUID under a placeholder name. */
    public static com.mojang.authlib.GameProfile anonymousProfile() {
        return new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "[Player]");
    }

    /** 1.6.4 {@code getOps()}: the set of operator names. */
=======
    /** 1.5.2 / 1.6.4 static {@code MinecraftServer.getServerConfigurationManager(server)}. */
    public static legacyconnector.v1_7_10.net.minecraft.server.management.ServerConfigurationManager serverConfigurationManager(
            legacyconnector.v1_7_10.net.minecraft.server.MinecraftServer server) {
        return server.getConfigurationManager();
    }

    /** 1.6.4 {@code getOps()}:the set of operator names. */
>>>>>>> 9506c1d5 (Old network packets: 1.4.7-1.6.4 chunk wire format (DEFLATE section arrays, round-trip tested), Packet14BlockDig, Packet1Login fields, areCommandsAllowed, static MinecraftServer.getServerConfigurationManager)
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
