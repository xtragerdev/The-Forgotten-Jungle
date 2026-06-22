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


final class RaptorRoleInfo {
    final EggInfo info;
    final String role;
    final String stageLabel;
    final String statusLabel;
    final String nextRole;
    final long durationMs;
    final boolean breedingStage;
    final double visualScale;

    RaptorRoleInfo(EggInfo info, String role, String stageLabel, String statusLabel, String nextRole, long durationMs, boolean breedingStage, double visualScale) {
        this.info = info;
        this.role = role;
        this.stageLabel = stageLabel;
        this.statusLabel = statusLabel;
        this.nextRole = nextRole;
        this.durationMs = Math.max(1_000L, durationMs);
        this.breedingStage = breedingStage;
        this.visualScale = clamp(visualScale, 0.1, 4.0);
    }

    boolean canAdvance() {
        return this.nextRole != null && !this.nextRole.isBlank();
    }

    boolean isGrowthStage() {
        return canAdvance();
    }

    boolean isBreedingStage() {
        return this.breedingStage;
    }
}
