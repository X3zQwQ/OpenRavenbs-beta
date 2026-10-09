package keystrokesmod.module.impl.render;

import java.util.EnumSet;
import java.util.Map.Entry;
import java.util.Set;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.BlockListSetting;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.BlockUtils;
import keystrokesmod.utility.RenderUtils;
import keystrokesmod.utility.SharedBlockHighlightCache;
import keystrokesmod.utility.Utils;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class BlockESP extends Module {

    private final BlockListSetting blockList;

    private final ButtonSetting outline;

    private final ButtonSetting shade;

    private final SliderSetting range;

    private final SliderSetting maxRenders;

    private final SliderSetting scanSpeed;

    private int prevListHash;

    public BlockESP() {
        super("BlockESP", Module.category.render);
        this.registerSetting(this.blockList = new BlockListSetting("Blocks"));
        this.registerSetting(this.outline = new ButtonSetting("Outline", true));
        this.registerSetting(this.shade = new ButtonSetting("Shade", false));
        this.registerSetting(this.range = new SliderSetting("Range", 64.0, 8.0, 256.0, 8.0));
        this.registerSetting(this.maxRenders = new SliderSetting("Max renders", 2048.0, 128.0, 8192.0, 128.0));
        this.registerSetting(this.scanSpeed = new SliderSetting("Scan speed", 8.0, 1.0, 32.0, 1.0));
    }

    @Override
    public void onEnable() {
        SharedBlockHighlightCache cache = SharedBlockHighlightCache.get();
        cache.attachBlockList(this.blockList);
        this.prevListHash = this.blockList.getBlocks().hashCode();
        if (!this.blockList.getBlocks().isEmpty()) {
            cache.enqueueLoadedChunks();
        }
    }

    @Override
    public void onDisable() {
        SharedBlockHighlightCache.get().detachBlockList();
    }

    @Override
    public void onUpdate() {
        if (Utils.nullCheck()) {
            int h = this.blockList.getBlocks().hashCode();
            if (h != this.prevListHash) {
                this.prevListHash = h;
                SharedBlockHighlightCache.get().onBlockListSettingsChanged();
            }
        }
    }

    public int getScanSpeedBudget() {
        return this.isEnabled() && !this.blockList.getBlocks().isEmpty() ? (int) this.scanSpeed.getInput() : 0;
    }

    @Override
    public String getInfo() {
        int total = SharedBlockHighlightCache.get().totalBlockList();
        return total > 0 ? String.valueOf(total) : "";
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent ev) {
        SharedBlockHighlightCache cache = SharedBlockHighlightCache.get();
        if (cache.anyConsumerActive() && Utils.nullCheck()) {
            double rangeSq = this.range.getInput() * this.range.getInput();
            double px = mc.field_71439_g.field_70165_t;
            double py = mc.field_71439_g.field_70163_u;
            double pz = mc.field_71439_g.field_70161_v;
            boolean doOutline = this.outline.isToggled();
            boolean doShade = this.shade.isToggled();
            int max = (int) this.maxRenders.getInput();
            int rendered = 0;
            for (Entry<Long, Set<BlockPos>> entry : cache.entriesBlockList()) {
                if (rendered >= max) {
                    break;
                }
                for (BlockPos pos : entry.getValue()) {
                    if (rendered >= max) {
                        break;
                    }
                    double dx = pos.func_177958_n() + 0.5 - px;
                    double dy = pos.func_177956_o() + 0.5 - py;
                    double dz = pos.func_177952_p() + 0.5 - pz;
                    if (!(dx * dx + dy * dy + dz * dz > rangeSq)) {
                        AxisAlignedBB box = BlockUtils.getBlockSelectionBox(pos);
                        if (box == null || RenderUtils.isInViewFrustum(box)) {
                            EnumSet<EnumFacing> visibleFaces = EnumSet.noneOf(EnumFacing.class);
                            for (EnumFacing f : EnumFacing.field_82609_l) {
                                if (!cache.containsBlockList(pos.func_177972_a(f))) {
                                    visibleFaces.add(f);
                                }
                            }
                            if (!visibleFaces.isEmpty()) {
                                IBlockState state = mc.field_71441_e.func_180495_p(pos);
                                MapColor mapColor = state.func_177230_c().func_180659_g(state);
                                int rgb = mapColor != null ? 0xFF000000 | mapColor.field_76291_p : -65536;
                                RenderUtils.renderBlockShape(pos, state, rgb, doOutline, doShade, visibleFaces);
                                rendered++;
                            }
                        }
                    }
                }
            }
        }
    }
}
