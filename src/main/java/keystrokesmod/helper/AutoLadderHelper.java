package keystrokesmod.helper;

import java.util.Map;
import java.util.WeakHashMap;
import keystrokesmod.Raven;
import keystrokesmod.event.ClientRotationEvent;
import keystrokesmod.event.DispatchPacketEvent;
import keystrokesmod.event.GameTickEvent;
import keystrokesmod.utility.RotationUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.network.play.client.C0BPacketEntityAction;
import net.minecraft.network.play.client.C16PacketClientStatus;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public final class AutoLadderHelper {

    private final Minecraft mc = Minecraft.func_71410_x();

    private EntityPlayerSP player;

    private NetworkManager network;

    private World world;

    private final Map<Packet<?>, Boolean> dispatched = new WeakHashMap<>();

    private volatile AutoLadderHelper.SentState sent = AutoLadderHelper.SentState.empty();

    private volatile C08PacketPlayerBlockPlacement lastPlacement;

    private boolean registered;

    private boolean closing;

    private boolean restorePending;

    private int originalSlot = -1;

    private int selectedSlot = -1;

    private boolean manualChange;

    private long holdAfterMovement = -1L;

    private float holdYaw;

    private float holdPitch;

    private boolean aimed;

    private long slotRequestSequence = -1L;

    public void observe() {
        if (!this.registered) {
            MinecraftForge.EVENT_BUS.register(this);
            this.registered = true;
        }
        this.updatePlayer();
    }

    public void start() {
        this.observe();
        this.closing = false;
    }

    public synchronized void updatePlayer() {
        if (this.player != this.mc.field_71439_g || this.world != this.mc.field_71441_e || this.player != null && this.network != this.player.field_71174_a.func_147298_b()) {
            this.player = this.mc.field_71439_g;
            this.world = this.mc.field_71441_e;
            this.network = this.player == null ? null : this.player.field_71174_a.func_147298_b();
            this.sent = AutoLadderHelper.SentState.empty();
            this.lastPlacement = null;
            this.originalSlot = this.selectedSlot = -1;
            this.restorePending = this.manualChange = this.aimed = false;
            this.holdAfterMovement = this.slotRequestSequence = -1L;
            this.dispatched.clear();
        }
    }

    public AutoLadderHelper.SentState sent() {
        return this.sent;
    }

    public C08PacketPlayerBlockPlacement lastPlacement() {
        return this.lastPlacement;
    }

    @SubscribeEvent
    public synchronized void onDispatch(DispatchPacketEvent event) {
        this.updatePlayer();
        if (this.network != null && event.getNetworkManager() == this.network) {
            Packet<?> packet = event.getPacket();
            if (this.dispatched.put(packet, Boolean.TRUE) == null) {
                AutoLadderHelper.SentState old = this.sent;
                if (packet instanceof C08PacketPlayerBlockPlacement) {
                    this.lastPlacement = (C08PacketPlayerBlockPlacement) packet;
                }
                if (packet instanceof C03PacketPlayer) {
                    C03PacketPlayer movement = (C03PacketPlayer) packet;
                    this.sent = new AutoLadderHelper.SentState(old.sequence + 1L, -1L, movement.func_149466_j() ? movement.func_149464_c() : old.x, movement.func_149466_j() ? movement.func_149467_d() : old.y, movement.func_149466_j() ? movement.func_149472_e() : old.z, movement.func_149463_k() ? movement.func_149462_g() : old.yaw, movement.func_149463_k() ? movement.func_149470_h() : old.pitch, old.positionKnown || movement.func_149466_j(), old.rotationKnown || movement.func_149463_k(), old.slot);
                } else if (packet instanceof C09PacketHeldItemChange) {
                    this.sent = new AutoLadderHelper.SentState(old.sequence, old.actionSequence, old.x, old.y, old.z, old.yaw, old.pitch, old.positionKnown, old.rotationKnown, ((C09PacketHeldItemChange) packet).func_149614_c());
                } else if (packet instanceof C08PacketPlayerBlockPlacement || packet instanceof C02PacketUseEntity || packet instanceof C07PacketPlayerDigging || packet instanceof C0BPacketEntityAction || packet instanceof C16PacketClientStatus) {
                    this.sent = new AutoLadderHelper.SentState(old.sequence, old.sequence, old.x, old.y, old.z, old.yaw, old.pitch, old.positionKnown, old.rotationKnown, old.slot);
                }
            }
        }
    }

    public boolean canSwitch() {
        AutoLadderHelper.SentState state = this.sent;
        return this.player != null && this.mc.field_71442_b != null && state.sequence > 0L && state.actionSequence != state.sequence && (Raven.lagHandler == null || !Raven.lagHandler.isBuffering());
    }

    public boolean select(int slot) {
        if (slot < 0 || slot > 8 || this.player == null || this.manualChange || this.restorePending) {
            return false;
        } else if (this.selectedSlot >= 0 && this.player.field_71071_by.field_70461_c != this.selectedSlot) {
            this.abandonManualSelection();
            return false;
        } else if (!this.canSwitch()) {
            return false;
        } else {
            if (this.originalSlot < 0) {
                this.originalSlot = this.player.field_71071_by.field_70461_c;
            }
            if (!this.requestSlot(slot)) {
                return false;
            } else {
                this.selectedSlot = slot;
                return this.sent.slot == slot;
            }
        }
    }

    private boolean requestSlot(int slot) {
        if (this.sent.slot == slot && this.player.field_71071_by.field_70461_c == slot) {
            return true;
        } else if (this.slotRequestSequence == this.sent.sequence) {
            return false;
        } else {
            this.slotRequestSequence = this.sent.sequence;
            if (this.player.field_71071_by.field_70461_c != slot) {
                Utils.switchSlot(slot, true);
            } else {
                this.player.field_71174_a.func_147297_a(new C09PacketHeldItemChange(slot));
            }
            return this.player.field_71071_by.field_70461_c == slot;
        }
    }

    public boolean changedManually() {
        if (this.player != null && this.selectedSlot >= 0 && this.player.field_71071_by.field_70461_c != this.selectedSlot) {
            this.abandonManualSelection();
        }
        return this.manualChange;
    }

    private void abandonManualSelection() {
        this.originalSlot = this.selectedSlot = -1;
        this.restorePending = false;
        this.manualChange = true;
    }

    public void clearManualChange() {
        this.manualChange = false;
    }

    public boolean restoring() {
        return this.restorePending;
    }

    public void restore() {
        this.restorePending = this.originalSlot >= 0;
    }

    public int transferSlot() {
        this.updatePlayer();
        this.changedManually();
        int slot = this.originalSlot;
        this.originalSlot = this.selectedSlot = -1;
        this.restorePending = false;
        this.aimed = false;
        this.holdAfterMovement = -1L;
        return slot;
    }

    public void pinAim(float yaw, float pitch) {
        this.holdAfterMovement = this.sent.sequence;
        this.holdYaw = yaw;
        this.holdPitch = pitch;
        this.aimed = true;
    }

    public boolean holdingAim() {
        return this.holdAfterMovement >= 0L && this.sent.sequence <= this.holdAfterMovement;
    }

    public float heldYaw() {
        return this.holdYaw;
    }

    public float heldPitch() {
        return this.holdPitch;
    }

    public void rememberAim() {
        this.aimed = true;
    }

    public void finish() {
        this.restore();
        this.closing = true;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onTick(GameTickEvent event) {
        this.updatePlayer();
        if (this.player != null && this.restorePending && !this.changedManually() && this.canSwitch() && this.requestSlot(this.originalSlot)) {
            this.selectedSlot = this.originalSlot;
            if (this.sent.slot == this.originalSlot) {
                this.originalSlot = this.selectedSlot = -1;
                this.restorePending = false;
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onRotation(ClientRotationEvent event) {
        if (this.closing && this.player != null && this.aimed) {
            if (event.yaw != null || event.pitch != null) {
                this.aimed = false;
            } else if (this.holdingAim()) {
                event.claimExactRotations(this.holdYaw, this.holdPitch, false);
            } else {
                AutoLadderHelper.SentState state = this.sent;
                float dy = MathHelper.func_76142_g(this.player.field_70177_z - state.yaw);
                float dp = this.player.field_70125_A - state.pitch;
                if (Math.abs(dy) < 1.0F && Math.abs(dp) < 1.0F) {
                    this.aimed = false;
                } else {
                    float[] fixed = RotationUtils.fixRotation(state.yaw + MathHelper.func_76131_a(dy, -45.0F, 45.0F), state.pitch + MathHelper.func_76131_a(dp, -45.0F, 45.0F), state.yaw, state.pitch);
                    event.claimExactRotations(fixed[0], fixed[1], false);
                }
            }
        }
    }

    public static final class SentState {

        public final long sequence;

        public final long actionSequence;

        public final double x;

        public final double y;

        public final double z;

        public final float yaw;

        public final float pitch;

        public final boolean positionKnown;

        public final boolean rotationKnown;

        public final int slot;

        SentState(long sequence, long actionSequence, double x, double y, double z, float yaw, float pitch, boolean positionKnown, boolean rotationKnown, int slot) {
            this.sequence = sequence;
            this.actionSequence = actionSequence;
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
            this.positionKnown = positionKnown;
            this.rotationKnown = rotationKnown;
            this.slot = slot;
        }

        static AutoLadderHelper.SentState empty() {
            return new AutoLadderHelper.SentState(0L, -1L, 0.0, 0.0, 0.0, 0.0F, 0.0F, false, false, 0);
        }
    }
}
