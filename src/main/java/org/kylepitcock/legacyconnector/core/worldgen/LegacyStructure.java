package org.kylepitcock.legacyconnector.core.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

/**
 * A legacy structure generator as a modern structure ({@code legacyconnector:legacy_structure}, one per generator, generated into the
 * data pack by {@link LegacyStructureRegistry}). Its structure set places it with random spread spacing 1, so every chunk is a
 * candidate and the legacy {@code canSpawnStructureAtCoords} decides, with its own spacing grid and biome checks as in 1.7.10; the
 * modern game adds the structure's biome tag, the set's exclusion zone around villages, saving, {@code /locate} and the references
 * other generation sees. Each legacy component becomes one {@link LegacyStructurePiece}.
 */
public final class LegacyStructure extends Structure {
    public static final MapCodec<LegacyStructure> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            settingsCodec(i),
            Codec.STRING.fieldOf("generator").forGetter(s -> s.generator)).apply(i, LegacyStructure::new));

    public static StructureType<LegacyStructure> TYPE;

    private final String generator;

    public LegacyStructure(StructureSettings settings, String generator) {
        super(settings);
        this.generator = generator;
    }

    public String generator() {
        return generator;
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        LegacyStructureSource source = LegacyStructureRegistry.source(generator);
        if (source == null) return Optional.empty();
        var level = LegacyStructureRegistry.levelOf(context.chunkGenerator());
        if (level == null) return Optional.empty();
        int cx = context.chunkPos().x, cz = context.chunkPos().z;
        List<LegacyStructureSource.Part> parts;
        try {
            parts = source.start(level, context.seed(), cx, cz);
        } catch (RuntimeException | LinkageError e) {
            LegacyStructureRegistry.failed(generator, "laying out a start", e);
            return Optional.empty();
        }
        if (parts == null || parts.isEmpty()) return Optional.empty();
        // the biome check of the modern placement samples here: the chunk centre at sea level (1.7.10 biomes were per column)
        BlockPos at = new BlockPos(context.chunkPos().getMiddleBlockX(), context.chunkGenerator().getSeaLevel(), context.chunkPos().getMiddleBlockZ());
        return Optional.of(new GenerationStub(at, builder -> {
            for (int n = 0; n < parts.size(); n++) {
                var p = parts.get(n);
                builder.addPiece(new LegacyStructurePiece(generator, cx, cz, n, p.handle(), p.box(), p.depth()));
            }
        }));
    }

    @Override
    public StructureType<?> type() {
        return TYPE;
    }
}
