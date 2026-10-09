package keystrokesmod.script.model;

import com.google.common.collect.Iterables;
import com.mojang.authlib.properties.Property;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.scoreboard.ScorePlayerTeam;

public class NetworkPlayer {

    private NetworkPlayerInfo playerInfo;

    private static HashMap<String, NetworkPlayer> cache = new HashMap<>();

    public NetworkPlayer(NetworkPlayerInfo playerInfo) {
        this.playerInfo = playerInfo;
    }

    public String getCape() {
        return this.playerInfo.func_178861_h().func_110623_a();
    }

    public String getDisplayName() {
        return this.playerInfo.func_178845_a() == Minecraft.func_71410_x().field_71439_g.func_146103_bH() ? Minecraft.func_71410_x().field_71439_g.func_145748_c_().func_150260_c() : ScorePlayerTeam.func_96667_a(this.playerInfo.func_178850_i(), this.getName());
    }

    public String getName() {
        return this.playerInfo == null ? "" : this.playerInfo.func_178845_a().getName();
    }

    public int getPing() {
        return this.playerInfo == null ? 0 : this.playerInfo.func_178853_c();
    }

    public String getSkinData() {
        Property texture = (Property) Iterables.getFirst(this.playerInfo.func_178845_a().getProperties().get("textures"), null);
        return texture == null ? null : new String(Base64.getDecoder().decode(texture.getValue().getBytes(StandardCharsets.UTF_8)));
    }

    public String getUUID() {
        return this.playerInfo == null ? "" : this.playerInfo.func_178845_a().getId().toString();
    }

    public static NetworkPlayer convert(NetworkPlayerInfo networkPlayerInfo) {
        if (networkPlayerInfo == null) {
            return null;
        } else {
            String id = networkPlayerInfo.func_178845_a().getId().toString();
            NetworkPlayer cachedEntity = cache.get(id);
            if (cachedEntity == null) {
                cachedEntity = new NetworkPlayer(networkPlayerInfo);
                cache.put(id, cachedEntity);
            }
            return cachedEntity;
        }
    }

    public static void clearCache() {
        cache.clear();
    }
}
