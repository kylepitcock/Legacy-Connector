package legacyconnector.vb1_7_3.net.minecraft.client.render;

/**
 * Beta's {@code RenderGlobal}, as far as Forge's highlight handlers use it: the mining progress of the targeted block, the texture
 * manager, and the standard outline ({@link #renderBlockOutline}), which the native game draws when a handler asks for it.
 */
public class WorldRenderer {
    private static final WorldRenderer INSTANCE = new WorldRenderer();
    /** Beta's {@code damagePartialTime}: 0 when the targeted block is not being mined. */
    public float miningProgress;
    public legacyconnector.vb1_7_3.net.minecraft.client.texture.TextureManager textureManager =
            legacyconnector.vb1_7_3.net.minecraft.client.texture.TextureManager.mcc$instance();
    private boolean defaultOutline;

    protected WorldRenderer() {}

    public static WorldRenderer mcc$instance() { return INSTANCE; }

    /** Beta's {@code drawSelectionBox}: the standard outline of the target, which the native game draws. */
    public void renderBlockOutline(legacyconnector.vb1_7_3.net.minecraft.entity.player.PlayerEntity player,
            legacyconnector.vb1_7_3.net.minecraft.util.hit.HitResult target, int mode,
            legacyconnector.vb1_7_3.net.minecraft.item.ItemStack held, float partial) {
        defaultOutline = true;
    }

    /** Starts a highlight call; true afterwards when a handler asked for the standard outline. */
    public void mcc$begin(float progress) { miningProgress = progress; defaultOutline = false; }
    public boolean mcc$defaultOutline() { return defaultOutline; }
}
