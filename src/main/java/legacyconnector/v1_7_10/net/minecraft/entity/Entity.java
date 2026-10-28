package legacyconnector.v1_7_10.net.minecraft.entity;

import java.util.Random;
import java.util.UUID;
import legacyconnector.v1_7_10.net.minecraft.entity.item.EntityItem;
import legacyconnector.v1_7_10.net.minecraft.item.Item;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_7_10.net.minecraft.util.AxisAlignedBB;
import legacyconnector.v1_7_10.net.minecraft.util.ChunkCoordinates;
import legacyconnector.v1_7_10.net.minecraft.util.DamageSource;
import legacyconnector.v1_7_10.net.minecraft.util.IChatComponent;
import legacyconnector.v1_7_10.net.minecraft.util.ChatComponentText;
import legacyconnector.v1_7_10.net.minecraft.util.Vec3;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import legacyconnector.v1_7_10.net.minecraft.world.WorldProvider;
import legacyconnector.v1_7_10.net.minecraft.command.ICommandSender;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.EntityBridge;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.SoundBridge;

/**
 * 1.7.10 Entity. For entities that exist in the modern world this is a view over the modern entity: every public
 * legacy field (posX, motionY, worldObj, ...) is a phantom whose accesses the loader redirects to the static
 * accessors below. Legacy entities constructed by mods before they are spawned keep their state in local storage.
 */
public abstract class Entity implements ICommandSender {
    /** Phantom field: accesses are redirected to {@code mcc$get$posX}. */
    @Deprecated
    public double posX;
    private double mcc$local_posX;
    /** Phantom field: accesses are redirected to {@code mcc$get$posY}. */
    @Deprecated
    public double posY;
    private double mcc$local_posY;
    /** Phantom field: accesses are redirected to {@code mcc$get$posZ}. */
    @Deprecated
    public double posZ;
    private double mcc$local_posZ;
    /** Phantom field: accesses are redirected to {@code mcc$get$prevPosX}. */
    @Deprecated
    public double prevPosX;
    private double mcc$local_prevPosX;
    /** Phantom field: accesses are redirected to {@code mcc$get$prevPosY}. */
    @Deprecated
    public double prevPosY;
    private double mcc$local_prevPosY;
    /** Phantom field: accesses are redirected to {@code mcc$get$prevPosZ}. */
    @Deprecated
    public double prevPosZ;
    private double mcc$local_prevPosZ;
    /** Phantom field: accesses are redirected to {@code mcc$get$lastTickPosX}. */
    @Deprecated
    public double lastTickPosX;
    private double mcc$local_lastTickPosX;
    /** Phantom field: accesses are redirected to {@code mcc$get$lastTickPosY}. */
    @Deprecated
    public double lastTickPosY;
    private double mcc$local_lastTickPosY;
    /** Phantom field: accesses are redirected to {@code mcc$get$lastTickPosZ}. */
    @Deprecated
    public double lastTickPosZ;
    private double mcc$local_lastTickPosZ;
    /** Phantom field: accesses are redirected to {@code mcc$get$motionX}. */
    @Deprecated
    public double motionX;
    private double mcc$local_motionX;
    /** Phantom field: accesses are redirected to {@code mcc$get$motionY}. */
    @Deprecated
    public double motionY;
    private double mcc$local_motionY;
    /** Phantom field: accesses are redirected to {@code mcc$get$motionZ}. */
    @Deprecated
    public double motionZ;
    private double mcc$local_motionZ;
    /** Phantom field: accesses are redirected to {@code mcc$get$rotationYaw}. */
    @Deprecated
    public float rotationYaw;
    private float mcc$local_rotationYaw;
    /** Phantom field: accesses are redirected to {@code mcc$get$rotationPitch}. */
    @Deprecated
    public float rotationPitch;
    private float mcc$local_rotationPitch;
    /** Phantom field: accesses are redirected to {@code mcc$get$prevRotationYaw}. */
    @Deprecated
    public float prevRotationYaw;
    private float mcc$local_prevRotationYaw;
    /** Phantom field: accesses are redirected to {@code mcc$get$prevRotationPitch}. */
    @Deprecated
    public float prevRotationPitch;
    private float mcc$local_prevRotationPitch;
    /** Phantom field: accesses are redirected to {@code mcc$get$onGround}. */
    @Deprecated
    public boolean onGround;
    private boolean mcc$local_onGround;
    /** Phantom field: accesses are redirected to {@code mcc$get$isDead}. */
    @Deprecated
    public boolean isDead;
    private boolean mcc$local_isDead;
    /** Phantom field: accesses are redirected to {@code mcc$get$addedToChunk}. */
    @Deprecated
    public boolean addedToChunk;
    private boolean mcc$local_addedToChunk;
    /** Phantom field: accesses are redirected to {@code mcc$get$ticksExisted}. */
    @Deprecated
    public int ticksExisted;
    private int mcc$local_ticksExisted;
    /** Phantom field: accesses are redirected to {@code mcc$get$fallDistance}. */
    @Deprecated
    public float fallDistance;
    private float mcc$local_fallDistance;
    /** Phantom field: accesses are redirected to {@code mcc$get$isAirBorne}. */
    @Deprecated
    public boolean isAirBorne;
    private boolean mcc$local_isAirBorne;
    /** Phantom field: accesses are redirected to {@code mcc$get$isCollidedHorizontally}. */
    @Deprecated
    public boolean isCollidedHorizontally;
    private boolean mcc$local_isCollidedHorizontally;
    /** Phantom field: accesses are redirected to {@code mcc$get$isCollidedVertically}. */
    @Deprecated
    public boolean isCollidedVertically;
    private boolean mcc$local_isCollidedVertically;
    /** Phantom field: accesses are redirected to {@code mcc$get$isCollided}. */
    @Deprecated
    public boolean isCollided;
    private boolean mcc$local_isCollided;
    /** Phantom field: accesses are redirected to {@code mcc$get$hurtResistantTime}. */
    @Deprecated
    public int hurtResistantTime;
    private int mcc$local_hurtResistantTime;
    /** Phantom field: accesses are redirected to {@code mcc$get$noClip}. */
    @Deprecated
    public boolean noClip;
    private boolean mcc$local_noClip;
    /** Phantom field: accesses are redirected to {@code mcc$get$width}. */
    @Deprecated
    public float width;
    private float mcc$local_width;
    /** Phantom field: accesses are redirected to {@code mcc$get$height}. */
    @Deprecated
    public float height;
    private float mcc$local_height;
    /** Phantom field: accesses are redirected to {@code mcc$get$stepHeight}. */
    @Deprecated
    public float stepHeight;
    private float mcc$local_stepHeight;
    /** Phantom field: accesses are redirected to {@code mcc$get$timeUntilPortal}. */
    @Deprecated
    public int timeUntilPortal;
    private int mcc$local_timeUntilPortal;
    /** Phantom field: accesses are redirected to {@code mcc$get$inWater}. */
    @Deprecated
    public boolean inWater;
    private boolean mcc$local_inWater;
    /** Phantom field: accesses are redirected to {@code mcc$get$dimension}. */
    @Deprecated
    public int dimension;
    private int mcc$local_dimension;
    /** Phantom field: accesses are redirected to {@code mcc$get$velocityChanged}. */
    @Deprecated
    public boolean velocityChanged;
    private boolean mcc$local_velocityChanged;
    /** Phantom field: accesses are redirected to {@code mcc$get$worldObj}. */
    @Deprecated
    public World worldObj;
    /** Phantom field: accesses are redirected to {@code mcc$get$boundingBox}. */
    @Deprecated
    public AxisAlignedBB boundingBox;
    /** Phantom field: accesses are redirected to {@code mcc$get$riddenByEntity}. */
    @Deprecated
    public Entity riddenByEntity;
    /** Phantom field: accesses are redirected to {@code mcc$get$ridingEntity}. */
    @Deprecated
    public Entity ridingEntity;
    /** Phantom field: accesses are redirected to {@code mcc$get$entityUniqueID}. */
    @Deprecated
    public java.util.UUID entityUniqueID;

    public Random rand = new Random();
    public float yOffset;

    /**
     * Added to the modern y in {@code posY}/{@code prevPosY}/{@code lastTickPosY}: 1.62 for the local client player, whose posY
     * was at eye level in 1.7.10 (EntityPlayerSP's yOffset); renderers translate by {@code -player.posY} (Thaumcraft's nodes and
     * beams) and were drawn 1.62 blocks too high with a feet-level posY. 0 for every other entity.
     */
    protected double mcc$posYShift;

    /** {@link #mcc$posYShift} for shim code outside the subclasses (renderers, hosts). */
    public final double mcc$yShift() {
        return mcc$posYShift;
    }
    /** How much less this entity is pushed by collisions (0: fully, 1: not at all). */
    public float entityCollisionReduction;
    public boolean forceSpawn;
    public boolean preventEntitySpawning;
    public boolean isImmuneToFire;

    /** Modern entity this view represents (null for a legacy entity that is not in a world yet). */
    public net.minecraft.world.entity.Entity mcc$entity;
    private World mcc$world;
    private UUID mcc$uuid = UUID.randomUUID();
    private NBTTagCompound mcc$customData;

    protected DataWatcher dataWatcher;

    public Entity(World world) {
        this.mcc$world = world;
        this.dataWatcher = new DataWatcher(this);
        dataWatcher.addObject(0, Byte.valueOf((byte) 0));
        dataWatcher.addObject(1, Short.valueOf((short) 300));
        // A host constructing this legacy entity, or a new host for an entity a mod constructs itself.
        org.kylepitcock.legacyconnector.v1_7_10.entity.LegacyHost host = org.kylepitcock.legacyconnector.v1_7_10.entity.LegacyHosts.takeBinding();
        // An exact vanilla class (new EntityGhast(world)) is a view of the real modern entity.
        net.minecraft.world.entity.Entity vanilla = host == null ? org.kylepitcock.legacyconnector.v1_7_10.entity.VanillaEntities.create(this, world) : null;
        if (vanilla != null) EntityBridge.bind(this, vanilla);
        else if (host == null && world != null) host = org.kylepitcock.legacyconnector.v1_7_10.entity.LegacyHosts.adopt(this, world);
        if (host != null) this.mcc$entity = host.mcc$self();
        entityInit();
        mcc$fireConstructing();
    }

    /** Forge's EntityConstructing (legacy fired it at the end of the base constructor). */
    public final void mcc$fireConstructing() {
        try {
            legacyconnector.v1_7_10.net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(
                    new legacyconnector.v1_7_10.net.minecraftforge.event.entity.EntityEvent.EntityConstructing(this));
        } catch (RuntimeException | LinkageError e) {
            org.slf4j.LoggerFactory.getLogger("LegacyConnector/Entities").warn("EntityConstructing handler failed for {}: {}", getClass().getName(), e.toString());
        }
    }

    /** View constructor used by the bridge for existing modern entities. */
    protected Entity(net.minecraft.world.entity.Entity modern) {
        this.mcc$entity = modern;
        this.dataWatcher = new DataWatcher(this);
    }

    protected void entityInit() {
    }

    protected void readEntityFromNBT(NBTTagCompound tag) {
    }

    protected void writeEntityToNBT(NBTTagCompound tag) {
    }

    // Bridges for the host (legacy protected methods, overridden by mods with protected access).

    public final void mcc$readEntityFromNBT(NBTTagCompound tag) {
        readEntityFromNBT(tag);
    }

    public final void mcc$writeEntityToNBT(NBTTagCompound tag) {
        writeEntityToNBT(tag);
    }

    public final DataWatcher mcc$dataWatcher() {
        return dataWatcher;
    }

    /** True when this legacy object is run by a host (rather than being a view of a vanilla entity). */
    public final boolean mcc$hosted() {
        return mcc$entity instanceof org.kylepitcock.legacyconnector.v1_7_10.entity.LegacyHost h && h.mcc$legacy() == this;
    }

    protected final org.kylepitcock.legacyconnector.v1_7_10.entity.LegacyHost mcc$host() {
        return mcc$entity instanceof org.kylepitcock.legacyconnector.v1_7_10.entity.LegacyHost h ? h : null;
    }

    public DataWatcher getDataWatcher() {
        return dataWatcher;
    }

    // ------------------------------------------------------------------ Forge extended properties

    private java.util.Map<String, legacyconnector.v1_7_10.net.minecraftforge.common.IExtendedEntityProperties> mcc$extended;

    public String registerExtendedProperties(String identifier, legacyconnector.v1_7_10.net.minecraftforge.common.IExtendedEntityProperties properties) {
        if (identifier == null || properties == null) return identifier;
        if (mcc$extended == null) mcc$extended = new java.util.LinkedHashMap<>();
        String id = identifier;
        for (int i = 1; mcc$extended.containsKey(id); i++) id = identifier + i;
        mcc$extended.put(id, properties);
        properties.init(this, mcc$get$worldObj(this));
        return id;
    }

    public legacyconnector.v1_7_10.net.minecraftforge.common.IExtendedEntityProperties getExtendedProperties(String identifier) {
        return mcc$extended == null ? null : mcc$extended.get(identifier);
    }

    /** Saves every extended property into its own compound under {@code tag} (legacy layout). */
    public final void mcc$saveExtended(NBTTagCompound tag) {
        if (mcc$extended == null) return;
        mcc$extended.forEach((id, p) -> {
            try {
                p.saveNBTData(tag);
            } catch (RuntimeException | LinkageError e) {
                org.slf4j.LoggerFactory.getLogger("LegacyConnector/Entities").warn("Extended properties {} failed to save: {}", id, e.toString());
            }
        });
    }

    public final void mcc$loadExtended(NBTTagCompound tag) {
        if (mcc$extended == null) return;
        mcc$extended.forEach((id, p) -> {
            try {
                p.loadNBTData(tag);
            } catch (RuntimeException | LinkageError e) {
                org.slf4j.LoggerFactory.getLogger("LegacyConnector/Entities").warn("Extended properties {} failed to load: {}", id, e.toString());
            }
        });
    }

    public final boolean mcc$hasExtended() {
        return mcc$extended != null && !mcc$extended.isEmpty();
    }

    public static double mcc$get$posX(Entity e) {
        var m = e.mcc$entity;
        return m != null ? m.getX() : e.mcc$local_posX;
    }

    public static void mcc$set$posX(Entity e, double v) {
        var m = e.mcc$entity;
        if (m != null) {
            m.setPos(v, m.getY(), m.getZ());
        } else {
            e.mcc$local_posX = v;
        }
    }

    public static double mcc$get$posY(Entity e) {
        var m = e.mcc$entity;
        return m != null ? m.getY() + e.mcc$posYShift : e.mcc$local_posY;
    }

    public static void mcc$set$posY(Entity e, double v) {
        var m = e.mcc$entity;
        if (m != null) {
            m.setPos(m.getX(), v - e.mcc$posYShift, m.getZ());
        } else {
            e.mcc$local_posY = v;
        }
    }

    public static double mcc$get$posZ(Entity e) {
        var m = e.mcc$entity;
        return m != null ? m.getZ() : e.mcc$local_posZ;
    }

    public static void mcc$set$posZ(Entity e, double v) {
        var m = e.mcc$entity;
        if (m != null) {
            m.setPos(m.getX(), m.getY(), v);
        } else {
            e.mcc$local_posZ = v;
        }
    }

    public static double mcc$get$prevPosX(Entity e) {
        var m = e.mcc$entity;
        return m != null ? m.xo : e.mcc$local_prevPosX;
    }

    public static void mcc$set$prevPosX(Entity e, double v) {
        var m = e.mcc$entity;
        if (m != null) {
            m.xo = v;
        } else {
            e.mcc$local_prevPosX = v;
        }
    }

    public static double mcc$get$prevPosY(Entity e) {
        var m = e.mcc$entity;
        return m != null ? m.yo + e.mcc$posYShift : e.mcc$local_prevPosY;
    }

    public static void mcc$set$prevPosY(Entity e, double v) {
        var m = e.mcc$entity;
        if (m != null) {
            m.yo = v - e.mcc$posYShift;
        } else {
            e.mcc$local_prevPosY = v;
        }
    }

    public static double mcc$get$prevPosZ(Entity e) {
        var m = e.mcc$entity;
        return m != null ? m.zo : e.mcc$local_prevPosZ;
    }

    public static void mcc$set$prevPosZ(Entity e, double v) {
        var m = e.mcc$entity;
        if (m != null) {
            m.zo = v;
        } else {
            e.mcc$local_prevPosZ = v;
        }
    }

    public static double mcc$get$lastTickPosX(Entity e) {
        var m = e.mcc$entity;
        return m != null ? m.xOld : e.mcc$local_lastTickPosX;
    }

    public static void mcc$set$lastTickPosX(Entity e, double v) {
        var m = e.mcc$entity;
        if (m != null) {
            m.xOld = v;
        } else {
            e.mcc$local_lastTickPosX = v;
        }
    }

    public static double mcc$get$lastTickPosY(Entity e) {
        var m = e.mcc$entity;
        return m != null ? m.yOld + e.mcc$posYShift : e.mcc$local_lastTickPosY;
    }

    public static void mcc$set$lastTickPosY(Entity e, double v) {
        var m = e.mcc$entity;
        if (m != null) {
            m.yOld = v - e.mcc$posYShift;
        } else {
            e.mcc$local_lastTickPosY = v;
        }
    }

    public static double mcc$get$lastTickPosZ(Entity e) {
        var m = e.mcc$entity;
        return m != null ? m.zOld : e.mcc$local_lastTickPosZ;
    }

    public static void mcc$set$lastTickPosZ(Entity e, double v) {
        var m = e.mcc$entity;
        if (m != null) {
            m.zOld = v;
        } else {
            e.mcc$local_lastTickPosZ = v;
        }
    }

    public static double mcc$get$motionX(Entity e) {
        var m = e.mcc$entity;
        return m != null ? m.getDeltaMovement().x : e.mcc$local_motionX;
    }

    public static void mcc$set$motionX(Entity e, double v) {
        var m = e.mcc$entity;
        if (m != null) {
            m.setDeltaMovement(v, m.getDeltaMovement().y, m.getDeltaMovement().z); m.hurtMarked = true;
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
            m.setDeltaMovement(m.getDeltaMovement().x, v, m.getDeltaMovement().z); m.hurtMarked = true;
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
            m.setDeltaMovement(m.getDeltaMovement().x, m.getDeltaMovement().y, v); m.hurtMarked = true;
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
        if (m != null) {
            m.setYRot(v);
        } else {
            e.mcc$local_rotationYaw = v;
        }
    }

    public static float mcc$get$rotationPitch(Entity e) {
        var m = e.mcc$entity;
        return m != null ? m.getXRot() : e.mcc$local_rotationPitch;
    }

    public static void mcc$set$rotationPitch(Entity e, float v) {
        var m = e.mcc$entity;
        if (m != null) {
            m.setXRot(v);
        } else {
            e.mcc$local_rotationPitch = v;
        }
    }

    public static float mcc$get$prevRotationYaw(Entity e) {
        var m = e.mcc$entity;
        return m != null ? m.yRotO : e.mcc$local_prevRotationYaw;
    }

    public static void mcc$set$prevRotationYaw(Entity e, float v) {
        var m = e.mcc$entity;
        if (m != null) {
            m.yRotO = v;
        } else {
            e.mcc$local_prevRotationYaw = v;
        }
    }

    public static float mcc$get$prevRotationPitch(Entity e) {
        var m = e.mcc$entity;
        return m != null ? m.xRotO : e.mcc$local_prevRotationPitch;
    }

    public static void mcc$set$prevRotationPitch(Entity e, float v) {
        var m = e.mcc$entity;
        if (m != null) {
            m.xRotO = v;
        } else {
            e.mcc$local_prevRotationPitch = v;
        }
    }

    public static boolean mcc$get$onGround(Entity e) {
        var m = e.mcc$entity;
        return m != null ? m.onGround() : e.mcc$local_onGround;
    }

    public static void mcc$set$onGround(Entity e, boolean v) {
        var m = e.mcc$entity;
        if (m != null) {
            m.setOnGround(v);
        } else {
            e.mcc$local_onGround = v;
        }
    }

    public static boolean mcc$get$isDead(Entity e) {
        var m = e.mcc$entity;
        return m != null ? m.isRemoved() : e.mcc$local_isDead;
    }

    /** Whether the entity is in its world (JourneyMap maps only while the player is: the declared field stayed false). */
    public static boolean mcc$get$addedToChunk(Entity e) {
        var m = e.mcc$entity;
        return m != null ? m.isAddedToLevel() && !m.isRemoved() : e.mcc$local_addedToChunk;
    }

    /** Chunk bookkeeping of the legacy world: the modern entity keeps its own. */
    public static void mcc$set$addedToChunk(Entity e, boolean v) {
        e.mcc$local_addedToChunk = v;
    }

    public static void mcc$set$isDead(Entity e, boolean v) {
        var m = e.mcc$entity;
        if (m != null) {
            if (v) m.discard();
        } else {
            e.mcc$local_isDead = v;
        }
    }

    public static int mcc$get$ticksExisted(Entity e) {
        var m = e.mcc$entity;
        return m != null ? m.tickCount : e.mcc$local_ticksExisted;
    }

    public static void mcc$set$ticksExisted(Entity e, int v) {
        var m = e.mcc$entity;
        if (m != null) {
            m.tickCount = v;
        } else {
            e.mcc$local_ticksExisted = v;
        }
    }

    public static float mcc$get$fallDistance(Entity e) {
        var m = e.mcc$entity;
        return m != null ? m.fallDistance : e.mcc$local_fallDistance;
    }

    public static void mcc$set$fallDistance(Entity e, float v) {
        var m = e.mcc$entity;
        if (m != null) {
            m.fallDistance = v;
        } else {
            e.mcc$local_fallDistance = v;
        }
    }

    public static boolean mcc$get$isAirBorne(Entity e) {
        var m = e.mcc$entity;
        return m != null ? m.hasImpulse : e.mcc$local_isAirBorne;
    }

    public static void mcc$set$isAirBorne(Entity e, boolean v) {
        var m = e.mcc$entity;
        if (m != null) {
            m.hasImpulse = v;
        } else {
            e.mcc$local_isAirBorne = v;
        }
    }

    public static boolean mcc$get$isCollidedHorizontally(Entity e) {
        var m = e.mcc$entity;
        return m != null ? m.horizontalCollision : e.mcc$local_isCollidedHorizontally;
    }

    public static void mcc$set$isCollidedHorizontally(Entity e, boolean v) {
        var m = e.mcc$entity;
        if (m != null) {
            m.horizontalCollision = v;
        } else {
            e.mcc$local_isCollidedHorizontally = v;
        }
    }

    public static boolean mcc$get$isCollidedVertically(Entity e) {
        var m = e.mcc$entity;
        return m != null ? m.verticalCollision : e.mcc$local_isCollidedVertically;
    }

    public static void mcc$set$isCollidedVertically(Entity e, boolean v) {
        var m = e.mcc$entity;
        if (m != null) {
            m.verticalCollision = v;
        } else {
            e.mcc$local_isCollidedVertically = v;
        }
    }

    public static boolean mcc$get$isCollided(Entity e) {
        var m = e.mcc$entity;
        return m != null ? m.horizontalCollision || m.verticalCollision : e.mcc$local_isCollided;
    }

    public static void mcc$set$isCollided(Entity e, boolean v) {
        var m = e.mcc$entity;
        if (m != null) {
            // read-only in modern Minecraft;
        } else {
            e.mcc$local_isCollided = v;
        }
    }

    public static int mcc$get$hurtResistantTime(Entity e) {
        var m = e.mcc$entity;
        return m != null ? m.invulnerableTime : e.mcc$local_hurtResistantTime;
    }

    public static void mcc$set$hurtResistantTime(Entity e, int v) {
        var m = e.mcc$entity;
        if (m != null) {
            m.invulnerableTime = v;
        } else {
            e.mcc$local_hurtResistantTime = v;
        }
    }

    public static boolean mcc$get$noClip(Entity e) {
        var m = e.mcc$entity;
        return m != null ? m.noPhysics : e.mcc$local_noClip;
    }

    public static void mcc$set$noClip(Entity e, boolean v) {
        var m = e.mcc$entity;
        if (m != null) {
            m.noPhysics = v;
        } else {
            e.mcc$local_noClip = v;
        }
    }

    public static float mcc$get$width(Entity e) {
        var m = e.mcc$entity;
        return m != null ? m.getBbWidth() : e.mcc$local_width;
    }

    public static void mcc$set$width(Entity e, float v) {
        var m = e.mcc$entity;
        if (m != null) {
            // read-only in modern Minecraft;
        } else {
            e.mcc$local_width = v;
        }
    }

    public static float mcc$get$height(Entity e) {
        var m = e.mcc$entity;
        return m != null ? m.getBbHeight() : e.mcc$local_height;
    }

    public static void mcc$set$height(Entity e, float v) {
        var m = e.mcc$entity;
        if (m != null) {
            // read-only in modern Minecraft;
        } else {
            e.mcc$local_height = v;
        }
    }

    public static float mcc$get$stepHeight(Entity e) {
        var m = e.mcc$entity;
        return m != null ? m.maxUpStep() : e.mcc$local_stepHeight;
    }

    public static void mcc$set$stepHeight(Entity e, float v) {
        var m = e.mcc$entity;
        if (m != null) {
            // read-only in modern Minecraft;
        } else {
            e.mcc$local_stepHeight = v;
        }
    }

    public static int mcc$get$timeUntilPortal(Entity e) {
        var m = e.mcc$entity;
        return m != null ? m.getPortalCooldown() : e.mcc$local_timeUntilPortal;
    }

    public static void mcc$set$timeUntilPortal(Entity e, int v) {
        var m = e.mcc$entity;
        if (m != null) {
            m.setPortalCooldown(v);
        } else {
            e.mcc$local_timeUntilPortal = v;
        }
    }

    public static boolean mcc$get$inWater(Entity e) {
        var m = e.mcc$entity;
        return m != null ? m.isInWater() : e.mcc$local_inWater;
    }

    public static void mcc$set$inWater(Entity e, boolean v) {
        var m = e.mcc$entity;
        if (m != null) {
            // read-only in modern Minecraft;
        } else {
            e.mcc$local_inWater = v;
        }
    }

    public static int mcc$get$dimension(Entity e) {
        var m = e.mcc$entity;
        return m != null ? WorldProvider.dimensionId(m.level().dimension()) : e.mcc$local_dimension;
    }

    public static void mcc$set$dimension(Entity e, int v) {
        var m = e.mcc$entity;
        if (m != null) {
            // read-only in modern Minecraft;
        } else {
            e.mcc$local_dimension = v;
        }
    }

    public static boolean mcc$get$velocityChanged(Entity e) {
        var m = e.mcc$entity;
        return m != null ? m.hurtMarked : e.mcc$local_velocityChanged;
    }

    public static void mcc$set$velocityChanged(Entity e, boolean v) {
        var m = e.mcc$entity;
        if (m != null) {
            m.hurtMarked = v;
        } else {
            e.mcc$local_velocityChanged = v;
        }
    }

    /**
     * The level accessor to answer {@code worldObj} with instead of the entity's own level: spawn checks during chunk
     * generation run against the generation region (asking the real level for a chunk under generation deadlocks).
     */
    public net.minecraft.world.level.LevelAccessor mcc$worldOverride;

    public static World mcc$get$worldObj(Entity e) {
        var override = e.mcc$worldOverride;
        if (override != null) return World.mcc$wrap(override);
        var m = e.mcc$entity;
        return m != null ? World.mcc$wrap(m.level()) : e.mcc$world;
    }

    public static AxisAlignedBB mcc$get$boundingBox(Entity e) {
        var m = e.mcc$entity;
        return m != null ? AxisAlignedBB.mcc$of(m.getBoundingBox()) : AxisAlignedBB.getBoundingBox(e.mcc$local_posX - 0.3, e.mcc$local_posY, e.mcc$local_posZ - 0.3, e.mcc$local_posX + 0.3, e.mcc$local_posY + 1.8, e.mcc$local_posZ + 0.3);
    }

    public static Entity mcc$get$riddenByEntity(Entity e) {
        var m = e.mcc$entity;
        return m != null ? EntityBridge.wrap(m.getFirstPassenger()) : null;
    }

    public static Entity mcc$get$ridingEntity(Entity e) {
        var m = e.mcc$entity;
        return m != null ? EntityBridge.wrap(m.getVehicle()) : null;
    }

    public static java.util.UUID mcc$get$entityUniqueID(Entity e) {
        var m = e.mcc$entity;
        return m != null ? m.getUUID() : e.mcc$uuid;
    }

    // ------------------------------------------------------------------ shim-internal accessors

    public double mcc$x() {
        return mcc$get$posX(this);
    }

    public double mcc$y() {
        return mcc$get$posY(this);
    }

    public double mcc$z() {
        return mcc$get$posZ(this);
    }

    public net.minecraft.world.entity.Entity mcc$entity() {
        return mcc$entity;
    }

    public World mcc$world() {
        return mcc$get$worldObj(this);
    }

    // ------------------------------------------------------------------ API

    public int getEntityId() {
        return mcc$entity == null ? mcc$localId : mcc$entity.getId();
    }

    private int mcc$localId = -1;

    /**
     * Mods number client-side entities they build from their own spawn packets before adding them. The id of a modern entity
     * already in a level is left alone: its level looks entities up by id and would lose it.
     */
    public void setEntityId(int id) {
        if (mcc$entity == null) mcc$localId = id;
        else if (!mcc$entity.isAddedToLevel()) mcc$entity.setId(id);
    }

    /** Forge: the id that persists across sessions (the player's profile id). */
    public UUID getPersistentID() {
        return getUniqueID();
    }

    public UUID getUniqueID() {
        return mcc$get$entityUniqueID(this);
    }

    public String getCommandSenderName() {
        return mcc$entity == null ? getClass().getSimpleName() : mcc$entity.getName().getString();
    }

    public IChatComponent getFormattedCommandSenderName() {
        return new ChatComponentText(getCommandSenderName());
    }

    @Override
    public void addChatMessage(IChatComponent message) {
        if (mcc$entity != null) mcc$entity.sendSystemMessage(message.mcc$toModern());
    }

    @Override
    public boolean canCommandSenderUseCommand(int level, String command) {
        return false;
    }

    @Override
    public ChunkCoordinates getPlayerCoordinates() {
        return new ChunkCoordinates((int) Math.floor(mcc$x()), (int) Math.floor(mcc$y() + 0.5), (int) Math.floor(mcc$z()));
    }

    @Override
    public World getEntityWorld() {
        return mcc$world();
    }

    public void setWorld(World world) {
        this.mcc$world = world;
    }

    public void setDead() {
        if (mcc$entity != null) mcc$entity.discard();
        else mcc$set$isDead(this, true);
    }

    public boolean isEntityAlive() {
        return mcc$entity != null ? mcc$entity.isAlive() : !mcc$get$isDead(this);
    }

    /** Legacy y is {@code posY}: the modern feet y plus {@link #mcc$posYShift} (eye level for the local player, mid-height for carts). */
    public void setPosition(double x, double y, double z) {
        if (mcc$entity != null) mcc$entity.setPos(x, y - mcc$posYShift, z);
        else {
            mcc$set$posX(this, x);
            mcc$set$posY(this, y);
            mcc$set$posZ(this, z);
        }
    }

    public void setLocationAndAngles(double x, double y, double z, float yaw, float pitch) {
        if (mcc$entity != null) mcc$entity.moveTo(x, y - mcc$posYShift, z, yaw, pitch);
        else {
            setPosition(x, y, z);
            mcc$set$rotationYaw(this, yaw);
            mcc$set$rotationPitch(this, pitch);
        }
    }

    public void setPositionAndRotation(double x, double y, double z, float yaw, float pitch) {
        setLocationAndAngles(x, y, z, yaw, pitch);
    }

    public void setRotation(float yaw, float pitch) {
        mcc$set$rotationYaw(this, yaw % 360.0F);
        mcc$set$rotationPitch(this, pitch % 360.0F);
    }

    public void setVelocity(double x, double y, double z) {
        if (mcc$entity != null) mcc$entity.setDeltaMovement(x, y, z);
        else {
            mcc$set$motionX(this, x);
            mcc$set$motionY(this, y);
            mcc$set$motionZ(this, z);
        }
    }

    public void addVelocity(double x, double y, double z) {
        if (mcc$entity != null) {
            mcc$entity.push(x, y, z);
        } else {
            setVelocity(mcc$get$motionX(this) + x, mcc$get$motionY(this) + y, mcc$get$motionZ(this) + z);
        }
    }

    /** Speeds the entity up along its facing by the given input (1.7.10's movement and flight steering; Fisk's web swinging). */
    public void moveFlying(float strafe, float forward, float friction) {
        if (mcc$entity != null) mcc$entity.moveRelative(friction, new net.minecraft.world.phys.Vec3(strafe, 0, forward));
    }

    public void moveEntity(double dx, double dy, double dz) {
        if (mcc$entity != null) mcc$entity.move(net.minecraft.world.entity.MoverType.SELF, new net.minecraft.world.phys.Vec3(dx, dy, dz));
        else setPosition(mcc$x() + dx, mcc$y() + dy, mcc$z() + dz);
    }

    public float getDistanceToEntity(Entity other) {
        return (float) Math.sqrt(getDistanceSqToEntity(other));
    }

    public double getDistanceSqToEntity(Entity other) {
        return getDistanceSq(other.mcc$x(), other.mcc$y(), other.mcc$z());
    }

    public double getDistanceSq(double x, double y, double z) {
        double dx = mcc$x() - x, dy = mcc$y() - y, dz = mcc$z() - z;
        return dx * dx + dy * dy + dz * dz;
    }

    public double getDistance(double x, double y, double z) {
        return Math.sqrt(getDistanceSq(x, y, z));
    }

    /** The position interpolated between the previous and the current tick ({@code 1.0F}: the current one). */
    public Vec3 getPosition(float partialTicks) {
        if (partialTicks == 1.0F) return Vec3.createVectorHelper(mcc$get$posX(this), mcc$get$posY(this), mcc$get$posZ(this));
        double px = mcc$get$prevPosX(this), py = mcc$get$prevPosY(this), pz = mcc$get$prevPosZ(this);
        return Vec3.createVectorHelper(px + (mcc$get$posX(this) - px) * partialTicks, py + (mcc$get$posY(this) - py) * partialTicks,
                pz + (mcc$get$posZ(this) - pz) * partialTicks);
    }

    /** Standing in a portal block; the modern portal blocks handle the entity themselves, so nothing is needed here. */
    /** How far away the entity is still drawn, relative to the default (arrows and other small projectiles use a larger value). */
    public double renderDistanceWeight = 1.0D;

    /** Whether the entity is near enough to the point (the camera) to be drawn (mods skip their own drawing on it). */
    public boolean isInRangeToRender3d(double x, double y, double z) {
        double dx = mcc$x() - x;
        double dy = mcc$y() - y;
        double dz = mcc$z() - z;
        return isInRangeToRenderDist(dx * dx + dy * dy + dz * dz);
    }

    public boolean isInRangeToRenderDist(double distanceSq) {
        double size = mcc$get$boundingBox(this).getAverageEdgeLength() * 64.0D * renderDistanceWeight;
        return distanceSq < size * size;
    }

    /** Applies the effects of the blocks the entity overlaps; the modern game does this for every entity itself. */
    public void doBlockCollisions() {
    }

    public void setInPortal() {
    }

    public void setInWeb() {
    }

    public float getShadowSize() {
        return mcc$get$height(this) / 2.0F;
    }

    public Vec3 getLookVec() {
        if (mcc$entity == null) return Vec3.createVectorHelper(0, 0, 1);
        var v = mcc$entity.getLookAngle();
        return Vec3.createVectorHelper(v.x, v.y, v.z);
    }

    /** The look direction, interpolated by the partial tick (1.0 = current). */
    public Vec3 getLook(float partialTick) {
        if (mcc$entity == null) return Vec3.createVectorHelper(0, 0, 1);
        var v = mcc$entity.getViewVector(partialTick);
        return Vec3.createVectorHelper(v.x, v.y, v.z);
    }

    public float getCollisionBorderSize() {
        return 0.1F;
    }

    public boolean doesEntityNotTriggerPressurePlate() {
        return false;
    }

    public boolean hitByEntity(Entity attacker) {
        return false;
    }

    public void setBeenAttacked() {
        if (mcc$entity != null) mcc$entity.hurtMarked = true;
    }

    public boolean canAttackWithItem() {
        return true;
    }

    public int getPortalCooldown() {
        return 900;
    }

    public boolean pushOutOfBlocks(double x, double y, double z) {
        return false;
    }

    public float getEyeHeight() {
        return mcc$entity == null ? 1.62F : mcc$entity.getEyeHeight();
    }

    public boolean isSneaking() {
        return mcc$entity != null && mcc$entity.isShiftKeyDown();
    }

    public void setSneaking(boolean sneaking) {
        if (mcc$entity != null) mcc$entity.setShiftKeyDown(sneaking);
    }

    public boolean isSprinting() {
        return mcc$entity != null && mcc$entity.isSprinting();
    }

    public void setSprinting(boolean sprinting) {
        if (mcc$entity != null) mcc$entity.setSprinting(sprinting);
    }

    public boolean isInvisible() {
        return mcc$entity != null && mcc$entity.isInvisible();
    }

    public void setInvisible(boolean invisible) {
        if (mcc$entity != null) mcc$entity.setInvisible(invisible);
    }

    /** Invisible to this player: teammates who may see friendly invisibles (and spectators) still see it. */
    public boolean isInvisibleToPlayer(legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer player) {
        if (mcc$entity == null || player == null || player.mcc$entity == null) return isInvisible();
        return player.mcc$entity instanceof net.minecraft.world.entity.player.Player p ? mcc$entity.isInvisibleTo(p) : isInvisible();
    }

    /** Bit {@code flag} of the shared flags byte (watcher slot 0: 0 burning, 1 sneaking, 3 sprinting, 4 eating, 5 invisible). */
    protected boolean getFlag(int flag) {
        return (dataWatcher.getWatchableObjectByte(0) & 1 << flag) != 0;
    }

    protected void setFlag(int flag, boolean value) {
        byte flags = dataWatcher.getWatchableObjectByte(0);
        dataWatcher.updateObject(0, Byte.valueOf((byte) (value ? flags | 1 << flag : flags & ~(1 << flag))));
    }

    /** Ticks the entity must stand in a portal before it travels (0; players override it). */
    public int getMaxInPortalTime() {
        return 0;
    }

    public boolean isBurning() {
        return mcc$entity != null && mcc$entity.isOnFire();
    }

    public void setFire(int seconds) {
        if (mcc$entity != null) mcc$entity.igniteForSeconds(seconds);
    }

    public void extinguish() {
        if (mcc$entity != null) mcc$entity.clearFire();
    }

    public boolean isImmuneToFire() {
        return mcc$entity != null ? mcc$entity.fireImmune() : isImmuneToFire;
    }

    /** Whether this is the same entity as {@code other} (multi-part bosses compare their parts to the whole). */
    public boolean isEntityEqual(Entity other) {
        return this == other;
    }

    /** The extra hit boxes of a multi-part entity, or null. */
    public Entity[] getParts() {
        return null;
    }

    /** Forge: whether the entity counts as {@code type} for natural spawning limits. */
    public boolean isCreatureType(EnumCreatureType type, boolean forSpawnCount) {
        return false;
    }

    public boolean isInWater() {
        return mcc$entity != null && mcc$entity.isInWater();
    }

    /**
     * Pushes the entity along flowing water and returns whether it is in water; in water it stops burning and its fall resets.
     * Legacy entities whose onUpdate does not chain to the base tick (TNT, projectiles) call this themselves.
     */
    public boolean handleWaterMovement() {
        if (mcc$entity == null) return false;
        if (!mcc$entity.updateFluidHeightAndDoFluidPushing(net.minecraft.tags.FluidTags.WATER, 0.014D)) return false;
        mcc$entity.resetFallDistance();
        mcc$entity.clearFire();
        return true;
    }

    public boolean isWet() {
        return mcc$entity != null && mcc$entity.isInWaterRainOrBubble();
    }

    public boolean handleLavaMovement() {
        return mcc$entity != null && mcc$entity.isInLava();
    }

    public boolean isRiding() {
        return mcc$entity != null && mcc$entity.isPassenger();
    }

    public void mountEntity(Entity vehicle) {
        if (mcc$entity == null) return;
        if (vehicle == null) mcc$entity.stopRiding();
        else if (vehicle.mcc$entity != null) mcc$entity.startRiding(vehicle.mcc$entity, true);
    }

    public boolean isEntityInvulnerable() {
        return mcc$entity != null && mcc$entity.isInvulnerable();
    }

    public boolean attackEntityFrom(DamageSource source, float amount) {
        if (mcc$entity == null) return false;
        if (source == null) source = DamageSource.generic;
        if (mcc$entity instanceof org.kylepitcock.legacyconnector.v1_7_10.entity.LegacyHost h) {
            return h.mcc$superHurt(source.mcc$modern(mcc$entity.level()), amount);
        }
        return mcc$entity.hurt(source.mcc$modern(mcc$entity.level()), amount);
    }

    public void travelToDimension(int dimension) {
        EntityBridge.travelToDimension(this, dimension);
    }

    public void playSound(String name, float volume, float pitch) {
        if (mcc$entity != null) SoundBridge.play(mcc$entity.level(), null, mcc$x(), mcc$y(), mcc$z(), name, volume, pitch);
    }

    public EntityItem dropItem(Item item, int count) {
        return dropItemWithOffset(item, count, 0.0F);
    }

    public EntityItem dropItemWithOffset(Item item, int count, float offset) {
        return entityDropItem(new ItemStack(item, count, 0), offset);
    }

    public EntityItem entityDropItem(ItemStack stack, float offsetY) {
        if (stack == null || stack.mcc$count() == 0 || mcc$world().isRemote) return null;
        EntityItem item = new EntityItem(mcc$world(), mcc$x(), mcc$y() + offsetY, mcc$z(), stack);
        if (captureDrops) capturedDrops.add(item);
        else mcc$get$worldObj(this).spawnEntityInWorld(item);
        return item;
    }

    public void setSize(float width, float height) {
        var host = mcc$host();
        if (host != null) host.mcc$setSize(width, height);
        // 1.7.10 picked the tracker's position rounding from the width (HBM's TrackerUtil reads it)
        float f = width % 2.0F;
        myEntitySize = f < 0.375D ? EnumEntitySize.SIZE_1 : f < 0.75D ? EnumEntitySize.SIZE_2 : f < 1.0D ? EnumEntitySize.SIZE_3
                : f < 1.375D ? EnumEntitySize.SIZE_4 : f < 1.75D ? EnumEntitySize.SIZE_5 : EnumEntitySize.SIZE_6;
    }

    /** How the entity tracker rounds this entity's position to 1/32 block. */
    public EnumEntitySize myEntitySize = EnumEntitySize.SIZE_2;

    /** 1.7.10's tracker rounding per entity size. */
    public static enum EnumEntitySize {
        SIZE_1, SIZE_2, SIZE_3, SIZE_4, SIZE_5, SIZE_6;

        public int multiplyBy32AndRound(double value) {
            double frac = value - (Math.floor(value) + 0.5D);
            int floor = (int) Math.floor(value * 32.0D), ceil = (int) Math.ceil(value * 32.0D);
            return switch (this) {
                case SIZE_1 -> (frac < 0.0D ? frac < -0.3125D : frac < 0.3125D) ? ceil : floor;
                case SIZE_2 -> (frac < 0.0D ? frac < -0.3125D : frac < 0.3125D) ? floor : ceil;
                case SIZE_3 -> frac > 0.0D ? floor : ceil;
                case SIZE_4 -> (frac < 0.0D ? frac < -0.1875D : frac < 0.1875D) ? ceil : floor;
                case SIZE_5 -> (frac < 0.0D ? frac < -0.1875D : frac < 0.1875D) ? floor : ceil;
                case SIZE_6 -> frac > 0.0D ? ceil : floor;
            };
        }
    }

    protected boolean canTriggerWalking() {
        return true;
    }

    public boolean canBeCollidedWith() {
        return !mcc$hosted() && mcc$entity != null && mcc$entity.isPickable();
    }

    public boolean canBePushed() {
        return !mcc$hosted() && mcc$entity != null && mcc$entity.isPushable();
    }

    public AxisAlignedBB getBoundingBox() {
        return null;
    }

    public void onUpdate() {
        var host = mcc$host();
        if (host != null) host.mcc$superTick();
    }

    public void onEntityUpdate() {
        var host = mcc$host();
        if (host != null) host.mcc$superBaseTick();
    }

    public void onCollideWithPlayer(legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer player) {
    }

    public boolean interactFirst(legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer player) {
        return false;
    }

    /** Forge persistent data compound (stored in the modern entity's persistent data). */
    public NBTTagCompound getEntityData() {
        if (mcc$entity != null) {
            NBTTagCompound view = new NBTTagCompound(mcc$entity.getPersistentData());
            return view;
        }
        if (mcc$customData == null) mcc$customData = new NBTTagCompound();
        return mcc$customData;
    }

    public void writeToNBT(NBTTagCompound tag) {
        if (mcc$entity != null) mcc$entity.saveWithoutId(tag.mcc$modern());
        if (!mcc$hosted()) writeEntityToNBT(tag); // a host's save already includes the legacy data
    }

    public void readFromNBT(NBTTagCompound tag) {
        if (mcc$entity != null) mcc$entity.load(tag.mcc$modern());
        if (!mcc$hosted()) readEntityFromNBT(tag);
    }

    public boolean writeToNBTOptional(NBTTagCompound tag) {
        if (mcc$get$isDead(this) || mcc$entity == null) return false;
        if (mcc$entity.getType().canSerialize()) {
            tag.setString("id", net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(mcc$entity.getType()).toString());
        }
        writeToNBT(tag);
        return true;
    }

    public String getEntityString() {
        return EntityBridge.legacyName(this);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Entity other)) return false;
        if (mcc$entity != null) return mcc$entity == other.mcc$entity;
        return this == o;
    }

    @Override
    public int hashCode() {
        return mcc$entity != null ? mcc$entity.hashCode() : System.identityHashCode(this);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + getCommandSenderName() + "]";
    }

    public void handleHealthUpdate(byte state) {
    }

    public int getAir() {
        return mcc$entity != null ? mcc$entity.getAirSupply() : 300;
    }

    public void setAir(int air) {
        if (mcc$entity != null) mcc$entity.setAirSupply(air);
    }

    public boolean isInsideOfMaterial(legacyconnector.v1_7_10.net.minecraft.block.material.Material material) {
        if (mcc$entity == null) return false;
        if (material == legacyconnector.v1_7_10.net.minecraft.block.material.Material.water) return mcc$entity.isEyeInFluid(net.minecraft.tags.FluidTags.WATER);
        if (material == legacyconnector.v1_7_10.net.minecraft.block.material.Material.lava) return mcc$entity.isEyeInFluid(net.minecraft.tags.FluidTags.LAVA);
        return false;
    }

    public void copyDataFrom(Entity other, boolean full) {
        NBTTagCompound tag = new NBTTagCompound();
        other.writeToNBT(tag);
        tag.removeTag("Dimension");
        readFromNBT(tag);
    }

    public int getBrightnessForRender(float partial) {
        if (mcc$entity == null) return 0;
        var pos = mcc$entity.blockPosition();
        int block = mcc$entity.level().getBrightness(net.minecraft.world.level.LightLayer.BLOCK, pos);
        int sky = mcc$entity.level().getBrightness(net.minecraft.world.level.LightLayer.SKY, pos);
        return sky << 20 | block << 4;
    }

    public void setCurrentItemOrArmor(int slot, legacyconnector.v1_7_10.net.minecraft.item.ItemStack stack) {
    }

    public void applyEntityCollision(Entity other) {
    }

    public float getBrightness(float partial) {
        if (mcc$entity == null) return 1.0F;
        return mcc$entity.level().getLightEngine().getRawBrightness(mcc$entity.blockPosition(), 0) / 15.0F;
    }

    public double getYOffset() {
        return yOffset;
    }

    /** How well the block at the position resists this entity's explosion (Forge, func_145772_a in vanilla). */
    public float getExplosionResistance(legacyconnector.v1_7_10.net.minecraft.world.Explosion explosion, World world, int x, int y, int z,
            legacyconnector.v1_7_10.net.minecraft.block.Block block) {
        return block.getExplosionResistance(this);
    }

    /** Whether the explosion may destroy the block (Forge's shouldExplodeBlock). */
    public boolean func_145774_a(legacyconnector.v1_7_10.net.minecraft.world.Explosion explosion, World world, int x, int y, int z,
            legacyconnector.v1_7_10.net.minecraft.block.Block block, float power) {
        return true;
    }

    protected legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagList newDoubleNBTList(double... values) {
        legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagList list = new legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagList();
        for (double v : values) list.appendTag(new legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagDouble(v));
        return list;
    }

    /** Fall bookkeeping of the entity; the modern game tracks fall distance itself. */
    protected void updateFallState(double y, boolean onGround) {
    }

    /** Whether the entity's head is inside an opaque block (it suffocates). */
    public boolean isEntityInsideOpaqueBlock() {
        return false;
    }

    // ------------------------------------------------------------------ Forge drop capture

    /** Forge: while set, {@link #entityDropItem} collects the drops in {@link #capturedDrops} instead of spawning them. */
    public boolean captureDrops;
    public java.util.ArrayList<EntityItem> capturedDrops = new java.util.ArrayList<>();

    // ------------------------------------------------------------------ falling, the void, lightning

    /**
     * The entity landed after falling {@code distance} blocks: passes the fall on to the rider (living entities take damage).
     * Hosts route the modern causeFallDamage here so mods' overrides run; the base hands it back to the vanilla handling.
     */
    protected void fall(float distance) {
        var host = mcc$host();
        if (host != null) host.mcc$superFall(distance);
        else if (mcc$entity != null) mcc$entity.causeFallDamage(distance, 1.0F, mcc$entity.damageSources().fall());
    }

    public final void mcc$fall(float distance) {
        fall(distance);
    }

    /** Fell out of the world: removed (living entities take 4 void damage per tick instead). */
    protected void kill() {
        var host = mcc$host();
        if (host != null) {
            host.mcc$superKill();
        } else if (mcc$entity instanceof net.minecraft.world.entity.LivingEntity l) {
            l.hurt(l.damageSources().fellOutOfWorld(), 4.0F);
        } else {
            setDead();
        }
    }

    public final void mcc$kill() {
        kill();
    }

    /** Struck by lightning: set on fire and hurt (creepers become charged, pigs turn into zombie pigmen, ...). */
    public void onStruckByLightning(legacyconnector.v1_7_10.net.minecraft.entity.effect.EntityLightningBolt bolt) {
        if (mcc$entity == null || !(mcc$entity.level() instanceof net.minecraft.server.level.ServerLevel level)) return;
        net.minecraft.world.entity.LightningBolt modern = bolt != null && bolt.mcc$entity instanceof net.minecraft.world.entity.LightningBolt b ? b : null;
        if (modern == null) {
            modern = net.minecraft.world.entity.EntityType.LIGHTNING_BOLT.create(level);
            if (modern == null) return;
            if (bolt != null) modern.moveTo(mcc$get$posX(bolt), mcc$get$posY(bolt), mcc$get$posZ(bolt));
            else modern.moveTo(mcc$entity.position());
        }
        var host = mcc$host();
        if (host != null) host.mcc$superThunderHit(level, modern);
        else mcc$entity.thunderHit(level, modern);
    }

    // ------------------------------------------------------------------ riding

    /** How high above this entity's position a rider sits (three quarters of the height). */
    public double getMountedYOffset() {
        return mcc$get$height(this) * 0.75D;
    }

    /**
     * Places the rider on this entity. A class that changes {@link #getMountedYOffset} gets the 1.7.10 placement (rider's feet at
     * this entity's y plus the mounted offset plus the rider's own {@link #getYOffset}); otherwise the vanilla seat is used.
     */
    public void updateRiderPosition() {
        Entity rider = mcc$get$riddenByEntity(this);
        if (rider == null || rider.mcc$entity == null || mcc$entity == null) return;
        if (org.kylepitcock.legacyconnector.v1_7_10.entity.LegacyHosts.overrides(getClass(), "getMountedYOffset")) {
            rider.mcc$entity.setPos(mcc$entity.getX(), mcc$get$posY(this) + getMountedYOffset() + rider.getYOffset() - rider.mcc$posYShift, mcc$entity.getZ());
            return;
        }
        var host = mcc$host();
        if (host != null) host.mcc$superPositionRider(rider.mcc$entity);
        else mcc$entity.positionRider(rider.mcc$entity);
    }

    /** One tick while riding: the entity's own update, then its vehicle places it (the modern rideTick). */
    public void updateRidden() {
        var host = mcc$host();
        if (host != null) host.mcc$superRideTick();
        else if (mcc$entity != null) mcc$entity.rideTick();
    }

    /** Forge: whether the rider is drawn sitting. */
    public boolean shouldRiderSit() {
        return true;
    }

    /** Forge: whether the rider may interact with the world while riding (only through this entity otherwise). */
    public boolean canRiderInteract() {
        return false;
    }

    /** Moves an entity that is getting off {@code vehicle} to a free spot next to it. */
    public void dismountEntity(Entity vehicle) {
        if (mcc$entity == null) return;
        if (vehicle != null && vehicle.mcc$entity != null && mcc$entity instanceof net.minecraft.world.entity.LivingEntity l) {
            var spot = vehicle.mcc$entity.getDismountLocationForPassenger(l);
            mcc$entity.dismountTo(spot.x, spot.y, spot.z);
        } else if (vehicle != null) {
            setPosition(mcc$get$posX(vehicle), mcc$get$boundingBox(vehicle).maxY + 0.001D, mcc$get$posZ(vehicle));
        }
    }

    // ------------------------------------------------------------------ position and rotation

    /** Takes the position and rotation of {@code other}. */
    public void copyLocationAndAnglesFrom(Entity other) {
        setLocationAndAngles(mcc$get$posX(other), mcc$get$posY(other), mcc$get$posZ(other), mcc$get$rotationYaw(other), mcc$get$rotationPitch(other));
    }

    /**
     * Client: the position and rotation from the server, reached over {@code increments} ticks (the modern lerpTo). Hosts route
     * the modern lerpTo here so mods' overrides run ({@code y} is the legacy posY, so the modern y plus {@link #mcc$posYShift}).
     */
    public void setPositionAndRotation2(double x, double y, double z, float yaw, float pitch, int increments) {
        var host = mcc$host();
        if (host != null) {
            host.mcc$superLerpTo(x, y - mcc$posYShift, z, yaw, pitch, increments);
        } else if (mcc$entity != null) {
            mcc$entity.lerpTo(x, y - mcc$posYShift, z, yaw, pitch, increments);
        } else {
            setPosition(x, y, z);
            setRotation(yaw, pitch);
        }
    }

    /** Turns the entity by mouse movement (0.15 degrees per unit; the pitch is clamped to +-90). */
    public void setAngles(float yaw, float pitch) {
        if (mcc$entity != null) {
            mcc$entity.turn(yaw, -pitch);
            return;
        }
        float oldPitch = mcc$local_rotationPitch, oldYaw = mcc$local_rotationYaw;
        mcc$local_rotationYaw = (float) (mcc$local_rotationYaw + yaw * 0.15D);
        mcc$local_rotationPitch = Math.max(-90.0F, Math.min(90.0F, (float) (mcc$local_rotationPitch - pitch * 0.15D)));
        mcc$local_prevRotationPitch += mcc$local_rotationPitch - oldPitch;
        mcc$local_prevRotationYaw += mcc$local_rotationYaw - oldYaw;
    }

    /** The head's yaw; only living entities turn their head (0 for everything else). */
    public float getRotationYawHead() {
        return 0.0F;
    }

    /** Box another entity collides with when it touches this one (boats and carts are solid), or null. */
    public AxisAlignedBB getCollisionBox(Entity other) {
        return null;
    }

    /** Forge: whether the entity is drawn in render pass {@code pass} (0 opaque, 1 translucent). */
    public boolean shouldRenderInPass(int pass) {
        return pass == 0;
    }

    /** Client: watcher slot {@code id} received a new value from the server. */
    public void func_145781_i(int id) {
    }

    /** Whether flowing water pushes the entity (flying players are not pushed). */
    public boolean isPushedByWater() {
        return true;
    }

    /** Saves this entity as a mount ("id" and its data); false when it is dead or has no name to be saved under. */
    public boolean writeMountToNBT(NBTTagCompound tag) {
        if (mcc$get$isDead(this) || mcc$entity == null || !mcc$entity.getType().canSerialize()) return false;
        tag.setString("id", net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(mcc$entity.getType()).toString());
        writeToNBT(tag);
        return true;
    }

    private boolean mcc$eating;

    /** Eating flag (bit 4 of the shared flags: horses and mods' eating animations). */
    public boolean isEating() {
        return mcc$eating || getFlag(4);
    }

    public void setEating(boolean eating) {
        mcc$eating = eating;
        setFlag(4, eating);
    }

    /** Called after this entity killed {@code victim} (zombies infect villagers). */
    public void onKillEntity(EntityLivingBase victim) {
    }

    /** Plays the step sound of the block walked on (snow on top of it wins; liquids are silent). */
    protected void playStepSound(int x, int y, int z, legacyconnector.v1_7_10.net.minecraft.block.Block block) {
        if (mcc$entity == null) return;
        var level = mcc$entity.level();
        var pos = new net.minecraft.core.BlockPos(x, y, z);
        var above = level.getBlockState(pos.above());
        var state = above.is(net.minecraft.world.level.block.Blocks.SNOW) ? above : level.getBlockState(pos);
        if (!state.liquid()) {
            var sound = state.getSoundType(level, pos, mcc$entity);
            mcc$entity.playSound(sound.getStepSound(), sound.getVolume() * 0.15F, sound.getPitch());
        }
    }

    public final void mcc$playStepSound(int x, int y, int z, legacyconnector.v1_7_10.net.minecraft.block.Block block) {
        playStepSound(x, y, z, block);
    }

    /** Adds this entity's type, id, name, position and motion to a crash report section. */
    public void addEntityCrashInfo(legacyconnector.v1_7_10.net.minecraft.crash.CrashReportCategory category) {
        category.addCrashSection("Entity Type", EntityList.getEntityString(this) + " (" + getClass().getCanonicalName() + ")");
        category.addCrashSection("Entity ID", getEntityId());
        category.addCrashSection("Entity Name", getCommandSenderName());
        double x = mcc$get$posX(this), y = mcc$get$posY(this), z = mcc$get$posZ(this);
        category.addCrashSection("Entity's Exact location", String.format(java.util.Locale.ROOT, "%.2f, %.2f, %.2f", x, y, z));
        category.addCrashSection("Entity's Block location", legacyconnector.v1_7_10.net.minecraft.crash.CrashReportCategory.getCoordinateInfo(
                (int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z)));
        category.addCrashSection("Entity's Momentum", String.format(java.util.Locale.ROOT, "%.2f, %.2f, %.2f", mcc$get$motionX(this),
                mcc$get$motionY(this), mcc$get$motionZ(this)));
    }
}
