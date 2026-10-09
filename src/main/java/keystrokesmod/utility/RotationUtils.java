package keystrokesmod.utility;

import com.google.common.base.Predicates;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import keystrokesmod.event.PreMotionEvent;
import keystrokesmod.helper.RotationHelper;
import keystrokesmod.module.impl.client.Settings;
import net.minecraft.block.Block;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EntitySelectors;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

public class RotationUtils implements IMinecraftInstance {

    public static float renderPitch;

    public static float prevRenderPitch;

    public static float renderYaw;

    public static float prevRenderYaw;

    public static float[] serverRotations = new float[] { 0.0F, 0.0F };

    public static Float[] fakeRotations;

    public static boolean setFakeRotations;

    private static final double BACKUP_FACE_INSET = 0.05;

    private static final int BACKUP_TARGET_TOTAL = 30;

    private static final float FAR_THRESHOLD = 180.0F;

    public static void setFakeRotations(float yaw, float pitch) {
        fakeRotations = new Float[] { yaw, pitch };
        setFakeRotations = true;
    }

    public static void setRenderYaw(float yaw) {
        mc.field_71439_g.field_70759_as = yaw;
        if (Settings.rotateBody.isToggled() && Settings.fullBody.isToggled()) {
            mc.field_71439_g.field_70760_ar = prevRenderYaw;
            mc.field_71439_g.field_70761_aq = yaw;
        }
    }

    public static float[] getRotations(BlockPos blockPos, float n, float n2) {
        float[] array = getRotations(blockPos);
        return fixRotation(array[0], array[1], n, n2);
    }

    public static float[] getRotationsToBlock(BlockPos blockPos, EnumFacing facing, float yaw, float pitch) {
        float[] array = getRotationsToBlock(blockPos, facing);
        return fixRotation(array[0], array[1], yaw, pitch);
    }

    public static float[] getRotations(BlockPos blockPos) {
        double x = blockPos.func_177958_n() + 0.45 - mc.field_71439_g.field_70165_t;
        double y = blockPos.func_177956_o() + 0.45 - (mc.field_71439_g.field_70163_u + mc.field_71439_g.func_70047_e());
        double z = blockPos.func_177952_p() + 0.45 - mc.field_71439_g.field_70161_v;
        float angleToBlock = (float) (Math.atan2(z, x) * (180.0 / Math.PI)) - 90.0F;
        float deltaYaw = MathHelper.func_76142_g(angleToBlock - mc.field_71439_g.field_70177_z);
        float yaw = mc.field_71439_g.field_70177_z + deltaYaw;
        double distance = MathHelper.func_76133_a(x * x + z * z);
        float angleToBlockPitch = (float) (-(Math.atan2(y, distance) * (180.0 / Math.PI)));
        float deltaPitch = MathHelper.func_76142_g(angleToBlockPitch - mc.field_71439_g.field_70125_A);
        float pitch = mc.field_71439_g.field_70125_A + deltaPitch;
        pitch = clampPitch(pitch);
        return new float[] { yaw, pitch };
    }

    public static float[] getRotations(double posX, double posY, double posZ) {
        double x = posX + 1.0 - mc.field_71439_g.field_70165_t;
        double y = posY + 1.0 - (mc.field_71439_g.field_70163_u + mc.field_71439_g.func_70047_e());
        double z = posZ + 1.0 - mc.field_71439_g.field_70161_v;
        float angleToBlock = (float) (Math.atan2(z, x) * (180.0 / Math.PI)) - 90.0F;
        float deltaYaw = MathHelper.func_76142_g(angleToBlock - mc.field_71439_g.field_70177_z);
        float yaw = mc.field_71439_g.field_70177_z + deltaYaw;
        double distance = MathHelper.func_76133_a(x * x + z * z);
        float angleToBlockPitch = (float) (-(Math.atan2(y, distance) * (180.0 / Math.PI)));
        float deltaPitch = MathHelper.func_76142_g(angleToBlockPitch - mc.field_71439_g.field_70125_A);
        float pitch = mc.field_71439_g.field_70125_A + deltaPitch;
        pitch = clampPitch(pitch);
        return new float[] { yaw, pitch };
    }

    public static float[] getRotations(Vec3 vec3) {
        double x = vec3.field_72450_a + 1.0 - mc.field_71439_g.field_70165_t;
        double y = vec3.field_72448_b + 1.0 - (mc.field_71439_g.field_70163_u + mc.field_71439_g.func_70047_e());
        double z = vec3.field_72449_c + 1.0 - mc.field_71439_g.field_70161_v;
        float angleToBlock = (float) (Math.atan2(z, x) * (180.0 / Math.PI)) - 90.0F;
        float deltaYaw = MathHelper.func_76142_g(angleToBlock - mc.field_71439_g.field_70177_z);
        float yaw = mc.field_71439_g.field_70177_z + deltaYaw;
        double distance = MathHelper.func_76133_a(x * x + z * z);
        float angleToBlockPitch = (float) (-(Math.atan2(y, distance) * (180.0 / Math.PI)));
        float deltaPitch = MathHelper.func_76142_g(angleToBlockPitch - mc.field_71439_g.field_70125_A);
        float pitch = mc.field_71439_g.field_70125_A + deltaPitch;
        pitch = clampPitch(pitch);
        return new float[] { yaw, pitch };
    }

    public static float[] getRotations(Entity entity, float yaw, float pitch) {
        float[] array = getRotations(entity);
        return array == null ? null : fixRotation(array[0], array[1], yaw, pitch);
    }

    public static float[] getRotationsToBlock(BlockPos pos, EnumFacing facing) {
        double diffX = pos.func_177958_n() + 0.45 - mc.field_71439_g.field_70165_t;
        double diffY = pos.func_177956_o() + 0.45 - (mc.field_71439_g.field_70163_u + mc.field_71439_g.func_70047_e());
        double diffZ = pos.func_177952_p() + 0.45 - mc.field_71439_g.field_70161_v;
        if (facing != null) {
            diffX += facing.func_176730_m().func_177958_n() * 0.5;
            diffY += facing.func_176730_m().func_177956_o() * 0.5;
            diffZ += facing.func_176730_m().func_177952_p() * 0.5;
        }
        double dist = MathHelper.func_76133_a(diffX * diffX + diffZ * diffZ);
        float yaw = (float) (Math.atan2(diffZ, diffX) * (float) (180.0 / Math.PI)) - 90.0F;
        float pitch = (float) (-(Math.atan2(diffY, dist) * (float) (180.0 / Math.PI)));
        return new float[] { mc.field_71439_g.field_70177_z + MathHelper.func_76142_g(yaw - mc.field_71439_g.field_70177_z), clampPitch(mc.field_71439_g.field_70125_A + MathHelper.func_76142_g(pitch - mc.field_71439_g.field_70125_A)) };
    }

    public static double distanceFromYaw(Entity entity, boolean b) {
        return Math.abs(MathHelper.func_76138_g(i(entity.field_70165_t, entity.field_70161_v) - (b && PreMotionEvent.setRenderYaw() ? renderYaw : mc.field_71439_g.field_70177_z)));
    }

    public static float i(double n, double n2) {
        return (float) (Math.atan2(n - mc.field_71439_g.field_70165_t, n2 - mc.field_71439_g.field_70161_v) * (float) (180.0 / Math.PI) * -1.0);
    }

    public static boolean isPossibleToHit(Entity target, double reach, float[] rotations) {
        Vec3 eyePosition = mc.field_71439_g.func_174824_e(1.0F);
        float yaw = rotations[0];
        float pitch = rotations[1];
        float radianYaw = -yaw * (float) (Math.PI / 180.0) - (float) Math.PI;
        float radianPitch = -pitch * (float) (Math.PI / 180.0);
        float cosYaw = MathHelper.func_76134_b(radianYaw);
        float sinYaw = MathHelper.func_76126_a(radianYaw);
        float cosPitch = -MathHelper.func_76134_b(radianPitch);
        float sinPitch = MathHelper.func_76126_a(radianPitch);
        Vec3 lookVector = new Vec3(sinYaw * cosPitch, sinPitch, cosYaw * cosPitch);
        double lookVecX = lookVector.field_72450_a * reach;
        double lookVecY = lookVector.field_72448_b * reach;
        double lookVecZ = lookVector.field_72449_c * reach;
        Vec3 endPosition = eyePosition.func_72441_c(lookVecX, lookVecY, lookVecZ);
        Entity renderViewEntity = mc.func_175606_aa();
        AxisAlignedBB axisAlignedBB = renderViewEntity.func_174813_aQ().func_72321_a(lookVecX, lookVecY, lookVecZ).func_72314_b(1.0, 1.0, 1.0);
        for (Entity entity : mc.field_71441_e.func_72839_b(renderViewEntity, axisAlignedBB)) {
            if (entity == target && entity.func_70067_L()) {
                float borderSize = entity.func_70111_Y();
                AxisAlignedBB axisAlignedBB2 = entity.func_174813_aQ().func_72314_b(borderSize, borderSize, borderSize);
                MovingObjectPosition intercept = axisAlignedBB2.func_72327_a(eyePosition, endPosition);
                return intercept != null;
            }
        }
        return false;
    }

    public static boolean inRange(BlockPos blockPos, double n) {
        float[] array = getRotations(blockPos);
        Vec3 getPositionEyes = mc.field_71439_g.func_174824_e(1.0F);
        float n2 = -array[0] * (float) (Math.PI / 180.0);
        float n3 = -array[1] * (float) (Math.PI / 180.0);
        float cos = MathHelper.func_76134_b(n2 - (float) Math.PI);
        float sin = MathHelper.func_76126_a(n2 - (float) Math.PI);
        float n4 = -MathHelper.func_76134_b(n3);
        Vec3 vec3 = new Vec3(sin * n4, MathHelper.func_76126_a(n3), cos * n4);
        Block block = BlockUtils.getBlock(blockPos);
        IBlockState blockState = BlockUtils.getBlockState(blockPos);
        if (block != null && blockState != null) {
            AxisAlignedBB axisAlignedBB3 = block.func_180640_a(mc.field_71441_e, blockPos, blockState);
            if (axisAlignedBB3 != null) {
                Vec3 targetVec = getPositionEyes.func_72441_c(vec3.field_72450_a * n, vec3.field_72448_b * n, vec3.field_72449_c * n);
                MovingObjectPosition intercept = axisAlignedBB3.func_72327_a(getPositionEyes, targetVec);
                if (intercept != null) {
                    return true;
                }
            }
        }
        return false;
    }

    public static float[] getRotations(Entity entity) {
        return getRotations(entity, RotationUtils.PLAYER_OFFSETS.NONE);
    }

    public static float[] getRotations(Entity entity, RotationUtils.PLAYER_OFFSETS playerOffset) {
        if (entity == null) {
            return null;
        } else {
            double deltaX = entity.field_70165_t - mc.field_71439_g.field_70165_t;
            double deltaZ = entity.field_70161_v - mc.field_71439_g.field_70161_v;
            double deltaY;
            if (entity instanceof EntityLivingBase) {
                EntityLivingBase entityLivingBase = (EntityLivingBase) entity;
                deltaY = entityLivingBase.field_70163_u + playerOffset.getHeightOffset(entityLivingBase) * 0.9 - (mc.field_71439_g.field_70163_u + mc.field_71439_g.func_70047_e());
            } else {
                deltaY = (entity.func_174813_aQ().field_72338_b + entity.func_174813_aQ().field_72337_e) / 2.0 - (mc.field_71439_g.field_70163_u + mc.field_71439_g.func_70047_e());
            }
            return new float[] { mc.field_71439_g.field_70177_z + MathHelper.func_76142_g((float) (Math.atan2(deltaZ, deltaX) * (float) (180.0 / Math.PI)) - 90.0F - mc.field_71439_g.field_70177_z), clampPitch(mc.field_71439_g.field_70125_A + MathHelper.func_76142_g((float) (-(Math.atan2(deltaY, MathHelper.func_76133_a(deltaX * deltaX + deltaZ * deltaZ)) * (float) (180.0 / Math.PI))) - mc.field_71439_g.field_70125_A) + 3.0F) };
        }
    }

    public static float[] getRotationsToPoint(double x, double y, double z) {
        return getRotationsToPoint(x, y, z, mc.field_71439_g.field_70177_z, mc.field_71439_g.field_70125_A);
    }

    public static float[] getRotationsToPoint(double x, double y, double z, float baseYaw, float basePitch) {
        double deltaX = x - mc.field_71439_g.field_70165_t;
        double deltaZ = z - mc.field_71439_g.field_70161_v;
        double deltaY = y - (mc.field_71439_g.field_70163_u + mc.field_71439_g.func_70047_e());
        double horizDistSq = deltaX * deltaX + deltaZ * deltaZ;
        float yaw;
        float targetPitch;
        if (horizDistSq < 1.0E-12) {
            yaw = baseYaw;
            targetPitch = (float) (-(Math.atan2(deltaY, 0.0) * (float) (180.0 / Math.PI)));
        } else {
            float targetYaw = (float) (Math.atan2(deltaZ, deltaX) * (float) (180.0 / Math.PI)) - 90.0F;
            yaw = baseYaw + MathHelper.func_76142_g(targetYaw - baseYaw);
            double horizDist = MathHelper.func_76133_a(horizDistSq);
            targetPitch = (float) (-(Math.atan2(deltaY, horizDist) * (float) (180.0 / Math.PI)));
        }
        float pitch = basePitch + MathHelper.func_76142_g(targetPitch - basePitch) + 3.0F;
        return new float[] { yaw, clampPitch(pitch) };
    }

    public static float[] getRotations(Entity entity, double horizontalMultipoint, double verticalMultipoint, float baseYaw, float basePitch) {
        Vec3 aimPoint = getAimPoint(entity, horizontalMultipoint, verticalMultipoint);
        return aimPoint == null ? null : getRotationsToPoint(aimPoint.field_72450_a, aimPoint.field_72448_b, aimPoint.field_72449_c, baseYaw, basePitch);
    }

    public static Vec3 getAimPoint(Entity entity, double horizontalMultipoint, double verticalMultipoint) {
        if (entity != null && mc.field_71439_g != null) {
            float borderSize = entity.func_70111_Y();
            AxisAlignedBB bb = entity.func_174813_aQ().func_72314_b(borderSize, borderSize, borderSize);
            double centerX = (bb.field_72340_a + bb.field_72336_d) / 2.0;
            double centerY;
            if (entity instanceof EntityLivingBase) {
                centerY = entity.field_70163_u + ((EntityLivingBase) entity).func_70047_e();
            } else {
                centerY = (bb.field_72338_b + bb.field_72337_e) / 2.0;
            }
            double centerZ = (bb.field_72339_c + bb.field_72334_f) / 2.0;
            Vec3 eye = mc.field_71439_g.func_174824_e(1.0F);
            if (bb.func_72318_a(eye)) {
                return new Vec3(centerX, eye.field_72448_b, centerZ);
            } else {
                Vec3 cl = closestPointOnAabb(bb, eye);
                double tH = Math.max(0.0, Math.min(1.0, horizontalMultipoint / 100.0));
                double tV = Math.max(0.0, Math.min(1.0, verticalMultipoint / 100.0));
                double targetX = centerX + (cl.field_72450_a - centerX) * tH;
                double targetY = centerY + (cl.field_72448_b - centerY) * tV;
                double targetZ = centerZ + (cl.field_72449_c - centerZ) * tH;
                return new Vec3(targetX, targetY, targetZ);
            }
        } else {
            return null;
        }
    }

    public static Vec3 closestPointOnAabb(AxisAlignedBB box, Vec3 point) {
        double x = Math.max(box.field_72340_a, Math.min(box.field_72336_d, point.field_72450_a));
        double y = Math.max(box.field_72338_b, Math.min(box.field_72337_e, point.field_72448_b));
        double z = Math.max(box.field_72339_c, Math.min(box.field_72334_f, point.field_72449_c));
        return new Vec3(x, y, z);
    }

    public static List<Vec3> buildBackupPoints(Entity entity, Vec3 eye) {
        if (entity != null && mc.field_71439_g != null) {
            float borderSize = entity.func_70111_Y();
            AxisAlignedBB bb = entity.func_174813_aQ().func_72314_b(borderSize, borderSize, borderSize);
            double sizeX = bb.field_72336_d - bb.field_72340_a;
            double sizeY = bb.field_72337_e - bb.field_72338_b;
            double sizeZ = bb.field_72334_f - bb.field_72339_c;
            boolean xPos = eye.field_72450_a > bb.field_72336_d;
            boolean xNeg = eye.field_72450_a < bb.field_72340_a;
            boolean yPos = eye.field_72448_b > bb.field_72337_e;
            boolean yNeg = eye.field_72448_b < bb.field_72338_b;
            boolean zPos = eye.field_72449_c > bb.field_72334_f;
            boolean zNeg = eye.field_72449_c < bb.field_72339_c;
            int visibleFaceCount = (!xPos && !xNeg ? 0 : 1) + (!yPos && !yNeg ? 0 : 1) + (!zPos && !zNeg ? 0 : 1);
            if (visibleFaceCount == 0) {
                return new ArrayList<>();
            } else {
                int pointsPerFace = 30 / visibleFaceCount;
                List<Vec3> points = new ArrayList<>(36);
                if (xPos || xNeg) {
                    double fixedX = xPos ? bb.field_72336_d - 0.05 : bb.field_72340_a + 0.05;
                    addFaceGrid(points, 0, fixedX, bb.field_72338_b + 0.05, bb.field_72337_e - 0.05, bb.field_72339_c + 0.05, bb.field_72334_f - 0.05, pointsPerFace, sizeY, sizeZ);
                }
                if (yPos || yNeg) {
                    double fixedY = yPos ? bb.field_72337_e - 0.05 : bb.field_72338_b + 0.05;
                    addFaceGrid(points, 1, fixedY, bb.field_72340_a + 0.05, bb.field_72336_d - 0.05, bb.field_72339_c + 0.05, bb.field_72334_f - 0.05, pointsPerFace, sizeX, sizeZ);
                }
                if (zPos || zNeg) {
                    double fixedZ = zPos ? bb.field_72334_f - 0.05 : bb.field_72339_c + 0.05;
                    addFaceGrid(points, 2, fixedZ, bb.field_72340_a + 0.05, bb.field_72336_d - 0.05, bb.field_72338_b + 0.05, bb.field_72337_e - 0.05, pointsPerFace, sizeX, sizeY);
                }
                return points;
            }
        } else {
            return new ArrayList<>();
        }
    }

    private static void addFaceGrid(List<Vec3> out, int fixedAxis, double fixedVal, double uMin, double doubleValue, double vMin, double doubleValue2, int targetPoints, double dimU, double dimV) {
        if (!(dimU < 1.0E-4) && !(dimV < 1.0E-4)) {
            double ratio = dimU / dimV;
            int gridU = Math.max(2, (int) Math.round(Math.sqrt(targetPoints * ratio)));
            int gridV = Math.max(2, (int) Math.round(Math.sqrt(targetPoints / ratio)));
            for (int i = 0; i < gridU; i++) {
                double u = uMin + (doubleValue - uMin) * i / (gridU - 1);
                for (int j = 0; j < gridV; j++) {
                    double v = vMin + (doubleValue2 - vMin) * j / (gridV - 1);
                    switch(fixedAxis) {
                        case 0:
                            out.add(new Vec3(fixedVal, u, v));
                            break;
                        case 1:
                            out.add(new Vec3(u, fixedVal, v));
                            break;
                        case 2:
                            out.add(new Vec3(u, v, fixedVal));
                    }
                }
            }
        } else {
            double uMid = (uMin + doubleValue) / 2.0;
            double vMid = (vMin + doubleValue2) / 2.0;
            switch(fixedAxis) {
                case 0:
                    out.add(new Vec3(fixedVal, uMid, vMid));
                    break;
                case 1:
                    out.add(new Vec3(uMid, fixedVal, vMid));
                    break;
                case 2:
                    out.add(new Vec3(uMid, vMid, fixedVal));
            }
        }
    }

    public static double distanceSqFromEyeToClosestOnAABB(Entity entity) {
        if (entity != null && mc.field_71439_g != null) {
            Vec3 eye = mc.field_71439_g.func_174824_e(1.0F);
            float borderSize = entity.func_70111_Y();
            AxisAlignedBB bb = entity.func_174813_aQ().func_72314_b(borderSize, borderSize, borderSize);
            Vec3 closest = closestPointOnAabb(bb, eye);
            double dx = eye.field_72450_a - closest.field_72450_a;
            double dy = eye.field_72448_b - closest.field_72448_b;
            double dz = eye.field_72449_c - closest.field_72449_c;
            return dx * dx + dy * dy + dz * dz;
        } else {
            return Double.MAX_VALUE;
        }
    }

    public static double distanceFromEyeToClosestOnAABB(Entity entity) {
        double dSq = distanceSqFromEyeToClosestOnAABB(entity);
        return dSq == Double.MAX_VALUE ? Double.MAX_VALUE : Math.sqrt(dSq);
    }

    public static double distanceFromEyeToClosestOnAABB(Entity entity, Vec3 position) {
        double dSq = distanceSqFromEyeToClosestOnAABB(entity, position);
        return dSq == Double.MAX_VALUE ? Double.MAX_VALUE : Math.sqrt(dSq);
    }

    public static double distanceSqFromEyeToClosestOnAABB(Entity entity, Vec3 position) {
        if (entity != null && position != null && mc.field_71439_g != null) {
            Vec3 eye = mc.field_71439_g.func_174824_e(1.0F);
            float borderSize = entity.func_70111_Y();
            double offsetX = position.field_72450_a - entity.field_70165_t;
            double offsetY = position.field_72448_b - entity.field_70163_u;
            double offsetZ = position.field_72449_c - entity.field_70161_v;
            AxisAlignedBB bb = entity.func_174813_aQ().func_72317_d(offsetX, offsetY, offsetZ).func_72314_b(borderSize, borderSize, borderSize);
            Vec3 closest = closestPointOnAabb(bb, eye);
            double dx = eye.field_72450_a - closest.field_72450_a;
            double dy = eye.field_72448_b - closest.field_72448_b;
            double dz = eye.field_72449_c - closest.field_72449_c;
            return dx * dx + dy * dy + dz * dz;
        } else {
            return Double.MAX_VALUE;
        }
    }

    public static boolean canAimAtPoint(Vec3 eye, Vec3 point, Entity target, double range) {
        return canAimAtPoint(eye, point, target, range, false, true);
    }

    public static boolean canAimAtPoint(Vec3 eye, Vec3 point, Entity target, double range, boolean allowThroughBlocks, boolean allowThroughEntities) {
        return canAimAtPoint(eye, point, target, range, allowThroughBlocks, allowThroughEntities, false);
    }

    public static boolean canAimAtPoint(Vec3 eye, Vec3 point, Entity target, double range, boolean allowThroughBlocks, boolean allowThroughEntities, boolean ignoreOpenFenceGates) {
        if (target == null) {
            return false;
        } else {
            double dx = point.field_72450_a - eye.field_72450_a;
            double dy = point.field_72448_b - eye.field_72448_b;
            double dz = point.field_72449_c - eye.field_72449_c;
            double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (len < 1.0E-6) {
                return false;
            } else {
                double scale = range / len;
                Vec3 end = new Vec3(eye.field_72450_a + dx * scale, eye.field_72448_b + dy * scale, eye.field_72449_c + dz * scale);
                float borderSize = target.func_70111_Y();
                AxisAlignedBB aabb = target.func_174813_aQ().func_72314_b(borderSize, borderSize, borderSize);
                MovingObjectPosition entityHit = aabb.func_72327_a(eye, end);
                if (entityHit == null) {
                    return false;
                } else {
                    double entityDistSq = eye.func_72436_e(entityHit.field_72307_f);
                    if (!allowThroughBlocks) {
                        MovingObjectPosition blockHit = ignoreOpenFenceGates ? rayTraceBlocksIgnoringOpenFenceGates(eye, end, false, false, false) : mc.field_71441_e.func_147447_a(eye, end, false, false, false);
                        if (blockHit != null && blockHit.field_72313_a == MovingObjectType.BLOCK) {
                            double blockDistSq = eye.func_72436_e(blockHit.field_72307_f);
                            if (blockDistSq < entityDistSq) {
                                return false;
                            }
                        }
                    }
                    return allowThroughEntities || !hasEntityBlockingPath(eye, end, target, entityDistSq);
                }
            }
        }
    }

    public static MovingObjectPosition rayTraceBlocksIgnoringOpenFenceGates(Vec3 start, Vec3 end, boolean stopOnLiquid, boolean enabled, boolean returnLastUncollidableBlock) {
        if (mc.field_71441_e != null && start != null && end != null) {
            Vec3 traceStart = start;
            for (int ignoredGates = 0; ignoredGates < 200; ignoredGates++) {
                MovingObjectPosition hit = mc.field_71441_e.func_147447_a(traceStart, end, stopOnLiquid, enabled, returnLastUncollidableBlock);
                if (!isOpenFenceGateHit(hit)) {
                    return hit;
                }
                traceStart = movePastBlock(hit.field_72307_f, end, hit.func_178782_a());
                if (traceStart == null) {
                    return null;
                }
            }
            return null;
        } else {
            return null;
        }
    }

    private static boolean isOpenFenceGateHit(MovingObjectPosition hit) {
        if (hit != null && hit.field_72313_a == MovingObjectType.BLOCK && hit.func_178782_a() != null) {
            IBlockState state = mc.field_71441_e.func_180495_p(hit.func_178782_a());
            return state.func_177230_c() instanceof BlockFenceGate && (Boolean) state.func_177229_b(BlockFenceGate.field_176466_a);
        } else {
            return false;
        }
    }

    private static Vec3 movePastBlock(Vec3 hitVec, Vec3 end, BlockPos blockPos) {
        if (hitVec != null && end != null && blockPos != null) {
            double dx = end.field_72450_a - hitVec.field_72450_a;
            double dy = end.field_72448_b - hitVec.field_72448_b;
            double dz = end.field_72449_c - hitVec.field_72449_c;
            double remainingDistance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (remainingDistance < 1.0E-7) {
                return null;
            } else {
                dx /= remainingDistance;
                dy /= remainingDistance;
                dz /= remainingDistance;
                double distanceToExit = Double.POSITIVE_INFINITY;
                if (dx > 1.0E-7) {
                    distanceToExit = Math.min(distanceToExit, (blockPos.func_177958_n() + 1.0 - hitVec.field_72450_a) / dx);
                } else if (dx < -1.0E-7) {
                    distanceToExit = Math.min(distanceToExit, (blockPos.func_177958_n() - hitVec.field_72450_a) / dx);
                }
                if (dy > 1.0E-7) {
                    distanceToExit = Math.min(distanceToExit, (blockPos.func_177956_o() + 1.0 - hitVec.field_72448_b) / dy);
                } else if (dy < -1.0E-7) {
                    distanceToExit = Math.min(distanceToExit, (blockPos.func_177956_o() - hitVec.field_72448_b) / dy);
                }
                if (dz > 1.0E-7) {
                    distanceToExit = Math.min(distanceToExit, (blockPos.func_177952_p() + 1.0 - hitVec.field_72449_c) / dz);
                } else if (dz < -1.0E-7) {
                    distanceToExit = Math.min(distanceToExit, (blockPos.func_177952_p() - hitVec.field_72449_c) / dz);
                }
                double advance = Math.max(0.0, distanceToExit) + 1.0E-5;
                return !Double.isInfinite(distanceToExit) && !Double.isNaN(distanceToExit) && !(advance >= remainingDistance) ? hitVec.func_72441_c(dx * advance, dy * advance, dz * advance) : null;
            }
        } else {
            return null;
        }
    }

    private static boolean hasEntityBlockingPath(Vec3 eye, Vec3 end, Entity target, double targetDistSq) {
        if (mc.field_71439_g != null && mc.field_71441_e != null) {
            Vec3 delta = end.func_178788_d(eye);
            AxisAlignedBB axisAlignedBB4 = mc.field_71439_g.func_174813_aQ().func_72321_a(delta.field_72450_a, delta.field_72448_b, delta.field_72449_c).func_72314_b(1.0, 1.0, 1.0);
            for (Entity entity : mc.field_71441_e.func_175674_a(mc.field_71439_g, axisAlignedBB4, Predicates.and(EntitySelectors.field_180132_d, Entity::func_70067_L))) {
                if (entity != null && entity != target && !entity.field_70128_L) {
                    float border = entity.func_70111_Y();
                    AxisAlignedBB bb = entity.func_174813_aQ().func_72314_b(border, border, border);
                    MovingObjectPosition hit = bb.func_72327_a(eye, end);
                    if (bb.func_72318_a(eye)) {
                        return true;
                    }
                    if (hit != null) {
                        double entityDistSq = eye.func_72436_e(hit.field_72307_f);
                        if (entityDistSq < targetDistSq - 1.0E-7) {
                            return true;
                        }
                    }
                }
            }
            return false;
        } else {
            return false;
        }
    }

    public static boolean isPathBlockedByEntity(Vec3 eye, Vec3 hitVec, Entity target) {
        if (eye != null && hitVec != null && target != null) {
            double targetDistSq = eye.func_72436_e(hitVec);
            return hasEntityBlockingPath(eye, hitVec, target, targetDistSq);
        } else {
            return false;
        }
    }

    public static boolean isEyeInsideEntityAABB(Entity entity) {
        if (entity != null && mc.field_71439_g != null) {
            Vec3 eye = mc.field_71439_g.func_174824_e(1.0F);
            float borderSize = entity.func_70111_Y();
            AxisAlignedBB bb = entity.func_174813_aQ().func_72314_b(borderSize, borderSize, borderSize);
            return bb.func_72318_a(eye);
        } else {
            return false;
        }
    }

    private static boolean mainRayHitsTargetAABB(Vec3 eye, Vec3 point, Entity target, double range) {
        double dx = point.field_72450_a - eye.field_72450_a;
        double dy = point.field_72448_b - eye.field_72448_b;
        double dz = point.field_72449_c - eye.field_72449_c;
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1.0E-6) {
            return false;
        } else {
            double scale = range / len;
            Vec3 end = new Vec3(eye.field_72450_a + dx * scale, eye.field_72448_b + dy * scale, eye.field_72449_c + dz * scale);
            float borderSize = target.func_70111_Y();
            AxisAlignedBB aabb = target.func_174813_aQ().func_72314_b(borderSize, borderSize, borderSize);
            return aabb.func_72327_a(eye, end) != null;
        }
    }

    public static boolean hasValidAimPoint(Entity entity, double hMult, double vMult, double range) {
        return hasValidAimPoint(entity, hMult, vMult, range, false, true);
    }

    public static boolean hasValidAimPoint(Entity entity, double hMult, double vMult, double range, boolean allowThroughBlocks, boolean allowThroughEntities) {
        return hasValidAimPoint(entity, hMult, vMult, range, allowThroughBlocks, allowThroughEntities, false);
    }

    public static boolean hasValidAimPoint(Entity entity, double hMult, double vMult, double range, boolean allowThroughBlocks, boolean allowThroughEntities, boolean ignoreOpenFenceGates) {
        if (entity != null && mc.field_71439_g != null) {
            Vec3 mainPoint = getAimPoint(entity, hMult, vMult);
            if (mainPoint == null) {
                return false;
            } else {
                Vec3 eye = mc.field_71439_g.func_174824_e(1.0F);
                if (eye.func_72436_e(mainPoint) < 1.0E-6) {
                    return true;
                } else if (!mainRayHitsTargetAABB(eye, mainPoint, entity, range)) {
                    return false;
                } else if (canAimAtPoint(eye, mainPoint, entity, range, allowThroughBlocks, allowThroughEntities, ignoreOpenFenceGates)) {
                    return true;
                } else {
                    List<Vec3> backups = buildBackupPoints(entity, eye);
                    Collections.sort(backups, Comparator.comparingDouble(px -> {
                        double dx = px.field_72450_a - eye.field_72450_a;
                        double dy = px.field_72448_b - eye.field_72448_b;
                        double dz = px.field_72449_c - eye.field_72449_c;
                        return dx * dx + dy * dy + dz * dz;
                    }));
                    for (Vec3 p : backups) {
                        if (canAimAtPoint(eye, p, entity, range, allowThroughBlocks, allowThroughEntities, ignoreOpenFenceGates)) {
                            return true;
                        }
                    }
                    return false;
                }
            }
        } else {
            return false;
        }
    }

    public static float[] getRotationsWithBackup(Entity entity, double horizontalMultipoint, double verticalMultipoint, float baseYaw, float basePitch, double range) {
        return getRotationsWithBackup(entity, horizontalMultipoint, verticalMultipoint, baseYaw, basePitch, range, false, true);
    }

    public static float[] getRotationsWithBackup(Entity entity, double horizontalMultipoint, double verticalMultipoint, float baseYaw, float basePitch, double range, boolean allowThroughBlocks, boolean allowThroughEntities) {
        return getRotationsWithBackup(entity, horizontalMultipoint, verticalMultipoint, baseYaw, basePitch, range, allowThroughBlocks, allowThroughEntities, false);
    }

    public static float[] getRotationsWithBackup(Entity entity, double horizontalMultipoint, double verticalMultipoint, float baseYaw, float basePitch, double range, boolean allowThroughBlocks, boolean allowThroughEntities, boolean ignoreOpenFenceGates) {
        if (entity != null && mc.field_71439_g != null) {
            Vec3 eye = mc.field_71439_g.func_174824_e(1.0F);
            float borderSize = entity.func_70111_Y();
            AxisAlignedBB bb = entity.func_174813_aQ().func_72314_b(borderSize, borderSize, borderSize);
            if (bb.func_72318_a(eye)) {
                double centerX = (bb.field_72340_a + bb.field_72336_d) / 2.0;
                double centerZ = (bb.field_72339_c + bb.field_72334_f) / 2.0;
                return getRotationsToPoint(centerX, eye.field_72448_b, centerZ, baseYaw, basePitch);
            } else {
                Vec3 mainPoint = getAimPoint(entity, horizontalMultipoint, verticalMultipoint);
                if (mainPoint == null) {
                    return null;
                } else if (eye.func_72436_e(mainPoint) < 1.0E-6) {
                    return null;
                } else if (!mainRayHitsTargetAABB(eye, mainPoint, entity, range)) {
                    return getRotationsToPoint(mainPoint.field_72450_a, mainPoint.field_72448_b, mainPoint.field_72449_c, baseYaw, basePitch);
                } else if (canAimAtPoint(eye, mainPoint, entity, range, allowThroughBlocks, allowThroughEntities, ignoreOpenFenceGates)) {
                    return getRotationsToPoint(mainPoint.field_72450_a, mainPoint.field_72448_b, mainPoint.field_72449_c, baseYaw, basePitch);
                } else {
                    List<Vec3> backups = buildBackupPoints(entity, eye);
                    Collections.sort(backups, Comparator.comparingDouble(px -> {
                        double dx = px.field_72450_a - eye.field_72450_a;
                        double dy = px.field_72448_b - eye.field_72448_b;
                        double dz = px.field_72449_c - eye.field_72449_c;
                        return dx * dx + dy * dy + dz * dz;
                    }));
                    for (Vec3 p : backups) {
                        if (canAimAtPoint(eye, p, entity, range, allowThroughBlocks, allowThroughEntities, ignoreOpenFenceGates)) {
                            return getRotationsToPoint(p.field_72450_a, p.field_72448_b, p.field_72449_c, baseYaw, basePitch);
                        }
                    }
                    return null;
                }
            }
        } else {
            return null;
        }
    }

    public static float[] getRotationsPredicated(Entity entity, int ticks) {
        if (entity == null) {
            return null;
        } else if (ticks == 0) {
            return getRotations(entity);
        } else {
            double posX = entity.field_70165_t;
            double posY = entity.field_70163_u;
            double posZ = entity.field_70161_v;
            double n2 = posX - entity.field_70142_S;
            double n3 = posZ - entity.field_70136_U;
            for (int i = 0; i < ticks; i++) {
                posX += n2;
                posZ += n3;
            }
            double n4 = posX - mc.field_71439_g.field_70165_t;
            double n5;
            if (entity instanceof EntityLivingBase) {
                n5 = posY + entity.func_70047_e() * 0.9 - (mc.field_71439_g.field_70163_u + mc.field_71439_g.func_70047_e());
            } else {
                n5 = (entity.func_174813_aQ().field_72338_b + entity.func_174813_aQ().field_72337_e) / 2.0 - (mc.field_71439_g.field_70163_u + mc.field_71439_g.func_70047_e());
            }
            double n6 = posZ - mc.field_71439_g.field_70161_v;
            return new float[] { applyVanilla(mc.field_71439_g.field_70177_z + MathHelper.func_76142_g((float) (Math.atan2(n6, n4) * (float) (180.0 / Math.PI)) - 90.0F - mc.field_71439_g.field_70177_z)), clampPitch(mc.field_71439_g.field_70125_A + MathHelper.func_76142_g((float) (-(Math.atan2(n5, MathHelper.func_76133_a(n4 * n4 + n6 * n6)) * (float) (180.0 / Math.PI))) - mc.field_71439_g.field_70125_A) + 3.0F) };
        }
    }

    public static float[] smoothRotation(float baseYaw, float basePitch, float targetYaw, float targetPitch, int speed) {
        return smoothRotation(baseYaw, basePitch, targetYaw, targetPitch, speed, 0.0F);
    }

    public static float[] smoothRotation(float baseYaw, float basePitch, float targetYaw, float targetPitch, int speed, float randomizationPercent) {
        if (speed <= 0) {
            return new float[] { baseYaw, clampPitch(basePitch) };
        } else if (speed >= 30) {
            return new float[] { targetYaw, clampPitch(targetPitch) };
        } else {
            float deltaYaw = MathHelper.func_76142_g(targetYaw - baseYaw);
            float deltaPitch = targetPitch - basePitch;
            float magnitude = MathHelper.func_76133_a(deltaYaw * deltaYaw + deltaPitch * deltaPitch);
            if (magnitude < 0.001F) {
                return new float[] { targetYaw, clampPitch(targetPitch) };
            } else {
                float t = speed / 30.0F;
                float stepSize = t * t * 180.0F;
                float range = 0.6F * (float) (randomizationPercent / 100.0);
                float multiplier = range <= 0.001F ? 1.0F : 1.0F - range / 2.0F + (float) (Math.random() * range);
                stepSize *= multiplier;
                float proximityFactor = Math.min(1.0F, magnitude / 180.0F);
                proximityFactor = (float) Math.pow(proximityFactor, 0.7);
                float maxSlowdown = (float) (randomizationPercent / 100.0);
                float proximityMult = Math.max(0.8F, 1.0F - maxSlowdown * (1.0F - proximityFactor));
                stepSize *= proximityMult;
                float stepLength = Math.min(stepSize, magnitude);
                float scale = stepLength / magnitude;
                float stepYaw = deltaYaw * scale;
                float stepPitch = deltaPitch * scale;
                float yaw = baseYaw + stepYaw;
                float pitch = basePitch + stepPitch;
                return new float[] { yaw, clampPitch(pitch) };
            }
        }
    }

    public static float clampPitch(float n) {
        return MathHelper.func_76131_a(n, -90.0F, 90.0F);
    }

    public static float[] fixRotation(float targetYaw, float targetPitch, float yaw, float pitch) {
        targetYaw = RotationHelper.unwrapYaw(targetYaw, yaw);
        float n5 = targetYaw - yaw;
        float abs = Math.abs(n5);
        float n7 = targetPitch - pitch;
        float n8 = mc.field_71474_y.field_74341_c * 0.6F + 0.2F;
        double n9 = n8 * n8 * n8 * 1.2;
        float n10 = (float) (Math.round(n5 / n9) * n9);
        float n11 = (float) (Math.round(n7 / n9) * n9);
        targetYaw = yaw + n10;
        targetPitch = pitch + n11;
        return new float[] { targetYaw, clampPitch(targetPitch) };
    }

    public static float angle(double n, double n2) {
        return (float) (Math.atan2(n - mc.field_71439_g.field_70165_t, n2 - mc.field_71439_g.field_70161_v) * (float) (180.0 / Math.PI) * -1.0);
    }

    public static float deltaAngle(double n, double n2) {
        return (float) (Math.atan2(n, n2) * (float) (180.0 / Math.PI) * -1.0);
    }

    public static MovingObjectPosition rayCast(double distance, float yaw, float pitch, boolean collisionCheck) {
        Vec3 getPositionEyes = mc.field_71439_g.func_174824_e(1.0F);
        float n4 = -yaw * (float) (Math.PI / 180.0);
        float n5 = -pitch * (float) (Math.PI / 180.0);
        float cos = MathHelper.func_76134_b(n4 - (float) Math.PI);
        float sin = MathHelper.func_76126_a(n4 - (float) Math.PI);
        float n6 = -MathHelper.func_76134_b(n5);
        Vec3 vec3 = new Vec3(sin * n6, MathHelper.func_76126_a(n5), cos * n6);
        return mc.field_71441_e.func_147447_a(getPositionEyes, getPositionEyes.func_72441_c(vec3.field_72450_a * distance, vec3.field_72448_b * distance, vec3.field_72449_c * distance), true, collisionCheck, true);
    }

    public static MovingObjectPosition rayCastBlock(double distance, float yaw, float pitch) {
        Vec3 eyeVec = mc.field_71439_g.func_174824_e(1.0F);
        Vec3 lookVec = Utils.getLookVec(yaw, pitch);
        Vec3 sumVec = eyeVec.func_72441_c(lookVec.field_72450_a * distance, lookVec.field_72448_b * distance, lookVec.field_72449_c * distance);
        MovingObjectPosition mop = mc.field_71441_e.func_147447_a(eyeVec, sumVec, false, false, false);
        return mop != null && mop.field_72313_a == MovingObjectType.BLOCK ? mop : null;
    }

    public static MovingObjectPosition rayTraceCustom(double blockReachDistance, float yaw, float pitch) {
        Vec3 vec3 = mc.field_71439_g.func_174824_e(1.0F);
        Vec3 vec31 = getVectorForRotation(pitch, yaw);
        Vec3 vec32 = vec3.func_72441_c(vec31.field_72450_a * blockReachDistance, vec31.field_72448_b * blockReachDistance, vec31.field_72449_c * blockReachDistance);
        return mc.field_71441_e.func_147447_a(vec3, vec32, false, false, true);
    }

    public static MovingObjectPosition rayTraceBlockIfNoEntityInFront(double reach, float yaw, float pitch) {
        if (mc.field_71439_g != null && mc.field_71441_e != null) {
            MovingObjectPosition blockHit = rayTraceCustom(reach, yaw, pitch);
            Vec3 eyes = mc.field_71439_g.func_174824_e(1.0F);
            if (blockHit != null && blockHit.field_72313_a == MovingObjectType.BLOCK) {
                double blockDist = blockHit.field_72307_f.func_72438_d(eyes);
                Vec3 lookVec = getVectorForRotation(pitch, yaw);
                Vec3 end = eyes.func_72441_c(lookVec.field_72450_a * reach, lookVec.field_72448_b * reach, lookVec.field_72449_c * reach);
                for (Entity entity : mc.field_71441_e.func_175674_a(mc.field_71439_g, mc.field_71439_g.func_174813_aQ().func_72321_a(lookVec.field_72450_a * reach, lookVec.field_72448_b * reach, lookVec.field_72449_c * reach).func_72314_b(1.0, 1.0, 1.0), Predicates.and(EntitySelectors.field_180132_d, Entity::func_70067_L))) {
                    float border = entity.func_70111_Y();
                    AxisAlignedBB aabb = entity.func_174813_aQ().func_72314_b(border, border, border);
                    MovingObjectPosition entityHit = aabb.func_72327_a(eyes, end);
                    if (aabb.func_72318_a(eyes)) {
                        return null;
                    }
                    if (entityHit != null) {
                        double entityDist = eyes.func_72438_d(entityHit.field_72307_f);
                        if (entityDist < blockDist) {
                            return null;
                        }
                    }
                }
                return blockHit;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    public static Vec3 getVectorForRotation(float pitch, float yaw) {
        float f = MathHelper.func_76134_b(-yaw * (float) (Math.PI / 180.0) - (float) Math.PI);
        float f1 = MathHelper.func_76126_a(-yaw * (float) (Math.PI / 180.0) - (float) Math.PI);
        float f2 = -MathHelper.func_76134_b(-pitch * (float) (Math.PI / 180.0));
        float f3 = MathHelper.func_76126_a(-pitch * (float) (Math.PI / 180.0));
        return new Vec3(f1 * f2, f3, f * f2);
    }

    public static float applyVanilla(float yaw, boolean stop) {
        if (stop) {
            return yaw;
        } else {
            int scaleFactor = (int) Math.floor(serverRotations[0] / 360.0F);
            float unwrappedYaw = yaw + 360 * scaleFactor;
            if (unwrappedYaw < serverRotations[0] - 180.0F) {
                unwrappedYaw += 360.0F;
            } else if (unwrappedYaw > serverRotations[0] + 180.0F) {
                unwrappedYaw -= 360.0F;
            }
            float deltaYaw = unwrappedYaw - serverRotations[0];
            return serverRotations[0] + deltaYaw;
        }
    }

    public static MovingObjectPosition rayTrace(double range, float partialTicks, float[] rotations, EntityLivingBase ignoreCollision) {
        if (ignoreCollision != null) {
            MovingObjectPosition target = rayTraceIgnore(range, partialTicks, rotations, ignoreCollision);
            if (target != null) {
                return target;
            }
        }
        Entity targetEntity = null;
        double d0 = range;
        if (rotations == null) {
            rotations = new float[] { mc.field_71439_g.field_70177_z, mc.field_71439_g.field_70125_A };
        }
        MovingObjectPosition hitObject = rayTraceCustom(range, rotations[0], rotations[1]);
        double distanceTo = range;
        Vec3 vec3 = mc.field_71439_g.func_174824_e(partialTicks);
        if (mc.field_71442_b.func_78749_i()) {
            d0 = 6.0;
            distanceTo = 6.0;
        }
        if (hitObject != null) {
            distanceTo = hitObject.field_72307_f.func_72438_d(vec3);
        }
        Vec3 vec31 = getVectorForRotation(rotations[1], rotations[0]);
        Vec3 vec32 = vec3.func_72441_c(vec31.field_72450_a * d0, vec31.field_72448_b * d0, vec31.field_72449_c * d0);
        Vec3 vec33 = null;
        float f = 1.0F;
        List<Entity> list = mc.field_71441_e.func_175674_a(mc.field_71439_g, mc.field_71439_g.func_174813_aQ().func_72321_a(vec31.field_72450_a * d0, vec31.field_72448_b * d0, vec31.field_72449_c * d0).func_72314_b(f, f, f), Predicates.and(EntitySelectors.field_180132_d, Entity::func_70067_L));
        double d2 = distanceTo;
        for (int j = 0; j < list.size(); j++) {
            Entity entity1 = list.get(j);
            float f1 = entity1.func_70111_Y();
            AxisAlignedBB axisalignedbb = entity1.func_174813_aQ().func_72314_b(f1, f1, f1);
            MovingObjectPosition movingobjectposition = axisalignedbb.func_72327_a(vec3, vec32);
            if (axisalignedbb.func_72318_a(vec3)) {
                if (d2 >= 0.0) {
                    targetEntity = entity1;
                    vec33 = movingobjectposition == null ? vec3 : movingobjectposition.field_72307_f;
                    d2 = 0.0;
                }
            } else if (movingobjectposition != null) {
                double d3 = vec3.func_72438_d(movingobjectposition.field_72307_f);
                if (d3 < d2 || d2 == 0.0) {
                    if (entity1 != mc.field_71439_g.field_70154_o || mc.field_71439_g.canRiderInteract()) {
                        targetEntity = entity1;
                        vec33 = movingobjectposition.field_72307_f;
                        d2 = d3;
                    } else if (d2 == 0.0) {
                        targetEntity = entity1;
                        vec33 = movingobjectposition.field_72307_f;
                    }
                }
            }
        }
        return targetEntity != null && d2 < distanceTo ? new MovingObjectPosition(targetEntity, vec33) : null;
    }

    public static MovingObjectPosition rayTraceIgnore(double range, float partialTicks, float[] rotations, EntityLivingBase ignoreCollision) {
        MovingObjectPosition blockHit = rayTraceCustom(range, rotations[0], rotations[1]);
        Vec3 start = mc.field_71439_g.func_174824_e(partialTicks);
        double blockDistance = range;
        if (blockHit != null) {
            blockDistance = blockHit.field_72307_f.func_72438_d(start);
        }
        if (ignoreCollision != null) {
            if (rotations == null) {
                rotations = new float[] { mc.field_71439_g.field_70177_z, mc.field_71439_g.field_70125_A };
            }
            Vec3 lookVec = getVectorForRotation(rotations[1], rotations[0]);
            Vec3 end = start.func_72441_c(lookVec.field_72450_a * range, lookVec.field_72448_b * range, lookVec.field_72449_c * range);
            float f1 = ignoreCollision.func_70111_Y();
            AxisAlignedBB aabb = ignoreCollision.func_174813_aQ().func_72314_b(f1, f1, f1);
            MovingObjectPosition ignoreMOP = aabb.func_72327_a(start, end);
            if (aabb.func_72318_a(start)) {
                return new MovingObjectPosition(ignoreCollision, start);
            }
            if (ignoreMOP != null) {
                double ignoreDist = start.func_72438_d(ignoreMOP.field_72307_f);
                if (ignoreDist < blockDistance) {
                    return new MovingObjectPosition(ignoreCollision, ignoreMOP.field_72307_f);
                }
            }
        }
        return blockHit != null ? blockHit : null;
    }

    public static float applyVanilla(float yaw) {
        return applyVanilla(yaw, false);
    }

    public static float[] getRotationsFromEye(Vec3 eye, double tx, double ty, double tz) {
        double dx = tx - eye.field_72450_a;
        double dy = ty - eye.field_72448_b;
        double dz = tz - eye.field_72449_c;
        double dist = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
        float pitch = (float) (-Math.toDegrees(Math.atan2(dy, dist)));
        return new float[] { yaw, pitch };
    }

    public static enum PLAYER_OFFSETS {

        EYE, CHEST, FOOT, NONE;

        public double getHeightOffset(Entity entity) {
            switch(this) {
                case NONE:
                case EYE:
                    return entity.func_70047_e();
                case CHEST:
                    return entity.field_70131_O / 2.0F;
                case FOOT:
                    return 0.0;
                default:
                    return entity.func_70047_e();
            }
        }
    }
}
