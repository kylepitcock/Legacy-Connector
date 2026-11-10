package legacyconnector.vb1_7_3.net.minecraft.block;

import legacyconnector.vb1_7_3.net.minecraft.world.World;
import legacyconnector.vb1_7_3.net.minecraft.block.material.Material;
import org.kylepitcock.legacyconnector.vb1_7_3.RecordsB173;

/** Original record-block API backed by the native jukebox container and song lifecycle. */
public class JukeboxBlock extends BlockWithEntity {
    protected JukeboxBlock(int id,int texture){super(id,texture,Material.WOOD);}
    @Override public int getTexture(int side){return textureId+(side==1?1:0);}
    public void insertRecord(World world,int x,int y,int z,int item){RecordsB173.insert(world,x,y,z,item);}
    public void tryEjectRecord(World world,int x,int y,int z){RecordsB173.eject(world,x,y,z);}
    @Override public boolean onUse(World world,int x,int y,int z,legacyconnector.vb1_7_3.net.minecraft.entity.player.PlayerEntity player){
        if(world.getBlockMeta(x,y,z)==0)return false;
        tryEjectRecord(world,x,y,z);return true;
    }
    @Override public void onBreak(World world,int x,int y,int z){tryEjectRecord(world,x,y,z);super.onBreak(world,x,y,z);}
    @Override public legacyconnector.vb1_7_3.net.minecraft.block.entity.BlockEntity createBlockEntity(){
        return new legacyconnector.vb1_7_3.net.minecraft.block.entity.JukeboxBlockEntity();
    }
}
