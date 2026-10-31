package org.kylepitcock.legacyconnector.v1_7_10.bridge;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import legacyconnector.v1_7_10.net.minecraft.world.gen.structure.StructureBoundingBox;
import legacyconnector.v1_7_10.net.minecraft.world.gen.structure.StructureComponent;
import legacyconnector.v1_7_10.net.minecraft.world.gen.structure.StructureVillagePieces;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import org.kylepitcock.legacyconnector.core.worldgen.LegacyWorldGenFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Legacy village components (Forge village creation handlers: Witchery's walls and shops, AgriCraft's greenhouse) in the modern,
 * jigsaw-based villages: each becomes an element of the village house pools ({@link LegacyVillagePoolElement}), so the village
 * generator places it like a modern mod's house, at a street, clear of the other buildings.
 */
public final class LegacyVillages {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/Villages");
    private static boolean registered;

    private LegacyVillages() {}

    public static synchronized void register() {
        if (registered) return;
        registered = true;
        // Legacy buildings join the villages as jigsaw pool elements, like any modern mod's houses (LegacyVillagePoolElement):
        // planned beside the streets by this class instead, they crowded and overlapped the modern buildings.
        net.minecraft.world.level.levelgen.structure.pools.StructurePoolElementType<LegacyVillagePoolElement> type = () -> LegacyVillagePoolElement.CODEC;
        org.kylepitcock.legacyconnector.core.registry.LegacyRegistrar.register(net.minecraft.core.registries.BuiltInRegistries.STRUCTURE_POOL_ELEMENT,
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("legacyconnector", "legacy_village_building"), type);
        LegacyVillagePoolElement.TYPE = type;
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(LegacyVillages::addToPools);
    }

    /** The vanilla village house pools, by village type. */
    private static final List<String> VILLAGE_TYPES = List.of("plains", "desert", "savanna", "snowy", "taiga");

    /**
     * Adds every legacy village building to the house pool of each village type. 1.7.10 weights (vanilla's houses 3-20) are
     * scaled to the modern pools' (2-ish per house); the jigsaw generator then picks them like the pool's other houses.
     */
    private static void addToPools(net.neoforged.neoforge.event.server.ServerAboutToStartEvent event) {
        var handlers = VillagerBridge.creationHandlers();
        if (handlers.isEmpty()) return;
        var pools = event.getServer().registryAccess().registryOrThrow(Registries.TEMPLATE_POOL);
        int added = 0;
        for (String type : VILLAGE_TYPES) {
            var pool = pools.get(net.minecraft.resources.ResourceLocation.withDefaultNamespace("village/" + type + "/houses"));
            if (pool == null) continue;
            for (var handler : handlers) {
                int weight;
                try {
                    var w = handler.getVillagePieceWeight(new Random(handler.getClass().getName().hashCode()), 0);
                    if (w == null) continue;
                    weight = Math.max(1, Math.min(6, Math.round(w.villagePieceWeight / 4.0F)));
                } catch (RuntimeException | LinkageError e) {
                    LOG.warn("Village creation handler {} failed: {}", handler.getClass().getName(), e.toString());
                    continue;
                }
                var element = new LegacyVillagePoolElement(handler.getClass().getName(), type,
                        net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool.Projection.RIGID);
                if (addToPool(pool, element, weight)) added++;
            }
        }
        if (added > 0) LOG.info("Added {} legacy village building(s) to the village house pools", added);
    }

    @SuppressWarnings("unchecked")
    private static boolean addToPool(net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool pool,
            net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement element, int weight) {
        try {
            Class<?> c = net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool.class;
            for (java.lang.reflect.Field f : c.getDeclaredFields()) {
                if (java.lang.reflect.Modifier.isStatic(f.getModifiers())) continue;
                f.setAccessible(true);
                Object v = f.get(pool);
                if (v instanceof it.unimi.dsi.fastutil.objects.ObjectArrayList<?> list) {
                    for (int i = 0; i < weight; i++) ((it.unimi.dsi.fastutil.objects.ObjectArrayList<Object>) list).add(element);
                } else if (v instanceof List<?> raw && f.getType() == List.class) {
                    List<Object> copy = new ArrayList<>((List<Object>) raw);
                    copy.add(com.mojang.datafixers.util.Pair.of(element, weight));
                    f.set(pool, copy);
                }
            }
            return true;
        } catch (ReflectiveOperationException | RuntimeException e) {
            LOG.warn("Could not add a legacy village building to a village pool: {}", e.toString());
            return false;
        }
    }

    /** The vanilla village house pools, by village type. */
    private static final List<String> VILLAGE_TYPES = List.of("plains", "desert", "savanna", "snowy", "taiga");

    /**
     * Adds every legacy village building to the house pool of each village type. 1.7.10 weights (vanilla's houses 3-20) are
     * scaled to the modern pools' (2-ish per house); the jigsaw generator then picks them like the pool's other houses.
     */
    private static void addToPools(net.neoforged.neoforge.event.server.ServerAboutToStartEvent event) {
        var handlers = VillagerBridge.creationHandlers();
        if (handlers.isEmpty()) return;
        var pools = event.getServer().registryAccess().registryOrThrow(Registries.TEMPLATE_POOL);
        int added = 0;
        for (String type : VILLAGE_TYPES) {
            var pool = pools.get(net.minecraft.resources.ResourceLocation.withDefaultNamespace("village/" + type + "/houses"));
            if (pool == null) continue;
            for (var handler : handlers) {
                int weight;
                try {
                    var w = handler.getVillagePieceWeight(new Random(handler.getClass().getName().hashCode()), 0);
                    if (w == null) continue;
                    weight = Math.max(1, Math.min(6, Math.round(w.villagePieceWeight / 4.0F)));
                } catch (RuntimeException | LinkageError e) {
                    LOG.warn("Village creation handler {} failed: {}", handler.getClass().getName(), e.toString());
                    continue;
                }
                var element = new LegacyVillagePoolElement(handler.getClass().getName(), type,
                        net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool.Projection.RIGID);
                if (addToPool(pool, element, weight)) added++;
            }
        }
        if (added > 0) LOG.info("Added {} legacy village building(s) to the village house pools", added);
    }

    @SuppressWarnings("unchecked")
    private static boolean addToPool(net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool pool,
            net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement element, int weight) {
        try {
            Class<?> c = net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool.class;
            for (java.lang.reflect.Field f : c.getDeclaredFields()) {
                if (java.lang.reflect.Modifier.isStatic(f.getModifiers())) continue;
                f.setAccessible(true);
                Object v = f.get(pool);
                if (v instanceof it.unimi.dsi.fastutil.objects.ObjectArrayList<?> list) {
                    for (int i = 0; i < weight; i++) ((it.unimi.dsi.fastutil.objects.ObjectArrayList<Object>) list).add(element);
                } else if (v instanceof List<?> raw && f.getType() == List.class) {
                    List<Object> copy = new ArrayList<>((List<Object>) raw);
                    copy.add(com.mojang.datafixers.util.Pair.of(element, weight));
                    f.set(pool, copy);
                }
            }
            return true;
        } catch (ReflectiveOperationException | RuntimeException e) {
            LOG.warn("Could not add a legacy village building to a village pool: {}", e.toString());
            return false;
        }
    }

    /** A piece that is already there (a modern village piece): occupies space, builds nothing. */
    private static final class Occupied extends StructureVillagePieces.Village {
        Occupied(StructureBoundingBox box) {
            this.boundingBox = box;
        }

        @Override
        protected void writeStructureToNBT(legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound tag) {
        }

        @Override
        protected void readStructureFromNBT(legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound tag) {
        }

        @Override
        public boolean addComponentParts(World world, Random rand, StructureBoundingBox box) {
            return true;
        }
    }



: x, y, z of the corner at the
     * street and the facing that turns the building's front to it. Without streets, a random spot in the village.
     */
    private static int[] plot(List<BoundingBox> streets, BoundingBox village, Random rand) {
        if (streets.isEmpty()) {
            int x = village.minX() + 3 + rand.nextInt(Math.max(1, village.getXSpan() - 6));
            int z = village.minZ() + 3 + rand.nextInt(Math.max(1, village.getZSpan() - 6));
            return new int[] {x, village.minY(), z, rand.nextInt(4)};
        }
        BoundingBox s = streets.get(rand.nextInt(streets.size()));
        boolean low = rand.nextBoolean();
        if (s.getXSpan() >= s.getZSpan()) {
            // the street runs along x: plots north (facing 2) or south (facing 0) of it
            int x = s.minX() + rand.nextInt(Math.max(1, s.getXSpan()));
            return low ? new int[] {x, s.minY(), s.minZ() - 1, 2} : new int[] {x, s.minY(), s.maxZ() + 1, 0};
        }
        int z = s.minZ() + rand.nextInt(Math.max(1, s.getZSpan()));
        return low ? new int[] {s.minX() - 1, s.minY(), z, 1} : new int[] {s.maxX() + 1, s.minY(), z, 3};
    }
}
