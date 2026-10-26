package legacyconnector.v1_7_10.net.minecraft.block;

/** The anvil: a falling block whose top shows its damage (metadata bits 2-3); RenderBlocks draws its model (render type 35). */
public class BlockAnvil extends BlockFalling {
    public static final String[] anvilDamageNames = {"intact", "slightlyDamaged", "veryDamaged"};
    public static final String[] anvilIconNames = {"anvil_top_damaged_0", "anvil_top_damaged_1", "anvil_top_damaged_2"};
    /** Set to 3 by the renderer while it draws the top plate, so {@link #getIcon} answers the damaged top. */
    public int anvilRenderSide;
    public legacyconnector.v1_7_10.net.minecraft.util.IIcon[] anvilIcons;

    public BlockAnvil(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    protected BlockAnvil() {
        super(legacyconnector.v1_7_10.net.minecraft.block.material.Material.anvil);
        setLightOpacity(0);
        setCreativeTab(legacyconnector.v1_7_10.net.minecraft.creativetab.CreativeTabs.tabDecorations);
    }

    @Override
    public legacyconnector.v1_7_10.net.minecraft.util.IIcon getIcon(int side, int meta) {
        if (anvilRenderSide == 3 && side == 1) {
            int damage = (meta >> 2) % anvilIconNames.length;
            if (anvilIcons != null && anvilIcons[damage] != null) return anvilIcons[damage];
            return mcc$vanillaIcon(anvilIconNames[damage]);
        }
        return blockIcon != null ? blockIcon : mcc$vanillaIcon("anvil_base");
    }

    @Override
    public void registerBlockIcons(legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.IIconRegister register) {
        blockIcon = register.registerIcon("anvil_base");
        anvilIcons = new legacyconnector.v1_7_10.net.minecraft.util.IIcon[anvilIconNames.length];
        for (int i = 0; i < anvilIcons.length; i++) anvilIcons[i] = register.registerIcon(anvilIconNames[i]);
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    @Override
    public int getRenderType() {
        return 35;
    }

    @Override
    public int damageDropped(int meta) {
        return meta >> 2;
    }

    @Override
    public void setBlockBoundsBasedOnState(legacyconnector.v1_7_10.net.minecraft.world.IBlockAccess world, int x, int y, int z) {
        int dir = world.getBlockMetadata(x, y, z) & 3;
        if (dir != 3 && dir != 1) setBlockBounds(0.125F, 0.0F, 0.0F, 0.875F, 1.0F, 1.0F);
        else setBlockBounds(0.0F, 0.0F, 0.125F, 1.0F, 1.0F, 0.875F);
    }

    @Override
    public boolean shouldSideBeRendered(legacyconnector.v1_7_10.net.minecraft.world.IBlockAccess world, int x, int y, int z, int side) {
        return true;
    }
}
