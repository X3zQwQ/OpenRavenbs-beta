package keystrokesmod.utility.shader;

import java.util.ArrayList;
import java.util.List;
import keystrokesmod.utility.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.Framebuffer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

public class KawaseBlur {

    private static final Minecraft mc = Minecraft.func_71410_x();

    public static ShaderUtils kawaseDown = new ShaderUtils("kawaseDown");

    public static ShaderUtils kawaseUp = new ShaderUtils("kawaseUp");

    public static Framebuffer framebuffer;

    private static int currentIterations;

    private static int currentDisplayWidth;

    private static int currentDisplayHeight;

    private static final List<Framebuffer> framebufferList = new ArrayList<>();

    private static void initFrameBuffers(int iterations) {
        for (Framebuffer oldFramebuffer : framebufferList) {
            oldFramebuffer.func_147608_a();
        }
        framebufferList.clear();
        for (int i = 1; i <= iterations; i++) {
            Framebuffer currentBuffer = new Framebuffer((int) (mc.field_71443_c / Math.pow(3.0, i)), (int) (mc.field_71440_d / Math.pow(3.0, i)), false);
            currentBuffer.func_147607_a(9729);
            GlStateManager.func_179144_i(currentBuffer.field_147617_g);
            GL11.glTexParameteri(3553, 10242, 33648);
            GL11.glTexParameteri(3553, 10243, 33648);
            GlStateManager.func_179144_i(0);
            framebufferList.add(currentBuffer);
        }
        framebuffer = framebufferList.get(0);
        currentIterations = iterations;
        currentDisplayWidth = mc.field_71443_c;
        currentDisplayHeight = mc.field_71440_d;
    }

    public static void renderBlur(int stencilFrameBufferTexture, int iterations, float offset) {
        if (OpenGlHelper.func_148822_b()) {
            if (currentIterations != iterations || currentDisplayWidth != mc.field_71443_c || currentDisplayHeight != mc.field_71440_d) {
                initFrameBuffers(iterations);
            }
            renderFBO(framebufferList.get(0), mc.func_147110_a().field_147617_g, kawaseDown, offset);
            for (int i = 1; i < iterations; i++) {
                renderFBO(framebufferList.get(i), framebufferList.get(i - 1).field_147617_g, kawaseDown, offset);
            }
            for (int i = iterations - 1; i > 0; i--) {
                renderFBO(framebufferList.get(i - 1), framebufferList.get(i).field_147617_g, kawaseUp, offset);
            }
            Framebuffer destination = mc.func_147110_a();
            destination.func_147610_a(true);
            RenderUtils.setAlphaLimit(0.0F);
            GlStateManager.func_179147_l();
            GlStateManager.func_179112_b(770, 771);
            kawaseUp.init();
            kawaseUp.setUniformf("offset", offset, offset);
            kawaseUp.setUniformi("inTexture", 0);
            kawaseUp.setUniformi("check", 1);
            kawaseUp.setUniformi("textureToCheck", 2);
            kawaseUp.setUniformf("halfpixel", 1.0F / destination.field_147621_c, 1.0F / destination.field_147618_d);
            kawaseUp.setUniformf("iResolution", destination.field_147621_c, destination.field_147618_d);
            GL13.glActiveTexture(33986);
            RenderUtils.bindTexture(stencilFrameBufferTexture);
            GL13.glActiveTexture(33984);
            RenderUtils.bindTexture(framebufferList.get(0).field_147617_g);
            ShaderUtils.drawQuads();
            kawaseUp.unload();
            GlStateManager.func_179144_i(0);
            GlStateManager.func_179084_k();
        }
    }

    private static void renderFBO(Framebuffer framebuffer, int framebufferTexture, ShaderUtils shader, float offset) {
        framebuffer.func_147614_f();
        framebuffer.func_147610_a(false);
        shader.init();
        GL13.glActiveTexture(33984);
        RenderUtils.bindTexture(framebufferTexture);
        shader.setUniformf("offset", offset, offset);
        shader.setUniformi("inTexture", 0);
        shader.setUniformi("check", 0);
        shader.setUniformf("halfpixel", 1.0F / framebuffer.field_147621_c, 1.0F / framebuffer.field_147618_d);
        shader.setUniformf("iResolution", framebuffer.field_147621_c, framebuffer.field_147618_d);
        ShaderUtils.drawQuads();
        shader.unload();
    }
}
