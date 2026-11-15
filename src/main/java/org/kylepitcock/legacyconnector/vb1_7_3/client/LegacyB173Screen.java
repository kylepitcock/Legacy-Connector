package org.kylepitcock.legacyconnector.vb1_7_3.client;

import legacyconnector.vb1_7_3.net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.kylepitcock.legacyconnector.vb1_7_3.LegacyB173Menu;
import org.kylepitcock.legacyconnector.vb1_7_3.LegacyMenuGeometryB173;
import org.kylepitcock.legacyconnector.vb1_7_3.MenusB173;
import org.slf4j.LoggerFactory;

/**
 * The modern screen of a {@link LegacyB173Menu}: the mod's {@code GuiContainer} draws background, labels and buttons (its {@code drawScreen}),
 * the modern screen the slots, items, carried stack and tooltips, and handles slot clicks. Without a legacy GUI (a ModLoaderMp container,
 * or a GUI that failed) a plain panel with the slots is drawn.
 */
public class LegacyB173Screen extends AbstractContainerScreen<LegacyB173Menu> {
    private final HandledScreen gui;
    private boolean failed;
    private int pendingKey = -1;
    private boolean backgroundPrepared;

    public LegacyB173Screen(LegacyB173Menu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.gui = menu.gui() instanceof HandledScreen hs ? hs : null;
        this.imageWidth = menu.backgroundWidth();
        this.imageHeight = menu.backgroundHeight();
    }

    /** The original screen replaced Beta's {@code GuiContainer.drawScreen} and drew its slots' tooltips itself (or none). */
    private boolean ownSlots() {
        if (remote != null) return remote.getBoolean("ownSlots");
        return live() && !gui.mcc$drewSlots;
    }

    private boolean live() {
        return gui != null && !failed;
    }

    private void fail(String what, Throwable e) {
        if (failed) return;
        failed = true;
        LoggerFactory.getLogger("LegacyConnector/b1.7.3").error("b1.7.3 GUI {} failed in {}; drawn as a plain panel", gui.getClass().getName(), what, e);
    }

    /** A frame of this menu's original screen drawn by the server, when the client could not make the screen itself. */
    private net.minecraft.nbt.CompoundTag remote;
    private boolean remoteRequested;
    private int lastMouseX, lastMouseY;

    void remoteFrame(net.minecraft.nbt.CompoundTag frame) { remote = frame; }

    private void sendRemote(String action, java.util.function.Consumer<net.minecraft.nbt.CompoundTag> values) {
        if (minecraft == null || minecraft.getConnection() == null) return;
        var tag = new net.minecraft.nbt.CompoundTag();
        tag.putInt("id", remote == null ? 0 : remote.getInt("id"));
        tag.putInt("menu", menu.containerId);
        tag.putString("action", action);
        tag.putInt("width", width); tag.putInt("height", height); tag.putInt("x", lastMouseX); tag.putInt("y", lastMouseY);
        values.accept(tag);
        org.kylepitcock.legacyconnector.vb1_7_3.GuiSessionsB173.sendInput(tag);
    }

    @Override
    protected void init() {
        super.init();
        MenusB173.trace("screen for menu {} shown ({}x{} at {},{})", menu.containerId, imageWidth, imageHeight, leftPos, topPos);
        // no screen of its own: the server, which made the original one, draws it (a dedicated server's trader screen)
        if (gui == null && minecraft != null && minecraft.getSingleplayerServer() == null) {
            var metrics = new int[256];
            for (int i = 0; i < metrics.length; i++) metrics[i] = font.width(Character.toString((char) i));
            String action = remoteRequested && remote != null ? "resize" : "attach";
            sendRemote(action, tag -> tag.putIntArray("metrics", metrics));
            remoteRequested = true;
        }
        if (!live()) return;
        try {
            gui.init(legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$instance(), width, height);
        } catch (RuntimeException | LinkageError e) {
            fail("initGui", e);
        }
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        if (live()) {
            ClientGuiB173.begin(g);
            try {
                gui.mcc$drewSlots = false;
                gui.render(mouseX, mouseY, partialTick);
            } catch (RuntimeException | LinkageError e) {
                fail("drawScreen", e);
            } finally {
                ClientGuiB173.end();
            }
        }
        if (!live()) {
            if (remote != null) ServerGuiScreenB173.draw(g, remote);
            else plainPanel(g);
        }
    }

    /** A vanilla-like panel with a frame for every slot. */
    private void plainPanel(GuiGraphics g) {
        int x = leftPos, y = topPos;
        g.fill(x, y, x + imageWidth, y + imageHeight, 0xFFC6C6C6);
        g.renderOutline(x, y, imageWidth, imageHeight, 0xFF555555);
        for (var slot : menu.slots) {
            g.fill(x + slot.x - 1, y + slot.y - 1, x + slot.x + 17, y + slot.y + 17, 0xFF8B8B8B);
            g.fill(x + slot.x, y + slot.y, x + slot.x + 16, y + slot.y + 16, 0xFF555555);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {}

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        lastMouseX = mouseX; lastMouseY = mouseY;
        if(live()){
            // AbstractContainerScreen snapshots its origin before renderBackground. Let the original
            // draw once and publish any late size change before native slots and hitboxes use that origin.
            var geometry=LegacyMenuGeometryB173.afterDraw(gui,width,height,
                    ()->super.renderBackground(g,mouseX,mouseY,partialTick));
            if(live()){
                imageWidth=geometry.width();imageHeight=geometry.height();
                leftPos=geometry.left();topPos=geometry.top();
            }
            backgroundPrepared=true;
        }
        try{
            super.render(g, mouseX, mouseY, partialTick);
            if (!ownSlots()) renderTooltip(g, mouseX, mouseY);
        }finally{backgroundPrepared=false;}
    }

    @Override public void renderBackground(GuiGraphics g,int mouseX,int mouseY,float partialTick){
        if(!backgroundPrepared)super.renderBackground(g,mouseX,mouseY,partialTick);
    }

    /**
     * A screen handed over from the integrated server edits the server player's legacy view on this thread (a trade takes coins and gives
     * goods through {@code player.inventory}). Beta had one player object; here the view's changes are written to the native player on the
     * server thread, bounded so a stalled server cannot hang the screen.
     */
    private static void commitPlayer() {
        var minecraft = net.minecraft.client.Minecraft.getInstance();
        var server = minecraft.getSingleplayerServer();
        if (server == null || minecraft.player == null) return;
        var id = minecraft.player.getUUID();
        try {
            server.submit(() -> {
                var player = server.getPlayerList().getPlayer(id);
                var view = player == null ? null : org.kylepitcock.legacyconnector.vb1_7_3.PlayerViewB173.existing(player);
                if (view != null) view.writeBack();
            }).get(2, java.util.concurrent.TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (java.util.concurrent.ExecutionException | java.util.concurrent.TimeoutException e) {
            org.slf4j.LoggerFactory.getLogger("LegacyConnector/b1.7.3").warn("b1.7.3 screen changes to the player could not be written back: {}", e.toString());
        }
    }

    @Override
    protected void containerTick() {
        if (remote != null) sendRemote("tick", tag -> {});
        if (!live()) return;
        try {
            gui.tick();
        } catch (RuntimeException | LinkageError e) {
            fail("updateScreen", e);
        }
    }

    /** Whether the original screen lets clicks reach its slots: null until a click shows it (a locked chest's screen swallows every click). */
    private Boolean slotsClickable;
    private boolean swallowedPress;

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        swallowedPress = false;
        if (remote != null) {
            // the server's screen decides: its own handler (a trade) or the slot click Beta's GuiContainer made, run on the server
            lastMouseX = (int) mouseX; lastMouseY = (int) mouseY;
            boolean shift = hasShiftDown();
            sendRemote("press", tag -> { tag.putInt("button", button); tag.putBoolean("shift", shift); });
            swallowedPress = true;
            return true;
        }
        if (live()) {
            try {
                boolean reached = gui.mcc$clickReachesSlots((int) mouseX, (int) mouseY, button);
                commitPlayer();
                slotsClickable = reached;
                // Beta clicked slots in GuiContainer.mouseClicked; a screen that never calls it keeps its slots shut
                if (!reached) { swallowedPress = true; return true; }
            } catch (RuntimeException | LinkageError e) {
                fail("mouseClicked", e);
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (swallowedPress) return true;
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    /** Slot keys (hotbar swap, drop) were not in Beta: they act only for a screen whose clicks reach its slots. */
    @Override
    protected void slotClicked(net.minecraft.world.inventory.Slot slot, int slotId, int button, net.minecraft.world.inventory.ClickType type) {
        if (remote != null || live() && !Boolean.TRUE.equals(slotsClickable)) return;
        super.slotClicked(slot, slotId, button, type);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (remote != null) {
            lastMouseX = (int) mouseX; lastMouseY = (int) mouseY;
            sendRemote("release", tag -> tag.putInt("button", button));
            swallowedPress = false;
            return true;
        }
        if (live()) {
            try {
                gui.mcc$mouseReleased((int) mouseX, (int) mouseY, button);
                commitPlayer();
            } catch (RuntimeException | LinkageError e) {
                fail("mouseMovedOrUp", e);
            }
        }
        if (swallowedPress) { swallowedPress = false; return true; }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (remote != null && !KeysB173.types(key, modifiers)) {
            char c = KeysB173.character(key, modifiers); int code = KeysB173.lwjgl(key);
            sendRemote("key", tag -> { tag.putInt("character", c); tag.putInt("key", code); });
        }
        if (live()) {
            if (KeysB173.types(key, modifiers)) {
                pendingKey = key;
            } else {
                legacyKey(KeysB173.character(key,modifiers), KeysB173.lwjgl(key));
            }
        }
        return super.keyPressed(key, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char c, int modifiers) {
        if (live()) {
            legacyKey(c, pendingKey < 0 ? 0 : KeysB173.lwjgl(pendingKey));
            pendingKey = -1;
            return true;
        }
        return super.charTyped(c, modifiers);
    }

    private void legacyKey(char c, int code) {
        legacyconnector.vb1_7_3.org.lwjgl.input.Keyboard.mcc$setEvent(code, true, c);
        try {
            gui.mcc$keyPressed(c, code);
            commitPlayer();
        } catch (RuntimeException | LinkageError e) {
            fail("keyTyped", e);
        } finally {
            legacyconnector.vb1_7_3.org.lwjgl.input.Keyboard.mcc$setEvent(0, false, '\u0000');
        }
    }

    @Override
    public void removed() {
        super.removed();
        if (!live()) return;
        try {
            gui.removed();
        } catch (RuntimeException | LinkageError e) {
            fail("onGuiClosed", e);
        }
    }
}
