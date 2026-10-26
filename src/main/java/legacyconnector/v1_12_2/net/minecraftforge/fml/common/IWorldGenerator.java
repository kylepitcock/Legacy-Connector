package legacyconnector.v1_12_2.net.minecraftforge.fml.common;

/**
 * A mod's chunk populator. Its {@code generate(Random, int, int, World, IChunkGenerator, IChunkProvider)} takes 1.12 world-generation
 * types the layer does not model yet, so the interface only marks implementations; generators are kept by {@code GameRegistry}.
 */
public interface IWorldGenerator {
}
