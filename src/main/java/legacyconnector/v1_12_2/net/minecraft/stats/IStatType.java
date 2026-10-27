package legacyconnector.v1_12_2.net.minecraft.stats;

/** How a statistic's value is shown. */
public interface IStatType {
    String format(int value);
}
