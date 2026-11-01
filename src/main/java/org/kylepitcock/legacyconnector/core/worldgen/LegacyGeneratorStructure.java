package org.kylepitcock.legacyconnector.core.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

/**
 * {@code legacyconnector:legacy_generator_structure}: what a legacy world generator ({@code IWorldGenerator}) builds in place, as a
 * modern structure, so {@code /locate structure} finds it ({@link LegacyGeneratorStructures}). The game never starts one: its starts are
 * added where the legacy generator actually built during decoration (one {@link LegacyMarkerPiece} with the built area), so generation
 * stays exactly the legacy one and only built structures are found.
 */
public final class LegacyGeneratorStructure extends Structure {
    public static final MapCodec<LegacyGeneratorStructure> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            settingsCodec(i),
            Codec.STRING.fieldOf("generator").forGetter(s -> s.generator)).apply(i, LegacyGeneratorStructure::new));

    /** Set when the structure type is registered (LegacyConnector). */
    public static StructureType<LegacyGeneratorStructure> TYPE;

    private final String generator;

    public LegacyGeneratorStructure(StructureSettings settings, String generator) {
        super(settings);
        this.generator = generator;
    }

    /** The id of the legacy generator's feature ({@code <mod>:legacy_worldgen/<generator>}). */
    public String generator() {
        return generator;
    }

    /**
     * Never a start of its own: predicting a legacy generator would mean running it for chunks that do not exist yet, and legacy
     * generators keep state of their own (ChromatiCraft's dungeon generator records each chunk's outcome and skips it afterwards,
     * Witchery keeps a list of its structures and builds none near one, Thaumcraft keeps a map), so a dry run would change where
     * they really build. Starts are recorded as the generator builds.
     */
    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        return Optional.empty();
    }

    @Override
    public StructureType<?> type() {
        return TYPE;
    }
}
