// Spawns nothing: after the entity self test has populated the overworld, finds hosted legacy mobs with a non-zero armour value and hurts a
// few of each type with generic damage, comparing the health lost with the 1.7.10 formula damage*(25-armor)/25 (#283).
// run: ./gradlew runServerSubTest -PsubTestDir=run-wb -PautostopTicks=1800 -PserverTickScriptFrom=900 -PserverTickScript=<abs path>
var Mob = Java.type("org.kylepitcock.legacyconnector.v1_7_10.entity.LegacyMobHost");
var level = server.overworld();
if (typeof done === "undefined") { done = 0; }
if (tick % 20 == 0 && done < 8) {
  var seen = {};
  var out = "";
  var it = level.getAllEntities().iterator();
  var list = [];
  while (it.hasNext()) list.push(it.next());
  for (var i = 0; i < list.length && done < 8; i++) {
    var e = list[i];
    if (!(e instanceof Mob)) continue;
    var a = e.getArmorValue();
    var key = String(e.getType().toString());
    if (a <= 0 || seen[key] || !e.isAlive()) continue;
    seen[key] = 1;
    e.setHealth(e.getMaxHealth());
    var before = e.getHealth();
    e.invulnerableTime = 0;
    var src = level.damageSources().cactus();
    var m = Mob.class.getDeclaredMethod("getDamageAfterArmorAbsorb", src.getClass().getSuperclass() == null ? src.getClass() : Java.type("net.minecraft.world.damagesource.DamageSource").class, java.lang.Float.TYPE);
    m.setAccessible(true);
    var viaFormula = m.invoke(e, src, java.lang.Float.valueOf(10.0));
    var ok = e.hurt(src, 10.0);
    var lost = before - e.getHealth();
    out += key + " armor=" + a + " hurt=" + ok + " lost=" + lost + " formula=" + viaFormula + " health=" + e.getHealth() + "/" + e.getMaxHealth() + " class=" + e.getClass().getSimpleName() + " expected=" + (10 * (25 - Math.min(25, a)) / 25) + "; ";
    done++;
  }
  if (out) out; else (tick > 200 && done == 0 ? "no armoured legacy mob found" : undefined);
}
