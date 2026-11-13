package org.kylepitcock.legacyconnector;

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
import org.kylepitcock.legacyconnector.core.LegacyLifecycle;
import org.kylepitcock.legacyconnector.core.LegacyMods;
import org.kylepitcock.legacyconnector.core.data.CompatComponents;
import org.kylepitcock.legacyconnector.core.network.LegacyNetwork;
import org.kylepitcock.legacyconnector.core.pack.CoreData;
import org.kylepitcock.legacyconnector.core.pack.GeneratedPack;
import org.kylepitcock.legacyconnector.core.worldgen.LegacyWorldGenFeature;
import org.kylepitcock.legacyconnector.v1_7_10.Assets1710;
import org.kylepitcock.legacyconnector.v1_7_10.Data1710;
import org.kylepitcock.legacyconnector.v1_7_10.Platform1710;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.CreativeTabBridge;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.EventBridge1710;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.FuelBridge;

/**
 * Runtime half of MC Compatibility. Legacy mods themselves are discovered, translated and handed to FML by the
 * loader service (see the {@code :loader} project); this mod provides the legacy APIs they now link against and
 * drives their lifecycle.
 */
@Mod(LegacyConnector.MOD_ID)
public final class LegacyConnector {
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("LegacyConnector/Test");
    public static final String MOD_ID = "legacyconnector";

    private static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, MOD_ID);
    private static final DeferredRegister<net.minecraft.world.entity.EntityType<?>> BETA_ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, MOD_ID);
    public static final DeferredHolder<net.minecraft.world.entity.EntityType<?>, net.minecraft.world.entity.EntityType<org.kylepitcock.legacyconnector.vb1_7_3.LegacyB173VanillaArrow>> BETA_VANILLA_ARROW =
            BETA_ENTITIES.register("vanilla_arrow_b1_7_3", () -> net.minecraft.world.entity.EntityType.Builder
                    .<org.kylepitcock.legacyconnector.vb1_7_3.LegacyB173VanillaArrow>of(org.kylepitcock.legacyconnector.vb1_7_3.LegacyB173VanillaArrow::new, net.minecraft.world.entity.MobCategory.MISC)
                    .sized(0.5F,0.5F).clientTrackingRange(8).updateInterval(1).build("legacyconnector:vanilla_arrow_b1_7_3"));
    public static final DeferredHolder<net.minecraft.world.entity.EntityType<?>, net.minecraft.world.entity.EntityType<org.kylepitcock.legacyconnector.vb1_7_3.LegacyB173Arrow>> BETA_ARROW =
            BETA_ENTITIES.register("arrow_b1_7_3", () -> net.minecraft.world.entity.EntityType.Builder
                    .<org.kylepitcock.legacyconnector.vb1_7_3.LegacyB173Arrow>of(org.kylepitcock.legacyconnector.vb1_7_3.LegacyB173Arrow::new, net.minecraft.world.entity.MobCategory.MISC)
                    .sized(0.5F,0.5F).clientTrackingRange(8).updateInterval(1).build("legacyconnector:arrow_b1_7_3"));
    public static final DeferredHolder<net.minecraft.world.entity.EntityType<?>, net.minecraft.world.entity.EntityType<org.kylepitcock.legacyconnector.vb1_7_3.LegacyB173Arrow>> BETA_THROWN =
            BETA_ENTITIES.register("thrown_b1_7_3", () -> net.minecraft.world.entity.EntityType.Builder
                    .<org.kylepitcock.legacyconnector.vb1_7_3.LegacyB173Arrow>of(org.kylepitcock.legacyconnector.vb1_7_3.LegacyB173Arrow::new, net.minecraft.world.entity.MobCategory.MISC)
                    .sized(0.25F,0.25F).clientTrackingRange(8).updateInterval(1).build("legacyconnector:thrown_b1_7_3"));
    public static final DeferredHolder<net.minecraft.world.entity.EntityType<?>, net.minecraft.world.entity.EntityType<org.kylepitcock.legacyconnector.vb1_7_3.LegacyB173Entity>> BETA_ENTITY =
            BETA_ENTITIES.register("entity_b1_7_3", () -> net.minecraft.world.entity.EntityType.Builder
                    .<org.kylepitcock.legacyconnector.vb1_7_3.LegacyB173Entity>of(org.kylepitcock.legacyconnector.vb1_7_3.LegacyB173Entity::new, net.minecraft.world.entity.MobCategory.MISC)
                    .sized(.25F,.25F).clientTrackingRange(8).updateInterval(1).build("legacyconnector:entity_b1_7_3"));
    public static final DeferredHolder<net.minecraft.world.entity.EntityType<?>, net.minecraft.world.entity.EntityType<org.kylepitcock.legacyconnector.vb1_7_3.LegacyB173Lightning>> BETA_LIGHTNING =
            BETA_ENTITIES.register("lightning_b1_7_3", () -> net.minecraft.world.entity.EntityType.Builder
                    .<org.kylepitcock.legacyconnector.vb1_7_3.LegacyB173Lightning>of(org.kylepitcock.legacyconnector.vb1_7_3.LegacyB173Lightning::new, net.minecraft.world.entity.MobCategory.MISC)
                    .sized(0F,0F).clientTrackingRange(64).updateInterval(1).build("legacyconnector:lightning_b1_7_3"));
    public static final DeferredHolder<net.minecraft.world.entity.EntityType<?>, net.minecraft.world.entity.EntityType<org.kylepitcock.legacyconnector.vb1_7_3.LegacyB173ItemEntity>> BETA_ITEM_ENTITY =
            BETA_ENTITIES.register("item_entity_b1_7_3", () -> net.minecraft.world.entity.EntityType.Builder
                    .<org.kylepitcock.legacyconnector.vb1_7_3.LegacyB173ItemEntity>of(org.kylepitcock.legacyconnector.vb1_7_3.LegacyB173ItemEntity::new, net.minecraft.world.entity.MobCategory.MISC)
                    .sized(.25F,.25F).clientTrackingRange(8).updateInterval(1).build("legacyconnector:item_entity_b1_7_3"));
    public static final DeferredHolder<Feature<?>, LegacyWorldGenFeature> LEGACY_WORLDGEN =
            FEATURES.register("legacy_worldgen", () -> new LegacyWorldGenFeature(NoneFeatureConfiguration.CODEC));
    static {
        FEATURES.register("legacy_biome", () -> new org.kylepitcock.legacyconnector.core.worldgen.LegacyBiomeFeature(NoneFeatureConfiguration.CODEC));
        // one per legacy world generator (configured features generated by core.worldgen.LegacyGenerators)
        FEATURES.register("legacy_generator", org.kylepitcock.legacyconnector.core.worldgen.LegacyGeneratorFeature::new);
        // 1.7.10 deep layer: one placed feature per legacy IWorldGenerator (DeepLayer1710)
        FEATURES.register("legacy_worldgen_deep", org.kylepitcock.legacyconnector.v1_7_10.bridge.LegacyDeepWorldGenFeature::new);
    }

    private static final DeferredRegister<com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.chunk.ChunkGenerator>> CHUNK_GENERATORS =
            DeferredRegister.create(Registries.CHUNK_GENERATOR, MOD_ID);
    private static final DeferredRegister<com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.biome.BiomeSource>> BIOME_SOURCES =
            DeferredRegister.create(Registries.BIOME_SOURCE, MOD_ID);

    static {
        // Mod dimensions (see org.kylepitcock.legacyconnector.v1_7_10.dimension): ids "legacyconnector:legacy".
        CHUNK_GENERATORS.register("legacy", () -> org.kylepitcock.legacyconnector.v1_7_10.dimension.LegacyChunkGenerator.CODEC);
        CHUNK_GENERATORS.register("legacy_1122", () -> org.kylepitcock.legacyconnector.v1_12_2.dimension.LegacyChunkGenerator1122.CODEC);
        CHUNK_GENERATORS.register("legacy_b173", () -> org.kylepitcock.legacyconnector.vb1_7_3.dimension.ChunkGeneratorB173.CODEC);
        BIOME_SOURCES.register("legacy", () -> org.kylepitcock.legacyconnector.v1_7_10.dimension.LegacyBiomeSource.CODEC);
        BIOME_SOURCES.register("legacy_1122", () -> org.kylepitcock.legacyconnector.v1_12_2.dimension.LegacyBiomeSource1122.CODEC);
        BIOME_SOURCES.register("legacy_b173", () -> org.kylepitcock.legacyconnector.vb1_7_3.dimension.BiomeSourceB173.CODEC);
        // World presets of legacy mod world types: the overworld multi-noise source with the world type's legacy biome layers.
        BIOME_SOURCES.register("legacy_layers", () -> org.kylepitcock.legacyconnector.v1_7_10.bridge.LegacyLayeredBiomeSource.CODEC);
    }

    private static final DeferredRegister<net.minecraft.world.level.levelgen.structure.StructureType<?>> STRUCTURE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_TYPE, MOD_ID);
    private static final DeferredRegister<net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType> STRUCTURE_PIECES =
            DeferredRegister.create(Registries.STRUCTURE_PIECE, MOD_ID);

    static {
        // What legacy world generators build in place, recorded as locatable structures (core.worldgen.LegacyGeneratorStructures).
        // Registered always, so recorded starts load even with structures.locatable=false.
        STRUCTURE_TYPES.register("legacy_generator_structure", () -> {
            net.minecraft.world.level.levelgen.structure.StructureType<org.kylepitcock.legacyconnector.core.worldgen.LegacyGeneratorStructure> t =
                    () -> org.kylepitcock.legacyconnector.core.worldgen.LegacyGeneratorStructure.CODEC;
            org.kylepitcock.legacyconnector.core.worldgen.LegacyGeneratorStructure.TYPE = t;
            return t;
        });
        STRUCTURE_PIECES.register("legacy_marker", () -> {
            net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType.ContextlessType t =
                    org.kylepitcock.legacyconnector.core.worldgen.LegacyMarkerPiece::new;
            org.kylepitcock.legacyconnector.core.worldgen.LegacyMarkerPiece.TYPE = t;
            return t;
        });
    }

    private static final DeferredRegister<net.minecraft.advancements.CriterionTrigger<?>> TRIGGERS = DeferredRegister.create(Registries.TRIGGER_TYPE, MOD_ID);

    static {
        // The criteria of 1.12.2 advancements that use a trigger a mod registered itself (v1_12_2.advancement.LegacyTrigger1122).
        TRIGGERS.register("legacy", () -> org.kylepitcock.legacyconnector.v1_12_2.advancement.LegacyTrigger1122.INSTANCE);
    }

    private static final DeferredRegister<net.minecraft.world.inventory.MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, MOD_ID);
    public static final DeferredHolder<net.minecraft.world.inventory.MenuType<?>, net.minecraft.world.inventory.MenuType<org.kylepitcock.legacyconnector.v1_7_10.gui.LegacyMenu>> LEGACY_MENU_1_7_10 =
            MENUS.register("legacy_1_7_10", () -> net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create(
                    org.kylepitcock.legacyconnector.v1_7_10.bridge.GuiBridge::clientMenu));
    public static final DeferredHolder<net.minecraft.world.inventory.MenuType<?>, net.minecraft.world.inventory.MenuType<org.kylepitcock.legacyconnector.v1_12_2.gui.LegacyMenu1122>> LEGACY_MENU_1_12_2 =
            MENUS.register("legacy_1_12_2", () -> net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create(
                    org.kylepitcock.legacyconnector.v1_12_2.bridge.GuiBridge1122::clientMenu));
    public static final DeferredHolder<net.minecraft.world.inventory.MenuType<?>, net.minecraft.world.inventory.MenuType<org.kylepitcock.legacyconnector.vb1_7_3.LegacyB173Menu>> LEGACY_MENU_B1_7_3 =
            MENUS.register("legacy_b1_7_3", () -> net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create(
                    org.kylepitcock.legacyconnector.vb1_7_3.LegacyB173Menu::client));

    public LegacyConnector(IEventBus modBus, ModContainer container) {
        BETA_ENTITIES.register(modBus);
        if (!System.getProperty("legacyconnector.forge1201.mods", "").isEmpty()) org.kylepitcock.legacyconnector.v1_20_1.Forge1201Runtime.init(modBus);
        org.slf4j.LoggerFactory.getLogger("LegacyConnector").info("Legacy Connector {} (runtime) loaded",
                container.getModInfo().getVersion());
        org.kylepitcock.legacyconnector.core.DirectMemoryGuard.start();
        org.kylepitcock.legacyconnector.core.LegacyLogFilter.install();
        org.kylepitcock.legacyconnector.core.EntityCensus.register();
        org.kylepitcock.legacyconnector.core.LegacyCrashInfo.register();
        org.kylepitcock.legacyconnector.core.SilentFailureChecks.register();
        LegacyMods.registerPlatform(new Platform1710());
        LegacyMods.registerPlatform(new org.kylepitcock.legacyconnector.v1_12_2.Platform1122());
        LegacyMods.registerPlatform(new org.kylepitcock.legacyconnector.vb1_7_3.PlatformB173());
        org.kylepitcock.legacyconnector.vb1_7_3.GameHookNetworkB173.install();
        org.kylepitcock.legacyconnector.vb1_7_3.PlayerApiLifecycleB173.install(modBus);
        org.kylepitcock.legacyconnector.vb1_7_3.PlayerInventoryNetworkB173.install();

        CompatComponents.REGISTER.register(modBus);
        org.kylepitcock.legacyconnector.vb1_7_3.SpawnOverrideModifierB173.REGISTER.register(modBus);
        org.kylepitcock.legacyconnector.vb1_7_3.BetaMetadataIngredient.REGISTER.register(modBus);
        org.kylepitcock.legacyconnector.core.recipe.OversizedCraftingRecipe.REGISTER.register(modBus);
        org.kylepitcock.legacyconnector.v1_7_10.adapter.LegacyCraftingRecipe.REGISTER.register(modBus);
        org.kylepitcock.legacyconnector.v1_7_10.bridge.LegacyFishingLoot.REGISTER.register(modBus);
        FEATURES.register(modBus);
        CHUNK_GENERATORS.register(modBus);
        BIOME_SOURCES.register(modBus);
        STRUCTURE_TYPES.register(modBus);
        STRUCTURE_PIECES.register(modBus);
        TRIGGERS.register(modBus);
        modBus.addListener(org.kylepitcock.legacyconnector.core.worldgen.BiomePlacement::onCommonSetup);
        modBus.addListener(org.kylepitcock.legacyconnector.v1_7_10.entity.LegacyHosts::onAttributeCreation);
        modBus.addListener(org.kylepitcock.legacyconnector.v1_12_2.entity.Hosts1122::onAttributeCreation);
        modBus.addListener(org.kylepitcock.legacyconnector.v1_7_10.entity.LegacyHosts::onSpawnPlacements);
        modBus.addListener(org.kylepitcock.legacyconnector.v1_12_2.entity.Hosts1122::onSpawnPlacements);
        MENUS.register(modBus);
        modBus.addListener((net.neoforged.neoforge.registries.RegisterEvent e) -> {
            if (e.getRegistryKey().equals(Registries.MENU)) {
                org.kylepitcock.legacyconnector.v1_7_10.bridge.GuiBridge.setMenuType(LEGACY_MENU_1_7_10.get());
                org.kylepitcock.legacyconnector.v1_12_2.bridge.GuiBridge1122.setMenuType(LEGACY_MENU_1_12_2.get());
                org.kylepitcock.legacyconnector.vb1_7_3.MenusB173.setMenuType(LEGACY_MENU_B1_7_3.get());
            }
        });
        modBus.addListener(LegacyConnector::registerCapabilities);
        // Optional: Create's contraption movement checks follow the legacy piston rules (also used by Create Aeronautics).
        if (net.neoforged.fml.ModList.get().isLoaded("create")) org.kylepitcock.legacyconnector.compat.create.CreateCompat.init();
        if (FMLEnvironment.dist.isClient()) org.kylepitcock.legacyconnector.v1_7_10.client.Client1710.register(modBus);
        if (FMLEnvironment.dist.isClient()) org.kylepitcock.legacyconnector.v1_12_2.client.Client1122.register(modBus);
        if (FMLEnvironment.dist.isClient()) org.kylepitcock.legacyconnector.vb1_7_3.client.ClientB173.register(modBus);
        if (FMLEnvironment.dist.isClient()) org.kylepitcock.legacyconnector.client.LegacyConnectorClient.register(container);
        if (FMLEnvironment.dist.isClient()) org.kylepitcock.legacyconnector.core.pack.ModernizedTextures.register();
        // LOWEST: legacy loading runs after every modern mod registered its own entries for that registry.
        modBus.addListener(EventPriority.LOWEST, LegacyLifecycle::onRegister);
        modBus.addListener(LegacyLifecycle::onCommonSetup);
        modBus.addListener(LegacyLifecycle::onLoadComplete);
        modBus.addListener(GeneratedPack::onAddPackFinders);
        modBus.addListener(org.kylepitcock.legacyconnector.core.pack.GameResourcesFolderPack::onAddPackFinders);
        modBus.addListener(org.kylepitcock.legacyconnector.v1_7_10.LegacyModPacks::onAddPackFinders);
        modBus.addListener(LegacyNetwork::onRegisterPayloads);
        org.kylepitcock.legacyconnector.vb1_7_3.PlayerMotionB173.init();
        org.kylepitcock.legacyconnector.vb1_7_3.AudioB173.init();
        org.kylepitcock.legacyconnector.vb1_7_3.DimensionsB173.install();
        org.kylepitcock.legacyconnector.vb1_7_3.BetaDeferredGenData.install();
        org.kylepitcock.legacyconnector.vb1_7_3.KeyBindingsB173.install();
        org.kylepitcock.legacyconnector.vb1_7_3.ReachB173.install();
        org.kylepitcock.legacyconnector.vb1_7_3.CombatTargetsB173.install();
        modBus.addListener(CreativeTabBridge::onBuildContents);
        modBus.addListener(org.kylepitcock.legacyconnector.v1_12_2.bridge.CreativeTabBridge1122::onBuildContents);
        modBus.addListener(org.kylepitcock.legacyconnector.v1_7_10.entity.LegacySpawnEggs::addToTab);

        GeneratedPack.addContributor(new CoreData());
        GeneratedPack.addContributor(new org.kylepitcock.legacyconnector.vb1_7_3.AudioB173());
        GeneratedPack.addContributor(new Data1710());
        GeneratedPack.addContributor(new org.kylepitcock.legacyconnector.v1_12_2.Data1122());
        GeneratedPack.addContributor(new org.kylepitcock.legacyconnector.v1_12_2.Assets1122());
        GeneratedPack.addContributor(new Assets1710());
        GeneratedPack.addContributor(new org.kylepitcock.legacyconnector.vb1_7_3.AssetsB173());
        GeneratedPack.addContributor(new org.kylepitcock.legacyconnector.vb1_7_3.MobTexturesB173());
        GeneratedPack.addContributor((type,out)->{if(type==net.minecraft.server.packs.PackType.SERVER_DATA){org.kylepitcock.legacyconnector.vb1_7_3.SpawnRulesB173.contribute(out);org.kylepitcock.legacyconnector.vb1_7_3.AdvancementsB173.contribute(out);org.kylepitcock.legacyconnector.vb1_7_3.RecordsB173.contribute(out);org.kylepitcock.legacyconnector.vb1_7_3.DimensionsB173.contribute(out);}});
        GeneratedPack.addRecipeContributor((type, out) -> {
            if (type == net.minecraft.server.packs.PackType.SERVER_DATA) org.kylepitcock.legacyconnector.v1_7_10.bridge.RecipeBridge.generate(out);
        });
        GeneratedPack.addRecipeContributor(org.kylepitcock.legacyconnector.v1_7_10.bridge.Achievements1710::contribute);
        org.kylepitcock.legacyconnector.v1_7_10.bridge.Achievements1710.register();
        org.kylepitcock.legacyconnector.core.worldgen.LegacyStructureIndex.register();
        GeneratedPack.addRecipeContributor((type, out) -> {
            if (type == net.minecraft.server.packs.PackType.SERVER_DATA) org.kylepitcock.legacyconnector.v1_12_2.bridge.RecipeBridge1122.generate(out);
        });
        org.kylepitcock.legacyconnector.core.pack.LegacyRecipeSync.watch(org.kylepitcock.legacyconnector.v1_7_10.bridge.RecipeBridge::changedSinceGenerate);
        org.kylepitcock.legacyconnector.core.pack.LegacyRecipeSync.watch(org.kylepitcock.legacyconnector.v1_12_2.bridge.RecipeBridge1122::changedSinceGenerate);
        GeneratedPack.addRecipeContributor((type, out) -> {
            if (type == net.minecraft.server.packs.PackType.SERVER_DATA) org.kylepitcock.legacyconnector.vb1_7_3.RecipesB173.generate(out);
        });

        IEventBus game = NeoForge.EVENT_BUS;
        org.kylepitcock.legacyconnector.core.LevelReleases.register(game);
        org.kylepitcock.legacyconnector.core.pack.LegacyRecipeSync.register(game);
        EventBridge1710.register(game);
        org.kylepitcock.legacyconnector.v1_7_10.bridge.ForgeEvents1710.register(game);
        org.kylepitcock.legacyconnector.v1_7_10.bridge.DimensionPhysics1710.register(game);
        org.kylepitcock.legacyconnector.v1_7_10.compat.chromaticraft.ChromaDimensionBarriers.register(game);
        org.kylepitcock.legacyconnector.v1_12_2.entity.Entities1122.register(game);
        org.kylepitcock.legacyconnector.vb1_7_3.SelfTestB173.register(game);
        org.kylepitcock.legacyconnector.vb1_7_3.MobsB173.registerEvents(modBus,game);
        org.kylepitcock.legacyconnector.vb1_7_3.AnimalsB173.registerEvents(modBus);
        org.kylepitcock.legacyconnector.vb1_7_3.SpawnSchedulerB173.register(game);
        org.kylepitcock.legacyconnector.vb1_7_3.BlockHeightmapsB173.register(game);
        game.addListener((net.neoforged.neoforge.event.server.ServerStoppedEvent e) -> {
            if (!e.getServer().isDedicatedServer()) return;
            // Legacy mods start non-daemon thread pools (IC2 and others) they never stop, which keep a stopped dedicated server's JVM alive.
            Thread exit = new Thread(() -> {
                try {
                    Thread.sleep(10_000);
                } catch (InterruptedException ex) {
                    return;
                }
                StringBuilder names = new StringBuilder();
                for (Thread t : Thread.getAllStackTraces().keySet()) {
                    if (t.isAlive() && !t.isDaemon() && t != Thread.currentThread() && !t.getName().equals("main")) names.append(' ').append(t.getName());
                }
                LOGGER.info("The server has stopped and saved; exiting now. Threads that would have kept the JVM alive:{}", names);
                System.exit(0);
            }, "legacyconnector-exit");
            exit.setDaemon(true);
            exit.start();
        });
        org.kylepitcock.legacyconnector.core.ConfigPacketSizes.register(game);
        org.kylepitcock.legacyconnector.core.ServerScript.register();
        org.kylepitcock.legacyconnector.core.ServerTickScript.register(game);
        org.kylepitcock.legacyconnector.core.ServerDevScript.register();
        int stopTicks = Integer.getInteger("legacyconnector.autostopTicks", 0);
        if (stopTicks > 0) {
            int[] ticks = {0};
            game.addListener((net.neoforged.neoforge.event.tick.ServerTickEvent.Post e) -> {
                if (++ticks[0] != stopTicks) return;
                org.kylepitcock.legacyconnector.v1_6_4.bridge.SelfTest164.report();
                LOGGER.info("Test autostop after {} ticks: saving and stopping", stopTicks);
                long chunks = 0;
                for (var level : e.getServer().getAllLevels()) chunks += level.getChunkSource().getLoadedChunksCount();
                Runtime rt = Runtime.getRuntime();
                LOGGER.info("Before the stop: {} chunks loaded, heap {} MB used of {} MB", chunks, (rt.totalMemory() - rt.freeMemory()) >> 20, rt.maxMemory() >> 20);
                long start = System.nanoTime();
                Thread watch = new Thread(() -> {
                    try {
                        Thread.sleep(120_000);
                    } catch (InterruptedException ex) {
                        return;
                    }
                    LOGGER.error("Server still saving 120 s after the stop request");
                    Thread.getAllStackTraces().forEach((t, st) -> {
                        StringBuilder sb = new StringBuilder(t.getName());
                        for (StackTraceElement f : st) sb.append("\n\tat ").append(f);
                        LOGGER.error("{}", sb);
                    });
                }, "autostop-watchdog");
                watch.setDaemon(true);
                watch.start();
                e.getServer().halt(false);
                LOGGER.info("Stop requested in {} ms", (System.nanoTime() - start) / 1_000_000);
            });
        }
        org.kylepitcock.legacyconnector.v1_7_10.bridge.ChestLootBridge1710.register(game);
        game.addListener(FuelBridge::onBurnTime);
        if (FMLEnvironment.dist.isClient()) EventBridge1710.registerClient(game);
    }

    /** Hoppers, comparators-by-capability and modern item pipes can use legacy tile entity inventories. */
    private static void registerCapabilities(net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent event) {
        // public addon API: addons' rules are asked first, then the 1.12.2 built-ins (see api/LegacyCapabilities)
        org.kylepitcock.legacyconnector.api.internal.CapabilityRules.apply(event);
        org.kylepitcock.legacyconnector.v1_7_10.bridge.EnergyBridge.registerCapabilities(event);
        org.kylepitcock.legacyconnector.v1_7_10.bridge.ForeignEnergy1710.registerItems(event);
        org.kylepitcock.legacyconnector.v1_7_10.bridge.FluidBridge.registerCapabilities(event);
        org.kylepitcock.legacyconnector.v1_12_2.bridge.ItemCapabilities1122.register(event);
        // after the legacy item capabilities: a mod's own bucket with a legacy fluid capability answers through it
        org.kylepitcock.legacyconnector.v1_12_2.bridge.FluidBridge1122.registerCapabilities(event);
        org.kylepitcock.legacyconnector.v1_12_2.bridge.EnergyBridge1122.registerItems(event);
        var type = org.kylepitcock.legacyconnector.v1_7_10.bridge.TileEntityBridge.type();
        if (type == null) return;
        event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, type, (be, side) ->
                org.kylepitcock.legacyconnector.api.internal.BuiltinAdapters.inventory1710(be.legacy(), side, be));
    }
}
