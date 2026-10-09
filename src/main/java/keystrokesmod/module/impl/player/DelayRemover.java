package keystrokesmod.module.impl.player;

import keystrokesmod.mixin.impl.accessor.IAccessorEntityLivingBase;
import keystrokesmod.mixin.impl.accessor.IAccessorMinecraft;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.utility.Utils;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.PlayerTickEvent;

public class DelayRemover extends Module {

    public ButtonSetting oldReg;

    public ButtonSetting removeJumpTicks;

    public DelayRemover() {
        super("Delay Remover", Module.category.player, 0);
        this.liteModule = true;
        this.registerSetting(this.oldReg = new ButtonSetting("1.7 hitreg", true));
        this.registerSetting(this.removeJumpTicks = new ButtonSetting("Remove jump ticks", false));
        this.closetModule = true;
    }

    @SubscribeEvent
    public void onTick(PlayerTickEvent e) {
        if (e.phase == Phase.END && mc.field_71415_G && Utils.nullCheck()) {
            if (this.oldReg.isToggled()) {
                ((IAccessorMinecraft) mc).setLeftClickCounter(0);
            }
            if (this.removeJumpTicks.isToggled()) {
                ((IAccessorEntityLivingBase) mc.field_71439_g).setJumpTicks(0);
            }
        }
    }
}
