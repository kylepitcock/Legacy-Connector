package legacyconnector.v1_12_2.net.minecraft.inventory;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.NonNullList;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.Inventories1122;

/** Drops the contents of an inventory into the world (a broken chest or machine), scattered as vanilla does. */
public class InventoryHelper {
    private static final Random RANDOM = new Random();

    public static void dropInventoryItems(World world, BlockPos pos, IInventory inventory) {
        dropInventoryItems(world, pos.getX(), pos.getY(), pos.getZ(), inventory);
    }

    public static void dropInventoryItems(World world, Entity entity, IInventory inventory) {
        dropInventoryItems(world, Entity.mcc$get$posX(entity), Entity.mcc$get$posY(entity), Entity.mcc$get$posZ(entity), inventory);
    }

    private static void dropInventoryItems(World world, double x, double y, double z, IInventory inventory) {
        for (int i = 0; i < inventory.getSizeInventory(); ++i) {
            ItemStack itemstack = inventory.getStackInSlot(i);
            if (!itemstack.isEmpty()) spawnItemStack(world, x, y, z, itemstack);
        }
    }

    public static void dropInventoryItems(World world, BlockPos pos, NonNullList<ItemStack> stacks) {
        for (ItemStack s : stacks) spawnItemStack(world, pos.getX(), pos.getY(), pos.getZ(), s);
    }

    /** The stack in pieces of 10-30 around the point (the stack is emptied, as in 1.12.2). */
    public static void spawnItemStack(World world, double x, double y, double z, ItemStack stack) {
        if (stack.isEmpty()) return;
        Inventories1122.scatter(world, x, y, z, stack.copy());
        stack.setCount(0);
    }
}
