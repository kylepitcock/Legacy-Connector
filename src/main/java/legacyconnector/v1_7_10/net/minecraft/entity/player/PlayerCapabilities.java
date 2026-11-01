package legacyconnector.v1_7_10.net.minecraft.entity.player;

import legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.entity.player.Abilities;

/** View over the modern player's {@link Abilities}; the public flags are phantoms redirected to accessors. */
public class PlayerCapabilities {
    @Deprecated
    public boolean disableDamage;
    @Deprecated
    public boolean isFlying;
    @Deprecated
    public boolean allowFlying;
    @Deprecated
    public boolean isCreativeMode;
    @Deprecated
    public boolean allowEdit;

    private final net.minecraft.world.entity.player.Player mcc$player;

    private final Abilities mcc$own;

    public PlayerCapabilities(net.minecraft.world.entity.player.Player player) {
        this.mcc$player = player;
        this.mcc$own = null;
    }

    /** A detached set of abilities (mods build their own to copy or inspect flags). */
    public PlayerCapabilities() {
        this.mcc$player = null;
        this.mcc$own = new Abilities();
    }

    private Abilities a() {
        return mcc$player != null ? mcc$player.getAbilities() : mcc$own;
    }

    public static boolean mcc$get$disableDamage(PlayerCapabilities c) {
        return c.a().invulnerable;
    }

    public static void mcc$set$disableDamage(PlayerCapabilities c, boolean v) {
        c.a().invulnerable = v;
    }

    public static boolean mcc$get$isFlying(PlayerCapabilities c) {
        return c.a().flying;
    }

    public static void mcc$set$isFlying(PlayerCapabilities c, boolean v) {
        if (!v && c.mcc$spectator()) return;
        c.a().flying = v;
    }

    /**
     * 1.7.10 had no spectator mode: mods that take flight away from every player not in creative (flight rings, rituals) would
     * turn it off for spectators, who pass through blocks and then fall through the world into the void.
     */
    private boolean mcc$spectator() {
        return mcc$player != null && mcc$player.isSpectator();
    }

    public static boolean mcc$get$allowFlying(PlayerCapabilities c) {
        return c.a().mayfly;
    }

    public static void mcc$set$allowFlying(PlayerCapabilities c, boolean v) {
        if (!v && c.mcc$spectator()) return;
        c.a().mayfly = v;
    }

    public static boolean mcc$get$isCreativeMode(PlayerCapabilities c) {
        return c.a().instabuild;
    }

    public static void mcc$set$isCreativeMode(PlayerCapabilities c, boolean v) {
        c.a().instabuild = v;
    }

    public static boolean mcc$get$allowEdit(PlayerCapabilities c) {
        return c.a().mayBuild;
    }

    public static void mcc$set$allowEdit(PlayerCapabilities c, boolean v) {
        c.a().mayBuild = v;
    }

    public float getFlySpeed() {
        return a().getFlyingSpeed();
    }

    public void setFlySpeed(float speed) {
        a().setFlyingSpeed(speed);
    }

    public float getWalkSpeed() {
        return a().getWalkingSpeed();
    }

    public void setPlayerWalkSpeed(float speed) {
        a().setWalkingSpeed(speed);
    }

    public void writeCapabilitiesToNBT(NBTTagCompound tag) {
        a().addSaveData(tag.mcc$modern());
    }

    public void readCapabilitiesFromNBT(NBTTagCompound tag) {
        a().loadSaveData(tag.mcc$modern());
        if (mcc$spectator()) {
            // saved abilities of a non-spectator would ground a spectator (see mcc$spectator)
            a().mayfly = true;
            a().flying = true;
        }
    }

    public boolean mcc$creative() {
        return a().instabuild;
    }
}
