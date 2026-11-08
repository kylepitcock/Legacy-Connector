package legacyconnector.vb1_7_3.net.minecraft.block;

import legacyconnector.vb1_7_3.net.minecraft.block.material.Material;
import legacyconnector.vb1_7_3.net.minecraft.entity.Entity;
import legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity;
import legacyconnector.vb1_7_3.net.minecraft.entity.player.PlayerEntity;
import legacyconnector.vb1_7_3.net.minecraft.util.math.Box;
import legacyconnector.vb1_7_3.net.minecraft.world.BlockView;
import legacyconnector.vb1_7_3.net.minecraft.world.World;

/** Beta pressure plates: binary power, entity filters and a scheduled release check. */
public class PressurePlateBlock extends Block {
    private final PressurePlateActivationRule activationRule;
    protected PressurePlateBlock(int id,int texture,PressurePlateActivationRule rule,Material material) {
        super(id,texture,material);activationRule=rule;setupRenderBoundingBox();setTickRandomly(true);
    }
    @Override public int getTickRate(){return 20;}
    @Override public boolean isOpaque(){return false;}
    @Override public boolean isFullCube(){return false;}
    @Override public Box getCollisionShape(World w,int x,int y,int z){return null;}
    @Override public boolean canPlaceAt(World w,int x,int y,int z){return w.method_1783(x,y-1,z);}
    @Override public void neighborUpdate(World w,int x,int y,int z,int other){
        if(!canPlaceAt(w,x,y,z)){dropStacks(w,x,y,z,w.getBlockMeta(x,y,z));w.setBlock(x,y,z,0);}
    }
    private boolean accepts(Entity e){return switch(activationRule){case EVERYTHING->true;case MOBS->e instanceof LivingEntity;case PLAYERS->e instanceof PlayerEntity;};}
    @Override public void onEntityCollision(World w,int x,int y,int z,Entity e){
        if(!w.isRemote && w.getBlockMeta(x,y,z)==0 && accepts(e) && e.boundingBox!=null
                && e.boundingBox.intersects(Box.create(x+0.125,y,z+0.125,x+0.875,y+0.25,z+0.875)))update(w,x,y,z,true);
    }
    @Override public void onTick(World w,int x,int y,int z,java.util.Random random){
        if(!w.isRemote && w.getBlockMeta(x,y,z)!=0)update(w,x,y,z,w.getEntities(null,Box.create(x+0.125,y,z+0.125,x+0.875,y+0.25,z+0.875)).stream().anyMatch(this::accepts));
    }
    private void update(World w,int x,int y,int z,boolean pressed){
        if((w.getBlockMeta(x,y,z)!=0)!=pressed){
            w.setBlockMeta(x,y,z,pressed?1:0);w.notifyNeighbors(x,y,z,id);w.notifyNeighbors(x,y-1,z,id);
        }
        if(pressed)w.scheduleBlockUpdate(x,y,z,id,getTickRate());
    }
    @Override public void onBreak(World w,int x,int y,int z){w.notifyNeighbors(x,y,z,id);w.notifyNeighbors(x,y-1,z,id);}
    @Override public void updateBoundingBox(BlockView w,int x,int y,int z){setBoundingBox(0.0625F,0,0.0625F,0.9375F,w.getBlockMeta(x,y,z)!=0?0.03125F:0.0625F,0.9375F);}
    public void setupRenderBoundingBox(){setBoundingBox(0.0625F,0,0.0625F,0.9375F,0.0625F,0.9375F);}
    @Override public boolean canEmitRedstonePower(){return true;}
    @Override public boolean isEmittingRedstonePowerInDirection(BlockView w,int x,int y,int z,int side){return w.getBlockMeta(x,y,z)!=0;}
    @Override public boolean canTransferPowerInDirection(World w,int x,int y,int z,int side){return side==1 && w.getBlockMeta(x,y,z)!=0;}
    public int getPistonBehavior(){return 1;}
}
