package legacyconnector.v1_12_2.net.minecraft.tileentity;

/** 1.12.2's end portal tile entity (it only exists for its renderer). */
public class TileEntityEndPortal extends TileEntity {
    public TileEntityEndPortal() {
    }

    /** Whether a face is drawn: the end portal shows its top only. */
    public boolean shouldRenderFace(legacyconnector.v1_12_2.net.minecraft.util.EnumFacing facing) {
        return facing == legacyconnector.v1_12_2.net.minecraft.util.EnumFacing.UP;
    }
}
