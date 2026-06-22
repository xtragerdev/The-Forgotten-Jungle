package com.icedfoxstudios.forgottenjungle.core;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.joml.Vector3d;

import static com.icedfoxstudios.forgottenjungle.raptor.config.RaptorConfig.*;
import static com.icedfoxstudios.forgottenjungle.raptor.text.RaptorText.*;


final class EggRuntimeState {
    final String worldKey;
    final int x;
    final int y;
    final int z;
    final EggInfo info;
    long startedAtMs;
    long hatchAtMs;
    long nextHatchAttemptMs;
    long lastSeenMs;
    boolean hatchPending;
    boolean hatched;

    EggRuntimeState(String worldKey, int x, int y, int z, EggInfo info, long nowMs) {
        this.worldKey = worldKey;
        this.x = x;
        this.y = y;
        this.z = z;
        this.info = info;
        this.startedAtMs = nowMs;
        this.hatchAtMs = nowMs + INCUBATION_MS;
        this.nextHatchAttemptMs = this.hatchAtMs;
        this.lastSeenMs = nowMs;
    }
}
