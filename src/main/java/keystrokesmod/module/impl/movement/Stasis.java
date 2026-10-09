package keystrokesmod.module.impl.movement;

import keystrokesmod.event.PreUpdateEvent;
import keystrokesmod.event.ReceivePacketEvent;
import keystrokesmod.event.SendPacketEvent;
import keystrokesmod.mixin.impl.accessor.IAccessorMinecraft;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.Utils;
import net.minecraft.network.play.client.C03PacketPlayer.C05PacketPlayerLook;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;

public class Stasis extends Module {

    private SliderSetting pulse;

    private SliderSetting pulseDelay;

    private boolean allowNextC03;

    private int defaultTicks;

    private int pulseTicks;

    private float originalTimerSpeed;

    private boolean controlsTimer;

    private boolean pulseActive;

    private boolean motionStored;

    private boolean damageSuspended;

    private double storedMotionX;

    private double storedMotionY;

    private double storedMotionZ;

    public Stasis() {
        super("Stasis", Module.category.movement);
        this.registerSetting(this.pulse = new SliderSetting("Pulse", " tick", "§cNever", 60.0, 0.0, 200.0, 2.0));
        this.registerSetting(this.pulseDelay = new SliderSetting("Pulse delay", " tick", 10.0, 1.0, 150.0, 1.0));
    }

    @Override
    public void guiUpdate() {
        this.pulseDelay.setVisible(this.pulse.getInput() > 0.0, this);
    }

    @Override
    public void onEnable() {
        this.allowNextC03 = false;
        this.defaultTicks = 0;
        this.pulseTicks = 0;
        this.originalTimerSpeed = this.getTimerSpeed();
        this.controlsTimer = false;
        this.pulseActive = false;
        this.motionStored = false;
        this.damageSuspended = false;
        if (Utils.nullCheck()) {
            this.storeCurrentMotion();
        }
    }

    @Override
    public void onDisable() {
        this.restoreTimerSpeed();
        if (Utils.nullCheck() && !this.pulseActive && this.motionStored) {
            this.restoreStoredMotion();
        }
        this.defaultTicks = 0;
        this.pulseTicks = 0;
        this.pulseActive = false;
        this.motionStored = false;
        this.damageSuspended = false;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onClientTick(ClientTickEvent e) {
        if (e.phase == Phase.END) {
            if (!Utils.nullCheck()) {
                this.restoreTimerSpeed();
                this.defaultTicks = 0;
                this.pulseTicks = 0;
                this.pulseActive = false;
                this.motionStored = false;
                this.damageSuspended = false;
            } else if (mc.field_71439_g.field_70737_aN != 0) {
                this.restoreTimerSpeed();
                this.defaultTicks = 0;
                this.pulseTicks = 0;
                this.pulseActive = false;
                this.motionStored = false;
                this.damageSuspended = true;
            } else {
                if (this.damageSuspended) {
                    this.damageSuspended = false;
                    if (!this.motionStored) {
                        this.storeCurrentMotion();
                    }
                }
                int defaultDuration = (int) Math.round(this.pulse.getInput());
                if (defaultDuration <= 0) {
                    if (this.pulseActive) {
                        this.storeCurrentMotion();
                        this.pulseActive = false;
                    }
                    this.restoreTimerSpeed();
                    this.defaultTicks = 0;
                    this.pulseTicks = 0;
                } else {
                    this.controlsTimer = true;
                    int pulseDuration = Math.max(1, (int) Math.round(this.pulseDelay.getInput()));
                    if (this.pulseActive) {
                        this.pulseTicks++;
                        if (this.pulseTicks >= pulseDuration) {
                            this.finishPulse();
                        } else {
                            this.setPulseTimerSpeed(this.pulseTicks + 1, pulseDuration);
                        }
                    } else if (this.defaultTicks < defaultDuration) {
                        this.setTimerSpeed(this.originalTimerSpeed);
                        this.defaultTicks++;
                    } else {
                        this.startPulse(pulseDuration);
                    }
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPreUpdate(PreUpdateEvent e) {
        if (this.shouldFreezeLocalMovement()) {
            if (!this.motionStored) {
                this.storeCurrentMotion();
            } else {
                this.restoreStoredMotion();
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onLivingUpdate(LivingUpdateEvent e) {
        if (e.entityLiving == mc.field_71439_g && this.shouldFreezeLocalMovement()) {
            e.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onSendPacket(SendPacketEvent e) {
        if (e.getPacket() instanceof C03PacketPlayer) {
            C03PacketPlayer packet = (C03PacketPlayer) e.getPacket();
            if (this.allowNextC03) {
                this.allowNextC03 = false;
            } else if (this.shouldFreezeLocalMovement()) {
                if (!mc.field_71439_g.func_71039_bw() || packet.func_149466_j()) {
                    if (!(packet instanceof C05PacketPlayerLook)) {
                        e.setCanceled(true);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onReceivePacket(ReceivePacketEvent e) {
        if (e.getPacket() instanceof S08PacketPlayerPosLook) {
            this.allowNextC03 = true;
        }
    }

    private float getTimerSpeed() {
        return ((IAccessorMinecraft) mc).getTimer().field_74278_d;
    }

    private void setTimerSpeed(float speed) {
        ((IAccessorMinecraft) mc).getTimer().field_74278_d = speed;
    }

    private void restoreTimerSpeed() {
        if (this.controlsTimer) {
            this.setTimerSpeed(this.originalTimerSpeed);
            this.controlsTimer = false;
        }
    }

    private void startPulse(int pulseDuration) {
        this.pulseActive = true;
        this.pulseTicks = 0;
        if (!this.motionStored) {
            this.storeCurrentMotion();
        }
        this.restoreStoredMotion();
        this.setPulseTimerSpeed(1, pulseDuration);
    }

    private void finishPulse() {
        this.storeCurrentMotion();
        this.pulseActive = false;
        this.defaultTicks = 0;
        this.pulseTicks = 0;
        this.setTimerSpeed(this.originalTimerSpeed);
    }

    private void setPulseTimerSpeed(int pulseStep, int pulseDuration) {
        int peakTick = (pulseDuration + 1) / 2;
        double progress;
        if (pulseStep <= peakTick) {
            progress = (double) pulseStep / peakTick;
        } else {
            progress = (double) (pulseDuration - pulseStep) / (pulseDuration - peakTick);
        }
        float heartbeat = (float) Math.sin((Math.PI / 2) * progress);
        this.setTimerSpeed(this.originalTimerSpeed + (1.0F - this.originalTimerSpeed) * heartbeat);
    }

    public boolean shouldFreezeLocalMovement() {
        return this.isEnabled() && Utils.nullCheck() && mc.field_71439_g.field_70737_aN == 0 && !this.pulseActive;
    }

    private void storeCurrentMotion() {
        if (Utils.nullCheck()) {
            this.storedMotionX = mc.field_71439_g.field_70159_w;
            this.storedMotionY = mc.field_71439_g.field_70181_x;
            this.storedMotionZ = mc.field_71439_g.field_70179_y;
            this.motionStored = true;
        }
    }

    private void restoreStoredMotion() {
        if (Utils.nullCheck() && this.motionStored) {
            mc.field_71439_g.field_70159_w = this.storedMotionX;
            mc.field_71439_g.field_70181_x = this.storedMotionY;
            mc.field_71439_g.field_70179_y = this.storedMotionZ;
        }
    }
}
