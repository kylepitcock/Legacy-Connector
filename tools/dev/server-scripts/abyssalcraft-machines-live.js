var LItem = Java.type("legacyconnector.v1_12_2.net.minecraft.item.Item");
var LIS = Java.type("legacyconnector.v1_12_2.net.minecraft.item.ItemStack");
function it(n, c, m) { var i = LItem.getByNameOrId(n); return i == null ? null : new LIS(i, c || 1, m || 0); }
var X0 = G.X0, Y0 = G.Y0, Z0 = G.Z0;
var t0 = L.te(X0 + 2, Y0, Z0 + 2), t1 = L.te(X0 + 5, Y0, Z0 + 2), t2 = L.te(X0 + 8, Y0, Z0 + 2);
var o = [];
t0.setInventorySlotContents(0, it("abyssalcraft:ingotnugget", 4));
t0.setInventorySlotContents(1, it("abyssalcraft:dreadfragment", 8));
t1.setInventorySlotContents(0, it("abyssalcraft:coin", 3));
t1.setInventorySlotContents(1, it("abyssalcraft:engraving_azathoth"));
o.push(L.slots(t0)); o.push(L.slots(t1));
var MR = Java.type("com.shinoow.abyssalcraft.api.recipe.MaterializerRecipes").instance();
var l=MR.getMaterializationList(); var m=l.get(0); o.push("MAT in "+java.util.Arrays.toString(m.input)+" out "+m.output);
G.mat = m;
o.join("\n")
