package org.kylepitcock.legacyconnector.vb1_7_3;

import legacyconnector.vb1_7_3.net.minecraft.world.World;
import legacyconnector.vb1_7_3.net.minecraft.block.Block;
import legacyconnector.vb1_7_3.net.minecraft.block.material.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

/** Read-only world view for shape queries, including render chunks and model generation. */
public final class BlockQueryB173 extends World {
    private final BlockGetter level;
    private final BlockPos origin;
    private final BlockState state;
    public BlockQueryB173(BlockGetter level,BlockPos origin,BlockState state) { this.level=level;this.origin=origin;this.state=state; }
    private BlockState state(int x,int y,int z) { var p=new BlockPos(x,y,z);return p.equals(origin)?state:level.getBlockState(p); }
    @Override public int getBlockId(int x,int y,int z) { return BlocksB173.legacyId(state(x,y,z)); }
    @Override public int getBlockMeta(int x,int y,int z) { return BlocksB173.legacyMeta(state(x,y,z)); }
    @Override public boolean method_1783(int x,int y,int z) { return state(x,y,z).canOcclude(); }
    @Override public boolean shouldSuffocate(int x,int y,int z) { return method_1783(x,y,z); }
    @Override public Material getMaterial(int x,int y,int z) { var b=Block.BLOCKS[getBlockId(x,y,z)];return b==null?Material.AIR:b.material; }
    @Override public legacyconnector.vb1_7_3.net.minecraft.block.entity.BlockEntity getBlockEntity(int x,int y,int z) {
        var tile=level.getBlockEntity(new BlockPos(x,y,z));return tile instanceof LegacyB173BlockEntity b?b.legacy():null;
    }
}
