package legacyconnector.vb1_7_3.net.minecraft.client.option;

/** Beta's {@code EnumOptions}: the options of the video/audio/control screens, in Beta's order (the ordinal is the id a screen's button carries). */
public final class Option {
    private static final java.util.List<Option> ALL = new java.util.ArrayList<>();
    public static final Option MUSIC = new Option("options.music", true, false);
    public static final Option SOUND = new Option("options.sound", true, false);
    public static final Option INVERT_MOUSE = new Option("options.invertMouse", false, true);
    public static final Option SENSITIVITY = new Option("options.sensitivity", true, false);
    public static final Option RENDER_DISTANCE = new Option("options.renderDistance", false, false);
    public static final Option VIEW_BOBBING = new Option("options.viewBobbing", false, true);
    public static final Option ANAGLYPH = new Option("options.anaglyph", false, true);
    public static final Option ADVANCED_OPENGL = new Option("options.advancedOpengl", false, true);
    public static final Option FRAMERATE_LIMIT = new Option("options.framerateLimit", false, false);
    public static final Option DIFFICULTY = new Option("options.difficulty", false, false);
    public static final Option GRAPHICS = new Option("options.graphics", false, false);
    public static final Option AMBIENT_OCCLUSION = new Option("options.ao", false, true);
    public static final Option GUI_SCALE = new Option("options.guiScale", false, false);

    private final String name;
    private final boolean slider, toggle;
    private final int id;

    private Option(String name, boolean slider, boolean toggle) {
        this.name = name;
        this.slider = slider;
        this.toggle = toggle;
        synchronized (ALL) { id = ALL.size(); ALL.add(this); }
    }

    public static Option[] values() { synchronized (ALL) { return ALL.toArray(new Option[0]); } }
    public static Option getById(int id) { synchronized (ALL) { return id >= 0 && id < ALL.size() ? ALL.get(id) : null; } }
    public int getId() { return id; }
    public int ordinal() { return id; }
    public String getName() { return name; }
    public boolean isSlider() { return slider; }
    public boolean isToggle() { return toggle; }
}
