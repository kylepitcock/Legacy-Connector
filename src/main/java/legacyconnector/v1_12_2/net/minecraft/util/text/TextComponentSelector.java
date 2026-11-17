package legacyconnector.v1_12_2.net.minecraft.util.text;

/** A target selector shown as the names it matches (the bare selector text until the server resolves it). */
public class TextComponentSelector extends TextComponentBase {
    private final String selector;

    public TextComponentSelector(String selector) {
        this.selector = selector;
    }

    public String getSelector() {
        return selector;
    }

    @Override
    public String getUnformattedComponentText() {
        return selector;
    }

    @Override
    public TextComponentSelector createCopy() {
        TextComponentSelector copy = new TextComponentSelector(selector);
        copy.setStyle(getStyle().createShallowCopy());
        for (ITextComponent s : getSiblings()) copy.appendSibling(s.createCopy());
        return copy;
    }

    @Override
    public net.minecraft.network.chat.MutableComponent mcc$modern() {
        var out = net.minecraft.network.chat.Component.selector(selector, java.util.Optional.empty()).setStyle(getStyle().mcc$modern());
        for (ITextComponent s : getSiblings()) out.append(s.mcc$modern());
        return out;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof TextComponentSelector t && selector.equals(t.selector) && super.equals(o);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + selector.hashCode();
    }

    @Override
    public String toString() {
        return "SelectorComponent{pattern='" + selector + "', siblings=" + siblings + ", style=" + getStyle() + "}";
    }
}
