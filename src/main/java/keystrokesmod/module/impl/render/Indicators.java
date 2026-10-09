package keystrokesmod.module.impl.render;

import java.awt.Color;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import keystrokesmod.mixin.impl.accessor.IAccessorEntityArrow;
import keystrokesmod.mixin.impl.accessor.IAccessorEntityRenderer;
import keystrokesmod.mixin.impl.accessor.IAccessorMinecraft;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.GroupSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.RenderUtils;
import keystrokesmod.utility.Utils;
import keystrokesmod.utility.font.FontManager;
import keystrokesmod.utility.font.RavenFontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityEnderPearl;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.projectile.EntityEgg;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.entity.projectile.EntityLargeFireball;
import net.minecraft.entity.projectile.EntitySnowball;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Vec3;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.RenderTickEvent;
import org.lwjgl.opengl.GL11;

public class Indicators extends Module {

    private GroupSetting items;

    private ButtonSetting renderArrows;

    private ButtonSetting renderPearls;

    private ButtonSetting renderFireballs;

    private ButtonSetting renderEggs;

    private ButtonSetting renderSnowballs;

    private SliderSetting arrow;

    private SliderSetting radius;

    private SliderSetting font;

    private ButtonSetting itemColors;

    private ButtonSetting renderItem;

    private ButtonSetting renderDistance;

    private ButtonSetting onlyWhenApproaching;

    private ButtonSetting renderOnlyOffScreen;

    private static final int APPROACH_INTERVAL_TICKS = 5;

    private static final double MIN_NET_TOWARD_BLOCKS = 1.0;

    private int tickCounter;

    private final Map<Entity, Vec3> lastPosition = new HashMap<>();

    private final Set<Entity> entitiesToRender = new HashSet<>();

    private String[] arrowTypes = new String[] { "Caret", "Greater than", "Triangle" };

    public Indicators() {
        super("Indicators", Module.category.render);
        this.liteModule = true;
        this.registerSetting(this.arrow = new SliderSetting("Arrow", 0, this.arrowTypes));
        this.registerSetting(this.radius = new SliderSetting("Circle radius", 50.0, 30.0, 200.0, 5.0));
        this.registerSetting(this.font = FontManager.createFontSetting("Font"));
        this.registerSetting(this.itemColors = new ButtonSetting("Item colors", true));
        this.registerSetting(this.renderItem = new ButtonSetting("Render item", true));
        this.registerSetting(this.renderDistance = new ButtonSetting("Render distance", true));
        this.registerSetting(this.onlyWhenApproaching = new ButtonSetting("Only when approaching", false));
        this.registerSetting(this.renderOnlyOffScreen = new ButtonSetting("Render only offscreen", false));
        this.registerSetting(this.items = new GroupSetting("Items"));
        this.registerSetting(this.renderArrows = new ButtonSetting(this.items, "Render arrows", true));
        this.registerSetting(this.renderPearls = new ButtonSetting(this.items, "Render ender pearls", true));
        this.registerSetting(this.renderFireballs = new ButtonSetting(this.items, "Render fireballs", true));
        this.registerSetting(this.renderEggs = new ButtonSetting(this.items, "Render eggs", false));
        this.registerSetting(this.renderSnowballs = new ButtonSetting(this.items, "Render snowballs", false));
    }

    @Override
    public void onDisable() {
        this.lastPosition.clear();
        this.entitiesToRender.clear();
    }

    @SubscribeEvent
    public void onClientTick(ClientTickEvent event) {
        if (event.phase == Phase.END) {
            if (Utils.nullCheck()) {
                this.tickCounter++;
                if (this.tickCounter % 5 == 0) {
                    Set<Entity> seen = new HashSet<>();
                    this.entitiesToRender.clear();
                    double px = mc.field_71439_g.field_70165_t;
                    double py = mc.field_71439_g.field_70163_u;
                    double pz = mc.field_71439_g.field_70161_v;
                    for (Entity en : mc.field_71441_e.field_72996_f) {
                        if (en != null && en != mc.field_71439_g) {
                            ItemStack itemStack = this.getTrackedItemStack(en);
                            if (itemStack != null && this.canRender(en)) {
                                seen.add(en);
                                Vec3 posThen = this.lastPosition.get(en);
                                if (this.onlyWhenApproaching.isToggled()) {
                                    if (posThen == null) {
                                        this.lastPosition.put(en, new Vec3(en.field_70165_t, en.field_70163_u, en.field_70161_v));
                                        continue;
                                    }
                                    double distanceThen = Math.sqrt((px - posThen.field_72450_a) * (px - posThen.field_72450_a) + (py - posThen.field_72448_b) * (py - posThen.field_72448_b) + (pz - posThen.field_72449_c) * (pz - posThen.field_72449_c));
                                    double distanceNow = mc.field_71439_g.func_70032_d(en);
                                    if (distanceThen - distanceNow <= 1.0) {
                                        this.lastPosition.put(en, new Vec3(en.field_70165_t, en.field_70163_u, en.field_70161_v));
                                        continue;
                                    }
                                }
                                this.entitiesToRender.add(en);
                                this.lastPosition.put(en, new Vec3(en.field_70165_t, en.field_70163_u, en.field_70161_v));
                            }
                        }
                    }
                    this.lastPosition.keySet().retainAll(seen);
                }
            }
        }
    }

    @SubscribeEvent
    public void onRenderTick(RenderTickEvent event) {
        if (event.phase == Phase.END) {
            if (mc.field_71462_r == null && Utils.nullCheck()) {
                try {
                    for (Entity en : this.entitiesToRender) {
                        ItemStack itemStack = this.getTrackedItemStack(en);
                        if (itemStack != null) {
                            this.renderIndicatorFor(en, itemStack, event.renderTickTime);
                        }
                    }
                } catch (Exception exception) {
                }
            }
        }
    }

    private ItemStack getTrackedItemStack(Entity en) {
        if (en == null) {
            return null;
        } else if (en instanceof EntityArrow) {
            return ((IAccessorEntityArrow) en).getInGround() ? null : new ItemStack(Items.field_151032_g);
        } else if (en instanceof EntityFireball) {
            return new ItemStack(Items.field_151059_bz);
        } else if (en instanceof EntityEnderPearl) {
            return new ItemStack(Items.field_151079_bi);
        } else if (en instanceof EntityEgg) {
            return new ItemStack(Items.field_151110_aK);
        } else {
            return en instanceof EntitySnowball ? new ItemStack(Items.field_151126_ay) : null;
        }
    }

    private boolean canRender(Entity entity) {
        if (entity instanceof EntityArrow && !((IAccessorEntityArrow) entity).getInGround() && this.renderArrows.isToggled()) {
            return true;
        } else if (entity instanceof EntityLargeFireball && this.renderFireballs.isToggled()) {
            return true;
        } else if (entity instanceof EntityEnderPearl && this.renderPearls.isToggled()) {
            return true;
        } else {
            return entity instanceof EntityEgg && this.renderEggs.isToggled() ? true : entity instanceof EntitySnowball && this.renderSnowballs.isToggled();
        }
    }

    private void renderIndicatorFor(Entity en, ItemStack itemStack, float partialTicks) {
        if (this.canRender(en)) {
            if (this.shouldRender(en, itemStack)) {
                if (!this.renderOnlyOffScreen.isToggled() || !RenderUtils.isInViewFrustum(en)) {
                    Color colorForStack = this.getColorForItem(itemStack);
                    int color = this.itemColors.isToggled() ? colorForStack.getRGB() : -1;
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
                        if (this.renderItem.isToggled()) {
                            radiusInput += 20.0;
                        }
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
                                GL11.glColor3d(colorForStack.getRed(), colorForStack.getGreen(), colorForStack.getBlue());
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
                            RavenFontRenderer fr = this.getIndicatorFontRenderer();
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
                            RavenFontRenderer fr = this.getIndicatorFontRenderer();
                            fr.drawString(text, -fr.getStringWidth(text) / 2, -4.0F, -1, true);
                        }
                        GlStateManager.func_179121_F();
                        if (this.renderItem.isToggled() && itemStack != null) {
                            GlStateManager.func_179094_E();
                            if (itemStack.func_77973_b() == Items.field_151032_g) {
                                renderX = baseX + (radiusInput - 26.0) * sinAng;
                                renderY = baseY + (radiusInput - 26.0) * cosAng;
                                GlStateManager.func_179137_b(renderX, renderY, 0.0);
                                GlStateManager.func_179152_a(1.0F, 1.0F, 1.0F);
                                GlStateManager.func_179114_b((float) angle2 - 45.0F, 0.0F, 0.0F, 1.0F);
                                mc.func_175599_af().func_175042_a(itemStack, -12, -4);
                            } else {
                                renderX = baseX + (radiusInput - 29.0) * sinAng;
                                renderY = baseY + (radiusInput - 29.0) * cosAng;
                                GlStateManager.func_179137_b(renderX, renderY, 0.0);
                                GlStateManager.func_179152_a(1.0F, 1.0F, 1.0F);
                                mc.func_175599_af().func_175042_a(itemStack, -8, -9);
                            }
                            GlStateManager.func_179121_F();
                        }
                    }
                }
            }
        }
    }

    private Color getColorForItem(ItemStack itemStack) {
        if (itemStack == null) {
            return Color.WHITE;
        } else if (itemStack.func_77973_b() == Items.field_151079_bi) {
            return new Color(210, 0, 255);
        } else if (itemStack.func_77973_b() == Items.field_151059_bz) {
            return new Color(255, 150, 0);
        } else {
            return itemStack.func_77973_b() == Items.field_151110_aK ? new Color(255, 238, 154) : Color.WHITE;
        }
    }

    private boolean shouldRender(Entity en, ItemStack stack) {
        return true;
    }

    private String getSelectedFontName() {
        if (this.font == null) {
            return FontManager.getDefaultFontName();
        } else {
            int index = (int) Math.max(0.0, Math.min((double) (this.font.getOptions().length - 1), this.font.getInput()));
            return this.font.getOptions()[index];
        }
    }

    private RavenFontRenderer getIndicatorFontRenderer() {
        return FontManager.getNametagRenderer(this.getSelectedFontName());
    }
}
