package legacyconnector.v1_12_2.net.minecraft.entity.player;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.entity.item.EntityItem;
import legacyconnector.v1_12_2.net.minecraft.entity.ai.attributes.IAttribute;
import legacyconnector.v1_12_2.net.minecraft.entity.ai.attributes.RangedAttribute;
import legacyconnector.v1_12_2.net.minecraft.inventory.EntityEquipmentSlot;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import net.minecraft.world.entity.player.Player;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122;

/** 1.12.2 player: a view over a modern player (inventory, abilities, food, experience, cooldowns). */
public abstract class EntityPlayer extends EntityLivingBase {
    /** Forge's block reach attribute (the modern block interaction range). */
    public static final IAttribute REACH_DISTANCE = new RangedAttribute(null, "generic.reachDistance", 5.0D, 0.0D, 1024.0D).setShouldWatch(true);

    @Deprecated
    public InventoryPlayer inventory;
    @Deprecated
    public PlayerCapabilities capabilities;
    @Deprecated
    public int experienceLevel;
    @Deprecated
    public int experienceTotal;
    @Deprecated
    public float experience;
    @Deprecated
    public float eyeHeight;
    @Deprecated
    public float cameraYaw;
    @Deprecated
    public float prevCameraYaw;
    @Deprecated
    public int xpCooldown;
    @Deprecated
    public double chasingPosX;
    @Deprecated
    public double chasingPosY;
    @Deprecated
    public double chasingPosZ;
    @Deprecated
    public double prevChasingPosX;
    @Deprecated
    public double prevChasingPosY;
    @Deprecated
    public double prevChasingPosZ;
    @Deprecated
    public float speedInAir;

    private InventoryPlayer mcc$inventory;
    private PlayerCapabilities mcc$capabilities;
    private legacyconnector.v1_12_2.net.minecraft.util.FoodStats mcc$food;
    private legacyconnector.v1_12_2.net.minecraft.util.CooldownTracker mcc$cooldowns;

    public EntityPlayer(World world, GameProfile profile) {
        super(world);
    }

    protected EntityPlayer(Player modern) {
        super(modern);
    }

    public Player mcc$player() {
        return (Player) mcc$entity;
    }

    // ------------------------------------------------------------------ phantom fields

    public static InventoryPlayer mcc$get$inventory(EntityPlayer p) {
        if (p.mcc$inventory == null) p.mcc$inventory = new InventoryPlayer(p);
        return p.mcc$inventory;
    }

    public static void mcc$set$inventory(EntityPlayer p, InventoryPlayer v) {
        if (v != null) p.mcc$inventory = v;
    }

    public static PlayerCapabilities mcc$get$capabilities(EntityPlayer p) {
        if (p.mcc$capabilities == null) p.mcc$capabilities = new PlayerCapabilities(p);
        return p.mcc$capabilities;
    }

    public static int mcc$get$experienceLevel(EntityPlayer p) {
        return p.mcc$entity instanceof Player m ? m.experienceLevel : 0;
    }

    public static void mcc$set$experienceLevel(EntityPlayer p, int v) {
        if (p.mcc$entity instanceof Player m) m.experienceLevel = v;
    }

    public static int mcc$get$experienceTotal(EntityPlayer p) {
        return p.mcc$entity instanceof Player m ? m.totalExperience : 0;
    }

    public static void mcc$set$experienceTotal(EntityPlayer p, int v) {
        if (p.mcc$entity instanceof Player m) m.totalExperience = v;
    }

    public static float mcc$get$experience(EntityPlayer p) {
        return p.mcc$entity instanceof Player m ? m.experienceProgress : 0.0F;
    }

    public static void mcc$set$experience(EntityPlayer p, float v) {
        if (p.mcc$entity instanceof Player m) m.experienceProgress = v;
    }

    public static float mcc$get$eyeHeight(EntityPlayer p) {
        return p.getEyeHeight();
    }

    public static void mcc$set$eyeHeight(EntityPlayer p, float v) {
    }

    public static int mcc$get$xpCooldown(EntityPlayer p) {
        return p.mcc$entity instanceof Player m ? m.takeXpDelay : 0;
    }

    public static void mcc$set$xpCooldown(EntityPlayer p, int v) {
        if (p.mcc$entity instanceof Player m) m.takeXpDelay = v;
    }

    public static float mcc$get$cameraYaw(EntityPlayer p) {
        return p.mcc$entity instanceof Player m ? m.bob : 0.0F;
    }

    public static void mcc$set$cameraYaw(EntityPlayer p, float v) {
        if (p.mcc$entity instanceof Player m) m.bob = v;
    }

    public static float mcc$get$prevCameraYaw(EntityPlayer p) {
        return p.mcc$entity instanceof Player m ? m.oBob : 0.0F;
    }

    public static void mcc$set$prevCameraYaw(EntityPlayer p, float v) {
        if (p.mcc$entity instanceof Player m) m.oBob = v;
    }

    public static double mcc$get$chasingPosX(EntityPlayer p) {
        return p.mcc$entity instanceof Player m ? m.xCloak : 0.0D;
    }

    public static double mcc$get$chasingPosY(EntityPlayer p) {
        return p.mcc$entity instanceof Player m ? m.yCloak : 0.0D;
    }

    public static double mcc$get$chasingPosZ(EntityPlayer p) {
        return p.mcc$entity instanceof Player m ? m.zCloak : 0.0D;
    }

    public static double mcc$get$prevChasingPosX(EntityPlayer p) {
        return p.mcc$entity instanceof Player m ? m.xCloakO : 0.0D;
    }

    public static double mcc$get$prevChasingPosY(EntityPlayer p) {
        return p.mcc$entity instanceof Player m ? m.yCloakO : 0.0D;
    }

    public static double mcc$get$prevChasingPosZ(EntityPlayer p) {
        return p.mcc$entity instanceof Player m ? m.zCloakO : 0.0D;
    }

    public static float mcc$get$speedInAir(EntityPlayer p) {
        return 0.02F;
    }

    public static void mcc$set$speedInAir(EntityPlayer p, float v) {
    }

    // ------------------------------------------------------------------ identity and game mode

    public GameProfile getGameProfile() {
        return mcc$player().getGameProfile();
    }

    public static UUID getUUID(GameProfile profile) {
        UUID id = profile.getId();
        return id != null ? id : getOfflineUUID(profile.getName());
    }

    public static UUID getOfflineUUID(String username) {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    @Override
    public String getName() {
        return mcc$player().getGameProfile().getName();
    }

    public String getDisplayNameString() {
        return mcc$player().getDisplayName().getString();
    }

    public boolean isUser() {
        return false;
    }

    public boolean isSpectator() {
        return mcc$player().isSpectator();
    }

    public boolean isCreative() {
        return mcc$player().isCreative();
    }

    public boolean isAllowEdit() {
        return mcc$player().getAbilities().mayBuild;
    }

    public boolean canPlayerEdit(BlockPos pos, EnumFacing facing, ItemStack stack) {
        if (isAllowEdit()) return true;
        if (stack == null || stack.isEmpty()) return false;
        var modern = Stacks1122.toModern(stack);
        return modern.canPlaceOnBlockInAdventureMode(new net.minecraft.world.level.block.state.pattern.BlockInWorld(mcc$player().level(),
                pos.offset(facing.getOpposite()).mcc$modern(), false));
    }

    public boolean canUseCommandBlock() {
        return mcc$player().canUseGameMasterBlocks();
    }

    @Override
    protected boolean isPlayer() {
        return true;
    }

    public boolean canAttackPlayer(EntityPlayer other) {
        return other != null && mcc$player().canHarmPlayer(other.mcc$player());
    }

    public void sendStatusMessage(legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent message, boolean actionBar) {
        if (message != null) mcc$player().displayClientMessage(message.mcc$modern(), actionBar);
    }

    @Override
    public void sendMessage(legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent component) {
        if (component != null) mcc$player().sendSystemMessage(component.mcc$modern());
    }

    @Override
    public legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent getDisplayName() {
        return new legacyconnector.v1_12_2.net.minecraft.util.text.TextComponentString(mcc$player().getDisplayName().getString());
    }

    @Override
    public legacyconnector.v1_12_2.net.minecraft.util.SoundCategory getSoundCategory() {
        return legacyconnector.v1_12_2.net.minecraft.util.SoundCategory.PLAYERS;
    }

    /** Plays a sound for everyone else (the player plays it locally; on the server every listener but the player hears it). */
    @Override
    public void playSound(legacyconnector.v1_12_2.net.minecraft.util.SoundEvent sound, float volume, float pitch) {
        if (sound == null) return;
        var event = sound.mcc$event();
        if (event != null) mcc$player().playSound(event, volume, pitch);
    }

    @Override
    public float getEyeHeight() {
        return mcc$player().getEyeHeight();
    }

    public float getDefaultEyeHeight() {
        return 1.62F;
    }

    // ------------------------------------------------------------------ inventory and items

    @Override
    public ItemStack getItemStackFromSlot(EntityEquipmentSlot slot) {
        return super.getItemStackFromSlot(slot);
    }

    public boolean addItemStackToInventory(ItemStack stack) {
        return mcc$get$inventory(this).addItemStackToInventory(stack);
    }

    public EntityItem dropItem(boolean dropAll) {
        var p = mcc$player();
        var dropped = p.drop(p.getInventory().removeFromSelected(dropAll), false, true);
        return dropped == null ? null : org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122.wrap(dropped) instanceof EntityItem e ? e : null;
    }

    public EntityItem dropItem(ItemStack stack, boolean unused) {
        return dropItem(stack, false, unused);
    }

    public EntityItem dropItem(ItemStack stack, boolean dropAround, boolean traceItem) {
        if (stack == null || stack.isEmpty()) return null;
        var dropped = mcc$player().drop(Stacks1122.toModern(stack), dropAround, traceItem);
        return dropped == null ? null : org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122.wrap(dropped) instanceof EntityItem e ? e : null;
    }

    public ItemStack dropItemAndGetStack(EntityItem item) {
        if (item != null) org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122.spawn(mcc$world(), item);
        return item == null ? ItemStack.EMPTY : item.getItem();
    }

    public void closeScreen() {
        mcc$player().closeContainer();
    }

    /** Opens a mod's GUI (FML's IGuiHandler); the GUI bridge does the opening. */
    public void openGui(Object mod, int modGuiId, World world, int x, int y, int z) {
        org.kylepitcock.legacyconnector.v1_12_2.entity.PlayerHooks1122.openGui(this, mod, modGuiId, world, x, y, z);
    }

    public legacyconnector.v1_12_2.net.minecraft.util.FoodStats getFoodStats() {
        if (mcc$food == null) mcc$food = legacyconnector.v1_12_2.net.minecraft.util.FoodStats.mcc$of(mcc$player().getFoodData());
        return mcc$food;
    }

    public legacyconnector.v1_12_2.net.minecraft.util.CooldownTracker getCooldownTracker() {
        if (mcc$cooldowns == null) mcc$cooldowns = legacyconnector.v1_12_2.net.minecraft.util.CooldownTracker.mcc$of(mcc$player().getCooldowns());
        return mcc$cooldowns;
    }

    public boolean canEat(boolean ignoreHunger) {
        return mcc$player().canEat(ignoreHunger);
    }

    public boolean shouldHeal() {
        return getHealth() > 0.0F && getHealth() < getMaxHealth();
    }

    public void addExhaustion(float exhaustion) {
        mcc$player().causeFoodExhaustion(exhaustion);
    }

    public void addExperience(int amount) {
        mcc$player().giveExperiencePoints(amount);
    }

    public void addExperienceLevel(int levels) {
        mcc$player().giveExperienceLevels(levels);
    }

    public void onEnchant(ItemStack stack, int levels) {
        mcc$player().onEnchantmentPerformed(Stacks1122.toModern(stack), levels);
    }

    public int xpBarCap() {
        return mcc$player().getXpNeededForNextLevel();
    }

    public int getXPSeed() {
        return mcc$player().getEnchantmentSeed();
    }

    public int getScore() {
        return mcc$player().getScore();
    }

    public void setScore(int score) {
        mcc$player().setScore(score);
    }

    public void addScore(int score) {
        mcc$player().increaseScore(score);
    }

    public float getCooledAttackStrength(float adjustTicks) {
        return mcc$player().getAttackStrengthScale(adjustTicks);
    }

    public float getCooldownPeriod() {
        return mcc$player().getCurrentItemAttackStrengthDelay();
    }

    public void resetCooldown() {
        mcc$player().resetAttackStrengthTicker();
    }

    public float getLuck() {
        return mcc$player().getLuck();
    }

    public void sendPlayerAbilities() {
        mcc$player().onUpdateAbilities();
    }

    public void attackTargetEntityWithCurrentItem(Entity target) {
        if (target != null && target.mcc$entity != null) mcc$player().attack(target.mcc$entity);
    }

    public void onCriticalHit(Entity target) {
        if (target != null && target.mcc$entity != null) mcc$player().crit(target.mcc$entity);
    }

    public void onEnchantmentCritical(Entity target) {
        if (target != null && target.mcc$entity != null) mcc$player().magicCrit(target.mcc$entity);
    }

    public void spawnSweepParticles() {
        mcc$player().sweepAttack();
    }

    public void disableShield(boolean always) {
        mcc$player().disableShield();
    }

    public legacyconnector.v1_12_2.net.minecraft.util.EnumActionResult interactOn(Entity entity, legacyconnector.v1_12_2.net.minecraft.util.EnumHand hand) {
        if (entity == null || entity.mcc$entity == null) return legacyconnector.v1_12_2.net.minecraft.util.EnumActionResult.PASS;
        var r = mcc$player().interactOn(entity.mcc$entity, mcc$hand(hand));
        return r.consumesAction() ? legacyconnector.v1_12_2.net.minecraft.util.EnumActionResult.SUCCESS
                : r == net.minecraft.world.InteractionResult.FAIL ? legacyconnector.v1_12_2.net.minecraft.util.EnumActionResult.FAIL
                : legacyconnector.v1_12_2.net.minecraft.util.EnumActionResult.PASS;
    }

    public float getDigSpeed(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state) {
        return mcc$player().getDestroySpeed(org.kylepitcock.legacyconnector.v1_12_2.bridge.StatesBridge1122.toModern(state));
    }

    public float getDigSpeed(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state, BlockPos pos) {
        return mcc$player().getDigSpeed(org.kylepitcock.legacyconnector.v1_12_2.bridge.StatesBridge1122.toModern(state), pos == null ? null : pos.mcc$modern());
    }

    public boolean canHarvestBlock(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state) {
        return mcc$player().hasCorrectToolForDrops(org.kylepitcock.legacyconnector.v1_12_2.bridge.StatesBridge1122.toModern(state));
    }

    // ------------------------------------------------------------------ sleeping and spawn

    @Override
    public boolean isPlayerSleeping() {
        return mcc$player().isSleeping();
    }

    public boolean isPlayerFullyAsleep() {
        return mcc$player().isSleepingLongEnough();
    }

    public int getSleepTimer() {
        return mcc$player().getSleepTimer();
    }

    public void wakeUpPlayer(boolean immediately, boolean updateWorldFlag, boolean setSpawn) {
        mcc$player().stopSleepInBed(immediately, updateWorldFlag);
    }

    public BlockPos getBedLocation() {
        if (mcc$player() instanceof net.minecraft.server.level.ServerPlayer sp && sp.getRespawnPosition() != null) return BlockPos.mcc$of(sp.getRespawnPosition());
        return null;
    }

    public BlockPos getBedLocation(int dimension) {
        return getBedLocation();
    }

    public boolean isSpawnForced() {
        return mcc$player() instanceof net.minecraft.server.level.ServerPlayer sp && sp.isRespawnForced();
    }

    public boolean isSpawnForced(int dimension) {
        return isSpawnForced();
    }

    public void setSpawnPoint(BlockPos pos, boolean forced) {
        if (mcc$player() instanceof net.minecraft.server.level.ServerPlayer sp) {
            sp.setRespawnPosition(sp.level().dimension(), pos == null ? null : pos.mcc$modern(), 0.0F, forced, false);
        }
    }

    public void setSpawnChunk(BlockPos pos, boolean forced, int dimension) {
        if (mcc$player() instanceof net.minecraft.server.level.ServerPlayer sp) {
            var level = org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122.level(sp.server, dimension);
            sp.setRespawnPosition(level == null ? sp.level().dimension() : level.dimension(), pos == null ? null : pos.mcc$modern(), 0.0F, forced, false);
        }
    }

    // ------------------------------------------------------------------ capabilities (the player's own item handler, then mods')

    @Override
    public boolean hasCapability(legacyconnector.v1_12_2.net.minecraftforge.common.capabilities.Capability<?> capability, EnumFacing facing) {
        if (capability != null && capability == legacyconnector.v1_12_2.net.minecraftforge.items.CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) return true;
        return super.hasCapability(capability, facing);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getCapability(legacyconnector.v1_12_2.net.minecraftforge.common.capabilities.Capability<T> capability, EnumFacing facing) {
        T fromMods = super.getCapability(capability, facing);
        if (fromMods != null) return fromMods;
        if (capability != null && capability == legacyconnector.v1_12_2.net.minecraftforge.items.CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return (T) mcc$get$inventory(this).mcc$itemHandler(facing);
        }
        return null;
    }

    // ------------------------------------------------------------------ hooks

    @Override
    public void setDead() {
        // a player view is never removed by legacy code
    }

    @Override
    public boolean isPushedByWater() {
        return !mcc$player().getAbilities().flying;
    }

    @Override
    public boolean isEntityInsideOpaqueBlock() {
        return !mcc$player().isSleeping() && super.isEntityInsideOpaqueBlock();
    }

    @Override
    public double getYOffset() {
        return -0.35D;
    }

    public void respawnPlayer() {
    }

    public void preparePlayerToSpawn() {
    }

    public boolean hasReducedDebug() {
        return mcc$player().isReducedDebugInfo();
    }

    public void setReducedDebug(boolean reduced) {
        mcc$player().setReducedDebugInfo(reduced);
    }

    @Override
    public legacyconnector.v1_12_2.net.minecraft.util.EnumHandSide getPrimaryHand() {
        return mcc$player().getMainArm() == net.minecraft.world.entity.HumanoidArm.LEFT ? legacyconnector.v1_12_2.net.minecraft.util.EnumHandSide.LEFT
                : legacyconnector.v1_12_2.net.minecraft.util.EnumHandSide.RIGHT;
    }

    public void setPrimaryHand(legacyconnector.v1_12_2.net.minecraft.util.EnumHandSide side) {
        mcc$player().setMainArm(side == legacyconnector.v1_12_2.net.minecraft.util.EnumHandSide.LEFT ? net.minecraft.world.entity.HumanoidArm.LEFT
                : net.minecraft.world.entity.HumanoidArm.RIGHT);
    }

    /** Forge: refreshes the display name (after a NameFormat handler changed). */
    public String refreshDisplayName() {
        mcc$player().refreshDisplayName();
        return getDisplayNameString();
    }

    /** Sleep results of trySleep. */
    public enum SleepResult {
        OK, NOT_POSSIBLE_HERE, NOT_POSSIBLE_NOW, TOO_FAR_AWAY, OTHER_PROBLEM, NOT_SAFE
    }
}
