package legacyconnector.v1_12_2.net.minecraft.world.gen.structure;

import java.util.Map;
import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.Mirror;
import legacyconnector.v1_12_2.net.minecraft.util.Rotation;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import legacyconnector.v1_12_2.net.minecraft.world.gen.structure.template.PlacementSettings;
import legacyconnector.v1_12_2.net.minecraft.world.gen.structure.template.Template;
import legacyconnector.v1_12_2.net.minecraft.world.gen.structure.template.TemplateManager;

/**
 * A structure piece built from a structure template (end cities, mansions and mods' template pieces): the template is placed at
 * {@link #templatePosition} with {@link #placeSettings}, then each data marker block is handed to {@link #handleDataMarker}.
 */
public abstract class StructureComponentTemplate extends StructureComponent {
    protected Template template;
    protected PlacementSettings placeSettings;
    protected BlockPos templatePosition;

    public StructureComponentTemplate() {
        this.placeSettings = mcc$defaultSettings();
    }

    public StructureComponentTemplate(int type) {
        super(type);
        this.placeSettings = mcc$defaultSettings();
    }

    private static PlacementSettings mcc$defaultSettings() {
        PlacementSettings s = new PlacementSettings().setIgnoreEntities(true);
        try {
            s.setReplacedBlock(legacyconnector.v1_12_2.net.minecraft.init.Blocks.AIR);
        } catch (RuntimeException | LinkageError e) {
            // vanilla constants not ready: nothing is replaced
        }
        return s;
    }

    protected void setup(Template templateIn, BlockPos pos, PlacementSettings settings) {
        this.template = templateIn;
        setCoordBaseMode(EnumFacing.NORTH);
        this.templatePosition = pos;
        this.placeSettings = settings;
        setBoundingBoxFromTemplate();
    }

    @Override
    protected void writeStructureToNBT(NBTTagCompound tagCompound) {
        if (templatePosition == null) return;
        tagCompound.setInteger("TPX", templatePosition.getX());
        tagCompound.setInteger("TPY", templatePosition.getY());
        tagCompound.setInteger("TPZ", templatePosition.getZ());
    }

    @Override
    protected void readStructureFromNBT(NBTTagCompound tagCompound, TemplateManager manager) {
        this.templatePosition = new BlockPos(tagCompound.getInteger("TPX"), tagCompound.getInteger("TPY"), tagCompound.getInteger("TPZ"));
    }

    @Override
    public boolean addComponentParts(World worldIn, Random randomIn, StructureBoundingBox structureBoundingBoxIn) {
        if (template == null || templatePosition == null) return false;
        try {
            placeSettings.setBoundingBox(structureBoundingBoxIn);
            template.addBlocksToWorld(worldIn, templatePosition, placeSettings, 18);
            Map<BlockPos, String> markers = template.getDataBlocks(templatePosition, placeSettings);
            for (var e : markers.entrySet()) handleDataMarker(e.getValue(), e.getKey(), worldIn, randomIn, structureBoundingBoxIn);
        } catch (RuntimeException e) {
            // world generation must not fail
        }
        return true;
    }

    protected abstract void handleDataMarker(String function, BlockPos pos, World worldIn, Random rand, StructureBoundingBox sbb);

    /** The piece's box: the template's rotated size at the template position, shifted for rotation and mirroring as 1.12 did. */
    private void setBoundingBoxFromTemplate() {
        Rotation rotation = placeSettings.getRotation();
        BlockPos size = template.transformedSize(rotation);
        Mirror mirror = placeSettings.getMirror();
        this.boundingBox = new StructureBoundingBox(0, 0, 0, size.getX(), size.getY() - 1, size.getZ());
        switch (rotation) {
            case CLOCKWISE_90 -> boundingBox.offset(-size.getX(), 0, 0);
            case COUNTERCLOCKWISE_90 -> boundingBox.offset(0, 0, -size.getZ());
            case CLOCKWISE_180 -> boundingBox.offset(-size.getX(), 0, -size.getZ());
            default -> { }
        }
        boolean quarter = rotation == Rotation.CLOCKWISE_90 || rotation == Rotation.COUNTERCLOCKWISE_90;
        if (mirror == Mirror.FRONT_BACK) {
            BlockPos b = BlockPos.ORIGIN;
            if (!quarter) b = rotation == Rotation.CLOCKWISE_180 ? b.offset(EnumFacing.EAST, size.getX()) : b.offset(EnumFacing.WEST, size.getX());
            else b = b.offset(rotation.rotate(EnumFacing.WEST), size.getZ());
            boundingBox.offset(b.getX(), 0, b.getZ());
        } else if (mirror == Mirror.LEFT_RIGHT) {
            BlockPos b = BlockPos.ORIGIN;
            if (!quarter) b = rotation == Rotation.CLOCKWISE_180 ? b.offset(EnumFacing.SOUTH, size.getZ()) : b.offset(EnumFacing.NORTH, size.getZ());
            else b = b.offset(rotation.rotate(EnumFacing.NORTH), size.getX());
            boundingBox.offset(b.getX(), 0, b.getZ());
        }
        boundingBox.offset(templatePosition.getX(), templatePosition.getY(), templatePosition.getZ());
    }

    @Override
    public void offset(int x, int y, int z) {
        super.offset(x, y, z);
        if (templatePosition != null) templatePosition = templatePosition.add(x, y, z);
    }
}
