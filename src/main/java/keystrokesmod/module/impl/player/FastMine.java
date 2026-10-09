package keystrokesmod.module.impl.player;

import keystrokesmod.mixin.impl.accessor.IAccessorPlayerControllerMP;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.BlockListSetting;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.DescriptionSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.Utils;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemShears;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.util.ResourceLocation;

public class FastMine extends Module {

    private SliderSetting delay;

    public SliderSetting multiplier;

    private ButtonSetting creativeDisable;

    private ButtonSetting decreaseBreakDelay;

    private ButtonSetting ignoreShears;

    private final ButtonSetting blockBlacklistToggle;

    private final BlockListSetting blockBlacklist;

    private IBlockState blockHitDelayState;

    private int passiveBlockHitLastSeen;

    public FastMine() {
        super("Fast Mine", Module.category.player);
        this.liteModule = true;
        this.registerSetting(new DescriptionSetting("Vanilla is 250ms delay & 1x speed."));
        this.registerSetting(this.delay = new SliderSetting("Break delay", "ms", 250.0, 0.0, 250.0, 50.0));
        this.registerSetting(this.multiplier = new SliderSetting("Break speed", "x", 1.0, 1.0, 2.0, 0.02));
        this.registerSetting(this.creativeDisable = new ButtonSetting("Disable in creative", true));
        this.registerSetting(this.decreaseBreakDelay = new ButtonSetting("Decrease break delay", false));
        this.registerSetting(this.ignoreShears = new ButtonSetting("Ignore shears", false));
        this.registerSetting(this.blockBlacklistToggle = new ButtonSetting("Block blacklist", false));
        this.registerSetting(this.blockBlacklist = new BlockListSetting("Blacklisted blocks"));
        this.closetModule = true;
    }

    @Override
    public void guiUpdate() {
        this.blockBlacklist.setVisible(this.blockBlacklistToggle.isToggled(), this);
    }

    private boolean isBlacklisted(IBlockState state) {
        if (this.blockBlacklistToggle.isToggled() && state != null) {
            Block block = state.func_177230_c();
            ResourceLocation registryName = (ResourceLocation) Block.field_149771_c.func_177774_c(block);
            if (registryName == null) {
                return false;
            } else {
                String registryId = registryName.toString();
                int meta = block.func_176201_c(state);
                String storageId = meta != 0 ? registryId + ":" + meta : registryId;
                return this.blockBlacklist.contains(storageId) || this.blockBlacklist.contains(registryId);
            }
        } else {
            return false;
        }
    }

    private boolean isTargetBlacklisted() {
        return mc.field_71441_e != null && mc.field_71476_x != null && mc.field_71476_x.field_72313_a == MovingObjectType.BLOCK && this.isBlacklisted(mc.field_71441_e.func_180495_p(mc.field_71476_x.func_178782_a()));
    }

    public float getBreakSpeedMultiplier(IBlockState state) {
        if (!this.isEnabled() || this.isBlacklisted(state)) {
            return 1.0F;
        } else if (this.creativeDisable.isToggled() && mc.field_71439_g != null && mc.field_71439_g.field_71075_bZ.field_75098_d) {
            return 1.0F;
        } else if (this.ignoreShears.isToggled() && mc.field_71439_g != null && mc.field_71439_g.func_70694_bm() != null && mc.field_71439_g.func_70694_bm().func_77973_b() instanceof ItemShears) {
            return 1.0F;
        } else {
            float m = (float) this.multiplier.getInput();
            return m > 1.0F ? m : 1.0F;
        }
    }

    public boolean shouldPassiveDecreaseBlockHitDelay() {
        return this.isEnabled() && this.decreaseBreakDelay.isToggled() && Utils.nullCheck() && !this.isBlacklisted(this.blockHitDelayState) && !this.isTargetBlacklisted();
    }

    public void tickPassiveBlockHitDecay(Minecraft mc) {
        if (mc.field_71442_b != null) {
            IAccessorPlayerControllerMP acc = (IAccessorPlayerControllerMP) mc.field_71442_b;
            int delay = acc.getBlockHitDelay();
            if (delay <= 0) {
                this.blockHitDelayState = null;
            }
            if (!this.shouldPassiveDecreaseBlockHitDelay()) {
                this.passiveBlockHitLastSeen = delay;
            } else {
                if (delay > 0 && delay == this.passiveBlockHitLastSeen) {
                    acc.setBlockHitDelay(delay - 1);
                }
                this.passiveBlockHitLastSeen = delay;
            }
        }
    }

    public int getBlockHitDelayOverrideOrMinusOne(IBlockState state) {
        this.blockHitDelayState = state;
        if (!this.isEnabled() || !Utils.nullCheck() || !mc.field_71415_G || this.isBlacklisted(state)) {
            return -1;
        } else if (this.creativeDisable.isToggled() && mc.field_71439_g != null && mc.field_71439_g.field_71075_bZ.field_75098_d) {
            return -1;
        } else {
            int ticks = (int) (this.delay.getInput() / 50.0);
            return ticks >= 5 ? -1 : ticks;
        }
    }

    @Override
    public String getInfo() {
        return ((int) this.multiplier.getInput() == this.multiplier.getInput() ? (int) this.multiplier.getInput() + "" : this.multiplier.getInput()) + this.multiplier.getSuffix();
    }
}
