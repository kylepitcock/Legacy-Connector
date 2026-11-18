package legacyconnector.v1_7_10.codechicken.nei.forge;

import java.awt.Point;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import legacyconnector.v1_7_10.codechicken.lib.gui.GuiDraw;
import legacyconnector.v1_7_10.codechicken.nei.recipe.TemplateRecipeHandler;
import legacyconnector.v1_7_10.net.minecraft.client.gui.FontRenderer;
import legacyconnector.v1_7_10.net.minecraft.client.gui.inventory.GuiContainer;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;

/**
 * NEI 1.4.7 / 1.5.2's GUI drawing hub ({@code codechicken.nei.forge.GuiContainerManager}; the 1.7.10 one is {@code guihook.GuiContainerManager}).
 * The recipe handlers of that era draw their machine pictures, progress bars, energy gauges and text through the manager they are handed
 * ({@code drawBackground(manager, recipe)}, {@code drawExtras(manager, recipe)}, {@code drawProgressBar(manager, ...)}) and read the mouse
 * from it ({@code handleTooltip(GuiRecipe, ...)} via {@code gui.manager.getMousePosition()}). Every drawing call goes to the legacy render
 * context the 1.7.10 handlers draw in (the recipe area's coordinates, the texture bound by the handler), through {@link GuiDraw}.
 *
 * <p>The handler lists are kept for mods that register hooks, but under JEI there is no NEI panel or container overlay to run them in.</p>
 */
public class GuiContainerManager {
    /** The manager the recipe drawing hands to handlers (the mouse is the real one). */
    public static final GuiContainerManager INSTANCE = new GuiContainerManager(null);

    public static final LinkedList<IContainerTooltipHandler> tooltipHandlers = new LinkedList<>();
    public static final LinkedList<IContainerInputHandler> inputHandlers = new LinkedList<>();
    public static final LinkedList<IContainerDrawHandler> drawHandlers = new LinkedList<>();
    public static final LinkedList<IContainerObjectHandler> objectHandlers = new LinkedList<>();
    public static final LinkedList<IContainerSlotClickHandler> slotClickHandlers = new LinkedList<>();

    private static final java.util.Map<GuiContainer, GuiContainerManager> MANAGERS = new java.util.WeakHashMap<>();

    /** NEI: the screen this manager belongs to. */
    public GuiContainer window;
    /** The mouse in GUI coordinates when a caller fixes it (the JEI tooltip, which asks the handler in the recipe's own frame); null reads the real mouse. */
    private Point mouse;

    public GuiContainerManager(GuiContainer gui) {
        this.window = gui;
    }

    /** The manager of a screen (one per screen). */
    public static synchronized GuiContainerManager getManager(GuiContainer gui) {
        return gui == null ? INSTANCE : MANAGERS.computeIfAbsent(gui, GuiContainerManager::new);
    }

    /** A manager that reports {@code mouse} as the mouse position (the layer's JEI tooltips). */
    public static GuiContainerManager withMouse(GuiContainer window, Point mouse) {
        GuiContainerManager m = new GuiContainerManager(window);
        m.mouse = mouse;
        return m;
    }

    // ------------------------------------------------------------------ registration

    public static void addTooltipHandler(IContainerTooltipHandler handler) {
        tooltipHandlers.add(handler);
    }

    public static void addInputHandler(IContainerInputHandler handler) {
        inputHandlers.add(handler);
    }

    public static void addDrawHandler(IContainerDrawHandler handler) {
        drawHandlers.add(handler);
    }

    public static void addObjectHandler(IContainerObjectHandler handler) {
        objectHandlers.add(handler);
    }

    public static void addSlotClickHandler(IContainerSlotClickHandler handler) {
        slotClickHandlers.add(handler);
    }

    // ------------------------------------------------------------------ mouse and strings

    /** The mouse in GUI coordinates. */
    public Point getMousePosition() {
        return mouse != null ? new Point(mouse) : GuiDraw.getMousePosition();
    }

    /** The stack of the slot under the mouse of this screen, or what an object handler shows there. */
    public ItemStack getStackMouseOver() {
        Point m = getMousePosition();
        for (IContainerObjectHandler h : objectHandlers) {
            ItemStack s = h.getStackUnderMouse(window, m.x, m.y);
            if (s != null) return s;
        }
        if (window == null) return null;
        try {
            var slot = window.mcc$slotAt(m.x, m.y);
            return slot == null ? null : slot.getStack();
        } catch (RuntimeException | LinkageError e) {
            return null;
        }
    }

    public int getStringWidth(String text) {
        return GuiDraw.getStringWidth(text);
    }

    public static int getStringWidthNoColours(FontRenderer font, String text) {
        return font.getStringWidth(text == null ? "" : text.replaceAll("§.", ""));
    }

    public static String itemDisplayNameShort(ItemStack stack) {
        return legacyconnector.v1_7_10.codechicken.nei.guihook.GuiContainerManager.itemDisplayNameShort(stack);
    }

    /** The name line and the extra lines of the stack's tooltip. */
    public static List<String> itemDisplayNameMultiline(ItemStack stack, GuiContainer gui, boolean includeHandlers) {
        List<String> lines = new ArrayList<>();
        lines.add(itemDisplayNameShort(stack));
        if (includeHandlers && gui != null) for (IContainerTooltipHandler h : tooltipHandlers) lines = h.handleItemTooltip(gui, stack, lines);
        return lines;
    }

    public static String concatenatedDisplayName(ItemStack stack, boolean includeHandlers) {
        StringBuilder out = new StringBuilder();
        for (String line : itemDisplayNameMultiline(stack, null, includeHandlers)) {
            if (out.length() > 0) out.append('\n');
            out.append(line);
        }
        return out.toString();
    }

    public FontRenderer getFontRenderer(ItemStack stack) {
        return legacyconnector.v1_7_10.codechicken.nei.guihook.GuiContainerManager.getFontRenderer(stack);
    }

    // ------------------------------------------------------------------ drawing

    /** A filled rectangle at (x, y), w wide and h high. */
    public void drawRect(int x, int y, int w, int h, int colour) {
        GuiDraw.drawRect(x, y, w, h, colour);
    }

    public void drawGradientRect(int x, int y, int w, int h, int colour1, int colour2) {
        GuiDraw.drawGradientRect(x, y, w, h, colour1, colour2);
    }

    /** A region of the bound 256x256 texture at (x, y). */
    public void drawTexturedModalRect(int x, int y, int u, int v, int w, int h) {
        TemplateRecipeHandler.mcc$drawTexturedModalRect(x, y, u, v, w, h);
    }

    /** Text at (x, y) in a colour (0xRRGGBB), with the shadow vanilla's labels have when asked. */
    public void drawText(int x, int y, String text, int colour, boolean shadow) {
        if (text == null) return;
        GuiDraw.drawString(text, x, y, colour, shadow);
    }

    public void drawText(int x, int y, String text, boolean shadow) {
        drawText(x, y, text, 0xFFFFFF, shadow);
    }

    public void drawText(int x, int y, String text, int colour) {
        drawText(x, y, text, colour, true);
    }

    public void drawText(int x, int y, String text) {
        drawText(x, y, text, 0xFFFFFF, true);
    }

    /** Text centred in the rectangle (x, y, w, h). */
    public void drawTextCentered(int x, int y, int w, int h, String text, int colour, boolean shadow) {
        if (text == null) return;
        GuiDraw.drawStringC(text, x, y, w, h, colour, shadow);
    }

    public void drawTextCentered(int x, int y, int w, int h, String text, int colour) {
        drawTextCentered(x, y, w, h, text, colour, true);
    }

    /** Text centred on x, with its top at y, in a width of w around it (NEI: the text is centred on the point x + w / 2). */
    public void drawTextCentered(String text, int x, int y, int colour, boolean shadow) {
        if (text == null) return;
        GuiDraw.drawStringC(text, x, y, colour, shadow);
    }

    public void drawTextCentered(String text, int x, int y, int colour) {
        drawTextCentered(text, x, y, colour, true);
    }

    /** A one-line tooltip box at (x, y). */
    public void drawTip(int x, int y, String text) {
        if (text == null) return;
        List<String> lines = new ArrayList<>();
        lines.add(text);
        drawMultilineTip(x, y, lines);
    }

    public void drawMultilineTip(int x, int y, List<String> lines) {
        GuiDraw.drawMultilineTip(GuiDraw.fontRenderer, x, y, lines);
    }

    /** An item at (x, y) with its count and damage bar, as a slot draws it. */
    public void drawItem(int x, int y, ItemStack stack) {
        if (stack == null) return;
        var render = new legacyconnector.v1_7_10.net.minecraft.client.renderer.entity.RenderItem();
        render.renderItemAndEffectIntoGUI(GuiDraw.fontRenderer, GuiDraw.renderEngine, stack, x, y);
        render.renderItemOverlayIntoGUI(GuiDraw.fontRenderer, GuiDraw.renderEngine, stack, x, y);
    }

    public static void drawItem(int x, int y, ItemStack stack, FontRenderer font, legacyconnector.v1_7_10.net.minecraft.client.renderer.entity.RenderItem render) {
        if (stack == null) return;
        if (render == null) render = new legacyconnector.v1_7_10.net.minecraft.client.renderer.entity.RenderItem();
        render.renderItemAndEffectIntoGUI(font, GuiDraw.renderEngine, stack, x, y);
        render.renderItemOverlayIntoGUI(font, GuiDraw.renderEngine, stack, x, y);
    }

    /** NEI drew the items of its panel without the tint of the held colour; the layer's item drawing needs no such switch. */
    public void setColouredItemRender(boolean coloured) {
    }

    public static void enable3DRender() {
    }

    public static void enable2DRender() {
    }

    // ------------------------------------------------------------------ textures

    /** 1.4.7: binds a texture by its path ({@code "/mods/thermalexpansion/gfx/gui_smelter.png"}). */
    public void bindTextureByName(String path) {
        TemplateRecipeHandler.mcc$changeTexture(path);
    }

    /** 1.5.2: the same by the name {@code bindTexture(String)}. */
    public void bindTexture(String path) {
        TemplateRecipeHandler.mcc$changeTexture(path);
    }

    /** 1.4.7: the OpenGL id of a texture path (a texture the game loads when it is first bound). */
    public int getTexture(String path) {
        return legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.TextureManager.INSTANCE.getTexture(path);
    }

    /** 1.4.7: binds a texture by the id {@link #getTexture} returned. */
    public void bindTexture(int glId) {
        legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.TextureManager.INSTANCE.bindTexture(glId);
    }

    // ------------------------------------------------------------------ lifecycle (the screen's own hooks; nothing drives them under JEI)

    public void load() {
    }

    public void refresh() {
    }

    public void guiTick() {
    }
}
