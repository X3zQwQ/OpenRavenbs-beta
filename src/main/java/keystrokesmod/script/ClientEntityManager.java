package keystrokesmod.script;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;

public final class ClientEntityManager {

    private static final Minecraft mc = Minecraft.func_71410_x();

    private static final Map<Entity, ClientEntityManager.ClientEntity> clientEntities = new IdentityHashMap<>();

    private static final Map<String, String> aliases = new HashMap<>();

    private static int nextEntityId = -1;

    private ClientEntityManager() {
    }

    public static keystrokesmod.script.model.Entity spawnMob(String type, double x, double y, double z) {
        return callOnClientThread(() -> spawnMobNow(type, x, y, z), null);
    }

    public static boolean removeClientEntity(keystrokesmod.script.model.Entity entity) {
        return callOnClientThread(() -> removeClientEntityNow(entity), false);
    }

    public static void clearClientEntities() {
        callOnClientThread(() -> {
            clearClientEntitiesNow();
            return true;
        }, false);
    }

    private static keystrokesmod.script.model.Entity spawnMobNow(String type, double x, double y, double z) {
        WorldClient world = mc.field_71441_e;
        String entityName = resolveEntityName(type);
        if (world != null && entityName != null) {
            Entity entity = EntityList.func_75620_a(entityName, world);
            if (!(entity instanceof EntityLivingBase)) {
                return null;
            } else {
                int entityId = allocateEntityId(world);
                entity.func_70080_a(x, y, z, 0.0F, 0.0F);
                world.func_73027_a(entityId, entity);
                clientEntities.put(entity, new ClientEntityManager.ClientEntity(world, entity, entityId));
                return keystrokesmod.script.model.Entity.convert(entity);
            }
        } else {
            return null;
        }
    }

    private static boolean removeClientEntityNow(keystrokesmod.script.model.Entity entity) {
        if (entity != null && entity.entity != null) {
            ClientEntityManager.ClientEntity clientEntity = clientEntities.remove(entity.entity);
            if (clientEntity == null) {
                return false;
            } else {
                removeFromWorld(clientEntity);
                return true;
            }
        } else {
            return false;
        }
    }

    private static void clearClientEntitiesNow() {
        List<ClientEntityManager.ClientEntity> entities = new ArrayList<>(clientEntities.values());
        clientEntities.clear();
        for (ClientEntityManager.ClientEntity entity : entities) {
            removeFromWorld(entity);
        }
        nextEntityId = -1;
    }

    private static void removeFromWorld(ClientEntityManager.ClientEntity clientEntity) {
        if (clientEntity.world.func_73045_a(clientEntity.entityId) == clientEntity.entity) {
            clientEntity.world.func_73028_b(clientEntity.entityId);
        } else if (clientEntity.world.field_72996_f.contains(clientEntity.entity)) {
            clientEntity.world.func_72900_e(clientEntity.entity);
        }
    }

    private static int allocateEntityId(WorldClient world) {
        while (world.func_73045_a(nextEntityId) != null) {
            decrementEntityId();
        }
        int entityId = nextEntityId;
        decrementEntityId();
        return entityId;
    }

    private static void decrementEntityId() {
        nextEntityId = nextEntityId == Integer.MIN_VALUE ? -1 : nextEntityId - 1;
    }

    private static String resolveEntityName(String type) {
        if (type != null && !type.trim().isEmpty()) {
            String normalizedType = normalize(type);
            String alias = aliases.get(normalizedType);
            if (alias != null) {
                return alias;
            } else {
                for (String entityName : EntityList.field_75625_b.keySet()) {
                    if (normalize(entityName).equals(normalizedType)) {
                        return entityName;
                    }
                }
                return null;
            }
        } else {
            return null;
        }
    }

    private static String normalize(String value) {
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith("minecraft:")) {
            normalized = normalized.substring("minecraft:".length());
        }
        return normalized.replace("_", "").replace("-", "").replace(" ", "");
    }

    private static <T> T callOnClientThread(Callable<T> callable, T fallback) {
        try {
            return (T) (mc.func_152345_ab() ? callable.call() : mc.func_152343_a(callable).get());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return fallback;
        } catch (Exception exception2) {
            return fallback;
        }
    }

    static {
        aliases.put("wither", "WitherBoss");
        aliases.put("irongolem", "VillagerGolem");
        aliases.put("snowgolem", "SnowMan");
        aliases.put("mooshroom", "MushroomCow");
        aliases.put("zombiepigman", "PigZombie");
        aliases.put("magmacube", "LavaSlime");
        aliases.put("ocelot", "Ozelot");
        aliases.put("horse", "EntityHorse");
    }

    private static final class ClientEntity {

        private final WorldClient world;

        private final Entity entity;

        private final int entityId;

        private ClientEntity(WorldClient world, Entity entity, int entityId) {
            this.world = world;
            this.entity = entity;
            this.entityId = entityId;
        }
    }
}
