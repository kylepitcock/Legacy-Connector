package legacyconnector.v1_12_2.net.minecraft.inventory;

import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.entity.player.InventoryPlayer;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122;

/**
 * 1.12.2 anvil container: two inputs, the repaired/combined/renamed result, its level cost ({@link #maximumCost}) and the number of
 * repair materials it uses ({@link #materialCost}). The result and costs are the modern anvil's (a scratch modern anvil menu computes
 * them, NeoForge's anvil event included); taking the result costs levels and may damage the anvil block as in 1.12.
 */
public class ContainerRepair extends Container {
    private final IInventory outputSlot = new InventoryCraftResult();
    private final IInventory inputSlots;
    private final World world;
    private final BlockPos pos;
    public int maximumCost;
    public int materialCost;
    private String repairedItemName;
    private final EntityPlayer player;

    public ContainerRepair(InventoryPlayer playerInventory, World worldIn, EntityPlayer player) {
        this(playerInventory, worldIn, BlockPos.ORIGIN, player);
    }

    public ContainerRepair(InventoryPlayer playerInventory, final World worldIn, final BlockPos blockPosIn, EntityPlayer player) {
        this.pos = blockPosIn;
        this.world = worldIn;
        this.player = player;
        this.inputSlots = new InventoryBasic("Repair", true, 2) {
            @Override
            public void markDirty() {
                super.markDirty();
                ContainerRepair.this.onCraftMatrixChanged(this);
            }
        };
        addSlotToContainer(new Slot(inputSlots, 0, 27, 47));
        addSlotToContainer(new Slot(inputSlots, 1, 76, 47));
        addSlotToContainer(new Slot(outputSlot, 2, 134, 47) {
            @Override
            public boolean isItemValid(ItemStack stack) {
                return false;
            }

            @Override
            public boolean canTakeStack(EntityPlayer playerIn) {
                var p = playerIn == null ? null : playerIn.mcc$player();
                boolean afford = p == null || p.getAbilities().instabuild || p.experienceLevel >= maximumCost;
                return afford && maximumCost > 0 && getHasStack();
            }

            @Override
            public ItemStack onTake(EntityPlayer thePlayer, ItemStack stack) {
                return mcc$take(thePlayer, stack);
            }
        });
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) addSlotToContainer(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
        }
        for (int k = 0; k < 9; ++k) addSlotToContainer(new Slot(playerInventory, k, 8 + k * 18, 142));
    }

    /** Taking the result: pays the levels, uses up the inputs and wears the anvil (12% per use, none in creative). */
    private ItemStack mcc$take(EntityPlayer thePlayer, ItemStack stack) {
        var p = thePlayer == null ? null : thePlayer.mcc$player();
        boolean creative = p != null && p.getAbilities().instabuild;
        if (p != null && !creative) p.giveExperienceLevels(-maximumCost);
        float breakChance = 0.12F;
        if (p != null) {
            breakChance = net.neoforged.neoforge.common.CommonHooks.onAnvilRepair(p, Stacks1122.toModern(stack), Stacks1122.toModern(inputSlots.getStackInSlot(0)),
                    Stacks1122.toModern(inputSlots.getStackInSlot(1)));
        }
        inputSlots.setInventorySlotContents(0, ItemStack.EMPTY);
        if (materialCost > 0) {
            ItemStack material = inputSlots.getStackInSlot(1);
            if (!material.isEmpty() && material.getCount() > materialCost) {
                material.shrink(materialCost);
                inputSlots.setInventorySlotContents(1, material);
            } else {
                inputSlots.setInventorySlotContents(1, ItemStack.EMPTY);
            }
        } else {
            inputSlots.setInventorySlotContents(1, ItemStack.EMPTY);
        }
        maximumCost = 0;
        var level = world == null ? null : world.mcc$level();
        if (level != null && !level.isClientSide) {
            var mp = pos.mcc$modern();
            var state = level.getBlockState(mp);
            if (!creative && state.is(net.minecraft.tags.BlockTags.ANVIL) && level.random.nextFloat() < breakChance) {
                var damaged = net.minecraft.world.level.block.AnvilBlock.damage(state);
                if (damaged == null) {
                    level.removeBlock(mp, false);
                    level.levelEvent(1029, mp, 0);
                } else {
                    level.setBlock(mp, damaged, 2);
                    level.levelEvent(1030, mp, 0);
                }
            } else {
                level.levelEvent(1030, mp, 0);
            }
        }
        return stack;
    }

    @Override
    public void onCraftMatrixChanged(IInventory inventoryIn) {
        super.onCraftMatrixChanged(inventoryIn);
        if (inventoryIn == inputSlots) updateRepairOutput();
    }

    /** Recomputes the result and its costs from the inputs and the name typed (the modern anvil's rules). */
    public void updateRepairOutput() {
        var p = player == null ? null : player.mcc$player();
        ItemStack first = inputSlots.getStackInSlot(0);
        if (p == null || first.isEmpty()) {
            outputSlot.setInventorySlotContents(0, ItemStack.EMPTY);
            maximumCost = 0;
            materialCost = 0;
            detectAndSendChanges();
            return;
        }
        var menu = new net.minecraft.world.inventory.AnvilMenu(-1, p.getInventory(), net.minecraft.world.inventory.ContainerLevelAccess.NULL);
        menu.getSlot(0).set(Stacks1122.toModern(first));
        menu.getSlot(1).set(Stacks1122.toModern(inputSlots.getStackInSlot(1)));
        if (repairedItemName != null) menu.setItemName(repairedItemName);
        menu.createResult();
        outputSlot.setInventorySlotContents(0, Stacks1122.fromModern(menu.getSlot(2).getItem()));
        maximumCost = menu.getCost();
        materialCost = menu.repairItemCountCost;
        detectAndSendChanges();
    }

    @Override
    public void addListener(IContainerListener listener) {
        super.addListener(listener);
        listener.sendWindowProperty(this, 0, maximumCost);
    }

    @Override
    public void updateProgressBar(int id, int data) {
        if (id == 0) maximumCost = data;
    }

    @Override
    public void onContainerClosed(EntityPlayer playerIn) {
        super.onContainerClosed(playerIn);
        if (world != null && !world.isRemote) clearContainer(playerIn, world, inputSlots);
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        var level = world == null ? null : world.mcc$level();
        if (level != null && pos != BlockPos.ORIGIN && !level.getBlockState(pos.mcc$modern()).is(net.minecraft.tags.BlockTags.ANVIL)) return false;
        return pos == BlockPos.ORIGIN
                || playerIn.getDistanceSq((double) pos.getX() + 0.5D, (double) pos.getY() + 0.5D, (double) pos.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = inventorySlots.get(index);
        if (slot != null && slot.getHasStack()) {
            ItemStack itemstack1 = slot.getStack();
            itemstack = itemstack1.copy();
            if (index == 2) {
                if (!mergeItemStack(itemstack1, 3, 39, true)) return ItemStack.EMPTY;
                slot.onSlotChange(itemstack1, itemstack);
            } else if (index != 0 && index != 1) {
                if (index >= 3 && index < 39 && !mergeItemStack(itemstack1, 0, 2, false)) return ItemStack.EMPTY;
            } else if (!mergeItemStack(itemstack1, 3, 39, false)) {
                return ItemStack.EMPTY;
            }
            if (itemstack1.isEmpty()) slot.putStack(ItemStack.EMPTY);
            else slot.onSlotChanged();
            if (itemstack1.getCount() == itemstack.getCount()) return ItemStack.EMPTY;
            slot.onTake(playerIn, itemstack1);
        }
        return itemstack;
    }

    /** The name typed into the anvil: renames the result (an empty name removes a custom name). */
    public void updateItemName(String newName) {
        this.repairedItemName = newName;
        if (getSlot(2).getHasStack()) {
            ItemStack stack = getSlot(2).getStack();
            if (newName == null || newName.trim().isEmpty()) stack.clearCustomName();
            else stack.setStackDisplayName(repairedItemName);
        }
        updateRepairOutput();
    }
}
