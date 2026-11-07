package org.kylepitcock.legacyconnector.compat.b173;

import org.kylepitcock.legacyconnector.vb1_7_3.*;
import legacyconnector.vb1_7_3.net.minecraft.item.Item;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

/** Exercises user-supplied mod bytecode, not replacement test implementations. Only called by the development self test. */
public final class BucketBowSelfTest {
    private static final org.slf4j.Logger LOG=org.slf4j.LoggerFactory.getLogger("LegacyConnector/b1.7.3");
    public static void run(ServerLevel level, ServerPlayer player, BlockPos base) {
        BlockPos pos=base.offset(512,-20,0);
        int passed=0, failed=0;
        for (Item item : ContentB173.items()) {
            if (!(item.mcc$modern instanceof LegacyB173Item)) continue;
            String name=item.getClass().getSimpleName();
            if (!name.equals("ItemNBucket") && !name.equals("ItemSkyrootBucket")) continue;
            try {
                if (name.equals("ItemNBucket")) {
                    var field=item.getClass().getDeclaredField("isFull"); field.setAccessible(true);
                    if (field.getInt(item)!=0) continue;
                }
                bucket(level,player,pos,item,name.equals("ItemSkyrootBucket")); passed++;
                LOG.info("B1.7.3 ACTUAL BUCKET TEST: {} source pickup, placement, flowing rejection, miss and permission checks passed",name);
            } catch (Exception | LinkageError e) { failed++; LOG.error("Actual bucket test failed: {}",name,e); }
        }
        for (Item item : ContentB173.items()) {
            if (!(item.mcc$modern instanceof LegacyB173Item)) continue;
            String name=item.getClass().getSimpleName();
            if (!java.util.Set.of("ItemNetherbow","ItemLbow","ItemNbow","ItemPbow","ItemPhoenixBow").contains(name)) continue;
            try {
                if (name.equals("ItemPhoenixBow")) bow(level,player,pos,item,262,true);
                else {
                    Class<?> mod=Class.forName(item.getClass().getPackageName()+".mod_Nether",false,item.getClass().getClassLoader());
                    for (String field : java.util.List.of("larrowItem","parrowItem","narrowItem","netherarrowItem"))
                        bow(level,player,pos,item,((Item)mod.getField(field).get(null)).id,false);
                }
                passed++; LOG.info("B1.7.3 ACTUAL BOW TEST: {} ammo, launch, flight, damage and no-ammo checks passed",name);
            } catch (Exception | LinkageError e) { failed++; LOG.error("Actual bow test failed: {}",name,e); }
        }
        player.getInventory().clearContent();
        LOG.info("B1.7.3 ACTUAL BUCKET/BOW TEST: {} items passed, {} failed",passed,failed);
        if (failed!=0) throw new IllegalStateException("Actual bucket/bow regression failures: "+failed);
    }
    private static void equip(ServerPlayer p, Item item) {
        p.getInventory().clearContent(); p.getInventory().selected=0;
        p.setItemInHand(InteractionHand.MAIN_HAND,StacksB173.toModern(item.id,1,0));
    }
    private static void use(ServerLevel level, ServerPlayer p) { p.getMainHandItem().getItem().use(level,p,InteractionHand.MAIN_HAND); }
    private static void bucket(ServerLevel level,ServerPlayer p,BlockPos pos,Item item,boolean water) {
        var original=level.getBlockState(pos); var floor=level.getBlockState(pos.below());
        try {
            equip(p,item); p.setPos(pos.getX()+0.5,pos.getY()+2,pos.getZ()+0.5);p.setXRot(90);p.setYRot(0);
            level.setBlock(pos.below(),Blocks.STONE.defaultBlockState(),2);
            var liquid=water?Blocks.WATER:Blocks.LAVA;
            level.setBlock(pos,liquid.defaultBlockState(),2); use(level,p);
            require(level.getBlockState(pos).isAir(),"source removed");
            var full=StacksB173.toLegacy(p.getMainHandItem());
            require(full!=null && (water?full.getDamage()==8:full.itemId!=item.id),"filled bucket retained");
            use(level,p);
            require(level.getBlockState(pos).is(liquid),"liquid placed on clicked face");
            var empty=StacksB173.toLegacy(p.getMainHandItem()); require(empty.itemId==item.id && empty.getDamage()==0,"empty bucket returned");
            level.setBlock(pos,liquid.defaultBlockState().setValue(net.minecraft.world.level.block.LiquidBlock.LEVEL,3),2);
            use(level,p); require(StacksB173.toLegacy(p.getMainHandItem()).itemId==item.id && StacksB173.toLegacy(p.getMainHandItem()).getDamage()==0,"flowing fluid not collected");
            p.setXRot(-90);use(level,p);require(StacksB173.toLegacy(p.getMainHandItem()).getDamage()==0,"miss unchanged");p.setXRot(90);
            level.setBlock(pos,liquid.defaultBlockState(),2); p.getAbilities().mayBuild=false;
            use(level,p);require(level.getBlockState(pos).is(liquid),"permission denial leaves fluid");
        } finally { p.getAbilities().mayBuild=true;level.setBlock(pos,original,2);level.setBlock(pos.below(),floor,2); }
    }
    private static void bow(ServerLevel level,ServerPlayer p,BlockPos pos,Item item,int ammo,boolean flaming) {
        equip(p,item);p.setPos(pos.getX()+0.5,pos.getY(),pos.getZ()+0.5);p.setYRot(0);p.setXRot(0);
        AABB area=new AABB(pos).inflate(30);
        use(level,p);require(level.getEntitiesOfClass(LegacyB173Arrow.class,area).isEmpty(),"no ammo means no arrow");
        p.getInventory().items.set(1,StacksB173.toModern(ammo,2,0));
        var target=net.minecraft.world.entity.EntityType.ZOMBIE.create(level);
        target.setPos(pos.getX()+0.5,pos.getY(),pos.getZ()+4.5);target.setNoAi(true);level.addFreshEntity(target);
        try {
            use(level,p);
            var arrows=level.getEntitiesOfClass(LegacyB173Arrow.class,area);
            require(arrows.size()==1,"one arrow spawned");require(p.getInventory().items.get(1).getCount()==1,"one ammo consumed");
            var arrow=arrows.getFirst();var start=arrow.position();float health=target.getHealth();
            require(!arrow.texture().equals(ProjectileTexturesB173.FALLBACK),"mod projectile texture selected: "+arrow.legacy.getClass());
            var assets=new java.util.HashMap<String,java.util.function.Supplier<byte[]>>();
            ProjectileTexturesB173.contribute(assets);
            var texture=assets.get("assets/"+arrow.texture().getNamespace()+"/"+arrow.texture().getPath());
            require(texture!=null && texture.get().length>8,"projectile PNG included in resource pack");
            String source=switch(arrow.legacy.getClass().getSimpleName()) {
                case "EntityLarrow" -> "item/larrows.png";
                case "EntityNarrow" -> "item/narrows.png";
                case "EntityParrow" -> "item/parrows.png";
                case "EntityNetherArrow" -> "item/netherarrows.png";
                case "EntityFlamingArrow" -> "aether/other/FlamingArrows.png";
                default -> throw new IllegalStateException("Unknown test arrow "+arrow.legacy.getClass());
            };
            try(var stream=arrow.legacy.getClass().getModule().getResourceAsStream(source)) {
                require(stream!=null && java.util.Arrays.equals(texture.get(),stream.readAllBytes()),"original mod PNG bytes retained");
            } catch(java.io.IOException e) { throw new IllegalStateException(e); }
            var saved=new net.minecraft.nbt.CompoundTag(); arrow.save(saved);
            var restored=net.minecraft.world.entity.EntityType.loadEntityRecursive(saved,level,e -> e);
            require(restored instanceof LegacyB173Arrow copy && copy.legacy!=null && copy.legacy.getClass()==arrow.legacy.getClass(),"arrow saves and reloads as same mod class");
            require(((LegacyB173Arrow)restored).texture().equals(arrow.texture()),"custom texture survives reload");
            restored.discard();
            for(int tick=0;tick<20 && !arrow.isRemoved() && target.getHealth()==health;tick++) arrow.tick();
            require(arrow.position().distanceTo(start)>0.1,"arrow moves");
            require(target.getHealth()<health,"legacy damage reaches modern mob");
            if(flaming) require(target.getRemainingFireTicks()>0,"flaming arrow ignites mob");
        } finally { target.discard();for(var arrow:level.getEntitiesOfClass(LegacyB173Arrow.class,area))arrow.discard();
            if(level.getBlockState(pos.offset(0,0,4)).is(Blocks.FIRE))level.removeBlock(pos.offset(0,0,4),false);
        }
        // Solid wall: the legacy arrow must stop, then its own pickup callback returns its ammunition.
        var old=new java.util.LinkedHashMap<BlockPos,net.minecraft.world.level.block.state.BlockState>();
        for(int x=-1;x<=1;x++)for(int y=0;y<4;y++) {
            BlockPos at=pos.offset(x,y,4);old.put(at,level.getBlockState(at));level.setBlock(at,Blocks.STONE.defaultBlockState(),2);
        }
        try {
            equip(p,item);p.getInventory().items.set(1,StacksB173.toModern(ammo,1,0));use(level,p);
            var arrows=level.getEntitiesOfClass(LegacyB173Arrow.class,area);require(arrows.size()==1,"wall shot spawned");
            var arrow=arrows.getFirst();
            for(int tick=0;tick<25 && !arrow.isRemoved();tick++)arrow.tick();
            require(!arrow.isRemoved() && arrow.getZ()<pos.getZ()+4.2,"wall stops arrow");
            var before=arrow.position();arrow.tick();require(arrow.position().distanceTo(before)<0.001,"embedded arrow stays put");
            arrow.playerTouch(p);require(arrow.isRemoved(),"legacy pickup removes arrow");
            require(p.getInventory().items.stream().anyMatch(s -> !s.isEmpty() && StacksB173.toLegacy(s)!=null && StacksB173.toLegacy(s).itemId==ammo),"pickup returns correct ammunition");
        } finally {
            for(var arrow:level.getEntitiesOfClass(LegacyB173Arrow.class,area))arrow.discard();
            old.forEach((at,state)->level.setBlock(at,state,2));
        }
    }
    private static void require(boolean ok,String message) { if(!ok)throw new IllegalStateException(message); }
}
