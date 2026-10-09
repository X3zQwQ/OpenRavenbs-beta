package keystrokesmod.clickgui.components.impl;

import keystrokesmod.Raven;
import keystrokesmod.clickgui.components.Component;
import keystrokesmod.module.Module;
import keystrokesmod.module.impl.client.Gui;
import keystrokesmod.module.setting.impl.KeySetting;
import keystrokesmod.utility.RenderUtils;
import keystrokesmod.utility.Theme;
import keystrokesmod.utility.font.RavenFontRenderer;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

public class BindComponent extends Component {

    private static final String EYE_ICON_PATH = "/assets/keystrokesmod/textures/gui/eye.png";

    private static final String EYE_OFF_ICON_PATH = "/assets/keystrokesmod/textures/gui/eye_off.png";

    private static final int EYE_ICON_PADDING = 2;

    public boolean isBinding;

    public ModuleComponent moduleComponent;

    public float o;

    public float x;

    private float y;

    public KeySetting keySetting;

    public float xOffset;

    public BindComponent(ModuleComponent moduleComponent, float o) {
        this.moduleComponent = moduleComponent;
        this.x = moduleComponent.categoryComponent.getX() + moduleComponent.categoryComponent.getWidth();
        this.y = moduleComponent.categoryComponent.getY() + moduleComponent.yPos;
        this.o = o;
    }

    public BindComponent(ModuleComponent moduleComponent, KeySetting keySetting, float o) {
        this.moduleComponent = moduleComponent;
        this.x = moduleComponent.categoryComponent.getX() + moduleComponent.categoryComponent.getWidth();
        this.y = moduleComponent.categoryComponent.getY() + moduleComponent.yPos;
        this.keySetting = keySetting;
        this.o = o;
    }

    @Override
    public void updateHeight(float n) {
        this.o = n;
    }

    @Override
    public float getOffset() {
        return this.o;
    }

    @Override
    public boolean isBaseVisible() {
        return this.keySetting == null || this.keySetting.visible;
    }

    @Override
    public void render() {
        RavenFontRenderer renderer = Gui.getClickGuiSettingFontRenderer();
        GL11.glPushMatrix();
        GL11.glScaled(0.5, 0.5, 0.5);
        if (this.keySetting == null) {
            this.drawString(renderer, !this.moduleComponent.mod.canBeEnabled() && this.moduleComponent.mod.script == null ? "Module cannot be bound." : (this.isBinding ? "Press a key..." : "Current bind: '§e" + this.getKeyAsStr(false) + "§r'"));
        } else {
            renderer.drawString(this.isBinding ? "Press a key..." : this.keySetting.getName() + ": '§e" + this.getKeyAsStr(true) + "§r'", (this.moduleComponent.categoryComponent.getX() + 4.0F) * 2.0F + this.xOffset, (this.moduleComponent.categoryComponent.getY() + this.o + (this.keySetting == null ? 3 : 4)) * 2.0F, Theme.getGradient(Theme.descriptor[0], Theme.descriptor[1], 0.0), true);
        }
        GL11.glPopMatrix();
        if (this.keySetting == null && this.moduleComponent.mod.canBeEnabled() && this.moduleComponent.mod.moduleCategory() != Module.category.profiles) {
            int iconSize = this.getEyeIconSize();
            float iconX = this.getEyeIconX(iconSize);
            float textHeight = Gui.getClickGuiSettingFontRenderer().getFontHeight() * 0.5F;
            float iconY = this.getRenderTextY() + (textHeight - iconSize) / 2.0F;
            int themeColor = !this.moduleComponent.mod.hidden ? Theme.getGradient(Theme.descriptor[0], Theme.descriptor[1], 0.0) : Theme.getGradient(Theme.hiddenBind[0], Theme.hiddenBind[1], 0.0);
            String iconPath = this.moduleComponent.mod.isHidden() ? "/assets/keystrokesmod/textures/gui/eye_off.png" : "/assets/keystrokesmod/textures/gui/eye.png";
            RenderUtils.drawIcon(RenderUtils.getIcon(iconPath), iconX, iconY, iconSize, themeColor);
        }
    }

    @Override
    public void drawScreen(int x, int y) {
        this.y = this.moduleComponent.categoryComponent.getModuleY() + this.o;
        this.x = this.moduleComponent.categoryComponent.getX();
    }

    @Override
    public boolean onClick(int x, int y, int button) {
        if (this.overSetting(x, y) && this.moduleComponent.isOpened && this.moduleComponent.isVisible(this)) {
            if (button == 0 && this.moduleComponent.mod.canBeEnabled() && this.moduleComponent.mod.moduleCategory() != Module.category.profiles && this.overEyeIcon(x, y)) {
                this.moduleComponent.mod.setHidden(!this.moduleComponent.mod.isHidden());
                if (Raven.currentProfile != null) {
                    Raven.currentProfile.getModule().saved = false;
                }
                return true;
            } else if (this.moduleComponent.mod.canBeEnabled() && button == 0 && this.overBindText(x, y)) {
                this.isBinding = !this.isBinding;
                return true;
            } else if (this.moduleComponent.mod.canBeEnabled() && button > 1 && this.isBinding) {
                if (this.keySetting != null) {
                    this.keySetting.setKey(button + 1000);
                } else {
                    this.moduleComponent.mod.setBind(button + 1000);
                }
                if (Raven.currentProfile != null) {
                    Raven.currentProfile.getModule().saved = false;
                }
                this.isBinding = false;
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    private boolean overEyeIcon(int x, int y) {
        int iconSize = this.getEyeIconSize();
        float iconX = this.getEyeIconX(iconSize);
        float iconY = this.getEyeIconY(iconSize);
        return x >= iconX && x < iconX + iconSize && y >= iconY && y < iconY + iconSize;
    }

    private float getBindTextX() {
        return this.moduleComponent.categoryComponent.getX() + 4.0F + this.xOffset * 0.5F;
    }

    private float getBindTextY() {
        return this.moduleComponent.categoryComponent.getModuleY() + this.o + (this.keySetting == null ? 3.0F : 4.0F);
    }

    private float getRenderTextY() {
        return this.moduleComponent.categoryComponent.getY() + this.o + (this.keySetting == null ? 3.0F : 4.0F);
    }

    private String getBindDisplayString() {
        if (this.keySetting != null) {
            return this.isBinding ? "Press a key..." : this.keySetting.getName() + ": '§e" + this.getKeyAsStr(true) + "§r'";
        } else {
            return !this.moduleComponent.mod.canBeEnabled() && this.moduleComponent.mod.script == null ? "Module cannot be bound." : (this.isBinding ? "Press a key..." : "Current bind: '§e" + this.getKeyAsStr(false) + "§r'");
        }
    }

    private boolean overBindText(int mouseX, int mouseY) {
        String text = this.getBindDisplayString();
        RavenFontRenderer renderer = Gui.getClickGuiSettingFontRenderer();
        float left = this.getBindTextX();
        float top = this.getBindTextY();
        float width = renderer.getStringWidth(text) * 0.5F;
        float height = renderer.getFontHeight() * 0.5F;
        return mouseX >= left && mouseX < left + width && mouseY >= top && mouseY < top + height;
    }

    private int getEyeIconSize() {
        int fontH = Math.round(Gui.getClickGuiSettingFontRenderer().getFontHeight() * 0.5F);
        return Math.max(6, fontH - 1);
    }

    private float getEyeIconX(int iconSize) {
        return this.moduleComponent.categoryComponent.getX() + this.moduleComponent.categoryComponent.getWidth() - iconSize - 2.0F;
    }

    private float getEyeIconY(int iconSize) {
        float textY = this.getBindTextY();
        float textHeight = Gui.getClickGuiSettingFontRenderer().getFontHeight() * 0.5F;
        return textY + (textHeight - iconSize) / 2.0F;
    }

    @Override
    public void onScroll(int scroll) {
        if (this.isBinding && scroll != 0) {
            if (this.keySetting != null) {
                this.keySetting.setKey(scroll > 0 ? 1069 : 1070);
            } else {
                this.moduleComponent.mod.setBind(scroll > 0 ? 1069 : 1070);
            }
            if (Raven.currentProfile != null) {
                Raven.currentProfile.getModule().saved = false;
            }
            this.isBinding = false;
        }
    }

    @Override
    public void keyTyped(char t, int keybind) {
        if (this.isBinding) {
            if (keybind != 11 && keybind != 1) {
                if (this.keySetting != null) {
                    this.keySetting.setKey(keybind);
                } else {
                    this.moduleComponent.mod.setBind(keybind);
                }
            } else if (this.moduleComponent.mod instanceof Gui) {
                this.moduleComponent.mod.setBind(54);
            } else if (this.keySetting != null) {
                this.keySetting.setKey(0);
            } else {
                this.moduleComponent.mod.setBind(0);
            }
            if (Raven.currentProfile != null) {
                Raven.currentProfile.getModule().saved = false;
            }
            this.isBinding = false;
        }
    }

    public boolean overSetting(int mouseX, int mouseY) {
        float rowX = this.moduleComponent.categoryComponent.getX();
        float rowY = this.moduleComponent.categoryComponent.getModuleY() + this.o;
        float rowW = this.moduleComponent.categoryComponent.getWidth();
        return mouseX > rowX && mouseX < rowX + rowW && mouseY > rowY - 1.0F && mouseY < rowY + 12.0F;
    }

    public String getKeyAsStr(boolean isKey) {
        int key = isKey ? this.keySetting.getKey() : this.moduleComponent.mod.getKeycode();
        return key >= 1000 ? (key != 1069 && key != 1070 ? "M" + (key - 1000) : this.getScroll(key)) : Keyboard.getKeyName(key);
    }

    public String getScroll(int key) {
        if (key == 1069) {
            return "MScrollUp";
        } else {
            return key == 1070 ? "MScrollDown" : "&cERROR";
        }
    }

    @Override
    public float getHeightF() {
        return this.keySetting != null ? 0.0F : 16.0F;
    }

    @Override
    public int getHeight() {
        return Math.round(this.getHeightF());
    }

    private void drawString(RavenFontRenderer renderer, String s) {
        renderer.drawString(s, (this.moduleComponent.categoryComponent.getX() + 4.0F) * 2.0F + this.xOffset, (this.moduleComponent.categoryComponent.getY() + this.o + (this.keySetting == null ? 3 : 4)) * 2.0F, Theme.getGradient(Theme.descriptor[0], Theme.descriptor[1], 0.0), true);
    }

    @Override
    public void onGuiClosed() {
        this.isBinding = false;
    }
}
