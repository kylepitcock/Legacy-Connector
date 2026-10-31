var Y0 = 150, X0 = 2100, Z0 = 2100;
var MB = Java.type("net.minecraft.world.level.block.Blocks");
function setv(x, y, z, b) { L.lvl().setBlock(new L.BP(x, y, z), b.defaultBlockState(), 3); }
if (tick == 0) {
  L.platform(X0 - 6, Y0, Z0, 30, 40, 8);
  G.b = L.rig({name: "BORER", X: X0, Z: Z0 + 30, Y: Y0, fuel: [["rc ethanol", 20000]], done: function (c) {
    var m = c.mach; if (!m) return;
    var cs = m.cutShape;
    // a single-block shaft: the centre of the 7x5 shape
    for (var i = 0; i < 7; i++) for (var j = 0; j < 5; j++) cs[i][j] = false;
    cs[3][2] = true;
    G.wall = [];
    for (var k = 1; k <= 10; k++) { setv(c.mx + c.dx * k, Y0, c.mz + c.dz * k, k % 3 == 0 ? MB.IRON_ORE : MB.STONE); G.wall.push([c.mx + c.dx * k, c.mz + c.dz * k]); }
    // chests on both sides of the borer
    var sx = c.dz != 0 ? 1 : 0, sz = c.dx != 0 ? 1 : 0;
    setv(c.mx + sx, Y0, c.mz + sz, MB.CHEST); setv(c.mx - sx, Y0, c.mz - sz, MB.CHEST);
    G.chests = [[c.mx + sx, c.mz + sz], [c.mx - sx, c.mz - sz]];
    L.say("borer ready: shape set, wall of 10 (iron ore every 3rd), chests beside; borer dir=" + c.dx + "," + c.dz);
  }});
}
if (tick % 100 == 0 && tick >= 100 && G.b && G.b.mach) {
  var c = G.b, m = L.te(c.mx, Y0, c.mz);
  var left = 0, ores = 0; G.wall.forEach(function (w) { var b = L.blk(w[0], Y0, w[1]); if (b.indexOf("stone") >= 0 || b.indexOf("ore") >= 0) left++; });
  L.say("t" + tick + " borer " + L.snap(m) + " nodig=" + L.fs(m, "nodig") + " jammed=" + L.fs(m, "jammed") + " step=" + L.fs(m, "step") + " wall blocks left=" + left + "/10");
  // items: chests + item entities near the wall
  var lv = L.lvl(), tot = 0;
  G.chests.forEach(function (ch) { var be = lv.getBlockEntity(new L.BP(ch[0], Y0, ch[1])); if (be) for (var i = 0; i < be.getContainerSize(); i++) tot += be.getItem(i).getCount(); });
  var AABB = Java.type("net.minecraft.world.phys.AABB");
  var ents = lv.getEntities(null, new AABB(c.mx - 14, Y0 - 3, c.mz - 14, c.mx + 14, Y0 + 3, c.mz + 14));
  var drops = 0; Java.from(ents.toArray()).forEach(function (e) { if (("" + e.getClass().getSimpleName()).indexOf("ItemEntity") >= 0) drops += e.getItem().getCount(); });
  L.say("   items in chests=" + tot + " dropped item entities=" + drops);
}
L.run(tick);
