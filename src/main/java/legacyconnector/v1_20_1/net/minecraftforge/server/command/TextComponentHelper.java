package legacyconnector.v1_20_1.net.minecraftforge.server.command;

import net.minecraft.commands.CommandSource;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * Forge 1.20.1's {@code TextComponentHelper} (removed in NeoForge 1.21.1): command feedback as a translatable component. Forge sent vanilla clients
 * (which lack the mod's language files) the text formatted on the server; a 1.21.1 client of this pack has the mod's translations, so the key is sent.
 */
public final class TextComponentHelper {
    private TextComponentHelper() {}

    public static MutableComponent createComponentTranslation(CommandSource source, String translation, Object... args) {
        return Component.translatable(translation, args);
    }
}
