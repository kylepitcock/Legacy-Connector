package legacyconnector.v1_20_1.net.minecraftforge.client.gui.overlay;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;

/** Forge 1.20.1's in-game GUI subclass, which overlays received as their first argument. A plain {@code Gui} of its own; the layer keeps one instance. */
public class ForgeGui extends Gui {
    public int leftHeight = 39;
    public int rightHeight = 39;
    private final Minecraft mcc$minecraft;

    public ForgeGui(Minecraft mc) {
        super(mc);
        this.mcc$minecraft = mc;
    }

    public Minecraft getMinecraft() {
        return mcc$minecraft;
    }

    public boolean shouldDrawSurvivalElements() {
        return mcc$minecraft.gameMode != null && mcc$minecraft.gameMode.canHurtPlayer() && mcc$minecraft.getCameraEntity() instanceof net.minecraft.world.entity.player.Player;
    }

    public void setupOverlayRenderState(boolean blend, boolean depthTest) {
        if (blend) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
        } else {
            RenderSystem.disableBlend();
        }
        if (depthTest) RenderSystem.enableDepthTest();
        else RenderSystem.disableDepthTest();
    }

    private static ForgeGui instance;

    /** The shared instance handed to overlays. */
    public static synchronized ForgeGui mcc$instance() {
        if (instance == null) instance = new ForgeGui(Minecraft.getInstance());
        return instance;
    }
}
