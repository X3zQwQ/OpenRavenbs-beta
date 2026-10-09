package keystrokesmod.module.impl.minigames;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import keystrokesmod.mixin.impl.accessor.IAccessorMinecraft;
import keystrokesmod.module.Module;
import keystrokesmod.module.impl.world.AntiBot;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.ItemListSetting;
import keystrokesmod.utility.ItemSearchIndex;
import keystrokesmod.utility.RenderUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

public class MurderMystery extends Module {

    private final ItemListSetting murderWeapons;

    private final ButtonSetting alert;

    private final ButtonSetting highlightMurderer;

    private final ButtonSetting highlightBow;

    private final ButtonSetting highlightInnocent;

    private final ButtonSetting highlightDead;

    private final ButtonSetting goldEsp;

    private final List<EntityPlayer> murderers = new ArrayList<>();

    private final List<EntityPlayer> hasBow = new ArrayList<>();

    private boolean override;

    public MurderMystery() {
        super("Murder Mystery", Module.category.minigames);
        this.registerSetting(this.murderWeapons = new ItemListSetting("Murder weapons"));
        this.addDefaultMurderWeapon(Items.field_151040_l);
        this.addDefaultMurderWeapon(Items.field_151052_q);
        this.addDefaultMurderWeapon(Items.field_151037_a);
        this.addDefaultMurderWeapon(Items.field_151055_y);
        this.addDefaultMurderWeapon(Items.field_151053_p);
        this.addDefaultMurderWeapon(Items.field_151041_m);
        this.addDefaultMurderWeapon(Items.field_151051_r);
        this.addDefaultMurderWeapon(Items.field_151072_bj);
        this.addDefaultMurderWeapon(Items.field_151047_v);
        this.addDefaultMurderWeapon(Items.field_151128_bU);
        this.addDefaultMurderWeapon(Items.field_151158_bO);
        this.addDefaultMurderWeapon(Items.field_151005_D);
        this.addDefaultMurderWeapon(Items.field_151034_e);
        this.addDefaultMurderWeapon(Items.field_151057_cb);
        this.addDefaultMurderWeapon(Items.field_151146_bM);
        this.addDefaultMurderWeapon(Items.field_151103_aS);
        this.addDefaultMurderWeapon(Items.field_151172_bF);
        this.addDefaultMurderWeapon(Items.field_151150_bK);
        this.addDefaultMurderWeapon(Items.field_151106_aX);
        this.addDefaultMurderWeapon(Items.field_151056_x);
        this.addDefaultMurderWeapon(Items.field_179562_cC);
        this.addDefaultMurderWeapon(Items.field_151083_be);
        this.addDefaultMurderWeapon(Items.field_151130_bT);
        this.addDefaultMurderWeapon(Items.field_151077_bg);
        this.addDefaultMurderWeapon(Items.field_151010_B);
        this.addDefaultMurderWeapon(Items.field_151048_u);
        this.addDefaultMurderWeapon(Items.field_151012_L);
        this.addDefaultMurderWeapon(Items.field_151097_aZ);
        this.addDefaultMurderWeapon(Items.field_151115_aP);
        this.addDefaultMurderWeapon(Items.field_151100_aR);
        this.addDefaultMurderWeapon(Items.field_151124_az);
        this.addDefaultMurderWeapon(Items.field_151060_bw);
        this.addDefaultMurderWeapon(Items.field_151122_aG);
        this.addDefaultMurderWeapon(Item.func_150898_a(Blocks.field_150398_cm));
        this.addDefaultMurderWeapon(Item.func_150898_a(Blocks.field_150360_v));
        this.addDefaultMurderWeapon(Item.func_150898_a(Blocks.field_150330_I));
        this.registerSetting(this.alert = new ButtonSetting("Alert murderer", true));
        this.registerSetting(this.highlightBow = new ButtonSetting("Highlight bow", true));
        this.registerSetting(this.highlightDead = new ButtonSetting("Highlight dead", true));
        this.registerSetting(this.goldEsp = new ButtonSetting("Gold ingots", true));
        this.registerSetting(this.highlightInnocent = new ButtonSetting("Highlight innocent", true));
        this.registerSetting(this.highlightMurderer = new ButtonSetting("Highlight murderer", true));
    }

    @Override
    public void onDisable() {
        this.clear();
    }

    @SubscribeEvent
    public void onRenderWordLast(RenderWorldLastEvent e) {
        if (Utils.nullCheck()) {
            if (!this.isMurderMystery()) {
                this.clear();
            } else {
                this.override = false;
                for (EntityPlayer en : mc.field_71441_e.field_73010_i) {
                    if (en != mc.field_71439_g && !en.func_82150_aj() && (!AntiBot.isBot(en) || this.highlightDead.isToggled())) {
                        if (en.func_70694_bm() != null) {
                            ItemStack heldStack = en.func_70694_bm();
                            Item heldItem = heldStack.func_77973_b();
                            if (this.murderWeapons.matches(heldStack)) {
                                if (!this.murderers.contains(en)) {
                                    this.murderers.add(en);
                                    if (this.alert.isToggled()) {
                                        mc.field_71439_g.func_85030_a("note.pling", 1.0F, 1.0F);
                                        Utils.sendMessage("&b" + en.func_70005_c_() + " &7is the &cmurderer&7! " + Utils.getDistanceColor(mc.field_71439_g.func_70032_d(en)) + (int) mc.field_71439_g.func_70032_d(en) + "m&r");
                                    }
                                }
                            } else if (heldItem instanceof ItemBow && this.highlightBow.isToggled() && !this.hasBow.contains(en)) {
                                this.hasBow.add(en);
                            }
                        }
                        this.override = true;
                        int rgb = Color.green.getRGB();
                        if (this.murderers.contains(en) && this.highlightMurderer.isToggled()) {
                            rgb = Color.red.getRGB();
                        } else if (this.hasBow.contains(en) && this.highlightBow.isToggled()) {
                            rgb = Color.orange.getRGB();
                        } else if (!this.highlightInnocent.isToggled()) {
                            continue;
                        }
                        if (this.highlightDead.isToggled() || !(this.getBoundingBoxVolume(en) <= 0.009)) {
                            RenderUtils.renderEntity(en, 2, 0.0, 0.0, rgb, false);
                        }
                    }
                }
                if (!this.goldEsp.isToggled()) {
                    return;
                }
                float renderPartialTicks = ((IAccessorMinecraft) mc).getTimer().field_74281_c;
                int n4 = -331703;
                for (Entity entity : mc.field_71441_e.field_72996_f) {
                    if (entity instanceof EntityItem && entity.field_70173_aa >= 3) {
                        EntityItem entityItem = (EntityItem) entity;
                        if (entityItem.func_92059_d().field_77994_a != 0) {
                            Item getItem = entityItem.func_92059_d().func_77973_b();
                            if (getItem != null && getItem == Items.field_151043_k) {
                                double n5 = entity.field_70142_S + (entity.field_70165_t - entity.field_70142_S) * renderPartialTicks;
                                double n6 = entity.field_70137_T + (entity.field_70163_u - entity.field_70137_T) * renderPartialTicks;
                                double n7 = entity.field_70136_U + (entity.field_70161_v - entity.field_70136_U) * renderPartialTicks;
                                double n8 = mc.field_71439_g.field_70142_S + (mc.field_71439_g.field_70165_t - mc.field_71439_g.field_70142_S) * renderPartialTicks - n5;
                                double n9 = mc.field_71439_g.field_70137_T + (mc.field_71439_g.field_70163_u - mc.field_71439_g.field_70137_T) * renderPartialTicks - n6;
                                double n10 = mc.field_71439_g.field_70136_U + (mc.field_71439_g.field_70161_v - mc.field_71439_g.field_70136_U) * renderPartialTicks - n7;
                                GlStateManager.func_179094_E();
                                this.drawBox(n4, n5, n6, n7, MathHelper.func_76133_a(n8 * n8 + n9 * n9 + n10 * n10));
                                GlStateManager.func_179121_F();
                            }
                        }
                    }
                }
            }
        }
    }

    public void drawBox(int n, double n4, double n5, double n6, double n7) {
        n4 -= mc.func_175598_ae().field_78730_l;
        n5 -= mc.func_175598_ae().field_78731_m;
        n6 -= mc.func_175598_ae().field_78728_n;
        GL11.glPushMatrix();
        GL11.glBlendFunc(770, 771);
        GL11.glEnable(3042);
        GL11.glLineWidth(2.0F);
        GL11.glDisable(3553);
        GL11.glDisable(2929);
        GL11.glDepthMask(false);
        float n8 = (n >> 16 & 0xFF) / 255.0F;
        float n9 = (n >> 8 & 0xFF) / 255.0F;
        float n10 = (n & 0xFF) / 255.0F;
        float min = Math.min(Math.max(0.2F, (float) (0.01F * n7)), 0.4F);
        RenderUtils.drawBoundingBox(new AxisAlignedBB(n4 - min, n5, n6 - min, n4 + min, n5 + min * 2.0F, n6 + min), n8, n9, n10, 0.35F);
        GL11.glEnable(3553);
        GL11.glEnable(2929);
        GL11.glDepthMask(true);
        GL11.glDisable(3042);
        GL11.glPopMatrix();
    }

    private boolean isMurderMystery() {
        if (Utils.isHypixel()) {
            if (mc.field_71439_g.func_96123_co() == null || mc.field_71439_g.func_96123_co().func_96539_a(1) == null) {
                return false;
            }
            String d = mc.field_71439_g.func_96123_co().func_96539_a(1).func_96678_d();
            if (!d.contains("MURDER") && !d.contains("MYSTERY")) {
                return false;
            }
            for (String l : Utils.getScoreBoardOld()) {
                String s = Utils.stripColor(l);
                if (s.contains("Role:") || s.contains("Innocents Left:")) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isEmpty() {
        return this.murderers.isEmpty() && this.hasBow.isEmpty() && !this.override;
    }

    private void clear() {
        this.override = false;
        this.murderers.clear();
        this.hasBow.clear();
    }

    private double getBoundingBoxVolume(Entity entity) {
        AxisAlignedBB axisAlignedBB = entity.func_174813_aQ();
        if (axisAlignedBB == null) {
            return 0.0;
        } else {
            double length = axisAlignedBB.field_72336_d - axisAlignedBB.field_72340_a;
            double width = axisAlignedBB.field_72334_f - axisAlignedBB.field_72339_c;
            double height = axisAlignedBB.field_72337_e - axisAlignedBB.field_72338_b;
            return length * width * height;
        }
    }

    private void addDefaultMurderWeapon(Item item) {
        if (item != null) {
            String registryId = ItemSearchIndex.getRegistryId(item);
            if (registryId != null) {
                this.murderWeapons.addItem(ItemSearchIndex.hasMultipleVariants(item) ? registryId + ":*" : registryId);
            }
        }
    }
}
