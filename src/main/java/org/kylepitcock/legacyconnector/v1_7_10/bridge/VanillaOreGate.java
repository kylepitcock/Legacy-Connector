package org.kylepitcock.legacyconnector.v1_7_10.bridge;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import legacyconnector.v1_7_10.net.minecraftforge.common.MinecraftForge;
import legacyconnector.v1_7_10.net.minecraftforge.event.terraingen.OreGenEvent;
import legacyconnector.v1_7_10.net.minecraftforge.event.terraingen.TerrainGen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/**
 * 1.7.10 mods turn vanilla ores off by denying Forge's {@code OreGenEvent.GenerateMinable} (CustomOreGen with
 * {@code vanillaOreGen = false}). The modern overworld's ores are placed features that never asked: before one of them is
 * placed, the event for its ore type is posted for the chunk, and a denied type is not placed. Nothing is posted unless a
 * legacy mod listens for the event.
 */
public final class VanillaOreGate {
    private static final Map<PlacedFeature, Optional<OreGenEvent.GenerateMinable.EventType>> TYPES = new ConcurrentHashMap<>();

    private VanillaOreGate() {}

    /** Whether the placed feature may run at {@code origin} (the event gets its own random: the feature's must not move). */
    public static boolean allow(PlacedFeature feature, WorldGenLevel level, RandomSource random, BlockPos origin) {
        if (!MinecraftForge.ORE_GEN_BUS.mcc$hasListeners(OreGenEvent.GenerateMinable.class)) return true;
        var type = TYPES.computeIfAbsent(feature, f -> level.registryAccess().registry(Registries.PLACED_FEATURE)
                .flatMap(r -> r.getResourceKey(f)).map(k -> k.location()).filter(id -> id.getNamespace().equals("minecraft"))
                .map(id -> typeOf(id.getPath())));
        if (type.isEmpty()) return true;
        try {
            var world = legacyconnector.v1_7_10.net.minecraft.world.World.mcc$wrap(level);
            return TerrainGen.generateOre(world, new java.util.Random(origin.asLong()), null, origin.getX() & ~15, origin.getZ() & ~15, type.get());
        } catch (RuntimeException | LinkageError e) {
            return true;
        }
    }

    private static OreGenEvent.GenerateMinable.EventType typeOf(String path) {
        if (!path.startsWith("ore_")) return null;
        String ore = path.substring(4);
        if (ore.startsWith("coal")) return OreGenEvent.GenerateMinable.EventType.COAL;
        if (ore.startsWith("iron")) return OreGenEvent.GenerateMinable.EventType.IRON;
        if (ore.startsWith("gold")) return OreGenEvent.GenerateMinable.EventType.GOLD;
        if (ore.startsWith("redstone")) return OreGenEvent.GenerateMinable.EventType.REDSTONE;
        if (ore.startsWith("diamond")) return OreGenEvent.GenerateMinable.EventType.DIAMOND;
        if (ore.startsWith("lapis")) return OreGenEvent.GenerateMinable.EventType.LAPIS;
        if (ore.equals("dirt")) return OreGenEvent.GenerateMinable.EventType.DIRT;
        if (ore.equals("gravel")) return OreGenEvent.GenerateMinable.EventType.GRAVEL;
        if (ore.equals("quartz_nether") || ore.equals("quartz_deltas")) return OreGenEvent.GenerateMinable.EventType.QUARTZ;
        return null;
    }
}
