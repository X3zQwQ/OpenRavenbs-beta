package keystrokesmod.clickgui.components.impl;

import java.util.List;
import keystrokesmod.Raven;
import keystrokesmod.clickgui.animation.ScrollOffsetAnimation;
import keystrokesmod.module.impl.client.Gui;
import keystrokesmod.utility.RenderUtils;
import keystrokesmod.utility.Theme;
import keystrokesmod.utility.Timer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

public abstract class AbstractSearchListComponent extends AbstractTextInputComponent {

    private static final String CLOSE_ICON_PATH = "/assets/keystrokesmod/textures/gui/close.png";

    private static final String ARROW_ICON_PATH = "/assets/keystrokesmod/textures/gui/arrow_left.png";

    protected static final float ANIMATION_DURATION = 250.0F;

    protected static final int MAX_VISIBLE_RESULTS = 7;

    protected static final int MAX_VISIBLE_SELECTED = 7;

    protected static final int CLOSE_SIZE = 6;

    protected static final float CLOSE_PAD = 3.0F;

    protected static final float SELECTED_LIST_GAP = 4.0F;

    protected static final float LIST_ROW_TEXT_SCALE = 0.56F;

    protected static final float LIST_ROW_TEXT_Y_OFFSET = 0.56F;

    protected static final float LIST_ROW_VISUAL_HEIGHT = 11.0F;

    protected final ScrollOffsetAnimation dropdownScrollAnim = new ScrollOffsetAnimation(200L);

    protected final ScrollOffsetAnimation selectedScrollAnim = new ScrollOffsetAnimation(200L);

    protected float lastMouseX;

    protected float lastMouseY;

    private Timer dropdownAnimTimer;

    private float dropdownAnimStartH;

    private float dropdownAnimTargetH;

    private float dropdownAnimH;

    protected AbstractSearchListComponent(ModuleComponent moduleComponent, float o, String placeholder) {
        super(moduleComponent, o, placeholder, 128);
    }

    @Override
    public void render() {
        AbstractTextInputComponent.Layout layout = this.layout(false);
        this.renderLabel(layout, this.getLabelText());
        this.renderTextField(layout);
        this.renderDropdown(layout);
        this.renderSelectedEntries(layout);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY) {
        super.drawScreen(mouseX, mouseY);
        this.lastMouseX = mouseX;
        this.lastMouseY = mouseY;
        this.updateDropdownAnimation();
        this.onAfterBaseDrawScreen();
    }

    @Override
    public boolean onClick(int mouseX, int mouseY, int button) {
        if (this.moduleComponent.isOpened && this.moduleComponent.isVisible(this)) {
            AbstractTextInputComponent.Layout layout = this.layout(true);
            if (button == 0 && this.handleDropdownClick(mouseX, mouseY, layout)) {
                this.onDropdownClickHandled(mouseX, mouseY);
                return true;
            } else if (button == 0 && this.handleSelectedEntryClick(mouseX, mouseY, layout)) {
                this.onSelectedEntryClickHandled(mouseX, mouseY);
                return true;
            } else if (this.handleTextFieldFocusClick(mouseX, mouseY, layout)) {
                this.onSearchFocusHandled(mouseX, mouseY);
                return true;
            } else {
                if (this.isSearchFocused()) {
                    this.unfocusSearch();
                }
                this.onOutsideClick(mouseX, mouseY, button);
                return false;
            }
        } else {
            return false;
        }
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        if (this.moduleComponent.isOpened) {
            if (keyCode == 1 && this.isSearchFocused()) {
                if (!this.handleSearchEscape()) {
                    this.unfocusSearch();
                }
            } else if ((keyCode == 28 || keyCode == 156) && this.isSearchFocused()) {
                this.unfocusSearch();
            } else {
                if (this.getTextField().textboxKeyTyped(typedChar, keyCode)) {
                    this.onSearchTextChanged(this.getTextField().getText());
                    this.dropdownScrollAnim.reset(0.0F);
                    this.updateDropdownAnimation();
                    this.moduleComponent.updateSettingPositions();
                }
            }
        }
    }

    @Override
    public void onScroll(int scroll) {
        if (this.moduleComponent.isOpened && this.moduleComponent.isVisible(this)) {
            float scrollSpeed = (float) Gui.scrollSpeed.getInput();
            float delta = scrollSpeed * (scroll / 120.0F);
            if (this.isMouseOverDropdown()) {
                if (delta != 0.0F) {
                    this.dropdownScrollAnim.extend(-delta);
                }
                this.clampDropdownScroll();
            } else {
                if (this.isMouseOverSelectedList() && this.getSelectedEntryCount() > 7) {
                    if (delta != 0.0F) {
                        this.selectedScrollAnim.extend(-delta);
                    }
                    this.clampSelectedScroll();
                }
            }
        }
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        this.getTextField().setText("");
        this.resetSearchState();
        this.dropdownScrollAnim.reset(0.0F);
        this.selectedScrollAnim.reset(0.0F);
        this.dropdownAnimTimer = null;
        this.dropdownAnimH = 0.0F;
        this.dropdownAnimStartH = 0.0F;
        this.dropdownAnimTargetH = 0.0F;
    }

    @Override
    public float getHeightF() {
        return this.getCurrentHeight();
    }

    public boolean isSearchFocused() {
        return this.isTextFieldFocused();
    }

    public void unfocusSearch() {
        this.setTextFieldFocused(false);
        this.updateDropdownAnimation();
    }

    @Override
    public boolean isTextInputFocused() {
        return this.isSearchFocused() || this.hasAdditionalTextInputFocus();
    }

    @Override
    public void unfocusTextInput() {
        this.clearAdditionalTextInputFocus();
        this.unfocusSearch();
    }

    @Override
    public boolean containsClick(int mouseX, int mouseY) {
        AbstractTextInputComponent.Layout layout = this.layout(true);
        return this.isTextFieldClicked(mouseX, mouseY, layout) || this.isMouseOverDropdown(mouseX, mouseY) || this.isMouseOverSelectedList(mouseX, mouseY);
    }

    public boolean capturesCategoryScroll(float mouseX, float mouseY) {
        if (this.moduleComponent.isOpened && this.moduleComponent.isVisible(this)) {
            return this.isMouseOverDropdown(mouseX, mouseY) && this.getDropdownRowCount() > 7 ? true : this.isMouseOverSelectedList(mouseX, mouseY) && this.getSelectedEntryCount() > 7;
        } else {
            return false;
        }
    }

    public float getCurrentHeight() {
        int selected = this.getSelectedEntryCount();
        float selectedHeight = selected == 0 ? 0.0F : 4.0F + Math.min(7, selected) * 12.0F;
        return 24.0F + this.getAnimatedDropdownHeight() + selectedHeight;
    }

    protected final float getAnimatedDropdownHeight() {
        if (this.dropdownAnimTimer != null) {
            if ((float) (System.currentTimeMillis() - this.dropdownAnimTimer.last) >= 280.0F) {
                this.dropdownAnimTimer = null;
                this.dropdownAnimH = this.dropdownAnimTargetH;
                this.dropdownAnimStartH = this.dropdownAnimTargetH;
            } else {
                this.dropdownAnimH = this.dropdownAnimTimer.getValueFloat(this.dropdownAnimStartH, this.dropdownAnimTargetH, 1);
                if (this.dropdownAnimH == this.dropdownAnimTargetH) {
                    this.dropdownAnimTimer = null;
                    this.dropdownAnimStartH = this.dropdownAnimTargetH;
                }
            }
        }
        return this.dropdownAnimH;
    }

    protected final float getSelectedTop(AbstractTextInputComponent.Layout layout) {
        return layout.contentTop + this.getAnimatedDropdownHeight() + 4.0F;
    }

    protected final float getSelectedVisibleHeight() {
        return Math.min(7, this.getSelectedEntryCount()) * 12.0F;
    }

    protected final void clampDropdownScroll() {
        float floatValue3 = Math.max(0.0F, (this.getDropdownRowCount() - 7) * 12.0F);
        this.dropdownScrollAnim.clampTarget(0.0F, floatValue3);
    }

    protected final void clampSelectedScroll() {
        float floatValue4 = Math.max(0.0F, (this.getSelectedEntryCount() - 7) * 12.0F);
        this.selectedScrollAnim.clampTarget(0.0F, floatValue4);
    }

    protected final void updateDropdownAnimation() {
        float newTarget = this.computeDropdownTarget();
        if (newTarget != this.dropdownAnimTargetH) {
            this.dropdownAnimStartH = this.dropdownAnimH;
            this.dropdownAnimTargetH = newTarget;
            this.dropdownAnimTimer = new Timer(250.0F);
            this.dropdownAnimTimer.start();
        }
    }

    protected final void notifySelectionListChanged() {
        this.markUnsaved();
        this.updateDropdownAnimation();
        this.moduleComponent.updateSettingPositions();
    }

    protected final void markUnsaved() {
        if (Raven.currentProfile != null) {
            Raven.currentProfile.getModule().saved = false;
        }
    }

    protected final ItemStack getPreviewStack(AbstractSearchListComponent.SelectedRowData row) {
        if (row == null) {
            return null;
        } else if (row.cyclingStacks != null && !row.cyclingStacks.isEmpty()) {
            int index = (int) (System.currentTimeMillis() / 1000L % row.cyclingStacks.size());
            return row.cyclingStacks.get(index);
        } else {
            return row.stack;
        }
    }

    protected final void renderBackRow(float left, float right, float rowTop, int bgColor, String groupName) {
        RenderUtils.drawRect(left, rowTop, right, rowTop + 12.0F - 1.0F, bgColor);
        ResourceLocation arrow = RenderUtils.getIcon("/assets/keystrokesmod/textures/gui/arrow_left.png");
        if (arrow != null) {
            float iconX = left + 2.0F;
            float iconY = rowTop + 2.5F;
            RenderUtils.drawIcon(arrow, iconX, iconY, 6, -1);
        }
        this.drawListRowText(groupName != null ? groupName : "Back", left + 13.0F, rowTop, -3355444);
    }

    protected final void renderStandardRow(String label, ItemStack stack, float left, float right, float rowTop, int bgColor, boolean showClose) {
        RenderUtils.drawRect(left, rowTop, right, rowTop + 12.0F - 1.0F, bgColor);
        this.renderItemInRow(stack, left + 2.0F, rowTop);
        this.drawListRowText(label != null ? label : "", left + 13.0F, rowTop, -3355444);
        if (showClose) {
            this.renderCloseIcon(right, rowTop);
        }
    }

    protected final void drawListRowText(String text, float x, float rowTop, int color) {
        drawScaledText(text, x, centeredScaledTextY(rowTop, 11.0F, 0.56F) + 0.56F, color, 0.56F);
    }

    protected final void renderCloseIcon(float right, float rowTop) {
        ResourceLocation close = RenderUtils.getIcon("/assets/keystrokesmod/textures/gui/close.png");
        if (close != null) {
            float closeX = right - 6.0F - 3.0F;
            float closeY = rowTop + 2.5F;
            int closeColor = Theme.getGradient(Theme.hiddenBind[0], Theme.hiddenBind[1], 0.0);
            RenderUtils.drawIcon(close, closeX, closeY, 6, closeColor);
        }
    }

    protected final boolean isOverClose(float mouseX, float mouseY, float rowTop, float right) {
        float closeX = right - 6.0F - 3.0F;
        float closeY = rowTop + 3.0F;
        return mouseX >= closeX && mouseX <= closeX + 6.0F && mouseY >= closeY && mouseY <= closeY + 6.0F;
    }

    protected final void renderItemInRow(ItemStack stack, float x, float rowTop) {
        if (stack != null) {
            RenderItem renderItem = Minecraft.func_71410_x().func_175599_af();
            double scale = 0.55;
            float itemHeight = (float) (16.0 * scale);
            float pad = (11.0F - itemHeight) / 2.0F;
            float itemY = rowTop + pad;
            float px = (float) (x / scale);
            float py = (float) (itemY / scale);
            GlStateManager.func_179094_E();
            GlStateManager.func_179139_a(scale, scale, scale);
            GlStateManager.func_179109_b(px, py, 0.0F);
            RenderHelper.func_74520_c();
            GlStateManager.func_179084_k();
            renderItem.func_180450_b(stack, 0, 0);
            GlStateManager.func_179147_l();
            RenderHelper.func_74518_a();
            GlStateManager.func_179121_F();
        }
    }

    protected final boolean isMouseOverDropdown(float mouseX, float mouseY) {
        if (this.getDropdownRowCount() == 0) {
            return false;
        } else {
            float dropdownHeight = this.getAnimatedDropdownHeight();
            if (dropdownHeight <= 0.0F) {
                return false;
            } else {
                AbstractTextInputComponent.Layout layout = this.layout(true);
                return mouseX >= layout.left && mouseX <= layout.right && mouseY >= layout.contentTop && mouseY < layout.contentTop + dropdownHeight;
            }
        }
    }

    protected final boolean isMouseOverSelectedList(float mouseX, float mouseY) {
        if (this.getSelectedEntryCount() == 0) {
            return false;
        } else {
            AbstractTextInputComponent.Layout layout = this.layout(true);
            float selectedTop = this.getSelectedTop(layout);
            float selectedHeight = this.getSelectedVisibleHeight();
            return mouseX >= layout.left && mouseX <= layout.right && mouseY >= selectedTop && mouseY < selectedTop + selectedHeight;
        }
    }

    protected final boolean isMouseOverDropdown() {
        return this.isMouseOverDropdown(this.lastMouseX, this.lastMouseY);
    }

    protected final boolean isMouseOverSelectedList() {
        return this.isMouseOverSelectedList(this.lastMouseX, this.lastMouseY);
    }

    private float computeDropdownTarget() {
        int rows = this.getDropdownRowCount();
        return this.isSearchFocused() && rows > 0 ? Math.min(7, rows) * 12.0F : 0.0F;
    }

    private void renderDropdown(AbstractTextInputComponent.Layout layout) {
        int rowCount = this.getDropdownRowCount();
        float dropdownHeight = this.getAnimatedDropdownHeight();
        float scrollOffset = this.moduleComponent.categoryComponent.moduleY - layout.cy;
        if (!(dropdownHeight <= 0.0F) && rowCount != 0) {
            float dropdownTopScreen = layout.contentTop + scrollOffset;
            RenderUtils.scissorPushGui(layout.left, dropdownTopScreen, layout.right - layout.left, dropdownHeight);
            float value = this.dropdownScrollAnim.getValue();
            int firstRow = (int) (value / 12.0F);
            int end = Math.min(firstRow + 7 + 1, rowCount);
            int rowUnderMouse = -1;
            if (this.lastMouseX >= layout.left && this.lastMouseX <= layout.right && this.lastMouseY >= dropdownTopScreen && this.lastMouseY < dropdownTopScreen + dropdownHeight) {
                float relY = this.lastMouseY - dropdownTopScreen;
                rowUnderMouse = (int) ((relY + value) / 12.0F);
                if (rowUnderMouse < 0 || rowUnderMouse >= rowCount) {
                    rowUnderMouse = -1;
                }
            }
            this.renderDropdownRows(layout, value, firstRow, end, rowUnderMouse);
            RenderUtils.scissorPop();
        }
    }

    private void renderSelectedEntries(AbstractTextInputComponent.Layout layout) {
        int selectedCount = this.getSelectedEntryCount();
        if (selectedCount != 0) {
            float selectedTop = this.getSelectedTop(layout);
            float selectedHeight = this.getSelectedVisibleHeight();
            float scrollOffset = this.moduleComponent.categoryComponent.moduleY - layout.cy;
            RenderUtils.scissorPushGui(layout.left, selectedTop + scrollOffset, layout.right - layout.left, selectedHeight);
            float value2 = this.selectedScrollAnim.getValue();
            int firstRow = (int) (value2 / 12.0F);
            int end = Math.min(firstRow + 7 + 1, selectedCount);
            this.renderSelectedRows(layout, value2, firstRow, end);
            RenderUtils.scissorPop();
        }
    }

    private boolean handleTextFieldFocusClick(int mouseX, int mouseY, AbstractTextInputComponent.Layout layout) {
        if (this.isTextFieldClicked(mouseX, mouseY, layout)) {
            this.setTextFieldFocused(true);
            this.onSearchFieldFocused();
            this.updateDropdownAnimation();
            return true;
        } else {
            return false;
        }
    }

    protected boolean hasAdditionalTextInputFocus() {
        return false;
    }

    protected void clearAdditionalTextInputFocus() {
    }

    protected void onAfterBaseDrawScreen() {
    }

    protected void onDropdownClickHandled(int mouseX, int mouseY) {
    }

    protected void onSelectedEntryClickHandled(int mouseX, int mouseY) {
    }

    protected void onSearchFocusHandled(int mouseX, int mouseY) {
    }

    protected void onOutsideClick(int mouseX, int mouseY, int button) {
    }

    protected abstract String getLabelText();

    protected abstract int getSelectedEntryCount();

    protected abstract int getDropdownRowCount();

    protected abstract void renderDropdownRows(AbstractTextInputComponent.Layout layout2, float floatValue, int intValue, int intValue2, int intValue3);

    protected abstract void renderSelectedRows(AbstractTextInputComponent.Layout layout3, float floatValue2, int intValue4, int intValue5);

    protected abstract boolean handleDropdownClick(int intValue6, int intValue7, AbstractTextInputComponent.Layout layout4);

    protected abstract boolean handleSelectedEntryClick(int intValue8, int intValue9, AbstractTextInputComponent.Layout layout5);

    protected abstract void onSearchTextChanged(String text2);

    protected abstract boolean handleSearchEscape();

    protected abstract void onSearchFieldFocused();

    protected abstract void resetSearchState();

    protected static class SelectedRowData {

        final String storageId;

        final String displayName;

        final ItemStack stack;

        final List<ItemStack> cyclingStacks;

        protected SelectedRowData(String storageId, String displayName, ItemStack stack, List<ItemStack> cyclingStacks) {
            this.storageId = storageId;
            this.displayName = displayName;
            this.stack = stack;
            this.cyclingStacks = cyclingStacks;
        }
    }
}
