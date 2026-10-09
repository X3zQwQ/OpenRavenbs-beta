package keystrokesmod.module.impl.render;

import java.io.IOException;
import java.util.List;
import keystrokesmod.mixin.impl.accessor.IAccessorShaderGroup;
import keystrokesmod.mixin.interfaces.ISaturationRenderer;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.SliderSetting;
import net.minecraft.client.shader.Shader;
import net.minecraft.client.shader.ShaderGroup;
import net.minecraft.client.shader.ShaderUniform;
import net.minecraft.util.ResourceLocation;

public class Saturation extends Module {

    private static final ResourceLocation SHADER_LOCATION = new ResourceLocation("minecraft:shaders/post/color_convolve.json");

    private SliderSetting saturationSlider;

    private float lastSaturation = 1.0F;

    public Saturation() {
        super("Saturation", Module.category.render);
        this.registerSetting(this.saturationSlider = new SliderSetting("Saturation", 1.0, -1.0, 5.0, 0.05));
    }

    @Override
    public void onEnable() {
        this.loadShader();
    }

    @Override
    public void onDisable() {
        this.removeShader();
    }

    @Override
    public void onUpdate() {
        float current = (float) this.saturationSlider.getInput();
        if (!this.isShaderActive()) {
            this.loadShader();
        }
        if (current != this.lastSaturation) {
            this.lastSaturation = current;
            this.applySaturation();
        }
    }

    private void loadShader() {
        if (mc.field_71441_e != null) {
            if (!this.isShaderActive()) {
                try {
                    ShaderGroup shader = new ShaderGroup(mc.func_110434_K(), mc.func_110442_L(), mc.func_147110_a(), SHADER_LOCATION);
                    shader.func_148026_a(mc.field_71443_c, mc.field_71440_d);
                    ISaturationRenderer ren = this.getSaturationRenderer();
                    if (ren != null) {
                        ren.raven$setSaturationShader(shader);
                    }
                    this.lastSaturation = (float) this.saturationSlider.getInput();
                    this.applySaturation();
                } catch (IOException exception) {
                    exception.printStackTrace();
                }
            }
        }
    }

    private void removeShader() {
        ISaturationRenderer renderer = this.getSaturationRenderer();
        if (renderer != null) {
            ShaderGroup shader = renderer.raven$getSaturationShader();
            if (shader != null) {
                shader.func_148021_a();
            }
            renderer.raven$setSaturationShader(null);
        }
    }

    private void applySaturation() {
        ISaturationRenderer renderer = this.getSaturationRenderer();
        if (renderer != null) {
            ShaderGroup shader = renderer.raven$getSaturationShader();
            if (shader != null) {
                List<Shader> shaders = ((IAccessorShaderGroup) shader).getListShaders();
                if (shaders != null) {
                    for (Shader s : shaders) {
                        ShaderUniform su = s.func_148043_c().func_147991_a("Saturation");
                        if (su != null) {
                            su.func_148090_a(this.lastSaturation);
                        }
                    }
                }
            }
        }
    }

    private boolean isShaderActive() {
        ISaturationRenderer renderer = this.getSaturationRenderer();
        return renderer != null && renderer.raven$getSaturationShader() != null;
    }

    private ISaturationRenderer getSaturationRenderer() {
        return mc.field_71460_t == null ? null : (ISaturationRenderer) mc.field_71460_t;
    }
}
