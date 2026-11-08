package legacyconnector.vb1_7_3.net.minecraft.entity;

import legacyconnector.vb1_7_3.net.minecraft.item.ItemStack;
import legacyconnector.vb1_7_3.net.minecraft.world.World;

/** Beta 1.7.3 {@code EntityLiving}: health and movement input. */
public abstract class LivingEntity extends Entity {
    public int maxHealth = 20;
    public float bodyYaw;
    public float lastBodyYaw;
    public float lastWalkAnimationSpeed,walkAnimationSpeed,walkAnimationProgress;
    public int health = 10;
    public int lastHealth;
    public int hurtTime;
    public int damagedTime;
    public int deathTime;
    public int attackCooldown;
    public boolean killedByOtherEntity;
    public float rotationSpeed,swingAnimationProgress;
    public int scoreAmount,despawnCounter;
    protected boolean interpolateOnly;
    protected float sidewaysSpeed;
    protected float forwardSpeed;
    protected boolean jumping;
    protected float movementSpeed = 0.7F;
    protected String texture = "/mob/char.png";

    public LivingEntity(World world) {
        super(world);stepHeight=.5F;
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
    protected Entity lookTarget;
    protected int lookTimer;
    public boolean hasLookTarget(){return lookTarget!=null;}
    public Entity getLookTarget(){return lookTarget;}
    public final void mcc$idleLook(){
        sidewaysSpeed=0;forwardSpeed=0;
        if(random.nextFloat()<.02F){var player=world.getClosestPlayer(this,8);if(player!=null){lookTarget=player;lookTimer=10+random.nextInt(20);}else rotationSpeed=(random.nextFloat()-.5F)*20;}
        if(lookTarget!=null){lookAt(lookTarget,10,40);if(lookTimer--<=0||lookTarget.dead||getSquaredDistance(lookTarget)>64)lookTarget=null;}
        else{if(random.nextFloat()<.05F)rotationSpeed=(random.nextFloat()-.5F)*20;yaw+=rotationSpeed;pitch=0;}
        if(isSubmergedInWater()||isTouchingLava())jumping=random.nextFloat()<.8F;
    }
    protected void applyDamage(int amount){
        health-=amount;
        if(mcc$modern instanceof net.minecraft.world.entity.LivingEntity living&&!(mcc$modern instanceof org.kylepitcock.legacyconnector.vb1_7_3.B173LivingHost))living.setHealth(health);
    }
    public boolean canSee(Entity other) {
        return other!=null && other.mcc$modern!=null && mcc$modern instanceof net.minecraft.world.entity.LivingEntity nativeLiving && nativeLiving.hasLineOfSight(other.mcc$modern);
    }
    public boolean canSpawn(){return mcc$modern instanceof org.kylepitcock.legacyconnector.vb1_7_3.B173LivingHost host?host.baseCanSpawn():mcc$modern instanceof org.kylepitcock.legacyconnector.vb1_7_3.LegacyB173Cow cow&&cow.baseCanSpawn();}
    protected boolean canDespawn(){return !isPersistent;}
    public final boolean mcc$canDespawn(){return canDespawn();}
    protected void tryDespawn(){
        if(world==null)return;
        var player=world.getClosestPlayer(this,-1D);
        if(!canDespawn()||player==null)return;
        if(mcc$modern instanceof org.kylepitcock.legacyconnector.vb1_7_3.B173LivingHost host&&!host.baseCanDespawn())return;
        if(mcc$modern instanceof org.kylepitcock.legacyconnector.vb1_7_3.LegacyB173Cow cow&&!cow.baseCanDespawn())return;
        double dx=player.x-x,dy=player.y-y,dz=player.z-z;
        double distance=dx*dx+dy*dy+dz*dz;
        if(distance>16384D)markDead();
        if(despawnCounter>600&&random.nextInt(800)==0){
            if(distance<1024D)despawnCounter=0;
            else markDead();
        }
    }
    public final void mcc$tryDespawn(){tryDespawn();}
    protected String getRandomSound(){return null;}
    protected String getHurtSound(){return null;}
    protected String getDeathSound(){return null;}
    public final String mcc$randomSound(){return getRandomSound();}
    public final String mcc$hurtSound(){return getHurtSound();}
    public final String mcc$deathSound(){return getDeathSound();}
    @Override public boolean isAlive(){return !dead&&health>0;}
    @Override public boolean isPushable(){return !dead;}
    @Override public boolean isCollidable(){return !dead;}
    public boolean interact(legacyconnector.vb1_7_3.net.minecraft.entity.player.PlayerEntity player){return false;}
    public void onKilledBy(Entity killer) {
        if(scoreAmount>=0&&killer!=null)killer.updateKilledAchievement(this,scoreAmount);
        if(killer!=null)killer.onKilledOther(this);
        killedByOtherEntity=true;
        if(world!=null&&!world.isRemote)dropItems();
    }
    protected void dropItems(){int id=getDroppedItemId();if(id>0){int amount=random.nextInt(3);for(int i=0;i<amount;i++)dropItem(id,1);}}
    protected int getDroppedItemId(){return 0;}
    protected float getSoundVolume(){return 1F;}
    public final float mcc$soundVolume(){return getSoundVolume();}
    public final void mcc$dropItems(){dropItems();}
    public int getMinAmbientSoundDelay(){return 80;}
    public boolean canBreatheInWater(){return false;}
    /** Beta {@code spawnExplosionParticle}-style spawn animation (puffs of smoke); the host spawns no extra particles. */
    public void animateSpawn(){}
    public String getTexture(){return texture;}
    public void travel(float sideways,float forward){if(mcc$modern instanceof org.kylepitcock.legacyconnector.vb1_7_3.B173LivingHost host)host.baseTravel(sideways,forward);}
    public boolean isOnLadder(){return mcc$modern instanceof org.kylepitcock.legacyconnector.vb1_7_3.B173LivingHost host?host.baseIsOnLadder():mcc$modern instanceof net.minecraft.world.entity.LivingEntity living&&living.onClimbable();}
    protected boolean isImmobile(){return health<=0;}
    public final boolean mcc$isImmobile(){return isImmobile();}
    protected void jump(){velocityY=.42;}
    public void heal(int amount){if(health>0){health=Math.min(maxHealth,health+amount);damagedTime=20;}}
    public void processServerEntityStatus(byte status){if(mcc$modern instanceof org.kylepitcock.legacyconnector.vb1_7_3.B173LivingHost host)host.baseEntityStatus(status);else if(status==2){hurtTime=10;damagedTime=10;}else if(status==3){health=0;deathTime=1;}}

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
    public String mcc$texture(){return getTexture();}
    public void mcc$setTexture(String texture){this.texture=texture;}
    public float getHandSwingProgress(float partial){return mcc$modern instanceof net.minecraft.world.entity.LivingEntity living?living.getAttackAnim(partial):0;}

    public ItemStack getHeldItem() {
        if(mcc$modern instanceof org.kylepitcock.legacyconnector.vb1_7_3.B173LivingHost)return null;
        return mcc$modern instanceof net.minecraft.world.entity.LivingEntity living
                ?org.kylepitcock.legacyconnector.vb1_7_3.StacksB173.toLegacy(living.getMainHandItem()):null;
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
