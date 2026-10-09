package keystrokesmod.mixin.impl.network;

import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import keystrokesmod.event.DispatchPacketEvent;
import keystrokesmod.event.NoEventPacketEvent;
import keystrokesmod.event.ReceivePacketEvent;
import keystrokesmod.event.SendPacketEvent;
import keystrokesmod.utility.PacketUtils;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetworkManager.class)
public class MixinNetworkManager {

    @Inject(method = "sendPacket(Lnet/minecraft/network/Packet;)V", at = @At("HEAD"), cancellable = true)
    public void sendPacket(Packet p_sendPacket_1_, CallbackInfo ci) {
        if (PacketUtils.consumeSendEventSkip(p_sendPacket_1_)) {
            MinecraftForge.EVENT_BUS.post(new NoEventPacketEvent(p_sendPacket_1_));
        } else {
            SendPacketEvent sendPacketEvent = new SendPacketEvent(p_sendPacket_1_);
            MinecraftForge.EVENT_BUS.post(sendPacketEvent);
            if (sendPacketEvent.isCanceled()) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "dispatchPacket", at = @At("HEAD"))
    public void dispatchPacket(Packet inPacket, GenericFutureListener<? extends Future<? super Void>>[] futureListeners, CallbackInfo ci) {
        DispatchPacketEvent dispatchPacketEvent = new DispatchPacketEvent((NetworkManager) (Object) this, inPacket);
        MinecraftForge.EVENT_BUS.post(dispatchPacketEvent);
    }

    @Inject(method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/Packet;)V", at = @At("HEAD"), cancellable = true)
    public void receivePacket(ChannelHandlerContext p_channelRead0_1_, Packet p_channelRead0_2_, CallbackInfo ci) {
        if (!PacketUtils.consumeReceiveEventSkip(p_channelRead0_2_)) {
            ReceivePacketEvent receivePacketEvent = new ReceivePacketEvent(p_channelRead0_2_);
            MinecraftForge.EVENT_BUS.post(receivePacketEvent);
            if (receivePacketEvent.isCanceled()) {
                ci.cancel();
            }
        }
    }
}
