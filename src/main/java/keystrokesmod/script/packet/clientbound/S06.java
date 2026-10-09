package keystrokesmod.script.packet.clientbound;

import net.minecraft.network.play.server.S06PacketUpdateHealth;

public class S06 extends SPacket {

    public float health;

    public float saturation;

    public int food;

    public S06(S06PacketUpdateHealth packet) {
        super(packet);
        this.health = packet.func_149332_c();
        this.saturation = packet.func_149331_e();
        this.food = packet.func_149330_d();
    }

    public S06(float health, float saturation, int food) {
        super(new S06PacketUpdateHealth(health, food, saturation));
        this.health = health;
        this.saturation = saturation;
        this.food = food;
    }
}
