package org.kylepitcock.legacyconnector.vb1_7_3;

import legacyconnector.vb1_7_3.net.minecraft.entity.Entity;
import legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity;
import legacyconnector.vb1_7_3.net.minecraft.world.World;

/** Admission for registered and unregistered original entity classes, independent of names. */
public final class NeutralEntitiesB173 {
    private NeutralEntitiesB173(){}
    private static final ClassValue<String> REJECTION=new ClassValue<>(){
        @Override protected String computeValue(Class<?> type){
            if(!Entity.class.isAssignableFrom(type)||LivingEntity.class.isAssignableFrom(type))return "not a neutral Entity family";
            boolean installed=false;
            for(var mod:ContentB173.loadedMods()){
                try{
                    if(type.getClassLoader()==mod.getClass().getClassLoader()
                            && java.util.Objects.equals(type.getProtectionDomain().getCodeSource(),mod.getClass().getProtectionDomain().getCodeSource())){installed=true;break;}
                }catch(SecurityException ignored){}
            }
            if(!installed)return "class is not owned by an installed original mod";
            try{type.getConstructor(World.class);}catch(NoSuchMethodException e){return "no World restore constructor";}
            String closure=AnimalsB173.unsupportedBehaviour(type,java.util.Set.of("shouldRender","getShadowRadius","getTargetingMargin","setVelocityClient","getBoundingBox","getCollisionAgainstShape","bypassesSteppingEffects","animateHurt"));
            return closure==null?"":closure;
        }
    };
    public static String unsupportedReason(Class<?> type){String reason=REJECTION.get(type);return reason.isEmpty()?null:reason;}
    public static boolean supports(Class<?> type){return unsupportedReason(type)==null;}
    public static Class<? extends Entity> savedType(String name){
        for(var mod:ContentB173.loadedMods())try{
            Class<?> type=Class.forName(name,false,mod.getClass().getClassLoader());
            if(supports(type))return type.asSubclass(Entity.class);
        }catch(ClassNotFoundException|LinkageError ignored){}
        return null;
    }
    public static Entity restore(String name,net.minecraft.world.level.Level level){
        Class<? extends Entity> type=savedType(name);if(type==null)return null;
        try{return type.getConstructor(World.class).newInstance(World.mcc$wrap(level));}
        catch(ReflectiveOperationException|LinkageError e){throw new IllegalStateException("Cannot restore admitted original entity "+name,e);}
    }
    public static boolean spawn(net.minecraft.world.level.Level level,Entity original){
        if(level.isClientSide||original.mcc$modern!=null||!supports(original.getClass()))return false;
        net.minecraft.world.entity.Entity nativeHost=null;boolean added=false;
        try{
            if(original instanceof legacyconnector.vb1_7_3.net.minecraft.entity.ItemEntity){
                var item=new LegacyB173ItemEntity(org.kylepitcock.legacyconnector.LegacyConnector.BETA_ITEM_ENTITY.get(),level);nativeHost=item;item.bind(original);
            }else{var entity=new LegacyB173Entity(org.kylepitcock.legacyconnector.LegacyConnector.BETA_ENTITY.get(),level);nativeHost=entity;entity.bind(original);}
            added=World.mcc$wrap(level).mcc$spawnNative(nativeHost);if(added)EntityViewsB173.spawned(nativeHost,original);return added;
        }catch(RuntimeException|LinkageError error){org.slf4j.LoggerFactory.getLogger("LegacyConnector/b1.7.3").error("Cannot spawn original neutral entity {}",original.getClass().getName(),error);return false;}
        finally{if(!added){original.mcc$modern=null;if(nativeHost!=null)nativeHost.discard();}}
    }
}
