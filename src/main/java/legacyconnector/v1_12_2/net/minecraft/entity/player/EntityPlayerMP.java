package legacyconnector.v1_12_2.net.minecraft.entity.player;

import legacyconnector.v1_12_2.net.minecraft.world.World;

public class EntityPlayerMP extends EntityPlayer implements legacyconnector.v1_12_2.net.minecraft.inventory.IContainerListener {
    /** Phantom field: accesses are redirected to {@code mcc$get$connection}. */
    @Deprecated
    public legacyconnector.v1_12_2.net.minecraft.network.NetHandlerPlayServer connection;
    private legacyconnector.v1_12_2.net.minecraft.network.NetHandlerPlayServer mcc$connection;

    public EntityPlayerMP(World world) {
        super(world);
    }

    public static legacyconnector.v1_12_2.net.minecraft.network.NetHandlerPlayServer mcc$get$connection(EntityPlayerMP p) {
        if (p.mcc$serverPlayer() == null) return null;
        if (p.mcc$connection == null) p.mcc$connection = new legacyconnector.v1_12_2.net.minecraft.network.NetHandlerPlayServer(p);
        return p.mcc$connection;
    }

    public int getPing() {
        var player = mcc$serverPlayer();
        return player == null || player.connection == null ? 0 : player.connection.latency();
    }

    public void setGameType(legacyconnector.v1_12_2.net.minecraft.world.GameType type) {
        var player = mcc$serverPlayer();
        if (player != null && type != null) player.setGameMode(type.mcc$modern());
    }

    /** Moves the player to another dimension through the modern teleport (no portal is placed, as Forge's teleporter-less call). */
    public void changeDimension(int dimension) {
        var player = mcc$serverPlayer();
        if (player == null) return;
        var level = org.kylepitcock.legacyconnector.v1_12_2.bridge.WorldBridge1122.levelOf(player.server, dimension);
        if (level != null) player.teleportTo(level, player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
    }

    public net.minecraft.server.level.ServerPlayer mcc$serverPlayer() {
        return (net.minecraft.server.level.ServerPlayer) mcc$entity;
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

    // ------------------------------------------------------------------ IContainerListener (1.12.2 players listen to their open container)

    /** Slots of a container hosted by a modern menu are synced by the menu; otherwise each slot goes out as a set-slot packet. */
    @Override
    public void sendAllContents(legacyconnector.v1_12_2.net.minecraft.inventory.Container container,
            legacyconnector.v1_12_2.net.minecraft.util.NonNullList<legacyconnector.v1_12_2.net.minecraft.item.ItemStack> items) {
        if (container.mcc$menu != null) return;
        for (int i = 0; i < items.size(); i++) sendSlotContents(container, i, items.get(i));
    }

    @Override
    public void sendSlotContents(legacyconnector.v1_12_2.net.minecraft.inventory.Container container, int slot,
            legacyconnector.v1_12_2.net.minecraft.item.ItemStack stack) {
        var player = mcc$serverPlayer();
        if (player == null || container.mcc$menu != null) return;
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket(container.windowId, 0, slot,
                org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122.toModern(stack)));
    }

    @Override
    public void sendWindowProperty(legacyconnector.v1_12_2.net.minecraft.inventory.Container container, int id, int value) {
        var player = mcc$serverPlayer();
        if (player != null) org.kylepitcock.legacyconnector.v1_12_2.gui.LegacyMenu1122.sendProperty(player, container, id, value);
    }

    @Override
    public void sendAllWindowProperties(legacyconnector.v1_12_2.net.minecraft.inventory.Container container,
            legacyconnector.v1_12_2.net.minecraft.inventory.IInventory inventory) {
        for (int i = 0; i < inventory.getFieldCount(); ++i) sendWindowProperty(container, i, inventory.getField(i));
    }
}
