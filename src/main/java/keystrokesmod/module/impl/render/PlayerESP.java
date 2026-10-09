package keystrokesmod.module.impl.render;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import keystrokesmod.Raven;
import keystrokesmod.mixin.impl.accessor.IAccessorEntityRenderer;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.client.Settings;
import keystrokesmod.module.impl.player.Freecam;
import keystrokesmod.module.impl.world.AntiBot;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.ColorSetting;
import keystrokesmod.module.setting.impl.GroupSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.RenderUtils;
import keystrokesmod.utility.Utils;
import keystrokesmod.utility.shader.GlowShader;
import keystrokesmod.utility.shader.KawaseBloom;
import keystrokesmod.utility.shader.OutlineShader;
import keystrokesmod.utility.shader.SeparableOutlineShader;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.client.event.RenderPlayerEvent.Post;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import org.lwjgl.opengl.GL11;

public class PlayerESP extends Module {

    public ColorSetting color;

    public ButtonSetting teamColor;

    public ButtonSetting rainbow;

    public GroupSetting espTypes;

    public ButtonSetting twoD;

    public ButtonSetting box;

    public ButtonSetting healthBar;

    public ButtonSetting outline;

    public ButtonSetting heavyGlow;

    public ButtonSetting shaded;

    public ButtonSetting skeleton;

    public ButtonSetting ring;

    public ButtonSetting redOnDamage;

    public ButtonSetting renderSelf;

    public ButtonSetting showInvis;

    private final SliderSetting maxDistance;

    private static final float RAD_TO_DEG = (float) (180.0 / Math.PI);

    public static boolean renderingOutlinePass = false;

    private final List<PlayerESP.EspRenderState> renderStates = new ArrayList<>();

    private final List<PlayerESP.EspRenderState> visibleRenderStates = new ArrayList<>();

    private final Map<EntityPlayer, PlayerESP.EspRenderState> playerRenderStates = new HashMap<>();

    private final double[] projectedPoint = new double[3];

    private RenderUtils.ProjectionContext projectionContext;

    private int renderStateCount;

    private int visibleRenderStateCount;

    private Framebuffer outlineFramebuffer;

    private final OutlineShader outlineShader = new OutlineShader();

    private final SeparableOutlineShader separableOutlineShader = new SeparableOutlineShader();

    private final GlowShader glowShader = new GlowShader();

    public PlayerESP() {
        super("PlayerESP", Module.category.render, 0);
        this.liteModule = true;
        this.registerSetting(this.espTypes = new GroupSetting("Types"));
        this.registerSetting(this.twoD = new ButtonSetting(this.espTypes, "2D", false));
        this.registerSetting(this.box = new ButtonSetting(this.espTypes, "Box", false));
        this.registerSetting(this.outline = new ButtonSetting(this.espTypes, "Outline (Light Glow)", false));
        this.registerSetting(this.heavyGlow = new ButtonSetting(this.espTypes, "Outline (Heavy Glow)", false));
        this.registerSetting(this.ring = new ButtonSetting(this.espTypes, "Ring", false));
        this.registerSetting(this.shaded = new ButtonSetting(this.espTypes, "Shaded", false));
        this.registerSetting(this.skeleton = new ButtonSetting(this.espTypes, "Skeleton", false));
        this.registerSetting(this.color = new ColorSetting("Color", 0, 255, 0));
        this.registerSetting(this.rainbow = new ButtonSetting("Rainbow", false));
        this.registerSetting(this.healthBar = new ButtonSetting("Health bar", true));
        this.registerSetting(this.redOnDamage = new ButtonSetting("Red on damage", true));
        this.registerSetting(this.renderSelf = new ButtonSetting("Render self", false));
        this.registerSetting(this.teamColor = new ButtonSetting("Team color", false));
        this.registerSetting(this.showInvis = new ButtonSetting("Show invis", true));
        this.registerSetting(this.maxDistance = new SliderSetting("Max distance", 128.0, 32.0, 256.0, 8.0));
    }

    @Override
    public void onDisable() {
        this.clearRenderStates();
    }

    @SubscribeEvent
    public void onClientTick(ClientTickEvent event) {
        if (event.phase == Phase.END) {
            if (Utils.nullCheck() && mc.field_71441_e != null) {
                this.updateRenderStates();
            } else {
                this.clearRenderStates();
            }
        }
    }

    @SubscribeEvent
    public void onRenderPlayerEvent(Post e) {
        if (this.skeleton.isToggled() && e.entityPlayer != null && Utils.nullCheck()) {
            PlayerESP.EspRenderState renderState = this.playerRenderStates.get(e.entityPlayer);
            if (renderState != null) {
                this.renderSkeleton(e.entityPlayer, e.renderer.func_177087_b(), this.resolveBaseRenderColor(renderState), e.partialRenderTick);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRenderWorld(RenderWorldLastEvent e) {
        this.visibleRenderStateCount = 0;
        if (Utils.nullCheck()) {
            boolean renderWorldTypes = this.box.isToggled() || this.shaded.isToggled() || this.healthBar.isToggled() || this.ring.isToggled();
            boolean captureVisibleStates = this.outline.isToggled() || this.heavyGlow.isToggled() || this.twoD.isToggled();
            if (this.renderStateCount != 0 && (renderWorldTypes || captureVisibleStates)) {
                for (int i = 0; i < this.renderStateCount; i++) {
                    PlayerESP.EspRenderState renderState = this.renderStates.get(i);
                    EntityLivingBase entity = renderState.entity;
                    if (entity != null && RenderUtils.isInViewFrustum(entity)) {
                        renderState.renderColor = this.resolveBaseRenderColor(renderState);
                        if (renderWorldTypes) {
                            this.render(entity, renderState.renderColor);
                        }
                        if (captureVisibleStates) {
                            this.addVisibleRenderState(renderState);
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onRenderOutline(RenderWorldLastEvent e) {
        if (Utils.nullCheck() && this.visibleRenderStateCount != 0) {
            if (this.outline.isToggled() || this.heavyGlow.isToggled()) {
                this.runOutlinePass(e.partialTicks);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onRenderTwo2D(RenderWorldLastEvent e) {
        if (Utils.nullCheck() && this.visibleRenderStateCount != 0) {
            if (this.twoD.isToggled()) {
                this.renderTwoDPass(e.partialTicks);
            }
        }
    }

    private void runOutlinePass(float partialTicks) {
        boolean renderHeavyGlow = this.heavyGlow.isToggled();
        if (this.glowShader.isValid() && (renderHeavyGlow ? this.separableOutlineShader.isValid() : this.outlineShader.isValid()) && this.visibleRenderStateCount != 0) {
            this.outlineFramebuffer = this.createOutlineFramebuffer(this.outlineFramebuffer, renderHeavyGlow);
            if (this.outlineFramebuffer != null) {
                mc.func_147110_a().func_147610_a(true);
                GlStateManager.func_179094_E();
                GlStateManager.func_179123_a();
                this.outlineFramebuffer.func_147610_a(true);
                ((IAccessorEntityRenderer) mc.field_71460_t).callSetupCameraTransform(partialTicks, 0);
                boolean shadows = mc.field_71474_y.field_181151_V;
                mc.field_71474_y.field_181151_V = false;
                renderingOutlinePass = true;
                this.glowShader.use();
                for (int i = 0; i < this.visibleRenderStateCount; i++) {
                    PlayerESP.EspRenderState renderState = this.visibleRenderStates.get(i);
                    EntityLivingBase ent = renderState.entity;
                    int col = this.resolveOutlineColor(renderState);
                    this.glowShader.setColor(col >> 16 & 0xFF, col >> 8 & 0xFF, col & 0xFF, col >> 24 & 0xFF);
                    boolean invis = ent.func_82150_aj();
                    if (this.showInvis.isToggled()) {
                        ent.func_82142_c(false);
                    }
                    mc.func_175598_ae().func_147936_a(ent, partialTicks, true);
                    ent.func_82142_c(invis);
                }
                this.glowShader.stop();
                renderingOutlinePass = false;
                mc.field_71474_y.field_181151_V = shadows;
                mc.field_71460_t.func_175072_h();
                mc.field_71460_t.func_78478_c();
                mc.func_147110_a().func_147610_a(true);
                if (renderHeavyGlow) {
                    KawaseBloom.renderBlur(this.outlineFramebuffer.field_147617_g, 4, 4.0F);
                    mc.func_147110_a().func_147610_a(false);
                }
                if (renderHeavyGlow) {
                    this.separableOutlineShader.render(this.outlineFramebuffer);
                } else {
                    this.outlineShader.use();
                    RenderUtils.drawFramebufferFullscreen(this.outlineFramebuffer);
                    this.outlineShader.stop();
                }
                this.outlineFramebuffer.func_147614_f();
                mc.func_147110_a().func_147610_a(true);
                GlStateManager.func_179099_b();
                GlStateManager.func_179121_F();
                ((IAccessorEntityRenderer) mc.field_71460_t).callSetupCameraTransform(partialTicks, 0);
            }
        }
    }

    private Framebuffer createOutlineFramebuffer(Framebuffer framebuffer, boolean reducedResolution) {
        int width = reducedResolution ? Math.max(1, mc.field_71443_c * 3 / 4) : mc.field_71443_c;
        int height = reducedResolution ? Math.max(1, mc.field_71440_d * 3 / 4) : mc.field_71440_d;
        if (framebuffer == null || framebuffer.field_147621_c != width || framebuffer.field_147618_d != height) {
            if (framebuffer != null) {
                framebuffer.func_147608_a();
            }
            framebuffer = new Framebuffer(width, height, false);
        }
        framebuffer.func_147604_a(0.0F, 0.0F, 0.0F, 0.0F);
        return framebuffer;
    }

    private void clearRenderStates() {
        this.renderStateCount = 0;
        this.visibleRenderStateCount = 0;
        this.playerRenderStates.clear();
    }

    private void updateRenderStates() {
        this.renderStateCount = 0;
        this.visibleRenderStateCount = 0;
        this.playerRenderStates.clear();
        double maxDistSq = this.maxDistance.getInput() * this.maxDistance.getInput();
        if (Raven.DEBUG && ModuleManager.debug.debugEntities.isToggled()) {
            for (Entity entity : mc.field_71441_e.field_72996_f) {
                if (entity instanceof EntityLivingBase && entity != mc.field_71439_g && RenderUtils.isWithinDistanceSqToRenderView(entity, maxDistSq)) {
                    this.addRenderState((EntityLivingBase) entity, this.resolveStaticColor(entity));
                }
            }
        } else {
            EntityPlayer selfPlayer = (EntityPlayer) (Freecam.freeEntity == null ? mc.field_71439_g : Freecam.freeEntity);
            boolean allowSelf = this.shouldRenderSelf(selfPlayer);
            for (EntityPlayer player : mc.field_71441_e.field_73010_i) {
                if ((player != selfPlayer || allowSelf) && player.field_70725_aQ == 0 && (this.showInvis.isToggled() || !player.func_82150_aj()) && (selfPlayer == player || !AntiBot.isBot(player)) && RenderUtils.isWithinDistanceSqToRenderView(player, maxDistSq)) {
                    this.addRenderState(player, this.resolveStaticColor(player));
                }
            }
        }
    }

    private boolean shouldRenderSelf(EntityPlayer selfPlayer) {
        return selfPlayer == mc.field_71439_g && this.renderSelf.isToggled() && (!Settings.hideFirstPersonESP.isToggled() || mc.field_71474_y.field_74320_O > 0);
    }

    private void addRenderState(EntityLivingBase entity, int staticColor) {
        if (this.renderStateCount >= this.renderStates.size()) {
            this.renderStates.add(new PlayerESP.EspRenderState());
        }
        PlayerESP.EspRenderState renderState = this.renderStates.get(this.renderStateCount++);
        renderState.set(entity, staticColor);
        if (entity instanceof EntityPlayer) {
            this.playerRenderStates.put((EntityPlayer) entity, renderState);
        }
    }

    private void addVisibleRenderState(PlayerESP.EspRenderState renderState) {
        if (this.visibleRenderStateCount >= this.visibleRenderStates.size()) {
            this.visibleRenderStates.add(renderState);
        } else {
            this.visibleRenderStates.set(this.visibleRenderStateCount, renderState);
        }
        this.visibleRenderStateCount++;
    }

    private int resolveStaticColor(Entity entity) {
        int rgb = this.teamColor.isToggled() ? Utils.getColorFromEntity(entity) : this.color.getColor();
        return Utils.mergeAlpha(rgb, 255);
    }

    private int resolveBaseRenderColor(PlayerESP.EspRenderState renderState) {
        return !this.teamColor.isToggled() && this.rainbow.isToggled() ? Utils.mergeAlpha(Utils.getChroma(2L, 0L), 255) : renderState.staticColor;
    }

    private int resolveOutlineColor(PlayerESP.EspRenderState renderState) {
        return this.redOnDamage.isToggled() && renderState.entity.field_70737_aN != 0 ? -65536 : renderState.renderColor;
    }

    public void render(Entity en, int rgb) {
        if (this.box.isToggled() || this.shaded.isToggled() || this.healthBar.isToggled() || this.ring.isToggled()) {
            if (this.box.isToggled()) {
                RenderUtils.renderEntity(en, 1, 0.0, 0.0, rgb, this.redOnDamage.isToggled());
            }
            if (this.shaded.isToggled() && (ModuleManager.murderMystery == null || !ModuleManager.murderMystery.isEnabled() || ModuleManager.murderMystery.isEmpty())) {
                RenderUtils.renderEntity(en, 2, 0.0, 0.0, rgb, this.redOnDamage.isToggled());
            }
            if (this.healthBar.isToggled()) {
                RenderUtils.renderEntity(en, 4, 0.0, 0.0, rgb, this.redOnDamage.isToggled());
            }
            if (this.ring.isToggled()) {
                RenderUtils.renderEntity(en, 6, 0.0, 0.0, rgb, this.redOnDamage.isToggled());
            }
        }
    }

    private void renderTwoDPass(float partialTicks) {
        RenderManager renderManager = mc.func_175598_ae();
        if (renderManager != null) {
            ScaledResolution scaledResolution = new ScaledResolution(mc);
            ((IAccessorEntityRenderer) mc.field_71460_t).callSetupCameraTransform(partialTicks, 0);
            this.projectionContext = RenderUtils.captureProjectionContext(this.projectionContext, scaledResolution.func_78325_e());
            mc.field_71460_t.func_78478_c();
            int screenWidth = scaledResolution.func_78326_a();
            int screenHeight = scaledResolution.func_78328_b();
            for (int i = 0; i < this.visibleRenderStateCount; i++) {
                this.renderTwoD(this.visibleRenderStates.get(i), renderManager, screenWidth, screenHeight, partialTicks);
            }
        }
    }

    private void renderTwoD(PlayerESP.EspRenderState renderState, RenderManager renderManager, int screenWidth, int screenHeight, float partialTicks) {
        EntityLivingBase en = renderState.entity;
        double playerX = en.field_70142_S + (en.field_70165_t - en.field_70142_S) * partialTicks - renderManager.field_78730_l;
        double playerY = en.field_70137_T + (en.field_70163_u - en.field_70137_T) * partialTicks - renderManager.field_78731_m;
        double playerZ = en.field_70136_U + (en.field_70161_v - en.field_70136_U) * partialTicks - renderManager.field_78728_n;
        AxisAlignedBB axisAlignedBB = en.func_174813_aQ().func_72314_b(0.1, 0.1, 0.1);
        double minRelX = axisAlignedBB.field_72340_a - en.field_70165_t + playerX;
        double minRelY = axisAlignedBB.field_72338_b - en.field_70163_u + playerY;
        double minRelZ = axisAlignedBB.field_72339_c - en.field_70161_v + playerZ;
        double maxRelX = axisAlignedBB.field_72336_d - en.field_70165_t + playerX;
        double maxRelY = axisAlignedBB.field_72337_e - en.field_70163_u + playerY;
        double maxRelZ = axisAlignedBB.field_72334_f - en.field_70161_v + playerZ;
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        boolean isInView = false;
        for (int corner = 0; corner < 8; corner++) {
            double x = (corner & 1) == 0 ? minRelX : maxRelX;
            double y = (corner & 2) == 0 ? minRelY : maxRelY;
            double z = (corner & 4) == 0 ? minRelZ : maxRelZ;
            if (RenderUtils.projectTo2D(this.projectionContext, x, y, z, this.projectedPoint)) {
                double depth = this.projectedPoint[2];
                if (!(depth >= 1.0003684) && !(depth <= 0.0)) {
                    isInView = true;
                    double screenX = this.projectedPoint[0];
                    double screenY = this.projectedPoint[1];
                    if (screenX < minX) {
                        minX = screenX;
                    }
                    if (screenY < minY) {
                        minY = screenY;
                    }
                    if (screenX > maxX) {
                        maxX = screenX;
                    }
                    if (screenY > maxY) {
                        maxY = screenY;
                    }
                }
            }
        }
        if (isInView) {
            minX = Math.max(0.0, minX);
            minY = Math.max(0.0, minY);
            maxX = Math.min((double) screenWidth, maxX);
            maxY = Math.min((double) screenHeight, maxY);
            if (!(maxX <= minX) && !(maxY <= minY)) {
                int rgb = renderState.renderColor;
                float red = (rgb >> 16 & 0xFF) / 255.0F;
                float green = (rgb >> 8 & 0xFF) / 255.0F;
                float blue = (rgb & 0xFF) / 255.0F;
                GL11.glPushMatrix();
                GL11.glDisable(3553);
                GL11.glDisable(2929);
                GL11.glEnable(2848);
                GL11.glLineWidth(1.0F);
                GL11.glColor4f(0.0F, 0.0F, 0.0F, 0.4F);
                GL11.glBegin(2);
                GL11.glVertex2d(minX, minY);
                GL11.glVertex2d(maxX, minY);
                GL11.glVertex2d(maxX, maxY);
                GL11.glVertex2d(minX, maxY);
                GL11.glEnd();
                GL11.glColor4f(0.0F, 0.0F, 0.0F, 0.4F);
                GL11.glBegin(2);
                GL11.glVertex2d(minX + 1.0, minY + 1.0);
                GL11.glVertex2d(maxX - 1.0, minY + 1.0);
                GL11.glVertex2d(maxX - 1.0, maxY - 1.0);
                GL11.glVertex2d(minX + 1.0, maxY - 1.0);
                GL11.glEnd();
                GL11.glColor4f(red, green, blue, 1.0F);
                GL11.glBegin(2);
                GL11.glVertex2d(minX + 0.5, minY + 0.5);
                GL11.glVertex2d(maxX - 0.5, minY + 0.5);
                GL11.glVertex2d(maxX - 0.5, maxY - 0.5);
                GL11.glVertex2d(minX + 0.5, maxY - 0.5);
                GL11.glEnd();
                GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
                GL11.glEnable(3553);
                GL11.glEnable(2929);
                GL11.glDisable(2848);
                GL11.glPopMatrix();
            }
        }
    }

    public void renderSkeleton(EntityPlayer player, ModelBiped modelBiped, int color, float partialTicks) {
        GL11.glPushMatrix();
        GL11.glDisable(2929);
        double viewerPosX = mc.func_175598_ae().field_78730_l;
        double viewerPosY = mc.func_175598_ae().field_78731_m;
        double viewerPosZ = mc.func_175598_ae().field_78728_n;
        double posX = player.field_70142_S + (player.field_70165_t - player.field_70142_S) * partialTicks - viewerPosX;
        double posY = player.field_70137_T + (player.field_70163_u - player.field_70137_T) * partialTicks - viewerPosY;
        double posZ = player.field_70136_U + (player.field_70161_v - player.field_70136_U) * partialTicks - viewerPosZ;
        boolean wasBlendEnabled = GL11.glIsEnabled(3042);
        GL11.glPushMatrix();
        GL11.glBlendFunc(770, 771);
        if (!wasBlendEnabled) {
            GL11.glEnable(3042);
        }
        GL11.glBlendFunc(770, 771);
        GL11.glColor4f((color >> 16 & 0xFF) / 255.0F, (color >> 8 & 0xFF) / 255.0F, (color & 0xFF) / 255.0F, 1.0F);
        GL11.glDisable(2896);
        GL11.glEnable(2848);
        GL11.glDisable(3553);
        GL11.glTranslated(posX, posY, posZ);
        float distance = mc.field_71439_g.func_70032_d(player);
        float computedLineWidth = 4.0F * ((100.0F - Math.min(distance, 100.0F)) / 100.0F);
        float lineWidth = Math.max(1.0F, computedLineWidth);
        GL11.glLineWidth(lineWidth);
        boolean isSneaking = player.func_70093_af();
        float legHeight = isSneaking ? 0.6F : 0.75F;
        double legOffsetZ = isSneaking ? -0.2 : 0.0;
        GL11.glRotatef(player.field_70761_aq, 0.0F, -999.0F, 0.0F);
        GL11.glTranslated(-0.15, legHeight, legOffsetZ);
        float rightLegRotX = modelBiped.field_178721_j.field_78795_f * (float) (180.0 / Math.PI);
        float rightLegRotY = modelBiped.field_178721_j.field_78796_g * (float) (180.0 / Math.PI);
        float rightLegRotZ = modelBiped.field_178721_j.field_78808_h * (float) (180.0 / Math.PI);
        GL11.glRotatef(rightLegRotX, 1.0F, 0.0F, 0.0F);
        GL11.glRotatef(-rightLegRotY, 0.0F, 1.0F, 0.0F);
        GL11.glRotatef(-rightLegRotZ, 0.0F, 0.0F, 1.0F);
        this.drawLine(0.0, 0.0, 0.0, 0.0, -legHeight, 0.0);
        GL11.glRotatef(rightLegRotZ, 0.0F, 0.0F, 1.0F);
        GL11.glRotatef(rightLegRotY, 0.0F, 1.0F, 0.0F);
        GL11.glRotatef(-rightLegRotX, 1.0F, 0.0F, 0.0F);
        GL11.glTranslated(0.3, 0.0, 0.0);
        float leftLegRotX = modelBiped.field_178722_k.field_78795_f * (float) (180.0 / Math.PI);
        float leftLegRotY = modelBiped.field_178722_k.field_78796_g * (float) (180.0 / Math.PI);
        float leftLegRotZ = modelBiped.field_178722_k.field_78808_h * (float) (180.0 / Math.PI);
        GL11.glRotatef(leftLegRotX, 1.0F, 0.0F, 0.0F);
        GL11.glRotatef(-leftLegRotY, 0.0F, 1.0F, 0.0F);
        GL11.glRotatef(-leftLegRotZ, 0.0F, 0.0F, 1.0F);
        this.drawLine(0.0, 0.0, 0.0, 0.0, -legHeight, 0.0);
        GL11.glRotatef(leftLegRotZ, 0.0F, 0.0F, 1.0F);
        GL11.glRotatef(leftLegRotY, 0.0F, 1.0F, 0.0F);
        GL11.glRotatef(-leftLegRotX, 1.0F, 0.0F, 0.0F);
        GL11.glTranslated(-0.15, 0.0, 0.0);
        this.drawLine(0.15, 0.0, 0.0, -0.15, 0.0, 0.0);
        if (player.func_70093_af()) {
            GL11.glRotatef(20.0F, 1.0F, 0.0F, 0.0F);
        }
        this.drawLine(0.0, 0.0, 0.0, 0.0, 0.65, 0.0);
        GL11.glTranslated(0.0, 0.65, 0.0);
        this.drawLine(0.35, 0.0, 0.0, -0.35, 0.0, 0.0);
        GL11.glTranslated(-0.35, 0.0, 0.0);
        float rightArmRotX = modelBiped.field_178723_h.field_78795_f * (float) (180.0 / Math.PI);
        float rightArmRotY = modelBiped.field_178723_h.field_78796_g * (float) (180.0 / Math.PI);
        float rightArmRotZ = modelBiped.field_178723_h.field_78808_h * (float) (180.0 / Math.PI);
        GL11.glRotatef(rightArmRotX, 1.0F, 0.0F, 0.0F);
        GL11.glRotatef(-rightArmRotY, 0.0F, 1.0F, 0.0F);
        GL11.glRotatef(-rightArmRotZ, 0.0F, 0.0F, 1.0F);
        this.drawLine(0.0, 0.0, 0.0, 0.0, -0.6, 0.0);
        GL11.glRotatef(rightArmRotZ, 0.0F, 0.0F, 1.0F);
        GL11.glRotatef(rightArmRotY, 0.0F, 1.0F, 0.0F);
        GL11.glRotatef(-rightArmRotX, 1.0F, 0.0F, 0.0F);
        GL11.glTranslated(0.7, 0.0, 0.0);
        float leftArmRotX = modelBiped.field_178724_i.field_78795_f * (float) (180.0 / Math.PI);
        float leftArmRotY = modelBiped.field_178724_i.field_78796_g * (float) (180.0 / Math.PI);
        float leftArmRotZ = modelBiped.field_178724_i.field_78808_h * (float) (180.0 / Math.PI);
        GL11.glRotatef(leftArmRotX, 1.0F, 0.0F, 0.0F);
        GL11.glRotatef(-leftArmRotY, 0.0F, 1.0F, 0.0F);
        GL11.glRotatef(-leftArmRotZ, 0.0F, 0.0F, 1.0F);
        this.drawLine(0.0, 0.0, 0.0, 0.0, -0.6, 0.0);
        GL11.glRotatef(leftArmRotZ, 0.0F, 0.0F, 1.0F);
        GL11.glRotatef(leftArmRotY, 0.0F, 1.0F, 0.0F);
        GL11.glRotatef(-leftArmRotX, 1.0F, 0.0F, 0.0F);
        GL11.glTranslated(-0.35, 0.0, 0.0);
        GL11.glRotatef(-player.field_70761_aq, 0.0F, -999.0F, 0.0F);
        double headHeight = 0.4;
        GL11.glRotated(player.field_70177_z, 0.0, -999.0, 0.0);
        GL11.glRotated(player.field_70125_A, 999.0, 0.0, 0.0);
        this.drawLine(0.0, 0.0, 0.0, 0.0, headHeight, 0.0);
        this.drawLine(0.0, headHeight, 0.0, 0.0, headHeight, 0.25);
        GL11.glRotated(player.field_70125_A, 999.0, 0.0, 0.0);
        GL11.glRotated(-player.field_70177_z, 0.0, 999.0, 0.0);
        if (!wasBlendEnabled) {
            GL11.glDisable(3042);
        }
        GL11.glEnable(3553);
        GL11.glDisable(2848);
        GL11.glEnable(2896);
        GL11.glPopMatrix();
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glEnable(2929);
        GL11.glPopMatrix();
    }

    private void drawLine(double x1, double y1, double z1, double x2, double y2, double z2) {
        GL11.glBegin(1);
        GL11.glVertex3d(x1, y1, z1);
        GL11.glVertex3d(x2, y2, z2);
        GL11.glEnd();
    }

    private static final class EspRenderState {

        private EntityLivingBase entity;

        private int staticColor;

        private int renderColor;

        private EspRenderState() {
        }

        private void set(EntityLivingBase entity, int staticColor) {
            this.entity = entity;
            this.staticColor = staticColor;
            this.renderColor = staticColor;
        }
    }
}
