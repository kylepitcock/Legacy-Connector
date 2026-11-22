// Breaks IC2 Classic cables in survival and reads what drops (1.7.10; jar in run-subtest/mods/1.7.10).
var Y0 = 150, X0 = 2600, Z0 = 2600;
var ItemEntity = Java.type("net.minecraft.world.entity.item.ItemEntity");
var GM = Java.type("net.minecraft.world.level.GameType");
var AABB = Java.type("net.minecraft.world.phys.AABB");
function items(x, y, z) {
  var l = L.lvl().getEntitiesOfClass(ItemEntity.class, new AABB(x - 2, y - 2, z - 2, x + 3, y + 3, z + 3));
  var o = []; for (var i = 0; i < l.size(); i++) o.push("" + l.get(i).getItem()); return o.join(",");
}
if (tick == 0) {
  L.platform(X0 - 2, Y0, Z0, 30, 6, 6);
  var names = [];
  L.R.ITEM.keySet().forEach(function (k) { if (k.getNamespace() == "ic2" && ("" + k).indexOf("able") >= 0 && ("" + k).indexOf("cable_") >= 0) names.push("" + k); });
  L.say("cable-ish items: " + names.join(" "));
  G.names = names;
}
if (tick == 20) {
  G.names.forEach(function (n, i) {
    var it = L.R.ITEM.get(L.RL.parse(n));
    var x = X0 + i * 2, p = L.fp();
    var st = new L.IS(it, 1);
    p.setGameMode(GM.SURVIVAL);
    p.teleportTo(L.lvl(), x - 1.5, Y0, Z0 + 0.5, -90, 30);
    p.setItemInHand(L.Hand.MAIN_HAND, st);
    var r = st.getItem().useOn(new L.UOC(L.lvl(), p, L.Hand.MAIN_HAND, st, new L.BHR(new L.Vec3(x + 0.5, Y0, Z0 + 0.5), L.Dir.UP, new L.BP(x, Y0 - 1, Z0), false)));
    L.say(n + " place " + r + " -> " + L.blk(x, Y0, Z0));
  });
}
if (tick == 30) {
  G.names.forEach(function (n, i) {
    var x = X0 + i * 2, p = L.fp();
    var ok = p.gameMode.destroyBlock(new L.BP(x, Y0, Z0));
    L.say(n + " destroy " + ok + " now " + L.blk(x, Y0, Z0) + " items near: " + items(x, Y0, Z0));
  });
}
if (tick == 25) {
  try {
    var x = X0, lv = L.lvl();
    var st = lv.getBlockState(new L.BP(x, Y0, Z0)), b = st.getBlock(), lg = L.f(b, "legacy");
    L.say("block " + b.getClass().getName() + " legacy " + lg + " te=" + L.te(x, Y0, Z0));
    var w = Java.type("legacyconnector.v1_7_10.net.minecraft.world.World").mcc$wrap(lv);
    L.say("legacy getDrops -> " + lg.getDrops(w, x, Y0, Z0, 9, 0));
    var cls = Java.type("ic2.core.Ic2Items").class;
    var fld = cls.getDeclaredField("insulatedCopperCableItem"); fld.setAccessible(true);
    L.say("insulatedCopperCableItem " + fld.get(null));
  } catch (e) { L.say("DBG ERR " + e); }
}
L.run(tick);
