package keystrokesmod.utility;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import keystrokesmod.mixin.impl.accessor.IAccessorEntityArrow;
import keystrokesmod.module.impl.world.AntiBot;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.item.EntityEnderPearl;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.projectile.EntityLargeFireball;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos.MutableBlockPos;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;

public final class ProjectileTrajectoryRenderer {

    private static final Minecraft mc = Minecraft.func_71410_x();

    private static final Color FIREBALL_COLOR = new Color(255, 150, 0);

    private static final Color PEARL_COLOR = new Color(210, 0, 255);

    private static final float FIREBALL_TRAJECTORY_LINE_WIDTH = 2.0F;

    private static final float FIREBALL_TRAJECTORY_SHADE_ALPHA = 0.18F;

    private static final double FIREBALL_ACCELERATION = 0.1;

    private static final double FIRST_PERSON_RENDER_CLIP_DISTANCE = 0.5;

    private static final double ARROW_GRAVITY = 0.05;

    private static final double THROWABLE_GRAVITY = 0.03;

    private static final double PROJECTILE_DRAG = 0.99;

    private static final double ARROW_WATER_DRAG = 0.6;

    private static final double THROWABLE_WATER_DRAG = 0.8;

    private static final double WATER_FLOW_ACCELERATION = 0.014;

    private static final double WATER_CHECK_EXPAND_Y = -0.4F;

    private static final double WATER_CHECK_CONTRACT = 0.001;

    private static final double COLLISION_EPSILON_SQ = 1.0E-7;

    private static final double ENTITY_HIT_EXPANSION = 0.3;

    private static final int PROJECTILE_TRAJECTORY_MAX_TICKS = 120;

    private static final int PROJECTILE_TRAJECTORY_SUBDIVISIONS = 4;

    private static final int PROJECTILE_IGNORE_OWNER_TICKS = 5;

    private static final double ARROW_TRAJECTORY_MARKER_BACK_OFFSET = 0.12;

    private static final double ARROW_TRAJECTORY_MARKER_INNER_RADIUS = 0.02;

    private static final double ARROW_TRAJECTORY_MARKER_OUTER_RADIUS = 0.15;

    private static final float ARROW_TRAJECTORY_MARKER_LINE_WIDTH = 2.0F;

    private static final float ARROW_TRAJECTORY_MARKER_ALPHA = 1.0F;

    private static final double ARROW_TRAJECTORY_MARKER_SPIN_SPEED = 0.35;

    private static final double ARROW_TRAJECTORY_MARKER_INNER_BACK_SWEEP = 0.01;

    private static final double ARROW_TRAJECTORY_MARKER_OUTER_BACK_SWEEP = 0.05;

    private final Map<EntityLargeFireball, Vec3> fireballAccelerationOverrides = new ConcurrentHashMap<>();

    private World world;

    private EntityArrow arrowPreview;

    private EntityEnderPearl pearlPreview;

    private EntityLargeFireball fireballPreview;

    private boolean crossTarget;

    private boolean autoScale;

    private float targetLineWidth = 2.0F;

    public void configureTarget(boolean crossTarget, boolean autoScale, float targetLineWidth) {
        this.crossTarget = crossTarget;
        this.autoScale = autoScale;
        this.targetLineWidth = targetLineWidth;
    }

    public void clear() {
        this.fireballAccelerationOverrides.clear();
        this.arrowPreview = null;
        this.pearlPreview = null;
        this.fireballPreview = null;
        this.world = null;
    }

    public void updateWorld() {
        if (this.world != mc.field_71441_e) {
            this.clear();
            this.world = mc.field_71441_e;
        }
        this.fireballAccelerationOverrides.keySet().removeIf(fireball -> fireball.field_70128_L || fireball.field_70170_p != this.world || this.world == null || this.world.func_73045_a(fireball.func_145782_y()) != fireball);
    }

    public void onVelocityPacket(S12PacketEntityVelocity packet) {
        if (mc.field_71441_e != null) {
            Entity entity = mc.field_71441_e.func_73045_a(packet.func_149412_c());
            if (entity instanceof EntityLargeFireball) {
                Vec3 motion = new Vec3(packet.func_149411_d() / 8000.0, packet.func_149410_e() / 8000.0, packet.func_149409_f() / 8000.0);
                double length = motion.func_72433_c();
                if (length > 0.0) {
                    double scale = 0.1 / length;
                    this.fireballAccelerationOverrides.put((EntityLargeFireball) entity, new Vec3(motion.field_72450_a * scale, motion.field_72448_b * scale, motion.field_72449_c * scale));
                }
            }
        }
    }

    public void renderThrown(Entity entity, float partialTicks) {
        if (!entity.field_70128_L) {
            if (entity instanceof EntityLargeFireball) {
                this.renderFireballTrajectory((EntityLargeFireball) entity, partialTicks);
            } else if (entity instanceof EntityArrow && !((IAccessorEntityArrow) entity).getInGround()) {
                this.renderArrowTrajectory((EntityArrow) entity, partialTicks);
            } else if (entity instanceof EntityEnderPearl) {
                EntityEnderPearl pearl = (EntityEnderPearl) entity;
                this.renderPearlTrajectory(pearl, pearl.func_85052_h(), partialTicks);
            }
        }
    }

    public void renderHeld(EntityPlayer player, float partialTicks) {
        ItemStack heldStack = player.func_70694_bm();
        if (heldStack != null) {
            Item item = heldStack.func_77973_b();
            double velocity = 1.5;
            if (item == Items.field_151031_f) {
                if (!player.func_71039_bw() || player.func_71011_bu().func_77973_b() != Items.field_151031_f) {
                    return;
                }
                float charge = (player.func_71057_bx() + partialTicks) / 20.0F;
                charge = Math.min(1.0F, (charge * charge + charge * 2.0F) / 3.0F);
                if (charge < 0.1F) {
                    return;
                }
                velocity = charge * 3.0;
            } else if (item != Items.field_151079_bi && item != Items.field_151059_bz) {
                return;
            }
            float previousYaw = player == mc.field_71439_g ? player.field_70126_B : player.field_70758_at;
            float currentYaw = player == mc.field_71439_g ? player.field_70177_z : player.field_70759_as;
            float yaw = (float) Math.toRadians(previousYaw + MathHelper.func_76142_g(currentYaw - previousYaw) * partialTicks);
            float pitch = (float) Math.toRadians(player.field_70127_C + (player.field_70125_A - player.field_70127_C) * partialTicks);
            Vec3 direction = new Vec3(-MathHelper.func_76126_a(yaw) * MathHelper.func_76134_b(pitch), -MathHelper.func_76126_a(pitch), MathHelper.func_76134_b(yaw) * MathHelper.func_76134_b(pitch)).func_72432_b();
            double posX = player.field_70142_S + (player.field_70165_t - player.field_70142_S) * partialTicks;
            double posY = player.field_70137_T + (player.field_70163_u - player.field_70137_T) * partialTicks + player.func_70047_e();
            double posZ = player.field_70136_U + (player.field_70161_v - player.field_70136_U) * partialTicks;
            posX -= MathHelper.func_76134_b(yaw) * 0.16F;
            posY -= 0.1F;
            posZ -= MathHelper.func_76126_a(yaw) * 0.16F;
            if (item == Items.field_151059_bz) {
                if (this.fireballPreview == null) {
                    this.fireballPreview = new EntityLargeFireball(mc.field_71441_e);
                }
                this.fireballPreview.field_70235_a = player;
                this.fireballPreview.field_70232_b = direction.field_72450_a * 0.1;
                this.fireballPreview.field_70233_c = direction.field_72448_b * 0.1;
                this.fireballPreview.field_70230_d = direction.field_72449_c * 0.1;
                this.positionPreview(this.fireballPreview, posX, posY - this.fireballPreview.field_70131_O * 0.5, posZ, direction, 0.0);
                this.renderFireballTrajectory(this.fireballPreview, partialTicks);
            } else {
                if (item == Items.field_151031_f) {
                    if (this.arrowPreview == null) {
                        this.arrowPreview = new EntityArrow(mc.field_71441_e);
                    }
                    this.arrowPreview.field_70250_c = player;
                    this.positionPreview(this.arrowPreview, posX, posY, posZ, direction, velocity);
                    this.renderArrowTrajectory(this.arrowPreview, partialTicks);
                } else {
                    if (this.pearlPreview == null) {
                        this.pearlPreview = new EntityEnderPearl(mc.field_71441_e);
                    }
                    this.positionPreview(this.pearlPreview, posX, posY, posZ, direction, velocity);
                    this.renderPearlTrajectory(this.pearlPreview, player, partialTicks);
                }
            }
        }
    }

    private void positionPreview(Entity projectile, double posX, double posY, double posZ, Vec3 direction, double velocity) {
        projectile.func_70107_b(posX, posY, posZ);
        projectile.field_70142_S = posX;
        projectile.field_70137_T = posY;
        projectile.field_70136_U = posZ;
        projectile.field_70159_w = direction.field_72450_a * velocity;
        projectile.field_70181_x = direction.field_72448_b * velocity;
        projectile.field_70179_y = direction.field_72449_c * velocity;
        projectile.field_70173_aa = 0;
    }

    private void renderFireballTrajectory(EntityLargeFireball fireball, float partialTicks) {
        Vec3 accelerationOverride = this.fireballAccelerationOverrides.get(fireball);
        FireballSImulator.Result result = this.crossTarget ? FireballSImulator.simulateCenterLine(fireball, accelerationOverride) : FireballSImulator.simulate(fireball, accelerationOverride);
        Vec3 impactPosition = result.getImpactPosition();
        if (impactPosition != null) {
            double viewerX = mc.func_175598_ae().field_78730_l;
            double viewerY = mc.func_175598_ae().field_78731_m;
            double viewerZ = mc.func_175598_ae().field_78728_n;
            double startX = fireball.field_70142_S + (fireball.field_70165_t - fireball.field_70142_S) * partialTicks;
            double startY = fireball.field_70137_T + (fireball.field_70163_u - fireball.field_70137_T) * partialTicks + fireball.field_70131_O * 0.5;
            double startZ = fireball.field_70136_U + (fireball.field_70161_v - fireball.field_70136_U) * partialTicks;
            Vec3 targetPosition = this.crossTarget && result.hitBlock() ? result.getHitPosition() : impactPosition.func_72441_c(0.0, fireball.field_70131_O * 0.5, 0.0);
            if (fireball == this.fireballPreview && fireball.field_70235_a == mc.field_71439_g && mc.field_71474_y.field_74320_O == 0) {
                Vec3 segment = new Vec3(targetPosition.field_72450_a - startX, targetPosition.field_72448_b - startY, targetPosition.field_72449_c - startZ);
                double length = segment.func_72433_c();
                if (length > 0.0) {
                    double clipScale = Math.min(0.5, length) / length;
                    startX += segment.field_72450_a * clipScale;
                    startY += segment.field_72448_b * clipScale;
                    startZ += segment.field_72449_c * clipScale;
                }
            }
            Color fireballColor = FIREBALL_COLOR;
            float red = fireballColor.getRed() / 255.0F;
            float green = fireballColor.getGreen() / 255.0F;
            float blue = fireballColor.getBlue() / 255.0F;
            GL11.glPushMatrix();
            GL11.glEnable(3042);
            GL11.glBlendFunc(770, 771);
            GL11.glEnable(2848);
            GL11.glDisable(3553);
            GL11.glDisable(2929);
            GL11.glDepthMask(false);
            GL11.glLineWidth(2.0F);
            GL11.glColor4f(red, green, blue, 1.0F);
            GL11.glBegin(1);
            GL11.glVertex3d(startX - viewerX, startY - viewerY, startZ - viewerZ);
            GL11.glVertex3d(targetPosition.field_72450_a - viewerX, targetPosition.field_72448_b - viewerY, targetPosition.field_72449_c - viewerZ);
            GL11.glEnd();
            if (this.crossTarget) {
                MovingObjectPosition collision = result.getCollision();
                renderCrossIndicator(targetPosition, collision != null ? collision.field_178784_b : null, viewerX, viewerY, viewerZ, red, green, blue, this.autoScale, this.targetLineWidth);
            } else {
                AxisAlignedBB impactBox2 = this.getImpactBox(fireball, impactPosition);
                RenderUtils.drawOutlinedBox(impactBox2, viewerX, viewerY, viewerZ);
                RenderUtils.drawBoundingBox(impactBox2.func_72317_d(-viewerX, -viewerY, -viewerZ), red, green, blue, 0.18F);
            }
            GL11.glLineWidth(1.0F);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            GL11.glDepthMask(true);
            GL11.glEnable(2929);
            GL11.glEnable(3553);
            GL11.glDisable(2848);
            GL11.glDisable(3042);
            GL11.glPopMatrix();
        }
    }

    private void renderArrowTrajectory(EntityArrow arrowEntity, float partialTicks) {
        ProjectileTrajectoryRenderer.ProjectileTrajectoryProps props = new ProjectileTrajectoryRenderer.ProjectileTrajectoryProps(0.05, 0.99, 0.6, Math.max(arrowEntity.field_70130_N * 0.5, 0.25), arrowEntity.field_70130_N, arrowEntity.field_70131_O, true, 5);
        this.renderPredictedTrajectory(arrowEntity, partialTicks, this.predictProjectileTrajectory(arrowEntity, props, arrowEntity.field_70250_c), Color.WHITE);
    }

    private void renderPearlTrajectory(EntityEnderPearl pearlEntity, Entity owner, float partialTicks) {
        ProjectileTrajectoryRenderer.ProjectileTrajectoryProps props = new ProjectileTrajectoryRenderer.ProjectileTrajectoryProps(0.03, 0.99, 0.8, Math.max(pearlEntity.field_70130_N * 0.5, 0.125), pearlEntity.field_70130_N, pearlEntity.field_70131_O, false, 5);
        Color pearlColor = PEARL_COLOR;
        this.renderPredictedTrajectory(pearlEntity, partialTicks, this.predictProjectileTrajectory(pearlEntity, props, owner), pearlColor);
    }

    private ProjectileTrajectoryRenderer.TrajectoryPrediction predictProjectileTrajectory(Entity projectile, ProjectileTrajectoryRenderer.ProjectileTrajectoryProps props, Entity owner) {
        double posX = projectile.field_70165_t;
        double posY = projectile.field_70163_u;
        double posZ = projectile.field_70161_v;
        double motionX = projectile.field_70159_w;
        double motionY = projectile.field_70181_x;
        double motionZ = projectile.field_70179_y;
        int ticksInAir = Math.max(0, projectile.field_70173_aa);
        List<Vec3> points = new ArrayList<>();
        points.add(new Vec3(posX, posY, posZ));
        for (int tick = 0; tick < 120; tick++) {
            ProjectileTrajectoryRenderer.FluidState fluidState = this.sampleFluidState(posX, posY, posZ, props.width, props.height, projectile);
            double[] motion = new double[] { motionX, motionY, motionZ };
            this.applyWaterFlowAcceleration(fluidState, motion);
            motionX = motion[0];
            motionY = motion[1];
            motionZ = motion[2];
            double nextX = posX + motionX;
            double nextY = posY + motionY;
            double nextZ = posZ + motionZ;
            Vec3 start = new Vec3(posX, posY, posZ);
            Vec3 end = new Vec3(nextX, nextY, nextZ);
            ProjectileTrajectoryRenderer.BlockCollisionResult blockCollision = this.getNearestBlockCollision(start, end, props);
            MovingObjectPosition blockHit = blockCollision.hit;
            double bestDistanceSq = blockCollision.distanceSq;
            Vec3 clampedEnd = blockHit != null ? blockHit.field_72307_f : end;
            AxisAlignedBB axisAlignedBB = new AxisAlignedBB(posX - props.hitboxRadius, posY - props.hitboxRadius, posZ - props.hitboxRadius, posX + props.hitboxRadius, posY + props.hitboxRadius, posZ + props.hitboxRadius).func_72321_a(motionX, motionY, motionZ).func_72314_b(1.0, 1.0, 1.0);
            Vec3 bestEntityHit = null;
            EnumFacing bestEntityHitFace = null;
            for (Entity entity : mc.field_71441_e.func_72839_b(projectile, axisAlignedBB)) {
                if (entity instanceof EntityLivingBase && !(entity instanceof EntityArmorStand) && entity.func_70067_L() && ((EntityLivingBase) entity).field_70725_aQ == 0 && (!(entity instanceof EntityPlayer) || !AntiBot.isBot(entity)) && (owner == null || !entity.func_70028_i(owner) || ticksInAir >= props.ignoreOwnerTicks)) {
                    AxisAlignedBB axisAlignedBB2 = entity.func_174813_aQ().func_72314_b(0.3, 0.3, 0.3);
                    MovingObjectPosition entityHit = axisAlignedBB2.func_72327_a(start, clampedEnd);
                    if (entityHit != null) {
                        double distanceSq = start.func_72436_e(entityHit.field_72307_f);
                        if (distanceSq + 1.0E-7 < bestDistanceSq) {
                            bestDistanceSq = distanceSq;
                            bestEntityHit = entityHit.field_72307_f;
                            bestEntityHitFace = entityHit.field_178784_b;
                        }
                    }
                }
            }
            if (bestEntityHit != null) {
                this.addHitSegmentPoints(points, posX, posY, posZ, motionX, motionY, motionZ, bestEntityHit);
                return new ProjectileTrajectoryRenderer.TrajectoryPrediction(points, bestEntityHit, null, bestEntityHitFace, props.width, props.height);
            }
            if (blockHit != null) {
                this.addHitSegmentPoints(points, posX, posY, posZ, motionX, motionY, motionZ, blockHit.field_72307_f);
                return new ProjectileTrajectoryRenderer.TrajectoryPrediction(points, blockHit.field_72307_f, blockHit.func_178782_a(), blockHit.field_178784_b, props.width, props.height);
            }
            this.addFullSegmentPoints(points, posX, posY, posZ, motionX, motionY, motionZ);
            posX = nextX;
            posY = nextY;
            posZ = nextZ;
            motionX *= fluidState.inWater ? props.dragInWater : props.drag;
            motionY *= fluidState.inWater ? props.dragInWater : props.drag;
            motionZ *= fluidState.inWater ? props.dragInWater : props.drag;
            motionY -= props.gravity;
            ticksInAir++;
            if (nextY < -64.0) {
                break;
            }
        }
        return new ProjectileTrajectoryRenderer.TrajectoryPrediction(points, null, null, null, props.width, props.height);
    }

    private void addFullSegmentPoints(List<Vec3> points, double posX, double posY, double posZ, double motionX, double motionY, double motionZ) {
        for (int step = 1; step <= 4; step++) {
            double t = step / 4.0;
            points.add(new Vec3(posX + motionX * t, posY + motionY * t, posZ + motionZ * t));
        }
    }

    private void addHitSegmentPoints(List<Vec3> points, double posX, double posY, double posZ, double motionX, double motionY, double motionZ, Vec3 hitVec) {
        double segmentLengthSq = motionX * motionX + motionY * motionY + motionZ * motionZ;
        double doubleValue = hitVec.field_72450_a - posX;
        double hitDy = hitVec.field_72448_b - posY;
        double hitDz = hitVec.field_72449_c - posZ;
        double hitDistanceSq = doubleValue * doubleValue + hitDy * hitDy + hitDz * hitDz;
        double hitT = segmentLengthSq > 0.0 ? Math.sqrt(hitDistanceSq / segmentLengthSq) : 0.0;
        hitT = Math.max(0.0, Math.min(1.0, hitT));
        int subCount = (int) Math.ceil(hitT * 4.0);
        for (int step = 1; step < subCount; step++) {
            double t = step / 4.0;
            points.add(new Vec3(posX + motionX * t, posY + motionY * t, posZ + motionZ * t));
        }
        points.add(hitVec);
    }

    private void renderPredictedTrajectory(Entity projectile, float partialTicks, ProjectileTrajectoryRenderer.TrajectoryPrediction prediction, Color color) {
        if (prediction.points.size() >= 2) {
            double viewerX = mc.func_175598_ae().field_78730_l;
            double viewerY = mc.func_175598_ae().field_78731_m;
            double viewerZ = mc.func_175598_ae().field_78728_n;
            double startX = projectile.field_70142_S + (projectile.field_70165_t - projectile.field_70142_S) * partialTicks;
            double startY = projectile.field_70137_T + (projectile.field_70163_u - projectile.field_70137_T) * partialTicks;
            double startZ = projectile.field_70136_U + (projectile.field_70161_v - projectile.field_70136_U) * partialTicks;
            float red = color.getRed() / 255.0F;
            float green = color.getGreen() / 255.0F;
            float blue = color.getBlue() / 255.0F;
            GL11.glPushMatrix();
            GL11.glEnable(3042);
            GL11.glBlendFunc(770, 771);
            GL11.glEnable(2848);
            GL11.glDisable(3553);
            GL11.glDisable(2929);
            GL11.glDepthMask(false);
            GL11.glLineWidth(2.0F);
            GL11.glColor4f(red, green, blue, 1.0F);
            GL11.glBegin(3);
            GL11.glVertex3d(startX - viewerX, startY - viewerY, startZ - viewerZ);
            for (int i = 1; i < prediction.points.size(); i++) {
                Vec3 point = prediction.points.get(i);
                GL11.glVertex3d(point.field_72450_a - viewerX, point.field_72448_b - viewerY, point.field_72449_c - viewerZ);
            }
            GL11.glEnd();
            if (prediction.impactPosition != null) {
                if (this.crossTarget) {
                    renderCrossIndicator(prediction.impactPosition, prediction.hitFace, viewerX, viewerY, viewerZ, red, green, blue, this.autoScale, this.targetLineWidth);
                } else {
                    AxisAlignedBB predictedImpactBox = this.getPredictedImpactBox(projectile, prediction);
                    RenderUtils.drawOutlinedBox(predictedImpactBox, viewerX, viewerY, viewerZ);
                    RenderUtils.drawBoundingBox(predictedImpactBox.func_72317_d(-viewerX, -viewerY, -viewerZ), red, green, blue, 0.18F);
                }
            }
            if (projectile instanceof EntityArrow && projectile != this.arrowPreview) {
                this.renderArrowTrajectoryStartMarker(prediction, projectile, partialTicks, startX, startY, startZ, viewerX, viewerY, viewerZ, red, green, blue);
            }
            GL11.glLineWidth(1.0F);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            GL11.glDepthMask(true);
            GL11.glEnable(2929);
            GL11.glEnable(3553);
            GL11.glDisable(2848);
            GL11.glDisable(3042);
            GL11.glPopMatrix();
        }
    }

    public static void renderCrossIndicator(Vec3 hitPos, EnumFacing hitFace, double camX, double camY, double camZ, float r, float g, float b, boolean autoScale, float lineWidth) {
        double distanceSq = mc.field_71439_g.func_70092_e(hitPos.field_72450_a, hitPos.field_72448_b, hitPos.field_72449_c);
        double size = autoScale ? Math.min(0.1 * (1.0 + distanceSq / 500.0), 0.5) : 0.1;
        double x = hitPos.field_72450_a - camX;
        double y = hitPos.field_72448_b - camY;
        double z = hitPos.field_72449_c - camZ;
        GL11.glLineWidth(lineWidth);
        GL11.glColor4f(r, g, b, 1.0F);
        GL11.glBegin(1);
        switch(hitFace != null ? hitFace : EnumFacing.UP) {
            case WEST:
            case EAST:
                GL11.glVertex3d(x, y - size, z - size);
                GL11.glVertex3d(x, y + size, z + size);
                GL11.glVertex3d(x, y - size, z + size);
                GL11.glVertex3d(x, y + size, z - size);
                break;
            case NORTH:
            case SOUTH:
                GL11.glVertex3d(x - size, y - size, z);
                GL11.glVertex3d(x + size, y + size, z);
                GL11.glVertex3d(x + size, y - size, z);
                GL11.glVertex3d(x - size, y + size, z);
                break;
            case DOWN:
            case UP:
                GL11.glVertex3d(x - size, y, z - size);
                GL11.glVertex3d(x + size, y, z + size);
                GL11.glVertex3d(x + size, y, z - size);
                GL11.glVertex3d(x - size, y, z + size);
        }
        GL11.glEnd();
    }

    private void renderArrowTrajectoryStartMarker(ProjectileTrajectoryRenderer.TrajectoryPrediction prediction, Entity projectile, float partialTicks, double startX, double startY, double startZ, double viewerX, double viewerY, double viewerZ, float red, float green, float blue) {
        Vec3 direction = this.getArrowTrajectoryMarkerDirection(prediction, projectile, startX, startY, startZ);
        if (direction != null) {
            Vec3 armAxisA = this.getPerpendicularUnitVector(direction);
            if (armAxisA != null) {
                Vec3 armAxisB = this.normalizeVec3(this.cross(direction, armAxisA));
                if (armAxisB != null) {
                    double centerX = startX - direction.field_72450_a * 0.12;
                    double centerY = startY - direction.field_72448_b * 0.12;
                    double centerZ = startZ - direction.field_72449_c * 0.12;
                    double spinAngle = (projectile.field_70173_aa + partialTicks) * 0.35;
                    double cosine = Math.cos(spinAngle);
                    double sine = Math.sin(spinAngle);
                    Vec3 rotatedArmAxisA = this.rotateMarkerAxis(armAxisA, armAxisB, cosine, sine);
                    Vec3 rotatedArmAxisB = this.rotateMarkerAxis(armAxisB, armAxisA, cosine, -sine);
                    GL11.glLineWidth(2.0F);
                    GL11.glColor4f(red, green, blue, 1.0F);
                    GL11.glBegin(1);
                    this.addMarkerSegment(centerX, centerY, centerZ, rotatedArmAxisA, direction, viewerX, viewerY, viewerZ);
                    this.addMarkerSegment(centerX, centerY, centerZ, new Vec3(-rotatedArmAxisA.field_72450_a, -rotatedArmAxisA.field_72448_b, -rotatedArmAxisA.field_72449_c), direction, viewerX, viewerY, viewerZ);
                    this.addMarkerSegment(centerX, centerY, centerZ, rotatedArmAxisB, direction, viewerX, viewerY, viewerZ);
                    this.addMarkerSegment(centerX, centerY, centerZ, new Vec3(-rotatedArmAxisB.field_72450_a, -rotatedArmAxisB.field_72448_b, -rotatedArmAxisB.field_72449_c), direction, viewerX, viewerY, viewerZ);
                    GL11.glEnd();
                }
            }
        }
    }

    private void addMarkerSegment(double centerX, double centerY, double centerZ, Vec3 axis, Vec3 direction, double viewerX, double viewerY, double viewerZ) {
        GL11.glVertex3d(centerX + axis.field_72450_a * 0.02 - direction.field_72450_a * 0.01 - viewerX, centerY + axis.field_72448_b * 0.02 - direction.field_72448_b * 0.01 - viewerY, centerZ + axis.field_72449_c * 0.02 - direction.field_72449_c * 0.01 - viewerZ);
        GL11.glVertex3d(centerX + axis.field_72450_a * 0.15 - direction.field_72450_a * 0.05 - viewerX, centerY + axis.field_72448_b * 0.15 - direction.field_72448_b * 0.05 - viewerY, centerZ + axis.field_72449_c * 0.15 - direction.field_72449_c * 0.05 - viewerZ);
    }

    private Vec3 getArrowTrajectoryMarkerDirection(ProjectileTrajectoryRenderer.TrajectoryPrediction prediction, Entity projectile, double startX, double startY, double startZ) {
        Vec3 direction = this.normalizeVec3(new Vec3(projectile.field_70159_w, projectile.field_70181_x, projectile.field_70179_y));
        if (direction != null) {
            return direction;
        } else if (prediction.points.size() < 2) {
            return null;
        } else {
            Vec3 nextPoint = prediction.points.get(1);
            return this.normalizeVec3(new Vec3(nextPoint.field_72450_a - startX, nextPoint.field_72448_b - startY, nextPoint.field_72449_c - startZ));
        }
    }

    private Vec3 getPerpendicularUnitVector(Vec3 direction) {
        Vec3 reference = Math.abs(direction.field_72448_b) < 0.9 ? new Vec3(0.0, 1.0, 0.0) : new Vec3(1.0, 0.0, 0.0);
        return this.normalizeVec3(this.cross(direction, reference));
    }

    private Vec3 rotateMarkerAxis(Vec3 primaryAxis, Vec3 secondaryAxis, double primaryWeight, double secondaryWeight) {
        return new Vec3(primaryAxis.field_72450_a * primaryWeight + secondaryAxis.field_72450_a * secondaryWeight, primaryAxis.field_72448_b * primaryWeight + secondaryAxis.field_72448_b * secondaryWeight, primaryAxis.field_72449_c * primaryWeight + secondaryAxis.field_72449_c * secondaryWeight);
    }

    private Vec3 cross(Vec3 a, Vec3 b) {
        return new Vec3(a.field_72448_b * b.field_72449_c - a.field_72449_c * b.field_72448_b, a.field_72449_c * b.field_72450_a - a.field_72450_a * b.field_72449_c, a.field_72450_a * b.field_72448_b - a.field_72448_b * b.field_72450_a);
    }

    private Vec3 normalizeVec3(Vec3 vector) {
        double lengthSq = vector.field_72450_a * vector.field_72450_a + vector.field_72448_b * vector.field_72448_b + vector.field_72449_c * vector.field_72449_c;
        if (lengthSq <= 1.0E-6) {
            return null;
        } else {
            double invLength = 1.0 / Math.sqrt(lengthSq);
            return new Vec3(vector.field_72450_a * invLength, vector.field_72448_b * invLength, vector.field_72449_c * invLength);
        }
    }

    private AxisAlignedBB getPredictedImpactBox(Entity projectile, ProjectileTrajectoryRenderer.TrajectoryPrediction prediction) {
        if (projectile instanceof EntityArrow) {
            return this.getProjectileImpactBox(prediction);
        } else if (prediction.hitBlockPos != null) {
            AxisAlignedBB blockSelectionBox = BlockUtils.getBlockSelectionBox(prediction.hitBlockPos);
            return blockSelectionBox != null ? blockSelectionBox : new AxisAlignedBB(prediction.hitBlockPos.func_177958_n(), prediction.hitBlockPos.func_177956_o(), prediction.hitBlockPos.func_177952_p(), prediction.hitBlockPos.func_177958_n() + 1.0, prediction.hitBlockPos.func_177956_o() + 1.0, prediction.hitBlockPos.func_177952_p() + 1.0);
        } else {
            return this.getProjectileImpactBox(prediction);
        }
    }

    private AxisAlignedBB getProjectileImpactBox(ProjectileTrajectoryRenderer.TrajectoryPrediction prediction) {
        double halfWidth = prediction.width * 0.5;
        return new AxisAlignedBB(prediction.impactPosition.field_72450_a - halfWidth, prediction.impactPosition.field_72448_b, prediction.impactPosition.field_72449_c - halfWidth, prediction.impactPosition.field_72450_a + halfWidth, prediction.impactPosition.field_72448_b + prediction.height, prediction.impactPosition.field_72449_c + halfWidth);
    }

    private AxisAlignedBB getImpactBox(EntityLargeFireball fireball, Vec3 impactPosition) {
        double halfWidth = fireball.field_70130_N * 0.5;
        return new AxisAlignedBB(impactPosition.field_72450_a - halfWidth, impactPosition.field_72448_b, impactPosition.field_72449_c - halfWidth, impactPosition.field_72450_a + halfWidth, impactPosition.field_72448_b + fireball.field_70131_O, impactPosition.field_72449_c + halfWidth);
    }

    private ProjectileTrajectoryRenderer.FluidState sampleFluidState(double posX, double posY, double posZ, double width, double height, Entity projectile) {
        AxisAlignedBB axisAlignedBB3 = new AxisAlignedBB(posX - width * 0.5, posY, posZ - width * 0.5, posX + width * 0.5, posY + height, posZ + width * 0.5);
        AxisAlignedBB axisAlignedBB4 = axisAlignedBB3.func_72314_b(0.0, -0.4F, 0.0).func_72331_e(0.001, 0.001, 0.001);
        int minX = MathHelper.func_76128_c(axisAlignedBB4.field_72340_a);
        int maxX = MathHelper.func_76128_c(axisAlignedBB4.field_72336_d + 1.0);
        int minY = MathHelper.func_76128_c(axisAlignedBB4.field_72338_b);
        int maxY = MathHelper.func_76128_c(axisAlignedBB4.field_72337_e + 1.0);
        int minZ = MathHelper.func_76128_c(axisAlignedBB4.field_72339_c);
        int maxZ = MathHelper.func_76128_c(axisAlignedBB4.field_72334_f + 1.0);
        if (!mc.field_71441_e.func_175706_a(new BlockPos(minX, minY, minZ), new BlockPos(maxX, maxY, maxZ), true)) {
            return new ProjectileTrajectoryRenderer.FluidState(false, new Vec3(0.0, 0.0, 0.0));
        } else {
            boolean inWater = false;
            Vec3 flowDirection = new Vec3(0.0, 0.0, 0.0);
            MutableBlockPos mutablePos = new MutableBlockPos();
            for (int x = minX; x < maxX; x++) {
                for (int y = minY; y < maxY; y++) {
                    for (int z = minZ; z < maxZ; z++) {
                        mutablePos.func_181079_c(x, y, z);
                        IBlockState blockState = mc.field_71441_e.func_180495_p(mutablePos);
                        if (blockState.func_177230_c().func_149688_o() == Material.field_151586_h) {
                            double liquidSurfaceY = y + 1 - BlockLiquid.func_149801_b((Integer) blockState.func_177229_b(BlockLiquid.field_176367_b));
                            if (maxY >= liquidSurfaceY) {
                                inWater = true;
                                flowDirection = blockState.func_177230_c().func_176197_a(mc.field_71441_e, mutablePos, projectile, flowDirection);
                            }
                        }
                    }
                }
            }
            if (flowDirection.func_72433_c() > 0.0) {
                flowDirection = flowDirection.func_72432_b();
            }
            return new ProjectileTrajectoryRenderer.FluidState(inWater, flowDirection);
        }
    }

    private void applyWaterFlowAcceleration(ProjectileTrajectoryRenderer.FluidState fluidState, double[] motion) {
        if (fluidState.inWater && !(fluidState.flowDirection.func_72433_c() <= 0.0)) {
            motion[0] += fluidState.flowDirection.field_72450_a * 0.014;
            motion[1] += fluidState.flowDirection.field_72448_b * 0.014;
            motion[2] += fluidState.flowDirection.field_72449_c * 0.014;
        }
    }

    private AxisAlignedBB getBlockSweepBounds(Vec3 start, Vec3 end) {
        return new AxisAlignedBB(Math.min(start.field_72450_a, end.field_72450_a), Math.min(start.field_72448_b, end.field_72448_b), Math.min(start.field_72449_c, end.field_72449_c), Math.max(start.field_72450_a, end.field_72450_a), Math.max(start.field_72448_b, end.field_72448_b), Math.max(start.field_72449_c, end.field_72449_c));
    }

    private AxisAlignedBB getVanillaProjectileBounds(Block block, BlockPos pos) {
        block.func_180654_a(mc.field_71441_e, pos);
        return block.func_180646_a(mc.field_71441_e, pos);
    }

    private AxisAlignedBB intersectBoxes(AxisAlignedBB first, AxisAlignedBB second) {
        double minX = Math.max(first.field_72340_a, second.field_72340_a);
        double minY = Math.max(first.field_72338_b, second.field_72338_b);
        double minZ = Math.max(first.field_72339_c, second.field_72339_c);
        double maxX = Math.min(first.field_72336_d, second.field_72336_d);
        double maxY = Math.min(first.field_72337_e, second.field_72337_e);
        double maxZ = Math.min(first.field_72334_f, second.field_72334_f);
        return !(maxX - minX <= 1.0E-7) && !(maxY - minY <= 1.0E-7) && !(maxZ - minZ <= 1.0E-7) ? new AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ) : null;
    }

    private ProjectileTrajectoryRenderer.BlockCollisionResult rayTraceBlockCollisionBoxes(Vec3 start, Vec3 end, ProjectileTrajectoryRenderer.ProjectileTrajectoryProps props) {
        AxisAlignedBB sweepBounds = this.getBlockSweepBounds(start, end);
        int minX = MathHelper.func_76128_c(sweepBounds.field_72340_a);
        int maxX = MathHelper.func_76128_c(sweepBounds.field_72336_d + 1.0);
        int minY = MathHelper.func_76128_c(sweepBounds.field_72338_b);
        int maxY = MathHelper.func_76128_c(sweepBounds.field_72337_e + 1.0);
        int minZ = MathHelper.func_76128_c(sweepBounds.field_72339_c);
        int maxZ = MathHelper.func_76128_c(sweepBounds.field_72334_f + 1.0);
        if (!mc.field_71441_e.func_175706_a(new BlockPos(minX, minY, minZ), new BlockPos(maxX, maxY, maxZ), true)) {
            return new ProjectileTrajectoryRenderer.BlockCollisionResult(null, Double.MAX_VALUE);
        } else {
            List<AxisAlignedBB> collisionBoxes = new ArrayList<>();
            MutableBlockPos mutablePos = new MutableBlockPos();
            MovingObjectPosition bestHit = null;
            double bestDistanceSq = Double.MAX_VALUE;
            for (int x = minX; x < maxX; x++) {
                for (int y = minY; y < maxY; y++) {
                    for (int z = minZ; z < maxZ; z++) {
                        mutablePos.func_181079_c(x, y, z);
                        IBlockState blockState = mc.field_71441_e.func_180495_p(mutablePos);
                        Block block = blockState.func_177230_c();
                        if ((!props.ignoreBlockWithoutBoundingBox || block.func_180640_a(mc.field_71441_e, mutablePos, blockState) != null) && block.func_176209_a(blockState, false)) {
                            collisionBoxes.clear();
                            AxisAlignedBB vanillaProjectileBounds = this.getVanillaProjectileBounds(block, mutablePos);
                            block.func_180638_a(mc.field_71441_e, mutablePos, blockState, sweepBounds, collisionBoxes, null);
                            for (AxisAlignedBB axisAlignedBB5 : collisionBoxes) {
                                AxisAlignedBB axisAlignedBB6 = this.intersectBoxes(axisAlignedBB5, vanillaProjectileBounds);
                                if (axisAlignedBB6 != null) {
                                    MovingObjectPosition hit = axisAlignedBB6.func_72327_a(start, end);
                                    if (hit != null) {
                                        double distanceSq = start.func_72436_e(hit.field_72307_f);
                                        if (distanceSq + 1.0E-7 < bestDistanceSq) {
                                            bestDistanceSq = distanceSq;
                                            bestHit = new MovingObjectPosition(hit.field_72307_f, hit.field_178784_b, new BlockPos(mutablePos));
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            return new ProjectileTrajectoryRenderer.BlockCollisionResult(bestHit, bestDistanceSq);
        }
    }

    private ProjectileTrajectoryRenderer.BlockCollisionResult getNearestBlockCollision(Vec3 start, Vec3 end, ProjectileTrajectoryRenderer.ProjectileTrajectoryProps props) {
        MovingObjectPosition vanillaHit = mc.field_71441_e.func_147447_a(start, end, false, props.ignoreBlockWithoutBoundingBox, false);
        double vanillaDistanceSq = vanillaHit != null ? start.func_72436_e(vanillaHit.field_72307_f) : Double.MAX_VALUE;
        ProjectileTrajectoryRenderer.BlockCollisionResult collisionBoxHit = this.rayTraceBlockCollisionBoxes(start, end, props);
        return collisionBoxHit.hit != null && collisionBoxHit.distanceSq + 1.0E-7 < vanillaDistanceSq ? collisionBoxHit : new ProjectileTrajectoryRenderer.BlockCollisionResult(vanillaHit, vanillaDistanceSq);
    }

    private static final class BlockCollisionResult {

        final MovingObjectPosition hit;

        final double distanceSq;

        private BlockCollisionResult(MovingObjectPosition hit, double distanceSq) {
            this.hit = hit;
            this.distanceSq = distanceSq;
        }
    }

    private static final class FluidState {

        final boolean inWater;

        final Vec3 flowDirection;

        private FluidState(boolean inWater, Vec3 flowDirection) {
            this.inWater = inWater;
            this.flowDirection = flowDirection;
        }
    }

    private static final class ProjectileTrajectoryProps {

        final double gravity;

        final double drag;

        final double dragInWater;

        final double hitboxRadius;

        final double width;

        final double height;

        final boolean ignoreBlockWithoutBoundingBox;

        final int ignoreOwnerTicks;

        private ProjectileTrajectoryProps(double gravity, double drag, double dragInWater, double hitboxRadius, double width, double height, boolean enabled, int ignoreOwnerTicks) {
            this.gravity = gravity;
            this.drag = drag;
            this.dragInWater = dragInWater;
            this.hitboxRadius = hitboxRadius;
            this.width = width;
            this.height = height;
            this.ignoreBlockWithoutBoundingBox = enabled;
            this.ignoreOwnerTicks = ignoreOwnerTicks;
        }
    }

    private static final class TrajectoryPrediction {

        final List<Vec3> points;

        final Vec3 impactPosition;

        final BlockPos hitBlockPos;

        final EnumFacing hitFace;

        final double width;

        final double height;

        private TrajectoryPrediction(List<Vec3> points, Vec3 impactPosition, BlockPos hitBlockPos, EnumFacing hitFace, double width, double height) {
            this.points = points;
            this.impactPosition = impactPosition;
            this.hitBlockPos = hitBlockPos;
            this.hitFace = hitFace;
            this.width = width;
            this.height = height;
        }
    }
}
