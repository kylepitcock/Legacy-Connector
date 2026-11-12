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
    private static final ThreadLocal<Boolean> CAPTURING=new ThreadLocal<>();
    /** True while a Beta block renderer runs: Beta drew on the game thread, so renderers could write blocks; here geometry is built on worker threads, where world writes are dropped. */
    public static boolean capturing(){return CAPTURING.get()!=null;}
    public static void register(int id,BaseMod mod){RENDERERS.put(id,mod);}
    private static final Map<Class<?>,Boolean> POSITIONED=new java.util.concurrent.ConcurrentHashMap<>();
    public static boolean hasRenderer(int id){return id==11 || RENDERERS.containsKey(id);}
    /** Beta sets bounds per position before every standard render, so untinted standard blocks with their own bounds are captured per position. */
    public static boolean capturesWorld(Block block){
        int type=block.getRenderType();
        if(hasRenderer(type))return true;
        return type==0&&!BlockTintB173.hasColor(block)&&POSITIONED.computeIfAbsent(block.getClass(),c->{
            try{return c.getMethod("updateBoundingBox",BlockView.class,int.class,int.class,int.class).getDeclaringClass()!=Block.class;}
            catch(ReflectiveOperationException e){return false;}
        });
    }
    public static List<Tessellator.Vertex> capture(Block block,BlockView world,int x,int y,int z){
        BaseMod mod=RENDERERS.get(block.getRenderType());if(!capturesWorld(block))return List.of();
        // Legacy renderers mutate block bounds, and may keep mutable renderer state on the mod instance.
        synchronized(mod==null?BlockRenderB173.class:mod){synchronized(block){
            double[] bounds={block.minX,block.minY,block.minZ,block.maxX,block.maxY,block.maxZ};
            Tessellator.beginCapture();CAPTURING.set(Boolean.TRUE);
            try{
                block.updateBoundingBox(world,x,y,z);
                var renderer=new BlockRenderManager(world);
                boolean rendered=mod!=null?mod.RenderWorldBlock(renderer,world,x,y,z,block,block.getRenderType())
                        :block.getRenderType()==11?renderer.renderFence(block,x,y,z):renderer.renderBlock(block,x,y,z);
                var vertices=Tessellator.endCapture();return rendered?vertices:List.of();
            }catch(RuntimeException|LinkageError e){
                FAILURES.incrementAndGet();if(REPORTED.add(block.getClass()))org.slf4j.LoggerFactory.getLogger("LegacyConnector/b1.7.3").warn("Beta block renderer failed for {}",block.getClass().getName(),e);
                return List.of();
            }finally{CAPTURING.remove();Tessellator.endCapture();block.minX=bounds[0];block.minY=bounds[1];block.minZ=bounds[2];block.maxX=bounds[3];block.maxY=bounds[4];block.maxZ=bounds[5];}
        }}
    }
}
