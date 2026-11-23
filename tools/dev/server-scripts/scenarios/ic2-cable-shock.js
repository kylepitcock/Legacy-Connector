// IC2 Classic (1.7.10): a burning generator next to a bare copper cable; a pig stands on the cable. The net shocks entities near cables
// carrying more energy than their insulation absorbs (EnergyNetLocal: getEntitiesWithinAABB around the path, then attackEntityFrom(electricity)).
var Y0 = 150, X0 = 2600, Z0 = 2600;
var GM = Java.type("net.minecraft.world.level.GameType");
function itemNamed(frag, not) {
  var r = [];
  L.R.ITEM.keySet().forEach(function (k) { var s = "" + k; if (k.getNamespace() == "ic2" && s.indexOf(frag) >= 0 && (!not || s.indexOf(not) < 0)) r.push(s); });
  return r;
}
function put(name, x, z) {
  var it = L.R.ITEM.get(L.RL.parse(name)), p = L.fp(), st = new L.IS(it, 1);
  p.setGameMode(GM.CREATIVE);
  p.teleportTo(L.lvl(), x - 1.5, Y0, z + 0.5, -90, 30);
  p.setItemInHand(L.Hand.MAIN_HAND, st);
  var r = st.getItem().useOn(new L.UOC(L.lvl(), p, L.Hand.MAIN_HAND, st, new L.BHR(new L.Vec3(x + 0.5, Y0, z + 0.5), L.Dir.UP, new L.BP(x, Y0 - 1, z), false)));
  L.say(name + " at " + x + "," + z + " -> " + r + " " + L.blk(x, Y0, z));
}
if (tick == 0) {
  L.platform(X0 - 4, Y0, Z0 - 4, 12, 12, 6);
  L.say("gener: " + itemNamed("gener").join(" "));
  L.say("cable: " + itemNamed("cable").join(" "));
}
if (tick == 20) {
  var gen = itemNamed("generator", "geo").filter(function (n) { return n.indexOf("solar") < 0 && n.indexOf("wind") < 0 && n.indexOf("water") < 0 && n.indexOf("thermal") < 0 && n.indexOf("semi") < 0; })[0] || "ic2:generator";
  var cab = itemNamed("cable").filter(function (n) { return n.indexOf("copper") >= 0 && n.indexOf("insul") < 0; })[0];
  L.say("using " + gen + " / " + cab);
  put(gen, X0, Z0);
  put(cab, X0 + 1, Z0);
  put(cab, X0 + 2, Z0);
  var t = L.te(X0, Y0, Z0);
  G.gen = t;
  L.say("gen tile " + (t ? t.getClass().getName() : null));
  G.pig = Java.type("net.minecraft.world.entity.EntityType").PIG.create(L.lvl());
  G.pig.moveTo(X0 + 2.5, Y0, Z0 + 0.5, 0, 0);
  L.lvl().addFreshEntity(G.pig);
  G.hp0 = G.pig.getHealth();
  // fuel
  try {
    var coal = new L.LIS(Java.type("legacyconnector.v1_7_10.net.minecraft.init.Items").field_151044_h, 16, 0);
    t.setInventorySlotContents(0, coal);
    L.say("fuel slot: " + t.getStackInSlot(0));
  } catch (e) { L.say("fuel ERR " + e); }
}
if (tick > 20 && tick % 20 == 0 && G.gen) {
  try {
    L.say("t" + tick + " gen " + L.dump(G.gen).substring(0, 300) + " | pig hp " + G.pig.getHealth() + "/" + G.hp0);
    var cabT = L.te(X0 + 1, Y0, Z0);
    if (cabT) L.say("cable tile " + cabT.getClass().getName() + " " + L.dump(cabT).substring(0, 200));
  } catch (e) { L.say("ERR " + e); }
}
L.run(tick);
