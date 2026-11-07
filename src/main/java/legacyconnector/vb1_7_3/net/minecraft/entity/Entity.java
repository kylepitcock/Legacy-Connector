package legacyconnector.vb1_7_3.net.minecraft.entity;

import java.util.Random;

import legacyconnector.vb1_7_3.net.minecraft.world.World;

/**
 * Beta 1.7.3 {@code Entity}: position, motion and rotation fields. Mod entities are not hosted in the modern world yet; the layer's own
 * subclasses are views of modern entities (players in block callbacks), whose fields it copies before a callback. The bounding box and
 * data tracker (synthesized types) are not declared here.
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

    public Entity(World world) {
        this.world = world;
        setPosition(0.0, 0.0, 0.0);
    }

    public void markDead() {
        this.dead = true;
        if (mcc$modern instanceof org.kylepitcock.legacyconnector.vb1_7_3.B173LivingHost host) host.baseMarkDead();
    }

    public void setPosition(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
        boundingBox = legacyconnector.vb1_7_3.net.minecraft.util.math.Box.create(x-width/2, y-standingEyeHeight, z-width/2,
                x+width/2, y-standingEyeHeight+height, z+width/2);
    }

    public void tick() {}

    public void setBoundingBoxSpacing(float width, float height) {
        this.width=width; this.height=height; setPosition(x,y,z);
    }
    public boolean isCollidable() { return mcc$modern != null && mcc$modern.isPickable(); }
    public boolean checkWaterCollisions() { return mcc$modern != null && mcc$modern.level().getFluidState(mcc$modern.blockPosition()).is(net.minecraft.tags.FluidTags.WATER); }
    public boolean isSubmergedInWater() { return checkWaterCollisions(); }
    public boolean damage(Entity attacker, int amount) {
        if (mcc$modern instanceof org.kylepitcock.legacyconnector.vb1_7_3.B173LivingHost host) return host.baseDamage(attacker, amount);
        if (mcc$modern == null) return false;
        var source = attacker != null && attacker.mcc$modern instanceof net.minecraft.world.entity.player.Player p
                ? mcc$modern.damageSources().playerAttack(p) : mcc$modern.damageSources().generic();
        return mcc$modern.hurt(source, amount);
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
    protected void onLanding(float distance) {
        if(mcc$modern instanceof org.kylepitcock.legacyconnector.vb1_7_3.B173LivingHost host)host.baseOnLanding(distance);
    }
    public final void mcc$onLanding(float distance){onLanding(distance);}
    public float getBrightnessAtEyes(float partial) {
        return world==null||world.mcc$level()==null?0:world.mcc$level().getMaxLocalRawBrightness(net.minecraft.core.BlockPos.containing(x,y+getEyeHeight(),z))/15F;
    }
    public legacyconnector.vb1_7_3.net.minecraft.entity.ItemEntity dropItem(int id,int count) {
        if(count<=0||world==null)return null;
        var item=new legacyconnector.vb1_7_3.net.minecraft.entity.ItemEntity(world,x,y,z,new legacyconnector.vb1_7_3.net.minecraft.item.ItemStack(id,count,0));
        item.pickupDelay=10;return world.spawnEntity(item)?item:null;
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
