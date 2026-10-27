package legacyconnector.v1_12_2.net.minecraft.client.gui.toasts;

import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/** A notification in the top right corner, drawn by the mod every frame until it says HIDE. */
public interface IToast {
    ResourceLocation TEXTURE_TOASTS = new ResourceLocation("textures/gui/toasts.png");
    Object NO_TOKEN = new Object();

    Visibility draw(GuiToast toastGui, long delta);

    default Object getType() {
        return NO_TOKEN;
    }

    enum Visibility {
        SHOW(net.minecraft.client.gui.components.toasts.Toast.Visibility.SHOW),
        HIDE(net.minecraft.client.gui.components.toasts.Toast.Visibility.HIDE);

        private final net.minecraft.client.gui.components.toasts.Toast.Visibility modern;

        Visibility(net.minecraft.client.gui.components.toasts.Toast.Visibility modern) {
            this.modern = modern;
        }

        public net.minecraft.client.gui.components.toasts.Toast.Visibility mcc$modern() {
            return modern;
        }

        public void playSound(legacyconnector.v1_12_2.net.minecraft.client.audio.SoundHandler handler) {
            modern.playSound(net.minecraft.client.Minecraft.getInstance().getSoundManager());
        }
    }
}
