package keystrokesmod.module.impl.render;

import java.awt.Color;
import java.util.ArrayList;
import keystrokesmod.mixin.impl.accessor.IAccessorEntityRenderer;
import keystrokesmod.mixin.impl.accessor.IAccessorMinecraft;
import keystrokesmod.module.Module;
import keystrokesmod.module.impl.world.AntiBot;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.RenderUtils;
import keystrokesmod.utility.Utils;
import keystrokesmod.utility.font.FontManager;
import keystrokesmod.utility.font.RavenFontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.Vec3;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.RenderTickEvent;
import org.lwjgl.opengl.GL11;

public class Arrows extends Module {

    private SliderSetting arrow;

    private SliderSetting range;

    private SliderSetting radius;

    private SliderSetting font;

    private ButtonSetting teamColor;

    private ButtonSetting hideTeammates;

    private ButtonSetting renderFriends;

    private ButtonSetting renderEnemies;

    private ButtonSetting renderDistance;

    private ButtonSetting renderOnlyOffScreen;

    private int friendColor = new Color(0, 255, 0, 255).getRGB();

    private int enemyColor = new Color(255, 0, 0, 255).getRGB();

    private final ArrayList<Arrows.ArrowRenderState> renderStates = new ArrayList<>();

    private int renderStateCount = 0;

    private String[] arrowTypes = new String[] { "Caret", "Greater than", "Triangle" };

    public Arrows() {
        super("Arrows", Module.category.render);
        this.registerSetting(this.arrow = new SliderSetting("Arrow", 0, this.arrowTypes));
        this.registerSetting(this.radius = new SliderSetting("Circle radius", 50.0, 30.0, 200.0, 5.0));
        this.registerSetting(this.range = new SliderSetting("Range", " block", 200.0, 25.0, 300.0, 5.0));
        this.registerSetting(this.font = FontManager.createFontSetting("Font"));
        this.registerSetting(this.teamColor = new ButtonSetting("Team color", true));
        this.registerSetting(this.hideTeammates = new ButtonSetting("Hide teammates", true));
        this.registerSetting(this.renderFriends = new ButtonSetting("Render friends (green)", true));
        this.registerSetting(this.renderEnemies = new ButtonSetting("Render enemies (red)", true));
        this.registerSetting(this.renderDistance = new ButtonSetting("Render distance", true));
        this.registerSetting(this.renderOnlyOffScreen = new ButtonSetting("Render only offscreen", false));
    }

    @SubscribeEvent
    public void onClientTick(ClientTickEvent event) {
        if (event.phase == Phase.END) {
            this.updateRenderStates();
        }
    }

    @SubscribeEvent
    public void onRenderTick(RenderTickEvent event) {
        if (event.phase == Phase.END) {
            if (mc.field_71462_r == null && Utils.nullCheck()) {
                try {
                    for (int i = 0; i < this.renderStateCount; i++) {
                        Arrows.ArrowRenderState renderState = this.renderStates.get(i);
                        if (renderState.player != null) {
                            this.renderIndicatorFor(renderState.player, renderState.color, event.renderTickTime);
                        }
                    }
                } catch (Exception exception) {
                }
            }
        }
    }

    private void updateRenderStates() {
        this.renderStateCount = 0;
        if (Utils.nullCheck() && mc.field_71441_e != null) {
            for (EntityPlayer en : mc.field_71441_e.field_73010_i) {
                if (en != null && en != mc.field_71439_g && !AntiBot.isBot(en) && (!Utils.isTeammate(en) || !this.hideTeammates.isToggled()) && !(mc.field_71439_g.func_70032_d(en) > this.range.getInput())) {
                    int color = -1;
                    if (this.renderFriends.isToggled() && Utils.isFriended(en)) {
                        color = this.friendColor;
                    } else if (this.renderEnemies.isToggled() && Utils.isEnemy(en)) {
                        color = this.enemyColor;
                    } else if (this.teamColor.isToggled()) {
                        color = Utils.getColorFromEntity(en);
                    }
                    if (this.renderStateCount >= this.renderStates.size()) {
                        this.renderStates.add(new Arrows.ArrowRenderState());
                    }
                    this.renderStates.get(this.renderStateCount++).set(en, color);
                }
            }
        }
    }

    private void renderIndicatorFor(EntityPlayer en, int color, float partialTicks) {
        if (!this.renderOnlyOffScreen.isToggled() || !RenderUtils.isInViewFrustum(en)) {
            double x = en.field_70142_S + (en.field_70165_t - en.field_70142_S) * partialTicks - mc.func_175598_ae().field_78730_l;
            double y = en.field_70137_T + (en.field_70163_u - en.field_70137_T) * partialTicks - mc.func_175598_ae().field_78731_m + en.field_70131_O / 2.0F;
            double z = en.field_70136_U + (en.field_70161_v - en.field_70136_U) * partialTicks - mc.func_175598_ae().field_78728_n;
            ((IAccessorEntityRenderer) mc.field_71460_t).callSetupCameraTransform(((IAccessorMinecraft) mc).getTimer().field_74281_c, 0);
            ScaledResolution scaledResolution = new ScaledResolution(mc);
            Vec3 vec = RenderUtils.convertTo2D(scaledResolution.func_78325_e(), x, y, z);
            if (vec != null) {
                mc.field_71460_t.func_78478_c();
                ScaledResolution res = new ScaledResolution(mc);
                double dx = vec.field_72450_a - res.func_78326_a() / 2.0;
                double dy = vec.field_72448_b - res.func_78328_b() / 2.0;
                boolean inFrustum = vec.field_72449_c < 1.0003684;
                if (!inFrustum) {
                    dx *= -1.0;
                    dy *= -1.0;
                }
                double angle1 = Math.atan2(dx, dy);
                double angle2 = Math.atan2(dy, dx) * (float) (180.0 / Math.PI) + 90.0;
                double hypotenuse = Math.hypot(dx, dy);
                double radiusInput = this.radius.getInput();
                if (inFrustum && hypotenuse < radiusInput + 15.0) {
                    return;
                }
                double baseX = res.func_78326_a() / 2.0;
                double baseY = res.func_78328_b() / 2.0;
                double sinAng = Math.sin(angle1);
                double cosAng = Math.cos(angle1);
                double renderX = baseX + radiusInput * sinAng;
                double renderY = baseY + radiusInput * cosAng;
                GlStateManager.func_179094_E();
                GlStateManager.func_179137_b(renderX, renderY, 0.0);
                GlStateManager.func_179114_b((float) angle2, 0.0F, 0.0F, 1.0F);
                GlStateManager.func_179152_a(1.0F, 1.0F, 1.0F);
                int arrowInput = (int) this.arrow.getInput();
                if (arrowInput == 0) {
                    if (color == -1) {
                        GL11.glColor3d(1.0, 1.0, 1.0);
                    } else {
                        float red = (color >> 16 & 0xFF) / 255.0F;
                        float green = (color >> 8 & 0xFF) / 255.0F;
                        float blue = (color & 0xFF) / 255.0F;
                        GL11.glColor4f(red, green, blue, 1.0F);
                    }
                    GL11.glEnable(3042);
                    GL11.glDisable(3553);
                    GL11.glBlendFunc(770, 771);
                    GL11.glEnable(2848);
                    double halfAngle = 0.61086524F;
                    double size = 9.0;
                    double offsetY = 5.0;
                    GL11.glLineWidth(3.0F);
                    GL11.glBegin(3);
                    GL11.glVertex2d(Math.sin(-halfAngle) * size, Math.cos(-halfAngle) * size - offsetY);
                    GL11.glVertex2d(0.0, -offsetY);
                    GL11.glVertex2d(Math.sin(halfAngle) * size, Math.cos(halfAngle) * size - offsetY);
                    GL11.glEnd();
                    GL11.glEnable(3553);
                    GL11.glDisable(3042);
                    GL11.glDisable(2848);
                } else if (arrowInput == 1) {
                    GlStateManager.func_179114_b(-90.0F, 0.0F, 0.0F, 1.0F);
                    GlStateManager.func_179139_a(1.5, 1.5, 1.5);
                    RavenFontRenderer fr = this.getArrowFontRenderer();
                    fr.drawString(">", -2.0F, -4.0F, color, false);
                } else if (arrowInput == 2) {
                    RenderUtils.draw2DPolygon(0.0, 0.0, 5.0, 3, Utils.mergeAlpha(color, 255));
                }
                GlStateManager.func_179121_F();
                renderX = baseX + (radiusInput - 13.0) * sinAng;
                renderY = baseY + (radiusInput - 13.0) * cosAng;
                GlStateManager.func_179094_E();
                GlStateManager.func_179137_b(renderX, renderY, 0.0);
                GlStateManager.func_179139_a(0.8, 0.8, 0.8);
                if (this.renderDistance.isToggled()) {
                    String text = (int) mc.field_71439_g.func_70032_d(en) + "m";
                    RavenFontRenderer fr = this.getArrowFontRenderer();
                    fr.drawString(text, -fr.getStringWidth(text) / 2, -4.0F, -1, true);
                }
                GlStateManager.func_179121_F();
            }
        }
    }

    private String getSelectedFontName() {
        if (this.font == null) {
            return FontManager.getDefaultFontName();
        } else {
            int index = (int) Math.max(0.0, Math.min((double) (this.font.getOptions().length - 1), this.font.getInput()));
            return this.font.getOptions()[index];
        }
    }

    private RavenFontRenderer getArrowFontRenderer() {
        return FontManager.getNametagRenderer(this.getSelectedFontName());
    }

    private static final class ArrowRenderState {

        private EntityPlayer player;

        private int color;

        private ArrowRenderState() {
        }

        private void set(EntityPlayer player, int color) {
            this.player = player;
            this.color = color;
        }
    }
}
