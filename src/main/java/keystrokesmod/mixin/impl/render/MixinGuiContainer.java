package keystrokesmod.mixin.impl.render;

import keystrokesmod.module.ModuleManager;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiContainer.class)
public class MixinGuiContainer {

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void raven$cancelManagedInventoryMouseClick(int mouseX, int mouseY, int mouseButton, CallbackInfo callbackInfo) {
        if (shouldCancelManualInventoryInput()) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = "mouseClickMove", at = @At("HEAD"), cancellable = true)
    private void raven$cancelManagedInventoryMouseDrag(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick, CallbackInfo callbackInfo) {
        if (shouldCancelManualInventoryInput()) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void raven$cancelManagedInventoryMouseRelease(int mouseX, int mouseY, int state, CallbackInfo callbackInfo) {
        if (shouldCancelManualInventoryInput()) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = "handleMouseClick", at = @At("HEAD"), cancellable = true)
    private void raven$cancelManagedInventoryWindowClick(Slot slotIn, int slotId, int clickedButton, int clickType, CallbackInfo callbackInfo) {
        if (shouldCancelManualInventoryInput()) {
            callbackInfo.cancel();
        }
    }

    private static boolean shouldCancelManualInventoryInput() {
        return ModuleManager.inventory != null && ModuleManager.inventory.shouldCancelManualInventoryInput();
    }
}
