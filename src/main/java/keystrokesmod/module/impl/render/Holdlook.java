package keystrokesmod.module.impl.render;

import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.KeySetting;
import keystrokesmod.utility.Utils;
import net.minecraft.entity.Entity;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;

public class Holdlook extends Module {

    private final KeySetting rearCamKey;

    private final KeySetting frontCamKey;

    private boolean rearActive;

    private boolean frontActive;

    private int savedPerspective;

    public Holdlook() {
        super("Hold Look", Module.category.render);
        this.registerSetting(this.rearCamKey = new KeySetting("Rear cam", 0));
        this.registerSetting(this.frontCamKey = new KeySetting("Front cam", 0));
    }

    @SubscribeEvent
    public void onClientTick(ClientTickEvent e) {
        if (e.phase == Phase.START && Utils.nullCheck()) {
            if (mc.field_71462_r != null) {
                if (this.rearActive || this.frontActive) {
                    this.applyThirdPersonView(0);
                    this.rearActive = false;
                    this.frontActive = false;
                }
            } else {
                boolean rearDown = this.rearCamKey.isPressed();
                boolean frontDown = this.frontCamKey.isPressed();
                if (rearDown && !this.rearActive) {
                    this.savedPerspective = mc.field_71474_y.field_74320_O;
                    this.applyThirdPersonView(1);
                    this.rearActive = true;
                } else if (!rearDown && this.rearActive) {
                    this.applyThirdPersonView(this.frontActive ? 2 : 0);
                    this.rearActive = false;
                }
                if (frontDown && !this.frontActive) {
                    if (!this.rearActive) {
                        this.savedPerspective = mc.field_71474_y.field_74320_O;
                    }
                    this.applyThirdPersonView(2);
                    this.frontActive = true;
                } else if (!frontDown && this.frontActive) {
                    this.applyThirdPersonView(this.rearActive ? 1 : 0);
                    this.frontActive = false;
                }
            }
        }
    }

    @Override
    public void onDisable() {
        if (this.rearActive || this.frontActive) {
            this.applyThirdPersonView(0);
            this.rearActive = false;
            this.frontActive = false;
        }
    }

    private void applyThirdPersonView(int view) {
        if (view < 0) {
            view = 0;
        } else if (view > 2) {
            view = 2;
        }
        mc.field_71474_y.field_74320_O = view;
        if (mc.field_71460_t != null) {
            if (view == 0) {
                mc.field_71460_t.func_175066_a(mc.func_175606_aa());
            } else if (view == 1) {
                mc.field_71460_t.func_175066_a((Entity) null);
            }
        }
        if (mc.field_71438_f != null) {
            mc.field_71438_f.func_174979_m();
        }
    }
}
