package legacyconnector.v1_12_2.net.minecraft.world;

/**
 * 1.12.2 interaction object: something that opens a container GUI through {@code EntityPlayer.displayGui} (crafting tables, anvils,
 * enchanting tables and the inventories of mods' tile entities). {@link #getGuiID} names the vanilla GUI the client shows.
 * (1.12 inherited the name methods from IWorldNameable, which the layer only synthesizes; they are declared here.)
 */
public interface IInteractionObject {
    legacyconnector.v1_12_2.net.minecraft.inventory.Container createContainer(legacyconnector.v1_12_2.net.minecraft.entity.player.InventoryPlayer playerInventory,
            legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player);

    String getGuiID();

    String getName();

    boolean hasCustomName();

    legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent getDisplayName();
}
