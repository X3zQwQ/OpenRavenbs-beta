package keystrokesmod.module.impl.player;

import java.io.IOException;
import java.util.List;
import keystrokesmod.event.ReceivePacketEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.ColorSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.module.setting.impl.StringListSetting;
import keystrokesmod.utility.RenderUtils;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.network.play.server.S2EPacketCloseWindow;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.event.world.WorldEvent.Load;
import net.minecraftforge.fml.client.config.GuiButtonExt;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.RenderTickEvent;

public class HideWindow extends Module {

    private static final String ICON_PATH = "/assets/keystrokesmod/textures/gui/tv_off.png";

    private static final int ICON_BASE_SIZE = 16;

    private static final float DEFAULT_RELATIVE_X = 0.5F;

    private static final float DEFAULT_RELATIVE_Y = 0.05F;

    private static final int EDIT_OUTLINE_COLOR = -1;

    private final ColorSetting iconColor;

    private final SliderSetting iconScale;

    private final ButtonSetting onlyWhileCrouching;

    private final ButtonSetting whitelist;

    private final StringListSetting whitelistEntries;

    private GuiContainer hiddenGui;

    private float posX = Float.NaN;

    private float posY = Float.NaN;

    private float relativePosX = Float.NaN;

    private float relativePosY = Float.NaN;

    public HideWindow() {
        super("Hide Window", Module.category.player);
        this.registerSetting(this.iconColor = new ColorSetting("Icon color", 255, 255, 255));
        this.registerSetting(this.iconScale = new SliderSetting("Icon scale", 1.0, 0.5, 3.0, 0.1));
        this.registerSetting(new ButtonSetting("Edit position", () -> mc.func_147108_a(new HideWindow.EditScreen())));
        this.registerSetting(this.onlyWhileCrouching = new ButtonSetting("Only while crouching", false));
        this.registerSetting(this.whitelist = new ButtonSetting("Whitelist", false));
        this.registerSetting(this.whitelistEntries = new StringListSetting("Whitelist names", "e.g. Upgrades & Traps", 128));
        this.whitelistEntries.visible = false;
    }

    @Override
    public String getInfo() {
        return this.hiddenGui != null ? "Hidden" : "";
    }

    @Override
    public void guiUpdate() {
        this.whitelistEntries.setVisible(this.whitelist.isToggled(), this);
    }

    @Override
    public void onDisable() {
        if (this.hiddenGui != null && mc.field_71439_g != null) {
            mc.field_71439_g.func_71053_j();
        }
        this.hiddenGui = null;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onGuiOpen(GuiOpenEvent event) {
        if (event.gui != null && mc.field_71439_g != null) {
            if (event.gui instanceof GuiContainer && !(event.gui instanceof GuiInventory)) {
                if (mc.field_71462_r instanceof GuiContainer) {
                    this.hiddenGui = null;
                } else {
                    GuiContainer gui = (GuiContainer) event.gui;
                    if (!this.onlyWhileCrouching.isToggled() || mc.field_71439_g.func_70093_af()) {
                        if (!this.whitelist.isToggled() || this.matchesWhitelist(gui)) {
                            this.hiddenGui = gui;
                            mc.field_71439_g.field_71070_bA = this.hiddenGui.field_147002_h;
                            event.setCanceled(true);
                        }
                    }
                }
            } else {
                if (event.gui instanceof GuiInventory && this.hiddenGui != null) {
                    event.gui = this.hiddenGui;
                    this.hiddenGui = null;
                }
            }
        }
    }

    @SubscribeEvent
    public void onReceivePacket(ReceivePacketEvent event) {
        if (event.getPacket() instanceof S2EPacketCloseWindow) {
            this.hiddenGui = null;
        }
    }

    @SubscribeEvent
    public void onWorldLoad(Load event) {
        this.hiddenGui = null;
    }

    @SubscribeEvent
    public void onRenderTick(RenderTickEvent event) {
        if (event.phase == Phase.END) {
            if (this.hiddenGui != null && mc.field_71462_r == null && !mc.field_71474_y.field_74330_P) {
                this.renderIcon(false);
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

    public void resetPosition() {
        this.setRelativePosition(0.5F, 0.05F);
    }

    private void renderIcon(boolean editing) {
        this.syncPosition();
        ResourceLocation icon = RenderUtils.getIcon("/assets/keystrokesmod/textures/gui/tv_off.png");
        if (icon != null) {
            float scale = (float) this.iconScale.getInput();
            int size = Math.round(16.0F * scale);
            float drawX = this.posX - size / 2.0F;
            float drawY = this.posY - size / 2.0F;
            RenderUtils.drawIcon(icon, drawX, drawY, size, 0xFF000000 | this.iconColor.getRGB());
            if (editing) {
                float outLeft = drawX - 2.0F;
                float outTop = drawY - 2.0F;
                float outRight = drawX + size + 2.0F;
                float outBottom = drawY + size + 2.0F;
                RenderUtils.drawRect(outLeft, outTop, outRight, outTop + 1.0F, -1);
                RenderUtils.drawRect(outLeft, outBottom - 1.0F, outRight, outBottom, -1);
                RenderUtils.drawRect(outLeft, outTop, outLeft + 1.0F, outBottom, -1);
                RenderUtils.drawRect(outRight - 1.0F, outTop, outRight, outBottom, -1);
            }
        }
    }

    private void syncPosition() {
        this.syncPosition(new ScaledResolution(mc));
    }

    private void syncPosition(ScaledResolution resolution) {
        int w = Math.max(1, resolution.func_78326_a());
        int h = Math.max(1, resolution.func_78328_b());
        if (Float.isNaN(this.relativePosX) || Float.isNaN(this.relativePosY)) {
            if (!Float.isNaN(this.posX) && !Float.isNaN(this.posY)) {
                this.relativePosX = this.posX / w;
                this.relativePosY = this.posY / h;
            } else {
                this.relativePosX = 0.5F;
                this.relativePosY = 0.05F;
            }
        }
        this.posX = this.relativePosX * w;
        this.posY = this.relativePosY * h;
    }

    private void setAbsolutePosition(float absoluteX, float absoluteY, ScaledResolution resolution) {
        this.posX = absoluteX;
        this.posY = absoluteY;
        int w = Math.max(1, resolution.func_78326_a());
        int h = Math.max(1, resolution.func_78328_b());
        this.relativePosX = absoluteX / w;
        this.relativePosY = absoluteY / h;
    }

    private boolean matchesWhitelist(GuiContainer gui) {
        List<String> entries = this.whitelistEntries.getEntries();
        if (entries.isEmpty()) {
            return false;
        } else {
            String title = getContainerTitle(gui);
            if (title.isEmpty()) {
                return false;
            } else {
                String lower = title.toLowerCase();
                for (String name : entries) {
                    if (lower.contains(name.toLowerCase())) {
                        return true;
                    }
                }
                return false;
            }
        }
    }

    private static String getContainerTitle(GuiContainer gui) {
        return gui.field_147002_h instanceof ContainerChest ? ((ContainerChest) gui.field_147002_h).func_85151_d().func_145748_c_().func_150260_c() : "";
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
            HideWindow.this.syncPosition(new ScaledResolution(this.field_146297_k));
            this.actualX = HideWindow.this.posX;
            this.actualY = HideWindow.this.posY;
        }

        public void func_73863_a(int mouseX, int mouseY, float partialTicks) {
            ScaledResolution resolution = new ScaledResolution(this.field_146297_k);
            if (!this.dragging) {
                HideWindow.this.syncPosition(resolution);
                this.actualX = HideWindow.this.posX;
                this.actualY = HideWindow.this.posY;
            }
            func_73734_a(0, 0, this.field_146294_l, this.field_146295_m, -1308622848);
            HideWindow.this.setAbsolutePosition(this.actualX, this.actualY, resolution);
            HideWindow.this.renderIcon(true);
            this.actualX = HideWindow.this.posX;
            this.actualY = HideWindow.this.posY;
            String message = "Drag the icon to reposition it.";
            int textX = resolution.func_78326_a() / 2 - this.field_146289_q.func_78256_a(message) / 2;
            int textY = resolution.func_78328_b() / 2 - 10;
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
                    float scale = (float) HideWindow.this.iconScale.getInput();
                    int size = Math.round(16.0F * scale);
                    float minX = HideWindow.this.posX - size / 2.0F - 2.0F;
                    float minY = HideWindow.this.posY - size / 2.0F - 2.0F;
                    float maxX = HideWindow.this.posX + size / 2.0F + 2.0F;
                    float maxY = HideWindow.this.posY + size / 2.0F + 2.0F;
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
                HideWindow.this.resetPosition();
                this.actualX = HideWindow.this.posX;
                this.actualY = HideWindow.this.posY;
            }
        }

        public boolean func_73868_f() {
            return false;
        }
    }
}
