package legacyconnector.v1_12_2.net.minecraft.world.gen.structure;

import java.util.List;
import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.util.Rotation;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.ModernPiece1122;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.ModernPieces1122;

/**
 * 1.12.2 End city layout. A mod's End city start gets the modern game's city (its successor: the same towers, bridges and ship from
 * the server's templates), one legacy piece per modern piece.
 */
public class StructureEndCityPieces {
    public static void registerPieces() {
        MapGenStructureIO.registerStructureComponent(ModernPiece1122.class, "MCCModern");
    }

    /** Adds the pieces of a whole city starting with its base floor at {@code pos}. */
    public static void startHouseTower(legacyconnector.v1_12_2.net.minecraft.world.gen.structure.template.TemplateManager templateManager, BlockPos pos,
            Rotation rot, List<StructureComponent> pieces, Random rand) {
        var templates = ModernPieces1122.templates();
        if (templates == null || pos == null || pieces == null) return;
        List<net.minecraft.world.level.levelgen.structure.StructurePiece> modern = new java.util.ArrayList<>();
        net.minecraft.world.level.levelgen.structure.structures.EndCityPieces.startHouseTower(templates, pos.mcc$modern(), ModernPieces1122.rotation(rot), modern,
                ModernPieces1122.random(rand));
        for (var p : modern) pieces.add(new ModernPiece1122(p));
    }
}
