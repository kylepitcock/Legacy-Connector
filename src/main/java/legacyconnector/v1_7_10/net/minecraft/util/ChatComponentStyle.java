package legacyconnector.v1_7_10.net.minecraft.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.network.chat.MutableComponent;

public abstract class ChatComponentStyle implements IChatComponent {
    protected List<IChatComponent> siblings = new ArrayList<>();
    private ChatStyle style;

    @Override
    public IChatComponent appendSibling(IChatComponent component) {
        siblings.add(component);
        return this;
    }

    @Override
    public List<IChatComponent> getSiblings() {
        return siblings;
    }

    @Override
    public IChatComponent appendText(String text) {
        return appendSibling(new ChatComponentText(text));
    }

    @Override
    public IChatComponent setChatStyle(ChatStyle style) {
        this.style = style;
        return this;
    }

    @Override
    public ChatStyle getChatStyle() {
        if (style == null) style = new ChatStyle();
        return style;
    }

    @Override
    public Iterator<IChatComponent> iterator() {
        List<IChatComponent> all = new ArrayList<>();
        all.add(this);
        for (IChatComponent s : siblings) s.forEach(all::add);
        return all.iterator();
    }

    @Override
    public final String getUnformattedText() {
        StringBuilder sb = new StringBuilder();
        for (IChatComponent c : this) sb.append(c.getUnformattedTextForChat());
        return sb.toString();
    }

    @Override
    public final String getFormattedText() {
        return getUnformattedText();
    }

    /** Subclasses produce the modern component for themselves; siblings and style are applied here. */
    protected abstract MutableComponent mcc$self();

    @Override
    public MutableComponent mcc$toModern() {
        MutableComponent out = mcc$self();
        if (style != null) out = out.withStyle(style.mcc$modern());
        for (IChatComponent s : siblings) out.append(s.mcc$toModern());
        return out;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" + getUnformattedText() + "}";
    }
}
