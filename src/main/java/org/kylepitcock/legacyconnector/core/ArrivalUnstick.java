package org.kylepitcock.legacyconnector.core;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A player who arrives in a legacy mod dimension inside solid blocks (a mod placed the arrival point with the height of a
 * chunk that was not generated yet, or with the source world's height: The Midnight's rifts) is moved to the nearest free
 * standing spot above. The legacy portal code is left alone: this only corrects a result that would suffocate the player.
 */
public final class ArrivalUnstick {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/Arrival");
    private static boolean registered;
    private static final List<Pending> PENDING = new ArrayList<>();

    private ArrivalUnstick() {}

    private static final class Pending {
        final ServerPlayer player;
        int ticks;

        Pending(ServerPlayer player) {
            this.player = player;
        }
    }

    private static final java.util.Map<java.util.UUID, net.minecraft.resources.ResourceKey<Level>> LAST = new java.util.HashMap<>();

    public static synchronized void register() {
        if (registered) return;
        registered = true;
        // the dimension is compared every tick: legacy portal code moves players without firing the dimension-change event
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> {
            for (ServerPlayer p : e.getServer().getPlayerList().getPlayers()) {
                var dim = p.level().dimension();
                var before = LAST.put(p.getUUID(), dim);
                if (before != null && !before.equals(dim) && "legacyconnector".equals(dim.location().getNamespace())) {
                    synchronized (PENDING) {
                        PENDING.add(new Pending(p));
                    }
                }
            }
            tick();
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> LAST.remove(e.getEntity().getUUID()));
    }

    private static void tick() {
        synchronized (PENDING) {
            if (PENDING.isEmpty()) return;
            PENDING.removeIf(p -> {
                p.ticks++;
                if (p.player.isRemoved() || p.player.hasDisconnected()) return true;
                // wait a few ticks for the arrival chunk to be sent and the legacy code to finish placing the player
                if (p.ticks < 10) return false;
                try {
                    fix(p.player);
                } catch (RuntimeException ex) {
                    LOG.warn("Arrival check failed: {}", ex.toString());
                }
                return true;
            });
        }
    }

    private static boolean free(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).getCollisionShape(level, pos).isEmpty() && level.getFluidState(pos).isEmpty();
    }

    private static boolean solidBelow(ServerLevel level, BlockPos pos) {
        BlockPos below = pos.below();
        return !level.getBlockState(below).getCollisionShape(level, below).isEmpty();
    }

    private static void fix(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) return;
        if (player.isSpectator() || player.getAbilities().flying) return;
        if (!player.isInWall()) return;
        BlockPos start = player.blockPosition();
        int top = level.getMaxBuildHeight() - 2;
        for (int y = start.getY(); y < top; y++) {
            BlockPos feet = new BlockPos(start.getX(), y, start.getZ());
            if (free(level, feet) && free(level, feet.above()) && solidBelow(level, feet)) {
                player.teleportTo(level, player.getX(), y, player.getZ(), player.getYRot(), player.getXRot());
                LOG.info("{} arrived inside blocks in {} at y={}; moved to y={}", player.getGameProfile().getName(), level.dimension().location(), start.getY(), y);
                return;
            }
        }
    }
}
