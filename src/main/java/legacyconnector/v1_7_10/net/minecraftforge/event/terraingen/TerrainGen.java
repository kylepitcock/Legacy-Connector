package legacyconnector.v1_7_10.net.minecraftforge.event.terraingen;

import java.util.Random;
import legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.Event;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import legacyconnector.v1_7_10.net.minecraft.world.WorldType;
import legacyconnector.v1_7_10.net.minecraft.world.gen.feature.WorldGenerator;
import legacyconnector.v1_7_10.net.minecraft.world.gen.layer.GenLayer;
import legacyconnector.v1_7_10.net.minecraftforge.common.MinecraftForge;

/** Forge's world generation gates: each posts its event and allows the pass unless a listener denies it. */
public abstract class TerrainGen {
    public static GenLayer[] getModdedBiomeGenerators(WorldType worldType, long seed, GenLayer[] original) {
        return original;
    }

    public static boolean decorate(World world, Random rand, int chunkX, int chunkZ, DecorateBiomeEvent.Decorate.EventType type) {
        DecorateBiomeEvent.Decorate event = new DecorateBiomeEvent.Decorate(world, rand, chunkX, chunkZ, type);
        MinecraftForge.TERRAIN_GEN_BUS.post(event);
        return event.getResult() != Event.Result.DENY;
    }

    public static boolean populate(legacyconnector.v1_7_10.net.minecraft.world.chunk.IChunkProvider provider, World world, Random rand, int chunkX,
            int chunkZ, boolean hasVillageGenerated, PopulateChunkEvent.Populate.EventType type) {
        PopulateChunkEvent.Populate event = new PopulateChunkEvent.Populate(provider, world, rand, chunkX, chunkZ, hasVillageGenerated, type);
        MinecraftForge.TERRAIN_GEN_BUS.post(event);
        return event.getResult() != Event.Result.DENY;
    }

    public static boolean generateOre(World world, Random rand, WorldGenerator generator, int worldX, int worldZ,
            OreGenEvent.GenerateMinable.EventType type) {
        OreGenEvent.GenerateMinable event = new OreGenEvent.GenerateMinable(world, rand, generator, worldX, worldZ, type);
        MinecraftForge.ORE_GEN_BUS.post(event);
        return event.getResult() != Event.Result.DENY;
    }

    /** The generator a world uses for a structure or carver: the original unless a handler of {@link InitMapGenEvent} substitutes one. */
    public static <T extends legacyconnector.v1_7_10.net.minecraft.world.gen.MapGenBase> T getModdedMapGen(T original, InitMapGenEvent.EventType type) {
        InitMapGenEvent event = new InitMapGenEvent(type, original);
        MinecraftForge.TERRAIN_GEN_BUS.post(event);
        @SuppressWarnings("unchecked")
        T result = (T) event.newGen;
        return result;
    }

    public static boolean saplingGrowTree(legacyconnector.v1_7_10.net.minecraft.world.World world, java.util.Random random, int x, int y, int z) {
        return true;
    }
}
