package legacyconnector.v1_6_4.net.minecraft.src.forge;

/** Tag comparison for {@link DungeonLoot}. */
final class ItemStackTags {
    private ItemStackTags() {}

    static boolean same(legacyconnector.v1_7_10.net.minecraft.item.ItemStack a, legacyconnector.v1_7_10.net.minecraft.item.ItemStack b) {
        return java.util.Objects.equals(a.getTagCompound(), b.getTagCompound());
    }
}
