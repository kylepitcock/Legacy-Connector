package legacyconnector.vb1_7_3.net.minecraft.client.gui.screen.ingame;

import legacyconnector.vb1_7_3.net.minecraft.client.gui.screen.Screen;
import legacyconnector.vb1_7_3.net.minecraft.screen.ScreenHandler;
import org.kylepitcock.legacyconnector.vb1_7_3.GuiDrawB173;

/**
 * Beta 1.7.3 {@code GuiContainer}. Its container is hosted by a modern menu ({@code LegacyB173Menu}) and the modern container screen draws
 * the slots, their items, the carried stack and tooltips and handles the clicks; this class draws what the mod draws: the background
 * ({@link #drawBackground}), the foreground relative to the GUI's corner ({@link #drawForeground}) and buttons.
 */
public abstract class HandledScreen extends Screen {
    protected int backgroundWidth = 176;
    protected int backgroundHeight = 166;
    public ScreenHandler container;

    public HandledScreen(ScreenHandler container) {
        this.container = container;
    }

    @Override
    public void init() {
        super.init();
    }

    /** MCP {@code drawScreen} without the slots (the modern screen draws those over it). */
    @Override
    public void render(int mouseX, int mouseY, float delta) {
        renderBackground();
        int x = (width - backgroundWidth) / 2;
        int y = (height - backgroundHeight) / 2;
        drawBackground(delta);
        GuiDrawB173.push();
        GuiDrawB173.translate(x, y, 0);
        try {
            drawForeground();
        } finally {
            GuiDrawB173.pop();
        }
        super.render(mouseX, mouseY, delta);
    }

    /** MCP {@code drawGuiContainerForegroundLayer}: drawn relative to the GUI's top left corner. */
    protected void drawForeground() {}

    /** MCP {@code drawGuiContainerBackgroundLayer}: drawn in screen coordinates. */
    protected abstract void drawBackground(float delta);

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) {
        super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int button) {
        super.mouseReleased(mouseX, mouseY, button);
    }

    /** Escape and the inventory key close the modern screen itself. */
    @Override
    protected void keyPressed(char character, int keyCode) {}

    @Override
    public void removed() {}

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
    }

    public int mcc$backgroundWidth() {
        return backgroundWidth;
    }

    public int mcc$backgroundHeight() {
        return backgroundHeight;
    }
}
