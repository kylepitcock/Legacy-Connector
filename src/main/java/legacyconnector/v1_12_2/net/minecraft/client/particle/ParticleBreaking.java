package legacyconnector.v1_12_2.net.minecraft.client.particle;

import legacyconnector.v1_12_2.net.minecraft.client.Minecraft;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.BufferBuilder;
import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.item.Item;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2 item fragment (eating, snowballs, slime): a random quarter of the item's particle sprite, falling with snow's gravity. */
public class ParticleBreaking extends Particle {
    protected ParticleBreaking(World worldIn, double posXIn, double posYIn, double posZIn, Item itemIn) {
        this(worldIn, posXIn, posYIn, posZIn, itemIn, 0);
    }

    protected ParticleBreaking(World worldIn, double posXIn, double posYIn, double posZIn, double xSpeedIn, double ySpeedIn, double zSpeedIn, Item itemIn, int meta) {
        this(worldIn, posXIn, posYIn, posZIn, itemIn, meta);
        motionX *= 0.10000000149011612D;
        motionY *= 0.10000000149011612D;
        motionZ *= 0.10000000149011612D;
        motionX += xSpeedIn;
        motionY += ySpeedIn;
        motionZ += zSpeedIn;
    }

    protected ParticleBreaking(World worldIn, double posXIn, double posYIn, double posZIn, Item itemIn, int meta) {
        super(worldIn, posXIn, posYIn, posZIn, 0.0D, 0.0D, 0.0D);
        try {
            setParticleTexture(Minecraft.getMinecraft().getRenderItem().getItemModelMesher().getParticleIcon(itemIn, meta));
        } catch (RuntimeException e) {
            // no item models yet: the fragment shows the particle sheet
        }
        particleRed = 1.0F;
        particleGreen = 1.0F;
        particleBlue = 1.0F;
        particleGravity = 1.0F; // Blocks.SNOW.blockParticleGravity
        particleScale /= 2.0F;
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

    public static class Factory implements IParticleFactory {
        @Override
        public Particle createParticle(int particleID, World worldIn, double xCoordIn, double yCoordIn, double zCoordIn, double xSpeedIn, double ySpeedIn,
                double zSpeedIn, int... p_178902_15_) {
            int meta = p_178902_15_ != null && p_178902_15_.length > 1 ? p_178902_15_[1] : 0;
            Item item = p_178902_15_ != null && p_178902_15_.length > 0 ? Item.getItemById(p_178902_15_[0]) : null;
            return item == null ? null : new ParticleBreaking(worldIn, xCoordIn, yCoordIn, zCoordIn, xSpeedIn, ySpeedIn, zSpeedIn, item, meta);
        }
    }

    public static class SlimeFactory implements IParticleFactory {
        @Override
        public Particle createParticle(int particleID, World worldIn, double xCoordIn, double yCoordIn, double zCoordIn, double xSpeedIn, double ySpeedIn,
                double zSpeedIn, int... p_178902_15_) {
            return new ParticleBreaking(worldIn, xCoordIn, yCoordIn, zCoordIn, legacyconnector.v1_12_2.net.minecraft.init.Items.SLIME_BALL);
        }
    }

    public static class SnowballFactory implements IParticleFactory {
        @Override
        public Particle createParticle(int particleID, World worldIn, double xCoordIn, double yCoordIn, double zCoordIn, double xSpeedIn, double ySpeedIn,
                double zSpeedIn, int... p_178902_15_) {
            return new ParticleBreaking(worldIn, xCoordIn, yCoordIn, zCoordIn, legacyconnector.v1_12_2.net.minecraft.init.Items.SNOWBALL);
        }
    }
}
