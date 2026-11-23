package legacyconnector.v1_6_4.facade.net.minecraft.block;

/** Facade of the 1.7.10 shim's {@code BlockAnvil} for 1.6.4 mods: the damage-state names. Never instantiated. */
public final class BlockAnvil {
    private BlockAnvil() {}

    private static final String[] STATUSES = {"intact", "slightlyDamaged", "veryDamaged"};

    /** 1.6.4 {@code statuses}: the names of the three damage states of an anvil. */
    public static String[] mcc$get$statuses() {
        return STATUSES;
    }
}
