package keystrokesmod.clickgui.components.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import keystrokesmod.module.setting.impl.ItemListSetting;
import keystrokesmod.utility.ItemSearchIndex;
import net.minecraft.item.ItemStack;

public abstract class AbstractItemSearchComponent<T extends ItemListSetting> extends AbstractSearchListComponent {

    protected final T setting;

    private List<ItemSearchIndex.GroupedItemResult> cachedResults = Collections.emptyList();

    private String expandedGroupId;

    private String expandedGroupLabel;

    private String expandedAllSelectionStorageId;

    private List<ItemSearchIndex.ItemEntry> expandedVariants = Collections.emptyList();

    protected AbstractItemSearchComponent(T setting, ModuleComponent moduleComponent, float o) {
        super(moduleComponent, o, "Search items...");
        this.setting = setting;
    }

    @Override
    protected String getLabelText() {
        int count = this.setting.getItems().size();
        return this.setting.getName() + (count > 0 ? " (" + count + ")" : "");
    }

    @Override
    public boolean isBaseVisible() {
        return this.setting.visible;
    }

    @Override
    public String getGroupName() {
        return this.setting.group != null ? this.setting.group.getName() : "";
    }

    @Override
    protected int getDropdownRowCount() {
        return this.expandedGroupId != null ? 2 + this.expandedVariants.size() : this.cachedResults.size();
    }

    @Override
    protected void renderDropdownRows(AbstractTextInputComponent.Layout layout, float floatValue, int firstRow, int end, int rowUnderMouse) {
        if (this.expandedGroupId != null) {
            String groupName = this.expandedGroupLabel != null ? this.expandedGroupLabel : this.expandedGroupId;
            for (int i = firstRow; i < end; i++) {
                float rowTop = layout.contentTop - floatValue + i * 12.0F;
                int bg = i == rowUnderMouse ? -14013892 : (i % 2 == 0 ? -15066582 : -14803410);
                if (i == 0) {
                    this.renderBackRow(layout.left, layout.right, rowTop, bg, groupName);
                } else if (i == 1) {
                    this.renderStandardRow(groupName + " (All)", this.getExpandedAllCyclingIcon(), layout.left, layout.right, rowTop, bg, false);
                } else {
                    ItemSearchIndex.ItemEntry entry = this.expandedVariants.get(i - 2);
                    this.renderStandardRow(entry.displayName, entry.toItemStack(), layout.left, layout.right, rowTop, bg, false);
                }
            }
        } else {
            for (int ix = firstRow; ix < end; ix++) {
                ItemSearchIndex.GroupedItemResult result = this.cachedResults.get(ix);
                float rowTop = layout.contentTop - floatValue + ix * 12.0F;
                int bg = ix == rowUnderMouse ? -14013892 : (ix % 2 == 0 ? -15066582 : -14803410);
                if (result.isSingleVariant()) {
                    ItemSearchIndex.ItemEntry single = result.variants.get(0);
                    this.renderStandardRow(single.displayName, single.toItemStack(), layout.left, layout.right, rowTop, bg, false);
                } else {
                    this.renderStandardRow(result.getGroupLabel(), result.getCyclingIcon(), layout.left, layout.right, rowTop, bg, false);
                }
            }
        }
    }

    @Override
    protected boolean handleDropdownClick(int mouseX, int mouseY, AbstractTextInputComponent.Layout layout) {
        if (!this.isSearchFocused()) {
            return false;
        } else {
            int rowCount = this.getDropdownRowCount();
            if (rowCount == 0) {
                return false;
            } else {
                float value = this.dropdownScrollAnim.getValue();
                float relY = mouseY - layout.contentTop;
                int intValue = (int) ((relY + value) / 12.0F);
                if (intValue >= 0 && intValue < rowCount && !(mouseX <= layout.left) && !(mouseX >= layout.right)) {
                    float rowTop = layout.contentTop - value + intValue * 12.0F;
                    if (!(mouseY < rowTop) && !(mouseY >= rowTop + 12.0F)) {
                        if (this.expandedGroupId != null) {
                            if (intValue == 0) {
                                this.collapseExpandedGroup();
                                return true;
                            } else if (intValue == 1) {
                                this.setting.addItem(this.expandedAllSelectionStorageId != null ? this.expandedAllSelectionStorageId : this.expandedGroupId + ":*");
                                this.afterSelectionAdded();
                                return true;
                            } else {
                                int intValue2 = intValue - 2;
                                if (intValue2 >= 0 && intValue2 < this.expandedVariants.size()) {
                                    this.setting.addItem(this.expandedVariants.get(intValue2).storageId);
                                    this.afterSelectionAdded();
                                    return true;
                                } else {
                                    return true;
                                }
                            }
                        } else {
                            ItemSearchIndex.GroupedItemResult result = this.cachedResults.get(intValue);
                            if (result.isSingleVariant()) {
                                this.setting.addItem(result.variants.get(0).storageId);
                                this.afterSelectionAdded();
                                return true;
                            } else {
                                this.expandedGroupId = result.registryId;
                                this.expandedGroupLabel = result.getGroupDisplayName();
                                this.expandedAllSelectionStorageId = result.getAllSelectionStorageId();
                                this.expandedVariants = new ArrayList<>();
                                for (ItemSearchIndex.ItemEntry entry : result.variants) {
                                    if (!this.setting.containsItem(entry.storageId)) {
                                        this.expandedVariants.add(entry);
                                    }
                                }
                                this.dropdownScrollAnim.reset(0.0F);
                                this.updateDropdownAnimation();
                                this.moduleComponent.updateSettingPositions();
                                return true;
                            }
                        }
                    } else {
                        return false;
                    }
                } else {
                    return false;
                }
            }
        }
    }

    @Override
    protected void onSearchTextChanged(String text) {
        this.collapseExpandedGroupStateOnly();
        this.cachedResults = ItemSearchIndex.searchGrouped(text, this.setting);
    }

    @Override
    protected boolean handleSearchEscape() {
        if (this.expandedGroupId != null) {
            this.collapseExpandedGroup();
            return true;
        } else {
            return false;
        }
    }

    @Override
    protected void onSearchFieldFocused() {
        if (!this.getTextField().getText().isEmpty() && this.cachedResults.isEmpty()) {
            this.cachedResults = ItemSearchIndex.searchGrouped(this.getTextField().getText(), this.setting);
        }
    }

    @Override
    protected void resetSearchState() {
        this.cachedResults = Collections.emptyList();
        this.collapseExpandedGroupStateOnly();
        this.onSearchStateReset();
    }

    protected final void removeSelection(String storageId) {
        this.setting.removeItem(storageId);
        this.invalidateSelectedRows();
        this.notifySelectionListChanged();
    }

    protected final void refreshSelectedRows() {
        this.invalidateSelectedRows();
    }

    protected final T getItemSetting() {
        return this.setting;
    }

    protected final ItemSearchIndex.GroupedItemResult getCachedResult(int index) {
        return this.cachedResults.get(index);
    }

    protected final List<ItemSearchIndex.ItemEntry> getExpandedVariants() {
        return this.expandedVariants;
    }

    private void collapseExpandedGroup() {
        this.collapseExpandedGroupStateOnly();
        this.dropdownScrollAnim.reset(0.0F);
        this.updateDropdownAnimation();
        this.moduleComponent.updateSettingPositions();
    }

    private void collapseExpandedGroupStateOnly() {
        this.expandedGroupId = null;
        this.expandedGroupLabel = null;
        this.expandedAllSelectionStorageId = null;
        this.expandedVariants = Collections.emptyList();
    }

    private void afterSelectionAdded() {
        this.getTextField().setText("");
        this.setTextFieldFocused(false);
        this.cachedResults = Collections.emptyList();
        this.collapseExpandedGroupStateOnly();
        this.dropdownScrollAnim.reset(0.0F);
        this.invalidateSelectedRows();
        this.notifySelectionListChanged();
    }

    private ItemStack getExpandedAllCyclingIcon() {
        if (this.expandedVariants.isEmpty()) {
            return null;
        } else {
            int index = (int) (System.currentTimeMillis() / 1000L % this.expandedVariants.size());
            return this.expandedVariants.get(index).toItemStack();
        }
    }

    protected void onSearchStateReset() {
    }

    protected abstract void invalidateSelectedRows();
}
