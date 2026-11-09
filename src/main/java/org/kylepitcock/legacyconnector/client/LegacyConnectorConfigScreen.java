package org.kylepitcock.legacyconnector.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.kylepitcock.legacyconnector.core.LegacyConfig;
import org.slf4j.LoggerFactory;

/**
 * Editor for {@code config/legacyconnector.properties}: one row per setting (a toggle for true/false, a text box otherwise) with the
 * file's comment as the tooltip. Opened from the mod list (Legacy Connector, Config) and from a button on the Options screen.
 */
public final class LegacyConnectorConfigScreen extends Screen {
    private final Screen parent;
    private final List<Row> rows = new ArrayList<>();
    private SettingsList list;

    public LegacyConnectorConfigScreen(Screen parent) {
        super(Component.literal("Legacy Connector settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        list = new SettingsList(minecraft, width, height - 64, 32);
        if (rows.isEmpty()) {
            for (LegacyConfig.Entry e : LegacyConfig.entries()) rows.add(new Row(e));
        }
        for (Row r : rows) list.add(r);
        addRenderableWidget(list);
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> saveAndClose()).bounds(width / 2 - 155, height - 28, 150, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, b -> onClose()).bounds(width / 2 + 5, height - 28, 150, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        super.render(g, mouseX, mouseY, partial);
        g.drawCenteredString(font, title, width / 2, 8, 0xFFFFFF);
        g.drawCenteredString(font, Component.literal("Hover a setting for its description. Most changes apply after a restart."), width / 2, 20, 0xA0A0A0);
    }

    private void saveAndClose() {
        Map<String, String> changed = new LinkedHashMap<>();
        for (Row r : rows) {
            String now = r.value();
            if (!now.equals(r.initial)) changed.put(r.entry.key(), now);
        }
        if (!changed.isEmpty()) {
            try {
                LegacyConfig.save(changed);
            } catch (IOException e) {
                LoggerFactory.getLogger("LegacyConnector/Config").warn("Could not save legacyconnector.properties: {}", e.toString());
            }
        }
        minecraft.setScreen(parent);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    /** One setting: its widget and the value it started with. */
    private static final class Row {
        final LegacyConfig.Entry entry;
        final String initial;
        boolean flag;
        EditBox text;

        Row(LegacyConfig.Entry entry) {
            this.entry = entry;
            this.initial = LegacyConfig.currentValue(entry.key(), entry.defaultValue());
            this.flag = initial.equalsIgnoreCase("true");
        }

        String value() {
            if (entry.isBoolean()) return Boolean.toString(flag);
            return text != null ? text.getValue().trim() : initial;
        }
    }

    private final class SettingsList extends ContainerObjectSelectionList<SettingsList.Line> {
        SettingsList(Minecraft mc, int width, int height, int y) {
            super(mc, width, height, y, 24);
        }

        void add(Row row) {
            addEntry(new Line(row));
        }

        @Override
        public int getRowWidth() {
            return Math.min(width - 40, 420);
        }

        @Override
        protected int getScrollbarPosition() {
            return width / 2 + getRowWidth() / 2 + 6;
        }

        final class Line extends ContainerObjectSelectionList.Entry<Line> {
            private final Row row;
            private final AbstractWidget widget;

            Line(Row row) {
                this.row = row;
                Tooltip tip = Tooltip.create(Component.literal(row.entry.description().isEmpty() ? row.entry.key() : row.entry.description()));
                if (row.entry.isBoolean()) {
                    Button b = Button.builder(label(row), btn -> {
                        row.flag = !row.flag;
                        btn.setMessage(label(row));
                    }).bounds(0, 0, 100, 20).build();
                    widget = b;
                } else {
                    EditBox box = new EditBox(font, 0, 0, 100, 20, Component.literal(row.entry.key()));
                    box.setMaxLength(512);
                    box.setValue(row.initial);
                    row.text = box;
                    widget = box;
                }
                widget.setTooltip(tip);
            }

            private Component label(Row r) {
                return r.flag ? CommonComponents.OPTION_ON : CommonComponents.OPTION_OFF;
            }

            @Override
            public void render(GuiGraphics g, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovering, float partial) {
                String name = row.entry.key();
                g.drawString(font, name, left, top + 6, 0xFFFFFF);
                widget.setX(left + width - 110);
                widget.setY(top);
                widget.setWidth(110);
                widget.render(g, mouseX, mouseY, partial);
            }

            @Override
            public List<? extends GuiEventListener> children() {
                return List.of(widget);
            }

            @Override
            public List<? extends NarratableEntry> narratables() {
                return List.of(widget);
            }
        }
    }
}
