package legacyconnector.v1_7_10.net.minecraft.block;

/** The first log block (oak, spruce, birch, jungle): a {@link BlockLog} like in 1.7.10, so wood checks ({@code isWood}, {@code canSustainLeaves}) and casts of mods work. */
public class BlockOldLog extends BlockLog {
    public BlockOldLog(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    public BlockOldLog() {
        super();
    }
}
