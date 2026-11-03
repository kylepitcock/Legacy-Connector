package legacyconnector.v1_7_10.net.minecraft.item;

/** Mob heads: skeleton, wither skeleton, zombie, player (profile in the stack tag), creeper. */
public class ItemSkull extends Item {
    private static final String[] skullTypes = {"skeleton", "wither", "zombie", "char", "creeper"};

    public ItemSkull() {
        setMaxDamage(0);
        setHasSubtypes(true);
    }

    @Override
    public int getMetadata(int damage) {
        return damage;
    }

    @Override
    public String getUnlocalizedName(ItemStack stack) {
        int meta = stack.getItemDamage();
        if (meta < 0 || meta >= skullTypes.length) meta = 0;
        return super.getUnlocalizedName() + "." + skullTypes[meta];
    }
}
