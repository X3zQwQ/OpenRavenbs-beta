package keystrokesmod.module.impl.combat;

import com.google.common.base.Predicates;
import keystrokesmod.module.Module;
import keystrokesmod.module.impl.world.AntiBot;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.Utils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EntitySelectors;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

public class Piercing extends Module {

    private SliderSetting sortMode;

    private ButtonSetting ignoreBlocks;

    private ButtonSetting ignoreTeammates;

    private ButtonSetting ignoreNonPlayer;

    private ButtonSetting weaponOnly;

    private ButtonSetting insideHitboxOnly;

    private int lastMouseOverTick = -1;

    private String[] sortModes = new String[] { "Hurt time", "Health" };

    public Piercing() {
        super("Piercing", Module.category.combat);
        this.registerSetting(this.sortMode = new SliderSetting("Sort mode", 0, this.sortModes));
        this.registerSetting(this.ignoreBlocks = new ButtonSetting("Ignore blocks", false));
        this.registerSetting(this.ignoreNonPlayer = new ButtonSetting("Ignore non-players", true));
        this.registerSetting(this.ignoreTeammates = new ButtonSetting("Ignore teammates", true));
        this.registerSetting(this.weaponOnly = new ButtonSetting("Weapon only", false));
        this.registerSetting(this.insideHitboxOnly = new ButtonSetting("Inside hitbox only", false));
    }

    @Override
    public String getInfo() {
        return this.sortModes[(int) this.sortMode.getInput()];
    }

    public boolean shouldOverrideMouseOver() {
        if (!this.isEnabled()) {
            return false;
        } else if (mc == null || mc.field_71439_g == null || mc.field_71441_e == null) {
            return false;
        } else {
            return this.weaponOnly.isToggled() && !Utils.holdingWeapon() ? false : this.ignoreBlocks.isToggled() || mc.field_71476_x == null || mc.field_71476_x.field_72313_a != MovingObjectType.BLOCK;
        }
    }

    public void modifyMouseOverFromGetMouseOver(float partialTicks) {
        if (this.shouldOverrideMouseOver()) {
            this.keystrokesmod$modifyMouseOverVanillaLook(partialTicks);
        }
    }

    private void keystrokesmod$modifyMouseOverVanillaLook(float partialTicks) {
        Entity viewEntity = mc.func_175606_aa();
        if (viewEntity != null && mc.field_71441_e != null) {
            double reach = mc.field_71442_b.func_78757_d();
            Vec3 eyes = viewEntity.func_174824_e(partialTicks);
            if (mc.field_71442_b.func_78749_i()) {
                reach = 6.0;
            }
            Vec3 look = viewEntity.func_70676_i(partialTicks);
            Vec3 rayEnd = eyes.func_72441_c(look.field_72450_a * reach, look.field_72448_b * reach, look.field_72449_c * reach);
            Entity best = null;
            Vec3 bestHit = null;
            double bestDist = Double.MAX_VALUE;
            boolean bestLiving = false;
            int bestHurt = Integer.MAX_VALUE;
            float bestHp = Float.POSITIVE_INFINITY;
            int modeSel = (int) this.sortMode.getInput();
            for (Entity e : mc.field_71441_e.func_175674_a(viewEntity, viewEntity.func_174813_aQ().func_72321_a(look.field_72450_a * reach, look.field_72448_b * reach, look.field_72449_c * reach).func_72314_b(1.0, 1.0, 1.0), Predicates.and(EntitySelectors.field_180132_d, Entity::func_70067_L))) {
                if ((!this.ignoreNonPlayer.isToggled() || e instanceof EntityPlayer) && (!this.ignoreTeammates.isToggled() || !Utils.isTeammate(e)) && !AntiBot.isBot(e) && (!(e instanceof EntityPlayer) || !Utils.isFriended((EntityPlayer) e))) {
                    float cb = e.func_70111_Y();
                    AxisAlignedBB bb = e.func_174813_aQ().func_72314_b(cb, cb, cb);
                    MovingObjectPosition hit = bb.func_72327_a(eyes, rayEnd);
                    boolean inside = bb.func_72318_a(eyes);
                    if (inside || hit != null) {
                        double dist = inside ? 0.0 : eyes.func_72438_d(hit.field_72307_f);
                        if ((mc.field_71442_b.func_78749_i() || !(dist > 3.0)) && !(dist > reach) && !(dist >= bestDist) && (!this.insideHitboxOnly.isToggled() || !(dist > 0.1F)) && (e != viewEntity.field_70154_o || viewEntity.canRiderInteract() || best == null)) {
                            boolean living = e instanceof EntityLivingBase;
                            int hurt = living ? ((EntityLivingBase) e).field_70737_aN : Integer.MAX_VALUE;
                            float hp = living ? ((EntityLivingBase) e).func_110143_aJ() : Float.POSITIVE_INFINITY;
                            boolean take = false;
                            if (best == null) {
                                take = true;
                            } else if (living && !bestLiving) {
                                take = true;
                            } else if (living == bestLiving) {
                                if (!living) {
                                    take = dist < bestDist;
                                } else if (modeSel == 0) {
                                    if (hurt < bestHurt) {
                                        take = true;
                                    } else if (hurt == bestHurt && dist < bestDist) {
                                        take = true;
                                    }
                                } else if (hp < bestHp) {
                                    take = true;
                                } else if (hp == bestHp && dist < bestDist) {
                                    take = true;
                                }
                            }
                            if (take) {
                                best = e;
                                bestHit = inside ? (hit == null ? eyes : hit.field_72307_f) : hit.field_72307_f;
                                bestDist = dist;
                                bestLiving = living;
                                bestHurt = hurt;
                                bestHp = hp;
                            }
                        }
                    }
                }
            }
            if (best != null && reach > 3.0 && bestDist > 3.0 && !mc.field_71442_b.func_78749_i()) {
                mc.field_71476_x = new MovingObjectPosition(MovingObjectType.MISS, bestHit, null, new BlockPos(bestHit));
            } else {
                if (best != null) {
                    mc.field_71476_x = new MovingObjectPosition(best, bestHit);
                    if (best instanceof EntityLivingBase || best instanceof EntityItemFrame) {
                        mc.field_147125_j = best;
                    }
                }
            }
        }
    }
}
