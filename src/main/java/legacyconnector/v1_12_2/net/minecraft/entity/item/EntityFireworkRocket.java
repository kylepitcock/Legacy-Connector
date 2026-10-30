package legacyconnector.v1_12_2.net.minecraft.entity.item;

import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2 firework rocket: {@code new EntityFireworkRocket(world, x, y, z, stack)} is a modern rocket of that firework. */
public class EntityFireworkRocket extends legacyconnector.v1_12_2.net.minecraft.entity.Entity {
    public EntityFireworkRocket(World world) {
        super(world);
        setSize(0.25F, 0.25F);
    }

    public EntityFireworkRocket(World world, double x, double y, double z, legacyconnector.v1_12_2.net.minecraft.item.ItemStack stack) {
        this(world);
        setPosition(x, y, z);
        var m = mcc$self();
        if (m != null && !m.isAddedToLevel()) {
            var modern = org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122.toModern(stack);
            var tag = new net.minecraft.nbt.CompoundTag();
            m.saveWithoutId(tag);
            if (!modern.isEmpty()) tag.put("FireworksItem", modern.save(m.registryAccess()));
            var fireworks = modern.get(net.minecraft.core.component.DataComponents.FIREWORKS);
            int flight = 1 + (fireworks == null ? 0 : fireworks.flightDuration());
            tag.putInt("LifeTime", 10 * flight + rand.nextInt(6) + rand.nextInt(7));
            m.load(tag);
            m.setDeltaMovement(rand.nextGaussian() * 0.001D, 0.05D, rand.nextGaussian() * 0.001D);
        }
    }

    protected EntityFireworkRocket(net.minecraft.world.entity.Entity modern) {
        super(modern);
    }

    protected net.minecraft.world.entity.projectile.FireworkRocketEntity mcc$self() {
        return !mcc$hosted() && mcc$entity instanceof net.minecraft.world.entity.projectile.FireworkRocketEntity m ? m : null;
    }

    @Override
    protected void entityInit() {
    }

    /** Whether it boosts an elytra flyer (the modern rocket's own check). */
    public boolean isAttachedToEntity() {
        var m = mcc$self();
        if (m == null) return false;
        try {
            var method = net.minecraft.world.entity.projectile.FireworkRocketEntity.class.getDeclaredMethod("isAttachedToEntity");
            method.setAccessible(true);
            return (Boolean) method.invoke(m);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return false;
        }
    }

    @Override
    protected void readEntityFromNBT(legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound tag) {
    }

    @Override
    protected void writeEntityToNBT(legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound tag) {
    }
}
