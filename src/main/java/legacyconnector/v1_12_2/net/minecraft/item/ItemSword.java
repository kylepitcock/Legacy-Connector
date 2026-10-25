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
}
