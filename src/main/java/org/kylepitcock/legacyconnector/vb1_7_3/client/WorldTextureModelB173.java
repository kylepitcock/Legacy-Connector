package org.kylepitcock.legacyconnector.vb1_7_3.client;

import java.util.List;
import java.util.Map;
import legacyconnector.vb1_7_3.net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import org.kylepitcock.legacyconnector.vb1_7_3.*;

/** Selects Beta face sprites from the render region, including neighbour-dependent chest fronts. */
final class WorldTextureModelB173 extends BakedModelWrapper<BakedModel> {
    private static final ModelProperty<int[]> FACES=new ModelProperty<>();
    private static final ModelProperty<List<BakedQuad>> CUSTOM=new ModelProperty<>();
    private static final ModelProperty<Integer> VISIBLE=new ModelProperty<>();
    private final Block block;
    private final Map<Integer,String> palette;
    private WorldTextureModelB173(BakedModel base,Block block){super(base);this.block=block;palette=AssetsB173.worldTextures(block);}
    static void wrap(Map<ModelResourceLocation,BakedModel> models){
        for(var block:ContentB173.blocks()){
            if(!(block.mcc$modern instanceof LegacyB173Block) || (!AssetsB173.usesWorldTextures(block) && !BlockFacesB173.hasCallback(block) && !BlockRenderB173.capturesWorld(block)))continue;
            for(var state:block.mcc$modern.getStateDefinition().getPossibleStates()){
                var key=BlockModelShaper.stateToModelLocation(state);var base=models.get(key);
                if(base!=null)models.put(key,new WorldTextureModelB173(base,block));
            }
        }
    }
    @Override public ModelData getModelData(BlockAndTintGetter level,BlockPos pos,BlockState state,ModelData data){
        var result=originalModel.getModelData(level,pos,state,data).derive().with(FACES,AssetsB173.faceTextures(block,level,pos,state));
        if(BlockFacesB173.hasCallback(block))result.with(VISIBLE,BlockFacesB173.visibleFaces(block,new BlockQueryB173(level,pos,state),pos.getX(),pos.getY(),pos.getZ()));
        if(BlockRenderB173.capturesWorld(block)){
            var captured=BlockRenderB173.captureOrNull(block,new BlockQueryB173(level,pos,state),pos.getX(),pos.getY(),pos.getZ());
            if(captured!=null)result.with(CUSTOM,bake(captured,pos));
        }
        return result.build();
    }
    @Override public List<BakedQuad> getQuads(BlockState state,Direction side,RandomSource random,ModelData data,RenderType renderType){
        var custom=data.get(CUSTOM);if(custom!=null)return side==null?custom:List.of();
        var visible=data.get(VISIBLE);
        List<BakedQuad> quads;
        if(visible!=null) {
            // Unculled output lets the original callback decide visibility even against native opaque neighbours.
            if(side!=null)return List.of();
            var selected=new java.util.ArrayList<BakedQuad>();
            for(var quad:originalModel.getQuads(state,null,random,data,renderType))
                if((visible&(1<<quad.getDirection().get3DDataValue()))!=0)selected.add(quad);
            for(var face:Direction.values())if((visible&(1<<face.get3DDataValue()))!=0)
                selected.addAll(originalModel.getQuads(state,face,random,data,renderType));
            quads=selected;
        }else quads=originalModel.getQuads(state,side,random,data,renderType);
        int[] faces=data.get(FACES);
        if(faces==null)return quads;
        return quads.stream().map(q->retexture(q,faces[q.getDirection().get3DDataValue()])).toList();
    }
    private List<BakedQuad> bake(List<legacyconnector.vb1_7_3.net.minecraft.client.render.Tessellator.Vertex> vertices,BlockPos pos){
        var result=new java.util.ArrayList<BakedQuad>();
        for(int q=0;q+3<vertices.size();q+=4){
            var a=vertices.get(q);var b=vertices.get(q+1);var c=vertices.get(q+2);
            var normal=new org.joml.Vector3f((float)(b.x()-a.x()),(float)(b.y()-a.y()),(float)(b.z()-a.z()))
                    .cross((float)(c.x()-a.x()),(float)(c.y()-a.y()),(float)(c.z()-a.z()));
            if(normal.lengthSquared()<1e-12)continue;normal.normalize();
            double u=0,v=0;for(int i=0;i<4;i++){u+=vertices.get(q+i).u()/4;v+=vertices.get(q+i).v()/4;}
            int tileU=(int)Math.floor(u*16),tileV=(int)Math.floor(v*16);String ref=palette.get(tileV*16+tileU);
            if(ref==null)continue;
            var sprite=Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(ResourceLocation.parse(ref));
            int[] packed=new int[32];int normalBits=((byte)(normal.x*127)&255)|(((byte)(normal.y*127)&255)<<8)|(((byte)(normal.z*127)&255)<<16);
            for(int i=0;i<4;i++){
                var p=vertices.get(q+i);int o=i*8;
                packed[o]=Float.floatToRawIntBits((float)(p.x()-pos.getX()));packed[o+1]=Float.floatToRawIntBits((float)(p.y()-pos.getY()));packed[o+2]=Float.floatToRawIntBits((float)(p.z()-pos.getZ()));
                packed[o+3]=p.color();packed[o+4]=Float.floatToRawIntBits(sprite.getU((float)(p.u()*16-tileU)));packed[o+5]=Float.floatToRawIntBits(sprite.getV((float)(p.v()*16-tileV)));packed[o+7]=normalBits;
            }
            result.add(new BakedQuad(packed,-1,Direction.getNearest(normal.x,normal.y,normal.z),sprite,true));
        }
        return List.copyOf(result);
    }
    private BakedQuad retexture(BakedQuad quad,int index){
        String ref=palette.get(Math.abs(index));if(ref==null)return quad;
        var sprite=Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(ResourceLocation.parse(ref));
        var old=quad.getSprite();if(sprite==old && index>=0)return quad;
        int[] vertices=quad.getVertices().clone();int stride=vertices.length/4;
        for(int i=0;i<4;i++){
            int offset=i*stride;float u=(Float.intBitsToFloat(vertices[offset+4])-old.getU0())/(old.getU1()-old.getU0());
            float v=(Float.intBitsToFloat(vertices[offset+5])-old.getV0())/(old.getV1()-old.getV0());
            if(index<0)u=1-u;
            vertices[offset+4]=Float.floatToRawIntBits(sprite.getU(u));vertices[offset+5]=Float.floatToRawIntBits(sprite.getV(v));
        }
        return new BakedQuad(vertices,quad.getTintIndex(),quad.getDirection(),sprite,quad.isShade(),quad.hasAmbientOcclusion());
    }
}
