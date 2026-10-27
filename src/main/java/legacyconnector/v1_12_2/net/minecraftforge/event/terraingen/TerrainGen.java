package legacyconnector.v1_12_2.net.minecraftforge.event.terraingen;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.util.math.ChunkPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Cancelable;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event.HasResult;

/** Forge 1.12.2's helpers legacy generators call to post the terrain events and honour their results. */
public abstract class TerrainGen {
    private static legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.EventBus terrain() {
        return legacyconnector.v1_12_2.net.minecraftforge.common.MinecraftForge.TERRAIN_GEN_BUS;
    }

    public static <T extends InitNoiseGensEvent.Context> T getModdedNoiseGenerators(World world, Random rand, T original) {
        InitNoiseGensEvent<T> event = new InitNoiseGensEvent<>(world, rand, original);
        terrain().post(event);
        return event.getNewValues();
    }

    public static legacyconnector.v1_12_2.net.minecraft.world.gen.MapGenBase getModdedMapGen(legacyconnector.v1_12_2.net.minecraft.world.gen.MapGenBase original,
            InitMapGenEvent.EventType type) {
        InitMapGenEvent event = new InitMapGenEvent(type, original);
        terrain().post(event);
        return event.getNewGen();
    }

    public static boolean populate(legacyconnector.v1_12_2.net.minecraft.world.gen.IChunkGenerator gen, World world, Random rand, int chunkX, int chunkZ,
            boolean hasVillageGenerated, PopulateChunkEvent.Populate.EventType type) {
        PopulateChunkEvent.Populate event = new PopulateChunkEvent.Populate(gen, world, rand, chunkX, chunkZ, hasVillageGenerated, type);
        terrain().post(event);
        return event.getResult() != Event.Result.DENY;
    }

    public static boolean decorate(World world, Random rand, ChunkPos chunkPos, DecorateBiomeEvent.Decorate.EventType type) {
        return decorate(world, rand, chunkPos, null, type);
    }

    public static boolean decorate(World world, Random rand, ChunkPos chunkPos, BlockPos placementPos, DecorateBiomeEvent.Decorate.EventType type) {
        if (world == null || rand == null || chunkPos == null) return true;
        DecorateBiomeEvent.Decorate event = new DecorateBiomeEvent.Decorate(world, rand, chunkPos, placementPos, type);
        terrain().post(event);
        return event.getResult() != Event.Result.DENY;
    }

    @Deprecated
    public static boolean decorate(World world, Random rand, BlockPos pos, DecorateBiomeEvent.Decorate.EventType type) {
        return decorate(world, rand, new ChunkPos(pos), null, type);
    }

    public static boolean generateOre(World world, Random rand, legacyconnector.v1_12_2.net.minecraft.world.gen.feature.WorldGenerator generator, BlockPos pos,
            OreGenEvent.GenerateMinable.EventType type) {
        OreGenEvent.GenerateMinable event = new OreGenEvent.GenerateMinable(world, rand, generator, pos, type);
        legacyconnector.v1_12_2.net.minecraftforge.common.MinecraftForge.ORE_GEN_BUS.post(event);
        return event.getResult() != Event.Result.DENY;
    }

    public static boolean saplingGrowTree(World world, Random rand, BlockPos pos) {
        SaplingGrowTreeEvent event = new SaplingGrowTreeEvent(world, rand, pos);
        terrain().post(event);
        return event.getResult() != Event.Result.DENY;
    }
}
