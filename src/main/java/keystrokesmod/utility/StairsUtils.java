package keystrokesmod.utility;

import net.minecraft.block.BlockStairs.EnumHalf;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing.Axis;
import net.minecraft.util.EnumFacing;
import org.lwjgl.opengl.GL11;

public final class StairsUtils {

    public static void drawStairs(BlockPos blockPos, IBlockState blockState, AxisAlignedBB box, EnumFacing side, double viewerX, double viewerY, double viewerZ, int overlayStartColor, int overlayEndColor, int outlineStartColor, int outlineEndColor, boolean overlay, boolean outline, StairsUtils.BlockFaceDrawer drawer) {
        EnumFacing blockFacing = (EnumFacing) blockState.func_177229_b(BlockStairs.field_176309_a);
        EnumHalf blockHalf = (EnumHalf) blockState.func_177229_b(BlockStairs.field_176308_b);
        int blockX = blockPos.func_177958_n();
        int blockY = blockPos.func_177956_o();
        int blockZ = blockPos.func_177952_p();
        int angleX = blockHalf == EnumHalf.TOP ? 270 : 0;
        int angleY = 0;
        switch(blockFacing) {
            case NORTH:
                angleY = 180;
                break;
            case EAST:
                angleY = 90;
                break;
            case WEST:
                angleY = 270;
        }
        GL11.glPushMatrix();
        GL11.glTranslated(-viewerX, -viewerY, -viewerZ);
        GL11.glTranslated(blockX + 0.5, blockY, blockZ + 0.5);
        GL11.glRotated(angleY, 0.0, 1.0, 0.0);
        GL11.glTranslated(0.0, 0.5, 0.0);
        GL11.glRotated(angleX, 1.0, 0.0, 0.0);
        GL11.glTranslated(-blockX - 0.5, -blockY - 0.5, -blockZ - 0.5);
        if (side == null) {
            drawStairsFull(box, overlayStartColor, overlayEndColor, outlineStartColor, outlineEndColor, overlay, outline, drawer);
        } else {
            drawStairsSide(box, blockHalf, blockFacing, side, overlayStartColor, overlayEndColor, outlineStartColor, outlineEndColor, overlay, outline, drawer);
        }
        GL11.glPopMatrix();
    }

    private static void drawStairsFull(AxisAlignedBB box, int overlayStart, int overlayEnd, int outlineStart, int outlineEnd, boolean overlay, boolean outline, StairsUtils.BlockFaceDrawer drawer) {
        drawStairsTop(box, overlayStart, overlayEnd, outlineStart, outlineEnd, overlay, outline, drawer);
        drawStairsBottom(box, overlayStart, overlayEnd, outlineStart, outlineEnd, overlay, outline, drawer);
        drawStairsNorth(box, overlayStart, overlayEnd, outlineStart, outlineEnd, overlay, outline, drawer);
        drawStairsEast(box, overlayStart, overlayEnd, outlineStart, outlineEnd, overlay, outline, drawer);
        drawStairsSouth(box, overlayStart, overlayEnd, outlineStart, outlineEnd, overlay, outline, drawer);
        drawStairsWest(box, overlayStart, overlayEnd, outlineStart, outlineEnd, overlay, outline, drawer);
    }

    private static void drawStairsSide(AxisAlignedBB box, EnumHalf blockHalf, EnumFacing blockFacing, EnumFacing side, int overlayStart, int overlayEnd, int outlineStart, int outlineEnd, boolean overlay, boolean outline, StairsUtils.BlockFaceDrawer drawer) {
        EnumFacing mapped = getSide(blockHalf, blockFacing, side);
        switch(mapped) {
            case NORTH:
                drawStairsNorth(box, overlayStart, overlayEnd, outlineStart, outlineEnd, overlay, outline, drawer);
                break;
            case EAST:
                drawStairsEast(box, overlayStart, overlayEnd, outlineStart, outlineEnd, overlay, outline, drawer);
                break;
            case WEST:
                drawStairsWest(box, overlayStart, overlayEnd, outlineStart, outlineEnd, overlay, outline, drawer);
                break;
            case UP:
                drawStairsTop(box, overlayStart, overlayEnd, outlineStart, outlineEnd, overlay, outline, drawer);
                break;
            case DOWN:
                drawStairsBottom(box, overlayStart, overlayEnd, outlineStart, outlineEnd, overlay, outline, drawer);
                break;
            case SOUTH:
                drawStairsSouth(box, overlayStart, overlayEnd, outlineStart, outlineEnd, overlay, outline, drawer);
        }
    }

    private static void drawStairsTop(AxisAlignedBB box, int os, int oe, int ls, int le, boolean overlay, boolean outline, StairsUtils.BlockFaceDrawer drawer) {
        AxisAlignedBB b1 = box.func_72331_e(0.0, 0.0, 0.25).func_72317_d(0.0, 0.0, 0.25);
        AxisAlignedBB b2 = box.func_72331_e(0.0, 0.0, 0.25).func_72317_d(0.0, -0.5, -0.25);
        drawer.draw(b1, EnumFacing.UP, os, oe, ls, le, overlay, outline);
        drawer.draw(b2, EnumFacing.UP, os, oe, ls, le, overlay, outline);
    }

    private static void drawStairsBottom(AxisAlignedBB box, int os, int oe, int ls, int le, boolean overlay, boolean outline, StairsUtils.BlockFaceDrawer drawer) {
        drawer.draw(box, EnumFacing.DOWN, os, oe, ls, le, overlay, outline);
    }

    private static void drawStairsNorth(AxisAlignedBB box, int os, int oe, int ls, int le, boolean overlay, boolean outline, StairsUtils.BlockFaceDrawer drawer) {
        drawer.draw(box.func_72331_e(0.0, 0.252, 0.0).func_72317_d(0.0, 0.252, 0.5), EnumFacing.NORTH, os, oe, ls, le, overlay, outline);
        drawer.draw(box.func_72331_e(0.0, 0.25, 0.0).func_72317_d(0.0, -0.25, 0.0), EnumFacing.NORTH, os, oe, ls, le, overlay, outline);
    }

    private static void drawStairsEast(AxisAlignedBB box, int os, int oe, int ls, int le, boolean overlay, boolean outline, StairsUtils.BlockFaceDrawer drawer) {
        drawer.draw(box.func_72331_e(0.0, 0.252, 0.25).func_72317_d(0.0, 0.252, 0.25), EnumFacing.EAST, os, oe, ls, le, overlay, outline);
        drawer.draw(box.func_72331_e(0.0, 0.25, 0.0).func_72317_d(0.0, -0.25, 0.0), EnumFacing.EAST, os, oe, ls, le, overlay, outline);
    }

    private static void drawStairsSouth(AxisAlignedBB box, int os, int oe, int ls, int le, boolean overlay, boolean outline, StairsUtils.BlockFaceDrawer drawer) {
        drawer.draw(box, EnumFacing.SOUTH, os, oe, ls, le, overlay, outline);
    }

    private static void drawStairsWest(AxisAlignedBB box, int os, int oe, int ls, int le, boolean overlay, boolean outline, StairsUtils.BlockFaceDrawer drawer) {
        drawer.draw(box.func_72331_e(0.0, 0.252, 0.25).func_72317_d(0.0, 0.252, 0.25), EnumFacing.WEST, os, oe, ls, le, overlay, outline);
        drawer.draw(box.func_72331_e(0.0, 0.25, 0.0).func_72317_d(0.0, -0.25, 0.0), EnumFacing.WEST, os, oe, ls, le, overlay, outline);
    }

    private static EnumFacing getSide(EnumHalf blockHalf, EnumFacing blockFacing, EnumFacing side) {
        if (blockHalf == EnumHalf.TOP) {
            switch(blockFacing) {
                case NORTH:
                    side = side.func_176732_a(Axis.X);
                    side = side.func_176732_a(Axis.Y);
                    side = side.func_176732_a(Axis.Y);
                    break;
                case EAST:
                    side = side.func_176732_a(Axis.Z);
                    side = side.func_176732_a(Axis.Y);
                    break;
                case WEST:
                    side = side.func_176732_a(Axis.Z);
                    side = side.func_176732_a(Axis.Y);
                    side = side.func_176732_a(Axis.Z);
                    side = side.func_176732_a(Axis.Z);
                case UP:
                case DOWN:
                default:
                    break;
                case SOUTH:
                    side = side.func_176732_a(Axis.X);
                    side = side.func_176732_a(Axis.X);
                    side = side.func_176732_a(Axis.X);
            }
        } else if (side != EnumFacing.UP && side != EnumFacing.DOWN) {
            switch(blockFacing) {
                case NORTH:
                    side = side.func_176734_d();
                    break;
                case EAST:
                    side = side.func_176735_f();
                    break;
                case WEST:
                    side = side.func_176735_f();
            }
        }
        return side;
    }

    private StairsUtils() {
    }

    public interface BlockFaceDrawer {

        void draw(AxisAlignedBB axisAlignedBB, EnumFacing enumFacing, int intValue, int intValue2, int intValue3, int intValue4, boolean enabled, boolean enabled2);
    }
}
