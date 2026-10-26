package legacyconnector.v1_7_10.net.minecraft.tileentity;

import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound;

/**
 * A sign: four lines of text (saved as {@code Text1}-{@code Text4}). Mods that read or write sign text, or extend the class for
 * their own signs, link against it. For a vanilla sign it is a view of the modern sign's front text: {@link #signText} is
 * refreshed when the view is handed out and written back on {@link #markDirty()} (and before the block changes).
 */
public class TileEntitySign extends TileEntity {
    public String[] signText = {"", "", "", ""};
    public int lineBeingEdited = -1;
    private boolean isEditable = true;
    /** The player allowed to edit the sign (the one who placed it). */
    private EntityPlayer field_145917_k;
    /** The modern sign this is a view of; null for mod signs. */
    private final net.minecraft.world.level.block.entity.SignBlockEntity mcc$sign;

    public TileEntitySign() {
        this.mcc$sign = null;
    }

    /** View of a vanilla sign. */
    public TileEntitySign(net.minecraft.world.level.block.entity.SignBlockEntity sign) {
        this.mcc$sign = sign;
        mcc$refresh();
    }

    /** The lines last read from the modern sign: only lines a mod changed since are written back. */
    private final String[] mcc$seen = {"", "", "", ""};

    /** Reads the modern sign's front lines into {@link #signText}. */
    public void mcc$refresh() {
        if (mcc$sign == null) return;
        var text = mcc$sign.getFrontText();
        for (int i = 0; i < 4; i++) signText[i] = mcc$seen[i] = text.getMessage(i, false).getString();
    }

    /** Writes the lines of {@link #signText} a mod changed back into the modern sign. */
    public void mcc$flush() {
        if (mcc$sign == null) return;
        var text = mcc$sign.getFrontText();
        boolean changed = false;
        for (int i = 0; i < 4; i++) {
            String line = signText[i] == null ? "" : signText[i];
            if (!line.equals(mcc$seen[i])) {
                mcc$seen[i] = line;
                text = text.setMessage(i, net.minecraft.network.chat.Component.literal(line));
                changed = true;
            }
        }
        if (changed) mcc$sign.setText(text, true);
    }

    @Override
    public void markDirty() {
        mcc$flush();
        super.markDirty();
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        for (int i = 0; i < 4; i++) tag.setString("Text" + (i + 1), signText[i] == null ? "" : signText[i]);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        isEditable = false;
        super.readFromNBT(tag);
        for (int i = 0; i < 4; i++) {
            signText[i] = tag.getString("Text" + (i + 1));
            if (signText[i].length() > 15) signText[i] = signText[i].substring(0, 15);
        }
    }

    public boolean getIsEditable() {
        return isEditable;
    }

    /** Making a sign editable again forgets its editing player. */
    public void setEditable(boolean editable) {
        this.isEditable = editable;
        if (!editable) field_145917_k = null;
    }

    public boolean func_145914_a() {
        return getIsEditable();
    }

    public void func_145913_a(boolean editable) {
        setEditable(editable);
    }

    /** Sets the player allowed to edit the sign (the modern sign's editor too). */
    public void func_145912_a(EntityPlayer player) {
        this.field_145917_k = player;
        if (mcc$sign != null) mcc$sign.setAllowedPlayerEditor(player == null || player.mcc$entity == null ? null : player.mcc$entity.getUUID());
    }

    /** The player allowed to edit the sign, or null. */
    public EntityPlayer func_145911_b() {
        return field_145917_k;
    }
}
