package keystrokesmod.event;

import net.minecraft.entity.Entity;
import net.minecraftforge.fml.common.eventhandler.Event;

public class StepHeightEvent extends Event {

    public Entity entity;

    public float stepHeight;

    public StepHeightEvent(Entity entity, float stepHeight) {
        this.entity = entity;
        this.stepHeight = stepHeight;
    }
}
