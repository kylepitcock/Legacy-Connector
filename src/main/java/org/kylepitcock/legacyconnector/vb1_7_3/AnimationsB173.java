package org.kylepitcock.legacyconnector.vb1_7_3;

import java.util.*;
import java.awt.image.BufferedImage;
import legacyconnector.vb1_7_3.net.minecraft.client.render.texture.DynamicTexture;
import legacyconnector.vb1_7_3.net.minecraft.client.texture.TextureManager;
import org.kylepitcock.legacyconnector.core.LegacyModRecord;

/** Shared TextureFX registration, sheet binding and live RGBA frames. No client/GL dependencies. */
public final class AnimationsB173 {
    public record Entry(DynamicTexture effect,String sheet,LegacyModRecord owner,boolean hook){}
    public record Frame(String texture,int size,byte[] pixels){}
    private static final List<Entry> EFFECTS=new ArrayList<>();
    private static final Map<String,String> TARGETS=new HashMap<>();
    private static final Set<DynamicTexture> FAILED=Collections.newSetFromMap(new IdentityHashMap<>());
    public static final java.util.concurrent.atomic.AtomicInteger FAILURES=new java.util.concurrent.atomic.AtomicInteger();
    private static boolean inHook;
    private static final class Binding {final Map<Integer,String> ids=new HashMap<>();String sheet;}
    private static final ThreadLocal<Binding> BINDING=new ThreadLocal<>();
    private static String key(String sheet,int sprite){return sheet+"#"+sprite;}
    public static Integer textureId(String path){var b=BINDING.get();if(b==null)return null;int id=-1-b.ids.size();b.ids.put(id,path);return id;}
    public static boolean bindId(int id){var b=BINDING.get();if(b==null)return false;b.sheet=b.ids.get(id);return true;}
    public static void bindSheet(String path){var b=BINDING.get();if(b!=null)b.sheet=path;}
    public static synchronized void add(DynamicTexture effect){
        var binding=new Binding();BINDING.set(binding);
        try{effect.bind(TextureManager.mcc$instance());}finally{BINDING.remove();}
        String sheet=binding.sheet;if(sheet==null)sheet=effect.atlas==0?"/terrain.png":"/gui/items.png";
        effect.tick();size(effect);
        var owner=ContentB173.current();
        var entry=new Entry(effect,sheet,owner,inHook);
        // The original texture manager ticks registrations in order, including shared sprite slots.
        // Keep every callback; later uploads to the same slot win, as in Beta.
        EFFECTS.add(entry);
    }
    /** Recreate hook-owned effects for resource reload, retaining registrations made by constructors. */
    public static synchronized void registerHooks(){
        EFFECTS.removeIf(Entry::hook);TARGETS.clear();FAILED.clear();inHook=true;
        try{
            for(var mod:org.kylepitcock.legacyconnector.core.LegacyMods.all()){
                if(!PlatformB173.VERSION.equals(mod.version) || !(mod.instance instanceof legacyconnector.vb1_7_3.net.minecraft.src.BaseMod base))continue;
                ContentB173.enter(mod);
                try{base.RegisterAnimation(legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$instance());}
                catch(RuntimeException|LinkageError e){FAILURES.incrementAndGet();org.slf4j.LoggerFactory.getLogger("LegacyConnector/b1.7.3").warn("Beta animation registration failed for {}",base.getClass().getName(),e);}
                finally{ContentB173.exit();}
            }
        }finally{inHook=false;}
    }
    public static synchronized List<Entry> entries(){return List.copyOf(EFFECTS);}
    public static synchronized Entry find(String sheet,int sprite){
        for(int i=EFFECTS.size()-1;i>=0;i--){var e=EFFECTS.get(i);if(e.sheet.equals(sheet)){
            int delta=sprite-e.effect.sprite;int repeat=Math.max(1,e.effect.replicate);
            if(delta>=0 && delta/16<repeat && delta%16<repeat)return e;
        }}return null;
    }
    public static synchronized void target(String sheet,int sprite,String texture){TARGETS.put(key(sheet,sprite),texture);}
    public static synchronized String target(String sheet,int sprite){return TARGETS.get(key(sheet,sprite));}
    public static int size(DynamicTexture effect){
        int size=(int)Math.sqrt(effect.pixels.length/4);if(size<=0 || size*size*4!=effect.pixels.length)throw new IllegalArgumentException("Invalid Beta RGBA frame length "+effect.pixels.length);return size;
    }
    public static BufferedImage image(DynamicTexture effect){
        int size=size(effect);var image=new BufferedImage(size,size,BufferedImage.TYPE_INT_ARGB);
        for(int y=0;y<size;y++)for(int x=0;x<size;x++){int i=(y*size+x)*4;byte[] p=effect.pixels;image.setRGB(x,y,((p[i+3]&255)<<24)|((p[i]&255)<<16)|((p[i+1]&255)<<8)|(p[i+2]&255));}return image;
    }
    /** One legacy texture tick, delivered to either the atlas uploader or a headless verification sink. */
    public static synchronized void tick(java.util.function.Consumer<Frame> sink){
        for(var e:EFFECTS){
            if(FAILED.contains(e.effect))continue;
            try{
                e.effect.tick();int size=size(e.effect);byte[] pixels=e.effect.pixels.clone();int repeat=Math.max(1,e.effect.replicate);
                for(int y=0;y<repeat;y++)for(int x=0;x<repeat;x++){
                    String target=TARGETS.get(key(e.sheet,e.effect.sprite+x+16*y));if(target!=null)sink.accept(new Frame(target,size,pixels));
                }
            }catch(RuntimeException|LinkageError ex){FAILED.add(e.effect);FAILURES.incrementAndGet();org.slf4j.LoggerFactory.getLogger("LegacyConnector/b1.7.3").warn("Beta animation tick failed for {}",e.effect.getClass().getName(),ex);}
        }
    }
}
