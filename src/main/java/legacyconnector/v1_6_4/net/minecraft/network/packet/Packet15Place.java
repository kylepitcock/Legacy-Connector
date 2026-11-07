package legacyconnector.v1_6_4.net.minecraft.network.packet;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import legacyconnector.v1_6_4.forward.Forward164;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.network.Packet;

/**
 * 1.6.4's block-placement packet, sent by the client: use the held item on a face of the block at the position (direction 0 down, 1 up, 2 north,
 * 3 south, 4 west, 5 east), or in the air when the direction is 255. Sent as the modern use-item-on / use-item packet.
 */
public class Packet15Place extends Packet {
    public int xPosition;
    public int yPosition;
    public int zPosition;
    public int direction;
    public ItemStack itemStack;
    public float xOffset;
    public float yOffset;
    public float zOffset;

    public Packet15Place() {}

    public Packet15Place(int x, int y, int z, int direction, ItemStack stack, float xOffset, float yOffset, float zOffset) {
        this.xPosition = x;
        this.yPosition = y;
        this.zPosition = z;
        this.direction = direction;
        this.itemStack = stack == null ? null : stack.copy();
        this.xOffset = xOffset;
        this.yOffset = yOffset;
        this.zOffset = zOffset;
    }

    public int getXPosition() {
        return xPosition;
    }

    public int getYPosition() {
        return yPosition;
    }

    public int getZPosition() {
        return zPosition;
    }

    public int getDirection() {
        return direction;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public float getXOffset() {
        return xOffset;
    }

    public float getYOffset() {
        return yOffset;
    }

    public float getZOffset() {
        return zOffset;
    }

    @Override
    public void readPacketData(DataInput in) throws IOException {
        xPosition = in.readInt();
        yPosition = in.readUnsignedByte();
        zPosition = in.readInt();
        direction = in.readUnsignedByte();
        itemStack = Forward164.readItemStack(in);
        xOffset = in.readUnsignedByte() / 16.0F;
        yOffset = in.readUnsignedByte() / 16.0F;
        zOffset = in.readUnsignedByte() / 16.0F;
    }

    @Override
    public void writePacketData(DataOutput out) throws IOException {
        out.writeInt(xPosition);
        out.write(yPosition);
        out.writeInt(zPosition);
        out.write(direction);
        Forward164.writeItemStack(itemStack, out);
        out.write((int) (xOffset * 16.0F));
        out.write((int) (yOffset * 16.0F));
        out.write((int) (zOffset * 16.0F));
    }

    @Override
    public int getPacketSize() {
        return 19;
    }

    @Override
    public void mcc$sendToServer() {
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null) return;
        if (direction == 255 || direction > 5) {
            mc.getConnection().send(new net.minecraft.network.protocol.game.ServerboundUseItemPacket(net.minecraft.world.InteractionHand.MAIN_HAND, 0,
                    mc.player.getYRot(), mc.player.getXRot()));
            return;
        }
        var pos = new net.minecraft.core.BlockPos(xPosition, yPosition, zPosition);
        var hit = new net.minecraft.world.phys.BlockHitResult(new net.minecraft.world.phys.Vec3(pos.getX() + xOffset, pos.getY() + yOffset, pos.getZ() + zOffset),
                net.minecraft.core.Direction.from3DDataValue(direction), pos, false);
        mc.getConnection().send(new net.minecraft.network.protocol.game.ServerboundUseItemOnPacket(net.minecraft.world.InteractionHand.MAIN_HAND, hit, 0));
    }
}
