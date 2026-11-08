package legacyconnector.v1_12_2.net.minecraftforge.event.enchanting;

import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event;

/** The enchantment table works out the level shown for one of its three rows: handlers may change it. */
public class EnchantmentLevelSetEvent extends Event {
    private final World world;
    private final BlockPos pos;
    private final int enchantRow;
    private final int power;
    private final ItemStack itemStack;
    private final int originalLevel;
    private int level;

    public EnchantmentLevelSetEvent(World world, BlockPos pos, int enchRow, int power, ItemStack itemStack, int level) {
        this.world = world;
        this.pos = pos;
        this.enchantRow = enchRow;
        this.power = power;
        this.itemStack = itemStack;
        this.originalLevel = level;
        this.level = level;
    }

    public World getWorld() {
        return world;
    }

    public BlockPos getPos() {
        return pos;
    }

    public int getEnchantRow() {
        return enchantRow;
    }

    public int getPower() {
        return power;
    }

    public ItemStack getItem() {
        return itemStack;
    }

    public int getOriginalLevel() {
        return originalLevel;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }
}
