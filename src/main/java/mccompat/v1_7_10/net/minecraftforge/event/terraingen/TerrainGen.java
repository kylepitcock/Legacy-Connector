package mccompat.v1_7_10.net.minecraftforge.event.terraingen;

import java.util.Random;
import mccompat.v1_7_10.cpw.mods.fml.common.eventhandler.Event;
import mccompat.v1_7_10.net.minecraft.world.World;
import mccompat.v1_7_10.net.minecraft.world.WorldType;
import mccompat.v1_7_10.net.minecraft.world.gen.feature.WorldGenerator;
import mccompat.v1_7_10.net.minecraft.world.gen.layer.GenLayer;
import mccompat.v1_7_10.net.minecraftforge.common.MinecraftForge;

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

    public static boolean generateOre(World world, Random rand, WorldGenerator generator, int worldX, int worldZ,
            OreGenEvent.GenerateMinable.EventType type) {
        OreGenEvent.GenerateMinable event = new OreGenEvent.GenerateMinable(world, rand, generator, worldX, worldZ, type);
        MinecraftForge.ORE_GEN_BUS.post(event);
        return event.getResult() != Event.Result.DENY;
    }
}
