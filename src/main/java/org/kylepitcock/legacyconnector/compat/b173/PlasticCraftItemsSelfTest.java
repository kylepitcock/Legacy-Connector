package org.kylepitcock.legacyconnector.compat.b173;

import java.util.*;
import legacyconnector.vb1_7_3.net.minecraft.item.*;
import legacyconnector.vb1_7_3.net.minecraft.achievement.Achievement;
import net.minecraft.server.level.ServerPlayer;
import org.kylepitcock.legacyconnector.vb1_7_3.*;

/** Optional original-jar checks. No local mod, config, resource or jar is required by ordinary builds. */
public final class PlasticCraftItemsSelfTest {
    private PlasticCraftItemsSelfTest(){}
    public static String run(ServerPlayer player)throws ReflectiveOperationException {
        if(!Boolean.getBoolean("legacyconnector.plasticcraftItemsSelfTest"))return "disabled";
        var mod=ContentB173.loadedMods().stream().filter(m->m.getClass().getSimpleName().equals("mod_PlasticCraft")).findFirst().orElseThrow();
        Class<?> type=mod.getClass();int checks=0;
        var inventory=new ArrayList<net.minecraft.world.item.ItemStack>();
        for(int i=0;i<player.getInventory().getContainerSize();i++)inventory.add(player.getInventory().getItem(i).copy());
        int selected=player.getInventory().selected;float health=player.getHealth();boolean creative=player.getAbilities().instabuild;
        var achievements=new LinkedHashMap<net.minecraft.advancements.AdvancementHolder,Boolean>();
        try{
            for(var field:type.getFields())if(Achievement.class.isAssignableFrom(field.getType())){
                var achievement=(Achievement)field.get(null);var holder=player.getServer().getAdvancements().get(AdvancementsB173.idOf(achievement));
                require(holder!=null,"registered original advancement "+field.getName());
                achievements.put(holder,player.getAdvancements().getOrStartProgress(holder).isDone());checks++;
            }
            require(achievements.size()==32,"all 32 original achievements");checks++;
            String[][] crafted={
                {"itemCLPLASTICBALL","achClrPlastic"},{"blockPLEXIGLASS","achPlexiglass"},{"itemPLASTICDOOR","achPlexDoor"},
                {"itemPLASTICBOAT","achPlasticBoat"},{"itemEPLASTICBUCKET","achBucket"},{"itemPLASTICSTRING","achPlasString"},
                {"blockPLASTICCLOTH","achSynthFiber"},{"Feather","achSynthFeather"},{"itemPLASTICWRAP","achPlasWrap"},
                {"itemJELLO","achJello"},{"itemTAPE","achTape"},{"blockGOOPLASTIC","achGooblock"},{"itemNEEDLERED","achNeedle"},
                {"Redstone","achRedstone"},{"itemSILICONCHIP","achChip"},{"armorNVGOGGLES","achGoggles"},
                {"blockMICROWAVEOFF","achMicrowave"},{"blockC4","achC4"},{"itemBATTERY","achBattery"},
                {"itemPLASTICRECORD","achPlasRecord"},{"GreenRecord","achPlasRecord"},{"GoldRecord","achPlasRecord"},
                {"blockTRAMPOLINE","achTrampoline"},{"blockACCELERATOR","achAccelerator"},{"armorFALLBOOTS","achPlasBoots"}};
            for(var pair:crafted){
                var achievement=(Achievement)type.getField(pair[1]).get(null);var holder=player.getServer().getAdvancements().get(AdvancementsB173.idOf(achievement));
                player.getAdvancements().revoke(holder,AdvancementsB173.CRITERION);
                var output=stack(type.getField(pair[0]).get(null));
                net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.entity.player.PlayerEvent.ItemCraftedEvent(player,StacksB173.toModern(output),new net.minecraft.world.SimpleContainer(9)));
                require(player.getAdvancements().getOrStartProgress(holder).isDone(),"native crafting event -> original "+pair[1]);checks++;
            }
            for(var pair:new String[][]{{"itemPLASTICBALL","achPlastic"},{"itemGOOPLASTIC","achGoo"},{"itemRUBBERBALL","achRubber"}}){
                var achievement=(Achievement)type.getField(pair[1]).get(null);var holder=player.getServer().getAdvancements().get(AdvancementsB173.idOf(achievement));
                player.getAdvancements().revoke(holder,AdvancementsB173.CRITERION);
                net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.entity.player.PlayerEvent.ItemSmeltedEvent(player,StacksB173.toModern(stack(type.getField(pair[0]).get(null)))));
                require(player.getAdvancements().getOrStartProgress(holder).isDone(),"native furnace event -> original "+pair[1]);checks++;
            }
            player.getAbilities().instabuild=false;
            String[][] foods={{"itemWATERBOTTLE","itemEMPTYBOTTLE","5"},{"itemMILKBOTTLE","itemEMPTYBOTTLE","10"},{"itemNEEDLERED","itemNEEDLE","20"},{"itemJELLO","itemJELLO","6"}};
            for(var food:foods){
                var item=(Item)type.getField(food[0]).get(null);var remainder=(Item)type.getField(food[1]).get(null);
                player.setHealth(2);player.getInventory().items.set(selected,StacksB173.toModern(new ItemStack(item,food[0].equals("itemJELLO")?2:1,0)));
                item.mcc$modern.use(player.level(),player,net.minecraft.world.InteractionHand.MAIN_HAND);
                require(player.getHealth()==Float.parseFloat(food[2]),"native food heals original amount "+food[0]);
                var result=StacksB173.toLegacy(player.getMainHandItem());require(result!=null&&result.itemId==remainder.id&&result.count==1,"native food replacement "+food[0]);checks+=2;
            }
            var disc=(Item)type.getField("itemPLASTICRECORD").get(null);
            require(disc instanceof MusicDiscItem,"record has original concrete API");
            require(new net.minecraft.world.item.ItemStack(disc.mcc$modern).has(net.minecraft.core.component.DataComponents.JUKEBOX_PLAYABLE),"record native jukebox component");checks+=2;
            var shovel=(Item)type.getField("toolPLASTICSHOVEL").get(null);
            require(shovel.getMaxDamage()==250,"original shovel durability");
            require(shovel.mcc$modern.getDestroySpeed(StacksB173.toModern(new ItemStack(shovel)),net.minecraft.world.level.block.Blocks.DIRT.defaultBlockState())==shovel.getMiningSpeedMultiplier(new ItemStack(shovel),legacyconnector.vb1_7_3.net.minecraft.block.Block.BLOCKS[3]),"native shovel speed invokes original callback");checks+=2;
            for(String field:new String[]{"armorFALLBOOTS","armorNVGOGGLES"}){
                var armor=(ArmorItem)type.getField(field).get(null);var stack=StacksB173.toModern(new ItemStack(armor));
                require(armor.getMaxDamage()==0&&armor.maxProtection==0,"original utility armor reflection "+field);
                require(armor.mcc$modern instanceof LegacyB173ArmorItem,"native armor host "+field);
                var nativeArmor=(net.minecraft.world.item.ArmorItem)armor.mcc$modern;
                require(!stack.isDamageableItem()&&nativeArmor.getDefense()==0,"original utility armor has no wear or protection "+field);
                String prefix=ContentB173.armorTexture(armor.textureIndex);
                String encoded=java.util.HexFormat.of().formatHex(prefix.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                var texture=nativeArmor.getMaterial().value().layers().getFirst().texture(false);
                require(texture.getPath().equals("textures/models/armor/beta/"+encoded+"_layer_1.png"),"native layer uses original case-sensitive armor prefix "+field);
                player.getInventory().items.set(selected,stack);player.setItemSlot(nativeArmor.getEquipmentSlot(),net.minecraft.world.item.ItemStack.EMPTY);
                var used=armor.mcc$modern.use(player.level(),player,net.minecraft.world.InteractionHand.MAIN_HAND);
                require(used.getResult()==net.minecraft.world.InteractionResult.PASS&&player.getItemBySlot(nativeArmor.getEquipmentSlot()).isEmpty()
                        &&player.getMainHandItem()==stack,"Beta armor retains no right-click autoequip "+field);checks+=5;
            }
            return checks+" original item/achievement checks passed";
        }finally{
            for(int i=0;i<inventory.size();i++)player.getInventory().setItem(i,inventory.get(i));player.getInventory().selected=selected;
            player.setHealth(health);player.getAbilities().instabuild=creative;
            for(var entry:achievements.entrySet()){
                if(entry.getValue())player.getAdvancements().award(entry.getKey(),AdvancementsB173.CRITERION);
                else player.getAdvancements().revoke(entry.getKey(),AdvancementsB173.CRITERION);
            }
            player.inventoryMenu.broadcastChanges();
        }
    }
    private static ItemStack stack(Object object){
        return object instanceof Item item?new ItemStack(item):new ItemStack((legacyconnector.vb1_7_3.net.minecraft.block.Block)object);
    }
    private static void require(boolean condition,String message){if(!condition)throw new AssertionError(message);}
}
