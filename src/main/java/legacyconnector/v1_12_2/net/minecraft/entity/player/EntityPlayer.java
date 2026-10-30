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
    /** Vanilla's absorption key: the modern player's absorption amount. */
    public static final legacyconnector.v1_12_2.net.minecraft.network.datasync.DataParameter<Float> ABSORPTION = org.kylepitcock.legacyconnector.v1_12_2.entity.VanillaDataKey1122.of(11, legacyconnector.v1_12_2.net.minecraft.network.datasync.DataSerializers.FLOAT, EntityPlayer.class, 0.0F, e -> e.mcc$living().getAbsorptionAmount(),
            (e, v) -> e.mcc$living().setAbsorptionAmount(v));

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
        if (capability != null && mcc$baubles(capability)) return true;
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
        if (capability != null && mcc$baubles(capability)) return (T) legacyconnector.v1_12_2.baubles.api.BaublesApi.getBaublesHandler(this);
        return null;
    }

    /** Baubles' slots, answered from Curios when the Baubles bridge is active (Curios installed). */
    private static boolean mcc$baubles(legacyconnector.v1_12_2.net.minecraftforge.common.capabilities.Capability<?> capability) {
        return capability == legacyconnector.v1_12_2.baubles.api.cap.BaublesCapabilities.CAPABILITY_BAUBLES
                && net.neoforged.fml.ModList.get() != null && net.neoforged.fml.ModList.get().isLoaded("curios");
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
    public void refreshDisplayName() {
        mcc$player().refreshDisplayName();
    }

    /** The scoreboard of the player's world. */
    public legacyconnector.v1_12_2.net.minecraft.scoreboard.Scoreboard getWorldScoreboard() {
        return legacyconnector.v1_12_2.net.minecraft.scoreboard.Scoreboard.mcc$of(mcc$player().level().getScoreboard());
    }

    /** Opens the sign editor for a sign of the game (front text). */
    public void openEditSign(legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntitySign sign) {
        var modern = sign == null ? null : sign.mcc$modern();
        if (modern != null) mcc$player().openTextEdit(modern, true);
    }

    // ------------------------------------------------------------------ containers, GUIs and statistics (area E, 1.12.2)

    @Deprecated
    public legacyconnector.v1_12_2.net.minecraft.inventory.Container openContainer;
    @Deprecated
    public legacyconnector.v1_12_2.net.minecraft.inventory.Container inventoryContainer;
    @Deprecated
    public BlockPos bedLocation;
    @Deprecated
    public legacyconnector.v1_12_2.net.minecraft.util.FoodStats foodStats;
    @Deprecated
    public int sleepTimer;
    /** Client render offsets while sleeping (only legacy renderers read them). */
    public float renderOffsetX;
    public float renderOffsetY;
    public float renderOffsetZ;
    private legacyconnector.v1_12_2.net.minecraft.inventory.InventoryEnderChest mcc$enderChest;

    /** The container the player has open: a legacy container, a view of a vanilla menu, or the inventory container. */
    public static legacyconnector.v1_12_2.net.minecraft.inventory.Container mcc$get$openContainer(EntityPlayer p) {
        Player m = p.mcc$player();
        if (m == null) return null;
        if (m.containerMenu == null || m.containerMenu == m.inventoryMenu) return mcc$get$inventoryContainer(p);
        return org.kylepitcock.legacyconnector.v1_12_2.entity.MenuView1122.of(m.containerMenu);
    }

    /** Mods reset {@code openContainer} to the inventory container after closing their own; a hosted container becomes the open menu. */
    public static void mcc$set$openContainer(EntityPlayer p, legacyconnector.v1_12_2.net.minecraft.inventory.Container v) {
        Player m = p.mcc$player();
        if (m == null) return;
        var menu = org.kylepitcock.legacyconnector.v1_12_2.entity.MenuView1122.modern(v);
        if (v == null || menu == m.inventoryMenu) m.containerMenu = m.inventoryMenu;
        else if (menu != null) m.containerMenu = menu;
    }

    public static legacyconnector.v1_12_2.net.minecraft.inventory.Container mcc$get$inventoryContainer(EntityPlayer p) {
        Player m = p.mcc$player();
        return m == null ? null : org.kylepitcock.legacyconnector.v1_12_2.entity.MenuView1122.of(m.inventoryMenu);
    }

    public static void mcc$set$inventoryContainer(EntityPlayer p, legacyconnector.v1_12_2.net.minecraft.inventory.Container v) {
        // the inventory menu belongs to the modern player
    }

    public static BlockPos mcc$get$bedLocation(EntityPlayer p) {
        return p.getBedLocation();
    }

    public static void mcc$set$bedLocation(EntityPlayer p, BlockPos v) {
        if (p.mcc$player() instanceof net.minecraft.server.level.ServerPlayer sp) {
            sp.setRespawnPosition(sp.getRespawnDimension(), v == null ? null : v.mcc$modern(), sp.getRespawnAngle(), sp.isRespawnForced(), false);
        }
    }

    public static legacyconnector.v1_12_2.net.minecraft.util.FoodStats mcc$get$foodStats(EntityPlayer p) {
        return p.getFoodStats();
    }

    public static void mcc$set$foodStats(EntityPlayer p, legacyconnector.v1_12_2.net.minecraft.util.FoodStats v) {
        // the food data belongs to the modern player
    }

    public static int mcc$get$sleepTimer(EntityPlayer p) {
        return p.getSleepTimer();
    }

    public static void mcc$set$sleepTimer(EntityPlayer p, int v) {
        // counted by the modern player while it sleeps
    }

    /** Awards a statistic (vanilla statistics count as their modern ones; a mod's own statistic has no modern counter). */
    public void addStat(legacyconnector.v1_12_2.net.minecraft.stats.StatBase stat) {
        addStat(stat, 1);
    }

    public void addStat(legacyconnector.v1_12_2.net.minecraft.stats.StatBase stat, int amount) {
        var modern = stat == null ? null : stat.mcc$modern();
        if (modern != null && mcc$player() != null) mcc$player().awardStat(modern, amount);
    }

    public void takeStat(legacyconnector.v1_12_2.net.minecraft.stats.StatBase stat) {
        var modern = stat == null ? null : stat.mcc$modern();
        if (modern != null && mcc$player() != null) mcc$player().resetStat(modern);
    }

    /** Opens a chest-like GUI on an inventory (server side; the client is told to show it). */
    public void displayGUIChest(legacyconnector.v1_12_2.net.minecraft.inventory.IInventory inventory) {
        org.kylepitcock.legacyconnector.v1_12_2.entity.PlayerGuis1122.displayGUIChest(this, inventory);
    }

    public void displayGui(legacyconnector.v1_12_2.net.minecraft.world.IInteractionObject guiOwner) {
        org.kylepitcock.legacyconnector.v1_12_2.entity.PlayerGuis1122.displayGui(this, guiOwner);
    }

    public void displayVillagerTradeGui(legacyconnector.v1_12_2.net.minecraft.entity.IMerchant merchant) {
        org.kylepitcock.legacyconnector.v1_12_2.entity.PlayerGuis1122.displayVillagerTradeGui(this, merchant);
    }

    public legacyconnector.v1_12_2.net.minecraft.inventory.InventoryEnderChest getInventoryEnderChest() {
        if (mcc$enderChest == null) mcc$enderChest = new legacyconnector.v1_12_2.net.minecraft.inventory.InventoryEnderChest(mcc$player().getEnderChestInventory());
        return mcc$enderChest;
    }

    /** Whether the player shows a skin layer (the client's skin customisation). */
    public boolean isWearing(EnumPlayerModelParts part) {
        return part != null && mcc$player().isModelPartShown(part.mcc$modern());
    }

    /** Lies down in the bed at {@code pos} (server: the modern sleep rules and Forge's sleep events); the 1.12 result. */
    public SleepResult trySleep(BlockPos pos) {
        if (pos == null) return SleepResult.OTHER_PROBLEM;
        var result = mcc$player().startSleepInBed(pos.mcc$modern());
        return result.right().isPresent() ? SleepResult.OK : switch (result.left().get()) {
            case NOT_POSSIBLE_HERE -> SleepResult.NOT_POSSIBLE_HERE;
            case NOT_POSSIBLE_NOW -> SleepResult.NOT_POSSIBLE_NOW;
            case TOO_FAR_AWAY -> SleepResult.TOO_FAR_AWAY;
            case NOT_SAFE -> SleepResult.NOT_SAFE;
            default -> SleepResult.OTHER_PROBLEM;
        };
    }

    /** The yaw a sleeping player faces in its bed (1.12: south 90, north 270, east 180, west 0). */
    public float getBedOrientationInDegrees() {
        var d = mcc$player().getBedOrientation();
        if (d == null) return 0.0F;
        return switch (d) {
            case SOUTH -> 90.0F;
            case NORTH -> 270.0F;
            case EAST -> 180.0F;
            default -> 0.0F;
        };
    }

    public void setRenderOffsetForSleep(EnumFacing facing) {
        renderOffsetX = -1.8F * facing.getXOffset();
        renderOffsetZ = -1.8F * facing.getZOffset();
    }

    /**
     * Where a player whose spawn point is {@code pos} would respawn: next to the bed there (the modern stand-up search), at {@code pos}
     * itself when the spawn is forced and the two blocks there are free, otherwise null.
     */
    public static BlockPos getBedSpawnLocation(World world, BlockPos pos, boolean forceSpawn) {
        if (world == null || pos == null || world.mcc$level() == null) return null;
        var level = world.mcc$level();
        var modernPos = pos.mcc$modern();
        var state = level.getBlockState(modernPos);
        if (state.getBlock() instanceof net.minecraft.world.level.block.BedBlock) {
            var found = net.minecraft.world.level.block.BedBlock.findStandUpPosition(net.minecraft.world.entity.EntityType.PLAYER, level, modernPos,
                    state.getValue(net.minecraft.world.level.block.BedBlock.FACING), 0.0F);
            return found.map(v -> new BlockPos(v.x, v.y, v.z)).orElse(null);
        }
        if (!forceSpawn) return null;
        boolean free = !level.getBlockState(modernPos).isSolid() && !level.getBlockState(modernPos.above()).isSolid();
        return free ? pos : null;
    }

    /** The 1.12 dimension id of the player's respawn point. */
    public int getSpawnDimension() {
        if (mcc$player() instanceof net.minecraft.server.level.ServerPlayer sp) {
            var level = sp.server.getLevel(sp.getRespawnDimension());
            return level == null ? 0 : org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122.dimensionId(level);
        }
        return 0;
    }

    public float getArmorVisibility() {
        return mcc$player().getArmorCoverPercentage();
    }

    private static java.lang.reflect.Method mcc$shoulderSetter(String name) {
        try {
            return net.neoforged.fml.util.ObfuscationReflectionHelper.findMethod(Player.class, name, net.minecraft.nbt.CompoundTag.class);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static final java.lang.reflect.Method MCC$SET_LEFT = mcc$shoulderSetter("setShoulderEntityLeft");
    private static final java.lang.reflect.Method MCC$SET_RIGHT = mcc$shoulderSetter("setShoulderEntityRight");

    private void mcc$setShoulder(java.lang.reflect.Method setter, legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound tag) {
        if (setter == null) return;
        try {
            setter.invoke(mcc$player(), tag == null ? new net.minecraft.nbt.CompoundTag() : tag.mcc$modern().copy());
        } catch (ReflectiveOperationException e) {
            // keeps the current shoulder entity
        }
    }

    /** Puts the saved form of an entity (a parrot) on the left shoulder (an empty tag clears it). */
    protected void setLeftShoulderEntity(legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound tag) {
        mcc$setShoulder(MCC$SET_LEFT, tag);
    }

    protected void setRightShoulderEntity(legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound tag) {
        mcc$setShoulder(MCC$SET_RIGHT, tag);
    }

    public legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound getLeftShoulderEntity() {
        return new legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound(mcc$player().getShoulderEntityLeft().copy());
    }

    public legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound getRightShoulderEntity() {
        return new legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound(mcc$player().getShoulderEntityRight().copy());
    }

    public void setGameType(legacyconnector.v1_12_2.net.minecraft.world.GameType type) {
        if (type != null && mcc$player() instanceof net.minecraft.server.level.ServerPlayer sp) sp.setGameMode(type.mcc$modern());
    }

    /** Unlocks recipes in the player's recipe book (the modern recipes of the same names). */
    public void unlockRecipes(java.util.List<legacyconnector.v1_12_2.net.minecraft.item.crafting.IRecipe> recipes) {
        if (recipes == null || !(mcc$player() instanceof net.minecraft.server.level.ServerPlayer sp)) return;
        java.util.List<net.minecraft.resources.ResourceLocation> ids = new java.util.ArrayList<>();
        for (var r : recipes) {
            if (r != null && r.getRegistryName() != null) ids.add(r.getRegistryName().mcc$modern());
        }
        sp.awardRecipesByKey(ids);
    }

    @Deprecated
    public legacyconnector.v1_12_2.net.minecraft.entity.projectile.EntityFishHook fishEntity;

    /** The player's fishing bobber (the modern one), or null. */
    public static legacyconnector.v1_12_2.net.minecraft.entity.projectile.EntityFishHook mcc$get$fishEntity(EntityPlayer p) {
        Player m = p.mcc$player();
        return m == null ? null : org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122.wrap(m.fishing)
                instanceof legacyconnector.v1_12_2.net.minecraft.entity.projectile.EntityFishHook h ? h : null;
    }

    public static void mcc$set$fishEntity(EntityPlayer p, legacyconnector.v1_12_2.net.minecraft.entity.projectile.EntityFishHook v) {
        Player m = p.mcc$player();
        if (m != null) m.fishing = v != null && v.mcc$entity instanceof net.minecraft.world.entity.projectile.FishingHook h ? h : null;
    }

    /** Sleep results of trySleep. */
    public enum SleepResult {
        OK, NOT_POSSIBLE_HERE, NOT_POSSIBLE_NOW, TOO_FAR_AWAY, OTHER_PROBLEM, NOT_SAFE
    }
}
