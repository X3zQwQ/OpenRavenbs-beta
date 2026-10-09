package keystrokesmod.mixin.impl.render;

import keystrokesmod.Raven;
import keystrokesmod.clickgui.animation.ScrollOffsetAnimation;
import keystrokesmod.module.impl.client.Gui;
import keystrokesmod.utility.RenderUtils;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiTextField;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SideOnly(Side.CLIENT)
@Mixin(GuiChat.class)
public abstract class MixinGuiChat extends MixinGuiScreen {

    private static final int MAX_PREVIEW_ROWS = 6;

    private static final long PREVIEW_SCROLL_DURATION_MS = 200L;

    @Unique
    private String[] raven$commandCompletions = new String[0];

    @Unique
    private int raven$commandCompletionIndex = -1;

    @Unique
    private final ScrollOffsetAnimation raven$previewScrollAnim = new ScrollOffsetAnimation(200L);

    @Unique
    private String raven$previewScrollInput = "";

    @Shadow
    protected GuiTextField field_146415_a;

    @Shadow
    private boolean field_146414_r;

    @Shadow
    public abstract void func_146406_a(String[] stringArray);

    @Inject(method = "keyTyped", at = @At("RETURN"))
    private void updateLength(CallbackInfo callbackInfo) {
        if (Raven.commandManager != null && Raven.commandManager.isCommand(this.field_146415_a.func_146179_b())) {
            this.field_146415_a.func_146203_f(256);
        } else {
            this.field_146415_a.func_146203_f(100);
        }
    }

    @Inject(method = "keyTyped", at = @At("HEAD"))
    private void clearCommandCompletionState(char typedChar, int keyCode, CallbackInfo callbackInfo) {
        if (keyCode != 15) {
            this.raven$resetCommandCompletions();
        }
    }

    @Inject(method = "handleMouseInput", at = @At("HEAD"), cancellable = true)
    private void handleSuggestionScroll(CallbackInfo callbackInfo) {
        if (Raven.commandManager != null) {
            int wheelInput = Mouse.getEventDWheel();
            if (wheelInput != 0) {
                String input = this.field_146415_a.func_146179_b();
                if (!Raven.commandManager.isCommand(input)) {
                    this.raven$resetPreviewScroll();
                } else {
                    String[] suggestions = Raven.commandManager.getPreviewSuggestions(input);
                    int visibleRows = Math.min(6, suggestions.length);
                    if (visibleRows != 0 && suggestions.length > visibleRows) {
                        int mouseX = Mouse.getEventX() * this.field_146294_l / this.field_146297_k.field_71443_c;
                        int mouseY = this.field_146295_m - Mouse.getEventY() * this.field_146295_m / this.field_146297_k.field_71440_d - 1;
                        if (this.raven$isMouseOverPreviewPanel(mouseX, mouseY, suggestions, visibleRows)) {
                            this.raven$syncPreviewScrollState(input, suggestions.length, visibleRows);
                            float scrollSpeed = Gui.scrollSpeed != null ? (float) Gui.scrollSpeed.getInput() : 20.0F;
                            float delta = scrollSpeed * (wheelInput / 120.0F);
                            if (delta != 0.0F) {
                                this.raven$previewScrollAnim.extend(-delta);
                                this.raven$clampPreviewScroll(suggestions.length, visibleRows);
                            }
                            callbackInfo.cancel();
                        }
                    } else {
                        this.raven$syncPreviewScrollState(input, suggestions.length, visibleRows);
                    }
                }
            }
        }
    }

    @Inject(method = "drawScreen", at = @At("RETURN"))
    private void drawLiveCommandSuggestions(int mouseX, int mouseY, float partialTicks, CallbackInfo callbackInfo) {
        if (Raven.commandManager == null) {
            this.raven$resetPreviewScroll();
        } else {
            String input = this.field_146415_a.func_146179_b();
            if (!Raven.commandManager.isCommand(input)) {
                this.raven$resetPreviewScroll();
            } else {
                String[] suggestions = Raven.commandManager.getPreviewSuggestions(input);
                if (suggestions.length == 0) {
                    this.raven$resetPreviewScroll();
                } else {
                    int rows = Math.min(6, suggestions.length);
                    this.raven$syncPreviewScrollState(input, suggestions.length, rows);
                    int fontHeight = this.field_146297_k.field_71466_p.field_78288_b;
                    int rowHeight = fontHeight + 2;
                    int widest = this.field_146297_k.field_71466_p.func_78256_a("Suggestions");
                    for (String suggestion : suggestions) {
                        widest = Math.max(widest, this.field_146297_k.field_71466_p.func_78256_a(suggestion));
                    }
                    int panelLeft = 4;
                    int panelRight = Math.min(this.field_146294_l - 4, panelLeft + widest + 10);
                    int panelBottom = this.field_146295_m - 16;
                    int panelTop = panelBottom - 6 - fontHeight - rows * (fontHeight + 2);
                    int contentTop = panelTop + 5 + fontHeight;
                    int contentHeight = rows * rowHeight;
                    net.minecraft.client.gui.Gui.func_73734_a(panelLeft, panelTop, panelRight, panelBottom, -1072689130);
                    net.minecraft.client.gui.Gui.func_73734_a(panelLeft, panelTop, panelRight, panelTop + 1, -3381505);
                    net.minecraft.client.gui.Gui.func_73734_a(panelLeft, panelBottom - 1, panelRight, panelBottom, -2145378262);
                    this.field_146297_k.field_71466_p.func_175063_a("Suggestions", panelLeft + 4, panelTop + 3, -1517313);
                    RenderUtils.scissorPushGui(panelLeft, contentTop, panelRight - panelLeft, contentHeight);
                    float scrollOffset = this.raven$previewScrollAnim.getValue();
                    int intValue = this.raven$getActiveSuggestionIndex(input, suggestions);
                    for (int i = 0; i < suggestions.length; i++) {
                        int textY = Math.round(contentTop - scrollOffset + i * rowHeight);
                        if (textY + fontHeight >= contentTop && textY <= contentTop + contentHeight) {
                            int color = i == intValue ? -1 : -4209450;
                            this.field_146297_k.field_71466_p.func_175063_a(suggestions[i], panelLeft + 4, textY, color);
                        }
                    }
                    RenderUtils.scissorPop();
                }
            }
        }
    }

    @Inject(method = "sendAutocompleteRequest", at = @At("HEAD"), cancellable = true)
    private void handleClientCommandCompletion(String full, String ignored, CallbackInfo callbackInfo) {
        if (Raven.commandManager != null && Raven.commandManager.isCommand(full)) {
            String[] suggestions = Raven.commandManager.getAutoComplete(full);
            if (suggestions.length != 0) {
                if (suggestions.length != 1 || !full.equalsIgnoreCase(suggestions[0])) {
                    this.field_146414_r = true;
                    this.func_146406_a(suggestions);
                    callbackInfo.cancel();
                }
            }
        }
    }

    @Inject(method = "autocompletePlayerNames", at = @At("HEAD"), cancellable = true)
    private void handleCommandAutocomplete(CallbackInfo callbackInfo) {
        if (Raven.commandManager != null) {
            String input = this.field_146415_a.func_146179_b();
            if (!Raven.commandManager.isCommand(input)) {
                this.raven$resetCommandCompletions();
            } else {
                String[] suggestions;
                if (this.raven$canCycleCurrentCompletion(input)) {
                    suggestions = this.raven$commandCompletions;
                    this.raven$commandCompletionIndex = (this.raven$commandCompletionIndex + 1) % suggestions.length;
                } else {
                    suggestions = Raven.commandManager.getAutoComplete(input);
                    if (suggestions.length == 0) {
                        this.raven$resetCommandCompletions();
                        callbackInfo.cancel();
                        return;
                    }
                    this.raven$commandCompletions = suggestions;
                    this.raven$commandCompletionIndex = 0;
                }
                this.field_146415_a.func_146180_a(suggestions[this.raven$commandCompletionIndex]);
                this.field_146415_a.func_146202_e();
                callbackInfo.cancel();
            }
        }
    }

    @Unique
    private boolean raven$canCycleCurrentCompletion(String input) {
        if (this.raven$commandCompletions.length != 0 && this.raven$commandCompletionIndex >= 0) {
            for (String suggestion : this.raven$commandCompletions) {
                if (suggestion.equals(input)) {
                    return true;
                }
            }
            return false;
        } else {
            return false;
        }
    }

    @Unique
    private void raven$resetCommandCompletions() {
        this.raven$commandCompletions = new String[0];
        this.raven$commandCompletionIndex = -1;
    }

    @Unique
    private void raven$syncPreviewScrollState(String input, int suggestionCount, int visibleRows) {
        if (!input.equals(this.raven$previewScrollInput)) {
            this.raven$previewScrollInput = input;
            this.raven$previewScrollAnim.reset(0.0F);
        }
        this.raven$clampPreviewScroll(suggestionCount, visibleRows);
    }

    @Unique
    private void raven$clampPreviewScroll(int suggestionCount, int visibleRows) {
        int rowHeight = this.field_146297_k.field_71466_p.field_78288_b + 2;
        float maxScroll = Math.max(0.0F, (float) ((suggestionCount - visibleRows) * rowHeight));
        this.raven$previewScrollAnim.clampTarget(0.0F, maxScroll);
    }

    @Unique
    private boolean raven$isMouseOverPreviewPanel(int mouseX, int mouseY, String[] suggestions, int visibleRows) {
        int fontHeight = this.field_146297_k.field_71466_p.field_78288_b;
        int widest = this.field_146297_k.field_71466_p.func_78256_a("Suggestions");
        for (String suggestion : suggestions) {
            widest = Math.max(widest, this.field_146297_k.field_71466_p.func_78256_a(suggestion));
        }
        int panelLeft = 4;
        int panelRight = Math.min(this.field_146294_l - 4, panelLeft + widest + 10);
        int panelBottom = this.field_146295_m - 16;
        int panelTop = panelBottom - 6 - fontHeight - visibleRows * (fontHeight + 2);
        return mouseX >= panelLeft && mouseX <= panelRight && mouseY >= panelTop && mouseY <= panelBottom;
    }

    @Unique
    private int raven$getActiveSuggestionIndex(String input, String[] suggestions) {
        String loweredInput = input == null ? "" : input.toLowerCase();
        for (int i = 0; i < suggestions.length; i++) {
            if (loweredInput.endsWith(suggestions[i].toLowerCase())) {
                return i;
            }
        }
        return 0;
    }

    @Unique
    private void raven$resetPreviewScroll() {
        this.raven$previewScrollInput = "";
        this.raven$previewScrollAnim.reset(0.0F);
    }
}
