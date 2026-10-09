package keystrokesmod.mixin.impl.render;

import java.util.Random;
import keystrokesmod.module.impl.render.ItemPhysics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderEntityItem;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SideOnly(Side.CLIENT)
@Mixin(RenderEntityItem.class)
public abstract class MixinRenderEntityItem extends Render<Entity> {

    @Unique
    private static final Random itemPhysics$random = new Random();

    @Unique
    private static long itemPhysics$lastNano = System.nanoTime();

    @Unique
    private static double itemPhysics$rotation;

    protected MixinRenderEntityItem(RenderManager renderManager) {
        super(renderManager);
    }

    @Inject(method = "doRender(Lnet/minecraft/entity/item/EntityItem;DDDFF)V", at = @At("HEAD"), cancellable = true)
    private void itemPhysics$doRender(EntityItem entity, double x, double y, double z, float entityYaw, float partialTicks, CallbackInfo ci) {
        if (ItemPhysics.instance != null) {
            ci.cancel();
            Minecraft mc = Minecraft.func_71410_x();
            RenderEntityItem self = (RenderEntityItem) (Object) this;
            double speed = ItemPhysics.instance.rotationSpeed.getInput();
            itemPhysics$rotation = (System.nanoTime() - itemPhysics$lastNano) / 2500000.0 * speed;
            if (!mc.func_147113_T()) {
                itemPhysics$lastNano = System.nanoTime();
            } else {
                itemPhysics$rotation = 0.0;
            }
            ItemStack stack = entity.func_92059_d();
            if (stack != null && stack.func_77973_b() != null) {
                int seed = Item.func_150891_b(stack.func_77973_b()) + stack.func_77960_j();
                itemPhysics$random.setSeed(seed);
                self.func_110776_a(TextureMap.field_110575_b);
                self.func_177068_d().field_78724_e.func_110581_b(TextureMap.field_110575_b).func_174936_b(false, false);
                GlStateManager.func_179091_B();
                GlStateManager.func_179092_a(516, 0.1F);
                GlStateManager.func_179147_l();
                GlStateManager.func_179120_a(770, 771, 1, 0);
                GlStateManager.func_179094_E();
                IBakedModel model = mc.func_175599_af().func_175037_a().func_178089_a(stack);
                boolean is3D = model.func_177556_c();
                int count = itemPhysics$getModelCount(stack);
                GlStateManager.func_179109_b((float) x, (float) y, (float) z);
                if (is3D) {
                    GlStateManager.func_179152_a(0.5F, 0.5F, 0.5F);
                }
                GL11.glRotatef(90.0F, 1.0F, 0.0F, 0.0F);
                GL11.glRotatef(entity.field_70177_z, 0.0F, 0.0F, 1.0F);
                if (is3D) {
                    GlStateManager.func_179137_b(0.0, 0.0, -0.08);
                } else {
                    GlStateManager.func_179137_b(0.0, 0.0, -0.04);
                }
                if (!entity.field_70122_E) {
                    double rot = itemPhysics$rotation * 2.0;
                    entity.field_70125_A += (float) rot;
                } else if (!is3D) {
                    entity.field_70125_A = 0.0F;
                }
                if (is3D || mc.func_175598_ae().field_78733_k != null) {
                    GlStateManager.func_179114_b(entity.field_70125_A, 1.0F, 0.0F, 0.0F);
                }
                GlStateManager.func_179131_c(1.0F, 1.0F, 1.0F, 1.0F);
                for (int k = 0; k < count; k++) {
                    GlStateManager.func_179094_E();
                    if (is3D) {
                        if (k > 0) {
                            float ox = (itemPhysics$random.nextFloat() * 2.0F - 1.0F) * 0.15F;
                            float oy = (itemPhysics$random.nextFloat() * 2.0F - 1.0F) * 0.15F;
                            float oz = (itemPhysics$random.nextFloat() * 2.0F - 1.0F) * 0.15F;
                            GlStateManager.func_179109_b(self.shouldSpreadItems() ? ox : 0.0F, self.shouldSpreadItems() ? oy : 0.0F, oz);
                        }
                        model = ForgeHooksClient.handleCameraTransforms(model, TransformType.GROUND);
                        mc.func_175599_af().func_180454_a(stack, model);
                        GlStateManager.func_179121_F();
                    } else {
                        model = ForgeHooksClient.handleCameraTransforms(model, TransformType.GROUND);
                        mc.func_175599_af().func_180454_a(stack, model);
                        GlStateManager.func_179121_F();
                        GlStateManager.func_179109_b(0.0F, 0.0F, 0.05375F);
                    }
                }
                GlStateManager.func_179121_F();
                GlStateManager.func_179101_C();
                GlStateManager.func_179084_k();
                self.func_110776_a(TextureMap.field_110575_b);
                self.func_177068_d().field_78724_e.func_110581_b(TextureMap.field_110575_b).func_174935_a();
            }
        }
    }

    @Unique
    private static int itemPhysics$getModelCount(ItemStack stack) {
        if (stack.field_77994_a > 48) {
            return 5;
        } else if (stack.field_77994_a > 32) {
            return 4;
        } else if (stack.field_77994_a > 16) {
            return 3;
        } else {
            return stack.field_77994_a > 1 ? 2 : 1;
        }
    }
}
