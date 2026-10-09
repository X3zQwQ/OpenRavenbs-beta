package keystrokesmod.clickgui.components.impl;

import java.awt.Color;
import keystrokesmod.Raven;
import keystrokesmod.clickgui.components.Component;
import keystrokesmod.module.impl.client.Gui;
import keystrokesmod.module.setting.impl.ColorSetting;
import keystrokesmod.utility.RenderUtils;
import keystrokesmod.utility.Timer;
import keystrokesmod.utility.font.RavenFontRenderer;
import org.lwjgl.opengl.GL11;

public class ColorComponent extends Component {

    public ColorSetting colorSetting;

    private ModuleComponent moduleComponent;

    public float o;

    public float x;

    private float y;

    public float xOffset;

    public boolean expanded;

    private int dragMode;

    private float cachedHue;

    private float cachedSat;

    private float cachedBri;

    private Timer smoothTimer;

    private float animationProgress;

    private float animationStartProgress;

    private float animationTargetProgress;

    private static final float ANIMATION_DURATION = 250.0F;

    private static final float LABEL_HEIGHT = 12.0F;

    private static final float SQUARE_SIZE = 50.0F;

    private static final float HUE_BAR_WIDTH = 10.0F;

    private static final float HUE_GAP = 4.0F;

    private static final float BLACK_BRI_EPSILON = 0.001F;

    private static final float GREY_SAT_EPSILON = 0.001F;

    private static final float ALPHA_BAR_WIDTH = 10.0F;

    private static final float ALPHA_GAP = 4.0F;

    private static final float SQUARE_TOP_PAD = 2.0F;

    private static final float BOTTOM_PAD = 2.0F;

    private static final int HUE_STEPS = 20;

    private static final float PREVIEW_BOX_SIZE = 5.0F;

    public ColorComponent(ColorSetting colorSetting, ModuleComponent moduleComponent, float o) {
        this.colorSetting = colorSetting;
        this.moduleComponent = moduleComponent;
        this.o = o;
        this.animationProgress = 0.0F;
        this.animationStartProgress = 0.0F;
        this.animationTargetProgress = 0.0F;
    }

    public float getExpandedHeight() {
        return 66.0F;
    }

    public float getAnimationProgress() {
        if (this.smoothTimer != null) {
            if ((float) (System.currentTimeMillis() - this.smoothTimer.last) >= 280.0F) {
                this.smoothTimer = null;
                this.animationProgress = this.animationTargetProgress;
                this.animationStartProgress = this.animationTargetProgress;
            } else {
                this.animationProgress = this.smoothTimer.getValueFloat(this.animationStartProgress, this.animationTargetProgress, 1);
                if (this.animationProgress == this.animationTargetProgress) {
                    this.smoothTimer = null;
                    this.animationStartProgress = this.animationTargetProgress;
                }
            }
        }
        return this.animationProgress;
    }

    @Override
    public void render() {
        float cx = this.moduleComponent.categoryComponent.getX();
        float cy = this.moduleComponent.categoryComponent.getY();
        float cw = this.moduleComponent.categoryComponent.getWidth();
        float boxX = cx + 4.0F + this.xOffset / 2.0F;
        float boxY = cy + this.o + 3.0F;
        RenderUtils.drawRect(boxX - 0.5, boxY - 0.5, boxX + 5.0F + 0.5, boxY + 5.0F + 0.5, -12829626);
        if (this.colorSetting.hasAlpha()) {
            int checkSize = 2;
            for (float px = boxX; px < boxX + 5.0F; px += checkSize) {
                for (float py = boxY; py < boxY + 5.0F; py += checkSize) {
                    int col = ((int) ((px - boxX) / checkSize) + (int) ((py - boxY) / checkSize)) % 2 == 0 ? -10066330 : -6710887;
                    RenderUtils.drawRect(px, py, Math.min(px + checkSize, boxX + 5.0F), Math.min(py + checkSize, boxY + 5.0F), col);
                }
            }
        }
        RenderUtils.drawRect(boxX, boxY, boxX + 5.0F, boxY + 5.0F, this.colorSetting.getColor());
        RavenFontRenderer renderer = Gui.getClickGuiSettingFontRenderer();
        GL11.glPushMatrix();
        GL11.glScaled(0.5, 0.5, 0.5);
        float textOffset = renderer.getStringWidth("[+]  ");
        renderer.drawString(this.colorSetting.getName(), (cx + 4.0F) * 2.0F + this.xOffset + textOffset, (cy + this.o + 4.0F) * 2.0F, -1, true);
        GL11.glPopMatrix();
        float progress = this.getAnimationProgress();
        if (!(progress <= 0.0F)) {
            float scrollOffset = this.moduleComponent.categoryComponent.moduleY - cy;
            float contentTopScreen = cy + this.o + 12.0F + scrollOffset;
            float revealH = (this.getExpandedHeight() - 12.0F) * progress;
            RenderUtils.scissorPushGui(cx, contentTopScreen, cw, revealH);
            this.renderPickerContent(cx, cy);
            RenderUtils.scissorPop();
        }
    }

    private void renderPickerContent(float cx, float cy) {
        float areaLeft = cx + 4.0F + this.xOffset / 2.0F;
        float sqTop = cy + this.o + 12.0F + 2.0F;
        float sqRight = areaLeft + 50.0F;
        float sqBottom = sqTop + 50.0F;
        float bri = this.dragMode != 0 ? this.cachedBri : this.colorSetting.getBrightness();
        float satFromSetting = this.dragMode != 0 ? this.cachedSat : this.colorSetting.getSaturation();
        boolean isBlack = bri < 0.001F;
        boolean isGrey = satFromSetting < 0.001F;
        if (this.dragMode == 0 && !isBlack) {
            this.cachedBri = bri;
            this.cachedSat = this.colorSetting.getSaturation();
            if (!isGrey) {
                this.cachedHue = this.colorSetting.getHue();
            }
        }
        boolean useCachedHue = this.dragMode != 0 || isBlack || isGrey;
        float hue = useCachedHue ? this.cachedHue / 360.0F : this.colorSetting.getHue() / 360.0F;
        float sat = this.dragMode == 0 && !isBlack ? satFromSetting : this.cachedSat;
        int hueRGB = Color.HSBtoRGB(hue, 1.0F, 1.0F) | 0xFF000000;
        RenderUtils.drawRect(areaLeft, sqTop, sqRight, sqBottom, hueRGB);
        RenderUtils.drawHorizontalGradientRect(areaLeft, sqTop, sqRight, sqBottom, -1, 16777215);
        RenderUtils.drawVerticalGradientRect(areaLeft, sqTop, sqRight, sqBottom, 0, -16777216);
        RenderUtils.drawOutline(areaLeft - 1.0F, sqTop - 1.0F, sqRight + 1.0F, sqBottom + 1.0F, 1.0F, -12829626);
        float indX = areaLeft + sat * 50.0F;
        float indY = sqTop + (1.0F - bri) * 50.0F;
        RenderUtils.drawRect(indX - 2.0F, indY, indX + 3.0F, indY + 1.0F, -1);
        RenderUtils.drawRect(indX, indY - 2.0F, indX + 1.0F, indY + 3.0F, -1);
        float hueLeft = sqRight + 4.0F;
        float hueRight = hueLeft + 10.0F;
        float stepH = 2.5F;
        for (int i = 0; i < 20; i++) {
            float h1 = i / 20.0F;
            float h2 = (i + 1) / 20.0F;
            int c1 = Color.HSBtoRGB(h1, 1.0F, 1.0F) | 0xFF000000;
            int c2 = Color.HSBtoRGB(h2, 1.0F, 1.0F) | 0xFF000000;
            RenderUtils.drawVerticalGradientRect(hueLeft, sqTop + i * stepH, hueRight, sqTop + (i + 1) * stepH, c1, c2);
        }
        RenderUtils.drawOutline(hueLeft - 1.0F, sqTop - 1.0F, hueRight + 1.0F, sqBottom + 1.0F, 1.0F, -12829626);
        float hueIndY = sqTop + Math.max(0.0F, Math.min(1.0F, hue)) * 50.0F;
        RenderUtils.drawRect(hueLeft - 1.0F, hueIndY - 1.0F, hueRight + 1.0F, hueIndY + 2.0F, -1);
        if (this.colorSetting.hasAlpha()) {
            float alphaLeft = hueRight + 4.0F;
            float alphaRight = alphaLeft + 10.0F;
            int checkSize = 4;
            for (float ax = alphaLeft; ax < alphaRight; ax += checkSize) {
                for (float ay = sqTop; ay < sqBottom; ay += checkSize) {
                    int col = ((int) ((ax - alphaLeft) / checkSize) + (int) ((ay - sqTop) / checkSize)) % 2 == 0 ? -10066330 : -6710887;
                    RenderUtils.drawRect(ax, ay, Math.min(ax + checkSize, alphaRight), Math.min(ay + checkSize, sqBottom), col);
                }
            }
            int rgb = this.colorSetting.getRGB();
            RenderUtils.drawVerticalGradientRect(alphaLeft, sqTop, alphaRight, sqBottom, rgb & 16777215, rgb | 0xFF000000);
            RenderUtils.drawOutline(alphaLeft - 1.0F, sqTop - 1.0F, alphaRight + 1.0F, sqBottom + 1.0F, 1.0F, -12829626);
            float alphaFrac = this.colorSetting.getAlpha() / 255.0F;
            float alphaIndY = sqTop + alphaFrac * 50.0F;
            RenderUtils.drawRect(alphaLeft - 1.0F, alphaIndY - 1.0F, alphaRight + 1.0F, alphaIndY + 2.0F, -1);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY) {
        this.y = this.moduleComponent.categoryComponent.getModuleY() + this.o;
        this.x = this.moduleComponent.categoryComponent.getX();
        if (this.dragMode != 0 && !(this.getAnimationProgress() < 1.0F)) {
            float areaLeft = this.x + 4.0F + this.xOffset / 2.0F;
            float sqTop = this.y + 12.0F + 2.0F;
            float sqRight = areaLeft + 50.0F;
            float sqBottom = sqTop + 50.0F;
            float hueLeft = sqRight + 4.0F;
            float hueRight = hueLeft + 10.0F;
            if (this.dragMode == 1) {
                this.cachedSat = Math.max(0.0F, Math.min(1.0F, (mouseX - areaLeft) / 50.0F));
                this.cachedBri = Math.max(0.0F, Math.min(1.0F, 1.0F - (mouseY - sqTop) / 50.0F));
                this.colorSetting.setFromHSB(this.cachedHue, this.cachedSat, this.cachedBri);
                this.markUnsaved();
            } else if (this.dragMode == 2) {
                this.cachedHue = Math.max(0.0F, Math.min(360.0F, (mouseY - sqTop) / 50.0F * 360.0F));
                this.colorSetting.setFromHSB(this.cachedHue, this.cachedSat, this.cachedBri);
                this.markUnsaved();
            } else if (this.dragMode == 3 && this.colorSetting.hasAlpha()) {
                float a = Math.max(0.0F, Math.min(1.0F, (mouseY - sqTop) / 50.0F));
                this.colorSetting.setAlpha((int) (a * 255.0F));
                this.markUnsaved();
            }
        }
    }

    @Override
    public boolean onClick(int mouseX, int mouseY, int button) {
        if (this.moduleComponent.isOpened && this.moduleComponent.isVisible(this)) {
            float cw = this.moduleComponent.categoryComponent.getWidth();
            if (!(mouseX > this.x) || !(mouseX < this.x + cw) || !(mouseY > this.y) || !(mouseY < this.y + 12.0F) || button != 0 && button != 1) {
                if (button != 0) {
                    return false;
                } else if (this.getAnimationProgress() < 1.0F) {
                    return false;
                } else {
                    float areaLeft = this.x + 4.0F + this.xOffset / 2.0F;
                    float sqTop = this.y + 12.0F + 2.0F;
                    float sqRight = areaLeft + 50.0F;
                    float sqBottom = sqTop + 50.0F;
                    float hueLeft = sqRight + 4.0F;
                    float hueRight = hueLeft + 10.0F;
                    if (mouseX >= areaLeft && mouseX <= sqRight && mouseY >= sqTop && mouseY <= sqBottom) {
                        this.cacheHSB();
                        this.dragMode = 1;
                        return false;
                    } else if (mouseX >= hueLeft - 2.0F && mouseX <= hueRight + 2.0F && mouseY >= sqTop && mouseY <= sqBottom) {
                        this.cacheHSB();
                        this.dragMode = 2;
                        return false;
                    } else {
                        if (this.colorSetting.hasAlpha()) {
                            float alphaLeft = hueRight + 4.0F;
                            float alphaRight = alphaLeft + 10.0F;
                            if (mouseX >= alphaLeft - 2.0F && mouseX <= alphaRight + 2.0F && mouseY >= sqTop && mouseY <= sqBottom) {
                                this.cacheHSB();
                                this.dragMode = 3;
                                return false;
                            }
                        }
                        return false;
                    }
                }
            } else {
                float currentProgress = this.getAnimationProgress();
                this.animationStartProgress = currentProgress;
                this.expanded = !this.expanded;
                this.animationTargetProgress = this.expanded ? 1.0F : 0.0F;
                (this.smoothTimer = new Timer(250.0F)).start();
                this.moduleComponent.updateSettingPositions();
                return true;
            }
        } else {
            return false;
        }
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int button) {
        this.dragMode = 0;
    }

    @Override
    public void onGuiClosed() {
        this.dragMode = 0;
        this.smoothTimer = null;
        this.animationProgress = this.expanded ? 1.0F : 0.0F;
        this.animationStartProgress = this.animationProgress;
        this.animationTargetProgress = this.animationProgress;
    }

    @Override
    public void updateHeight(float n) {
        this.o = n;
    }

    @Override
    public float getOffset() {
        return this.o;
    }

    @Override
    public boolean isBaseVisible() {
        return this.colorSetting.visible;
    }

    public void restoreExpandedState(boolean expanded) {
        this.expanded = expanded;
        this.smoothTimer = null;
        this.animationProgress = expanded ? 1.0F : 0.0F;
        this.animationStartProgress = this.animationProgress;
        this.animationTargetProgress = this.animationProgress;
    }

    private void cacheHSB() {
        float bri = this.colorSetting.getBrightness();
        float sat = this.colorSetting.getSaturation();
        this.cachedBri = bri;
        if (bri >= 0.001F) {
            this.cachedSat = sat;
            if (sat >= 0.001F) {
                this.cachedHue = this.colorSetting.getHue();
            }
        }
    }

    private void markUnsaved() {
        if (Raven.currentProfile != null) {
            Raven.currentProfile.getModule().saved = false;
        }
    }
}
