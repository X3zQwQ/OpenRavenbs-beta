package keystrokesmod.module.setting.impl;

import java.util.List;
import keystrokesmod.utility.ItemSearchIndex;
import net.minecraft.item.ItemStack;

public class ItemListSetting extends BlockListSetting {

    public ItemListSetting(String name) {
        super(name);
    }

    public ItemListSetting(String name, String... legacyProfileKeys) {
        super(name, legacyProfileKeys);
    }

    public ItemListSetting(GroupSetting group, String name) {
        super(group, name);
    }

    public ItemListSetting(GroupSetting group, String name, String... legacyProfileKeys) {
        super(group, name, legacyProfileKeys);
    }

    public void addItem(String storageId) {
        this.addBlock(storageId);
    }

    public void removeItem(String storageId) {
        this.removeBlock(storageId);
    }

    public List<String> getItems() {
        return this.getBlocks();
    }

    public boolean containsItem(String storageId) {
        return this.contains(storageId);
    }

    public boolean matches(ItemStack stack) {
        return ItemSearchIndex.matches(this.getItems(), stack);
    }
}
