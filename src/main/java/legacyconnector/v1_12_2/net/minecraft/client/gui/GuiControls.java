package legacyconnector.v1_12_2.net.minecraft.client.gui;

import java.io.IOException;
import legacyconnector.v1_12_2.net.minecraft.client.Minecraft;
import legacyconnector.v1_12_2.net.minecraft.client.resources.I18n;
import legacyconnector.v1_12_2.net.minecraft.client.settings.GameSettings;
import legacyconnector.v1_12_2.net.minecraft.client.settings.KeyBinding;
import legacyconnector.v1_12_2.net.minecraftforge.client.settings.KeyModifier;

/**
 * 1.12.2's controls screen (Forge's version: key modifiers). Displaying this class itself shows the modern controls screen, which is
 * announced to mods as one of these; mods' subclasses run this screen: the key binding list, Done and Reset All. The mouse option
 * buttons of 1.12 are on the modern mouse settings screen.
 */
public class GuiControls extends GuiScreen {
    protected GuiScreen parentScreen;
    protected String screenTitle = "Controls";
    private final GameSettings options;
    public KeyBinding buttonId;
    public long time;
    private GuiKeyBindingList keyBindingList;
    private GuiButton buttonReset;

    public GuiControls(GuiScreen screen, GameSettings settings) {
        this.parentScreen = screen;
        this.options = settings;
    }

    public GuiScreen mcc$parent() {
        return parentScreen;
    }

    private KeyBinding[] keys() {
        GameSettings s = options != null ? options : Minecraft.mcc$get$gameSettings(mc);
        KeyBinding[] keys = GameSettings.mcc$get$keyBindings(s);
        return keys == null ? new KeyBinding[0] : keys;
    }

    @Override
    public void initGui() {
        keyBindingList = new GuiKeyBindingList(this, mc);
        buttonList.add(new GuiButton(200, width / 2 - 155 + 160, height - 29, 150, 20, I18n.format("gui.done")));
        buttonReset = addButton(new GuiButton(201, width / 2 - 155, height - 29, 150, 20, I18n.format("controls.resetAll")));
        screenTitle = I18n.format("controls.title");
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        if (keyBindingList != null) keyBindingList.handleMouseInput();
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 200) {
            mc.displayGuiScreen(parentScreen);
        } else if (button.id == 201) {
            for (KeyBinding k : keys()) k.setToDefault();
            KeyBinding.resetKeyBindingArrayAndHash();
        }
    }

    private void bind(KeyModifier modifier, int keyCode) {
        buttonId.setKeyModifierAndCode(modifier, keyCode);
        GameSettings s = options != null ? options : Minecraft.mcc$get$gameSettings(mc);
        s.setOptionKeyBinding(buttonId, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (buttonId != null) {
            bind(KeyModifier.getActiveModifier(), -100 + mouseButton);
            buttonId = null;
            KeyBinding.resetKeyBindingArrayAndHash();
        } else if (mouseButton != 0 || keyBindingList == null || !keyBindingList.mouseClicked(mouseX, mouseY, mouseButton)) {
            super.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        if (state != 0 || keyBindingList == null || !keyBindingList.mouseReleased(mouseX, mouseY, state)) super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (buttonId != null) {
            if (keyCode == 1) bind(KeyModifier.NONE, 0);
            else if (keyCode != 0) bind(KeyModifier.getActiveModifier(), keyCode);
            else if (typedChar > 0) bind(KeyModifier.getActiveModifier(), typedChar + 256);
            if (!KeyModifier.isKeyCodeModifier(keyCode)) buttonId = null;
            time = Minecraft.getSystemTime();
            KeyBinding.resetKeyBindingArrayAndHash();
        } else {
            super.keyTyped(typedChar, keyCode);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        if (keyBindingList != null) keyBindingList.drawScreen(mouseX, mouseY, partialTicks);
        drawCenteredString(fontRenderer, screenTitle, width / 2, 8, 16777215);
        boolean changed = false;
        for (KeyBinding k : keys()) {
            if (!k.isSetToDefaultValue()) {
                changed = true;
                break;
            }
        }
        if (buttonReset != null) buttonReset.enabled = changed;
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}
