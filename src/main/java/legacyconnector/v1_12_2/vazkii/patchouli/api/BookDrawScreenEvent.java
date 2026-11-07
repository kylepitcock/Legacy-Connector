package legacyconnector.v1_12_2.vazkii.patchouli.api;

import legacyconnector.v1_12_2.net.minecraft.client.gui.GuiScreen;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/** Posted (client side, on the legacy event bus) after a book screen was drawn (Patchouli 1.12 API). */
public class BookDrawScreenEvent extends BookEvent {
    public final GuiScreen gui;
    public final int mouseX;
    public final int mouseY;
    public final float partialTicks;

    public BookDrawScreenEvent(GuiScreen gui, ResourceLocation book, int mouseX, int mouseY, float partialTicks) {
        super(book);
        this.gui = gui;
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        this.partialTicks = partialTicks;
    }
}
