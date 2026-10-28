package legacyconnector.v1_12_2.net.minecraftforge.items.wrapper;

import legacyconnector.v1_12_2.net.minecraft.entity.player.InventoryPlayer;

/** Forge: a player's whole inventory (main, armour, off hand) as one item handler. */
public class PlayerInvWrapper extends CombinedInvWrapper {
    public PlayerInvWrapper(InventoryPlayer inv) {
        super(new PlayerMainInvWrapper(inv), new PlayerArmorInvWrapper(inv), new PlayerOffhandInvWrapper(inv));
    }
}
