package keystrokesmod.event;

import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.fml.common.eventhandler.Cancelable;
import net.minecraftforge.fml.common.eventhandler.Event;

@Cancelable
public class JumpEvent extends Event {

    public EntityLivingBase entity;

    private float motionY;

    private float yaw;

    private boolean applySprint;

    public JumpEvent(EntityLivingBase entity, float motionY, float yaw, boolean applySprint) {
        this.entity = entity;
        this.motionY = motionY;
        this.yaw = yaw;
        this.applySprint = applySprint;
    }

    public float getMotionY() {
        return this.motionY;
    }

    public void setMotionY(float motionY) {
        this.motionY = motionY;
    }

    public float getYaw() {
        return this.yaw;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public boolean applySprint() {
        return this.applySprint;
    }

    public void setSprint(boolean applySprint) {
        this.applySprint = applySprint;
    }
}
