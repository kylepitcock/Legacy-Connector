package legacyconnector.v1_12_2.net.minecraft.client.gui.chat;

import legacyconnector.v1_12_2.net.minecraft.util.text.ChatType;
import legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent;

/** Receives every chat message of a kind (the HUD's chat and action bar, the narrator). */
public interface IChatListener {
    void say(ChatType chatTypeIn, ITextComponent message);
}
