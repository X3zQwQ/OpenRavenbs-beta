package keystrokesmod.module.impl.combat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import keystrokesmod.event.ClientRotationEvent;
import keystrokesmod.event.PrePlayerInteractEvent;
import keystrokesmod.helper.RotationHelper;
import keystrokesmod.mixin.impl.accessor.IAccessorEntityRenderer;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.minigames.BedWars;
import keystrokesmod.module.impl.minigames.SkyWars;
import keystrokesmod.module.impl.world.AntiBot;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.ReflectionUtils;
import keystrokesmod.utility.RotationUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.monster.EntityGiantZombie;
import net.minecraft.entity.monster.EntityIronGolem;
import net.minecraft.entity.monster.EntityPigZombie;
import net.minecraft.entity.monster.EntitySilverfish;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingSetAttackTargetEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Mouse;

public class KillAura extends Module {

    private SliderSetting targetCPS;

    private SliderSetting fov;

    private SliderSetting attackRange;

    private SliderSetting swingRange;

    private SliderSetting aimRange;

    public SliderSetting rotationMode;

    private SliderSetting speed;

    private SliderSetting sortMode;

    private SliderSetting switchDelay;

    private SliderSetting targets;

    private ButtonSetting attackMobs;

    private ButtonSetting targetInvis;

    private ButtonSetting disableInInventory;

    private ButtonSetting disableWhileMining;

    private ButtonSetting aimThroughBlocks;

    private ButtonSetting aimThroughEntities;

    private ButtonSetting ignoreTeammates;

    private ButtonSetting prioritizeEnemies;

    private ButtonSetting notUsingItem;

    private ButtonSetting requireMouseDown;

    private ButtonSetting weaponOnly;

    private String[] rotationModes = new String[] { "Silent", "Lock view", "None" };

    private String[] sortModes = new String[] { "Distance", "Health", "Hurt time", "Yaw" };

    public static EntityLivingBase target;

    public static EntityLivingBase attackingEntity;

    private HashMap<Integer, Integer> hitMap = new HashMap<>();

    private List<Entity> hostileMobs = new ArrayList<>();

    private Map<Integer, Boolean> golems = new HashMap<>();

    private long nextClickTime;

    private Random rand;

    private double targetDistance = Double.MAX_VALUE;

    public boolean isRequireMouseDown() {
        return this.requireMouseDown.isToggled();
    }

    public KillAura() {
        super("KillAura", Module.category.combat);
        this.liteModule = true;
        this.registerSetting(this.targetCPS = new SliderSetting("Target CPS", 10.0, 1.0, 20.0, 0.5));
        this.registerSetting(this.fov = new SliderSetting("FOV", "°", 360.0, 30.0, 360.0, 4.0));
        this.registerSetting(this.attackRange = new SliderSetting("Range (attack)", 3.0, 3.0, 6.0, 0.05));
        this.registerSetting(this.swingRange = new SliderSetting("Range (swing)", 4.5, 3.0, 8.0, 0.05));
        this.registerSetting(this.aimRange = new SliderSetting("Range (aim)", 4.5, 3.0, 8.0, 0.05));
        this.registerSetting(this.rotationMode = new SliderSetting("Rotation mode", 0, this.rotationModes));
        this.registerSetting(this.speed = new SliderSetting("Speed", 10.0, 1.0, 30.0, 1.0));
        this.registerSetting(this.sortMode = new SliderSetting("Sort mode", 0, this.sortModes));
        this.registerSetting(this.switchDelay = new SliderSetting("Switch delay", "ms", 200.0, 50.0, 1000.0, 25.0));
        this.registerSetting(this.targets = new SliderSetting("Targets", 3.0, 1.0, 10.0, 1.0));
        this.registerSetting(this.targetInvis = new ButtonSetting("Target invis", true));
        this.registerSetting(this.attackMobs = new ButtonSetting("Attack mobs", false));
        this.registerSetting(this.aimThroughBlocks = new ButtonSetting("Hit through walls", false));
        this.registerSetting(this.aimThroughEntities = new ButtonSetting("Hit through entities", false));
        this.registerSetting(this.disableInInventory = new ButtonSetting("Disable in inventory", true));
        this.registerSetting(this.disableWhileMining = new ButtonSetting("Disable while mining", false));
        this.registerSetting(this.ignoreTeammates = new ButtonSetting("Ignore teammates", true));
        this.registerSetting(this.notUsingItem = new ButtonSetting("Not using item", false));
        this.registerSetting(this.prioritizeEnemies = new ButtonSetting("Prioritize enemies", false));
        this.registerSetting(this.requireMouseDown = new ButtonSetting("Require mouse down", false));
        this.registerSetting(this.weaponOnly = new ButtonSetting("Weapon only", false));
    }

    @Override
    public String getInfo() {
        return this.rotationMode.getInput() == 2.0 ? (int) this.fov.getInput() + this.fov.getSuffix() : this.rotationModes[(int) this.rotationMode.getInput()];
    }

    @Override
    public void onEnable() {
        this.rand = new Random();
        this.nextClickTime = 0L;
    }

    @Override
    public void onDisable() {
        this.hitMap.clear();
        this.setTarget(null);
        this.nextClickTime = 0L;
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onClientRotation(ClientRotationEvent e) {
        if (this.basicCondition() && this.settingCondition()) {
            boolean bedAuraHasPriority = ModuleManager.bedAura != null && ModuleManager.bedAura.shouldOverrideMouseOver() && !ModuleManager.bedAura.isPrioritizingKillAura();
            if (bedAuraHasPriority) {
                this.setTarget(null);
            } else {
                this.handleTarget();
                if (target != null) {
                    this.targetDistance = RotationUtils.distanceFromEyeToClosestOnAABB(target);
                    if (this.rotationMode.getInput() == 0.0) {
                        double aimRangeVal = this.aimRange.getInput();
                        if (this.targetDistance <= aimRangeVal) {
                            int speedVal = (int) this.speed.getInput();
                            boolean useBackup = !this.aimThroughBlocks.isToggled() || !this.aimThroughEntities.isToggled();
                            float[] rot = RotationHelper.get().getRotationsToTarget(target, e, speedVal, 100.0, 100.0, 0.0F, useBackup, aimRangeVal, this.aimThroughBlocks.isToggled(), this.aimThroughEntities.isToggled(), true);
                            if (rot != null) {
                                e.yaw = rot[0];
                                e.pitch = rot[1];
                            }
                        }
                    }
                }
            }
        } else {
            this.setTarget(null);
        }
    }

    @Override
    public void onUpdate() {
        if (this.rotationMode.getInput() == 1.0 && target != null) {
            double aimRangeVal = this.aimRange.getInput();
            if (this.targetDistance <= aimRangeVal) {
                int speedVal = (int) this.speed.getInput();
                boolean useBackup = !this.aimThroughBlocks.isToggled() || !this.aimThroughEntities.isToggled();
                float[] rot = RotationHelper.get().getRotationsToTarget(target, speedVal, 100.0, 100.0, 0.0F, useBackup, aimRangeVal, this.aimThroughBlocks.isToggled(), this.aimThroughEntities.isToggled(), true);
                if (rot != null) {
                    mc.field_71439_g.field_70177_z = rot[0];
                    mc.field_71439_g.field_70125_A = rot[1];
                }
            }
        }
        if (target != null && this.targetDistance <= this.attackRange.getInput()) {
            attackingEntity = target;
        } else {
            attackingEntity = null;
        }
    }

    @SubscribeEvent
    public void onPrePlayerInteract(PrePlayerInteractEvent e) {
        if (Utils.nullCheck()) {
            if (target != null) {
                if (!(this.targetDistance > this.swingRange.getInput())) {
                    int key = mc.field_71474_y.field_74312_F.func_151463_i();
                    long now = System.currentTimeMillis();
                    if (this.nextClickTime == 0L) {
                        this.nextClickTime = now;
                    }
                    int clicks;
                    for (clicks = 0; this.nextClickTime <= now; this.nextClickTime = this.nextClickTime + this.nextDelay()) {
                        clicks++;
                    }
                    if (this.basicCondition() && this.settingCondition()) {
                        if (!this.notUsingItem.isToggled() || !mc.field_71439_g.func_71039_bw()) {
                            for (int i = 0; i < clicks; i++) {
                                KeyBinding.func_74507_a(key);
                                ReflectionUtils.setButton(0, true);
                            }
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onSetAttackTarget(LivingSetAttackTargetEvent e) {
        if (e.entity != null && !this.hostileMobs.contains(e.entity)) {
            if (!(e.target instanceof EntityPlayer) || !e.target.func_70005_c_().equals(mc.field_71439_g.func_70005_c_())) {
                return;
            }
            if (Utils.getBedwarsStatus() == 2 && e.entity instanceof EntityPigZombie) {
                return;
            }
            this.hostileMobs.add(e.entity);
        }
        if (e.target == null && this.hostileMobs.contains(e.entity)) {
            this.hostileMobs.remove(e.entity);
        }
    }

    @SubscribeEvent
    public void onWorldJoin(EntityJoinWorldEvent e) {
        if (e.entity == mc.field_71439_g) {
            this.hitMap.clear();
            this.hostileMobs.clear();
            this.golems.clear();
        }
    }

    private void setTarget(Entity entity) {
        if (!(entity instanceof EntityLivingBase)) {
            target = null;
            attackingEntity = null;
            this.targetDistance = Double.MAX_VALUE;
            this.nextClickTime = 0L;
        } else {
            target = (EntityLivingBase) entity;
        }
    }

    private void handleTarget() {
        double maxRange = Math.max(this.attackRange.getInput(), this.aimRange.getInput());
        float fovValue = (float) this.fov.getInput();
        List<KillAura.KillAuraTarget> candidates = new ArrayList<>();
        for (Entity entity : mc.field_71441_e.field_72996_f) {
            KillAura.Candidate candidate = this.getCandidateTarget(entity, maxRange, fovValue);
            if (candidate != null) {
                KillAura.KillAuraTarget auraTarget = this.buildKillAuraTarget(candidate.entity, candidate.distance, maxRange);
                if (auraTarget != null) {
                    candidates.add(auraTarget);
                }
            }
        }
        if (this.prioritizeEnemies.isToggled()) {
            List<KillAura.KillAuraTarget> enemies = new ArrayList<>();
            for (KillAura.KillAuraTarget candidate : candidates) {
                if (candidate.isEnemy) {
                    enemies.add(candidate);
                }
            }
            if (!enemies.isEmpty()) {
                candidates = enemies;
            }
        }
        candidates.sort(this.getTargetComparator().thenComparingDouble(c -> c.distance));
        double attackRangeValue = this.attackRange.getInput();
        List<KillAura.KillAuraTarget> attackTargets = new ArrayList<>();
        for (KillAura.KillAuraTarget killAuraTarget : candidates) {
            if (killAuraTarget.distance <= attackRangeValue) {
                attackTargets.add(killAuraTarget);
            }
        }
        if (!attackTargets.isEmpty()) {
            KillAura.KillAuraTarget selectedAttackTarget = this.selectAttackTarget(attackTargets);
            if (selectedAttackTarget != null) {
                this.setTarget(selectedAttackTarget.entity);
            }
        } else if (!candidates.isEmpty()) {
            this.setTarget(candidates.get(0).entity);
        } else {
            this.setTarget(null);
        }
    }

    private KillAura.Candidate getCandidateTarget(Entity entity, double maxRange, float fovValue) {
        if (entity instanceof EntityLivingBase && entity != mc.field_71439_g && !entity.field_70128_L) {
            boolean targetableUnicorn = entity instanceof EntityHorse && BedWars.shouldTargetUnicorn();
            if (entity instanceof EntityPlayer) {
                EntityPlayer player = (EntityPlayer) entity;
                if (Utils.isFriended(player) || player.field_70725_aQ != 0) {
                    return null;
                }
                if (AntiBot.isBot(entity) || this.ignoreTeammates.isToggled() && Utils.isTeammate(entity)) {
                    return null;
                }
            } else if (targetableUnicorn) {
                if (((EntityHorse) entity).field_70725_aQ != 0) {
                    return null;
                }
            } else {
                if (!(entity instanceof EntityCreature) || !this.attackMobs.isToggled()) {
                    return null;
                }
                EntityCreature creature = (EntityCreature) entity;
                if (creature.field_70714_bg == null || creature.func_175446_cd() || creature.field_70725_aQ != 0) {
                    return null;
                }
                String canonicalName = entity.getClass().getCanonicalName();
                if (canonicalName == null || !canonicalName.startsWith("net.minecraft.entity.monster.")) {
                    return null;
                }
            }
            if (entity.func_82150_aj() && !this.targetInvis.isToggled()) {
                return null;
            } else if (fovValue != 360.0F && !Utils.inFov(fovValue, entity)) {
                return null;
            } else {
                double distance = RotationUtils.distanceFromEyeToClosestOnAABB(entity);
                return distance > maxRange ? null : new KillAura.Candidate((EntityLivingBase) entity, distance);
            }
        } else {
            return null;
        }
    }

    private KillAura.KillAuraTarget buildKillAuraTarget(EntityLivingBase entity, double doubleValue, double maxRange) {
        boolean targetableUnicorn = entity instanceof EntityHorse && BedWars.shouldTargetUnicorn();
        if (entity instanceof EntityCreature && !targetableUnicorn && this.attackMobs.isToggled() && !this.isHostile((EntityCreature) entity)) {
            return null;
        } else {
            double multipointH = 100.0;
            double multipointV = 100.0;
            if (!RotationUtils.hasValidAimPoint(entity, multipointH, multipointV, maxRange, this.aimThroughBlocks.isToggled(), this.aimThroughEntities.isToggled(), true)) {
                return null;
            } else {
                boolean isEnemyPlayer = entity instanceof EntityPlayer && Utils.isEnemy((EntityPlayer) entity);
                return new KillAura.KillAuraTarget(entity, doubleValue, entity.func_110143_aJ(), entity.field_70737_aN, RotationUtils.distanceFromYaw(entity, false), entity.func_145782_y(), isEnemyPlayer);
            }
        }
    }

    private Comparator<KillAura.KillAuraTarget> getTargetComparator() {
        switch((int) this.sortMode.getInput()) {
            case 0:
            default:
                return Comparator.comparingDouble(target -> target.distance);
            case 1:
                return Comparator.comparingDouble(target -> target.health);
            case 2:
                return Comparator.comparingInt(target -> target.hurttime);
            case 3:
                return Comparator.comparingDouble(target -> target.yawDelta);
        }
    }

    private KillAura.KillAuraTarget selectAttackTarget(List<KillAura.KillAuraTarget> attackTargets) {
        int ticksExisted = mc.field_71439_g.field_70173_aa;
        int switchDelayTicks = (int) (this.switchDelay.getInput() / 50.0);
        long noHitTicks = (long) Math.min((double) attackTargets.size(), this.targets.getInput()) * switchDelayTicks;
        for (KillAura.KillAuraTarget candidate : attackTargets) {
            Integer firstHitTick = this.hitMap.get(candidate.entityId);
            if (firstHitTick != null && ticksExisted - firstHitTick < switchDelayTicks) {
                return candidate;
            }
        }
        for (KillAura.KillAuraTarget killAuraTarget2 : attackTargets) {
            Integer firstHitTick = this.hitMap.get(killAuraTarget2.entityId);
            if (firstHitTick == null || ticksExisted >= firstHitTick.intValue() + noHitTicks) {
                this.hitMap.put(killAuraTarget2.entityId, ticksExisted);
                return killAuraTarget2;
            }
        }
        return null;
    }

    private boolean isHostile(EntityCreature entityCreature) {
        if (SkyWars.onlyAuraHostiles()) {
            return entityCreature instanceof EntityGiantZombie ? false : !ModuleManager.skyWars.spawnedMobs.contains(entityCreature.func_145782_y());
        } else if (entityCreature instanceof EntitySilverfish) {
            String teamColor = Utils.getFirstColorCode(entityCreature.func_95999_t());
            String teamColorSelf = Utils.getFirstColorCode(mc.field_71439_g.func_145748_c_().func_150254_d());
            return teamColor.isEmpty() || !teamColorSelf.equals(teamColor) && !Utils.isTeammate(entityCreature);
        } else if (!(entityCreature instanceof EntityIronGolem)) {
            return entityCreature instanceof EntityPigZombie && Utils.getBedwarsStatus() != 2 ? false : this.hostileMobs.contains(entityCreature);
        } else if (Utils.getBedwarsStatus() != 2) {
            return true;
        } else if (this.golems.containsKey(entityCreature.func_145782_y())) {
            return !this.golems.getOrDefault(entityCreature.func_145782_y(), false);
        } else {
            double nearestDistance = -1.0;
            EntityArmorStand nearestArmorStand = null;
            for (Entity entity : mc.field_71441_e.field_72996_f) {
                if (entity instanceof EntityArmorStand) {
                    String stripped = Utils.stripString(entity.func_145748_c_().func_150254_d());
                    if (stripped.contains("[") && stripped.endsWith("]")) {
                        double distanceSq = entity.func_70092_e(entityCreature.field_70165_t, entityCreature.field_70163_u, entityCreature.field_70161_v);
                        if (distanceSq < nearestDistance || nearestDistance == -1.0) {
                            nearestDistance = distanceSq;
                            nearestArmorStand = (EntityArmorStand) entity;
                        }
                    }
                }
            }
            if (nearestArmorStand == null) {
                return !ModuleManager.bedwars.spawnedMobs.contains(entityCreature.func_145782_y());
            } else {
                String teamColor = Utils.getFirstColorCode(nearestArmorStand.func_145748_c_().func_150254_d());
                String teamColorSelf = Utils.getFirstColorCode(mc.field_71439_g.func_145748_c_().func_150254_d());
                boolean isTeam = !teamColor.isEmpty() && (teamColorSelf.equals(teamColor) || Utils.isTeammate(nearestArmorStand));
                this.golems.put(entityCreature.func_145782_y(), isTeam);
                return !isTeam;
            }
        }
    }

    private boolean basicCondition() {
        return !Utils.nullCheck() ? false : !mc.field_71439_g.field_70128_L;
    }

    private boolean settingCondition() {
        if (this.requireMouseDown.isToggled() && !Mouse.isButtonDown(0)) {
            return false;
        } else if (this.weaponOnly.isToggled() && !Utils.holdingWeapon()) {
            return false;
        } else {
            return this.disableWhileMining.isToggled() && Utils.isMining() ? false : !this.disableInInventory.isToggled() || mc.field_71462_r == null;
        }
    }

    private long nextDelay() {
        int cps = Math.max(1, (int) this.targetCPS.getInput());
        int baseDelay = 1000 / cps;
        int finalDelay = baseDelay + (this.rand.nextInt(21) - 10);
        return Math.max(33, Math.min(180, finalDelay));
    }

    public SliderSetting getAttackRangeSetting() {
        return this.attackRange;
    }

    public SliderSetting getSwingRangeSetting() {
        return this.swingRange;
    }

    public SliderSetting getAimRangeSetting() {
        return this.aimRange;
    }

    public boolean shouldOverrideMouseOver() {
        return this.isEnabled() && Utils.nullCheck() && attackingEntity != null && target == attackingEntity && this.basicCondition() && this.targetDistance <= this.swingRange.getInput();
    }

    public void modifyMouseOverFromGetMouseOver(float partialTicks) {
        if (this.shouldOverrideMouseOver()) {
            Entity viewEntity = mc.func_175606_aa();
            if (viewEntity != null) {
                Vec3 eyes = viewEntity.func_174824_e(partialTicks);
                Vec3 look = viewEntity.func_70676_i(partialTicks);
                double reach = this.attackRange.getInput();
                Vec3 rayEnd = eyes.func_72441_c(look.field_72450_a * reach, look.field_72448_b * reach, look.field_72449_c * reach);
                float border = attackingEntity.func_70111_Y();
                AxisAlignedBB bb = attackingEntity.func_174813_aQ().func_72314_b(border, border, border);
                MovingObjectPosition intercept = bb.func_72327_a(eyes, rayEnd);
                boolean inside = bb.func_72318_a(eyes);
                if (inside || intercept != null) {
                    Vec3 hitVec = inside ? (intercept == null ? eyes : intercept.field_72307_f) : intercept.field_72307_f;
                    if (!this.aimThroughBlocks.isToggled()) {
                        MovingObjectPosition blockHit = RotationUtils.rayTraceBlocksIgnoringOpenFenceGates(eyes, hitVec, false, false, true);
                        if (blockHit != null && blockHit.field_72313_a == MovingObjectType.BLOCK) {
                            return;
                        }
                    }
                    if (this.aimThroughEntities.isToggled() || !RotationUtils.isPathBlockedByEntity(eyes, hitVec, attackingEntity)) {
                        mc.field_71476_x = new MovingObjectPosition(attackingEntity, hitVec);
                        mc.field_147125_j = attackingEntity;
                        EntityRenderer renderer = mc.field_71460_t;
                        if (renderer instanceof IAccessorEntityRenderer) {
                            ((IAccessorEntityRenderer) renderer).setPointedEntity(attackingEntity);
                        }
                    }
                }
            }
        }
    }

    private static final class Candidate {

        final EntityLivingBase entity;

        final double distance;

        Candidate(EntityLivingBase entity, double distance) {
            this.entity = entity;
            this.distance = distance;
        }
    }

    static class KillAuraTarget {

        final EntityLivingBase entity;

        final double distance;

        final float health;

        final int hurttime;

        final double yawDelta;

        final int entityId;

        final boolean isEnemy;

        public KillAuraTarget(EntityLivingBase entity, double distance, float health, int hurttime, double yawDelta, int entityId, boolean isEnemy) {
            this.entity = entity;
            this.distance = distance;
            this.health = health;
            this.hurttime = hurttime;
            this.yawDelta = yawDelta;
            this.entityId = entityId;
            this.isEnemy = isEnemy;
        }
    }
}
