package legacyconnector.vb1_7_3.net.minecraft.client.gui.screen.option;

import legacyconnector.vb1_7_3.net.minecraft.client.gui.screen.Screen;
import legacyconnector.vb1_7_3.net.minecraft.client.gui.widget.ButtonWidget;
import legacyconnector.vb1_7_3.net.minecraft.client.gui.widget.OptionButtonWidget;
import legacyconnector.vb1_7_3.net.minecraft.client.option.GameOptions;
import legacyconnector.vb1_7_3.net.minecraft.client.option.Option;

/**
 * Beta's {@code GuiOptions}: one button per option (sliders for the volumes and sensitivity) over the game's own options, and Done back to
 * the screen that opened it. A mod's options screen that extends or opens this one gets the same buttons.
 */
public class OptionsScreen extends Screen {
    protected final Screen parent;
    protected final GameOptions options;
    protected String title = "Options";

    public OptionsScreen(Screen parent, GameOptions options) {
        this.parent = parent;
        this.options = options;
    }

    @Override public void init() {
        Option[] all = Option.values();
        for (int i = 0; i < all.length; i++) {
            Option option = all[i];
            int x = width / 2 - 155 + (i % 2) * 160, y = height / 6 + 24 * (i >> 1);
            buttons.add(option.isSlider()
                    ? new OptionButtonWidget(option.getId(), x, y, option, options.getString(option), options.getFloat(option))
                    : new OptionButtonWidget(option.getId(), x, y, option, options.getString(option)));
        }
        buttons.add(new ButtonWidget(200, width / 2 - 100, height / 6 + 168, 200, 20, "Done"));
    }

    @Override protected void buttonClicked(ButtonWidget button) {
        if (!button.active) return;
        if (button.id == 200) {
            options.save();
            minecraft.setScreen(parent);
        } else if (button instanceof OptionButtonWidget widget && widget.getOption() != null && !widget.getOption().isSlider()) {
            options.setInt(widget.getOption(), 1);
            widget.text = options.getString(widget.getOption());
        }
    }

    @Override public void render(int mouseX, int mouseY, float delta) {
        renderBackground();
        drawCenteredTextWithShadow(textRenderer, title, width / 2, 20, 0xFFFFFF);
        super.render(mouseX, mouseY, delta);
    }
}
