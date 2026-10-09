package keystrokesmod.mixin.interfaces;

import net.minecraft.client.shader.ShaderGroup;

public interface ISaturationRenderer {

    ShaderGroup raven$getSaturationShader();

    void raven$setSaturationShader(ShaderGroup shaderGroup);
}
