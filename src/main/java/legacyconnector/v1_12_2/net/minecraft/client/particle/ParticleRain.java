package legacyconnector.v1_12_2.net.minecraft.client.particle;

import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2 rain splash: a white droplet thrown up that falls back and vanishes on the ground or inside a liquid or solid block. */
public class ParticleRain extends Particle {
    protected ParticleRain(World worldIn, double xCoordIn, double yCoordIn, double zCoordIn) {
        super(worldIn, xCoordIn, yCoordIn, zCoordIn, 0.0D, 0.0D, 0.0D);
        motionX *= 0.30000001192092896D;
        motionY = Math.random() * 0.20000000298023224D + 0.10000000149011612D;
        motionZ *= 0.30000001192092896D;
        particleRed = 1.0F;
        particleGreen = 1.0F;
        particleBlue = 1.0F;
        setParticleTextureIndex(19 + rand.nextInt(4));
        setSize(0.01F, 0.01F);
        particleGravity = 0.06F;
        particleMaxAge = (int) (8.0D / (Math.random() * 0.8D + 0.2D));
    }

    @Override
    public void onUpdate() {
        prevPosX = posX;
        prevPosY = posY;
        prevPosZ = posZ;
        motionY -= particleGravity;
        move(motionX, motionY, motionZ);
        motionX *= 0.9800000190734863D;
        motionY *= 0.9800000190734863D;
        motionZ *= 0.9800000190734863D;
        if (particleMaxAge-- <= 0) setExpired();
        if (onGround) {
            if (Math.random() < 0.5D) setExpired();
            motionX *= 0.699999988079071D;
            motionZ *= 0.699999988079071D;
        }
        var level = world == null ? null : world.mcc$level();
        if (level == null) return;
        var pos = net.minecraft.core.BlockPos.containing(posX, posY, posZ);
        if (!level.isLoaded(pos)) return;
        var state = level.getBlockState(pos);
        var fluid = state.getFluidState();
        double top;
        if (!fluid.isEmpty()) {
            top = fluid.getHeight(level, pos);
        } else {
            var shape = state.getCollisionShape(level, pos);
            if (shape.isEmpty()) return;
            top = shape.max(net.minecraft.core.Direction.Axis.Y);
        }
        if (posY < Math.floor(posY) + top) setExpired();
    }

    public static class Factory implements IParticleFactory {
        @Override
        public Particle createParticle(int particleID, World worldIn, double xCoordIn, double yCoordIn, double zCoordIn, double xSpeedIn, double ySpeedIn,
                double zSpeedIn, int... p_178902_15_) {
            return new ParticleRain(worldIn, xCoordIn, yCoordIn, zCoordIn);
        }
    }
}
