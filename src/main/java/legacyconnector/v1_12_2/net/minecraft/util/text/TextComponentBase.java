package legacyconnector.v1_12_2.net.minecraft.util.text;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Siblings and style shared by the text components. */
public abstract class TextComponentBase implements ITextComponent {
    protected List<ITextComponent> siblings = new ArrayList<>();
    private Style style;

    @Override
    public ITextComponent appendSibling(ITextComponent component) {
        component.getStyle().setParentStyle(getStyle());
        siblings.add(component);
        return this;
    }

    @Override
    public List<ITextComponent> getSiblings() {
        return siblings;
    }

    @Override
    public ITextComponent appendText(String text) {
        return appendSibling(new TextComponentString(text));
    }

    @Override
    public ITextComponent setStyle(Style style) {
        this.style = style;
        for (ITextComponent c : siblings) c.getStyle().setParentStyle(getStyle());
        return this;
    }

    @Override
    public Style getStyle() {
        if (style == null) {
            style = new Style();
            for (ITextComponent c : siblings) c.getStyle().setParentStyle(style);
        }
        return style;
    }

    /** This component followed by every part of its siblings, each a copy styled as it shows. */
    @Override
    public Iterator<ITextComponent> iterator() {
        List<ITextComponent> all = new ArrayList<>();
        all.add(this);
        all.addAll(deepCopies(siblings));
        return all.iterator();
    }

    /** Copies of every part of the components, with their inherited style resolved (they print alone). */
    public static Iterator<ITextComponent> createDeepCopyIterator(Iterable<ITextComponent> components) {
        return deepCopies(components).iterator();
    }

    private static List<ITextComponent> deepCopies(Iterable<ITextComponent> components) {
        List<ITextComponent> out = new ArrayList<>();
        for (ITextComponent c : components) {
            for (ITextComponent part : c) {
                ITextComponent copy = part.createCopy();
                copy.setStyle(copy.getStyle().createDeepCopy());
                out.add(copy);
            }
        }
        return out;
    }

    /** Only this component's own text: the parts' texts concatenated (siblings excluded from each part, as in 1.12). */
    @Override
    public final String getUnformattedText() {
        StringBuilder sb = new StringBuilder();
        for (ITextComponent c : this) sb.append(c.getUnformattedComponentText());
        return sb.toString();
    }

    @Override
    public final String getFormattedText() {
        StringBuilder sb = new StringBuilder();
        for (ITextComponent c : this) {
            String s = c.getUnformattedComponentText();
            if (!s.isEmpty()) {
                sb.append(c.getStyle().getFormattingCode());
                sb.append(s);
                sb.append(TextFormatting.RESET);
            }
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TextComponentBase other)) return false;
        return siblings.equals(other.siblings) && getStyle().equals(other.getStyle());
    }

    @Override
    public int hashCode() {
        return 31 * getStyle().hashCode() + siblings.hashCode();
    }

    @Override
    public String toString() {
        return "BaseComponent{style=" + style + ", siblings=" + siblings + "}";
    }
}
