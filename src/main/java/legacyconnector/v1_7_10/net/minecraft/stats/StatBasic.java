package legacyconnector.v1_7_10.net.minecraft.stats;

import legacyconnector.v1_7_10.net.minecraft.util.IChatComponent;

public class StatBasic extends StatBase {
    public StatBasic(String id, IChatComponent name, IStatType type) {
        super(id, name, type);
    }

    public StatBasic(String id, IChatComponent name) {
        super(id, name);
    }

    @Override
    public StatBase registerStat() {
        super.registerStat();
        StatList.generalStats.add(this);
        return this;
    }
}
