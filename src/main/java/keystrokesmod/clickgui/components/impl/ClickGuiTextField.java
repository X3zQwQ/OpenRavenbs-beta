package keystrokesmod.clickgui.components.impl;

import java.lang.reflect.Field;
import keystrokesmod.module.impl.client.Gui;
import keystrokesmod.utility.RenderUtils;
import keystrokesmod.utility.font.RavenFontRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiTextField;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import org.lwjgl.opengl.GL11;

public class ClickGuiTextField {

    private static final float DEFAULT_TEXT_SCALE = 0.5F;

    private static final int BACKGROUND_COLOR = -14540254;

    private static final int FOCUSED_BACKGROUND_COLOR = -14013910;

    private static final int OUTLINE_COLOR = -12961222;

    private static final int FOCUSED_OUTLINE_COLOR = -11184896;

    private static final int SELECTION_COLOR = -2142596894;

    private static final int CURSOR_COLOR = -3092272;

    private static final Field LINE_SCROLL_OFFSET_FIELD = ReflectionHelper.findField(GuiTextField.class, new String[] { "lineScrollOffset", "field_146225_q" });

    private static final Field CURSOR_POSITION_FIELD = ReflectionHelper.findField(GuiTextField.class, new String[] { "cursorPosition", "field_146224_r" });

    private static final Field SELECTION_END_FIELD = ReflectionHelper.findField(GuiTextField.class, new String[] { "selectionEnd", "field_146223_s" });

    private static final Field ENABLED_COLOR_FIELD = ReflectionHelper.findField(GuiTextField.class, new String[] { "enabledColor", "field_146222_t" });

    private static final Field DISABLED_COLOR_FIELD = ReflectionHelper.findField(GuiTextField.class, new String[] { "disabledColor", "field_146221_u" });

    private static final Field IS_ENABLED_FIELD = ReflectionHelper.findField(GuiTextField.class, new String[] { "isEnabled", "field_146226_p" });

    private static int nextId;

    private final GuiTextField textField;

    private final String placeholder;

    private final float textScale;

    private long lastCursorTick;

    private boolean cursorVisible;

    public ClickGuiTextField(String placeholder, int maxLength) {
        this(placeholder, maxLength, 0.5F);
    }

    public ClickGuiTextField(String placeholder, int maxLength, float textScale) {
        this.placeholder = placeholder == null ? "" : placeholder;
        this.textScale = textScale;
        this.textField = new GuiTextField(nextId++, Minecraft.func_71410_x().field_71466_p, 0, 0, 100, 20);
        this.textField.func_146203_f(maxLength);
        this.textField.func_146185_a(false);
        this.textField.func_146205_d(true);
    }

    public void render(float left, float top, float right, float bottom) {
        float textLeft = left + 2.0F;
        float width = Math.max(0.0F, right - left - 4.0F);
        float backgroundBottom = bottom - 1.0F;
        float height = Math.max(0.0F, backgroundBottom - top);
        RavenFontRenderer renderer = Gui.getClickGuiSettingFontRenderer();
        String fullText = this.textField.func_146179_b();
        int lineScrollOffset = Math.max(0, Math.min(getIntField(this.textField, LINE_SCROLL_OFFSET_FIELD, 0), fullText.length()));
        String visibleText = this.getVisibleText(fullText, lineScrollOffset, width, renderer);
        int cursorPosition = getIntField(this.textField, CURSOR_POSITION_FIELD, fullText.length());
        int selectionEnd = getIntField(this.textField, SELECTION_END_FIELD, cursorPosition);
        int visibleEnd = lineScrollOffset + visibleText.length();
        float textY = this.centeredScaledTextY(top, height, renderer);
        int textColor = getBooleanField(this.textField, IS_ENABLED_FIELD, true) ? getIntField(this.textField, ENABLED_COLOR_FIELD, 14737632) : getIntField(this.textField, DISABLED_COLOR_FIELD, 7368816);
        RenderUtils.drawRect(left, top, right, backgroundBottom, this.isFocused() ? -14013910 : -14540254);
        RenderUtils.drawOutline(left, top, right, backgroundBottom, 1.0F, this.isFocused() ? -11184896 : -12961222);
        if (!visibleText.isEmpty()) {
            this.drawSelection(textLeft, top, backgroundBottom, renderer, visibleText, cursorPosition, selectionEnd, lineScrollOffset, visibleEnd);
            this.drawScaledText(visibleText, textLeft, textY, textColor, renderer);
        } else if (!this.textField.func_146206_l() && !this.placeholder.isEmpty()) {
            this.drawScaledText("§7" + this.placeholder, textLeft, textY, 11184810, renderer);
        }
        if (this.textField.func_146206_l() && this.cursorVisible) {
            this.drawCursor(textLeft, top, backgroundBottom, renderer, visibleText, cursorPosition, lineScrollOffset, visibleEnd);
        }
    }

    public void tickCursor() {
        if (System.currentTimeMillis() - this.lastCursorTick >= 500L) {
            this.lastCursorTick = System.currentTimeMillis();
            this.cursorVisible = !this.cursorVisible;
        }
        this.textField.func_146178_a();
    }

    public boolean contains(int mouseX, int mouseY, float left, float top, float right, float bottom) {
        return mouseX >= left && mouseX <= right && mouseY >= top && mouseY <= bottom;
    }

    public boolean textboxKeyTyped(char typedChar, int keyCode) {
        return this.textField.func_146201_a(typedChar, keyCode);
    }

    public String getText() {
        return this.textField.func_146179_b();
    }

    public void setText(String text) {
        this.textField.func_146180_a(text == null ? "" : text);
    }

    public boolean isFocused() {
        return this.textField.func_146206_l();
    }

    public void setFocused(boolean focused) {
        this.textField.func_146195_b(focused);
        if (focused) {
            this.cursorVisible = true;
            this.lastCursorTick = System.currentTimeMillis();
        }
    }

    private String getVisibleText(String text, int intValue, float width, RavenFontRenderer renderer) {
        if (text != null && !text.isEmpty() && !(width <= 0.0F)) {
            float maxScaledWidth = width / this.textScale;
            String remaining = text.substring(intValue);
            StringBuilder visible = new StringBuilder();
            for (int i = 0; i < remaining.length(); i++) {
                char character = remaining.charAt(i);
                String candidate = visible.toString() + character;
                if (renderer.getStringWidth(candidate) > maxScaledWidth) {
                    break;
                }
                visible.append(character);
            }
            return visible.toString();
        } else {
            return "";
        }
    }

    private void drawSelection(float textLeft, float top, float bottom, RavenFontRenderer renderer, String visibleText, int cursorPosition, int selectionEnd, int visibleStart, int visibleEnd) {
        int selectionStart = Math.min(cursorPosition, selectionEnd);
        int selectionStop = Math.max(cursorPosition, selectionEnd);
        if (selectionStart != selectionStop && selectionStop > visibleStart && selectionStart < visibleEnd) {
            int localStart = Math.max(0, selectionStart - visibleStart);
            int localEnd = Math.min(visibleText.length(), selectionStop - visibleStart);
            float selectionLeft = textLeft + renderer.getStringWidth(visibleText.substring(0, localStart)) * this.textScale;
            float selectionRight = textLeft + renderer.getStringWidth(visibleText.substring(0, localEnd)) * this.textScale;
            if (selectionRight > selectionLeft) {
                RenderUtils.drawRect(selectionLeft, top + 1.0F, selectionRight, bottom - 1.0F, -2142596894);
            }
        }
    }

    private void drawCursor(float textLeft, float top, float bottom, RavenFontRenderer renderer, String visibleText, int cursorPosition, int visibleStart, int visibleEnd) {
        float cursorX;
        if (cursorPosition <= visibleStart) {
            cursorX = textLeft;
        } else if (cursorPosition >= visibleEnd) {
            cursorX = textLeft + renderer.getStringWidth(visibleText) * this.textScale;
        } else {
            cursorX = textLeft + renderer.getStringWidth(visibleText.substring(0, cursorPosition - visibleStart)) * this.textScale;
        }
        RenderUtils.drawRect(cursorX, top + 2.0F, cursorX + 1.0F, bottom - 2.0F, -3092272);
    }

    private float centeredScaledTextY(float top, float height, RavenFontRenderer renderer) {
        float textBoxHeight = Math.max(1.0F, (renderer.getTextBottomOffset() - renderer.getTextTopOffset()) * this.textScale);
        return top + (height - textBoxHeight) / 2.0F - renderer.getTextTopOffset() * this.textScale;
    }

    private void drawScaledText(String text, float x, float y, int color, RavenFontRenderer renderer) {
        GL11.glPushMatrix();
        GL11.glScaled(this.textScale, this.textScale, this.textScale);
        renderer.drawString(text, x / this.textScale, y / this.textScale, color, false);
        GL11.glPopMatrix();
    }

    private static int getIntField(GuiTextField textField, Field field, int fallback) {
        try {
            return field.getInt(textField);
        } catch (IllegalAccessException exception) {
            return fallback;
        }
    }

    private static boolean getBooleanField(GuiTextField textField, Field field, boolean fallback) {
        try {
            return field.getBoolean(textField);
        } catch (IllegalAccessException exception2) {
            return fallback;
        }
    }
}
