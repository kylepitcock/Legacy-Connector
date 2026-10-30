package org.kylepitcock.legacyconnector.v1_12_2.bridge;

import java.util.List;
import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import legacyconnector.v1_12_2.net.minecraft.world.gen.structure.StructureBoundingBox;
import legacyconnector.v1_12_2.net.minecraft.world.gen.structure.StructureComponent;

/**
 * A legacy structure piece standing for one modern piece (End city and mansion pieces the legacy generators add to their lists):
 * it builds, moves and saves the modern piece ({@link ModernPieces1122}). Saved as "MCCModern".
 */
public class ModernPiece1122 extends StructureComponent {
    private List<net.minecraft.world.level.levelgen.structure.StructurePiece> modern = new java.util.ArrayList<>();

    public ModernPiece1122() {
    }

    public ModernPiece1122(net.minecraft.world.level.levelgen.structure.StructurePiece piece) {
        super(piece.getGenDepth());
        this.modern = new java.util.ArrayList<>(List.of(piece));
        this.boundingBox = ModernPieces1122.box(this.modern);
        this.setCoordBaseMode(ModernPieces1122.facing(this.modern));
    }

    public net.minecraft.world.level.levelgen.structure.StructurePiece mcc$modern() {
        return modern.isEmpty() ? null : modern.get(0);
    }

    @Override
    public boolean addComponentParts(World worldIn, Random randomIn, StructureBoundingBox structureBoundingBoxIn) {
        return ModernPieces1122.place(modern, worldIn, randomIn, structureBoundingBoxIn);
    }

    @Override
    public void offset(int x, int y, int z) {
        super.offset(x, y, z);
        ModernPieces1122.offset(modern, x, y, z);
    }

    @Override
    protected void writeStructureToNBT(NBTTagCompound tagCompound) {
        ModernPieces1122.save(modern, tagCompound);
    }

    @Override
    protected void readStructureFromNBT(NBTTagCompound tag, legacyconnector.v1_12_2.net.minecraft.world.gen.structure.template.TemplateManager manager) {
        this.modern = ModernPieces1122.load(tag);
    }
}
