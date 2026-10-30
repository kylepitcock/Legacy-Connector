package org.kylepitcock.legacyconnector.v1_12_2.client;

import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.StatesBridge1122;

/**
 * A legacy block access as modern renderers read it (the liquid renderer, ambient occlusion): states bridged to modern ones, light from the
 * legacy combined light, biome tints and shading from the client level. Without an access it reads the client level.
 */
public final class LegacyBlockGetter1122 implements net.minecraft.world.level.BlockAndTintGetter {
    private final IBlockAccess access;

    public LegacyBlockGetter1122(IBlockAccess access) {
        this.access = access;
    }

    private static net.minecraft.client.multiplayer.ClientLevel level() {
        return net.minecraft.client.Minecraft.getInstance().level;
    }

    @Override
    public net.minecraft.world.level.block.state.BlockState getBlockState(net.minecraft.core.BlockPos pos) {
        if (access == null) {
            var level = level();
            return level == null ? net.minecraft.world.level.block.Blocks.AIR.defaultBlockState() : level.getBlockState(pos);
        }
        var state = StatesBridge1122.toModern(access.getBlockState(new BlockPos(pos.getX(), pos.getY(), pos.getZ())));
        return state == null ? net.minecraft.world.level.block.Blocks.AIR.defaultBlockState() : state;
    }

    @Override
    public net.minecraft.world.level.material.FluidState getFluidState(net.minecraft.core.BlockPos pos) {
        return getBlockState(pos).getFluidState();
    }

    @Override
    public net.minecraft.world.level.block.entity.BlockEntity getBlockEntity(net.minecraft.core.BlockPos pos) {
        return null;
    }

    @Override
    public int getBrightness(net.minecraft.world.level.LightLayer layer, net.minecraft.core.BlockPos pos) {
        int combined;
        if (access != null) {
            combined = access.getCombinedLight(new BlockPos(pos.getX(), pos.getY(), pos.getZ()), 0);
        } else {
            var level = level();
            return level == null ? 15 : level.getBrightness(layer, pos);
        }
        return layer == net.minecraft.world.level.LightLayer.SKY ? combined >> 20 & 15 : combined >> 4 & 15;
    }

    @Override
    public int getRawBrightness(net.minecraft.core.BlockPos pos, int amount) {
        return Math.max(getBrightness(net.minecraft.world.level.LightLayer.SKY, pos) - amount, getBrightness(net.minecraft.world.level.LightLayer.BLOCK, pos));
    }

    @Override
    public float getShade(net.minecraft.core.Direction direction, boolean shade) {
        var level = level();
        if (level != null) return level.getShade(direction, shade);
        if (!shade) return 1.0F;
        return switch (direction) {
            case DOWN -> 0.5F;
            case NORTH, SOUTH -> 0.8F;
            case WEST, EAST -> 0.6F;
            default -> 1.0F;
        };
    }

    @Override
    public net.minecraft.world.level.lighting.LevelLightEngine getLightEngine() {
        var level = level();
        return level == null ? null : level.getLightEngine();
    }

    @Override
    public int getBlockTint(net.minecraft.core.BlockPos pos, net.minecraft.world.level.ColorResolver resolver) {
        var level = level();
        return level == null ? 0xFFFFFF : level.getBlockTint(pos, resolver);
    }

    @Override
    public int getHeight() {
        var level = level();
        return level == null ? 384 : level.getHeight();
    }

    @Override
    public int getMinBuildHeight() {
        var level = level();
        return level == null ? -64 : level.getMinBuildHeight();
    }
}
