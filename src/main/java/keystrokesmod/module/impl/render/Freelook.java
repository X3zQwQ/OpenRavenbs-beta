package keystrokesmod.module.impl.render;

import keystrokesmod.mixin.impl.accessor.IAccessorMouseHelper;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.player.Freecam;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.KeySetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.event.world.WorldEvent.Load;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.RenderTickEvent;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;

public class Freelook extends Module {

    public static boolean perspectiveToggled;

    public static float cameraYaw;

    public static float cameraPitch;

    private ButtonSetting hold;

    private ButtonSetting invertPitch;

    private ButtonSetting lockPitch;

    private ButtonSetting customFov;

    private SliderSetting fov;

    private KeySetting freelookKey;

    private boolean prevKeyState;

    private int previousPerspective;

    private float lastFov;

    public Freelook() {
        super("Free Look", Module.category.render);
        this.registerSetting(this.freelookKey = new KeySetting("Key", 56));
        this.registerSetting(this.hold = new ButtonSetting("Hold", true));
        this.registerSetting(this.invertPitch = new ButtonSetting("Invert pitch", false));
        this.registerSetting(this.lockPitch = new ButtonSetting("Lock pitch", true));
        this.registerSetting(this.customFov = new ButtonSetting("Custom FOV", false));
        this.registerSetting(this.fov = new SliderSetting("FOV", 90.0, 10.0, 150.0, 1.0));
    }

    @Override
    public void guiUpdate() {
        this.fov.setVisible(this.customFov.isToggled(), this);
    }

    @SubscribeEvent
    public void onRenderTick(RenderTickEvent e) {
        if (e.phase == Phase.END && mc.field_71462_r == null && Utils.nullCheck()) {
            Module freecamMod = ModuleManager.getModule(Freecam.class);
            if (!(freecamMod instanceof Freecam) || Freecam.freeEntity == null) {
                boolean down = this.freelookKey.isPressed();
                if (down != this.prevKeyState) {
                    this.onPressed(down);
                    this.prevKeyState = down;
                }
            }
        }
    }

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        if (event.gui != null && perspectiveToggled && this.hold.isToggled()) {
            this.resetPerspective();
        }
    }

    @SubscribeEvent
    public void onWorldLoad(Load e) {
        if (perspectiveToggled) {
            this.resetPerspective();
        }
    }

    private void onPressed(boolean state) {
        if (!this.isEnabled()) {
            if (perspectiveToggled) {
                this.resetPerspective();
            }
        } else {
            if (state) {
                cameraYaw = mc.field_71439_g.field_70177_z;
                cameraPitch = mc.field_71439_g.field_70125_A;
                if (perspectiveToggled) {
                    this.resetPerspective();
                } else {
                    this.enterPerspective();
                }
            } else if (this.hold.isToggled()) {
                this.resetPerspective();
            }
        }
    }

    private void enterPerspective() {
        perspectiveToggled = true;
        this.previousPerspective = mc.field_71474_y.field_74320_O;
        this.applyThirdPersonView(1);
        this.lastFov = mc.field_71474_y.field_74334_X;
    }

    public void resetPerspective() {
        perspectiveToggled = false;
        this.applyThirdPersonView(this.previousPerspective);
        if (mc.field_71462_r == null && mc.field_71415_G && !Mouse.isGrabbed()) {
            mc.field_71417_B.func_74372_a();
        }
        if (this.hold.isToggled() || mc.field_71474_y.field_74334_X == this.lastFov || this.customFov.isToggled()) {
            mc.field_71474_y.field_74334_X = this.lastFov;
        }
    }

    public static boolean overrideMouse(Minecraft mc) {
        if (!mc.field_71415_G) {
            return false;
        } else if (ModuleManager.freelook != null && ModuleManager.freelook.isEnabled() && perspectiveToggled) {
            if (Minecraft.field_142025_a && Display.isActive() && !Mouse.isInsideWindow()) {
                Mouse.setGrabbed(false);
                Mouse.setCursorPosition(mc.field_71443_c / 2, mc.field_71440_d / 2 - 20);
                Mouse.setGrabbed(true);
            }
            mc.field_71417_B.func_74374_c();
            float sens = mc.field_71474_y.field_74341_c * 0.6F + 0.2F;
            float mult = sens * sens * sens * 8.0F;
            Freelook fl = ModuleManager.freelook;
            if (fl != null) {
                int dx = ((IAccessorMouseHelper) mc.field_71417_B).getDeltaX();
                int dy = ((IAccessorMouseHelper) mc.field_71417_B).getDeltaY();
                float floatValue = dx * mult;
                float fdy = dy * mult;
                cameraYaw += floatValue * 0.15F;
                if (fl.invertPitch.isToggled()) {
                    fdy = -fdy;
                }
                cameraPitch += fdy * 0.15F;
                if (fl.lockPitch.isToggled()) {
                    cameraPitch = Math.max(-90.0F, Math.min(90.0F, cameraPitch));
                }
                if (fl.customFov.isToggled()) {
                    mc.field_71474_y.field_74334_X = (float) fl.fov.getInput();
                }
            }
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void onDisable() {
        if (perspectiveToggled) {
            perspectiveToggled = false;
            this.applyThirdPersonView(0);
            if (mc.field_71462_r == null && mc.field_71415_G && !Mouse.isGrabbed()) {
                mc.field_71417_B.func_74372_a();
            }
            mc.field_71474_y.field_74334_X = this.lastFov;
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
