package keystrokesmod.module.impl.combat;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Map;
import keystrokesmod.event.PreAttackEvent;
import keystrokesmod.event.PrePlayerInteractEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.DescriptionSetting;
import keystrokesmod.module.setting.impl.ItemListSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.CombatTargeting;
import keystrokesmod.utility.Utils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.Potion;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class HitSelect extends Module {

    private static final double HIT_RANGE = 3.0;

    private static final double HIT_RANGE_SQ = 9.0;

    private static final int HURT_WINDOW_TICKS = 10;

    private static final int SERVER_CONFIRM_COOLDOWN_TICKS = 10;

    private static final int SERVER_CONFIRM_TIMEOUT_TICKS = 30;

    private static final int BLOCK_WAIT_FIRST = 1;

    private static final int BLOCK_SERVER_COOLDOWN = 8;

    private static final int BLOCK_PREDICTED_BURST = 16;

    private static final int BLOCK_CRITICALS = 32;

    private final SliderSetting pauseDuration;

    private final SliderSetting mode;

    private final SliderSetting waitForFirstHit;

    private final ButtonSetting weaponOnly;

    private final ButtonSetting ignoreTeammates;

    private final ButtonSetting disableDuringKnockback;

    private final ButtonSetting onlyWhileDamaged;

    private final ButtonSetting useServerAttackTime;

    private final ButtonSetting fakeSwing;

    private final SliderSetting inCombatCancelRate;

    private final SliderSetting missedSwingsCancelRate;

    private final ButtonSetting itemBlacklistToggle;

    private final ItemListSetting itemBlacklist;

    private final String[] modes = new String[] { "Burst", "Criticals" };

    private EntityPlayer currentTarget;

    private final Map<Integer, HitSelect.TargetState> targetStates = new HashMap<>();

    private int lastSelfHurtTime;

    private boolean takingKnockback;

    private boolean waitFirstTracking;

    private int waitFirstStartTick = -1;

    private boolean waitFirstUnlocked;

    private int tickCounter;

    public HitSelect() {
        super("Hit Select", Module.category.combat);
        this.registerSetting(new DescriptionSetting("Filters unnecessary clicks."));
        this.registerSetting(this.pauseDuration = new SliderSetting("Pause duration", "ms", 500.0, 0.0, 500.0, 50.0));
        this.registerSetting(this.mode = new SliderSetting("Mode", 0, this.modes));
        this.registerSetting(this.waitForFirstHit = new SliderSetting("Wait for first hit", "ms", 0.0, 0.0, 500.0, 50.0));
        this.registerSetting(this.weaponOnly = new ButtonSetting("Weapons only", false));
        this.registerSetting(this.ignoreTeammates = new ButtonSetting("Ignore teammates", true));
        this.registerSetting(this.disableDuringKnockback = new ButtonSetting("Disable during knockback", false));
        this.registerSetting(this.onlyWhileDamaged = new ButtonSetting("Only while damaged", false));
        this.registerSetting(this.useServerAttackTime = new ButtonSetting("Use server attack time", false));
        this.registerSetting(this.fakeSwing = new ButtonSetting("Fake swing", false));
        this.registerSetting(new DescriptionSetting("Cancel rate"));
        this.registerSetting(this.inCombatCancelRate = new SliderSetting("In combat", "%", 100.0, 0.0, 100.0, 1.0));
        this.registerSetting(this.missedSwingsCancelRate = new SliderSetting("Missed swings", "%", 0.0, 0.0, 100.0, 1.0));
        this.registerSetting(this.itemBlacklistToggle = new ButtonSetting("Item blacklist", false));
        this.registerSetting(this.itemBlacklist = new ItemListSetting("Blacklisted items"));
        this.closetModule = true;
        this.liteModule = true;
    }

    @Override
    public String getInfo() {
        return this.modes[(int) this.mode.getInput()];
    }

    @Override
    public void guiUpdate() {
        this.itemBlacklist.setVisible(this.itemBlacklistToggle.isToggled(), this);
    }

    @Override
    public void onEnable() {
        this.tickCounter = 0;
        this.resetAllState();
    }

    @Override
    public void onDisable() {
        this.resetAllState();
    }

    private static int msToTicks(double ms) {
        return ms <= 0.0 ? 0 : (int) Math.ceil(ms / 50.0);
    }

    @SubscribeEvent
    public void onPrePlayerInteract(PrePlayerInteractEvent event) {
        if (Utils.nullCheck() && !mc.field_71439_g.field_70128_L && mc.field_71441_e != null) {
            this.tickCounter++;
            int currentTick = this.tickCounter;
            this.pruneTargetStates();
            EntityPlayer nextTarget = CombatTargeting.findTarget(9.0, this.ignoreTeammates.isToggled());
            this.updateCurrentTarget(nextTarget, currentTick);
            this.updateSelfDamage(currentTick);
            this.updateTargetDamage(currentTick);
        } else {
            this.resetAllState();
        }
    }

    @SubscribeEvent
    public void onPreAttack(PreAttackEvent event) {
        if (this.canProcessClicks() && (!this.weaponOnly.isToggled() || Utils.holdingWeapon()) && (!this.itemBlacklistToggle.isToggled() || !this.itemBlacklist.matches(mc.field_71439_g.func_70694_bm()))) {
            int currentTick = this.tickCounter;
            HitSelect.ClickType clickType = this.classifyClick(event.objectMouseOver);
            if (clickType != HitSelect.ClickType.BLOCK_INTERACTION) {
                if (clickType == HitSelect.ClickType.MISSED_SWING) {
                    if (this.shouldCancel(this.missedSwingsCancelRate.getInput())) {
                        this.cancelClick(event);
                    }
                } else {
                    EntityPlayer clickedTarget = CombatTargeting.asValidPlayer(event.objectMouseOver == null ? null : event.objectMouseOver.field_72308_g, 9.0, this.ignoreTeammates.isToggled());
                    if (clickedTarget != null) {
                        this.updateCurrentTarget(clickedTarget, currentTick);
                        HitSelect.TargetState state = this.getTargetState(clickedTarget, currentTick);
                        int blockMask = this.getValidHitBlockMask(state, currentTick);
                        boolean shouldBlock = (blockMask & 1) != 0 || (blockMask & 16) != 0 || this.applyPauseDuration(state, blockMask & -17, currentTick);
                        if (shouldBlock && this.shouldCancel(this.inCombatCancelRate.getInput())) {
                            this.cancelClick(event);
                        } else {
                            this.recordPassedValidHit(clickedTarget, currentTick);
                        }
                    }
                }
            }
        }
    }

    private boolean canProcessClicks() {
        return Utils.nullCheck() && mc.field_71441_e != null && mc.field_71439_g != null && !mc.field_71439_g.field_70128_L;
    }

    private HitSelect.ClickType classifyClick(MovingObjectPosition objectMouseOver) {
        if (objectMouseOver == null) {
            return HitSelect.ClickType.MISSED_SWING;
        } else if (objectMouseOver.field_72313_a == MovingObjectType.BLOCK) {
            return HitSelect.ClickType.BLOCK_INTERACTION;
        } else if (objectMouseOver.field_72313_a == MovingObjectType.ENTITY) {
            Entity entityHit = objectMouseOver.field_72308_g;
            return CombatTargeting.asValidPlayer(entityHit, 9.0, this.ignoreTeammates.isToggled()) != null ? HitSelect.ClickType.VALID_HIT : HitSelect.ClickType.MISSED_SWING;
        } else {
            return HitSelect.ClickType.MISSED_SWING;
        }
    }

    private void cancelClick(PreAttackEvent event) {
        if (this.fakeSwing.isToggled() && Utils.nullCheck()) {
            Utils.setSwinging();
        }
        event.setCanceled(true);
    }

    private void updateCurrentTarget(EntityPlayer nextTarget, int currentTick) {
        if (this.sameTarget(nextTarget)) {
            if (nextTarget != null) {
                this.currentTarget = nextTarget;
                this.getTargetState(nextTarget, currentTick);
            }
        } else {
            this.currentTarget = nextTarget;
            if (nextTarget == null) {
                this.resetWaitFirstState();
            } else if (!this.waitFirstTracking) {
                this.waitFirstTracking = true;
                this.waitFirstStartTick = currentTick;
                this.waitFirstUnlocked = false;
            }
            if (nextTarget != null) {
                this.getTargetState(nextTarget, currentTick);
            }
        }
    }

    private void updateSelfDamage(int currentTick) {
        int hurtTime = mc.field_71439_g.field_70737_aN;
        boolean hurtAgain = hurtTime > this.lastSelfHurtTime;
        if (hurtAgain) {
            if (this.waitFirstTracking && !this.waitFirstUnlocked) {
                this.waitFirstUnlocked = true;
            }
            if (!this.takingKnockback) {
                this.takingKnockback = true;
            }
            if (this.currentTarget != null) {
                HitSelect.TargetState state = this.getTargetState(this.currentTarget, currentTick);
                state.firstSelfHitSeen = true;
            }
        }
        if (this.takingKnockback && mc.field_71439_g.field_70122_E && !hurtAgain) {
            this.takingKnockback = false;
        }
        this.lastSelfHurtTime = hurtTime;
    }

    private void updateTargetDamage(int currentTick) {
        if (this.currentTarget != null && this.useServerAttackTime.isToggled()) {
            HitSelect.TargetState state = this.getTargetState(this.currentTarget, currentTick);
            int targetHurtTime = this.currentTarget.field_70737_aN;
            if (state.pendingServerConfirmationTick >= 0 && currentTick - state.pendingServerConfirmationTick > 30) {
                state.pendingServerConfirmationTick = -1;
            }
            if (state.pendingServerConfirmationTick >= 0 && targetHurtTime > state.lastObservedTargetHurtTime) {
                state.pendingServerConfirmationTick = -1;
                state.lastConfirmedTargetDamageTick = currentTick;
                state.rawBlockMask = 8;
                state.rawBlockStartTick = currentTick;
            }
            state.lastObservedTargetHurtTime = targetHurtTime;
        }
    }

    private int getValidHitBlockMask(HitSelect.TargetState state, int currentTick) {
        if (this.currentTarget == null) {
            return 0;
        } else if (this.disableDuringKnockback.isToggled() && this.isTakingKnockback()) {
            return 0;
        } else {
            int blockMask = 0;
            if (this.isWaitingForFirstHit(currentTick)) {
                blockMask |= 1;
            }
            blockMask |= this.getBurstBlockMask(state, currentTick);
            if (this.isCriticalsBlocked(state, currentTick)) {
                blockMask |= 32;
            }
            return blockMask;
        }
    }

    private int getBurstBlockMask(HitSelect.TargetState state, int currentTick) {
        if (this.useServerAttackTime.isToggled()) {
            return state.lastConfirmedTargetDamageTick >= 0 && currentTick - state.lastConfirmedTargetDamageTick < 10 ? 8 : 0;
        } else if (!this.isPredictedBurstWindowActive(state, currentTick)) {
            return 0;
        } else {
            int pauseTicks = msToTicks(this.pauseDuration.getInput());
            return pauseTicks > 0 && currentTick - state.predictedBurstWindowStartTick < pauseTicks ? 16 : 0;
        }
    }

    private boolean isCriticalsBlocked(HitSelect.TargetState state, int currentTick) {
        if ((int) this.mode.getInput() != 1) {
            return false;
        } else if (mc.field_71439_g.field_70122_E) {
            return false;
        } else if (this.onlyWhileDamaged.isToggled() && !state.firstSelfHitSeen) {
            return false;
        } else {
            return this.disableDuringKnockback.isToggled() && this.isTakingKnockback() ? false : !this.canCriticalHit();
        }
    }

    private boolean isWaitingForFirstHit(int currentTick) {
        if (!(this.waitForFirstHit.getInput() <= 0.0) && this.currentTarget != null && this.waitFirstTracking && !this.waitFirstUnlocked && this.waitFirstStartTick >= 0) {
            int requiredTicks = msToTicks(this.waitForFirstHit.getInput());
            return requiredTicks > 0 && currentTick - this.waitFirstStartTick < requiredTicks;
        } else {
            return false;
        }
    }

    private boolean canCriticalHit() {
        return mc.field_71439_g.field_70143_R > 0.0F && !mc.field_71439_g.field_70122_E && !mc.field_71439_g.func_70617_f_() && !mc.field_71439_g.func_70090_H() && !mc.field_71439_g.func_70644_a(Potion.field_76440_q) && mc.field_71439_g.field_70154_o == null;
    }

    private boolean isTakingKnockback() {
        return this.takingKnockback || mc.field_71439_g.field_70737_aN > 0;
    }

    private boolean applyPauseDuration(HitSelect.TargetState state, int blockMask, int currentTick) {
        if (blockMask == 0) {
            state.rawBlockMask = 0;
            state.rawBlockStartTick = -1;
            return false;
        } else if (this.pauseDuration.getInput() <= 0.0) {
            state.rawBlockMask = blockMask;
            state.rawBlockStartTick = currentTick;
            return false;
        } else {
            if (blockMask != state.rawBlockMask) {
                state.rawBlockMask = blockMask;
                state.rawBlockStartTick = currentTick;
            } else if (state.rawBlockStartTick < 0) {
                state.rawBlockStartTick = currentTick;
            }
            int requiredTicks = msToTicks(this.pauseDuration.getInput());
            return requiredTicks > 0 && currentTick - state.rawBlockStartTick < requiredTicks;
        }
    }

    private void recordPassedValidHit(EntityPlayer target, int currentTick) {
        if (target != null) {
            this.updateCurrentTarget(target, currentTick);
            HitSelect.TargetState state = this.getTargetState(target, currentTick);
            if (this.useServerAttackTime.isToggled()) {
                state.pendingServerConfirmationTick = currentTick;
                state.lastConfirmedTargetDamageTick = -1;
            } else {
                if (!this.isPredictedBurstWindowActive(state, currentTick)) {
                    this.startPredictedBurstWindow(state, currentTick, 10);
                }
            }
        }
    }

    private boolean shouldCancel(double chance) {
        if (chance <= 0.0) {
            return false;
        } else {
            return chance >= 100.0 ? true : Math.random() * 100.0 < chance;
        }
    }

    private boolean sameTarget(EntityPlayer nextTarget) {
        return this.currentTarget != null && nextTarget != null ? this.currentTarget.func_145782_y() == nextTarget.func_145782_y() : this.currentTarget == nextTarget;
    }

    private void resetWaitFirstState() {
        this.waitFirstTracking = false;
        this.waitFirstStartTick = -1;
        this.waitFirstUnlocked = false;
    }

    private int getHurtWindowTicks(EntityPlayer target) {
        return target != null && target.field_70738_aO > 0 ? Math.max(10, target.field_70738_aO) : 10;
    }

    private boolean isPredictedBurstWindowActive(HitSelect.TargetState state, int currentTick) {
        return state.predictedBurstWindowEndTick >= 0 && currentTick < state.predictedBurstWindowEndTick;
    }

    private void startPredictedBurstWindow(HitSelect.TargetState state, int startTick, int windowTicks) {
        int hurtWindowTicks = Math.max(1, windowTicks);
        state.predictedBurstWindowStartTick = startTick;
        state.predictedBurstWindowEndTick = startTick + hurtWindowTicks;
    }

    private void clearPredictedBurstWindow(HitSelect.TargetState state) {
        state.predictedBurstWindowStartTick = -1;
        state.predictedBurstWindowEndTick = -1;
    }

    private void syncPredictedBurstWindow(HitSelect.TargetState state, EntityPlayer target, int currentTick) {
        if (state.predictedBurstWindowEndTick >= 0 && currentTick >= state.predictedBurstWindowEndTick) {
            this.clearPredictedBurstWindow(state);
        }
        if (target != null && target.field_70737_aN > 0) {
            int hurtWindowTicks = Math.max(this.getHurtWindowTicks(target), target.field_70737_aN);
            int elapsedWindowTicks = hurtWindowTicks - target.field_70737_aN;
            int estimatedStartTick = currentTick - Math.max(0, elapsedWindowTicks);
            if (!this.isPredictedBurstWindowActive(state, currentTick) || estimatedStartTick > state.predictedBurstWindowStartTick) {
                this.startPredictedBurstWindow(state, estimatedStartTick, hurtWindowTicks);
            }
        }
    }

    private HitSelect.TargetState getTargetState(EntityPlayer target, int currentTick) {
        HitSelect.TargetState state = this.targetStates.get(target.func_145782_y());
        if (state == null) {
            state = new HitSelect.TargetState();
            if (this.useServerAttackTime.isToggled()) {
                state.lastObservedTargetHurtTime = target.field_70737_aN;
            }
            this.targetStates.put(target.func_145782_y(), state);
        }
        return state;
    }

    private void pruneTargetStates() {
        if (mc.field_71441_e == null) {
            this.targetStates.clear();
        } else {
            Iterator<Entry<Integer, HitSelect.TargetState>> iterator = this.targetStates.entrySet().iterator();
            while (iterator.hasNext()) {
                Entry<Integer, HitSelect.TargetState> entry = iterator.next();
                Entity entity = mc.field_71441_e.func_73045_a(entry.getKey());
                if (!(entity instanceof EntityPlayer) || entity.field_70128_L || ((EntityPlayer) entity).field_70725_aQ != 0) {
                    iterator.remove();
                }
            }
        }
    }

    private void resetAllState() {
        this.currentTarget = null;
        this.targetStates.clear();
        this.lastSelfHurtTime = 0;
        this.takingKnockback = false;
        this.resetWaitFirstState();
    }

    private static enum ClickType {

        VALID_HIT, BLOCK_INTERACTION, MISSED_SWING
    }

    private static class TargetState {

        boolean firstSelfHitSeen;

        int lastConfirmedTargetDamageTick = -1;

        int pendingServerConfirmationTick = -1;

        int predictedBurstWindowStartTick = -1;

        int predictedBurstWindowEndTick = -1;

        int lastObservedTargetHurtTime;

        int rawBlockStartTick = -1;

        int rawBlockMask;

        private TargetState() {
        }
    }
}
