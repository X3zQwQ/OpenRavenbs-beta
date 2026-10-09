package keystrokesmod.clickgui.components.impl;

import java.util.ArrayList;
import java.util.List;
import keystrokesmod.module.impl.client.Gui;
import keystrokesmod.module.setting.impl.InventoryItemListSetting;
import keystrokesmod.utility.ItemSearchIndex;
import keystrokesmod.utility.RenderUtils;
import keystrokesmod.utility.font.RavenFontRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;

public class InventoryItemSearchComponent extends AbstractItemSearchComponent<InventoryItemListSetting> {

    private static final float SLOT_PILL_MIN_WIDTH = 9.0F;

    private static final float SLOT_PILL_HORIZONTAL_PAD = 3.0F;

    private static final float SLOT_PILL_TEXT_Y_OFFSET = 0.5F;

    private static final float SLOT_BOX_GAP = 3.0F;

    private static final float DRAG_SCROLL_EDGE = 10.0F;

    private static final float DRAG_SCROLL_SPEED = 3.0F;

    private List<InventoryItemSearchComponent.InventorySelectedRowData> selectedRowsCache;

    private String listeningStorageId;

    private String draggingStorageId;

    private float dragGrabOffsetY;

    public InventoryItemSearchComponent(InventoryItemListSetting setting, ModuleComponent moduleComponent, float o) {
        super(setting, moduleComponent, o);
    }

    @Override
    protected int getSelectedEntryCount() {
        return this.setting.getItems().size();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY) {
        super.drawScreen(mouseX, mouseY);
        this.updateDragState();
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int button) {
        if (button == 0) {
            this.draggingStorageId = null;
        }
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        if (this.moduleComponent.isOpened) {
            if (this.listeningStorageId != null) {
                if (keyCode == 1) {
                    this.listeningStorageId = null;
                } else {
                    int slot = this.getHotbarSlotForKey(keyCode);
                    if (slot != -1) {
                        this.setting.setAssignedSlot(this.listeningStorageId, slot);
                        this.listeningStorageId = null;
                        this.invalidateSelectedRows();
                        this.markUnsaved();
                    }
                }
            } else {
                super.keyTyped(typedChar, keyCode);
            }
        }
    }

    @Override
    protected boolean hasAdditionalTextInputFocus() {
        return this.listeningStorageId != null;
    }

    @Override
    protected void clearAdditionalTextInputFocus() {
        this.listeningStorageId = null;
    }

    @Override
    protected void onDropdownClickHandled(int mouseX, int mouseY) {
        this.listeningStorageId = null;
    }

    @Override
    protected void onSearchFocusHandled(int mouseX, int mouseY) {
        this.listeningStorageId = null;
    }

    @Override
    protected void onOutsideClick(int mouseX, int mouseY, int button) {
        this.listeningStorageId = null;
    }

    @Override
    protected void renderSelectedRows(AbstractTextInputComponent.Layout layout, float floatValue, int firstRow, int end) {
        List<String> items = this.setting.getItems();
        if (this.selectedRowsCache == null || this.selectedRowsCache.size() != items.size()) {
            this.selectedRowsCache = new ArrayList<>();
            for (String storageId : items) {
                List<ItemSearchIndex.ItemEntry> variants = ItemSearchIndex.isGroupedSelection(storageId) ? ItemSearchIndex.getSelectionVariants(storageId) : null;
                List<ItemStack> cyclingStacks = null;
                if (variants != null && !variants.isEmpty()) {
                    cyclingStacks = new ArrayList<>();
                    for (ItemSearchIndex.ItemEntry variant : variants) {
                        cyclingStacks.add(variant.toItemStack());
                    }
                }
                this.selectedRowsCache.add(new InventoryItemSearchComponent.InventorySelectedRowData(storageId, ItemSearchIndex.getDisplayName(storageId), ItemSearchIndex.getItemStack(storageId), cyclingStacks, this.setting.getAssignedSlot(storageId)));
            }
        }
        for (int i = firstRow; i < end; i++) {
            InventoryItemSearchComponent.InventorySelectedRowData row = this.selectedRowsCache.get(i);
            float rowTop = this.getSelectedTop(layout) - floatValue + i * 12.0F;
            int bg = row.storageId.equals(this.draggingStorageId) ? -14013892 : (i % 2 == 0 ? -15066582 : -14803410);
            this.renderSelectedRow(row, layout.left, layout.right, rowTop, bg);
        }
    }

    @Override
    protected boolean handleSelectedEntryClick(int mouseX, int mouseY, AbstractTextInputComponent.Layout layout) {
        int selectedRowIndex = this.getSelectedRowIndex(mouseX, mouseY, layout);
        if (selectedRowIndex < 0) {
            this.draggingStorageId = null;
            return false;
        } else {
            String storageId = this.setting.getItems().get(selectedRowIndex);
            float rowTop = this.getSelectedTop(layout) - this.selectedScrollAnim.getValue() + selectedRowIndex * 12.0F;
            if (this.isOverClose(mouseX, mouseY, rowTop, layout.right)) {
                this.setting.removeItem(storageId);
                this.invalidateSelectedRows();
                this.listeningStorageId = null;
                this.draggingStorageId = null;
                this.markUnsaved();
                this.clampSelectedScroll();
                this.updateDropdownAnimation();
                this.moduleComponent.updateSettingPositions();
                return true;
            } else if (this.isOverSlotPill(mouseX, mouseY, rowTop, layout.right, storageId)) {
                this.listeningStorageId = storageId;
                this.draggingStorageId = null;
                return true;
            } else {
                this.draggingStorageId = storageId;
                this.dragGrabOffsetY = mouseY - rowTop;
                if (!this.isPointInSlotPill(mouseX, mouseY, layout)) {
                    this.listeningStorageId = null;
                }
                return true;
            }
        }
    }

    @Override
    protected void invalidateSelectedRows() {
        this.selectedRowsCache = null;
    }

    @Override
    protected void onSearchStateReset() {
        this.listeningStorageId = null;
        this.draggingStorageId = null;
    }

    private void renderSelectedRow(InventoryItemSearchComponent.InventorySelectedRowData row, float left, float right, float rowTop, int bgColor) {
        RenderUtils.drawRect(left, rowTop, right, rowTop + 12.0F - 1.0F, bgColor);
        this.renderItemInRow(this.getPreviewStack(row), left + 2.0F, rowTop);
        this.drawListRowText(row.displayName != null ? row.displayName : "", left + 13.0F, rowTop, -3355444);
        float closeX = right - 6.0F - 3.0F;
        float slotRight = closeX - 3.0F;
        float slotLeft = slotRight - this.getSlotPillWidth(row.storageId);
        this.renderSlotPill(row, slotLeft, slotRight, rowTop);
        this.renderCloseIcon(right, rowTop);
    }

    private void renderSlotPill(InventoryItemSearchComponent.InventorySelectedRowData row, float left, float right, float rowTop) {
        boolean listening = row.storageId.equals(this.listeningStorageId);
        int fill = listening ? -13281926 : -14399130;
        float top = rowTop + 2.0F;
        float bottom = rowTop + 12.0F - 3.0F;
        RenderUtils.drawRect(left, top, right, bottom, -15657956);
        RenderUtils.drawRect(left + 1.0F, top + 1.0F, right - 1.0F, bottom - 1.0F, fill);
        String label = this.getSlotPillLabel(row.storageId);
        RavenFontRenderer renderer = Gui.getClickGuiSettingFontRenderer();
        float textWidth = renderer.getStringWidth(label) * 0.5F;
        float textX = left + (right - left - textWidth) / 2.0F;
        float textY = centeredScaledTextY(top, bottom - top) + 0.5F;
        drawScaledText(label, textX, textY, -1511691);
    }

    private void updateDragState() {
        if (this.draggingStorageId != null && !this.setting.getItems().isEmpty()) {
            AbstractTextInputComponent.Layout layout = this.layout(true);
            float selectedTop = this.getSelectedTop(layout);
            float selectedHeight = this.getSelectedVisibleHeight();
            if (selectedHeight <= 0.0F) {
                this.draggingStorageId = null;
            } else {
                if (this.setting.getItems().size() > 7 && this.lastMouseX >= layout.left && this.lastMouseX <= layout.right) {
                    if (this.lastMouseY < selectedTop + 10.0F) {
                        this.selectedScrollAnim.extend(3.0F);
                        this.clampSelectedScroll();
                    } else if (this.lastMouseY > selectedTop + selectedHeight - 10.0F) {
                        this.selectedScrollAnim.extend(-3.0F);
                        this.clampSelectedScroll();
                    }
                }
                List<String> orderedItems = this.setting.getItems();
                int intValue = orderedItems.indexOf(this.draggingStorageId);
                if (intValue < 0) {
                    this.draggingStorageId = null;
                } else {
                    float draggedRowTop = this.lastMouseY - this.dragGrabOffsetY;
                    float draggedRowCenter = draggedRowTop + 6.0F;
                    int intValue2 = (int) Math.floor((draggedRowCenter - selectedTop + this.selectedScrollAnim.getValue()) / 12.0F);
                    intValue2 = Math.max(0, Math.min(intValue2, orderedItems.size() - 1));
                    if (intValue2 != intValue) {
                        this.setting.moveItem(this.draggingStorageId, intValue2);
                        this.invalidateSelectedRows();
                        this.markUnsaved();
                    }
                }
            }
        }
    }

    private int getSelectedRowIndex(int mouseX, int mouseY, AbstractTextInputComponent.Layout layout) {
        if (!this.isMouseOverSelectedList(mouseX, mouseY)) {
            return -1;
        } else {
            float selectedTop = this.getSelectedTop(layout);
            float value = this.selectedScrollAnim.getValue();
            int intValue3 = (int) ((mouseY - selectedTop + value) / 12.0F);
            if (intValue3 >= 0 && intValue3 < this.setting.getItems().size()) {
                float rowTop = selectedTop - value + intValue3 * 12.0F;
                return mouseY >= rowTop && mouseY < rowTop + 12.0F ? intValue3 : -1;
            } else {
                return -1;
            }
        }
    }

    private boolean isPointInSlotPill(int mouseX, int mouseY, AbstractTextInputComponent.Layout layout) {
        int selectedRowIndex2 = this.getSelectedRowIndex(mouseX, mouseY, layout);
        if (selectedRowIndex2 < 0) {
            return false;
        } else {
            String storageId = this.setting.getItems().get(selectedRowIndex2);
            float rowTop = this.getSelectedTop(layout) - this.selectedScrollAnim.getValue() + selectedRowIndex2 * 12.0F;
            return this.isOverSlotPill(mouseX, mouseY, rowTop, layout.right, storageId);
        }
    }

    private boolean isOverSlotPill(int mouseX, int mouseY, float rowTop, float right, String storageId) {
        float slotRight = right - 6.0F - 3.0F - 3.0F;
        float slotLeft = slotRight - this.getSlotPillWidth(storageId);
        return mouseX >= slotLeft && mouseX <= slotRight && mouseY >= rowTop + 1.0F && mouseY <= rowTop + 12.0F - 1.0F;
    }

    private float getSlotPillWidth(String storageId) {
        String label = this.getSlotPillLabel(storageId);
        RavenFontRenderer renderer = Gui.getClickGuiSettingFontRenderer();
        float textWidth = renderer.getStringWidth(label) * 0.5F;
        return Math.max(9.0F, textWidth + 6.0F);
    }

    private String getSlotPillLabel(String storageId) {
        if (storageId != null && storageId.equals(this.listeningStorageId)) {
            return "...";
        } else {
            Integer assignedSlot = this.setting.getAssignedSlot(storageId);
            return Integer.toString(assignedSlot != null ? assignedSlot : 1);
        }
    }

    private int getHotbarSlotForKey(int keyCode) {
        Minecraft minecraft = Minecraft.func_71410_x();
        for (int i = 0; i < minecraft.field_71474_y.field_151456_ac.length; i++) {
            if (keyCode == minecraft.field_71474_y.field_151456_ac[i].func_151463_i()) {
                return i + 1;
            }
        }
        return -1;
    }

    private static final class InventorySelectedRowData extends AbstractSearchListComponent.SelectedRowData {

        final Integer assignedSlot;

        private InventorySelectedRowData(String storageId, String displayName, ItemStack stack, List<ItemStack> cyclingStacks, Integer assignedSlot) {
            super(storageId, displayName, stack, cyclingStacks);
            this.assignedSlot = assignedSlot;
        }
    }
}
