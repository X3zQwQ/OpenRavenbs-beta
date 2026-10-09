package keystrokesmod.utility.shader;

import java.awt.Color;
import keystrokesmod.utility.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

public class RoundedUtils {

    public static ShaderUtils roundedShader = new ShaderUtils("roundedRect");

    public static ShaderUtils roundedOutlineShader = new ShaderUtils("roundRectOutline");

    private static final ShaderUtils roundedTexturedShader = new ShaderUtils("roundRectTexture");

    private static final ShaderUtils roundedGradientShader = new ShaderUtils("roundedRectGradient");

    private static final ShaderUtils roundedRectRiseShader = new ShaderUtils("roundedRectRise");

    public static void drawRound(float x, float y, float width, float height, float radius, Color color) {
        drawRound(x, y, width, height, radius, false, color);
    }

    public static void drawGradientHorizontal(float x, float y, float width, float height, float radius, Color left, Color right) {
        drawGradientRound(x, y, width, height, radius, left, left, right, right);
    }

    public static void drawGradientVertical(float x, float y, float width, float height, float radius, Color top, Color bottom) {
        drawGradientRound(x, y, width, height, radius, bottom, top, bottom, top);
    }

    public static void drawGradientCornerLR(float x, float y, float width, float height, float radius, Color topLeft, Color bottomRight) {
        Color mixedColor = RenderUtils.interpolateColorC(topLeft, bottomRight, 0.5F);
        drawGradientRound(x, y, width, height, radius, mixedColor, topLeft, bottomRight, mixedColor);
    }

    public static void drawGradientCornerRL(float x, float y, float width, float height, float radius, Color bottomLeft, Color topRight) {
        Color mixedColor = RenderUtils.interpolateColorC(topRight, bottomLeft, 0.5F);
        drawGradientRound(x, y, width, height, radius, bottomLeft, mixedColor, mixedColor, topRight);
    }

    public static void drawRound(float x, float y, float width, float height, float radius, int color) {
        drawRound(x, y, width, height, radius, false, color);
    }

    public static void drawRound(float x, float y, float width, float height, float radius, boolean blur, int color) {
        RenderUtils.resetColor();
        GlStateManager.func_179147_l();
        GlStateManager.func_179112_b(770, 771);
        GL11.glBlendFunc(770, 771);
        RenderUtils.setAlphaLimit(0.0F);
        roundedShader.init();
        setupRoundedRectUniforms(x, y, width, height, radius, roundedShader);
        roundedShader.setUniformi("blur", blur ? 1 : 0);
        roundedShader.setUniformf("color", getRed(color), getGreen(color), getBlue(color), getAlpha(color));
        ShaderUtils.drawQuads(x - 1.0F, y - 1.0F, width + 2.0F, height + 2.0F);
        roundedShader.unload();
        GlStateManager.func_179084_k();
    }

    public static void drawGradientRound(float x, float y, float width, float height, float radius, Color bottomLeft, Color topLeft, Color bottomRight, Color topRight) {
        RenderUtils.setAlphaLimit(0.0F);
        RenderUtils.resetColor();
        GlStateManager.func_179147_l();
        GlStateManager.func_179112_b(770, 771);
        roundedGradientShader.init();
        setupRoundedRectUniforms(x, y, width, height, radius, roundedGradientShader);
        roundedGradientShader.setUniformf("color1", topLeft.getRed() / 255.0F, topLeft.getGreen() / 255.0F, topLeft.getBlue() / 255.0F, topLeft.getAlpha() / 255.0F);
        roundedGradientShader.setUniformf("color2", bottomLeft.getRed() / 255.0F, bottomLeft.getGreen() / 255.0F, bottomLeft.getBlue() / 255.0F, bottomLeft.getAlpha() / 255.0F);
        roundedGradientShader.setUniformf("color3", topRight.getRed() / 255.0F, topRight.getGreen() / 255.0F, topRight.getBlue() / 255.0F, topRight.getAlpha() / 255.0F);
        roundedGradientShader.setUniformf("color4", bottomRight.getRed() / 255.0F, bottomRight.getGreen() / 255.0F, bottomRight.getBlue() / 255.0F, bottomRight.getAlpha() / 255.0F);
        ShaderUtils.drawQuads(x - 1.0F, y - 1.0F, width + 2.0F, height + 2.0F);
        roundedGradientShader.unload();
        GlStateManager.func_179084_k();
    }

    public static void drawGradientRound(float x, float y, float width, float height, float radius, int bottomLeft, int topLeft, int bottomRight, int topRight) {
        RenderUtils.setAlphaLimit(0.0F);
        RenderUtils.resetColor();
        GlStateManager.func_179147_l();
        GlStateManager.func_179112_b(770, 771);
        roundedGradientShader.init();
        setupRoundedRectUniforms(x, y, width, height, radius, roundedGradientShader);
        roundedGradientShader.setUniformf("color1", getRed(topLeft), getGreen(topLeft), getBlue(topLeft), getAlpha(topLeft));
        roundedGradientShader.setUniformf("color2", getRed(bottomLeft), getGreen(bottomLeft), getBlue(bottomLeft), getAlpha(bottomLeft));
        roundedGradientShader.setUniformf("color3", getRed(topRight), getGreen(topRight), getBlue(topRight), getAlpha(topRight));
        roundedGradientShader.setUniformf("color4", getRed(bottomRight), getGreen(bottomRight), getBlue(bottomRight), getAlpha(bottomRight));
        ShaderUtils.drawQuads(x - 1.0F, y - 1.0F, width + 2.0F, height + 2.0F);
        roundedGradientShader.unload();
        GlStateManager.func_179084_k();
    }

    public static void drawRound(float x, float y, float width, float height, float radius, boolean blur, Color color) {
        RenderUtils.resetColor();
        GlStateManager.func_179147_l();
        GlStateManager.func_179112_b(770, 771);
        GL11.glBlendFunc(770, 771);
        RenderUtils.setAlphaLimit(0.0F);
        roundedShader.init();
        setupRoundedRectUniforms(x, y, width, height, radius, roundedShader);
        roundedShader.setUniformi("blur", blur ? 1 : 0);
        roundedShader.setUniformf("color", color.getRed() / 255.0F, color.getGreen() / 255.0F, color.getBlue() / 255.0F, color.getAlpha() / 255.0F);
        ShaderUtils.drawQuads(x - 1.0F, y - 1.0F, width + 2.0F, height + 2.0F);
        roundedShader.unload();
        GlStateManager.func_179084_k();
    }

    public static void drawRoundOutline(float x, float y, float width, float height, float radius, float outlineThickness, Color color, Color outlineColor) {
        RenderUtils.resetColor();
        GlStateManager.func_179147_l();
        GlStateManager.func_179112_b(770, 771);
        GL11.glBlendFunc(770, 771);
        RenderUtils.setAlphaLimit(0.0F);
        roundedOutlineShader.init();
        ScaledResolution sr = new ScaledResolution(Minecraft.func_71410_x());
        setupRoundedRectUniforms(x, y, width, height, radius, roundedOutlineShader);
        roundedOutlineShader.setUniformf("outlineThickness", outlineThickness * sr.func_78325_e());
        roundedOutlineShader.setUniformf("color", color.getRed() / 255.0F, color.getGreen() / 255.0F, color.getBlue() / 255.0F, color.getAlpha() / 255.0F);
        roundedOutlineShader.setUniformf("outlineColor", outlineColor.getRed() / 255.0F, outlineColor.getGreen() / 255.0F, outlineColor.getBlue() / 255.0F, outlineColor.getAlpha() / 255.0F);
        ShaderUtils.drawQuads(x - (2.0F + outlineThickness), y - (2.0F + outlineThickness), width + (4.0F + outlineThickness * 2.0F), height + (4.0F + outlineThickness * 2.0F));
        roundedOutlineShader.unload();
        GlStateManager.func_179084_k();
    }

    public static void drawRoundTextured(float x, float y, float width, float height, float radius, float alpha) {
        RenderUtils.resetColor();
        RenderUtils.setAlphaLimit(0.0F);
        GlStateManager.func_179147_l();
        GlStateManager.func_179112_b(770, 771);
        roundedTexturedShader.init();
        roundedTexturedShader.setUniformi("textureIn", 0);
        setupRoundedRectUniforms(x, y, width, height, radius, roundedTexturedShader);
        roundedTexturedShader.setUniformf("alpha", alpha);
        ShaderUtils.drawQuads(x - 1.0F, y - 1.0F, width + 2.0F, height + 2.0F);
        roundedTexturedShader.unload();
        GlStateManager.func_179084_k();
    }

    private static void setupRoundedRectUniforms(float x, float y, float width, float height, float radius, ShaderUtils roundedTexturedShader) {
        ScaledResolution sr = new ScaledResolution(Minecraft.func_71410_x());
        roundedTexturedShader.setUniformf("location", x * sr.func_78325_e(), Minecraft.func_71410_x().field_71440_d - height * sr.func_78325_e() - y * sr.func_78325_e());
        roundedTexturedShader.setUniformf("rectSize", width * sr.func_78325_e(), height * sr.func_78325_e());
        roundedTexturedShader.setUniformf("radius", radius * sr.func_78325_e());
    }

    public static void drawRoundedRectRise(float x, float y, float width, float height, float radius, int color, boolean leftTop, boolean rightTop, boolean rightBottom, boolean leftBottom) {
        GL11.glPushMatrix();
        GlStateManager.func_179123_a();
        int programId = roundedRectRiseShader.programID;
        GL20.glUseProgram(programId);
        roundedRectRiseShader.setUniformf("u_size", width, height);
        roundedRectRiseShader.setUniformf("u_radius", radius);
        roundedRectRiseShader.setUniformf("u_color", getRed(color), getGreen(color), getBlue(color), getAlpha(color));
        roundedRectRiseShader.setUniformf("u_edges", leftTop ? 1.0F : 0.0F, rightTop ? 1.0F : 0.0F, rightBottom ? 1.0F : 0.0F, leftBottom ? 1.0F : 0.0F);
        GlStateManager.func_179147_l();
        GlStateManager.func_179112_b(770, 771);
        ShaderUtils.drawQuads(x, y, width, height);
        GlStateManager.func_179084_k();
        GL20.glUseProgram(0);
        GlStateManager.func_179099_b();
        GL11.glPopMatrix();
    }

    public static void drawRoundedRectRise(double x, double y, double width, double height, double radius, int color) {
        drawRoundedRectRise((float) x, (float) y, (float) width, (float) height, (float) radius, color, true, true, true, true);
    }

    private static float getRed(int color) {
        return (color >> 16 & 0xFF) / 255.0F;
    }

    private static float getGreen(int color) {
        return (color >> 8 & 0xFF) / 255.0F;
    }

    private static float getBlue(int color) {
        return (color & 0xFF) / 255.0F;
    }

    private static float getAlpha(int color) {
        return (color >> 24 & 0xFF) / 255.0F;
    }
}
