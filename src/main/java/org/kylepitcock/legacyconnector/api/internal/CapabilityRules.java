package org.kylepitcock.legacyconnector.api.internal;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.ICapabilityProvider;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.kylepitcock.legacyconnector.api.LegacyApi;
import org.kylepitcock.legacyconnector.api.LegacyCapabilities;
import org.kylepitcock.legacyconnector.api.LegacyCapabilities.TileAdapter;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.TileEntityBridge1122;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.TileEntityBridge;

/**
 * The addons' capability rules, registered on the host block entity types of every hosted legacy version (1.7.10 family, 1.12.2, Beta 1.7.3) while NeoForge fires
 * {@code RegisterCapabilitiesEvent}.
 */
public final class CapabilityRules implements LegacyCapabilities {
    private static final class Rule<T> {
        final String modId;
        final String className;
        final TileAdapter<T> adapter;
        private Class<?> resolved;
        private boolean tried;

        Rule(String modId, String className, TileAdapter<T> adapter) {
            this.modId = modId;
            this.className = className;
            this.adapter = adapter;
        }

        boolean matches(Object tile) {
            if (className == null) return true;
            if (!tried) {
                tried = true;
                resolved = LegacyApi.classOf(modId, className).orElse(null);
            }
            return resolved != null && resolved.isInstance(tile);
        }
    }

    private final Map<BlockCapability<?, Direction>, List<Rule<?>>> rules = new LinkedHashMap<>();

    @Override
    public <T> void forTile(String modId, String tileClassName, BlockCapability<T, Direction> capability, TileAdapter<T> adapter) {
        rules.computeIfAbsent(capability, c -> new ArrayList<>()).add(new Rule<>(modId, tileClassName, adapter));
    }

    @Override
    public <T> void forAnyTile(BlockCapability<T, Direction> capability, TileAdapter<T> adapter) {
        rules.computeIfAbsent(capability, c -> new ArrayList<>()).add(new Rule<>(null, null, adapter));
    }

    /** Collects the addons' rules and registers them, plus the 1.12.2 built-ins, on the host types that exist. */
    public static void apply(RegisterCapabilitiesEvent event) {
        CapabilityRules registrar = new CapabilityRules();
        registrar.forAnyTile(Capabilities.ItemHandler.BLOCK, LegacyCapabilities.INVENTORY_ITEMS_B173);
        registrar.forAnyTile(Capabilities.ItemHandler.BLOCK, LegacyCapabilities.FORGE_ITEMS_1122);
        registrar.forAnyTile(Capabilities.FluidHandler.BLOCK, LegacyCapabilities.FORGE_FLUIDS_1122);
        registrar.forAnyTile(Capabilities.EnergyStorage.BLOCK, LegacyCapabilities.FORGE_ENERGY_1122);
        // tiles that only implement Redstone Flux (cofh.redstoneflux.api), asked after Forge Energy
        registrar.forAnyTile(Capabilities.EnergyStorage.BLOCK, LegacyCapabilities.RF_ENERGY_1122);
        // addons first: their rules are asked before the built-ins of the same capability
        CapabilityRules addons = new CapabilityRules();
        AddonHost.registerCapabilities(addons);
        Map<BlockCapability<?, Direction>, List<Rule<?>>> all = new LinkedHashMap<>();
        addons.rules.forEach((cap, list) -> all.computeIfAbsent(cap, c -> new ArrayList<>()).addAll(list));
        registrar.rules.forEach((cap, list) -> all.computeIfAbsent(cap, c -> new ArrayList<>()).addAll(list));
        for (var e : all.entrySet()) register(event, e.getKey(), e.getValue());
        if (!InPlaceMods.ids().isEmpty()) for (var e : addons.rules.entrySet()) registerInPlace(event, e.getKey(), e.getValue());
    }

    @SuppressWarnings("unchecked")
    private static <T> void register(RegisterCapabilitiesEvent event, BlockCapability<T, Direction> cap, List<Rule<?>> list) {
        ICapabilityProvider<BlockEntity, Direction, T> provider = (be, side) -> {
            var tile = Convert.tileOf(be).orElse(null);
            if (tile == null) return null;
            for (Rule<?> r : list) {
                if (!r.matches(tile)) continue;
                T result = ((TileAdapter<T>) r.adapter).adapt(tile, side, be);
                if (result != null) return result;
            }
            return null;
        };
        BlockEntityType<?> t1710 = TileEntityBridge.type();
        BlockEntityType<?> t1122 = TileEntityBridge1122.type();
        if (t1710 != null) event.registerBlockEntity(cap, t1710, provider);
        if (t1122 != null) event.registerBlockEntity(cap, t1122, provider);
        BlockEntityType<?> tB173 = org.kylepitcock.legacyconnector.vb1_7_3.TileEntitiesB173.type();
        if (tB173 != null) event.registerBlockEntity(cap, tB173, provider);
    }

    /**
     * Addon rules on the block entity types of Forge 1.20.1 mods: those are the mod's own types, so the "legacy tile" an adapter gets is
     * the block entity itself (only addon rules; the mods expose their own capabilities natively).
     */
    @SuppressWarnings("unchecked")
    private static <T> void registerInPlace(RegisterCapabilitiesEvent event, BlockCapability<T, Direction> cap, List<Rule<?>> list) {
        ICapabilityProvider<BlockEntity, Direction, T> inPlace = (be, side) -> {
            for (Rule<?> r : list) {
                if (!r.matches(be)) continue;
                T result = ((TileAdapter<T>) r.adapter).adapt(be, side, be);
                if (result != null) return result;
            }
            return null;
        };
        for (var entry : net.minecraft.core.registries.BuiltInRegistries.BLOCK_ENTITY_TYPE.entrySet()) {
            if (InPlaceMods.isInPlaceNamespace(entry.getKey().location().getNamespace())) event.registerBlockEntity(cap, entry.getValue(), inPlace);
        }
    }
}
