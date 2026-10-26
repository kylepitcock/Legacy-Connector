package legacyconnector.v1_12_2.net.minecraft.util.text;

import java.util.List;

/** A chat/tooltip text: a component with a style and appended siblings (iterating gives it and all its parts). */
public interface ITextComponent extends Iterable<ITextComponent> {
    ITextComponent setStyle(Style style);

    Style getStyle();

    ITextComponent appendText(String text);

    ITextComponent appendSibling(ITextComponent component);

    String getUnformattedComponentText();

    String getUnformattedText();

    String getFormattedText();

    List<ITextComponent> getSiblings();

    ITextComponent createCopy();

    /** The modern component (literal or translatable text, its style and siblings). */
    default net.minecraft.network.chat.MutableComponent mcc$modern() {
        var out = net.minecraft.network.chat.Component.literal(getUnformattedComponentText());
        out.setStyle(getStyle().mcc$modern());
        for (ITextComponent s : getSiblings()) out.append(s.mcc$modern());
        return out;
    }
}
