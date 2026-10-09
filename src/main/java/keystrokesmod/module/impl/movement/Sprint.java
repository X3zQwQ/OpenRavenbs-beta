package keystrokesmod.module.impl.movement;

import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.DescriptionSetting;
import keystrokesmod.utility.Utils;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.settings.KeyBinding;

public class Sprint extends Module {

    private final ButtonSetting allowUsingItem;

    private final ButtonSetting allowBackwards;

    private final ButtonSetting allowSideways;

    private final ButtonSetting allowInInventory;

    public Sprint() {
        super("Sprint", Module.category.movement, 0);
        this.liteModule = true;
        this.registerSetting(new DescriptionSetting("Allow while"));
        this.registerSetting(this.allowUsingItem = new ButtonSetting("Using item", false));
        this.registerSetting(this.allowBackwards = new ButtonSetting("Backwards", false));
        this.registerSetting(this.allowSideways = new ButtonSetting("Sideways", false));
        this.registerSetting(this.allowInInventory = new ButtonSetting("In inventory", false));
        this.closetModule = true;
    }

    @Override
    public void onDisable() {
        if (Utils.nullCheck()) {
            KeyBinding.func_74510_a(mc.field_71474_y.field_151444_V.func_151463_i(), false);
        }
    }

    @Override
    public void onUpdate() {
        if (Utils.nullCheck()) {
            boolean inGame = mc.field_71415_G;
            boolean inInv = this.allowInInventory.isToggled() && (mc.field_71462_r instanceof GuiInventory || mc.field_71462_r instanceof GuiChest);
            if (inGame || inInv) {
                KeyBinding.func_74510_a(mc.field_71474_y.field_151444_V.func_151463_i(), true);
            }
        }
    }

    public boolean allowWhileUsingItem() {
        return this.isEnabled() && this.allowUsingItem.isToggled();
    }

    public boolean allowWhileBackwards() {
        return this.isEnabled() && this.allowBackwards.isToggled();
    }

    public boolean allowWhileSideways() {
        return this.isEnabled() && this.allowSideways.isToggled();
    }
}
