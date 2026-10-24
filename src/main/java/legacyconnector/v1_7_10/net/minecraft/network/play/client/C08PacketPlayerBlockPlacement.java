package legacyconnector.v1_7_10.net.minecraft.network.play.client;

import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.network.Packet;

/** A player right-clicked a block (or the air) with an item; mods build it to hand to their own network handlers. */
public class C08PacketPlayerBlockPlacement extends Packet {
    private final int x;
    private final int y;
    private final int z;
    private final int side;
    private final ItemStack stack;
    private final float hitX;
    private final float hitY;
    private final float hitZ;

    public C08PacketPlayerBlockPlacement(int x, int y, int z, int side, ItemStack stack, float hitX, float hitY, float hitZ) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.side = side;
        this.stack = stack;
        this.hitX = hitX;
        this.hitY = hitY;
        this.hitZ = hitZ;
    }

    public int func_149576_c() {
        return x;
    }

    public int func_149571_d() {
        return y;
    }

    public int func_149570_e() {
        return z;
    }

    public int func_149568_f() {
        return side;
    }

    public ItemStack func_149574_g() {
        return stack;
    }

    public float func_149573_h() {
        return hitX;
    }

    public float func_149569_i() {
        return hitY;
    }

    public float func_149575_j() {
        return hitZ;
    }
}
