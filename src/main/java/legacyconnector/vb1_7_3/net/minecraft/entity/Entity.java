package legacyconnector.vb1_7_3.net.minecraft.entity;

import java.util.Random;

import legacyconnector.vb1_7_3.net.minecraft.world.World;

/**
 * Beta 1.7.3 {@code Entity}: position, motion and rotation fields. Supported original entities retain this object as their
 * native host's stable callback view. Ordinary native entities receive temporary synchronized views for original callbacks.
 */
public abstract class Entity {
    private static int nextId;

    public int id = nextId++;
    public double renderDistanceMultiplier = 1.0;
    public boolean blocksSameBlockSpawning;
    public Entity passenger;
    public Entity vehicle;
    public World world;
    public double prevX;
    public double prevY;
    public double prevZ;
    public double x;
    public double y;
    public double z;
    public double velocityX;
    public double velocityY;
    public double velocityZ;
    public float yaw;
    public float pitch;
    public float prevYaw;
    public float prevPitch;
    public boolean onGround;
    public boolean horizontalCollision;
    public boolean verticalCollision;
    public boolean hasCollided;
    public boolean velocityModified;
    public boolean slowed;
    public boolean keepVelocityOnCollision = true;
    public boolean dead;
    public float standingEyeHeight;
    public float width = 0.6F;
    public float height = 1.8F;
    public float prevHorizontalSpeed;
    public float horizontalSpeed;
    protected float fallDistance;
    public double lastTickX;
    public double lastTickY;
    public double lastTickZ;
    public float cameraOffset;
    public float stepHeight;
    public boolean noClip;
    public float pushSpeedReduction;
    protected Random random = new Random();
    public int age;
    public int fireImmunityTicks = 1;
    public int fireTicks;
    protected int maxAir = 300;
    protected boolean submergedInWater;
    public int hearts;
    public int air = 300;
    public String skinUrl;
    public String capeUrl;
    protected boolean fireImmune;
    public final boolean mcc$fireImmune(){return fireImmune;}
    public float minBrightness;
    public boolean isPersistent;
    public int chunkX;
    public int chunkSlice;
    public int chunkZ;
    public int trackedPosX;
    public int trackedPosY;
    public int trackedPosZ;
    public boolean ignoreFrustumCull;

    /** The modern entity this one is a view of, or null for a mod's own entity. */
    public net.minecraft.world.entity.Entity mcc$modern;
    public legacyconnector.vb1_7_3.net.minecraft.util.math.Box boundingBox;

    /** The entity's data slots, created before {@link #initDataTracker()} runs. */
    public legacyconnector.vb1_7_3.net.minecraft.entity.data.DataTracker dataTracker = new legacyconnector.vb1_7_3.net.minecraft.entity.data.DataTracker();
    public legacyconnector.vb1_7_3.net.minecraft.entity.data.DataTracker getDataTracker() { return dataTracker; }

    public Entity(World world) {
        this.world = world;
        setPosition(0.0, 0.0, 0.0);
        initDataTracker();
    }
    protected void initDataTracker() {}

    /** Beta {@code onStruckByLightning}: fire damage; the host's own lightning handling applies the native effects. */
    public void onStruckByLightning(LightningEntity lightning) {}

    public void markDead() {
        this.dead = true;
        if (mcc$modern instanceof org.kylepitcock.legacyconnector.vb1_7_3.B173EntityHost host) host.baseMarkDead();
        else if(mcc$modern!=null)mcc$modern.discard();
    }

    public void setPosition(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
        boundingBox = legacyconnector.vb1_7_3.net.minecraft.util.math.Box.create(x-width/2, y-standingEyeHeight, z-width/2,
                x+width/2, y-standingEyeHeight+height, z+width/2);
    }

    public void tick() {if(mcc$modern instanceof org.kylepitcock.legacyconnector.vb1_7_3.B173EntityHost host)host.mcc$baseTick();}
    protected void damage(int amount){if(!fireImmune)damage(null,amount);}
    protected void setOnFire(){if(!fireImmune){damage(null,4);fireTicks=600;}}
    protected void scheduleVelocityUpdate(){velocityModified=true;if(mcc$modern!=null)mcc$modern.hurtMarked=true;}
    protected void fall(double deltaY,boolean landed){
        if(mcc$modern instanceof org.kylepitcock.legacyconnector.vb1_7_3.B173LivingHost host){host.baseFall(deltaY,landed);return;}
        if(landed){if(fallDistance>0){onLanding(fallDistance);fallDistance=0;}}else if(deltaY<0)fallDistance-=deltaY;
    }
    public final void mcc$fall(double deltaY,boolean landed){fall(deltaY,landed);}
    public final float mcc$fallDistance(){return fallDistance;}
    public final void mcc$fallDistance(float distance){fallDistance=distance;}
    public double getSquaredDistance(Entity entity){return getSquaredDistance(entity.x,entity.y,entity.z);}
    public void move(double dx,double dy,double dz){
        if(mcc$modern instanceof org.kylepitcock.legacyconnector.vb1_7_3.B173EntityHost host)host.baseMove(dx,dy,dz);
        else if(mcc$modern!=null){mcc$modern.move(net.minecraft.world.entity.MoverType.SELF,new net.minecraft.world.phys.Vec3(dx,dy,dz));setPosition(mcc$modern.getX(),mcc$modern.getY()+standingEyeHeight,mcc$modern.getZ());}
        else setPosition(x+dx,y+dy,z+dz);
    }
    public void moveNonSolid(float sideways,float forward,float speed){
        float length=sideways*sideways+forward*forward;if(length<.01F)return;
        length=(float)Math.sqrt(length);if(length<1)length=1;length=speed/length;sideways*=length;forward*=length;
        float sin=(float)Math.sin(yaw*Math.PI/180),cos=(float)Math.cos(yaw*Math.PI/180);
        velocityX+=sideways*cos-forward*sin;velocityZ+=forward*cos+sideways*sin;
    }
    public void setRotation(float yaw,float pitch){
        this.yaw=yaw%360;this.pitch=pitch%360;
        if(mcc$modern instanceof org.kylepitcock.legacyconnector.vb1_7_3.B173LivingHost host)host.baseSetRotation(this.yaw,this.pitch);
    }
    public boolean isInsideWall(){
        if(mcc$modern instanceof org.kylepitcock.legacyconnector.vb1_7_3.B173LivingHost host)return host.baseInsideWall();
        return mcc$modern!=null&&mcc$modern.isInWall();
    }
    public boolean isTouchingLava(){return mcc$modern!=null&&mcc$modern.isInLava();}
    public boolean isPushable(){return false;}
    public boolean hasVehicle(){return vehicle!=null;}
    public boolean getEntitiesInside(double dx,double dy,double dz){
        if(world==null||boundingBox==null)return false;var box=boundingBox.offset(dx,dy,dz);
        return world.getEntityCollisions(this,box).isEmpty()&&!world.isBoxSubmergedInFluid(box);
    }
    public void onCollision(Entity other){
        if(other==null||other==vehicle||other==passenger)return;
        double dx=other.x-x,dz=other.z-z,d=Math.max(Math.abs(dx),Math.abs(dz));if(d<.01)return;
        d=Math.sqrt(d);dx/=d;dz/=d;double factor=Math.min(1,1/d)*.05*(1-pushSpeedReduction);dx*=factor;dz*=factor;
        velocityX-=dx;velocityZ-=dz;other.velocityX+=dx;other.velocityZ+=dz;velocityModified=other.velocityModified=true;
    }
    public void lookAt(Entity other,float yawLimit,float pitchLimit){
        double dx=other.x-x,dz=other.z-z,dy=other.y+other.getEyeHeight()-(y+getEyeHeight());
        yaw=turn(yaw,(float)(Math.atan2(dz,dx)*180/Math.PI)-90,yawLimit);
        pitch=turn(pitch,(float)(-Math.atan2(dy,Math.sqrt(dx*dx+dz*dz))*180/Math.PI),pitchLimit);
    }
    private static float turn(float angle,float target,float max){float delta=(target-angle)%360;if(delta>=180)delta-=360;if(delta< -180)delta+=360;return angle+Math.max(-max,Math.min(max,delta));}
    /** Vanilla Entity's hooks are empty; native death accounting handles player scores. */
    public void updateKilledAchievement(Entity killed,int score){}
    public void onKilledOther(LivingEntity killed){}

    public void setBoundingBoxSpacing(float width, float height) {
        this.width=width; this.height=height; setPosition(x,y,z);
    }
    public boolean isCollidable() { return !(mcc$modern instanceof org.kylepitcock.legacyconnector.vb1_7_3.B173EntityHost)&&mcc$modern != null && mcc$modern.isPickable(); }
    public boolean shouldRender(double squaredDistance){
        double distance=((boundingBox.maxX-boundingBox.minX)+(boundingBox.maxY-boundingBox.minY)+(boundingBox.maxZ-boundingBox.minZ))/3*64*renderDistanceMultiplier;
        return squaredDistance<distance*distance;
    }
    /** Biny names this radius, but original sn.x_ is the vertical shadow origin offset. */
    public float getShadowRadius(){return height/2;}
    public float getTargetingMargin(){return .1F;}
    public void setVelocityClient(double x,double y,double z){velocityX=x;velocityY=y;velocityZ=z;if(mcc$modern!=null)mcc$modern.setDeltaMovement(x,y,z);}
    /** Native water contact is refreshed at the host's base-tick boundary, as Beta cached its fluid collision result. */
    public final void mcc$environment(boolean inWater){submergedInWater=inWater;}
    public boolean checkWaterCollisions() { return mcc$modern != null && mcc$modern.isInWater(); }
    public boolean isSubmergedInWater() { return submergedInWater; }
    public boolean damage(Entity attacker, int amount) {
        if (mcc$modern instanceof org.kylepitcock.legacyconnector.vb1_7_3.B173EntityHost host) return host.baseDamage(attacker, amount);
        if (mcc$modern == null) return false;
        var nativeAttacker = attacker == null ? null : attacker.mcc$modern;
        var source = nativeAttacker instanceof net.minecraft.world.entity.player.Player p
                ? mcc$modern.damageSources().playerAttack(p)
                : nativeAttacker instanceof net.minecraft.world.entity.LivingEntity living
                        ? mcc$modern.damageSources().mobAttack(living) : mcc$modern.damageSources().generic();
        boolean damaged = mcc$modern.hurt(source, amount);
        // Original callbacks can inspect health immediately after damage. Preserve their pending motion writes.
        if (this instanceof LivingEntity view && mcc$modern instanceof net.minecraft.world.entity.LivingEntity living)
            view.health = Math.round(living.getHealth());
        return damaged;
    }
    public void onPlayerInteraction(legacyconnector.vb1_7_3.net.minecraft.entity.player.PlayerEntity player) {}
    public void writeNbt(legacyconnector.vb1_7_3.net.minecraft.nbt.NbtCompound tag) {
        tag.putFloat("FallDistance", fallDistance);tag.putShort("Fire", (short)fireTicks);
        tag.putShort("Air", (short)air);tag.putBoolean("OnGround", onGround);
    }
    public void readNbt(legacyconnector.vb1_7_3.net.minecraft.nbt.NbtCompound tag) {
        if(tag.contains("FallDistance"))fallDistance=tag.getFloat("FallDistance");
        if(tag.contains("Fire"))fireTicks=tag.getShort("Fire");
        if(tag.contains("Air"))air=tag.getShort("Air");if(tag.contains("OnGround"))onGround=tag.getBoolean("OnGround");
    }

    /** Beta mount toggles when called again with the current vehicle. */
    public void setVehicle(Entity next) {
        if (next == this) return;
        if (next == vehicle) next = null;
        Entity previous=vehicle;
        if (mcc$modern != null) {
            if (next == null) mcc$modern.stopRiding();
            else if (next.mcc$modern == null || !mcc$modern.startRiding(next.mcc$modern, true)) return;
        }
        if (previous != null && previous.passenger == this) previous.passenger=null;
        vehicle=next;if(next!=null)next.passenger=this;
    }
    public double getStandingEyeHeight(){return standingEyeHeight;}
    public double getPassengerRidingHeight(){return height*.75D;}
    public void updatePassengerPosition(){if(passenger!=null)passenger.setPosition(x,y+getPassengerRidingHeight()+passenger.getStandingEyeHeight(),z);}
    protected void onLanding(float distance) {
        if(mcc$modern instanceof org.kylepitcock.legacyconnector.vb1_7_3.B173LivingHost host)host.baseOnLanding(distance);
    }
    public final void mcc$onLanding(float distance){onLanding(distance);}
    public float getBrightnessAtEyes(float partial) {
        return world==null||world.mcc$level()==null?0:world.mcc$access().getMaxLocalRawBrightness(net.minecraft.core.BlockPos.containing(x,y+getEyeHeight(),z))/15F;
    }
    public legacyconnector.vb1_7_3.net.minecraft.entity.ItemEntity dropItem(int id,int count) {
        if(count<=0||world==null)return null;
        var item=new legacyconnector.vb1_7_3.net.minecraft.entity.ItemEntity(world,x,y,z,new legacyconnector.vb1_7_3.net.minecraft.item.ItemStack(id,count,0));
        item.pickupDelay=10;return world.spawnEntity(item)?item:null;
    }
    public legacyconnector.vb1_7_3.net.minecraft.entity.ItemEntity dropItem(legacyconnector.vb1_7_3.net.minecraft.item.ItemStack stack,float offset){
        if(stack==null||stack.count<=0||world==null)return null;
        var item=new legacyconnector.vb1_7_3.net.minecraft.entity.ItemEntity(world,x,y+offset,z,stack);item.pickupDelay=10;
        return world.spawnEntity(item)?item:null;
    }

    public float getEyeHeight() {
        return 0.0F;
    }

    public void setWorld(World world) {
        this.world = world;
    }

    public void setPositionAndAngles(double x, double y, double z, float yaw, float pitch) {
        setPosition(x,y,z);
        this.prevX = this.x = x;
        this.prevY = this.y = y;
        this.prevZ = this.z = z;
        this.prevYaw = this.yaw = yaw;
        this.prevPitch = this.pitch = pitch;
        if(mcc$modern instanceof org.kylepitcock.legacyconnector.vb1_7_3.B173LivingHost host)host.baseSetPositionAndAngles(x,y,z,yaw,pitch);
    }

    /** Beta's network placement callback; living hosts use native interpolation as the superclass operation. */
    public void setPositionAndAnglesAvoidEntities(double x,double y,double z,float yaw,float pitch,int steps){
        if(mcc$modern instanceof org.kylepitcock.legacyconnector.vb1_7_3.B173LivingHost host){host.baseInterpolate(x,y,z,yaw,pitch,steps);return;}
        setPosition(x,y,z);setRotation(yaw,pitch);
        if(world!=null){double top=boundingBox.minY;for(var box:world.getEntityCollisions(this,boundingBox.expand(-.03125,0,-.03125)))top=Math.max(top,box.maxY);if(top>boundingBox.minY)setPosition(x,y+top-boundingBox.minY,z);}
    }

    public void setPositionAndAnglesKeepPrevAngles(double x, double y, double z, float yaw, float pitch) {
        setPosition(x,y,z);
        this.prevX = this.x = x;
        this.prevY = this.y = y;
        this.prevZ = this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public float getDistance(Entity other) {
        double dx = x - other.x, dy = y - other.y, dz = z - other.z;
        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public double getDistance(double px, double py, double pz) {
        double dx = x - px, dy = y - py, dz = z - pz;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public double getSquaredDistance(double px, double py, double pz) {
        double dx = x - px, dy = y - py, dz = z - pz;
        return dx * dx + dy * dy + dz * dz;
    }

    public boolean isSneaking() {
        return false;
    }

    public boolean isAlive() {
        return !dead;
    }

    /** MCP {@code getLookVec}: none for a plain entity. */
    public legacyconnector.vb1_7_3.net.minecraft.util.math.Vec3d getLookVector() {
        return null;
    }
}
