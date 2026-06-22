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


final class PterodactylMoveInput {
    final boolean jumping;
    final boolean swimJumping;
    final boolean crouching;
    final boolean forcedCrouching;

    PterodactylMoveInput(boolean jumping, boolean swimJumping, boolean crouching, boolean forcedCrouching) {
        this.jumping = jumping;
        this.swimJumping = swimJumping;
        this.crouching = crouching;
        this.forcedCrouching = forcedCrouching;
    }

    static PterodactylMoveInput fromStates(Object states) {
        return new PterodactylMoveInput(
            ForgottenJungleRuntime.getBooleanMember(states, "jumping"),
            ForgottenJungleRuntime.getBooleanMember(states, "swimJumping"),
            ForgottenJungleRuntime.getBooleanMember(states, "crouching"),
            ForgottenJungleRuntime.getBooleanMember(states, "forcedCrouching")
        );
    }
}
