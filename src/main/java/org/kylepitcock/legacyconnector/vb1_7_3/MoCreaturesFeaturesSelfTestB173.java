package org.kylepitcock.legacyconnector.vb1_7_3;

import java.lang.reflect.*;
import java.util.*;
import legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
import net.minecraft.world.InteractionHand;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/** Controlled original MoCreatures gameplay callbacks. No production behavior is selected by these names. */
public final class MoCreaturesFeaturesSelfTestB173 {
    private static final org.slf4j.Logger LOG=org.slf4j.LoggerFactory.getLogger("LegacyConnector/B173FeaturesTest");
    private MoCreaturesFeaturesSelfTestB173(){}
    public static void register(IEventBus bus){if(Boolean.getBoolean("legacyconnector.entitySelfTest"))bus.addListener((ServerStartedEvent event)->{try{run(event.getServer().overworld());}catch(RuntimeException|LinkageError error){LOG.error("B1.7.3 MOC FEATURES TEST failed",error);}});}
    private static void require(boolean value,String detail){if(!value)throw new IllegalStateException(detail);}
    private static Field field(Object object,String name)throws ReflectiveOperationException {for(Class<?> type=object.getClass();type!=null;type=type.getSuperclass())try{var field=type.getDeclaredField(name);field.setAccessible(true);return field;}catch(NoSuchFieldException ignored){}throw new NoSuchFieldException(name);}
    private static void put(Object object,String name,Object value)throws ReflectiveOperationException {field(object,name).set(object,value);}
    private static Object get(Object object,String name)throws ReflectiveOperationException {return field(object,name).get(object);}
    private static Object call(Object object,String name,Class<?>[] signature,Object... args)throws ReflectiveOperationException {for(Class<?> type=object.getClass();type!=null;type=type.getSuperclass())try{var method=type.getDeclaredMethod(name,signature);method.setAccessible(true);return method.invoke(object,args);}catch(NoSuchMethodException ignored){}throw new NoSuchMethodException(name);}
    private static final class ZeroRandom extends Random {@Override public int nextInt(int bound){return 0;}@Override public float nextFloat(){return .5F;}}
    /** FakePlayer deliberately disables riding, menus and advancements; these proofs need normal native behavior. */
    private static net.minecraft.server.level.ServerPlayer actor(net.minecraft.server.level.ServerLevel level,int index){
        var profile=new com.mojang.authlib.GameProfile(java.util.UUID.nameUUIDFromBytes(("legacyconnector:features:"+index).getBytes(java.nio.charset.StandardCharsets.UTF_8)),"B173Feature"+index);
        var player=new net.minecraft.server.level.ServerPlayer(level.getServer(),level,profile,net.minecraft.server.level.ClientInformation.createDefault());
        var connection=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new net.minecraft.server.network.ServerGamePacketListenerImpl(level.getServer(),connection,player,net.minecraft.server.network.CommonListenerCookie.createInitial(profile,false)){
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet){}
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet,net.minecraft.network.PacketSendListener listener){}
        };
        return player;
    }
    private static final class Context implements AutoCloseable {
        final net.minecraft.server.level.ServerLevel level;final World world;final net.minecraft.server.level.ServerPlayer player;
        final ContentB173.Registration registration;final net.minecraft.core.BlockPos origin;final ArrayList<net.minecraft.world.entity.Entity> entities=new ArrayList<>();
        final Map<net.minecraft.core.BlockPos,net.minecraft.world.level.block.state.BlockState> blocks=new LinkedHashMap<>();
        final net.minecraft.nbt.ListTag inventory=new net.minecraft.nbt.ListTag();final net.minecraft.world.phys.Vec3 previousPosition,previousMotion;final int selected;final Random random;final boolean shifted;
        Context(net.minecraft.server.level.ServerLevel level,ContentB173.Registration registration,int index){this.level=level;this.registration=registration;world=World.mcc$wrap(level);player=actor(level,index);previousPosition=player.position();previousMotion=player.getDeltaMovement();selected=player.getInventory().selected;shifted=player.isShiftKeyDown();player.setShiftKeyDown(false);player.getInventory().save(inventory);player.getInventory().clearContent();origin=level.getSharedSpawnPos().offset(8,24,8);level.getChunk(origin);player.setPos(origin.getX()+.5,origin.getY(),origin.getZ()+.5);player.setDeltaMovement(0,0,0);random=world.random;world.random=new ZeroRandom();}
        Class<?> type(String name)throws ClassNotFoundException{return Class.forName(registration.type().getPackageName()+"."+name,true,registration.type().getClassLoader());}
        LegacyB173Animal spawn(String name)throws ReflectiveOperationException {var original=(LivingEntity)type(name).getConstructor(World.class).newInstance(world);original.setPosition(origin.getX()+.5,origin.getY(),origin.getZ()+.5);require(world.spawnEntity(original),"original spawn rejected: "+name);require(original.mcc$modern instanceof LegacyB173Animal,"generic living host required");var host=(LegacyB173Animal)original.mcc$modern;entities.add(host);require(level.getEntities((net.minecraft.world.entity.Entity)null,host.getBoundingBox().inflate(.1)).contains(host),"fixture original host must enter loaded native entity query sections");put(original,"random",new ZeroRandom());return host;}
        void block(net.minecraft.core.BlockPos position,net.minecraft.world.level.block.state.BlockState state){blocks.putIfAbsent(position,level.getBlockState(position));level.setBlock(position,state,3);}
        void water(){for(int x=-3;x<=3;x++)for(int y=-2;y<=3;y++)for(int z=-3;z<=3;z++)block(origin.offset(x,y,z),net.minecraft.world.level.block.Blocks.WATER.defaultBlockState());}
        legacyconnector.vb1_7_3.net.minecraft.item.Item item(String field)throws ReflectiveOperationException{return (legacyconnector.vb1_7_3.net.minecraft.item.Item)registration.mod().instance.getClass().getField(field).get(null);}
        void hold(legacyconnector.vb1_7_3.net.minecraft.item.Item item,int count,int damage){player.setItemInHand(InteractionHand.MAIN_HAND,StacksB173.toModern(item.id,count,damage));}
        void useWhip(net.minecraft.core.BlockPos position)throws ReflectiveOperationException {hold(item("whip"),1,0);var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(position),net.minecraft.core.Direction.UP,position,false);require(player.getMainHandItem().getItem().useOn(new net.minecraft.world.item.context.UseOnContext(player,InteractionHand.MAIN_HAND,hit)).consumesAction(),"native whip use invokes original callback");require(player.getMainHandItem().getDamageValue()==1,"original whip consumes one durability");}
        net.minecraft.advancements.AdvancementHolder achievement(String name)throws ReflectiveOperationException {var achievement=(legacyconnector.vb1_7_3.net.minecraft.achievement.Achievement)registration.mod().instance.getClass().getField(name).get(null);var nativeAchievement=level.getServer().getAdvancements().get(AdvancementsB173.idOf(achievement));require(nativeAchievement!=null,"original achievement loaded from native pack: "+name);player.getAdvancements().revoke(nativeAchievement,AdvancementsB173.CRITERION);return nativeAchievement;}
        @Override public void close(){MenusB173.withActor(player,()->legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$instance().setScreen(null));player.stopRiding();player.closeContainer();for(var entity:entities)entity.discard();blocks.forEach((position,state)->level.setBlock(position,state,3));player.getInventory().clearContent();player.getInventory().load(inventory);player.getInventory().selected=selected;player.setPos(previousPosition.x,previousPosition.y,previousPosition.z);player.setDeltaMovement(previousMotion);world.random=random;player.setShiftKeyDown(shifted);EntityViewsB173.flush();player.discard();}
    }
    @FunctionalInterface private interface Proof {void run(Context context)throws ReflectiveOperationException;}
    private record Case(String name,Proof proof){}
    private static void run(net.minecraft.server.level.ServerLevel level){
        var selected=ContentB173.entities().stream().filter(r->r.type().getPackageName().equals("legacyconnector_b173.mocreatures")).findFirst();if(selected.isEmpty())return;
        var cases=List.of(new Case("actual item placements",MoCreaturesFeaturesSelfTestB173::placements),new Case("original egg hatch and pickup",MoCreaturesFeaturesSelfTestB173::eggs),new Case("whip tame controls and Indiana",MoCreaturesFeaturesSelfTestB173::whipCats),new Case("whip sign and BunnyKilla",MoCreaturesFeaturesSelfTestB173::whipBunnies),new Case("horse feeding riding and storage",MoCreaturesFeaturesSelfTestB173::horse),new Case("bird carry and dolphin riding",MoCreaturesFeaturesSelfTestB173::rides),new Case("werewolf golden weapon damage",MoCreaturesFeaturesSelfTestB173::werewolf),new Case("kitty bed food and litter cleaning",MoCreaturesFeaturesSelfTestB173::furniture),new Case("fed bigcat medallion taming",MoCreaturesFeaturesSelfTestB173::bigcatTame),new Case("dolphin raw-fish feeding",MoCreaturesFeaturesSelfTestB173::dolphinFeed),new Case("native horse rider input",MoCreaturesFeaturesSelfTestB173::riderInput),new Case("original untamed mount bucking",MoCreaturesFeaturesSelfTestB173::bucking),new Case("native water spawn placement",MoCreaturesFeaturesSelfTestB173::waterPlacement),new Case("original ogre destruction and flame attack",MoCreaturesFeaturesSelfTestB173::originalDestructionAndFire),new Case("original dolphin reproduction",MoCreaturesFeaturesSelfTestB173::originalDolphinReproduction),new Case("kitty bed feeding and offspring",MoCreaturesFeaturesSelfTestB173::kittyCycle),new Case("original horse breeding genetics",MoCreaturesFeaturesSelfTestB173::horseBreeding),new Case("dropped food and item age",MoCreaturesFeaturesSelfTestB173::predatorFood),new Case("original rat arrow retaliation",MoCreaturesFeaturesSelfTestB173::ratArrow));
        var extended=new ArrayList<>(cases);extended.add(new Case("retained FlameWraith target references",MoCreaturesFeaturesSelfTestB173::retainedFlameTargets));extended.add(new Case("inherited boar saddle and duck wings",MoCreaturesFeaturesSelfTestB173::inheritedFamilies));cases=List.copyOf(extended);
        extended.add(new Case("original bunny breeding query aliasing",MoCreaturesFeaturesSelfTestB173::bunnyQueries));cases=List.copyOf(extended);
        var failures=new ArrayList<String>();int index=0;
        for(var proof:cases)try(var context=new Context(level,selected.get(),index++)){proof.proof.run(context);LOG.info("B1.7.3 MOC FEATURES TEST: {} passed using original callbacks",proof.name);}
        catch(ReflectiveOperationException|RuntimeException|LinkageError error){failures.add(proof.name+": "+error);LOG.error("B1.7.3 MOC FEATURES TEST failed: {}",proof.name,error);}
        failures.addAll(MoCreaturesRosterSelfTestB173.failures());
        require(failures.isEmpty(),"B1.7.3 MOC FEATURES TEST: "+failures.size()+" baseline/gameplay failures: "+failures);
        LOG.info("B1.7.3 MOC FEATURES TEST: all {} specialized original-callback proofs passed",cases.size());
    }
    private static String interactionState(Context c,LegacyB173Animal host){return "held="+c.player.getMainHandItem()+" legacyHeld="+StacksB173.toLegacy(c.player.getMainHandItem())+" nativeVehicle="+c.player.getVehicle()+" legacyPassenger="+host.legacy().passenger+" removed="+host.isRemoved()+" playerAlive="+c.player.isAlive()+" playerShift="+c.player.isShiftKeyDown()+" worldRemote="+c.world.isRemote;}
    private static void placements(Context c)throws ReflectiveOperationException {
        String[][] mappings={{"sharkegg","EntitySharkEgg"},{"fishyegg","EntityFishyEgg"},{"kittybed","EntityKittyBed"},{"litterbox","EntityLitterBox"}};
        for(var mapping:mappings){var before=c.level.getEntities(c.player,c.player.getBoundingBox().inflate(4));c.hold(c.item(mapping[0]),2,mapping[0].equals("kittybed")?3:0);c.player.getMainHandItem().getItem().use(c.level,c.player,InteractionHand.MAIN_HAND);require(c.player.getMainHandItem().getCount()==1,"original "+mapping[0]+" consumes one item");
            var added=c.level.getEntities(c.player,c.player.getBoundingBox().inflate(4)).stream().filter(entity->!before.contains(entity)&&entity instanceof LegacyB173Animal host&&host.legacy().getClass().getSimpleName().equals(mapping[1])).toList();require(added.size()==1,"original "+mapping[0]+" spawns original "+mapping[1]);c.entities.addAll(added);
            var host=(LegacyB173Animal)added.getFirst();require(host.getDeltaMovement().y>.02&&host.getDeltaMovement().y<.03,"post-spawn original toss velocity reaches native host");host.discard();
        }
    }
    private static void eggs(Context c)throws ReflectiveOperationException {
        c.water();for(String species:List.of("Shark","Fishy")){
            var egg=c.spawn("Entity"+species+"Egg");put(egg.legacy(),"tCounter",49);var before=c.level.getEntities(egg,egg.getBoundingBox().inflate(4));egg.tick();
            require(egg.isRemoved(),"original wet "+species+" egg hatches at50");var babies=c.level.getEntities(egg,egg.getBoundingBox().inflate(4)).stream().filter(entity->!before.contains(entity)&&entity instanceof LegacyB173Animal host&&host.legacy().getClass().getSimpleName().equals("Entity"+species)).toList();require(babies.size()==1,"original egg spawns one "+species);c.entities.addAll(babies);
            var baby=(LegacyB173Animal)babies.getFirst();require((Boolean)get(baby.legacy(),"tamed")&&Math.abs((Float)get(baby.legacy(),"b")-.3F)<.0001,"original hatchling tame/growth fields");baby.discard();
            var pickup=c.spawn("Entity"+species+"Egg");put(pickup.legacy(),"lCounter",11);var beforeCount=c.player.getInventory().countItem(StacksB173.toModern(c.item(species.equals("Shark")?"sharkegg":"fishyegg").id,1,0).getItem());
            pickup.playerTouch(c.player);require(pickup.isRemoved()&&c.player.getInventory().countItem(StacksB173.toModern(c.item(species.equals("Shark")?"sharkegg":"fishyegg").id,1,0).getItem())==beforeCount+1,"native player touch executes original egg pickup callback");
        }
    }
    private static void whipCats(Context c)throws ReflectiveOperationException {
        var achievement=c.achievement("Indiana");var cats=new ArrayList<LegacyB173Animal>();for(int i=0;i<7;i++){var cat=c.spawn("EntityBigCat");put(cat.legacy(),"tamed",true);put(cat.legacy(),"sitting",false);cats.add(cat);}
        var horse=c.spawn("EntityHorse");put(horse.legacy(),"tamed",true);put(horse.legacy(),"eatinghaystack",false);
        c.block(c.origin.below(),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());c.block(c.origin,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());c.useWhip(c.origin.below());
        for(var cat:cats)require((Boolean)get(cat.legacy(),"sitting"),"original whip toggles every nearby tame cat");require((Boolean)get(horse.legacy(),"eatinghaystack"),"original whip toggles horse grazing");require(c.player.getAdvancements().getOrStartProgress(achievement).isDone(),"original seven-cat whip callback awards native Indiana advancement");
    }
    private static void whipBunnies(Context c)throws ReflectiveOperationException {
        var achievement=c.achievement("BunnyKilla");var bunnies=new ArrayList<LegacyB173Animal>();for(int i=0;i<70;i++)bunnies.add(c.spawn("EntityBunny"));
        c.block(c.origin.below(),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());c.block(c.origin,net.minecraft.world.level.block.Blocks.OAK_SIGN.defaultBlockState());c.useWhip(c.origin);
        require(bunnies.stream().allMatch(net.minecraft.world.entity.Entity::isRemoved),"original sign-whip callback removes all70 original bunnies");
        var sign=c.level.getBlockEntity(c.origin);require(sign instanceof net.minecraft.world.level.block.entity.SignBlockEntity,"native sign remains");var text=((net.minecraft.world.level.block.entity.SignBlockEntity)sign).getFrontText();require(text.getMessage(1,false).getString().equals("R.I.P.")&&text.getMessage(2,false).getString().equals("70 Bunnies"),"original sign texts reach native sign");
        require(c.player.getAdvancements().getOrStartProgress(achievement).isDone(),"original70-bunny callback awards native BunnyKilla advancement");
    }
    private static void horse(Context c)throws ReflectiveOperationException {
        var horse=c.spawn("EntityHorse");put(horse.legacy(),"adult",false);put(horse.legacy(),"b",.5F);put(horse.legacy(),"temper",100);horse.setHealth(Math.min(10,horse.getMaxHealth()-5));float before=horse.getHealth();
        c.player.setItemInHand(InteractionHand.MAIN_HAND,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.WHEAT,2));require(StacksB173.toLegacy(c.player.getMainHandItem())!=null&&StacksB173.toLegacy(c.player.getMainHandItem()).itemId==296,"native wheat maps to original item296 (not cropblock59): "+StacksB173.toLegacy(c.player.getMainHandItem()));require(horse.mobInteract(c.player,InteractionHand.MAIN_HAND).consumesAction(),"original horse feeding "+interactionState(c,horse));
        require(c.player.getMainHandItem().getCount()==1&&horse.getHealth()==before+5&&(Integer)get(horse.legacy(),"temper")==75&&Math.abs((Float)get(horse.legacy(),"b")-.51F)<.0001,"original wheat consumption/heal/temper/growth");
        put(horse.legacy(),"adult",true);put(horse.legacy(),"tamed",true);put(horse.legacy(),"rideable",true);c.player.setItemInHand(InteractionHand.MAIN_HAND,net.minecraft.world.item.ItemStack.EMPTY);require(horse.mobInteract(c.player,InteractionHand.MAIN_HAND).consumesAction()&&c.player.getVehicle()==horse,"original horse riding mounts native player");c.player.stopRiding();
        call(horse.legacy(),"setType",new Class<?>[]{int.class},6);require((Integer)get(horse.legacy(),"typeint")==6,"original packhorse type enables chest persistence");
        var stacks=new legacyconnector.vb1_7_3.net.minecraft.item.ItemStack[27];stacks[0]=new legacyconnector.vb1_7_3.net.minecraft.item.ItemStack(264,5,0);var chest=(legacyconnector.vb1_7_3.net.minecraft.inventory.Inventory)c.type("AnimalChest").getConstructor(stacks.getClass(),String.class).newInstance(stacks,"Horse chest");
        require(chest.size()==27&&chest.removeStack(0,2).count==2&&stacks[0].count==3,"original AnimalChest splits mutable inventory");put(horse.legacy(),"localstack",stacks);put(horse.legacy(),"chestedhorse",true);
        c.player.setItemInHand(InteractionHand.MAIN_HAND,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STONE_SHOVEL));require(horse.mobInteract(c.player,InteractionHand.MAIN_HAND).consumesAction()&&c.player.containerMenu instanceof net.minecraft.world.inventory.ChestMenu&&c.player.containerMenu.slots.size()==63,"original horse chest interaction opens native27-slot menu");
        c.player.containerMenu.clicked(0,0,net.minecraft.world.inventory.ClickType.PICKUP,c.player);require(stacks[0]==null&&c.player.containerMenu.getCarried().getCount()==3,"native chest pickup mutates original horse inventory");
        c.player.containerMenu.clicked(1,0,net.minecraft.world.inventory.ClickType.PICKUP,c.player);require(stacks[1]!=null&&stacks[1].count==3&&c.player.containerMenu.getCarried().isEmpty(),"native chest insertion mutates original horse inventory");c.player.closeContainer();
        var tag=new net.minecraft.nbt.CompoundTag();require(horse.save(tag),"horse chest saves");var restored=(LegacyB173Animal)net.minecraft.world.entity.EntityType.loadEntityRecursive(tag,c.level,e->e);require(restored!=null,"horse chest registry reload");c.entities.add(restored);var loaded=(legacyconnector.vb1_7_3.net.minecraft.item.ItemStack[])get(restored.legacy(),"localstack");require((Boolean)get(restored.legacy(),"chestedhorse")&&loaded[1]!=null&&loaded[1].count==3,"original horse chest contents persist");
    }
    private static void rides(Context c)throws ReflectiveOperationException {
        var bird=c.spawn("EntityBird");put(bird.legacy(),"tamed",true);c.player.setItemInHand(InteractionHand.MAIN_HAND,net.minecraft.world.item.ItemStack.EMPTY);require(bird.mobInteract(c.player,InteractionHand.MAIN_HAND).consumesAction()&&bird.getVehicle()==c.player&&(Boolean)get(bird.legacy(),"picked"),"original tame bird carry mounts bird on player");bird.mobInteract(c.player,InteractionHand.MAIN_HAND);require(bird.getVehicle()==null,"original bird second interaction releases");bird.discard();
        c.water();var dolphin=c.spawn("EntityDolphin");put(dolphin.legacy(),"adult",true);put(dolphin.legacy(),"tamed",true);require(dolphin.mobInteract(c.player,InteractionHand.MAIN_HAND).consumesAction()&&c.player.getVehicle()==dolphin,"original dolphin riding mounts native player in water "+interactionState(c,dolphin));c.player.stopRiding();
    }
    private static void werewolf(Context c)throws ReflectiveOperationException {
        var wolf=c.spawn("EntityWerewolf");put(wolf.legacy(),"humanform",false);float before=wolf.getHealth();c.player.setItemInHand(InteractionHand.MAIN_HAND,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_SWORD));require(wolf.hurt(c.level.damageSources().playerAttack(c.player),9)&&wolf.getHealth()==before-1,"original wolf form resists ordinary player weapons");wolf.invulnerableTime=0;c.player.setItemInHand(InteractionHand.MAIN_HAND,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.GOLDEN_SWORD));before=wolf.getHealth();require(wolf.hurt(c.level.damageSources().playerAttack(c.player),1)&&wolf.getHealth()==before-10,"original golden sword vulnerability applies10 native damage");
    }
    private static void furniture(Context c)throws ReflectiveOperationException {
        var bed=c.spawn("EntityKittyBed");c.player.setItemInHand(InteractionHand.MAIN_HAND,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.MILK_BUCKET));
        require(bed.mobInteract(c.player,InteractionHand.MAIN_HAND).consumesAction()&&(Boolean)get(bed.legacy(),"hasMilk")&&!(Boolean)get(bed.legacy(),"hasFood"),"original bed milk interaction changes contents");
        c.hold(c.item("petfood"),2,0);require(bed.mobInteract(c.player,InteractionHand.MAIN_HAND).consumesAction()&&c.player.getMainHandItem().getCount()==1&&(Boolean)get(bed.legacy(),"hasFood")&&!(Boolean)get(bed.legacy(),"hasMilk"),"original bed petfood consumes one and replaces milk");
        var litter=c.spawn("EntityLitterBox");put(litter.legacy(),"usedlitter",true);put(litter.legacy(),"littertime",100);c.player.setItemInHand(InteractionHand.MAIN_HAND,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.SAND,2));
        require(litter.mobInteract(c.player,InteractionHand.MAIN_HAND).consumesAction()&&c.player.getMainHandItem().getCount()==1&&!(Boolean)get(litter.legacy(),"usedlitter")&&(Integer)get(litter.legacy(),"littertime")==0,"original sand interaction cleans used litter and consumes one");
        c.player.setItemInHand(InteractionHand.MAIN_HAND,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STONE_PICKAXE));int before=c.player.getInventory().countItem(StacksB173.toModern(c.item("litterbox").id,1,0).getItem());
        require(litter.mobInteract(c.player,InteractionHand.MAIN_HAND).consumesAction()&&litter.isRemoved()&&c.player.getInventory().countItem(StacksB173.toModern(c.item("litterbox").id,1,0).getItem())==before+1,"original pickaxe retrieves native litter entity into inventory");
    }
    private static void bigcatTame(Context c)throws ReflectiveOperationException {
        var cat=c.spawn("EntityBigCat");put(cat.legacy(),"eaten",true);put(cat.legacy(),"tamed",false);c.hold(c.item("medallion"),2,0);
        require(cat.mobInteract(c.player,InteractionHand.MAIN_HAND).consumesAction()&&(Boolean)get(cat.legacy(),"tamed")&&c.player.getMainHandItem().getCount()==1,"original fed-cat medallion branch tames and consumes one "+interactionState(c,cat)+" tamed="+get(cat.legacy(),"tamed")+" eaten="+get(cat.legacy(),"eaten"));
        c.hold(c.item("whip"),1,0);put(cat.legacy(),"sitting",false);require(cat.mobInteract(c.player,InteractionHand.MAIN_HAND).consumesAction()&&(Boolean)get(cat.legacy(),"sitting"),"original direct whip interaction toggles tame cat sitting");
    }
    private static void dolphinFeed(Context c)throws ReflectiveOperationException {
        c.water();var dolphin=c.spawn("EntityDolphin");put(dolphin.legacy(),"adult",false);put(dolphin.legacy(),"b",.5F);put(dolphin.legacy(),"temper",100);dolphin.setHealth(5);float health=dolphin.getHealth();
        c.player.setItemInHand(InteractionHand.MAIN_HAND,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.COD,2));
        require(dolphin.mobInteract(c.player,InteractionHand.MAIN_HAND).consumesAction()&&c.player.getMainHandItem().getCount()==1&&(Integer)get(dolphin.legacy(),"temper")==75&&Math.abs((Float)get(dolphin.legacy(),"b")-.51F)<.0001&&dolphin.getHealth()==Math.min(health+15,(Integer)get(dolphin.legacy(),"maxhealth")),"original dolphin raw-fish consumption/temper/growth/healing");
    }

    private static void riderInput(Context c)throws ReflectiveOperationException {
        var horse=c.spawn("EntityHorse");call(horse.legacy(),"setType",new Class<?>[]{int.class},1);put(horse.legacy(),"adult",true);put(horse.legacy(),"tamed",true);put(horse.legacy(),"rideable",true);
        for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)c.block(c.origin.offset(x,-1,z),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        c.player.setItemInHand(InteractionHand.MAIN_HAND,net.minecraft.world.item.ItemStack.EMPTY);require(horse.mobInteract(c.player,InteractionHand.MAIN_HAND).consumesAction()&&c.player.getVehicle()==horse,"original ride mounts native rider before input proof "+interactionState(c,horse));
        var start=horse.position();double speed=(Double)get(horse.legacy(),"HorseSpeed");float yaw=c.player.getYRot(),pitch=c.player.getXRot();
        try{
            c.player.setYRot(0);c.player.setXRot(0);c.player.setPlayerInput(0,0,false,false);c.player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);horse.setOnGround(true);horse.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);horse.travel(net.minecraft.world.phys.Vec3.ZERO);
            double idle=horse.position().subtract(start).horizontalDistance();horse.setPos(start.x,start.y,start.z);horse.setOnGround(true);horse.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);put(horse.legacy(),"isjumping",false);
            c.player.setPlayerInput(0,1,true,false);c.player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);horse.travel(net.minecraft.world.phys.Vec3.ZERO);
            require(horse.position().subtract(start).horizontalDistance()>idle+.001&&horse.getY()>start.y+.2&&(Boolean)get(horse.legacy(),"isjumping"),"native forward/jump input reaches original horse travel and jump mechanics");
            require((Double)get(horse.legacy(),"HorseSpeed")==speed,"rider bridge retains original horse speed");
            c.player.setDeltaMovement(.13,0,.07);var playerView=PlayerViewB173.of(c.player);require(Math.abs(playerView.velocityX-.13)<.00001&&Math.abs(playerView.velocityZ-.07)<.00001,"rider bridge preserves existing nonzero native player velocity");
        }finally{c.player.setPlayerInput(0,0,false,false);c.player.setYRot(yaw);c.player.setXRot(pitch);c.player.stopRiding();}
    }

    private static void bucking(Context c)throws ReflectiveOperationException {
        for(String species:List.of("EntityHorse","EntityDolphin")){
            if(species.equals("EntityDolphin"))c.water();var host=c.spawn(species);put(host.legacy(),"tamed",false);put(host.legacy(),"adult",true);
            put(host.legacy(),"random",new Random(){@Override public int nextInt(int bound){return bound==50?0:bound-1;}@Override public float nextFloat(){return .5F;}});
            require(c.player.startRiding(host,true),"native real player mounts original untamed "+species);c.player.setPlayerInput(0,0,false,false);c.player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);host.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);host.travel(net.minecraft.world.phys.Vec3.ZERO);
            require(c.player.getVehicle()==null&&host.getFirstPassenger()==null&&host.legacy().passenger==null,"original "+species+" direct passenger=null buck detaches native relation");
            require(c.player.getDeltaMovement().y>=.89&&c.player.getDeltaMovement().z<=-.29,"original "+species+" buck impulse reaches native rider");host.discard();
        }
    }
    private static void waterPlacement(Context c)throws ReflectiveOperationException {
        var fish=c.spawn("EntityFishy");var placement=net.minecraft.world.entity.SpawnPlacements.getPlacementType(fish.getType());
        require(placement==net.minecraft.world.entity.SpawnPlacementTypes.IN_WATER,"original water family registered with native water placement");
        require(!placement.isSpawnPositionOk(c.level,c.origin,fish.getType()),"native water-family placement rejects dry air");c.water();
        require(placement.isSpawnPositionOk(c.level,c.origin,fish.getType()),"native water-family placement accepts controlled water");
    }

private static void originalDestructionAndFire(Context c)throws ReflectiveOperationException {
    // Explicitly exercise the original Destroyer utility; renderer/admission/3-tick roster checks do not enter it.
    var ogre=c.spawn("EntityOgre");put(ogre.legacy(),"destroyForce",3F);
    var center=c.origin.above();
    for(int x=-4;x<=4;x++)for(int y=-3;y<=3;y++)for(int z=-4;z<=4;z++)c.block(center.offset(x,y,z),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
    for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)c.block(center.offset(x,-1,z),net.minecraft.world.level.block.Blocks.DIRT.defaultBlockState());
    c.block(center.offset(1,0,0),net.minecraft.world.level.block.Blocks.DIRT.defaultBlockState());
    var resistant=center.offset(-1,0,0);c.block(resistant,net.minecraft.world.level.block.Blocks.BEDROCK.defaultBlockState());
    var watched=new ArrayList<net.minecraft.core.BlockPos>();for(var entry:c.blocks.entrySet())if(c.level.getBlockState(entry.getKey()).is(net.minecraft.world.level.block.Blocks.DIRT))watched.add(entry.getKey());
    var beforeItems=c.level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(center).inflate(8));
    call(ogre.legacy(),"DestroyingOgre",new Class<?>[]{});EntityViewsB173.flush();
    require(watched.stream().anyMatch(p->c.level.getBlockState(p).isAir()),"original DestroyBlast destroys nearby low-resistance blocks at their real coordinates");
    require(c.level.getBlockState(resistant).is(net.minecraft.world.level.block.Blocks.BEDROCK),"original resistance traversal preserves bedrock");
    var dropped=c.level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(center).inflate(8));
    for(var item:dropped)if(!beforeItems.contains(item))c.entities.add(item);
    // Do NOT assert fire blocks from Destroyer itself: original mapped code passes FIRE.textureId to setBlockMetaWithoutNotifyingNeighbors.
    // Keep that original behavior until raw original/mapping evidence proves an API mapping defect.
    var flame=c.spawn("EntityFlameWraith");var victim=c.spawn("EntityBear");
    flame.setPos(c.origin.getX()+.5,c.origin.getY(),c.origin.getZ()+.5);victim.setPos(c.origin.getX()+1,c.origin.getY(),c.origin.getZ()+.5);
    flame.syncView();put(flame.legacy(),"attackCooldown",0);float health=victim.getHealth();
    try(var scope=EntityViewsB173.callbackScope()){var victimView=EntityViewsB173.wrap(victim);((legacyconnector.vb1_7_3.net.minecraft.entity.MobEntity)flame.legacy()).mcc$attack(victimView,.5F);flame.flushView();}
    require(victim.getHealth()==health-2&&victim.getRemainingFireTicks()==(Integer)get(flame.legacy(),"burningTime"),"original FlameWraith contact attack damages and sets the target fire duration");
    require(flame.fireImmune(),"native host preserves original fire immunity");
    health=flame.getHealth();flame.hurt(c.level.damageSources().inFire(),2);require(flame.getHealth()==health,"native fire damage honors original immunity");
}

private static void originalDolphinReproduction(Context c)throws ReflectiveOperationException {
    c.water();var a=c.spawn("EntityDolphin");var b=c.spawn("EntityDolphin");
    a.setPos(c.origin.getX()+.5,c.origin.getY(),c.origin.getZ()+.5);b.setPos(c.origin.getX()+1.5,c.origin.getY(),c.origin.getZ()+.5);
    for(var dolphin:List.of(a,b)){put(dolphin.legacy(),"adult",true);put(dolphin.legacy(),"tamed",true);put(dolphin.legacy(),"eaten",true);put(dolphin.legacy(),"typechosen",true);put(dolphin.legacy(),"typeint",2);put(dolphin.legacy(),"gestationtime",50);dolphin.syncView();}
    // Both original queries deliberately have identical membership: original source indexes its inner list using outer list size.
    var before=c.level.getEntitiesOfClass(LegacyB173Animal.class,a.getBoundingBox().inflate(8));
    a.tick();
    var after=c.level.getEntitiesOfClass(LegacyB173Animal.class,a.getBoundingBox().inflate(8));var children=after.stream().filter(e->!before.contains(e)&&e.legacy().getClass()==a.legacy().getClass()).toList();c.entities.addAll(children);
    require(children.size()==1,"original dolphin ready parents and gestation threshold spawn one offspring");
    var child=children.getFirst().legacy();require(children.getFirst().renderState().equals(AnimalRenderStateB173.capture(child)),"post-spawn original dolphin fields reach initial native tracking state");require(!(Boolean)get(child,"adult")&&(Boolean)get(child,"bred")&&Math.abs((Float)get(child,"b")-.35F)<.0001&&(Integer)get(child,"typeint")==2,"post-spawn original offspring growth/type/bred mutations survive host baseline and synchronization");
    require(!(Boolean)get(a.legacy(),"eaten")&&!(Boolean)get(b.legacy(),"eaten")&&(Integer)get(a.legacy(),"gestationtime")==0&&(Integer)get(b.legacy(),"gestationtime")==0,"original dolphin parents consume readiness and reset gestation");
}


    private static void kittyCycle(Context c)throws ReflectiveOperationException {
        var bed=c.spawn("EntityKittyBed");var kitty=c.spawn("EntityKitty");put(kitty.legacy(),"adult",true);put(kitty.legacy(),"kittystate",4);put(kitty.legacy(),"hungry",false);
        put(kitty.legacy(),"random",new Random(){@Override public int nextInt(int bound){return bound==3?0:bound-1;}});require(kitty.startRiding(bed,true),"native kitty occupies actual bed");
        put(bed.legacy(),"hasFood",true);put(bed.legacy(),"milklevel",1.999F);bed.tick();
        require(!(Boolean)get(bed.legacy(),"hasFood")&&!(Boolean)get(bed.legacy(),"hasMilk")&&Math.abs((Float)get(bed.legacy(),"milklevel"))<.0001&&bed.getFirstPassenger()==kitty,"original occupied bed consumes its food at depletion threshold");
        kitty.setHealth(2);kitty.tick();require((Integer)get(kitty.legacy(),"kittystate")==5&&kitty.getVehicle()==null&&kitty.getHealth()==(Integer)get(kitty.legacy(),"maxhealth"),"original kitty feeding completion heals, changes state and leaves depleted bed");
        require(kitty.startRiding(bed,true),"native kitty reoccupies bed for original pregnancy state");put(kitty.legacy(),"kittystate",20);put(kitty.legacy(),"kittytimer",1000);
        var before=c.level.getEntitiesOfClass(LegacyB173Animal.class,kitty.getBoundingBox().inflate(8));kitty.tick();
        var offspring=c.level.getEntitiesOfClass(LegacyB173Animal.class,kitty.getBoundingBox().inflate(8)).stream().filter(entity->!before.contains(entity)&&entity.legacy().getClass()==kitty.legacy().getClass()).toList();c.entities.addAll(offspring);
        require(offspring.size()==1,"original controlled kitty pregnancy produces one kitten");var child=offspring.getFirst();require(!(Boolean)get(child.legacy(),"adult")&&(Integer)get(child.legacy(),"kittystate")==10&&Math.abs((Float)get(child.legacy(),"edad")-.4F)<.0001,"original kitten post-spawn juvenile state and growth");
        require(kitty.getVehicle()==null&&(Integer)get(kitty.legacy(),"kittystate")==21,"original mother changes state and releases native bed mount");require(child.renderState().equals(AnimalRenderStateB173.capture(child.legacy())),"post-spawn original kitten fields reach initial native tracking state");
    }
    private static void horseBreeding(Context c)throws ReflectiveOperationException {
        var a=c.spawn("EntityHorse");var b=c.spawn("EntityHorse");b.setPos(c.origin.getX()+1.5,c.origin.getY(),c.origin.getZ()+.5);
        for(var parent:List.of(a,b)){call(parent.legacy(),"setType",new Class<?>[]{int.class},1);put(parent.legacy(),"adult",true);put(parent.legacy(),"tamed",true);put(parent.legacy(),"eatenpumpkin",true);put(parent.legacy(),"hasreproduced",false);put(parent.legacy(),"gestationtime",50);parent.syncView();}
        var before=c.level.getEntitiesOfClass(LegacyB173Animal.class,a.getBoundingBox().inflate(8));a.tick();
        var offspring=c.level.getEntitiesOfClass(LegacyB173Animal.class,a.getBoundingBox().inflate(8)).stream().filter(entity->!before.contains(entity)&&entity.legacy().getClass()==a.legacy().getClass()).toList();c.entities.addAll(offspring);
        require(offspring.size()==1,"original ready horse pair produces one foal at gestation threshold");var child=offspring.getFirst();require(!(Boolean)get(child.legacy(),"adult")&&(Boolean)get(child.legacy(),"bred")&&(Integer)get(child.legacy(),"typeint")==1,"original HorseGenetics keeps same-type pair and post-spawn foal flags");
        require(!(Boolean)get(a.legacy(),"eatenpumpkin")&&!(Boolean)get(b.legacy(),"eatenpumpkin")&&(Integer)get(a.legacy(),"gestationtime")==0&&(Integer)get(b.legacy(),"gestationtime")==0,"original horse parents consume readiness and reset gestation");require(child.renderState().equals(AnimalRenderStateB173.capture(child.legacy())),"post-spawn original foal fields reach initial native tracking state");
    }
    private static void predatorFood(Context c)throws ReflectiveOperationException {
        var cat=c.spawn("EntityBigCat");cat.tick();
        require((Boolean)get(cat.legacy(),"typechosen")&&(Integer)get(cat.legacy(),"typeint")>0&&cat.getHealth()==(Integer)get(cat.legacy(),"maxhealth"),"original renderer initializes authoritative predator type and health before injury/feeding");
        put(cat.legacy(),"adult",false);put(cat.legacy(),"edad",.5F);put(cat.legacy(),"hungry",true);put(cat.legacy(),"sitting",false);cat.setHealth(5);
        var food=new net.minecraft.world.entity.item.ItemEntity(c.level,cat.getX()+.2,cat.getY(),cat.getZ(),new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.COD));require(c.level.addFreshEntity(food),"native dropped food enters world");c.entities.add(food);cat.tick();
        require(food.isRemoved()&&cat.getHealth()==15&&(Boolean)get(cat.legacy(),"eaten"),"original juvenile predator eats native dropped fish, heals and enters taming readiness");
        var bear=c.spawn("EntityBear");var fresh=new net.minecraft.world.entity.item.ItemEntity(c.level,bear.getX()+.2,bear.getY(),bear.getZ(),new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.PORKCHOP));var old=new net.minecraft.world.entity.item.ItemEntity(c.level,bear.getX()+.4,bear.getY(),bear.getZ(),new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.PORKCHOP));require(c.level.addFreshEntity(fresh)&&c.level.addFreshEntity(old),"native age-test drops enter world");c.entities.add(fresh);c.entities.add(old);EntityFieldsB173.itemAge(fresh,49);EntityFieldsB173.itemAge(old,50);
        var setting=(legacyconnector.vb1_7_3.net.minecraft.src.SettingBoolean)c.registration.mod().instance.getClass().getField("destroyitems").get(null);var saved=new HashMap<>(setting.values);setting.values.put(legacyconnector.vb1_7_3.net.minecraft.src.ModSettings.currentContext,true);
        try{call(bear.legacy(),"destroyDrops",new Class<?>[]{legacyconnector.vb1_7_3.net.minecraft.entity.Entity.class,double.class},bear.legacy(),4D);EntityViewsB173.flush();require(fresh.isRemoved()&&!old.isRemoved(),"original predator drop cleanup reads native item age and removes only age below50");}finally{setting.values.clear();setting.values.putAll(saved);}
    }
    private static void ratArrow(Context c)throws ReflectiveOperationException {
        var rat=c.spawn("EntityRat");var neighbor=c.spawn("EntityRat");var arrow=new net.minecraft.world.entity.projectile.Arrow(net.minecraft.world.entity.EntityType.ARROW,c.level);arrow.setOwner(c.player);arrow.setPos(c.origin.getX(),c.origin.getY(),c.origin.getZ());c.entities.add(arrow);
        var view=EntityViewsB173.wrap(arrow);require(view instanceof legacyconnector.vb1_7_3.net.minecraft.entity.projectile.ArrowEntity&&((legacyconnector.vb1_7_3.net.minecraft.entity.projectile.ArrowEntity)view).owner.mcc$modern==c.player,"native arrow view preserves typed original shooter");
        require(rat.legacy().damage(view,1),"original rat arrow-damage callback accepted");EntityViewsB173.flush();
        require(((legacyconnector.vb1_7_3.net.minecraft.entity.MobEntity)rat.legacy()).target==view&&((legacyconnector.vb1_7_3.net.minecraft.entity.MobEntity)neighbor.legacy()).target.mcc$modern==c.player,"original struck rat retains arrow target while nearby pack recruits arrow owner");
    }

    private static void retainedFlameTargets(Context c)throws ReflectiveOperationException {
        // Newly constructed real ServerPlayers have a separate sixty-tick spawn protection timer.
        var protection=net.minecraft.server.level.ServerPlayer.class.getDeclaredField("spawnInvulnerableTime");protection.setAccessible(true);protection.setInt(c.player,0);
        for(boolean ordinary:new boolean[]{false,true}){
            var flame=c.spawn("EntityFlameWraith");var bear=ordinary?null:c.spawn("EntityBear");
            net.minecraft.world.entity.LivingEntity victim=ordinary?c.player:bear;
            var target=ordinary?PlayerViewB173.of(c.player):bear.legacy();
            ((legacyconnector.vb1_7_3.net.minecraft.entity.MobEntity)flame.legacy()).target=target;EntityViewsB173.flush();
            // Move the native target after retaining its original reference. The tick must refresh it without a new query/wrap.
            flame.setPos(c.origin.getX()+.5,c.origin.getY(),c.origin.getZ()+.5);victim.setPos(c.origin.getX()+1,c.origin.getY(),c.origin.getZ()+.5);
            victim.setHealth(10);victim.invulnerableTime=0;victim.setRemainingFireTicks(0);put(flame.legacy(),"attackCooldown",0);
            float health=victim.getHealth();flame.tick();
            require(((legacyconnector.vb1_7_3.net.minecraft.entity.MobEntity)flame.legacy()).target==target,"original retained FlameWraith target identity survives tick");
            require(victim.getHealth()==health-2&&victim.getRemainingFireTicks()==(Integer)get(flame.legacy(),"burningTime"),"actual FlameWraith tick refreshes retained "+(ordinary?"player":"host")+" target and publishes damage/fire: health="+victim.getHealth()+" fire="+victim.getRemainingFireTicks());
            require(Math.abs(target.x-victim.getX())<.001,"retained target reads current native pose");flame.discard();if(bear!=null)bear.discard();
        }
    }

    private static LegacyB173Animal trackedCopy(Context c,LegacyB173Animal source){
        var info=AnimalsB173.all().stream().filter(entry->entry.registration().type()==source.legacy().getClass()).findFirst().orElseThrow();var copy=new LegacyB173Animal(info.type(),c.level,null);c.entities.add(copy);
        var values=source.getEntityData().getNonDefaultValues();require(values!=null,"inherited family tracking snapshot exists");
        var buffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),c.level.registryAccess());
        try{net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket.STREAM_CODEC.encode(buffer,new net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket(source.getId(),values));copy.getEntityData().assignValues(net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket.STREAM_CODEC.decode(buffer).packedItems());copy.applyTrackedRenderData();}finally{buffer.release();}
        return copy;
    }
    private static void bunnyQueries(Context c)throws ReflectiveOperationException {
        var bunny=c.spawn("EntityBunny");
        var nearby=new net.minecraft.world.entity.animal.Cow(net.minecraft.world.entity.EntityType.COW,c.level);nearby.setNoAi(true);
        nearby.setPos(bunny.getX()+8,bunny.getY(),bunny.getZ());require(c.level.addFreshEntity(nearby),"wide-query native Cow enters query sections");c.entities.add(nearby);
        var wideBox=bunny.legacy().boundingBox.expand(16,16,16);var nearBox=bunny.legacy().boundingBox.expand(1,1,1);
        var wide=c.world.getEntities(bunny.legacy(),wideBox);require(wide.contains(EntityViewsB173.wrap(nearby)),"wide original query sees native Cow");
        var typed=c.world.collectEntitiesByClass(legacyconnector.vb1_7_3.net.minecraft.entity.passive.CowEntity.class,wideBox);
        require(typed!=wide&&typed.contains(EntityViewsB173.wrap(nearby))&&wide.contains(EntityViewsB173.wrap(nearby)),"typed query owns a separate result without clearing untyped buffer");
        var narrow=c.world.getEntities(bunny.legacy(),nearBox);require(narrow==wide&&wide.isEmpty(),"narrow original query replaces the same mutable wide-query list: same="+(narrow==wide)+" nearby="+narrow.stream().map(e->e.getClass().getName()+"@"+e.x+","+e.y+","+e.z).toList());
        narrow.add(bunny.legacy());require(wide.contains(bunny.legacy()),"untyped original query result is mutable");
        c.world.getEntities(bunny.legacy(),nearBox);require(wide.isEmpty()&&typed.contains(EntityViewsB173.wrap(nearby)),"next untyped query clears mutations while typed snapshot survives");
        put(bunny.legacy(),"tamed",true);put(bunny.legacy(),"adult",true);put(bunny.legacy(),"j",1023);put(bunny.legacy(),"i",127);
        EntityViewsB173.flush();bunny.tick();require(bunny.isAlive(),"actual original breeding tick with wide Cow and empty narrow query does not crash");
    }
    private static List<String> familyGeometry(LegacyB173Animal host){
        var result=new ArrayList<String>();AnimalRenderingB173.draw(host,.5F,(draw,texture)->{for(var box:draw.cuboids())for(var face:ModelGeometryB173.vertices(box))for(var vertex:face){var p=draw.transform().transformPosition(new org.joml.Vector3f(vertex.x()*draw.scale(),vertex.y()*draw.scale(),vertex.z()*draw.scale()));require(Float.isFinite(p.x)&&Float.isFinite(p.y)&&Float.isFinite(p.z),"inherited renderer finite geometry");result.add(texture+":"+p.x+":"+p.y+":"+p.z);}});require(!result.isEmpty(),"inherited original renderer emits geometry");return result;
    }
    private static void inheritedFamilies(Context c)throws ReflectiveOperationException {
        var boar=c.spawn("EntityBoar");var bare=familyGeometry(boar);var saddle=new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.SADDLE);
        require(saddle.getItem().interactLivingEntity(saddle,c.player,boar,InteractionHand.MAIN_HAND).consumesAction()&&saddle.isEmpty(),"native vanilla saddle item equips original Boar");
        require((Boolean)get(boar.legacy(),"saddled"),"native saddle writes original pig family flag");
        c.player.setItemInHand(InteractionHand.MAIN_HAND,net.minecraft.world.item.ItemStack.EMPTY);require(boar.mobInteract(c.player,InteractionHand.MAIN_HAND).consumesAction()&&c.player.getVehicle()==boar,"original saddled Boar interaction mounts native player");c.player.stopRiding();
        var tag=new net.minecraft.nbt.CompoundTag();require(boar.save(tag),"saddled Boar saves");var loaded=(LegacyB173Animal)net.minecraft.world.entity.EntityType.loadEntityRecursive(tag,c.level,e->e);require(loaded!=null,"saddled Boar registry reload");c.entities.add(loaded);require((Boolean)get(loaded.legacy(),"saddled"),"original pig saddle flag persists");
        var copy=trackedCopy(c,boar);require((Boolean)get(copy.legacy(),"saddled"),"client tracking carries inherited pig saddle flag");var saddled=familyGeometry(copy);require(saddled.size()>bare.size(),"actual pig saddle renderer adds geometry after client tracking");
        var duck=c.spawn("EntityDuck");var duckCopy=trackedCopy(c,duck);var resting=familyGeometry(duckCopy);duck.setOnGround(false);duck.setDeltaMovement(0,-.1,0);duck.tick();
        require((Float)get(duck.legacy(),"flapProgress")>0&&(Float)get(duck.legacy(),"maxWingDeviation")>0,"actual Duck tick updates inherited chicken wing phase");
        duckCopy=trackedCopy(c,duck);for(String field:new String[]{"flapProgress","prevFlapProgress","maxWingDeviation","prevMaxWingDeviation"})require(get(duckCopy.legacy(),field).equals(get(duck.legacy(),field)),"client tracking carries inherited chicken field "+field);
        require(!familyGeometry(duckCopy).equals(resting),"actual client Duck geometry animates from inherited wing snapshot");
    }

}
