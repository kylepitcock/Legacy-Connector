package legacyconnector.v1_12_2.net.minecraftforge.items.wrapper;

import legacyconnector.v1_12_2.net.minecraft.entity.player.InventoryPlayer;

/** Forge: the 36 main slots of a player's inventory (hotbar first) as an item handler. */
public class PlayerMainInvWrapper extends RangedWrapper {
    private final InventoryPlayer inventoryPlayer;

    public PlayerMainInvWrapper(InventoryPlayer inv) {
        super(new InvWrapper(inv), 0, 36);
        this.inventoryPlayer = inv;
    }

    public InventoryPlayer getInventoryPlayer() {
        return inventoryPlayer;
    }
}
