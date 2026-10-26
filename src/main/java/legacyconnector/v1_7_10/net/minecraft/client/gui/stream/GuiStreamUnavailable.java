package legacyconnector.v1_7_10.net.minecraft.client.gui.stream;

import legacyconnector.v1_7_10.net.minecraft.client.gui.GuiButton;
import legacyconnector.v1_7_10.net.minecraft.client.gui.GuiScreen;
import legacyconnector.v1_7_10.net.minecraft.client.resources.I18n;

/** "Broadcasting unavailable": Twitch broadcasting does not exist any more, so this is what every stream button shows. */
public class GuiStreamUnavailable extends GuiScreen {
    private final GuiScreen parentScreen;
    private final String field_152325_g;

    public GuiStreamUnavailable(GuiScreen parent) {
        this.parentScreen = parent;
        this.field_152325_g = "Broadcasting is not available in this version of Minecraft.";
    }

    /** Shows why broadcasting cannot start (it never can now). */
    public static void func_152321_a(GuiScreen parent) {
        legacyconnector.v1_7_10.net.minecraft.client.Minecraft.getMinecraft().displayGuiScreen(new GuiStreamUnavailable(parent));
    }

    @Override
    public void initGui() {
        buttonList.add(new GuiButton(1, width / 2 - 100, height / 2 + 20, I18n.format("gui.done")));
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawCenteredString(fontRendererObj, I18n.format("stream.unavailable.title"), width / 2, height / 2 - 30, 0xFFFFFF);
        drawCenteredString(fontRendererObj, field_152325_g, width / 2, height / 2 - 10, 0xA0A0A0);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        mc.displayGuiScreen(parentScreen);
    }
}
