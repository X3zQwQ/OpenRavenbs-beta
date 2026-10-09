package keystrokesmod.clickgui.components.impl;

import com.mojang.authlib.GameProfile;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import keystrokesmod.clickgui.animation.ScrollOffsetAnimation;
import keystrokesmod.module.impl.client.Gui;
import keystrokesmod.module.setting.impl.PlayerListSetting;
import keystrokesmod.utility.PlayerRelationsManager;
import keystrokesmod.utility.PlayerSkinCache;
import keystrokesmod.utility.RenderUtils;
import keystrokesmod.utility.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

public class PlayerListComponent extends AbstractTextInputComponent {

    private static final String CLOSE_ICON_PATH = "/assets/keystrokesmod/textures/gui/close.png";

    private static final float SELECTED_LIST_GAP = 4.0F;

    private static final int MAX_VISIBLE_SELECTED = 7;

    private static final int CLOSE_SIZE = 6;

    private static final float CLOSE_PAD = 3.0F;

    private static final float HEAD_SIZE = 8.0F;

    private static final float LIST_ROW_TEXT_SCALE = 0.56F;

    private static final float LIST_ROW_TEXT_Y_OFFSET = 0.56F;

    private static final float LIST_ROW_VISUAL_HEIGHT = 11.0F;

    private final PlayerListSetting setting;

    private final ScrollOffsetAnimation selectedScrollAnim = new ScrollOffsetAnimation(200L);

    private float lastMouseX;

    private float lastMouseY;

    public PlayerListComponent(PlayerListSetting setting, ModuleComponent moduleComponent, float o) {
        super(moduleComponent, o, setting.getPlaceholder(), setting.getMaxLength());
        this.setting = setting;
    }

    @Override
    public void render() {
        AbstractTextInputComponent.Layout layout = this.layout(false);
        this.renderLabel(layout, this.setting.getName());
        this.renderTextField(layout);
        this.renderSelectedEntries(layout);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY) {
        super.drawScreen(mouseX, mouseY);
        this.lastMouseX = mouseX;
        this.lastMouseY = mouseY;
        this.clampSelectedScroll();
    }

    @Override
    public boolean onClick(int mouseX, int mouseY, int button) {
        if (this.moduleComponent.isOpened && this.moduleComponent.isVisible(this)) {
            AbstractTextInputComponent.Layout layout = this.layout(true);
            if (button == 0 && this.isTextFieldClicked(mouseX, mouseY, layout)) {
                this.setTextFieldFocused(true);
                return true;
            } else if (button == 0 && this.handleSelectedEntryClick(mouseX, mouseY, layout)) {
                return true;
            } else {
                if (this.isTextFieldFocused()) {
                    this.getTextField().setText("");
                    this.setTextFieldFocused(false);
                }
                return false;
            }
        } else {
            return false;
        }
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        if (this.moduleComponent.isOpened && this.isTextFieldFocused()) {
            if (keyCode == 1) {
                this.getTextField().setText("");
                this.setTextFieldFocused(false);
            } else if (keyCode != 28 && keyCode != 156) {
                this.getTextField().textboxKeyTyped(typedChar, keyCode);
            } else {
                this.submitText();
                this.setTextFieldFocused(false);
            }
        }
    }

    @Override
    public void onScroll(int scroll) {
        if (this.moduleComponent.isOpened && this.moduleComponent.isVisible(this)) {
            if (this.capturesCategoryScroll(this.lastMouseX, this.lastMouseY)) {
                float scrollSpeed = (float) Gui.scrollSpeed.getInput();
                float delta = scrollSpeed * (scroll / 120.0F);
                if (delta != 0.0F) {
                    this.selectedScrollAnim.extend(-delta);
                }
                this.clampSelectedScroll();
            }
        }
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        this.getTextField().setText("");
        this.selectedScrollAnim.reset(0.0F);
    }

    @Override
    public float getHeightF() {
        int count = this.setting.getEntries().size();
        float selectedHeight = count == 0 ? 0.0F : 4.0F + Math.min(7, count) * 12.0F;
        return 24.0F + selectedHeight;
    }

    @Override
    public boolean isBaseVisible() {
        return this.setting.visible;
    }

    @Override
    public String getGroupName() {
        return this.setting.group != null ? this.setting.group.getName() : "";
    }

    public boolean capturesCategoryScroll(float mouseX, float mouseY) {
        return this.setting.getEntries().size() > 7 && this.isMouseOverSelectedList(mouseX, mouseY);
    }

    @Override
    public boolean containsClick(int mouseX, int mouseY) {
        AbstractTextInputComponent.Layout layout = this.layout(true);
        return this.isTextFieldClicked(mouseX, mouseY, layout) || this.isMouseOverSelectedList(mouseX, mouseY);
    }

    public void onExternalDataChanged() {
        this.clampSelectedScroll();
    }

    private void submitText() {
        String typedName = this.getTextField().getText();
        if (typedName != null && !typedName.trim().isEmpty()) {
            if (this.setting.addPlayer(typedName)) {
                this.getTextField().setText("");
            }
            this.moduleComponent.updateSettingPositions();
            this.clampSelectedScroll();
        }
    }

    private void renderSelectedEntries(AbstractTextInputComponent.Layout layout) {
        List<PlayerRelationsManager.PlayerEntry> entries = this.setting.getEntries();
        if (!entries.isEmpty()) {
            float selectedTop = this.getSelectedTop(layout);
            float selectedHeight = this.getSelectedVisibleHeight(entries.size());
            float scrollOffset = this.moduleComponent.categoryComponent.getModuleY() - layout.cy;
            RenderUtils.scissorPushGui(layout.left, selectedTop + scrollOffset, layout.right - layout.left, selectedHeight);
            float value = this.selectedScrollAnim.getValue();
            int firstRow = (int) (value / 12.0F);
            int end = Math.min(firstRow + 7 + 1, entries.size());
            Map<String, NetworkPlayerInfo> playerInfoMap = this.getPlayerInfoMap();
            for (int i = firstRow; i < end; i++) {
                PlayerRelationsManager.PlayerEntry entry = entries.get(i);
                float rowTop = selectedTop - value + i * 12.0F;
                int bg = i % 2 == 0 ? -15066582 : -14803410;
                this.renderEntryRow(entry, playerInfoMap.get(entry.getKey()), layout.left, layout.right, rowTop, bg);
            }
            RenderUtils.scissorPop();
        }
    }

    private void renderEntryRow(PlayerRelationsManager.PlayerEntry entry, NetworkPlayerInfo playerInfo, float left, float right, float rowTop, int bgColor) {
        RenderUtils.drawRect(left, rowTop, right, rowTop + 12.0F - 1.0F, bgColor);
        this.renderPlayerHead(entry, playerInfo, left + 2.0F, rowTop + 1.5F);
        drawScaledTextNoShadow(entry.getDisplayName(), left + 13.0F, centeredScaledTextY(rowTop, 11.0F, 0.56F) + 0.56F, -3355444);
        this.renderCloseIcon(right, rowTop);
    }

    private boolean handleSelectedEntryClick(int mouseX, int mouseY, AbstractTextInputComponent.Layout layout) {
        List<PlayerRelationsManager.PlayerEntry> entries = this.setting.getEntries();
        float value2 = this.selectedScrollAnim.getValue();
        for (int i = 0; i < entries.size(); i++) {
            float rowTop = this.getSelectedTop(layout) - value2 + i * 12.0F;
            if (this.isOverClose(mouseX, mouseY, rowTop, layout.right)) {
                this.setting.removePlayer(entries.get(i).getKey());
                this.moduleComponent.updateSettingPositions();
                this.clampSelectedScroll();
                return true;
            }
        }
        return false;
    }

    private void renderPlayerHead(PlayerRelationsManager.PlayerEntry entry, NetworkPlayerInfo playerInfo, float x, float y) {
        ResourceLocation skin = this.getSkin(entry, playerInfo);
        if (skin != null) {
            boolean depthEnabled = GL11.glIsEnabled(2929);
            boolean blendEnabled = GL11.glIsEnabled(3042);
            boolean depthMask = GL11.glGetBoolean(2930);
            try {
                RenderUtils.prepareGuiTextureRenderState();
                Minecraft.func_71410_x().func_110434_K().func_110577_a(skin);
                GlStateManager.func_179131_c(1.0F, 1.0F, 1.0F, 1.0F);
                net.minecraft.client.gui.Gui.func_152125_a((int) x, (int) y, 8.0F, 8.0F, 8, 8, 8, 8, 64.0F, 64.0F);
                net.minecraft.client.gui.Gui.func_152125_a((int) x, (int) y, 40.0F, 8.0F, 8, 8, 8, 8, 64.0F, 64.0F);
            } finally {
                RenderUtils.restoreGuiRenderState(depthEnabled, blendEnabled, depthMask);
            }
        }
    }

    private ResourceLocation getSkin(PlayerRelationsManager.PlayerEntry entry, NetworkPlayerInfo playerInfo) {
        return PlayerSkinCache.getSkin(entry.getDisplayName(), playerInfo);
    }

    private Map<String, NetworkPlayerInfo> getPlayerInfoMap() {
        Map<String, NetworkPlayerInfo> playerInfoMap = new HashMap<>();
        Minecraft minecraft = Minecraft.func_71410_x();
        if (minecraft.func_147114_u() == null) {
            return playerInfoMap;
        } else {
            for (NetworkPlayerInfo playerInfo : minecraft.func_147114_u().func_175106_d()) {
                if (playerInfo != null) {
                    GameProfile profile = playerInfo.func_178845_a();
                    if (profile != null && profile.getName() != null) {
                        playerInfoMap.put(profile.getName().toLowerCase(), playerInfo);
                    }
                }
            }
            return playerInfoMap;
        }
    }

    private void clampSelectedScroll() {
        int count = this.setting.getEntries().size();
        float floatValue = Math.max(0.0F, (count - 7) * 12.0F);
        this.selectedScrollAnim.clampTarget(0.0F, floatValue);
        if (this.selectedScrollAnim.getValue() > floatValue) {
            this.selectedScrollAnim.reset(floatValue);
        }
    }

    private boolean isMouseOverSelectedList(float mouseX, float mouseY) {
        List<PlayerRelationsManager.PlayerEntry> entries = this.setting.getEntries();
        if (entries.isEmpty()) {
            return false;
        } else {
            AbstractTextInputComponent.Layout layout = this.layout(true);
            float selectedTop = this.getSelectedTop(layout);
            float selectedHeight = this.getSelectedVisibleHeight(entries.size());
            return mouseX >= layout.left && mouseX <= layout.right && mouseY >= selectedTop && mouseY < selectedTop + selectedHeight;
        }
    }

    private float getSelectedTop(AbstractTextInputComponent.Layout layout) {
        return layout.contentTop + 4.0F;
    }

    private float getSelectedVisibleHeight(int count) {
        return Math.min(7, count) * 12.0F;
    }

    private void renderCloseIcon(float right, float rowTop) {
        ResourceLocation close = RenderUtils.getIcon("/assets/keystrokesmod/textures/gui/close.png");
        if (close != null) {
            float closeX = right - 6.0F - 3.0F;
            float closeY = rowTop + 2.5F;
            int closeColor = Theme.getGradient(Theme.hiddenBind[0], Theme.hiddenBind[1], 0.0);
            RenderUtils.drawIcon(close, closeX, closeY, 6, closeColor);
        }
    }

    private boolean isOverClose(float mouseX, float mouseY, float rowTop, float right) {
        float closeX = right - 6.0F - 3.0F;
        float closeY = rowTop + 2.5F;
        return mouseX >= closeX && mouseX <= closeX + 6.0F && mouseY >= closeY && mouseY <= closeY + 6.0F;
    }

    private static void drawScaledTextNoShadow(String text, float x, float y, int color) {
        drawScaledText(text, x, y, color, 0.56F);
    }
}
