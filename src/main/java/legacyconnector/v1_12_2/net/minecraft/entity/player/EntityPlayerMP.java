package legacyconnector.v1_12_2.net.minecraft.entity.player;

import legacyconnector.v1_12_2.net.minecraft.world.World;

public class EntityPlayerMP extends EntityPlayer implements legacyconnector.v1_12_2.net.minecraft.inventory.IContainerListener {
    public EntityPlayerMP(World world) {
        super(world);
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
}
