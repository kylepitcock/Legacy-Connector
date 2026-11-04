package legacyconnector.v1_7_10.net.minecraft.tileentity;

/** The tile entity of an ender chest: its lid state; the contents belong to the player. */
public class TileEntityEnderChest extends TileEntity {
    public float field_145972_a;
    public float field_145975_i;
    public int field_145973_j;
    private int field_145974_k;

    public void func_145969_a() {
        this.field_145973_j++;
    }

    public void func_145970_b() {
        this.field_145973_j--;
    }
}
