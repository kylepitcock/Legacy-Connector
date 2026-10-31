var Y0 = 150, X0 = 2000, Z0 = 2000;
var LB = Java.type("legacyconnector.v1_7_10.net.minecraft.init.Blocks");
var LItems = Java.type("legacyconnector.v1_7_10.net.minecraft.init.Items");
var GB = Java.type("org.kylepitcock.legacyconnector.v1_7_10.bridge.GuiBridge");
var LM = Java.type("org.kylepitcock.legacyconnector.v1_7_10.gui.LegacyMenu");
var EB = Java.type("org.kylepitcock.legacyconnector.v1_7_10.bridge.EntityBridge");
var LW = Java.type("legacyconnector.v1_7_10.net.minecraft.world.World");
var M = Java.type("Reika.RotaryCraft.Registry.MachineRegistry");
var NAMES = Java.from(M.values()).map(function (m) { return m.name(); });
if (tick == 0) {
  L.platform(X0, Y0, Z0, 12 * 4 + 4, 12 * 5, 8);
  NAMES.forEach(function (n, i) {
    var x = X0 + 2 + (i % 12) * 4, z = Z0 + 2 + Math.floor(i / 12) * 4;
    var r = L.place(L.stackOf(n), x, Y0, z, null, 0);
    if (r.r != "CONSUME") G["np_" + n] = r.r;
  });
  // the mod's gui handler
  var f = GB.class.getDeclaredField("HANDLERS"); f.setAccessible(true);
  var hs = f.get(null); G.handler = null;
  Java.from(hs.entrySet().toArray()).forEach(function (e) { if (("" + e.getKey().getClass().getName()).indexOf("Reika.RotaryCraft") == 0) G.handler = e.getValue(); });
  L.say("RC gui handler: " + (G.handler ? G.handler.getClass().getName() : "NONE") + " menuType=" + GB.menuType());
}
if (tick == 25) {
  var p = L.fp(), lv = L.lvl();
  var lp = EB.wrap(p), lw = LW.mcc$wrap(lv);
  var rows = [], noGui = [], fails = [], notPlaced = Object.keys(G).filter(function (k) { return k.indexOf("np_") == 0; }).map(function (k) { return k.substring(3) + "=" + G[k]; });
  var win = 1;
  NAMES.forEach(function (n, i) {
    var x = X0 + 2 + (i % 12) * 4, z = Z0 + 2 + Math.floor(i / 12) * 4;
    if (G["np_" + n]) return;
    try {
      var el = G.handler.getServerGuiElement(0, lp, lw, x, Y0, z);
      if (el == null) { noGui.push(n); return; }
      var menu = new LM(GB.menuType(), win++, p.getInventory(), el, null);
      var slots = menu.slots.size(), machineSlots = Math.max(0, slots - 36);
      var tests = {GRINDER: new L.LIS(LB.iron_ore, 8), EXTRACTOR: new L.LIS(LB.iron_ore, 8), BIGFURNACE: new L.LIS(LB.iron_ore, 8), BLASTFURNACE: new L.LIS(LB.iron_ore, 8), COMPACTOR: new L.LIS(LItems.iron_ingot, 8), FERMENTER: new L.LIS(LItems.wheat, 8), CRAFTER: new L.LIS(LItems.stick, 8), SCALECHEST: new L.LIS(LB.dirt, 8), HEATER: new L.LIS(LItems.coal, 8), FILLINGSTATION: new L.LIS(LItems.bucket, 1)};
      var moved = "-";
      if (tests[n] && machineSlots > 0) {
        p.getInventory().setItem(0, tests[n].mcc$modern());
        var want = tests[n].stackSize;
        var hs = menu.getSlot(slots - 9);
        var dbg = "slotItem-before=" + hs.getItem() + " invItem-before=" + p.getInventory().getItem(0) + " sameContainer=" + (hs.container === p.getInventory()) + " hsContainerIdx=" + hs.getContainerSlot() + " cls=" + hs.container.getClass().getSimpleName();
        var qm = menu.quickMoveStack(p, slots - 9);
        dbg += " | quickMove returned " + qm + " slotItem-after=" + hs.getItem() + " invItem-after=" + p.getInventory().getItem(0);
        L.say("QM " + n + ": " + dbg);
        var left = p.getInventory().getItem(0).getCount(), inM = 0;
        for (var s3 = 0; s3 < machineSlots; s3++) inM += menu.getSlot(s3).getItem().getCount();
        moved = (want - left) + " moved, " + inM + " in machine slots";
      }
      p.getInventory().setItem(0, L.IS.EMPTY);
      rows.push(n + ":" + el.getClass().getSimpleName() + " slots=" + slots + " machine=" + machineSlots + " ore-shift-click: " + moved);
    } catch (e) { fails.push(n + " EXC " + e + (e.getStackTrace ? " @ " + e.getStackTrace()[0] + " / " + e.getStackTrace()[1] : "")); }
  });
  L.say("GUI results: " + rows.length + " containers, " + noGui.length + " without container, " + fails.length + " exceptions, " + notPlaced.length + " not placed");
  L.say("NOGUI " + noGui.join(" "));
  L.say("NOTPLACED " + notPlaced.join(" "));
  L.say("FAILS " + fails.join(" | "));
  rows.forEach(function (r) { L.say("ROW " + r); });
}
L.run(tick);
