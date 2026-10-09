package keystrokesmod.module.impl.world;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import keystrokesmod.Raven;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.player.Freecam;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.Utils;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class AntiBot extends Module {

    private static final HashMap<EntityPlayer, Long> entities = new HashMap<>();

    private static SliderSetting delay;

    private static SliderSetting pitSpawn;

    private static ButtonSetting tablist;

    private ButtonSetting printWorldJoin;

    public AntiBot() {
        super("Anti Bot", Module.category.world);
        this.registerSetting(delay = new SliderSetting("Delay", " second", true, -1.0, 0.5, 15.0, 0.5));
        this.registerSetting(pitSpawn = new SliderSetting("Pit spawn", true, -1.0, 70.0, 120.0, 1.0));
        this.registerSetting(tablist = new ButtonSetting("Tab list", false));
        this.registerSetting(this.printWorldJoin = new ButtonSetting("Print world join", false));
        this.closetModule = true;
        this.liteModule = true;
    }

    @SubscribeEvent
    public void onEntityJoin(EntityJoinWorldEvent e) {
        if ((e.entity instanceof EntityPlayer || Raven.DEBUG) && e.entity != mc.field_71439_g) {
            if (delay.getInput() != -1.0 && e.entity instanceof EntityPlayer) {
                entities.put((EntityPlayer) e.entity, System.currentTimeMillis());
            }
            if (this.printWorldJoin.isToggled()) {
                Utils.sendMessage("&7Entity &b" + e.entity.func_145782_y() + " &7joined: &r" + e.entity.func_145748_c_().func_150254_d());
            }
        }
    }

    @Override
    public void onUpdate() {
        if (delay.getInput() != -1.0 && !entities.isEmpty()) {
            entities.values().removeIf(n -> n.longValue() < System.currentTimeMillis() - delay.getInput());
        }
    }

    @Override
    public void onDisable() {
        entities.clear();
    }

    public static boolean isBot(Entity entity) {
        if (!ModuleManager.antiBot.isEnabled()) {
            return false;
        } else if (Freecam.freeEntity != null && Freecam.freeEntity == entity) {
            return true;
        } else if (!(entity instanceof EntityPlayer)) {
            return true;
        } else {
            EntityPlayer entityPlayer = (EntityPlayer) entity;
            if (delay.getInput() != -1.0 && !entities.isEmpty() && entities.containsKey(entityPlayer)) {
                return true;
            } else if (entityPlayer.field_70128_L) {
                return true;
            } else if (Utils.removeFormatCodes(entityPlayer.func_70005_c_()).trim().isEmpty()) {
                return true;
            } else if (tablist.isToggled() && !getTablist().contains(entityPlayer.func_70005_c_())) {
                return true;
            } else if (entityPlayer.func_110143_aJ() != 20.0F && entityPlayer.func_70005_c_().startsWith("§c")) {
                return true;
            } else {
                if (pitSpawn.getInput() != -1.0 && entityPlayer.field_70163_u >= pitSpawn.getInput() && entityPlayer.field_70163_u <= 130.0 && entityPlayer.func_70011_f(0.0, 114.0, 0.0) <= 25.0 && Utils.isHypixel()) {
                    List<String> sidebarLines = Utils.getSidebarLines();
                    if (!sidebarLines.isEmpty() && Utils.stripColor(sidebarLines.get(0)).contains("THE HYPIXEL PIT")) {
                        return true;
                    }
                }
                if (entityPlayer.field_70738_aO == 0) {
                    if (entityPlayer.func_110143_aJ() == 20.0F) {
                        String unformattedText = entityPlayer.func_145748_c_().func_150260_c();
                        if (unformattedText.length() == 10 && unformattedText.charAt(0) != 167) {
                            return true;
                        }
                        if (unformattedText.length() == 12 && entityPlayer.func_70608_bn() && unformattedText.charAt(0) == 167) {
                            return true;
                        }
                        if (unformattedText.length() >= 7 && unformattedText.charAt(2) == '[' && unformattedText.charAt(3) == 'N' && unformattedText.charAt(6) == ']') {
                            return true;
                        }
                        if (entityPlayer.func_70005_c_().contains(" ")) {
                            return true;
                        }
                    } else if (entityPlayer.func_82150_aj()) {
                        String text = entityPlayer.func_145748_c_().func_150260_c();
                        if (text.length() >= 3 && text.charAt(0) == 167 && text.charAt(1) == 'c') {
                            return true;
                        }
                    }
                }
                return false;
            }
        }
    }

    private static List<String> getTablist() {
        List<String> tab = new ArrayList<>();
        for (NetworkPlayerInfo networkPlayerInfo : Utils.getTablist(true)) {
            if (networkPlayerInfo != null) {
                tab.add(networkPlayerInfo.func_178845_a().getName());
            }
        }
        return tab;
    }
}
