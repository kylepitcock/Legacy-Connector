package legacyconnector.v1_12_2.net.minecraftforge.items.wrapper;

import legacyconnector.v1_12_2.net.minecraft.entity.player.InventoryPlayer;

/** Forge: the off-hand slot of a player's inventory as an item handler. */
public class PlayerOffhandInvWrapper extends RangedWrapper {
    public PlayerOffhandInvWrapper(InventoryPlayer inv) {
        super(new InvWrapper(inv), 40, 41);
    }
}
