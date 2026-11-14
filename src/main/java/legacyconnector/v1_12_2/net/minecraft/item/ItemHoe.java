package legacyconnector.v1_12_2.net.minecraft.item;

public class ItemHoe extends Item {
    protected final ToolMaterial toolMaterial;

    public ItemHoe(ToolMaterial material) {
        this.toolMaterial = material;
        this.maxStackSize = 1;
        setMaxDamage(material.getMaxUses());
    }

    public String getMaterialName() {
        return toolMaterial.toString();
    }

    /** Tilling: the modern hoe's rules (grass, dirt and paths to farmland, coarse dirt to dirt), wearing this stack. */
    @Override
    public legacyconnector.v1_12_2.net.minecraft.util.EnumActionResult onItemUse(legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player,
            legacyconnector.v1_12_2.net.minecraft.world.World worldIn, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos,
            legacyconnector.v1_12_2.net.minecraft.util.EnumHand hand, legacyconnector.v1_12_2.net.minecraft.util.EnumFacing facing, float hitX, float hitY,
            float hitZ) {
        return mcc$useOnModern(net.minecraft.world.item.Items.IRON_HOE, player, worldIn, pos, hand, facing, hitX, hitY, hitZ);
    }

    /** Sets the tilled block with the hoe's sound and wears the hoe by one. */
    protected void setBlock(ItemStack stack, legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player,
            legacyconnector.v1_12_2.net.minecraft.world.World worldIn, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos,
            legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state) {
        worldIn.playSound(player, pos, legacyconnector.v1_12_2.net.minecraft.init.SoundEvents.ITEM_HOE_TILL,
                legacyconnector.v1_12_2.net.minecraft.util.SoundCategory.BLOCKS, 1.0F, 1.0F);
        if (!worldIn.isRemote) {
            worldIn.setBlockState(pos, state, 11);
            stack.damageItem(1, player);
        }
    }

    @Override
    public boolean hitEntity(ItemStack stack, legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase target,
            legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase attacker) {
        stack.damageItem(1, attacker);
        return true;
    }

    @Override
    public boolean isFull3D() {
        return true;
    }

    /** 1.12's hoe: no damage bonus, attack speed by material (wood -3.0 ... diamond 0.0). */
    @Override
    public com.google.common.collect.Multimap<String, legacyconnector.v1_12_2.net.minecraft.entity.ai.attributes.AttributeModifier> getItemAttributeModifiers(
            legacyconnector.v1_12_2.net.minecraft.inventory.EntityEquipmentSlot equipmentSlot) {
        com.google.common.collect.Multimap<String, legacyconnector.v1_12_2.net.minecraft.entity.ai.attributes.AttributeModifier> multimap = super.getItemAttributeModifiers(equipmentSlot);
        if (equipmentSlot == legacyconnector.v1_12_2.net.minecraft.inventory.EntityEquipmentSlot.MAINHAND) {
            multimap.put("generic.attackDamage", new legacyconnector.v1_12_2.net.minecraft.entity.ai.attributes.AttributeModifier(ATTACK_DAMAGE_MODIFIER, "Weapon modifier", 0.0D, 0));
            multimap.put("generic.attackSpeed", new legacyconnector.v1_12_2.net.minecraft.entity.ai.attributes.AttributeModifier(ATTACK_SPEED_MODIFIER, "Weapon modifier", (double) (toolMaterial.getAttackDamage() - 3.0F), 0));
        }
        return multimap;
    }
}
