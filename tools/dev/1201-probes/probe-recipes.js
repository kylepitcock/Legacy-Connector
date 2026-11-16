function say(s) { log.info("RPROBE " + s); }
if (tick === 1) {
  try {
    var rm = server.getRecipeManager();
    var all = rm.getRecipes(), it = all.iterator(), byNs = {}, ser = {};
    while (it.hasNext()) {
      var h = it.next(), ns = h.id().getNamespace();
      byNs[ns] = (byNs[ns] || 0) + 1;
      var sname = String(h.value().getSerializer().getClass().getName());
      if (sname.indexOf("net.minecraft.world") !== 0) ser[sname] = (ser[sname] || 0) + 1;
    }
    say("recipes " + all.size() + " byNs " + JSON.stringify(byNs));
    say("custom serializers " + JSON.stringify(ser));
    var Unpooled = Java.type("io.netty.buffer.Unpooled");
    var RFB = Java.type("net.minecraft.network.RegistryFriendlyByteBuf");
    var Pkt = Java.type("net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket");
    try {
      var buf = new RFB(Unpooled.buffer(), server.registryAccess());
      Pkt.STREAM_CODEC.encode(buf, new Pkt(all));
      say("recipe sync encoded bytes=" + buf.writerIndex());
      var back = Pkt.STREAM_CODEC.decode(buf);
      say("recipe sync decoded packet " + back);
    } catch (e) { say("recipe sync whole-list FAIL " + e); }
    var RH = Java.type("net.minecraft.world.item.crafting.RecipeHolder");
    var bad = 0, mismatch = 0, ok = 0, it2 = all.iterator(), failNs = {};
    var acc = server.registryAccess();
    while (it2.hasNext()) {
      var rh = it2.next();
      try {
        var b2 = new RFB(Unpooled.buffer(), acc);
        RH.STREAM_CODEC.encode(b2, rh);
        var d = RH.STREAM_CODEC.decode(b2);
        var a1 = rh.value().getResultItem(acc), a2 = d.value().getResultItem(acc);
        if (!Java.type("net.minecraft.world.item.ItemStack").matches(a1, a2)) { if (mismatch++ < 8) say("recipe result mismatch " + rh.id() + " " + a1 + " vs " + a2); }
        else ok++;
      } catch (e) {
        failNs[rh.id().getNamespace()] = (failNs[rh.id().getNamespace()] || 0) + 1;
        if (bad++ < 12) say("recipe sync FAIL " + rh.id() + " " + e);
      }
    }
    say("recipe round trip ok=" + ok + " failures=" + bad + " mismatches=" + mismatch + " failNs=" + JSON.stringify(failNs));
  } catch (e) { say("recipe probe error " + e); }
}
