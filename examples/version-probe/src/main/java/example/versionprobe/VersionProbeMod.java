package example.versionprobe;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import org.kylepitcock.legacyconnector.api.LegacyApi;
import org.kylepitcock.legacyconnector.api.LegacyBlocks;
import org.kylepitcock.legacyconnector.api.LegacyVersion;
import org.kylepitcock.legacyconnector.api.LegacyWorlds;
import org.kylepitcock.legacyconnector.api.event.LegacyLoadCompleteEvent;
import org.kylepitcock.legacyconnector.api.event.LegacyModsDiscoveredEvent;
import org.kylepitcock.legacyconnector.api.event.LegacyRegistrationFinishedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The addon is a normal NeoForge mod; this class listens to the three lifecycle events on the mod bus, to show they arrive, and once
 * the server runs places up to two tile-entity blocks of every legacy mod to probe the tile conversions and the capability lookup.
 */
@Mod("versionprobe")
public final class VersionProbeMod {
    private static final Logger LOG = LoggerFactory.getLogger("VersionProbe");

    public VersionProbeMod(IEventBus modBus) {
        modBus.addListener(LegacyModsDiscoveredEvent.class, e -> LOG.info("PROBE event LegacyModsDiscoveredEvent mods={}", e.getMods().size()));
        modBus.addListener(LegacyRegistrationFinishedEvent.class, e -> LOG.info("PROBE event LegacyRegistrationFinishedEvent"));
        modBus.addListener(LegacyLoadCompleteEvent.class, e -> LOG.info("PROBE event LegacyLoadCompleteEvent"));
        NeoForge.EVENT_BUS.addListener(ServerStartedEvent.class, e -> {
            try {
                probeTiles(e.getServer().overworld());
            } catch (Throwable t) {
                LOG.info("PROBE tiles FAILED {}", t.toString());
            }
        });
    }

    private static void probeTiles(ServerLevel level) {
        Map<String, Integer> perMod = new HashMap<>();
        int n = 0;
        for (Block block : BuiltInRegistries.BLOCK) {
            if (!(block instanceof EntityBlock)) continue;
            var id = BuiltInRegistries.BLOCK.getKey(block);
            var mod = LegacyApi.mod(id.getNamespace());
            if (mod.isEmpty() || perMod.merge(id.getNamespace(), 1, Integer::sum) > 2) continue;
            LegacyVersion v = mod.get().minecraftVersion();
            BlockPos pos = new BlockPos(8 + 2 * n++, 200, 8);
            level.getChunk(pos);
            try {
                level.setBlock(pos, block.defaultBlockState(), 3);
                BlockEntity be = level.getBlockEntity(pos);
                if (be == null) {
                    LOG.info("PROBE tile mod={} mc={} {} no block entity", id.getNamespace(), v.id(), id);
                    continue;
                }
                var tile = LegacyBlocks.legacyTile(be);
                var tv = LegacyBlocks.tileVersion(be);
                Object world = LegacyWorlds.toLegacy(level, v);
                boolean same = world == LegacyWorlds.toLegacy(level, v);
                var items = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
                var fluids = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, null);
                var energy = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, null);
                boolean ok = tile.isPresent() && tv.isPresent() && tv.get() == v && world != null && same;
                LOG.info("PROBE tile mod={} mc={} {} tile={} tileVersion={} world={} items={} fluids={} energy={} {}", id.getNamespace(), v.id(), id,
                        tile.map(t -> t.getClass().getName()).orElse("-"), tv.map(LegacyVersion::id).orElse("-"), world == null ? "-" : world.getClass().getSimpleName(),
                        items != null, fluids != null, energy != null, ok ? "ok" : "BAD");
            } catch (Throwable t) {
                LOG.info("PROBE tile mod={} {} FAILED {}", id.getNamespace(), id, t.toString());
            } finally {
                level.removeBlock(pos, false);
            }
        }
        LOG.info("PROBE tiles done: {} blocks tried", n);
    }
}
