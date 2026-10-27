package legacyconnector.v1_12_2.net.minecraft.scoreboard;

/** One player's (or name's) score on one objective: a view over the modern scoreboard's entry. */
public class Score {
    private final net.minecraft.world.scores.Scoreboard scoreboard;
    private final ScoreObjective objective;
    private final String scorePlayerName;

    public Score(Scoreboard scoreboard, ScoreObjective objective, String playerName) {
        this.scoreboard = scoreboard.mcc$modern();
        this.objective = objective;
        this.scorePlayerName = playerName;
    }

    private net.minecraft.world.scores.ScoreAccess mcc$access() {
        return scoreboard.getOrCreatePlayerScore(net.minecraft.world.scores.ScoreHolder.forNameOnly(scorePlayerName), objective.mcc$modern(), true);
    }

    public void increaseScore(int amount) {
        if (objective.getCriteria().isReadOnly()) throw new IllegalStateException("Cannot modify read-only score");
        mcc$access().add(amount);
    }

    public void decreaseScore(int amount) {
        if (objective.getCriteria().isReadOnly()) throw new IllegalStateException("Cannot modify read-only score");
        mcc$access().add(-amount);
    }

    public void incrementScore() {
        increaseScore(1);
    }

    public int getScorePoints() {
        var info = scoreboard.getPlayerScoreInfo(net.minecraft.world.scores.ScoreHolder.forNameOnly(scorePlayerName), objective.mcc$modern());
        return info == null ? 0 : info.value();
    }

    public void setScorePoints(int points) {
        mcc$access().set(points);
    }

    public ScoreObjective getObjective() {
        return objective;
    }

    public String getPlayerName() {
        return scorePlayerName;
    }

    public Scoreboard getScoreScoreboard() {
        return Scoreboard.mcc$of(scoreboard);
    }

    public boolean isLocked() {
        return mcc$access().locked();
    }

    public void setLocked(boolean locked) {
        if (locked) mcc$access().lock();
        else mcc$access().unlock();
    }
}
