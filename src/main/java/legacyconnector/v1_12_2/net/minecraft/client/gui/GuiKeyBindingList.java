package legacyconnector.v1_12_2.net.minecraft.client.gui;

import java.util.Arrays;
import legacyconnector.v1_12_2.net.minecraft.client.Minecraft;
import legacyconnector.v1_12_2.net.minecraft.client.resources.I18n;
import legacyconnector.v1_12_2.net.minecraft.client.settings.GameSettings;
import legacyconnector.v1_12_2.net.minecraft.client.settings.KeyBinding;
import legacyconnector.v1_12_2.net.minecraft.util.text.TextFormatting;

/** The key binding list of {@link GuiControls}: a header per category, then each binding with its key button and a reset button. */
public class GuiKeyBindingList extends GuiListExtended {
    private final GuiControls controlsScreen;
    private final Minecraft mc;
    private final GuiListExtended.IGuiListEntry[] listEntries;
    private int maxListLabelWidth;

    public GuiKeyBindingList(GuiControls controls, Minecraft mcIn) {
        super(mcIn, controls.width + 45, controls.height, 63, controls.height - 32, 20);
        this.controlsScreen = controls;
        this.mc = mcIn;
        KeyBinding[] keys = GameSettings.mcc$get$keyBindings(Minecraft.mcc$get$gameSettings(mcIn));
        keys = keys == null ? new KeyBinding[0] : keys.clone();
        Arrays.sort(keys);
        java.util.List<IGuiListEntry> entries = new java.util.ArrayList<>();
        String category = null;
        FontRenderer font = font();
        for (KeyBinding k : keys) {
            String c = k.getKeyCategory();
            if (!c.equals(category)) {
                category = c;
                entries.add(new CategoryEntry(c));
            }
            int w = font.getStringWidth(I18n.format(k.getKeyDescription()));
            if (w > maxListLabelWidth) maxListLabelWidth = w;
            entries.add(new KeyEntry(k));
        }
        this.listEntries = entries.toArray(new IGuiListEntry[0]);
    }

    private FontRenderer font() {
        return mc != null && mc.fontRenderer != null ? mc.fontRenderer : FontRenderer.mcc$INSTANCE;
    }

    @Override
    protected int getSize() {
        return listEntries.length;
    }

    @Override
    public IGuiListEntry getListEntry(int index) {
        return listEntries[index];
    }

    @Override
    protected int getScrollBarX() {
        return super.getScrollBarX() + 15;
    }

    @Override
    public int getListWidth() {
        return super.getListWidth() + 32;
    }

    public class CategoryEntry implements IGuiListEntry {
        private final String labelText;
        private final int labelWidth;

        public CategoryEntry(String name) {
            this.labelText = I18n.format(name);
            this.labelWidth = font().getStringWidth(labelText);
        }

        @Override
        public void drawEntry(int slotIndex, int x, int y, int listWidth, int slotHeight, int mouseX, int mouseY, boolean isSelected, float partialTicks) {
            font().drawString(labelText, controlsScreen.width / 2 - labelWidth / 2, y + slotHeight - font().FONT_HEIGHT - 1, 16777215);
        }

        @Override
        public boolean mousePressed(int slotIndex, int mouseX, int mouseY, int mouseEvent, int relativeX, int relativeY) {
            return false;
        }

        @Override
        public void mouseReleased(int slotIndex, int x, int y, int mouseEvent, int relativeX, int relativeY) {
        }

        @Override
        public void updatePosition(int slotIndex, int x, int y, float partialTicks) {
        }
    }

    public class KeyEntry implements IGuiListEntry {
        private final KeyBinding keybinding;
        private final String keyDesc;
        private final GuiButton btnChangeKeyBinding;
        private final GuiButton btnReset;

        private KeyEntry(KeyBinding name) {
            this.keybinding = name;
            this.keyDesc = I18n.format(name.getKeyDescription());
            this.btnChangeKeyBinding = new GuiButton(0, 0, 0, 95, 20, I18n.format(name.getKeyDescription()));
            this.btnReset = new GuiButton(0, 0, 0, 50, 20, I18n.format("controls.reset"));
        }

        @Override
        public void drawEntry(int slotIndex, int x, int y, int listWidth, int slotHeight, int mouseX, int mouseY, boolean isSelected, float partialTicks) {
            boolean editing = controlsScreen.buttonId == keybinding;
            font().drawString(keyDesc, x + 90 - maxListLabelWidth, y + slotHeight / 2 - font().FONT_HEIGHT / 2, 16777215);
            btnReset.x = x + 210;
            btnReset.y = y;
            btnReset.enabled = !keybinding.isSetToDefaultValue();
            btnReset.drawButton(mc, mouseX, mouseY, partialTicks);
            btnChangeKeyBinding.x = x + 105;
            btnChangeKeyBinding.y = y;
            btnChangeKeyBinding.displayString = keybinding.getDisplayName();
            boolean conflict = false;
            boolean modifierConflict = true;
            if (keybinding.getKeyCode() != 0) {
                KeyBinding[] all = GameSettings.mcc$get$keyBindings(Minecraft.mcc$get$gameSettings(mc));
                if (all != null) {
                    for (KeyBinding other : all) {
                        if (other != keybinding && other.conflicts(keybinding)) {
                            conflict = true;
                            modifierConflict &= other.hasKeyCodeModifierConflict(keybinding);
                        }
                    }
                }
            }
            if (editing) {
                btnChangeKeyBinding.displayString = TextFormatting.WHITE + "> " + TextFormatting.YELLOW + btnChangeKeyBinding.displayString + TextFormatting.WHITE + " <";
            } else if (conflict) {
                btnChangeKeyBinding.displayString = (modifierConflict ? TextFormatting.GOLD : TextFormatting.RED) + btnChangeKeyBinding.displayString;
            }
            btnChangeKeyBinding.drawButton(mc, mouseX, mouseY, partialTicks);
        }

        @Override
        public boolean mousePressed(int slotIndex, int mouseX, int mouseY, int mouseEvent, int relativeX, int relativeY) {
            if (btnChangeKeyBinding.mousePressed(mc, mouseX, mouseY)) {
                controlsScreen.buttonId = keybinding;
                return true;
            }
            if (btnReset.mousePressed(mc, mouseX, mouseY)) {
                keybinding.setToDefault();
                Minecraft.mcc$get$gameSettings(mc).setOptionKeyBinding(keybinding, keybinding.getKeyCodeDefault());
                KeyBinding.resetKeyBindingArrayAndHash();
                return true;
            }
            return false;
        }

        @Override
        public void mouseReleased(int slotIndex, int x, int y, int mouseEvent, int relativeX, int relativeY) {
            btnChangeKeyBinding.mouseReleased(x, y);
            btnReset.mouseReleased(x, y);
        }

        @Override
        public void updatePosition(int slotIndex, int x, int y, float partialTicks) {
        }
    }
}
