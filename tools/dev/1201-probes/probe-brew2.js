function say(s) { log.info("BPROBE " + s); }
if (tick === 1) {
  try {
    var pb = server.potionBrewing();
    var f = pb.getClass().getDeclaredField("registry");
    f.setAccessible(true);
    var reg = f.get(pb);
    var rf = reg.getClass().getDeclaredFields();
    for (var i = 0; i < rf.length; i++) {
      rf[i].setAccessible(true);
      var v = rf[i].get(reg);
      if (v != null && v.size && v.iterator) {
        var it = v.iterator(), by = {};
        while (it.hasNext()) { var o = it.next(); var n = String(o.getClass().getName()); by[n] = (by[n] || 0) + 1; }
        say("registry field " + rf[i].getName() + " size " + v.size() + " " + JSON.stringify(by));
      }
    }
    var LB = Java.type("org.kylepitcock.legacyconnector.v1_20_1.migrate.LegacyBrewing");
    say("LegacyBrewing loaded " + LB);
    var Items = Java.type("net.minecraft.world.item.Items");
    var Potions = Java.type("net.minecraft.world.item.alchemy.Potions");
    var PC = Java.type("net.minecraft.world.item.alchemy.PotionContents");
    var IS = Java.type("net.minecraft.world.item.ItemStack");
    var awk = PC.createItemStack(Items.POTION, Potions.AWKWARD);
    say("vanilla hasMix(awkward, nether_wart)=" + pb.hasMix(awk, new IS(Items.NETHER_WART)) + " sample output " + pb.mix(new IS(Items.NETHER_WART), awk));
  } catch (e) { say("error " + e); }
}
