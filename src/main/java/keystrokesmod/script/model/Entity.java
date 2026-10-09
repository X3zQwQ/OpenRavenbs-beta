package keystrokesmod.script.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import keystrokesmod.utility.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityFishHook;
import net.minecraft.item.ItemBlock;
import net.minecraft.potion.PotionEffect;

public class Entity {

    public net.minecraft.entity.Entity entity;

    public String type;

    public int entityId;

    public boolean isLiving;

    public boolean isPlayer;

    public boolean isUser;

    private static HashMap<Integer, Entity> cache = new HashMap<>();

    public Entity(net.minecraft.entity.Entity entity) {
        this.entity = entity;
        if (entity != null) {
            this.type = entity.getClass().getSimpleName();
            this.entityId = entity.func_145782_y();
            this.isLiving = entity instanceof EntityLivingBase;
            this.isPlayer = entity instanceof EntityPlayer;
            if (this.isPlayer && Minecraft.func_71410_x().field_71439_g != null && entity.func_110124_au().equals(Minecraft.func_71410_x().field_71439_g.func_110124_au())) {
                this.isUser = true;
            }
        }
    }

    public static Entity convert(net.minecraft.entity.Entity entity) {
        if (entity == null) {
            return null;
        } else {
            int id = entity.func_145782_y() + System.identityHashCode(entity);
            Entity cachedEntity = cache.get(id);
            if (cachedEntity == null) {
                cachedEntity = new Entity(entity);
                cache.put(id, cachedEntity);
            }
            return cachedEntity;
        }
    }

    public static void clearCache() {
        cache.clear();
    }

    public boolean allowEditing() {
        return !(this.entity instanceof EntityPlayer) ? false : ((EntityPlayer) this.entity).field_71075_bZ.field_75099_e;
    }

    public double distanceTo(Vec3 position) {
        return this.entity.func_70011_f(position.x, position.y, position.z);
    }

    public double distanceToSq(Vec3 position) {
        return this.entity.func_70092_e(position.x, position.y, position.z);
    }

    public double distanceToGround() {
        return Utils.distanceToGround(this.entity);
    }

    public boolean isHoldingBlock() {
        return this.isLiving && ((EntityLivingBase) this.entity).func_70694_bm() != null && ((EntityLivingBase) this.entity).func_70694_bm().func_77973_b() instanceof ItemBlock;
    }

    public boolean isHoldingWeapon() {
        return this.isLiving && Utils.holdingWeapon((EntityLivingBase) this.entity);
    }

    public float getAbsorption() {
        return !(this.entity instanceof EntityLivingBase) ? -1.0F : ((EntityLivingBase) this.entity).func_110139_bj();
    }

    public Vec3 getBlockPosition() {
        return new Vec3(this.entity.func_180425_c().func_177958_n(), this.entity.func_180425_c().func_177956_o(), this.entity.func_180425_c().func_177952_p());
    }

    public String getDisplayName() {
        return this.entity instanceof EntityItem ? ((EntityItem) this.entity).func_92059_d().func_82833_r() : this.entity.func_145748_c_().func_150260_c();
    }

    public Entity getRidingEntity() {
        return convert(this.entity.field_70154_o);
    }

    public Entity getRiddenByEntity() {
        return convert(this.entity.field_70153_n);
    }

    public Vec3 getServerPosition() {
        return new Vec3(this.entity.field_70118_ct, this.entity.field_70117_cu, this.entity.field_70116_cv);
    }

    public int getExperienceLevel() {
        return !(this.entity instanceof EntityPlayer) ? 0 : ((EntityPlayer) this.entity).field_71068_ca;
    }

    public float getExperience() {
        return !(this.entity instanceof EntityPlayer) ? 0.0F : ((EntityPlayer) this.entity).field_71106_cc;
    }

    public float getFallDistance() {
        return this.entity.field_70143_R;
    }

    public String getUUID() {
        return this.entity.func_110124_au().toString();
    }

    public String getCustomNameTag() {
        return this.entity.func_95999_t();
    }

    public double getBPS() {
        if (!this.isLiving) {
            return 0.0;
        } else {
            double x = this.entity.field_70165_t - this.entity.field_70169_q;
            double z = this.entity.field_70161_v - this.entity.field_70166_s;
            return Math.sqrt(x * x + z * z) * 20.0;
        }
    }

    public String getFacing() {
        return this.entity.func_174811_aO().name();
    }

    public float getHealth() {
        return !(this.entity instanceof EntityLivingBase) ? -1.0F : ((EntityLivingBase) this.entity).func_110143_aJ();
    }

    public boolean isSleeping() {
        return this.isPlayer ? ((EntityPlayer) this.entity).func_70608_bn() : false;
    }

    public float getEyeHeight() {
        return this.entity.func_70047_e();
    }

    public float getHeight() {
        return this.entity.field_70131_O;
    }

    public float getWidth() {
        return this.entity.field_70130_N;
    }

    public boolean isBurning() {
        return this.entity.func_70027_ad();
    }

    public int getStackSize() {
        if (this.entity instanceof EntityItem) {
            net.minecraft.item.ItemStack item = ((EntityItem) this.entity).func_92059_d();
            return item == null ? -1 : item.field_77994_a;
        } else if (!(this.entity instanceof EntityLivingBase)) {
            return -1;
        } else {
            net.minecraft.item.ItemStack stack = ((EntityLivingBase) this.entity).func_70694_bm();
            return stack == null ? -1 : stack.field_77994_a;
        }
    }

    public ItemStack getHeldItem() {
        if (this.entity instanceof EntityItem) {
            net.minecraft.item.ItemStack item = ((EntityItem) this.entity).func_92059_d();
            return item == null ? null : new ItemStack(item, (byte) 0);
        } else if (!(this.entity instanceof EntityLivingBase)) {
            return null;
        } else {
            net.minecraft.item.ItemStack stack = ((EntityLivingBase) this.entity).func_70694_bm();
            return stack == null ? null : new ItemStack(stack, (byte) 0);
        }
    }

    public int getHurtTime() {
        return !(this.entity instanceof EntityLivingBase) ? -1 : ((EntityLivingBase) this.entity).field_70737_aN;
    }

    public boolean isConsuming() {
        return Utils.isConsuming(this.entity);
    }

    public Vec3 getLastPosition() {
        return new Vec3(this.entity.field_70142_S, this.entity.field_70137_T, this.entity.field_70136_U);
    }

    public float getMaxHealth() {
        return !(this.entity instanceof EntityLivingBase) ? -1.0F : ((EntityLivingBase) this.entity).func_110138_aP();
    }

    public int getMaxHurtTime() {
        return !(this.entity instanceof EntityLivingBase) ? -1 : ((EntityLivingBase) this.entity).field_70738_aO;
    }

    public String getName() {
        return this.entity instanceof EntityItem ? ((EntityItem) this.entity).func_92059_d().func_77973_b().getRegistryName().substring(10) : this.entity.func_70005_c_();
    }

    public NetworkPlayer getNetworkPlayer() {
        return NetworkPlayer.convert(Minecraft.func_71410_x().func_147114_u().func_175102_a(this.entity.func_110124_au()));
    }

    public float getPitch() {
        return this.entity.field_70125_A;
    }

    public Vec3 getPosition() {
        return this.entity == null ? null : new Vec3(this.entity.field_70165_t, this.entity.field_70163_u, this.entity.field_70161_v);
    }

    public List<Object[]> getPotionEffects() {
        List<Object[]> potionEffects = new ArrayList<>();
        if (!(this.entity instanceof EntityLivingBase)) {
            return potionEffects;
        } else {
            for (PotionEffect potionEffect : ((EntityLivingBase) this.entity).func_70651_bq()) {
                Object[] potionData = new Object[] { potionEffect.func_76456_a(), potionEffect.func_76453_d(), potionEffect.func_76458_c(), potionEffect.func_76459_b() };
                potionEffects.add(potionData);
            }
            return potionEffects;
        }
    }

    public ItemStack getArmorInSlot(int slot) {
        return this.isPlayer && slot >= 0 && slot <= 3 ? ItemStack.convert(((EntityPlayer) this.entity).field_71071_by.field_70460_b[slot]) : null;
    }

    public double getSpeed() {
        return Utils.getHorizontalSpeed(this.entity);
    }

    public int getSwingProgress() {
        return this.isLiving ? ((EntityLivingBase) this.entity).field_110158_av : -1;
    }

    public float getPrevSwingProgress() {
        return this.isLiving ? ((EntityLivingBase) this.entity).field_70732_aI : -1.0F;
    }

    public int getTicksExisted() {
        return this.entity.field_70173_aa;
    }

    public float getYaw() {
        return this.entity.field_70177_z;
    }

    public int getFireResistance() {
        return this.entity.field_70174_ab;
    }

    public float getPrevYaw() {
        return this.entity.field_70126_B;
    }

    public void setPrevYaw(float prevYaw) {
        this.entity.field_70126_B = prevYaw;
    }

    public void setPrevPitch(float prevPitch) {
        this.entity.field_70127_C = prevPitch;
    }

    public float getPrevPitch() {
        return this.entity.field_70127_C;
    }

    public boolean isCreative() {
        return !(this.entity instanceof EntityPlayer) ? false : ((EntityPlayer) this.entity).field_71075_bZ.field_75098_d;
    }

    public boolean isCollided() {
        return !(this.entity instanceof EntityPlayer) ? Minecraft.func_71410_x().field_71441_e.func_72829_c(this.entity.func_174813_aQ().func_72314_b(0.05, 0.0, 0.05)) : this.entity.field_70132_H;
    }

    public boolean isCollidedHorizontally() {
        return this.entity.field_70123_F;
    }

    public boolean isCollidedVertically() {
        return this.entity.field_70124_G;
    }

    public boolean isDead() {
        return this.entity.field_70128_L || this.isLiving && ((EntityLivingBase) this.entity).field_70725_aQ > 0;
    }

    public int getHunger() {
        return this.isPlayer && ((EntityPlayer) this.entity).func_71024_bL() != null ? ((EntityPlayer) this.entity).func_71024_bL().func_75116_a() : 0;
    }

    public float getSaturation() {
        return this.isPlayer && ((EntityPlayer) this.entity).func_71024_bL() != null ? ((EntityPlayer) this.entity).func_71024_bL().func_75115_e() : 0.0F;
    }

    public float getAir() {
        return this.entity.func_70086_ai();
    }

    public boolean isInvisible() {
        return this.entity.func_82150_aj();
    }

    public boolean isInWater() {
        return this.entity.func_70090_H();
    }

    public boolean isInLava() {
        return this.entity.func_180799_ab();
    }

    public Entity getFisher() {
        return this.entity instanceof EntityFishHook ? convert(((EntityFishHook) this.entity).field_146042_b) : null;
    }

    public boolean isInLiquid() {
        return !this.entity.func_70038_c(0.0, 0.0, 0.0);
    }

    public boolean isOnLadder() {
        return this.isLiving && ((EntityLivingBase) this.entity).func_70617_f_();
    }

    public boolean isOnEdge() {
        return Utils.onEdge(this.entity);
    }

    public boolean isSprinting() {
        return this.entity.func_70051_ag();
    }

    public boolean isSneaking() {
        return this.entity.func_70093_af();
    }

    public boolean isUsingItem() {
        return !(this.entity instanceof EntityPlayer) ? false : ((EntityPlayer) this.entity).func_71039_bw();
    }

    public boolean onGround() {
        return this.entity.field_70122_E;
    }

    public void setMotion(double x, double y, double z) {
        this.entity.field_70159_w = x;
        this.entity.field_70181_x = y;
        this.entity.field_70179_y = z;
    }

    public Vec3 getMotion() {
        return new Vec3(this.entity.field_70159_w, this.entity.field_70181_x, this.entity.field_70179_y);
    }

    public void setPitch(float pitch) {
        this.entity.field_70125_A = pitch;
    }

    public void setYaw(float yaw) {
        this.entity.field_70177_z = yaw;
    }

    public void moveTo(Vec3 position) {
        if (position != null) {
            this.moveTo(position.x, position.y, position.z);
        }
    }

    public void moveTo(double x, double y, double z) {
        this.entity.func_70080_a(x, y, z, this.entity.field_70177_z, this.entity.field_70125_A);
    }

    public void moveTo(Vec3 position, float yaw, float pitch) {
        if (position != null) {
            this.moveTo(position.x, position.y, position.z, yaw, pitch);
        }
    }

    public void moveTo(double x, double y, double z, float yaw, float pitch) {
        this.entity.func_70080_a(x, y, z, yaw, pitch);
    }

    public void moveTo(Vec3 position, float yaw, float pitch, int interpolationTicks) {
        if (position != null) {
            this.moveTo(position.x, position.y, position.z, yaw, pitch, interpolationTicks);
        }
    }

    public void moveTo(double x, double y, double z, float yaw, float pitch, int interpolationTicks) {
        if (interpolationTicks <= 0) {
            this.moveTo(x, y, z, yaw, pitch);
        } else {
            this.entity.func_180426_a(x, y, z, yaw, pitch, interpolationTicks, false);
        }
    }

    public void setPosition(Vec3 position) {
        this.entity.func_70107_b(position.x, position.y, position.z);
    }

    public void setPosition(double x, double y, double z) {
        this.entity.func_70107_b(x, y, z);
    }
}
