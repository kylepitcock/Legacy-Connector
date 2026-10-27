package legacyconnector.v1_12_2.net.minecraftforge.fml.client.config;

import static legacyconnector.v1_12_2.net.minecraftforge.fml.client.config.GuiUtils.RESET_CHAR;
import static legacyconnector.v1_12_2.net.minecraftforge.fml.client.config.GuiUtils.UNDO_CHAR;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.Loader;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.ModContainer;
import legacyconnector.v1_12_2.net.minecraft.client.Minecraft;
import legacyconnector.v1_12_2.net.minecraft.client.gui.GuiListExtended;
import legacyconnector.v1_12_2.net.minecraft.client.gui.GuiScreen;
import legacyconnector.v1_12_2.net.minecraft.client.gui.GuiTextField;
import legacyconnector.v1_12_2.net.minecraft.client.resources.I18n;
import legacyconnector.v1_12_2.net.minecraft.util.text.TextFormatting;
import org.slf4j.LoggerFactory;

/**
 * The list of a {@link GuiConfig} screen (Forge 1.7.10): one entry per config element, with a control that edits its value (a
 * toggle, a cycling or selecting button, a slider, a text field, a list editor or a sub-screen) and undo / reset buttons.
 */
public class GuiConfigEntries extends GuiListExtended {
    static final int KEY_BACK = 14;
    static final int KEY_HOME = 199;
    static final int KEY_LEFT = 203;
    static final int KEY_RIGHT = 205;
    static final int KEY_END = 207;
    static final int KEY_DELETE = 211;

    public final GuiConfig owningScreen;
    public final Minecraft mc;
    public List<IConfigEntry> listEntries;
    /** The width of the widest label; the controls start after it. */
    public int maxLabelTextWidth = 0;
    public int maxEntryRightBound = 0;
    public int labelX;
    public int controlX;
    public int controlWidth;
    public int resetX;
    public int scrollBarX;

    @SuppressWarnings("unchecked")
    public GuiConfigEntries(GuiConfig parent, Minecraft mc) {
        super(mc, parent.width, parent.height, parent.titleLine2 != null ? 33 : 23, parent.height - 32, 20);
        this.owningScreen = parent;
        this.setShowSelectionBox(false);
        this.mc = mc;
        this.listEntries = new ArrayList<>();
        for (IConfigElement configElement : parent.configElements) {
            if (configElement != null && configElement.isProperty() && configElement.showInGui()) {
                String key = configElement.getLanguageKey();
                int length = key != null && !I18n.format(key).equals(key) ? mc.fontRenderer.getStringWidth(I18n.format(key))
                        : mc.fontRenderer.getStringWidth(configElement.getName());
                if (length > maxLabelTextWidth) maxLabelTextWidth = length;
            }
        }
        int viewWidth = maxLabelTextWidth + 8 + width / 2;
        labelX = width / 2 - viewWidth / 2;
        controlX = labelX + maxLabelTextWidth + 8;
        resetX = width / 2 + viewWidth / 2 - 45;
        controlWidth = resetX - controlX - 5;
        scrollBarX = width;

        for (IConfigElement configElement : parent.configElements) {
            if (configElement == null || !configElement.showInGui()) continue;
            try {
                IConfigEntry entry = createEntry(configElement);
                if (entry != null) listEntries.add(entry);
            } catch (Throwable e) {
                LoggerFactory.getLogger("LegacyConnector/Gui").error("Could not create the config entry for {}", configElement.getName(), e);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private IConfigEntry createEntry(IConfigElement configElement) throws ReflectiveOperationException {
        if (configElement.getConfigEntryClass() != null) {
            return (IConfigEntry) configElement.getConfigEntryClass().getConstructor(GuiConfig.class, GuiConfigEntries.class, IConfigElement.class)
                    .newInstance(owningScreen, this, configElement);
        }
        if (configElement.isProperty()) {
            if (configElement.isList()) return new ArrayEntry(owningScreen, this, configElement);
            ConfigGuiType type = configElement.getType();
            if (type == ConfigGuiType.BOOLEAN) return new BooleanEntry(owningScreen, this, configElement);
            if (type == ConfigGuiType.INTEGER) return new IntegerEntry(owningScreen, this, configElement);
            if (type == ConfigGuiType.DOUBLE) return new DoubleEntry(owningScreen, this, configElement);
            if (type == ConfigGuiType.COLOR) {
                String[] valid = configElement.getValidValues();
                return valid != null && valid.length > 0 ? new ChatColorEntry(owningScreen, this, configElement) : new StringEntry(owningScreen, this, configElement);
            }
            if (type == ConfigGuiType.MOD_ID) {
                Map<Object, String> values = new TreeMap<>();
                for (Object o : Loader.instance().getActiveModList()) {
                    ModContainer mod = (ModContainer) o;
                    values.put(mod.getModId(), mod.getName());
                }
                values.put("minecraft", "Minecraft");
                return new SelectValueEntry(owningScreen, this, configElement, values);
            }
            if (type == ConfigGuiType.STRING) {
                String[] valid = configElement.getValidValues();
                return valid != null && valid.length > 0 ? new CycleValueEntry(owningScreen, this, configElement) : new StringEntry(owningScreen, this, configElement);
            }
            return null;
        }
        return configElement.getType() == ConfigGuiType.CONFIG_CATEGORY ? new CategoryEntry(owningScreen, this, configElement) : null;
    }

    protected void initGui() {
        this.width = owningScreen.width;
        this.height = owningScreen.height;
        this.maxLabelTextWidth = 0;
        for (IConfigEntry entry : listEntries) maxLabelTextWidth = Math.max(maxLabelTextWidth, entry.getLabelWidth());
        this.top = owningScreen.titleLine2 != null ? 33 : 23;
        this.bottom = owningScreen.height - 32;
        this.left = 0;
        this.right = width;
        int viewWidth = maxLabelTextWidth + 8 + width / 2;
        labelX = width / 2 - viewWidth / 2;
        controlX = labelX + maxLabelTextWidth + 8;
        resetX = width / 2 + viewWidth / 2 - 45;
        this.maxEntryRightBound = 0;
        for (IConfigEntry entry : listEntries) maxEntryRightBound = Math.max(maxEntryRightBound, entry.getEntryRightBound());
        scrollBarX = maxEntryRightBound + 5;
        controlWidth = maxEntryRightBound - controlX - 45;
    }

    @Override
    public int getSize() {
        return listEntries.size();
    }

    @Override
    public IConfigEntry getListEntry(int index) {
        return listEntries.get(index);
    }

    @Override
    public int getScrollBarX() {
        return scrollBarX;
    }

    @Override
    public int getListWidth() {
        return owningScreen.width;
    }

    public void keyTyped(char eventChar, int eventKey) {
        for (IConfigEntry entry : listEntries) entry.keyTyped(eventChar, eventKey);
    }

    public void updateScreen() {
        for (IConfigEntry entry : listEntries) entry.updateCursorCounter();
    }

    public void mouseClickedPassThru(int mouseX, int mouseY, int mouseEvent) {
        for (IConfigEntry entry : listEntries) entry.mouseClicked(mouseX, mouseY, mouseEvent);
    }

    /** Writes every changed entry to its element; true if one of them needs a game restart. */
    public boolean saveConfigElements() {
        boolean requiresRestart = false;
        for (IConfigEntry entry : listEntries) {
            if (entry.saveConfigElement()) requiresRestart = true;
        }
        return requiresRestart;
    }

    public boolean areAllEntriesDefault(boolean includeChildren) {
        for (IConfigEntry entry : listEntries) {
            if ((includeChildren || !(entry instanceof CategoryEntry)) && !entry.isDefault()) return false;
        }
        return true;
    }

    public void setAllToDefault(boolean includeChildren) {
        for (IConfigEntry entry : listEntries) {
            if (includeChildren || !(entry instanceof CategoryEntry)) entry.setToDefault();
        }
    }

    public boolean hasChangedEntry(boolean includeChildren) {
        for (IConfigEntry entry : listEntries) {
            if ((includeChildren || !(entry instanceof CategoryEntry)) && entry.isChanged()) return true;
        }
        return false;
    }

    public boolean areAnyEntriesEnabled(boolean includeChildren) {
        for (IConfigEntry entry : listEntries) {
            if ((includeChildren || !(entry instanceof CategoryEntry)) && entry.enabled()) return true;
        }
        return false;
    }

    public void undoAllChanges(boolean includeChildren) {
        for (IConfigEntry entry : listEntries) {
            if (includeChildren || !(entry instanceof CategoryEntry)) entry.undoChanges();
        }
    }

    public void onGuiClosed() {
        for (IConfigEntry entry : listEntries) entry.onGuiClosed();
    }

    public void drawScreenPost(int mouseX, int mouseY, float partialTicks) {
        for (IConfigEntry entry : listEntries) entry.drawToolTip(mouseX, mouseY);
    }

    static int mcc$int(Object o, int fallback) {
        try {
            return o == null ? fallback : Integer.parseInt(o.toString().trim());
        } catch (NumberFormatException e) {
            try {
                return (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, Double.parseDouble(o.toString().trim())));
            } catch (NumberFormatException e2) {
                return fallback;
            }
        }
    }

    static double mcc$double(Object o, double fallback) {
        try {
            return o == null ? fallback : Double.parseDouble(o.toString().trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    // ------------------------------------------------------------------ entries

    /** A plain on/off toggle. */
    public static class BooleanEntry extends ButtonEntry {
        protected final boolean beforeValue;
        protected boolean currentValue;

        public BooleanEntry(GuiConfig owningScreen, GuiConfigEntries owningEntryList, IConfigElement configElement) {
            super(owningScreen, owningEntryList, configElement);
            this.beforeValue = Boolean.valueOf(String.valueOf(configElement.get()));
            this.currentValue = beforeValue;
            this.btnValue.enabled = enabled();
            updateValueButtonText();
        }

        @Override
        public void updateValueButtonText() {
            this.btnValue.displayString = I18n.format(String.valueOf(currentValue));
            btnValue.packedFGColour = currentValue ? GuiUtils.getColorCode('2', true) : GuiUtils.getColorCode('4', true);
        }

        @Override
        public void valueButtonPressed(int slotIndex) {
            if (enabled()) currentValue = !currentValue;
        }

        @Override
        public boolean isDefault() {
            return currentValue == Boolean.valueOf(String.valueOf(configElement.getDefault()));
        }

        @Override
        public void setToDefault() {
            if (enabled()) {
                currentValue = Boolean.valueOf(String.valueOf(configElement.getDefault()));
                updateValueButtonText();
            }
        }

        @Override
        public boolean isChanged() {
            return currentValue != beforeValue;
        }

        @Override
        public void undoChanges() {
            if (enabled()) {
                currentValue = beforeValue;
                updateValueButtonText();
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        public boolean saveConfigElement() {
            if (enabled() && isChanged()) {
                configElement.set(currentValue);
                return configElement.requiresMcRestart();
            }
            return false;
        }

        @Override
        public Boolean getCurrentValue() {
            return currentValue;
        }

        @Override
        public Boolean[] getCurrentValues() {
            return new Boolean[] {getCurrentValue()};
        }
    }

    /** Cycles through the element's valid values. */
    public static class CycleValueEntry extends ButtonEntry {
        protected final int beforeIndex;
        protected final int defaultIndex;
        protected int currentIndex;

        public CycleValueEntry(GuiConfig owningScreen, GuiConfigEntries owningEntryList, IConfigElement configElement) {
            super(owningScreen, owningEntryList, configElement);
            beforeIndex = getIndex(String.valueOf(configElement.get()));
            defaultIndex = getIndex(String.valueOf(configElement.getDefault()));
            currentIndex = beforeIndex;
            this.btnValue.enabled = enabled();
            updateValueButtonText();
        }

        private int getIndex(String s) {
            String[] valid = configElement.getValidValues();
            for (int i = 0; i < valid.length; i++) {
                if (valid[i].equalsIgnoreCase(s)) return i;
            }
            return 0;
        }

        @Override
        public void updateValueButtonText() {
            this.btnValue.displayString = I18n.format(configElement.getValidValues()[currentIndex]);
        }

        @Override
        public void valueButtonPressed(int slotIndex) {
            if (enabled()) {
                if (++currentIndex >= configElement.getValidValues().length) currentIndex = 0;
                updateValueButtonText();
            }
        }

        @Override
        public boolean isDefault() {
            return currentIndex == defaultIndex;
        }

        @Override
        public void setToDefault() {
            if (enabled()) {
                currentIndex = defaultIndex;
                updateValueButtonText();
            }
        }

        @Override
        public boolean isChanged() {
            return currentIndex != beforeIndex;
        }

        @Override
        public void undoChanges() {
            if (enabled()) {
                currentIndex = beforeIndex;
                updateValueButtonText();
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        public boolean saveConfigElement() {
            if (enabled() && isChanged()) {
                configElement.set(configElement.getValidValues()[currentIndex]);
                return configElement.requiresMcRestart();
            }
            return false;
        }

        @Override
        public String getCurrentValue() {
            return configElement.getValidValues()[currentIndex];
        }

        @Override
        public String[] getCurrentValues() {
            return new String[] {getCurrentValue()};
        }
    }

    /** A chat colour code (valid values are the codes), the button text drawn in that colour. */
    public static class ChatColorEntry extends CycleValueEntry {
        public ChatColorEntry(GuiConfig owningScreen, GuiConfigEntries owningEntryList, IConfigElement configElement) {
            super(owningScreen, owningEntryList, configElement);
            this.btnValue.enabled = enabled();
            updateValueButtonText();
        }

        @Override
        public void drawEntry(int slotIndex, int x, int y, int listWidth, int slotHeight, int mouseX, int mouseY, boolean isSelected, float partial) {
            String code = configElement.getValidValues()[currentIndex];
            if (!code.isEmpty()) this.btnValue.packedFGColour = GuiUtils.getColorCode(code.charAt(0), true);
            super.drawEntry(slotIndex, x, y, listWidth, slotHeight, mouseX, mouseY, isSelected, partial);
        }

        @Override
        public void updateValueButtonText() {
            this.btnValue.displayString = I18n.format(configElement.getValidValues()[currentIndex]) + " - " + I18n.format("fml.configgui.sampletext");
        }
    }

    /** Picks one of a map of values in a {@link GuiSelectString} screen (mod ids). */
    public static class SelectValueEntry extends ButtonEntry {
        protected final String beforeValue;
        protected Object currentValue;
        protected Map<Object, String> selectableValues;

        public SelectValueEntry(GuiConfig owningScreen, GuiConfigEntries owningEntryList, IConfigElement configElement, Map<Object, String> selectableValues) {
            super(owningScreen, owningEntryList, configElement);
            beforeValue = String.valueOf(configElement.get());
            currentValue = String.valueOf(configElement.get());
            this.selectableValues = selectableValues;
            updateValueButtonText();
        }

        @Override
        public void updateValueButtonText() {
            this.btnValue.displayString = String.valueOf(currentValue);
        }

        @Override
        public void valueButtonPressed(int slotIndex) {
            mc.displayGuiScreen(new GuiSelectString(owningScreen, configElement, slotIndex, selectableValues, currentValue, enabled()));
        }

        public void setValueFromChildScreen(Object newValue) {
            if (enabled() && (currentValue != null ? !currentValue.equals(newValue) : newValue != null)) {
                currentValue = newValue;
                updateValueButtonText();
            }
        }

        @Override
        public boolean isDefault() {
            return configElement.getDefault() != null ? configElement.getDefault().equals(currentValue) : currentValue == null;
        }

        @Override
        public void setToDefault() {
            if (enabled()) {
                currentValue = String.valueOf(configElement.getDefault());
                updateValueButtonText();
            }
        }

        @Override
        public boolean isChanged() {
            return beforeValue != null ? !beforeValue.equals(currentValue) : currentValue == null;
        }

        @Override
        public void undoChanges() {
            if (enabled()) {
                currentValue = beforeValue;
                updateValueButtonText();
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        public boolean saveConfigElement() {
            if (enabled() && isChanged()) {
                configElement.set(currentValue);
                return configElement.requiresMcRestart();
            }
            return false;
        }

        @Override
        public String getCurrentValue() {
            return String.valueOf(currentValue);
        }

        @Override
        public String[] getCurrentValues() {
            return new String[] {getCurrentValue()};
        }
    }

    /** A list value, edited in a {@link GuiEditArray} screen. */
    public static class ArrayEntry extends ButtonEntry {
        protected final Object[] beforeValues;
        protected Object[] currentValues;

        public ArrayEntry(GuiConfig owningScreen, GuiConfigEntries owningEntryList, IConfigElement configElement) {
            super(owningScreen, owningEntryList, configElement);
            beforeValues = configElement.getList();
            currentValues = configElement.getList();
            updateValueButtonText();
        }

        @Override
        public void updateValueButtonText() {
            StringBuilder sb = new StringBuilder();
            if (currentValues != null) {
                for (Object o : currentValues) sb.append(", [").append(o).append(']');
            }
            this.btnValue.displayString = sb.length() > 0 ? sb.substring(2) : "";
        }

        @Override
        public void valueButtonPressed(int slotIndex) {
            mc.displayGuiScreen(new GuiEditArray(owningScreen, configElement, slotIndex, currentValues, enabled()));
        }

        public void setListFromChildScreen(Object[] newList) {
            if (enabled() && !Arrays.deepEquals(currentValues, newList)) {
                currentValues = newList;
                updateValueButtonText();
            }
        }

        @Override
        public boolean isDefault() {
            return Arrays.deepEquals(configElement.getDefaults(), currentValues);
        }

        @Override
        public void setToDefault() {
            if (enabled()) {
                currentValues = configElement.getDefaults();
                updateValueButtonText();
            }
        }

        @Override
        public boolean isChanged() {
            return !Arrays.deepEquals(beforeValues, currentValues);
        }

        @Override
        public void undoChanges() {
            if (enabled()) {
                currentValues = beforeValues;
                updateValueButtonText();
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        public boolean saveConfigElement() {
            if (enabled() && isChanged()) {
                configElement.set(currentValues);
                return configElement.requiresMcRestart();
            }
            return false;
        }

        @Override
        public Object getCurrentValue() {
            return this.btnValue.displayString;
        }

        @Override
        public Object[] getCurrentValues() {
            return currentValues;
        }
    }

    /** A number edited with a slider between the element's bounds. */
    public static class NumberSliderEntry extends ButtonEntry {
        protected final double beforeValue;

        public NumberSliderEntry(GuiConfig owningScreen, GuiConfigEntries owningEntryList, IConfigElement configElement) {
            super(owningScreen, owningEntryList, configElement, new GuiSlider(0, owningEntryList.controlX, 0, owningEntryList.controlWidth, 18, "", "",
                    mcc$double(configElement.getMinValue(), 0), mcc$double(configElement.getMaxValue(), 1), mcc$double(configElement.get(), 0),
                    configElement.getType() == ConfigGuiType.DOUBLE, true));
            this.beforeValue = configElement.getType() == ConfigGuiType.INTEGER ? mcc$int(configElement.get(), 0) : mcc$double(configElement.get(), 0);
        }

        private boolean isInteger() {
            return configElement.getType() == ConfigGuiType.INTEGER;
        }

        @Override
        public void updateValueButtonText() {
            ((GuiSlider) this.btnValue).updateSlider();
        }

        @Override
        public void valueButtonPressed(int slotIndex) {
        }

        @Override
        public boolean isDefault() {
            GuiSlider slider = (GuiSlider) btnValue;
            return isInteger() ? slider.getValueInt() == mcc$int(configElement.getDefault(), 0) : slider.getValue() == mcc$double(configElement.getDefault(), 0);
        }

        @Override
        public void setToDefault() {
            if (enabled()) {
                ((GuiSlider) btnValue).setValue(mcc$double(configElement.getDefault(), 0));
                ((GuiSlider) btnValue).updateSlider();
            }
        }

        @Override
        public boolean isChanged() {
            GuiSlider slider = (GuiSlider) btnValue;
            return isInteger() ? slider.getValueInt() != (int) Math.round(beforeValue) : slider.getValue() != beforeValue;
        }

        @Override
        public void undoChanges() {
            if (enabled()) {
                ((GuiSlider) btnValue).setValue(beforeValue);
                ((GuiSlider) btnValue).updateSlider();
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        public boolean saveConfigElement() {
            if (enabled() && isChanged()) {
                GuiSlider slider = (GuiSlider) btnValue;
                if (isInteger()) configElement.set(slider.getValueInt());
                else configElement.set(slider.getValue());
                return configElement.requiresMcRestart();
            }
            return false;
        }

        @Override
        public Object getCurrentValue() {
            GuiSlider slider = (GuiSlider) btnValue;
            return isInteger() ? String.valueOf(slider.getValueInt()) : String.valueOf(slider.getValue());
        }

        @Override
        public Object[] getCurrentValues() {
            return new Object[] {getCurrentValue()};
        }
    }

    /** An entry whose control is one button. */
    public abstract static class ButtonEntry extends ListEntryBase {
        protected final GuiButtonExt btnValue;

        public ButtonEntry(GuiConfig owningScreen, GuiConfigEntries owningEntryList, IConfigElement configElement) {
            this(owningScreen, owningEntryList, configElement, new GuiButtonExt(0, owningEntryList.controlX, 0, owningEntryList.controlWidth, 18,
                    configElement.get() != null ? I18n.format(String.valueOf(configElement.get())) : ""));
        }

        public ButtonEntry(GuiConfig owningScreen, GuiConfigEntries owningEntryList, IConfigElement configElement, GuiButtonExt button) {
            super(owningScreen, owningEntryList, configElement);
            this.btnValue = button;
        }

        public abstract void updateValueButtonText();

        public abstract void valueButtonPressed(int slotIndex);

        @Override
        public void drawEntry(int slotIndex, int x, int y, int listWidth, int slotHeight, int mouseX, int mouseY, boolean isSelected, float partial) {
            super.drawEntry(slotIndex, x, y, listWidth, slotHeight, mouseX, mouseY, isSelected, partial);
            this.btnValue.width = owningEntryList.controlWidth;
            this.btnValue.x = owningScreen.entryList.controlX;
            this.btnValue.y = y;
            this.btnValue.enabled = enabled();
            this.btnValue.drawButton(mc, mouseX, mouseY, partial);
        }

        @Override
        public boolean mousePressed(int index, int x, int y, int mouseEvent, int relativeX, int relativeY) {
            if (this.btnValue.mousePressed(mc, x, y)) {
                btnValue.playPressSound(mc.getSoundHandler());
                valueButtonPressed(index);
                updateValueButtonText();
                return true;
            }
            return super.mousePressed(index, x, y, mouseEvent, relativeX, relativeY);
        }

        @Override
        public void mouseReleased(int index, int x, int y, int mouseEvent, int relativeX, int relativeY) {
            super.mouseReleased(index, x, y, mouseEvent, relativeX, relativeY);
            this.btnValue.mouseReleased(x, y);
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
    }

    /** A decimal number typed into a text field. */
    public static class DoubleEntry extends StringEntry {
        protected final double beforeValue;

        public DoubleEntry(GuiConfig owningScreen, GuiConfigEntries owningEntryList, IConfigElement configElement) {
            super(owningScreen, owningEntryList, configElement);
            this.beforeValue = mcc$double(configElement.get(), 0);
        }

        @Override
        public void keyTyped(char eventChar, int eventKey) {
            if (enabled() || eventKey == KEY_LEFT || eventKey == KEY_RIGHT || eventKey == KEY_HOME || eventKey == KEY_END) {
                String validChars = "0123456789";
                String before = textFieldValue.getText();
                if ((eventChar != 0 && validChars.indexOf(eventChar) >= 0)
                        || (!before.startsWith("-") && textFieldValue.getCursorPosition() == 0 && eventChar == '-')
                        || (!before.contains(".") && eventChar == '.')
                        || eventKey == KEY_BACK || eventKey == KEY_DELETE || eventKey == KEY_LEFT || eventKey == KEY_RIGHT || eventKey == KEY_HOME || eventKey == KEY_END) {
                    textFieldValue.textboxKeyTyped(enabled() ? eventChar : '\0', eventKey);
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
        public boolean isChanged() {
            try {
                return beforeValue != Double.parseDouble(textFieldValue.getText().trim());
            } catch (Throwable e) {
                return true;
            }
        }

        @Override
        public void undoChanges() {
            if (enabled()) textFieldValue.setText(String.valueOf(beforeValue));
        }

        @Override
        @SuppressWarnings("unchecked")
        public boolean saveConfigElement() {
            if (!enabled()) return false;
            if (isChanged() && isValidValue) {
                try {
                    configElement.set(Double.parseDouble(textFieldValue.getText().trim()));
                    return configElement.requiresMcRestart();
                } catch (Throwable e) {
                    configElement.setToDefault();
                }
            } else if (isChanged() && !isValidValue) {
                try {
                    double value = Double.parseDouble(textFieldValue.getText().trim());
                    if (value < mcc$double(configElement.getMinValue(), -Double.MAX_VALUE)) configElement.set(configElement.getMinValue());
                    else configElement.set(configElement.getMaxValue());
                } catch (Throwable e) {
                    configElement.setToDefault();
                }
            }
            return configElement.requiresMcRestart() && beforeValue != mcc$double(configElement.get(), beforeValue);
        }
    }

    /** A whole number typed into a text field. */
    public static class IntegerEntry extends StringEntry {
        protected final int beforeValue;

        public IntegerEntry(GuiConfig owningScreen, GuiConfigEntries owningEntryList, IConfigElement configElement) {
            super(owningScreen, owningEntryList, configElement);
            this.beforeValue = mcc$int(configElement.get(), 0);
        }

        @Override
        public void keyTyped(char eventChar, int eventKey) {
            if (enabled() || eventKey == KEY_LEFT || eventKey == KEY_RIGHT || eventKey == KEY_HOME || eventKey == KEY_END) {
                String validChars = "0123456789";
                String before = textFieldValue.getText();
                if ((eventChar != 0 && validChars.indexOf(eventChar) >= 0)
                        || (!before.startsWith("-") && textFieldValue.getCursorPosition() == 0 && eventChar == '-')
                        || eventKey == KEY_BACK || eventKey == KEY_DELETE || eventKey == KEY_LEFT || eventKey == KEY_RIGHT || eventKey == KEY_HOME || eventKey == KEY_END) {
                    textFieldValue.textboxKeyTyped(enabled() ? eventChar : '\0', eventKey);
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
        public boolean isChanged() {
            try {
                return beforeValue != Integer.parseInt(textFieldValue.getText().trim());
            } catch (Throwable e) {
                return true;
            }
        }

        @Override
        public void undoChanges() {
            if (enabled()) textFieldValue.setText(String.valueOf(beforeValue));
        }

        @Override
        @SuppressWarnings("unchecked")
        public boolean saveConfigElement() {
            if (!enabled()) return false;
            if (isChanged() && isValidValue) {
                try {
                    configElement.set(Integer.parseInt(textFieldValue.getText().trim()));
                    return configElement.requiresMcRestart();
                } catch (Throwable e) {
                    configElement.setToDefault();
                }
            } else if (isChanged() && !isValidValue) {
                try {
                    long value = Long.parseLong(textFieldValue.getText().trim());
                    if (value < mcc$int(configElement.getMinValue(), Integer.MIN_VALUE)) configElement.set(configElement.getMinValue());
                    else configElement.set(configElement.getMaxValue());
                } catch (Throwable e) {
                    configElement.setToDefault();
                }
            }
            return configElement.requiresMcRestart() && beforeValue != mcc$int(configElement.get(), beforeValue);
        }
    }

    /** Text typed into a text field, checked against the element's pattern if it has one. */
    public static class StringEntry extends ListEntryBase {
        protected final GuiTextField textFieldValue;
        protected final String beforeValue;

        public StringEntry(GuiConfig owningScreen, GuiConfigEntries owningEntryList, IConfigElement configElement) {
            super(owningScreen, owningEntryList, configElement);
            beforeValue = String.valueOf(configElement.get());
            this.textFieldValue = new GuiTextField(10, mc.fontRenderer, owningEntryList.controlX + 1, 0, owningEntryList.controlWidth - 3, 16);
            this.textFieldValue.setMaxStringLength(10000);
            this.textFieldValue.setText(String.valueOf(configElement.get()));
        }

        @Override
        public void drawEntry(int slotIndex, int x, int y, int listWidth, int slotHeight, int mouseX, int mouseY, boolean isSelected, float partial) {
            super.drawEntry(slotIndex, x, y, listWidth, slotHeight, mouseX, mouseY, isSelected, partial);
            textFieldValue.x = owningEntryList.controlX + 2;
            textFieldValue.y = y + 1;
            textFieldValue.width = owningEntryList.controlWidth - 4;
            textFieldValue.setEnabled(enabled());
            textFieldValue.drawTextBox();
        }

        @Override
        public void keyTyped(char eventChar, int eventKey) {
            if (enabled() || eventKey == KEY_LEFT || eventKey == KEY_RIGHT || eventKey == KEY_HOME || eventKey == KEY_END) {
                textFieldValue.textboxKeyTyped(enabled() ? eventChar : '\0', eventKey);
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
        public boolean isDefault() {
            return configElement.getDefault() != null ? configElement.getDefault().toString().equals(textFieldValue.getText())
                    : textFieldValue.getText().trim().isEmpty();
        }

        @Override
        public void setToDefault() {
            if (enabled()) {
                textFieldValue.setText(String.valueOf(configElement.getDefault()));
                keyTyped('\0', KEY_HOME);
            }
        }

        @Override
        public boolean isChanged() {
            return beforeValue != null ? !beforeValue.equals(textFieldValue.getText()) : textFieldValue.getText().trim().isEmpty();
        }

        @Override
        public void undoChanges() {
            if (enabled()) textFieldValue.setText(beforeValue);
        }

        @Override
        @SuppressWarnings("unchecked")
        public boolean saveConfigElement() {
            if (enabled()) {
                if (isChanged() && isValidValue) {
                    configElement.set(textFieldValue.getText());
                    return configElement.requiresMcRestart();
                } else if (isChanged() && !isValidValue) {
                    configElement.setToDefault();
                    return configElement.requiresMcRestart() && beforeValue != null ? beforeValue.equals(configElement.getDefault())
                            : configElement.getDefault() == null;
                }
            }
            return false;
        }

        @Override
        public Object getCurrentValue() {
            return textFieldValue.getText();
        }

        @Override
        public Object[] getCurrentValues() {
            return new Object[] {getCurrentValue()};
        }
    }

    /** A category: a wide button opening a child {@link GuiConfig} with the category's elements. */
    public static class CategoryEntry extends ListEntryBase {
        protected GuiScreen childScreen;
        protected final GuiButtonExt btnSelectCategory;

        public CategoryEntry(GuiConfig owningScreen, GuiConfigEntries owningEntryList, IConfigElement configElement) {
            super(owningScreen, owningEntryList, configElement);
            this.childScreen = buildChildScreen();
            this.btnSelectCategory = new GuiButtonExt(0, 0, 0, 300, 18, I18n.format(name));
            this.tooltipHoverChecker = new HoverChecker(btnSelectCategory, 800);
            this.drawLabel = false;
        }

        /** The sub-screen; mods override this to show their own screen for a category. */
        protected GuiScreen buildChildScreen() {
            return new GuiConfig(owningScreen, configElement.getChildElements(), owningScreen.modID,
                    owningScreen.allRequireWorldRestart || configElement.requiresWorldRestart(),
                    owningScreen.allRequireMcRestart || configElement.requiresMcRestart(), owningScreen.title,
                    (owningScreen.titleLine2 == null ? "" : owningScreen.titleLine2) + " > " + name);
        }

        @Override
        public void drawEntry(int slotIndex, int x, int y, int listWidth, int slotHeight, int mouseX, int mouseY, boolean isSelected, float partial) {
            btnSelectCategory.x = listWidth / 2 - 150;
            btnSelectCategory.y = y;
            btnSelectCategory.enabled = enabled();
            btnSelectCategory.drawButton(mc, mouseX, mouseY, partial);
            super.drawEntry(slotIndex, x, y, listWidth, slotHeight, mouseX, mouseY, isSelected, partial);
        }

        @Override
        public void drawToolTip(int mouseX, int mouseY) {
            boolean canHover = mouseY < owningScreen.entryList.bottom && mouseY > owningScreen.entryList.top;
            if (tooltipHoverChecker.checkHover(mouseX, mouseY, canHover)) owningScreen.drawToolTip(toolTip, mouseX, mouseY);
            super.drawToolTip(mouseX, mouseY);
        }

        @Override
        public boolean mousePressed(int index, int x, int y, int mouseEvent, int relativeX, int relativeY) {
            if (btnSelectCategory.mousePressed(mc, x, y)) {
                btnSelectCategory.playPressSound(mc.getSoundHandler());
                Minecraft.getMinecraft().displayGuiScreen(childScreen);
                return true;
            }
            return super.mousePressed(index, x, y, mouseEvent, relativeX, relativeY);
        }

        @Override
        public void mouseReleased(int index, int x, int y, int mouseEvent, int relativeX, int relativeY) {
            btnSelectCategory.mouseReleased(x, y);
        }

        private GuiConfigEntries childList() {
            return childScreen instanceof GuiConfig child ? child.entryList : null;
        }

        @Override
        public boolean isDefault() {
            GuiConfigEntries list = childList();
            return list == null || list.areAllEntriesDefault(true);
        }

        @Override
        public void setToDefault() {
            GuiConfigEntries list = childList();
            if (list != null) list.setAllToDefault(true);
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
        public boolean saveConfigElement() {
            GuiConfigEntries list = childList();
            if (list == null) return false;
            boolean requiresRestart = configElement.requiresMcRestart() && list.hasChangedEntry(true);
            if (list.saveConfigElements()) requiresRestart = true;
            return requiresRestart;
        }

        @Override
        public boolean isChanged() {
            GuiConfigEntries list = childList();
            return list != null && list.hasChangedEntry(true);
        }

        @Override
        public void undoChanges() {
            GuiConfigEntries list = childList();
            if (list != null) list.undoAllChanges(true);
        }

        @Override
        public boolean enabled() {
            return true;
        }

        @Override
        public int getLabelWidth() {
            return 0;
        }

        @Override
        public int getEntryRightBound() {
            return owningEntryList.width / 2 + 155 + 22 + 18;
        }

        @Override
        public String getCurrentValue() {
            return "";
        }

        @Override
        public String[] getCurrentValues() {
            return new String[] {getCurrentValue()};
        }
    }

    /** The label, tooltip and undo / reset buttons every entry has. */
    public abstract static class ListEntryBase implements IConfigEntry {
        protected final GuiConfig owningScreen;
        protected final GuiConfigEntries owningEntryList;
        protected final IConfigElement configElement;
        protected final Minecraft mc;
        protected final String name;
        protected final GuiButtonExt btnUndoChanges;
        protected final GuiButtonExt btnDefault;
        protected List toolTip;
        protected List undoToolTip;
        protected List defaultToolTip;
        protected boolean isValidValue = true;
        protected HoverChecker tooltipHoverChecker;
        protected HoverChecker undoHoverChecker;
        protected HoverChecker defaultHoverChecker;
        protected boolean drawLabel;

        @SuppressWarnings("unchecked")
        public ListEntryBase(GuiConfig owningScreen, GuiConfigEntries owningEntryList, IConfigElement configElement) {
            this.owningScreen = owningScreen;
            this.owningEntryList = owningEntryList;
            this.configElement = configElement;
            this.mc = Minecraft.getMinecraft();
            String key = configElement.getLanguageKey();
            String trans = key == null ? null : I18n.format(key);
            this.name = trans != null && !trans.equals(key) ? trans : configElement.getName();
            this.btnUndoChanges = new GuiButtonExt(0, 0, 0, 18, 18, UNDO_CHAR);
            this.btnDefault = new GuiButtonExt(0, 0, 0, 18, 18, RESET_CHAR);
            this.undoHoverChecker = new HoverChecker(btnUndoChanges, 800);
            this.defaultHoverChecker = new HoverChecker(btnDefault, 800);
            this.undoToolTip = Arrays.asList(I18n.format("fml.configgui.tooltip.undoChanges"));
            this.defaultToolTip = Arrays.asList(I18n.format("fml.configgui.tooltip.resetToDefault"));
            this.toolTip = new ArrayList();
            this.drawLabel = true;

            String comment = key == null ? null : I18n.format(key + ".tooltip").replace("\\n", "\n");
            if (comment != null && !comment.equals(key + ".tooltip")) {
                Collections.addAll(toolTip, (TextFormatting.GREEN + name + "\n" + TextFormatting.YELLOW + removeTag(comment, "[default:", "]")).split("\n"));
            } else if (configElement.getComment() != null && !configElement.getComment().trim().isEmpty()) {
                Collections.addAll(toolTip, (TextFormatting.GREEN + name + "\n" + TextFormatting.YELLOW
                        + removeTag(configElement.getComment(), "[default:", "]")).split("\n"));
            } else {
                Collections.addAll(toolTip, (TextFormatting.GREEN + name + "\n" + TextFormatting.RED + "No tooltip defined.").split("\n"));
            }
            ConfigGuiType type = configElement.getType();
            if ((type == ConfigGuiType.INTEGER && (mcc$int(configElement.getMinValue(), Integer.MIN_VALUE) != Integer.MIN_VALUE
                    || mcc$int(configElement.getMaxValue(), Integer.MAX_VALUE) != Integer.MAX_VALUE))
                    || (type == ConfigGuiType.DOUBLE && (mcc$double(configElement.getMinValue(), -Double.MAX_VALUE) != -Double.MAX_VALUE
                            || mcc$double(configElement.getMaxValue(), Double.MAX_VALUE) != Double.MAX_VALUE))) {
                Collections.addAll(toolTip, (TextFormatting.AQUA + I18n.format("fml.configgui.tooltip.defaultNumeric", configElement.getMinValue(),
                        configElement.getMaxValue(), configElement.getDefault())).split("\n"));
            } else if (type != ConfigGuiType.CONFIG_CATEGORY) {
                Collections.addAll(toolTip, (TextFormatting.AQUA + I18n.format("fml.configgui.tooltip.default", configElement.getDefault())).split("\n"));
            }
            if (configElement.requiresMcRestart() || owningScreen.allRequireMcRestart) {
                toolTip.add(TextFormatting.RED + "[" + I18n.format("fml.configgui.gameRestartTitle") + "]");
            }
        }

        @Override
        public void drawEntry(int slotIndex, int x, int y, int listWidth, int slotHeight, int mouseX, int mouseY, boolean isSelected, float partial) {
            boolean isChanged = isChanged();
            if (drawLabel) {
                String label = (!isValidValue ? TextFormatting.RED.toString() : isChanged ? TextFormatting.WHITE.toString()
                        : TextFormatting.GRAY.toString()) + (isChanged ? TextFormatting.ITALIC : "") + name;
                mc.fontRenderer.drawString(label, owningScreen.entryList.labelX, y + slotHeight / 2 - mc.fontRenderer.FONT_HEIGHT / 2, 16777215);
            }
            btnUndoChanges.x = owningEntryList.scrollBarX - 44;
            btnUndoChanges.y = y;
            btnUndoChanges.enabled = enabled() && isChanged;
            btnUndoChanges.drawButton(mc, mouseX, mouseY, partial);
            btnDefault.x = owningEntryList.scrollBarX - 22;
            btnDefault.y = y;
            btnDefault.enabled = enabled() && !isDefault();
            btnDefault.drawButton(mc, mouseX, mouseY, partial);
            if (tooltipHoverChecker == null) tooltipHoverChecker = new HoverChecker(y, y + slotHeight, x, owningScreen.entryList.controlX - 8, 800);
            else tooltipHoverChecker.updateBounds(y, y + slotHeight, x, owningScreen.entryList.controlX - 8);
        }

        @Override
        public void drawToolTip(int mouseX, int mouseY) {
            boolean canHover = mouseY < owningScreen.entryList.bottom && mouseY > owningScreen.entryList.top;
            if (toolTip != null && tooltipHoverChecker != null && tooltipHoverChecker.checkHover(mouseX, mouseY, canHover)) {
                owningScreen.drawToolTip(toolTip, mouseX, mouseY);
            }
            if (undoHoverChecker.checkHover(mouseX, mouseY, canHover)) owningScreen.drawToolTip(undoToolTip, mouseX, mouseY);
            if (defaultHoverChecker.checkHover(mouseX, mouseY, canHover)) owningScreen.drawToolTip(defaultToolTip, mouseX, mouseY);
        }

        @Override
        public boolean mousePressed(int index, int x, int y, int mouseEvent, int relativeX, int relativeY) {
            if (btnDefault.mousePressed(mc, x, y)) {
                btnDefault.playPressSound(mc.getSoundHandler());
                setToDefault();
                return true;
            } else if (btnUndoChanges.mousePressed(mc, x, y)) {
                btnUndoChanges.playPressSound(mc.getSoundHandler());
                undoChanges();
                return true;
            }
            return false;
        }

        @Override
        public void mouseReleased(int index, int x, int y, int mouseEvent, int relativeX, int relativeY) {
            btnDefault.mouseReleased(x, y);
        }

        @Override
        public abstract boolean isDefault();

        @Override
        public abstract void setToDefault();

        @Override
        public abstract void keyTyped(char eventChar, int eventKey);

        @Override
        public abstract void updateCursorCounter();

        @Override
        public abstract void mouseClicked(int x, int y, int mouseEvent);

        @Override
        public abstract boolean isChanged();

        @Override
        public abstract void undoChanges();

        @Override
        public abstract boolean saveConfigElement();

        @Override
        public String getName() {
            return configElement.getName();
        }

        @Override
        public abstract Object getCurrentValue();

        @Override
        public abstract Object[] getCurrentValues();

        /** Values that need a world restart are locked while a world is open. */
        @Override
        public boolean enabled() {
            return !owningScreen.isWorldRunning || (!owningScreen.allRequireWorldRestart && !configElement.requiresWorldRestart());
        }

        @Override
        public int getLabelWidth() {
            return mc.fontRenderer.getStringWidth(name);
        }

        @Override
        public int getEntryRightBound() {
            return owningEntryList.resetX + 40;
        }

        @Override
        public IConfigElement getConfigElement() {
            return configElement;
        }

        @Override
        public void onGuiClosed() {
        }

        /** Comments carry their own "[default: x]"; the tooltip shows the default on a line of its own. */
        protected String removeTag(String target, String tagStart, String tagEnd) {
            int start = target.indexOf(tagStart);
            int end = start < 0 ? -1 : target.indexOf(tagEnd, start + tagStart.length());
            if (start == -1 || end == -1) return target;
            return target.substring(0, start) + target.substring(end + 1);
        }
    }

    /** What the list needs from an entry; custom entries implement it (usually through {@link ListEntryBase}). */
    public interface IConfigEntry<T> extends GuiListExtended.IGuiListEntry {
        /** 1.12: told the position of entries scrolled out of view (text fields follow it). */
        @Override
        default void updatePosition(int slotIndex, int x, int y, float partialTicks) {
        }

        IConfigElement<T> getConfigElement();

        String getName();

        T getCurrentValue();

        T[] getCurrentValues();

        boolean enabled();

        void keyTyped(char eventChar, int eventKey);

        void updateCursorCounter();

        void mouseClicked(int x, int y, int mouseEvent);

        boolean isDefault();

        void setToDefault();

        void undoChanges();

        boolean isChanged();

        boolean saveConfigElement();

        void drawToolTip(int mouseX, int mouseY);

        int getLabelWidth();

        int getEntryRightBound();

        void onGuiClosed();
    }
}
