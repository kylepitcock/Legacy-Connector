package legacyconnector.v1_12_2.net.minecraft.block;

import legacyconnector.v1_12_2.net.minecraft.dispenser.BehaviorDefaultDispenseItem;
import legacyconnector.v1_12_2.net.minecraft.dispenser.IBehaviorDispenseItem;
import legacyconnector.v1_12_2.net.minecraft.inventory.IInventory;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntity;
import legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntityDispenser;
import legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntityDropper;
import legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntityHopper;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2's dropper: a dispenser that only drops items, or moves them into the inventory it faces. */
public class BlockDropper extends BlockDispenser {
    private final IBehaviorDispenseItem dropBehavior = new BehaviorDefaultDispenseItem();

    public BlockDropper() {
    }

    @Override
    protected IBehaviorDispenseItem getBehavior(ItemStack stack) {
        return dropBehavior;
    }

    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityDropper();
    }

    @Override
    protected void dispense(World worldIn, BlockPos pos) {
        BlockSourceImpl source = new BlockSourceImpl(worldIn, pos);
        if (!(source.<TileEntity>getBlockTileEntity() instanceof TileEntityDispenser te)) return;
        int slot = te.getDispenseSlot();
        if (slot < 0) {
            worldIn.playEvent(1001, pos, 0);
            return;
        }
        ItemStack stack = te.getStackInSlot(slot);
        if (stack.isEmpty()) return;
        EnumFacing facing = source.mcc$facing();
        BlockPos front = pos.offset(facing);
        IInventory target = TileEntityHopper.getInventoryAtPosition(worldIn, front.getX(), front.getY(), front.getZ());
        ItemStack rest;
        if (target == null) {
            rest = dropBehavior.dispense(source, stack);
        } else {
            rest = TileEntityHopper.putStackInInventoryAllSlots(te, target, stack.copy().splitStack(1), facing.getOpposite());
            if (rest.isEmpty()) {
                rest = stack.copy();
                rest.shrink(1);
            } else {
                rest = stack.copy();
            }
        }
        te.setInventorySlotContents(slot, rest);
    }
}
