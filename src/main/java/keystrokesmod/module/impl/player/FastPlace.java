package keystrokesmod.module.impl.player;

import java.io.IOException;
import keystrokesmod.event.RightClickDelayTickEvent;
import keystrokesmod.event.SendPacketEvent;
import keystrokesmod.mixin.impl.accessor.IAccessorMinecraft;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.setting.impl.BlockListSetting;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.ItemListSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.RenderUtils;
import keystrokesmod.utility.Timer;
import keystrokesmod.utility.Utils;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.config.GuiButtonExt;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.RenderTickEvent;

public class FastPlace extends Module {

    public SliderSetting tickDelay;

    public SliderSetting activationTime;

    public ButtonSetting blocksOnly;

    public ButtonSetting pitchCheck;

    public ButtonSetting showBlockCount;

    public ButtonSetting editPosition;

    public ButtonSetting ignoredHeldItemsToggle;

    public ItemListSetting ignoredHeldItems;

    public ButtonSetting blockBlacklistToggle;

    public BlockListSetting blockBlacklist;

    private float posX = Float.NaN;

    private float posY = Float.NaN;

    private float relativePosX = Float.NaN;

    private float relativePosY = Float.NaN;

    private long rightClickStartTime;

    private Timer fadeTimer;

    private Timer fadeInTimer;

    private float previousAlpha;

    public FastPlace() {
        super("Fast Place", Module.category.player);
        this.liteModule = true;
        this.registerSetting(this.tickDelay = new SliderSetting("Delay", " tick", 1.0, 0.0, 3.0, 1.0));
        this.registerSetting(this.activationTime = new SliderSetting("Minimum hold time", "ms", 0.0, 0.0, 100.0, 5.0));
        this.registerSetting(this.blocksOnly = new ButtonSetting("Blocks only", true));
        this.registerSetting(this.pitchCheck = new ButtonSetting("Pitch check", false));
        this.registerSetting(this.showBlockCount = new ButtonSetting("Show block count", false));
        this.registerSetting(this.editPosition = new ButtonSetting("Edit position", () -> mc.func_147108_a(new FastPlace.EditScreen())));
        this.registerSetting(this.ignoredHeldItemsToggle = new ButtonSetting("Held item blacklist", false, "Ignore held items", "Restrict held items", "Allow while holding"));
        this.registerSetting(this.ignoredHeldItems = new ItemListSetting("Held items", "Items"));
        this.registerSetting(this.blockBlacklistToggle = new ButtonSetting("Block blacklist", false, "Blocks.Block blacklist"));
        this.registerSetting(this.blockBlacklist = new BlockListSetting("Blacklisted blocks", "Block blacklist", "Blocks.Block blacklist", "Blocks.Blacklisted blocks"));
        this.closetModule = true;
    }

    @Override
    public void onDisable() {
        this.rightClickStartTime = 0L;
    }

    @Override
    public void guiUpdate() {
        this.editPosition.setVisible(this.showBlockCount.isToggled(), this);
        this.ignoredHeldItems.setVisible(this.ignoredHeldItemsToggle.isToggled(), this);
        this.blockBlacklist.setVisible(this.blockBlacklistToggle.isToggled(), this);
    }

    @SubscribeEvent
    public void onRightClickDelayTick(RightClickDelayTickEvent e) {
        if (!Utils.nullCheck() || !mc.field_71415_G) {
            this.rightClickStartTime = 0L;
        } else if (!this.isRightClickActive()) {
            this.rightClickStartTime = 0L;
        } else {
            long now = System.currentTimeMillis();
            if (this.rightClickStartTime == 0L) {
                this.rightClickStartTime = now;
            }
            if (this.canFastPlace(now, true)) {
                int delay = (int) this.tickDelay.getInput();
                if (delay == 0) {
                    ((IAccessorMinecraft) mc).setRightClickDelayTimer(0);
                } else {
                    if (delay == 4) {
                        return;
                    }
                    if (((IAccessorMinecraft) mc).getRightClickDelayTimer() > delay) {
                        ((IAccessorMinecraft) mc).setRightClickDelayTimer(delay);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onSendPacket(SendPacketEvent e) {
        if (Utils.nullCheck() && e.getPacket() instanceof C08PacketPlayerBlockPlacement) {
            C08PacketPlayerBlockPlacement packet = (C08PacketPlayerBlockPlacement) e.getPacket();
            if (packet.func_149568_f() == 255) {
                ItemStack packetStack = packet.func_149574_g();
                if (packetStack != null && packetStack.func_77973_b() instanceof ItemBlock) {
                    if (this.canFastPlace(System.currentTimeMillis(), true)) {
                        if (Math.random() < 0.7) {
                            e.setCanceled(true);
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onRenderTick(RenderTickEvent ev) {
        if (ev.phase == Phase.END) {
            if (Utils.nullCheck()) {
                if (mc.field_71462_r != null) {
                    this.fadeTimer = null;
                    this.fadeInTimer = null;
                    this.previousAlpha = 0.0F;
                } else {
                    boolean fastPlaceEnabled = ModuleManager.fastPlace != null && ModuleManager.fastPlace.isEnabled();
                    boolean showBlockCountToggled = fastPlaceEnabled && ModuleManager.fastPlace.showBlockCount.isToggled();
                    ItemStack heldItem = mc.field_71439_g.func_70694_bm();
                    boolean holdingPlaceableBlock = heldItem != null && heldItem.func_77973_b() instanceof ItemBlock && Utils.canBePlaced((ItemBlock) heldItem.func_77973_b());
                    boolean shouldShow = showBlockCountToggled && mc.field_71462_r == null && holdingPlaceableBlock && this.canFastPlace(System.currentTimeMillis(), false);
                    if (shouldShow) {
                        if (this.fadeTimer != null) {
                            this.fadeTimer = null;
                            (this.fadeInTimer = new Timer(150.0F)).start();
                        } else if (this.fadeInTimer == null && this.previousAlpha < 255.0F) {
                            (this.fadeInTimer = new Timer(150.0F)).start();
                        }
                        float alpha;
                        if (this.fadeInTimer != null) {
                            alpha = this.fadeInTimer.getValueFloat(10.0F, 255.0F, 1);
                            if (alpha >= 255.0F) {
                                alpha = 255.0F;
                                this.fadeInTimer = null;
                            }
                        } else {
                            alpha = 255.0F;
                        }
                        this.previousAlpha = alpha;
                        this.renderBlockCount((int) alpha);
                    } else {
                        if (this.previousAlpha <= 10.0F) {
                            this.fadeTimer = null;
                            this.fadeInTimer = null;
                            this.previousAlpha = 0.0F;
                            return;
                        }
                        if (this.fadeTimer == null) {
                            (this.fadeTimer = new Timer(150.0F)).start();
                            this.fadeInTimer = null;
                        }
                        float alpha = 255.0F - this.fadeTimer.getValueInt(0, 255, 1);
                        if (alpha < 0.0F) {
                            alpha = 0.0F;
                        }
                        this.previousAlpha = alpha;
                        if (alpha > 10.0F) {
                            this.renderBlockCount((int) alpha);
                        }
                    }
                }
            }
        }
    }

    public float getPosX() {
        this.syncPosition();
        return this.posX;
    }

    public float getPosY() {
        this.syncPosition();
        return this.posY;
    }

    public float getRelativePosX() {
        this.syncPosition();
        return this.relativePosX;
    }

    public float getRelativePosY() {
        this.syncPosition();
        return this.relativePosY;
    }

    public void setRelativePosition(float normalizedX, float normalizedY) {
        this.relativePosX = normalizedX;
        this.relativePosY = normalizedY;
        this.syncPosition();
    }

    public void setAbsolutePosition(float absoluteX, float absoluteY) {
        this.setAbsolutePosition(absoluteX, absoluteY, new ScaledResolution(mc));
    }

    public void setAbsolutePosition(float absoluteX, float absoluteY, ScaledResolution resolution) {
        this.posX = absoluteX;
        this.posY = absoluteY;
        int w = Math.max(1, resolution.func_78326_a());
        int h = Math.max(1, resolution.func_78328_b());
        this.relativePosX = absoluteX / w;
        this.relativePosY = absoluteY / h;
    }

    public void resetPosition() {
        this.relativePosX = Float.NaN;
        this.relativePosY = Float.NaN;
        this.posX = Float.NaN;
        this.posY = Float.NaN;
        this.syncPosition();
    }

    private void syncPosition() {
        this.syncPosition(new ScaledResolution(mc));
    }

    private void syncPosition(ScaledResolution resolution) {
        int w = Math.max(1, resolution.func_78326_a());
        int h = Math.max(1, resolution.func_78328_b());
        if (Float.isNaN(this.relativePosX) || Float.isNaN(this.relativePosY)) {
            if (Float.isNaN(this.posX) || Float.isNaN(this.posY)) {
                this.posX = w / 2.0F + 9.0F;
                this.posY = h / 2.0F - mc.field_71466_p.field_78288_b / 2.0F + 1.0F;
            }
            this.relativePosX = this.posX / w;
            this.relativePosY = this.posY / h;
        }
        this.posX = this.relativePosX * w;
        this.posY = this.relativePosY * h;
    }

    private void renderBlockCount(int alpha) {
        this.renderBlockCount(alpha, false);
    }

    private void renderBlockCount(int alpha, boolean editing) {
        int blocks = this.getTotalBlocks();
        if (editing && blocks == 0) {
            blocks = 64;
        }
        String color = "§";
        if (blocks <= 5) {
            color = color + "c";
        } else if (blocks <= 15) {
            color = color + "6";
        } else if (blocks <= 25) {
            color = color + "e";
        } else {
            color = "";
        }
        int colorAlpha = Utils.mergeAlpha(-1, alpha);
        ScaledResolution scaledResolution = new ScaledResolution(mc);
        this.syncPosition(scaledResolution);
        GlStateManager.func_179094_E();
        GlStateManager.func_179147_l();
        GlStateManager.func_179112_b(770, 771);
        mc.field_71466_p.func_175063_a(color + blocks + " §rblock" + (blocks == 1 ? "" : "s"), this.posX, this.posY, colorAlpha);
        GlStateManager.func_179084_k();
        GlStateManager.func_179121_F();
    }

    private boolean isBlockedHoverBlock() {
        if (this.blockBlacklistToggle.isToggled() && mc.field_71476_x != null && mc.field_71476_x.field_72313_a == MovingObjectType.BLOCK) {
            BlockPos hoveredPos = mc.field_71476_x.func_178782_a();
            if (hoveredPos == null) {
                return false;
            } else {
                IBlockState state = mc.field_71441_e.func_180495_p(hoveredPos);
                Block hoveredBlock = state.func_177230_c();
                if (hoveredBlock != null && Block.field_149771_c.func_177774_c(hoveredBlock) != null) {
                    String registryId = ((ResourceLocation) Block.field_149771_c.func_177774_c(hoveredBlock)).toString();
                    int meta = hoveredBlock.func_176201_c(state);
                    String storageId = meta != 0 ? registryId + ":" + meta : registryId;
                    return this.blockBlacklist.contains(storageId) || this.blockBlacklist.contains(registryId);
                } else {
                    return false;
                }
            }
        } else {
            return false;
        }
    }

    private boolean isRightClickActive() {
        return Utils.isBindDown(mc.field_71474_y.field_74313_G) || mc.field_71439_g.func_71039_bw();
    }

    private boolean canFastPlace(long now, boolean requireActivationDelay) {
        if (this.blocksOnly.isToggled()) {
            ItemStack item = mc.field_71439_g.func_70694_bm();
            if (item == null || !(item.func_77973_b() instanceof ItemBlock)) {
                return false;
            }
        }
        if (this.pitchCheck.isToggled() && mc.field_71439_g.field_70125_A < 70.0F) {
            return false;
        } else if (this.ignoredHeldItemsToggle.isToggled() && this.ignoredHeldItems.matches(mc.field_71439_g.func_70694_bm())) {
            return false;
        } else {
            return this.isBlockedHoverBlock() ? false : !requireActivationDelay || now - this.rightClickStartTime >= (long) this.activationTime.getInput();
        }
    }

    public int getTotalBlocks() {
        int totalBlocks = 0;
        if (mc.field_71439_g != null && mc.field_71439_g.field_71071_by != null) {
            for (int i = 0; i < 9; i++) {
                ItemStack stack = mc.field_71439_g.field_71071_by.field_70462_a[i];
                if (stack != null && stack.func_77973_b() instanceof ItemBlock && Utils.canBePlaced((ItemBlock) stack.func_77973_b()) && stack.field_77994_a > 0) {
                    totalBlocks += stack.field_77994_a;
                }
            }
            return totalBlocks;
        } else {
            return 0;
        }
    }

    private class EditScreen extends GuiScreen {

        private GuiButtonExt resetBtn;

        private boolean dragging;

        private float actualX;

        private float actualY;

        private float lastActualX;

        private float lastActualY;

        private int lastMouseX;

        private int lastMouseY;

        private EditScreen() {
        }

        public void func_73866_w_() {
            super.func_73866_w_();
            this.field_146292_n.add(this.resetBtn = new GuiButtonExt(1, this.field_146294_l - 90, this.field_146295_m - 25, 85, 20, "Reset position"));
            FastPlace.this.syncPosition(new ScaledResolution(this.field_146297_k));
            this.actualX = FastPlace.this.posX;
            this.actualY = FastPlace.this.posY;
        }

        public void func_73863_a(int mouseX, int mouseY, float partialTicks) {
            ScaledResolution resolution = new ScaledResolution(this.field_146297_k);
            if (!this.dragging) {
                FastPlace.this.syncPosition(resolution);
                this.actualX = FastPlace.this.posX;
                this.actualY = FastPlace.this.posY;
            }
            func_73734_a(0, 0, this.field_146294_l, this.field_146295_m, -1308622848);
            FastPlace.this.setAbsolutePosition(this.actualX, this.actualY, resolution);
            FastPlace.this.renderBlockCount(255, true);
            this.actualX = FastPlace.this.posX;
            this.actualY = FastPlace.this.posY;
            String message = "Edit the position by dragging.";
            int textX = resolution.func_78326_a() / 2 - this.field_146289_q.func_78256_a(message) / 2;
            int textY = resolution.func_78328_b() / 2 - 20;
            RenderUtils.drawColoredString(message, '-', textX, textY, 2L, 0L, true, this.field_146297_k.field_71466_p);
            try {
                this.func_146269_k();
            } catch (IOException exception) {
            }
            super.func_73863_a(mouseX, mouseY, partialTicks);
        }

        protected void func_146273_a(int mouseX, int mouseY, int button, long timeSinceLastClick) {
            super.func_146273_a(mouseX, mouseY, button, timeSinceLastClick);
            if (button == 0) {
                if (this.dragging) {
                    this.actualX = this.lastActualX + (mouseX - this.lastMouseX);
                    this.actualY = this.lastActualY + (mouseY - this.lastMouseY);
                } else {
                    int blocks = FastPlace.this.getTotalBlocks();
                    if (blocks == 0) {
                        blocks = 64;
                    }
                    String text = blocks + " block" + (blocks == 1 ? "" : "s");
                    int textWidth = this.field_146289_q.func_78256_a(text);
                    int textHeight = this.field_146289_q.field_78288_b;
                    float minX = FastPlace.this.posX - 2.0F;
                    float minY = FastPlace.this.posY - 2.0F;
                    float maxX = FastPlace.this.posX + textWidth + 2.0F;
                    float maxY = FastPlace.this.posY + textHeight + 2.0F;
                    if (mouseX >= minX && mouseX <= maxX && mouseY >= minY && mouseY <= maxY) {
                        this.dragging = true;
                        this.lastMouseX = mouseX;
                        this.lastMouseY = mouseY;
                        this.lastActualX = this.actualX;
                        this.lastActualY = this.actualY;
                    }
                }
            }
        }

        protected void func_146286_b(int mouseX, int mouseY, int state) {
            super.func_146286_b(mouseX, mouseY, state);
            if (state == 0) {
                this.dragging = false;
            }
        }

        public void func_146284_a(GuiButton button) {
            if (button == this.resetBtn) {
                FastPlace.this.resetPosition();
                this.actualX = FastPlace.this.posX;
                this.actualY = FastPlace.this.posY;
            }
        }

        public boolean func_73868_f() {
            return false;
        }
    }
}
