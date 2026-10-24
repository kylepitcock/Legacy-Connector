package legacyconnector.v1_7_10.net.minecraft.tileentity;

/** A sign: four lines of text. Mods that read or write sign text, or extend the class for their own signs, link against it. */
public class TileEntitySign extends TileEntity {
    public String[] signText = {"", "", "", ""};
    public int lineBeingEdited = -1;
    private boolean isEditable = true;

    public boolean func_145914_a() {
        return isEditable;
    }

    public void func_145913_a(boolean editable) {
        this.isEditable = editable;
    }
}
