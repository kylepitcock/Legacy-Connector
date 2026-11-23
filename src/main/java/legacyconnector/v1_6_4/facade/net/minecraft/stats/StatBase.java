package legacyconnector.v1_6_4.facade.net.minecraft.stats;

/** Facade of the 1.7.10 shim's {@code StatBase} for 1.6.4 mods: the number formatters of the statistics screens. Never instantiated. */
public final class StatBase {
    private StatBase() {}

    /** 1.6.4 static {@code getNumberFormat()}: the integer formatter the statistics screens print counts with. */
    public static java.text.NumberFormat getNumberFormat() {
        return legacyconnector.v1_7_10.net.minecraft.stats.StatBase.numberFormat;
    }

    /** 1.6.4 static {@code getDecimalFormat()}: the two-decimals formatter (distances, times). */
    public static java.text.DecimalFormat getDecimalFormat() {
        return legacyconnector.v1_7_10.net.minecraft.stats.StatBase.decimalFormat;
    }
}
