package keystrokesmod.module.impl.combat;

import keystrokesmod.Raven;
import keystrokesmod.event.GameTickEvent;
import keystrokesmod.event.ReceivePacketEvent;
import keystrokesmod.lag.api.EnumLagDirection;
import keystrokesmod.lag.api.LagRequest;
import keystrokesmod.lag.timeout.ModuleBackedTimeout;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.network.Blink;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.DescriptionSetting;
import keystrokesmod.module.setting.impl.ItemListSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.CombatTargeting;
import keystrokesmod.utility.Utils;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Mouse;

public class KnockbackDelay extends Module {

    private final SliderSetting distanceToTarget;

    private final SliderSetting chance;

    private final SliderSetting maximumDelay;

    private final ButtonSetting inAir;

    private final ButtonSetting lookingAtPlayer;

    private final ButtonSetting requireLeftMouse;

    private final ButtonSetting onlyWhitelistedItem;

    private final ItemListSetting whitelistedItems;

    private LagRequest inboundLagRequest;

    public KnockbackDelay() {
        super("Knockback Delay", Module.category.combat);
        this.registerSetting(this.distanceToTarget = new SliderSetting("Distance to target", 6.0, 3.0, 12.0, 0.1));
        this.registerSetting(this.chance = new SliderSetting("Chance", "%", 100.0, 0.0, 100.0, 1.0));
        this.registerSetting(this.maximumDelay = new SliderSetting("Maximum delay", "ms", 200.0, 50.0, 1000.0, 10.0));
        this.registerSetting(new DescriptionSetting("Conditions"));
        this.registerSetting(this.inAir = new ButtonSetting("In air", true));
        this.registerSetting(this.lookingAtPlayer = new ButtonSetting("Looking at player", false));
        this.registerSetting(this.requireLeftMouse = new ButtonSetting("Require Left mouse", false, "Require mouse down"));
        this.registerSetting(this.onlyWhitelistedItem = new ButtonSetting("Restrict held item", false, "Item whitelist", "Restrict to listed items"));
        this.registerSetting(this.whitelistedItems = new ItemListSetting("Whitelisted items"));
        this.closetModule = true;
    }

    @Override
    public void guiUpdate() {
        this.whitelistedItems.setVisible(this.onlyWhitelistedItem.isToggled(), this);
    }

    @Override
    public void onEnable() {
        if (blinksInbound()) {
            Utils.sendMessage("&cKnockback Delay conflicts with Blink inbound / both. Disable Blink or use outbound-only.");
            this.disable();
        } else {
            this.inboundLagRequest = null;
        }
    }

    @Override
    public void onDisable() {
        this.flushInboundLagAndClear();
    }

    @Override
    public String getInfo() {
        return (int) this.maximumDelay.getInput() + "ms";
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onReceivePacketHigh(ReceivePacketEvent e) {
        if (this.isEnabled() && !e.isCanceled()) {
            if (e.getPacket() instanceof S08PacketPlayerPosLook) {
                this.flushInboundLagAndClear();
            } else if (e.getPacket() instanceof S12PacketEntityVelocity) {
                if (Utils.nullCheck() && mc.field_71439_g != null && mc.field_71441_e != null) {
                    S12PacketEntityVelocity packet = (S12PacketEntityVelocity) e.getPacket();
                    if (packet.func_149412_c() == mc.field_71439_g.func_145782_y()) {
                        if (this.conditionsFailureReason() == null) {
                            if (!(this.chance.getInput() < 100.0) || !(Math.random() * 100.0 >= this.chance.getInput())) {
                                if (!this.isInboundSessionActive()) {
                                    this.inboundLagRequest = new LagRequest(EnumLagDirection.ONLY_INBOUND, new ModuleBackedTimeout(this));
                                    Raven.lagHandler.requestLag(this.inboundLagRequest);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onGameTick(GameTickEvent e) {
        if (this.isEnabled()) {
            if (!Utils.nullCheck() || mc.field_71439_g == null || mc.field_71441_e == null || mc.field_71439_g.field_70128_L) {
                this.flushInboundLagAndClear();
            } else if (this.isInboundSessionActive()) {
                if (this.conditionsFailureReason() != null) {
                    this.flushInboundLagAndClear();
                } else {
                    Raven.lagHandler.releaseExpiredPackets(EnumLagDirection.INBOUND, (long) this.maximumDelay.getInput());
                }
            }
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

    private String conditionsFailureReason() {
        double maxSq = this.distanceToTarget.getInput() * this.distanceToTarget.getInput();
        if (CombatTargeting.findTarget(maxSq) == null) {
            return "no target in range";
        } else if (this.inAir.isToggled() && mc.field_71439_g.field_70122_E) {
            return "not in air";
        } else if (this.lookingAtPlayer.isToggled() && CombatTargeting.getMouseOverTarget(maxSq) == null) {
            return "not looking at player";
        } else if (this.requireLeftMouse.isToggled() && !Mouse.isButtonDown(0)) {
            return "LMB not held";
        } else {
            if (this.onlyWhitelistedItem.isToggled()) {
                ItemStack held = mc.field_71439_g.func_70694_bm();
                if (held == null || !this.whitelistedItems.matches(held)) {
                    return "held item not whitelisted";
                }
            }
            return null;
        }
    }

    private static boolean blinksInbound() {
        Blink blink = ModuleManager.blink;
        return blink != null && blink.isEnabled() && blink.delaysInboundPackets();
    }
}
