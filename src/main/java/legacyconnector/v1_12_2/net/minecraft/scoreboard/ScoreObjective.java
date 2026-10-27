package legacyconnector.v1_12_2.net.minecraft.scoreboard;

/** An objective: a view over the modern one. */
public class ScoreObjective {
    private final net.minecraft.world.scores.Objective objective;

    public ScoreObjective(net.minecraft.world.scores.Objective objective) {
        this.objective = objective;
    }

    public static ScoreObjective mcc$of(net.minecraft.world.scores.Objective objective) {
        return objective == null ? null : new ScoreObjective(objective);
    }

    public net.minecraft.world.scores.Objective mcc$modern() {
        return objective;
    }

    public Scoreboard getScoreboard() {
        return Scoreboard.mcc$of(objective.getScoreboard());
    }

    public String getName() {
        return objective.getName();
    }

    public IScoreCriteria getCriteria() {
        return IScoreCriteria.mcc$of(objective.getCriteria());
    }

    public String getDisplayName() {
        return objective.getDisplayName().getString();
    }

    public void setDisplayName(String name) {
        objective.setDisplayName(net.minecraft.network.chat.Component.literal(name));
    }

    public IScoreCriteria.EnumRenderType getRenderType() {
        return objective.getRenderType() == net.minecraft.world.scores.criteria.ObjectiveCriteria.RenderType.HEARTS
                ? IScoreCriteria.EnumRenderType.HEARTS : IScoreCriteria.EnumRenderType.INTEGER;
    }

    public void setRenderType(IScoreCriteria.EnumRenderType type) {
        objective.setRenderType(type == IScoreCriteria.EnumRenderType.HEARTS ? net.minecraft.world.scores.criteria.ObjectiveCriteria.RenderType.HEARTS
                : net.minecraft.world.scores.criteria.ObjectiveCriteria.RenderType.INTEGER);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ScoreObjective s && s.objective == objective;
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(objective);
    }
}
