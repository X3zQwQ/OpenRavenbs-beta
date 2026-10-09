package keystrokesmod.utility;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.gson.JsonObject;
import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;
import java.util.stream.IntStream;
import keystrokesmod.Raven;
import keystrokesmod.helper.MouseHelper;
import keystrokesmod.mixin.impl.accessor.IAccessorGuiIngame;
import keystrokesmod.mixin.impl.accessor.IAccessorItemFood;
import keystrokesmod.mixin.impl.accessor.IAccessorMinecraft;
import keystrokesmod.mixin.impl.accessor.IAccessorPlayerControllerMP;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.client.Settings;
import keystrokesmod.module.impl.minigames.DuelsStats;
import keystrokesmod.module.impl.player.Freecam;
import keystrokesmod.module.impl.player.Scaffold;
import keystrokesmod.module.impl.world.AntiBot;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.color.ColorConstants;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.block.BlockBanner;
import net.minecraft.block.BlockBeacon;
import net.minecraft.block.BlockButton;
import net.minecraft.block.BlockCactus;
import net.minecraft.block.BlockCarpet;
import net.minecraft.block.BlockClay;
import net.minecraft.block.BlockDaylightDetector;
import net.minecraft.block.BlockDoublePlant;
import net.minecraft.block.BlockEndPortal;
import net.minecraft.block.BlockEndPortalFrame;
import net.minecraft.block.BlockFence;
import net.minecraft.block.BlockFlower;
import net.minecraft.block.BlockFlowerPot;
import net.minecraft.block.BlockGravel;
import net.minecraft.block.BlockLadder;
import net.minecraft.block.BlockLever;
import net.minecraft.block.BlockLilyPad;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.BlockPane;
import net.minecraft.block.BlockRailBase;
import net.minecraft.block.BlockRedstoneTorch;
import net.minecraft.block.BlockSand;
import net.minecraft.block.BlockSapling;
import net.minecraft.block.BlockSign;
import net.minecraft.block.BlockSkull;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.BlockSnow;
import net.minecraft.block.BlockSoulSand;
import net.minecraft.block.BlockStainedGlassPane;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.BlockTallGrass;
import net.minecraft.block.BlockTorch;
import net.minecraft.block.BlockTripWire;
import net.minecraft.block.BlockTripWireHook;
import net.minecraft.block.BlockWeb;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.ContainerPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemFireball;
import net.minecraft.item.ItemFishingRod;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemHoe;
import net.minecraft.item.ItemSpade;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.network.play.client.C03PacketPlayer.C05PacketPlayerLook;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.potion.Potion;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StringUtils;
import net.minecraft.util.Vec3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public class Utils implements IMinecraftInstance {

    private static final Random rand = new Random();

    private static final ThreadLocal<Integer> LOCAL_PLAYER_SUB_UPDATE_DEPTH = ThreadLocal.withInitial(() -> 0);

    public static HashSet<String> friends = new HashSet<>();

    public static HashSet<String> enemies = new HashSet<>();

    public static final Logger log = LogManager.getLogger();

    public static boolean addEnemy(String name) {
        if (Raven.playerRelationsManager != null) {
            return Raven.playerRelationsManager.addEnemy(name);
        } else if (enemies.add(name.toLowerCase())) {
            sendMessage("&7Added enemy&7: &b" + name);
            return true;
        } else {
            return false;
        }
    }

    public static boolean removeEnemy(String name) {
        if (Raven.playerRelationsManager != null) {
            return Raven.playerRelationsManager.removeEnemy(name);
        } else if (enemies.remove(name.toLowerCase())) {
            sendMessage("&7Removed enemy&7: &b" + name);
            return true;
        } else {
            return false;
        }
    }

    public static float getCameraYaw() {
        return (float) Math.toDegrees(Math.atan2(ActiveRenderInfo.func_178803_d(), ActiveRenderInfo.func_178808_b()));
    }

    public static float getCameraPitch() {
        return (float) Math.toDegrees(Math.acos(ActiveRenderInfo.func_178809_c()));
    }

    public static Vec3 getCameraPos(double renderPartialTicks) {
        if (mc.field_71474_y.field_74320_O == 0) {
            return new Vec3(mc.field_71439_g.field_70165_t, mc.field_71439_g.field_70163_u + mc.field_71439_g.func_70047_e(), mc.field_71439_g.field_70161_v);
        } else {
            float cameraDistance = 4.0F;
            if (ModuleManager.extendCamera != null && ModuleManager.extendCamera.isEnabled()) {
                cameraDistance = (float) ModuleManager.extendCamera.distance.getInput();
            }
            Entity renderEntity = mc.func_175606_aa();
            float entityEyeHeight = renderEntity.func_70047_e();
            double interpolatedX = renderEntity.field_70169_q + (renderEntity.field_70165_t - renderEntity.field_70169_q) * renderPartialTicks;
            double interpolatedY = renderEntity.field_70167_r + (renderEntity.field_70163_u - renderEntity.field_70167_r) * renderPartialTicks + entityEyeHeight;
            double interpolatedZ = renderEntity.field_70166_s + (renderEntity.field_70161_v - renderEntity.field_70166_s) * renderPartialTicks;
            double adjustedDistance = cameraDistance;
            float cameraYaw = getCameraYaw();
            float cameraPitch = getCameraPitch();
            double offsetX = -MathHelper.func_76126_a(cameraYaw / 180.0F * (float) Math.PI) * MathHelper.func_76134_b(cameraPitch / 180.0F * (float) Math.PI) * adjustedDistance;
            double offsetZ = MathHelper.func_76134_b(cameraYaw / 180.0F * (float) Math.PI) * MathHelper.func_76134_b(cameraPitch / 180.0F * (float) Math.PI) * adjustedDistance;
            double offsetY = -MathHelper.func_76126_a(cameraPitch / 180.0F * (float) Math.PI) * adjustedDistance;
            if (ModuleManager.noCameraClip == null || !ModuleManager.noCameraClip.isEnabled()) {
                for (int i = 0; i < 8; i++) {
                    float cornerOffsetX = ((i & 1) * 2 - 1) * 0.1F;
                    float cornerOffsetY = ((i >> 1 & 1) * 2 - 1) * 0.1F;
                    float cornerOffsetZ = ((i >> 2 & 1) * 2 - 1) * 0.1F;
                    MovingObjectPosition rayTraceResult = mc.field_71441_e.func_72933_a(new Vec3(interpolatedX + cornerOffsetX, interpolatedY + cornerOffsetY, interpolatedZ + cornerOffsetZ), new Vec3(interpolatedX - offsetX + cornerOffsetX + cornerOffsetZ, interpolatedY - offsetY + cornerOffsetY, interpolatedZ - offsetZ + cornerOffsetZ));
                    if (rayTraceResult != null) {
                        double blockHitDistance = rayTraceResult.field_72307_f.func_72438_d(new Vec3(interpolatedX, interpolatedY, interpolatedZ));
                        if (blockHitDistance < adjustedDistance) {
                            adjustedDistance = blockHitDistance;
                        }
                    }
                }
            }
            double finalCameraX = interpolatedX - offsetX * (adjustedDistance / cameraDistance);
            double finalCameraY = interpolatedY - offsetY * (adjustedDistance / cameraDistance);
            double finalCameraZ = interpolatedZ - offsetZ * (adjustedDistance / cameraDistance);
            return new Vec3(finalCameraX, finalCameraY, finalCameraZ);
        }
    }

    public static void printInfo(EntityLivingBase ent) {
        if (ent != null) {
            sendMessage("&7&m-------------------------");
            sendMessage("&eattacking: &r" + ent.func_70005_c_());
            sendMessage("&7type: &b" + ent.getClass().getSimpleName());
            sendMessage("&7bot: &r" + (ModuleManager.antiBot.isEnabled() ? AntiBot.isBot(ent) : "&cantibot disabled"));
            boolean isPlayer = ent instanceof EntityPlayer;
            sendMessage("&7player: &r" + isPlayer);
            sendMessage("&7dist eye: &d" + round(getDistanceToEye(ent), 2));
            sendMessage("&7min dist: &d" + round(Math.sqrt(raycastDistanceSq(ent, 12.0, false)), 2));
            IChatComponent displayName = ent.func_145748_c_();
            boolean hasDisplayName = displayName != null;
            if (isPlayer) {
                EntityPlayer p = (EntityPlayer) ent;
                UUID uuid = p.func_110124_au();
                sendMessage("&7uuid: &d" + uuid.toString() + " &b" + uuid.variant() + " " + uuid.version());
                NetworkPlayerInfo clientPlayer = mc.func_147114_u().func_175102_a(p.func_110124_au());
                sendMessage("&7ping: &d" + (clientPlayer == null ? "&cnot found" : clientPlayer.func_178853_c()));
                sendMessage("&7teammate: &r" + isTeammate(p));
                sendMessage("&7tablist: &r" + isInTabList(p));
                if (p.func_96124_cp() != null) {
                    ScorePlayerTeam scoreTeam = (ScorePlayerTeam) p.func_96124_cp();
                    sendMessage("&7team name: &r" + scoreTeam.func_96669_c());
                    sendMessage("&7team prefix: &r" + scoreTeam.func_96668_e());
                    sendMessage("&7team suffix: &r" + scoreTeam.func_96663_f());
                }
            }
            sendMessage("&7display unformatted: &r" + (hasDisplayName ? displayName.func_150260_c() : "&cnull"));
            sendMessage("&7insertion: &r" + (hasDisplayName ? displayName.func_150256_b().func_179986_j() : "&cnull"));
            sendMessage("&7health: &r" + ent.func_110143_aJ());
            sendMessage("&7ht: &d" + ent.field_70737_aN + " &7mht: &d" + ent.field_70738_aO);
            sendMessage("&7ticks existed: &r" + ent.field_70173_aa);
            sendMessage("&7invisible: &r" + ent.func_82150_aj());
            sendMessage("&7dead: &r" + ent.field_70128_L);
        }
    }

    public static double raycastDistanceSq(Entity en, double max_reach, boolean calc_rot) {
        Vec3 eyeVec = mc.field_71439_g.func_174824_e(1.0F);
        float yaw;
        float pitch;
        if (calc_rot) {
            float[] rot = RotationUtils.getRotations(en);
            yaw = rot[0];
            pitch = rot[1];
        } else {
            yaw = mc.field_71439_g.field_70177_z;
            pitch = mc.field_71439_g.field_70125_A;
        }
        float ff = MathHelper.func_76134_b(-yaw * (float) (Math.PI / 180.0) - (float) Math.PI);
        float ff2 = MathHelper.func_76126_a(-yaw * (float) (Math.PI / 180.0) - (float) Math.PI);
        float ff3 = -MathHelper.func_76134_b(-pitch * (float) (Math.PI / 180.0));
        float ff4 = MathHelper.func_76126_a(-pitch * (float) (Math.PI / 180.0));
        Vec3 lookVec = new Vec3(ff2 * ff3, ff4, ff * ff3);
        double lookVecX = lookVec.field_72450_a * max_reach;
        double lookVecY = lookVec.field_72448_b * max_reach;
        double lookVecZ = lookVec.field_72449_c * max_reach;
        Vec3 sumVec = eyeVec.func_72441_c(lookVecX, lookVecY, lookVecZ);
        List list = mc.field_71441_e.func_72839_b(mc.func_175606_aa(), mc.func_175606_aa().func_174813_aQ().func_72321_a(lookVecX, lookVecY, lookVecZ).func_72314_b(1.0, 1.0, 1.0));
        for (int i = 0; i < list.size(); i++) {
            Entity entity = (Entity) list.get(i);
            if (entity == en && entity.func_70067_L()) {
                float cbs = entity.func_70111_Y();
                AxisAlignedBB axis = entity.func_174813_aQ().func_72314_b(cbs, cbs, cbs);
                MovingObjectPosition mop = axis.func_72327_a(eyeVec, sumVec);
                if (mop != null) {
                    return eyeVec.func_72436_e(mop.field_72307_f);
                }
            }
        }
        return -1.0;
    }

    public static double getDistanceToEye(Entity en) {
        return mc.field_71439_g.func_174824_e(1.0F).func_72438_d(en.func_174824_e(1.0F));
    }

    public static boolean isInTabList(EntityPlayer p) {
        for (NetworkPlayerInfo playerInfo : mc.func_147114_u().func_175106_d()) {
            if (playerInfo.func_178845_a().equals(p.func_146103_bH())) {
                return true;
            }
        }
        return false;
    }

    public static String getServerName() {
        return DuelsStats.nick.isEmpty() ? mc.field_71439_g.func_70005_c_() : DuelsStats.nick;
    }

    public static boolean tabbedIn() {
        return mc.field_71462_r == null && mc.field_71415_G;
    }

    public static boolean isConsuming(Entity entity) {
        return !(entity instanceof EntityPlayer) ? false : ((EntityPlayer) entity).func_71039_bw() && holdingFood((EntityPlayer) entity);
    }

    public static boolean holdingFood(EntityLivingBase entity) {
        return entity.func_70694_bm() != null && entity.func_70694_bm().func_77973_b() instanceof ItemFood;
    }

    public static int getColorFromEntity(Entity entity) {
        if (entity == null) {
            return -1;
        } else {
            if (entity instanceof EntityPlayer) {
                ScorePlayerTeam scoreplayerteam = (ScorePlayerTeam) ((EntityLivingBase) entity).func_96124_cp();
                if (scoreplayerteam != null) {
                    String s = FontRenderer.func_78282_e(scoreplayerteam.func_96668_e());
                    if (s.length() >= 2) {
                        int teamColor = getColorFromFormattingCode(s.charAt(1));
                        if (teamColor != -1) {
                            return teamColor;
                        }
                    }
                }
            }
            IChatComponent displayNameComponent = entity.func_145748_c_();
            if (displayNameComponent == null) {
                return -1;
            } else {
                String displayName = removeFormatCodes(displayNameComponent.func_150254_d());
                return displayName.length() >= 2 && displayName.startsWith("§") && Character.toLowerCase(displayName.charAt(1)) != 102 ? getColorFromFormattingCode(displayName.charAt(1)) : -1;
            }
        }
    }

    private static int getColorFromFormattingCode(char formatCode) {
        switch(Character.toLowerCase(formatCode)) {
            case '0':
                return ColorConstants.BLACK;
            case '1':
                return ColorConstants.DARK_BLUE;
            case '2':
                return ColorConstants.DARK_GREEN;
            case '3':
                return ColorConstants.DARK_AQUA;
            case '4':
                return ColorConstants.DARK_RED;
            case '5':
                return ColorConstants.DARK_PURPLE;
            case '6':
                return ColorConstants.GOLD;
            case '7':
                return ColorConstants.GRAY;
            case '8':
                return ColorConstants.DARK_GRAY;
            case '9':
                return ColorConstants.BLUE;
            case ':':
            case ';':
            case '<':
            case '=':
            case '>':
            case '?':
            case '@':
            case 'A':
            case 'B':
            case 'C':
            case 'D':
            case 'E':
            case 'F':
            case 'G':
            case 'H':
            case 'I':
            case 'J':
            case 'K':
            case 'L':
            case 'M':
            case 'N':
            case 'O':
            case 'P':
            case 'Q':
            case 'R':
            case 'S':
            case 'T':
            case 'U':
            case 'V':
            case 'W':
            case 'X':
            case 'Y':
            case 'Z':
            case '[':
            case '\\':
            case ']':
            case '^':
            case '_':
            case '`':
            default:
                return -1;
            case 'a':
                return ColorConstants.GREEN;
            case 'b':
                return ColorConstants.AQUA;
            case 'c':
                return ColorConstants.RED;
            case 'd':
                return ColorConstants.LIGHT_PURPLE;
            case 'e':
                return ColorConstants.YELLOW;
            case 'f':
                return -1;
        }
    }

    public static boolean overVoid(double posX, double posY, double posZ) {
        for (int i = (int) posY; i > -1; i--) {
            if (!(mc.field_71441_e.func_180495_p(new BlockPos(posX, i, posZ)).func_177230_c() instanceof BlockAir)) {
                return false;
            }
        }
        return true;
    }

    public static Block getBlockFromName(String name) {
        return (Block) Block.field_149771_c.func_82594_a(new ResourceLocation("minecraft:" + name));
    }

    public static boolean canPlayerBeSeen(EntityLivingBase player) {
        double x = player.field_70165_t;
        double y = player.field_70163_u;
        double z = player.field_70161_v;
        Vec3 vecPlayer = mc.field_71439_g.func_174824_e(1.0F);
        double shoulderHeight = player.func_70047_e() - 0.2;
        if (canSeeVec(vecPlayer, new Vec3(x + 0.3, shoulderHeight, z))) {
            return true;
        } else if (canSeeVec(vecPlayer, new Vec3(x - 0.3, shoulderHeight, z))) {
            return true;
        } else if (canSeeVec(vecPlayer, new Vec3(x, shoulderHeight, z + 0.3))) {
            return true;
        } else if (canSeeVec(vecPlayer, new Vec3(x, shoulderHeight, z - 0.3))) {
            return true;
        } else {
            for (double d = player.func_70047_e() + 0.2; d > 0.0; d -= 0.2) {
                Vec3 vecPoint = new Vec3(x, y + d, z);
                if (canSeeVec(vecPlayer, vecPoint)) {
                    return true;
                }
            }
            return false;
        }
    }

    public static boolean holdingFireball() {
        return mc.field_71439_g.func_70694_bm() == null ? false : mc.field_71439_g.func_70694_bm().func_77973_b() instanceof ItemFireball;
    }

    public static boolean canSeeVec(Vec3 vecPlayer, Vec3 vecTarget) {
        MovingObjectPosition mop = mc.field_71441_e.func_147447_a(vecPlayer, vecTarget, false, false, false);
        return mop == null || mop.field_72313_a != MovingObjectType.BLOCK;
    }

    public static List<NetworkPlayerInfo> getTablist(boolean removeSelf) {
        ArrayList<NetworkPlayerInfo> list = new ArrayList<>(mc.func_147114_u().func_175106_d());
        removeDuplicates(list);
        if (removeSelf) {
            list.remove(mc.func_147114_u().func_175102_a(mc.field_71439_g.func_110124_au()));
        }
        return list;
    }

    public static void removeDuplicates(ArrayList list) {
        HashSet set = new HashSet(list);
        list.clear();
        list.addAll(set);
    }

    public static boolean removeFriend(String name) {
        if (Raven.playerRelationsManager != null) {
            return Raven.playerRelationsManager.removeFriend(name);
        } else if (friends.remove(name.toLowerCase())) {
            sendMessage("&7Removed &afriend&7: &b" + name);
            return true;
        } else {
            return false;
        }
    }

    public static String getCompilerDirectory() {
        String tempDirStr = System.getProperty("java.io.tmpdir") + "cmF2ZW5fc2NyaXB0cw";
        if (System.getProperty("os.name").toLowerCase().contains("linux")) {
            File tempDir = new File(mc.field_71412_D + File.separator + "keystrokes" + File.separator + "scripts", "compiler_temp");
            return !tempDir.exists() && !tempDir.mkdirs() ? tempDirStr : tempDir.getAbsolutePath();
        } else {
            return tempDirStr;
        }
    }

    public static boolean addFriend(String name) {
        if (Raven.playerRelationsManager != null) {
            return Raven.playerRelationsManager.addFriend(name);
        } else if (friends.add(name.toLowerCase())) {
            sendMessage("&7Added &afriend&7: &b" + name);
            if (enemies.contains(name.toLowerCase())) {
                enemies.remove(name.toLowerCase());
            }
            return true;
        } else {
            return false;
        }
    }

    public static String getDistanceColor(double distance) {
        if (distance < 10.0) {
            return "&c";
        } else if (distance < 25.0) {
            return "&6";
        } else if (distance < 50.0) {
            return "&e";
        } else {
            return distance > 100.0 ? "&2" : "&a";
        }
    }

    public static boolean isWholeNumber(double num) {
        return num == Math.floor(num);
    }

    public static String asWholeNum(double input) {
        return isWholeNumber(input) ? (int) input + "" : String.valueOf(input);
    }

    public static int asWholeNumInt(double input) {
        return isWholeNumber(input) ? (int) input : (int) input;
    }

    public static int randomizeInt(int min, int max) {
        return rand.nextInt(max - min + 1) + min;
    }

    public static double randomizeDouble(double min, double max) {
        return min + (max - min) * rand.nextDouble();
    }

    public static boolean inFov(float fov, BlockPos blockPos) {
        return inFov(fov, (float) blockPos.func_177958_n(), (float) blockPos.func_177952_p());
    }

    public static boolean inFov(float fov, Entity entity) {
        return inFov(fov, entity.field_70165_t, entity.field_70161_v);
    }

    public static boolean inFov(float fov, double posX, double posZ) {
        return inFov(mc.field_71439_g, fov, posX, posZ);
    }

    public static boolean inFov(Entity viewPoint, float fov, double posX, double posZ) {
        fov = (float) (fov * 0.5);
        double wrapAngleTo180_double = MathHelper.func_76138_g((viewPoint.field_70177_z - RotationUtils.angle(posX, posZ)) % 360.0F);
        if (wrapAngleTo180_double > 0.0) {
            if (wrapAngleTo180_double < fov) {
                return true;
            }
        } else if (wrapAngleTo180_double > -fov) {
            return true;
        }
        return false;
    }

    public static boolean inFov(float origin, float fov, float targetYaw) {
        fov *= 0.5F;
        double wrapAngleTo180_double = MathHelper.func_76138_g((origin - targetYaw) % 360.0F);
        return wrapAngleTo180_double > 0.0 ? wrapAngleTo180_double < fov : wrapAngleTo180_double > -fov;
    }

    public static Vec3 getLookVec(float yaw, float pitch) {
        float f = MathHelper.func_76134_b(-yaw * (float) (Math.PI / 180.0) - (float) Math.PI);
        float f1 = MathHelper.func_76126_a(-yaw * (float) (Math.PI / 180.0) - (float) Math.PI);
        float f2 = -MathHelper.func_76134_b(-pitch * (float) (Math.PI / 180.0));
        float f3 = MathHelper.func_76126_a(-pitch * (float) (Math.PI / 180.0));
        return new Vec3(f1 * f2, f3, f * f2);
    }

    public static boolean holdingBow() {
        return mc.field_71439_g.func_70694_bm() == null ? false : mc.field_71439_g.func_70694_bm().func_77973_b() instanceof ItemBow;
    }

    public static boolean bowBackwards() {
        return holdingBow() && mc.field_71439_g.field_70702_br == 0.0F && mc.field_71439_g.field_70701_bs <= 0.0F && isMoving();
    }

    public static boolean noSlowingBackWithBow() {
        return ModuleManager.noSlow.noSlowing && bowBackwards();
    }

    public static void sendMessage(String txt) {
        if (nullCheck()) {
            String m = formatColor("&7[&dR&7]&r " + txt);
            mc.field_71439_g.func_145747_a(new ChatComponentText(m));
        }
    }

    public static void sendMessageStr(String txt) {
        if (nullCheck()) {
            String m = formatColor("&7[&dR&7]&r " + txt);
            mc.field_71439_g.func_145747_a(new ChatComponentText(m));
        }
    }

    public static void sendMessage(Object object) {
        String toString = String.valueOf(object);
        sendMessage(toString);
    }

    public static void sendDebugMessage(String message) {
        if (nullCheck()) {
            mc.field_71439_g.func_145747_a(new ChatComponentText("§7[§dR§7]§r " + message));
        }
    }

    public static void attackEntity(Entity e, boolean clientSwing, boolean silentSwing) {
        if (clientSwing) {
            mc.field_71439_g.func_71038_i();
        } else if (silentSwing || !silentSwing && !clientSwing) {
            mc.field_71439_g.field_71174_a.func_147297_a(new C0APacketAnimation());
        }
        mc.field_71442_b.func_78764_a(mc.field_71439_g, e);
    }

    public static void sendRawMessage(String txt) {
        if (nullCheck()) {
            mc.field_71439_g.func_145747_a(new ChatComponentText(formatColor(txt)));
        }
    }

    public static float getTotalHealth(EntityLivingBase entity) {
        return entity.func_110143_aJ() + entity.func_110139_bj();
    }

    public static String getHealthStr(EntityLivingBase entity, boolean accountDead) {
        float totalHealth = getTotalHealth(entity);
        if (accountDead && entity.field_70128_L) {
            totalHealth = 0.0F;
        }
        return getColorForHealth(entity.func_110143_aJ() / entity.func_110138_aP(), totalHealth);
    }

    public static boolean isBindDown(KeyBinding keyBinding) {
        int keyCode = keyBinding.func_151463_i();
        return keyCode < 0 ? Mouse.isButtonDown(keyCode + 100) : Keyboard.isKeyDown(keyCode);
    }

    public static int getTool(Block block) {
        double bestScore = 1.0;
        int bestSlot = -1;
        for (int i = 0; i < InventoryPlayer.func_70451_h(); i++) {
            ItemStack getStackInSlot = mc.field_71439_g.field_71071_by.func_70301_a(i);
            if (getStackInSlot != null) {
                double score = ItemSortScoring.getBlockBreakingScore(getStackInSlot, block);
                if (score > bestScore) {
                    bestScore = score;
                    bestSlot = i;
                }
            }
        }
        return bestSlot;
    }

    public static boolean onLadder(Entity entity) {
        int posX = MathHelper.func_76128_c(entity.field_70165_t);
        int posY = MathHelper.func_76128_c(entity.field_70163_u - 0.2F);
        int posZ = MathHelper.func_76128_c(entity.field_70161_v);
        BlockPos blockpos = new BlockPos(posX, posY, posZ);
        Block block1 = mc.field_71441_e.func_180495_p(blockpos).func_177230_c();
        return block1 instanceof BlockLadder && !entity.field_70122_E;
    }

    public static float getEfficiency(ItemStack itemStack, Block block) {
        float getStrVsBlock = itemStack.func_150997_a(block);
        if (getStrVsBlock > 1.0F) {
            int getEnchantmentLevel = EnchantmentHelper.func_77506_a(Enchantment.field_77349_p.field_77352_x, itemStack);
            if (getEnchantmentLevel > 0) {
                getStrVsBlock += getEnchantmentLevel * getEnchantmentLevel + 1;
            }
        }
        return getStrVsBlock;
    }

    public static boolean isEnemy(EntityPlayer entityPlayer) {
        return entityPlayer != null && isEnemy(entityPlayer.func_70005_c_());
    }

    public static boolean isEnemy(String name) {
        if (ModuleManager.relationships != null && !ModuleManager.relationships.isEnabled()) {
            return false;
        } else {
            return Raven.playerRelationsManager != null ? Raven.playerRelationsManager.isEnemy(name) : name != null && !enemies.isEmpty() && enemies.contains(name.toLowerCase());
        }
    }

    public static String getColorForHealth(double percent, double totalHealth) {
        if (Settings.showHealthAsHearts.isToggled()) {
            totalHealth /= 2.0;
        }
        double health = round(totalHealth, 1);
        String healthStr = (percent < 0.3 ? "§c" : (percent < 0.5 ? "§6" : (percent < 0.7 ? "§e" : "§a"))) + asWholeNum(health);
        if (Settings.showHeartSymbol.isToggled()) {
            healthStr = healthStr + "§c❤§r";
        }
        return healthStr;
    }

    public static int getColorForHealth(double health) {
        return health < 0.3 ? -43691 : (health < 0.5 ? -22016 : (health < 0.7 ? -171 : -11141291));
    }

    public static String formatColor(String txt) {
        return txt.replaceAll("&", "§");
    }

    public static String getFirstColorCode(String input) {
        if (input != null && input.length() >= 2) {
            for (int i = 0; i < input.length() - 1; i++) {
                if (input.charAt(i) == 167) {
                    char c = input.charAt(i + 1);
                    if (c >= '0' && c <= '9' || c >= 'a' && c <= 'f' || c >= 'A' && c <= 'F') {
                        return "§" + c;
                    }
                }
            }
            return "";
        } else {
            return "";
        }
    }

    public static int getBoldWidth(String string) {
        boolean bold = false;
        int additionalWidth = 0;
        for (int i = 0; i < string.length(); i++) {
            char c0 = string.charAt(i);
            if (c0 == 167 && i + 1 < string.length()) {
                int i2 = "0123456789abcdefklmnor".indexOf(string.toLowerCase(Locale.ENGLISH).charAt(i + 1));
                if (i2 == 17) {
                    bold = true;
                }
                i++;
            } else if (bold) {
                additionalWidth++;
            }
        }
        return additionalWidth;
    }

    public static void correctValue(SliderSetting c, SliderSetting d) {
        if (c.getInput() > d.getInput()) {
            double p = c.getInput();
            c.setValueWithEvent(d.getInput());
            d.setValueWithEvent(p);
        }
    }

    public static String generateRandomString(int n) {
        char[] array = "abcdefghijklmnopqrstuvwxyz0123456789".toCharArray();
        StringBuilder sb = new StringBuilder();
        IntStream.range(0, n).forEach(p2 -> sb.append(array[rand.nextInt(array.length)]));
        return sb.toString();
    }

    public static boolean isFriended(EntityPlayer entityPlayer) {
        return entityPlayer != null && isFriended(entityPlayer.func_70005_c_());
    }

    public static boolean isFriended(String name) {
        if (ModuleManager.relationships != null && !ModuleManager.relationships.isEnabled()) {
            return false;
        } else {
            return Raven.playerRelationsManager != null ? Raven.playerRelationsManager.isFriend(name) : name != null && !friends.isEmpty() && friends.contains(name.toLowerCase());
        }
    }

    public static double getRandomValue(SliderSetting a, SliderSetting b, Random r) {
        return a.getInput() == b.getInput() ? a.getInput() : a.getInput() + r.nextDouble() * (b.getInput() - a.getInput());
    }

    public static boolean nullCheck() {
        return mc.field_71439_g != null && mc.field_71441_e != null;
    }

    public static boolean isHypixel() {
        boolean isHypixel = !mc.func_71356_B() && mc.func_147104_D() != null && mc.func_147104_D().field_78845_b.contains("hypixel.net");
        if (!isHypixel) {
            List<String> scoreboard = getSidebarLines();
            isHypixel = !scoreboard.isEmpty() && stripString(scoreboard.get(scoreboard.size() - 1)).equals("www.hypixel.net");
        }
        return isHypixel;
    }

    public static String getHitsToKillStr(EntityPlayer entityPlayer, ItemStack itemStack) {
        int n = (int) Math.ceil(getHitsToKill(entityPlayer, itemStack));
        return "§" + (n <= 1 ? "c" : (n <= 3 ? "6" : (n <= 5 ? "e" : "a"))) + n;
    }

    public static double getHitsToKill(EntityPlayer target, ItemStack usedItem) {
        double heldItemDamageLevel = 1.0;
        if (usedItem != null && (usedItem.func_77973_b() instanceof ItemSword || usedItem.func_77973_b() instanceof ItemAxe)) {
            heldItemDamageLevel += getDamageLevel(usedItem);
        }
        double armorProtPercentage = 0.0;
        double totalEPF = 0.0;
        for (int i = 0; i < 4; i++) {
            ItemStack stack = target.field_71071_by.func_70440_f(i);
            if (stack != null && stack.func_77973_b() instanceof ItemArmor) {
                armorProtPercentage += ((ItemArmor) stack.func_77973_b()).field_77879_b * 0.04;
                int protLevel = EnchantmentHelper.func_77506_a(Enchantment.field_180310_c.field_77352_x, stack);
                if (protLevel != 0) {
                    double epf = Math.floor(0.75 * (6 + protLevel * protLevel) / 3.0);
                    totalEPF += epf;
                }
            }
        }
        totalEPF = 0.04 * Math.min(Math.ceil(Math.min(totalEPF, 25.0) * 0.75), 20.0);
        double armorReduction = armorProtPercentage + totalEPF * (1.0 - armorProtPercentage);
        double damage = heldItemDamageLevel * (1.0 - armorReduction);
        double hitsToKill = getTotalHealth(target) / damage;
        return round(hitsToKill, 1);
    }

    public static float n() {
        return ae(mc.field_71439_g.field_70177_z, mc.field_71439_g.field_71158_b.field_78900_b, mc.field_71439_g.field_71158_b.field_78902_a);
    }

    public static String extractFileName(String name) {
        int intValue = name.indexOf("_");
        int intValue2 = name.lastIndexOf("_");
        return intValue != -1 && intValue2 != -1 && intValue2 > intValue ? name.substring(intValue + 1, intValue2) : name;
    }

    public static int mergeAlpha(int color, int alpha) {
        return color & 16777215 | alpha << 24;
    }

    public static int clamp(int n) {
        if (n > 255) {
            return 255;
        } else {
            return n < 4 ? 4 : n;
        }
    }

    public static boolean hasArrows(ItemStack stack) {
        boolean flag = mc.field_71439_g.field_71075_bZ.field_75098_d || EnchantmentHelper.func_77506_a(Enchantment.field_77342_w.field_77352_x, stack) > 0;
        return flag || mc.field_71439_g.field_71071_by.func_146028_b(Items.field_151032_g);
    }

    public static int darkenColor(int color, double percent) {
        int alpha = color >> 24 & 0xFF;
        int red = color >> 16 & 0xFF;
        int green = color >> 8 & 0xFF;
        int blue = color & 0xFF;
        percent = (100.0 - percent) / 100.0;
        red = (int) (red * percent);
        green = (int) (green * percent);
        blue = (int) (blue * percent);
        red = clamp(red);
        green = clamp(green);
        blue = clamp(blue);
        return alpha << 24 | red << 16 | green << 8 | blue;
    }

    public static boolean isTeammate(Entity entity) {
        try {
            if (mc.field_71439_g.func_142014_c((EntityLivingBase) entity) || mc.field_71439_g.func_145748_c_().func_150260_c().startsWith(entity.func_145748_c_().func_150260_c().substring(0, 2)) || getNetworkDisplayName().startsWith(entity.func_145748_c_().func_150260_c().substring(0, 2))) {
                return true;
            }
        } catch (Exception exception) {
        }
        return false;
    }

    public static String getNetworkDisplayName() {
        try {
            NetworkPlayerInfo playerInfo = mc.func_147114_u().func_175102_a(mc.field_71439_g.func_110124_au());
            return ScorePlayerTeam.func_96667_a(playerInfo.func_178850_i(), playerInfo.func_178845_a().getName());
        } catch (Exception exception2) {
            return "";
        }
    }

    public static void setSpeed(double n) {
        if (n == 0.0) {
            mc.field_71439_g.field_70179_y = 0.0;
            mc.field_71439_g.field_70159_w = 0.0;
        } else {
            float n3 = n();
            mc.field_71439_g.field_70159_w = -Math.sin(n3) * n;
            mc.field_71439_g.field_70179_y = Math.cos(n3) * n;
        }
    }

    public static void resetTimer() {
        ((IAccessorMinecraft) mc).getTimer().field_74278_d = 1.0F;
    }

    public static void beginLocalPlayerSubUpdate() {
        LOCAL_PLAYER_SUB_UPDATE_DEPTH.set(LOCAL_PLAYER_SUB_UPDATE_DEPTH.get() + 1);
    }

    public static void endLocalPlayerSubUpdate() {
        int depth = LOCAL_PLAYER_SUB_UPDATE_DEPTH.get() - 1;
        LOCAL_PLAYER_SUB_UPDATE_DEPTH.set(Math.max(0, depth));
    }

    public static boolean isLocalPlayerSubUpdate() {
        return LOCAL_PLAYER_SUB_UPDATE_DEPTH.get() > 0;
    }

    public static boolean inInventory() {
        return !nullCheck() ? false : mc.field_71462_r != null && mc.field_71439_g.field_71069_bz != null && mc.field_71439_g.field_71069_bz instanceof ContainerPlayer && mc.field_71462_r instanceof GuiInventory;
    }

    public static int getSkyWarsStatus() {
        List<String> sidebar = getSidebarLines();
        if (sidebar.isEmpty()) {
            return -1;
        } else if (!stripColor(sidebar.get(0)).startsWith("SKYWARS")) {
            return -1;
        } else {
            for (String line : sidebar) {
                line = stripColor(line);
                if (line.equals("Waiting...") || line.startsWith("Starting in ")) {
                    return 1;
                }
                if (line.startsWith("Players left: ")) {
                    return 2;
                }
            }
            return 0;
        }
    }

    public static String getString(JsonObject type, String member) {
        try {
            return type.get(member).getAsString();
        } catch (Exception exception3) {
            return "";
        }
    }

    public static int getBedwarsStatus() {
        if (!nullCheck()) {
            return -1;
        } else {
            Scoreboard scoreboard = mc.field_71441_e.func_96441_U();
            if (scoreboard == null) {
                return -1;
            } else {
                ScoreObjective objective = scoreboard.func_96539_a(1);
                if (objective != null && stripString(objective.func_96678_d()).contains("BED WARS")) {
                    for (String line : getSidebarLines()) {
                        line = stripString(line);
                        String[] parts = line.split("  ");
                        if (parts.length <= 1) {
                            if (!line.equals("Waiting...") && !line.startsWith("Starting in")) {
                                if (!line.startsWith("R Red:") && !line.startsWith("B Blue:")) {
                                    continue;
                                }
                                return 2;
                            }
                            return 1;
                        } else if (parts[1].startsWith("L")) {
                            return 0;
                        }
                    }
                    return -1;
                } else {
                    return -1;
                }
            }
        }
    }

    public static String stripString(String s) {
        char[] nonValidatedString = StringUtils.func_76338_a(s).toCharArray();
        StringBuilder validated = new StringBuilder();
        for (char c : nonValidatedString) {
            if (c < 127 && c > 20) {
                validated.append(c);
            }
        }
        return validated.toString();
    }

    public static List<String> getSidebarLines() {
        List<String> lines = new ArrayList<>();
        if (mc.field_71441_e == null) {
            return lines;
        } else {
            Scoreboard scoreboard = mc.field_71441_e.func_96441_U();
            if (scoreboard == null) {
                return lines;
            } else {
                ScoreObjective objective = scoreboard.func_96539_a(1);
                if (objective == null) {
                    return lines;
                } else {
                    Collection<Score> scores = scoreboard.func_96534_i(objective);
                    List<Score> list = new ArrayList<>();
                    for (Score input : scores) {
                        if (input != null && input.func_96653_e() != null && !input.func_96653_e().startsWith("#")) {
                            list.add(input);
                        }
                    }
                    if (list.size() > 15) {
                        scores = new ArrayList<>(Lists.newArrayList(Iterables.skip(list, list.size() - 15)));
                    } else {
                        scores = list;
                    }
                    int index = 0;
                    for (Score score : scores) {
                        index++;
                        ScorePlayerTeam team = scoreboard.func_96509_i(score.func_96653_e());
                        lines.add(ScorePlayerTeam.func_96667_a(team, score.func_96653_e()));
                        if (index == scores.size()) {
                            lines.add(objective.func_96678_d());
                        }
                    }
                    Collections.reverse(lines);
                    return lines;
                }
            }
        }
    }

    public static Random getRandom() {
        return rand;
    }

    public static boolean isMoving() {
        return mc.field_71439_g.field_70701_bs != 0.0F || mc.field_71439_g.field_70702_br != 0.0F;
    }

    public static void aim(Entity en, float offset, boolean sendPacket) {
        if (en != null) {
            float[] t = getRotationsOld(en);
            if (t != null) {
                float y = t[0];
                float p = t[1] + 4.0F + offset;
                if (sendPacket) {
                    mc.func_147114_u().func_147297_a(new C05PacketPlayerLook(y, p, mc.field_71439_g.field_70122_E));
                } else {
                    mc.field_71439_g.field_70177_z = y;
                    mc.field_71439_g.field_70125_A = p;
                }
            }
        }
    }

    public static float[] getRotationsOld(Entity q) {
        if (q == null) {
            return null;
        } else {
            double diffX = q.field_70165_t - mc.field_71439_g.field_70165_t;
            double diffY;
            if (q instanceof EntityLivingBase) {
                EntityLivingBase en = (EntityLivingBase) q;
                diffY = en.field_70163_u + en.func_70047_e() * 0.9 - (mc.field_71439_g.field_70163_u + mc.field_71439_g.func_70047_e());
            } else {
                diffY = (q.func_174813_aQ().field_72338_b + q.func_174813_aQ().field_72337_e) / 2.0 - (mc.field_71439_g.field_70163_u + mc.field_71439_g.func_70047_e());
            }
            double diffZ = q.field_70161_v - mc.field_71439_g.field_70161_v;
            double dist = MathHelper.func_76133_a(diffX * diffX + diffZ * diffZ);
            float yaw = (float) (Math.atan2(diffZ, diffX) * 180.0 / Math.PI) - 90.0F;
            float pitch = (float) (-(Math.atan2(diffY, dist) * 180.0 / Math.PI));
            return new float[] { mc.field_71439_g.field_70177_z + MathHelper.func_76142_g(yaw - mc.field_71439_g.field_70177_z), mc.field_71439_g.field_70125_A + MathHelper.func_76142_g(pitch - mc.field_71439_g.field_70125_A) };
        }
    }

    public static double aimDifference(Entity en, boolean useServerYaw) {
        return (((useServerYaw ? RotationUtils.serverRotations[0] : mc.field_71439_g.field_70177_z) - getYaw(en)) % 360.0 + 540.0) % 360.0 - 180.0;
    }

    public static double pitchDifference(Entity en, boolean useServerPitch) {
        return (((useServerPitch ? RotationUtils.serverRotations[1] : mc.field_71439_g.field_70125_A) - getPitch(en)) % 360.0 + 540.0) % 360.0 - 180.0;
    }

    public static float getYaw(Entity ent) {
        double x = ent.field_70165_t - mc.field_71439_g.field_70165_t;
        double z = ent.field_70161_v - mc.field_71439_g.field_70161_v;
        double yaw = Math.atan2(x, z) * (180.0 / Math.PI);
        return (float) (yaw * -1.0);
    }

    public static float getPitch(Entity ent) {
        double x = ent.field_70165_t - mc.field_71439_g.field_70165_t;
        double z = ent.field_70161_v - mc.field_71439_g.field_70161_v;
        double y = ent.field_70163_u + ent.func_70047_e() / 2.0F - (mc.field_71439_g.field_70163_u + mc.field_71439_g.func_70047_e());
        double pitch = Math.atan2(y, Math.sqrt(x * x + z * z)) * (180.0 / Math.PI);
        return (float) (pitch * -1.0);
    }

    public static void switchSlot(int slot, boolean instant) {
        mc.field_71439_g.field_71071_by.field_70461_c = slot;
        if (instant) {
            ((IAccessorPlayerControllerMP) mc.field_71442_b).callSyncCurrentPlayItem();
        }
    }

    public static MovingObjectPosition getTarget(double reach, float yaw, float pitch) {
        Vec3 eyeVec = mc.field_71439_g.func_174824_e(1.0F);
        float y = -yaw * (float) (Math.PI / 180.0);
        float p = -pitch * (float) (Math.PI / 180.0);
        float f = MathHelper.func_76134_b(y - (float) Math.PI);
        float f2 = MathHelper.func_76126_a(y - (float) Math.PI);
        float f3 = -MathHelper.func_76134_b(p);
        float f4 = MathHelper.func_76126_a(p);
        Vec3 lookVec = new Vec3(f2 * f3, f4, f * f3);
        Vec3 sumVec = eyeVec.func_72441_c(lookVec.field_72450_a * reach, lookVec.field_72448_b * reach, lookVec.field_72449_c * reach);
        return mc.field_71441_e.func_147447_a(eyeVec, sumVec, false, false, false);
    }

    public static boolean isPossibleToReach(BlockPos pos, double reach) {
        float[] rot = RotationUtils.getRotations(pos);
        Vec3 eyeVec = mc.field_71439_g.func_174824_e(1.0F);
        float y = -rot[0] * (float) (Math.PI / 180.0);
        float p = -rot[1] * (float) (Math.PI / 180.0);
        float f = MathHelper.func_76134_b(y - (float) Math.PI);
        float f2 = MathHelper.func_76126_a(y - (float) Math.PI);
        float f3 = -MathHelper.func_76134_b(p);
        float f4 = MathHelper.func_76126_a(p);
        Vec3 lookVec = new Vec3(f2 * f3, f4, f * f3);
        Vec3 sumVec = eyeVec.func_72441_c(lookVec.field_72450_a * reach, lookVec.field_72448_b * reach, lookVec.field_72449_c * reach);
        AxisAlignedBB axis = BlockUtils.getBlock(pos).func_180640_a(mc.field_71441_e, pos, BlockUtils.getBlockState(pos));
        if (axis == null) {
            return false;
        } else {
            MovingObjectPosition mop = axis.func_72327_a(eyeVec, sumVec);
            return mop != null;
        }
    }

    public static void setSpeed(double val, boolean checkMoving) {
        if (!checkMoving || isMoving()) {
            mc.field_71439_g.field_70159_w = -Math.sin(gd()) * val;
            mc.field_71439_g.field_70179_y = Math.cos(gd()) * val;
        }
    }

    public static boolean keysDown() {
        return Keyboard.isKeyDown(mc.field_71474_y.field_74351_w.func_151463_i()) || Keyboard.isKeyDown(mc.field_71474_y.field_74368_y.func_151463_i()) || Keyboard.isKeyDown(mc.field_71474_y.field_74370_x.func_151463_i()) || Keyboard.isKeyDown(mc.field_71474_y.field_74366_z.func_151463_i());
    }

    public static boolean jumpDown() {
        return Keyboard.isKeyDown(mc.field_71474_y.field_74314_A.func_151463_i());
    }

    public static double distanceToGround(Entity entity) {
        if (entity.field_70122_E) {
            return 0.0;
        } else {
            double fallDistance = -1.0;
            double y = entity.field_70163_u;
            if (entity.field_70163_u % 1.0 == 0.0) {
                y--;
            }
            for (int i = (int) Math.floor(y); i > -1; i--) {
                if (!isPlaceable(new BlockPos(entity.field_70165_t, i, entity.field_70161_v))) {
                    fallDistance = y - i;
                    break;
                }
            }
            return fallDistance - 1.0;
        }
    }

    public static float gd() {
        float yw = mc.field_71439_g.field_70177_z;
        if (mc.field_71439_g.field_70701_bs < 0.0F) {
            yw += 180.0F;
        }
        float f;
        if (mc.field_71439_g.field_70701_bs < 0.0F) {
            f = -0.5F;
        } else if (mc.field_71439_g.field_70701_bs > 0.0F) {
            f = 0.5F;
        } else {
            f = 1.0F;
        }
        if (mc.field_71439_g.field_70702_br > 0.0F) {
            yw -= 90.0F * f;
        }
        if (mc.field_71439_g.field_70702_br < 0.0F) {
            yw += 90.0F * f;
        }
        return yw * (float) (Math.PI / 180.0);
    }

    public static float ae(float n, float n2, float n3) {
        float n4 = 1.0F;
        if (n2 < 0.0F) {
            n += 180.0F;
            n4 = -0.5F;
        } else if (n2 > 0.0F) {
            n4 = 0.5F;
        }
        if (n3 > 0.0F) {
            n -= 90.0F * n4;
        } else if (n3 < 0.0F) {
            n += 90.0F * n4;
        }
        return n * (float) (Math.PI / 180.0);
    }

    public static double getHorizontalSpeed() {
        return getHorizontalSpeed(mc.field_71439_g);
    }

    public static double getHorizontalSpeed(Entity entity) {
        return Math.sqrt(entity.field_70159_w * entity.field_70159_w + entity.field_70179_y * entity.field_70179_y);
    }

    public static List<String> getTopLevelLines(String fileContents) {
        List<String> topLevelLines = new ArrayList<>();
        String[] lines = fileContents.split("\\r?\\n");
        int braceLevel = 0;
        boolean[] inBlockComment = new boolean[] { false };
        for (String line : lines) {
            String processedLine = stripComments(line, inBlockComment).trim();
            if (!processedLine.isEmpty()) {
                String lineWithoutStrings = removeStringLiterals(processedLine);
                int openBraces = 0;
                int closeBraces = 0;
                for (char ch : lineWithoutStrings.toCharArray()) {
                    if (ch == '{') {
                        openBraces++;
                    } else if (ch == '}') {
                        closeBraces++;
                    }
                }
                braceLevel += openBraces - closeBraces;
                if (braceLevel == 0 && !lineWithoutStrings.contains("{") && !lineWithoutStrings.contains("}") && !processedLine.startsWith("@")) {
                    topLevelLines.add(processedLine);
                }
            }
        }
        return topLevelLines;
    }

    private static String stripComments(String line, boolean[] inBlockComment) {
        StringBuilder result = new StringBuilder();
        boolean inString = false;
        boolean inCharacter = false;
        boolean escaped = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            char next = i + 1 < line.length() ? line.charAt(i + 1) : 0;
            if (inBlockComment[0]) {
                if (ch == '*' && next == '/') {
                    inBlockComment[0] = false;
                    i++;
                }
            } else if (!inString && !inCharacter) {
                if (ch == '/' && next == '/') {
                    break;
                }
                if (ch == '/' && next == '*') {
                    inBlockComment[0] = true;
                    i++;
                } else {
                    result.append(ch);
                    if (ch == '"') {
                        inString = true;
                    } else if (ch == '\'') {
                        inCharacter = true;
                    }
                }
            } else {
                result.append(ch);
                if (escaped) {
                    escaped = false;
                } else if (ch == '\\') {
                    escaped = true;
                } else if (inString && ch == '"' || inCharacter && ch == '\'') {
                    inString = false;
                    inCharacter = false;
                }
            }
        }
        return result.toString();
    }

    public static boolean holdingEdible(ItemStack stack) {
        if (stack.func_77973_b() instanceof ItemFood && mc.field_71439_g.func_71024_bL().func_75116_a() == 20) {
            ItemFood food = (ItemFood) stack.func_77973_b();
            return ((IAccessorItemFood) food).getAlwaysEdible();
        } else {
            return true;
        }
    }

    private static String removeStringLiterals(String line) {
        StringBuilder sb = new StringBuilder();
        boolean inString = false;
        boolean inCharacter = false;
        boolean escaped = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (!inString && !inCharacter) {
                if (ch == '"') {
                    inString = true;
                } else if (ch == '\'') {
                    inCharacter = true;
                } else {
                    sb.append(ch);
                }
            } else if (escaped) {
                escaped = false;
            } else if (ch == '\\') {
                escaped = true;
            } else if (inString && ch == '"' || inCharacter && ch == '\'') {
                inString = false;
                inCharacter = false;
            }
        }
        return sb.toString();
    }

    public static boolean blockAbove() {
        return !(BlockUtils.getBlock(new BlockPos(mc.field_71439_g.field_70165_t, mc.field_71439_g.field_70163_u + 2.0, mc.field_71439_g.field_70161_v)) instanceof BlockAir);
    }

    public static boolean onEdge() {
        return onEdge(mc.field_71439_g);
    }

    public static boolean onEdge(Entity entity) {
        return mc.field_71441_e.func_72945_a(entity, entity.func_174813_aQ().func_72317_d(entity.field_70159_w / 3.0, -1.0, entity.field_70179_y / 3.0)).isEmpty();
    }

    public static boolean lookingAtBlock() {
        return mc.field_71476_x != null && mc.field_71476_x.field_72313_a == MovingObjectType.BLOCK && mc.field_71476_x.func_178782_a() != null;
    }

    public static boolean isDiagonal(boolean strict) {
        float yaw = (mc.field_71439_g.field_70177_z % 360.0F + 360.0F) % 360.0F;
        yaw = yaw > 180.0F ? yaw - 360.0F : yaw;
        boolean isYawDiagonal = inBetween(-170.0, 170.0, yaw) && !inBetween(-10.0, 10.0, yaw) && !inBetween(80.0, 100.0, yaw) && !inBetween(-100.0, -80.0, yaw);
        if (strict) {
            isYawDiagonal = inBetween(-178.5, 178.5, yaw) && !inBetween(-1.5, 1.5, yaw) && !inBetween(88.5, 91.5, yaw) && !inBetween(-91.5, -88.5, yaw);
        }
        boolean isStrafing = Keyboard.isKeyDown(mc.field_71474_y.field_74370_x.func_151463_i()) || Keyboard.isKeyDown(mc.field_71474_y.field_74366_z.func_151463_i());
        return isYawDiagonal || isStrafing;
    }

    public static double gbps(Entity en, int d) {
        double x = en.field_70165_t - en.field_70169_q;
        double z = en.field_70161_v - en.field_70166_s;
        double sp = Math.sqrt(x * x + z * z) * 20.0;
        return d == 0 ? sp : round(sp, d);
    }

    public static boolean inBetween(double min, double max, double value) {
        return value >= min && value <= max;
    }

    public static String removeFormatCodes(String str) {
        return str.replace("§k", "").replace("§l", "").replace("§m", "").replace("§n", "").replace("§o", "").replace("§r", "");
    }

    public static boolean isClicking() {
        return ModuleManager.autoClicker != null && ModuleManager.autoClicker.isEnabled() ? Mouse.isButtonDown(0) : MouseHelper.f() > 1 && System.currentTimeMillis() - MouseHelper.LL < 300L;
    }

    public static boolean isMining() {
        int keyCode = mc.field_71474_y.field_74312_F.func_151463_i();
        if (keyCode == 0) {
            return false;
        } else {
            boolean attackDown = keyCode < 0 ? Mouse.isButtonDown(keyCode + 100) : Keyboard.isKeyDown(keyCode);
            if (!attackDown) {
                return false;
            } else {
                double reach = mc.field_71442_b.func_78757_d();
                float yaw = mc.field_71439_g.field_70177_z;
                float pitch = mc.field_71439_g.field_70125_A;
                MovingObjectPosition entityHit = RotationUtils.rayTrace(reach, 1.0F, new float[] { yaw, pitch }, null);
                if (entityHit != null && entityHit.field_72313_a == MovingObjectType.ENTITY) {
                    return false;
                } else {
                    MovingObjectPosition blockHit = RotationUtils.rayCastBlock(reach, yaw, pitch);
                    return blockHit != null && blockHit.field_72313_a == MovingObjectType.BLOCK && blockHit.func_178782_a() != null;
                }
            }
        }
    }

    public static boolean isEdgeOfBlock() {
        BlockPos pos = new BlockPos(mc.field_71439_g.field_70165_t, mc.field_71439_g.field_70163_u - (mc.field_71439_g.field_70163_u % 1.0 == 0.0 ? 1 : 0), mc.field_71439_g.field_70161_v);
        return mc.field_71441_e.func_175623_d(pos);
    }

    public static long timeBetween(long val, long val2) {
        return Math.abs(val2 - val);
    }

    public static void sendModuleMessage(Module module, String s) {
        sendRawMessage("&3" + module.getName() + "&7: &r" + s);
    }

    public static EntityLivingBase raytrace(int range) {
        Entity entity = null;
        EntityPlayer self = (EntityPlayer) (Freecam.freeEntity == null ? mc.field_71439_g : Freecam.freeEntity);
        MovingObjectPosition rayTrace = self.func_174822_a(range, 1.0F);
        Vec3 getPositionEyes = self.func_174824_e(1.0F);
        float rotationYaw = self.field_70177_z;
        float rotationPitch = self.field_70125_A;
        float cos = MathHelper.func_76134_b(-rotationYaw * (float) (Math.PI / 180.0) - (float) Math.PI);
        float sin = MathHelper.func_76126_a(-rotationYaw * (float) (Math.PI / 180.0) - (float) Math.PI);
        float n2 = -MathHelper.func_76134_b(-rotationPitch * (float) (Math.PI / 180.0));
        Vec3 vec3 = new Vec3(sin * n2, MathHelper.func_76126_a(-rotationPitch * (float) (Math.PI / 180.0)), cos * n2);
        Vec3 addVector = getPositionEyes.func_72441_c(vec3.field_72450_a * range, vec3.field_72448_b * range, vec3.field_72449_c * range);
        Vec3 vec4 = null;
        List getEntitiesWithinAABBExcludingEntity = mc.field_71441_e.func_72839_b(mc.func_175606_aa(), mc.func_175606_aa().func_174813_aQ().func_72321_a(vec3.field_72450_a * range, vec3.field_72448_b * range, vec3.field_72449_c * range).func_72314_b(1.0, 1.0, 1.0));
        double n3 = range;
        for (int i = 0; i < getEntitiesWithinAABBExcludingEntity.size(); i++) {
            Entity entity2 = (Entity) getEntitiesWithinAABBExcludingEntity.get(i);
            if (entity2.func_70067_L()) {
                float getCollisionBorderSize = entity2.func_70111_Y();
                AxisAlignedBB expand = entity2.func_174813_aQ().func_72314_b(getCollisionBorderSize, getCollisionBorderSize, getCollisionBorderSize);
                MovingObjectPosition calculateIntercept = expand.func_72327_a(getPositionEyes, addVector);
                if (expand.func_72318_a(getPositionEyes)) {
                    if (0.0 < n3 || n3 == 0.0) {
                        entity = entity2;
                        vec4 = calculateIntercept == null ? getPositionEyes : calculateIntercept.field_72307_f;
                        n3 = 0.0;
                    }
                } else if (calculateIntercept != null) {
                    double distanceTo = getPositionEyes.func_72438_d(calculateIntercept.field_72307_f);
                    if (distanceTo < n3 || n3 == 0.0) {
                        if (entity2 != mc.func_175606_aa().field_70154_o || entity2.canRiderInteract()) {
                            entity = entity2;
                            vec4 = calculateIntercept.field_72307_f;
                            n3 = distanceTo;
                        } else if (n3 == 0.0) {
                            entity = entity2;
                            vec4 = calculateIntercept.field_72307_f;
                        }
                    }
                }
            }
        }
        if (entity != null && (n3 < range || rayTrace == null)) {
            rayTrace = new MovingObjectPosition(entity, vec4);
        }
        return rayTrace != null && rayTrace.field_72313_a == MovingObjectType.ENTITY && rayTrace.field_72308_g instanceof EntityLivingBase ? (EntityLivingBase) rayTrace.field_72308_g : null;
    }

    public static int getChroma(long speed, long... delay) {
        long time = System.currentTimeMillis() + (delay.length > 0 ? delay[0] : 0L);
        return Color.getHSBColor((float) (time % (15000L / speed)) / (15000.0F / (float) speed), 1.0F, 1.0F).getRGB();
    }

    public static double round(double val, int decimalPlaces) {
        if (decimalPlaces == 0) {
            return Math.round(val);
        } else {
            double p = Math.pow(10.0, decimalPlaces);
            return Math.round(val * p) / p;
        }
    }

    public static String stripColor(String string) {
        if (string.isEmpty()) {
            return string;
        } else {
            char[] array = StringUtils.func_76338_a(string).toCharArray();
            StringBuilder sb = new StringBuilder();
            for (char c : array) {
                if (c < 127 && c > 20) {
                    sb.append(c);
                }
            }
            return sb.toString();
        }
    }

    public static List<String> getScoreBoardOld() {
        List<String> lines = new ArrayList<>();
        if (mc.field_71441_e == null) {
            return lines;
        } else {
            Scoreboard scoreboard = mc.field_71441_e.func_96441_U();
            if (scoreboard == null) {
                return lines;
            } else {
                ScoreObjective objective = scoreboard.func_96539_a(1);
                if (objective == null) {
                    return lines;
                } else {
                    Collection<Score> scores = scoreboard.func_96534_i(objective);
                    List<Score> list = new ArrayList<>();
                    for (Score score : scores) {
                        if (score != null && score.func_96653_e() != null && !score.func_96653_e().startsWith("#")) {
                            list.add(score);
                        }
                    }
                    if (list.size() > 15) {
                        scores = Lists.newArrayList(Iterables.skip(list, scores.size() - 15));
                    } else {
                        scores = list;
                    }
                    for (Score score2 : scores) {
                        ScorePlayerTeam team = scoreboard.func_96509_i(score2.func_96653_e());
                        lines.add(ScorePlayerTeam.func_96667_a(team, score2.func_96653_e()));
                    }
                    return lines;
                }
            }
        }
    }

    public static void setSwinging() {
        int armSwingEnd = mc.field_71439_g.func_70644_a(Potion.field_76422_e) ? 6 - (1 + mc.field_71439_g.func_70660_b(Potion.field_76422_e).func_76458_c()) : (mc.field_71439_g.func_70644_a(Potion.field_76419_f) ? 6 + (1 + mc.field_71439_g.func_70660_b(Potion.field_76419_f).func_76458_c()) * 2 : 6);
        if (!mc.field_71439_g.field_82175_bq || mc.field_71439_g.field_110158_av >= armSwingEnd / 2 || mc.field_71439_g.field_110158_av < 0) {
            mc.field_71439_g.field_110158_av = -1;
            mc.field_71439_g.field_82175_bq = true;
        }
    }

    public static String uppercaseFirst(String s) {
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    public static boolean isPlaceable(BlockPos blockPos) {
        return BlockUtils.replaceable(blockPos) || BlockUtils.isFluid(BlockUtils.getBlock(blockPos));
    }

    public static boolean spectatorCheck() {
        return mc.field_71439_g.field_71071_by.func_70301_a(8) != null && mc.field_71439_g.field_71071_by.func_70301_a(8).func_82833_r().contains("Return") || stripString(((IAccessorGuiIngame) mc.field_71456_v).getDisplayedTitle()).contains("YOU DIED");
    }

    public static boolean holdingWeapon() {
        return holdingWeapon(mc.field_71439_g);
    }

    public static boolean holdingWeapon(EntityLivingBase entityLivingBase) {
        if (entityLivingBase.func_70694_bm() == null) {
            return Settings.weaponFist.isToggled();
        } else {
            Item getItem = entityLivingBase.func_70694_bm().func_77973_b();
            return getItem instanceof ItemSword || Settings.weaponAxe.isToggled() && getItem instanceof ItemAxe || Settings.weaponRod.isToggled() && getItem instanceof ItemFishingRod || Settings.weaponStick.isToggled() && getItem == Items.field_151055_y || Settings.weaponHoe.isToggled() && getItem instanceof ItemHoe || Settings.weaponShovel.isToggled() && getItem instanceof ItemSpade || Settings.weaponEnchanted.isToggled() && entityLivingBase.func_70694_bm().func_77948_v();
        }
    }

    public static boolean holdingSword() {
        return mc.field_71439_g.func_70694_bm() == null ? false : mc.field_71439_g.func_70694_bm().func_77973_b() instanceof ItemSword;
    }

    public static double getDamageLevel(ItemStack itemStack) {
        return ItemSortScoring.getMeleeDamage(itemStack);
    }

    public static float getDirection() {
        return getCustomDirection(mc.field_71439_g.field_70177_z, mc.field_71439_g.field_71158_b.field_78900_b, mc.field_71439_g.field_71158_b.field_78902_a);
    }

    public static boolean isUserMoving() {
        return mc.field_71439_g.field_71158_b.field_78900_b != 0.0F || mc.field_71439_g.field_71158_b.field_78902_a != 0.0F;
    }

    public static float getCustomDirection(float yaw, float moveForward, float moveStrafe) {
        float forward = 1.0F;
        if (moveForward < 0.0F) {
            yaw += 180.0F;
            forward = -0.5F;
        } else if (moveForward > 0.0F) {
            forward = 0.5F;
        }
        if (moveStrafe > 0.0F) {
            yaw -= 90.0F * forward;
        } else if (moveStrafe < 0.0F) {
            yaw += 90.0F * forward;
        }
        return yaw * (float) (Math.PI / 180.0);
    }

    public static boolean canBePlaced(ItemBlock itemBlock) {
        Block block = itemBlock.func_179223_d();
        return block == null ? false : !BlockUtils.isInteractable(block) && !(block instanceof BlockSnow) && !(block instanceof BlockWeb) && !(block instanceof BlockSapling) && !(block instanceof BlockDaylightDetector) && !(block instanceof BlockBeacon) && !(block instanceof BlockBanner) && !(block instanceof BlockEndPortalFrame) && !(block instanceof BlockEndPortal) && !(block instanceof BlockLever) && !(block instanceof BlockButton) && !(block instanceof BlockSkull) && !(block instanceof BlockLiquid) && !(block instanceof BlockCactus) && !(block instanceof BlockDoublePlant) && !(block instanceof BlockLilyPad) && !(block instanceof BlockCarpet) && !(block instanceof BlockTripWire) && !(block instanceof BlockTripWireHook) && !(block instanceof BlockTallGrass) && !(block instanceof BlockFlower) && !(block instanceof BlockFlowerPot) && !(block instanceof BlockSign) && !(block instanceof BlockLadder) && !(block instanceof BlockTorch) && !(block instanceof BlockRedstoneTorch) && !(block instanceof BlockStairs) && !(block instanceof BlockSlab) && !(block instanceof BlockFence) && !(block instanceof BlockPane) && !(block instanceof BlockStainedGlassPane) && !(block instanceof BlockGravel) && !(block instanceof BlockClay) && !(block instanceof BlockSand) && !(block instanceof BlockSoulSand) && !(block instanceof BlockRailBase);
    }

    public static <E extends Enum<E>> E getEnum(Class<E> enumClass, String value) {
        for (E enumConstant : enumClass.getEnumConstants()) {
            if (enumConstant.name().equals(value)) {
                return enumConstant;
            }
        }
        return null;
    }

    public static int getSpeedAmplifier() {
        return mc.field_71439_g.func_70644_a(Potion.field_76424_c) ? 1 + mc.field_71439_g.func_70660_b(Potion.field_76424_c).func_76458_c() : 0;
    }

    public static ItemStack getSpoofedItem(ItemStack original) {
        Scaffold scaffold = (Scaffold) ModuleManager.getModule(Scaffold.class);
        if (scaffold != null && mc.field_71439_g != null && scaffold.getSpoofedSlot() != -1) {
            return mc.field_71439_g.field_71071_by.func_70301_a(scaffold.getSpoofedSlot());
        } else {
            return ModuleManager.autoTool != null && ModuleManager.autoTool.isEnabled() && ModuleManager.autoTool.spoofItem.isToggled() && mc.field_71439_g != null ? mc.field_71439_g.field_71071_by.func_70301_a(ModuleManager.autoTool.previousSlot == -1 ? mc.field_71439_g.field_71071_by.field_70461_c : ModuleManager.autoTool.previousSlot) : original;
        }
    }

    public static boolean scaffoldDiagonal(boolean strict) {
        return false;
    }

    public static String readInputStream(InputStream inputStream) {
        StringBuilder stringBuilder = new StringBuilder();
        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line).append('\n');
            }
        } catch (Exception exception4) {
            exception4.printStackTrace();
        }
        return stringBuilder.toString();
    }

    public static boolean isLobby() {
        if (isHypixel()) {
            List<String> sidebarLines = getSidebarLines();
            if (!sidebarLines.isEmpty()) {
                String[] parts = stripColor(sidebarLines.get(1)).split("  ");
                if (parts.length > 1 && parts[1].charAt(0) == 'L') {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isBedwarsPracticeOrReplay() {
        if (isHypixel()) {
            if (!nullCheck()) {
                return false;
            } else {
                Scoreboard scoreboard = mc.field_71441_e.func_96441_U();
                if (scoreboard == null) {
                    return false;
                } else {
                    ScoreObjective objective = scoreboard.func_96539_a(1);
                    if (objective == null) {
                        return false;
                    } else {
                        String stripped = stripString(objective.func_96678_d());
                        return stripped.contains("BED WARS PRACTICE") || stripped.contains("REPLAY");
                    }
                }
            }
        } else {
            return false;
        }
    }

    public static Vec3 getClosestPlayerPos(double maxDistSq) {
        if (mc.field_71441_e != null && mc.field_71439_g != null) {
            Vec3 closest = null;
            double bestDist = maxDistSq;
            for (EntityPlayer player : mc.field_71441_e.field_73010_i) {
                if (player != mc.field_71439_g && mc.func_147114_u() != null && mc.func_147114_u().func_175102_a(player.func_110124_au()) != null) {
                    double dx = player.field_70165_t - mc.field_71439_g.field_70165_t;
                    double dy = player.field_70163_u - mc.field_71439_g.field_70163_u;
                    double dz = player.field_70161_v - mc.field_71439_g.field_70161_v;
                    double dist = dx * dx + dy * dy + dz * dz;
                    if (dist < bestDist) {
                        bestDist = dist;
                        closest = new Vec3(player.field_70165_t, player.field_70163_u, player.field_70161_v);
                    }
                }
            }
            return closest;
        } else {
            return null;
        }
    }
}
