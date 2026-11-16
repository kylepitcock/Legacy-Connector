package legacyconnector.v1_6_4.net.minecraft.src.forge.oredict;

import java.util.ArrayList;
import legacyconnector.v1_6_4.net.minecraft.src.forge.IOreHandler;
import legacyconnector.v1_6_4.net.minecraft.src.forge.MinecraftForge;
import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.item.Item;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;

/** Forge 3.x's ore dictionary on the shim's: the same names and registrations, so recipes and tags see them. */
public class OreDictionary {
    public static int getOreID(String name) {
        return legacyconnector.v1_7_10.net.minecraftforge.oredict.OreDictionary.getOreID(name);
    }

    public static String getOreName(int id) {
        return legacyconnector.v1_7_10.net.minecraftforge.oredict.OreDictionary.getOreName(id);
    }

    public static ArrayList<ItemStack> getOres(String name) {
        return legacyconnector.v1_7_10.net.minecraftforge.oredict.OreDictionary.getOres(name);
    }

    public static ArrayList<ItemStack> getOres(Integer id) {
        return legacyconnector.v1_7_10.net.minecraftforge.oredict.OreDictionary.getOres(id);
    }

    public static void registerOreHandler(IOreHandler handler) {
        MinecraftForge.registerOreHandler(handler);
    }

    public static void registerOre(String name, Item ore) {
        registerOre(name, new ItemStack(ore));
    }

    public static void registerOre(String name, Block ore) {
        registerOre(name, new ItemStack(ore));
    }

    public static void registerOre(String name, ItemStack ore) {
        legacyconnector.v1_7_10.net.minecraftforge.oredict.OreDictionary.registerOre(name, ore);
    }

    public static void registerOre(int id, Item ore) {
        registerOre(getOreName(id), ore);
    }

    public static void registerOre(int id, Block ore) {
        registerOre(getOreName(id), ore);
    }

    public static void registerOre(int id, ItemStack ore) {
        registerOre(getOreName(id), ore);
    }
}
