package legacyconnector.v1_12_2.net.minecraft.entity.player;

import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;

/**
 * A player's abilities: a view of the modern abilities when it belongs to a player (the phantom fields read and write them), plain
 * values otherwise. Changes reach the client with {@link EntityPlayer#sendPlayerAbilities}.
 */
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
    private final EntityPlayer mcc$player;
    private final net.minecraft.world.entity.player.Abilities mcc$local = new net.minecraft.world.entity.player.Abilities();

    public PlayerCapabilities() {
        this(null);
    }

    PlayerCapabilities(EntityPlayer player) {
        this.mcc$player = player;
    }

    private static net.minecraft.world.entity.player.Abilities a(PlayerCapabilities c) {
        if (c == null) return new net.minecraft.world.entity.player.Abilities();
        if (c.mcc$player != null && c.mcc$player.mcc$entity instanceof net.minecraft.world.entity.player.Player p) return p.getAbilities();
        return c.mcc$local;
    }

    public static boolean mcc$get$disableDamage(PlayerCapabilities c) {
        return a(c).invulnerable;
    }

    public static void mcc$set$disableDamage(PlayerCapabilities c, boolean v) {
        a(c).invulnerable = v;
    }

    public static boolean mcc$get$isFlying(PlayerCapabilities c) {
        return a(c).flying;
    }

    public static void mcc$set$isFlying(PlayerCapabilities c, boolean v) {
        a(c).flying = v;
    }

    public static boolean mcc$get$allowFlying(PlayerCapabilities c) {
        return a(c).mayfly;
    }

    public static void mcc$set$allowFlying(PlayerCapabilities c, boolean v) {
        a(c).mayfly = v;
    }

    public static boolean mcc$get$isCreativeMode(PlayerCapabilities c) {
        return a(c).instabuild;
    }

    public static void mcc$set$isCreativeMode(PlayerCapabilities c, boolean v) {
        a(c).instabuild = v;
    }

    public static boolean mcc$get$allowEdit(PlayerCapabilities c) {
        return a(c).mayBuild;
    }

    public static void mcc$set$allowEdit(PlayerCapabilities c, boolean v) {
        a(c).mayBuild = v;
    }

    public void writeCapabilitiesToNBT(NBTTagCompound tag) {
        a(this).addSaveData(tag.mcc$modern());
    }

    public void readCapabilitiesFromNBT(NBTTagCompound tag) {
        a(this).loadSaveData(tag.mcc$modern());
    }

    public float getFlySpeed() {
        return a(this).getFlyingSpeed();
    }

    public void setFlySpeed(float speed) {
        a(this).setFlyingSpeed(speed);
    }

    public float getWalkSpeed() {
        return a(this).getWalkingSpeed();
    }

    public void setPlayerWalkSpeed(float speed) {
        a(this).setWalkingSpeed(speed);
    }
}
