package keystrokesmod.module.impl.minigames;

import java.awt.Color;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import keystrokesmod.clickgui.animation.ScrollOffsetAnimation;
import keystrokesmod.event.ReceivePacketEvent;
import keystrokesmod.event.SendPacketEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.world.AntiBot;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.DescriptionSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.BlockUtils;
import keystrokesmod.utility.RenderUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.block.BlockBed;
import net.minecraft.block.BlockObsidian;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.monster.EntityIronGolem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemEnderPearl;
import net.minecraft.item.ItemFireball;
import net.minecraft.item.ItemMonsterPlacer;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.server.S23PacketBlockChange;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.StringUtils;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.player.PlayerUseItemEvent.Finish;
import net.minecraftforge.fml.client.config.GuiButtonExt;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.RenderTickEvent;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

public class BedWars extends Module {

    private static final long MAGIC_MILK_DURATION_MS = 30000L;

    private static final long CLOSEST_ENEMY_ANIMATION_DURATION_MS = 100L;

    private static final long CLOSEST_ENEMY_FADE_DURATION_MS = 150L;

    private static final FloatBuffer ITEM_ALPHA_COLOR = BufferUtils.createFloatBuffer(4);

    private static final Pattern SCOREBOARD_DATE = Pattern.compile("\\d{1,2}/\\d{1,2}/\\d{2,4}");

    private static final double OWN_BED_PROTECTION_RADIUS_SQ = 800.0;

    private static final int OWN_BED_SEARCH_HORIZONTAL_RADIUS = 24;

    private static final int OWN_BED_SEARCH_VERTICAL_RADIUS = 6;

    private final SliderSetting closestEnemy;

    private final SliderSetting alertInterval;

    private final ButtonSetting whitelistOwnBed;

    private final ButtonSetting magicMilkTimer;

    private final ButtonSetting removeGameId;

    private final ButtonSetting targetUnicorn;

    private ButtonSetting bow;

    private ButtonSetting diamondArmor;

    private ButtonSetting dreamDefender;

    private ButtonSetting fireball;

    private ButtonSetting enderPearl;

    private ButtonSetting knockbackStick;

    private ButtonSetting magicMilk;

    private ButtonSetting obsidian;

    private ButtonSetting potions;

    private ButtonSetting waterBucket;

    private ButtonSetting ignoreTeammate;

    private ButtonSetting shouldPing;

    private List<String> armoredPlayer = new ArrayList<>();

    private Map<String, String> lastHeldMap = new ConcurrentHashMap<>();

    private Map<String, Long> lastAlertMap = new ConcurrentHashMap<>();

    private Map<BlockPos, Long> obsidianPos = new HashMap<>();

    public List<SkyWars.SpawnEggInfo> entitySpawnQueue = new ArrayList<>();

    public List<Integer> spawnedMobs = new ArrayList<>();

    private BlockPos spawnAnchor;

    private Vec3 ownBedCenter;

    private boolean ownBedDestroyed;

    private boolean pendingSpawnAnchorCapture;

    private boolean waitingForRespawn;

    private long respawnMessageTime;

    private long magicMilkExpiresAt;

    private float closestEnemyPosX = Float.NaN;

    private float closestEnemyPosY = Float.NaN;

    private float closestEnemyRelativePosX = Float.NaN;

    private float closestEnemyRelativePosY = Float.NaN;

    private final ScrollOffsetAnimation closestEnemyWidthAnimation = new ScrollOffsetAnimation(100L);

    private final ScrollOffsetAnimation closestEnemyHeightAnimation = new ScrollOffsetAnimation(100L);

    private final ScrollOffsetAnimation closestEnemyContentAnimation = new ScrollOffsetAnimation(100L);

    private final BedWars.SmoothFloatAnimation closestEnemyVisibilityAnimation = new BedWars.SmoothFloatAnimation(150L);

    private boolean closestEnemySizeInitialized;

    private boolean closestEnemyVisibilityInitialized;

    private List<Integer> closestEnemySelection;

    private List<BedWars.EnemyHudEntry> lastClosestEnemyEntries = Collections.emptyList();

    private float magicMilkPosX = Float.NaN;

    private float magicMilkPosY = Float.NaN;

    private float magicMilkRelativePosX = Float.NaN;

    private float magicMilkRelativePosY = Float.NaN;

    private int obsidianColor = new Color(106, 13, 173).getRGB();

    public BedWars() {
        super("Bed Wars", Module.category.minigames);
        this.registerSetting(this.closestEnemy = new SliderSetting("Closest enemy", true, 1.0, 1.0, 5.0, 1.0));
        this.registerSetting(this.removeGameId = new ButtonSetting("Remove game id", false));
        this.registerSetting(this.targetUnicorn = new ButtonSetting("Target unicorn", false));
        this.registerSetting(this.magicMilkTimer = new ButtonSetting("Magic milk timer", true));
        this.registerSetting(this.whitelistOwnBed = new ButtonSetting("Whitelist own bed", true));
        this.registerSetting(new ButtonSetting("Edit positions", () -> mc.func_147108_a(new BedWars.EditPositionsScreen())));
        this.registerSetting(new DescriptionSetting("Game alerts"));
        this.registerSetting(this.alertInterval = new SliderSetting("Alert interval", " second", 5.0, 0.0, 30.0, 1.0));
        this.registerSetting(this.bow = new ButtonSetting("Bow", true));
        this.registerSetting(this.diamondArmor = new ButtonSetting("Diamond armor", true));
        this.registerSetting(this.dreamDefender = new ButtonSetting("Dream defender", true));
        this.registerSetting(this.fireball = new ButtonSetting("Fireball", false));
        this.registerSetting(this.knockbackStick = new ButtonSetting("Knockback stick", true));
        this.registerSetting(this.magicMilk = new ButtonSetting("Magic milk", true));
        this.registerSetting(this.obsidian = new ButtonSetting("Obsidian", true));
        this.registerSetting(this.enderPearl = new ButtonSetting("Ender pearl", true));
        this.registerSetting(this.potions = new ButtonSetting("Potions", true));
        this.registerSetting(this.waterBucket = new ButtonSetting("Water bucket", true));
        this.registerSetting(this.ignoreTeammate = new ButtonSetting("Ignore teammate", false));
        this.registerSetting(this.shouldPing = new ButtonSetting("Should ping", true));
        this.closetModule = true;
        this.liteModule = true;
    }

    public static boolean shouldTargetUnicorn() {
        return ModuleManager.bedwars != null && ModuleManager.bedwars.isEnabled() && ModuleManager.bedwars.targetUnicorn.isToggled() && Utils.getBedwarsStatus() == 2;
    }

    public String filterScoreboardLine(String line) {
        if (!this.removeGameId.isToggled()) {
            return line;
        } else {
            String visibleLine = StringUtils.func_76338_a(line);
            Matcher date = SCOREBOARD_DATE.matcher(visibleLine);
            if (date.find() && !visibleLine.substring(date.end()).trim().isEmpty()) {
                int bedWarsStatus = Utils.getBedwarsStatus();
                if (bedWarsStatus != 1 && bedWarsStatus != 2) {
                    return line;
                } else {
                    int intValue = 0;
                    int intValue2 = 0;
                    while (intValue < line.length() && intValue2 < date.end()) {
                        if (line.charAt(intValue) == 167 && intValue + 1 < line.length()) {
                            intValue += 2;
                        } else {
                            intValue++;
                            intValue2++;
                        }
                    }
                    return line.substring(0, intValue);
                }
            } else {
                return line;
            }
        }
    }

    @Override
    public void onDisable() {
        this.entitySpawnQueue.clear();
        this.spawnedMobs.clear();
        this.lastAlertMap.clear();
        this.resetSpawnTracking();
        this.magicMilkExpiresAt = 0L;
        this.resetClosestEnemyHud();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRenderWorld(RenderWorldLastEvent e) {
        if (Utils.nullCheck() && this.obsidian.isToggled()) {
            if (this.obsidianPos.isEmpty()) {
                return;
            }
            try {
                List<BlockPos> blocksToRender = new ArrayList<>();
                Iterator<Entry<BlockPos, Long>> iterator = this.obsidianPos.entrySet().iterator();
                while (iterator.hasNext()) {
                    Entry<BlockPos, Long> entry = iterator.next();
                    BlockPos blockPos = entry.getKey();
                    Long receivedMs = entry.getValue();
                    if (!(mc.field_71441_e.func_180495_p(blockPos).func_177230_c() instanceof BlockObsidian) && Utils.timeBetween(System.currentTimeMillis(), receivedMs) >= 500L) {
                        iterator.remove();
                    } else {
                        blocksToRender.add(blockPos);
                    }
                }
                this.renderObsidianOverlays(this.mergeObsidianBounds(blocksToRender));
            } catch (Exception exception) {
            }
        }
    }

    private List<AxisAlignedBB> mergeObsidianBounds(List<BlockPos> blocks) {
        List<AxisAlignedBB> bounds = new ArrayList<>();
        for (BlockPos pos : blocks) {
            bounds.add(new AxisAlignedBB(pos.func_177958_n(), pos.func_177956_o(), pos.func_177952_p(), pos.func_177958_n() + 1.0, pos.func_177956_o() + 1.0, pos.func_177952_p() + 1.0));
        }
        boolean merged;
        label32: do {
            merged = false;
            for (int i = 0; i < bounds.size(); i++) {
                for (int j = i + 1; j < bounds.size(); j++) {
                    AxisAlignedBB combined = this.mergeAdjacentBounds(bounds.get(i), bounds.get(j));
                    if (combined != null) {
                        bounds.set(i, combined);
                        bounds.remove(j);
                        merged = true;
                        continue label32;
                    }
                }
            }
        } while (merged);
        return bounds;
    }

    private AxisAlignedBB mergeAdjacentBounds(AxisAlignedBB first, AxisAlignedBB second) {
        boolean sameX = first.field_72340_a == second.field_72340_a && first.field_72336_d == second.field_72336_d;
        boolean sameY = first.field_72338_b == second.field_72338_b && first.field_72337_e == second.field_72337_e;
        boolean sameZ = first.field_72339_c == second.field_72339_c && first.field_72334_f == second.field_72334_f;
        if (!sameY || !sameZ || first.field_72336_d != second.field_72340_a && second.field_72336_d != first.field_72340_a) {
            if (!sameX || !sameZ || first.field_72337_e != second.field_72338_b && second.field_72337_e != first.field_72338_b) {
                return !sameX || !sameY || first.field_72334_f != second.field_72339_c && second.field_72334_f != first.field_72339_c ? null : new AxisAlignedBB(first.field_72340_a, first.field_72338_b, Math.min(first.field_72339_c, second.field_72339_c), first.field_72336_d, first.field_72337_e, Math.max(first.field_72334_f, second.field_72334_f));
            } else {
                return new AxisAlignedBB(first.field_72340_a, Math.min(first.field_72338_b, second.field_72338_b), first.field_72339_c, first.field_72336_d, Math.max(first.field_72337_e, second.field_72337_e), first.field_72334_f);
            }
        } else {
            return new AxisAlignedBB(Math.min(first.field_72340_a, second.field_72340_a), first.field_72338_b, first.field_72339_c, Math.max(first.field_72336_d, second.field_72336_d), first.field_72337_e, first.field_72334_f);
        }
    }

    private void renderObsidianOverlays(List<AxisAlignedBB> obsidianBounds) {
        if (!obsidianBounds.isEmpty()) {
            float red = (this.obsidianColor >> 16 & 0xFF) / 255.0F;
            float green = (this.obsidianColor >> 8 & 0xFF) / 255.0F;
            float blue = (this.obsidianColor & 0xFF) / 255.0F;
            RenderManager renderManager = mc.func_175598_ae();
            GL11.glPushMatrix();
            GL11.glBlendFunc(770, 771);
            GL11.glEnable(3042);
            GL11.glDisable(3553);
            GL11.glDisable(2929);
            GL11.glDepthMask(false);
            try {
                for (AxisAlignedBB bounds : obsidianBounds) {
                    AxisAlignedBB renderedBounds = bounds.func_72317_d(-renderManager.field_78730_l, -renderManager.field_78731_m, -renderManager.field_78728_n);
                    RenderUtils.drawBoundingBox(renderedBounds, red, green, blue, 0.25F);
                }
            } finally {
                GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
                GL11.glEnable(3553);
                GL11.glEnable(2929);
                GL11.glDepthMask(true);
                GL11.glDisable(3042);
                GL11.glPopMatrix();
            }
        }
    }

    @SubscribeEvent
    public void onRenderTick(RenderTickEvent event) {
        if (event.phase == Phase.END && Utils.nullCheck() && mc.field_71462_r == null && !mc.field_71474_y.field_74330_P) {
            boolean playingBedWars = Utils.getBedwarsStatus() == 2;
            boolean showClosestEnemyExit = !this.lastClosestEnemyEntries.isEmpty();
            long remainingMilkTime = this.magicMilkExpiresAt - System.currentTimeMillis();
            boolean showMagicMilkTimer = playingBedWars && this.magicMilkTimer.isToggled() && remainingMilkTime > 0L;
            boolean showClosestEnemy = playingBedWars && this.closestEnemy.getInput() != -1.0;
            if (showClosestEnemy || showClosestEnemyExit || showMagicMilkTimer) {
                ScaledResolution resolution = new ScaledResolution(mc);
                this.syncHudPositions(resolution);
                if (showClosestEnemy) {
                    Vec3 bedReference = !this.ownBedDestroyed ? this.getOwnBedReference() : null;
                    List<EntityPlayer> enemies = this.findClosestEnemies(bedReference, (int) this.closestEnemy.getInput());
                    List<BedWars.EnemyHudEntry> entries = new ArrayList<>();
                    for (EntityPlayer enemy : enemies) {
                        double distance = Math.round(this.distanceToReference(enemy, bedReference));
                        String name = enemy.func_145748_c_().func_150254_d() + "§r";
                        String distanceText = Utils.formatColor(Utils.getDistanceColor(distance)) + Utils.asWholeNum(distance) + "m§r";
                        entries.add(new BedWars.EnemyHudEntry(enemy.func_145782_y(), name, distanceText, enemy.func_70694_bm()));
                    }
                    this.drawEnemyHudBox(entries, this.closestEnemyPosX, this.closestEnemyPosY);
                } else if (showClosestEnemyExit) {
                    this.drawEnemyHudBox(Collections.emptyList(), this.closestEnemyPosX, this.closestEnemyPosY);
                }
                if (showMagicMilkTimer) {
                    String text = String.format(Locale.ROOT, "Magic Milk: §b%.1fs", remainingMilkTime / 1000.0);
                    this.drawHudBox(text, this.magicMilkPosX, this.magicMilkPosY);
                }
            }
        }
    }

    private BedWars.HudBoxBounds drawHudBox(String text, float x, float textY) {
        return this.drawHudBox(text, null, x, textY);
    }

    private BedWars.HudBoxBounds drawHudBox(String text, ItemStack heldItem, float x, float textY) {
        float horizontalPadding = 3.0F;
        float verticalPadding = 4.0F;
        float textX = x + horizontalPadding;
        float textWidth = mc.field_71466_p.func_78256_a(text);
        float left = x - 10.0F;
        float top = textY - verticalPadding;
        float right = textX + textWidth + horizontalPadding + 1.0F;
        float bottom = textY + mc.field_71466_p.field_78288_b + verticalPadding - 1.0F;
        float itemX = 0.0F;
        if (heldItem != null) {
            itemX = textX + textWidth + 3.0F;
            right = itemX + 14.5F;
        }
        RenderUtils.drawRoundedRectangle(left, top, right, bottom, 7.0F, 2013265920);
        mc.field_71466_p.func_175065_a(text, textX, textY, Color.WHITE.getRGB(), true);
        if (heldItem != null) {
            this.renderHeldItem(heldItem, itemX, top + 2.0F);
        }
        return new BedWars.HudBoxBounds(left, top, right, bottom);
    }

    private void renderHeldItem(ItemStack heldItem, float x, float y) {
        this.renderHeldItem(heldItem, x, y, 1.0F);
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

    @SubscribeEvent
    public void onItemUseFinish(Finish event) {
        if (this.magicMilkTimer.isToggled() && Utils.nullCheck() && Utils.getBedwarsStatus() == 2 && event.entityPlayer == mc.field_71439_g && event.item != null && event.item.func_77973_b() == Items.field_151117_aB) {
            this.magicMilkExpiresAt = System.currentTimeMillis() + 30000L;
        }
    }

    @SubscribeEvent
    public void onWorldJoin(EntityJoinWorldEvent e) {
        if (e.entity == mc.field_71439_g) {
            this.armoredPlayer.clear();
            this.lastHeldMap.clear();
            this.lastAlertMap.clear();
            this.obsidianPos.clear();
            this.entitySpawnQueue.clear();
            this.spawnedMobs.clear();
            this.resetSpawnTracking();
            this.magicMilkExpiresAt = 0L;
            this.resetClosestEnemyHud();
        } else if (e.entity instanceof EntityIronGolem) {
            if (Utils.getBedwarsStatus() != 2) {
                return;
            }
            Vec3 spawnPosition = new Vec3(e.entity.field_70165_t, e.entity.field_70163_u, e.entity.field_70161_v);
            for (SkyWars.SpawnEggInfo eggInfo : this.entitySpawnQueue) {
                if (eggInfo.spawnPos.func_72438_d(spawnPosition) > 3.0 || Utils.timeBetween(mc.field_71439_g.field_70173_aa, eggInfo.tickSpawned) > 60L) {
                    return;
                }
                if (!this.entitySpawnQueue.remove(eggInfo)) {
                    return;
                }
                this.spawnedMobs.add(e.entity.func_145782_y());
            }
        }
    }

    @Override
    public void onUpdate() {
        if (Utils.nullCheck()) {
            if (this.pendingSpawnAnchorCapture && Utils.getBedwarsStatus() == 2) {
                this.spawnAnchor = mc.field_71439_g.func_180425_c();
                this.ownBedCenter = this.findOwnBedCenter();
                this.pendingSpawnAnchorCapture = false;
            }
            if (Utils.getBedwarsStatus() == 2 && (this.bow.isToggled() || this.diamondArmor.isToggled() || this.dreamDefender.isToggled() || this.enderPearl.isToggled() || this.fireball.isToggled() || this.knockbackStick.isToggled() || this.magicMilk.isToggled() || this.obsidian.isToggled() || this.potions.isToggled() || this.waterBucket.isToggled())) {
                for (EntityPlayer p : mc.field_71441_e.field_73010_i) {
                    if (p != null && p != mc.field_71439_g && !AntiBot.isBot(p) && (!this.ignoreTeammate.isToggled() || !Utils.isTeammate(p))) {
                        String name = p.func_70005_c_();
                        ItemStack item = p.func_70694_bm();
                        if (this.diamondArmor.isToggled()) {
                            ItemStack leggings = p.field_71071_by.field_70460_b[1];
                            if (!this.armoredPlayer.contains(name) && leggings != null && leggings.func_77973_b() != null && leggings.func_77973_b() == Items.field_151173_ae) {
                                this.armoredPlayer.add(name);
                                double distance = Math.round(mc.field_71439_g.func_70032_d(p));
                                Utils.sendMessage(p.func_145748_c_().func_150254_d() + " &7has purchased &bDiamond Armor " + Utils.getDistanceColor(distance) + Utils.asWholeNum(distance) + "m§r");
                                this.ping();
                            }
                        }
                        if (item != null && !this.lastHeldMap.containsKey(name)) {
                            String itemType = this.getItemType(item);
                            if (itemType != null) {
                                this.lastHeldMap.put(name, itemType);
                                double distance = Math.round(mc.field_71439_g.func_70032_d(p));
                                this.handleAlert(p, itemType, distance);
                            }
                        } else if (this.lastHeldMap.containsKey(name)) {
                            String itemType = this.lastHeldMap.get(name);
                            if (!itemType.equals(this.getItemType(item))) {
                                this.lastHeldMap.remove(name);
                            }
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        if (Utils.nullCheck()) {
            String strippedMessage = Utils.stripColor(event.message.func_150260_c());
            if (strippedMessage.startsWith(" ") && strippedMessage.contains("Protect your bed and destroy the enemy beds.")) {
                this.ownBedDestroyed = false;
                this.ownBedCenter = null;
                this.pendingSpawnAnchorCapture = true;
                this.waitingForRespawn = false;
                this.magicMilkExpiresAt = 0L;
            } else if (strippedMessage.equals("You will respawn because you still have a bed!")) {
                this.waitingForRespawn = true;
                this.respawnMessageTime = System.currentTimeMillis();
            } else if (strippedMessage.equals("You have respawned!") && this.waitingForRespawn && Utils.timeBetween(System.currentTimeMillis(), this.respawnMessageTime) <= 12000L) {
                this.pendingSpawnAnchorCapture = true;
                this.waitingForRespawn = false;
            }
            if (strippedMessage.trim().startsWith("BED DESTRUCTION > Your Bed")) {
                this.ownBedDestroyed = true;
                this.waitingForRespawn = false;
            }
        }
    }

    public void removeOwnBedPair(List<BlockPos[]> bedPairs) {
        if (this.shouldWhitelistOwnBed() && !bedPairs.isEmpty()) {
            BlockPos[] ownBedPair = null;
            double closestDistance = Double.POSITIVE_INFINITY;
            Vec3 spawnCenter = this.spawnAnchorCenter();
            for (BlockPos[] pair : bedPairs) {
                double distance = spawnCenter.func_72436_e(this.bedCenter(pair));
                if (distance < closestDistance) {
                    closestDistance = distance;
                    ownBedPair = pair;
                }
            }
            if (ownBedPair != null) {
                this.ownBedCenter = this.bedCenter(ownBedPair);
                bedPairs.remove(ownBedPair);
            }
        }
    }

    @SubscribeEvent
    public void onSendPacket(SendPacketEvent e) {
        if (e.getPacket() instanceof C08PacketPlayerBlockPlacement) {
            C08PacketPlayerBlockPlacement p = (C08PacketPlayerBlockPlacement) e.getPacket();
            if (p.func_149568_f() != 255 && p.func_149574_g() != null && p.func_149574_g().func_77973_b() != null && p.func_149574_g().func_77973_b() instanceof ItemMonsterPlacer) {
                Class<? extends Entity> oclass = (Class<? extends Entity>) EntityList.field_75625_b.get(ItemMonsterPlacer.getEntityName(p.func_149574_g()));
                if (oclass == null) {
                    return;
                }
                if (oclass.getSimpleName().equals("EntityIronGolem")) {
                    this.entitySpawnQueue.add(new SkyWars.SpawnEggInfo(p.func_179724_a(), mc.field_71439_g.field_70173_aa));
                }
            }
        }
    }

    @SubscribeEvent
    public void onReceivePacket(ReceivePacketEvent e) {
        if (e.getPacket() instanceof S23PacketBlockChange) {
            S23PacketBlockChange p = (S23PacketBlockChange) e.getPacket();
            if (p.func_180728_a() != null && p.func_180728_a().func_177230_c() instanceof BlockObsidian && this.isNextToBed(p.func_179827_b())) {
                this.obsidianPos.put(p.func_179827_b(), System.currentTimeMillis());
            }
        }
    }

    private boolean isNextToBed(BlockPos blockPos) {
        for (EnumFacing enumFacing : EnumFacing.values()) {
            BlockPos offset = blockPos.func_177972_a(enumFacing);
            if (BlockUtils.getBlock(offset) instanceof BlockBed) {
                return true;
            }
        }
        return false;
    }

    private boolean shouldWhitelistOwnBed() {
        return this.whitelistOwnBed.isToggled() && this.spawnAnchor != null && Utils.getBedwarsStatus() == 2 && mc.field_71439_g.func_174818_b(this.spawnAnchor) <= 800.0;
    }

    private Vec3 spawnAnchorCenter() {
        return new Vec3(this.spawnAnchor.func_177958_n() + 0.5, this.spawnAnchor.func_177956_o() + 0.5, this.spawnAnchor.func_177952_p() + 0.5);
    }

    private Vec3 getOwnBedReference() {
        return this.ownBedCenter != null ? this.ownBedCenter : (this.spawnAnchor == null ? null : this.spawnAnchorCenter());
    }

    private Vec3 findOwnBedCenter() {
        if (this.spawnAnchor == null) {
            return null;
        } else {
            Vec3 spawnCenter = this.spawnAnchorCenter();
            Vec3 closestBedCenter = null;
            double closestDistance = Double.POSITIVE_INFINITY;
            for (int x = -24; x <= 24; x++) {
                for (int y = -6; y <= 6; y++) {
                    for (int z = -24; z <= 24; z++) {
                        BlockPos position = this.spawnAnchor.func_177982_a(x, y, z);
                        if (BlockUtils.getBlock(position) instanceof BlockBed) {
                            Vec3 center = new Vec3(position.func_177958_n() + 0.5, position.func_177956_o() + 0.5, position.func_177952_p() + 0.5);
                            double distance = spawnCenter.func_72436_e(center);
                            if (distance < closestDistance) {
                                closestDistance = distance;
                                closestBedCenter = center;
                            }
                        }
                    }
                }
            }
            return closestBedCenter;
        }
    }

    private List<EntityPlayer> findClosestEnemies(Vec3 bedReference, int players) {
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
        enemies.sort(Comparator.comparingDouble(value2 -> this.distanceToReference(value2, bedReference)));
        int amount = Math.min(Math.max(players, 0), enemies.size());
        return new ArrayList<>(enemies.subList(0, amount));
    }

    private double distanceToReference(EntityPlayer player, Vec3 bedReference) {
        return bedReference == null ? mc.field_71439_g.func_70032_d(player) : player.func_70011_f(bedReference.field_72450_a, bedReference.field_72448_b, bedReference.field_72449_c);
    }

    private Vec3 bedCenter(BlockPos[] pair) {
        return new Vec3((pair[0].func_177958_n() + pair[1].func_177958_n() + 1.0) * 0.5, (pair[0].func_177956_o() + pair[1].func_177956_o() + 1.0) * 0.5, (pair[0].func_177952_p() + pair[1].func_177952_p() + 1.0) * 0.5);
    }

    private void resetSpawnTracking() {
        this.spawnAnchor = null;
        this.ownBedCenter = null;
        this.ownBedDestroyed = false;
        this.pendingSpawnAnchorCapture = false;
        this.waitingForRespawn = false;
        this.respawnMessageTime = 0L;
    }

    private void resetClosestEnemyHud() {
        this.closestEnemySizeInitialized = false;
        this.closestEnemyVisibilityInitialized = false;
        this.closestEnemySelection = null;
        this.lastClosestEnemyEntries = Collections.emptyList();
    }

    private String getItemType(ItemStack item) {
        if (item != null && item.func_77973_b() != null) {
            String unlocalizedName = item.func_77973_b().func_77658_a();
            if (item.func_77973_b() instanceof ItemEnderPearl && this.enderPearl.isToggled()) {
                return "&7an §3Ender Pearl";
            } else if (item.func_77973_b() instanceof ItemBow && this.bow.isToggled()) {
                return "&7a §6Bow";
            } else if (item.func_77973_b() == Items.field_151117_aB && this.magicMilk.isToggled()) {
                return "§fMagic Milk";
            } else if (item.func_77973_b() == Items.field_151055_y && item.func_77948_v() && this.knockbackStick.isToggled()) {
                return "&7a §dKnockback Stick";
            } else if (item.func_77973_b() instanceof ItemMonsterPlacer && !item.func_77948_v() && this.dreamDefender.isToggled()) {
                return "&7a §fDream Defender";
            } else {
                if (item.func_77973_b() instanceof ItemPotion && this.potions.isToggled()) {
                    String potionType = this.getPotionType(item);
                    if (potionType != null) {
                        String article = potionType.equals("Invis") ? "an" : "a";
                        return "&7" + article + " §d" + potionType + " §7Potion";
                    }
                } else {
                    if (item.func_77973_b() == Items.field_151131_as && this.waterBucket.isToggled()) {
                        return "&7a §3Water Bucket";
                    }
                    if (unlocalizedName.contains("tile.obsidian") && this.obsidian.isToggled()) {
                        return "§dObsidian";
                    }
                    if (item.func_77973_b() instanceof ItemFireball && this.fireball.isToggled()) {
                        return "&7a §6Fireball";
                    }
                }
                return null;
            }
        } else {
            return null;
        }
    }

    private String getPotionType(ItemStack item) {
        List<PotionEffect> effects = ((ItemPotion) item.func_77973_b()).func_77832_l(item);
        if (effects == null) {
            return null;
        } else {
            for (PotionEffect effect : effects) {
                if (effect.func_76456_a() == Potion.field_76424_c.field_76415_H) {
                    return "Speed";
                }
                if (effect.func_76456_a() == Potion.field_76441_p.field_76415_H) {
                    return "Invis";
                }
                if (effect.func_76456_a() == Potion.field_76430_j.field_76415_H) {
                    return "Jump";
                }
            }
            return null;
        }
    }

    private void handleAlert(EntityPlayer player, String itemType, double distance) {
        long currentTime = System.currentTimeMillis();
        long interval = Math.round(this.alertInterval.getInput() * 1000.0);
        String alertKey = player.func_110124_au() + ":" + itemType;
        Long lastAlert = this.lastAlertMap.get(alertKey);
        if (lastAlert == null || currentTime - lastAlert >= interval) {
            this.lastAlertMap.put(alertKey, currentTime);
            String alert = player.func_145748_c_().func_150254_d() + " &7is holding " + itemType + " " + Utils.getDistanceColor(distance) + Utils.asWholeNum(distance) + "m§r";
            Utils.sendMessage(alert);
            this.ping();
        }
    }

    public float getClosestEnemyPosX() {
        this.syncHudPositions();
        return this.closestEnemyPosX;
    }

    public float getClosestEnemyPosY() {
        this.syncHudPositions();
        return this.closestEnemyPosY;
    }

    public float getClosestEnemyRelativePosX() {
        this.syncHudPositions();
        return this.closestEnemyRelativePosX;
    }

    public float getClosestEnemyRelativePosY() {
        this.syncHudPositions();
        return this.closestEnemyRelativePosY;
    }

    public void setClosestEnemyRelativePosition(float normalizedX, float normalizedY) {
        this.closestEnemyRelativePosX = normalizedX;
        this.closestEnemyRelativePosY = normalizedY;
        this.syncHudPositions();
    }

    public void setClosestEnemyAbsolutePosition(float absoluteX, float absoluteY) {
        this.setClosestEnemyAbsolutePosition(absoluteX, absoluteY, new ScaledResolution(mc));
    }

    public float getMagicMilkPosX() {
        this.syncHudPositions();
        return this.magicMilkPosX;
    }

    public float getMagicMilkPosY() {
        this.syncHudPositions();
        return this.magicMilkPosY;
    }

    public float getMagicMilkRelativePosX() {
        this.syncHudPositions();
        return this.magicMilkRelativePosX;
    }

    public float getMagicMilkRelativePosY() {
        this.syncHudPositions();
        return this.magicMilkRelativePosY;
    }

    public void setMagicMilkRelativePosition(float normalizedX, float normalizedY) {
        this.magicMilkRelativePosX = normalizedX;
        this.magicMilkRelativePosY = normalizedY;
        this.syncHudPositions();
    }

    public void setMagicMilkAbsolutePosition(float absoluteX, float absoluteY) {
        this.setMagicMilkAbsolutePosition(absoluteX, absoluteY, new ScaledResolution(mc));
    }

    private void syncHudPositions() {
        this.syncHudPositions(new ScaledResolution(mc));
    }

    private void syncHudPositions(ScaledResolution resolution) {
        int scaledWidth = Math.max(1, resolution.func_78326_a());
        int scaledHeight = Math.max(1, resolution.func_78328_b());
        float defaultClosestEnemyY = scaledHeight / 4.0F - mc.field_71466_p.field_78288_b / 2.0F;
        if (Float.isNaN(this.closestEnemyRelativePosX) || Float.isNaN(this.closestEnemyRelativePosY)) {
            if (Float.isNaN(this.closestEnemyPosX) || Float.isNaN(this.closestEnemyPosY)) {
                this.closestEnemyPosX = 0.0F;
                this.closestEnemyPosY = defaultClosestEnemyY;
            }
            this.closestEnemyRelativePosX = this.closestEnemyPosX / scaledWidth;
            this.closestEnemyRelativePosY = this.closestEnemyPosY / scaledHeight;
        }
        this.closestEnemyPosX = this.closestEnemyRelativePosX * scaledWidth;
        this.closestEnemyPosY = this.closestEnemyRelativePosY * scaledHeight;
        if (Float.isNaN(this.magicMilkRelativePosX) || Float.isNaN(this.magicMilkRelativePosY)) {
            if (Float.isNaN(this.magicMilkPosX) || Float.isNaN(this.magicMilkPosY)) {
                this.magicMilkPosX = 0.0F;
                this.magicMilkPosY = defaultClosestEnemyY + mc.field_71466_p.field_78288_b + 9.0F;
            }
            this.magicMilkRelativePosX = this.magicMilkPosX / scaledWidth;
            this.magicMilkRelativePosY = this.magicMilkPosY / scaledHeight;
        }
        this.magicMilkPosX = this.magicMilkRelativePosX * scaledWidth;
        this.magicMilkPosY = this.magicMilkRelativePosY * scaledHeight;
    }

    private void setClosestEnemyAbsolutePosition(float absoluteX, float absoluteY, ScaledResolution resolution) {
        this.closestEnemyPosX = absoluteX;
        this.closestEnemyPosY = absoluteY;
        this.closestEnemyRelativePosX = absoluteX / Math.max(1, resolution.func_78326_a());
        this.closestEnemyRelativePosY = absoluteY / Math.max(1, resolution.func_78328_b());
    }

    private void setMagicMilkAbsolutePosition(float absoluteX, float absoluteY, ScaledResolution resolution) {
        this.magicMilkPosX = absoluteX;
        this.magicMilkPosY = absoluteY;
        this.magicMilkRelativePosX = absoluteX / Math.max(1, resolution.func_78326_a());
        this.magicMilkRelativePosY = absoluteY / Math.max(1, resolution.func_78328_b());
    }

    private void resetHudPositions(ScaledResolution resolution) {
        float closestEnemyY = resolution.func_78328_b() / 4.0F - mc.field_71466_p.field_78288_b / 2.0F;
        this.setClosestEnemyAbsolutePosition(0.0F, closestEnemyY, resolution);
        this.setMagicMilkAbsolutePosition(0.0F, closestEnemyY + mc.field_71466_p.field_78288_b + 9.0F, resolution);
    }

    private void ping() {
        if (this.shouldPing.isToggled()) {
            mc.field_71439_g.func_85030_a("note.pling", 1.0F, 1.0F);
        }
    }

    private BedWars.HudBoxBounds drawEnemyHudBox(List<BedWars.EnemyHudEntry> entries, float x, float textY) {
        float verticalPadding = 4.0F;
        float left = x - 10.0F;
        float top = textY - verticalPadding;
        float contentProgress = this.updateClosestEnemySelection(entries);
        float visibility = this.updateClosestEnemyVisibility(entries);
        boolean fadingOut = entries.isEmpty();
        List<BedWars.EnemyHudEntry> renderedEntries = fadingOut ? this.lastClosestEnemyEntries : entries;
        if (!renderedEntries.isEmpty() && !(visibility <= 0.0F)) {
            float horizontalPadding = 3.0F;
            float columnSpacing = 6.0F;
            float itemSpacing = 3.0F;
            float rowHeight = mc.field_71466_p.field_78288_b + 4.0F;
            float textX = x + horizontalPadding;
            float maxNameWidth = 0.0F;
            float maxDistanceWidth = 0.0F;
            boolean hasItem = false;
            for (BedWars.EnemyHudEntry entry : renderedEntries) {
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
                    BedWars.EnemyHudEntry enemyHudEntry = renderedEntries.get(i);
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
            return new BedWars.HudBoxBounds(left, top, right, bottom);
        } else {
            return null;
        }
    }

    private void enableClosestEnemyAlphaBlending() {
        GlStateManager.func_179147_l();
        GlStateManager.func_179120_a(770, 771, 1, 0);
    }

    private float updateClosestEnemyVisibility(List<BedWars.EnemyHudEntry> entries) {
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

    private float updateClosestEnemySelection(List<BedWars.EnemyHudEntry> entries) {
        List<Integer> selection = new ArrayList<>(entries.size());
        for (BedWars.EnemyHudEntry entry : entries) {
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

    private class EditPositionsScreen extends GuiScreen {

        private static final int DRAG_NONE = 0;

        private static final int DRAG_CLOSEST_ENEMY = 1;

        private static final int DRAG_MAGIC_MILK = 2;

        private GuiButtonExt resetPositions;

        private BedWars.HudBoxBounds closestEnemyBounds;

        private BedWars.HudBoxBounds magicMilkBounds;

        private int dragging = 0;

        private float closestEnemyX;

        private float closestEnemyY;

        private float magicMilkX;

        private float magicMilkY;

        private float dragStartX;

        private float dragStartY;

        private int lastMouseX;

        private int lastMouseY;

        private EditPositionsScreen() {
        }

        public void func_73866_w_() {
            super.func_73866_w_();
            this.field_146292_n.add(this.resetPositions = new GuiButtonExt(1, this.field_146294_l - 95, this.field_146295_m - 25, 90, 20, "Reset positions"));
            BedWars.this.syncHudPositions(new ScaledResolution(this.field_146297_k));
            this.syncEditorPositions();
        }

        public void func_73863_a(int mouseX, int mouseY, float partialTicks) {
            ScaledResolution resolution = new ScaledResolution(this.field_146297_k);
            if (this.dragging == 0) {
                BedWars.this.syncHudPositions(resolution);
                this.syncEditorPositions();
            }
            func_73734_a(0, 0, this.field_146294_l, this.field_146295_m, -1308622848);
            BedWars.this.setClosestEnemyAbsolutePosition(this.closestEnemyX, this.closestEnemyY, resolution);
            BedWars.this.setMagicMilkAbsolutePosition(this.magicMilkX, this.magicMilkY, resolution);
            List<BedWars.EnemyHudEntry> previewEntries = new ArrayList<>();
            int previewPlayers = Math.max(1, (int) BedWars.this.closestEnemy.getInput());
            for (int i = 0; i < previewPlayers; i++) {
                String name = this.field_146297_k.field_71439_g.func_145748_c_().func_150254_d() + "§r";
                String distance = "§a" + (16 + i * 3) + "m§r";
                previewEntries.add(new BedWars.EnemyHudEntry(this.field_146297_k.field_71439_g.func_145782_y(), name, distance, this.field_146297_k.field_71439_g.func_70694_bm()));
            }
            this.closestEnemyBounds = BedWars.this.drawEnemyHudBox(previewEntries, this.closestEnemyX, this.closestEnemyY);
            this.magicMilkBounds = BedWars.this.drawHudBox("Magic Milk: §b30.0s", this.magicMilkX, this.magicMilkY);
            String message = "Drag either box to reposition it.";
            int messageX = resolution.func_78326_a() / 2 - this.field_146289_q.func_78256_a(message) / 2;
            int messageY = resolution.func_78328_b() / 2 - 10;
            RenderUtils.drawColoredString(message, '-', messageX, messageY, 2L, 0L, true, this.field_146289_q);
            try {
                this.func_146269_k();
            } catch (IOException exception2) {
            }
            super.func_73863_a(mouseX, mouseY, partialTicks);
        }

        protected void func_146273_a(int mouseX, int mouseY, int button, long timeSinceLastClick) {
            super.func_146273_a(mouseX, mouseY, button, timeSinceLastClick);
            if (button == 0) {
                if (this.dragging == 1) {
                    this.closestEnemyX = this.dragStartX + mouseX - this.lastMouseX;
                    this.closestEnemyY = this.dragStartY + mouseY - this.lastMouseY;
                } else if (this.dragging == 2) {
                    this.magicMilkX = this.dragStartX + mouseX - this.lastMouseX;
                    this.magicMilkY = this.dragStartY + mouseY - this.lastMouseY;
                } else if (this.closestEnemyBounds != null && this.closestEnemyBounds.contains(mouseX, mouseY)) {
                    this.beginDragging(1, this.closestEnemyX, this.closestEnemyY, mouseX, mouseY);
                } else if (this.magicMilkBounds != null && this.magicMilkBounds.contains(mouseX, mouseY)) {
                    this.beginDragging(2, this.magicMilkX, this.magicMilkY, mouseX, mouseY);
                }
            }
        }

        private void beginDragging(int element, float elementX, float elementY, int mouseX, int mouseY) {
            this.dragging = element;
            this.dragStartX = elementX;
            this.dragStartY = elementY;
            this.lastMouseX = mouseX;
            this.lastMouseY = mouseY;
        }

        protected void func_146286_b(int mouseX, int mouseY, int state) {
            super.func_146286_b(mouseX, mouseY, state);
            if (state == 0) {
                this.dragging = 0;
            }
        }

        public void func_146284_a(GuiButton button) {
            if (button == this.resetPositions) {
                this.dragging = 0;
                BedWars.this.resetHudPositions(new ScaledResolution(this.field_146297_k));
                this.syncEditorPositions();
            }
        }

        private void syncEditorPositions() {
            this.closestEnemyX = BedWars.this.closestEnemyPosX;
            this.closestEnemyY = BedWars.this.closestEnemyPosY;
            this.magicMilkX = BedWars.this.magicMilkPosX;
            this.magicMilkY = BedWars.this.magicMilkPosY;
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
}
