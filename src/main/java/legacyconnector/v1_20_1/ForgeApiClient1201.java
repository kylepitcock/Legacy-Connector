package legacyconnector.v1_20_1;

import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;

/** Client-only helpers of {@link ForgeApi1201} (kept apart so the server never loads client classes). */
final class ForgeApiClient1201 {
    private ForgeApiClient1201() {
    }

    static HolderLookup.Provider registries() {
        Minecraft mc = Minecraft.getInstance();
        return mc != null && mc.level != null ? mc.level.registryAccess() : null;
    }

    static net.minecraft.client.RecipeBookCategories categoriesCreate(String name, net.minecraft.world.item.ItemStack... icons) {
        return org.kylepitcock.legacyconnector.core.LegacyEnums.add(net.minecraft.client.RecipeBookCategories.class, name,
                new Class<?>[] {net.minecraft.world.item.ItemStack[].class}, new Object[] {icons});
    }
}
