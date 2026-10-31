package legacyconnector.v1_7_10.net.minecraft.item;

import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.block.material.Material;
import legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** Vanilla 1.7.10 sword: 4 + material damage, fast on cobwebs and plants, blocks on right click. */
public class ItemSword extends Item {
    /** Not final: mods assign the damage from their own constructors (RotaryCraft's bedrock sword). */
    public float field_150934_a;
    public Item.ToolMaterial field_150933_b;

    public ItemSword(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
        this.field_150933_b = ItemTool.mcc$materialOf(info);
        this.field_150934_a = 4.0F + field_150933_b.getDamageVsEntity();
    }

    public ItemSword(Item.ToolMaterial material) {
        this.field_150933_b = material == null ? Item.ToolMaterial.WOOD : material;
        this.maxStackSize = 1;
        this.setMaxDamage(field_150933_b.getMaxUses());
        this.field_150934_a = 4.0F + field_150933_b.getDamageVsEntity();
    }

    public float func_150931_i() {
        return field_150933_b.getDamageVsEntity();
    }

    @Override
    public float getStrVsBlock(ItemStack stack, Block block) {
        if (block == legacyconnector.v1_7_10.net.minecraft.init.Blocks.web) return 15.0F;
        Material m = block.getMaterial();
        return m != Material.plants && m != Material.vine && m != Material.coral && m != Material.leaves && m != Material.gourd ? 1.0F : 1.5F;
    }

    @Override
    public boolean hitEntity(ItemStack stack, EntityLivingBase target, EntityLivingBase attacker) {
        stack.damageItem(1, attacker);
        return true;
    }

    @Override
    public boolean onBlockDestroyed(ItemStack stack, World world, Block block, int x, int y, int z, EntityLivingBase entity) {
        if (block.getBlockHardness(world, x, y, z) != 0.0D) stack.damageItem(2, entity);
        return true;
    }

    @Override
    public boolean isFull3D() {
        return true;
    }

    @Override
    public EnumAction getItemUseAction(ItemStack stack) {
        return EnumAction.block;
    }

    @Override
    public int getMaxItemUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        player.setItemInUse(stack, getMaxItemUseDuration(stack));
        return stack;
    }

    @Override
    public boolean canItemHarvestBlock(Block block) {
        return block == legacyconnector.v1_7_10.net.minecraft.init.Blocks.web;
    }

    @Override
    public int getItemEnchantability() {
        return field_150933_b.getEnchantability();
    }

    public String getToolMaterialName() {
        return field_150933_b.toString();
    }

    @Override
    public boolean getIsRepairable(ItemStack toRepair, ItemStack material) {
        Item repair = field_150933_b.getBaseItemForRepair();
        return repair != null && material != null && material.getItem() == repair || super.getIsRepairable(toRepair, material);
    }

    /** Vanilla 1.7.10 adds the weapon damage here; mods call super and add to it, or replace it (ProjectE). */
    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public com.google.common.collect.Multimap getItemAttributeModifiers() {
        com.google.common.collect.Multimap map = super.getItemAttributeModifiers();
        map.put(legacyconnector.v1_7_10.net.minecraft.entity.SharedMonsterAttributes.attackDamage.getAttributeUnlocalizedName(),
                new legacyconnector.v1_7_10.net.minecraft.entity.ai.attributes.AttributeModifier(field_111210_e, "Weapon modifier", field_150934_a, 0));
        return map;
    }

    @Override
    public float mcc$attackDamage() {
        return field_150934_a;
    }
}
