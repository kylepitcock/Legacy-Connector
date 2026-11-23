package legacyconnector.v1_6_4.facade.net.minecraft.block;

/** Facade of the 1.7.10 shim's {@code BlockEndPortal} for 1.6.4 mods: the static flag the dragon's death set. Never instantiated. */
public final class BlockEndPortal {
    private BlockEndPortal() {}

    private static volatile boolean bossDefeated;

    /** 1.6.4 {@code bossDefeated}: whether the dragon was killed (mods read it to gate content). */
    public static boolean mcc$get$bossDefeated() {
        return bossDefeated;
    }

    public static void mcc$set$bossDefeated(boolean value) {
        bossDefeated = value;
    }
}
