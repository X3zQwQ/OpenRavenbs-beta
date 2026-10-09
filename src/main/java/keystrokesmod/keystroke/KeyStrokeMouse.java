package keystrokesmod.keystroke;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import keystrokesmod.helper.MouseHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

public class KeyStrokeMouse {

    private static String[] a = new String[] { "LMB", "RMB" };

    private Minecraft mc = Minecraft.func_71410_x();

    private int c;

    private int d;

    private int e;

    private List<Long> f = new ArrayList<>();

    private boolean g = true;

    private long h = 0L;

    private int i = 255;

    private double j = 1.0;

    public KeyStrokeMouse(int k, int l, int m) {
        this.c = k;
        this.d = l;
        this.e = m;
    }

    public void n(int o, int p, int color) {
        boolean r = Mouse.isButtonDown(this.c);
        String s = a[this.c];
        if (r != this.g) {
            this.g = r;
            this.h = System.currentTimeMillis();
            if (r) {
                this.f.add(this.h);
            }
        }
        if (r) {
            this.i = Math.min(255, (int) (2L * (System.currentTimeMillis() - this.h)));
            this.j = Math.max(0.0, 1.0 - (System.currentTimeMillis() - this.h) / 20.0);
        } else {
            this.i = Math.max(0, 255 - (int) (2L * (System.currentTimeMillis() - this.h)));
            this.j = Math.min(1.0, (System.currentTimeMillis() - this.h) / 20.0);
        }
        int t = color >> 16 & 0xFF;
        int u = color >> 8 & 0xFF;
        int v = color & 0xFF;
        int c = new Color(t, u, v).getRGB();
        Gui.func_73734_a(o + this.d, p + this.e, o + this.d + 34, p + this.e + 22, 2013265920 + (this.i << 16) + (this.i << 8) + this.i);
        if (KeyStroke.f) {
            Gui.func_73734_a(o + this.d, p + this.e, o + this.d + 34, p + this.e + 1, c);
            Gui.func_73734_a(o + this.d, p + this.e + 21, o + this.d + 34, p + this.e + 22, c);
            Gui.func_73734_a(o + this.d, p + this.e, o + this.d + 1, p + this.e + 22, c);
            Gui.func_73734_a(o + this.d + 33, p + this.e, o + this.d + 34, p + this.e + 22, c);
        }
        this.mc.field_71466_p.func_78276_b(s, o + this.d + 8, p + this.e + 4, -16777216 + ((int) (t * this.j) << 16) + ((int) (u * this.j) << 8) + (int) (v * this.j));
        String w = MouseHelper.f() + " CPS";
        String x = MouseHelper.i() + " CPS";
        int y = this.mc.field_71466_p.func_78256_a(w);
        int z = this.mc.field_71466_p.func_78256_a(x);
        boolean a2 = this.c == 0;
        int b2 = a2 ? y : z;
        GL11.glScalef(0.5F, 0.5F, 0.5F);
        this.mc.field_71466_p.func_78276_b(a2 ? w : x, (o + this.d + 17) * 2 - b2 / 2, (p + this.e + 14) * 2, -16777216 + ((int) (255.0 * this.j) << 16) + ((int) (255.0 * this.j) << 8) + (int) (255.0 * this.j));
        GL11.glScalef(2.0F, 2.0F, 2.0F);
    }
}
