package legacyconnector.v1_12_2.net.minecraft.client.gui.toasts;

import legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent;
import org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext;

/**
 * A plain text toast (the narrator toggle, tutorial hints): a title and an optional subtitle, shown five seconds after its last update.
 * {@link #addOrUpdate} goes to the modern system toast of the same kind; instances a mod adds itself draw the modern system toast frame.
 */
public class SystemToast implements IToast {
    private static final net.minecraft.resources.ResourceLocation BACKGROUND = net.minecraft.resources.ResourceLocation.withDefaultNamespace("toast/system");
    private final Type type;
    private String title;
    private String subtitle;
    private long firstDrawTime;
    private boolean newDisplay;

    public SystemToast(Type typeIn, ITextComponent titleComponent, ITextComponent subtitleComponent) {
        this.type = typeIn;
        this.title = titleComponent == null ? "" : titleComponent.getFormattedText();
        this.subtitle = subtitleComponent == null ? null : subtitleComponent.getFormattedText();
    }

    @Override
    public Visibility draw(GuiToast toastGui, long delta) {
        if (newDisplay) {
            firstDrawTime = delta;
            newDisplay = false;
        }
        var gui = RenderContext.gui();
        if (gui != null) {
            var font = net.minecraft.client.Minecraft.getInstance().font;
            gui.blitSprite(BACKGROUND, 0, 0, 160, 32);
            if (subtitle == null) {
                gui.drawString(font, title, 18, 12, -256, false);
            } else {
                gui.drawString(font, title, 18, 7, -256, false);
                gui.drawString(font, subtitle, 18, 18, -1, false);
            }
        }
        return delta - firstDrawTime < 5000L ? Visibility.SHOW : Visibility.HIDE;
    }

    public void setDisplayedText(ITextComponent titleComponent, ITextComponent subtitleComponent) {
        this.title = titleComponent == null ? "" : titleComponent.getFormattedText();
        this.subtitle = subtitleComponent == null ? null : subtitleComponent.getFormattedText();
        this.newDisplay = true;
    }

    @Override
    public Type getType() {
        return type;
    }

    /** Shows a system toast of this kind, or updates the one showing. */
    public static void addOrUpdate(GuiToast guiToast, Type type, ITextComponent title, ITextComponent subtitle) {
        net.minecraft.client.gui.components.toasts.SystemToast.addOrUpdate(net.minecraft.client.Minecraft.getInstance().getToasts(), type.modern,
                title == null ? net.minecraft.network.chat.Component.empty() : title.mcc$modern(), subtitle == null ? null : subtitle.mcc$modern());
    }

    public enum Type {
        NARRATOR_TOGGLE(net.minecraft.client.gui.components.toasts.SystemToast.SystemToastId.NARRATOR_TOGGLE),
        TUTORIAL_HINT(new net.minecraft.client.gui.components.toasts.SystemToast.SystemToastId());

        private final net.minecraft.client.gui.components.toasts.SystemToast.SystemToastId modern;

        Type(net.minecraft.client.gui.components.toasts.SystemToast.SystemToastId modern) {
            this.modern = modern;
        }
    }
}
