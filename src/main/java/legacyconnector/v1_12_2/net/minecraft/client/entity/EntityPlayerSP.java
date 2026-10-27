package legacyconnector.v1_12_2.net.minecraft.client.entity;

/** The local player: a view of the modern local player. */
public class EntityPlayerSP extends AbstractClientPlayer {
    public EntityPlayerSP(net.minecraft.world.entity.player.Player modern) {
        super(modern);
    }

    @Override
    public boolean isUser() {
        return true;
    }

    @Override
    public void sendMessage(legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent component) {
        if (component != null) mcc$player().displayClientMessage(component.mcc$modern(), false);
    }
}
