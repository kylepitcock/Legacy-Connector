package mccompat.v1_7_10.net.minecraft.stats;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import mccompat.v1_7_10.net.minecraft.util.ChatComponentTranslation;
import mccompat.v1_7_10.net.minecraft.util.IChatComponent;
import mccompat.v1_7_10.net.minecraft.util.StatCollector;

/** 1.7.10 statistic bookkeeping; the modern stat system is not driven by these (counts are not tracked yet). */
public class StatBase {
    public final String statId;
    private final IChatComponent statName;
    public boolean isIndependent;
    private final IStatType type;
    private Class<?> field_150956_d;

    public static final NumberFormat numberFormat = NumberFormat.getIntegerInstance(java.util.Locale.US);
    public static final DecimalFormat decimalFormat = new DecimalFormat("########0.00");
    public static final IStatType simpleStatType = value -> numberFormat.format(value);
    public static final IStatType timeStatType = value -> {
        double seconds = value / 20.0D;
        return seconds < 60 ? decimalFormat.format(seconds) + " s" : decimalFormat.format(seconds / 60.0D) + " m";
    };
    public static final IStatType distanceStatType = value -> {
        double meters = value / 100.0D;
        return meters < 1000 ? decimalFormat.format(meters) + " m" : decimalFormat.format(meters / 1000.0D) + " km";
    };

    public StatBase(String id, IChatComponent name, IStatType type) {
        this.statId = id;
        this.statName = name;
        this.type = type;
    }

    public StatBase(String id, IChatComponent name) {
        this(id, name, simpleStatType);
    }

    public StatBase initIndependentStat() {
        this.isIndependent = true;
        return this;
    }

    public StatBase registerStat() {
        StatList.allStats.add(this);
        StatList.oneShotStats.put(statId, this);
        return this;
    }

    public boolean isAchievement() {
        return false;
    }

    public String func_75968_a(int value) {
        return type.format(value);
    }

    public IChatComponent getStatName() {
        return statName;
    }

    public IChatComponent func_150955_j() {
        return statName.createCopy();
    }

    public StatBase func_150953_b(Class<?> cls) {
        this.field_150956_d = cls;
        return this;
    }

    public Class<?> func_150954_l() {
        return field_150956_d;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof StatBase s && statId.equals(s.statId);
    }

    @Override
    public int hashCode() {
        return statId.hashCode();
    }

    @Override
    public String toString() {
        return "Stat{id=" + statId + "}";
    }

    static IChatComponent translated(String key) {
        return new ChatComponentTranslation(key);
    }
}
