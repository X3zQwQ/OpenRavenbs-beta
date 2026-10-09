package keystrokesmod.utility;

import java.awt.Color;
import keystrokesmod.module.impl.client.Settings;

public enum Theme {

    Rainbow(null, null),
    Cherry(new Color(255, 200, 200), new Color(243, 58, 106)),
    Cotton_candy(new Color(99, 249, 255), new Color(255, 104, 204)),
    Flare(new Color(231, 39, 24), new Color(245, 173, 49)),
    Flower(new Color(215, 166, 231), new Color(211, 90, 232)),
    Gold(new Color(255, 215, 0), new Color(240, 159, 0)),
    Grayscale(new Color(240, 240, 240), new Color(110, 110, 110)),
    Royal(new Color(125, 204, 241), new Color(30, 71, 170)),
    Sky(new Color(160, 230, 225), new Color(15, 190, 220)),
    Vine(new Color(17, 192, 45), new Color(201, 234, 198));

    private final Color firstGradient;

    private final Color secondGradient;

    public static Color[] descriptor = new Color[] { new Color(95, 235, 255), new Color(68, 102, 250) };

    public static Color[] hiddenBind = new Color[] { new Color(245, 33, 33), new Color(229, 21, 98) };

    public static String[] THEMES_SETTING = new String[] { "Rainbow", "Cherry", "Cotton candy", "Flare", "Flower", "Gold", "Grayscale", "Royal", "Sky", "Vine", "Default" };

    public static String[] THEMES_STRING = new String[] { "Rainbow", "Cherry", "Cotton candy", "Flare", "Flower", "Gold", "Grayscale", "Royal", "Sky", "Vine" };

    private Theme(Color firstGradient, Color secondGradient) {
        this.firstGradient = firstGradient;
        this.secondGradient = secondGradient;
    }

    public static int getGradient(int index, double delay) {
        if (index == 10) {
            return getGradient((int) Settings.defaultTheme.getInput(), delay);
        } else if (index > 0) {
            return convert(values()[index].firstGradient, values()[index].secondGradient, (Math.sin(System.currentTimeMillis() / 1.0E8 * Settings.timeMultiplier.getInput() * 400000.0 + delay * Settings.offset.getInput()) + 1.0) * 0.5).getRGB();
        } else {
            return index == 0 ? getChromaOffset(2L, (long) delay) : -1;
        }
    }

    public static int getChromaOffset(long speed, long delay) {
        double offset = Settings.offset.getInput() * delay;
        long time = System.currentTimeMillis() + (long) offset;
        float hue = (float) (time % (15000L / speed)) / (15000.0F / (float) speed);
        return Color.getHSBColor(hue, 1.0F, 1.0F).getRGB();
    }

    public static int getGradient(Color firstGradient, Color secondGradient, double delay) {
        return convert(firstGradient, secondGradient, (Math.sin(System.currentTimeMillis() / 1.0E8 * 0.5 * 400000.0 + delay * 0.55F) + 1.0) * 0.5).getRGB();
    }

    public static Color convert(Color color, Color color2, double delay) {
        double n2 = 1.0 - delay;
        return new Color((int) (color.getRed() * delay + color2.getRed() * n2), (int) (color.getGreen() * delay + color2.getGreen() * n2), (int) (color.getBlue() * delay + color2.getBlue() * n2));
    }

    public static int[] getGradients(int index) {
        if (index == 10) {
            index = (int) Settings.defaultTheme.getInput();
        }
        Theme[] values = values();
        if (values != null && index >= 0 && index < values.length && values[index] != null) {
            Color firstGradient = values[index].firstGradient;
            Color secondGradient = values[index].secondGradient;
            return firstGradient != null && secondGradient != null ? new int[] { firstGradient.getRGB(), secondGradient.getRGB() } : new int[] { Utils.getChroma(2L, 0L), Utils.getChroma(2L, 0L) };
        } else {
            return new int[] { 0, 0 };
        }
    }
}
