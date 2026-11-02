// What the planet selection screen's S_TELEPORT_ENTITY does: transfers the player (with the rocket) to the dimension DEST (default the Moon).
var p = GC.player();
var EB = Java.type("org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122");
var WU = Java.type("micdoodle8.mods.galacticraft.core.util.WorldUtil");
var dest = (typeof DEST === "undefined") ? CM.idDimensionMoon : DEST;
var lp = EB.wrap(p);
var ws = WB.wrap(p.serverLevel());
var o = "before: " + p.level().dimension().location() + " y " + p.getY() + " rocketAlive " + ROCKET.isAlive() + "\n";
var r = WU.transferEntityToDimension(lp, dest, ws, false, LROCKET);
o += "result " + r + "\n";
o += "after: " + p.level().dimension().location() + " pos " + p.blockPosition() + " riding " + p.isPassenger() + "\n";
o;
