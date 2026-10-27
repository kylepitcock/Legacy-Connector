package legacyconnector.v1_12_2.net.minecraft.client.gui;

import legacyconnector.v1_12_2.net.minecraft.client.Minecraft;
import legacyconnector.v1_12_2.net.minecraft.client.resources.I18n;

/** 1.12.2's yes/no toggle of the world customisation pages: "caption: Yes/No", each click flips it and tells the responder. */
public class GuiListButton extends GuiButton {
    private boolean value;
    private final String localizationStr;
    private final GuiPageButtonList.GuiResponder guiResponder;

    public GuiListButton(GuiPageButtonList.GuiResponder responder, int buttonId, int x, int y, String localizationStrIn, boolean valueIn) {
        super(buttonId, x, y, 150, 20, "");
        this.localizationStr = localizationStrIn;
        this.value = valueIn;
        this.displayString = buildDisplayString();
        this.guiResponder = responder;
    }

    private String buildDisplayString() {
        return I18n.format(localizationStr) + ": " + I18n.format(value ? "gui.yes" : "gui.no");
    }

    public void setValue(boolean valueIn) {
        value = valueIn;
        displayString = buildDisplayString();
        if (guiResponder != null) guiResponder.setEntryValue(id, valueIn);
    }

    @Override
    public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
        if (!super.mousePressed(mc, mouseX, mouseY)) return false;
        value = !value;
        displayString = buildDisplayString();
        if (guiResponder != null) guiResponder.setEntryValue(id, value);
        return true;
    }
}
