package keystrokesmod.event;

import net.minecraftforge.fml.common.eventhandler.Event;

public class KeyEvent extends Event {

    public String keyName;

    public int keyCode;

    public boolean state;

    public boolean inGui;

    public KeyEvent(String keyName, int keyCode, boolean state, boolean inGui) {
        this.keyName = keyName;
        this.keyCode = keyCode;
        this.state = state;
        this.inGui = inGui;
    }
}
