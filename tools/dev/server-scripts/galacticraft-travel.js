// Planet travel (issue #131) on a headless server: a real player is moved with WorldUtil.transferEntityToDimension as the planet selection screen does,
// then the script reads where the player ended up. Run: cat lib.js galacticraft-lib.js galacticraft-travel.js > run.js, then
// (the file is evaluated on every tick; TRAVEL keeps the state)
// ./gradlew runServerSubTest -PautostopTicks=400 -PserverTickScriptFrom=150 -PserverTickScript=<run.js>. Output: SERVERSCRIPT: lines.
if (typeof TRAVEL === "undefined") TRAVEL = { step: 0, p: null, ticks: 0, done: false };
function onTick(n) {
  var o = "";
  var EB = Java.type("org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122");
  var WU = Java.type("micdoodle8.mods.galacticraft.core.util.WorldUtil");
  if (TRAVEL.step == 0) {
    TRAVEL.p = realPlayer("Traveller");
    TRAVEL.step = 1;
    TRAVEL.ticks = 0;
    return false;
  }
  TRAVEL.ticks++;
  var p = TRAVEL.p;
  if (TRAVEL.step == 1 && TRAVEL.ticks >= 20) {
    var dest = (typeof DEST === "undefined") ? CM.idDimensionMoon : DEST;
    o += "before: " + p.level().dimension().location() + " removed " + p.isRemoved() + "\n";
    o += P("transfer", function () {
      var lp = EB.wrap(p);
      return "result " + WU.transferEntityToDimension(lp, dest, WB.wrap(p.serverLevel()), false, null);
    });
    TRAVEL.step = 2;
    TRAVEL.ticks = 0;
    log.info("SERVERSCRIPT: " + o);
    return false;
  }
  if (TRAVEL.step == 2 && TRAVEL.ticks >= 40) {
    var pl = server.getPlayerList().getPlayers();
    var cur = null;
    for (var i = 0; i < pl.size(); i++) if (("" + pl.get(i).getGameProfile().getName()) == "Traveller") cur = pl.get(i);
    o += "after: in list " + (cur != null) + (cur != null ? " dim " + cur.level().dimension().location() + " removed " + cur.isRemoved() + " pos " + cur.blockPosition() : "") + "\n";
    o += "original player removed " + p.isRemoved() + " dim " + p.level().dimension().location() + " sameObject " + (cur === p) + "\n";
    log.info("SERVERSCRIPT: " + o);
    return true;
  }
  return false;
}
(TRAVEL.done || onTick(tick)) ? (TRAVEL.done = true, "done") : "travel step " + TRAVEL.step;
