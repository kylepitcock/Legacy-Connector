package mccompat.v1_7_10.net.minecraft.client.entity;

import net.minecraft.world.entity.player.Player;

public class EntityPlayerSP extends AbstractClientPlayer {
    public EntityPlayerSP(Player modern) {
        super(modern);
    }

    public void closeScreen() {
        mcc$player().closeContainer();
    }
}
