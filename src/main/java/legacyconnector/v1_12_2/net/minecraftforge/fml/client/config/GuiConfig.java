package legacyconnector.v1_12_2.net.minecraftforge.fml.client.config;

import static legacyconnector.v1_12_2.net.minecraftforge.fml.client.config.GuiUtils.RESET_CHAR;
import static legacyconnector.v1_12_2.net.minecraftforge.fml.client.config.GuiUtils.UNDO_CHAR;

import java.util.ArrayList;
import java.util.List;
import legacyconnector.v1_12_2.net.minecraftforge.fml.client.event.ConfigChangedEvent;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.FMLCommonHandler;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.Loader;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event;
import legacyconnector.v1_12_2.net.minecraft.client.Minecraft;
import legacyconnector.v1_12_2.net.minecraft.client.gui.GuiButton;
import legacyconnector.v1_12_2.net.minecraft.client.gui.GuiScreen;
import legacyconnector.v1_12_2.net.minecraft.client.resources.I18n;
import legacyconnector.v1_12_2.net.minecraft.util.text.TextComponentString;
import org.slf4j.LoggerFactory;

/**
 * Forge 1.7.10's config screen: the elements as an editable list ({@link GuiConfigEntries}) with Done, Undo All, Reset All and
 * "apply globally". Done writes the changes to the elements and posts {@link ConfigChangedEvent.OnConfigChangedEvent} on the FML bus
 * (the mod saves its Configuration there), then {@link ConfigChangedEvent.PostConfigChangedEvent}.
 */
public class GuiConfig extends GuiScreen {
    public final GuiScreen parentScreen;
    public String title = "Config GUI";
    public String titleLine2;
    public final List<IConfigElement> configElements;
    public final List<GuiConfigEntries.IConfigEntry> initEntries;
    public GuiConfigEntries entryList;
    protected GuiButtonExt btnDefaultAll;
    protected GuiButtonExt btnUndoAll;
    protected GuiCheckBox chkApplyGlobally;
    public final String modID;
    /** When set, this screen is a whole config file of its own and saves on Done even as a child screen. */
    public final String configID;
    public final boolean isWorldRunning;
    public final boolean allRequireWorldRestart;
    public final boolean allRequireMcRestart;
    public boolean needsRefresh = true;
    protected HoverChecker undoHoverChecker;
    protected HoverChecker resetHoverChecker;
    protected HoverChecker checkBoxHoverChecker;

    public GuiConfig(GuiScreen parentScreen, List<IConfigElement> configElements, String modID, String configID, boolean allRequireWorldRestart,
            boolean allRequireMcRestart, String title) {
        this(parentScreen, configElements, modID, configID, allRequireWorldRestart, allRequireMcRestart, title, null);
    }

    public GuiConfig(GuiScreen parentScreen, List<IConfigElement> configElements, String modID, boolean allRequireWorldRestart,
            boolean allRequireMcRestart, String title) {
        this(parentScreen, configElements, modID, null, allRequireWorldRestart, allRequireMcRestart, title, null);
    }

    public GuiConfig(GuiScreen parentScreen, List<IConfigElement> configElements, String modID, boolean allRequireWorldRestart,
            boolean allRequireMcRestart, String title, String titleLine2) {
        this(parentScreen, configElements, modID, null, allRequireWorldRestart, allRequireMcRestart, title, titleLine2);
    }

    public GuiConfig(GuiScreen parentScreen, List<IConfigElement> configElements, String modID, String configID, boolean allRequireWorldRestart,
            boolean allRequireMcRestart, String title, String titleLine2) {
        this.mc = Minecraft.getMinecraft();
        this.fontRenderer = mc.fontRenderer;
        this.parentScreen = parentScreen;
        this.configElements = configElements == null ? new ArrayList<>() : configElements;
        this.allRequireWorldRestart = allRequireWorldRestart;
        this.allRequireMcRestart = allRequireMcRestart;
        this.modID = modID;
        this.configID = configID;
        this.isWorldRunning = Minecraft.mcc$get$world(mc) != null;
        if (title != null) this.title = title;
        this.titleLine2 = titleLine2;
        if (this.titleLine2 != null && this.titleLine2.startsWith(" > ")) this.titleLine2 = this.titleLine2.replaceFirst(" > ", "");
        this.entryList = new GuiConfigEntries(this, mc);
        this.initEntries = new ArrayList<>(entryList.listEntries);
    }

    /** The screen of a mod's {@code @Config} classes (what {@code DefaultGuiFactory} opens). */
    public GuiConfig(GuiScreen parentScreen, String modID, String title) {
        this(parentScreen, collectConfigElements(modID), modID, false, false, title);
    }

    public GuiConfig(GuiScreen parentScreen, String modID, boolean allRequireWorldRestart, boolean allRequireMcRestart, String title, Class<?>... configClasses) {
        this(parentScreen, collectConfigElements(modID), modID, allRequireWorldRestart, allRequireMcRestart, title);
    }

    /** The elements of the mod's config categories: one category shows its entries directly, several show as category buttons. */
    private static List<IConfigElement> collectConfigElements(String modID) {
        var categories = legacyconnector.v1_12_2.net.minecraftforge.common.config.ConfigManager.mcc$categories(modID);
        List<IConfigElement> out = new ArrayList<>();
        if (categories.size() == 1) {
            out.addAll(new legacyconnector.v1_12_2.net.minecraftforge.common.config.ConfigElement(categories.get(0)).getChildElements());
        } else {
            for (var c : categories) out.add(new legacyconnector.v1_12_2.net.minecraftforge.common.config.ConfigElement(c));
        }
        return out;
    }

    /** A config file's path as the user sees it, from the game folder. */
    public static String getAbridgedConfigPath(String path) {
        if (path == null) return "";
        Minecraft mc = Minecraft.getMinecraft();
        String dir = mc.gameDir.getAbsolutePath().replace("\\", "/");
        String p = path.replace("\\", "/");
        if (dir.endsWith(".")) return p.replace(dir.substring(0, dir.length() - 1), "/.minecraft/");
        return p.replace(dir, "/.minecraft");
    }

    @Override
    public void initGui() {
        if (entryList == null || needsRefresh) {
            entryList = new GuiConfigEntries(this, mc);
            needsRefresh = false;
        }
        int undoGlyphWidth = mc.fontRenderer.getStringWidth(UNDO_CHAR) * 2;
        int resetGlyphWidth = mc.fontRenderer.getStringWidth(RESET_CHAR) * 2;
        int doneWidth = Math.max(mc.fontRenderer.getStringWidth(I18n.format("gui.done")) + 20, 100);
        int undoWidth = mc.fontRenderer.getStringWidth(" " + I18n.format("fml.configgui.tooltip.undoChanges")) + undoGlyphWidth + 20;
        int resetWidth = mc.fontRenderer.getStringWidth(" " + I18n.format("fml.configgui.tooltip.resetToDefault")) + resetGlyphWidth + 20;
        int checkWidth = mc.fontRenderer.getStringWidth(I18n.format("fml.configgui.applyGlobally")) + 13;
        int buttonWidthHalf = (doneWidth + 5 + undoWidth + 5 + resetWidth + 5 + checkWidth) / 2;
        buttonList.add(new GuiButtonExt(2000, width / 2 - buttonWidthHalf, height - 29, doneWidth, 20, I18n.format("gui.done")));
        buttonList.add(btnDefaultAll = new GuiUnicodeGlyphButton(2001, width / 2 - buttonWidthHalf + doneWidth + 5 + undoWidth + 5, height - 29, resetWidth,
                20, " " + I18n.format("fml.configgui.tooltip.resetToDefault"), RESET_CHAR, 2.0F));
        buttonList.add(btnUndoAll = new GuiUnicodeGlyphButton(2002, width / 2 - buttonWidthHalf + doneWidth + 5, height - 29, undoWidth, 20,
                " " + I18n.format("fml.configgui.tooltip.undoChanges"), UNDO_CHAR, 2.0F));
        buttonList.add(chkApplyGlobally = new GuiCheckBox(2003, width / 2 - buttonWidthHalf + doneWidth + 5 + undoWidth + 5 + resetWidth + 5, height - 24,
                I18n.format("fml.configgui.applyGlobally"), false));
        undoHoverChecker = new HoverChecker(btnUndoAll, 800);
        resetHoverChecker = new HoverChecker(btnDefaultAll, 800);
        checkBoxHoverChecker = new HoverChecker(chkApplyGlobally, 800);
        entryList.initGui();
    }

    @Override
    public void onGuiClosed() {
        entryList.onGuiClosed();
        if (configID != null && parentScreen instanceof GuiConfig parent) {
            parent.needsRefresh = true;
            parent.initGui();
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 2000) {
            boolean close = true;
            try {
                if ((configID != null || !(parentScreen instanceof GuiConfig)) && entryList.hasChangedEntry(true)) {
                    boolean requiresMcRestart = entryList.saveConfigElements();
                    if (Loader.isModLoaded(modID)) {
                        ConfigChangedEvent event = new ConfigChangedEvent.OnConfigChangedEvent(modID, configID, isWorldRunning, requiresMcRestart);
                        FMLCommonHandler.instance().bus().post(event);
                        if (event.getResult() != Event.Result.DENY) {
                            FMLCommonHandler.instance().bus().post(new ConfigChangedEvent.PostConfigChangedEvent(modID, configID, isWorldRunning, requiresMcRestart));
                        }
                        if (requiresMcRestart) {
                            close = false;
                            mc.displayGuiScreen(new GuiMessageDialog(parentScreen, "fml.configgui.gameRestartTitle",
                                    new TextComponentString(I18n.format("fml.configgui.gameRestartRequired")), "fml.configgui.confirmRestartMessage"));
                        }
                        if (parentScreen instanceof GuiConfig parent) parent.needsRefresh = true;
                    }
                }
            } catch (Throwable e) {
                LoggerFactory.getLogger("LegacyConnector/Gui").error("Could not save the config of {}", modID, e);
            }
            if (close) mc.displayGuiScreen(parentScreen);
        } else if (button.id == 2001) {
            entryList.setAllToDefault(chkApplyGlobally.isChecked());
        } else if (button.id == 2002) {
            entryList.undoAllChanges(chkApplyGlobally.isChecked());
        }
    }

    /** The list scrolls and drags with the mouse events (1.12 forwarded them to it). */
    @Override
    public void handleMouseInput() throws java.io.IOException {
        super.handleMouseInput();
        entryList.handleMouseInput();
    }

    @Override
    protected void mouseClicked(int x, int y, int mouseEvent) throws java.io.IOException {
        if (mouseEvent != 0 || !entryList.mouseClicked(x, y, mouseEvent)) {
            entryList.mouseClickedPassThru(x, y, mouseEvent);
            super.mouseClicked(x, y, mouseEvent);
        }
    }

    @Override
    protected void mouseReleased(int x, int y, int mouseEvent) {
        if (mouseEvent != 0 || !entryList.mouseReleased(x, y, mouseEvent)) super.mouseReleased(x, y, mouseEvent);
    }

    @Override
    protected void keyTyped(char eventChar, int eventKey) {
        if (eventKey == 1) mc.displayGuiScreen(parentScreen);
        else entryList.keyTyped(eventChar, eventKey);
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        entryList.updateScreen();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        entryList.drawScreen(mouseX, mouseY, partialTicks);
        drawCenteredString(fontRenderer, title, width / 2, 8, 16777215);
        String title2 = titleLine2;
        if (title2 != null) {
            int strWidth = mc.fontRenderer.getStringWidth(title2);
            int ellipsisWidth = mc.fontRenderer.getStringWidth("...");
            if (strWidth > width - 6 && strWidth > ellipsisWidth) title2 = mc.fontRenderer.trimStringToWidth(title2, width - 6 - ellipsisWidth).trim() + "...";
            drawCenteredString(fontRenderer, title2, width / 2, 18, 16777215);
        }
        boolean global = chkApplyGlobally.isChecked();
        btnUndoAll.enabled = entryList.areAnyEntriesEnabled(global) && entryList.hasChangedEntry(global);
        btnDefaultAll.enabled = entryList.areAnyEntriesEnabled(global) && !entryList.areAllEntriesDefault(global);
        super.drawScreen(mouseX, mouseY, partialTicks);
        entryList.drawScreenPost(mouseX, mouseY, partialTicks);
        if (undoHoverChecker.checkHover(mouseX, mouseY)) drawToolTip(mc.fontRenderer.listFormattedStringToWidth(I18n.format("fml.configgui.tooltip.undoAll"), 300), mouseX, mouseY);
        if (resetHoverChecker.checkHover(mouseX, mouseY)) drawToolTip(mc.fontRenderer.listFormattedStringToWidth(I18n.format("fml.configgui.tooltip.resetAll"), 300), mouseX, mouseY);
        if (checkBoxHoverChecker.checkHover(mouseX, mouseY)) {
            drawToolTip(mc.fontRenderer.listFormattedStringToWidth(I18n.format("fml.configgui.tooltip.applyGlobally"), 300), mouseX, mouseY);
        }
    }

    public void drawToolTip(List stringList, int x, int y) {
        drawHoveringText(stringList, x, y);
    }
}
