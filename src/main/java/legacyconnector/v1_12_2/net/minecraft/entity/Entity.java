package legacyconnector.v1_12_2.net.minecraft.entity;

import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.math.AxisAlignedBB;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.util.math.Vec3d;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import legacyconnector.v1_12_2.net.minecraftforge.common.capabilities.Capability;
import legacyconnector.v1_12_2.net.minecraftforge.common.capabilities.ICapabilityProvider;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122;

/**
 * 1.12.2 entity. For an entity in the modern world this is a view over the modern entity: the public legacy fields (posX, motionY,
 * world, ...) are phantoms whose accesses in mod code the loader redirects to the {@code mcc$get$}/{@code mcc$set$} accessors below,
 * which read and write the modern entity, or a local copy while the entity is not bound yet (a mod entity built before it spawns).
 *
 * <p>Mod entity classes are run by a modern host ({@link org.kylepitcock.legacyconnector.v1_12_2.entity.Host1122}): the host calls
 * the legacy hooks (onUpdate, attackEntityFrom, ...), and the base implementations here hand the call back to the host's vanilla
 * behaviour. Exact vanilla classes a mod constructs ({@code new EntityItem(world, x, y, z, stack)}) are views of a new modern entity
 * of that type.</p>
 */
public abstract class Entity implements legacyconnector.v1_12_2.net.minecraft.command.ICommandSender, ICapabilityProvider {
    /** Phantom field: {@link #mcc$get$world}. */
    @Deprecated
    public World world;
    private World mcc$local_world;
    @Deprecated
    public double posX;
    private double mcc$local_posX;
    @Deprecated
    public double posY;
    private double mcc$local_posY;
    @Deprecated
    public double posZ;
    private double mcc$local_posZ;
    @Deprecated
    public double prevPosX;
    private double mcc$local_prevPosX;
    @Deprecated
    public double prevPosY;
    private double mcc$local_prevPosY;
    @Deprecated
    public double prevPosZ;
    private double mcc$local_prevPosZ;
    @Deprecated
    public double lastTickPosX;
    private double mcc$local_lastTickPosX;
    @Deprecated
    public double lastTickPosY;
    private double mcc$local_lastTickPosY;
    @Deprecated
    public double lastTickPosZ;
    private double mcc$local_lastTickPosZ;
    @Deprecated
    public double motionX;
    private double mcc$local_motionX;
    @Deprecated
    public double motionY;
    private double mcc$local_motionY;
    @Deprecated
    public double motionZ;
    private double mcc$local_motionZ;
    @Deprecated
    public float rotationYaw;
    private float mcc$local_rotationYaw;
    @Deprecated
    public float rotationPitch;
    private float mcc$local_rotationPitch;
    @Deprecated
    public float prevRotationYaw;
    private float mcc$local_prevRotationYaw;
    @Deprecated
    public float prevRotationPitch;
    private float mcc$local_prevRotationPitch;
    @Deprecated
    public boolean onGround;
    private boolean mcc$local_onGround;
    @Deprecated
    public boolean isDead;
    private boolean mcc$local_isDead;
    @Deprecated
    public float width;
    private float mcc$local_width = 0.6F;
    @Deprecated
    public float height;
    private float mcc$local_height = 1.8F;
    @Deprecated
    public int ticksExisted;
    private int mcc$local_ticksExisted;
    @Deprecated
    public float fallDistance;
    private float mcc$local_fallDistance;
    @Deprecated
    public int dimension;
    private int mcc$local_dimension;
    @Deprecated
    public int hurtResistantTime;
    private int mcc$local_hurtResistantTime;
    @Deprecated
    public boolean noClip;
    private boolean mcc$local_noClip;
    @Deprecated
    public float stepHeight;
    private float mcc$local_stepHeight = 0.6F;
    @Deprecated
    public boolean collidedHorizontally;
    private boolean mcc$local_collidedHorizontally;
    @Deprecated
    public boolean collidedVertically;
    private boolean mcc$local_collidedVertically;
    @Deprecated
    public boolean collided;
    @Deprecated
    public boolean velocityChanged;
    private boolean mcc$local_velocityChanged;
    @Deprecated
    public boolean isAirBorne;
    private boolean mcc$local_isAirBorne;
    @Deprecated
    public int timeUntilPortal;
    private int mcc$local_timeUntilPortal;
    @Deprecated
    public boolean inWater;
    private boolean mcc$local_inWater;
    @Deprecated
    public boolean isImmuneToFire;
    private boolean mcc$local_isImmuneToFire;
    @Deprecated
    public float distanceWalkedModified;
    private float mcc$local_distanceWalkedModified;
    @Deprecated
    public float prevDistanceWalkedModified;
    private float mcc$local_prevDistanceWalkedModified;
    @Deprecated
    public java.util.UUID entityUniqueID;
    private java.util.UUID mcc$local_entityUniqueID = java.util.UUID.randomUUID();

    /** Every entity's own random, as in 1.12 (mods roll it for particles and drops). */
    protected java.util.Random rand = new java.util.Random();
    /** Whether the renderer skips the view-frustum check (big entities). */
    public boolean ignoreFrustumCheck;
    /** True until the first tick ran. */
    protected boolean firstUpdate = true;
    /** Blocks are not placed into this entity's box (boats, carts, living entities). */
    public boolean preventEntitySpawning;
    /** Spawns even outside the players' loaded area. */
    public boolean forceSpawn;
    public float entityCollisionReduction;
    protected boolean isInWeb;
    public boolean addedToChunk;
    public int chunkCoordX;
    public int chunkCoordY;
    public int chunkCoordZ;
    public long serverPosX;
    public long serverPosY;
    public long serverPosZ;
    /** Forge: while set, dropped items are collected in {@link #capturedDrops} instead of being spawned. */
    public boolean captureDrops;
    public java.util.ArrayList<legacyconnector.v1_12_2.net.minecraft.entity.item.EntityItem> capturedDrops = new java.util.ArrayList<>();

    /** The modern entity this one stands for. */
    public net.minecraft.world.entity.Entity mcc$entity;
    /**
     * The level accessor to answer {@code world} with instead of the entity's own level: spawn checks during chunk generation run
     * against the generation region (asking the real level for a chunk under generation deadlocks).
     */
    public net.minecraft.world.level.LevelAccessor mcc$worldOverride;

    protected legacyconnector.v1_12_2.net.minecraft.network.datasync.EntityDataManager dataManager;

    public Entity(World world) {
        this.mcc$local_world = world;
        this.dataManager = new legacyconnector.v1_12_2.net.minecraft.network.datasync.EntityDataManager(this);
        // a view of an existing modern entity (the bridge constructing a vanilla class through its World constructor)
        var viewed = EntityBridge1122.takeViewBinding();
        if (viewed != null) {
            this.mcc$entity = viewed;
            try {
                entityInit();
            } catch (RuntimeException | LinkageError e) {
                // a vanilla class's data keys are not needed by a view
            }
            return;
        }
        // a host constructing this legacy entity, or a new host for an entity a mod constructs itself
        var host = org.kylepitcock.legacyconnector.v1_12_2.entity.Hosts1122.takeBinding();
        // an exact vanilla class (new EntityItem(world, ...)) is a view of a real modern entity
        net.minecraft.world.entity.Entity vanilla = host == null ? org.kylepitcock.legacyconnector.v1_12_2.entity.VanillaEntities1122.create(this, world) : null;
        if (vanilla != null) EntityBridge1122.bind(this, vanilla);
        else if (host == null && world != null && !mcc$isView()) host = org.kylepitcock.legacyconnector.v1_12_2.entity.Hosts1122.adopt(this, world);
        if (host != null) {
            this.mcc$entity = host.mcc$self();
            this.mcc$hostedFlag = true;
        }
        try {
            entityInit();
        } catch (RuntimeException | LinkageError e) {
            // a mod's data registration reaching an API the layer lacks must not stop the entity from being made
            org.slf4j.LoggerFactory.getLogger("LegacyConnector/Entity").debug("entityInit of {} failed: {}", getClass().getName(), e.toString());
        }
        mcc$fireConstructing();
    }

    /** View constructor: the legacy face of an existing modern entity. */
    protected Entity(net.minecraft.world.entity.Entity modern) {
        var viewed = EntityBridge1122.takeViewBinding();
        this.mcc$entity = modern != null ? modern : viewed;
        this.dataManager = new legacyconnector.v1_12_2.net.minecraft.network.datasync.EntityDataManager(this);
    }

    /** Views of vanilla classes made through their World constructor bind to a vanilla entity, never to a host. */
    protected boolean mcc$isView() {
        return false;
    }

    /** Forge's EntityConstructing, then AttachCapabilitiesEvent (fired at the end of the base constructor). */
    public final void mcc$fireConstructing() {
        try {
            legacyconnector.v1_12_2.net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(
                    new legacyconnector.v1_12_2.net.minecraftforge.event.entity.EntityEvent.EntityConstructing(this));
        } catch (RuntimeException | LinkageError e) {
            org.slf4j.LoggerFactory.getLogger("LegacyConnector/Entities").warn("EntityConstructing handler failed for {}: {}", getClass().getName(), e.toString());
        }
        mcc$attachCapabilities();
    }

    // ------------------------------------------------------------------ phantom field accessors

    public static World mcc$get$world(Entity e) {
        var override = e.mcc$worldOverride;
        if (override instanceof net.minecraft.world.level.Level l) return org.kylepitcock.legacyconnector.v1_12_2.bridge.WorldBridge1122.wrap(l);
        var m = e.mcc$entity;
        return m != null ? org.kylepitcock.legacyconnector.v1_12_2.bridge.WorldBridge1122.wrap(m.level()) : e.mcc$local_world;
    }

    public static void mcc$set$world(Entity e, World v) {
        e.mcc$local_world = v;
    }

    public static double mcc$get$posX(Entity e) {
        var m = e.mcc$entity;
        return m != null ? m.getX() : e.mcc$local_posX;
    }

    public static void mcc$set$posX(Entity e, double v) {
        var m = e.mcc$entity;
        if (m != null) m.setPos(v, m.getY(), m.getZ());
        else e.mcc$local_posX = v;
    }

    public static double mcc$get$posY(Entity e) {
        var m = e.mcc$entity;
        return m != null ? m.getY() : e.mcc$local_posY;
    }

    public static void mcc$set$posY(Entity e, double v) {
        var m = e.mcc$entity;
        if (m != null) m.setPos(m.getX(), v, m.getZ());
        else e.mcc$local_posY = v;
    }

    public static double mcc$get$posZ(Entity e) {
        var m = e.mcc$entity;
        return m != null ? m.getZ() : e.mcc$local_posZ;
    }

    public static void mcc$set$posZ(Entity e, double v) {
        var m = e.mcc$entity;
        if (m != null) m.setPos(m.getX(), m.getY(), v);
        else e.mcc$local_posZ = v;
    }

    public static double mcc$get$prevPosX(Entity e) {
        var m = e.mcc$entity;
        return m != null ? m.xo : e.mcc$local_prevPosX;
    }

    public static void mcc$set$prevPosX(Entity e, double v) {
        var m = e.mcc$entity;
        if (m != null) m.xo = v;
        else e.mcc$local_prevPosX = v;
    }

    public static double mcc$get$prevPosY(Entity e) {
        var m = e.mcc$entity;
        return m != null ? m.yo : e.mcc$local_prevPosY;
    }

    public static void mcc$set$prevPosY(Entity e, double v) {
        var m = e.mcc$entity;
        if (m != null) m.yo = v;
        else e.mcc$local_prevPosY = v;
    }

    public static double mcc$get$prevPosZ(Entity e) {
        var m = e.mcc$entity;
        return m != null ? m.zo : e.mcc$local_prevPosZ;
    }

    public static void mcc$set$prevPosZ(Entity e, double v) {
        var m = e.mcc$entity;
        if (m != null) m.zo = v;
        else e.mcc$local_prevPosZ = v;
    }

    public static double mcc$get$lastTickPosX(Entity e) {
        var m = e.mcc$entity;
        return m != null ? m.xOld : e.mcc$local_lastTickPosX;
    }

    public static void mcc$set$lastTickPosX(Entity e, double v) {
        var m = e.mcc$entity;
        if (m != null) m.xOld = v;
        else e.mcc$local_lastTickPosX = v;
    }

    public static double mcc$get$lastTickPosY(Entity e) {
        var m = e.mcc$entity;
        return m != null ? m.yOld : e.mcc$local_lastTickPosY;
    }

    public static void mcc$set$lastTickPosY(Entity e, double v) {
        var m = e.mcc$entity;
        if (m != null) m.yOld = v;
        else e.mcc$local_lastTickPosY = v;
    }

    public static double mcc$get$lastTickPosZ(Entity e) {
        var m = e.mcc$entity;
        return m != null ? m.zOld : e.mcc$local_lastTickPosZ;
    }

    public static void mcc$set$lastTickPosZ(Entity e, double v) {
        var m = e.mcc$entity;
        if (m != null) m.zOld = v;
        else e.mcc$local_lastTickPosZ = v;
    }

    public static double mcc$get$motionX(Entity e) {
        var m = e.mcc$entity;
        return m != null ? m.getDeltaMovement().x : e.mcc$local_motionX;
    }

    public static void mcc$set$motionX(Entity e, double v) {
        var m = e.mcc$entity;
        if (m != null) {
            var d = m.getDeltaMovement();
            m.setDeltaMovement(v, d.y, d.z);
        } else {
            e.mcc$local_motionX = v;
        }
    }

    public static double mcc$get$motionY(Entity e) {
        var m = e.mcc$entity;
        return m != null ? m.getDeltaMovement().y : e.mcc$local_motionY;
    }

    public static void mcc$set$motionY(Entity e, double v) {
        var m = e.mcc$entity;
        if (m != null) {
            var d = m.getDeltaMovement();
            m.setDeltaMovement(d.x, v, d.z);
        } else {
            e.mcc$local_motionY = v;
        }
    }

    public static double mcc$get$motionZ(Entity e) {
        var m = e.mcc$entity;
        return m != null ? m.getDeltaMovement().z : e.mcc$local_motionZ;
    }

    public static void mcc$set$motionZ(Entity e, double v) {
        var m = e.mcc$entity;
        if (m != null) {
            var d = m.getDeltaMovement();
            m.setDeltaMovement(d.x, d.y, v);
        } else {
            e.mcc$local_motionZ = v;
        }
    }

    public static float mcc$get$rotationYaw(Entity e) {
        var m = e.mcc$entity;
        return m != null ? m.getYRot() : e.mcc$local_rotationYaw;
    }

    public static void mcc$set$rotationYaw(Entity e, float v) {
        var m = e.mcc$entity;
        if (m != null) m.setYRot(v);
        else e.mcc$local_rotationYaw = v;
    }

    public static float mcc$get$rotationPitch(Entity e) {
        var m = e.mcc$entity;
        return m != null ? m.getXRot() : e.mcc$local_rotationPitch;
    }

    public static void mcc$set$rotationPitch(Entity e, float v) {
        var m = e.mcc$entity;
        if (m != null) m.setXRot(v);
        else e.mcc$local_rotationPitch = v;
    }

    public static float mcc$get$prevRotationYaw(Entity e) {
        var m = e.mcc$entity;
        return m != null ? m.yRotO : e.mcc$local_prevRotationYaw;
    }

    public static void mcc$set$prevRotationYaw(Entity e, float v) {
        var m = e.mcc$entity;
        if (m != null) m.yRotO = v;
        else e.mcc$local_prevRotationYaw = v;
    }

    public static float mcc$get$prevRotationPitch(Entity e) {
        var m = e.mcc$entity;
        return m != null ? m.xRotO : e.mcc$local_prevRotationPitch;
    }

    public static void mcc$set$prevRotationPitch(Entity e, float v) {
        var m = e.mcc$entity;
        if (m != null) m.xRotO = v;
        else e.mcc$local_prevRotationPitch = v;
    }

    public static boolean mcc$get$onGround(Entity e) {
        var m = e.mcc$entity;
        return m != null ? m.onGround() : e.mcc$local_onGround;
    }

    public static void mcc$set$onGround(Entity e, boolean v) {
        var m = e.mcc$entity;
        if (m != null) m.setOnGround(v);
        else e.mcc$local_onGround = v;
    }

    /** Removed from the world (or, for players, dead: 1.12 set it when a player died). */
    public static boolean mcc$get$isDead(Entity e) {
        var m = e.mcc$entity;
        if (m == null) return e.mcc$local_isDead;
        return m.isRemoved() || m instanceof net.minecraft.world.entity.player.Player p && p.isDeadOrDying();
    }

    public static void mcc$set$isDead(Entity e, boolean v) {
        if (v) e.setDead();
        else e.mcc$local_isDead = false;
    }

    public static float mcc$get$width(Entity e) {
        var m = e.mcc$entity;
        return m != null ? m.getBbWidth() : e.mcc$local_width;
    }

    /** Writes resize the host (a mod setting {@code width} directly, as some 1.12 code did instead of setSize). */
    public static void mcc$set$width(Entity e, float v) {
        e.mcc$local_width = v;
        if (e.mcc$host() != null) e.mcc$host().mcc$setSize(v, mcc$get$height(e));
    }

    public static float mcc$get$height(Entity e) {
        var m = e.mcc$entity;
        return m != null ? m.getBbHeight() : e.mcc$local_height;
    }

    public static void mcc$set$height(Entity e, float v) {
        e.mcc$local_height = v;
        if (e.mcc$host() != null) e.mcc$host().mcc$setSize(mcc$get$width(e), v);
    }

    public static int mcc$get$ticksExisted(Entity e) {
        var m = e.mcc$entity;
        return m != null ? m.tickCount : e.mcc$local_ticksExisted;
    }

    public static void mcc$set$ticksExisted(Entity e, int v) {
        var m = e.mcc$entity;
        if (m != null) m.tickCount = v;
        else e.mcc$local_ticksExisted = v;
    }

    public static float mcc$get$fallDistance(Entity e) {
        var m = e.mcc$entity;
        return m != null ? m.fallDistance : e.mcc$local_fallDistance;
    }

    public static void mcc$set$fallDistance(Entity e, float v) {
        var m = e.mcc$entity;
        if (m != null) m.fallDistance = v;
        else e.mcc$local_fallDistance = v;
    }

    public static int mcc$get$dimension(Entity e) {
        var m = e.mcc$entity;
        return m != null ? EntityBridge1122.dimensionId(m.level()) : e.mcc$local_dimension;
    }

    public static void mcc$set$dimension(Entity e, int v) {
        e.mcc$local_dimension = v;
    }

    public static int mcc$get$hurtResistantTime(Entity e) {
        var m = e.mcc$entity;
        return m != null ? m.invulnerableTime : e.mcc$local_hurtResistantTime;
    }

    public static void mcc$set$hurtResistantTime(Entity e, int v) {
        var m = e.mcc$entity;
        if (m != null) m.invulnerableTime = v;
        else e.mcc$local_hurtResistantTime = v;
    }

    public static boolean mcc$get$noClip(Entity e) {
        var m = e.mcc$entity;
        return m != null ? m.noPhysics : e.mcc$local_noClip;
    }

    public static void mcc$set$noClip(Entity e, boolean v) {
        var m = e.mcc$entity;
        if (m != null) m.noPhysics = v;
        else e.mcc$local_noClip = v;
    }

    public static float mcc$get$stepHeight(Entity e) {
        var m = e.mcc$entity;
        return m != null ? m.maxUpStep() : e.mcc$local_stepHeight;
    }

    /** The modern step height is an attribute of living entities: a mod's value becomes its base. */
    public static void mcc$set$stepHeight(Entity e, float v) {
        e.mcc$local_stepHeight = v;
        if (e.mcc$entity instanceof net.minecraft.world.entity.LivingEntity l) {
            var a = l.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.STEP_HEIGHT);
            if (a != null) a.setBaseValue(v);
        }
    }

    public static boolean mcc$get$collidedHorizontally(Entity e) {
        var m = e.mcc$entity;
        return m != null ? m.horizontalCollision : e.mcc$local_collidedHorizontally;
    }

    public static void mcc$set$collidedHorizontally(Entity e, boolean v) {
        var m = e.mcc$entity;
        if (m != null) m.horizontalCollision = v;
        else e.mcc$local_collidedHorizontally = v;
    }

    public static boolean mcc$get$collidedVertically(Entity e) {
        var m = e.mcc$entity;
        return m != null ? m.verticalCollision : e.mcc$local_collidedVertically;
    }

    public static void mcc$set$collidedVertically(Entity e, boolean v) {
        var m = e.mcc$entity;
        if (m != null) m.verticalCollision = v;
        else e.mcc$local_collidedVertically = v;
    }

    public static boolean mcc$get$collided(Entity e) {
        return mcc$get$collidedHorizontally(e) || mcc$get$collidedVertically(e);
    }

    public static void mcc$set$collided(Entity e, boolean v) {
        // derived from the two collision flags on the modern entity
    }

    /** Set after changing a player's motion so the server sends it (the modern hurtMarked). */
    public static boolean mcc$get$velocityChanged(Entity e) {
        var m = e.mcc$entity;
        return m != null ? m.hurtMarked : e.mcc$local_velocityChanged;
    }

    public static void mcc$set$velocityChanged(Entity e, boolean v) {
        var m = e.mcc$entity;
        if (m != null) m.hurtMarked = v;
        else e.mcc$local_velocityChanged = v;
    }

    public static boolean mcc$get$isAirBorne(Entity e) {
        var m = e.mcc$entity;
        return m != null ? m.hasImpulse : e.mcc$local_isAirBorne;
    }

    public static void mcc$set$isAirBorne(Entity e, boolean v) {
        var m = e.mcc$entity;
        if (m != null) m.hasImpulse = v;
        else e.mcc$local_isAirBorne = v;
    }

    public static int mcc$get$timeUntilPortal(Entity e) {
        var m = e.mcc$entity;
        return m != null ? m.getPortalCooldown() : e.mcc$local_timeUntilPortal;
    }

    public static void mcc$set$timeUntilPortal(Entity e, int v) {
        var m = e.mcc$entity;
        if (m != null) m.setPortalCooldown(v);
        else e.mcc$local_timeUntilPortal = v;
    }

    public static boolean mcc$get$inWater(Entity e) {
        var m = e.mcc$entity;
        return m != null ? m.isInWater() : e.mcc$local_inWater;
    }

    public static void mcc$set$inWater(Entity e, boolean v) {
        e.mcc$local_inWater = v;
    }

    public static boolean mcc$get$isImmuneToFire(Entity e) {
        var m = e.mcc$entity;
        if (e.mcc$local_isImmuneToFire) return true;
        return m != null && !e.mcc$hosted() && m.fireImmune();
    }

    /** Hosts answer fireImmune() from this flag. */
    public static void mcc$set$isImmuneToFire(Entity e, boolean v) {
        e.mcc$local_isImmuneToFire = v;
    }

    public static float mcc$get$distanceWalkedModified(Entity e) {
        var m = e.mcc$entity;
        return m != null ? m.walkDist : e.mcc$local_distanceWalkedModified;
    }

    public static void mcc$set$distanceWalkedModified(Entity e, float v) {
        var m = e.mcc$entity;
        if (m != null) m.walkDist = v;
        else e.mcc$local_distanceWalkedModified = v;
    }

    public static float mcc$get$prevDistanceWalkedModified(Entity e) {
        var m = e.mcc$entity;
        return m != null ? m.walkDistO : e.mcc$local_prevDistanceWalkedModified;
    }

    public static void mcc$set$prevDistanceWalkedModified(Entity e, float v) {
        var m = e.mcc$entity;
        if (m != null) m.walkDistO = v;
        else e.mcc$local_prevDistanceWalkedModified = v;
    }

    public static java.util.UUID mcc$get$entityUniqueID(Entity e) {
        var m = e.mcc$entity;
        return m != null ? m.getUUID() : e.mcc$local_entityUniqueID;
    }

    public static void mcc$set$entityUniqueID(Entity e, java.util.UUID v) {
        if (v == null) return;
        var m = e.mcc$entity;
        if (m != null && !m.isAddedToLevel()) m.setUUID(v);
        e.mcc$local_entityUniqueID = v;
    }

    // ------------------------------------------------------------------ shim helpers

    public double mcc$x() {
        return mcc$get$posX(this);
    }

    public double mcc$y() {
        return mcc$get$posY(this);
    }

    public double mcc$z() {
        return mcc$get$posZ(this);
    }

    public World mcc$world() {
        return mcc$get$world(this);
    }

    public net.minecraft.world.entity.Entity mcc$modern() {
        return mcc$entity;
    }

    /** Set when a host runs this object (known in the constructor already, before the host stored its legacy object). */
    private boolean mcc$hostedFlag;

    /** True when this legacy object is run by a host (rather than being a view of a vanilla entity). */
    public final boolean mcc$hosted() {
        return mcc$hostedFlag && mcc$entity instanceof org.kylepitcock.legacyconnector.v1_12_2.entity.Host1122;
    }

    /** Marks a legacy object bound to a host later (spawnEntity adopting one constructed without a world). */
    public final void mcc$markHosted() {
        mcc$hostedFlag = true;
    }

    protected final org.kylepitcock.legacyconnector.v1_12_2.entity.Host1122 mcc$host() {
        return mcc$hosted() ? (org.kylepitcock.legacyconnector.v1_12_2.entity.Host1122) mcc$entity : null;
    }

    /** Whether the host should be immune to fire (set by the mod through isImmuneToFire, or isImmuneToFire() overridden). */
    public final boolean mcc$fireImmune() {
        return mcc$local_isImmuneToFire || org.kylepitcock.legacyconnector.v1_12_2.entity.Hosts1122.overrides(getClass(), "isImmuneToFire") && isImmuneToFire();
    }

    private net.minecraft.world.level.Level mcc$level() {
        if (mcc$entity != null) return mcc$entity.level();
        World w = mcc$local_world;
        return w == null ? null : w.mcc$level();
    }

    // ------------------------------------------------------------------ data, NBT, identity

    /** Registers the entity's data keys (vanilla's are not kept; mods register theirs here). */
    protected void entityInit() {
    }

    public legacyconnector.v1_12_2.net.minecraft.network.datasync.EntityDataManager getDataManager() {
        return dataManager;
    }

    /** Client: a data value arrived from the server. */
    public void notifyDataManagerChange(legacyconnector.v1_12_2.net.minecraft.network.datasync.DataParameter<?> key) {
    }

    protected void readEntityFromNBT(NBTTagCompound tag) {
    }

    protected void writeEntityToNBT(NBTTagCompound tag) {
    }

    public final void mcc$readEntityFromNBT(NBTTagCompound tag) {
        readEntityFromNBT(tag);
    }

    public final void mcc$writeEntityToNBT(NBTTagCompound tag) {
        writeEntityToNBT(tag);
    }

    /** The whole entity as NBT (the modern save of the entity, plus the legacy data of a view). */
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        if (mcc$entity != null) mcc$entity.saveWithoutId(tag.mcc$modern());
        if (!mcc$hosted()) {
            writeEntityToNBT(tag);
            mcc$writeCaps(tag);
        }
        return tag;
    }

    public void readFromNBT(NBTTagCompound tag) {
        if (mcc$entity != null) mcc$entity.load(tag.mcc$modern());
        if (!mcc$hosted()) {
            readEntityFromNBT(tag);
            mcc$readCaps(tag);
        }
    }

    public boolean writeToNBTOptional(NBTTagCompound tag) {
        if (mcc$get$isDead(this) || mcc$entity == null) return false;
        String id = getEntityString();
        if (id == null) return false;
        tag.setString("id", id);
        writeToNBT(tag);
        return true;
    }

    public boolean writeToNBTAtomically(NBTTagCompound tag) {
        return writeToNBTOptional(tag);
    }

    /** Forge: the entity as NBT (with its id). */
    public NBTTagCompound serializeNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        String id = getEntityString();
        if (id != null) tag.setString("id", id);
        return writeToNBT(tag);
    }

    public void deserializeNBT(NBTTagCompound tag) {
        readFromNBT(tag);
    }

    /** The modern type's id ("minecraft:pig", "mod:thing"), or null when the entity is never saved. */
    public String getEntityString() {
        if (mcc$entity == null) {
            var key = legacyconnector.v1_12_2.net.minecraft.entity.EntityList.getKey(this);
            return key == null ? null : key.toString();
        }
        if (!mcc$entity.getType().canSerialize()) return null;
        return net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(mcc$entity.getType()).toString();
    }

    protected legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagList newDoubleNBTList(double... values) {
        var list = new legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagList();
        for (double v : values) list.appendTag(new legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagDouble(v));
        return list;
    }

    protected legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagList newFloatNBTList(float... values) {
        var list = new legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagList();
        for (float v : values) list.appendTag(new legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagFloat(v));
        return list;
    }

    /** Forge's persistent data of the entity (kept across saves; players keep it through death under "PlayerPersisted"). */
    public NBTTagCompound getEntityData() {
        var m = mcc$entity;
        if (m == null) {
            if (mcc$localData == null) mcc$localData = new NBTTagCompound();
            return mcc$localData;
        }
        return new NBTTagCompound(m.getPersistentData());
    }

    private NBTTagCompound mcc$localData;

    public int getEntityId() {
        return mcc$entity == null ? mcc$localId : mcc$entity.getId();
    }

    private int mcc$localId = -1;

    /** Mods number client-side entities they build from their own packets; the id of an entity already in a level is left alone. */
    public void setEntityId(int id) {
        if (mcc$entity == null) mcc$localId = id;
        else if (!mcc$entity.isAddedToLevel()) mcc$entity.setId(id);
    }

    public java.util.UUID getUniqueID() {
        return mcc$get$entityUniqueID(this);
    }

    public void setUniqueId(java.util.UUID id) {
        mcc$set$entityUniqueID(this, id);
    }

    /** Forge: the id kept across sessions. */
    public java.util.UUID getPersistentID() {
        return getUniqueID();
    }

    public String getCachedUniqueIdString() {
        return getUniqueID().toString();
    }

    public String getName() {
        if (mcc$entity == null) {
            String s = getEntityString();
            return s == null ? "generic" : s;
        }
        return mcc$entity.getName().getString();
    }

    public java.util.Set<String> getTags() {
        return mcc$entity == null ? new java.util.HashSet<>() : mcc$entity.getTags();
    }

    public boolean addTag(String tag) {
        return mcc$entity != null && mcc$entity.addTag(tag);
    }

    public boolean removeTag(String tag) {
        return mcc$entity != null && mcc$entity.removeTag(tag);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Entity other)) return false;
        if (mcc$entity != null) return mcc$entity == other.mcc$entity;
        return this == o;
    }

    @Override
    public int hashCode() {
        return mcc$entity != null ? System.identityHashCode(mcc$entity) : System.identityHashCode(this);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "['" + getName() + "'/" + getEntityId() + ", x=" + mcc$x() + ", y=" + mcc$y() + ", z=" + mcc$z() + "]";
    }

    // ------------------------------------------------------------------ capabilities

    private java.util.Map<legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation, ICapabilityProvider> mcc$caps;

    /** Posts AttachCapabilitiesEvent for this entity (once). */
    public final void mcc$attachCapabilities() {
        if (mcc$caps != null) return;
        mcc$caps = new java.util.LinkedHashMap<>();
        var bus = legacyconnector.v1_12_2.net.minecraftforge.common.MinecraftForge.EVENT_BUS;
        if (!bus.mcc$hasListeners(legacyconnector.v1_12_2.net.minecraftforge.event.AttachCapabilitiesEvent.class)) return;
        try {
            var event = new legacyconnector.v1_12_2.net.minecraftforge.event.AttachCapabilitiesEvent<>(Entity.class, this);
            bus.post(event);
            mcc$caps.putAll(event.getCapabilities());
        } catch (RuntimeException | LinkageError e) {
            org.slf4j.LoggerFactory.getLogger("LegacyConnector/Entities").warn("Attaching capabilities to {} failed: {}", getClass().getName(), e.toString());
        }
    }

    public final boolean mcc$hasCaps() {
        return mcc$caps != null && !mcc$caps.isEmpty();
    }

    @Override
    public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
        if (capability == null || mcc$caps == null) return false;
        for (ICapabilityProvider p : mcc$caps.values()) {
            if (p.hasCapability(capability, facing)) return true;
        }
        return false;
    }

    @Override
    public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
        if (capability == null || mcc$caps == null) return null;
        for (ICapabilityProvider p : mcc$caps.values()) {
            T t = p.getCapability(capability, facing);
            if (t != null) return t;
        }
        return null;
    }

    /** Writes the serializable capabilities under "ForgeCaps" (1.12 layout). */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public final void mcc$writeCaps(NBTTagCompound tag) {
        if (mcc$caps == null || mcc$caps.isEmpty()) return;
        NBTTagCompound caps = new NBTTagCompound();
        mcc$caps.forEach((key, p) -> {
            if (!(p instanceof legacyconnector.v1_12_2.net.minecraftforge.common.util.INBTSerializable s)) return;
            try {
                var nbt = s.serializeNBT();
                if (nbt != null) caps.setTag(key.toString(), (legacyconnector.v1_12_2.net.minecraft.nbt.NBTBase) nbt);
            } catch (RuntimeException | LinkageError e) {
                org.slf4j.LoggerFactory.getLogger("LegacyConnector/Entities").warn("Capability {} failed to save: {}", key, e.toString());
            }
        });
        if (!caps.getKeySet().isEmpty()) tag.setTag("ForgeCaps", caps);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public final void mcc$readCaps(NBTTagCompound tag) {
        if (mcc$caps == null || mcc$caps.isEmpty() || !tag.hasKey("ForgeCaps", 10)) return;
        NBTTagCompound caps = tag.getCompoundTag("ForgeCaps");
        mcc$caps.forEach((key, p) -> {
            if (!(p instanceof legacyconnector.v1_12_2.net.minecraftforge.common.util.INBTSerializable s) || !caps.hasKey(key.toString())) return;
            try {
                s.deserializeNBT(caps.getTag(key.toString()));
            } catch (RuntimeException | LinkageError e) {
                org.slf4j.LoggerFactory.getLogger("LegacyConnector/Entities").warn("Capability {} failed to load: {}", key, e.toString());
            }
        });
    }

    // ------------------------------------------------------------------ ticking (hosts hand these back to vanilla)

    public void onUpdate() {
        var host = mcc$host();
        if (host != null) host.mcc$superTick();
        else if (!mcc$entity()) onEntityUpdate();
        firstUpdate = false;
    }

    private boolean mcc$entity() {
        return mcc$entity != null;
    }

    public void onEntityUpdate() {
        var host = mcc$host();
        if (host != null) host.mcc$superBaseTick();
    }

    /** Fell out of the world: removed (living entities take void damage instead). */
    protected void outOfWorld() {
        var host = mcc$host();
        if (host != null) host.mcc$superOutOfWorld();
        else setDead();
    }

    public final void mcc$outOfWorld() {
        outOfWorld();
    }

    public void onKillCommand() {
        setDead();
    }

    public void setDead() {
        mcc$local_isDead = true;
        var m = mcc$entity;
        if (m != null && !(m instanceof net.minecraft.world.entity.player.Player)) m.discard();
    }

    public void setDropItemsWhenDead(boolean drop) {
    }

    public boolean isEntityAlive() {
        var m = mcc$entity;
        return m != null ? m.isAlive() : !mcc$local_isDead;
    }

    public boolean isServerWorld() {
        return mcc$entity != null && !mcc$entity.level().isClientSide();
    }

    // ------------------------------------------------------------------ position, size and movement

    public void setPosition(double x, double y, double z) {
        var m = mcc$entity;
        if (m != null) {
            m.setPos(x, y, z);
        } else {
            mcc$local_posX = x;
            mcc$local_posY = y;
            mcc$local_posZ = z;
        }
    }

    public void setLocationAndAngles(double x, double y, double z, float yaw, float pitch) {
        var m = mcc$entity;
        if (m != null) {
            m.moveTo(x, y, z, yaw, pitch);
        } else {
            mcc$local_posX = mcc$local_prevPosX = mcc$local_lastTickPosX = x;
            mcc$local_posY = mcc$local_prevPosY = mcc$local_lastTickPosY = y;
            mcc$local_posZ = mcc$local_prevPosZ = mcc$local_lastTickPosZ = z;
            mcc$local_rotationYaw = mcc$local_prevRotationYaw = yaw;
            mcc$local_rotationPitch = mcc$local_prevRotationPitch = pitch;
        }
    }

    public void moveToBlockPosAndAngles(BlockPos pos, float yaw, float pitch) {
        setLocationAndAngles(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, yaw, pitch);
    }

    public void setPositionAndRotation(double x, double y, double z, float yaw, float pitch) {
        var m = mcc$entity;
        if (m != null) m.absMoveTo(x, y, z, yaw, pitch);
        else setLocationAndAngles(x, y, z, yaw, pitch);
    }

    /** Client: the position and rotation from the server, reached over {@code increments} ticks (the modern lerpTo). */
    public void setPositionAndRotationDirect(double x, double y, double z, float yaw, float pitch, int increments, boolean teleport) {
        var host = mcc$host();
        if (host != null) {
            host.mcc$superLerpTo(x, y, z, yaw, pitch, increments);
        } else if (mcc$entity != null) {
            mcc$entity.lerpTo(x, y, z, yaw, pitch, increments);
        } else {
            setPosition(x, y, z);
            setRotation(yaw, pitch);
        }
    }

    /** Moves the entity and tells the clients (players: a teleport). */
    public void setPositionAndUpdate(double x, double y, double z) {
        var m = mcc$entity;
        if (m != null) m.teleportTo(x, y, z);
        else setPosition(x, y, z);
    }

    public void setRotation(float yaw, float pitch) {
        mcc$set$rotationYaw(this, yaw % 360.0F);
        mcc$set$rotationPitch(this, pitch % 360.0F);
    }

    /** Turns the entity by mouse movement (0.15 degrees per unit; the pitch is clamped to +-90). */
    public void turn(float yaw, float pitch) {
        if (mcc$entity != null) {
            mcc$entity.turn(yaw, pitch);
            return;
        }
        mcc$local_rotationYaw = (float) (mcc$local_rotationYaw + yaw * 0.15D);
        mcc$local_rotationPitch = Math.max(-90.0F, Math.min(90.0F, (float) (mcc$local_rotationPitch - pitch * 0.15D)));
    }

    public void copyLocationAndAnglesFrom(Entity other) {
        setLocationAndAngles(other.mcc$x(), other.mcc$y(), other.mcc$z(), mcc$get$rotationYaw(other), mcc$get$rotationPitch(other));
    }

    public void setSize(float width, float height) {
        mcc$local_width = width;
        mcc$local_height = height;
        var host = mcc$host();
        if (host != null) host.mcc$setSize(width, height);
    }

    public AxisAlignedBB getEntityBoundingBox() {
        var m = mcc$entity;
        if (m != null) return AxisAlignedBB.mcc$of(m.getBoundingBox());
        double w = mcc$local_width / 2.0D;
        return new AxisAlignedBB(mcc$local_posX - w, mcc$local_posY, mcc$local_posZ - w, mcc$local_posX + w, mcc$local_posY + mcc$local_height,
                mcc$local_posZ + w);
    }

    public void setEntityBoundingBox(AxisAlignedBB box) {
        if (mcc$entity != null && box != null) mcc$entity.setBoundingBox(box.mcc$modern());
    }

    /** Moves the position to the bottom centre of the box (after setEntityBoundingBox). */
    public void resetPositionToBB() {
        AxisAlignedBB box = getEntityBoundingBox();
        setPosition((box.minX + box.maxX) / 2.0D, box.minY, (box.minZ + box.maxZ) / 2.0D);
    }

    public AxisAlignedBB getRenderBoundingBox() {
        return getEntityBoundingBox();
    }

    /** The box other entities collide with (boats, shulkers), or null when nothing collides with it. */
    public AxisAlignedBB getCollisionBoundingBox() {
        return null;
    }

    /** The box this entity collides with when it touches {@code other} (boats and carts are solid), or null. */
    public AxisAlignedBB getCollisionBox(Entity other) {
        return null;
    }

    public void addVelocity(double x, double y, double z) {
        var m = mcc$entity;
        if (m != null) {
            m.push(x, y, z);
        } else {
            mcc$local_motionX += x;
            mcc$local_motionY += y;
            mcc$local_motionZ += z;
            mcc$local_isAirBorne = true;
        }
    }

    /** Client: the velocity from the server. */
    public void setVelocity(double x, double y, double z) {
        var m = mcc$entity;
        if (m != null) {
            m.lerpMotion(x, y, z);
        } else {
            mcc$local_motionX = x;
            mcc$local_motionY = y;
            mcc$local_motionZ = z;
        }
    }

    public void markVelocityChanged() {
        if (mcc$entity != null) mcc$entity.hurtMarked = true;
        else mcc$local_velocityChanged = true;
    }

    /** Moves by the given amounts with collisions (the modern move). */
    public void move(MoverType type, double x, double y, double z) {
        var m = mcc$entity;
        if (m != null) {
            m.move(type == null ? net.minecraft.world.entity.MoverType.SELF : type.mcc$modern(), new net.minecraft.world.phys.Vec3(x, y, z));
        } else {
            setPosition(mcc$local_posX + x, mcc$local_posY + y, mcc$local_posZ + z);
        }
    }

    /** Speeds the entity up along its facing by the given input. */
    public void moveRelative(float strafe, float up, float forward, float friction) {
        if (mcc$entity != null) mcc$entity.moveRelative(friction, new net.minecraft.world.phys.Vec3(strafe, up, forward));
    }

    public float getEyeHeight() {
        var m = mcc$entity;
        return m != null ? m.getEyeHeight() : mcc$local_height * 0.85F;
    }

    public double getYOffset() {
        return 0.0D;
    }

    public double getMountedYOffset() {
        return mcc$get$height(this) * 0.75D;
    }

    public float getCollisionBorderSize() {
        return 0.0F;
    }

    public EnumFacing getHorizontalFacing() {
        return EnumFacing.byHorizontalIndex(legacyconnector.v1_12_2.net.minecraft.util.math.MathHelper.floor(mcc$get$rotationYaw(this) * 4.0F / 360.0F + 0.5D) & 3);
    }

    public EnumFacing getAdjustedHorizontalFacing() {
        return getHorizontalFacing();
    }

    public Vec3d getLookVec() {
        return getLook(1.0F);
    }

    public Vec3d getLook(float partialTicks) {
        var m = mcc$entity;
        if (m != null) return Vec3d.mcc$of(m.getViewVector(partialTicks));
        return getVectorForRotation(mcc$local_rotationPitch, mcc$local_rotationYaw);
    }

    protected final Vec3d getVectorForRotation(float pitch, float yaw) {
        float f = (float) Math.cos(-yaw * 0.017453292F - (float) Math.PI);
        float f1 = (float) Math.sin(-yaw * 0.017453292F - (float) Math.PI);
        float f2 = (float) -Math.cos(-pitch * 0.017453292F);
        float f3 = (float) Math.sin(-pitch * 0.017453292F);
        return new Vec3d(f1 * f2, f3, f * f2);
    }

    public Vec3d getForward() {
        return getVectorForRotation(mcc$get$rotationPitch(this), mcc$get$rotationYaw(this));
    }

    public Vec3d getPositionEyes(float partialTicks) {
        var m = mcc$entity;
        if (m != null) return Vec3d.mcc$of(m.getEyePosition(partialTicks));
        return new Vec3d(mcc$local_posX, mcc$local_posY + getEyeHeight(), mcc$local_posZ);
    }

    /** Traces blocks along the look vector (the client's helper, also used by mods on the server). */
    public legacyconnector.v1_12_2.net.minecraft.util.math.RayTraceResult rayTrace(double blockReachDistance, float partialTicks) {
        var m = mcc$entity;
        if (m == null) return null;
        var hit = m.pick(blockReachDistance, partialTicks, false);
        return legacyconnector.v1_12_2.net.minecraft.util.math.RayTraceResult.mcc$of(hit);
    }

    public float getDistance(Entity other) {
        return (float) Math.sqrt(getDistanceSq(other));
    }

    public double getDistance(double x, double y, double z) {
        return Math.sqrt(getDistanceSq(x, y, z));
    }

    public double getDistanceSq(Entity other) {
        return getDistanceSq(other.mcc$x(), other.mcc$y(), other.mcc$z());
    }

    public double getDistanceSq(double x, double y, double z) {
        double dx = mcc$x() - x, dy = mcc$y() - y, dz = mcc$z() - z;
        return dx * dx + dy * dy + dz * dz;
    }

    public double getDistanceSq(BlockPos pos) {
        return pos.distanceSq(mcc$x(), mcc$y(), mcc$z());
    }

    public double getDistanceSqToCenter(BlockPos pos) {
        return pos.distanceSqToCenter(mcc$x(), mcc$y(), mcc$z());
    }

    public boolean isEntityEqual(Entity other) {
        return this == other || other != null && mcc$entity != null && mcc$entity == other.mcc$entity;
    }

    /** The extra hit boxes of a multi-part entity, or null. */
    public Entity[] getParts() {
        return null;
    }

    public boolean isInRangeToRender3d(double x, double y, double z) {
        double dx = mcc$x() - x, dy = mcc$y() - y, dz = mcc$z() - z;
        return isInRangeToRenderDist(dx * dx + dy * dy + dz * dz);
    }

    public boolean isInRangeToRenderDist(double distanceSq) {
        double size = getEntityBoundingBox().getAverageEdgeLength();
        if (Double.isNaN(size)) size = 1.0D;
        size = size * 64.0D * renderDistanceWeight;
        return distanceSq < size * size;
    }

    private double renderDistanceWeight = 1.0D;

    public static double getRenderDistanceWeight() {
        return 1.0D;
    }

    public static void setRenderDistanceWeight(double weight) {
    }

    public int getBrightnessForRender() {
        if (mcc$entity == null) return 0;
        var pos = mcc$entity.blockPosition();
        var level = mcc$entity.level();
        int block = level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, pos);
        int sky = level.getBrightness(net.minecraft.world.level.LightLayer.SKY, pos);
        return sky << 20 | block << 4;
    }

    public float getBrightness() {
        if (mcc$entity == null) return 1.0F;
        return mcc$entity.level().getMaxLocalRawBrightness(mcc$entity.blockPosition()) / 15.0F;
    }

    public void setWorld(World world) {
        this.mcc$local_world = world;
    }

    // ------------------------------------------------------------------ state

    public boolean attackEntityFrom(legacyconnector.v1_12_2.net.minecraft.util.DamageSource source, float amount) {
        var m = mcc$entity;
        if (m == null || source == null) return false;
        var modern = source.mcc$modern(m.level());
        var host = mcc$host();
        if (host != null) return host.mcc$superHurt(modern, amount);
        return m.hurt(modern, amount);
    }

    public boolean isEntityInvulnerable(legacyconnector.v1_12_2.net.minecraft.util.DamageSource source) {
        var m = mcc$entity;
        if (m == null || source == null) return false;
        return m.isInvulnerable() && !source.canHarmInCreative() && !source.isCreativePlayer();
    }

    public boolean getIsInvulnerable() {
        return mcc$entity != null && mcc$entity.isInvulnerable();
    }

    public void setEntityInvulnerable(boolean value) {
        if (mcc$entity != null) mcc$entity.setInvulnerable(value);
    }

    public boolean hitByEntity(Entity attacker) {
        return false;
    }

    public boolean canBeAttackedWithItem() {
        return mcc$entity == null || mcc$entity.isAttackable();
    }

    public void performHurtAnimation() {
        if (mcc$entity != null) mcc$entity.animateHurt(0.0F);
    }

    public void handleStatusUpdate(byte id) {
        var host = mcc$host();
        if (host != null) host.mcc$superHandleEntityEvent(id);
        else if (mcc$entity != null) mcc$entity.handleEntityEvent(id);
    }

    public void setFire(int seconds) {
        if (mcc$entity != null) mcc$entity.igniteForSeconds(seconds);
    }

    public void extinguish() {
        if (mcc$entity != null) mcc$entity.clearFire();
    }

    public boolean isBurning() {
        return mcc$entity != null && mcc$entity.isOnFire();
    }

    public boolean canRenderOnFire() {
        return isBurning();
    }

    public int getFireImmuneTicks() {
        return 1;
    }

    public boolean isImmuneToFire() {
        return mcc$get$isImmuneToFire(this);
    }

    public boolean isImmuneToExplosions() {
        return false;
    }

    public boolean isSneaking() {
        return mcc$entity != null && mcc$entity.isShiftKeyDown();
    }

    public void setSneaking(boolean value) {
        if (mcc$entity != null) mcc$entity.setShiftKeyDown(value);
    }

    public boolean isSprinting() {
        return mcc$entity != null && mcc$entity.isSprinting();
    }

    public void setSprinting(boolean value) {
        if (mcc$entity != null) mcc$entity.setSprinting(value);
    }

    public boolean isInvisible() {
        return mcc$entity != null && mcc$entity.isInvisible();
    }

    public void setInvisible(boolean value) {
        if (mcc$entity != null) mcc$entity.setInvisible(value);
    }

    public boolean isInvisibleToPlayer(legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player) {
        if (mcc$entity == null || player == null || !(player.mcc$entity instanceof net.minecraft.world.entity.player.Player p)) return isInvisible();
        return mcc$entity.isInvisibleTo(p);
    }

    public boolean isGlowing() {
        return mcc$entity != null && mcc$entity.isCurrentlyGlowing();
    }

    public void setGlowing(boolean value) {
        if (mcc$entity != null) mcc$entity.setGlowingTag(value);
    }

    public boolean isSilent() {
        return mcc$entity != null && mcc$entity.isSilent();
    }

    public void setSilent(boolean value) {
        if (mcc$entity != null) mcc$entity.setSilent(value);
    }

    public boolean hasNoGravity() {
        return mcc$entity != null && mcc$entity.isNoGravity();
    }

    public void setNoGravity(boolean value) {
        if (mcc$entity != null) mcc$entity.setNoGravity(value);
    }

    /** Bit {@code flag} of the shared flags (0 burning, 1 sneaking, 3 sprinting, 5 invisible, 6 glowing, 7 elytra). */
    protected boolean getFlag(int flag) {
        return switch (flag) {
            case 0 -> isBurning();
            case 1 -> isSneaking();
            case 3 -> isSprinting();
            case 5 -> isInvisible();
            case 6 -> isGlowing();
            case 7 -> mcc$entity instanceof net.minecraft.world.entity.LivingEntity l && l.isFallFlying();
            default -> false;
        };
    }

    protected void setFlag(int flag, boolean value) {
        switch (flag) {
            case 1 -> setSneaking(value);
            case 3 -> setSprinting(value);
            case 5 -> setInvisible(value);
            case 6 -> setGlowing(value);
            default -> {
            }
        }
    }

    public int getAir() {
        return mcc$entity != null ? mcc$entity.getAirSupply() : 300;
    }

    public void setAir(int air) {
        if (mcc$entity != null) mcc$entity.setAirSupply(air);
    }

    public boolean isInWater() {
        return mcc$get$inWater(this);
    }

    public boolean isInLava() {
        return mcc$entity != null && mcc$entity.isInLava();
    }

    public boolean isWet() {
        return mcc$entity != null && mcc$entity.isInWaterRainOrBubble();
    }

    public boolean isOverWater() {
        return isInWater();
    }

    /** Pushes the entity along flowing water and returns whether it is in water (it stops burning; its fall resets). */
    public boolean handleWaterMovement() {
        if (mcc$entity == null) return false;
        if (!mcc$entity.updateFluidHeightAndDoFluidPushing(net.minecraft.tags.FluidTags.WATER, 0.014D)) return false;
        mcc$entity.resetFallDistance();
        mcc$entity.clearFire();
        return true;
    }

    public boolean isInsideOfMaterial(legacyconnector.v1_12_2.net.minecraft.block.material.Material material) {
        if (mcc$entity == null || material == null) return false;
        if (material == legacyconnector.v1_12_2.net.minecraft.block.material.Material.WATER) return mcc$entity.isEyeInFluid(net.minecraft.tags.FluidTags.WATER);
        if (material == legacyconnector.v1_12_2.net.minecraft.block.material.Material.LAVA) return mcc$entity.isEyeInFluid(net.minecraft.tags.FluidTags.LAVA);
        return false;
    }

    public boolean isEntityInsideOpaqueBlock() {
        return mcc$entity != null && mcc$entity.isInWall();
    }

    public boolean isPushedByWater() {
        return true;
    }

    public boolean isNonBoss() {
        return mcc$entity == null || mcc$entity.canUsePortal(false);
    }

    public boolean doesEntityNotTriggerPressurePlate() {
        return false;
    }

    public void setInWeb() {
        var m = mcc$entity;
        if (m != null) m.makeStuckInBlock(net.minecraft.world.level.block.Blocks.COBWEB.defaultBlockState(), new net.minecraft.world.phys.Vec3(0.25D, 0.05D, 0.25D));
        isInWeb = true;
    }

    public boolean canBeCollidedWith() {
        return !mcc$hosted() && mcc$entity != null && mcc$entity.isPickable();
    }

    public boolean canBePushed() {
        return !mcc$hosted() && mcc$entity != null && mcc$entity.isPushable();
    }

    public void applyEntityCollision(Entity other) {
        if (mcc$entity != null && other != null && other.mcc$entity != null && !mcc$hosted()) mcc$entity.push(other.mcc$entity);
    }

    public void onCollideWithPlayer(legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player) {
    }

    /** Right-clicking the entity; true when it used the click. */
    public boolean processInitialInteract(legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player, legacyconnector.v1_12_2.net.minecraft.util.EnumHand hand) {
        return false;
    }

    public legacyconnector.v1_12_2.net.minecraft.util.EnumActionResult applyPlayerInteraction(legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player,
            Vec3d vec, legacyconnector.v1_12_2.net.minecraft.util.EnumHand hand) {
        return legacyconnector.v1_12_2.net.minecraft.util.EnumActionResult.PASS;
    }

    /** The landing after a fall: living entities take damage. Hosts route the modern causeFallDamage here. */
    public void fall(float distance, float damageMultiplier) {
        var host = mcc$host();
        if (host != null) host.mcc$superFall(distance, damageMultiplier);
        else if (mcc$entity != null) mcc$entity.causeFallDamage(distance, damageMultiplier, mcc$entity.damageSources().fall());
    }

    public void onStruckByLightning(legacyconnector.v1_12_2.net.minecraft.entity.effect.EntityLightningBolt bolt) {
        if (mcc$entity == null || !(mcc$entity.level() instanceof net.minecraft.server.level.ServerLevel level)) return;
        net.minecraft.world.entity.LightningBolt modern = bolt != null && bolt.mcc$entity instanceof net.minecraft.world.entity.LightningBolt b ? b : null;
        if (modern == null) {
            modern = net.minecraft.world.entity.EntityType.LIGHTNING_BOLT.create(level);
            if (modern == null) return;
            modern.moveTo(mcc$entity.position());
        }
        var host = mcc$host();
        if (host != null) host.mcc$superThunderHit(level, modern);
        else mcc$entity.thunderHit(level, modern);
    }

    public void onKillEntity(EntityLivingBase victim) {
    }

    public void addTrackingPlayer(legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayerMP player) {
    }

    public void removeTrackingPlayer(legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayerMP player) {
    }

    public boolean isSpectatedByPlayer(legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayerMP player) {
        return true;
    }

    public void playSound(legacyconnector.v1_12_2.net.minecraft.util.SoundEvent sound, float volume, float pitch) {
        var m = mcc$entity;
        if (m == null || sound == null || m.isSilent()) return;
        var event = sound.mcc$event();
        if (event != null) m.level().playSound(null, m.getX(), m.getY(), m.getZ(), event, getSoundCategory().mcc$modern(), volume, pitch);
    }

    public legacyconnector.v1_12_2.net.minecraft.util.SoundCategory getSoundCategory() {
        return mcc$entity == null ? legacyconnector.v1_12_2.net.minecraft.util.SoundCategory.NEUTRAL
                : legacyconnector.v1_12_2.net.minecraft.util.SoundCategory.mcc$of(mcc$entity.getSoundSource());
    }

    protected legacyconnector.v1_12_2.net.minecraft.util.SoundEvent getSwimSound() {
        return org.kylepitcock.legacyconnector.v1_12_2.entity.Sounds1122.legacy(net.minecraft.sounds.SoundEvents.GENERIC_SWIM);
    }

    protected legacyconnector.v1_12_2.net.minecraft.util.SoundEvent getSplashSound() {
        return org.kylepitcock.legacyconnector.v1_12_2.entity.Sounds1122.legacy(net.minecraft.sounds.SoundEvents.GENERIC_SPLASH);
    }

    protected boolean canTriggerWalking() {
        return true;
    }

    public void setPortal(BlockPos pos) {
    }

    public int getPortalCooldown() {
        return 300;
    }

    public int getMaxInPortalTime() {
        return 1;
    }

    public int getMaxFallHeight() {
        return 3;
    }

    /** Moves to another dimension (the same position in the target level), returning the entity there. */
    public Entity changeDimension(int dimension) {
        return EntityBridge1122.changeDimension(this, dimension);
    }

    public boolean pushOutOfBlocks(double x, double y, double z) {
        return false;
    }

    // ------------------------------------------------------------------ names and teams

    @Override
    public legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent getDisplayName() {
        var m = mcc$entity;
        return new legacyconnector.v1_12_2.net.minecraft.util.text.TextComponentString(m == null ? getName() : m.getDisplayName().getString());
    }

    public boolean hasCustomName() {
        return mcc$entity != null && mcc$entity.hasCustomName();
    }

    public String getCustomNameTag() {
        var m = mcc$entity;
        return m == null || m.getCustomName() == null ? "" : m.getCustomName().getString();
    }

    public void setCustomNameTag(String name) {
        if (mcc$entity != null) mcc$entity.setCustomName(name == null || name.isEmpty() ? null : net.minecraft.network.chat.Component.literal(name));
    }

    public boolean getAlwaysRenderNameTag() {
        return mcc$entity != null && mcc$entity.isCustomNameVisible();
    }

    public void setAlwaysRenderNameTag(boolean value) {
        if (mcc$entity != null) mcc$entity.setCustomNameVisible(value);
    }

    public boolean getAlwaysRenderNameTagForRender() {
        return getAlwaysRenderNameTag();
    }

    public boolean isOnSameTeam(Entity other) {
        return mcc$entity != null && other != null && other.mcc$entity != null && mcc$entity.isAlliedTo(other.mcc$entity);
    }

    // ------------------------------------------------------------------ riding

    public boolean isRiding() {
        return mcc$entity != null && mcc$entity.isPassenger();
    }

    public Entity getRidingEntity() {
        var m = mcc$entity;
        return m == null ? null : EntityBridge1122.wrap(m.getVehicle());
    }

    public Entity getLowestRidingEntity() {
        var m = mcc$entity;
        return m == null ? this : EntityBridge1122.wrap(m.getRootVehicle());
    }

    public boolean isBeingRidden() {
        return mcc$entity != null && mcc$entity.isVehicle();
    }

    public java.util.List<Entity> getPassengers() {
        java.util.List<Entity> out = new java.util.ArrayList<>();
        var m = mcc$entity;
        if (m != null) for (var p : m.getPassengers()) out.add(EntityBridge1122.wrap(p));
        return out;
    }

    public java.util.Collection<Entity> getRecursivePassengers() {
        java.util.List<Entity> out = new java.util.ArrayList<>();
        var m = mcc$entity;
        if (m != null) m.getIndirectPassengers().forEach(p -> out.add(EntityBridge1122.wrap(p)));
        return out;
    }

    public <T extends Entity> java.util.Collection<T> getRecursivePassengersByType(Class<T> type) {
        java.util.List<T> out = new java.util.ArrayList<>();
        for (Entity e : getRecursivePassengers()) if (type.isInstance(e)) out.add(type.cast(e));
        return out;
    }

    public boolean isPassenger(Entity entity) {
        return mcc$entity != null && entity != null && entity.mcc$entity != null && mcc$entity.hasPassenger(entity.mcc$entity);
    }

    public boolean isRidingSameEntity(Entity other) {
        return getLowestRidingEntity() == other.getLowestRidingEntity();
    }

    public boolean isRidingOrBeingRiddenBy(Entity other) {
        return mcc$entity != null && other != null && other.mcc$entity != null && mcc$entity.hasIndirectPassenger(other.mcc$entity)
                || other != null && other.mcc$entity != null && mcc$entity != null && other.mcc$entity.hasIndirectPassenger(mcc$entity);
    }

    public Entity getControllingPassenger() {
        var m = mcc$entity;
        return m == null ? null : EntityBridge1122.wrap(m.getControllingPassenger());
    }

    public boolean canPassengerSteer() {
        return mcc$entity != null && mcc$entity.isControlledByLocalInstance();
    }

    public boolean startRiding(Entity vehicle) {
        return startRiding(vehicle, false);
    }

    public boolean startRiding(Entity vehicle, boolean force) {
        var m = mcc$entity;
        return m != null && vehicle != null && vehicle.mcc$entity != null && m.startRiding(vehicle.mcc$entity, force);
    }

    public void dismountRidingEntity() {
        if (mcc$entity != null) mcc$entity.stopRiding();
    }

    public void removePassengers() {
        if (mcc$entity != null) mcc$entity.ejectPassengers();
    }

    protected boolean canBeRidden(Entity entity) {
        return true;
    }

    protected boolean canFitPassenger(Entity passenger) {
        return getPassengers().isEmpty();
    }

    /** Places a passenger on this entity. Hosts route the modern positionRider here when a mod changes it. */
    public void updatePassenger(Entity passenger) {
        if (passenger == null || passenger.mcc$entity == null || mcc$entity == null) return;
        if (org.kylepitcock.legacyconnector.v1_12_2.entity.Hosts1122.overrides(getClass(), "getMountedYOffset")) {
            passenger.mcc$entity.setPos(mcc$entity.getX(), mcc$get$posY(this) + getMountedYOffset() + passenger.getYOffset(), mcc$entity.getZ());
            return;
        }
        var host = mcc$host();
        if (host != null) host.mcc$superPositionRider(passenger.mcc$entity);
        else mcc$entity.positionRider(passenger.mcc$entity);
    }

    /** One tick while riding: the entity's own update, then its vehicle places it. */
    public void updateRidden() {
        var host = mcc$host();
        if (host != null) host.mcc$superRideTick();
        else if (mcc$entity != null) mcc$entity.rideTick();
    }

    public void applyOrientationToEntity(Entity passenger) {
    }

    /** Forge: whether the rider is drawn sitting. */
    public boolean shouldRiderSit() {
        return true;
    }

    /** Forge: whether the rider may interact with the world while riding. */
    public boolean canRiderInteract() {
        return false;
    }

    public float getRotationYawHead() {
        return 0.0F;
    }

    public void setRotationYawHead(float yaw) {
    }

    public void setRenderYawOffset(float offset) {
    }

    // ------------------------------------------------------------------ equipment (living entities override)

    public Iterable<legacyconnector.v1_12_2.net.minecraft.item.ItemStack> getHeldEquipment() {
        return java.util.Collections.emptyList();
    }

    public Iterable<legacyconnector.v1_12_2.net.minecraft.item.ItemStack> getArmorInventoryList() {
        return java.util.Collections.emptyList();
    }

    public Iterable<legacyconnector.v1_12_2.net.minecraft.item.ItemStack> getEquipmentAndArmor() {
        java.util.List<legacyconnector.v1_12_2.net.minecraft.item.ItemStack> out = new java.util.ArrayList<>();
        getHeldEquipment().forEach(out::add);
        getArmorInventoryList().forEach(out::add);
        return out;
    }

    public void setItemStackToSlot(legacyconnector.v1_12_2.net.minecraft.inventory.EntityEquipmentSlot slot, legacyconnector.v1_12_2.net.minecraft.item.ItemStack stack) {
    }

    public boolean replaceItemInInventory(int slot, legacyconnector.v1_12_2.net.minecraft.item.ItemStack stack) {
        return false;
    }

    /** Forge: the stack picking this entity gives (middle click), or empty. */
    public legacyconnector.v1_12_2.net.minecraft.item.ItemStack getPickedResult(legacyconnector.v1_12_2.net.minecraft.util.math.RayTraceResult target) {
        if (mcc$entity == null) return legacyconnector.v1_12_2.net.minecraft.item.ItemStack.EMPTY;
        var picked = mcc$entity.getPickResult();
        return picked == null ? legacyconnector.v1_12_2.net.minecraft.item.ItemStack.EMPTY : org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122.fromModern(picked);
    }

    // ------------------------------------------------------------------ drops

    public legacyconnector.v1_12_2.net.minecraft.entity.item.EntityItem dropItem(legacyconnector.v1_12_2.net.minecraft.item.Item item, int size) {
        return dropItemWithOffset(item, size, 0.0F);
    }

    public legacyconnector.v1_12_2.net.minecraft.entity.item.EntityItem dropItemWithOffset(legacyconnector.v1_12_2.net.minecraft.item.Item item, int size,
            float offsetY) {
        return entityDropItem(new legacyconnector.v1_12_2.net.minecraft.item.ItemStack(item, size, 0), offsetY);
    }

    /**
     * Drops a stack at the entity. While Forge's capture is on (legacy captureDrops, or the modern death-drop capture of the host) it
     * is collected instead of spawned.
     */
    public legacyconnector.v1_12_2.net.minecraft.entity.item.EntityItem entityDropItem(legacyconnector.v1_12_2.net.minecraft.item.ItemStack stack, float offsetY) {
        World w = mcc$get$world(this);
        if (stack == null || stack.isEmpty() || w == null || w.isRemote) return null;
        var item = new legacyconnector.v1_12_2.net.minecraft.entity.item.EntityItem(w, mcc$x(), mcc$y() + offsetY, mcc$z(), stack);
        item.setDefaultPickupDelay();
        if (captureDrops) {
            capturedDrops.add(item);
        } else if (mcc$entity != null && mcc$entity.captureDrops() != null && item.mcc$entity instanceof net.minecraft.world.entity.item.ItemEntity ie) {
            mcc$entity.captureDrops().add(ie);
        } else {
            EntityBridge1122.spawn(w, item);
        }
        return item;
    }

    // ------------------------------------------------------------------ an entity as a command sender

    @Override
    public boolean canUseCommand(int permLevel, String commandName) {
        return true;
    }

    @Override
    public BlockPos getPosition() {
        if (mcc$entity == null) return new BlockPos(mcc$local_posX, mcc$local_posY + 0.5D, mcc$local_posZ);
        var p = mcc$entity.blockPosition();
        return new BlockPos(p.getX(), p.getY(), p.getZ());
    }

    @Override
    public Vec3d getPositionVector() {
        if (mcc$entity == null) return new Vec3d(mcc$local_posX, mcc$local_posY, mcc$local_posZ);
        return new Vec3d(mcc$entity.getX(), mcc$entity.getY(), mcc$entity.getZ());
    }

    @Override
    public World getEntityWorld() {
        return mcc$get$world(this);
    }

    @Override
    public Entity getCommandSenderEntity() {
        return this;
    }

    @Override
    public legacyconnector.v1_12_2.net.minecraft.server.MinecraftServer getServer() {
        return legacyconnector.v1_12_2.net.minecraft.server.MinecraftServer.mcc$instance();
    }

    @Override
    public void sendMessage(legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent component) {
    }

    /** Adds this entity's type, id, name, position and motion to a crash report section. */
    public void addEntityCrashInfo(legacyconnector.v1_12_2.net.minecraft.crash.CrashReportCategory category) {
        category.addCrashSection("Entity Type", getEntityString() + " (" + getClass().getCanonicalName() + ")");
        category.addCrashSection("Entity ID", getEntityId());
        category.addCrashSection("Entity Name", getName());
        category.addCrashSection("Entity's Exact location", String.format(java.util.Locale.ROOT, "%.2f, %.2f, %.2f", mcc$x(), mcc$y(), mcc$z()));
        category.addCrashSection("Entity's Momentum", String.format(java.util.Locale.ROOT, "%.2f, %.2f, %.2f", mcc$get$motionX(this), mcc$get$motionY(this),
                mcc$get$motionZ(this)));
    }


    /** The scoreboard team the entity is on (area F), or null. */
    public legacyconnector.v1_12_2.net.minecraft.scoreboard.Team getTeam() {
        var team = mcc$entity == null ? null : mcc$entity.getTeam();
        return team instanceof net.minecraft.world.scores.PlayerTeam p ? legacyconnector.v1_12_2.net.minecraft.scoreboard.ScorePlayerTeam.mcc$of(p) : null;
    }
}
