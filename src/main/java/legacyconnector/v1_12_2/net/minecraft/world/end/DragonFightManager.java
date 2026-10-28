package legacyconnector.v1_12_2.net.minecraft.world.end;

import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;

/**
 * 1.12.2 ender dragon fight of the End: a view over the End level's modern {@code EndDragonFight} (killed state, crystals, respawn).
 * The modern level ticks the fight.
 */
public class DragonFightManager {
    private final net.minecraft.world.level.dimension.end.EndDragonFight mcc$modern;
    /** Phantom: redirected to {@link #mcc$get$dragonKilled}. */
    @Deprecated
    public boolean dragonKilled;

    public DragonFightManager(net.minecraft.world.level.dimension.end.EndDragonFight modern) {
        this.mcc$modern = modern;
    }

    public static boolean mcc$get$dragonKilled(DragonFightManager m) {
        if (m.mcc$modern == null) return false;
        try {
            var f = net.minecraft.world.level.dimension.end.EndDragonFight.class.getDeclaredField("dragonKilled");
            f.setAccessible(true);
            return f.getBoolean(m.mcc$modern);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return m.mcc$modern.hasPreviouslyKilledDragon();
        }
    }

    public static void mcc$set$dragonKilled(DragonFightManager m, boolean v) {
        if (m.mcc$modern == null) return;
        try {
            var f = net.minecraft.world.level.dimension.end.EndDragonFight.class.getDeclaredField("dragonKilled");
            f.setAccessible(true);
            f.setBoolean(m.mcc$modern, v);
        } catch (ReflectiveOperationException | RuntimeException e) {
            // stays
        }
    }

    public NBTTagCompound getCompound() {
        NBTTagCompound out = new NBTTagCompound();
        if (mcc$modern == null) return out;
        var data = mcc$modern.saveData();
        out.setBoolean("DragonKilled", data.dragonKilled());
        out.setBoolean("PreviouslyKilled", data.previouslyKilled());
        data.dragonUUID().ifPresent(u -> out.setUniqueId("DragonUUID", u));
        return out;
    }

    public void tick() {
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
