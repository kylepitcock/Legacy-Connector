package legacyconnector.v1_12_2.net.minecraft.client.gui;

import legacyconnector.v1_12_2.net.minecraft.client.Minecraft;

/** 1.12.2's list of entry objects that draw themselves and take clicks (server list, key bindings, Forge's config entries). */
public abstract class GuiListExtended extends GuiSlot {
    public GuiListExtended(Minecraft mcIn, int widthIn, int heightIn, int topIn, int bottomIn, int slotHeightIn) {
        super(mcIn, widthIn, heightIn, topIn, bottomIn, slotHeightIn);
    }

    @Override
    protected void elementClicked(int slotIndex, boolean isDoubleClick, int mouseX, int mouseY) {
    }

    @Override
    protected boolean isSelected(int slotIndex) {
        return false;
    }

    @Override
    protected void drawBackground() {
    }

    @Override
    protected void drawSlot(int slotIndex, int xPos, int yPos, int heightIn, int mouseXIn, int mouseYIn, float partialTicks) {
        getListEntry(slotIndex).drawEntry(slotIndex, xPos, yPos, getListWidth(), heightIn, mouseXIn, mouseYIn,
                isMouseYWithinSlotBounds(mouseYIn) && getSlotIndexFromScreenCoords(mouseXIn, mouseYIn) == slotIndex, partialTicks);
    }

    @Override
    protected void updateItemPos(int entryID, int insideLeft, int yPos, float partialTicks) {
        getListEntry(entryID).updatePosition(entryID, insideLeft, yPos, partialTicks);
    }

    /** Passes a click to the entry under the mouse; true if it took it. */
    public boolean mouseClicked(int mouseX, int mouseY, int mouseEvent) {
        if (isMouseYWithinSlotBounds(mouseY)) {
            int i = getSlotIndexFromScreenCoords(mouseX, mouseY);
            if (i >= 0) {
                int j = left + width / 2 - getListWidth() / 2 + 2;
                int k = top + 4 - getAmountScrolled() + i * slotHeight + headerPadding;
                int l = mouseX - j;
                int i1 = mouseY - k;
                if (getListEntry(i).mousePressed(i, mouseX, mouseY, mouseEvent, l, i1)) {
                    setEnabled(false);
                    return true;
                }
            }
        }
        return false;
    }

    /** Every entry hears the release. */
    public boolean mouseReleased(int x, int y, int mouseEvent) {
        for (int i = 0; i < getSize(); ++i) {
            int j = left + width / 2 - getListWidth() / 2 + 2;
            int k = top + 4 - getAmountScrolled() + i * slotHeight + headerPadding;
            getListEntry(i).mouseReleased(i, x, y, mouseEvent, x - j, y - k);
        }
        setEnabled(true);
        return false;
    }

    public abstract IGuiListEntry getListEntry(int index);

    public interface IGuiListEntry {
        void updatePosition(int slotIndex, int x, int y, float partialTicks);

        void drawEntry(int slotIndex, int x, int y, int listWidth, int slotHeight, int mouseX, int mouseY, boolean isSelected, float partialTicks);

        boolean mousePressed(int slotIndex, int mouseX, int mouseY, int mouseEvent, int relativeX, int relativeY);

        void mouseReleased(int slotIndex, int x, int y, int mouseEvent, int relativeX, int relativeY);
    }
}
