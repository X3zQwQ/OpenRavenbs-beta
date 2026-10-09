package keystrokesmod.utility.font;

public interface RavenFontRenderer {

    int drawString(String text2, float floatValue, float floatValue2, int intValue, boolean enabled);

    int drawGlyphString(String text3, float floatValue3, float floatValue4, RavenFontRenderer.GlyphColorProvider glyphColorProvider, boolean enabled2);

    default int drawString(String text, float x, float y, int color) {
        return this.drawString(text, x, y, color, false);
    }

    default int drawGlyphString(String text, float x, float y, RavenFontRenderer.GlyphColorProvider colorProvider) {
        return this.drawGlyphString(text, x, y, colorProvider, false);
    }

    default int drawStringWithShadow(String text, float x, float y, int color) {
        return this.drawString(text, x, y, color, true);
    }

    int getStringWidth(String text4);

    int getFontHeight();

    default int getLineHeight() {
        return this.getFontHeight();
    }

    default int getTextTopOffset() {
        return 0;
    }

    default int getTextBottomOffset() {
        return this.getFontHeight();
    }

    default void destroy() {
    }

    @FunctionalInterface
    public interface GlyphColorProvider {

        int colorForGlyph(char charValue, float floatValue5, float floatValue6, Integer integer);
    }
}
