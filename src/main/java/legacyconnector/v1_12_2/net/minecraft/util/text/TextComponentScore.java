package legacyconnector.v1_12_2.net.minecraft.util.text;

/** A scoreboard value shown as text: the score of {@code name} in {@code objective}, or the value set by the resolver. */
public class TextComponentScore extends TextComponentBase {
    private final String name;
    private final String objective;
    private String value = "";

    public TextComponentScore(String name, String objective) {
        this.name = name;
        this.objective = objective;
    }

    public String getName() {
        return name;
    }

    public String getObjective() {
        return objective;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String getUnformattedComponentText() {
        return value;
    }

    @Override
    public TextComponentScore createCopy() {
        TextComponentScore copy = new TextComponentScore(name, objective);
        copy.setValue(value);
        copy.setStyle(getStyle().createShallowCopy());
        for (ITextComponent s : getSiblings()) copy.appendSibling(s.createCopy());
        return copy;
    }

    @Override
    public net.minecraft.network.chat.MutableComponent mcc$modern() {
        var out = net.minecraft.network.chat.Component.score(name, objective).setStyle(getStyle().mcc$modern());
        for (ITextComponent s : getSiblings()) out.append(s.mcc$modern());
        return out;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof TextComponentScore t && name.equals(t.name) && objective.equals(t.objective) && value.equals(t.value) && super.equals(o);
    }

    @Override
    public int hashCode() {
        return 31 * (31 * super.hashCode() + name.hashCode()) + objective.hashCode();
    }

    @Override
    public String toString() {
        return "ScoreComponent{name='" + name + "', objective='" + objective + "', value='" + value + "', siblings=" + siblings + ", style=" + getStyle() + "}";
    }
}
