package org.kylepitcock.legacyconnector.v1_7_10.bridge;

/**
 * Legacy village components (Forge village creation handlers: Thaumcraft's wizard tower, Witchery's shops, AgriCraft's greenhouse) in
 * the modern villages. Vanilla's village pools are left exactly as they are; the buildings are added to each assembled village as
 * extra pieces beside its streets ({@link LegacyVillageAdditions}), each drawn by {@link LegacyVillagePoolElement}.
 */
public final class LegacyVillages {
    private static boolean registered;

    private LegacyVillages() {}

    public static synchronized void register() {
        if (registered) return;
        registered = true;
        // the element type saves and loads the legacy pieces of a village with the world
        net.minecraft.world.level.levelgen.structure.pools.StructurePoolElementType<LegacyVillagePoolElement> type = () -> LegacyVillagePoolElement.CODEC;
        org.kylepitcock.legacyconnector.core.registry.LegacyRegistrar.register(net.minecraft.core.registries.BuiltInRegistries.STRUCTURE_POOL_ELEMENT,
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("legacyconnector", "legacy_village_building"), type);
        LegacyVillagePoolElement.TYPE = type;
        org.kylepitcock.legacyconnector.core.worldgen.VillageAdditions.register(LegacyVillageAdditions.INSTANCE);
    }
}
