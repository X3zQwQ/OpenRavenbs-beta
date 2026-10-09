package keystrokesmod.clickgui.components.impl;

import java.awt.Color;
import keystrokesmod.Raven;
import keystrokesmod.clickgui.components.Component;
import keystrokesmod.module.Module;
import keystrokesmod.module.impl.client.Gui;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.utility.font.RavenFontRenderer;
import org.lwjgl.opengl.GL11;

public class ButtonComponent extends Component {

    private static final int ENABLED_COLOR = new Color(20, 255, 0).getRGB();

    private Module mod;

    public ButtonSetting buttonSetting;

    private ModuleComponent moduleComponent;

    public float o;

    public float x;

    private float y;

    public float xOffset;

    public ButtonComponent(Module mod, ButtonSetting op, ModuleComponent b, float o) {
        this.mod = mod;
        this.buttonSetting = op;
        this.moduleComponent = b;
        this.x = b.categoryComponent.getX() + b.categoryComponent.getWidth();
        this.y = b.categoryComponent.getY() + b.yPos;
        this.o = o;
    }

    @Override
    public void render() {
        RavenFontRenderer renderer = Gui.getClickGuiSettingFontRenderer();
        GL11.glPushMatrix();
        GL11.glScaled(0.5, 0.5, 0.5);
        renderer.drawString((this.buttonSetting.isMethodButton ? "[=]  " : (this.buttonSetting.isToggled() ? "[+]  " : "[-]  ")) + this.buttonSetting.getName(), (this.moduleComponent.categoryComponent.getX() + 4.0F) * 2.0F + this.xOffset, (this.moduleComponent.categoryComponent.getY() + this.o + 4.0F) * 2.0F, this.buttonSetting.isToggled() ? ENABLED_COLOR : -1, false);
        GL11.glScaled(1.0, 1.0, 1.0);
        GL11.glPopMatrix();
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
        return this.buttonSetting.visible;
    }

    @Override
    public void drawScreen(int x, int y) {
        this.y = this.moduleComponent.categoryComponent.getModuleY() + this.o;
        this.x = this.moduleComponent.categoryComponent.getX();
    }

    @Override
    public boolean onClick(int x, int y, int b) {
        if (this.i(x, y) && b == 0 && this.moduleComponent.isOpened && this.moduleComponent.isVisible(this)) {
            if (this.buttonSetting.isMethodButton) {
                this.buttonSetting.runMethod();
                return false;
            }
            this.buttonSetting.toggle();
            this.mod.guiButtonToggled(this.buttonSetting);
            if (Raven.currentProfile != null && !this.mod.ignoreOnSave) {
                Raven.currentProfile.getModule().saved = false;
            }
        }
        return false;
    }

    public boolean i(int x, int y) {
        return x > this.x && x < this.x + this.moduleComponent.categoryComponent.getWidth() && y > this.y && y < this.y + 11.0F;
    }
}
