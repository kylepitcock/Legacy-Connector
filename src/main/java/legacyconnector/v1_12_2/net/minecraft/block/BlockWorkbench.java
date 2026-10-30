package legacyconnector.v1_12_2.net.minecraft.block;

import legacyconnector.v1_12_2.net.minecraft.block.material.Material;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.EnumHand;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2 crafting tables (mods' workbenches extend it): right-click opens the 3x3 crafting screen, valid while the player stays near. */
public class BlockWorkbench extends Block {
    protected BlockWorkbench() {
        super(Material.WOOD);
        setCreativeTab(legacyconnector.v1_12_2.net.minecraft.creativetab.CreativeTabs.DECORATIONS);
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX,
            float hitY, float hitZ) {
        if (worldIn.isRemote) return true;
        var player = playerIn.mcc$player();
        var level = worldIn.mcc$level();
        if (player == null || level == null) return true;
        var mp = org.kylepitcock.legacyconnector.v1_12_2.bridge.Inventories1122.pos(pos);
        var table = level.getBlockState(mp).getBlock();
        player.openMenu(new net.minecraft.world.SimpleMenuProvider((id, inventory, p) -> new net.minecraft.world.inventory.CraftingMenu(id, inventory,
                net.minecraft.world.inventory.ContainerLevelAccess.create(level, mp)) {
            /** Vanilla's check wants a vanilla crafting table; this one is the mod's block. */
            @Override
            public boolean stillValid(net.minecraft.world.entity.player.Player who) {
                return level.getBlockState(mp).is(table) && who.distanceToSqr(mp.getX() + 0.5D, mp.getY() + 0.5D, mp.getZ() + 0.5D) <= 64.0D;
            }
        }, net.minecraft.network.chat.Component.translatable("container.crafting")));
        return true;
    }

    /** The crafting table as an interaction object (EntityPlayer.displayGui opens it): a 3x3 crafting container at a position. */
    public static class InterfaceCraftingTable implements legacyconnector.v1_12_2.net.minecraft.world.IInteractionObject {
        private final World world;
        private final BlockPos position;

        public InterfaceCraftingTable(World worldIn, BlockPos pos) {
            this.world = worldIn;
            this.position = pos;
        }

        @Override
        public String getName() {
            return "crafting_table";
        }

        @Override
        public boolean hasCustomName() {
            return false;
        }

        @Override
        public legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent getDisplayName() {
            return new legacyconnector.v1_12_2.net.minecraft.util.text.TextComponentTranslation(
                    legacyconnector.v1_12_2.net.minecraft.init.Blocks.CRAFTING_TABLE.getTranslationKey() + ".name");
        }

        @Override
        public legacyconnector.v1_12_2.net.minecraft.inventory.Container createContainer(legacyconnector.v1_12_2.net.minecraft.entity.player.InventoryPlayer playerInventory,
                EntityPlayer playerIn) {
            return new legacyconnector.v1_12_2.net.minecraft.inventory.ContainerWorkbench(playerInventory, world, position);
        }

        @Override
        public String getGuiID() {
            return "minecraft:crafting_table";
        }
    }
}
