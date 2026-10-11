package mccompat.v1_7_10.net.minecraft.item;

import java.util.Collections;
import java.util.Set;
import mccompat.v1_7_10.net.minecraft.block.Block;
import mccompat.v1_7_10.net.minecraft.entity.EntityLivingBase;
import mccompat.v1_7_10.net.minecraft.world.World;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;

/**
 * Vanilla/Forge 1.7.10 tool base. Effectiveness and harvest levels use the modern block tags
 * ({@code mineable/pickaxe}, {@code needs_iron_tool}, ...) plus legacy Forge harvest tools, so a legacy pickaxe works
 * on vanilla and legacy blocks alike.
 */
public class ItemTool extends Item {
    protected float efficiencyOnProperMaterial = 4.0F;
    private final float damageVsEntity;
    protected Item.ToolMaterial toolMaterial;
    private final Set<Block> field_150914_c;
    /** Forge: the tool class ("pickaxe", "axe", "shovel") this tool mines with, if any. */
    private String toolClass;

    @SuppressWarnings({"rawtypes", "unchecked"})
    /** Vanilla tool wrapper: the material follows from the vanilla name (wooden_/stone_/iron_/golden_/diamond_). */
    protected ItemTool(org.kylepitcock.mccompat.v1_7_10.bridge.VanillaInfo info, float damage) {
        super(info);
        this.toolMaterial = mcc$materialOf(info);
        this.efficiencyOnProperMaterial = toolMaterial.getEfficiencyOnProperMaterial();
        this.damageVsEntity = damage + toolMaterial.getDamageVsEntity();
        this.field_150914_c = java.util.Collections.emptySet();
    }

    public static Item.ToolMaterial mcc$materialOf(org.kylepitcock.mccompat.v1_7_10.bridge.VanillaInfo info) {
        String n = info.legacyName();
        if (n.contains("diamond")) return Item.ToolMaterial.EMERALD;
        if (n.contains("golden")) return Item.ToolMaterial.GOLD;
        if (n.contains("iron")) return Item.ToolMaterial.IRON;
        if (n.contains("stone")) return Item.ToolMaterial.STONE;
        return Item.ToolMaterial.WOOD;
    }

    protected ItemTool(float damage, Item.ToolMaterial material, Set effectiveBlocks) {
        this.toolMaterial = material == null ? Item.ToolMaterial.WOOD : material;
        this.field_150914_c = effectiveBlocks == null ? Collections.emptySet() : effectiveBlocks;
        this.maxStackSize = 1;
        this.setMaxDamage(toolMaterial.getMaxUses());
        this.efficiencyOnProperMaterial = toolMaterial.getEfficiencyOnProperMaterial();
        this.damageVsEntity = damage + toolMaterial.getDamageVsEntity();
        if (this instanceof ItemPickaxe) toolClass = "pickaxe";
        else if (this instanceof ItemAxe) toolClass = "axe";
        else if (this instanceof ItemSpade) toolClass = "shovel";
    }

    static TagKey<net.minecraft.world.level.block.Block> mineable(String toolClass) {
        return switch (toolClass == null ? "" : toolClass) {
            case "pickaxe" -> BlockTags.MINEABLE_WITH_PICKAXE;
            case "axe" -> BlockTags.MINEABLE_WITH_AXE;
            case "shovel" -> BlockTags.MINEABLE_WITH_SHOVEL;
            case "hoe" -> BlockTags.MINEABLE_WITH_HOE;
            default -> null;
        };
    }

    /** Harvest level a block needs from its modern tier tags (0 = any tool of the right kind). */
    static int requiredLevel(Block block) {
        if (block == null || block.mcc$modern == null) return 0;
        var state = block.mcc$modern.defaultBlockState();
        if (state.is(BlockTags.NEEDS_DIAMOND_TOOL)) return 3;
        if (state.is(BlockTags.NEEDS_IRON_TOOL)) return 2;
        if (state.is(BlockTags.NEEDS_STONE_TOOL)) return 1;
        return 0;
    }

    /** True if blocks of this kind are mined by this tool class (Forge harvest tool, or the modern mineable tag). */
    static boolean effectiveFor(String toolClass, Block block, int meta) {
        if (block == null || toolClass == null) return false;
        if (block.isToolEffective(toolClass, meta)) return true;
        TagKey<net.minecraft.world.level.block.Block> tag = mineable(toolClass);
        return tag != null && block.mcc$modern != null && block.mcc$modern.defaultBlockState().is(tag);
    }

    @Override
    public float getStrVsBlock(ItemStack stack, Block block) {
        return field_150914_c.contains(block) || effectiveFor(toolClass, block, 0) ? efficiencyOnProperMaterial : 1.0F;
    }

    @Override
    public float getDigSpeed(ItemStack stack, Block block, int meta) {
        return effectiveFor(toolClass, block, meta) || field_150914_c.contains(block) ? efficiencyOnProperMaterial : super.getDigSpeed(stack, block, meta);
    }

    @Override
    public boolean canHarvestBlock(Block block, ItemStack stack) {
        if (toolClass == null || !effectiveFor(toolClass, block, 0)) return super.canHarvestBlock(block, stack);
        int needed = block.getHarvestTool(0) != null ? block.getHarvestLevel(0) : requiredLevel(block);
        return toolMaterial.getHarvestLevel() >= needed;
    }

    @Override
    public boolean hitEntity(ItemStack stack, EntityLivingBase target, EntityLivingBase attacker) {
        stack.damageItem(2, attacker);
        return true;
    }

    @Override
    public boolean onBlockDestroyed(ItemStack stack, World world, Block block, int x, int y, int z, EntityLivingBase entity) {
        if (block.getBlockHardness(world, x, y, z) != 0.0D) stack.damageItem(1, entity);
        return true;
    }

    @Override
    public boolean isFull3D() {
        return true;
    }

    public Item.ToolMaterial func_150913_i() {
        return toolMaterial;
    }

    @Override
    public int getItemEnchantability() {
        return toolMaterial.getEnchantability();
    }

    public String getToolMaterialName() {
        return toolMaterial.toString();
    }

    @Override
    public boolean getIsRepairable(ItemStack toRepair, ItemStack material) {
        Item repair = toolMaterial.getBaseItemForRepair();
        return repair != null && material != null && material.getItem() == repair || super.getIsRepairable(toRepair, material);
    }

    @Override
    public int getHarvestLevel(ItemStack stack, String toolClass) {
        return toolClass != null && toolClass.equals(this.toolClass) ? toolMaterial.getHarvestLevel() : super.getHarvestLevel(stack, toolClass);
    }

    @Override
    public Set<String> getToolClasses(ItemStack stack) {
        return toolClass != null ? Set.of(toolClass) : super.getToolClasses(stack);
    }

    @Override
    public void setHarvestLevel(String toolClass, int level) {
        super.setHarvestLevel(toolClass, level);
        this.toolClass = toolClass;
    }

    @Override
    public float mcc$attackDamage() {
        return damageVsEntity;
    }
}
