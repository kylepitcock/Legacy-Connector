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
    /** Beta {@code getFluidHeight}: the surface height at a block, averaged over the four corners it touches. */
    private float fluidHeight(Block b,int x,int y,int z){
        float total=0;int count=0;
        for(int i=0;i<4;i++){
            int cx=x-(i&1),cz=z-(i>>1&1);
            if(blockView.getMaterial(cx,y+1,cz)==b.material)return 1.0F;
            var m=blockView.getMaterial(cx,y,cz);
            if(m==b.material){
                int meta=blockView.getBlockMeta(cx,y,cz);
                if(meta>=8||meta==0){total+=legacyconnector.vb1_7_3.net.minecraft.block.LiquidBlock.getFluidHeightFromMeta(meta)*10F;count+=10;}
                total+=legacyconnector.vb1_7_3.net.minecraft.block.LiquidBlock.getFluidHeightFromMeta(meta);count++;
            }else if(!m.blocksMovement()){total++;count++;}
        }
        return 1.0F-total/count;
    }
    private static void fluidVertex(double x,double y,double z,double u,double v){Tessellator.INSTANCE.vertex(x,y,z,u/256.0,v/256.0);}
    /** Beta {@code renderBlockFluids}: a fluid block with a sloped surface and flowing-texture rotation; hidden faces are skipped. */
    public boolean renderFluid(Block b,int x,int y,int z){
        int tint=b.getColorMultiplier(blockView,x,y,z);
        float cr=((tint>>16)&255)/255F,cg=((tint>>8)&255)/255F,cb=(tint&255)/255F;
        boolean top=renderAllSides||b.isSideVisible(blockView,x,y+1,z,1),bottom=renderAllSides||b.isSideVisible(blockView,x,y-1,z,0);
        boolean[] side={renderAllSides||b.isSideVisible(blockView,x,y,z-1,2),renderAllSides||b.isSideVisible(blockView,x,y,z+1,3),
                renderAllSides||b.isSideVisible(blockView,x-1,y,z,4),renderAllSides||b.isSideVisible(blockView,x+1,y,z,5)};
        if(!top&&!bottom&&!side[0]&&!side[1]&&!side[2]&&!side[3])return false;
        boolean drawn=false;
        int meta=blockView.getBlockMeta(x,y,z);
        float h00=fluidHeight(b,x,y,z),h01=fluidHeight(b,x,y,z+1),h11=fluidHeight(b,x+1,y,z+1),h10=fluidHeight(b,x+1,y,z);
        if(top){
            drawn=true;
            int tex=b.getTexture(1,meta);
            float angle=(float)legacyconnector.vb1_7_3.net.minecraft.block.LiquidBlock.getFlowingAngle(blockView,x,y,z,b.material);
            if(angle>-999F)tex=b.getTexture(2,meta);
            int u0=(tex&15)<<4,v0=tex&0xF0;
            double cu=u0+8D,cv=v0+8D;
            if(angle<-999F)angle=0;else{cu=u0+16D;cv=v0+16D;}
            float sa=net.minecraft.util.Mth.sin(angle)*8F,ca=net.minecraft.util.Mth.cos(angle)*8F;
            Tessellator.INSTANCE.color(cr,cg,cb);
            fluidVertex(x,y+h00,z,cu-ca-sa,cv-ca+sa);
            fluidVertex(x,y+h01,z+1,cu-ca+sa,cv+ca+sa);
            fluidVertex(x+1,y+h11,z+1,cu+ca+sa,cv+ca-sa);
            fluidVertex(x+1,y+h10,z,cu+ca-sa,cv-ca-sa);
        }
        if(bottom){
            Tessellator.INSTANCE.color(cr*.5F,cg*.5F,cb*.5F);
            face(b,x,y,z,0,b.getTexture(0,meta));
            drawn=true;
        }
        for(int i=0;i<4;i++){
            if(!side[i])continue;
            int tex=b.getTexture(i+2,meta);
            int u0=(tex&15)<<4,v0=tex&0xF0;
            float ha,hb;double xa,xb,za,zb;
            if(i==0){ha=h00;hb=h10;xa=x;xb=x+1;za=z;zb=z;}
            else if(i==1){ha=h11;hb=h01;xa=x+1;xb=x;za=z+1;zb=z+1;}
            else if(i==2){ha=h01;hb=h00;xa=x;xb=x;za=z+1;zb=z;}
            else{ha=h10;hb=h11;xa=x+1;xb=x+1;za=z;zb=z+1;}
            double ua=u0,ub=u0+16-0.01,va=v0+(1F-ha)*16F,vb=v0+(1F-hb)*16F,vBottom=v0+16-0.01;
            float shade=i<2?0.8F:0.6F;
            Tessellator.INSTANCE.color(cr*shade,cg*shade,cb*shade);
            fluidVertex(xa,y+ha,za,ua,va);
            fluidVertex(xb,y+hb,zb,ub,vb);
            fluidVertex(xb,y,zb,ub,vBottom);
            fluidVertex(xa,y,za,ua,vBottom);
            drawn=true;
        }
        return drawn;
    }
    private static final int[][] NEIGHBOURS={{0,-1,0},{0,1,0},{0,0,-1},{0,0,1},{-1,0,0},{1,0,0}};
    /** Beta renderStandardBlock: each face is drawn only when the block's own isSideVisible accepts the neighbour position. */
    public boolean renderBlock(Block b,int x,int y,int z){
        for(int side=0;side<6;side++){
            var d=NEIGHBOURS[side];
            if(blockView!=null&&!renderAllSides&&!b.isSideVisible(blockView,x+d[0],y+d[1],z+d[2],side))continue;
            face(b,x,y,z,side,textureOverride>=0?textureOverride:b.getTextureId(blockView,x,y,z,side));
        }
        return true;
    }
    public boolean renderFlat(Block b,int x,int y,int z,float r,float g,float blue){Tessellator.INSTANCE.color(r,g,blue);return renderBlock(b,x,y,z);}
    public boolean renderSmooth(Block b,int x,int y,int z,float r,float g,float blue){return renderFlat(b,x,y,z,r,g,blue);}
    public void renderWithTexture(Block b,int x,int y,int z,int tex){int old=textureOverride;try{textureOverride=tex;renderBlock(b,x,y,z);}finally{textureOverride=old;}}
    public void renderWithoutCulling(Block b,int x,int y,int z){boolean old=renderAllSides;renderAllSides=true;try{renderBlock(b,x,y,z);}finally{renderAllSides=old;}}
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
