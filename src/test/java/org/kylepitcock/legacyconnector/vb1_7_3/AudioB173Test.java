package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.Supplier;
import java.util.zip.ZipFile;
import javax.sound.sampled.*;
import legacyconnector.vb1_7_3.net.minecraft.client.sound.SoundEntry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.kylepitcock.legacyconnector.vb1_7_3.AudioB173.*;
import org.kylepitcock.legacyconnector.vb1_7_3.client.AudioStreamsB173;

class AudioB173Test {
    @TempDir Path temp;

    @Test void betaNamesAndStreamingDigits() throws Exception {
        assertEquals("mob.bird.call",SoundEntry.key("mob/bird/call12.ogg",true));
        assertEquals("record13",SoundEntry.key("record13.ogg",false));
        assertEquals("first",SoundEntry.key("first.extra/call1.ogg",true));
        var pool=new SoundEntry();
        pool.loadStatic("mob/bird/call1.ogg",temp.resolve("a.ogg").toFile());
        pool.loadStatic("mob/bird/call2.ogg",temp.resolve("b.ogg").toFile());
        assertEquals(2,pool.weightedSoundSet.get("mob.bird.call").size());
        assertNotNull(pool.get("mob.bird.call"));assertNull(pool.get("missing"));
        assertEquals(2,pool.loadedSoundCount);
    }

    @Test void portableInstanceFoldersAndResourcePackRefresh() throws Exception {
        Path instance=temp.resolve("portable-instance");
        Path effect=put(instance,"sound/mob/test/call1.ogg",new byte[]{1,2,3});
        put(instance,"sound/mob/test/call2.wav",new byte[]{4,5});
        put(instance,"streaming/record13.ogg",new byte[]{6});
        put(instance,"music/tune1.ogg",new byte[]{7});
        put(instance,"cavemusic/deep1.ogg",new byte[]{8});
        put(instance,"sound/.hidden/no.ogg",new byte[]{9});
        put(instance,"sound/readme.txt",new byte[]{10});
        Catalog before=AudioB173.scan(instance);
        assertEquals(5,before.entries().size());
        assertEquals(2,before.variants(Kind.SOUND,"mob.test.call").size());
        assertEquals(1,before.variants(Kind.STREAMING,"record13").size());
        Map<String,Supplier<byte[]>> pack=new HashMap<>();AudioB173.contribute(before,pack);
        assertEquals(6,pack.size());
        Source source=before.variants(Kind.SOUND,"mob.test.call").getFirst();
        assertArrayEquals(Files.readAllBytes(source.file()),pack.get(source.packPath()).get());
        var json=com.google.gson.JsonParser.parseString(new String(pack.get("assets/legacyconnector_b173_audio/sounds.json").get(),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
        assertEquals(2,json.getAsJsonObject(AudioB173.identifier(Kind.SOUND,"mob.test.call").getPath()).getAsJsonArray("sounds").size());
        Files.delete(effect);
        Catalog after=AudioB173.scan(instance);
        assertEquals(1,after.variants(Kind.SOUND,"mob.test.call").size());
        assertTrue(AudioB173.scan(temp.resolve("other-instance")).entries().isEmpty());
        assertNotEquals(AudioB173.identifier(Kind.SOUND,"A"),AudioB173.identifier(Kind.SOUND,"a"));
    }

    @Test void explicitFilesOutsideInstanceAndPlaybackDispatch() throws Exception {
        Path outside=Files.write(temp.resolve("outside.wav"),new byte[]{1});
        List<Request> heard=new ArrayList<>();
        AudioB173.installClient(new AudioB173.Client(){public void play(Request r){heard.add(r);}public void stop(){}public void tick(){}});
        try {
            var manager=new legacyconnector.vb1_7_3.net.minecraft.client.sound.SoundManager();
            manager.loadSound("external/test1.wav",outside.toFile());
            assertEquals(outside.toAbsolutePath(),AudioB173.catalog().variants(Kind.SOUND,"external.test").getFirst().file());
            manager.playSound("external.test",1,2,3,0.8F,1.2F);
            manager.playSound("external.test",4,1);
            manager.playStreaming(null,1,2,3,0,1);
            assertEquals(3,heard.size());assertEquals("external.test",heard.getFirst().name());
            assertEquals(0.25F,heard.get(1).volume());assertTrue(heard.get(1).relative());
            for(Request request:heard)assertEquals(request,AudioB173.decode(AudioB173.encode(request)));
        } finally {AudioB173.installClient(null);}
    }

    @Test void wavDecoderProducesPcmAndRejectsBadData() throws Exception {
        var pcm=new AudioFormat(22050,16,1,true,false);
        byte[] samples=new byte[]{0,1,2,3,4,5,6,7};var wav=new ByteArrayOutputStream();
        AudioSystem.write(new AudioInputStream(new ByteArrayInputStream(samples),pcm,4),AudioFileFormat.Type.WAVE,wav);
        try(var stream=AudioStreamsB173.open(new ByteArrayInputStream(wav.toByteArray()))){
            assertEquals(22050,stream.getFormat().getSampleRate());assertEquals(16,stream.getFormat().getSampleSizeInBits());
            var output=stream.read(4096);byte[] decoded=new byte[output.remaining()];output.get(decoded);assertArrayEquals(samples,decoded);
        }
        assertThrows(IOException.class,()->AudioStreamsB173.open(new ByteArrayInputStream(new byte[]{0,1,2,3})));
    }

    @Test void nativeSoundInstancesPreserveParametersAndChannelChoice() throws Exception {
        // Same offline bootstrap as src/devcheck: registry-backed FloatProvider codecs need vanilla registries.
        var dist=net.neoforged.fml.loading.FMLLoader.class.getDeclaredField("dist");dist.setAccessible(true);
        dist.set(null,net.neoforged.api.distmarker.Dist.CLIENT);
        net.neoforged.fml.loading.LoadingModList.of(List.of(),List.of(),List.of(),List.of(),Map.of());
        net.minecraft.SharedConstants.tryDetectVersion();net.minecraft.server.Bootstrap.bootStrap();
        for(String name:List.of("effect1.ogg","effect1.wav")){
            var source=new Source(Kind.SOUND,name,"effect",temp.resolve(name));
            var request=new Request(Kind.SOUND,"effect",10,20,30,.5F,1.2F,false);
            var sound=new org.kylepitcock.legacyconnector.vb1_7_3.client.AudioClientB173.Instance(request,source,source.streaming());
            assertNotNull(sound.resolve(null));assertEquals(source.id(),sound.getSound().getLocation());
            assertEquals(name.endsWith(".wav"),sound.getSound().shouldStream());
            assertEquals(.5F,sound.getVolume());assertEquals(1.2F,sound.getPitch());
            assertEquals(10,sound.getX());assertFalse(sound.isRelative());
        }
        var record=new Source(Kind.STREAMING,"record13.ogg","record13",temp.resolve("record13.ogg"));
        assertTrue(record.streaming());
        var stream=new org.kylepitcock.legacyconnector.vb1_7_3.client.AudioClientB173.Instance(new Request(Kind.STREAMING,"record13",0,0,0,4,2,false),record,true);
        stream.resolve(null);assertEquals(.5F,stream.getVolume());assertEquals(1,stream.getPitch());assertEquals(64,stream.getSound().getAttenuationDistance());
    }

    @Test void actualArchiveOggAndWavAssetsOneModAtATime() throws Exception {
        Path archive=Path.of("../Minecraft Beta 1.7.3 Mods Archive - 1000+ Mods").toAbsolutePath().normalize();
        org.junit.jupiter.api.Assumptions.assumeTrue(Files.isDirectory(archive),"Local user-owned mod archive is required");
        checkArchive(archive,"PortalGun_v5_b173.zip","Client/place in resources/mod/",239);
        checkArchive(archive,"DrZharks_MoCreatures_Mod_v2.12.2_b1.7.3.zip","resources/mod/",168);
        checkArchive(archive,"Aether_1.7.3_v1.01.zip","Resources/mod/",16);
    }
    private void checkArchive(Path archive,String filename,String prefix,int expected) throws Exception {
        Path zipPath;
        try(var walk=Files.walk(archive)){zipPath=walk.filter(p->p.getFileName().toString().equals(filename)).findFirst().orElseThrow();}
        // Deliberately outside AppData: the same layout as a portable/Prism game directory.
        Path instance=Files.createTempDirectory(Path.of("build"),"b173-audio-").toAbsolutePath();int decoded=0;
        try(var zip=new ZipFile(zipPath.toFile())){
            for(var entry:Collections.list(zip.entries())){
                String name=entry.getName();if(entry.isDirectory()||!name.startsWith(prefix))continue;
                String relative=name.substring(prefix.length());
                if(!(relative.endsWith(".ogg")||relative.endsWith(".wav")))continue;
                if(Arrays.stream(Kind.values()).noneMatch(k->relative.startsWith(k.name().toLowerCase(Locale.ROOT)+"/")))continue;
                try(var input=zip.getInputStream(entry)){put(instance,relative,input.readAllBytes());}
            }
        }
        Catalog catalog=AudioB173.scan(instance);assertEquals(expected,catalog.entries().size(),filename);
        for(Source source:catalog.entries()){
            // This archived Aether file contains MP3 frames despite its .wav suffix; original CodecWav also rejects it.
            if(filename.equals("Aether_1.7.3_v1.01.zip") && source.name().equals("mob/aether/aerwhale/aerwhale_cal555l2.wav")){
                assertThrows(IOException.class,()->AudioStreamsB173.open(Files.newInputStream(source.file())));continue;
            }
            try(var stream=AudioStreamsB173.open(Files.newInputStream(source.file()))){
                assertTrue(stream.getFormat().getSampleRate()>0,source.name());
                assertTrue(stream.read(4096).hasRemaining(),source.name());
                while(stream.read(16384).hasRemaining()){} decoded++;
            }catch(IOException e){throw new IOException("Cannot decode "+source.file(),e);}
        }
        assertEquals(filename.startsWith("Aether_")?15:expected,decoded);
        System.out.println("AudioMod actual assets: "+filename+" discovered and decoded "+decoded+" files from "+instance);
    }
    private Path put(Path instance,String relative,byte[] bytes)throws IOException {
        Path path=instance.resolve("resources/mod").resolve(relative);Files.createDirectories(path.getParent());return Files.write(path,bytes);
    }
}
