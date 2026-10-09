package keystrokesmod.module.impl.combat;

import java.lang.reflect.Field;
import java.util.Random;
import keystrokesmod.event.PrePlayerInteractEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.DescriptionSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.ReflectionUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.Slot;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.RenderTickEvent;
import org.lwjgl.input.Mouse;

public class AutoClicker extends Module {

    public SliderSetting targetCPS;

    public ButtonSetting simulateExhaust;

    public ButtonSetting notUsingItem;

    public ButtonSetting breakBlocks;

    public ButtonSetting weaponOnly;

    public ButtonSetting disableCreative;

    public ButtonSetting inventory;

    public SliderSetting inventoryStartDelay;

    private long nextClickTime;

    private long inventoryNextClickTime;

    private boolean isControllingAttack;

    private boolean displaceRecoveryClickRequested;

    private Random rand;

    private static Field hoveredSlotField;

    public AutoClicker() {
        super("Auto Clicker", Module.category.combat, 0);
        this.registerSetting(new DescriptionSetting("Best with delay remover."));
        this.registerSetting(this.targetCPS = new SliderSetting("Target CPS", 10.0, 1.0, 20.0, 0.5));
        this.registerSetting(this.simulateExhaust = new ButtonSetting("Simulate exhaust", true));
        this.registerSetting(this.notUsingItem = new ButtonSetting("Not using item", false));
        this.registerSetting(this.breakBlocks = new ButtonSetting("Break blocks", false));
        this.registerSetting(this.weaponOnly = new ButtonSetting("Weapon only", false));
        this.registerSetting(this.disableCreative = new ButtonSetting("Disable in creative", false));
        this.registerSetting(this.inventory = new ButtonSetting("Inventory", false));
        this.registerSetting(this.inventoryStartDelay = new SliderSetting("Start delay", "ms", 100.0, 0.0, 250.0, 10.0));
        this.closetModule = true;
        this.liteModule = true;
    }

    @Override
    public String getInfo() {
        double cps = this.targetCPS.getInput();
        return cps == Math.rint(cps) ? Integer.toString((int) cps) : Double.toString(Utils.round(cps, 1));
    }

    @Override
    public void onEnable() {
        this.rand = new Random();
        this.nextClickTime = 0L;
        this.inventoryNextClickTime = 0L;
        this.isControllingAttack = false;
        this.displaceRecoveryClickRequested = false;
        ensureHoveredSlotField();
    }

    @Override
    public void onDisable() {
        this.releaseAttackControl();
        this.nextClickTime = 0L;
        this.inventoryNextClickTime = 0L;
        this.isControllingAttack = false;
        this.displaceRecoveryClickRequested = false;
    }

    private boolean consumeDisplaceRecoveryClickRequest() {
        boolean requested = this.displaceRecoveryClickRequested;
        this.displaceRecoveryClickRequested = false;
        return requested;
    }

    @SubscribeEvent
    public void onRenderTick(RenderTickEvent e) {
        if (e.phase == Phase.END) {
            if (this.inventory.isToggled()) {
                if (Utils.nullCheck()) {
                    if (!(mc.field_71462_r instanceof GuiContainer)) {
                        this.inventoryNextClickTime = 0L;
                    } else if (!Mouse.isButtonDown(0)) {
                        this.inventoryNextClickTime = 0L;
                    } else {
                        ensureHoveredSlotField();
                        if (hoveredSlotField != null) {
                            long now = System.currentTimeMillis();
                            if (this.inventoryNextClickTime == 0L) {
                                this.inventoryNextClickTime = now + (long) this.inventoryStartDelay.getInput();
                            }
                            int clicks;
                            for (clicks = 0; this.inventoryNextClickTime <= now; this.inventoryNextClickTime = this.inventoryNextClickTime + this.nextDelay()) {
                                clicks++;
                            }
                            if (clicks > 0) {
                                GuiContainer gui = (GuiContainer) mc.field_71462_r;
                                Slot slot = getHoveredSlot(gui);
                                if (slot != null && slot.field_75222_d >= 0) {
                                    int windowId = gui.field_147002_h.field_75152_c;
                                    int slotId = slot.field_75222_d;
                                    int mode = GuiScreen.func_146272_n() ? 1 : 0;
                                    if (mc.field_71442_b != null && mc.field_71439_g != null) {
                                        for (int i = 0; i < clicks; i++) {
                                            mc.field_71442_b.func_78753_a(windowId, slotId, 0, mode, mc.field_71439_g);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onPrePlayerInteract(PrePlayerInteractEvent e) {
        boolean recoveryClickRequested = this.consumeDisplaceRecoveryClickRequest();
        if (Utils.nullCheck() && (ModuleManager.killAura == null || !ModuleManager.killAura.isEnabled() || KillAura.target == null) && (!this.notUsingItem.isToggled() || !mc.field_71439_g.func_71039_bw()) && (!this.disableCreative.isToggled() || !mc.field_71439_g.field_71075_bZ.field_75098_d) && mc.field_71462_r == null && mc.field_71415_G && (!this.weaponOnly.isToggled() || Utils.holdingWeapon())) {
            int key = mc.field_71474_y.field_74312_F.func_151463_i();
            if (Mouse.isButtonDown(0)) {
                this.isControllingAttack = true;
                if (this.breakBlocks.isToggled() && mc.field_71439_g.field_71075_bZ.field_75099_e && mc.field_71476_x != null && mc.field_71476_x.field_72313_a == MovingObjectType.BLOCK) {
                    BlockPos pos = mc.field_71476_x.func_178782_a();
                    Block block = mc.field_71441_e.func_180495_p(pos).func_177230_c();
                    if (block != Blocks.field_150350_a && !(block instanceof BlockLiquid)) {
                        KeyBinding.func_74510_a(key, true);
                        this.nextClickTime = 0L;
                        return;
                    }
                }
                KeyBinding.func_74510_a(key, false);
                long now = System.currentTimeMillis();
                int clicks = 0;
                if (this.nextClickTime == 0L) {
                    if (recoveryClickRequested) {
                        clicks = 1;
                    }
                    this.nextClickTime = now + this.nextDelay();
                } else {
                    while (this.nextClickTime <= now) {
                        clicks++;
                        this.nextClickTime = this.nextClickTime + this.nextDelay();
                    }
                    if (recoveryClickRequested && clicks == 0) {
                        clicks = 1;
                        this.nextClickTime = now + this.nextDelay();
                    }
                }
                for (int i = 0; i < clicks; i++) {
                    KeyBinding.func_74507_a(key);
                    ReflectionUtils.setButton(0, true);
                }
            } else {
                this.nextClickTime = 0L;
                this.isControllingAttack = false;
                KeyBinding.func_74510_a(key, false);
                ReflectionUtils.setButton(0, false);
            }
        } else {
            this.releaseAttackControl();
        }
    }

    private void releaseAttackControl() {
        if (this.isControllingAttack) {
            KeyBinding.func_74510_a(mc.field_71474_y.field_74312_F.func_151463_i(), mc.field_71462_r == null && mc.field_71415_G && Mouse.isButtonDown(0));
            this.isControllingAttack = false;
        }
        this.nextClickTime = 0L;
    }

    private long nextDelay() {
        int target = Math.max(1, (int) this.targetCPS.getInput());
        int baseDelay = 1000 / target;
        int finalDelay;
        if (this.simulateExhaust.isToggled()) {
            int variation = this.rand.nextInt(baseDelay + 1) - baseDelay / 2;
            finalDelay = baseDelay + variation;
            if (this.rand.nextInt(100) < 15) {
                if (this.rand.nextBoolean()) {
                    finalDelay = 25 + this.rand.nextInt(16);
                } else {
                    finalDelay = baseDelay + 50 + this.rand.nextInt(41);
                }
            }
            if (this.rand.nextInt(100) < 8) {
                int spikeMult = 50 + this.rand.nextInt(151);
                finalDelay = finalDelay * spikeMult / 100;
            }
            if (this.rand.nextInt(100) < 10) {
                finalDelay += 10 + this.rand.nextInt(26);
            }
        } else {
            finalDelay = baseDelay + (this.rand.nextInt(21) - 10);
        }
        return Math.max(33, Math.min(180, finalDelay));
    }

    private static void ensureHoveredSlotField() {
        if (hoveredSlotField == null) {
            try {
                hoveredSlotField = GuiContainer.class.getDeclaredField("theSlot");
                hoveredSlotField.setAccessible(true);
            } catch (NoSuchFieldException exception) {
                try {
                    hoveredSlotField = GuiContainer.class.getDeclaredField("field_147006_u");
                    hoveredSlotField.setAccessible(true);
                } catch (NoSuchFieldException exception2) {
                    hoveredSlotField = null;
                }
            }
        }
    }

    private static Slot getHoveredSlot(GuiContainer gui) {
        if (hoveredSlotField != null && gui != null) {
            try {
                Object value = hoveredSlotField.get(gui);
                if (value instanceof Slot) {
                    return (Slot) value;
                }
            } catch (IllegalAccessException exception3) {
            }
            return null;
        } else {
            return null;
        }
    }
}
