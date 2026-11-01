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

    private static final java.util.Set<String> DESCRIBED = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private static final java.util.Set<String> NOT_BUILDINGS = java.util.concurrent.ConcurrentHashMap.newKeySet();

    /** The vanilla village house pools, by village type. */
    private static final List<String> VILLAGE_TYPES = List.of("plains", "desert", "savanna", "snowy", "taiga");

    /** Total weight of 1.7.10's own village buildings (House4Garden 4, Church 20, House1 20, WoodHut 3, Hall 15, Field1 3, Field2 3, House2 15, House3 8). */
    private static final float VANILLA_1710_WEIGHT = 91.0F;

    /**
     * Adds every legacy village building to the house pool of each village type, with the share of the village it had on 1.7.10:
     * there a building of weight w was picked w times in (91 + the mods' weights) against vanilla's buildings, so here it gets
     * w / 91 of the pool's own (non-empty) weight. The jigsaw generator then picks it like the pool's other houses.
     */
    private static void addToPools(net.neoforged.neoforge.event.server.ServerAboutToStartEvent event) {
        var handlers = VillagerBridge.creationHandlers();
        if (handlers.isEmpty()) return;
        if (!org.kylepitcock.legacyconnector.core.LegacyConfig.villageLegacyBuildings()) {
            LOG.info("Legacy village buildings are switched off (villages.legacyBuildings=false)");
            return;
        }
        var pools = event.getServer().registryAccess().registryOrThrow(Registries.TEMPLATE_POOL);
        int added = 0;
        for (String type : VILLAGE_TYPES) {
            var pool = pools.get(net.minecraft.resources.ResourceLocation.withDefaultNamespace("village/" + type + "/houses"));
            if (pool == null) continue;
            float poolWeight = houseWeight(pool);
            for (var handler : handlers) {
                int weight;
                try {
                    var w = handler.getVillagePieceWeight(new Random(handler.getClass().getName().hashCode()), 0);
                    if (w == null || w.villagePieceWeight <= 0) continue;
                    weight = Math.max(1, Math.round(poolWeight * w.villagePieceWeight / VANILLA_1710_WEIGHT));
                } catch (RuntimeException | LinkageError e) {
                    LOG.warn("Village creation handler {} failed: {}", handler.getClass().getName(), e.toString());
                    continue;
                }
                var element = new LegacyVillagePoolElement(handler.getClass().getName(), type,
                        net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool.Projection.RIGID);
                if (!element.isBuilding()) {
                    if (NOT_BUILDINGS.add(handler.getClass().getName())) {
                        LOG.info("Legacy village piece {} is not added to the village pools: it builds no footprint of at least 3x3", handler.getClass().getName());
                    }
                    continue;
                }
                if (addToPool(pool, element, weight)) {
                    added++;
                    if (DESCRIBED.add(handler.getClass().getName())) {
                        LOG.info("Legacy village building {}: 1.7.10 weight {} -> pool weight {} (of {}), footprint {}", handler.getClass().getName(),
                                Math.round(weight * VANILLA_1710_WEIGHT / poolWeight), weight, Math.round(poolWeight), element.describeFootprint());
                    }
                }
            }
        }
        if (added > 0) LOG.info("Added {} legacy village building(s) to the village house pools", added);
    }

    /** The pool's weight of real buildings (empty elements, the pool's "no house here", left out). */
    private static float houseWeight(net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool pool) {
        java.util.Map<Object, Integer> counts = new java.util.IdentityHashMap<>();
        var rnd = net.minecraft.util.RandomSource.create(0);
        for (var e : pool.getShuffledTemplates(rnd)) {
            if (e instanceof net.minecraft.world.level.levelgen.structure.pools.EmptyPoolElement) continue;
            counts.merge(e, 1, Integer::sum);
        }
        int total = 0;
        for (int c : counts.values()) total += c;
        return Math.max(1, total);
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
}
