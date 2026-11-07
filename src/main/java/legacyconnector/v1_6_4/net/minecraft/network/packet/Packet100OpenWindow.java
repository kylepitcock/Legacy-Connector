package legacyconnector.v1_6_4.net.minecraft.network.packet;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import legacyconnector.v1_6_4.forward.Packets164;
import legacyconnector.v1_7_10.net.minecraft.network.Packet;
import net.minecraft.server.level.ServerPlayer;

/**
 * 1.6.4's open-window packet: tells the client to open the screen of a container whose slots the server already tracks under {@code windowId}.
 * {@code inventoryType} is 0 chest, 1 crafting table, 2 furnace, 3 dispenser, 4 enchanting table, 5 brewing stand, 6 villager, 7 beacon,
 * 8 anvil, 9 hopper. Delivered as the modern open-screen packet with the menu type of the same kind.
 */
public class Packet100OpenWindow extends Packet {
    public int windowId;
    public int inventoryType;
    public String windowTitle;
    public int slotsCount;
    public boolean useProvidedWindowTitle;

    public Packet100OpenWindow() {}

    public Packet100OpenWindow(int windowId, int inventoryType, String title, int slots, boolean useTitle) {
        this.windowId = windowId;
        this.inventoryType = inventoryType;
        this.windowTitle = title;
        this.slotsCount = slots;
        this.useProvidedWindowTitle = useTitle;
    }

    /** The registry key of the modern menu type for a legacy window type ({@code null} for an unknown type): chests are 9 wide with slots/9 rows. */
    public static String menuTypeKey(int inventoryType, int slots) {
        return switch (inventoryType) {
            case 0 -> "minecraft:generic_9x" + Math.max(1, Math.min(6, slots / 9));
            case 1 -> "minecraft:crafting";
            case 2 -> "minecraft:furnace";
            case 3 -> "minecraft:generic_3x3";
            case 4 -> "minecraft:enchantment";
            case 5 -> "minecraft:brewing_stand";
            case 6 -> "minecraft:merchant";
            case 7 -> "minecraft:beacon";
            case 8 -> "minecraft:anvil";
            case 9 -> "minecraft:hopper";
            default -> null;
        };
    }

    @Override
    public void readPacketData(DataInput in) throws IOException {
        windowId = in.readByte() & 255;
        inventoryType = in.readByte() & 255;
        windowTitle = Packets164.readString(in, 32767);
        slotsCount = in.readByte() & 255;
        useProvidedWindowTitle = in.readBoolean();
    }

    @Override
    public void writePacketData(DataOutput out) throws IOException {
        out.writeByte(windowId & 255);
        out.writeByte(inventoryType & 255);
        Packets164.writeString(windowTitle == null ? "" : windowTitle, out);
        out.writeByte(slotsCount & 255);
        out.writeBoolean(useProvidedWindowTitle);
    }

    @Override
    public int getPacketSize() {
        return 4 + (windowTitle == null ? 0 : windowTitle.length());
    }

    @Override
    public void mcc$send(ServerPlayer player) {
        String key = menuTypeKey(inventoryType, slotsCount);
        if (key == null) return;
        var type = net.minecraft.core.registries.BuiltInRegistries.MENU.get(net.minecraft.resources.ResourceLocation.parse(key));
        if (type == null) return;
        String title = windowTitle == null ? "" : windowTitle;
        net.minecraft.network.chat.Component text = useProvidedWindowTitle ? net.minecraft.network.chat.Component.literal(title)
                : net.minecraft.network.chat.Component.translatable(title);
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundOpenScreenPacket(windowId, type, text));
    }
}
