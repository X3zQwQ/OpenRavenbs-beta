package keystrokesmod.clickgui.components.impl;

import java.util.List;
import keystrokesmod.clickgui.animation.ScrollOffsetAnimation;
import keystrokesmod.module.impl.client.Gui;
import keystrokesmod.module.setting.impl.StringListSetting;
import keystrokesmod.utility.RenderUtils;
import keystrokesmod.utility.Theme;
import net.minecraft.util.ResourceLocation;

public class StringListComponent extends AbstractTextInputComponent {

    private static final String CLOSE_ICON_PATH = "/assets/keystrokesmod/textures/gui/close.png";

    private static final float SELECTED_LIST_GAP = 4.0F;

    private static final int MAX_VISIBLE_SELECTED = 7;

    private static final int CLOSE_SIZE = 6;

    private static final float CLOSE_PAD = 3.0F;

    private static final float LIST_ROW_TEXT_SCALE = 0.56F;

    private static final float LIST_ROW_TEXT_Y_OFFSET = 0.56F;

    private static final float LIST_ROW_VISUAL_HEIGHT = 11.0F;

    private final StringListSetting setting;

    private final ScrollOffsetAnimation selectedScrollAnim = new ScrollOffsetAnimation(200L);

    private float lastMouseX;

    private float lastMouseY;

    public StringListComponent(StringListSetting setting, ModuleComponent moduleComponent, float o) {
        super(moduleComponent, o, setting.getPlaceholder(), setting.getMaxLength());
        this.setting = setting;
    }

    @Override
    public void render() {
        AbstractTextInputComponent.Layout layout = this.layout(false);
        this.renderLabel(layout, this.setting.getName());
        this.renderTextField(layout);
        this.renderSelectedEntries(layout);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY) {
        super.drawScreen(mouseX, mouseY);
        this.lastMouseX = mouseX;
        this.lastMouseY = mouseY;
        this.clampSelectedScroll();
    }

    @Override
    public boolean onClick(int mouseX, int mouseY, int button) {
        if (this.moduleComponent.isOpened && this.moduleComponent.isVisible(this)) {
            AbstractTextInputComponent.Layout layout = this.layout(true);
            if (button == 0 && this.isTextFieldClicked(mouseX, mouseY, layout)) {
                this.setTextFieldFocused(true);
                return true;
            } else if (button == 0 && this.handleSelectedEntryClick(mouseX, mouseY, layout)) {
                return true;
            } else {
                if (this.isTextFieldFocused()) {
                    this.getTextField().setText("");
                    this.setTextFieldFocused(false);
                }
                return false;
            }
        } else {
            return false;
        }
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        if (this.moduleComponent.isOpened && this.isTextFieldFocused()) {
            if (keyCode == 1) {
                this.getTextField().setText("");
                this.setTextFieldFocused(false);
            } else if (keyCode != 28 && keyCode != 156) {
                this.getTextField().textboxKeyTyped(typedChar, keyCode);
            } else {
                this.submitText();
                this.setTextFieldFocused(false);
            }
        }
    }

    @Override
    public void onScroll(int scroll) {
        if (this.moduleComponent.isOpened && this.moduleComponent.isVisible(this)) {
            if (this.capturesCategoryScroll(this.lastMouseX, this.lastMouseY)) {
                float scrollSpeed = (float) Gui.scrollSpeed.getInput();
                float delta = scrollSpeed * (scroll / 120.0F);
                if (delta != 0.0F) {
                    this.selectedScrollAnim.extend(-delta);
                }
                this.clampSelectedScroll();
            }
        }
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        this.getTextField().setText("");
        this.selectedScrollAnim.reset(0.0F);
    }

    @Override
    public float getHeightF() {
        int count = this.setting.getEntries().size();
        float selectedHeight = count == 0 ? 0.0F : 4.0F + Math.min(7, count) * 12.0F;
        return 24.0F + selectedHeight;
    }

    @Override
    public boolean isBaseVisible() {
        return this.setting.visible;
    }

    @Override
    public String getGroupName() {
        return this.setting.group != null ? this.setting.group.getName() : "";
    }

    public boolean capturesCategoryScroll(float mouseX, float mouseY) {
        return this.setting.getEntries().size() > 7 && this.isMouseOverSelectedList(mouseX, mouseY);
    }

    @Override
    public boolean containsClick(int mouseX, int mouseY) {
        AbstractTextInputComponent.Layout layout = this.layout(true);
        return this.isTextFieldClicked(mouseX, mouseY, layout) || this.isMouseOverSelectedList(mouseX, mouseY);
    }

    private void submitText() {
        String typed = this.getTextField().getText();
        if (typed != null && !typed.trim().isEmpty()) {
            if (this.setting.addEntry(typed)) {
                this.getTextField().setText("");
            }
            this.moduleComponent.updateSettingPositions();
            this.clampSelectedScroll();
        }
    }

    private void renderSelectedEntries(AbstractTextInputComponent.Layout layout) {
        List<String> entries = this.setting.getEntries();
        if (!entries.isEmpty()) {
            float selectedTop = this.getSelectedTop(layout);
            float selectedHeight = this.getSelectedVisibleHeight(entries.size());
            float scrollOffset = this.moduleComponent.categoryComponent.getModuleY() - layout.cy;
            RenderUtils.scissorPushGui(layout.left, selectedTop + scrollOffset, layout.right - layout.left, selectedHeight);
            float value = this.selectedScrollAnim.getValue();
            int firstRow = (int) (value / 12.0F);
            int end = Math.min(firstRow + 7 + 1, entries.size());
            for (int i = firstRow; i < end; i++) {
                String entry = entries.get(i);
                float rowTop = selectedTop - value + i * 12.0F;
                int bg = i % 2 == 0 ? -15066582 : -14803410;
                this.renderEntryRow(entry, layout.left, layout.right, rowTop, bg);
            }
            RenderUtils.scissorPop();
        }
    }

    private void renderEntryRow(String entry, float left, float right, float rowTop, int bgColor) {
        RenderUtils.drawRect(left, rowTop, right, rowTop + 12.0F - 1.0F, bgColor);
        drawScaledTextNoShadow(entry, left + 4.0F, centeredScaledTextY(rowTop, 11.0F, 0.56F) + 0.56F, -3355444);
        this.renderCloseIcon(right, rowTop);
    }

    private boolean handleSelectedEntryClick(int mouseX, int mouseY, AbstractTextInputComponent.Layout layout) {
        List<String> entries = this.setting.getEntries();
        float value2 = this.selectedScrollAnim.getValue();
        for (int i = 0; i < entries.size(); i++) {
            float rowTop = this.getSelectedTop(layout) - value2 + i * 12.0F;
            if (this.isOverClose(mouseX, mouseY, rowTop, layout.right)) {
                this.setting.removeEntry(entries.get(i));
                this.moduleComponent.updateSettingPositions();
                this.clampSelectedScroll();
                return true;
            }
        }
        return false;
    }

    private void clampSelectedScroll() {
        int count = this.setting.getEntries().size();
        float floatValue = Math.max(0.0F, (count - 7) * 12.0F);
        this.selectedScrollAnim.clampTarget(0.0F, floatValue);
        if (this.selectedScrollAnim.getValue() > floatValue) {
            this.selectedScrollAnim.reset(floatValue);
        }
    }

    private boolean isMouseOverSelectedList(float mouseX, float mouseY) {
        List<String> entries = this.setting.getEntries();
        if (entries.isEmpty()) {
            return false;
        } else {
            AbstractTextInputComponent.Layout layout = this.layout(true);
            float selectedTop = this.getSelectedTop(layout);
            float selectedHeight = this.getSelectedVisibleHeight(entries.size());
            return mouseX >= layout.left && mouseX <= layout.right && mouseY >= selectedTop && mouseY < selectedTop + selectedHeight;
        }
    }

    private float getSelectedTop(AbstractTextInputComponent.Layout layout) {
        return layout.contentTop + 4.0F;
    }

    private float getSelectedVisibleHeight(int count) {
        return Math.min(7, count) * 12.0F;
    }

    private void renderCloseIcon(float right, float rowTop) {
        ResourceLocation close = RenderUtils.getIcon("/assets/keystrokesmod/textures/gui/close.png");
        if (close != null) {
            float closeX = right - 6.0F - 3.0F;
            float closeY = rowTop + 2.5F;
            int closeColor = Theme.getGradient(Theme.hiddenBind[0], Theme.hiddenBind[1], 0.0);
            RenderUtils.drawIcon(close, closeX, closeY, 6, closeColor);
        }
    }

    private boolean isOverClose(float mouseX, float mouseY, float rowTop, float right) {
        float closeX = right - 6.0F - 3.0F;
        float closeY = rowTop + 2.5F;
        return mouseX >= closeX && mouseX <= closeX + 6.0F && mouseY >= closeY && mouseY <= closeY + 6.0F;
    }

    private static void drawScaledTextNoShadow(String text, float x, float y, int color) {
        drawScaledText(text, x, y, color, 0.56F);
    }
}
