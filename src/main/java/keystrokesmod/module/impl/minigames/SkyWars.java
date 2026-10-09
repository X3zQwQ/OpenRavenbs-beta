package keystrokesmod.module.impl.minigames;

import java.awt.Color;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Map;
import java.util.UUID;
import keystrokesmod.clickgui.animation.ScrollOffsetAnimation;
import keystrokesmod.event.PreUpdateEvent;
import keystrokesmod.event.SendPacketEvent;
import keystrokesmod.event.UseItemEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.world.AntiBot;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.RenderUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemEnderPearl;
import net.minecraft.item.ItemMonsterPlacer;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.client.config.GuiButtonExt;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.RenderTickEvent;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

public class SkyWars extends Module {

    private static final long CLOSEST_ENEMY_ANIMATION_DURATION_MS = 100L;

    private static final long CLOSEST_ENEMY_FADE_DURATION_MS = 150L;

    private static final FloatBuffer ITEM_ALPHA_COLOR = BufferUtils.createFloatBuffer(4);

    public SliderSetting closestEnemy;

    public ButtonSetting strengthIndicator;

    public ButtonSetting onlyAuraHostileMobs;

    public ButtonSetting renderTimeWarp;

    public Map<EntityPlayer, Long> strengthPlayers = new HashMap<>();

    private Map<String, SkyWars.SpawnEggInfo> entitySpawnQueue = new LinkedHashMap<>();

    private Map<Vec3, Long> timeWarpPositions = new LinkedHashMap<>();

    public List<Integer> spawnedMobs = new ArrayList<>();

    private final int STRENGTH_COLOR = new Color(255, 0, 0).getRGB();

    private final int TIME_WARP_COLOR = new Color(210, 0, 255, 64).getRGB();

    private String[] KILL_MESSAGES = new String[] { " by ", " to ", " with ", " of ", " from ", " knight ", " for " };

    private boolean thrownPearl;

    private float closestEnemyPosX = Float.NaN;

    private float closestEnemyPosY = Float.NaN;

    private float closestEnemyRelativePosX = Float.NaN;

    private float closestEnemyRelativePosY = Float.NaN;

    private final ScrollOffsetAnimation closestEnemyWidthAnimation = new ScrollOffsetAnimation(100L);

    private final ScrollOffsetAnimation closestEnemyHeightAnimation = new ScrollOffsetAnimation(100L);

    private final ScrollOffsetAnimation closestEnemyContentAnimation = new ScrollOffsetAnimation(100L);

    private final SkyWars.SmoothFloatAnimation closestEnemyVisibilityAnimation = new SkyWars.SmoothFloatAnimation(150L);

    private boolean closestEnemySizeInitialized;

    private boolean closestEnemyVisibilityInitialized;

    private List<Integer> closestEnemySelection;

    private List<SkyWars.EnemyHudEntry> lastClosestEnemyEntries = Collections.emptyList();

    public static boolean isSkyWarsTeams = false;

    public SkyWars() {
        super("Sky Wars", Module.category.minigames);
        this.liteModule = true;
        this.registerSetting(this.closestEnemy = new SliderSetting("Closest enemy", true, 1.0, 1.0, 5.0, 1.0));
        this.registerSetting(new ButtonSetting("Edit positions", () -> mc.func_147108_a(new SkyWars.EditPositionScreen())));
        this.registerSetting(this.onlyAuraHostileMobs = new ButtonSetting("Only aura hostile mobs", true));
        this.registerSetting(this.renderTimeWarp = new ButtonSetting("Render time warp", true));
        this.registerSetting(this.strengthIndicator = new ButtonSetting("Strength indicator", true));
    }

    @Override
    public void onDisable() {
        this.clear();
    }

    @SubscribeEvent
    public void onPreUpdate(PreUpdateEvent e) {
        if (this.strengthIndicator.isToggled() && Utils.nullCheck() && !this.strengthPlayers.isEmpty() && Utils.getSkyWarsStatus() == 2) {
            int customMode = this.getCustomMode();
            if (customMode != 2) {
                isSkyWarsTeams = customMode == 1;
                long duration = isSkyWarsTeams ? 2000L : 5000L;
                for (EntityPlayer entityPlayer : new ArrayList<>(this.strengthPlayers.keySet())) {
                    long storedTime = this.strengthPlayers.get(entityPlayer);
                    long timePassed = System.currentTimeMillis() - storedTime;
                    if (timePassed >= duration || AntiBot.isBot(entityPlayer)) {
                        this.strengthPlayers.remove(entityPlayer);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent e) {
        if (e.type != 2 && Utils.nullCheck()) {
            String stripped = Utils.stripColor(e.message.func_150260_c());
            if (!stripped.isEmpty()) {
                if (stripped.equals("You will be warped back in 3 seconds!") && this.thrownPearl) {
                    this.timeWarpPositions.put(new Vec3(mc.field_71439_g.field_70142_S, mc.field_71439_g.field_70137_T, mc.field_71439_g.field_70136_U), System.currentTimeMillis());
                    this.thrownPearl = false;
                } else {
                    if (this.strengthIndicator.isToggled() && Utils.getSkyWarsStatus() == 2) {
                        if (this.getCustomMode() == 2) {
                            return;
                        }
                        if (stripped.endsWith(".") && Arrays.stream(this.KILL_MESSAGES).anyMatch(stripped::contains)) {
                            String[] parts = stripped.split(" ");
                            for (String part : parts) {
                                if (part.endsWith(".")) {
                                    String name = part.substring(0, part.length() - 1);
                                    for (EntityPlayer entity : mc.field_71441_e.field_73010_i) {
                                        if (entity.func_70005_c_().trim().equals(name) && entity != mc.field_71439_g) {
                                            this.strengthPlayers.put(entity, System.currentTimeMillis());
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent e) {
        if (Utils.nullCheck() && Utils.getSkyWarsStatus() == 2) {
            if (this.strengthIndicator.isToggled()) {
                for (EntityPlayer entityPlayer : this.strengthPlayers.keySet()) {
                    if (!AntiBot.isBot(entityPlayer)) {
                        RenderUtils.renderEntity(entityPlayer, 2, 0.0, 0.0, this.STRENGTH_COLOR, false);
                    }
                }
            }
            if (this.renderTimeWarp.isToggled()) {
                Iterator<Entry<Vec3, Long>> iterator = this.timeWarpPositions.entrySet().iterator();
                long currentTime = System.currentTimeMillis();
                while (iterator.hasNext()) {
                    Entry<Vec3, Long> entry = iterator.next();
                    Vec3 position = entry.getKey();
                    long timeThrown = entry.getValue();
                    if (currentTime - timeThrown >= 3050L) {
                        iterator.remove();
                    } else {
                        RenderUtils.drawPlayerBoundingBox(position, this.TIME_WARP_COLOR);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onWorldJoin(EntityJoinWorldEvent e) {
        if (e.entity == mc.field_71439_g) {
            this.clear();
        } else if (e.entity != null) {
            if (Utils.getSkyWarsStatus() != 2) {
                return;
            }
            String entityClassName = e.entity.getClass().getSimpleName();
            if (this.entitySpawnQueue.containsKey(entityClassName)) {
                Vec3 spawnPosition = new Vec3(e.entity.field_70165_t, e.entity.field_70163_u, e.entity.field_70161_v);
                SkyWars.SpawnEggInfo eggInfo = this.entitySpawnQueue.get(entityClassName);
                if (eggInfo.spawnPos.func_72438_d(spawnPosition) > 3.0 || Utils.timeBetween(mc.field_71439_g.field_70173_aa, eggInfo.tickSpawned) > 60L) {
                    return;
                }
                if (!this.entitySpawnQueue.remove(entityClassName, eggInfo)) {
                    return;
                }
                this.spawnedMobs.add(e.entity.func_145782_y());
            }
        }
    }

    @SubscribeEvent
    public void onSendPacket(SendPacketEvent e) {
        if (e.getPacket() instanceof C08PacketPlayerBlockPlacement) {
            C08PacketPlayerBlockPlacement p = (C08PacketPlayerBlockPlacement) e.getPacket();
            if (p.func_149568_f() != 255 && p.func_149574_g() != null && p.func_149574_g().func_77973_b() != null) {
                if (!(p.func_149574_g().func_77973_b() instanceof ItemMonsterPlacer)) {
                    return;
                }
                Class<? extends Entity> oclass = (Class<? extends Entity>) EntityList.field_75625_b.get(ItemMonsterPlacer.getEntityName(p.func_149574_g()));
                if (oclass == null) {
                    return;
                }
                this.entitySpawnQueue.put(oclass.getSimpleName(), new SkyWars.SpawnEggInfo(p.func_179724_a(), mc.field_71439_g.field_70173_aa));
            }
        }
    }

    @SubscribeEvent
    public void onUseItem(UseItemEvent e) {
        if (e.usedItemStack != null && e.usedItemStack.func_77973_b() instanceof ItemEnderPearl && Utils.getSkyWarsStatus() == 2) {
            ItemStack stack = e.usedItemStack;
            if (Utils.stripString(stack.func_82833_r()).equals("Time Warp Pearl")) {
                this.thrownPearl = true;
            } else if (stack.func_82833_r().startsWith("§b§l")) {
                List<String> toolTip = stack.func_82840_a(mc.field_71439_g, true);
                if (toolTip != null && toolTip.size() > 1 && Utils.stripString(toolTip.get(1)).contains("Teleports you back to your")) {
                    this.thrownPearl = true;
                }
            }
        }
    }

    @SubscribeEvent
    public void onRenderTick(RenderTickEvent event) {
        if (event.phase == Phase.END && Utils.nullCheck() && mc.field_71462_r == null && !mc.field_71474_y.field_74330_P) {
            boolean playingSkyWars = Utils.getSkyWarsStatus() == 2;
            boolean showClosestEnemyExit = !this.lastClosestEnemyEntries.isEmpty();
            boolean showClosestEnemy = playingSkyWars && this.closestEnemy.getInput() != -1.0;
            if (showClosestEnemy || showClosestEnemyExit) {
                ScaledResolution resolution = new ScaledResolution(mc);
                this.syncClosestEnemyPosition(resolution);
                if (showClosestEnemy) {
                    List<EntityPlayer> enemies = this.findClosestEnemies((int) this.closestEnemy.getInput());
                    List<SkyWars.EnemyHudEntry> entries = new ArrayList<>();
                    for (EntityPlayer enemy : enemies) {
                        double distance = Math.round(mc.field_71439_g.func_70032_d(enemy));
                        String name = enemy.func_145748_c_().func_150254_d() + "§r";
                        String distanceText = Utils.formatColor(Utils.getDistanceColor(distance)) + Utils.asWholeNum(distance) + "m§r";
                        entries.add(new SkyWars.EnemyHudEntry(enemy.func_145782_y(), name, distanceText, enemy.func_70694_bm()));
                    }
                    this.drawEnemyHudBox(entries, this.closestEnemyPosX, this.closestEnemyPosY);
                } else if (showClosestEnemyExit) {
                    this.drawEnemyHudBox(Collections.emptyList(), this.closestEnemyPosX, this.closestEnemyPosY);
                }
            }
        }
    }

    private void clear() {
        this.strengthPlayers.clear();
        this.spawnedMobs.clear();
        this.entitySpawnQueue.clear();
        this.timeWarpPositions.clear();
        this.thrownPearl = false;
        this.resetClosestEnemyHud();
    }

    private List<EntityPlayer> findClosestEnemies(int players) {
        Map<UUID, EntityPlayer> enemiesById = new LinkedHashMap<>();
        for (EntityPlayer player : mc.field_71441_e.field_73010_i) {
            if (player != null && player != mc.field_71439_g && player.func_70089_S() && !player.func_175149_v() && !AntiBot.isBot(player) && !Utils.isTeammate(player) && !Utils.isFriended(player)) {
                UUID playerId = player.func_110124_au();
                EntityPlayer existing = enemiesById.get(playerId);
                if (existing == null || player.field_70173_aa < existing.field_70173_aa || player.field_70173_aa == existing.field_70173_aa && player.func_145782_y() > existing.func_145782_y()) {
                    enemiesById.put(playerId, player);
                }
            }
        }
        List<EntityPlayer> enemies = new ArrayList<>(enemiesById.values());
        enemies.sort(Comparator.comparingDouble(mc.field_71439_g::func_70032_d));
        int amount = Math.min(Math.max(players, 0), enemies.size());
        return new ArrayList<>(enemies.subList(0, amount));
    }

    private void resetClosestEnemyHud() {
        this.closestEnemySizeInitialized = false;
        this.closestEnemyVisibilityInitialized = false;
        this.closestEnemySelection = null;
        this.lastClosestEnemyEntries = Collections.emptyList();
    }

    public float getClosestEnemyPosX() {
        this.syncClosestEnemyPosition();
        return this.closestEnemyPosX;
    }

    public float getClosestEnemyPosY() {
        this.syncClosestEnemyPosition();
        return this.closestEnemyPosY;
    }

    public float getClosestEnemyRelativePosX() {
        this.syncClosestEnemyPosition();
        return this.closestEnemyRelativePosX;
    }

    public float getClosestEnemyRelativePosY() {
        this.syncClosestEnemyPosition();
        return this.closestEnemyRelativePosY;
    }

    public void setClosestEnemyRelativePosition(float normalizedX, float normalizedY) {
        this.closestEnemyRelativePosX = normalizedX;
        this.closestEnemyRelativePosY = normalizedY;
        this.syncClosestEnemyPosition();
    }

    public void setClosestEnemyAbsolutePosition(float absoluteX, float absoluteY) {
        this.setClosestEnemyAbsolutePosition(absoluteX, absoluteY, new ScaledResolution(mc));
    }

    public void resetClosestEnemyPosition() {
        this.resetClosestEnemyPosition(new ScaledResolution(mc));
    }

    private void syncClosestEnemyPosition() {
        this.syncClosestEnemyPosition(new ScaledResolution(mc));
    }

    private void syncClosestEnemyPosition(ScaledResolution resolution) {
        int scaledWidth = Math.max(1, resolution.func_78326_a());
        int scaledHeight = Math.max(1, resolution.func_78328_b());
        if (Float.isNaN(this.closestEnemyRelativePosX) || Float.isNaN(this.closestEnemyRelativePosY)) {
            if (Float.isNaN(this.closestEnemyPosX) || Float.isNaN(this.closestEnemyPosY)) {
                this.closestEnemyPosX = 0.0F;
                this.closestEnemyPosY = scaledHeight / 4.0F - mc.field_71466_p.field_78288_b / 2.0F;
            }
            this.closestEnemyRelativePosX = this.closestEnemyPosX / scaledWidth;
            this.closestEnemyRelativePosY = this.closestEnemyPosY / scaledHeight;
        }
        this.closestEnemyPosX = this.closestEnemyRelativePosX * scaledWidth;
        this.closestEnemyPosY = this.closestEnemyRelativePosY * scaledHeight;
    }

    private void setClosestEnemyAbsolutePosition(float absoluteX, float absoluteY, ScaledResolution resolution) {
        this.closestEnemyPosX = absoluteX;
        this.closestEnemyPosY = absoluteY;
        this.closestEnemyRelativePosX = absoluteX / Math.max(1, resolution.func_78326_a());
        this.closestEnemyRelativePosY = absoluteY / Math.max(1, resolution.func_78328_b());
    }

    private void resetClosestEnemyPosition(ScaledResolution resolution) {
        this.setClosestEnemyAbsolutePosition(0.0F, resolution.func_78328_b() / 4.0F - mc.field_71466_p.field_78288_b / 2.0F, resolution);
    }

    private void renderHeldItem(ItemStack heldItem, float x, float y, float opacity) {
        float scale = 0.6875F;
        boolean fading = opacity < 1.0F;
        GL11.glPushMatrix();
        try {
            GL11.glTranslatef(x, y, 0.0F);
            GL11.glScalef(scale, scale, 1.0F);
            if (fading) {
                this.setItemOpacity(opacity);
            }
            RenderUtils.renderItemAndEffectIntoGui3D(heldItem, 0, 0);
        } finally {
            if (fading) {
                this.resetItemOpacity();
            }
            GL11.glPopMatrix();
        }
    }

    private void setItemOpacity(float opacity) {
        OpenGlHelper.func_77473_a(OpenGlHelper.field_77478_a);
        ((Buffer) ITEM_ALPHA_COLOR).clear();
        ((Buffer) ITEM_ALPHA_COLOR.put(1.0F).put(1.0F).put(1.0F).put(opacity)).flip();
        GL11.glTexEnvi(8960, 8704, OpenGlHelper.field_176095_s);
        GL11.glTexEnvi(8960, OpenGlHelper.field_176099_x, 8448);
        GL11.glTexEnvi(8960, OpenGlHelper.field_176098_y, OpenGlHelper.field_77478_a);
        GL11.glTexEnvi(8960, OpenGlHelper.field_176097_z, OpenGlHelper.field_176093_u);
        GL11.glTexEnvi(8960, OpenGlHelper.field_176081_B, 768);
        GL11.glTexEnvi(8960, OpenGlHelper.field_176082_C, 768);
        GL11.glTexEnvi(8960, OpenGlHelper.field_176077_E, 8448);
        GL11.glTexEnvi(8960, OpenGlHelper.field_176078_F, OpenGlHelper.field_77478_a);
        GL11.glTexEnvi(8960, OpenGlHelper.field_176079_G, OpenGlHelper.field_176092_v);
        GL11.glTexEnvi(8960, OpenGlHelper.field_176085_I, 770);
        GL11.glTexEnvi(8960, OpenGlHelper.field_176086_J, 770);
        GL11.glTexEnv(8960, 8705, ITEM_ALPHA_COLOR);
    }

    private void resetItemOpacity() {
        OpenGlHelper.func_77473_a(OpenGlHelper.field_77478_a);
        ((Buffer) ITEM_ALPHA_COLOR).clear();
        ((Buffer) ITEM_ALPHA_COLOR.put(1.0F).put(1.0F).put(1.0F).put(1.0F)).flip();
        GL11.glTexEnvi(8960, 8704, 8448);
        GL11.glTexEnv(8960, 8705, ITEM_ALPHA_COLOR);
    }

    private SkyWars.HudBoxBounds drawEnemyHudBox(List<SkyWars.EnemyHudEntry> entries, float x, float textY) {
        float verticalPadding = 4.0F;
        float left = x - 10.0F;
        float top = textY - verticalPadding;
        float contentProgress = this.updateClosestEnemySelection(entries);
        float visibility = this.updateClosestEnemyVisibility(entries);
        boolean fadingOut = entries.isEmpty();
        List<SkyWars.EnemyHudEntry> renderedEntries = fadingOut ? this.lastClosestEnemyEntries : entries;
        if (!renderedEntries.isEmpty() && !(visibility <= 0.0F)) {
            float horizontalPadding = 3.0F;
            float columnSpacing = 6.0F;
            float itemSpacing = 3.0F;
            float rowHeight = mc.field_71466_p.field_78288_b + 4.0F;
            float textX = x + horizontalPadding;
            float maxNameWidth = 0.0F;
            float maxDistanceWidth = 0.0F;
            boolean hasItem = false;
            for (SkyWars.EnemyHudEntry entry : renderedEntries) {
                maxNameWidth = Math.max(maxNameWidth, (float) mc.field_71466_p.func_78256_a(entry.name));
                maxDistanceWidth = Math.max(maxDistanceWidth, (float) mc.field_71466_p.func_78256_a(entry.distance));
                if (entry.heldItem != null) {
                    hasItem = true;
                }
            }
            float distanceX = textX + maxNameWidth + columnSpacing;
            float itemX = distanceX + maxDistanceWidth + itemSpacing;
            float contentOffset = (1.0F - contentProgress) * 6.0F;
            float contentOpacity = contentProgress * visibility;
            int contentColor = Utils.mergeAlpha(Color.WHITE.getRGB(), Math.max(4, Math.round(255.0F * contentOpacity)));
            float targetRight = distanceX + maxDistanceWidth + horizontalPadding + 1.0F;
            if (hasItem) {
                targetRight = itemX + 14.5F;
            }
            float targetBottom = textY + (renderedEntries.size() - 1) * rowHeight + mc.field_71466_p.field_78288_b + verticalPadding - 1.0F;
            if (!fadingOut) {
                this.updateClosestEnemySize(targetRight - left, targetBottom - top);
            }
            float right = left + this.closestEnemyWidthAnimation.getValue();
            float bottom = top + this.closestEnemyHeightAnimation.getValue();
            int backgroundColor = Utils.mergeAlpha(Color.BLACK.getRGB(), Math.round(120.0F * visibility));
            GL11.glPushAttrib(24577);
            try {
                this.enableClosestEnemyAlphaBlending();
                RenderUtils.drawRoundedRectangle(left, top, right, bottom, 7.0F, backgroundColor);
                for (int i = 0; i < renderedEntries.size(); i++) {
                    SkyWars.EnemyHudEntry enemyHudEntry = renderedEntries.get(i);
                    float rowY = textY + i * rowHeight;
                    this.enableClosestEnemyAlphaBlending();
                    mc.field_71466_p.func_175065_a(enemyHudEntry.name, textX + contentOffset, rowY, contentColor, true);
                    float distanceWidth = mc.field_71466_p.func_78256_a(enemyHudEntry.distance);
                    mc.field_71466_p.func_175065_a(enemyHudEntry.distance, distanceX + maxDistanceWidth - distanceWidth + contentOffset, rowY, contentColor, true);
                    if (enemyHudEntry.heldItem != null) {
                        this.renderHeldItem(enemyHudEntry.heldItem, itemX + contentOffset, rowY - 2.0F, contentOpacity);
                    }
                }
            } finally {
                GL11.glPopAttrib();
                GlStateManager.func_179131_c(1.0F, 1.0F, 1.0F, 1.0F);
                GlStateManager.func_179147_l();
                GlStateManager.func_179084_k();
            }
            return new SkyWars.HudBoxBounds(left, top, right, bottom);
        } else {
            return null;
        }
    }

    private void enableClosestEnemyAlphaBlending() {
        GlStateManager.func_179147_l();
        GlStateManager.func_179120_a(770, 771, 1, 0);
    }

    private float updateClosestEnemyVisibility(List<SkyWars.EnemyHudEntry> entries) {
        if (!entries.isEmpty()) {
            this.lastClosestEnemyEntries = new ArrayList<>(entries);
            if (!this.closestEnemyVisibilityInitialized) {
                this.closestEnemyVisibilityAnimation.reset(1.0F);
                this.closestEnemyVisibilityInitialized = true;
            } else if (Float.compare(this.closestEnemyVisibilityAnimation.getTarget(), 1.0F) != 0) {
                this.closestEnemyVisibilityAnimation.setTarget(1.0F);
            }
        } else {
            if (this.lastClosestEnemyEntries.isEmpty()) {
                return 0.0F;
            }
            if (!this.closestEnemyVisibilityInitialized) {
                this.closestEnemyVisibilityAnimation.reset(1.0F);
                this.closestEnemyVisibilityInitialized = true;
            }
            if (Float.compare(this.closestEnemyVisibilityAnimation.getTarget(), 0.0F) != 0) {
                if (this.closestEnemySizeInitialized) {
                    this.closestEnemyWidthAnimation.reset(this.closestEnemyWidthAnimation.getValue());
                    this.closestEnemyHeightAnimation.reset(this.closestEnemyHeightAnimation.getValue());
                }
                this.closestEnemyVisibilityAnimation.setTarget(0.0F);
            }
        }
        float visibility = this.closestEnemyVisibilityAnimation.getValue();
        if (entries.isEmpty() && visibility <= 0.0F) {
            this.lastClosestEnemyEntries = Collections.emptyList();
        }
        return visibility;
    }

    private float updateClosestEnemySelection(List<SkyWars.EnemyHudEntry> entries) {
        List<Integer> selection = new ArrayList<>(entries.size());
        for (SkyWars.EnemyHudEntry entry : entries) {
            selection.add(entry.entityId);
        }
        if (this.closestEnemySelection == null) {
            this.closestEnemySelection = selection;
            this.closestEnemyContentAnimation.reset(1.0F);
        } else if (!this.closestEnemySelection.equals(selection)) {
            this.closestEnemySelection = selection;
            if (selection.isEmpty()) {
                this.closestEnemyContentAnimation.reset(1.0F);
            } else {
                this.closestEnemyContentAnimation.reset(0.0F);
                this.closestEnemyContentAnimation.setTarget(1.0F);
            }
        }
        return this.closestEnemyContentAnimation.getValue();
    }

    private void updateClosestEnemySize(float width, float height) {
        if (!this.closestEnemySizeInitialized) {
            this.closestEnemyWidthAnimation.reset(width);
            this.closestEnemyHeightAnimation.reset(height);
            this.closestEnemySizeInitialized = true;
        } else {
            if (Float.compare(this.closestEnemyWidthAnimation.getTarget(), width) != 0) {
                this.closestEnemyWidthAnimation.setTarget(width);
            }
            if (Float.compare(this.closestEnemyHeightAnimation.getTarget(), height) != 0) {
                this.closestEnemyHeightAnimation.setTarget(height);
            }
        }
    }

    public static boolean onlyAuraHostiles() {
        return ModuleManager.skyWars != null && ModuleManager.skyWars.isEnabled() && ModuleManager.skyWars.onlyAuraHostileMobs.isToggled() && Utils.getSkyWarsStatus() == 2;
    }

    public int getCustomMode() {
        List<String> sidebar = Utils.getSidebarLines();
        if (sidebar.isEmpty()) {
            return -1;
        } else {
            for (String line : sidebar) {
                line = Utils.stripColor(line);
                if (line.startsWith("Teams left: ")) {
                    return 1;
                }
                if (line.startsWith("Lab: ") || line.startsWith("Mode: Mini")) {
                    return 2;
                }
            }
            return -1;
        }
    }

    private class EditPositionScreen extends GuiScreen {

        private GuiButtonExt resetPosition;

        private SkyWars.HudBoxBounds bounds;

        private boolean dragging;

        private float actualX;

        private float actualY;

        private float dragStartX;

        private float dragStartY;

        private int lastMouseX;

        private int lastMouseY;

        private EditPositionScreen() {
        }

        public void func_73866_w_() {
            super.func_73866_w_();
            this.field_146292_n.add(this.resetPosition = new GuiButtonExt(1, this.field_146294_l - 95, this.field_146295_m - 25, 90, 20, "Reset position"));
            SkyWars.this.syncClosestEnemyPosition(new ScaledResolution(this.field_146297_k));
            this.syncEditorPosition();
        }

        public void func_73863_a(int mouseX, int mouseY, float partialTicks) {
            ScaledResolution resolution = new ScaledResolution(this.field_146297_k);
            if (!this.dragging) {
                SkyWars.this.syncClosestEnemyPosition(resolution);
                this.syncEditorPosition();
            }
            func_73734_a(0, 0, this.field_146294_l, this.field_146295_m, -1308622848);
            SkyWars.this.setClosestEnemyAbsolutePosition(this.actualX, this.actualY, resolution);
            List<SkyWars.EnemyHudEntry> previewEntries = new ArrayList<>();
            int previewPlayers = Math.max(1, (int) SkyWars.this.closestEnemy.getInput());
            for (int i = 0; i < previewPlayers; i++) {
                String name = this.field_146297_k.field_71439_g.func_145748_c_().func_150254_d() + "§r";
                String distance = "§a" + (16 + i * 3) + "m§r";
                previewEntries.add(new SkyWars.EnemyHudEntry(this.field_146297_k.field_71439_g.func_145782_y(), name, distance, this.field_146297_k.field_71439_g.func_70694_bm()));
            }
            this.bounds = SkyWars.this.drawEnemyHudBox(previewEntries, this.actualX, this.actualY);
            String message = "Drag the box to reposition it.";
            int messageX = resolution.func_78326_a() / 2 - this.field_146289_q.func_78256_a(message) / 2;
            int messageY = resolution.func_78328_b() / 2 - 10;
            RenderUtils.drawColoredString(message, '-', messageX, messageY, 2L, 0L, true, this.field_146289_q);
            try {
                this.func_146269_k();
            } catch (IOException exception) {
            }
            super.func_73863_a(mouseX, mouseY, partialTicks);
        }

        protected void func_146273_a(int mouseX, int mouseY, int button, long timeSinceLastClick) {
            super.func_146273_a(mouseX, mouseY, button, timeSinceLastClick);
            if (button == 0) {
                if (this.dragging) {
                    this.actualX = this.dragStartX + mouseX - this.lastMouseX;
                    this.actualY = this.dragStartY + mouseY - this.lastMouseY;
                } else if (this.bounds != null && this.bounds.contains(mouseX, mouseY)) {
                    this.dragging = true;
                    this.dragStartX = this.actualX;
                    this.dragStartY = this.actualY;
                    this.lastMouseX = mouseX;
                    this.lastMouseY = mouseY;
                }
            }
        }

        protected void func_146286_b(int mouseX, int mouseY, int state) {
            super.func_146286_b(mouseX, mouseY, state);
            if (state == 0) {
                this.dragging = false;
            }
        }

        public void func_146284_a(GuiButton button) {
            if (button == this.resetPosition) {
                this.dragging = false;
                SkyWars.this.resetClosestEnemyPosition(new ScaledResolution(this.field_146297_k));
                this.syncEditorPosition();
            }
        }

        private void syncEditorPosition() {
            this.actualX = SkyWars.this.closestEnemyPosX;
            this.actualY = SkyWars.this.closestEnemyPosY;
        }

        public boolean func_73868_f() {
            return false;
        }
    }

    private static final class EnemyHudEntry {

        private final int entityId;

        private final String name;

        private final String distance;

        private final ItemStack heldItem;

        private EnemyHudEntry(int entityId, String name, String distance, ItemStack heldItem) {
            this.entityId = entityId;
            this.name = name;
            this.distance = distance;
            this.heldItem = heldItem;
        }
    }

    private static final class HudBoxBounds {

        private final float left;

        private final float top;

        private final float right;

        private final float bottom;

        private HudBoxBounds(float left, float top, float right, float bottom) {
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
        }

        private boolean contains(int mouseX, int mouseY) {
            return mouseX >= this.left && mouseX <= this.right && mouseY >= this.top && mouseY <= this.bottom;
        }
    }

    private static final class SmoothFloatAnimation {

        private final long durationMs;

        private float from;

        private float target;

        private long startedAt;

        private SmoothFloatAnimation(long durationMs) {
            this.durationMs = durationMs;
        }

        private void reset(float value) {
            this.from = value;
            this.target = value;
            this.startedAt = 0L;
        }

        private void setTarget(float value) {
            this.from = this.getValue();
            this.target = value;
            this.startedAt = System.currentTimeMillis();
        }

        private float getValue() {
            if (this.startedAt == 0L) {
                return this.target;
            } else {
                float progress = (float) (System.currentTimeMillis() - this.startedAt) / (float) this.durationMs;
                if (progress >= 1.0F) {
                    this.reset(this.target);
                    return this.target;
                } else {
                    progress = Math.max(0.0F, progress);
                    float eased = progress * progress * (3.0F - 2.0F * progress);
                    return this.from + (this.target - this.from) * eased;
                }
            }
        }

        private float getTarget() {
            return this.target;
        }
    }

    public static class SpawnEggInfo {

        public Vec3 spawnPos;

        public int tickSpawned;

        public SpawnEggInfo(BlockPos spawnPos, int tickSpawned) {
            this.spawnPos = new Vec3(spawnPos.func_177958_n(), spawnPos.func_177956_o(), spawnPos.func_177952_p());
            this.tickSpawned = tickSpawned;
        }
    }
}
