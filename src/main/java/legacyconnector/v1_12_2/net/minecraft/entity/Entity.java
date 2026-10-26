package legacyconnector.v1_12_2.net.minecraft.entity;

import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2 entity: a view over the modern entity it is bound to (plain state until then). */
public abstract class Entity implements legacyconnector.v1_12_2.net.minecraft.command.ICommandSender {
    public World world;
    public double posX;
    public double posY;
    public double posZ;
    public double motionX;
    public double motionY;
    public double motionZ;
    public float rotationYaw;
    public float rotationPitch;
    public boolean isDead;
    public int ticksExisted;
    /** The modern entity this one stands for. */
    public net.minecraft.world.entity.Entity mcc$entity;

    protected legacyconnector.v1_12_2.net.minecraft.network.datasync.EntityDataManager dataManager;

    public Entity(World world) {
        this.world = world;
        this.dataManager = new legacyconnector.v1_12_2.net.minecraft.network.datasync.EntityDataManager(this);
        try {
            entityInit();
        } catch (RuntimeException | LinkageError e) {
            // a mod's data registration reaching an API the layer lacks must not stop the entity from being made
            org.slf4j.LoggerFactory.getLogger("LegacyConnector/Entity").debug("entityInit of {} failed: {}", getClass().getName(), e.toString());
        }
    }

    /** Registers the entity's data keys (vanilla's are not kept; mods register theirs here). */
    protected void entityInit() {
    }

    public legacyconnector.v1_12_2.net.minecraft.network.datasync.EntityDataManager getDataManager() {
        return dataManager;
    }

    public net.minecraft.world.entity.Entity mcc$modern() {
        return mcc$entity;
    }

    public World getEntityWorld() {
        return world;
    }

    public int getEntityId() {
        return mcc$entity == null ? 0 : mcc$entity.getId();
    }

    public java.util.UUID getUniqueID() {
        return mcc$entity == null ? new java.util.UUID(0, 0) : mcc$entity.getUUID();
    }

    public String getName() {
        return mcc$entity == null ? "" : mcc$entity.getName().getString();
    }

    // an entity as a command sender

    @Override
    public boolean canUseCommand(int permLevel, String commandName) {
        return true;
    }

    @Override
    public legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos getPosition() {
        if (mcc$entity == null) return new legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos(posX, posY + 0.5D, posZ);
        var p = mcc$entity.blockPosition();
        return new legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos(p.getX(), p.getY(), p.getZ());
    }

    @Override
    public legacyconnector.v1_12_2.net.minecraft.util.math.Vec3d getPositionVector() {
        if (mcc$entity == null) return new legacyconnector.v1_12_2.net.minecraft.util.math.Vec3d(posX, posY, posZ);
        return new legacyconnector.v1_12_2.net.minecraft.util.math.Vec3d(mcc$entity.getX(), mcc$entity.getY(), mcc$entity.getZ());
    }

    @Override
    public Entity getCommandSenderEntity() {
        return this;
    }

    @Override
    public legacyconnector.v1_12_2.net.minecraft.server.MinecraftServer getServer() {
        return legacyconnector.v1_12_2.net.minecraft.server.MinecraftServer.mcc$instance();
    }
}
