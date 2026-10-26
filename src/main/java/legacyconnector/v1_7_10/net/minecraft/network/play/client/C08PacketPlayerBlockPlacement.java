package legacyconnector.v1_7_10.net.minecraft.network.play.client;

import java.io.IOException;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.network.Packet;
import legacyconnector.v1_7_10.net.minecraft.network.PacketBuffer;

/**
 * A player right-clicked a block (or the air, direction 255) with an item. Mods build it to hand to their own network
 * handlers (Extra Utilities' alternative use packet reads one and has the server process it).
 */
public class C08PacketPlayerBlockPlacement extends Packet {
    public int placedBlockX;
    public int placedBlockY;
    public int placedBlockZ;
    public int placedBlockDirection;
    public ItemStack stack;
    public float facingX;
    public float facingY;
    public float facingZ;

    public C08PacketPlayerBlockPlacement() {
    }

    public C08PacketPlayerBlockPlacement(int x, int y, int z, int side, ItemStack stack, float hitX, float hitY, float hitZ) {
        this.placedBlockX = x;
        this.placedBlockY = y;
        this.placedBlockZ = z;
        this.placedBlockDirection = side;
        this.stack = stack != null ? stack.copy() : null;
        this.facingX = hitX;
        this.facingY = hitY;
        this.facingZ = hitZ;
    }

    @Override
    public void readPacketData(PacketBuffer data) throws IOException {
        placedBlockX = data.readInt();
        placedBlockY = data.readUnsignedByte();
        placedBlockZ = data.readInt();
        placedBlockDirection = data.readUnsignedByte();
        stack = data.readItemStackFromBuffer();
        facingX = data.readUnsignedByte() / 16.0F;
        facingY = data.readUnsignedByte() / 16.0F;
        facingZ = data.readUnsignedByte() / 16.0F;
    }

    @Override
    public void writePacketData(PacketBuffer data) throws IOException {
        data.writeInt(placedBlockX);
        data.writeByte(placedBlockY);
        data.writeInt(placedBlockZ);
        data.writeByte(placedBlockDirection);
        data.writeItemStackToBuffer(stack);
        data.writeByte((int) (facingX * 16.0F));
        data.writeByte((int) (facingY * 16.0F));
        data.writeByte((int) (facingZ * 16.0F));
    }

    public int getPlacedBlockX() {
        return placedBlockX;
    }

    public int getPlacedBlockY() {
        return placedBlockY;
    }

    public int getPlacedBlockZ() {
        return placedBlockZ;
    }

    public int getPlacedBlockDirection() {
        return placedBlockDirection;
    }

    public ItemStack getStack() {
        return stack;
    }

    public float getPlacedBlockOffsetX() {
        return facingX;
    }

    public float getPlacedBlockOffsetY() {
        return facingY;
    }

    public float getPlacedBlockOffsetZ() {
        return facingZ;
    }
}
