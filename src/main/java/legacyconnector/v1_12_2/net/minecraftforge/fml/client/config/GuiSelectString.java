package legacyconnector.v1_12_2.net.minecraftforge.fml.client.config;

import static legacyconnector.v1_12_2.net.minecraftforge.fml.client.config.GuiUtils.RESET_CHAR;
import static legacyconnector.v1_12_2.net.minecraftforge.fml.client.config.GuiUtils.UNDO_CHAR;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import legacyconnector.v1_12_2.net.minecraft.client.Minecraft;
import legacyconnector.v1_12_2.net.minecraft.client.gui.GuiButton;
import legacyconnector.v1_12_2.net.minecraft.client.gui.GuiScreen;
import legacyconnector.v1_12_2.net.minecraft.client.resources.I18n;
import legacyconnector.v1_12_2.net.minecraft.util.text.TextFormatting;
import org.slf4j.LoggerFactory;

/** Forge's screen for choosing one value of a map (the config screens' mod id picker). */
public class GuiSelectString extends GuiScreen {
    protected GuiScreen parentScreen;
    protected IConfigElement configElement;
    protected GuiSelectStringEntries entryList;
    protected GuiButtonExt btnUndoChanges;
    protected GuiButtonExt btnDefault;
    protected GuiButtonExt btnDone;
    protected String title;
    protected String titleLine2;
    protected String titleLine3;
    protected int slotIndex;
    protected final Map<Object, String> selectableValues;
    public final Object beforeValue;
    public Object currentValue;
    protected HoverChecker tooltipHoverChecker;
    protected List toolTip;
    protected boolean enabled;

    @SuppressWarnings("unchecked")
    public GuiSelectString(GuiScreen parentScreen, IConfigElement configElement, int slotIndex, Map<Object, String> selectableValues, Object currentValue,
            boolean enabled) {
        this.mc = Minecraft.getMinecraft();
        this.fontRenderer = mc.fontRenderer;
        this.parentScreen = parentScreen;
        this.configElement = configElement;
        this.slotIndex = slotIndex;
        this.selectableValues = selectableValues;
        this.beforeValue = currentValue;
        this.currentValue = currentValue;
        this.toolTip = new ArrayList();
        this.enabled = enabled;
        String key = configElement.getLanguageKey();
        String propName = I18n.format(key);
        String comment = I18n.format(key + ".tooltip");
        if (!comment.equals(key + ".tooltip")) {
            Collections.addAll(toolTip, (TextFormatting.GREEN + propName + "\n" + TextFormatting.YELLOW + comment).split("\n"));
        } else if (configElement.getComment() != null && !configElement.getComment().trim().isEmpty()) {
            Collections.addAll(toolTip, (TextFormatting.GREEN + propName + "\n" + TextFormatting.YELLOW + configElement.getComment()).split("\n"));
        } else {
            Collections.addAll(toolTip, (TextFormatting.GREEN + propName + "\n" + TextFormatting.RED + "No tooltip defined.").split("\n"));
        }
        if (parentScreen instanceof GuiConfig config) {
            this.title = config.title;
            this.titleLine2 = config.titleLine2;
            this.titleLine3 = propName;
            this.tooltipHoverChecker = new HoverChecker(28, 37, 0, parentScreen.width, 800);
        } else {
            this.title = propName;
            this.tooltipHoverChecker = new HoverChecker(8, 17, 0, parentScreen == null ? 0 : parentScreen.width, 800);
        }
    }

    @Override
    public void initGui() {
        entryList = new GuiSelectStringEntries(this, mc, configElement, selectableValues);
        int undoGlyphWidth = mc.fontRenderer.getStringWidth(UNDO_CHAR) * 2;
        int resetGlyphWidth = mc.fontRenderer.getStringWidth(RESET_CHAR) * 2;
        int doneWidth = Math.max(mc.fontRenderer.getStringWidth(I18n.format("gui.done")) + 20, 100);
        int undoWidth = mc.fontRenderer.getStringWidth(" " + I18n.format("fml.configgui.tooltip.undoChanges")) + undoGlyphWidth + 20;
        int resetWidth = mc.fontRenderer.getStringWidth(" " + I18n.format("fml.configgui.tooltip.resetToDefault")) + resetGlyphWidth + 20;
        int buttonWidthHalf = (doneWidth + 5 + undoWidth + 5 + resetWidth) / 2;
        buttonList.add(btnDone = new GuiButtonExt(2000, width / 2 - buttonWidthHalf, height - 29, doneWidth, 20, I18n.format("gui.done")));
        buttonList.add(btnDefault = new GuiUnicodeGlyphButton(2001, width / 2 - buttonWidthHalf + doneWidth + 5 + undoWidth + 5, height - 29, resetWidth, 20,
                " " + I18n.format("fml.configgui.tooltip.resetToDefault"), RESET_CHAR, 2.0F));
        buttonList.add(btnUndoChanges = new GuiUnicodeGlyphButton(2002, width / 2 - buttonWidthHalf + doneWidth + 5, height - 29, undoWidth, 20,
                " " + I18n.format("fml.configgui.tooltip.undoChanges"), UNDO_CHAR, 2.0F));
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 2000) {
            try {
                entryList.saveChanges();
            } catch (Throwable e) {
                LoggerFactory.getLogger("LegacyConnector/Gui").error("Could not save {}", configElement.getName(), e);
            }
            mc.displayGuiScreen(parentScreen);
        } else if (button.id == 2001) {
            currentValue = configElement.getDefault();
            entryList = new GuiSelectStringEntries(this, mc, configElement, selectableValues);
        } else if (button.id == 2002) {
            currentValue = beforeValue;
            entryList = new GuiSelectStringEntries(this, mc, configElement, selectableValues);
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
        if (mouseEvent != 0 || !entryList.mouseClicked(x, y, mouseEvent)) super.mouseClicked(x, y, mouseEvent);
    }

    @Override
    protected void mouseReleased(int x, int y, int mouseEvent) {
        if (mouseEvent != 0 || !entryList.mouseReleased(x, y, mouseEvent)) super.mouseReleased(x, y, mouseEvent);
    }

    @Override
    protected void keyTyped(char eventChar, int eventKey) {
        if (eventKey == 1) mc.displayGuiScreen(parentScreen);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        entryList.drawScreen(mouseX, mouseY, partialTicks);
        drawCenteredString(fontRenderer, title, width / 2, 8, 16777215);
        if (titleLine2 != null) drawCenteredString(fontRenderer, titleLine2, width / 2, 18, 16777215);
        if (titleLine3 != null) drawCenteredString(fontRenderer, titleLine3, width / 2, 28, 16777215);
        btnDone.enabled = currentValue != null;
        btnDefault.enabled = enabled && !entryList.isDefault();
        btnUndoChanges.enabled = enabled && entryList.isChanged();
        super.drawScreen(mouseX, mouseY, partialTicks);
        if (tooltipHoverChecker != null && tooltipHoverChecker.checkHover(mouseX, mouseY)) drawToolTip(toolTip, mouseX, mouseY);
    }

    public void drawToolTip(List stringList, int x, int y) {
        drawHoveringText(stringList, x, y);
    }
}
