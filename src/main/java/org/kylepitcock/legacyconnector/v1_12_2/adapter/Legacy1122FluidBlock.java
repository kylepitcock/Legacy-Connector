package org.kylepitcock.legacyconnector.v1_12_2.adapter;

import java.util.Optional;
import legacyconnector.v1_12_2.net.minecraft.block.Block;
import legacyconnector.v1_12_2.net.minecraftforge.fluids.Fluid;
import legacyconnector.v1_12_2.net.minecraftforge.fluids.FluidStack;
import legacyconnector.v1_12_2.net.minecraftforge.fluids.IFluidBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.FluidBridge1122;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.WorldBridge1122;

/**
 * The modern block of a 1.12.2 fluid block (Forge's {@code BlockFluidBase}: {@code BlockFluidClassic}, {@code BlockFluidFinite}, or a mod's
 * {@code BlockLiquid}). Once {@link FluidBridge1122} binds it to the modern fluid of its Forge {@code Fluid}, each state holds that fluid's
 * state (source at level 0, flowing with the level mapped), the way a modern {@code LiquidBlock} does: the fluid renderer draws it (the block
 * model is not drawn), entities swim in it, the vanilla bucket picks a source up (through the legacy {@code drain}), and it has no outline or
 * collision box. The legacy block keeps flowing by its own scheduled ticks ({@code updateTick}) and keeps its legacy callbacks
 * ({@code onEntityCollision}, {@code neighborChanged}, ...). Unbound (a fluid block of no registered Forge fluid) it is a plain legacy block.
 */
public class Legacy1122FluidBlock extends Legacy1122Block implements BucketPickup {
    private volatile FluidState[] fluidByMeta;
    private volatile int[] lightByMeta;
    private volatile Fluid fluid;
    private int pickupFailures;

    public Legacy1122FluidBlock(Block legacy, Properties properties) {
        super(legacy, properties);
    }

    /** Binds the block to its modern fluid: the fluid state and the light of each metadata value. */
    public void mcc$bind(Fluid legacyFluid, FluidState[] byMeta, int[] light) {
        this.fluid = legacyFluid;
        this.lightByMeta = light;
        this.fluidByMeta = byMeta;
    }

    /** The legacy fluid this block holds, or null while unbound. */
    public Fluid mcc$fluid() {
        return fluidByMeta == null ? null : fluid;
    }

    private boolean bound() {
        return fluidByMeta != null;
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        FluidState[] table = fluidByMeta;
        if (table == null) return super.getFluidState(state);
        FluidState f = table[state.getValue(META)];
        return f != null ? f : super.getFluidState(state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return bound() ? RenderShape.INVISIBLE : super.getRenderShape(state);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter getter, BlockPos pos, CollisionContext context) {
        return bound() ? Shapes.empty() : super.getShape(state, getter, pos, context);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter getter, BlockPos pos, CollisionContext context) {
        return bound() ? Shapes.empty() : super.getCollisionShape(state, getter, pos, context);
    }

    /** Forge's BlockFluidBase light: the fluid's luminosity scaled by how full the block is. */
    @Override
    public int getLightEmission(BlockState state, BlockGetter level, BlockPos pos) {
        int own = super.getLightEmission(state, level, pos);
        int[] light = lightByMeta;
        return light == null ? own : Math.max(own, light[state.getValue(META)]);
    }

    // ------------------------------------------------------------------ buckets

    /** An empty bucket on a source: the legacy block is drained (its own {@code drain}) and the bucket of its fluid is returned. */
    @Override
    public ItemStack pickupBlock(Player player, LevelAccessor level, BlockPos pos, BlockState state) {
        Fluid own = mcc$fluid();
        if (own == null || !(level instanceof Level real) || !state.getFluidState().isSource()) return ItemStack.EMPTY;
        try {
            var world = WorldBridge1122.wrap(real);
            var p = legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos.mcc$of(pos);
            if (legacy() instanceof IFluidBlock block) {
                if (!block.canDrain(world, p)) return ItemStack.EMPTY;
                FluidStack simulated = block.drain(world, p, false);
                if (simulated == null || simulated.getFluid() == null || simulated.amount < Fluid.BUCKET_VOLUME) return ItemStack.EMPTY;
                ItemStack bucket = FluidBridge1122.modernBucket(simulated.getFluid());
                if (bucket.isEmpty()) return ItemStack.EMPTY;
                FluidStack drained = block.drain(world, p, true);
                return drained == null ? ItemStack.EMPTY : bucket;
            }
            ItemStack bucket = FluidBridge1122.modernBucket(own);
            if (bucket.isEmpty()) return ItemStack.EMPTY;
            real.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 11);
            return bucket;
        } catch (RuntimeException | LinkageError e) {
            if (pickupFailures++ < 3) {
                org.slf4j.LoggerFactory.getLogger("LegacyConnector/Fluids1122").error("Picking up {} with a bucket failed", legacy().getClass().getName(), e);
            }
            return ItemStack.EMPTY;
        }
    }

    @Override
    public Optional<SoundEvent> getPickupSound() {
        Fluid own = mcc$fluid();
        try {
            var sound = own == null ? null : own.getFillSound();
            var modern = sound == null ? null : sound.mcc$event();
            if (modern != null) return Optional.of(modern);
        } catch (RuntimeException | LinkageError e) {
            // the vanilla sound
        }
        return Optional.of(net.minecraft.sounds.SoundEvents.BUCKET_FILL);
    }
}
