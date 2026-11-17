package legacyconnector.v1_12_2.net.minecraft.nbt;

import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

public class NBTTagString extends NBTBase {
    private final String data;

    public NBTTagString() {
        this("");
    }

    public NBTTagString(String data) {
        if (data == null) throw new IllegalArgumentException("Empty string not allowed");
        this.data = data;
    }

    @Override
    public byte getId() {
        return 8;
    }

    @Override
    public NBTBase copy() {
        return new NBTTagString(data);
    }

    @Override
    public Tag mcc$modern() {
        return StringTag.valueOf(data);
    }

    /** The string as the legacy NBT text form writes it: in double quotes, with backslash and quote escaped. */
    public static String quoteAndEscape(String text) {
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\\' || c == '"') sb.append('\\');
            sb.append(c);
        }
        return sb.append('"').toString();
    }

    @Override
    public String getString() {
        return data;
    }
}
