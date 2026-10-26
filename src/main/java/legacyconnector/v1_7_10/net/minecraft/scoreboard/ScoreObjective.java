package legacyconnector.v1_7_10.net.minecraft.scoreboard;

import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.Objective;

/** A view of a modern objective. */
public class ScoreObjective {
    private final Scoreboard scoreboard;
    final Objective modern;

    ScoreObjective(Scoreboard scoreboard, Objective modern) {
        this.scoreboard = scoreboard;
        this.modern = modern;
    }

    public Scoreboard getScoreboard() {
        return scoreboard;
    }

    public String getName() {
        return modern.getName();
    }

    public IScoreObjectiveCriteria getCriteria() {
        return ScoreDummyCriteria.mcc$wrap(modern.getCriteria());
    }

    public String getDisplayName() {
        return modern.getDisplayName().getString();
    }

    public void setDisplayName(String name) {
        modern.setDisplayName(Component.literal(name));
    }

    public Objective mcc$modern() {
        return modern;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ScoreObjective other && other.modern == modern;
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(modern);
    }
}
