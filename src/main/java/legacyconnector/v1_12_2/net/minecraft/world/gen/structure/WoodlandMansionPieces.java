package legacyconnector.v1_12_2.net.minecraft.world.gen.structure;

import java.util.List;
import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.util.Rotation;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.ModernPiece1122;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.ModernPieces1122;

/**
 * 1.12.2 woodland mansion layout. A mod's mansion start gets the modern game's mansion (its successor: the same room grid and
 * templates), one legacy piece per modern piece.
 */
public class WoodlandMansionPieces {
    public static void registerWoodlandMansionPieces() {
        MapGenStructureIO.registerStructureComponent(ModernPiece1122.class, "MCCModern");
    }

    /** Adds the pieces of a whole mansion at {@code pos}. */
    public static void generateMansion(legacyconnector.v1_12_2.net.minecraft.world.gen.structure.template.TemplateManager templateManager, BlockPos pos,
            Rotation rotation, List<StructureComponent> pieces, Random random) {
        var templates = ModernPieces1122.templates();
        if (templates == null || pos == null || pieces == null) return;
        List<net.minecraft.world.level.levelgen.structure.structures.WoodlandMansionPieces.WoodlandMansionPiece> modern = new java.util.ArrayList<>();
        net.minecraft.world.level.levelgen.structure.structures.WoodlandMansionPieces.generateMansion(templates, pos.mcc$modern(),
                ModernPieces1122.rotation(rotation), modern, ModernPieces1122.random(random));
        for (var p : modern) pieces.add(new ModernPiece1122(p));
    }
}
