package legacyconnector.v1_12_2.net.minecraftforge.server.command;

import legacyconnector.v1_12_2.net.minecraft.command.ICommandSender;
import legacyconnector.v1_12_2.net.minecraft.util.text.TextComponentBase;
import legacyconnector.v1_12_2.net.minecraft.util.text.TextComponentTranslation;

/**
 * A translated message for a command sender. 1.12 sent clients without the mod the text translated on the server; the layer serves the
 * mods' language files to every client (as resources), so the message stays a translation.
 */
public class TextComponentHelper {
    private TextComponentHelper() {
    }

    public static TextComponentBase createComponentTranslation(ICommandSender sender, String translation, Object... args) {
        return new TextComponentTranslation(translation, args);
    }
}
