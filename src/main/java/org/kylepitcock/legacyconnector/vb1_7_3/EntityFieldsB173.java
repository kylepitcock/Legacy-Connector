package org.kylepitcock.legacyconnector.vb1_7_3;

import java.lang.reflect.Field;
import net.neoforged.fml.util.ObfuscationReflectionHelper;

/** Native fields needed by Beta entity and navigation APIs; names are remapped by NeoForge. */
public final class EntityFieldsB173 {
    private EntityFieldsB173(){}
    private static final Field JUMP_INPUT=ObfuscationReflectionHelper.findField(net.minecraft.world.entity.LivingEntity.class,"jumping");
    private static final Field ITEM_AGE=ObfuscationReflectionHelper.findField(net.minecraft.world.entity.item.ItemEntity.class,"age");
    private static final Field PICKUP_DELAY=ObfuscationReflectionHelper.findField(net.minecraft.world.entity.item.ItemEntity.class,"pickupDelay");
    private static final Field CREEPER_FUSE=ObfuscationReflectionHelper.findField(net.minecraft.world.entity.monster.Creeper.class,"swell");
    private static final Field FALLING_TIME=ObfuscationReflectionHelper.findField(net.minecraft.world.entity.item.FallingBlockEntity.class,"time");
    private static final Field PATH_TIMEOUT_CLOCK=ObfuscationReflectionHelper.findField(net.minecraft.world.entity.ai.navigation.PathNavigation.class,"lastTimeoutCheck");
    private static int get(Field field,Object target){try{return field.getInt(target);}catch(IllegalAccessException e){throw new IllegalStateException("Cannot read native Beta entity state",e);}}
    private static void set(Field field,Object target,int value){try{field.setInt(target,value);}catch(IllegalAccessException e){throw new IllegalStateException("Cannot write native Beta entity state",e);}}
    public static boolean jumpInput(net.minecraft.world.entity.LivingEntity entity){try{return JUMP_INPUT.getBoolean(entity);}catch(IllegalAccessException error){throw new IllegalStateException("Cannot read native jump input",error);}}
    public static int itemAge(net.minecraft.world.entity.item.ItemEntity item){return get(ITEM_AGE,item);}
    public static void itemAge(net.minecraft.world.entity.item.ItemEntity item,int age){set(ITEM_AGE,item,age);}
    public static int pickupDelay(net.minecraft.world.entity.item.ItemEntity item){return get(PICKUP_DELAY,item);}
    public static int creeperFuse(net.minecraft.world.entity.monster.Creeper creeper){return get(CREEPER_FUSE,creeper);}
    public static void creeperFuse(net.minecraft.world.entity.monster.Creeper creeper,int fuse){set(CREEPER_FUSE,creeper,fuse);}
    public static int fallingTime(net.minecraft.world.entity.item.FallingBlockEntity entity){return get(FALLING_TIME,entity);}
    public static void fallingTime(net.minecraft.world.entity.item.FallingBlockEntity entity,int time){set(FALLING_TIME,entity,time);}
    /** Paused legacy routes must not count world time as time spent failing to reach the next node. */
    public static void pauseNavigationTimeout(net.minecraft.world.entity.ai.navigation.PathNavigation navigation,long gameTime){try{PATH_TIMEOUT_CLOCK.setLong(navigation,gameTime);}catch(IllegalAccessException error){throw new IllegalStateException("Cannot pause native navigation timeout",error);}}
}
