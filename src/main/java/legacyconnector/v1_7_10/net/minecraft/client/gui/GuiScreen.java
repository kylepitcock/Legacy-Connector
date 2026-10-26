package legacyconnector.v1_7_10.net.minecraft.client.gui;

import java.util.ArrayList;
import java.util.List;
import legacyconnector.v1_7_10.net.minecraft.client.Minecraft;
import legacyconnector.v1_7_10.net.minecraft.client.audio.SoundHandler;
import legacyconnector.v1_7_10.net.minecraft.client.renderer.entity.RenderItem;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext;

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
    /** The button held down (-1 for none) and when it was pressed; drags are reported for it (1.7.10's handleMouseInput). */
    private int eventButton = -1;
    private long lastMouseEvent;
    protected int touchValue;

    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        for (Object b : buttonList) ((GuiButton) b).drawButton(mc, mouseX, mouseY);
        for (Object l : labelList) {
            if (l instanceof GuiLabel label) label.drawLabel(mc, mouseX, mouseY);
        }
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

    /**
     * Turns the mouse event being dispatched into a click, release or drag, as 1.7.10 did. The hosting screen sends every modern
     * mouse event through here (see {@link #mcc$mouseClicked}), so mods that override this to read {@code Mouse.getEventDWheel()}
     * or {@code Mouse.getEventButton()} see them; the position is the event's GUI position.
     */
    public void handleMouseInput() {
        int x;
        int y;
        if (mcc$dispatching) {
            x = mcc$eventX;
            y = mcc$eventY;
        } else {
            x = legacyconnector.v1_7_10.org.lwjgl.input.Mouse.getEventX() * width / Math.max(1, Minecraft.mcc$get$displayWidth(mc));
            y = height - legacyconnector.v1_7_10.org.lwjgl.input.Mouse.getEventY() * height / Math.max(1, Minecraft.mcc$get$displayHeight(mc)) - 1;
        }
        int button = legacyconnector.v1_7_10.org.lwjgl.input.Mouse.getEventButton();
        if (legacyconnector.v1_7_10.org.lwjgl.input.Mouse.getEventButtonState()) {
            eventButton = button;
            lastMouseEvent = Minecraft.getSystemTime();
            mouseClicked(x, y, eventButton);
        } else if (button != -1) {
            eventButton = -1;
            mouseMovedOrUp(x, y, button);
        } else if (eventButton != -1 && lastMouseEvent > 0L) {
            mouseClickMove(x, y, eventButton, Minecraft.getSystemTime() - lastMouseEvent);
        }
    }

    /** A key press becomes {@link #keyTyped}; releases are only seen by overrides (1.7.10's handleKeyboardInput). */
    public void handleKeyboardInput() {
        if (legacyconnector.v1_7_10.org.lwjgl.input.Keyboard.getEventKeyState()) {
            keyTyped(legacyconnector.v1_7_10.org.lwjgl.input.Keyboard.getEventCharacter(), legacyconnector.v1_7_10.org.lwjgl.input.Keyboard.getEventKey());
        }
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

    // ------------------------------------------------------------------ bridge entry points used by the hosting modern screen

    private boolean mcc$dispatching;
    private int mcc$eventX;
    private int mcc$eventY;
    private static int mcc$pendingWheel;
    private static int mcc$frameWheel;

    /** Runs {@link #handleMouseInput} with the LWJGL 2 event state of one modern mouse event. */
    private void mcc$dispatchMouse(int x, int y, int button, boolean pressed, int wheel) {
        boolean outer = mcc$dispatching;
        int ox = mcc$eventX;
        int oy = mcc$eventY;
        mcc$dispatching = true;
        mcc$eventX = x;
        mcc$eventY = y;
        legacyconnector.v1_7_10.org.lwjgl.input.Mouse.mcc$setEvent(button, pressed);
        legacyconnector.v1_7_10.org.lwjgl.input.Mouse.mcc$setEventWheel(wheel, true);
        try {
            handleMouseInput();
        } finally {
            mcc$dispatching = outer;
            mcc$eventX = ox;
            mcc$eventY = oy;
            legacyconnector.v1_7_10.org.lwjgl.input.Mouse.mcc$setEvent(-1, false);
            legacyconnector.v1_7_10.org.lwjgl.input.Mouse.mcc$setEventWheel(0, false);
        }
    }

    public void mcc$mouseClicked(int x, int y, int button) {
        mcc$dispatchMouse(x, y, button, true, 0);
    }

    public void mcc$mouseReleased(int x, int y, int button) {
        mcc$dispatchMouse(x, y, button, false, 0);
    }

    public void mcc$mouseDragged(int x, int y, int button) {
        mcc$dispatchMouse(x, y, -1, false, 0);
    }

    /** A wheel event: {@code clicks} notches, positive away from the user (LWJGL 2 reports 120 per notch). */
    public void mcc$mouseScrolled(int x, int y, double clicks) {
        int wheel = (int) Math.round(clicks * 120.0D);
        if (wheel == 0) return;
        mcc$pendingWheel += wheel;
        mcc$dispatchMouse(x, y, -1, false, wheel);
    }

    /** Called by the hosting screen before each frame: the wheel movement {@link GuiSlot} lists see while drawing. */
    public static void mcc$beginFrame() {
        mcc$frameWheel = mcc$pendingWheel;
        mcc$pendingWheel = 0;
    }

    public static int mcc$frameWheel() {
        return mcc$frameWheel;
    }

    public void mcc$keyTyped(char c, int legacyKeyCode) {
        mcc$dispatchKey(c, legacyKeyCode, true);
    }

    public void mcc$keyReleased(int legacyKeyCode) {
        mcc$dispatchKey('\0', legacyKeyCode, false);
    }

    private void mcc$dispatchKey(char c, int legacyKeyCode, boolean pressed) {
        legacyconnector.v1_7_10.org.lwjgl.input.Keyboard.mcc$setEvent(legacyKeyCode, pressed, c);
        try {
            handleKeyboardInput();
        } finally {
            legacyconnector.v1_7_10.org.lwjgl.input.Keyboard.mcc$setEvent(0, false, '\0');
        }
    }

    public void mcc$actionPerformed(GuiButton button) {
        actionPerformed(button);
    }

    public List mcc$buttons() {
        return buttonList;
    }
}
