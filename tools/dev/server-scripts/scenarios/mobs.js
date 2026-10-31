var Y0 = 150, X0 = 2600, Z0 = 2600;
var EntityType = Java.type("net.minecraft.world.entity.EntityType");
var MB = Java.type("net.minecraft.world.level.block.Blocks");
var AABB = Java.type("net.minecraft.world.phys.AABB");
function setv(x, y, z, b) { L.lvl().setBlock(new L.BP(x, y, z), b.defaultBlockState(), 3); }
function spawn(type, x, y, z) { var e = type.create(L.lvl()); e.moveTo(x, y, z, 0, 0); L.lvl().addFreshEntity(e); return e; }
if (tick == 0) {
  L.platform(X0 - 8, Y0, Z0, 30, 40, 10);
  G.h = L.rig({name: "MOBHARVESTER", X: X0, Z: Z0 + 14, Y: Y0, fuel: [["rc ethanol", 20000]], anyYaw: true, done: function (c) {
    var m = c.mach; if (!m) { L.say("no harvester"); return; }
    setv(c.mx + 1, Y0, c.mz, MB.CHEST);
    L.say("harvester ready: " + L.dump(m).substring(0, 600));
  }});
}
if (tick == 50) {
  var c = G.h; G.mobs = [];
  for (var i = 0; i < 4; i++) { G.mobs.push(spawn(EntityType.COW, c.mx + 2.5 + i, Y0, c.mz + 2.5)); }
  for (var i = 0; i < 2; i++) { G.mobs.push(spawn(EntityType.PIG, c.mx - 2.5 - i, Y0, c.mz - 2.5)); }
  G.mobs.push(spawn(EntityType.ZOMBIE, c.mx + 3.5, Y0, c.mz - 3.5));
  L.say("spawned " + G.mobs.length + " mobs near harvester");
}
if (tick % 100 == 0 && tick >= 100 && G.h && G.h.mach) {
  var alive = 0; G.mobs.forEach(function (m) { if (m.isAlive()) alive++; });
  var c = G.h, lv = L.lvl(), chest = lv.getBlockEntity(new L.BP(c.mx + 1, Y0, c.mz)), items = 0;
  if (chest) for (var i = 0; i < chest.getContainerSize(); i++) items += chest.getItem(i).getCount();
  var ents = lv.getEntities(null, new AABB(c.mx - 12, Y0 - 2, c.mz - 12, c.mx + 12, Y0 + 4, c.mz + 12)), drops = 0, xp = 0;
  Java.from(ents.toArray()).forEach(function (e) { var n = "" + e.getClass().getSimpleName(); if (n.indexOf("ItemEntity") >= 0) drops += e.getItem().getCount(); if (n.indexOf("ExperienceOrb") >= 0) xp++; });
  L.say("t" + tick + " harvester " + L.snap(L.te(c.mx, Y0, c.mz)) + " mobs alive=" + alive + "/" + G.mobs.length + " chest items=" + items + " loose drops=" + drops + " xp orbs=" + xp);
}
L.run(tick);
