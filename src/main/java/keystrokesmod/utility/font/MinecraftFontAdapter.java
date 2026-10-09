package keystrokesmod.utility.font;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;

public final class MinecraftFontAdapter implements RavenFontRenderer {

    private static final String COLOR_CODES = "0123456789abcdef";

    private static final char SECTION_SIGN = '§';

    private final FontRenderer fontRenderer;

    private final float scale;

    public MinecraftFontAdapter(FontRenderer fontRenderer) {
        this(fontRenderer, 1.0F);
    }

    public MinecraftFontAdapter(FontRenderer fontRenderer, float scale) {
        this.fontRenderer = fontRenderer;
        this.scale = Math.max(0.5F, Math.min(2.0F, scale));
    }

    @Override
    public int drawString(String text, float x, float y, int color, boolean shadow) {
        if (this.scale == 1.0F) {
            return this.fontRenderer.func_175065_a(text, x, y, color, shadow);
        } else {
            GlStateManager.func_179094_E();
            GlStateManager.func_179109_b(x, y, 0.0F);
            GlStateManager.func_179152_a(this.scale, this.scale, 1.0F);
            int width = this.fontRenderer.func_175065_a(text, 0.0F, 0.0F, color, shadow);
            GlStateManager.func_179121_F();
            return Math.round(width * this.scale);
        }
    }

    @Override
    public int drawGlyphString(String text, float x, float y, RavenFontRenderer.GlyphColorProvider colorProvider, boolean shadow) {
        if (text != null && !text.isEmpty()) {
            int rawMeasuredWidth = 0;
            Integer formattingColor = null;
            StringBuilder activeFormats = new StringBuilder();
            for (int i = 0; i < text.length(); i++) {
                char character = text.charAt(i);
                if (character == 167 && i + 1 < text.length()) {
                    char formatCode = Character.toLowerCase(text.charAt(++i));
                    int intValue = "0123456789abcdef".indexOf(formatCode);
                    if (intValue >= 0) {
                        formattingColor = 0xFF000000 | this.fontRenderer.func_175064_b(formatCode);
                        activeFormats.setLength(0);
                    } else if (formatCode == 'r') {
                        formattingColor = null;
                        activeFormats.setLength(0);
                    } else if (isStyleCode(formatCode)) {
                        appendFormatCode(activeFormats, formatCode);
                    }
                } else if (character != '\n') {
                    int rawGlyphWidth = this.fontRenderer.func_78263_a(character);
                    if (rawGlyphWidth >= 0) {
                        if (hasFormat(activeFormats, 'l') && rawGlyphWidth > 0) {
                            rawGlyphWidth++;
                        }
                        float glyphXOffset = rawMeasuredWidth * this.scale;
                        float glyphWidth = rawGlyphWidth * this.scale;
                        int glyphColor = colorProvider.colorForGlyph(character, glyphXOffset, glyphWidth, formattingColor);
                        this.drawFormattedGlyph(character, activeFormats, x + glyphXOffset, y, glyphColor, shadow);
                        rawMeasuredWidth += rawGlyphWidth;
                    }
                }
            }
            return Math.round(rawMeasuredWidth * this.scale);
        } else {
            return 0;
        }
    }

    @Override
    public int getStringWidth(String text) {
        return Math.round(this.fontRenderer.func_78256_a(text) * this.scale);
    }

    @Override
    public int getFontHeight() {
        return Math.round(this.fontRenderer.field_78288_b * this.scale);
    }

    @Override
    public int getLineHeight() {
        return Math.round(this.fontRenderer.field_78288_b * this.scale);
    }

    @Override
    public int getTextTopOffset() {
        return 0;
    }

    @Override
    public int getTextBottomOffset() {
        return Math.max(1, Math.round((this.fontRenderer.field_78288_b - 1.0F) * this.scale));
    }

    public float getScale() {
        return this.scale;
    }

    private void drawFormattedGlyph(char character, StringBuilder activeFormats, float x, float y, int color, boolean shadow) {
        if (activeFormats.length() == 0) {
            this.drawString(String.valueOf(character), x, y, color, shadow);
        } else {
            StringBuilder glyphText = new StringBuilder(activeFormats.length() + 1);
            glyphText.append((CharSequence) activeFormats).append(character);
            this.drawString(glyphText.toString(), x, y, color, shadow);
        }
    }

    private static boolean isStyleCode(char formatCode) {
        return formatCode >= 'k' && formatCode <= 'o';
    }

    private static boolean hasFormat(StringBuilder activeFormats, char formatCode) {
        for (int i = 1; i < activeFormats.length(); i += 2) {
            if (activeFormats.charAt(i) == formatCode) {
                return true;
            }
        }
        return false;
    }

    private static void appendFormatCode(StringBuilder activeFormats, char formatCode) {
        if (!hasFormat(activeFormats, formatCode)) {
            activeFormats.append('§').append(formatCode);
        }
    }
}
