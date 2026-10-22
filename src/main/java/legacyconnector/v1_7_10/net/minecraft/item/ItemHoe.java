package legacyconnector.v1_7_10.net.minecraft.item;

/** Vanilla 1.7.10 hoe. Tilling is done by the modern hoe behaviour of the item's tool class. */
public class ItemHoe extends Item {
    protected Item.ToolMaterial theToolMaterial;

    public ItemHoe(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
        this.theToolMaterial = ItemTool.mcc$materialOf(info);
    }

    public ItemHoe(Item.ToolMaterial material) {
        this.theToolMaterial = material == null ? Item.ToolMaterial.WOOD : material;
        this.maxStackSize = 1;
        this.setMaxDamage(theToolMaterial.getMaxUses());
    }

    @Override
    public boolean isFull3D() {
        return true;
    }

    public String getToolMaterialName() {
        return theToolMaterial.toString();
    }
}
