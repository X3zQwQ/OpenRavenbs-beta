package keystrokesmod.mixin.impl.entity;

import com.mojang.authlib.GameProfile;
import keystrokesmod.event.PostMotionEvent;
import keystrokesmod.event.PostUpdateEvent;
import keystrokesmod.event.PreMotionEvent;
import keystrokesmod.event.PreMovementPacketEvent;
import keystrokesmod.event.PreUpdateEvent;
import keystrokesmod.helper.RotationHelper;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.combat.WTap;
import keystrokesmod.module.impl.movement.NoSlow;
import keystrokesmod.module.impl.movement.Sprint;
import keystrokesmod.module.impl.movement.Timer;
import keystrokesmod.module.impl.player.Freecam;
import keystrokesmod.module.impl.player.Scaffold;
import keystrokesmod.utility.ModuleUtils;
import keystrokesmod.utility.RotationUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition;
import net.minecraft.network.play.client.C03PacketPlayer.C05PacketPlayerLook;
import net.minecraft.network.play.client.C03PacketPlayer.C06PacketPlayerPosLook;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C0BPacketEntityAction.Action;
import net.minecraft.network.play.client.C0BPacketEntityAction;
import net.minecraft.potion.Potion;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovementInput;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityPlayerSP.class)
public abstract class MixinEntityPlayerSP extends AbstractClientPlayer {

    @Shadow
    public int field_71157_e;

    @Shadow
    protected int field_71156_d;

    @Shadow
    public float field_71080_cy;

    @Shadow
    public float field_71086_bY;

    @Shadow
    protected Minecraft field_71159_c;

    @Shadow
    public MovementInput field_71158_b;

    @Shadow
    private int field_110320_a;

    @Shadow
    private float field_110321_bQ;

    @Shadow
    private boolean field_175171_bO;

    @Shadow
    @Final
    public NetHandlerPlayClient field_71174_a;

    @Shadow
    private boolean field_175170_bN;

    @Shadow
    private double field_175172_bI;

    @Shadow
    private double field_175166_bJ;

    @Shadow
    private double field_175167_bK;

    @Shadow
    private float field_175164_bL;

    @Shadow
    private float field_175165_bM;

    @Shadow
    private int field_175168_bP;

    public MixinEntityPlayerSP(World world, GameProfile gameProfile) {
        super(world, gameProfile);
    }

    @Shadow
    public abstract void func_70031_b(boolean enabled);

    @Shadow
    public abstract void func_71016_p();

    @Shadow
    protected abstract boolean func_175160_A();

    @Shadow
    public abstract boolean func_110317_t();

    @Shadow
    protected abstract void func_110318_g();

    @Shadow
    public abstract boolean func_70093_af();

    @Inject(method = "onUpdate", at = @At("HEAD"), cancellable = true)
    private void onUpdatePre(CallbackInfo c) {
        if (!Utils.isLocalPlayerSubUpdate() && Timer.shouldSkipBaseLocalUpdate()) {
            this.syncPrevRenderStateToCurrent();
            c.cancel();
        } else if (!Utils.isLocalPlayerSubUpdate()) {
            if (this.field_70170_p.func_175667_e(new BlockPos(this.field_70165_t, 0.0, this.field_70161_v))) {
                RotationUtils.prevRenderPitch = RotationUtils.renderPitch;
                RotationUtils.prevRenderYaw = RotationUtils.renderYaw;
                MinecraftForge.EVENT_BUS.post(new PreUpdateEvent());
            }
        }
    }

    @Inject(method = "onUpdate", at = @At("RETURN"))
    private void onUpdatePost(CallbackInfo c) {
        if (!Utils.isLocalPlayerSubUpdate()) {
            if (this.field_70170_p.func_175667_e(new BlockPos(this.field_70165_t, 0.0, this.field_70161_v))) {
                MinecraftForge.EVENT_BUS.post(new PostUpdateEvent());
            }
            int extraUpdates = Timer.consumeExtraLocalUpdatesForBaseTick();
            if (extraUpdates > 0) {
                MixinEntityPlayerSP.InterpolationState interpolationState = this.captureInterpolationState();
                for (int i = 0; i < extraUpdates; i++) {
                    Utils.beginLocalPlayerSubUpdate();
                    try {
                        this.func_70071_h_();
                    } finally {
                        Utils.endLocalPlayerSubUpdate();
                    }
                }
                this.restoreInterpolationState(interpolationState);
            }
        }
    }

    @Overwrite
    public void func_175161_p() {
        PreMotionEvent.setRotations = false;
        PreMotionEvent.setRenderYaw(false);
        RotationUtils.setFakeRotations = false;
        PreMotionEvent preMotionEvent = new PreMotionEvent(this.field_70165_t, this.func_174813_aQ().field_72338_b, this.field_70161_v, this.field_70177_z, this.field_70125_A, this.field_70122_E, this.func_70051_ag(), this.func_70093_af());
        MinecraftForge.EVENT_BUS.post(preMotionEvent);
        RotationUtils.serverRotations = new float[] { preMotionEvent.getYaw(), preMotionEvent.getPitch() };
        boolean flag = preMotionEvent.isSprinting();
        if (flag != this.field_175171_bO) {
            if (flag) {
                this.field_71174_a.func_147297_a(new C0BPacketEntityAction(this, Action.START_SPRINTING));
            } else {
                this.field_71174_a.func_147297_a(new C0BPacketEntityAction(this, Action.STOP_SPRINTING));
            }
            this.field_175171_bO = flag;
        }
        boolean flag1 = preMotionEvent.isSneaking();
        if (flag1 != this.field_175170_bN) {
            if (flag1) {
                this.field_71174_a.func_147297_a(new C0BPacketEntityAction(this, Action.START_SNEAKING));
            } else {
                this.field_71174_a.func_147297_a(new C0BPacketEntityAction(this, Action.STOP_SNEAKING));
            }
            this.field_175170_bN = flag1;
        }
        MinecraftForge.EVENT_BUS.post(new PreMovementPacketEvent());
        if (this.func_175160_A() || Freecam.freeEntity != null) {
            if (PreMotionEvent.setRenderYaw()) {
                RotationUtils.setRenderYaw(preMotionEvent.getYaw());
            }
            RotationUtils.renderPitch = preMotionEvent.getPitch();
            RotationUtils.renderYaw = preMotionEvent.getYaw();
            if (RotationUtils.setFakeRotations) {
                RotationUtils.renderPitch = RotationUtils.fakeRotations[1];
                RotationUtils.renderYaw = RotationUtils.fakeRotations[0];
                RotationUtils.setRenderYaw(RotationUtils.renderYaw);
            }
            RotationUtils.setFakeRotations = false;
            double d0 = preMotionEvent.getPosX() - this.field_175172_bI;
            double d1 = preMotionEvent.getPosY() - this.field_175166_bJ;
            double d2 = preMotionEvent.getPosZ() - this.field_175167_bK;
            double d3 = preMotionEvent.getYaw() - this.field_175164_bL;
            double d4 = preMotionEvent.getPitch() - this.field_175165_bM;
            boolean flag2 = d0 * d0 + d1 * d1 + d2 * d2 > 9.0E-4 || this.field_175168_bP >= 20;
            boolean flag3 = d3 != 0.0 || d4 != 0.0;
            if (this.field_70154_o == null) {
                if (flag2 && flag3) {
                    this.field_71174_a.func_147297_a(new C06PacketPlayerPosLook(preMotionEvent.getPosX(), preMotionEvent.getPosY(), preMotionEvent.getPosZ(), preMotionEvent.getYaw(), preMotionEvent.getPitch(), preMotionEvent.isOnGround()));
                } else if (flag2) {
                    this.field_71174_a.func_147297_a(new C04PacketPlayerPosition(preMotionEvent.getPosX(), preMotionEvent.getPosY(), preMotionEvent.getPosZ(), preMotionEvent.isOnGround()));
                } else if (flag3) {
                    this.field_71174_a.func_147297_a(new C05PacketPlayerLook(preMotionEvent.getYaw(), preMotionEvent.getPitch(), preMotionEvent.isOnGround()));
                } else {
                    this.field_71174_a.func_147297_a(new C03PacketPlayer(preMotionEvent.isOnGround()));
                }
            } else {
                this.field_71174_a.func_147297_a(new C06PacketPlayerPosLook(this.field_70159_w, -999.0, this.field_70179_y, preMotionEvent.getYaw(), preMotionEvent.getPitch(), preMotionEvent.isOnGround()));
                flag2 = false;
            }
            this.field_175168_bP++;
            if (flag2) {
                this.field_175172_bI = preMotionEvent.getPosX();
                this.field_175166_bJ = preMotionEvent.getPosY();
                this.field_175167_bK = preMotionEvent.getPosZ();
                this.field_175168_bP = 0;
            }
            if (flag3) {
                this.field_175164_bL = preMotionEvent.getYaw();
                this.field_175165_bM = preMotionEvent.getPitch();
            }
        }
        MinecraftForge.EVENT_BUS.post(new PostMotionEvent());
    }

    @Overwrite
    public void func_70636_d() {
        if (this.field_71157_e > 0) {
            this.field_71157_e--;
            if (this.field_71157_e == 0) {
                this.func_70031_b(false);
            }
        }
        if (this.field_71156_d > 0) {
            this.field_71156_d--;
        }
        this.field_71080_cy = this.field_71086_bY;
        if (this.field_71087_bX) {
            if (this.field_71159_c.field_71462_r != null && !this.field_71159_c.field_71462_r.func_73868_f()) {
                this.field_71159_c.func_147108_a(null);
            }
            if (this.field_71086_bY == 0.0F) {
                this.field_71159_c.func_147118_V().func_147682_a(PositionedSoundRecord.func_147674_a(new ResourceLocation("portal.trigger"), this.field_70146_Z.nextFloat() * 0.4F + 0.8F));
            }
            this.field_71086_bY += 0.0125F;
            if (this.field_71086_bY >= 1.0F) {
                this.field_71086_bY = 1.0F;
            }
            this.field_71087_bX = false;
        } else if (this.func_70644_a(Potion.field_76431_k) && this.func_70660_b(Potion.field_76431_k).func_76459_b() > 60 && (ModuleManager.antiDebuff == null || !ModuleManager.antiDebuff.canRemoveNausea(Potion.field_76431_k))) {
            this.field_71086_bY += 0.006666667F;
            if (this.field_71086_bY > 1.0F) {
                this.field_71086_bY = 1.0F;
            }
        } else {
            if (this.field_71086_bY > 0.0F) {
                this.field_71086_bY -= 0.05F;
            }
            if (this.field_71086_bY < 0.0F) {
                this.field_71086_bY = 0.0F;
            }
        }
        if (this.field_71088_bW > 0) {
            this.field_71088_bW--;
        }
        boolean flag = this.field_71158_b.field_78901_c;
        boolean flag1 = this.field_71158_b.field_78899_d;
        float f = 0.8F;
        boolean flag2 = this.field_71158_b.field_78900_b >= f;
        this.field_71158_b.func_78898_a();
        boolean stopSprint = ModuleManager.noSlow == null || !ModuleManager.noSlow.isEnabled() || NoSlow.slowed.getInput() == 80.0;
        if (this.func_71039_bw() && !this.func_70115_ae()) {
            MovementInput movementInput = this.field_71158_b;
            float slowed = NoSlow.getSlowed();
            movementInput.field_78902_a *= slowed;
            movementInput = this.field_71158_b;
            movementInput.field_78900_b *= slowed;
            if (stopSprint) {
                this.field_71156_d = 0;
            }
        }
        this.func_145771_j(this.field_70165_t - this.field_70130_N * 0.35, this.func_174813_aQ().field_72338_b + 0.5, this.field_70161_v + this.field_70130_N * 0.35);
        this.func_145771_j(this.field_70165_t - this.field_70130_N * 0.35, this.func_174813_aQ().field_72338_b + 0.5, this.field_70161_v - this.field_70130_N * 0.35);
        this.func_145771_j(this.field_70165_t + this.field_70130_N * 0.35, this.func_174813_aQ().field_72338_b + 0.5, this.field_70161_v - this.field_70130_N * 0.35);
        this.func_145771_j(this.field_70165_t + this.field_70130_N * 0.35, this.func_174813_aQ().field_72338_b + 0.5, this.field_70161_v + this.field_70130_N * 0.35);
        boolean flag3 = this.func_71024_bL().func_75116_a() > 6.0F || this.field_71075_bZ.field_75101_c;
        Scaffold scaffold = (Scaffold) ModuleManager.getModule(Scaffold.class);
        boolean scaffoldSprint = scaffold != null && scaffold.isAlwaysSprinting() && !this.field_71158_b.field_78899_d && (this.field_71158_b.field_78900_b != 0.0F || this.field_71158_b.field_78902_a != 0.0F);
        boolean sprintDirectionAllowed = this.field_71158_b.field_78900_b >= f || scaffoldSprint;
        boolean effectiveSprintKeyDown = this.field_71159_c.field_71474_y.field_151444_V.func_151470_d() || scaffoldSprint;
        if (this.field_70122_E && !flag1 && !flag2 && this.field_71158_b.field_78900_b >= f && !this.func_70051_ag() && flag3 && (!this.func_71039_bw() || !stopSprint) && !this.func_70644_a(Potion.field_76440_q)) {
            if (this.field_71156_d <= 0 && !effectiveSprintKeyDown) {
                this.field_71156_d = 7;
            } else {
                this.func_70031_b(true);
            }
        }
        if (!this.func_70051_ag() && effectiveSprintKeyDown && (this.field_71158_b.field_78900_b != 0.0F || this.field_71158_b.field_78902_a != 0.0F) && sprintDirectionAllowed && flag3 && (!this.func_71039_bw() && !this.field_71159_c.field_71439_g.func_70632_aY() || !stopSprint) && !this.func_70644_a(Potion.field_76440_q)) {
            this.func_70031_b(true);
        }
        if (this.func_70051_ag() && (!sprintDirectionAllowed || !flag3 || this.field_70123_F || ModuleUtils.setSlow || this.field_71158_b.field_78900_b == 0.0F && this.field_71158_b.field_78902_a == 0.0F || this.field_71159_c.field_71474_y.field_74311_E.func_151470_d() || ModuleManager.wTap.isEnabled() && WTap.stopSprint)) {
            this.func_70031_b(false);
            WTap.stopSprint = false;
        }
        Sprint sprintMod = ModuleManager.sprint;
        if (!RotationHelper.get().ownsServerRelativeMovement() && !this.func_70051_ag() && sprintMod != null && sprintMod.isEnabled() && !this.field_70123_F && (this.field_71158_b.field_78900_b != 0.0F || this.field_71158_b.field_78902_a != 0.0F) && !this.field_71159_c.field_71474_y.field_74311_E.func_151470_d() && !this.func_70644_a(Potion.field_76440_q)) {
            boolean movingBackwards = this.field_71158_b.field_78900_b < 0.0F;
            boolean movingSideways = this.field_71158_b.field_78900_b == 0.0F && this.field_71158_b.field_78902_a != 0.0F;
            boolean directionAllowed = !movingBackwards && !movingSideways || movingBackwards && sprintMod.allowWhileBackwards() || movingSideways && sprintMod.allowWhileSideways();
            boolean usingItemAllowed = !this.func_71039_bw() || sprintMod.allowWhileUsingItem();
            boolean needsOverride = movingBackwards || movingSideways || this.func_71039_bw();
            if (directionAllowed && usingItemAllowed && needsOverride) {
                this.func_70031_b(true);
            }
        }
        if (this.field_71075_bZ.field_75101_c) {
            if (this.field_71159_c.field_71442_b.func_178887_k()) {
                if (!this.field_71075_bZ.field_75100_b) {
                    this.field_71075_bZ.field_75100_b = true;
                    this.func_71016_p();
                }
            } else if (!flag && this.field_71158_b.field_78901_c) {
                if (this.field_71101_bC == 0) {
                    this.field_71101_bC = 7;
                } else {
                    this.field_71075_bZ.field_75100_b = !this.field_71075_bZ.field_75100_b;
                    this.func_71016_p();
                    this.field_71101_bC = 0;
                }
            }
        }
        if (this.field_71075_bZ.field_75100_b && this.func_175160_A()) {
            if (this.field_71158_b.field_78899_d) {
                this.field_70181_x = this.field_70181_x - this.field_71075_bZ.func_75093_a() * 3.0F;
            }
            if (this.field_71158_b.field_78901_c) {
                this.field_70181_x = this.field_70181_x + this.field_71075_bZ.func_75093_a() * 3.0F;
            }
        }
        if (this.func_110317_t()) {
            if (this.field_110320_a < 0) {
                this.field_110320_a++;
                if (this.field_110320_a == 0) {
                    this.field_110321_bQ = 0.0F;
                }
            }
            if (flag && !this.field_71158_b.field_78901_c) {
                this.field_110320_a = -10;
                this.func_110318_g();
            } else if (!flag && this.field_71158_b.field_78901_c) {
                this.field_110320_a = 0;
                this.field_110321_bQ = 0.0F;
            } else if (flag) {
                this.field_110320_a++;
                if (this.field_110320_a < 10) {
                    this.field_110321_bQ = this.field_110320_a * 0.1F;
                } else {
                    this.field_110321_bQ = 0.8F + 2.0F / (this.field_110320_a - 9) * 0.1F;
                }
            }
        } else {
            this.field_110321_bQ = 0.0F;
        }
        super.func_70636_d();
        if (this.field_70122_E && this.field_71075_bZ.field_75100_b && !this.field_71159_c.field_71442_b.func_178887_k()) {
            this.field_71075_bZ.field_75100_b = false;
            this.func_71016_p();
        }
    }

    private MixinEntityPlayerSP.InterpolationState captureInterpolationState() {
        return new MixinEntityPlayerSP.InterpolationState(this.field_70169_q, this.field_70167_r, this.field_70166_s, this.field_70142_S, this.field_70137_T, this.field_70136_U, this.field_70126_B, this.field_70127_C, this.field_70758_at, this.field_70760_ar, this.field_71107_bF, this.field_70727_aS, this.field_70141_P, this.field_70722_aY, this.field_70732_aI, this.field_71091_bM, this.field_71096_bN, this.field_71097_bO);
    }

    private void restoreInterpolationState(MixinEntityPlayerSP.InterpolationState state) {
        if (state != null) {
            this.field_70169_q = state.prevPosX;
            this.field_70167_r = state.prevPosY;
            this.field_70166_s = state.prevPosZ;
            this.field_70142_S = state.lastTickPosX;
            this.field_70137_T = state.lastTickPosY;
            this.field_70136_U = state.lastTickPosZ;
            this.field_70126_B = state.prevRotationYaw;
            this.field_70127_C = state.prevRotationPitch;
            this.field_70758_at = state.prevRotationYawHead;
            this.field_70760_ar = state.prevRenderYawOffset;
            this.field_71107_bF = state.prevCameraYaw;
            this.field_70727_aS = state.prevCameraPitch;
            this.field_70141_P = state.prevDistanceWalkedModified;
            this.field_70722_aY = state.prevLimbSwingAmount;
            this.field_70732_aI = state.prevSwingProgress;
            this.field_71091_bM = state.prevChasingPosX;
            this.field_71096_bN = state.prevChasingPosY;
            this.field_71097_bO = state.prevChasingPosZ;
        }
    }

    private void syncPrevRenderStateToCurrent() {
        this.field_70169_q = this.field_70165_t;
        this.field_70167_r = this.field_70163_u;
        this.field_70166_s = this.field_70161_v;
        this.field_70142_S = this.field_70165_t;
        this.field_70137_T = this.field_70163_u;
        this.field_70136_U = this.field_70161_v;
        this.field_70126_B = this.field_70177_z;
        this.field_70127_C = this.field_70125_A;
        this.field_70758_at = this.field_70759_as;
        this.field_70760_ar = this.field_70761_aq;
        this.field_71107_bF = this.field_71109_bG;
        this.field_70727_aS = this.field_70726_aT;
        this.field_70141_P = this.field_70140_Q;
        this.field_70722_aY = this.field_70721_aZ;
        this.field_70732_aI = this.field_70733_aJ;
        this.field_71091_bM = this.field_71094_bP;
        this.field_71096_bN = this.field_71095_bQ;
        this.field_71097_bO = this.field_71085_bR;
        this.field_71080_cy = this.field_71086_bY;
    }

    private static class InterpolationState {

        private final double prevPosX;

        private final double prevPosY;

        private final double prevPosZ;

        private final double lastTickPosX;

        private final double lastTickPosY;

        private final double lastTickPosZ;

        private final float prevRotationYaw;

        private final float prevRotationPitch;

        private final float prevRotationYawHead;

        private final float prevRenderYawOffset;

        private final float prevCameraYaw;

        private final float prevCameraPitch;

        private final float prevDistanceWalkedModified;

        private final float prevLimbSwingAmount;

        private final float prevSwingProgress;

        private final double prevChasingPosX;

        private final double prevChasingPosY;

        private final double prevChasingPosZ;

        private InterpolationState(double prevPosX, double prevPosY, double prevPosZ, double lastTickPosX, double lastTickPosY, double lastTickPosZ, float prevRotationYaw, float prevRotationPitch, float prevRotationYawHead, float prevRenderYawOffset, float prevCameraYaw, float prevCameraPitch, float prevDistanceWalkedModified, float prevLimbSwingAmount, float prevSwingProgress, double prevChasingPosX, double prevChasingPosY, double prevChasingPosZ) {
            this.prevPosX = prevPosX;
            this.prevPosY = prevPosY;
            this.prevPosZ = prevPosZ;
            this.lastTickPosX = lastTickPosX;
            this.lastTickPosY = lastTickPosY;
            this.lastTickPosZ = lastTickPosZ;
            this.prevRotationYaw = prevRotationYaw;
            this.prevRotationPitch = prevRotationPitch;
            this.prevRotationYawHead = prevRotationYawHead;
            this.prevRenderYawOffset = prevRenderYawOffset;
            this.prevCameraYaw = prevCameraYaw;
            this.prevCameraPitch = prevCameraPitch;
            this.prevDistanceWalkedModified = prevDistanceWalkedModified;
            this.prevLimbSwingAmount = prevLimbSwingAmount;
            this.prevSwingProgress = prevSwingProgress;
            this.prevChasingPosX = prevChasingPosX;
            this.prevChasingPosY = prevChasingPosY;
            this.prevChasingPosZ = prevChasingPosZ;
        }
    }
}
