package legacyconnector.v1_12_2.net.minecraft.inventory;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** Spills inventories into the world (a broken chest), in small random stacks as 1.12 did. */
public class InventoryHelper {
    private static final Random RANDOM = new Random();

    public static void dropInventoryItems(World world, BlockPos pos, IInventory inventory) {
        dropInventoryItems(world, pos.getX(), pos.getY(), pos.getZ(), inventory);
    }

    public static void dropInventoryItems(World world, legacyconnector.v1_12_2.net.minecraft.entity.Entity at, IInventory inventory) {
        var m = at == null ? null : at.mcc$entity;
        if (m == null) return;
        dropInventoryItems(world, m.getX(), m.getY(), m.getZ(), inventory);
    }

    private static void dropInventoryItems(World world, double x, double y, double z, IInventory inventory) {
        if (inventory == null) return;
        for (int i = 0; i < inventory.getSizeInventory(); i++) {
            ItemStack s = inventory.getStackInSlot(i);
            if (!s.isEmpty()) spawnItemStack(world, x, y, z, s);
        }
    }

    /** Splits the stack into random handfuls (10-30) thrown from a random point of the block; the stack ends up empty. */
    public static void spawnItemStack(World world, double x, double y, double z, ItemStack stack) {
        if (world == null || stack == null) return;
        var level = world.mcc$level();
        if (!(level instanceof net.minecraft.server.level.ServerLevel)) return;
        float f = RANDOM.nextFloat() * 0.8F + 0.1F;
        float f1 = RANDOM.nextFloat() * 0.8F + 0.1F;
        float f2 = RANDOM.nextFloat() * 0.8F + 0.1F;
        while (!stack.isEmpty()) {
            var part = org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122.toModern(stack.splitStack(RANDOM.nextInt(21) + 10));
            if (part.isEmpty()) continue;
            var entity = new net.minecraft.world.entity.item.ItemEntity(level, x + f, y + f1, z + f2, part);
            entity.setDeltaMovement(RANDOM.nextGaussian() * 0.05D, RANDOM.nextGaussian() * 0.05D + 0.2D, RANDOM.nextGaussian() * 0.05D);
            level.addFreshEntity(entity);
        }
    }
}
