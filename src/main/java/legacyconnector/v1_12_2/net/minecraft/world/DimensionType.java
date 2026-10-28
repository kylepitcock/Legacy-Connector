package legacyconnector.v1_12_2.net.minecraft.world;

/**
 * A dimension kind: id, name, save-folder suffix and provider class (an enum in 1.12.2; a class here so {@link #register} can add
 * constants the way Forge's EnumHelper did).
 */
public final class DimensionType {
    private static final java.util.List<DimensionType> ALL = new java.util.ArrayList<>();

    public static final DimensionType OVERWORLD = new DimensionType("OVERWORLD", 0, "overworld", "", null, true);
    public static final DimensionType NETHER = new DimensionType("NETHER", -1, "the_nether", "_nether", null, false);
    public static final DimensionType THE_END = new DimensionType("THE_END", 1, "the_end", "_end", null, false);

    private final String constant;
    private final int ordinal;
    private final int id;
    private final String name;
    private final String suffix;
    private final Class<?> providerClass;
    private boolean shouldLoadSpawn;

    private DimensionType(String constant, int id, String name, String suffix, Class<?> providerClass, boolean loadSpawn) {
        this.constant = constant;
        this.id = id;
        this.name = name;
        this.suffix = suffix;
        this.providerClass = providerClass;
        this.shouldLoadSpawn = loadSpawn;
        synchronized (ALL) {
            this.ordinal = ALL.size();
            ALL.add(this);
        }
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSuffix() {
        return suffix;
    }

    public boolean shouldLoadSpawn() {
        return shouldLoadSpawn;
    }

    public DimensionType setLoadSpawn(boolean value) {
        this.shouldLoadSpawn = value;
        return this;
    }

    /** The mod's WorldProvider class (null for the vanilla dimensions, which the modern game provides). */
    public Class<?> mcc$providerClass() {
        return providerClass;
    }

    public static DimensionType getById(int id) {
        for (DimensionType t : values()) if (t.id == id) return t;
        throw new IllegalArgumentException("Invalid dimension id " + id);
    }

    public static DimensionType byName(String name) {
        for (DimensionType t : values()) if (t.name.equals(name)) return t;
        throw new IllegalArgumentException("Invalid dimension " + name);
    }

    /** Forge's way to add a dimension type: a new constant named after {@code name}. */
    public static DimensionType register(String name, String suffix, int id, Class<?> provider, boolean keepLoaded) {
        String constant = name.replace(' ', '_').toUpperCase(java.util.Locale.ROOT);
        return new DimensionType(constant, id, name, suffix, provider, keepLoaded);
    }

    public static DimensionType[] values() {
        synchronized (ALL) {
            return ALL.toArray(new DimensionType[0]);
        }
    }

    public static DimensionType valueOf(String constant) {
        for (DimensionType t : values()) if (t.constant.equals(constant)) return t;
        throw new IllegalArgumentException("No enum constant " + constant);
    }

    public String name() {
        return constant;
    }

    public int ordinal() {
        return ordinal;
    }

    /** {@code Enum.compareTo}: legacy calls taking an Enum are linked here. */
    public int compareTo(Object other) {
        return Integer.compare(ordinal, ((DimensionType) other).ordinal);
    }

    @Override
    public String toString() {
        return constant;
    }

    /** Phantom: redirected to {@link #mcc$get$clazz} (the provider class). */
    @Deprecated
    public Class<?> clazz;

    public static Class<?> mcc$get$clazz(DimensionType t) {
        return t.providerClass;
    }
}
