package legacyconnector.v1_12_2.net.minecraft.network.play.server;

import java.io.IOException;
import legacyconnector.v1_12_2.net.minecraft.network.Packet;
import legacyconnector.v1_12_2.net.minecraft.network.PacketBuffer;
import legacyconnector.v1_12_2.net.minecraft.network.play.INetHandlerPlayClient;

/** A title, subtitle or action bar text, or its timing. */
public class SPacketTitle implements Packet<INetHandlerPlayClient> {
    private Type type;
    private legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent message;
    private int fadeInTime;
    private int displayTime;
    private int fadeOutTime;

    public SPacketTitle() {
    }

    public SPacketTitle(Type typeIn, legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent messageIn) {
        this(typeIn, messageIn, -1, -1, -1);
    }

    public SPacketTitle(int fadeInTimeIn, int displayTimeIn, int fadeOutTimeIn) {
        this(Type.TIMES, null, fadeInTimeIn, displayTimeIn, fadeOutTimeIn);
    }

    public SPacketTitle(Type typeIn, legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent messageIn, int fadeInTimeIn, int displayTimeIn, int fadeOutTimeIn) {
        this.type = typeIn;
        this.message = messageIn;
        this.fadeInTime = fadeInTimeIn;
        this.displayTime = displayTimeIn;
        this.fadeOutTime = fadeOutTimeIn;
    }

    public Type getType() {
        return type;
    }

    public legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent getMessage() {
        return message;
    }

    public int getFadeInTime() {
        return fadeInTime;
    }

    public int getDisplayTime() {
        return displayTime;
    }

    public int getFadeOutTime() {
        return fadeOutTime;
    }

    @Override
    public void readPacketData(PacketBuffer buf) throws IOException {
        type = buf.readEnumValue(Type.class);
        if (type == Type.TITLE || type == Type.SUBTITLE || type == Type.ACTIONBAR) message = buf.readTextComponent();
        if (type == Type.TIMES) {
            fadeInTime = buf.readInt();
            displayTime = buf.readInt();
            fadeOutTime = buf.readInt();
        }
    }

    @Override
    public void writePacketData(PacketBuffer buf) throws IOException {
        buf.writeEnumValue(type);
        if (type == Type.TITLE || type == Type.SUBTITLE || type == Type.ACTIONBAR) buf.writeTextComponent(message);
        if (type == Type.TIMES) {
            buf.writeInt(fadeInTime);
            buf.writeInt(displayTime);
            buf.writeInt(fadeOutTime);
        }
    }

    /** Handled by the modern client as the modern packet. */
    @Override
    public void processPacket(INetHandlerPlayClient handler) {
    }

    @Override
    public net.minecraft.network.protocol.Packet<?> mcc$modern() {
        net.minecraft.network.chat.Component text = message == null ? net.minecraft.network.chat.Component.empty() : message.mcc$modern();
        return switch (type) {
            case TITLE -> new net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket(text);
            case SUBTITLE -> new net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket(text);
            case ACTIONBAR -> new net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket(text);
            case TIMES -> new net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket(fadeInTime, displayTime, fadeOutTime);
            case CLEAR -> new net.minecraft.network.protocol.game.ClientboundClearTitlesPacket(false);
            case RESET -> new net.minecraft.network.protocol.game.ClientboundClearTitlesPacket(true);
        };
    }

    public enum Type {
        TITLE,
        SUBTITLE,
        ACTIONBAR,
        TIMES,
        CLEAR,
        RESET;

        public static Type byName(String name) {
            for (Type t : values()) if (t.name().equalsIgnoreCase(name)) return t;
            return TITLE;
        }

        public static String[] getNames() {
            String[] out = new String[values().length];
            for (int i = 0; i < out.length; i++) out[i] = values()[i].name().toLowerCase(java.util.Locale.ROOT);
            return out;
        }
    }
}
