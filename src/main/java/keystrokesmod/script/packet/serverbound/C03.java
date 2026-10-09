package keystrokesmod.script.packet.serverbound;

import keystrokesmod.script.model.Vec3;
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition;
import net.minecraft.network.play.client.C03PacketPlayer.C05PacketPlayerLook;
import net.minecraft.network.play.client.C03PacketPlayer.C06PacketPlayerPosLook;
import net.minecraft.network.play.client.C03PacketPlayer;

public class C03 extends CPacket {

    public Vec3 position;

    public float yaw;

    public float pitch;

    public boolean ground;

    public boolean moving;

    public boolean rotating;

    public C03(C03PacketPlayer packet, byte f1, byte f2, byte f3, byte f4, byte f5, byte f6) {
        super(packet);
        if (packet instanceof C04PacketPlayerPosition || packet instanceof C06PacketPlayerPosLook) {
            this.position = new Vec3(packet.func_149464_c(), packet.func_149467_d(), packet.func_149472_e());
        }
        if (packet instanceof C05PacketPlayerLook || packet instanceof C06PacketPlayerPosLook) {
            this.yaw = packet.func_149462_g();
            this.pitch = packet.func_149470_h();
        }
        this.ground = packet.func_149465_i();
        this.moving = packet.func_149466_j();
        this.rotating = packet.func_149463_k();
    }

    public C03(boolean ground) {
        super(new C03PacketPlayer(ground));
        this.ground = ground;
    }

    public C03(Vec3 position, boolean ground) {
        super(new C04PacketPlayerPosition(position.x, position.y, position.z, ground));
        this.position = position;
        this.ground = ground;
    }

    public C03(float yaw, float pitch, boolean ground) {
        super(new C05PacketPlayerLook(yaw, pitch, ground));
        this.yaw = yaw;
        this.pitch = pitch;
        this.ground = ground;
    }

    public C03(Vec3 position, float yaw, float pitch, boolean ground) {
        super(new C06PacketPlayerPosLook(position.x, position.y, position.z, yaw, pitch, ground));
        this.position = position;
        this.yaw = yaw;
        this.pitch = pitch;
        this.ground = ground;
    }
}
