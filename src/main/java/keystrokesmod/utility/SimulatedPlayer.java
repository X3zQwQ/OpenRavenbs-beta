package keystrokesmod.utility;

import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import keystrokesmod.mixin.impl.accessor.IAccessorEntity;
import keystrokesmod.mixin.impl.accessor.IAccessorEntityLivingBase;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.movement.NoSlow;
import net.minecraft.block.Block;
import net.minecraft.block.BlockFence;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.BlockLadder;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.BlockSlime;
import net.minecraft.block.BlockSoulSand;
import net.minecraft.block.BlockWall;
import net.minecraft.block.BlockWeb;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.EnchantmentProtection;
import net.minecraft.entity.Entity;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.BaseAttributeMap;
import net.minecraft.entity.ai.attributes.IAttribute;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.ai.attributes.ServersideAttributeMap;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.player.PlayerCapabilities;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos.MutableBlockPos;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EntitySelectors;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.FoodStats;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovementInput;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import org.apache.commons.lang3.tuple.Pair;

public class SimulatedPlayer {

    private static final Minecraft mc = Minecraft.func_71410_x();

    private final EntityPlayerSP player;

    private final Map<BlockPos, IBlockState> blockOverrides = new HashMap<>();

    private float itemInputMultiplier = 1.0F;

    public AxisAlignedBB box;

    public final MovementInput movementInput;

    private int jumpTicks;

    public double motionZ;

    public double motionY;

    public double motionX;

    private boolean inWater;

    public boolean onGround;

    private boolean isAirBorne;

    public float rotationYaw;

    public float rotationPitch;

    private double posX;

    private double posY;

    private double posZ;

    private final PlayerCapabilities capabilities;

    private final Entity ridingEntity;

    private float jumpMovementFactor;

    private final World worldObj;

    public boolean isCollidedHorizontally;

    private boolean isCollidedVertically;

    private final WorldBorder worldBorder;

    private final IChunkProvider chunkProvider;

    private boolean isOutsideBorder;

    private final Entity riddenByEntity;

    private BaseAttributeMap attributeMap;

    private final boolean isSpectator;

    public float fallDistance;

    private final float stepHeight;

    private boolean isCollided;

    private int fire;

    private float distanceWalkedModified;

    private float distanceWalkedOnStepModified;

    private int nextStepDistance;

    public final float height;

    private final float width;

    private final int fireResistance;

    private boolean isInWeb;

    private boolean noClip;

    private boolean isSprinting;

    private boolean sprintRequested;

    private final FoodStats foodStats;

    private float moveForward = 0.0F;

    private float moveStrafing = 0.0F;

    private boolean isJumping = false;

    public boolean safeWalk = false;

    private static final float SPEED_IN_AIR = 0.02F;

    public SimulatedPlayer(EntityPlayerSP player, AxisAlignedBB box, MovementInput movementInput, int jumpTicks, double motionZ, double motionY, double motionX, boolean inWater, boolean onGround, boolean isAirBorne, float rotationYaw, float rotationPitch, double posX, double posY, double posZ, PlayerCapabilities capabilities, Entity ridingEntity, float jumpMovementFactor, World worldObj, boolean isCollidedHorizontally, boolean isCollidedVertically, WorldBorder worldBorder, IChunkProvider chunkProvider, boolean isOutsideBorder, Entity riddenByEntity, BaseAttributeMap attributeMap, boolean isSpectator, float fallDistance, float stepHeight, boolean isCollided, int fire, float distanceWalkedModified, float distanceWalkedOnStepModified, int nextStepDistance, float height, float width, int fireResistance, boolean isInWeb, boolean noClip, boolean isSprinting, FoodStats foodStats) {
        this.player = player;
        this.box = box;
        this.movementInput = movementInput;
        this.jumpTicks = jumpTicks;
        this.motionZ = motionZ;
        this.motionY = motionY;
        this.motionX = motionX;
        this.inWater = inWater;
        this.onGround = onGround;
        this.isAirBorne = isAirBorne;
        this.rotationYaw = rotationYaw;
        this.rotationPitch = rotationPitch;
        this.posX = posX;
        this.posY = posY;
        this.posZ = posZ;
        this.capabilities = capabilities;
        this.ridingEntity = ridingEntity;
        this.jumpMovementFactor = jumpMovementFactor;
        this.worldObj = worldObj;
        this.isCollidedHorizontally = isCollidedHorizontally;
        this.isCollidedVertically = isCollidedVertically;
        this.worldBorder = worldBorder;
        this.chunkProvider = chunkProvider;
        this.isOutsideBorder = isOutsideBorder;
        this.riddenByEntity = riddenByEntity;
        this.attributeMap = attributeMap;
        this.isSpectator = isSpectator;
        this.fallDistance = fallDistance;
        this.stepHeight = stepHeight;
        this.isCollided = isCollided;
        this.fire = fire;
        this.distanceWalkedModified = distanceWalkedModified;
        this.distanceWalkedOnStepModified = distanceWalkedOnStepModified;
        this.nextStepDistance = nextStepDistance;
        this.height = height;
        this.width = width;
        this.fireResistance = fireResistance;
        this.isInWeb = isInWeb;
        this.noClip = noClip;
        this.isSprinting = isSprinting;
        this.sprintRequested = isSprinting;
        this.foodStats = foodStats;
    }

    public Vec3 getPos() {
        return new Vec3(this.posX, this.posY, this.posZ);
    }

    public static SimulatedPlayer fromClientPlayer(MovementInput input) {
        EntityPlayerSP player = mc.field_71439_g;
        PlayerCapabilities capabilities = createCapabilitiesCopy(player);
        FoodStats foodStats = createFoodStatsCopy(player);
        MovementInput movementInput = new MovementInput();
        movementInput.field_78901_c = input.field_78901_c;
        movementInput.field_78900_b = input.field_78900_b;
        movementInput.field_78902_a = input.field_78902_a;
        movementInput.field_78899_d = input.field_78899_d;
        return new SimulatedPlayer(player, player.func_174813_aQ(), movementInput, ((IAccessorEntityLivingBase) player).getJumpTicks(), player.field_70179_y, player.field_70181_x, player.field_70159_w, player.func_70090_H(), player.field_70122_E, player.field_70160_al, player.field_70177_z, player.field_70125_A, player.field_70165_t, player.field_70163_u, player.field_70161_v, capabilities, player.field_70154_o, player.field_70747_aH, player.field_70170_p, player.field_70123_F, player.field_70124_G, player.field_70170_p.func_175723_af(), player.field_70170_p.func_72863_F(), player.func_174832_aS(), player.field_70153_n, player.func_110140_aT(), player.func_175149_v(), player.field_70143_R, player.field_70138_W, player.field_70132_H, ((IAccessorEntity) player).getFire(), player.field_70140_Q, player.field_82151_R, ((IAccessorEntity) player).getNextStepDistance(), player.field_70131_O, player.field_70130_N, player.field_70174_ab, ((IAccessorEntity) player).getIsInWeb(), player.field_70145_X, player.func_70051_ag(), foodStats);
    }

    private static FoodStats createFoodStatsCopy(EntityPlayerSP player) {
        NBTTagCompound foodStatsNBT = new NBTTagCompound();
        FoodStats foodStats = new FoodStats();
        player.func_71024_bL().func_75117_b(foodStatsNBT);
        foodStats.func_75112_a(foodStatsNBT);
        return foodStats;
    }

    private static PlayerCapabilities createCapabilitiesCopy(EntityPlayerSP player) {
        NBTTagCompound capabilitiesNBT = new NBTTagCompound();
        PlayerCapabilities capabilities = new PlayerCapabilities();
        player.field_71075_bZ.func_75091_a(capabilitiesNBT);
        capabilities.func_75095_b(capabilitiesNBT);
        capabilities.field_75100_b = false;
        capabilities.field_75101_c = false;
        return capabilities;
    }

    private boolean onEntityUpdate() {
        this.handleWaterMovement();
        if (this.worldObj.field_72995_K) {
            this.fire = 0;
        } else if (this.fire > 0) {
            this.fire--;
        }
        if (this.isInLava()) {
            this.setOnFireFromLava();
            this.fallDistance *= 0.5F;
        }
        return !(this.posY < -64.0);
    }

    public void tick() {
        if (this.onEntityUpdate() && !this.player.func_70115_ae()) {
            this.playerUpdate(false);
            this.clientPlayerLivingUpdate();
            this.playerUpdate(true);
        }
    }

    private void clientPlayerLivingUpdate() {
        this.pushOutOfBlocks(this.posX - this.width * 0.35, this.getEntityBoundingBox().field_72338_b + 0.5, this.posZ + this.width * 0.35);
        this.pushOutOfBlocks(this.posX - this.width * 0.35, this.getEntityBoundingBox().field_72338_b + 0.5, this.posZ - this.width * 0.35);
        this.pushOutOfBlocks(this.posX + this.width * 0.35, this.getEntityBoundingBox().field_72338_b + 0.5, this.posZ - this.width * 0.35);
        this.pushOutOfBlocks(this.posX + this.width * 0.35, this.getEntityBoundingBox().field_72338_b + 0.5, this.posZ + this.width * 0.35);
        boolean useItemBlocksSprint = ModuleManager.noSlow == null || !ModuleManager.noSlow.isEnabled() || NoSlow.slowed.getInput() == 80.0;
        this.itemInputMultiplier = this.player.func_71039_bw() && this.ridingEntity == null ? NoSlow.getSlowed() : 1.0F;
        int foodLevel = this.foodStats.func_75116_a();
        boolean flag3 = foodLevel > 6 || this.capabilities.field_75101_c;
        boolean blind = this.isPotionActive(Potion.field_76440_q);
        if (this.sprintRequested && !this.movementInput.field_78899_d && !this.isCollidedHorizontally && this.movementInput.field_78900_b * this.itemInputMultiplier >= 0.8F && flag3 && (!this.player.func_71039_bw() || !useItemBlocksSprint) && !blind) {
            this.setSprinting(true);
        }
        if (this.movementInput.field_78899_d) {
            this.setSprinting(false);
        }
        if (this.isSprinting() && (this.movementInput.field_78900_b * this.itemInputMultiplier < 0.8F || this.movementInput.field_78899_d || this.isCollidedHorizontally || foodLevel <= 6 && !this.capabilities.field_75101_c)) {
            this.setSprinting(false);
        }
        if (this.capabilities.field_75101_c && mc.field_71442_b.func_178887_k() && !this.capabilities.field_75100_b) {
            this.capabilities.field_75100_b = true;
        }
        if (this.capabilities.field_75100_b) {
            if (this.movementInput.field_78899_d) {
                this.motionY = this.motionY - this.capabilities.func_75093_a() * 3.0F;
            }
            if (this.movementInput.field_78901_c) {
                this.motionY = this.motionY + this.capabilities.func_75093_a() * 3.0F;
            }
        }
        this.livingEntityUpdate();
    }

    private void playerUpdate(boolean post) {
        if (!post) {
            this.noClip = this.isSpectator;
            if (this.isSpectator) {
                this.onGround = false;
            }
        } else {
            this.clampPositionFromEntityPlayer();
        }
    }

    private void livingEntityUpdate() {
        if (this.jumpTicks > 0) {
            this.jumpTicks--;
        }
        if (Math.abs(this.motionX) < 0.005) {
            this.motionX = 0.0;
        }
        if (Math.abs(this.motionY) < 0.005) {
            this.motionY = 0.0;
        }
        if (Math.abs(this.motionZ) < 0.005) {
            this.motionZ = 0.0;
        }
        if (this.isMovementBlocked()) {
            this.isJumping = false;
            this.moveStrafing = 0.0F;
            this.moveForward = 0.0F;
        } else if (this.isServerWorld()) {
            this.updateLivingEntityInput();
        }
        if (this.isJumping) {
            if (this.isInWater() || this.isInLava()) {
                this.updateAITick();
            } else if (this.onGround && this.jumpTicks == 0) {
                this.jump();
            }
        } else {
            this.jumpTicks = 0;
        }
        this.moveStrafing *= 0.98F;
        this.moveForward *= 0.98F;
        this.playerSideMoveEntityWithHeading(this.moveStrafing, this.moveForward);
        this.jumpMovementFactor = 0.02F;
        if (this.isSprinting()) {
            this.jumpMovementFactor = (float) (this.jumpMovementFactor + 0.005999999865889549);
        }
        if (this.onGround && this.capabilities.field_75100_b && !this.isSpectator) {
            this.capabilities.field_75100_b = false;
        }
    }

    private void clampPositionFromEntityPlayer() {
        double d3 = MathHelper.func_151237_a(this.posX, -2.9999999E7, 2.9999999E7);
        double d4 = MathHelper.func_151237_a(this.posZ, -2.9999999E7, 2.9999999E7);
        if (d3 != this.posX || d4 != this.posZ) {
            this.setPosition(d3, this.posY, d4);
        }
    }

    private void setPosition(double x, double y, double z) {
        this.posX = x;
        this.posY = y;
        this.posZ = z;
        float f = this.width / 2.0F;
        float f1 = this.height;
        this.setEntityBoundingBox(new AxisAlignedBB(x - f, y, z - f, x + f, y + f1, z + f));
    }

    private void setSprinting(boolean state) {
        this.isSprinting = state;
    }

    public void setSprintRequested(boolean sprintRequested) {
        this.sprintRequested = sprintRequested;
    }

    public float getAirAcceleration() {
        return this.jumpMovementFactor;
    }

    public void resetSimulationState(double x, double y, double z, boolean onGround) {
        this.blockOverrides.clear();
        this.setPosition(x, y, z);
        this.motionX = 0.0;
        this.motionY = 0.0;
        this.motionZ = 0.0;
        this.onGround = onGround;
        this.isAirBorne = !onGround;
        this.isCollided = false;
        this.isCollidedHorizontally = false;
        this.isCollidedVertically = onGround;
        this.fallDistance = 0.0F;
        this.jumpTicks = 0;
        this.inWater = false;
        this.isInWeb = false;
        this.isSprinting = false;
        this.sprintRequested = false;
        this.movementInput.field_78900_b = 0.0F;
        this.movementInput.field_78902_a = 0.0F;
        this.movementInput.field_78901_c = false;
        this.movementInput.field_78899_d = false;
    }

    private boolean pushOutOfBlocks(double x, double y, double z) {
        if (this.noClip) {
            return false;
        } else {
            BlockPos blockPos = new BlockPos(x, y, z);
            double d0 = x - blockPos.func_177958_n();
            double d1 = z - blockPos.func_177952_p();
            int entHeight = (int) Math.ceil(this.height);
            boolean inTranslucentBlock = !this.isHeadspaceFree(blockPos, entHeight);
            if (inTranslucentBlock) {
                int i = -1;
                double d2 = 9999.0;
                if (this.isHeadspaceFree(blockPos.func_177976_e(), entHeight) && d0 < d2) {
                    d2 = d0;
                    i = 0;
                }
                if (this.isHeadspaceFree(blockPos.func_177974_f(), entHeight) && 1.0 - d0 < d2) {
                    d2 = 1.0 - d0;
                    i = 1;
                }
                if (this.isHeadspaceFree(blockPos.func_177978_c(), entHeight) && d1 < d2) {
                    d2 = d1;
                    i = 4;
                }
                if (this.isHeadspaceFree(blockPos.func_177968_d(), entHeight) && 1.0 - d1 < d2) {
                    i = 5;
                }
                float f = 0.1F;
                if (i == 0) {
                    this.motionX = -f;
                }
                if (i == 1) {
                    this.motionX = f;
                }
                if (i == 4) {
                    this.motionZ = -f;
                }
                if (i == 5) {
                    this.motionZ = f;
                }
            }
            return false;
        }
    }

    private boolean isHeadspaceFree(BlockPos pos, int height) {
        for (int y = 0; y < height; y++) {
            if (!this.isOpenBlockSpace(pos.func_177982_a(0, y, 0))) {
                return false;
            }
        }
        return true;
    }

    private boolean isOpenBlockSpace(BlockPos pos) {
        return !this.getBlockState(pos).func_177230_c().func_149721_r();
    }

    private void playerSideMoveEntityWithHeading(float moveStrafing, float moveForward) {
        if (this.capabilities.field_75100_b && this.ridingEntity == null) {
            double d3 = this.motionY;
            float f = this.jumpMovementFactor;
            this.jumpMovementFactor = this.capabilities.func_75093_a() * (this.isSprinting() ? 2 : 1);
            this.livingEntitySideMoveEntityWithHeading(moveStrafing, moveForward);
            this.motionY = d3 * 0.6;
            this.jumpMovementFactor = f;
        } else {
            this.livingEntitySideMoveEntityWithHeading(moveStrafing, moveForward);
        }
    }

    private void livingEntitySideMoveEntityWithHeading(float strafing, float forwards) {
        if (this.isServerWorld()) {
            if (this.isInWater() && !this.capabilities.field_75100_b) {
                double d0 = this.posY;
                float f5 = 0.8F;
                float f6 = 0.02F;
                float f3 = EnchantmentHelper.func_180318_b(this.player);
                if (f3 > 3.0F) {
                    f3 = 3.0F;
                }
                if (!this.onGround) {
                    f3 *= 0.5F;
                }
                if (f3 > 0.0F) {
                    f5 += (0.54600006F - f5) * f3 / 3.0F;
                    f6 += (this.getAIMoveSpeed() - f6) * f3 / 3.0F;
                }
                this.moveFlying(strafing, forwards, f6);
                this.moveEntity(this.motionX, this.motionY, this.motionZ);
                this.motionX *= f5;
                this.motionY *= 0.8F;
                this.motionZ *= f5;
                this.motionY -= 0.02;
                if (this.isCollidedHorizontally && this.isOffsetPositionInLiquid(this.motionX, this.motionY + 0.6F - this.posY + d0, this.motionZ)) {
                    this.motionY = 0.3F;
                }
            } else if (this.isInLava() && !this.capabilities.field_75100_b) {
                double posY2 = this.posY;
                this.moveFlying(strafing, forwards, 0.02F);
                this.moveEntity(this.motionX, this.motionY, this.motionZ);
                this.motionX *= 0.5;
                this.motionY *= 0.5;
                this.motionZ *= 0.5;
                this.motionY -= 0.02;
                if (this.isCollidedHorizontally && this.isOffsetPositionInLiquid(this.motionX, this.motionY + 0.6F - this.posY + posY2, this.motionZ)) {
                    this.motionY = 0.3F;
                }
            } else {
                float f4 = 0.91F;
                if (this.onGround) {
                    f4 = this.getBlockState(new BlockPos(MathHelper.func_76128_c(this.posX), MathHelper.func_76128_c(this.getEntityBoundingBox().field_72338_b) - 1, MathHelper.func_76128_c(this.posZ))).func_177230_c().field_149765_K * 0.91F;
                }
                float f = 0.16277136F / (f4 * f4 * f4);
                float floatValue = this.onGround ? this.getAIMoveSpeed() * f : this.jumpMovementFactor;
                this.moveFlying(strafing, forwards, floatValue);
                f4 = 0.91F;
                if (this.onGround) {
                    f4 = this.getBlockState(new BlockPos(MathHelper.func_76128_c(this.posX), MathHelper.func_76128_c(this.getEntityBoundingBox().field_72338_b) - 1, MathHelper.func_76128_c(this.posZ))).func_177230_c().field_149765_K * 0.91F;
                }
                if (this.isOnLadder()) {
                    float floatValue2 = 0.15F;
                    this.motionX = MathHelper.func_151237_a(this.motionX, -floatValue2, floatValue2);
                    this.motionZ = MathHelper.func_151237_a(this.motionZ, -floatValue2, floatValue2);
                    this.fallDistance = 0.0F;
                    if (this.motionY < -0.15) {
                        this.motionY = -0.15;
                    }
                    boolean flag = this.isSneaking();
                    if (flag && this.motionY < 0.0) {
                        this.motionY = 0.0;
                    }
                }
                this.moveEntity(this.motionX, this.motionY, this.motionZ);
                if (this.isCollidedHorizontally && this.isOnLadder()) {
                    this.motionY = 0.2;
                }
                if (this.worldObj.field_72995_K && (!this.worldObj.func_175667_e(new BlockPos(this.posX, 0.0, this.posZ)) || !this.worldObj.func_175726_f(new BlockPos(this.posX, 0.0, this.posZ)).func_177410_o())) {
                    this.motionY = this.posY > 0.0 ? -0.1 : 0.0;
                } else {
                    this.motionY -= 0.08;
                }
                this.motionY *= 0.98F;
                this.motionX *= f4;
                this.motionZ *= f4;
            }
        }
    }

    public void moveEntity(double xMotion, double yMotion, double zMotion) {
        double velocityX = xMotion;
        double velocityY = yMotion;
        double velocityZ = zMotion;
        if (this.noClip) {
            this.setEntityBoundingBox(this.getEntityBoundingBox().func_72317_d(xMotion, yMotion, zMotion));
            this.resetPositionToBB();
        } else {
            double d0 = this.posX;
            double d1 = this.posY;
            double d2 = this.posZ;
            if (this.isInWeb) {
                this.isInWeb = false;
                velocityX = xMotion * 0.25;
                velocityY = yMotion * 0.05F;
                velocityZ = zMotion * 0.25;
                this.motionX = 0.0;
                this.motionY = 0.0;
                this.motionZ = 0.0;
            }
            double d3 = velocityX;
            double d4 = velocityY;
            double d5 = velocityZ;
            boolean flag = this.onGround && (this.isSneaking() || this.safeWalk);
            if (flag) {
                d3 = (Double) this.checkForCollision(this, velocityX, velocityZ).getLeft();
                d5 = (Double) this.checkForCollision(this, velocityX, velocityZ).getRight();
            }
            List<AxisAlignedBB> list1 = this.getCollidingBoundingBoxes(this.getEntityBoundingBox().func_72321_a(velocityX, velocityY, velocityZ));
            AxisAlignedBB axisalignedbb = this.getEntityBoundingBox();
            for (AxisAlignedBB axisalignedbb1 : list1) {
                velocityY = axisalignedbb1.func_72323_b(this.getEntityBoundingBox(), velocityY);
            }
            this.setEntityBoundingBox(this.getEntityBoundingBox().func_72317_d(0.0, velocityY, 0.0));
            boolean flag1 = this.onGround || d4 != velocityY && d4 < 0.0;
            for (AxisAlignedBB axisalignedbb2 : list1) {
                velocityX = axisalignedbb2.func_72316_a(this.getEntityBoundingBox(), velocityX);
            }
            this.setEntityBoundingBox(this.getEntityBoundingBox().func_72317_d(velocityX, 0.0, 0.0));
            for (AxisAlignedBB axisalignedbb13 : list1) {
                velocityZ = axisalignedbb13.func_72322_c(this.getEntityBoundingBox(), velocityZ);
            }
            this.setEntityBoundingBox(this.getEntityBoundingBox().func_72317_d(0.0, 0.0, velocityZ));
            if (this.stepHeight > 0.0F && flag1 && (d3 != velocityX || d5 != velocityZ)) {
                double d11 = velocityX;
                double d7 = velocityY;
                double d8 = velocityZ;
                AxisAlignedBB axisalignedbb3 = this.getEntityBoundingBox();
                this.setEntityBoundingBox(axisalignedbb);
                velocityY = this.stepHeight;
                List<AxisAlignedBB> list = this.getCollidingBoundingBoxes(this.getEntityBoundingBox().func_72321_a(d3, velocityY, d5));
                AxisAlignedBB axisalignedbb4 = this.getEntityBoundingBox();
                AxisAlignedBB axisalignedbb5 = axisalignedbb4.func_72321_a(d3, 0.0, d5);
                double d9 = velocityY;
                for (AxisAlignedBB axisalignedbb6 : list) {
                    d9 = axisalignedbb6.func_72323_b(axisalignedbb5, d9);
                }
                axisalignedbb4 = axisalignedbb4.func_72317_d(0.0, d9, 0.0);
                double d15 = d3;
                for (AxisAlignedBB axisalignedbb7 : list) {
                    d15 = axisalignedbb7.func_72316_a(axisalignedbb4, d15);
                }
                axisalignedbb4 = axisalignedbb4.func_72317_d(d15, 0.0, 0.0);
                double d16 = d5;
                for (AxisAlignedBB axisalignedbb8 : list) {
                    d16 = axisalignedbb8.func_72322_c(axisalignedbb4, d16);
                }
                axisalignedbb4 = axisalignedbb4.func_72317_d(0.0, 0.0, d16);
                AxisAlignedBB axisalignedbb14 = this.getEntityBoundingBox();
                double d17 = velocityY;
                for (AxisAlignedBB axisalignedbb9 : list) {
                    d17 = axisalignedbb9.func_72323_b(axisalignedbb14, d17);
                }
                axisalignedbb14 = axisalignedbb14.func_72317_d(0.0, d17, 0.0);
                double d18 = d3;
                for (AxisAlignedBB axisalignedbb10 : list) {
                    d18 = axisalignedbb10.func_72316_a(axisalignedbb14, d18);
                }
                axisalignedbb14 = axisalignedbb14.func_72317_d(d18, 0.0, 0.0);
                double d19 = d5;
                for (AxisAlignedBB axisalignedbb11 : list) {
                    d19 = axisalignedbb11.func_72322_c(axisalignedbb14, d19);
                }
                axisalignedbb14 = axisalignedbb14.func_72317_d(0.0, 0.0, d19);
                double d20 = d15 * d15 + d16 * d16;
                double d10 = d18 * d18 + d19 * d19;
                if (d20 > d10) {
                    velocityX = d15;
                    velocityZ = d16;
                    velocityY = -d9;
                    this.setEntityBoundingBox(axisalignedbb4);
                } else {
                    velocityX = d18;
                    velocityZ = d19;
                    velocityY = -d17;
                    this.setEntityBoundingBox(axisalignedbb14);
                }
                for (AxisAlignedBB axisalignedbb12 : list) {
                    velocityY = axisalignedbb12.func_72323_b(this.getEntityBoundingBox(), velocityY);
                }
                this.setEntityBoundingBox(this.getEntityBoundingBox().func_72317_d(0.0, velocityY, 0.0));
                if (d11 * d11 + d8 * d8 >= velocityX * velocityX + velocityZ * velocityZ) {
                    velocityX = d11;
                    velocityY = d7;
                    velocityZ = d8;
                    this.setEntityBoundingBox(axisalignedbb3);
                }
            }
            this.resetPositionToBB();
            this.isCollidedHorizontally = d3 != velocityX || d5 != velocityZ;
            this.isCollidedVertically = d4 != velocityY;
            this.onGround = this.isCollidedVertically && d4 < 0.0;
            this.isCollided = this.isCollidedHorizontally || this.isCollidedVertically;
            int i = MathHelper.func_76128_c(this.posX);
            int j = MathHelper.func_76128_c(this.posY - 0.2F);
            int k = MathHelper.func_76128_c(this.posZ);
            BlockPos blockPos = new BlockPos(i, j, k);
            Block block1 = this.getBlockState(blockPos).func_177230_c();
            if (block1.func_149688_o() == Material.field_151579_a) {
                Block block = this.getBlockState(blockPos.func_177977_b()).func_177230_c();
                if (block instanceof BlockFence || block instanceof BlockWall || block instanceof BlockFenceGate) {
                    block1 = block;
                }
            }
            this.updateFallState(velocityY, this.onGround);
            if (d3 != velocityX) {
                this.motionX = 0.0;
            }
            if (d5 != velocityZ) {
                this.motionZ = 0.0;
            }
            if (d4 != velocityY) {
                this.onLanded(block1);
            }
            if (this.canTriggerWalking() && !flag && this.ridingEntity == null) {
                double d12 = this.posX - d0;
                double d13 = this.posY - d1;
                double d14 = this.posZ - d2;
                if (block1 != Blocks.field_150468_ap) {
                    d13 = 0.0;
                }
                if (block1 != null && this.onGround) {
                    this.onEntityCollidedWithBlock(block1);
                }
                this.distanceWalkedModified = (float) (this.distanceWalkedModified + MathHelper.func_76133_a(d12 * d12 + d14 * d14) * 0.6);
                this.distanceWalkedOnStepModified = (float) (this.distanceWalkedOnStepModified + MathHelper.func_76133_a(d12 * d12 + d13 * d13 + d14 * d14) * 0.6);
                if (this.distanceWalkedOnStepModified > this.nextStepDistance && block1.func_149688_o() != Material.field_151579_a) {
                    this.nextStepDistance = (int) this.distanceWalkedOnStepModified + 1;
                }
            }
            try {
                this.doBlockCollisions();
            } catch (Throwable exception) {
                exception.printStackTrace();
            }
            boolean flag2 = this.isWet();
            if (this.worldObj.func_147470_e(this.getEntityBoundingBox().func_72331_e(0.001, 0.001, 0.001))) {
                if (!flag2) {
                    this.fire++;
                    if (this.fire == 0) {
                        this.setFire(8);
                    }
                }
            } else if (this.fire <= 0) {
                this.fire = -this.fireResistance;
            }
            if (flag2 && this.fire > 0) {
                this.fire = -this.fireResistance;
            }
        }
    }

    public AxisAlignedBB getEntityBoundingBox() {
        return this.box;
    }

    public void setEntityBoundingBox(AxisAlignedBB box) {
        this.box = box;
    }

    public void setOnFireFromLava() {
        this.setFire(15);
    }

    public void setFire(int seconds) {
        int i = seconds * 20;
        i = EnchantmentProtection.func_92093_a(this.player, i);
        if (this.fire < i) {
            this.fire = i;
        }
    }

    public boolean isWet() {
        return this.inWater || this.isRainingAt(new BlockPos(this.posX, this.posY, this.posZ)) || this.isRainingAt(new BlockPos(this.posX, this.posY + this.height, this.posZ));
    }

    public void doBlockCollisions() {
        BlockPos blockpos = new BlockPos(this.getEntityBoundingBox().field_72340_a + 0.001, this.getEntityBoundingBox().field_72338_b + 0.001, this.getEntityBoundingBox().field_72339_c + 0.001);
        BlockPos blockpos1 = new BlockPos(this.getEntityBoundingBox().field_72336_d - 0.001, this.getEntityBoundingBox().field_72337_e - 0.001, this.getEntityBoundingBox().field_72334_f - 0.001);
        if (this.isAreaLoaded(blockpos.func_177958_n(), blockpos.func_177956_o(), blockpos.func_177952_p(), blockpos1.func_177958_n(), blockpos1.func_177956_o(), blockpos1.func_177952_p(), true)) {
            for (int i = blockpos.func_177958_n(); i <= blockpos1.func_177958_n(); i++) {
                for (int j = blockpos.func_177956_o(); j <= blockpos1.func_177956_o(); j++) {
                    for (int k = blockpos.func_177952_p(); k <= blockpos1.func_177952_p(); k++) {
                        BlockPos pos = new BlockPos(i, j, k);
                        IBlockState state = this.getBlockState(pos);
                        try {
                            Block block = state.func_177230_c();
                            if (block instanceof BlockWeb) {
                                this.isInWeb = true;
                            } else if (block instanceof BlockSoulSand) {
                                this.motionX *= 0.4;
                                this.motionZ *= 0.4;
                            }
                        } catch (Throwable exception2) {
                            exception2.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    public void updateFallState(double motionY, boolean onGround) {
        if (!this.isInWater()) {
            this.handleWaterMovement();
        }
        if (onGround) {
            if (this.fallDistance > 0.0F) {
                this.fallDistance = 0.0F;
            }
        } else if (motionY < 0.0) {
            this.fallDistance = (float) (this.fallDistance - motionY);
        }
    }

    public boolean handleWaterMovement() {
        if (this.handleMaterialAcceleration(this.getEntityBoundingBox().func_72314_b(0.0, -0.4F, 0.0).func_72331_e(0.001, 0.001, 0.001), Material.field_151586_h)) {
            this.fallDistance = 0.0F;
            this.inWater = true;
            this.fire = 0;
        } else {
            this.inWater = false;
        }
        return this.inWater;
    }

    public boolean handleMaterialAcceleration(AxisAlignedBB axisAlignedBB, Material material) {
        int i = MathHelper.func_76128_c(axisAlignedBB.field_72340_a);
        int j = MathHelper.func_76128_c(axisAlignedBB.field_72336_d + 1.0);
        int k = MathHelper.func_76128_c(axisAlignedBB.field_72338_b);
        int l = MathHelper.func_76128_c(axisAlignedBB.field_72337_e + 1.0);
        int i1 = MathHelper.func_76128_c(axisAlignedBB.field_72339_c);
        int j1 = MathHelper.func_76128_c(axisAlignedBB.field_72334_f + 1.0);
        if (!this.isAreaLoaded(i, k, i1, j, l, j1, true)) {
            return false;
        } else {
            boolean flag = false;
            Vec3 vec3 = new Vec3(0.0, 0.0, 0.0);
            MutableBlockPos blockPos = new MutableBlockPos();
            for (int k1 = i; k1 < j; k1++) {
                for (int l1 = k; l1 < l; l1++) {
                    for (int i2 = i1; i2 < j1; i2++) {
                        blockPos.func_181079_c(k1, l1, i2);
                        IBlockState state = this.getBlockState(blockPos);
                        if (state != null) {
                            Block block = state.func_177230_c();
                            if (block != null && block.func_149688_o() == material) {
                                double d0 = l1 + 1 - BlockLiquid.func_149801_b((Integer) state.func_177229_b(BlockLiquid.field_176367_b));
                                if (l >= d0) {
                                    flag = true;
                                    vec3 = block.func_176197_a(this.worldObj, blockPos, this.player, vec3);
                                }
                            }
                        }
                    }
                }
            }
            if (vec3.func_72433_c() > 0.0 && this.isPushedByWater()) {
                vec3 = vec3.func_72432_b();
                double d1 = 0.014;
                this.motionX = this.motionX + vec3.field_72450_a * d1;
                this.motionY = this.motionY + vec3.field_72448_b * d1;
                this.motionZ = this.motionZ + vec3.field_72449_c * d1;
            }
            return flag;
        }
    }

    public boolean isAreaLoaded(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, boolean idfk) {
        if (maxY >= 0 && minY < 256) {
            minX >>= 4;
            minZ >>= 4;
            maxX >>= 4;
            maxZ >>= 4;
            for (int i = minX; i <= maxX; i++) {
                for (int j = minZ; j <= maxZ; j++) {
                    if (!this.isChunkLoaded(i, j, idfk)) {
                        return false;
                    }
                }
            }
            return true;
        } else {
            return false;
        }
    }

    public void onEntityCollidedWithBlock(Block block) {
        if (block instanceof BlockSlime && Math.abs(this.motionY) < 0.1 && !this.isSneaking()) {
            double motion = 0.4 + Math.abs(this.motionY) * 0.2;
            this.motionX *= motion;
            this.motionZ *= motion;
        }
    }

    public boolean canTriggerWalking() {
        return !this.capabilities.field_75100_b;
    }

    public boolean isOnLadder() {
        int i = MathHelper.func_76128_c(this.posX);
        int j = MathHelper.func_76128_c(this.getEntityBoundingBox().field_72338_b);
        int k = MathHelper.func_76128_c(this.posZ);
        Block block = this.getBlockState(new BlockPos(i, j, k)).func_177230_c();
        return (block == Blocks.field_150468_ap || block == Blocks.field_150395_bd) && !this.isSpectator;
    }

    public void moveFlying(float strafe, float forward, float friction) {
        float f = strafe * strafe + forward * forward;
        if (f >= 1.0E-4F) {
            f = MathHelper.func_76129_c(f);
            if (f < 1.0F) {
                f = 1.0F;
            }
            f = friction / f;
            float newStrafe = strafe * f;
            float newForward = forward * f;
            float f1 = MathHelper.func_76126_a(this.rotationYaw * (float) Math.PI / 180.0F);
            float f2 = MathHelper.func_76134_b(this.rotationYaw * (float) Math.PI / 180.0F);
            this.motionX += newStrafe * f2 - newForward * f1;
            this.motionZ += newForward * f2 + newStrafe * f1;
        }
    }

    public void jump() {
        this.motionY = this.getJumpUpwardsMotion();
        if (this.isPotionActive(Potion.field_76430_j)) {
            this.motionY = this.motionY + (this.getActivePotionEffect(Potion.field_76430_j).func_76458_c() + 1) * 0.1F;
        }
        if (this.isSprinting()) {
            float f = this.rotationYaw * (float) (Math.PI / 180.0);
            this.motionX = this.motionX - MathHelper.func_76126_a(f) * 0.2F;
            this.motionZ = this.motionZ + MathHelper.func_76134_b(f) * 0.2F;
        }
        this.isAirBorne = true;
    }

    public boolean isSprinting() {
        return this.isSprinting;
    }

    public boolean isPotionActive(Potion potion) {
        return this.player.func_70660_b(potion) != null;
    }

    public PotionEffect getActivePotionEffect(Potion potion) {
        return this.player.func_70660_b(potion);
    }

    public float getJumpUpwardsMotion() {
        return 0.42F;
    }

    public boolean isInWater() {
        return this.inWater;
    }

    public void updateLivingEntityInput() {
        this.moveForward = this.movementInput.field_78900_b * this.itemInputMultiplier;
        this.moveStrafing = this.movementInput.field_78902_a * this.itemInputMultiplier;
        this.isJumping = this.movementInput.field_78901_c;
    }

    public boolean isServerWorld() {
        return true;
    }

    public boolean isMovementBlocked() {
        return this.player.func_110143_aJ() <= 0.0F || this.player.func_70608_bn();
    }

    public boolean isInLava() {
        return this.worldObj.func_72875_a(this.getEntityBoundingBox().func_72314_b(-0.1F, -0.4F, -0.1F), Material.field_151587_i);
    }

    public void updateAITick() {
        this.motionY += 0.04F;
    }

    public boolean isOffsetPositionInLiquid(double x, double y, double z) {
        AxisAlignedBB box = this.getEntityBoundingBox().func_72317_d(x, y, z);
        return this.isLiquidPresentInAABB(box);
    }

    public boolean isLiquidPresentInAABB(AxisAlignedBB box) {
        return this.getCollidingBoundingBoxes(box).isEmpty() && !this.worldObj.func_72953_d(box);
    }

    public List<AxisAlignedBB> getCollidingBoundingBoxes(AxisAlignedBB box) {
        List<AxisAlignedBB> list = Lists.newArrayList();
        int i = MathHelper.func_76128_c(box.field_72340_a);
        int j = MathHelper.func_76128_c(box.field_72336_d + 1.0);
        int k = MathHelper.func_76128_c(box.field_72338_b);
        int l = MathHelper.func_76128_c(box.field_72337_e + 1.0);
        int i1 = MathHelper.func_76128_c(box.field_72339_c);
        int j1 = MathHelper.func_76128_c(box.field_72334_f + 1.0);
        WorldBorder worldBorder = this.getWorldBorder();
        boolean flag = this.isOutsideBorder;
        boolean flag1 = this.isInsideBorder(worldBorder, flag);
        IBlockState iblockstate = Blocks.field_150348_b.func_176223_P();
        MutableBlockPos blockPos = new MutableBlockPos();
        for (int k1 = i; k1 < j; k1++) {
            for (int l1 = i1; l1 < j1; l1++) {
                if (this.isBlockLoaded(blockPos.func_181079_c(k1, 64, l1))) {
                    for (int i2 = k - 1; i2 < l; i2++) {
                        blockPos.func_181079_c(k1, i2, l1);
                        if (flag && flag1) {
                            this.isOutsideBorder = false;
                        } else if (!flag && !flag1) {
                            this.isOutsideBorder = true;
                        }
                        IBlockState state = iblockstate;
                        if (worldBorder.func_177746_a(blockPos) || !flag1) {
                            state = this.getBlockState(blockPos);
                        }
                        if (state.func_177230_c() == Blocks.field_150468_ap && this.blockOverrides.containsKey(blockPos)) {
                            AxisAlignedBB ladderCollisionBox = this.getLadderCollisionBox(blockPos, state);
                            if (ladderCollisionBox.func_72326_a(box)) {
                                list.add(ladderCollisionBox);
                            }
                        } else {
                            state.func_177230_c().func_180638_a(this.worldObj, blockPos, state, box, list, this.player);
                        }
                    }
                }
            }
        }
        double d0 = 0.25;
        for (Entity entity : this.getEntitiesWithinAABBExcludingEntity(this.player, box.func_72314_b(d0, d0, d0))) {
            if (this.riddenByEntity != entity && this.ridingEntity != entity) {
                AxisAlignedBB axisAlignedBB2 = entity.func_70046_E();
                if (axisAlignedBB2 != null && axisAlignedBB2.func_72326_a(box)) {
                    list.add(axisAlignedBB2);
                }
                axisAlignedBB2 = this.getCollisionBox(this.player, entity);
                if (axisAlignedBB2 != null && axisAlignedBB2.func_72326_a(box)) {
                    list.add(axisAlignedBB2);
                }
            }
        }
        return list;
    }

    private AxisAlignedBB getLadderCollisionBox(BlockPos pos, IBlockState state) {
        EnumFacing facing = (EnumFacing) state.func_177229_b(BlockLadder.field_176382_a);
        double minX = facing == EnumFacing.WEST ? 0.875 : 0.0;
        double maxX = facing == EnumFacing.EAST ? 0.125 : 1.0;
        double minZ = facing == EnumFacing.NORTH ? 0.875 : 0.0;
        double maxZ = facing == EnumFacing.SOUTH ? 0.125 : 1.0;
        return new AxisAlignedBB(pos.func_177958_n() + minX, pos.func_177956_o(), pos.func_177952_p() + minZ, pos.func_177958_n() + maxX, pos.func_177956_o() + 1.0, pos.func_177952_p() + maxZ);
    }

    public void setBlockState(BlockPos blockPos, IBlockState state) {
        this.blockOverrides.put(blockPos, state);
    }

    public IBlockState getBlockState(BlockPos blockPos) {
        IBlockState state = this.blockOverrides.get(blockPos);
        return state == null ? this.worldObj.func_180495_p(blockPos) : state;
    }

    private Chunk getChunkFromBlockCoords(BlockPos blockPos) {
        return this.getChunkFromChunkCoords(blockPos.func_177958_n() >> 4, blockPos.func_177952_p() >> 4);
    }

    private Chunk getChunkFromChunkCoords(int x, int z) {
        return this.chunkProvider.func_73154_d(x, z);
    }

    private boolean isValid(BlockPos pos) {
        return pos.func_177958_n() >= -30000000 && pos.func_177952_p() >= -30000000 && pos.func_177958_n() < 30000000 && pos.func_177952_p() < 30000000 && pos.func_177956_o() >= 0 && pos.func_177956_o() < 256;
    }

    private WorldBorder getWorldBorder() {
        return this.worldBorder;
    }

    private boolean isInsideBorder(WorldBorder border, boolean insideBorder) {
        double d0 = border.func_177726_b();
        double d1 = border.func_177736_c();
        double d2 = border.func_177728_d();
        double d3 = border.func_177733_e();
        if (insideBorder) {
            d0++;
            d1++;
            d2--;
            d3--;
        } else {
            d0--;
            d1--;
            d2++;
            d3++;
        }
        return this.posX > d0 && this.posX < d2 && this.posZ > d1 && this.posZ < d3;
    }

    private boolean isBlockLoaded(BlockPos pos) {
        return this.isBlockLoaded(pos, true);
    }

    private boolean isBlockLoaded(BlockPos pos, boolean check2) {
        return this.isValid(pos) && this.isChunkLoaded(pos.func_177958_n() >> 4, pos.func_177952_p() >> 4, check2);
    }

    private boolean isChunkLoaded(int x, int z, boolean flag) {
        return this.chunkProvider.func_73149_a(x, z) && (flag || !this.chunkProvider.func_73154_d(x, z).func_76621_g());
    }

    private List<Entity> getEntitiesWithinAABBExcludingEntity(Entity entity, AxisAlignedBB box) {
        return this.getEntitiesInAABBexcluding(entity, box, EntitySelectors.field_180132_d);
    }

    private List<Entity> getEntitiesInAABBexcluding(Entity entity, AxisAlignedBB bb, Predicate<Entity> predicate) {
        List<Entity> list = Lists.newArrayList();
        int i = MathHelper.func_76128_c((bb.field_72340_a - 2.0) / 16.0);
        int j = MathHelper.func_76128_c((bb.field_72336_d + 2.0) / 16.0);
        int k = MathHelper.func_76128_c((bb.field_72339_c - 2.0) / 16.0);
        int l = MathHelper.func_76128_c((bb.field_72334_f + 2.0) / 16.0);
        for (int i1 = i; i1 <= j; i1++) {
            for (int j1 = k; j1 <= l; j1++) {
                if (this.isChunkLoaded(i1, j1, true)) {
                    this.getChunkFromChunkCoords(i1, j1).func_177414_a(entity, bb, list, predicate);
                }
            }
        }
        return list;
    }

    private AxisAlignedBB getCollisionBox(Entity player, Entity entity) {
        if (entity instanceof EntityBoat) {
            return entity.func_174813_aQ();
        } else {
            return entity instanceof EntityMinecart ? player.func_70114_g(entity) : null;
        }
    }

    private float getAIMoveSpeed() {
        return (float) this.getEntityAttribute(SharedMonsterAttributes.field_111263_d).func_111126_e();
    }

    private IAttributeInstance getEntityAttribute(IAttribute iAttribute) {
        return this.getAttributeMap().func_111151_a(iAttribute);
    }

    private BaseAttributeMap getAttributeMap() {
        if (this.attributeMap == null) {
            this.attributeMap = new ServersideAttributeMap();
        }
        return this.attributeMap;
    }

    private void resetPositionToBB() {
        this.posX = (this.getEntityBoundingBox().field_72340_a + this.getEntityBoundingBox().field_72336_d) / 2.0;
        this.posY = this.getEntityBoundingBox().field_72338_b;
        this.posZ = (this.getEntityBoundingBox().field_72339_c + this.getEntityBoundingBox().field_72334_f) / 2.0;
    }

    private void onLanded(Block block) {
        if (block instanceof BlockSlime) {
            if (this.isSneaking()) {
                this.motionY = 0.0;
            } else if (this.motionY < 0.0) {
                this.motionY = -this.motionY;
            }
        } else {
            this.motionY = 0.0;
        }
    }

    public boolean isSneaking() {
        return this.movementInput.field_78899_d && !this.player.func_70608_bn();
    }

    private boolean isRainingAt(BlockPos pos) {
        if (this.worldObj.func_72867_j(1.0F) <= 0.2) {
            return false;
        } else if (!this.canSeeSky(pos)) {
            return false;
        } else if (this.worldObj.func_175725_q(pos).func_177956_o() > pos.func_177956_o()) {
            return false;
        } else {
            BiomeGenBase base = this.worldObj.func_180494_b(pos);
            if (base.func_76746_c()) {
                return false;
            } else {
                return this.worldObj.func_175708_f(pos, false) ? false : base.func_76738_d();
            }
        }
    }

    private boolean canSeeSky(BlockPos pos) {
        return this.getChunkFromBlockCoords(pos).func_177444_d(pos);
    }

    private boolean isPushedByWater() {
        return !this.capabilities.field_75100_b;
    }

    public Pair<Double, Double> checkForCollision(SimulatedPlayer simPlayer, double velocityX, double velocityZ) {
        double d3 = velocityX;
        double d5 = velocityZ;
        double d6;
        for (d6 = 0.05; velocityX != 0.0 && simPlayer.getCollidingBoundingBoxes(simPlayer.box.func_72317_d(velocityX, -1.0, 0.0)).isEmpty(); d3 = velocityX) {
            if (velocityX < d6 && velocityX >= -d6) {
                velocityX = 0.0;
            } else if (velocityX > 0.0) {
                velocityX -= d6;
            } else {
                velocityX += d6;
            }
        }
        for (; velocityZ != 0.0 && simPlayer.getCollidingBoundingBoxes(simPlayer.box.func_72317_d(0.0, -1.0, velocityZ)).isEmpty(); d5 = velocityZ) {
            if (velocityZ < d6 && velocityZ >= -d6) {
                velocityZ = 0.0;
            } else if (velocityZ > 0.0) {
                velocityZ -= d6;
            } else {
                velocityZ += d6;
            }
        }
        for (; velocityX != 0.0 && velocityZ != 0.0 && simPlayer.getCollidingBoundingBoxes(simPlayer.box.func_72317_d(velocityX, -1.0, velocityZ)).isEmpty(); d5 = velocityZ) {
            if (velocityX < d6 && velocityX >= -d6) {
                velocityX = 0.0;
            } else if (velocityX > 0.0) {
                velocityX -= d6;
            } else {
                velocityX += d6;
            }
            d3 = velocityX;
            if (velocityZ < d6 && velocityZ >= -d6) {
                velocityZ = 0.0;
            } else if (velocityZ > 0.0) {
                velocityZ -= d6;
            } else {
                velocityZ += d6;
            }
        }
        return Pair.of(d3, d5);
    }

    public float getEyeHeight() {
        return this.height * 0.85F;
    }
}
