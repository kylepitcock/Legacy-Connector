package legacyconnector.v1_12_2.net.minecraft.client.gui.toasts;

import legacyconnector.v1_12_2.net.minecraft.advancements.Advancement;

/** The "Advancement Made!" toast of an advancement: drawn as the modern advancement toast (frame, icon, title and sound). */
public class AdvancementToast implements IToast {
    private final Advancement advancement;
    private boolean hasPlayedSound;
    private net.minecraft.client.gui.components.toasts.AdvancementToast mcc$modern;

    public AdvancementToast(Advancement advancementIn) {
        this.advancement = advancementIn;
    }

    @Override
    public Visibility draw(GuiToast toastGui, long delta) {
        var gui = org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext.gui();
        if (advancement == null || advancement.mcc$modern() == null) return Visibility.HIDE;
        if (mcc$modern == null) mcc$modern = new net.minecraft.client.gui.components.toasts.AdvancementToast(advancement.mcc$modern());
        if (gui == null) return delta >= 5000L ? Visibility.HIDE : Visibility.SHOW;
        var result = mcc$modern.render(gui, net.minecraft.client.Minecraft.getInstance().getToasts(), delta);
        hasPlayedSound = true;
        return result == net.minecraft.client.gui.components.toasts.Toast.Visibility.HIDE ? Visibility.HIDE : Visibility.SHOW;
    }
}
