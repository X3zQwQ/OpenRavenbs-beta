package keystrokesmod.mixin.impl.entity;

import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.combat.Reduce;
import keystrokesmod.module.impl.movement.KeepSprint;
import keystrokesmod.utility.BlockAnimationUtils;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.EnumCreatureAttribute;
import net.minecraft.entity.IEntityMultiPart;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.boss.EntityDragonPart;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.potion.Potion;
import net.minecraft.stats.AchievementList;
import net.minecraft.stats.StatBase;
import net.minecraft.stats.StatList;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityPlayer.class)
public abstract class MixinEntityPlayer extends EntityLivingBase {

    public MixinEntityPlayer(World world) {
        super(world);
    }

    @Shadow
    public abstract ItemStack func_70694_bm();

    @Shadow
    public abstract void func_71009_b(Entity entity2);

    @Shadow
    public abstract void func_71047_c(Entity entity3);

    @Shadow
    public abstract void func_71029_a(StatBase statBase);

    @Shadow
    public abstract ItemStack func_71045_bC();

    @Shadow
    public abstract void func_71028_bD();

    @Shadow
    public abstract void func_71064_a(StatBase statBase2, int intValue);

    @Shadow
    public abstract void func_71020_j(float floatValue);

    @Overwrite
    public void func_71059_n(Entity targetEntity) {
        if (ForgeHooks.onPlayerAttackTarget((EntityPlayer) (Object) this, targetEntity) && targetEntity.func_70075_an() && !targetEntity.func_85031_j(this)) {
            float f = (float) this.func_110148_a(SharedMonsterAttributes.field_111264_e).func_111126_e();
            int i = 0;
            float f1 = 0.0F;
            if (targetEntity instanceof EntityLivingBase) {
                f1 = EnchantmentHelper.func_152377_a(this.func_70694_bm(), ((EntityLivingBase) targetEntity).func_70668_bt());
            } else {
                f1 = EnchantmentHelper.func_152377_a(this.func_70694_bm(), EnumCreatureAttribute.UNDEFINED);
            }
            i += EnchantmentHelper.func_77501_a(this);
            if (this.func_70051_ag()) {
                i++;
            }
            if (f > 0.0F || f1 > 0.0F) {
                boolean flag = this.field_70143_R > 0.0F && !this.field_70122_E && !this.func_70617_f_() && !this.func_70090_H() && !this.func_70644_a(Potion.field_76440_q) && this.field_70154_o == null && targetEntity instanceof EntityLivingBase;
                if (flag && f > 0.0F) {
                    f *= 1.5F;
                }
                f += f1;
                boolean flag1 = false;
                int j = EnchantmentHelper.func_90036_a(this);
                if (targetEntity instanceof EntityLivingBase && j > 0 && !targetEntity.func_70027_ad()) {
                    flag1 = true;
                    targetEntity.func_70015_d(1);
                }
                double d0 = targetEntity.field_70159_w;
                double d1 = targetEntity.field_70181_x;
                double d2 = targetEntity.field_70179_y;
                boolean flag2 = targetEntity.func_70097_a(DamageSource.func_76365_a((EntityPlayer) (Object) this), f);
                if (flag2) {
                    if (i > 0) {
                        targetEntity.func_70024_g(-MathHelper.func_76126_a(this.field_70177_z * (float) Math.PI / 180.0F) * i * 0.5F, 0.1, MathHelper.func_76134_b(this.field_70177_z * (float) Math.PI / 180.0F) * i * 0.5F);
                        if (ModuleManager.reduce != null && ModuleManager.reduce.isEnabled()) {
                            Reduce.reduce(targetEntity);
                        } else if (ModuleManager.keepSprint != null && ModuleManager.keepSprint.isEnabled()) {
                            KeepSprint.keepSprint(targetEntity);
                        } else {
                            this.field_70159_w *= 0.6;
                            this.field_70179_y *= 0.6;
                            this.func_70031_b(false);
                        }
                    }
                    if (targetEntity instanceof EntityPlayerMP && targetEntity.field_70133_I) {
                        ((EntityPlayerMP) targetEntity).field_71135_a.func_147359_a(new S12PacketEntityVelocity(targetEntity));
                        targetEntity.field_70133_I = false;
                        targetEntity.field_70159_w = d0;
                        targetEntity.field_70181_x = d1;
                        targetEntity.field_70179_y = d2;
                    }
                    if (flag) {
                        this.func_71009_b(targetEntity);
                    }
                    if (f1 > 0.0F) {
                        this.func_71047_c(targetEntity);
                    }
                    if (f >= 18.0F) {
                        this.func_71029_a(AchievementList.field_75999_E);
                    }
                    this.func_130011_c(targetEntity);
                    if (targetEntity instanceof EntityLivingBase) {
                        EnchantmentHelper.func_151384_a((EntityLivingBase) targetEntity, this);
                    }
                    EnchantmentHelper.func_151385_b(this, targetEntity);
                    ItemStack itemstack = this.func_71045_bC();
                    Entity entity = targetEntity;
                    if (targetEntity instanceof EntityDragonPart) {
                        IEntityMultiPart ientitymultipart = ((EntityDragonPart) targetEntity).field_70259_a;
                        if (ientitymultipart instanceof EntityLivingBase) {
                            entity = (EntityLivingBase) ientitymultipart;
                        }
                    }
                    if (itemstack != null && entity instanceof EntityLivingBase) {
                        itemstack.func_77961_a((EntityLivingBase) entity, (EntityPlayer) (Object) this);
                        if (itemstack.field_77994_a <= 0) {
                            this.func_71028_bD();
                        }
                    }
                    if (targetEntity instanceof EntityLivingBase) {
                        this.func_71064_a(StatList.field_75951_w, Math.round(f * 10.0F));
                        if (j > 0) {
                            targetEntity.func_70015_d(j * 4);
                        }
                    }
                    this.func_71020_j(0.3F);
                } else if (flag1) {
                    targetEntity.func_70066_B();
                }
            }
        }
    }

    @Inject(method = "isBlocking", at = @At("RETURN"), cancellable = true)
    private void isBlocking(CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue() && BlockAnimationUtils.shouldForceBlockAnimation((EntityPlayer) (Object) this, this.func_70694_bm())) {
            cir.setReturnValue(true);
        }
    }
}
