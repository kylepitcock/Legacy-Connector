package legacyconnector.v1_12_2.net.minecraft.client.particle;

import legacyconnector.v1_12_2.net.minecraft.block.Block;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.BlockRendererDispatcher;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.BufferBuilder;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.color.BlockColors;
import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2 block-breaking fragment: a random quarter of the block's particle sprite, tinted by the block's colour, with the block's light. */
public class ParticleDigging extends Particle {
    private final IBlockState sourceState;
    private BlockPos sourcePos;

    public ParticleDigging(World worldIn, double xCoordIn, double yCoordIn, double zCoordIn, double xSpeedIn, double ySpeedIn, double zSpeedIn, IBlockState state) {
        super(worldIn, xCoordIn, yCoordIn, zCoordIn, xSpeedIn, ySpeedIn, zSpeedIn);
        this.sourceState = state;
        try {
            setParticleTexture(BlockRendererDispatcher.mcc$instance().getBlockModelShapes().getTexture(state));
        } catch (RuntimeException e) {
            // no model yet: the fragment shows the particle sheet
        }
        this.particleGravity = state.getBlock().blockParticleGravity;
        this.particleRed = 0.6F;
        this.particleGreen = 0.6F;
        this.particleBlue = 0.6F;
        this.particleScale /= 2.0F;
    }

    public ParticleDigging setBlockPos(BlockPos pos) {
        this.sourcePos = pos;
        if (!mcc$isGrass()) multiplyColor(pos);
        return this;
    }

    public ParticleDigging init() {
        this.sourcePos = new BlockPos(posX, posY, posZ);
        if (!mcc$isGrass()) multiplyColor(sourcePos);
        return this;
    }

    private boolean mcc$isGrass() {
        var modern = org.kylepitcock.legacyconnector.v1_12_2.bridge.StatesBridge1122.toModern(sourceState);
        return modern != null && modern.is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK);
    }

    protected void multiplyColor(BlockPos pos) {
        int color = BlockColors.mcc$instance().colorMultiplier(sourceState, world, pos, 0);
        particleRed *= (float) (color >> 16 & 255) / 255.0F;
        particleGreen *= (float) (color >> 8 & 255) / 255.0F;
        particleBlue *= (float) (color & 255) / 255.0F;
    }

    @Override
    public int getFXLayer() {
        return 1;
    }

    @Override
    public void renderParticle(BufferBuilder buffer, Entity entityIn, float partialTicks, float rotationX, float rotationZ, float rotationYZ, float rotationXY, float rotationXZ) {
        float u0 = ((float) particleTextureIndexX + particleTextureJitterX / 4.0F) / 16.0F;
        float u1 = u0 + 0.015609375F;
        float v0 = ((float) particleTextureIndexY + particleTextureJitterY / 4.0F) / 16.0F;
        float v1 = v0 + 0.015609375F;
        float size = 0.1F * particleScale;
        if (particleTexture != null) {
            u0 = particleTexture.getInterpolatedU((double) (particleTextureJitterX / 4.0F * 16.0F));
            u1 = particleTexture.getInterpolatedU((double) ((particleTextureJitterX + 1.0F) / 4.0F * 16.0F));
            v0 = particleTexture.getInterpolatedV((double) (particleTextureJitterY / 4.0F * 16.0F));
            v1 = particleTexture.getInterpolatedV((double) ((particleTextureJitterY + 1.0F) / 4.0F * 16.0F));
        }
        float x = (float) (prevPosX + (posX - prevPosX) * (double) partialTicks - interpPosX);
        float y = (float) (prevPosY + (posY - prevPosY) * (double) partialTicks - interpPosY);
        float z = (float) (prevPosZ + (posZ - prevPosZ) * (double) partialTicks - interpPosZ);
        int light = getBrightnessForRender(partialTicks);
        int sky = light >> 16 & 65535, block = light & 65535;
        buffer.pos(x - rotationX * size - rotationXY * size, y - rotationZ * size, z - rotationYZ * size - rotationXZ * size).tex(u0, v1)
                .color(particleRed, particleGreen, particleBlue, 1.0F).lightmap(sky, block).endVertex();
        buffer.pos(x - rotationX * size + rotationXY * size, y + rotationZ * size, z - rotationYZ * size + rotationXZ * size).tex(u0, v0)
                .color(particleRed, particleGreen, particleBlue, 1.0F).lightmap(sky, block).endVertex();
        buffer.pos(x + rotationX * size + rotationXY * size, y + rotationZ * size, z + rotationYZ * size + rotationXZ * size).tex(u1, v0)
                .color(particleRed, particleGreen, particleBlue, 1.0F).lightmap(sky, block).endVertex();
        buffer.pos(x + rotationX * size - rotationXY * size, y - rotationZ * size, z + rotationYZ * size - rotationXZ * size).tex(u1, v1)
                .color(particleRed, particleGreen, particleBlue, 1.0F).lightmap(sky, block).endVertex();
    }

    @Override
    public int getBrightnessForRender(float partialTick) {
        int i = super.getBrightnessForRender(partialTick);
        int j = 0;
        if (world != null && sourcePos != null && world.isBlockLoaded(sourcePos)) j = world.getCombinedLight(sourcePos, 0);
        return i == 0 ? j : i;
    }

    public static class Factory implements IParticleFactory {
        @Override
        public Particle createParticle(int particleID, World worldIn, double xCoordIn, double yCoordIn, double zCoordIn, double xSpeedIn, double ySpeedIn, double zSpeedIn,
                int... p_178902_15_) {
            IBlockState state = p_178902_15_ == null || p_178902_15_.length == 0 ? null : Block.getStateById(p_178902_15_[0]);
            return state == null ? null : new ParticleDigging(worldIn, xCoordIn, yCoordIn, zCoordIn, xSpeedIn, ySpeedIn, zSpeedIn, state).init();
        }
    }
}
