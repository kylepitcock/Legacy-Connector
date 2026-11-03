package legacyconnector.v1_12_2.net.minecraft.client.particle;

import legacyconnector.v1_12_2.net.minecraft.client.renderer.BufferBuilder;
import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2 firework particles: the {@link Spark} mods spawn for their own bursts (vanilla fireworks are the modern ones). */
public class ParticleFirework {
    /** A firework spark: the sheet's spark frames, optionally leaving a trail of half-aged sparks and twinkling in its last two thirds. */
    public static class Spark extends ParticleSimpleAnimated {
        private boolean trail;
        private boolean twinkle;
        private final ParticleManager effectRenderer;

        public Spark(World worldIn, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, ParticleManager effectRendererIn) {
            super(worldIn, x, y, z, 160, 8, -0.004F);
            motionX = xSpeed;
            motionY = ySpeed;
            motionZ = zSpeed;
            effectRenderer = effectRendererIn;
            particleScale *= 0.75F;
            particleMaxAge = 48 + rand.nextInt(12);
        }

        public void setTrail(boolean trailIn) {
            trail = trailIn;
        }

        public void setTwinkle(boolean twinkleIn) {
            twinkle = twinkleIn;
        }

        @Override
        public boolean shouldDisableDepth() {
            return true;
        }

        @Override
        public void renderParticle(BufferBuilder buffer, Entity entityIn, float partialTicks, float rotationX, float rotationZ, float rotationYZ, float rotationXY,
                float rotationXZ) {
            if (!twinkle || particleAge < particleMaxAge / 3 || (particleAge + particleMaxAge) / 3 % 2 == 0) {
                super.renderParticle(buffer, entityIn, partialTicks, rotationX, rotationZ, rotationYZ, rotationXY, rotationXZ);
            }
        }

        @Override
        public void onUpdate() {
            super.onUpdate();
            if (trail && particleAge < particleMaxAge / 2 && (particleAge + particleMaxAge) % 2 == 0 && effectRenderer != null) {
                Spark spark = new Spark(world, posX, posY, posZ, 0.0D, 0.0D, 0.0D, effectRenderer);
                spark.setAlphaF(0.99F);
                spark.setRBGColorF(particleRed, particleGreen, particleBlue);
                spark.particleAge = spark.particleMaxAge / 2;
                mcc$copyFade(spark);
                spark.twinkle = twinkle;
                effectRenderer.addEffect(spark);
            }
        }
    }

    /**
     * The rocket burst: the explosions of a firework (an NBT list of Type, Flicker, Trail, Colors, FadeColors) start as the modern firework
     * burst at the same place (the starter itself draws nothing).
     */
    public static class Starter extends Particle {
        public Starter(World worldIn, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, ParticleManager manager,
                legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagList explosions) {
            super(worldIn, x, y, z, 0.0D, 0.0D, 0.0D);
            this.motionX = xSpeed;
            this.motionY = ySpeed;
            this.motionZ = zSpeed;
            this.particleMaxAge = 8;
            var level = net.minecraft.client.Minecraft.getInstance().level;
            if (level == null || explosions == null || explosions.tagCount() == 0) {
                setExpired();
                return;
            }
            java.util.List<net.minecraft.world.item.component.FireworkExplosion> list = new java.util.ArrayList<>();
            for (int i = 0; i < explosions.tagCount(); i++) {
                var tag = explosions.getCompoundTagAt(i);
                var shapes = net.minecraft.world.item.component.FireworkExplosion.Shape.values();
                int type = tag.getByte("Type");
                list.add(new net.minecraft.world.item.component.FireworkExplosion(shapes[type < 0 || type >= shapes.length ? 0 : type],
                        new it.unimi.dsi.fastutil.ints.IntArrayList(tag.getIntArray("Colors")),
                        new it.unimi.dsi.fastutil.ints.IntArrayList(tag.getIntArray("FadeColors")), tag.getBoolean("Trail"), tag.getBoolean("Flicker")));
            }
            net.minecraft.client.Minecraft.getInstance().particleEngine.add(new net.minecraft.client.particle.FireworkParticles.Starter(level, x, y, z, xSpeed,
                    ySpeed, zSpeed, net.minecraft.client.Minecraft.getInstance().particleEngine, list));
            setExpired();
        }

        @Override
        public int getFXLayer() {
            return 0;
        }

        @Override
        public void renderParticle(legacyconnector.v1_12_2.net.minecraft.client.renderer.BufferBuilder buffer, Entity entityIn, float partialTicks,
                float rotationX, float rotationZ, float rotationYZ, float rotationXY, float rotationXZ) {
        }
    }

    public static class Factory implements IParticleFactory {
        @Override
        public Particle createParticle(int particleID, World worldIn, double xCoordIn, double yCoordIn, double zCoordIn, double xSpeedIn, double ySpeedIn,
                double zSpeedIn, int... p_178902_15_) {
            var mc = legacyconnector.v1_12_2.net.minecraft.client.Minecraft.getMinecraft();
            Spark spark = new Spark(worldIn, xCoordIn, yCoordIn, zCoordIn, xSpeedIn, ySpeedIn, zSpeedIn, mc == null ? null : mc.effectRenderer);
            spark.setAlphaF(0.99F);
            return spark;
        }
    }
}
