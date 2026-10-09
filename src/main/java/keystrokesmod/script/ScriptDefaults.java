package keystrokesmod.script;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import keystrokesmod.Raven;
import keystrokesmod.clickgui.ClickGui;
import keystrokesmod.clickgui.components.impl.CategoryComponent;
import keystrokesmod.clickgui.components.impl.ModuleComponent;
import keystrokesmod.helper.RotationHelper;
import keystrokesmod.mixin.impl.accessor.IAccessorEntityPlayer;
import keystrokesmod.mixin.impl.accessor.IAccessorEntityRenderer;
import keystrokesmod.mixin.impl.accessor.IAccessorGuiIngame;
import keystrokesmod.mixin.impl.accessor.IAccessorGuiPlayerTabOverlay;
import keystrokesmod.mixin.impl.accessor.IAccessorGuiScreenBook;
import keystrokesmod.mixin.impl.accessor.IAccessorItemRenderer;
import keystrokesmod.mixin.impl.accessor.IAccessorMinecraft;
import keystrokesmod.mixin.impl.accessor.IAccessorNetworkManager;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.combat.AimAssist;
import keystrokesmod.module.impl.combat.KillAura;
import keystrokesmod.module.impl.network.Backtrack;
import keystrokesmod.module.impl.network.LagRange;
import keystrokesmod.module.setting.Setting;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.ColorSetting;
import keystrokesmod.module.setting.impl.DescriptionSetting;
import keystrokesmod.module.setting.impl.GroupSetting;
import keystrokesmod.module.setting.impl.KeySetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.script.model.Block;
import keystrokesmod.script.model.Bridge;
import keystrokesmod.script.model.Entity;
import keystrokesmod.script.model.Image;
import keystrokesmod.script.model.ItemStack;
import keystrokesmod.script.model.Message;
import keystrokesmod.script.model.NetworkPlayer;
import keystrokesmod.script.model.PlayerState;
import keystrokesmod.script.model.TileEntity;
import keystrokesmod.script.model.Vec3;
import keystrokesmod.script.packet.clientbound.SPacket;
import keystrokesmod.script.packet.serverbound.CPacket;
import keystrokesmod.script.packet.serverbound.PacketHandler;
import keystrokesmod.utility.BlockUtils;
import keystrokesmod.utility.PacketUtils;
import keystrokesmod.utility.ReflectionUtils;
import keystrokesmod.utility.RenderUtils;
import keystrokesmod.utility.RotationUtils;
import keystrokesmod.utility.Theme;
import keystrokesmod.utility.Utils;
import keystrokesmod.utility.shader.BlurUtils;
import keystrokesmod.utility.shader.RoundedUtils;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiScreenBook;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.resources.ResourcePackRepository.Entry;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.ContainerWorkbench;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemBlock;
import net.minecraft.network.Packet;
import net.minecraft.realms.RealmsBridge;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

public class ScriptDefaults {

    private static ExecutorService cachedExecutor;

    private static final Minecraft mc = Minecraft.func_71410_x();

    public static final Bridge bridge = new Bridge();

    private static final LinkedHashMap<String, Module> modulesMap = new LinkedHashMap<>();

    public static void reloadModules() {
        modulesMap.clear();
        for (Module module : Raven.getModuleManager().getModules()) {
            modulesMap.put(module.getName(), module);
        }
        for (Module module : Raven.scriptManager.scripts.values()) {
            modulesMap.put(module.getName(), module);
        }
    }

    public static class client {

        public static boolean allowFlying() {
            return ScriptDefaults.mc.field_71439_g.field_71075_bZ.field_75101_c;
        }

        public static void setItemInUse(boolean inUse) {
            ReflectionUtils.setItemInUse(inUse);
        }

        public static void setTabName(String name, String displayName) {
            if (name != null && displayName != null && ScriptDefaults.mc.func_147114_u() != null) {
                for (NetworkPlayerInfo info : ScriptDefaults.mc.func_147114_u().func_175106_d()) {
                    if (info.func_178845_a() != null && name.equalsIgnoreCase(info.func_178845_a().getName())) {
                        info.func_178859_a(new ChatComponentText(displayName));
                        return;
                    }
                }
            }
        }

        public static long nanoTime() {
            return System.nanoTime();
        }

        public static void removePotionEffect(int id) {
            if (ScriptDefaults.mc.field_71439_g != null) {
                ScriptDefaults.mc.field_71439_g.func_70618_n(id);
            }
        }

        public static int getUID() {
            return 4;
        }

        public static String getUser() {
            return ScriptDefaults.mc.field_71439_g.func_70005_c_();
        }

        public static void addEnemy(String username) {
            Utils.addEnemy(username);
        }

        public static void addFriend(String username) {
            Utils.addFriend(username);
        }

        public static void async(Runnable method) {
            if (ScriptDefaults.cachedExecutor == null) {
                ScriptDefaults.cachedExecutor = Executors.newCachedThreadPool();
            }
            ScriptDefaults.cachedExecutor.execute(method);
        }

        public static int getFPS() {
            return Minecraft.func_175610_ah();
        }

        public static void chat(String message) {
            ScriptDefaults.mc.field_71439_g.func_71165_d(message);
        }

        public static void print(String string) {
            Utils.sendRawMessage(string);
        }

        public static void print(Message component) {
            ScriptDefaults.mc.field_71439_g.func_145747_a(component.component);
        }

        public static void print(Object object) {
            String s = String.valueOf(object);
            Utils.sendRawMessage(s);
        }

        public static boolean isDiagonal() {
            return Utils.isDiagonal(false);
        }

        public static void setTimer(float timer) {
            ((IAccessorMinecraft) ScriptDefaults.mc).getTimer().field_74278_d = timer;
        }

        public static void setTimer(double timer) {
            ((IAccessorMinecraft) ScriptDefaults.mc).getTimer().field_74278_d = (float) timer;
        }

        public static boolean isCreative() {
            return ScriptDefaults.mc.field_71439_g.field_71075_bZ.field_75098_d;
        }

        public static void processPacket(SPacket packet) {
            packet.packet.func_148833_a(((IAccessorNetworkManager) ScriptDefaults.mc.func_147114_u().func_147298_b()).getPacketListener());
        }

        public static void multiplyMotion(double factor) {
            ScriptDefaults.mc.field_71439_g.field_70179_y *= factor;
            ScriptDefaults.mc.field_71439_g.field_70159_w *= factor;
        }

        public static void processPacketNoEvent(SPacket packet) {
            PacketUtils.receivePacketNoEvent(packet.packet);
        }

        public static String getTitle() {
            return ((IAccessorGuiIngame) ScriptDefaults.mc.field_71456_v).getDisplayedTitle();
        }

        public static String getSubTitle() {
            return ((IAccessorGuiIngame) ScriptDefaults.mc.field_71456_v).getDisplayedSubTitle();
        }

        public static String getRecordPlaying() {
            return ((IAccessorGuiIngame) ScriptDefaults.mc.field_71456_v).getRecordPlaying();
        }

        public static boolean isFlying() {
            return ScriptDefaults.mc.field_71439_g.field_71075_bZ.field_75100_b;
        }

        public static void attack(Entity entity) {
            Utils.attackEntity(entity.entity, true, true);
        }

        public static boolean isSinglePlayer() {
            return ScriptDefaults.mc.func_71356_B();
        }

        public static boolean isSpectator() {
            return ScriptDefaults.mc.field_71439_g.func_175149_v();
        }

        public static void setFlying(boolean flying) {
            ScriptDefaults.mc.field_71439_g.field_71075_bZ.field_75100_b = flying;
        }

        public static void setJump(boolean jump) {
            ScriptDefaults.mc.field_71439_g.field_71158_b.field_78901_c = jump;
        }

        public static void setJumping(boolean jump) {
            ScriptDefaults.mc.field_71439_g.func_70637_d(jump);
        }

        public static void setRenderArmPitch(float pitch) {
            ScriptDefaults.mc.field_71439_g.field_71164_i = pitch;
            ScriptDefaults.mc.field_71439_g.field_71155_g = pitch;
        }

        public static float getEquippedProgress() {
            return ((IAccessorItemRenderer) ScriptDefaults.mc.func_175597_ag()).getEquippedProgress();
        }

        public static void disconnect() {
            boolean isLocal = ScriptDefaults.mc.func_71387_A();
            boolean isRealms = ScriptDefaults.mc.func_181540_al();
            ScriptDefaults.mc.field_71441_e.func_72882_A();
            ScriptDefaults.mc.func_71403_a(null);
            if (isLocal) {
                ScriptDefaults.mc.func_147108_a(new GuiMainMenu());
            } else if (isRealms) {
                new RealmsBridge().switchToRealms(new GuiMainMenu());
            } else {
                ScriptDefaults.mc.func_147108_a(new GuiMultiplayer(new GuiMainMenu()));
            }
        }

        public static float getRenderArmPitch() {
            return ScriptDefaults.mc.field_71439_g.field_71155_g;
        }

        public static void setRenderArmYaw(float yaw) {
            ScriptDefaults.mc.field_71439_g.field_71163_h = yaw;
            ScriptDefaults.mc.field_71439_g.field_71154_f = yaw;
        }

        public static float getRenderArmYaw() {
            return ScriptDefaults.mc.field_71439_g.field_71154_f;
        }

        public static long getTotalMemory() {
            return Runtime.getRuntime().totalMemory();
        }

        public static long getFreeMemory() {
            return Runtime.getRuntime().freeMemory();
        }

        public static long getMaxMemory() {
            return Runtime.getRuntime().maxMemory();
        }

        public static List<String[]> getResourcePacks() {
            List<String[]> packs = new ArrayList<>();
            if (ScriptDefaults.mc.func_110438_M().func_110613_c() != null && !ScriptDefaults.mc.func_110438_M().func_110613_c().isEmpty()) {
                for (Entry entry : ScriptDefaults.mc.func_110438_M().func_110613_c()) {
                    packs.add(new String[] { entry.func_110515_d(), entry.func_110519_e() });
                }
            } else {
                packs.add(new String[] { ScriptDefaults.mc.field_110450_ap.func_130077_b(), "" });
            }
            Collections.reverse(packs);
            return packs;
        }

        public static void jump() {
            ScriptDefaults.mc.field_71439_g.func_70664_aZ();
        }

        public static boolean allowEditing() {
            return ScriptDefaults.mc.field_71439_g != null && ScriptDefaults.mc.field_71439_g.field_71075_bZ != null ? ScriptDefaults.mc.field_71439_g.field_71075_bZ.field_75099_e : false;
        }

        public static void setItemInUseCount(int count) {
            ((IAccessorEntityPlayer) ScriptDefaults.mc.field_71439_g).setItemInUseCount(count);
        }

        public static int getItemInUseCount() {
            return ScriptDefaults.mc.field_71439_g.func_71052_bv();
        }

        public static int getItemInUseDuration() {
            return ScriptDefaults.mc.field_71439_g.func_71057_bx();
        }

        public static void log(Object obj) {
            Utils.log.info(obj);
        }

        public static void setSneaking(boolean sneak) {
            ScriptDefaults.mc.field_71439_g.func_70095_a(sneak);
        }

        public static void setSneak(boolean sneak) {
            ScriptDefaults.mc.field_71439_g.field_71158_b.field_78899_d = sneak;
        }

        public static boolean isSneak() {
            return ScriptDefaults.mc.field_71439_g.field_71158_b.field_78899_d;
        }

        public static Entity getPlayer() {
            return ScriptDefaults.mc != null && ScriptDefaults.mc.field_71439_g != null ? Entity.convert(ScriptDefaults.mc.field_71439_g) : null;
        }

        public static void removeEnemy(String username) {
            Utils.removeEnemy(username);
        }

        public static void removeFriend(String username) {
            Utils.removeFriend(username);
        }

        public static boolean isRiding() {
            return ScriptDefaults.mc.field_71439_g.func_70115_ae();
        }

        public static void setRidingEntity(Entity entity) {
            if (entity == null) {
                ScriptDefaults.mc.field_71439_g.field_70154_o = null;
            } else {
                ScriptDefaults.mc.field_71439_g.field_70154_o = entity.entity;
            }
        }

        public static Vec3 getMotion() {
            return new Vec3(ScriptDefaults.mc.field_71439_g.field_70159_w, ScriptDefaults.mc.field_71439_g.field_70181_x, ScriptDefaults.mc.field_71439_g.field_70179_y);
        }

        public static void sleep(long ms) {
            try {
                Thread.sleep(ms);
            } catch (InterruptedException exception) {
            }
        }

        public static void ping() {
            ScriptDefaults.mc.field_71439_g.func_85030_a("note.pling", 1.0F, 1.0F);
        }

        public static void playSound(String name, float volume, float pitch) {
            ScriptDefaults.mc.field_71439_g.func_85030_a(name, volume, pitch);
        }

        public static boolean isMoving() {
            return Utils.isMoving();
        }

        public static boolean isJump() {
            return ScriptDefaults.mc.field_71439_g.field_71158_b.field_78901_c;
        }

        public static float getStrafe() {
            return ScriptDefaults.mc.field_71439_g.field_71158_b.field_78902_a;
        }

        public static void sleep(int ms) {
            try {
                Thread.sleep(ms);
            } catch (Exception exception2) {
                exception2.printStackTrace();
            }
        }

        public static float getForward() {
            return ScriptDefaults.mc.field_71439_g.field_71158_b.field_78900_b;
        }

        public static void closeScreen() {
            if (ScriptDefaults.mc.field_71462_r instanceof ClickGui) {
                ScriptDefaults.mc.func_147108_a(null);
            } else {
                ScriptDefaults.mc.field_71439_g.func_71053_j();
            }
        }

        public static String getScreen() {
            return ScriptDefaults.mc.field_71462_r == null ? "" : ScriptDefaults.mc.field_71462_r.getClass().getSimpleName();
        }

        public static float[] getRotationsToEntity(Entity entity) {
            return RotationUtils.getRotations(entity.entity);
        }

        public static void sendPacket(CPacket packet) {
            Packet packet1 = PacketHandler.convertCPacket(packet);
            if (packet1 != null) {
                ScriptDefaults.mc.field_71439_g.field_71174_a.func_147297_a(packet1);
            }
        }

        public static void sendPacketNoEvent(CPacket packet) {
            Packet packet1 = PacketHandler.convertCPacket(packet);
            if (packet1 != null) {
                PacketUtils.sendPacketNoEvent(packet1);
            }
        }

        public static boolean inFocus() {
            return ScriptDefaults.mc.field_71415_G;
        }

        public static void dropItem(boolean dropStack) {
            ScriptDefaults.mc.field_71439_g.func_71040_bB(dropStack);
        }

        public static void setMotion(double x, double y, double z) {
            ScriptDefaults.mc.field_71439_g.field_70159_w = x;
            ScriptDefaults.mc.field_71439_g.field_70181_x = y;
            ScriptDefaults.mc.field_71439_g.field_70179_y = z;
        }

        public static void setSpeed(double speed) {
            Utils.setSpeed(speed);
        }

        public static void setForward(float forward) {
            ScriptDefaults.mc.field_71439_g.field_71158_b.field_78900_b = forward;
        }

        public static void setStrafe(float strafe) {
            ScriptDefaults.mc.field_71439_g.field_71158_b.field_78902_a = strafe;
        }

        public static String getServerIP() {
            return ScriptDefaults.mc.func_147104_D() != null && !ScriptDefaults.mc.func_71356_B() ? ScriptDefaults.mc.func_147104_D().field_78845_b : "";
        }

        public static int[] getDisplaySize() {
            ScaledResolution scaledResolution = new ScaledResolution(ScriptDefaults.mc);
            return new int[] { scaledResolution.func_78326_a(), scaledResolution.func_78328_b(), scaledResolution.func_78325_e() };
        }

        public static float getServerDirection(PlayerState state) {
            return state.yaw;
        }

        public static Object[] raycastBlock(double distance) {
            return raycastBlock(distance, ScriptDefaults.mc.field_71439_g.field_70177_z, ScriptDefaults.mc.field_71439_g.field_70125_A);
        }

        public static Object[] raycastBlock(double distance, float yaw, float pitch) {
            net.minecraft.util.Vec3 eyeVec = ScriptDefaults.mc.field_71439_g.func_174824_e(1.0F);
            net.minecraft.util.Vec3 lookVec = Utils.getLookVec(yaw, pitch);
            net.minecraft.util.Vec3 sumVec = eyeVec.func_72441_c(lookVec.field_72450_a * distance, lookVec.field_72448_b * distance, lookVec.field_72449_c * distance);
            MovingObjectPosition mop = ScriptDefaults.mc.field_71441_e.func_147447_a(eyeVec, sumVec, false, false, false);
            if (mop != null && mop.field_72313_a == MovingObjectType.BLOCK) {
                Vec3 pos = new Vec3(mop.func_178782_a());
                Vec3 offset = new Vec3(mop.field_72307_f.field_72450_a - pos.x, mop.field_72307_f.field_72448_b - pos.y, mop.field_72307_f.field_72449_c - pos.z);
                return new Object[] { pos, offset, mop.field_178784_b.name() };
            } else {
                return null;
            }
        }

        public static Object[] raycastEntity(double distance) {
            return raycastEntity(distance, ScriptDefaults.mc.field_71439_g.field_70177_z, ScriptDefaults.mc.field_71439_g.field_70125_A);
        }

        public static Object[] raycastEntity(double distance, float yaw, float pitch) {
            net.minecraft.entity.Entity pointedEntity = null;
            MovingObjectPosition mop = ScriptDefaults.mc.field_71439_g.func_174822_a(distance, 1.0F);
            net.minecraft.util.Vec3 eyeVec = ScriptDefaults.mc.field_71439_g.func_174824_e(1.0F);
            net.minecraft.util.Vec3 lookVec = Utils.getLookVec(yaw, pitch);
            net.minecraft.util.Vec3 vec32 = eyeVec.func_72441_c(lookVec.field_72450_a * distance, lookVec.field_72448_b * distance, lookVec.field_72449_c * distance);
            net.minecraft.util.Vec3 vec33 = null;
            List list = ScriptDefaults.mc.field_71441_e.func_72839_b(ScriptDefaults.mc.func_175606_aa(), ScriptDefaults.mc.func_175606_aa().func_174813_aQ().func_72321_a(lookVec.field_72450_a * distance, lookVec.field_72448_b * distance, lookVec.field_72449_c * distance).func_72314_b(1.0, 1.0, 1.0));
            double d2 = distance;
            for (int i = 0; i < list.size(); i++) {
                net.minecraft.entity.Entity entity = (net.minecraft.entity.Entity) list.get(i);
                if (entity instanceof EntityLivingBase && entity.func_70067_L() && ((EntityLivingBase) entity).field_70725_aQ == 0) {
                    float cbs = entity.func_70111_Y();
                    AxisAlignedBB axisalignedbb = entity.func_174813_aQ().func_72314_b(cbs, cbs, cbs);
                    MovingObjectPosition movingobjectposition = axisalignedbb.func_72327_a(eyeVec, vec32);
                    if (axisalignedbb.func_72318_a(eyeVec)) {
                        if (0.0 < d2 || d2 == 0.0) {
                            pointedEntity = entity;
                            vec33 = movingobjectposition == null ? eyeVec : movingobjectposition.field_72307_f;
                            d2 = 0.0;
                        }
                    } else if (movingobjectposition != null) {
                        double d3 = eyeVec.func_72438_d(movingobjectposition.field_72307_f);
                        if (d3 < d2 || d2 == 0.0) {
                            if (entity != ScriptDefaults.mc.func_175606_aa().field_70154_o || entity.canRiderInteract()) {
                                pointedEntity = entity;
                                vec33 = movingobjectposition.field_72307_f;
                                d2 = d3;
                            } else if (d2 == 0.0) {
                                pointedEntity = entity;
                                vec33 = movingobjectposition.field_72307_f;
                            }
                        }
                    }
                }
            }
            if (pointedEntity == null || !(d2 < distance) && mop != null) {
                return null;
            } else {
                mop = new MovingObjectPosition(pointedEntity, vec33);
                Vec3 offset = new Vec3(mop.field_72307_f.field_72450_a - pointedEntity.field_70165_t, mop.field_72307_f.field_72448_b - pointedEntity.field_70163_u, mop.field_72307_f.field_72449_c - pointedEntity.field_70161_v);
                return new Object[] { new Entity(mop.field_72308_g), offset, eyeVec.func_72436_e(mop.field_72307_f) };
            }
        }

        public static boolean canPlaceBlock(ItemStack stack, Vec3 pos, String side) {
            return stack != null && stack.itemStack != null && stack.itemStack.func_77973_b() != null && stack.isBlock ? ((ItemBlock) stack.itemStack.func_77973_b()).func_179222_a(ScriptDefaults.mc.field_71441_e, Vec3.getBlockPos(pos), Utils.getEnum(EnumFacing.class, side), ScriptDefaults.mc.field_71439_g, stack.itemStack) : false;
        }

        public static boolean placeBlock(Vec3 targetPos, String side, Vec3 hitVec) {
            return ScriptDefaults.mc.field_71442_b.func_178890_a(ScriptDefaults.mc.field_71439_g, ScriptDefaults.mc.field_71441_e, ScriptDefaults.mc.field_71439_g.func_70694_bm(), Vec3.getBlockPos(targetPos), Utils.getEnum(EnumFacing.class, side), Vec3.getVec3(hitVec));
        }

        public static void clickBlock(Vec3 blockpos, String side) {
            ScriptDefaults.mc.field_71442_b.func_180511_b(Vec3.getBlockPos(blockpos), Utils.getEnum(EnumFacing.class, side));
        }

        public static void enableMovementFix() {
            RotationHelper.get().forceMovementFix = true;
        }

        public static void disableMovementFix() {
            RotationHelper.get().forceMovementFix = false;
        }

        public static boolean isMovementFixActive() {
            return RotationHelper.get().fixMovement();
        }

        public static boolean isRotationActive() {
            return RotationHelper.get().isActive();
        }

        public static void setRotations(float yaw, float pitch) {
            RotationHelper.get().setRotations(yaw, pitch);
        }

        public static void setYaw(float yaw) {
            RotationHelper.get().setYaw(yaw);
        }

        public static void setPitch(float pitch) {
            RotationHelper.get().setPitch(pitch);
        }

        public static Float getServerYaw() {
            return RotationHelper.get().getServerYaw();
        }

        public static Float getServerPitch() {
            return RotationHelper.get().getServerPitch();
        }

        public static float[] getRotationsToBlock(Vec3 position) {
            return RotationUtils.getRotations(new BlockPos(position.x, position.y, position.z));
        }

        public static void setSprinting(boolean sprinting) {
            ScriptDefaults.mc.field_71439_g.func_70031_b(sprinting);
        }

        public static void swing() {
            ScriptDefaults.mc.field_71439_g.func_71038_i();
        }

        public static void swingReset() {
            ScriptDefaults.mc.field_71439_g.func_71038_i();
            ScriptDefaults.mc.func_175597_ag().func_78444_b();
        }

        public static long time() {
            return System.currentTimeMillis();
        }

        public static boolean isFriend(String username) {
            return Utils.isFriended(username);
        }

        public static boolean isEnemy(String username) {
            return Utils.isEnemy(username);
        }
    }

    public static class config {

        private static String CONFIG_DIR = ScriptDefaults.mc.field_71412_D + File.separator + "keystrokes" + File.separator + "script_config.txt";

        private static String SEPARATOR = ":";

        private static String SEPARATOR_FULL = SEPARATOR + " ";

        private static void ensureConfigFileExists() throws IOException {
            Path configPath = Paths.get(CONFIG_DIR);
            if (Files.notExists(configPath)) {
                Files.createDirectories(configPath.getParent());
                Files.createFile(configPath);
            }
        }

        public static boolean set(String key, String value) {
            if (key != null && !key.isEmpty()) {
                key = key.replace(SEPARATOR, "");
                String entry = key + SEPARATOR_FULL + value;
                try {
                    ensureConfigFileExists();
                    Path configPath = new File(CONFIG_DIR).toPath();
                    List<String> lines = new ArrayList<>(Files.readAllLines(configPath));
                    boolean keyExists = false;
                    for (int i = 0; i < lines.size(); i++) {
                        String line = lines.get(i);
                        if (line.startsWith(key + SEPARATOR_FULL)) {
                            lines.set(i, entry);
                            keyExists = true;
                            break;
                        }
                    }
                    if (!keyExists) {
                        lines.add(entry);
                    }
                    Files.write(configPath, lines);
                    return true;
                } catch (IOException exception3) {
                    return false;
                }
            } else {
                return false;
            }
        }

        public static String get(String key) {
            if (key != null && !key.isEmpty()) {
                try {
                    ensureConfigFileExists();
                    Path configPath = new File(CONFIG_DIR).toPath();
                    for (String line : Files.readAllLines(configPath)) {
                        if (line.startsWith(key + SEPARATOR_FULL)) {
                            return line.substring((key + SEPARATOR_FULL).length());
                        }
                    }
                } catch (IOException exception4) {
                }
                return null;
            } else {
                return null;
            }
        }
    }

    public static class gl {

        public static void alpha(boolean alpha) {
            if (alpha) {
                GlStateManager.func_179141_d();
            } else {
                GlStateManager.func_179118_c();
            }
        }

        public static void begin(int mode) {
            GL11.glBegin(mode);
        }

        public static void blend(boolean blend) {
            if (blend) {
                GlStateManager.func_179147_l();
            } else {
                GlStateManager.func_179084_k();
            }
        }

        public static void color(float r, float g, float b, float a) {
            GlStateManager.func_179131_c(r, g, b, a);
        }

        public static void color(double r, double g, double b, double a) {
            GlStateManager.func_179131_c((float) r, (float) g, (float) b, (float) a);
        }

        public static void color(int r, int g, int b, int a) {
            if (r <= 1 && g <= 1 && b <= 1 && a <= 1) {
                GlStateManager.func_179131_c(r, g, b, a);
            } else {
                GlStateManager.func_179131_c(r / 255.0F, g / 255.0F, b / 255.0F, a / 255.0F);
            }
        }

        public static void depthFunc(int func) {
            GL11.glDepthFunc(func);
        }

        public static void colorMask(boolean red, boolean green, boolean blue, boolean alpha) {
            GlStateManager.func_179135_a(red, green, blue, alpha);
        }

        public static void polygonOffset(float factor, float units) {
            GL11.glPolygonOffset(factor, units);
        }

        public static void texCoord2(double u, double v) {
            GL11.glTexCoord2d(u, v);
        }

        public static void texCoord2(float u, float v) {
            GL11.glTexCoord2f(u, v);
        }

        public static void bindTexture(String resource) {
            ResourceLocation resLoc = new ResourceLocation(resource);
            ScriptDefaults.mc.func_110434_K().func_110577_a(resLoc);
        }

        public static void bindTextureId(int textureId) {
            GlStateManager.func_179144_i(textureId);
        }

        public static int getBoundTexture() {
            return GL11.glGetInteger(32873);
        }

        public static void polygonOffset(boolean enabled) {
            if (enabled) {
                GlStateManager.func_179088_q();
            } else {
                GlStateManager.func_179113_r();
            }
        }

        public static void cull(boolean cull) {
            if (cull) {
                GlStateManager.func_179089_o();
            } else {
                GlStateManager.func_179129_p();
            }
        }

        public static void depth(boolean depth) {
            if (depth) {
                GlStateManager.func_179126_j();
            } else {
                GlStateManager.func_179097_i();
            }
        }

        public static void depthMask(boolean depthMask) {
            GlStateManager.func_179132_a(depthMask);
        }

        public static void disable(int cap) {
            setCapability(cap, false);
        }

        public static void disableItemLighting() {
            RenderHelper.func_74518_a();
        }

        public static void enable(int cap) {
            setCapability(cap, true);
        }

        public static void enableItemLighting(boolean gui) {
            if (gui) {
                RenderHelper.func_74520_c();
            } else {
                RenderHelper.func_74519_b();
            }
        }

        public static void resetColor() {
            GlStateManager.func_179117_G();
            GlStateManager.func_179131_c(1.0F, 1.0F, 1.0F, 1.0F);
        }

        public static void end() {
            GL11.glEnd();
        }

        public static void lighting(boolean lighting) {
            if (lighting) {
                GlStateManager.func_179145_e();
            } else {
                GlStateManager.func_179140_f();
            }
        }

        public static void lineSmooth(boolean lineSmooth) {
            setGLEnable(2848, lineSmooth);
        }

        public static void lineWidth(float width) {
            GL11.glLineWidth(width);
        }

        public static void lineWidth(double width) {
            GL11.glLineWidth((float) width);
        }

        public static void normal(float x, float y, float z) {
            GL11.glNormal3f(x, y, z);
        }

        public static void normal(double x, double y, double z) {
            GL11.glNormal3d(x, y, z);
        }

        public static void pop() {
            GL11.glPopMatrix();
        }

        public static void push() {
            GL11.glPushMatrix();
        }

        public static void rotate(float angle, float x, float y, float z) {
            GL11.glRotatef(angle, x, y, z);
        }

        public static void rotate(double angle, double x, double y, double z) {
            GL11.glRotated(angle, x, y, z);
        }

        public static void scale(float x, float y, float z) {
            GL11.glScalef(x, y, z);
        }

        public static void scissor(int x, int y, int width, int height) {
            GL11.glScissor(x, y, width, height);
        }

        public static void scissor(boolean scissor) {
            setGLEnable(3089, scissor);
        }

        public static void texture2d(boolean texture2d) {
            if (texture2d) {
                GlStateManager.func_179098_w();
            } else {
                GlStateManager.func_179090_x();
            }
        }

        public static void translate(float x, float y, float z) {
            GL11.glTranslatef(x, y, z);
        }

        public static void translate(double x, double y, double z) {
            GL11.glTranslated(x, y, z);
        }

        public static void vertex2(float x, float y) {
            GL11.glVertex2f(x, y);
        }

        public static void vertex3(float x, float y, float z) {
            GL11.glVertex3f(x, y, z);
        }

        public static void vertex3(double x, double y, double z) {
            GL11.glVertex3d(x, y, z);
        }

        private static void setGLEnable(int cap, boolean enable) {
            if (enable) {
                GL11.glEnable(cap);
            } else {
                GL11.glDisable(cap);
            }
        }

        private static void setCapability(int cap, boolean enable) {
            switch(cap) {
                case 2884:
                    cull(enable);
                    break;
                case 2896:
                    lighting(enable);
                    break;
                case 2912:
                    if (enable) {
                        GlStateManager.func_179127_m();
                    } else {
                        GlStateManager.func_179106_n();
                    }
                    break;
                case 2929:
                    depth(enable);
                    break;
                case 2977:
                    if (enable) {
                        GlStateManager.func_179108_z();
                    } else {
                        GlStateManager.func_179133_A();
                    }
                    break;
                case 3008:
                    alpha(enable);
                    break;
                case 3042:
                    blend(enable);
                    break;
                case 3058:
                    if (enable) {
                        GlStateManager.func_179115_u();
                    } else {
                        GlStateManager.func_179134_v();
                    }
                    break;
                case 3553:
                    texture2d(enable);
                    break;
                case 32823:
                    if (enable) {
                        GlStateManager.func_179088_q();
                    } else {
                        GlStateManager.func_179113_r();
                    }
                    break;
                case 32826:
                    if (enable) {
                        GlStateManager.func_179091_B();
                    } else {
                        GlStateManager.func_179101_C();
                    }
                    break;
                default:
                    setGLEnable(cap, enable);
            }
        }
    }

    public static class inventory {

        public static int getSlot() {
            return ScriptDefaults.mc.field_71439_g.field_71071_by.field_70461_c;
        }

        public static int getHoveringSlot() {
            if (!(ScriptDefaults.mc.field_71462_r instanceof GuiContainer)) {
                return -1;
            } else {
                Slot slot = ((GuiContainer) ScriptDefaults.mc.field_71462_r).getSlotUnderMouse();
                return slot == null ? -1 : slot.field_75222_d;
            }
        }

        public static void setSlot(int slot) {
            ScriptDefaults.mc.field_71439_g.field_71071_by.field_70461_c = slot;
        }

        public static void click(int slot, int button, int mode) {
            ScriptDefaults.mc.field_71442_b.func_78753_a(ScriptDefaults.mc.field_71439_g.field_71070_bA.field_75152_c, slot, button, mode, ScriptDefaults.mc.field_71439_g);
        }

        public static List<String> getBookContents() {
            if (ScriptDefaults.mc.field_71462_r instanceof GuiScreenBook) {
                List<String> contents = new ArrayList<>();
                List<IChatComponent> bookContents = ((IAccessorGuiScreenBook) ScriptDefaults.mc.field_71462_r).getBookContents();
                if (bookContents == null) {
                    return contents;
                }
                int max = Math.min(128 / ScriptDefaults.mc.field_71466_p.field_78288_b, bookContents.size());
                for (int line = 0; line < max; line++) {
                    IChatComponent lineStr = bookContents.get(line);
                    contents.add(lineStr.func_150260_c());
                }
                if (!contents.isEmpty()) {
                    return contents;
                }
            }
            return null;
        }

        public static String getChest() {
            if (ScriptDefaults.mc.field_71439_g.field_71070_bA instanceof ContainerChest) {
                ContainerChest chest = (ContainerChest) ScriptDefaults.mc.field_71439_g.field_71070_bA;
                return chest == null ? "" : chest.func_85151_d().func_145748_c_().func_150260_c();
            } else {
                return "";
            }
        }

        public static String getContainer() {
            if (ScriptDefaults.mc.field_71462_r instanceof GuiContainerCreative) {
                CreativeTabs creativetabs = CreativeTabs.field_78032_a[((GuiContainerCreative) ScriptDefaults.mc.field_71462_r).func_147056_g()];
                if (creativetabs != null) {
                    return I18n.func_135052_a(creativetabs.func_78024_c(), new Object[0]);
                }
            } else if (ScriptDefaults.mc.field_71462_r != null) {
                try {
                    return ((IInventory) ReflectionUtils.containerInventoryPlayer.get(ScriptDefaults.mc.field_71462_r.getClass()).get(ScriptDefaults.mc.field_71462_r)).func_145748_c_().func_150260_c();
                } catch (Exception exception5) {
                }
            }
            return "";
        }

        public static int getSize() {
            return ScriptDefaults.mc.field_71439_g.field_71071_by.func_70302_i_();
        }

        public static int getChestSize() {
            if (ScriptDefaults.mc.field_71439_g.field_71070_bA instanceof ContainerChest) {
                ContainerChest chest = (ContainerChest) ScriptDefaults.mc.field_71439_g.field_71070_bA;
                return chest == null ? -1 : chest.func_85151_d().func_70302_i_();
            } else {
                return -1;
            }
        }

        public static ItemStack getStackInSlot(int slot) {
            return ScriptDefaults.mc.field_71439_g.field_71071_by.func_70301_a(slot) == null ? null : new ItemStack(ScriptDefaults.mc.field_71439_g.field_71071_by.func_70301_a(slot), (byte) 0);
        }

        public static ItemStack getStackInChestSlot(int slot) {
            if (ScriptDefaults.mc.field_71439_g.field_71070_bA instanceof ContainerChest) {
                ContainerChest chest = (ContainerChest) ScriptDefaults.mc.field_71439_g.field_71070_bA;
                return chest.func_85151_d().func_70301_a(slot) == null ? null : new ItemStack(chest.func_85151_d().func_70301_a(slot), (byte) 0);
            } else {
                return null;
            }
        }

        public static ItemStack getStackInCraftingSlot(int slot) {
            if (ScriptDefaults.mc.field_71439_g.field_71070_bA instanceof ContainerWorkbench) {
                InventoryCrafting inventoryCrafting = ((ContainerWorkbench) ScriptDefaults.mc.field_71439_g.field_71070_bA).field_75162_e;
                return inventoryCrafting.func_70301_a(slot) == null ? null : new ItemStack(inventoryCrafting.func_70301_a(slot), (byte) 0);
            } else {
                return null;
            }
        }

        public static ItemStack getCraftResult() {
            if (ScriptDefaults.mc.field_71439_g.field_71070_bA instanceof ContainerWorkbench) {
                IInventory craftResult = ((ContainerWorkbench) ScriptDefaults.mc.field_71439_g.field_71070_bA).field_75160_f;
                return craftResult.func_70301_a(0) == null ? null : new ItemStack(craftResult.func_70301_a(0), (byte) 0);
            } else {
                return null;
            }
        }

        public static void open() {
            KeyBinding inventoryKey = ScriptDefaults.mc.field_71474_y.field_151445_Q;
            int originalKeyCode = inventoryKey.func_151463_i();
            if (originalKeyCode == 0) {
                inventoryKey.func_151462_b(13);
                KeyBinding.func_74508_b();
            }
            KeyBinding.func_74510_a(inventoryKey.func_151463_i(), true);
            KeyBinding.func_74507_a(inventoryKey.func_151463_i());
            KeyBinding.func_74510_a(inventoryKey.func_151463_i(), false);
            if (originalKeyCode == 0) {
                inventoryKey.func_151462_b(0);
                KeyBinding.func_74508_b();
            }
        }
    }

    public static class keybinds {

        public static int[] getMousePosition() {
            return new int[] { Mouse.getX(), Mouse.getY() };
        }

        public static boolean isPressed(String key) {
            KeyBinding keyBind = ReflectionUtils.keybinds.get(key);
            return keyBind != null && keyBind.func_151470_d();
        }

        public static void setPressed(String key, boolean pressed) {
            KeyBinding keyBind = ReflectionUtils.keybinds.get(key);
            if (keyBind != null) {
                KeyBinding.func_74510_a(keyBind.func_151463_i(), pressed);
            }
        }

        public static void onTick(String key) {
            KeyBinding keyBind = ReflectionUtils.keybinds.get(key);
            if (keyBind != null) {
                KeyBinding.func_74507_a(keyBind.func_151463_i());
            }
        }

        public static int getKeyCode(String key) {
            KeyBinding keyBind = ReflectionUtils.keybinds.get(key);
            return keyBind != null ? keyBind.func_151463_i() : -1;
        }

        public static int getKeycode(String key) {
            return getKeyCode(key);
        }

        public static int getKeyIndex(String key) {
            return Keyboard.getKeyIndex(key);
        }

        public static boolean isMouseDown(int mouseButton) {
            return Mouse.isButtonDown(mouseButton);
        }

        public static boolean isKeyDown(int key) {
            return Keyboard.isKeyDown(key);
        }

        public static void rightClick() {
            ((IAccessorMinecraft) ScriptDefaults.mc).callRightClickMouse();
        }

        public static void leftClick() {
            ((IAccessorMinecraft) ScriptDefaults.mc).callClickMouse();
        }

        public static int getScroll() {
            return Mouse.getDWheel();
        }
    }

    public static class modules {

        private String superName;

        public modules(String superName) {
            this.superName = superName;
        }

        private static Module getModule(String moduleName) {
            return ScriptDefaults.modulesMap.get(moduleName);
        }

        private static Module getScript(String name) {
            return ScriptDefaults.modulesMap.get(name);
        }

        private static Setting getSetting(Module module, String settingName) {
            if (module == null) {
                return null;
            } else {
                for (Setting setting : module.getSettings()) {
                    if (setting.getName().equals(settingName)) {
                        return setting;
                    }
                }
                return null;
            }
        }

        private GroupSetting getGroupForString(String group) {
            if (group.isEmpty()) {
                return null;
            } else {
                for (Setting setting : getScript(this.superName).getSettings()) {
                    if (setting instanceof GroupSetting && setting.getName().equals(group)) {
                        return (GroupSetting) setting;
                    }
                }
                return null;
            }
        }

        public void enable(String moduleName) {
            if (getModule(moduleName) != null) {
                getModule(moduleName).enable();
            }
        }

        public void disable(String moduleName) {
            if (getModule(moduleName) != null) {
                getModule(moduleName).disable();
            }
        }

        public boolean isEnabled(String moduleName) {
            return getModule(moduleName) == null ? false : getModule(moduleName).isEnabled();
        }

        public Entity getKillAuraTarget() {
            return KillAura.target == null ? null : Entity.convert(KillAura.target);
        }

        public Entity getAimAssistTarget() {
            AimAssist aimAssist = (AimAssist) ModuleManager.getModule(AimAssist.class);
            return aimAssist != null && aimAssist.isEnabled() ? Entity.convert(aimAssist.getAimAssistTarget()) : null;
        }

        public Entity getLagRangeTarget() {
            LagRange lagRange = (LagRange) ModuleManager.getModule(LagRange.class);
            return lagRange != null && lagRange.isEnabled() ? Entity.convert(lagRange.getLagRangeTarget()) : null;
        }

        public Entity getBacktrackTarget() {
            Backtrack backtrack = ModuleManager.backtrack;
            return backtrack != null && backtrack.isEnabled() ? Entity.convert(backtrack.getBacktrackTarget()) : null;
        }

        public Vec3 getLagRangePosition() {
            LagRange lagRange = (LagRange) ModuleManager.getModule(LagRange.class);
            if (lagRange != null && lagRange.isEnabled()) {
                net.minecraft.util.Vec3 position = lagRange.getLagRangePosition();
                return position == null ? null : new Vec3(position);
            } else {
                return null;
            }
        }

        public Vec3 getBacktrackPosition() {
            Backtrack backtrack = ModuleManager.backtrack;
            if (backtrack != null && backtrack.isEnabled()) {
                net.minecraft.util.Vec3 position = backtrack.getBacktrackPosition();
                return position == null ? null : new Vec3(position);
            } else {
                return null;
            }
        }

        public Map<String, Object> getSettings(String name) {
            Map<String, Object> settings = new HashMap<>();
            Module module = getModule(name);
            if (module == null) {
                return settings;
            } else {
                for (Setting setting : module.getSettings()) {
                    if (setting instanceof SliderSetting) {
                        settings.put(setting.getName(), ((SliderSetting) setting).getInput());
                    } else if (setting instanceof ButtonSetting) {
                        settings.put(setting.getName(), ((ButtonSetting) setting).isToggled());
                    } else if (setting instanceof ColorSetting) {
                        settings.put(setting.getName(), ((ColorSetting) setting).getColor());
                    }
                }
                return settings;
            }
        }

        public Map<String, List<String>> getCategories() {
            Map<String, List<String>> categories = new HashMap<>();
            for (CategoryComponent categoryComponent : ClickGui.categories) {
                List<String> modules = new ArrayList<>();
                for (ModuleComponent module : categoryComponent.modules) {
                    modules.add(module.mod.getName());
                }
                categories.put(categoryComponent.category.name(), modules);
            }
            return categories;
        }

        public Vec3 getBedAuraPosition() {
            if (ModuleManager.bedAura != null && ModuleManager.bedAura.isEnabled()) {
                BlockPos p = ModuleManager.bedAura.getAuraTargetPos();
                return p == null ? null : new Vec3(p.func_177958_n(), p.func_177956_o(), p.func_177952_p());
            } else {
                return null;
            }
        }

        public float[] getBedAuraProgress() {
            if (ModuleManager.bedAura != null && ModuleManager.bedAura.isEnabled()) {
                float d = ModuleManager.bedAura.getAuraBreakProgress();
                return new float[] { d, 0.0F };
            } else {
                return new float[] { 0.0F, 0.0F };
            }
        }

        public boolean isScaffolding() {
            return false;
        }

        public boolean isTowering() {
            return false;
        }

        public boolean isHidden(String moduleName) {
            Module module = getModule(moduleName);
            return module != null ? module.isHidden() : false;
        }

        public void registerGroup(String name) {
            getScript(this.superName).registerSetting(new GroupSetting(name));
        }

        public void registerButton(String name, boolean defaultValue) {
            getScript(this.superName).registerSetting(new ButtonSetting(name, defaultValue));
        }

        public void registerButton(String group, String name, boolean defaultValue) {
            getScript(this.superName).registerSetting(new ButtonSetting(this.getGroupForString(group), name, defaultValue));
        }

        public void registerKey(String group, String name, int defaultKey) {
            getScript(this.superName).registerSetting(new KeySetting(this.getGroupForString(group), name, defaultKey));
        }

        public void registerKey(String name, int defaultKey) {
            getScript(this.superName).registerSetting(new KeySetting(name, defaultKey));
        }

        public void registerSlider(String group, String name, String suffix, double defaultValue, double minimum, double maximum, double interval) {
            getScript(this.superName).registerSetting(new SliderSetting(this.getGroupForString(group), name, suffix, defaultValue, minimum, maximum, interval));
        }

        public void registerSlider(String group, String name, String suffix, int defaultValue, String[] stringArray) {
            getScript(this.superName).registerSetting(new SliderSetting(this.getGroupForString(group), name, suffix, defaultValue, stringArray));
        }

        public void registerSlider(String name, double defaultValue, double minimum, double maximum, double interval) {
            this.registerSlider("", name, "", defaultValue, minimum, maximum, interval);
        }

        public void registerSlider(String name, int defaultValue, String[] stringArray) {
            this.registerSlider("", name, "", defaultValue, stringArray);
        }

        public void registerSlider(String name, String suffix, double defaultValue, double minimum, double maximum, double interval) {
            this.registerSlider("", name, suffix, defaultValue, minimum, maximum, interval);
        }

        public void registerSlider(String name, String suffix, int defaultValue, String[] stringArray) {
            this.registerSlider("", name, suffix, defaultValue, stringArray);
        }

        public void registerDescription(String description) {
            getScript(this.superName).registerSetting(new DescriptionSetting(description));
        }

        public void registerColor(String name, int red, int green, int blue) {
            getScript(this.superName).registerSetting(new ColorSetting(name, red, green, blue));
        }

        public void registerColor(String name, int red, int green, int blue, int alpha) {
            getScript(this.superName).registerSetting(new ColorSetting(name, red, green, blue, alpha));
        }

        public void registerColor(String group, String name, int red, int green, int blue) {
            getScript(this.superName).registerSetting(new ColorSetting(this.getGroupForString(group), name, red, green, blue));
        }

        public void registerColor(String group, String name, int red, int green, int blue, int alpha) {
            getScript(this.superName).registerSetting(new ColorSetting(this.getGroupForString(group), name, red, green, blue, alpha));
        }

        public int getColor(String moduleName, String name) {
            ColorSetting setting = (ColorSetting) getSetting(getModule(moduleName), name);
            return setting == null ? 0 : setting.getColor();
        }

        public void setColor(String moduleName, String name, int red, int green, int blue) {
            ColorSetting setting = (ColorSetting) getSetting(getModule(moduleName), name);
            if (setting != null) {
                setting.setColor(red, green, blue);
            }
        }

        public void setColor(String moduleName, String name, int red, int green, int blue, int alpha) {
            ColorSetting setting = (ColorSetting) getSetting(getModule(moduleName), name);
            if (setting != null) {
                setting.setColor(red, green, blue, alpha);
            }
        }

        public boolean getButton(String moduleName, String name) {
            ButtonSetting setting = (ButtonSetting) getSetting(getModule(moduleName), name);
            return setting == null ? false : setting.isToggled();
        }

        public double getSlider(String moduleName, String name) {
            SliderSetting setting = (SliderSetting) getSetting(getModule(moduleName), name);
            return setting == null ? 0.0 : setting.getInput();
        }

        public double getSliderMin(String moduleName, String name) {
            SliderSetting setting = (SliderSetting) getSetting(getModule(moduleName), name);
            return setting == null ? 0.0 : setting.getMin();
        }

        public double getSliderMax(String moduleName, String name) {
            SliderSetting setting = (SliderSetting) getSetting(getModule(moduleName), name);
            return setting == null ? 0.0 : setting.getMax();
        }

        public String getSliderString(String moduleName, String name) {
            SliderSetting setting = (SliderSetting) getSetting(getModule(moduleName), name);
            if (setting != null && setting.isString) {
                int intValue = (int) Math.round(setting.getInput());
                intValue = Math.max(0, Math.min(intValue, setting.getOptions().length - 1));
                return setting.getOptions()[intValue];
            } else {
                return "";
            }
        }

        public boolean getKeyPressed(String moduleName, String name) {
            KeySetting setting = (KeySetting) getSetting(getModule(moduleName), name);
            return setting == null ? false : setting.isPressed();
        }

        public void setButton(String moduleName, String name, boolean value) {
            ButtonSetting setting = (ButtonSetting) getSetting(getModule(moduleName), name);
            if (setting != null) {
                setting.setEnabled(value);
            }
        }

        public void setSlider(String moduleName, String name, double value) {
            SliderSetting setting = (SliderSetting) getSetting(getModule(moduleName), name);
            if (setting != null) {
                setting.setValueRawWithEvent(value);
            }
        }

        public void setKey(String moduleName, String name, int code) {
            KeySetting setting = (KeySetting) getSetting(getModule(moduleName), name);
            if (setting != null) {
                setting.setKey(code);
            }
        }
    }

    public static class render {

        private static final double[] WORLD_TO_SCREEN_COORDS = new double[3];

        private static RenderUtils.ProjectionContext worldToScreenContext;

        private static boolean worldToScreenContextActive;

        static void beginWorldToScreen(int scaleFactor) {
            worldToScreenContext = RenderUtils.captureProjectionContext(worldToScreenContext, scaleFactor);
            worldToScreenContextActive = true;
        }

        static void endWorldToScreen() {
            worldToScreenContextActive = false;
        }

        public static void block(Vec3 position, int color, boolean outline, boolean shade) {
            RenderUtils.renderBlock(new BlockPos(position.x, position.y, position.z), color, outline, shade);
        }

        public static int getThemeRGB(String theme) {
            return Theme.getGradient(Utils.getEnum(Theme.class, theme).ordinal(), 0.0);
        }

        public static void block(int x, int y, int z, int color, boolean outline, boolean shade) {
            RenderUtils.renderBlock(new BlockPos(x, y, z), color, outline, shade);
        }

        public static void entity(Entity en, int color, float partialTicks, boolean outline, boolean shade) {
            net.minecraft.entity.Entity e = en.entity;
            double x = e.field_70142_S + (e.field_70165_t - e.field_70142_S) * partialTicks - ScriptDefaults.mc.func_175598_ae().field_78730_l;
            double y = e.field_70137_T + (e.field_70163_u - e.field_70137_T) * partialTicks - ScriptDefaults.mc.func_175598_ae().field_78731_m;
            double z = e.field_70136_U + (e.field_70161_v - e.field_70136_U) * partialTicks - ScriptDefaults.mc.func_175598_ae().field_78728_n;
            AxisAlignedBB axisAlignedBB = e.func_174813_aQ().func_72314_b(0.1, 0.1, 0.1);
            AxisAlignedBB axis = new AxisAlignedBB(axisAlignedBB.field_72340_a - e.field_70165_t + x, axisAlignedBB.field_72338_b - e.field_70163_u + y, axisAlignedBB.field_72339_c - e.field_70161_v + z, axisAlignedBB.field_72336_d - e.field_70165_t + x, axisAlignedBB.field_72337_e - e.field_70163_u + y, axisAlignedBB.field_72334_f - e.field_70161_v + z);
            GL11.glPushMatrix();
            GL11.glBlendFunc(770, 771);
            GL11.glEnable(3042);
            GL11.glDisable(3553);
            GL11.glDisable(2929);
            GL11.glDepthMask(false);
            GL11.glLineWidth(2.0F);
            float a = (color >> 24 & 0xFF) / 255.0F;
            float r = (color >> 16 & 0xFF) / 255.0F;
            float g = (color >> 8 & 0xFF) / 255.0F;
            float b = (color & 0xFF) / 255.0F;
            GL11.glColor4f(r, g, b, a);
            if (outline) {
                RenderGlobal.func_181561_a(axis);
            }
            if (shade) {
                RenderUtils.drawBoundingBox(axis, r, g, b);
            }
            GL11.glEnable(3553);
            GL11.glEnable(2929);
            GL11.glDepthMask(true);
            GL11.glDisable(3042);
            GL11.glPopMatrix();
        }

        public static void entityGui(Entity en, int x, int y, float mouseX, float mouseY, int scale) {
            if (en.isLiving) {
                GL11.glPushMatrix();
                GuiInventory.func_147046_a(x, y, scale, mouseX, mouseY, (EntityLivingBase) en.entity);
                GL11.glPopMatrix();
            }
        }

        public static void resetEquippedProgress() {
            ScriptDefaults.mc.func_175597_ag().func_78444_b();
        }

        public static void tracer(Entity entity, float lineWidth, int color, float partialTicks) {
            RenderUtils.drawTracerLine(entity.entity, color, lineWidth, partialTicks);
        }

        public static void showGui() {
            ScriptDefaults.mc.func_147108_a(new ScriptDefaults.render.EmptyGuiScreen());
        }

        public static void item(ItemStack item, float x, float y, float scale) {
            GlStateManager.func_179094_E();
            ScriptDefaults.mc.field_71460_t.func_78478_c();
            if (scale != 1.0F) {
                GlStateManager.func_179152_a(scale, scale, scale);
            }
            RenderHelper.func_74520_c();
            GlStateManager.func_179084_k();
            GlStateManager.func_179109_b(x / scale, y / scale, 0.0F);
            ScriptDefaults.mc.func_175599_af().func_175042_a(item.itemStack, 0, 0);
            GlStateManager.func_179147_l();
            RenderHelper.func_74518_a();
            if (scale != 1.0F) {
                GlStateManager.func_179152_a(scale, scale, scale);
            }
            GlStateManager.func_179121_F();
        }

        public static void image(Image image, float x, float y, float width, float height) {
            if (image != null && image.isLoaded()) {
                if (image.textureId == -1) {
                    DynamicTexture dynamicTexture = new DynamicTexture(image.bufferedImage);
                    GL11.glTexParameteri(3553, 10240, 9728);
                    dynamicTexture.func_110564_a();
                    image.textureId = dynamicTexture.func_110552_b();
                }
                GlStateManager.func_179094_E();
                GlStateManager.func_179098_w();
                GlStateManager.func_179144_i(image.textureId);
                GlStateManager.func_179131_c(1.0F, 1.0F, 1.0F, 1.0F);
                GL11.glTexParameteri(3553, 10240, 9728);
                Tessellator tessellator = Tessellator.func_178181_a();
                WorldRenderer worldrenderer = tessellator.func_178180_c();
                worldrenderer.func_181668_a(7, DefaultVertexFormats.field_181709_i);
                worldrenderer.func_181662_b(x, y + height, 0.0).func_181673_a(0.0, 1.0).func_181669_b(255, 255, 255, 255).func_181675_d();
                worldrenderer.func_181662_b(x + width, y + height, 0.0).func_181673_a(1.0, 1.0).func_181669_b(255, 255, 255, 255).func_181675_d();
                worldrenderer.func_181662_b(x + width, y, 0.0).func_181673_a(1.0, 0.0).func_181669_b(255, 255, 255, 255).func_181675_d();
                worldrenderer.func_181662_b(x, y, 0.0).func_181673_a(0.0, 0.0).func_181669_b(255, 255, 255, 255).func_181675_d();
                tessellator.func_78381_a();
                GlStateManager.func_179121_F();
            }
        }

        public static Vec3 worldToScreen(double x, double y, double z, int scaleFactor, float partialTicks) {
            boolean temporaryContext = !worldToScreenContextActive;
            if (temporaryContext) {
                ((IAccessorEntityRenderer) ScriptDefaults.mc.field_71460_t).callSetupCameraTransform(partialTicks, 0);
                beginWorldToScreen(scaleFactor);
            }
            x -= ScriptDefaults.mc.func_175598_ae().field_78730_l;
            y -= ScriptDefaults.mc.func_175598_ae().field_78731_m;
            z -= ScriptDefaults.mc.func_175598_ae().field_78728_n;
            boolean projected = RenderUtils.projectTo2D(worldToScreenContext, x, y, z, scaleFactor, WORLD_TO_SCREEN_COORDS);
            ScriptDefaults.mc.field_71460_t.func_78478_c();
            if (temporaryContext) {
                endWorldToScreen();
            }
            return !projected ? null : new Vec3(WORLD_TO_SCREEN_COORDS[0], WORLD_TO_SCREEN_COORDS[1], WORLD_TO_SCREEN_COORDS[2]);
        }

        public static void roundedRect(float startX, float startY, float endX, float endY, float radius, int color) {
            RoundedUtils.drawRoundedRectRise(startX, startY, Math.abs(startX - endX), Math.abs(startY - endY), radius, color);
        }

        public static void gradientRect(float startX, float startY, float endX, float endY, int leftColor, int rightColor) {
            gradientRect(startX, startY, endX, endY, leftColor, leftColor, rightColor, rightColor);
        }

        public static void gradientRect(float startX, float startY, float endX, float endY, int topLeftColor, int bottomLeftColor, int topRightColor, int bottomRightColor) {
            RenderUtils.drawRoundedGradientRect(startX, startY, endX, endY, 0.0F, topLeftColor, bottomLeftColor, topRightColor, bottomRightColor);
        }

        public static double[] getRotations() {
            return new double[] { ScriptDefaults.mc.func_175598_ae().field_78735_i, ScriptDefaults.mc.func_175598_ae().field_78732_j };
        }

        public static double[] getCameraRotations() {
            return new double[] { Utils.getCameraYaw(), Utils.getCameraPitch() };
        }

        public static int getFontWidth(String text) {
            return ScriptDefaults.mc.field_71466_p.func_78256_a(text) + Utils.getBoldWidth(text);
        }

        public static int getFontHeight() {
            return ScriptDefaults.mc.field_71466_p.field_78288_b;
        }

        public static Vec3 getPosition() {
            return new Vec3(ScriptDefaults.mc.func_175598_ae().field_78730_l, ScriptDefaults.mc.func_175598_ae().field_78731_m, ScriptDefaults.mc.func_175598_ae().field_78728_n);
        }

        public static void text(String text, float x, float y, float scale, int color, boolean shadow) {
            text2d(text, x, y, scale, color, shadow);
        }

        public static void text2d(String text, float x, float y, float scale, int color, boolean shadow) {
            GlStateManager.func_179094_E();
            if (scale != 1.0F) {
                GlStateManager.func_179152_a(scale, scale, scale);
            }
            GlStateManager.func_179147_l();
            GlStateManager.func_179120_a(770, 771, 1, 0);
            ScriptDefaults.mc.field_71466_p.func_175065_a(text, x / scale, y / scale, color, shadow);
            GlStateManager.func_179084_k();
            if (scale != 1.0F) {
                GlStateManager.func_179152_a(1.0F, 1.0F, 1.0F);
            }
            GlStateManager.func_179121_F();
        }

        public static void text3d(String text, Vec3 position, float scale, boolean shadow, boolean depth, boolean background, int color) {
            ((IAccessorEntityRenderer) ScriptDefaults.mc.field_71460_t).callSetupCameraTransform(((IAccessorMinecraft) ScriptDefaults.mc).getTimer().field_74281_c, 0);
            GlStateManager.func_179094_E();
            float partialTicks = ((IAccessorMinecraft) ScriptDefaults.mc).getTimer().field_74281_c;
            double px = ScriptDefaults.mc.field_71439_g.field_70169_q + (ScriptDefaults.mc.field_71439_g.field_70165_t - ScriptDefaults.mc.field_71439_g.field_70169_q) * partialTicks;
            double py = ScriptDefaults.mc.field_71439_g.field_70167_r + (ScriptDefaults.mc.field_71439_g.field_70163_u - ScriptDefaults.mc.field_71439_g.field_70167_r) * partialTicks;
            double pz = ScriptDefaults.mc.field_71439_g.field_70166_s + (ScriptDefaults.mc.field_71439_g.field_70161_v - ScriptDefaults.mc.field_71439_g.field_70166_s) * partialTicks;
            GlStateManager.func_179137_b((float) position.x - px, (float) position.y - py, (float) position.z - pz);
            GL11.glNormal3f(0.0F, 1.0F, 0.0F);
            GlStateManager.func_179114_b(-ScriptDefaults.mc.func_175598_ae().field_78735_i, 0.0F, 1.0F, 0.0F);
            GlStateManager.func_179114_b(ScriptDefaults.mc.func_175598_ae().field_78732_j, 1.0F, 0.0F, 0.0F);
            float f1 = 0.02666667F;
            GlStateManager.func_179152_a(-f1, -f1, f1);
            if (depth) {
                GlStateManager.func_179132_a(false);
                GlStateManager.func_179097_i();
            }
            GlStateManager.func_179147_l();
            GlStateManager.func_179120_a(770, 771, 1, 0);
            if (background) {
                GlStateManager.func_179090_x();
                int width = ScriptDefaults.mc.field_71466_p.func_78256_a(text);
                Tessellator tessellator = Tessellator.func_178181_a();
                WorldRenderer worldrenderer = tessellator.func_178180_c();
                worldrenderer.func_181668_a(7, DefaultVertexFormats.field_181706_f);
                worldrenderer.func_181662_b(-1.0, -1.0, 0.0).func_181666_a(0.0F, 0.0F, 0.0F, 0.25F).func_181675_d();
                worldrenderer.func_181662_b(-1.0, 7.0, 0.0).func_181666_a(0.0F, 0.0F, 0.0F, 0.25F).func_181675_d();
                worldrenderer.func_181662_b(width + 1, 7.0, 0.0).func_181666_a(0.0F, 0.0F, 0.0F, 0.25F).func_181675_d();
                worldrenderer.func_181662_b(width + 1, -1.0, 0.0).func_181666_a(0.0F, 0.0F, 0.0F, 0.25F).func_181675_d();
                tessellator.func_78381_a();
                GlStateManager.func_179098_w();
            }
            if (scale != 1.0F) {
                GlStateManager.func_179152_a(scale, scale, scale);
            }
            ScriptDefaults.mc.field_71466_p.func_175065_a(text, 0.0F, 0.0F, color, shadow);
            if (scale != 1.0F) {
                GlStateManager.func_179152_a(1.0F, 1.0F, 1.0F);
            }
            if (depth) {
                GlStateManager.func_179126_j();
                GlStateManager.func_179132_a(true);
            }
            GlStateManager.func_179084_k();
            GlStateManager.func_179131_c(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.func_179121_F();
        }

        public static void rect(float startX, float startY, float endX, float endY, int color) {
            if (startX < endX) {
                float i = startX;
                startX = endX;
                endX = i;
            }
            if (startY < endY) {
                float j = startY;
                startY = endY;
                endY = j;
            }
            float f3 = (color >> 24 & 0xFF) / 255.0F;
            float f4 = (color >> 16 & 0xFF) / 255.0F;
            float f5 = (color >> 8 & 0xFF) / 255.0F;
            float f6 = (color & 0xFF) / 255.0F;
            Tessellator tessellator = Tessellator.func_178181_a();
            WorldRenderer worldrenderer = tessellator.func_178180_c();
            GL11.glPushMatrix();
            GlStateManager.func_179147_l();
            GlStateManager.func_179090_x();
            GlStateManager.func_179120_a(770, 771, 1, 0);
            GlStateManager.func_179131_c(f4, f5, f6, f3);
            worldrenderer.func_181668_a(7, DefaultVertexFormats.field_181705_e);
            worldrenderer.func_181662_b(startX, endY, 0.0).func_181675_d();
            worldrenderer.func_181662_b(endX, endY, 0.0).func_181675_d();
            worldrenderer.func_181662_b(endX, startY, 0.0).func_181675_d();
            worldrenderer.func_181662_b(startX, startY, 0.0).func_181675_d();
            tessellator.func_78381_a();
            GlStateManager.func_179098_w();
            GlStateManager.func_179084_k();
            GL11.glPopMatrix();
        }

        public static void line2D(double startX, double startY, double endX, double endY, float lineWidth, int color) {
            GL11.glPushMatrix();
            GL11.glEnable(2848);
            GL11.glDisable(3553);
            GL11.glEnable(3042);
            RenderUtils.glColor(color);
            GL11.glLineWidth(lineWidth);
            GL11.glBegin(1);
            GL11.glVertex2d(startX, startY);
            GL11.glVertex2d(endX, endY);
            GL11.glEnd();
            GL11.glDisable(3042);
            GL11.glEnable(3553);
            GL11.glDisable(2848);
            GL11.glPopMatrix();
        }

        public static void line3D(Vec3 pos1, Vec3 pos2, float lineWidth, int color) {
            line3D(pos1.x, pos1.y, pos1.z, pos2.x, pos2.y, pos2.z, lineWidth, color);
        }

        public static void line3D(double startX, double startY, double startZ, double endX, double endY, double endZ, float lineWidth, int color) {
            double vx = ScriptDefaults.mc.func_175598_ae().field_78730_l;
            double vy = ScriptDefaults.mc.func_175598_ae().field_78731_m;
            double vz = ScriptDefaults.mc.func_175598_ae().field_78728_n;
            double sx = startX - vx;
            double sy = startY - vy;
            double sz = startZ - vz;
            endX -= vx;
            endY -= vy;
            endZ -= vz;
            float a = (color >> 24 & 0xFF) / 255.0F;
            float r = (color >> 16 & 0xFF) / 255.0F;
            float g = (color >> 8 & 0xFF) / 255.0F;
            float b = (color & 0xFF) / 255.0F;
            GL11.glPushMatrix();
            GL11.glEnable(3042);
            GL11.glEnable(2848);
            GL11.glDisable(2929);
            GL11.glDisable(3553);
            GL11.glBlendFunc(770, 771);
            GL11.glLineWidth(lineWidth);
            GlStateManager.func_179131_c(r, g, b, a);
            GL11.glBegin(2);
            GL11.glVertex3d(sx, sy, sz);
            GL11.glVertex3d(endX, endY, endZ);
            GL11.glEnd();
            GlStateManager.func_179131_c(1.0F, 1.0F, 1.0F, 1.0F);
            GL11.glEnable(3553);
            GL11.glEnable(2929);
            GL11.glDisable(2848);
            GL11.glDisable(3042);
            GL11.glPopMatrix();
        }

        public static boolean isInView(Entity en) {
            return RenderUtils.isInViewFrustum(en.entity);
        }

        private static class EmptyGuiScreen extends GuiScreen {

            private EmptyGuiScreen() {
            }
        }

        public static class bloom {

            public static void prepare() {
                BlurUtils.prepareBloom();
            }

            public static void apply(int passes, float radius) {
                BlurUtils.bloomEnd(passes, radius);
            }
        }

        public static class blur {

            public static void prepare() {
                BlurUtils.prepareBlur();
            }

            public static void apply(int passes, float radius) {
                BlurUtils.blurEnd(passes, radius);
            }
        }
    }

    public static class util {

        public static final String colorSymbol = "§";

        public static String color(String message) {
            return Utils.formatColor(message);
        }

        public static String strip(String string) {
            return Utils.stripColor(string);
        }

        public static double round(double value, int decimals) {
            return Utils.round(value, decimals);
        }

        public static int randomInt(int min, int max) {
            return Utils.randomizeInt(min, max);
        }

        public static double randomDouble(double min, double max) {
            return Utils.randomizeDouble(min, max);
        }
    }

    public static class world {

        public static boolean exists() {
            return ScriptDefaults.mc.field_71441_e != null;
        }

        public static Entity spawnMob(String type, double x, double y, double z) {
            return ClientEntityManager.spawnMob(type, x, y, z);
        }

        public static Entity spawnMob(String type, Vec3 position) {
            return position == null ? null : spawnMob(type, position.x, position.y, position.z);
        }

        public static boolean removeClientEntity(Entity entity) {
            return ClientEntityManager.removeClientEntity(entity);
        }

        public static void clearClientEntities() {
            ClientEntityManager.clearClientEntities();
        }

        public static void playSound(String name, float volume, float pitch, double x, double y, double z) {
            ScriptDefaults.mc.field_71441_e.func_72980_b(x, y, z, name, volume, pitch, false);
        }

        public static boolean isValidEntity(Entity entity) {
            return ScriptDefaults.mc.field_71441_e != null && ScriptDefaults.mc.field_71441_e.field_72996_f.contains(entity);
        }

        public static Block getBlockAt(int x, int y, int z) {
            IBlockState state = BlockUtils.getBlockState(new BlockPos(x, y, z));
            return state == null ? new Block(Blocks.field_150350_a, new BlockPos(x, y, z)) : new Block(state, new BlockPos(x, y, z));
        }

        public static String getTitleText() {
            return ScriptDefaults.client.getTitle();
        }

        public static void setTitleText(String title, String subTitle, int timeFadeIn, int displayTime, int timeFadeOut) {
            ScriptDefaults.mc.field_71456_v.func_175178_a(title, subTitle, timeFadeIn, displayTime, timeFadeOut);
        }

        public static void clearTitleText() {
            ScriptDefaults.mc.field_71456_v.func_175178_a(null, null, -1, -1, -1);
        }

        public static Block getBlockAt(Vec3 pos) {
            IBlockState state = BlockUtils.getBlockState(new BlockPos(pos.x, pos.y, pos.z));
            return state == null ? new Block(Blocks.field_150350_a, new BlockPos(pos.x, pos.y, pos.z)) : new Block(state, new BlockPos(pos.x, pos.y, pos.z));
        }

        public static String getDimension() {
            return ScriptDefaults.mc.field_71441_e == null ? "" : ScriptDefaults.mc.field_71441_e.field_73011_w.func_80007_l();
        }

        public static List<Entity> getEntities() {
            List<Entity> entities = new ArrayList<>();
            if (ScriptDefaults.mc.field_71441_e == null) {
                return entities;
            } else {
                for (net.minecraft.entity.Entity entity : ScriptDefaults.mc.field_71441_e.field_72996_f) {
                    entities.add(Entity.convert(entity));
                }
                return entities;
            }
        }

        public static Entity getEntityById(int entityId) {
            return ScriptDefaults.mc.field_71441_e == null ? null : Entity.convert(ScriptDefaults.mc.field_71441_e.func_73045_a(entityId));
        }

        public static List<NetworkPlayer> getNetworkPlayers() {
            List<NetworkPlayer> entities = new ArrayList<>();
            for (NetworkPlayerInfo networkPlayerInfo : Utils.getTablist(false)) {
                entities.add(NetworkPlayer.convert(networkPlayerInfo));
            }
            return entities;
        }

        public static List<Entity> getPlayerEntities() {
            List<Entity> entities = new ArrayList<>();
            for (net.minecraft.entity.Entity entity : ScriptDefaults.mc.field_71441_e.field_73010_i) {
                entities.add(Entity.convert(entity));
            }
            return entities;
        }

        public static List<String> getScoreboard() {
            List<String> sidebarLines = Utils.getSidebarLines();
            return sidebarLines.isEmpty() ? null : sidebarLines;
        }

        public static String getTabHeader() {
            if (ScriptDefaults.mc != null && ScriptDefaults.mc.field_71456_v != null && ScriptDefaults.mc.field_71456_v.func_175181_h() != null) {
                IChatComponent header = ((IAccessorGuiPlayerTabOverlay) ScriptDefaults.mc.field_71456_v.func_175181_h()).getHeader();
                return header != null ? header.func_150260_c() : "";
            } else {
                return "";
            }
        }

        public static String getTabFooter() {
            if (ScriptDefaults.mc != null && ScriptDefaults.mc.field_71456_v != null && ScriptDefaults.mc.field_71456_v.func_175181_h() != null) {
                IChatComponent footer = ((IAccessorGuiPlayerTabOverlay) ScriptDefaults.mc.field_71456_v.func_175181_h()).getFooter();
                return footer != null ? footer.func_150260_c() : "";
            } else {
                return "";
            }
        }

        public static Map<String, List<String>> getTeams() {
            Map<String, List<String>> teams = new HashMap<>();
            for (Team team : ScriptDefaults.mc.field_71441_e.func_96441_U().func_96525_g()) {
                List<String> members = new ArrayList<>();
                for (String member : team.func_96670_d()) {
                    members.add(member);
                }
                teams.put(team.func_96661_b(), members);
            }
            return teams;
        }

        public static List<TileEntity> getTileEntities() {
            List<TileEntity> tileEntities = new ArrayList<>();
            for (net.minecraft.tileentity.TileEntity entity : ScriptDefaults.mc.field_71441_e.field_147482_g) {
                tileEntities.add(new TileEntity(entity));
            }
            return tileEntities;
        }
    }
}
