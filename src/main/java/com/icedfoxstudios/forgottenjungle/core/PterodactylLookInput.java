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


final class PterodactylLookInput {
    final double pitch;
    final double yaw;
    @SuppressWarnings("unused")
    final double roll;

    PterodactylLookInput(double pitch, double yaw, double roll) {
        this.pitch = pitch;
        this.yaw = yaw;
        this.roll = roll;
    }

    Vector3d toForward() {
        try {
            double normalizedPitch = Math.abs(pitch) > Math.PI * 2.0 ? Math.toRadians(pitch) : pitch;
            double normalizedYaw = Math.abs(yaw) > Math.PI * 2.0 ? Math.toRadians(yaw) : yaw;
            Vector3d forward = new Vector3d(0.0, 0.0, -1.0);
            forward.rotateX(normalizedPitch);
            forward.rotateY(normalizedYaw);
            if (!forward.isFinite() || ForgottenJungleRuntime.isNearZero(forward, 0.0001)) {
                return null;
            }
            return forward.normalize();
        } catch (Throwable ignored) {
            return null;
        }
    }
}
