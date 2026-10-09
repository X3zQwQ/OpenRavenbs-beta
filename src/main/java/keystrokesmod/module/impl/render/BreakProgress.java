package keystrokesmod.module.impl.render;

import keystrokesmod.mixin.impl.accessor.IAccessorPlayerControllerMP;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.BlockUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

public class BreakProgress extends Module {

    private SliderSetting mode;

    private ButtonSetting manual;

    private ButtonSetting bedAura;

    private ButtonSetting fadeIn;

    private String[] MODES = new String[] { "Percentage", "Second", "Decimal" };

    private float progress;

    private BlockPos block;

    private String progressStr;

    public BreakProgress() {
        super("BreakProgress", Module.category.render);
        this.liteModule = true;
        this.registerSetting(this.mode = new SliderSetting("Mode", 0, this.MODES));
        this.registerSetting(this.manual = new ButtonSetting("Show manual", true));
        this.registerSetting(this.bedAura = new ButtonSetting("Show BedAura", true));
        this.registerSetting(this.fadeIn = new ButtonSetting("Fade in", false));
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRenderWorld(RenderWorldLastEvent e) {
        if (this.progress != 0.0F && this.block != null && Utils.nullCheck()) {
            double x = this.block.func_177958_n() + 0.5 - mc.func_175598_ae().field_78730_l;
            double y = this.block.func_177956_o() + 0.5 - mc.func_175598_ae().field_78731_m;
            double z = this.block.func_177952_p() + 0.5 - mc.func_175598_ae().field_78728_n;
            GlStateManager.func_179094_E();
            GlStateManager.func_179109_b((float) x, (float) y, (float) z);
            GlStateManager.func_179114_b(-mc.func_175598_ae().field_78735_i, 0.0F, 1.0F, 0.0F);
            GlStateManager.func_179114_b((mc.field_71474_y.field_74320_O == 2 ? -1 : 1) * mc.func_175598_ae().field_78732_j, 1.0F, 0.0F, 0.0F);
            GlStateManager.func_179152_a(-0.02266667F, -0.02266667F, -0.02266667F);
            GlStateManager.func_179132_a(false);
            GlStateManager.func_179097_i();
            GL11.glEnable(3042);
            int colorAlpha = Utils.mergeAlpha(-1, Math.max(10, (int) (255.0F * this.progress)));
            mc.field_71466_p.func_175065_a(this.progressStr, -mc.field_71466_p.func_78256_a(this.progressStr) / 2, -3.0F, this.fadeIn.isToggled() ? colorAlpha : -1, true);
            GL11.glDisable(3042);
            GlStateManager.func_179126_j();
            GlStateManager.func_179132_a(true);
            GlStateManager.func_179131_c(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.func_179121_F();
        }
    }

    private void setProgress() {
        switch((int) this.mode.getInput()) {
            case 0:
                this.progressStr = (int) (100.0 * (this.progress / 1.0)) + "%";
                break;
            case 1:
                double timeLeft = Utils.round((1.0F - this.progress) / BlockUtils.getBlockHardness(BlockUtils.getBlock(this.block), mc.field_71439_g.func_70694_bm(), false, false) / 20.0, 1);
                this.progressStr = timeLeft == 0.0 ? "0" : timeLeft + "s";
                break;
            case 2:
                this.progressStr = String.valueOf(Utils.round(this.progress, 2));
        }
    }

    @Override
    public void onUpdate() {
        if (!mc.field_71439_g.field_71075_bZ.field_75098_d && mc.field_71439_g.field_71075_bZ.field_75099_e) {
            if (this.bedAura.isToggled() && ModuleManager.bedAura != null && ModuleManager.bedAura.isEnabled()) {
                BlockPos ap = ModuleManager.bedAura.getAuraTargetPos();
                float bp = ModuleManager.bedAura.getAuraBreakProgress();
                if (ap != null && bp > 0.0F) {
                    this.progress = Math.min(1.0F, bp);
                    this.block = ap;
                    this.setProgress();
                    return;
                }
            }
            if (this.manual.isToggled() && mc.field_71476_x != null && mc.field_71476_x.field_72313_a == MovingObjectType.BLOCK) {
                this.progress = ((IAccessorPlayerControllerMP) mc.field_71442_b).getCurBlockDamageMP();
                if (this.progress == 0.0F) {
                    this.resetVariables();
                } else {
                    this.block = mc.field_71476_x.func_178782_a();
                    this.setProgress();
                }
            } else {
                this.resetVariables();
            }
        } else {
            this.resetVariables();
        }
    }

    @Override
    public void onDisable() {
        this.resetVariables();
    }

    private void resetVariables() {
        this.progress = 0.0F;
        this.block = null;
        this.progressStr = "";
    }
}
