package keystrokesmod.module.impl.render;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Map;
import java.util.Set;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.ColorSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.RenderUtils;
import keystrokesmod.utility.SharedBlockHighlightCache;
import keystrokesmod.utility.Theme;
import keystrokesmod.utility.Timer;
import keystrokesmod.utility.Utils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBed.EnumPartType;
import net.minecraft.block.BlockBed;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos.MutableBlockPos;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing.Axis;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

public class BedESP extends Module {

    private static final String[] COLOR_MODES = new String[] { "Static", "Gradient", "Rainbow", "Theme" };

    private static final int THEME_COLOR_MODE = 3;

    private static final int DEFAULT_THEME_INDEX = 10;

    private static final int THEME_ALPHA = 64;

    private static final int BED_BREAK_ANIMATION_DURATION = 300;

    private static final int BED_BREAK_ANIMATION_ALPHA = 230;

    private static final float DEFENSE_AUTO_SCALE_THRESHOLD = 8.0F;

    private SliderSetting colorMode;

    private SliderSetting theme;

    private ColorSetting color;

    private ColorSetting color2;

    private SliderSetting gradientSpeed;

    private SliderSetting range;

    private SliderSetting scanSpeed;

    private ButtonSetting disableInLobby;

    private ButtonSetting firstBed;

    private ButtonSetting renderFullBlock;

    private ButtonSetting showExposedOutline;

    private ButtonSetting disableFullyExposed;

    private SliderSetting renderExposedBlocks;

    private ColorSetting exposedColor;

    private ColorSetting exposedOutlineColor;

    private ButtonSetting showDefenseLayers;

    private ButtonSetting showDefenseTools;

    private ButtonSetting showDefenseCounts;

    private SliderSetting defenseHeight;

    private SliderSetting defenseScale;

    private ButtonSetting defenseAutoScale;

    private boolean lastDefenseToolMode;

    private boolean pausedForLobby;

    private final List<BlockPos[]> lastRenderedBedPairs = new ArrayList<>();

    private static final int DEFENSE_ICON_SIZE = 16;

    private static final int DEFENSE_ICON_SPACING = 18;

    private static final int DEFENSE_PADDING = 3;

    private static final float DEFENSE_BACKGROUND_RADIUS = 8.0F;

    private static final int DEFENSE_BACKGROUND_COLOR = 1929379840;

    private static final int DEFENSE_BACKGROUND_OUTLINE_COLOR = -1778384896;

    private static final int DEFENSE_MAX_LAYERS = 5;

    private static final float DEFENSE_AIR_RATIO_THRESHOLD = 0.2F;

    private static final float DEFENSE_BLOCK_RATIO_THRESHOLD = 0.2F;

    private static final EnumMap<EnumFacing, BedESP.LayerOffsets[]> DEFENSE_OFFSETS = buildLayerOffsetsByFacing();

    private final List<BlockPos[]> activeBedPairs = new ArrayList<>();

    private final Map<Long, BedESP.BedBreakAnimation> bedBreakAnimations = new HashMap<>();

    private final Map<Long, BedESP.DefenseOverlaySnapshot> defenseSnapshots = new HashMap<>();

    private final Map<Long, BedESP.DefenseWatchRegion> defenseWatchRegions = new HashMap<>();

    private final Map<Long, Set<Long>> watchedBedsByDefensePos = new HashMap<>();

    private final Map<Long, Set<Long>> watchedBedsByChunk = new HashMap<>();

    private final Set<Long> dirtyDefenseBeds = new HashSet<>();

    private final SharedBlockHighlightCache.UpdateListener defenseUpdateListener = new SharedBlockHighlightCache.UpdateListener() {

        @Override
        public void onBlockChanged(BlockPos pos, IBlockState newState) {
            BedESP.this.markBedsDirtyForDefensePos(pos);
        }

        @Override
        public void onChunkQueued(int chunkX, int chunkZ) {
            BedESP.this.markBedsDirtyForChunk(chunkX, chunkZ);
        }

        @Override
        public void onChunkRemoved(int chunkX, int chunkZ) {
            BedESP.this.markBedsDirtyForChunk(chunkX, chunkZ);
        }

        @Override
        public void onCacheCleared() {
            BedESP.this.clearDefenseState();
        }
    };

    public BedESP() {
        super("BedESP", Module.category.render);
        this.liteModule = true;
        this.registerSetting(this.colorMode = new SliderSetting("Color mode", 0, COLOR_MODES));
        this.registerSetting(this.theme = new SliderSetting("Theme", 10, Theme.THEMES_SETTING));
        this.registerSetting(this.color = new ColorSetting("Color", 255, 85, 85, 64));
        this.registerSetting(this.color2 = new ColorSetting("Color 2", 85, 85, 255, 64));
        this.registerSetting(this.gradientSpeed = new SliderSetting("Gradient speed", 1.0, 0.1, 8.0, 0.1));
        this.registerSetting(this.range = new SliderSetting("Range", 10.0, 4.0, 200.0, 2.0));
        this.registerSetting(this.scanSpeed = new SliderSetting("Scan speed", 8.0, 1.0, 32.0, 1.0));
        this.registerSetting(this.disableInLobby = new ButtonSetting("Disable in lobby", true));
        this.registerSetting(this.firstBed = new ButtonSetting("Only render first bed", false));
        this.registerSetting(this.renderFullBlock = new ButtonSetting("Render full block", false));
        this.registerSetting(this.renderExposedBlocks = new SliderSetting("Render exposed", " block", true, -1.0, 1.0, 8.0, 1.0));
        this.registerSetting(this.exposedColor = new ColorSetting("Exposed color", 255, 0, 0, 64));
        this.registerSetting(this.showExposedOutline = new ButtonSetting("Outline exposed", true));
        this.registerSetting(this.disableFullyExposed = new ButtonSetting("Disable fully exposed", false));
        this.registerSetting(this.exposedOutlineColor = new ColorSetting("Exposed outline color", 255, 220, 64, 255));
        this.registerSetting(this.showDefenseLayers = new ButtonSetting("Show defense layers", false));
        this.registerSetting(this.showDefenseTools = new ButtonSetting("Show break tools", false));
        this.registerSetting(this.showDefenseCounts = new ButtonSetting("Show defense counts", true));
        this.registerSetting(this.defenseHeight = new SliderSetting("Defense height", 0.6, 0.1, 3.0, 0.1));
        this.registerSetting(this.defenseScale = new SliderSetting("Defense scale", 1.0, 0.1, 2.0, 0.05));
        this.registerSetting(this.defenseAutoScale = new ButtonSetting("Auto Scale", false));
    }

    @Override
    public void guiUpdate() {
        int mode = this.colorMode == null ? 0 : (int) this.colorMode.getInput();
        boolean staticMode = mode == 0;
        boolean gradientMode = mode == 1;
        boolean rainbowMode = mode == 2;
        boolean themeMode = mode == 3;
        boolean renderExposedEnabled = (int) this.renderExposedBlocks.getInput() != -1;
        this.theme.setVisible(themeMode, this);
        this.color.setVisible(staticMode || gradientMode || rainbowMode, this);
        this.color2.setVisible(gradientMode, this);
        this.gradientSpeed.setVisible(gradientMode, this);
        this.exposedColor.setVisible(renderExposedEnabled, this);
        boolean exposedOutlineAvailable = this.showExposedOutline.isToggled();
        this.disableFullyExposed.setVisible(exposedOutlineAvailable, this);
        this.exposedOutlineColor.setVisible(exposedOutlineAvailable, this);
        boolean defenseVisible = this.showDefenseLayers.isToggled();
        this.showDefenseTools.setVisible(defenseVisible, this);
        this.showDefenseCounts.setVisible(defenseVisible && !this.showDefenseTools.isToggled(), this);
        this.defenseHeight.setVisible(defenseVisible, this);
        this.defenseScale.setVisible(defenseVisible, this);
        this.defenseAutoScale.setVisible(defenseVisible, this);
    }

    @Override
    public void onEnable() {
        SharedBlockHighlightCache cache = SharedBlockHighlightCache.get();
        cache.addUpdateListener(this.defenseUpdateListener);
        this.pausedForLobby = this.shouldPauseForLobby();
        if (this.pausedForLobby) {
            cache.detachBed();
            this.clearRenderedState();
        } else {
            cache.attachBed();
            cache.enqueueLoadedChunks();
        }
    }

    @Override
    public void onDisable() {
        SharedBlockHighlightCache cache = SharedBlockHighlightCache.get();
        cache.removeUpdateListener(this.defenseUpdateListener);
        cache.detachBed();
        this.pausedForLobby = false;
        this.clearRenderedState();
    }

    @SubscribeEvent
    public void onEntityJoin(EntityJoinWorldEvent e) {
        if (e.entity == mc.field_71439_g) {
            this.clearRenderedState();
        }
    }

    public int getScanSpeedBudget() {
        return this.isEnabled() && !this.pausedForLobby ? (int) this.scanSpeed.getInput() : 0;
    }

    @Override
    public void onUpdate() {
        if (!Utils.nullCheck()) {
            this.activeBedPairs.clear();
            this.clearDefenseState();
        } else {
            boolean pauseForLobby = this.shouldPauseForLobby();
            if (pauseForLobby != this.pausedForLobby) {
                this.setPausedForLobby(pauseForLobby);
            }
            if (pauseForLobby) {
                this.clearRenderedState();
            } else {
                SharedBlockHighlightCache cache = SharedBlockHighlightCache.get();
                double rangeSq = this.range.getInput() * this.range.getInput();
                double px = mc.field_71439_g.field_70165_t;
                double py = mc.field_71439_g.field_70163_u;
                double pz = mc.field_71439_g.field_70161_v;
                List<BlockPos[]> candidatePairs = this.collectActiveBedPairs(cache, px, py, pz, rangeSq);
                this.activeBedPairs.clear();
                for (BlockPos[] pair : candidatePairs) {
                    this.activeBedPairs.add(copyBedPair(pair));
                }
                boolean defenseToolMode = this.showDefenseTools.isToggled();
                if (this.lastDefenseToolMode != defenseToolMode) {
                    this.clearDefenseState();
                    this.lastDefenseToolMode = defenseToolMode;
                }
                if (!this.showDefenseLayers.isToggled()) {
                    this.clearDefenseState();
                } else {
                    Set<Long> activeFeet = new HashSet<>();
                    for (BlockPos[] pair : candidatePairs) {
                        BlockPos foot = pair[0];
                        BlockPos head = pair[1];
                        long footKey = foot.func_177986_g();
                        activeFeet.add(footKey);
                        BedESP.DefenseWatchRegion region = this.defenseWatchRegions.get(footKey);
                        if (region == null || !region.matches(head)) {
                            this.unregisterDefenseWatch(footKey);
                            this.registerDefenseWatch(foot, head);
                            this.dirtyDefenseBeds.add(footKey);
                        }
                        BedESP.DefenseOverlaySnapshot snapshot = this.defenseSnapshots.get(footKey);
                        if (snapshot == null || !snapshot.matches(head) || this.dirtyDefenseBeds.remove(footKey)) {
                            this.defenseSnapshots.put(footKey, this.computeDefenseSnapshot(foot, head));
                        }
                    }
                    for (Long regionKey : new ArrayList<>(this.defenseWatchRegions.keySet())) {
                        if (!activeFeet.contains(regionKey)) {
                            this.unregisterDefenseWatch(regionKey);
                        }
                    }
                    this.dirtyDefenseBeds.retainAll(activeFeet);
                }
            }
        }
    }

    @Override
    public String getInfo() {
        if (!this.pausedForLobby && !this.shouldPauseForLobby()) {
            int n = SharedBlockHighlightCache.get().totalBedFeet();
            return n > 0 ? String.valueOf(n) : "";
        } else {
            return "";
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onRenderWorld(RenderWorldLastEvent e) {
        if (Utils.nullCheck()) {
            if (this.isEnabled() && !this.shouldPauseForLobby()) {
                float blockHeight = this.getBlockHeight();
                double rangeSq = this.range.getInput() * this.range.getInput();
                double px = mc.field_71439_g.field_70165_t;
                double py = mc.field_71439_g.field_70163_u;
                double pz = mc.field_71439_g.field_70161_v;
                List<BlockPos[]> pairsToRender = new ArrayList<>();
                Set<BlockPos> addedFeet = new HashSet<>();
                Set<Long> activeFeet = new HashSet<>();
                Map<Long, Float> animatedAlphas = new HashMap<>();
                for (BlockPos[] pair : this.activeBedPairs) {
                    BlockPos foot = pair[0];
                    long footKey = foot.func_177986_g();
                    activeFeet.add(footKey);
                    if (!this.hasBedAt(foot)) {
                        this.startBedBreakAnimation(pair);
                    } else {
                        this.bedBreakAnimations.remove(footKey);
                        AxisAlignedBB bb = bedWorldBounds(pair[0], pair[1], blockHeight);
                        if (RenderUtils.isInViewFrustum(bb) && addedFeet.add(foot)) {
                            pairsToRender.add(copyBedPair(pair));
                        }
                    }
                }
                for (BlockPos[] prev : new ArrayList<>(this.lastRenderedBedPairs)) {
                    if (prev != null && prev.length >= 2) {
                        BlockPos foot = prev[0];
                        BlockPos head = prev[1];
                        long footKey = foot.func_177986_g();
                        if (!activeFeet.contains(footKey)) {
                            if (!this.hasBedAt(foot)) {
                                this.startBedBreakAnimation(prev);
                            } else {
                                this.bedBreakAnimations.remove(footKey);
                                double dx = foot.func_177958_n() + 0.5 - px;
                                double dy = foot.func_177956_o() + 0.5 - py;
                                double dz = foot.func_177952_p() + 0.5 - pz;
                                if (!(dx * dx + dy * dy + dz * dz > rangeSq)) {
                                    AxisAlignedBB bb = bedWorldBounds(foot, head, blockHeight);
                                    if (RenderUtils.isInViewFrustum(bb)) {
                                        pairsToRender.add(copyBedPair(prev));
                                        addedFeet.add(foot);
                                    }
                                }
                            }
                        }
                    }
                }
                Iterator<Entry<Long, BedESP.BedBreakAnimation>> animationIterator = this.bedBreakAnimations.entrySet().iterator();
                while (animationIterator.hasNext()) {
                    Entry<Long, BedESP.BedBreakAnimation> entry = animationIterator.next();
                    BedESP.BedBreakAnimation animation = entry.getValue();
                    BlockPos[] pair2 = animation.pair;
                    BlockPos foot = pair2[0];
                    if (this.hasBedAt(foot)) {
                        animationIterator.remove();
                    } else {
                        int alpha = 230 - animation.timer.getValueInt(0, 230, 1);
                        if (alpha <= 0) {
                            animationIterator.remove();
                        } else {
                            double dx = foot.func_177958_n() + 0.5 - px;
                            double dy = foot.func_177956_o() + 0.5 - py;
                            double dz = foot.func_177952_p() + 0.5 - pz;
                            if (!(dx * dx + dy * dy + dz * dz > rangeSq)) {
                                AxisAlignedBB bb = bedWorldBounds(pair2[0], pair2[1], blockHeight);
                                if (RenderUtils.isInViewFrustum(bb) && addedFeet.add(foot)) {
                                    pairsToRender.add(copyBedPair(pair2));
                                    animatedAlphas.put(entry.getKey(), alpha / 255.0F);
                                }
                            }
                        }
                    }
                }
                for (BlockPos[] blockPosArray : pairsToRender) {
                    Float animatedAlpha = animatedAlphas.get(blockPosArray[0].func_177986_g());
                    this.renderBed(blockPosArray, blockHeight, animatedAlpha);
                    if (this.showDefenseLayers.isToggled() && this.isLiveBedPair(blockPosArray)) {
                        this.renderDefenseOverlay(blockPosArray, blockHeight);
                    }
                }
                this.lastRenderedBedPairs.clear();
                for (BlockPos[] blockPosArray2 : pairsToRender) {
                    this.lastRenderedBedPairs.add(copyBedPair(blockPosArray2));
                }
            }
        }
    }

    private boolean shouldPauseForLobby() {
        return this.disableInLobby != null && this.disableInLobby.isToggled() && Utils.nullCheck() && Utils.isLobby();
    }

    private void setPausedForLobby(boolean paused) {
        this.pausedForLobby = paused;
        SharedBlockHighlightCache cache = SharedBlockHighlightCache.get();
        if (paused) {
            cache.detachBed();
            this.clearRenderedState();
        } else {
            cache.attachBed();
            cache.enqueueLoadedChunks();
        }
    }

    private void clearRenderedState() {
        this.activeBedPairs.clear();
        this.lastRenderedBedPairs.clear();
        this.bedBreakAnimations.clear();
        this.lastDefenseToolMode = false;
        this.clearDefenseState();
    }

    private void startBedBreakAnimation(BlockPos[] pair) {
        long footKey = pair[0].func_177986_g();
        if (!this.bedBreakAnimations.containsKey(footKey)) {
            Timer timer = new Timer(300.0F);
            timer.start();
            this.bedBreakAnimations.put(footKey, new BedESP.BedBreakAnimation(copyBedPair(pair), timer));
        }
    }

    private static BlockPos[] copyBedPair(BlockPos[] pair) {
        return new BlockPos[] { new BlockPos(pair[0]), new BlockPos(pair[1]) };
    }

    private List<BlockPos[]> collectActiveBedPairs(SharedBlockHighlightCache cache, double px, double py, double pz, double rangeSq) {
        List<BlockPos[]> candidatePairs = new ArrayList<>();
        for (Entry<Long, Set<BlockPos>> chunk : cache.entriesBedFeet()) {
            for (BlockPos foot : chunk.getValue()) {
                double dx = foot.func_177958_n() + 0.5 - px;
                double dy = foot.func_177956_o() + 0.5 - py;
                double dz = foot.func_177952_p() + 0.5 - pz;
                if (!(dx * dx + dy * dy + dz * dz > rangeSq)) {
                    BlockPos[] pair = this.footAndHead(foot);
                    if (pair != null && this.stillHasRenderableBed(pair)) {
                        candidatePairs.add(copyBedPair(pair));
                    }
                }
            }
        }
        if (this.firstBed.isToggled() && candidatePairs.size() > 1) {
            BlockPos[] nearest = null;
            double nearestDistanceSq = Double.MAX_VALUE;
            for (BlockPos[] pair : candidatePairs) {
                double dx = pair[0].func_177958_n() + 0.5 - px;
                double dy = pair[0].func_177956_o() + 0.5 - py;
                double dz = pair[0].func_177952_p() + 0.5 - pz;
                double distanceSq = dx * dx + dy * dy + dz * dz;
                if (distanceSq < nearestDistanceSq) {
                    nearestDistanceSq = distanceSq;
                    nearest = pair;
                }
            }
            candidatePairs.clear();
            if (nearest != null) {
                candidatePairs.add(nearest);
            }
            return candidatePairs;
        } else {
            return candidatePairs;
        }
    }

    private static boolean isBedFoot(IBlockState st) {
        return st != null && st.func_177230_c() instanceof BlockBed && st.func_177229_b(BlockBed.field_176472_a) == EnumPartType.FOOT;
    }

    private boolean stillHasRenderableBed(BlockPos[] pair) {
        if (pair != null && pair.length >= 2 && mc.field_71441_e != null) {
            IBlockState a = mc.field_71441_e.func_180495_p(pair[0]);
            IBlockState b = mc.field_71441_e.func_180495_p(pair[1]);
            return a != null && a.func_177230_c() instanceof BlockBed || b != null && b.func_177230_c() instanceof BlockBed;
        } else {
            return false;
        }
    }

    private boolean hasBedAt(BlockPos pos) {
        return mc.field_71441_e != null && mc.field_71441_e.func_180495_p(pos).func_177230_c() instanceof BlockBed;
    }

    private boolean isLiveBedPair(BlockPos[] pair) {
        if (pair != null && pair.length >= 2 && mc.field_71441_e != null) {
            IBlockState footState = mc.field_71441_e.func_180495_p(pair[0]);
            IBlockState headState = mc.field_71441_e.func_180495_p(pair[1]);
            return isBedFoot(footState) && headState != null && headState.func_177230_c() instanceof BlockBed;
        } else {
            return false;
        }
    }

    private static AxisAlignedBB bedWorldBounds(BlockPos foot, BlockPos head, float height) {
        int fx = foot.func_177958_n();
        int fy = foot.func_177956_o();
        int fz = foot.func_177952_p();
        double h = fy + height;
        if (foot.func_177958_n() != head.func_177958_n()) {
            return foot.func_177958_n() > head.func_177958_n() ? new AxisAlignedBB(fx - 1.0, fy, fz, fx + 1.0, h, fz + 1.0) : new AxisAlignedBB(fx, fy, fz, fx + 2.0, h, fz + 1.0);
        } else {
            return foot.func_177952_p() > head.func_177952_p() ? new AxisAlignedBB(fx, fy, fz - 1.0, fx + 1.0, h, fz + 1.0) : new AxisAlignedBB(fx, fy, fz, fx + 1.0, h, fz + 2.0);
        }
    }

    private BlockPos[] footAndHead(BlockPos foot) {
        IBlockState st = mc.field_71441_e.func_180495_p(foot);
        if (!(st.func_177230_c() instanceof BlockBed)) {
            return null;
        } else {
            EnumFacing facing = (EnumFacing) st.func_177229_b(BlockBed.field_176387_N);
            return new BlockPos[] { foot, foot.func_177972_a(facing) };
        }
    }

    private void renderBed(BlockPos[] blocks, float height, Float animatedAlpha) {
        List<BlockPos> exposedBlocks = this.getExposedBlocks(blocks);
        List<AxisAlignedBB> mergedExposedBounds = this.mergeExposedBounds(exposedBlocks);
        int exposedCount = exposedBlocks.size();
        int exposedLimit = (int) this.renderExposedBlocks.getInput();
        boolean exposedRenderingEnabled = exposedLimit != -1;
        boolean renderExposedOverlays = exposedRenderingEnabled && exposedCount > 0 && exposedCount <= exposedLimit;
        boolean fullyExposed = this.isFullyExposed(blocks);
        boolean renderExposedOutline = this.showExposedOutline.isToggled() && exposedCount > 0 && (!this.disableFullyExposed.isToggled() || !fullyExposed) && (!exposedRenderingEnabled || exposedCount > exposedLimit);
        double x = blocks[0].func_177958_n() - mc.func_175598_ae().field_78730_l;
        double y = blocks[0].func_177956_o() - mc.func_175598_ae().field_78731_m;
        double z = blocks[0].func_177952_p() - mc.func_175598_ae().field_78728_n;
        GL11.glBlendFunc(770, 771);
        GL11.glEnable(3042);
        GL11.glLineWidth(2.0F);
        GL11.glDisable(3553);
        GL11.glDisable(2929);
        GL11.glDepthMask(false);
        int col = this.getCurrentColor();
        float drawA = animatedAlpha == null ? (col >> 24 & 0xFF) / 255.0F : animatedAlpha;
        float r = (col >> 16 & 0xFF) / 255.0F;
        float g = (col >> 8 & 0xFF) / 255.0F;
        float b = (col & 0xFF) / 255.0F;
        GL11.glColor4d(r, g, b, drawA);
        AxisAlignedBB axisAlignedBB;
        if (blocks[0].func_177958_n() != blocks[1].func_177958_n()) {
            if (blocks[0].func_177958_n() > blocks[1].func_177958_n()) {
                axisAlignedBB = new AxisAlignedBB(x - 1.0, y, z, x + 1.0, y + height, z + 1.0);
            } else {
                axisAlignedBB = new AxisAlignedBB(x, y, z, x + 2.0, y + height, z + 1.0);
            }
        } else if (blocks[0].func_177952_p() > blocks[1].func_177952_p()) {
            axisAlignedBB = new AxisAlignedBB(x, y, z - 1.0, x + 1.0, y + height, z + 1.0);
        } else {
            axisAlignedBB = new AxisAlignedBB(x, y, z, x + 1.0, y + height, z + 2.0);
        }
        RenderUtils.drawBoundingBox(axisAlignedBB, r, g, b, drawA);
        if (renderExposedOverlays) {
            this.renderExposedBlockOverlays(mergedExposedBounds);
        }
        if (renderExposedOutline) {
            int outlineColor = this.exposedOutlineColor.getColor();
            float outlineA = (outlineColor >> 24 & 0xFF) / 255.0F;
            float outlineR = (outlineColor >> 16 & 0xFF) / 255.0F;
            float outlineG = (outlineColor >> 8 & 0xFF) / 255.0F;
            float outlineB = (outlineColor & 0xFF) / 255.0F;
            GL11.glLineWidth(3.0F);
            GL11.glColor4f(outlineR, outlineG, outlineB, outlineA);
            RenderGlobal.func_181561_a(axisAlignedBB);
            GL11.glLineWidth(2.0F);
        }
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glEnable(3553);
        GL11.glEnable(2929);
        GL11.glDepthMask(true);
        GL11.glDisable(3042);
    }

    private List<AxisAlignedBB> mergeExposedBounds(List<BlockPos> exposedBlocks) {
        List<AxisAlignedBB> bounds = new ArrayList<>();
        for (BlockPos pos : exposedBlocks) {
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

    private void renderExposedBlockOverlays(List<AxisAlignedBB> exposedBounds) {
        if (!exposedBounds.isEmpty()) {
            int overlayColor = this.exposedColor.getColor();
            float overlayA = (overlayColor >> 24 & 0xFF) / 255.0F;
            float overlayR = (overlayColor >> 16 & 0xFF) / 255.0F;
            float overlayG = (overlayColor >> 8 & 0xFF) / 255.0F;
            float overlayB = (overlayColor & 0xFF) / 255.0F;
            RenderManager renderManager = mc.func_175598_ae();
            GL11.glColor4f(overlayR, overlayG, overlayB, overlayA);
            for (AxisAlignedBB bounds : exposedBounds) {
                AxisAlignedBB renderedBounds = bounds.func_72317_d(-renderManager.field_78730_l, -renderManager.field_78731_m, -renderManager.field_78728_n);
                RenderUtils.drawBoundingBox(renderedBounds, overlayR, overlayG, overlayB, overlayA);
            }
        }
    }

    private List<BlockPos> getExposedBlocks(BlockPos[] pair) {
        if (pair != null && pair.length >= 2 && mc.field_71441_e != null) {
            Set<BlockPos> exposedBlocks = new HashSet<>();
            for (BlockPos bedPart : pair) {
                IBlockState bedState = mc.field_71441_e.func_180495_p(bedPart);
                if (bedState != null && bedState.func_177230_c() instanceof BlockBed) {
                    for (EnumFacing side : EnumFacing.values()) {
                        BlockPos neighbor = bedPart.func_177972_a(side);
                        if (!neighbor.equals(pair[0]) && !neighbor.equals(pair[1])) {
                            Block neighborBlock = mc.field_71441_e.func_180495_p(neighbor).func_177230_c();
                            if (neighborBlock == Blocks.field_150350_a) {
                                exposedBlocks.add(neighbor);
                            }
                        }
                    }
                }
            }
            return new ArrayList<>(exposedBlocks);
        } else {
            return Collections.emptyList();
        }
    }

    private boolean isFullyExposed(BlockPos[] pair) {
        if (pair != null && pair.length >= 2 && mc.field_71441_e != null) {
            boolean hasBedPart = false;
            for (BlockPos bedPart : pair) {
                IBlockState state = mc.field_71441_e.func_180495_p(bedPart);
                Block block = state.func_177230_c();
                if (block instanceof BlockBed) {
                    hasBedPart = true;
                } else if (block != Blocks.field_150350_a) {
                    return false;
                }
                for (EnumFacing side : EnumFacing.values()) {
                    if (side != EnumFacing.DOWN) {
                        BlockPos neighbor = bedPart.func_177972_a(side);
                        if (!neighbor.equals(pair[0]) && !neighbor.equals(pair[1]) && mc.field_71441_e.func_180495_p(neighbor).func_177230_c() != Blocks.field_150350_a) {
                            return false;
                        }
                    }
                }
            }
            return hasBedPart;
        } else {
            return false;
        }
    }

    private void renderDefenseOverlay(BlockPos[] blocks, float blockHeight) {
        BedESP.DefenseOverlaySnapshot snapshot = this.defenseSnapshots.get(blocks[0].func_177986_g());
        if (snapshot != null && snapshot.matches(blocks[1]) && !snapshot.entries.isEmpty()) {
            RenderManager renderManager = mc.func_175598_ae();
            FontRenderer fontRenderer = mc.field_71466_p;
            if (renderManager != null && fontRenderer != null) {
                AxisAlignedBB bedBounds = bedWorldBounds(blocks[0], blocks[1], blockHeight);
                double x = (bedBounds.field_72340_a + bedBounds.field_72336_d) * 0.5 - renderManager.field_78730_l;
                double y = bedBounds.field_72337_e + this.defenseHeight.getInput() - renderManager.field_78731_m;
                double z = (bedBounds.field_72339_c + bedBounds.field_72334_f) * 0.5 - renderManager.field_78728_n;
                float renderScale = this.computeDefenseBaseScaleValue();
                if (this.defenseAutoScale.isToggled()) {
                    float distance = (float) Math.sqrt(x * x + y * y + z * z);
                    renderScale = this.computeDefenseScaleValue(distance);
                }
                List<BedESP.DefenseOverlayEntry> stacks = snapshot.entries;
                int contentWidth = stacks.size() * 18 - 2;
                int left = -contentWidth / 2;
                int iconY = -8;
                int backgroundLeft = left - 3;
                int backgroundTop = iconY - 3;
                int backgroundRight = left + contentWidth + 3;
                int backgroundBottom = iconY + 16 + 3;
                GlStateManager.func_179094_E();
                try {
                    GlStateManager.func_179109_b((float) x, (float) y, (float) z);
                    GlStateManager.func_179114_b(-renderManager.field_78735_i, 0.0F, 1.0F, 0.0F);
                    GlStateManager.func_179114_b(renderManager.field_78732_j, 1.0F, 0.0F, 0.0F);
                    GlStateManager.func_179152_a(-renderScale, -renderScale, renderScale);
                    GlStateManager.func_179140_f();
                    GlStateManager.func_179132_a(false);
                    GlStateManager.func_179097_i();
                    GlStateManager.func_179147_l();
                    GlStateManager.func_179120_a(770, 771, 1, 0);
                    this.renderDefenseBackground(backgroundLeft, backgroundTop, backgroundRight, backgroundBottom);
                    this.applyDefenseOverlayTextState();
                    for (int i = 0; i < stacks.size(); i++) {
                        BedESP.DefenseOverlayEntry stackData = stacks.get(i);
                        int iconX = left + i * 18;
                        this.renderDefenseEntry(stackData, iconX, iconY);
                        this.applyDefenseOverlayTextState();
                        if (!this.showDefenseTools.isToggled() && this.showDefenseCounts.isToggled() && stackData.count > 1) {
                            String countText = String.valueOf(stackData.getCount());
                            fontRenderer.func_175063_a(countText, iconX + 17 - fontRenderer.func_78256_a(countText), iconY + 9, 16777215);
                            this.applyDefenseOverlayTextState();
                        }
                    }
                } finally {
                    GlStateManager.func_179126_j();
                    GlStateManager.func_179132_a(true);
                    GlStateManager.func_179140_f();
                    GlStateManager.func_179101_C();
                    GlStateManager.func_179084_k();
                    GlStateManager.func_179141_d();
                    GlStateManager.func_179098_w();
                    GlStateManager.func_179120_a(770, 771, 1, 0);
                    GL11.glTexEnvi(8960, 8704, 8448);
                    GlStateManager.func_179131_c(1.0F, 1.0F, 1.0F, 1.0F);
                    GlStateManager.func_179121_F();
                }
            }
        }
    }

    private void renderDefenseEntry(BedESP.DefenseOverlayEntry entry, int iconX, int iconY) {
        if (entry != null) {
            if (entry.hasItemStack()) {
                RenderUtils.renderItemAndEffectIntoGui3D(entry.renderStack, iconX, iconY);
            } else {
                if (entry.hasBlockSprite()) {
                    this.renderDefenseBlockSprite(entry.blockSprite, iconX, iconY);
                }
            }
        }
    }

    private void renderDefenseBlockSprite(TextureAtlasSprite sprite, int iconX, int iconY) {
        if (sprite != null) {
            mc.func_110434_K().func_110577_a(TextureMap.field_110575_b);
            GlStateManager.func_179098_w();
            GlStateManager.func_179147_l();
            GlStateManager.func_179141_d();
            GlStateManager.func_179131_c(1.0F, 1.0F, 1.0F, 1.0F);
            Tessellator tessellator = Tessellator.func_178181_a();
            WorldRenderer worldRenderer = tessellator.func_178180_c();
            worldRenderer.func_181668_a(7, DefaultVertexFormats.field_181707_g);
            worldRenderer.func_181662_b(iconX, iconY + 16, 0.0).func_181673_a(sprite.func_94209_e(), sprite.func_94210_h()).func_181675_d();
            worldRenderer.func_181662_b(iconX + 16, iconY + 16, 0.0).func_181673_a(sprite.func_94212_f(), sprite.func_94210_h()).func_181675_d();
            worldRenderer.func_181662_b(iconX + 16, iconY, 0.0).func_181673_a(sprite.func_94212_f(), sprite.func_94206_g()).func_181675_d();
            worldRenderer.func_181662_b(iconX, iconY, 0.0).func_181673_a(sprite.func_94209_e(), sprite.func_94206_g()).func_181675_d();
            tessellator.func_78381_a();
        }
    }

    private void renderDefenseBackground(int left, int top, int right, int bottom) {
        RenderUtils.drawRoundedGradientOutlinedRectangle(left, top, right, bottom, 8.0F, 1929379840, -1778384896, -1778384896);
    }

    private void applyDefenseOverlayTextState() {
        GlStateManager.func_179140_f();
        GlStateManager.func_179097_i();
        GlStateManager.func_179132_a(false);
        GlStateManager.func_179098_w();
        GlStateManager.func_179141_d();
        GlStateManager.func_179092_a(516, 0.1F);
        GlStateManager.func_179147_l();
        GlStateManager.func_179120_a(770, 771, 1, 0);
        GL11.glTexEnvi(8960, 8704, 8448);
        GlStateManager.func_179131_c(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private BedESP.DefenseOverlaySnapshot computeDefenseSnapshot(BlockPos foot, BlockPos head) {
        BedESP.LayerOffsets[] layers = this.getLayerOffsets(foot, head);
        if (layers == null) {
            return new BedESP.DefenseOverlaySnapshot(new BlockPos(head), Collections.emptyList());
        } else {
            Map<BedESP.DefenseBlockKey, Integer> finalCounts = new HashMap<>();
            Map<Integer, BedESP.DefenseBlockKey> resolvedBlockKeys = new HashMap<>();
            int sparseLayerCount = 0;
            MutableBlockPos mutablePos = new MutableBlockPos();
            for (BedESP.LayerOffsets layerOffsets : layers) {
                Map<BedESP.DefenseBlockKey, Integer> layerCounts = new HashMap<>();
                int layerTotalBlocks = 0;
                int layerAirBlocks = 0;
                for (BedESP.RelativeOffset offset : layerOffsets.positions) {
                    mutablePos.func_181079_c(foot.func_177958_n() + offset.x, foot.func_177956_o() + offset.y, foot.func_177952_p() + offset.z);
                    if (this.accumulateDefenseBlock(mutablePos, layerCounts, resolvedBlockKeys)) {
                        layerAirBlocks++;
                    }
                    layerTotalBlocks++;
                }
                if (layerTotalBlocks != 0 && !((float) layerAirBlocks / layerTotalBlocks > 0.2F)) {
                    sparseLayerCount = 0;
                    for (Entry<BedESP.DefenseBlockKey, Integer> countedBlock : layerCounts.entrySet()) {
                        int count = countedBlock.getValue();
                        if ((float) count / layerTotalBlocks >= 0.2F) {
                            finalCounts.put(countedBlock.getKey(), finalCounts.getOrDefault(countedBlock.getKey(), 0) + count);
                        }
                    }
                } else if (++sparseLayerCount >= 2) {
                    break;
                }
            }
            List<BedESP.DefenseOverlayEntry> entries;
            if (this.showDefenseTools.isToggled()) {
                Set<BedESP.ToolOverlayType> toolTypes = new HashSet<>();
                for (BedESP.DefenseBlockKey defenseBlockKey : finalCounts.keySet()) {
                    BedESP.ToolOverlayType toolType = BedESP.ToolOverlayType.fromState(defenseBlockKey.state);
                    if (toolType != null) {
                        toolTypes.add(toolType);
                    }
                }
                if (toolTypes.contains(BedESP.ToolOverlayType.DIAMOND_PICKAXE)) {
                    toolTypes.remove(BedESP.ToolOverlayType.IRON_PICKAXE);
                }
                entries = new ArrayList<>(toolTypes.size());
                for (BedESP.ToolOverlayType toolType : toolTypes) {
                    entries.add(new BedESP.DefenseOverlayEntry(toolType.createRenderStack(), null, 1, toolType.sortName));
                }
                entries.sort((left, right) -> left.sortName.compareToIgnoreCase(right.sortName));
            } else {
                entries = new ArrayList<>();
                for (Entry<BedESP.DefenseBlockKey, Integer> entry2 : finalCounts.entrySet()) {
                    entries.add(new BedESP.DefenseOverlayEntry(entry2.getKey().createRenderStack(), entry2.getKey().blockSprite, entry2.getValue(), entry2.getKey().sortName));
                }
                entries.sort((left, right) -> {
                    int countCompare = Integer.compare(right.count, left.count);
                    return countCompare != 0 ? countCompare : left.sortName.compareToIgnoreCase(right.sortName);
                });
            }
            return new BedESP.DefenseOverlaySnapshot(new BlockPos(head), Collections.unmodifiableList(entries));
        }
    }

    private boolean accumulateDefenseBlock(BlockPos pos, Map<BedESP.DefenseBlockKey, Integer> layerCounts, Map<Integer, BedESP.DefenseBlockKey> resolvedBlockKeys) {
        IBlockState state = mc.field_71441_e.func_180495_p(pos);
        if (state != null && state.func_177230_c() != Blocks.field_150350_a) {
            IBlockState normalizedState = normalizeDefenseState(state);
            int stateId = Block.func_176210_f(normalizedState);
            BedESP.DefenseBlockKey key = resolvedBlockKeys.get(stateId);
            if (key == null) {
                key = BedESP.DefenseBlockKey.from(normalizedState, pos, mc.field_71441_e);
                resolvedBlockKeys.put(stateId, key);
            }
            layerCounts.put(key, layerCounts.getOrDefault(key, 0) + 1);
            return false;
        } else {
            return true;
        }
    }

    private BedESP.LayerOffsets[] getLayerOffsets(BlockPos foot, BlockPos head) {
        EnumFacing facing = getBedFacing(foot, head);
        return facing == null ? null : DEFENSE_OFFSETS.get(facing);
    }

    private static EnumMap<EnumFacing, BedESP.LayerOffsets[]> buildLayerOffsetsByFacing() {
        EnumMap<EnumFacing, BedESP.LayerOffsets[]> offsets = new EnumMap<>(EnumFacing.class);
        offsets.put(EnumFacing.EAST, buildLayerOffsets(EnumFacing.EAST));
        offsets.put(EnumFacing.WEST, buildLayerOffsets(EnumFacing.WEST));
        offsets.put(EnumFacing.SOUTH, buildLayerOffsets(EnumFacing.SOUTH));
        offsets.put(EnumFacing.NORTH, buildLayerOffsets(EnumFacing.NORTH));
        return offsets;
    }

    private static BedESP.LayerOffsets[] buildLayerOffsets(EnumFacing canonicalFacing) {
        BlockPos foot = BlockPos.field_177992_a;
        BlockPos head = foot.func_177972_a(canonicalFacing);
        boolean facingZ = canonicalFacing.func_176740_k() == Axis.Z;
        BlockPos firstBedPart = facingZ ? (head.func_177952_p() > foot.func_177952_p() ? head : foot) : (head.func_177958_n() > foot.func_177958_n() ? head : foot);
        BlockPos secondBedPart = firstBedPart.equals(foot) ? head : foot;
        BlockPos[] bedParts = new BlockPos[] { firstBedPart, secondBedPart };
        BedESP.LayerOffsets[] layers = new BedESP.LayerOffsets[5];
        Set<Long> seenAcrossLayers = new HashSet<>();
        for (int layer = 1; layer <= 5; layer++) {
            List<BedESP.RelativeOffset> offsets = new ArrayList<>();
            for (int intValue = 0; intValue < bedParts.length; intValue++) {
                BlockPos bed = bedParts[intValue];
                int outwardOffset = intValue == 0 ? layer : -layer;
                int startX = facingZ ? bed.func_177958_n() : bed.func_177958_n() + outwardOffset;
                int startZ = facingZ ? bed.func_177952_p() + outwardOffset : bed.func_177952_p();
                for (int advance = 0; advance <= layer; advance++) {
                    int yOffset = 0;
                    for (int breadth = advance; breadth >= 0; breadth--) {
                        int firstY = bed.func_177956_o() + yOffset;
                        int firstX;
                        int firstZ;
                        int secondX;
                        int secondZ;
                        if (facingZ) {
                            int z = startZ - (intValue == 0 ? advance : -advance);
                            firstX = startX - breadth;
                            firstZ = z;
                            secondX = startX + breadth;
                            secondZ = z;
                        } else {
                            int x = startX - (intValue == 0 ? advance : -advance);
                            firstX = x;
                            firstZ = startZ - breadth;
                            secondX = x;
                            secondZ = startZ + breadth;
                        }
                        addOffset(offsets, seenAcrossLayers, firstX, firstY, firstZ);
                        addOffset(offsets, seenAcrossLayers, secondX, firstY, secondZ);
                        if (breadth > 0) {
                            yOffset++;
                        }
                    }
                }
            }
            layers[layer - 1] = new BedESP.LayerOffsets(Collections.unmodifiableList(offsets));
        }
        return layers;
    }

    private static void addOffset(List<BedESP.RelativeOffset> offsets, Set<Long> seen, int x, int y, int z) {
        long key = new BlockPos(x, y, z).func_177986_g();
        if (seen.add(key)) {
            offsets.add(new BedESP.RelativeOffset(x, y, z));
        }
    }

    private static EnumFacing getBedFacing(BlockPos foot, BlockPos head) {
        int dx = head.func_177958_n() - foot.func_177958_n();
        int dz = head.func_177952_p() - foot.func_177952_p();
        for (EnumFacing facing : EnumFacing.field_176754_o) {
            if (facing.func_82601_c() == dx && facing.func_82599_e() == dz) {
                return facing;
            }
        }
        return null;
    }

    private void clearDefenseState() {
        this.defenseSnapshots.clear();
        this.defenseWatchRegions.clear();
        this.watchedBedsByDefensePos.clear();
        this.watchedBedsByChunk.clear();
        this.dirtyDefenseBeds.clear();
    }

    private void markBedsDirtyForDefensePos(BlockPos pos) {
        if (pos != null) {
            Set<Long> feet = this.watchedBedsByDefensePos.get(pos.func_177986_g());
            if (feet != null) {
                this.dirtyDefenseBeds.addAll(feet);
            }
        }
    }

    private void markBedsDirtyForChunk(int chunkX, int chunkZ) {
        Set<Long> feet = this.watchedBedsByChunk.get(chunkKey(chunkX, chunkZ));
        if (feet != null) {
            this.dirtyDefenseBeds.addAll(feet);
        }
    }

    private void registerDefenseWatch(BlockPos foot, BlockPos head) {
        BedESP.LayerOffsets[] layers = this.getLayerOffsets(foot, head);
        long footKey = foot.func_177986_g();
        if (layers == null) {
            this.defenseWatchRegions.put(footKey, new BedESP.DefenseWatchRegion(head.func_177986_g(), Collections.emptySet(), Collections.emptySet()));
        } else {
            Set<Long> watchedPositions = new HashSet<>();
            Set<Long> watchedChunks = new HashSet<>();
            for (BedESP.LayerOffsets layer : layers) {
                for (BedESP.RelativeOffset offset : layer.positions) {
                    BlockPos watchedPos = new BlockPos(foot.func_177958_n() + offset.x, foot.func_177956_o() + offset.y, foot.func_177952_p() + offset.z);
                    long posKey = watchedPos.func_177986_g();
                    if (watchedPositions.add(posKey)) {
                        this.watchedBedsByDefensePos.computeIfAbsent(posKey, ignored -> new HashSet<>()).add(footKey);
                        watchedChunks.add(chunkKey(watchedPos.func_177958_n() >> 4, watchedPos.func_177952_p() >> 4));
                    }
                }
            }
            for (Long chunkKey : watchedChunks) {
                this.watchedBedsByChunk.computeIfAbsent(chunkKey, ignored -> new HashSet<>()).add(footKey);
            }
            this.defenseWatchRegions.put(footKey, new BedESP.DefenseWatchRegion(head.func_177986_g(), Collections.unmodifiableSet(watchedPositions), Collections.unmodifiableSet(watchedChunks)));
        }
    }

    private void unregisterDefenseWatch(long footKey) {
        BedESP.DefenseWatchRegion region = this.defenseWatchRegions.remove(footKey);
        this.defenseSnapshots.remove(footKey);
        this.dirtyDefenseBeds.remove(footKey);
        if (region != null) {
            for (Long posKey : region.watchedPositions) {
                Set<Long> feet = this.watchedBedsByDefensePos.get(posKey);
                if (feet != null) {
                    feet.remove(footKey);
                    if (feet.isEmpty()) {
                        this.watchedBedsByDefensePos.remove(posKey);
                    }
                }
            }
            for (Long chunkKey : region.watchedChunks) {
                Set<Long> feet = this.watchedBedsByChunk.get(chunkKey);
                if (feet != null) {
                    feet.remove(footKey);
                    if (feet.isEmpty()) {
                        this.watchedBedsByChunk.remove(chunkKey);
                    }
                }
            }
        }
    }

    private static long chunkKey(int chunkX, int chunkZ) {
        return (long) chunkX << 32 | chunkZ & 4294967295L;
    }

    private float getBlockHeight() {
        return this.renderFullBlock.isToggled() ? 1.0F : 0.5625F;
    }

    private float computeDefenseBaseScaleValue() {
        return (float) this.defenseScale.getInput() * 0.02F;
    }

    private float computeDefenseScaleValue(float distance) {
        float baseScale = this.computeDefenseBaseScaleValue();
        float effectiveDistance = Math.max(1.0F, distance);
        float scaledValue = baseScale * (effectiveDistance / 8.0F);
        return Math.max(baseScale, scaledValue);
    }

    private static ItemStack resolveBlockItemStack(IBlockState state, BlockPos pos, World world) {
        Block block = state.func_177230_c();
        try {
            Item item = block.func_180665_b(world, pos);
            if (item == null) {
                return null;
            } else {
                int meta = item.func_77614_k() ? block.func_176222_j(world, pos) : 0;
                return new ItemStack(item, 1, meta);
            }
        } catch (Exception exception) {
            return null;
        }
    }

    private static TextureAtlasSprite resolveBlockSprite(IBlockState state) {
        if (mc != null && mc.func_175602_ab() != null && mc.func_175602_ab().func_175023_a() != null) {
            TextureAtlasSprite sprite = mc.func_175602_ab().func_175023_a().func_178122_a(state);
            return sprite == null ? null : sprite;
        } else {
            return null;
        }
    }

    private static IBlockState normalizeDefenseState(IBlockState state) {
        if (state == null) {
            return null;
        } else {
            Block block = state.func_177230_c();
            if (block == Blocks.field_150355_j || block == Blocks.field_150358_i) {
                return Blocks.field_150355_j.func_176223_P();
            } else if (block == Blocks.field_150353_l || block == Blocks.field_150356_k) {
                return Blocks.field_150353_l.func_176223_P();
            } else {
                return block == Blocks.field_150480_ab ? Blocks.field_150480_ab.func_176223_P() : state;
            }
        }
    }

    private static String getFallbackStateName(IBlockState state) {
        String localizedName = state.func_177230_c().func_149732_F();
        if (localizedName != null && !localizedName.isEmpty()) {
            return localizedName;
        } else {
            Object registryName = Block.field_149771_c.func_177774_c(state.func_177230_c());
            if (registryName != null) {
                int meta = state.func_177230_c().func_176201_c(state);
                return meta != 0 ? registryName + ":" + meta : registryName.toString();
            } else {
                return "unknown";
            }
        }
    }

    private static ItemStack fallbackRenderStack(Block block) {
        if (block == Blocks.field_150324_C) {
            return new ItemStack(Items.field_151104_aV);
        } else {
            try {
                Item item = Item.func_150898_a(block);
                if (item != null) {
                    int meta = block.func_176201_c(block.func_176223_P());
                    return new ItemStack(item, 1, meta);
                }
            } catch (Exception exception2) {
            }
            return new ItemStack(Blocks.field_180401_cv);
        }
    }

    public int getCurrentColor() {
        int mode = this.colorMode == null ? 0 : (int) this.colorMode.getInput();
        switch(mode) {
            case 0:
                return this.color.getColor();
            case 1:
                double pct = Math.sin(System.currentTimeMillis() * this.gradientSpeed.getInput() / 1000.0) * 0.5 + 0.5;
                Color firstColor = new Color(this.color.getRed(), this.color.getGreen(), this.color.getBlue(), this.color.getAlpha());
                Color secondColor = new Color(this.color2.getRed(), this.color2.getGreen(), this.color2.getBlue(), this.color2.getAlpha());
                Color blended = Theme.convert(firstColor, secondColor, pct);
                int alpha = (int) (this.color.getAlpha() * pct + this.color2.getAlpha() * (1.0 - pct));
                return Utils.mergeAlpha(blended.getRGB(), alpha);
            case 2:
                return Utils.mergeAlpha(Utils.getChroma(2L, 0L), this.color.getAlpha());
            case 3:
                return Utils.mergeAlpha(Theme.getGradient(this.getSelectedThemeIndex(), 0.0), 64);
            default:
                return this.color.getColor();
        }
    }

    private int getSelectedThemeIndex() {
        return this.theme != null && this.theme.getOptions().length != 0 ? (int) Math.max(0.0, Math.min((double) (this.theme.getOptions().length - 1), this.theme.getInput())) : 10;
    }

    private static final class BedBreakAnimation {

        private final BlockPos[] pair;

        private final Timer timer;

        private BedBreakAnimation(BlockPos[] pair, Timer timer) {
            this.pair = pair;
            this.timer = timer;
        }
    }

    private static final class DefenseBlockKey {

        private final IBlockState state;

        private final String identityKey;

        private final String sortName;

        private final int hashCode;

        private final ItemStack renderStack;

        private final TextureAtlasSprite blockSprite;

        private DefenseBlockKey(IBlockState state, String identityKey, String sortName, ItemStack renderStack, TextureAtlasSprite blockSprite) {
            this.state = state;
            this.identityKey = identityKey;
            this.sortName = sortName;
            this.renderStack = renderStack;
            this.blockSprite = blockSprite;
            this.hashCode = identityKey.hashCode();
        }

        private static BedESP.DefenseBlockKey from(IBlockState state, BlockPos pos, World world) {
            String fallbackName = BedESP.getFallbackStateName(state);
            ItemStack stack = BedESP.resolveBlockItemStack(state, pos, world);
            String identityKey = resolveIdentityKey(state, stack);
            TextureAtlasSprite sprite = null;
            if (stack == null || stack.func_77973_b() == null) {
                sprite = BedESP.resolveBlockSprite(state);
                if (sprite == null) {
                    stack = BedESP.fallbackRenderStack(state.func_177230_c());
                } else {
                    stack = null;
                }
            }
            String sortName = stack != null && stack.func_77973_b() != null ? getSafeDisplayName(stack, fallbackName) : fallbackName;
            return new BedESP.DefenseBlockKey(state, identityKey, sortName, stack, sprite);
        }

        private ItemStack createRenderStack() {
            return this.renderStack == null ? null : this.renderStack.func_77946_l();
        }

        private static String resolveIdentityKey(IBlockState state, ItemStack stack) {
            if (stack != null && stack.func_77973_b() != null) {
                return Item.func_150891_b(stack.func_77973_b()) + ":" + stack.func_77960_j();
            } else {
                Object registryName = Block.field_149771_c.func_177774_c(state.func_177230_c());
                return registryName != null ? registryName.toString() : Integer.toString(Block.func_149682_b(state.func_177230_c()));
            }
        }

        private static String getSafeDisplayName(ItemStack stack, String fallback) {
            if (stack != null && stack.func_77973_b() != null) {
                try {
                    String displayName = stack.func_82833_r();
                    return displayName != null && !displayName.isEmpty() ? displayName : fallback;
                } catch (Exception exception3) {
                    return fallback;
                }
            } else {
                return fallback;
            }
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            } else if (!(obj instanceof BedESP.DefenseBlockKey)) {
                return false;
            } else {
                BedESP.DefenseBlockKey other = (BedESP.DefenseBlockKey) obj;
                return this.identityKey.equals(other.identityKey);
            }
        }

        @Override
        public int hashCode() {
            return this.hashCode;
        }
    }

    private static final class DefenseOverlayEntry {

        private final ItemStack renderStack;

        private final TextureAtlasSprite blockSprite;

        private final int count;

        private final String sortName;

        private DefenseOverlayEntry(ItemStack renderStack, TextureAtlasSprite blockSprite, int count, String sortName) {
            this.renderStack = renderStack;
            this.blockSprite = blockSprite;
            this.count = count;
            this.sortName = sortName;
        }

        private boolean hasItemStack() {
            return this.renderStack != null && this.renderStack.func_77973_b() != null;
        }

        private boolean hasBlockSprite() {
            return this.blockSprite != null;
        }

        private int getCount() {
            return this.count;
        }
    }

    private static final class DefenseOverlaySnapshot {

        private final long headKey;

        private final List<BedESP.DefenseOverlayEntry> entries;

        private DefenseOverlaySnapshot(BlockPos head, List<BedESP.DefenseOverlayEntry> entries) {
            this.headKey = head.func_177986_g();
            this.entries = entries;
        }

        private boolean matches(BlockPos otherHead) {
            return otherHead != null && this.headKey == otherHead.func_177986_g();
        }
    }

    private static final class DefenseWatchRegion {

        private final long headKey;

        private final Set<Long> watchedPositions;

        private final Set<Long> watchedChunks;

        private DefenseWatchRegion(long headKey, Set<Long> watchedPositions, Set<Long> watchedChunks) {
            this.headKey = headKey;
            this.watchedPositions = watchedPositions;
            this.watchedChunks = watchedChunks;
        }

        private boolean matches(BlockPos otherHead) {
            return otherHead != null && this.headKey == otherHead.func_177986_g();
        }
    }

    private static final class LayerOffsets {

        private final List<BedESP.RelativeOffset> positions;

        private LayerOffsets(List<BedESP.RelativeOffset> positions) {
            this.positions = positions;
        }
    }

    private static final class RelativeOffset {

        private final int x;

        private final int y;

        private final int z;

        private RelativeOffset(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    private static enum ToolOverlayType {

        DIAMOND_PICKAXE(Items.field_151046_w, "Diamond Pickaxe"),
        IRON_AXE(Items.field_151036_c, "Iron Axe"),
        IRON_HOE(Items.field_151019_K, "Iron Hoe"),
        IRON_PICKAXE(Items.field_151035_b, "Iron Pickaxe"),
        SHEARS(Items.field_151097_aZ, "Shears"),
        IRON_SHOVEL(Items.field_151037_a, "Iron Shovel"),
        IRON_SWORD(Items.field_151040_l, "Iron Sword");

        private final Item item;

        private final String sortName;

        private final ItemStack renderStack;

        private ToolOverlayType(Item item, String sortName) {
            this.item = item;
            this.sortName = sortName;
            this.renderStack = new ItemStack(item);
        }

        private ItemStack createRenderStack() {
            return this.renderStack.func_77946_l();
        }

        private static BedESP.ToolOverlayType fromState(IBlockState state) {
            if (state == null) {
                return null;
            } else {
                Block block = state.func_177230_c();
                if (block == Blocks.field_150343_Z) {
                    return DIAMOND_PICKAXE;
                } else {
                    BedESP.ToolOverlayType bestTool = IRON_PICKAXE;
                    float bestEfficiency = 1.0F;
                    for (BedESP.ToolOverlayType toolType : values()) {
                        if (toolType != DIAMOND_PICKAXE) {
                            float efficiency = Utils.getEfficiency(toolType.renderStack, block);
                            if (efficiency > bestEfficiency) {
                                bestEfficiency = efficiency;
                                bestTool = toolType;
                            }
                        }
                    }
                    return bestTool;
                }
            }
        }
    }
}
