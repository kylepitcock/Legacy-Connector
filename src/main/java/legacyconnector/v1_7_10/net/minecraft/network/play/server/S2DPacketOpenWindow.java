package legacyconnector.v1_7_10.net.minecraft.network.play.server;

import java.io.IOException;
import legacyconnector.v1_7_10.net.minecraft.network.Packet;
import legacyconnector.v1_7_10.net.minecraft.network.PacketBuffer;

/**
 * Opens a container window on the client. Inventory types: 0 chest, 1 crafting table, 2 furnace, 3 dispenser, 4 enchanting table, 5 brewing stand,
 * 6 villager trade, 7 beacon, 8 anvil, 9 hopper, 10 dropper, 11 horse (needs an entity, not delivered).
 */
public class S2DPacketOpenWindow extends Packet {
    private int windowId;
    private int inventoryType;
    private String windowTitle = "";
    private int slotCount;
    private boolean useProvidedWindowTitle;
    private int entityId;

    public S2DPacketOpenWindow() {
    }

    public S2DPacketOpenWindow(int windowId, int inventoryType, String title, int slotCount, boolean useTitle) {
        this.windowId = windowId;
        this.inventoryType = inventoryType;
        this.windowTitle = title == null ? "" : title;
        this.slotCount = slotCount;
        this.useProvidedWindowTitle = useTitle;
    }

    public S2DPacketOpenWindow(int windowId, int inventoryType, String title, int slotCount, boolean useTitle, int horseId) {
        this(windowId, inventoryType, title, slotCount, useTitle);
        this.entityId = horseId;
    }

    @Override
    public void readPacketData(PacketBuffer data) throws IOException {
        windowId = data.readUnsignedByte();
        inventoryType = data.readUnsignedByte();
        windowTitle = data.readStringFromBuffer(32);
        slotCount = data.readUnsignedByte();
        useProvidedWindowTitle = data.readBoolean();
        if (inventoryType == 11) entityId = data.readInt();
    }

    @Override
    public void writePacketData(PacketBuffer data) throws IOException {
        data.writeByte(windowId);
        data.writeByte(inventoryType);
        data.writeStringToBuffer(windowTitle);
        data.writeByte(slotCount);
        data.writeBoolean(useProvidedWindowTitle);
        if (inventoryType == 11) data.writeInt(entityId);
    }

    public int func_148901_c() {
        return windowId;
    }

    public int func_148899_d() {
        return inventoryType;
    }

    public String func_148902_e() {
        return windowTitle;
    }

    public int func_148898_f() {
        return slotCount;
    }

    public boolean func_148900_g() {
        return useProvidedWindowTitle;
    }

    public int func_148897_h() {
        return entityId;
    }

    @Override
    public void mcc$send(net.minecraft.server.level.ServerPlayer player) {
        net.minecraft.world.inventory.MenuType<?> type = switch (inventoryType) {
            case 0 -> switch (Math.max(1, Math.min(6, (slotCount + 8) / 9))) {
                case 1 -> net.minecraft.world.inventory.MenuType.GENERIC_9x1;
                case 2 -> net.minecraft.world.inventory.MenuType.GENERIC_9x2;
                case 3 -> net.minecraft.world.inventory.MenuType.GENERIC_9x3;
                case 4 -> net.minecraft.world.inventory.MenuType.GENERIC_9x4;
                case 5 -> net.minecraft.world.inventory.MenuType.GENERIC_9x5;
                default -> net.minecraft.world.inventory.MenuType.GENERIC_9x6;
            };
            case 1 -> net.minecraft.world.inventory.MenuType.CRAFTING;
            case 2 -> net.minecraft.world.inventory.MenuType.FURNACE;
            case 3, 10 -> net.minecraft.world.inventory.MenuType.GENERIC_3x3;
            case 4 -> net.minecraft.world.inventory.MenuType.ENCHANTMENT;
            case 5 -> net.minecraft.world.inventory.MenuType.BREWING_STAND;
            case 6 -> net.minecraft.world.inventory.MenuType.MERCHANT;
            case 7 -> net.minecraft.world.inventory.MenuType.BEACON;
            case 8 -> net.minecraft.world.inventory.MenuType.ANVIL;
            case 9 -> net.minecraft.world.inventory.MenuType.HOPPER;
            default -> null;
        };
        if (type == null) return;
        net.minecraft.network.chat.Component title = useProvidedWindowTitle || windowTitle.isEmpty()
                ? net.minecraft.network.chat.Component.literal(windowTitle) : net.minecraft.network.chat.Component.translatable(windowTitle);
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundOpenScreenPacket(windowId, type, title));
    }
}
