package legacyconnector.v1_12_2.net.minecraft.network;

import legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent;

/** The handler of one connection's packets. */
public interface INetHandler {
    void onDisconnect(ITextComponent reason);
}
