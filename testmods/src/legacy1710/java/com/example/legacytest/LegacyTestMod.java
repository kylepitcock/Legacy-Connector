package com.example.legacytest;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import javax.xml.bind.DatatypeConverter;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;
import org.apache.logging.log4j.Logger;

/** A deliberately "typical" 1.7.10 mod, written the way mods were written in 2014. */
@Mod(modid = LegacyTestMod.MODID, name = "Legacy Test Mod", version = "1.0", acceptedMinecraftVersions = "[1.7.10]")
public class LegacyTestMod {
    public static final String MODID = "LegacyTest";

    @Mod.Instance(MODID)
    public static LegacyTestMod instance;

    @SidedProxy(clientSide = "com.example.legacytest.ClientProxy", serverSide = "com.example.legacytest.CommonProxy")
    public static CommonProxy proxy;

    public static final String CONSTANT = "original";

    public static Logger log;
    public static Block ore;
    public static Block machine;
    public static Item ingot;
    public static int oreVeinsPerChunk;
    public static SimpleNetworkWrapper network;
    public static CreativeTabs tab;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        log = event.getModLog();
        Configuration config = new Configuration(event.getSuggestedConfigurationFile());
        config.load();
        oreVeinsPerChunk = config.getInt("oreVeinsPerChunk", Configuration.CATEGORY_GENERAL, 6, 0, 64, "Copper/tin veins generated per chunk");
        if (config.hasChanged()) config.save();

        tab = new CreativeTabs("legacytest") {
            @Override
            public Item getTabIconItem() {
                return ingot;
            }
        };

        ore = new BlockLegacyOre().setBlockName("legacytest.ore").setCreativeTab(tab);
        machine = new BlockMachine().setBlockName("legacytest.machine").setCreativeTab(tab);
        ingot = new ItemIngot().setUnlocalizedName("legacytest.ingot").setCreativeTab(tab);

        GameRegistry.registerBlock(ore, ItemBlockOre.class, "ore");
        GameRegistry.registerBlock(machine, "machine");
        GameRegistry.registerItem(ingot, "ingot");
        GameRegistry.registerTileEntity(TileEntityCounter.class, "LegacyTestCounter");

        OreDictionary.registerOre("ingotCopper", new ItemStack(ingot, 1, 0));
        OreDictionary.registerOre("ingotTin", new ItemStack(ingot, 1, 1));
        OreDictionary.registerOre("oreCopper", new ItemStack(ore, 1, 0));
        OreDictionary.registerOre("oreTin", new ItemStack(ore, 1, 1));

        network = NetworkRegistry.INSTANCE.newSimpleChannel("LegacyTest");
        network.registerMessage(MessageCounter.Handler.class, MessageCounter.class, 0, Side.CLIENT);
        proxy.preInit();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        GameRegistry.addRecipe(new ShapedOreRecipe(new ItemStack(machine), "III", "IRI", "III", 'I', "ingotCopper", 'R', Items.redstone));
        GameRegistry.addShapelessRecipe(new ItemStack(ingot, 2, 1), new ItemStack(Blocks.wool, 1, 3), Items.iron_ingot);
        GameRegistry.addRecipe(new ItemStack(Items.diamond), "CC", "CC", 'C', new ItemStack(ingot, 1, 0));
        GameRegistry.addSmelting(new ItemStack(ore, 1, 0), new ItemStack(ingot, 1, 0), 0.5F);
        GameRegistry.addSmelting(new ItemStack(ore, 1, 1), new ItemStack(ingot, 1, 1), 0.5F);
        GameRegistry.registerWorldGenerator(new OreGenerator(), 0);
        MinecraftForge.EVENT_BUS.register(new ForgeEventHandler());
        FMLCommonHandler.instance().bus().register(new TickHandler());
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        log.info("LegacyTest postInit: Loader.isModLoaded(LegacyTest)=" + Loader.isModLoaded(MODID));
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandLegacyTest());
    }

    @Mod.EventHandler
    public void serverStarted(FMLServerStartedEvent event) {
        SelfTest.run(log);
        if (Boolean.getBoolean("legacytest.autostop")) {
            log.info("LegacyTest: autostop requested, shutting the server down");
            MinecraftServer.getServer().initiateShutdown();
        }
    }

    /** Java 8 era APIs that were removed or locked down in later JDKs. */
    static String jdkChecks() throws Exception {
        String hex = DatatypeConverter.printHexBinary(new byte[] {1, 2, (byte) 0xAB});
        Field f = LegacyTestMod.class.getDeclaredField("CONSTANT");
        f.setAccessible(true);
        Field modifiers = Field.class.getDeclaredField("modifiers");
        modifiers.setAccessible(true);
        modifiers.setInt(f, f.getModifiers() & ~Modifier.FINAL);
        f.set(null, "patched");
        return hex + "/" + f.get(null);
    }

    static World overworld() {
        return MinecraftServer.getServer().worldServerForDimension(0);
    }
}
