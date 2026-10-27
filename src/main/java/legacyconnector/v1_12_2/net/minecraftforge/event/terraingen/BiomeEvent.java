package legacyconnector.v1_12_2.net.minecraftforge.event.terraingen;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.util.math.ChunkPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Cancelable;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event.HasResult;

/** Forge 1.12.2 biome events (decorator creation, colours, village blocks), posted on the terrain bus. */
public class BiomeEvent extends Event {
    private final legacyconnector.v1_12_2.net.minecraft.world.biome.Biome biome;

    public BiomeEvent(legacyconnector.v1_12_2.net.minecraft.world.biome.Biome biome) {
        this.biome = biome;
    }

    public legacyconnector.v1_12_2.net.minecraft.world.biome.Biome getBiome() {
        return biome;
    }

    public static class CreateDecorator extends BiomeEvent {
        private final legacyconnector.v1_12_2.net.minecraft.world.biome.BiomeDecorator originalBiomeDecorator;
        private legacyconnector.v1_12_2.net.minecraft.world.biome.BiomeDecorator newBiomeDecorator;

        public CreateDecorator(legacyconnector.v1_12_2.net.minecraft.world.biome.Biome biome, legacyconnector.v1_12_2.net.minecraft.world.biome.BiomeDecorator original) {
            super(biome);
            this.originalBiomeDecorator = original;
            this.newBiomeDecorator = original;
        }

        public legacyconnector.v1_12_2.net.minecraft.world.biome.BiomeDecorator getOriginalBiomeDecorator() {
            return originalBiomeDecorator;
        }

        public legacyconnector.v1_12_2.net.minecraft.world.biome.BiomeDecorator getNewBiomeDecorator() {
            return newBiomeDecorator;
        }

        public void setNewBiomeDecorator(legacyconnector.v1_12_2.net.minecraft.world.biome.BiomeDecorator decorator) {
            this.newBiomeDecorator = decorator;
        }
    }

    public static class BiomeColor extends BiomeEvent {
        private final int originalColor;
        private int newColor;

        public BiomeColor(legacyconnector.v1_12_2.net.minecraft.world.biome.Biome biome, int original) {
            super(biome);
            this.originalColor = original;
            this.newColor = original;
        }

        public int getOriginalColor() {
            return originalColor;
        }

        public int getNewColor() {
            return newColor;
        }

        public void setNewColor(int newColor) {
            this.newColor = newColor;
        }
    }

    @HasResult
    public static class GetVillageBlockID extends BiomeEvent {
        private final legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState original;
        private legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState replacement;

        public GetVillageBlockID(legacyconnector.v1_12_2.net.minecraft.world.biome.Biome biome, legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState original) {
            super(biome);
            this.original = original;
        }

        public legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState getOriginal() {
            return original;
        }

        public legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState getReplacement() {
            return replacement;
        }

        public void setReplacement(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState replacement) {
            this.replacement = replacement;
        }
    }

    public static class GetGrassColor extends BiomeColor {
        public GetGrassColor(legacyconnector.v1_12_2.net.minecraft.world.biome.Biome biome, int original) {
            super(biome, original);
        }
    }

    public static class GetFoliageColor extends BiomeColor {
        public GetFoliageColor(legacyconnector.v1_12_2.net.minecraft.world.biome.Biome biome, int original) {
            super(biome, original);
        }
    }

    public static class GetWaterColor extends BiomeColor {
        public GetWaterColor(legacyconnector.v1_12_2.net.minecraft.world.biome.Biome biome, int original) {
            super(biome, original);
        }
    }
}
