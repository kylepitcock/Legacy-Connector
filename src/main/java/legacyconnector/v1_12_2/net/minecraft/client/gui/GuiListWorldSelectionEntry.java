package legacyconnector.v1_12_2.net.minecraft.client.gui;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import legacyconnector.v1_12_2.net.minecraft.client.Minecraft;
import legacyconnector.v1_12_2.net.minecraft.client.resources.I18n;
import legacyconnector.v1_12_2.net.minecraft.world.storage.ISaveFormat;
import legacyconnector.v1_12_2.net.minecraft.world.storage.WorldSummary;

/**
 * One world of 1.12.2's world list: draws its name, folder and last played date; a double click (or {@link #joinWorld}) opens it.
 * Editing and re-creating open the game's own screens for the world.
 */
public class GuiListWorldSelectionEntry implements GuiListExtended.IGuiListEntry {
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat();
    private final Minecraft client;
    private final GuiWorldSelection worldSelScreen;
    private final WorldSummary worldSummary;
    private final GuiListWorldSelection containingListSel;
    private final ISaveFormat saveFormat;
    private long lastClickTime;

    public GuiListWorldSelectionEntry(GuiListWorldSelection listWorldSelIn, WorldSummary worldSummaryIn, ISaveFormat saveFormat) {
        this.containingListSel = listWorldSelIn;
        this.worldSelScreen = listWorldSelIn == null ? null : listWorldSelIn.getGuiWorldSelection();
        this.worldSummary = worldSummaryIn;
        this.saveFormat = saveFormat;
        this.client = Minecraft.getMinecraft();
    }

    public WorldSummary mcc$summary() {
        return worldSummary;
    }

    @Override
    public void drawEntry(int slotIndex, int x, int y, int listWidth, int slotHeight, int mouseX, int mouseY, boolean isSelected, float partialTicks) {
        String name = worldSummary.getDisplayName();
        String folder = worldSummary.getFileName() + " (" + DATE_FORMAT.format(new Date(worldSummary.getLastTimePlayed())) + ")";
        if (name == null || name.isEmpty()) name = I18n.format("selectWorld.world") + " " + (slotIndex + 1);
        String info = worldSummary.isHardcoreModeEnabled() ? I18n.format("gameMode.hardcore")
                : worldSummary.getCheatsEnabled() ? I18n.format("selectWorld.cheats") : "";
        FontRenderer font = client.fontRenderer != null ? client.fontRenderer : FontRenderer.mcc$INSTANCE;
        if (font == null) return;
        font.drawString(name, x + 32 + 3, y + 1, 16777215);
        font.drawString(folder, x + 32 + 3, y + font.FONT_HEIGHT + 3, 8421504);
        font.drawString(info, x + 32 + 3, y + font.FONT_HEIGHT + font.FONT_HEIGHT + 3, 8421504);
    }

    @Override
    public boolean mousePressed(int slotIndex, int mouseX, int mouseY, int mouseEvent, int relativeX, int relativeY) {
        if (containingListSel != null) containingListSel.selectWorld(slotIndex);
        if (relativeX < 32) {
            joinWorld();
            return true;
        }
        if (Minecraft.getSystemTime() - lastClickTime < 250L) {
            joinWorld();
            return true;
        }
        lastClickTime = Minecraft.getSystemTime();
        return false;
    }

    /** Opens the world (the game shows its loading screens). */
    public void joinWorld() {
        client.launchIntegratedServer(worldSummary.getFileName(), worldSummary.getDisplayName(), null);
    }

    /** Asks before deleting the world folder, then refreshes the list. */
    public void deleteWorld() {
        GuiScreen back = worldSelScreen;
        client.displayGuiScreen(new GuiYesNo((result, id) -> {
            if (result) {
                try {
                    saveFormat.flushCache();
                    saveFormat.deleteWorldDirectory(worldSummary.getFileName());
                } catch (RuntimeException e) {
                    // the world stays
                }
                if (containingListSel != null) containingListSel.refreshList();
            }
            client.displayGuiScreen(back);
        }, I18n.format("selectWorld.deleteQuestion"), "'" + worldSummary.getDisplayName() + "' " + I18n.format("selectWorld.deleteWarning"),
                I18n.format("selectWorld.deleteButton"), I18n.format("gui.cancel"), 0));
    }

    /** The game's edit world screen for this world. */
    public void editWorld() {
        if (!net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) return;
        Modern.edit(worldSummary.getFileName());
    }

    /** The game's world list (re-creating a world starts from there). */
    public void recreateWorld() {
        client.displayGuiScreen(new GuiWorldSelection(worldSelScreen));
    }

    @Override
    public void mouseReleased(int slotIndex, int x, int y, int mouseEvent, int relativeX, int relativeY) {
    }

    @Override
    public void updatePosition(int slotIndex, int x, int y, float partialTicks) {
    }

    /** The modern edit screen, kept apart so this class verifies without the client classes. */
    private static final class Modern {
        static void edit(String folder) {
            var mc = net.minecraft.client.Minecraft.getInstance();
            var previous = mc.screen;
            try {
                var access = mc.getLevelSource().validateAndCreateAccess(folder);
                mc.setScreen(net.minecraft.client.gui.screens.worldselection.EditWorldScreen.create(mc, access, changed -> {
                    access.safeClose();
                    mc.setScreen(previous);
                }));
            } catch (Exception e) {
                org.slf4j.LoggerFactory.getLogger("LegacyConnector/Gui").warn("Could not edit world {}: {}", folder, e.toString());
            }
        }
    }
}
