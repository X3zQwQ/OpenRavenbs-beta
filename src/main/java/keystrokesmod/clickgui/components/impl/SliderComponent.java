package keystrokesmod.clickgui.components.impl;

import java.awt.Color;
import java.math.BigDecimal;
import java.math.RoundingMode;
import keystrokesmod.Raven;
import keystrokesmod.clickgui.components.Component;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.client.Gui;
import keystrokesmod.module.impl.render.HUD;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.RenderUtils;
import keystrokesmod.utility.Utils;
import keystrokesmod.utility.font.FontManager;
import keystrokesmod.utility.font.RavenFontRenderer;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;

public class SliderComponent extends Component {

    public SliderSetting sliderSetting;

    private ModuleComponent moduleComponent;

    public float o;

    public float x;

    private float y;

    public boolean heldDown = false;

    private double width;

    public float xOffset;

    private double targetValue;

    private double displayedValue;

    private static final double SLIDER_SPEED = 0.6;

    private static final float SLIDER_BAR_HEIGHT = 4.0F;

    public SliderComponent(SliderSetting sliderSetting, ModuleComponent moduleComponent, float o) {
        this.sliderSetting = sliderSetting;
        this.moduleComponent = moduleComponent;
        this.o = o;
        double initial = sliderSetting.getInput() == -1.0 && sliderSetting.canBeDisabled ? -1.0 : sliderSetting.getInput();
        this.targetValue = initial;
        this.displayedValue = initial;
        this.width = this.sliderSetting.getInput() == -1.0 ? 0.0 : (this.moduleComponent.categoryComponent.getWidth() - 8.0F) * (this.sliderSetting.getInput() - this.sliderSetting.getMin()) / (this.sliderSetting.getMax() - this.sliderSetting.getMin());
    }

    @Override
    public void render() {
        float barLeft = this.moduleComponent.categoryComponent.getX() + 4.0F + this.xOffset / 2.0F;
        float barTop = this.moduleComponent.categoryComponent.getY() + this.o + 11.0F;
        float barRight = this.moduleComponent.categoryComponent.getX() + 4.0F + this.moduleComponent.categoryComponent.getWidth() - 8.0F;
        float barBottom = barTop + 4.0F;
        RenderUtils.drawRoundedRectangle(barLeft, barTop, barRight, barBottom, 4.0F, -12302777);
        float fillRight = (float) Math.min((double) barRight, barLeft + this.width);
        float fillWidth = Math.max(0.0F, fillRight - barLeft);
        float fillHeight = Math.min(4.0F, fillWidth);
        float fillTop = barTop + (4.0F - fillHeight) / 2.0F;
        float fillBottom = fillTop + fillHeight;
        RenderUtils.drawRoundedRectangle(barLeft, fillTop, fillRight, fillBottom, 4.0F, Color.getHSBColor((float) (System.currentTimeMillis() % 11000L) / 11000.0F, 0.75F, 0.9F).getRGB());
        GL11.glPushMatrix();
        GL11.glScaled(0.5, 0.5, 0.5);
        double input = this.getRenderedInputValue();
        String suffix = this.sliderSetting.getSuffix();
        String valueText;
        if (input == -1.0 && this.sliderSetting.canBeDisabled) {
            valueText = this.sliderSetting.getMinString() != null ? this.sliderSetting.getMinString() : "§cDisabled";
            suffix = "";
        } else if (this.sliderSetting.getMinString() != null && input == this.sliderSetting.getMin()) {
            valueText = this.sliderSetting.getMinString();
            suffix = "";
        } else {
            if (input != 1.0 && (suffix.equals(" second") || suffix.equals(" block") || suffix.equals(" tick")) && this.moduleComponent.mod.moduleCategory() != Module.category.scripts) {
                suffix = suffix + "s";
            }
            if (this.sliderSetting.isString) {
                int intValue = (int) Math.round(input);
                intValue = Math.max(0, Math.min(intValue, this.sliderSetting.getOptions().length - 1));
                valueText = this.sliderSetting.getOptions()[intValue];
            } else {
                valueText = Utils.asWholeNum(input);
            }
        }
        float labelX = (this.moduleComponent.categoryComponent.getX() + 4.0F) * 2.0F + this.xOffset;
        float labelY = (this.moduleComponent.categoryComponent.getY() + this.o + 3.0F) * 2.0F;
        if (this.shouldPreviewFontSlider()) {
            this.drawFontPreview(labelX, labelY, valueText, suffix);
        } else {
            Gui.getClickGuiSettingFontRenderer().drawString(this.sliderSetting.getName() + ": " + (this.sliderSetting.isString ? "§e" : "§b") + valueText + suffix, labelX, labelY, -1, true);
        }
        GL11.glPopMatrix();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY) {
        this.y = this.moduleComponent.categoryComponent.getModuleY() + this.o;
        this.x = this.moduleComponent.categoryComponent.getX();
        if (this.heldDown) {
            double d = Math.min(this.moduleComponent.categoryComponent.getWidth() - 8.0F, Math.max(0.0F, mouseX - this.x));
            if (d == 0.0 && this.sliderSetting.canBeDisabled) {
                this.targetValue = -1.0;
            } else {
                double n = roundToInterval(d / (this.moduleComponent.categoryComponent.getWidth() - 8.0F) * (this.sliderSetting.getMax() - this.sliderSetting.getMin()) + this.sliderSetting.getMin(), 4);
                this.targetValue = n;
            }
            this.displayedValue = this.displayedValue + (this.targetValue - this.displayedValue) * 0.6;
            if (!this.shouldCommitOnRelease()) {
                if (this.targetValue == -1.0) {
                    this.sliderSetting.setValueRaw(-1.0);
                } else {
                    this.sliderSetting.setValue(this.targetValue);
                }
            }
            if (this.displayedValue == -1.0) {
                this.width = 0.0;
            } else {
                double range = this.sliderSetting.getMax() - this.sliderSetting.getMin();
                double fraction = (this.displayedValue - this.sliderSetting.getMin()) / range;
                this.width = (this.moduleComponent.categoryComponent.getWidth() - 8.0F) * fraction;
            }
            if (this.sliderSetting.getInput() != this.sliderSetting.getMin() && ModuleManager.hud != null && ModuleManager.hud.isEnabled() && !ModuleManager.organizedModules.isEmpty()) {
                ModuleManager.sort();
            }
            if (Raven.currentProfile != null) {
                Raven.currentProfile.getModule().saved = false;
            }
        }
    }

    public void onSliderChange() {
        double initial = this.sliderSetting.getInput() == -1.0 && this.sliderSetting.canBeDisabled ? -1.0 : this.sliderSetting.getInput();
        this.targetValue = initial;
        this.displayedValue = initial;
        this.width = this.sliderSetting.getInput() == -1.0 ? 0.0 : (this.moduleComponent.categoryComponent.getWidth() - 8.0F) * (this.sliderSetting.getInput() - this.sliderSetting.getMin()) / (this.sliderSetting.getMax() - this.sliderSetting.getMin());
    }

    public boolean isHovered(int mouseX, int mouseY) {
        return (this.u(mouseX, mouseY) || this.i(mouseX, mouseY)) && this.moduleComponent.isOpened && this.moduleComponent.isVisible(this);
    }

    public void adjustValue(int direction) {
        if (direction != 0) {
            double previousValue = this.sliderSetting.getInput();
            if (this.sliderSetting.canBeDisabled && previousValue == -1.0) {
                if (direction > 0) {
                    this.sliderSetting.setValue(this.sliderSetting.getMin());
                }
            } else if (this.sliderSetting.canBeDisabled && direction < 0 && previousValue <= this.sliderSetting.getMin()) {
                this.sliderSetting.setValueRaw(-1.0);
            } else {
                this.sliderSetting.setValue(previousValue + direction * this.sliderSetting.getInterval());
            }
            if (Double.compare(previousValue, this.sliderSetting.getInput()) != 0) {
                this.onSliderChange();
                if (ModuleManager.hud != null && ModuleManager.hud.isEnabled() && !ModuleManager.organizedModules.isEmpty()) {
                    ModuleManager.sort();
                }
                if (Raven.currentProfile != null) {
                    Raven.currentProfile.getModule().saved = false;
                }
                if (this.shouldCommitOnRelease()) {
                    Raven.clickGui.requestScaleRefresh();
                }
            }
        }
    }

    private static double roundToInterval(double value, int places) {
        if (places < 0) {
            return 0.0;
        } else {
            BigDecimal bd = new BigDecimal(value);
            bd = bd.setScale(places, RoundingMode.HALF_UP);
            return bd.doubleValue();
        }
    }

    @Override
    public boolean onClick(int mouseX, int mouseY, int button) {
        if ((this.u(mouseX, mouseY) || this.i(mouseX, mouseY)) && button == 0 && this.moduleComponent.isOpened && this.moduleComponent.isVisible(this)) {
            this.heldDown = true;
        }
        return false;
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int button) {
        boolean wasHeldDown = this.heldDown;
        this.heldDown = false;
        if (button == 0 && wasHeldDown && this.shouldCommitOnRelease()) {
            if (this.targetValue == -1.0) {
                this.sliderSetting.setValueRaw(-1.0);
            } else {
                this.sliderSetting.setValue(this.targetValue);
            }
            this.onSliderChange();
            Raven.clickGui.requestScaleRefresh();
        }
    }

    public boolean u(int mouseX, int mouseY) {
        return mouseX > this.x && mouseX < this.x + this.moduleComponent.categoryComponent.getWidth() / 2.0F + 1.0F && mouseY > this.y && mouseY < this.y + 16.0F;
    }

    public boolean i(int mouseX, int mouseY) {
        return mouseX > this.x + this.moduleComponent.categoryComponent.getWidth() / 2.0F && mouseX < this.x + this.moduleComponent.categoryComponent.getWidth() && mouseY > this.y && mouseY < this.y + 16.0F;
    }

    @Override
    public void onGuiClosed() {
        this.heldDown = false;
    }

    @Override
    public void updateHeight(float n) {
        this.o = n;
    }

    private boolean shouldPreviewFontSlider() {
        return this.sliderSetting.isString && (this.moduleComponent.mod instanceof HUD && this.sliderSetting == HUD.font || this.moduleComponent.mod instanceof Gui && this.sliderSetting == Gui.font);
    }

    private boolean shouldCommitOnRelease() {
        return this.moduleComponent.mod instanceof Gui && this.sliderSetting == Gui.guiScale;
    }

    private double getRenderedInputValue() {
        return this.shouldCommitOnRelease() && this.heldDown ? this.targetValue : this.sliderSetting.getInput();
    }

    private void drawFontPreview(float labelX, float labelY, String valueText, String suffix) {
        String prefix = this.sliderSetting.getName() + ": ";
        Minecraft mc = Minecraft.func_71410_x();
        mc.field_71466_p.func_175063_a(prefix, labelX, labelY, -1);
        RavenFontRenderer previewRenderer = FontManager.getClickGuiSettingRenderer(valueText);
        float valueX = labelX + mc.field_71466_p.func_78256_a(prefix);
        float valueY = labelY - (previewRenderer.getFontHeight() - mc.field_71466_p.field_78288_b) / 2.0F;
        previewRenderer.drawString(valueText + suffix, valueX, valueY, 16777215, true);
    }

    @Override
    public float getOffset() {
        return this.o;
    }

    @Override
    public boolean isBaseVisible() {
        return this.sliderSetting.visible;
    }
}
