package legacyconnector.v1_12_2.net.minecraft.client.gui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import legacyconnector.v1_12_2.net.minecraft.client.resources.I18n;

/** A question with two buttons; the answer goes to the callback's {@code confirmClicked(result, id)}. */
public class GuiYesNo extends GuiScreen {
    protected GuiYesNoCallback parentScreen;
    protected String messageLine1;
    private final String messageLine2;
    private final List<String> listLines = new ArrayList<>();
    protected String confirmButtonText;
    protected String cancelButtonText;
    protected int parentButtonClickedId;
    private int ticksUntilEnable;

    public GuiYesNo(GuiYesNoCallback parentScreenIn, String messageLine1In, String messageLine2In, int parentButtonClickedIdIn) {
        this(parentScreenIn, messageLine1In, messageLine2In, I18n.format("gui.yes"), I18n.format("gui.no"), parentButtonClickedIdIn);
    }

    public GuiYesNo(GuiYesNoCallback parentScreenIn, String messageLine1In, String messageLine2In, String confirmButtonTextIn, String cancelButtonTextIn,
            int parentButtonClickedIdIn) {
        this.parentScreen = parentScreenIn;
        this.messageLine1 = messageLine1In;
        this.messageLine2 = messageLine2In;
        this.confirmButtonText = confirmButtonTextIn;
        this.cancelButtonText = cancelButtonTextIn;
        this.parentButtonClickedId = parentButtonClickedIdIn;
    }

    @Override
    public void initGui() {
        buttonList.add(new GuiOptionButton(0, width / 2 - 155, height / 6 + 96, confirmButtonText));
        buttonList.add(new GuiOptionButton(1, width / 2 - 155 + 160, height / 6 + 96, cancelButtonText));
        listLines.clear();
        listLines.addAll(fontRenderer.listFormattedStringToWidth(messageLine2 == null ? "" : messageLine2, width - 50));
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (parentScreen != null) parentScreen.confirmClicked(button.id == 0, parentButtonClickedId);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawCenteredString(fontRenderer, messageLine1, width / 2, 70, 16777215);
        int i = 90;
        for (String s : listLines) {
            drawCenteredString(fontRenderer, s, width / 2, i, 16777215);
            i += fontRenderer.FONT_HEIGHT;
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    public void setButtonDelay(int ticksUntilEnableIn) {
        this.ticksUntilEnable = ticksUntilEnableIn;
        for (GuiButton b : buttonList) b.enabled = false;
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (--ticksUntilEnable == 0) for (GuiButton b : buttonList) b.enabled = true;
    }
}
