package keystrokesmod.module.impl.movement;

import keystrokesmod.mixin.impl.accessor.IAccessorMinecraft;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.Utils;

public class Timer extends Module {

    private SliderSetting speed;

    private ButtonSetting gameplayOnly;

    private static double localUpdateAccumulator;

    private static int scheduledWorldUpdates = 1;

    private static boolean playerActionTick = true;

    private static boolean pendingAttack;

    private static boolean pendingUse;

    public Timer() {
        super("Timer", Module.category.movement);
        this.registerSetting(this.speed = new SliderSetting("Speed", 1.0, 0.0, 2.0, 0.05).allowOutOfRangeProfileValues());
        this.registerSetting(this.gameplayOnly = new ButtonSetting("Gameplay only", true, "Player only"));
    }

    public double getSpeed() {
        return this.speed.getInput();
    }

    public void setSpeed(double value) {
        this.speed.setValueRawWithEvent(value);
    }

    public void resetSpeed() {
        this.setSpeed(1.0);
    }

    @Override
    public String getInfo() {
        return Utils.asWholeNum(this.speed.getInput());
    }

    @Override
    public void onEnable() {
        resetLocalState();
        Utils.resetTimer();
    }

    @Override
    public void onDisable() {
        resetLocalState();
        Utils.resetTimer();
    }

    @Override
    public void onUpdate() {
        if (Utils.nullCheck()) {
            float configuredSpeed = (float) this.speed.getInput();
            if (usesGameplayTiming(this)) {
                Utils.resetTimer();
            } else {
                ((IAccessorMinecraft) mc).getTimer().field_74278_d = configuredSpeed;
            }
        }
    }

    public static void beginClientTick() {
        Timer timer = ModuleManager.timer;
        if (usesGameplayTiming(timer) && Utils.nullCheck()) {
            double configuredSpeed = Math.max(0.0, Math.min(2.0, timer.speed.getInput()));
            localUpdateAccumulator += configuredSpeed;
            int updates = (int) Math.floor(localUpdateAccumulator + 1.0E-9);
            localUpdateAccumulator -= updates;
            scheduledWorldUpdates = updates;
            playerActionTick = updates > 0;
        } else {
            resetLocalState();
        }
    }

    public static int consumeExtraLocalUpdatesForBaseTick() {
        return 0;
    }

    public static boolean shouldSkipBaseLocalUpdate() {
        return false;
    }

    public static int getWorldUpdateCount() {
        Timer timer = ModuleManager.timer;
        return usesGameplayTiming(timer) && Utils.nullCheck() ? scheduledWorldUpdates : 1;
    }

    public static float getWorldRenderPartialTicks(float partialTicks) {
        Timer timer = ModuleManager.timer;
        if (usesGameplayTiming(timer) && Utils.nullCheck()) {
            double renderPartialTicks = localUpdateAccumulator + partialTicks * timer.speed.getInput();
            return (float) Math.max(0.0, Math.min(1.0, renderPartialTicks));
        } else {
            return partialTicks;
        }
    }

    public static boolean allowAttack() {
        if (shouldProcessPlayerActions()) {
            return true;
        } else {
            pendingAttack = true;
            return false;
        }
    }

    public static boolean allowUse() {
        if (shouldProcessPlayerActions()) {
            return true;
        } else {
            pendingUse = true;
            return false;
        }
    }

    public static boolean shouldProcessPlayerActions() {
        Timer timer = ModuleManager.timer;
        return !usesGameplayTiming(timer) || !Utils.nullCheck() || playerActionTick;
    }

    public static void flushPendingActions() {
        if (shouldProcessPlayerActions() && Utils.nullCheck()) {
            if (mc.field_71462_r != null) {
                pendingAttack = false;
                pendingUse = false;
            } else {
                if (pendingAttack) {
                    pendingAttack = false;
                    ((IAccessorMinecraft) mc).callClickMouse();
                }
                if (pendingUse) {
                    pendingUse = false;
                    ((IAccessorMinecraft) mc).callRightClickMouse();
                }
            }
        }
    }

    private static void resetLocalState() {
        localUpdateAccumulator = 0.0;
        scheduledWorldUpdates = 1;
        playerActionTick = true;
        pendingAttack = false;
        pendingUse = false;
    }

    private static boolean usesGameplayTiming(Timer timer) {
        return timer != null && timer.isEnabled() && (timer.gameplayOnly.isToggled() || timer.speed.getInput() <= 0.0);
    }
}
