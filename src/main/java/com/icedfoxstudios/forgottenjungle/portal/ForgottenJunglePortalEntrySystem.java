package com.icedfoxstudios.forgottenjungle.portal;

import com.hypixel.hytale.builtin.instances.InstancesPlugin;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.icedfoxstudios.forgottenjungle.core.ForgottenJungleRuntime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.joml.Vector3d;

public final class ForgottenJunglePortalEntrySystem extends EntityTickingSystem<EntityStore> {
    @Override
    public Query<EntityStore> getQuery() {
        return PlayerRef.getComponentType();
    }

    @Override
    public void tick(
        float deltaTime,
        int index,
        ArchetypeChunk<EntityStore> archetypeChunk,
        Store<EntityStore> store,
        CommandBuffer<EntityStore> commandBuffer
    ) {
        World world = ForgottenJungleRuntime.getWorld(store);
        if (world == null || commandBuffer == null || ForgottenJungleRuntime.isForgottenJungleWorld(world)) {
            return;
        }

        PlayerRef playerRef = archetypeChunk.getComponent(index, PlayerRef.getComponentType());
        if (playerRef == null || playerRef.getUuid() == null || playerRef.getReference() == null || playerRef.getTransform() == null) {
            return;
        }

        long nowMs = System.currentTimeMillis();
        UUID playerUuid = playerRef.getUuid();
        if (ForgottenJungleRuntime.isPortalEntryOnCooldown(playerUuid, nowMs)) {
            return;
        }

        Vector3d position = playerRef.getTransform().getPosition();
        if (position == null) {
            return;
        }

        int[] portalPos = ForgottenJungleRuntime.findNearestBlock(world, position, ForgottenJungleRuntime.getTFJPortalBlockId(), "TFJ_Jungle_Portal", 2, 3);
        if (portalPos == null) {
            return;
        }

        ForgottenJungleRuntime.setPortalEntryCooldown(playerUuid, nowMs + ForgottenJungleRuntime.TFJ_PORTAL_COOLDOWN_MS);
        Transform returnTransform = playerRef.getTransform().clone();
        Rotation3f rotation = returnTransform.getRotation();
        if (rotation == null) {
            rotation = new Rotation3f();
        }
        Transform entryTransform = new Transform(new Vector3d(0.5, 96.0, 0.5), rotation);

        try {
            CompletableFuture<World> jungleWorld = ForgottenJungleRuntime.getOrCreateForgottenJungleWorld(world, returnTransform);
            jungleWorld.whenComplete((createdWorld, throwable) -> {
                if (throwable != null || createdWorld == null) {
                    ForgottenJungleRuntime.setPortalEntryCooldown(playerUuid, System.currentTimeMillis() + 15000L);
                }
            });
            System.out.println("[TFJ][Portal] Enter player=" + playerUuid + " portal=" + portalPos[0] + "," + portalPos[1] + "," + portalPos[2] + " instance=the_forgotten_jungle_world");
            InstancesPlugin.teleportPlayerToLoadingInstance(playerRef.getReference(), commandBuffer, jungleWorld, entryTransform);
        } catch (Throwable throwable) {
            ForgottenJungleRuntime.clearPortalEntryCooldown(playerUuid);
            System.out.println("[TFJ][Portal] Entry failed player=" + playerUuid + ": " + ForgottenJungleRuntime.describeThrowable(throwable));
        }
    }
}
