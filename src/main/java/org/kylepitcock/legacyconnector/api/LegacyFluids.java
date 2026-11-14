package org.kylepitcock.legacyconnector.api;

import java.util.Optional;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import org.kylepitcock.legacyconnector.api.internal.FluidViews;

/**
 * Legacy fluids and their modern counterparts (added in API 0.3). Every legacy {@code Fluid} of 1.4.7 to 1.7.10 and 1.12.2 mods gets
 * a modern fluid (and fluid type) of its own, vanilla water and lava map to the vanilla fluids; for Forge 1.20.1 mods the legacy
 * fluid IS the modern one. Beta 1.7.3 has no fluid registry, so every method answers empty there
 * (see {@link LegacySupport#of(LegacySupport.Feature, LegacyVersion)}).
 */
public final class LegacyFluids {
    private LegacyFluids() {}

    /** The modern fluid (the source form) of a legacy {@code Fluid} object; empty for an unknown fluid and for objects that are no legacy fluid. */
    public static Optional<Fluid> modernFluid(Object legacyFluid) {
        return FluidViews.modernFluid(legacyFluid);
    }

    /** The legacy {@code Fluid} (opaque, see {@link LegacyAccess}) of a modern fluid for one version; empty when the version has no such fluid. */
    public static Optional<Object> legacyFluid(Fluid modern, LegacyVersion version) {
        return FluidViews.legacyFluid(modern, version);
    }

    /** The modern stack of a legacy {@code FluidStack} ({@link FluidStack#EMPTY} for null, empty or unknown fluids). */
    public static FluidStack fromLegacy(Object legacyFluidStack) {
        return FluidViews.stackFromLegacy(legacyFluidStack);
    }

    /** The legacy {@code FluidStack} (opaque) of a modern one; empty for an empty stack or a fluid the version has no counterpart of. */
    public static Optional<Object> toLegacy(FluidStack modern, LegacyVersion version) {
        return FluidViews.stackToLegacy(modern, version);
    }

    /**
     * The modern filled bucket of a legacy fluid: vanilla's for water and lava, else the mod's own bucket item (1.12.2: also the
     * generated bucket of a fluid without one; 1.7.10 family: the bucket registered with {@code FluidContainerRegistry}). Empty when the
     * fluid has none.
     */
    public static Optional<ItemStack> bucketOf(Object legacyFluid) {
        return FluidViews.bucketOf(legacyFluid);
    }
}
