package keystrokesmod.script.packet.clientbound;

import keystrokesmod.script.model.Vec3;
import net.minecraft.network.play.server.S29PacketSoundEffect;

public class S29 extends SPacket {

    public String sound;

    public Vec3 position;

    public float volume;

    public float pitch;

    public S29(S29PacketSoundEffect packet) {
        super(packet);
        this.sound = packet.func_149212_c();
        this.position = new Vec3(packet.func_149207_d(), packet.func_149211_e(), packet.func_149210_f());
        this.volume = packet.func_149208_g();
        this.pitch = packet.func_149209_h();
    }

    public S29(String sound, Vec3 position, float volume, float pitch) {
        super(new S29PacketSoundEffect(sound, position.x, position.y, position.z, volume, pitch));
        this.sound = sound;
        this.position = position;
        this.volume = volume;
        this.pitch = pitch;
    }
}
