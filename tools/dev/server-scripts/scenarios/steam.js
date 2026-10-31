var Y0 = 150, X0 = 1900, Z0 = 1900;
var MB = Java.type("net.minecraft.world.level.block.Blocks");
function setv(x, y, z, b) { L.lvl().setBlock(new L.BP(x, y, z), b.defaultBlockState(), 3); }
// place an item at (x,Y,z) by clicking the WEST face of the stone block at (x+1,Y,z)
function placeSide(stack, x, Y, z) {
  var lv = L.lvl(), p = L.fp(), st = stack.mcc$modern();
  p.teleportTo(lv, x - 1.5, Y, z + 0.5, -90, 30);
  p.setItemInHand(L.Hand.MAIN_HAND, st);
  var r = st.getItem().useOn(new L.UOC(lv, p, L.Hand.MAIN_HAND, st, new L.BHR(new L.Vec3(x + 1, Y + 0.5, z + 0.5), L.Dir.WEST, new L.BP(x + 1, Y, z), false)));
  return {r: "" + r, te: L.te(x, Y, z)};
}
if (tick == 0) {
  L.platform(X0, Y0, Z0, 40, 12, 8);
  G.eng = [];
  var cfgs = ["lava below", "fire below", "control", "magma below"];
  cfgs.forEach(function (c, i) {
    var x = X0 + 3 + i * 6, z = Z0 + 4;
    setv(x + 1, Y0, z, MB.STONE);
    if (c == "lava below") setv(x, Y0 - 1, z, MB.LAVA);
    if (c == "fire below") { setv(x, Y0 - 1, z, MB.AIR); setv(x, Y0 - 1, z, MB.FIRE); }
    if (c == "magma below") setv(x, Y0 - 1, z, MB.MAGMA_BLOCK);
    var r = placeSide(L.engStack(2), x, Y0, z);
    L.say("steam engine [" + c + "] place=" + r.r + " te=" + (r.te ? "ok" : "none") + " below=" + L.blk(x, Y0 - 1, z));
    G.eng.push({c: c, x: x, z: z, t: r.te});
  });
}
if (tick == 20) {
  G.eng.forEach(function (e) {
    if (!e.t) return;
    var t = L.te(e.x, Y0, e.z);
    L.say("feed [" + e.c + "] water " + L.feed(t, "water", 4000));
    L.rs(e.x, Y0 + 1, e.z, true);
  });
}
if (tick == 60 || tick == 200 || tick == 400) {
  G.eng.forEach(function (e) {
    var t = L.te(e.x, Y0, e.z); if (!t) return;
    L.say("t" + tick + " steam [" + e.c + "] temp=" + L.fs(t, "temperature") + " " + L.snap(t) + " on=" + t.isOn() + " water=" + L.fs(t, "water") + " below=" + L.blk(e.x, Y0 - 1, e.z));
  });
}
L.run(tick);
