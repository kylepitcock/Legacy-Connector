var out = "";
if (tick == 0) {
  sp = realPlayer("CCPlayer");
  p = legacy(sp);
  T("Reika.ChromatiCraft.Magic.Progression.ProgressionManager").instance.maxPlayerProgression(p, false);
  T("Reika.ChromatiCraft.Magic.Progression.ChromaResearchManager").instance.maxPlayerResearch(p, false);
  var l60 = null;
  server.getAllLevels().forEach(function (l) { if (String(l.dimension().location()).indexOf("dim_60") >= 0) l60 = l; });
  dim = l60;
  out += P("dim60 level", function () { return "" + dim.dimension().location() + " minY " + dim.getMinBuildHeight() + " maxY " + dim.getMaxBuildHeight() + " spawn " + dim.getSharedSpawnPos() + " provider " + lworld(dim).provider; });
  t0 = server.getAverageTickTimeNanos();
}
if (tick == 5) {
  out += P("teleport to dim60", function () {
    var sp0 = dim.getSharedSpawnPos();
    var r = sp.teleportTo(dim, sp0.getX() + 0.5, sp0.getY() + 1, sp0.getZ() + 0.5, 0, 0);
    return "result " + r + " now in " + sp.level().dimension().location() + " at " + sp.blockPosition();
  });
}
if (tick > 5 && tick % 100 == 0) {
  out += P("stay@" + tick, function () {
    var n = 0, cnt = 0;
    dim.getAllEntities().forEach(function (e) { n++; });
    var ex = Date.now();
    return "dim " + sp.level().dimension().location() + " pos " + sp.blockPosition() + " hp " + sp.getHealth() + " alive " + sp.isAlive() + " entities(dim60) " + n + " avgTickMs " + (server.getAverageTickTimeNanos() / 1e6).toFixed(1) + " chunks " + dim.getChunkSource().getLoadedChunksCount();
  });
}
if (tick == 120) {
  out += P("spawn mobs in dim60", function () {
    var BI = T("net.minecraft.core.registries.BuiltInRegistries");
    var MST = T("net.minecraft.world.entity.MobSpawnType");
    var names = []; var spawned = [];
    BI.ENTITY_TYPE.keySet().forEach(function (k) { if (k.getNamespace() == "chromaticraft") names.push(k); });
    var base = sp.blockPosition();
    mobs = [];
    names.forEach(function (k, i) {
      try {
        var et = BI.ENTITY_TYPE.get(k);
        var e = et.spawn(dim, bp(base.getX() + 3 + (i % 6) * 2, base.getY() + 1, base.getZ() + 3 + Math.floor(i / 6) * 2), MST.COMMAND);
        if (e) { mobs.push([String(k), e]); spawned.push(String(k)); }
      } catch (x) { spawned.push(String(k) + "!!" + x); }
    });
    return "types " + names.length + " spawned " + mobs.length + ": " + spawned.join(",");
  });
}
if (tick == 340) {
  out += P("mobs after 220 ticks", function () {
    var s = [];
    mobs.forEach(function (m) { s.push(m[0].replace("chromaticraft:", "") + "=" + (m[1].isAlive() ? "alive" : "gone")); });
    return s.join(" ");
  });
}
if (tick == 360) {
  out += P("return to overworld", function () {
    var o = server.overworld();
    sp.teleportTo(o, 8.5, 120, 8.5, 0, 0);
    return "now in " + sp.level().dimension().location() + " at " + sp.blockPosition();
  });
}
out;
