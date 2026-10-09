package keystrokesmod.clickgui;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import keystrokesmod.Raven;
import keystrokesmod.clickgui.components.Component;
import keystrokesmod.clickgui.components.FocusableTextComponent;
import keystrokesmod.clickgui.components.impl.BindComponent;
import keystrokesmod.clickgui.components.impl.CategoryComponent;
import keystrokesmod.clickgui.components.impl.ModuleComponent;
import keystrokesmod.clickgui.components.impl.SliderComponent;
import keystrokesmod.module.Module;
import keystrokesmod.module.impl.client.CommandLine;
import keystrokesmod.module.impl.client.Gui;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.CommandHandler;
import keystrokesmod.utility.Timer;
import keystrokesmod.utility.Utils;
import keystrokesmod.utility.shader.BlurUtils;
import keystrokesmod.utility.shader.RoundedUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.MathHelper;
import net.minecraftforge.client.event.GuiScreenEvent.InitGuiEvent.Post;
import net.minecraftforge.client.event.GuiScreenEvent.InitGuiEvent.Pre;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.config.GuiButtonExt;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

public class ClickGui extends GuiScreen {

    private ScheduledFuture sf;

    private Timer logoSmoothWidth;

    private Timer logoSmoothLength;

    private Timer smoothEntity;

    private Timer backgroundFade;

    private Timer blurSmooth;

    private ScaledResolution sr;

    private GuiButtonExt commandLineSend;

    private GuiTextField commandLineInput;

    public static ArrayList<CategoryComponent> categories;

    private int actualScreenWidth;

    private int actualScreenHeight;

    private double previousScale;

    private static boolean isNotFirstOpen;

    private boolean pendingScaleRefresh;

    private SliderSetting heldArrowSlider;

    private int heldArrowDirection;

    private long arrowHoldStartedAt;

    private long lastArrowAdjustmentAt;

    private static final long ARROW_HOLD_DELAY_MS = 300L;

    private static final double ARROW_INITIAL_REPEAT_MS = 180.0;

    private static final double ARROW_ACCELERATION_HALF_LIFE_MS = 750.0;

    private static final long ARROW_MIN_REPEAT_MS = 20L;

    public ClickGui() {
        categories = new ArrayList<>();
        int y = 5;
        Module.category[] values;
        for (Module.category c : values = Module.category.values()) {
            CategoryComponent categoryComponent = new CategoryComponent(c);
            categoryComponent.setY(y, false);
            categories.add(categoryComponent);
            y += 20;
        }
    }

    public void initMain() {
        (this.logoSmoothWidth = this.smoothEntity = this.blurSmooth = this.backgroundFade = new Timer(500.0F)).start();
        this.sf = Raven.getScheduledExecutor().schedule(() -> (this.logoSmoothLength = new Timer(650.0F)).start(), 650L, TimeUnit.MILLISECONDS);
    }

    public void resetPositions() {
        int y = 5;
        for (CategoryComponent categoryComponent : categories) {
            categoryComponent.applySavedState(5.0F, y, false, false);
            y += 20;
        }
    }

    public void func_73866_w_() {
        super.func_73866_w_();
        double configuredScale = this.getConfiguredGuiScale();
        if (!isNotFirstOpen) {
            isNotFirstOpen = true;
            this.previousScale = configuredScale;
        }
        for (CategoryComponent categoryComponent : categories) {
            categoryComponent.setScreenSize(this.field_146294_l, this.field_146295_m);
        }
        if (Double.compare(this.previousScale, configuredScale) != 0) {
            for (CategoryComponent categoryComponent : categories) {
                categoryComponent.limitPositions();
            }
        }
        this.reloadModulesForCurrentMode();
        (this.commandLineInput = new GuiTextField(1, this.field_146297_k.field_71466_p, 22, this.field_146295_m - 100, 150, 20)).func_146203_f(256);
        this.field_146292_n.add(this.commandLineSend = new GuiButtonExt(2, 22, this.field_146295_m - 70, 150, 20, "Send"));
        this.commandLineSend.field_146125_m = CommandLine.opened;
        this.previousScale = configuredScale;
    }

    public void reloadModulesForCurrentMode() {
        for (CategoryComponent categoryComponent : categories) {
            if (categoryComponent.category == Module.category.profiles) {
                categoryComponent.reloadModules(true);
            } else if (categoryComponent.category == Module.category.scripts) {
                categoryComponent.reloadModules(false);
            } else {
                categoryComponent.reloadModules();
            }
        }
    }

    private List<CategoryComponent> getCategoriesInRenderOrder() {
        List<CategoryComponent> renderOrder = new ArrayList<>(categories);
        renderOrder.sort(Comparator.comparingLong(c -> c.lastInteractedTime));
        return renderOrder;
    }

    private CategoryComponent getTopmostUnderCursor(List<CategoryComponent> renderOrder, int x, int y) {
        for (int i = renderOrder.size() - 1; i >= 0; i--) {
            if (renderOrder.get(i).overRect(x, y)) {
                return renderOrder.get(i);
            }
        }
        return null;
    }

    public void func_73863_a(int x, int y, float p) {
        if (this.pendingScaleRefresh) {
            this.pendingScaleRefresh = false;
            this.refreshLayoutForConfiguredScale();
        }
        int logicalMouseX = this.toLogicalCoordinate(x);
        int logicalMouseY = this.toLogicalCoordinate(y);
        if (Gui.backgroundBlur.getInput() != 0.0) {
            BlurUtils.prepareBlur();
            RoundedUtils.drawRound(0.0F, 0.0F, this.actualScreenWidth, this.actualScreenHeight, 0.0F, true, Color.black);
            float inputToRange = (float) (3.0 * ((Gui.backgroundBlur.getInput() + 35.0) / 100.0));
            BlurUtils.blurEnd(2, this.blurSmooth.getValueFloat(0.0F, inputToRange, 1));
        }
        if (Gui.darkBackground.isToggled()) {
            func_73734_a(0, 0, this.actualScreenWidth, this.actualScreenHeight, (int) (this.backgroundFade.getValueFloat(0.0F, 0.7F, 2) * 255.0F) << 24);
        }
        GlStateManager.func_179094_E();
        GlStateManager.func_179139_a(this.getRenderScale(), this.getRenderScale(), 1.0);
        if (!Gui.hideWatermark.isToggled()) {
            int h = this.field_146295_m / 4;
            int wd = this.field_146294_l / 2;
            int w_c = 30 - this.logoSmoothWidth.getValueInt(0, 30, 3);
            this.func_73732_a(this.field_146289_q, "r", wd + 1 - w_c, h - 25, Utils.getChroma(2L, 1500L));
            this.func_73732_a(this.field_146289_q, "a", wd - w_c, h - 15, Utils.getChroma(2L, 1200L));
            this.func_73732_a(this.field_146289_q, "v", wd - w_c, h - 5, Utils.getChroma(2L, 900L));
            this.func_73732_a(this.field_146289_q, "e", wd - w_c, h + 5, Utils.getChroma(2L, 600L));
            this.func_73732_a(this.field_146289_q, "n", wd - w_c, h + 15, Utils.getChroma(2L, 300L));
            this.func_73732_a(this.field_146289_q, "bS", wd + 1 + w_c, h + 30, Utils.getChroma(2L, 0L));
            this.func_73728_b(wd - 10 - w_c, h - 30, h + 43, Color.white.getRGB());
            this.func_73728_b(wd + 10 + w_c, h - 30, h + 43, Color.white.getRGB());
            if (this.logoSmoothLength != null) {
                int r = this.logoSmoothLength.getValueInt(0, 20, 2);
                this.func_73730_a(wd - 10, wd - 10 + r, h - 29, -1);
                this.func_73730_a(wd + 10, wd + 10 - r, h + 42, -1);
            }
        }
        this.renderClientInfo(this.field_146295_m + 8 - this.logoSmoothWidth.getValueInt(0, 30, 2), this.field_146289_q);
        List<CategoryComponent> renderOrder = this.getCategoriesInRenderOrder();
        CategoryComponent topmostUnderCursor = this.getTopmostUnderCursor(renderOrder, logicalMouseX, logicalMouseY);
        for (CategoryComponent c : renderOrder) {
            c.render(this.field_146289_q);
            c.mousePosition(logicalMouseX, logicalMouseY, c == topmostUnderCursor);
            for (Component m : c.getModules()) {
                m.drawScreen(logicalMouseX, logicalMouseY);
            }
        }
        this.updateHeldSliderAdjustment(logicalMouseX, logicalMouseY);
        GL11.glColor3f(1.0F, 1.0F, 1.0F);
        if (!Gui.hidePlayerModel.isToggled()) {
            GlStateManager.func_179094_E();
            GlStateManager.func_179084_k();
            GuiInventory.func_147046_a(this.field_146294_l + 15 - this.smoothEntity.getValueInt(0, 40, 2), this.field_146295_m - 10, 40, this.field_146294_l - 25 - logicalMouseX, this.field_146295_m - 50 - logicalMouseY, this.field_146297_k.field_71439_g);
            GlStateManager.func_179147_l();
            GlStateManager.func_179121_F();
        }
        if (CommandLine.opened) {
            if (!this.commandLineSend.field_146125_m) {
                this.commandLineSend.field_146125_m = true;
            }
            int r = CommandLine.animate.isToggled() ? CommandLine.animation.getValueInt(0, 200, 2) : 200;
            if (CommandLine.closed) {
                r = 200 - r;
                if (r == 0) {
                    CommandLine.closed = false;
                    CommandLine.opened = false;
                    this.commandLineSend.field_146125_m = false;
                }
            }
            func_73734_a(0, 0, r, this.field_146295_m, -1089466352);
            this.func_73730_a(0, r - 1, this.field_146295_m - 345, -1);
            this.func_73730_a(0, r - 1, this.field_146295_m - 115, -1);
            func_73734_a(r - 1, 0, r, this.field_146295_m, -1);
            CommandHandler.renderCommandOutput(this.field_146289_q, this.field_146295_m, r, this.sr.func_78325_e());
            int x2 = r - 178;
            this.commandLineInput.field_146209_f = x2;
            this.commandLineSend.field_146128_h = x2;
            this.commandLineInput.func_146194_f();
            super.func_73863_a(logicalMouseX, logicalMouseY, p);
        } else if (CommandLine.closed) {
            CommandLine.closed = false;
        }
        GlStateManager.func_179121_F();
    }

    public void func_73864_a(int mouseX, int mouseY, int mouseButton) throws IOException {
        List<CategoryComponent> inputOrder = new ArrayList<>(categories);
        inputOrder.sort((a, b) -> Long.compare(b.lastInteractedTime, a.lastInteractedTime));
        CategoryComponent topmostCategory = null;
        for (CategoryComponent category : inputOrder) {
            if (category.overRect(mouseX, mouseY)) {
                topmostCategory = category;
                break;
            }
        }
        if (topmostCategory != null) {
            topmostCategory.markInteracted();
        }
        if (mouseButton == 0) {
            for (CategoryComponent categoryComponent2 : categories) {
                categoryComponent2.overTitle(false);
            }
            if (topmostCategory != null && topmostCategory.draggable(mouseX, mouseY)) {
                topmostCategory.overTitle(true);
                topmostCategory.xx = mouseX - topmostCategory.getX();
                topmostCategory.yy = mouseY - topmostCategory.getY();
                topmostCategory.dragging = true;
            }
        }
        if (mouseButton == 1 && topmostCategory != null && topmostCategory.overTitle(mouseX, mouseY)) {
            topmostCategory.mouseClicked(!topmostCategory.isOpened());
        }
        if (topmostCategory != null && topmostCategory.isOpened() && !topmostCategory.getModules().isEmpty() && !topmostCategory.overTitle(mouseX, mouseY)) {
            for (ModuleComponent component : topmostCategory.getModules()) {
                if (component.onClick(mouseX, mouseY, mouseButton)) {
                    break;
                }
            }
        }
        if (CommandLine.opened) {
            this.commandLineInput.func_146192_a(mouseX, mouseY, mouseButton);
            super.func_73864_a(mouseX, mouseY, mouseButton);
        }
        if (mouseButton == 0 || mouseButton == 1) {
            FocusableTextComponent focusedComponent = this.findFocusedTextComponentAt(mouseX, mouseY);
            this.enforceSingleFocusedTextInput(focusedComponent);
        }
    }

    public void func_146286_b(int x, int y, int button) {
        if (button == 0) {
            for (CategoryComponent category : categories) {
                category.overTitle(false);
                if (category.isOpened() && !category.getModules().isEmpty()) {
                    for (Component module : category.getModules()) {
                        module.mouseReleased(x, y, button);
                    }
                }
            }
        }
        if (this.pendingScaleRefresh) {
            this.pendingScaleRefresh = false;
            this.refreshLayoutForConfiguredScale();
        }
    }

    public void func_146274_d() throws IOException {
        super.func_146274_d();
        int wheelInput = Mouse.getDWheel();
        if (wheelInput != 0) {
            int mouseX = Mouse.getEventX() * this.field_146294_l / this.field_146297_k.field_71443_c;
            int mouseY = this.field_146295_m - Mouse.getEventY() * this.field_146295_m / this.field_146297_k.field_71440_d - 1;
            for (CategoryComponent category : categories) {
                category.onScroll(wheelInput, mouseX, mouseY);
            }
        }
    }

    public void refreshAfterProfileLoad() {
        if (this.field_146297_k == null) {
            this.field_146297_k = Minecraft.func_71410_x();
        }
        this.reloadModulesForCurrentMode();
        this.refreshLayoutForConfiguredScale();
    }

    public void func_146280_a(Minecraft p_setWorldAndResolution_1_, int p_setWorldAndResolution_2_, int p_setWorldAndResolution_3_) {
        this.field_146297_k = p_setWorldAndResolution_1_;
        this.field_146296_j = p_setWorldAndResolution_1_.func_175599_af();
        this.field_146289_q = p_setWorldAndResolution_1_.field_71466_p;
        this.refreshScaledResolution();
        if (!MinecraftForge.EVENT_BUS.post(new Pre(this, this.field_146292_n))) {
            this.field_146292_n.clear();
            this.func_73866_w_();
        }
        MinecraftForge.EVENT_BUS.post(new Post(this, this.field_146292_n));
    }

    public void func_73869_a(char t, int k) {
        FocusableTextComponent activeTextInput = this.getActiveFocusedTextInput();
        if (k == 1) {
            if (activeTextInput != null) {
                activeTextInput.unfocusTextInput();
                return;
            }
            if (!this.binding()) {
                this.field_146297_k.func_147108_a(null);
                return;
            }
        }
        if (activeTextInput != null) {
            for (CategoryComponent category : categories) {
                if (category.isOpened() && !category.getModules().isEmpty()) {
                    for (Component module : category.getModules()) {
                        module.keyTyped(t, k);
                    }
                }
            }
        } else if (!this.binding() && this.adjustHoveredSlider(k)) {
            if (this.pendingScaleRefresh) {
                this.pendingScaleRefresh = false;
                this.refreshLayoutForConfiguredScale();
            }
        } else {
            for (CategoryComponent categoryComponent3 : categories) {
                if (categoryComponent3.isOpened() && !categoryComponent3.getModules().isEmpty()) {
                    for (Component module : categoryComponent3.getModules()) {
                        module.keyTyped(t, k);
                    }
                }
            }
            if (CommandLine.opened) {
                String cm = this.commandLineInput.func_146179_b();
                if (k == 28 && !cm.isEmpty()) {
                    CommandHandler.runCommand(this.commandLineInput.func_146179_b());
                    this.commandLineInput.func_146180_a("");
                    return;
                }
                this.commandLineInput.func_146201_a(t, k);
            }
        }
    }

    private boolean adjustHoveredSlider(int keyCode) {
        if (keyCode != 203 && keyCode != 205) {
            return false;
        } else if (CommandLine.opened && this.commandLineInput.func_146206_l()) {
            return false;
        } else {
            int mouseX = Mouse.getX() * this.field_146294_l / this.field_146297_k.field_71443_c;
            int mouseY = this.field_146295_m - Mouse.getY() * this.field_146295_m / this.field_146297_k.field_71440_d - 1;
            SliderComponent slider = this.getHoveredSlider(mouseX, mouseY);
            if (slider == null) {
                return false;
            } else {
                int direction = keyCode == 205 ? 1 : -1;
                if (this.heldArrowSlider != slider.sliderSetting || this.heldArrowDirection != direction) {
                    long now = System.currentTimeMillis();
                    this.heldArrowSlider = slider.sliderSetting;
                    this.heldArrowDirection = direction;
                    this.arrowHoldStartedAt = now;
                    this.lastArrowAdjustmentAt = now;
                    slider.adjustValue(direction);
                }
                return true;
            }
        }
    }

    private void renderClientInfo(int y, FontRenderer fontRenderer) {
        fontRenderer.func_175063_a("raven bs beta-17-1092726", 4.0F, y + 10.0F, Gui.INFO_COLOR);
    }

    private SliderComponent getHoveredSlider(int mouseX, int mouseY) {
        CategoryComponent category = this.getTopmostUnderCursor(this.getCategoriesInRenderOrder(), mouseX, mouseY);
        if (category != null && category.isOpened() && !category.overTitle(mouseX, mouseY)) {
            for (ModuleComponent module : category.getModules()) {
                for (Component component : module.settings) {
                    if (component instanceof SliderComponent) {
                        SliderComponent slider = (SliderComponent) component;
                        if (slider.isHovered(mouseX, mouseY)) {
                            return slider;
                        }
                    }
                }
            }
            return null;
        } else {
            return null;
        }
    }

    private void updateHeldSliderAdjustment(int mouseX, int mouseY) {
        if (this.heldArrowSlider != null) {
            int heldKey = this.heldArrowDirection > 0 ? 205 : 203;
            SliderComponent hoveredSlider = this.getHoveredSlider(mouseX, mouseY);
            if (Keyboard.isKeyDown(heldKey) && !this.binding() && (!CommandLine.opened || !this.commandLineInput.func_146206_l()) && hoveredSlider != null && hoveredSlider.sliderSetting == this.heldArrowSlider) {
                long now = System.currentTimeMillis();
                long heldDuration = now - this.arrowHoldStartedAt;
                if (heldDuration >= 300L) {
                    long acceleratingDuration = heldDuration - 300L;
                    long repeatDelay = Math.max(20L, Math.round(180.0 * Math.pow(0.5, acceleratingDuration / 750.0)));
                    if (now - this.lastArrowAdjustmentAt >= repeatDelay) {
                        hoveredSlider.adjustValue(this.heldArrowDirection);
                        this.lastArrowAdjustmentAt = now;
                    }
                }
            } else {
                this.clearHeldSliderAdjustment();
            }
        }
    }

    private void clearHeldSliderAdjustment() {
        this.heldArrowSlider = null;
        this.heldArrowDirection = 0;
        this.arrowHoldStartedAt = 0L;
        this.lastArrowAdjustmentAt = 0L;
    }

    public void func_146284_a(GuiButton b) {
        if (b == this.commandLineSend) {
            CommandHandler.runCommand(this.commandLineInput.func_146179_b());
            this.commandLineInput.func_146180_a("");
        }
    }

    public void func_146281_b() {
        this.logoSmoothLength = null;
        if (this.sf != null) {
            this.sf.cancel(true);
            this.sf = null;
        }
        for (CategoryComponent c : categories) {
            c.dragging = false;
            c.onGuiClosed();
            for (Component m : c.getModules()) {
                m.onGuiClosed();
            }
        }
        this.clearHeldSliderAdjustment();
    }

    public boolean func_73868_f() {
        return false;
    }

    private boolean binding() {
        for (CategoryComponent c : categories) {
            for (ModuleComponent m : c.getModules()) {
                for (Component component : m.settings) {
                    if (component instanceof BindComponent && ((BindComponent) component).isBinding) {
                        return true;
                    }
                    if (component instanceof FocusableTextComponent && ((FocusableTextComponent) component).isTextInputFocused()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean unfocusFocusedTextInput() {
        for (CategoryComponent category : categories) {
            for (ModuleComponent module : category.getModules()) {
                for (Component component : module.settings) {
                    if (component instanceof FocusableTextComponent) {
                        FocusableTextComponent textComponent = (FocusableTextComponent) component;
                        if (textComponent.isTextInputFocused()) {
                            textComponent.unfocusTextInput();
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private FocusableTextComponent getActiveFocusedTextInput() {
        FocusableTextComponent activeComponent = null;
        for (CategoryComponent category : categories) {
            for (ModuleComponent module : category.getModules()) {
                for (Component component : module.settings) {
                    if (component instanceof FocusableTextComponent) {
                        FocusableTextComponent textComponent = (FocusableTextComponent) component;
                        if (textComponent.isTextInputFocused()) {
                            if (activeComponent == null) {
                                activeComponent = textComponent;
                            } else {
                                textComponent.unfocusTextInput();
                            }
                        }
                    }
                }
            }
        }
        return activeComponent;
    }

    private FocusableTextComponent findFocusedTextComponentAt(int mouseX, int mouseY) {
        List<CategoryComponent> inputOrder = new ArrayList<>(categories);
        inputOrder.sort((a, b) -> Long.compare(b.lastInteractedTime, a.lastInteractedTime));
        for (CategoryComponent category : inputOrder) {
            if (category.isOpened() && category.overRect(mouseX, mouseY)) {
                for (ModuleComponent module : category.getModules()) {
                    for (Component component : module.settings) {
                        if (component instanceof FocusableTextComponent) {
                            FocusableTextComponent textComponent = (FocusableTextComponent) component;
                            if (textComponent.isTextInputFocused() && textComponent.containsClick(mouseX, mouseY)) {
                                return textComponent;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private void enforceSingleFocusedTextInput(FocusableTextComponent focusedComponentToKeep) {
        for (CategoryComponent category : categories) {
            for (ModuleComponent module : category.getModules()) {
                for (Component component : module.settings) {
                    if (component instanceof FocusableTextComponent) {
                        FocusableTextComponent textComponent = (FocusableTextComponent) component;
                        if (textComponent != focusedComponentToKeep && textComponent.isTextInputFocused()) {
                            textComponent.unfocusTextInput();
                        }
                    }
                }
            }
        }
    }

    public void onSliderChange() {
        for (CategoryComponent c : categories) {
            for (ModuleComponent m : c.getModules()) {
                m.onSliderChange();
            }
        }
    }

    public void requestScaleRefresh() {
        this.pendingScaleRefresh = true;
    }

    private void refreshLayoutForConfiguredScale() {
        this.refreshScaledResolution();
        for (CategoryComponent categoryComponent : categories) {
            categoryComponent.setScreenSize(this.field_146294_l, this.field_146295_m);
            categoryComponent.limitPositions();
        }
        this.field_146292_n.clear();
        this.func_73866_w_();
    }

    private void refreshScaledResolution() {
        this.sr = new ScaledResolution(this.field_146297_k);
        this.actualScreenWidth = this.sr.func_78326_a();
        this.actualScreenHeight = this.sr.func_78328_b();
        double targetScaleFactor = this.getTargetGuiScaleFactor();
        this.field_146294_l = Math.max(1, MathHelper.func_76143_f(this.field_146297_k.field_71443_c / targetScaleFactor));
        this.field_146295_m = Math.max(1, MathHelper.func_76143_f(this.field_146297_k.field_71440_d / targetScaleFactor));
    }

    private int getMaximumGuiScaleFactor() {
        int scaleFactor = 1;
        while (this.field_146297_k.field_71443_c / (scaleFactor + 1) >= 320 && this.field_146297_k.field_71440_d / (scaleFactor + 1) >= 240) {
            scaleFactor++;
        }
        if (this.field_146297_k.func_152349_b() && scaleFactor % 2 != 0 && scaleFactor != 1) {
            scaleFactor--;
        }
        return scaleFactor;
    }

    private double getTargetGuiScaleFactor() {
        return Math.max(1.0, Math.min((double) this.getMaximumGuiScaleFactor(), this.getConfiguredGuiScale() * 2.0));
    }

    private int toLogicalCoordinate(int coordinate) {
        return (int) Math.floor(coordinate / this.getRenderScale());
    }

    private double getRenderScale() {
        return this.actualScreenWidth > 0 && this.field_146294_l > 0 ? (double) this.actualScreenWidth / this.field_146294_l : 1.0;
    }

    public static double getActiveRenderScale() {
        Minecraft minecraft = Minecraft.func_71410_x();
        return minecraft.field_71462_r instanceof ClickGui ? ((ClickGui) minecraft.field_71462_r).getRenderScale() : 1.0;
    }

    private double getConfiguredGuiScale() {
        return Gui.getClickGuiScale();
    }
}
