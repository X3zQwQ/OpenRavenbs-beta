package keystrokesmod.utility;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import keystrokesmod.mixin.interfaces.IMixinItemRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiEnchantment;
import net.minecraft.client.gui.inventory.GuiBrewingStand;
import net.minecraft.client.gui.inventory.GuiDispenser;
import net.minecraft.client.gui.inventory.GuiFurnace;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.inventory.ContainerHopper;
import net.minecraft.inventory.ContainerHorseInventory;
import net.minecraft.inventory.ContainerMerchant;
import net.minecraft.inventory.ContainerWorkbench;
import net.minecraft.inventory.IInventory;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

public class ReflectionUtils {

    public static Field button;

    public static Field buttonstate;

    public static HashMap<Class, Field> containerInventoryPlayer = new HashMap<>();

    private static List<Class> containerClasses = Arrays.asList(GuiFurnace.class, GuiBrewingStand.class, GuiEnchantment.class, ContainerHopper.class, GuiDispenser.class, ContainerWorkbench.class, ContainerMerchant.class, ContainerHorseInventory.class);

    public static boolean ERROR = false;

    public static Map<String, KeyBinding> keybinds = new HashMap<>();

    public static void setupFields() {
        try {
            button = MouseEvent.class.getDeclaredField("button");
            buttonstate = MouseEvent.class.getDeclaredField("buttonstate");
            for (Class clazz : containerClasses) {
                for (Field field : clazz.getDeclaredFields()) {
                    addToMap(clazz, field);
                }
            }
        } catch (Exception exception) {
            System.out.println("There was an error, relaunch the game.");
            exception.printStackTrace();
            ERROR = true;
        }
    }

    public static void setKeyBindings() {
        for (KeyBinding keyBind : Minecraft.func_71410_x().field_71474_y.field_74324_K) {
            String keyName = keyBind.func_151464_g().replaceFirst("key\\.", "");
            keybinds.put(keyName, keyBind);
        }
    }

    public static void setButton(int button, boolean state) {
        if (ReflectionUtils.button != null && buttonstate != null) {
            MouseEvent m = new MouseEvent();
            try {
                ReflectionUtils.button.setAccessible(true);
                ReflectionUtils.button.set(m, button);
                buttonstate.setAccessible(true);
                buttonstate.set(m, state);
                MinecraftForge.EVENT_BUS.post(m);
            } catch (IllegalAccessException exception2) {
            }
        }
    }

    private static void addToMap(Class clazz, Field field) {
        if (field != null && field.getType() == IInventory.class) {
            field = ReflectionHelper.findField(clazz, new String[] { field.getName() });
            if (field != null) {
                field.setAccessible(true);
                containerInventoryPlayer.put(clazz, field);
            }
        }
    }

    public static boolean setItemInUse(boolean blocking) {
        Minecraft mc = Minecraft.func_71410_x();
        if (mc.func_175597_ag() != null) {
            ((IMixinItemRenderer) mc.func_175597_ag()).setRenderItemInUse(blocking);
        }
        return blocking;
    }
}
