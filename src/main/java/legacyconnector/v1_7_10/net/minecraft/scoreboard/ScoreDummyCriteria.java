package legacyconnector.v1_7_10.net.minecraft.scoreboard;

import java.util.List;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

public class ScoreDummyCriteria implements IScoreObjectiveCriteria {
    private final String name;

    @SuppressWarnings("unchecked")
    public ScoreDummyCriteria(String name) {
        this.name = name;
        INSTANCES.put(name, this);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int func_96635_a(List players) {
        return 0;
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    /** The modern criteria (custom ones a mod made count as dummies). */
    public ObjectiveCriteria mcc$modern() {
        return switch (name) {
            case "deathCount" -> ObjectiveCriteria.DEATH_COUNT;
            case "playerKillCount" -> ObjectiveCriteria.KILL_COUNT_PLAYERS;
            case "totalKillCount" -> ObjectiveCriteria.KILL_COUNT_ALL;
            case "health" -> ObjectiveCriteria.HEALTH;
            default -> ObjectiveCriteria.byName(name).orElse(ObjectiveCriteria.DUMMY);
        };
    }

    /** The legacy criteria of a modern one. */
    public static IScoreObjectiveCriteria mcc$wrap(ObjectiveCriteria modern) {
        if (modern == ObjectiveCriteria.DEATH_COUNT) return deathCount;
        if (modern == ObjectiveCriteria.KILL_COUNT_PLAYERS) return playerKillCount;
        if (modern == ObjectiveCriteria.KILL_COUNT_ALL) return totalKillCount;
        if (modern == ObjectiveCriteria.HEALTH) return health;
        Object known = INSTANCES.get(modern.getName());
        return known instanceof IScoreObjectiveCriteria c ? c : DUMMY;
    }
}
