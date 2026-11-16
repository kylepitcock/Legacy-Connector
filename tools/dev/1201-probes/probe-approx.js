function say(s) { log.info("APROBE " + s); }
var RL = Java.type("net.minecraft.resources.ResourceLocation");
var BI = Java.type("net.minecraft.core.registries.BuiltInRegistries");
var Registries = Java.type("net.minecraft.core.registries.Registries");
var ResourceKey = Java.type("net.minecraft.resources.ResourceKey");
var BlockPos = Java.type("net.minecraft.core.BlockPos");
var Blocks = Java.type("net.minecraft.world.level.block.Blocks");
var Items = Java.type("net.minecraft.world.item.Items");
var Class = null;
var IS = Java.type("net.minecraft.world.item.ItemStack");
var MT = Java.type("net.minecraft.world.entity.MobSpawnType");
var AABB = Java.type("net.minecraft.world.phys.AABB");
var ItemEntity = Java.type("net.minecraft.world.entity.item.ItemEntity");
var ApiCompat = Java.type("org.kylepitcock.legacyconnector.v1_20_1.migrate.ApiCompat");
var R5 = Java.type("org.kylepitcock.legacyconnector.v1_20_1.migrate.Round5Compat");
var R4 = Java.type("org.kylepitcock.legacyconnector.v1_20_1.migrate.Round4Compat");
var LE = Java.type("org.kylepitcock.legacyconnector.v1_20_1.migrate.LegacyEnchantment");
function lvl(name) { return server.getLevel(ResourceKey.create(Registries.DIMENSION, RL.parse(name))); }
function spawn(level, id, x, y, z) {
  var t = BI.ENTITY_TYPE.get(RL.parse(id));
  var e = t.create(level);
  e.moveTo(x, y, z, 0, 0);
  level.addFreshEntity(e);
  return e;
}
if (typeof S === "undefined") { var S = {}; }
try {
  var ow = server.overworld();
  if (tick === 3) {
    // 1. changeDimension: a vanilla pig and (when present) a mod mob to the nether
    var nether = lvl("minecraft:the_nether");
    ow.getChunk(0, 0);
    var pig = spawn(ow, "minecraft:pig", 0.5, 100, 0.5);
    S.pig = pig;
    var moved = ApiCompat.changeDimension(pig, nether, null);
    S.pigMoved = moved;
    say("changeDimension pig: returned " + moved + " alive=" + (moved != null && moved.isAlive()) + " dim=" + (moved != null ? moved.level().dimension().location() : "-"));
  }
  if (tick === 5) {
    var m = S.pigMoved;
    say("changeDimension pig after 2 ticks: alive=" + (m != null && m.isAlive()) + " removed=" + (m != null ? m.isRemoved() : "-") + " dim=" + (m != null ? m.level().dimension().location() : "-") + " pos=" + (m != null ? m.position() : "-"));
  }
  if (tick === 6) {
    // 2. getBlockPathTypeStatic
    var pos = new BlockPos(0, 120, 0);
    var mp = new BlockPos.MutableBlockPos(0, 120, 0);
    ow.setBlockAndUpdate(pos.below(), Blocks.LAVA.defaultBlockState());
    ow.setBlockAndUpdate(pos.below(2), Blocks.WATER.defaultBlockState());
    ow.setBlockAndUpdate(pos.below(3), Blocks.STONE.defaultBlockState());
    ow.setBlockAndUpdate(pos.below(4), Blocks.CACTUS.defaultBlockState());
    ow.setBlockAndUpdate(pos.below(5), Blocks.MAGMA_BLOCK.defaultBlockState());
    var out = [];
    for (var d = 0; d <= 5; d++) { var mq = new BlockPos.MutableBlockPos(0.0, 120.0 - d, 0.0); out.push(d + ":" + R5.blockPathTypeStatic(ow, mq)); }
    say("blockPathTypeStatic " + out.join(" "));
    // 5. canApplyAtEnchantingTable
    var ench = ow.registryAccess().registryOrThrow(Registries.ENCHANTMENT);
    var sharp = LE.of(ench.getHolderOrThrow(ResourceKey.create(Registries.ENCHANTMENT, RL.parse("minecraft:sharpness"))));
    var prot = LE.of(ench.getHolderOrThrow(ResourceKey.create(Registries.ENCHANTMENT, RL.parse("minecraft:protection"))));
    say("canApplyAtEnchantingTable sword/sharpness=" + R4.itemCanApplyAtEnchantingTable(Items.DIAMOND_SWORD, new IS(Items.DIAMOND_SWORD), sharp)
      + " stick/sharpness=" + R4.itemCanApplyAtEnchantingTable(Items.STICK, new IS(Items.STICK), sharp)
      + " helmet/protection=" + R4.itemCanApplyAtEnchantingTable(Items.DIAMOND_HELMET, new IS(Items.DIAMOND_HELMET), prot)
      + " sword/protection=" + R4.itemCanApplyAtEnchantingTable(Items.DIAMOND_SWORD, new IS(Items.DIAMOND_SWORD), prot)
      + " stackExt sword/sharpness=" + R4.stackCanApplyAtEnchantingTable(new IS(Items.DIAMOND_SWORD), sharp));
  }
  if (tick === 7) {
    // 3. gateway: a gateway block in the end with an exit portal 100 blocks away
    var end = lvl("minecraft:the_end");
    for (var cx = -1; cx <= 3; cx++) for (var cz = -1; cz <= 1; cz++) { end.setChunkForced(cx, cz, true); end.getChunk(cx, cz); }
    var gp = new BlockPos(0, 80, 0);
    end.setBlockAndUpdate(gp, Blocks.END_GATEWAY.defaultBlockState());
    var be = end.getBlockEntity(gp);
    S.gate = be;
    S.gp = gp;
    be.setExitPosition(new BlockPos(30, 66, 0), true);
    var ent = spawn(end, "minecraft:pig", 0.5, 80, 0.5);
    S.gpig = ent;
    R5.gatewayTeleport(end, gp, end.getBlockState(gp), ent, be);
    say("gateway: handed over, pig at " + ent.position());
  }
  if (tick === 8 || tick === 12 || tick === 40) {
    var p = S.gpig;
    var end2 = lvl("minecraft:the_end");
    var Pig = Java.type("net.minecraft.world.entity.animal.Pig");
    var all = []; var it0 = end2.getAllEntities().iterator();
    while (it0.hasNext()) { var en = it0.next(); if (String(en.getType()).indexOf("pig") >= 0) all.push(en.position() + " alive=" + en.isAlive() + " removal=" + en.getRemovalReason()); }
    var all2 = []; var itn = lvl("minecraft:the_nether").getAllEntities().iterator();
    while (itn.hasNext()) { var en2 = itn.next(); if (String(en2.getType()).indexOf("pig") >= 0) all2.push(en2.position()); }
    say("pigs in end: " + all.join(" | ") + "; in nether: " + all2.join(" | ") + "; original removal=" + p.getRemovalReason() + " lastpos=" + p.position());
    var near0 = end2.getEntitiesOfClass(Pig.class, new AABB(-5, 0, -5, 5, 250, 5)).size();
    var near100 = end2.getEntitiesOfClass(Pig.class, new AABB(5, -64, -40, 60, 250, 40));
    say("gateway tick " + tick + ": original alive=" + p.isAlive() + " pigs at gateway=" + near0 + " pigs at exit=" + near100.size() + (near100.size() > 0 ? " alive=" + near100.get(0).isAlive() + " pos=" + near100.get(0).position() : ""));
  }
  if (tick === 9) {
    // 4. loot override of a mod mob
    var id = (typeof MOB === "undefined") ? "alexsmobs:cockroach" : MOB;
    if (BI.ENTITY_TYPE.containsKey(RL.parse(id))) {
      var mob = spawn(ow, id, 10.5, 101, 10.5);
      S.mob = mob;
      ow.setBlockAndUpdate(new BlockPos(10, 100, 10), Blocks.STONE.defaultBlockState());
      say("mob " + id + " lootTable=" + mob.getLootTable().location() + " default=" + mob.getType().getDefaultLootTable().location());
      mob.kill();
    } else say("mob type " + id + " absent");
  }
  if (tick === 12) {
    if (S.mob) {
      var items = ow.getEntitiesOfClass(ItemEntity.class, new AABB(5, 95, 5, 16, 110, 16));
      var names = [];
      for (var i = 0; i < items.size(); i++) names.push(String(items.get(i).getItem()));
      say("mob drops after kill: " + items.size() + " " + names.join(","));
    }
  }
} catch (e) {
  var t = (e.getStackTrace ? e : (e.asHostException ? e.asHostException() : null));
  say("error at tick " + tick + ": " + e);
  if (t != null) { var st = t.getStackTrace(); for (var i = 0; i < Math.min(8, st.length); i++) say("   at " + st[i]); }
}
