package keystrokesmod.mixin.impl.network;

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.handshake.FMLHandshakeMessage.ModList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModList.class)
public abstract class MixinModList {

    private static final List<String> exemptMods = Arrays.asList("FML", "mcp", "Forge");

    @Shadow(remap = false)
    private Map<String, String> modTags;

    @Inject(method = "toBytes", at = @At("HEAD"), cancellable = true, remap = false)
    public void toBytes(ByteBuf buffer, CallbackInfo callbackInfo) {
        if (!Minecraft.func_71410_x().func_71356_B()) {
            callbackInfo.cancel();
            ArrayList<Entry<String, String>> shownTags = new ArrayList<>();
            for (Entry<String, String> modTag : this.modTags.entrySet()) {
                if (exemptMods.contains(modTag.getKey())) {
                    shownTags.add(modTag);
                }
            }
            ByteBufUtils.writeVarInt(buffer, shownTags.size(), 2);
            for (Entry<String, String> entry : shownTags) {
                ByteBufUtils.writeUTF8String(buffer, entry.getKey());
                ByteBufUtils.writeUTF8String(buffer, entry.getValue());
            }
        }
    }
}
