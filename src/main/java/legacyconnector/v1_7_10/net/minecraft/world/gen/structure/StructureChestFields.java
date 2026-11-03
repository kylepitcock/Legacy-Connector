package legacyconnector.v1_7_10.net.minecraft.world.gen.structure;

import legacyconnector.v1_7_10.net.minecraft.util.WeightedRandomChestContent;
import legacyconnector.v1_7_10.net.minecraftforge.common.ChestGenHooks;

/**
 * The live chest-list fields of the structure pieces ({@code StructureStrongholdPieces.ChestCorridor.strongholdChestContents},
 * {@code StructureVillagePieces.House2.villageBlacksmithChestContents}, ...), by the Forge chest category they belong to. Mods that
 * add to those arrays directly instead of through {@code ChestGenHooks} are picked up when the loot bridge builds its pools.
 */
public final class StructureChestFields {
    /** Loot-table category of the nether fortress chests (Forge 1.7.10 had no {@code ChestGenHooks} category for them). */
    public static final String NETHER_FORTRESS = "netherFortress";

    private StructureChestFields() {}

    /** The entries a mod put into the piece class's chest list for the category (never null). */
    public static WeightedRandomChestContent[] extras(String category) {
        WeightedRandomChestContent[] items = null;
        if (category == null) {
            items = null;
        } else if (category.equals(ChestGenHooks.STRONGHOLD_CORRIDOR)) {
            items = StructureStrongholdPieces.ChestCorridor.strongholdChestContents;
        } else if (category.equals(ChestGenHooks.STRONGHOLD_LIBRARY)) {
            items = StructureStrongholdPieces.Library.strongholdLibraryChestContents;
        } else if (category.equals(ChestGenHooks.STRONGHOLD_CROSSING)) {
            items = StructureStrongholdPieces.RoomCrossing.strongholdRoomCrossingChestContents;
        } else if (category.equals(ChestGenHooks.MINESHAFT_CORRIDOR)) {
            items = StructureMineshaftPieces.Corridor.mineshaftChestContents;
        } else if (category.equals(ChestGenHooks.VILLAGE_BLACKSMITH)) {
            items = StructureVillagePieces.House2.villageBlacksmithChestContents;
        } else if (category.equals(ChestGenHooks.PYRAMID_DESERT_CHEST)) {
            items = ComponentScatteredFeaturePieces.DesertPyramid.itemsToGenerateInTemple;
        } else if (category.equals(ChestGenHooks.PYRAMID_JUNGLE_CHEST)) {
            items = ComponentScatteredFeaturePieces.JunglePyramid.junglePyramidsChestContents;
        } else if (category.equals(NETHER_FORTRESS)) {
            items = StructureNetherBridgePieces.Piece.field_111019_a;
        }
        return items == null ? new WeightedRandomChestContent[0] : items;
    }
}
