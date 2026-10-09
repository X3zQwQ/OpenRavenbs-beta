package keystrokesmod.clickgui.components.impl;

import keystrokesmod.clickgui.components.Component;
import keystrokesmod.clickgui.components.FocusableTextComponent;
import keystrokesmod.module.impl.client.Gui;
import keystrokesmod.utility.Theme;
import keystrokesmod.utility.font.RavenFontRenderer;
import org.lwjgl.opengl.GL11;

public abstract class AbstractTextInputComponent extends Component implements FocusableTextComponent {

    protected static final float ROW_HEIGHT = 12.0F;

    protected static final float TEXT_SCALE = 0.5F;

    protected static final int DEFAULT_TEXT_MAX_LENGTH = 128;

    protected final ModuleComponent moduleComponent;

    public float o;

    public float xOffset;

    private final ClickGuiTextField textField;

    protected AbstractTextInputComponent(ModuleComponent moduleComponent, float o, String placeholder, int maxLength) {
        this.moduleComponent = moduleComponent;
        this.o = o;
        this.textField = new ClickGuiTextField(placeholder, maxLength, 0.5F);
    }

    protected final ClickGuiTextField getTextField() {
        return this.textField;
    }

    protected final AbstractTextInputComponent.Layout layout(boolean useModuleY) {
        AbstractTextInputComponent.Layout layout = new AbstractTextInputComponent.Layout();
        layout.cx = this.moduleComponent.categoryComponent.getX();
        layout.cy = useModuleY ? this.moduleComponent.categoryComponent.getModuleY() : this.moduleComponent.categoryComponent.getY();
        layout.cw = this.moduleComponent.categoryComponent.getWidth();
        layout.left = layout.cx + 4.0F + this.xOffset / 2.0F;
        layout.right = layout.cx + layout.cw - 4.0F;
        layout.searchTop = layout.cy + this.o + 12.0F;
        layout.contentTop = layout.cy + this.o + 24.0F;
        return layout;
    }

    protected final void renderTextInput(String label) {
        AbstractTextInputComponent.Layout layout = this.layout(false);
        this.renderLabel(layout, label);
        this.renderTextField(layout);
    }

    protected final void renderLabel(AbstractTextInputComponent.Layout layout, String label) {
        drawScaledText(label, layout.left, centeredScaledTextY(layout.cy + this.o, 12.0F), Theme.getGradient(Theme.descriptor[0], Theme.descriptor[1], 0.0));
    }

    protected final void renderTextField(AbstractTextInputComponent.Layout layout) {
        float boxTop = layout.searchTop + 1.0F;
        float boxBottom = layout.searchTop + 12.0F - 1.0F;
        this.textField.render(layout.left, boxTop, layout.right, boxBottom);
    }

    protected final boolean isTextFieldClicked(int mouseX, int mouseY, AbstractTextInputComponent.Layout layout) {
        float boxTop = layout.searchTop + 1.0F;
        float boxBottom = layout.searchTop + 12.0F - 1.0F;
        return this.textField.contains(mouseX, mouseY, layout.left, boxTop, layout.right, boxBottom);
    }

    protected final float getTextFieldTop(AbstractTextInputComponent.Layout layout) {
        return layout.searchTop + 1.0F;
    }

    protected final float getTextFieldBottom(AbstractTextInputComponent.Layout layout) {
        return layout.searchTop + 12.0F - 1.0F;
    }

    protected final void setTextFieldFocused(boolean focused) {
        this.textField.setFocused(focused);
    }

    protected final boolean isTextFieldFocused() {
        return this.textField.isFocused();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY) {
        this.textField.tickCursor();
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
    public float getHeightF() {
        return 24.0F;
    }

    @Override
    public boolean isTextInputFocused() {
        return this.isTextFieldFocused();
    }

    @Override
    public boolean containsClick(int mouseX, int mouseY) {
        return this.isTextFieldClicked(mouseX, mouseY, this.layout(true));
    }

    @Override
    public void unfocusTextInput() {
        this.setTextFieldFocused(false);
    }

    @Override
    public void onGuiClosed() {
        this.setTextFieldFocused(false);
    }

    public void setXOffset(float xOffset) {
        this.xOffset = xOffset;
    }

    public abstract String getGroupName();

    protected static float centeredScaledTextY(float top, float height) {
        return centeredScaledTextY(top, height, 0.5F);
    }

    protected static float centeredScaledTextY(float top, float height, float scale) {
        RavenFontRenderer renderer = Gui.getClickGuiSettingFontRenderer();
        float textBoxHeight = Math.max(1.0F, (renderer.getTextBottomOffset() - renderer.getTextTopOffset()) * scale);
        return top + (height - textBoxHeight) / 2.0F - renderer.getTextTopOffset() * scale;
    }

    protected static void drawScaledText(String text, float x, float y, int color) {
        drawScaledText(text, x, y, color, 0.5F);
    }

    protected static void drawScaledText(String text, float x, float y, int color, float scale) {
        RavenFontRenderer renderer = Gui.getClickGuiSettingFontRenderer();
        GL11.glPushMatrix();
        GL11.glScaled(scale, scale, scale);
        renderer.drawString(text, x / scale, y / scale, color, false);
        GL11.glPopMatrix();
    }

    protected static final class Layout {

        float cx;

        float cy;

        float cw;

        float left;

        float right;

        float searchTop;

        float contentTop;
    }
}
