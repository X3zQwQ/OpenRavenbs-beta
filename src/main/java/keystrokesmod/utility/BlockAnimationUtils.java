package keystrokesmod.utility;

import keystrokesmod.mixin.interfaces.IMixinItemRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemStack;

public final class BlockAnimationUtils {

    private static EntityPlayer renderingPlayer;

    private static int renderingDepth;

    private BlockAnimationUtils() {
    }

    public static void beginRender(EntityPlayer player) {
        if (player == Minecraft.func_71410_x().field_71439_g && renderingDepth++ == 0) {
            renderingPlayer = player;
        }
    }

    public static void endRender(EntityPlayer player) {
        if (player == renderingPlayer && renderingDepth > 0) {
            if (--renderingDepth == 0) {
                renderingPlayer = null;
            }
        }
    }

    public static boolean shouldForceBlockAnimation(EntityPlayer player, ItemStack stack) {
        if (player != null && player == renderingPlayer && stack != null && stack.func_77975_n() == EnumAction.BLOCK) {
            Minecraft mc = Minecraft.func_71410_x();
            return mc.func_175597_ag() != null && ((IMixinItemRenderer) mc.func_175597_ag()).isRenderItemInUse();
        } else {
            return false;
        }
    }
}
