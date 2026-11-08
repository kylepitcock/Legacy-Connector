package org.kylepitcock.legacyconnector.vb1_7_3.client;

import java.util.*;
import java.util.concurrent.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.*;
import net.minecraft.client.sounds.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.valueproviders.ConstantFloat;
import net.neoforged.neoforge.client.event.SelectMusicEvent;
import org.kylepitcock.legacyconnector.vb1_7_3.AudioB173;
import org.kylepitcock.legacyconnector.vb1_7_3.AudioB173.*;

/** Native sound channels, sliders and listener, with Beta's named variants and one streaming source. */
public final class AudioClientB173 implements AudioB173.Client {
    private final Set<SoundInstance> active=new HashSet<>();
    private final Random random=new Random();
    private SoundInstance streaming,music;
    private net.minecraft.client.multiplayer.ClientLevel previousLevel;
    private int musicDelay=random.nextInt(6000);

    public static void install(){
        var client=new AudioClientB173();AudioB173.installClient(client);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.ClientTickEvent.Post e)->client.tick());
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(client::selectMusic);
    }
    @Override public void play(Request request){Minecraft.getInstance().execute(()->playNow(request));}
    private void playNow(Request request){
        Minecraft mc=Minecraft.getInstance();
        if(request.kind()==Kind.STREAMING && streaming!=null){mc.getSoundManager().stop(streaming);active.remove(streaming);streaming=null;}
        if(request.name()==null||request.name().isEmpty()||request.volume()<=0)return;
        var variants=AudioB173.catalog().variants(request.kind(),request.name());
        SoundInstance sound;
        if(!variants.isEmpty()){
            Source file=variants.get(random.nextInt(variants.size()));
            boolean stream=file.streaming() || mc.getResourceManager().getResource(resource(file)).isEmpty();
            sound=new Instance(request,file,stream);
        }
        else {
            String name=request.kind()==Kind.STREAMING && !request.name().contains(".")?"records."+request.name():request.name();
            var event=org.kylepitcock.legacyconnector.v1_7_10.bridge.SoundBridge.sound(name);
            if(event==null)return;
            sound=new SimpleSoundInstance(event.getLocation(),category(request.kind()),request.volume(),request.pitch(),SoundInstance.createUnseededRandom(),false,0,
                    request.relative()?SoundInstance.Attenuation.NONE:SoundInstance.Attenuation.LINEAR,request.x(),request.y(),request.z(),request.relative());
        }
        if(request.kind()==Kind.STREAMING){streaming=sound;if(music!=null)mc.getSoundManager().stop(music);mc.getMusicManager().stopPlaying();}
        if(request.kind()==Kind.MUSIC||request.kind()==Kind.CAVEMUSIC)music=sound;
        active.add(sound);mc.getSoundManager().play(sound);
    }
    private static SoundSource category(Kind kind){return switch(kind){case SOUND->SoundSource.BLOCKS;case STREAMING->SoundSource.RECORDS;default->SoundSource.MUSIC;};}
    private static ResourceLocation resource(Source file){return ResourceLocation.fromNamespaceAndPath(file.id().getNamespace(),"sounds/"+file.id().getPath()+".ogg");}
    @Override public void stop(){Minecraft.getInstance().execute(()->{
        var manager=Minecraft.getInstance().getSoundManager();for(var sound:active)manager.stop(sound);active.clear();streaming=null;music=null;
    });}
    @Override public void tick(){
        var mc=Minecraft.getInstance();
        if(previousLevel!=mc.level){if(previousLevel!=null)stop();previousLevel=mc.level;}
        active.removeIf(sound->!mc.getSoundManager().isActive(sound));
        if(mc.level==null||mc.isPaused())return;
        if(musicDelay>0)musicDelay--;
    }
    private void selectMusic(SelectMusicEvent event){
        var mc=Minecraft.getInstance();if(mc.level==null||mc.player==null||mc.isPaused())return;
        var manager=mc.getSoundManager();
        if(streaming!=null && manager.isActive(streaming) || music!=null && manager.isActive(music)){event.setMusic(null);return;}
        if(event.getPlayingMusic()!=null||musicDelay>0||mc.options.getSoundSourceVolume(SoundSource.MUSIC)<=0)return;
        Kind kind=mc.level.canSeeSky(mc.player.blockPosition())?Kind.MUSIC:Kind.CAVEMUSIC;
        var pool=AudioB173.catalog().sources(kind);if(pool.isEmpty())return;
        var chosen=pool.get(random.nextInt(pool.size()));
        playNow(new Request(kind,chosen.event(),0,0,0,1,1,true));
        musicDelay=6000+random.nextInt(6000);event.setMusic(null);
    }
    /** File chosen once, so event variants and WAV decoding use the same source. */
    public static final class Instance extends SimpleSoundInstance {
        private final Source file;
        private final boolean stream;
        public Instance(Request request,Source file,boolean stream){
            super(AudioB173.identifier(request.kind(),request.name()),category(request.kind()),request.kind()==Kind.STREAMING?.5F:request.volume(),request.kind()==Kind.STREAMING?1:request.pitch(),SoundInstance.createUnseededRandom(),false,0,
                    request.relative()?SoundInstance.Attenuation.NONE:SoundInstance.Attenuation.LINEAR,request.x(),request.y(),request.z(),request.relative());this.file=file;this.stream=stream;
        }
        @Override public WeighedSoundEvents resolve(SoundManager manager){
            this.sound=new Sound(file.id(),ConstantFloat.of(1),ConstantFloat.of(1),1,Sound.Type.FILE,stream,false,file.kind()==Kind.STREAMING?64:16);
            return new WeighedSoundEvents(getLocation(),null);
        }
        @Override public CompletableFuture<AudioStream> getStream(SoundBufferLibrary buffers,Sound sound,boolean looping){
            return CompletableFuture.supplyAsync(()->{
                try{
                    var found=Minecraft.getInstance().getResourceManager().getResource(resource(file));
                    var in=found.isPresent()?found.get().open():java.nio.file.Files.newInputStream(file.file());
                    return AudioStreamsB173.open(in);
                }catch(java.io.IOException e){throw new CompletionException(e);}
            },net.minecraft.Util.backgroundExecutor());
        }
    }
}
