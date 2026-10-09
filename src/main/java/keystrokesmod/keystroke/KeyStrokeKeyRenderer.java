package keystrokesmod.keystroke;

import java.awt.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;

public class KeyStrokeKeyRenderer {

    private Minecraft mc = Minecraft.func_71410_x();

    private KeyBinding keyBinding;

    private int c;

    private int d;

    private boolean e = true;

    private long f = 0L;

    private int g = 255;

    private double h = 1.0;

    public KeyStrokeKeyRenderer(KeyBinding i, int j, int k) {
        this.keyBinding = i;
        this.c = j;
        this.d = k;
    }

    public void renderKey(int l, int m, int color) {
        boolean o = this.keyBinding.func_151470_d();
        String p = Keyboard.getKeyName(this.keyBinding.func_151463_i());
        if (o != this.e) {
            this.e = o;
            this.f = System.currentTimeMillis();
        }
        if (o) {
            this.g = Math.min(255, (int) (2L * (System.currentTimeMillis() - this.f)));
            this.h = Math.max(0.0, 1.0 - (System.currentTimeMillis() - this.f) / 20.0);
        } else {
            this.g = Math.max(0, 255 - (int) (2L * (System.currentTimeMillis() - this.f)));
            this.h = Math.min(1.0, (System.currentTimeMillis() - this.f) / 20.0);
        }
        int q = color >> 16 & 0xFF;
        int r = color >> 8 & 0xFF;
        int s = color & 0xFF;
        int c = new Color(q, r, s).getRGB();
        Gui.func_73734_a(l + this.c, m + this.d, l + this.c + 22, m + this.d + 22, 2013265920 + (this.g << 16) + (this.g << 8) + this.g);
        if (KeyStroke.f) {
            Gui.func_73734_a(l + this.c, m + this.d, l + this.c + 22, m + this.d + 1, c);
            Gui.func_73734_a(l + this.c, m + this.d + 21, l + this.c + 22, m + this.d + 22, c);
            Gui.func_73734_a(l + this.c, m + this.d, l + this.c + 1, m + this.d + 22, c);
            Gui.func_73734_a(l + this.c + 21, m + this.d, l + this.c + 22, m + this.d + 22, c);
        }
        this.mc.field_71466_p.func_78276_b(p, l + this.c + 8, m + this.d + 8, -16777216 + ((int) (q * this.h) << 16) + ((int) (r * this.h) << 8) + (int) (s * this.h));
    }
}
