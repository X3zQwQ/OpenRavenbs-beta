package keystrokesmod.event;

import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.fml.common.eventhandler.Cancelable;
import net.minecraftforge.fml.common.eventhandler.Event;

@Cancelable
public class PreAttackEvent extends Event {

    public final MovingObjectPosition objectMouseOver;

    public PreAttackEvent(MovingObjectPosition objectMouseOver) {
        this.objectMouseOver = objectMouseOver;
    }
}
