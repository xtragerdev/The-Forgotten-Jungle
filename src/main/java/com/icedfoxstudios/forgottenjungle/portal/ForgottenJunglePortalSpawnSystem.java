package com.icedfoxstudios.forgottenjungle.portal;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.icedfoxstudios.forgottenjungle.core.ForgottenJungleRuntime;
import org.joml.Vector3d;

public final class ForgottenJunglePortalSpawnSystem extends EntityTickingSystem<EntityStore> {
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
        if (world == null || commandBuffer == null) {
            return;
        }

        PlayerRef playerRef = archetypeChunk.getComponent(index, PlayerRef.getComponentType());
        if (playerRef == null || playerRef.getUuid() == null || playerRef.getTransform() == null) {
            return;
        }

        Transform transform = playerRef.getTransform();
        Vector3d position = transform.getPosition();
        if (position == null) {
            return;
        }

        if (ForgottenJungleRuntime.isForgottenJungleWorld(world)) {
            ForgottenJungleRuntime.ensureManagedPortalNearPlayer(world, playerRef.getUuid(), position, true, commandBuffer);
        } else {
            ForgottenJungleRuntime.ensureManagedPortalNearPlayer(world, playerRef.getUuid(), position, false, commandBuffer);
        }
    }
}
