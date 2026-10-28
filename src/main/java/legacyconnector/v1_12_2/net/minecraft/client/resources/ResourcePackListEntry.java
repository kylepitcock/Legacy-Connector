package legacyconnector.v1_12_2.net.minecraft.client.resources;

/** A pack in the resource packs screen's lists: its name and description drawn with the font, clicked to move between the lists. */
public abstract class ResourcePackListEntry implements legacyconnector.v1_12_2.net.minecraft.client.gui.GuiListExtended.IGuiListEntry {
    protected final legacyconnector.v1_12_2.net.minecraft.client.Minecraft mc;
    protected final legacyconnector.v1_12_2.net.minecraft.client.gui.GuiScreenResourcePacks resourcePacksGUI;

    public ResourcePackListEntry(legacyconnector.v1_12_2.net.minecraft.client.gui.GuiScreenResourcePacks gui) {
        this.resourcePacksGUI = gui;
        this.mc = legacyconnector.v1_12_2.net.minecraft.client.Minecraft.getMinecraft();
    }

    protected abstract String getResourcePackName();

    protected abstract String getResourcePackDescription();

    /** Whether this is the server's pack (never: the game handles server packs). */
    public boolean isServerPack() {
        return false;
    }

    @Override
    public void drawEntry(int slotIndex, int x, int y, int listWidth, int slotHeight, int mouseX, int mouseY, boolean isSelected, float partialTicks) {
        var font = mc.fontRenderer;
        if (font == null) return;
        font.drawStringWithShadow(getResourcePackName(), x + 32 + 2, y + 1, 0xFFFFFF);
        String description = getResourcePackDescription();
        if (description != null) font.drawStringWithShadow(description, x + 32 + 2, y + 12, 0x808080);
    }

    @Override
    public void updatePosition(int slotIndex, int x, int y, float partialTicks) {
    }

    @Override
    public boolean mousePressed(int slotIndex, int mouseX, int mouseY, int mouseEvent, int relativeX, int relativeY) {
        return false;
    }

    @Override
    public void mouseReleased(int slotIndex, int x, int y, int mouseEvent, int relativeX, int relativeY) {
    }
}
