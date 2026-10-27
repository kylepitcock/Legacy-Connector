package legacyconnector.v1_12_2.net.minecraft.network.play.server;

import java.io.IOException;
import legacyconnector.v1_12_2.net.minecraft.network.Packet;
import legacyconnector.v1_12_2.net.minecraft.network.PacketBuffer;
import legacyconnector.v1_12_2.net.minecraft.network.play.INetHandlerPlayClient;

/** What an entity holds or wears in one slot. */
public class SPacketEntityEquipment implements Packet<INetHandlerPlayClient> {
    private int entityID;
    private legacyconnector.v1_12_2.net.minecraft.inventory.EntityEquipmentSlot equipmentSlot;
    private legacyconnector.v1_12_2.net.minecraft.item.ItemStack itemStack = legacyconnector.v1_12_2.net.minecraft.item.ItemStack.EMPTY;

    public SPacketEntityEquipment() {
    }

    public SPacketEntityEquipment(int entityIDIn, legacyconnector.v1_12_2.net.minecraft.inventory.EntityEquipmentSlot equipmentSlotIn, legacyconnector.v1_12_2.net.minecraft.item.ItemStack itemStackIn) {
        this.entityID = entityIDIn;
        this.equipmentSlot = equipmentSlotIn;
        this.itemStack = itemStackIn.copy();
    }

    public legacyconnector.v1_12_2.net.minecraft.item.ItemStack getItemStack() {
        return itemStack;
    }

    public int getEntityID() {
        return entityID;
    }

    public legacyconnector.v1_12_2.net.minecraft.inventory.EntityEquipmentSlot getEquipmentSlot() {
        return equipmentSlot;
    }

    @Override
    public void readPacketData(PacketBuffer buf) throws IOException {
        entityID = buf.readVarInt();
        equipmentSlot = buf.readEnumValue(legacyconnector.v1_12_2.net.minecraft.inventory.EntityEquipmentSlot.class);
        itemStack = buf.readItemStack();
    }

    @Override
    public void writePacketData(PacketBuffer buf) throws IOException {
        buf.writeVarInt(entityID);
        buf.writeEnumValue(equipmentSlot);
        buf.writeItemStack(itemStack);
    }

    /** Handled by the modern client as the modern packet. */
    @Override
    public void processPacket(INetHandlerPlayClient handler) {
    }

    @Override
    public net.minecraft.network.protocol.Packet<?> mcc$modern() {
        if (equipmentSlot == null) return null;
        var slot = net.minecraft.world.entity.EquipmentSlot.byName(equipmentSlot.getName());
        return new net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket(entityID,
                java.util.List.of(com.mojang.datafixers.util.Pair.of(slot, org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122.toModern(itemStack))));
    }
}
