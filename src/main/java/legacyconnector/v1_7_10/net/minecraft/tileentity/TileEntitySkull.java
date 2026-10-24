package legacyconnector.v1_7_10.net.minecraft.tileentity;

/** A mob head on the ground or a wall: type 0 skeleton .. 4 creeper, 3 player (with a profile name). */
public class TileEntitySkull extends TileEntity {
    private int skullType;
    private int skullRotation;
    private String ownerName = "";

    public void func_145905_a(int type, String owner) {
        this.skullType = type;
        this.ownerName = owner == null ? "" : owner;
    }

    public int getSkullType() {
        return skullType;
    }

    public int func_145906_b() {
        return skullRotation;
    }

    public void func_145903_a(int rotation) {
        this.skullRotation = rotation;
    }

    public String func_145907_c() {
        return ownerName;
    }
}
