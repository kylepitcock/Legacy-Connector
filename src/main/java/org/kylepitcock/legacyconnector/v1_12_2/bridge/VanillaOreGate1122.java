package org.kylepitcock.legacyconnector.v1_12_2.bridge;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import legacyconnector.v1_12_2.net.minecraftforge.common.MinecraftForge;
import legacyconnector.v1_12_2.net.minecraftforge.event.terraingen.OreGenEvent;
import legacyconnector.v1_12_2.net.minecraftforge.event.terraingen.TerrainGen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/**
 * 1.12.2 mods turn vanilla ores off by denying Forge's {@code OreGenEvent.GenerateMinable} (CustomOreGen with
 * {@code vanillaOreGen = false}). The modern ores are placed features that never asked: before one is placed, the event for its
 * ore type is posted and a denied type is not placed (like {@code VanillaOreGate} for 1.7.10). Nothing is posted unless a
 * legacy mod listens for the event.
 */
public final class VanillaOreGate1122 {
    private static final Map<PlacedFeature, Optional<OreGenEvent.GenerateMinable.EventType>> TYPES = new ConcurrentHashMap<>();

    private VanillaOreGate1122() {}

    public static boolean allow(PlacedFeature feature, WorldGenLevel level, RandomSource random, BlockPos origin) {
        if (!MinecraftForge.ORE_GEN_BUS.mcc$hasListeners(OreGenEvent.GenerateMinable.class)) return true;
        var type = TYPES.computeIfAbsent(feature, f -> level.registryAccess().registry(Registries.PLACED_FEATURE)
                .flatMap(r -> r.getResourceKey(f)).map(k -> k.location()).filter(id -> id.getNamespace().equals("minecraft"))
                .map(id -> typeOf(id.getPath())));
        if (type.isEmpty()) return true;
        try {
            var world = WorldBridge1122.wrap(level);
            try {
                return TerrainGen.generateOre(world, new java.util.Random(origin.asLong()), null,
                        new legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos(origin.getX() & ~15, 0, origin.getZ() & ~15), type.get());
            } finally {
                WorldBridge1122.mcc$releaseRegion();
            }
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
        if (ore.startsWith("emerald")) return OreGenEvent.GenerateMinable.EventType.EMERALD;
        if (ore.startsWith("granite")) return OreGenEvent.GenerateMinable.EventType.GRANITE;
        if (ore.startsWith("diorite")) return OreGenEvent.GenerateMinable.EventType.DIORITE;
        if (ore.startsWith("andesite")) return OreGenEvent.GenerateMinable.EventType.ANDESITE;
        if (ore.equals("infested")) return OreGenEvent.GenerateMinable.EventType.SILVERFISH;
        if (ore.equals("dirt")) return OreGenEvent.GenerateMinable.EventType.DIRT;
        if (ore.equals("gravel")) return OreGenEvent.GenerateMinable.EventType.GRAVEL;
        if (ore.equals("quartz_nether") || ore.equals("quartz_deltas")) return OreGenEvent.GenerateMinable.EventType.QUARTZ;
        return null;
    }
}
