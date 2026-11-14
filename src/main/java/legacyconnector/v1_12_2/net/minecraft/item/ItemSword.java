package legacyconnector.v1_12_2.net.minecraft.item;

public class ItemSword extends Item {
    private final float attackDamage;
    private final ToolMaterial material;

    public ItemSword(ToolMaterial material) {
        this.material = material;
        this.maxStackSize = 1;
        setMaxDamage(material.getMaxUses());
        this.attackDamage = 3.0F + material.getAttackDamage();
    }

    public float getAttackDamage() {
        return material.getAttackDamage();
    }

    public String getToolMaterialName() {
        return material.toString();
    }

    public int getItemEnchantability() {
        return material.getEnchantability();
    }

    /** 1.12's weapon/tool modifiers: attack damage and attack speed in the main hand. */
    @Override
    public com.google.common.collect.Multimap<String, legacyconnector.v1_12_2.net.minecraft.entity.ai.attributes.AttributeModifier> getItemAttributeModifiers(
            legacyconnector.v1_12_2.net.minecraft.inventory.EntityEquipmentSlot equipmentSlot) {
        com.google.common.collect.Multimap<String, legacyconnector.v1_12_2.net.minecraft.entity.ai.attributes.AttributeModifier> multimap = super.getItemAttributeModifiers(equipmentSlot);
        if (equipmentSlot == legacyconnector.v1_12_2.net.minecraft.inventory.EntityEquipmentSlot.MAINHAND) {
            multimap.put("generic.attackDamage", new legacyconnector.v1_12_2.net.minecraft.entity.ai.attributes.AttributeModifier(ATTACK_DAMAGE_MODIFIER, "Weapon modifier", (double) attackDamage, 0));
            multimap.put("generic.attackSpeed", new legacyconnector.v1_12_2.net.minecraft.entity.ai.attributes.AttributeModifier(ATTACK_SPEED_MODIFIER, "Weapon modifier", -2.4000000953674316, 0));
        }
        return multimap;
    }

    /** Hitting wears the item (1 points), as 1.12's did. */
    @Override
    public boolean hitEntity(ItemStack stack, legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase target,
            legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase attacker) {
        stack.damageItem(1, attacker);
        return true;
    }

    /** Breaking a block that takes time wears the item (2 points). */
    @Override
    public boolean onBlockDestroyed(ItemStack stack, legacyconnector.v1_12_2.net.minecraft.world.World world,
            legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos,
            legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase entity) {
        if ((double) state.getBlockHardness(world, pos) != 0.0D) stack.damageItem(2, entity);
        return true;
    }

    @Override
    public float getDestroySpeed(ItemStack stack, legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state) {
        if (state.getBlock() == legacyconnector.v1_12_2.net.minecraft.init.Blocks.WEB) return 15.0F;
        var m = state.getMaterial();
        return m != legacyconnector.v1_12_2.net.minecraft.block.material.Material.PLANTS && m != legacyconnector.v1_12_2.net.minecraft.block.material.Material.VINE && m != legacyconnector.v1_12_2.net.minecraft.block.material.Material.CORAL && m != legacyconnector.v1_12_2.net.minecraft.block.material.Material.LEAVES && m != legacyconnector.v1_12_2.net.minecraft.block.material.Material.GOURD ? 1.0F : 1.5F;
    }
}
