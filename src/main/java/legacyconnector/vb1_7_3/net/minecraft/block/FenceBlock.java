package legacyconnector.vb1_7_3.net.minecraft.block;

import legacyconnector.vb1_7_3.net.minecraft.block.material.Material;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
import legacyconnector.vb1_7_3.net.minecraft.util.math.Box;

/** Beta fences have a full-width 1.5-block collision box; connections are a render concern. */
public class FenceBlock extends Block {
    public FenceBlock(int id,int texture){super(id,texture,Material.WOOD);}
    @Override public boolean canPlaceAt(World world,int x,int y,int z){return world.getBlockId(x,y-1,z)!=id && world.getMaterial(x,y-1,z).isSolid() && super.canPlaceAt(world,x,y,z);}
    @Override public Box getCollisionShape(World world,int x,int y,int z){return Box.create(x,y,z,x+1,y+1.5,z+1);}
    @Override public boolean isOpaque(){return false;}
    @Override public boolean isFullCube(){return false;}
    @Override public int getRenderType(){return 11;}
}
