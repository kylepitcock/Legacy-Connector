package mccompat.v1_7_10.net.minecraft.client.gui;

import java.util.ArrayList;
import java.util.List;
import mccompat.v1_7_10.net.minecraft.client.Minecraft;
import mccompat.v1_7_10.net.minecraft.client.audio.SoundHandler;
import mccompat.v1_7_10.net.minecraft.client.renderer.entity.RenderItem;
import mccompat.v1_7_10.net.minecraft.item.ItemStack;
import org.kylepitcock.mccompat.v1_7_10.client.RenderContext;

/**
 * 1.7.10 screen. Shown inside a modern screen ({@code LegacyScreen}/{@code LegacyContainerScreen}) that forwards
 * sizing, ticking, drawing, mouse and keyboard to these methods. Key codes are translated to LWJGL 2 values.
 */
public class GuiScreen extends Gui {
    protected static RenderItem itemRender = new RenderItem();
    public Minecraft mc;
    public int width;
    public int height;
    protected List buttonList = new ArrayList();
    protected List labelList = new ArrayList();
    public boolean allowUserInput;
    protected FontRenderer fontRendererObj;
    private GuiButton selectedButton;

    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        for (Object b : buttonList) ((GuiButton) b).drawButton(mc, mouseX, mouseY);
    }

    protected void keyTyped(char c, int keyCode) {
        if (keyCode == 1) {
            mc.displayGuiScreen(null);
        }
    }

    public static String getClipboardString() {
        return net.minecraft.client.Minecraft.getInstance().keyboardHandler.getClipboard();
    }

    public static void setClipboardString(String s) {
        net.minecraft.client.Minecraft.getInstance().keyboardHandler.setClipboard(s);
    }

    protected void renderToolTip(ItemStack stack, int x, int y) {
        var gui = RenderContext.gui();
        if (gui != null && stack != null) gui.renderTooltip(net.minecraft.client.Minecraft.getInstance().font, stack.mcc$modern(), x, y);
    }

    protected void drawCreativeTabHoveringText(String text, int x, int y) {
        drawHoveringText(java.util.Arrays.asList(text), x, y);
    }

    protected void drawHoveringText(List lines, int x, int y) {
        var gui = RenderContext.gui();
        if (gui == null || lines.isEmpty()) return;
        List<net.minecraft.network.chat.Component> components = new ArrayList<>();
        for (Object o : lines) components.add(net.minecraft.network.chat.Component.literal(String.valueOf(o)));
        gui.renderComponentTooltip(net.minecraft.client.Minecraft.getInstance().font, components, x, y);
    }

    protected void drawHoveringText(List lines, int x, int y, FontRenderer font) {
        drawHoveringText(lines, x, y);
    }

    protected void mouseClicked(int mouseX, int mouseY, int button) {
        if (button == 0) {
            for (Object o : buttonList) {
                GuiButton b = (GuiButton) o;
                if (b.mousePressed(mc, mouseX, mouseY)) {
                    selectedButton = b;
                    b.playPressSound(SoundHandler.INSTANCE);
                    actionPerformed(b);
                }
            }
        }
    }

    protected void mouseMovedOrUp(int mouseX, int mouseY, int which) {
        if (selectedButton != null && which == 0) {
            selectedButton.mouseReleased(mouseX, mouseY);
            selectedButton = null;
        }
    }

    protected void mouseClickMove(int mouseX, int mouseY, int button, long time) {
    }

    protected void actionPerformed(GuiButton button) {
    }

    public void setWorldAndResolution(Minecraft mc, int width, int height) {
        this.mc = mc;
        this.fontRendererObj = mc.fontRenderer;
        this.width = width;
        this.height = height;
        this.buttonList.clear();
        initGui();
    }

    public void initGui() {
    }

    public void handleInput() {
    }

    public void handleMouseInput() {
    }

    public void handleKeyboardInput() {
    }

    public void updateScreen() {
    }

    public void onGuiClosed() {
    }

    public void drawDefaultBackground() {
        drawWorldBackground(0);
    }

    public void drawWorldBackground(int tint) {
        var gui = RenderContext.gui();
        if (gui != null) gui.fillGradient(0, 0, width, height, -1072689136, -804253680);
    }

    public void drawBackground(int tint) {
        drawWorldBackground(tint);
    }

    public boolean doesGuiPauseGame() {
        return true;
    }

    public void confirmClicked(boolean result, int id) {
    }

    public static boolean isCtrlKeyDown() {
        return net.minecraft.client.gui.screens.Screen.hasControlDown();
    }

    public static boolean isShiftKeyDown() {
        return net.minecraft.client.gui.screens.Screen.hasShiftDown();
    }

    /** Bridge entry points used by the hosting modern screen. */
    public void mcc$mouseClicked(int x, int y, int button) {
        mouseClicked(x, y, button);
    }

    public void mcc$mouseReleased(int x, int y, int button) {
        mouseMovedOrUp(x, y, button);
    }

    public void mcc$mouseDragged(int x, int y, int button) {
        mouseClickMove(x, y, button, 0L);
    }

    public void mcc$keyTyped(char c, int legacyKeyCode) {
        keyTyped(c, legacyKeyCode);
    }

    public void mcc$actionPerformed(GuiButton button) {
        actionPerformed(button);
    }

    public List mcc$buttons() {
        return buttonList;
    }
}
