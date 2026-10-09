package keystrokesmod.module.impl.combat;

import java.util.List;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.RotationUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.init.Blocks;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class Reach extends Module {

    public static SliderSetting min;

    public static SliderSetting max;

    public static ButtonSetting weaponOnly;

    public static ButtonSetting movingOnly;

    public static ButtonSetting sprintOnly;

    public static ButtonSetting hitThroughBlocks;

    public Reach() {
        super("Reach", Module.category.combat, 0);
        this.liteModule = true;
        this.registerSetting(min = new SliderSetting("Min", 3.1, 3.0, 6.0, 0.05));
        this.registerSetting(max = new SliderSetting("Max", 3.3, 3.0, 6.0, 0.05));
        this.registerSetting(weaponOnly = new ButtonSetting("Weapon only", false));
        this.registerSetting(movingOnly = new ButtonSetting("Moving only", false));
        this.registerSetting(sprintOnly = new ButtonSetting("Sprint only", false));
        this.registerSetting(hitThroughBlocks = new ButtonSetting("Hit through blocks", false));
        this.closetModule = true;
    }

    @Override
    public void guiUpdate() {
        Utils.correctValue(min, max);
    }

    @SubscribeEvent
    public void onMouse(MouseEvent ev) {
        if (ev.button >= 0 && ev.buttonstate && Utils.nullCheck()) {
            call();
        }
    }

    public static boolean call() {
        if (!Utils.nullCheck()) {
            return false;
        } else if (weaponOnly.isToggled() && !Utils.holdingWeapon()) {
            return false;
        } else if (movingOnly.isToggled() && mc.field_71439_g.field_70701_bs == 0.0 && mc.field_71439_g.field_70702_br == 0.0) {
            return false;
        } else if (sprintOnly.isToggled() && !mc.field_71439_g.func_70051_ag()) {
            return false;
        } else {
            if (!hitThroughBlocks.isToggled() && mc.field_71476_x != null) {
                BlockPos p = mc.field_71476_x.func_178782_a();
                if (p != null && mc.field_71441_e.func_180495_p(p).func_177230_c() != Blocks.field_150350_a) {
                    return false;
                }
            }
            double reach = Utils.getRandomValue(min, max, Utils.getRandom());
            Object[] entity = getEntity(reach, 0.0);
            if (entity == null) {
                return false;
            } else {
                Entity en = (Entity) entity[0];
                mc.field_71476_x = new MovingObjectPosition(en, (Vec3) entity[1]);
                mc.field_147125_j = en;
                return true;
            }
        }
    }

    private static Object[] getEntity(double reach, double expand) {
        if (!ModuleManager.reach.isEnabled()) {
            reach = mc.field_71442_b.func_78749_i() ? 6.0 : 3.0;
        }
        return getEntity(reach, expand, null);
    }

    public static Object[] getEntity(double reach, double expand, float[] rotations) {
        Entity zz2 = mc.func_175606_aa();
        Entity entity = null;
        if (zz2 == null) {
            return null;
        } else {
            Vec3 zz3 = zz2.func_174824_e(1.0F);
            Vec3 zz4;
            if (rotations != null) {
                zz4 = RotationUtils.getVectorForRotation(rotations[1], rotations[0]);
            } else {
                zz4 = zz2.func_70676_i(1.0F);
            }
            Vec3 zz5 = zz3.func_72441_c(zz4.field_72450_a * reach, zz4.field_72448_b * reach, zz4.field_72449_c * reach);
            Vec3 hitVec = null;
            List zz8 = mc.field_71441_e.func_72839_b(zz2, zz2.func_174813_aQ().func_72321_a(zz4.field_72450_a * reach, zz4.field_72448_b * reach, zz4.field_72449_c * reach).func_72314_b(1.0, 1.0, 1.0));
            double zz9 = reach;
            for (int zz10 = 0; zz10 < zz8.size(); zz10++) {
                Entity zz11 = (Entity) zz8.get(zz10);
                if (zz11.func_70067_L()) {
                    float ex = (float) (zz11.func_70111_Y() * HitBox.getExpand(zz11));
                    AxisAlignedBB zz13 = zz11.func_174813_aQ().func_72314_b(ex, ex, ex);
                    zz13 = zz13.func_72314_b(expand, expand, expand);
                    MovingObjectPosition zz14 = zz13.func_72327_a(zz3, zz5);
                    if (zz13.func_72318_a(zz3)) {
                        if (0.0 < zz9 || zz9 == 0.0) {
                            entity = zz11;
                            hitVec = zz14 == null ? zz3 : zz14.field_72307_f;
                            zz9 = 0.0;
                        }
                    } else if (zz14 != null) {
                        double zz15 = zz3.func_72438_d(zz14.field_72307_f);
                        if (zz15 < zz9 || zz9 == 0.0) {
                            if (zz11 == zz2.field_70154_o) {
                                if (zz9 == 0.0) {
                                    entity = zz11;
                                    hitVec = zz14.field_72307_f;
                                }
                            } else {
                                entity = zz11;
                                hitVec = zz14.field_72307_f;
                                zz9 = zz15;
                            }
                        }
                    }
                }
            }
            if (zz9 < reach && !(entity instanceof EntityLivingBase) && !(entity instanceof EntityItemFrame)) {
                entity = null;
            }
            return entity != null && hitVec != null ? new Object[] { entity, hitVec } : null;
        }
    }
}
