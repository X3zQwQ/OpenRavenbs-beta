package keystrokesmod.script.packet.serverbound;

import net.minecraft.entity.player.PlayerCapabilities;
import net.minecraft.network.play.client.C13PacketPlayerAbilities;

public class C13 extends CPacket {

    public boolean invulnerable;

    public boolean flying;

    public boolean allowFlying;

    public boolean creativeMode;

    public float flySpeed;

    public float walkSpeed;

    public C13(boolean invulnerable, boolean flying, boolean allowFlying, boolean creativeMode, float flySpeed, float walkSpeed) {
        super(null);
        this.invulnerable = invulnerable;
        this.flying = flying;
        this.allowFlying = allowFlying;
        this.creativeMode = creativeMode;
        this.flySpeed = flySpeed;
        this.walkSpeed = walkSpeed;
    }

    public C13(C13PacketPlayerAbilities packet) {
        super(packet);
    }

    public C13PacketPlayerAbilities convert() {
        PlayerCapabilities capabilities = new PlayerCapabilities();
        capabilities.field_75102_a = this.invulnerable;
        capabilities.field_75100_b = this.flying;
        capabilities.field_75101_c = this.allowFlying;
        capabilities.field_75098_d = this.creativeMode;
        capabilities.func_75092_a(this.flySpeed);
        capabilities.func_82877_b(this.walkSpeed);
        return new C13PacketPlayerAbilities(capabilities);
    }
}
