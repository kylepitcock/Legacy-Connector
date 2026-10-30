package org.kylepitcock.legacyconnector.v1_12_2.bridge;

import java.util.IdentityHashMap;
import java.util.Map;
import legacyconnector.v1_12_2.net.minecraft.block.Block;
import legacyconnector.v1_12_2.net.minecraft.block.material.Material;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.kylepitcock.legacyconnector.core.Ids;
import org.kylepitcock.legacyconnector.core.registry.LegacyRegistrar;
import org.kylepitcock.legacyconnector.v1_12_2.adapter.Legacy1122Block;

/** Turns 1.12.2 blocks registered through {@code RegistryEvent.Register<Block>} into modern blocks. */
public final class BlockBridge1122 {
    private static final Map<Block, net.minecraft.world.level.block.Block> MODERN = new IdentityHashMap<>();

    private BlockBridge1122() {}

    static ResourceLocation modernId(legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation name) {
        return ResourceLocation.fromNamespaceAndPath(Ids.modernModId(name.getNamespace()), Ids.normalizePath(name.getPath()).replace('/', '_'));
    }

    static void register(Block block) {
        var id = modernId(block.getRegistryName());
        var legacySound = block.getSoundType();
        BlockBehaviour.Properties p = BlockBehaviour.Properties.of().sound(legacySound != null && legacySound.mcc$modern != null
                ? legacySound.mcc$modern : sound(block.getMaterialLegacy()));
        float hardness = block.mcc$hardness();
        if (hardness < 0) p.strength(-1.0F, 3600000.0F);
        // 1.12 kept three times the setResistance value and divided by five when an explosion asked (stone: 30 -> 6, modern stone 6)
        else p.strength(hardness, block.mcc$resistance() / 5.0F);
        int light = block.mcc$light();
        if (light > 0) p.lightLevel(s -> light);
        if (block.slipperiness != 0.6F) p.friction(block.slipperiness);
        Material m = block.getMaterialLegacy();
        if (m != null && !m.isOpaque()) p.noOcclusion();
        if (m != null && !m.blocksMovement()) p.noCollission();
        if (m != null) {
            p.pushReaction(m.mcc$pushReaction());
            if (m.getCanBurn()) p.ignitedByLava();
            if (m.isReplaceable()) p.replaceable();
        }
        var spawn = creatureSpawnHook(block);
        if (spawn != null) p.isValidSpawn((state, level, pos, type) -> canCreatureSpawn(block, spawn, state, level, pos, type));
        boolean entity = false;
        try {
            entity = block.hasTileEntity(block.getDefaultState());
        } catch (RuntimeException | LinkageError e) {
            // a block that cannot answer before it has a world has no tile entity
        }
        Legacy1122Block modern;
        if (entity) {
            var eb = new org.kylepitcock.legacyconnector.v1_12_2.adapter.Legacy1122EntityBlock(block, p);
            TileEntityBridge1122.addBlock(eb);
            modern = eb;
        } else {
            modern = new Legacy1122Block(block, p);
        }
        LegacyRegistrar.register(BuiltInRegistries.BLOCK, id, modern);
        block.mcc$modern = modern;
        synchronized (MODERN) {
            MODERN.put(block, modern);
        }
    }

    public static net.minecraft.world.level.block.Block modern(Block block) {
        synchronized (MODERN) {
            return MODERN.get(block);
        }
    }

    /**
     * The mod's own Forge {@code canCreatureSpawn(state, world, pos, placementType)} (its last parameter is a synthesized enum, so it is
     * found by name), or null when the block keeps the modern rule (a sturdy top face).
     */
    private static java.lang.reflect.Method creatureSpawnHook(Block block) {
        for (Class<?> c = block.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            String name = c.getName();
            if (name.startsWith("legacyconnector.") || name.startsWith("legacyconnector_synth.")) return null;
            try {
                for (var m : c.getDeclaredMethods()) {
                    if (m.getName().equals("canCreatureSpawn") && m.getParameterCount() == 4 && m.getReturnType() == boolean.class
                            && !java.lang.reflect.Modifier.isStatic(m.getModifiers())) {
                        m.setAccessible(true);
                        return m;
                    }
                }
            } catch (LinkageError | RuntimeException e) {
                return null;
            }
        }
        return null;
    }

    private static int spawnFailures;

    /** Asks the legacy block whether a mob of this type may spawn on it, with the 1.12 placement type of the entity type. */
    private static boolean canCreatureSpawn(Block block, java.lang.reflect.Method hook, net.minecraft.world.level.block.state.BlockState state,
            net.minecraft.world.level.BlockGetter level, net.minecraft.core.BlockPos pos, net.minecraft.world.entity.EntityType<?> type) {
        try {
            var placement = net.minecraft.world.entity.SpawnPlacements.getPlacementType(type);
            String legacy = placement == net.minecraft.world.entity.SpawnPlacementTypes.IN_WATER ? "IN_WATER"
                    : placement == net.minecraft.world.entity.SpawnPlacementTypes.NO_RESTRICTIONS ? "IN_AIR" : "ON_GROUND";
            Object legacyType = null;
            Object[] constants = hook.getParameterTypes()[3].getEnumConstants();
            if (constants != null) for (Object c : constants) if (((Enum<?>) c).name().equals(legacy)) legacyType = c;
            return (Boolean) hook.invoke(block, StatesBridge1122.toLegacy(state), WorldBridge1122.access(level),
                    legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos.mcc$of(pos), legacyType);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            if (spawnFailures++ < 3) {
                org.slf4j.LoggerFactory.getLogger("LegacyConnector/Blocks").warn("canCreatureSpawn of {} failed; the modern rule applies", block.getClass().getName(), e);
            }
            return state.isFaceSturdy(level, pos, net.minecraft.core.Direction.UP) && state.getLightEmission(level, pos) < 14;
        }
    }

    private static SoundType sound(Material m) {
        if (m == null) return SoundType.STONE;
        if (m == Material.WOOD) return SoundType.WOOD;
        if (m == Material.GROUND || m == Material.CLAY) return SoundType.GRAVEL;
        if (m == Material.GRASS || m == Material.PLANTS || m == Material.LEAVES || m == Material.VINE) return SoundType.GRASS;
        if (m == Material.SAND) return SoundType.SAND;
        if (m == Material.GLASS || m == Material.ICE || m == Material.PACKED_ICE) return SoundType.GLASS;
        if (m == Material.IRON || m == Material.ANVIL) return SoundType.METAL;
        if (m == Material.CLOTH || m == Material.CARPET) return SoundType.WOOL;
        if (m == Material.SNOW || m == Material.CRAFTED_SNOW) return SoundType.SNOW;
        return SoundType.STONE;
    }
}
