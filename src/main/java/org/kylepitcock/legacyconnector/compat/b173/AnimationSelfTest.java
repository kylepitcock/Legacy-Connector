package org.kylepitcock.legacyconnector.compat.b173;

import java.util.*;
import java.awt.image.BufferedImage;
import org.kylepitcock.legacyconnector.vb1_7_3.*;

/** Actual supplied mods' animation callbacks, source pixels, custom-sheet routing and reloads. */
public final class AnimationSelfTest {
    public static void run(){
        int failures=AnimationsB173.FAILURES.get();
        var assets=new HashMap<String,java.util.function.Supplier<byte[]>>();
        new AssetsB173().contribute(net.minecraft.server.packs.PackType.CLIENT_RESOURCES,assets);
        var entries=AnimationsB173.entries();
        var factory=entries.stream().filter(e->e.effect().getClass().getName().equals("fizz.minefactoryreloaded.TextureFrameAnimFX")).toList();
        var portal=entries.stream().filter(e->e.effect().getClass().getSimpleName().equals("TextAnimFramesFX")).toList();
        if(ContentB173.isModLoaded("mod_MineFactory"))require(factory.size()==8,"eight original factory animations registered; got "+entries.stream().map(e->e.effect().getClass().getName()+"@"+e.sheet()+"#"+e.effect().sprite).toList());
        if(ContentB173.isModLoaded("mod_PortalGun"))require(portal.size()==1,"original fizzler animation registered");
        String[] names={"conveyor","conveyorR","harvester","rancher","fertilizer","vet","blockbreaker","crafter"};
        var sources=new LinkedHashMap<AnimationsB173.Entry,BufferedImage>();
        for(int i=0;i<factory.size();i++){
            var entry=factory.get(i);require(!entry.sheet().equals("/terrain.png"),"original Forge bind chooses custom sheet");
            sources.put(entry,read("/fizz/MineFactory/animations/"+names[i]+".png"));
        }
        for(var entry:portal){require(entry.sheet().equals("/terrain.png"),"default terrain atlas binding");sources.put(entry,read("/portalgun/Block_PortalFizzler.png"));}
        for(var block:ContentB173.blocks())if(block.getClass().getSimpleName().equals("BlockFactoryConveyor")){
            var belt=factory.getFirst();require(AssetsB173.worldTextures(block).get(block.textureId).equals(AnimationsB173.target(belt.sheet(),belt.effect().sprite)),"conveyor renderer uses the animated atlas sprite");
        }
        int ticks=sources.values().stream().mapToInt(i->i.getWidth()/i.getHeight()).max().orElse(1)*2;
        var distinct=new HashMap<AnimationsB173.Entry,Set<Integer>>();
        for(var entry:sources.keySet()){
            String target=AnimationsB173.target(entry.sheet(),entry.effect().sprite);require(target!=null,"animation has atlas target");
            require(assets.containsKey("assets/"+target.replace(":","/textures/")+".png"),"animated sprite PNG generated");
            distinct.put(entry,new HashSet<>());
        }
        for(int tick=0;tick<ticks;tick++){
            var delivered=new HashMap<String,List<AnimationsB173.Frame>>();AnimationsB173.tick(frame->delivered.computeIfAbsent(frame.texture(),k->new ArrayList<>()).add(frame));
            var consumed=new HashMap<String,Integer>();
            for(var pair:sources.entrySet()){
                var entry=pair.getKey();var source=pair.getValue();int count=source.getWidth()/source.getHeight();
                String target=AnimationsB173.target(entry.sheet(),entry.effect().sprite);
                var frames=delivered.get(target);require(frames!=null,"registered frame delivered to atlas sink");
                int delivery=consumed.merge(target,1,Integer::sum)-1;require(delivery<frames.size(),"shared sprite retains every callback in order");var frame=frames.get(delivery);
                int expected=(tick+2)%count; // registration tick selects frame 1, then the first live tick selects frame 2.
                require(Arrays.equals(frame.pixels(),rgba(source,expected,frame.size())),"original frame pixels/order "+entry.effect().getClass()+" tick "+tick);
                distinct.get(entry).add(Arrays.hashCode(frame.pixels()));
            }
        }
        for(var pair:distinct.entrySet())require(pair.getValue().size()>1,"actual animation changes pixels "+pair.getKey().effect().getClass());
        int count=entries.size();new AssetsB173().contribute(net.minecraft.server.packs.PackType.CLIENT_RESOURCES,new HashMap<>());
        require(AnimationsB173.entries().size()==count,"resource reload replaces effects without duplicates");
        require(AnimationsB173.FAILURES.get()==failures,"no animation registration/tick failures");
        stripTiming();
        org.slf4j.LoggerFactory.getLogger("LegacyConnector/b1.7.3").info("B1.7.3 ACTUAL ANIMATION TEST: {} factory + {} portal animations, {} ticks of source-pixel/atlas checks, reload and strip timing passed",factory.size(),portal.size(),ticks);
    }
    private static BufferedImage read(String path){try{byte[] bytes=AssetsB173.resourceBytes(path);require(bytes!=null,"actual animation resource "+path);return javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(bytes));}catch(java.io.IOException e){throw new IllegalStateException(e);}}
    private static byte[] rgba(BufferedImage image,int frame,int size){
        int source=image.getHeight();byte[] result=new byte[size*size*4];
        for(int y=0;y<size;y++)for(int x=0;x<size;x++){int color=image.getRGB(frame*source+x*source/size,y*source/size),i=(y*size+x)*4;result[i]=(byte)(color>>16);result[i+1]=(byte)(color>>8);result[i+2]=(byte)color;result[i+3]=(byte)(color>>24);}return result;
    }
    private static void stripTiming(){
        var source=new BufferedImage(2,6,BufferedImage.TYPE_INT_ARGB);int[] colors={0xff123456,0x80765432,0xffabcdef};
        for(int f=0;f<3;f++)for(int y=0;y<2;y++)for(int x=0;x<2;x++)source.setRGB(x,y+2*f,colors[f]);
        var effect=new legacyconnector.vb1_7_3.net.minecraft.src.ModTextureAnimation(7,2,1,source,3);
        require(effect.replicate==2 && effect.atlas==1,"strip constructor preserves replicate and item atlas");
        for(int tick=0;tick<10;tick++){effect.tick();int expected=(1+tick/3)%3;require(AnimationsB173.image(effect).getRGB(0,0)==colors[expected],"ModLoader strip frame duration and RGBA");}
    }
    private static void require(boolean ok,String message){if(!ok)throw new IllegalStateException(message);}
}
