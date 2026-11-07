package org.kylepitcock.legacyconnector.vb1_7_3;

import java.util.*;
import legacyconnector.vb1_7_3.net.minecraft.block.Block;
import legacyconnector.vb1_7_3.net.minecraft.src.BaseMod;
import legacyconnector.vb1_7_3.net.minecraft.client.render.Tessellator;
import legacyconnector.vb1_7_3.net.minecraft.client.render.block.BlockRenderManager;
import legacyconnector.vb1_7_3.net.minecraft.world.BlockView;

/** ModLoader render IDs dispatch to their owning mod; geometry is captured without GL or client classes. */
public final class BlockRenderB173 {
    private static final Map<Integer,BaseMod> RENDERERS=new java.util.concurrent.ConcurrentHashMap<>();
    private static final Set<Class<?>> REPORTED=java.util.concurrent.ConcurrentHashMap.newKeySet();
    public static final java.util.concurrent.atomic.AtomicInteger FAILURES=new java.util.concurrent.atomic.AtomicInteger();
    public static void register(int id,BaseMod mod){RENDERERS.put(id,mod);}
    public static boolean hasRenderer(int id){return RENDERERS.containsKey(id);}
    public static List<Tessellator.Vertex> capture(Block block,BlockView world,int x,int y,int z){
        BaseMod mod=RENDERERS.get(block.getRenderType());if(mod==null)return List.of();
        // Legacy renderers mutate block bounds, and may keep mutable renderer state on the mod instance.
        synchronized(mod){synchronized(block){
            double[] bounds={block.minX,block.minY,block.minZ,block.maxX,block.maxY,block.maxZ};
            Tessellator.beginCapture();
            try{
                block.updateBoundingBox(world,x,y,z);
                boolean rendered=mod.RenderWorldBlock(new BlockRenderManager(world),world,x,y,z,block,block.getRenderType());
                var vertices=Tessellator.endCapture();return rendered?vertices:List.of();
            }catch(RuntimeException|LinkageError e){
                FAILURES.incrementAndGet();if(REPORTED.add(block.getClass()))org.slf4j.LoggerFactory.getLogger("LegacyConnector/b1.7.3").warn("Beta block renderer failed for {}",block.getClass().getName(),e);
                return List.of();
            }finally{Tessellator.endCapture();block.minX=bounds[0];block.minY=bounds[1];block.minZ=bounds[2];block.maxX=bounds[3];block.maxY=bounds[4];block.maxZ=bounds[5];}
        }}
    }
}
