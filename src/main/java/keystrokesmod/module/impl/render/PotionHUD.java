package keystrokesmod.module.impl.render;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.PotionListSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.RenderUtils;
import keystrokesmod.utility.Utils;
import keystrokesmod.utility.font.FontManager;
import keystrokesmod.utility.font.RavenFontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.StatCollector;
import net.minecraftforge.fml.client.config.GuiButtonExt;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.RenderTickEvent;

public class PotionHUD extends Module {

    private static final String[] TIME_FORMAT_OPTIONS = new String[] { "1m20s", "01:20" };

    private static final String[] SORT_OPTIONS = new String[] { "Duration", "Length" };

    private static final String[] SORT_DIRECTION_OPTIONS = new String[] { "Descending", "Ascending" };

    private static final String[] ALIGNMENT_OPTIONS = new String[] { "Left", "Center", "Right" };

    private static final String[] VERTICAL_ALIGNMENT_OPTIONS = new String[] { "Top", "Center", "Bottom" };

    private static final float DEFAULT_RELATIVE_X = 0.5F;

    private static final float DEFAULT_RELATIVE_Y = 0.5F;

    private static final int TIMER_COLOR = -5592406;

    private static final int EDIT_OUTLINE_COLOR = -1;

    private static final int BACKGROUND_COLOR = new Color(0, 0, 0, 110).getRGB();

    private static final String PLACEHOLDER_TEXT = "No Potions Active";

    private final SliderSetting timeFormat;

    private final SliderSetting sortMode;

    private final SliderSetting sortDirection;

    private final SliderSetting horizontalAlignment;

    private final SliderSetting verticalAlignment;

    private final SliderSetting font;

    private final SliderSetting scale;

    private final ButtonSetting excludePermanent;

    private final ButtonSetting drawBackground;

    private final ButtonSetting textShadow;

    private final PotionListSetting potionBlacklist;

    private float posX = Float.NaN;

    private float posY = Float.NaN;

    private float relativePosX = Float.NaN;

    private float relativePosY = Float.NaN;

    public PotionHUD() {
        super("Potion HUD", Module.category.render);
        this.registerSetting(this.timeFormat = new SliderSetting("Time Format", 0, TIME_FORMAT_OPTIONS));
        this.registerSetting(this.sortMode = new SliderSetting("Sort By", 0, SORT_OPTIONS));
        this.registerSetting(this.sortDirection = new SliderSetting("Sort Direction", 0, SORT_DIRECTION_OPTIONS));
        this.registerSetting(this.horizontalAlignment = new SliderSetting("Horizontal Alignment", 0, ALIGNMENT_OPTIONS, "Alignment"));
        this.registerSetting(this.verticalAlignment = new SliderSetting("Vertical Alignment", 0, VERTICAL_ALIGNMENT_OPTIONS, "Direction"));
        this.registerSetting(this.font = FontManager.createFontSetting("Font"));
        this.registerSetting(this.scale = new SliderSetting("Scale", 1.0, 0.5, 2.0, 0.1));
        this.registerSetting(new ButtonSetting("Edit position", () -> mc.func_147108_a(new PotionHUD.EditScreen())));
        this.registerSetting(this.excludePermanent = new ButtonSetting("Exclude permanent", false));
        this.registerSetting(this.drawBackground = new ButtonSetting("Draw background", false));
        this.registerSetting(this.textShadow = new ButtonSetting("Text shadow", true));
        this.registerSetting(this.potionBlacklist = new PotionListSetting("Blacklisted potions", "Potions", "Potion blacklist"));
    }

    @SubscribeEvent
    public void onRenderTick(RenderTickEvent event) {
        if (event.phase == Phase.END && Utils.nullCheck()) {
            if (mc.field_71462_r == null && !mc.field_71474_y.field_74330_P) {
                this.render(false);
            }
        }
    }

    public float getPosX() {
        this.syncPositionToResolution();
        return this.posX;
    }

    public float getPosY() {
        this.syncPositionToResolution();
        return this.posY;
    }

    public float getRelativePosX() {
        this.syncPositionToResolution();
        return this.relativePosX;
    }

    public float getRelativePosY() {
        this.syncPositionToResolution();
        return this.relativePosY;
    }

    public void setRelativePosition(float normalizedX, float normalizedY) {
        this.relativePosX = normalizedX;
        this.relativePosY = normalizedY;
        this.syncPositionToResolution();
    }

    public void setAbsolutePosition(float absoluteX, float absoluteY) {
        this.setAbsolutePosition(absoluteX, absoluteY, new ScaledResolution(mc));
    }

    public void resetPosition() {
        this.setRelativePosition(0.5F, 0.5F);
    }

    private void render(boolean editing) {
        ScaledResolution resolution = new ScaledResolution(mc);
        this.syncPositionToResolution(resolution);
        PotionHUD.RenderState state = this.buildRenderState(editing);
        if (!state.entries.isEmpty()) {
            this.adjustAnchorForLayoutChanges(resolution, state);
            PotionHUD.Bounds bounds = this.renderState(state);
            if (editing) {
                this.drawBounds(bounds);
            }
        }
    }

    private PotionHUD.RenderState buildRenderState(boolean includePlaceholder) {
        RavenFontRenderer renderer = this.getFontRenderer();
        PotionHUD.LayoutMetrics metrics = PotionHUD.LayoutMetrics.from(renderer, this.getSelectedScale());
        ArrayList<PotionHUD.PotionEntry> entries = new ArrayList<>();
        Collection<PotionEffect> activeEffects = mc.field_71439_g.func_70651_bq();
        if (activeEffects != null) {
            for (PotionEffect effect : activeEffects) {
                PotionHUD.PotionEntry entry = this.buildEntry(effect, renderer);
                if (entry != null) {
                    entries.add(entry);
                }
            }
        }
        this.sortEntries(entries);
        if (entries.isEmpty() && includePlaceholder) {
            entries.add(PotionHUD.PotionEntry.placeholder(renderer));
        }
        int maxWidth = 0;
        for (PotionHUD.PotionEntry entry : entries) {
            maxWidth = Math.max(maxWidth, entry.totalWidth);
        }
        return new PotionHUD.RenderState(renderer, metrics, entries, maxWidth);
    }

    private PotionHUD.PotionEntry buildEntry(PotionEffect effect, RavenFontRenderer renderer) {
        if (effect == null) {
            return null;
        } else {
            int duration = effect.func_76459_b();
            if (this.excludePermanent.isToggled() && duration > 32000) {
                return null;
            } else {
                Potion potion = Potion.field_76425_a[effect.func_76456_a()];
                if (potion == null) {
                    return null;
                } else if (this.potionBlacklist.containsPotion(potion.func_76393_a())) {
                    return null;
                } else {
                    String label = this.getPotionLabel(effect, potion);
                    String durationText = this.formatDuration(duration);
                    int labelWidth = renderer.getStringWidth(label);
                    int durationWidth = durationText.isEmpty() ? 0 : renderer.getStringWidth(durationText);
                    int gapWidth = durationWidth > 0 ? renderer.getStringWidth(" ") : 0;
                    return new PotionHUD.PotionEntry(label, durationText, labelWidth, durationWidth, gapWidth, duration, potion.func_76401_j());
                }
            }
        }
    }

    private void sortEntries(List<PotionHUD.PotionEntry> entries) {
        final int directionMultiplier = (int) this.sortDirection.getInput() == 1 ? 1 : -1;
        if ((int) this.sortMode.getInput() == 0) {
            entries.sort(new Comparator<PotionHUD.PotionEntry>() {

                public int compare(PotionHUD.PotionEntry first, PotionHUD.PotionEntry second) {
                    return Integer.compare(first.durationTicks, second.durationTicks) * directionMultiplier;
                }
            });
        } else {
            entries.sort(new Comparator<PotionHUD.PotionEntry>() {

                public int compare(PotionHUD.PotionEntry first, PotionHUD.PotionEntry second) {
                    return Integer.compare(first.totalWidth, second.totalWidth) * directionMultiplier;
                }
            });
        }
    }

    private PotionHUD.Bounds renderState(PotionHUD.RenderState state) {
        int horizontalAlignMode = (int) this.horizontalAlignment.getInput();
        int verticalAlignMode = (int) this.verticalAlignment.getInput();
        float stackHeight = state.entries.size() * state.metrics.rowHeight;
        float firstRowTop = verticalAlignMode == 0 ? this.posY : (verticalAlignMode == 1 ? this.posY - stackHeight / 2.0F : this.posY - stackHeight);
        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;
        for (int i = 0; i < state.entries.size(); i++) {
            PotionHUD.PotionEntry entry = state.entries.get(i);
            float rowTop = firstRowTop + i * state.metrics.rowHeight;
            float rowLeft = horizontalAlignMode == 0 ? this.posX : (horizontalAlignMode == 2 ? this.posX - entry.totalWidth : this.posX - entry.totalWidth / 2.0F);
            float backgroundLeft = rowLeft - state.metrics.horizontalTextPadding;
            float backgroundRight = rowLeft + entry.totalWidth + state.metrics.horizontalTextPadding;
            float backgroundBottom = rowTop + state.metrics.rowHeight;
            float textY = rowTop + state.metrics.textTopPadding - state.metrics.textTopOffset;
            float timerX = rowLeft + entry.labelWidth + entry.gapWidth;
            if (this.drawBackground.isToggled()) {
                RenderUtils.drawRect(backgroundLeft, rowTop, backgroundRight, backgroundBottom, BACKGROUND_COLOR);
            }
            state.renderer.drawString(entry.label, rowLeft, textY, entry.color, this.textShadow.isToggled());
            if (!entry.durationText.isEmpty()) {
                state.renderer.drawString(entry.durationText, timerX, textY, -5592406, this.textShadow.isToggled());
            }
            minX = Math.min(minX, backgroundLeft);
            minY = Math.min(minY, rowTop);
            maxX = Math.max(maxX, backgroundRight);
            maxY = Math.max(maxY, backgroundBottom);
        }
        return new PotionHUD.Bounds(minX, minY, maxX, maxY);
    }

    private void drawBounds(PotionHUD.Bounds bounds) {
        float left = bounds.left - 1.0F;
        float top = bounds.top - 1.0F;
        float right = bounds.right + 1.0F;
        float bottom = bounds.bottom + 1.0F;
        RenderUtils.drawRect(left, top, right, top + 1.0F, -1);
        RenderUtils.drawRect(left, bottom - 1.0F, right, bottom, -1);
        RenderUtils.drawRect(left, top, left + 1.0F, bottom, -1);
        RenderUtils.drawRect(right - 1.0F, top, right, bottom, -1);
    }

    private void adjustAnchorForLayoutChanges(ScaledResolution resolution, PotionHUD.RenderState state) {
        this.syncPositionToResolution(resolution);
    }

    private void syncPositionToResolution() {
        this.syncPositionToResolution(new ScaledResolution(mc));
    }

    private void syncPositionToResolution(ScaledResolution resolution) {
        int scaledWidth = Math.max(1, resolution.func_78326_a());
        int scaledHeight = Math.max(1, resolution.func_78328_b());
        if (Float.isNaN(this.relativePosX) || Float.isNaN(this.relativePosY)) {
            if (!Float.isNaN(this.posX) && !Float.isNaN(this.posY)) {
                this.relativePosX = this.posX / scaledWidth;
                this.relativePosY = this.posY / scaledHeight;
            } else {
                this.relativePosX = 0.5F;
                this.relativePosY = 0.5F;
            }
        }
        this.posX = this.relativePosX * scaledWidth;
        this.posY = this.relativePosY * scaledHeight;
    }

    private void setAbsolutePosition(float absoluteX, float absoluteY, ScaledResolution resolution) {
        this.posX = absoluteX;
        this.posY = absoluteY;
        int scaledWidth = Math.max(1, resolution.func_78326_a());
        int scaledHeight = Math.max(1, resolution.func_78328_b());
        this.relativePosX = absoluteX / scaledWidth;
        this.relativePosY = absoluteY / scaledHeight;
    }

    private RavenFontRenderer getFontRenderer() {
        return FontManager.getHudRenderer(this.getSelectedFontName(), this.getSelectedScale());
    }

    private String getSelectedFontName() {
        int index = (int) Math.max(0.0, Math.min((double) (this.font.getOptions().length - 1), this.font.getInput()));
        return this.font.getOptions()[index];
    }

    private float getSelectedScale() {
        return (float) this.scale.getInput();
    }

    private String getPotionLabel(PotionEffect effect, Potion potion) {
        String label = StatCollector.func_74838_a(effect.func_76453_d());
        if (effect.func_76458_c() >= 1) {
            label = label + " " + toRomanNumeral(effect.func_76458_c() + 1);
        }
        return label.isEmpty() ? StatCollector.func_74838_a(potion.func_76393_a()) : label;
    }

    private String formatDuration(int durationTicks) {
        int totalSeconds = Math.max(0, durationTicks / 20);
        if ((int) this.timeFormat.getInput() == 1) {
            int minutes = totalSeconds / 60;
            int seconds = totalSeconds % 60;
            return String.format(Locale.ROOT, "%02d:%02d", minutes, seconds);
        } else {
            int minutes = totalSeconds / 60;
            int seconds = totalSeconds % 60;
            return minutes > 0 && seconds == 0 ? minutes + "m" : (minutes > 0 ? minutes + "m" : "") + seconds + "s";
        }
    }

    private static int getScaledHudPixels(float basePixels, float fontScale) {
        return Math.max(1, Math.round(basePixels * fontScale));
    }

    private static String toRomanNumeral(int value) {
        if (value <= 0) {
            return Integer.toString(value);
        } else {
            int[] values = new int[] { 1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1 };
            String[] numerals = new String[] { "M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I" };
            StringBuilder builder = new StringBuilder();
            int remaining = value;
            for (int i = 0; i < values.length; i++) {
                while (remaining >= values[i]) {
                    builder.append(numerals[i]);
                    remaining -= values[i];
                }
            }
            return builder.toString();
        }
    }

    private static final class Bounds {

        private final float left;

        private final float top;

        private final float right;

        private final float bottom;

        private Bounds(float left, float top, float right, float bottom) {
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
        }
    }

    private class EditScreen extends GuiScreen {

        private GuiButtonExt resetPosition;

        private boolean dragging;

        private float minX;

        private float minY;

        private float maxX;

        private float maxY;

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
            this.field_146292_n.add(this.resetPosition = new GuiButtonExt(1, this.field_146294_l - 90, this.field_146295_m - 25, 85, 20, "Reset position"));
            PotionHUD.this.syncPositionToResolution(new ScaledResolution(this.field_146297_k));
            this.actualX = PotionHUD.this.posX;
            this.actualY = PotionHUD.this.posY;
        }

        public void func_73863_a(int mouseX, int mouseY, float partialTicks) {
            ScaledResolution resolution = new ScaledResolution(this.field_146297_k);
            if (!this.dragging) {
                PotionHUD.this.syncPositionToResolution(resolution);
                this.actualX = PotionHUD.this.posX;
                this.actualY = PotionHUD.this.posY;
            }
            func_73734_a(0, 0, this.field_146294_l, this.field_146295_m, -1308622848);
            PotionHUD.this.setAbsolutePosition(this.actualX, this.actualY, resolution);
            PotionHUD.RenderState state = PotionHUD.this.buildRenderState(true);
            PotionHUD.this.adjustAnchorForLayoutChanges(resolution, state);
            PotionHUD.Bounds bounds = PotionHUD.this.renderState(state);
            PotionHUD.this.drawBounds(bounds);
            this.minX = bounds.left;
            this.minY = bounds.top;
            this.maxX = bounds.right;
            this.maxY = bounds.bottom;
            this.actualX = PotionHUD.this.posX;
            this.actualY = PotionHUD.this.posY;
            String message = "Edit the HUD position by dragging.";
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
                } else if (mouseX >= this.minX && mouseX <= this.maxX && mouseY >= this.minY && mouseY <= this.maxY) {
                    this.dragging = true;
                    this.lastMouseX = mouseX;
                    this.lastMouseY = mouseY;
                    this.lastActualX = this.actualX;
                    this.lastActualY = this.actualY;
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
            if (button == this.resetPosition) {
                PotionHUD.this.resetPosition();
                this.actualX = PotionHUD.this.posX;
                this.actualY = PotionHUD.this.posY;
            }
        }

        public boolean func_73868_f() {
            return false;
        }
    }

    private static final class LayoutMetrics {

        private final int textTopOffset;

        private final int textTopPadding;

        private final int horizontalTextPadding;

        private final int rowHeight;

        private LayoutMetrics(int textTopOffset, int textTopPadding, int horizontalTextPadding, int rowHeight) {
            this.textTopOffset = textTopOffset;
            this.textTopPadding = textTopPadding;
            this.horizontalTextPadding = horizontalTextPadding;
            this.rowHeight = rowHeight;
        }

        private static PotionHUD.LayoutMetrics from(RavenFontRenderer renderer, float fontScale) {
            int textTopOffset = renderer.getTextTopOffset();
            int textBottomOffset = renderer.getTextBottomOffset();
            int textTopPadding = PotionHUD.getScaledHudPixels(2.0F, fontScale);
            int textBottomPadding = 0;
            int horizontalTextPadding = PotionHUD.getScaledHudPixels(2.0F, fontScale);
            int textBoxHeight = Math.max(1, textBottomOffset - textTopOffset);
            int rowHeight = Math.max(1, textBoxHeight + textTopPadding + textBottomPadding);
            return new PotionHUD.LayoutMetrics(textTopOffset, textTopPadding, horizontalTextPadding, rowHeight);
        }
    }

    private static final class PotionEntry {

        private final String label;

        private final String durationText;

        private final int labelWidth;

        private final int durationWidth;

        private final int gapWidth;

        private final int totalWidth;

        private final int durationTicks;

        private final int color;

        private PotionEntry(String label, String durationText, int labelWidth, int durationWidth, int gapWidth, int durationTicks, int color) {
            this.label = label;
            this.durationText = durationText;
            this.labelWidth = labelWidth;
            this.durationWidth = durationWidth;
            this.gapWidth = gapWidth;
            this.totalWidth = labelWidth + gapWidth + durationWidth;
            this.durationTicks = durationTicks;
            this.color = color;
        }

        private static PotionHUD.PotionEntry placeholder(RavenFontRenderer renderer) {
            String text = "No Potions Active";
            return new PotionHUD.PotionEntry(text, "", renderer.getStringWidth(text), 0, 0, 0, -1);
        }
    }

    private static final class RenderState {

        private final RavenFontRenderer renderer;

        private final PotionHUD.LayoutMetrics metrics;

        private final List<PotionHUD.PotionEntry> entries;

        private final int maxWidth;

        private RenderState(RavenFontRenderer renderer, PotionHUD.LayoutMetrics metrics, List<PotionHUD.PotionEntry> entries, int maxWidth) {
            this.renderer = renderer;
            this.metrics = metrics;
            this.entries = entries;
            this.maxWidth = maxWidth;
        }
    }
}
