package keystrokesmod.module.impl.combat;

import keystrokesmod.Raven;
import keystrokesmod.event.PrePlayerInteractEvent;
import keystrokesmod.event.RightClickMouseEvent;
import keystrokesmod.event.SendPacketEvent;
import keystrokesmod.event.UseItemEvent;
import keystrokesmod.lag.api.EnumLagDirection;
import keystrokesmod.lag.api.LagRequest;
import keystrokesmod.lag.timeout.ModuleBackedTimeout;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.world.AntiBot;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.BlockUtils;
import keystrokesmod.utility.CombatTargeting;
import keystrokesmod.utility.ReflectionUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.client.C02PacketUseEntity.Action;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.RenderTickEvent;
import org.lwjgl.input.Mouse;

public class Autoblock extends Module {

    private static final String[] MODES = new String[] { "Vanilla", "Lag" };

    private static final String[] UNBLOCK_OUT_OF_RANGE_MODES = new String[] { "Once", "Always", "Fake" };

    private static final int UNBLOCK_ONCE = 0;

    private static final int UNBLOCK_ALWAYS = 1;

    private static final int UNBLOCK_ALWAYS_FAKE = 2;

    private final SliderSetting mode;

    private final SliderSetting range;

    private final SliderSetting maxHurtTimeMs;

    private final SliderSetting maxHoldMs;

    private final SliderSetting cooldownMs;

    private final SliderSetting unblockOutOfRange;

    private final ButtonSetting requireLmb;

    private final ButtonSetting requireRmb;

    private final ButtonSetting onlyWhenDamaged;

    private final ButtonSetting ignoreTeammates;

    private final SliderSetting lagChance;

    private final SliderSetting lagMaxDuration;

    private final ButtonSetting preventDelayAttacks;

    private final ButtonSetting blockAgainImmediately;

    private final ButtonSetting forceBlockAnimation;

    private final ButtonSetting forceAttack;

    private int pendingForceAttacks;

    private boolean forceAttackUnblocked;

    private boolean isBlocking;

    private boolean manualBlock;

    private boolean targetWasInRange;

    private boolean unblockedAfterLeavingRange;

    private boolean allowingAlwaysInteraction;

    private int blockStartTick = -1;

    private long lastBlockEndTimeMs;

    private EntityPlayer currentTarget;

    private int lastSelfHurtTime;

    private boolean isLagging;

    private int lagStartTick = -1;

    private LagRequest outboundLag;

    private int tickCounter;

    public Autoblock() {
        super("Auto Block", Module.category.combat);
        this.liteModule = true;
        this.registerSetting(this.mode = new SliderSetting("Mode", 0, MODES));
        this.registerSetting(this.range = new SliderSetting("Range", 4.0, 2.0, 6.0, 0.1));
        this.registerSetting(this.maxHurtTimeMs = new SliderSetting("Maximum hurt time", "ms", 200.0, 50.0, 500.0, 50.0));
        this.registerSetting(this.maxHoldMs = new SliderSetting("Maximum hold duration", "ms", 150.0, 50.0, 500.0, 50.0));
        this.registerSetting(this.cooldownMs = new SliderSetting("Cooldown", "ms", 0.0, 0.0, 500.0, 50.0));
        this.registerSetting(this.lagChance = new SliderSetting("Lag chance", "%", 100.0, 0.0, 100.0, 5.0));
        this.registerSetting(this.lagMaxDuration = new SliderSetting("Lag max duration", "ms", 200.0, 50.0, 500.0, 50.0));
        this.registerSetting(this.unblockOutOfRange = new SliderSetting("Unblock out of range", true, 0, UNBLOCK_OUT_OF_RANGE_MODES));
        this.registerSetting(this.preventDelayAttacks = new ButtonSetting("Prevent delaying attacks", true));
        this.registerSetting(this.blockAgainImmediately = new ButtonSetting("Block again immediately", true));
        this.registerSetting(this.forceAttack = new ButtonSetting("Force attack", false));
        this.registerSetting(this.forceBlockAnimation = new ButtonSetting("Force block animation", true));
        this.registerSetting(this.requireLmb = new ButtonSetting("Require left mouse", true));
        this.registerSetting(this.requireRmb = new ButtonSetting("Require right mouse", false));
        this.registerSetting(this.onlyWhenDamaged = new ButtonSetting("Damaged", false));
        this.registerSetting(this.ignoreTeammates = new ButtonSetting("Ignore teammates", true));
        this.closetModule = true;
    }

    @Override
    public String getInfo() {
        return MODES[(int) this.mode.getInput()];
    }

    @Override
    public void guiUpdate() {
        boolean lagMode = this.isLagMode();
        this.lagChance.setVisible(lagMode, this);
        this.lagMaxDuration.setVisible(lagMode, this);
        this.preventDelayAttacks.setVisible(lagMode, this);
        this.blockAgainImmediately.setVisible(lagMode, this);
    }

    @Override
    public void onEnable() {
        this.tickCounter = 0;
        this.resetState(false);
    }

    private static int msToTicks(double ms) {
        return ms <= 0.0 ? 0 : (int) Math.ceil(ms / 50.0);
    }

    @Override
    public void onDisable() {
        this.resetState(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onMouse(MouseEvent e) {
        if (Utils.nullCheck() && Utils.holdingSword()) {
            if (ModuleManager.bedAura == null || !ModuleManager.bedAura.isActivelyMining()) {
                if (e.button == 1) {
                    if (this.isAlwaysUnblockMode()) {
                        if (!e.buttonstate && this.allowingAlwaysInteraction) {
                            this.allowingAlwaysInteraction = false;
                            return;
                        }
                        if (e.buttonstate && this.canInteractWhileAlwaysUnblocked()) {
                            this.releaseLag();
                            this.stopBlocking(true);
                            this.manualBlock = false;
                            this.allowingAlwaysInteraction = true;
                            return;
                        }
                    }
                    e.setCanceled(true);
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRightClickMouse(RightClickMouseEvent e) {
        if (this.shouldBlockVanillaUse()) {
            e.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onUseItem(UseItemEvent e) {
        if (this.allowingAlwaysInteraction || this.shouldBlockVanillaUse() || this.isFakeBlockOutOfRange()) {
            e.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onRenderTick(RenderTickEvent e) {
        if (e.phase == Phase.START) {
            if (!Utils.nullCheck()) {
                this.syncBlockAnimation();
            } else if (ModuleManager.bedAura != null && ModuleManager.bedAura.isActivelyMining()) {
                ReflectionUtils.setItemInUse(false);
            } else if (mc.field_71462_r == null || !this.isBlocking && !this.isLagging) {
                this.syncBlockAnimation();
            } else {
                this.resetState(true);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onSendPacket(SendPacketEvent e) {
        if (ModuleManager.bedAura != null && ModuleManager.bedAura.isActivelyMining()) {
            this.releaseLag();
        } else if (this.isLagging && this.preventDelayAttacks.isToggled()) {
            if (e.getPacket() instanceof C02PacketUseEntity) {
                if (((C02PacketUseEntity) e.getPacket()).func_149565_c() == Action.ATTACK) {
                    this.releaseLag();
                    if (this.blockAgainImmediately.isToggled() && Utils.holdingSword()) {
                        this.startBlocking(this.tickCounter);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onPrePlayerInteract(PrePlayerInteractEvent e) {
        this.forceAttackUnblocked = false;
        if (!Utils.nullCheck() || mc.field_71439_g.field_70128_L || mc.field_71462_r != null) {
            this.resetState(true);
        } else if (ModuleManager.bedAura != null && ModuleManager.bedAura.isActivelyMining()) {
            this.resetState(true);
        } else {
            int selfHurtTime = mc.field_71439_g.field_70737_aN;
            boolean hurtAgain = selfHurtTime > this.lastSelfHurtTime;
            this.lastSelfHurtTime = selfHurtTime;
            if (!Utils.holdingSword()) {
                this.resetState(false);
            } else if (this.allowingAlwaysInteraction) {
                this.releaseLag();
                this.stopBlocking(true);
                this.manualBlock = false;
            } else {
                this.tickCounter++;
                int currentTick = this.tickCounter;
                if (!this.forceAttack.isToggled() || !mc.field_71415_G) {
                    this.pendingForceAttacks = 0;
                }
                if (this.pendingForceAttacks > 0) {
                    this.forceAttackUnblocked = true;
                    this.releaseLag();
                    this.stopBlocking(true);
                    this.manualBlock = false;
                } else {
                    if (!this.isLagMode() && this.isLagging) {
                        this.releaseLag();
                    }
                    this.currentTarget = CombatTargeting.findTarget(this.range.getInput() * this.range.getInput(), this.ignoreTeammates.isToggled());
                    boolean killAuraAttacking = ModuleManager.killAura != null && ModuleManager.killAura.isEnabled() && !ModuleManager.killAura.isRequireMouseDown() && this.currentTarget != null;
                    boolean rmbDown = Mouse.isButtonDown(1);
                    boolean lmbDown = Mouse.isButtonDown(0) || killAuraAttacking;
                    boolean hasTarget = this.currentTarget != null;
                    boolean conditionsMet = hasTarget && this.checkConditions(lmbDown, rmbDown);
                    boolean leftTargetRange = this.targetWasInRange && !hasTarget;
                    this.targetWasInRange = hasTarget;
                    int unblockMode = (int) this.unblockOutOfRange.getInput();
                    if (unblockMode != 0 || !rmbDown || hasTarget) {
                        this.unblockedAfterLeavingRange = false;
                    }
                    if (this.isAlwaysUnblockMode() && !hasTarget) {
                        this.releaseLag();
                        this.stopBlocking(true);
                        this.manualBlock = false;
                    } else if (unblockMode == 0 && rmbDown && leftTargetRange) {
                        if (this.isLagging) {
                            this.releaseLag();
                        }
                        this.stopBlocking(true);
                        this.manualBlock = false;
                        this.unblockedAfterLeavingRange = true;
                    } else {
                        if (hurtAgain) {
                            this.releaseLag();
                            this.stopBlocking(true);
                            this.manualBlock = false;
                        }
                        if (!conditionsMet && rmbDown) {
                            if (!this.unblockedAfterLeavingRange) {
                                if (this.isLagging) {
                                    this.releaseLag();
                                }
                                if (!this.isBlocking) {
                                    this.startBlocking(currentTick);
                                }
                                this.manualBlock = true;
                            }
                        } else {
                            if (this.manualBlock) {
                                this.stopBlocking(true);
                                this.manualBlock = false;
                            }
                            if (this.isLagging) {
                                int lagMaxTicks = msToTicks(this.lagMaxDuration.getInput());
                                boolean lagExpired = lagMaxTicks > 0 && this.lagStartTick >= 0 && currentTick - this.lagStartTick >= lagMaxTicks;
                                if (lagExpired || !conditionsMet) {
                                    this.releaseLag();
                                    if (lagExpired && this.blockAgainImmediately.isToggled() && conditionsMet) {
                                        this.startBlocking(currentTick);
                                    }
                                }
                            }
                            if (!conditionsMet) {
                                this.stopBlocking(true);
                            } else {
                                if (!this.isBlocking && !this.isLagging && this.shouldPredictiveBlock()) {
                                    this.startBlocking(currentTick);
                                }
                                if (this.isBlocking) {
                                    int maxHoldTicks = msToTicks(this.maxHoldMs.getInput());
                                    boolean timeExpired = maxHoldTicks > 0 && this.blockStartTick >= 0 && currentTick - this.blockStartTick >= maxHoldTicks;
                                    if (timeExpired) {
                                        if (this.shouldStartLag()) {
                                            this.startLag(currentTick);
                                        }
                                        this.stopBlocking(true);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onForceAttack(PrePlayerInteractEvent e) {
        if (this.forceAttack.isToggled() && Utils.nullCheck() && !mc.field_71439_g.field_70128_L && mc.field_71462_r == null && mc.field_71415_G && Utils.holdingSword()) {
            if (ModuleManager.bedAura == null || !ModuleManager.bedAura.isActivelyMining()) {
                if (this.allowingAlwaysInteraction) {
                    this.pendingForceAttacks = 0;
                } else {
                    KeyBinding attackKey = mc.field_71474_y.field_74312_F;
                    if (this.pendingForceAttacks > 0 && !mc.field_71439_g.func_71039_bw()) {
                        for (int i = 0; i < this.pendingForceAttacks; i++) {
                            KeyBinding.func_74507_a(attackKey.func_151463_i());
                        }
                        this.pendingForceAttacks = 0;
                    } else if (this.isBlocking || this.isLagging || mc.field_71439_g.func_70632_aY() || this.pendingForceAttacks != 0) {
                        boolean attackPressed = attackKey.func_151468_f();
                        if (attackPressed || this.pendingForceAttacks != 0) {
                            if (mc.field_71439_g.func_71039_bw()) {
                                if (attackPressed) {
                                    this.pendingForceAttacks++;
                                }
                                while (attackKey.func_151468_f()) {
                                    this.pendingForceAttacks++;
                                }
                            } else {
                                KeyBinding.func_74507_a(attackKey.func_151463_i());
                            }
                            this.forceAttackUnblocked = true;
                            this.releaseLag();
                            this.stopBlocking(true);
                            this.manualBlock = false;
                        }
                    }
                }
            }
        }
    }

    private boolean checkConditions(boolean lmbDown, boolean rmbDown) {
        return this.requireLmb.isToggled() && !lmbDown ? false : !this.requireRmb.isToggled() || rmbDown;
    }

    private boolean shouldPredictiveBlock() {
        int ourHurtTime = mc.field_71439_g.field_70737_aN;
        int triggerTick = (int) Math.round(this.maxHurtTimeMs.getInput() / 50.0);
        triggerTick = Math.max(1, Math.min(10, triggerTick));
        return ourHurtTime == triggerTick || !this.onlyWhenDamaged.isToggled() && ourHurtTime == 0;
    }

    private boolean shouldBlockVanillaUse() {
        return this.isEnabled() && (this.isLagging || this.forceAttackUnblocked) && Utils.nullCheck() && Utils.holdingSword() && mc.field_71462_r == null;
    }

    private void startBlocking(int currentTick) {
        if (!this.forceAttackUnblocked && Utils.holdingSword() && !this.isCooldownActive()) {
            int keyCode = mc.field_71474_y.field_74313_G.func_151463_i();
            KeyBinding.func_74510_a(keyCode, true);
            KeyBinding.func_74507_a(keyCode);
            this.isBlocking = true;
            this.blockStartTick = currentTick;
            this.syncBlockAnimation();
        }
    }

    private void stopBlocking(boolean forceRelease) {
        if (this.isBlocking || forceRelease) {
            boolean wasBlocking = this.isBlocking;
            int keyCode = mc.field_71474_y.field_74313_G.func_151463_i();
            KeyBinding.func_74510_a(keyCode, false);
            this.isBlocking = false;
            this.blockStartTick = -1;
            if (wasBlocking) {
                this.lastBlockEndTimeMs = System.currentTimeMillis();
            }
            this.syncBlockAnimation();
        }
    }

    private boolean isCooldownActive() {
        double cooldown = this.cooldownMs.getInput();
        return cooldown > 0.0 && this.lastBlockEndTimeMs > 0L && System.currentTimeMillis() - this.lastBlockEndTimeMs < cooldown;
    }

    private boolean shouldStartLag() {
        if (!this.isLagMode()) {
            return false;
        } else {
            double chance = this.lagChance.getInput();
            if (chance <= 0.0) {
                return false;
            } else {
                return chance >= 100.0 ? true : Math.random() * 100.0 < chance;
            }
        }
    }

    private void startLag(int currentTick) {
        if (!this.isLagging) {
            int lagReferenceTick = this.blockStartTick >= 0 ? this.blockStartTick : currentTick;
            int lagMaxTicks = msToTicks(this.lagMaxDuration.getInput());
            if (lagMaxTicks <= 0 || currentTick - lagReferenceTick < lagMaxTicks) {
                this.outboundLag = new LagRequest(EnumLagDirection.ONLY_OUTBOUND, new ModuleBackedTimeout(this));
                Raven.lagHandler.requestLag(this.outboundLag);
                this.isLagging = true;
                this.lagStartTick = lagReferenceTick;
                this.syncBlockAnimation();
            }
        }
    }

    private void releaseLag() {
        if (this.isLagging) {
            if (this.outboundLag != null) {
                this.outboundLag.getTimeout().forceTimeOut();
                this.outboundLag = null;
            }
            this.isLagging = false;
            this.lagStartTick = -1;
            this.syncBlockAnimation();
        }
    }

    public boolean isActive() {
        return this.isEnabled() && (this.isBlocking || this.isLagging);
    }

    private boolean isLagMode() {
        return this.mode.getInput() == 1.0;
    }

    private boolean isAlwaysUnblockMode() {
        int unblockMode = (int) this.unblockOutOfRange.getInput();
        return unblockMode == 1 || unblockMode == 2;
    }

    private boolean isFakeBlockOutOfRange() {
        return this.isEnabled() && (int) this.unblockOutOfRange.getInput() == 2 && this.currentTarget == null && Utils.nullCheck() && !mc.field_71439_g.field_70128_L && mc.field_71462_r == null && Utils.holdingSword() && (ModuleManager.bedAura == null || !ModuleManager.bedAura.isActivelyMining());
    }

    private boolean canInteractWhileAlwaysUnblocked() {
        MovingObjectPosition hit = mc.field_71476_x;
        if (hit == null) {
            return false;
        } else if (hit.field_72313_a == MovingObjectType.BLOCK) {
            return BlockUtils.isInteractable(hit);
        } else if (hit.field_72313_a == MovingObjectType.ENTITY && hit.field_72308_g != null) {
            Entity entity = hit.field_72308_g;
            return !(entity instanceof EntityPlayer) || AntiBot.isBot(entity);
        } else {
            return false;
        }
    }

    private void syncBlockAnimation() {
        boolean killAuraAttacking = ModuleManager.killAura != null && ModuleManager.killAura.isEnabled() && !ModuleManager.killAura.isRequireMouseDown() && this.currentTarget != null;
        boolean requiredMouseButtonsDown = this.checkConditions(Mouse.isButtonDown(0) || killAuraAttacking, Mouse.isButtonDown(1));
        boolean continuousUndamagedBlock = !this.onlyWhenDamaged.isToggled() && this.currentTarget != null && !this.allowingAlwaysInteraction;
        boolean shouldAnimate = this.forceBlockAnimation.isToggled() && Utils.nullCheck() && mc.field_71462_r == null && Utils.holdingSword() && requiredMouseButtonsDown && (continuousUndamagedBlock || this.isBlocking || this.isLagging);
        boolean fakeBlock = this.isFakeBlockOutOfRange() && mc.field_71415_G && !this.allowingAlwaysInteraction && Utils.isBindDown(mc.field_71474_y.field_74313_G);
        ReflectionUtils.setItemInUse(shouldAnimate || fakeBlock);
    }

    private void resetState(boolean releaseUseKey) {
        this.pendingForceAttacks = 0;
        this.forceAttackUnblocked = false;
        boolean restorePhysicalUse = this.isBlocking && mc.field_71474_y.field_74313_G.func_151470_d() && Mouse.isButtonDown(1) && mc.field_71462_r == null;
        this.releaseLag();
        this.stopBlocking(releaseUseKey);
        this.manualBlock = false;
        this.targetWasInRange = false;
        this.unblockedAfterLeavingRange = false;
        this.allowingAlwaysInteraction = false;
        this.lastBlockEndTimeMs = 0L;
        this.currentTarget = null;
        this.lastSelfHurtTime = 0;
        this.syncBlockAnimation();
        if (restorePhysicalUse) {
            KeyBinding.func_74510_a(mc.field_71474_y.field_74313_G.func_151463_i(), true);
        }
    }
}
