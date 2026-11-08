package legacyconnector.v1_7_10.net.minecraft.stats;

import legacyconnector.v1_7_10.net.minecraft.util.IChatComponent;

public class StatBasic extends StatBase {
    public StatBasic(String id, IChatComponent name, IStatType type) {
        super(id, name, type);
    }

    /** 1.6.4: statistics were keyed by number; the number becomes part of the string id. */
    public StatBasic(int id, String name) {
        super("legacy.stat." + id, new legacyconnector.v1_7_10.net.minecraft.util.ChatComponentText(name));
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
