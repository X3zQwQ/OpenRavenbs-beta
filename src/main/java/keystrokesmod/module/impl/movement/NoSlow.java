package keystrokesmod.module.impl.movement;

import keystrokesmod.event.PostPlayerInputEvent;
import keystrokesmod.event.PreUpdateEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.DescriptionSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.Utils;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemSword;
import net.minecraft.network.play.client.C07PacketPlayerDigging.Action;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class NoSlow extends Module {

    public static SliderSetting mode;

    public static SliderSetting slowed;

    public static ButtonSetting disableBow;

    public static ButtonSetting disablePotions;

    public static ButtonSetting swordOnly;

    public static ButtonSetting vanillaSword;

    private final String[] NOSLOW_MODES = new String[] { "Vanilla", "Beta" };

    public boolean noSlowing;

    private boolean setJump;

    public NoSlow() {
        super("NoSlow", Module.category.movement, 0);
        this.registerSetting(new DescriptionSetting("Default is 80% motion reduction."));
        this.registerSetting(mode = new SliderSetting("Mode", 0, this.NOSLOW_MODES));
        this.registerSetting(slowed = new SliderSetting("Slow %", 80.0, 0.0, 80.0, 1.0));
        this.registerSetting(disableBow = new ButtonSetting("Disable bow", false));
        this.registerSetting(disablePotions = new ButtonSetting("Disable potions", false));
        this.registerSetting(swordOnly = new ButtonSetting("Sword only", false));
        this.registerSetting(vanillaSword = new ButtonSetting("Vanilla sword", false));
    }

    @Override
    public void onDisable() {
        this.noSlowing = false;
    }

    @SubscribeEvent
    public void onPreUpdate(PreUpdateEvent e) {
        if (!vanillaSword.isToggled() || !Utils.holdingSword()) {
            boolean apply = getSlowed() != 0.2F;
            if (apply && mc.field_71439_g.func_71039_bw()) {
                switch((int) mode.getInput()) {
                    case 1:
                        mc.field_71439_g.field_71174_a.func_147297_a(new C09PacketHeldItemChange(mc.field_71439_g.field_71071_by.field_70461_c % 8 + 1));
                        mc.field_71439_g.field_71174_a.func_147297_a(new C09PacketHeldItemChange(mc.field_71439_g.field_71071_by.field_70461_c));
                        mc.field_71439_g.field_71174_a.func_147297_a(new C07PacketPlayerDigging(Action.RELEASE_USE_ITEM, BlockPos.field_177992_a, EnumFacing.DOWN));
                }
            }
        }
    }

    @SubscribeEvent
    public void onPostPlayerInput(PostPlayerInputEvent e) {
        if (this.setJump) {
            mc.field_71439_g.field_71158_b.field_78901_c = true;
            this.setJump = false;
        }
    }

    public static float getSlowed() {
        if (mc.field_71439_g.func_70694_bm() == null || ModuleManager.noSlow == null || !ModuleManager.noSlow.isEnabled()) {
            return 0.2F;
        } else if (swordOnly.isToggled() && !(mc.field_71439_g.func_70694_bm().func_77973_b() instanceof ItemSword)) {
            return 0.2F;
        } else if (mc.field_71439_g.func_70694_bm().func_77973_b() instanceof ItemBow && disableBow.isToggled()) {
            return 0.2F;
        } else {
            return mc.field_71439_g.func_70694_bm().func_77973_b() instanceof ItemPotion && !ItemPotion.func_77831_g(mc.field_71439_g.func_70694_bm().func_77952_i()) && disablePotions.isToggled() ? 0.2F : (100.0F - (float) slowed.getInput()) / 100.0F;
        }
    }

    @Override
    public String getInfo() {
        return this.NOSLOW_MODES[(int) mode.getInput()];
    }
}
