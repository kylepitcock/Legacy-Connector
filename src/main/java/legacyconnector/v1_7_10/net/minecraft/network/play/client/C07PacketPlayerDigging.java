package legacyconnector.v1_7_10.net.minecraft.network.play.client;

import legacyconnector.v1_7_10.net.minecraft.network.Packet;

/** A player started, aborted or finished digging a block (status 0, 1, 2) or dropped an item (3, 4, 5). */
public class C07PacketPlayerDigging extends Packet {
    private final int x;
    private final int y;
    private final int z;
    private final int side;
    private final int status;

    public C07PacketPlayerDigging(int status, int x, int y, int z, int side) {
        this.status = status;
        this.x = x;
        this.y = y;
        this.z = z;
        this.side = side;
    }

    public int func_149505_c() {
        return x;
    }

    public int func_149503_d() {
        return y;
    }

    public int func_149502_e() {
        return z;
    }

    public int func_149501_g() {
        return side;
    }

    public int func_149506_g() {
        return status;
    }
}
