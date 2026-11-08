// Showcase of one mod on the flat world, run on the server thread. Placeholders: __NS__, __PHASE__ (blocks|entities|clear), __NAMES__ (comma list of paths).
// Answers JSON {ok, fail, errors:[..]}.
var NS = "__NS__", PHASE = "__PHASE__", NAMES = "__NAMES__".length ? "__NAMES__".split(",") : [];
var res = {ok: 0, fail: 0, errors: []};
function err(what, e) { res.fail++; if (res.errors.length < 6) res.errors.push(what + ": " + String(e).substring(0, 160)); }
var task = function () {
  var BR = Java.type("net.minecraft.core.registries.BuiltInRegistries");
  var RL = Java.type("net.minecraft.resources.ResourceLocation");
  var BP = Java.type("net.minecraft.core.BlockPos");
  var Blocks = Java.type("net.minecraft.world.level.block.Blocks");
  var Player = Java.type("net.minecraft.world.entity.player.Player");
  var level = server.overworld();
  var Y = -60;
  // clear the stage: blocks and every non-player entity
  var air = Blocks.AIR.defaultBlockState();
  for (var x = -12; x <= 12; x++) for (var z = -2; z <= 18; z++) for (var y = Y; y <= Y + 4; y++) {
    var p = new BP(x, y, z);
    if (!level.getBlockState(p).isAir()) level.setBlock(p, air, 18);
  }
  var ents = level.getAllEntities().iterator(), dead = [];
  while (ents.hasNext()) { var e = ents.next(); if (!(e instanceof Player)) dead.push(e); }
  for (var i = 0; i < dead.length; i++) dead[i].discard();
  if (PHASE === "blocks") {
    for (var n = 0; n < NAMES.length; n++) {
      try {
        var b = BR.BLOCK.get(RL.fromNamespaceAndPath(NS, NAMES[n]));
        var st = b.defaultBlockState();
        level.setBlock(new BP(-8 + (n % 9) * 2, Y, 1 + Math.floor(n / 9) * 2), st, 3);
        res.ok++;
      } catch (ex) { err(NAMES[n], ex); }
    }
  } else if (PHASE === "entities") {
    for (var m = 0; m < NAMES.length; m++) {
      try {
        var t = BR.ENTITY_TYPE.get(RL.fromNamespaceAndPath(NS, NAMES[m]));
        var ent = t.create(level);
        if (ent === null) { err(NAMES[m], "create() returned null"); continue; }
        var a = m * 6.2832 / Math.max(NAMES.length, 1);
        ent.setPos(Math.cos(a) * 5, Y, 8 + Math.sin(a) * 4);
        level.addFreshEntity(ent);
        res.ok++;
      } catch (ex) { err(NAMES[m], ex); }
    }
  }
};
try { server.executeBlocking(new java.lang.Runnable({run: task})); } catch (ex) { err("stage", ex); }
JSON.stringify(res);
