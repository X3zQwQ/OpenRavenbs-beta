package keystrokesmod.utility.shader;

import keystrokesmod.utility.RenderUtils;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.Framebuffer;

public class BlurUtils {

    private static Framebuffer stencilFrameBufferBlur = new Framebuffer(1, 1, false);

    private static Framebuffer stencilFrameBufferBloom = new Framebuffer(1, 1, false);

    private static boolean blurPrepared;

    private static boolean bloomPrepared;

    public static void prepareBlur() {
        blurPrepared = OpenGlHelper.func_148822_b();
        if (!blurPrepared) {
            GlStateManager.func_179135_a(false, false, false, false);
        } else {
            stencilFrameBufferBlur = RenderUtils.createFrameBuffer(stencilFrameBufferBlur);
            stencilFrameBufferBlur.func_147614_f();
            stencilFrameBufferBlur.func_147610_a(false);
        }
    }

    public static void prepareBloom() {
        bloomPrepared = OpenGlHelper.func_148822_b();
        if (!bloomPrepared) {
            GlStateManager.func_179135_a(false, false, false, false);
        } else {
            stencilFrameBufferBloom = RenderUtils.createFrameBuffer(stencilFrameBufferBloom);
            stencilFrameBufferBloom.func_147614_f();
            stencilFrameBufferBloom.func_147610_a(false);
        }
    }

    public static void blurEnd(int passes, float radius) {
        if (!blurPrepared) {
            GlStateManager.func_179135_a(true, true, true, true);
        } else {
            stencilFrameBufferBlur.func_147609_e();
            KawaseBlur.renderBlur(stencilFrameBufferBlur.field_147617_g, passes, radius);
        }
    }

    public static void bloomEnd(int passes, float radius) {
        if (!bloomPrepared) {
            GlStateManager.func_179135_a(true, true, true, true);
        } else {
            stencilFrameBufferBloom.func_147609_e();
            KawaseBloom.renderBlur(stencilFrameBufferBloom.field_147617_g, passes, radius);
        }
    }
}
