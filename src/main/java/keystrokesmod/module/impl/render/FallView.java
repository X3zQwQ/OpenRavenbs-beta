package keystrokesmod.module.impl.render;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import keystrokesmod.event.PlayerTeleportEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.impl.client.Settings;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.DescriptionSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.Utils;
import net.minecraft.block.material.Material;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.BlockPos;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.RenderTickEvent;

public class FallView extends Module {

    private ButtonSetting disableWhileFlying;

    private ButtonSetting onlyWhileSneaking;

    private ButtonSetting overrideHealthFormat;

    private ButtonSetting showDamage;

    private ButtonSetting showDistance;

    private ButtonSetting showRemainingHealth;

    private SliderSetting damageThreshold;

    private double fallStartY = -1.0;

    private double groundY = -1.0;

    private float cachedFallDistance = 0.0F;

    private int cachedEnchantmentModifier = -1;

    private ItemStack[] cachedArmorInventory = new ItemStack[4];

    private boolean armorCacheValid = false;

    private final Map<FallView.DamageCacheKey, Integer> damageCache = new HashMap<>();

    private static final int MAX_CACHE_SIZE = 1000;

    private String damageText;

    private boolean showDamageText;

    private String distanceText;

    private int distanceTextColor = Color.WHITE.getRGB();

    private boolean showDistanceText;

    private boolean recalculateAfterTeleport;

    public FallView() {
        super("Fall View", Module.category.render);
        this.registerSetting(new DescriptionSetting("Shows fall distance damage."));
        this.registerSetting(this.damageThreshold = new SliderSetting("Damage threshold", "%", 0.0, 0.0, 100.0, 5.0));
        this.registerSetting(this.disableWhileFlying = new ButtonSetting("Disable while flying", true));
        this.registerSetting(this.onlyWhileSneaking = new ButtonSetting("Only while sneaking", false));
        this.registerSetting(this.overrideHealthFormat = new ButtonSetting("Override health format", true));
        this.registerSetting(this.showDamage = new ButtonSetting("Show damage", true));
        this.registerSetting(this.showDistance = new ButtonSetting("Show distance", false));
        this.registerSetting(this.showRemainingHealth = new ButtonSetting("Show remaining health", false));
    }

    @Override
    public void onDisable() {
        this.fallStartY = -1.0;
        this.groundY = -1.0;
        this.cachedFallDistance = 0.0F;
        this.cachedEnchantmentModifier = -1;
        this.cachedArmorInventory = new ItemStack[4];
        this.armorCacheValid = false;
        this.damageCache.clear();
        this.recalculateAfterTeleport = false;
        this.clearOverlayState();
    }

    @SubscribeEvent
    public void onClientTick(ClientTickEvent ev) {
        if (ev.phase == Phase.END) {
            this.clearOverlayState();
            if (mc.field_71462_r == null) {
                if (Utils.nullCheck() && !mc.field_71439_g.field_71075_bZ.field_75098_d) {
                    if (this.recalculateAfterTeleport) {
                        this.recalculateAfterTeleport = false;
                        this.recalculateFallTracking();
                    }
                    if (!this.disableWhileFlying.isToggled() || !mc.field_71439_g.field_71075_bZ.field_75101_c) {
                        if (!this.onlyWhileSneaking.isToggled() || mc.field_71439_g.func_70093_af()) {
                            boolean onGround = mc.field_71439_g.field_70122_E;
                            if (onGround) {
                                this.fallStartY = -1.0;
                                this.groundY = -1.0;
                                this.cachedFallDistance = 0.0F;
                            } else if (this.fallStartY == -1.0) {
                                this.fallStartY = mc.field_71439_g.field_70163_u;
                                this.groundY = this.findGroundY(mc.field_71439_g.field_70165_t, mc.field_71439_g.field_70161_v);
                            } else {
                                double newGroundY = this.findGroundY(mc.field_71439_g.field_70165_t, mc.field_71439_g.field_70161_v);
                                if (newGroundY != this.groundY) {
                                    this.groundY = newGroundY;
                                    this.cachedFallDistance = 0.0F;
                                }
                            }
                            float fallDistance = this.calculateFallDistance();
                            if (!(fallDistance <= 2.5F)) {
                                PotionEffect jumpEffect = mc.field_71439_g.func_70660_b(Potion.field_76430_j);
                                float jumpAmplifier = jumpEffect != null ? jumpEffect.func_76458_c() + 1 : 0.0F;
                                int jumpBoostLevel = jumpEffect != null ? jumpEffect.func_76458_c() + 1 : 0;
                                PotionEffect resistanceEffect = mc.field_71439_g.func_70660_b(Potion.field_76429_m);
                                boolean hasResistance = resistanceEffect != null;
                                int resistanceLevel = hasResistance ? resistanceEffect.func_76458_c() + 1 : 0;
                                ItemStack[] armorInventory = new ItemStack[4];
                                boolean armorChanged = false;
                                int armorHash = 0;
                                for (int i = 0; i < 4; i++) {
                                    ItemStack currentArmor = mc.field_71439_g.field_71071_by.func_70440_f(i);
                                    armorInventory[i] = currentArmor;
                                    if (this.cachedArmorInventory[i] != currentArmor) {
                                        armorChanged = true;
                                    }
                                    if (currentArmor != null) {
                                        armorHash = armorHash * 31 + (currentArmor.func_77973_b() != null ? currentArmor.func_77973_b().hashCode() : 0);
                                        armorHash = armorHash * 31 + currentArmor.func_77952_i();
                                        armorHash = armorHash * 31 + EnchantmentHelper.func_77506_a(Enchantment.field_180309_e.field_77352_x, currentArmor);
                                    }
                                }
                                int enchantmentModifier = this.cachedEnchantmentModifier;
                                if (armorChanged || !this.armorCacheValid) {
                                    long totalModifier = 0L;
                                    for (int i = 0; i < 100; i++) {
                                        int mod = EnchantmentHelper.func_77508_a(armorInventory, DamageSource.field_76379_h);
                                        if (mod > 20) {
                                            mod = 20;
                                        }
                                        totalModifier += mod;
                                    }
                                    enchantmentModifier = (int) Math.round(totalModifier / 100.0);
                                    this.cachedEnchantmentModifier = enchantmentModifier;
                                    System.arraycopy(armorInventory, 0, this.cachedArmorInventory, 0, 4);
                                    this.armorCacheValid = true;
                                    if (this.damageCache.size() > 500) {
                                        this.damageCache.clear();
                                    }
                                }
                                FallView.DamageCacheKey cacheKey = new FallView.DamageCacheKey(armorHash, fallDistance, jumpBoostLevel, resistanceLevel);
                                Integer cachedFinalDamage = this.damageCache.get(cacheKey);
                                int finalDamage;
                                if (cachedFinalDamage != null) {
                                    finalDamage = cachedFinalDamage;
                                } else {
                                    float damagePoints = fallDistance - 3.0F - jumpAmplifier;
                                    double damage = Math.max(0, MathHelper.func_76143_f(damagePoints));
                                    if (hasResistance && damage > 0.0) {
                                        int resistanceReduction = resistanceLevel * 5;
                                        int damageMultiplier = 25 - resistanceReduction;
                                        damage = damageMultiplier * damage / 25.0;
                                    }
                                    if (damage > 0.0 && enchantmentModifier > 0) {
                                        damage = (25 - enchantmentModifier) * damage / 25.0;
                                    }
                                    finalDamage = MathHelper.func_76143_f(damage);
                                    if (this.damageCache.size() >= 1000) {
                                        this.damageCache.clear();
                                    }
                                    this.damageCache.put(cacheKey, finalDamage);
                                }
                                double currentHealth = mc.field_71439_g.func_110143_aJ();
                                double damagePercent = finalDamage / currentHealth * 100.0;
                                if (this.showDamage.isToggled() && finalDamage > 0 && damagePercent > this.damageThreshold.getInput()) {
                                    double displayVal = this.showRemainingHealth.isToggled() ? Math.max(0.0, currentHealth - finalDamage) : finalDamage;
                                    float hearts = (float) displayVal;
                                    if (Settings.showHealthAsHearts.isToggled() || this.overrideHealthFormat.isToggled()) {
                                        hearts = (float) (displayVal / 2.0);
                                        hearts = (float) Utils.round(hearts, 1);
                                    }
                                    double percent = finalDamage / currentHealth;
                                    String healthStr = finalDamage >= currentHealth ? "§4" : (percent >= 0.7 ? "§c" : (percent >= 0.5 ? "§6" : (percent >= 0.3 ? "§e" : "§a")));
                                    this.damageText = healthStr + Utils.asWholeNum(hearts);
                                    if (Settings.showHeartSymbol.isToggled()) {
                                        this.damageText = this.damageText + "§c❤§r";
                                    }
                                    this.showDamageText = true;
                                }
                                if (this.showDistance.isToggled()) {
                                    this.distanceText = Utils.asWholeNum(Utils.round(fallDistance, 2)) + "m";
                                    this.distanceTextColor = this.getDistanceColor(fallDistance).getRGB();
                                    this.showDistanceText = true;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onPlayerTeleport(PlayerTeleportEvent e) {
        this.recalculateAfterTeleport = true;
        this.cachedFallDistance = 0.0F;
        this.clearOverlayState();
    }

    @SubscribeEvent
    public void onRenderTick(RenderTickEvent ev) {
        if (ev.phase == Phase.END && mc.field_71462_r == null && Utils.nullCheck()) {
            if (this.showDamageText || this.showDistanceText) {
                ScaledResolution scaledResolution = new ScaledResolution(mc);
                if (this.showDamageText && this.damageText != null) {
                    mc.field_71466_p.func_175063_a(this.damageText, scaledResolution.func_78326_a() / 2 - mc.field_71466_p.func_78256_a(this.damageText) / 2, scaledResolution.func_78328_b() / 2 - 15, Color.WHITE.getRGB());
                }
                if (this.showDistanceText && this.distanceText != null) {
                    mc.field_71466_p.func_175063_a(this.distanceText, scaledResolution.func_78326_a() / 2 - mc.field_71466_p.func_78256_a(this.distanceText) / 2, scaledResolution.func_78328_b() / 2 + 6, this.distanceTextColor);
                }
            }
        }
    }

    private float calculateFallDistance() {
        if (this.fallStartY != -1.0 && this.groundY != -1.0) {
            if (this.cachedFallDistance == 0.0F) {
                this.cachedFallDistance = (float) Math.max(0.0, this.fallStartY - this.groundY);
            }
            return this.cachedFallDistance;
        } else {
            double currentY = mc.field_71439_g.field_70163_u;
            double ground = this.findGroundY(mc.field_71439_g.field_70165_t, mc.field_71439_g.field_70161_v);
            return ground == -1.0 ? 0.0F : (float) Math.max(0.0, currentY - ground);
        }
    }

    private double findGroundY(double x, double z) {
        int startY = (int) Math.floor(mc.field_71439_g.field_70163_u);
        for (int y = startY; y > -1; y--) {
            BlockPos pos = new BlockPos(Math.floor(x), y, Math.floor(z));
            if (mc.field_71441_e.func_180495_p(pos).func_177230_c().func_149688_o() == Material.field_151586_h) {
                return -1.0;
            }
            if (!Utils.isPlaceable(pos)) {
                return y + 1;
            }
        }
        return -1.0;
    }

    private Color getDistanceColor(float distance) {
        float minDistance = 2.5F;
        float maxDistance = 20.0F;
        float normalized = MathHelper.func_76131_a((distance - minDistance) / (maxDistance - minDistance), 0.0F, 1.0F);
        int red = 255;
        int green = (int) (255.0F * (1.0F - normalized));
        int blue = 0;
        return new Color(red, green, blue);
    }

    private void clearOverlayState() {
        this.damageText = null;
        this.showDamageText = false;
        this.distanceText = null;
        this.distanceTextColor = Color.WHITE.getRGB();
        this.showDistanceText = false;
    }

    private void recalculateFallTracking() {
        this.cachedFallDistance = 0.0F;
        this.damageCache.clear();
        this.clearOverlayState();
        if (mc.field_71439_g.field_70122_E) {
            this.fallStartY = -1.0;
            this.groundY = -1.0;
        } else {
            this.fallStartY = mc.field_71439_g.field_70163_u;
            this.groundY = this.findGroundY(mc.field_71439_g.field_70165_t, mc.field_71439_g.field_70161_v);
        }
    }

    private static class DamageCacheKey {

        final int armorHash;

        final int fallDistanceInt;

        final int jumpBoostLevel;

        final int resistanceLevel;

        DamageCacheKey(int armorHash, float fallDistance, int jumpBoost, int resistance) {
            this.armorHash = armorHash;
            this.fallDistanceInt = Math.round(fallDistance * 100.0F);
            this.jumpBoostLevel = jumpBoost;
            this.resistanceLevel = resistance;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            } else if (o != null && this.getClass() == o.getClass()) {
                FallView.DamageCacheKey that = (FallView.DamageCacheKey) o;
                return this.armorHash == that.armorHash && this.fallDistanceInt == that.fallDistanceInt && this.jumpBoostLevel == that.jumpBoostLevel && this.resistanceLevel == that.resistanceLevel;
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return this.armorHash * 31 * 31 * 31 + this.fallDistanceInt * 31 * 31 + this.jumpBoostLevel * 31 + this.resistanceLevel;
        }
    }
}
