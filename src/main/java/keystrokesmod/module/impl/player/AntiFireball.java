package keystrokesmod.module.impl.player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import keystrokesmod.event.ClientRotationEvent;
import keystrokesmod.event.PrePlayerInputEvent;
import keystrokesmod.event.PrePlayerInteractEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.world.AntiBot;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.ReflectionUtils;
import keystrokesmod.utility.RotationUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class AntiFireball extends Module {

    private final SliderSetting fov;

    private final SliderSetting range;

    private final SliderSetting targetCPS;

    private final SliderSetting rotationSpeed;

    private final ButtonSetting noRotate;

    private final ButtonSetting onlyOnGround;

    private final ButtonSetting sneakWhileActive;

    public EntityFireball fireball;

    private final HashSet<Entity> fireballs = new HashSet<>();

    private long nextClickTimeNanos;

    private EntityFireball scheduledFireball;

    private final Random rand = new Random();

    public AntiFireball() {
        super("Anti Fireball", Module.category.player);
        this.registerSetting(this.targetCPS = new SliderSetting("Target CPS", 12.0, 1.0, 20.0, 0.5));
        this.registerSetting(this.fov = new SliderSetting("FOV", 360.0, 30.0, 360.0, 4.0));
        this.registerSetting(this.range = new SliderSetting("Range", 8.0, 3.0, 15.0, 0.5));
        this.registerSetting(this.rotationSpeed = new SliderSetting("Rotation speed", 15.0, 1.0, 30.0, 1.0));
        this.registerSetting(this.noRotate = new ButtonSetting("No rotate", false));
        this.registerSetting(this.onlyOnGround = new ButtonSetting("Only on ground", false));
        this.registerSetting(this.sneakWhileActive = new ButtonSetting("Sneak while active", false));
    }

    @Override
    public void onDisable() {
        this.fireballs.clear();
        this.fireball = null;
        this.resetClickScheduler();
    }

    @Override
    public void onUpdate() {
        if (Utils.nullCheck() && mc.field_71462_r == null) {
            EntityFireball selectedFireball = this.getFireball();
            if (selectedFireball == null || selectedFireball != this.fireball) {
                this.resetClickScheduler();
            }
            this.fireball = selectedFireball;
        } else {
            this.fireball = null;
            this.resetClickScheduler();
        }
    }

    @SubscribeEvent
    public void onPrePlayerInput(PrePlayerInputEvent e) {
        if (!Utils.nullCheck() || mc.field_71462_r != null) {
            this.resetClickScheduler();
        } else if (!this.isFireballValid(this.fireball)) {
            this.resetClickScheduler();
        } else {
            if (this.sneakWhileActive.isToggled() && !mc.field_71439_g.func_70115_ae() && !mc.field_71439_g.field_71075_bZ.field_75100_b) {
                e.setSneak(true);
            }
        }
    }

    @SubscribeEvent
    public void onClientRotation(ClientRotationEvent e) {
        if (!Utils.nullCheck() || mc.field_71462_r != null) {
            this.resetClickScheduler();
        } else if (ModuleManager.bedAura != null && ModuleManager.bedAura.shouldOverrideMouseOver()) {
            this.resetClickScheduler();
        } else if (this.onlyOnGround.isToggled() && !mc.field_71439_g.field_70122_E) {
            this.resetClickScheduler();
        } else if (!this.isFireballValid(this.fireball)) {
            this.resetClickScheduler();
        } else if (!this.noRotate.isToggled()) {
            float baseYaw = e.yaw != null ? e.yaw : RotationUtils.serverRotations[0];
            float basePitch = e.pitch != null ? e.pitch : RotationUtils.serverRotations[1];
            float[] target = this.computeAimRotations(baseYaw, basePitch);
            float[] smooth = RotationUtils.smoothRotation(baseYaw, basePitch, target[0], target[1], (int) this.rotationSpeed.getInput());
            e.setYaw(smooth[0]);
            e.setPitch(smooth[1]);
        }
    }

    private float[] computeAimRotations(float baseYaw, float basePitch) {
        Vec3 eye = mc.field_71439_g.func_174824_e(1.0F);
        float borderSize = this.fireball.func_70111_Y();
        AxisAlignedBB axisAlignedBB = this.fireball.func_174813_aQ().func_72314_b(borderSize, borderSize, borderSize);
        double reach = mc.field_71442_b.func_78757_d();
        List<EntityPlayer> players = new ArrayList<>();
        for (EntityPlayer player : mc.field_71441_e.field_73010_i) {
            if (player != mc.field_71439_g && player.field_70725_aQ == 0 && !Utils.isFriended(player) && !Utils.isTeammate(player) && !AntiBot.isBot(player)) {
                players.add(player);
            }
        }
        players.sort(Comparator.comparingDouble(p -> mc.field_71439_g.func_70068_e(p)));
        for (EntityPlayer entityPlayer : players) {
            float[] rot = RotationUtils.getRotationsToPoint(entityPlayer.field_70165_t, entityPlayer.field_70163_u, entityPlayer.field_70161_v, baseYaw, basePitch);
            if (this.hitsFireballBox(eye, rot[0], rot[1], axisAlignedBB, reach)) {
                return rot;
            }
        }
        double topY = axisAlignedBB.field_72337_e;
        double centerX = (axisAlignedBB.field_72340_a + axisAlignedBB.field_72336_d) / 2.0;
        double centerZ = (axisAlignedBB.field_72339_c + axisAlignedBB.field_72334_f) / 2.0;
        return RotationUtils.getRotationsToPoint(centerX, topY, centerZ, baseYaw, basePitch);
    }

    private boolean hitsFireballBox(Vec3 eye, float yaw, float pitch, AxisAlignedBB box, double range) {
        Vec3 look = Utils.getLookVec(yaw, pitch);
        Vec3 end = eye.func_72441_c(look.field_72450_a * range, look.field_72448_b * range, look.field_72449_c * range);
        MovingObjectPosition intercept = box.func_72327_a(eye, end);
        return intercept != null;
    }

    @SubscribeEvent
    public void onPrePlayerInteract(PrePlayerInteractEvent e) {
        if (!Utils.nullCheck() || mc.field_71462_r != null) {
            this.resetClickScheduler();
        } else if (ModuleManager.bedAura != null && ModuleManager.bedAura.shouldOverrideMouseOver()) {
            this.resetClickScheduler();
        } else if (this.onlyOnGround.isToggled() && !mc.field_71439_g.field_70122_E) {
            this.resetClickScheduler();
        } else if (!this.isFireballValid(this.fireball)) {
            this.resetClickScheduler();
        } else {
            MovingObjectPosition mop = mc.field_71476_x;
            if (mop != null && mop.field_72313_a == MovingObjectType.ENTITY && mop.field_72308_g == this.fireball) {
                if (this.scheduledFireball != this.fireball) {
                    this.resetClickScheduler();
                    this.scheduledFireball = this.fireball;
                }
                long now = System.nanoTime();
                if (this.nextClickTimeNanos == 0L) {
                    this.nextClickTimeNanos = now;
                }
                int key = mc.field_71474_y.field_74312_F.func_151463_i();
                if (now - this.nextClickTimeNanos >= 0L) {
                    if (!this.isFireballValid(this.fireball) || mc.field_71476_x == null || mc.field_71476_x.field_72313_a != MovingObjectType.ENTITY || mc.field_71476_x.field_72308_g != this.fireball) {
                        this.resetClickScheduler();
                        return;
                    }
                    KeyBinding.func_74507_a(key);
                    ReflectionUtils.setButton(0, true);
                    this.nextClickTimeNanos = now + this.nextDelayNanos();
                }
            } else {
                this.resetClickScheduler();
            }
        }
    }

    private boolean isFireballValid(EntityFireball target) {
        return target != null && mc.field_71441_e != null && !target.field_70128_L && mc.field_71441_e.func_73045_a(target.func_145782_y()) == target && mc.field_71441_e.field_72996_f.contains(target);
    }

    private void resetClickScheduler() {
        this.nextClickTimeNanos = 0L;
        this.scheduledFireball = null;
    }

    private EntityFireball getFireball() {
        double rangeSq = this.range.getInput() * this.range.getInput();
        float fovVal = (float) this.fov.getInput();
        if (this.onlyOnGround.isToggled() && !mc.field_71439_g.field_70122_E) {
            return null;
        } else {
            for (Entity entity : mc.field_71441_e.field_72996_f) {
                if (entity instanceof EntityFireball && this.fireballs.contains(entity) && this.isFireballValid((EntityFireball) entity) && !(mc.field_71439_g.func_70068_e(entity) > rangeSq) && (fovVal == 360.0F || Utils.inFov(fovVal, entity))) {
                    return (EntityFireball) entity;
                }
            }
            return null;
        }
    }

    @SubscribeEvent
    public void onEntityJoin(EntityJoinWorldEvent e) {
        if (!Utils.nullCheck()) {
            this.resetClickScheduler();
        } else {
            if (e.entity == mc.field_71439_g) {
                this.fireballs.clear();
                this.fireball = null;
                this.resetClickScheduler();
            } else if (e.entity instanceof EntityFireball && mc.field_71439_g.func_70068_e(e.entity) > 16.0) {
                this.fireballs.add(e.entity);
            }
        }
    }

    private long nextDelayNanos() {
        int target = Math.max(1, (int) this.targetCPS.getInput());
        int baseDelay = 1000 / target;
        int variation = this.rand.nextInt(Math.max(1, baseDelay / 3 + 1)) - baseDelay / 6;
        return TimeUnit.MILLISECONDS.toNanos(Math.max(33, baseDelay + variation));
    }
}
