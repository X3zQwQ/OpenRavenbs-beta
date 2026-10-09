package keystrokesmod.module.impl.render;

import java.awt.Color;
import java.io.IOException;
import keystrokesmod.module.Module;
import keystrokesmod.module.impl.world.AntiBot;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.RenderUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.client.config.GuiButtonExt;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.RenderTickEvent;
import org.lwjgl.opengl.GL11;

public class Radar extends Module {

    private static final float BASE_SIZE = 100.0F;

    private static final float DEFAULT_POS_X = 5.0F;

    private static final float DEFAULT_POS_Y = 70.0F;

    private static final float CORNER_RADIUS = 8.0F;

    private static final int BACKGROUND_COLOR = new Color(0, 0, 0, 125).getRGB();

    private static final int OUTLINE_COLOR = Color.WHITE.getRGB();

    private final SliderSetting range;

    private final SliderSetting radarScale;

    private final ButtonSetting tracerLines;

    private float posX = Float.NaN;

    private float posY = Float.NaN;

    private float relativePosX = Float.NaN;

    private float relativePosY = Float.NaN;

    public Radar() {
        super("Radar", Module.category.render);
        this.registerSetting(this.range = new SliderSetting("Range", " block", 20.0, 8.0, 256.0, 1.0));
        this.registerSetting(this.radarScale = new SliderSetting("Scale", 1.0, 0.5, 2.0, 0.1));
        this.registerSetting(this.tracerLines = new ButtonSetting("Show tracer lines", false));
        this.registerSetting(new ButtonSetting("Edit position", () -> mc.func_147108_a(new Radar.EditScreen())));
    }

    @SubscribeEvent
    public void onRenderTick(RenderTickEvent event) {
        if (event.phase == Phase.END && Utils.nullCheck()) {
            if (mc.field_71462_r == null && !mc.field_71474_y.field_74330_P) {
                this.renderRadar(false);
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
        this.setAbsolutePosition(5.0F, 70.0F);
    }

    private void renderRadar(boolean editing) {
        ScaledResolution resolution = new ScaledResolution(mc);
        this.syncPositionToResolution(resolution);
        float size = this.getRadarSize();
        float centerX = this.posX + size / 2.0F;
        float centerY = this.posY + size / 2.0F;
        float visualScale = this.getRadarScale();
        RenderUtils.drawRoundedGradientOutlinedRectangle(this.posX, this.posY, this.posX + size, this.posY + size, 8.0F * visualScale, BACKGROUND_COLOR, OUTLINE_COLOR, OUTLINE_COLOR);
        RenderUtils.drawPolygon(centerX, centerY + 2.0F * visualScale, 5.0F * visualScale, 3, Color.WHITE.getRGB());
        double selectedRange = this.range.getInput();
        double selectedRangeSquared = selectedRange * selectedRange;
        double usableRadius = Math.max(1.0, (double) (size / 2.0F - 5.0F * visualScale));
        for (EntityPlayer player : mc.field_71441_e.field_73010_i) {
            if (player != mc.field_71439_g && player.field_70725_aQ == 0 && !AntiBot.isBot(player)) {
                double deltaX = player.field_70165_t - mc.field_71439_g.field_70165_t;
                double deltaZ = player.field_70161_v - mc.field_71439_g.field_70161_v;
                double horizontalDistanceSquared = deltaX * deltaX + deltaZ * deltaZ;
                if (!(horizontalDistanceSquared > selectedRangeSquared)) {
                    double playerAngle = mc.field_71439_g.field_70177_z + Math.toDegrees(Math.atan2(deltaX, deltaZ));
                    double radarDistance = Math.sqrt(horizontalDistanceSquared) / selectedRange * usableRadius;
                    double xOffset = radarDistance * Math.sin(Math.toRadians(playerAngle));
                    double yOffset = radarDistance * Math.cos(Math.toRadians(playerAngle));
                    double markerX = centerX - xOffset;
                    double markerY = centerY - yOffset;
                    if (this.tracerLines.isToggled()) {
                        this.drawTracer(centerX, centerY, markerX, markerY);
                    }
                    RenderUtils.drawPolygon(markerX, markerY, 3.0F * visualScale, 4, Color.RED.getRGB());
                }
            }
        }
    }

    private void drawTracer(double startX, double startY, double endX, double endY) {
        GL11.glPushMatrix();
        GL11.glPushAttrib(1048575);
        GL11.glEnable(3042);
        GL11.glEnable(2848);
        GL11.glDisable(2929);
        GL11.glDisable(3553);
        GL11.glBlendFunc(770, 771);
        GL11.glLineWidth(0.5F);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glBegin(1);
        GL11.glVertex2d(startX, startY);
        GL11.glVertex2d(endX, endY);
        GL11.glEnd();
        GL11.glPopAttrib();
        GL11.glPopMatrix();
    }

    private float getRadarScale() {
        return (float) this.radarScale.getInput();
    }

    private float getRadarSize() {
        return 100.0F * this.getRadarScale();
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
                this.relativePosX = 5.0F / scaledWidth;
                this.relativePosY = 70.0F / scaledHeight;
            }
        }
        this.posX = this.relativePosX * scaledWidth;
        this.posY = this.relativePosY * scaledHeight;
        this.clampPosition(resolution);
    }

    private void setAbsolutePosition(float absoluteX, float absoluteY, ScaledResolution resolution) {
        this.posX = absoluteX;
        this.posY = absoluteY;
        this.clampPosition(resolution);
        this.updateRelativePosition(resolution);
    }

    private void clampPosition(ScaledResolution resolution) {
        float maxX = Math.max(0.0F, resolution.func_78326_a() - this.getRadarSize());
        float maxY = Math.max(0.0F, resolution.func_78328_b() - this.getRadarSize());
        this.posX = Math.max(0.0F, Math.min(maxX, this.posX));
        this.posY = Math.max(0.0F, Math.min(maxY, this.posY));
    }

    private void updateRelativePosition(ScaledResolution resolution) {
        this.relativePosX = this.posX / Math.max(1, resolution.func_78326_a());
        this.relativePosY = this.posY / Math.max(1, resolution.func_78328_b());
    }

    private class EditScreen extends GuiScreen {

        private GuiButtonExt resetPosition;

        private boolean dragging;

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
            Radar.this.syncPositionToResolution(new ScaledResolution(this.field_146297_k));
            this.actualX = Radar.this.posX;
            this.actualY = Radar.this.posY;
        }

        public void func_73863_a(int mouseX, int mouseY, float partialTicks) {
            ScaledResolution resolution = new ScaledResolution(this.field_146297_k);
            if (!this.dragging) {
                Radar.this.syncPositionToResolution(resolution);
                this.actualX = Radar.this.posX;
                this.actualY = Radar.this.posY;
            }
            func_73734_a(0, 0, this.field_146294_l, this.field_146295_m, -1308622848);
            Radar.this.setAbsolutePosition(this.actualX, this.actualY, resolution);
            Radar.this.renderRadar(true);
            this.actualX = Radar.this.posX;
            this.actualY = Radar.this.posY;
            String message = "Drag the radar to reposition it.";
            int textX = resolution.func_78326_a() / 2 - this.field_146289_q.func_78256_a(message) / 2;
            int textY = resolution.func_78328_b() / 2 - 10;
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
                } else {
                    float size = Radar.this.getRadarSize();
                    if (mouseX >= Radar.this.posX && mouseX <= Radar.this.posX + size && mouseY >= Radar.this.posY && mouseY <= Radar.this.posY + size) {
                        this.dragging = true;
                        this.lastMouseX = mouseX;
                        this.lastMouseY = mouseY;
                        this.lastActualX = this.actualX;
                        this.lastActualY = this.actualY;
                    }
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
                Radar.this.resetPosition();
                this.actualX = Radar.this.posX;
                this.actualY = Radar.this.posY;
            }
        }

        public boolean func_73868_f() {
            return false;
        }
    }
}
