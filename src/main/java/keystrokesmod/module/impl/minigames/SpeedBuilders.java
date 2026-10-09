package keystrokesmod.module.impl.minigames;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import keystrokesmod.Raven;
import keystrokesmod.event.PreUpdateEvent;
import keystrokesmod.event.ReceivePacketEvent;
import keystrokesmod.mixin.impl.accessor.IAccessorMinecraft;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.DescriptionSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.BlockUtils;
import keystrokesmod.utility.RenderUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockButton;
import net.minecraft.block.BlockCauldron;
import net.minecraft.block.BlockDirectional;
import net.minecraft.block.BlockDispenser;
import net.minecraft.block.BlockDoublePlant;
import net.minecraft.block.BlockDropper;
import net.minecraft.block.BlockFlower;
import net.minecraft.block.BlockHopper;
import net.minecraft.block.BlockLadder;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.BlockLever;
import net.minecraft.block.BlockPumpkin;
import net.minecraft.block.BlockRail;
import net.minecraft.block.BlockRailBase;
import net.minecraft.block.BlockSkull;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.BlockStairs.EnumHalf;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.BlockTorch;
import net.minecraft.block.BlockTrapDoor;
import net.minecraft.block.BlockTripWire;
import net.minecraft.block.BlockTripWireHook;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.RenderTickEvent;

public class SpeedBuilders extends Module {

    private SliderSetting placeDelay;

    private ButtonSetting antiMiss;

    private ButtonSetting autoPlace;

    private ButtonSetting autoSwap;

    private ButtonSetting hoverPlace;

    private ButtonSetting infoHud;

    private ButtonSetting renderBlocks;

    private ButtonSetting renderOnlyPlaceable;

    private ConcurrentHashMap<BlockPos, SpeedBuilders.BuildBlockInfo> buildInfo = new ConcurrentHashMap<>();

    private BlockPos platformCenter;

    private boolean listenForPacket;

    public List<BlockPos> PLATFORM_POSITIONS = Arrays.asList(new BlockPos(45, 71, -18), new BlockPos(-16, 71, 45), new BlockPos(18, 71, 45), new BlockPos(45, 71, 16), new BlockPos(-18, 71, -45), new BlockPos(-45, 71, -16), new BlockPos(-45, 71, 18), new BlockPos(16, 71, -45));

    private int HIGHLIGHT_COLOR = new Color(31, 255, 22, 44).getRGB();

    private int UNPLACEABLE_COLOR = new Color(184, 255, 183, 30).getRGB();

    private boolean doneCollecting;

    private double blockCount;

    private int lastPlaceTick = 0;

    private boolean eliminated;

    public SpeedBuilders() {
        super("Speed Builders", Module.category.minigames);
        this.registerSetting(new DescriptionSetting("Middle click to toggle auto."));
        this.registerSetting(this.placeDelay = new SliderSetting("Place delay", " tick", 0.5, 0.0, 10.0, 0.5));
        this.registerSetting(this.antiMiss = new ButtonSetting("Anti miss", false));
        this.registerSetting(this.autoPlace = new ButtonSetting("Auto place", false));
        this.registerSetting(this.autoSwap = new ButtonSetting("Auto swap", true));
        this.registerSetting(this.hoverPlace = new ButtonSetting("Hover place", true));
        this.registerSetting(this.infoHud = new ButtonSetting("Info HUD", true));
        this.registerSetting(this.renderBlocks = new ButtonSetting("Render blocks", true));
        this.registerSetting(this.renderOnlyPlaceable = new ButtonSetting("Render only placeable", false));
    }

    @Override
    public void onDisable() {
        this.lastPlaceTick = 0;
    }

    @SubscribeEvent
    public void onPreUpdate(PreUpdateEvent e) {
        int gameStatus = this.getGameStatus();
        if (gameStatus != -1 && this.platformCenter != null) {
            if (gameStatus == 4) {
                this.doneCollecting = true;
            }
            if (gameStatus == 1 && !this.doneCollecting) {
                this.buildInfo = this.getBuildInfo(this.platformCenter);
                if (!this.buildInfo.isEmpty()) {
                    this.blockCount = this.buildInfo.size();
                }
            }
            if (gameStatus == 2) {
                this.doneCollecting = false;
                for (Entry<BlockPos, SpeedBuilders.BuildBlockInfo> entry : this.buildInfo.entrySet()) {
                    IBlockState currentState = mc.field_71441_e.func_180495_p(entry.getKey());
                    IBlockState requiredState = entry.getValue().requiredState;
                    if (currentState == null || requiredState == null) {
                        entry.getValue().isPlaced = false;
                    } else if (!currentState.equals(requiredState) && (!(requiredState.func_177230_c() instanceof BlockLeaves) || !currentState.func_177230_c().equals(requiredState.func_177230_c()))) {
                        entry.getValue().isPlaced = false;
                    } else {
                        entry.getValue().isPlaced = true;
                    }
                }
                if (this.getLookInfo() != null) {
                    MovingObjectPosition mop = this.getLookInfo();
                    if (mop.field_178784_b != null) {
                        BlockPos targetPos = mop.func_178782_a();
                        BlockPos facePos = targetPos.func_177972_a(mop.field_178784_b);
                        SpeedBuilders.BuildBlockInfo info = this.buildInfo.get(facePos);
                        if (info != null && !info.isPlaced) {
                            if (this.autoSwap.isToggled()) {
                                int requiredMeta = info.requiredState.func_177230_c().func_176201_c(info.requiredState);
                                int slot = this.getSlot(info.requiredState.func_177230_c(), requiredMeta);
                                if (slot != -1 && slot != mc.field_71439_g.field_71071_by.field_70461_c) {
                                    mc.field_71439_g.field_71071_by.field_70461_c = slot;
                                }
                            }
                            if (this.hoverPlace.isToggled() && this.holdingSameBlock(info.requiredState) && this.correctPlaceState(info.requiredState, targetPos, mop.field_178784_b, mop.field_72307_f, mc.field_71439_g.func_70694_bm())) {
                                if (this.lastPlaceTick++ < this.placeDelay.getInput()) {
                                    return;
                                }
                                ((IAccessorMinecraft) mc).callRightClickMouse();
                                this.lastPlaceTick = 0;
                            }
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onMouse(MouseEvent e) {
        if (e.buttonstate && Utils.nullCheck() && mc.field_71462_r == null) {
            if (e.button == 1 && this.antiMiss.isToggled() && this.getLookInfo() != null && this.getGameStatus() == 2) {
                MovingObjectPosition mop = this.getLookInfo();
                if (mop.field_178784_b != null) {
                    BlockPos targetPos = mop.func_178782_a();
                    BlockPos facePos = targetPos.func_177972_a(mop.field_178784_b);
                    SpeedBuilders.BuildBlockInfo info = this.buildInfo.get(facePos);
                    if (info == null || !this.holdingSameBlock(info.requiredState) || !this.correctPlaceState(info.requiredState, targetPos, mop.field_178784_b, mop.field_72307_f, mc.field_71439_g.func_70694_bm())) {
                        e.setCanceled(true);
                    }
                }
            } else if (e.button == 2) {
                if (this.autoSwap.isToggled()) {
                    this.autoSwap.disable();
                    this.hoverPlace.disable();
                } else {
                    this.autoSwap.enable();
                    this.hoverPlace.enable();
                }
            }
        }
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent ev) {
        if (Utils.nullCheck() && this.getGameStatus() == 2 && this.renderBlocks.isToggled()) {
            for (Entry<BlockPos, SpeedBuilders.BuildBlockInfo> buildData : this.buildInfo.entrySet()) {
                SpeedBuilders.BuildBlockInfo info = buildData.getValue();
                if (!info.isPlaced && this.holdingSameBlock(info.requiredState)) {
                    BlockPos pos = buildData.getKey();
                    boolean useWhite = true;
                    for (EnumFacing dir : EnumFacing.values()) {
                        BlockPos neighborPos = pos.func_177972_a(dir);
                        if (BlockUtils.getBlock(neighborPos) != Blocks.field_150350_a) {
                            useWhite = false;
                        }
                    }
                    if (!this.renderOnlyPlaceable.isToggled() || !useWhite) {
                        RenderUtils.renderBlockModel(buildData.getValue().requiredState, pos.func_177958_n(), pos.func_177956_o(), pos.func_177952_p(), useWhite ? this.UNPLACEABLE_COLOR : this.HIGHLIGHT_COLOR);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onRenderTick(RenderTickEvent e) {
        if (e.phase == Phase.END && Utils.nullCheck() && mc.field_71462_r == null) {
            int gameStatus = this.getGameStatus();
            if (Raven.DEBUG) {
                this.renderDebugMenu(gameStatus);
            }
            if (this.infoHud.isToggled()) {
                List<String> lines = new ArrayList<>();
                lines.add("§6Speed Builders");
                lines.add("§7Status: §b" + this.getGameStatusName(gameStatus));
                if (gameStatus == 2 && !this.eliminated) {
                    double placedCount = 0.0;
                    for (SpeedBuilders.BuildBlockInfo info : this.buildInfo.values()) {
                        if (info.isPlaced) {
                            placedCount++;
                        }
                    }
                    double percentage = 0.0;
                    if (this.buildInfo.isEmpty()) {
                        percentage = 100.0;
                        placedCount = this.blockCount;
                    } else if (this.blockCount > 0.0) {
                        percentage = placedCount / this.blockCount * 100.0;
                    }
                    lines.add("§7Progress: §b" + (int) placedCount + "§7/§b" + (int) this.blockCount + " " + Math.round(percentage) + "%");
                }
                lines.add("§7Auto: " + (this.autoEnabled() ? "§aENABLED" : "§cDISABLED"));
                int padding = 4;
                int maxWidth = 0;
                for (String line : lines) {
                    int lineWidth = mc.field_71466_p.func_78256_a(line);
                    if (lineWidth > maxWidth) {
                        maxWidth = lineWidth;
                    }
                }
                int lineHeight = mc.field_71466_p.field_78288_b;
                int lineSpacing = 3;
                int totalHeight = lines.size() * lineHeight + (lines.size() - 1) * lineSpacing + padding * 2;
                int totalWidth = maxWidth + padding * 2;
                float x = -5.0F;
                float y = 110.0F;
                RenderUtils.drawRoundedRectangle(x, y, x + totalWidth + 7.0F, y + totalHeight - 2.0F, 7.0F, Utils.mergeAlpha(Color.black.getRGB(), 120));
                float textX = x + padding;
                float textY = y + padding;
                for (int i = 0; i < lines.size(); i++) {
                    mc.field_71466_p.func_78276_b(lines.get(i), (int) (textX + 5.0F), (int) (textY + i * (lineHeight + lineSpacing)), -1);
                }
            }
        }
    }

    private void renderDebugMenu(int gameStatus) {
        List<String> lines = Arrays.asList("§6Debug", "§7Status: §b" + this.getGameStatusName(gameStatus) + " §8(" + gameStatus + ")", "§7Eliminated: " + (this.eliminated ? "§cYES" : "§aNO"));
        int padding = 5;
        int lineSpacing = 2;
        int maxWidth = 0;
        for (String line : lines) {
            maxWidth = Math.max(maxWidth, mc.field_71466_p.func_78256_a(line));
        }
        int lineHeight = mc.field_71466_p.field_78288_b;
        int width = maxWidth + padding * 2;
        int height = lines.size() * lineHeight + (lines.size() - 1) * lineSpacing + padding * 2;
        int x = -5;
        int y = 14;
        RenderUtils.drawRoundedRectangle(x, y, x + width, y + height, 7.0F, Utils.mergeAlpha(Color.black.getRGB(), 120));
        for (int i = 0; i < lines.size(); i++) {
            mc.field_71466_p.func_175065_a(lines.get(i), x + padding + 2, y + padding + i * (lineHeight + lineSpacing), -1, true);
        }
    }

    private String getGameStatusName(int gameStatus) {
        switch(gameStatus) {
            case -1:
                return "Disabled";
            case 0:
            default:
                return "Unknown";
            case 1:
            case 4:
                return "Showing";
            case 2:
                return "Building";
            case 3:
                return "Judging";
        }
    }

    @SubscribeEvent
    public void onEntityJoin(EntityJoinWorldEvent e) {
        if (Utils.nullCheck() && e.entity != null) {
            if (e.entity == mc.field_71439_g) {
                this.buildInfo.clear();
                this.platformCenter = null;
                this.listenForPacket = false;
                this.doneCollecting = false;
                this.eliminated = false;
            }
        }
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent e) {
        if (e.type != 2 && Utils.nullCheck() && this.getGameStatus() != -1 && !this.listenForPacket) {
            String stripped = Utils.stripColor(e.message.func_150260_c());
            if (!stripped.isEmpty()) {
                if (stripped.contains("Perfectly recreate the build you are shown each") || stripped.contains("The game starts in 1 second!")) {
                    this.listenForPacket = true;
                }
                if (stripped.startsWith(Utils.getServerName()) && stripped.contains(" got a perfect build in ") && stripped.endsWith("s!")) {
                    this.buildInfo.clear();
                    this.doneCollecting = false;
                }
                if (stripped.startsWith("Player eliminated: " + Utils.getServerName()) && stripped.endsWith("%)")) {
                    this.eliminated = true;
                }
            }
        }
    }

    @SubscribeEvent
    public void onReceivePacket(ReceivePacketEvent e) {
        if (this.listenForPacket && Utils.nullCheck() && e.getPacket() instanceof S08PacketPlayerPosLook) {
            Vec3 setPos = new Vec3(((S08PacketPlayerPosLook) e.getPacket()).func_148932_c(), ((S08PacketPlayerPosLook) e.getPacket()).func_148928_d(), ((S08PacketPlayerPosLook) e.getPacket()).func_148933_e());
            if (this.platformCenter == null) {
                this.platformCenter = this.findCenter(setPos);
            }
            this.listenForPacket = false;
        }
    }

    public int getGameStatus() {
        List<String> sidebar = Utils.getSidebarLines();
        if (sidebar.isEmpty()) {
            return -1;
        } else if (!Utils.stripColor(sidebar.get(0)).startsWith("BUILD BATTLE")) {
            return -1;
        } else {
            for (int i = 0; i < sidebar.size() - 1; i++) {
                String currentLine = Utils.stripColor(sidebar.get(i));
                String nextLine = Utils.stripColor(sidebar.get(i + 1));
                if (currentLine.startsWith("Round:")) {
                    if (nextLine.startsWith("Starts In: 00:03") && Utils.stripColor(sidebar.get(i + 3)).startsWith("Theme:")) {
                        return 4;
                    }
                    if (nextLine.startsWith("Starts In:")) {
                        return 1;
                    }
                    if (nextLine.startsWith("Time Left:")) {
                        return 2;
                    }
                    if (nextLine.startsWith("Judging:")) {
                        return 3;
                    }
                }
            }
            return 0;
        }
    }

    public BlockPos findCenter(Vec3 position) {
        BlockPos closestPos = null;
        double closestDistSq = Double.MAX_VALUE;
        double maxDistance = 30.0;
        double maxDistSq = maxDistance * maxDistance;
        for (BlockPos pos : this.PLATFORM_POSITIONS) {
            double dx = pos.func_177958_n() - position.field_72450_a;
            double dy = pos.func_177956_o() - position.field_72448_b;
            double dz = pos.func_177952_p() - position.field_72449_c;
            double distSq = Math.abs(dx * dx + dy * dy + dz * dz);
            if (distSq <= maxDistSq && distSq < closestDistSq) {
                closestDistSq = distSq;
                closestPos = pos;
            }
        }
        return closestPos;
    }

    public ConcurrentHashMap<BlockPos, SpeedBuilders.BuildBlockInfo> getBuildInfo(BlockPos centerPos) {
        ConcurrentHashMap<BlockPos, SpeedBuilders.BuildBlockInfo> blockInfo = new ConcurrentHashMap<>();
        int startX = centerPos.func_177958_n() - 3;
        int endX = centerPos.func_177958_n() + 3;
        int startZ = centerPos.func_177952_p() - 3;
        int endZ = centerPos.func_177952_p() + 3;
        int startY = centerPos.func_177956_o() + 1;
        int endY = startY + 30;
        for (int x = startX; x <= endX; x++) {
            for (int z = startZ; z <= endZ; z++) {
                for (int y = startY; y <= endY; y++) {
                    BlockPos currentPos = new BlockPos(x, y, z);
                    IBlockState state = mc.field_71441_e.func_180495_p(currentPos);
                    if (state.func_177230_c() != Blocks.field_150350_a) {
                        blockInfo.put(currentPos, new SpeedBuilders.BuildBlockInfo(state));
                    }
                }
            }
        }
        return blockInfo;
    }

    public boolean autoEnabled() {
        return this.autoSwap.isToggled() && this.hoverPlace.isToggled();
    }

    public boolean holdingSameBlock(IBlockState requiredState) {
        if (mc.field_71439_g != null && requiredState != null) {
            ItemStack heldItem = mc.field_71439_g.func_70694_bm();
            if (heldItem == null) {
                return false;
            } else {
                Item item = heldItem.func_77973_b();
                Block requiredBlock = requiredState.func_177230_c();
                if ((requiredBlock == Blocks.field_150355_j || requiredBlock == Blocks.field_150358_i) && item == Items.field_151131_as) {
                    return true;
                } else if (!(item instanceof ItemBlock)) {
                    return false;
                } else {
                    Block heldBlock = ((ItemBlock) item).func_179223_d();
                    int heldMeta = heldItem.func_77952_i();
                    int requiredMeta = requiredBlock.func_176201_c(requiredState);
                    if (requiredBlock == Blocks.field_150362_t || requiredBlock == Blocks.field_150361_u) {
                        requiredMeta &= 3;
                        heldMeta &= 3;
                    }
                    if (this.removeMeta(heldBlock)) {
                        heldMeta = 0;
                        requiredMeta = 0;
                    }
                    return heldBlock == requiredBlock && heldMeta == requiredMeta;
                }
            }
        } else {
            return false;
        }
    }

    public MovingObjectPosition getLookInfo() {
        MovingObjectPosition movingObjectPosition = mc.field_71476_x;
        return movingObjectPosition != null && movingObjectPosition.field_72313_a == MovingObjectType.BLOCK && movingObjectPosition.func_178782_a() != null ? mc.field_71476_x : null;
    }

    private int getSlot(Block block, int meta) {
        if (this.removeMeta(block)) {
            meta = 0;
        }
        if (block == Blocks.field_150362_t || block == Blocks.field_150361_u) {
            meta &= 3;
        }
        for (int i = 0; i < 9; i++) {
            ItemStack itemStack = mc.field_71439_g.field_71071_by.field_70462_a[i];
            if (itemStack != null && itemStack.func_77973_b() instanceof ItemBlock && itemStack.field_77994_a > 0) {
                Block invBlock = ((ItemBlock) itemStack.func_77973_b()).func_179223_d();
                int invMeta = itemStack.func_77952_i();
                if (this.removeMeta(block)) {
                    invMeta = 0;
                }
                if (invBlock == Blocks.field_150362_t || invBlock == Blocks.field_150361_u) {
                    invMeta &= 3;
                }
                if (invBlock == block && invMeta == meta) {
                    return i;
                }
            }
        }
        return -1;
    }

    private boolean removeMeta(Block block) {
        return block instanceof BlockStairs || block instanceof BlockDoublePlant || block instanceof BlockFlower || block instanceof BlockSkull || block instanceof BlockLadder || block instanceof BlockPumpkin || block instanceof BlockCauldron || block instanceof BlockRail || block instanceof BlockRailBase || block instanceof BlockTripWireHook || block instanceof BlockTripWire || block instanceof BlockDispenser || block instanceof BlockDropper || block instanceof BlockHopper || block instanceof BlockTorch || block instanceof BlockButton || block instanceof BlockLever || block instanceof BlockTrapDoor || block instanceof BlockSlab;
    }

    private boolean correctPlaceState(IBlockState requiredState, BlockPos blockPos, EnumFacing enumFacing, Vec3 hitVec, ItemStack heldItem) {
        if (requiredState == null || blockPos == null || enumFacing == null || hitVec == null || heldItem == null || !(heldItem.func_77973_b() instanceof ItemBlock)) {
            return false;
        } else if (!(requiredState.func_177230_c() instanceof BlockLeaves) && !(requiredState.func_177230_c() instanceof BlockButton)) {
            ItemBlock itemBlock = (ItemBlock) heldItem.func_77973_b();
            Block block = itemBlock.func_179223_d();
            int meta = heldItem.func_77952_i();
            Vec3 relativeHitVec = hitVec.func_178788_d(new Vec3(blockPos.func_177958_n(), blockPos.func_177956_o(), blockPos.func_177952_p()));
            IBlockState simulatedState = block.func_180642_a(mc.field_71441_e, blockPos, enumFacing, (float) relativeHitVec.field_72450_a, (float) relativeHitVec.field_72448_b, (float) relativeHitVec.field_72449_c, meta, mc.field_71439_g);
            if (simulatedState == null) {
                return false;
            } else if (simulatedState.func_177230_c() != requiredState.func_177230_c()) {
                return false;
            } else {
                int simulatedMeta = simulatedState.func_177230_c().func_176201_c(simulatedState);
                int requiredMeta = requiredState.func_177230_c().func_176201_c(requiredState);
                if (simulatedMeta != requiredMeta) {
                    return false;
                } else {
                    if (simulatedState.func_177228_b().containsKey(BlockDirectional.field_176387_N) && requiredState.func_177228_b().containsKey(BlockDirectional.field_176387_N)) {
                        EnumFacing simulatedFacing = (EnumFacing) simulatedState.func_177229_b(BlockDirectional.field_176387_N);
                        EnumFacing requiredFacing = (EnumFacing) requiredState.func_177229_b(BlockDirectional.field_176387_N);
                        if (simulatedFacing != requiredFacing) {
                            return false;
                        }
                    }
                    if (simulatedState.func_177230_c() instanceof BlockStairs && requiredState.func_177230_c() instanceof BlockStairs) {
                        EnumFacing simulatedFacing = (EnumFacing) simulatedState.func_177229_b(BlockStairs.field_176309_a);
                        EnumFacing requiredFacing = (EnumFacing) requiredState.func_177229_b(BlockStairs.field_176309_a);
                        EnumHalf simulatedHalf = (EnumHalf) simulatedState.func_177229_b(BlockStairs.field_176308_b);
                        EnumHalf requiredHalf = (EnumHalf) requiredState.func_177229_b(BlockStairs.field_176308_b);
                        if (simulatedFacing != requiredFacing || simulatedHalf != requiredHalf) {
                            return false;
                        }
                    }
                    return true;
                }
            }
        } else {
            return true;
        }
    }

    class BuildBlockInfo {

        public IBlockState requiredState;

        public boolean isPlaced;

        public BuildBlockInfo(IBlockState state) {
            this.requiredState = state;
            this.isPlaced = false;
        }
    }
}
