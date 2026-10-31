package org.kylepitcock.legacyconnector.api;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;
import org.kylepitcock.legacyconnector.api.internal.BuiltinAdapters;

/**
 * Registrar for exposing legacy tile entities as NeoForge block capabilities. Passed to
 * {@link LegacyAddon#registerCapabilities(LegacyCapabilities)}.
 *
 * <p>All legacy tile entities of one legacy version share one modern block entity type (see the API guide), so a capability for
 * "the essentia jar" is a rule: <em>if the hosted legacy tile is an instance of class X of mod M, answer capability C with this
 * adapter</em>. Rules of addons are asked before Legacy Connector's own built-in adapters; an adapter that returns {@code null}
 * lets the next rule (and finally the built-ins) answer.
 *
 * <p>NeoForge caches capability lookups ({@code BlockCapabilityCache}) until {@code Level.invalidateCapabilities(pos)}. Adapters
 * should therefore return a handle that stays valid while the tile entity lives (read the legacy tile on every call instead of
 * snapshotting its state).
 *
 * <h2>Built-in adapters</h2>
 * Legacy Connector already registers these on every legacy tile of the matching kind, so addons do not need to:
 * <ul>
 *   <li>1.7.10 {@code IInventory}/{@code ISidedInventory} as {@code Capabilities.ItemHandler.BLOCK};</li>
 *   <li>1.7.10 Forge {@code IFluidHandler} as {@code Capabilities.FluidHandler.BLOCK};</li>
 *   <li>1.7.10 CoFH {@code IEnergyReceiver}/{@code IEnergyProvider} as {@code Capabilities.EnergyStorage.BLOCK} (1 RF = 1 FE);</li>
 *   <li>1.12.2 tiles answering {@code CapabilityItemHandler.ITEM_HANDLER_CAPABILITY} / {@code CapabilityEnergy.ENERGY} as
 *       the item handler / fluid handler / energy capabilities.</li>
 * </ul>
 * The same adapters are available as constants below, to reuse inside an adapter of your own (e.g. to expose only the
 * output slots of an inventory by wrapping the result).
 */
public interface LegacyCapabilities {
    /**
     * Converts a legacy tile entity to the capability object.
     *
     * @param legacyTile the mod's own {@code TileEntity} instance (opaque; use {@link LegacyAccess})
     * @param side       the queried side, or null for "no particular side"
     * @param host       the modern block entity hosting the tile (level, position, state)
     * @return the capability object, or null if this adapter does not provide it
     */
    @FunctionalInterface
    interface TileAdapter<T> {
        @Nullable T adapt(Object legacyTile, @Nullable Direction side, BlockEntity host);
    }

    /**
     * Provides {@code capability} for legacy tiles that are instances of the class {@code tileClassName} (a class, superclass or
     * interface) of the legacy mod {@code modId}. The class is resolved lazily; if the mod or class is missing the rule never matches.
     */
    <T> void forTile(String modId, String tileClassName, BlockCapability<T, @Nullable Direction> capability, TileAdapter<T> adapter);

    /** Provides {@code capability} for every legacy tile entity for which the adapter returns non-null (it must check the tile itself). */
    <T> void forAnyTile(BlockCapability<T, @Nullable Direction> capability, TileAdapter<T> adapter);

    /** 1.7.10 {@code IInventory}/{@code ISidedInventory} tiles as an item handler. */
    TileAdapter<IItemHandler> INVENTORY_ITEMS = BuiltinAdapters::inventory1710;
    /** 1.7.10 Forge {@code IFluidHandler} tiles as a fluid handler. */
    TileAdapter<IFluidHandler> FLUIDS_1710 = BuiltinAdapters::fluids1710;
    /** 1.7.10 CoFH RF tiles as energy storage. */
    TileAdapter<IEnergyStorage> RF_ENERGY = BuiltinAdapters::rf1710;
    /** 1.12.2 tiles that answer the Forge item handler capability. */
    TileAdapter<IItemHandler> FORGE_ITEMS_1122 = BuiltinAdapters::items1122;
    /** 1.12.2 tiles that answer the Forge fluid handler capability. */
    TileAdapter<IFluidHandler> FORGE_FLUIDS_1122 = BuiltinAdapters::fluids1122;
    /** 1.12.2 tiles that answer the Forge energy capability. */
    TileAdapter<IEnergyStorage> FORGE_ENERGY_1122 = BuiltinAdapters::energy1122;
}
