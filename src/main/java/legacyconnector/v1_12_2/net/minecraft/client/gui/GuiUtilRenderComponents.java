package legacyconnector.v1_12_2.net.minecraft.client.gui;

import java.util.ArrayList;
import java.util.List;
import legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent;
import org.kylepitcock.legacyconnector.v1_12_2.client.Fields1122;

/** Wraps chat components to a width as the chat does, keeping their styles. */
public class GuiUtilRenderComponents {
    /** The text without colour codes when the player turned chat colours off (unless colours are forced). */
    public static String removeTextColorsIfConfigured(String text, boolean forceTextColor) {
        boolean colors = net.minecraft.client.Minecraft.getInstance().options.chatColors().get();
        return !forceTextColor && !colors ? legacyconnector.v1_12_2.net.minecraft.util.text.TextFormatting.getTextWithoutFormattingCodes(text) : text;
    }

    /** The component split into lines no wider than {@code maxTextLenght} pixels (the modern chat's wrapping). */
    public static List<ITextComponent> splitText(ITextComponent textComponent, int maxTextLenght, FontRenderer fontRendererIn, boolean trimSpace,
            boolean forceTextColor) {
        List<ITextComponent> out = new ArrayList<>();
        if (textComponent == null) return out;
        var font = net.minecraft.client.Minecraft.getInstance().font;
        for (var line : net.minecraft.client.gui.components.ComponentRenderUtils.wrapComponents(textComponent.mcc$modern(), maxTextLenght, font)) {
            out.add(Fields1122.legacy(Fields1122.component(line)));
        }
        return out;
    }
}
