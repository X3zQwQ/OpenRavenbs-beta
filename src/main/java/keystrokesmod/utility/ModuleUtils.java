package keystrokesmod.utility;

import keystrokesmod.event.NoEventPacketEvent;
import keystrokesmod.event.PostMotionEvent;
import keystrokesmod.event.PreMotionEvent;
import keystrokesmod.event.PreUpdateEvent;
import keystrokesmod.event.ReceivePacketEvent;
import keystrokesmod.event.SendPacketEvent;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.combat.KillAura;
import keystrokesmod.module.impl.combat.Velocity;
import keystrokesmod.module.impl.movement.LongJump;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C07PacketPlayerDigging.Action;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.network.play.server.S27PacketExplosion;
import net.minecraft.util.BlockPos;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class ModuleUtils implements IMinecraftInstance {

    public static boolean isBreaking;

    public static boolean threwFireball;

    public static boolean threwFireballLow;

    private int isBreakingTick;

    public static long MAX_EXPLOSION_DIST_SQ = 10L;

    private long FIREBALL_TIMEOUT = 500L;

    private long fireballTime = 0L;

    public static int inAirTicks;

    public static int groundTicks;

    public static int stillTicks;

    public static int fadeEdge;

    public static int lastFaceDifference;

    private int lastFace;

    public static double offsetValue = 2.01E-11;

    public static boolean isAttacking;

    private int attackingTicks;

    public static int profileTicks = -1;

    public static boolean lastTickOnGround;

    public static boolean lastTickPos1;

    private boolean thisTickOnGround;

    private boolean thisTickPos1;

    public static boolean firstDamage;

    public static boolean isBlocked;

    public static boolean damage;

    private int damageTicks;

    private boolean lowhopAir;

    private static boolean allowFriction;

    public static boolean canSlow;

    public static boolean didSlow;

    public static boolean setSlow;

    @SubscribeEvent
    public void onSendPacketNoEvent(NoEventPacketEvent e) {
        this.handleAllPacket(e.getPacket());
    }

    @SubscribeEvent
    public void onSendPacket(SendPacketEvent e) {
        this.handleAllPacket(e.getPacket());
        if (e.getPacket() instanceof C07PacketPlayerDigging) {
            isBreaking = true;
        }
        if (e.getPacket() instanceof C08PacketPlayerBlockPlacement && Utils.holdingFireball() && Utils.isBindDown(mc.field_71474_y.field_74313_G)) {
            this.fireballTime = System.currentTimeMillis();
            threwFireball = true;
            if (mc.field_71439_g.field_70125_A > 50.0F) {
                threwFireballLow = true;
            }
        }
        if (e.getPacket() instanceof C08PacketPlayerBlockPlacement && Utils.scaffoldDiagonal(false) && ((C08PacketPlayerBlockPlacement) e.getPacket()).func_149568_f() != 1) {
            int currentFace = ((C08PacketPlayerBlockPlacement) e.getPacket()).func_149568_f();
            if (currentFace == this.lastFace) {
                lastFaceDifference++;
            } else {
                lastFaceDifference = 0;
            }
            this.lastFace = currentFace;
        }
    }

    @SubscribeEvent
    public void onReceivePacket(ReceivePacketEvent e) {
        if (Utils.nullCheck() && !e.isCanceled()) {
            if (e.getPacket() instanceof S27PacketExplosion) {
                S27PacketExplosion s27 = (S27PacketExplosion) e.getPacket();
                if (threwFireball && mc.field_71439_g.func_180425_c().func_177954_c(s27.func_149148_f(), s27.func_149143_g(), s27.func_149145_h()) <= MAX_EXPLOSION_DIST_SQ) {
                    ModuleManager.velocity.disable = false;
                    ModuleManager.antiKnockback.disable = false;
                    threwFireball = false;
                    e.setCanceled(false);
                }
            }
        }
    }

    private void handleAllPacket(Packet<?> packet) {
        if (Utils.nullCheck()) {
            if (packet instanceof C08PacketPlayerBlockPlacement && Utils.holdingSword() && !BlockUtils.isInteractable(mc.field_71476_x) && !isBlocked) {
                isBlocked = true;
            } else if (packet instanceof C07PacketPlayerDigging && isBlocked) {
                if (((C07PacketPlayerDigging) packet).func_180762_c() == Action.RELEASE_USE_ITEM) {
                    isBlocked = false;
                }
            } else if (packet instanceof C09PacketHeldItemChange && isBlocked) {
                isBlocked = false;
            }
            if (packet instanceof C02PacketUseEntity) {
                isAttacking = true;
                this.attackingTicks = 5;
            }
        }
    }

    @SubscribeEvent
    public void onPreUpdate(PreUpdateEvent e) {
        if (damage && ++this.damageTicks >= 8) {
            firstDamage = false;
            damage = false;
            this.damageTicks = 0;
        }
        profileTicks++;
        if (isAttacking) {
            if (this.attackingTicks <= 0) {
                isAttacking = false;
            } else {
                this.attackingTicks--;
            }
        }
        if (LongJump.slotReset && ++LongJump.slotResetTicks >= 2) {
            LongJump.stopModules = false;
            LongJump.slotResetTicks = 0;
            LongJump.slotReset = false;
        }
        if (!ModuleManager.bHop.hopping) {
            allowFriction = false;
        } else if (!mc.field_71439_g.field_70122_E) {
            allowFriction = true;
        }
        if (this.fireballTime > 0L && System.currentTimeMillis() - this.fireballTime > this.FIREBALL_TIMEOUT / 3L) {
            threwFireballLow = false;
            ModuleManager.velocity.disable = false;
            ModuleManager.antiKnockback.disable = false;
        }
        if (this.fireballTime > 0L && System.currentTimeMillis() - this.fireballTime > this.FIREBALL_TIMEOUT) {
            threwFireballLow = false;
            threwFireball = false;
            this.fireballTime = 0L;
            ModuleManager.velocity.disable = false;
            ModuleManager.antiKnockback.disable = false;
        }
        if (isBreaking && ++this.isBreakingTick >= 1) {
            isBreaking = false;
            this.isBreakingTick = 0;
        }
    }

    @SubscribeEvent
    public void onPostMotion(PostMotionEvent e) {
        if (this.bHopBoostConditions() && firstDamage) {
            Utils.setSpeed(Utils.getHorizontalSpeed());
            firstDamage = false;
        }
    }

    private boolean bHopBoostConditions() {
        return ModuleManager.bHop.isEnabled() && ModuleManager.bHop.damageBoost.isToggled() && (!ModuleManager.bHop.damageBoostRequireKey.isToggled() || ModuleManager.bHop.damageBoostKey.isPressed());
    }

    public static double applyFrictionMulti() {
        int speedAmplifier = Utils.getSpeedAmplifier();
        return speedAmplifier > 1 && allowFriction ? 1.0 : 1.0;
    }

    @SubscribeEvent
    public void onPreMotion(PreMotionEvent e) {
        int simpleY = (int) Math.round(e.posY % 1.0 * 10000.0);
        lastTickOnGround = this.thisTickOnGround;
        this.thisTickOnGround = mc.field_71439_g.field_70122_E;
        lastTickPos1 = this.thisTickPos1;
        this.thisTickPos1 = mc.field_71439_g.field_70163_u % 1.0 == 0.0;
        inAirTicks = mc.field_71439_g.field_70122_E ? 0 : ++inAirTicks;
        groundTicks = !mc.field_71439_g.field_70122_E ? 0 : ++groundTicks;
        stillTicks = Utils.isMoving() ? 0 : ++stillTicks;
        Block blockBelow = BlockUtils.getBlock(new BlockPos(mc.field_71439_g.field_70165_t, mc.field_71439_g.field_70163_u - 1.0, mc.field_71439_g.field_70161_v));
        Block blockBelow2 = BlockUtils.getBlock(new BlockPos(mc.field_71439_g.field_70165_t, mc.field_71439_g.field_70163_u - 2.0, mc.field_71439_g.field_70161_v));
        Block block = BlockUtils.getBlock(new BlockPos(mc.field_71439_g.field_70165_t, mc.field_71439_g.field_70163_u, mc.field_71439_g.field_70161_v));
        if (ModuleManager.bHop.didMove && (!damage || Velocity.vertical.getInput() == 0.0) && !mc.field_71439_g.field_70123_F) {
            if (!(block instanceof BlockAir) || blockBelow instanceof BlockAir && blockBelow2 instanceof BlockAir) {
                this.resetLowhop();
            }
            label88: switch((int) ModuleManager.bHop.mode.getInput()) {
                case 2:
                    switch(simpleY) {
                        case 13:
                            mc.field_71439_g.field_70181_x -= 0.02483;
                            break;
                        case 2000:
                            mc.field_71439_g.field_70181_x -= 0.1913;
                            break;
                        case 7016:
                            mc.field_71439_g.field_70181_x += 0.08;
                    }
                    if (inAirTicks > 6 && Utils.isMoving()) {
                        Utils.setSpeed(Utils.getHorizontalSpeed(mc.field_71439_g));
                    }
                    if (inAirTicks > 8) {
                        this.resetLowhop();
                    }
                    break;
                case 3:
                    switch(simpleY) {
                        case 13:
                            mc.field_71439_g.field_70181_x -= 0.045;
                            break label88;
                        case 2000:
                            mc.field_71439_g.field_70181_x -= 0.175;
                            this.resetLowhop();
                        default:
                            break label88;
                    }
                case 4:
                    switch(simpleY) {
                        case 1138:
                            mc.field_71439_g.field_70181_x -= 0.13;
                            break;
                        case 2031:
                            mc.field_71439_g.field_70181_x -= 0.2;
                            this.resetLowhop();
                            break;
                        case 4200:
                            mc.field_71439_g.field_70181_x = 0.39;
                    }
            }
        }
        if (!mc.field_71439_g.field_70122_E) {
            this.lowhopAir = true;
        } else if (this.lowhopAir) {
            this.resetLowhop();
        }
        if (ModuleManager.bHop.setRotation) {
            if (KillAura.target == null) {
                float yaw = mc.field_71439_g.field_70177_z - 55.0F;
                e.setYaw(yaw);
            }
            if (mc.field_71439_g.field_70122_E) {
                ModuleManager.bHop.setRotation = false;
            }
        }
        if (canSlow && !mc.field_71439_g.field_70122_E) {
            double motionVal = 0.9 - inAirTicks / 10000.0 - Utils.randomizeDouble(1.0E-5, 6.0E-5);
            if (mc.field_71439_g.field_70737_aN == 0 && inAirTicks > 4 && !setSlow) {
                mc.field_71439_g.field_70159_w *= motionVal;
                mc.field_71439_g.field_70179_y *= motionVal;
                setSlow = true;
            }
            didSlow = true;
        } else if (didSlow) {
            didSlow = false;
            canSlow = false;
        }
        if (mc.field_71439_g.field_70122_E) {
            setSlow = false;
        }
    }

    private void resetLowhop() {
        ModuleManager.bHop.lowhop = false;
        ModuleManager.bHop.didMove = false;
        this.lowhopAir = false;
    }

    public static void handleSlow() {
        didSlow = false;
        canSlow = true;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRenderWorld(RenderWorldLastEvent e) {
        if (Utils.nullCheck()) {
            if (ModuleManager.killAura.rotationMode.getInput() == 0.0 && KillAura.target != null) {
                mc.field_71439_g.field_71163_h = mc.field_71439_g.field_70177_z;
                mc.field_71439_g.field_71154_f = mc.field_71439_g.field_70177_z;
            }
        }
    }
}
