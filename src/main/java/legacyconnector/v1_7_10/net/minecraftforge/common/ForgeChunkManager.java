package legacyconnector.v1_7_10.net.minecraftforge.common;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.SetMultimap;
import java.io.File;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import legacyconnector.v1_7_10.cpw.mods.fml.common.Loader;
import legacyconnector.v1_7_10.cpw.mods.fml.common.ModContainer;
import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_7_10.net.minecraft.world.ChunkCoordIntPair;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import legacyconnector.v1_7_10.net.minecraftforge.common.config.Configuration;
import net.minecraft.server.level.ServerLevel;

/**
 * Forge's chunk loading tickets. Forced chunks are really kept loaded ({@link ServerLevel#setChunkForced}); a chunk
 * stays forced while any ticket holds it. Tickets of each level are saved with it (saved data, as Forge's forcedchunks.dat) and handed
 * back to their mod's {@link LoadingCallback} when the server has started: BuildCraft's quarry marks itself alive in that callback and
 * stalls without it. Per-mod limits come from {@code config/forgeChunkLoading.cfg}, as in Forge.
 */
public class ForgeChunkManager {
    private static final Map<String, LoadingCallback> CALLBACKS = new ConcurrentHashMap<>();
    private static final Map<ServerLevel, SetMultimap<ChunkCoordIntPair, Ticket>> FORCED = new ConcurrentHashMap<>();
    private static Configuration config;
    /** Forge's private limit overrides; mods (Dimensional Anchors' bypass of the chunk limits) edit them reflectively. */
    private static boolean overridesEnabled;
    private static final Map<String, Integer> ticketConstraints = new ConcurrentHashMap<>();
    private static final Map<String, Integer> chunkConstraints = new ConcurrentHashMap<>();

    private static final String STORE_NAME = "legacyconnector_forced_chunk_tickets";
    private static final Map<ServerLevel, Store> STORES = new ConcurrentHashMap<>();

    /** The tickets of one level as saved data: written from the live tickets on save, read back once when the server starts. */
    private static final class Store extends net.minecraft.world.level.saveddata.SavedData {
        net.minecraft.nbt.ListTag loaded;
        ServerLevel owner;

        @Override
        public net.minecraft.nbt.CompoundTag save(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
            net.minecraft.nbt.ListTag out = new net.minecraft.nbt.ListTag();
            SetMultimap<ChunkCoordIntPair, Ticket> forced = owner == null ? null : FORCED.get(owner);
            if (forced != null) {
                Set<Ticket> tickets;
                synchronized (forced) {
                    tickets = new LinkedHashSet<>(forced.values());
                }
                for (Ticket t : tickets) {
                    if (t.ticketType != Type.NORMAL) continue; // an entity ticket is bound again by its entity
                    net.minecraft.nbt.CompoundTag e = new net.minecraft.nbt.CompoundTag();
                    e.putString("mod", t.modId);
                    e.putInt("depth", t.maxDepth);
                    if (t.player != null) e.putString("player", t.player);
                    e.put("data", t.modData.mcc$modern().copy());
                    net.minecraft.nbt.ListTag chunks = new net.minecraft.nbt.ListTag();
                    for (ChunkCoordIntPair c : t.requestedChunks) chunks.add(net.minecraft.nbt.LongTag.valueOf(net.minecraft.world.level.ChunkPos.asLong(c.chunkXPos, c.chunkZPos)));
                    e.put("chunks", chunks);
                    out.add(e);
                }
            }
            tag.put("tickets", out);
            return tag;
        }
    }

    private static Store store(ServerLevel level) {
        return STORES.computeIfAbsent(level, l -> {
            Store st = l.getDataStorage().computeIfAbsent(new net.minecraft.world.level.saveddata.SavedData.Factory<Store>(Store::new, (tag, reg) -> {
                Store loaded = new Store();
                loaded.loaded = tag.getList("tickets", 10);
                return loaded;
            }, null), STORE_NAME);
            st.owner = l;
            return st;
        });
    }

    private static void markDirty(ServerLevel level) {
        try {
            store(level).setDirty();
        } catch (RuntimeException e) {
            // saved data is unavailable this early or late: the tickets are then just not persisted
        }
    }

    /** Hands the tickets a level saved to their mods' callbacks (Forge does it when a world loads). */
    private static void restoreTickets(ServerLevel level) {
        Store st = store(level);
        net.minecraft.nbt.ListTag saved = st.loaded;
        st.loaded = null;
        if (saved == null || saved.isEmpty()) return;
        World world = World.mcc$wrap(level);
        Map<String, java.util.List<Ticket>> byMod = new java.util.LinkedHashMap<>();
        for (int i = 0; i < saved.size(); i++) {
            net.minecraft.nbt.CompoundTag e = saved.getCompound(i);
            String mod = e.getString("mod");
            Ticket t = new Ticket(mod, Type.NORMAL, world, e.contains("player") ? e.getString("player") : null);
            t.maxDepth = e.getInt("depth");
            net.minecraft.nbt.CompoundTag data = e.getCompound("data");
            for (String k : data.getAllKeys()) t.modData.mcc$modern().put(k, data.get(k).copy());
            SetMultimap<ChunkCoordIntPair, Ticket> forced = FORCED.computeIfAbsent(level, l -> LinkedHashMultimap.create());
            net.minecraft.nbt.ListTag chunks = e.getList("chunks", 4);
            for (int c = 0; c < chunks.size(); c++) {
                long packed = ((net.minecraft.nbt.LongTag) chunks.get(c)).getAsLong();
                ChunkCoordIntPair pair = new ChunkCoordIntPair(net.minecraft.world.level.ChunkPos.getX(packed), net.minecraft.world.level.ChunkPos.getZ(packed));
                t.requestedChunks.add(pair);
                synchronized (forced) {
                    forced.put(pair, t);
                }
                level.setChunkForced(pair.chunkXPos, pair.chunkZPos, true);
            }
            byMod.computeIfAbsent(mod, k -> new java.util.ArrayList<>()).add(t);
        }
        for (var entry : byMod.entrySet()) {
            LoadingCallback callback = CALLBACKS.get(entry.getKey());
            java.util.List<Ticket> tickets = entry.getValue();
            if (callback == null) {
                for (Ticket t : tickets) releaseTicket(t); // the mod is gone
                continue;
            }
            try {
                if (callback instanceof OrderedLoadingCallback ordered) {
                    java.util.List<Ticket> kept = ordered.ticketsLoaded(tickets, world, getMaxTicketLengthFor(entry.getKey()));
                    if (kept == null) kept = tickets;
                    for (Ticket t : tickets) if (!kept.contains(t)) releaseTicket(t);
                    callback.ticketsLoaded(kept, world); // Forge hands the kept tickets to the plain callback too
                } else {
                    callback.ticketsLoaded(tickets, world);
                }
            } catch (RuntimeException | LinkageError ex) {
                org.slf4j.LoggerFactory.getLogger("LegacyConnector/ChunkLoading").error("Chunk loading callback of {} failed on load", entry.getKey(), ex);
            }
        }
        markDirty(level);
    }

    static {
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.server.ServerStartedEvent e) -> {
            for (ServerLevel level : e.getServer().getAllLevels()) {
                try {
                    restoreTickets(level);
                } catch (RuntimeException | LinkageError ex) {
                    org.slf4j.LoggerFactory.getLogger("LegacyConnector/ChunkLoading").error("Could not restore the chunk loading tickets of {}", level.dimension().location(), ex);
                }
            }
        });
        // the tickets and their mod data are written from the live objects on every save of the level
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.level.LevelEvent.Save e) -> {
            if (e.getLevel() instanceof ServerLevel level && FORCED.containsKey(level)) markDirty(level);
        });
        // perf.releaseUnloadedLevels: as Forge's unloadWorld, the tickets of an unloaded level go with it (they hold its world)
        org.kylepitcock.legacyconnector.core.LevelReleases.onLevelUnload(level -> {
            if (level instanceof ServerLevel s) {
                FORCED.remove(s);
                STORES.remove(s);
            }
        });
    }

    public interface LoadingCallback {
        void ticketsLoaded(java.util.List<Ticket> tickets, World world);
    }

    public interface OrderedLoadingCallback extends LoadingCallback {
        java.util.List<Ticket> ticketsLoaded(java.util.List<Ticket> tickets, World world, int maxTicketCount);
    }

    public interface PlayerOrderedLoadingCallback extends LoadingCallback {
        com.google.common.collect.ListMultimap<String, Ticket> playerTicketsLoaded(com.google.common.collect.ListMultimap<String, Ticket> tickets, World world);
    }

    public enum Type {
        NORMAL, ENTITY
    }

    public static class Ticket {
        private final String modId;
        private final Type ticketType;
        private final LinkedHashSet<ChunkCoordIntPair> requestedChunks = new LinkedHashSet<>();
        private final NBTTagCompound modData = new NBTTagCompound();
        public final World world;
        private int maxDepth;
        private String player;
        private Entity entity;

        Ticket(String modId, Type type, World world) {
            this.modId = modId;
            this.ticketType = type;
            this.world = world;
            this.maxDepth = getMaxChunkDepthFor(modId);
        }

        Ticket(String modId, Type type, World world, String player) {
            this(modId, type, world);
            this.player = player;
        }

        public void setChunkListDepth(int depth) {
            maxDepth = Math.max(0, Math.min(depth, getMaxChunkDepthFor(modId)));
        }

        public int getChunkListDepth() {
            return maxDepth;
        }

        public int getMaxChunkListDepth() {
            return getMaxChunkDepthFor(modId);
        }

        public void bindEntity(Entity entity) {
            if (ticketType != Type.ENTITY) throw new RuntimeException("Cannot bind an entity to a non-entity ticket");
            this.entity = entity;
        }

        public NBTTagCompound getModData() {
            return modData;
        }

        public Entity getEntity() {
            return entity;
        }

        public boolean isPlayerTicket() {
            return player != null;
        }

        public String getPlayerName() {
            return player;
        }

        public String getModId() {
            return modId;
        }

        public Type getType() {
            return ticketType;
        }

        public ImmutableSet<ChunkCoordIntPair> getChunkList() {
            return ImmutableSet.copyOf(requestedChunks);
        }
    }

    public static class ForceChunkEvent extends legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.Event {
        public final Ticket ticket;
        public final ChunkCoordIntPair location;

        public ForceChunkEvent(Ticket ticket, ChunkCoordIntPair location) {
            this.ticket = ticket;
            this.location = location;
        }
    }

    public static class UnforceChunkEvent extends legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.Event {
        public final Ticket ticket;
        public final ChunkCoordIntPair location;

        public UnforceChunkEvent(Ticket ticket, ChunkCoordIntPair location) {
            this.ticket = ticket;
            this.location = location;
        }
    }

    public static synchronized Configuration getConfig() {
        if (config == null) {
            config = new Configuration(new File(Loader.instance().getConfigDir(), "forgeChunkLoading.cfg"), true);
            config.get("defaults", "maximumTicketCount", 200);
            config.get("defaults", "maximumChunksPerTicket", 25);
            config.get("defaults", "playerTicketCount", 500);
            config.get("defaults", "dormantChunkCacheSize", 0);
            config.save();
        }
        return config;
    }

    /** Re-reads the chunk loading defaults (forgeChunkLoading.cfg, read live by this shim) and saves the file if a value changed. */
    public static void syncConfigDefaults() {
        Configuration c = getConfig();
        c.get("defaults", "maximumTicketCount", 200);
        c.get("defaults", "maximumChunksPerTicket", 25);
        c.get("defaults", "playerTicketCount", 500);
        c.get("defaults", "dormantChunkCacheSize", 0);
        if (c.hasChanged()) c.save();
    }

    public static int getMaxTicketLengthFor(String modId) {
        if (overridesEnabled && ticketConstraints.containsKey(modId)) return ticketConstraints.get(modId);
        return getConfig().hasCategory(modId) ? getConfig().get(modId, "maximumTicketCount", 200).getInt()
                : getConfig().get("defaults", "maximumTicketCount", 200).getInt();
    }

    public static int getMaxChunkDepthFor(String modId) {
        if (overridesEnabled && chunkConstraints.containsKey(modId)) return chunkConstraints.get(modId);
        return getConfig().hasCategory(modId) ? getConfig().get(modId, "maximumChunksPerTicket", 25).getInt()
                : getConfig().get("defaults", "maximumChunksPerTicket", 25).getInt();
    }

    public static int ticketCountAvailableFor(String player) {
        return getConfig().get("defaults", "playerTicketCount", 500).getInt();
    }

    /** How many more tickets the mod may request in this world: its maximum minus the tickets that currently hold chunks there. */
    public static int ticketCountAvailableFor(Object mod, World world) {
        String id = modId(mod);
        if (id == null) return 0;
        int used = 0;
        if (world != null && world.mcc$fullLevel() instanceof ServerLevel level) {
            SetMultimap<ChunkCoordIntPair, Ticket> forced = FORCED.get(level);
            if (forced != null) {
                Set<Ticket> tickets = new java.util.HashSet<>();
                synchronized (forced) {
                    for (Ticket t : forced.values()) if (id.equals(t.getModId())) tickets.add(t);
                }
                used = tickets.size();
            }
        }
        return Math.max(0, getMaxTicketLengthFor(id) - used);
    }

    public static boolean savedWorldHasForcedChunkTickets(World world) {
        return false;
    }

    public static void setForcedChunkLoadingCallback(Object mod, LoadingCallback callback) {
        String id = modId(mod);
        if (id != null && callback != null) CALLBACKS.put(id, callback);
    }

    public static Ticket requestTicket(Object mod, World world, Type type) {
        String id = modId(mod);
        if (id == null || !CALLBACKS.containsKey(id)) return null; // Forge refuses mods without a callback
        return new Ticket(id, type, world);
    }

    public static Ticket requestPlayerTicket(Object mod, String player, World world, Type type) {
        String id = modId(mod);
        if (id == null) return null;
        return new Ticket(id, type, world, player);
    }

    public static void releaseTicket(Ticket ticket) {
        if (ticket == null) return;
        for (ChunkCoordIntPair c : Set.copyOf(ticket.requestedChunks)) unforceChunk(ticket, c);
    }

    public static void forceChunk(Ticket ticket, ChunkCoordIntPair chunk) {
        if (ticket == null || chunk == null || !(ticket.world.mcc$fullLevel() instanceof ServerLevel level)) return;
        if (ticket.requestedChunks.contains(chunk)) return;
        ticket.requestedChunks.add(chunk);
        SetMultimap<ChunkCoordIntPair, Ticket> forced = FORCED.computeIfAbsent(level, l -> LinkedHashMultimap.create());
        synchronized (forced) {
            forced.put(chunk, ticket);
        }
        level.setChunkForced(chunk.chunkXPos, chunk.chunkZPos, true);
        markDirty(level);
        // Over the depth limit, the oldest chunk is released (Forge's behaviour).
        if (ticket.maxDepth > 0 && ticket.requestedChunks.size() > ticket.maxDepth) {
            unforceChunk(ticket, ticket.requestedChunks.iterator().next());
        }
    }

    public static void reorderChunk(Ticket ticket, ChunkCoordIntPair chunk) {
        if (ticket == null || !ticket.requestedChunks.remove(chunk)) return;
        ticket.requestedChunks.add(chunk);
    }

    public static void unforceChunk(Ticket ticket, ChunkCoordIntPair chunk) {
        if (ticket == null || chunk == null || !ticket.requestedChunks.remove(chunk)) return;
        if (!(ticket.world.mcc$fullLevel() instanceof ServerLevel level)) return;
        SetMultimap<ChunkCoordIntPair, Ticket> forced = FORCED.get(level);
        boolean stillHeld;
        synchronized (forced) {
            forced.remove(chunk, ticket);
            stillHeld = forced.containsKey(chunk);
        }
        if (!stillHeld) level.setChunkForced(chunk.chunkXPos, chunk.chunkZPos, false);
        markDirty(level);
    }

    public static ImmutableSetMultimap<ChunkCoordIntPair, Ticket> getPersistentChunksFor(World world) {
        if (world == null || !(world.mcc$fullLevel() instanceof ServerLevel level)) return ImmutableSetMultimap.of();
        SetMultimap<ChunkCoordIntPair, Ticket> forced = FORCED.get(level);
        if (forced == null) return ImmutableSetMultimap.of();
        synchronized (forced) {
            return ImmutableSetMultimap.copyOf(forced);
        }
    }

    private static String modId(Object mod) {
        if (mod == null) return null;
        for (ModContainer c : Loader.instance().getActiveModList()) {
            if (c.getMod() == mod) return c.getModId();
        }
        return null;
    }
}
