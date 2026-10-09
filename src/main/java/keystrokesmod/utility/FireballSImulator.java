package keystrokesmod.utility;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.projectile.EntityLargeFireball;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos.MutableBlockPos;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public final class FireballSImulator {

    private static final int DEFAULT_MAX_TICKS = 300;

    private static final int IGNORE_SHOOTER_TICKS = 25;

    private static final double ENTITY_HIT_EXPANSION = 0.3;

    private static final double SEARCH_EXPANSION = 1.0;

    private static final double COLLISION_EPSILON_SQ = 1.0E-7;

    private static final double WATER_FLOW_ACCELERATION = 0.014;

    private static final double WATER_CHECK_EXPAND_Y = -0.4F;

    private static final double WATER_CHECK_CONTRACT = 0.001;

    private static final float AIR_MOTION_FACTOR = 0.95F;

    private static final float WATER_MOTION_FACTOR = 0.8F;

    private FireballSImulator() {
    }

    public static FireballSImulator.Result simulate(EntityLargeFireball fireball) {
        return simulate(fireball, null, 300, false);
    }

    public static FireballSImulator.Result simulate(EntityLargeFireball fireball, Vec3 accelerationOverride) {
        return simulate(fireball, accelerationOverride, 300, false);
    }

    public static FireballSImulator.Result simulateCenterLine(EntityLargeFireball fireball, Vec3 accelerationOverride) {
        return simulate(fireball, accelerationOverride, 300, true);
    }

    public static FireballSImulator.Result simulate(EntityLargeFireball fireball, int maxTicks) {
        return simulate(fireball, null, maxTicks, false);
    }

    private static FireballSImulator.Result simulate(EntityLargeFireball fireball, Vec3 accelerationOverride, int maxTicks, boolean traceCenterLine) {
        if (fireball == null) {
            throw new IllegalArgumentException("fireball cannot be null");
        } else {
            World world = fireball.field_70170_p;
            if (world == null) {
                Vec3 pos = new Vec3(fireball.field_70165_t, fireball.field_70163_u, fireball.field_70161_v);
                return new FireballSImulator.Result(FireballSImulator.HitType.NONE, null, null, null, pos, 0);
            } else {
                double posX = fireball.field_70165_t;
                double posY = fireball.field_70163_u;
                double posZ = fireball.field_70161_v;
                double motionX = fireball.field_70159_w;
                double motionY = fireball.field_70181_x;
                double motionZ = fireball.field_70179_y;
                double accelerationX = accelerationOverride != null ? accelerationOverride.field_72450_a : fireball.field_70232_b;
                double accelerationY = accelerationOverride != null ? accelerationOverride.field_72448_b : fireball.field_70233_c;
                double accelerationZ = accelerationOverride != null ? accelerationOverride.field_72449_c : fireball.field_70230_d;
                float width = fireball.field_70130_N;
                float height = fireball.field_70131_O;
                double halfWidth = traceCenterLine ? 0.0 : width * 0.5;
                double collisionHeight = traceCenterLine ? 0.0 : height;
                double collisionOffsetY = traceCenterLine ? height * 0.5 : 0.0;
                EntityLivingBase shooter = fireball.field_70235_a;
                int ticksInAir = Math.max(0, fireball.field_70173_aa);
                int simulatedTicks = 0;
                Vec3 finalPosition = new Vec3(posX, posY, posZ);
                for (int tick = 0; tick < Math.max(1, maxTicks); tick++) {
                    simulatedTicks = tick + 1;
                    double[] motion = motionRef(motionX, motionY, motionZ);
                    FireballSImulator.WaterState waterState = sampleWaterState(world, posX, posY, posZ, width, height, fireball);
                    if (waterState.flowDirection.func_72433_c() > 0.0) {
                        motion[0] += waterState.flowDirection.field_72450_a * 0.014;
                        motion[1] += waterState.flowDirection.field_72448_b * 0.014;
                        motion[2] += waterState.flowDirection.field_72449_c * 0.014;
                    }
                    motionX = motion[0];
                    motionY = motion[1];
                    motionZ = motion[2];
                    Vec3 start = new Vec3(posX, posY + collisionOffsetY, posZ);
                    Vec3 end = new Vec3(posX + motionX, posY + motionY + collisionOffsetY, posZ + motionZ);
                    AxisAlignedBB sweepBounds = getSweepBounds(posX, posY, posZ, motionX, motionY, motionZ, width, height);
                    FireballSImulator.BlockCollisionResult blockCollision = getBlockCollision(world, start, end, sweepBounds, halfWidth, collisionHeight);
                    MovingObjectPosition blockHit = blockCollision.collision;
                    Vec3 searchEnd = blockCollision.impactPosition != null ? blockCollision.impactPosition : end;
                    double bestDistanceSq = blockCollision.distanceSq;
                    MovingObjectPosition bestEntityHit = null;
                    Vec3 bestEntityImpact = null;
                    AxisAlignedBB axisAlignedBB2 = sweepBounds.func_72314_b(1.0, 1.0, 1.0);
                    for (Entity candidate : world.func_72839_b(fireball, axisAlignedBB2)) {
                        if (candidate.func_70067_L() && (!candidate.func_70028_i(shooter) || ticksInAir >= 25)) {
                            AxisAlignedBB axisAlignedBB3 = expandTargetForProjectile(candidate.func_174813_aQ(), halfWidth, collisionHeight, 0.3);
                            MovingObjectPosition entityHit = axisAlignedBB3.func_72327_a(start, searchEnd);
                            if (entityHit == null && axisAlignedBB3.func_72318_a(start)) {
                                entityHit = new MovingObjectPosition(candidate, start);
                            }
                            if (entityHit != null) {
                                double distanceSq = start.func_72436_e(entityHit.field_72307_f);
                                if (distanceSq + 1.0E-7 < bestDistanceSq) {
                                    bestDistanceSq = distanceSq;
                                    bestEntityHit = new MovingObjectPosition(candidate, entityHit.field_72307_f);
                                    bestEntityHit.field_178784_b = entityHit.field_178784_b;
                                    bestEntityImpact = entityHit.field_72307_f.func_72441_c(0.0, -collisionOffsetY, 0.0);
                                }
                            }
                        }
                    }
                    if (bestEntityHit != null) {
                        return new FireballSImulator.Result(FireballSImulator.HitType.ENTITY, bestEntityHit, bestEntityHit.field_72307_f, bestEntityImpact, bestEntityImpact, simulatedTicks);
                    }
                    if (blockHit != null) {
                        Vec3 impactPosition = blockCollision.impactPosition.func_72441_c(0.0, -collisionOffsetY, 0.0);
                        return new FireballSImulator.Result(FireballSImulator.HitType.BLOCK, blockHit, blockHit.field_72307_f, impactPosition, impactPosition, simulatedTicks);
                    }
                    posX += motionX;
                    posY += motionY;
                    posZ += motionZ;
                    finalPosition = new Vec3(posX, posY, posZ);
                    motionX += accelerationX;
                    motionY += accelerationY;
                    motionZ += accelerationZ;
                    float motionFactor = waterState.inWater ? 0.8F : 0.95F;
                    motionX *= motionFactor;
                    motionY *= motionFactor;
                    motionZ *= motionFactor;
                    ticksInAir++;
                    if (posY < -64.0) {
                        break;
                    }
                }
                return new FireballSImulator.Result(FireballSImulator.HitType.NONE, null, null, finalPosition, finalPosition, simulatedTicks);
            }
        }
    }

    private static AxisAlignedBB getBoundingBox(double posX, double posY, double posZ, float width, float height) {
        double halfWidth = width * 0.5;
        return new AxisAlignedBB(posX - halfWidth, posY, posZ - halfWidth, posX + halfWidth, posY + height, posZ + halfWidth);
    }

    private static AxisAlignedBB getSweepBounds(double posX, double posY, double posZ, double motionX, double motionY, double motionZ, float width, float height) {
        AxisAlignedBB boundingBox = getBoundingBox(posX, posY, posZ, width, height);
        AxisAlignedBB axisAlignedBB4 = boundingBox.func_72317_d(motionX, motionY, motionZ);
        return new AxisAlignedBB(Math.min(boundingBox.field_72340_a, axisAlignedBB4.field_72340_a), Math.min(boundingBox.field_72338_b, axisAlignedBB4.field_72338_b), Math.min(boundingBox.field_72339_c, axisAlignedBB4.field_72339_c), Math.max(boundingBox.field_72336_d, axisAlignedBB4.field_72336_d), Math.max(boundingBox.field_72337_e, axisAlignedBB4.field_72337_e), Math.max(boundingBox.field_72334_f, axisAlignedBB4.field_72334_f));
    }

    private static AxisAlignedBB expandTargetForProjectile(AxisAlignedBB axisAlignedBB, double halfWidth, double projectileHeight, double extraExpansion) {
        return new AxisAlignedBB(axisAlignedBB.field_72340_a - halfWidth - extraExpansion, axisAlignedBB.field_72338_b - projectileHeight - extraExpansion, axisAlignedBB.field_72339_c - halfWidth - extraExpansion, axisAlignedBB.field_72336_d + halfWidth + extraExpansion, axisAlignedBB.field_72337_e + extraExpansion, axisAlignedBB.field_72334_f + halfWidth + extraExpansion);
    }

    private static FireballSImulator.BlockCollisionResult getBlockCollision(World world, Vec3 start, Vec3 end, AxisAlignedBB sweepBounds, double halfWidth, double projectileHeight) {
        int minX = MathHelper.func_76128_c(sweepBounds.field_72340_a);
        int maxX = MathHelper.func_76128_c(sweepBounds.field_72336_d + 1.0);
        int minY = MathHelper.func_76128_c(sweepBounds.field_72338_b);
        int maxY = MathHelper.func_76128_c(sweepBounds.field_72337_e + 1.0);
        int minZ = MathHelper.func_76128_c(sweepBounds.field_72339_c);
        int maxZ = MathHelper.func_76128_c(sweepBounds.field_72334_f + 1.0);
        if (!world.func_175706_a(new BlockPos(minX, minY, minZ), new BlockPos(maxX, maxY, maxZ), true)) {
            return new FireballSImulator.BlockCollisionResult(null, null, Double.MAX_VALUE);
        } else {
            List<AxisAlignedBB> collisionBoxes = new ArrayList<>();
            MutableBlockPos mutablePos = new MutableBlockPos();
            MovingObjectPosition bestCollision = null;
            Vec3 bestImpactPosition = null;
            double bestDistanceSq = Double.MAX_VALUE;
            for (int x = minX; x < maxX; x++) {
                for (int y = minY; y < maxY; y++) {
                    for (int z = minZ; z < maxZ; z++) {
                        mutablePos.func_181079_c(x, y, z);
                        IBlockState blockState = world.func_180495_p(mutablePos);
                        Block block = blockState.func_177230_c();
                        if (block.func_176209_a(blockState, false)) {
                            collisionBoxes.clear();
                            block.func_180638_a(world, mutablePos, blockState, sweepBounds, collisionBoxes, null);
                            for (AxisAlignedBB axisAlignedBB5 : collisionBoxes) {
                                AxisAlignedBB axisAlignedBB6 = expandTargetForProjectile(axisAlignedBB5, halfWidth, projectileHeight, 0.0);
                                MovingObjectPosition collision = axisAlignedBB6.func_72327_a(start, end);
                                if (collision == null && axisAlignedBB6.func_72318_a(start)) {
                                    collision = new MovingObjectPosition(start, EnumFacing.UP, new BlockPos(mutablePos));
                                }
                                if (collision != null) {
                                    double distanceSq = start.func_72436_e(collision.field_72307_f);
                                    if (distanceSq + 1.0E-7 < bestDistanceSq) {
                                        bestDistanceSq = distanceSq;
                                        bestImpactPosition = collision.field_72307_f;
                                        bestCollision = new MovingObjectPosition(collision.field_72307_f, collision.field_178784_b, new BlockPos(mutablePos));
                                    }
                                }
                            }
                        }
                    }
                }
            }
            return new FireballSImulator.BlockCollisionResult(bestCollision, bestImpactPosition, bestDistanceSq);
        }
    }

    private static FireballSImulator.WaterState sampleWaterState(World world, double posX, double posY, double posZ, float width, float height, Entity fireball) {
        AxisAlignedBB axisAlignedBB7 = getBoundingBox(posX, posY, posZ, width, height).func_72314_b(0.0, -0.4F, 0.0).func_72331_e(0.001, 0.001, 0.001);
        int minX = MathHelper.func_76128_c(axisAlignedBB7.field_72340_a);
        int maxX = MathHelper.func_76128_c(axisAlignedBB7.field_72336_d + 1.0);
        int minY = MathHelper.func_76128_c(axisAlignedBB7.field_72338_b);
        int maxY = MathHelper.func_76128_c(axisAlignedBB7.field_72337_e + 1.0);
        int minZ = MathHelper.func_76128_c(axisAlignedBB7.field_72339_c);
        int maxZ = MathHelper.func_76128_c(axisAlignedBB7.field_72334_f + 1.0);
        if (!world.func_175706_a(new BlockPos(minX, minY, minZ), new BlockPos(maxX, maxY, maxZ), true)) {
            return new FireballSImulator.WaterState(false, new Vec3(0.0, 0.0, 0.0));
        } else {
            boolean inWater = false;
            Vec3 flow = new Vec3(0.0, 0.0, 0.0);
            MutableBlockPos mutablePos = new MutableBlockPos();
            for (int x = minX; x < maxX; x++) {
                for (int y = minY; y < maxY; y++) {
                    for (int z = minZ; z < maxZ; z++) {
                        mutablePos.func_181079_c(x, y, z);
                        IBlockState blockState = world.func_180495_p(mutablePos);
                        if (blockState.func_177230_c().func_149688_o() == Material.field_151586_h) {
                            double liquidSurfaceY = y + 1 - BlockLiquid.func_149801_b((Integer) blockState.func_177229_b(BlockLiquid.field_176367_b));
                            if (!(maxY < liquidSurfaceY)) {
                                inWater = true;
                                flow = blockState.func_177230_c().func_176197_a(world, mutablePos, fireball, flow);
                            }
                        }
                    }
                }
            }
            if (flow.func_72433_c() > 0.0) {
                flow = flow.func_72432_b();
            }
            return new FireballSImulator.WaterState(inWater, flow);
        }
    }

    private static double[] motionRef(double motionX, double motionY, double motionZ) {
        return new double[] { motionX, motionY, motionZ };
    }

    private static final class BlockCollisionResult {

        private final MovingObjectPosition collision;

        private final Vec3 impactPosition;

        private final double distanceSq;

        private BlockCollisionResult(MovingObjectPosition collision, Vec3 impactPosition, double distanceSq) {
            this.collision = collision;
            this.impactPosition = impactPosition;
            this.distanceSq = distanceSq;
        }
    }

    public static enum HitType {

        NONE, BLOCK, ENTITY
    }

    public static final class Result {

        private final FireballSImulator.HitType hitType;

        private final MovingObjectPosition collision;

        private final Vec3 hitPosition;

        private final Vec3 impactPosition;

        private final Vec3 finalPosition;

        private final int simulatedTicks;

        private Result(FireballSImulator.HitType hitType, MovingObjectPosition collision, Vec3 hitPosition, Vec3 impactPosition, Vec3 finalPosition, int simulatedTicks) {
            this.hitType = hitType;
            this.collision = collision;
            this.hitPosition = hitPosition;
            this.impactPosition = impactPosition;
            this.finalPosition = finalPosition;
            this.simulatedTicks = simulatedTicks;
        }

        public FireballSImulator.HitType getHitType() {
            return this.hitType;
        }

        public boolean collided() {
            return this.hitType != FireballSImulator.HitType.NONE;
        }

        public boolean hitBlock() {
            return this.hitType == FireballSImulator.HitType.BLOCK;
        }

        public boolean hitEntity() {
            return this.hitType == FireballSImulator.HitType.ENTITY;
        }

        public MovingObjectPosition getCollision() {
            return this.collision;
        }

        public Entity getTarget() {
            return this.collision != null ? this.collision.field_72308_g : null;
        }

        public Entity getHitEntity() {
            return this.getTarget();
        }

        public BlockPos getHitBlockPos() {
            return this.collision != null ? this.collision.func_178782_a() : null;
        }

        public Vec3 getHitPosition() {
            return this.hitPosition;
        }

        public Vec3 getImpactPosition() {
            return this.impactPosition;
        }

        public Vec3 getFinalPosition() {
            return this.finalPosition;
        }

        public int getSimulatedTicks() {
            return this.simulatedTicks;
        }
    }

    private static final class WaterState {

        private final boolean inWater;

        private final Vec3 flowDirection;

        private WaterState(boolean inWater, Vec3 flowDirection) {
            this.inWater = inWater;
            this.flowDirection = flowDirection;
        }
    }
}
