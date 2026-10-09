package keystrokesmod.module.impl.combat;

import keystrokesmod.event.PreUpdateEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.impl.world.AntiBot;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.Utils;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class TPAura extends Module {

    private SliderSetting range;

    private ButtonSetting weaponOnly;

    private double x = 0.0;

    private double z = 0.0;

    private double y = 0.0;

    public TPAura() {
        super("TPAura", Module.category.combat);
        this.registerSetting(this.range = new SliderSetting("Range", 0.0, 0.0, 50.0, 1.0));
        this.registerSetting(this.weaponOnly = new ButtonSetting("Weapon only", false));
    }

    @Override
    public void onEnable() {
        if (this.range.getInput() == 0.0) {
            Utils.sendMessage("&cTPAura range values are set to 0.");
            this.disable();
        } else {
            this.updatePosition();
        }
    }

    @SubscribeEvent
    public void onLivingUpdate(LivingUpdateEvent e) {
        if (Utils.nullCheck() && mc.field_71439_g.field_70738_aO > 0 && mc.field_71439_g.field_70737_aN == mc.field_71439_g.field_70738_aO) {
            this.updatePosition();
        }
    }

    @SubscribeEvent
    public void onPreUpdate(PreUpdateEvent e) {
        if (!this.weaponOnly.isToggled() || Utils.holdingWeapon()) {
            double rangeSq = this.range.getInput() * this.range.getInput();
            for (EntityPlayer entityPlayer : mc.field_71441_e.field_73010_i) {
                if (entityPlayer != mc.field_71439_g && entityPlayer.field_70725_aQ == 0 && !(mc.field_71439_g.func_70068_e(entityPlayer) > rangeSq) && !AntiBot.isBot(entityPlayer) && !Utils.isFriended(entityPlayer)) {
                    mc.field_71439_g.func_70107_b(entityPlayer.field_70165_t + this.x, entityPlayer.field_70163_u + this.y, entityPlayer.field_70161_v + this.z);
                    Utils.attackEntity(entityPlayer, true, false);
                    break;
                }
            }
        }
    }

    private void updatePosition() {
        this.x = Utils.randomizeInt(-15, 15) / 10.0;
        this.y = Utils.randomizeInt(10, 15) / 10.0;
        this.z = Utils.randomizeInt(-15, 15) / 10.0;
    }
}
