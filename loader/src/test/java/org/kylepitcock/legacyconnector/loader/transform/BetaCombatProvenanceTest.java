package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.kylepitcock.legacyconnector.loader.LegacyVersion;
import org.kylepitcock.legacyconnector.loader.shim.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;

class BetaCombatProvenanceTest implements Opcodes {
    static final String B="legacyconnector/vb1_7_3/net/minecraft/", E=B+"entity/Entity", W=B+"world/World", P=B+"entity/player/PlayerEntity", L=B+"entity/LivingEntity", H="org/kylepitcock/legacyconnector/vb1_7_3/CombatTargetsB173";
    static ClassNode node(String name,String parent){var n=new ClassNode();n.version=V17;n.access=ACC_PUBLIC;n.name=name;n.superName=parent;return n;}
    static MethodNode method(ClassNode node,String name,String desc){var m=new MethodNode(ACC_PUBLIC,name,desc,null,null);m.maxStack=32;m.maxLocals=24;node.methods.add(m);return m;}
    static Hierarchy hierarchy(Map<String,ClassNode> nodes){var map=new HashMap<String,ClassInfo>();
        var entity=new ClassInfo(E,"java/lang/Object",List.of(),ACC_PUBLIC);entity.fields.put("world:L"+W+";",ACC_PUBLIC);entity.fields.put("x:D",ACC_PUBLIC);map.put(E,entity);map.put(L,new ClassInfo(L,E,List.of(),ACC_PUBLIC));map.put(P,new ClassInfo(P,L,List.of(),ACC_PUBLIC));map.put(W,new ClassInfo(W,"java/lang/Object",List.of(),ACC_PUBLIC));var mob=new ClassInfo(B+"entity/MobEntity",L,List.of(),ACC_PUBLIC);mob.fields.put("target:L"+E+";",ACC_PUBLIC);map.put(mob.name,mob);
        var hierarchy=new Hierarchy(new ShimIndex(map),BetaCombatProvenanceTest.class.getClassLoader());for(var n:nodes.values())hierarchy.addLocal(ClassInfo.of(n));return hierarchy;
    }
    static void query(InsnList c,String owner){c.add(new VarInsnNode(ALOAD,0));c.add(new FieldInsnNode(GETFIELD,E,"world","L"+W+";"));c.add(new VarInsnNode(ALOAD,0));c.add(new LdcInsnNode(32D));c.add(new MethodInsnNode(INVOKEVIRTUAL,W,"getClosestPlayer","(L"+E+";D)L"+P+";",false));}
    static void field(InsnList c,String owner,String name){c.add(new VarInsnNode(ALOAD,0));c.add(new FieldInsnNode(GETFIELD,owner,name,"L"+E+";"));}
    static void hurt(InsnList c){c.add(new VarInsnNode(ALOAD,0));c.add(new InsnNode(ICONST_1));c.add(new MethodInsnNode(INVOKEVIRTUAL,E,"damage","(L"+E+";I)Z",false));c.add(new InsnNode(POP));}
    static Set<String> roles(ClassNode node){var method=node.methods.stream().filter(m->m.name.equals("mcc$combatRoles")).findFirst();if(method.isEmpty())return Set.of();var result=new HashSet<String>();for(var i:method.get().instructions)if(i instanceof LdcInsnNode ldc&&ldc.cst instanceof String s)result.add(s);return result;}
    static long calls(ClassNode n,String owner,String name){return n.methods.stream().flatMap(m->java.util.stream.StreamSupport.stream(m.instructions.spliterator(),false)).filter(i->i instanceof MethodInsnNode c&&c.owner.equals(owner)&&c.name.equals(name)).count();}
    @Test void exactFieldQueryAndRetainedWritesAreProtectedButOwnerFollowQueryIsUnchanged()throws Exception{
        var n=node("fixture/Combat",L);n.fields.add(new FieldNode(ACC_PRIVATE,"victim","L"+E+";",null,null));n.fields.add(new FieldNode(ACC_PRIVATE,"owner","L"+E+";",null,null));
        var tick=method(n,"tickLiving","()V");var c=tick.instructions;
        c.add(new VarInsnNode(ALOAD,0));query(c,n.name);c.add(new FieldInsnNode(PUTFIELD,n.name,"victim","L"+E+";"));
        c.add(new VarInsnNode(ALOAD,0));query(c,n.name);c.add(new FieldInsnNode(PUTFIELD,n.name,"owner","L"+E+";"));
        field(c,n.name,"victim");hurt(c);c.add(new InsnNode(RETURN));
        var nodes=new LinkedHashMap<String,ClassNode>();nodes.put(n.name,n);var notes=new ArrayList<String>();assertTrue(BetaCombatProvenance.apply(nodes,hierarchy(nodes),LegacyVersion.VB1_7_3,notes)>0);
        assertTrue(roles(n).contains("F|"+n.name+"|victim|L"+E+";"));assertFalse(roles(n).stream().anyMatch(s->s.contains("|owner|")));assertEquals(1,calls(n,H,"closest"));assertEquals(1,calls(n,W,"getClosestPlayer"));assertEquals(1,calls(n,H,"stored"));
        for(var m:n.methods)new Analyzer<BasicValue>(new BasicInterpreter()).analyze(n.name,m);
    }
    @Test void offensiveProjectileAimProvesPrivateTargetWithoutTreatingShooterAsVictim(){
        var shot=node("fixture/Shot",E);var hit=method(shot,"onHitTarget","(L"+E+";)Z");hit.instructions.add(new VarInsnNode(ALOAD,1));hit.instructions.add(new VarInsnNode(ALOAD,0));hit.instructions.add(new InsnNode(ICONST_0));hit.instructions.add(new MethodInsnNode(INVOKEVIRTUAL,E,"damage","(L"+E+";I)Z",false));hit.instructions.add(new InsnNode(IRETURN));
        var n=node("fixture/Flyer",L);n.fields.add(new FieldNode(ACC_PRIVATE,"prey","L"+E+";",null,null));var tick=method(n,"tickLiving","()V");var c=tick.instructions;c.add(new VarInsnNode(ALOAD,0));query(c,n.name);c.add(new FieldInsnNode(PUTFIELD,n.name,"prey","L"+E+";"));
        c.add(new TypeInsnNode(NEW,shot.name));c.add(new InsnNode(DUP));c.add(new VarInsnNode(ALOAD,0));
        field(c,n.name,"prey");c.add(new FieldInsnNode(GETFIELD,E,"x","D"));c.add(new VarInsnNode(ALOAD,0));c.add(new FieldInsnNode(GETFIELD,E,"x","D"));c.add(new InsnNode(DSUB));
        c.add(new MethodInsnNode(INVOKESPECIAL,shot.name,"<init>","(L"+L+";D)V",false));c.add(new InsnNode(POP));c.add(new InsnNode(RETURN));
        var nodes=new LinkedHashMap<String,ClassNode>();nodes.put(n.name,n);nodes.put(shot.name,shot);BetaCombatProvenance.apply(nodes,hierarchy(nodes),LegacyVersion.VB1_7_3,new ArrayList<>());
        assertTrue(roles(n).contains("F|"+n.name+"|prey|L"+E+";"));assertEquals(1,calls(n,H,"closest"));assertFalse(roles(n).stream().anyMatch(s->s.contains("shooter")));
    }
    @Test void autonomousCaptureUsesPositiveAliasGuardAndDoesNotRewriteInteractionMount(){
        String mob=B+"entity/MobEntity";var n=node("fixture/Captor",mob);var capture=method(n,"capture","(L"+E+";)V");capture.instructions.add(new VarInsnNode(ALOAD,1));capture.instructions.add(new VarInsnNode(ALOAD,0));capture.instructions.add(new MethodInsnNode(INVOKEVIRTUAL,E,"setVehicle","(L"+E+";)V",false));capture.instructions.add(new InsnNode(RETURN));
        var collision=method(n,"onCollision","(L"+E+";)V");var c=collision.instructions;var end=new LabelNode();c.add(new VarInsnNode(ALOAD,1));field(c,mob,"target");c.add(new JumpInsnNode(IF_ACMPNE,end));c.add(new VarInsnNode(ALOAD,0));c.add(new VarInsnNode(ALOAD,1));c.add(new MethodInsnNode(INVOKEVIRTUAL,n.name,"capture","(L"+E+";)V",false));c.add(end);c.add(new InsnNode(RETURN));
        var tick=method(n,"tickLiving","()V");c=tick.instructions;c.add(new VarInsnNode(ALOAD,0));query(c,n.name);c.add(new FieldInsnNode(PUTFIELD,mob,"target","L"+E+";"));c.add(new InsnNode(RETURN));
        var interaction=method(n,"interact","(L"+P+";)Z");c=interaction.instructions;c.add(new VarInsnNode(ALOAD,1));c.add(new VarInsnNode(ALOAD,0));c.add(new MethodInsnNode(INVOKEVIRTUAL,E,"setVehicle","(L"+E+";)V",false));c.add(new InsnNode(ICONST_1));c.add(new InsnNode(IRETURN));
        var nodes=Map.of(n.name,n);BetaCombatProvenance.apply(nodes,hierarchy(nodes),LegacyVersion.VB1_7_3,new ArrayList<>());
        assertTrue(roles(n).contains("F|"+mob+"|target|L"+E+";"));assertEquals(1,calls(n,H,"capture"));assertEquals(1,calls(n,E,"setVehicle"));assertEquals(1,calls(n,H,"closest"));
    }
    @Test void sameCaptureAndQueryHelpersUsedByManualInteractionNeedRuntimeAutonomousScope(){
        String mob=B+"entity/MobEntity";var n=node("fixture/SharedManualHelpers",mob);
        var getter=method(n,"findPlayer","()L"+E+";");query(getter.instructions,n.name);getter.instructions.add(new InsnNode(ARETURN));
        var capture=method(n,"mount","(L"+E+";)V");var c=capture.instructions;c.add(new VarInsnNode(ALOAD,1));c.add(new VarInsnNode(ALOAD,0));c.add(new MethodInsnNode(INVOKEVIRTUAL,E,"setVehicle","(L"+E+";)V",false));c.add(new InsnNode(RETURN));
        var tick=method(n,"tickLiving","()V");c=tick.instructions;c.add(new VarInsnNode(ALOAD,0));c.add(new VarInsnNode(ALOAD,0));c.add(new MethodInsnNode(INVOKEVIRTUAL,n.name,"findPlayer","()L"+E+";",false));c.add(new FieldInsnNode(PUTFIELD,n.name,"target","L"+E+";"));
        field(c,n.name,"target");hurt(c);c.add(new VarInsnNode(ALOAD,0));field(c,n.name,"target");c.add(new MethodInsnNode(INVOKEVIRTUAL,n.name,"mount","(L"+E+";)V",false));c.add(new InsnNode(RETURN));
        var manual=method(n,"interact","(L"+P+";)Z");c=manual.instructions;c.add(new VarInsnNode(ALOAD,0));c.add(new MethodInsnNode(INVOKEVIRTUAL,n.name,"findPlayer","()L"+E+";",false));c.add(new InsnNode(POP));c.add(new VarInsnNode(ALOAD,0));c.add(new VarInsnNode(ALOAD,1));c.add(new MethodInsnNode(INVOKEVIRTUAL,n.name,"mount","(L"+E+";)V",false));c.add(new InsnNode(ICONST_1));c.add(new InsnNode(IRETURN));
        var nodes=Map.of(n.name,n);BetaCombatProvenance.apply(nodes,hierarchy(nodes),LegacyVersion.VB1_7_3,new ArrayList<>());
        assertEquals(1,calls(n,H,"closest"));assertEquals(1,calls(n,H,"capture"));assertTrue(roles(n).stream().anyMatch(r->r.startsWith("Q|")));assertTrue(roles(n).stream().anyMatch(r->r.startsWith("C|")));
        // The exact same helpers remain reachable manually; metadata alone must never activate filtering.
        assertEquals(2,java.util.stream.StreamSupport.stream(manual.instructions.spliterator(),false).filter(i->i instanceof MethodInsnNode call&&call.owner.equals(n.name)&&(call.name.equals("findPlayer")||call.name.equals("mount"))).count());
        assertFalse(java.util.stream.StreamSupport.stream(manual.instructions.spliterator(),false).anyMatch(i->i instanceof MethodInsnNode call&&call.owner.equals(H)));
    }
    @Test void inverseOwnerExclusionDoesNotMakeOwnerACombatTarget(){
        var n=node("fixture/OwnerGuard",L);n.fields.add(new FieldNode(ACC_PRIVATE,"owner","L"+E+";",null,null));var tick=method(n,"onCollision","(L"+E+";)V");var c=tick.instructions;var end=new LabelNode();c.add(new VarInsnNode(ALOAD,1));field(c,n.name,"owner");c.add(new JumpInsnNode(IF_ACMPEQ,end));c.add(new VarInsnNode(ALOAD,1));hurt(c);c.add(end);c.add(new InsnNode(RETURN));
        var nodes=Map.of(n.name,n);BetaCombatProvenance.apply(nodes,hierarchy(nodes),LegacyVersion.VB1_7_3,new ArrayList<>());assertTrue(roles(n).isEmpty());
    }
    @Test void sharedBaseQueryRolesRemainPerConcreteClass(){
        var base=node("fixture/Base",L);base.access|=ACC_ABSTRACT;var find=method(base,"find","()L"+E+";");query(find.instructions,base.name);find.instructions.add(new InsnNode(ARETURN));
        var predator=node("fixture/Predator",base.name);var tick=method(predator,"tickLiving","()V");tick.instructions.add(new VarInsnNode(ALOAD,0));tick.instructions.add(new MethodInsnNode(INVOKEVIRTUAL,base.name,"find","()L"+E+";",false));hurt(tick.instructions);tick.instructions.add(new InsnNode(RETURN));
        var friend=node("fixture/Friend",base.name);tick=method(friend,"tickLiving","()V");tick.instructions.add(new VarInsnNode(ALOAD,0));tick.instructions.add(new MethodInsnNode(INVOKEVIRTUAL,base.name,"find","()L"+E+";",false));tick.instructions.add(new InsnNode(POP));tick.instructions.add(new InsnNode(RETURN));
        var nodes=new LinkedHashMap<String,ClassNode>();for(var n:List.of(base,predator,friend))nodes.put(n.name,n);BetaCombatProvenance.apply(nodes,hierarchy(nodes),LegacyVersion.VB1_7_3,new ArrayList<>());
        assertTrue(roles(predator).stream().anyMatch(s->s.startsWith("Q|")));assertTrue(roles(friend).isEmpty());assertEquals(1,calls(base,H,"closest"));
    }
    @Test void nativeBaseAiVirtualHooksLinkAcquisitionOnlyToAProvenOffensiveAttack(){
        for(boolean offensive:List.of(false,true)){
            String mob=B+"entity/MobEntity";var n=node("fixture/Virtual"+(offensive?"Offense":"Follow"),mob);var acquire=method(n,"getTargetInRange","()L"+E+";");query(acquire.instructions,n.name);acquire.instructions.add(new InsnNode(ARETURN));var attack=method(n,"attack","(L"+E+";F)V");if(offensive){attack.instructions.add(new VarInsnNode(ALOAD,1));hurt(attack.instructions);}attack.instructions.add(new InsnNode(RETURN));
            var nodes=Map.of(n.name,n);var hierarchy=hierarchy(nodes);var info=new ClassInfo(mob,L,List.of(),ACC_PUBLIC);info.fields.put("target:L"+E+";",ACC_PUBLIC);hierarchy.addLocal(info);BetaCombatProvenance.apply(nodes,hierarchy,LegacyVersion.VB1_7_3,new ArrayList<>());
            assertEquals(offensive,roles(n).contains("F|"+mob+"|target|L"+E+";"));assertEquals(offensive?1:0,calls(n,H,"closest"));
        }
    }
    @Test void inheritedSdkTargetQualifiedByOriginalSubclassUsesCanonicalDeclaringOwnerBeforeLinking(){
        String mob=B+"entity/MobEntity";var n=node("fixture/QualifiedTarget",mob);var tick=method(n,"tickLiving","()V");var c=tick.instructions;c.add(new VarInsnNode(ALOAD,0));query(c,n.name);c.add(new FieldInsnNode(PUTFIELD,n.name,"target","L"+E+";"));field(c,n.name,"target");hurt(c);c.add(new InsnNode(RETURN));
        var nodes=Map.of(n.name,n);var hierarchy=hierarchy(nodes);var info=new ClassInfo(mob,L,List.of(),ACC_PUBLIC);info.fields.put("target:L"+E+";",ACC_PUBLIC);hierarchy.addLocal(info);BetaCombatProvenance.apply(nodes,hierarchy,LegacyVersion.VB1_7_3,new ArrayList<>());
        assertTrue(roles(n).contains("F|"+mob+"|target|L"+E+";"));assertFalse(roles(n).contains("F|"+n.name+"|target|L"+E+";"));assertEquals(1,calls(n,H,"stored"));
        var bridge=n.methods.stream().filter(m->m.name.startsWith("mcc$combatStore$")).findFirst().orElseThrow();assertTrue(java.util.stream.StreamSupport.stream(bridge.instructions.spliterator(),false).anyMatch(i->i instanceof FieldInsnNode field&&field.getOpcode()==PUTFIELD&&field.owner.equals(n.name)));
        assertTrue(java.util.stream.StreamSupport.stream(bridge.instructions.spliterator(),false).anyMatch(i->i instanceof LdcInsnNode ldc&&mob.equals(ldc.cst)));
    }
    @Test void modernArtifactsRemainBytecodeUnchanged(){var n=node("fixture/Other",L);var nodes=Map.of(n.name,n);assertEquals(0,BetaCombatProvenance.apply(nodes,hierarchy(nodes),LegacyVersion.V1_7_10,new ArrayList<>()));assertTrue(n.methods.isEmpty());}
    @Test void unresolvedExternalConsumerReportsMixedQueryInsteadOfHidingPlayers(){
        var n=node("fixture/Mixed",L);var tick=method(n,"tickLiving","()V");var c=tick.instructions;query(c,n.name);c.add(new VarInsnNode(ASTORE,1));c.add(new VarInsnNode(ALOAD,1));hurt(c);c.add(new VarInsnNode(ALOAD,1));c.add(new MethodInsnNode(INVOKESTATIC,"external/Unknown","remember","(L"+E+";)V",false));c.add(new InsnNode(RETURN));
        var nodes=Map.of(n.name,n);var notes=new ArrayList<String>();BetaCombatProvenance.apply(nodes,hierarchy(nodes),LegacyVersion.VB1_7_3,notes);assertEquals(0,calls(n,H,"closest"));assertTrue(notes.stream().anyMatch(s->s.contains("mixed query")));
    }
    @Test void branchAndExceptionFramesAndOriginalLocalsSurviveRoundTrip()throws Exception{
        var n=node("fixture/Guarded",L);n.fields.add(new FieldNode(ACC_PRIVATE,"enemy","L"+E+";",null,null));var tick=method(n,"tickLiving","()V");var c=tick.instructions;var start=new LabelNode();var finish=new LabelNode();var handler=new LabelNode();var done=new LabelNode();
        c.add(start);c.add(new VarInsnNode(ALOAD,0));query(c,n.name);c.add(new FieldInsnNode(PUTFIELD,n.name,"enemy","L"+E+";"));field(c,n.name,"enemy");c.add(new JumpInsnNode(IFNULL,finish));field(c,n.name,"enemy");hurt(c);c.add(finish);c.add(new JumpInsnNode(GOTO,done));c.add(handler);c.add(new VarInsnNode(ASTORE,1));c.add(done);c.add(new InsnNode(RETURN));tick.tryCatchBlocks.add(new TryCatchBlockNode(start,finish,handler,"java/lang/RuntimeException"));
        var before=new ClassWriter(ClassWriter.COMPUTE_FRAMES|ClassWriter.COMPUTE_MAXS);n.accept(before);n=new ClassNode();new ClassReader(before.toByteArray()).accept(n,0);var original=n.methods.getFirst();int locals=original.maxLocals;var originalFrames=new ArrayList<FrameNode>();for(var i:original.instructions)if(i instanceof FrameNode f)originalFrames.add(f);
        var nodes=Map.of(n.name,n);BetaCombatProvenance.apply(nodes,hierarchy(nodes),LegacyVersion.VB1_7_3,new ArrayList<>());assertEquals(locals,original.maxLocals);assertTrue(originalFrames.stream().allMatch(f->original.instructions.indexOf(f)>=0));
        for(var m:n.methods)new Analyzer<BasicValue>(new BasicInterpreter()).analyze(n.name,m);var writer=new ClassWriter(0);n.accept(writer);var roundTrip=new ClassNode();new ClassReader(writer.toByteArray()).accept(roundTrip,0);for(var m:roundTrip.methods)new Analyzer<BasicValue>(new BasicInterpreter()).analyze(roundTrip.name,m);
        var bytecode=new HashMap<String,byte[]>();bytecode.put(roundTrip.name,writer.toByteArray());for(String name:List.of(E,L,P,W)){var stub=node(name,name.equals(L)?E:name.equals(P)?L:"java/lang/Object");if(name.equals(E)){stub.fields.add(new FieldNode(ACC_PUBLIC,"world","L"+W+";",null,null));stub.fields.add(new FieldNode(ACC_PUBLIC,"x","D",null,null));var damage=method(stub,"damage","(L"+E+";I)Z");damage.instructions.add(new InsnNode(ICONST_0));damage.instructions.add(new InsnNode(IRETURN));}var stubWriter=new ClassWriter(0);stub.accept(stubWriter);bytecode.put(name,stubWriter.toByteArray());}
        var helper=node(H,"java/lang/Object");for(String[] signature:List.of(new String[]{"closest","(L"+W+";L"+E+";DLjava/lang/String;)L"+P+";"},new String[]{"stored","(L"+E+";L"+E+";Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)L"+E+";"})){var m=method(helper,signature[0],signature[1]);m.access|=ACC_STATIC;m.instructions.add(new InsnNode(ACONST_NULL));m.instructions.add(new InsnNode(ARETURN));}var helperWriter=new ClassWriter(0);helper.accept(helperWriter);bytecode.put(H,helperWriter.toByteArray());
        var loader=new ClassLoader(getClass().getClassLoader()){@Override protected Class<?> findClass(String name)throws ClassNotFoundException{var bytes=bytecode.get(name.replace('.','/'));if(bytes==null)throw new ClassNotFoundException(name);return defineClass(name,bytes,0,bytes.length);}};
        assertDoesNotThrow(()->loader.loadClass("fixture.Guarded").getDeclaredMethods());
    }
    @Test void recursiveHelperGraphConvergesWithoutTraversingRenderOrConstructors(){
        var n=node("fixture/Recursive",L);var find=method(n,"find","(Z)L"+E+";");var ordinary=new LabelNode();var c=find.instructions;c.add(new VarInsnNode(ILOAD,1));c.add(new JumpInsnNode(IFEQ,ordinary));c.add(new VarInsnNode(ALOAD,0));c.add(new InsnNode(ICONST_0));c.add(new MethodInsnNode(INVOKEVIRTUAL,n.name,"find","(Z)L"+E+";",false));c.add(new InsnNode(ARETURN));c.add(ordinary);query(c,n.name);c.add(new InsnNode(ARETURN));
        var tick=method(n,"tickLiving","()V");c=tick.instructions;c.add(new VarInsnNode(ALOAD,0));c.add(new InsnNode(ICONST_1));c.add(new MethodInsnNode(INVOKEVIRTUAL,n.name,"find","(Z)L"+E+";",false));hurt(c);c.add(new InsnNode(RETURN));
        var render=method(n,"render","()V");query(render.instructions,n.name);render.instructions.add(new InsnNode(POP));render.instructions.add(new InsnNode(RETURN));var nodes=Map.of(n.name,n);var notes=new ArrayList<String>();BetaCombatProvenance.apply(nodes,hierarchy(nodes),LegacyVersion.VB1_7_3,notes);
        assertEquals(1,calls(n,H,"closest"));assertEquals(1,calls(n,W,"getClosestPlayer"));assertFalse(notes.stream().anyMatch(note->note.contains("unresolved fixed point")));
    }
    static final String INV=B+"entity/player/PlayerInventory",STACK=B+"item/ItemStack",MONSTER=B+"entity/mob/MonsterEntity";
    static Hierarchy inventoryHierarchy(Map<String,ClassNode> nodes){
        var h=hierarchy(nodes);h.addLocal(new ClassInfo(INV,"java/lang/Object",List.of(),ACC_PUBLIC));h.addLocal(new ClassInfo(STACK,"java/lang/Object",List.of(),ACC_PUBLIC));
        var monster=new ClassInfo(MONSTER,B+"entity/MobEntity",List.of(),ACC_PUBLIC);monster.methods.put("attack(L"+E+";F)V",ACC_PROTECTED);h.addLocal(monster);return h;
    }
    static ClassNode extractor(String suffix,boolean offensive){var n=node("fixture/Inventory"+suffix,offensive?MONSTER:L);n.fields.add(new FieldNode(ACC_PRIVATE,"kept","L"+STACK+";",null,null));return n;}
    static void inventoryArray(InsnList c){c.add(new FieldInsnNode(GETFIELD,P,"inventory","L"+INV+";"));c.add(new FieldInsnNode(GETFIELD,INV,"main","[L"+STACK+";"));c.add(new VarInsnNode(ASTORE,2));c.add(new VarInsnNode(ALOAD,2));c.add(new InsnNode(ICONST_0));c.add(new InsnNode(AALOAD));c.add(new VarInsnNode(ASTORE,3));}
    static void keepStack(InsnList c,ClassNode n){c.add(new VarInsnNode(ALOAD,0));c.add(new VarInsnNode(ALOAD,3));c.add(new FieldInsnNode(PUTFIELD,n.name,"kept","L"+STACK+";"));}
    static void clearSlot(InsnList c){c.add(new VarInsnNode(ALOAD,2));c.add(new InsnNode(ICONST_0));c.add(new InsnNode(ACONST_NULL));c.add(new InsnNode(AASTORE));}
    static void countChange(InsnList c,int operation,int delta){c.add(new VarInsnNode(ALOAD,3));c.add(new VarInsnNode(ALOAD,3));c.add(new FieldInsnNode(GETFIELD,STACK,"count","I"));c.add(new LdcInsnNode(delta));c.add(new InsnNode(operation));c.add(new FieldInsnNode(PUTFIELD,STACK,"count","I"));}
    @Test void autonomousExtractionRequiresActorRetentionAndIndependentOffense(){
        for(boolean offensive:List.of(false,true))for(boolean retained:List.of(false,true)){
            var n=extractor("Null"+offensive+retained,offensive);var m=method(n,"tickMovement","()V");query(m.instructions,n.name);inventoryArray(m.instructions);if(retained)keepStack(m.instructions,n);clearSlot(m.instructions);m.instructions.add(new InsnNode(RETURN));var nodes=Map.of(n.name,n);
            BetaCombatProvenance.apply(nodes,inventoryHierarchy(nodes),LegacyVersion.VB1_7_3,new ArrayList<>());assertEquals(offensive&&retained?1:0,calls(n,H,"closest"));
        }
    }
    @Test void retainedSameStackProvableDecrementsQualifyButIncreasesAndUnrelatedStackDoNot(){
        for(int[] operation:List.of(new int[]{ISUB,2,1},new int[]{IADD,-1,1},new int[]{IADD,1,0},new int[]{ISUB,-1,0})){
            var n=extractor("Delta"+operation[0]+"x"+operation[1],true);var m=method(n,"tickMovement","()V");query(m.instructions,n.name);inventoryArray(m.instructions);keepStack(m.instructions,n);countChange(m.instructions,operation[0],operation[1]);m.instructions.add(new InsnNode(RETURN));var nodes=Map.of(n.name,n);
            BetaCombatProvenance.apply(nodes,inventoryHierarchy(nodes),LegacyVersion.VB1_7_3,new ArrayList<>());assertEquals(operation[2],calls(n,H,"closest"));
        }
        var n=extractor("OtherStack",true);n.fields.add(new FieldNode(ACC_PRIVATE,"personal","L"+STACK+";",null,null));var m=method(n,"tickMovement","()V");var c=m.instructions;query(c,n.name);inventoryArray(c);keepStack(c,n);c.add(new VarInsnNode(ALOAD,0));c.add(new FieldInsnNode(GETFIELD,n.name,"personal","L"+STACK+";"));c.add(new VarInsnNode(ALOAD,3));c.add(new FieldInsnNode(GETFIELD,STACK,"count","I"));c.add(new InsnNode(ICONST_1));c.add(new InsnNode(ISUB));c.add(new FieldInsnNode(PUTFIELD,STACK,"count","I"));c.add(new InsnNode(RETURN));var nodes=Map.of(n.name,n);BetaCombatProvenance.apply(nodes,inventoryHierarchy(nodes),LegacyVersion.VB1_7_3,new ArrayList<>());assertEquals(0,calls(n,H,"closest"));
    }
    @Test void compoundArithmeticCannotCarryAnInnerDecrementProof()throws Exception{
        for(String operation:List.of("netIncrease","negation","multiplication","shift","conversion","compoundDecrease","sourcePositive","sourceNegative")){
            var n=extractor("Compound"+operation,true);var m=method(n,"tickMovement","()V");var c=m.instructions;query(c,n.name);inventoryArray(c);keepStack(c,n);
            boolean sourceConstant=operation.startsWith("source");if(sourceConstant){c.add(new InsnNode(operation.equals("sourcePositive")?ICONST_2:ICONST_M1));c.add(new VarInsnNode(ISTORE,4));}
            c.add(new VarInsnNode(ALOAD,3));c.add(new VarInsnNode(ALOAD,3));c.add(new FieldInsnNode(GETFIELD,STACK,"count","I"));
            if(sourceConstant){c.add(new VarInsnNode(ILOAD,4));c.add(new InsnNode(operation.equals("sourcePositive")?ISUB:IADD));}
            else if(operation.equals("compoundDecrease")){c.add(new IntInsnNode(BIPUSH,100));c.add(new InsnNode(IADD));c.add(new IntInsnNode(BIPUSH,101));c.add(new InsnNode(ISUB));}
            else {c.add(new InsnNode(ICONST_1));c.add(new InsnNode(ISUB));switch(operation){
                case "netIncrease"->{c.add(new IntInsnNode(BIPUSH,100));c.add(new InsnNode(IADD));}
                case "negation"->c.add(new InsnNode(INEG));
                case "multiplication"->{c.add(new InsnNode(ICONST_M1));c.add(new InsnNode(IMUL));}
                case "shift"->{c.add(new InsnNode(ICONST_1));c.add(new InsnNode(ISHL));}
                case "conversion"->{c.add(new InsnNode(I2L));c.add(new InsnNode(L2I));}
                default->throw new AssertionError(operation);
            }}
            c.add(new FieldInsnNode(PUTFIELD,STACK,"count","I"));c.add(new InsnNode(RETURN));var nodes=Map.of(n.name,n);BetaCombatProvenance.apply(nodes,inventoryHierarchy(nodes),LegacyVersion.VB1_7_3,new ArrayList<>());
            assertEquals(sourceConstant?1:0,calls(n,H,"closest"),operation);for(var candidate:n.methods)new Analyzer<BasicValue>(new BasicInterpreter()).analyze(n.name,candidate);
        }
    }
    @Test void retainedFieldWithMultiplePossibleStackOriginsDoesNotCreditEitherPlayerQuery(){
        var n=extractor("AmbiguousAlias",true);var m=method(n,"tickMovement","()V");var c=m.instructions;
        query(c,n.name);inventoryArray(c);keepStack(c,n);query(c,n.name);inventoryArray(c);keepStack(c,n);
        c.add(new VarInsnNode(ALOAD,0));c.add(new FieldInsnNode(GETFIELD,n.name,"kept","L"+STACK+";"));c.add(new InsnNode(DUP));c.add(new FieldInsnNode(GETFIELD,STACK,"count","I"));c.add(new InsnNode(ICONST_1));c.add(new InsnNode(ISUB));c.add(new FieldInsnNode(PUTFIELD,STACK,"count","I"));c.add(new InsnNode(RETURN));
        var nodes=Map.of(n.name,n);var notes=new ArrayList<String>();BetaCombatProvenance.apply(nodes,inventoryHierarchy(nodes),LegacyVersion.VB1_7_3,notes);
        assertEquals(0,calls(n,H,"closest"));assertEquals(2,calls(n,W,"getClosestPlayer"));assertFalse(roles(n).stream().anyMatch(role->role.startsWith("Q|")));assertTrue(notes.stream().anyMatch(note->note.contains("ambiguous retained aliases")),notes.toString());
    }
    @Test void positiveInsertionReadingAndLocalSortingWithoutRetentionPreserveNearestPlayer(){
        for(boolean retained:List.of(false,true)){
            var n=extractor("Positive"+retained,true);var m=method(n,"tickMovement","()V");var c=m.instructions;query(c,n.name);inventoryArray(c);if(retained)keepStack(c,n);else clearSlot(c);c.add(new VarInsnNode(ALOAD,2));c.add(new InsnNode(ICONST_0));c.add(new VarInsnNode(ALOAD,3));c.add(new InsnNode(AASTORE));countChange(c,IADD,1);c.add(new InsnNode(RETURN));var nodes=Map.of(n.name,n);BetaCombatProvenance.apply(nodes,inventoryHierarchy(nodes),LegacyVersion.VB1_7_3,new ArrayList<>());assertEquals(0,calls(n,H,"closest"));
        }
    }
    @Test void actorInventoryAndExplicitOwnerWithoutQueryAreNeverInventoryQueryRoles(){
        for(boolean own:List.of(false,true)){
            var n=extractor("Owner"+own,true);if(own)n.superName=P;n.fields.add(new FieldNode(ACC_PUBLIC,"owner","L"+P+";",null,null));n.fields.add(new FieldNode(ACC_PRIVATE,"combatVictim","L"+E+";",null,null));var m=method(n,"tickMovement","()V");var c=m.instructions;if(own){field(c,n.name,"combatVictim");hurt(c);}
            // An unrelated nearby-player lookup must not be broadened by mutation of another inventory.
            query(c,n.name);c.add(new InsnNode(POP));c.add(new VarInsnNode(ALOAD,0));if(!own)c.add(new FieldInsnNode(GETFIELD,n.name,"owner","L"+P+";"));inventoryArray(c);keepStack(c,n);clearSlot(c);c.add(new InsnNode(RETURN));var nodes=Map.of(n.name,n);var notes=new ArrayList<String>();BetaCombatProvenance.apply(nodes,inventoryHierarchy(nodes),LegacyVersion.VB1_7_3,notes);assertEquals(0,calls(n,H,"closest"));assertFalse(roles(n).stream().anyMatch(r->r.contains("|owner|")));
        }
    }
    @Test void extractionThroughProvenRetainedVictimFieldAddsItsCanonicalFieldRole(){
        var n=extractor("VictimField",true);n.fields.add(new FieldNode(ACC_PRIVATE,"candidate","L"+P+";",null,null));var m=method(n,"tickMovement","()V");var c=m.instructions;c.add(new VarInsnNode(ALOAD,0));query(c,n.name);c.add(new FieldInsnNode(PUTFIELD,n.name,"candidate","L"+P+";"));c.add(new VarInsnNode(ALOAD,0));c.add(new FieldInsnNode(GETFIELD,n.name,"candidate","L"+P+";"));inventoryArray(c);keepStack(c,n);clearSlot(c);c.add(new InsnNode(RETURN));var nodes=Map.of(n.name,n);BetaCombatProvenance.apply(nodes,inventoryHierarchy(nodes),LegacyVersion.VB1_7_3,new ArrayList<>());
        assertEquals(1,calls(n,H,"closest"));assertTrue(roles(n).contains("F|"+n.name+"|candidate|L"+P+";"));
    }
    @Test void retainedStackAliasCanBeDecrementedThroughItsActorField(){
        var n=extractor("RetainedAlias",true);var m=method(n,"tickMovement","()V");var c=m.instructions;query(c,n.name);inventoryArray(c);keepStack(c,n);c.add(new VarInsnNode(ALOAD,0));c.add(new FieldInsnNode(GETFIELD,n.name,"kept","L"+STACK+";"));c.add(new InsnNode(DUP));c.add(new FieldInsnNode(GETFIELD,STACK,"count","I"));c.add(new InsnNode(ICONST_1));c.add(new InsnNode(ISUB));c.add(new FieldInsnNode(PUTFIELD,STACK,"count","I"));c.add(new InsnNode(RETURN));var nodes=Map.of(n.name,n);BetaCombatProvenance.apply(nodes,inventoryHierarchy(nodes),LegacyVersion.VB1_7_3,new ArrayList<>());assertEquals(1,calls(n,H,"closest"));
    }
    @Test void constructorAndManualOnlyExtractionDoNotBecomeAutonomousQuerySites(){
        for(String root:List.of("interact","<init>")){
            var n=extractor("Manual"+root.replace("<","").replace(">",""),true);var helper=method(n,"extract","()V");var c=helper.instructions;query(c,n.name);inventoryArray(c);keepStack(c,n);clearSlot(c);c.add(new InsnNode(RETURN));var entry=method(n,root,root.equals("interact")?"(L"+P+";)Z":"(L"+W+";)V");c=entry.instructions;if(root.equals("<init>")){c.add(new VarInsnNode(ALOAD,0));c.add(new VarInsnNode(ALOAD,1));c.add(new MethodInsnNode(INVOKESPECIAL,MONSTER,"<init>","(L"+W+";)V",false));}c.add(new VarInsnNode(ALOAD,0));c.add(new MethodInsnNode(INVOKEVIRTUAL,n.name,"extract","()V",false));if(root.equals("interact"))c.add(new InsnNode(ICONST_0));c.add(new InsnNode(root.equals("interact")?IRETURN:RETURN));var nodes=Map.of(n.name,n);BetaCombatProvenance.apply(nodes,inventoryHierarchy(nodes),LegacyVersion.VB1_7_3,new ArrayList<>());assertEquals(0,calls(n,H,"closest"));
        }
    }
    /** Optional unchanged-artifact audit: class names are assertions, never production selectors. */
    @Test @EnabledIfSystemProperty(named="legacyconnector.beta.combatArtifact",matches=".+")
    void actualArtifactPrivateAimAndCaptureHaveClosedRoles()throws Exception{
        var nodes=new LinkedHashMap<String,ClassNode>();try(var jar=new java.util.jar.JarFile(System.getProperty("legacyconnector.beta.combatArtifact"))){var entries=jar.entries();while(entries.hasMoreElements()){var entry=entries.nextElement();if(!entry.getName().endsWith(".class"))continue;var node=new ClassNode();try(var stream=jar.getInputStream(entry)){new ClassReader(stream.readAllBytes()).accept(node,0);}nodes.put(node.name,node);}}
        // A translated artifact is supplied explicitly; it is never copied into the portable test fixtures.
        String index=System.getProperty("legacyconnector.beta.combatShimIndex");assertNotNull(index,"Supply the compiled runtime shim-index.txt as combatShimIndex");Hierarchy hierarchy;try(var stream=java.nio.file.Files.newInputStream(java.nio.file.Path.of(index))){hierarchy=new Hierarchy(ShimIndex.read(stream),getClass().getClassLoader());}for(var node:nodes.values())hierarchy.addLocal(ClassInfo.of(node));var notes=new ArrayList<String>();BetaCombatProvenance.apply(nodes,hierarchy,LegacyVersion.VB1_7_3,notes);
        for(String family:List.of("EntityZephyr","EntitySwet","EntityAechorPlant")){var node=nodes.values().stream().filter(n->n.name.endsWith("/"+family)).findFirst().orElseThrow();assertTrue(roles(node).stream().anyMatch(r->r.startsWith("F|")),family+" fields "+notes);assertTrue(roles(node).stream().anyMatch(r->r.startsWith("Q|")),family+" queries "+notes);if(family.equals("EntitySwet"))assertTrue(roles(node).stream().anyMatch(r->r.startsWith("C|")),notes.toString());}
    }

}
