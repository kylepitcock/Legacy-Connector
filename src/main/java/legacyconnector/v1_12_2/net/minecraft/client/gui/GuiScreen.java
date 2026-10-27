package legacyconnector.v1_12_2.net.minecraft.client.gui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import legacyconnector.v1_12_2.net.minecraft.client.Minecraft;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import org.kylepitcock.legacyconnector.v1_12_2.client.InputEvent1122;
import org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext;

/**
 * 1.12.2 screen. Shown inside a modern screen ({@code LegacyScreen1122}/{@code LegacyContainerScreen1122}) that forwards sizing, ticking,
 * drawing, mouse and keyboard here. Mouse and key events go through {@link #handleMouseInput}/{@link #handleKeyboardInput} as in 1.12, with
 * the event state in {@link InputEvent1122}; key codes are LWJGL 2 values.
 */
public class GuiScreen extends Gui implements GuiYesNoCallback {
    public Minecraft mc;
    public int width;
    public int height;
    protected List<GuiButton> buttonList = new ArrayList<>();
    protected List<GuiLabel> labelList = new ArrayList<>();
    public boolean allowUserInput;
    protected FontRenderer fontRenderer;
    /** The game's item renderer (screens draw stacks with it). */
    protected legacyconnector.v1_12_2.net.minecraft.client.renderer.RenderItem itemRender;
    protected GuiButton selectedButton;
    private int eventButton;
    private long lastMouseEvent;
    private int touchValue;
    protected boolean keyHandled;
    protected boolean mouseHandled;

    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        for (int i = 0; i < buttonList.size(); ++i) buttonList.get(i).drawButton(mc, mouseX, mouseY, partialTicks);
        for (int j = 0; j < labelList.size(); ++j) labelList.get(j).drawLabel(mc, mouseX, mouseY);
    }

    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == 1) {
            mc.displayGuiScreen(null);
        }
    }

    protected <T extends GuiButton> T addButton(T button) {
        buttonList.add(button);
        return button;
    }

    public static String getClipboardString() {
        try {
            return net.minecraft.client.Minecraft.getInstance().keyboardHandler.getClipboard();
        } catch (RuntimeException e) {
            return "";
        }
    }

    public static void setClipboardString(String copyText) {
        if (copyText == null || copyText.isEmpty()) return;
        try {
            net.minecraft.client.Minecraft.getInstance().keyboardHandler.setClipboard(copyText);
        } catch (RuntimeException e) {
            // no clipboard
        }
    }

    protected void renderToolTip(ItemStack stack, int x, int y) {
        var gui = RenderContext.gui();
        if (gui == null || stack == null || stack.isEmpty()) return;
        gui.renderTooltip(net.minecraft.client.Minecraft.getInstance().font, org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122.toModern(stack), x, y);
    }

    /** The stack's tooltip lines (name first) with their formatting codes, advanced details when F3+H is on. */
    public List<String> getItemToolTip(ItemStack stack) {
        return org.kylepitcock.legacyconnector.v1_12_2.client.ClientGui1122.tooltipLines(stack);
    }

    public void drawHoveringText(String text, int x, int y) {
        drawHoveringText(java.util.Arrays.asList(text), x, y);
    }

    private boolean focused;

    /** Whether a control of the screen (a text field) has the keyboard; key bindings are not handled while it has. */
    public void setFocused(boolean hasFocusedControlIn) {
        this.focused = hasFocusedControlIn;
    }

    public boolean isFocused() {
        return focused;
    }

    public void drawHoveringText(List<String> textLines, int x, int y) {
        drawHoveringText(textLines, x, y, fontRenderer);
    }

    protected void drawHoveringText(List<String> textLines, int x, int y, FontRenderer font) {
        var gui = RenderContext.gui();
        if (gui == null || textLines == null || textLines.isEmpty()) return;
        List<net.minecraft.network.chat.Component> lines = new ArrayList<>();
        for (Object o : textLines) lines.add(net.minecraft.network.chat.Component.literal(String.valueOf(o)));
        gui.renderComponentTooltip(net.minecraft.client.Minecraft.getInstance().font, lines, x, y);
    }

    /** Runs the click action of a chat component (open a link, run a command, suggest text) as a modern screen does. */
    public boolean handleComponentClick(legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent component) {
        if (component == null) return false;
        var screen = net.minecraft.client.Minecraft.getInstance().screen;
        if (screen == null) return false;
        return screen.handleComponentClicked(component.mcc$modern().getStyle());
    }

    public void sendChatMessage(String msg) {
        sendChatMessage(msg, true);
    }

    public void sendChatMessage(String msg, boolean addToChat) {
        var mcm = net.minecraft.client.Minecraft.getInstance();
        if (addToChat) mcm.gui.getChat().addRecentChat(msg);
        if (mcm.player == null) return;
        if (msg.startsWith("/")) mcm.player.connection.sendCommand(msg.substring(1));
        else mcm.player.connection.sendChat(msg);
    }

    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (mouseButton != 0) return;
        for (int i = 0; i < buttonList.size(); ++i) {
            GuiButton b = buttonList.get(i);
            if (b.mousePressed(mc, mouseX, mouseY)) {
                var pre = new legacyconnector.v1_12_2.net.minecraftforge.client.event.GuiScreenEvent.ActionPerformedEvent.Pre(this, b, buttonList);
                if (mcc$post(pre)) break;
                b = pre.getButton();
                selectedButton = b;
                b.playPressSound(mc == null ? null : mc.getSoundHandler());
                actionPerformed(b);
                if (org.kylepitcock.legacyconnector.v1_12_2.client.ClientGui1122.currentLegacyScreen() == this) {
                    mcc$post(new legacyconnector.v1_12_2.net.minecraftforge.client.event.GuiScreenEvent.ActionPerformedEvent.Post(this, pre.getButton(), buttonList));
                }
            }
        }
    }

    protected void mouseReleased(int mouseX, int mouseY, int state) {
        if (selectedButton != null && state == 0) {
            selectedButton.mouseReleased(mouseX, mouseY);
            selectedButton = null;
        }
    }

    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
    }

    protected void actionPerformed(GuiButton button) throws IOException {
    }

    public void setWorldAndResolution(Minecraft mc, int width, int height) {
        this.mc = mc;
        this.itemRender = legacyconnector.v1_12_2.net.minecraft.client.renderer.RenderItem.mcc$instance();
        this.fontRenderer = mc.fontRenderer;
        this.width = width;
        this.height = height;
        if (!mcc$post(new legacyconnector.v1_12_2.net.minecraftforge.client.event.GuiScreenEvent.InitGuiEvent.Pre(this, buttonList))) {
            this.buttonList.clear();
            initGui();
        }
        mcc$post(new legacyconnector.v1_12_2.net.minecraftforge.client.event.GuiScreenEvent.InitGuiEvent.Post(this, buttonList));
    }

    /** Posts a Forge event when somebody listens to it; true if it was cancelled. */
    public static boolean mcc$post(legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event event) {
        var bus = legacyconnector.v1_12_2.net.minecraftforge.common.MinecraftForge.EVENT_BUS;
        return bus.mcc$hasListeners(event.getClass()) && bus.post(event);
    }

    /** Sets up a stand-in of a modern screen (no init events: the modern screen was set up by the game). */
    public void mcc$initView(Minecraft mc, int w, int h) {
        this.mc = mc;
        this.fontRenderer = mc.fontRenderer;
        this.itemRender = legacyconnector.v1_12_2.net.minecraft.client.renderer.RenderItem.mcc$instance();
        this.width = w;
        this.height = h;
    }

    public void setGuiSize(int w, int h) {
        this.width = w;
        this.height = h;
    }

    public void initGui() {
    }

    /** The hosting screen hands each event to the handlers itself; this is what a mod's own call does. */
    public void handleInput() throws IOException {
    }

    /** Turns the mouse event being dispatched into a click, release or drag, as 1.12 did (mods override this to read the wheel). */
    public void handleMouseInput() throws IOException {
        int i = InputEvent1122.guiX;
        int j = InputEvent1122.guiY;
        int k = InputEvent1122.eventButton;
        if (InputEvent1122.eventButtonState) {
            eventButton = k;
            lastMouseEvent = Minecraft.getSystemTime();
            mouseClicked(i, j, eventButton);
        } else if (k != -1) {
            eventButton = -1;
            mouseReleased(i, j, k);
        } else if (eventButton != -1 && lastMouseEvent > 0L) {
            long l = Minecraft.getSystemTime() - lastMouseEvent;
            mouseClickMove(i, j, eventButton, l);
        }
    }

    /** A key press (or a typed character) becomes {@link #keyTyped}; releases are only seen by overrides. */
    public void handleKeyboardInput() throws IOException {
        char c = InputEvent1122.eventCharacter;
        if (InputEvent1122.eventKey == 0 && c >= ' ' || InputEvent1122.eventKeyState) keyTyped(c, InputEvent1122.eventKey);
    }

    public void updateScreen() {
    }

    public void onGuiClosed() {
    }

    public void drawDefaultBackground() {
        drawWorldBackground(0);
        mcc$post(new legacyconnector.v1_12_2.net.minecraftforge.client.event.GuiScreenEvent.BackgroundDrawnEvent(this));
    }

    public void drawWorldBackground(int tint) {
        var gui = RenderContext.gui();
        if (gui == null) return;
        if (net.minecraft.client.Minecraft.getInstance().level != null) gui.fillGradient(0, 0, width, height, -1072689136, -804253680);
        else drawBackground(tint);
    }

    /** The options background tiled over the screen (the dirt image of 1.12 is the dirt block texture now). */
    public void drawBackground(int tint) {
        var gui = RenderContext.gui();
        if (gui == null) return;
        gui.setColor(0.25F, 0.25F, 0.25F, 1.0F);
        gui.blit(net.minecraft.resources.ResourceLocation.withDefaultNamespace("textures/block/dirt.png"), 0, 0, 0.0F, (float) tint, width, height, 32, 32);
        gui.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public boolean doesGuiPauseGame() {
        return true;
    }

    @Override
    public void confirmClicked(boolean result, int id) {
    }

    public static boolean isCtrlKeyDown() {
        return net.minecraft.client.gui.screens.Screen.hasControlDown();
    }

    public static boolean isShiftKeyDown() {
        return net.minecraft.client.gui.screens.Screen.hasShiftDown();
    }

    public static boolean isAltKeyDown() {
        return net.minecraft.client.gui.screens.Screen.hasAltDown();
    }

    public static boolean isKeyComboCtrlX(int keyID) {
        return keyID == 45 && isCtrlKeyDown() && !isShiftKeyDown() && !isAltKeyDown();
    }

    public static boolean isKeyComboCtrlV(int keyID) {
        return keyID == 47 && isCtrlKeyDown() && !isShiftKeyDown() && !isAltKeyDown();
    }

    public static boolean isKeyComboCtrlC(int keyID) {
        return keyID == 46 && isCtrlKeyDown() && !isShiftKeyDown() && !isAltKeyDown();
    }

    public static boolean isKeyComboCtrlA(int keyID) {
        return keyID == 30 && isCtrlKeyDown() && !isShiftKeyDown() && !isAltKeyDown();
    }

    public void onResize(Minecraft mcIn, int w, int h) {
        setWorldAndResolution(mcIn, w, h);
    }

    // ------------------------------------------------------------------ entry points of the hosting modern screen

    /** Runs {@link #handleMouseInput} with one modern mouse event as LWJGL 2 state. */
    private void mcc$dispatchMouse(int x, int y, int button, boolean pressed, int wheel) {
        boolean outer = InputEvent1122.mouseDispatching;
        int ob = InputEvent1122.eventButton, ow = InputEvent1122.eventDWheel, ox = InputEvent1122.guiX, oy = InputEvent1122.guiY;
        boolean os = InputEvent1122.eventButtonState;
        InputEvent1122.mouseDispatching = true;
        InputEvent1122.eventButton = button;
        InputEvent1122.eventButtonState = pressed;
        InputEvent1122.eventDWheel = wheel;
        InputEvent1122.guiX = x;
        InputEvent1122.guiY = y;
        try {
            var window = net.minecraft.client.Minecraft.getInstance().getWindow();
            double scale = window.getGuiScale();
            InputEvent1122.eventX = (int) (x * scale);
            InputEvent1122.eventY = window.getHeight() - (int) (y * scale) - 1;
        } catch (RuntimeException e) {
            InputEvent1122.eventX = x;
            InputEvent1122.eventY = y;
        }
        try {
            if (!mcc$post(new legacyconnector.v1_12_2.net.minecraftforge.client.event.GuiScreenEvent.MouseInputEvent.Pre(this))) {
                handleMouseInput();
                mcc$post(new legacyconnector.v1_12_2.net.minecraftforge.client.event.GuiScreenEvent.MouseInputEvent.Post(this));
            }
        } catch (IOException e) {
            throw new java.io.UncheckedIOException(e);
        } finally {
            InputEvent1122.mouseDispatching = outer;
            InputEvent1122.eventButton = ob;
            InputEvent1122.eventButtonState = os;
            InputEvent1122.eventDWheel = ow;
            InputEvent1122.guiX = ox;
            InputEvent1122.guiY = oy;
        }
    }

    public void mcc$mouseClicked(int x, int y, int button) {
        mcc$dispatchMouse(x, y, button, true, 0);
    }

    public void mcc$mouseReleased(int x, int y, int button) {
        mcc$dispatchMouse(x, y, button, false, 0);
    }

    public void mcc$mouseDragged(int x, int y) {
        mcc$dispatchMouse(x, y, -1, false, 0);
    }

    /** A wheel event of {@code clicks} notches, positive away from the user (LWJGL 2 reported 120 per notch). */
    public void mcc$mouseScrolled(int x, int y, double clicks) {
        int wheel = (int) Math.round(clicks * 120.0D);
        if (wheel != 0) mcc$dispatchMouse(x, y, -1, false, wheel);
    }

    public void mcc$keyTyped(char c, int legacyKeyCode) {
        mcc$dispatchKey(c, legacyKeyCode, true);
    }

    public void mcc$keyReleased(int legacyKeyCode) {
        mcc$dispatchKey('\0', legacyKeyCode, false);
    }

    private void mcc$dispatchKey(char c, int legacyKeyCode, boolean pressed) {
        boolean outer = InputEvent1122.keyDispatching;
        int ok = InputEvent1122.eventKey;
        char oc = InputEvent1122.eventCharacter;
        boolean os = InputEvent1122.eventKeyState;
        InputEvent1122.keyDispatching = true;
        InputEvent1122.eventKey = legacyKeyCode;
        InputEvent1122.eventCharacter = c;
        InputEvent1122.eventKeyState = pressed;
        try {
            if (!mcc$post(new legacyconnector.v1_12_2.net.minecraftforge.client.event.GuiScreenEvent.KeyboardInputEvent.Pre(this))) {
                handleKeyboardInput();
                mcc$post(new legacyconnector.v1_12_2.net.minecraftforge.client.event.GuiScreenEvent.KeyboardInputEvent.Post(this));
            }
        } catch (IOException e) {
            throw new java.io.UncheckedIOException(e);
        } finally {
            InputEvent1122.keyDispatching = outer;
            InputEvent1122.eventKey = ok;
            InputEvent1122.eventCharacter = oc;
            InputEvent1122.eventKeyState = os;
        }
    }

    public List<GuiButton> mcc$buttons() {
        return buttonList;
    }

    public void mcc$actionPerformed(GuiButton button) {
        try {
            actionPerformed(button);
        } catch (IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }
}
