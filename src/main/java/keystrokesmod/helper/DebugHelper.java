package keystrokesmod.helper;

import keystrokesmod.Raven;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.utility.IMinecraftInstance;
import keystrokesmod.utility.RenderUtils;
import keystrokesmod.utility.Utils;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.RenderTickEvent;

public class DebugHelper implements IMinecraftInstance {

    public static boolean MIXIN;

    public static boolean BACKGROUND;

    @SubscribeEvent
    public void onRenderTick(RenderTickEvent ev) {
        if (Raven.DEBUG && ev.phase == Phase.END && Utils.nullCheck() && ModuleManager.debug.debugBPS.isToggled() && mc.field_71462_r == null) {
            RenderUtils.renderBPS(true, true);
        }
    }

    public static void debugMixin(Object obj, String message) {
        if (MIXIN) {
            Utils.sendMessage("&d" + obj.getClass().getSimpleName() + "&7: " + message);
        }
    }
}
