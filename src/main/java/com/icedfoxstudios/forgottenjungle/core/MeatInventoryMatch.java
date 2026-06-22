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


final class MeatInventoryMatch {
    final String itemId;
    final int quantity;

    MeatInventoryMatch(String itemId, int quantity) {
        this.itemId = itemId == null ? "" : itemId;
        this.quantity = Math.max(0, quantity);
    }
}
