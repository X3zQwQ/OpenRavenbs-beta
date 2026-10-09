package keystrokesmod.module.impl.player;

import keystrokesmod.event.PrePlayerInteractEvent;
import keystrokesmod.event.ReceivePacketEvent;
import keystrokesmod.event.SendPacketEvent;
import keystrokesmod.mixin.impl.accessor.IAccessorPlayerControllerMP;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.BlockListSetting;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.BlockUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.block.Block;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.server.S2FPacketSetSlot;
import net.minecraft.network.play.server.S30PacketWindowItems;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class AutoSwap extends Module {

    private static final String[] ALLOWED_TYPES = new String[] { "Exact", "Variants", "Full", "Any" };

    private static final int EXACT = 0;

    private static final int VARIANTS = 1;

    private static final int FULL = 2;

    private static final int ANY = 3;

    private final SliderSetting allowedTypes;

    private final ButtonSetting swapOnRmb;

    private final ButtonSetting syncStackSize;

    private final ButtonSetting useBlockWhitelist;

    private final BlockListSetting blockWhitelist;

    private ItemStack trackedStack;

    private volatile int lastPlaceSlot = -1;

    private int lastSwapSlot = -1;

    private volatile int serverStackSize = -1;

    private long lastSwapTime;

    public AutoSwap() {
        super("Auto Swap", Module.category.player);
        this.liteModule = true;
        this.registerSetting(this.allowedTypes = new SliderSetting("Allowed types", 0, ALLOWED_TYPES));
        this.registerSetting(this.swapOnRmb = new ButtonSetting("Swap on RMB", true));
        this.registerSetting(this.syncStackSize = new ButtonSetting("Sync stack size", true));
        this.registerSetting(this.useBlockWhitelist = new ButtonSetting("Use block whitelist", false));
        this.registerSetting(this.blockWhitelist = new BlockListSetting("Whitelisted blocks", "Blocks", "Blocks.Block whitelist", "Blocks.Whitelisted blocks"));
        this.blockWhitelist.visible = false;
        this.closetModule = true;
    }

    @Override
    public void guiUpdate() {
        this.blockWhitelist.setVisible(this.useBlockWhitelist.isToggled(), this);
    }

    @Override
    public void onEnable() {
        this.resetState();
    }

    @Override
    public void onDisable() {
        this.resetState();
    }

    @SubscribeEvent
    public void onSendPacket(SendPacketEvent e) {
        if (Utils.nullCheck() && e.getPacket() instanceof C08PacketPlayerBlockPlacement) {
            C08PacketPlayerBlockPlacement packet = (C08PacketPlayerBlockPlacement) e.getPacket();
            if (packet.func_149568_f() != 255) {
                ItemStack stack = packet.func_149574_g();
                if (stack != null && stack.func_77973_b() instanceof ItemBlock) {
                    int currentSlot = mc.field_71439_g.field_71071_by.field_70461_c;
                    boolean continuingStack = this.lastPlaceSlot == currentSlot && this.isSameStack(stack, this.trackedStack);
                    if (!continuingStack) {
                        this.serverStackSize = -1;
                    }
                    this.trackedStack = stack.func_77946_l();
                    this.trackedStack.field_77994_a = 1;
                    this.lastPlaceSlot = currentSlot;
                }
            }
        }
    }

    @SubscribeEvent
    public void onReceivePacket(ReceivePacketEvent e) {
        int hotbarSlot = this.lastPlaceSlot;
        if (hotbarSlot != -1) {
            int inventorySlot = 36 + hotbarSlot;
            if (e.getPacket() instanceof S2FPacketSetSlot) {
                S2FPacketSetSlot packet = (S2FPacketSetSlot) e.getPacket();
                if (packet.func_149175_c() == 0 && packet.func_149173_d() == inventorySlot) {
                    this.updateServerStackSize(packet.func_149174_e());
                }
            } else if (e.getPacket() instanceof S30PacketWindowItems) {
                S30PacketWindowItems packet = (S30PacketWindowItems) e.getPacket();
                ItemStack[] stacks = packet.func_148910_d();
                if (packet.func_148911_c() == 0 && inventorySlot < stacks.length) {
                    this.updateServerStackSize(stacks[inventorySlot]);
                }
            }
        }
    }

    @SubscribeEvent
    public void onPrePlayerInteract(PrePlayerInteractEvent e) {
        this.trySwap();
    }

    private void trySwap() {
        if (!Utils.nullCheck()) {
            this.resetState();
        } else if (mc.field_71415_G && mc.field_71462_r == null && (!this.swapOnRmb.isToggled() || Utils.isBindDown(mc.field_71474_y.field_74313_G))) {
            if (this.trackedStack != null && this.lastPlaceSlot != -1 && mc.field_71439_g.field_71071_by.field_70461_c == this.lastPlaceSlot) {
                ItemStack held = mc.field_71439_g.func_70694_bm();
                if (held == null || this.isSameStack(held, this.trackedStack)) {
                    boolean clientStackEmpty = held == null || held.field_77994_a <= 0;
                    boolean serverStackEmpty = this.syncStackSize.isToggled() && !mc.field_71442_b.func_78758_h() && this.serverStackSize == 0;
                    if (clientStackEmpty || serverStackEmpty) {
                        if (this.isWhitelistedBlock(this.trackedStack)) {
                            long now = System.currentTimeMillis();
                            for (int slot = 8; slot >= 0; slot--) {
                                if (slot != this.lastPlaceSlot && (slot != this.lastSwapSlot || now - this.lastSwapTime >= 300L)) {
                                    ItemStack candidate = mc.field_71439_g.field_71071_by.func_70301_a(slot);
                                    if (this.matchesTrackedStack(candidate)) {
                                        this.swapToSlot(slot);
                                        this.lastSwapSlot = slot;
                                        this.lastSwapTime = now;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean isWhitelistedBlock(ItemStack stack) {
        if (!this.useBlockWhitelist.isToggled()) {
            return true;
        } else if (stack != null && stack.func_77973_b() instanceof ItemBlock) {
            Block block = ((ItemBlock) stack.func_77973_b()).func_179223_d();
            Object registryName = Block.field_149771_c.func_177774_c(block);
            if (block != null && registryName != null) {
                String registryId = registryName.toString();
                int meta = stack.func_77960_j();
                String storageId = meta != 0 ? registryId + ":" + meta : registryId;
                return this.blockWhitelist.contains(storageId) || this.blockWhitelist.contains(registryId);
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    private boolean isSameStack(ItemStack first, ItemStack second) {
        return first != null && second != null && first.func_77973_b() == second.func_77973_b() && first.func_77960_j() == second.func_77960_j() && ItemStack.func_77970_a(first, second);
    }

    private void updateServerStackSize(ItemStack stack) {
        this.serverStackSize = stack == null ? 0 : stack.field_77994_a;
    }

    private boolean matchesTrackedStack(ItemStack stack) {
        if (this.trackedStack != null && stack != null && stack.func_77973_b() instanceof ItemBlock) {
            int allowedType = (int) this.allowedTypes.getInput();
            if (allowedType != 0 && allowedType != 1) {
                Block block = ((ItemBlock) stack.func_77973_b()).func_179223_d();
                if (block == null) {
                    return false;
                } else {
                    return allowedType == 2 ? block.func_149730_j() : allowedType == 3 && !BlockUtils.isInteractable(block);
                }
            } else if (stack.func_77973_b() != this.trackedStack.func_77973_b()) {
                return false;
            } else {
                return allowedType == 0 && stack.func_77960_j() != this.trackedStack.func_77960_j() ? false : ItemStack.func_77970_a(stack, this.trackedStack);
            }
        } else {
            return false;
        }
    }

    private void swapToSlot(int slot) {
        if (slot != -1 && slot != mc.field_71439_g.field_71071_by.field_70461_c) {
            mc.field_71439_g.field_71071_by.field_70461_c = slot;
            ((IAccessorPlayerControllerMP) mc.field_71442_b).callSyncCurrentPlayItem();
        }
    }

    private void resetState() {
        this.trackedStack = null;
        this.lastPlaceSlot = -1;
        this.lastSwapSlot = -1;
        this.serverStackSize = -1;
        this.lastSwapTime = 0L;
    }
}
