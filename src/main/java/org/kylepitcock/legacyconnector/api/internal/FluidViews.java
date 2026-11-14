package org.kylepitcock.legacyconnector.api.internal;

import java.util.Optional;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import org.kylepitcock.legacyconnector.api.LegacyVersion;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.FluidBridge1122;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.FluidBridge;

/** The fluid conversions behind {@code LegacyFluids}. */
public final class FluidViews {
    private FluidViews() {}

    public static Optional<Fluid> modernFluid(Object legacy) {
        if (legacy instanceof legacyconnector.v1_7_10.net.minecraftforge.fluids.Fluid f) return Optional.ofNullable(FluidBridge.modernOf(f));
        if (legacy instanceof legacyconnector.v1_12_2.net.minecraftforge.fluids.Fluid f) return Optional.ofNullable(FluidBridge1122.modernOf(f));
        if (legacy instanceof Fluid f) return Optional.of(f); // 1.20.1
        return Optional.empty();
    }

    public static Optional<Object> legacyFluid(Fluid modern, LegacyVersion version) {
        if (modern == null) return Optional.empty();
        return Optional.ofNullable(switch (version) {
            case V1_7_10, V1_4_7, V1_5_2, V1_6_4 -> FluidBridge.legacyOf(modern);
            case V1_12_2 -> FluidBridge1122.legacyOf(modern);
            case V1_20_1 -> modern;
            case VB1_7_3 -> null; // Beta has no fluid registry
        });
    }

    public static FluidStack stackFromLegacy(Object legacy) {
        if (legacy instanceof legacyconnector.v1_7_10.net.minecraftforge.fluids.FluidStack s) return FluidBridge.toModern(s);
        if (legacy instanceof legacyconnector.v1_12_2.net.minecraftforge.fluids.FluidStack s) return FluidBridge1122.toModern(s);
        if (legacy instanceof FluidStack s) return s;
        return FluidStack.EMPTY;
    }

    public static Optional<Object> stackToLegacy(FluidStack modern, LegacyVersion version) {
        if (modern == null || modern.isEmpty()) return Optional.empty();
        return Optional.ofNullable(switch (version) {
            case V1_7_10, V1_4_7, V1_5_2, V1_6_4 -> FluidBridge.fromModern(modern);
            case V1_12_2 -> FluidBridge1122.fromModern(modern);
            case V1_20_1 -> modern;
            case VB1_7_3 -> null;
        });
    }

    public static Optional<ItemStack> bucketOf(Object legacy) {
        if (legacy instanceof Fluid f) {
            // 1.20.1: the fluid's own bucket item
            ItemStack bucket = new ItemStack(f.getBucket());
            return f.getBucket() == net.minecraft.world.item.Items.AIR ? Optional.empty() : Optional.of(bucket);
        }
        Fluid modern = modernFluid(legacy).orElse(null);
        if (modern == Fluids.WATER) return Optional.of(new ItemStack(net.minecraft.world.item.Items.WATER_BUCKET));
        if (modern == Fluids.LAVA) return Optional.of(new ItemStack(net.minecraft.world.item.Items.LAVA_BUCKET));
        if (legacy instanceof legacyconnector.v1_12_2.net.minecraftforge.fluids.Fluid f) {
            ItemStack bucket = FluidBridge1122.modernBucket(f);
            return bucket.isEmpty() ? Optional.empty() : Optional.of(bucket);
        }
        if (legacy instanceof legacyconnector.v1_7_10.net.minecraftforge.fluids.Fluid f) {
            var filled = legacyconnector.v1_7_10.net.minecraftforge.fluids.FluidContainerRegistry.fillFluidContainer(
                    new legacyconnector.v1_7_10.net.minecraftforge.fluids.FluidStack(f, legacyconnector.v1_7_10.net.minecraftforge.fluids.FluidContainerRegistry.BUCKET_VOLUME),
                    legacyconnector.v1_7_10.net.minecraftforge.fluids.FluidContainerRegistry.EMPTY_BUCKET);
            if (filled == null) return Optional.empty();
            ItemStack bucket = legacyconnector.v1_7_10.net.minecraft.item.ItemStack.mcc$toModern(filled);
            return bucket.isEmpty() ? Optional.empty() : Optional.of(bucket);
        }
        return Optional.empty();
    }
}
