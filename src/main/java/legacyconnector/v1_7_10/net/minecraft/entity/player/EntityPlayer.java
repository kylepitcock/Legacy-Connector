package legacyconnector.v1_7_10.net.minecraft.entity.player;

import com.mojang.authlib.GameProfile;
import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_7_10.net.minecraft.entity.item.EntityItem;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.util.ChunkCoordinates;
import legacyconnector.v1_7_10.net.minecraft.util.FoodStats;
import legacyconnector.v1_7_10.net.minecraft.util.IChatComponent;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.BlockBridge;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.GuiBridge;

public abstract class EntityPlayer extends EntityLivingBase {
    /** Phantom: redirected to {@link #mcc$get$eyeHeight}/{@link #mcc$set$eyeHeight} (Forge's field; mods change it, Thermal Dynamics' ducts). */
    @Deprecated
    public float eyeHeight;
    /** An eye height a mod set through {@link #eyeHeight}, or NaN for the default. */
    protected float mcc$eyeHeightOverride = Float.NaN;

    public static float mcc$get$eyeHeight(EntityPlayer p) {
        return p.getEyeHeight();
    }

    public static void mcc$set$eyeHeight(EntityPlayer p, float v) {
        p.mcc$eyeHeightOverride = v;
    }

    @Override
    public float getEyeHeight() {
        return Float.isNaN(mcc$eyeHeightOverride) ? super.getEyeHeight() : mcc$eyeHeightOverride;
    }
    /** The outcome of trying to sleep in a bed. */
    public enum EnumStatus {
        OK, NOT_POSSIBLE_HERE, NOT_POSSIBLE_NOW, TOO_FAR_AWAY, OTHER_PROBLEM, NOT_SAFE
    }

    public final InventoryPlayer inventory;
    public final PlayerCapabilities capabilities;
    /** Phantom: the legacy container behind the open menu (null for modern menus). */
    @Deprecated
    public legacyconnector.v1_7_10.net.minecraft.inventory.Container openContainer;
    /** Phantom: the container of the player's own inventory (redirected to {@code mcc$get$inventoryContainer}). */
    @Deprecated
    public legacyconnector.v1_7_10.net.minecraft.inventory.Container inventoryContainer;
    private legacyconnector.v1_7_10.net.minecraft.inventory.Container mcc$inventoryContainer;
    @Deprecated
    public int experienceLevel;
    @Deprecated
    public int experienceTotal;
    @Deprecated
    public float experience;
    private final FoodStats mcc$food;
    private ItemStack mcc$itemInUse;

    protected EntityPlayer(Player modern) {
        super(modern);
        this.inventory = new InventoryPlayer(this);
        this.capabilities = new PlayerCapabilities(modern);
        this.mcc$food = new FoodStats(modern.getFoodData());
    }

    public Player mcc$player() {
        return (Player) mcc$entity;
    }

    public static legacyconnector.v1_7_10.net.minecraft.inventory.Container mcc$get$inventoryContainer(EntityPlayer p) {
        if (p.mcc$inventoryContainer == null) {
            p.mcc$inventoryContainer = new legacyconnector.v1_7_10.net.minecraft.inventory.ContainerPlayer(p.inventory, !p.mcc$player().level().isClientSide(), p);
        }
        return p.mcc$inventoryContainer;
    }

    public static legacyconnector.v1_7_10.net.minecraft.inventory.Container mcc$get$openContainer(EntityPlayer p) {
        // As 1.7.10: with nothing open (or a menu legacy code knows nothing about) it is the player's own container, never null.
        return p.mcc$player().containerMenu instanceof org.kylepitcock.legacyconnector.v1_7_10.gui.LegacyMenu m ? m.legacy() : mcc$get$inventoryContainer(p);
    }

    public static void mcc$set$openContainer(EntityPlayer p, legacyconnector.v1_7_10.net.minecraft.inventory.Container c) {
        // Legacy code assigned this when opening containers manually; modern menus are opened by the bridge.
    }

    public static int mcc$get$experienceLevel(EntityPlayer p) {
        return p.mcc$player().experienceLevel;
    }

    public static void mcc$set$experienceLevel(EntityPlayer p, int v) {
        p.mcc$player().experienceLevel = v;
    }

    public static int mcc$get$experienceTotal(EntityPlayer p) {
        return p.mcc$player().totalExperience;
    }

    public static void mcc$set$experienceTotal(EntityPlayer p, int v) {
        p.mcc$player().totalExperience = v;
    }

    public static float mcc$get$experience(EntityPlayer p) {
        return p.mcc$player().experienceProgress;
    }

    public static void mcc$set$experience(EntityPlayer p, float v) {
        p.mcc$player().experienceProgress = v;
    }

    public GameProfile getGameProfile() {
        return mcc$player().getGameProfile();
    }

    public legacyconnector.v1_7_10.net.minecraft.scoreboard.Scoreboard getWorldScoreboard() {
        return mcc$entity == null ? null : legacyconnector.v1_7_10.net.minecraft.scoreboard.Scoreboard.mcc$wrap(mcc$entity.level().getScoreboard());
    }

    @Override
    public String getCommandSenderName() {
        return getGameProfile().getName();
    }

    public String getDisplayName() {
        return mcc$player().getDisplayName().getString();
    }

    public void addChatComponentMessage(IChatComponent message) {
        mcc$player().sendSystemMessage(message.mcc$toModern());
    }

    public ItemStack getCurrentEquippedItem() {
        return inventory.getCurrentItem();
    }

    public void destroyCurrentEquippedItem() {
        mcc$player().getInventory().setItem(mcc$player().getInventory().selected, net.minecraft.world.item.ItemStack.EMPTY);
    }

    public ItemStack getCurrentArmor(int slot) {
        return inventory.armorItemInSlot(slot);
    }

    public FoodStats getFoodStats() {
        return mcc$food;
    }

    public boolean canEat(boolean ignoreHunger) {
        return mcc$player().canEat(ignoreHunger);
    }

    public boolean shouldHeal() {
        return getHealth() > 0 && getHealth() < getMaxHealth();
    }

    public void addExhaustion(float amount) {
        mcc$player().causeFoodExhaustion(amount);
    }

    public void addExperience(int amount) {
        mcc$player().giveExperiencePoints(amount);
    }

    public void addExperienceLevel(int levels) {
        mcc$player().giveExperienceLevels(levels);
    }

    public int xpBarCap() {
        return mcc$player().getXpNeededForNextLevel();
    }

    public void addScore(int score) {
        mcc$player().increaseScore(score);
    }

    public int getScore() {
        return mcc$player().getScore();
    }

    public boolean canPlayerEdit(int x, int y, int z, int side, ItemStack stack) {
        Direction d = side >= 0 && side < 6 ? Direction.from3DDataValue(side) : Direction.UP;
        return mcc$player().mayUseItemAt(new BlockPos(x, y, z), d, ItemStack.mcc$toModern(stack));
    }

    public boolean canHarvestBlock(Block block) {
        return mcc$player().hasCorrectToolForDrops(BlockBridge.toModern(block, 0));
    }

    public float getCurrentPlayerStrVsBlock(Block block, boolean useTool) {
        return mcc$player().getDestroySpeed(BlockBridge.toModern(block, 0));
    }

    public float getBreakSpeed(Block block, boolean useTool, int meta, int x, int y, int z) {
        return mcc$breakSpeed(block, meta);
    }

    public float mcc$breakSpeed(Block block, int meta) {
        return mcc$player().getDestroySpeed(BlockBridge.toModern(block, meta));
    }

    public void setItemInUse(ItemStack stack, int duration) {
        mcc$itemInUse = stack;
        mcc$player().startUsingItem(net.minecraft.world.InteractionHand.MAIN_HAND);
    }

    public ItemStack getItemInUse() {
        return mcc$player().isUsingItem() ? ItemStack.mcc$wrap(mcc$player().getUseItem()) : null;
    }

    public int getItemInUseCount() {
        return mcc$player().getUseItemRemainingTicks();
    }

    public boolean isUsingItem() {
        return mcc$player().isUsingItem();
    }

    public int getItemInUseDuration() {
        return mcc$player().getTicksUsingItem();
    }

    public void stopUsingItem() {
        mcc$player().releaseUsingItem();
    }

    public void clearItemInUse() {
        mcc$player().stopUsingItem();
    }

    public boolean isBlocking() {
        return mcc$player().isBlocking();
    }

    /** Whether this player may be hurt by {@code other} (the server's player-versus-player setting). */
    public boolean canAttackPlayer(EntityPlayer other) {
        var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        return server == null || server.isPvpAllowed();
    }

    /** Plays the pickup animation of {@code entity} flying into this player. */
    public void onItemPickup(legacyconnector.v1_7_10.net.minecraft.entity.Entity entity, int count) {
        if (entity != null && entity.mcc$entity != null) mcc$player().take(entity.mcc$entity, count);
    }

    public EntityItem dropPlayerItemWithRandomChoice(ItemStack stack, boolean randomly) {
        var dropped = mcc$player().drop(ItemStack.mcc$toModern(stack), randomly, false);
        return dropped == null ? null : (EntityItem) org.kylepitcock.legacyconnector.v1_7_10.bridge.EntityBridge.wrap(dropped);
    }

    public EntityItem dropOneItem(boolean wholeStack) {
        return null;
    }

    public void openGui(Object mod, int id, World world, int x, int y, int z) {
        GuiBridge.openGui(this, mod, id, world, x, y, z);
    }

    /**
     * Shows a chest-style window over {@code inventory} (what a vanilla chest or a mod's storage calls on a player).
     * The window is the modern generic chest menu, so only sizes that are a multiple of nine up to 54 can be shown.
     */
    public void displayGUIChest(legacyconnector.v1_7_10.net.minecraft.inventory.IInventory inventory) {
        if (!(mcc$player() instanceof net.minecraft.server.level.ServerPlayer sp) || inventory == null) return;
        int size = inventory.getSizeInventory();
        if (size < 9 || size > 54 || size % 9 != 0) {
            org.slf4j.LoggerFactory.getLogger("LegacyConnector/Gui").warn("displayGUIChest: cannot show an inventory of {} slots", size);
            return;
        }
        int rows = size / 9;
        var view = org.kylepitcock.legacyconnector.v1_7_10.gui.LegacyInventoryContainer.of(inventory);
        var type = switch (rows) {
            case 1 -> net.minecraft.world.inventory.MenuType.GENERIC_9x1;
            case 2 -> net.minecraft.world.inventory.MenuType.GENERIC_9x2;
            case 3 -> net.minecraft.world.inventory.MenuType.GENERIC_9x3;
            case 4 -> net.minecraft.world.inventory.MenuType.GENERIC_9x4;
            case 5 -> net.minecraft.world.inventory.MenuType.GENERIC_9x5;
            default -> net.minecraft.world.inventory.MenuType.GENERIC_9x6;
        };
        String name = inventory.getInventoryName();
        net.minecraft.network.chat.Component title = name == null ? net.minecraft.network.chat.Component.empty()
                : net.minecraft.network.chat.Component.translatableWithFallback(name, name);
        sp.openMenu(new net.minecraft.world.SimpleMenuProvider(
                (windowId, inv, p) -> new net.minecraft.world.inventory.ChestMenu(type, windowId, inv, view, rows), title));
    }

    public void closeScreen() {
        mcc$player().closeContainer();
    }

    public ChunkCoordinates getBedLocation() {
        return null;
    }

    public boolean isPlayerFullyAsleep() {
        return mcc$player().isSleepingLongEnough();
    }

    public boolean isSpectator() {
        return mcc$player().isSpectator();
    }

    public void triggerAchievement(legacyconnector.v1_7_10.net.minecraft.stats.StatBase stat) {
        addStat(stat, 1);
    }

    private static final String ACHIEVEMENTS_TAG = "legacyconnector_achievements";

    /** Achievements earned are remembered in the player's persistent data; counts of other stats are not tracked. */
    public boolean hasAchievementUnlocked(legacyconnector.v1_7_10.net.minecraft.stats.Achievement achievement) {
        var data = mcc$player().getPersistentData();
        if (!data.contains(ACHIEVEMENTS_TAG, 9)) return false;
        for (var tag : data.getList(ACHIEVEMENTS_TAG, 8)) if (tag.getAsString().equals(achievement.statId)) return true;
        return false;
    }

    public void addStat(legacyconnector.v1_7_10.net.minecraft.stats.StatBase stat, int amount) {
        if (!(stat instanceof legacyconnector.v1_7_10.net.minecraft.stats.Achievement achievement)) return;
        // an achievement needs its parent first, and is only earned once
        if (achievement.parentAchievement != null && !hasAchievementUnlocked(achievement.parentAchievement)) return;
        if (hasAchievementUnlocked(achievement)) return;
        var data = mcc$player().getPersistentData();
        var list = data.getList(ACHIEVEMENTS_TAG, 8);
        list.add(net.minecraft.nbt.StringTag.valueOf(achievement.statId));
        data.put(ACHIEVEMENTS_TAG, list);
        String name = achievement.getStatName().getFormattedText();
        if (mcc$player() instanceof net.minecraft.server.level.ServerPlayer sp) {
            if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient() && sp.getServer() != null && sp.getServer().isSingleplayer()
                    || net.neoforged.fml.loading.FMLEnvironment.dist.isClient() && org.kylepitcock.legacyconnector.v1_7_10.client.LegacyToasts.isLocal(sp.getUUID())) {
                org.kylepitcock.legacyconnector.v1_7_10.client.LegacyToasts.achievement(name);
            } else {
                sp.sendSystemMessage(net.minecraft.network.chat.Component.literal("Achievement get! " + name));
            }
        }
    }

    public float getDefaultEyeHeight() {
        return 0.12F;
    }

    /** Whether the player turned their cape off in the skin customisation options. */
    public boolean getHideCape() {
        return getHideCape(1);
    }

    /** Bit {@code bit} of 1.7.10's hidden-parts byte; only bit 1 (the cape) existed, read from the modern model-part settings. */
    protected boolean getHideCape(int bit) {
        return bit == 1 && mcc$entity != null && !mcc$player().isModelPartShown(net.minecraft.world.entity.player.PlayerModelPart.CAPE);
    }

    /** Players wait 80 ticks in a portal before travelling, or none when they cannot take damage (creative). */
    @Override
    public int getMaxInPortalTime() {
        return PlayerCapabilities.mcc$get$disableDamage(capabilities) ? 0 : 80;
    }

    public void setSpawnChunk(ChunkCoordinates coordinates, boolean forced, int dimension) {
        if (coordinates == null || !(mcc$player() instanceof net.minecraft.server.level.ServerPlayer sp)) return;
        var target = org.kylepitcock.legacyconnector.v1_7_10.bridge.DimensionBridge.level(sp.server, dimension);
        var key = target == null ? sp.level().dimension() : target.dimension();
        if (key != null) sp.setRespawnPosition(key, new BlockPos(coordinates.posX, coordinates.posY, coordinates.posZ), 0.0F, forced, false);
    }

    public void mcc$addStat(String name) {
    }

    public void sendPlayerAbilities() {
        mcc$player().onUpdateAbilities();
    }

    public boolean isCurrentToolAdventureModeExempt(int x, int y, int z) {
        return true;
    }

    /** Changes the player's game mode (server players; the client follows). */
    public void setGameType(legacyconnector.v1_7_10.net.minecraft.world.WorldSettings.GameType type) {
        if (type == null || !(mcc$player() instanceof net.minecraft.server.level.ServerPlayer sp)) return;
        var mode = net.minecraft.world.level.GameType.byId(type.getID());
        if (type.getID() >= 0) sp.setGameMode(mode);
    }

    // ------------------------------------------------------------------ fishing

    /** Phantom: the player's fishing hook (a mod's legacy hook, or the vanilla hook the player cast), redirected to the accessors. */
    @Deprecated
    public legacyconnector.v1_7_10.net.minecraft.entity.projectile.EntityFishHook fishEntity;
    private legacyconnector.v1_7_10.net.minecraft.entity.projectile.EntityFishHook mcc$fishEntity;

    public static legacyconnector.v1_7_10.net.minecraft.entity.projectile.EntityFishHook mcc$get$fishEntity(EntityPlayer p) {
        if (p.mcc$fishEntity != null) return p.mcc$fishEntity;
        var hook = p.mcc$entity instanceof Player player ? player.fishing : null;
        return hook != null && org.kylepitcock.legacyconnector.v1_7_10.bridge.EntityBridge.wrap(hook) instanceof legacyconnector.v1_7_10.net.minecraft.entity.projectile.EntityFishHook h
                ? h : null;
    }

    public static void mcc$set$fishEntity(EntityPlayer p, legacyconnector.v1_7_10.net.minecraft.entity.projectile.EntityFishHook hook) {
        p.mcc$fishEntity = hook;
    }

    // ------------------------------------------------------------------ sleeping and respawning

    /** Tries to sleep in the bed at the position (the modern checks: dimension, time, distance, monsters nearby). */
    public EnumStatus sleepInBedAt(int x, int y, int z) {
        var pos = new BlockPos(x, y, z);
        if (!(mcc$player() instanceof net.minecraft.server.level.ServerPlayer sp)) {
            mcc$player().startSleeping(pos);
            return EnumStatus.OK;
        }
        var result = sp.startSleepInBed(pos);
        if (result.right().isPresent()) return EnumStatus.OK;
        return switch (result.left().get()) {
            case NOT_POSSIBLE_HERE -> EnumStatus.NOT_POSSIBLE_HERE;
            case NOT_POSSIBLE_NOW -> EnumStatus.NOT_POSSIBLE_NOW;
            case TOO_FAR_AWAY -> EnumStatus.TOO_FAR_AWAY;
            case NOT_SAFE -> EnumStatus.NOT_SAFE;
            default -> EnumStatus.OTHER_PROBLEM;
        };
    }

    /**
     * Gets out of bed. {@code immediately}: no wake-up delay; {@code updateWorld}: the level re-checks whether everyone sleeps;
     * {@code setSpawn}: the bed becomes the spawn point (the modern game set it when the player lay down).
     */
    public void wakeUpPlayer(boolean immediately, boolean updateWorld, boolean setSpawn) {
        if (mcc$player().isSleeping()) mcc$player().stopSleepInBed(immediately, updateWorld);
    }

    /** Forge: the spawn point in the current dimension. */
    public void setSpawnChunk(ChunkCoordinates coordinates, boolean forced) {
        if (mcc$entity == null) return;
        setSpawnChunk(coordinates, forced, mcc$get$dimension(this));
    }

    /**
     * Where a player whose spawn point is {@code coordinates} respawns: beside the bed there, at the point itself when it is forced
     * and free (two blocks of non-solid, non-liquid space), or null.
     */
    public static ChunkCoordinates verifyRespawnCoordinates(World world, ChunkCoordinates coordinates, boolean forced) {
        if (world == null || coordinates == null) return null;
        var level = world.mcc$fullLevel();
        if (level == null) return null;
        var pos = new BlockPos(coordinates.posX, coordinates.posY, coordinates.posZ);
        var state = level.getBlockState(pos);
        if (state.isBed(level, pos, null)) {
            var spot = state.getRespawnPosition(net.minecraft.world.entity.EntityType.PLAYER, level, pos, 0.0F);
            if (spot.isEmpty()) return null;
            var v = spot.get().position();
            return new ChunkCoordinates((int) Math.floor(v.x), (int) Math.floor(v.y), (int) Math.floor(v.z));
        }
        var above = level.getBlockState(pos.above());
        boolean free = !state.isSolid() && !state.liquid() && !above.isSolid() && !above.liquid();
        return forced && free ? coordinates : null;
    }

    /** Copies what a respawned (or returning) player keeps from its previous entity (all of it when {@code keepEverything}). */
    public void clonePlayer(EntityPlayer old, boolean keepEverything) {
        if (old != null && mcc$player() instanceof net.minecraft.server.level.ServerPlayer sp && old.mcc$player() instanceof net.minecraft.server.level.ServerPlayer o && sp != o) {
            sp.restoreFrom(o, keepEverything);
        }
    }

    /** The UUID of a profile: its own, or the offline-mode UUID derived from the name. */
    public static java.util.UUID getUUID(GameProfile profile) {
        java.util.UUID id = profile.getId();
        return id != null ? id : net.minecraft.core.UUIDUtil.createOfflinePlayerUUID(profile.getName());
    }

    // ------------------------------------------------------------------ interaction

    /** How much of the body armour covers (worn pieces / 4); mobs see a well-armoured invisible player more easily. */
    public float getArmorVisibility() {
        return mcc$player().getArmorCoverPercentage();
    }

    /** Right-clicks an entity with the held item (the entity's own interaction first, then the item's). */
    public boolean interactWith(legacyconnector.v1_7_10.net.minecraft.entity.Entity entity) {
        if (entity == null || entity.mcc$entity == null) return false;
        return mcc$player().interactOn(entity.mcc$entity, net.minecraft.world.InteractionHand.MAIN_HAND).consumesAction();
    }

    /** Attacks an entity with the held item (criticals, sweeping, knockback and enchantments included). */
    public void attackTargetEntityWithCurrentItem(legacyconnector.v1_7_10.net.minecraft.entity.Entity target) {
        if (target != null && target.mcc$entity != null) mcc$player().attack(target.mcc$entity);
    }

    /** Opens the trading screen of a merchant (a villager or a mod's trader). */
    public void displayGUIMerchant(legacyconnector.v1_7_10.net.minecraft.entity.IMerchant merchant, String name) {
        org.kylepitcock.legacyconnector.v1_7_10.bridge.MerchantBridge.open(this, merchant, name);
    }

    /** Client: the command block editor; the base player has no screen to show (the client player's own class opens it). */
    public void func_146095_a(legacyconnector.v1_7_10.net.minecraft.command.server.CommandBlockLogic logic) {
    }

    private legacyconnector.v1_7_10.net.minecraft.inventory.InventoryEnderChest mcc$enderChest;

    /** The player's ender chest contents (shared with vanilla ender chests). */
    public legacyconnector.v1_7_10.net.minecraft.inventory.InventoryEnderChest getInventoryEnderChest() {
        if (mcc$enderChest == null) mcc$enderChest = new legacyconnector.v1_7_10.net.minecraft.inventory.InventoryEnderChest(mcc$player().getEnderChestInventory());
        return mcc$enderChest;
    }

    @Override
    public boolean isPushedByWater() {
        return !mcc$player().getAbilities().flying;
    }

    @Override
    protected boolean isPlayer() {
        return true;
    }

    /** Players are offset half a block down when they ride (their legacy {@code yOffset} minus 0.5). */
    @Override
    public double getYOffset() {
        return mcc$posYShift - 0.5D;
    }

    @Override
    public void addToPlayerScore(legacyconnector.v1_7_10.net.minecraft.entity.Entity killed, int score) {
        addScore(score);
    }

    @Override
    public boolean canCommandSenderUseCommand(int level, String command) {
        return mcc$player().hasPermissions(level);
    }

    @Override
    public ChunkCoordinates getPlayerCoordinates() {
        BlockPos p = mcc$player().blockPosition();
        return new ChunkCoordinates(p.getX(), p.getY(), p.getZ());
    }

    public void onEnchantmentCritical(legacyconnector.v1_7_10.net.minecraft.entity.Entity target) {
    }

    public legacyconnector.v1_7_10.net.minecraft.util.ChunkCoordinates getBedLocation(int dimension) {
        if (mcc$entity instanceof net.minecraft.server.level.ServerPlayer sp && sp.getRespawnPosition() != null) {
            var pos = sp.getRespawnPosition();
            return new legacyconnector.v1_7_10.net.minecraft.util.ChunkCoordinates(pos.getX(), pos.getY(), pos.getZ());
        }
        return null;
    }

    public void onCriticalHit(legacyconnector.v1_7_10.net.minecraft.entity.Entity target) {
    }

    /** Drops a stack from the player (the legacy dropPlayerItem): thrown forward, or scattered when {@code scatter}. */
    public legacyconnector.v1_7_10.net.minecraft.entity.item.EntityItem func_146097_a(legacyconnector.v1_7_10.net.minecraft.item.ItemStack stack, boolean scatter, boolean setThrower) {
        if (stack == null) return null;
        var modern = stack.mcc$modern();
        if (modern.isEmpty()) return null;
        var dropped = mcc$player().drop(modern, scatter, setThrower);
        return dropped == null ? null : (legacyconnector.v1_7_10.net.minecraft.entity.item.EntityItem) org.kylepitcock.legacyconnector.v1_7_10.bridge.EntityBridge.wrap(dropped);
    }
}
