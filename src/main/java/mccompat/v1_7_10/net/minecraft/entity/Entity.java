package mccompat.v1_7_10.net.minecraft.entity;

import java.util.Random;
import java.util.UUID;
import mccompat.v1_7_10.net.minecraft.entity.item.EntityItem;
import mccompat.v1_7_10.net.minecraft.item.Item;
import mccompat.v1_7_10.net.minecraft.item.ItemStack;
import mccompat.v1_7_10.net.minecraft.nbt.NBTTagCompound;
import mccompat.v1_7_10.net.minecraft.util.AxisAlignedBB;
import mccompat.v1_7_10.net.minecraft.util.ChunkCoordinates;
import mccompat.v1_7_10.net.minecraft.util.DamageSource;
import mccompat.v1_7_10.net.minecraft.util.IChatComponent;
import mccompat.v1_7_10.net.minecraft.util.ChatComponentText;
import mccompat.v1_7_10.net.minecraft.util.Vec3;
import mccompat.v1_7_10.net.minecraft.world.World;
import mccompat.v1_7_10.net.minecraft.world.WorldProvider;
import mccompat.v1_7_10.net.minecraft.command.ICommandSender;
import org.kylepitcock.mccompat.v1_7_10.bridge.EntityBridge;
import org.kylepitcock.mccompat.v1_7_10.bridge.SoundBridge;

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
        org.kylepitcock.mccompat.v1_7_10.entity.LegacyHost host = org.kylepitcock.mccompat.v1_7_10.entity.LegacyHosts.takeBinding();
        if (host == null && world != null) host = org.kylepitcock.mccompat.v1_7_10.entity.LegacyHosts.adopt(this, world);
        if (host != null) this.mcc$entity = host.mcc$self();
        entityInit();
        mcc$fireConstructing();
    }

    /** Forge's EntityConstructing (legacy fired it at the end of the base constructor). */
    public final void mcc$fireConstructing() {
        try {
            mccompat.v1_7_10.net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(
                    new mccompat.v1_7_10.net.minecraftforge.event.entity.EntityEvent.EntityConstructing(this));
        } catch (RuntimeException | LinkageError e) {
            org.slf4j.LoggerFactory.getLogger("Retrofit/Entities").warn("EntityConstructing handler failed for {}: {}", getClass().getName(), e.toString());
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
        return mcc$entity instanceof org.kylepitcock.mccompat.v1_7_10.entity.LegacyHost h && h.mcc$legacy() == this;
    }

    protected final org.kylepitcock.mccompat.v1_7_10.entity.LegacyHost mcc$host() {
        return mcc$entity instanceof org.kylepitcock.mccompat.v1_7_10.entity.LegacyHost h ? h : null;
    }

    public DataWatcher getDataWatcher() {
        return dataWatcher;
    }

    // ------------------------------------------------------------------ Forge extended properties

    private java.util.Map<String, mccompat.v1_7_10.net.minecraftforge.common.IExtendedEntityProperties> mcc$extended;

    public String registerExtendedProperties(String identifier, mccompat.v1_7_10.net.minecraftforge.common.IExtendedEntityProperties properties) {
        if (identifier == null || properties == null) return identifier;
        if (mcc$extended == null) mcc$extended = new java.util.LinkedHashMap<>();
        String id = identifier;
        for (int i = 1; mcc$extended.containsKey(id); i++) id = identifier + i;
        mcc$extended.put(id, properties);
        properties.init(this, mcc$world);
        return id;
    }

    public mccompat.v1_7_10.net.minecraftforge.common.IExtendedEntityProperties getExtendedProperties(String identifier) {
        return mcc$extended == null ? null : mcc$extended.get(identifier);
    }

    /** Saves every extended property into its own compound under {@code tag} (legacy layout). */
    public final void mcc$saveExtended(NBTTagCompound tag) {
        if (mcc$extended == null) return;
        mcc$extended.forEach((id, p) -> {
            try {
                p.saveNBTData(tag);
            } catch (RuntimeException | LinkageError e) {
                org.slf4j.LoggerFactory.getLogger("Retrofit/Entities").warn("Extended properties {} failed to save: {}", id, e.toString());
            }
        });
    }

    public final void mcc$loadExtended(NBTTagCompound tag) {
        if (mcc$extended == null) return;
        mcc$extended.forEach((id, p) -> {
            try {
                p.loadNBTData(tag);
            } catch (RuntimeException | LinkageError e) {
                org.slf4j.LoggerFactory.getLogger("Retrofit/Entities").warn("Extended properties {} failed to load: {}", id, e.toString());
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
        return m != null ? m.getY() : e.mcc$local_posY;
    }

    public static void mcc$set$posY(Entity e, double v) {
        var m = e.mcc$entity;
        if (m != null) {
            m.setPos(m.getX(), v, m.getZ());
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
        return m != null ? m.yo : e.mcc$local_prevPosY;
    }

    public static void mcc$set$prevPosY(Entity e, double v) {
        var m = e.mcc$entity;
        if (m != null) {
            m.yo = v;
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
        return m != null ? m.yOld : e.mcc$local_lastTickPosY;
    }

    public static void mcc$set$lastTickPosY(Entity e, double v) {
        var m = e.mcc$entity;
        if (m != null) {
            m.yOld = v;
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
        return mcc$entity == null ? -1 : mcc$entity.getId();
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

    public void setPosition(double x, double y, double z) {
        if (mcc$entity != null) mcc$entity.setPos(x, y, z);
        else {
            mcc$set$posX(this, x);
            mcc$set$posY(this, y);
            mcc$set$posZ(this, z);
        }
    }

    public void setLocationAndAngles(double x, double y, double z, float yaw, float pitch) {
        if (mcc$entity != null) mcc$entity.moveTo(x, y, z, yaw, pitch);
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

    public boolean isInWater() {
        return mcc$entity != null && mcc$entity.isInWater();
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
        if (mcc$entity instanceof org.kylepitcock.mccompat.v1_7_10.entity.LegacyHost h) {
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
        mcc$get$worldObj(this).spawnEntityInWorld(item);
        return item;
    }

    public void setSize(float width, float height) {
        var host = mcc$host();
        if (host != null) host.mcc$setSize(width, height);
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

    public void onCollideWithPlayer(mccompat.v1_7_10.net.minecraft.entity.player.EntityPlayer player) {
    }

    public boolean interactFirst(mccompat.v1_7_10.net.minecraft.entity.player.EntityPlayer player) {
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
}
