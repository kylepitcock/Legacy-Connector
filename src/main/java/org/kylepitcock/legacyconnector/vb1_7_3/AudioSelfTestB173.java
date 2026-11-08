package org.kylepitcock.legacyconnector.vb1_7_3;

import java.nio.file.*;
import java.util.*;
import legacyconnector.vb1_7_3.net.minecraft.block.entity.BlockEntity;
import legacyconnector.vb1_7_3.net.minecraft.entity.Entity;
import legacyconnector.vb1_7_3.net.minecraft.world.World;

/** Development-only proof using the unmodified PortalGun launch callback and its external sound. */
final class AudioSelfTestB173 {
    static void run(){
        // Never replace a running client's sound sink during an integrated-server test.
        if(net.neoforged.fml.loading.FMLEnvironment.dist.isClient())return;
        var block=ContentB173.blocks().stream().filter(b->b.getClass().getSimpleName().equals("Portal_BlockAFP")).findFirst();
        if(block.isEmpty())return;
        List<AudioB173.Request> heard=new ArrayList<>();
        java.lang.reflect.Field redstone=null;int oldRedstone=0;
        try {
            Path game=net.neoforged.fml.loading.FMLPaths.GAMEDIR.get();
            var values=AudioB173.reload(game);
            if(values.variants(AudioB173.Kind.SOUND,"portalgun.afp_launch").isEmpty())return; // the mod's external audio is not installed in this game directory: nothing to check
            
            require(legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.getApplicationDirectory("minecraft/resources/mod/sound").toPath().equals(game.resolve("resources/mod/sound")),"instance-relative application directory");
            var loader=block.get().getClass().getClassLoader();
            String prefix=block.get().getClass().getPackageName()+".";
            var type=Class.forName(prefix+"Portal_TileEntityAFP",true,loader);
            BlockEntity tile=(BlockEntity)type.getConstructor().newInstance();
            World world=new World(){};tile.world=world;
            type.getField("type").setInt(tile,1);
            redstone=Class.forName(prefix+"mod_PortalGun",true,loader).getField("AFPredstone");oldRedstone=redstone.getInt(null);redstone.setInt(null,0);
            Entity entity=new Entity(world){};entity.setPosition(.5,1,.5);entity.velocityY=.2;
            AudioB173.installClient(new AudioB173.Client(){public void play(AudioB173.Request r){heard.add(r);}public void stop(){}public void tick(){}});
            type.getMethod("launchEntity",Entity.class).invoke(tile,entity);
            require(heard.size()==1 && heard.getFirst().name().equals("portalgun.afp_launch"),"original callback reaches World sound bridge");
            var request=heard.getFirst();require(AudioB173.decode(AudioB173.encode(request)).equals(request),"clientbound sound payload");
            require(request.volume()==.5F && request.pitch()==1,"original sound parameters");
            var pack=new HashMap<String,java.util.function.Supplier<byte[]>>();AudioB173.contribute(values,pack);
            var source=values.variants(AudioB173.Kind.SOUND,request.name()).getFirst();
            require(Arrays.equals(Files.readAllBytes(source.file()),pack.get(source.packPath()).get()),"native pack retains original sound bytes");
            org.slf4j.LoggerFactory.getLogger("LegacyConnector/b1.7.3").info("B1.7.3 AUDIO TEST: original PortalGun AFP callback, external resources, payload and pack passed; {} audio files",values.entries().size());
        }catch(Exception e){throw new IllegalStateException("B1.7.3 AUDIO TEST failed",e);}
        finally{AudioB173.installClient(null);if(redstone!=null)try{redstone.setInt(null,oldRedstone);}catch(IllegalAccessException e){throw new IllegalStateException(e);}}
    }
    private static void require(boolean condition,String message){if(!condition)throw new IllegalStateException(message);}
}
