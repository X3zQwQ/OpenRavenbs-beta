package keystrokesmod.mixin.impl.network;

import keystrokesmod.event.PlayerTeleportEvent;
import keystrokesmod.event.PreEntityVelocityEvent;
import keystrokesmod.event.PreExplosionPacketEvent;
import keystrokesmod.module.ModuleManager;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S27PacketExplosion;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetHandlerPlayClient.class)
public class MixinNetHandlerPlayClient {

    @Inject(method = "handlePlayerPosLook", at = @At("HEAD"))
    public void handlePlayerPosLookPre(S08PacketPlayerPosLook packetIn, CallbackInfo ci) {
        PlayerTeleportEvent e = new PlayerTeleportEvent(packetIn);
        MinecraftForge.EVENT_BUS.post(e);
        if (e.isCanceled()) {
            ci.cancel();
        }
    }

    @Inject(method = "handlePlayerPosLook", at = @At("RETURN"))
    public void handlePlayerPosLook(S08PacketPlayerPosLook packetIn, CallbackInfo ci) {
        ModuleManager.noRotate.handlePlayerPosLook(packetIn);
    }

    @Inject(method = "handleEntityVelocity", at = @At("HEAD"), cancellable = true)
    public void handleEntityVelocityInjection(S12PacketEntityVelocity packet, CallbackInfo ci) {
        PreEntityVelocityEvent preEntityVelocityEvent = new PreEntityVelocityEvent(packet);
        MinecraftForge.EVENT_BUS.post(preEntityVelocityEvent);
        if (preEntityVelocityEvent.isCanceled()) {
            ci.cancel();
        }
    }

    @Inject(method = "handleExplosion", at = @At("HEAD"), cancellable = true)
    public void handleExplosionInjection(S27PacketExplosion packet, CallbackInfo ci) {
        PreExplosionPacketEvent event = new PreExplosionPacketEvent(packet);
        MinecraftForge.EVENT_BUS.post(event);
        if (event.isCanceled()) {
            ci.cancel();
        }
    }
}
