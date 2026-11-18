package legacyconnector.v1_12_2.net.minecraft.entity.player;

import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2 server player: a view over a modern server player. */
public class EntityPlayerMP extends EntityPlayer implements legacyconnector.v1_12_2.net.minecraft.inventory.IContainerListener {
    @Deprecated
    public legacyconnector.v1_12_2.net.minecraft.network.NetHandlerPlayServer connection;
    @Deprecated
    public legacyconnector.v1_12_2.net.minecraft.server.MinecraftServer server;
    @Deprecated
    public int currentWindowId;
    @Deprecated
    public boolean isChangingQuantityOnly;
    @Deprecated
    public boolean invulnerableDimensionChange;
    @Deprecated
    public int ping;
    private legacyconnector.v1_12_2.net.minecraft.network.NetHandlerPlayServer mcc$connection;

    public EntityPlayerMP(World world) {
        super(world, null);
    }

    /** 1.12's constructor of a server player (mods build fake players with it); the modern fake player is bound to the world and profile. */
    public EntityPlayerMP(legacyconnector.v1_12_2.net.minecraft.server.MinecraftServer server,
            legacyconnector.v1_12_2.net.minecraft.world.WorldServer world, com.mojang.authlib.GameProfile profile,
            legacyconnector.v1_12_2.net.minecraft.server.management.PlayerInteractionManager manager) {
        super(world, profile);
        if (manager != null) mcc$set$interactionManager(this, manager);
    }

    public EntityPlayerMP(net.minecraft.server.level.ServerPlayer modern) {
        super(modern);
    }

    public void setGameType(legacyconnector.v1_12_2.net.minecraft.world.GameType type) {
        var player = mcc$serverPlayer();
        if (player != null && type != null) player.setGameMode(type.mcc$modern());
    }

    /** The chat messages the player's client wants (its chat visibility setting). */
    public EntityPlayer.EnumChatVisibility getChatVisibility() {
        var p = mcc$serverPlayer();
        return p == null ? EntityPlayer.EnumChatVisibility.FULL : EntityPlayer.EnumChatVisibility.getEnumChatVisibility(p.getChatVisibility().getId());
    }

    /** Game time (ms) of the player's last action, for the idle kick. */
    public long getLastActiveTime() {
        var p = mcc$serverPlayer();
        return p == null ? 0L : p.getLastActionTime();
    }

    /** Ends the invulnerability a dimension change gives. */
    public void clearInvulnerableDimensionChange() {
        mcc$set$invulnerableDimensionChange(this, false);
    }

    /** The server's fall bookkeeping for a move the client reported (landing hands the fall to the block below). */
    public void handleFalling(double y, boolean onGroundIn) {
        legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos below = getPosition().down();
        legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state = mcc$world() == null ? null : mcc$world().getBlockState(below);
        updateFallState(y, onGroundIn, state, below);
    }

    public net.minecraft.server.level.ServerPlayer mcc$serverPlayer() {
        return (net.minecraft.server.level.ServerPlayer) mcc$entity;
    }

    public static legacyconnector.v1_12_2.net.minecraft.network.NetHandlerPlayServer mcc$get$connection(EntityPlayerMP p) {
        if (p.mcc$connection == null) p.mcc$connection = new legacyconnector.v1_12_2.net.minecraft.network.NetHandlerPlayServer(p);
        return p.mcc$connection;
    }

    public static legacyconnector.v1_12_2.net.minecraft.server.MinecraftServer mcc$get$server(EntityPlayerMP p) {
        return legacyconnector.v1_12_2.net.minecraft.server.MinecraftServer.mcc$instance();
    }

    public static int mcc$get$currentWindowId(EntityPlayerMP p) {
        var player = p.mcc$serverPlayer();
        return player == null ? 0 : player.containerMenu.containerId;
    }

    public static boolean mcc$get$invulnerableDimensionChange(EntityPlayerMP p) {
        var player = p.mcc$serverPlayer();
        return player != null && player.isChangingDimension();
    }

    public static void mcc$set$invulnerableDimensionChange(EntityPlayerMP p, boolean v) {
        // the modern flag is managed by the teleport itself
    }

    public static int mcc$get$ping(EntityPlayerMP p) {
        var player = p.mcc$serverPlayer();
        return player == null || player.connection == null ? 0 : player.connection.latency();
    }

    /** Operators of the needed level may use a command (single player's host and cheats-enabled worlds included). */
    @Override
    public boolean canUseCommand(int permLevel, String commandName) {
        var player = mcc$serverPlayer();
        return player != null && player.hasPermissions(permLevel);
    }

    @Override
    public void sendMessage(legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent component) {
        var player = mcc$serverPlayer();
        if (player != null && component != null) player.sendSystemMessage(component.mcc$modern());
    }

    @Override
    public boolean isSpectator() {
        return mcc$serverPlayer().isSpectator();
    }

    @Override
    public boolean isCreative() {
        return mcc$serverPlayer().isCreative();
    }

    public String getPlayerIP() {
        return mcc$serverPlayer().getIpAddress();
    }

    public boolean hasDisconnected() {
        return mcc$serverPlayer().hasDisconnected();
    }

    public void closeContainer() {
        mcc$serverPlayer().doCloseContainer();
    }

    public void getNextWindowId() {
        // the modern player numbers its containers when it opens them
    }

    public void updateHeldItem() {
        var player = mcc$serverPlayer();
        player.containerMenu.broadcastChanges();
    }

    public void setPlayerHealthUpdated() {
        mcc$serverPlayer().resetSentInfo();
    }

    public void mountEntityAndWakeUp() {
        var player = mcc$serverPlayer();
        player.ejectPassengers();
        if (player.isSleeping()) player.stopSleepInBed(true, false);
    }

    @Override
    public void setPositionAndUpdate(double x, double y, double z) {
        var player = mcc$serverPlayer();
        player.connection.teleport(x, y, z, player.getYRot(), player.getXRot());
    }

    public int getPing() {
        return mcc$get$ping(this);
    }

    // the player as a listener of the container it has open (progress bars, forced slot updates)

    @Override
    public void sendAllContents(legacyconnector.v1_12_2.net.minecraft.inventory.Container container,
            legacyconnector.v1_12_2.net.minecraft.util.NonNullList<legacyconnector.v1_12_2.net.minecraft.item.ItemStack> items) {
        org.kylepitcock.legacyconnector.v1_12_2.bridge.GuiBridge1122.sendAllContents(mcc$serverPlayer(), container);
    }

    @Override
    public void sendSlotContents(legacyconnector.v1_12_2.net.minecraft.inventory.Container container, int slot,
            legacyconnector.v1_12_2.net.minecraft.item.ItemStack stack) {
        org.kylepitcock.legacyconnector.v1_12_2.bridge.GuiBridge1122.sendSlotContents(mcc$serverPlayer(), container, slot, stack);
    }

    @Override
    public void sendWindowProperty(legacyconnector.v1_12_2.net.minecraft.inventory.Container container, int id, int value) {
        org.kylepitcock.legacyconnector.v1_12_2.bridge.GuiBridge1122.sendWindowProperty(mcc$serverPlayer(), container, id, value);
    }

    @Override
    public void sendAllWindowProperties(legacyconnector.v1_12_2.net.minecraft.inventory.Container container,
            legacyconnector.v1_12_2.net.minecraft.inventory.IInventory inventory) {
        for (int i = 0; i < inventory.getFieldCount(); ++i) sendWindowProperty(container, i, inventory.getField(i));
    }

    /** A handler a mod built for this player (fake players get theirs from the handler's constructor). */
    public static void mcc$set$connection(EntityPlayerMP p, legacyconnector.v1_12_2.net.minecraft.network.NetHandlerPlayServer handler) {
        p.mcc$connection = handler;
    }


    /** The player's advancements (area F; a view over the modern player's). */
    public legacyconnector.v1_12_2.net.minecraft.advancements.PlayerAdvancements getAdvancements() {
        return new legacyconnector.v1_12_2.net.minecraft.advancements.PlayerAdvancements(this);
    }

    /** Phantom field (area F): accesses are redirected to {@code mcc$get$interactionManager}, a view over the modern game mode. */
    @Deprecated
    public legacyconnector.v1_12_2.net.minecraft.server.management.PlayerInteractionManager interactionManager;
    private legacyconnector.v1_12_2.net.minecraft.server.management.PlayerInteractionManager mcc$interactionManager;

    public static legacyconnector.v1_12_2.net.minecraft.server.management.PlayerInteractionManager mcc$get$interactionManager(EntityPlayerMP p) {
        if (p.mcc$interactionManager == null) p.mcc$interactionManager = new legacyconnector.v1_12_2.net.minecraft.server.management.PlayerInteractionManager(p);
        return p.mcc$interactionManager;
    }

    public static void mcc$set$interactionManager(EntityPlayerMP p, legacyconnector.v1_12_2.net.minecraft.server.management.PlayerInteractionManager manager) {
        if (manager != null) manager.player = p;
        p.mcc$interactionManager = manager;
    }

    public static legacyconnector.v1_12_2.net.minecraft.entity.Entity mcc$get$spectatingEntity(EntityPlayerMP p) {
        return p.getSpectatingEntity();
    }

    public static void mcc$set$spectatingEntity(EntityPlayerMP p, legacyconnector.v1_12_2.net.minecraft.entity.Entity v) {
        p.setSpectatingEntity(v);
    }

    public static String mcc$get$language(EntityPlayerMP p) {
        var sp = p.mcc$serverPlayer();
        return sp == null ? "en_us" : sp.clientInformation().language();
    }

    public static void mcc$set$language(EntityPlayerMP p, String v) {
        // reported by the client
    }

    public legacyconnector.v1_12_2.net.minecraft.world.WorldServer getServerWorld() {
        return (legacyconnector.v1_12_2.net.minecraft.world.WorldServer) org.kylepitcock.legacyconnector.v1_12_2.bridge.WorldBridge1122.wrap(mcc$serverPlayer().serverLevel());
    }

    /** Resends the whole container (slots, carried stack and properties) to the client. */
    public void sendContainerToPlayer(legacyconnector.v1_12_2.net.minecraft.inventory.Container container) {
        var menu = org.kylepitcock.legacyconnector.v1_12_2.entity.MenuView1122.modern(container);
        if (menu != null) menu.sendAllDataToRemote();
        else if (container != null) mcc$serverPlayer().containerMenu.sendAllDataToRemote();
    }

    public legacyconnector.v1_12_2.net.minecraft.entity.Entity getSpectatingEntity() {
        var camera = mcc$serverPlayer().getCamera();
        return org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122.wrap(camera);
    }

    public void setSpectatingEntity(legacyconnector.v1_12_2.net.minecraft.entity.Entity entity) {
        mcc$serverPlayer().setCamera(entity == null ? null : entity.mcc$entity);
    }

    /** Whether the player is mid dimension change (takes no damage from it). */
    public boolean isInvulnerableDimensionChange() {
        return mcc$get$invulnerableDimensionChange(this);
    }

    /** Ends elytra flight. */
    public void clearElytraFlying() {
        var p = mcc$player();
        if (p != null && p.isFallFlying()) p.stopFallFlying();
    }

    /** Starts elytra flight (the player glides until they land). */
    public void setElytraFlying() {
        var p = mcc$player();
        if (p != null && !p.isFallFlying()) p.startFallFlying();
    }

    public void markPlayerActive() {
        mcc$serverPlayer().resetLastActionTime();
    }

    public boolean hasSpawnDimension() {
        return true;
    }

    /** Forge: moves the player to another dimension; the teleporter places it (or the same position when it is a vanilla teleporter). */
    @Override
    public legacyconnector.v1_12_2.net.minecraft.entity.Entity changeDimension(int dimension,
            legacyconnector.v1_12_2.net.minecraftforge.common.util.ITeleporter teleporter) {
        return super.changeDimension(dimension, teleporter);
    }

    /** The player's recipe book (the modern server book: unlocking there also tells the client). */
    public legacyconnector.v1_12_2.net.minecraft.stats.RecipeBookServer getRecipeBook() {
        var sp = mcc$serverPlayer();
        return sp == null ? new legacyconnector.v1_12_2.net.minecraft.stats.RecipeBookServer()
                : legacyconnector.v1_12_2.net.minecraft.stats.RecipeBookServer.mcc$of(sp.getRecipeBook());
    }

    /** The player's statistics. */
    public legacyconnector.v1_12_2.net.minecraft.stats.StatisticsManagerServer getStatFile() {
        var sp = mcc$serverPlayer();
        return sp == null ? null : legacyconnector.v1_12_2.net.minecraft.stats.StatisticsManagerServer.mcc$of(sp.getStats());
    }

    /** The values last sent to the client (1.12's change detection of the player list); -99999999 forces the first send. */
    public float lastHealth = -1.0E8F;
    public int lastFoodLevel = -99999999;
    public int lastExperience = -99999999;
    public double managedPosX;
    public double managedPosZ;

    @Deprecated
    public legacyconnector.v1_12_2.net.minecraft.server.MinecraftServer mcServer;
    @Deprecated
    public boolean queuedEndExit;
    @Deprecated
    public legacyconnector.v1_12_2.net.minecraft.stats.RecipeBookServer recipeBook;

    public static legacyconnector.v1_12_2.net.minecraft.server.MinecraftServer mcc$get$mcServer(EntityPlayerMP p) {
        return mcc$get$server(p);
    }

    public static void mcc$set$mcServer(EntityPlayerMP p, legacyconnector.v1_12_2.net.minecraft.server.MinecraftServer v) {
        // the one server of the game
    }

    /** Whether the player left the End by its exit portal and has the credits (and the dimension change) pending. */
    public static boolean mcc$get$queuedEndExit(EntityPlayerMP p) {
        var sp = p.mcc$serverPlayer();
        return sp != null && sp.wonGame;
    }

    public static void mcc$set$queuedEndExit(EntityPlayerMP p, boolean v) {
        var sp = p.mcc$serverPlayer();
        if (sp != null) sp.wonGame = v;
    }

    public static legacyconnector.v1_12_2.net.minecraft.stats.RecipeBookServer mcc$get$recipeBook(EntityPlayerMP p) {
        return p.getRecipeBook();
    }

    public static void mcc$set$recipeBook(EntityPlayerMP p, legacyconnector.v1_12_2.net.minecraft.stats.RecipeBookServer v) {
        // the recipe book belongs to the modern player
    }

    /** Takes over everything of the player this one replaces (a respawn or a return from the End); the inventory only when it is the same life. */
    public void copyFrom(EntityPlayerMP old, boolean keepEverything) {
        var sp = mcc$serverPlayer();
        if (sp != null && old != null && old.mcc$serverPlayer() != null) sp.restoreFrom(old.mcc$serverPlayer(), keepEverything);
    }

    /** Where the player entered the Nether (the portal link of the way back), or null. */
    public legacyconnector.v1_12_2.net.minecraft.util.math.Vec3d getEnteredNetherPosition() {
        var sp = mcc$serverPlayer();
        net.minecraft.world.phys.Vec3 v = null;
        try {
            if (sp != null) v = net.neoforged.fml.util.ObfuscationReflectionHelper.getPrivateValue(net.minecraft.server.level.ServerPlayer.class, sp, "enteredNetherPosition");
        } catch (RuntimeException e) {
            v = null;
        }
        return v == null ? null : new legacyconnector.v1_12_2.net.minecraft.util.math.Vec3d(v.x, v.y, v.z);
    }

    /** The name shown in the player list (text styles are not carried back from the modern component). */
    public legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent getTabListDisplayName() {
        var sp = mcc$serverPlayer();
        var name = sp == null ? null : sp.getTabListDisplayName();
        return name == null ? null : new legacyconnector.v1_12_2.net.minecraft.util.text.TextComponentString(name.getString());
    }

    /** Asks the client to download and use a resource pack. */
    public void loadResourcePack(String url, String hash) {
        var sp = mcc$serverPlayer();
        if (sp == null || sp.connection == null || url == null) return;
        sp.connection.send(new net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket(
                java.util.UUID.nameUUIDFromBytes(url.getBytes(java.nio.charset.StandardCharsets.UTF_8)), url, hash == null ? "" : hash, false,
                java.util.Optional.empty()));
    }

    /** Sends the open container's changes to the client (the per-tick sync of 1.12's player). */
    public void onUpdateEntity() {
        var sp = mcc$serverPlayer();
        if (sp != null) sp.containerMenu.broadcastChanges();
    }

    /** Sets the movement the player's client asked for (strafe, forward, jump, sneak). */
    public void setEntityActionState(float strafe, float forward, boolean jumping, boolean sneaking) {
        var sp = mcc$serverPlayer();
        if (sp != null) sp.setPlayerInput(strafe, forward, jumping, sneaking);
    }

    /** Registers the player's own inventory menu as the listener of the player's crafting inventory. */
    public void addSelfToInternalCraftingInventory() {
        var sp = mcc$serverPlayer();
        if (sp != null) sp.initInventoryMenu();
    }
}
