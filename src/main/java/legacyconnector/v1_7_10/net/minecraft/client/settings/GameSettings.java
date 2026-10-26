package legacyconnector.v1_7_10.net.minecraft.client.settings;

/**
 * 1.7.10's options. Values are the vanilla defaults; mods read a few (mipmaps, graphics, language). The shim does not
 * write to the modern options.
 */
public class GameSettings {
    public int mipmapLevels = 4;
    public int anisotropicFiltering = 1;
    public boolean fancyGraphics = true;
    /** 0 off, 1 minimum, 2 maximum (an int in 1.7.10). */
    public int ambientOcclusion = 2;
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

    public static boolean isKeyDown(KeyBinding key) {
        int code = key.getKeyCode();
        if (code == 0) return false;
        long window = net.minecraft.client.Minecraft.getInstance().getWindow().getWindow();
        return code < 0
            ? org.lwjgl.glfw.GLFW.glfwGetMouseButton(window, code + 100) == org.lwjgl.glfw.GLFW.GLFW_PRESS
            : legacyconnector.v1_7_10.org.lwjgl.input.Keyboard.isKeyDown(code);
    }

    public static String getKeyDisplayString(int code) {
        return code < 0
            ? legacyconnector.v1_7_10.net.minecraft.client.resources.I18n.format("key.mouseButton", code + 101)
            : legacyconnector.v1_7_10.org.lwjgl.input.Keyboard.getKeyName(code);
    }

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

    /** Sends the language and view distance to the server; the modern client does this itself. */
    public void sendSettingsToServer() {
    }

    private static net.minecraft.client.Options modern() {
        return net.minecraft.client.Minecraft.getInstance().options;
    }

    /** Phantom: the enabled resource packs (the modern option list, live: "file/<name>" for packs in the folder). */
    @Deprecated
    public java.util.List resourcePacks;

    public static java.util.List mcc$get$resourcePacks(GameSettings s) {
        return modern().resourcePacks;
    }

    public static void mcc$set$resourcePacks(GameSettings s, java.util.List packs) {
        modern().resourcePacks.clear();
        if (packs != null) for (Object o : packs) modern().resourcePacks.add(String.valueOf(o));
    }

    /** The volume of a slider, 0-1. */
    public float getSoundLevel(legacyconnector.v1_7_10.net.minecraft.client.audio.SoundCategory category) {
        return category == null ? 1.0F : modern().getSoundSourceVolume(category.mcc$source());
    }

    public void setSoundLevel(legacyconnector.v1_7_10.net.minecraft.client.audio.SoundCategory category, float volume) {
        if (category != null) modern().getSoundSourceOptionInstance(category.mcc$source()).set((double) volume);
    }

    public void saveOptions() {
        modern().save();
    }

    public void loadOptions() {
        modern().load();
    }

    public boolean shouldRenderClouds() {
        return modern().getCloudsType() != net.minecraft.client.CloudStatus.OFF;
    }

    public void setOptionKeyBinding(KeyBinding key, int code) {
        if (key != null) key.setKeyCode(code);
    }

    /** An on/off option's state. */
    public boolean getOptionOrdinalValue(Options option) {
        var o = modern();
        return switch (option) {
            case INVERT_MOUSE -> o.invertYMouse().get();
            case VIEW_BOBBING -> o.bobView().get();
            case FBO_ENABLE -> true;
            case RENDER_CLOUDS -> o.getCloudsType() != net.minecraft.client.CloudStatus.OFF;
            case CHAT_COLOR -> o.chatColors().get();
            case CHAT_LINKS -> o.chatLinks().get();
            case CHAT_LINKS_PROMPT -> o.chatLinksPrompt().get();
            case USE_FULLSCREEN -> o.fullscreen().get();
            case ENABLE_VSYNC -> o.enableVsync().get();
            case SHOW_CAPE -> o.isModelPartEnabled(net.minecraft.world.entity.player.PlayerModelPart.CAPE);
            case TOUCHSCREEN -> o.touchscreen().get();
            case FORCE_UNICODE_FONT -> o.forceUnicodeFont().get();
            default -> false;
        };
    }

    /** A slider option's value in its own unit (FOV in degrees, render distance in chunks, ...). */
    public float getOptionFloatValue(Options option) {
        var o = modern();
        return switch (option) {
            case FOV -> o.fov().get();
            case GAMMA -> o.gamma().get().floatValue();
            case SENSITIVITY -> o.sensitivity().get().floatValue();
            case RENDER_DISTANCE -> o.renderDistance().get();
            case FRAMERATE_LIMIT -> o.framerateLimit().get();
            case CHAT_OPACITY -> o.chatOpacity().get().floatValue();
            case CHAT_SCALE -> o.chatScale().get().floatValue();
            case CHAT_WIDTH -> o.chatWidth().get().floatValue();
            case CHAT_HEIGHT_FOCUSED -> o.chatHeightFocused().get().floatValue();
            case CHAT_HEIGHT_UNFOCUSED -> o.chatHeightUnfocused().get().floatValue();
            case MIPMAP_LEVELS -> o.mipmapLevels().get();
            case ANISOTROPIC_FILTERING -> 1.0F;
            default -> 0.0F;
        };
    }

    public void setOptionFloatValue(Options option, float value) {
        var o = modern();
        switch (option) {
            case FOV -> o.fov().set(Math.round(value));
            case GAMMA -> o.gamma().set((double) value);
            case SENSITIVITY -> o.sensitivity().set((double) value);
            case RENDER_DISTANCE -> o.renderDistance().set(Math.round(value));
            case FRAMERATE_LIMIT -> o.framerateLimit().set(Math.round(value));
            case CHAT_OPACITY -> o.chatOpacity().set((double) value);
            case CHAT_SCALE -> o.chatScale().set((double) value);
            case CHAT_WIDTH -> o.chatWidth().set((double) value);
            case CHAT_HEIGHT_FOCUSED -> o.chatHeightFocused().set((double) value);
            case CHAT_HEIGHT_UNFOCUSED -> o.chatHeightUnfocused().set((double) value);
            default -> {
            }
        }
    }

    /** Steps a button option (on/off ones toggle). */
    public void setOptionValue(Options option, int step) {
        var o = modern();
        switch (option) {
            case INVERT_MOUSE -> o.invertYMouse().set(!o.invertYMouse().get());
            case VIEW_BOBBING -> o.bobView().set(!o.bobView().get());
            case CHAT_COLOR -> o.chatColors().set(!o.chatColors().get());
            case CHAT_LINKS -> o.chatLinks().set(!o.chatLinks().get());
            case CHAT_LINKS_PROMPT -> o.chatLinksPrompt().set(!o.chatLinksPrompt().get());
            case ENABLE_VSYNC -> o.enableVsync().set(!o.enableVsync().get());
            case SHOW_CAPE -> o.toggleModelPart(net.minecraft.world.entity.player.PlayerModelPart.CAPE,
                    !o.isModelPartEnabled(net.minecraft.world.entity.player.PlayerModelPart.CAPE));
            case TOUCHSCREEN -> o.touchscreen().set(!o.touchscreen().get());
            case FORCE_UNICODE_FONT -> o.forceUnicodeFont().set(!o.forceUnicodeFont().get());
            default -> {
            }
        }
    }

    /** The button text of an option ("FOV: 70"). */
    public String getKeyBinding(Options option) {
        String name = legacyconnector.v1_7_10.net.minecraft.client.resources.I18n.format(option.getEnumString()) + ": ";
        if (option.getEnumFloat()) return name + (int) getOptionFloatValue(option);
        if (option.getEnumBoolean()) return name + legacyconnector.v1_7_10.net.minecraft.client.resources.I18n.format(getOptionOrdinalValue(option) ? "options.on" : "options.off");
        return name;
    }

    /** 1.7.10's option list: sliders carry their range and step. */
    public enum Options {
        INVERT_MOUSE("options.invertMouse", false, true),
        SENSITIVITY("options.sensitivity", true, false),
        FOV("options.fov", true, false, 30.0F, 110.0F, 1.0F),
        GAMMA("options.gamma", true, false),
        SATURATION("options.saturation", true, false),
        RENDER_DISTANCE("options.renderDistance", true, false, 2.0F, 16.0F, 1.0F),
        VIEW_BOBBING("options.viewBobbing", false, true),
        ANAGLYPH("options.anaglyph", false, true),
        ADVANCED_OPENGL("options.advancedOpengl", false, true),
        FRAMERATE_LIMIT("options.framerateLimit", true, false, 10.0F, 260.0F, 10.0F),
        FBO_ENABLE("options.fboEnable", false, true),
        DIFFICULTY("options.difficulty", false, false),
        GRAPHICS("options.graphics", false, false),
        AMBIENT_OCCLUSION("options.ao", false, false),
        GUI_SCALE("options.guiScale", false, false),
        RENDER_CLOUDS("options.renderClouds", false, true),
        PARTICLES("options.particles", false, false),
        CHAT_VISIBILITY("options.chat.visibility", false, false),
        CHAT_COLOR("options.chat.color", false, true),
        CHAT_LINKS("options.chat.links", false, true),
        CHAT_OPACITY("options.chat.opacity", true, false),
        CHAT_LINKS_PROMPT("options.chat.links.prompt", false, true),
        SNOOPER_ENABLED("options.snooper", false, true),
        USE_FULLSCREEN("options.fullscreen", false, true),
        ENABLE_VSYNC("options.vsync", false, true),
        SHOW_CAPE("options.showCape", false, true),
        TOUCHSCREEN("options.touchscreen", false, true),
        CHAT_SCALE("options.chat.scale", true, false),
        CHAT_WIDTH("options.chat.width", true, false),
        CHAT_HEIGHT_FOCUSED("options.chat.height.focused", true, false),
        CHAT_HEIGHT_UNFOCUSED("options.chat.height.unfocused", true, false),
        MIPMAP_LEVELS("options.mipmapLevels", true, false, 0.0F, 4.0F, 1.0F),
        ANISOTROPIC_FILTERING("options.anisotropicFiltering", true, false, 1.0F, 16.0F, 0.0F),
        FORCE_UNICODE_FONT("options.forceUnicodeFont", false, true),
        STREAM_BYTES_PER_PIXEL("options.stream.bytesPerPixel", true, false),
        STREAM_VOLUME_MIC("options.stream.micVolumne", true, false),
        STREAM_VOLUME_SYSTEM("options.stream.systemVolume", true, false),
        STREAM_KBPS("options.stream.kbps", true, false),
        STREAM_FPS("options.stream.fps", true, false),
        STREAM_COMPRESSION("options.stream.compression", false, false),
        STREAM_SEND_METADATA("options.stream.sendMetadata", false, true),
        STREAM_CHAT_ENABLED("options.stream.chat.enabled", false, false),
        STREAM_CHAT_USER_FILTER("options.stream.chat.userFilter", false, false),
        STREAM_MIC_TOGGLE_BEHAVIOR("options.stream.micToggleBehavior", false, false);

        private final boolean enumFloat;
        private final boolean enumBoolean;
        private final String enumString;
        private final float valueStep;
        private float valueMin;
        private float valueMax;

        Options(String name, boolean isFloat, boolean isBoolean) {
            this(name, isFloat, isBoolean, 0.0F, 1.0F, 0.0F);
        }

        Options(String name, boolean isFloat, boolean isBoolean, float min, float max, float step) {
            this.enumString = name;
            this.enumFloat = isFloat;
            this.enumBoolean = isBoolean;
            this.valueMin = min;
            this.valueMax = max;
            this.valueStep = step;
        }

        public static Options getEnumOptions(int ordinal) {
            for (Options o : values()) if (o.returnEnumOrdinal() == ordinal) return o;
            return null;
        }

        public boolean getEnumFloat() {
            return enumFloat;
        }

        public boolean getEnumBoolean() {
            return enumBoolean;
        }

        public int returnEnumOrdinal() {
            return ordinal();
        }

        public String getEnumString() {
            return enumString;
        }

        public float getValueMax() {
            return valueMax;
        }

        public void setValueMax(float max) {
            this.valueMax = max;
        }

        public float normalizeValue(float value) {
            return Math.max(0.0F, Math.min(1.0F, (snapToStepClamp(value) - valueMin) / (valueMax - valueMin)));
        }

        public float denormalizeValue(float value) {
            return snapToStepClamp(valueMin + (valueMax - valueMin) * Math.max(0.0F, Math.min(1.0F, value)));
        }

        public float snapToStepClamp(float value) {
            return Math.max(valueMin, Math.min(valueMax, snapToStep(value)));
        }

        public float snapToStep(float value) {
            if (valueStep > 0.0F) value = valueStep * Math.round(value / valueStep);
            return value;
        }
    }
}
