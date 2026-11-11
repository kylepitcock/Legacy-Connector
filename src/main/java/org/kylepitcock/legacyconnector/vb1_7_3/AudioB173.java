package org.kylepitcock.legacyconnector.vb1_7_3;

import com.google.gson.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.function.Supplier;
import legacyconnector.vb1_7_3.net.minecraft.client.sound.SoundEntry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.PackType;
import org.kylepitcock.legacyconnector.core.network.*;
import org.kylepitcock.legacyconnector.core.pack.GeneratedPack;

/** Instance-relative AudioMod resources and a clientbound sound request (no file paths cross the network). */
public final class AudioB173 implements GeneratedPack.Contributor {
    public enum Kind { SOUND, STREAMING, MUSIC, CAVEMUSIC }
    public record Request(Kind kind,String name,double x,double y,double z,float volume,float pitch,boolean relative) {}
    public record Source(Kind kind,String name,String event,Path file) {
        public ResourceLocation id(){return identifier(kind,"file/"+name);}
        public boolean streaming(){return kind!=Kind.SOUND || !name.toLowerCase(Locale.ROOT).endsWith(".ogg");}
        public String packPath(){return "assets/"+id().getNamespace()+"/sounds/"+id().getPath()+".ogg";}
    }
    public record Catalog(List<Source> entries) {
        public Catalog { entries=List.copyOf(entries); }
        public List<Source> sources(Kind kind){return entries.stream().filter(s->s.kind()==kind).toList();}
        public List<Source> variants(Kind kind,String event){return entries.stream().filter(s->s.kind()==kind && s.event().equals(event)).toList();}
    }
    public interface Client { void play(Request request); void stop(); void tick(); }
    private static final String FAMILY="b1.7.3-audio";
    private static final org.slf4j.Logger LOG=org.slf4j.LoggerFactory.getLogger("LegacyConnector/b1.7.3/Audio");
    private static final Map<String,Source> EXPLICIT=new LinkedHashMap<>();
    private static final Set<String> WARNED=java.util.concurrent.ConcurrentHashMap.newKeySet();
    private static volatile Catalog catalog=new Catalog(List.of());
    private static volatile Client client;

    public static void init(){LegacyNetwork.registerReceiver(FAMILY,(payload,context)->{
        if(!context.flow().isClientbound())return;
        Request request=decode(payload.data());context.enqueueWork(()->local(request));
    });}
    public static void installClient(Client value){client=value;}
    public static Catalog catalog(){return catalog;}
    /** Beta vanilla names, consulted only after the AudioMod catalog has no variants. */
    public static String fallbackSoundName(String name){return switch(name){
        case "mob.slime"->"entity.slime.squish";
        case "mob.slimeattack"->"entity.slime.attack";
        default->name;
    };}
    public static void local(Request request){Client value=client;if(value!=null)value.play(request);}
    public static void stop(){Client value=client;if(value!=null)value.stop();}
    public static void tick(){Client value=client;if(value!=null)value.tick();}
    public static ResourceLocation identifier(Kind kind,String name){
        return ResourceLocation.fromNamespaceAndPath("legacyconnector_b173_audio",kind.name().toLowerCase(Locale.ROOT)+"/"+HexFormat.of().formatHex(name.getBytes(StandardCharsets.UTF_8)));
    }
    public static synchronized void register(Kind kind,String name,Path file,boolean random){
        if(!supported(file))return;
        Source source=new Source(kind,name,SoundEntry.key(name,random),file.toAbsolutePath().normalize());
        EXPLICIT.put(kind+":"+name,source);
        var entries=new ArrayList<>(catalog.entries());entries.removeIf(s->s.kind()==kind && s.name().equals(name));entries.add(source);catalog=new Catalog(entries);
    }
    private static boolean supported(Path path){
        String lower=path.toString().toLowerCase(Locale.ROOT);
        if(lower.endsWith(".ogg")||lower.endsWith(".wav"))return true;
        if((lower.endsWith(".xm")||lower.endsWith(".s3m")||lower.endsWith(".mod")||lower.endsWith(".mus"))&&WARNED.add(path.toString()))LOG.warn("Beta sound codec is not implemented for {} (OGG and WAV are supported)",path);
        return false;
    }
    /** No APPDATA/user.home lookup: folders always belong to this instance. */
    public static Catalog scan(Path gameDirectory){
        List<Source> entries=new ArrayList<>();
        for(Kind kind:Kind.values()){
            Path root=gameDirectory.resolve("resources/mod/"+kind.name().toLowerCase(Locale.ROOT));
            if(!Files.isDirectory(root))continue;
            try(var walk=Files.walk(root)){
                for(Path file:walk.filter(Files::isRegularFile).sorted().toList()){
                    String name=root.relativize(file).toString().replace('\\','/');
                    if(Arrays.stream(name.split("/")).anyMatch(n->n.startsWith("."))||!supported(file))continue;
                    entries.add(new Source(kind,name,SoundEntry.key(name,kind!=Kind.STREAMING),file.toAbsolutePath().normalize()));
                }
            }catch(java.io.IOException e){LOG.warn("Cannot read Beta audio folder {}",root,e);}
        }
        return new Catalog(entries);
    }
    public static synchronized Catalog reload(Path gameDirectory){
        Map<String,Source> all=new LinkedHashMap<>();
        for(Source source:scan(gameDirectory).entries())all.put(source.kind()+":"+source.name(),source);
        EXPLICIT.values().stream().filter(s->Files.isRegularFile(s.file())).forEach(s->all.put(s.kind()+":"+s.name(),s));
        catalog=new Catalog(new ArrayList<>(all.values()));
        legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$instance().soundManager.mcc$external(catalog);
        return catalog;
    }
    @Override public void contribute(PackType type,Map<String,Supplier<byte[]>> out){
        if(type!=PackType.CLIENT_RESOURCES)return;
        contribute(reload(net.neoforged.fml.loading.FMLPaths.GAMEDIR.get()),out);
    }
    public static void contribute(Catalog values,Map<String,Supplier<byte[]>> out){
        JsonObject events=new JsonObject();
        for(Source source:values.entries()){
            out.put(source.packPath(),()->{try{return Files.readAllBytes(source.file());}catch(java.io.IOException e){throw new java.io.UncheckedIOException(e);}});
            String key=identifier(source.kind(),source.event()).getPath();
            JsonObject event=events.has(key)?events.getAsJsonObject(key):new JsonObject();
            if(!events.has(key)){event.add("sounds",new JsonArray());events.add(key,event);}
            JsonObject sound=new JsonObject();sound.addProperty("name",source.id().toString());sound.addProperty("stream",source.streaming());
            event.getAsJsonArray("sounds").add(sound);
        }
        if(!events.isEmpty()){byte[] bytes=events.toString().getBytes(StandardCharsets.UTF_8);out.put("assets/legacyconnector_b173_audio/sounds.json",()->bytes);}
    }
    public static byte[] encode(Request r){
        FriendlyByteBuf buf=new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        try{buf.writeEnum(r.kind()).writeBoolean(r.name()!=null);if(r.name()!=null)buf.writeUtf(r.name(),1024);
            buf.writeDouble(r.x()).writeDouble(r.y()).writeDouble(r.z()).writeFloat(r.volume()).writeFloat(r.pitch()).writeBoolean(r.relative());
            byte[] bytes=new byte[buf.readableBytes()];buf.readBytes(bytes);return bytes;
        }finally{buf.release();}
    }
    public static Request decode(byte[] data){
        FriendlyByteBuf buf=new FriendlyByteBuf(io.netty.buffer.Unpooled.wrappedBuffer(data));
        try{return new Request(buf.readEnum(Kind.class),buf.readBoolean()?buf.readUtf(1024):null,buf.readDouble(),buf.readDouble(),buf.readDouble(),buf.readFloat(),buf.readFloat(),buf.readBoolean());}
        finally{buf.release();}
    }
    public static void play(net.minecraft.world.level.Level level,Request request){
        if(level instanceof ServerLevel server){
            var payload=new LegacyPayload(FAMILY,"play",0,encode(request));
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayersNear(server,null,request.x(),request.y(),request.z(),Math.max(16,16*request.volume()),payload);
        }else local(request);
    }
}
