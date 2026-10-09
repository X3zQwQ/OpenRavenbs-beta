package keystrokesmod.module.impl.network;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import keystrokesmod.event.ReceivePacketEvent;
import keystrokesmod.mixin.impl.accessor.IAccessorS14PacketEntity;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.render.TargetHUD;
import keystrokesmod.module.impl.world.AntiBot;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.ColorSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.RenderUtils;
import keystrokesmod.utility.RotationUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S13PacketDestroyEntities;
import net.minecraft.network.play.server.S14PacketEntity;
import net.minecraft.network.play.server.S18PacketEntityTeleport;
import net.minecraft.network.play.server.S40PacketDisconnect;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class Backtrack extends Module {

    private static final long POSITION_INTERP_MS = 80L;

    private static final double POS_EPS = 1.0E-6;

    private static final double MOVEMENT_DISTANCE_EPS = 0.001;

    private final SliderSetting minDistance;

    private final SliderSetting maxDistance;

    private final SliderSetting maxDelay;

    private final SliderSetting maxHurtTime;

    private final SliderSetting cooldown;

    private final ButtonSetting disableAdventure;

    private final ButtonSetting flushOnDamage;

    private final ButtonSetting ignoreTeammates;

    private final ButtonSetting weaponOnly;

    private final ButtonSetting showServerPosition;

    private final ColorSetting positionColor;

    private final Queue<Backtrack.TimedPacket> packetQueue = new ConcurrentLinkedQueue<>();

    private Vec3 targetPos = null;

    private Vec3 lastTargetSnapshot = null;

    private EntityPlayer target = null;

    private int currentDelay = 0;

    private long lastDeactivationTime = 0L;

    private long lastQueuedReleaseAt = 0L;

    private boolean delayingPackets = false;

    private boolean wasActive = false;

    private Vec3 positionInterpFrom;

    private Vec3 positionInterpTo;

    private long positionInterpStartMs;

    public Backtrack() {
        super("Backtrack", Module.category.network);
        this.liteModule = true;
        this.registerSetting(this.maxDelay = new SliderSetting("Delay", "ms", 200.0, 50.0, 500.0, 10.0));
        this.registerSetting(this.minDistance = new SliderSetting("Distance (min)", 0.0, 0.0, 3.0, 0.1));
        this.registerSetting(this.maxDistance = new SliderSetting("Distance (max)", 4.0, 1.0, 6.0, 0.1));
        this.registerSetting(this.cooldown = new SliderSetting("Cooldown", "ms", 0.0, 0.0, 2000.0, 50.0));
        this.registerSetting(this.maxHurtTime = new SliderSetting("Max hurt time", "ms", 500.0, 0.0, 500.0, 10.0));
        this.registerSetting(this.disableAdventure = new ButtonSetting("Disable adventure", true));
        this.registerSetting(this.flushOnDamage = new ButtonSetting("Flush on damage", true));
        this.registerSetting(this.ignoreTeammates = new ButtonSetting("Ignore teammates", true));
        this.registerSetting(this.weaponOnly = new ButtonSetting("Weapon only", false));
        this.registerSetting(this.showServerPosition = new ButtonSetting("Show server position", true));
        this.registerSetting(this.positionColor = new ColorSetting("Position color", 0, 0, 250, 100));
    }

    @Override
    public void guiUpdate() {
        Utils.correctValue(this.minDistance, this.maxDistance);
    }

    @Override
    public void onEnable() {
        this.clearPendingPackets();
        this.targetPos = null;
        this.lastTargetSnapshot = null;
        this.target = null;
        this.currentDelay = 0;
        this.lastDeactivationTime = 0L;
        this.delayingPackets = false;
        this.wasActive = false;
        this.clearPositionInterp();
    }

    @Override
    public void onDisable() {
        if (mc.func_71356_B()) {
            this.resetWithoutProcessingPackets();
        } else {
            this.releaseAll();
        }
        this.target = null;
        this.targetPos = null;
        this.lastTargetSnapshot = null;
        this.currentDelay = 0;
        this.clearPositionInterp();
    }

    @Override
    public void onUpdate() {
        if (mc.func_71356_B()) {
            this.resetWithoutProcessingPackets();
        } else if (mc.field_71439_g == null || mc.field_71441_e == null) {
            this.releaseAll();
        } else if (this.weaponOnly.isToggled() && !Utils.holdingWeapon()) {
            this.releaseAll();
        } else if (this.disableAdventure.isToggled() && mc.field_71442_b.func_178889_l().func_82752_c()) {
            this.releaseAll();
        } else if (this.flushOnDamage.isToggled() && mc.field_71439_g.field_70737_aN > 0) {
            this.releaseAll();
        } else {
            if (this.targetPos != null && this.target != null) {
                double distance = RotationUtils.distanceFromEyeToClosestOnAABB(this.target, this.targetPos);
                if ((distance > this.maxDistance.getInput() || distance < this.minDistance.getInput()) && this.wasActive) {
                    this.releaseAll();
                    this.lastDeactivationTime = System.currentTimeMillis();
                    this.wasActive = false;
                }
            }
            this.processDelayedPackets();
            if (this.isPacketQueueEmpty() && this.target != null) {
                this.targetPos = this.target.func_174791_d();
            }
            this.wasActive = this.currentDelay > 0 && !this.isPacketQueueEmpty();
        }
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) {
        if (!mc.func_71356_B()) {
            if (this.showServerPosition.isToggled()) {
                if (this.target != null && this.targetPos != null && !this.target.field_70128_L) {
                    if (this.currentDelay > 0) {
                        long nowMs = System.currentTimeMillis();
                        if (this.positionInterpTo == null) {
                            this.positionInterpFrom = this.targetPos;
                            this.positionInterpTo = this.targetPos;
                            this.positionInterpStartMs = nowMs;
                        } else if (positionChanged(this.targetPos, this.positionInterpTo)) {
                            double elapsedProgress = Math.min(1.0, (nowMs - this.positionInterpStartMs) / 80.0);
                            this.positionInterpFrom = lerpVec3(this.positionInterpFrom, this.positionInterpTo, elapsedProgress);
                            this.positionInterpTo = this.targetPos;
                            this.positionInterpStartMs = nowMs;
                        }
                        double progress = Math.min(1.0, (nowMs - this.positionInterpStartMs) / 80.0);
                        Vec3 drawPos = lerpVec3(this.positionInterpFrom, this.positionInterpTo, progress);
                        int color = this.positionColor.getColor();
                        TargetHUD targetHUD = ModuleManager.targetHUD;
                        if (targetHUD.isEspActiveFor(this.target)) {
                            color = targetHUD.getCurrentEspColor(this.positionColor.getAlpha()).getRGB();
                        }
                        RenderUtils.drawPlayerBoundingBox(drawPos, color);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onAttackEntity(AttackEntityEvent e) {
        if (!mc.func_71356_B()) {
            if (!this.weaponOnly.isToggled() || Utils.holdingWeapon()) {
                if (!this.disableAdventure.isToggled() || !mc.field_71442_b.func_178889_l().func_82752_c()) {
                    if (this.cooldown.getInput() > 0.0 && this.lastDeactivationTime > 0L) {
                        long elapsed = System.currentTimeMillis() - this.lastDeactivationTime;
                        if (elapsed < this.cooldown.getInput()) {
                            return;
                        }
                    }
                    if (e.target instanceof EntityPlayer) {
                        EntityPlayer attacked = (EntityPlayer) e.target;
                        if (!AntiBot.isBot(attacked) && (!Utils.isTeammate(attacked) || !this.ignoreTeammates.isToggled()) && !Utils.isFriended(attacked)) {
                            double distance = mc.field_71439_g.func_70032_d(attacked);
                            if (!(distance > this.maxDistance.getInput()) && !(distance < this.minDistance.getInput())) {
                                if (this.maxHurtTime.getInput() < 500.0) {
                                    int hurtTimeMs = attacked.field_70737_aN * 50;
                                    if (hurtTimeMs > this.maxHurtTime.getInput()) {
                                        return;
                                    }
                                }
                                if (this.target == null || attacked != this.target) {
                                    this.releaseAll();
                                    this.targetPos = attacked.func_174791_d();
                                }
                                this.target = attacked;
                                this.currentDelay = (int) this.maxDelay.getInput();
                            }
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onReceivePacket(ReceivePacketEvent e) {
        if (!mc.func_71356_B()) {
            if (Utils.nullCheck()) {
                if (mc.field_71439_g.field_70173_aa < 20) {
                    this.clearPendingPackets();
                    this.delayingPackets = false;
                } else {
                    Packet packet = e.getPacket();
                    this.processDelayedPackets();
                    if (this.weaponOnly.isToggled() && !Utils.holdingWeapon()) {
                        this.releaseAll();
                    } else if (this.disableAdventure.isToggled() && mc.field_71442_b.func_178889_l().func_82752_c()) {
                        this.releaseAll();
                    } else if (this.flushOnDamage.isToggled() && mc.field_71439_g.field_70737_aN > 0) {
                        this.releaseAll();
                    } else if (this.target == null) {
                        this.releaseAll();
                    } else if (packet instanceof S40PacketDisconnect) {
                        this.releaseAll();
                        this.target = null;
                        this.targetPos = null;
                    } else {
                        if (packet instanceof S13PacketDestroyEntities) {
                            S13PacketDestroyEntities destroyPacket = (S13PacketDestroyEntities) packet;
                            for (int id : destroyPacket.func_149098_c()) {
                                if (this.target != null && id == this.target.func_145782_y()) {
                                    this.target = null;
                                    this.targetPos = null;
                                    this.releaseAll();
                                    return;
                                }
                            }
                        }
                        if (this.currentDelay > 0) {
                            Vec3 nextTargetSnapshot = null;
                            if (packet instanceof S14PacketEntity) {
                                S14PacketEntity movePacket = (S14PacketEntity) packet;
                                if (((IAccessorS14PacketEntity) movePacket).getEntityId() == this.target.func_145782_y()) {
                                    Vec3 position = this.targetPos != null ? this.targetPos : this.target.func_174791_d();
                                    nextTargetSnapshot = position.func_72441_c(movePacket.func_149062_c() / 32.0, movePacket.func_149061_d() / 32.0, movePacket.func_149064_e() / 32.0);
                                }
                            }
                            if (packet instanceof S18PacketEntityTeleport) {
                                S18PacketEntityTeleport teleportPacket = (S18PacketEntityTeleport) packet;
                                if (teleportPacket.func_149451_c() == this.target.func_145782_y()) {
                                    nextTargetSnapshot = new Vec3(teleportPacket.func_149449_d() / 32.0, teleportPacket.func_149448_e() / 32.0, teleportPacket.func_149446_f() / 32.0);
                                }
                            }
                            if (nextTargetSnapshot != null) {
                                this.delayingPackets = this.isMovingAway(this.lastTargetSnapshot, nextTargetSnapshot);
                                this.targetPos = nextTargetSnapshot;
                                this.lastTargetSnapshot = nextTargetSnapshot;
                            }
                            if (this.shouldFlushForPacket(packet)) {
                                this.flushPendingPackets();
                            } else {
                                if (this.delayingPackets) {
                                    this.queuePacket(packet);
                                    e.setCanceled(true);
                                } else if (!this.isPacketQueueEmpty()) {
                                    this.flushPendingPackets();
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean shouldFlushForPacket(Packet packet) {
        if (packet instanceof S08PacketPlayerPosLook) {
            return true;
        } else {
            return packet instanceof S12PacketEntityVelocity && mc.field_71439_g != null ? ((S12PacketEntityVelocity) packet).func_149412_c() == mc.field_71439_g.func_145782_y() : false;
        }
    }

    private boolean isMovingAway(Vec3 previousSnapshot, Vec3 newSnapshot) {
        if (previousSnapshot != null && newSnapshot != null && this.target != null) {
            double previousDistance = RotationUtils.distanceFromEyeToClosestOnAABB(this.target, previousSnapshot);
            double newDistance = RotationUtils.distanceFromEyeToClosestOnAABB(this.target, newSnapshot);
            return newDistance > previousDistance + 0.001;
        } else {
            return false;
        }
    }

    private void processDelayedPackets() {
        synchronized (this.packetQueue) {
            long now = System.currentTimeMillis();
            while (!this.packetQueue.isEmpty()) {
                Backtrack.TimedPacket tp = this.packetQueue.peek();
                if (tp == null || now < tp.releaseAt) {
                    break;
                }
                this.packetQueue.poll();
                this.processPacket(tp.packet);
            }
            if (this.packetQueue.isEmpty()) {
                this.lastQueuedReleaseAt = 0L;
            }
        }
    }

    private void queuePacket(Packet packet) {
        synchronized (this.packetQueue) {
            long releaseAt = System.currentTimeMillis() + this.currentDelay;
            if (this.lastQueuedReleaseAt > 0L && releaseAt <= this.lastQueuedReleaseAt) {
                releaseAt = this.lastQueuedReleaseAt + 1L;
            }
            this.packetQueue.add(new Backtrack.TimedPacket(packet, releaseAt));
            this.lastQueuedReleaseAt = releaseAt;
        }
    }

    private void releaseAll() {
        this.flushPendingPackets();
        this.currentDelay = 0;
        this.lastTargetSnapshot = null;
        this.delayingPackets = false;
        this.clearPositionInterp();
    }

    private void flushPendingPackets() {
        synchronized (this.packetQueue) {
            while (!this.packetQueue.isEmpty()) {
                Backtrack.TimedPacket tp = this.packetQueue.poll();
                if (tp != null) {
                    this.processPacket(tp.packet);
                }
            }
            this.lastQueuedReleaseAt = 0L;
        }
    }

    private void clearPendingPackets() {
        synchronized (this.packetQueue) {
            this.packetQueue.clear();
            this.lastQueuedReleaseAt = 0L;
        }
    }

    private boolean isPacketQueueEmpty() {
        synchronized (this.packetQueue) {
            return this.packetQueue.isEmpty();
        }
    }

    private void resetWithoutProcessingPackets() {
        this.clearPendingPackets();
        this.target = null;
        this.targetPos = null;
        this.lastTargetSnapshot = null;
        this.currentDelay = 0;
        this.lastDeactivationTime = 0L;
        this.delayingPackets = false;
        this.wasActive = false;
        this.clearPositionInterp();
    }

    private void clearPositionInterp() {
        this.positionInterpFrom = null;
        this.positionInterpTo = null;
        this.positionInterpStartMs = 0L;
    }

    private static boolean positionChanged(Vec3 a, Vec3 b) {
        return Math.abs(a.field_72450_a - b.field_72450_a) > 1.0E-6 || Math.abs(a.field_72448_b - b.field_72448_b) > 1.0E-6 || Math.abs(a.field_72449_c - b.field_72449_c) > 1.0E-6;
    }

    private static Vec3 lerpVec3(Vec3 from, Vec3 to, double progress) {
        if (progress <= 0.0) {
            return from;
        } else {
            return progress >= 1.0 ? to : new Vec3(from.field_72450_a + (to.field_72450_a - from.field_72450_a) * progress, from.field_72448_b + (to.field_72448_b - from.field_72448_b) * progress, from.field_72449_c + (to.field_72449_c - from.field_72449_c) * progress);
        }
    }

    private void processPacket(Object packet) {
        if (packet != null) {
            try {
                if (packet instanceof Packet && mc.func_147114_u() != null) {
                    ((Packet) packet).func_148833_a(mc.func_147114_u());
                }
            } catch (Exception exception) {
            }
        }
    }

    public boolean isRenderingServerPositionFor(EntityLivingBase entity) {
        return !mc.func_71356_B() && this.showServerPosition.isToggled() && this.target == entity && this.targetPos != null && !this.target.field_70128_L && this.currentDelay > 0;
    }

    public Vec3 getBacktrackPosition() {
        return this.target != null && this.targetPos != null && !this.target.field_70128_L && this.currentDelay > 0 ? this.targetPos : null;
    }

    public EntityPlayer getBacktrackTarget() {
        return this.target != null && !this.target.field_70128_L && this.currentDelay > 0 ? this.target : null;
    }

    @Override
    public String getInfo() {
        return this.currentDelay > 0 && !this.isPacketQueueEmpty() ? this.currentDelay + "ms" : (int) this.maxDelay.getInput() + "ms";
    }

    private static class TimedPacket {

        final Object packet;

        final long releaseAt;

        TimedPacket(Object packet, long releaseAt) {
            this.packet = packet;
            this.releaseAt = releaseAt;
        }
    }
}
