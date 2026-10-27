package legacyconnector.v1_12_2.net.minecraft.scoreboard;

import java.util.Map;

/** What an objective counts (by name: dummy, deathCount, health, ...); the modern criterion of the same name does the counting. */
public interface IScoreCriteria {
    Map<String, IScoreCriteria> INSTANCES = new java.util.concurrent.ConcurrentHashMap<>();
    IScoreCriteria DUMMY = new ScoreCriteria("dummy");
    IScoreCriteria TRIGGER = new ScoreCriteria("trigger");
    IScoreCriteria DEATH_COUNT = new ScoreCriteria("deathCount");
    IScoreCriteria PLAYER_KILL_COUNT = new ScoreCriteria("playerKillCount");
    IScoreCriteria TOTAL_KILL_COUNT = new ScoreCriteria("totalKillCount");
    IScoreCriteria HEALTH = new ScoreCriteriaReadOnly("health");
    IScoreCriteria FOOD = new ScoreCriteriaReadOnly("food");
    IScoreCriteria AIR = new ScoreCriteriaReadOnly("air");
    IScoreCriteria ARMOR = new ScoreCriteriaReadOnly("armor");
    IScoreCriteria XP = new ScoreCriteriaReadOnly("xp");
    IScoreCriteria LEVEL = new ScoreCriteriaReadOnly("level");

    String getName();

    boolean isReadOnly();

    EnumRenderType getRenderType();

    /** The modern criterion of this name (a custom dummy-like one for names the modern game does not know). */
    default net.minecraft.world.scores.criteria.ObjectiveCriteria mcc$modern() {
        return net.minecraft.world.scores.criteria.ObjectiveCriteria.byName(getName()).orElse(net.minecraft.world.scores.criteria.ObjectiveCriteria.DUMMY);
    }

    static IScoreCriteria mcc$of(net.minecraft.world.scores.criteria.ObjectiveCriteria criteria) {
        if (criteria == null) return DUMMY;
        IScoreCriteria known = INSTANCES.get(criteria.getName());
        if (known != null) return known;
        return criteria.isReadOnly() ? new ScoreCriteriaReadOnly(criteria.getName()) : new ScoreCriteria(criteria.getName());
    }

    enum EnumRenderType {
        INTEGER("integer"),
        HEARTS("hearts");

        private final String renderType;

        EnumRenderType(String renderTypeIn) {
            this.renderType = renderTypeIn;
        }

        public String getRenderType() {
            return renderType;
        }

        public static EnumRenderType getByName(String name) {
            for (EnumRenderType t : values()) if (t.renderType.equals(name)) return t;
            return INTEGER;
        }
    }
}
