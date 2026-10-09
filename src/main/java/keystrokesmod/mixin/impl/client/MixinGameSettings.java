package keystrokesmod.mixin.impl.client;

import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.player.SafeWalk;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(GameSettings.class)
public class MixinGameSettings {

    @Overwrite
    public static boolean func_100015_a(KeyBinding key) {
        SafeWalk safewalk = ModuleManager.safeWalk;
        return key == Minecraft.func_71410_x().field_71474_y.field_74311_E && safewalk != null && safewalk.isEnabled() && safewalk.sneak.isToggled() && safewalk.isSneaking ? true : key.func_151463_i() != 0 && (key.func_151463_i() < 0 ? Mouse.isButtonDown(key.func_151463_i() + 100) : Keyboard.isKeyDown(key.func_151463_i()));
    }
}
