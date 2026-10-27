package legacyconnector.v1_12_2.net.minecraft.scoreboard;

/** A criterion the game computes (health, food, xp...): its scores cannot be set by commands. */
public class ScoreCriteriaReadOnly extends ScoreCriteria {
    public ScoreCriteriaReadOnly(String name) {
        super(name);
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }
}
