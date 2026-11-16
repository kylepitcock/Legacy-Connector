package legacyconnector.v1_6_4.net.minecraft.src.forge;

/** Forge 3.x's item renderer hook; {@link MinecraftForgeClient#registerItemRenderer} adapts it to the shim's item renderer registry. */
public interface IItemRenderer {
    boolean handleRenderType(legacyconnector.v1_7_10.net.minecraft.item.ItemStack item, ItemRenderType type);

    boolean shouldUseRenderHelper(ItemRenderType type, legacyconnector.v1_7_10.net.minecraft.item.ItemStack item, ItemRendererHelper helper);

    void renderItem(ItemRenderType type, legacyconnector.v1_7_10.net.minecraft.item.ItemStack item, Object... data);

    enum ItemRenderType {
        ENTITY, EQUIPPED, INVENTORY, FIRST_PERSON_MAP
    }

    enum ItemRendererHelper {
        ENTITY_ROTATION, ENTITY_BOBBING, EQUIPPED_BLOCK, BLOCK_3D, INVENTORY_BLOCK
    }
}
