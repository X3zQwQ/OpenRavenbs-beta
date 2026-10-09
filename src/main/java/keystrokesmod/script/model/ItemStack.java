package keystrokesmod.script.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

public class ItemStack {

    public String type;

    public String name;

    public String displayName;

    public int stackSize;

    public int maxStackSize;

    public int durability;

    public int maxDurability;

    public boolean isBlock;

    public net.minecraft.item.ItemStack itemStack;

    public int meta;

    public ItemStack(net.minecraft.item.ItemStack itemStack, byte f1) {
        if (itemStack != null) {
            this.itemStack = itemStack;
            this.isBlock = itemStack.func_77973_b() instanceof ItemBlock;
            this.type = this.isBlock ? ((ItemBlock) itemStack.func_77973_b()).func_179223_d().getClass().getSimpleName() : itemStack.func_77973_b().getClass().getSimpleName();
            this.name = itemStack.func_77973_b().getRegistryName().substring(10);
            this.displayName = itemStack.func_82833_r();
            this.stackSize = itemStack.field_77994_a;
            this.maxStackSize = itemStack.func_77976_d();
            this.durability = itemStack.func_77958_k() - itemStack.func_77952_i();
            this.maxDurability = itemStack.func_77958_k();
            this.meta = itemStack.func_77960_j();
        }
    }

    public ItemStack(String name) {
        this(withMeta(name), (byte) 0);
    }

    private static net.minecraft.item.ItemStack withMeta(String name) {
        String[] parts = name.split(":");
        String itemName = parts[0];
        int meta = 0;
        if (parts.length > 1) {
            meta = parseMeta(parts[1]);
        }
        Item item = (Item) Item.field_150901_e.func_82594_a(new ResourceLocation("minecraft:" + itemName));
        return new net.minecraft.item.ItemStack(item, 1, meta);
    }

    private static int parseMeta(String metaStr) {
        try {
            return Integer.parseInt(metaStr);
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    public List<String> getTooltip() {
        return this.itemStack.func_82840_a(Minecraft.func_71410_x().field_71439_g, true);
    }

    public List<Object[]> getEnchantments() {
        Map<Integer, Integer> enchants = EnchantmentHelper.func_82781_a(this.itemStack);
        if (enchants.isEmpty()) {
            return null;
        } else {
            List<Object[]> enchantments = new ArrayList<>();
            for (Entry<Integer, Integer> entry : enchants.entrySet()) {
                Enchantment enchant = Enchantment.func_180306_c(entry.getKey());
                String name = StatCollector.func_150826_b(enchant.func_77320_a()).toLowerCase().replace(" ", "_");
                enchantments.add(new Object[] { name, entry.getValue() });
            }
            return enchantments;
        }
    }

    public static ItemStack convert(net.minecraft.item.ItemStack itemStack) {
        return itemStack == null ? null : new ItemStack(itemStack, (byte) 0);
    }

    @Override
    public String toString() {
        return "ItemStack(" + this.type + "," + this.name + ")";
    }
}
