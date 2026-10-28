package org.kylepitcock.legacyconnector.v1_12_2.bridge;

import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayerMP;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraftforge.common.MinecraftForge;
import legacyconnector.v1_12_2.net.minecraftforge.event.world.BlockEvent;
import legacyconnector.v1_12_2.net.minecraftforge.event.world.ChunkWatchEvent;
import legacyconnector.v1_12_2.net.minecraftforge.event.world.NoteBlockEvent;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event;

/**
 * Posts more of Forge 1.12.2's world events from their NeoForge counterparts: chunk watch/unwatch, note block play/change (cancel
 * and changed notes/instruments carried back), fluids placing blocks (the replaced state carried back). Nothing is built when no
 * legacy handler listens; a failing handler is logged (throttled).
 */
public final class WorldEventsW1122 {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger("LegacyConnector/1.12.2");
    private static final java.util.concurrent.atomic.AtomicInteger FAILURES = new java.util.concurrent.atomic.AtomicInteger();
    private static boolean registered;

    private WorldEventsW1122() {}

    private static boolean listens(Class<? extends Event> type) {
        return MinecraftForge.EVENT_BUS.mcc$hasListeners(type);
    }

    private static boolean post(Event e) {
        try {
            return MinecraftForge.EVENT_BUS.post(e);
        } catch (RuntimeException | LinkageError ex) {
            int n = FAILURES.incrementAndGet();
            if (n <= 3 || n % 500 == 0) LOG.warn("A 1.12.2 {} handler failed ({} failures so far)", e.getClass().getSimpleName(), n, ex);
            return false;
        }
    }

    private static EntityPlayerMP player(net.minecraft.server.level.ServerPlayer p) {
        return EntityBridge1122.wrap(p) instanceof EntityPlayerMP mp ? mp : null;
    }

    private static BlockPos pos(net.minecraft.core.BlockPos p) {
        return BlockPos.mcc$of(p);
    }

    public static synchronized void register() {
        if (registered) return;
        registered = true;
        var bus = net.neoforged.neoforge.common.NeoForge.EVENT_BUS;
        bus.addListener((net.neoforged.neoforge.event.level.ChunkWatchEvent.Watch e) -> {
            if (!listens(ChunkWatchEvent.Watch.class)) return;
            var p = player(e.getPlayer());
            if (p == null) return;
            var world = WorldBridge1122.wrap(e.getLevel());
            post(new ChunkWatchEvent.Watch(legacyconnector.v1_12_2.net.minecraft.world.chunk.Chunk.mcc$of(world, e.getChunk()), p));
        });
        bus.addListener((net.neoforged.neoforge.event.level.ChunkWatchEvent.UnWatch e) -> {
            if (!listens(ChunkWatchEvent.UnWatch.class)) return;
            var p = player(e.getPlayer());
            if (p != null) post(new ChunkWatchEvent.UnWatch(legacyconnector.v1_12_2.net.minecraft.util.math.ChunkPos.mcc$of(e.getPos()), p));
        });
        bus.addListener((net.neoforged.neoforge.event.level.NoteBlockEvent.Play e) -> {
            if (!listens(NoteBlockEvent.Play.class) || !(e.getLevel() instanceof net.minecraft.world.level.Level level)) return;
            var legacy = new NoteBlockEvent.Play(WorldBridge1122.wrap(level), pos(e.getPos()), StatesBridge1122.toLegacy(e.getState()), e.getVanillaNoteId(),
                    e.getInstrument().ordinal());
            if (post(legacy)) {
                e.setCanceled(true);
                return;
            }
            e.setNote(net.neoforged.neoforge.event.level.NoteBlockEvent.Note.values()[legacy.getNote().ordinal()],
                    net.neoforged.neoforge.event.level.NoteBlockEvent.Octave.values()[legacy.getOctave().ordinal()]);
            e.setInstrument(net.minecraft.world.level.block.state.properties.NoteBlockInstrument.values()[legacy.getInstrument().ordinal()]);
        });
        bus.addListener((net.neoforged.neoforge.event.level.NoteBlockEvent.Change e) -> {
            if (!listens(NoteBlockEvent.Change.class) || !(e.getLevel() instanceof net.minecraft.world.level.Level level)) return;
            int oldNote = e.getOldNote().ordinal() + e.getOldOctave().ordinal() * 12;
            var legacy = new NoteBlockEvent.Change(WorldBridge1122.wrap(level), pos(e.getPos()), StatesBridge1122.toLegacy(e.getState()), oldNote,
                    e.getVanillaNoteId());
            if (post(legacy)) {
                e.setCanceled(true);
                return;
            }
            e.setNote(net.neoforged.neoforge.event.level.NoteBlockEvent.Note.values()[legacy.getNote().ordinal()],
                    net.neoforged.neoforge.event.level.NoteBlockEvent.Octave.values()[legacy.getOctave().ordinal()]);
        });
        bus.addListener((net.neoforged.neoforge.event.level.BlockEvent.FluidPlaceBlockEvent e) -> {
            if (!listens(BlockEvent.FluidPlaceBlockEvent.class) || !(e.getLevel() instanceof net.minecraft.world.level.Level level)) return;
            var legacy = new BlockEvent.FluidPlaceBlockEvent(WorldBridge1122.wrap(level), pos(e.getPos()), pos(e.getLiquidPos()),
                    StatesBridge1122.toLegacy(e.getNewState()));
            if (post(legacy)) {
                e.setCanceled(true);
                return;
            }
            if (legacy.getNewState() != null) e.setNewState(StatesBridge1122.toModern(legacy.getNewState()));
        });
    }
}
