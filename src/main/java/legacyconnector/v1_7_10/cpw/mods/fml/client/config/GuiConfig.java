package legacyconnector.v1_7_10.cpw.mods.fml.client.config;

import java.util.List;
import legacyconnector.v1_7_10.net.minecraft.client.gui.GuiButton;
import legacyconnector.v1_7_10.net.minecraft.client.gui.GuiScreen;
import legacyconnector.v1_7_10.net.minecraft.util.StatCollector;

/**
 * A plain configuration screen: one button per element, paged. Booleans toggle, categories open a sub-screen, other values
 * are listed (editing them is done in the config file). Forge's own screen had text fields; mods subclass this one only to
 * choose their elements and title.
 */
public class GuiConfig extends GuiScreen {
    private static final int DONE = 2000;
    private static final int PREVIOUS = 2001;
    private static final int NEXT = 2002;

    public final GuiScreen parentScreen;
    public List<IConfigElement> configElements;
    public final String modID;
    public final String configID;
    public String title = "Config GUI";
    public String titleLine2;
    public final boolean allRequireWorldRestart;
    public final boolean allRequireMcRestart;
    private int page;

    public GuiConfig(GuiScreen parentScreen, List<IConfigElement> configElements, String modID, boolean allRequireWorldRestart,
            boolean allRequireMcRestart, String title) {
        this(parentScreen, configElements, modID, null, allRequireWorldRestart, allRequireMcRestart, title, null);
    }

    public GuiConfig(GuiScreen parentScreen, List<IConfigElement> configElements, String modID, boolean allRequireWorldRestart,
            boolean allRequireMcRestart, String title, String titleLine2) {
        this(parentScreen, configElements, modID, null, allRequireWorldRestart, allRequireMcRestart, title, titleLine2);
    }

    public GuiConfig(GuiScreen parentScreen, List<IConfigElement> configElements, String modID, String configID,
            boolean allRequireWorldRestart, boolean allRequireMcRestart, String title) {
        this(parentScreen, configElements, modID, configID, allRequireWorldRestart, allRequireMcRestart, title, null);
    }

    public GuiConfig(GuiScreen parentScreen, List<IConfigElement> configElements, String modID, String configID,
            boolean allRequireWorldRestart, boolean allRequireMcRestart, String title, String titleLine2) {
        this.parentScreen = parentScreen;
        this.configElements = configElements;
        this.modID = modID;
        this.configID = configID;
        this.allRequireWorldRestart = allRequireWorldRestart;
        this.allRequireMcRestart = allRequireMcRestart;
        if (title != null) this.title = title;
        this.titleLine2 = titleLine2;
    }

    public GuiConfig(GuiScreen parentScreen, String modID, String title) {
        this(parentScreen, new java.util.ArrayList<>(), modID, false, false, title);
    }

    public static String getAbridgedConfigPath(String path) {
        if (path == null) return "";
        String normalized = path.replace('\\', '/');
        int config = normalized.lastIndexOf("/config/");
        return config >= 0 ? normalized.substring(config + 1) : normalized;
    }

    private int rowsPerPage() {
        return Math.max(1, (height - 80) / 24);
    }

    private static String label(IConfigElement e) {
        String key = e.getLanguageKey();
        String name = key != null && !key.isEmpty() ? StatCollector.translateToLocal(key) : e.getName();
        if (name == null || name.equals(key)) name = e.getName();
        if (e.getType() == GuiConfigEntries.ConfigGuiType.CONFIG_CATEGORY) return name + " ...";
        return name + ": " + (e.isList() ? java.util.Arrays.toString(e.getList()) : String.valueOf(e.get()));
    }

    @Override
    public void initGui() {
        buttonList.clear();
        int rows = rowsPerPage();
        int total = configElements == null ? 0 : configElements.size();
        page = Math.max(0, Math.min(page, Math.max(0, (total - 1) / rows)));
        for (int i = 0; i < rows; i++) {
            int index = page * rows + i;
            if (index >= total) break;
            buttonList.add(new GuiButton(index, width / 2 - 150, 50 + i * 24, 300, 20, label(configElements.get(index))));
        }
        buttonList.add(new GuiButton(PREVIOUS, width / 2 - 155, height - 27, 40, 20, "<"));
        buttonList.add(new GuiButton(DONE, width / 2 - 100, height - 27, 200, 20, StatCollector.translateToLocal("gui.done")));
        buttonList.add(new GuiButton(NEXT, width / 2 + 115, height - 27, 40, 20, ">"));
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == DONE) {
            mc.displayGuiScreen(parentScreen);
        } else if (button.id == PREVIOUS) {
            page--;
            initGui();
        } else if (button.id == NEXT) {
            page++;
            initGui();
        } else if (button.id >= 0 && button.id < configElements.size()) {
            IConfigElement e = configElements.get(button.id);
            if (e.getType() == GuiConfigEntries.ConfigGuiType.CONFIG_CATEGORY && e.getChildElements() != null) {
                mc.displayGuiScreen(new GuiConfig(this, e.getChildElements(), modID, configID, allRequireWorldRestart, allRequireMcRestart,
                        title, (titleLine2 == null ? "" : titleLine2 + " > ") + e.getName()));
            } else if (e.getType() == GuiConfigEntries.ConfigGuiType.BOOLEAN && !e.isList()) {
                e.set(!Boolean.TRUE.equals(e.get()));
                button.displayString = label(e);
            }
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawCenteredString(fontRendererObj, title, width / 2, 15, 0xFFFFFF);
        if (titleLine2 != null) drawCenteredString(fontRendererObj, titleLine2, width / 2, 30, 0xAAAAAA);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}
