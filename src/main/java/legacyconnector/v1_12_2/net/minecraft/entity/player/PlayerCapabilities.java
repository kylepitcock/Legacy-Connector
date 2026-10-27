package legacyconnector.v1_12_2.net.minecraft.entity.player;

/**
 * 1.12.2 player abilities: a view over the modern player's abilities when it belongs to a player (mods change them and then
 * call {@code sendPlayerAbilities}), plain values otherwise.
 */
public class PlayerCapabilities {
    /** Phantom field: accesses are redirected to {@code mcc$get$disableDamage}. */
    @Deprecated
    public boolean disableDamage;
    private boolean mcc$local_disableDamage;
    /** Phantom field: accesses are redirected to {@code mcc$get$isFlying}. */
    @Deprecated
    public boolean isFlying;
    private boolean mcc$local_isFlying;
    /** Phantom field: accesses are redirected to {@code mcc$get$allowFlying}. */
    @Deprecated
    public boolean allowFlying;
    private boolean mcc$local_allowFlying;
    /** Phantom field: accesses are redirected to {@code mcc$get$isCreativeMode}. */
    @Deprecated
    public boolean isCreativeMode;
    private boolean mcc$local_isCreativeMode;
    /** Phantom field: accesses are redirected to {@code mcc$get$allowEdit}. */
    @Deprecated
    public boolean allowEdit;
    private boolean mcc$local_allowEdit;
    private float mcc$local_flySpeed = 0.05F;
    private float mcc$local_walkSpeed = 0.1F;
    EntityPlayer mcc$player;

    public PlayerCapabilities() {
        mcc$local_allowEdit = true;
    }

    net.minecraft.world.entity.player.Abilities mcc$abilities() {
        var p = mcc$player == null ? null : mcc$player.mcc$player();
        return p == null ? null : p.getAbilities();
    }

    public float getFlySpeed() {
        var a = mcc$abilities();
        return a != null ? a.getFlyingSpeed() : mcc$local_flySpeed;
    }

    public void setFlySpeed(float speed) {
        var a = mcc$abilities();
        if (a != null) a.setFlyingSpeed(speed);
        else mcc$local_flySpeed = speed;
    }

    public float getWalkSpeed() {
        var a = mcc$abilities();
        return a != null ? a.getWalkingSpeed() : mcc$local_walkSpeed;
    }

    public void setPlayerWalkSpeed(float speed) {
        var a = mcc$abilities();
        if (a != null) a.setWalkingSpeed(speed);
        else mcc$local_walkSpeed = speed;
    }

    public void writeCapabilitiesToNBT(legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound tag) {
        var abilities = new legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound();
        abilities.setBoolean("invulnerable", mcc$get$disableDamage(this));
        abilities.setBoolean("flying", mcc$get$isFlying(this));
        abilities.setBoolean("mayfly", mcc$get$allowFlying(this));
        abilities.setBoolean("instabuild", mcc$get$isCreativeMode(this));
        abilities.setBoolean("mayBuild", mcc$get$allowEdit(this));
        abilities.setFloat("flySpeed", getFlySpeed());
        abilities.setFloat("walkSpeed", getWalkSpeed());
        tag.setTag("abilities", abilities);
    }

    public void readCapabilitiesFromNBT(legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound tag) {
        if (!tag.hasKey("abilities", 10)) return;
        var abilities = tag.getCompoundTag("abilities");
        mcc$set$disableDamage(this, abilities.getBoolean("invulnerable"));
        mcc$set$isFlying(this, abilities.getBoolean("flying"));
        mcc$set$allowFlying(this, abilities.getBoolean("mayfly"));
        mcc$set$isCreativeMode(this, abilities.getBoolean("instabuild"));
        if (abilities.hasKey("flySpeed", 99)) {
            setFlySpeed(abilities.getFloat("flySpeed"));
            setPlayerWalkSpeed(abilities.getFloat("walkSpeed"));
        }
        if (abilities.hasKey("mayBuild", 1)) mcc$set$allowEdit(this, abilities.getBoolean("mayBuild"));
    }

    // ------------------------------------------------------------------ phantom field accessors

    public static boolean mcc$get$disableDamage(PlayerCapabilities c) {
        var a = c.mcc$abilities();
        return a != null ? a.invulnerable : c.mcc$local_disableDamage;
    }

    public static void mcc$set$disableDamage(PlayerCapabilities c, boolean v) {
        var a = c.mcc$abilities();
        if (a != null) a.invulnerable = v;
        else c.mcc$local_disableDamage = v;
    }

    public static boolean mcc$get$isFlying(PlayerCapabilities c) {
        var a = c.mcc$abilities();
        return a != null ? a.flying : c.mcc$local_isFlying;
    }

    public static void mcc$set$isFlying(PlayerCapabilities c, boolean v) {
        var a = c.mcc$abilities();
        if (a != null) a.flying = v;
        else c.mcc$local_isFlying = v;
    }

    public static boolean mcc$get$allowFlying(PlayerCapabilities c) {
        var a = c.mcc$abilities();
        return a != null ? a.mayfly : c.mcc$local_allowFlying;
    }

    public static void mcc$set$allowFlying(PlayerCapabilities c, boolean v) {
        var a = c.mcc$abilities();
        if (a != null) a.mayfly = v;
        else c.mcc$local_allowFlying = v;
    }

    public static boolean mcc$get$isCreativeMode(PlayerCapabilities c) {
        var a = c.mcc$abilities();
        return a != null ? a.instabuild : c.mcc$local_isCreativeMode;
    }

    public static void mcc$set$isCreativeMode(PlayerCapabilities c, boolean v) {
        var a = c.mcc$abilities();
        if (a != null) a.instabuild = v;
        else c.mcc$local_isCreativeMode = v;
    }

    public static boolean mcc$get$allowEdit(PlayerCapabilities c) {
        var a = c.mcc$abilities();
        return a != null ? a.mayBuild : c.mcc$local_allowEdit;
    }

    public static void mcc$set$allowEdit(PlayerCapabilities c, boolean v) {
        var a = c.mcc$abilities();
        if (a != null) a.mayBuild = v;
        else c.mcc$local_allowEdit = v;
    }
}
