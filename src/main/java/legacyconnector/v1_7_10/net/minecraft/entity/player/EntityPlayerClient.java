package legacyconnector.v1_7_10.net.minecraft.entity.player;

import net.minecraft.world.entity.player.Player;

/** Legacy view of a non-server player (client-side players, remote players). */
public class EntityPlayerClient extends EntityPlayer {
    public EntityPlayerClient(Player modern) {
        super(modern);
    }
}
