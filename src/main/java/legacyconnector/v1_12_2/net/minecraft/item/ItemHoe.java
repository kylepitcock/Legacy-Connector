package legacyconnector.v1_12_2.net.minecraft.item;

public class ItemHoe extends Item {
    protected final ToolMaterial theToolMaterial;

    public ItemHoe(ToolMaterial material) {
        this.theToolMaterial = material;
        this.maxStackSize = 1;
        setMaxDamage(material.getMaxUses());
    }

    public String getMaterialName() {
        return theToolMaterial.toString();
    }
}
