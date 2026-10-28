package legacyconnector.v1_12_2.net.minecraft.client.gui;

import java.util.ArrayList;
import java.util.List;
import legacyconnector.v1_12_2.net.minecraft.client.Minecraft;
import legacyconnector.v1_12_2.net.minecraft.client.settings.GameSettings;

/** A scrolling list of option buttons and sliders, two per row (the video settings list). */
public class GuiOptionsRowList extends GuiListExtended {
    private final List<Row> options = new ArrayList<>();

    public GuiOptionsRowList(Minecraft mc, int width, int height, int top, int bottom, int slotHeight, GameSettings.Options... optionsIn) {
        super(mc, width, height, top, bottom, slotHeight);
        for (int i = 0; i < optionsIn.length; i += 2) {
            GameSettings.Options a = optionsIn[i];
            GameSettings.Options b = i < optionsIn.length - 1 ? optionsIn[i + 1] : null;
            options.add(new Row(createButton(mc, width / 2 - 155, 0, a), createButton(mc, width / 2 - 155 + 160, 0, b)));
        }
    }

    private GuiButton createButton(Minecraft mc, int x, int y, GameSettings.Options option) {
        if (option == null) return null;
        int id = option.getOrdinal();
        if (option.isFloat()) return new GuiOptionSlider(id, x, y, option);
        return new GuiOptionButton(id, x, y, option, Minecraft.mcc$get$gameSettings(mc).getKeyBinding(option));
    }

    @Override
    public Row getListEntry(int index) {
        return options.get(index);
    }

    @Override
    protected int getSize() {
        return options.size();
    }

    @Override
    public int getListWidth() {
        return 400;
    }

    /** Two buttons side by side. */
    public static class Row implements GuiListExtended.IGuiListEntry {
        private final Minecraft client = Minecraft.getMinecraft();
        public final GuiButton buttonA;
        public final GuiButton buttonB;

        public Row(GuiButton buttonA, GuiButton buttonB) {
            this.buttonA = buttonA;
            this.buttonB = buttonB;
        }

        @Override
        public void drawEntry(int slotIndex, int x, int y, int listWidth, int slotHeight, int mouseX, int mouseY, boolean isSelected, float partialTicks) {
            if (buttonA != null) {
                buttonA.y = y;
                buttonA.drawButton(client, mouseX, mouseY, partialTicks);
            }
            if (buttonB != null) {
                buttonB.y = y;
                buttonB.drawButton(client, mouseX, mouseY, partialTicks);
            }
        }

        private boolean press(GuiButton button, int mouseX, int mouseY) {
            if (button == null || !button.mousePressed(client, mouseX, mouseY)) return false;
            if (button instanceof GuiOptionButton option && option.getOption() != null) {
                var settings = Minecraft.mcc$get$gameSettings(client);
                settings.setOptionValue(option.getOption(), 1);
                button.displayString = settings.getKeyBinding(option.getOption());
            }
            return true;
        }

        @Override
        public boolean mousePressed(int slotIndex, int mouseX, int mouseY, int mouseEvent, int relativeX, int relativeY) {
            return press(buttonA, mouseX, mouseY) || press(buttonB, mouseX, mouseY);
        }

        @Override
        public void mouseReleased(int slotIndex, int x, int y, int mouseEvent, int relativeX, int relativeY) {
            if (buttonA != null) buttonA.mouseReleased(x, y);
            if (buttonB != null) buttonB.mouseReleased(x, y);
        }

        @Override
        public void updatePosition(int slotIndex, int x, int y, float partialTicks) {
        }
    }
}
