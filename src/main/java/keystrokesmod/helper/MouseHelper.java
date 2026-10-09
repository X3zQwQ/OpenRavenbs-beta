package keystrokesmod.helper;

import java.util.ArrayList;
import java.util.List;
import keystrokesmod.Raven;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.world.AntiBot;
import keystrokesmod.utility.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Mouse;

public class MouseHelper {

    private static Minecraft mc = Minecraft.func_71410_x();

    private static List<Long> a = new ArrayList<>();

    private static List<Long> b = new ArrayList<>();

    public static long LL = 0L;

    public static long LR = 0L;

    private static int cachedWheelDelta = 0;

    private static boolean wheelDeltaCached = false;

    public static void updateWheelCache() {
        cachedWheelDelta = Mouse.getDWheel();
        wheelDeltaCached = true;
    }

    public static boolean isScrollDown(int key) {
        if (!wheelDeltaCached) {
            cachedWheelDelta = Mouse.getDWheel();
            wheelDeltaCached = true;
        }
        if (cachedWheelDelta != 0) {
            if (key == 1069) {
                return cachedWheelDelta > 0;
            }
            if (key == 1070) {
                return cachedWheelDelta < 0;
            }
        }
        return false;
    }

    public static void clearWheelCache() {
        wheelDeltaCached = false;
        cachedWheelDelta = 0;
    }

    @SubscribeEvent
    public void onMouse(MouseEvent e) {
        if (e.buttonstate) {
            if (e.button != 0) {
                if (e.button == 1) {
                    aR();
                } else if (e.button == 2 && ModuleManager.relationships != null && ModuleManager.relationships.isEnabled() && ModuleManager.relationships.middleClickFriends.isToggled()) {
                    EntityLivingBase g = Utils.raytrace(200);
                    if (g != null && !AntiBot.isBot(g)) {
                        String n = g.func_70005_c_();
                        if (Utils.addFriend(n)) {
                            Utils.sendMessage("&7Added &afriend&7: &b" + n);
                        } else if (Utils.removeFriend(n)) {
                            Utils.sendMessage("&7Removed &afriend&7: &b" + n);
                        }
                    }
                }
            } else {
                aL();
                boolean debugAttacked = ModuleManager.debug == null || ModuleManager.debug.shouldDebugAttacked();
                if (Raven.DEBUG && debugAttacked && mc.field_71476_x != null) {
                    Entity en = mc.field_71476_x.field_72308_g;
                    if (en == null || !(en instanceof EntityLivingBase)) {
                        return;
                    }
                    Utils.printInfo((EntityLivingBase) en);
                }
            }
        }
    }

    public static void aL() {
        a.add(LL = System.currentTimeMillis());
    }

    public static void aR() {
        b.add(LR = System.currentTimeMillis());
    }

    public static int f() {
        a.removeIf(o -> o < System.currentTimeMillis() - 1000L);
        return a.size();
    }

    public static int i() {
        b.removeIf(o -> o < System.currentTimeMillis() - 1000L);
        return b.size();
    }
}
