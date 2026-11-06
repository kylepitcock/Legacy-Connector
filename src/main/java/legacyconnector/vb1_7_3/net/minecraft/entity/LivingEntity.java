package legacyconnector.vb1_7_3.net.minecraft.entity;

import legacyconnector.vb1_7_3.net.minecraft.item.ItemStack;
import legacyconnector.vb1_7_3.net.minecraft.world.World;

/** Beta 1.7.3 {@code EntityLiving}: health and movement input. */
public abstract class LivingEntity extends Entity {
    public int maxHealth = 20;
    public float bodyYaw;
    public float lastBodyYaw;
    public int health = 10;
    public int lastHealth;
    public int hurtTime;
    public int damagedTime;
    public int deathTime;
    public int attackCooldown;
    protected float sidewaysSpeed;
    protected float forwardSpeed;
    protected boolean jumping;
    protected float movementSpeed = 0.7F;
    protected String texture = "/mob/char.png";

    public LivingEntity(World world) {
        super(world);
    }

    public ItemStack getHeldItem() {
        return null;
    }

    @Override
    public float getEyeHeight() {
        return height * 0.85F;
    }
}
