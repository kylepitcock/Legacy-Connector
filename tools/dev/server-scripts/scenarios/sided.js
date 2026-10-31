var Y0 = 150, X0 = 1700, Z0 = 1700;
var Caps = Java.type("net.neoforged.neoforge.capabilities.Capabilities");
var Dirs = Java.from(Java.type("net.minecraft.core.Direction").values());
var LB = Java.type("legacyconnector.v1_7_10.net.minecraft.init.Blocks");
var LItems = Java.type("legacyconnector.v1_7_10.net.minecraft.init.Items");
var NAMES = ["GRINDER", "BIGFURNACE", "COMPACTOR", "BLASTFURNACE", "EXTRACTOR", "FERMENTER", "CRAFTER", "SCALECHEST", "FILLINGSTATION", "BUCKETFILLER"];
var TESTS = function () { return [new L.LIS(LB.iron_ore, 4), new L.LIS(LItems.coal, 4), new L.LIS(LB.dirt, 4), new L.LIS(LItems.iron_ingot, 4), new L.LIS(LItems.wheat, 4), new L.LIS(LItems.bucket, 1)]; };
if (tick == 0) {
  L.platform(X0, Y0, Z0, 50, 12, 8);
  NAMES.forEach(function (n, i) { var r = L.place(L.stackOf(n), X0 + 2 + i * 4, Y0, Z0 + 2, null, 0); if (r.r != "CONSUME") L.say("place " + n + " " + r.r); });
}
if (tick == 30) {
  var mism = 0, total = 0, lines = [];
  NAMES.forEach(function (n, i) {
    var x = X0 + 2 + i * 4, z = Z0 + 2, t = L.te(x, Y0, z);
    var sided = !!t.getAccessibleSlotsFromSide;
    var accepted = [], rejected = [];
    Dirs.forEach(function (d) {
      var ih = L.lvl().getCapability(Caps.ItemHandler.BLOCK, new L.BP(x, Y0, z), d);
      if (!ih) { lines.push(n + " " + d + ": NO item capability"); return; }
      var side = d.get3DDataValue();
      var accSlots = sided ? Java.from(t.getAccessibleSlotsFromSide(side)) : null;
      TESTS().forEach(function (st) {
        for (var slot = 0; slot < t.getSizeInventory(); slot++) {
          var legacyOk;
          try {
            legacyOk = t.isItemValidForSlot(slot, st) && (!sided || (accSlots.indexOf(slot) >= 0 && t.canInsertItem(slot, st, side)));
          } catch (e) { legacyOk = "EXC"; }
          // the capability's slot numbering for a sided handler is the accessible-slot list
          var idx = sided ? accSlots.indexOf(slot) : slot;
          var capOk = false;
          if (idx >= 0 && idx < ih.getSlots()) {
            var rem = ih.insertItem(idx, st.mcc$modern().copy(), true);
            capOk = rem.getCount() < st.mcc$modern().getCount();
          }
          total++;
          if (legacyOk !== capOk && legacyOk !== "EXC") { mism++; if (lines.length < 40) lines.push("MISMATCH " + n + " " + d + " slot " + slot + " " + st.getItem().getClass().getSimpleName() + ": legacy=" + legacyOk + " cap=" + capOk); }
        }
      });
    });
  });
  L.say("sided inventory check: " + total + " comparisons, " + mism + " mismatches");
  lines.forEach(function (l) { L.say(l); });
}
L.run(tick);
