package keystrokesmod.utility;

import net.minecraft.block.Block;
import net.minecraft.block.BlockAnvil;
import net.minecraft.block.BlockBasePressurePlate;
import net.minecraft.block.BlockBed;
import net.minecraft.block.BlockBrewingStand;
import net.minecraft.block.BlockChest;
import net.minecraft.block.BlockColored;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.BlockDispenser;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.BlockDropper;
import net.minecraft.block.BlockEnchantmentTable;
import net.minecraft.block.BlockEnderChest;
import net.minecraft.block.BlockFence;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.BlockFlowerPot;
import net.minecraft.block.BlockHopper;
import net.minecraft.block.BlockJukebox;
import net.minecraft.block.BlockLadder;
import net.minecraft.block.BlockNote;
import net.minecraft.block.BlockTrapDoor;
import net.minecraft.block.BlockWorkbench;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.init.Blocks;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

public class BlockUtils implements IMinecraftInstance {

    public static boolean isSamePos(BlockPos blockPos, BlockPos blockPos2) {
        return blockPos == blockPos2 || blockPos.func_177958_n() == blockPos2.func_177958_n() && blockPos.func_177956_o() == blockPos2.func_177956_o() && blockPos.func_177952_p() == blockPos2.func_177952_p();
    }

    public static boolean notFull(Block block) {
        return block instanceof BlockFenceGate || block instanceof BlockLadder || block instanceof BlockFlowerPot || block instanceof BlockBasePressurePlate || isFluid(block) || block instanceof BlockFence || block instanceof BlockAnvil || block instanceof BlockEnchantmentTable || block instanceof BlockChest;
    }

    public static boolean isNormalBlock(Block block) {
        return block == Blocks.field_150359_w || block.func_149730_j() && block != Blocks.field_150351_n && block != Blocks.field_150354_m && block != Blocks.field_150425_aM && block != Blocks.field_150335_W && block != Blocks.field_150462_ai && block != Blocks.field_150460_al && block != Blocks.field_150367_z && block != Blocks.field_150409_cd && block != Blocks.field_150323_B && block != Blocks.field_150483_bI;
    }

    public static BlockPos pos(double x, double y, double z) {
        return new BlockPos(x, y, z);
    }

    public static boolean isBlockPosEqual(BlockPos pos1, BlockPos pos2) {
        return pos1 == pos2 || pos1.func_177958_n() == pos2.func_177958_n() && pos1.func_177956_o() == pos2.func_177956_o() && pos1.func_177952_p() == pos2.func_177952_p();
    }

    public static BlockPos offsetPos(MovingObjectPosition mop) {
        return mop.func_178782_a().func_177972_a(mop.field_178784_b);
    }

    public static boolean isFluid(Block block) {
        return block.func_149688_o() == Material.field_151587_i || block.func_149688_o() == Material.field_151586_h;
    }

    public static boolean isInteractable(Block block) {
        return block instanceof BlockTrapDoor || block instanceof BlockDoor || block instanceof BlockContainer || block instanceof BlockJukebox || block instanceof BlockFenceGate || block instanceof BlockChest || block instanceof BlockEnderChest || block instanceof BlockEnchantmentTable || block instanceof BlockBrewingStand || block instanceof BlockBed || block instanceof BlockDropper || block instanceof BlockDispenser || block instanceof BlockHopper || block instanceof BlockAnvil || block instanceof BlockNote || block instanceof BlockWorkbench;
    }

    public static boolean isInteractable(MovingObjectPosition mv) {
        if (mv == null || mv.field_72313_a != MovingObjectType.BLOCK || mv.func_178782_a() == null) {
            return false;
        } else {
            return mc.field_71439_g.func_70093_af() && mc.field_71439_g.func_70694_bm() != null ? false : isInteractable(getBlock(mv.func_178782_a()));
        }
    }

    public static float getBlockHardness(Block block, ItemStack itemStack, boolean ignoreSlow, boolean ignoreGround) {
        float getBlockHardness = block.func_176195_g(mc.field_71441_e, null);
        if (getBlockHardness < 0.0F) {
            return 0.0F;
        } else {
            return !block.func_149688_o().func_76229_l() && (itemStack == null || !itemStack.func_150998_b(block)) ? getToolDigEfficiency(itemStack, block, ignoreSlow, ignoreGround) / getBlockHardness / 100.0F : getToolDigEfficiency(itemStack, block, ignoreSlow, ignoreGround) / getBlockHardness / 30.0F;
        }
    }

    public static float maxDigRateAcrossSlots(Block block, int slotCount) {
        if (mc.field_71439_g != null && slotCount > 0) {
            int n = Math.min(slotCount, mc.field_71439_g.field_71071_by.func_70302_i_());
            float best = 0.0F;
            for (int i = 0; i < n; i++) {
                float h = getBlockHardness(block, mc.field_71439_g.field_71071_by.func_70301_a(i), false, false);
                if (h > best) {
                    best = h;
                }
            }
            return best;
        } else {
            return 0.0F;
        }
    }

    public static float getToolDigEfficiency(ItemStack itemStack, Block block, boolean ignoreSlow, boolean ignoreGround) {
        float n = itemStack == null ? 1.0F : itemStack.func_77973_b().func_150893_a(itemStack, block);
        if (n > 1.0F) {
            int getEnchantmentLevel = EnchantmentHelper.func_77506_a(Enchantment.field_77349_p.field_77352_x, itemStack);
            if (getEnchantmentLevel > 0 && itemStack != null) {
                n += getEnchantmentLevel * getEnchantmentLevel + 1;
            }
        }
        if (mc.field_71439_g.func_70644_a(Potion.field_76422_e)) {
            n *= 1.0F + (mc.field_71439_g.func_70660_b(Potion.field_76422_e).func_76458_c() + 1) * 0.2F;
        }
        if (!ignoreSlow) {
            if (mc.field_71439_g.func_70644_a(Potion.field_76419_f)) {
                float n2;
                switch(mc.field_71439_g.func_70660_b(Potion.field_76419_f).func_76458_c()) {
                    case 0:
                        n2 = 0.3F;
                        break;
                    case 1:
                        n2 = 0.09F;
                        break;
                    case 2:
                        n2 = 0.0027F;
                        break;
                    default:
                        n2 = 8.1E-4F;
                }
                n *= n2;
            }
            if (mc.field_71439_g.func_70055_a(Material.field_151586_h) && !EnchantmentHelper.func_77510_g(mc.field_71439_g)) {
                n /= 5.0F;
            }
            if (!mc.field_71439_g.field_70122_E && !ignoreGround) {
                n /= 5.0F;
            }
        }
        return n;
    }

    public static Block getBlock(BlockPos blockPos) {
        return getBlockState(blockPos).func_177230_c();
    }

    public static Block getBlock(double x, double y, double z) {
        return getBlockState(new BlockPos(x, y, z)).func_177230_c();
    }

    public static Block getBlock(Vec3 position) {
        return getBlockState(new BlockPos(position.field_72450_a, position.field_72448_b, position.field_72449_c)).func_177230_c();
    }

    public static IBlockState getBlockState(BlockPos blockPos) {
        return mc.field_71441_e != null && blockPos != null ? mc.field_71441_e.func_180495_p(blockPos) : Blocks.field_150350_a.func_176223_P();
    }

    public static AxisAlignedBB getBlockSelectionBox(BlockPos pos) {
        if (mc.field_71441_e != null && pos != null) {
            IBlockState state = mc.field_71441_e.func_180495_p(pos);
            Block block = state.func_177230_c();
            block.func_180654_a(mc.field_71441_e, pos);
            AxisAlignedBB box = block.func_180646_a(mc.field_71441_e, pos);
            if (box == null) {
                box = new AxisAlignedBB(pos.func_177958_n(), pos.func_177956_o(), pos.func_177952_p(), pos.func_177958_n() + 1.0, pos.func_177956_o() + 1.0, pos.func_177952_p() + 1.0);
            }
            return box;
        } else {
            return null;
        }
    }

    public static AxisAlignedBB getCollisionOrSelectionBox(BlockPos pos) {
        if (mc.field_71441_e != null && pos != null) {
            IBlockState st = mc.field_71441_e.func_180495_p(pos);
            Block block = st.func_177230_c();
            AxisAlignedBB bb = block.func_180640_a(mc.field_71441_e, pos, st);
            if (bb == null) {
                bb = block.func_180646_a(mc.field_71441_e, pos);
            }
            if (bb == null) {
                bb = new AxisAlignedBB(pos.func_177958_n(), pos.func_177956_o(), pos.func_177952_p(), pos.func_177958_n() + 1.0, pos.func_177956_o() + 1.0, pos.func_177952_p() + 1.0);
            }
            return bb;
        } else {
            return null;
        }
    }

    public static AxisAlignedBB getCollisionOrSelectedOnly(BlockPos pos) {
        if (mc.field_71441_e != null && pos != null) {
            IBlockState st = mc.field_71441_e.func_180495_p(pos);
            Block block = st.func_177230_c();
            AxisAlignedBB bb = block.func_180640_a(mc.field_71441_e, pos, st);
            if (bb == null) {
                bb = block.func_180646_a(mc.field_71441_e, pos);
            }
            return bb;
        } else {
            return null;
        }
    }

    public static AxisAlignedBB unionBlockBounds(BlockPos a, BlockPos b) {
        AxisAlignedBB ua = getCollisionOrSelectionBox(a);
        AxisAlignedBB ub = getCollisionOrSelectionBox(b);
        return ua.func_111270_a(ub);
    }

    public static EnumFacing facingFromBlockCenterToPoint(BlockPos pos, Vec3 hit) {
        double px = hit.field_72450_a - (pos.func_177958_n() + 0.5);
        double py = hit.field_72448_b - (pos.func_177956_o() + 0.5);
        double pz = hit.field_72449_c - (pos.func_177952_p() + 0.5);
        double ax = Math.abs(px);
        double ay = Math.abs(py);
        double az = Math.abs(pz);
        if (ax > ay && ax > az) {
            return px > 0.0 ? EnumFacing.EAST : EnumFacing.WEST;
        } else if (ay > az) {
            return py > 0.0 ? EnumFacing.UP : EnumFacing.DOWN;
        } else {
            return pz > 0.0 ? EnumFacing.SOUTH : EnumFacing.NORTH;
        }
    }

    public static boolean check(BlockPos blockPos, Block block) {
        return getBlock(blockPos) == block;
    }

    public static boolean replaceable(BlockPos blockPos) {
        return !Utils.nullCheck() ? true : getBlock(blockPos).func_176200_f(mc.field_71441_e, blockPos);
    }

    public static boolean canSeeVecBlock(BlockPos pos, Vec3 vecPlayer, Vec3 vecBlockPoint) {
        MovingObjectPosition mop = mc.field_71441_e.func_147447_a(vecPlayer, vecBlockPoint, false, false, false);
        if (mop == null) {
            return true;
        } else {
            if (mop.field_72313_a == MovingObjectType.BLOCK) {
                BlockPos mopPos = mop.func_178782_a();
                if (mopPos.func_177958_n() == pos.func_177958_n() && mopPos.func_177956_o() == pos.func_177956_o() && mopPos.func_177952_p() == pos.func_177952_p()) {
                    return true;
                }
            }
            return false;
        }
    }

    public static boolean canBlockBeSeen(BlockPos pos) {
        Vec3 vecPlayer = new Vec3(mc.field_71439_g.field_70165_t, mc.field_71439_g.field_70163_u + mc.field_71439_g.func_70047_e(), mc.field_71439_g.field_70161_v);
        for (double offsetY = 0.0; offsetY <= 0.5; offsetY += 0.5) {
            double y = pos.func_177956_o() + offsetY;
            Vec3 vecBlockPoint = new Vec3(pos.func_177958_n() + 1, y, pos.func_177952_p() + 0.5);
            if (canSeeVecBlock(pos, vecPlayer, vecBlockPoint)) {
                return true;
            }
            vecBlockPoint = new Vec3(pos.func_177958_n(), y, pos.func_177952_p() + 0.5);
            if (canSeeVecBlock(pos, vecPlayer, vecBlockPoint)) {
                return true;
            }
            vecBlockPoint = new Vec3(pos.func_177958_n() + 0.5, y, pos.func_177952_p() + 1);
            if (canSeeVecBlock(pos, vecPlayer, vecBlockPoint)) {
                return true;
            }
            vecBlockPoint = new Vec3(pos.func_177958_n() + 0.5, y, pos.func_177952_p());
            if (canSeeVecBlock(pos, vecPlayer, vecBlockPoint)) {
                return true;
            }
        }
        return false;
    }

    public static EnumDyeColor getWoolColor(IBlockState state) {
        return (EnumDyeColor) state.func_177228_b().get(BlockColored.field_176581_a);
    }

    public static EnumFacing[] getVisibleFaces(Vec3 eye, BlockPos block) {
        EnumFacing yFace = Math.abs(eye.field_72448_b - (block.func_177956_o() + 1)) < Math.abs(eye.field_72448_b - block.func_177956_o()) ? EnumFacing.UP : EnumFacing.DOWN;
        EnumFacing zFace = Math.abs(eye.field_72449_c - (block.func_177952_p() + 1)) < Math.abs(eye.field_72449_c - block.func_177952_p()) ? EnumFacing.SOUTH : EnumFacing.NORTH;
        EnumFacing xFace = Math.abs(eye.field_72450_a - (block.func_177958_n() + 1)) < Math.abs(eye.field_72450_a - block.func_177958_n()) ? EnumFacing.EAST : EnumFacing.WEST;
        return new EnumFacing[] { yFace, zFace, xFace };
    }

    public static boolean containsFace(EnumFacing[] faces, EnumFacing face) {
        for (EnumFacing f : faces) {
            if (f == face) {
                return true;
            }
        }
        return false;
    }

    public static Vec3 getFaceCenter(BlockPos block, EnumFacing face) {
        double eps = 0.001;
        double cx = block.func_177958_n() + 0.5;
        double cy = block.func_177956_o() + 0.5;
        double cz = block.func_177952_p() + 0.5;
        switch(face) {
            case UP:
                return new Vec3(cx, block.func_177956_o() + 1 - eps, cz);
            case DOWN:
                return new Vec3(cx, block.func_177956_o() + eps, cz);
            case NORTH:
                return new Vec3(cx, cy, block.func_177952_p() + eps);
            case SOUTH:
                return new Vec3(cx, cy, block.func_177952_p() + 1 - eps);
            case EAST:
                return new Vec3(block.func_177958_n() + 1 - eps, cy, cz);
            case WEST:
                return new Vec3(block.func_177958_n() + eps, cy, cz);
            default:
                return new Vec3(cx, cy, cz);
        }
    }

    public static double dist2PointAABB(Vec3 p, BlockPos b) {
        double cx = Math.max((double) b.func_177958_n(), Math.min((double) (b.func_177958_n() + 1), p.field_72450_a));
        double cy = Math.max((double) b.func_177956_o(), Math.min((double) (b.func_177956_o() + 1), p.field_72448_b));
        double cz = Math.max((double) b.func_177952_p(), Math.min((double) (b.func_177952_p() + 1), p.field_72449_c));
        double dx = p.field_72450_a - cx;
        double dy = p.field_72448_b - cy;
        double dz = p.field_72449_c - cz;
        return dx * dx + dy * dy + dz * dz;
    }

    public static boolean canPlaceBlockOnSide(ItemStack stack, BlockPos pos, EnumFacing side) {
        return stack != null && stack.func_77973_b() instanceof ItemBlock ? ((ItemBlock) stack.func_77973_b()).func_179222_a(mc.field_71441_e, pos, side, mc.field_71439_g, stack) : false;
    }

    public static float getFistBreakTicks(Block block) {
        float hardness = block.func_176195_g(mc.field_71441_e, null);
        if (hardness < 0.0F) {
            return Float.MAX_VALUE;
        } else {
            return hardness == 0.0F ? 0.0F : hardness * (block.func_149688_o().func_76229_l() ? 30.0F : 100.0F);
        }
    }

    public static boolean hasAirNeighbor(BlockPos pos, BlockPos... exclude) {
        for (EnumFacing f : EnumFacing.values()) {
            BlockPos n = pos.func_177972_a(f);
            if (mc.field_71441_e.func_180495_p(n).func_177230_c() == Blocks.field_150350_a) {
                boolean excluded = false;
                for (BlockPos ex : exclude) {
                    if (n.equals(ex)) {
                        excluded = true;
                        break;
                    }
                }
                if (!excluded) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isAdjacentToBed(BlockPos pos) {
        for (EnumFacing face : EnumFacing.values()) {
            if (getBlock(pos.func_177972_a(face)) instanceof BlockBed) {
                return true;
            }
        }
        return false;
    }

    public static MovingObjectPosition traverseBlocksAlongRay(Vec3 start, Vec3 end, boolean wantBed, boolean wantAdjacent) {
        if (mc.field_71441_e == null) {
            return null;
        } else if (Double.isNaN(start.field_72450_a) || Double.isNaN(start.field_72448_b) || Double.isNaN(start.field_72449_c)) {
            return null;
        } else if (!Double.isNaN(end.field_72450_a) && !Double.isNaN(end.field_72448_b) && !Double.isNaN(end.field_72449_c)) {
            int destX = MathHelper.func_76128_c(end.field_72450_a);
            int destY = MathHelper.func_76128_c(end.field_72448_b);
            int destZ = MathHelper.func_76128_c(end.field_72449_c);
            int curX = MathHelper.func_76128_c(start.field_72450_a);
            int curY = MathHelper.func_76128_c(start.field_72448_b);
            int curZ = MathHelper.func_76128_c(start.field_72449_c);
            MovingObjectPosition firstHit = null;
            MovingObjectPosition candidate = getBlockCollisionHit(curX, curY, curZ, start, end);
            if (candidate != null) {
                if (isBedOrAdjacentMatch(candidate.func_178782_a(), wantBed, wantAdjacent)) {
                    return candidate;
                }
                firstHit = candidate;
            }
            Vec3 tracePos = start;
            int remaining = 200;
            while (remaining-- >= 0) {
                if (Double.isNaN(tracePos.field_72450_a) || Double.isNaN(tracePos.field_72448_b) || Double.isNaN(tracePos.field_72449_c)) {
                    return firstHit;
                }
                if (curX == destX && curY == destY && curZ == destZ) {
                    return firstHit;
                }
                boolean crossX = true;
                boolean crossY = true;
                boolean crossZ = true;
                double boundX = 999.0;
                double boundY = 999.0;
                double boundZ = 999.0;
                if (destX > curX) {
                    boundX = curX + 1.0;
                } else if (destX < curX) {
                    boundX = curX;
                } else {
                    crossX = false;
                }
                if (destY > curY) {
                    boundY = curY + 1.0;
                } else if (destY < curY) {
                    boundY = curY;
                } else {
                    crossY = false;
                }
                if (destZ > curZ) {
                    boundZ = curZ + 1.0;
                } else if (destZ < curZ) {
                    boundZ = curZ;
                } else {
                    crossZ = false;
                }
                double dx = end.field_72450_a - tracePos.field_72450_a;
                double dy = end.field_72448_b - tracePos.field_72448_b;
                double dz = end.field_72449_c - tracePos.field_72449_c;
                double tX = 999.0;
                double tY = 999.0;
                double tZ = 999.0;
                if (crossX) {
                    tX = (boundX - tracePos.field_72450_a) / dx;
                }
                if (crossY) {
                    tY = (boundY - tracePos.field_72448_b) / dy;
                }
                if (crossZ) {
                    tZ = (boundZ - tracePos.field_72449_c) / dz;
                }
                if (tX == -0.0) {
                    tX = -1.0E-4;
                }
                if (tY == -0.0) {
                    tY = -1.0E-4;
                }
                if (tZ == -0.0) {
                    tZ = -1.0E-4;
                }
                EnumFacing face;
                if (tX < tY && tX < tZ) {
                    face = destX > curX ? EnumFacing.WEST : EnumFacing.EAST;
                    tracePos = new Vec3(boundX, tracePos.field_72448_b + dy * tX, tracePos.field_72449_c + dz * tX);
                } else if (tY < tZ) {
                    face = destY > curY ? EnumFacing.DOWN : EnumFacing.UP;
                    tracePos = new Vec3(tracePos.field_72450_a + dx * tY, boundY, tracePos.field_72449_c + dz * tY);
                } else {
                    face = destZ > curZ ? EnumFacing.NORTH : EnumFacing.SOUTH;
                    tracePos = new Vec3(tracePos.field_72450_a + dx * tZ, tracePos.field_72448_b + dy * tZ, boundZ);
                }
                curX = MathHelper.func_76128_c(tracePos.field_72450_a) - (face == EnumFacing.EAST ? 1 : 0);
                curY = MathHelper.func_76128_c(tracePos.field_72448_b) - (face == EnumFacing.UP ? 1 : 0);
                curZ = MathHelper.func_76128_c(tracePos.field_72449_c) - (face == EnumFacing.SOUTH ? 1 : 0);
                candidate = getBlockCollisionHit(curX, curY, curZ, start, end);
                if (candidate != null) {
                    if (isBedOrAdjacentMatch(candidate.func_178782_a(), wantBed, wantAdjacent)) {
                        return candidate;
                    }
                    if (firstHit == null) {
                        firstHit = candidate;
                    }
                }
            }
            return firstHit;
        } else {
            return null;
        }
    }

    private static MovingObjectPosition getBlockCollisionHit(int x, int y, int z, Vec3 start, Vec3 end) {
        BlockPos pos = new BlockPos(x, y, z);
        IBlockState state = mc.field_71441_e.func_180495_p(pos);
        Block block = state.func_177230_c();
        return !block.func_176209_a(state, false) ? null : block.func_180636_a(mc.field_71441_e, pos, start, end);
    }

    private static boolean isBedOrAdjacentMatch(BlockPos pos, boolean wantBed, boolean wantAdjacent) {
        Block block = getBlock(pos);
        boolean isBed = block instanceof BlockBed;
        return wantBed && isBed ? true : wantAdjacent && !isBed && isAdjacentToBed(pos);
    }
}
