package keystrokesmod.utility.shader;

import keystrokesmod.utility.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.shader.Framebuffer;
import org.lwjgl.opengl.GL20;

public class SeparableOutlineShader {

    private static final Minecraft mc = Minecraft.func_71410_x();

    private final SeparableOutlineShader.DirectionalShader shader = new SeparableOutlineShader.DirectionalShader();

    private Framebuffer framebuffer;

    public boolean isValid() {
        return this.shader.isValid();
    }

    public void render(Framebuffer source) {
        this.framebuffer = RenderUtils.createFrameBuffer(this.framebuffer, false);
        if (this.framebuffer != null) {
            this.framebuffer.func_147614_f();
            this.framebuffer.func_147610_a(false);
            GlStateManager.func_179084_k();
            RenderUtils.setAlphaLimit(0.0F);
            this.shader.use();
            this.shader.setPass(0.0F, 1.0F, false);
            RenderUtils.drawFramebufferFullscreen(source);
            this.shader.stop();
            mc.func_147110_a().func_147610_a(false);
            GlStateManager.func_179147_l();
            GlStateManager.func_179112_b(770, 771);
            this.shader.use();
            this.shader.setPass(1.0F, 0.0F, true);
            GlStateManager.func_179138_g(34000);
            RenderUtils.bindTexture(source.field_147617_g);
            GlStateManager.func_179138_g(33984);
            RenderUtils.drawFramebufferFullscreen(this.framebuffer);
            this.shader.stop();
            GlStateManager.func_179144_i(0);
        }
    }

    private static final class DirectionalShader extends OutlineESPShader {

        private static final String FRAG = "#version 120\nuniform sampler2D tex;\nuniform sampler2D original;\nuniform vec2 texelSize;\nuniform vec2 direction;\nuniform int finalPass;\nvoid main() {\n  vec2 uv = gl_TexCoord[0].xy;\n  if (finalPass == 1 && texture2D(original, uv).a > 0.0) { gl_FragColor = vec4(0.0); return; }\n  vec4 selected = vec4(0.0);\n  for (float offset = -2.0; offset <= 2.0; offset += 1.0) {\n    vec4 sampleColor = texture2D(tex, uv + direction * texelSize * offset);\n    if (sampleColor.a > 0.0) selected = sampleColor;\n  }\n  gl_FragColor = selected;\n}";

        private DirectionalShader() {
            super("#version 120\nuniform sampler2D tex;\nuniform sampler2D original;\nuniform vec2 texelSize;\nuniform vec2 direction;\nuniform int finalPass;\nvoid main() {\n  vec2 uv = gl_TexCoord[0].xy;\n  if (finalPass == 1 && texture2D(original, uv).a > 0.0) { gl_FragColor = vec4(0.0); return; }\n  vec4 selected = vec4(0.0);\n  for (float offset = -2.0; offset <= 2.0; offset += 1.0) {\n    vec4 sampleColor = texture2D(tex, uv + direction * texelSize * offset);\n    if (sampleColor.a > 0.0) selected = sampleColor;\n  }\n  gl_FragColor = selected;\n}");
        }

        @Override
        public void onLink() {
            this.cacheUniform("tex");
            this.cacheUniform("original");
            this.cacheUniform("texelSize");
            this.cacheUniform("direction");
            this.cacheUniform("finalPass");
        }

        @Override
        public void onUse() {
            GL20.glUseProgram(this.programId);
            int location = this.uniform("tex");
            if (location >= 0) {
                GL20.glUniform1i(location, 0);
            }
            location = this.uniform("original");
            if (location >= 0) {
                GL20.glUniform1i(location, 16);
            }
            location = this.uniform("texelSize");
            if (location >= 0) {
                GL20.glUniform2f(location, 1.0F / SeparableOutlineShader.mc.field_71443_c, 1.0F / SeparableOutlineShader.mc.field_71440_d);
            }
        }

        private void setPass(float x, float y, boolean finalPass) {
            int location = this.uniform("direction");
            if (location >= 0) {
                GL20.glUniform2f(location, x, y);
            }
            location = this.uniform("finalPass");
            if (location >= 0) {
                GL20.glUniform1i(location, finalPass ? 1 : 0);
            }
        }
    }
}
