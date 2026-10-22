package legacyconnector.v1_7_10.net.minecraft.client.settings;

/**
 * 1.7.10's options. Values are the vanilla defaults; mods read a few (mipmaps, graphics, language). The shim does not
 * write to the modern options.
 */
public class GameSettings {
    public int mipmapLevels = 4;
    public int anisotropicFiltering = 1;
    public boolean fancyGraphics = true;
    public boolean ambientOcclusion = true;
    public int renderDistanceChunks = 8;
    public int particleSetting = 0;
    public float gammaSetting = 0.0F;
    public int thirdPersonView = 0;
    public boolean hideGUI = false;
    public boolean showDebugInfo = false;
    public String language = "en_US";
    public boolean advancedItemTooltips = false;
    public int guiScale = 0;
    public boolean anaglyph = false;

    public KeyBinding keyBindForward = vanilla("key.forward", m -> m.keyUp);
    public KeyBinding keyBindLeft = vanilla("key.left", m -> m.keyLeft);
    public KeyBinding keyBindBack = vanilla("key.back", m -> m.keyDown);
    public KeyBinding keyBindRight = vanilla("key.right", m -> m.keyRight);
    public KeyBinding keyBindJump = vanilla("key.jump", m -> m.keyJump);
    public KeyBinding keyBindSneak = vanilla("key.sneak", m -> m.keyShift);
    public KeyBinding keyBindSprint = vanilla("key.sprint", m -> m.keySprint);
    public KeyBinding keyBindInventory = vanilla("key.inventory", m -> m.keyInventory);
    public KeyBinding keyBindDrop = vanilla("key.drop", m -> m.keyDrop);
    public KeyBinding keyBindChat = vanilla("key.chat", m -> m.keyChat);
    public KeyBinding keyBindAttack = vanilla("key.attack", m -> m.keyAttack);
    public KeyBinding keyBindUseItem = vanilla("key.use", m -> m.keyUse);
    public KeyBinding keyBindPickBlock = vanilla("key.pickItem", m -> m.keyPickItem);
    public KeyBinding keyBindPlayerList = vanilla("key.playerlist", m -> m.keyPlayerList);
    public KeyBinding[] keyBindsHotbar = hotbar();
    public KeyBinding[] keyBindings = new KeyBinding[0];

    private static KeyBinding vanilla(String name, java.util.function.Function<net.minecraft.client.Options, net.minecraft.client.KeyMapping> get) {
        try {
            return new KeyBinding(get.apply(net.minecraft.client.Minecraft.getInstance().options));
        } catch (RuntimeException | LinkageError e) {
            return new KeyBinding(name, 0, "key.categories.misc");
        }
    }

    private static KeyBinding[] hotbar() {
        KeyBinding[] keys = new KeyBinding[9];
        for (int i = 0; i < 9; i++) {
            int slot = i;
            keys[i] = vanilla("key.hotbar." + (i + 1), o -> o.keyHotbarSlots[slot]);
        }
        return keys;
    }
}
