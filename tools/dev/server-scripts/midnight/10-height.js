var res = "";
WB = T("org.kylepitcock.legacyconnector.v1_12_2.bridge.WorldBridge1122");
LBP = T("legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos");
function lbp(a, b, c) { var I = Java.type("int").class; return LBP.class.getConstructor(I, I, I).newInstance(java.lang.Integer.valueOf(a), java.lang.Integer.valueOf(b), java.lang.Integer.valueOf(c)); }
var md = null;
server.getAllLevels().forEach(function (l) { if (String(l.dimension().location()).indexOf("dim_-23") >= 0) md = l; });
var lw = WB.wrap(md);
[[-51, 22], [500, 500], [-600, 300]].forEach(function (c) {
  res += P("height " + c, function () {
    var p = lbp(c[0], 0, c[1]);
    var ch = lw.getChunkFromBlockCoords(p);
    return "chunk " + ch + " getHeight=" + ch.getHeight(p) + " topSolid=" + lw.getTopSolidOrLiquidBlock(p) + " heightValue=" + lw.getHeight(p);
  });
});
res;
log.info("SSOUT " + res);
