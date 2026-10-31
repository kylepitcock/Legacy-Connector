var Y0 = 150, X0 = 1600, Z0 = 1600;
var MARK = "run-subtest/persist_state.txt";
var Files = Java.type("java.nio.file.Files"), Paths = Java.type("java.nio.file.Paths");
var LB = Java.type("legacyconnector.v1_7_10.net.minecraft.init.Blocks");
var LItems = Java.type("legacyconnector.v1_7_10.net.minecraft.init.Items");
var IR = Java.type("Reika.RotaryCraft.Registry.ItemRegistry");
var PHASE_B = Files.exists(Paths.get(MARK));
var SPOTS = ["GAS", "SPORT", "GEARBOX", "GRINDER", "SCALECHEST", "RESERVOIR", "BIGFURNACE", "FERMENTER", "FLYWHEEL", "COMPACTOR", "FILLINGSTATION", "CRAFTER", "EXTRACTOR", "BLASTFURNACE", "SHAFT"];
function pos(i) { return [X0 + 2 + i * 3, Z0 + 2]; }
function norm(s) { return s.replace(/@[0-9a-f]{4,}/g, "@H").replace(/ ?(iotick|phi|soundtick|tickcount|prevpower|pointoffset[xyz]|ticks?|timer|age|animtick)[A-Za-z0-9]*=[^ ]*/g, ""); }
function snapshot() {
  var out = {};
  SPOTS.forEach(function (n, i) {
    var p = pos(i), t = L.te(p[0], Y0, p[1]);
    out[n] = t ? t.getClass().getSimpleName() + " " + norm(L.dump(t)) + " | slots=" + (t.getSizeInventory ? L.slots(t) : "-") : "NO TILE blk=" + L.blk(p[0], Y0, p[1]);
  });
  return out;
}
if (!PHASE_B) {
  if (tick == 0) {
    L.platform(X0, Y0, Z0, 60, 12, 8);
    SPOTS.forEach(function (n, i) {
      var p = pos(i), stack;
      if (n == "GAS") stack = L.engStack(3); else if (n == "SPORT") stack = L.engStack(5); else stack = L.stackOf(n);
      var r = L.place(stack, p[0], Y0, p[1], null, 0);
      if (r.r != "CONSUME") L.say("place " + n + " -> " + r.r);
    });
  }
  if (tick == 30) {
    SPOTS.forEach(function (n, i) { var t = L.te(pos(i)[0], Y0, pos(i)[1]); L.say("spot " + n + " -> " + (t ? t.getClass().getName() : "NONE") + " " + L.blk(pos(i)[0], Y0, pos(i)[1])); });
    var gas = L.te(pos(0)[0], Y0, pos(0)[1]); L.say("gas feed " + L.feed(gas, "rc ethanol", 3333));
    var sp = L.te(pos(1)[0], Y0, pos(1)[1]); L.say("sport feed " + L.feed(sp, "rc ethanol", 2222) + " " + L.feed(sp, "water", 1111));
    var gb = L.te(pos(2)[0], Y0, pos(2)[1]); L.setf(gb, "damage", 37); L.say("gearbox lube " + L.feed(gb, "rc lubricant", 1500)); L.say("gearbox class=" + gb.getClass().getSimpleName() + " dmg=" + L.fs(gb, "damage"));
    var gr = L.te(pos(3)[0], Y0, pos(3)[1]); gr.setInventorySlotContents(0, new L.LIS(LB.iron_ore, 5)); gr.setInventorySlotContents(1, new L.LIS(LItems.wheat, 7));
    var sc = L.te(pos(4)[0], Y0, pos(4)[1]); sc.setInventorySlotContents(5, new L.LIS(LItems.diamond, 12)); sc.setInventorySlotContents(100, new L.LIS(LB.cobblestone, 33));
    var rs = L.te(pos(5)[0], Y0, pos(5)[1]); L.say("reservoir feed " + L.feed(rs, "water", 12345));
    var bf = L.te(pos(6)[0], Y0, pos(6)[1]); bf.setInventorySlotContents(0, new L.LIS(LB.sand, 9));
    var fm = L.te(pos(7)[0], Y0, pos(7)[1]); fm.setInventorySlotContents(0, IR.YEAST.getStackOf()); L.say("fermenter feed " + L.feed(fm, "water", 777));
    var cp = L.te(pos(9)[0], Y0, pos(9)[1]); cp.setInventorySlotContents(0, new L.LIS(LItems.iron_ingot, 4));
    var cr = L.te(pos(11)[0], Y0, pos(11)[1]); cr.setInventorySlotContents(3, new L.LIS(LItems.stick, 11));
    var ex = L.te(pos(12)[0], Y0, pos(12)[1]); ex.setInventorySlotContents(0, new L.LIS(LB.iron_ore, 3));
  }
  if (tick == 60) {
    var snap = snapshot(), lines = [];
    Object.keys(snap).forEach(function (k) { lines.push(k + "\t" + snap[k]); });
    Files.write(Paths.get(MARK), java.util.Arrays.asList(Java.to(lines, "java.lang.String[]")));
    L.say("PHASE A saved snapshot of " + lines.length + " tiles");
    Object.keys(snap).forEach(function (k) { L.say("A " + k + ": " + snap[k].substring(0, 160)); });
  }
} else {
  if (tick == 20) {
    var snap = snapshot();
    var saved = Files.readAllLines(Paths.get(MARK));
    var diffs = 0;
    Java.from(saved).forEach(function (ln) {
      var k = ln.split("\t")[0], a = ln.substring(k.length + 1), b = snap[k];
      if (b === undefined) { L.say("SKIP key '" + k + "' (saved line has " + ln.length + " chars)"); return; }
      if (a === b) { L.say("SAME " + k); return; }
      diffs++;
      // find first differing token
      var ta = a.split(" "), tb = b.split(" "), d = [];
      for (var i = 0; i < Math.max(ta.length, tb.length) && d.length < 6; i++) if (ta[i] !== tb[i]) d.push((ta[i] || "-") + " => " + (tb[i] || "-"));
      L.say("DIFF " + k + ": " + d.join(" ; "));
    });
    L.say("PHASE B compared: " + diffs + " tiles differ");
  }
}
L.run(tick);
