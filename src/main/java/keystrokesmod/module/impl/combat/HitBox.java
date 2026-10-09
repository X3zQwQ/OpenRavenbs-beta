package keystrokesmod.module.impl.combat;

import java.awt.Color;
import java.util.List;
import keystrokesmod.mixin.impl.accessor.IAccessorMinecraft;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.Utils;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

public class HitBox extends Module {

    public static SliderSetting multiplier;

    public ButtonSetting showHitbox;

    public ButtonSetting playersOnly;

    public ButtonSetting weaponOnly;

    private Entity pointedEntity;

    private MovingObjectPosition mv;

    public HitBox() {
        super("Hitboxes", Module.category.combat, 0);
        this.registerSetting(multiplier = new SliderSetting("Multiplier", "x", 1.2, 1.0, 5.0, 0.05));
        this.registerSetting(this.playersOnly = new ButtonSetting("Players only", true));
        this.registerSetting(this.showHitbox = new ButtonSetting("Show new hitbox", false));
        this.registerSetting(this.weaponOnly = new ButtonSetting("Weapon only", false));
        this.closetModule = true;
    }

    @Override
    public String getInfo() {
        return ((int) multiplier.getInput() == multiplier.getInput() ? (int) multiplier.getInput() + "" : multiplier.getInput()) + multiplier.getSuffix();
    }

    @SubscribeEvent
    public void onMouse(MouseEvent e) {
        if (e.button == 0 && e.buttonstate && Utils.nullCheck() && multiplier.getInput() != 1.0 && !mc.field_71439_g.func_70632_aY() && mc.field_71462_r == null) {
            if (!this.weaponOnly.isToggled() || Utils.holdingWeapon()) {
                Entity c = this.getEntity(1.0F);
                if (c != null) {
                    if (c instanceof EntityPlayer) {
                        if (Utils.isFriended((EntityPlayer) c)) {
                            return;
                        }
                    } else if (this.playersOnly.isToggled()) {
                        return;
                    }
                    mc.field_71476_x = this.mv;
                }
            }
        }
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent e) {
        if (this.showHitbox.isToggled() && Utils.nullCheck()) {
            for (Entity en : mc.field_71441_e.field_72996_f) {
                if (en != mc.field_71439_g && en instanceof EntityLivingBase && ((EntityLivingBase) en).field_70725_aQ == 0 && !(en instanceof EntityArmorStand) && !en.func_82150_aj()) {
                    this.rh(en, Color.WHITE);
                }
            }
        }
    }

    public static double getExpand(Entity en) {
        return multiplier.getInput();
    }

    public Entity getEntity(float partialTicks) {
        if (mc.func_175606_aa() != null && mc.field_71441_e != null) {
            mc.field_147125_j = null;
            this.pointedEntity = null;
            double d0 = mc.field_71442_b.func_78749_i() ? 6.0 : (ModuleManager.reach.isEnabled() ? Utils.getRandomValue(Reach.min, Reach.max, Utils.getRandom()) : 3.0);
            this.mv = mc.func_175606_aa().func_174822_a(d0, partialTicks);
            double d2 = d0;
            Vec3 vec3 = mc.func_175606_aa().func_174824_e(partialTicks);
            if (this.mv != null) {
                d2 = this.mv.field_72307_f.func_72438_d(vec3);
            }
            Vec3 vec4 = mc.func_175606_aa().func_70676_i(partialTicks);
            Vec3 vec5 = vec3.func_72441_c(vec4.field_72450_a * d0, vec4.field_72448_b * d0, vec4.field_72449_c * d0);
            Vec3 vec6 = null;
            float f1 = 1.0F;
            List list = mc.field_71441_e.func_72839_b(mc.func_175606_aa(), mc.func_175606_aa().func_174813_aQ().func_72321_a(vec4.field_72450_a * d0, vec4.field_72448_b * d0, vec4.field_72449_c * d0).func_72314_b(f1, f1, f1));
            double d3 = d2;
            for (Object o : list) {
                Entity entity = (Entity) o;
                if (entity.func_70067_L()) {
                    float ex = (float) (entity.func_70111_Y() * getExpand(entity));
                    AxisAlignedBB ax = entity.func_174813_aQ().func_72314_b(ex, ex, ex);
                    MovingObjectPosition mop = ax.func_72327_a(vec3, vec5);
                    if (ax.func_72318_a(vec3)) {
                        if (0.0 < d3 || d3 == 0.0) {
                            this.pointedEntity = entity;
                            vec6 = mop == null ? vec3 : mop.field_72307_f;
                            d3 = 0.0;
                        }
                    } else if (mop != null) {
                        double d4 = vec3.func_72438_d(mop.field_72307_f);
                        if (d4 < d3 || d3 == 0.0) {
                            if (entity != mc.func_175606_aa().field_70154_o || entity.canRiderInteract()) {
                                this.pointedEntity = entity;
                                vec6 = mop.field_72307_f;
                                d3 = d4;
                            } else if (d3 == 0.0) {
                                this.pointedEntity = entity;
                                vec6 = mop.field_72307_f;
                            }
                        }
                    }
                }
            }
            if (this.pointedEntity != null && (d3 < d2 || this.mv == null)) {
                this.mv = new MovingObjectPosition(this.pointedEntity, vec6);
                if (this.pointedEntity instanceof EntityLivingBase || this.pointedEntity instanceof EntityItemFrame) {
                    return this.pointedEntity;
                }
            }
        }
        return null;
    }

    private void rh(Entity e, Color c) {
        if (e instanceof EntityLivingBase) {
            float partialTicks = ((IAccessorMinecraft) mc).getTimer().field_74281_c;
            double x = e.field_70142_S + (e.field_70165_t - e.field_70142_S) * partialTicks - mc.func_175598_ae().field_78730_l;
            double y = e.field_70137_T + (e.field_70163_u - e.field_70137_T) * partialTicks - mc.func_175598_ae().field_78731_m;
            double z = e.field_70136_U + (e.field_70161_v - e.field_70136_U) * partialTicks - mc.func_175598_ae().field_78728_n;
            float ex = (float) (e.func_70111_Y() * multiplier.getInput());
            AxisAlignedBB axisAlignedBB = e.func_174813_aQ().func_72314_b(ex, ex, ex);
            AxisAlignedBB axis = new AxisAlignedBB(axisAlignedBB.field_72340_a - e.field_70165_t + x, axisAlignedBB.field_72338_b - e.field_70163_u + y, axisAlignedBB.field_72339_c - e.field_70161_v + z, axisAlignedBB.field_72336_d - e.field_70165_t + x, axisAlignedBB.field_72337_e - e.field_70163_u + y, axisAlignedBB.field_72334_f - e.field_70161_v + z);
            GL11.glBlendFunc(770, 771);
            GL11.glEnable(3042);
            GL11.glDisable(3553);
            GL11.glDisable(2929);
            GL11.glDepthMask(false);
            GL11.glLineWidth(2.0F);
            GL11.glColor3d(c.getRed(), c.getGreen(), c.getBlue());
            RenderGlobal.func_181561_a(axis);
            GL11.glEnable(3553);
            GL11.glEnable(2929);
            GL11.glDepthMask(true);
            GL11.glDisable(3042);
        }
    }
}
