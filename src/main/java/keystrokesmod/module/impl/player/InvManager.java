package keystrokesmod.module.impl.player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import keystrokesmod.event.PostProfileLoadEvent;
import keystrokesmod.event.PreSlotScrollEvent;
import keystrokesmod.event.PreUpdateEvent;
import keystrokesmod.event.SendPacketEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.InventoryItemListSetting;
import keystrokesmod.module.setting.impl.KeySetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.ItemSearchIndex;
import keystrokesmod.utility.Utils;
import net.minecraft.block.BlockRailBase;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.ContainerPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemAppleGold;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemEgg;
import net.minecraft.item.ItemEnderPearl;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemSnowball;
import net.minecraft.item.ItemSpade;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.network.play.client.C0EPacketClickWindow;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class InvManager extends Module {

    private static final int HOTBAR_SIZE = InventoryPlayer.func_70451_h();

    private static final int AUTO_SORT_DISABLED = -1;

    private static final int AUTO_SORT_NORMAL = 0;

    private static final int AUTO_SORT_CUSTOM = 1;

    private static final double QUALITY_EPSILON = 1.0E-6;

    private static final Comparator<InvManager.PlannedAction> ACTION_COMPARATOR = (first, second) -> {
        int comparison = Integer.compare(first.clickCount, second.clickCount);
        if (comparison != 0) {
            return comparison;
        } else {
            comparison = Boolean.compare(second.completesTarget, first.completesTarget);
            if (comparison != 0) {
                return comparison;
            } else {
                comparison = Integer.compare(first.priorityIndex, second.priorityIndex);
                if (comparison != 0) {
                    return comparison;
                } else {
                    comparison = Integer.compare(first.displacesCorrectHotbar, second.displacesCorrectHotbar);
                    if (comparison != 0) {
                        return comparison;
                    } else {
                        comparison = Integer.compare(second.resultingSize, first.resultingSize);
                        if (comparison != 0) {
                            return comparison;
                        } else {
                            comparison = Integer.compare(first.targetHotbarSlot, second.targetHotbarSlot);
                            if (comparison != 0) {
                                return comparison;
                            } else {
                                comparison = Integer.compare(first.primarySourceIndex, second.primarySourceIndex);
                                return comparison != 0 ? comparison : Integer.compare(first.type.ordinal(), second.type.ordinal());
                            }
                        }
                    }
                }
            }
        }
    };

    private final SliderSetting autoSortMode;

    private final SliderSetting normalSortDelay;

    private final SliderSetting swordSlot;

    private final SliderSetting blocksSlot;

    private final SliderSetting goldenAppleSlot;

    private final SliderSetting projectileSlot;

    private final SliderSetting speedSlot;

    private final SliderSetting pearlSlot;

    private final SliderSetting targetCPS;

    private final ButtonSetting disableWhenComplete;

    private final ButtonSetting stackItems;

    private final InventoryItemListSetting items;

    private static SliderSetting chestStealer;

    private static SliderSetting autoArmor;

    private static SliderSetting inventoryCleaner;

    private final ButtonSetting closeChest;

    private final ButtonSetting closeInventory;

    private final ButtonSetting disableInLobby;

    private final ButtonSetting stealFromCustomChests;

    private final KeySetting cleanKey;

    private InvManager.PlannedAction currentAction;

    private int cursorRecoveryInventoryIndex = -1;

    private double windowClickBudget;

    private boolean sessionOpen;

    private InvManager.SessionState sessionState = InvManager.SessionState.ACTIVE;

    private long ticks = 0L;

    private long nextDelay = 0L;

    private boolean closeGui;

    private boolean closeInventoryGui;

    private boolean inventoryActionPerformed;

    private boolean inventoryScreenReady;

    private boolean preUpdatePacketWindow;

    private boolean sendingInventoryClick;

    private boolean externalInventoryClickPending;

    private final InvManager.CurrentArmor[] armorArr = InvManager.CurrentArmor.values();

    private static final InvManager.StepHook NO_OP = new InvManager.StepHook() {

        @Override
        public void run(InvManager module) {
        }
    };

    public InvManager() {
        super("InvManager", Module.category.player);
        this.liteModule = true;
        this.registerSetting(this.closeChest = new ButtonSetting("Close chest", false));
        this.registerSetting(this.closeInventory = new ButtonSetting("Close inventory", false));
        this.registerSetting(this.disableInLobby = new ButtonSetting("Disable in lobby", true));
        this.registerSetting(autoArmor = new SliderSetting("Auto armor", " tick", true, 3.0, 1.0, 20.0, 1.0));
        this.registerSetting(chestStealer = new SliderSetting("Chest stealer", " tick", true, 3.0, 1.0, 20.0, 1.0));
        this.registerSetting(this.stealFromCustomChests = new ButtonSetting("Steal from custom chests", false));
        this.registerSetting(inventoryCleaner = new SliderSetting("Inventory cleaner", " tick", true, 5.0, 1.0, 20.0, 1.0));
        this.registerSetting(this.cleanKey = new KeySetting("Clean key", 1002));
        this.registerSetting(this.autoSortMode = new SliderSetting("Auto sort mode", true, 0, new String[] { "Normal", "Custom" }));
        this.registerSetting(this.normalSortDelay = new SliderSetting("Auto sort", " tick", 3.0, 1.0, 20.0, 1.0, "Normal auto sort.Auto sort"));
        this.registerSetting(this.swordSlot = new SliderSetting("Sword slot", true, -1.0, 1.0, 9.0, 1.0, "Normal auto sort.Sword slot"));
        this.registerSetting(this.blocksSlot = new SliderSetting("Blocks slot", true, -1.0, 1.0, 9.0, 1.0, "Normal auto sort.Blocks slot"));
        this.registerSetting(this.goldenAppleSlot = new SliderSetting("Golden apple slot", true, -1.0, 1.0, 9.0, 1.0, "Normal auto sort.Golden apple slot"));
        this.registerSetting(this.projectileSlot = new SliderSetting("Projectile slot", true, -1.0, 1.0, 9.0, 1.0, "Normal auto sort.Projectile slot"));
        this.registerSetting(this.speedSlot = new SliderSetting("Speed potion slot", true, -1.0, 1.0, 9.0, 1.0, "Normal auto sort.Speed potion slot"));
        this.registerSetting(this.pearlSlot = new SliderSetting("Pearl slot", true, -1.0, 1.0, 9.0, 1.0, "Normal auto sort.Pearl slot"));
        this.registerSetting(this.targetCPS = new SliderSetting("Target CPS", 10.0, 1.0, 20.0, 0.5, "Auto sort.Target CPS"));
        this.registerSetting(this.disableWhenComplete = new ButtonSetting("Disable when complete", false, "Auto sort.Disable when complete"));
        this.registerSetting(this.stackItems = new ButtonSetting("Stack items", false, "Auto sort.Stack items"));
        this.registerSetting(this.items = new InventoryItemListSetting("Items", "Auto sort.Items"));
        this.closetModule = true;
    }

    @Override
    public void guiUpdate() {
        boolean normalMode = this.isAutoSortMode(0);
        this.normalSortDelay.setVisible(normalMode, this);
        this.swordSlot.setVisible(normalMode, this);
        this.blocksSlot.setVisible(normalMode, this);
        this.goldenAppleSlot.setVisible(normalMode, this);
        this.projectileSlot.setVisible(normalMode, this);
        this.speedSlot.setVisible(normalMode, this);
        this.pearlSlot.setVisible(normalMode, this);
        boolean customMode = this.isAutoSortMode(1);
        this.targetCPS.setVisible(customMode, this);
        this.disableWhenComplete.setVisible(customMode, this);
        this.stackItems.setVisible(customMode, this);
        this.items.setVisible(customMode, this);
    }

    @Override
    public void onEnable() {
        this.migrateLegacyAutoSortSetting();
        this.ticks = 0L;
        this.nextDelay = 0L;
        this.closeGui = false;
        this.closeInventoryGui = false;
        this.inventoryActionPerformed = false;
        this.inventoryScreenReady = false;
        this.preUpdatePacketWindow = false;
        this.sendingInventoryClick = false;
        this.externalInventoryClickPending = false;
        this.resetRuntimeState();
    }

    @Override
    public void onDisable() {
        this.ticks = 0L;
        this.nextDelay = 0L;
        this.closeGui = false;
        this.closeInventoryGui = false;
        this.inventoryActionPerformed = false;
        this.inventoryScreenReady = false;
        this.preUpdatePacketWindow = false;
        this.sendingInventoryClick = false;
        this.externalInventoryClickPending = false;
        this.resetRuntimeState();
    }

    @Override
    public void guiButtonToggled(ButtonSetting buttonSetting) {
        if (this.isAutoSortMode(1) && buttonSetting == this.disableWhenComplete && this.isManagedInventoryOpen()) {
            if (!this.disableWhenComplete.isToggled()) {
                if (this.sessionState == InvManager.SessionState.COMPLETE_LATCHED) {
                    this.sessionState = InvManager.SessionState.ACTIVE;
                }
            } else {
                if (this.currentAction == null && this.cursorRecoveryInventoryIndex < 0) {
                    InvManager.InventorySnapshot snapshot = InvManager.InventorySnapshot.capture();
                    InvManager.SnapshotContext context = InvManager.SnapshotContext.create(snapshot, this.resolveAssignments(snapshot));
                    if (this.findBestSortingAction(context) == null) {
                        this.sessionState = InvManager.SessionState.COMPLETE_LATCHED;
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onSlotScroll(PreSlotScrollEvent event) {
        if (this.shouldCancelManualInventoryInput()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onProfileLoad(PostProfileLoadEvent event) {
        this.migrateLegacyAutoSortSetting();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onSendPacket(SendPacketEvent event) {
        if (event.getPacket() instanceof C0EPacketClickWindow && mc.field_71462_r instanceof GuiInventory && !this.sendingInventoryClick) {
            this.externalInventoryClickPending = true;
            this.closeInventoryGui = false;
        }
    }

    private void migrateLegacyAutoSortSetting() {
        if (this.normalSortDelay.getInput() == -1.0) {
            this.autoSortMode.setValueRaw(-1.0);
            this.normalSortDelay.setValue(3.0);
        }
    }

    @SubscribeEvent
    public void onPreUpdate(PreUpdateEvent event) {
        this.preUpdatePacketWindow = true;
        try {
            this.handlePreUpdate();
        } finally {
            this.preUpdatePacketWindow = false;
        }
    }

    private void handlePreUpdate() {
        if (!Utils.nullCheck()) {
            this.closeGui = false;
            this.closeInventoryGui = false;
            this.inventoryActionPerformed = false;
            this.inventoryScreenReady = false;
            this.externalInventoryClickPending = false;
            this.closeSession();
        } else if (mc.field_71462_r == null) {
            this.closeInventoryGui = false;
            this.inventoryActionPerformed = false;
            this.inventoryScreenReady = false;
            this.externalInventoryClickPending = false;
            this.closeSession();
        } else {
            if (!(mc.field_71462_r instanceof GuiInventory)) {
                this.inventoryScreenReady = false;
                this.externalInventoryClickPending = false;
            }
            if (this.shouldPauseForLobby()) {
                this.inventoryScreenReady = false;
                this.closeSession();
            } else if (this.externalInventoryClickPending) {
                this.externalInventoryClickPending = false;
                this.closeSession();
            } else if (this.closeChest.isToggled() && this.closeGui) {
                this.closeGui = false;
                this.closeSession();
                mc.field_71439_g.func_71053_j();
            } else if (this.closeInventory.isToggled() && this.closeInventoryGui && mc.field_71462_r instanceof GuiInventory) {
                this.closeInventoryGui = false;
                this.closeSession();
                mc.field_71439_g.func_71053_j();
            } else if (mc.field_71462_r instanceof GuiInventory) {
                if (!this.inventoryScreenReady) {
                    this.inventoryScreenReady = true;
                    this.closeSession();
                } else {
                    this.handleInventoryScreen();
                }
            } else {
                this.closeSession();
                if (mc.field_71462_r instanceof GuiChest) {
                    this.handleChestScreen();
                }
            }
        }
    }

    private boolean shouldPauseForLobby() {
        return this.disableInLobby.isToggled() && Utils.isLobby() || ModuleManager.skyWars != null && ModuleManager.invmove != null && ModuleManager.skyWars.isEnabled() && ModuleManager.invmove.isEnabled() && ModuleManager.invmove.isCloseInventoryMode() && Utils.getSkyWarsStatus() != 2;
    }

    private void handleInventoryScreen() {
        if (!this.isManagedInventoryOpen()) {
            this.closeSession();
        } else if (this.consumeExternalActionDelay()) {
            if (!this.tryAutoArmorAction()) {
                if (this.isAutoSortMode(1)) {
                    if (this.runInventorySorterTick()) {
                        return;
                    }
                } else {
                    this.stopCustomSorter();
                    if (this.isAutoSortMode(0) && this.tryNormalAutoSortAction()) {
                        return;
                    }
                }
                if (!this.tryInventoryCleanerAction()) {
                    if (this.closeInventory.isToggled() && this.inventoryActionPerformed && !this.ownsCarriedStack()) {
                        this.closeInventoryGui = true;
                    }
                }
            }
        }
    }

    private boolean consumeExternalActionDelay() {
        if (this.nextDelay <= 0L) {
            return true;
        } else if (++this.ticks < this.nextDelay) {
            return false;
        } else {
            this.ticks = 0L;
            this.nextDelay = 0L;
            return true;
        }
    }

    private boolean isAutoSortMode(int mode) {
        return (int) this.autoSortMode.getInput() == mode;
    }

    private void stopCustomSorter() {
        if (this.sessionOpen || this.currentAction != null || this.cursorRecoveryInventoryIndex >= 0) {
            InvManager.InventorySnapshot snapshot = InvManager.InventorySnapshot.capture();
            if (this.ownsCarriedStack() && snapshot.carried != null) {
                this.recoverCarriedStackImmediately(snapshot, false);
            }
            this.closeSession();
        }
    }

    private boolean tryNormalAutoSortAction() {
        if (mc.field_71439_g.field_71071_by.func_70445_o() != null) {
            return false;
        } else {
            InvManager.InventoryData data = new InvManager.InventoryData(mc.field_71439_g.field_71071_by, true, false);
            if (this.fixNormalSwordSlot(data)) {
                return true;
            } else {
                for (int intValue20 = 0; intValue20 < data.size; intValue20++) {
                    ItemStack itemStack = data.inventory.func_70301_a(intValue20);
                    if (itemStack != null && !this.isSword(itemStack) && !this.isArmor(itemStack)) {
                        Item item = itemStack.func_77973_b();
                        if (item instanceof ItemBlock) {
                            int targetSlot = this.configuredHotbarSlot(this.blocksSlot);
                            if (targetSlot >= 0 && intValue20 != targetSlot) {
                                ItemStack currentBlocks = data.inventory.func_70301_a(targetSlot);
                                if (currentBlocks == null || !(currentBlocks.func_77973_b() instanceof ItemBlock)) {
                                    this.normalSortClick(intValue20, targetSlot, 2);
                                    return true;
                                }
                                if (currentBlocks.field_77994_a < currentBlocks.func_77976_d()) {
                                    if (canStacksMerge(itemStack, currentBlocks)) {
                                        if (itemStack.field_77994_a == itemStack.func_77976_d()) {
                                            this.normalSortClick(intValue20, targetSlot, 2);
                                            return true;
                                        }
                                        if (data.emptyWithoutHotbar != 0 || intValue20 >= HOTBAR_SIZE) {
                                            this.normalSortClick(intValue20, 0, 1);
                                            return true;
                                        }
                                    } else if (itemStack.field_77994_a > currentBlocks.field_77994_a) {
                                        this.normalSortClick(intValue20, targetSlot, 2);
                                        return true;
                                    }
                                }
                            }
                        } else if (item instanceof ItemAppleGold) {
                            int targetSlot = this.configuredHotbarSlot(this.goldenAppleSlot);
                            if (targetSlot >= 0 && intValue20 != targetSlot) {
                                ItemStack currentGoldenApple = data.inventory.func_70301_a(targetSlot);
                                if (currentGoldenApple == null || !(currentGoldenApple.func_77973_b() instanceof ItemAppleGold)) {
                                    this.normalSortClick(intValue20, targetSlot, 2);
                                    return true;
                                }
                            }
                        } else if (item instanceof ItemSnowball || item instanceof ItemEgg) {
                            int targetSlot = this.configuredHotbarSlot(this.projectileSlot);
                            if (targetSlot >= 0 && intValue20 != targetSlot) {
                                ItemStack currentProjectile = data.inventory.func_70301_a(targetSlot);
                                if (currentProjectile == null || !(currentProjectile.func_77973_b() instanceof ItemSnowball) && !(currentProjectile.func_77973_b() instanceof ItemEgg)) {
                                    this.normalSortClick(intValue20, targetSlot, 2);
                                    return true;
                                }
                                if (itemStack.field_77994_a >= itemStack.func_77976_d()) {
                                    if (itemStack.field_77994_a > currentProjectile.field_77994_a) {
                                        this.normalSortClick(intValue20, targetSlot, 2);
                                        return true;
                                    }
                                } else if (currentProjectile.field_77994_a < currentProjectile.func_77976_d()) {
                                    if (item == currentProjectile.func_77973_b()) {
                                        if (data.emptyWithoutHotbar != 0 || intValue20 >= HOTBAR_SIZE) {
                                            this.normalSortClick(intValue20, 0, 1);
                                            return true;
                                        }
                                    } else if (itemStack.field_77994_a > currentProjectile.field_77994_a) {
                                        this.normalSortClick(intValue20, targetSlot, 2);
                                        return true;
                                    }
                                }
                            }
                        } else if (item instanceof ItemPotion) {
                            int targetSlot = this.configuredHotbarSlot(this.speedSlot);
                            if (targetSlot >= 0 && intValue20 != targetSlot && this.isSpeedPotion(itemStack) && !this.isSpeedPotion(data.inventory.func_70301_a(targetSlot))) {
                                this.normalSortClick(intValue20, targetSlot, 2);
                                return true;
                            }
                        } else if (item instanceof ItemEnderPearl) {
                            int targetSlot = this.configuredHotbarSlot(this.pearlSlot);
                            if (targetSlot >= 0 && intValue20 != targetSlot) {
                                ItemStack currentPearl = data.inventory.func_70301_a(targetSlot);
                                if (currentPearl == null || !(currentPearl.func_77973_b() instanceof ItemEnderPearl)) {
                                    this.normalSortClick(intValue20, targetSlot, 2);
                                    return true;
                                }
                            }
                        }
                    }
                }
                return false;
            }
        }
    }

    private boolean fixNormalSwordSlot(InvManager.InventoryData data) {
        int targetSlot = this.configuredHotbarSlot(this.swordSlot);
        if (targetSlot < 0) {
            return false;
        } else {
            ItemStack targetStack = data.inventory.func_70301_a(targetSlot);
            double bestDamage = this.isSword(targetStack) ? Utils.getDamageLevel(targetStack) : -1.0;
            int intValue21 = -1;
            for (int intValue22 = 0; intValue22 < data.size; intValue22++) {
                if (intValue22 != targetSlot) {
                    ItemStack candidate = data.inventory.func_70301_a(intValue22);
                    if (this.isSword(candidate)) {
                        double damage = Utils.getDamageLevel(candidate);
                        if (damage > bestDamage) {
                            bestDamage = damage;
                            intValue21 = intValue22;
                        }
                    }
                }
            }
            if (intValue21 < 0) {
                return false;
            } else {
                this.normalSortClick(intValue21, targetSlot, 2);
                return true;
            }
        }
    }

    private int configuredHotbarSlot(SliderSetting setting) {
        return (int) setting.getInput() - 1;
    }

    private void normalSortClick(int intValue2, int button, int mode) {
        this.delayedClick(toContainerSlot(intValue2), button, mode, this.normalSortDelay);
    }

    private boolean runInventorySorterTick() {
        InvManager.InventorySnapshot snapshot = InvManager.InventorySnapshot.capture();
        if (!this.sessionOpen) {
            this.openSession(snapshot);
        }
        if (this.sessionState == InvManager.SessionState.COMPLETE_LATCHED) {
            return false;
        } else {
            InvManager.SlotAssignment[] assignments = this.resolveAssignments(snapshot);
            InvManager.SnapshotContext context = InvManager.SnapshotContext.create(snapshot, assignments);
            if (this.currentAction == null) {
                if (snapshot.carried != null) {
                    this.currentAction = this.buildRecoveryAction(context, InvManager.PlanType.RECOVER_CURSOR, false);
                    if (this.currentAction == null) {
                        this.sessionState = InvManager.SessionState.ACTIVE;
                        return true;
                    }
                    this.sessionState = InvManager.SessionState.EXECUTING;
                } else {
                    this.cursorRecoveryInventoryIndex = -1;
                    this.currentAction = this.findBestSortingAction(context);
                    if (this.currentAction == null) {
                        this.sessionState = this.disableWhenComplete.isToggled() ? InvManager.SessionState.COMPLETE_LATCHED : InvManager.SessionState.ACTIVE;
                        return false;
                    }
                    this.sessionState = InvManager.SessionState.EXECUTING;
                }
            }
            int clickBudget = this.consumeWindowClickBudget();
            if (clickBudget <= 0) {
                return true;
            } else {
                return this.executeCurrentStep(context) ? true : this.currentAction != null || context.snapshot.carried != null;
            }
        }
    }

    private boolean tryAutoArmorAction() {
        if (autoArmor.getInput() != -1.0 && mc.field_71439_g.field_71071_by.func_70445_o() == null) {
            this.updateCurrentArmor();
            InvManager.InventoryData data = new InvManager.InventoryData(mc.field_71439_g.field_71071_by, true, true);
            for (int i = 0; i < data.armorData[0].length; i++) {
                if (data.armorData[1][i] != -1) {
                    InvManager.CurrentArmor currentArmor = this.armorArr[i];
                    if (data.armorData[1][i] > currentArmor.defenceLevel) {
                        if (currentArmor.getItemStack() != null) {
                            this.delayedClick(currentArmor.invSlot, 0, 4, autoArmor);
                        }
                        this.delayedClick(toContainerSlot(data.armorData[0][i]), 0, 1, autoArmor);
                        return true;
                    }
                }
            }
            return false;
        } else {
            return false;
        }
    }

    private boolean tryInventoryCleanerAction() {
        if (inventoryCleaner.getInput() != -1.0 && mc.field_71439_g.field_71071_by.func_70445_o() == null) {
            if (this.cleanKey.getKey() != 0 && !this.cleanKey.isPressed()) {
                return false;
            } else {
                this.updateCurrentArmor();
                InvManager.InventoryData data = new InvManager.InventoryData(mc.field_71439_g.field_71071_by, true, false);
                int[] bestArmorLevels = new int[4];
                boolean[] keptBestArmor = new boolean[4];
                for (int intValue23 = 0; intValue23 < bestArmorLevels.length; intValue23++) {
                    bestArmorLevels[intValue23] = Math.max(this.armorArr[intValue23].defenceLevel, data.armorData[1][intValue23]);
                    if (this.armorArr[intValue23].defenceLevel == bestArmorLevels[intValue23] && bestArmorLevels[intValue23] > 0) {
                        keptBestArmor[intValue23] = true;
                    }
                }
                List<Integer> duplicateItems = new ArrayList<>();
                boolean keptBestSword = false;
                boolean keptBestPick = false;
                boolean keptBestAxe = false;
                boolean keptBestShovel = false;
                int j = 0;
                while (j < data.size) {
                    int slotId;
                    label118: {
                        ItemStack itemStack = data.inventory.func_70301_a(j);
                        if (itemStack != null) {
                            Item item = itemStack.func_77973_b();
                            slotId = toContainerSlot(j);
                            if (this.isSword(itemStack)) {
                                double damage = Utils.getDamageLevel(itemStack);
                                if (damage < data.bestSwordDamage || keptBestSword && damage == data.bestSwordDamage) {
                                    this.delayedClick(slotId, 1, 4, inventoryCleaner);
                                    return true;
                                }
                                keptBestSword = true;
                            } else if (this.isArmor(itemStack)) {
                                ItemArmor armor = (ItemArmor) item;
                                int armorSlot = 3 - armor.field_77881_a;
                                int defenceLevel = this.getDefenceLevel(itemStack);
                                int bestLevel = bestArmorLevels[armorSlot];
                                if (defenceLevel < bestLevel) {
                                    this.delayedClick(slotId, 1, 4, inventoryCleaner);
                                    return true;
                                }
                                if (defenceLevel == bestLevel) {
                                    if (keptBestArmor[armorSlot]) {
                                        this.delayedClick(slotId, 1, 4, inventoryCleaner);
                                        return true;
                                    }
                                    keptBestArmor[armorSlot] = true;
                                } else {
                                    bestArmorLevels[armorSlot] = defenceLevel;
                                    keptBestArmor[armorSlot] = true;
                                }
                            } else if (item instanceof ItemPickaxe) {
                                double efficiency = Utils.getEfficiency(itemStack, Blocks.field_150348_b);
                                if (efficiency < data.bestPickaxe || keptBestPick && efficiency == data.bestPickaxe) {
                                    this.delayedClick(slotId, 1, 4, inventoryCleaner);
                                    return true;
                                }
                                keptBestPick = true;
                            } else if (item instanceof ItemAxe) {
                                double efficiency = Utils.getEfficiency(itemStack, Blocks.field_150364_r);
                                if (efficiency < data.bestAxe || keptBestAxe && efficiency == data.bestAxe) {
                                    this.delayedClick(slotId, 1, 4, inventoryCleaner);
                                    return true;
                                }
                                keptBestAxe = true;
                            } else if (item instanceof ItemSpade) {
                                double efficiency = Utils.getEfficiency(itemStack, Blocks.field_150346_d);
                                if (efficiency < data.bestShovel || keptBestShovel && efficiency == data.bestShovel) {
                                    this.delayedClick(slotId, 1, 4, inventoryCleaner);
                                    return true;
                                }
                                keptBestShovel = true;
                            } else if (!this.isProtectedInventoryItem(itemStack) && item != Items.field_151063_bx) {
                                if (item instanceof ItemPotion) {
                                    if (this.isBadPotion(itemStack)) {
                                        break label118;
                                    }
                                } else {
                                    if (itemStack.func_77976_d() != 1) {
                                        break label118;
                                    }
                                    int id = Item.func_150891_b(item);
                                    if (duplicateItems.contains(id)) {
                                        break label118;
                                    }
                                    duplicateItems.add(id);
                                }
                            }
                        }
                        j++;
                        continue;
                    }
                    this.delayedClick(slotId, 1, 4, inventoryCleaner);
                    return true;
                }
                return false;
            }
        } else {
            return false;
        }
    }

    private void handleChestScreen() {
        if (chestStealer.getInput() != -1.0 && mc.field_71439_g.field_71070_bA instanceof ContainerChest) {
            if (this.consumeExternalActionDelay()) {
                IInventory chestInventory = ((ContainerChest) mc.field_71439_g.field_71070_bA).func_85151_d();
                if (this.stealFromCustomChests.isToggled() || chestInventory.func_70005_c_().contains("Chest")) {
                    this.updateCurrentArmor();
                    InvManager.InventoryData chestData = new InvManager.InventoryData(chestInventory, false, false);
                    InvManager.InventoryData playerData = new InvManager.InventoryData(mc.field_71439_g.field_71071_by, true, false);
                    chestData.compareAndRemove(playerData);
                    if (!this.isAutoSortMode(1)) {
                        this.handleNormalChestStealer(chestData, playerData);
                    } else {
                        if (playerData.size != playerData.filled) {
                            if (chestData.swordData[1] != -1.0) {
                                this.stealChestItem((int) chestData.swordData[0], chestData.inventory.func_70301_a((int) chestData.swordData[0]), playerData);
                                return;
                            }
                            for (int k = 0; k < chestData.armorData[0].length; k++) {
                                if (chestData.armorData[1][k] != -1) {
                                    InvManager.CurrentArmor currentArmor = this.armorArr[k];
                                    if (chestData.armorData[1][k] > currentArmor.defenceLevel) {
                                        int chestSlot = chestData.armorData[0][k];
                                        this.stealChestItem(chestSlot, chestData.inventory.func_70301_a(chestSlot), playerData);
                                        return;
                                    }
                                }
                            }
                            for (int kx = 0; kx < chestData.size; kx++) {
                                ItemStack itemStack = chestData.inventory.func_70301_a(kx);
                                if (itemStack != null && !this.isSword(itemStack) && !this.isArmor(itemStack) && !this.shouldSkipChestItem(itemStack, playerData)) {
                                    this.stealChestItem(kx, itemStack, playerData);
                                    return;
                                }
                            }
                        } else {
                            for (int intValue24 = 0; intValue24 < chestData.size; intValue24++) {
                                ItemStack chestStack = chestData.inventory.func_70301_a(intValue24);
                                if (chestStack != null && !this.shouldSkipChestItem(chestStack, playerData) && this.canMergeChestStackIntoInventory(chestStack, playerData)) {
                                    this.stealChestItem(intValue24, chestStack, playerData);
                                    return;
                                }
                            }
                        }
                        if (this.closeChest.isToggled()) {
                            this.closeGui = true;
                        }
                    }
                }
            }
        }
    }

    private void handleNormalChestStealer(InvManager.InventoryData chestData, InvManager.InventoryData playerData) {
        if (playerData.size != playerData.filled) {
            if (this.stealBestSwordToNormalSlot(chestData, playerData)) {
                return;
            }
            for (int intValue25 = 0; intValue25 < chestData.armorData[0].length; intValue25++) {
                if (chestData.armorData[1][intValue25] != -1) {
                    InvManager.CurrentArmor currentArmor = this.armorArr[intValue25];
                    if (chestData.armorData[1][intValue25] > currentArmor.defenceLevel) {
                        this.normalChestClick(chestData.armorData[0][intValue25], 0, 1);
                        return;
                    }
                }
            }
            for (int chestSlot = 0; chestSlot < chestData.size; chestSlot++) {
                ItemStack chestStack = chestData.inventory.func_70301_a(chestSlot);
                if (chestStack != null && !this.isSword(chestStack) && !this.isArmor(chestStack) && !this.shouldSkipChestItem(chestStack, playerData)) {
                    Item item = chestStack.func_77973_b();
                    if (item instanceof ItemBlock) {
                        int targetSlot = this.configuredHotbarSlot(this.blocksSlot);
                        if (targetSlot < 0) {
                            this.normalChestClick(chestSlot, 0, 1);
                            return;
                        }
                        ItemStack currentBlocks = playerData.inventory.func_70301_a(targetSlot);
                        if (currentBlocks == null || !(currentBlocks.func_77973_b() instanceof ItemBlock) || ((ItemBlock) item).func_179223_d() != ((ItemBlock) currentBlocks.func_77973_b()).func_179223_d() && chestStack.field_77994_a > currentBlocks.field_77994_a) {
                            this.normalChestClick(chestSlot, targetSlot, 2);
                            return;
                        }
                    } else if (item instanceof ItemAppleGold) {
                        int intValue26 = this.configuredHotbarSlot(this.goldenAppleSlot);
                        if (intValue26 < 0) {
                            this.normalChestClick(chestSlot, 0, 1);
                            return;
                        }
                        ItemStack currentGoldenApple = playerData.inventory.func_70301_a(intValue26);
                        if (currentGoldenApple == null || !(currentGoldenApple.func_77973_b() instanceof ItemAppleGold)) {
                            this.normalChestClick(chestSlot, intValue26, 2);
                            return;
                        }
                    } else if (item instanceof ItemSnowball || item instanceof ItemEgg) {
                        int intValue27 = this.configuredHotbarSlot(this.projectileSlot);
                        if (intValue27 < 0) {
                            this.normalChestClick(chestSlot, 0, 1);
                            return;
                        }
                        ItemStack currentProjectile = playerData.inventory.func_70301_a(intValue27);
                        if (currentProjectile == null || !(currentProjectile.func_77973_b() instanceof ItemSnowball) && !(currentProjectile.func_77973_b() instanceof ItemEgg)) {
                            this.normalChestClick(chestSlot, intValue27, 2);
                            return;
                        }
                        if (chestStack.field_77994_a > currentProjectile.field_77994_a) {
                            if (item == currentProjectile.func_77973_b() && chestStack.field_77994_a <= chestStack.func_77976_d()) {
                                this.normalChestClick(chestSlot, 0, 1);
                            } else {
                                this.normalChestClick(chestSlot, intValue27, 2);
                            }
                            return;
                        }
                    } else if (item instanceof ItemPotion && this.isSpeedPotion(chestStack)) {
                        int intValue28 = this.configuredHotbarSlot(this.speedSlot);
                        if (intValue28 < 0) {
                            this.normalChestClick(chestSlot, 0, 1);
                            return;
                        }
                        if (!this.isSpeedPotion(playerData.inventory.func_70301_a(intValue28))) {
                            this.normalChestClick(chestSlot, intValue28, 2);
                            return;
                        }
                    } else if (item instanceof ItemEnderPearl) {
                        int intValue29 = this.configuredHotbarSlot(this.pearlSlot);
                        if (intValue29 < 0) {
                            this.normalChestClick(chestSlot, 0, 1);
                            return;
                        }
                        ItemStack currentPearl = playerData.inventory.func_70301_a(intValue29);
                        if (currentPearl == null || !(currentPearl.func_77973_b() instanceof ItemEnderPearl)) {
                            this.normalChestClick(chestSlot, intValue29, 2);
                            return;
                        }
                    }
                    this.normalChestClick(chestSlot, 0, 1);
                    return;
                }
            }
        } else {
            for (int intValue30 = 0; intValue30 < chestData.size; intValue30++) {
                ItemStack chestStack = chestData.inventory.func_70301_a(intValue30);
                if (chestStack != null && !this.shouldSkipChestItem(chestStack, playerData) && this.isNormalMergeItem(chestStack)) {
                    for (int intValue31 = 0; intValue31 < playerData.size; intValue31++) {
                        ItemStack inventoryStack = playerData.inventory.func_70301_a(intValue31);
                        if (inventoryStack != null && canStacksMerge(chestStack, inventoryStack) && inventoryStack.field_77994_a < inventoryStack.func_77976_d()) {
                            this.normalChestClick(intValue30, 0, 1);
                            return;
                        }
                    }
                }
            }
        }
        if (this.closeChest.isToggled()) {
            this.closeGui = true;
        }
    }

    private boolean stealBestSwordToNormalSlot(InvManager.InventoryData chestData, InvManager.InventoryData playerData) {
        int targetSlot = this.configuredHotbarSlot(this.swordSlot);
        if (targetSlot >= 0 && chestData.swordData[1] != -1.0) {
            ItemStack currentSword = playerData.inventory.func_70301_a(targetSlot);
            double currentDamage = this.isSword(currentSword) ? Utils.getDamageLevel(currentSword) : -1.0;
            if (chestData.swordData[1] <= currentDamage) {
                return false;
            } else {
                this.normalChestClick((int) chestData.swordData[0], targetSlot, 2);
                return true;
            }
        } else {
            return false;
        }
    }

    private boolean isNormalMergeItem(ItemStack stack) {
        Item item = stack.func_77973_b();
        return item instanceof ItemBlock || item instanceof ItemAppleGold || item instanceof ItemSnowball || item instanceof ItemEgg || item instanceof ItemEnderPearl;
    }

    private void normalChestClick(int chestSlot, int button, int mode) {
        this.delayedClick(chestSlot, button, mode, chestStealer);
    }

    private void stealChestItem(int chestSlot, ItemStack stack, InvManager.InventoryData playerData) {
        int targetHotbarSlot = this.getAssignedHotbarSlot(stack);
        if (targetHotbarSlot >= 0 && playerData.inventory.func_70301_a(targetHotbarSlot) == null) {
            this.delayedClick(chestSlot, targetHotbarSlot, 2, chestStealer);
        } else {
            this.delayedClick(chestSlot, 0, 1, chestStealer);
        }
    }

    private int getAssignedHotbarSlot(ItemStack stack) {
        if (this.isAutoSortMode(1) && stack != null) {
            for (String storageId : this.items.getItems()) {
                if (ItemSearchIndex.matches(storageId, stack)) {
                    Integer assignedSlot = this.items.getAssignedSlot(storageId);
                    if (assignedSlot == null) {
                        return -1;
                    }
                    int hotbarSlot = assignedSlot - 1;
                    return hotbarSlot >= 0 && hotbarSlot < HOTBAR_SIZE ? hotbarSlot : -1;
                }
            }
            return -1;
        } else {
            return -1;
        }
    }

    private void delayedClick(int slotId, int button, int mode, SliderSetting delaySlider) {
        this.nextDelay = (long) delaySlider.getInput();
        this.ticks = 0L;
        this.closeSession();
        this.click(slotId, button, mode);
    }

    private boolean isProtectedInventoryItem(ItemStack itemStack) {
        if (itemStack == null) {
            return false;
        } else {
            Item item = itemStack.func_77973_b();
            return this.isAutoSortMode(1) && this.items.matches(itemStack) || item instanceof ItemBlock && !(((ItemBlock) item).func_179223_d() instanceof BlockRailBase) || item instanceof ItemAppleGold || item instanceof ItemSnowball || item instanceof ItemEgg || item instanceof ItemEnderPearl || item == Items.field_151032_g;
        }
    }

    private boolean canMergeChestStackIntoInventory(ItemStack chestStack, InvManager.InventoryData playerData) {
        Item chestItem = chestStack.func_77973_b();
        if (this.isProtectedInventoryItem(chestStack) || chestItem instanceof ItemPotion && !this.isBadPotion(chestStack)) {
            for (int intValue32 = 0; intValue32 < playerData.size; intValue32++) {
                ItemStack playerStack = playerData.inventory.func_70301_a(intValue32);
                if (playerStack != null && canStacksMerge(playerStack, chestStack) && playerStack.field_77994_a < playerStack.func_77976_d()) {
                    return true;
                }
            }
            return false;
        } else {
            return false;
        }
    }

    private boolean isSword(ItemStack itemStack) {
        return itemStack != null && itemStack.func_77973_b() instanceof ItemSword;
    }

    private boolean isArmor(ItemStack itemStack) {
        return itemStack != null && itemStack.func_77973_b() instanceof ItemArmor;
    }

    private int getDefenceLevel(ItemStack itemStack) {
        return ((ItemArmor) itemStack.func_77973_b()).field_77879_b + EnchantmentHelper.func_77508_a(new ItemStack[] { itemStack }, DamageSource.field_76377_j);
    }

    private void updateCurrentArmor() {
        for (InvManager.CurrentArmor armor : this.armorArr) {
            ItemStack itemStack = armor.getItemStack();
            armor.defenceLevel = this.isArmor(itemStack) ? this.getDefenceLevel(itemStack) : 0;
        }
    }

    private boolean isSpeedPotion(ItemStack itemStack) {
        if (itemStack != null && itemStack.func_77973_b() instanceof ItemPotion) {
            List<PotionEffect> effects = ((ItemPotion) itemStack.func_77973_b()).func_77832_l(itemStack);
            if (effects == null) {
                return false;
            } else {
                for (PotionEffect effect : effects) {
                    if (effect.toString().contains("moveSpeed")) {
                        return true;
                    }
                }
                return false;
            }
        } else {
            return false;
        }
    }

    private boolean isBadPotion(ItemStack itemStack) {
        if (itemStack != null && itemStack.func_77973_b() instanceof ItemPotion) {
            List<PotionEffect> effects = ((ItemPotion) itemStack.func_77973_b()).func_77832_l(itemStack);
            if (effects != null && !effects.isEmpty()) {
                for (PotionEffect effect : effects) {
                    String desc = effect.toString();
                    if (desc.contains("poison") || desc.contains("moveSlowdown") || desc.contains("weakness") || desc.contains("harm") || desc.contains("digSlowDown")) {
                        return true;
                    }
                }
                return false;
            } else {
                return true;
            }
        } else {
            return false;
        }
    }

    private boolean shouldSkipChestItem(ItemStack stack, InvManager.InventoryData playerData) {
        Item item = stack.func_77973_b();
        if (this.isAutoSortMode(1) && this.items.matches(stack)) {
            return false;
        } else if (item instanceof ItemPotion && this.isBadPotion(stack)) {
            return true;
        } else if (item == Items.field_151063_bx) {
            return true;
        } else if (item instanceof ItemPickaxe) {
            double efficiency = Utils.getEfficiency(stack, Blocks.field_150348_b);
            return playerData.bestPickaxe >= 0.0 && efficiency <= playerData.bestPickaxe;
        } else if (item instanceof ItemAxe) {
            double efficiency = Utils.getEfficiency(stack, Blocks.field_150364_r);
            return playerData.bestAxe >= 0.0 && efficiency <= playerData.bestAxe;
        } else if (item instanceof ItemSpade) {
            double efficiency = Utils.getEfficiency(stack, Blocks.field_150346_d);
            return playerData.bestShovel >= 0.0 && efficiency <= playerData.bestShovel;
        } else if (!this.isProtectedInventoryItem(stack) && (!(item instanceof ItemPotion) || this.isBadPotion(stack))) {
            if (stack.func_77976_d() == 1) {
                int id = Item.func_150891_b(item);
                return playerData.uniqueSingles.contains(id);
            } else {
                return true;
            }
        } else {
            return false;
        }
    }

    public boolean shouldCancelManualInventoryInput() {
        return this.isEnabled() && this.isAutoSortMode(1) && this.isManagedInventoryOpen() && this.sessionState == InvManager.SessionState.EXECUTING && this.currentAction != null;
    }

    public boolean isSendingInventoryClick() {
        return this.sendingInventoryClick;
    }

    private void resetRuntimeState() {
        this.currentAction = null;
        this.cursorRecoveryInventoryIndex = -1;
        this.windowClickBudget = 0.0;
        this.sessionOpen = false;
        this.sessionState = InvManager.SessionState.ACTIVE;
    }

    private void openSession(InvManager.InventorySnapshot snapshot) {
        this.sessionOpen = true;
        this.currentAction = null;
        this.cursorRecoveryInventoryIndex = -1;
        this.windowClickBudget = 0.0;
        this.sessionState = InvManager.SessionState.ACTIVE;
        if (this.disableWhenComplete.isToggled()) {
            InvManager.SnapshotContext context = InvManager.SnapshotContext.create(snapshot, this.resolveAssignments(snapshot));
            if (this.findBestSortingAction(context) == null) {
                this.sessionState = InvManager.SessionState.COMPLETE_LATCHED;
            }
        }
    }

    private void closeSession() {
        this.sessionOpen = false;
        this.currentAction = null;
        this.cursorRecoveryInventoryIndex = -1;
        this.windowClickBudget = 0.0;
        this.sessionState = InvManager.SessionState.ACTIVE;
    }

    private boolean ownsCarriedStack() {
        return this.currentAction != null || this.cursorRecoveryInventoryIndex >= 0;
    }

    private int consumeWindowClickBudget() {
        this.windowClickBudget = this.windowClickBudget + Math.max(0.0, this.targetCPS.getInput()) / 20.0;
        int clicks = Math.min(1, (int) this.windowClickBudget);
        if (clicks <= 0) {
            return 0;
        } else {
            this.windowClickBudget -= clicks;
            this.windowClickBudget = Math.min(this.windowClickBudget, 1.0);
            return clicks;
        }
    }

    private boolean executeCurrentStep(InvManager.SnapshotContext context) {
        if (this.currentAction == null) {
            return false;
        } else {
            InvManager.PlannedClick step = this.currentAction.peek();
            if (step == null) {
                this.currentAction = null;
                this.sessionState = InvManager.SessionState.ACTIVE;
                return false;
            } else if (!step.validator.isValid(context, this)) {
                this.currentAction = null;
                this.sessionState = InvManager.SessionState.ACTIVE;
                if (context.snapshot.carried != null && this.cursorRecoveryInventoryIndex >= 0) {
                    InvManager.PlannedAction recoveryAction = this.buildRecoveryAction(context, InvManager.PlanType.RECOVER_CURSOR, false);
                    if (recoveryAction != null) {
                        this.currentAction = recoveryAction;
                        this.sessionState = InvManager.SessionState.EXECUTING;
                        return this.executeCurrentStep(context);
                    }
                }
                if (context.snapshot.carried == null) {
                    this.cursorRecoveryInventoryIndex = -1;
                }
                return false;
            } else {
                step.beforeExecute.run(this);
                this.click(step.slotId, step.button, step.mode);
                step.afterExecute.run(this);
                this.currentAction.advance();
                if (this.currentAction.isComplete()) {
                    this.currentAction = null;
                    if (this.sessionState != InvManager.SessionState.COMPLETE_LATCHED) {
                        this.sessionState = InvManager.SessionState.ACTIVE;
                    }
                }
                return true;
            }
        }
    }

    private InvManager.PlannedAction findBestSortingAction(InvManager.SnapshotContext context) {
        InvManager.PlannedAction bestAction = null;
        for (InvManager.SlotAssignment assignment : context.assignments) {
            if (assignment != null) {
                ItemStack targetStack = context.snapshot.getSlot(assignment.hotbarSlot);
                boolean targetMatchesRule = this.isAssignedTargetSatisfied(context, assignment, targetStack);
                if (targetMatchesRule && isPartialStack(targetStack)) {
                    InvManager.PlannedAction mergeAction = this.buildMergeAction(context, assignment, targetStack);
                    bestAction = pickBetterAction(bestAction, mergeAction);
                    InvManager.PlannedAction pickupAllAction = this.buildPickupAllAction(context, assignment, targetStack);
                    bestAction = pickBetterAction(bestAction, pickupAllAction);
                } else if (!targetMatchesRule) {
                    InvManager.PlannedAction placementAction = this.buildPlacementAction(context, assignment);
                    InvManager.PlannedAction pickupAllPlacementAction = this.buildPickupAllPlacementAction(context, assignment, targetStack);
                    if (pickupAllPlacementAction == null || placementAction != null && pickupAllPlacementAction.resultingSize <= placementAction.resultingSize) {
                        if (placementAction != null) {
                            bestAction = pickBetterAction(bestAction, placementAction);
                        }
                    } else {
                        bestAction = pickBetterAction(bestAction, pickupAllPlacementAction);
                    }
                }
            }
        }
        return bestAction == null && this.stackItems.isToggled() ? this.buildUnconfiguredStackAction(context) : bestAction;
    }

    private InvManager.PlannedAction buildPlacementAction(InvManager.SnapshotContext context, InvManager.SlotAssignment assignment) {
        InvManager.PlacementSource bestSource = this.findBestPlacementSource(context, assignment, assignment.hotbarSlot);
        if (bestSource == null) {
            return null;
        } else {
            final int inventoryIndex2 = bestSource.inventoryIndex;
            List<InvManager.PlannedClick> steps = new ArrayList<>(1);
            steps.add(new InvManager.PlannedClick(toContainerSlot(inventoryIndex2), assignment.hotbarSlot, 2, new InvManager.StepValidator() {

                @Override
                public boolean isValid(InvManager.SnapshotContext current, InvManager module) {
                    return current.snapshot.carried == null && ItemSearchIndex.matches(assignment.storageId, current.snapshot.getSlot(inventoryIndex2)) && (inventoryIndex2 >= InvManager.HOTBAR_SIZE || !module.isCorrectHotbarSlot(current, inventoryIndex2));
                }
            }, NO_OP, NO_OP));
            return new InvManager.PlannedAction(InvManager.PlanType.PLACE_TO_HOTBAR, 1, assignment.hotbarSlot, assignment.priorityIndex, bestSource.resultingSize, true, 0, inventoryIndex2, steps);
        }
    }

    private InvManager.PlannedAction buildMergeAction(InvManager.SnapshotContext context, InvManager.SlotAssignment assignment, ItemStack targetStack) {
        InvManager.MergePlanData mergePlan = this.buildMergePlanData(context, assignment, targetStack);
        if (mergePlan == null) {
            return null;
        } else {
            final int targetHotbarSlot = assignment.hotbarSlot;
            final String storageId = assignment.storageId;
            List<InvManager.PlannedClick> steps = new ArrayList<>(mergePlan.clickCount);
            for (InvManager.MergeUse mergeUse : mergePlan.uses) {
                final int inventoryIndex3 = mergeUse.source.inventoryIndex;
                if (mergeUse.source.type == InvManager.MergeSourceType.MAIN_INVENTORY) {
                    steps.add(new InvManager.PlannedClick(toContainerSlot(inventoryIndex3), 0, 1, new InvManager.StepValidator() {

                        @Override
                        public boolean isValid(InvManager.SnapshotContext current, InvManager module) {
                            ItemStack sourceStack = current.snapshot.getSlot(inventoryIndex3);
                            ItemStack currentTarget = current.snapshot.getSlot(targetHotbarSlot);
                            return current.snapshot.carried == null && ItemSearchIndex.matches(storageId, currentTarget) && InvManager.canStacksMerge(sourceStack, currentTarget) && InvManager.isPartialStack(currentTarget) && InvManager.isQuickMoveMergeSafe(current.snapshot, targetHotbarSlot, sourceStack, currentTarget);
                        }
                    }, NO_OP, NO_OP));
                } else {
                    steps.add(new InvManager.PlannedClick(toContainerSlot(inventoryIndex3), 0, 0, new InvManager.StepValidator() {

                        @Override
                        public boolean isValid(InvManager.SnapshotContext current, InvManager module) {
                            ItemStack sourceStack = current.snapshot.getSlot(inventoryIndex3);
                            ItemStack currentTarget = current.snapshot.getSlot(targetHotbarSlot);
                            return current.snapshot.carried == null && ItemSearchIndex.matches(storageId, currentTarget) && InvManager.canStacksMerge(sourceStack, currentTarget) && InvManager.isPartialStack(currentTarget) && !module.isCorrectHotbarSlot(current, inventoryIndex3);
                        }
                    }, new InvManager.StepHook() {

                        @Override
                        public void run(InvManager module) {
                            module.cursorRecoveryInventoryIndex = inventoryIndex3;
                        }
                    }, NO_OP));
                    steps.add(new InvManager.PlannedClick(toContainerSlot(targetHotbarSlot), 0, 0, new InvManager.StepValidator() {

                        @Override
                        public boolean isValid(InvManager.SnapshotContext current, InvManager module) {
                            ItemStack currentTarget = current.snapshot.getSlot(targetHotbarSlot);
                            return current.snapshot.carried != null && ItemSearchIndex.matches(storageId, currentTarget) && InvManager.canStacksMerge(current.snapshot.carried, currentTarget) && InvManager.isPartialStack(currentTarget);
                        }
                    }, NO_OP, mergeUse.returnsRemainder ? NO_OP : new InvManager.StepHook() {

                        @Override
                        public void run(InvManager module) {
                            module.cursorRecoveryInventoryIndex = -1;
                        }
                    }));
                    if (mergeUse.returnsRemainder) {
                        steps.add(new InvManager.PlannedClick(toContainerSlot(inventoryIndex3), 0, 0, new InvManager.StepValidator() {

                            @Override
                            public boolean isValid(InvManager.SnapshotContext current, InvManager module) {
                                return current.snapshot.carried != null && InvManager.canDepositToSlot(current.snapshot, inventoryIndex3);
                            }
                        }, NO_OP, new InvManager.StepHook() {

                            @Override
                            public void run(InvManager module) {
                                module.cursorRecoveryInventoryIndex = -1;
                            }
                        }));
                    }
                }
            }
            return new InvManager.PlannedAction(InvManager.PlanType.MERGE_TO_TARGET, mergePlan.clickCount, assignment.hotbarSlot, assignment.priorityIndex, mergePlan.resultingSize, mergePlan.resultingSize >= targetStack.func_77976_d(), 0, mergePlan.primarySourceIndex, steps);
        }
    }

    private InvManager.PlannedAction buildPickupAllAction(InvManager.SnapshotContext context, InvManager.SlotAssignment assignment, ItemStack targetStack) {
        int resultingSize = this.getPickupAllResultingSize(context, assignment.hotbarSlot, targetStack);
        if (resultingSize <= targetStack.field_77994_a) {
            return null;
        } else {
            final int targetHotbarSlot = assignment.hotbarSlot;
            final String storageId = assignment.storageId;
            List<InvManager.PlannedClick> steps = new ArrayList<>(3);
            steps.add(new InvManager.PlannedClick(toContainerSlot(targetHotbarSlot), 0, 0, new InvManager.StepValidator() {

                @Override
                public boolean isValid(InvManager.SnapshotContext current, InvManager module) {
                    ItemStack currentTarget = current.snapshot.getSlot(targetHotbarSlot);
                    return current.snapshot.carried == null && ItemSearchIndex.matches(storageId, currentTarget) && InvManager.isPartialStack(currentTarget) && module.getPickupAllResultingSize(current, targetHotbarSlot, currentTarget) > currentTarget.field_77994_a;
                }
            }, new InvManager.StepHook() {

                @Override
                public void run(InvManager module) {
                    module.cursorRecoveryInventoryIndex = targetHotbarSlot;
                }
            }, NO_OP));
            steps.add(new InvManager.PlannedClick(toContainerSlot(targetHotbarSlot), 0, 6, new InvManager.StepValidator() {

                @Override
                public boolean isValid(InvManager.SnapshotContext current, InvManager module) {
                    return current.snapshot.carried != null && current.snapshot.getSlot(targetHotbarSlot) == null && module.getPickupAllResultingSize(current, targetHotbarSlot, current.snapshot.carried) > current.snapshot.carried.field_77994_a;
                }
            }, NO_OP, NO_OP));
            steps.add(new InvManager.PlannedClick(toContainerSlot(targetHotbarSlot), 0, 0, new InvManager.StepValidator() {

                @Override
                public boolean isValid(InvManager.SnapshotContext current, InvManager module) {
                    return current.snapshot.carried != null && InvManager.canDepositToSlot(current.snapshot, targetHotbarSlot);
                }
            }, NO_OP, new InvManager.StepHook() {

                @Override
                public void run(InvManager module) {
                    module.cursorRecoveryInventoryIndex = -1;
                }
            }));
            return new InvManager.PlannedAction(InvManager.PlanType.PICKUP_ALL_TO_TARGET, 3, assignment.hotbarSlot, assignment.priorityIndex, resultingSize, resultingSize >= targetStack.func_77976_d(), 0, assignment.hotbarSlot, steps);
        }
    }

    private InvManager.PlannedAction buildPickupAllPlacementAction(InvManager.SnapshotContext context, InvManager.SlotAssignment assignment, ItemStack targetStack) {
        InvManager.PlacementSource bestSource = null;
        for (int intValue33 = 0; intValue33 < 36; intValue33++) {
            if (intValue33 != assignment.hotbarSlot) {
                ItemStack sourceStack = context.snapshot.getSlot(intValue33);
                if (ItemSearchIndex.matches(assignment.storageId, sourceStack) && (intValue33 >= HOTBAR_SIZE || !this.isCorrectHotbarSlot(context, intValue33))) {
                    int resultingSize = this.getPickupAllResultingSize(context, intValue33, sourceStack);
                    if (resultingSize > sourceStack.field_77994_a) {
                        InvManager.PlacementSource candidate = new InvManager.PlacementSource(intValue33, ItemSearchIndex.getMatchQuality(assignment.storageId, sourceStack), intValue33 < HOTBAR_SIZE ? 1 : 0, resultingSize);
                        if (bestSource == null || candidate.isBetterThan(bestSource)) {
                            bestSource = candidate;
                        }
                    }
                }
            }
        }
        if (bestSource == null) {
            return null;
        } else {
            final int inventoryIndex4 = bestSource.inventoryIndex;
            final int targetHotbarSlot = assignment.hotbarSlot;
            final String storageId = assignment.storageId;
            boolean targetOccupiedByDifferentItem = targetStack != null && !ItemSearchIndex.matches(storageId, targetStack);
            List<InvManager.PlannedClick> steps = new ArrayList<>(targetOccupiedByDifferentItem ? 4 : 3);
            steps.add(new InvManager.PlannedClick(toContainerSlot(inventoryIndex4), 0, 0, new InvManager.StepValidator() {

                @Override
                public boolean isValid(InvManager.SnapshotContext current, InvManager module) {
                    return current.snapshot.carried == null && ItemSearchIndex.matches(storageId, current.snapshot.getSlot(inventoryIndex4)) && (inventoryIndex4 >= InvManager.HOTBAR_SIZE || !module.isCorrectHotbarSlot(current, inventoryIndex4));
                }
            }, new InvManager.StepHook() {

                @Override
                public void run(InvManager module) {
                    module.cursorRecoveryInventoryIndex = inventoryIndex4;
                }
            }, NO_OP));
            steps.add(new InvManager.PlannedClick(toContainerSlot(inventoryIndex4), 0, 6, new InvManager.StepValidator() {

                @Override
                public boolean isValid(InvManager.SnapshotContext current, InvManager module) {
                    return current.snapshot.carried != null && ItemSearchIndex.matches(storageId, current.snapshot.carried) && current.snapshot.getSlot(inventoryIndex4) == null && module.getPickupAllResultingSize(current, inventoryIndex4, current.snapshot.carried) > current.snapshot.carried.field_77994_a;
                }
            }, NO_OP, NO_OP));
            steps.add(new InvManager.PlannedClick(toContainerSlot(targetHotbarSlot), 0, 0, new InvManager.StepValidator() {

                @Override
                public boolean isValid(InvManager.SnapshotContext current, InvManager module) {
                    if (current.snapshot.carried != null && ItemSearchIndex.matches(storageId, current.snapshot.carried)) {
                        ItemStack currentTarget = current.snapshot.getSlot(targetHotbarSlot);
                        if (currentTarget == null) {
                            return true;
                        } else {
                            return !ItemSearchIndex.matches(storageId, currentTarget) ? true : InvManager.canStacksMerge(current.snapshot.carried, currentTarget) && InvManager.isPartialStack(currentTarget);
                        }
                    } else {
                        return false;
                    }
                }
            }, NO_OP, targetOccupiedByDifferentItem ? NO_OP : new InvManager.StepHook() {

                @Override
                public void run(InvManager module) {
                    module.cursorRecoveryInventoryIndex = -1;
                }
            }));
            if (targetOccupiedByDifferentItem) {
                steps.add(new InvManager.PlannedClick(toContainerSlot(inventoryIndex4), 0, 0, new InvManager.StepValidator() {

                    @Override
                    public boolean isValid(InvManager.SnapshotContext current, InvManager module) {
                        return current.snapshot.carried != null && InvManager.canDepositToSlot(current.snapshot, inventoryIndex4);
                    }
                }, NO_OP, new InvManager.StepHook() {

                    @Override
                    public void run(InvManager module) {
                        module.cursorRecoveryInventoryIndex = -1;
                    }
                }));
            }
            return new InvManager.PlannedAction(InvManager.PlanType.PICKUP_ALL_TO_TARGET, steps.size(), assignment.hotbarSlot, assignment.priorityIndex, bestSource.resultingSize, bestSource.resultingSize >= context.snapshot.getSlot(inventoryIndex4).func_77976_d(), 0, inventoryIndex4, steps);
        }
    }

    private InvManager.PlannedAction buildUnconfiguredStackAction(InvManager.SnapshotContext context) {
        InvManager.PlannedAction bestAction = null;
        for (int intValue34 = 0; intValue34 < 36; intValue34++) {
            ItemStack targetStack = context.snapshot.getSlot(intValue34);
            if (isPartialStack(targetStack) && !this.isManagedStack(targetStack)) {
                InvManager.PlannedAction pickupAllAction = this.buildUnconfiguredPickupAllAction(context, intValue34, targetStack);
                bestAction = pickBetterAction(bestAction, pickupAllAction);
            }
        }
        return bestAction;
    }

    private InvManager.PlannedAction buildUnconfiguredPickupAllAction(InvManager.SnapshotContext context, int intValue3, ItemStack targetStack) {
        int resultingSize = this.getUnconfiguredPickupAllResultingSize(context, intValue3, targetStack);
        if (resultingSize <= targetStack.field_77994_a) {
            return null;
        } else {
            List<InvManager.PlannedClick> steps = new ArrayList<>(3);
            steps.add(new InvManager.PlannedClick(toContainerSlot(intValue3), 0, 0, new InvManager.StepValidator() {

                @Override
                public boolean isValid(InvManager.SnapshotContext current, InvManager module) {
                    ItemStack currentTarget = current.snapshot.getSlot(intValue3);
                    return current.snapshot.carried == null && InvManager.isPartialStack(currentTarget) && !module.isManagedStack(currentTarget) && module.getUnconfiguredPickupAllResultingSize(current, intValue3, currentTarget) > currentTarget.field_77994_a;
                }
            }, new InvManager.StepHook() {

                @Override
                public void run(InvManager module) {
                    module.cursorRecoveryInventoryIndex = intValue3;
                }
            }, NO_OP));
            steps.add(new InvManager.PlannedClick(toContainerSlot(intValue3), 0, 6, new InvManager.StepValidator() {

                @Override
                public boolean isValid(InvManager.SnapshotContext current, InvManager module) {
                    return current.snapshot.carried != null && !module.isManagedStack(current.snapshot.carried) && current.snapshot.getSlot(intValue3) == null && module.getUnconfiguredPickupAllResultingSize(current, intValue3, current.snapshot.carried) > current.snapshot.carried.field_77994_a;
                }
            }, NO_OP, NO_OP));
            steps.add(new InvManager.PlannedClick(toContainerSlot(intValue3), 0, 0, new InvManager.StepValidator() {

                @Override
                public boolean isValid(InvManager.SnapshotContext current, InvManager module) {
                    return current.snapshot.carried != null && InvManager.canDepositToSlot(current.snapshot, intValue3);
                }
            }, NO_OP, new InvManager.StepHook() {

                @Override
                public void run(InvManager module) {
                    module.cursorRecoveryInventoryIndex = -1;
                }
            }));
            return new InvManager.PlannedAction(InvManager.PlanType.STACK_UNCONFIGURED_ITEMS, 3, intValue3, Integer.MAX_VALUE, resultingSize, resultingSize >= targetStack.func_77976_d(), 0, intValue3, steps);
        }
    }

    private InvManager.PlannedAction buildRecoveryAction(InvManager.SnapshotContext context, InvManager.PlanType type, boolean allowDrop) {
        InvManager.RecoveryPlanData recoveryPlan = this.simulateRecoveryPlan(context.snapshot, this.cursorRecoveryInventoryIndex, allowDrop);
        if (recoveryPlan == null) {
            return null;
        } else {
            List<InvManager.PlannedClick> steps = new ArrayList<>(recoveryPlan.depositIndices.size() + (recoveryPlan.willDrop ? 1 : 0));
            for (int i = 0; i < recoveryPlan.depositIndices.size(); i++) {
                final int intValue35 = recoveryPlan.depositIndices.get(i);
                boolean clearsCursor = !recoveryPlan.willDrop && i == recoveryPlan.depositIndices.size() - 1;
                steps.add(new InvManager.PlannedClick(toContainerSlot(intValue35), 0, 0, new InvManager.StepValidator() {

                    @Override
                    public boolean isValid(InvManager.SnapshotContext current, InvManager module) {
                        return current.snapshot.carried != null && InvManager.canDepositToSlot(current.snapshot, intValue35);
                    }
                }, new InvManager.StepHook() {

                    @Override
                    public void run(InvManager module) {
                        module.cursorRecoveryInventoryIndex = intValue35;
                    }
                }, clearsCursor ? new InvManager.StepHook() {

                    @Override
                    public void run(InvManager module) {
                        module.cursorRecoveryInventoryIndex = -1;
                    }
                } : NO_OP));
            }
            if (recoveryPlan.willDrop) {
                steps.add(new InvManager.PlannedClick(-999, 0, 0, new InvManager.StepValidator() {

                    @Override
                    public boolean isValid(InvManager.SnapshotContext current, InvManager module) {
                        return current.snapshot.carried != null;
                    }
                }, NO_OP, new InvManager.StepHook() {

                    @Override
                    public void run(InvManager module) {
                        module.cursorRecoveryInventoryIndex = -1;
                    }
                }));
            }
            return new InvManager.PlannedAction(type, steps.size(), Integer.MAX_VALUE, Integer.MAX_VALUE, 0, false, 0, recoveryPlan.depositIndices.isEmpty() ? Integer.MAX_VALUE : recoveryPlan.depositIndices.get(0), steps);
        }
    }

    private InvManager.MergePlanData buildMergePlanData(InvManager.SnapshotContext context, InvManager.SlotAssignment assignment, ItemStack targetStack) {
        int room = getRemainingRoom(targetStack);
        if (room <= 0) {
            return null;
        } else {
            List<InvManager.MergeSource> sources = this.collectMergeSources(context, assignment.hotbarSlot, targetStack);
            if (sources.isEmpty()) {
                return null;
            } else {
                InvManager.MergePath[] bestPaths = new InvManager.MergePath[room + 1];
                bestPaths[0] = new InvManager.MergePath(0, new ArrayList<>());
                for (InvManager.MergeSource source : sources) {
                    InvManager.MergePath[] nextPaths = new InvManager.MergePath[room + 1];
                    System.arraycopy(bestPaths, 0, nextPaths, 0, bestPaths.length);
                    for (int added = 0; added <= room; added++) {
                        InvManager.MergePath path = bestPaths[added];
                        if (path != null && added < room) {
                            int effectiveAdded = Math.min(room - added, source.stackSize);
                            if (effectiveAdded > 0) {
                                boolean returnsRemainder = source.type == InvManager.MergeSourceType.HOTBAR && source.stackSize > effectiveAdded;
                                int extraClicks = source.type == InvManager.MergeSourceType.MAIN_INVENTORY ? 1 : (returnsRemainder ? 3 : 2);
                                InvManager.MergeUse mergeUse = new InvManager.MergeUse(source, effectiveAdded, returnsRemainder);
                                InvManager.MergePath candidate = path.append(mergeUse, extraClicks);
                                int newAdded = added + effectiveAdded;
                                if (this.isBetterMergePath(candidate, nextPaths[newAdded])) {
                                    nextPaths[newAdded] = candidate;
                                }
                            }
                        }
                    }
                    bestPaths = nextPaths;
                }
                for (int intValue36 = room; intValue36 > 0; intValue36--) {
                    InvManager.MergePath bestPath = bestPaths[intValue36];
                    if (bestPath != null) {
                        return new InvManager.MergePlanData(targetStack.field_77994_a + intValue36, bestPath.cost, bestPath.uses, bestPath.uses.isEmpty() ? Integer.MAX_VALUE : bestPath.uses.get(0).source.inventoryIndex);
                    }
                }
                return null;
            }
        }
    }

    private boolean isBetterMergePath(InvManager.MergePath candidate, InvManager.MergePath existing) {
        if (candidate == null) {
            return false;
        } else if (existing == null) {
            return true;
        } else {
            int comparison = Integer.compare(candidate.cost, existing.cost);
            if (comparison != 0) {
                return comparison < 0;
            } else {
                comparison = Integer.compare(candidate.uses.size(), existing.uses.size());
                if (comparison != 0) {
                    return comparison < 0;
                } else {
                    for (int index = 0; index < candidate.uses.size() && index < existing.uses.size(); index++) {
                        InvManager.MergeUse candidateUse = candidate.uses.get(index);
                        InvManager.MergeUse existingUse = existing.uses.get(index);
                        comparison = Integer.compare(candidateUse.source.type.ordinal(), existingUse.source.type.ordinal());
                        if (comparison != 0) {
                            return comparison < 0;
                        }
                        comparison = Integer.compare(candidateUse.effectiveAdded, existingUse.effectiveAdded);
                        if (comparison != 0) {
                            return comparison > 0;
                        }
                        comparison = Integer.compare(candidateUse.source.inventoryIndex, existingUse.source.inventoryIndex);
                        if (comparison != 0) {
                            return comparison < 0;
                        }
                    }
                    return false;
                }
            }
        }
    }

    private List<InvManager.MergeSource> collectMergeSources(InvManager.SnapshotContext context, int targetHotbarSlot, ItemStack targetStack) {
        List<InvManager.MergeSource> sources = new ArrayList<>();
        for (int intValue37 = 0; intValue37 < 36; intValue37++) {
            if (intValue37 != targetHotbarSlot) {
                ItemStack sourceStack = context.snapshot.getSlot(intValue37);
                if (canStacksMerge(sourceStack, targetStack)) {
                    if (intValue37 < HOTBAR_SIZE) {
                        if (!this.isCorrectHotbarSlot(context, intValue37)) {
                            sources.add(new InvManager.MergeSource(intValue37, sourceStack.field_77994_a, InvManager.MergeSourceType.HOTBAR));
                        }
                    } else if (isQuickMoveMergeSafe(context.snapshot, targetHotbarSlot, sourceStack, targetStack)) {
                        sources.add(new InvManager.MergeSource(intValue37, sourceStack.field_77994_a, InvManager.MergeSourceType.MAIN_INVENTORY));
                    }
                }
            }
        }
        sources.sort(new Comparator<InvManager.MergeSource>() {

            public int compare(InvManager.MergeSource first, InvManager.MergeSource second) {
                int comparison = Integer.compare(first.type.ordinal(), second.type.ordinal());
                if (comparison != 0) {
                    return comparison;
                } else {
                    comparison = Integer.compare(second.stackSize, first.stackSize);
                    return comparison != 0 ? comparison : Integer.compare(first.inventoryIndex, second.inventoryIndex);
                }
            }
        });
        return sources;
    }

    private int getPickupAllResultingSize(InvManager.SnapshotContext context, int targetHotbarSlot, ItemStack targetStack) {
        if (!isPartialStack(targetStack)) {
            return 0;
        } else {
            int mergedSize = targetStack.field_77994_a;
            for (int intValue38 = 0; intValue38 < 36 && mergedSize < targetStack.func_77976_d(); intValue38++) {
                if (intValue38 != targetHotbarSlot) {
                    ItemStack sourceStack = context.snapshot.getSlot(intValue38);
                    if (canStacksMerge(sourceStack, targetStack)) {
                        if (intValue38 < HOTBAR_SIZE && this.isCorrectHotbarSlot(context, intValue38)) {
                            return 0;
                        }
                        mergedSize = Math.min(targetStack.func_77976_d(), mergedSize + sourceStack.field_77994_a);
                    }
                }
            }
            return mergedSize;
        }
    }

    private int getUnconfiguredPickupAllResultingSize(InvManager.SnapshotContext context, int intValue4, ItemStack targetStack) {
        if (isPartialStack(targetStack) && !this.isManagedStack(targetStack)) {
            int mergedSize = targetStack.field_77994_a;
            for (int intValue39 = 0; intValue39 < 36 && mergedSize < targetStack.func_77976_d(); intValue39++) {
                if (intValue39 != intValue4) {
                    ItemStack sourceStack = context.snapshot.getSlot(intValue39);
                    if (sourceStack != null && !this.isManagedStack(sourceStack) && isPartialStack(sourceStack) && canStacksMerge(sourceStack, targetStack)) {
                        mergedSize = Math.min(targetStack.func_77976_d(), mergedSize + sourceStack.field_77994_a);
                    }
                }
            }
            return mergedSize;
        } else {
            return 0;
        }
    }

    private void recoverCarriedStackImmediately(InvManager.InventorySnapshot snapshot, boolean allowDrop) {
        if (mc.field_71439_g.field_71070_bA instanceof ContainerPlayer && snapshot.carried != null) {
            int cursorRecoveryInventoryIndex2 = this.cursorRecoveryInventoryIndex;
            for (int remainingAttempts = 38; snapshot.carried != null && remainingAttempts-- > 0; snapshot = InvManager.InventorySnapshot.capture()) {
                int intValue40 = this.findRecoveryDepositIndex(snapshot, cursorRecoveryInventoryIndex2);
                if (intValue40 < 0) {
                    break;
                }
                this.cursorRecoveryInventoryIndex = intValue40;
                this.click(toContainerSlot(intValue40), 0, 0);
                cursorRecoveryInventoryIndex2 = -1;
            }
            if (snapshot.carried != null && allowDrop) {
                this.click(-999, 0, 0);
                snapshot = InvManager.InventorySnapshot.capture();
            }
            this.currentAction = null;
            if (snapshot.carried == null || allowDrop) {
                this.cursorRecoveryInventoryIndex = -1;
            }
            if (this.sessionState != InvManager.SessionState.COMPLETE_LATCHED) {
                this.sessionState = InvManager.SessionState.ACTIVE;
            }
        } else {
            this.currentAction = null;
            if (allowDrop) {
                this.cursorRecoveryInventoryIndex = -1;
            }
            if (this.sessionState != InvManager.SessionState.COMPLETE_LATCHED) {
                this.sessionState = InvManager.SessionState.ACTIVE;
            }
        }
    }

    private InvManager.RecoveryPlanData simulateRecoveryPlan(InvManager.InventorySnapshot snapshot, int intValue5, boolean allowDrop) {
        if (snapshot.carried == null) {
            return null;
        } else {
            InvManager.SimulatedInventory simulated = new InvManager.SimulatedInventory(snapshot);
            List<Integer> depositIndices = new ArrayList<>();
            int remainingAttempts = 37;
            for (int intValue41 = intValue5; simulated.getCarried() != null && remainingAttempts-- > 0; intValue41 = -1) {
                int intValue42 = this.findRecoveryDepositIndex(simulated, intValue41);
                if (intValue42 < 0) {
                    break;
                }
                depositIndices.add(intValue42);
                simulated.deposit(intValue42);
            }
            if (simulated.getCarried() != null && !allowDrop) {
                return null;
            } else {
                boolean willDrop = simulated.getCarried() != null;
                return depositIndices.isEmpty() && !willDrop ? null : new InvManager.RecoveryPlanData(depositIndices, willDrop);
            }
        }
    }

    private int findRecoveryDepositIndex(InvManager.RecoveryView view, int intValue6) {
        if (intValue6 >= 0 && canDepositToSlot(view, intValue6)) {
            return intValue6;
        } else {
            for (int intValue43 = HOTBAR_SIZE; intValue43 < 36; intValue43++) {
                if (canMergeIntoSlot(view, intValue43)) {
                    return intValue43;
                }
            }
            for (int intValue44 = 0; intValue44 < HOTBAR_SIZE; intValue44++) {
                if (canMergeIntoSlot(view, intValue44)) {
                    return intValue44;
                }
            }
            for (int intValue45 = HOTBAR_SIZE; intValue45 < 36; intValue45++) {
                if (view.getSlot(intValue45) == null) {
                    return intValue45;
                }
            }
            for (int intValue46 = 0; intValue46 < HOTBAR_SIZE; intValue46++) {
                if (view.getSlot(intValue46) == null) {
                    return intValue46;
                }
            }
            return -1;
        }
    }

    private InvManager.SlotAssignment[] resolveAssignments(InvManager.InventorySnapshot snapshot) {
        InvManager.SlotAssignment[] assignments = new InvManager.SlotAssignment[HOTBAR_SIZE];
        List<String> orderedItems = this.items.getItems();
        for (int intValue47 = 0; intValue47 < orderedItems.size(); intValue47++) {
            String storageId = orderedItems.get(intValue47);
            Integer assignedSlot = this.items.getAssignedSlot(storageId);
            if (assignedSlot != null) {
                int hotbarSlot = assignedSlot - 1;
                if (hotbarSlot >= 0 && hotbarSlot < HOTBAR_SIZE && assignments[hotbarSlot] == null && this.hasMatchingStack(snapshot, storageId)) {
                    assignments[hotbarSlot] = new InvManager.SlotAssignment(hotbarSlot, storageId, intValue47);
                }
            }
        }
        return assignments;
    }

    private boolean hasMatchingStack(InvManager.InventorySnapshot snapshot, String storageId) {
        for (int intValue48 = 0; intValue48 < 36; intValue48++) {
            if (ItemSearchIndex.matches(storageId, snapshot.getSlot(intValue48))) {
                return true;
            }
        }
        return false;
    }

    private boolean isCorrectHotbarSlot(InvManager.SnapshotContext context, int hotbarSlot) {
        if (hotbarSlot >= 0 && hotbarSlot < HOTBAR_SIZE) {
            InvManager.SlotAssignment assignment = context.assignments[hotbarSlot];
            return assignment != null && ItemSearchIndex.matches(assignment.storageId, context.snapshot.getSlot(hotbarSlot));
        } else {
            return false;
        }
    }

    private boolean isManagedStack(ItemStack stack) {
        return stack != null && this.items.matches(stack);
    }

    private boolean isAssignedTargetSatisfied(InvManager.SnapshotContext context, InvManager.SlotAssignment assignment, ItemStack targetStack) {
        if (!ItemSearchIndex.matches(assignment.storageId, targetStack)) {
            return false;
        } else {
            InvManager.PlacementSource betterSource = this.findBestPlacementSource(context, assignment, assignment.hotbarSlot);
            if (betterSource == null) {
                return true;
            } else {
                double targetQuality = ItemSearchIndex.getMatchQuality(assignment.storageId, targetStack);
                return betterSource.quality <= targetQuality + 1.0E-6;
            }
        }
    }

    private InvManager.PlacementSource findBestPlacementSource(InvManager.SnapshotContext context, InvManager.SlotAssignment assignment, int intValue7) {
        InvManager.PlacementSource bestSource = null;
        for (int intValue49 = 0; intValue49 < 36; intValue49++) {
            if (intValue49 != intValue7) {
                ItemStack sourceStack = context.snapshot.getSlot(intValue49);
                if (ItemSearchIndex.matches(assignment.storageId, sourceStack) && (intValue49 >= HOTBAR_SIZE || !this.isCorrectHotbarSlot(context, intValue49))) {
                    InvManager.PlacementSource candidate = new InvManager.PlacementSource(intValue49, ItemSearchIndex.getMatchQuality(assignment.storageId, sourceStack), intValue49 < HOTBAR_SIZE ? 1 : 0, sourceStack != null ? sourceStack.field_77994_a : 0);
                    if (bestSource == null || candidate.isBetterThan(bestSource)) {
                        bestSource = candidate;
                    }
                }
            }
        }
        return bestSource;
    }

    private static InvManager.PlannedAction pickBetterAction(InvManager.PlannedAction currentBest, InvManager.PlannedAction candidate) {
        if (candidate == null) {
            return currentBest;
        } else {
            return currentBest != null && ACTION_COMPARATOR.compare(candidate, currentBest) >= 0 ? currentBest : candidate;
        }
    }

    private void click(int slotId, int button, int mode) {
        if (this.preUpdatePacketWindow) {
            this.inventoryActionPerformed = true;
            this.sendingInventoryClick = true;
            try {
                mc.field_71442_b.func_78753_a(mc.field_71439_g.field_71070_bA.field_75152_c, slotId, button, mode, mc.field_71439_g);
            } finally {
                this.sendingInventoryClick = false;
            }
        }
    }

    private boolean isManagedInventoryOpen() {
        return Utils.nullCheck() && mc.field_71462_r instanceof GuiInventory && mc.field_71439_g.field_71070_bA instanceof ContainerPlayer && Utils.inInventory();
    }

    private static boolean isQuickMoveMergeSafe(InvManager.InventorySnapshot snapshot, int targetHotbarSlot, ItemStack sourceStack, ItemStack targetStack) {
        if (canStacksMerge(sourceStack, targetStack) && isPartialStack(targetStack)) {
            for (int hotbarSlot = 0; hotbarSlot < HOTBAR_SIZE; hotbarSlot++) {
                if (hotbarSlot != targetHotbarSlot) {
                    ItemStack hotbarStack = snapshot.getSlot(hotbarSlot);
                    if (hotbarStack != null && canStacksMerge(hotbarStack, sourceStack) && isPartialStack(hotbarStack) && hotbarSlot < targetHotbarSlot) {
                        return false;
                    }
                }
            }
            return true;
        } else {
            return false;
        }
    }

    private static boolean canDepositToSlot(InvManager.RecoveryView view, int intValue8) {
        ItemStack slotStack = view.getSlot(intValue8);
        return slotStack == null || canMergeIntoSlot(view, intValue8);
    }

    private static boolean canMergeIntoSlot(InvManager.RecoveryView view, int intValue9) {
        ItemStack slotStack = view.getSlot(intValue9);
        ItemStack carried = view.getCarried();
        return canStacksMerge(slotStack, carried) && isPartialStack(slotStack);
    }

    private static boolean canStacksMerge(ItemStack first, ItemStack second) {
        if (first == null || second == null || first.func_77973_b() != second.func_77973_b()) {
            return false;
        } else {
            return first.func_77981_g() && first.func_77960_j() != second.func_77960_j() ? false : ItemStack.func_77970_a(first, second);
        }
    }

    private static boolean isPartialStack(ItemStack stack) {
        return stack != null && stack.func_77985_e() && stack.field_77994_a < stack.func_77976_d();
    }

    private static int getRemainingRoom(ItemStack stack) {
        return stack == null ? 0 : Math.max(0, stack.func_77976_d() - stack.field_77994_a);
    }

    private static int toContainerSlot(int intValue10) {
        if (intValue10 >= 0 && intValue10 < 36) {
            return intValue10 < HOTBAR_SIZE ? intValue10 + 36 : intValue10;
        } else {
            return -1;
        }
    }

    private static enum CurrentArmor {

        BOOTS(0, 8), LEGGINGS(1, 7), CHESTPLATE(2, 6), HELMET(3, 5);

        final int slot;

        final int invSlot;

        int defenceLevel;

        private CurrentArmor(int slot, int invSlot) {
            this.slot = slot;
            this.invSlot = invSlot;
        }

        ItemStack getItemStack() {
            return InvManager.mc.field_71439_g.field_71071_by.func_70440_f(this.slot);
        }
    }

    private final class InventoryData {

        final IInventory inventory;

        final int size;

        int filled;

        int emptyWithoutHotbar;

        final double[] swordData = new double[] { -1.0, -1.0 };

        final int[][] armorData = new int[][] { { -1, -1, -1, -1 }, { -1, -1, -1, -1 } };

        double bestSwordDamage = -1.0;

        double bestPickaxe = -1.0;

        double bestAxe = -1.0;

        double bestShovel = -1.0;

        final HashSet<Integer> uniqueSingles = new HashSet<>();

        InventoryData(IInventory inventory, boolean playerInventory, boolean armorOnly) {
            this.inventory = inventory;
            this.size = playerInventory ? inventory.func_70302_i_() - 4 : inventory.func_70302_i_();
            for (int i = 0; i < this.size; i++) {
                ItemStack itemStack = inventory.func_70301_a(i);
                if (itemStack != null) {
                    this.filled++;
                } else if (i >= InvManager.HOTBAR_SIZE) {
                    this.emptyWithoutHotbar++;
                }
                if (itemStack != null) {
                    if (InvManager.this.isArmor(itemStack)) {
                        ItemArmor armor = (ItemArmor) itemStack.func_77973_b();
                        int slot = 3 - armor.field_77881_a;
                        int defenceLevel = InvManager.this.getDefenceLevel(itemStack);
                        if (defenceLevel > this.armorData[1][slot]) {
                            this.armorData[1][slot] = defenceLevel;
                            this.armorData[0][slot] = i;
                        }
                    }
                    if (!armorOnly) {
                        Item item = itemStack.func_77973_b();
                        if (InvManager.this.isSword(itemStack)) {
                            double damageLevel = Utils.getDamageLevel(itemStack);
                            if (damageLevel > this.bestSwordDamage) {
                                this.bestSwordDamage = damageLevel;
                            }
                            if (damageLevel > this.swordData[1]) {
                                this.swordData[1] = damageLevel;
                                this.swordData[0] = i;
                            }
                        } else if (item instanceof ItemPickaxe) {
                            double efficiency = Utils.getEfficiency(itemStack, Blocks.field_150348_b);
                            if (efficiency > this.bestPickaxe) {
                                this.bestPickaxe = efficiency;
                            }
                        } else if (item instanceof ItemAxe) {
                            double efficiency = Utils.getEfficiency(itemStack, Blocks.field_150364_r);
                            if (efficiency > this.bestAxe) {
                                this.bestAxe = efficiency;
                            }
                        } else if (item instanceof ItemSpade) {
                            double efficiency = Utils.getEfficiency(itemStack, Blocks.field_150346_d);
                            if (efficiency > this.bestShovel) {
                                this.bestShovel = efficiency;
                            }
                        } else if (!(item instanceof ItemBlock) && itemStack.func_77976_d() == 1) {
                            this.uniqueSingles.add(Item.func_150891_b(item));
                        }
                    }
                }
            }
        }

        void compareAndRemove(InvManager.InventoryData data) {
            if (data.bestSwordDamage > this.swordData[1]) {
                this.swordData[1] = -1.0;
            }
            for (int i = 0; i < this.armorData[0].length; i++) {
                if (data.armorData[1][i] > this.armorData[1][i]) {
                    this.armorData[1][i] = -1;
                }
            }
        }
    }

    private static final class InventorySnapshot implements InvManager.RecoveryView {

        static final int INVENTORY_SIZE = 36;

        final ItemStack[] slots = new ItemStack[36];

        final ItemStack carried;

        private InventorySnapshot(ItemStack[] slots, ItemStack carried) {
            System.arraycopy(slots, 0, this.slots, 0, slots.length);
            this.carried = carried;
        }

        static InvManager.InventorySnapshot capture() {
            ItemStack[] slots = new ItemStack[36];
            for (int intValue50 = 0; intValue50 < 36; intValue50++) {
                ItemStack stack = InvManager.mc.field_71439_g.field_71071_by.func_70301_a(intValue50);
                slots[intValue50] = stack != null ? stack.func_77946_l() : null;
            }
            ItemStack carried = InvManager.mc.field_71439_g.field_71071_by.func_70445_o();
            return new InvManager.InventorySnapshot(slots, carried != null ? carried.func_77946_l() : null);
        }

        @Override
        public ItemStack getSlot(int intValue11) {
            return intValue11 >= 0 && intValue11 < 36 ? this.slots[intValue11] : null;
        }

        @Override
        public ItemStack getCarried() {
            return this.carried;
        }
    }

    private static final class MergePath {

        final int cost;

        final List<InvManager.MergeUse> uses;

        MergePath(int cost, List<InvManager.MergeUse> uses) {
            this.cost = cost;
            this.uses = uses;
        }

        InvManager.MergePath append(InvManager.MergeUse mergeUse, int extraCost) {
            List<InvManager.MergeUse> nextUses = new ArrayList<>(this.uses.size() + 1);
            nextUses.addAll(this.uses);
            nextUses.add(mergeUse);
            return new InvManager.MergePath(this.cost + extraCost, nextUses);
        }
    }

    private static final class MergePlanData {

        final int resultingSize;

        final int clickCount;

        final List<InvManager.MergeUse> uses;

        final int primarySourceIndex;

        MergePlanData(int resultingSize, int clickCount, List<InvManager.MergeUse> uses, int intValue12) {
            this.resultingSize = resultingSize;
            this.clickCount = clickCount;
            this.uses = uses;
            this.primarySourceIndex = intValue12;
        }
    }

    private static final class MergeSource {

        final int inventoryIndex;

        final int stackSize;

        final InvManager.MergeSourceType type;

        MergeSource(int intValue13, int stackSize, InvManager.MergeSourceType type) {
            this.inventoryIndex = intValue13;
            this.stackSize = stackSize;
            this.type = type;
        }
    }

    private static enum MergeSourceType {

        MAIN_INVENTORY, HOTBAR
    }

    private static final class MergeUse {

        final InvManager.MergeSource source;

        final int effectiveAdded;

        final boolean returnsRemainder;

        MergeUse(InvManager.MergeSource source, int effectiveAdded, boolean returnsRemainder) {
            this.source = source;
            this.effectiveAdded = effectiveAdded;
            this.returnsRemainder = returnsRemainder;
        }
    }

    private static final class PlacementSource {

        final int inventoryIndex;

        final double quality;

        final int sourcePreference;

        final int resultingSize;

        PlacementSource(int intValue14, double quality, int sourcePreference, int resultingSize) {
            this.inventoryIndex = intValue14;
            this.quality = quality;
            this.sourcePreference = sourcePreference;
            this.resultingSize = resultingSize;
        }

        boolean isBetterThan(InvManager.PlacementSource other) {
            int comparison = Double.compare(this.quality, other.quality);
            if (Math.abs(this.quality - other.quality) > 1.0E-6) {
                return comparison > 0;
            } else {
                comparison = Integer.compare(this.sourcePreference, other.sourcePreference);
                if (comparison != 0) {
                    return comparison < 0;
                } else {
                    comparison = Integer.compare(this.resultingSize, other.resultingSize);
                    return comparison != 0 ? comparison > 0 : this.inventoryIndex < other.inventoryIndex;
                }
            }
        }
    }

    private static enum PlanType {

        PLACE_TO_HOTBAR,
        MERGE_TO_TARGET,
        PICKUP_ALL_TO_TARGET,
        STACK_UNCONFIGURED_ITEMS,
        RECOVER_CURSOR,
        PRE_CLOSE_RECOVERY
    }

    private static final class PlannedAction {

        final InvManager.PlanType type;

        final int clickCount;

        final int targetHotbarSlot;

        final int priorityIndex;

        final int resultingSize;

        final boolean completesTarget;

        final int displacesCorrectHotbar;

        final int primarySourceIndex;

        final List<InvManager.PlannedClick> steps;

        int nextStepIndex;

        PlannedAction(InvManager.PlanType type, int clickCount, int targetHotbarSlot, int intValue15, int resultingSize, boolean completesTarget, int displacesCorrectHotbar, int intValue16, List<InvManager.PlannedClick> steps) {
            this.type = type;
            this.clickCount = clickCount;
            this.targetHotbarSlot = targetHotbarSlot;
            this.priorityIndex = intValue15;
            this.resultingSize = resultingSize;
            this.completesTarget = completesTarget;
            this.displacesCorrectHotbar = displacesCorrectHotbar;
            this.primarySourceIndex = intValue16;
            this.steps = steps;
        }

        InvManager.PlannedClick peek() {
            return this.nextStepIndex < this.steps.size() ? this.steps.get(this.nextStepIndex) : null;
        }

        void advance() {
            this.nextStepIndex++;
        }

        boolean isComplete() {
            return this.nextStepIndex >= this.steps.size();
        }
    }

    private static final class PlannedClick {

        final int slotId;

        final int button;

        final int mode;

        final InvManager.StepValidator validator;

        final InvManager.StepHook beforeExecute;

        final InvManager.StepHook afterExecute;

        PlannedClick(int slotId, int button, int mode, InvManager.StepValidator validator, InvManager.StepHook beforeExecute, InvManager.StepHook afterExecute) {
            this.slotId = slotId;
            this.button = button;
            this.mode = mode;
            this.validator = validator;
            this.beforeExecute = beforeExecute;
            this.afterExecute = afterExecute;
        }
    }

    private static final class RecoveryPlanData {

        final List<Integer> depositIndices;

        final boolean willDrop;

        RecoveryPlanData(List<Integer> depositIndices, boolean willDrop) {
            this.depositIndices = depositIndices;
            this.willDrop = willDrop;
        }
    }

    private interface RecoveryView {

        ItemStack getSlot(int intValue);

        ItemStack getCarried();
    }

    private static enum SessionState {

        ACTIVE, EXECUTING, COMPLETE_LATCHED
    }

    private static final class SimulatedInventory implements InvManager.RecoveryView {

        final ItemStack[] slots = new ItemStack[36];

        ItemStack carried;

        SimulatedInventory(InvManager.InventorySnapshot snapshot) {
            for (int intValue51 = 0; intValue51 < 36; intValue51++) {
                ItemStack stack = snapshot.getSlot(intValue51);
                this.slots[intValue51] = stack != null ? stack.func_77946_l() : null;
            }
            this.carried = snapshot.carried != null ? snapshot.carried.func_77946_l() : null;
        }

        void deposit(int intValue17) {
            if (this.carried != null) {
                ItemStack slotStack = this.slots[intValue17];
                if (slotStack == null) {
                    this.slots[intValue17] = this.carried.func_77946_l();
                    this.carried = null;
                } else if (InvManager.canStacksMerge(slotStack, this.carried)) {
                    int moved = Math.min(InvManager.getRemainingRoom(slotStack), this.carried.field_77994_a);
                    slotStack.field_77994_a += moved;
                    this.carried.field_77994_a -= moved;
                    if (this.carried.field_77994_a <= 0) {
                        this.carried = null;
                    }
                }
            }
        }

        @Override
        public ItemStack getSlot(int intValue18) {
            return intValue18 >= 0 && intValue18 < this.slots.length ? this.slots[intValue18] : null;
        }

        @Override
        public ItemStack getCarried() {
            return this.carried;
        }
    }

    private static final class SlotAssignment {

        final int hotbarSlot;

        final String storageId;

        final int priorityIndex;

        SlotAssignment(int hotbarSlot, String storageId, int intValue19) {
            this.hotbarSlot = hotbarSlot;
            this.storageId = storageId;
            this.priorityIndex = intValue19;
        }
    }

    private static final class SnapshotContext {

        final InvManager.InventorySnapshot snapshot;

        final InvManager.SlotAssignment[] assignments;

        private SnapshotContext(InvManager.InventorySnapshot snapshot, InvManager.SlotAssignment[] assignments) {
            this.snapshot = snapshot;
            this.assignments = assignments;
        }

        static InvManager.SnapshotContext create(InvManager.InventorySnapshot snapshot, InvManager.SlotAssignment[] assignments) {
            return new InvManager.SnapshotContext(snapshot, assignments);
        }
    }

    private interface StepHook {

        void run(InvManager invManager);
    }

    private interface StepValidator {

        boolean isValid(InvManager.SnapshotContext snapshotContext, InvManager invManager2);
    }
}
