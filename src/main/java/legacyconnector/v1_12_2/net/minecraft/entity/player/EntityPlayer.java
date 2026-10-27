package legacyconnector.v1_12_2.net.minecraft.entity.player;

import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2 player over a modern {@link net.minecraft.world.entity.player.Player}; its inventory and abilities are live views. */
public abstract class EntityPlayer extends EntityLivingBase {
    /** Phantom field: accesses are redirected to {@code mcc$get$inventory}. */
    @Deprecated
    public InventoryPlayer inventory;
    private InventoryPlayer mcc$inventory;
    /** Phantom field: accesses are redirected to {@code mcc$get$capabilities}. */
    @Deprecated
    public PlayerCapabilities capabilities;
    private PlayerCapabilities mcc$capabilities;
    /** Phantom field: accesses are redirected to {@code mcc$get$experienceLevel}. */
    @Deprecated
    public int experienceLevel;
    /** Phantom field: accesses are redirected to {@code mcc$get$experienceTotal}. */
    @Deprecated
    public int experienceTotal;
    /** Phantom field: accesses are redirected to {@code mcc$get$experience}. */
    @Deprecated
    public float experience;

    public EntityPlayer(World world) {
        super(world);
    }

    public net.minecraft.world.entity.player.Player mcc$player() {
        return mcc$entity instanceof net.minecraft.world.entity.player.Player p ? p : null;
    }

    public com.mojang.authlib.GameProfile getGameProfile() {
        var p = mcc$player();
        return p == null ? new com.mojang.authlib.GameProfile(getUniqueID(), getName()) : p.getGameProfile();
    }

    public String getDisplayNameString() {
        return getName();
    }

    public boolean isSpectator() {
        var p = mcc$player();
        return p != null && p.isSpectator();
    }

    public boolean isCreative() {
        var p = mcc$player();
        return p != null && p.isCreative();
    }

    public boolean isAllowEdit() {
        var p = mcc$player();
        return p == null || p.mayBuild();
    }

    public boolean canPlayerEdit(legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos, legacyconnector.v1_12_2.net.minecraft.util.EnumFacing facing, ItemStack stack) {
        var p = mcc$player();
        if (p == null) return true;
        var direction = facing == null ? net.minecraft.core.Direction.UP : net.minecraft.core.Direction.byName(facing.getName());
        return p.mayUseItemAt(new net.minecraft.core.BlockPos(pos.getX(), pos.getY(), pos.getZ()), direction,
                org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122.toModern(stack));
    }

    /** The message in chat, or above the hot bar when {@code actionBar}. */
    public void sendStatusMessage(legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent message, boolean actionBar) {
        var p = mcc$player();
        if (p != null && message != null) p.displayClientMessage(message.mcc$modern(), actionBar);
    }

    public void sendPlayerAbilities() {
        var p = mcc$player();
        if (p != null) p.onUpdateAbilities();
    }

    public void addExperience(int amount) {
        var p = mcc$player();
        if (p != null) p.giveExperiencePoints(amount);
    }

    public void addExperienceLevel(int levels) {
        var p = mcc$player();
        if (p != null) p.giveExperienceLevels(levels);
    }

    public int xpBarCap() {
        var p = mcc$player();
        return p == null ? 7 : p.getXpNeededForNextLevel();
    }

    public void closeScreen() {
        var p = mcc$player();
        if (p != null) p.closeContainer();
    }

    public boolean isUser() {
        return false;
    }

    public boolean shouldHeal() {
        var p = mcc$player();
        return p != null && p.isHurt();
    }

    public void addExhaustion(float exhaustion) {
        var p = mcc$player();
        if (p != null) p.causeFoodExhaustion(exhaustion);
    }

    public boolean canEat(boolean ignoreHunger) {
        var p = mcc$player();
        return p != null && p.canEat(ignoreHunger);
    }

    public legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos getBedLocation() {
        return null;
    }

    // ------------------------------------------------------------------ phantom field accessors

    public static InventoryPlayer mcc$get$inventory(EntityPlayer p) {
        if (p.mcc$inventory == null) p.mcc$inventory = new InventoryPlayer(p);
        return p.mcc$inventory;
    }

    public static PlayerCapabilities mcc$get$capabilities(EntityPlayer p) {
        if (p.mcc$capabilities == null) {
            PlayerCapabilities c = new PlayerCapabilities();
            c.mcc$player = p;
            p.mcc$capabilities = c;
        }
        return p.mcc$capabilities;
    }

    public static int mcc$get$experienceLevel(EntityPlayer p) {
        var m = p.mcc$player();
        return m == null ? 0 : m.experienceLevel;
    }

    public static void mcc$set$experienceLevel(EntityPlayer p, int v) {
        var m = p.mcc$player();
        if (m != null) m.experienceLevel = v;
    }

    public static int mcc$get$experienceTotal(EntityPlayer p) {
        var m = p.mcc$player();
        return m == null ? 0 : m.totalExperience;
    }

    public static void mcc$set$experienceTotal(EntityPlayer p, int v) {
        var m = p.mcc$player();
        if (m != null) m.totalExperience = v;
    }

    public static float mcc$get$experience(EntityPlayer p) {
        var m = p.mcc$player();
        return m == null ? 0 : m.experienceProgress;
    }

    public static void mcc$set$experience(EntityPlayer p, float v) {
        var m = p.mcc$player();
        if (m != null) m.experienceProgress = v;
    }
}
