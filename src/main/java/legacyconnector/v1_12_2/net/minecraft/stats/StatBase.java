package legacyconnector.v1_12_2.net.minecraft.stats;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.function.Supplier;
import legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent;

/**
 * A statistic. The vanilla ones stand for the modern statistic they became ({@link #mcc$modern}), which is what awarding one
 * counts; statistics mods make have no modern counterpart and only count in the mod's own code.
 */
public class StatBase {
    public final String statId;
    private final ITextComponent statName;
    public boolean isIndependent;
    private final IStatType formatter;
    private Supplier<net.minecraft.stats.Stat<?>> mcc$modern;

    private static final NumberFormat NUMBER_FORMAT = NumberFormat.getIntegerInstance(Locale.US);
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("########0.00");

    public static final IStatType simpleStatType = NUMBER_FORMAT::format;
    public static final IStatType timeStatType = number -> {
        double d0 = number / 20.0D;
        double d1 = d0 / 60.0D;
        double d2 = d1 / 60.0D;
        double d3 = d2 / 24.0D;
        double d4 = d3 / 365.0D;
        if (d4 > 0.5D) return DECIMAL_FORMAT.format(d4) + " y";
        if (d3 > 0.5D) return DECIMAL_FORMAT.format(d3) + " d";
        if (d2 > 0.5D) return DECIMAL_FORMAT.format(d2) + " h";
        return d1 > 0.5D ? DECIMAL_FORMAT.format(d1) + " m" : d0 + " s";
    };
    public static final IStatType distanceStatType = number -> {
        double d0 = number / 100.0D;
        double d1 = d0 / 1000.0D;
        if (d1 > 0.5D) return DECIMAL_FORMAT.format(d1) + " km";
        return d0 > 0.5D ? DECIMAL_FORMAT.format(d0) + " m" : number + " cm";
    };
    public static final IStatType divideByTen = number -> DECIMAL_FORMAT.format(number * 0.1D);

    public StatBase(String statIdIn, ITextComponent statNameIn, IStatType formatterIn) {
        this.statId = statIdIn;
        this.statName = statNameIn;
        this.formatter = formatterIn;
    }

    public StatBase(String statIdIn, ITextComponent statNameIn) {
        this(statIdIn, statNameIn, simpleStatType);
    }

    /** Binds this statistic to the modern one it counts as. */
    public StatBase mcc$bind(Supplier<net.minecraft.stats.Stat<?>> modern) {
        this.mcc$modern = modern;
        return this;
    }

    /** The modern statistic this one counts as, or null for a mod's own statistic. */
    public net.minecraft.stats.Stat<?> mcc$modern() {
        try {
            return mcc$modern == null ? null : mcc$modern.get();
        } catch (RuntimeException e) {
            return null;
        }
    }

    public StatBase initIndependentStat() {
        this.isIndependent = true;
        return this;
    }

    public StatBase registerStat() {
        StatList.mcc$register(this);
        return this;
    }

    public String format(int number) {
        return formatter.format(number);
    }

    public ITextComponent getStatName() {
        ITextComponent t = statName.createCopy();
        t.getStyle().setColor(legacyconnector.v1_12_2.net.minecraft.util.text.TextFormatting.GRAY);
        return t;
    }

    public ITextComponent createChatComponent() {
        return getStatName();
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof StatBase s && statId.equals(s.statId);
    }

    @Override
    public int hashCode() {
        return statId.hashCode();
    }

    @Override
    public String toString() {
        return "Stat{id=" + statId + ", nameId=" + statName + ", awardLocallyOnly=" + isIndependent + ", formatter=" + formatter + "}";
    }

    public Class<?> getSerializableClazz() {
        return null;
    }
}
