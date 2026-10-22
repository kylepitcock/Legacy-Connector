package org.kylepitcock.mccompat;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.kylepitcock.mccompat.core.LegacyLifecycle;
import org.kylepitcock.mccompat.core.LegacyMods;
import org.kylepitcock.mccompat.core.data.CompatComponents;
import org.kylepitcock.mccompat.core.network.LegacyNetwork;
import org.kylepitcock.mccompat.core.pack.CoreData;
import org.kylepitcock.mccompat.core.pack.GeneratedPack;
import org.kylepitcock.mccompat.core.worldgen.LegacyWorldGenFeature;
import org.kylepitcock.mccompat.v1_7_10.Assets1710;
import org.kylepitcock.mccompat.v1_7_10.Data1710;
import org.kylepitcock.mccompat.v1_7_10.Platform1710;
import org.kylepitcock.mccompat.v1_7_10.bridge.CreativeTabBridge;
import org.kylepitcock.mccompat.v1_7_10.bridge.EventBridge1710;
import org.kylepitcock.mccompat.v1_7_10.bridge.FuelBridge;

/**
 * Runtime half of MC Compatibility. Legacy mods themselves are discovered, translated and handed to FML by the
 * loader service (see the {@code :loader} project); this mod provides the legacy APIs they now link against and
 * drives their lifecycle.
 */
@Mod(MCCompat.MOD_ID)
public final class MCCompat {
    public static final String MOD_ID = "retrofit";

    private static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, MOD_ID);
    public static final DeferredHolder<Feature<?>, LegacyWorldGenFeature> LEGACY_WORLDGEN =
            FEATURES.register("legacy_worldgen", () -> new LegacyWorldGenFeature(NoneFeatureConfiguration.CODEC));
    static {
        FEATURES.register("legacy_biome", () -> new org.kylepitcock.mccompat.core.worldgen.LegacyBiomeFeature(NoneFeatureConfiguration.CODEC));
    }

    private static final DeferredRegister<com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.chunk.ChunkGenerator>> CHUNK_GENERATORS =
            DeferredRegister.create(Registries.CHUNK_GENERATOR, MOD_ID);
    private static final DeferredRegister<com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.biome.BiomeSource>> BIOME_SOURCES =
            DeferredRegister.create(Registries.BIOME_SOURCE, MOD_ID);

    static {
        // Mod dimensions (see org.kylepitcock.mccompat.v1_7_10.dimension): ids "retrofit:legacy".
        CHUNK_GENERATORS.register("legacy", () -> org.kylepitcock.mccompat.v1_7_10.dimension.LegacyChunkGenerator.CODEC);
        BIOME_SOURCES.register("legacy", () -> org.kylepitcock.mccompat.v1_7_10.dimension.LegacyBiomeSource.CODEC);
    }

    private static final DeferredRegister<net.minecraft.world.inventory.MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, MOD_ID);
    public static final DeferredHolder<net.minecraft.world.inventory.MenuType<?>, net.minecraft.world.inventory.MenuType<org.kylepitcock.mccompat.v1_7_10.gui.LegacyMenu>> LEGACY_MENU_1_7_10 =
            MENUS.register("legacy_1_7_10", () -> net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create(
                    org.kylepitcock.mccompat.v1_7_10.bridge.GuiBridge::clientMenu));

    public MCCompat(IEventBus modBus, ModContainer container) {
        LegacyMods.registerPlatform(new Platform1710());
        LegacyMods.registerPlatform(new org.kylepitcock.mccompat.v1_12_2.Platform1122());

        CompatComponents.REGISTER.register(modBus);
        FEATURES.register(modBus);
        CHUNK_GENERATORS.register(modBus);
        BIOME_SOURCES.register(modBus);
        modBus.addListener(org.kylepitcock.mccompat.v1_7_10.entity.LegacyHosts::onAttributeCreation);
        MENUS.register(modBus);
        modBus.addListener((net.neoforged.neoforge.registries.RegisterEvent e) -> {
            if (e.getRegistryKey().equals(Registries.MENU)) org.kylepitcock.mccompat.v1_7_10.bridge.GuiBridge.setMenuType(LEGACY_MENU_1_7_10.get());
        });
        modBus.addListener(MCCompat::registerCapabilities);
        if (FMLEnvironment.dist.isClient()) org.kylepitcock.mccompat.v1_7_10.client.Client1710.register(modBus);
        // LOWEST: legacy loading runs after every modern mod registered its own entries for that registry.
        modBus.addListener(EventPriority.LOWEST, LegacyLifecycle::onRegister);
        modBus.addListener(LegacyLifecycle::onCommonSetup);
        modBus.addListener(LegacyLifecycle::onLoadComplete);
        modBus.addListener(GeneratedPack::onAddPackFinders);
        modBus.addListener(LegacyNetwork::onRegisterPayloads);
        modBus.addListener(CreativeTabBridge::onBuildContents);
        modBus.addListener(org.kylepitcock.mccompat.v1_7_10.entity.LegacySpawnEggs::addToTab);

        GeneratedPack.addContributor(new CoreData());
        GeneratedPack.addContributor(new Data1710());
        GeneratedPack.addContributor(new Assets1710());

        IEventBus game = NeoForge.EVENT_BUS;
        EventBridge1710.register(game);
        game.addListener(FuelBridge::onBurnTime);
        if (FMLEnvironment.dist.isClient()) EventBridge1710.registerClient(game);
    }

    /** Hoppers, comparators-by-capability and modern item pipes can use legacy tile entity inventories. */
    private static void registerCapabilities(net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent event) {
        org.kylepitcock.mccompat.v1_7_10.bridge.EnergyBridge.registerCapabilities(event);
        org.kylepitcock.mccompat.v1_7_10.bridge.FluidBridge.registerCapabilities(event);
        var type = org.kylepitcock.mccompat.v1_7_10.bridge.TileEntityBridge.type();
        if (type == null) return;
        event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, type, (be, side) ->
                be.legacy() instanceof mccompat.v1_7_10.net.minecraft.inventory.IInventory inv
                        ? new net.neoforged.neoforge.items.wrapper.InvWrapper(org.kylepitcock.mccompat.v1_7_10.gui.LegacyInventoryContainer.of(inv))
                        : null);
    }
}
