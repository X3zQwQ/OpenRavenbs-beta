package keystrokesmod.utility;

import java.util.Map.Entry;
import net.minecraft.block.Block;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EnumCreatureAttribute;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemHoe;
import net.minecraft.item.ItemStack;

public final class ItemSortScoring {

    private static final double FIRE_ASPECT_BONUS = 4.0;

    private static final double FLAME_BONUS = 4.0;

    private static final double BOW_KNOCKBACK_BONUS = 0.15;

    private static final double MELEE_KNOCKBACK_BONUS = 0.1;

    private static final double FULLY_CHARGED_ARROW_SPEED = 3.0;

    private static final double BASE_ARROW_DAMAGE = 2.0;

    private ItemSortScoring() {
    }

    public static double getMeleeDamage(ItemStack stack) {
        if (stack == null) {
            return 0.0;
        } else {
            double damage = getAttackDamageAttribute(stack);
            damage += EnchantmentHelper.func_152377_a(stack, EnumCreatureAttribute.UNDEFINED);
            return damage + EnchantmentHelper.func_77506_a(Enchantment.field_77334_n.field_77352_x, stack) * 4.0;
        }
    }

    public static double getMeleeSelectionScore(ItemStack stack) {
        if (stack == null) {
            return 0.0;
        } else {
            double score = getMeleeDamage(stack);
            score += EnchantmentHelper.func_77506_a(Enchantment.field_180313_o.field_77352_x, stack) * 0.1;
            return score + getDurabilityTieBreaker(stack);
        }
    }

    public static double getBowDamage(ItemStack stack) {
        if (stack != null && stack.func_77973_b() instanceof ItemBow) {
            int powerLevel = EnchantmentHelper.func_77506_a(Enchantment.field_77345_t.field_77352_x, stack);
            int flameLevel = EnchantmentHelper.func_77506_a(Enchantment.field_77343_v.field_77352_x, stack);
            double projectileDamage = 2.0 + powerLevel * 0.5 + 0.5;
            double impactDamage = 3.0 * projectileDamage;
            double damage = impactDamage + impactDamage * 0.25 + 0.5;
            return damage + flameLevel * 4.0;
        } else {
            return 0.0;
        }
    }

    public static double getBowSelectionScore(ItemStack stack) {
        if (stack == null) {
            return 0.0;
        } else {
            double score = getBowDamage(stack);
            score += EnchantmentHelper.func_77506_a(Enchantment.field_77344_u.field_77352_x, stack) * 0.15;
            return score + getDurabilityTieBreaker(stack);
        }
    }

    public static double getToolSelectionScore(ItemStack stack) {
        return Math.max(Math.max(getAxeScore(stack), getPickaxeScore(stack)), Math.max(Math.max(getShovelScore(stack), getHoeScore(stack)), getShearsScore(stack)));
    }

    public static double getAxeScore(ItemStack stack) {
        return getBlockBreakingScore(stack, Blocks.field_150364_r);
    }

    public static double getPickaxeScore(ItemStack stack) {
        return getBlockBreakingScore(stack, Blocks.field_150348_b);
    }

    public static double getShovelScore(ItemStack stack) {
        return getBlockBreakingScore(stack, Blocks.field_150346_d);
    }

    public static double getHoeScore(ItemStack stack) {
        if (stack != null && stack.func_77973_b() instanceof ItemHoe) {
            double score = stack.func_77958_k();
            return score + getDurabilityTieBreaker(stack);
        } else {
            return 0.0;
        }
    }

    public static double getShearsScore(ItemStack stack) {
        return getBlockBreakingScore(stack, Blocks.field_150321_G);
    }

    public static double getBlockBreakingScore(ItemStack stack, Block block) {
        double speed = getBlockBreakingSpeed(stack, block);
        return speed > 0.0 ? speed + getDurabilityTieBreaker(stack) : 0.0;
    }

    public static double getBlockBreakingSpeed(ItemStack stack, Block block) {
        if (stack != null && block != null) {
            float speed = stack.func_150997_a(block);
            if (speed > 1.0F) {
                int efficiencyLevel = EnchantmentHelper.func_77506_a(Enchantment.field_77349_p.field_77352_x, stack);
                if (efficiencyLevel > 0) {
                    speed += efficiencyLevel * efficiencyLevel + 1;
                }
            }
            if (speed <= 1.0F) {
                return 0.0;
            } else {
                if (!block.func_149688_o().func_76229_l() && !stack.func_150998_b(block)) {
                    speed *= 0.3F;
                }
                return speed;
            }
        } else {
            return 0.0;
        }
    }

    private static double getAttackDamageAttribute(ItemStack stack) {
        if (stack == null) {
            return 0.0;
        } else {
            for (Entry<String, AttributeModifier> entry : stack.func_111283_C().entries()) {
                if (SharedMonsterAttributes.field_111264_e.func_111108_a().equals(entry.getKey())) {
                    return entry.getValue().func_111164_d();
                }
            }
            return 0.0;
        }
    }

    private static double getDurabilityTieBreaker(ItemStack stack) {
        if (stack != null && stack.func_77984_f()) {
            int maxDurability = stack.func_77958_k();
            if (maxDurability <= 0) {
                return 0.0;
            } else {
                int remainingDurability = maxDurability - stack.func_77952_i();
                return (double) remainingDurability / maxDurability / 1000.0;
            }
        } else {
            return 0.0;
        }
    }
}
