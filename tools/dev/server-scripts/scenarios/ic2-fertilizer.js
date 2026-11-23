// IC2 Classic (1.7.10): right-click IC2 fertilizer (and vanilla bone meal) on a crop stick with the real block use path (#324).
var Y0 = 150, X0 = 2600, Z0 = 2600;
var GM = Java.type("net.minecraft.world.level.GameType");
function itemNamed(frag) {
  var r = [];
  L.R.ITEM.keySet().forEach(function (k) { var s = "" + k; if (k.getNamespace() == "ic2" && s.indexOf(frag) >= 0) r.push(s); });
  return r;
}
function use(st, x, z) {
  var p = L.fp();
  p.setGameMode(GM.SURVIVAL);
  p.teleportTo(L.lvl(), x - 1.5, Y0, z + 0.5, -90, 30);
  p.setItemInHand(L.Hand.MAIN_HAND, st);
  var bp = new L.BP(x, Y0, z);
  var hit = new L.BHR(new L.Vec3(x + 0.5, Y0 + 0.5, z + 0.5), L.Dir.UP, bp, false);
  var bs = L.lvl().getBlockState(bp);
  var r = bs.useItemOn(st, L.lvl(), p, L.Hand.MAIN_HAND, hit);
  var r2 = null;
  if ("" + r == "PASS_TO_DEFAULT_BLOCK_INTERACTION") r2 = bs.useWithoutItem(L.lvl(), p, hit);
  return r + "/" + r2;
}
if (tick == 0) {
  L.platform(X0 - 4, Y0, Z0 - 4, 12, 12, 6);
  L.say("crop: " + itemNamed("crop").join(" "));
  L.say("fert: " + itemNamed("fert").join(" "));
}
if (tick == 20) {
  var cs = itemNamed("crop")[0];
  var it = L.R.ITEM.get(L.RL.parse(cs)), p = L.fp();
  p.setGameMode(GM.CREATIVE);
  var st = new L.IS(it, 1);
  p.teleportTo(L.lvl(), X0 - 1.5, Y0, Z0 + 0.5, -90, 30);
  p.setItemInHand(L.Hand.MAIN_HAND, st);
  var r = st.getItem().useOn(new L.UOC(L.lvl(), p, L.Hand.MAIN_HAND, st, new L.BHR(new L.Vec3(X0 + 0.5, Y0, Z0 + 0.5), L.Dir.UP, new L.BP(X0, Y0 - 1, Z0), false)));
  L.say("crop place " + r + " " + L.blk(X0, Y0, Z0));
  G.t = L.te(X0, Y0, Z0);
  L.say("tile " + (G.t ? G.t.getClass().getName() : null));
  if (G.t) L.say("before " + L.dump(G.t).substring(0, 400));
}
if (tick == 30 && G.t) {
  var f = itemNamed("fertilizer")[0];
  var st = new L.IS(L.R.ITEM.get(L.RL.parse(f)), 4);
  L.say("fert use -> " + use(st, X0, Z0) + " stack now " + st.getCount());
  L.say("nutrientStorage " + L.fs(G.t, "nutrientStorage"));
  var bm = new L.IS(L.R.ITEM.get(L.RL.parse("minecraft:bone_meal")), 4);
  L.say("bonemeal use -> " + use(bm, X0, Z0) + " stack now " + bm.getCount());
  L.say("nutrientStorage " + L.fs(G.t, "nutrientStorage"));
}
L.run(tick);
