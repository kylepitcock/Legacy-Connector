package legacyconnector.vb1_7_3.net.minecraft.block;

/** Beta shared bed metadata queries used by independently implemented bed blocks. */
public class BedBlock extends Block {
    public BedBlock(int id){super(id,legacyconnector.vb1_7_3.net.minecraft.block.material.Material.WOOL);}
    public static int getDirection(int meta){return meta&3;}
    public static boolean isHeadOfBed(int meta){return (meta&8)!=0;}
    public static boolean isOccupied(int meta){return (meta&4)!=0;}
    public static void updateState(legacyconnector.vb1_7_3.net.minecraft.world.World world,int x,int y,int z,boolean occupied){
        world.setBlockMeta(x,y,z,org.kylepitcock.legacyconnector.vb1_7_3.BedsB173.occupied(world.getBlockMeta(x,y,z),occupied));
    }
    @Override public int getRenderType(){return 14;}
    @Override public boolean isOpaque(){return false;}
    @Override public boolean isFullCube(){return false;}
}
