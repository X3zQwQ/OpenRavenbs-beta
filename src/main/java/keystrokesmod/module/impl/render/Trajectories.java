package keystrokesmod.module.impl.render;

import java.util.ArrayList;
import java.util.List;
import keystrokesmod.event.ReceivePacketEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.impl.world.AntiBot;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.ColorSetting;
import keystrokesmod.module.setting.impl.GroupSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.BlockUtils;
import keystrokesmod.utility.ProjectileTrajectoryRenderer;
import keystrokesmod.utility.RenderUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.item.EntityEnderPearl;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.projectile.EntityLargeFireball;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos.MutableBlockPos;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import org.lwjgl.opengl.GL11;

public class Trajectories extends Module {

    private static final int HIT_NONE = 0;

    private static final int HIT_ENTITY = 1;

    private static final int HIT_WALL = 2;

    private static final int HIT_GROUND = 3;

    private ButtonSetting disableUnchargedBow;

    private ButtonSetting highlightEntities;

    private ButtonSetting shortenLine;

    private SliderSetting lineThickness;

    private SliderSetting target;

    private ButtonSetting autoScale;

    private ButtonSetting addPlayerVelocity;

    private ColorSetting defaultColor;

    private ColorSetting enemyColor;

    private ColorSetting wallColor;

    private ColorSetting groundColor;

    private SliderSetting maxTicks;

    private ButtonSetting showLanding;

    private ButtonSetting onlyWhenHolding;

    private ButtonSetting fireball;

    private ButtonSetting fireballThrown;

    private ButtonSetting fireballEnemy;

    private ButtonSetting pearlThrown;

    private ButtonSetting pearlEnemy;

    private ButtonSetting arrowThrown;

    private ButtonSetting arrowEnemy;

    private final ProjectileTrajectoryRenderer projectileRenderer = new ProjectileTrajectoryRenderer();

    private static final double ARROW_WATER_DRAG = 0.6;

    private static final double THROWABLE_WATER_DRAG = 0.8;

    private static final double FISH_HOOK_DRAG = 0.92;

    private static final double WATER_FLOW_ACCELERATION = 0.014;

    private static final double WATER_CHECK_EXPAND_Y = -0.4F;

    private static final double WATER_CHECK_CONTRACT = 0.001;

    private static final double LAVA_CHECK_EXPAND_XZ = -0.1F;

    private static final double LAVA_CHECK_EXPAND_Y = -0.4F;

    private static final int FISH_HOOK_WATER_SLICE_COUNT = 5;

    private static final double FISH_HOOK_BUOYANCY_ACCELERATION = 0.04F;

    private static final double FISH_HOOK_WATER_VERTICAL_DAMPING = 0.8;

    private static final double FISH_HOOK_WATER_DRAG_MULTIPLIER = 0.9;

    private static final double FIRST_PERSON_RENDER_CLIP_DISTANCE = 0.5;

    private static final double COLLISION_EPSILON_SQ = 1.0E-7;

    private static final double ENTITY_HIT_EXPANSION = 0.3;

    private static final float BEDBUG_SNOWBALL_VELOCITY = 1.75F;

    public Trajectories() {
        super("Trajectories", Module.category.render);
        this.registerSetting(this.lineThickness = new SliderSetting("Line thickness", 2.0, 1.0, 5.0, 0.1));
        this.registerSetting(this.maxTicks = new SliderSetting("Max ticks", 100.0, 30.0, 200.0, 10.0));
        this.registerSetting(this.target = new SliderSetting("Target", 0, new String[] { "Cross", "Box" }));
        this.registerSetting(this.defaultColor = new ColorSetting("Default", 170, 0, 255));
        this.registerSetting(this.enemyColor = new ColorSetting("Enemy Hit", 255, 50, 50));
        this.registerSetting(this.wallColor = new ColorSetting("Wall Hit", 50, 255, 50));
        this.registerSetting(this.groundColor = new ColorSetting("Ground Hit", 85, 255, 255));
        this.registerSetting(this.autoScale = new ButtonSetting("Auto-scale", true));
        this.registerSetting(this.disableUnchargedBow = new ButtonSetting("Disable uncharged bow", true));
        this.registerSetting(this.highlightEntities = new ButtonSetting("Highlight on entity", true));
        this.registerSetting(this.shortenLine = new ButtonSetting("Shorten line", false));
        this.registerSetting(this.showLanding = new ButtonSetting("Show landing", true));
        this.registerSetting(this.onlyWhenHolding = new ButtonSetting("Only when holding", true));
        this.registerSetting(this.addPlayerVelocity = new ButtonSetting("Add player velocity", false));
        GroupSetting items = new GroupSetting("Items");
        this.registerSetting(items);
        this.registerSetting(this.fireball = new ButtonSetting(items, "Fireball", true));
        this.registerSetting(this.fireballThrown = new ButtonSetting(items, "Fireball (Thrown)", true));
        this.registerSetting(this.fireballEnemy = new ButtonSetting(items, "Fireball (Enemy)", false));
        this.registerSetting(this.pearlThrown = new ButtonSetting(items, "Pearl (Thrown)", true));
        this.registerSetting(this.pearlEnemy = new ButtonSetting(items, "Pearl (Enemy)", false));
        this.registerSetting(this.arrowThrown = new ButtonSetting(items, "Arrow (Thrown)", true));
        this.registerSetting(this.arrowEnemy = new ButtonSetting(items, "Arrow (Enemy)", false));
        this.liteModule = true;
    }

    @Override
    public void onDisable() {
        this.projectileRenderer.clear();
    }

    @SubscribeEvent
    public void onReceivePacket(ReceivePacketEvent event) {
        if (event.getPacket() instanceof S12PacketEntityVelocity) {
            this.projectileRenderer.onVelocityPacket((S12PacketEntityVelocity) event.getPacket());
        }
    }

    @SubscribeEvent
    public void onClientTick(ClientTickEvent event) {
        if (event.phase == Phase.END) {
            this.projectileRenderer.updateWorld();
        }
    }

    private void renderItemTrajectories(float partialTicks) {
        this.projectileRenderer.updateWorld();
        this.projectileRenderer.configureTarget(this.target.getInput() == 0.0, this.autoScale.isToggled(), (float) this.lineThickness.getInput());
        ItemStack localHeldStack = mc.field_71439_g.func_70694_bm();
        if (this.fireball.isToggled() && localHeldStack != null && localHeldStack.func_77973_b() == Items.field_151059_bz) {
            this.projectileRenderer.renderHeld(mc.field_71439_g, partialTicks);
        }
        if (this.fireballThrown.isToggled() || this.pearlThrown.isToggled() || this.arrowThrown.isToggled()) {
            for (Entity entity : mc.field_71441_e.field_72996_f) {
                if (entity instanceof EntityLargeFireball && this.fireballThrown.isToggled() || entity instanceof EntityEnderPearl && this.pearlThrown.isToggled() || entity instanceof EntityArrow && this.arrowThrown.isToggled()) {
                    this.projectileRenderer.renderThrown(entity, partialTicks);
                }
            }
        }
        if (this.fireballEnemy.isToggled() || this.pearlEnemy.isToggled() || this.arrowEnemy.isToggled()) {
            for (EntityPlayer player : mc.field_71441_e.field_73010_i) {
                if (player != mc.field_71439_g && !player.field_70128_L && player.field_70725_aQ == 0 && !player.func_175149_v() && !AntiBot.isBot(player) && !Utils.isFriended(player) && !Utils.isTeammate(player)) {
                    ItemStack heldStack = player.func_70694_bm();
                    if (heldStack != null) {
                        Item item = heldStack.func_77973_b();
                        if (item == Items.field_151059_bz && this.fireballEnemy.isToggled() || item == Items.field_151079_bi && this.pearlEnemy.isToggled() || item == Items.field_151031_f && this.arrowEnemy.isToggled()) {
                            this.projectileRenderer.renderHeld(player, partialTicks);
                        }
                    }
                }
            }
        }
    }

    private float getBowVelocity(float partialTicks) {
        int timeLeft = mc.field_71439_g.func_71052_bv();
        float drawTicks = 72000 - timeLeft + partialTicks;
        float f = drawTicks / 20.0F;
        f = (f * f + f * 2.0F) / 3.0F;
        if (f > 1.0F) {
            f = 1.0F;
        }
        return f * 2.0F * 1.5F;
    }

    private Trajectories.TrajectoryProps getProjectileProperties(Item item, EntityPlayer player, float partialTicks) {
        if (item == Items.field_151031_f) {
            float vel = this.getBowVelocity(partialTicks);
            return new Trajectories.TrajectoryProps(Trajectories.PhysicsModel.ARROW, 0.05, 0.99, 0.6, 0.5, 0.5, 0.5, vel, false, true);
        } else if (item == Items.field_151079_bi) {
            return new Trajectories.TrajectoryProps(Trajectories.PhysicsModel.THROWABLE, 0.03, 0.99, 0.8, 0.25, 0.25, 0.25, 1.5, true, false);
        } else if (item == Items.field_151126_ay || item == Items.field_151110_aK) {
            return new Trajectories.TrajectoryProps(Trajectories.PhysicsModel.THROWABLE, 0.03, 0.99, 0.8, 0.25, 0.25, 0.25, 1.5, true, false);
        } else if (item == Items.field_151062_by) {
            return new Trajectories.TrajectoryProps(Trajectories.PhysicsModel.THROWABLE, 0.07, 0.99, 0.8, 0.25, 0.25, 0.25, 0.7, true, false);
        } else if (item == Items.field_151068_bn) {
            return new Trajectories.TrajectoryProps(Trajectories.PhysicsModel.THROWABLE, 0.05, 0.99, 0.8, 0.25, 0.25, 0.25, 0.5, true, false);
        } else {
            return item == Items.field_151112_aM ? new Trajectories.TrajectoryProps(Trajectories.PhysicsModel.FISH_HOOK, 0.04, 0.92, 0.92, 0.25, 0.25, 0.25, 1.5, false, false) : null;
        }
    }

    private boolean isHypixelBedwars() {
        if (Utils.isHypixel() && mc.field_71441_e != null) {
            Scoreboard sb = mc.field_71441_e.func_96441_U();
            if (sb == null) {
                return false;
            } else {
                ScoreObjective objective = sb.func_96539_a(1);
                return objective != null && Utils.stripString(objective.func_96678_d()).contains("BED WARS");
            }
        } else {
            return false;
        }
    }

    private boolean isBedbugSnowball(ItemStack stack) {
        if (stack != null && stack.func_77973_b() == Items.field_151126_ay) {
            String name = stack.func_82833_r();
            return name == null ? false : Utils.stripString(name).toLowerCase().contains("bedbug");
        } else {
            return false;
        }
    }

    private Trajectories.FluidState sampleFluidState(double posX, double posY, double posZ, Trajectories.TrajectoryProps props) {
        AxisAlignedBB entityBox2 = this.getEntityBox(posX, posY, posZ, props);
        AxisAlignedBB axisAlignedBB = entityBox2.func_72314_b(0.0, -0.4F, 0.0).func_72331_e(0.001, 0.001, 0.001);
        int minX = MathHelper.func_76128_c(axisAlignedBB.field_72340_a);
        int maxX = MathHelper.func_76128_c(axisAlignedBB.field_72336_d + 1.0);
        int minY = MathHelper.func_76128_c(axisAlignedBB.field_72338_b);
        int maxY = MathHelper.func_76128_c(axisAlignedBB.field_72337_e + 1.0);
        int minZ = MathHelper.func_76128_c(axisAlignedBB.field_72339_c);
        int maxZ = MathHelper.func_76128_c(axisAlignedBB.field_72334_f + 1.0);
        if (!mc.field_71441_e.func_175706_a(new BlockPos(minX, minY, minZ), new BlockPos(maxX, maxY, maxZ), true)) {
            return new Trajectories.FluidState(false, false, new Vec3(0.0, 0.0, 0.0));
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
                                flowDirection = blockState.func_177230_c().func_176197_a(mc.field_71441_e, mutablePos, mc.field_71439_g, flowDirection);
                            }
                        }
                    }
                }
            }
            if (flowDirection.func_72433_c() > 0.0) {
                flowDirection = flowDirection.func_72432_b();
            }
            boolean inLava = mc.field_71441_e.func_72875_a(entityBox2.func_72314_b(-0.1F, -0.4F, -0.1F), Material.field_151587_i);
            return new Trajectories.FluidState(inWater, inLava, flowDirection);
        }
    }

    private AxisAlignedBB getEntityBox(double posX, double posY, double posZ, Trajectories.TrajectoryProps props) {
        return new AxisAlignedBB(posX - props.width * 0.5, posY, posZ - props.width * 0.5, posX + props.width * 0.5, posY + props.height, posZ + props.width * 0.5);
    }

    private double sampleWaterSubmersion(double posX, double posY, double posZ, Trajectories.TrajectoryProps props) {
        AxisAlignedBB entityBox3 = this.getEntityBox(posX, posY, posZ, props);
        double submersion = 0.0;
        for (int slice = 0; slice < 5; slice++) {
            double boxHeight = entityBox3.field_72337_e - entityBox3.field_72338_b;
            double minSliceY = entityBox3.field_72338_b + boxHeight * slice / 5.0;
            double maxSliceY = entityBox3.field_72338_b + boxHeight * (slice + 1) / 5.0;
            AxisAlignedBB axisAlignedBB2 = new AxisAlignedBB(entityBox3.field_72340_a, minSliceY, entityBox3.field_72339_c, entityBox3.field_72336_d, maxSliceY, entityBox3.field_72334_f);
            if (mc.field_71441_e.func_72830_b(axisAlignedBB2, Material.field_151586_h)) {
                submersion += 0.2;
            }
        }
        return submersion;
    }

    private void applyWaterFlowAcceleration(Trajectories.FluidState fluidState, double[] mot) {
        if (fluidState.inWater && !(fluidState.flowDirection.func_72433_c() <= 0.0)) {
            mot[0] += fluidState.flowDirection.field_72450_a * 0.014;
            mot[1] += fluidState.flowDirection.field_72448_b * 0.014;
            mot[2] += fluidState.flowDirection.field_72449_c * 0.014;
        }
    }

    private void tickVelocity(Trajectories.TrajectoryProps props, Trajectories.FluidState fluidState, double[] mot) {
        double drag = fluidState.inWater ? props.dragInWater : props.drag;
        mot[0] *= drag;
        mot[1] *= drag;
        mot[2] *= drag;
        mot[1] -= props.gravity;
    }

    private void tickFishingHookVelocity(double posX, double posY, double posZ, Trajectories.TrajectoryProps props, double[] mot) {
        double drag = props.drag;
        double waterSubmersion = this.sampleWaterSubmersion(posX, posY, posZ, props);
        double buoyancy = waterSubmersion * 2.0 - 1.0;
        mot[1] += 0.04F * buoyancy;
        if (waterSubmersion > 0.0) {
            drag *= 0.9;
            mot[1] *= 0.8;
        }
        mot[0] *= drag;
        mot[1] *= drag;
        mot[2] *= drag;
    }

    private void tickPostMoveVelocity(double posX, double posY, double posZ, Trajectories.TrajectoryProps props, Trajectories.FluidState fluidState, double[] mot) {
        if (props.physicsModel == Trajectories.PhysicsModel.FISH_HOOK) {
            this.tickFishingHookVelocity(posX, posY, posZ, props, mot);
        } else {
            if (fluidState.inLava && !fluidState.inWater) {
            }
            this.tickVelocity(props, fluidState, mot);
        }
    }

    private AxisAlignedBB expandEntityCollisionBox(AxisAlignedBB box) {
        return box.func_72314_b(0.3, 0.3, 0.3);
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

    private Trajectories.BlockCollisionResult rayTraceBlockCollisionBoxes(Vec3 start, Vec3 end, Trajectories.TrajectoryProps props) {
        AxisAlignedBB sweepBounds = this.getBlockSweepBounds(start, end);
        int minX = MathHelper.func_76128_c(sweepBounds.field_72340_a);
        int maxX = MathHelper.func_76128_c(sweepBounds.field_72336_d + 1.0);
        int minY = MathHelper.func_76128_c(sweepBounds.field_72338_b);
        int maxY = MathHelper.func_76128_c(sweepBounds.field_72337_e + 1.0);
        int minZ = MathHelper.func_76128_c(sweepBounds.field_72339_c);
        int maxZ = MathHelper.func_76128_c(sweepBounds.field_72334_f + 1.0);
        if (!mc.field_71441_e.func_175706_a(new BlockPos(minX, minY, minZ), new BlockPos(maxX, maxY, maxZ), true)) {
            return new Trajectories.BlockCollisionResult(null, Double.MAX_VALUE);
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
                            for (AxisAlignedBB axisAlignedBB3 : collisionBoxes) {
                                AxisAlignedBB axisAlignedBB4 = this.intersectBoxes(axisAlignedBB3, vanillaProjectileBounds);
                                if (axisAlignedBB4 != null) {
                                    MovingObjectPosition hit = axisAlignedBB4.func_72327_a(start, end);
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
            return new Trajectories.BlockCollisionResult(bestHit, bestDistanceSq);
        }
    }

    private Trajectories.BlockCollisionResult getNearestBlockCollision(Vec3 start, Vec3 end, Trajectories.TrajectoryProps props) {
        MovingObjectPosition vanillaHit = mc.field_71441_e.func_147447_a(start, end, false, props.ignoreBlockWithoutBoundingBox, false);
        double vanillaDistanceSq = vanillaHit != null ? start.func_72436_e(vanillaHit.field_72307_f) : Double.MAX_VALUE;
        Trajectories.BlockCollisionResult collisionBoxHit = this.rayTraceBlockCollisionBoxes(start, end, props);
        return collisionBoxHit.hit != null && collisionBoxHit.distanceSq + 1.0E-7 < vanillaDistanceSq ? collisionBoxHit : new Trajectories.BlockCollisionResult(vanillaHit, vanillaDistanceSq);
    }

    private List<double[]> clipRenderPoints(List<double[]> renderPoints, double hiddenDistance) {
        if (!(hiddenDistance <= 0.0) && renderPoints.size() >= 2) {
            List<double[]> clippedPoints = new ArrayList<>();
            double remainingDistance = hiddenDistance;
            double[] previousPoint = renderPoints.get(0);
            for (int i = 1; i < renderPoints.size(); i++) {
                double[] currentPoint = renderPoints.get(i);
                double deltaX = currentPoint[0] - previousPoint[0];
                double deltaY = currentPoint[1] - previousPoint[1];
                double deltaZ = currentPoint[2] - previousPoint[2];
                double segmentLength = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
                if (remainingDistance > 0.0) {
                    if (segmentLength <= 1.0E-7) {
                        previousPoint = currentPoint;
                        continue;
                    }
                    if (segmentLength <= remainingDistance) {
                        remainingDistance -= segmentLength;
                        previousPoint = currentPoint;
                        continue;
                    }
                    double t = remainingDistance / segmentLength;
                    clippedPoints.add(new double[] { previousPoint[0] + deltaX * t, previousPoint[1] + deltaY * t, previousPoint[2] + deltaZ * t });
                    clippedPoints.add(currentPoint);
                    remainingDistance = 0.0;
                } else {
                    clippedPoints.add(currentPoint);
                }
                previousPoint = currentPoint;
            }
            return clippedPoints;
        } else {
            return renderPoints;
        }
    }

    private ItemStack getHeldProjectile(EntityPlayer player) {
        ItemStack held = player.func_70694_bm();
        if (held == null) {
            return null;
        } else {
            Item item = held.func_77973_b();
            if (item == Items.field_151079_bi || item == Items.field_151126_ay || item == Items.field_151110_aK || item == Items.field_151062_by) {
                return held;
            } else if (item == Items.field_151068_bn) {
                return ItemPotion.func_77831_g(held.func_77960_j()) ? held : null;
            } else if (item instanceof ItemBow) {
                return held;
            } else {
                return item == Items.field_151112_aM ? held : null;
            }
        }
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent e) {
        if (Utils.nullCheck() && mc.field_71441_e != null) {
            this.renderItemTrajectories(e.partialTicks);
            EntityPlayer player = mc.field_71439_g;
            float partialTicks = e.partialTicks;
            ItemStack heldStack = this.getHeldProjectile(player);
            if (!this.onlyWhenHolding.isToggled() || heldStack != null) {
                if (heldStack != null) {
                    Item item = heldStack.func_77973_b();
                    Trajectories.TrajectoryProps props = this.getProjectileProperties(item, player, partialTicks);
                    if (props != null) {
                        double velocity = props.initialVelocity;
                        if (item == Items.field_151126_ay && this.isHypixelBedwars() && this.isBedbugSnowball(heldStack)) {
                            velocity = 1.75;
                        }
                        if (item != Items.field_151031_f || !this.disableUnchargedBow.isToggled() || player.func_71039_bw() && !(velocity < 0.1)) {
                            float yaw = (float) Math.toRadians(player.field_70177_z);
                            float pitch = (float) Math.toRadians(player.field_70125_A);
                            double posX = player.field_70142_S + (player.field_70165_t - player.field_70142_S) * partialTicks - MathHelper.func_76134_b(yaw) * 0.16F;
                            double posY = player.field_70137_T + (player.field_70163_u - player.field_70137_T) * partialTicks + player.func_70047_e() - 0.1;
                            double posZ = player.field_70136_U + (player.field_70161_v - player.field_70136_U) * partialTicks - MathHelper.func_76126_a(yaw) * 0.16F;
                            double hiddenRenderDistance = mc.field_71474_y.field_74320_O == 0 ? 0.5 : 0.0;
                            double motX = -MathHelper.func_76126_a(yaw) * MathHelper.func_76134_b(pitch);
                            double motY = -MathHelper.func_76126_a(pitch);
                            double motZ = MathHelper.func_76134_b(yaw) * MathHelper.func_76134_b(pitch);
                            double len = Math.sqrt(motX * motX + motY * motY + motZ * motZ);
                            motX /= len;
                            motY /= len;
                            motZ /= len;
                            motX *= velocity;
                            motY *= velocity;
                            motZ *= velocity;
                            if (this.addPlayerVelocity.isToggled()) {
                                motX += player.field_70159_w;
                                motY += player.field_70122_E ? 0.0 : player.field_70181_x;
                                motZ += player.field_70179_y;
                            }
                            double[] mot = new double[] { motX, motY, motZ };
                            if (props.requiresInitialTickCorrection) {
                                Trajectories.FluidState fluidState = this.sampleFluidState(posX, posY, posZ, props);
                                this.applyWaterFlowAcceleration(fluidState, mot);
                                this.tickVelocity(props, fluidState, mot);
                                motX = mot[0];
                                motY = mot[1];
                                motZ = mot[2];
                            }
                            List<double[]> renderPoints = new ArrayList<>();
                            MovingObjectPosition hitBlock = null;
                            Entity hitEntity = null;
                            AxisAlignedBB axisAlignedBB5 = null;
                            int hitType = 0;
                            Vec3 hitPos = null;
                            EnumFacing hitFace = EnumFacing.UP;
                            Vec3 terminalPos = null;
                            RenderManager rm = mc.func_175598_ae();
                            int maxSteps = (int) this.maxTicks.getInput();
                            int SUB = 4;
                            double hw = props.hitboxRadius;
                            for (int i = 0; i < maxSteps; i++) {
                                Trajectories.FluidState fluidState = this.sampleFluidState(posX, posY, posZ, props);
                                mot[0] = motX;
                                mot[1] = motY;
                                mot[2] = motZ;
                                this.applyWaterFlowAcceleration(fluidState, mot);
                                motX = mot[0];
                                motY = mot[1];
                                motZ = mot[2];
                                double nextX = posX + motX;
                                double nextY = posY + motY;
                                double nextZ = posZ + motZ;
                                Vec3 start = new Vec3(posX, posY, posZ);
                                Vec3 end = new Vec3(nextX, nextY, nextZ);
                                Trajectories.BlockCollisionResult blockCollision = this.getNearestBlockCollision(start, end, props);
                                MovingObjectPosition blockMop = blockCollision.hit;
                                double blockDistSq = blockCollision.distanceSq;
                                Vec3 clampedEnd = blockMop != null ? new Vec3(blockMop.field_72307_f.field_72450_a, blockMop.field_72307_f.field_72448_b, blockMop.field_72307_f.field_72449_c) : end;
                                AxisAlignedBB axisAlignedBB6 = new AxisAlignedBB(posX - hw, posY - hw, posZ - hw, posX + hw, posY + hw, posZ + hw).func_72321_a(motX, motY, motZ).func_72314_b(1.0, 1.0, 1.0);
                                List<Entity> candidates = mc.field_71441_e.func_72839_b(mc.func_175606_aa(), axisAlignedBB6);
                                Entity bestEntity = null;
                                Vec3 bestHitVec = null;
                                EnumFacing bestHitFace = null;
                                AxisAlignedBB axisAlignedBB7 = null;
                                double bestDistSq = blockDistSq;
                                for (Entity en : candidates) {
                                    if (en instanceof EntityLivingBase && !(en instanceof EntityArmorStand) && en.func_70067_L() && ((EntityLivingBase) en).field_70725_aQ == 0 && (!(en instanceof EntityPlayer) || !AntiBot.isBot(en))) {
                                        AxisAlignedBB axisAlignedBB8 = en.func_174813_aQ();
                                        AxisAlignedBB axisAlignedBB9 = this.expandEntityCollisionBox(axisAlignedBB8);
                                        MovingObjectPosition mop = axisAlignedBB9.func_72327_a(start, clampedEnd);
                                        if (mop != null) {
                                            double dSq = start.func_72436_e(mop.field_72307_f);
                                            if (dSq + 1.0E-7 < bestDistSq) {
                                                bestDistSq = dSq;
                                                bestEntity = en;
                                                bestHitVec = mop.field_72307_f;
                                                bestHitFace = mop.field_178784_b;
                                                axisAlignedBB7 = axisAlignedBB9;
                                            }
                                        }
                                    }
                                }
                                if (bestEntity != null) {
                                    double hitT = Math.sqrt(bestDistSq) / Math.sqrt(motX * motX + motY * motY + motZ * motZ);
                                    hitT = Math.max(0.0, Math.min(1.0, hitT));
                                    int subCount = (int) Math.ceil(hitT * 4.0);
                                    for (int s = 0; s < subCount; s++) {
                                        double t = s / 4.0;
                                        renderPoints.add(new double[] { posX + motX * t - rm.field_78730_l, posY + motY * t - rm.field_78731_m, posZ + motZ * t - rm.field_78728_n });
                                    }
                                    renderPoints.add(new double[] { bestHitVec.field_72450_a - rm.field_78730_l, bestHitVec.field_72448_b - rm.field_78731_m, bestHitVec.field_72449_c - rm.field_78728_n });
                                    hitEntity = bestEntity;
                                    axisAlignedBB5 = axisAlignedBB7;
                                    hitType = 1;
                                    hitPos = bestHitVec;
                                    hitFace = bestHitFace;
                                    break;
                                }
                                if (blockMop != null) {
                                    Vec3 hitVec = blockMop.field_72307_f;
                                    int side = blockMop.field_178784_b.func_176745_a();
                                    hitType = side != 0 && side != 1 ? 2 : 3;
                                    hitPos = hitVec;
                                    hitFace = blockMop.field_178784_b;
                                    double segLenSq = motX * motX + motY * motY + motZ * motZ;
                                    double doubleValue = hitVec.field_72450_a - posX;
                                    double hitDy = hitVec.field_72448_b - posY;
                                    double hitDz = hitVec.field_72449_c - posZ;
                                    double hitT = segLenSq > 0.0 ? Math.sqrt((doubleValue * doubleValue + hitDy * hitDy + hitDz * hitDz) / segLenSq) : 0.0;
                                    hitT = Math.max(0.0, Math.min(1.0, hitT));
                                    int subCount = (int) Math.ceil(hitT * 4.0);
                                    for (int s = 0; s < subCount; s++) {
                                        double t = s / 4.0;
                                        renderPoints.add(new double[] { posX + motX * t - rm.field_78730_l, posY + motY * t - rm.field_78731_m, posZ + motZ * t - rm.field_78728_n });
                                    }
                                    renderPoints.add(new double[] { hitVec.field_72450_a - rm.field_78730_l, hitVec.field_72448_b - rm.field_78731_m, hitVec.field_72449_c - rm.field_78728_n });
                                    hitBlock = blockMop;
                                    break;
                                }
                                for (int s = 0; s < 4; s++) {
                                    double t = s / 4.0;
                                    renderPoints.add(new double[] { posX + motX * t - rm.field_78730_l, posY + motY * t - rm.field_78731_m, posZ + motZ * t - rm.field_78728_n });
                                }
                                posX = nextX;
                                posY = nextY;
                                posZ = nextZ;
                                terminalPos = new Vec3(nextX, nextY, nextZ);
                                mot[0] = motX;
                                mot[1] = motY;
                                mot[2] = motZ;
                                this.tickPostMoveVelocity(nextX, nextY, nextZ, props, fluidState, mot);
                                motX = mot[0];
                                motY = mot[1];
                                motZ = mot[2];
                                if (nextY < -64.0) {
                                    break;
                                }
                            }
                            if (hitPos == null && props.physicsModel == Trajectories.PhysicsModel.FISH_HOOK && terminalPos != null) {
                                hitPos = terminalPos;
                            }
                            int color;
                            switch(hitType) {
                                case 1:
                                    color = this.enemyColor.getColor();
                                    break;
                                case 2:
                                    color = this.wallColor.getColor();
                                    break;
                                case 3:
                                    color = this.groundColor.getColor();
                                    break;
                                default:
                                    color = this.defaultColor.getColor();
                            }
                            float r = (color >> 16 & 0xFF) / 255.0F;
                            float g = (color >> 8 & 0xFF) / 255.0F;
                            float b = (color & 0xFF) / 255.0F;
                            GL11.glPushMatrix();
                            GL11.glEnable(2848);
                            GL11.glBlendFunc(770, 771);
                            GL11.glEnable(3042);
                            GL11.glDisable(3553);
                            GL11.glDisable(2929);
                            GL11.glDepthMask(false);
                            float lineW = (float) this.lineThickness.getInput();
                            GL11.glColor4f(r, g, b, 1.0F);
                            GL11.glLineWidth(lineW);
                            GL11.glBegin(3);
                            boolean first = true;
                            for (double[] pt : this.clipRenderPoints(renderPoints, hiddenRenderDistance)) {
                                if (first && this.shortenLine.isToggled()) {
                                    first = false;
                                } else {
                                    first = false;
                                    GL11.glVertex3d(pt[0], pt[1], pt[2]);
                                }
                            }
                            GL11.glEnd();
                            if (hitEntity != null && this.highlightEntities.isToggled() && axisAlignedBB5 != null) {
                                double ex = hitEntity.field_70142_S + (hitEntity.field_70165_t - hitEntity.field_70142_S) * partialTicks;
                                double ey = hitEntity.field_70137_T + (hitEntity.field_70163_u - hitEntity.field_70137_T) * partialTicks;
                                double ez = hitEntity.field_70136_U + (hitEntity.field_70161_v - hitEntity.field_70136_U) * partialTicks;
                                AxisAlignedBB axisAlignedBB10 = new AxisAlignedBB(axisAlignedBB5.field_72340_a - hitEntity.field_70165_t + ex, axisAlignedBB5.field_72338_b - hitEntity.field_70163_u + ey, axisAlignedBB5.field_72339_c - hitEntity.field_70161_v + ez, axisAlignedBB5.field_72336_d - hitEntity.field_70165_t + ex, axisAlignedBB5.field_72337_e - hitEntity.field_70163_u + ey, axisAlignedBB5.field_72334_f - hitEntity.field_70161_v + ez);
                                GL11.glColor4f(r, g, b, 1.0F);
                                RenderUtils.drawOutlinedBox(axisAlignedBB10, rm.field_78730_l, rm.field_78731_m, rm.field_78728_n);
                            } else if (hitBlock != null && !this.showLanding.isToggled()) {
                                BlockPos bpos = hitBlock.func_178782_a();
                                AxisAlignedBB blockSelectionBox = BlockUtils.getBlockSelectionBox(bpos);
                                if (blockSelectionBox != null) {
                                    GL11.glColor4f(r, g, b, 1.0F);
                                    RenderUtils.drawOutlinedBox(blockSelectionBox, rm.field_78730_l, rm.field_78731_m, rm.field_78728_n);
                                }
                            }
                            if (this.showLanding.isToggled() && hitPos != null) {
                                this.renderLandingIndicator(hitPos, hitFace, rm.field_78730_l, rm.field_78731_m, rm.field_78728_n, r, g, b, hitType);
                            }
                            GL11.glDisable(3042);
                            GL11.glEnable(3553);
                            GL11.glEnable(2929);
                            GL11.glDepthMask(true);
                            GL11.glDisable(2848);
                            GL11.glPopMatrix();
                        }
                    }
                }
            }
        }
    }

    private void renderLandingIndicator(Vec3 hitPos, EnumFacing hitFace, double camX, double camY, double camZ, float r, float g, float b, int hitType) {
        if (this.target.getInput() == 0.0) {
            ProjectileTrajectoryRenderer.renderCrossIndicator(hitPos, hitFace, camX, camY, camZ, r, g, b, this.autoScale.isToggled(), (float) this.lineThickness.getInput());
        } else {
            double boxSize = hitType == 1 ? 0.4 : 0.2;
            AxisAlignedBB axisAlignedBB11 = new AxisAlignedBB(hitPos.field_72450_a - boxSize, hitPos.field_72448_b - boxSize, hitPos.field_72449_c - boxSize, hitPos.field_72450_a + boxSize, hitPos.field_72448_b + boxSize, hitPos.field_72449_c + boxSize);
            GL11.glLineWidth(2.0F);
            GL11.glColor4f(r, g, b, 1.0F);
            RenderUtils.drawOutlinedBox(axisAlignedBB11, camX, camY, camZ);
            AxisAlignedBB axisAlignedBB12 = axisAlignedBB11.func_72317_d(-camX, -camY, -camZ);
            GL11.glColor4f(r, g, b, 0.3F);
            RenderUtils.drawBoundingBox(axisAlignedBB12, r, g, b, 0.3F);
            double x = hitPos.field_72450_a - camX;
            double y = hitPos.field_72448_b - camY;
            double z = hitPos.field_72449_c - camZ;
            double crossSize = hitType == 1 ? 0.5 : 0.3;
            GL11.glLineWidth(1.5F);
            GL11.glColor4f(r, g, b, 1.0F);
            GL11.glBegin(1);
            GL11.glVertex3d(x - crossSize, y, z);
            GL11.glVertex3d(x + crossSize, y, z);
            GL11.glVertex3d(x, y - crossSize, z);
            GL11.glVertex3d(x, y + crossSize, z);
            GL11.glVertex3d(x, y, z - crossSize);
            GL11.glVertex3d(x, y, z + crossSize);
            GL11.glEnd();
        }
    }

    private static final class BlockCollisionResult {

        final MovingObjectPosition hit;

        final double distanceSq;

        BlockCollisionResult(MovingObjectPosition hit, double distanceSq) {
            this.hit = hit;
            this.distanceSq = distanceSq;
        }
    }

    private static final class FluidState {

        final boolean inWater;

        final boolean inLava;

        final Vec3 flowDirection;

        FluidState(boolean inWater, boolean inLava, Vec3 flowDirection) {
            this.inWater = inWater;
            this.inLava = inLava;
            this.flowDirection = flowDirection;
        }
    }

    private static enum PhysicsModel {

        ARROW, THROWABLE, FISH_HOOK
    }

    private static final class TrajectoryProps {

        final Trajectories.PhysicsModel physicsModel;

        final double gravity;

        final double drag;

        final double dragInWater;

        final double hitboxRadius;

        final double width;

        final double height;

        final double initialVelocity;

        final boolean requiresInitialTickCorrection;

        final boolean ignoreBlockWithoutBoundingBox;

        TrajectoryProps(Trajectories.PhysicsModel physicsModel, double gravity, double drag, double dragInWater, double hitboxRadius, double width, double height, double initialVelocity, boolean requiresInitialTickCorrection, boolean enabled) {
            this.physicsModel = physicsModel;
            this.gravity = gravity;
            this.drag = drag;
            this.dragInWater = dragInWater;
            this.hitboxRadius = hitboxRadius;
            this.width = width;
            this.height = height;
            this.initialVelocity = initialVelocity;
            this.requiresInitialTickCorrection = requiresInitialTickCorrection;
            this.ignoreBlockWithoutBoundingBox = enabled;
        }
    }
}
