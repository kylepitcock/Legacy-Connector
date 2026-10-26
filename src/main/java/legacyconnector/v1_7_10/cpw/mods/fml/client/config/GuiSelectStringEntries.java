package legacyconnector.v1_7_10.cpw.mods.fml.client.config;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import legacyconnector.v1_7_10.net.minecraft.client.Minecraft;
import legacyconnector.v1_7_10.net.minecraft.client.gui.GuiListExtended;
import legacyconnector.v1_7_10.net.minecraft.client.renderer.Tessellator;

/** The values of a {@link GuiSelectString}, sorted by name; clicking one selects it. */
public class GuiSelectStringEntries extends GuiListExtended {
    public GuiSelectString owningScreen;
    public Minecraft mc;
    public IConfigElement configElement;
    public List<IGuiSelectStringListEntry> listEntries;
    public final Map<Object, String> selectableValues;
    public int selectedIndex = -1;
    public int maxEntryWidth = 0;

    public GuiSelectStringEntries(GuiSelectString owningScreen, Minecraft mc, IConfigElement configElement, Map<Object, String> selectableValues) {
        super(mc, owningScreen.width, owningScreen.height, owningScreen.titleLine2 != null ? (owningScreen.titleLine3 != null ? 43 : 33) : 23,
                owningScreen.height - 32, 11);
        this.owningScreen = owningScreen;
        this.mc = mc;
        this.configElement = configElement;
        this.selectableValues = selectableValues;
        this.setShowSelectionBox(true);
        this.listEntries = new ArrayList<>();
        List<Entry<Object, String>> sorted = new ArrayList<>(selectableValues.entrySet());
        sorted.sort(new EntryComparator());
        int index = 0;
        for (Entry<Object, String> entry : sorted) {
            listEntries.add(new ListEntry(this, entry));
            maxEntryWidth = Math.max(maxEntryWidth, mc.fontRenderer.getStringWidth(entry.getValue()));
            if (owningScreen.currentValue != null && owningScreen.currentValue.equals(entry.getKey())) selectedIndex = index;
            index++;
        }
    }

    public static class EntryComparator implements Comparator<Entry<Object, String>> {
        @Override
        public int compare(Entry<Object, String> o1, Entry<Object, String> o2) {
            return String.valueOf(o1.getValue()).compareTo(String.valueOf(o2.getValue()));
        }
    }

    @Override
    protected void elementClicked(int index, boolean doubleClick, int mouseX, int mouseY) {
        selectedIndex = index;
        owningScreen.currentValue = listEntries.get(index).getValue();
    }

    @Override
    protected boolean isSelected(int index) {
        return index == selectedIndex;
    }

    @Override
    protected int getScrollBarX() {
        return width / 2 + maxEntryWidth / 2 + 5;
    }

    @Override
    public int getListWidth() {
        return maxEntryWidth + 5;
    }

    @Override
    public IGuiSelectStringListEntry getListEntry(int index) {
        return listEntries.get(index);
    }

    @Override
    protected int getSize() {
        return listEntries.size();
    }

    public boolean isChanged() {
        return owningScreen.beforeValue != null ? !owningScreen.beforeValue.equals(owningScreen.currentValue) : owningScreen.currentValue != null;
    }

    public boolean isDefault() {
        return owningScreen.currentValue != null ? owningScreen.currentValue.equals(configElement.getDefault()) : configElement.getDefault() == null;
    }

    @SuppressWarnings("unchecked")
    public void saveChanges() {
        if (owningScreen.slotIndex != -1 && owningScreen.parentScreen instanceof GuiConfig config
                && config.entryList.getListEntry(owningScreen.slotIndex) instanceof GuiConfigEntries.SelectValueEntry entry) {
            entry.setValueFromChildScreen(owningScreen.currentValue);
        } else {
            configElement.set(owningScreen.currentValue);
        }
    }

    public static class ListEntry implements IGuiSelectStringListEntry {
        protected final GuiSelectStringEntries owningList;
        protected final Entry<Object, String> value;

        public ListEntry(GuiSelectStringEntries owningList, Entry<Object, String> value) {
            this.owningList = owningList;
            this.value = value;
        }

        @Override
        public void drawEntry(int slotIndex, int x, int y, int listWidth, int slotHeight, Tessellator tessellator, int mouseX, int mouseY, boolean isSelected) {
            owningList.mc.fontRenderer.drawString(value.getValue(), x + 1, y, slotIndex == owningList.selectedIndex ? 16777215 : 14737632);
        }

        @Override
        public boolean mousePressed(int index, int x, int y, int mouseEvent, int relativeX, int relativeY) {
            return false;
        }

        @Override
        public void mouseReleased(int index, int x, int y, int mouseEvent, int relativeX, int relativeY) {
        }

        @Override
        public Object getValue() {
            return value.getKey();
        }
    }

    public interface IGuiSelectStringListEntry extends GuiListExtended.IGuiListEntry {
        Object getValue();
    }
}
