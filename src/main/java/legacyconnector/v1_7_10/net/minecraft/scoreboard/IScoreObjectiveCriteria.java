package legacyconnector.v1_7_10.net.minecraft.scoreboard;

import java.util.List;
import java.util.Map;

/** An objective's criteria; the constants stand for the modern criteria of the same names. */
public interface IScoreObjectiveCriteria {
    Map INSTANCES = new java.util.concurrent.ConcurrentHashMap();
    IScoreObjectiveCriteria DUMMY = new ScoreDummyCriteria("dummy");
    IScoreObjectiveCriteria deathCount = new ScoreDummyCriteria("deathCount");
    IScoreObjectiveCriteria playerKillCount = new ScoreDummyCriteria("playerKillCount");
    IScoreObjectiveCriteria totalKillCount = new ScoreDummyCriteria("totalKillCount");
    IScoreObjectiveCriteria health = new ScoreHealthCriteria("health");

    String getName();

    int func_96635_a(List players);

    boolean isReadOnly();
}
