package keystrokesmod.mixin.impl.accessor;

import net.minecraft.client.Minecraft;
import net.minecraft.network.NetworkManager;
import net.minecraft.util.Timer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@SideOnly(Side.CLIENT)
@Mixin(Minecraft.class)
public interface IAccessorMinecraft {

    @Accessor("timer")
    Timer getTimer();

    @Accessor("myNetworkManager")
    NetworkManager getMyNetworkManager();

    @Accessor("rightClickDelayTimer")
    int getRightClickDelayTimer();

    @Accessor("rightClickDelayTimer")
    void setRightClickDelayTimer(int rightClickDelayTimer);

    @Accessor("leftClickCounter")
    void setLeftClickCounter(int leftClickCounter);

    @Invoker("rightClickMouse")
    void callRightClickMouse();

    @Invoker("clickMouse")
    void callClickMouse();

    @Invoker("dispatchKeypresses")
    void invokeDispatchKeypresses();
}
