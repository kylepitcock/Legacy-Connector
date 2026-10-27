package legacyconnector.v1_12_2.net.minecraft.client.gui.chat;

import legacyconnector.v1_12_2.net.minecraft.util.text.ChatType;
import legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent;

/** The narrator, as the modern game's narrator: chat messages are read out as the narrator setting allows. */
public class NarratorChatListener implements IChatListener {
    public static final NarratorChatListener INSTANCE = new NarratorChatListener();

    private static net.minecraft.client.GameNarrator narrator() {
        return net.minecraft.client.Minecraft.getInstance().getNarrator();
    }

    @Override
    public void say(ChatType chatTypeIn, ITextComponent message) {
        if (message == null) return;
        if (chatTypeIn == ChatType.CHAT) narrator().sayChat(message.mcc$modern());
        else narrator().say(message.mcc$modern());
    }

    /** Announces the narrator mode after it was switched (0 off, 1 all, 2 chat, 3 system). */
    public void announceMode(int mode) {
        var status = net.minecraft.client.NarratorStatus.byId(mode);
        narrator().updateNarratorStatus(status);
    }

    public boolean isActive() {
        return narrator().isActive();
    }

    public void clear() {
        narrator().clear();
    }
}
