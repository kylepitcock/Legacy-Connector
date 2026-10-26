package legacyconnector.v1_7_10.net.minecraft.scoreboard;

public class ScoreHealthCriteria extends ScoreDummyCriteria {
    public ScoreHealthCriteria(String name) {
        super(name);
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }
}
