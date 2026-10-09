package keystrokesmod.module.impl.network;

import keystrokesmod.Raven;
import keystrokesmod.event.AttackEvent;
import keystrokesmod.event.GameTickEvent;
import keystrokesmod.event.PrePlayerInteractEvent;
import keystrokesmod.lag.api.EnumLagDirection;
import keystrokesmod.lag.api.LagRequest;
import keystrokesmod.lag.timeout.ModuleBackedTimeout;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.combat.Autoblock;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.ColorSetting;
import keystrokesmod.module.setting.impl.DescriptionSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.CombatTargeting;
import keystrokesmod.utility.RenderUtils;
import keystrokesmod.utility.RotationUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

public class LagRange extends Module {

    private static final double MINIMUM_DISTANCE_SQ = 9.0;

    private static final long INDICATOR_INTERP_MS = 80L;

    private static final double POS_EPS = 1.0E-6;

    private final SliderSetting range;

    private final SliderSetting maximumDelay;

    private final ButtonSetting ignoreTeammates;

    private final ButtonSetting sprintReset;

    private final ButtonSetting blockSword;

    private final ButtonSetting usedSplashPotion;

    private final ButtonSetting holdingWeapon;

    private final ButtonSetting realPositionIndicator;

    private final ButtonSetting showInFirstPerson;

    private final ColorSetting indicatorColor;

    private final SliderSetting indicatorLineWidth;

    private final ButtonSetting indicatorFilled;

    private EntityPlayer currentTarget;

    private double lastDistSq = -1.0;

    private boolean isLagging;

    private int lastSelfHurtTime;

    private int lastTargetHurtTime;

    private int hitMarkedEntityId;

    private boolean lastSprintState;

    private boolean lastBlockingState;

    private LagRequest outboundLag;

    private Vec3 indicatorInterpFrom;

    private Vec3 indicatorInterpTo;

    private long indicatorInterpStartMs;

    public LagRange() {
        super("Lag Range", Module.category.network);
        this.liteModule = true;
        this.registerSetting(this.range = new SliderSetting("Range", 6.0, 3.0, 10.0, 0.1));
        this.registerSetting(this.maximumDelay = new SliderSetting("Maximum delay", "ms", 200.0, 50.0, 1000.0, 10.0));
        this.registerSetting(this.ignoreTeammates = new ButtonSetting("Ignore teammates", true));
        this.registerSetting(new DescriptionSetting("Flush conditions"));
        this.registerSetting(this.sprintReset = new ButtonSetting("Sprint reset", true));
        this.registerSetting(this.blockSword = new ButtonSetting("Block sword", true));
        this.registerSetting(this.usedSplashPotion = new ButtonSetting("Used splash potion", true));
        this.registerSetting(new DescriptionSetting("Indicator"));
        this.registerSetting(this.realPositionIndicator = new ButtonSetting("Real position indicator", true));
        this.registerSetting(this.showInFirstPerson = new ButtonSetting("Show in first person", false));
        this.registerSetting(this.indicatorColor = new ColorSetting("Indicator color", 255, 0, 0, 100));
        this.registerSetting(this.indicatorLineWidth = new SliderSetting("Indicator line width", 2.0, 1.0, 5.0, 0.5));
        this.registerSetting(this.indicatorFilled = new ButtonSetting("Indicator filled", false));
        this.registerSetting(new DescriptionSetting("Conditions"));
        this.registerSetting(this.holdingWeapon = new ButtonSetting("Holding a weapon", true));
        this.closetModule = true;
    }

    @Override
    public void onEnable() {
        this.resetState();
    }

    @Override
    public void onDisable() {
        this.flushLag();
        this.resetState();
    }

    @Override
    public String getInfo() {
        return (int) this.maximumDelay.getInput() + "ms";
    }

    public Vec3 getLagRangePosition() {
        return !this.isLagging ? null : Raven.lagHandler.getLastReleasedServerPosition();
    }

    public EntityPlayer getLagRangeTarget() {
        return this.isLagging && this.currentTarget != null && !this.currentTarget.field_70128_L ? this.currentTarget : null;
    }

    @SubscribeEvent
    public void onPrePlayerInteract(PrePlayerInteractEvent e) {
        if (Utils.nullCheck() && !mc.field_71439_g.field_70128_L && mc.field_71441_e != null) {
            if (ModuleManager.bedAura != null && ModuleManager.bedAura.isActivelyMining()) {
                if (this.isLagging) {
                    this.flushLag();
                }
            } else {
                Autoblock autoblock = (Autoblock) ModuleManager.getModule(Autoblock.class);
                if (autoblock != null && autoblock.isActive()) {
                    if (this.isLagging) {
                        this.flushLag();
                    }
                } else {
                    double rangeSq = this.range.getInput() * this.range.getInput();
                    boolean moving = this.isMoving();
                    EntityPlayer nextTarget = CombatTargeting.findTarget(rangeSq, this.ignoreTeammates.isToggled());
                    if (!this.sameTarget(nextTarget)) {
                        if (this.isLagging) {
                            this.flushLag();
                        }
                        this.lastDistSq = -1.0;
                        this.hitMarkedEntityId = -1;
                        this.lastTargetHurtTime = nextTarget != null ? nextTarget.field_70737_aN : 0;
                    }
                    this.currentTarget = nextTarget;
                    if (this.currentTarget != null) {
                        double distSq = RotationUtils.distanceSqFromEyeToClosestOnAABB(this.currentTarget);
                        if (this.isLagging) {
                            if (distSq > rangeSq) {
                                this.flushLag();
                                this.lastDistSq = distSq;
                                this.hitMarkedEntityId = -1;
                                this.lastTargetHurtTime = this.currentTarget.field_70737_aN;
                                return;
                            }
                            if (this.lastDistSq >= 0.0 && distSq >= this.lastDistSq) {
                                boolean hitHold = this.hitMarkedEntityId == this.currentTarget.func_145782_y() && distSq <= 9.0 && mc.field_71439_g.field_70737_aN == 0;
                                if (!hitHold) {
                                    this.flushLag();
                                    this.lastDistSq = distSq;
                                    this.lastTargetHurtTime = this.currentTarget.field_70737_aN;
                                    return;
                                }
                            }
                            int hurtTime = mc.field_71439_g.field_70737_aN;
                            if (hurtTime > this.lastSelfHurtTime) {
                                this.flushLag();
                                this.hitMarkedEntityId = -1;
                                this.lastSelfHurtTime = hurtTime;
                                this.lastDistSq = distSq;
                                this.lastTargetHurtTime = this.currentTarget.field_70737_aN;
                                return;
                            }
                            this.lastSelfHurtTime = hurtTime;
                            Raven.lagHandler.releaseExpiredPackets(EnumLagDirection.OUTBOUND, (long) this.maximumDelay.getInput());
                            if (this.holdingWeapon.isToggled() && !Utils.holdingWeapon()) {
                                this.flushLag();
                                this.lastDistSq = distSq;
                                this.lastTargetHurtTime = this.currentTarget.field_70737_aN;
                                return;
                            }
                            if (this.sprintReset.isToggled()) {
                                boolean sprintingNow = mc.field_71439_g.func_70051_ag();
                                if (sprintingNow && !this.lastSprintState) {
                                    this.flushLag();
                                    this.lastSprintState = sprintingNow;
                                    this.lastDistSq = distSq;
                                    this.lastTargetHurtTime = this.currentTarget.field_70737_aN;
                                    return;
                                }
                                this.lastSprintState = sprintingNow;
                            }
                            if (this.blockSword.isToggled()) {
                                boolean blockingNow = mc.field_71439_g.func_70632_aY();
                                if (blockingNow && !this.lastBlockingState) {
                                    this.flushLag();
                                    this.lastBlockingState = blockingNow;
                                    this.lastDistSq = distSq;
                                    this.lastTargetHurtTime = this.currentTarget.field_70737_aN;
                                    return;
                                }
                                this.lastBlockingState = blockingNow;
                            }
                            if (this.usedSplashPotion.isToggled() && mc.field_71439_g.func_71039_bw()) {
                                ItemStack held = mc.field_71439_g.func_70694_bm();
                                if (held != null && held.func_77973_b() instanceof ItemPotion && ItemPotion.func_77831_g(held.func_77960_j())) {
                                    this.flushLag();
                                    this.lastDistSq = distSq;
                                    this.lastTargetHurtTime = this.currentTarget.field_70737_aN;
                                    return;
                                }
                            }
                            this.lastDistSq = distSq;
                            this.lastTargetHurtTime = this.currentTarget.field_70737_aN;
                            return;
                        }
                        int intValue = mc.field_71439_g.field_70737_aN;
                        if (intValue > this.lastSelfHurtTime) {
                            this.hitMarkedEntityId = -1;
                        }
                        this.lastSelfHurtTime = intValue;
                        this.lastSprintState = mc.field_71439_g.func_70051_ag();
                        this.lastBlockingState = mc.field_71439_g.func_70632_aY();
                        if (intValue == 0 && this.lastTargetHurtTime == 0 && this.currentTarget.field_70737_aN > 0) {
                            this.hitMarkedEntityId = this.currentTarget.func_145782_y();
                        }
                        this.lastTargetHurtTime = this.currentTarget.field_70737_aN;
                        boolean closing = this.lastDistSq >= 0.0 && distSq < this.lastDistSq;
                        boolean outsideMinDist = distSq > 9.0;
                        boolean weaponOk = !this.holdingWeapon.isToggled() || Utils.holdingWeapon();
                        boolean hitMarkedHere = this.hitMarkedEntityId == this.currentTarget.func_145782_y();
                        boolean hitStart = hitMarkedHere && distSq <= 9.0 && intValue == 0 && moving && weaponOk;
                        this.lastDistSq = distSq;
                        if (intValue == 0 && weaponOk && moving && (closing && outsideMinDist || hitStart)) {
                            this.startLag();
                        }
                    } else {
                        if (this.isLagging) {
                            this.flushLag();
                        }
                        this.lastDistSq = -1.0;
                        this.hitMarkedEntityId = -1;
                        this.lastTargetHurtTime = 0;
                    }
                }
            }
        } else {
            if (this.isLagging) {
                this.flushLag();
            }
            this.resetState();
        }
    }

    @SubscribeEvent
    public void onAttackEvent(AttackEvent e) {
        if (this.isLagging && e.attacker == mc.field_71439_g) {
            this.flushLag();
        }
    }

    @SubscribeEvent
    public void onGameTick(GameTickEvent e) {
        if (mc.field_71462_r != null) {
            if (this.isLagging) {
                this.flushLag();
            }
            this.resetState();
        }
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent e) {
        if (Utils.nullCheck()) {
            if (!this.isLagging) {
                this.clearIndicatorInterp();
            } else if (this.realPositionIndicator.isToggled()) {
                if (mc.field_71474_y.field_74320_O != 0 || this.showInFirstPerson.isToggled()) {
                    Vec3 delayedPos = Raven.lagHandler.getLastReleasedServerPosition();
                    if (delayedPos == null) {
                        this.clearIndicatorInterp();
                    } else {
                        long nowMs = System.currentTimeMillis();
                        if (this.indicatorInterpTo == null) {
                            this.indicatorInterpFrom = delayedPos;
                            this.indicatorInterpTo = delayedPos;
                            this.indicatorInterpStartMs = nowMs;
                        } else if (serverPosChanged(delayedPos, this.indicatorInterpTo)) {
                            double te = Math.min(1.0, (nowMs - this.indicatorInterpStartMs) / 80.0);
                            this.indicatorInterpFrom = lerpVec3(this.indicatorInterpFrom, this.indicatorInterpTo, te);
                            this.indicatorInterpTo = delayedPos;
                            this.indicatorInterpStartMs = nowMs;
                        }
                        double t = Math.min(1.0, (nowMs - this.indicatorInterpStartMs) / 80.0);
                        Vec3 drawPos = lerpVec3(this.indicatorInterpFrom, this.indicatorInterpTo, t);
                        double viewX = mc.func_175598_ae().field_78730_l;
                        double viewY = mc.func_175598_ae().field_78731_m;
                        double viewZ = mc.func_175598_ae().field_78728_n;
                        float halfW = mc.field_71439_g.field_70130_N / 2.0F;
                        float height = mc.field_71439_g.field_70131_O;
                        AxisAlignedBB axisAlignedBB = new AxisAlignedBB(drawPos.field_72450_a - halfW, drawPos.field_72448_b, drawPos.field_72449_c - halfW, drawPos.field_72450_a + halfW, drawPos.field_72448_b + height, drawPos.field_72449_c + halfW);
                        Vec3 cameraPos = Utils.getCameraPos(e.partialTicks);
                        if (!axisAlignedBB.func_72318_a(cameraPos)) {
                            AxisAlignedBB box = axisAlignedBB.func_72317_d(-viewX, -viewY, -viewZ);
                            float r = this.indicatorColor.getRed() / 255.0F;
                            float g = this.indicatorColor.getGreen() / 255.0F;
                            float b = this.indicatorColor.getBlue() / 255.0F;
                            float a = this.indicatorColor.getAlpha() / 255.0F;
                            GL11.glPushMatrix();
                            GlStateManager.func_179147_l();
                            GlStateManager.func_179090_x();
                            GlStateManager.func_179097_i();
                            GlStateManager.func_179132_a(false);
                            GL11.glBlendFunc(770, 771);
                            if (this.indicatorFilled.isToggled()) {
                                RenderUtils.drawBoundingBox(box, r, g, b, a);
                            }
                            GL11.glLineWidth((float) this.indicatorLineWidth.getInput());
                            GL11.glColor4f(r, g, b, a);
                            RenderGlobal.func_181561_a(box);
                            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
                            GlStateManager.func_179126_j();
                            GlStateManager.func_179132_a(true);
                            GlStateManager.func_179098_w();
                            GlStateManager.func_179084_k();
                            GL11.glPopMatrix();
                        }
                    }
                }
            }
        }
    }

    private void startLag() {
        this.outboundLag = new LagRequest(EnumLagDirection.ONLY_OUTBOUND, new ModuleBackedTimeout(this));
        Raven.lagHandler.requestLag(this.outboundLag);
        this.isLagging = true;
    }

    private void flushLag() {
        if (this.isLagging) {
            if (this.outboundLag != null) {
                this.outboundLag.getTimeout().forceTimeOut();
                this.outboundLag = null;
            }
            this.isLagging = false;
            this.clearIndicatorInterp();
        }
    }

    private void resetState() {
        this.currentTarget = null;
        this.lastDistSq = -1.0;
        this.isLagging = false;
        this.lastSelfHurtTime = 0;
        this.lastTargetHurtTime = 0;
        this.hitMarkedEntityId = -1;
        this.lastSprintState = false;
        this.lastBlockingState = false;
        this.outboundLag = null;
        this.clearIndicatorInterp();
    }

    private void clearIndicatorInterp() {
        this.indicatorInterpFrom = null;
        this.indicatorInterpTo = null;
        this.indicatorInterpStartMs = 0L;
    }

    private static boolean serverPosChanged(Vec3 a, Vec3 b) {
        return Math.abs(a.field_72450_a - b.field_72450_a) > 1.0E-6 || Math.abs(a.field_72448_b - b.field_72448_b) > 1.0E-6 || Math.abs(a.field_72449_c - b.field_72449_c) > 1.0E-6;
    }

    private static Vec3 lerpVec3(Vec3 from, Vec3 to, double t) {
        if (t <= 0.0) {
            return from;
        } else {
            return t >= 1.0 ? to : new Vec3(from.field_72450_a + (to.field_72450_a - from.field_72450_a) * t, from.field_72448_b + (to.field_72448_b - from.field_72448_b) * t, from.field_72449_c + (to.field_72449_c - from.field_72449_c) * t);
        }
    }

    private boolean sameTarget(EntityPlayer nextTarget) {
        return this.currentTarget != null && nextTarget != null ? this.currentTarget.func_145782_y() == nextTarget.func_145782_y() : this.currentTarget == nextTarget;
    }

    private boolean isMoving() {
        return mc.field_71439_g.field_70701_bs != 0.0F || mc.field_71439_g.field_70702_br != 0.0F;
    }
}
