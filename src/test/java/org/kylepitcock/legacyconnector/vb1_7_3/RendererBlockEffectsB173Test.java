package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/** Synthetic original-mod bytecode is inspected, never instantiated or executed. */
class RendererBlockEffectsB173Test implements Opcodes {
    private static final String PREFIX="legacyconnector_b173/fixture/";
    private static final String RENDERER="legacyconnector/vb1_7_3/net/minecraft/client/render/entity/EntityRenderer";
    private static final String ENTRY="(Llegacyconnector/vb1_7_3/net/minecraft/entity/Entity;DDDFF)V";
    private static final class Classes extends ClassLoader {
        final Map<String,byte[]> bytes=new HashMap<>();
        Classes(){super(RendererBlockEffectsB173Test.class.getClassLoader());}
        @Override protected Class<?> findClass(String name)throws ClassNotFoundException {
            var data=bytes.get(name.replace('.','/')+".class");if(data==null)throw new ClassNotFoundException(name);return defineClass(name,data,0,data.length);
        }
        @Override public InputStream getResourceAsStream(String name){var data=bytes.get(name);return data==null?super.getResourceAsStream(name):new ByteArrayInputStream(data);}
        boolean effects(String name)throws ReflectiveOperationException {
            var field=RendererBlockEffectsB173.class.getDeclaredField("EFFECTS");field.setAccessible(true);
            return (Boolean)((ClassValue<?>)field.get(null)).get(Class.forName(PREFIX.replace('/','.')+name,false,this));
        }
    }
    private static ClassWriter begin(String name,String parent){
        var writer=new ClassWriter(ClassWriter.COMPUTE_MAXS);writer.visit(V1_8,ACC_PUBLIC|ACC_ABSTRACT,PREFIX+name,null,parent,null);return writer;
    }
    private static void finish(Classes classes,String name,ClassWriter writer){writer.visitEnd();classes.bytes.put(PREFIX+name+".class",writer.toByteArray());}
    private static void method(ClassWriter writer,String name,String descriptor,int access,Consumer<MethodVisitor> body){
        var method=writer.visitMethod(access,name,descriptor,null,null);method.visitCode();body.accept(method);method.visitInsn(RETURN);method.visitMaxs(0,0);method.visitEnd();
    }
    private static void call(MethodVisitor method,String owner,String name){method.visitMethodInsn(INVOKESTATIC,PREFIX+owner,name,"()V",false);}
    private static void mutation(MethodVisitor method){
        method.visitInsn(ACONST_NULL);for(int i=0;i<4;i++)method.visitInsn(ICONST_0);
        method.visitMethodInsn(INVOKEVIRTUAL,"legacyconnector/vb1_7_3/net/minecraft/world/World","setBlock","(IIII)Z",false);method.visitInsn(POP);
    }
    private static void fieldWrite(MethodVisitor method,String owner){
        method.visitInsn(ACONST_NULL);method.visitInsn(ICONST_0);
        method.visitFieldInsn(PUTFIELD,owner,"value","I");
    }
    private static void entity(Classes classes,String name,String parent){
        var writer=begin(name,parent);writer.visitField(ACC_PUBLIC,"value","I",null,null).visitEnd();
        writer.visitField(ACC_PUBLIC|ACC_STATIC,"staticValue","I",null,null).visitEnd();finish(classes,name,writer);
    }
    @Test void followsTransitiveEntityFieldWritesAndInheritedOwners()throws ReflectiveOperationException {
        var classes=new Classes();
        entity(classes,"Actor","legacyconnector/vb1_7_3/net/minecraft/entity/Entity");
        finish(classes,"ChildActor",begin("ChildActor",PREFIX+"Actor"));
        var renderer=begin("EntityEffects",RENDERER);
        method(renderer,"render",ENTRY,ACC_PUBLIC,m->call(m,"EntityFirst","reachable"));finish(classes,"EntityEffects",renderer);
        var first=begin("EntityFirst","java/lang/Object");method(first,"reachable","()V",ACC_PUBLIC|ACC_STATIC,m->call(m,"EntitySecond","mutate"));finish(classes,"EntityFirst",first);
        var second=begin("EntitySecond","java/lang/Object");method(second,"mutate","()V",ACC_PUBLIC|ACC_STATIC,m->fieldWrite(m,PREFIX+"ChildActor"));finish(classes,"EntitySecond",second);
        assertTrue(classes.effects("EntityEffects"),"inherited entity fields remain simulation effects through helper calls");
        assertTrue(classes.effects("EntityEffects"),"entity effect result is cached");
    }
    @Test void ignoresVisualFieldsReadsStaticsAndUnreachableEntityWrites()throws ReflectiveOperationException {
        var classes=new Classes();entity(classes,"VisualActor","legacyconnector/vb1_7_3/net/minecraft/entity/Entity");
        entity(classes,"ModelState","java/lang/Object");
        var renderer=begin("VisualFields",RENDERER);renderer.visitField(ACC_PUBLIC,"value","I",null,null).visitEnd();
        method(renderer,"render",ENTRY,ACC_PUBLIC,m->{fieldWrite(m,PREFIX+"VisualFields");call(m,"VisualHelper","visual");});finish(classes,"VisualFields",renderer);
        var helper=begin("VisualHelper","java/lang/Object");
        method(helper,"visual","()V",ACC_PUBLIC|ACC_STATIC,m->{
            fieldWrite(m,PREFIX+"ModelState");
            m.visitInsn(ACONST_NULL);m.visitFieldInsn(GETFIELD,PREFIX+"VisualActor","value","I");m.visitInsn(POP);
            m.visitFieldInsn(GETSTATIC,PREFIX+"VisualActor","staticValue","I");m.visitInsn(POP);
            m.visitInsn(ICONST_0);m.visitFieldInsn(PUTSTATIC,PREFIX+"VisualActor","staticValue","I");
        });
        method(helper,"unreachable","()V",ACC_PUBLIC|ACC_STATIC,m->fieldWrite(m,PREFIX+"VisualActor"));
        method(helper,"unlinkedUnreachable","()V",ACC_PUBLIC|ACC_STATIC,m->fieldWrite(m,PREFIX+"AbsentOwner"));finish(classes,"VisualHelper",helper);
        assertFalse(classes.effects("VisualFields"),"only reachable entity instance writes qualify; unreachable owners need not link");
    }
    @Test void resolvesEntityFieldEffectsThroughVirtualOverrides()throws ReflectiveOperationException {
        var classes=new Classes();entity(classes,"HookActor","legacyconnector/vb1_7_3/net/minecraft/entity/Entity");
        var base=begin("EntityBase",RENDERER);
        method(base,"render",ENTRY,ACC_PUBLIC,m->{m.visitVarInsn(ALOAD,0);m.visitMethodInsn(INVOKEVIRTUAL,PREFIX+"EntityBase","hook","()V",false);});
        method(base,"hook","()V",ACC_PROTECTED,m->fieldWrite(m,PREFIX+"HookActor"));finish(classes,"EntityBase",base);
        finish(classes,"EntityInherited",begin("EntityInherited",PREFIX+"EntityBase"));
        var visual=begin("EntityVisualOverride",PREFIX+"EntityBase");method(visual,"hook","()V",ACC_PROTECTED,m->{});finish(classes,"EntityVisualOverride",visual);
        var effect=begin("EntityEffectOverride",PREFIX+"EntityVisualOverride");method(effect,"hook","()V",ACC_PROTECTED,m->fieldWrite(m,PREFIX+"HookActor"));finish(classes,"EntityEffectOverride",effect);
        assertTrue(classes.effects("EntityInherited"));assertFalse(classes.effects("EntityVisualOverride"));assertTrue(classes.effects("EntityEffectOverride"));
    }
    @Test void followsReachableMethodsAcrossModHelpers()throws ReflectiveOperationException {
        var classes=new Classes();var renderer=begin("Transitive",RENDERER);
        method(renderer,"render",ENTRY,ACC_PUBLIC,m->call(m,"First","reachable"));finish(classes,"Transitive",renderer);
        var first=begin("First","java/lang/Object");method(first,"reachable","()V",ACC_PUBLIC|ACC_STATIC,m->call(m,"Second","mutate"));finish(classes,"First",first);
        var second=begin("Second","java/lang/Object");method(second,"mutate","()V",ACC_PUBLIC|ACC_STATIC,RendererBlockEffectsB173Test::mutation);finish(classes,"Second",second);
        assertTrue(classes.effects("Transitive"));assertTrue(classes.effects("Transitive"),"cached inspection preserves result");
    }
    @Test void ignoresUnreachableMutatorInReachableHelperClass()throws ReflectiveOperationException {
        var classes=new Classes();var renderer=begin("Unreachable",RENDERER);
        method(renderer,"render",ENTRY,ACC_PUBLIC,m->call(m,"Helper","visual"));finish(classes,"Unreachable",renderer);
        var helper=begin("Helper","java/lang/Object");method(helper,"visual","()V",ACC_PUBLIC|ACC_STATIC,m->{});
        method(helper,"unrelatedGameplay","()V",ACC_PUBLIC|ACC_STATIC,RendererBlockEffectsB173Test::mutation);finish(classes,"Helper",helper);
        assertFalse(classes.effects("Unreachable"),"class-wide scans would incorrectly replay this renderer");
    }
    @Test void resolvesInheritedEntryAndVirtualHookAgainstActualRenderer()throws ReflectiveOperationException {
        var classes=new Classes();var base=begin("Base",RENDERER);
        method(base,"render",ENTRY,ACC_PUBLIC,m->{m.visitVarInsn(ALOAD,0);m.visitMethodInsn(INVOKEVIRTUAL,PREFIX+"Base","hook","()V",false);});
        method(base,"hook","()V",ACC_PROTECTED,RendererBlockEffectsB173Test::mutation);finish(classes,"Base",base);
        var inherited=begin("Inherited",PREFIX+"Base");finish(classes,"Inherited",inherited);
        var visualOverride=begin("VisualOverride",PREFIX+"Base");method(visualOverride,"hook","()V",ACC_PROTECTED,m->{});finish(classes,"VisualOverride",visualOverride);
        assertTrue(classes.effects("Inherited"),"inherited render and mutation hook remain reachable");
        assertFalse(classes.effects("VisualOverride"),"virtual override hides base hook mutation");
        var effectOverride=begin("EffectOverride",PREFIX+"VisualOverride");method(effectOverride,"hook","()V",ACC_PROTECTED,RendererBlockEffectsB173Test::mutation);finish(classes,"EffectOverride",effectOverride);
        assertTrue(classes.effects("EffectOverride"),"most-derived virtual mutation is detected");
    }
    @Test void pureVisualEntryDoesNotReplayUnrelatedRendererMethods()throws ReflectiveOperationException {
        var classes=new Classes();var renderer=begin("PureVisual",RENDERER);
        method(renderer,"render",ENTRY,ACC_PUBLIC,m->{});method(renderer,"otherEntry","()V",ACC_PUBLIC,RendererBlockEffectsB173Test::mutation);finish(classes,"PureVisual",renderer);
        assertFalse(classes.effects("PureVisual"));
    }
}
