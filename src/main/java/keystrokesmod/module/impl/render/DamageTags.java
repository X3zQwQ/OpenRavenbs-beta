package keystrokesmod.module.impl.render;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import keystrokesmod.event.ReceivePacketEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.RenderUtils;
import keystrokesmod.utility.Utils;
import keystrokesmod.utility.font.FontManager;
import keystrokesmod.utility.font.RavenFontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.DataWatcher.WatchableObject;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.server.S1CPacketEntityMetadata;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

public class DamageTags extends Module {

    private static final String[] DEPTH_MODES = new String[] { "Vanilla", "Visible", "Through walls" };

    private static final int HEALTH_WATCHER_ID = 6;

    private static final int ABSORPTION_WATCHER_ID = 17;

    private static final double MAX_RENDER_DISTANCE_SQ = 4096.0;

    private static final double RISE_DISTANCE = 1.0;

    private static final double FRUSTUM_AABB_EXPAND = 0.35;

    private static final double STACK_RADIUS_SQ = 0.36;

    private static final float HIDDEN_TEXT_ALPHA = 0.1254902F;

    private static final float FADE_START = 0.72F;

    private static final int MIN_FONT_ALPHA = 4;

    private final SliderSetting duration;

    private final SliderSetting scale;

    private final SliderSetting font;

    private final SliderSetting depthMode;

    private final ButtonSetting textShadow;

    private final ButtonSetting background;

    private final SliderSetting backgroundOpacity;

    private final List<DamageTags.DamageTag> activeTags = new ArrayList<>();

    private final Map<Integer, DamageTags.HealthSnapshot> trackedStates = new HashMap<>();

    private final ConcurrentLinkedQueue<DamageTags.QueuedMetadataUpdate> pendingUpdates = new ConcurrentLinkedQueue<>();

    private World lastWorld;

    public DamageTags() {
        super("Damage Tags", Module.category.render, 0);
        this.registerSetting(this.duration = new SliderSetting("Duration", " ms", 1200.0, 0.0, 2000.0, 100.0));
        this.registerSetting(this.scale = new SliderSetting("Scale", 1.0, 0.5, 3.0, 0.1));
        this.registerSetting(this.font = FontManager.createFontSetting("Font"));
        this.registerSetting(this.depthMode = new SliderSetting("Depth", 0, DEPTH_MODES));
        this.registerSetting(this.textShadow = new ButtonSetting("Text Shadow", false));
        this.registerSetting(this.background = new ButtonSetting("Background", true));
        this.registerSetting(this.backgroundOpacity = new SliderSetting("Background Opacity", 0.25, 0.0, 1.0, 0.05));
    }

    @Override
    public void guiUpdate() {
        this.backgroundOpacity.setVisible(this.background.isToggled(), this);
    }

    @Override
    public void onEnable() {
        this.resetState();
        this.lastWorld = mc.field_71441_e;
    }

    @Override
    public void onDisable() {
        this.resetState();
    }

    @SubscribeEvent
    public void onReceivePacket(ReceivePacketEvent e) {
        if (e.getPacket() instanceof S1CPacketEntityMetadata && mc.field_71441_e != null) {
            S1CPacketEntityMetadata packet = (S1CPacketEntityMetadata) e.getPacket();
            List<WatchableObject> watchedObjects = packet.func_149376_c();
            if (watchedObjects != null && !watchedObjects.isEmpty()) {
                boolean hasHealth = false;
                float health = 0.0F;
                boolean hasAbsorption = false;
                float absorption = 0.0F;
                for (WatchableObject watchedObject : watchedObjects) {
                    if (watchedObject != null && watchedObject.func_75669_b() != null) {
                        int dataValueId = watchedObject.func_75672_a();
                        if (dataValueId == 6 && watchedObject.func_75669_b() instanceof Float) {
                            hasHealth = true;
                            health = (Float) watchedObject.func_75669_b();
                        } else if (dataValueId == 17 && watchedObject.func_75669_b() instanceof Float) {
                            hasAbsorption = true;
                            absorption = (Float) watchedObject.func_75669_b();
                        }
                    }
                }
                if (hasHealth || hasAbsorption) {
                    this.pendingUpdates.offer(new DamageTags.QueuedMetadataUpdate(packet.func_149375_d(), hasHealth, health, hasAbsorption, absorption));
                }
            }
        }
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent e) {
        if (Utils.nullCheck()) {
            if (mc.field_71441_e != this.lastWorld) {
                this.resetState();
                this.lastWorld = mc.field_71441_e;
            }
            long now = System.currentTimeMillis();
            this.flushPendingUpdates(e.partialTicks, now);
            this.updateTrackedStates();
            this.pruneExpiredTags(now);
            if (!this.activeTags.isEmpty()) {
                RavenFontRenderer fontRenderer = this.getDamageTagFontRenderer();
                RenderManager renderManager = mc.func_175598_ae();
                if (fontRenderer != null && renderManager != null) {
                    double viewerX = renderManager.field_78730_l;
                    double viewerY = renderManager.field_78731_m;
                    double viewerZ = renderManager.field_78728_n;
                    int depthOrdinal = (int) this.depthMode.getInput();
                    float scaleMul = (float) this.scale.getInput();
                    boolean bgEnabled = this.background.isToggled();
                    float bgOpacitySlider = (float) this.backgroundOpacity.getInput();
                    GlStateManager.func_179123_a();
                    try {
                        for (DamageTags.DamageTag tag : this.activeTags) {
                            double dx = tag.x - viewerX;
                            double dy = tag.y - viewerY;
                            double dz = tag.z - viewerZ;
                            if (!(dx * dx + dy * dy + dz * dz > 4096.0) && RenderUtils.isInViewFrustum(tag.frustumAabb)) {
                                this.renderTag(tag, now, renderManager, fontRenderer, viewerX, viewerY, viewerZ, depthOrdinal, scaleMul, bgEnabled, bgOpacitySlider);
                            }
                        }
                    } finally {
                        GlStateManager.func_179099_b();
                    }
                }
            }
        }
    }

    private void flushPendingUpdates(float partialTicks, long nowMillis) {
        if (mc.field_71441_e == null) {
            this.pendingUpdates.clear();
        } else {
            DamageTags.QueuedMetadataUpdate update;
            while ((update = this.pendingUpdates.poll()) != null) {
                Entity entity = mc.field_71441_e.func_73045_a(update.entityId);
                if (entity instanceof EntityLivingBase && !(entity instanceof EntityArmorStand) && entity != mc.field_71439_g) {
                    EntityLivingBase living = (EntityLivingBase) entity;
                    DamageTags.HealthSnapshot previous = this.trackedStates.get(update.entityId);
                    float newHealth = update.hasHealth ? update.health : this.getHealth(living);
                    float newAbsorption = update.hasAbsorption ? update.absorption : this.getAbsorption(living);
                    DamageTags.HealthSnapshot current = new DamageTags.HealthSnapshot(newHealth, newAbsorption);
                    if (previous == null) {
                        this.trackedStates.put(update.entityId, current);
                    } else {
                        float delta = current.total() - previous.total();
                        this.trackedStates.put(update.entityId, current);
                        if (!(Math.abs(delta) <= 0.01F)) {
                            this.spawnTag(living, delta, partialTicks, nowMillis);
                        }
                    }
                } else {
                    this.trackedStates.remove(update.entityId);
                }
            }
        }
    }

    private void updateTrackedStates() {
        if (mc.field_71441_e == null) {
            this.trackedStates.clear();
        } else {
            this.trackedStates.entrySet().removeIf(entry -> {
                Entity entity2 = mc.field_71441_e.func_73045_a(entry.getKey());
                return !(entity2 instanceof EntityLivingBase) || entity2 instanceof EntityArmorStand || entity2 == mc.field_71439_g;
            });
            List<Entity> loaded = mc.field_71441_e.field_72996_f;
            int i = 0;
            for (int n = loaded.size(); i < n; i++) {
                Entity entity = loaded.get(i);
                if (entity instanceof EntityLivingBase && !(entity instanceof EntityArmorStand) && entity != mc.field_71439_g) {
                    EntityLivingBase living = (EntityLivingBase) entity;
                    this.trackedStates.put(entity.func_145782_y(), new DamageTags.HealthSnapshot(this.getHealth(living), this.getAbsorption(living)));
                }
            }
        }
    }

    private void pruneExpiredTags(long now) {
        Iterator<DamageTags.DamageTag> iterator = this.activeTags.iterator();
        while (iterator.hasNext()) {
            DamageTags.DamageTag tag = iterator.next();
            if (now - tag.createdAt >= tag.durationMs) {
                iterator.remove();
            }
        }
    }

    private void spawnTag(EntityLivingBase living, float delta, float partialTicks, long nowMillis) {
        double x = this.interpolate(living.field_70142_S, living.field_70165_t, partialTicks);
        double y = this.interpolate(living.field_70137_T, living.field_70163_u, partialTicks) + living.field_70131_O + 0.5;
        double z = this.interpolate(living.field_70136_U, living.field_70161_v, partialTicks);
        y += this.getStackOffset(x, y, z, nowMillis);
        long durationMs = Math.max(1L, Math.round(this.duration.getInput()));
        String text = (delta > 0.0F ? "+" : "-") + this.fastOneDecimal(Math.abs(delta));
        int color = delta > 0.0F ? -11141291 : -43691;
        RavenFontRenderer fr = this.getDamageTagFontRenderer();
        int halfW = fr != null ? fr.getStringWidth(text) >> 1 : 0;
        this.activeTags.add(new DamageTags.DamageTag(text, color, x, y, z, nowMillis, durationMs, halfW));
        if (this.activeTags.size() > 256) {
            this.activeTags.remove(0);
        }
    }

    private void renderTag(DamageTags.DamageTag tag, long now, RenderManager renderManager, RavenFontRenderer fontRenderer, double viewerX, double viewerY, double viewerZ, int depthOrdinal, float scaleMul, boolean bgEnabled, float bgOpacitySlider) {
        float progress = this.getProgress(tag, now);
        if (!(progress >= 1.0F)) {
            float rise = this.expoOut(progress);
            float alpha = this.getAlpha(progress);
            if (!(alpha <= 0.0F)) {
                double x = tag.x - viewerX;
                double y = tag.y + rise * 1.0 - viewerY;
                double z = tag.z - viewerZ;
                float renderScale = 0.02666667F * scaleMul;
                int halfWidth = tag.textHalfWidth;
                GlStateManager.func_179094_E();
                GlStateManager.func_179109_b((float) x, (float) y, (float) z);
                GL11.glNormal3f(0.0F, 1.0F, 0.0F);
                GlStateManager.func_179114_b(-renderManager.field_78735_i, 0.0F, 1.0F, 0.0F);
                GlStateManager.func_179114_b(renderManager.field_78732_j * (mc.field_71474_y.field_74320_O == 2 ? -1 : 1), 1.0F, 0.0F, 0.0F);
                GlStateManager.func_179152_a(-renderScale, -renderScale, renderScale);
                GlStateManager.func_179140_f();
                GlStateManager.func_179147_l();
                GlStateManager.func_179120_a(770, 771, 1, 0);
                switch(depthOrdinal) {
                    case 1:
                        this.renderVisibleTag(fontRenderer, tag, halfWidth, alpha, bgEnabled, bgOpacitySlider);
                        break;
                    case 2:
                        this.renderThroughWallsTag(fontRenderer, tag, halfWidth, alpha, bgEnabled, bgOpacitySlider);
                        break;
                    default:
                        this.renderVanillaDepthTag(fontRenderer, tag, halfWidth, alpha, bgEnabled, bgOpacitySlider);
                }
                GlStateManager.func_179126_j();
                GlStateManager.func_179132_a(true);
                GlStateManager.func_179098_w();
                GlStateManager.func_179084_k();
                GlStateManager.func_179145_e();
                GlStateManager.func_179131_c(1.0F, 1.0F, 1.0F, 1.0F);
                GlStateManager.func_179121_F();
            }
        }
    }

    private void renderVanillaDepthTag(RavenFontRenderer fontRenderer, DamageTags.DamageTag tag, int halfWidth, float alpha, boolean bgEnabled, float bgOpacitySlider) {
        GlStateManager.func_179132_a(false);
        GlStateManager.func_179097_i();
        this.drawBackground(halfWidth, alpha, bgEnabled, bgOpacitySlider);
        fontRenderer.drawString(tag.text, -halfWidth, 0.0F, this.applyFontAlpha(tag.color, 0.1254902F * alpha), false);
        GlStateManager.func_179126_j();
        GlStateManager.func_179132_a(true);
        fontRenderer.drawString(tag.text, -halfWidth, 0.0F, this.applyFontAlpha(tag.color, alpha), this.textShadow.isToggled());
    }

    private void renderVisibleTag(RavenFontRenderer fontRenderer, DamageTags.DamageTag tag, int halfWidth, float alpha, boolean bgEnabled, float bgOpacitySlider) {
        GlStateManager.func_179132_a(false);
        GlStateManager.func_179126_j();
        this.drawBackground(halfWidth, alpha, bgEnabled, bgOpacitySlider);
        fontRenderer.drawString(tag.text, -halfWidth, 0.0F, this.applyFontAlpha(tag.color, alpha), this.textShadow.isToggled());
    }

    private void renderThroughWallsTag(RavenFontRenderer fontRenderer, DamageTags.DamageTag tag, int halfWidth, float alpha, boolean bgEnabled, float bgOpacitySlider) {
        GlStateManager.func_179132_a(false);
        GlStateManager.func_179097_i();
        this.drawBackground(halfWidth, alpha, bgEnabled, bgOpacitySlider);
        fontRenderer.drawString(tag.text, -halfWidth, 0.0F, this.applyFontAlpha(tag.color, alpha), this.textShadow.isToggled());
    }

    private void drawBackground(int halfWidth, float alpha, boolean bgEnabled, float bgOpacitySlider) {
        if (bgEnabled) {
            float bgAlpha = bgOpacitySlider * alpha;
            if (!(bgAlpha <= 0.0F)) {
                GlStateManager.func_179090_x();
                Tessellator tessellator = Tessellator.func_178181_a();
                WorldRenderer worldRenderer = tessellator.func_178180_c();
                worldRenderer.func_181668_a(7, DefaultVertexFormats.field_181706_f);
                worldRenderer.func_181662_b(-halfWidth - 1, -1.0, 0.0).func_181666_a(0.0F, 0.0F, 0.0F, bgAlpha).func_181675_d();
                worldRenderer.func_181662_b(-halfWidth - 1, 8.0, 0.0).func_181666_a(0.0F, 0.0F, 0.0F, bgAlpha).func_181675_d();
                worldRenderer.func_181662_b(halfWidth + 1, 8.0, 0.0).func_181666_a(0.0F, 0.0F, 0.0F, bgAlpha).func_181675_d();
                worldRenderer.func_181662_b(halfWidth + 1, -1.0, 0.0).func_181666_a(0.0F, 0.0F, 0.0F, bgAlpha).func_181675_d();
                tessellator.func_78381_a();
                GlStateManager.func_179098_w();
            }
        }
    }

    private float getProgress(DamageTags.DamageTag tag, long now) {
        return MathHelper.func_76131_a((float) (now - tag.createdAt) / (float) tag.durationMs, 0.0F, 1.0F);
    }

    private float getAlpha(float progress) {
        if (progress <= 0.72F) {
            return 1.0F;
        } else {
            float fadeProgress = MathHelper.func_76131_a((progress - 0.72F) / 0.27999997F, 0.0F, 1.0F);
            return 1.0F - fadeProgress;
        }
    }

    private double getStackOffset(double x, double y, double z, long nowMillis) {
        int overlaps = 0;
        for (DamageTags.DamageTag tag : this.activeTags) {
            if (nowMillis - tag.createdAt < tag.durationMs) {
                double dx = tag.x - x;
                double dz = tag.z - z;
                if (dx * dx + dz * dz <= 0.36 && Math.abs(tag.y - y) <= 1.5) {
                    overlaps++;
                }
            }
        }
        return overlaps * 0.15;
    }

    private float getHealth(EntityLivingBase living) {
        return Math.max(0.0F, living.func_110143_aJ());
    }

    private float getAbsorption(EntityLivingBase living) {
        return living instanceof EntityPlayer ? Math.max(0.0F, ((EntityPlayer) living).func_110139_bj()) : 0.0F;
    }

    private double interpolate(double start, double end, float partialTicks) {
        return start + (end - start) * partialTicks;
    }

    private int applyFontAlpha(int color, float alpha) {
        int alphaChannel = MathHelper.func_76125_a(Math.round(alpha * 255.0F), 0, 255);
        if (alpha > 0.0F && alphaChannel < 4) {
            alphaChannel = 4;
        }
        return alphaChannel << 24 | color & 16777215;
    }

    private String fastOneDecimal(float value) {
        int tenths = Math.round(value * 10.0F);
        int intPart = tenths / 10;
        int fracPart = Math.abs(tenths % 10);
        return fracPart == 0 ? String.valueOf(intPart) : intPart + "." + fracPart;
    }

    private float expoOut(float t) {
        if (t <= 0.0F) {
            return 0.0F;
        } else {
            return t >= 1.0F ? 1.0F : 1.0F - (float) Math.pow(2.0, -10.0 * t);
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

    private RavenFontRenderer getDamageTagFontRenderer() {
        return FontManager.getNametagRenderer(this.getSelectedFontName());
    }

    private void resetState() {
        this.activeTags.clear();
        this.trackedStates.clear();
        this.pendingUpdates.clear();
    }

    private static class DamageTag {

        private final String text;

        private final int color;

        private final double x;

        private final double y;

        private final double z;

        private final long createdAt;

        private final long durationMs;

        private final AxisAlignedBB frustumAabb;

        private final int textHalfWidth;

        private DamageTag(String text, int color, double x, double y, double z, long createdAt, long durationMs, int textHalfWidth) {
            this.text = text;
            this.color = color;
            this.x = x;
            this.y = y;
            this.z = z;
            this.createdAt = createdAt;
            this.durationMs = durationMs;
            this.textHalfWidth = textHalfWidth;
            double e = 0.35;
            this.frustumAabb = new AxisAlignedBB(x - e, y - e, z - e, x + e, y + 1.0 + e, z + e);
        }
    }

    private static class HealthSnapshot {

        private final float health;

        private final float absorption;

        private HealthSnapshot(float health, float absorption) {
            this.health = health;
            this.absorption = absorption;
        }

        private float total() {
            return this.health + this.absorption;
        }
    }

    private static class QueuedMetadataUpdate {

        private final int entityId;

        private final boolean hasHealth;

        private final float health;

        private final boolean hasAbsorption;

        private final float absorption;

        private QueuedMetadataUpdate(int entityId, boolean hasHealth, float health, boolean hasAbsorption, float absorption) {
            this.entityId = entityId;
            this.hasHealth = hasHealth;
            this.health = health;
            this.hasAbsorption = hasAbsorption;
            this.absorption = absorption;
        }
    }
}
