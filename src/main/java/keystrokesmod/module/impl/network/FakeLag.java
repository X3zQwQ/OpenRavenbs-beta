package keystrokesmod.module.impl.network;

import java.util.Set;
import keystrokesmod.Raven;
import keystrokesmod.event.GameTickEvent;
import keystrokesmod.lag.api.EnumLagDirection;
import keystrokesmod.lag.api.LagRequest;
import keystrokesmod.lag.timeout.ModuleBackedTimeout;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.Utils;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import org.jetbrains.annotations.Nullable;

public class FakeLag extends Module {

    private static final String[] MODE_LABELS = new String[] { "Inbound", "Outbound", "Both" };

    private final SliderSetting mode;

    private final SliderSetting packetDelaySlider;

    private int appliedMode = -1;

    private long appliedDelayMs = -1L;

    @Nullable
    private LagRequest activeLagRequest;

    public FakeLag() {
        super("Fake Lag", Module.category.network);
        this.registerSetting(this.mode = new SliderSetting("Mode", 1, MODE_LABELS));
        this.registerSetting(this.packetDelaySlider = new SliderSetting("Packet delay", "ms", 0.0, 0.0, 1500.0, 20.0));
    }

    @Override
    public String getInfo() {
        return (int) this.packetDelaySlider.getInput() + "ms";
    }

    @Override
    public void guiUpdate() {
        if (this.isEnabled()) {
            if (this.packetDelaySlider.getInput() <= 0.0) {
                this.disable();
            } else {
                int m = (int) this.mode.getInput();
                long d = (long) this.packetDelaySlider.getInput();
                if (m != this.appliedMode || d != this.appliedDelayMs) {
                    this.appliedMode = m;
                    this.appliedDelayMs = d;
                    this.rebindLagRequest();
                }
            }
        }
    }

    private void rebindLagRequest() {
        if (this.activeLagRequest != null) {
            this.activeLagRequest.getTimeout().forceTimeOut();
        }
        this.activeLagRequest = new LagRequest(this.lagDirectionsForMode(), new ModuleBackedTimeout(this));
        Raven.lagHandler.requestLag(this.activeLagRequest);
    }

    private Set<EnumLagDirection> lagDirectionsForMode() {
        switch((int) this.mode.getInput()) {
            case 0:
                return EnumLagDirection.ONLY_INBOUND;
            case 1:
            default:
                return EnumLagDirection.ONLY_OUTBOUND;
            case 2:
                return EnumLagDirection.BIDIRECTIONAL;
        }
    }

    @Override
    public void onEnable() {
        if (mc.func_71356_B()) {
            Utils.sendMessage("&cFake lag cannot be enabled in singleplayer.");
            this.disable();
        } else if (ModuleManager.blink.isEnabled()) {
            Utils.sendMessage("&cCannot use fake lag with blink!");
            this.disable();
        } else {
            this.appliedMode = (int) this.mode.getInput();
            this.appliedDelayMs = (long) this.packetDelaySlider.getInput();
            this.rebindLagRequest();
        }
    }

    @Override
    public void onDisable() {
        if (this.activeLagRequest != null) {
            this.activeLagRequest.getTimeout().forceTimeOut();
            this.activeLagRequest = null;
        }
        this.appliedMode = -1;
        this.appliedDelayMs = -1L;
    }

    @SubscribeEvent
    public void onGameTick(GameTickEvent e) {
        if (this.isEnabled()) {
            if (Utils.nullCheck() && mc.field_71441_e != null) {
                long delayMs = (long) this.packetDelaySlider.getInput();
                if (delayMs > 0L) {
                    Set<EnumLagDirection> directions = this.lagDirectionsForMode();
                    if (directions.contains(EnumLagDirection.INBOUND)) {
                        Raven.lagHandler.releaseExpiredPackets(EnumLagDirection.INBOUND, delayMs);
                    }
                    if (directions.contains(EnumLagDirection.OUTBOUND)) {
                        Raven.lagHandler.releaseExpiredPackets(EnumLagDirection.OUTBOUND, delayMs);
                    }
                }
            } else {
                this.disable();
            }
        }
    }

    @SubscribeEvent
    public void onTick(ClientTickEvent e) {
        if (e.phase == Phase.END) {
            if (mc.field_71441_e == null && this.isEnabled()) {
                this.disable();
            }
        }
    }
}
