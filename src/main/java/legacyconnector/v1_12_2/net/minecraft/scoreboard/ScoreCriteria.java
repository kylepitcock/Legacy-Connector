package legacyconnector.v1_12_2.net.minecraft.scoreboard;

/** A score criterion players' scores can be changed for (registered by name in {@link IScoreCriteria#INSTANCES}). */
public class ScoreCriteria implements IScoreCriteria {
    private final String name;

    public ScoreCriteria(String name) {
        this.name = name;
        IScoreCriteria.INSTANCES.put(name, this);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
    public EnumRenderType getRenderType() {
        return "health".equals(name) ? EnumRenderType.HEARTS : EnumRenderType.INTEGER;
    }
}
