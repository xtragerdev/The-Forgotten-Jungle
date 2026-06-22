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


final class RaptorGuideRow {
    final String name;
    final String detail;
    final String chance;

    RaptorGuideRow(String name, String detail, String chance) {
        this.name = name == null ? "" : name;
        this.detail = detail == null ? "" : detail;
        this.chance = chance == null ? "" : chance;
    }
}
