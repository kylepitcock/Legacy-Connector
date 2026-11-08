package legacyconnector.v1_7_10.codechicken.nei.guihook;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import legacyconnector.v1_7_10.net.minecraft.client.gui.FontRenderer;
import legacyconnector.v1_7_10.net.minecraft.client.gui.inventory.GuiContainer;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;

/**
 * NEI's per-GUI hook manager: the handler lists mods register into (tooltips, input, objects, drawing). The layer's JEI bridge draws the
 * overlay itself, so the lists are only consulted by the helpers below (item names and the stack under the cursor).
 */
public class GuiContainerManager {
    public static final LinkedList<IContainerTooltipHandler> tooltipHandlers = new LinkedList<>();
    public static final LinkedList<IContainerInputHandler> inputHandlers = new LinkedList<>();
    public static final LinkedList<IContainerDrawHandler> drawHandlers = new LinkedList<>();
    public static final List<IContainerObjectHandler> objectHandlers = new CopyOnWriteArrayList<>();

    private static final java.util.Map<GuiContainer, GuiContainerManager> MANAGERS = new java.util.WeakHashMap<>();

    public final GuiContainer window;

    public GuiContainerManager(GuiContainer gui) {
        this.window = gui;
    }

    public static synchronized GuiContainerManager getManager(GuiContainer gui) {
        return gui == null ? null : MANAGERS.computeIfAbsent(gui, GuiContainerManager::new);
    }

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

    /** False when an object handler says its object under the cursor draws its own tooltip. */
    public static boolean shouldShowTooltip(GuiContainer gui) {
        for (IContainerObjectHandler h : objectHandlers) if (!h.shouldShowTooltip(gui)) return false;
        return true;
    }

    /** The stack of the slot under the cursor, or what an object handler shows there. */
    public static ItemStack getStackMouseOver(GuiContainer gui) {
        return null;
    }

    public static String itemDisplayNameShort(ItemStack stack) {
        try {
            String name = stack.getDisplayName();
            return name == null ? "" : name;
        } catch (RuntimeException e) {
            return stack.getUnlocalizedName();
        }
    }

    /** The name line and the extra lines of the stack's tooltip. */
    public static List<String> itemDisplayNameMultiline(ItemStack stack, GuiContainer gui, boolean includeHandlers) {
        List<String> lines = new ArrayList<>();
        lines.add(itemDisplayNameShort(stack));
        if (includeHandlers && gui != null) {
            for (IContainerTooltipHandler h : tooltipHandlers) lines = h.handleItemDisplayName(gui, stack, lines);
        }
        return lines;
    }

    public static FontRenderer getFontRenderer(ItemStack stack) {
        FontRenderer f = stack == null ? null : stack.getItem().getFontRenderer(stack);
        return f != null ? f : legacyconnector.v1_7_10.net.minecraft.client.Minecraft.getMinecraft().fontRenderer;
    }

    public void load() {
    }

    public void refresh() {
    }
}
