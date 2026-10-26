package legacyconnector.v1_7_10.cpw.mods.fml.client.config;

import static legacyconnector.v1_7_10.cpw.mods.fml.client.config.GuiConfigEntries.KEY_BACK;
import static legacyconnector.v1_7_10.cpw.mods.fml.client.config.GuiConfigEntries.KEY_DELETE;
import static legacyconnector.v1_7_10.cpw.mods.fml.client.config.GuiConfigEntries.KEY_END;
import static legacyconnector.v1_7_10.cpw.mods.fml.client.config.GuiConfigEntries.KEY_HOME;
import static legacyconnector.v1_7_10.cpw.mods.fml.client.config.GuiConfigEntries.KEY_LEFT;
import static legacyconnector.v1_7_10.cpw.mods.fml.client.config.GuiConfigEntries.KEY_RIGHT;
import static legacyconnector.v1_7_10.cpw.mods.fml.client.config.GuiConfigEntries.mcc$double;
import static legacyconnector.v1_7_10.cpw.mods.fml.client.config.GuiConfigEntries.mcc$int;
import static legacyconnector.v1_7_10.cpw.mods.fml.client.config.GuiUtils.INVALID;
import static legacyconnector.v1_7_10.cpw.mods.fml.client.config.GuiUtils.VALID;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import legacyconnector.v1_7_10.net.minecraft.client.Minecraft;
import legacyconnector.v1_7_10.net.minecraft.client.gui.GuiListExtended;
import legacyconnector.v1_7_10.net.minecraft.client.gui.GuiTextField;
import legacyconnector.v1_7_10.net.minecraft.client.renderer.Tessellator;
import legacyconnector.v1_7_10.net.minecraft.client.resources.I18n;
import legacyconnector.v1_7_10.net.minecraft.util.EnumChatFormatting;
import org.slf4j.LoggerFactory;

/** The rows of a {@link GuiEditArray}: one entry per item, plus a last row with only the "add" button for open-ended lists. */
public class GuiEditArrayEntries extends GuiListExtended {
    protected GuiEditArray owningGui;
    public IConfigElement configElement;
    public List<IArrayEntry> listEntries;
    public boolean isDefault;
    public boolean isChanged;
    public boolean canAddMoreEntries;
    public final int controlWidth;
    public final Object[] beforeValues;
    public Object[] currentValues;

    public GuiEditArrayEntries(GuiEditArray parent, Minecraft mc, IConfigElement configElement, Object[] beforeValues, Object[] currentValues) {
        super(mc, parent.width, parent.height, parent.titleLine2 != null ? (parent.titleLine3 != null ? 43 : 33) : 23, parent.height - 32, 20);
        this.owningGui = parent;
        this.configElement = configElement;
        this.beforeValues = beforeValues;
        this.currentValues = currentValues == null ? new Object[0] : currentValues;
        this.setShowSelectionBox(false);
        this.isChanged = !Arrays.deepEquals(beforeValues, this.currentValues);
        this.isDefault = Arrays.deepEquals(this.currentValues, configElement.getDefaults());
        this.canAddMoreEntries = !configElement.isListLengthFixed()
                && (configElement.getMaxListLength() == -1 || this.currentValues.length < configElement.getMaxListLength());
        this.listEntries = new ArrayList<>();
        this.controlWidth = parent.width / 2 - (configElement.isListLengthFixed() ? 0 : 48);

        if (configElement.isList() && configElement.getArrayEntryClass() != null) {
            Class<? extends IArrayEntry> clazz = configElement.getArrayEntryClass();
            for (Object value : this.currentValues) {
                try {
                    listEntries.add(clazz.getConstructor(GuiEditArray.class, GuiEditArrayEntries.class, IConfigElement.class, Object.class)
                            .newInstance(owningGui, this, configElement, value));
                } catch (Throwable e) {
                    LoggerFactory.getLogger("LegacyConnector/Gui").error("Could not create a list entry of {}", configElement.getName(), e);
                }
            }
        } else if (configElement.isList() && configElement.getType() == ConfigGuiType.BOOLEAN) {
            for (Object value : this.currentValues) listEntries.add(new BooleanEntry(owningGui, this, configElement, Boolean.valueOf(String.valueOf(value))));
        } else if (configElement.isList() && configElement.getType() == ConfigGuiType.INTEGER) {
            for (Object value : this.currentValues) listEntries.add(new IntegerEntry(owningGui, this, configElement, mcc$int(value, 0)));
        } else if (configElement.isList() && configElement.getType() == ConfigGuiType.DOUBLE) {
            for (Object value : this.currentValues) listEntries.add(new DoubleEntry(owningGui, this, configElement, mcc$double(value, 0)));
        } else if (configElement.isList()) {
            for (Object value : this.currentValues) listEntries.add(new StringEntry(owningGui, this, configElement, String.valueOf(value)));
        }
        if (!configElement.isListLengthFixed()) listEntries.add(new BaseEntry(owningGui, this, configElement));
    }

    @Override
    protected int getScrollBarX() {
        return width - width / 4;
    }

    @Override
    public int getListWidth() {
        return owningGui.width;
    }

    @Override
    public IGuiListEntry getListEntry(int index) {
        return listEntries.get(index);
    }

    @Override
    protected int getSize() {
        return listEntries.size();
    }

    public void addNewEntry(int index) {
        if (configElement.isList() && configElement.getType() == ConfigGuiType.BOOLEAN) listEntries.add(index, new BooleanEntry(owningGui, this, configElement, true));
        else if (configElement.isList() && configElement.getType() == ConfigGuiType.INTEGER) listEntries.add(index, new IntegerEntry(owningGui, this, configElement, 0));
        else if (configElement.isList() && configElement.getType() == ConfigGuiType.DOUBLE) listEntries.add(index, new DoubleEntry(owningGui, this, configElement, 0.0D));
        else if (configElement.isList()) listEntries.add(index, new StringEntry(owningGui, this, configElement, ""));
        updateCanAdd();
        keyTyped('\0', KEY_END);
    }

    public void removeEntry(int index) {
        listEntries.remove(index);
        updateCanAdd();
        keyTyped('\0', KEY_END);
    }

    private void updateCanAdd() {
        canAddMoreEntries = !configElement.isListLengthFixed()
                && (configElement.getMaxListLength() == -1 || listEntries.size() - 1 < configElement.getMaxListLength());
    }

    public boolean isChanged() {
        return isChanged;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void recalculateState() {
        isDefault = true;
        isChanged = false;
        int listLength = configElement.isListLengthFixed() ? listEntries.size() : listEntries.size() - 1;
        Object[] defaults = configElement.getDefaults();
        if (defaults == null || listLength != defaults.length) isDefault = false;
        if (beforeValues == null || listLength != beforeValues.length) isChanged = true;
        if (isDefault) {
            for (int i = 0; i < listLength; i++) {
                if (!String.valueOf(defaults[i]).equals(String.valueOf(listEntries.get(i).getValue()))) isDefault = false;
            }
        }
        if (!isChanged) {
            for (int i = 0; i < listLength; i++) {
                if (!String.valueOf(beforeValues[i]).equals(String.valueOf(listEntries.get(i).getValue()))) isChanged = true;
            }
        }
    }

    protected void keyTyped(char eventChar, int eventKey) {
        for (IArrayEntry entry : listEntries) entry.keyTyped(eventChar, eventKey);
        recalculateState();
    }

    protected void updateScreen() {
        for (IArrayEntry entry : listEntries) entry.updateCursorCounter();
    }

    protected void mouseClicked(int x, int y, int mouseEvent) {
        for (IArrayEntry entry : listEntries) entry.mouseClicked(x, y, mouseEvent);
    }

    protected boolean isListSavable() {
        for (IArrayEntry entry : listEntries) {
            if (!entry.isValueSavable()) return false;
        }
        return true;
    }

    /** Hands the edited list back to the config screen's entry, or writes it to the element when opened on its own. */
    @SuppressWarnings("unchecked")
    protected void saveListChanges() {
        int listLength = configElement.isListLengthFixed() ? listEntries.size() : listEntries.size() - 1;
        if (owningGui.slotIndex != -1 && owningGui.parentScreen instanceof GuiConfig config
                && config.entryList.getListEntry(owningGui.slotIndex) instanceof GuiConfigEntries.ArrayEntry entry) {
            Object[] ao = new Object[listLength];
            for (int i = 0; i < listLength; i++) ao[i] = listEntries.get(i).getValue();
            entry.setListFromChildScreen(ao);
        } else if (configElement.isList() && configElement.getType() == ConfigGuiType.BOOLEAN) {
            Boolean[] a = new Boolean[listLength];
            for (int i = 0; i < listLength; i++) a[i] = Boolean.valueOf(String.valueOf(listEntries.get(i).getValue()));
            configElement.set(a);
        } else if (configElement.isList() && configElement.getType() == ConfigGuiType.INTEGER) {
            Integer[] a = new Integer[listLength];
            for (int i = 0; i < listLength; i++) a[i] = mcc$int(listEntries.get(i).getValue(), 0);
            configElement.set(a);
        } else if (configElement.isList() && configElement.getType() == ConfigGuiType.DOUBLE) {
            Double[] a = new Double[listLength];
            for (int i = 0; i < listLength; i++) a[i] = mcc$double(listEntries.get(i).getValue(), 0);
            configElement.set(a);
        } else if (configElement.isList()) {
            String[] a = new String[listLength];
            for (int i = 0; i < listLength; i++) a[i] = String.valueOf(listEntries.get(i).getValue());
            configElement.set(a);
        }
    }

    protected void drawScreenPost(int mouseX, int mouseY, float partialTicks) {
        for (IArrayEntry entry : listEntries) entry.drawToolTip(mouseX, mouseY);
    }

    public Minecraft getMC() {
        return mc;
    }

    // ------------------------------------------------------------------ entries

    public static class DoubleEntry extends StringEntry {
        public DoubleEntry(GuiEditArray owningScreen, GuiEditArrayEntries owningEntryList, IConfigElement configElement, Double value) {
            super(owningScreen, owningEntryList, configElement, value);
            this.isValidated = true;
        }

        @Override
        public void keyTyped(char eventChar, int eventKey) {
            if (owningScreen.enabled || eventKey == KEY_LEFT || eventKey == KEY_RIGHT || eventKey == KEY_HOME || eventKey == KEY_END) {
                String before = textFieldValue.getText();
                if ((eventChar != 0 && "0123456789".indexOf(eventChar) >= 0)
                        || (!before.startsWith("-") && textFieldValue.getCursorPosition() == 0 && eventChar == '-')
                        || (!before.contains(".") && eventChar == '.')
                        || eventKey == KEY_BACK || eventKey == KEY_DELETE || eventKey == KEY_LEFT || eventKey == KEY_RIGHT || eventKey == KEY_HOME || eventKey == KEY_END) {
                    textFieldValue.textboxKeyTyped(owningScreen.enabled ? eventChar : '\0', eventKey);
                }
                String text = textFieldValue.getText().trim();
                if (!text.isEmpty() && !text.equals("-")) {
                    try {
                        double value = Double.parseDouble(text);
                        isValidValue = !(value < mcc$double(configElement.getMinValue(), -Double.MAX_VALUE)
                                || value > mcc$double(configElement.getMaxValue(), Double.MAX_VALUE));
                    } catch (Throwable e) {
                        isValidValue = false;
                    }
                } else {
                    isValidValue = false;
                }
            }
        }

        @Override
        public Double getValue() {
            try {
                return Double.valueOf(textFieldValue.getText().trim());
            } catch (Throwable e) {
                return Double.MAX_VALUE;
            }
        }
    }

    public static class IntegerEntry extends StringEntry {
        public IntegerEntry(GuiEditArray owningScreen, GuiEditArrayEntries owningEntryList, IConfigElement configElement, Integer value) {
            super(owningScreen, owningEntryList, configElement, value);
            this.isValidated = true;
        }

        @Override
        public void keyTyped(char eventChar, int eventKey) {
            if (owningScreen.enabled || eventKey == KEY_LEFT || eventKey == KEY_RIGHT || eventKey == KEY_HOME || eventKey == KEY_END) {
                String before = textFieldValue.getText();
                if ((eventChar != 0 && "0123456789".indexOf(eventChar) >= 0)
                        || (!before.startsWith("-") && textFieldValue.getCursorPosition() == 0 && eventChar == '-')
                        || eventKey == KEY_BACK || eventKey == KEY_DELETE || eventKey == KEY_LEFT || eventKey == KEY_RIGHT || eventKey == KEY_HOME || eventKey == KEY_END) {
                    textFieldValue.textboxKeyTyped(owningScreen.enabled ? eventChar : '\0', eventKey);
                }
                String text = textFieldValue.getText().trim();
                if (!text.isEmpty() && !text.equals("-")) {
                    try {
                        long value = Long.parseLong(text);
                        isValidValue = !(value < mcc$int(configElement.getMinValue(), Integer.MIN_VALUE) || value > mcc$int(configElement.getMaxValue(), Integer.MAX_VALUE));
                    } catch (Throwable e) {
                        isValidValue = false;
                    }
                } else {
                    isValidValue = false;
                }
            }
        }

        @Override
        public Integer getValue() {
            try {
                return Integer.valueOf(textFieldValue.getText().trim());
            } catch (Throwable e) {
                return Integer.MAX_VALUE;
            }
        }
    }

    public static class StringEntry extends BaseEntry {
        protected GuiTextField textFieldValue;

        public StringEntry(GuiEditArray owningScreen, GuiEditArrayEntries owningEntryList, IConfigElement configElement, Object value) {
            super(owningScreen, owningEntryList, configElement);
            this.textFieldValue = new GuiTextField(owningEntryList.getMC().fontRenderer, owningEntryList.width / 4 + 1, 0, owningEntryList.controlWidth - 3, 16);
            this.textFieldValue.setMaxStringLength(10000);
            this.textFieldValue.setText(String.valueOf(value));
            this.isValidated = configElement.getValidationPattern() != null;
            if (configElement.getValidationPattern() != null) {
                isValidValue = configElement.getValidationPattern().matcher(textFieldValue.getText().trim()).matches();
            }
        }

        @Override
        public void drawEntry(int slotIndex, int x, int y, int listWidth, int slotHeight, Tessellator tessellator, int mouseX, int mouseY, boolean isSelected) {
            super.drawEntry(slotIndex, x, y, listWidth, slotHeight, tessellator, mouseX, mouseY, isSelected);
            if (configElement.isListLengthFixed() || slotIndex != owningEntryList.listEntries.size() - 1) {
                textFieldValue.setVisible(true);
                textFieldValue.yPosition = y + 1;
                textFieldValue.drawTextBox();
            } else {
                textFieldValue.setVisible(false);
            }
        }

        @Override
        public void keyTyped(char eventChar, int eventKey) {
            if (owningScreen.enabled || eventKey == KEY_LEFT || eventKey == KEY_RIGHT || eventKey == KEY_HOME || eventKey == KEY_END) {
                textFieldValue.textboxKeyTyped(owningScreen.enabled ? eventChar : '\0', eventKey);
                if (configElement.getValidationPattern() != null) {
                    isValidValue = configElement.getValidationPattern().matcher(textFieldValue.getText().trim()).matches();
                }
            }
        }

        @Override
        public void updateCursorCounter() {
            textFieldValue.updateCursorCounter();
        }

        @Override
        public void mouseClicked(int x, int y, int mouseEvent) {
            textFieldValue.mouseClicked(x, y, mouseEvent);
        }

        @Override
        public Object getValue() {
            return textFieldValue.getText().trim();
        }
    }

    public static class BooleanEntry extends BaseEntry {
        protected final GuiButtonExt btnValue;
        private boolean value;

        public BooleanEntry(GuiEditArray owningScreen, GuiEditArrayEntries owningEntryList, IConfigElement configElement, boolean value) {
            super(owningScreen, owningEntryList, configElement);
            this.value = value;
            this.btnValue = new GuiButtonExt(0, 0, 0, owningEntryList.controlWidth, 18, I18n.format(String.valueOf(value)));
            this.btnValue.enabled = owningScreen.enabled;
            this.isValidated = false;
        }

        @Override
        public void drawEntry(int slotIndex, int x, int y, int listWidth, int slotHeight, Tessellator tessellator, int mouseX, int mouseY, boolean isSelected) {
            super.drawEntry(slotIndex, x, y, listWidth, slotHeight, tessellator, mouseX, mouseY, isSelected);
            btnValue.xPosition = listWidth / 4;
            btnValue.yPosition = y;
            String trans = I18n.format(String.valueOf(value));
            btnValue.displayString = !trans.equals(String.valueOf(value)) ? trans : String.valueOf(value);
            btnValue.packedFGColour = value ? GuiUtils.getColorCode('2', true) : GuiUtils.getColorCode('4', true);
            btnValue.drawButton(owningEntryList.getMC(), mouseX, mouseY);
        }

        @Override
        public boolean mousePressed(int index, int x, int y, int mouseEvent, int relativeX, int relativeY) {
            if (btnValue.mousePressed(owningEntryList.getMC(), x, y)) {
                btnValue.playPressSound(owningEntryList.getMC().getSoundHandler());
                value = !value;
                owningEntryList.recalculateState();
                return true;
            }
            return super.mousePressed(index, x, y, mouseEvent, relativeX, relativeY);
        }

        @Override
        public void mouseReleased(int index, int x, int y, int mouseEvent, int relativeX, int relativeY) {
            btnValue.mouseReleased(x, y);
            super.mouseReleased(index, x, y, mouseEvent, relativeX, relativeY);
        }

        @Override
        public Object getValue() {
            return value;
        }
    }

    /** A row's "add above" and "remove" buttons and validity mark. */
    public static class BaseEntry implements IArrayEntry {
        protected final GuiEditArray owningScreen;
        protected final GuiEditArrayEntries owningEntryList;
        protected final IConfigElement configElement;
        protected final GuiButtonExt btnAddNewEntryAbove;
        private final HoverChecker addNewEntryAboveHoverChecker;
        protected final GuiButtonExt btnRemoveEntry;
        private final HoverChecker removeEntryHoverChecker;
        private final List addNewToolTip;
        private final List removeToolTip;
        protected boolean isValidValue = true;
        protected boolean isValidated = false;

        @SuppressWarnings("unchecked")
        public BaseEntry(GuiEditArray owningScreen, GuiEditArrayEntries owningEntryList, IConfigElement configElement) {
            this.owningScreen = owningScreen;
            this.owningEntryList = owningEntryList;
            this.configElement = configElement;
            this.btnAddNewEntryAbove = new GuiButtonExt(0, 0, 0, 18, 18, "+");
            this.btnAddNewEntryAbove.packedFGColour = GuiUtils.getColorCode('2', true);
            this.btnAddNewEntryAbove.enabled = owningScreen.enabled;
            this.btnRemoveEntry = new GuiButtonExt(0, 0, 0, 18, 18, "x");
            this.btnRemoveEntry.packedFGColour = GuiUtils.getColorCode('c', true);
            this.btnRemoveEntry.enabled = owningScreen.enabled;
            this.addNewEntryAboveHoverChecker = new HoverChecker(btnAddNewEntryAbove, 800);
            this.removeEntryHoverChecker = new HoverChecker(btnRemoveEntry, 800);
            this.addNewToolTip = new ArrayList();
            this.removeToolTip = new ArrayList();
            addNewToolTip.add(I18n.format("fml.configgui.tooltip.addNewEntryAbove"));
            removeToolTip.add(I18n.format("fml.configgui.tooltip.removeEntry"));
        }

        @Override
        public void drawEntry(int slotIndex, int x, int y, int listWidth, int slotHeight, Tessellator tessellator, int mouseX, int mouseY, boolean isSelected) {
            var font = owningEntryList.getMC().fontRenderer;
            if (getValue() != null && isValidated) {
                font.drawString(isValidValue ? EnumChatFormatting.GREEN + VALID : EnumChatFormatting.RED + INVALID, listWidth / 4 - font.getStringWidth(VALID) - 2,
                        y + slotHeight / 2 - font.FONT_HEIGHT / 2, 16777215);
            }
            int half = listWidth / 2;
            if (owningEntryList.canAddMoreEntries) {
                btnAddNewEntryAbove.visible = true;
                btnAddNewEntryAbove.xPosition = half + (half / 2 - 44);
                btnAddNewEntryAbove.yPosition = y;
                btnAddNewEntryAbove.drawButton(owningEntryList.getMC(), mouseX, mouseY);
            } else {
                btnAddNewEntryAbove.visible = false;
            }
            if (!configElement.isListLengthFixed() && slotIndex != owningEntryList.listEntries.size() - 1) {
                btnRemoveEntry.visible = true;
                btnRemoveEntry.xPosition = half + (half / 2 - 22);
                btnRemoveEntry.yPosition = y;
                btnRemoveEntry.drawButton(owningEntryList.getMC(), mouseX, mouseY);
            } else {
                btnRemoveEntry.visible = false;
            }
        }

        @Override
        public void drawToolTip(int mouseX, int mouseY) {
            boolean canHover = mouseY < owningEntryList.bottom && mouseY > owningEntryList.top;
            if (btnAddNewEntryAbove.visible && addNewEntryAboveHoverChecker.checkHover(mouseX, mouseY, canHover)) owningScreen.drawToolTip(addNewToolTip, mouseX, mouseY);
            if (btnRemoveEntry.visible && removeEntryHoverChecker.checkHover(mouseX, mouseY, canHover)) owningScreen.drawToolTip(removeToolTip, mouseX, mouseY);
        }

        @Override
        public boolean mousePressed(int index, int x, int y, int mouseEvent, int relativeX, int relativeY) {
            if (btnAddNewEntryAbove.mousePressed(owningEntryList.getMC(), x, y)) {
                btnAddNewEntryAbove.playPressSound(owningEntryList.getMC().getSoundHandler());
                owningEntryList.addNewEntry(index);
                owningEntryList.recalculateState();
                return true;
            } else if (btnRemoveEntry.mousePressed(owningEntryList.getMC(), x, y)) {
                btnRemoveEntry.playPressSound(owningEntryList.getMC().getSoundHandler());
                owningEntryList.removeEntry(index);
                owningEntryList.recalculateState();
                return true;
            }
            return false;
        }

        @Override
        public void mouseReleased(int index, int x, int y, int mouseEvent, int relativeX, int relativeY) {
            btnAddNewEntryAbove.mouseReleased(x, y);
            btnRemoveEntry.mouseReleased(x, y);
        }

        @Override
        public void keyTyped(char eventChar, int eventKey) {
        }

        @Override
        public void updateCursorCounter() {
        }

        @Override
        public void mouseClicked(int x, int y, int mouseEvent) {
        }

        @Override
        public boolean isValueSavable() {
            return isValidValue;
        }

        @Override
        public Object getValue() {
            return null;
        }
    }

    public interface IArrayEntry extends GuiListExtended.IGuiListEntry {
        void keyTyped(char eventChar, int eventKey);

        void updateCursorCounter();

        void mouseClicked(int x, int y, int mouseEvent);

        void drawToolTip(int mouseX, int mouseY);

        boolean isValueSavable();

        Object getValue();
    }
}
