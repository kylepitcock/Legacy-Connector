package legacyconnector.vb1_7_3.net.minecraft.client.sound;

import java.io.File;
import org.kylepitcock.legacyconnector.vb1_7_3.AudioB173;

/** AudioMod's four pools, routed to the host sound engine without a second OpenAL system. */
public class SoundManager {
    public SoundEntry sounds=new SoundEntry(AudioB173.Kind.SOUND);
    public SoundEntry streamingSounds=new SoundEntry(AudioB173.Kind.STREAMING);
    public SoundEntry music=new SoundEntry(AudioB173.Kind.MUSIC);
    public SoundEntry cave=new SoundEntry(AudioB173.Kind.CAVEMUSIC);
    public void loadSound(String name,File file){sounds.loadStatic(name,file);}
    public void loadStreaming(String name,File file){streamingSounds.loadStatic(name,file);}
    public void loadMusic(String name,File file){music.loadStatic(name,file);}
    public void playSound(String name,float x,float y,float z,float volume,float pitch){AudioB173.local(new AudioB173.Request(AudioB173.Kind.SOUND,name,x,y,z,volume,pitch,false));}
    public void playSound(String name,float volume,float pitch){AudioB173.local(new AudioB173.Request(AudioB173.Kind.SOUND,name,0,0,0,Math.min(1,volume)*.25F,pitch,true));}
    public void playStreaming(String name,float x,float y,float z,float volume,float pitch){AudioB173.local(new AudioB173.Request(AudioB173.Kind.STREAMING,name,x,y,z,volume,pitch,false));}
    public void stop(){AudioB173.stop();}
    public void updateMusicVolume(){} // Native sound-category sliders are applied by the host engine.
    public void updateListenerPosition(legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity entity,float tickDelta){} // Host camera owns the listener.
    public void tick(){AudioB173.tick();}
    public void mcc$external(AudioB173.Catalog catalog){
        sounds.mcc$external(catalog.sources(AudioB173.Kind.SOUND));
        streamingSounds.mcc$external(catalog.sources(AudioB173.Kind.STREAMING));
        music.mcc$external(catalog.sources(AudioB173.Kind.MUSIC));
        cave.mcc$external(catalog.sources(AudioB173.Kind.CAVEMUSIC));
    }
}
