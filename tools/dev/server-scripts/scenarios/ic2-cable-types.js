// Places every IC2 Classic cable item and prints its insulation absorption / breakdown (bare cables must have 0 absorption: they shock; #324).
var Y0 = 150, X0 = 2700, Z0 = 2700;
var GM = Java.type("net.minecraft.world.level.GameType");
if (tick == 0) {
  L.platform(X0 - 2, Y0, Z0 - 2, 50, 6, 6);
  var names = [];
  L.R.ITEM.keySet().forEach(function (k) { var s = "" + k; if (k.getNamespace() == "ic2" && s.indexOf("ic2cable") >= 0) names.push(s); });
  names.sort();
  G.names = names;
}
if (tick == 20) {
  G.names.forEach(function (n, i) {
    var it = L.R.ITEM.get(L.RL.parse(n)), p = L.fp(), st = new L.IS(it, 1), x = X0 + i * 3;
    p.setGameMode(GM.CREATIVE);
    p.teleportTo(L.lvl(), x - 1.5, Y0, Z0 + 0.5, -90, 30);
    p.setItemInHand(L.Hand.MAIN_HAND, st);
    var r = st.getItem().useOn(new L.UOC(L.lvl(), p, L.Hand.MAIN_HAND, st, new L.BHR(new L.Vec3(x + 0.5, Y0, Z0 + 0.5), L.Dir.UP, new L.BP(x, Y0 - 1, Z0), false)));
    var ct = L.te(x, Y0, Z0);
    if (!ct) { L.say(n + " place " + r + " no tile"); return; }
    L.say(n + " type=" + L.fs(ct, "cableType") + " insAbs=" + L.call(ct, "getInsulationEnergyAbsorption") + " insBreak=" + L.call(ct, "getInsulationBreakdownEnergy") + " condBreak=" + L.call(ct, "getConductorBreakdownEnergy") + " color=" + L.fs(ct, "color"));
  });
}
L.run(tick);
