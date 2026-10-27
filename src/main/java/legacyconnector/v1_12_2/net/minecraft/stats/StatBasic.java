package legacyconnector.v1_12_2.net.minecraft.stats;

import legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent;

/** A general statistic (not about an item, block or entity). */
public class StatBasic extends StatBase {
    public StatBasic(String statIdIn, ITextComponent statNameIn, IStatType typeIn) {
        super(statIdIn, statNameIn, typeIn);
    }

    public StatBasic(String statIdIn, ITextComponent statNameIn) {
        super(statIdIn, statNameIn);
    }

    @Override
    public StatBase registerStat() {
        super.registerStat();
        StatList.BASIC_STATS.add(this);
        return this;
    }
}
