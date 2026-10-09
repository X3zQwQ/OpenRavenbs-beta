package keystrokesmod.utility;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import keystrokesmod.mixin.impl.accessor.IAccessorMinecraft;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S04PacketEntityEquipment;
import net.minecraft.network.play.server.S0APacketUseBed;
import net.minecraft.network.play.server.S0BPacketAnimation;
import net.minecraft.network.play.server.S0CPacketSpawnPlayer;
import net.minecraft.network.play.server.S0DPacketCollectItem;
import net.minecraft.network.play.server.S0EPacketSpawnObject;
import net.minecraft.network.play.server.S0FPacketSpawnMob;
import net.minecraft.network.play.server.S10PacketSpawnPainting;
import net.minecraft.network.play.server.S11PacketSpawnExperienceOrb;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S13PacketDestroyEntities;
import net.minecraft.network.play.server.S14PacketEntity;
import net.minecraft.network.play.server.S18PacketEntityTeleport;
import net.minecraft.network.play.server.S19PacketEntityHeadLook;
import net.minecraft.network.play.server.S19PacketEntityStatus;
import net.minecraft.network.play.server.S1BPacketEntityAttach;
import net.minecraft.network.play.server.S1CPacketEntityMetadata;
import net.minecraft.network.play.server.S1DPacketEntityEffect;
import net.minecraft.network.play.server.S1EPacketRemoveEntityEffect;
import net.minecraft.network.play.server.S2CPacketSpawnGlobalEntity;

public class FrozenEntitySync {

    private static final FrozenEntitySync INSTANCE = new FrozenEntitySync();

    private final Queue<Packet<?>> liveEntityQueue = new ConcurrentLinkedQueue<>();

    private final Queue<Packet<?>> deferredQueue = new ConcurrentLinkedQueue<>();

    private boolean active;

    private long lastFrozenTickNano;

    private double frozenTickAccumulator;

    private static final long TICK_NANOS = 50000000L;

    private static final int MAX_STEPS_PER_FRAME = 10;

    public static FrozenEntitySync get() {
        return INSTANCE;
    }

    public boolean isActive() {
        return this.active;
    }

    public void start() {
        if (!this.active) {
            this.active = true;
            this.lastFrozenTickNano = System.nanoTime();
            this.frozenTickAccumulator = 0.0;
        }
    }

    public void stop() {
        if (this.active) {
            this.active = false;
            this.flush();
        }
    }

    public void clearAll() {
        this.active = false;
        this.liveEntityQueue.clear();
        this.deferredQueue.clear();
        this.frozenTickAccumulator = 0.0;
    }

    public boolean intercept(Packet<?> packet) {
        if (!this.active) {
            return false;
        } else {
            if (isEntityPacket(packet)) {
                this.liveEntityQueue.add(packet);
            } else {
                this.deferredQueue.add(packet);
            }
            return true;
        }
    }

    public void pumpFrame() {
        if (this.active) {
            Minecraft mc = Minecraft.func_71410_x();
            if (mc.field_71441_e != null && mc.field_71439_g != null) {
                this.drainLiveQueue();
                long now = System.nanoTime();
                long elapsed = now - this.lastFrozenTickNano;
                this.lastFrozenTickNano = now;
                this.frozenTickAccumulator += elapsed;
                int steps = 0;
                while (this.frozenTickAccumulator >= 5.0E7 && steps < 10) {
                    this.frozenTickAccumulator -= 5.0E7;
                    steps++;
                    this.tickNonLocalEntities(mc);
                }
                if (steps >= 10) {
                    this.frozenTickAccumulator = 0.0;
                }
                float frozenPartial = (float) (this.frozenTickAccumulator / 5.0E7);
                net.minecraft.util.Timer timer = ((IAccessorMinecraft) mc).getTimer();
                timer.field_74281_c = frozenPartial;
                this.snapLocalPlayerInterpolation(mc.field_71439_g);
            }
        }
    }

    private void snapLocalPlayerInterpolation(EntityPlayerSP player) {
        player.field_70142_S = player.field_70165_t;
        player.field_70137_T = player.field_70163_u;
        player.field_70136_U = player.field_70161_v;
        player.field_70126_B = player.field_70177_z;
        player.field_70127_C = player.field_70125_A;
        player.field_70758_at = player.field_70759_as;
        player.field_70760_ar = player.field_70761_aq;
        player.field_70722_aY = player.field_70721_aZ;
        player.field_70732_aI = player.field_70733_aJ;
        player.field_70727_aS = player.field_70726_aT;
    }

    public void flush() {
        this.drainLiveQueue();
        Packet<?> p;
        while ((p = this.deferredQueue.poll()) != null) {
            PacketUtils.receivePacketNoEvent(p);
        }
        this.frozenTickAccumulator = 0.0;
    }

    private void drainLiveQueue() {
        Packet<?> p;
        while ((p = this.liveEntityQueue.poll()) != null) {
            PacketUtils.receivePacketNoEvent(p);
        }
    }

    private void tickNonLocalEntities(Minecraft mc) {
        EntityPlayerSP local = mc.field_71439_g;
        for (int i = 0; i < mc.field_71441_e.field_73007_j.size(); i++) {
            Entity entity = (Entity) mc.field_71441_e.field_73007_j.get(i);
            try {
                entity.field_70173_aa++;
                entity.func_70071_h_();
            } catch (Throwable exception) {
            }
            if (entity.field_70128_L) {
                mc.field_71441_e.field_73007_j.remove(i--);
            }
        }
        for (Entity entity : mc.field_71441_e.field_72996_f) {
            if (entity != null && !entity.field_70128_L && entity != local) {
                if (entity.field_70154_o != null) {
                    if (!entity.field_70154_o.field_70128_L && entity.field_70154_o.field_70153_n == entity) {
                        continue;
                    }
                    entity.field_70154_o.field_70153_n = null;
                    entity.field_70154_o = null;
                }
                try {
                    mc.field_71441_e.func_72866_a(entity, true);
                } catch (Throwable exception2) {
                }
            }
        }
    }

    private static boolean isEntityPacket(Packet<?> packet) {
        return packet instanceof S0CPacketSpawnPlayer || packet instanceof S0FPacketSpawnMob || packet instanceof S0EPacketSpawnObject || packet instanceof S10PacketSpawnPainting || packet instanceof S11PacketSpawnExperienceOrb || packet instanceof S2CPacketSpawnGlobalEntity || packet instanceof S12PacketEntityVelocity || packet instanceof S13PacketDestroyEntities || packet instanceof S14PacketEntity || packet instanceof S18PacketEntityTeleport || packet instanceof S19PacketEntityHeadLook || packet instanceof S0BPacketAnimation || packet instanceof S0APacketUseBed || packet instanceof S1BPacketEntityAttach || packet instanceof S1CPacketEntityMetadata || packet instanceof S04PacketEntityEquipment || packet instanceof S1DPacketEntityEffect || packet instanceof S1EPacketRemoveEntityEffect || packet instanceof S19PacketEntityStatus || packet instanceof S0DPacketCollectItem;
    }
}
