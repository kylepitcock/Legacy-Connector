package org.kylepitcock.legacyconnector.v1_12_2.bridge;

import legacyconnector.v1_12_2.net.minecraft.block.Block;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import org.kylepitcock.legacyconnector.core.synth.Synth;

/**
 * Members of 1.12.2 vanilla block and item classes the shim only synthesizes (BlockFire, BlockFurnace, BlockCauldron, ItemMonsterPlacer,
 * ItemBanner, BlockStone.EnumType), answered with their real behaviour on the modern game.
 */
public final class SynthBlocks1122 {
    private SynthBlocks1122() {}

    public static void register() {
        // Blocks.FIRE.setFireInfo(block, encouragement, flammability): the modern fire's table for the block's modern block
        Synth.addInstanceMethod("net.minecraft.block.BlockFire.setFireInfo", (self, args) -> {
            if (args.length != 3 || !(args[0] instanceof Block b)) return Synth.NONE;
            if (b.mcc$modern instanceof net.minecraft.world.level.block.Block modern) {
                ((net.minecraft.world.level.block.FireBlock) net.minecraft.world.level.block.Blocks.FIRE).setFlammable(modern, Synth.toInt(args[1]), Synth.toInt(args[2]));
            }
            return null;
        });
        // BlockFurnace.setState(active, world, pos): lights or puts out the furnace there (the modern furnace's LIT property)
        Synth.addStaticMethod("net.minecraft.block.BlockFurnace.setState", args -> {
            if (args.length != 3 || !(args[1] instanceof World w) || !(args[2] instanceof BlockPos p) || w.mcc$level() == null) return Synth.NONE;
            var level = w.mcc$level();
            var mp = Inventories1122.pos(p);
            var state = level.getBlockState(mp);
            var lit = net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT;
            if (state.hasProperty(lit)) level.setBlock(mp, state.setValue(lit, Synth.toInt(args[0]) != 0 || Boolean.TRUE.equals(args[0])), 3);
            return null;
        });
        // Blocks.CAULDRON.setWaterLevel(world, pos, state, level): the modern water cauldron at that level (0 empties it)
        Synth.addInstanceMethod("net.minecraft.block.BlockCauldron.setWaterLevel", (self, args) -> {
            if (args.length != 4 || !(args[0] instanceof World w) || !(args[1] instanceof BlockPos p) || w.mcc$level() == null) return Synth.NONE;
            int lvl = Math.max(0, Math.min(3, Synth.toInt(args[3])));
            var mp = Inventories1122.pos(p);
            var state = lvl == 0 ? net.minecraft.world.level.block.Blocks.CAULDRON.defaultBlockState()
                    : net.minecraft.world.level.block.Blocks.WATER_CAULDRON.defaultBlockState()
                            .setValue(net.minecraft.world.level.block.LayeredCauldronBlock.LEVEL, lvl);
            w.mcc$level().setBlock(mp, state, 2);
            return null;
        });
        // spawn eggs: the entity id in the EntityTag compound
        Synth.addStaticMethod("net.minecraft.item.ItemMonsterPlacer.applyEntityIdToItemStack", args -> {
            if (args.length != 2 || !(args[0] instanceof ItemStack s) || !(args[1] instanceof ResourceLocation id)) return Synth.NONE;
            NBTTagCompound tag = s.hasTagCompound() ? s.getTagCompound() : new NBTTagCompound();
            NBTTagCompound entity = new NBTTagCompound();
            entity.setString("id", id.toString());
            tag.setTag("EntityTag", entity);
            s.setTagCompound(tag);
            return null;
        });
        Synth.addStaticMethod("net.minecraft.item.ItemMonsterPlacer.getNamedIdFrom", args -> {
            if (args.length != 1 || !(args[0] instanceof ItemStack s)) return Synth.NONE;
            NBTTagCompound tag = s.getTagCompound();
            if (tag == null || !tag.hasKey("EntityTag", 10)) return null;
            String id = tag.getCompoundTag("EntityTag").getString("id");
            return id.isEmpty() ? null : new ResourceLocation(id);
        });
        // ItemBanner.makeBanner(color, patterns): a banner stack with its base colour and patterns in BlockEntityTag
        Synth.addStaticMethod("net.minecraft.item.ItemBanner.makeBanner", args -> {
            if (args.length != 2 || !(args[0] instanceof legacyconnector.v1_12_2.net.minecraft.item.EnumDyeColor color)) return Synth.NONE;
            ItemStack stack = new ItemStack(legacyconnector.v1_12_2.net.minecraft.init.Items.BANNER, 1, color.getDyeDamage());
            if (args[1] instanceof legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagList patterns && !patterns.isEmpty()) {
                NBTTagCompound be = stack.getOrCreateSubCompound("BlockEntityTag");
                be.setTag("Patterns", patterns.copy());
            }
            return stack;
        });
        // Forge's canCreatureSpawn(state, world, pos, placementType): a mob may spawn on a block whose top side is solid
        Synth.InstanceMethod creatureSpawn = (self, args) -> {
            if (args.length != 4 || !(args[0] instanceof legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state)
                    || !(args[1] instanceof legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess world) || !(args[2] instanceof BlockPos pos)) {
                return Synth.NONE;
            }
            return state.isSideSolid(world, pos, legacyconnector.v1_12_2.net.minecraft.util.EnumFacing.UP);
        };
        for (String owner : new String[] {"net.minecraft.block.Block", "net.minecraft.block.BlockContainer", "net.minecraftforge.fluids.BlockFluidClassic",
                "net.minecraftforge.fluids.BlockFluidBase", "net.minecraftforge.fluids.BlockFluidFinite"}) {
            Synth.addInstanceMethod(owner + ".canCreatureSpawn", creatureSpawn);
        }
        // Forge's Item.getHorseArmorType(stack): vanilla's horse armors by item (mod armor overrides it)
        Synth.addInstanceMethod("net.minecraft.item.Item.getHorseArmorType", (self, args) -> args.length != 1 ? Synth.NONE
                : Synth.callStatic("net.minecraft.entity.passive.HorseArmorType.getByItemStack", args));
        // ItemBanner.getBaseColor(stack): the banner item's colour
        Synth.addStaticMethod("net.minecraft.item.ItemBanner.getBaseColor", args -> args.length == 1 && args[0] instanceof ItemStack s
                ? legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntityBanner.getColor(s) : Synth.NONE);
        // ItemBanner.appendHoverTextFromTileEntityTag(stack, tooltip): one line per pattern (at most six), "item.banner.<pattern>.<colour>"
        Synth.addStaticMethod("net.minecraft.item.ItemBanner.appendHoverTextFromTileEntityTag", args -> {
            if (args.length != 2 || !(args[0] instanceof ItemStack s) || !(args[1] instanceof java.util.List<?> raw)) return Synth.NONE;
            @SuppressWarnings("unchecked")
            java.util.List<String> tooltip = (java.util.List<String>) raw;
            NBTTagCompound be = s.getSubCompound("BlockEntityTag");
            if (be == null || !be.hasKey("Patterns")) return null;
            var patterns = be.getTagList("Patterns", 10);
            for (int i = 0; i < patterns.tagCount() && i < 6; ++i) {
                NBTTagCompound p = patterns.getCompoundTagAt(i);
                var color = legacyconnector.v1_12_2.net.minecraft.item.EnumDyeColor.byDyeDamage(p.getInteger("Color"));
                var pattern = legacyconnector.v1_12_2.net.minecraft.tileentity.BannerPattern.byHash(p.getString("Pattern"));
                if (pattern != null) {
                    tooltip.add(legacyconnector.v1_12_2.net.minecraft.util.text.translation.I18n.translateToLocal(
                            "item.banner." + pattern.getFileName() + "." + color.getTranslationKey()));
                }
            }
            return null;
        });
        // BlockStone.EnumType.isNatural: granite, diorite and andesite are, their polished forms are not
        Synth.addInstanceMethod("net.minecraft.block.BlockStone$EnumType.isNatural", (self, args) -> {
            if (!(self instanceof Enum<?> e)) return Synth.NONE;
            return !e.name().endsWith("_SMOOTH");
        });
    }
}
