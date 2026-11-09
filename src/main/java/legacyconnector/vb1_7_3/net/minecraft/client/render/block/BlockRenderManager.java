package legacyconnector.vb1_7_3.net.minecraft.client.render.block;

import legacyconnector.vb1_7_3.net.minecraft.block.Block;
import legacyconnector.vb1_7_3.net.minecraft.world.BlockView;
import legacyconnector.vb1_7_3.net.minecraft.client.render.Tessellator;

/** Standard cuboid helper used by ModLoader's custom block render callbacks. */
public class BlockRenderManager {
    public BlockView blockView;
    public int textureOverride=-1;
    public boolean flipTextureHorizontally,renderAllSides;
    public boolean inventoryColorEnabled=true;
    public static boolean isSideLit(int type){return type==0||type==13||type==10||type==11||type==16;}
    public BlockRenderManager(){}
    public BlockRenderManager(BlockView view){blockView=view;}
    /** Beta's centered inventory/entity cuboid path, with per-face normals and metadata textures. */
    public void render(Block block,int metadata,float brightness){
        int type=block.getRenderType();
        if(type!=0&&type!=16)throw new UnsupportedOperationException("Beta inventory block render type "+type);
        if(inventoryColorEnabled){
            int color=block.getColor(metadata);
            legacyconnector.vb1_7_3.org.lwjgl.opengl.GL11.glColor4f(((color>>16)&255)/255F*brightness,((color>>8)&255)/255F*brightness,(color&255)/255F*brightness,1);
        }
        if(type==16)metadata=1;
        block.setupRenderBoundingBox();
        legacyconnector.vb1_7_3.org.lwjgl.opengl.GL11.glTranslatef(-.5F,-.5F,-.5F);
        try{
            float[][] normals={{0,-1,0},{0,1,0},{0,0,-1},{0,0,1},{-1,0,0},{1,0,0}};
            for(int side=0;side<6;side++){
                Tessellator.INSTANCE.startQuads();var normal=normals[side];Tessellator.INSTANCE.normal(normal[0],normal[1],normal[2]);
                if(org.kylepitcock.legacyconnector.vb1_7_3.RendererContextB173.active()){
                    int color=org.kylepitcock.legacyconnector.vb1_7_3.RendererContextB173.argb();
                    Tessellator.INSTANCE.color(color&0xFFFFFF,(color>>>24)&255);
                }
                face(block,0,0,0,side,block.getTexture(side,metadata));Tessellator.INSTANCE.draw();
            }
        }finally{legacyconnector.vb1_7_3.org.lwjgl.opengl.GL11.glTranslatef(.5F,.5F,.5F);}
    }
    public boolean render(Block b,int x,int y,int z){return renderBlock(b,x,y,z);}
    public boolean renderFence(Block b,int x,int y,int z){
        double[] saved={b.minX,b.minY,b.minZ,b.maxX,b.maxY,b.maxZ};
        try{
            b.setBoundingBox(.375F,0,.375F,.625F,1,.625F);renderBlock(b,x,y,z);
            boolean west=blockView.getBlockId(x-1,y,z)==b.id,east=blockView.getBlockId(x+1,y,z)==b.id;
            boolean north=blockView.getBlockId(x,y,z-1)==b.id,south=blockView.getBlockId(x,y,z+1)==b.id;
            boolean alongX=west||east,alongZ=north||south;if(!alongX&&!alongZ)alongX=true;
            for(float low:new float[]{.75F,.375F}){
                if(alongX){b.setBoundingBox(west?0:.4375F,low,.4375F,east?1:.5625F,low+.1875F,.5625F);renderBlock(b,x,y,z);}
                if(alongZ){b.setBoundingBox(.4375F,low,north?0:.4375F,.5625F,low+.1875F,south?1:.5625F);renderBlock(b,x,y,z);}
            }return true;
        }finally{b.minX=saved[0];b.minY=saved[1];b.minZ=saved[2];b.maxX=saved[3];b.maxY=saved[4];b.maxZ=saved[5];}
    }
    public boolean renderBlock(Block b,int x,int y,int z){
        for(int side=0;side<6;side++)face(b,x,y,z,side,textureOverride>=0?textureOverride:b.getTextureId(blockView,x,y,z,side));return true;
    }
    public boolean renderFlat(Block b,int x,int y,int z,float r,float g,float blue){Tessellator.INSTANCE.color(r,g,blue);return renderBlock(b,x,y,z);}
    public boolean renderSmooth(Block b,int x,int y,int z,float r,float g,float blue){return renderFlat(b,x,y,z,r,g,blue);}
    public void renderWithTexture(Block b,int x,int y,int z,int tex){int old=textureOverride;try{textureOverride=tex;renderBlock(b,x,y,z);}finally{textureOverride=old;}}
    public void renderWithoutCulling(Block b,int x,int y,int z){renderBlock(b,x,y,z);}
    public void renderBottomFace(Block b,double x,double y,double z,int tex){face(b,x,y,z,0,tex);}
    public void renderTopFace(Block b,double x,double y,double z,int tex){face(b,x,y,z,1,tex);}
    public void renderEastFace(Block b,double x,double y,double z,int tex){face(b,x,y,z,2,tex);}
    public void renderWestFace(Block b,double x,double y,double z,int tex){face(b,x,y,z,3,tex);}
    public void renderNorthFace(Block b,double x,double y,double z,int tex){face(b,x,y,z,4,tex);}
    public void renderSouthFace(Block b,double x,double y,double z,int tex){face(b,x,y,z,5,tex);}
    private void face(Block b,double x,double y,double z,int side,int tex){
        if(textureOverride>=0)tex=textureOverride;
        double a=b.minX,c=b.minY,e=b.minZ,d=b.maxX,f=b.maxY,g=b.maxZ;
        double[][] p=switch(side){
            case 0->new double[][]{{a,c,g},{a,c,e},{d,c,e},{d,c,g}};
            case 1->new double[][]{{d,f,g},{d,f,e},{a,f,e},{a,f,g}};
            case 2->new double[][]{{d,f,e},{d,c,e},{a,c,e},{a,f,e}};
            case 3->new double[][]{{a,f,g},{a,c,g},{d,c,g},{d,f,g}};
            case 4->new double[][]{{a,f,e},{a,c,e},{a,c,g},{a,f,g}};
            default->new double[][]{{d,f,g},{d,c,g},{d,c,e},{d,f,e}};
        };
        int index=Math.abs(tex);double u=(index%16)/16.0,v=(index/16)/16.0;
        for(var point:p){
            double su=side<2?point[0]:side<4?point[0]:point[2];double sv=side<2?point[2]:1-point[1];
            if(tex<0 || flipTextureHorizontally)su=1-su;
            Tessellator.INSTANCE.vertex(x+point[0],y+point[1],z+point[2],u+su*15.99/256,v+sv*15.99/256);
        }
    }
}
