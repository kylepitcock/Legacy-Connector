// Legacy FluidContainerRegistry vs modern containers (#105 tankers): a modern bucket of a modern fluid must read as a filled container of a
// (mirrored) legacy fluid, and a legacy tank must fill from it.
// run: ./gradlew runServerSubTest -PsubTestDir=run-tc -PautostopTicks=130 -PserverTickScriptFrom=100 -PserverTickScript=<abs path>
var __p = String(java.lang.System.getProperty("legacyconnector.serverTickScript")).split("\\").join("/");
load(__p.substring(0, __p.lastIndexOf("/") + 1) + "lib-reika.js");
var FCR = Java.type("legacyconnector.v1_7_10.net.minecraftforge.fluids.FluidContainerRegistry");
var LStack = Java.type("legacyconnector.v1_7_10.net.minecraft.item.ItemStack");
var LItems = Java.type("legacyconnector.v1_7_10.net.minecraft.init.Items");
var FReg = Java.type("legacyconnector.v1_7_10.net.minecraftforge.fluids.FluidRegistry");
function name(ls) { return ls == null ? "null" : String(R.ITEM.getKey(ls.mcc$modern().getItem())); }
if (tick == 1) {
  var out = [];
  ["minecraft:milk_bucket", "minecraft:water_bucket", "minecraft:lava_bucket", "minecraft:powder_snow_bucket", "minecraft:bucket"].forEach(function (id) {
    var ms = new IS(R.ITEM.get(RL.parse(id)), 1);
    var ls = LStack.mcc$wrap(ms);
    var fl = FCR.getFluidForFilledItem(ls);
    var drained = FCR.drainFluidContainer(ls);
    out.push(id + ": fluid=" + (fl == null ? "null" : fl.getFluid().getName() + "x" + fl.amount) + " empty=" + FCR.isEmptyContainer(ls) + " filled=" + FCR.isFilledContainer(ls) + " drained=" + name(drained) + " cap=" + FCR.getContainerCapacity(ls));
  });
  // an empty bucket filled with the mirrored milk
  var FB = Java.type("org.kylepitcock.legacyconnector.v1_7_10.bridge.FluidBridge");
  var VF = Java.type("net.minecraft.world.level.material.Fluids");
  out.push("flowing lava -> " + FB.legacyFor(VF.FLOWING_LAVA).getName() + ", flowing water -> " + FB.legacyFor(VF.FLOWING_WATER).getName());
  var mirrored = null;
  var milk = mirrored;
  if (milk != null) {
    var Fs = Java.type("legacyconnector.v1_7_10.net.minecraftforge.fluids.FluidStack");
    var filled = FCR.fillFluidContainer(new Fs(milk, 1000), new LStack(LItems.bucket));
    out.push("fill milk into bucket -> " + name(filled));
  }
  out.push("registered fluids: " + FReg.getRegisteredFluids().keySet());
  out.join("\n");
}
