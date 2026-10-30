package legacyconnector.v1_12_2.net.minecraft.world;

/** 1.12.2: an inventory with a GUI that a lock code can close (chests, furnaces, the double chest). */
public interface ILockableContainer extends legacyconnector.v1_12_2.net.minecraft.inventory.IInventory, IInteractionObject {
    boolean isLocked();

    void setLockCode(LockCode code);

    LockCode getLockCode();

    @Override
    default legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent getDisplayName() {
        return legacyconnector.v1_12_2.net.minecraft.inventory.IInventory.super.getDisplayName();
    }
}
