package keystrokesmod.utility;

import keystrokesmod.module.impl.world.AntiBot;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MovingObjectPosition;

public final class CombatTargeting implements IMinecraftInstance {

    private CombatTargeting() {
    }

    public static EntityPlayer findTarget(double maxDistanceSq) {
        return findTarget(maxDistanceSq, true);
    }

    public static EntityPlayer findTarget(double maxDistanceSq, boolean ignoreTeammates) {
        EntityPlayer mouseOverTarget = getMouseOverTarget(maxDistanceSq, ignoreTeammates);
        return mouseOverTarget != null ? mouseOverTarget : findClosestTarget(maxDistanceSq, ignoreTeammates);
    }

    public static EntityPlayer findClosestTarget(double maxDistanceSq) {
        return findClosestTarget(maxDistanceSq, true);
    }

    public static EntityPlayer findClosestTarget(double maxDistanceSq, boolean ignoreTeammates) {
        if (mc != null && mc.field_71441_e != null) {
            EntityPlayer closest = null;
            double closestDistanceSq = Double.MAX_VALUE;
            for (EntityPlayer player : mc.field_71441_e.field_73010_i) {
                if (isValidPlayer(player, maxDistanceSq, ignoreTeammates)) {
                    double distanceSq = RotationUtils.distanceSqFromEyeToClosestOnAABB(player);
                    if (distanceSq < closestDistanceSq) {
                        closestDistanceSq = distanceSq;
                        closest = player;
                    }
                }
            }
            return closest;
        } else {
            return null;
        }
    }

    public static EntityPlayer getMouseOverTarget(double maxDistanceSq) {
        return getMouseOverTarget(maxDistanceSq, true);
    }

    public static EntityPlayer getMouseOverTarget(double maxDistanceSq, boolean ignoreTeammates) {
        if (mc != null && mc.field_71476_x != null) {
            MovingObjectPosition objectMouseOver = mc.field_71476_x;
            return asValidPlayer(objectMouseOver.field_72308_g, maxDistanceSq, ignoreTeammates);
        } else {
            return null;
        }
    }

    public static EntityPlayer asValidPlayer(Entity entity, double maxDistanceSq) {
        return asValidPlayer(entity, maxDistanceSq, true);
    }

    public static EntityPlayer asValidPlayer(Entity entity, double maxDistanceSq, boolean ignoreTeammates) {
        if (!(entity instanceof EntityPlayer)) {
            return null;
        } else {
            EntityPlayer player = (EntityPlayer) entity;
            return isValidPlayer(player, maxDistanceSq, ignoreTeammates) ? player : null;
        }
    }

    public static boolean isValidPlayer(EntityPlayer player, double maxDistanceSq) {
        return isValidPlayer(player, maxDistanceSq, true);
    }

    public static boolean isValidPlayer(EntityPlayer player, double maxDistanceSq, boolean ignoreTeammates) {
        return isTrackablePlayer(player, ignoreTeammates) && isWithinRange(player, maxDistanceSq);
    }

    public static boolean isTrackablePlayer(EntityPlayer player) {
        return isTrackablePlayer(player, true);
    }

    public static boolean isTrackablePlayer(EntityPlayer player, boolean ignoreTeammates) {
        if (!Utils.nullCheck() || player == null || player == mc.field_71439_g || player.field_70128_L || player.field_70725_aQ != 0) {
            return false;
        } else {
            return Utils.isFriended(player) || AntiBot.isBot(player) ? false : !ignoreTeammates || !Utils.isTeammate(player);
        }
    }

    public static boolean isWithinRange(EntityPlayer player, double maxDistanceSq) {
        return player == null ? false : RotationUtils.distanceSqFromEyeToClosestOnAABB(player) <= maxDistanceSq;
    }
}
