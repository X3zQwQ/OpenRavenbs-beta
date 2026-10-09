package keystrokesmod.module.impl.player;

import keystrokesmod.Raven;
import keystrokesmod.event.ClientRotationEvent;
import keystrokesmod.event.PreUpdateEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.utility.Utils;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class WaterBucket extends Module {

    public ButtonSetting pickupWater;

    public ButtonSetting silentAim;

    public ButtonSetting switchToItem;

    private final long PLACE_DELAY = 500L;

    private final long PICKUP_WAIT = 150L;

    private long lastPlace = 0L;

    private boolean shouldPickup = false;

    private int lastSlot = -1;

    public WaterBucket() {
        super("Water Bucket", Module.category.player);
        this.liteModule = true;
        this.registerSetting(this.pickupWater = new ButtonSetting("Pickup water", true));
        this.registerSetting(this.silentAim = new ButtonSetting("Silent aim", true));
        this.registerSetting(this.switchToItem = new ButtonSetting("Switch to item", true));
    }

    @Override
    public void onDisable() {
        this.lastPlace = 0L;
        this.shouldPickup = false;
        this.lastSlot = -1;
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent e) {
        if (Utils.nullCheck() && !mc.func_147113_T() && !mc.field_71439_g.field_71075_bZ.field_75100_b && !mc.field_71439_g.field_71075_bZ.field_75098_d) {
            if (this.fallCheck()) {
                MovingObjectPosition mop = Utils.getTarget(mc.field_71442_b.func_78757_d(), mc.field_71439_g.field_70177_z, this.silentAim.isToggled() ? 90.0F : mc.field_71439_g.field_70125_A);
                if (mop != null && mop.field_72313_a == MovingObjectType.BLOCK && mop.field_178784_b == EnumFacing.UP) {
                    long now = System.currentTimeMillis();
                    if (Utils.timeBetween(this.lastPlace, now) >= 500L) {
                        if (!this.isItem(mc.field_71439_g.func_70694_bm(), Items.field_151131_as) && this.switchToItem.isToggled()) {
                            this.attemptSwitch();
                        }
                        if (this.silentAim.isToggled() || !(mc.field_71439_g.field_70125_A < 80.0F)) {
                            this.lastPlace = now;
                            this.useCurrentItem();
                            if (!(this.shouldPickup = this.pickupWater.isToggled())) {
                                this.lastSlot = -1;
                            }
                            if (Raven.DEBUG) {
                                Utils.sendModuleMessage(this, "&7Placed with motionY &d" + Utils.round(mc.field_71439_g.field_70181_x, 2) + " &7and fall distance &d" + Utils.round(mc.field_71439_g.field_70143_R, 2));
                            }
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onPreUpdate(PreUpdateEvent e) {
        if (!mc.func_147113_T()) {
            if (this.shouldPickup && Utils.timeBetween(this.lastPlace, System.currentTimeMillis()) > 150L && this.isItem(mc.field_71439_g.func_70694_bm(), Items.field_151133_ar)) {
                this.shouldPickup = false;
                this.useCurrentItem();
                if (this.lastSlot != -1) {
                    Utils.switchSlot(this.lastSlot, true);
                    this.lastSlot = -1;
                }
            }
        }
    }

    @SubscribeEvent
    public void onClientRotation(ClientRotationEvent e) {
        if (ModuleManager.bedAura == null || !ModuleManager.bedAura.shouldOverrideMouseOver()) {
            if (this.silentAim.isToggled() && (this.fallCheck() || Utils.timeBetween(this.lastPlace, System.currentTimeMillis()) < 500L) && this.getWaterBucketSlot() != -1) {
                e.setYaw(mc.field_71439_g.field_70177_z);
                e.setPitch(90.0F);
            }
        }
    }

    private void attemptSwitch() {
        int slot = this.getWaterBucketSlot();
        if (slot != -1) {
            this.lastSlot = mc.field_71439_g.field_71071_by.field_70461_c;
            Utils.switchSlot(slot, true);
        }
    }

    private int getWaterBucketSlot() {
        for (int slot = 0; slot < InventoryPlayer.func_70451_h(); slot++) {
            if (this.isItem(mc.field_71439_g.field_71071_by.func_70301_a(slot), Items.field_151131_as)) {
                return slot;
            }
        }
        return -1;
    }

    private void useCurrentItem() {
        mc.func_147114_u().func_147297_a(new C08PacketPlayerBlockPlacement(mc.field_71439_g.func_70694_bm()));
    }

    private boolean isItem(ItemStack itemStack, Item item) {
        return itemStack != null && itemStack.func_77973_b() == item;
    }

    private boolean fallCheck() {
        return !mc.field_71439_g.field_70122_E && mc.field_71439_g.field_70143_R >= 3.3;
    }
}
