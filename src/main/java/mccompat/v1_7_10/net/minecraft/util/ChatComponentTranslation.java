package mccompat.v1_7_10.net.minecraft.util;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class ChatComponentTranslation extends ChatComponentStyle {
    private final String key;
    private final Object[] formatArgs;

    public ChatComponentTranslation(String key, Object... args) {
        this.key = key;
        this.formatArgs = args;
    }

    public String getKey() {
        return key;
    }

    public Object[] getFormatArgs() {
        return formatArgs;
    }

    @Override
    public String getUnformattedTextForChat() {
        return mcc$self().getString();
    }

    @Override
    public IChatComponent createCopy() {
        ChatComponentTranslation copy = new ChatComponentTranslation(key, formatArgs.clone());
        copy.setChatStyle(getChatStyle().createShallowCopy());
        for (IChatComponent s : getSiblings()) copy.appendSibling(s.createCopy());
        return copy;
    }

    @Override
    protected MutableComponent mcc$self() {
        Object[] args = new Object[formatArgs.length];
        for (int i = 0; i < args.length; i++) {
            args[i] = formatArgs[i] instanceof IChatComponent c ? c.mcc$toModern() : formatArgs[i];
        }
        return Component.translatable(key, args);
    }
}
