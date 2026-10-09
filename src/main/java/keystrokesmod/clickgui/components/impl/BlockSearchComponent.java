package keystrokesmod.clickgui.components.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import keystrokesmod.module.setting.impl.BlockListSetting;
import keystrokesmod.utility.BlockSearchIndex;
import net.minecraft.item.ItemStack;

public class BlockSearchComponent extends AbstractSearchListComponent {

    public final BlockListSetting setting;

    private List<BlockSearchIndex.GroupedBlockResult> cachedResults = Collections.emptyList();

    private String expandedGroupId;

    private List<BlockSearchIndex.BlockEntry> expandedVariants = Collections.emptyList();

    private List<AbstractSearchListComponent.SelectedRowData> selectedRowsCache;

    public BlockSearchComponent(BlockListSetting setting, ModuleComponent moduleComponent, float o) {
        super(moduleComponent, o, "Search blocks...");
        this.setting = setting;
    }

    @Override
    protected String getLabelText() {
        int count = this.setting.getBlocks().size();
        return this.setting.getName() + (count > 0 ? " (" + count + ")" : "");
    }

    @Override
    protected int getSelectedEntryCount() {
        return this.setting.getBlocks().size();
    }

    @Override
    protected int getDropdownRowCount() {
        return this.expandedGroupId != null ? 2 + this.expandedVariants.size() : this.cachedResults.size();
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
    protected void renderDropdownRows(AbstractTextInputComponent.Layout layout, float floatValue, int firstRow, int end, int rowUnderMouse) {
        if (this.expandedGroupId != null) {
            String groupName = !this.expandedVariants.isEmpty() ? this.expandedVariants.get(0).displayName : this.expandedGroupId;
            for (int i = firstRow; i < end; i++) {
                float rowTop = layout.contentTop - floatValue + i * 12.0F;
                int bg = i == rowUnderMouse ? -14013892 : (i % 2 == 0 ? -15066582 : -14803410);
                if (i == 0) {
                    this.renderBackRow(layout.left, layout.right, rowTop, bg, groupName);
                } else if (i == 1) {
                    this.renderStandardRow(groupName + " (All)", this.getExpandedAllCyclingIcon(), layout.left, layout.right, rowTop, bg, false);
                } else {
                    BlockSearchIndex.BlockEntry entry = this.expandedVariants.get(i - 2);
                    this.renderStandardRow(entry.displayName, entry.toItemStack(), layout.left, layout.right, rowTop, bg, false);
                }
            }
        } else {
            for (int ix = firstRow; ix < end; ix++) {
                BlockSearchIndex.GroupedBlockResult result = this.cachedResults.get(ix);
                float rowTop = layout.contentTop - floatValue + ix * 12.0F;
                int bg = ix == rowUnderMouse ? -14013892 : (ix % 2 == 0 ? -15066582 : -14803410);
                if (result.isSingleVariant()) {
                    BlockSearchIndex.BlockEntry single = result.variants.get(0);
                    this.renderStandardRow(single.displayName, single.toItemStack(), layout.left, layout.right, rowTop, bg, false);
                } else {
                    this.renderStandardRow(result.getGroupLabel(), result.getCyclingIcon(), layout.left, layout.right, rowTop, bg, false);
                }
            }
        }
    }

    @Override
    protected void renderSelectedRows(AbstractTextInputComponent.Layout layout, float floatValue2, int firstRow, int end) {
        List<String> blocks = this.setting.getBlocks();
        if (this.selectedRowsCache == null || this.selectedRowsCache.size() != blocks.size()) {
            this.selectedRowsCache = new ArrayList<>();
            for (String storageId : blocks) {
                List<BlockSearchIndex.BlockEntry> variants = null;
                if (BlockSearchIndex.isWildcard(storageId)) {
                    variants = BlockSearchIndex.getVariants(BlockSearchIndex.getRegistryId(storageId));
                }
                List<ItemStack> cyclingStacks = null;
                if (variants != null && !variants.isEmpty()) {
                    cyclingStacks = new ArrayList<>();
                    for (BlockSearchIndex.BlockEntry variant : variants) {
                        cyclingStacks.add(variant.toItemStack());
                    }
                }
                this.selectedRowsCache.add(new AbstractSearchListComponent.SelectedRowData(storageId, BlockSearchIndex.getDisplayName(storageId), BlockSearchIndex.getItemStack(storageId), cyclingStacks));
            }
        }
        for (int i = firstRow; i < end; i++) {
            AbstractSearchListComponent.SelectedRowData row = this.selectedRowsCache.get(i);
            float rowTop = this.getSelectedTop(layout) - floatValue2 + i * 12.0F;
            int bg = i % 2 == 0 ? -15066582 : -14803410;
            this.renderStandardRow(row.displayName, this.getPreviewStack(row), layout.left, layout.right, rowTop, bg, true);
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
                int intValue = (int) ((mouseY - layout.contentTop + value) / 12.0F);
                if (intValue >= 0 && intValue < rowCount && !(mouseX <= layout.left) && !(mouseX >= layout.right)) {
                    float rowTop = layout.contentTop - value + intValue * 12.0F;
                    if (!(mouseY < rowTop) && !(mouseY >= rowTop + 12.0F)) {
                        if (this.expandedGroupId != null) {
                            if (intValue == 0) {
                                this.collapseExpandedGroup();
                                return true;
                            } else if (intValue == 1) {
                                this.setting.addBlock(this.expandedGroupId + ":*");
                                this.afterBlockSelectionAdded();
                                return true;
                            } else {
                                int intValue2 = intValue - 2;
                                if (intValue2 >= 0 && intValue2 < this.expandedVariants.size()) {
                                    this.setting.addBlock(this.expandedVariants.get(intValue2).storageId);
                                    this.afterBlockSelectionAdded();
                                    return true;
                                } else {
                                    return true;
                                }
                            }
                        } else {
                            BlockSearchIndex.GroupedBlockResult result = this.cachedResults.get(intValue);
                            if (result.isSingleVariant()) {
                                this.setting.addBlock(result.variants.get(0).storageId);
                                this.afterBlockSelectionAdded();
                                return true;
                            } else {
                                this.expandedGroupId = result.registryId;
                                this.expandedVariants = new ArrayList<>();
                                for (BlockSearchIndex.BlockEntry entry : result.variants) {
                                    if (!this.setting.contains(entry.storageId)) {
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
    protected boolean handleSelectedEntryClick(int mouseX, int mouseY, AbstractTextInputComponent.Layout layout) {
        float value2 = this.selectedScrollAnim.getValue();
        List<String> blocks = new ArrayList<>(this.setting.getBlocks());
        for (int i = 0; i < blocks.size(); i++) {
            float rowTop = this.getSelectedTop(layout) - value2 + i * 12.0F;
            if (this.isOverClose(mouseX, mouseY, rowTop, layout.right)) {
                this.setting.removeBlock(blocks.get(i));
                this.selectedRowsCache = null;
                this.notifySelectionListChanged();
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onSearchTextChanged(String text) {
        this.expandedGroupId = null;
        this.expandedVariants = Collections.emptyList();
        this.cachedResults = BlockSearchIndex.searchGrouped(text, this.setting);
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
            this.cachedResults = BlockSearchIndex.searchGrouped(this.getTextField().getText(), this.setting);
        }
    }

    @Override
    protected void resetSearchState() {
        this.cachedResults = Collections.emptyList();
        this.expandedGroupId = null;
        this.expandedVariants = Collections.emptyList();
        this.selectedRowsCache = null;
    }

    private void afterBlockSelectionAdded() {
        this.getTextField().setText("");
        this.setTextFieldFocused(false);
        this.cachedResults = Collections.emptyList();
        this.expandedGroupId = null;
        this.expandedVariants = Collections.emptyList();
        this.dropdownScrollAnim.reset(0.0F);
        this.selectedRowsCache = null;
        this.notifySelectionListChanged();
    }

    private void collapseExpandedGroup() {
        this.expandedGroupId = null;
        this.expandedVariants = Collections.emptyList();
        this.dropdownScrollAnim.reset(0.0F);
        this.updateDropdownAnimation();
        this.moduleComponent.updateSettingPositions();
    }

    private ItemStack getExpandedAllCyclingIcon() {
        if (this.expandedVariants.isEmpty()) {
            return null;
        } else {
            int index = (int) (System.currentTimeMillis() / 1000L % this.expandedVariants.size());
            return this.expandedVariants.get(index).toItemStack();
        }
    }
}
