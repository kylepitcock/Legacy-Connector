package legacyconnector.v1_12_2.net.minecraft.util.text;

import legacyconnector.v1_12_2.net.minecraft.command.ICommandSender;
import legacyconnector.v1_12_2.net.minecraft.entity.Entity;

/** Text helpers of commands. */
public class TextComponentUtils {
    private TextComponentUtils() {
    }

    /**
     * The component with its score and selector parts resolved for the sender and entity. The legacy text components are plain text,
     * translations and keybinds (none of them depends on who sees it), so this is a copy of the component and its children.
     */
    public static ITextComponent processComponent(ICommandSender commandSender, ITextComponent component, Entity entityIn) {
        return component == null ? null : component.createCopy();
    }
}
