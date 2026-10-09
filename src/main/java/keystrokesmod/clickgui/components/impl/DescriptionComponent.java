package keystrokesmod.clickgui.components.impl;

import keystrokesmod.clickgui.components.Component;
import keystrokesmod.module.impl.client.Gui;
import keystrokesmod.module.setting.impl.DescriptionSetting;
import keystrokesmod.utility.Theme;
import keystrokesmod.utility.font.RavenFontRenderer;
import org.lwjgl.opengl.GL11;

public class DescriptionComponent extends Component {

    public DescriptionSetting desc;

    private ModuleComponent p;

    public float o;

    public float x;

    public float y;

    public DescriptionComponent(DescriptionSetting desc, ModuleComponent b, float o) {
        this.desc = desc;
        this.p = b;
        this.x = b.categoryComponent.getX() + b.categoryComponent.getWidth();
        this.y = b.categoryComponent.getY() + b.yPos;
        this.o = o;
    }

    @Override
    public void render() {
        RavenFontRenderer renderer = Gui.getClickGuiSettingFontRenderer();
        GL11.glPushMatrix();
        GL11.glScaled(0.5, 0.5, 0.5);
        renderer.drawString(this.desc.getDesc(), (this.p.categoryComponent.getX() + 4.0F) * 2.0F, (this.p.categoryComponent.getY() + this.o + 4.0F) * 2.0F, Theme.getGradient(Theme.descriptor[0], Theme.descriptor[1], 0.0), true);
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
        return this.desc.visible;
    }
}
