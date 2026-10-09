package keystrokesmod.module.impl.render;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.ColorSetting;
import keystrokesmod.module.setting.impl.GroupSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.RenderUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityEnderChest;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import org.lwjgl.opengl.GL11;

public class ChestESP extends Module {

    private static final int TRAPPED_CHEST_TYPE = 1;

    private static final ChestESP.ChestKind[] RENDERABLE_CHEST_KINDS = new ChestESP.ChestKind[] { ChestESP.ChestKind.NORMAL, ChestESP.ChestKind.TRAPPED, ChestESP.ChestKind.ENDER };

    private static final ThreadLocal<Boolean> CHEST_CHAMS_ACTIVE = ThreadLocal.withInitial(() -> false);

    private final EnumMap<ChestESP.ChestKind, ChestESP.ChestVisualSettings> chestSettingsByKind = new EnumMap<>(ChestESP.ChestKind.class);

    private final EnumMap<ChestESP.ChestKind, List<BlockPos>> trackedWorldBatches = new EnumMap<>(ChestESP.ChestKind.class);

    private final SliderSetting maxDistance;

    public ChestESP() {
        super("ChestESP", Module.category.render, 0);
        this.liteModule = true;
        this.chestSettingsByKind.put(ChestESP.ChestKind.NORMAL, this.registerChestSettings("Chest", 198, 132, 56));
        this.chestSettingsByKind.put(ChestESP.ChestKind.TRAPPED, this.registerChestSettings("Trapped chest", 176, 64, 64));
        this.chestSettingsByKind.put(ChestESP.ChestKind.ENDER, this.registerChestSettings("Ender chest", 128, 64, 192));
        this.registerSetting(this.maxDistance = new SliderSetting("Max distance", 128.0, 32.0, 256.0, 8.0));
    }

    @Override
    public void guiUpdate() {
        for (ChestESP.ChestKind chestKind : RENDERABLE_CHEST_KINDS) {
            this.getChestSettings(chestKind).updateVisibility(this);
        }
    }

    public static void onRenderChestPre(TileEntity tileEntity) {
        ChestESP mod = getChestEspModule();
        if (mod != null && mod.shouldApplyChamsTo(tileEntity)) {
            GL11.glEnable(32823);
            GL11.glPolygonOffset(1.0F, -1100000.0F);
            CHEST_CHAMS_ACTIVE.set(true);
        }
    }

    public static void onRenderChestPost() {
        if (Boolean.TRUE.equals(CHEST_CHAMS_ACTIVE.get())) {
            CHEST_CHAMS_ACTIVE.set(false);
            GL11.glPolygonOffset(1.0F, 1100000.0F);
            GL11.glDisable(32823);
        }
    }

    private static ChestESP getChestEspModule() {
        Module m = ModuleManager.getModule(ChestESP.class);
        return m instanceof ChestESP && m.isEnabled() ? (ChestESP) m : null;
    }

    private boolean shouldApplyChamsTo(TileEntity tileEntity) {
        ChestESP.ChestVisualSettings settings = this.getChestSettings(this.getChestKind(tileEntity));
        return settings != null && settings.isChamsEnabled() && this.isWithinMaxDistance(tileEntity) && !this.shouldSkipOpened(settings, tileEntity);
    }

    private ChestESP.ChestKind getChestKind(TileEntity tileEntity) {
        if (tileEntity instanceof TileEntityEnderChest) {
            return ChestESP.ChestKind.ENDER;
        } else if (tileEntity instanceof TileEntityChest) {
            return ((TileEntityChest) tileEntity).func_145980_j() == 1 ? ChestESP.ChestKind.TRAPPED : ChestESP.ChestKind.NORMAL;
        } else {
            return ChestESP.ChestKind.NONE;
        }
    }

    private ChestESP.ChestVisualSettings registerChestSettings(String groupName, int red, int green, int blue) {
        GroupSetting group = new GroupSetting(groupName);
        this.registerSetting(group);
        ButtonSetting outline = new ButtonSetting(group, "Outline", false);
        this.registerSetting(outline);
        ColorSetting outlineColor = new ColorSetting(group, "Outline color", red, green, blue, 255);
        this.registerSetting(outlineColor);
        ButtonSetting shade = new ButtonSetting(group, "Shade", false);
        this.registerSetting(shade);
        ColorSetting shadeColor = new ColorSetting(group, "Shade color", red, green, blue, 255);
        this.registerSetting(shadeColor);
        ButtonSetting chams = new ButtonSetting(group, "Chams", false);
        this.registerSetting(chams);
        ButtonSetting disableIfOpened = new ButtonSetting(group, "Disable if opened", false);
        this.registerSetting(disableIfOpened);
        return new ChestESP.ChestVisualSettings(outline, outlineColor, shade, shadeColor, chams, disableIfOpened);
    }

    private ChestESP.ChestVisualSettings getChestSettings(ChestESP.ChestKind chestKind) {
        return this.chestSettingsByKind.get(chestKind);
    }

    private boolean shouldSkipOpened(ChestESP.ChestVisualSettings settings, TileEntity tileEntity) {
        if (settings == null || !settings.isDisableIfOpened()) {
            return false;
        } else {
            return tileEntity instanceof TileEntityChest ? ((TileEntityChest) tileEntity).field_145989_m > 0.0F : tileEntity instanceof TileEntityEnderChest && ((TileEntityEnderChest) tileEntity).field_145972_a > 0.0F;
        }
    }

    private boolean hasAnyWorldOverlayEnabled() {
        for (ChestESP.ChestKind chestKind : RENDERABLE_CHEST_KINDS) {
            if (this.getChestSettings(chestKind).hasWorldOverlayEnabled()) {
                return true;
            }
        }
        return false;
    }

    private boolean isWithinMaxDistance(TileEntity tileEntity) {
        double maxDistSq = this.maxDistance.getInput() * this.maxDistance.getInput();
        return RenderUtils.isBlockPosWithinDistanceSqToView(tileEntity.func_174877_v(), maxDistSq);
    }

    private AxisAlignedBB getChestBoundingBox(BlockPos pos) {
        return new AxisAlignedBB(pos.func_177958_n(), pos.func_177956_o(), pos.func_177952_p(), pos.func_177958_n() + 1, pos.func_177956_o() + 1, pos.func_177952_p() + 1);
    }

    @SubscribeEvent
    public void onClientTick(ClientTickEvent event) {
        if (event.phase == Phase.END) {
            this.updateTrackedWorldBatches();
        }
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent ev) {
        if (Utils.nullCheck() && !this.trackedWorldBatches.isEmpty()) {
            for (ChestESP.ChestKind chestKind : RENDERABLE_CHEST_KINDS) {
                List<BlockPos> trackedBatch = this.trackedWorldBatches.get(chestKind);
                if (trackedBatch != null && !trackedBatch.isEmpty()) {
                    ChestESP.ChestVisualSettings settings = this.getChestSettings(chestKind);
                    if (settings != null && settings.hasWorldOverlayEnabled()) {
                        List<BlockPos> visibleBatch = new ArrayList<>();
                        for (BlockPos pos : trackedBatch) {
                            if (RenderUtils.isInViewFrustum(this.getChestBoundingBox(pos))) {
                                visibleBatch.add(pos);
                            }
                        }
                        if (!visibleBatch.isEmpty()) {
                            RenderUtils.renderChestBatch(visibleBatch, settings.getOutlineColor(), settings.getShadeColor(), settings.isOutlineEnabled(), settings.isShadeEnabled());
                        }
                    }
                }
            }
        }
    }

    private void updateTrackedWorldBatches() {
        this.trackedWorldBatches.clear();
        if (Utils.nullCheck() && mc.field_71441_e != null && this.hasAnyWorldOverlayEnabled()) {
            double maxDistSq = this.maxDistance.getInput() * this.maxDistance.getInput();
            for (ChestESP.ChestKind chestKind : RENDERABLE_CHEST_KINDS) {
                ChestESP.ChestVisualSettings settings = this.getChestSettings(chestKind);
                if (settings != null && settings.hasWorldOverlayEnabled()) {
                    this.trackedWorldBatches.put(chestKind, new ArrayList<>());
                }
            }
            for (TileEntity tileEntity : mc.field_71441_e.field_147482_g) {
                ChestESP.ChestKind chestKind2 = this.getChestKind(tileEntity);
                ChestESP.ChestVisualSettings settings = this.getChestSettings(chestKind2);
                if (settings != null && settings.hasWorldOverlayEnabled() && !this.shouldSkipOpened(settings, tileEntity)) {
                    BlockPos pos = tileEntity.func_174877_v();
                    if (RenderUtils.isBlockPosWithinDistanceSqToView(pos, maxDistSq)) {
                        List<BlockPos> trackedBatch = this.trackedWorldBatches.get(chestKind2);
                        if (trackedBatch != null) {
                            trackedBatch.add(pos);
                        }
                    }
                }
            }
        }
    }

    private static enum ChestKind {

        NONE, NORMAL, TRAPPED, ENDER
    }

    private static final class ChestVisualSettings {

        private final ButtonSetting outline;

        private final ColorSetting outlineColor;

        private final ButtonSetting shade;

        private final ColorSetting shadeColor;

        private final ButtonSetting chams;

        private final ButtonSetting disableIfOpened;

        private ChestVisualSettings(ButtonSetting outline, ColorSetting outlineColor, ButtonSetting shade, ColorSetting shadeColor, ButtonSetting chams, ButtonSetting disableIfOpened) {
            this.outline = outline;
            this.outlineColor = outlineColor;
            this.shade = shade;
            this.shadeColor = shadeColor;
            this.chams = chams;
            this.disableIfOpened = disableIfOpened;
        }

        private void updateVisibility(Module owner) {
            this.outlineColor.setVisible(this.outline.isToggled(), owner);
            this.shadeColor.setVisible(this.shade.isToggled(), owner);
        }

        private boolean hasWorldOverlayEnabled() {
            return this.isOutlineEnabled() || this.isShadeEnabled();
        }

        private boolean isOutlineEnabled() {
            return this.outline.isToggled();
        }

        private int getOutlineColor() {
            return this.outlineColor.getColor();
        }

        private boolean isShadeEnabled() {
            return this.shade.isToggled();
        }

        private int getShadeColor() {
            return this.shadeColor.getColor();
        }

        private boolean isChamsEnabled() {
            return this.chams.isToggled();
        }

        private boolean isDisableIfOpened() {
            return this.disableIfOpened.isToggled();
        }
    }
}
