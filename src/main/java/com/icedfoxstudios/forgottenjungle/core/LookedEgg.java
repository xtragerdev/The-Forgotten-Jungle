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


final class LookedEgg {
    final int x;
    final int y;
    final int z;
    final EggInfo info;

    LookedEgg(int x, int y, int z, EggInfo info) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.info = info;
    }
}
