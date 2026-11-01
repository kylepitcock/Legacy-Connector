package legacyconnector.v1_7_10.net.minecraft.block;

/** The second log block (acacia, dark oak): a {@link BlockLog} like in 1.7.10, so wood checks ({@code isWood}, {@code canSustainLeaves}) and casts of mods work. */
public class BlockNewLog extends BlockLog {
    public BlockNewLog(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    public BlockNewLog() {
        super();
    }
}
