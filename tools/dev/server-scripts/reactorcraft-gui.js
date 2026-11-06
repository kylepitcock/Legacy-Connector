// Server side of every ReactorCraft tile's GUI: places each ReactorTiles block on a flat platform and asks the mod's GUI handler for its container
// (the server half of a right-click), then builds the menu. Prints "GUI results" and the failures.
// run: ./gradlew runServerSubTest -PautostopTicks=200 -PserverTickScriptFrom=100 -PserverTickScript=<abs path of this file>
var __p = String(java.lang.System.getProperty("legacyconnector.serverScript") || java.lang.System.getProperty("legacyconnector.serverTickScript")).split("\\").join("/");
load(__p.substring(0, __p.lastIndexOf("/") + 1) + "lib-reika.js");
var Y = 100, X = 300, Z = 300;
var RT = Java.type("Reika.ReactorCraft.Registry.ReactorTiles");
var GB = Java.type("org.kylepitcock.legacyconnector.v1_7_10.bridge.GuiBridge");
var LM = Java.type("org.kylepitcock.legacyconnector.v1_7_10.gui.LegacyMenu");
var EB = Java.type("org.kylepitcock.legacyconnector.v1_7_10.bridge.EntityBridge");
var LW = Java.type("legacyconnector.v1_7_10.net.minecraft.world.World");
var out = "";
var NAMES = Java.from(RT.values()).map(function (m) { return m.name(); });
function pos(i) { return [X + 2 + (i % 12) * 3, Z + 2 + Math.floor(i / 12) * 3]; }
if (tick == 0) flat(X - 2, Y, Z - 2, 12 * 3 + 6, Math.ceil(NAMES.length / 12) * 3 + 6, 6);
YAW = -90;
NAMES.forEach(function (n, i) {
  if (tick == 3 + i) {
    var q = pos(i);
    try { var r = place(ms(RT.valueOf(n).getCraftedProduct()), q[0], Y, q[1]); if (r != "CONSUME") G("np_" + n, r); } catch (e) { G("np_" + n, "EXC " + e); }
  }
});
var T1 = 3 + NAMES.length + 10;
if (tick == T1) {
  var handler = null;
  var f = GB.class.getDeclaredField("HANDLERS"); f.setAccessible(true);
  Java.from(f.get(null).entrySet().toArray()).forEach(function (e) { if (("" + e.getKey().getClass().getName()).indexOf("Reika.ReactorCraft") == 0) handler = e.getValue(); });
  var p = fp(), lp = EB.wrap(p), lw = LW.mcc$wrap(lvl());
  var rows = [], noGui = [], fails = [], np = [], win = 1;
  NAMES.forEach(function (n, i) {
    if (G("np_" + n)) { np.push(n + "=" + G("np_" + n)); return; }
    var q = pos(i);
    try {
      var el = handler.getServerGuiElement(0, lp, lw, q[0], Y, q[1]);
      if (el == null) { noGui.push(n); return; }
      var menu = new LM(GB.menuType(), win++, p.getInventory(), el, null);
      rows.push(n + ":" + el.getClass().getSimpleName() + " slots=" + menu.slots.size());
    } catch (e) { fails.push(n + " EXC " + e); }
  });
  out += "GUI results: handler=" + (handler ? handler.getClass().getName() : "NONE") + " " + rows.length + " containers, " + noGui.length + " without container, " + fails.length + " exceptions, " + np.length + " not placed\n";
  out += "NOGUI " + noGui.join(" ") + "\nNOTPLACED " + np.join(" ") + "\nFAILS " + fails.join(" | ") + "\n" + rows.join("\n");
}
out;
