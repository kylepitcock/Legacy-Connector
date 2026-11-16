package legacyconnector.v1_7_10.net.minecraft.network.play.server;

import java.io.IOException;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.network.Packet;
import legacyconnector.v1_7_10.net.minecraft.network.PacketBuffer;

/** What an entity holds or wears: slot 0 held item, 1 boots, 2 leggings, 3 chestplate, 4 helmet. */
public class S04PacketEntityEquipment extends Packet {
    private int entityId;
    private int slot;
    private ItemStack stack;

    public S04PacketEntityEquipment() {
    }

    public S04PacketEntityEquipment(int entityId, int slot, ItemStack stack) {
        this.entityId = entityId;
        this.slot = slot;
        this.stack = stack == null ? null : stack.copy();
    }

    @Override
    public void readPacketData(PacketBuffer data) throws IOException {
        entityId = data.readInt();
        slot = data.readShort();
        stack = data.readItemStackFromBuffer();
    }

    @Override
    public void writePacketData(PacketBuffer data) throws IOException {
        data.writeInt(entityId);
        data.writeShort(slot);
        data.writeItemStackToBuffer(stack);
    }

    public ItemStack func_149390_c() {
        return stack;
    }

    public int func_149389_d() {
        return entityId;
    }

    public int func_149388_e() {
        return slot;
    }

    @Override
    public void mcc$send(net.minecraft.server.level.ServerPlayer player) {
        net.minecraft.world.entity.EquipmentSlot target = switch (slot) {
            case 0 -> net.minecraft.world.entity.EquipmentSlot.MAINHAND;
            case 1 -> net.minecraft.world.entity.EquipmentSlot.FEET;
            case 2 -> net.minecraft.world.entity.EquipmentSlot.LEGS;
            case 3 -> net.minecraft.world.entity.EquipmentSlot.CHEST;
            case 4 -> net.minecraft.world.entity.EquipmentSlot.HEAD;
            default -> null;
        };
        if (target == null) return;
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket(entityId, java.util.List.of(
                com.mojang.datafixers.util.Pair.of(target, stack == null ? net.minecraft.world.item.ItemStack.EMPTY : stack.mcc$modern()))));
    }
}
