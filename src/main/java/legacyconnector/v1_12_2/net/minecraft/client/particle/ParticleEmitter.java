package legacyconnector.v1_12_2.net.minecraft.client.particle;

import legacyconnector.v1_12_2.net.minecraft.client.renderer.BufferBuilder;
import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.util.EnumParticleTypes;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2 particle emitter (crit hits): invisible, spawns up to 16 particles of its type inside the attached entity each tick. */
public class ParticleEmitter extends Particle {
    private final Entity attachedEntity;
    private int age;
    private final int lifetime;
    private final EnumParticleTypes particleTypes;

    public ParticleEmitter(World worldIn, Entity entityIn, EnumParticleTypes particleTypesIn) {
        this(worldIn, entityIn, particleTypesIn, 3);
    }

    public ParticleEmitter(World worldIn, Entity entityIn, EnumParticleTypes particleTypesIn, int lifetimeIn) {
        super(worldIn, Entity.mcc$get$posX(entityIn), entityIn.getEntityBoundingBox().minY + (double) (Entity.mcc$get$height(entityIn) / 2.0F),
                Entity.mcc$get$posZ(entityIn), Entity.mcc$get$motionX(entityIn), Entity.mcc$get$motionY(entityIn), Entity.mcc$get$motionZ(entityIn));
        attachedEntity = entityIn;
        lifetime = lifetimeIn;
        particleTypes = particleTypesIn;
        onUpdate();
    }

    @Override
    public void renderParticle(BufferBuilder buffer, Entity entityIn, float partialTicks, float rotationX, float rotationZ, float rotationYZ, float rotationXY, float rotationXZ) {
    }

    @Override
    public void onUpdate() {
        float w = Entity.mcc$get$width(attachedEntity);
        float h = Entity.mcc$get$height(attachedEntity);
        for (int i = 0; i < 16; ++i) {
            double d0 = (double) (rand.nextFloat() * 2.0F - 1.0F);
            double d1 = (double) (rand.nextFloat() * 2.0F - 1.0F);
            double d2 = (double) (rand.nextFloat() * 2.0F - 1.0F);
            if (d0 * d0 + d1 * d1 + d2 * d2 <= 1.0D && world != null) {
                double d3 = Entity.mcc$get$posX(attachedEntity) + d0 * (double) w / 4.0D;
                double d4 = attachedEntity.getEntityBoundingBox().minY + (double) (h / 2.0F) + d1 * (double) h / 4.0D;
                double d5 = Entity.mcc$get$posZ(attachedEntity) + d2 * (double) w / 4.0D;
                world.spawnParticle(particleTypes, false, d3, d4, d5, d0, d1 + 0.2D, d2);
            }
        }
        ++age;
        if (age >= lifetime) setExpired();
    }

    @Override
    public int getFXLayer() {
        return 3;
    }
}
