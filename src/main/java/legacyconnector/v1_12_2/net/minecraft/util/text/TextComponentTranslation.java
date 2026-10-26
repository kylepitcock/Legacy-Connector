package legacyconnector.v1_12_2.net.minecraft.util.text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A translated text: the translation of {@code key} with {@code %s} / {@code %1$s} replaced by the arguments (texts or other components),
 * translated with the server's language table when first read.
 */
public class TextComponentTranslation extends TextComponentBase {
    public static final Pattern STRING_VARIABLE_PATTERN = Pattern.compile("%(?:(\\d+)\\$)?([A-Za-z%]|$)");
    private final String key;
    private final Object[] formatArgs;
    private final Object syncLock = new Object();
    private long lastTranslationUpdateTimeInMilliseconds = -1L;
    List<ITextComponent> children = new ArrayList<>();

    public TextComponentTranslation(String key, Object... args) {
        this.key = key;
        this.formatArgs = args;
        for (Object o : args) if (o instanceof ITextComponent c) c.getStyle().setParentStyle(getStyle());
    }

    synchronized void ensureInitialized() {
        synchronized (syncLock) {
            if (lastTranslationUpdateTimeInMilliseconds != -1L) return;
            lastTranslationUpdateTimeInMilliseconds = System.currentTimeMillis();
            children.clear();
            try {
                initializeFromFormat(legacyconnector.v1_12_2.net.minecraft.util.text.translation.I18n.translateToLocal(key));
            } catch (IllegalArgumentException e) {
                children.clear();
                try {
                    initializeFromFormat(legacyconnector.v1_12_2.net.minecraft.util.text.translation.I18n.translateToFallback(key));
                } catch (IllegalArgumentException e2) {
                    children.clear();
                    children.add(new TextComponentString(key));
                }
            }
        }
    }

    protected void initializeFromFormat(String format) {
        Matcher m = STRING_VARIABLE_PATTERN.matcher(format);
        int next = 0;
        int last = 0;
        while (m.find(last)) {
            int start = m.start();
            int end = m.end();
            if (start > last) addLiteral(format.substring(last, start));
            String type = m.group(2);
            String whole = format.substring(start, end);
            if ("%".equals(type) && "%%".equals(whole)) {
                addLiteral("%");
            } else {
                if (!"s".equals(type)) throw new IllegalArgumentException("Unsupported format: '" + whole + "'");
                String index = m.group(1);
                int i = index != null ? Integer.parseInt(index) - 1 : next++;
                if (i < formatArgs.length) children.add(getFormatArgumentAsComponent(i));
            }
            last = end;
        }
        if (last < format.length()) addLiteral(format.substring(last));
    }

    private void addLiteral(String s) {
        TextComponentString c = new TextComponentString(s);
        c.getStyle().setParentStyle(getStyle());
        children.add(c);
    }

    private ITextComponent getFormatArgumentAsComponent(int index) {
        if (index >= formatArgs.length) throw new IllegalArgumentException("Invalid index " + index);
        Object o = formatArgs[index];
        ITextComponent c;
        if (o instanceof ITextComponent t) {
            c = t;
        } else {
            c = new TextComponentString(o == null ? "null" : o.toString());
            c.getStyle().setParentStyle(getStyle());
        }
        return c;
    }

    @Override
    public ITextComponent setStyle(Style style) {
        super.setStyle(style);
        for (Object o : formatArgs) if (o instanceof ITextComponent c) c.getStyle().setParentStyle(getStyle());
        if (lastTranslationUpdateTimeInMilliseconds > -1L) for (ITextComponent c : children) c.getStyle().setParentStyle(style);
        return this;
    }

    @Override
    public Iterator<ITextComponent> iterator() {
        ensureInitialized();
        List<ITextComponent> all = new ArrayList<>();
        createDeepCopyIterator(children).forEachRemaining(all::add);
        createDeepCopyIterator(siblings).forEachRemaining(all::add);
        return all.iterator();
    }

    @Override
    public String getUnformattedComponentText() {
        ensureInitialized();
        StringBuilder sb = new StringBuilder();
        for (ITextComponent c : children) sb.append(c.getUnformattedComponentText());
        return sb.toString();
    }

    @Override
    public TextComponentTranslation createCopy() {
        Object[] args = new Object[formatArgs.length];
        for (int i = 0; i < formatArgs.length; i++) args[i] = formatArgs[i] instanceof ITextComponent c ? c.createCopy() : formatArgs[i];
        TextComponentTranslation copy = new TextComponentTranslation(key, args);
        copy.setStyle(getStyle().createShallowCopy());
        for (ITextComponent s : getSiblings()) copy.appendSibling(s.createCopy());
        return copy;
    }

    public String getKey() {
        return key;
    }

    public Object[] getFormatArgs() {
        return formatArgs;
    }

    @Override
    public net.minecraft.network.chat.MutableComponent mcc$modern() {
        Object[] args = new Object[formatArgs.length];
        for (int i = 0; i < args.length; i++) args[i] = formatArgs[i] instanceof ITextComponent c ? c.mcc$modern() : formatArgs[i];
        var out = net.minecraft.network.chat.Component.translatable(key, args).setStyle(getStyle().mcc$modern());
        for (ITextComponent s : getSiblings()) out.append(s.mcc$modern());
        return out;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof TextComponentTranslation t && Arrays.equals(formatArgs, t.formatArgs) && key.equals(t.key) && super.equals(o);
    }

    @Override
    public int hashCode() {
        return 31 * (31 * super.hashCode() + key.hashCode()) + Arrays.hashCode(formatArgs);
    }

    @Override
    public String toString() {
        return "TranslatableComponent{key='" + key + "', args=" + Arrays.toString(formatArgs) + ", siblings=" + siblings + ", style=" + getStyle() + "}";
    }
}
