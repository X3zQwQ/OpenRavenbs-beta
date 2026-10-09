package keystrokesmod.mixin.impl.render;

import keystrokesmod.Raven;
import keystrokesmod.event.KeyPressEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.MinecraftForge;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiScreen.class)
public abstract class MixinGuiScreen {

    @Shadow
    public Minecraft field_146297_k;

    @Shadow
    public int field_146294_l;

    @Shadow
    public int field_146295_m;

    @Inject(method = "sendChatMessage(Ljava/lang/String;Z)V", at = @At("HEAD"), cancellable = true)
    private void messageSend(String msg, boolean addToChat, CallbackInfo callbackInfo) {
        if (addToChat && Raven.commandManager != null && Raven.commandManager.handleChatMessage(msg)) {
            this.field_146297_k.field_71456_v.func_146158_b().func_146239_a(msg);
            callbackInfo.cancel();
        }
    }

    @Inject(method = "handleKeyboardInput", at = @At("HEAD"), cancellable = true)
    private void injectHandleKeyboardInput(CallbackInfo callbackInfo) {
        KeyPressEvent event = new KeyPressEvent(Keyboard.getEventCharacter(), Keyboard.getEventKey());
        MinecraftForge.EVENT_BUS.post(event);
        if (event.isCanceled()) {
            callbackInfo.cancel();
        }
    }
}
