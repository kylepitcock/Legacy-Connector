package legacyconnector.v1_12_2.net.minecraft.client.gui;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import java.util.ArrayList;
import java.util.List;
import legacyconnector.v1_12_2.net.minecraft.client.Minecraft;
import legacyconnector.v1_12_2.net.minecraft.util.IntHashMap;

/**
 * 1.12.2's paged list of controls (the customized world settings screen and the mods that copy it): each page is a list of entries
 * laid out two per row as buttons, sliders, text fields or labels, all reporting their changes to one {@link GuiResponder}.
 */
public class GuiPageButtonList extends GuiListExtended {
    private final List<GuiEntry> entries = new ArrayList<>();
    private final IntHashMap<Gui> componentMap = new IntHashMap<>();
    private final List<GuiTextField> editBoxes = new ArrayList<>();
    private final GuiListEntry[][] pages;
    private int page;
    private final GuiResponder responder;
    private Gui focusedControl;

    public GuiPageButtonList(Minecraft mcIn, int widthIn, int heightIn, int topIn, int bottomIn, int slotHeightIn, GuiResponder responderIn, GuiListEntry[]... pagesIn) {
        super(mcIn, widthIn, heightIn, topIn, bottomIn, slotHeightIn);
        this.responder = responderIn;
        this.pages = pagesIn;
        this.centerListVertically = false;
        populateComponents();
        populateEntries();
    }

    private void populateComponents() {
        for (GuiListEntry[] entriesOfPage : pages) {
            for (int i = 0; i < entriesOfPage.length; i += 2) {
                GuiListEntry a = entriesOfPage[i];
                GuiListEntry b = i < entriesOfPage.length - 1 ? entriesOfPage[i + 1] : null;
                Gui ga = createEntry(a, 0, b == null);
                Gui gb = createEntry(b, 160, a == null);
                entries.add(new GuiEntry(ga, gb));
                if (a != null && ga != null) {
                    componentMap.addKey(a.getId(), ga);
                    if (ga instanceof GuiTextField tf) editBoxes.add(tf);
                }
                if (b != null && gb != null) {
                    componentMap.addKey(b.getId(), gb);
                    if (gb instanceof GuiTextField tf) editBoxes.add(tf);
                }
            }
        }
    }

    private void populateEntries() {
        entries.clear();
        GuiListEntry[] current = pages[page];
        for (int i = 0; i < current.length; i += 2) {
            GuiListEntry a = current[i];
            GuiListEntry b = i < current.length - 1 ? current[i + 1] : null;
            Gui ga = a == null ? null : componentMap.lookup(a.getId());
            Gui gb = b == null ? null : componentMap.lookup(b.getId());
            entries.add(new GuiEntry(ga, gb));
        }
    }

    public void setPage(int pageIn) {
        if (pageIn == page) return;
        int old = page;
        page = pageIn;
        populateEntries();
        markVisibility(old, pageIn);
        amountScrolled = 0.0F;
    }

    public int getPage() {
        return page;
    }

    public int getPageCount() {
        return pages.length;
    }

    public Gui getFocusedControl() {
        return focusedControl;
    }

    public void previousPage() {
        if (page > 0) setPage(page - 1);
    }

    public void nextPage() {
        if (page < pages.length - 1) setPage(page + 1);
    }

    public Gui getComponent(int id) {
        return componentMap.lookup(id);
    }

    private void markVisibility(int oldPage, int newPage) {
        for (GuiListEntry e : pages[oldPage]) if (e != null) setComponentVisibility(componentMap.lookup(e.getId()), false);
        for (GuiListEntry e : pages[newPage]) if (e != null) setComponentVisibility(componentMap.lookup(e.getId()), true);
    }

    private void setComponentVisibility(Gui component, boolean visible) {
        if (component instanceof GuiButton b) b.visible = visible;
        else if (component instanceof GuiTextField t) t.setVisible(visible);
        else if (component instanceof GuiLabel l) l.visible = visible;
    }

    private Gui createEntry(GuiListEntry entry, int xOffset, boolean alone) {
        int x = width / 2 - 155 + xOffset;
        if (entry instanceof GuiSlideEntry s) return createSlider(x, 0, s);
        if (entry instanceof GuiButtonEntry b) return createButton(x, 0, b);
        if (entry instanceof EditBoxEntry e) return createTextField(x, 0, e);
        if (entry instanceof GuiLabelEntry l) return createLabel(x, 0, l, alone);
        return null;
    }

    public void setActive(boolean active) {
        for (GuiEntry e : entries) {
            if (e.component1 instanceof GuiButton b) b.enabled = active;
            if (e.component2 instanceof GuiButton b) b.enabled = active;
        }
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int mouseEvent) {
        boolean clicked = super.mouseClicked(mouseX, mouseY, mouseEvent);
        int i = getSlotIndexFromScreenCoords(mouseX, mouseY);
        if (i >= 0) {
            GuiEntry e = getListEntry(i);
            if (focusedControl != e.focusedControl && focusedControl instanceof GuiTextField tf) tf.setFocused(false);
            focusedControl = e.focusedControl;
        }
        return clicked;
    }

    private GuiSlider createSlider(int x, int y, GuiSlideEntry entry) {
        GuiSlider slider = new GuiSlider(responder, entry.getId(), x, y, entry.getCaption(), entry.getMinValue(), entry.getMaxValue(), entry.getInitalValue(),
                entry.getFormatter());
        slider.visible = entry.shouldStartVisible();
        return slider;
    }

    private GuiListButton createButton(int x, int y, GuiButtonEntry entry) {
        GuiListButton button = new GuiListButton(responder, entry.getId(), x, y, entry.getCaption(), entry.getInitialValue());
        button.visible = entry.shouldStartVisible();
        return button;
    }

    private GuiTextField createTextField(int x, int y, EditBoxEntry entry) {
        GuiTextField field = new GuiTextField(entry.getId(), mc.fontRenderer, x, y, 150, 20);
        field.setText(entry.getCaption());
        field.setGuiResponder(responder);
        field.setVisible(entry.shouldStartVisible());
        field.setValidator(entry.getFilter());
        return field;
    }

    private GuiLabel createLabel(int x, int y, GuiLabelEntry entry, boolean fullWidth) {
        GuiLabel label = fullWidth ? new GuiLabel(mc.fontRenderer, entry.getId(), x, y, width - x * 2, 20, -1)
                : new GuiLabel(mc.fontRenderer, entry.getId(), x, y, 150, 20, -1);
        label.visible = entry.shouldStartVisible();
        label.addLine(entry.getCaption());
        label.setCentered();
        return label;
    }

    /** Keys go to the focused text field; Tab moves between fields, Ctrl+V pastes a {@code ;}-separated list over the fields from the focused one on. */
    public void onKeyPressed(char typedChar, int keyCode) {
        if (!(focusedControl instanceof GuiTextField field)) return;
        if (!GuiScreen.isKeyComboCtrlV(keyCode)) {
            if (keyCode == 15) {
                field.setFocused(false);
                int k = editBoxes.indexOf(focusedControl);
                if (GuiScreen.isShiftKeyDown()) k = k <= 0 ? editBoxes.size() - 1 : k - 1;
                else k = k == editBoxes.size() - 1 ? 0 : k + 1;
                focusedControl = editBoxes.get(k);
                field = (GuiTextField) focusedControl;
                field.setFocused(true);
                int fieldBottom = field.y + slotHeight;
                if (fieldBottom > bottom) amountScrolled += (float) (fieldBottom - bottom);
                else if (field.y < top) amountScrolled = (float) field.y;
            } else {
                field.textboxKeyTyped(typedChar, keyCode);
            }
        } else {
            String[] parts = GuiScreen.getClipboardString().split(";");
            int start = editBoxes.indexOf(focusedControl);
            int j = start;
            for (String part : parts) {
                GuiTextField f = editBoxes.get(j);
                f.setText(part);
                f.setResponderEntryValue(f.getId(), part);
                j = j == editBoxes.size() - 1 ? 0 : j + 1;
                if (j == start) break;
            }
        }
    }

    @Override
    public GuiEntry getListEntry(int index) {
        return entries.get(index);
    }

    @Override
    public int getSize() {
        return entries.size();
    }

    @Override
    public int getListWidth() {
        return 400;
    }

    @Override
    protected int getScrollBarX() {
        return super.getScrollBarX() + 32;
    }

    public static class EditBoxEntry extends GuiListEntry {
        private final Predicate<String> filter;

        @SuppressWarnings("unchecked")
        public EditBoxEntry(int id, String caption, boolean startVisible, Predicate<String> filter) {
            super(id, caption, startVisible);
            this.filter = filter != null ? filter : (Predicate<String>) (Predicate<?>) Predicates.alwaysTrue();
        }

        public Predicate<String> getFilter() {
            return filter;
        }
    }

    public static class GuiButtonEntry extends GuiListEntry {
        private final boolean initialValue;

        public GuiButtonEntry(int id, String caption, boolean startVisible, boolean initialValue) {
            super(id, caption, startVisible);
            this.initialValue = initialValue;
        }

        public boolean getInitialValue() {
            return initialValue;
        }
    }

    /** One row: up to two controls, drawn at the row's y. */
    public static class GuiEntry implements GuiListExtended.IGuiListEntry {
        private final Minecraft client = Minecraft.getMinecraft();
        private final Gui component1;
        private final Gui component2;
        private Gui focusedControl;

        public GuiEntry(Gui component1, Gui component2) {
            this.component1 = component1;
            this.component2 = component2;
        }

        public Gui getComponent1() {
            return component1;
        }

        public Gui getComponent2() {
            return component2;
        }

        @Override
        public void drawEntry(int slotIndex, int x, int y, int listWidth, int slotHeight, int mouseX, int mouseY, boolean isSelected, float partialTicks) {
            renderComponent(component1, y, mouseX, mouseY, false, partialTicks);
            renderComponent(component2, y, mouseX, mouseY, false, partialTicks);
        }

        private void renderComponent(Gui component, int y, int mouseX, int mouseY, boolean positionOnly, float partialTicks) {
            if (component instanceof GuiButton b) {
                b.y = y;
                if (!positionOnly) b.drawButton(client, mouseX, mouseY, partialTicks);
            } else if (component instanceof GuiTextField t) {
                t.y = y;
                if (!positionOnly) t.drawTextBox();
            } else if (component instanceof GuiLabel l) {
                l.y = y;
                if (!positionOnly) l.drawLabel(client, mouseX, mouseY);
            }
        }

        @Override
        public void updatePosition(int slotIndex, int x, int y, float partialTicks) {
            renderComponent(component1, y, 0, 0, true, partialTicks);
            renderComponent(component2, y, 0, 0, true, partialTicks);
        }

        @Override
        public boolean mousePressed(int slotIndex, int mouseX, int mouseY, int mouseEvent, int relativeX, int relativeY) {
            boolean a = clickComponent(component1, mouseX, mouseY, mouseEvent);
            boolean b = clickComponent(component2, mouseX, mouseY, mouseEvent);
            return a || b;
        }

        private boolean clickComponent(Gui component, int mouseX, int mouseY, int mouseEvent) {
            if (component instanceof GuiButton b) {
                boolean pressed = b.mousePressed(client, mouseX, mouseY);
                if (pressed) focusedControl = b;
                return pressed;
            }
            if (component instanceof GuiTextField t) {
                t.mouseClicked(mouseX, mouseY, mouseEvent);
                if (t.isFocused()) focusedControl = t;
            }
            return false;
        }

        @Override
        public void mouseReleased(int slotIndex, int x, int y, int mouseEvent, int relativeX, int relativeY) {
            if (component1 instanceof GuiButton b) b.mouseReleased(x, y);
            if (component2 instanceof GuiButton b) b.mouseReleased(x, y);
        }
    }

    public static class GuiLabelEntry extends GuiListEntry {
        public GuiLabelEntry(int id, String caption, boolean startVisible) {
            super(id, caption, startVisible);
        }
    }

    public static class GuiListEntry {
        private final int id;
        private final String caption;
        private final boolean startVisible;

        public GuiListEntry(int id, String caption, boolean startVisible) {
            this.id = id;
            this.caption = caption;
            this.startVisible = startVisible;
        }

        public int getId() {
            return id;
        }

        public String getCaption() {
            return caption;
        }

        public boolean shouldStartVisible() {
            return startVisible;
        }
    }

    /** Told about every change of a control of the list (and of text fields and sliders given it directly). */
    public interface GuiResponder {
        void setEntryValue(int id, boolean value);

        void setEntryValue(int id, float value);

        void setEntryValue(int id, String value);
    }

    public static class GuiSlideEntry extends GuiListEntry {
        private final GuiSlider.FormatHelper formatter;
        private final float minValue;
        private final float maxValue;
        private final float initialValue;

        public GuiSlideEntry(int id, String caption, boolean startVisible, GuiSlider.FormatHelper formatter, float minValue, float maxValue, float initialValue) {
            super(id, caption, startVisible);
            this.formatter = formatter;
            this.minValue = minValue;
            this.maxValue = maxValue;
            this.initialValue = initialValue;
        }

        public GuiSlider.FormatHelper getFormatter() {
            return formatter;
        }

        public float getMinValue() {
            return minValue;
        }

        public float getMaxValue() {
            return maxValue;
        }

        public float getInitalValue() {
            return initialValue;
        }
    }
}
