package legacyconnector.vb1_7_3.net.mine_diver.infsprites.api;

/**
 * InfSprites' render hook: mods registered one to rebind textures before the game drew an item or block from a sprite sheet beyond the
 * first. Mods built against InfSprites call every hook in {@link #renderHooks}; the layer registers none (its sprites are native textures).
 */
public interface IRenderHook {
    java.util.List<IRenderHook> renderHooks = new java.util.concurrent.CopyOnWriteArrayList<>();

    void overrideTexture(Object target);
}
