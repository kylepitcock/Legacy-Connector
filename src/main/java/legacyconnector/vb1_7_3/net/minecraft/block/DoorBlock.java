package legacyconnector.vb1_7_3.net.minecraft.block;
import legacyconnector.vb1_7_3.net.minecraft.block.material.Material;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
import legacyconnector.vb1_7_3.net.minecraft.world.BlockView;
import legacyconnector.vb1_7_3.net.minecraft.entity.player.PlayerEntity;
public class DoorBlock extends Block {
    protected DoorBlock(int id,Material material){super(id,material);textureId=material==Material.METAL?98:97;}
    @Override public boolean isOpaque(){return false;}
    @Override public boolean isFullCube(){return false;}
    @Override public int getRenderType(){return 7;}
    public static boolean getOpen(int meta){return (meta&4)!=0;}
    public int setOpen(int meta){return getOpen(meta)?meta&3:(meta-1)&3;}
    public void rotate(int direction){
        switch(direction&3){case 0->setBoundingBox(0,0,0,1,1,0.1875F);case 1->setBoundingBox(0.8125F,0,0,1,1,1);case 2->setBoundingBox(0,0,0.8125F,1,1,1);case 3->setBoundingBox(0,0,0,0.1875F,1,1);}
    }
    @Override public void updateBoundingBox(BlockView w,int x,int y,int z){rotate(setOpen(w.getBlockMeta(x,y,z)));}
    @Override public int getTexture(int side,int meta){return textureId-((meta&8)!=0?16:0);}
    @Override public boolean canPlaceAt(World w,int x,int y,int z){return (w.mcc$level()==null || y+1<w.mcc$level().getMaxBuildHeight()) && w.method_1783(x,y-1,z) && super.canPlaceAt(w,x,y,z) && super.canPlaceAt(w,x,y+1,z);}
    @Override public boolean onUse(World w,int x,int y,int z,PlayerEntity player){
        if(material==Material.METAL)return true;
        if((w.getBlockMeta(x,y,z)&8)!=0)y--;
        setOpen(w,x,y,z,!getOpen(w.getBlockMeta(x,y,z)));return true;
    }
    @Override public void onBlockBreakStart(World w,int x,int y,int z,PlayerEntity player){onUse(w,x,y,z,player);}
    public void setOpen(World w,int x,int y,int z,boolean open){
        int meta=w.getBlockMeta(x,y,z);if((meta&8)!=0){setOpen(w,x,y-1,z,open);return;}
        if(getOpen(meta)==open)return;
        int changed=meta^4;
        // Update both halves before notifying neighbors so neither sees an inconsistent pair.
        w.setBlockMetaWithoutNotifyingNeighbors(x,y,z,changed);
        if(w.getBlockId(x,y+1,z)==id)w.setBlockMetaWithoutNotifyingNeighbors(x,y+1,z,changed|8);
        w.notifyNeighbors(x,y,z,id);w.notifyNeighbors(x,y+1,z,id);
        if(w.mcc$level()!=null)w.mcc$level().playSound(null,new net.minecraft.core.BlockPos(x,y,z),open?net.minecraft.sounds.SoundEvents.WOODEN_DOOR_OPEN:net.minecraft.sounds.SoundEvents.WOODEN_DOOR_CLOSE,net.minecraft.sounds.SoundSource.BLOCKS,1,1);
    }
    @Override public void neighborUpdate(World w,int x,int y,int z,int other){
        int meta=w.getBlockMeta(x,y,z);
        if((meta&8)!=0){if(w.getBlockId(x,y-1,z)!=id)w.setBlock(x,y,z,0);else if(other!=id)neighborUpdate(w,x,y-1,z,other);return;}
        if(w.getBlockId(x,y+1,z)!=id || !w.method_1783(x,y-1,z)){
            w.setBlock(x,y,z,0);if(w.getBlockId(x,y+1,z)==id)w.setBlock(x,y+1,z,0);dropStacks(w,x,y,z,meta);return;
        }
        boolean powered=w.isEmittingRedstonePower(x,y,z)||w.isEmittingRedstonePower(x,y+1,z);
        Block source=other>0 && other<BLOCKS.length?BLOCKS[other]:null;
        if(powered || source!=null && source.canEmitRedstonePower())setOpen(w,x,y,z,powered);
    }
    @Override public int getDroppedItemId(int meta,java.util.Random random){return (meta&8)!=0?0:material==Material.METAL?330:324;}
}
