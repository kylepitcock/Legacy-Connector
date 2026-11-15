// Traincraft diagonal track probe (#105): places a diagonal piece through the mod's own item use (four yaws), then reports each placed block,
// its meta, the tile type and the colliding boxes the legacy world reports for a bogie-sized box above the rail.
// run: ./gradlew runServerSubTest -PsubTestDir=run-wb -PautostopTicks=300 -PserverTickScriptFrom=100 -PserverTickScript=<abs path>
var __p = String(java.lang.System.getProperty("legacyconnector.serverTickScript")).split("\\").join("/");
load(__p.substring(0, __p.lastIndexOf("/") + 1) + "lib-reika.js");
var X = 100, Y = 60, Z = 100;
var ITEMNAME = "tc:" + String(java.lang.System.getProperty("tc.item") || "");
function legacy(e) { return Java.type("org.kylepitcock.legacyconnector.v1_7_10.bridge.EntityBridge").wrap(e); }
function legacyWorld() { return Java.type("legacyconnector.v1_7_10.net.minecraft.world.World").mcc$wrap(lvl()); }

if (typeof CARTS === "undefined") CARTS = [];
var ENT = Java.type("net.minecraft.core.registries.BuiltInRegistries").ENTITY_TYPE;
function describe(e) { return e.getType() + " @" + e.getX().toFixed(2) + "," + e.getY().toFixed(2) + "," + e.getZ().toFixed(2) + " v=" + e.getDeltaMovement().x.toFixed(3) + "," + e.getDeltaMovement().z.toFixed(3); }
if (tick == 0) { flat(X - 6, Y, Z - 6, 60, 30, 6); "flat" }
else if (tick == 2) {
  YAW = -170;
  var r = place(modItem("tc:tcraillongdiagonalstraight"), X, Y, Z, Dir.UP, Y - 1);
  YAW = -80;
  var r2 = place(modItem("tc:tcrailverylongstraight"), X + 20, Y, Z, Dir.UP, Y - 1);
  "placed diag=" + r + " straight=" + r2;
} else if (tick == 6) {
  var out = "";
  var specs = [[X + 1.5, Z - 0.5, 0.15, -0.15, "diag"], [X + 20.5, Z + 0.5, 0.15, 0.0, "straight"]];
  specs.forEach(function (sp) {
    var type = ENT.get(RL.parse("tc:5plank"));
    var e = type.create(lvl());
    e.setPos(sp[0], Y + 0.3, sp[1]);
    lvl().addFreshEntity(e);
    e.setDeltaMovement(sp[2], 0, sp[3]);
    CARTS.push([e, sp[4]]);
    out += sp[4] + ":" + describe(e) + " ";
  });
  out;
} else if (tick > 6 && tick < 600 && (typeof LOGN === "undefined" || LOGN < 30)) {
  if (typeof LOGN === "undefined") { LOGN = 0; LAST = ""; }
  var LE = Java.type("legacyconnector.v1_7_10.net.minecraft.entity.Entity");
  var lw = legacyWorld();
  var c = CARTS[0], e = c[0], le = legacy(e), bb = LE.mcc$get$boundingBox(le);
  var cur = describe(e);
  if (cur != LAST) {
    LAST = cur; LOGN++;
    "t" + tick + " " + cur + " boxY=" + bb.minY.toFixed(2) + " collide=" + lw.getCollidingBoundingBoxes(le, bb).size() + " meta/blk=" + lw.getBlock(Math.floor(e.getX()), Math.floor(e.getY()), Math.floor(e.getZ())) ;
  }
}
