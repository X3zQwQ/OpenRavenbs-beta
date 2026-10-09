package keystrokesmod.utility;

import com.mojang.authlib.Agent;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.ProfileLookupCallback;
import com.mojang.authlib.minecraft.MinecraftProfileTexture.Type;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import java.net.Proxy;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import keystrokesmod.Raven;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

public class PlayerSkinCache {

    private static final long PROFILE_CACHE_TTL_MS = 1800000L;

    private static final Map<String, ResourceLocation> SKINS = new ConcurrentHashMap<>();

    private static final Map<String, UUID> UUIDS = new ConcurrentHashMap<>();

    private static final Map<String, PlayerSkinCache.CachedProfile> PROFILES = new ConcurrentHashMap<>();

    private static final Set<String> LOOKUP_IN_FLIGHT = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private static final Set<String> LOOKUP_FAILED = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private static final GameProfileRepository PROFILE_REPOSITORY = new YggdrasilAuthenticationService(Proxy.NO_PROXY, UUID.randomUUID().toString()).createProfileRepository();

    private static final ExecutorService LOOKUP_EXECUTOR = Executors.newFixedThreadPool(3);

    private PlayerSkinCache() {
    }

    public static ResourceLocation getSkin(String username, NetworkPlayerInfo playerInfo) {
        String normalized = normalize(username);
        if (normalized.isEmpty()) {
            return DefaultPlayerSkin.func_177334_a(EntityPlayer.func_175147_b("Steve"));
        } else {
            if (playerInfo != null && playerInfo.func_178845_a() != null) {
                GameProfile profile = playerInfo.func_178845_a();
                UUID uuid = profile.getId();
                if (uuid != null) {
                    UUIDS.put(normalized, uuid);
                }
                if (profile.getName() != null && Raven.playerRelationsManager != null) {
                    Raven.playerRelationsManager.refreshDisplayName(profile.getName());
                }
                ResourceLocation location = playerInfo.func_178837_g();
                if (location != null) {
                    SKINS.put(normalized, location);
                    return location;
                }
            }
            ResourceLocation cached = SKINS.get(normalized);
            if (cached != null) {
                return cached;
            } else {
                if (!LOOKUP_FAILED.contains(normalized)) {
                    requestSkin(normalized, username);
                }
                UUID fallbackUuid = UUIDS.get(normalized);
                if (fallbackUuid == null) {
                    fallbackUuid = EntityPlayer.func_175147_b(username);
                    UUIDS.put(normalized, fallbackUuid);
                }
                return DefaultPlayerSkin.func_177334_a(fallbackUuid);
            }
        }
    }

    private static void requestSkin(String normalized, String username) {
        long now = System.currentTimeMillis();
        PlayerSkinCache.CachedProfile cachedProfile = PROFILES.get(normalized);
        if (cachedProfile != null && !cachedProfile.isExpired(now)) {
            if (cachedProfile.profile == null) {
                LOOKUP_FAILED.add(normalized);
            } else {
                resolveSkinFromProfile(normalized, cachedProfile.profile);
            }
        } else if (LOOKUP_IN_FLIGHT.add(normalized)) {
            LOOKUP_EXECUTOR.execute(new Runnable() {

                @Override
                public void run() {
                    try {
                        final GameProfile[] holder = new GameProfile[1];
                        PlayerSkinCache.PROFILE_REPOSITORY.findProfilesByNames(new String[] { username }, Agent.MINECRAFT, new ProfileLookupCallback() {

                            public void onProfileLookupSucceeded(GameProfile profile) {
                                holder[0] = profile;
                            }

                            public void onProfileLookupFailed(GameProfile profile, Exception exception) {
                            }
                        });
                        GameProfile profile = holder[0];
                        if (profile == null) {
                            PlayerSkinCache.cacheProfile(normalized, null);
                            PlayerSkinCache.LOOKUP_FAILED.add(normalized);
                            return;
                        }
                        Minecraft minecraft = Minecraft.func_71410_x();
                        GameProfile filledProfile = minecraft.func_152347_ac().fillProfileProperties(profile, false);
                        if (filledProfile != null) {
                            PlayerSkinCache.cacheProfile(normalized, filledProfile);
                            PlayerSkinCache.resolveSkinFromProfile(normalized, filledProfile);
                            return;
                        }
                        PlayerSkinCache.cacheProfile(normalized, null);
                        PlayerSkinCache.LOOKUP_FAILED.add(normalized);
                    } catch (Exception exception2) {
                        PlayerSkinCache.cacheProfile(normalized, null);
                        PlayerSkinCache.LOOKUP_FAILED.add(normalized);
                        return;
                    } finally {
                        PlayerSkinCache.LOOKUP_IN_FLIGHT.remove(normalized);
                    }
                }
            });
        }
    }

    private static void resolveSkinFromProfile(String normalized, GameProfile profile) {
        if (profile == null) {
            LOOKUP_FAILED.add(normalized);
        } else {
            if (profile.getId() != null) {
                UUIDS.put(normalized, profile.getId());
            }
            if (profile.getName() != null && Raven.playerRelationsManager != null) {
                Raven.playerRelationsManager.refreshDisplayName(profile.getName());
            }
            Minecraft minecraft = Minecraft.func_71410_x();
            Map<Type, MinecraftProfileTexture> textures = minecraft.func_152342_ad().func_152788_a(profile);
            final MinecraftProfileTexture skinTexture = textures == null ? null : textures.get(Type.SKIN);
            if (skinTexture == null) {
                LOOKUP_FAILED.add(normalized);
            } else {
                minecraft.func_152344_a(new Runnable() {

                    @Override
                    public void run() {
                        ResourceLocation location = Minecraft.func_71410_x().func_152342_ad().func_152792_a(skinTexture, Type.SKIN);
                        if (location != null) {
                            PlayerSkinCache.SKINS.put(normalized, location);
                            PlayerSkinCache.LOOKUP_FAILED.remove(normalized);
                        } else {
                            PlayerSkinCache.LOOKUP_FAILED.add(normalized);
                        }
                    }
                });
            }
        }
    }

    private static void cacheProfile(String normalized, GameProfile profile) {
        PROFILES.put(normalized, new PlayerSkinCache.CachedProfile(profile, System.currentTimeMillis() + 1800000L));
    }

    private static String normalize(String username) {
        return username == null ? "" : username.trim().toLowerCase();
    }

    private static final class CachedProfile {

        private final GameProfile profile;

        private final long expiresAtMs;

        private CachedProfile(GameProfile profile, long expiresAtMs) {
            this.profile = profile;
            this.expiresAtMs = expiresAtMs;
        }

        private boolean isExpired(long now) {
            return now >= this.expiresAtMs;
        }
    }
}
