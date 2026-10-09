package keystrokesmod.utility;

import java.nio.Buffer;
import java.nio.FloatBuffer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

public final class GhostHandRenderState {

    private static final FloatBuffer previousBlendColor = BufferUtils.createFloatBuffer(16);

    private static final FloatBuffer previousColor = BufferUtils.createFloatBuffer(16);

    private static boolean active;

    private static float opacity;

    private static boolean blendEnabled;

    private static boolean cullEnabled;

    private static int cullFace;

    private static boolean depthMask;

    private static int depthFunction;

    private static int sourceRgb;

    private static int destinationRgb;

    private static int sourceAlpha;

    private static int destinationAlpha;

    private GhostHandRenderState() {
    }

    public static boolean begin(float alpha) {
        if (!active && !(alpha >= 1.0F)) {
            ((Buffer) previousBlendColor).clear();
            ((Buffer) previousColor).clear();
            GL11.glGetFloat(32773, previousBlendColor);
            GL11.glGetFloat(2816, previousColor);
            blendEnabled = GL11.glIsEnabled(3042);
            cullEnabled = GL11.glIsEnabled(2884);
            cullFace = GL11.glGetInteger(2885);
            depthMask = GL11.glGetBoolean(2930);
            depthFunction = GL11.glGetInteger(2932);
            sourceRgb = GL11.glGetInteger(32969);
            destinationRgb = GL11.glGetInteger(32968);
            sourceAlpha = GL11.glGetInteger(32971);
            destinationAlpha = GL11.glGetInteger(32970);
            opacity = alpha;
            active = true;
            setColorAlpha(previousColor.get(3));
            GlStateManager.func_179147_l();
            GlStateManager.func_179089_o();
            GlStateManager.func_179107_e(1029);
            GlStateManager.func_179120_a(770, 771, 1, 0);
            GlStateManager.func_179132_a(false);
            return true;
        } else {
            return false;
        }
    }

    public static boolean isActive() {
        return active;
    }

    public static void setColorAlpha(float alpha) {
        if (active) {
            GL14.glBlendColor(0.0F, 0.0F, 0.0F, opacity * alpha);
        }
    }

    public static void end() {
        if (active) {
            active = false;
            GL14.glBlendColor(previousBlendColor.get(0), previousBlendColor.get(1), previousBlendColor.get(2), previousBlendColor.get(3));
            GlStateManager.func_179120_a(sourceRgb, destinationRgb, sourceAlpha, destinationAlpha);
            OpenGlHelper.func_148821_a(sourceRgb, destinationRgb, sourceAlpha, destinationAlpha);
            if (blendEnabled) {
                GlStateManager.func_179147_l();
            } else {
                GlStateManager.func_179084_k();
            }
            GlStateManager.func_179107_e(cullFace);
            if (cullEnabled) {
                GlStateManager.func_179089_o();
            } else {
                GlStateManager.func_179129_p();
            }
            GlStateManager.func_179132_a(depthMask);
            GlStateManager.func_179143_c(depthFunction);
            GlStateManager.func_179117_G();
            GlStateManager.func_179131_c(previousColor.get(0), previousColor.get(1), previousColor.get(2), previousColor.get(3));
        }
    }
}
