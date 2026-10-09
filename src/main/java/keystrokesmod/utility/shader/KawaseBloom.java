package keystrokesmod.utility.shader;

import java.util.ArrayList;
import java.util.List;
import keystrokesmod.utility.IMinecraftInstance;
import keystrokesmod.utility.RenderUtils;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.Framebuffer;
import org.lwjgl.opengl.GL11;

public class KawaseBloom implements IMinecraftInstance {

    public static ShaderUtils kawaseDown = new ShaderUtils("kawaseDownBloom");

    public static ShaderUtils kawaseUp = new ShaderUtils("kawaseUpBloom");

    public static Framebuffer framebuffer;

    private static int currentIterations;

    private static int currentDisplayWidth;

    private static int currentDisplayHeight;

    private static final List<Framebuffer> framebufferList = new ArrayList<>();

    private static void initFramebuffers(int iterations) {
        for (Framebuffer oldFramebuffer : framebufferList) {
            oldFramebuffer.func_147608_a();
        }
        framebufferList.clear();
        for (int i = 1; i <= iterations; i++) {
            Framebuffer currentBuffer = new Framebuffer((int) (mc.field_71443_c / Math.pow(2.0, i)), (int) (mc.field_71440_d / Math.pow(2.0, i)), false);
            currentBuffer.func_147604_a(0.0F, 0.0F, 0.0F, 0.0F);
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

    public static void renderBlur(int framebufferTexture, int iterations, float offset) {
        if (OpenGlHelper.func_148822_b()) {
            if (currentIterations != iterations || currentDisplayWidth != mc.field_71443_c || currentDisplayHeight != mc.field_71440_d) {
                initFramebuffers(iterations);
            }
            RenderUtils.setAlphaLimit(0.0F);
            GlStateManager.func_179147_l();
            GlStateManager.func_179112_b(1, 1);
            GL11.glClearColor(0.0F, 0.0F, 0.0F, 0.0F);
            renderFBO(framebufferList.get(0), framebufferTexture, kawaseDown, offset);
            for (int i = 1; i < iterations; i++) {
                float currentOffset = offset / (float) Math.pow(1.5, i);
                renderFBO(framebufferList.get(i), framebufferList.get(i - 1).field_147617_g, kawaseDown, currentOffset);
            }
            for (int i = iterations - 1; i > 0; i--) {
                float floatValue = offset / (float) Math.pow(1.5, i);
                renderFBO(framebufferList.get(i - 1), framebufferList.get(i).field_147617_g, kawaseUp, floatValue);
            }
            Framebuffer destination = mc.func_147110_a();
            GlStateManager.func_179082_a(0.0F, 0.0F, 0.0F, 0.0F);
            destination.func_147610_a(true);
            RenderUtils.setAlphaLimit(0.0F);
            GlStateManager.func_179147_l();
            GlStateManager.func_179112_b(770, 771);
            kawaseUp.init();
            kawaseUp.setUniformf("offset", offset, offset);
            kawaseUp.setUniformi("inTexture", 0);
            kawaseUp.setUniformi("check", 1);
            kawaseUp.setUniformi("textureToCheck", 16);
            kawaseUp.setUniformf("halfpixel", 1.0F / destination.field_147621_c, 1.0F / destination.field_147618_d);
            kawaseUp.setUniformf("iResolution", destination.field_147621_c, destination.field_147618_d);
            GlStateManager.func_179138_g(34000);
            RenderUtils.bindTexture(framebufferTexture);
            GlStateManager.func_179138_g(33984);
            RenderUtils.bindTexture(framebufferList.get(0).field_147617_g);
            ShaderUtils.drawQuads();
            kawaseUp.unload();
            GlStateManager.func_179144_i(0);
            RenderUtils.setAlphaLimit(0.0F);
            GlStateManager.func_179147_l();
            GlStateManager.func_179112_b(770, 771);
        }
    }

    private static void renderFBO(Framebuffer framebuffer, int framebufferTexture, ShaderUtils shader, float offset) {
        framebuffer.func_147614_f();
        framebuffer.func_147610_a(false);
        shader.init();
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
