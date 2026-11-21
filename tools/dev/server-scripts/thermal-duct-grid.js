// Thermal Dynamics duct grids (issue 303): places a row of energy ducts and reports whether each duct got a grid (the grid container is a Level capability
// attached through AttachCapabilitiesEvent; a duct without a grid fails removal with "grid is null" and never moves energy).
// run: ./gradlew runServerSubTest -PsubTestDir=<dir with the Thermal 1.20.1 jars> -PautostopTicks=200 -PserverTickScriptFrom=0 -PserverTickScript=<abs path of this file>
var R = Java.type("net.minecraft.core.registries.BuiltInRegistries");
var BP = Java.type("net.minecraft.core.BlockPos");
var RL = Java.type("net.minecraft.resources.ResourceLocation");
var X = 300, Z = 300, Y = 150;
function lv() { return server.overworld(); }
function rep() {
  for (var i = 0; i < 4; i++) {
    var pos = new BP(X, Y, Z + i);
    var be = lv().getBlockEntity(pos);
    var msg = "DUCT " + i + " be=" + (be == null ? "null" : be.getClass().getSimpleName());
    try {
      var lg = be.legacy ? be.legacy() : be;
      msg += " legacy=" + lg.getClass().getName() + " hasGrid=" + lg.hasGrid();
      var g = lg.getGrid();
      msg += " grid=" + (g == null ? "null" : g.getClass().getSimpleName() + " nodes=" + g.getNodes().size());
    } catch (e) { msg += " ERR " + e; }
    log.info("SERVERSCRIPT: " + msg);
  }
}
if (tick == 3) {
  lv().getChunk(X >> 4, Z >> 4);
  var blk = R.BLOCK.get(RL.parse("thermal:energy_duct"));
  log.info("SERVERSCRIPT: block " + blk);
  for (var i = 0; i < 3; i++) lv().setBlock(new BP(X, Y, Z + i), blk.defaultBlockState(), 3);
}
if (tick == 20) rep();
if (tick == 25) {
  log.info("SERVERSCRIPT: removing middle");
  try { lv().removeBlock(new BP(X, Y, Z + 1), false); } catch (e) { log.info("SERVERSCRIPT: remove failed " + e); }
}
if (tick == 40) rep();
