package keystrokesmod.module.impl.render;

import java.awt.Color;
import java.io.IOException;
import keystrokesmod.event.AttackEvent;
import keystrokesmod.event.ClickMouseEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.combat.AimAssist;
import keystrokesmod.module.impl.combat.KillAura;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.DescriptionSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.RenderUtils;
import keystrokesmod.utility.Theme;
import keystrokesmod.utility.Timer;
import keystrokesmod.utility.Utils;
import keystrokesmod.utility.shader.BlurUtils;
import keystrokesmod.utility.shader.RoundedUtils;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.client.config.GuiButtonExt;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.RenderTickEvent;
import org.lwjgl.opengl.GL11;

public class TargetHUD extends Module {

    private SliderSetting mode;

    private SliderSetting theme;

    private ButtonSetting renderEsp;

    private ButtonSetting showDifference;

    private ButtonSetting showStatus;

    private ButtonSetting healthColor;

    private ButtonSetting manual;

    private ButtonSetting aimAssist;

    private ButtonSetting killAura;

    private Timer fadeTimer;

    private Timer healthBarTimer = null;

    private EntityLivingBase target;

    private EntityLivingBase manualClickTarget;

    private long lastAliveMS;

    private double lastHealth;

    private float lastHealthBar;

    public int posX = 70;

    public int posY = 30;

    private String[] modes = new String[] { "Modern", "Legacy" };

    public TargetHUD() {
        super("TargetHUD", Module.category.render);
        this.liteModule = true;
        this.registerSetting(this.mode = new SliderSetting("Mode", true, 1, this.modes));
        this.registerSetting(this.theme = new SliderSetting("Theme", 0, Theme.THEMES_SETTING));
        this.registerSetting(new ButtonSetting("Edit position", () -> mc.func_147108_a(new TargetHUD.EditScreen())));
        this.registerSetting(this.renderEsp = new ButtonSetting("Render ESP", true));
        this.registerSetting(this.showDifference = new ButtonSetting("Show difference", true));
        this.registerSetting(this.showStatus = new ButtonSetting("Show win or loss", true));
        this.registerSetting(this.healthColor = new ButtonSetting("Traditional health color", false));
        this.registerSetting(new DescriptionSetting("Targets"));
        this.registerSetting(this.manual = new ButtonSetting("Manual", false));
        this.registerSetting(this.aimAssist = new ButtonSetting("AimAssist", false));
        this.registerSetting(this.killAura = new ButtonSetting("KillAura", true));
    }

    @Override
    public void onDisable() {
        this.reset();
    }

    @SubscribeEvent
    public void onClickMouse(ClickMouseEvent event) {
        this.manualClickTarget = null;
        if (this.manual.isToggled() && Utils.nullCheck() && mc.field_71476_x != null) {
            Entity clickedEntity = mc.field_71476_x.field_72308_g;
            if (clickedEntity instanceof EntityLivingBase) {
                if (ModuleManager.killAura == null || !ModuleManager.killAura.isEnabled() || KillAura.attackingEntity != clickedEntity) {
                    this.manualClickTarget = (EntityLivingBase) clickedEntity;
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onAttack(AttackEvent event) {
        if (this.manualClickTarget != null) {
            if (event.attacker == mc.field_71439_g && event.target == this.manualClickTarget) {
                this.activateTarget(this.manualClickTarget);
            }
            this.manualClickTarget = null;
        }
    }

    @SubscribeEvent
    public void onRenderTick(RenderTickEvent ev) {
        if (!Utils.nullCheck()) {
            this.reset();
        } else {
            if (ev.phase == Phase.END) {
                if (mc.field_71462_r != null) {
                    this.reset();
                    return;
                }
                EntityLivingBase activeTarget = this.getActiveTarget();
                if (activeTarget != null) {
                    this.activateTarget(activeTarget);
                } else {
                    if (this.target == null) {
                        return;
                    }
                    if (System.currentTimeMillis() - this.lastAliveMS >= 400L && this.fadeTimer == null) {
                        (this.fadeTimer = new Timer(400.0F)).start();
                    }
                }
                if (this.fadeTimer != null && this.fadeTimer.getValueInt(0, 255, 1) >= 255) {
                    this.target = null;
                    this.healthBarTimer = null;
                    return;
                }
                if (this.mode.getInput() == -1.0) {
                    return;
                }
                String playerInfo = this.target.func_145748_c_().func_150254_d();
                double health = this.target.func_110143_aJ() / this.target.func_110138_aP();
                if (this.target.field_70128_L) {
                    health = 0.0;
                }
                if (health != this.lastHealth) {
                    (this.healthBarTimer = new Timer(this.mode.getInput() == 0.0 ? 500.0F : 350.0F)).start();
                }
                this.lastHealth = health;
                playerInfo = playerInfo + " " + Utils.getHealthStr(this.target, true);
                this.drawTargetHUD(this.fadeTimer, playerInfo, health);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRenderWorld(RenderWorldLastEvent renderWorldLastEvent) {
        if (this.renderEsp.isToggled() && Utils.nullCheck()) {
            EntityLivingBase hudTarget = this.target;
            if (hudTarget != null && !hudTarget.field_70128_L) {
                if (!ModuleManager.backtrack.isRenderingServerPositionFor(hudTarget)) {
                    RenderUtils.renderEntity(hudTarget, 2, 0.0, 0.0, Theme.getGradient((int) this.theme.getInput(), 0.0), false);
                }
            }
        }
    }

    private EntityLivingBase getActiveTarget() {
        if (this.killAura.isToggled() && KillAura.attackingEntity != null) {
            return KillAura.attackingEntity;
        } else {
            if (this.aimAssist.isToggled()) {
                Module aimAssistModule = ModuleManager.getModule(AimAssist.class);
                if (aimAssistModule instanceof AimAssist && aimAssistModule.isEnabled()) {
                    Entity aimAssistTarget = ((AimAssist) aimAssistModule).getAimAssistTarget();
                    if (aimAssistTarget instanceof EntityLivingBase) {
                        return (EntityLivingBase) aimAssistTarget;
                    }
                }
            }
            return null;
        }
    }

    private void activateTarget(EntityLivingBase activeTarget) {
        this.target = activeTarget;
        this.lastAliveMS = System.currentTimeMillis();
        this.fadeTimer = null;
    }

    private void drawTargetHUD(Timer fadeTimer, String string, double health) {
        if (this.mode.getInput() != -1.0) {
            if (this.showDifference.isToggled() && this.target != null) {
                float enemyHealth = this.target.field_70128_L ? 0.0F : Utils.getTotalHealth(this.target);
                float playerHealth = Utils.getTotalHealth(mc.field_71439_g);
                double diff = playerHealth - enemyHealth;
                double percent = playerHealth / mc.field_71439_g.func_110138_aP() - enemyHealth / this.target.func_110138_aP();
                diff = Utils.round(diff, 1);
                String color = percent < -0.4 ? "§c" : (percent < -0.15 ? "§6" : (percent <= 0.15 ? "§e" : (percent <= 0.4 ? "§a" : "§2")));
                if (diff > 0.0) {
                    string = string + " " + color + "+" + Utils.asWholeNum(diff);
                } else if (diff < 0.0) {
                    string = string + " " + color + "-" + Utils.asWholeNum(Math.abs(diff));
                }
            }
            if (this.showStatus.isToggled()) {
                string = string + " " + (health <= Utils.getTotalHealth(mc.field_71439_g) / mc.field_71439_g.func_110138_aP() ? "§aW" : "§cL");
            }
            ScaledResolution scaledResolution = new ScaledResolution(mc);
            int padding = 8;
            int targetStrWithPadding = mc.field_71466_p.func_78256_a(string) + 8;
            int x = scaledResolution.func_78326_a() / 2 - targetStrWithPadding / 2 + this.posX;
            int y = scaledResolution.func_78328_b() / 2 + 15 + this.posY;
            int n6 = x - 8;
            int n7 = y - 8;
            int n8 = x + targetStrWithPadding;
            int n9 = y + mc.field_71466_p.field_78288_b + 5 - 6 + 8;
            int alpha = fadeTimer == null ? 255 : 255 - fadeTimer.getValueInt(0, 255, 1);
            if (alpha > 0) {
                int maxAlphaOutline = alpha > 110 ? 110 : alpha;
                int maxAlphaBackground = alpha > 210 ? 210 : alpha;
                int[] gradientColors = Theme.getGradients((int) this.theme.getInput());
                switch((int) this.mode.getInput()) {
                    case 0:
                        float bloomRadius = fadeTimer == null ? 2.0F : 2.0F * alpha / 255.0F;
                        float blurRadius = fadeTimer == null ? 3.0F : 3.0F * alpha / 255.0F;
                        BlurUtils.prepareBloom();
                        RoundedUtils.drawRound(n6, n7, Math.abs((float) n6 - n8), Math.abs((float) n7 - (n9 + 13)), 8.0F, true, new Color(0, 0, 0, maxAlphaBackground));
                        BlurUtils.bloomEnd(3, bloomRadius);
                        BlurUtils.prepareBlur();
                        RoundedUtils.drawRound(n6, n7, Math.abs((float) n6 - n8), Math.abs((float) n7 - (n9 + 13)), 8.0F, true, new Color(Utils.mergeAlpha(Color.black.getRGB(), maxAlphaOutline)));
                        BlurUtils.blurEnd(2, blurRadius);
                        break;
                    case 1:
                        RenderUtils.drawRoundedGradientOutlinedRectangle(n6, n7, n8, n9 + 13, 10.0F, Utils.mergeAlpha(Color.black.getRGB(), maxAlphaOutline), Utils.mergeAlpha(gradientColors[0], alpha), Utils.mergeAlpha(gradientColors[1], alpha));
                }
                int n13 = n6 + 6;
                int n14 = n8 - 6;
                RenderUtils.drawRoundedRectangle(n13, n9, n14, n9 + 5, 4.0F, Utils.mergeAlpha(Color.black.getRGB(), maxAlphaOutline));
                int mergedGradientLeft = Utils.mergeAlpha(gradientColors[0], maxAlphaBackground);
                int mergedGradientRight = Utils.mergeAlpha(gradientColors[1], maxAlphaBackground);
                float healthBar = (int) (n14 + (n13 - n14) * (1.0 - health));
                boolean smoothBack = false;
                if (healthBar != this.lastHealthBar && this.lastHealthBar - n13 >= 3.0F && this.healthBarTimer != null) {
                    int type = this.mode.getInput() == 0.0 ? 4 : 1;
                    float diff = this.lastHealthBar - healthBar;
                    if (diff > 0.0F) {
                        this.lastHealthBar = this.lastHealthBar - this.healthBarTimer.getValueFloat(0.0F, diff, type);
                    } else {
                        smoothBack = true;
                        this.lastHealthBar = this.healthBarTimer.getValueFloat(this.lastHealthBar, healthBar, type);
                    }
                } else {
                    this.lastHealthBar = healthBar;
                }
                if (this.healthColor.isToggled()) {
                    mergedGradientLeft = mergedGradientRight = Utils.mergeAlpha(Utils.getColorForHealth(health), maxAlphaBackground);
                }
                if (this.lastHealthBar > n14) {
                    this.lastHealthBar = n14;
                }
                switch((int) this.mode.getInput()) {
                    case 0:
                        RenderUtils.drawRoundedRectangle(n13, n9, this.lastHealthBar, n9 + 5, 4.0F, Utils.darkenColor(mergedGradientRight, 25.0));
                        RenderUtils.drawRoundedGradientRect(n13, n9, smoothBack ? this.lastHealthBar : healthBar, n9 + 5, 4.0F, mergedGradientLeft, mergedGradientLeft, mergedGradientRight, mergedGradientRight);
                        break;
                    case 1:
                        RenderUtils.drawRoundedGradientRect(n13, n9, this.lastHealthBar, n9 + 5, 4.0F, mergedGradientLeft, mergedGradientLeft, mergedGradientRight, mergedGradientRight);
                }
                GL11.glPushMatrix();
                GL11.glEnable(3042);
                mc.field_71466_p.func_175065_a(string, x, y, new Color(220, 220, 220, 255).getRGB() & 16777215 | Utils.clamp(alpha + 15) << 24, true);
                GL11.glDisable(3042);
                GL11.glPopMatrix();
            }
        }
    }

    private void reset() {
        this.fadeTimer = null;
        this.target = null;
        this.manualClickTarget = null;
        this.healthBarTimer = null;
    }

    public boolean isEspActiveFor(EntityLivingBase entity) {
        return this.isEnabled() && this.renderEsp.isToggled() && entity != null && this.target == entity;
    }

    public Color getCurrentEspColor(int alpha) {
        int color = Theme.getGradient((int) this.theme.getInput(), 0.0);
        int safeAlpha = Math.max(0, Math.min(255, alpha));
        return new Color(color >> 16 & 0xFF, color >> 8 & 0xFF, color & 0xFF, safeAlpha);
    }

    class EditScreen extends GuiScreen {

        GuiButtonExt resetPosition;

        boolean d = false;

        int miX = 0;

        int miY = 0;

        int maX = 0;

        int maY = 0;

        int aX = 70;

        int aY = 30;

        int laX = 0;

        int laY = 0;

        int lmX = 0;

        int lmY = 0;

        int clickMinX = 0;

        public void func_73866_w_() {
            super.func_73866_w_();
            this.field_146292_n.add(this.resetPosition = new GuiButtonExt(1, this.field_146294_l - 90, this.field_146295_m - 25, 85, 20, "Reset position"));
            this.aX = TargetHUD.this.posX;
            this.aY = TargetHUD.this.posY;
        }

        public void func_73863_a(int mX, int mY, float pt) {
            ScaledResolution res = new ScaledResolution(this.field_146297_k);
            func_73734_a(0, 0, this.field_146294_l, this.field_146295_m, -1308622848);
            int miX = this.aX;
            int miY = this.aY;
            String playerInfo = this.field_146297_k.field_71439_g.func_145748_c_().func_150254_d();
            double health = this.field_146297_k.field_71439_g.func_110143_aJ() / this.field_146297_k.field_71439_g.func_110138_aP();
            if (this.field_146297_k.field_71439_g.field_70128_L) {
                health = 0.0;
            }
            TargetHUD.this.lastHealth = health;
            playerInfo = playerInfo + " " + Utils.getHealthStr(this.field_146297_k.field_71439_g, true);
            TargetHUD.this.drawTargetHUD(null, playerInfo, health);
            if (TargetHUD.this.showStatus.isToggled()) {
                playerInfo = playerInfo + " " + (health <= Utils.getTotalHealth(this.field_146297_k.field_71439_g) / this.field_146297_k.field_71439_g.func_110138_aP() ? "§aW" : "§cL");
            }
            int stringWidth = this.field_146297_k.field_71466_p.func_78256_a(playerInfo) + 8;
            int maX = res.func_78326_a() / 2 - stringWidth / 2 + miX + this.field_146297_k.field_71466_p.func_78256_a(playerInfo) + 8;
            int maY = res.func_78328_b() / 2 + 15 + miY + this.field_146297_k.field_71466_p.field_78288_b + 5 - 6 + 8;
            this.miX = miX;
            this.miY = miY;
            this.maX = maX;
            this.maY = maY;
            this.clickMinX = miX;
            TargetHUD.this.posX = miX;
            TargetHUD.this.posY = miY;
            String edit = "Edit the HUD position by dragging.";
            int x = res.func_78326_a() / 2 - this.field_146289_q.func_78256_a(edit) / 2;
            int y = res.func_78328_b() / 2 - 20;
            RenderUtils.drawColoredString(edit, '-', x, y, 2L, 0L, true, this.field_146297_k.field_71466_p);
            try {
                this.func_146269_k();
            } catch (IOException exception) {
            }
            super.func_73863_a(mX, mY, pt);
        }

        protected void func_146273_a(int mX, int mY, int b, long t) {
            super.func_146273_a(mX, mY, b, t);
            if (b == 0) {
                if (this.d) {
                    this.aX = this.laX + (mX - this.lmX);
                    this.aY = this.laY + (mY - this.lmY);
                } else if (mX > this.clickMinX && mX < this.maX && mY > this.miY && mY < this.maY) {
                    this.d = true;
                    this.lmX = mX;
                    this.lmY = mY;
                    this.laX = this.aX;
                    this.laY = this.aY;
                }
            }
        }

        protected void func_146286_b(int mX, int mY, int s) {
            super.func_146286_b(mX, mY, s);
            if (s == 0) {
                this.d = false;
            }
        }

        public void func_146284_a(GuiButton b) {
            if (b == this.resetPosition) {
                this.aX = TargetHUD.this.posX = 70;
                this.aY = TargetHUD.this.posY = 30;
            }
        }

        public boolean func_73868_f() {
            return false;
        }
    }
}
