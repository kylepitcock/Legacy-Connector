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

    /** The original skin path, also used by the native mob fallback renderer. */
    public String mcc$texture(){return texture;}

    public ItemStack getHeldItem() {
        return null;
    }

    @Override
    public float getEyeHeight() {
        return height * 0.85F;
    }

    /** MCP {@code getLook}: the unit vector the entity looks along. */
    public legacyconnector.vb1_7_3.net.minecraft.util.math.Vec3d getLookVector(float partialTicks) {
        float yawRad = -yaw * 0.017453292F - (float) Math.PI;
        float pitchRad = -pitch * 0.017453292F;
        float cosYaw = (float) Math.cos(yawRad), sinYaw = (float) Math.sin(yawRad);
        float cosPitch = (float) -Math.cos(pitchRad), sinPitch = (float) Math.sin(pitchRad);
        return legacyconnector.vb1_7_3.net.minecraft.util.math.Vec3d.createCached(sinYaw * cosPitch, sinPitch, cosYaw * cosPitch);
    }

    @Override
    public legacyconnector.vb1_7_3.net.minecraft.util.math.Vec3d getLookVector() {
        return getLookVector(1.0F);
    }
}
