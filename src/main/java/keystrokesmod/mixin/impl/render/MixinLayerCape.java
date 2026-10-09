package keystrokesmod.mixin.impl.render;

import keystrokesmod.module.impl.client.Settings;
import keystrokesmod.utility.CapeManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.renderer.entity.layers.LayerCape;
import net.minecraft.entity.player.EnumPlayerModelParts;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@SideOnly(Side.CLIENT)
@Mixin(LayerCape.class)
public class MixinLayerCape {

    @Shadow
    private final RenderPlayer field_177167_a;

    public MixinLayerCape(RenderPlayer playerRendererIn) {
        this.field_177167_a = playerRendererIn;
    }

    @Redirect(method = "doRenderLayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/AbstractClientPlayer;isWearing(Lnet/minecraft/entity/player/EnumPlayerModelParts;)Z"))
    private boolean modifyIsWearing(AbstractClientPlayer player, EnumPlayerModelParts part) {
        return player.equals(Minecraft.func_71410_x().field_71439_g) && this.getSelectedCape() != null ? true : player.func_175148_a(part);
    }

    @Redirect(method = "doRenderLayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/AbstractClientPlayer;getLocationCape()Lnet/minecraft/util/ResourceLocation;"))
    private ResourceLocation modifyGetLocationCape(AbstractClientPlayer player) {
        ResourceLocation selectedCape = this.getSelectedCape();
        return player.equals(Minecraft.func_71410_x().field_71439_g) && selectedCape != null ? selectedCape : player.func_110303_q();
    }

    private ResourceLocation getSelectedCape() {
        return Settings.customCapes == null ? null : CapeManager.getCape((int) Settings.customCapes.getInput());
    }
}
