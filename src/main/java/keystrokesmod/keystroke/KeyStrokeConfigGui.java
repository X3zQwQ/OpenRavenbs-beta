package keystrokesmod.keystroke;

import java.io.IOException;
import keystrokesmod.Raven;
import keystrokesmod.helper.MouseHelper;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

public class KeyStrokeConfigGui extends GuiScreen {

    private static final String[] colors = new String[] { "White", "Red", "Green", "Blue", "Yellow", "Purple", "Rainbow" };

    private GuiButton modeBtn;

    private GuiButton textColorBtn;

    private GuiButton showMouseBtn;

    private GuiButton outlineBtn;

    private boolean d = false;

    private int lx;

    private int ly;

    public void func_73866_w_() {
        this.field_146292_n.add(this.modeBtn = new GuiButton(0, this.field_146294_l / 2 - 70, this.field_146295_m / 2 - 28, 140, 20, "Mod: " + (KeyStroke.e ? "Enabled" : "Disabled")));
        this.field_146292_n.add(this.textColorBtn = new GuiButton(1, this.field_146294_l / 2 - 70, this.field_146295_m / 2 - 6, 140, 20, "Text color: " + colors[KeyStroke.currentColorNumber]));
        this.field_146292_n.add(this.showMouseBtn = new GuiButton(2, this.field_146294_l / 2 - 70, this.field_146295_m / 2 + 16, 140, 20, "Show mouse buttons: " + (KeyStroke.d ? "On" : "Off")));
        this.field_146292_n.add(this.outlineBtn = new GuiButton(3, this.field_146294_l / 2 - 70, this.field_146295_m / 2 + 38, 140, 20, "Outline: " + (KeyStroke.f ? "On" : "Off")));
    }

    public void func_73863_a(int mouseX, int mouseY, float partialTicks) {
        Raven.getKeyStrokeRenderer().renderKeystrokes();
        super.func_73863_a(mouseX, mouseY, partialTicks);
    }

    protected void func_146284_a(GuiButton button) {
        if (button == this.modeBtn) {
            KeyStroke.e = !KeyStroke.e;
            this.modeBtn.field_146126_j = "Mod: " + (KeyStroke.e ? "Enabled" : "Disabled");
        } else if (button == this.textColorBtn) {
            KeyStroke.currentColorNumber = KeyStroke.currentColorNumber == 6 ? 0 : KeyStroke.currentColorNumber + 1;
            this.textColorBtn.field_146126_j = "Text color: " + colors[KeyStroke.currentColorNumber];
        } else if (button == this.showMouseBtn) {
            KeyStroke.d = !KeyStroke.d;
            this.showMouseBtn.field_146126_j = "Show mouse buttons: " + (KeyStroke.d ? "On" : "Off");
        } else if (button == this.outlineBtn) {
            KeyStroke.f = !KeyStroke.f;
            this.outlineBtn.field_146126_j = "Outline: " + (KeyStroke.f ? "On" : "Off");
        }
    }

    protected void func_73864_a(int mouseX, int mouseY, int button) {
        try {
            super.func_73864_a(mouseX, mouseY, button);
        } catch (IOException exception) {
        }
        if (button == 0) {
            MouseHelper.aL();
            int startX = KeyStroke.x;
            int startY = KeyStroke.y;
            int endX = startX + 74;
            int endY = startY + (KeyStroke.d ? 74 : 50);
            if (mouseX >= startX && mouseX <= endX && mouseY >= startY && mouseY <= endY) {
                this.d = true;
                this.lx = mouseX;
                this.ly = mouseY;
            }
        } else if (button == 1) {
            MouseHelper.aR();
        }
    }

    protected void func_146286_b(int mouseX, int mouseY, int action) {
        super.func_146286_b(mouseX, mouseY, action);
        this.d = false;
    }

    protected void func_146273_a(int mouseX, int mouseY, int lastButtonClicked, long timeSinceMouseClick) {
        super.func_146273_a(mouseX, mouseY, lastButtonClicked, timeSinceMouseClick);
        if (this.d) {
            KeyStroke.x = KeyStroke.x + mouseX - this.lx;
            KeyStroke.y = KeyStroke.y + mouseY - this.ly;
            this.lx = mouseX;
            this.ly = mouseY;
        }
    }

    public boolean func_73868_f() {
        return false;
    }
}
