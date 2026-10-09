package keystrokesmod.module.impl.other;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import keystrokesmod.event.ReceivePacketEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.DescriptionSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.Utils;
import net.minecraft.event.HoverEvent.Action;
import net.minecraft.event.HoverEvent;
import net.minecraft.network.Packet;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class LatencyAlerts extends Module {

    private SliderSetting interval;

    private SliderSetting highLatency;

    private ButtonSetting ignoreLimbo;

    private long lastPacketTime = 0L;

    private long lastAlert = 0L;

    private Packet<?> lastPacket = null;

    public LatencyAlerts() {
        super("Latency Alerts", Module.category.other);
        this.registerSetting(new DescriptionSetting("Detects packet loss."));
        this.registerSetting(this.interval = new SliderSetting("Alert interval", " second", 3.0, 0.0, 5.0, 0.1));
        this.registerSetting(this.highLatency = new SliderSetting("High latency", " second", 0.5, 0.1, 5.0, 0.1));
        this.registerSetting(this.ignoreLimbo = new ButtonSetting("Ignore limbo", true));
        this.closetModule = true;
    }

    @Override
    public void onDisable() {
        this.lastPacketTime = 0L;
        this.lastAlert = 0L;
        this.lastPacket = null;
    }

    @SubscribeEvent
    public void onPacketReceive(ReceivePacketEvent e) {
        this.lastPacketTime = System.currentTimeMillis();
        this.lastPacket = e.getPacket();
    }

    @Override
    public void onUpdate() {
        if (!Utils.nullCheck()) {
            this.lastPacketTime = System.currentTimeMillis();
            this.lastAlert = System.currentTimeMillis();
            this.lastPacket = null;
        } else if (!mc.func_71356_B() && (!this.ignoreLimbo.isToggled() || !this.inLimbo())) {
            long currentMs = System.currentTimeMillis();
            if (currentMs - this.lastPacketTime >= this.highLatency.getInput() * 1000.0 && currentMs - this.lastAlert >= this.interval.getInput() * 1000.0) {
                String msSinceLastPacket = String.valueOf(Math.abs(System.currentTimeMillis() - this.lastPacketTime));
                ChatComponentText component = new ChatComponentText(Utils.formatColor("&7[&dR&7]&r &7Packet loss detected: "));
                ChatStyle style = new ChatStyle();
                String packetName = this.lastPacket == null ? "Unknown" : this.lastPacket.getClass().getSimpleName();
                String contents = "&7Last packet: &b" + packetName;
                String timeString = new SimpleDateFormat("h:mm:ss a").format(new Date(this.lastPacketTime));
                contents = contents + "\n&7Received at: &b" + timeString;
                style.func_150209_a(new HoverEvent(Action.SHOW_TEXT, new ChatComponentText(Utils.formatColor(contents))));
                component.func_150257_a(new ChatComponentText(Utils.formatColor("&c" + msSinceLastPacket)).func_150255_a(style));
                component.func_150257_a(new ChatComponentText(Utils.formatColor("&7ms")));
                mc.field_71439_g.func_145747_a(component);
                this.lastAlert = System.currentTimeMillis();
            }
        } else {
            this.lastPacketTime = System.currentTimeMillis();
            this.lastAlert = System.currentTimeMillis();
        }
    }

    @Override
    public void onEnable() {
        this.lastPacketTime = System.currentTimeMillis();
    }

    public boolean inLimbo() {
        if (!Utils.nullCheck()) {
            return false;
        } else {
            List<String> scoreboard = Utils.getSidebarLines();
            return scoreboard.isEmpty() ? mc.field_71441_e.field_73011_w.func_80007_l().equals("The End") : false;
        }
    }
}
