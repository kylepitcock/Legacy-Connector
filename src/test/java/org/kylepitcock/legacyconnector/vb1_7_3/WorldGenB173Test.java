package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Random;
import legacyconnector.vb1_7_3.net.minecraft.src.BaseMod;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
import org.junit.jupiter.api.Test;

class WorldGenB173Test {
    public static class Defaults extends BaseMod {@Override public String Version(){return "fixture";}}
    public static class Surface extends Defaults {
        int calls,x,z;World world;Random random;
        @Override public void GenerateSurface(World world,Random random,int x,int z){calls++;this.world=world;this.random=random;this.x=x;this.z=z;}
    }
    public static class Inherited extends Surface {}
    public static class Nether extends Defaults {
        int calls,x,z;
        @Override public void GenerateNether(World world,Random random,int x,int z){calls++;this.x=x;this.z=z;}
    }
    @Test void defaultsDoNotRegisterAndInheritedOverridesDo(){
        assertFalse(WorldGenB173.overrides(Defaults.class,false));assertFalse(WorldGenB173.overrides(Defaults.class,true));
        assertTrue(WorldGenB173.overrides(Surface.class,false));assertFalse(WorldGenB173.overrides(Surface.class,true));
        assertTrue(WorldGenB173.overrides(Inherited.class,false));assertTrue(WorldGenB173.overrides(Nether.class,true));
    }
    @Test void dispatchUsesOnlyMatchingDimensionAndBlockOrigins(){
        var surface=new Surface();var nether=new Nether();var random=new Random(5);
        WorldGenB173.dispatch(surface,0,null,random,-3,7);
        assertEquals(1,surface.calls);assertEquals(-48,surface.x);assertEquals(112,surface.z);assertSame(random,surface.random);
        WorldGenB173.dispatch(surface,-1,null,random,8,9);WorldGenB173.dispatch(surface,1,null,random,8,9);
        assertEquals(1,surface.calls);
        WorldGenB173.dispatch(nether,0,null,random,8,9);WorldGenB173.dispatch(nether,1,null,random,8,9);assertEquals(0,nether.calls);
        WorldGenB173.dispatch(nether,-1,null,random,8,-9);assertEquals(1,nether.calls);assertEquals(128,nether.x);assertEquals(-144,nether.z);
    }
    @Test void exactBetaSignedDivisionSeedsIncludeNegativeSaltsAndChunks(){
        // Audited ModLoader.PopulateChunk uses nextLong()/2*2+1, unlike FML's shifted salts.
        assertEquals(-4962768465676381896L,WorldGenB173.seededRandom(0,0,0).nextLong());
        assertEquals(2014880941284176763L,WorldGenB173.seededRandom(1,-2,3).nextLong());
        assertEquals(-330439659629180781L,WorldGenB173.seededRandom(-1,13,-7).nextLong());
        assertEquals(-8938490438770598892L,WorldGenB173.seededRandom(42,-9,-11).nextLong());
        assertEquals(-2788771387660887069L,WorldGenB173.seededRandom(Long.MIN_VALUE,-1,1).nextLong());
    }
    @Test void featureAndFallbackCallsShareOneStreamWithoutCrossChunkOrRegionReuse(){
        var region=new Object();var first=WorldGenB173.chunkRandom(region,1,-2,3);
        assertEquals(2014880941284176763L,first.nextLong());
        var second=WorldGenB173.chunkRandom(region,1,-2,3);assertSame(first,second);
        assertEquals(6833502985283993525L,second.nextLong(),"later mods consume the stream after earlier mods");
        assertNotSame(first,WorldGenB173.chunkRandom(region,1,-2,4));
        assertEquals(2014880941284176763L,WorldGenB173.chunkRandom(new Object(),1,-2,3).nextLong(),"another dimension/region starts its own population stream");
    }
    @Test void originalCallbackExceptionsAreNotSwallowed(){
        var expected=new IllegalStateException("original failure");
        var mod=new Defaults(){@Override public void GenerateSurface(World world,Random random,int x,int z){throw expected;}};
        assertSame(expected,assertThrows(IllegalStateException.class,()->WorldGenB173.dispatch(mod,0,null,new Random(),0,0)));
    }
    /** Runtime methods remain callable even if an optional, unrelated signature cannot link. */
    private static final class OptionalSignatureLoader extends ClassLoader {
        OptionalSignatureLoader(){super(WorldGenB173Test.class.getClassLoader());}
        @Override public java.io.InputStream getResourceAsStream(String name){return name.startsWith("fixture/worldgen/")?null:super.getResourceAsStream(name);}
        Class<? extends BaseMod> make(String simple,Class<? extends BaseMod> parent,boolean surface,boolean nether){
            return make(simple,parent,surface,nether,true);
        }
        Class<? extends BaseMod> make(String simple,Class<? extends BaseMod> parent,boolean surface,boolean nether,boolean publicClass){
            String name="fixture/worldgen/"+simple,superName=org.objectweb.asm.Type.getInternalName(parent);
            var writer=new org.objectweb.asm.ClassWriter(0);int pub=org.objectweb.asm.Opcodes.ACC_PUBLIC;
            writer.visit(org.objectweb.asm.Opcodes.V1_6,publicClass?pub:0,name,null,superName,null);
            var init=writer.visitMethod(pub,"<init>","()V",null,null);init.visitCode();init.visitVarInsn(org.objectweb.asm.Opcodes.ALOAD,0);init.visitMethodInsn(org.objectweb.asm.Opcodes.INVOKESPECIAL,superName,"<init>","()V",false);init.visitInsn(org.objectweb.asm.Opcodes.RETURN);init.visitMaxs(1,1);init.visitEnd();
            var version=writer.visitMethod(pub,"Version","()Ljava/lang/String;",null,null);version.visitCode();version.visitLdcInsn("optional-signature fixture");version.visitInsn(org.objectweb.asm.Opcodes.ARETURN);version.visitMaxs(1,1);version.visitEnd();
            var unrelated=writer.visitMethod(pub,"optionalMultiplayerCommand","(Lfixture/worldgen/AbsentCommandOutput;)V",null,null);unrelated.visitCode();unrelated.visitInsn(org.objectweb.asm.Opcodes.RETURN);unrelated.visitMaxs(0,2);unrelated.visitEnd();
            writer.visitField(pub|org.objectweb.asm.Opcodes.ACC_STATIC,"calls","I",null,null).visitEnd();
            String descriptor=org.objectweb.asm.Type.getMethodDescriptor(org.objectweb.asm.Type.VOID_TYPE,org.objectweb.asm.Type.getType(World.class),org.objectweb.asm.Type.getType(Random.class),org.objectweb.asm.Type.INT_TYPE,org.objectweb.asm.Type.INT_TYPE);
            for(String callback:new String[]{"GenerateSurface","GenerateNether"})if(callback.equals("GenerateSurface")?surface:nether){
                var method=writer.visitMethod(pub,callback,descriptor,null,null);method.visitCode();method.visitFieldInsn(org.objectweb.asm.Opcodes.GETSTATIC,name,"calls","I");method.visitInsn(org.objectweb.asm.Opcodes.ICONST_1);method.visitInsn(org.objectweb.asm.Opcodes.IADD);method.visitFieldInsn(org.objectweb.asm.Opcodes.PUTSTATIC,name,"calls","I");method.visitInsn(org.objectweb.asm.Opcodes.RETURN);method.visitMaxs(2,5);method.visitEnd();
            }
            writer.visitEnd();byte[] bytes=writer.toByteArray();return defineClass(name.replace('/','.'),bytes,0,bytes.length).asSubclass(BaseMod.class);
        }
    }
    @Test void missingUnrelatedMethodTypeCannotBlockRelevantRuntimeOverrides()throws Exception {
        var loader=new OptionalSignatureLoader();var both=loader.make("Both",BaseMod.class,true,true);
        assertThrows(NoClassDefFoundError.class,()->both.getMethod("GenerateSurface",World.class,Random.class,int.class,int.class),"old reflective enumeration reproduces optional-type linkage failure");
        assertNull(both.getResourceAsStream("Both.class"),"detection must not depend on remapped class resources");
        assertTrue(WorldGenB173.overrides(both,false));assertTrue(WorldGenB173.overrides(both,true));
        BaseMod mod=both.getConstructor().newInstance();WorldGenB173.dispatch(mod,0,null,new Random(),0,0);WorldGenB173.dispatch(mod,-1,null,new Random(),0,0);assertEquals(2,both.getField("calls").getInt(null));
        var inherited=loader.make("Inherited",both,false,false);assertTrue(WorldGenB173.overrides(inherited,false));assertTrue(WorldGenB173.overrides(inherited,true));
        WorldGenB173.dispatch(inherited.getConstructor().newInstance(),0,null,new Random(),0,0);assertEquals(3,both.getField("calls").getInt(null));
        var defaults=loader.make("Defaults",BaseMod.class,false,false);assertFalse(WorldGenB173.overrides(defaults,false));assertFalse(WorldGenB173.overrides(defaults,true));
        var hidden=loader.make("PackagePrivate",BaseMod.class,true,false,false);assertTrue(WorldGenB173.overrides(hidden,false));assertFalse(WorldGenB173.overrides(hidden,true));
    }
}
