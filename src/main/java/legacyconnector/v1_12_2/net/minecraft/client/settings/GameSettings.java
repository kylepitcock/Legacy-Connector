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

    // ------------------------------------------------------------------ area R2b: more options, the Options enum

    /** Phantoms answered from the game's options. */
    @Deprecated
    public boolean chatColours;
    @Deprecated
    public float chatHeightFocused;
    @Deprecated
    public int clouds;
    @Deprecated
    public boolean showDebugProfilerChart;
    @Deprecated
    public boolean showLagometer;
    @Deprecated
    public legacyconnector.v1_12_2.net.minecraft.util.EnumHandSide mainHand;
    @Deprecated
    public legacyconnector.v1_12_2.net.minecraft.world.EnumDifficulty difficulty;

    /** Phantom: which chat messages show (the game's chat visibility option). */
    @Deprecated
    public legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer.EnumChatVisibility chatVisibility;

    public static legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer.EnumChatVisibility mcc$get$chatVisibility(GameSettings s) {
        if (!net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) return legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer.EnumChatVisibility.FULL;
        return legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer.EnumChatVisibility.mcc$of(net.minecraft.client.Minecraft.getInstance().options.chatVisibility().get());
    }

    public static void mcc$set$chatVisibility(GameSettings s, legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer.EnumChatVisibility v) {
        if (v != null && net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) net.minecraft.client.Minecraft.getInstance().options.chatVisibility().set(v.mcc$modern());
    }

    public static boolean mcc$get$chatColours(GameSettings s) {
        return Opt.bool(Options.CHAT_COLOR);
    }

    public static void mcc$set$chatColours(GameSettings s, boolean v) {
        Opt.setBool(Options.CHAT_COLOR, v);
    }

    public static float mcc$get$chatHeightFocused(GameSettings s) {
        return Opt.floatValue(Options.CHAT_HEIGHT_FOCUSED);
    }

    public static void mcc$set$chatHeightFocused(GameSettings s, float v) {
        Opt.setFloat(Options.CHAT_HEIGHT_FOCUSED, v);
    }

    /** 0 off, 1 fast, 2 fancy. */
    public static int mcc$get$clouds(GameSettings s) {
        try {
            return switch (mc().options.cloudStatus().get()) {
                case OFF -> 0;
                case FAST -> 1;
                default -> 2;
            };
        } catch (RuntimeException | LinkageError e) {
            return 2;
        }
    }

    public static void mcc$set$clouds(GameSettings s, int v) {
        try {
            mc().options.cloudStatus().set(v <= 0 ? net.minecraft.client.CloudStatus.OFF : v == 1 ? net.minecraft.client.CloudStatus.FAST : net.minecraft.client.CloudStatus.FANCY);
        } catch (RuntimeException | LinkageError e) {
            // no client options
        }
    }

    public static boolean mcc$get$showDebugProfilerChart(GameSettings s) {
        try {
            return mc().getDebugOverlay().showProfilerChart();
        } catch (RuntimeException | LinkageError e) {
            return false;
        }
    }

    public static boolean mcc$get$showLagometer(GameSettings s) {
        try {
            return mc().getDebugOverlay().showFpsCharts();
        } catch (RuntimeException | LinkageError e) {
            return false;
        }
    }

    public static legacyconnector.v1_12_2.net.minecraft.util.EnumHandSide mcc$get$mainHand(GameSettings s) {
        try {
            return legacyconnector.v1_12_2.net.minecraft.util.EnumHandSide.mcc$of(mc().options.mainHand().get());
        } catch (RuntimeException | LinkageError e) {
            return legacyconnector.v1_12_2.net.minecraft.util.EnumHandSide.RIGHT;
        }
    }

    public static void mcc$set$mainHand(GameSettings s, legacyconnector.v1_12_2.net.minecraft.util.EnumHandSide v) {
        try {
            if (v != null) mc().options.mainHand().set(v.mcc$modern());
        } catch (RuntimeException | LinkageError e) {
            // no client options
        }
    }

    /** The difficulty of the world played in (the game keeps no client difficulty option any more). */
    public static legacyconnector.v1_12_2.net.minecraft.world.EnumDifficulty mcc$get$difficulty(GameSettings s) {
        try {
            var level = mc().level;
            return legacyconnector.v1_12_2.net.minecraft.world.EnumDifficulty.mcc$of(level == null ? net.minecraft.world.Difficulty.NORMAL : level.getDifficulty());
        } catch (RuntimeException | LinkageError e) {
            return legacyconnector.v1_12_2.net.minecraft.world.EnumDifficulty.NORMAL;
        }
    }

    /** The value of an on/off option. */
    public boolean getOptionOrdinalValue(Options option) {
        return Opt.bool(option);
    }

    /** Steps an option: on/off options flip, the others move to their next value. */
    public void setOptionValue(Options option, int value) {
        if (option.isBoolean()) {
            Opt.setBool(option, !Opt.bool(option));
        } else {
            Opt.cycle(option, value);
        }
        saveOptions();
    }

    public float getOptionFloatValue(Options option) {
        return Opt.floatValue(option);
    }

    public void setOptionFloatValue(Options option, float value) {
        Opt.setFloat(option, value);
    }

    /** The option's button text ("FOV: 70", "Auto-Jump: ON"). */
    public String getKeyBinding(Options option) {
        String label = legacyconnector.v1_12_2.net.minecraft.client.resources.I18n.format(option.getTranslation()) + ": ";
        if (option.isBoolean()) return label + legacyconnector.v1_12_2.net.minecraft.client.resources.I18n.format(Opt.bool(option) ? "options.on" : "options.off");
        if (option.isFloat()) return label + Math.round(Opt.floatValue(option));
        return label + Opt.text(option);
    }

    /** The bridge between the legacy options and the game's. */
    private static final class Opt {
        private Opt() {}

        static net.minecraft.client.OptionInstance<Boolean> boolOption(Options option) {
            var o = mc().options;
            return switch (option) {
                case INVERT_MOUSE -> o.invertYMouse();
                case VIEW_BOBBING -> o.bobView();
                case CHAT_COLOR -> o.chatColors();
                case CHAT_LINKS -> o.chatLinks();
                case CHAT_LINKS_PROMPT -> o.chatLinksPrompt();
                case USE_FULLSCREEN -> o.fullscreen();
                case ENABLE_VSYNC -> o.enableVsync();
                case TOUCHSCREEN -> o.touchscreen();
                case FORCE_UNICODE_FONT -> o.forceUnicodeFont();
                case REDUCED_DEBUG_INFO -> o.reducedDebugInfo();
                case ENTITY_SHADOWS -> o.entityShadows();
                case SHOW_SUBTITLES -> o.showSubtitles();
                case REALMS_NOTIFICATIONS -> o.realmsNotifications();
                case AUTO_JUMP -> o.autoJump();
                default -> null;
            };
        }

        static boolean bool(Options option) {
            try {
                if (option == Options.FBO_ENABLE || option == Options.USE_VBO) return true;
                var o = boolOption(option);
                return o != null && o.get();
            } catch (RuntimeException | LinkageError e) {
                return false;
            }
        }

        static void setBool(Options option, boolean value) {
            try {
                var o = boolOption(option);
                if (o != null) o.set(value);
            } catch (RuntimeException | LinkageError e) {
                // no client options
            }
        }

        static float floatValue(Options option) {
            try {
                var o = mc().options;
                return switch (option) {
                    case SENSITIVITY -> o.sensitivity().get().floatValue();
                    case FOV -> o.fov().get();
                    case GAMMA -> o.gamma().get().floatValue();
                    case RENDER_DISTANCE -> o.renderDistance().get();
                    case FRAMERATE_LIMIT -> o.framerateLimit().get();
                    case CHAT_OPACITY -> o.chatOpacity().get().floatValue();
                    case CHAT_SCALE -> o.chatScale().get().floatValue();
                    case CHAT_WIDTH -> o.chatWidth().get().floatValue();
                    case CHAT_HEIGHT_FOCUSED -> o.chatHeightFocused().get().floatValue();
                    case CHAT_HEIGHT_UNFOCUSED -> o.chatHeightUnfocused().get().floatValue();
                    case MIPMAP_LEVELS -> o.mipmapLevels().get();
                    default -> 0.0F;
                };
            } catch (RuntimeException | LinkageError e) {
                return 0.0F;
            }
        }

        static void setFloat(Options option, float v) {
            try {
                var o = mc().options;
                switch (option) {
                    case SENSITIVITY -> o.sensitivity().set((double) v);
                    case FOV -> o.fov().set(Math.round(v));
                    case GAMMA -> o.gamma().set((double) v);
                    case RENDER_DISTANCE -> o.renderDistance().set(Math.round(v));
                    case FRAMERATE_LIMIT -> o.framerateLimit().set(Math.round(v));
                    case CHAT_OPACITY -> o.chatOpacity().set((double) v);
                    case CHAT_SCALE -> o.chatScale().set((double) v);
                    case CHAT_WIDTH -> o.chatWidth().set((double) v);
                    case CHAT_HEIGHT_FOCUSED -> o.chatHeightFocused().set((double) v);
                    case CHAT_HEIGHT_UNFOCUSED -> o.chatHeightUnfocused().set((double) v);
                    case MIPMAP_LEVELS -> o.mipmapLevels().set(Math.round(v));
                    default -> {
                    }
                }
            } catch (RuntimeException | LinkageError e) {
                // no client options, or a value the option does not take
            }
        }

        static <T> T next(T[] values, T current, int step) {
            int i = java.util.Arrays.asList(values).indexOf(current);
            return values[Math.floorMod(i + step, values.length)];
        }

        static void cycle(Options option, int step) {
            try {
                var o = mc().options;
                switch (option) {
                    case GUI_SCALE -> o.guiScale().set(Math.floorMod(o.guiScale().get() + step, 5));
                    case PARTICLES -> o.particles().set(next(net.minecraft.client.ParticleStatus.values(), o.particles().get(), step));
                    case GRAPHICS -> o.graphicsMode().set(next(net.minecraft.client.GraphicsStatus.values(), o.graphicsMode().get(), step));
                    case RENDER_CLOUDS -> o.cloudStatus().set(next(net.minecraft.client.CloudStatus.values(), o.cloudStatus().get(), step));
                    case MAIN_HAND -> o.mainHand().set(o.mainHand().get().getOpposite());
                    case AMBIENT_OCCLUSION -> o.ambientOcclusion().set(!o.ambientOcclusion().get());
                    case CHAT_VISIBILITY -> o.chatVisibility().set(next(net.minecraft.world.entity.player.ChatVisiblity.values(), o.chatVisibility().get(), step));
                    default -> {
                    }
                }
                if (option == Options.GUI_SCALE) mc().resizeDisplay();
            } catch (RuntimeException | LinkageError e) {
                // no client options
            }
        }

        static String text(Options option) {
            try {
                var o = mc().options;
                return switch (option) {
                    case GUI_SCALE -> o.guiScale().get() == 0 ? legacyconnector.v1_12_2.net.minecraft.client.resources.I18n.format("options.guiScale.auto")
                            : String.valueOf(o.guiScale().get());
                    case PARTICLES -> o.particles().get().getCaption().getString();
                    case GRAPHICS -> o.graphicsMode().get().getCaption().getString();
                    case RENDER_CLOUDS -> o.cloudStatus().get().getCaption().getString();
                    case MAIN_HAND -> o.mainHand().get().getCaption().getString();
                    case AMBIENT_OCCLUSION -> legacyconnector.v1_12_2.net.minecraft.client.resources.I18n.format(o.ambientOcclusion().get() ? "options.on" : "options.off");
                    case CHAT_VISIBILITY -> legacyconnector.v1_12_2.net.minecraft.client.resources.I18n.format(o.chatVisibility().get().getKey());
                    default -> "";
                };
            } catch (RuntimeException | LinkageError e) {
                return "";
            }
        }
    }

    /** 1.12's options: on/off ones, sliders (with their range and step) and cycling ones. */
    public enum Options {
        INVERT_MOUSE("options.invertMouse", false, true),
        SENSITIVITY("options.sensitivity", true, false),
        FOV("options.fov", true, false, 30.0F, 110.0F, 1.0F),
        GAMMA("options.gamma", true, false),
        SATURATION("options.saturation", true, false),
        RENDER_DISTANCE("options.renderDistance", true, false, 2.0F, 16.0F, 1.0F),
        VIEW_BOBBING("options.viewBobbing", false, true),
        ANAGLYPH("options.anaglyph", false, true),
        FRAMERATE_LIMIT("options.framerateLimit", true, false, 10.0F, 260.0F, 10.0F),
        FBO_ENABLE("options.fboEnable", false, true),
        RENDER_CLOUDS("options.renderClouds", false, false),
        GRAPHICS("options.graphics", false, false),
        AMBIENT_OCCLUSION("options.ao", false, false),
        GUI_SCALE("options.guiScale", false, false),
        PARTICLES("options.particles", false, false),
        CHAT_VISIBILITY("options.chat.visibility", false, false),
        CHAT_COLOR("options.chat.color", false, true),
        CHAT_LINKS("options.chat.links", false, true),
        CHAT_OPACITY("options.chat.opacity", true, false),
        CHAT_LINKS_PROMPT("options.chat.links.prompt", false, true),
        SNOOPER_ENABLED("options.snooper", false, true),
        USE_FULLSCREEN("options.fullscreen", false, true),
        ENABLE_VSYNC("options.vsync", false, true),
        USE_VBO("options.vbo", false, true),
        TOUCHSCREEN("options.touchscreen", false, true),
        CHAT_SCALE("options.chat.scale", true, false),
        CHAT_WIDTH("options.chat.width", true, false),
        CHAT_HEIGHT_FOCUSED("options.chat.height.focused", true, false),
        CHAT_HEIGHT_UNFOCUSED("options.chat.height.unfocused", true, false),
        MIPMAP_LEVELS("options.mipmapLevels", true, false, 0.0F, 4.0F, 1.0F),
        FORCE_UNICODE_FONT("options.forceUnicodeFont", false, true),
        REDUCED_DEBUG_INFO("options.reducedDebugInfo", false, true),
        ENTITY_SHADOWS("options.entityShadows", false, true),
        MAIN_HAND("options.mainHand", false, false),
        ATTACK_INDICATOR("options.attackIndicator", false, false),
        ENABLE_WEAK_ATTACKS("options.enableWeakAttacks", false, true),
        SHOW_SUBTITLES("options.showSubtitles", false, true),
        REALMS_NOTIFICATIONS("options.realmsNotifications", false, true),
        AUTO_JUMP("options.autoJump", false, true),
        NARRATOR("options.narrator", false, false);

        private final boolean isFloat;
        private final boolean isBoolean;
        private final String translation;
        private final float valueStep;
        private float valueMin;
        private float valueMax;

        Options(String translation, boolean isFloat, boolean isBoolean) {
            this(translation, isFloat, isBoolean, 0.0F, 1.0F, 0.0F);
        }

        Options(String translation, boolean isFloat, boolean isBoolean, float valueMin, float valueMax, float valueStep) {
            this.translation = translation;
            this.isFloat = isFloat;
            this.isBoolean = isBoolean;
            this.valueMin = valueMin;
            this.valueMax = valueMax;
            this.valueStep = valueStep;
        }

        public static Options byOrdinal(int ordinal) {
            for (Options o : values()) if (o.getOrdinal() == ordinal) return o;
            return null;
        }

        public boolean isFloat() {
            return isFloat;
        }

        public boolean isBoolean() {
            return isBoolean;
        }

        public int getOrdinal() {
            return ordinal();
        }

        public int returnEnumOrdinal() {
            return ordinal();
        }

        public String getTranslation() {
            return translation;
        }

        public float getValueMin() {
            return valueMin;
        }

        public float getValueMax() {
            return valueMax;
        }

        public void setValueMax(float value) {
            this.valueMax = value;
        }

        public float normalizeValue(float value) {
            return net.minecraft.util.Mth.clamp((snapToStepClamp(value) - valueMin) / (valueMax - valueMin), 0.0F, 1.0F);
        }

        public float denormalizeValue(float value) {
            return snapToStepClamp(valueMin + (valueMax - valueMin) * net.minecraft.util.Mth.clamp(value, 0.0F, 1.0F));
        }

        public float snapToStepClamp(float value) {
            if (valueStep > 0.0F) value = valueStep * (float) Math.round(value / valueStep);
            return net.minecraft.util.Mth.clamp(value, valueMin, valueMax);
        }
    }
}
