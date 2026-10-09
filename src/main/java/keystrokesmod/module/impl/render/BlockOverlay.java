package keystrokesmod.module.impl.render;

import java.awt.Color;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.ColorSetting;
import keystrokesmod.module.setting.impl.GroupSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.BlockUtils;
import keystrokesmod.utility.StairsUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDeadBush;
import net.minecraft.block.BlockDoublePlant;
import net.minecraft.block.BlockFlower;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.BlockTallGrass;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.init.Blocks;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

public class BlockOverlay extends Module {

    private static final String[] RENDER_MODES = new String[] { "Hidden", "Vanilla", "Side", "Full" };

    private static final String[] COLOR_MODES = new String[] { "Static", "Gradient", "Fade", "Chroma" };

    private static final double PADDING = 0.002;

    private final SliderSetting renderMode;

    private final GroupSetting overlayGroup;

    private final ButtonSetting overlayVisible;

    private final SliderSetting overlayColorMode;

    private final ColorSetting overlayColor;

    private final ColorSetting overlayColor2;

    private final SliderSetting overlayFadeSpeed;

    private final SliderSetting overlayChromaSpeed;

    private final GroupSetting outlineGroup;

    private final ButtonSetting outlineVisible;

    private final SliderSetting outlineColorMode;

    private final ColorSetting outlineColor;

    private final ColorSetting outlineColor2;

    private final SliderSetting outlineFadeSpeed;

    private final SliderSetting outlineChromaSpeed;

    private final GroupSetting optionsGroup;

    private final SliderSetting thickness;

    private final ButtonSetting persistence;

    private final ButtonSetting depthless;

    private final ButtonSetting barriers;

    private final ButtonSetting hidePlants;

    public BlockOverlay() {
        super("Block Overlay", Module.category.render);
        this.registerSetting(this.renderMode = new SliderSetting("Mode", 2, RENDER_MODES));
        this.registerSetting(this.overlayGroup = new GroupSetting("Overlay"));
        this.registerSetting(this.overlayVisible = new ButtonSetting(this.overlayGroup, "Visible", true));
        this.registerSetting(this.overlayColorMode = new SliderSetting(this.overlayGroup, "Color mode", 0, COLOR_MODES));
        this.registerSetting(this.overlayColor = new ColorSetting(this.overlayGroup, "Color", 0, 0, 0, 100));
        this.registerSetting(this.overlayColor2 = new ColorSetting(this.overlayGroup, "Color 2", 255, 255, 255, 100));
        this.registerSetting(this.overlayFadeSpeed = new SliderSetting(this.overlayGroup, "Fade speed", 5.5, 1.0, 10.0, 0.5));
        this.registerSetting(this.overlayChromaSpeed = new SliderSetting(this.overlayGroup, "Chroma speed", 5.5, 1.0, 10.0, 0.5));
        this.registerSetting(this.outlineGroup = new GroupSetting("Outline"));
        this.registerSetting(this.outlineVisible = new ButtonSetting(this.outlineGroup, "Visible", true));
        this.registerSetting(this.outlineColorMode = new SliderSetting(this.outlineGroup, "Color mode", 0, COLOR_MODES));
        this.registerSetting(this.outlineColor = new ColorSetting(this.outlineGroup, "Color", 0, 0, 0, 255));
        this.registerSetting(this.outlineColor2 = new ColorSetting(this.outlineGroup, "Color 2", 255, 255, 255, 255));
        this.registerSetting(this.outlineFadeSpeed = new SliderSetting(this.outlineGroup, "Fade speed", 5.5, 1.0, 10.0, 0.5));
        this.registerSetting(this.outlineChromaSpeed = new SliderSetting(this.outlineGroup, "Chroma speed", 5.5, 1.0, 10.0, 0.5));
        this.registerSetting(this.optionsGroup = new GroupSetting("Options"));
        this.registerSetting(this.thickness = new SliderSetting(this.optionsGroup, "Thickness", 2.0, 1.0, 10.0, 0.5));
        this.registerSetting(this.persistence = new ButtonSetting(this.optionsGroup, "Persistence", false));
        this.registerSetting(this.depthless = new ButtonSetting(this.optionsGroup, "Depthless", false));
        this.registerSetting(this.barriers = new ButtonSetting(this.optionsGroup, "Barriers", false));
        this.registerSetting(this.hidePlants = new ButtonSetting(this.optionsGroup, "Hide plants", false));
    }

    @Override
    public void guiUpdate() {
        int oMode = (int) this.overlayColorMode.getInput();
        this.overlayColor2.setVisible(oMode == 1 || oMode == 2, this);
        this.overlayFadeSpeed.setVisible(oMode == 2, this);
        this.overlayChromaSpeed.setVisible(oMode == 3, this);
        int olMode = (int) this.outlineColorMode.getInput();
        this.outlineColor2.setVisible(olMode == 1 || olMode == 2, this);
        this.outlineFadeSpeed.setVisible(olMode == 2, this);
        this.outlineChromaSpeed.setVisible(olMode == 3, this);
    }

    @Override
    public String getInfo() {
        return RENDER_MODES[(int) this.renderMode.getInput()];
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onDrawBlockHighlight(DrawBlockHighlightEvent e) {
        int mode = (int) this.renderMode.getInput();
        if (mode == 0) {
            e.setCanceled(true);
        } else if (mode != 1) {
            e.setCanceled(true);
            if (Utils.nullCheck()) {
                if (this.persistence.isToggled() || !mc.field_71439_g.func_175149_v()) {
                    BlockPos pos = this.getFocusedBlock();
                    if (pos != null) {
                        boolean showOverlay = this.overlayVisible.isToggled();
                        boolean showOutline = this.outlineVisible.isToggled();
                        if (showOverlay || showOutline) {
                            EnumFacing side = mode == 2 ? mc.field_71476_x.field_178784_b : null;
                            this.renderCustomBlockOverlay(pos, side, showOverlay, showOutline);
                        }
                    }
                }
            }
        }
    }

    private void renderCustomBlockOverlay(BlockPos pos, EnumFacing side, boolean showOverlay, boolean showOutline) {
        AxisAlignedBB box = BlockUtils.getBlockSelectionBox(pos);
        if (box != null) {
            box = box.func_72314_b(0.002, 0.002, 0.002);
            double vx = mc.func_175598_ae().field_78730_l;
            double vy = mc.func_175598_ae().field_78731_m;
            double vz = mc.func_175598_ae().field_78728_n;
            int overlayStart = 0;
            int overlayEnd = 0;
            int outlineStart = 0;
            int outlineEnd = 0;
            if (showOverlay) {
                overlayStart = computeStart((int) this.overlayColorMode.getInput(), this.overlayColor, this.overlayColor2, this.overlayFadeSpeed.getInput(), this.overlayChromaSpeed.getInput());
                overlayEnd = computeEnd((int) this.overlayColorMode.getInput(), this.overlayColor, this.overlayColor2, this.overlayFadeSpeed.getInput(), this.overlayChromaSpeed.getInput());
            }
            if (showOutline) {
                outlineStart = computeStart((int) this.outlineColorMode.getInput(), this.outlineColor, this.outlineColor2, this.outlineFadeSpeed.getInput(), this.outlineChromaSpeed.getInput());
                outlineEnd = computeEnd((int) this.outlineColorMode.getInput(), this.outlineColor, this.outlineColor2, this.outlineFadeSpeed.getInput(), this.outlineChromaSpeed.getInput());
            }
            GL11.glPushMatrix();
            GlStateManager.func_179129_p();
            GlStateManager.func_179147_l();
            GlStateManager.func_179120_a(770, 771, 1, 0);
            GlStateManager.func_179090_x();
            GlStateManager.func_179132_a(false);
            boolean depthDisabled = this.depthless.isToggled();
            if (depthDisabled) {
                GlStateManager.func_179097_i();
            }
            GL11.glEnable(2848);
            GL11.glHint(3154, 4354);
            if (showOutline) {
                GL11.glLineWidth((float) this.thickness.getInput());
            }
            GL11.glShadeModel(7425);
            try {
                drawOverlayGeometry(mc, pos, box, side, vx, vy, vz, overlayStart, overlayEnd, outlineStart, outlineEnd, showOverlay, showOutline);
            } finally {
                GL11.glShadeModel(7424);
                GL11.glLineWidth(2.0F);
                GL11.glDisable(2848);
                if (depthDisabled) {
                    GlStateManager.func_179126_j();
                }
                GlStateManager.func_179132_a(true);
                GlStateManager.func_179098_w();
                GlStateManager.func_179089_o();
                GlStateManager.func_179084_k();
                GL11.glPopMatrix();
            }
        }
    }

    public static void renderBlockOutline(BlockPos pos, int outlineArgbStart, int outlineArgbEnd, float lineWidth, boolean depthless) {
        Minecraft m = Minecraft.func_71410_x();
        if (m.field_71441_e != null && pos != null) {
            AxisAlignedBB box = BlockUtils.getBlockSelectionBox(pos);
            if (box != null) {
                box = box.func_72314_b(0.002, 0.002, 0.002);
                double vx = m.func_175598_ae().field_78730_l;
                double vy = m.func_175598_ae().field_78731_m;
                double vz = m.func_175598_ae().field_78728_n;
                GL11.glPushMatrix();
                GlStateManager.func_179129_p();
                GlStateManager.func_179147_l();
                GlStateManager.func_179120_a(770, 771, 1, 0);
                GlStateManager.func_179090_x();
                GlStateManager.func_179132_a(false);
                if (depthless) {
                    GlStateManager.func_179097_i();
                }
                GL11.glEnable(2848);
                GL11.glHint(3154, 4354);
                GL11.glLineWidth(lineWidth);
                GL11.glShadeModel(7425);
                try {
                    drawOverlayGeometry(m, pos, box, null, vx, vy, vz, 0, 0, outlineArgbStart, outlineArgbEnd, false, true);
                } finally {
                    GL11.glShadeModel(7424);
                    GL11.glLineWidth(2.0F);
                    GL11.glDisable(2848);
                    if (depthless) {
                        GlStateManager.func_179126_j();
                    }
                    GlStateManager.func_179132_a(true);
                    GlStateManager.func_179098_w();
                    GlStateManager.func_179089_o();
                    GlStateManager.func_179084_k();
                    GL11.glPopMatrix();
                }
            }
        }
    }

    private static void drawOverlayGeometry(Minecraft mc, BlockPos pos, AxisAlignedBB axisAlignedBB, EnumFacing side, double vx, double vy, double vz, int overlayStart, int overlayEnd, int outlineStart, int outlineEnd, boolean showOverlay, boolean showOutline) {
        AxisAlignedBB axisAlignedBB2 = axisAlignedBB.func_72317_d(-vx, -vy, -vz);
        IBlockState state = mc.field_71441_e.func_180495_p(pos);
        if (state.func_177230_c() instanceof BlockStairs) {
            StairsUtils.drawStairs(pos, state, axisAlignedBB, side, vx, vy, vz, overlayStart, overlayEnd, outlineStart, outlineEnd, showOverlay, showOutline, BlockOverlay::drawFace);
        } else if (side != null) {
            drawFace(axisAlignedBB2, side, overlayStart, overlayEnd, outlineStart, outlineEnd, showOverlay, showOutline);
        } else {
            for (EnumFacing face : EnumFacing.values()) {
                drawFace(axisAlignedBB2, face, overlayStart, overlayEnd, outlineStart, outlineEnd, showOverlay, showOutline);
            }
        }
    }

    private BlockPos getFocusedBlock() {
        if (mc.field_71476_x != null && mc.field_71476_x.field_72313_a == MovingObjectType.BLOCK) {
            BlockPos pos = mc.field_71476_x.func_178782_a();
            if (pos == null) {
                return null;
            } else {
                Block block = mc.field_71441_e.func_180495_p(pos).func_177230_c();
                if (block == Blocks.field_150350_a) {
                    return null;
                } else if (block == Blocks.field_180401_cv && !this.barriers.isToggled()) {
                    return null;
                } else {
                    return !this.hidePlants.isToggled() || !(block instanceof BlockTallGrass) && !(block instanceof BlockFlower) && !(block instanceof BlockDeadBush) && !(block instanceof BlockDoublePlant) ? pos : null;
                }
            }
        } else {
            return null;
        }
    }

    private static int interpolate(int c1, int c2, double pct) {
        Color a = new Color(c1, true);
        Color b = new Color(c2, true);
        double inv = 1.0 - pct;
        return new Color((int) (a.getRed() * pct + b.getRed() * inv), (int) (a.getGreen() * pct + b.getGreen() * inv), (int) (a.getBlue() * pct + b.getBlue() * inv), (int) (a.getAlpha() * pct + b.getAlpha() * inv)).getRGB();
    }

    private static int computeStart(int colorMode, ColorSetting color1, ColorSetting color2, double fadeSpeed, double chromaSpeed) {
        switch(colorMode) {
            case 1:
                return color1.getColor();
            case 2:
                double pct = Math.sin(System.currentTimeMillis() / (1100.0 - fadeSpeed * 100.0)) * 0.5 + 0.5;
                return interpolate(color1.getColor(), color2.getColor(), pct);
            case 3:
                int alpha = color1.getColor() >> 24 & 0xFF;
                return Utils.mergeAlpha(Utils.getChroma((long) chromaSpeed), alpha);
            default:
                return color1.getColor();
        }
    }

    private static int computeEnd(int colorMode, ColorSetting color1, ColorSetting color2, double fadeSpeed, double chromaSpeed) {
        switch(colorMode) {
            case 1:
                return color2.getColor();
            case 2:
                double pct = Math.sin((System.currentTimeMillis() + 500L) / (1100.0 - fadeSpeed * 100.0)) * 0.5 + 0.5;
                return interpolate(color1.getColor(), color2.getColor(), pct);
            default:
                return computeStart(colorMode, color1, color2, fadeSpeed, chromaSpeed);
        }
    }

    private static void drawFace(AxisAlignedBB box, EnumFacing face, int os, int oe, int ls, int le, boolean overlay, boolean outline) {
        Tessellator ts = Tessellator.func_178181_a();
        WorldRenderer wr = ts.func_178180_c();
        if (overlay) {
            wr.func_181668_a(7, DefaultVertexFormats.field_181706_f);
            addFaceVertices(wr, face, box, os, oe);
            ts.func_78381_a();
        }
        if (outline) {
            wr.func_181668_a(2, DefaultVertexFormats.field_181706_f);
            addFaceVertices(wr, face, box, ls, le);
            ts.func_78381_a();
        }
    }

    private static void v(WorldRenderer wr, double x, double y, double z, int color) {
        wr.func_181662_b(x, y, z).func_181669_b(color >> 16 & 0xFF, color >> 8 & 0xFF, color & 0xFF, color >> 24 & 0xFF).func_181675_d();
    }

    private static void addFaceVertices(WorldRenderer wr, EnumFacing face, AxisAlignedBB box, int start, int end) {
        switch(face) {
            case UP:
                v(wr, box.field_72340_a, box.field_72337_e, box.field_72334_f, start);
                v(wr, box.field_72336_d, box.field_72337_e, box.field_72334_f, end);
                v(wr, box.field_72336_d, box.field_72337_e, box.field_72339_c, start);
                v(wr, box.field_72340_a, box.field_72337_e, box.field_72339_c, end);
                break;
            case DOWN:
                v(wr, box.field_72336_d, box.field_72338_b, box.field_72334_f, start);
                v(wr, box.field_72340_a, box.field_72338_b, box.field_72334_f, end);
                v(wr, box.field_72340_a, box.field_72338_b, box.field_72339_c, start);
                v(wr, box.field_72336_d, box.field_72338_b, box.field_72339_c, end);
                break;
            case NORTH:
                v(wr, box.field_72336_d, box.field_72337_e, box.field_72339_c, start);
                v(wr, box.field_72336_d, box.field_72338_b, box.field_72339_c, end);
                v(wr, box.field_72340_a, box.field_72338_b, box.field_72339_c, start);
                v(wr, box.field_72340_a, box.field_72337_e, box.field_72339_c, end);
                break;
            case SOUTH:
                v(wr, box.field_72340_a, box.field_72337_e, box.field_72334_f, start);
                v(wr, box.field_72340_a, box.field_72338_b, box.field_72334_f, end);
                v(wr, box.field_72336_d, box.field_72338_b, box.field_72334_f, start);
                v(wr, box.field_72336_d, box.field_72337_e, box.field_72334_f, end);
                break;
            case EAST:
                v(wr, box.field_72336_d, box.field_72337_e, box.field_72339_c, start);
                v(wr, box.field_72336_d, box.field_72337_e, box.field_72334_f, end);
                v(wr, box.field_72336_d, box.field_72338_b, box.field_72334_f, start);
                v(wr, box.field_72336_d, box.field_72338_b, box.field_72339_c, end);
                break;
            case WEST:
                v(wr, box.field_72340_a, box.field_72337_e, box.field_72334_f, start);
                v(wr, box.field_72340_a, box.field_72337_e, box.field_72339_c, end);
                v(wr, box.field_72340_a, box.field_72338_b, box.field_72339_c, start);
                v(wr, box.field_72340_a, box.field_72338_b, box.field_72334_f, end);
        }
    }
}
