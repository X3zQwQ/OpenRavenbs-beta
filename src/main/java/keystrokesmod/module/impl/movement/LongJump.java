package keystrokesmod.module.impl.movement;

import java.util.Iterator;
import keystrokesmod.event.CollisionEvent;
import keystrokesmod.event.PreMotionEvent;
import keystrokesmod.event.PrePlayerInputEvent;
import keystrokesmod.event.PreUpdateEvent;
import keystrokesmod.event.ReceivePacketEvent;
import keystrokesmod.mixin.interfaces.IMixinItemRenderer;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.KeySetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.ModuleUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.network.play.server.S0EPacketSpawnObject;
import net.minecraft.network.play.server.S27PacketExplosion;
import net.minecraft.network.play.server.S29PacketSoundEffect;
import net.minecraft.network.play.server.S2APacketParticles;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class LongJump extends Module {

    private SliderSetting mode;

    private SliderSetting boostSetting;

    private SliderSetting verticalMotion;

    private SliderSetting motionDecay;

    private ButtonSetting manual;

    private ButtonSetting onlyWithVelocity;

    private KeySetting disableKey;

    private ButtonSetting allowStrafe;

    private ButtonSetting invertYaw;

    private ButtonSetting stopMovement;

    private ButtonSetting hideExplosion;

    public ButtonSetting spoofItem;

    private KeySetting temporaryFlightKey;

    public String[] modes = new String[] { "Float", "Boost" };

    private boolean manualWasOn;

    private float yaw;

    private float pitch;

    private boolean notMoving;

    private boolean enabled;

    public static boolean function;

    private Double startY;

    private int boostTicks;

    public int lastSlot = -1;

    private int stopTime;

    private int rotateTick;

    private int motionDecayVal;

    private long fireballTime;

    private long MAX_EXPLOSION_DIST_SQ = 9L;

    private long FIREBALL_TIMEOUT = 750L;

    public static boolean stopVelocity;

    public static boolean stopModules;

    public static boolean slotReset;

    public static int slotResetTicks;

    private int firstSlot = -1;

    public LongJump() {
        super("Long Jump", Module.category.movement);
        this.registerSetting(this.mode = new SliderSetting("Mode", 0, this.modes));
        this.registerSetting(this.manual = new ButtonSetting("Manual", false));
        this.registerSetting(this.onlyWithVelocity = new ButtonSetting("Only while velocity enabled", false));
        this.registerSetting(this.disableKey = new KeySetting("Disable key", 57));
        this.registerSetting(this.boostSetting = new SliderSetting("Horizontal boost", 1.7, 0.0, 2.0, 0.05));
        this.registerSetting(this.verticalMotion = new SliderSetting("Vertical motion", 0.0, 0.4, 0.9, 0.01));
        this.registerSetting(this.motionDecay = new SliderSetting("Motion decay", 17.0, 1.0, 40.0, 1.0));
        this.registerSetting(this.allowStrafe = new ButtonSetting("Allow strafe", false));
        this.registerSetting(this.invertYaw = new ButtonSetting("Invert yaw", true));
        this.registerSetting(this.stopMovement = new ButtonSetting("Stop movement", false));
        this.registerSetting(this.hideExplosion = new ButtonSetting("Hide explosion", false));
        this.registerSetting(this.spoofItem = new ButtonSetting("Spoof item", false));
        this.registerSetting(this.temporaryFlightKey = new KeySetting("Vertical key", 57));
    }

    @Override
    public void guiUpdate() {
        this.onlyWithVelocity.setVisible(this.manual.isToggled(), this);
        this.disableKey.setVisible(this.manual.isToggled(), this);
        this.verticalMotion.setVisible(this.mode.getInput() == 0.0, this);
        this.motionDecay.setVisible(this.mode.getInput() == 0.0, this);
        this.temporaryFlightKey.setVisible(this.mode.getInput() == 0.0, this);
    }

    @Override
    public void onEnable() {
        if (!this.manual.isToggled()) {
            if (Utils.getTotalHealth(mc.field_71439_g) <= 3.0F) {
                Utils.sendMessage("&cPrevented throwing fireball due to low health");
                this.disable();
                return;
            }
            this.enabled();
        }
    }

    @Override
    public void onDisable() {
        this.disabled();
    }

    @SubscribeEvent
    public void onCollision(CollisionEvent event) {
        if (this.startY != null && !this.manual.isToggled() && this.boostTicks <= 0) {
            BlockPos blockPos = event.blockPos;
            if (event.block.func_176205_b(mc.field_71441_e, blockPos) && blockPos.func_177956_o() <= this.startY) {
                event.boundingBox = new AxisAlignedBB(blockPos.func_177958_n() - 2.0, blockPos.func_177956_o() - 1.0, blockPos.func_177952_p() - 2.0, blockPos.func_177958_n() + 2.0, blockPos.func_177956_o() + 1.0, blockPos.func_177952_p() + 2.0);
            }
        }
    }

    @SubscribeEvent
    public void onPreUpdate(PreUpdateEvent e) {
        if (this.manual.isToggled()) {
            this.manualWasOn = true;
        } else {
            if (this.manualWasOn) {
                this.disabled();
            }
            this.manualWasOn = false;
        }
        if (this.manual.isToggled() && this.disableKey.isPressed() && Utils.jumpDown()) {
            function = false;
            this.disabled();
        }
        if (!function) {
            if (this.manual.isToggled() && !this.enabled && ModuleUtils.threwFireballLow) {
                ModuleManager.velocity.disable = true;
                ModuleManager.antiKnockback.disable = true;
                this.enabled();
            }
        } else {
            if (this.spoofItem.isToggled()) {
                ((IMixinItemRenderer) mc.func_175597_ag()).setCancelUpdate(true);
                ((IMixinItemRenderer) mc.func_175597_ag()).setCancelReset(true);
            }
            if (this.enabled) {
                if (!Utils.isMoving()) {
                    this.notMoving = true;
                }
                if (this.boostSetting.getInput() == 0.0 && this.verticalMotion.getInput() == 0.0) {
                    Utils.sendMessage("&cValues are set to 0!");
                    this.disabled();
                    return;
                }
                int fireballSlot = this.setupFireballSlot(true);
                if (fireballSlot != -1) {
                    if (!this.manual.isToggled()) {
                        this.lastSlot = mc.field_71439_g.field_71071_by.field_70461_c;
                        if (mc.field_71439_g.field_71071_by.field_70461_c != fireballSlot) {
                            mc.field_71439_g.field_71071_by.field_70461_c = fireballSlot;
                        }
                    }
                    this.rotateTick = 1;
                    if (this.stopMovement.isToggled()) {
                        this.stopTime = 1;
                    }
                }
                this.enabled = false;
            }
            if (this.notMoving) {
                this.motionDecayVal = 21;
            } else {
                this.motionDecayVal = (int) this.motionDecay.getInput();
            }
            if (this.stopTime == -1 && ++this.boostTicks > (!this.temporaryFlightKey() ? 33 : (!this.notMoving ? 32 : 33))) {
                this.disabled();
            } else if (this.fireballTime > 0L && System.currentTimeMillis() - this.fireballTime > this.FIREBALL_TIMEOUT) {
                Utils.sendMessage("&cFireball timed out.");
                this.disabled();
            } else {
                if (this.boostTicks > 0) {
                    if (this.mode.getInput() == 0.0) {
                        this.modifyVertical();
                    }
                    if (this.allowStrafe.isToggled() && this.boostTicks < 32) {
                        Utils.setSpeed(Utils.getHorizontalSpeed(mc.field_71439_g));
                    }
                }
                if (this.stopMovement.isToggled() && !this.notMoving && this.stopTime > 0) {
                    this.stopTime++;
                }
                if (mc.field_71439_g.field_70122_E && this.boostTicks > 2) {
                    this.disabled();
                }
                if (this.firstSlot != -1) {
                    mc.field_71439_g.field_71071_by.field_70461_c = this.firstSlot;
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPreMotion(PreMotionEvent e) {
        if (Utils.nullCheck()) {
            if (this.rotateTick == 1) {
                if ((this.invertYaw.isToggled() || this.stopMovement.isToggled()) && !this.notMoving) {
                    if (!this.stopMovement.isToggled()) {
                        this.yaw = mc.field_71439_g.field_70177_z - 180.0F;
                        this.pitch = 90.0F;
                    } else {
                        this.yaw = mc.field_71439_g.field_70177_z - 180.0F;
                        this.pitch = 66.3F;
                    }
                } else {
                    this.yaw = mc.field_71439_g.field_70177_z;
                    this.pitch = 90.0F;
                }
                e.setRotations(this.yaw, this.pitch);
            }
            if (this.rotateTick > 0 && ++this.rotateTick >= 3) {
                this.rotateTick = 0;
                int fireballSlot = this.setupFireballSlot(false);
                if (fireballSlot != -1) {
                    this.fireballTime = System.currentTimeMillis();
                    if (!this.manual.isToggled()) {
                        mc.func_147114_u().func_147297_a(new C08PacketPlayerBlockPlacement(mc.field_71439_g.func_70694_bm()));
                    }
                    mc.field_71439_g.func_71038_i();
                    mc.func_175597_ag().func_78444_b();
                    stopVelocity = true;
                }
            }
            if (this.boostTicks == 1) {
                if (this.invertYaw.isToggled()) {
                }
                this.modifyHorizontal();
                stopVelocity = false;
                if (!this.manual.isToggled() && !this.allowStrafe.isToggled() && this.mode.getInput() == 1.0) {
                    this.disabled();
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onMoveInput(PrePlayerInputEvent e) {
        if (function) {
            mc.field_71439_g.field_71158_b.field_78901_c = false;
            if (this.rotateTick > 0 || this.fireballTime > 0L) {
                if (Utils.isMoving()) {
                    e.setForward(1.0F);
                }
                e.setStrafe(0.0F);
            }
            if (this.notMoving && this.boostTicks < 3) {
                e.setForward(0.0F);
                e.setStrafe(0.0F);
                Utils.setSpeed(0.0);
            }
            if (this.stopMovement.isToggled() && !this.notMoving && this.stopTime >= 1) {
                e.setForward(0.0F);
                e.setStrafe(0.0F);
                Utils.setSpeed(0.0);
            }
        }
    }

    @SubscribeEvent
    public void onReceivePacket(ReceivePacketEvent e) {
        if (function) {
            Packet packet = e.getPacket();
            if (packet instanceof S27PacketExplosion) {
                S27PacketExplosion s27 = (S27PacketExplosion) packet;
                if (this.fireballTime == 0L || mc.field_71439_g.func_180425_c().func_177954_c(s27.func_149148_f(), s27.func_149143_g(), s27.func_149145_h()) > this.MAX_EXPLOSION_DIST_SQ) {
                    e.setCanceled(true);
                }
                this.stopTime = -1;
                this.fireballTime = 0L;
                this.resetSlot();
                this.boostTicks = 0;
            } else if (packet instanceof S08PacketPlayerPosLook) {
                Utils.sendMessage("&cReceived setback, disabling.");
                this.disabled();
            }
            if (this.hideExplosion.isToggled() && this.fireballTime != 0L && (packet instanceof S0EPacketSpawnObject || packet instanceof S2APacketParticles || packet instanceof S29PacketSoundEffect)) {
                e.setCanceled(true);
            }
        }
    }

    private int getFireballSlot() {
        int n = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack getStackInSlot = mc.field_71439_g.field_71071_by.func_70301_a(i);
            if (getStackInSlot != null && getStackInSlot.func_77973_b() == Items.field_151059_bz) {
                n = i;
                break;
            }
        }
        return n;
    }

    private void enabled() {
        slotReset = false;
        slotResetTicks = 0;
        function = true;
        this.enabled = true;
        stopModules = true;
        this.startY = mc.field_71439_g.field_70163_u;
    }

    private void disabled() {
        this.fireballTime = this.rotateTick = this.stopTime = 0;
        this.boostTicks = -1;
        this.resetSlot();
        stopModules = false;
        stopVelocity = false;
        this.enabled = function = this.notMoving = false;
        if (!this.manual.isToggled()) {
            this.disable();
        }
        this.startY = null;
    }

    private int setupFireballSlot(boolean pre) {
        int fireballSlot = this.getFireballSlot();
        if (fireballSlot == -1) {
            Utils.sendMessage("&cFireball not found.");
            this.disabled();
        } else if (pre && Utils.distanceToGround(mc.field_71439_g) > 3.0) {
            Utils.sendMessage("&cCan't throw fireball right now.");
            this.disabled();
            fireballSlot = -1;
        }
        return fireballSlot;
    }

    private void resetSlot() {
        if (this.lastSlot != -1 && !this.manual.isToggled()) {
            if (this.spoofItem.isToggled()) {
                ((IMixinItemRenderer) mc.func_175597_ag()).setCancelUpdate(false);
                ((IMixinItemRenderer) mc.func_175597_ag()).setCancelReset(false);
            }
            mc.field_71439_g.field_71071_by.field_70461_c = this.lastSlot;
            this.lastSlot = -1;
            this.firstSlot = -1;
        }
        slotReset = true;
    }

    private int getSpeedLevel() {
        Iterator iterator2 = mc.field_71439_g.func_70651_bq().iterator();
        if (iterator2.hasNext()) {
            PotionEffect potionEffect = (PotionEffect) iterator2.next();
            return potionEffect.func_76453_d().equals("potion.moveSpeed") ? potionEffect.func_76458_c() + 1 : 0;
        } else {
            return 0;
        }
    }

    void modifyHorizontal() {
        if (this.boostSetting.getInput() != 0.0) {
            double speed = this.boostSetting.getInput() - Utils.randomizeDouble(1.0E-4, 0.0);
            if (Utils.isMoving()) {
                Utils.setSpeed(speed);
            }
        }
    }

    private void modifyVertical() {
        if (this.verticalMotion.getInput() != 0.0) {
            double ver = (!this.notMoving ? this.verticalMotion.getInput() : 1.16) * (1.0 / (1.0 + 0.05 * this.getSpeedLevel())) + Utils.randomizeDouble(1.0E-4, 0.1);
            double decay = this.motionDecay.getInput() / 1000.0;
            if (this.boostTicks > 1 && !this.temporaryFlightKey()) {
                if (this.boostTicks > 1 || this.boostTicks <= (!this.notMoving ? 32 : 33)) {
                    mc.field_71439_g.field_70181_x = Utils.randomizeDouble(0.0101, 0.01);
                }
            } else if (this.boostTicks >= 1 && this.boostTicks <= (!this.notMoving ? 32 : 33)) {
                mc.field_71439_g.field_70181_x = ver - this.boostTicks * decay;
            } else if (this.boostTicks >= (!this.notMoving ? 32 : 33) + 3) {
                mc.field_71439_g.field_70181_x += 0.028;
                Utils.sendMessage("?");
            }
        }
    }

    private boolean temporaryFlightKey() {
        return this.notMoving ? true : this.temporaryFlightKey.isPressed();
    }
}
