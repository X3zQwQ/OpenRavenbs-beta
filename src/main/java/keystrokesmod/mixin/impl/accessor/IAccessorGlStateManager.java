package keystrokesmod.mixin.impl.accessor;

import net.minecraft.client.renderer.GlStateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GlStateManager.class)
public interface IAccessorGlStateManager {

    @Accessor("activeTextureUnit")
    static int getActiveTextureUnit() {
        throw new AssertionError();
    }
}
