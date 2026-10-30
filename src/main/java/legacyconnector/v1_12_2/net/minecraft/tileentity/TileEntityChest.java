package legacyconnector.v1_12_2.net.minecraft.tileentity;

import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.entity.player.InventoryPlayer;
import legacyconnector.v1_12_2.net.minecraft.inventory.Container;
import legacyconnector.v1_12_2.net.minecraft.inventory.ItemStackHelper;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.ITickable;
import legacyconnector.v1_12_2.net.minecraft.util.NonNullList;
import legacyconnector.v1_12_2.net.minecraft.util.SoundCategory;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraftforge.common.capabilities.Capability;
import legacyconnector.v1_12_2.net.minecraftforge.items.CapabilityItemHandler;
import legacyconnector.v1_12_2.net.minecraftforge.items.IItemHandler;
import legacyconnector.v1_12_2.net.minecraftforge.items.VanillaDoubleChestItemHandler;

/**
 * 1.12.2's chest. Mods' chests extend it (27 slots, the lid animation, the open count sent as block event 1, double chests through
 * adjacent chests of the same block). A vanilla chest in the world is seen through a view of this class over the modern chest: its
 * {@link #chestContents} are the real slots and opening it counts on the modern chest.
 */
public class TileEntityChest extends TileEntityLockableLoot implements ITickable {
    private NonNullList<ItemStack> chestContents = NonNullList.withSize(27, ItemStack.EMPTY);
    public boolean adjacentChestChecked;
    public TileEntityChest adjacentChestZNeg;
    public TileEntityChest adjacentChestXPos;
    public TileEntityChest adjacentChestXNeg;
    public TileEntityChest adjacentChestZPos;
    public float lidAngle;
    public float prevLidAngle;
    public int numPlayersUsing;
    private int ticksSinceSync;
    public VanillaDoubleChestItemHandler doubleChestHandler;
    /** For a view: the modern chest. */
    private final net.minecraft.world.level.block.entity.ChestBlockEntity mcc$chest;

    public TileEntityChest() {
        this.mcc$chest = null;
    }

    /** A view of a game chest. */
    public TileEntityChest(net.minecraft.world.level.block.entity.ChestBlockEntity chest) {
        this.mcc$chest = chest;
        this.chestContents = new org.kylepitcock.legacyconnector.v1_12_2.bridge.ModernStackList1122(chest);
    }

    /** Refreshes a view's open count and lid from the modern chest. */
    public void mcc$refresh() {
        if (mcc$chest == null || mcc$chest.getLevel() == null) return;
        numPlayersUsing = net.minecraft.world.level.block.entity.ChestBlockEntity.getOpenCount(mcc$chest.getLevel(), mcc$chest.getBlockPos());
        prevLidAngle = lidAngle;
        lidAngle = mcc$chest.getOpenNess(1.0F);
    }

    @Override
    public int getSizeInventory() {
        return chestContents.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack s : chestContents) if (!s.isEmpty()) return false;
        return true;
    }

    @Override
    public String getName() {
        return hasCustomName() ? customName : "container.chest";
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        if (mcc$chest == null) chestContents = NonNullList.withSize(getSizeInventory(), ItemStack.EMPTY);
        if (!checkLootAndRead(compound)) ItemStackHelper.loadAllItems(compound, chestContents);
        if (compound.hasKey("CustomName", 8)) customName = compound.getString("CustomName");
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        if (!checkLootAndWrite(compound)) ItemStackHelper.saveAllItems(compound, chestContents);
        if (hasCustomName()) compound.setString("CustomName", customName);
        return compound;
    }

    @Override
    public int getInventoryStackLimit() {
        return 64;
    }

    @Override
    public void updateContainingBlockInfo() {
        super.updateContainingBlockInfo();
        adjacentChestChecked = false;
        doubleChestHandler = null;
    }

    private void setNeighbor(TileEntityChest chestTe, EnumFacing side) {
        if (chestTe.isInvalid()) {
            adjacentChestChecked = false;
        } else if (adjacentChestChecked) {
            switch (side) {
                case NORTH -> { if (adjacentChestZNeg != chestTe) adjacentChestChecked = false; }
                case SOUTH -> { if (adjacentChestZPos != chestTe) adjacentChestChecked = false; }
                case EAST -> { if (adjacentChestXPos != chestTe) adjacentChestChecked = false; }
                case WEST -> { if (adjacentChestXNeg != chestTe) adjacentChestChecked = false; }
                default -> { }
            }
        }
    }

    /** Finds the chests of the same block next to this one (a double chest is two of them side by side). */
    public void checkForAdjacentChests() {
        if (adjacentChestChecked || world == null) return;
        adjacentChestChecked = true;
        adjacentChestXNeg = getAdjacentChest(EnumFacing.WEST);
        adjacentChestXPos = getAdjacentChest(EnumFacing.EAST);
        adjacentChestZNeg = getAdjacentChest(EnumFacing.NORTH);
        adjacentChestZPos = getAdjacentChest(EnumFacing.SOUTH);
    }

    protected TileEntityChest getAdjacentChest(EnumFacing side) {
        BlockPos at = pos.offset(side);
        if (!isChestAt(at)) return null;
        TileEntity te = world.getTileEntity(at);
        if (te instanceof TileEntityChest chest) {
            chest.setNeighbor(this, side.getOpposite());
            return chest;
        }
        return null;
    }

    private boolean isChestAt(BlockPos posIn) {
        if (world == null) return false;
        if (mcc$chest != null) {
            var level = world.mcc$level();
            if (level == null) return false;
            var here = mcc$chest.getBlockState();
            var there = level.getBlockState(new net.minecraft.core.BlockPos(posIn.getX(), posIn.getY(), posIn.getZ()));
            return there.getBlock() == here.getBlock();
        }
        var block = world.getBlockState(posIn).getBlock();
        return block != null && block == getBlockType();
    }

    /** Animates the lid and plays the chest sounds (the chest at the north-west end of a double chest plays them). */
    @Override
    public void update() {
        checkForAdjacentChests();
        int i = pos.getX();
        int j = pos.getY();
        int k = pos.getZ();
        ++ticksSinceSync;
        prevLidAngle = lidAngle;
        if (numPlayersUsing > 0 && lidAngle == 0.0F && adjacentChestZNeg == null && adjacentChestXNeg == null) {
            double d1 = i + 0.5D;
            double d2 = k + 0.5D;
            if (adjacentChestZPos != null) d2 += 0.5D;
            if (adjacentChestXPos != null) d1 += 0.5D;
            world.playSound(null, d1, j + 0.5D, d2, legacyconnector.v1_12_2.net.minecraft.init.SoundEvents.BLOCK_CHEST_OPEN, SoundCategory.BLOCKS, 0.5F,
                    world.rand.nextFloat() * 0.1F + 0.9F);
        }
        if (numPlayersUsing == 0 && lidAngle > 0.0F || numPlayersUsing > 0 && lidAngle < 1.0F) {
            float before = lidAngle;
            if (numPlayersUsing > 0) lidAngle += 0.1F;
            else lidAngle -= 0.1F;
            if (lidAngle > 1.0F) lidAngle = 1.0F;
            if (lidAngle < 0.5F && before >= 0.5F && adjacentChestZNeg == null && adjacentChestXNeg == null) {
                double d3 = i + 0.5D;
                double d0 = k + 0.5D;
                if (adjacentChestZPos != null) d0 += 0.5D;
                if (adjacentChestXPos != null) d3 += 0.5D;
                world.playSound(null, d3, j + 0.5D, d0, legacyconnector.v1_12_2.net.minecraft.init.SoundEvents.BLOCK_CHEST_CLOSE, SoundCategory.BLOCKS, 0.5F,
                        world.rand.nextFloat() * 0.1F + 0.9F);
            }
            if (lidAngle < 0.0F) lidAngle = 0.0F;
        }
    }

    @Override
    public boolean receiveClientEvent(int id, int type) {
        if (id == 1) {
            numPlayersUsing = type;
            return true;
        }
        return super.receiveClientEvent(id, type);
    }

    private static net.minecraft.world.entity.player.Player modern(EntityPlayer player) {
        return player != null && player.mcc$entity instanceof net.minecraft.world.entity.player.Player p ? p : null;
    }

    @Override
    public void openInventory(EntityPlayer player) {
        if (player == null || player.isSpectator()) return;
        if (mcc$chest != null) {
            var p = modern(player);
            if (p != null) mcc$chest.startOpen(p);
            return;
        }
        if (numPlayersUsing < 0) numPlayersUsing = 0;
        ++numPlayersUsing;
        world.addBlockEvent(pos, getBlockType(), 1, numPlayersUsing);
        world.notifyNeighborsOfStateChange(pos, getBlockType(), false);
    }

    @Override
    public void closeInventory(EntityPlayer player) {
        if (player == null || player.isSpectator()) return;
        if (mcc$chest != null) {
            var p = modern(player);
            if (p != null) mcc$chest.stopOpen(p);
            return;
        }
        --numPlayersUsing;
        world.addBlockEvent(pos, getBlockType(), 1, numPlayersUsing);
        world.notifyNeighborsOfStateChange(pos, getBlockType(), false);
    }

    /** The item handler of this chest alone (the capability is the double chest's when there is one). */
    public IItemHandler getSingleChestHandler() {
        return super.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
        if (capability != null && capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            if (doubleChestHandler == null || doubleChestHandler.needsRefresh()) doubleChestHandler = VanillaDoubleChestItemHandler.get(this);
            if (doubleChestHandler != null && doubleChestHandler != VanillaDoubleChestItemHandler.NO_ADJACENT_CHESTS_INSTANCE) return (T) doubleChestHandler;
        }
        return super.getCapability(capability, facing);
    }

    @Override
    public void invalidate() {
        super.invalidate();
        updateContainingBlockInfo();
        checkForAdjacentChests();
    }

    @Override
    public String getGuiID() {
        return "minecraft:chest";
    }

    @Override
    public Container createContainer(InventoryPlayer playerInventory, EntityPlayer playerIn) {
        fillWithLoot(playerIn);
        return new legacyconnector.v1_12_2.net.minecraft.inventory.ContainerChest(playerInventory, this, playerIn);
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return chestContents;
    }
}
