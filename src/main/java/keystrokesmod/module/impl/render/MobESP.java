package keystrokesmod.module.impl.render;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Map;
import java.util.function.Predicate;
import keystrokesmod.mixin.impl.accessor.IAccessorEntityRenderer;
import keystrokesmod.mixin.impl.accessor.IAccessorMinecraft;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.ColorSetting;
import keystrokesmod.module.setting.impl.GroupSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.RenderUtils;
import keystrokesmod.utility.Utils;
import keystrokesmod.utility.shader.GlowShader;
import keystrokesmod.utility.shader.OutlineShader;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.boss.EntityWither;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.monster.EntityBlaze;
import net.minecraft.entity.monster.EntityCaveSpider;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.entity.monster.EntityEndermite;
import net.minecraft.entity.monster.EntityGhast;
import net.minecraft.entity.monster.EntityGiantZombie;
import net.minecraft.entity.monster.EntityGuardian;
import net.minecraft.entity.monster.EntityIronGolem;
import net.minecraft.entity.monster.EntityMagmaCube;
import net.minecraft.entity.monster.EntityPigZombie;
import net.minecraft.entity.monster.EntitySilverfish;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.entity.monster.EntitySlime;
import net.minecraft.entity.monster.EntitySnowman;
import net.minecraft.entity.monster.EntitySpider;
import net.minecraft.entity.monster.EntityWitch;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntityBat;
import net.minecraft.entity.passive.EntityChicken;
import net.minecraft.entity.passive.EntityCow;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.passive.EntityMooshroom;
import net.minecraft.entity.passive.EntityOcelot;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.passive.EntityRabbit;
import net.minecraft.entity.passive.EntitySheep;
import net.minecraft.entity.passive.EntitySquid;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.passive.EntityWolf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

public class MobESP extends Module {

    public static boolean renderingOutlinePass = false;

    private static final ThreadLocal<Boolean> MOB_CHAMS_ACTIVE = ThreadLocal.withInitial(() -> false);

    public GroupSetting espTypes;

    public ButtonSetting twoD;

    public ButtonSetting box;

    public ButtonSetting outline;

    public ButtonSetting chams;

    public ButtonSetting ring;

    public ButtonSetting shaded;

    public ButtonSetting healthBar;

    public ButtonSetting redOnDamage;

    public ButtonSetting showInvis;

    private final SliderSetting maxDistance;

    private final List<MobESP.MobEntry> mobEntries = new ArrayList<>();

    private final Map<EntityLivingBase, Integer> renderAsTwoD = new HashMap<>();

    private Framebuffer outlineFramebuffer;

    private final OutlineShader outlineShader = new OutlineShader();

    private final GlowShader glowShader = new GlowShader();

    public MobESP() {
        super("MobESP", Module.category.render);
        this.registerSetting(this.espTypes = new GroupSetting("Types"));
        this.registerSetting(this.twoD = new ButtonSetting(this.espTypes, "2D", false));
        this.registerSetting(this.box = new ButtonSetting(this.espTypes, "Box", false));
        this.registerSetting(this.outline = new ButtonSetting(this.espTypes, "Outline", false));
        this.registerSetting(this.chams = new ButtonSetting(this.espTypes, "Chams", false));
        this.registerSetting(this.ring = new ButtonSetting(this.espTypes, "Ring", false));
        this.registerSetting(this.shaded = new ButtonSetting(this.espTypes, "Shaded", false));
        this.registerSetting(this.healthBar = new ButtonSetting(this.espTypes, "Health bar", false));
        this.registerSetting(this.redOnDamage = new ButtonSetting("Red on damage", true));
        this.registerSetting(this.showInvis = new ButtonSetting("Show invis", true));
        this.registerSetting(this.maxDistance = new SliderSetting("Max distance", 128.0, 32.0, 256.0, 8.0));
        this.registerMobGroups();
    }

    private void registerMobGroups() {
        this.registerMob("Armor Stand", EntityArmorStand.class, false, 200, 200, 200);
        this.registerMob("Baby Zombie", EntityZombie.class, e -> !((EntityZombie) e).func_82231_m() && ((EntityZombie) e).func_70631_g_(), true, 0, 120, 255);
        this.registerMob("Bat", EntityBat.class, false, 80, 80, 80);
        this.registerMob("Blaze", EntityBlaze.class, true, 255, 165, 0);
        this.registerMob("Cave Spider", EntityCaveSpider.class, true, 64, 64, 64);
        this.registerMob("Chicken", EntityChicken.class, false, 255, 255, 255);
        this.registerMob("Cow", EntityCow.class, false, 120, 80, 60);
        this.registerMob("Creeper", EntityCreeper.class, true, 0, 255, 0);
        this.registerMob("Donkey", EntityHorse.class, e -> ((EntityHorse) e).func_110265_bP() == 1, false, 120, 120, 120);
        this.registerMob("Elder Guardian", EntityGuardian.class, e -> ((EntityGuardian) e).func_175461_cl(), false, 0, 90, 140);
        this.registerMob("Ender Dragon", EntityDragon.class, false, 200, 0, 200);
        this.registerMob("Enderman", EntityEnderman.class, true, 0, 0, 0);
        this.registerMob("Endermite", EntityEndermite.class, false, 80, 0, 120);
        this.registerMob("Ghast", EntityGhast.class, true, 255, 255, 255);
        this.registerMob("Giant", EntityGiantZombie.class, false, 0, 0, 139);
        this.registerMob("Guardian", EntityGuardian.class, g -> !((EntityGuardian) g).func_175461_cl(), false, 0, 150, 180);
        this.registerMob("Horse", EntityHorse.class, e -> ((EntityHorse) e).func_110265_bP() == 0, false, 150, 100, 60);
        this.registerMob("Iron Golem", EntityIronGolem.class, false, 200, 200, 200);
        this.registerMob("Magma Cube", EntityMagmaCube.class, false, 200, 60, 0);
        this.registerMob("Mooshroom", EntityMooshroom.class, false, 200, 0, 0);
        this.registerMob("Mule", EntityHorse.class, e -> ((EntityHorse) e).func_110265_bP() == 2, false, 130, 110, 80);
        this.registerMob("Ocelot", EntityOcelot.class, false, 200, 150, 100);
        this.registerMob("Pig", EntityPig.class, false, 255, 150, 200);
        this.registerMob("Rabbit", EntityRabbit.class, false, 180, 140, 100);
        this.registerMob("Sheep", EntitySheep.class, false, 255, 255, 255);
        this.registerMob("Silverfish", EntitySilverfish.class, true, 128, 128, 128);
        this.registerMob("Skeleton", EntitySkeleton.class, e -> ((EntitySkeleton) e).func_82202_m() == 0, true, 255, 255, 255);
        this.registerMob("Skeleton Horse", EntityHorse.class, e -> ((EntityHorse) e).func_110265_bP() == 4, false, 220, 220, 220);
        this.registerMob("Slime", EntitySlime.class, true, 0, 255, 0);
        this.registerMob("Snow Golem", EntitySnowman.class, false, 255, 255, 255);
        this.registerMob("Spider", EntitySpider.class, true, 0, 0, 0);
        this.registerMob("Squid", EntitySquid.class, false, 30, 60, 180);
        this.registerMob("Villager", EntityVillager.class, false, 180, 150, 120);
        this.registerMob("Witch", EntityWitch.class, false, 90, 50, 120);
        this.registerMob("Wither", EntityWither.class, false, 64, 64, 64);
        this.registerMob("Wither Skeleton", EntitySkeleton.class, e -> ((EntitySkeleton) e).func_82202_m() == 1, true, 55, 55, 55);
        this.registerMob("Wolf", EntityWolf.class, false, 200, 200, 200);
        this.registerMob("Zombie", EntityZombie.class, e -> !((EntityZombie) e).func_82231_m() && !((EntityZombie) e).func_70631_g_(), true, 0, 0, 255);
        this.registerMob("Zombie Horse", EntityHorse.class, e -> ((EntityHorse) e).func_110265_bP() == 3, false, 80, 100, 70);
        this.registerMob("Zombie Pigman", EntityPigZombie.class, true, 255, 192, 203);
        this.registerMob("Zombie Villager", EntityZombie.class, e -> ((EntityZombie) e).func_82231_m(), true, 100, 140, 90);
    }

    private void registerMob(String name, Class<? extends EntityLivingBase> clazz, boolean defaultOn, int r, int g, int b) {
        this.registerMob(name, clazz, null, defaultOn, r, g, b);
    }

    private void registerMob(String name, Class<? extends EntityLivingBase> clazz, Predicate<EntityLivingBase> refine, boolean defaultOn, int r, int g, int b) {
        GroupSetting group = new GroupSetting(name);
        this.registerSetting(group);
        ButtonSetting enable = new ButtonSetting(group, "Enable", defaultOn);
        this.registerSetting(enable);
        ColorSetting color = new ColorSetting(group, "Color", r, g, b);
        this.registerSetting(color);
        this.mobEntries.add(new MobESP.MobEntry(clazz, refine, enable, color));
    }

    private MobESP.MobEntry resolveEntry(EntityLivingBase entity) {
        if (entity instanceof EntityPlayer) {
            return null;
        } else {
            MobESP.MobEntry best = null;
            for (MobESP.MobEntry entry : this.mobEntries) {
                if (entry.enable.isToggled() && entry.matches(entity) && (best == null || best.type.isAssignableFrom(entry.type))) {
                    best = entry;
                }
            }
            return best;
        }
    }

    private int rgbFor(EntityLivingBase ent, MobESP.MobEntry entry) {
        int rgb = Utils.mergeAlpha(entry.color.getRGB(), 255);
        if (this.redOnDamage.isToggled() && ent.field_70737_aN != 0) {
            rgb = -65536;
        }
        return rgb;
    }

    public static void onRenderMobPre(EntityLivingBase entity) {
        MobESP mod = getMobEspModule();
        if (mod != null && mod.shouldApplyChamsTo(entity)) {
            GL11.glEnable(32823);
            GL11.glPolygonOffset(1.0F, -1100000.0F);
            MOB_CHAMS_ACTIVE.set(true);
        }
    }

    public static void onRenderMobPost() {
        if (Boolean.TRUE.equals(MOB_CHAMS_ACTIVE.get())) {
            MOB_CHAMS_ACTIVE.set(false);
            GL11.glPolygonOffset(1.0F, 1100000.0F);
            GL11.glDisable(32823);
        }
    }

    private static MobESP getMobEspModule() {
        Module module = ModuleManager.getModule(MobESP.class);
        return module instanceof MobESP && module.isEnabled() ? (MobESP) module : null;
    }

    private boolean shouldApplyChamsTo(EntityLivingBase entity) {
        if (!this.chams.isToggled() || !Utils.nullCheck() || entity == null || entity == mc.field_71439_g) {
            return false;
        } else if (entity.field_70725_aQ != 0) {
            return false;
        } else if (!this.showInvis.isToggled() && entity.func_82150_aj()) {
            return false;
        } else {
            double maxDistSq = this.maxDistance.getInput() * this.maxDistance.getInput();
            return !RenderUtils.isWithinDistanceSqToRenderView(entity, maxDistSq) ? false : this.resolveEntry(entity) != null;
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onRenderWorldLast(RenderWorldLastEvent e) {
        this.renderAsTwoD.clear();
        if (Utils.nullCheck() && this.isEnabled()) {
            double maxDistSq = this.maxDistance.getInput() * this.maxDistance.getInput();
            for (Entity entity : mc.field_71441_e.field_72996_f) {
                if (entity instanceof EntityLivingBase && entity != mc.field_71439_g) {
                    EntityLivingBase living = (EntityLivingBase) entity;
                    if (living.field_70725_aQ == 0 && (this.showInvis.isToggled() || !living.func_82150_aj()) && RenderUtils.isWithinDistanceSqToRenderView(living, maxDistSq)) {
                        MobESP.MobEntry entry = this.resolveEntry(living);
                        if (entry != null) {
                            int rgb = this.rgbFor(living, entry);
                            this.renderAsTwoD.put(living, rgb);
                            this.render(living, rgb);
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onRenderWorldLast2D(RenderWorldLastEvent e) {
        if (Utils.nullCheck() && this.isEnabled()) {
            if (this.outline.isToggled()) {
                this.runOutlinePass(e.partialTicks);
            }
            if (this.twoD.isToggled()) {
                for (Entry<EntityLivingBase, Integer> entry : this.renderAsTwoD.entrySet()) {
                    int col = this.redOnDamage.isToggled() && entry.getKey().field_70737_aN != 0 ? -65536 : entry.getValue();
                    this.renderTwoD(entry.getKey(), col, 0.0, e.partialTicks);
                }
            }
        }
    }

    private void runOutlinePass(float partialTicks) {
        if (this.outlineShader.isValid() && this.glowShader.isValid() && !this.renderAsTwoD.isEmpty()) {
            boolean anyVisible = false;
            for (EntityLivingBase ent : this.renderAsTwoD.keySet()) {
                if (RenderUtils.isInViewFrustum(ent)) {
                    anyVisible = true;
                    break;
                }
            }
            if (anyVisible) {
                this.outlineFramebuffer = RenderUtils.createFrameBuffer(this.outlineFramebuffer, false);
                if (this.outlineFramebuffer != null) {
                    GlStateManager.func_179094_E();
                    GlStateManager.func_179123_a();
                    this.outlineFramebuffer.func_147610_a(false);
                    ((IAccessorEntityRenderer) mc.field_71460_t).callSetupCameraTransform(partialTicks, 0);
                    boolean shadows = mc.field_71474_y.field_181151_V;
                    mc.field_71474_y.field_181151_V = false;
                    renderingOutlinePass = true;
                    this.glowShader.use();
                    for (Entry<EntityLivingBase, Integer> e : this.renderAsTwoD.entrySet()) {
                        EntityLivingBase key = e.getKey();
                        if (RenderUtils.isInViewFrustum(key)) {
                            int col = this.redOnDamage.isToggled() && key.field_70737_aN != 0 ? -65536 : e.getValue();
                            this.glowShader.setColor(col >> 16 & 0xFF, col >> 8 & 0xFF, col & 0xFF, col >> 24 & 0xFF);
                            boolean invis = key.func_82150_aj();
                            if (this.showInvis.isToggled()) {
                                key.func_82142_c(false);
                            }
                            mc.func_175598_ae().func_147936_a(key, partialTicks, true);
                            key.func_82142_c(invis);
                        }
                    }
                    this.glowShader.stop();
                    renderingOutlinePass = false;
                    mc.field_71474_y.field_181151_V = shadows;
                    mc.field_71460_t.func_175072_h();
                    mc.field_71460_t.func_78478_c();
                    mc.func_147110_a().func_147610_a(false);
                    this.outlineShader.use();
                    RenderUtils.drawFramebufferFullscreen(this.outlineFramebuffer);
                    this.outlineShader.stop();
                    this.outlineFramebuffer.func_147614_f();
                    mc.func_147110_a().func_147610_a(false);
                    GlStateManager.func_179099_b();
                    GlStateManager.func_179121_F();
                }
            }
        }
    }

    private void render(Entity en, int rgb) {
        if (this.box.isToggled() || this.shaded.isToggled() || this.healthBar.isToggled() || this.ring.isToggled()) {
            if (RenderUtils.isInViewFrustum(en)) {
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
    }

    private void renderTwoD(EntityLivingBase en, int rgb, double expand, float partialTicks) {
        if (RenderUtils.isInViewFrustum(en)) {
            ((IAccessorEntityRenderer) mc.field_71460_t).callSetupCameraTransform(((IAccessorMinecraft) mc).getTimer().field_74281_c, 0);
            double playerX = en.field_70142_S + (en.field_70165_t - en.field_70142_S) * partialTicks - mc.func_175598_ae().field_78730_l;
            double playerY = en.field_70137_T + (en.field_70163_u - en.field_70137_T) * partialTicks - mc.func_175598_ae().field_78731_m;
            double playerZ = en.field_70136_U + (en.field_70161_v - en.field_70136_U) * partialTicks - mc.func_175598_ae().field_78728_n;
            AxisAlignedBB axisAlignedBB = en.func_174813_aQ().func_72314_b(0.1 + expand, 0.1 + expand, 0.1 + expand);
            AxisAlignedBB axis = new AxisAlignedBB(axisAlignedBB.field_72340_a - en.field_70165_t + playerX, axisAlignedBB.field_72338_b - en.field_70163_u + playerY, axisAlignedBB.field_72339_c - en.field_70161_v + playerZ, axisAlignedBB.field_72336_d - en.field_70165_t + playerX, axisAlignedBB.field_72337_e - en.field_70163_u + playerY, axisAlignedBB.field_72334_f - en.field_70161_v + playerZ);
            Vec3[] corners = new Vec3[] { new Vec3(axis.field_72340_a, axis.field_72338_b, axis.field_72339_c), new Vec3(axis.field_72340_a, axis.field_72338_b, axis.field_72334_f), new Vec3(axis.field_72340_a, axis.field_72337_e, axis.field_72339_c), new Vec3(axis.field_72340_a, axis.field_72337_e, axis.field_72334_f), new Vec3(axis.field_72336_d, axis.field_72338_b, axis.field_72339_c), new Vec3(axis.field_72336_d, axis.field_72338_b, axis.field_72334_f), new Vec3(axis.field_72336_d, axis.field_72337_e, axis.field_72339_c), new Vec3(axis.field_72336_d, axis.field_72337_e, axis.field_72334_f) };
            double minX = Double.MAX_VALUE;
            double minY = Double.MAX_VALUE;
            double maxX = Double.MIN_VALUE;
            double maxY = Double.MIN_VALUE;
            boolean isInView = false;
            ScaledResolution scaledResolution = new ScaledResolution(mc);
            for (Vec3 corner : corners) {
                Vec3 screenVec = RenderUtils.convertTo2D(scaledResolution.func_78325_e(), corner.field_72450_a, corner.field_72448_b, corner.field_72449_c);
                if (screenVec != null && !(screenVec.field_72449_c >= 1.0003684) && !(screenVec.field_72449_c <= 0.0)) {
                    isInView = true;
                    double screenX = screenVec.field_72450_a;
                    double screenY = screenVec.field_72448_b;
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
            if (isInView) {
                mc.field_71460_t.func_78478_c();
                ScaledResolution res = new ScaledResolution(mc);
                int screenWidth = res.func_78326_a();
                int screenHeight = res.func_78328_b();
                minX = Math.max(0.0, minX);
                minY = Math.max(0.0, minY);
                maxX = Math.min((double) screenWidth, maxX);
                maxY = Math.min((double) screenHeight, maxY);
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

    private static final class MobEntry {

        final Class<? extends EntityLivingBase> type;

        final Predicate<EntityLivingBase> refine;

        final ButtonSetting enable;

        final ColorSetting color;

        MobEntry(Class<? extends EntityLivingBase> type, Predicate<EntityLivingBase> refine, ButtonSetting enable, ColorSetting color) {
            this.type = type;
            this.refine = refine;
            this.enable = enable;
            this.color = color;
        }

        boolean matches(EntityLivingBase entity) {
            return !this.type.isInstance(entity) ? false : this.refine == null || this.refine.test(entity);
        }
    }
}
