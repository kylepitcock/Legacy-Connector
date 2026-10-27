package legacyconnector.v1_12_2.net.minecraftforge.items;

import java.lang.ref.WeakReference;
import java.util.Objects;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntity;
import legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntityChest;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** Forge's item handler of a double chest: 54 slots, the upper chest's first. */
public class VanillaDoubleChestItemHandler extends WeakReference<TileEntityChest> implements IItemHandlerModifiable {
    public static final VanillaDoubleChestItemHandler NO_ADJACENT_CHESTS_INSTANCE = new VanillaDoubleChestItemHandler(null, null, false);
    private static final EnumFacing[] HORIZONTALS = {EnumFacing.SOUTH, EnumFacing.WEST, EnumFacing.NORTH, EnumFacing.EAST};
    private final boolean mainChestIsUpper;
    private final TileEntityChest mainChest;
    private final int hashCode;

    public VanillaDoubleChestItemHandler(TileEntityChest mainChest, TileEntityChest other, boolean mainChestIsUpper) {
        super(other);
        this.mainChest = mainChest;
        this.mainChestIsUpper = mainChestIsUpper;
        this.hashCode = Objects.hashCode(mainChestIsUpper ? mainChest : other) * 31 + Objects.hashCode(!mainChestIsUpper ? mainChest : other);
    }

    /** The double chest handler of a chest, {@link #NO_ADJACENT_CHESTS_INSTANCE} for a single chest, null where the world is not loaded. */
    public static VanillaDoubleChestItemHandler get(TileEntityChest chest) {
        World world = chest.getWorld();
        BlockPos pos = chest.getPos();
        if (world == null || pos == null || !world.isBlockLoaded(pos)) return null;
        var blockType = chest.getBlockType();
        for (int i = HORIZONTALS.length - 1; i >= 0; i--) {
            EnumFacing facing = HORIZONTALS[i];
            BlockPos other = pos.offset(facing);
            if (blockType != null && world.getBlockState(other).getBlock() == blockType) {
                TileEntity te = world.getTileEntity(other);
                if (te instanceof TileEntityChest otherChest) {
                    return new VanillaDoubleChestItemHandler(chest, otherChest, facing != EnumFacing.WEST && facing != EnumFacing.NORTH);
                }
            }
        }
        return NO_ADJACENT_CHESTS_INSTANCE;
    }

    public TileEntityChest getChest(boolean accessingUpper) {
        return accessingUpper == mainChestIsUpper ? mainChest : getOtherChest();
    }

    private TileEntityChest getOtherChest() {
        TileEntityChest other = get();
        return other != null && !other.isInvalid() ? other : null;
    }

    @Override
    public int getSlots() {
        return 27 * 2;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        boolean upper = slot < 27;
        TileEntityChest chest = getChest(upper);
        return chest != null ? chest.getStackInSlot(upper ? slot : slot - 27) : ItemStack.EMPTY;
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        boolean upper = slot < 27;
        TileEntityChest chest = getChest(upper);
        if (chest != null && chest.getSingleChestHandler() instanceof IItemHandlerModifiable h) h.setStackInSlot(upper ? slot : slot - 27, stack);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        boolean upper = slot < 27;
        TileEntityChest chest = getChest(upper);
        if (chest == null) return stack;
        int start = upper ? 0 : 27;
        return chest.getSingleChestHandler().insertItem(slot - start, stack, simulate);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        boolean upper = slot < 27;
        TileEntityChest chest = getChest(upper);
        if (chest == null) return ItemStack.EMPTY;
        int start = upper ? 0 : 27;
        return chest.getSingleChestHandler().extractItem(slot - start, amount, simulate);
    }

    @Override
    public int getSlotLimit(int slot) {
        TileEntityChest chest = getChest(slot < 27);
        return chest == null ? 0 : chest.getInventoryStackLimit();
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        boolean upper = slot < 27;
        TileEntityChest chest = getChest(upper);
        return chest != null && chest.isItemValidForSlot(upper ? slot : slot - 27, stack);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VanillaDoubleChestItemHandler that = (VanillaDoubleChestItemHandler) o;
        if (hashCode != that.hashCode) return false;
        TileEntityChest otherChest = getOtherChest();
        if (mainChestIsUpper == that.mainChestIsUpper) return Objects.equals(mainChest, that.mainChest) && Objects.equals(otherChest, that.getOtherChest());
        return Objects.equals(mainChest, that.getOtherChest()) && Objects.equals(otherChest, that.mainChest);
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    /** True when the other chest is gone (the handler must be rebuilt). */
    public boolean needsRefresh() {
        if (this == NO_ADJACENT_CHESTS_INSTANCE) return false;
        TileEntityChest other = get();
        return other == null || other.isInvalid();
    }
}
