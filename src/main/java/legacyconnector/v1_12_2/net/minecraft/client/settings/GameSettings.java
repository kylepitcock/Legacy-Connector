package legacyconnector.v1_12_2.net.minecraft.client.settings;

/**
 * 1.12.2 options as a view over the modern ones: the fields mods read are phantoms answered from {@code Minecraft.options} (a few can be
 * set too); the key bindings wrap the game's own key mappings.
 */
public class GameSettings {
    @Deprecated
    public int thirdPersonView;
    @Deprecated
    public boolean fancyGraphics;
    @Deprecated
    public boolean advancedItemTooltips;
    @Deprecated
    public int particleSetting;
    @Deprecated
    public boolean showDebugInfo;
    @Deprecated
    public boolean hideGUI;
    @Deprecated
    public int mipmapLevels;
    @Deprecated
    public String language;
    @Deprecated
    public boolean anaglyph;
    @Deprecated
    public float fovSetting;
    @Deprecated
    public float gammaSetting;
    @Deprecated
    public int renderDistanceChunks;
    @Deprecated
    public boolean touchscreen;
    @Deprecated
    public int guiScale;
    @Deprecated
    public boolean forceUnicodeFont;
    @Deprecated
    public boolean invertMouse;
    @Deprecated
    public float mouseSensitivity;
    @Deprecated
    public boolean smoothCamera;
    @Deprecated
    public int ambientOcclusion;
    @Deprecated
    public int attackIndicator;
    @Deprecated
    public boolean chatLinksPrompt;
    @Deprecated
    public boolean reducedDebugInfo;
    @Deprecated
    public boolean viewBobbing;
    @Deprecated
    public float chatOpacity;
    @Deprecated
    public boolean fboEnable;
    @Deprecated
    public boolean useVbo;
    @Deprecated
    public boolean autoJump;
    @Deprecated
    public boolean heldItemTooltips;
    @Deprecated
    public boolean entityShadows;
    @Deprecated
    public boolean enableVsync;
    @Deprecated
    public boolean fullScreen;
    @Deprecated
    public int limitFramerate;
    @Deprecated
    public float chatScale;
    @Deprecated
    public float chatWidth;
    @Deprecated
    public boolean showSubtitles;
    @Deprecated
    public boolean hideServerAddress;
    @Deprecated
    public boolean pauseOnLostFocus;

    public KeyBinding keyBindForward = key(o -> o.keyUp);
    public KeyBinding keyBindLeft = key(o -> o.keyLeft);
    public KeyBinding keyBindBack = key(o -> o.keyDown);
    public KeyBinding keyBindRight = key(o -> o.keyRight);
    public KeyBinding keyBindJump = key(o -> o.keyJump);
    public KeyBinding keyBindSneak = key(o -> o.keyShift);
    public KeyBinding keyBindSprint = key(o -> o.keySprint);
    public KeyBinding keyBindInventory = key(o -> o.keyInventory);
    public KeyBinding keyBindSwapHands = key(o -> o.keySwapOffhand);
    public KeyBinding keyBindDrop = key(o -> o.keyDrop);
    public KeyBinding keyBindUseItem = key(o -> o.keyUse);
    public KeyBinding keyBindAttack = key(o -> o.keyAttack);
    public KeyBinding keyBindPickBlock = key(o -> o.keyPickItem);
    public KeyBinding keyBindChat = key(o -> o.keyChat);
    public KeyBinding keyBindPlayerList = key(o -> o.keyPlayerList);
    public KeyBinding keyBindCommand = key(o -> o.keyCommand);
    public KeyBinding keyBindScreenshot = key(o -> o.keyScreenshot);
    public KeyBinding keyBindTogglePerspective = key(o -> o.keyTogglePerspective);
    public KeyBinding keyBindSmoothCamera = key(o -> o.keySmoothCamera);
    public KeyBinding keyBindFullscreen = key(o -> o.keyFullscreen);
    public KeyBinding keyBindSpectatorOutlines = key(o -> o.keySpectatorOutlines);
    public KeyBinding keyBindAdvancements = key(o -> o.keyAdvancements);
    public KeyBinding keyBindSaveToolbar = key(o -> o.keySaveHotbarActivator);
    public KeyBinding keyBindLoadToolbar = key(o -> o.keyLoadHotbarActivator);
    public KeyBinding[] keyBindsHotbar = hotbar();
    /** All bindings: the game's and the ones mods registered. */
    @Deprecated
    public KeyBinding[] keyBindings;

    private static net.minecraft.client.Minecraft mc() {
        return net.minecraft.client.Minecraft.getInstance();
    }

    private static KeyBinding key(java.util.function.Function<net.minecraft.client.Options, net.minecraft.client.KeyMapping> get) {
        try {
            return KeyBinding.mcc$vanilla(get.apply(mc().options));
        } catch (RuntimeException | LinkageError e) {
            return new KeyBinding("key.legacy.unbound", 0, "key.categories.misc");
        }
    }

    private static KeyBinding[] hotbar() {
        KeyBinding[] out = new KeyBinding[9];
        for (int i = 0; i < 9; i++) {
            final int slot = i;
            out[i] = key(o -> o.keyHotbarSlots[slot]);
        }
        return out;
    }

    public static int mcc$get$thirdPersonView(GameSettings s) {
        try {
            var o = mc().options;
            return o.getCameraType().ordinal();
        } catch (RuntimeException | LinkageError e) {
            return 0;
        }
    }

    public static void mcc$set$thirdPersonView(GameSettings s, int v) {
        try {
            var o = mc().options;
            o.setCameraType(net.minecraft.client.CameraType.values()[Math.floorMod(v, 3)]);
        } catch (RuntimeException | LinkageError e) {
            // options not available (dedicated server)
        }
    }

    public static boolean mcc$get$fancyGraphics(GameSettings s) {
        try {
            var o = mc().options;
            return o.graphicsMode().get() != net.minecraft.client.GraphicsStatus.FAST;
        } catch (RuntimeException | LinkageError e) {
            return false;
        }
    }

    public static void mcc$set$fancyGraphics(GameSettings s, boolean v) {
        try {
            var o = mc().options;
            o.graphicsMode().set(v ? net.minecraft.client.GraphicsStatus.FANCY : net.minecraft.client.GraphicsStatus.FAST);
        } catch (RuntimeException | LinkageError e) {
            // options not available (dedicated server)
        }
    }

    public static boolean mcc$get$advancedItemTooltips(GameSettings s) {
        try {
            var o = mc().options;
            return o.advancedItemTooltips;
        } catch (RuntimeException | LinkageError e) {
            return false;
        }
    }

    public static void mcc$set$advancedItemTooltips(GameSettings s, boolean v) {
        try {
            var o = mc().options;
            o.advancedItemTooltips = v;
        } catch (RuntimeException | LinkageError e) {
            // options not available (dedicated server)
        }
    }

    public static int mcc$get$particleSetting(GameSettings s) {
        try {
            var o = mc().options;
            return o.particles().get().getId();
        } catch (RuntimeException | LinkageError e) {
            return 0;
        }
    }

    public static void mcc$set$particleSetting(GameSettings s, int v) {
        try {
            var o = mc().options;
            o.particles().set(net.minecraft.client.ParticleStatus.byId(v));
        } catch (RuntimeException | LinkageError e) {
            // options not available (dedicated server)
        }
    }

    public static boolean mcc$get$showDebugInfo(GameSettings s) {
        try {
            var mc = mc();
            return mc.getDebugOverlay().showDebugScreen();
        } catch (RuntimeException | LinkageError e) {
            return false;
        }
    }

    public static void mcc$set$showDebugInfo(GameSettings s, boolean v) {
        // follows the modern option; mods cannot change it here
    }

    public static boolean mcc$get$hideGUI(GameSettings s) {
        try {
            var o = mc().options;
            return o.hideGui;
        } catch (RuntimeException | LinkageError e) {
            return false;
        }
    }

    public static void mcc$set$hideGUI(GameSettings s, boolean v) {
        try {
            var o = mc().options;
            o.hideGui = v;
        } catch (RuntimeException | LinkageError e) {
            // options not available (dedicated server)
        }
    }

    public static int mcc$get$mipmapLevels(GameSettings s) {
        try {
            var o = mc().options;
            return o.mipmapLevels().get();
        } catch (RuntimeException | LinkageError e) {
            return 0;
        }
    }

    public static void mcc$set$mipmapLevels(GameSettings s, int v) {
        // follows the modern option; mods cannot change it here
    }

    public static String mcc$get$language(GameSettings s) {
        try {
            var o = mc().options;
            return o.languageCode;
        } catch (RuntimeException | LinkageError e) {
            return "en_us";
        }
    }

    public static void mcc$set$language(GameSettings s, String v) {
        // follows the modern option; mods cannot change it here
    }

    public static boolean mcc$get$anaglyph(GameSettings s) {
        return false;
    }

    public static void mcc$set$anaglyph(GameSettings s, boolean v) {
        // follows the modern option; mods cannot change it here
    }

    public static float mcc$get$fovSetting(GameSettings s) {
        try {
            var o = mc().options;
            return (float) (int) o.fov().get();
        } catch (RuntimeException | LinkageError e) {
            return 0.0F;
        }
    }

    public static void mcc$set$fovSetting(GameSettings s, float v) {
        try {
            var o = mc().options;
            o.fov().set((int) v);
        } catch (RuntimeException | LinkageError e) {
            // options not available (dedicated server)
        }
    }

    public static float mcc$get$gammaSetting(GameSettings s) {
        try {
            var o = mc().options;
            return o.gamma().get().floatValue();
        } catch (RuntimeException | LinkageError e) {
            return 0.0F;
        }
    }

    public static void mcc$set$gammaSetting(GameSettings s, float v) {
        try {
            var o = mc().options;
            o.gamma().set((double) v);
        } catch (RuntimeException | LinkageError e) {
            // options not available (dedicated server)
        }
    }

    public static int mcc$get$renderDistanceChunks(GameSettings s) {
        try {
            var o = mc().options;
            return o.getEffectiveRenderDistance();
        } catch (RuntimeException | LinkageError e) {
            return 0;
        }
    }

    public static void mcc$set$renderDistanceChunks(GameSettings s, int v) {
        // follows the modern option; mods cannot change it here
    }

    public static boolean mcc$get$touchscreen(GameSettings s) {
        try {
            var o = mc().options;
            return o.touchscreen().get();
        } catch (RuntimeException | LinkageError e) {
            return false;
        }
    }

    public static void mcc$set$touchscreen(GameSettings s, boolean v) {
        // follows the modern option; mods cannot change it here
    }

    public static int mcc$get$guiScale(GameSettings s) {
        try {
            var o = mc().options;
            return o.guiScale().get();
        } catch (RuntimeException | LinkageError e) {
            return 0;
        }
    }

    public static void mcc$set$guiScale(GameSettings s, int v) {
        // follows the modern option; mods cannot change it here
    }

    public static boolean mcc$get$forceUnicodeFont(GameSettings s) {
        try {
            var o = mc().options;
            return o.forceUnicodeFont().get();
        } catch (RuntimeException | LinkageError e) {
            return false;
        }
    }

    public static void mcc$set$forceUnicodeFont(GameSettings s, boolean v) {
        // follows the modern option; mods cannot change it here
    }

    public static boolean mcc$get$invertMouse(GameSettings s) {
        try {
            var o = mc().options;
            return o.invertYMouse().get();
        } catch (RuntimeException | LinkageError e) {
            return false;
        }
    }

    public static void mcc$set$invertMouse(GameSettings s, boolean v) {
        // follows the modern option; mods cannot change it here
    }

    public static float mcc$get$mouseSensitivity(GameSettings s) {
        try {
            var o = mc().options;
            return o.sensitivity().get().floatValue();
        } catch (RuntimeException | LinkageError e) {
            return 0.0F;
        }
    }

    public static void mcc$set$mouseSensitivity(GameSettings s, float v) {
        // follows the modern option; mods cannot change it here
    }

    public static boolean mcc$get$smoothCamera(GameSettings s) {
        try {
            var o = mc().options;
            return o.smoothCamera;
        } catch (RuntimeException | LinkageError e) {
            return false;
        }
    }

    public static void mcc$set$smoothCamera(GameSettings s, boolean v) {
        try {
            var o = mc().options;
            o.smoothCamera = v;
        } catch (RuntimeException | LinkageError e) {
            // options not available (dedicated server)
        }
    }

    public static int mcc$get$ambientOcclusion(GameSettings s) {
        try {
            var o = mc().options;
            return o.ambientOcclusion().get() ? 2 : 0;
        } catch (RuntimeException | LinkageError e) {
            return 0;
        }
    }

    public static void mcc$set$ambientOcclusion(GameSettings s, int v) {
        // follows the modern option; mods cannot change it here
    }

    public static int mcc$get$attackIndicator(GameSettings s) {
        try {
            var o = mc().options;
            return o.attackIndicator().get().getId();
        } catch (RuntimeException | LinkageError e) {
            return 0;
        }
    }

    public static void mcc$set$attackIndicator(GameSettings s, int v) {
        // follows the modern option; mods cannot change it here
    }

    public static boolean mcc$get$chatLinksPrompt(GameSettings s) {
        try {
            var o = mc().options;
            return o.chatLinksPrompt().get();
        } catch (RuntimeException | LinkageError e) {
            return false;
        }
    }

    public static void mcc$set$chatLinksPrompt(GameSettings s, boolean v) {
        // follows the modern option; mods cannot change it here
    }

    public static boolean mcc$get$reducedDebugInfo(GameSettings s) {
        try {
            var o = mc().options;
            return o.reducedDebugInfo().get();
        } catch (RuntimeException | LinkageError e) {
            return false;
        }
    }

    public static void mcc$set$reducedDebugInfo(GameSettings s, boolean v) {
        // follows the modern option; mods cannot change it here
    }

    public static boolean mcc$get$viewBobbing(GameSettings s) {
        try {
            var o = mc().options;
            return o.bobView().get();
        } catch (RuntimeException | LinkageError e) {
            return false;
        }
    }

    public static void mcc$set$viewBobbing(GameSettings s, boolean v) {
        try {
            var o = mc().options;
            o.bobView().set(v);
        } catch (RuntimeException | LinkageError e) {
            // options not available (dedicated server)
        }
    }

    public static float mcc$get$chatOpacity(GameSettings s) {
        try {
            var o = mc().options;
            return o.chatOpacity().get().floatValue();
        } catch (RuntimeException | LinkageError e) {
            return 0.0F;
        }
    }

    public static void mcc$set$chatOpacity(GameSettings s, float v) {
        // follows the modern option; mods cannot change it here
    }

    public static boolean mcc$get$fboEnable(GameSettings s) {
        return true;
    }

    public static void mcc$set$fboEnable(GameSettings s, boolean v) {
        // follows the modern option; mods cannot change it here
    }

    public static boolean mcc$get$useVbo(GameSettings s) {
        return true;
    }

    public static void mcc$set$useVbo(GameSettings s, boolean v) {
        // follows the modern option; mods cannot change it here
    }

    public static boolean mcc$get$autoJump(GameSettings s) {
        try {
            var o = mc().options;
            return o.autoJump().get();
        } catch (RuntimeException | LinkageError e) {
            return false;
        }
    }

    public static void mcc$set$autoJump(GameSettings s, boolean v) {
        // follows the modern option; mods cannot change it here
    }

    public static boolean mcc$get$heldItemTooltips(GameSettings s) {
        return true;
    }

    public static void mcc$set$heldItemTooltips(GameSettings s, boolean v) {
        // follows the modern option; mods cannot change it here
    }

    public static boolean mcc$get$entityShadows(GameSettings s) {
        try {
            var o = mc().options;
            return o.entityShadows().get();
        } catch (RuntimeException | LinkageError e) {
            return false;
        }
    }

    public static void mcc$set$entityShadows(GameSettings s, boolean v) {
        // follows the modern option; mods cannot change it here
    }

    public static boolean mcc$get$enableVsync(GameSettings s) {
        try {
            var o = mc().options;
            return o.enableVsync().get();
        } catch (RuntimeException | LinkageError e) {
            return false;
        }
    }

    public static void mcc$set$enableVsync(GameSettings s, boolean v) {
        // follows the modern option; mods cannot change it here
    }

    public static boolean mcc$get$fullScreen(GameSettings s) {
        try {
            var o = mc().options;
            return o.fullscreen().get();
        } catch (RuntimeException | LinkageError e) {
            return false;
        }
    }

    public static void mcc$set$fullScreen(GameSettings s, boolean v) {
        // follows the modern option; mods cannot change it here
    }

    public static int mcc$get$limitFramerate(GameSettings s) {
        try {
            var o = mc().options;
            return o.framerateLimit().get();
        } catch (RuntimeException | LinkageError e) {
            return 0;
        }
    }

    public static void mcc$set$limitFramerate(GameSettings s, int v) {
        // follows the modern option; mods cannot change it here
    }

    public static float mcc$get$chatScale(GameSettings s) {
        try {
            var o = mc().options;
            return o.chatScale().get().floatValue();
        } catch (RuntimeException | LinkageError e) {
            return 0.0F;
        }
    }

    public static void mcc$set$chatScale(GameSettings s, float v) {
        // follows the modern option; mods cannot change it here
    }

    public static float mcc$get$chatWidth(GameSettings s) {
        try {
            var o = mc().options;
            return o.chatWidth().get().floatValue();
        } catch (RuntimeException | LinkageError e) {
            return 0.0F;
        }
    }

    public static void mcc$set$chatWidth(GameSettings s, float v) {
        // follows the modern option; mods cannot change it here
    }

    public static boolean mcc$get$showSubtitles(GameSettings s) {
        try {
            var o = mc().options;
            return o.showSubtitles().get();
        } catch (RuntimeException | LinkageError e) {
            return false;
        }
    }

    public static void mcc$set$showSubtitles(GameSettings s, boolean v) {
        // follows the modern option; mods cannot change it here
    }

    public static boolean mcc$get$hideServerAddress(GameSettings s) {
        try {
            var o = mc().options;
            return o.hideServerAddress;
        } catch (RuntimeException | LinkageError e) {
            return false;
        }
    }

    public static void mcc$set$hideServerAddress(GameSettings s, boolean v) {
        // follows the modern option; mods cannot change it here
    }

    public static boolean mcc$get$pauseOnLostFocus(GameSettings s) {
        try {
            var o = mc().options;
            return o.pauseOnLostFocus;
        } catch (RuntimeException | LinkageError e) {
            return false;
        }
    }

    public static void mcc$set$pauseOnLostFocus(GameSettings s, boolean v) {
        // follows the modern option; mods cannot change it here
    }

    public static KeyBinding[] mcc$get$keyBindings(GameSettings s) {
        java.util.List<KeyBinding> all = new java.util.ArrayList<>();
        for (var f : new KeyBinding[] {s.keyBindForward, s.keyBindLeft, s.keyBindBack, s.keyBindRight, s.keyBindJump, s.keyBindSneak, s.keyBindSprint,
                s.keyBindInventory, s.keyBindSwapHands, s.keyBindDrop, s.keyBindUseItem, s.keyBindAttack, s.keyBindPickBlock, s.keyBindChat,
                s.keyBindPlayerList, s.keyBindCommand, s.keyBindScreenshot, s.keyBindTogglePerspective, s.keyBindSmoothCamera, s.keyBindFullscreen,
                s.keyBindSpectatorOutlines, s.keyBindAdvancements, s.keyBindSaveToolbar, s.keyBindLoadToolbar}) all.add(f);
        java.util.Collections.addAll(all, s.keyBindsHotbar);
        all.addAll(KeyBinding.mcc$registered());
        return all.toArray(new KeyBinding[0]);
    }

    public static void mcc$set$keyBindings(GameSettings s, KeyBinding[] v) {
        // mods append their bindings to the array (pre-ClientRegistry style): register the new ones
        if (v != null) for (KeyBinding k : v) if (k != null) KeyBinding.mcc$register(k);
    }

    /** Whether the binding's key (or mouse button) is held right now. */
    public static boolean isKeyDown(KeyBinding key) {
        int code = key.getKeyCode();
        if (code == 0) return false;
        try {
            long window = mc().getWindow().getWindow();
            return code < 0
                    ? org.lwjgl.glfw.GLFW.glfwGetMouseButton(window, code + 100) == org.lwjgl.glfw.GLFW.GLFW_PRESS
                    : com.mojang.blaze3d.platform.InputConstants.isKeyDown(window, org.kylepitcock.legacyconnector.v1_7_10.client.KeyCodes.toGlfw(code));
        } catch (RuntimeException | LinkageError e) {
            return false;
        }
    }

    /** The display name of an LWJGL 2 key code (mouse buttons are negative). */
    public static String getKeyDisplayString(int key) {
        if (key < 0) {
            return switch (key) {
                case -100 -> net.minecraft.locale.Language.getInstance().getOrDefault("key.mouse.left");
                case -99 -> net.minecraft.locale.Language.getInstance().getOrDefault("key.mouse.right");
                case -98 -> net.minecraft.locale.Language.getInstance().getOrDefault("key.mouse.middle");
                default -> legacyconnector.v1_12_2.net.minecraft.client.resources.I18n.format("key.mouseButton", key + 101);
            };
        }
        int glfw = org.kylepitcock.legacyconnector.v1_7_10.client.KeyCodes.toGlfw(key);
        if (glfw <= 0) return "Key " + key;
        return com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM.getOrCreate(glfw).getDisplayName().getString();
    }

    /** The volume of a sound category (0-1, the master volume not included). */
    public float getSoundLevel(legacyconnector.v1_12_2.net.minecraft.util.SoundCategory category) {
        try {
            return mc().options.getSoundSourceVolume(net.minecraft.sounds.SoundSource.valueOf(category.name()));
        } catch (RuntimeException | LinkageError e) {
            return 1.0F;
        }
    }

    public void setSoundLevel(legacyconnector.v1_12_2.net.minecraft.util.SoundCategory category, float volume) {
        try {
            mc().options.getSoundSourceOptionInstance(net.minecraft.sounds.SoundSource.valueOf(category.name())).set((double) volume);
        } catch (RuntimeException | LinkageError e) {
            // no client options
        }
    }

    public void saveOptions() {
        try {
            mc().options.save();
        } catch (RuntimeException | LinkageError e) {
            // no client options
        }
    }

    public void loadOptions() {
    }

    public void setOptionKeyBinding(KeyBinding key, int keyCode) {
        key.setKeyCode(keyCode);
        saveOptions();
    }
}
