package keystrokesmod.module.impl.player;

import com.google.common.base.Predicates;
import java.util.List;
import keystrokesmod.mixin.impl.accessor.IAccessorEntityRenderer;
import keystrokesmod.module.Module;
import keystrokesmod.module.impl.world.AntiBot;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.GroupSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.BlockUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBed;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemBucket;
import net.minecraft.item.ItemFlintAndSteel;
import net.minecraft.item.ItemHoe;
import net.minecraft.item.ItemShears;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.item.ItemTool;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EntitySelectors;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import org.lwjgl.input.Mouse;

public class GhostHand extends Module {

    private final SliderSetting transparency;

    private GroupSetting interactGroup;

    private ButtonSetting throughNonPlayer;

    private ButtonSetting throughBots;

    private ButtonSetting throughFriendlies;

    private ButtonSetting throughEnemies;

    private GroupSetting priorityGroup;

    private ButtonSetting priorityEverything;

    private ButtonSetting priorityBed;

    private ButtonSetting priorityBedAdjacent;

    private GroupSetting useGroup;

    private GroupSetting conditionsGroup;

    private ButtonSetting useSword;

    private ButtonSetting useTool;

    private ButtonSetting useFists;

    private ButtonSetting useBucket;

    private ButtonSetting useFlintSteel;

    private ButtonSetting useCobweb;

    private ButtonSetting useOther;

    private ButtonSetting requireLmb;

    private ButtonSetting requireRmb;

    private ButtonSetting notSword;

    public GhostHand() {
        super("Ghost Hand", Module.category.player);
        this.liteModule = true;
        this.registerSetting(this.transparency = new SliderSetting("Transparency", "%", 0.0, 0.0, 100.0, 1.0));
        this.registerSetting(this.interactGroup = new GroupSetting("Interact through"));
        this.registerSetting(this.throughNonPlayer = new ButtonSetting(this.interactGroup, "Non-player entities", true));
        this.registerSetting(this.throughBots = new ButtonSetting(this.interactGroup, "Bots", false));
        this.registerSetting(this.throughFriendlies = new ButtonSetting(this.interactGroup, "Friendlies", false));
        this.registerSetting(this.throughEnemies = new ButtonSetting(this.interactGroup, "Enemies", true));
        this.registerSetting(this.priorityGroup = new GroupSetting("Preferred targets"));
        this.registerSetting(this.priorityEverything = new ButtonSetting(this.priorityGroup, "Everything", true));
        this.registerSetting(this.priorityBed = new ButtonSetting(this.priorityGroup, "Bed", false));
        this.registerSetting(this.priorityBedAdjacent = new ButtonSetting(this.priorityGroup, "Next to bed", false));
        this.registerSetting(this.useGroup = new GroupSetting("Allow while using"));
        this.registerSetting(this.useSword = new ButtonSetting(this.useGroup, "Sword", false));
        this.registerSetting(this.useTool = new ButtonSetting(this.useGroup, "Tool", true));
        this.registerSetting(this.useFists = new ButtonSetting(this.useGroup, "Fists", true));
        this.registerSetting(this.useBucket = new ButtonSetting(this.useGroup, "Bucket", true));
        this.registerSetting(this.useFlintSteel = new ButtonSetting(this.useGroup, "Flint and steel", true));
        this.registerSetting(this.useCobweb = new ButtonSetting(this.useGroup, "Cobweb", true));
        this.registerSetting(this.useOther = new ButtonSetting(this.useGroup, "Other", true));
        this.registerSetting(this.conditionsGroup = new GroupSetting("Conditions"));
        this.registerSetting(this.requireLmb = new ButtonSetting(this.conditionsGroup, "Require Left mouse", false));
        this.registerSetting(this.requireRmb = new ButtonSetting(this.conditionsGroup, "Require right mouse", false));
        this.registerSetting(this.notSword = new ButtonSetting(this.conditionsGroup, "Not holding a sword", false));
    }

    public boolean shouldOverrideMouseOver() {
        if (!this.isEnabled()) {
            return false;
        } else if (mc == null || mc.field_71441_e == null || mc.field_71439_g == null) {
            return false;
        } else if (mc.func_175606_aa() == null) {
            return false;
        } else if (this.notSword.isToggled() && Utils.holdingSword()) {
            return false;
        } else if (this.requireLmb.isToggled() && !Mouse.isButtonDown(0)) {
            return false;
        } else {
            return this.requireRmb.isToggled() && !Mouse.isButtonDown(1) ? false : this.heldItemAllowed();
        }
    }

    public void modifyMouseOverFromGetMouseOver(float partialTicks) {
        if (this.shouldOverrideMouseOver()) {
            Entity viewEntity = mc.func_175606_aa();
            double reach = mc.field_71442_b.func_78757_d();
            if (mc.field_71442_b.func_78749_i()) {
                reach = 6.0;
            }
            MovingObjectPosition blockHit = this.findPrioritizedBlock(viewEntity, reach, partialTicks);
            if (blockHit != null) {
                boolean priorityOverride = this.isPriorityTarget(blockHit);
                if (this.priorityEverything.isToggled() || priorityOverride) {
                    if (!priorityOverride) {
                        Vec3 eyes = viewEntity.func_174824_e(partialTicks);
                        Vec3 blockHitVec = blockHit.field_72307_f;
                        double blockDist = eyes.func_72438_d(blockHitVec);
                        AxisAlignedBB axisAlignedBB = new AxisAlignedBB(Math.min(eyes.field_72450_a, blockHitVec.field_72450_a) - 1.0, Math.min(eyes.field_72448_b, blockHitVec.field_72448_b) - 1.0, Math.min(eyes.field_72449_c, blockHitVec.field_72449_c) - 1.0, Math.max(eyes.field_72450_a, blockHitVec.field_72450_a) + 1.0, Math.max(eyes.field_72448_b, blockHitVec.field_72448_b) + 1.0, Math.max(eyes.field_72449_c, blockHitVec.field_72449_c) + 1.0);
                        List<Entity> candidates = mc.field_71441_e.func_175674_a(viewEntity, axisAlignedBB, Predicates.and(EntitySelectors.field_180132_d, Entity::func_70067_L));
                        Entity closest = null;
                        double closestDist = Double.MAX_VALUE;
                        for (Entity e : candidates) {
                            if (e != viewEntity) {
                                float cb = e.func_70111_Y();
                                AxisAlignedBB bb = e.func_174813_aQ().func_72314_b(cb, cb, cb);
                                MovingObjectPosition intercept = bb.func_72327_a(eyes, blockHitVec);
                                boolean inside = bb.func_72318_a(eyes);
                                if (inside || intercept != null) {
                                    double dist = inside ? 0.0 : eyes.func_72438_d(intercept.field_72307_f);
                                    if (!(dist >= blockDist) && (e != viewEntity.field_70154_o || viewEntity.canRiderInteract()) && dist < closestDist) {
                                        closestDist = dist;
                                        closest = e;
                                    }
                                }
                            }
                        }
                        if (closest == null || !this.obstructionAllowed(closest)) {
                            return;
                        }
                    }
                    mc.field_71476_x = blockHit;
                    mc.field_147125_j = null;
                    EntityRenderer renderer = mc.field_71460_t;
                    if (renderer instanceof IAccessorEntityRenderer) {
                        ((IAccessorEntityRenderer) renderer).setPointedEntity(null);
                    }
                }
            }
        }
    }

    public float getRenderOpacity(Entity entity, float partialTicks) {
        double amount = this.transparency.getInput();
        if (!(amount <= 0.0) && entity instanceof EntityPlayer && this.shouldOverrideMouseOver()) {
            Entity viewEntity = mc.func_175606_aa();
            if (entity == mc.field_71439_g || entity == viewEntity || ((EntityPlayer) entity).func_175149_v() || !entity.func_70067_L()) {
                return 1.0F;
            } else if (entity == viewEntity.field_70154_o && !viewEntity.canRiderInteract()) {
                return 1.0F;
            } else {
                if (!this.priorityEverything.isToggled() || !this.obstructionAllowed(entity)) {
                    if (!this.priorityBed.isToggled() && !this.priorityBedAdjacent.isToggled()) {
                        return 1.0F;
                    }
                    double reach = mc.field_71442_b.func_78749_i() ? 6.0 : mc.field_71442_b.func_78757_d();
                    if (!this.isPriorityTarget(this.findPrioritizedBlock(viewEntity, reach, partialTicks))) {
                        return 1.0F;
                    }
                }
                return (float) (1.0 - Math.min(100.0, amount) / 100.0);
            }
        } else {
            return 1.0F;
        }
    }

    private boolean isPriorityTarget(MovingObjectPosition blockHit) {
        if (blockHit == null) {
            return false;
        } else {
            BlockPos hitPos = blockHit.func_178782_a();
            Block hitBlock = BlockUtils.getBlock(hitPos);
            boolean isBed = hitBlock instanceof BlockBed;
            return this.priorityBed.isToggled() && isBed || this.priorityBedAdjacent.isToggled() && !isBed && BlockUtils.isAdjacentToBed(hitPos);
        }
    }

    private MovingObjectPosition findPrioritizedBlock(Entity viewEntity, double reach, float partialTicks) {
        Vec3 eyes = viewEntity.func_174824_e(partialTicks);
        Vec3 look = viewEntity.func_70676_i(partialTicks);
        Vec3 rayEnd = eyes.func_72441_c(look.field_72450_a * reach, look.field_72448_b * reach, look.field_72449_c * reach);
        return BlockUtils.traverseBlocksAlongRay(eyes, rayEnd, this.priorityBed.isToggled(), this.priorityBedAdjacent.isToggled());
    }

    private boolean heldItemAllowed() {
        ItemStack held = mc.field_71439_g.func_70694_bm();
        switch(classify(held)) {
            case SWORD:
                return this.useSword.isToggled();
            case TOOL:
                return this.useTool.isToggled();
            case FISTS:
                return this.useFists.isToggled();
            case BUCKET:
                return this.useBucket.isToggled();
            case FLINT_STEEL:
                return this.useFlintSteel.isToggled();
            case COBWEB:
                return this.useCobweb.isToggled();
            default:
                return this.useOther.isToggled();
        }
    }

    private boolean obstructionAllowed(Entity e) {
        if (!(e instanceof EntityPlayer)) {
            return this.throughNonPlayer.isToggled();
        } else {
            EntityPlayer player = (EntityPlayer) e;
            if (AntiBot.isBot(player)) {
                return this.throughBots.isToggled();
            } else {
                return !Utils.isFriended(player) && !Utils.isTeammate(player) ? this.throughEnemies.isToggled() : this.throughFriendlies.isToggled();
            }
        }
    }

    private static GhostHand.HeldCategory classify(ItemStack held) {
        if (held == null) {
            return GhostHand.HeldCategory.FISTS;
        } else {
            Item item = held.func_77973_b();
            if (item instanceof ItemSword) {
                return GhostHand.HeldCategory.SWORD;
            } else if (item instanceof ItemTool || item instanceof ItemHoe || item instanceof ItemShears) {
                return GhostHand.HeldCategory.TOOL;
            } else if (item instanceof ItemBucket) {
                return GhostHand.HeldCategory.BUCKET;
            } else if (item instanceof ItemFlintAndSteel) {
                return GhostHand.HeldCategory.FLINT_STEEL;
            } else {
                return item instanceof ItemBlock && ((ItemBlock) item).func_179223_d() == Blocks.field_150321_G ? GhostHand.HeldCategory.COBWEB : GhostHand.HeldCategory.OTHER;
            }
        }
    }

    private static enum HeldCategory {

        SWORD,
        TOOL,
        FISTS,
        BUCKET,
        FLINT_STEEL,
        COBWEB,
        OTHER
    }
}
