package legacyconnector.v1_12_2.net.minecraft.item;

import java.util.Set;
import legacyconnector.v1_12_2.net.minecraft.block.Block;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;

/**
 * Base of pickaxes, axes and shovels: a tool material and the blocks it is effective on. As in Forge 1.12 a pickaxe, axe or shovel
 * declares its tool class with the material's harvest level; whether a block is mined by a tool class comes from the block's Forge
 * harvest tool (legacy blocks) or the modern {@code mineable} tags (vanilla and modern blocks), the level a block needs from its
 * harvest level or the modern {@code needs_*_tool} tags. Tags are only read while mining, never while mods load.
 */
public class ItemTool extends Item {
    protected final Set<Block> effectiveBlocks;
    protected float efficiency;
    protected float attackDamage;
    protected float attackSpeed;
    protected final ToolMaterial toolMaterial;

    protected ItemTool(ToolMaterial material, Set<Block> effectiveBlocks) {
        this(0.0F, 0.0F, material, effectiveBlocks);
    }

    protected ItemTool(float attackDamage, float attackSpeed, ToolMaterial material, Set<Block> effectiveBlocks) {
        this.toolMaterial = material;
        this.effectiveBlocks = effectiveBlocks;
        this.maxStackSize = 1;
        setMaxDamage(material.getMaxUses());
        this.efficiency = material.getEfficiency();
        this.attackDamage = attackDamage + material.getAttackDamage();
        this.attackSpeed = attackSpeed;
        String toolClass = this instanceof ItemPickaxe ? "pickaxe" : this instanceof ItemAxe ? "axe" : this instanceof ItemSpade ? "shovel" : null;
        if (toolClass != null && material != null) setHarvestLevel(toolClass, material.getHarvestLevel());
    }

    /** Forge: full efficiency on blocks one of the tool's classes is effective on, or on the tool's own effective blocks. */
    @Override
    public float getDestroySpeed(ItemStack stack, IBlockState state) {
        try {
            for (String type : getToolClasses(stack)) {
                if (mcc$effective(type, state)) return efficiency;
            }
            return effectiveBlocks != null && state != null && effectiveBlocks.contains(state.getBlock()) ? efficiency : 1.0F;
        } catch (RuntimeException | LinkageError e) {
            return 1.0F;
        }
    }

    /** The modern block tag of the blocks a Forge tool class mines, or null for other classes. */
    public static TagKey<net.minecraft.world.level.block.Block> mcc$mineable(String toolClass) {
        return switch (toolClass == null ? "" : toolClass) {
            case "pickaxe" -> BlockTags.MINEABLE_WITH_PICKAXE;
            case "axe" -> BlockTags.MINEABLE_WITH_AXE;
            case "shovel" -> BlockTags.MINEABLE_WITH_SHOVEL;
            case "hoe" -> BlockTags.MINEABLE_WITH_HOE;
            default -> null;
        };
    }

    /** The 1.12 harvest level (wood 0, stone 1, iron 2, diamond 3) a modern block needs from its tier tags. */
    public static int mcc$requiredLevel(net.minecraft.world.level.block.state.BlockState state) {
        if (state == null) return 0;
        if (state.is(BlockTags.NEEDS_DIAMOND_TOOL)) return 3;
        if (state.is(BlockTags.NEEDS_IRON_TOOL)) return 2;
        if (state.is(BlockTags.NEEDS_STONE_TOOL)) return 1;
        return 0;
    }

    /** True if a tool of this class mines the block: the block's Forge harvest tool, else the modern mineable tag. */
    public static boolean mcc$effective(String toolClass, IBlockState state) {
        if (toolClass == null || state == null || state.getBlock() == null) return false;
        Block block = state.getBlock();
        if (block.getHarvestTool(state) != null) return block.isToolEffective(toolClass, state);
        TagKey<net.minecraft.world.level.block.Block> tag = mcc$mineable(toolClass);
        return tag != null && org.kylepitcock.legacyconnector.v1_12_2.bridge.StatesBridge1122.toModern(state).is(tag);
    }

    /** The harvest level the block needs: its Forge harvest level when it names a harvest tool, else the modern tier tags. */
    public static int mcc$requiredLevel(IBlockState state) {
        Block block = state.getBlock();
        if (block.getHarvestTool(state) != null) return Math.max(0, block.getHarvestLevel(state));
        return mcc$requiredLevel(org.kylepitcock.legacyconnector.v1_12_2.bridge.StatesBridge1122.toModern(state));
    }

    /** Forge 1.12's rule (ForgeHooks.canHarvestBlock): a tool class that mines the block, at a harvest level at least the block's. */
    public static boolean mcc$canHarvest(Item item, ItemStack stack, IBlockState state) {
        if (item == null || state == null || state.getBlock() == null) return false;
        java.util.Set<String> classes = item.getToolClasses(stack);
        if (classes == null || classes.isEmpty()) return false;
        int needed = mcc$requiredLevel(state);
        for (String type : classes) {
            if (mcc$effective(type, state) && item.getHarvestLevel(stack, type, null, state) >= needed) return true;
        }
        return false;
    }

    public ToolMaterial mcc$toolMaterial() {
        return toolMaterial;
    }

    public String getToolMaterialName() {
        return toolMaterial.toString();
    }

    public int getItemEnchantability() {
        return toolMaterial.getEnchantability();
    }
}
