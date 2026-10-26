package legacyconnector.v1_12_2.net.minecraft.util.text;

/** Literal text. */
public class TextComponentString extends TextComponentBase {
    private final String text;

    public TextComponentString(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    @Override
    public String getUnformattedComponentText() {
        return text;
    }

    @Override
    public TextComponentString createCopy() {
        TextComponentString copy = new TextComponentString(text);
        copy.setStyle(getStyle().createShallowCopy());
        for (ITextComponent s : getSiblings()) copy.appendSibling(s.createCopy());
        return copy;
    }

    @Override
    public net.minecraft.network.chat.MutableComponent mcc$modern() {
        var out = net.minecraft.network.chat.Component.literal(text).setStyle(getStyle().mcc$modern());
        for (ITextComponent s : getSiblings()) out.append(s.mcc$modern());
        return out;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof TextComponentString s && text.equals(s.getText()) && super.equals(o);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + text.hashCode();
    }

    @Override
    public String toString() {
        return "TextComponent{text='" + text + "', siblings=" + siblings + ", style=" + getStyle() + "}";
    }
}
