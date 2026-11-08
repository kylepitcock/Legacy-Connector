package org.kylepitcock.legacyconnector.vb1_7_3.client;

import java.io.*;
import java.nio.ByteBuffer;
import javax.sound.sampled.*;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.JOrbisAudioStream;

/** AudioMod effects used both OGG and WAV; the host's default decoder accepts only OGG. */
public final class AudioStreamsB173 {
    private AudioStreamsB173(){}
    public static AudioStream open(InputStream input) throws IOException {
        BufferedInputStream in=new BufferedInputStream(input);
        try {
            in.mark(4);byte[] magic=in.readNBytes(4);in.reset();
            if(java.util.Arrays.equals(magic,new byte[]{'O','g','g','S'}))return new JOrbisAudioStream(in);
            AudioInputStream source=AudioSystem.getAudioInputStream(in);
            AudioFormat format=source.getFormat();
            if(format.getChannels()<1||format.getChannels()>2)throw new IOException("Beta audio must be mono or stereo");
            AudioFormat pcm=new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,format.getSampleRate(),16,format.getChannels(),2*format.getChannels(),format.getSampleRate(),false);
            AudioInputStream converted=AudioSystem.getAudioInputStream(pcm,source);
            return new AudioStream(){
                public AudioFormat getFormat(){return pcm;}
                public ByteBuffer read(int size)throws IOException{
                    int frame=pcm.getFrameSize();byte[] bytes=converted.readNBytes(Math.max(frame,(size/frame)*frame));
                    return ByteBuffer.allocateDirect(bytes.length).put(bytes).flip();
                }
                public void close()throws IOException{converted.close();}
            };
        }catch(UnsupportedAudioFileException|IllegalArgumentException e){in.close();throw new IOException("Unsupported Beta audio data",e);}
        catch(IOException|RuntimeException e){in.close();throw e;}
    }
}
