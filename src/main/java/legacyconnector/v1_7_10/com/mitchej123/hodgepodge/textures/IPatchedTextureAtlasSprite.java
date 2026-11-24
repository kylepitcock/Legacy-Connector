package legacyconnector.v1_7_10.com.mitchej123.hodgepodge.textures;

/**
 * Hodgepodge's interface on sprites that animate only when visible. The patch library is an engine mod the layer never loads; the interface is
 * kept so the mods that implement or test for it link, and no vanilla sprite implements it (a mod's {@code instanceof} test is false).
 */
public interface IPatchedTextureAtlasSprite {
    void markNeedsAnimationUpdate();

    boolean needsAnimationUpdate();

    void unmarkNeedsAnimationUpdate();

    void updateAnimationsDryRun();
}
