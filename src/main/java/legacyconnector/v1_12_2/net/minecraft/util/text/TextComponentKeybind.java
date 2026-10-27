package legacyconnector.v1_12_2.net.minecraft.util.text;

/** The key bound to a control, shown as its key name (the bare control name until the client resolves it). */
public class TextComponentKeybind extends TextComponentBase {
    private final String keybind;

    public TextComponentKeybind(String keybind) {
        this.keybind = keybind;
    }

    public String getKeybind() {
        return keybind;
    }

    @Override
    public String getUnformattedComponentText() {
        return keybind;
    }

    @Override
    public TextComponentKeybind createCopy() {
        TextComponentKeybind copy = new TextComponentKeybind(keybind);
        copy.setStyle(getStyle().createShallowCopy());
        for (ITextComponent s : getSiblings()) copy.appendSibling(s.createCopy());
        return copy;
    }

    @Override
    public net.minecraft.network.chat.MutableComponent mcc$modern() {
        var out = net.minecraft.network.chat.Component.keybind(keybind).setStyle(getStyle().mcc$modern());
        for (ITextComponent s : getSiblings()) out.append(s.mcc$modern());
        return out;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof TextComponentKeybind k && keybind.equals(k.keybind) && super.equals(o);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + keybind.hashCode();
    }

    @Override
    public String toString() {
        return "KeybindComponent{keybind='" + keybind + "', siblings=" + siblings + ", style=" + getStyle() + "}";
    }
}
