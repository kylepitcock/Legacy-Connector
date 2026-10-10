package com.example.legacytest;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.oredict.OreDictionary;
import org.apache.logging.log4j.Logger;

/** Runs in a live server and reports what works, the way a mod would exercise the API in practice. */
public class SelfTest {
    private static final List<String> results = new ArrayList<String>();
    private static int passed;
    private static int failed;

    static void check(String name, boolean ok, Object detail) {
        if (ok) passed++;
        else failed++;
        results.add((ok ? "PASS " : "FAIL ") + name + " (" + detail + ")");
    }

    static String summary() {
        return passed + " passed, " + failed + " failed";
    }

    /** Optional cross-mod check: exercises Iron Chest through the legacy API when it is installed. */
    static void ironChest(World world) {
        Block iron = Block.getBlockFromName("IronChest:BlockIronChest");
        if (iron == null) return;
        int x = 2, y = 200, z = 2;
        world.setBlock(x, y, z, iron, 1, 3); // 1 = gold chest
        Object te = world.getTileEntity(x, y, z);
        check("iron chest tile entity", te instanceof net.minecraft.inventory.IInventory, te);
        if (te instanceof net.minecraft.inventory.IInventory) {
            net.minecraft.inventory.IInventory inv = (net.minecraft.inventory.IInventory) te;
            check("gold chest size", inv.getSizeInventory() == 81, inv.getSizeInventory());
            inv.setInventorySlotContents(0, new ItemStack(Items.diamond, 5));
            check("iron chest stores items", inv.getStackInSlot(0) != null && inv.getStackInSlot(0).stackSize == 5, inv.getStackInSlot(0));
            NBTTagCompound tag = new NBTTagCompound();
            ((net.minecraft.tileentity.TileEntity) te).writeToNBT(tag);
            check("iron chest NBT", tag.getTagList("Items", 10).tagCount() == 1, tag);
            check("iron chest description packet", ((net.minecraft.tileentity.TileEntity) te).getDescriptionPacket() != null,
                    ((net.minecraft.tileentity.TileEntity) te).getDescriptionPacket());
        }
        world.setBlockToAir(x, y, z);
    }

    public static void run(Logger log) {
        try {
            check("block registry lookup", Block.getBlockFromName("LegacyTest:ore") == LegacyTestMod.ore, Block.getBlockFromName("LegacyTest:ore"));
            check("item from block", Item.getItemFromBlock(LegacyTestMod.ore) instanceof ItemBlockOre, Item.getItemFromBlock(LegacyTestMod.ore));
            ItemStack tin = new ItemStack(LegacyTestMod.ingot, 5, 1);
            check("subtype metadata survives", tin.getItemDamage() == 1 && tin.stackSize == 5, tin);
            tin.stackSize -= 2;
            check("stackSize write-through", tin.stackSize == 3, tin.stackSize);
            NBTTagCompound tag = new NBTTagCompound();
            tag.setString("owner", "legacy");
            tin.setTagCompound(tag);
            tin.getTagCompound().setInteger("level", 7);
            check("item NBT write-through", tin.getTagCompound().getInteger("level") == 7 && "legacy".equals(tin.getTagCompound().getString("owner")), tin.getTagCompound());
            NBTTagCompound saved = tin.writeToNBT(new NBTTagCompound());
            ItemStack loaded = ItemStack.loadItemStackFromNBT(saved);
            check("item NBT round trip", loaded != null && loaded.getItem() == LegacyTestMod.ingot && loaded.getItemDamage() == 1 && loaded.stackSize == 3, loaded);
            ItemStack lapis = new ItemStack(Items.dye, 1, 4);
            check("vanilla metadata item (dye:4)", lapis.getItem() == Items.dye && lapis.getItemDamage() == 4, lapis.getDisplayName());
            check("ore dictionary", OreDictionary.getOres("ingotCopper").size() >= 1, OreDictionary.getOres("ingotCopper"));
            check("vanilla ore dictionary", OreDictionary.getOres("logWood").size() >= 1, OreDictionary.getOres("logWood").size());

            World world = LegacyTestMod.overworld();
            check("world view", world != null && !world.isRemote && world.provider.dimensionId == 0, world);
            int x = 0, y = 200, z = 0;
            world.setBlock(x, y, z, LegacyTestMod.ore, 1, 3);
            check("set block with metadata", world.getBlock(x, y, z) == LegacyTestMod.ore && world.getBlockMetadata(x, y, z) == 1, world.getBlockMetadata(x, y, z));
            world.setBlockMetadataWithNotify(x, y, z, 0, 3);
            check("metadata update", world.getBlockMetadata(x, y, z) == 0, world.getBlockMetadata(x, y, z));
            world.setBlock(x, y, z, Blocks.wool, 3, 3);
            check("vanilla flattening (wool:3)", world.getBlock(x, y, z) == Blocks.wool && world.getBlockMetadata(x, y, z) == 3, world.getBlockMetadata(x, y, z));
            world.setBlock(x, y, z, LegacyTestMod.machine);
            Object te = world.getTileEntity(x, y, z);
            check("tile entity created", te instanceof TileEntityCounter, te);
            world.setBlockToAir(x, y, z);
            check("set air", world.isAirBlock(x, y, z), world.getBlock(x, y, z));
            check("ticks reach legacy handlers", TickHandler.serverTicks >= 0, TickHandler.serverTicks);
            check("world generator ran", OreGenerator.placed > 0, OreGenerator.placed);
            ironChest(world);
            check("JDK 8 APIs", LegacyTestMod.jdkChecks().equals("0102AB/patched"), LegacyTestMod.jdkChecks());
        } catch (Throwable t) {
            failed++;
            results.add("FAIL exception: " + t);
            log.error("LegacyTest self test crashed", t);
        }
        for (String r : results) log.info("LegacyTest " + r);
        log.info("LegacyTest SUMMARY: " + summary());
    }
}
