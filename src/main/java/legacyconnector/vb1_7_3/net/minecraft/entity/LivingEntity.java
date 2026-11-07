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
    public boolean killedByOtherEntity;
    protected float sidewaysSpeed;
    protected float forwardSpeed;
    protected boolean jumping;
    protected float movementSpeed = 0.7F;
    protected String texture = "/mob/char.png";

    public LivingEntity(World world) {
        super(world);
    }

    @Override public void tick() {
        if(mcc$modern instanceof org.kylepitcock.legacyconnector.vb1_7_3.B173LivingHost host)host.mcc$baseTick();
        else super.tick();
    }
    public void tickMovement() {
        if(mcc$modern instanceof org.kylepitcock.legacyconnector.vb1_7_3.B173LivingHost host)host.baseMovement();
    }
    protected void tickLiving() {
        if(mcc$modern instanceof org.kylepitcock.legacyconnector.vb1_7_3.B173LivingHost host)host.baseAI();
    }
    public final void mcc$tickLiving(){tickLiving();}
    public float mcc$sidewaysSpeed(){return sidewaysSpeed;}
    public float mcc$forwardSpeed(){return forwardSpeed;}
    public float mcc$movementSpeed(){return movementSpeed;}
    public boolean mcc$jumping(){return jumping;}
    public void mcc$movementInputs(float sideways,float forward,boolean jump){sidewaysSpeed=sideways;forwardSpeed=forward;jumping=jump;}
    public void applyKnockback(Entity attacker,int amount,double x,double z) {
        if(mcc$modern instanceof org.kylepitcock.legacyconnector.vb1_7_3.B173LivingHost host)host.baseKnockback(attacker,amount,x,z);
        else {double length=Math.sqrt(x*x+z*z);if(length>0){velocityX=velocityX/2-x/length*.4;velocityY=Math.min(velocityY/2+.4,.4);velocityZ=velocityZ/2-z/length*.4;}}
    }
    public boolean canSee(Entity other) {
        return other!=null && other.mcc$modern!=null && mcc$modern instanceof net.minecraft.world.entity.LivingEntity nativeLiving && nativeLiving.hasLineOfSight(other.mcc$modern);
    }
    public boolean canSpawn(){return mcc$modern instanceof org.kylepitcock.legacyconnector.vb1_7_3.B173LivingHost host && host.baseCanSpawn();}
    protected boolean canDespawn(){return !isPersistent;}
    public final boolean mcc$canDespawn(){return canDespawn();}
    protected String getRandomSound(){return null;}
    protected String getHurtSound(){return null;}
    protected String getDeathSound(){return null;}
    public final String mcc$randomSound(){return getRandomSound();}
    public final String mcc$hurtSound(){return getHurtSound();}
    public final String mcc$deathSound(){return getDeathSound();}
    @Override public boolean isAlive(){return !dead&&health>0;}
    public boolean interact(legacyconnector.vb1_7_3.net.minecraft.entity.player.PlayerEntity player){return false;}
    public void onKilledBy(Entity killer) {}
    public int getLimitPerChunk(){return 4;}
    @Override public void writeNbt(legacyconnector.vb1_7_3.net.minecraft.nbt.NbtCompound tag){
        super.writeNbt(tag);tag.putShort("Health",(short)health);tag.putShort("HurtTime",(short)hurtTime);
        tag.putShort("DeathTime",(short)deathTime);tag.putShort("AttackTime",(short)attackCooldown);
    }
    @Override public void readNbt(legacyconnector.vb1_7_3.net.minecraft.nbt.NbtCompound tag){
        super.readNbt(tag);if(tag.contains("Health"))health=tag.getShort("Health");
        hurtTime=tag.getShort("HurtTime");deathTime=tag.getShort("DeathTime");attackCooldown=tag.getShort("AttackTime");
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
