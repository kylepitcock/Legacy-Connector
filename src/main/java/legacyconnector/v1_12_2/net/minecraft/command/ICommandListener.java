package legacyconnector.v1_12_2.net.minecraft.command;

/** Receives the feedback commands send ({@code CommandBase.notifyCommandListener}). */
public interface ICommandListener {
    void notifyListener(ICommandSender sender, ICommand command, int flags, String translationKey, Object... translationArgs);
}
