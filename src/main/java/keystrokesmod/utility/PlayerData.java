package keystrokesmod.utility;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;

public class PlayerData {

    public double speed;

    public int aboveVoidTicks;

    public int fastTick;

    public int autoBlockTicks;

    public int ticksExisted;

    public int lastSneakTick;

    public double posZ;

    public int sneakTicks;

    public int noSlowTicks;

    public double posY;

    public boolean sneaking;

    public double posX;

    public double serverPosX;

    public double serverPosY;

    public double serverPosZ;

    public void update(EntityPlayer entityPlayer) {
        int ticksExisted = entityPlayer.field_70173_aa;
        this.posX = entityPlayer.field_70165_t - entityPlayer.field_70142_S;
        this.posY = entityPlayer.field_70163_u - entityPlayer.field_70137_T;
        this.posZ = entityPlayer.field_70161_v - entityPlayer.field_70136_U;
        this.speed = Math.max(Math.abs(this.posX), Math.abs(this.posZ));
        if (this.speed >= 0.07) {
            this.fastTick++;
            this.ticksExisted = ticksExisted;
        } else {
            this.fastTick = 0;
        }
        if (Math.abs(this.posY) >= 0.1) {
            this.aboveVoidTicks = ticksExisted;
        }
        if (entityPlayer.func_70093_af()) {
            this.lastSneakTick = ticksExisted;
        }
        if (entityPlayer.field_82175_bq && entityPlayer.func_70632_aY()) {
            this.autoBlockTicks++;
        } else {
            this.autoBlockTicks = 0;
        }
        if (entityPlayer.func_70051_ag() && entityPlayer.func_71039_bw()) {
            this.noSlowTicks++;
        } else {
            this.noSlowTicks = 0;
        }
        if (!(entityPlayer.field_70125_A >= 70.0F) || entityPlayer.func_70694_bm() == null || !(entityPlayer.func_70694_bm().func_77973_b() instanceof ItemBlock)) {
            this.sneakTicks = 0;
        } else if (entityPlayer.field_110158_av == 1) {
            if (!this.sneaking && entityPlayer.func_70093_af()) {
                this.sneakTicks++;
            } else {
                this.sneakTicks = 0;
            }
        }
    }

    public void updateSneak(EntityPlayer entityPlayer) {
        this.sneaking = entityPlayer.func_70093_af();
    }

    public void updateServerPos(EntityPlayer entityPlayer) {
        this.serverPosX = entityPlayer.field_70118_ct / 32;
        this.serverPosY = entityPlayer.field_70117_cu / 32;
        this.serverPosZ = entityPlayer.field_70116_cv / 32;
    }
}
