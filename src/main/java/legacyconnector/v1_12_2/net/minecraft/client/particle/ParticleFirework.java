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
}
