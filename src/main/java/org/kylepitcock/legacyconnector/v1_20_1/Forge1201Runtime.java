package org.kylepitcock.legacyconnector.v1_20_1;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import legacyconnector.v1_20_1.Caps1201;
import legacyconnector.v1_20_1.net.minecraftforge.common.capabilities.Capability;
import legacyconnector.v1_20_1.net.minecraftforge.common.capabilities.ForgeCapabilities;
import legacyconnector.v1_20_1.net.minecraftforge.common.capabilities.ICapabilityProvider;
import legacyconnector.v1_20_1.net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import legacyconnector.v1_20_1.net.minecraftforge.event.TickEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.LogicalSide;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.EntityCapability;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runtime half of the Forge 1.20.1 facades ({@code legacyconnector/v1_20_1}): turns NeoForge 1.21.1's events into the old events the translated mods listen
 * to (tick events, the capability registration event) and exposes the capabilities of legacy block entities and entities to NeoForge. Created by the
 * runtime mod when the loader translated at least one Forge 1.20.1 jar ({@code legacyconnector.forge1201.mods}).
 */
public final class Forge1201Runtime {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/Forge1201");
    private static Set<String> modIds = Set.of();

    private Forge1201Runtime() {
    }

    public static void init(IEventBus modBus) {
        modIds = new HashSet<>(Arrays.asList(System.getProperty("legacyconnector.forge1201.mods", "").split(",")));
        modIds.remove("");
        LOG.info("Forge 1.20.1 facades active for {} mod(s): {}", modIds.size(), modIds);
        modBus.addListener(Forge1201Runtime::onRegisterCapabilities);
        modBus.addListener(Forge1201Runtime::onLoadComplete);
        modBus.addListener(org.kylepitcock.legacyconnector.v1_20_1.migrate.LegacyEnchantments::onNewRegistry);
        org.kylepitcock.legacyconnector.v1_20_1.migrate.LegacyComponents.COMPONENTS.register(modBus);
        modBus.addListener((net.neoforged.neoforge.registries.RegisterEvent e) -> {
            if (e.getRegistryKey().equals(net.neoforged.neoforge.registries.NeoForgeRegistries.Keys.CONDITION_CODECS)) {
                e.register(net.neoforged.neoforge.registries.NeoForgeRegistries.Keys.CONDITION_CODECS,
                        helper -> legacyconnector.v1_20_1.net.minecraftforge.common.crafting.CraftingHelper.flushConditions(helper::register));
            }
            if (e.getRegistryKey().equals(net.neoforged.neoforge.registries.NeoForgeRegistries.Keys.INGREDIENT_TYPES)) {
                e.register(net.neoforged.neoforge.registries.NeoForgeRegistries.Keys.INGREDIENT_TYPES,
                        helper -> legacyconnector.v1_20_1.net.minecraftforge.common.crafting.CraftingHelper.flushTypes(helper::register));
            }
        });
        modBus.addListener((net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent e) ->
                legacyconnector.v1_20_1.net.minecraftforge.common.world.ForgeChunkManager.registerControllers(e, modIds));
        org.kylepitcock.legacyconnector.core.pack.GeneratedPack.addContributor(org.kylepitcock.legacyconnector.v1_20_1.migrate.LegacyEnchantments::contribute);
        modBus.addListener(legacyconnector.v1_20_1.net.minecraftforge.network.simple.SimpleChannel::registerAll);
        modBus.addListener(legacyconnector.v1_20_1.net.minecraftforge.network.event.EventNetworkChannel::registerAll);
        // loot tables are parsed before the server exists: the handlers of legacy mods read enchantments through the registries of this reload
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.HIGHEST, (net.neoforged.neoforge.event.LootTableLoadEvent e) ->
                org.kylepitcock.legacyconnector.v1_20_1.migrate.MigrateContext.remember(e.getRegistries()));
        NeoForge.EVENT_BUS.addListener(Forge1201Runtime::onServerTickPre);
        NeoForge.EVENT_BUS.addListener(Forge1201Runtime::onServerTickPost);
        NeoForge.EVENT_BUS.addListener(Forge1201Runtime::onLevelTickPre);
        NeoForge.EVENT_BUS.addListener(Forge1201Runtime::onLevelTickPost);
        NeoForge.EVENT_BUS.addListener(Forge1201Runtime::onPlayerTickPre);
        NeoForge.EVENT_BUS.addListener(Forge1201Runtime::onPlayerTickPost);
        NeoForge.EVENT_BUS.addListener(Forge1201Runtime::onEntityJoin);
        Forge1201Events.init();
        if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) Forge1201Client.init(modBus);
    }

    /** The "1.20.1 content registered" line of the server self test (mirrors the 1.12.2 one). */
    private static void onLoadComplete(net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent event) {
        StringBuilder sb = new StringBuilder();
        int blocks = 0;
        int items = 0;
        int entities = 0;
        int blockEntities = 0;
        for (String id : new java.util.TreeSet<>(modIds)) {
            int b = count(BuiltInRegistries.BLOCK, id);
            int i = count(BuiltInRegistries.ITEM, id);
            int e = count(BuiltInRegistries.ENTITY_TYPE, id);
            int t = count(BuiltInRegistries.BLOCK_ENTITY_TYPE, id);
            blocks += b;
            items += i;
            entities += e;
            blockEntities += t;
            sb.append("\n  ").append(id).append(": ").append(b).append(" blocks, ").append(i).append(" items, ").append(e).append(" entities, ").append(t).append(" block entity types");
        }
        LOG.info("1.20.1 content registered: {} blocks, {} items, {} entity types, {} block entity types in {} mod(s){}", blocks, items, entities, blockEntities, modIds.size(), sb);
    }

    private static int count(net.minecraft.core.Registry<?> registry, String namespace) {
        int n = 0;
        for (var key : registry.keySet()) if (key.getNamespace().equals(namespace)) n++;
        return n;
    }

    public static Set<String> modIds() {
        return modIds;
    }

    // ---- tick events ------------------------------------------------------------------------------------------------------

    private static void onServerTickPre(ServerTickEvent.Pre e) {
        NeoForge.EVENT_BUS.post(new TickEvent.ServerTickEvent(TickEvent.Phase.START, e::hasTime));
    }

    private static void onServerTickPost(ServerTickEvent.Post e) {
        NeoForge.EVENT_BUS.post(new TickEvent.ServerTickEvent(TickEvent.Phase.END, e::hasTime));
    }

    private static void onLevelTickPre(LevelTickEvent.Pre e) {
        NeoForge.EVENT_BUS.post(new TickEvent.LevelTickEvent(side(e.getLevel().isClientSide), TickEvent.Phase.START, e.getLevel(), e::hasTime));
    }

    private static void onLevelTickPost(LevelTickEvent.Post e) {
        NeoForge.EVENT_BUS.post(new TickEvent.LevelTickEvent(side(e.getLevel().isClientSide), TickEvent.Phase.END, e.getLevel(), e::hasTime));
    }

    private static void onPlayerTickPre(PlayerTickEvent.Pre e) {
        NeoForge.EVENT_BUS.post(new TickEvent.PlayerTickEvent(TickEvent.Phase.START, e.getEntity()));
    }

    private static void onPlayerTickPost(PlayerTickEvent.Post e) {
        NeoForge.EVENT_BUS.post(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, e.getEntity()));
    }

    static LogicalSide side(boolean clientSide) {
        return clientSide ? LogicalSide.CLIENT : LogicalSide.SERVER;
    }

    // ---- capabilities -----------------------------------------------------------------------------------------------------

    private static void onEntityJoin(EntityJoinLevelEvent e) {
        Caps1201.attach(e.getEntity());
    }

    private static void onRegisterCapabilities(net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent event) {
        // Forge's registration event goes to the mods that listen to it, on their own mod buses
        for (String id : modIds) {
            ModList.get().getModContainerById(id).ifPresent(c -> {
                try {
                    c.getEventBus().post(new RegisterCapabilitiesEvent());
                } catch (RuntimeException ex) {
                    LOG.warn("{} failed in its capability registration: {}", id, ex.toString());
                }
            });
        }
        // legacy block entities and entities answer getCapability themselves: make NeoForge ask them
        int types = 0;
        for (var entry : BuiltInRegistries.BLOCK_ENTITY_TYPE.entrySet()) {
            if (!modIds.contains(entry.getKey().location().getNamespace())) continue;
            var type = entry.getValue();
            bridge(event, type, Capabilities.ItemHandler.BLOCK, ForgeCapabilities.ITEM_HANDLER);
            bridge(event, type, Capabilities.FluidHandler.BLOCK, ForgeCapabilities.FLUID_HANDLER);
            bridge(event, type, Capabilities.EnergyStorage.BLOCK, ForgeCapabilities.ENERGY);
            types++;
        }
        for (var entry : BuiltInRegistries.ENTITY_TYPE.entrySet()) {
            if (!modIds.contains(entry.getKey().location().getNamespace())) continue;
            var type = entry.getValue();
            bridgeEntity(event, type, Capabilities.ItemHandler.ENTITY, ForgeCapabilities.ITEM_HANDLER);
            bridgeEntity(event, type, Capabilities.FluidHandler.ENTITY, ForgeCapabilities.FLUID_HANDLER);
            bridgeEntity(event, type, Capabilities.EnergyStorage.ENTITY, ForgeCapabilities.ENERGY);
        }
        // legacy items that declare initCapabilities(stack, nbt): the provider of the stack answers NeoForge's item capabilities too
        int items = 0;
        for (var entry : BuiltInRegistries.ITEM.entrySet()) {
            if (!modIds.contains(entry.getKey().location().getNamespace()) || !(entry.getValue() instanceof Caps1201.InitCapabilities1201)) continue;
            bridgeItem(event, entry.getValue(), Capabilities.ItemHandler.ITEM, ForgeCapabilities.ITEM_HANDLER);
            bridgeItem(event, entry.getValue(), Capabilities.FluidHandler.ITEM, ForgeCapabilities.FLUID_HANDLER_ITEM);
            bridgeItem(event, entry.getValue(), Capabilities.EnergyStorage.ITEM, ForgeCapabilities.ENERGY);
            items++;
        }
        LOG.info("Capabilities of {} legacy block entity type(s) and {} item(s) are visible to NeoForge", types, items);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void bridge(net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent event, net.minecraft.world.level.block.entity.BlockEntityType type,
            BlockCapability neo, Capability forge) {
        event.registerBlockEntity(neo, type, (be, side) -> {
            if (!(be instanceof ICapabilityProvider p)) return null;
            return p.getCapability(forge, (net.minecraft.core.Direction) side).resolve().orElse(null);
        });
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void bridgeItem(net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent event, net.minecraft.world.item.Item item,
            net.neoforged.neoforge.capabilities.ItemCapability neo, Capability forge) {
        event.registerItem(neo, (stack, ctx) -> {
            ICapabilityProvider p = Caps1201.itemProvider((net.minecraft.world.item.ItemStack) stack);
            return p == null ? null : p.getCapability(forge, null).resolve().orElse(null);
        }, item);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void bridgeEntity(net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent event, net.minecraft.world.entity.EntityType type,
            EntityCapability neo, Capability forge) {
        event.registerEntity(neo, type, (entity, ctx) -> {
            if (!(entity instanceof ICapabilityProvider p)) return null;
            return p.getCapability(forge, ctx instanceof net.minecraft.core.Direction d ? d : null).resolve().orElse(null);
        });
    }
}
