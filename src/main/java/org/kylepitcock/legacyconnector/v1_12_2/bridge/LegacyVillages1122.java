package org.kylepitcock.legacyconnector.v1_12_2.bridge;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.registry.VillagerRegistry;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElementType;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import org.kylepitcock.legacyconnector.core.worldgen.LegacyVillageSource;
import org.kylepitcock.legacyconnector.core.worldgen.VillageAdditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 1.12.2's village buildings: the mods' {@code IVillageCreationHandler}s (Forge's VillagerRegistry) as a source for
 * {@link VillageAdditions}, which adds them as extra pieces of the vanilla village, each drawn by {@link LegacyVillagePoolElement1122}.
 * Handlers that build one of vanilla's own 1.12 buildings are left out (the modern village has houses of each kind).
 */
public final class LegacyVillages1122 implements LegacyVillageSource {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/Villages");
    private static final LegacyVillages1122 INSTANCE = new LegacyVillages1122();
    private static boolean registered;
    private static final Map<String, List<LegacyVillageSource.Building>> BUILDINGS = new java.util.concurrent.ConcurrentHashMap<>();

    private LegacyVillages1122() {}

    public static synchronized void register() {
        if (registered) return;
        registered = true;
        // the element type saves and loads the legacy pieces of a village with the world
        StructurePoolElementType<LegacyVillagePoolElement1122> type = () -> LegacyVillagePoolElement1122.CODEC;
        org.kylepitcock.legacyconnector.core.registry.LegacyRegistrar.register(net.minecraft.core.registries.BuiltInRegistries.STRUCTURE_POOL_ELEMENT,
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("legacyconnector", "legacy_village_building_1122"), type);
        LegacyVillagePoolElement1122.TYPE = type;
        VillageAdditions.register(INSTANCE);
    }

    static List<VillagerRegistry.IVillageCreationHandler> handlers() {
        return VillagerRegistry.instance().mcc$villageCreationHandlers();
    }

    private record Building(VillagerRegistry.IVillageCreationHandler handler, LegacyVillagePoolElement1122 element) implements LegacyVillageSource.Building {
        @Override
        public Weight weigh(Random rand) {
            var w = handler.getVillagePieceWeight(rand, 0);
            return w == null ? null : new Weight(w.villagePieceWeight, w.villagePiecesLimit);
        }

        @Override
        public BoundingBox footprint(int mode) {
            return element.footprint(mode);
        }

        @Override
        public StructurePoolElement inVillage(List<BoundingBox> streets, List<BoundingBox> others) {
            return element.inVillage(streets, others);
        }

        @Override
        public String toString() {
            return element.toString();
        }
    }

    @Override
    public boolean active() {
        return registered;
    }

    @Override
    public List<LegacyVillageSource.Building> buildings(String type) {
        return BUILDINGS.computeIfAbsent(type, t -> {
            List<LegacyVillageSource.Building> out = new ArrayList<>();
            for (var handler : handlers()) {
                try {
                    if (buildsVanillaPiece(handler)) continue;
                    var element = new LegacyVillagePoolElement1122(LegacyVillagePoolElement1122.key(handler), t, StructureTemplatePool.Projection.RIGID);
                    if (element.isBuilding()) out.add(new Building(handler, element));
                } catch (RuntimeException | LinkageError e) {
                    LOG.warn("1.12.2 village creation handler {} failed: {}", handler.getClass().getName(), e.toString());
                }
            }
            return List.copyOf(out);
        });
    }

    private static boolean buildsVanillaPiece(VillagerRegistry.IVillageCreationHandler handler) {
        try {
            Class<?> c = handler.getComponentClass();
            return c != null && c.getDeclaringClass() == legacyconnector.v1_12_2.net.minecraft.world.gen.structure.StructureVillagePieces.class;
        } catch (RuntimeException | LinkageError e) {
            return false;
        }
    }

    @Override
    public void describe() {
        Random rand = new Random(0);
        for (LegacyVillageSource.Building lb : buildings("plains")) {
            Building b = (Building) lb;
            String sample;
            try {
                var w = b.handler.getVillagePieceWeight(rand, 0);
                sample = w == null ? "none" : "weight " + w.villagePieceWeight + ", limit " + w.villagePiecesLimit;
            } catch (RuntimeException | LinkageError e) {
                sample = e.toString();
            }
            LOG.info("1.12.2 village building {}: footprint {}, a sample village gives it {} (vanilla buildings weigh 91; limit 0 leaves it out of the village)",
                    b.element, b.element.describeFootprint(), sample);
        }
    }
}
