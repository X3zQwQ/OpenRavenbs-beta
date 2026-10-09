package keystrokesmod.event;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.eventhandler.Cancelable;
import net.minecraftforge.fml.common.eventhandler.Event;

@Cancelable
public class AttackEvent extends Event {

    public Entity target;

    public EntityPlayer attacker;

    public boolean swing;

    public AttackEvent(Entity target, EntityPlayer attacker, boolean swing) {
        this.target = target;
        this.attacker = attacker;
        this.swing = swing;
    }
}
