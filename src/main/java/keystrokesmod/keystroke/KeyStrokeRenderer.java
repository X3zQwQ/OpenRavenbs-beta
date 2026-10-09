package keystrokesmod.keystroke;

import java.awt.Color;
import java.io.IOException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.RenderTickEvent;

public class KeyStrokeRenderer {

    private static final int[] a = new int[] { 16777215, 16711680, 65280, 255, 16776960, 11141290 };

    private Minecraft mc = Minecraft.func_71410_x();

    private KeyStrokeKeyRenderer[] b = new KeyStrokeKeyRenderer[4];

    private KeyStrokeMouse[] c = new KeyStrokeMouse[2];

    public KeyStrokeRenderer() {
        this.b[0] = new KeyStrokeKeyRenderer(this.mc.field_71474_y.field_74351_w, 26, 2);
        this.b[1] = new KeyStrokeKeyRenderer(this.mc.field_71474_y.field_74368_y, 26, 26);
        this.b[2] = new KeyStrokeKeyRenderer(this.mc.field_71474_y.field_74370_x, 2, 26);
        this.b[3] = new KeyStrokeKeyRenderer(this.mc.field_71474_y.field_74366_z, 50, 26);
        this.c[0] = new KeyStrokeMouse(0, 2, 50);
        this.c[1] = new KeyStrokeMouse(1, 38, 50);
    }

    @SubscribeEvent
    public void onRenderTick(RenderTickEvent e) {
        if (this.mc.field_71462_r != null) {
            if (this.mc.field_71462_r instanceof KeyStrokeConfigGui) {
                try {
                    this.mc.field_71462_r.func_146269_k();
                } catch (IOException exception) {
                }
            }
        } else if (this.mc.field_71415_G && !this.mc.field_71474_y.field_74330_P) {
            this.renderKeystrokes();
        }
    }

    public void renderKeystrokes() {
        if (KeyStroke.e) {
            int x = KeyStroke.x;
            int y = KeyStroke.y;
            int g = this.getColor(KeyStroke.currentColorNumber);
            boolean h = KeyStroke.d;
            ScaledResolution res = new ScaledResolution(this.mc);
            int width = 74;
            int height = h ? 74 : 50;
            if (x < 0) {
                KeyStroke.x = 0;
                x = KeyStroke.x;
            } else if (x > res.func_78326_a() - width) {
                KeyStroke.x = res.func_78326_a() - width;
                x = KeyStroke.x;
            }
            if (y < 0) {
                KeyStroke.y = 0;
                y = KeyStroke.y;
            } else if (y > res.func_78328_b() - height) {
                KeyStroke.y = res.func_78328_b() - height;
                y = KeyStroke.y;
            }
            this.drawMovementKeys(x, y, g);
            if (h) {
                this.drawMouseButtons(x, y, g);
            }
        }
    }

    private int getColor(int index) {
        return index == 6 ? Color.getHSBColor((float) (System.currentTimeMillis() % 3750L) / 3750.0F, 1.0F, 1.0F).getRGB() : a[index];
    }

    private void drawMovementKeys(int x, int y, int textColor) {
        for (KeyStrokeKeyRenderer key : this.b) {
            key.renderKey(x, y, textColor);
        }
    }

    private void drawMouseButtons(int x, int y, int textColor) {
        for (KeyStrokeMouse button : this.c) {
            button.n(x, y, textColor);
        }
    }
}
