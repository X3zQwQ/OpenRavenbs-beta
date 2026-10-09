package keystrokesmod.clickgui.components.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import keystrokesmod.module.setting.impl.PotionListSetting;
import keystrokesmod.utility.PotionSearchIndex;

public class PotionSearchComponent extends AbstractSearchListComponent {

    private final PotionListSetting setting;

    private List<PotionSearchIndex.PotionEntry> cachedResults = Collections.emptyList();

    public PotionSearchComponent(PotionListSetting setting, ModuleComponent moduleComponent, float o) {
        super(moduleComponent, o, "Search potion effects...");
        this.setting = setting;
    }

    @Override
    protected String getLabelText() {
        int count = this.setting.getPotions().size();
        return this.setting.getName() + (count > 0 ? " (" + count + ")" : "");
    }

    @Override
    protected int getSelectedEntryCount() {
        return this.setting.getPotions().size();
    }

    @Override
    protected int getDropdownRowCount() {
        return this.cachedResults.size();
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
        for (int i = firstRow; i < end; i++) {
            PotionSearchIndex.PotionEntry entry = this.cachedResults.get(i);
            float rowTop = layout.contentTop - floatValue + i * 12.0F;
            int bg = i == rowUnderMouse ? -14013892 : (i % 2 == 0 ? -15066582 : -14803410);
            this.renderStandardRow(entry.displayName, PotionSearchIndex.getItemStack(entry.key), layout.left, layout.right, rowTop, bg, false);
        }
    }

    @Override
    protected void renderSelectedRows(AbstractTextInputComponent.Layout layout, float floatValue2, int firstRow, int end) {
        List<String> selectedPotions = this.setting.getPotions();
        for (int i = firstRow; i < end; i++) {
            String potionKey = selectedPotions.get(i);
            float rowTop = this.getSelectedTop(layout) - floatValue2 + i * 12.0F;
            int bg = i % 2 == 0 ? -15066582 : -14803410;
            this.renderStandardRow(PotionSearchIndex.getDisplayName(potionKey), PotionSearchIndex.getItemStack(potionKey), layout.left, layout.right, rowTop, bg, true);
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
                        this.setting.addPotion(this.cachedResults.get(intValue).key);
                        this.afterPotionSelectionAdded();
                        return true;
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
        List<String> selectedPotions = new ArrayList<>(this.setting.getPotions());
        for (int i = 0; i < selectedPotions.size(); i++) {
            float rowTop = this.getSelectedTop(layout) - value2 + i * 12.0F;
            if (this.isOverClose(mouseX, mouseY, rowTop, layout.right)) {
                this.setting.removePotion(selectedPotions.get(i));
                this.notifySelectionListChanged();
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onSearchTextChanged(String text) {
        this.cachedResults = PotionSearchIndex.search(text, this.setting);
    }

    @Override
    protected boolean handleSearchEscape() {
        return false;
    }

    @Override
    protected void onSearchFieldFocused() {
        this.cachedResults = PotionSearchIndex.search(this.getTextField().getText(), this.setting);
    }

    @Override
    protected void resetSearchState() {
        this.cachedResults = Collections.emptyList();
    }

    private void afterPotionSelectionAdded() {
        this.getTextField().setText("");
        this.setTextFieldFocused(false);
        this.cachedResults = Collections.emptyList();
        this.dropdownScrollAnim.reset(0.0F);
        this.notifySelectionListChanged();
    }
}
