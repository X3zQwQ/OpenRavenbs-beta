package keystrokesmod.clickgui.components.impl;

import java.awt.Color;
import java.nio.Buffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import keystrokesmod.Raven;
import keystrokesmod.clickgui.ClickGui;
import keystrokesmod.clickgui.components.Component;
import keystrokesmod.module.Module;
import keystrokesmod.module.impl.client.Gui;
import keystrokesmod.module.setting.Setting;
import keystrokesmod.module.setting.impl.BlockListSetting;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.ColorSetting;
import keystrokesmod.module.setting.impl.DescriptionSetting;
import keystrokesmod.module.setting.impl.GroupSetting;
import keystrokesmod.module.setting.impl.InventoryItemListSetting;
import keystrokesmod.module.setting.impl.ItemListSetting;
import keystrokesmod.module.setting.impl.KeySetting;
import keystrokesmod.module.setting.impl.PlayerListSetting;
import keystrokesmod.module.setting.impl.PotionListSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.module.setting.impl.StringListSetting;
import keystrokesmod.module.setting.impl.TextSetting;
import keystrokesmod.utility.RenderUtils;
import keystrokesmod.utility.Timer;
import keystrokesmod.utility.Utils;
import keystrokesmod.utility.font.RavenFontRenderer;
import keystrokesmod.utility.profile.Manager;
import keystrokesmod.utility.profile.ProfileModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

public class ModuleComponent extends Component {

    public Module mod;

    public CategoryComponent categoryComponent;

    public float yPos;

    public ArrayList<Component> settings;

    public boolean isOpened;

    private boolean hovering;

    private Timer hoverTimer;

    private boolean hoverStarted;

    private Timer smoothTimer;

    private float smoothingY = 16.0F;

    private float animationStartY = 16.0F;

    private float animationTargetY = 16.0F;

    private static final IntBuffer SCISSOR_BOX = BufferUtils.createIntBuffer(16);

    private static final float GROUP_CHILD_INDENT = 6.0F;

    private static final int ORIGINAL_HOVER_ALPHA = 120;

    private static final int HOVER_COLOR = new Color(0, 0, 0, 120).getRGB();

    private final Map<Component, GroupComponent> owningGroups = new IdentityHashMap<>();

    private final Map<String, GroupComponent> groupsByName = new HashMap<>();

    private static final int MAX_SCISSOR_DEPTH = 4;

    private final int[][] scissorStack = new int[4][5];

    private int scissorDepth = 0;

    public ModuleComponent(Module mod, CategoryComponent p, float yPos) {
        this.mod = mod;
        this.categoryComponent = p;
        this.yPos = yPos;
        this.settings = new ArrayList<>();
        this.isOpened = false;
        float collapsedHeight = this.getCollapsedHeight();
        this.smoothingY = collapsedHeight;
        this.animationStartY = collapsedHeight;
        this.animationTargetY = collapsedHeight;
        this.rebuildSettingsList();
    }

    private void rebuildSettingsList() {
        this.settings = new ArrayList<>();
        float y = this.yPos + this.getSettingStartOffset();
        if (this.mod != null && !this.mod.getSettings().isEmpty()) {
            for (Setting v : this.mod.getSettings()) {
                if (v.visible) {
                    if (v instanceof SliderSetting) {
                        SliderSetting n = (SliderSetting) v;
                        SliderComponent s = new SliderComponent(n, this, y);
                        this.settings.add(s);
                        y += 12.0F;
                    } else if (v instanceof ButtonSetting) {
                        ButtonSetting b = (ButtonSetting) v;
                        ButtonComponent c = new ButtonComponent(this.mod, b, this, y);
                        this.settings.add(c);
                        y += 12.0F;
                    } else if (v instanceof DescriptionSetting) {
                        DescriptionSetting d = (DescriptionSetting) v;
                        DescriptionComponent m = new DescriptionComponent(d, this, y);
                        this.settings.add(m);
                        y += 12.0F;
                    } else if (v instanceof KeySetting) {
                        KeySetting setting = (KeySetting) v;
                        BindComponent keyComponent = new BindComponent(this, setting, y);
                        this.settings.add(keyComponent);
                        y += 12.0F;
                    } else if (v instanceof GroupSetting) {
                        GroupSetting b = (GroupSetting) v;
                        GroupComponent c = new GroupComponent(b, this, y);
                        this.settings.add(c);
                        y += 12.0F;
                    } else if (v instanceof ColorSetting) {
                        ColorSetting cs = (ColorSetting) v;
                        ColorComponent cc = new ColorComponent(cs, this, y);
                        this.settings.add(cc);
                        y += 12.0F;
                    } else if (v instanceof PotionListSetting) {
                        PotionListSetting pls = (PotionListSetting) v;
                        PotionSearchComponent psc = new PotionSearchComponent(pls, this, y);
                        this.settings.add(psc);
                        y += 12.0F;
                    } else if (v instanceof InventoryItemListSetting) {
                        InventoryItemListSetting iils = (InventoryItemListSetting) v;
                        InventoryItemSearchComponent iisc = new InventoryItemSearchComponent(iils, this, y);
                        this.settings.add(iisc);
                        y += 12.0F;
                    } else if (v instanceof ItemListSetting) {
                        ItemListSetting ils = (ItemListSetting) v;
                        ItemSearchComponent isc = new ItemSearchComponent(ils, this, y);
                        this.settings.add(isc);
                        y += 12.0F;
                    } else if (v instanceof PlayerListSetting) {
                        PlayerListSetting pls = (PlayerListSetting) v;
                        PlayerListComponent plc = new PlayerListComponent(pls, this, y);
                        this.settings.add(plc);
                        y += plc.getHeightF();
                    } else if (v instanceof StringListSetting) {
                        StringListSetting sls = (StringListSetting) v;
                        StringListComponent slc = new StringListComponent(sls, this, y);
                        this.settings.add(slc);
                        y += slc.getHeightF();
                    } else if (v instanceof BlockListSetting) {
                        BlockListSetting bls = (BlockListSetting) v;
                        BlockSearchComponent bsc = new BlockSearchComponent(bls, this, y);
                        this.settings.add(bsc);
                        y += 12.0F;
                    } else if (v instanceof TextSetting) {
                        TextSetting ts = (TextSetting) v;
                        TextFieldComponent tfc = new TextFieldComponent(ts, this, y);
                        this.settings.add(tfc);
                        y += tfc.getHeightF();
                    }
                }
            }
        }
        this.settings.add(new BindComponent(this, y));
        this.rebuildGroupOwnershipCache();
        this.normalizeGroupedSettingsOrder();
    }

    private void normalizeGroupedSettingsOrder() {
        ArrayList<Component> orderedSettings = new ArrayList<>();
        for (Component component : this.settings) {
            if (component instanceof GroupComponent) {
                GroupComponent group = (GroupComponent) component;
                orderedSettings.add(group);
                for (Component candidate : this.settings) {
                    if (this.getOwningGroup(candidate) == group) {
                        orderedSettings.add(candidate);
                    }
                }
            } else if (this.getOwningGroup(component) == null) {
                orderedSettings.add(component);
            }
        }
        this.settings = orderedSettings;
    }

    public void reloadSettings() {
        boolean wasOpened = this.isOpened;
        Map<SliderSetting, Boolean> sliderHeldStates = new HashMap<>();
        Map<ColorSetting, Boolean> colorExpandedStates = new HashMap<>();
        for (Component component : this.settings) {
            if (component instanceof SliderComponent) {
                SliderComponent sliderComponent = (SliderComponent) component;
                sliderHeldStates.put(sliderComponent.sliderSetting, sliderComponent.heldDown);
            } else if (component instanceof ColorComponent) {
                ColorComponent colorComponent = (ColorComponent) component;
                colorExpandedStates.put(colorComponent.colorSetting, colorComponent.expanded);
            }
        }
        this.rebuildSettingsList();
        for (Component component2 : this.settings) {
            if (component2 instanceof SliderComponent) {
                SliderComponent sliderComponent = (SliderComponent) component2;
                Boolean wasHeldDown = sliderHeldStates.get(sliderComponent.sliderSetting);
                if (wasHeldDown != null) {
                    sliderComponent.heldDown = wasHeldDown;
                }
            } else if (component2 instanceof ColorComponent) {
                ColorComponent colorComponent = (ColorComponent) component2;
                Boolean wasExpanded = colorExpandedStates.get(colorComponent.colorSetting);
                if (wasExpanded != null) {
                    colorComponent.restoreExpandedState(wasExpanded);
                }
            }
        }
        this.restoreOpenState(wasOpened);
        this.updateSettingPositions();
    }

    public void restoreOpenState(boolean opened) {
        this.isOpened = opened;
        this.smoothTimer = null;
        float height = this.isOpened ? this.getHeightF() : this.getCollapsedHeight();
        this.smoothingY = height;
        this.animationStartY = height;
        this.animationTargetY = height;
    }

    public void updateAnimationState() {
        if (this.smoothTimer != null) {
            if (System.currentTimeMillis() - this.smoothTimer.last >= 280L) {
                this.smoothTimer = null;
                this.smoothingY = this.animationTargetY;
                this.animationStartY = this.animationTargetY;
            } else {
                this.smoothingY = this.smoothTimer.getValueFloat(this.animationStartY, this.animationTargetY, 1);
                if (this.smoothingY == this.animationTargetY) {
                    this.smoothTimer = null;
                    this.animationStartY = this.animationTargetY;
                }
            }
        }
    }

    @Override
    public void updateHeight(float newY) {
        this.yPos = newY;
        float y = this.yPos + this.getCollapsedHeight();
        int intValue = 0;
        while (intValue < this.settings.size()) {
            Component co = this.settings.get(intValue);
            if (!this.isVisibleBase(co)) {
                intValue++;
            } else if (co instanceof GroupComponent) {
                GroupComponent group = (GroupComponent) co;
                float progress = group.getAnimationProgress();
                co.updateHeight(y);
                y += this.getBaseComponentHeightF(co);
                intValue++;
                float childY = y;
                float totalChildrenFullHeight = 0.0F;
                while (true) {
                    if (intValue < this.settings.size()) {
                        Component child = this.settings.get(intValue);
                        if (!this.isVisibleBase(child)) {
                            intValue++;
                            continue;
                        }
                        if (this.getOwningGroup(child) == group) {
                            child.updateHeight(childY);
                            float baseH = this.getBaseComponentHeightF(child);
                            childY += baseH;
                            totalChildrenFullHeight += baseH;
                            if (child instanceof SliderComponent) {
                                ((SliderComponent) child).xOffset = 6.0F;
                            } else if (child instanceof ButtonComponent) {
                                ((ButtonComponent) child).xOffset = 6.0F;
                            } else if (child instanceof BindComponent && ((BindComponent) child).keySetting != null) {
                                ((BindComponent) child).xOffset = 6.0F;
                            } else if (child instanceof ColorComponent) {
                                ((ColorComponent) child).xOffset = 6.0F;
                            } else if (child instanceof AbstractTextInputComponent) {
                                ((AbstractTextInputComponent) child).setXOffset(6.0F);
                            }
                            intValue++;
                            continue;
                        }
                    }
                    y = y + this.getBaseComponentHeightF(group) + totalChildrenFullHeight * progress;
                    break;
                }
            } else {
                co.updateHeight(y);
                GroupComponent group = this.getOwningGroup(co);
                float indent = group != null ? 6.0F : 0.0F;
                if (co instanceof SliderComponent) {
                    ((SliderComponent) co).xOffset = indent;
                } else if (co instanceof ButtonComponent) {
                    ((ButtonComponent) co).xOffset = indent;
                } else if (co instanceof BindComponent && ((BindComponent) co).keySetting != null) {
                    ((BindComponent) co).xOffset = indent;
                } else if (co instanceof ColorComponent) {
                    ((ColorComponent) co).xOffset = indent;
                } else if (co instanceof AbstractTextInputComponent) {
                    ((AbstractTextInputComponent) co).setXOffset(indent);
                }
                y += this.getBaseComponentHeightF(co);
                intValue++;
            }
        }
    }

    @Override
    public void render() {
        if (this.hovering || this.hoverTimer != null) {
            double hoverAlpha = this.hovering && this.hoverTimer != null ? this.hoverTimer.getValueFloat(0.0F, 120.0F, 1) : (this.hoverTimer != null && !this.hovering ? 120.0F - this.hoverTimer.getValueFloat(0.0F, 120.0F, 1) : 120.0);
            if (hoverAlpha == 0.0) {
                this.hoverTimer = null;
            }
            RenderUtils.drawRoundedRectangle(this.categoryComponent.getX(), this.categoryComponent.getY() + this.yPos, this.categoryComponent.getX() + this.categoryComponent.getWidth(), this.categoryComponent.getY() + 16.0F + this.yPos, 8.0F, Utils.mergeAlpha(HOVER_COLOR, (int) hoverAlpha));
        }
        int button_rgb = this.mod.isEnabled() ? Gui.enabledColor.getRGB() : Gui.disabledColor.getRGB();
        if (this.mod.script != null && this.mod.script.error) {
            button_rgb = Gui.INVALID_COLOR;
        }
        if (this.mod.moduleCategory() == Module.category.profiles && !(this.mod instanceof Manager) && !((ProfileModule) this.mod).saved && Raven.currentProfile != null && Raven.currentProfile.getModule() == this.mod) {
            button_rgb = Gui.UNSAVED_COLOR;
        }
        boolean scissorRequired = this.smoothTimer != null;
        RavenFontRenderer titleRenderer = Gui.getClickGuiHeaderFontRenderer();
        float textX = this.categoryComponent.getX() + this.categoryComponent.getWidth() / 2.0F - titleRenderer.getStringWidth(this.mod.getName()) / 2.0F;
        float textY = this.categoryComponent.getY() + this.yPos + 4.0F;
        titleRenderer.drawString(this.mod.getName(), textX, textY, button_rgb, true);
        if (scissorRequired) {
            ScaledResolution sr = new ScaledResolution(Minecraft.func_71410_x());
            int scale = sr.func_78325_e();
            double guiScale = ClickGui.getActiveRenderScale();
            float scrollOffset = this.categoryComponent.getModuleY() - this.categoryComponent.getY();
            int scissorX = (int) Math.floor((this.categoryComponent.getX() - 2.0F) * guiScale * scale);
            int scissorY = (int) Math.floor((sr.func_78328_b() - (this.categoryComponent.getY() + this.yPos + this.smoothingY + scrollOffset) * guiScale) * scale);
            int scissorW = (int) Math.ceil((this.categoryComponent.getWidth() + 4.0F) * guiScale * scale);
            int scissorH = (int) Math.ceil(this.smoothingY * guiScale * scale);
            this.pushScissor(scissorX, scissorY, scissorW, scissorH);
        }
        if (this.isOpened || this.smoothTimer != null) {
            this.renderSettingsWithGroupScissorReveal();
        }
        if (scissorRequired) {
            this.popScissor();
        }
    }

    @Override
    public float getHeightF() {
        if (this.smoothTimer != null) {
            return this.smoothingY;
        } else if (!this.isOpened) {
            return this.getCollapsedHeight();
        } else {
            float h = this.getCollapsedHeight();
            for (Component c : this.settings) {
                h += this.getAnimatedComponentHeightF(c);
            }
            return h;
        }
    }

    @Override
    public int getHeight() {
        return Math.round(this.getHeightF());
    }

    public void onSliderChange() {
        for (Component c : this.settings) {
            if (c instanceof SliderComponent) {
                ((SliderComponent) c).onSliderChange();
            }
        }
    }

    public float getScrollExtentHeightF() {
        if (this.isOpened || this.smoothTimer != null && this.animationTargetY > 16.0F) {
            float h = this.getCollapsedHeight();
            for (Component c : this.settings) {
                if (this.isVisibleBase(c)) {
                    GroupComponent group = this.getOwningGroup(c);
                    float progress = group != null ? group.getAnimationProgress() : 1.0F;
                    float effectiveProgress = group != null && group.opened ? Math.max(progress, 1.0F) : progress;
                    h += this.getBaseComponentHeightF(c) * effectiveProgress;
                }
            }
            return h;
        } else {
            return this.getHeightF();
        }
    }

    @Override
    public void drawScreen(int x, int y) {
        for (Component c : this.settings) {
            c.drawScreen(x, y);
        }
        if (this.overModuleName(x, y) && this.categoryComponent.opened) {
            this.hovering = true;
            if (this.hoverTimer == null) {
                (this.hoverTimer = new Timer(75.0F)).start();
                this.hoverStarted = true;
            }
        } else {
            if (this.hovering && this.hoverStarted) {
                (this.hoverTimer = new Timer(75.0F)).start();
            }
            this.hoverStarted = false;
            this.hovering = false;
        }
    }

    @Override
    public boolean onClick(int x, int y, int mouse) {
        if (this.overModuleName(x, y) && mouse == 0 && this.mod.canBeEnabled()) {
            this.mod.toggle();
            if (this.mod.moduleCategory() != Module.category.profiles && Raven.currentProfile != null) {
                Raven.currentProfile.getModule().saved = false;
            }
            return true;
        } else if (this.overModuleName(x, y) && mouse == 1) {
            float currentHeight = this.smoothTimer != null ? this.smoothingY : (this.isOpened ? this.getHeightF() : 16.0F);
            this.animationStartY = currentHeight;
            this.isOpened = !this.isOpened;
            float targetHeight;
            if (this.isOpened) {
                float h = this.getCollapsedHeight();
                for (Component c : this.settings) {
                    h += this.getAnimatedComponentHeightF(c);
                }
                targetHeight = h;
            } else {
                targetHeight = this.getCollapsedHeight();
            }
            this.animationTargetY = targetHeight;
            (this.smoothTimer = new Timer(250.0F)).start();
            return true;
        } else {
            for (Component settingComponent : this.settings) {
                if (settingComponent.onClick(x, y, mouse)) {
                    return true;
                }
            }
            return false;
        }
    }

    @Override
    public void mouseReleased(int x, int y, int m) {
        for (Component c : this.settings) {
            c.mouseReleased(x, y, m);
        }
    }

    @Override
    public void keyTyped(char t, int k) {
        for (Component c : this.settings) {
            c.keyTyped(t, k);
        }
    }

    @Override
    public void onScroll(int scroll) {
        for (Component component : this.settings) {
            component.onScroll(scroll);
        }
    }

    @Override
    public void onGuiClosed() {
        for (Component c : this.settings) {
            c.onGuiClosed();
        }
        this.smoothTimer = null;
        this.hoverTimer = null;
        float finalHeight = this.isOpened ? this.getHeightF() : this.getCollapsedHeight();
        this.smoothingY = finalHeight;
        this.animationStartY = finalHeight;
        this.animationTargetY = finalHeight;
    }

    public boolean overModuleName(int x, int y) {
        return x > this.categoryComponent.getX() && x < this.categoryComponent.getX() + this.categoryComponent.getWidth() && y > this.categoryComponent.getModuleY() + this.yPos && y < this.categoryComponent.getModuleY() + 16.0F + this.yPos;
    }

    public void updateSettingPositions() {
        this.categoryComponent.updateHeight();
    }

    public boolean isVisible(Component component) {
        if (!this.isVisibleBase(component)) {
            return false;
        } else {
            GroupComponent group = this.getOwningGroup(component);
            return group == null ? true : group.getAnimationProgress() > 0.0F;
        }
    }

    private GroupComponent getOwningGroup(Component component) {
        return this.owningGroups.get(component);
    }

    private String getGroupName(Component component) {
        if (component instanceof SliderComponent && ((SliderComponent) component).sliderSetting.groupSetting != null) {
            return ((SliderComponent) component).sliderSetting.groupSetting.getName();
        } else if (component instanceof ButtonComponent && ((ButtonComponent) component).buttonSetting.group != null) {
            return ((ButtonComponent) component).buttonSetting.group.getName();
        } else if (component instanceof BindComponent && ((BindComponent) component).keySetting != null && ((BindComponent) component).keySetting.group != null) {
            return ((BindComponent) component).keySetting.group.getName();
        } else if (component instanceof ColorComponent && ((ColorComponent) component).colorSetting.groupSetting != null) {
            return ((ColorComponent) component).colorSetting.groupSetting.getName();
        } else {
            return component instanceof AbstractTextInputComponent ? ((AbstractTextInputComponent) component).getGroupName() : "";
        }
    }

    private void rebuildGroupOwnershipCache() {
        this.owningGroups.clear();
        this.groupsByName.clear();
        for (Component component : this.settings) {
            if (component instanceof GroupComponent) {
                GroupComponent groupComponent = (GroupComponent) component;
                this.groupsByName.put(groupComponent.setting.getName(), groupComponent);
            }
        }
        for (Component component3 : this.settings) {
            String groupName = this.getGroupName(component3);
            if (!groupName.isEmpty()) {
                GroupComponent groupComponent = this.groupsByName.get(groupName);
                if (groupComponent != null) {
                    this.owningGroups.put(component3, groupComponent);
                }
            }
        }
    }

    private float getBaseComponentHeightF(Component component) {
        if (component instanceof SliderComponent) {
            return 16.0F;
        } else if (component instanceof ColorComponent) {
            ColorComponent cc = (ColorComponent) component;
            float progress = cc.getAnimationProgress();
            return 12.0F + (cc.getExpandedHeight() - 12.0F) * progress;
        } else {
            return !(component instanceof AbstractSearchListComponent) && !(component instanceof TextFieldComponent) && !(component instanceof PlayerListComponent) && !(component instanceof StringListComponent) ? 12.0F : component.getHeightF();
        }
    }

    private float getAnimatedComponentHeightF(Component component) {
        if (!this.isVisibleBase(component)) {
            return 0.0F;
        } else {
            float base = this.getBaseComponentHeightF(component);
            GroupComponent group = this.getOwningGroup(component);
            float progress = group != null ? group.getAnimationProgress() : 1.0F;
            return base * progress;
        }
    }

    private void renderSettingsWithGroupScissorReveal() {
        int i = 0;
        while (i < this.settings.size()) {
            Component c = this.settings.get(i);
            if (!this.isVisibleBase(c)) {
                i++;
            } else if (c instanceof GroupComponent) {
                ((GroupComponent) c).render();
                i++;
                while (i < this.settings.size()) {
                    Component child = this.settings.get(i);
                    if (!this.isVisibleBase(child)) {
                        i++;
                    } else {
                        if (this.getOwningGroup(child) != c) {
                            break;
                        }
                        i++;
                    }
                }
            } else {
                c.render();
                i++;
            }
        }
        i = 0;
        while (i < this.settings.size()) {
            Component c = this.settings.get(i);
            if (!this.isVisibleBase(c)) {
                i++;
            } else if (!(c instanceof GroupComponent)) {
                i++;
            } else {
                GroupComponent group = (GroupComponent) c;
                i++;
                float progress = group.getAnimationProgress();
                float groupContentTop = this.categoryComponent.getModuleY() + group.getOffset() + this.getBaseComponentHeightF(group);
                float groupContentHeight = 0.0F;
                int j = i;
                while (j < this.settings.size()) {
                    Component child = this.settings.get(j);
                    if (!this.isVisibleBase(child)) {
                        j++;
                    } else {
                        if (this.getOwningGroup(child) != group) {
                            break;
                        }
                        groupContentHeight += this.getBaseComponentHeightF(child);
                        j++;
                    }
                }
                if (progress > 0.0F && groupContentHeight > 0.0F) {
                    float revealHeight = groupContentHeight * progress;
                    ScaledResolution sr = new ScaledResolution(Minecraft.func_71410_x());
                    int sf = sr.func_78325_e();
                    double guiScale = ClickGui.getActiveRenderScale();
                    double screenH = sr.func_78328_b();
                    float compLeft = this.categoryComponent.getX();
                    float compWidth = this.categoryComponent.getWidth() + 4.0F;
                    int newLeft = (int) Math.floor(compLeft * guiScale * sf);
                    int newRight = (int) Math.ceil((compLeft + compWidth) * guiScale * sf);
                    int newW = Math.max(0, newRight - newLeft);
                    int newGlBottom = (int) Math.floor((screenH - (groupContentTop + revealHeight) * guiScale) * sf);
                    int newGlTop = (int) Math.ceil((screenH - groupContentTop * guiScale) * sf);
                    int newH = Math.max(0, newGlTop - newGlBottom);
                    this.pushScissor(newLeft, newGlBottom, newW, newH);
                    for (; i < j; i++) {
                        Component child = this.settings.get(i);
                        if (this.isVisibleBase(child) && this.getOwningGroup(child) == group) {
                            child.render();
                        }
                    }
                    this.popScissor();
                } else {
                    i = j;
                }
            }
        }
    }

    private void pushScissor(int x, int y, int w, int h) {
        boolean wasEnabled = GL11.glIsEnabled(3089);
        int[] saved = this.scissorStack[this.scissorDepth++];
        if (wasEnabled) {
            ((Buffer) SCISSOR_BOX).clear();
            GL11.glGetInteger(3088, SCISSOR_BOX);
            saved[0] = 1;
            saved[1] = SCISSOR_BOX.get(0);
            saved[2] = SCISSOR_BOX.get(1);
            saved[3] = SCISSOR_BOX.get(2);
            saved[4] = SCISSOR_BOX.get(3);
            int ix = Math.max(saved[1], x);
            int iy = Math.max(saved[2], y);
            int iw = Math.max(0, Math.min(saved[1] + saved[3], x + w) - ix);
            int ih = Math.max(0, Math.min(saved[2] + saved[4], y + h) - iy);
            GL11.glScissor(ix, iy, iw, ih);
        } else {
            saved[0] = 0;
            GL11.glEnable(3089);
            GL11.glScissor(x, y, w, h);
        }
    }

    private void popScissor() {
        int[] saved = this.scissorStack[--this.scissorDepth];
        if (saved[0] == 1) {
            GL11.glScissor(saved[1], saved[2], saved[3], saved[4]);
        } else {
            GL11.glDisable(3089);
        }
    }

    private boolean isVisibleBase(Component component) {
        return component.isBaseVisible();
    }

    private float getCollapsedHeight() {
        return 16.0F;
    }

    private float getSettingStartOffset() {
        return 12.0F;
    }
}
