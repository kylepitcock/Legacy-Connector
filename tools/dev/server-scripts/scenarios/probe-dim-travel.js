// Dev probe (#325): a real player travels to a mod dimension the way a portal block does (legacy travelToDimension and
// ServerConfigurationManager.transferPlayerToDimension with the mod's Teleporter) and the script prints where he ends up.
// Prepend tools/dev/server-scripts/lib.js (cat lib.js probe-dim-travel.js > out.js).
var DIMS = [44, 45, 1, 0];
if (tick == 120) { SP = realPlayer("Traveller"); log.info("SERVERSCRIPT: player in " + SP.level().dimension().location() + " at " + SP.blockPosition()); }
if (tick >= 140 && tick % 100 == 40 && DIMS.length) {
  var d = DIMS.shift();
  try {
    legacy(SP).travelToDimension(d);
  } catch (e) { log.info("SERVERSCRIPT: travelToDimension(" + d + ") threw " + e); }
  NEXT = tick + 40;
}
if (typeof NEXT !== "undefined" && tick == NEXT) {
  var lv = SP.level(), p = SP.blockPosition();
  var below = lv.getBlockState(p.below()), here = lv.getBlockState(p);
  log.info("SERVERSCRIPT: after travel: " + lv.dimension().location() + " at " + p + " y-range " + lv.getMinBuildHeight() + ".." + lv.getMaxBuildHeight() + " below=" + below.getBlock() + " here=" + here.getBlock() + " alive=" + SP.isAlive() + " hp=" + SP.getHealth());
}
