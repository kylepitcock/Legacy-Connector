package legacyconnector.v1_7_10.net.minecraft.scoreboard;

import java.util.Comparator;
import java.util.List;
import net.minecraft.world.scores.ScoreHolder;

/** One holder's score in an objective, read from and written to the modern scoreboard. */
public class Score {
    public static final Comparator scoreComparator = (a, b) -> {
        int pa = ((Score) a).getScorePoints(), pb = ((Score) b).getScorePoints();
        return pa != pb ? Integer.compare(pa, pb) : ((Score) b).getPlayerName().compareToIgnoreCase(((Score) a).getPlayerName());
    };

    private final Scoreboard scoreboard;
    private final ScoreObjective objective;
    private final String name;
    /** Points of a score with no objective (1.7.10 kept those in the scoreboard's map too; they are never shown). */
    private int detachedPoints;

    public Score(Scoreboard scoreboard, ScoreObjective objective, String name) {
        this.scoreboard = scoreboard;
        this.objective = objective;
        this.name = name;
    }

    private net.minecraft.world.scores.ScoreAccess access() {
        return scoreboard.modern.getOrCreatePlayerScore(ScoreHolder.forNameOnly(name), objective.modern);
    }

    public void increseScore(int amount) {
        setScorePoints(getScorePoints() + amount);
    }

    public void decreaseScore(int amount) {
        setScorePoints(getScorePoints() - amount);
    }

    public void func_96648_a() {
        increseScore(1);
    }

    public int getScorePoints() {
        if (objective == null) return detachedPoints;
        var info = scoreboard.modern.getPlayerScoreInfo(ScoreHolder.forNameOnly(name), objective.modern);
        return info == null ? 0 : info.value();
    }

    public void setScorePoints(int points) {
        if (objective == null) detachedPoints = points;
        else access().set(points);
    }

    public ScoreObjective func_96645_d() {
        return objective;
    }

    public String getPlayerName() {
        return name;
    }

    public Scoreboard getScoreScoreboard() {
        return scoreboard;
    }

    public void func_96651_a(List players) {
        setScorePoints(objective.getCriteria().func_96635_a(players));
    }
}
