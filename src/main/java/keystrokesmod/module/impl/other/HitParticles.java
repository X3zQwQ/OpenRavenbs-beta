package keystrokesmod.module.impl.other;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Map;
import java.util.Random;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.DescriptionSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.Utils;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.init.Blocks;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;

public class HitParticles extends Module {

    private static final int PARTICLE_CRIT = 4;

    private static final int PARTICLE_CRIT_MAGIC = 13;

    private static final double ARROW_SCAN_EXPAND = 96.0;

    private static final int[] ARGS_NONE = new int[0];

    private static final Random RANDOM = new Random();

    private final ButtonSetting onMelee;

    private final ButtonSetting onRanged;

    private final SliderSetting meleeParticle;

    private final SliderSetting rangedParticle;

    private final SliderSetting meleeMultiplier;

    private final SliderSetting rangedMultiplier;

    private final Map<Integer, Integer> rangedSpawnForArrow = new HashMap<>();

    private static final String[] PARTICLE_NAMES = new String[] { "Angry Villager", "Blood", "Cloud", "Confetti", "Critical", "Crit/Magic Crit", "Enchantment", "Explosion", "Flame", "Happy Villager", "Heart", "Instant Spell", "Lava", "Magic Critical", "Mob Spell", "Music Note", "Portal", "Slime", "Smoke", "Snow", "Spark", "Spell", "Splash", "Witch" };

    private static final int[] PARTICLE_IDS = new int[] { 20, 37, 32, 30, 9, -1, 25, 1, 26, 21, 34, 14, 27, 10, 15, 23, 24, 33, 11, 31, 3, 13, 5, 17 };

    private static final int[] PARTICLE_COUNTS = new int[] { 8, 32, 16, 16, 32, -1, 16, 1, 16, 16, 8, 16, 16, 32, 16, 8, 32, 16, 16, 16, 16, 16, 32, 16 };

    private static final float[] PARTICLE_OFFSETS = new float[] { 2.5F, 1.2F, 0.2F, 3.0F, 1.0F, -1.0F, 1.0F, 1.0F, 0.05F, 2.5F, 2.0F, 0.7F, 1.0F, 1.0F, 1.0F, 3.0F, 1.0F, 0.5F, 0.08F, 1.0F, 0.1F, 1.0F, 1.0F, 0.5F };

    private static final boolean[] PARTICLE_IGNORE_DIST = new boolean[] { true, true, false, true, false, false, true, true, false, true, true, false, false, false, false, true, true, false, true, true, false, false, false, false };

    private static final int[][] PARTICLE_ARGS = new int[][] { ARGS_NONE, { Block.func_149682_b(Blocks.field_150424_aL) }, ARGS_NONE, ARGS_NONE, ARGS_NONE, ARGS_NONE, ARGS_NONE, ARGS_NONE, ARGS_NONE, ARGS_NONE, ARGS_NONE, ARGS_NONE, ARGS_NONE, ARGS_NONE, ARGS_NONE, ARGS_NONE, ARGS_NONE, ARGS_NONE, ARGS_NONE, ARGS_NONE, ARGS_NONE, ARGS_NONE, ARGS_NONE, ARGS_NONE };

    public HitParticles() {
        super("Hit Particles", Module.category.other);
        this.registerSetting(new DescriptionSetting("Melee"));
        this.registerSetting(this.onMelee = new ButtonSetting("Melee hits", true));
        this.registerSetting(this.meleeParticle = new SliderSetting("Melee particle", 4, PARTICLE_NAMES));
        this.registerSetting(this.meleeMultiplier = new SliderSetting("Melee multiplier", 1.0, 1.0, 8.0, 1.0));
        this.registerSetting(new DescriptionSetting("Ranged"));
        this.registerSetting(this.onRanged = new ButtonSetting("Arrow hits", true));
        this.registerSetting(this.rangedParticle = new SliderSetting("Ranged particle", 4, PARTICLE_NAMES));
        this.registerSetting(this.rangedMultiplier = new SliderSetting("Ranged multiplier", 1.0, 1.0, 8.0, 1.0));
    }

    @Override
    public void onDisable() {
        this.rangedSpawnForArrow.clear();
    }

    @SubscribeEvent
    public void onAttackEntity(AttackEntityEvent event) {
        if (this.isEnabled() && this.onMelee.isToggled() && Utils.nullCheck()) {
            if (event.entityPlayer == mc.field_71439_g && event.target instanceof EntityLivingBase) {
                EntityLivingBase target = (EntityLivingBase) event.target;
                if (target.field_70737_aN <= 5 && target.field_70172_ad <= 0) {
                    int intValue = (int) this.meleeParticle.getInput();
                    this.spawnParticleType(intValue, target, (int) this.meleeMultiplier.getInput());
                }
            }
        }
    }

    @SubscribeEvent
    public void onClientTick(ClientTickEvent event) {
        if (this.isEnabled() && this.onRanged.isToggled() && event.phase == Phase.END && Utils.nullCheck()) {
            this.pruneArrowDedupeMap();
            AxisAlignedBB scan = mc.field_71439_g.func_174813_aQ().func_72314_b(96.0, 96.0, 96.0);
            List<EntityArrow> arrows = mc.field_71441_e.func_72872_a(EntityArrow.class, scan);
            int i = 0;
            for (int n = arrows.size(); i < n; i++) {
                EntityArrow arrow = arrows.get(i);
                if (arrow.field_70250_c == mc.field_71439_g) {
                    EntityLivingBase target = getCollisionEntity(arrow);
                    if (target != null && target.field_70737_aN <= 0) {
                        int aid = arrow.func_145782_y();
                        int tid = target.func_145782_y();
                        Integer prev = this.rangedSpawnForArrow.get(aid);
                        if (prev == null || prev != tid) {
                            this.rangedSpawnForArrow.put(aid, tid);
                            int intValue2 = (int) this.rangedParticle.getInput();
                            this.spawnParticleType(intValue2, target, (int) this.rangedMultiplier.getInput());
                        }
                    }
                }
            }
        }
    }

    private void pruneArrowDedupeMap() {
        Iterator<Entry<Integer, Integer>> it = this.rangedSpawnForArrow.entrySet().iterator();
        while (it.hasNext()) {
            Entry<Integer, Integer> entry = it.next();
            if (mc.field_71441_e.func_73045_a(entry.getKey()) == null) {
                it.remove();
            }
        }
    }

    private void spawnParticleType(int index, Entity entity, int multiplier) {
        if (index >= 0 && index < PARTICLE_IDS.length) {
            int id = PARTICLE_IDS[index];
            if (id == -1) {
                spawnParticleRaw(9, entity, multiplier, PARTICLE_COUNTS[4], PARTICLE_OFFSETS[4], PARTICLE_IGNORE_DIST[4], PARTICLE_ARGS[4]);
                spawnParticleRaw(10, entity, multiplier, PARTICLE_COUNTS[13], PARTICLE_OFFSETS[13], PARTICLE_IGNORE_DIST[13], PARTICLE_ARGS[13]);
            } else {
                spawnParticleRaw(id, entity, multiplier, PARTICLE_COUNTS[index], PARTICLE_OFFSETS[index], PARTICLE_IGNORE_DIST[index], PARTICLE_ARGS[index]);
            }
        }
    }

    private static void spawnParticleRaw(int id, Entity entity, int multiplier, int count, float offset, boolean ignoreDistance, int[] args) {
        for (int i = 0; i < count * multiplier; i++) {
            double xOffset = RANDOM.nextFloat() * (offset * 2.0F) - offset;
            double yOffset = RANDOM.nextFloat() * (offset * 2.0F) - offset;
            double zOffset = RANDOM.nextFloat() * (offset * 2.0F) - offset;
            if (ignoreDistance || xOffset * xOffset + yOffset * yOffset + zOffset * zOffset <= 1.0) {
                double x = entity.field_70165_t + xOffset * entity.field_70130_N / 4.0;
                double y = entity.func_174813_aQ().field_72338_b + entity.field_70131_O / 2.0F + yOffset * entity.field_70131_O / 4.0;
                double z = entity.field_70161_v + zOffset * entity.field_70130_N / 4.0;
                mc.field_71452_i.func_178927_a(id, x, y, z, xOffset, yOffset, zOffset, args);
            }
        }
    }

    private static EntityLivingBase getCollisionEntity(EntityArrow arrow) {
        World world = arrow.field_70170_p;
        Vec3 pos = new Vec3(arrow.field_70165_t, arrow.field_70163_u, arrow.field_70161_v);
        Vec3 motionEnd = new Vec3(arrow.field_70165_t + arrow.field_70159_w, arrow.field_70163_u + arrow.field_70181_x, arrow.field_70161_v + arrow.field_70179_y);
        MovingObjectPosition rayTrace = world.func_147447_a(pos, motionEnd, false, true, false);
        Vec3 traceEnd = motionEnd;
        if (rayTrace != null) {
            traceEnd = rayTrace.field_72307_f;
        }
        EntityLivingBase target = null;
        double closestSq = 0.0;
        AxisAlignedBB search = arrow.func_174813_aQ().func_72321_a(arrow.field_70159_w, arrow.field_70181_x, arrow.field_70179_y).func_72314_b(1.0, 1.0, 1.0);
        for (Entity entity : world.func_72839_b(arrow, search)) {
            if (entity instanceof EntityLivingBase) {
                EntityLivingBase living = (EntityLivingBase) entity;
                if (living.func_70067_L() && living != arrow.field_70250_c) {
                    AxisAlignedBB axisAlignedBB = entity.func_174813_aQ().func_72314_b(0.3, 0.3, 0.3);
                    MovingObjectPosition collision = axisAlignedBB.func_72327_a(pos, traceEnd);
                    if (collision != null) {
                        double distSq = pos.func_72436_e(collision.field_72307_f);
                        if (!(distSq >= closestSq) || closestSq == 0.0) {
                            target = living;
                            closestSq = distSq;
                        }
                    }
                }
            }
        }
        return target;
    }
}
