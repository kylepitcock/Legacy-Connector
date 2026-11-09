package org.kylepitcock.legacyconnector.vb1_7_3;

import java.util.ArrayDeque;
import legacyconnector.vb1_7_3.net.minecraft.entity.player.PlayerEntity;
import legacyconnector.vb1_7_3.net.minecraft.item.*;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
import net.minecraft.world.food.FoodProperties;

/** Native nutrition translates the food API while original callbacks retain counts, remainders and custom effects. */
public final class FoodUseB173 {
    private static final ThreadLocal<ArrayDeque<Object>> CONSUMERS=new ThreadLocal<>();
    private FoodUseB173(){}
    static FoodProperties properties(FoodItem food){return new FoodProperties.Builder()
            .nutrition(Math.max(0,Math.min(20,food.getHealthRestored()))).saturationModifier(.6F).build();}
    static ItemStack invoke(Item item,ItemStack held,World world,PlayerEntity player,boolean hunger){
        if(!hunger||!(item instanceof FoodItem))return item.use(held,world,player);
        try(var ignored=withoutHealing(player)){return item.use(held,world,player);}
    }
    private static Object identity(PlayerEntity player){return player.mcc$modern==null?player:player.mcc$modern;}
    public static boolean suppressesHealing(PlayerEntity player,int amount){
        if(amount<=0)return false;var consumers=CONSUMERS.get();if(consumers==null)return false;Object actor=identity(player);
        for(Object active:consumers)if(active==actor)return true;
        return false;
    }
    static Scope withoutHealing(PlayerEntity player){var active=CONSUMERS.get();if(active==null){active=new ArrayDeque<>();CONSUMERS.set(active);}active.addLast(identity(player));return new Scope();}
    static final class Scope implements AutoCloseable {
        private boolean closed;
        @Override public void close(){if(closed)return;closed=true;var active=CONSUMERS.get();active.removeLast();if(active.isEmpty())CONSUMERS.remove();}
    }
}
