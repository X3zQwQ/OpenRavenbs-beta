package keystrokesmod.clickgui.components.impl;

import java.util.ArrayList;
import java.util.List;
import keystrokesmod.module.setting.impl.ItemListSetting;
import keystrokesmod.utility.ItemSearchIndex;
import net.minecraft.item.ItemStack;

public class ItemSearchComponent extends AbstractItemSearchComponent<ItemListSetting> {

    private List<AbstractSearchListComponent.SelectedRowData> selectedRowsCache;

    public ItemSearchComponent(ItemListSetting setting, ModuleComponent moduleComponent, float o) {
        super(setting, moduleComponent, o);
    }

    @Override
    protected int getSelectedEntryCount() {
        return this.setting.getItems().size();
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
                this.selectedRowsCache.add(new AbstractSearchListComponent.SelectedRowData(storageId, ItemSearchIndex.getDisplayName(storageId), ItemSearchIndex.getItemStack(storageId), cyclingStacks));
            }
        }
        for (int i = firstRow; i < end; i++) {
            AbstractSearchListComponent.SelectedRowData row = this.selectedRowsCache.get(i);
            float rowTop = this.getSelectedTop(layout) - floatValue + i * 12.0F;
            int bg = i % 2 == 0 ? -15066582 : -14803410;
            this.renderStandardRow(row.displayName, this.getPreviewStack(row), layout.left, layout.right, rowTop, bg, true);
        }
    }

    @Override
    protected boolean handleSelectedEntryClick(int mouseX, int mouseY, AbstractTextInputComponent.Layout layout) {
        float value = this.selectedScrollAnim.getValue();
        List<String> items = new ArrayList<>(this.setting.getItems());
        for (int i = 0; i < items.size(); i++) {
            float rowTop = this.getSelectedTop(layout) - value + i * 12.0F;
            if (this.isOverClose(mouseX, mouseY, rowTop, layout.right)) {
                this.removeSelection(items.get(i));
                return true;
            }
        }
        return false;
    }

    @Override
    protected void invalidateSelectedRows() {
        this.selectedRowsCache = null;
    }
}
