package mccompat.v1_7_10.net.minecraftforge.common;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.SetMultimap;
import java.io.File;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import mccompat.v1_7_10.cpw.mods.fml.common.Loader;
import mccompat.v1_7_10.cpw.mods.fml.common.ModContainer;
import mccompat.v1_7_10.net.minecraft.entity.Entity;
import mccompat.v1_7_10.net.minecraft.nbt.NBTTagCompound;
import mccompat.v1_7_10.net.minecraft.world.ChunkCoordIntPair;
import mccompat.v1_7_10.net.minecraft.world.World;
import mccompat.v1_7_10.net.minecraftforge.common.config.Configuration;
import net.minecraft.server.level.ServerLevel;

/**
 * Forge's chunk loading tickets. Forced chunks are really kept loaded ({@link ServerLevel#setChunkForced}); a chunk
 * stays forced while any ticket holds it. Tickets are not persisted across restarts (mods re-request them when their
 * blocks load). Per-mod limits come from {@code config/forgeChunkLoading.cfg}, as in Forge.
 */
public class ForgeChunkManager {
    private static final Map<String, LoadingCallback> CALLBACKS = new ConcurrentHashMap<>();
    private static final Map<ServerLevel, SetMultimap<ChunkCoordIntPair, Ticket>> FORCED = new ConcurrentHashMap<>();
    private static Configuration config;

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

    public static class ForceChunkEvent extends mccompat.v1_7_10.cpw.mods.fml.common.eventhandler.Event {
        public final Ticket ticket;
        public final ChunkCoordIntPair location;

        public ForceChunkEvent(Ticket ticket, ChunkCoordIntPair location) {
            this.ticket = ticket;
            this.location = location;
        }
    }

    public static class UnforceChunkEvent extends mccompat.v1_7_10.cpw.mods.fml.common.eventhandler.Event {
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

    public static int getMaxTicketLengthFor(String modId) {
        return getConfig().hasCategory(modId) ? getConfig().get(modId, "maximumTicketCount", 200).getInt()
                : getConfig().get("defaults", "maximumTicketCount", 200).getInt();
    }

    public static int getMaxChunkDepthFor(String modId) {
        return getConfig().hasCategory(modId) ? getConfig().get(modId, "maximumChunksPerTicket", 25).getInt()
                : getConfig().get("defaults", "maximumChunksPerTicket", 25).getInt();
    }

    public static int ticketCountAvailableFor(String player) {
        return getConfig().get("defaults", "playerTicketCount", 500).getInt();
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
