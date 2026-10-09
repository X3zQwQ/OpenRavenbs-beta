package keystrokesmod.module.impl.network;

import keystrokesmod.Raven;
import keystrokesmod.event.GameTickEvent;
import keystrokesmod.event.ReceivePacketEvent;
import keystrokesmod.event.SendPacketEvent;
import keystrokesmod.lag.api.EnumLagDirection;
import keystrokesmod.lag.api.LagRequest;
import keystrokesmod.lag.timeout.ModuleBackedTimeout;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.DescriptionSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.PacketUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.server.S00PacketKeepAlive;
import net.minecraft.network.play.server.S01PacketJoinGame;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.network.play.server.S06PacketUpdateHealth;
import net.minecraft.network.play.server.S07PacketRespawn;
import net.minecraft.network.play.server.S0CPacketSpawnPlayer;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S14PacketEntity;
import net.minecraft.network.play.server.S18PacketEntityTeleport;
import net.minecraft.network.play.server.S19PacketEntityHeadLook;
import net.minecraft.network.play.server.S19PacketEntityStatus;
import net.minecraft.network.play.server.S21PacketChunkData;
import net.minecraft.network.play.server.S22PacketMultiBlockChange.BlockUpdateData;
import net.minecraft.network.play.server.S22PacketMultiBlockChange;
import net.minecraft.network.play.server.S23PacketBlockChange;
import net.minecraft.network.play.server.S24PacketBlockAction;
import net.minecraft.network.play.server.S25PacketBlockBreakAnim;
import net.minecraft.network.play.server.S26PacketMapChunkBulk;
import net.minecraft.network.play.server.S28PacketEffect;
import net.minecraft.network.play.server.S29PacketSoundEffect;
import net.minecraft.network.play.server.S32PacketConfirmTransaction;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.network.play.server.S3BPacketScoreboardObjective;
import net.minecraft.network.play.server.S3CPacketUpdateScore;
import net.minecraft.network.play.server.S3DPacketDisplayScoreboard;
import net.minecraft.network.play.server.S3EPacketTeams;
import net.minecraft.network.play.server.S3FPacketCustomPayload;
import net.minecraft.network.play.server.S40PacketDisconnect;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class Freeze extends Module {

    private final SliderSetting autoDisable;

    public final ButtonSetting activateOnHit;

    public final ButtonSetting disableOnBedBreak;

    public final ButtonSetting disableOnInteract;

    public final ButtonSetting showFreezeTicks;

    public final ButtonSetting showKnockbackDirection;

    public final ButtonSetting updateChatMessages;

    public final ButtonSetting updateBlockChanges;

    public final ButtonSetting updatePlayerHealth;

    public final ButtonSetting updatePlayerPositions;

    public final ButtonSetting updateScoreboard;

    public final ButtonSetting updateSoundEffects;

    private LagRequest inboundLagRequest;

    private boolean armed;

    private long freezeStartedAt;

    public Freeze() {
        super("Freeze", Module.category.network);
        this.registerSetting(new DescriptionSetting("Freeze inbound packets."));
        this.registerSetting(this.autoDisable = new SliderSetting("Auto-disable", " second", "§cNever", 2.0, 0.0, 10.0, 0.2));
        this.registerSetting(this.activateOnHit = new ButtonSetting("Activate on hit", false));
        this.registerSetting(this.disableOnBedBreak = new ButtonSetting("Disable on bed break", false));
        this.registerSetting(this.disableOnInteract = new ButtonSetting("Disable on interact", false));
        this.registerSetting(this.showFreezeTicks = new ButtonSetting("Show freeze ticks", true));
        this.registerSetting(this.showKnockbackDirection = new ButtonSetting("Show knockback direction", true));
        this.registerSetting(this.updateChatMessages = new ButtonSetting("Update chat messages", true));
        this.registerSetting(this.updateBlockChanges = new ButtonSetting("Update block changes", true));
        this.registerSetting(this.updatePlayerHealth = new ButtonSetting("Update player health", true));
        this.registerSetting(this.updatePlayerPositions = new ButtonSetting("Update player positions", true));
        this.registerSetting(this.updateScoreboard = new ButtonSetting("Update scoreboard", true));
        this.registerSetting(this.updateSoundEffects = new ButtonSetting("Update sound effects", true));
    }

    @Override
    public void onEnable() {
        if (blinksInbound()) {
            Utils.sendMessage("&cConflicted modules, disable blink or use outbound-only.");
            this.disable();
        } else {
            this.inboundLagRequest = null;
            this.freezeStartedAt = 0L;
            this.armed = this.activateOnHit.isToggled();
            if (!this.armed) {
                this.startInboundLag();
            }
        }
    }

    @Override
    public void onDisable() {
        this.flushInboundLagAndClear();
        this.armed = false;
        this.freezeStartedAt = 0L;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onReceivePacketHigh(ReceivePacketEvent e) {
        if (this.isEnabled() && !e.isCanceled() && e.getPacket() != null) {
            Packet<?> packet = e.getPacket();
            if (packet instanceof S40PacketDisconnect || packet instanceof S01PacketJoinGame || packet instanceof S07PacketRespawn) {
                this.disable();
            } else if (this.armed && this.isSelfVelocityPacket(packet)) {
                this.startInboundLag();
            } else if (this.isInboundSessionActive()) {
                if (this.disableOnBedBreak.isToggled() && this.isBedBreak(packet)) {
                    this.disable();
                } else {
                    if (this.shouldUpdateImmediately(packet)) {
                        PacketUtils.receivePacketNoEvent(packet);
                        e.setCanceled(true);
                    }
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onSendPacketHigh(SendPacketEvent e) {
        if (this.isEnabled() && this.isInboundSessionActive() && this.disableOnInteract.isToggled() && !e.isCanceled() && e.getPacket() != null) {
            Packet<?> packet = e.getPacket();
            if (packet instanceof C02PacketUseEntity || packet instanceof C07PacketPlayerDigging || packet instanceof C08PacketPlayerBlockPlacement) {
                this.disable();
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onGameTick(GameTickEvent e) {
        if (this.isEnabled()) {
            if (!Utils.nullCheck() || mc.field_71439_g == null || mc.field_71441_e == null || mc.field_71439_g.field_70128_L) {
                this.disable();
            } else if (this.isInboundSessionActive()) {
                long maximumFreezeTime = Math.round(this.autoDisable.getInput() * 1000.0);
                if (maximumFreezeTime > 0L && System.currentTimeMillis() - this.freezeStartedAt >= maximumFreezeTime) {
                    this.disable();
                }
            }
        }
    }

    private void startInboundLag() {
        if (!this.isInboundSessionActive()) {
            this.inboundLagRequest = new LagRequest(EnumLagDirection.ONLY_INBOUND, new ModuleBackedTimeout(this));
            Raven.lagHandler.requestLag(this.inboundLagRequest);
            this.freezeStartedAt = System.currentTimeMillis();
            this.armed = false;
        }
    }

    private boolean isInboundSessionActive() {
        return this.inboundLagRequest != null && !this.inboundLagRequest.getTimeout().isTimedOut();
    }

    private void flushInboundLagAndClear() {
        if (this.inboundLagRequest != null) {
            this.inboundLagRequest.getTimeout().forceTimeOut();
            this.inboundLagRequest = null;
        }
    }

    private boolean isSelfVelocityPacket(Packet<?> packet) {
        return packet instanceof S12PacketEntityVelocity && mc.field_71439_g != null ? ((S12PacketEntityVelocity) packet).func_149412_c() == mc.field_71439_g.func_145782_y() : false;
    }

    private boolean shouldUpdateImmediately(Packet<?> packet) {
        if (packet instanceof S00PacketKeepAlive || packet instanceof S32PacketConfirmTransaction || packet instanceof S3FPacketCustomPayload) {
            return true;
        } else if (this.updateChatMessages.isToggled() && packet instanceof S02PacketChat) {
            return true;
        } else if (this.updateBlockChanges.isToggled() && this.isBlockUpdatePacket(packet)) {
            return true;
        } else if (this.updatePlayerHealth.isToggled() && this.isHealthUpdatePacket(packet)) {
            return true;
        } else if (this.updatePlayerPositions.isToggled() && this.isOtherPlayerPositionPacket(packet)) {
            return true;
        } else {
            return this.updateScoreboard.isToggled() && this.isScoreboardPacket(packet) ? true : this.updateSoundEffects.isToggled() && this.isSoundPacket(packet);
        }
    }

    private boolean isBlockUpdatePacket(Packet<?> packet) {
        return packet instanceof S21PacketChunkData || packet instanceof S22PacketMultiBlockChange || packet instanceof S23PacketBlockChange || packet instanceof S24PacketBlockAction || packet instanceof S25PacketBlockBreakAnim || packet instanceof S26PacketMapChunkBulk || packet instanceof S35PacketUpdateTileEntity;
    }

    private boolean isHealthUpdatePacket(Packet<?> packet) {
        return packet instanceof S06PacketUpdateHealth || packet instanceof S19PacketEntityStatus;
    }

    private boolean isScoreboardPacket(Packet<?> packet) {
        return packet instanceof S3BPacketScoreboardObjective || packet instanceof S3CPacketUpdateScore || packet instanceof S3DPacketDisplayScoreboard || packet instanceof S3EPacketTeams;
    }

    private boolean isSoundPacket(Packet<?> packet) {
        return packet instanceof S28PacketEffect || packet instanceof S29PacketSoundEffect;
    }

    private boolean isOtherPlayerPositionPacket(Packet<?> packet) {
        if (mc.field_71441_e == null || mc.field_71439_g == null) {
            return false;
        } else if (packet instanceof S0CPacketSpawnPlayer) {
            return ((S0CPacketSpawnPlayer) packet).func_148943_d() != mc.field_71439_g.func_145782_y();
        } else if (packet instanceof S14PacketEntity) {
            Entity entity = ((S14PacketEntity) packet).func_149065_a(mc.field_71441_e);
            return entity instanceof EntityPlayer && entity != mc.field_71439_g;
        } else if (packet instanceof S18PacketEntityTeleport) {
            Entity entity = mc.field_71441_e.func_73045_a(((S18PacketEntityTeleport) packet).func_149451_c());
            return entity instanceof EntityPlayer && entity != mc.field_71439_g;
        } else if (!(packet instanceof S19PacketEntityHeadLook)) {
            return false;
        } else {
            Entity entity = ((S19PacketEntityHeadLook) packet).func_149381_a(mc.field_71441_e);
            return entity instanceof EntityPlayer && entity != mc.field_71439_g;
        }
    }

    private boolean isBedBreak(Packet<?> packet) {
        if (mc.field_71441_e == null) {
            return false;
        } else if (packet instanceof S23PacketBlockChange) {
            S23PacketBlockChange blockChange = (S23PacketBlockChange) packet;
            return mc.field_71441_e.func_180495_p(blockChange.func_179827_b()).func_177230_c() == Blocks.field_150324_C && blockChange.func_180728_a().func_177230_c() != Blocks.field_150324_C;
        } else {
            if (packet instanceof S22PacketMultiBlockChange) {
                S22PacketMultiBlockChange multiBlockChange = (S22PacketMultiBlockChange) packet;
                for (BlockUpdateData update : multiBlockChange.func_179844_a()) {
                    if (mc.field_71441_e.func_180495_p(update.func_180090_a()).func_177230_c() == Blocks.field_150324_C && update.func_180088_c().func_177230_c() != Blocks.field_150324_C) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    private static boolean blinksInbound() {
        Blink blink = ModuleManager.blink;
        return blink != null && blink.isEnabled() && blink.delaysInboundPackets();
    }

    @Override
    public String getInfo() {
        if (this.showFreezeTicks.isToggled() && this.isInboundSessionActive()) {
            long elapsed = Math.max(0L, System.currentTimeMillis() - this.freezeStartedAt);
            return String.valueOf(elapsed / 50L);
        } else {
            return "";
        }
    }
}
