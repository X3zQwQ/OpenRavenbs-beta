package keystrokesmod.module.impl.combat;

import keystrokesmod.event.ReceivePacketEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.impl.movement.LongJump;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.DescriptionSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.Utils;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S27PacketExplosion;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Mouse;

public class AntiKnockback extends Module {

    private SliderSetting horizontal;

    private SliderSetting vertical;

    private ButtonSetting disableInLobby;

    private ButtonSetting cancelBurning;

    private ButtonSetting cancelExplosion;

    private ButtonSetting cancelWhileFalling;

    private ButtonSetting cancelOffGround;

    private SliderSetting boostMultiplier;

    private ButtonSetting boostWithLMB;

    public boolean disable;

    public AntiKnockback() {
        super("AntiKnockback", Module.category.combat);
        this.liteModule = true;
        this.registerSetting(new DescriptionSetting("Overrides Velocity."));
        this.registerSetting(this.horizontal = new SliderSetting("Horizontal", 0.0, 0.0, 100.0, 1.0));
        this.registerSetting(this.vertical = new SliderSetting("Vertical", 0.0, 0.0, 100.0, 1.0));
        this.registerSetting(this.disableInLobby = new ButtonSetting("Disable in lobby", false));
        this.registerSetting(this.cancelBurning = new ButtonSetting("Cancel burning", true));
        this.registerSetting(this.cancelExplosion = new ButtonSetting("Cancel explosion", true));
        this.registerSetting(this.cancelWhileFalling = new ButtonSetting("Cancel while falling", true));
        this.registerSetting(this.cancelOffGround = new ButtonSetting("Cancel off ground", true));
        this.registerSetting(this.boostMultiplier = new SliderSetting("Damage boost", "x", 1.0, 0.5, 2.5, 0.01));
        this.registerSetting(this.boostWithLMB = new ButtonSetting("Boost with LMB", false));
    }

    @SubscribeEvent
    public void onReceivePacket(ReceivePacketEvent e) {
        if (Utils.nullCheck() && !LongJump.stopVelocity && !e.isCanceled()) {
            if (e.getPacket() instanceof S12PacketEntityVelocity) {
                if (((S12PacketEntityVelocity) e.getPacket()).func_149412_c() == mc.field_71439_g.func_145782_y() && !this.disable) {
                    if (!this.cancelBurning.isToggled() && mc.field_71439_g.func_70027_ad()) {
                        return;
                    }
                    if (this.disableInLobby.isToggled() && Utils.isLobby()) {
                        return;
                    }
                    e.setCanceled(true);
                    if (this.cancel()) {
                        return;
                    }
                    if (this.cancelConditions()) {
                        return;
                    }
                    S12PacketEntityVelocity s12PacketEntityVelocity = (S12PacketEntityVelocity) e.getPacket();
                    if (this.horizontal.getInput() == 0.0 && this.vertical.getInput() > 0.0) {
                        mc.field_71439_g.field_70181_x = s12PacketEntityVelocity.func_149410_e() / 8000.0 * this.vertical.getInput() / 100.0;
                    } else if (this.horizontal.getInput() > 0.0 && this.vertical.getInput() == 0.0) {
                        mc.field_71439_g.field_70159_w = s12PacketEntityVelocity.func_149411_d() / 8000.0 * this.horizontal.getInput() / 100.0;
                        mc.field_71439_g.field_70179_y = s12PacketEntityVelocity.func_149409_f() / 8000.0 * this.horizontal.getInput() / 100.0;
                    } else {
                        mc.field_71439_g.field_70159_w = s12PacketEntityVelocity.func_149411_d() / 8000.0 * this.horizontal.getInput() / 100.0;
                        mc.field_71439_g.field_70181_x = s12PacketEntityVelocity.func_149410_e() / 8000.0 * this.vertical.getInput() / 100.0;
                        mc.field_71439_g.field_70179_y = s12PacketEntityVelocity.func_149409_f() / 8000.0 * this.horizontal.getInput() / 100.0;
                    }
                    if (this.boostMultiplier.getInput() != 1.0) {
                        if (this.boostWithLMB.isToggled() && !Mouse.isButtonDown(0)) {
                            return;
                        }
                        Utils.setSpeed(Utils.getHorizontalSpeed() * this.boostMultiplier.getInput());
                    }
                }
            } else if (e.getPacket() instanceof S27PacketExplosion && !this.disable) {
                if (this.disableInLobby.isToggled() && Utils.isLobby()) {
                    return;
                }
                e.setCanceled(true);
                if (this.cancelExplosion.isToggled() || this.cancel()) {
                    return;
                }
                if (this.cancelConditions()) {
                    return;
                }
                S27PacketExplosion s27PacketExplosion = (S27PacketExplosion) e.getPacket();
                if (this.horizontal.getInput() == 0.0 && this.vertical.getInput() > 0.0) {
                    mc.field_71439_g.field_70181_x = mc.field_71439_g.field_70181_x + s27PacketExplosion.func_149144_d() * this.vertical.getInput() / 100.0;
                } else if (this.horizontal.getInput() > 0.0 && this.vertical.getInput() == 0.0) {
                    mc.field_71439_g.field_70159_w = mc.field_71439_g.field_70159_w + s27PacketExplosion.func_149149_c() * this.horizontal.getInput() / 100.0;
                    mc.field_71439_g.field_70179_y = mc.field_71439_g.field_70179_y + s27PacketExplosion.func_149147_e() * this.horizontal.getInput() / 100.0;
                } else {
                    mc.field_71439_g.field_70159_w = mc.field_71439_g.field_70159_w + s27PacketExplosion.func_149149_c() * this.horizontal.getInput() / 100.0;
                    mc.field_71439_g.field_70181_x = mc.field_71439_g.field_70181_x + s27PacketExplosion.func_149144_d() * this.vertical.getInput() / 100.0;
                    mc.field_71439_g.field_70179_y = mc.field_71439_g.field_70179_y + s27PacketExplosion.func_149147_e() * this.horizontal.getInput() / 100.0;
                }
            }
        }
    }

    private boolean cancel() {
        return this.vertical.getInput() == 0.0 && this.horizontal.getInput() == 0.0;
    }

    @Override
    public String getInfo() {
        return (int) this.horizontal.getInput() + "% " + (int) this.vertical.getInput() + "%";
    }

    private boolean cancelConditions() {
        if (mc.field_71439_g != null) {
            if (this.cancelWhileFalling.isToggled() && mc.field_71439_g.field_70143_R > 0.0F) {
                return true;
            }
            if (this.cancelOffGround.isToggled() && !mc.field_71439_g.field_70122_E) {
                return true;
            }
        }
        return false;
    }
}
