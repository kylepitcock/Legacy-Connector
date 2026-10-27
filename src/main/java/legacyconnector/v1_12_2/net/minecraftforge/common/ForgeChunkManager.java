package legacyconnector.v1_12_2.net.minecraftforge.common;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import java.io.File;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_12_2.net.minecraft.util.math.ChunkPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import legacyconnector.v1_12_2.net.minecraftforge.common.config.Configuration;

/**
 * Forge's chunk loading tickets. A forced chunk holds a modern force ticket (forced chunks load and tick like vanilla's
 * {@code /forceload}), limited by the counts of forgeChunkLoading.cfg. Tickets are not saved with the world: chunk loaders
 * force their chunks again when their tile entities load.
 */
public class ForgeChunkManager {
    private static Configuration config;
    private static final net.minecraft.server.level.TicketType<net.minecraft.world.level.ChunkPos> TICKET =
            net.minecraft.server.level.TicketType.create("legacyconnector_1122", java.util.Comparator.comparingLong(net.minecraft.world.level.ChunkPos::toLong));
    private static final Map<Object, LoadingCallback> CALLBACKS = new java.util.IdentityHashMap<>();
    private static final Set<Ticket> TICKETS = new LinkedHashSet<>();

    public enum Type {
        NORMAL, ENTITY
    }

    public interface LoadingCallback {
        void ticketsLoaded(List<Ticket> tickets, World world);
    }

    public interface OrderedLoadingCallback extends LoadingCallback {
        List<Ticket> ticketsLoaded(List<Ticket> tickets, World world, int maxTicketCount);
    }

    public interface PlayerOrderedLoadingCallback extends LoadingCallback {
        com.google.common.collect.ListMultimap<String, Ticket> playerTicketsLoaded(com.google.common.collect.ListMultimap<String, Ticket> tickets, World world);
    }

    public static class Ticket {
        private final String modId;
        private final Type ticketType;
        private final String player;
        private final NBTTagCompound modData = new NBTTagCompound();
        public final World world;
        private final Set<ChunkPos> requestedChunks = new LinkedHashSet<>();
        private int maxDepth;
        private legacyconnector.v1_12_2.net.minecraft.entity.Entity entity;

        Ticket(String modId, Type type, World world, String player) {
            this.modId = modId;
            this.ticketType = type;
            this.world = world;
            this.player = player;
            this.maxDepth = getMaxChunkDepthFor(modId);
        }

        public NBTTagCompound getModData() {
            return modData;
        }

        public Type getType() {
            return ticketType;
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

        public void bindEntity(legacyconnector.v1_12_2.net.minecraft.entity.Entity entity) {
            this.entity = entity;
        }

        public legacyconnector.v1_12_2.net.minecraft.entity.Entity getEntity() {
            return entity;
        }

        public String getModId() {
            return modId;
        }

        public boolean isPlayerTicket() {
            return player != null;
        }

        public String getPlayerName() {
            return player;
        }

        public synchronized ImmutableSet<ChunkPos> getChunkList() {
            return ImmutableSet.copyOf(requestedChunks);
        }
    }

    public static synchronized Configuration getConfig() {
        if (config == null) {
            config = new Configuration(new File(net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get().toFile(), "forgeChunkLoading.cfg"));
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

    private static int configInt(String key, int fallback) {
        try {
            return getConfig().get("defaults", key, fallback).getInt(fallback);
        } catch (RuntimeException | LinkageError e) {
            return fallback;
        }
    }

    private static String modIdOf(Object mod) {
        if (mod instanceof String s) return s;
        var container = org.kylepitcock.legacyconnector.v1_12_2.LegacyModContainer1122.active();
        if (container != null) return container.getModId();
        return mod == null ? "unknown" : mod.getClass().getName();
    }

    public static synchronized void setForcedChunkLoadingCallback(Object mod, LoadingCallback callback) {
        CALLBACKS.put(modIdOf(mod), callback);
    }

    private static net.minecraft.server.level.ServerLevel serverLevel(World world) {
        return world != null && world.mcc$level() instanceof net.minecraft.server.level.ServerLevel s ? s : null;
    }

    public static synchronized Ticket requestTicket(Object mod, World world, Type type) {
        if (serverLevel(world) == null) return null;
        String modId = modIdOf(mod);
        if (ticketCountAvailableFor(modId, world) <= 0) return null;
        Ticket t = new Ticket(modId, type, world, null);
        TICKETS.add(t);
        return t;
    }

    public static synchronized Ticket requestPlayerTicket(Object mod, String player, World world, Type type) {
        if (serverLevel(world) == null) return null;
        Ticket t = new Ticket(modIdOf(mod), type, world, player);
        TICKETS.add(t);
        return t;
    }

    public static synchronized void releaseTicket(Ticket ticket) {
        if (ticket == null || !TICKETS.remove(ticket)) return;
        for (ChunkPos pos : ticket.getChunkList()) unforceChunk(ticket, pos);
    }

    public static void forceChunk(Ticket ticket, ChunkPos chunk) {
        if (ticket == null || chunk == null) return;
        var level = serverLevel(ticket.world);
        if (level == null) return;
        synchronized (ticket) {
            if (ticket.requestedChunks.contains(chunk)) return;
            if (ticket.maxDepth > 0 && ticket.requestedChunks.size() >= ticket.maxDepth) {
                ChunkPos oldest = ticket.requestedChunks.iterator().next();
                unforceChunk(ticket, oldest);
            }
            ticket.requestedChunks.add(chunk);
        }
        var pos = chunk.mcc$modern();
        level.getServer().execute(() -> level.getChunkSource().addRegionTicket(TICKET, pos, 2, pos, true));
    }

    public static void unforceChunk(Ticket ticket, ChunkPos chunk) {
        if (ticket == null || chunk == null) return;
        var level = serverLevel(ticket.world);
        synchronized (ticket) {
            if (!ticket.requestedChunks.remove(chunk)) return;
        }
        if (level == null) return;
        var pos = chunk.mcc$modern();
        level.getServer().execute(() -> level.getChunkSource().removeRegionTicket(TICKET, pos, 2, pos, true));
    }

    public static void reorderChunk(Ticket ticket, ChunkPos chunk) {
        if (ticket == null || chunk == null) return;
        synchronized (ticket) {
            if (ticket.requestedChunks.remove(chunk)) ticket.requestedChunks.add(chunk);
        }
    }

    public static synchronized int ticketCountAvailableFor(Object mod, World world) {
        String modId = modIdOf(mod);
        int used = 0;
        for (Ticket t : TICKETS) if (t.modId.equals(modId) && t.world == world && !t.isPlayerTicket()) used++;
        return Math.max(0, getMaxTicketLengthFor(modId) - used);
    }

    public static int ticketCountAvailableFor(String username) {
        return configInt("playerTicketCount", 500);
    }

    public static int getMaxTicketLengthFor(String modid) {
        return configInt("maximumTicketCount", 200);
    }

    public static int getMaxChunkDepthFor(String modid) {
        return configInt("maximumChunksPerTicket", 25);
    }

    /** The chunks forced in a world, with the tickets that force them. */
    public static synchronized ImmutableSetMultimap<ChunkPos, Ticket> getPersistentChunksFor(World world) {
        ImmutableSetMultimap.Builder<ChunkPos, Ticket> out = ImmutableSetMultimap.builder();
        for (Ticket t : TICKETS) {
            if (t.world != world && (t.world == null || !t.world.equals(world))) continue;
            for (ChunkPos p : t.getChunkList()) out.put(p, t);
        }
        return out.build();
    }

    public static java.util.Iterator<legacyconnector.v1_12_2.net.minecraft.world.chunk.Chunk> getPersistentChunksIterableFor(World world,
            java.util.Iterator<legacyconnector.v1_12_2.net.minecraft.world.chunk.Chunk> chunkIterator) {
        return chunkIterator;
    }

    /** Tickets of a level that unloaded are dropped (the modern tickets went with it). */
    public static synchronized void mcc$unloadWorld(World world) {
        TICKETS.removeIf(t -> t.world == world || t.world != null && t.world.equals(world));
    }
}
