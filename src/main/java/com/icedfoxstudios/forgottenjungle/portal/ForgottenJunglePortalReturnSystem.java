package com.icedfoxstudios.forgottenjungle.portal;

import com.hypixel.hytale.builtin.instances.InstancesPlugin;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.icedfoxstudios.forgottenjungle.core.ForgottenJungleRuntime;
import java.util.UUID;
import org.joml.Vector3d;

public final class ForgottenJunglePortalReturnSystem extends EntityTickingSystem<EntityStore> {
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
        if (world == null || commandBuffer == null || !ForgottenJungleRuntime.isForgottenJungleWorld(world)) {
            return;
        }

        PlayerRef playerRef = archetypeChunk.getComponent(index, PlayerRef.getComponentType());
        if (playerRef == null || playerRef.getUuid() == null || playerRef.getReference() == null || playerRef.getTransform() == null) {
            return;
        }

        long nowMs = System.currentTimeMillis();
        UUID playerUuid = playerRef.getUuid();
        if (ForgottenJungleRuntime.isPortalReturnOnCooldown(playerUuid, nowMs)) {
            return;
        }

        Vector3d position = playerRef.getTransform().getPosition();
        if (position == null) {
            return;
        }

        int[] portalPos = ForgottenJungleRuntime.findNearestBlock(world, position, ForgottenJungleRuntime.getTFJReturnPortalBlockId(), "TFJ_Return_Portal", 2, 3);
        if (portalPos == null) {
            return;
        }

        ForgottenJungleRuntime.setPortalReturnCooldown(playerUuid, nowMs + 9000L);
        ForgottenJungleRuntime.setPortalEntryCooldown(playerUuid, nowMs + 8000L);
        try {
            System.out.println("[TFJ][Portal] Return player=" + playerUuid + " portal=" + portalPos[0] + "," + portalPos[1] + "," + portalPos[2]);
            InstancesPlugin.exitInstance(playerRef.getReference(), commandBuffer);
        } catch (Throwable throwable) {
            ForgottenJungleRuntime.clearPortalReturnCooldown(playerUuid);
            System.out.println("[TFJ][Portal] Return failed player=" + playerUuid + ": " + ForgottenJungleRuntime.describeThrowable(throwable));
        }
    }
}
