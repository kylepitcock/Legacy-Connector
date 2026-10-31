// Size manipulation: Ant-Man shrinks / Atom Smasher grows: the scale var drives the hitbox (PlayerSize.apply -> Entity.setSize), eye height and step height.
join("Hank"); flat(300, 300, 100); tp(300.5, 101, 300.5); wear("ant_man");
var F = function (x) { return new (Java.type("java.lang.Float"))(x); };
scenario([
  function () { return 5; },
  function () { out("worn " + worn() + " box " + P.getBbWidth().toFixed(2) + "x" + P.getBbHeight().toFixed(2) + " eye " + P.getEyeHeight().toFixed(2) + " scaleAttr " + P.getAttributeValue(Java.type("net.minecraft.world.entity.ai.attributes.Attributes").SCALE)); Vars.SCALE.set(L(), F(0.2)).sync(); return 8; },
  function () { out("scale 0.2: var " + Vars.SCALE.get(L()) + " box " + P.getBbWidth().toFixed(2) + "x" + P.getBbHeight().toFixed(2) + " eye " + P.getEyeHeight().toFixed(2) + " step " + P.maxUpStep()); Vars.SCALE.set(L(), F(3.0)).sync(); return 8; },
  function () { out("scale 3.0: var " + Vars.SCALE.get(L()) + " box " + P.getBbWidth().toFixed(2) + "x" + P.getBbHeight().toFixed(2) + " eye " + P.getEyeHeight().toFixed(2) + " step " + P.maxUpStep()); Vars.SCALE.set(L(), F(1.0)).sync(); return 8; },
  function () { out("scale 1.0: var " + Vars.SCALE.get(L()) + " box " + P.getBbWidth().toFixed(2) + "x" + P.getBbHeight().toFixed(2) + " eye " + P.getEyeHeight().toFixed(2)); out("sent: " + sentSummary()); }
]);
"size probe started"
