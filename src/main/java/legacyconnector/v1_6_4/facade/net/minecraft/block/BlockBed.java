package legacyconnector.v1_6_4.facade.net.minecraft.block;

/** Facade of the 1.7.10 shim's {@code BlockBed} for 1.6.4 mods: the foot-to-head offset table. Never instantiated. */
public final class BlockBed {
    private BlockBed() {}

    private static final int[][] FOOT_TO_HEAD = {{0, 1}, {-1, 0}, {0, -1}, {1, 0}};

    /** 1.6.4 {@code footBlockToHeadBlockMap}: per bed direction, the x and z offset from the foot to the head block. */
    public static int[][] mcc$get$footBlockToHeadBlockMap() {
        return FOOT_TO_HEAD;
    }
}
