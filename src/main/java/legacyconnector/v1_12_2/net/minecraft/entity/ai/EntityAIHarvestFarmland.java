package legacyconnector.v1_12_2.net.minecraft.entity.ai;

import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.passive.EntityVillager;
import legacyconnector.v1_12_2.net.minecraft.inventory.InventoryBasic;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/**
 * A farmer villager walks to farmland, harvests ripe crops on it (when it wants food) and plants seeds from its inventory on empty
 * farmland. Crops are the modern crop blocks, seeds any item that places one.
 */
public class EntityAIHarvestFarmland extends EntityAIMoveToBlock {
    private final EntityVillager villager;
    private boolean hasFarmItem;
    private boolean wantsToReapStuff;
    /** 0: harvest, 1: plant, -1: nothing chosen. */
    private int currentTask;

    public EntityAIHarvestFarmland(EntityVillager villager, double speed) {
        super(villager, speed, 16);
        this.villager = villager;
    }

    @Override
    public boolean shouldExecute() {
        if (runDelay <= 0) {
            var world = Entity.mcc$get$world(villager);
            if (world == null || !world.getGameRules().getBoolean("mobGriefing")) return false;
            currentTask = -1;
            hasFarmItem = villager.isFarmItemInInventory();
            wantsToReapStuff = villager.wantsMoreFood();
        }
        return super.shouldExecute();
    }

    @Override
    public boolean shouldContinueExecuting() {
        return currentTask >= 0 && super.shouldContinueExecuting();
    }

    private static boolean ripe(net.minecraft.world.level.block.state.BlockState state) {
        return state.getBlock() instanceof net.minecraft.world.level.block.CropBlock crop && crop.isMaxAge(state);
    }

    @Override
    public void updateTask() {
        super.updateTask();
        villager.getLookHelper().setLookPosition(destinationBlock.getX() + 0.5D, destinationBlock.getY() + 1, destinationBlock.getZ() + 0.5D, 10.0F,
                villager.getVerticalFaceSpeed());
        if (!getIsAboveDestination()) return;
        World world = Entity.mcc$get$world(villager);
        var access = world == null ? null : world.mcc$access();
        if (access != null) {
            BlockPos up = destinationBlock.up();
            var p = up.mcc$modern();
            var state = access.getBlockState(p);
            if (currentTask == 0 && ripe(state)) {
                world.destroyBlock(up, true);
            } else if (currentTask == 1 && state.isAir()) {
                InventoryBasic inv = villager.getVillagerInventory();
                for (int i = 0; i < inv.getSizeInventory(); ++i) {
                    ItemStack stack = inv.getStackInSlot(i);
                    if (stack.isEmpty() || !(stack.getItem().mcc$modern instanceof net.minecraft.world.item.BlockItem b)
                            || !(b.getBlock() instanceof net.minecraft.world.level.block.CropBlock crop)) {
                        continue;
                    }
                    access.setBlock(p, crop.defaultBlockState(), 3);
                    stack.shrink(1);
                    if (stack.isEmpty()) inv.setInventorySlotContents(i, ItemStack.EMPTY);
                    break;
                }
            }
        }
        currentTask = -1;
        runDelay = 10;
    }

    @Override
    protected boolean shouldMoveTo(World world, BlockPos pos) {
        var access = world == null ? null : world.mcc$access();
        if (access == null) return false;
        if (!(access.getBlockState(pos.mcc$modern()).getBlock() instanceof net.minecraft.world.level.block.FarmBlock)) return false;
        var above = access.getBlockState(pos.mcc$modern().above());
        if (ripe(above) && wantsToReapStuff && (currentTask == 0 || currentTask < 0)) {
            currentTask = 0;
            return true;
        }
        if (above.isAir() && hasFarmItem && (currentTask == 1 || currentTask < 0)) {
            currentTask = 1;
            return true;
        }
        return false;
    }
}
