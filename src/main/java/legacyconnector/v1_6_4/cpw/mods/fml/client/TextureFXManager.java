package legacyconnector.v1_6_4.cpw.mods.fml.client;

import legacyconnector.v1_6_4.net.minecraft.client.renderer.texturefx.TextureFX;
import org.kylepitcock.legacyconnector.v1_6_4.bridge.Animations147;

/** FML 1.4.7's registry of animated tiles. */
public class TextureFXManager {
    private static final TextureFXManager INSTANCE = new TextureFXManager();

    public static TextureFXManager instance() {
        return INSTANCE;
    }

    public void addAnimation(TextureFX anim) {
        Animations147.register(anim);
    }

    public void removeAnimation(TextureFX anim) {
        Animations147.unregister(anim);
    }
}
