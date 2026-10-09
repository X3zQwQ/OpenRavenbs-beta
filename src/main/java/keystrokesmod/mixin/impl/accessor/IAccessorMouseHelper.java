package keystrokesmod.mixin.impl.accessor;

import net.minecraft.util.MouseHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@SideOnly(Side.CLIENT)
@Mixin(MouseHelper.class)
public interface IAccessorMouseHelper {

    @Accessor("deltaX")
    int getDeltaX();

    @Accessor("deltaY")
    int getDeltaY();
}
