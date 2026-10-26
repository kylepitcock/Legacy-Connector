package legacyconnector.v1_7_10.net.minecraft.client.gui;

import legacyconnector.v1_7_10.net.minecraft.client.Minecraft;
import legacyconnector.v1_7_10.net.minecraft.client.renderer.Tessellator;

/** 1.7.10's list of entry objects that draw themselves and take clicks (server list, key bindings, Forge's config entries). */
public abstract class GuiListExtended extends GuiSlot {
    public GuiListExtended(Minecraft mc, int width, int height, int top, int bottom, int slotHeight) {
        super(mc, width, height, top, bottom, slotHeight);
    }

    @Override
    protected void elementClicked(int index, boolean doubleClick, int mouseX, int mouseY) {
    }

    @Override
    protected boolean isSelected(int index) {
        return false;
    }

    @Override
    protected void drawBackground() {
    }

    @Override
    protected void drawSlot(int index, int x, int y, int slotHeight, Tessellator tessellator, int mouseX, int mouseY) {
        getListEntry(index).drawEntry(index, x, y, getListWidth(), slotHeight, tessellator, mouseX, mouseY,
                getSlotIndexFromScreenCoords(mouseX, mouseY) == index);
    }

    /** mouseClicked: passes the click to the entry under the mouse; true if it took it. */
    public boolean func_148179_a(int mouseX, int mouseY, int button) {
        if (isMouseYWithinSlotBounds(mouseY)) {
            int index = getSlotIndexFromScreenCoords(mouseX, mouseY);
            if (index >= 0) {
                int x = left + width / 2 - getListWidth() / 2 + 2;
                int y = top + 4 - getAmountScrolled() + index * slotHeight + headerPadding;
                if (getListEntry(index).mousePressed(index, mouseX, mouseY, button, mouseX - x, mouseY - y)) {
                    setEnabled(false);
                    return true;
                }
            }
        }
        return false;
    }

    /** mouseReleased: every entry hears the release. */
    public boolean func_148181_b(int mouseX, int mouseY, int button) {
        for (int index = 0; index < getSize(); index++) {
            int x = left + width / 2 - getListWidth() / 2 + 2;
            int y = top + 4 - getAmountScrolled() + index * slotHeight + headerPadding;
            getListEntry(index).mouseReleased(index, mouseX, mouseY, button, mouseX - x, mouseY - y);
        }
        setEnabled(true);
        return false;
    }

    public abstract IGuiListEntry getListEntry(int index);

    public interface IGuiListEntry {
        void drawEntry(int index, int x, int y, int listWidth, int slotHeight, Tessellator tessellator, int mouseX, int mouseY, boolean isSelected);

        boolean mousePressed(int index, int mouseX, int mouseY, int button, int relativeX, int relativeY);

        void mouseReleased(int index, int mouseX, int mouseY, int button, int relativeX, int relativeY);
    }
}
