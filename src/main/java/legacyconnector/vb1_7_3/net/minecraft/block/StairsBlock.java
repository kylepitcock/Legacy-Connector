package legacyconnector.vb1_7_3.net.minecraft.block;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
import legacyconnector.vb1_7_3.net.minecraft.world.BlockView;
import legacyconnector.vb1_7_3.net.minecraft.util.math.Box;
import legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity;
public class StairsBlock extends Block {
    private final Block base;
    protected StairsBlock(int id,Block base) {
        super(id,base.textureId,base.material);this.base=base;hardness=base.hardness;resistance=base.resistance;soundGroup=base.soundGroup;setOpacity(255);
    }
    @Override public boolean isOpaque(){return false;}
    @Override public boolean isFullCube(){return false;}
    @Override public int getRenderType(){return 10;}
    @Override public int getTexture(int side,int meta){return base==null?super.getTexture(side,meta):base.getTexture(side,meta);}
    @Override public int getTexture(int side){return base==null?super.getTexture(side):base.getTexture(side);}
    @Override public void updateBoundingBox(BlockView world,int x,int y,int z){setBoundingBox(0,0,0,1,1,1);}
    @Override public void onPlaced(World world,int x,int y,int z,LivingEntity placer) {
        int direction=((int)Math.floor(placer.yaw*4/360+0.5))&3;
        world.setBlockMeta(x,y,z,new int[]{2,1,3,0}[direction]);
    }
    @Override public void addIntersectingBoundingBox(World world,int x,int y,int z,Box mask,java.util.ArrayList<Box> boxes) {
        Box bottom=Box.create(x,y,z,x+1,y+0.5,z+1);
        if(bottom.intersects(mask))boxes.add(bottom);
        int m=world.getBlockMeta(x,y,z)&3;
        Box top=switch(m) {
            case 0 -> Box.create(x+0.5,y+0.5,z,x+1,y+1,z+1);
            case 1 -> Box.create(x,y+0.5,z,x+0.5,y+1,z+1);
            case 2 -> Box.create(x,y+0.5,z+0.5,x+1,y+1,z+1);
            default -> Box.create(x,y+0.5,z,x+1,y+1,z+0.5);
        };
        if(top.intersects(mask))boxes.add(top);
    }
}
