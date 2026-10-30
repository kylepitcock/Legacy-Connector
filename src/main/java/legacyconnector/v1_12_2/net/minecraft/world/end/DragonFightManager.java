package legacyconnector.v1_12_2.net.minecraft.world.end;

import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;

/**
 * 1.12.2 ender dragon fight of the End: a view over the End level's modern {@code EndDragonFight} (killed state, crystals, respawn).
 * The modern level ticks its own fight; a fight a mod creates for its own End ({@code new DragonFightManager(world, nbt)}) is a modern
 * fight of that level which {@link #tick} drives. The legacy fields are views of the modern fight's state.
 */
public class DragonFightManager {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger("LegacyConnector/Dragon");
    private final net.minecraft.world.level.dimension.end.EndDragonFight mcc$modern;
    /** True for a fight a mod created (the modern level does not tick it). */
    private final boolean mcc$own;
    /** Phantom: redirected to {@link #mcc$get$dragonKilled}. */
    @Deprecated
    public boolean dragonKilled;
    /** Phantom: redirected to {@link #mcc$get$previouslyKilled}. */
    @Deprecated
    public boolean previouslyKilled;
    /** Phantom: redirected to {@link #mcc$get$dragonUniqueId}. */
    @Deprecated
    public java.util.UUID dragonUniqueId;
    /** Phantom: redirected to {@link #mcc$get$exitPortalLocation}. */
    @Deprecated
    public legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos exitPortalLocation;
    /** Phantom: redirected to {@link #mcc$get$scanForLegacyFight} (the modern fight's "scan the arena for its state" flag). */
    @Deprecated
    public boolean scanForLegacyFight;

    public DragonFightManager(net.minecraft.world.level.dimension.end.EndDragonFight modern) {
        this.mcc$modern = modern;
        this.mcc$own = false;
    }

    /** A fight of the world read from 1.12's NBT (DragonKilled, PreviouslyKilled, DragonUUID, ExitPortalLocation, Gateways). */
    public DragonFightManager(legacyconnector.v1_12_2.net.minecraft.world.WorldServer worldIn, NBTTagCompound compound) {
        net.minecraft.world.level.dimension.end.EndDragonFight fight = null;
        var level = worldIn == null ? null : worldIn.mcc$serverLevel();
        if (level != null) {
            var data = net.minecraft.world.level.dimension.end.EndDragonFight.Data.DEFAULT;
            if (compound != null) {
                java.util.Optional<java.util.UUID> uuid = compound.hasUniqueId("DragonUUID") ? java.util.Optional.of(compound.getUniqueId("DragonUUID")) : java.util.Optional.empty();
                java.util.Optional<net.minecraft.core.BlockPos> exit = java.util.Optional.empty();
                if (compound.hasKey("ExitPortalLocation", 10)) {
                    var e = compound.getCompoundTag("ExitPortalLocation");
                    exit = java.util.Optional.of(new net.minecraft.core.BlockPos(e.getInteger("X"), e.getInteger("Y"), e.getInteger("Z")));
                }
                java.util.Optional<java.util.List<Integer>> gateways = java.util.Optional.empty();
                if (compound.hasKey("Gateways", 9)) {
                    var list = compound.getTagList("Gateways", 3);
                    java.util.List<Integer> g = new java.util.ArrayList<>();
                    for (int i = 0; i < list.tagCount(); i++) g.add(list.getIntAt(i));
                    gateways = java.util.Optional.of(g);
                }
                // 1.12: without a saved fight the dragon counts as killed before (the arena scan finds a living one)
                boolean saved = compound.hasKey("DragonKilled", 99);
                data = new net.minecraft.world.level.dimension.end.EndDragonFight.Data(true, !saved || compound.getBoolean("DragonKilled"),
                        !saved || compound.getBoolean("PreviouslyKilled"), saved && compound.getBoolean("IsRespawning"), uuid, exit, gateways);
            }
            try {
                fight = new net.minecraft.world.level.dimension.end.EndDragonFight(level, level.getSeed(), data);
            } catch (RuntimeException e) {
                LOG.warn("Could not create the dragon fight of {}: {}", level.dimension().location(), e.toString());
            }
        }
        this.mcc$modern = fight;
        this.mcc$own = true;
    }

    // ------------------------------------------------------------------ the modern fight's private state

    private static java.lang.reflect.Field mcc$field(String name) {
        try {
            var f = net.minecraft.world.level.dimension.end.EndDragonFight.class.getDeclaredField(name);
            f.setAccessible(true);
            return f;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    private Object mcc$read(String name) {
        var f = mcc$modern == null ? null : mcc$field(name);
        try {
            return f == null ? null : f.get(mcc$modern);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    private void mcc$write(String name, Object value) {
        var f = mcc$modern == null ? null : mcc$field(name);
        try {
            if (f != null) f.set(mcc$modern, value);
        } catch (ReflectiveOperationException | RuntimeException e) {
            // stays
        }
    }

    public static boolean mcc$get$dragonKilled(DragonFightManager m) {
        Object v = m.mcc$read("dragonKilled");
        return v instanceof Boolean b ? b : m.hasPreviouslyKilledDragon();
    }

    public static void mcc$set$dragonKilled(DragonFightManager m, boolean v) {
        m.mcc$write("dragonKilled", v);
    }

    public static boolean mcc$get$previouslyKilled(DragonFightManager m) {
        return m.hasPreviouslyKilledDragon();
    }

    public static void mcc$set$previouslyKilled(DragonFightManager m, boolean v) {
        m.mcc$write("previouslyKilled", v);
    }

    public static java.util.UUID mcc$get$dragonUniqueId(DragonFightManager m) {
        return m.mcc$modern == null ? null : m.mcc$modern.getDragonUUID();
    }

    public static void mcc$set$dragonUniqueId(DragonFightManager m, java.util.UUID v) {
        m.mcc$write("dragonUUID", v);
    }

    public static legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos mcc$get$exitPortalLocation(DragonFightManager m) {
        Object v = m.mcc$read("portalLocation");
        return v instanceof net.minecraft.core.BlockPos p ? legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos.mcc$of(p) : null;
    }

    public static void mcc$set$exitPortalLocation(DragonFightManager m, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos v) {
        m.mcc$write("portalLocation", v == null ? null : v.mcc$modern());
    }

    public static boolean mcc$get$scanForLegacyFight(DragonFightManager m) {
        return m.mcc$read("needsStateScanning") instanceof Boolean b && b;
    }

    public static void mcc$set$scanForLegacyFight(DragonFightManager m, boolean v) {
        m.mcc$write("needsStateScanning", v);
    }

    // ------------------------------------------------------------------ the legacy API

    public NBTTagCompound getCompound() {
        NBTTagCompound out = new NBTTagCompound();
        if (mcc$modern == null) return out;
        var data = mcc$modern.saveData();
        out.setBoolean("DragonKilled", data.dragonKilled());
        out.setBoolean("PreviouslyKilled", data.previouslyKilled());
        data.dragonUUID().ifPresent(u -> out.setUniqueId("DragonUUID", u));
        data.exitPortalLocation().ifPresent(p -> {
            NBTTagCompound e = new NBTTagCompound();
            e.setInteger("X", p.getX());
            e.setInteger("Y", p.getY());
            e.setInteger("Z", p.getZ());
            out.setTag("ExitPortalLocation", e);
        });
        data.gateways().ifPresent(g -> {
            var list = new legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagList();
            for (int i : g) list.appendTag(new legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagInt(i));
            out.setTag("Gateways", list);
        });
        return out;
    }

    /** Runs a mod-created fight (the modern level ticks its own). */
    public void tick() {
        if (mcc$own && mcc$modern != null) {
            try {
                mcc$modern.tick();
            } catch (RuntimeException e) {
                // a fight whose arena is not loaded yet
            }
        }
    }

    /** The dragon died: the exit portal opens, a gateway appears and the egg is placed the first time (the modern fight's rules). */
    public void processDragonDeath(legacyconnector.v1_12_2.net.minecraft.entity.boss.EntityDragon dragon) {
        if (mcc$modern != null && dragon != null && dragon.mcc$modern() instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon d) {
            mcc$modern.setDragonKilled(d);
        }
    }

    /** Opens the next End gateway around the main island. */
    public void spawnNewGateway() {
        if (mcc$modern == null) return;
        try {
            var m = net.minecraft.world.level.dimension.end.EndDragonFight.class.getDeclaredMethod("spawnNewGateway");
            m.setAccessible(true);
            m.invoke(mcc$modern);
        } catch (ReflectiveOperationException | RuntimeException e) {
            LOG.debug("No new gateway: {}", e.toString());
        }
    }

    public int getNumAliveCrystals() {
        return mcc$modern == null ? 0 : mcc$modern.getCrystalsAlive();
    }

    public boolean hasPreviouslyKilledDragon() {
        return mcc$modern != null && mcc$modern.hasPreviouslyKilledDragon();
    }

    public void respawnDragon() {
        if (mcc$modern != null) mcc$modern.tryRespawn();
    }

    public void resetSpikeCrystals() {
        if (mcc$modern != null) mcc$modern.resetSpikeCrystals();
    }

    /** The fight of the End level (null when the level has none). */
    public static DragonFightManager mcc$of(net.minecraft.world.level.Level level) {
        var fight = level instanceof net.minecraft.server.level.ServerLevel s ? s.getDragonFight() : null;
        return fight == null ? null : new DragonFightManager(fight);
    }
}
