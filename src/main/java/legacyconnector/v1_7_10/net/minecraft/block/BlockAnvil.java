package legacyconnector.v1_7_10.net.minecraft.block;

import java.util.List;
import legacyconnector.v1_7_10.net.minecraft.block.material.Material;
import legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.util.IIcon;
import legacyconnector.v1_7_10.net.minecraft.world.IBlockAccess;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/**
 * The anvil: metadata bits 0-1 are the facing, bits 2-3 the damage (intact, slightly damaged, very damaged). A mod's anvil that
 * does not open its own screen opens the modern anvil screen.
 */
public class BlockAnvil extends BlockFalling {
    public static final String[] anvilDamageNames = {"intact", "slightlyDamaged", "veryDamaged"};
    private static final String[] anvilIconNames = {"anvil_top_damaged_0", "anvil_top_damaged_1", "anvil_top_damaged_2"};
    /** Set by the anvil renderer while it draws the top (side 1 then shows the damage texture). */
    public int anvilRenderSide;
    private IIcon[] anvilIcons;

    public BlockAnvil(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    protected BlockAnvil() {
        super(Material.anvil);
        setLightOpacity(0);
        setCreativeTab(legacyconnector.v1_7_10.net.minecraft.creativetab.CreativeTabs.tabDecorations);
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public IIcon getIcon(int side, int meta) {
        if (anvilRenderSide == 3 && side == 1) {
            int damage = (meta >> 2) % anvilIconNames.length;
            if (anvilIcons != null && anvilIcons[damage] != null) return anvilIcons[damage];
            return mcc$vanillaIcon(anvilIconNames[damage]);
        }
        if (blockIcon == null) return mcc$vanillaIcon("anvil_base");
        return blockIcon;
    }

    @Override
    public void registerBlockIcons(legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.IIconRegister register) {
        blockIcon = register.registerIcon("anvil_base");
        anvilIcons = new IIcon[anvilIconNames.length];
        for (int i = 0; i < anvilIcons.length; ++i) anvilIcons[i] = register.registerIcon(getTextureName() + "_" + anvilIconNames[i]);
    }

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase placer, ItemStack stack) {
        if (mcc$vanilla != null || placer == null) return;
        int facing = legacyconnector.v1_7_10.net.minecraft.util.MathHelper.floor_double(legacyconnector.v1_7_10.net.minecraft.entity.Entity.mcc$get$rotationYaw(placer) * 4.0F / 360.0F + 0.5D) & 3;
        int damage = world.getBlockMetadata(x, y, z) >> 2;
        int[] byFacing = {2, 3, 0, 1};
        world.setBlockMetadataWithNotify(x, y, z, byFacing[facing] | damage << 2, 2);
    }

    /** Opens the anvil screen (the modern anvil menu, valid while this block stands there). */
    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
        if (world.isRemote) return true;
        if (player != null && player.mcc$entity instanceof net.minecraft.server.level.ServerPlayer sp && world.mcc$liveLevel() != null) {
            var level = world.mcc$liveLevel();
            var pos = new net.minecraft.core.BlockPos(x, y, z);
            var modern = level.getBlockState(pos).getBlock();
            sp.openMenu(new net.minecraft.world.SimpleMenuProvider((id, inventory, p) -> new net.minecraft.world.inventory.AnvilMenu(id, inventory,
                    net.minecraft.world.inventory.ContainerLevelAccess.create(level, pos)) {
                @Override
                protected boolean isValidBlock(net.minecraft.world.level.block.state.BlockState state) {
                    return state.getBlock() == modern;
                }
            }, net.minecraft.network.chat.Component.translatable("container.repair")));
        }
        return true;
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
    public void setBlockBoundsBasedOnState(IBlockAccess world, int x, int y, int z) {
        int facing = world.getBlockMetadata(x, y, z) & 3;
        if (facing != 3 && facing != 1) setBlockBounds(0.125F, 0.0F, 0.0F, 0.875F, 1.0F, 1.0F);
        else setBlockBounds(0.0F, 0.0F, 0.125F, 1.0F, 1.0F, 0.875F);
    }

    @Override
    public void getSubBlocks(legacyconnector.v1_7_10.net.minecraft.item.Item item, legacyconnector.v1_7_10.net.minecraft.creativetab.CreativeTabs tab, List list) {
        list.add(new ItemStack(item, 1, 0));
        list.add(new ItemStack(item, 1, 1));
        list.add(new ItemStack(item, 1, 2));
    }

    @Override
    protected void onStartFalling(legacyconnector.v1_7_10.net.minecraft.entity.item.EntityFallingBlock entity) {
    }

    @Override
    public void playSoundWhenFallen(World world, int x, int y, int z, int meta) {
        world.playSoundEffect(x + 0.5D, y + 0.5D, z + 0.5D, "random.anvil_land", 0.3F, world.rand.nextFloat() * 0.1F + 0.9F);
    }

    @Override
    public boolean shouldSideBeRendered(IBlockAccess world, int x, int y, int z, int side) {
        return true;
    }
}
