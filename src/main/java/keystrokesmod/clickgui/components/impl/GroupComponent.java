package keystrokesmod.clickgui.components.impl;

import keystrokesmod.clickgui.components.Component;
import keystrokesmod.module.impl.client.Gui;
import keystrokesmod.module.setting.impl.GroupSetting;
import keystrokesmod.utility.Timer;
import keystrokesmod.utility.font.RavenFontRenderer;
import org.lwjgl.opengl.GL11;

public class GroupComponent extends Component {

    public GroupSetting setting;

    private ModuleComponent component;

    public float o;

    private float x;

    private float y;

    public boolean opened;

    private Timer smoothTimer;

    private float animationProgress;

    private float animationStartProgress;

    private float animationTargetProgress;

    private static final float ANIMATION_DURATION = 250.0F;

    public GroupComponent(GroupSetting setting, ModuleComponent moduleComponent, float o) {
        this.setting = setting;
        this.component = moduleComponent;
        this.o = o;
        this.x = moduleComponent.categoryComponent.getX() + moduleComponent.categoryComponent.getWidth();
        this.y = moduleComponent.categoryComponent.getY() + moduleComponent.yPos;
        this.opened = setting.isOpened();
        this.animationProgress = this.opened ? 1.0F : 0.0F;
        this.animationStartProgress = this.animationProgress;
        this.animationTargetProgress = this.animationProgress;
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
        float progress = this.getAnimationProgress();
        RavenFontRenderer renderer = Gui.getClickGuiSettingFontRenderer();
        GL11.glPushMatrix();
        GL11.glScaled(0.5, 0.5, 0.5);
        float strX = (this.component.categoryComponent.getX() + 4.0F) * 2.0F + 1.0F;
        float strY = (this.component.categoryComponent.getY() + this.o + 4.0F) * 2.0F;
        this.drawString(renderer, "[", strX, strY);
        int firstBracketWidth = renderer.getStringWidth("[");
        int fontHeight = renderer.getFontHeight();
        float arrowWidth = Math.max(6.0F, fontHeight * 0.45F);
        float arrowX = strX + firstBracketWidth;
        float centerX = arrowX + arrowWidth / 2.0F;
        float centerY = strY + fontHeight / 2.0F - 1.5F;
        float chevronSize = Math.max(4.5F, fontHeight * 0.3F);
        GL11.glPushMatrix();
        GL11.glTranslatef(centerX, centerY, 0.0F);
        GL11.glRotatef(90.0F * progress, 0.0F, 0.0F, 1.0F);
        this.drawChevron(chevronSize);
        GL11.glPopMatrix();
        this.drawString(renderer, "]  " + this.setting.getName(), strX + firstBracketWidth + arrowWidth, strY);
        GL11.glPopMatrix();
    }

    private void drawChevron(float size) {
        float half = size / 2.0F;
        GL11.glEnable(3042);
        GL11.glBlendFunc(770, 771);
        GL11.glEnable(2848);
        GL11.glDisable(3553);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glLineWidth(1.5F);
        GL11.glBegin(3);
        GL11.glVertex2f(-half, -half);
        GL11.glVertex2f(half, 0.0F);
        GL11.glVertex2f(-half, half);
        GL11.glEnd();
        GL11.glLineWidth(1.0F);
        GL11.glEnable(3553);
        GL11.glDisable(2848);
        GL11.glDisable(3042);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
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
    public void drawScreen(int x, int y) {
        this.y = this.component.categoryComponent.getModuleY() + this.o;
        this.x = this.component.categoryComponent.getX();
    }

    @Override
    public boolean onClick(int x, int y, int b) {
        if (this.overGroup(x, y) && (b == 0 || b == 1) && this.component.isOpened) {
            float currentProgress = this.getAnimationProgress();
            this.animationStartProgress = currentProgress;
            this.opened = !this.opened;
            this.setting.setOpened(this.opened);
            this.animationTargetProgress = this.opened ? 1.0F : 0.0F;
            (this.smoothTimer = new Timer(250.0F)).start();
            this.component.updateSettingPositions();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void onGuiClosed() {
        this.smoothTimer = null;
        this.animationProgress = this.opened ? 1.0F : 0.0F;
        this.animationStartProgress = this.animationProgress;
        this.animationTargetProgress = this.animationProgress;
    }

    public boolean overGroup(int x, int y) {
        return x > this.x && x < this.x + this.component.categoryComponent.getWidth() && y > this.y && y < this.y + 11.0F;
    }

    private void drawString(RavenFontRenderer renderer, String text, float x, float y) {
        renderer.drawString(text, x, y, -1, false);
    }
}
