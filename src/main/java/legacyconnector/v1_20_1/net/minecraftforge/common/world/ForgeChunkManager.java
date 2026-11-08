package legacyconnector.v1_20_1.net.minecraftforge.common.world;

import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent;
import net.neoforged.neoforge.common.world.chunk.TicketController;
import net.neoforged.neoforge.common.world.chunk.TicketSet;

/**
 * Forge 1.20.1's {@code ForgeChunkManager} over NeoForge's {@link TicketController}: one controller per mod id (registered when
 * {@link RegisterTicketControllersEvent} fires, for every 1.20.1 mod), whose validation callback is the one the mod set with
 * {@link #setForcedChunkLoadingCallback}. A request for a mod whose controller is not registered (it came too late, or the mod id is unknown) falls back to
 * the vanilla forced chunk of the level.
 */
public final class ForgeChunkManager {
    private static final Map<String, TicketController> CONTROLLERS = new ConcurrentHashMap<>();
    private static final Map<String, LoadingValidationCallback> CALLBACKS = new ConcurrentHashMap<>();

    private ForgeChunkManager() {
    }

    /** The 1.20.1 validation callback: the mod removes tickets it no longer wants when a level loads. */
    @FunctionalInterface
    public interface LoadingValidationCallback {
        void validateTickets(ServerLevel level, TicketHelper ticketHelper);
    }

    /** The tickets of one mod in one level, as 1.20.1 showed them: per owner a pair of (non ticking, ticking) chunk position sets. */
    public static final class TicketHelper {
        private final net.neoforged.neoforge.common.world.chunk.TicketHelper helper;

        TicketHelper(net.neoforged.neoforge.common.world.chunk.TicketHelper helper) {
            this.helper = helper;
        }

        public Map<BlockPos, Pair<LongSet, LongSet>> getBlockTickets() {
            Map<BlockPos, Pair<LongSet, LongSet>> out = new HashMap<>();
            for (Map.Entry<BlockPos, TicketSet> e : helper.getBlockTickets().entrySet()) out.put(e.getKey(), Pair.of(e.getValue().nonTicking(), e.getValue().ticking()));
            return out;
        }

        public Map<UUID, Pair<LongSet, LongSet>> getEntityTickets() {
            Map<UUID, Pair<LongSet, LongSet>> out = new HashMap<>();
            for (Map.Entry<UUID, TicketSet> e : helper.getEntityTickets().entrySet()) out.put(e.getKey(), Pair.of(e.getValue().nonTicking(), e.getValue().ticking()));
            return out;
        }

        public void removeAllTickets(BlockPos owner) {
            helper.removeAllTickets(owner);
        }

        public void removeAllTickets(UUID owner) {
            helper.removeAllTickets(owner);
        }

        public void removeTicket(BlockPos owner, long chunk, boolean ticking) {
            helper.removeTicket(owner, chunk, ticking);
        }

        public void removeTicket(UUID owner, long chunk, boolean ticking) {
            helper.removeTicket(owner, chunk, ticking);
        }
    }

    public static void setForcedChunkLoadingCallback(String modId, LoadingValidationCallback callback) {
        CALLBACKS.put(modId, callback);
    }

    /** Mod bus listener: a controller for each 1.20.1 mod id. */
    public static void registerControllers(RegisterTicketControllersEvent event, Iterable<String> modIds) {
        for (String id : modIds) {
            TicketController c = new TicketController(ResourceLocation.fromNamespaceAndPath(id, "legacy_chunks"), (level, helper) -> {
                LoadingValidationCallback cb = CALLBACKS.get(id);
                if (cb != null) cb.validateTickets(level, new TicketHelper(helper));
            });
            event.register(c);
            CONTROLLERS.put(id, c);
        }
    }

    public static boolean forceChunk(ServerLevel level, String modId, BlockPos owner, int chunkX, int chunkZ, boolean add, boolean ticking) {
        TicketController c = CONTROLLERS.get(modId);
        return c != null ? c.forceChunk(level, owner, chunkX, chunkZ, add, ticking) : level.setChunkForced(chunkX, chunkZ, add);
    }

    public static boolean forceChunk(ServerLevel level, String modId, Entity owner, int chunkX, int chunkZ, boolean add, boolean ticking) {
        TicketController c = CONTROLLERS.get(modId);
        return c != null ? c.forceChunk(level, owner, chunkX, chunkZ, add, ticking) : level.setChunkForced(chunkX, chunkZ, add);
    }

    public static boolean forceChunk(ServerLevel level, String modId, UUID owner, int chunkX, int chunkZ, boolean add, boolean ticking) {
        TicketController c = CONTROLLERS.get(modId);
        return c != null ? c.forceChunk(level, owner, chunkX, chunkZ, add, ticking) : level.setChunkForced(chunkX, chunkZ, add);
    }

    /** Whether any chunk of the level is force loaded. */
    public static boolean hasForcedChunks(ServerLevel level) {
        return !level.getForcedChunks().isEmpty();
    }
}
