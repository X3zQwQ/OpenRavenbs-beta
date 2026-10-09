package keystrokesmod.module.impl.render;

import java.awt.Color;
import java.text.DecimalFormat;
import java.util.ArrayList;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.Utils;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityTNTPrimed;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import org.lwjgl.opengl.GL11;

public class TNTTimer extends Module {

    private static final int BEDWARS_FUSE_OFFSET = 28;

    private static final float BEDWARS_MAX_FUSE = 52.0F;

    private static final float NORMAL_MAX_FUSE = 80.0F;

    private final SliderSetting scale;

    private final DecimalFormat timeFormatter = new DecimalFormat("0.0");

    private final ArrayList<EntityTNTPrimed> trackedTnt = new ArrayList<>();

    private int trackedTntCount = 0;

    private boolean trackedBedwars = false;

    public TNTTimer() {
        super("TNT Timer", Module.category.render, 0);
        this.registerSetting(this.scale = new SliderSetting("Scale", 1.0, 0.5, 3.0, 0.1));
    }

    @SubscribeEvent
    public void onClientTick(ClientTickEvent event) {
        if (event.phase == Phase.END) {
            this.updateTrackedTnt();
        }
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent e) {
        if (Utils.nullCheck() && this.trackedTntCount != 0) {
            RenderManager rm = mc.func_175598_ae();
            FontRenderer fr = mc.field_71466_p;
            float partialTicks = e.partialTicks;
            boolean bedwars = this.trackedBedwars;
            for (int i = 0; i < this.trackedTntCount; i++) {
                EntityTNTPrimed tnt = this.trackedTnt.get(i);
                if (tnt != null && !tnt.field_70128_L) {
                    int fuse = bedwars ? tnt.field_70516_a - 28 : tnt.field_70516_a;
                    if (fuse >= 1) {
                        double x = tnt.field_70142_S + (tnt.field_70165_t - tnt.field_70142_S) * partialTicks - rm.field_78730_l;
                        double y = tnt.field_70137_T + (tnt.field_70163_u - tnt.field_70137_T) * partialTicks - rm.field_78731_m;
                        double z = tnt.field_70136_U + (tnt.field_70161_v - tnt.field_70136_U) * partialTicks - rm.field_78728_n;
                        this.renderTimer(fr, rm, tnt, x, y, z, fuse, partialTicks, bedwars);
                    }
                }
            }
        }
    }

    private void updateTrackedTnt() {
        this.trackedTntCount = 0;
        this.trackedBedwars = false;
        if (Utils.nullCheck() && mc.field_71441_e != null) {
            this.trackedBedwars = this.isSidebarBedwars();
            for (Entity entity : mc.field_71441_e.field_72996_f) {
                if (entity instanceof EntityTNTPrimed) {
                    EntityTNTPrimed tnt = (EntityTNTPrimed) entity;
                    int fuse = this.trackedBedwars ? tnt.field_70516_a - 28 : tnt.field_70516_a;
                    if (fuse >= 1 && !(tnt.func_70068_e(mc.field_71439_g) > 4096.0)) {
                        if (this.trackedTntCount >= this.trackedTnt.size()) {
                            this.trackedTnt.add(tnt);
                        } else {
                            this.trackedTnt.set(this.trackedTntCount, tnt);
                        }
                        this.trackedTntCount++;
                    }
                }
            }
        }
    }

    private void renderTimer(FontRenderer fr, RenderManager rm, EntityTNTPrimed tnt, double x, double y, double z, int fuse, float partialTicks, boolean bedwars) {
        String time = this.timeFormatter.format((fuse - partialTicks) / 20.0F);
        float maxFuse = bedwars ? 52.0F : 80.0F;
        float green = Math.min(fuse / maxFuse, 1.0F);
        Color color = new Color(1.0F - green, green, 0.0F);
        float s = 0.02666667F * (float) this.scale.getInput();
        GL11.glPushMatrix();
        GlStateManager.func_179109_b((float) x, (float) y + tnt.field_70131_O + 0.5F, (float) z);
        GL11.glNormal3f(0.0F, 1.0F, 0.0F);
        GlStateManager.func_179114_b(-rm.field_78735_i, 0.0F, 1.0F, 0.0F);
        GlStateManager.func_179114_b(rm.field_78732_j * (mc.field_71474_y.field_74320_O == 2 ? -1 : 1), 1.0F, 0.0F, 0.0F);
        GlStateManager.func_179152_a(-s, -s, s);
        GlStateManager.func_179140_f();
        GlStateManager.func_179132_a(false);
        GlStateManager.func_179097_i();
        GlStateManager.func_179147_l();
        GlStateManager.func_179120_a(770, 771, 1, 0);
        int halfWidth = fr.func_78256_a(time) >> 1;
        GlStateManager.func_179126_j();
        GlStateManager.func_179132_a(true);
        GlStateManager.func_179090_x();
        Tessellator tessellator = Tessellator.func_178181_a();
        WorldRenderer wr = tessellator.func_178180_c();
        wr.func_181668_a(7, DefaultVertexFormats.field_181706_f);
        wr.func_181662_b(-halfWidth - 1, -1.0, 0.0).func_181666_a(0.0F, 0.0F, 0.0F, 0.25F).func_181675_d();
        wr.func_181662_b(-halfWidth - 1, 8.0, 0.0).func_181666_a(0.0F, 0.0F, 0.0F, 0.25F).func_181675_d();
        wr.func_181662_b(halfWidth + 1, 8.0, 0.0).func_181666_a(0.0F, 0.0F, 0.0F, 0.25F).func_181675_d();
        wr.func_181662_b(halfWidth + 1, -1.0, 0.0).func_181666_a(0.0F, 0.0F, 0.0F, 0.25F).func_181675_d();
        tessellator.func_78381_a();
        GlStateManager.func_179098_w();
        fr.func_78276_b(time, -halfWidth, 0, color.getRGB());
        GlStateManager.func_179145_e();
        GlStateManager.func_179084_k();
        GlStateManager.func_179131_c(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glPopMatrix();
    }

    private boolean isSidebarBedwars() {
        if (mc.field_71441_e == null) {
            return false;
        } else {
            Scoreboard sb = mc.field_71441_e.func_96441_U();
            if (sb == null) {
                return false;
            } else {
                ScoreObjective objective = sb.func_96539_a(1);
                return objective != null && Utils.stripString(objective.func_96678_d()).contains("BED WARS");
            }
        }
    }
}
