package legacyconnector.v1_7_10.net.minecraft.network.play.server;

import legacyconnector.v1_7_10.net.minecraft.network.Packet;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** Tells the client what block stands at a position. */
public class S23PacketBlockChange extends Packet {
    private final net.minecraft.core.BlockPos pos;
    private final net.minecraft.world.level.block.state.BlockState state;

    public S23PacketBlockChange(int x, int y, int z, World world) {
        this.pos = new net.minecraft.core.BlockPos(x, y, z);
        var level = world.mcc$liveLevel();
        this.state = level == null ? net.minecraft.world.level.block.Blocks.AIR.defaultBlockState() : level.getBlockState(pos);
    }

    /** The x coordinate of the changed block. */
    public int func_148879_d() {
        return pos.getX();
    }

    /** The y coordinate of the changed block. */
    public int func_148878_e() {
        return pos.getY();
    }

    /** The z coordinate of the changed block. */
    public int func_148877_f() {
        return pos.getZ();
    }

    @Override
    public void mcc$send(net.minecraft.server.level.ServerPlayer player) {
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket(pos, state));
    }
}
