package legacyconnector.v1_7_10.net.minecraftforge.event.terraingen;

import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import legacyconnector.v1_7_10.net.minecraft.world.gen.NoiseGenerator;
import legacyconnector.v1_7_10.net.minecraftforge.event.world.WorldEvent;

/** A chunk provider is about to use its noise generators: handlers may replace entries of {@link #newNoiseGens}. */
public class InitNoiseGensEvent extends WorldEvent {
    public final Random rand;
    public final NoiseGenerator[] originalNoiseGens;
    public NoiseGenerator[] newNoiseGens;

    public InitNoiseGensEvent(World world, Random rand, NoiseGenerator[] original) {
        super(world);
        this.rand = rand;
        this.originalNoiseGens = original;
        this.newNoiseGens = original == null ? null : original.clone();
    }
}
