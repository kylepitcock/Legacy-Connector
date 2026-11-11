package org.kylepitcock.legacyconnector.loader.transform;

import java.util.*;
import org.kylepitcock.legacyconnector.loader.LegacyVersion;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;

/** Same-artifact, bounded combat provenance. No names belonging to a mod are semantic inputs. */
public final class BetaCombatProvenance implements Opcodes {
    private static final String BASE="legacyconnector/vb1_7_3/net/minecraft/", ENTITY=BASE+"entity/Entity", WORLD=BASE+"world/World", BOX=BASE+"util/math/Box";
    private static final String PLAYER=BASE+"entity/player/PlayerEntity",INVENTORY=BASE+"entity/player/PlayerInventory",STACK=BASE+"item/ItemStack";
    private static final String HELPER="org/kylepitcock/legacyconnector/vb1_7_3/CombatTargetsB173";
    private static final Set<String> ROOTS=Set.of("tick","tickLiving","tickMovement","onCollision","getTargetInRange","attack");
    private static final int MAX_METHODS=512,MAX_INSNS=16384,MAX_TOKENS=256,MAX_ROUNDS=32;
    private final Map<String,ClassNode> classes;private final Hierarchy hierarchy;private final List<String> diagnostics;
    private final Map<Key,Frame<SourceValue>[]> frames=new HashMap<>();private final Set<Key> invalid=new HashSet<>();
    private final Set<String> projectiles=new HashSet<>();
    private record Key(String owner,String name,String desc) {String id(){return owner+"#"+name+desc;}}
    private record Field(String owner,String name,String desc) {String token(){return "F|"+owner+"|"+name+"|"+desc;}}
    private record Site(Key method,int ordinal) {String id(){return method.id()+"#"+ordinal;}}
    private record InventoryStack(String owner,String origin){}
    private final Map<InventoryStack,String> stackTokens=new HashMap<>();private final Map<String,InventoryStack> stackSources=new HashMap<>();private final Set<String> nearestQueries=new HashSet<>();
    private record Summary(Set<String> returned,Set<String> offense){}
    private BetaCombatProvenance(Map<String,ClassNode> classes,Hierarchy hierarchy,List<String> diagnostics){this.classes=classes;this.hierarchy=hierarchy;this.diagnostics=diagnostics;}

    public static int apply(Map<String,ClassNode> classes,Hierarchy hierarchy,LegacyVersion version,List<String> diagnostics){
        if(version!=LegacyVersion.VB1_7_3)return 0;
        return new BetaCombatProvenance(classes,hierarchy,diagnostics).run();
    }
    private int run(){
        if(classes.size()>4096){diagnostics.add("Beta combat provenance skipped: artifact exceeds 4096 classes");return 0;}
        long instructions=0;int methods=0;for(var node:classes.values())for(var method:node.methods){instructions+=method.instructions.size();methods++;}
        if(instructions>2_000_000||methods>32768){diagnostics.add("Beta combat provenance skipped: artifact exceeds executable bytecode bound");return 0;}
        discoverProjectiles();
        Map<String,Set<String>> manifests=new LinkedHashMap<>();Set<String> fields=new LinkedHashSet<>(),queries=new LinkedHashSet<>(),captures=new LinkedHashSet<>();
        for(var concrete:classes.values()){
            if((concrete.access&(ACC_ABSTRACT|ACC_INTERFACE))!=0||!subtype(concrete.name,ENTITY))continue;
            var graph=reachable(concrete.name);if(graph==null)continue;
            if(graph.stream().anyMatch(key->frames(key)==null)){diagnostics.add("Beta combat role map omitted after unresolved method: "+concrete.name);continue;}
            Map<Key,Summary> summaries=new HashMap<>();Analysis analysis=null;boolean stable=false;
            for(int round=0;round<MAX_ROUNDS;round++){
                analysis=new Analysis(concrete.name,graph,summaries);Map<Key,Summary> next=new HashMap<>();
                for(var key:graph)next.put(key,analysis.inspect(key));
                if(next.equals(summaries)){stable=true;break;}summaries=next;
            }
            if(!stable){diagnostics.add("Beta combat provenance unresolved fixed point: "+concrete.name);continue;}
            analysis=new Analysis(concrete.name,graph,summaries);for(var key:graph)analysis.inspect(key);
            if(analysis.incomplete){diagnostics.add("Beta combat provenance unresolved trace bound: "+concrete.name);continue;}
            var offensive=new LinkedHashSet<String>();for(var summary:summaries.values())offensive.addAll(summary.offense);
            // Native baseAI enters these original virtual hooks through the SDK, outside this artifact's call graph.
            var attack=resolve(concrete.name,"attack","(L"+ENTITY+";F)V");
            var attackTokens=new LinkedHashSet<String>(summaries.getOrDefault(attack,new Summary(Set.of(),Set.of())).offense);
            boolean expanded;do{expanded=false;for(var dependency:analysis.dependencies.entrySet())if(attackTokens.contains(dependency.getKey()))expanded|=attackTokens.addAll(dependency.getValue());}while(expanded&&attackTokens.size()<=MAX_TOKENS);
            boolean attacksParameter=attack!=null&&attackTokens.contains("P|"+attack.id()+"|1");
            boolean nativeMonsterAttack=attack==null&&inheritsSdkMethod(concrete.name,"attack","(L"+ENTITY+";F)V",BASE+"entity/mob/MonsterEntity");
            if(subtype(concrete.name,BASE+"entity/MobEntity")&&(attacksParameter||nativeMonsterAttack)){
                String targetField=field(new FieldInsnNode(GETFIELD,BASE+"entity/MobEntity","target","L"+ENTITY+";")).token();offensive.add(targetField);
                var acquire=resolve(concrete.name,"getTargetInRange","()L"+ENTITY+";");if(acquire!=null){var summary=summaries.get(acquire);if(summary!=null)analysis.dependencies.computeIfAbsent(targetField,ignored->new LinkedHashSet<>()).addAll(summary.returned);}
            }
            // Extraction requires both retention of a queried player's stack and a destructive write,
            // in an actor that already has independently proved offensive behavior.
            boolean offensiveActor=!offensive.isEmpty()||attacksParameter||nativeMonsterAttack;
            var depletedStackOrigins=new LinkedHashSet<String>();boolean countAliasAmbiguous=false;
            for(var reference:analysis.depletedStacks){var origins=analysis.stackOrigins(Set.of(reference),true);
                if(origins.size()==1)depletedStackOrigins.addAll(origins);
                else if((origins.size()>1||origins.isEmpty()&&!analysis.stackOrigins(Set.of(reference)).isEmpty())&&!countAliasAmbiguous){diagnostics.add("Beta inventory count depletion has ambiguous retained aliases: "+concrete.name);countAliasAmbiguous=true;}
            }
            boolean extractionAmbiguous=false;
            for(String retained:analysis.stackOrigins(analysis.retainedStacks)){
                var source=stackSources.get(retained);if(source==null)continue;
                boolean depleted=depletedStackOrigins.contains(retained)||analysis.depletedInventories.contains(source.owner);
                if(!depleted)continue;
                var queriesForOwner=nearestSources(source.owner,analysis.dependencies);
                if(offensiveActor&&queriesForOwner.size()==1){offensive.add(source.owner);offensive.addAll(queriesForOwner);}
                else if(!extractionAmbiguous){diagnostics.add("Beta inventory extraction remains ambiguous: "+concrete.name+" (offensive="+offensiveActor+", nearest sources="+queriesForOwner.size()+")");extractionAmbiguous=true;}
            }
            boolean changed;do{changed=false;for(var entry:analysis.dependencies.entrySet())if(offensive.contains(entry.getKey()))changed|=offensive.addAll(entry.getValue());
                for(var pair:analysis.equalities)if(offensive.contains(pair.get(0))||offensive.contains(pair.get(1))){changed|=offensive.add(pair.get(0));changed|=offensive.add(pair.get(1));}
            }while(changed&&offensive.size()<=MAX_TOKENS);
            if(offensive.size()>MAX_TOKENS){diagnostics.add("Beta combat provenance exceeds token bound: "+concrete.name);continue;}
            for(var dependency:analysis.dependencies.entrySet())if(!offensive.contains(dependency.getKey()))for(var token:dependency.getValue())if(token.startsWith("Q|"))analysis.neutral.add(token);
            do{changed=false;for(var dependency:analysis.dependencies.entrySet())if(analysis.neutral.contains(dependency.getKey()))changed|=analysis.neutral.addAll(dependency.getValue());}while(changed);
            var roles=new TreeSet<String>();for(var token:offensive)if(token.startsWith("F|")&&!sharedSdkRelationship(token)&&!analysis.neutral.contains(token))roles.add(token);
            for(var token:offensive)if(token.startsWith("Q|")){
                // A query also escaping to an unrelated consumer is deliberately not globally narrowed.
                if(analysis.neutral.contains(token)){diagnostics.add("Beta combat mixed query remains unchanged: "+concrete.name+" "+token.substring(2));continue;}
                roles.add(token);
            }
            for(var entry:analysis.captureVictims.entrySet())if(entry.getValue().stream().anyMatch(offensive::contains))roles.add("C|"+entry.getKey());
            if(roles.size()>256||roles.stream().mapToInt(s->s.length()*3).sum()>16384){diagnostics.add("Beta combat manifest exceeds bound: "+concrete.name);continue;}
            if(!roles.isEmpty()){
                manifests.put(concrete.name,roles);for(var role:roles){if(role.startsWith("F|"))fields.add(role);else if(role.startsWith("Q|"))queries.add(role.substring(2));else captures.add(role.substring(2));}
            }else if(analysis.queryCount>0)diagnostics.add("Beta autonomous queries have no proved combat sink: "+concrete.name+" ("+analysis.queryCount+")");
        }
        int changes=rewrite(fields,queries,captures);for(var entry:manifests.entrySet())emitManifest(classes.get(entry.getKey()),entry.getValue());
        for(var node:classes.values())hierarchy.addLocal(org.kylepitcock.legacyconnector.loader.shim.ClassInfo.of(node));
        if(!manifests.isEmpty())diagnostics.add("Beta combat provenance: "+manifests.size()+" concrete role maps, "+fields.size()+" fields, "+queries.size()+" queries, "+captures.size()+" captures");
        return changes;
    }
    private MethodNode method(Key key){var node=classes.get(key.owner);if(node==null)return null;for(var method:node.methods)if(method.name.equals(key.name)&&method.desc.equals(key.desc))return method;return null;}
    private Key resolve(String owner,String name,String desc){for(int depth=0;owner!=null&&depth<64;depth++){
        var node=classes.get(owner);if(node==null)return null;for(var method:node.methods)if(method.name.equals(name)&&method.desc.equals(desc))return new Key(owner,name,desc);owner=node.superName;
    }return null;}
    private boolean inheritsSdkMethod(String name,String method,String desc,String declaration){
        for(int depth=0;name!=null&&depth<64;depth++){var info=hierarchy.find(name);if(info==null)return false;if(info.hasMethod(method,desc))return name.equals(declaration);name=info.superName;}return false;
    }
    private boolean subtype(String name,String parent){for(int depth=0;name!=null&&depth<64;depth++){if(name.equals(parent))return true;var info=hierarchy.find(name);if(info==null)return false;name=info.superName;}return false;}
    private Field field(FieldInsnNode instruction){String owner=instruction.owner;for(int depth=0;owner!=null&&depth<64;depth++){
        var info=hierarchy.find(owner);if(info==null)break;var node=classes.get(owner);
        if(node!=null){for(var field:node.fields)if(field.name.equals(instruction.name)&&field.desc.equals(instruction.desc))return new Field(owner,field.name,field.desc);}
        else { // SDK fields may be referenced through their original subclass; use the SDK declaration when available.
            if(info.hasField(instruction.name,instruction.desc))return new Field(owner,instruction.name,instruction.desc);
        }owner=info.superName;
    }return new Field(instruction.owner,instruction.name,instruction.desc);}
    private boolean entityDescriptor(String desc){return desc.startsWith("L")&&subtype(Type.getType(desc).getInternalName(),ENTITY);}
    private Set<Key> reachable(String concrete){var result=new LinkedHashSet<Key>();var pending=new ArrayDeque<Key>();
        for(var name:ROOTS){if((name.equals("attack")||name.equals("getTargetInRange"))&&!subtype(concrete,BASE+"entity/MobEntity"))continue;String desc=switch(name){case "onCollision"->"(L"+ENTITY+";)V";case "getTargetInRange"->"()L"+ENTITY+";";case "attack"->"(L"+ENTITY+";F)V";default->"()V";};var key=resolve(concrete,name,desc);if(key!=null)pending.add(key);}
        while(!pending.isEmpty()){
            var key=pending.removeFirst();if(!result.add(key))continue;var method=method(key);
            if(result.size()>MAX_METHODS||method.instructions.size()>MAX_INSNS){diagnostics.add("Beta combat reachability exceeds bound: "+concrete);return null;}
            for(var instruction:method.instructions)if(instruction instanceof MethodInsnNode call&&!call.name.startsWith("<")){
                boolean self=call.getOpcode()!=INVOKESTATIC&&receiverIsThis(key,call);
                if(call.getOpcode()!=INVOKESTATIC&&subtype(call.owner,ENTITY)&&!self)continue;
                Key target=resolve(call.owner,call.name,call.desc);
                // Virtual dispatch on this artifact's inherited implementation must use the concrete role map.
                if(call.getOpcode()!=INVOKESTATIC&&call.getOpcode()!=INVOKESPECIAL&&self&&subtype(concrete,call.owner)){var virtual=resolve(concrete,call.name,call.desc);if(virtual!=null)target=virtual;}
                if(target!=null)pending.add(target);
            }
        }return result;
    }
    private boolean receiverIsThis(Key key,MethodInsnNode call){
        var fs=frames(key);if(fs==null)return false;int at=method(key).instructions.indexOf(call);var frame=fs[at];if(frame==null)return false;
        int receiver=frame.getStackSize()-Type.getArgumentTypes(call.desc).length-1;
        return receiver>=0&&onlyThis(key,frame.getStack(receiver),new HashSet<>(),0);
    }
    private boolean onlyThis(Key key,SourceValue value,Set<AbstractInsnNode> seen,int depth){
        if(depth>64||value.insns.isEmpty())return false;var method=method(key);var fs=frames(key);
        for(var source:value.insns){if(!seen.add(source))return false;int at=method.instructions.indexOf(source);var frame=fs[at];
            if(source instanceof VarInsnNode variable&&variable.getOpcode()==ALOAD){
                var local=frame.getLocal(variable.var);if(local.insns.isEmpty()){if(variable.var!=0||(method.access&ACC_STATIC)!=0)return false;}
                else if(!onlyThis(key,local,seen,depth+1))return false;
            }else if(source.getOpcode()==ASTORE||source.getOpcode()==CHECKCAST||source.getOpcode()==DUP){
                if(!onlyThis(key,frame.getStack(frame.getStackSize()-1),seen,depth+1))return false;
            }else return false;
        }return true;
    }
    private Frame<SourceValue>[] frames(Key key){if(invalid.contains(key))return null;return frames.computeIfAbsent(key,k->{try{return new Analyzer<>(new SourceInterpreter()).analyze(k.owner,method(k));}catch(AnalyzerException|RuntimeException ex){invalid.add(k);diagnostics.add("Beta combat unresolved bytecode: "+k.id()+" "+ex.getClass().getSimpleName());return null;}});}
    private void discoverProjectiles(){
        // A nonliving Entity implementation with a real victim damage call is an offensive projectile contract.
        for(var node:classes.values())if(subtype(node.name,ENTITY)&&!subtype(node.name,BASE+"entity/LivingEntity")){
            String owner=node.name;for(int depth=0;owner!=null&&depth<64;depth++){
                var ancestor=classes.get(owner);if(ancestor==null){if(subtype(owner,BASE+"entity/projectile/ArrowEntity")||subtype(owner,BASE+"entity/projectile/FireballEntity"))projectiles.add(node.name);break;}
                for(var method:ancestor.methods)if(!method.name.startsWith("<"))for(var instruction:method.instructions)if(instruction instanceof MethodInsnNode call&&damage(call)&&frames(new Key(ancestor.name,method.name,method.desc))!=null&&!receiverIsThis(new Key(ancestor.name,method.name,method.desc),call)){projectiles.add(node.name);break;}
                owner=ancestor.superName;
            }
        }
    }
    private boolean projectile(String owner){return projectiles.contains(owner)||subtype(owner,BASE+"entity/projectile/ArrowEntity")||subtype(owner,BASE+"entity/projectile/FireballEntity");}
    private static boolean sharedSdkRelationship(String token){return token.startsWith("F|"+BASE)&&!token.startsWith("F|"+BASE+"entity/MobEntity|target|");}
    private boolean damage(MethodInsnNode call){return call.name.equals("damage")&&call.desc.equals("(L"+ENTITY+";I)Z")&&subtype(call.owner,ENTITY);}
    private String query(MethodInsnNode call){if(!subtype(call.owner,WORLD))return null;
        if(call.name.equals("getClosestPlayer")&&(call.desc.equals("(L"+ENTITY+";D)L"+BASE+"entity/player/PlayerEntity;")||call.desc.equals("(DDDD)L"+BASE+"entity/player/PlayerEntity;")))return "closest";
        if(call.name.equals("getEntities")&&call.desc.equals("(L"+ENTITY+";L"+BOX+";)Ljava/util/List;"))return "entities";
        if(call.name.equals("collectEntitiesByClass")&&call.desc.equals("(Ljava/lang/Class;L"+BOX+";)Ljava/util/List;"))return "typedEntities";
        return null;
    }
    private final class Analysis {
        final String concrete;final Set<Key> graph;final Map<Key,Summary> summaries;
        final Map<String,Set<String>> dependencies=new HashMap<>(),captureVictims=new HashMap<>(),stackAliases=new HashMap<>();final List<List<String>> equalities=new ArrayList<>();final Set<String> neutral=new HashSet<>(),retainedStacks=new HashSet<>(),depletedStacks=new HashSet<>(),depletedInventories=new HashSet<>();int queryCount;boolean incomplete;
        Analysis(String concrete,Set<Key> graph,Map<Key,Summary> summaries){this.concrete=concrete;this.graph=graph;this.summaries=summaries;}
        Summary inspect(Key key){var method=method(key);var frames=frames(key);var returned=new LinkedHashSet<String>();var offense=new LinkedHashSet<String>();var sinkUses=new HashMap<Integer,Set<String>>();var comparisons=new ArrayList<Integer>();if(frames==null)return new Summary(Set.of(),Set.of());
            for(int index=0;index<method.instructions.size();index++){
                var instruction=method.instructions.get(index);var frame=frames[index];if(frame==null)continue;
                if(instruction instanceof MethodInsnNode call){var site=new Site(key,ordinal(method,index));var args=arguments(key,call,frame);var receiver=call.getOpcode()==INVOKESTATIC?Set.<String>of():trace(key,frame.getStack(frame.getStackSize()-args.size()-1),new HashSet<>(),0);
                    String query=query(call);if(query!=null){queryCount++;continue;}
                    var consumed=new LinkedHashSet<String>();
                    if(damage(call))consumed.addAll(withoutThis(receiver));
                    boolean capture=call.name.equals("setVehicle")&&call.desc.equals("(L"+ENTITY+";)V")&&subtype(call.owner,ENTITY)&&args.size()==1&&args.getFirst().contains("T");
                    if(capture){var victim=withoutThis(receiver);consumed.addAll(victim);captureVictims.computeIfAbsent(site.id(),ignored->new LinkedHashSet<>()).addAll(victim);}
                    if(projectile(call.owner))for(int a=0;a<args.size();a++)if(Type.getArgumentTypes(call.desc)[a].getSort()>=Type.BYTE&&Type.getArgumentTypes(call.desc)[a].getSort()<=Type.DOUBLE)for(var token:args.get(a))if(token.startsWith("a:"))consumed.add(token.substring(2));
                    var target=target(call,receiver);var summary=summaries.get(target);
                    if(summary!=null)consumed.addAll(substitute(summary.offense,receiver,args));
                    offense.addAll(consumed);if(!consumed.isEmpty())sinkUses.put(index,consumed);
                    // Calls to artifact helpers are analyzed; known geometry/query/collection operations preserve provenance.
                    // An unknown external consumer of a query reference makes narrowing ambiguous.
                    if(summary==null&&!damage(call)&&!capture&&!projectile(call.owner)&&!transparent(call)){for(var arg:args)for(var token:arg)if(token.startsWith("Q|")||token.startsWith("F|"))neutral.add(token);for(var token:receiver)if(token.startsWith("Q|")||token.startsWith("F|"))neutral.add(token);}
                }else if(instruction instanceof FieldInsnNode field&&field.getOpcode()==PUTFIELD&&entityDescriptor(field.desc)){
                    var owner=trace(key,frame.getStack(frame.getStackSize()-2),new HashSet<>(),0);if(owner.contains("T")&&subtype(concrete,field.owner))dependencies.computeIfAbsent(field(field).token(),ignored->new LinkedHashSet<>()).addAll(trace(key,frame.getStack(frame.getStackSize()-1),new HashSet<>(),0));
                }else if(instruction instanceof FieldInsnNode field&&field.getOpcode()==PUTFIELD&&subtype(Type.getType(field.desc).getSort()==Type.OBJECT?Type.getType(field.desc).getInternalName():null,STACK)){
                    var receiver=trace(key,frame.getStack(frame.getStackSize()-2),new HashSet<>(),0);var declared=field(field);
                    if(receiver.equals(Set.of("T"))&&subtype(concrete,declared.owner)&&classes.containsKey(declared.owner))
                        {var aliases=stackAliases.computeIfAbsent("r:"+declared.token(),ignored->new LinkedHashSet<>());var assigned=trace(key,frame.getStack(frame.getStackSize()-1),new HashSet<>(),0);boolean closed=!assigned.isEmpty();for(var token:assigned)if(stackReference(token)){retainedStacks.add(token);aliases.add(token);}else closed=false;if(!closed)aliases.add("unknown-stack:"+key.id()+"@"+index);}
                }else if(instruction instanceof FieldInsnNode field&&field.getOpcode()==PUTFIELD&&subtype(field.owner,STACK)&&field.name.equals("count")&&field.desc.equals("I")){
                    var receiver=trace(key,frame.getStack(frame.getStackSize()-2),new HashSet<>(),0);
                    for(var token:receiver)if(receiver.size()==1&&stackReference(token)&&negativeCountWrite(key,frame.getStack(frame.getStackSize()-1),token,new HashSet<>(),0))depletedStacks.add(token);
                }else if(instruction.getOpcode()==AASTORE&&nullValue(key,frame.getStack(frame.getStackSize()-1),new HashSet<>(),0)){
                    for(var token:trace(key,frame.getStack(frame.getStackSize()-3),new HashSet<>(),0))if(token.startsWith("v:"))depletedInventories.add(token.substring(2));
                }else if(instruction.getOpcode()==ARETURN)returned.addAll(trace(key,frame.getStack(frame.getStackSize()-1),new HashSet<>(),0));
                else if(instruction.getOpcode()==IF_ACMPEQ||instruction.getOpcode()==IF_ACMPNE)comparisons.add(index);
            }
            for(int at:comparisons){
                var jump=(JumpInsnNode)method.instructions.get(at);int target=method.instructions.indexOf(jump.label);
                int equal=jump.getOpcode()==IF_ACMPEQ?target:at+1,unequal=jump.getOpcode()==IF_ACMPNE?target:at+1;
                var frame=frames[at];var left=trace(key,frame.getStack(frame.getStackSize()-2),new HashSet<>(),0);var right=trace(key,frame.getStack(frame.getStackSize()-1),new HashSet<>(),0);
                for(var l:left)for(var r:right){String parameter=l.startsWith("P|")?l:r.startsWith("P|")?r:null;String field=l.startsWith("F|")?l:r.startsWith("F|")?r:null;
                    if(parameter==null||field==null)continue;
                    for(var sink:sinkUses.entrySet())if(sink.getValue().contains(parameter)&&reaches(method,equal,sink.getKey())==1&&reaches(method,unequal,sink.getKey())==0){equalities.add(List.of(parameter,field));break;}
                }
            }return new Summary(Set.copyOf(returned),Set.copyOf(offense));
        }
        Key target(MethodInsnNode call,Set<String> receiver){if(call.getOpcode()!=INVOKESTATIC&&subtype(call.owner,ENTITY)&&!receiver.equals(Set.of("T")))return null;var target=resolve(call.owner,call.name,call.desc);if(call.getOpcode()!=INVOKESTATIC&&call.getOpcode()!=INVOKESPECIAL&&receiver.equals(Set.of("T"))&&subtype(concrete,call.owner)){var virtual=resolve(concrete,call.name,call.desc);if(virtual!=null)target=virtual;}return target;}
        List<Set<String>> arguments(Key key,MethodInsnNode call,Frame<SourceValue> frame){var types=Type.getArgumentTypes(call.desc);var values=new ArrayList<Set<String>>();int start=frame.getStackSize()-types.length;for(int i=0;i<types.length;i++)values.add(trace(key,frame.getStack(start+i),new HashSet<>(),0));return values;}
        Set<String> trace(Key key,SourceValue value,Set<AbstractInsnNode> visited,int depth){
            if(value==null)return Set.of();if(depth>80||visited.size()>512){incomplete=true;return Set.of();}var out=new LinkedHashSet<String>();var method=method(key);var fs=frames(key);if(fs==null)return out;
            for(var source:value.insns){if(!visited.add(source))continue;int at=method.instructions.indexOf(source);if(at<0||fs[at]==null)continue;var frame=fs[at];int op=source.getOpcode();
                if(source instanceof VarInsnNode variable){
                    if(op==ALOAD||op==ILOAD||op==DLOAD||op==FLOAD||op==LLOAD){var local=frame.getLocal(variable.var);if(local.insns.isEmpty()){if(variable.var==0&&(method.access&ACC_STATIC)==0)out.add("T");else out.add("P|"+key.id()+"|"+variable.var);}else out.addAll(trace(key,local,visited,depth+1));}
                    else if(op==ASTORE||op==ISTORE||op==DSTORE||op==FSTORE||op==LSTORE)out.addAll(trace(key,frame.getStack(frame.getStackSize()-1),visited,depth+1));
                }else if(source instanceof FieldInsnNode field&&op==GETFIELD){
                    var receiver=trace(key,frame.getStack(frame.getStackSize()-1),visited,depth+1);
                    if(subtype(field.owner,PLAYER)&&field.name.equals("inventory")&&field.desc.equals("L"+INVENTORY+";")){
                        for(var token:receiver)if(token.startsWith("Q|")||token.startsWith("F|"))out.add("i:"+token);
                    }else if(subtype(field.owner,INVENTORY)&&Set.of("main","armor").contains(field.name)&&field.desc.equals("[L"+STACK+";")){
                        for(var token:receiver)if(token.startsWith("i:"))out.add("v:"+token.substring(2));
                    }else if(subtype(field.owner,STACK)&&field.name.equals("count")&&field.desc.equals("I")){
                        for(var token:receiver)if(stackReference(token))out.add("n:"+token);
                    }else if(subtype(Type.getType(field.desc).getSort()==Type.OBJECT?Type.getType(field.desc).getInternalName():null,STACK)&&receiver.equals(Set.of("T"))&&subtype(concrete,field.owner)&&classes.containsKey(field(field).owner)){out.add("r:"+field(field).token());
                    }else if(entityDescriptor(field.desc)&&receiver.contains("T")&&subtype(concrete,field.owner))out.add(field(field).token());
                    else if(subtype(field.owner,ENTITY)&&Set.of("x","y","z","prevX","prevY","prevZ").contains(field.name)&&(field.desc.equals("D")||field.desc.equals("F")))for(var token:receiver)out.add("c:"+token);
                }else if(source instanceof MethodInsnNode call){
                    int ordinal=ordinal(method,at);
                    if(query(call)!=null){String token="Q|"+new Site(key,ordinal).id();out.add(token);if(query(call).equals("closest"))nearestQueries.add(token);}
                    else{
                        var types=Type.getArgumentTypes(call.desc);int start=frame.getStackSize()-types.length;var args=new ArrayList<Set<String>>();for(int i=0;i<types.length;i++)args.add(trace(key,frame.getStack(start+i),new HashSet<>(visited),depth+1));
                        var receiver=call.getOpcode()==INVOKESTATIC?Set.<String>of():trace(key,frame.getStack(start-1),new HashSet<>(visited),depth+1);var target=target(call,receiver);var summary=summaries.get(target);
                        if(summary!=null)out.addAll(substitute(summary.returned,receiver,args));
                        else if(call.owner.startsWith("java/util/")&&(call.name.equals("get")||call.name.equals("iterator")||call.name.equals("next")))out.addAll(receiver);
                        else if(subtype(call.owner,ENTITY)&&call.name.equals("getEyeHeight"))for(var token:receiver)out.add("c:"+token);
                    }
                }else if(op==CHECKCAST||op==DUP)out.addAll(trace(key,frame.getStack(frame.getStackSize()-1),visited,depth+1));
                else if(op==DNEG||op==FNEG||op==INEG||op==I2D||op==D2F||op==F2D||op==I2F||op==L2D||op==D2I)arithmeticSources(out,trace(key,frame.getStack(frame.getStackSize()-1),visited,depth+1));
                else if((op>=IADD&&op<=DREM)||(op>=ISHL&&op<=LXOR)||op==AALOAD){
                    var left=trace(key,frame.getStack(frame.getStackSize()-2),new HashSet<>(visited),depth+1);var right=trace(key,frame.getStack(frame.getStackSize()-1),new HashSet<>(visited),depth+1);
                    if(op==AALOAD){for(var token:left)if(token.startsWith("v:")){String stack=inventoryStack(token.substring(2),key.id()+"@"+at);if(stack!=null)out.add(stack);else incomplete=true;}for(var token:left)if(!token.startsWith("v:")&&!token.startsWith("n:")&&!token.startsWith("d:"))out.add(token);arithmeticSources(out,right);}
                    else {arithmeticSources(out,left);arithmeticSources(out,right);}
                    if((op==DSUB||op==FSUB)&&(left.contains("c:T")||right.contains("c:T")))for(var token:new ArrayList<>(out))if(token.startsWith("c:")&&!token.equals("c:T"))out.add("a:"+token.substring(2));
                }
            }if(out.size()>MAX_TOKENS){incomplete=true;return Set.of();}return out;
        }
        /** Count admission proves the complete assigned expression, never a nested arithmetic subexpression. */
        private boolean negativeCountWrite(Key key,SourceValue value,String stack,Set<AbstractInsnNode> seen,int depth){
            if(value==null||value.insns.isEmpty()||depth>64)return false;var method=method(key);var fs=frames(key);
            for(var source:value.insns){if(!seen.add(source))return false;var frame=fs[method.instructions.indexOf(source)];int op=source.getOpcode();
                if(source instanceof VarInsnNode variable&&op==ILOAD){if(!negativeCountWrite(key,frame.getLocal(variable.var),stack,seen,depth+1))return false;}
                else if(op==ISTORE||op==DUP){if(!negativeCountWrite(key,frame.getStack(frame.getStackSize()-1),stack,seen,depth+1))return false;}
                else if(op==ISUB||op==IADD){
                    var delta=integerConstant(key,frame.getStack(frame.getStackSize()-1),new HashSet<>(),0);
                    if(delta==null||!(op==ISUB&&delta>0||op==IADD&&delta<0)||!exactCountRead(key,frame.getStack(frame.getStackSize()-2),stack,new HashSet<>(),0))return false;
                }else return false;
            }return true;
        }
        private boolean exactCountRead(Key key,SourceValue value,String stack,Set<AbstractInsnNode> seen,int depth){
            if(value==null||value.insns.isEmpty()||depth>64)return false;var method=method(key);var fs=frames(key);
            for(var source:value.insns){if(!seen.add(source))return false;var frame=fs[method.instructions.indexOf(source)];int op=source.getOpcode();
                if(source instanceof VarInsnNode variable&&op==ILOAD){if(!exactCountRead(key,frame.getLocal(variable.var),stack,seen,depth+1))return false;}
                else if(op==ISTORE||op==DUP){if(!exactCountRead(key,frame.getStack(frame.getStackSize()-1),stack,seen,depth+1))return false;}
                else if(source instanceof FieldInsnNode field&&op==GETFIELD&&subtype(field.owner,STACK)&&field.name.equals("count")&&field.desc.equals("I")){
                    if(!trace(key,frame.getStack(frame.getStackSize()-1),new HashSet<>(),0).equals(Set.of(stack)))return false;
                }else return false;
            }return true;
        }
        private void arithmeticSources(Set<String> destination,Set<String> sources){for(var token:sources)if(!token.startsWith("n:")&&!token.startsWith("d:"))destination.add(token);}
        private boolean stackReference(String token){return stackSources.containsKey(token)||token.startsWith("r:");}
        Set<String> stackOrigins(Set<String> references){return stackOrigins(references,false);}
        Set<String> stackOrigins(Set<String> references,boolean closed){
            var result=new LinkedHashSet<String>();var seen=new HashSet<String>();var pending=new ArrayDeque<String>(references);
            while(!pending.isEmpty()){String token=pending.removeFirst();if(!seen.add(token))continue;if(seen.size()>MAX_TOKENS)return Set.of();if(stackSources.containsKey(token))result.add(token);else {var aliases=stackAliases.get(token);if(closed&&(aliases==null||aliases.isEmpty()))return Set.of();pending.addAll(aliases==null?Set.of():aliases);}}return result;
        }
        private boolean nullValue(Key key,SourceValue value,Set<AbstractInsnNode> seen,int depth){
            if(value==null||value.insns.isEmpty()||depth>64)return false;var method=method(key);var fs=frames(key);
            for(var source:value.insns){if(!seen.add(source))return false;var frame=fs[method.instructions.indexOf(source)];if(source.getOpcode()==ACONST_NULL)continue;
                if(source instanceof VarInsnNode variable&&source.getOpcode()==ALOAD){if(!nullValue(key,frame.getLocal(variable.var),seen,depth+1))return false;}
                else if(source.getOpcode()==ASTORE||source.getOpcode()==CHECKCAST){if(!nullValue(key,frame.getStack(frame.getStackSize()-1),seen,depth+1))return false;}
                else return false;
            }return true;
        }
        private Integer integerConstant(Key key,SourceValue value,Set<AbstractInsnNode> seen,int depth){
            if(value==null||value.insns.isEmpty()||depth>64)return null;Integer result=null;var method=method(key);var fs=frames(key);
            for(var source:value.insns){if(!seen.add(source))return null;int op=source.getOpcode();Integer constant=null;var frame=fs[method.instructions.indexOf(source)];
                if(op>=ICONST_M1&&op<=ICONST_5)constant=op-ICONST_0;
                else if(source instanceof IntInsnNode integer&&(op==BIPUSH||op==SIPUSH))constant=integer.operand;
                else if(source instanceof LdcInsnNode ldc&&ldc.cst instanceof Integer integer)constant=integer;
                else if(source instanceof VarInsnNode variable&&op==ILOAD)constant=integerConstant(key,frame.getLocal(variable.var),seen,depth+1);
                else if(op==ISTORE)constant=integerConstant(key,frame.getStack(frame.getStackSize()-1),seen,depth+1);
                else if(op==INEG){var operand=integerConstant(key,frame.getStack(frame.getStackSize()-1),seen,depth+1);if(operand!=null&&operand!=Integer.MIN_VALUE)constant=-operand;}
                if(constant==null||result!=null&&!result.equals(constant))return null;result=constant;
            }return result;
        }
        Set<String> substitute(Set<String> tokens,Set<String> receiver,List<Set<String>> args){var out=new LinkedHashSet<String>();for(var token:tokens){
            if(token.equals("T")){out.addAll(receiver);continue;}
            if(token.startsWith("P|")){int split=token.lastIndexOf('|');int slot=Integer.parseInt(token.substring(split+1));String methodId=token.substring(2,split);Key target=null;for(var key:graph)if(key.id().equals(methodId)){target=key;break;}if(target!=null){int parameterSlot=(method(target).access&ACC_STATIC)==0?1:0;var types=Type.getArgumentTypes(target.desc);for(int a=0;a<types.length;a++){if(parameterSlot==slot&&a<args.size())out.addAll(args.get(a));parameterSlot+=types[a].getSize();}}}
            else out.add(token);
        }return out;}
    }
    private String inventoryStack(String owner,String origin){
        var source=new InventoryStack(owner,origin);String token=stackTokens.get(source);if(token!=null)return token;if(stackTokens.size()>=8192)return null;
        token="s:"+stackTokens.size();stackTokens.put(source,token);stackSources.put(token,source);return token;
    }
    private Set<String> nearestSources(String owner,Map<String,Set<String>> dependencies){
        var result=new LinkedHashSet<String>();var seen=new HashSet<String>();var pending=new ArrayDeque<String>();pending.add(owner);
        while(!pending.isEmpty()){
            String token=pending.removeFirst();if(!seen.add(token))continue;if(seen.size()>MAX_TOKENS)return Set.of();
            if(nearestQueries.contains(token)){result.add(token);continue;}
            var sources=dependencies.get(token);if(!token.startsWith("F|")||sources==null||sources.isEmpty())return Set.of();
            pending.addAll(sources);
        }return result;
    }
    private static int reaches(MethodNode method,int from,int target){
        var seen=new BitSet();var pending=new ArrayDeque<Integer>();pending.add(from);int steps=0;
        while(!pending.isEmpty()&&steps++<MAX_INSNS){int at=pending.removeFirst();if(at==target)return 1;if(at<0||at>=method.instructions.size()||seen.get(at))continue;seen.set(at);var instruction=method.instructions.get(at);int op=instruction.getOpcode();
            if(instruction instanceof JumpInsnNode jump){pending.add(method.instructions.indexOf(jump.label));if(op!=GOTO&&op!=JSR)pending.add(at+1);}
            else if(instruction instanceof TableSwitchInsnNode table){pending.add(method.instructions.indexOf(table.dflt));for(var label:table.labels)pending.add(method.instructions.indexOf(label));}
            else if(instruction instanceof LookupSwitchInsnNode lookup){pending.add(method.instructions.indexOf(lookup.dflt));for(var label:lookup.labels)pending.add(method.instructions.indexOf(label));}
            else if(op!=ATHROW&&(op<IRETURN||op>RETURN))pending.add(at+1);
        }return pending.isEmpty()?0:-1;
    }
    private static int ordinal(MethodNode method,int at){int result=0;for(int i=0;i<at;i++)if(method.instructions.get(i) instanceof MethodInsnNode)result++;return result;}
    private static Set<String> withoutThis(Set<String> values){var result=new LinkedHashSet<String>();for(var value:values)if(!value.equals("T")&&!value.startsWith("c:")&&!value.startsWith("a:"))result.add(value);return result;}
    private boolean transparent(MethodInsnNode call){return call.owner.startsWith("java/util/")||call.owner.equals("java/lang/Math")||subtype(call.owner,ENTITY)&&!call.name.equals("setVehicle")||subtype(call.owner,WORLD);}

    private int rewrite(Set<String> fields,Set<String> queries,Set<String> captures){int count=0;
        for(var node:classes.values())for(var method:new ArrayList<>(node.methods)){
            var key=new Key(node.name,method.name,method.desc);int ordinal=0;
            for(var instruction:method.instructions.toArray()){
                if(instruction instanceof MethodInsnNode call){String site=new Site(key,ordinal++).id();String query=query(call);
                    if(query!=null&&queries.contains(site)){
                        method.instructions.insertBefore(call,new LdcInsnNode(site));String descriptor="(L"+WORLD+";"+call.desc.substring(1,call.desc.indexOf(')'))+"Ljava/lang/String;)"+Type.getReturnType(call.desc).getDescriptor();
                        method.instructions.set(call,new MethodInsnNode(INVOKESTATIC,HELPER,query,descriptor,false));method.maxStack++;count++;
                    }else if(captures.contains(site)&&call.name.equals("setVehicle")){
                        method.instructions.insertBefore(call,new LdcInsnNode(site));method.instructions.set(call,new MethodInsnNode(INVOKESTATIC,HELPER,"capture","(L"+ENTITY+";L"+ENTITY+";Ljava/lang/String;)V",false));method.maxStack++;count++;
                    }
                }else if(instruction instanceof FieldInsnNode field&&field.getOpcode()==PUTFIELD&&entityDescriptor(field.desc)&&fields.contains(field(field).token())&&subtype(field.owner,ENTITY)){
                    String bridge="mcc$combatStore$"+node.methods.size();String descriptor="(L"+field.owner+";"+field.desc+")V";var wrapper=new MethodNode(ACC_PRIVATE|ACC_STATIC|ACC_SYNTHETIC,bridge,descriptor,null,null);var code=wrapper.instructions;
                    code.add(new VarInsnNode(ALOAD,0));code.add(new VarInsnNode(ALOAD,0));code.add(new VarInsnNode(ALOAD,1));var declared=field(field);code.add(new LdcInsnNode(declared.owner));code.add(new LdcInsnNode(declared.name));code.add(new LdcInsnNode(declared.desc));
                    code.add(new MethodInsnNode(INVOKESTATIC,HELPER,"stored","(L"+ENTITY+";L"+ENTITY+";Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)L"+ENTITY+";",false));code.add(new TypeInsnNode(CHECKCAST,Type.getType(field.desc).getInternalName()));code.add(new FieldInsnNode(PUTFIELD,field.owner,field.name,field.desc));code.add(new InsnNode(RETURN));wrapper.maxStack=6;wrapper.maxLocals=2;node.methods.add(wrapper);
                    method.instructions.set(field,new MethodInsnNode(INVOKESTATIC,node.name,bridge,descriptor,false));count++;
                }
            }
        }return count;
    }
    private static void emitManifest(ClassNode node,Set<String> roles){
        if(node.methods.stream().anyMatch(m->m.name.equals("mcc$combatRoles")))return;
        var method=new MethodNode(ACC_PUBLIC|ACC_STATIC|ACC_SYNTHETIC,"mcc$combatRoles","()[Ljava/lang/String;",null,null);var code=method.instructions;code.add(new LdcInsnNode(roles.size()));code.add(new TypeInsnNode(ANEWARRAY,"java/lang/String"));int index=0;
        for(var role:roles){code.add(new InsnNode(DUP));code.add(new LdcInsnNode(index++));code.add(new LdcInsnNode(role));code.add(new InsnNode(AASTORE));}code.add(new InsnNode(ARETURN));method.maxStack=4;method.maxLocals=0;node.methods.add(method);
    }
}
