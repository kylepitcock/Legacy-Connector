package org.kylepitcock.legacyconnector.vb1_7_3;

import io.netty.buffer.Unpooled;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.Consumer;
import legacyconnector.vb1_7_3.net.minecraft.client.particle.Particle;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
import net.minecraft.nbt.*;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.PacketDistributor;
import org.kylepitcock.legacyconnector.core.*;
import org.kylepitcock.legacyconnector.core.network.*;

/** Original particle requests and graphics-free state transport; no native client classes load on the server. */
public final class ParticlesB173 {
    private static final String FAMILY="b1.7.3-particles";
    private static final Set<String> WARNED=java.util.concurrent.ConcurrentHashMap.newKeySet();
    private static volatile Consumer<Particle> client;
    private static boolean installed;
    private ParticlesB173(){}
    /** Original named World particles retain the existing native mapping. */
    public static net.minecraft.core.particles.ParticleOptions of(String name){
        if(name==null)return null;
        return switch(name){
            case "smoke"->net.minecraft.core.particles.ParticleTypes.SMOKE;
            case "largesmoke"->net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE;
            case "flame"->net.minecraft.core.particles.ParticleTypes.FLAME;
            case "reddust"->net.minecraft.core.particles.DustParticleOptions.REDSTONE;
            case "portal"->net.minecraft.core.particles.ParticleTypes.PORTAL;
            case "explode"->net.minecraft.core.particles.ParticleTypes.POOF;
            case "bubble"->net.minecraft.core.particles.ParticleTypes.BUBBLE;
            case "splash"->net.minecraft.core.particles.ParticleTypes.SPLASH;
            case "lava"->net.minecraft.core.particles.ParticleTypes.LAVA;
            case "note"->net.minecraft.core.particles.ParticleTypes.NOTE;
            case "heart"->net.minecraft.core.particles.ParticleTypes.HEART;
            case "snowballpoof","snowshovel"->net.minecraft.core.particles.ParticleTypes.ITEM_SNOWBALL;
            case "slime"->net.minecraft.core.particles.ParticleTypes.ITEM_SLIME;
            case "dripWater"->net.minecraft.core.particles.ParticleTypes.DRIPPING_WATER;
            case "dripLava"->net.minecraft.core.particles.ParticleTypes.DRIPPING_LAVA;
            case "footstep"->net.minecraft.core.particles.ParticleTypes.CRIT;
            default->null;
        };
    }
    public static synchronized void init(){if(installed)return;installed=true;LegacyNetwork.registerReceiver(FAMILY,(payload,context)->{if(context.flow().isClientbound())context.enqueueWork(()->{Consumer<Particle> sink=client;if(sink!=null)sink.accept(restore(decode(payload.data()),World.mcc$wrap(context.player().level())));});});}
    public static void installClient(Consumer<Particle> value){init();client=value;}
    public static void add(Particle particle){
        init();String reason=unsupported(particle.getClass());
        if(reason!=null){if(WARNED.add(particle.getClass().getName()+reason))org.slf4j.LoggerFactory.getLogger("LegacyConnector/b1.7.3/Particles").warn("Original particle {} rejected: {}",particle.getClass().getName(),reason);return;}
        if(particle.world==null)particle.world=legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$instance().world;
        if(particle.world==null||particle.world.mcc$level()==null)return;
        if(particle.world.mcc$level() instanceof net.minecraft.server.level.ServerLevel level){
            var request=new LegacyPayload(FAMILY,"spawn",0,encode(capture(particle)));
            PacketDistributor.sendToPlayersNear(level,null,particle.x,particle.y,particle.z,64,request);
        }else{Consumer<Particle> sink=client;if(sink!=null)sink.accept(particle);}
    }
    private static final Map<Class<?>,List<Field>> FIELDS=new java.util.concurrent.ConcurrentHashMap<>();
    private static List<Field> fields(Class<?> type){return FIELDS.computeIfAbsent(type,ParticlesB173::inspectFields);}
    private static List<Field> inspectFields(Class<?> type){
        var fields=new ArrayList<Field>();
        for(Class<?> c=type;c!=null&&c!=Particle.class;c=c.getSuperclass())for(Field f:c.getDeclaredFields())if(!Modifier.isStatic(f.getModifiers())){if(!f.getType().isPrimitive()&&f.getType()!=String.class)throw new IllegalArgumentException("Unsupported particle field "+f);f.setAccessible(true);fields.add(f);}
        return fields;
    }
    private static final Map<Class<?>,String> CLOSURES=new java.util.concurrent.ConcurrentHashMap<>();
    public static String unsupported(Class<?> type){String reason=CLOSURES.computeIfAbsent(type,t->{String result=inspect(t);return result==null?"":result;});return reason.isEmpty()?null:reason;}
    private static String inspect(Class<?> type){
        try{
            if(!Particle.class.isAssignableFrom(type))return "not a Beta particle";fields(type);
            for(Class<?> c=type;c!=null&&c!=Particle.class;c=c.getSuperclass()){
                if(c.getName().startsWith("legacyconnector_synth."))return "synthesized particle superclass";
                String[] reason={null};
                try(var in=c.getResourceAsStream("/"+c.getName().replace('.','/')+".class")){
                    if(in==null)return "missing original particle bytes";
                    new org.objectweb.asm.ClassReader(in).accept(new org.objectweb.asm.ClassVisitor(org.objectweb.asm.Opcodes.ASM9){
                        @Override public org.objectweb.asm.MethodVisitor visitMethod(int access,String name,String descriptor,String signature,String[] exceptions){return new org.objectweb.asm.MethodVisitor(org.objectweb.asm.Opcodes.ASM9){
                            @Override public void visitMethodInsn(int opcode,String owner,String called,String desc,boolean itf){if(owner.startsWith("legacyconnector_synth/vb1_7_3/"))reason[0]="unsupported synthesized particle callback "+owner+"."+called;}
                            @Override public void visitFieldInsn(int opcode,String owner,String field,String desc){if(owner.startsWith("legacyconnector_synth/vb1_7_3/"))reason[0]="unsupported synthesized particle field "+field;}
                        };}
                    },org.objectweb.asm.ClassReader.SKIP_DEBUG|org.objectweb.asm.ClassReader.SKIP_FRAMES);
                }
                if(reason[0]!=null)return reason[0];
            }
            return null;
        }catch(java.io.IOException|RuntimeException|LinkageError e){return "cannot inspect original particle: "+e.getClass().getSimpleName();}
    }
    public static CompoundTag capture(Particle p){
        CompoundTag tag=new CompoundTag();tag.putString("class",p.getClass().getName());
        tag.putDouble("x",p.x);tag.putDouble("y",p.y);tag.putDouble("z",p.z);tag.putDouble("px",p.prevX);tag.putDouble("py",p.prevY);tag.putDouble("pz",p.prevZ);
        tag.putDouble("vx",p.velocityX);tag.putDouble("vy",p.velocityY);tag.putDouble("vz",p.velocityZ);tag.putFloat("width",p.width);tag.putFloat("height",p.height);tag.putFloat("eye",p.standingEyeHeight);
        tag.putInt("texture",p.textureId);tag.putInt("age",p.particleAge);tag.putInt("maxAge",p.maxParticleAge);tag.putFloat("u",p.prevU);tag.putFloat("v",p.prevV);tag.putFloat("scale",p.scale);tag.putFloat("gravity",p.gravityStrength);tag.putFloat("r",p.red);tag.putFloat("g",p.green);tag.putFloat("b",p.blue);tag.putBoolean("ground",p.onGround);
        CompoundTag extra=new CompoundTag();try{for(Field f:fields(p.getClass())){String key=f.getDeclaringClass().getName()+"#"+f.getName();Object value=f.get(p);if(value instanceof String s)extra.putString(key,s);else if(value instanceof Boolean b)extra.putBoolean(key,b);else if(value instanceof Character c)extra.putInt(key,c);else if(value instanceof Number n){if(f.getType()==float.class||f.getType()==double.class)extra.putDouble(key,n.doubleValue());else extra.putLong(key,n.longValue());}}}catch(IllegalAccessException e){throw new IllegalStateException(e);}tag.put("fields",extra);return tag;
    }
    public static Particle restore(CompoundTag tag,World world){
        String name=tag.getString("class");Class<?> type;
        try{type=Class.forName(name,false,Thread.currentThread().getContextClassLoader());}catch(ClassNotFoundException e){throw new IllegalArgumentException("Unknown original particle",e);}
        boolean owned=LegacyMods.ordered().stream().filter(m->PlatformB173.VERSION.equals(m.version)&&m.mainClass!=null).anyMatch(m->java.util.Objects.equals(m.mainClass.getProtectionDomain().getCodeSource(),type.getProtectionDomain().getCodeSource()));
        if(!owned||unsupported(type)!=null)throw new IllegalArgumentException("Particle class is not an admitted installed original");
        Particle p=(Particle)org.kylepitcock.legacyconnector.core.synth.Synth.blank(name);if(p==null)throw new IllegalArgumentException("Cannot allocate original particle");p.world=world;p.mcc$initializeRandom(System.nanoTime());
        p.width=tag.getFloat("width");p.height=tag.getFloat("height");p.standingEyeHeight=tag.getFloat("eye");p.setPosition(tag.getDouble("x"),tag.getDouble("y"),tag.getDouble("z"));p.prevX=tag.getDouble("px");p.prevY=tag.getDouble("py");p.prevZ=tag.getDouble("pz");p.velocityX=tag.getDouble("vx");p.velocityY=tag.getDouble("vy");p.velocityZ=tag.getDouble("vz");
        p.textureId=tag.getInt("texture");p.particleAge=tag.getInt("age");p.maxParticleAge=tag.getInt("maxAge");p.prevU=tag.getFloat("u");p.prevV=tag.getFloat("v");p.scale=tag.getFloat("scale");p.gravityStrength=tag.getFloat("gravity");p.red=tag.getFloat("r");p.green=tag.getFloat("g");p.blue=tag.getFloat("b");p.onGround=tag.getBoolean("ground");
        CompoundTag extra=tag.getCompound("fields");try{for(Field f:fields(type)){String k=f.getDeclaringClass().getName()+"#"+f.getName();Class<?> t=f.getType();if(t==String.class)f.set(p,extra.getString(k));else if(t==boolean.class)f.setBoolean(p,extra.getBoolean(k));else if(t==char.class)f.setChar(p,(char)extra.getInt(k));else if(t==float.class)f.setFloat(p,(float)extra.getDouble(k));else if(t==double.class)f.setDouble(p,extra.getDouble(k));else if(t==long.class)f.setLong(p,extra.getLong(k));else if(t==int.class)f.setInt(p,(int)extra.getLong(k));else if(t==short.class)f.setShort(p,(short)extra.getLong(k));else if(t==byte.class)f.setByte(p,(byte)extra.getLong(k));}}catch(IllegalAccessException e){throw new IllegalStateException(e);}return p;
    }
    public static byte[] encode(CompoundTag tag){var b=new FriendlyByteBuf(Unpooled.buffer());try{b.writeNbt(tag);byte[] bytes=new byte[b.readableBytes()];b.readBytes(bytes);return bytes;}finally{b.release();}}
    public static CompoundTag decode(byte[] bytes){var b=new FriendlyByteBuf(Unpooled.wrappedBuffer(bytes));try{return b.readNbt();}finally{b.release();}}
}
